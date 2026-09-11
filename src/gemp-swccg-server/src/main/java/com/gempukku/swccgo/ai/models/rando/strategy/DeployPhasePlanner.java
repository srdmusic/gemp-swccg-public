package com.gempukku.swccgo.ai.models.rando.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.common.phase.DeployPlanRankingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployFormationPromisePolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleInitiationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployUndercoverModePolicy;
import com.gempukku.swccgo.ai.models.common.phase.SpaceDeploymentAllocationFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.SpaceDeploymentAllocationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.phase.LateEstablishPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePlanAdapter;
import com.gempukku.swccgo.ai.models.common.phase.ProspectiveBattleSynergy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.EndorOperationsTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.strategy.ForceDrainProjection;
import com.gempukku.swccgo.ai.models.common.strategy.MovePredicates;
import com.gempukku.swccgo.ai.models.common.strategy.PublicImmediateReactAnalyzer;
import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;

import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: DEPLOY-1 (Sequencing & Budget) (reorg 2026-07-06) ═══
// Owns: deploy-phase planning layer: what to deploy this turn, hold-back, persona dead-card skip,
// V22 combo/turn planning, V22.3 pacing, V59 shuttle safety, V21 objective-critical holds. Hub: none.
// KIND mix (DEPLOY-1 overall): 11 ORDERING / 9 BANDED / 4 VETO.
// PARITY PAIR WARNING: V179 (DeployPhaseScript) <-> V67ai (deploy-from-reserve magnitudes in
// DeployEvaluator/ActionTextEvaluator) must stay in lockstep — re-check both when touching either.
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: DEPLOY-1 twin DeployPhaseScript (the DPS bucket walk), DEPLOY-2/DEPLOY-3 (siting/weapons),
// CombinedEvaluator DPS lines (co-owned with SVC-SAFETY), PULL-ENGINE. See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
/**
 * Deploy Phase Planner - Creates holistic deployment plans for the entire phase.
 *
 * COMPREHENSIVE PORT from Python deploy_planner.py (7000+ lines).
 *
 * Key features:
 * 1. MULTIPLE PLAN GENERATION - generates ground, space, combined plans and picks best
 * 2. OPTIMAL COMBINATION FINDING - finds best card combinations within budget
 * 3. FORCE DRAIN GAP - tracks drain economy to prioritize stop-bleeding
 * 4. DYNAMIC THRESHOLDS - adjusts thresholds based on life force and game state
 * 5. PLAN SCORING - scores all plans and selects highest
 * 6. NEXT-TURN OPPORTUNITY - checks if holding would enable better plays
 *
 * Strategic priority order:
 * 1. DEPLOY LOCATIONS FIRST - opens new deployment options
 * 2. STOP BLEEDING - contest locations where opponent drains us
 * 3. REINFORCE LOSING - add power where we're being beaten
 * 4. ESTABLISH - take uncontested high-value locations
 * 5. BUILD UP - reinforce winning positions (but not overkill)
 */
public class DeployPhasePlanner {
    private static final Logger LOG = RandoLogger.getStrategyLogger();

    // Config constants
    private final int deployThreshold;
    private final int battleForceReserve;

    // Current plan (cached)
    private DeploymentPlan currentPlan;
    private int lastPlanTurn = -1;
    private boolean lastPlanHadActiveFlipGate = false;
    private boolean lastPlanHadFlipGateActorInHand = false;
    private String lastPlanPreFlipProgressFingerprint = "";
    private long lastPlanPersistentResponseRevision = -1;
    private DeployFormationPromisePolicy.Promise activeFormationPromise;
    private int activeFormationPromiseTurn = -1;
    private boolean activeFormationPromiseIsBattleFocus = false;
    private Set<Integer> contestedAnchorsAtPlanCreation =
            Collections.emptySet();

    private enum CountedPackageState {
        NONE,
        CONTINUE,
        RELEASE
    }

    // Board state reference for scoring
    private SwccgGame currentGame;
    private String currentPlayerId;
    private Side currentSide;

    // V21: Objective awareness for location prioritization
    private ObjectiveAnalyzer objectiveAnalyzer;
    private StrategyController strategyController;

    public DeployPhasePlanner() {
        this(RandoConfig.DEPLOY_THRESHOLD, RandoConfig.BATTLE_FORCE_RESERVE);
    }

    public DeployPhasePlanner(int deployThreshold, int battleForceReserve) {
        this.deployThreshold = deployThreshold;
        this.battleForceReserve = battleForceReserve;
    }

    /**
     * Reset planner state for a new game.
     */
    public void reset() {
        currentPlan = null;
        lastPlanTurn = -1;
        lastPlanHadActiveFlipGate = false;
        lastPlanHadFlipGateActorInHand = false;
        lastPlanPreFlipProgressFingerprint = "";
        lastPlanPersistentResponseRevision = -1;
        activeFormationPromise = null;
        activeFormationPromiseTurn = -1;
        activeFormationPromiseIsBattleFocus = false;
        contestedAnchorsAtPlanCreation = Collections.emptySet();
        currentGame = null;
        currentPlayerId = null;
    }

    /**
     * V21: Set the objective analyzer for location prioritization.
     */
    public void setObjectiveAnalyzer(ObjectiveAnalyzer analyzer) {
        this.objectiveAnalyzer = analyzer;
    }

    public void setStrategyController(StrategyController controller) {
        strategyController = controller;
    }

    /**
     * Get the current deployment plan, if any.
     */
    public DeploymentPlan getCurrentPlan() {
        return currentPlan;
    }

    /** Release an unstarted packet when the engine does not offer its exact target. */
    public boolean rejectUnavailableDeployTarget(
            PhysicalCard deployingCard, Collection<String> offeredTargetIds) {
        if (currentPlan == null || deployingCard == null
                || offeredTargetIds == null || currentPlayerId == null
                || deployingCard.getZone() != Zone.HAND
                || !currentPlayerId.equals(deployingCard.getOwner())
                || deployingCard.getBlueprint() == null
                || deployingCard.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER
                || currentPlan.getDeploymentsMade() > 0
                || currentPlan.getCountedDeployRouteKey() != null
                || activeFormationPromise != null) {
            return false;
        }
        DeploymentInstruction instruction = currentPlan
                .getInstructionForPhysicalCard(
                    deployingCard.getPermanentCardId(),
                    deployingCard.getCardId(),
                    deployingCard.getBlueprintId(true));
        if (instruction == null
                || instruction.getCardPermanentCardId() == null
                || instruction.getCardCurrentCardId() == null
                || instruction.getTargetLocationId() == null
                || offeredTargetIds.contains(instruction.getTargetLocationId())) {
            return false;
        }
        PhysicalCard target;
        try {
            target = currentGame.getGameState().findCardById(
                    Integer.parseInt(instruction.getTargetLocationId()));
        } catch (RuntimeException ignored) {
            return false;
        }
        if (target == null || target.getBlueprint() == null
                || target.getBlueprint().getCardSubtype()
                    != com.gempukku.swccgo.common.CardSubtype.SITE) {
            return false;
        }
        LOG.warn("V312 DEPLOY TARGET REPLAN: {} cannot select planned {}; release unstarted packet",
                deployingCard.getTitle(), instruction.getTargetLocationName());
        currentPlan = null;
        lastPlanTurn = -1;
        return true;
    }

    private DeploymentPlan detectPromisedFormation(
            SwccgGame game, String playerId, int currentTurn) {
        if (currentPlan == null) return null;
        List<DeployFormationPromisePolicy.PlannedInstruction> facts =
                formationFacts(currentPlan);
        DeployFormationPromisePolicy.Promise promise =
                DeployFormationPromisePolicy.detect(
                        game, playerId,
                        game.getGameState().getForcePileSize(playerId),
                        battleForceReserve,
                        facts);
        if (promise == null) return null;

        List<DeploymentInstruction> committed =
                committedFormationInstructions(
                    currentPlan, promise.committedCards());
        if (committed == null || committed.isEmpty()) return null;
        currentPlan.recordDeployment(
                promise.anchorPermanentCardId(),
                promise.anchorCurrentCardId(), null);
        currentPlan.setInstructions(committed);
        currentPlan.setWaitingForPlannedCards(false);
        currentPlan.setForceAllowExtras(false);
        currentPlan.setForceToSpend(promise.committedCards().stream()
                .mapToInt(DeployFormationPromisePolicy.CommittedCard::cost)
                .sum());
        currentPlan.setReason("Finish promised formation at "
                + committed.get(0).getTargetLocationName());
        activeFormationPromise = promise;
        activeFormationPromiseTurn = currentTurn;
        activeFormationPromiseIsBattleFocus = Objects.equals(
                currentPlan.getPlannedBattleTargetLocationId(),
                promise.targetLocationId());
        LOG.warn("DEPLOY.FORMATION.PROMISE: preserving {} exact companion(s) at {} before replanning",
                committed.size(), committed.get(0).getTargetLocationName());
        return currentPlan;
    }

    private List<DeploymentInstruction> committedFormationInstructions(
            DeploymentPlan plan,
            List<DeployFormationPromisePolicy.CommittedCard> members) {
        List<DeploymentInstruction> result = new ArrayList<>();
        if (plan == null || members == null) return null;
        for (DeployFormationPromisePolicy.CommittedCard member : members) {
            DeploymentInstruction instruction =
                    plan.getInstructionForPhysicalCard(
                        member.permanentCardId(),
                        member.currentCardId(), null);
            if (instruction == null) return null;
            instruction.setDeployCost(member.cost());
            result.add(instruction);
        }
        return result;
    }

    private boolean refreshActiveFormationPromise(
            SwccgGame game, String playerId) {
        List<DeployFormationPromisePolicy.CommittedCard> remaining =
                DeployFormationPromisePolicy.remainingCommittedCards(
                    game, playerId,
                    game.getGameState().getForcePileSize(playerId),
                    battleForceReserve, activeFormationPromise);
        if (remaining == null || remaining.isEmpty()) return false;
        List<DeploymentInstruction> instructions =
                committedFormationInstructions(currentPlan, remaining);
        if (instructions == null || instructions.isEmpty()) return false;
        currentPlan.setInstructions(instructions);
        currentPlan.setForceToSpend(remaining.stream()
                .mapToInt(DeployFormationPromisePolicy.CommittedCard::cost)
                .sum());
        return true;
    }

    private CountedPackageState refreshCountedDeployPackage(
            SwccgGame game, String playerId) {
        if (currentPlan == null
                || currentPlan.getCountedDeployRouteKey() == null) {
            return CountedPackageState.NONE;
        }
        if (objectiveAnalyzer == null) {
            currentPlan.clearCountedDeployPackage();
            return CountedPackageState.RELEASE;
        }

        boolean progressed = false;
        Set<String> progressedTargets = new LinkedHashSet<>();
        int remainingCost = 0;
        List<DeploymentInstruction> remaining = new ArrayList<>();
        for (DeploymentPlan.CountedDeployCommitment commitment
                : currentPlan.getCountedDeployCommitments()) {
            PhysicalCard card = game.getGameState()
                    .findCardByPermanentId(
                        commitment.permanentCardId());
            PhysicalCard target = exactLocation(
                    game.getGameState(),
                    commitment.targetLocationId());
            if (card == null || target == null) {
                currentPlan.clearCountedDeployPackage();
                return CountedPackageState.RELEASE;
            }
            if (card.getCardId() == commitment.currentCardId()
                    && card.getZone() == Zone.HAND) {
                DeploymentInstruction template = currentPlan
                    .getCountedDeployInstructions().stream()
                    .filter(instruction ->
                        instruction.getCardPermanentCardId() != null
                            && instruction.getCardCurrentCardId() != null
                            && instruction.getCardPermanentCardId()
                                == commitment.permanentCardId()
                            && instruction.getCardCurrentCardId()
                                == commitment.currentCardId())
                    .findFirst().orElse(null);
                Integer exactCost = exactDeployCostAt(card, target);
                if (template == null || exactCost == null
                        || !canDeployPaidDirectly(card, target)) {
                    currentPlan.clearCountedDeployPackage();
                    return CountedPackageState.RELEASE;
                }
                DeploymentInstruction instruction =
                        new DeploymentInstruction(template);
                instruction.setDeployCost(exactCost);
                remaining.add(instruction);
                remainingCost += exactCost;
                continue;
            }
            if (!isCommittedCardAtTarget(
                    game, playerId, card, target)) {
                currentPlan.clearCountedDeployPackage();
                return CountedPackageState.RELEASE;
            }
            progressed = true;
            progressedTargets.add(commitment.targetLocationId());
        }

        boolean ruleSatisfied = objectiveAnalyzer
                .isCountedDeployRuleSatisfied(
                    game, playerId,
                    currentPlan.getCountedDeployRouteKey());
        if (ruleSatisfied && progressed) {
            remaining = remaining.stream()
                .filter(instruction -> progressedTargets.contains(
                    instruction.getTargetLocationId()))
                .collect(Collectors.toCollection(ArrayList::new));
            remainingCost = remaining.stream()
                .mapToInt(DeploymentInstruction::getDeployCost)
                .sum();
        }
        int spendable = Math.max(0,
                game.getGameState().getForcePileSize(playerId)
                    - battleForceReserve);
        if (remainingCost > spendable) {
            currentPlan.clearCountedDeployPackage();
            return CountedPackageState.RELEASE;
        }
        if (!progressed && ruleSatisfied) {
            currentPlan.clearCountedDeployPackage();
            return CountedPackageState.RELEASE;
        }
        if (remaining.isEmpty()) {
            currentPlan.clearCountedDeployPackage();
            return CountedPackageState.RELEASE;
        }
        if (!countedPackageStillSafe(game, playerId, remaining)) {
            currentPlan.clearCountedDeployPackage();
            return CountedPackageState.RELEASE;
        }
        String activeTarget = remaining.stream()
            .filter(instruction -> progressedTargets.contains(
                instruction.getTargetLocationId()))
            .map(DeploymentInstruction::getTargetLocationId)
            .findFirst()
            .orElse(remaining.get(0).getTargetLocationId());
        List<DeploymentInstruction> activePacket = remaining.stream()
            .filter(instruction -> Objects.equals(
                activeTarget, instruction.getTargetLocationId()))
            .collect(Collectors.toCollection(ArrayList::new));
        currentPlan.setInstructions(activePacket);
        currentPlan.setWaitingForPlannedCards(false);
        currentPlan.setForceAllowExtras(false);
        currentPlan.setForceToSpend(remainingCost);
        currentPlan.setReason(
                "Finish exact counted objective package");
        LOG.warn("V307 OBJECTIVE.DEPLOY.COUNTED_PACKAGE: exposing {} local member(s) at {} while preserving {} exact member(s)",
                activePacket.size(), activeTarget, remaining.size());
        return CountedPackageState.CONTINUE;
    }

    private boolean countedPackageStillSafe(
            SwccgGame game, String playerId,
            List<DeploymentInstruction> remaining) {
        String opponentId = game.getGameState().getOpponent(playerId);
        if (opponentId == null || remaining == null
                || remaining.isEmpty()) {
            return false;
        }
        Map<String, List<CardInfo>> packetsByTarget =
                new LinkedHashMap<>();
        for (DeploymentInstruction instruction : remaining) {
            if (instruction.getCardPermanentCardId() == null
                    || instruction.getTargetLocationId() == null) {
                return false;
            }
            PhysicalCard card = game.getGameState()
                    .findCardByPermanentId(
                        instruction.getCardPermanentCardId());
            if (card == null || card.getZone() != Zone.HAND) {
                return false;
            }
            packetsByTarget.computeIfAbsent(
                    instruction.getTargetLocationId(), ignored ->
                        new ArrayList<>()).add(new CardInfo(card));
        }
        for (Map.Entry<String, List<CardInfo>> entry
                : packetsByTarget.entrySet()) {
            PhysicalCard target = exactLocation(
                    game.getGameState(), entry.getKey());
            if (target == null) return false;
            AiBoardAnalyzer.LocationAnalysis liveTarget =
                    AiBoardAnalyzer.analyzeLocation(
                        game, playerId, opponentId, target, currentSide);
            if (!objectivePacketIsSafe(
                    liveTarget, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static PhysicalCard exactLocation(
            GameState gameState, String locationId) {
        if (gameState == null || locationId == null) return null;
        try {
            return gameState.findCardById(
                    Integer.parseInt(locationId));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isCommittedCardAtTarget(
            SwccgGame game, String playerId,
            PhysicalCard card, PhysicalCard target) {
        if (game == null || playerId == null || card == null
                || target == null || card.getZone() == null
                || !card.getZone().isInPlay()
                || !playerId.equals(card.getOwner())) {
            return false;
        }
        try {
            PhysicalCard actual = game.getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        game.getGameState(), card);
            return actual != null
                    && actual.getCardId() == target.getCardId();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private DeploymentPlan replanToLiveContestedAnchor(
            SwccgGame game, String playerId, int currentTurn) {
        if (currentPlan == null) return null;
        DeployFormationPromisePolicy.Promise promise =
                DeployFormationPromisePolicy.detectLiveContestedAnchor(
                        game, playerId,
                        game.getGameState().getForcePileSize(playerId),
                        battleForceReserve, formationFacts(currentPlan),
                        contestedAnchorsAtPlanCreation);
        if (promise == null) return null;

        DeploymentInstruction companion =
                currentPlan.getInstructionForPhysicalCard(
                        promise.companionPermanentCardId(),
                        promise.companionCurrentCardId(), null);
        PhysicalCard target;
        try {
            target = game.getGameState().findCardById(
                    Integer.parseInt(promise.targetLocationId()));
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (companion == null || target == null) return null;

        currentPlan.recordDeployment(
                promise.anchorPermanentCardId(),
                promise.anchorCurrentCardId(), null);
        companion.setTargetLocationId(promise.targetLocationId());
        companion.setTargetLocationName(target.getTitle());
        companion.setPriority(0);
        companion.setReason("Join new contested anchor before it fights alone");
        companion.setDeployCost(promise.companionCost());
        currentPlan.setInstructions(new ArrayList<>(List.of(companion)));
        currentPlan.setWaitingForPlannedCards(false);
        currentPlan.setForceAllowExtras(false);
        currentPlan.setForceToSpend(promise.companionCost());
        currentPlan.setForceReservedForBattle(battleForceReserve);
        activeFormationPromise = promise;
        activeFormationPromiseTurn = currentTurn;
        activeFormationPromiseIsBattleFocus = false;
        LOG.warn("DEPLOY.FORMATION.LIVE_ANCHOR: retargeting exact companion {} to {} and preserving {} Force for battle",
                promise.companionName(), target.getTitle(),
                battleForceReserve);
        return currentPlan;
    }

    private static List<DeployFormationPromisePolicy.PlannedInstruction>
    formationFacts(DeploymentPlan plan) {
        List<DeployFormationPromisePolicy.PlannedInstruction> facts =
                new ArrayList<>();
        if (plan == null) return facts;
        for (DeploymentInstruction instruction : plan.getInstructions()) {
            if (instruction.getCardPermanentCardId() == null
                    || instruction.getCardCurrentCardId() == null
                    || instruction.getTargetLocationId() == null) {
                continue;
            }
            facts.add(new DeployFormationPromisePolicy.PlannedInstruction(
                    instruction.getCardPermanentCardId(),
                    instruction.getCardCurrentCardId(),
                    instruction.getTargetLocationId()));
        }
        return facts;
    }

    // =========================================================================
    // MAIN ENTRY POINT - createPlan
    // =========================================================================

    /**
     * Create a deployment plan for this phase.
     *
     * This is the main entry point, ported from Python create_plan().
     * Generates MULTIPLE plans and picks the best one.
     */
    public DeploymentPlan createPlan(SwccgGame game, String playerId, Side side) {
        this.currentGame = game;
        this.currentPlayerId = playerId;
        this.currentSide = side;

        GameState gameState = game.getGameState();
        if (gameState == null) {
            return createHoldBackPlan("No game state available");
        }

        int currentTurn = gameState.getPlayersLatestTurnNumber(playerId);
        if (activeFormationPromiseTurn != currentTurn) {
            activeFormationPromise = null;
            activeFormationPromiseTurn = -1;
            activeFormationPromiseIsBattleFocus = false;
        }
        String opponentId = gameState.getOpponent(playerId);
        boolean hasActiveFlipGate = hasActiveFlipGateLocation(
                game, playerId);
        boolean hasFlipGateActorInHand = hasFlipGateActorInHand(
                game, playerId);
        String preFlipProgressFingerprint = objectiveAnalyzer == null
                ? ""
                : objectiveAnalyzer.getPreFlipProgressFingerprint(
                        game, playerId);
        if (preFlipProgressFingerprint == null) {
            preFlipProgressFingerprint = "";
        }
        PersistentResponsePolicy.Snapshot persistentSnapshot =
                strategyController == null
                        ? PersistentResponsePolicy.Snapshot.empty()
                        : strategyController.getPersistentResponseSnapshot();
        if (persistentSnapshot == null) {
            persistentSnapshot = PersistentResponsePolicy.Snapshot.empty();
        }
        long persistentRevision = persistentSnapshot.revision();

        // If we already have a plan for this turn, return it
        if (currentPlan != null && lastPlanTurn == currentTurn) {
            if (currentPlan.getPlannedBattleTargetLocationId() != null
                    && currentPlan.isPlanComplete()) {
                LOG.debug("V308 BATTLE STACK: deployment packet is complete; retain battle intent through this turn");
                return currentPlan;
            }
            CountedPackageState countedPackageState =
                    refreshCountedDeployPackage(game, playerId);
            if (countedPackageState
                    == CountedPackageState.CONTINUE) {
                return currentPlan;
            }
            boolean countedPackageReleased = countedPackageState
                    == CountedPackageState.RELEASE;
            boolean formationPromiseReleased = false;
            if (!countedPackageReleased
                    && activeFormationPromise != null) {
                if (refreshActiveFormationPromise(game, playerId)) {
                    LOG.debug("DEPLOY.FORMATION.PROMISE: returning exact formation queue for turn {}",
                            currentTurn);
                    return currentPlan;
                }
                if (activeFormationPromiseIsBattleFocus) {
                    LOG.warn("V308 BATTLE STACK COMPLETE: keep the exact battle target and do not rebuild a spreading plan before battle");
                    activeFormationPromise = null;
                    activeFormationPromiseTurn = -1;
                    activeFormationPromiseIsBattleFocus = false;
                    return currentPlan;
                }
                LOG.warn("DEPLOY.FORMATION.PROMISE.RELEASE: package is complete or no longer executable");
                activeFormationPromise = null;
                activeFormationPromiseTurn = -1;
                activeFormationPromiseIsBattleFocus = false;
                formationPromiseReleased = true;
            } else if (!countedPackageReleased) {
                DeploymentPlan promised = detectPromisedFormation(
                        game, playerId, currentTurn);
                if (promised != null) return promised;
            }
            if (!countedPackageReleased) {
                DeploymentPlan liveAnchorPlan =
                        replanToLiveContestedAnchor(
                            game, playerId, currentTurn);
                if (liveAnchorPlan != null) return liveAnchorPlan;
            }

            if (countedPackageReleased) {
                LOG.info("V307 OBJECTIVE.DEPLOY.COUNTED_PACKAGE.REPLAN: refreshing after package release");
            } else if (formationPromiseReleased) {
                LOG.info("DEPLOY.FORMATION.PROMISE.REPLAN: refreshing after promise release");
            } else if (!lastPlanHadActiveFlipGate && hasActiveFlipGate) {
                LOG.warn("OBJECTIVE.DEPLOY.REPLAN_GATE_APPEARED: refreshing turn {} plan "
                        + "because the exact pre-flip actor gate entered play",
                    currentTurn);
            } else if (!lastPlanHadFlipGateActorInHand
                    && hasFlipGateActorInHand) {
                LOG.warn("OBJECTIVE.DEPLOY.REPLAN_ACTOR_ARRIVED: refreshing turn {} plan "
                        + "because the missing flip-gate actor entered hand",
                    currentTurn);
            } else if (!lastPlanPreFlipProgressFingerprint.equals(
                    preFlipProgressFingerprint)) {
                LOG.warn("OBJECTIVE.DEPLOY.REPLAN_PROGRESS_CHANGED: refreshing turn {} plan "
                        + "because structured pre-flip progress changed from {} to {}",
                    currentTurn, lastPlanPreFlipProgressFingerprint,
                    preFlipProgressFingerprint);
            } else if (lastPlanPersistentResponseRevision
                    != persistentRevision) {
                LOG.warn("DEPLOY.PERSISTENT_RESPONSE.REPLAN: refreshing turn {} plan "
                        + "because completed public drain evidence changed from revision {} to {}",
                    currentTurn, lastPlanPersistentResponseRevision,
                    persistentRevision);
            } else {
                LOG.debug("📋 Returning cached plan for turn {} ({} instructions remaining)",
                    currentTurn, currentPlan.getInstructions().size());
                return currentPlan;
            }
        }

        LOG.info("📋 ═══════════════════════════════════════════════════════════════");
        LOG.info("📋 CREATING COMPREHENSIVE DEPLOYMENT PLAN (Turn {})", currentTurn);
        LOG.info("📋 ═══════════════════════════════════════════════════════════════");

        // === GET RESOURCES ===
        int forceAvailable = gameState.getForcePileSize(playerId);
        int lifeForce = gameState.getPlayerLifeForce(playerId);
        int opponentLifeForce = gameState.getPlayerLifeForce(opponentId);
        List<PhysicalCard> hand = gameState.getHand(playerId);

        LOG.info("📊 Resources: force={}, life={}, opponent_life={}, hand_size={}",
            forceAvailable, lifeForce, opponentLifeForce, hand.size());

        // === CATEGORIZE HAND ===
        List<CardInfo> allCards = new ArrayList<>();
        List<CardInfo> locations = new ArrayList<>();
        List<CardInfo> characters = new ArrayList<>();
        List<CardInfo> starships = new ArrayList<>();
        List<CardInfo> vehicles = new ArrayList<>();
        List<CardInfo> deadCards = new ArrayList<>();

        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) continue;
            CardInfo info = new CardInfo(card);
            allCards.add(info);

            // === PERSONA CHECK: Skip dead cards (persona already deployed) ===
            if (AiCardHelper.isDeadCard(card, game, playerId)) {
                deadCards.add(info);
                LOG.info("☠️ DEAD CARD: {} - persona already on table, skipping deployment planning",
                    info.name);
                continue;  // Don't add to deployable categories
            }

            if (info.isLocation) locations.add(info);
            else if (info.isCharacter) characters.add(info);
            else if (info.isStarship) starships.add(info);
            else if (info.isVehicle) vehicles.add(info);
        }

        LOG.info("📋 Hand: {} locations, {} characters, {} starships, {} vehicles, {} dead cards",
            locations.size(), characters.size(), starships.size(), vehicles.size(), deadCards.size());
        if (!deadCards.isEmpty()) {
            LOG.info("☠️ Dead cards (persona in play): {}", deadCards.stream()
                .map(c -> c.name).collect(Collectors.joining(", ")));
        }

        // Log hand details
        logHandDetails(characters, starships, vehicles);

        // === ANALYZE BOARD ===
        List<AiBoardAnalyzer.LocationAnalysis> allLocations = AiBoardAnalyzer.analyzeAllLocations(
            game, playerId, opponentId, side);

        logBoardAnalysis(allLocations);

        // === CATEGORIZE LOCATIONS ===
        LocationCategories categories = categorizeLocations(allLocations, playerId);

        LOG.info("📊 Location categories: {} losing, {} winning, {} bleed, {} establish, {} attack, {} weak",
            categories.losingLocations.size(), categories.winningLocations.size(),
            categories.bleedLocations.size(), categories.establishTargets.size(),
            categories.attackTargets.size(), categories.weakPresenceLocations.size());

        // === CALCULATE FORCE DRAIN GAP ===
        DrainGapResult drainGap = calculateForceDrainGap(allLocations);
        LOG.info("💧 Drain economy: we drain {}, they drain {} = gap {:+d}",
            drainGap.ourDrain, drainGap.theirDrain, drainGap.drainGap);

        // === CALCULATE DYNAMIC THRESHOLDS ===
        int groundThreshold = getDynamicThreshold(allLocations, false, currentTurn, lifeForce);
        int spaceThreshold = getDynamicThreshold(allLocations, true, currentTurn, lifeForce);

        LOG.info("📊 Dynamic thresholds: ground={}, space={}", groundThreshold, spaceThreshold);

        // === GENERATE MULTIPLE PLANS ===
        List<ScoredPlan> allPlans = new ArrayList<>();
        // V22.3: Reserve Force for maintenance cards already in play
        // T2 COMMIT-1 (2026-07-06, audit force-economy-1): reserve the ENGINE's
        // card-specific maintain cost (MaintenanceFacts, parsed from game text;
        // e.g. Lando Scoundrel maintains for 1, deploys for 5). The old comment
        // "Maintenance cost in SWCCG = card's deploy cost" was the audit-refuted
        // claim — it OVER-reserved 2-5x and starved the deploy budget.
        // T2 MOVE #1 COMMIT-2 (2026-07-06): the planner has no DecisionContext, so it
        // calls the shared ForceReserveService directly (plan creation is already
        // once-per-turn). The service is the sole live scan: in-play-gated, on the
        // MaintenanceFacts basis, replacing the old inline loop's deploy-cost basis
        // and its incorrect `if (allCards != null)` guard, which tested the WRONG
        // variable (loop removed in cleanup batch 1.6, 2026-07-13).
        // V22.3 updated in place; effectiveForce math unchanged.
        int maintenanceReserve = 0;
        try {
            maintenanceReserve = com.gempukku.swccgo.ai.models.common.strategy
                .ForceReserveService.compute(game, gameState, playerId).maintenanceObligation;
            if (maintenanceReserve > 0) {
                LOG.warn("V22.3 MAINTENANCE: Reserving {} total Force for maintenance upkeep", maintenanceReserve);
            }
        } catch (Exception e) {
            LOG.debug("V22.3 MAINTENANCE: Error counting maintenance cards: {}", e.getMessage());
        }
        int effectiveForce = forceAvailable - battleForceReserve - maintenanceReserve;

        // Track location deploys (apply to all plans)
        List<CardInfo> locationDeploys = planLocationDeploys(locations, effectiveForce);
        int forceAfterLocations = effectiveForce;
        for (CardInfo loc : locationDeploys) {
            forceAfterLocations -= loc.cost;
        }
        DeploymentPlan endorSystemPlan = generateEopEndorSystemPlan(
            starships,
            characters.stream().filter(card -> card.isPilot)
                .collect(Collectors.toList()),
            allLocations,
            Math.max(0, forceAfterLocations));

        // Generate ground plans
        List<ScoredPlan> groundPlans = generateGroundPlans(
            characters, vehicles, categories, forceAfterLocations,
            Math.max(0, forceAfterLocations), groundThreshold,
            allLocations, currentTurn, drainGap);
        allPlans.addAll(groundPlans);

        // Generate space plans
        List<ScoredPlan> spacePlans = generateSpacePlans(
            starships, characters, categories, forceAfterLocations, spaceThreshold,
            allLocations, currentTurn, drainGap, game, playerId, endorSystemPlan);
        allPlans.addAll(spacePlans);

        // Generate combined plans (best of ground + space within budget)
        List<ScoredPlan> combinedPlans = generateCombinedPlans(
            characters, starships, vehicles, categories, forceAfterLocations,
            groundThreshold, spaceThreshold, allLocations, currentTurn, drainGap);
        allPlans.addAll(combinedPlans);

        PersistentPlanSelection persistentSelection =
            selectPersistentResponsePlan(
                allPlans, allLocations,
                Math.max(0, forceAfterLocations),
                Math.max(0, forceAfterLocations),
                persistentSnapshot);

        // === SELECT BEST PLAN ===
        DeploymentPlan bestPlan = selectBestPlan(
            allPlans, locationDeploys, currentTurn, lifeForce,
            persistentSelection);

        // === EARLY GAME HOLD-BACK CHECK (at the END, like Python) ===
        if (!persistentResponseOverridesEarlyHold(persistentSelection)
                && currentTurn <= RandoConfig.DEPLOY_EARLY_GAME_TURNS
                && bestPlan != null) {
            float planScore = isObjectiveFlipGateFormationPlan(bestPlan)
                ? scoreObjectiveFlipGateFormationPlan(bestPlan, allLocations, currentTurn)
                : scorePlan(bestPlan, allLocations, currentTurn);
            if (!allPlans.isEmpty()) {
                planScore = allPlans.get(0).score;
            }
            if (planScore < RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD) {
                LOG.info("📋 EARLY GAME HOLD: plan score {} < threshold {} - holding back",
                    (int)planScore, RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD);
                bestPlan = createHoldBackPlan(String.format(
                    "Early game (turn %d) - plan score %.0f below threshold %d",
                    currentTurn, planScore, RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD));
            }
        }

        if (bestPlan == null || bestPlan.getInstructions().isEmpty()) {
            bestPlan = createHoldBackPlan("No strategic deployment targets");
        }

        bestPlan = concentratePlannedCharactersForBattle(
                bestPlan, allLocations,
                Math.max(0, forceAfterLocations));

        bestPlan.setTotalForceAvailable(forceAvailable);
        bestPlan.setForceReservedForBattle(battleForceReserve);
        currentPlan = bestPlan;
        if (currentPlan.getCountedDeployRouteKey() != null
                && refreshCountedDeployPackage(game, playerId)
                    == CountedPackageState.RELEASE) {
            currentPlan = createHoldBackPlan(
                    "Counted objective package no longer executable");
            currentPlan.setTotalForceAvailable(forceAvailable);
            currentPlan.setForceReservedForBattle(battleForceReserve);
        }
        lastPlanTurn = currentTurn;
        lastPlanHadActiveFlipGate = hasActiveFlipGate;
        lastPlanHadFlipGateActorInHand = hasFlipGateActorInHand;
        lastPlanPreFlipProgressFingerprint = preFlipProgressFingerprint;
        lastPlanPersistentResponseRevision = persistentRevision;
        contestedAnchorsAtPlanCreation =
                DeployFormationPromisePolicy.snapshotLiveContestedAnchors(
                        game, playerId);
        logFinalPlan(bestPlan);

        return currentPlan;
    }

    /**
     * V308: once the accepted plan can produce a real own-turn site battle,
     * stack every legal, affordable planned ground character there before
     * opening another site. Counted objective packages and persistent
     * responses retain their exact destinations.
     */
    private DeploymentPlan concentratePlannedCharactersForBattle(
            DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int deploymentBudget) {
        if (plan == null || plan.getInstructions().isEmpty()
                || plan.getStrategy() == DeployStrategy.HOLD_BACK
                || plan.getCountedDeployRouteKey() != null
                || plan.getPersistentResponseObligation() != null
                || currentGame == null || currentPlayerId == null
                || currentGame.getGameState() == null
                || currentGame.getModifiersQuerying() == null
                || allLocations == null || allLocations.isEmpty()) {
            return plan;
        }

        GameState gameState = currentGame.getGameState();
        if (!currentPlayerId.equals(gameState.getCurrentPlayerId())
                || gameState.getCurrentPhase() != Phase.DEPLOY) {
            return plan;
        }
        Map<String, AiBoardAnalyzer.LocationAnalysis> analysisById =
                new HashMap<>();
        for (AiBoardAnalyzer.LocationAnalysis analysis : allLocations) {
            if (analysis != null && analysis.location != null) {
                analysisById.put(
                        String.valueOf(analysis.location.getCardId()),
                        analysis);
            }
        }

        List<PlannedBattleCharacter> plannedCharacters =
                new ArrayList<>();
        LinkedHashSet<String> candidateIds = new LinkedHashSet<>();
        for (DeploymentInstruction instruction : plan.getInstructions()) {
            if (instruction.getCardPermanentCardId() == null
                    || instruction.getCardCurrentCardId() == null
                    || instruction.getTargetLocationId() == null
                    || instruction.isVerifiedCrewPackage()
                    || instruction.getAboardShipCardId() != null) {
                continue;
            }
            PhysicalCard card = gameState.findCardByPermanentId(
                    instruction.getCardPermanentCardId());
            PhysicalCard originalTarget = exactLocation(
                    gameState, instruction.getTargetLocationId());
            if (card == null || card.getCardId()
                    != instruction.getCardCurrentCardId()
                    || card.getZone() != Zone.HAND
                    || card.getBlueprint() == null
                    || card.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER
                    || DeployUndercoverModePolicy
                        .predictsUndercoverDeploy(
                            currentGame, card,
                            card.getBlueprint(), "")
                    || originalTarget == null) {
                continue;
            }
            boolean completesObjectiveAtOriginalTarget = false;
            try {
                completesObjectiveAtOriginalTarget =
                        objectiveAnalyzer != null
                        && objectiveAnalyzer
                            .wouldCompletePreFlipRequirementAt(
                                currentGame, currentPlayerId,
                                card, originalTarget);
            } catch (RuntimeException ignored) {
                // Unknown objective projection does not invent a protection.
            }
            plannedCharacters.add(new PlannedBattleCharacter(
                    instruction, card,
                    completesObjectiveAtOriginalTarget));
            candidateIds.add(instruction.getTargetLocationId());
        }
        if (plannedCharacters.isEmpty()) return plan;

        String opponentId = gameState.getOpponent(currentPlayerId);
        if (opponentId == null) return plan;
        for (String candidateId : candidateIds) {
            AiBoardAnalyzer.LocationAnalysis target =
                    analysisById.get(candidateId);
            if (target == null || !target.isSite
                    || target.theirPower <= 0.0f) {
                continue;
            }
            try {
                if (currentGame.getModifiersQuerying()
                        .mayNotInitiateBattleAtLocation(
                            gameState, target.location,
                            currentPlayerId)
                        || currentGame.getModifiersQuerying()
                            .isBattleOccurredAtLocationThisTurn(
                                target.location)) {
                    continue;
                }
            } catch (RuntimeException ignored) {
                continue;
            }

            float battleCost;
            try {
                battleCost = currentGame.getModifiersQuerying()
                        .getInitiateBattleCost(
                            gameState, target.location,
                            currentPlayerId, true);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (!Float.isFinite(battleCost)
                    || Math.ceil(Math.max(0.0f, battleCost))
                        > battleForceReserve) {
                continue;
            }

            List<PlannedBattleCharacter> focusMembers =
                    new ArrayList<>();
            Map<DeploymentInstruction, Integer> focusCosts =
                    new IdentityHashMap<>();
            boolean hasOriginalMember = false;
            for (PlannedBattleCharacter member : plannedCharacters) {
                boolean alreadyTargetsBattle = candidateId.equals(
                        member.instruction().getTargetLocationId());
                if (alreadyTargetsBattle) hasOriginalMember = true;
                if (!alreadyTargetsBattle
                        && member.completesObjectiveAtOriginalTarget()) {
                    continue;
                }
                if (!canDeployPaidDirectly(
                        member.card(), target.location)) {
                    continue;
                }
                Integer exactCost = exactDeployCostAt(
                        member.card(), target.location);
                if (exactCost == null) continue;
                focusMembers.add(member);
                focusCosts.put(member.instruction(), exactCost);
            }
            if (!hasOriginalMember || focusMembers.isEmpty()) continue;

            Set<DeploymentInstruction> focused =
                    Collections.newSetFromMap(new IdentityHashMap<>());
            for (PlannedBattleCharacter member : focusMembers) {
                focused.add(member.instruction());
            }
            int totalPlanCost = 0;
            for (DeploymentInstruction instruction
                    : plan.getInstructions()) {
                if (focused.contains(instruction)) {
                    totalPlanCost += focusCosts.get(instruction);
                } else if (instruction.getTargetLocationId() != null) {
                    totalPlanCost += Math.max(
                            0, instruction.getDeployCost());
                }
            }
            if (totalPlanCost > deploymentBudget) continue;

            float projectedPower = target.ourPower;
            float projectedAbility = target.ourAbility;
            for (PlannedBattleCharacter member : focusMembers) {
                projectedPower += Math.max(
                        0, member.instruction().getPowerContribution());
                projectedAbility += Math.max(
                        0.0f,
                        member.instruction().getAbilityContribution());
            }
            if (projectedAbility
                    < com.gempukku.swccgo.ai.models.common.strategy
                        .FormationSafety.DESTINY_ABILITY_THRESHOLD
                    || gameState.getReserveDeckSize(currentPlayerId) < 1) {
                continue;
            }
            float effectiveDiff = projectedPower - target.theirPower
                    + (projectedAbility - target.theirAbility) * 2.5f;
            boolean reserveHealthy =
                    gameState.getReserveDeckSize(currentPlayerId) >= 3;
            BattleInitiationPolicy.SpecificBattleDecision battle =
                    BattleInitiationPolicy.specificBattle(
                        target.location.getTitle(), projectedPower,
                        target.theirPower, projectedAbility,
                        target.theirAbility, 0.0f, effectiveDiff,
                        false, false, false, reserveHealthy);
            if (!battle.favorable()) continue;

            for (PlannedBattleCharacter member : focusMembers) {
                DeploymentInstruction instruction =
                        member.instruction();
                instruction.setTargetLocationId(candidateId);
                instruction.setTargetLocationName(
                        target.location.getTitle());
                instruction.setPriority(Math.min(
                        instruction.getPriority(), 1));
                instruction.setDeployCost(focusCosts.get(instruction));
                instruction.setReason(
                        "V308 stack before own-turn battle at "
                            + target.location.getTitle());
            }
            plan.markPlannedBattleTarget(
                    candidateId, target.location.getTitle());
            plan.setForceToSpend(totalPlanCost);
            plan.setReason(plan.getReason()
                    + "; stack before battle at "
                    + target.location.getTitle());
            LOG.warn("V308 BATTLE STACK: concentrated {} planned character(s) at {} (projected power {}/{}, ability {}/{}, cost {}, reserve {})",
                    focusMembers.size(), target.location.getTitle(),
                    (int) projectedPower, (int) target.theirPower,
                    (int) projectedAbility, (int) target.theirAbility,
                    totalPlanCost, battleForceReserve);
            return plan;
        }
        return plan;
    }

    private record PlannedBattleCharacter(
            DeploymentInstruction instruction,
            PhysicalCard card,
            boolean completesObjectiveAtOriginalTarget) {
    }

    private boolean hasActiveFlipGateLocation(
            SwccgGame game, String playerId) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || objectiveAnalyzer.isFlipped()
                || !objectiveAnalyzer.hasFlipGateActorRequirement()
                || game == null || game.getGameState() == null) {
            return false;
        }
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (locations == null) return false;
        for (PhysicalCard location : locations) {
            if (location != null && objectiveAnalyzer.isFlipGateLocation(
                    game, playerId, location)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlipGateActorInHand(
            SwccgGame game, String playerId) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || objectiveAnalyzer.isFlipped()
                || !objectiveAnalyzer.hasFlipGateActorRequirement()
                || game == null || game.getGameState() == null) {
            return false;
        }
        List<PhysicalCard> hand = game.getGameState().getHand(playerId);
        if (hand == null) return false;
        for (PhysicalCard card : hand) {
            if (objectiveAnalyzer.matchesFlipGateActorRequirement(
                    game, playerId, card)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // FORCE DRAIN GAP CALCULATION (Item #3)
    // =========================================================================

    /**
     * Calculate force drain economy.
     *
     * Ported from Python _calculate_force_drain_gap().
     */
    private DrainGapResult calculateForceDrainGap(List<AiBoardAnalyzer.LocationAnalysis> locations) {
        int theirDrain = 0;
        int ourDrain = 0;
        List<AiBoardAnalyzer.LocationAnalysis> bleedLocations = new ArrayList<>();

        for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
            // Opponent drains us: they have presence, we don't
            // They drain for OUR icons (ourForceIcons)
            if (loc.theirPower > 0 && loc.ourPower == 0 && loc.ourForceIcons > 0) {
                theirDrain += loc.ourForceIcons;

                // Add to bleed locations if enemy power is low enough to contest
                if (loc.theirPower <= RandoConfig.LOW_ENEMY_THRESHOLD) {
                    bleedLocations.add(loc);
                    String domain = loc.isSpace() ? "space" : "ground";
                    LOG.debug("   🩸 BLEED ({}, contestable): {} - they drain {} icons, enemy power {}",
                        domain, loc.location.getTitle(), loc.ourForceIcons, (int)loc.theirPower);
                }
            }

            // We drain opponent: we have presence, they don't
            if (loc.ourPower > 0 && loc.theirPower == 0 && loc.theirForceIcons > 0) {
                ourDrain += loc.theirForceIcons;
                LOG.debug("   💧 DRAIN: {} - we drain {} icons", loc.location.getTitle(), loc.theirForceIcons);
            }
        }

        return new DrainGapResult(theirDrain, ourDrain, ourDrain - theirDrain, bleedLocations);
    }

    // =========================================================================
    // DYNAMIC THRESHOLD CALCULATION (Item #4, #5)
    // =========================================================================

    /**
     * Calculate dynamic deploy threshold based on game state.
     *
     * Ported from Python _get_dynamic_threshold().
     */
    private int getDynamicThreshold(List<AiBoardAnalyzer.LocationAnalysis> locations,
                                     boolean isSpace, int turnNumber, int lifeForce) {
        int threshold = deployThreshold;
        String domain = isSpace ? "space" : "ground";

        // EARLY GAME RELAXATION: Before turn 4 with no contested locations
        boolean earlyGameRelaxed = false;
        if (turnNumber < 4) {
            boolean hasContested = false;
            for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                if (loc.ourPower <= 0 || loc.theirPower <= 0) continue;

                if (isSpace && loc.isSpace()) {
                    hasContested = true;
                    break;
                } else if (!isSpace && loc.isGround()) {
                    hasContested = true;
                    break;
                }
            }

            if (!hasContested) {
                // Check for react threats
                boolean hasReactThreat = false;
                for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                    boolean matchesDomain = isSpace ? loc.isSpace() : loc.isGround();
                    if (matchesDomain && loc.theirPower >= RandoConfig.REACT_THREAT_THRESHOLD) {
                        hasReactThreat = true;
                        break;
                    }
                }

                if (!hasReactThreat) {
                    threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER, threshold - 2);
                    earlyGameRelaxed = true;
                }
            }
        }

        // LIFE FORCE DECAY: Lower threshold when losing badly
        int lifeForceDecay = 0;
        if (lifeForce < 10) {
            lifeForceDecay = 2;
            threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER - 1, threshold - lifeForceDecay);
        } else if (lifeForce < 20) {
            lifeForceDecay = 1;
            threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER, threshold - lifeForceDecay);
        } else if (lifeForce < 30) {
            lifeForceDecay = 1;
            threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER, threshold - lifeForceDecay);
        }

        LOG.debug("   📊 Dynamic threshold ({}): {} (early={}, life_decay={})",
            domain, threshold, earlyGameRelaxed, lifeForceDecay);

        return threshold;
    }

    // =========================================================================
    // LOCATION CATEGORIZATION (Items #15, #16, #17)
    // =========================================================================

    /**
     * Categorize all locations into strategic groups.
     */
    private LocationCategories categorizeLocations(List<AiBoardAnalyzer.LocationAnalysis> locations,
                                                    String playerId) {
        LocationCategories cats = new LocationCategories();
        // V201 ADJUSTED 2026-08-08 (passivity fix, m01683): affordable-wave power from
        // hand, computed once — opens the 'attack' category below. FINAL PLAN: attack was
        // chosen 0 times in 225 logged plans: the LOW_ENEMY_THRESHOLD cap plus the
        // our-icons gate made 'attack' unreachable against any real enemy stack.
        float attackWave = attackWaveProjection(playerId);

        for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
            // Skip locations we can't deploy to (no icons)
            boolean hasOurIcons = loc.ourForceIcons > 0;
            boolean hasTheirIcons = loc.theirForceIcons > 0;

            if (loc.ourPower > 0 && loc.theirPower > 0) {
                // CONTESTED - both have presence
                if (loc.ourPower < loc.theirPower) {
                    cats.losingLocations.add(loc);
                } else if (loc.ourPower > loc.theirPower) {
                    cats.winningLocations.add(loc);
                    // Check if below reinforcement target
                    if (loc.ourPower < RandoConfig.REINFORCE_TARGET_POWER) {
                        cats.weakPresenceLocations.add(loc);
                    }
                }

                // Check if crushable (we have big advantage)
                if (loc.getPowerAdvantage() >= RandoConfig.BATTLE_FAVORABLE_THRESHOLD) {
                    cats.crushableLocations.add(loc);
                }
            // V201 ADJUSTED 2026-08-08 (passivity fix, m01683): enemy-held arm no longer
            // requires our icons — a dominance-wave attack candidate needs no drain there.
            // } else if (loc.theirPower > 0 && loc.ourPower == 0 && hasOurIcons) {
            } else if (loc.theirPower > 0 && loc.ourPower == 0) {
                if (hasOurIcons) {
                    // BLEEDING - they have presence, we don't, but we have icons (they drain us)
                    cats.bleedLocations.add(loc);
                    if (loc.theirPower <= RandoConfig.LOW_ENEMY_THRESHOLD) {
                        cats.attackTargets.add(loc);  // Low enemy = attack target
                    }
                }
                // V201 ADJUSTED 2026-08-08 (passivity fix, m01683): an ENEMY-held ground
                // location is an attack candidate when the affordable hand wave projects
                // 2x their weapon-adjusted power (FormationSafety.DOMINANCE_MULTIPLE, the
                // V172 standard — no new constants). generateAttackPlan still re-verifies
                // deployability and its +BATTLE_FAVORABLE_THRESHOLD combo goal.
                // GATE ADJUSTED 2026-08-08 (passivity fix, m01683/panel): isGround() is
                // subtype != SYSTEM, which admitted space SECTORS where characters cannot
                // deploy, and required no battle/drain value. Require a SITE that is a
                // battleground OR carries at least one of our force icons.
                // if (loc.isGround() && !cats.attackTargets.contains(loc)) {
                if (loc.isSite && (loc.isBattleground || loc.ourForceIcons > 0)
                        && !cats.attackTargets.contains(loc)) {
                    float attackOppEff = loc.theirPower
                        + attackOppWeaponBonus(loc.location, playerId);
                    if (attackWave >= com.gempukku.swccgo.ai.models.common.strategy
                            .FormationSafety.DOMINANCE_MULTIPLE * attackOppEff) {
                        cats.attackTargets.add(loc);
                        LOG.warn("📋 ATTACK CANDIDATE: {} enemy power {} (eff {}) — hand wave {} projects ≥2x, pile on",
                            loc.location.getTitle(), (int) loc.theirPower,
                            (int) attackOppEff, (int) attackWave);
                    }
                }
            } else if (loc.theirPower == 0 && loc.ourPower == 0 && hasTheirIcons) {
                // ESTABLISH - neither has presence but they have icons
                cats.establishTargets.add(loc);
            } else if (loc.ourPower > 0 && loc.theirPower == 0 && hasTheirIcons) {
                // DRAINING - we control and can drain them
                cats.drainingLocations.add(loc);
                // Check if we're below reinforcement target
                if (loc.ourPower < RandoConfig.REINFORCE_TARGET_POWER) {
                    cats.weakPresenceLocations.add(loc);
                }
            } else if (loc.ourPower > 0 && loc.theirPower == 0) {
                // CONTROLLED - we have presence, they don't
                if (loc.ourPower < RandoConfig.REINFORCE_TARGET_POWER) {
                    cats.weakPresenceLocations.add(loc);
                }
            }
        }

        return cats;
    }

    /** V201 (2026-08-08, passivity fix m01683): affordable-wave projection for attack
     *  classification — top-power hand CHARACTERS within the current force pile, max 3
     *  bodies (the plan-group cap). Deployability and the combo goal are re-verified by
     *  generateAttackPlan before any instruction is emitted. */
    private float attackWaveProjection(String playerId) {
        float wave = 0f;
        try {
            GameState gs = currentGame.getGameState();
            int budget = gs.getForcePileSize(playerId);
            List<CardInfo> waveChars = new ArrayList<>();
            for (PhysicalCard h : gs.getHand(playerId)) {
                if (h == null || h.getBlueprint() == null) continue;
                CardInfo info = new CardInfo(h);
                if (info.isCharacter) waveChars.add(info);
            }
            waveChars.sort(Comparator.comparingInt((CardInfo c) -> c.power).reversed());
            int bodies = 0;
            for (CardInfo c : waveChars) {
                if (bodies >= 3) break;
                if (c.cost > budget) continue;
                wave += c.power;
                budget -= c.cost;
                bodies++;
            }
        } catch (Exception e) { /* fail-open: 0 */ }
        return wave;
    }

    /** V201 (2026-08-08, passivity fix m01683): opponent weapon-adjustment for the attack
     *  classifier — the shared FormationSafety typed-weapon read. */
    private float attackOppWeaponBonus(PhysicalCard location, String playerId) {
        try {
            GameState gs = currentGame.getGameState();
            return com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                .weaponBonusAt(gs, location, gs.getOpponent(playerId));
        } catch (Exception e) {
            return 0f;
        }
    }

    private float projectedForfeitTotal(List<PhysicalCard> cards) {
        float total = 0.0f;
        if (cards == null) return total;
        for (PhysicalCard card : cards) {
            try {
                if (card == null || card.getBlueprint() == null
                        || !card.getBlueprint().hasForfeitAttribute()) {
                    continue;
                }
                Float forfeit = card.getBlueprint().getForfeit();
                if (forfeit != null && Float.isFinite(forfeit)
                        && forfeit > 0.0f) {
                    total += forfeit;
                }
            } catch (Exception ignored) {
                // Unknown forfeit remains zero, matching MovePredicates.
            }
        }
        return total;
    }

    private OptimalCombination findWinnableSiteCombination(
            List<CardInfo> cards, int budget,
            AiBoardAnalyzer.LocationAnalysis location) {
        int cleanWinPower = Math.max(0, (int) Math.ceil(location.theirPower));
        Map<SiteWaveState, SiteWaveCandidate> states = new HashMap<>();
        SiteWaveCandidate empty = SiteWaveCandidate.empty();
        states.put(SiteWaveState.of(empty, cleanWinPower), empty);

        for (CardInfo card : cards) {
            if (card == null || card.cost > budget) continue;
            float forfeit = projectedForfeitTotal(List.of(card.card));
            Map<SiteWaveState, SiteWaveCandidate> expanded =
                new HashMap<>(states);
            for (SiteWaveCandidate existing : states.values()) {
                if (existing.totalCost + card.cost > budget) continue;
                SiteWaveCandidate candidate = existing.add(card, forfeit);
                SiteWaveState state = SiteWaveState.of(
                    candidate, cleanWinPower);
                SiteWaveCandidate incumbent = expanded.get(state);
                if (incumbent == null
                        || candidate.preferredTo(
                            incumbent, cleanWinPower)) {
                    expanded.put(state, candidate);
                }
            }
            states = expanded;
        }

        int contestPowerFloor = Math.max(0,
            (int) Math.ceil(location.theirPower - MovePredicates.POWER_GAP_MAX));
        List<SiteWaveCandidate> frontier =
            new ArrayList<>(states.values());
        frontier.sort(Comparator
            .comparingInt((SiteWaveCandidate candidate) -> candidate.cards.size())
            .thenComparingInt(candidate -> candidate.totalCost)
            .thenComparingInt(candidate -> -candidate.totalPower)
            .thenComparingInt(candidate -> -candidate.totalAbility)
            .thenComparingDouble(candidate -> candidate.totalForfeit));

        for (SiteWaveCandidate candidate : frontier) {
            if (candidate.cards.isEmpty()
                    || candidate.totalPower < contestPowerFloor) {
                continue;
            }
            if (MovePredicates.canWinAt(
                    currentGame,
                    currentGame != null ? currentGame.getGameState() : null,
                    currentPlayerId,
                    location.location,
                    candidate.totalPower,
                    candidate.totalAbility,
                    candidate.totalForfeit)) {
                return candidate.toOptimalCombination();
            }
        }
        return OptimalCombination.empty();
    }

    private static class SiteWaveCandidate {
        private final List<PhysicalCard> cards;
        private final int totalPower;
        private final int totalCost;
        private final int totalAbility;
        private final float totalForfeit;

        private SiteWaveCandidate(List<PhysicalCard> cards, int totalPower,
                                  int totalCost, int totalAbility,
                                  float totalForfeit) {
            this.cards = cards;
            this.totalPower = totalPower;
            this.totalCost = totalCost;
            this.totalAbility = totalAbility;
            this.totalForfeit = totalForfeit;
        }

        private static SiteWaveCandidate empty() {
            return new SiteWaveCandidate(
                new ArrayList<>(), 0, 0, 0, 0.0f);
        }

        private SiteWaveCandidate add(CardInfo card, float forfeit) {
            List<PhysicalCard> selected = new ArrayList<>(cards);
            selected.add(card.card);
            return new SiteWaveCandidate(
                selected,
                totalPower + card.power,
                totalCost + card.cost,
                totalAbility + card.ability,
                totalForfeit + forfeit);
        }

        private boolean preferredTo(
                SiteWaveCandidate other, int cleanWinPower) {
            float risk = totalPower >= cleanWinPower && totalAbility >= 4
                ? 0.0f : totalForfeit;
            float otherRisk = other.totalPower >= cleanWinPower
                    && other.totalAbility >= 4
                ? 0.0f : other.totalForfeit;
            int riskComparison = Float.compare(risk, otherRisk);
            if (riskComparison != 0) return riskComparison < 0;
            if (cards.size() != other.cards.size()) {
                return cards.size() < other.cards.size();
            }
            if (totalPower != other.totalPower) {
                return totalPower > other.totalPower;
            }
            return totalAbility > other.totalAbility;
        }

        private OptimalCombination toOptimalCombination() {
            return new OptimalCombination(
                cards, totalPower, totalCost, totalAbility,
                totalAbility >= RandoConfig.ABILITY_THRESHOLD, true);
        }
    }

    private record SiteWaveState(int cost, int power, int ability) {
        private static SiteWaveState of(
                SiteWaveCandidate candidate, int cleanWinPower) {
            return new SiteWaveState(
                candidate.totalCost,
                Math.min(candidate.totalPower, cleanWinPower),
                Math.min(candidate.totalAbility, 4));
        }
    }

    // =========================================================================
    // OPTIMAL COMBINATION FINDING (Item #13)
    // =========================================================================

    /**
     * Find optimal combination of cards within budget.
     *
     * Ported from Python _find_optimal_combination().
     */
    private OptimalCombination findOptimalCombination(List<CardInfo> cards, int budget,
                                                       int powerGoal, boolean mustExceed) {
        if (cards.isEmpty() || budget <= 0) {
            return OptimalCombination.empty();
        }

        // Filter to affordable cards
        List<CardInfo> affordable = cards.stream()
            .filter(c -> c.cost <= budget)
            .collect(Collectors.toList());

        if (affordable.isEmpty()) {
            return OptimalCombination.empty();
        }

        // For small card counts, try all combinations
        if (affordable.size() <= 8) {
            return findOptimalBruteForce(affordable, budget, powerGoal, mustExceed);
        }

        // For larger hands, use greedy approach
        return findOptimalGreedy(affordable, budget, powerGoal, mustExceed);
    }

    /**
     * Brute force optimal combination (for small hand sizes).
     */
    private OptimalCombination findOptimalBruteForce(List<CardInfo> cards, int budget,
                                                      int powerGoal, boolean mustExceed) {
        List<PhysicalCard> bestCombo = new ArrayList<>();
        int bestPower = 0;
        int bestCost = Integer.MAX_VALUE;
        int bestAbility = 0;
        boolean bestHasAbility = false;
        boolean bestAchievesGoal = false;

        // Try all subset sizes
        for (int size = 1; size <= cards.size(); size++) {
            for (List<CardInfo> combo : combinations(cards, size)) {
                int totalCost = combo.stream().mapToInt(c -> c.cost).sum();
                if (totalCost > budget) continue;

                int totalPower = combo.stream().mapToInt(c -> c.power).sum();
                int totalAbility = combo.stream().mapToInt(c -> c.ability).sum();
                boolean hasAbility = totalAbility >= RandoConfig.ABILITY_THRESHOLD;

                // Ability compensation for battles
                int effectiveGoal = powerGoal;
                if (!hasAbility && mustExceed) {
                    effectiveGoal = powerGoal + RandoConfig.ABILITY_POWER_COMPENSATION;
                }

                boolean achievesGoal = mustExceed ?
                    totalPower > effectiveGoal : totalPower >= effectiveGoal;

                // Check if this is better
                boolean isBetter = false;
                if (achievesGoal && !bestAchievesGoal) {
                    isBetter = true;
                } else if (achievesGoal == bestAchievesGoal) {
                    if (hasAbility && !bestHasAbility) {
                        isBetter = true;
                    } else if (hasAbility == bestHasAbility) {
                        if (achievesGoal) {
                            if (mustExceed && totalPower > bestPower) isBetter = true;
                            else if (!mustExceed && totalCost < bestCost) isBetter = true;
                        } else if (totalPower > bestPower) {
                            isBetter = true;
                        }
                    }
                }

                if (isBetter) {
                    bestCombo = combo.stream().map(c -> c.card).collect(Collectors.toList());
                    bestPower = totalPower;
                    bestCost = totalCost;
                    bestAbility = totalAbility;
                    bestHasAbility = hasAbility;
                    bestAchievesGoal = achievesGoal;
                }
            }
        }

        return new OptimalCombination(bestCombo, bestPower,
            bestCost == Integer.MAX_VALUE ? 0 : bestCost,
            bestAbility, bestHasAbility, bestAchievesGoal);
    }

    /**
     * Greedy optimal combination (for larger hand sizes).
     */
    private OptimalCombination findOptimalGreedy(List<CardInfo> cards, int budget,
                                                  int powerGoal, boolean mustExceed) {
        // Sort by efficiency (power/cost) with ability bonus
        List<CardInfo> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> {
            float scoreA = a.getValueRatio() + (a.ability >= 3 ? a.ability * 0.5f : 0);
            float scoreB = b.getValueRatio() + (b.ability >= 3 ? b.ability * 0.5f : 0);
            return Float.compare(scoreB, scoreA);
        });

        List<PhysicalCard> selected = new ArrayList<>();
        int totalPower = 0;
        int totalCost = 0;
        int totalAbility = 0;

        for (CardInfo card : sorted) {
            if (totalCost + card.cost <= budget) {
                selected.add(card.card);
                totalPower += card.power;
                totalCost += card.cost;
                totalAbility += card.ability;

                // Check if we've met the goal with ability
                boolean hasAbility = totalAbility >= RandoConfig.ABILITY_THRESHOLD;
                int effectiveGoal = hasAbility ? powerGoal :
                    powerGoal + (mustExceed ? RandoConfig.ABILITY_POWER_COMPENSATION : 0);

                if (mustExceed ? totalPower > effectiveGoal : totalPower >= effectiveGoal) {
                    break;  // Goal met
                }
            }
        }

        boolean hasAbility = totalAbility >= RandoConfig.ABILITY_THRESHOLD;
        int effectiveGoal = hasAbility ? powerGoal :
            powerGoal + (mustExceed ? RandoConfig.ABILITY_POWER_COMPENSATION : 0);
        boolean achievesGoal = mustExceed ? totalPower > effectiveGoal : totalPower >= effectiveGoal;

        return new OptimalCombination(selected, totalPower, totalCost,
            totalAbility, hasAbility, achievesGoal);
    }

    // =========================================================================
    // PLAN GENERATION (Items #1, #9)
    // =========================================================================

    /**
     * Plan location deploys (always prioritized).
     * V22: Sort objective-relevant locations first.
     * V22: Throttle location deploys if force gen >= 15 AND reserve < 7.
     */
    private List<CardInfo> planLocationDeploys(List<CardInfo> locations, int forceAvailable) {
        List<CardInfo> deploys = new ArrayList<>();
        if (locations.isEmpty()) return deploys;

        // V22: Throttle check - skip ALL location deploys if already generating
        // 15+ force AND reserve deck is dangerously low (< 7 cards)
        if (currentGame != null && currentPlayerId != null) {
            try {
                GameState gs = currentGame.getGameState();
                int reserveSize = gs.getReserveDeckSize(currentPlayerId);
                int forceGen = gs.getForcePileSize(currentPlayerId)
                    + gs.getReserveDeckSize(currentPlayerId); // rough proxy for generation capacity
                // More accurate: count our force icons on locations we control
                int ourForceIcons = 0;
                String opponentId = gs.getOpponent(currentPlayerId);
                List<AiBoardAnalyzer.LocationAnalysis> locs = AiBoardAnalyzer.analyzeAllLocations(
                    currentGame, currentPlayerId, opponentId, currentSide);
                for (AiBoardAnalyzer.LocationAnalysis la : locs) {
                    ourForceIcons += la.ourForceIcons;
                }
                if (ourForceIcons >= 15 && reserveSize < 7) {
                    LOG.warn("V22 LOCATION THROTTLE: Force icons={}, reserve={} - skipping location deploys to conserve reserve",
                        ourForceIcons, reserveSize);
                    return deploys;
                }
            } catch (Exception e) {
                LOG.debug("V22 LOCATION THROTTLE: Error checking force/reserve: {}", e.getMessage());
            }
        }

        // Cost remains primary. The action evaluator owns objective preference.
        List<CardInfo> sorted = new ArrayList<>(locations);
        sorted.sort((a, b) -> {
            int cost = Integer.compare(a.cost, b.cost);
            if (cost != 0) return cost;
            return a.name.compareToIgnoreCase(b.name);
        });

        int remaining = forceAvailable;
        for (CardInfo loc : sorted) {
            if (loc.cost <= remaining) {
                deploys.add(loc);
                remaining -= loc.cost;
                boolean objRelevant = objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()
                    && objectiveAnalyzer.isObjectiveRelevantLocation(loc.name);
                LOG.info("   📍 Location deploy: {} (cost {}){}", loc.name, loc.cost,
                    objRelevant ? " ⭐ OBJECTIVE-RELEVANT" : "");
            }
        }

        return deploys;
    }

    /**
     * Generate ground deployment plans.
     */
    private List<ScoredPlan> generateGroundPlans(List<CardInfo> characters, List<CardInfo> vehicles,
                                                  LocationCategories categories, int forceAvailable,
                                                  int objectiveFormationForceAvailable,
                                                  int threshold, List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                  int turn, DrainGapResult drainGap) {
        List<ScoredPlan> plans = new ArrayList<>();

        // Get ground cards
        List<CardInfo> groundCards = new ArrayList<>(characters);
        groundCards.addAll(vehicles);

        if (groundCards.isEmpty()) {
            return plans;
        }

        for (DeploymentPlan countedTypePackage
                : generateCountedTypeCompletionPlans(
                    characters, allLocations,
                    objectiveFormationForceAvailable)) {
            float score = scoreObjectiveFlipGateFormationPlan(
                    countedTypePackage, allLocations, turn);
            plans.add(new ScoredPlan(
                    countedTypePackage, score,
                    "ground_objective_counted_type_completion"));
        }

        DeploymentPlan bunkerGarrisonPlan =
                generateEopBunkerGarrisonPlan(
                        characters, allLocations,
                        objectiveFormationForceAvailable);
        if (!bunkerGarrisonPlan.getInstructions().isEmpty()) {
            float score = scorePlan(
                    bunkerGarrisonPlan, allLocations, turn,
                    DeployPlanRankingPolicy.evaluateEndorAdjustment(
                        "eop-bunker-garrison", "V193-eop-bunker-garrison-plan",
                        EndorOperationsTacticalPolicy.BUNKER_GARRISON_PLAN_BONUS,
                        "EOP pre-flip Bunker garrison preference"));
            plans.add(new ScoredPlan(
                    bunkerGarrisonPlan, score,
                    "ground_eop_bunker_garrison"));
        }

        // V297: A control-gated actor objective needs a formation, not a sacrificial
        // one-body score. Build the exact actor-plus-buddy plan before generic siting.
        DeploymentPlan countedOperativeFormation =
            generateCountedOperativeFormationPlan(
                characters, allLocations,
                objectiveFormationForceAvailable);
        if (countedOperativeFormation != null
                && !countedOperativeFormation.getInstructions().isEmpty()) {
            float score = scoreObjectiveFlipGateFormationPlan(
                countedOperativeFormation, allLocations, turn);
            plans.add(new ScoredPlan(
                countedOperativeFormation, score,
                "ground_objective_counted_operative_formation"));
        }

        DeploymentPlan flipGateFormation = generateFlipGateFormationPlan(
            characters, allLocations, objectiveFormationForceAvailable);
        if (flipGateFormation != null && !flipGateFormation.getInstructions().isEmpty()) {
            float score = scoreObjectiveFlipGateFormationPlan(
                flipGateFormation, allLocations, turn);
            plans.add(new ScoredPlan(
                flipGateFormation, score, "ground_objective_flip_gate_formation"));
        }

        // Plan 1: Stop bleeding (highest priority if losing drain war)
        if (drainGap.isLosing() && !categories.bleedLocations.isEmpty()) {
            DeploymentPlan bleedPlan = generateStopBleedingPlan(
                groundCards, categories.bleedLocations.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, threshold, "ground");
            if (!bleedPlan.getInstructions().isEmpty()) {
                float score = scorePlan(bleedPlan, allLocations, turn);
                plans.add(new ScoredPlan(bleedPlan, score, "ground_bleed"));
            }
        }

        // Plan 2: Reinforce losing positions
        if (!categories.losingLocations.isEmpty()) {
            DeploymentPlan reinforcePlan = generateReinforcePlan(
                groundCards, categories.losingLocations.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, "ground");
            if (!reinforcePlan.getInstructions().isEmpty()) {
                float score = scorePlan(reinforcePlan, allLocations, turn,
                    endorPostFlipPlanAdjustment(reinforcePlan, true, false));
                plans.add(new ScoredPlan(reinforcePlan, score, "ground_reinforce"));
            }
        }

        // Plan 3: Establish at uncontested locations
        if (!categories.establishTargets.isEmpty()) {
            DeploymentPlan establishPlan = generateLateGroundEstablishPlan(
                groundCards,
                categories.establishTargets.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, threshold, "ground", turn);
            if (!establishPlan.getInstructions().isEmpty()) {
                float score = scorePlan(establishPlan, allLocations, turn,
                    endorPostFlipPlanAdjustment(establishPlan, false, true));
                plans.add(new ScoredPlan(establishPlan, score, "ground_establish"));
            }
        }

        // Plan 4: Attack enemy positions
        if (!categories.attackTargets.isEmpty()) {
            DeploymentPlan attackPlan = generateAttackPlan(
                groundCards, categories.attackTargets.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, "ground");
            if (!attackPlan.getInstructions().isEmpty()) {
                float score = scorePlan(attackPlan, allLocations, turn,
                    endorPostFlipPlanAdjustment(attackPlan, true, false));
                plans.add(new ScoredPlan(attackPlan, score, "ground_attack"));
            }
        }

        return plans;
    }

    /**
     * Build exact, fully funded packages for typed counted objective routes.
     * On-table counts concentrate at one site. Location-counting routes open
     * only the distinct sites required by the card and give every weak first
     * body a funded local buddy.
     */
    private List<DeploymentPlan> generateCountedTypeCompletionPlans(
            List<CardInfo> characters,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || currentGame == null || currentPlayerId == null
                || characters == null || characters.isEmpty()
                || forceAvailable <= 0) {
            return List.of();
        }
        List<ObjectiveAnalyzer.CountedDeployRoute> routes =
                objectiveAnalyzer.openCountedDeployCompletionRoutes(
                    currentGame, currentPlayerId);
        if (routes.isEmpty()) return List.of();

        List<AiBoardAnalyzer.LocationAnalysis> targets =
                allLocations.stream()
                    .filter(Objects::nonNull)
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .filter(loc -> loc.location != null)
                    .sorted(countedTypeTargetComparator())
                    .toList();
        List<DeploymentPlan> result = new ArrayList<>();
        for (ObjectiveAnalyzer.CountedDeployRoute route : routes) {
            if (route.countsCardsOnTable()) {
                for (AiBoardAnalyzer.LocationAnalysis target : targets) {
                    DeploymentPlan plan = buildCountedOnTablePackage(
                            route, characters, target, forceAvailable);
                    if (plan != null) result.add(plan);
                }
            } else {
                DeploymentPlan plan = buildCountedLocationPackage(
                        route, characters, targets, forceAvailable);
                if (plan != null) result.add(plan);
            }
        }
        return result;
    }

    private Comparator<AiBoardAnalyzer.LocationAnalysis>
            countedTypeTargetComparator() {
        return Comparator
            .comparingInt((AiBoardAnalyzer.LocationAnalysis loc) ->
                loc.ourCardCount > 0 ? 0 : 1)
            .thenComparingInt(loc ->
                currentPlayerId.equals(loc.location.getOwner()) ? 0 : 1)
            .thenComparing((AiBoardAnalyzer.LocationAnalysis loc) ->
                -projectedDrainAt(loc.location))
            .thenComparingInt(loc -> -loc.theirForceIcons)
            .thenComparing(loc -> loc.location.getTitle(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private DeploymentPlan buildCountedOnTablePackage(
            ObjectiveAnalyzer.CountedDeployRoute route,
            List<CardInfo> characters,
            AiBoardAnalyzer.LocationAnalysis target,
            int forceAvailable) {
        List<CardInfo> matching = characters.stream()
            .filter(card -> objectiveAnalyzer.matchesCountedDeployCandidateAt(
                currentGame, currentPlayerId, route.key(),
                card.card, target.location))
            .filter(card -> canDeployPaidDirectly(card.card, target.location))
            .filter(card -> exactDeployCostAt(
                card.card, target.location) != null)
            .sorted(Comparator
                .comparingInt((CardInfo card) ->
                    exactDeployCostAt(card.card, target.location))
                .thenComparingInt(card -> -(card.power + card.ability))
                .thenComparingInt(card ->
                    card.card.getPermanentCardId()))
            .limit(route.missingCount())
            .collect(Collectors.toCollection(ArrayList::new));
        if (matching.size() != route.missingCount()) return null;

        List<CardInfo> packet = new ArrayList<>(matching);
        int cost = exactPacketCost(packet, target.location);
        if (cost < 0 || cost > forceAvailable) return null;
        if (!objectivePacketIsSafe(target, packet)) {
            CardInfo buddy = cheapestSafeBuddy(
                    characters, packet, target, forceAvailable - cost,
                    card -> false);
            if (buddy == null) return null;
            packet.add(buddy);
            cost = exactPacketCost(packet, target.location);
        }
        if (cost < 0 || cost > forceAvailable
                || !objectivePacketIsSafe(target, packet)) {
            return null;
        }
        return countedTypePlan(route, packet, target, cost);
    }

    private DeploymentPlan buildCountedLocationPackage(
            ObjectiveAnalyzer.CountedDeployRoute route,
            List<CardInfo> characters,
            List<AiBoardAnalyzer.LocationAnalysis> targets,
            int forceAvailable) {
        List<CardInfo> available = new ArrayList<>(characters);
        DeploymentPlan plan = new DeploymentPlan(
                DeployStrategy.REINFORCE,
                "Objective counted-type completion: "
                    + route.key().ruleId());
        int remaining = forceAvailable;
        int completedLocations = 0;
        int priority = 1;
        Predicate<CardInfo> preserveForObjective = card ->
            targets.stream().anyMatch(candidateTarget ->
                objectiveAnalyzer.matchesCountedDeployCandidateAt(
                    currentGame, currentPlayerId, route.key(),
                    card.card, candidateTarget.location));

        for (AiBoardAnalyzer.LocationAnalysis target : targets) {
            if (completedLocations >= route.missingCount()) break;
            int actorsNeeded = objectiveAnalyzer.countedDeployActorsNeededAt(
                    currentGame, currentPlayerId,
                    route.key(), target.location);
            if (actorsNeeded <= 0) continue;
            List<CardInfo> actors = available.stream()
                .filter(card -> objectiveAnalyzer
                    .matchesCountedDeployCandidateAt(
                        currentGame, currentPlayerId, route.key(),
                        card.card, target.location))
                .filter(card -> canDeployPaidDirectly(
                    card.card, target.location))
                .filter(card -> exactDeployCostAt(
                    card.card, target.location) != null)
                .sorted(Comparator
                    .comparingInt((CardInfo card) ->
                        exactDeployCostAt(card.card, target.location))
                    .thenComparingInt(card ->
                        -(card.power + card.ability)))
                .limit(actorsNeeded)
                .collect(Collectors.toCollection(ArrayList::new));
            if (actors.size() != actorsNeeded) continue;

            List<CardInfo> packet = new ArrayList<>(actors);
            int packetCost = exactPacketCost(packet, target.location);
            if (packetCost < 0 || packetCost > remaining) continue;
            if (!objectivePacketIsSafe(target, packet)) {
                CardInfo buddy = cheapestSafeBuddy(
                        available, packet, target,
                        remaining - packetCost,
                        preserveForObjective);
                if (buddy == null) continue;
                packet.add(buddy);
                packetCost = exactPacketCost(packet, target.location);
            }
            if (packetCost < 0 || packetCost > remaining
                    || !objectivePacketIsSafe(target, packet)) {
                continue;
            }

            for (CardInfo card : packet) {
                Integer exactCost = exactDeployCostAt(
                        card.card, target.location);
                addCardToPlan(plan, card.card, target, priority++,
                        "Complete typed objective package at "
                            + target.location.getTitle(), exactCost);
            }
            available.removeAll(packet);
            remaining -= packetCost;
            completedLocations++;
        }
        if (completedLocations != route.missingCount()) return null;
        plan.setForceToSpend(forceAvailable - remaining);
        plan.markCountedDeployPackage(route.key());
        return plan;
    }

    private DeploymentPlan countedTypePlan(
            ObjectiveAnalyzer.CountedDeployRoute route,
            List<CardInfo> packet,
            AiBoardAnalyzer.LocationAnalysis target,
            int cost) {
        DeploymentPlan plan = new DeploymentPlan(
                DeployStrategy.REINFORCE,
                "Objective counted-type completion: "
                    + route.key().ruleId());
        int priority = 1;
        for (CardInfo card : packet) {
            Integer exactCost = exactDeployCostAt(
                    card.card, target.location);
            addCardToPlan(plan, card.card, target, priority++,
                    "Complete typed objective package at "
                        + target.location.getTitle(), exactCost);
        }
        plan.setForceToSpend(cost);
        plan.markCountedDeployPackage(route.key());
        return plan;
    }

    private int exactPacketCost(
            List<CardInfo> packet, PhysicalCard target) {
        int total = 0;
        for (CardInfo card : packet) {
            Integer cost = exactDeployCostAt(card.card, target);
            if (cost == null) return -1;
            total += cost;
        }
        return total;
    }

    private CardInfo cheapestSafeBuddy(
            List<CardInfo> candidates, List<CardInfo> packet,
            AiBoardAnalyzer.LocationAnalysis target, int budget,
            Predicate<CardInfo> preserveForObjective) {
        return candidates.stream()
            .filter(card -> !packet.contains(card))
            .filter(card -> card.isCharacter && !card.card.isUndercover())
            .filter(card -> !DeployUndercoverModePolicy
                .isUndercoverDeploy(currentGame, card.card,
                    card.card.getBlueprint(), ""))
            .filter(card -> canDeployPaidDirectly(
                card.card, target.location))
            .filter(card -> {
                Integer cost = exactDeployCostAt(
                        card.card, target.location);
                if (cost == null || cost > budget) return false;
                List<CardInfo> withBuddy = new ArrayList<>(packet);
                withBuddy.add(card);
                return objectivePacketIsSafe(target, withBuddy);
            })
            .min(Comparator
                .comparingInt((CardInfo card) ->
                    preserveForObjective != null
                        && preserveForObjective.test(card) ? 1 : 0)
                .thenComparingInt(card ->
                    exactDeployCostAt(card.card, target.location))
                .thenComparingInt(card -> -(card.power + card.ability)))
            .orElse(null);
    }

    private boolean objectivePacketIsSafe(
            AiBoardAnalyzer.LocationAnalysis target,
            List<CardInfo> packet) {
        if (target == null || packet == null || packet.isEmpty()) {
            return false;
        }
        int friendlyCharacters = com.gempukku.swccgo.ai.models.common
                .strategy.FormationSafety
                .countFriendlyNonUndercoverCharacters(
                    currentGame.getGameState()
                        .getCardsAtLocation(target.location),
                    currentPlayerId);
        int packetPower = packet.stream()
                .mapToInt(card -> card.power).sum();
        int packetAbility = packet.stream()
                .mapToInt(card -> card.ability).sum();
        boolean opponentPresent = target.theirCardCount > 0
                || target.theirPower > 0.0f
                || target.theirAbility > 0.0f;
        if (!opponentPresent) {
            return friendlyCharacters > 0
                    || packet.size() >= 2
                        && (packetPower >= RandoConfig.MIN_SOLO_DEPLOY_POWER
                            || packetAbility
                                >= RandoConfig.ABILITY_THRESHOLD);
        }
        CardInfo lead = packet.get(0);
        int formationBodies = friendlyCharacters + packet.size();
        DeployTacticalPolicy.ContactAssessment contact =
                DeployTacticalPolicy.assessV171V172Contact(
                    new DeployTacticalPolicy.ContactFacts(
                        "counted-type-completion",
                        target.location.getTitle(), true, true,
                        formationBodies, target.ourPower,
                        lead.power, packetPower - lead.power,
                        Math.max(0, formationBodies - 1), 0.0f,
                        target.theirPower,
                        packet.stream().mapToInt(card -> card.power)
                            .max().orElse(0),
                        countArmedOpponentCharactersAt(target.location),
                        target.ourAbility + packetAbility));
        return contact.viable()
                && target.ourAbility + packetAbility
                    >= com.gempukku.swccgo.ai.models.common.strategy
                        .FormationSafety.DESTINY_ABILITY_THRESHOLD;
    }

    private float projectedDrainAt(PhysicalCard location) {
        try {
            return ForceDrainProjection.projectedDamage(
                    currentGame.getGameState(), currentGame,
                    location, currentPlayerId);
        } catch (Exception ignored) {
            return 0.0f;
        }
    }

    private PolicyResult endorPostFlipPlanAdjustment(
            DeploymentPlan plan,
            boolean reinforceOrAttack,
            boolean establishesEmptySite) {
        boolean endorOperations = objectiveAnalyzer != null
            && objectiveAnalyzer.isAnalyzed()
            && EndorOperationsTacticalPolicy.isEndorOperations(
                objectiveAnalyzer.getObjectiveBlueprintId(),
                objectiveAnalyzer.getObjectiveTitle());
        boolean targetsEndorSite = plan != null
            && plan.getInstructions().stream()
                .map(DeploymentInstruction::getTargetLocationName)
                .filter(Objects::nonNull)
                .anyMatch(title -> title.toLowerCase(Locale.ROOT)
                    .startsWith("endor:"));
        float adjustment =
            EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                endorOperations,
                objectiveAnalyzer != null && objectiveAnalyzer.isFlipped(),
                reinforceOrAttack,
                targetsEndorSite,
                establishesEmptySite);
        if (adjustment != 0.0f) {
            LOG.warn("EOP POST-FLIP PLAN: {} receives {} for {}",
                plan.getStrategy(), adjustment, plan.getReason());
        }
        return DeployPlanRankingPolicy.evaluateEndorAdjustment(
            "eop-post-flip-" + plan.getStrategy(),
            "V193-eop-post-flip-plan", adjustment,
            "EOP post-flip reinforcement or spread adjustment");
    }

    /**
     * Build only complete operative-plus-companion teams and distribute them
     * across the three runtime-selected battleground sites. A lone operative
     * cannot control its matching-planet site, so a partial pair is never a
     * valid objective plan.
     */
    private DeploymentPlan generateCountedOperativeFormationPlan(
            List<CardInfo> characters,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || !objectiveAnalyzer
                    .hasCountedOperativeFormationRule()
                || currentGame == null || currentPlayerId == null
                || forceAvailable <= 0 || characters.isEmpty()) {
            return null;
        }
        int formationsNeeded = objectiveAnalyzer
                .getCountedOperativeFormationsStillNeeded(
                    currentGame, currentPlayerId);
        if (formationsNeeded <= 0) return null;

        List<AiBoardAnalyzer.LocationAnalysis> targets =
                allLocations.stream()
                    .filter(loc -> loc != null && loc.isGround()
                        && loc.location != null
                        && objectiveAnalyzer
                            .isCountedOperativeFormationLocation(
                                currentGame, currentPlayerId,
                                loc.location)
                        && !objectiveAnalyzer
                            .isCountedOperativeFormationCompleteAt(
                                currentGame, currentPlayerId,
                                loc.location))
                    .sorted(Comparator
                        .comparingInt((AiBoardAnalyzer.LocationAnalysis loc) -> {
                            int missing = 0;
                            if (!objectiveAnalyzer
                                    .hasCountedOperativeActorAtLocation(
                                        currentGame, currentPlayerId,
                                        loc.location)) missing++;
                            if (!objectiveAnalyzer
                                    .hasCountedOperativeCompanionAtLocation(
                                        currentGame, currentPlayerId,
                                        loc.location)) missing++;
                            return missing;
                        })
                        .thenComparingInt(loc ->
                            loc.location.getPermanentCardId()))
                    .collect(Collectors.toList());
        if (targets.isEmpty()) return null;

        List<CardInfo> available = new ArrayList<>(characters);
        DeploymentPlan plan = new DeploymentPlan(
            DeployStrategy.REINFORCE,
            "Objective counted-operative formations");
        int remainingForce = forceAvailable;
        int routeActionReserve = 0;
        int formationsPlanned = 0;
        int priority = 1;

        for (AiBoardAnalyzer.LocationAnalysis target : targets) {
            if (formationsPlanned >= formationsNeeded
                    || remainingForce <= 0) break;
            boolean actorPresent = objectiveAnalyzer
                    .hasCountedOperativeActorAtLocation(
                        currentGame, currentPlayerId,
                        target.location);
            boolean companionPresent = objectiveAnalyzer
                    .hasCountedOperativeCompanionAtLocation(
                        currentGame, currentPlayerId,
                        target.location);
            if (actorPresent && companionPresent) {
                continue;
            }

            List<CardInfo> deployable = available.stream()
                    .filter(card -> canDeployPaidDirectly(
                        card.card, target.location))
                    .collect(Collectors.toList());
            List<CardInfo> actors = deployable.stream()
                    .filter(card -> objectiveAnalyzer
                        .matchesCountedOperativeFormationActor(
                            currentGame, currentPlayerId,
                            card.card))
                    .collect(Collectors.toList());
            List<CardInfo> companions = deployable.stream()
                    .filter(card -> objectiveAnalyzer
                        .isCountedOperativeFormationCompanion(
                            currentGame, currentPlayerId,
                            card.card))
                    .collect(Collectors.toList());

            List<CardInfo> selected = new ArrayList<>(2);
            int targetActionReserve = target.theirCardCount > 0
                    || target.theirPower > 0.0f
                    || target.theirAbility > 0.0f ? 1 : 0;
            int spendableForce = remainingForce
                    - Math.max(routeActionReserve,
                        targetActionReserve);
            if (spendableForce <= 0) continue;
            if (actorPresent) {
                CardInfo companion = selectCheapestOperativeFormationCard(
                        companions, target.location, spendableForce);
                if (companion == null) continue;
                selected.add(companion);
            } else if (companionPresent) {
                CardInfo actor = selectCheapestOperativeFormationCard(
                        actors, target.location, spendableForce);
                if (actor == null) continue;
                selected.add(actor);
            } else {
                selected.addAll(selectCheapestOperativeFormationPair(
                        actors, companions,
                        target.location, spendableForce));
                if (selected.size() != 2) continue;
            }

            int selectedCost = selected.stream()
                    .map(card -> exactDeployCostAt(
                        card.card, target.location))
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).sum();
            if (selectedCost > spendableForce
                    || !countedOperativeContactIsSafe(
                        target, selected)) {
                continue;
            }

            for (CardInfo card : selected) {
                Integer exactCost = exactDeployCostAt(
                        card.card, target.location);
                if (exactCost == null) {
                    selectedCost = Integer.MAX_VALUE;
                    break;
                }
                addCardToPlan(
                    plan, card.card, target, priority++,
                    "Complete operative-plus-companion objective team");
                DeploymentInstruction instruction =
                    plan.getInstructions().get(
                        plan.getInstructions().size() - 1);
                instruction.setDeployCost(exactCost);
                instruction.setAbilityContribution(card.ability);
            }
            if (selectedCost == Integer.MAX_VALUE) {
                return null;
            }
            available.removeAll(selected);
            remainingForce -= selectedCost;
            routeActionReserve = Math.max(
                    routeActionReserve, targetActionReserve);
            formationsPlanned++;
        }

        if (plan.getInstructions().isEmpty()) return null;
        LOG.warn("OBJECTIVE OPERATIVE FORMATIONS: funded {} complete team(s) with {} Force remaining",
            formationsPlanned, remainingForce);
        return plan;
    }

    private CardInfo selectCheapestOperativeFormationCard(
            List<CardInfo> candidates, PhysicalCard location,
            int budget) {
        return candidates.stream()
            .filter(card -> {
                Integer cost = exactDeployCostAt(card.card, location);
                return cost != null && cost <= budget;
            })
            .min(Comparator
                .comparingInt((CardInfo card) ->
                    exactDeployCostAt(card.card, location))
                .thenComparingInt(card -> -(card.power + card.ability))
                .thenComparingInt(card ->
                    card.card.getPermanentCardId()))
            .orElse(null);
    }

    private List<CardInfo> selectCheapestOperativeFormationPair(
            List<CardInfo> actors, List<CardInfo> companions,
            PhysicalCard location, int budget) {
        CardInfo bestActor = null;
        CardInfo bestCompanion = null;
        int bestCost = Integer.MAX_VALUE;
        int bestStrength = Integer.MIN_VALUE;
        for (CardInfo actor : actors) {
            Integer actorCost = exactDeployCostAt(
                    actor.card, location);
            if (actorCost == null) continue;
            for (CardInfo companion : companions) {
                if (actor.card == companion.card) continue;
                Integer companionCost = exactDeployCostAt(
                        companion.card, location);
                if (companionCost == null) continue;
                int total = actorCost + companionCost;
                int strength = actor.power + actor.ability
                        + companion.power + companion.ability;
                if (total <= budget
                        && (total < bestCost
                            || total == bestCost
                                && strength > bestStrength)) {
                    bestActor = actor;
                    bestCompanion = companion;
                    bestCost = total;
                    bestStrength = strength;
                }
            }
        }
        return bestActor == null
                ? Collections.emptyList()
                : List.of(bestActor, bestCompanion);
    }

    private boolean countedOperativeContactIsSafe(
            AiBoardAnalyzer.LocationAnalysis target,
            List<CardInfo> selected) {
        boolean opponentPresent = target.theirCardCount > 0
                || target.theirPower > 0.0f
                || target.theirAbility > 0.0f;
        if (!opponentPresent) return true;
        int friendlyCharacters = com.gempukku.swccgo.ai.models.common
                .strategy.FormationSafety
                .countFriendlyNonUndercoverCharacters(
                    currentGame.getGameState()
                        .getCardsAtLocation(target.location),
                    currentPlayerId);
        int selectedPower = selected.stream()
                .mapToInt(card -> card.power).sum();
        float projectedAbility = objectiveAnalyzer
                .getCountedOperativeProjectedBattleDestinyAbility(
                    currentGame, currentPlayerId, target.location,
                    selected.stream().map(card -> card.card).toList());
        int armedOpponentCount =
                countArmedOpponentCharactersAt(target.location);
        CardInfo lead = selected.get(0);
        DeployTacticalPolicy.ContactAssessment contact =
            DeployTacticalPolicy.assessV171V172Contact(
                new DeployTacticalPolicy.ContactFacts(
                    "counted-operative-formation",
                    target.location.getTitle(), true, true,
                    friendlyCharacters + selected.size(),
                    target.ourPower, lead.power,
                    selectedPower - lead.power,
                    Math.max(0,
                        friendlyCharacters + selected.size() - 1),
                    0.0f, target.theirPower,
                    selected.stream().mapToInt(card -> card.power)
                        .max().orElse(0),
                    armedOpponentCount, projectedAbility));
        return contact.viable() && projectedAbility >= 4.0f;
    }

    private int countArmedOpponentCharactersAt(PhysicalCard location) {
        if (location == null || currentGame == null
                || currentGame.getGameState() == null) {
            return 0;
        }
        String opponent = currentGame.getOpponent(currentPlayerId);
        if (opponent == null) return 0;
        int armed = 0;
        try {
            for (PhysicalCard card : currentGame.getGameState()
                    .getCardsAtLocation(location)) {
                if (card == null || card.getBlueprint() == null
                        || !opponent.equals(card.getOwner())
                        || card.getBlueprint().getCardCategory()
                            != CardCategory.CHARACTER) {
                    continue;
                }
                boolean hasWeapon = false;
                List<PhysicalCard> attachments = currentGame.getGameState()
                        .getAttachedCards(card);
                if (attachments != null) {
                    for (PhysicalCard attachment : attachments) {
                        if (attachment != null
                                && attachment.getBlueprint() != null
                                && attachment.getBlueprint().getCardCategory()
                                    == CardCategory.WEAPON) {
                            hasWeapon = true;
                            break;
                        }
                    }
                }
                String gameText = card.getBlueprint().getGameText();
                if (!hasWeapon && gameText != null
                        && gameText.toLowerCase(Locale.ROOT)
                            .contains("permanent weapon")) {
                    hasWeapon = true;
                }
                if (hasWeapon) armed++;
            }
        } catch (Exception ignored) {
            return 0;
        }
        return armed;
    }

    /**
     * V297: create a defensible objective-gate formation. Pre-flip actor gates
     * require the named actor plus an existing or funded buddy. Post-flip, keep
     * two characters at the gate so a single battle cannot immediately undo it.
     */
    private DeploymentPlan generateFlipGateFormationPlan(
            List<CardInfo> characters,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || !objectiveAnalyzer.hasFlipGateActorRequirement()
                || currentGame == null || currentPlayerId == null
                || forceAvailable <= 0 || characters.isEmpty()) {
            return null;
        }

        String gateTitle = objectiveAnalyzer.getFlipCriticalControlSite();
        if (gateTitle == null) return null;

        AiBoardAnalyzer.LocationAnalysis gate = allLocations.stream()
            .filter(loc -> loc != null && loc.isGround()
                && loc.location != null && loc.location.getTitle() != null
                && gateTitle.equalsIgnoreCase(loc.location.getTitle()))
            .findFirst().orElse(null);
        if (gate == null) return null;

        List<CardInfo> deployable = filterDeployableCards(characters, gate.location);
        if (deployable.isEmpty()) return null;

        int friendlyCharacters = com.gempukku.swccgo.ai.models.common.strategy
            .FormationSafety.countFriendlyNonUndercoverCharacters(
                currentGame.getGameState().getCardsAtLocation(gate.location),
                currentPlayerId);
        boolean actorRequired = !objectiveAnalyzer.isFlipped();
        boolean actorPresent = objectiveAnalyzer.hasFlipGateActorAtLocation(
            currentGame, currentPlayerId, gate.location);
        boolean controlsGate = gate.weControl();
        List<CardInfo> selected = new ArrayList<>(2);

        if (actorRequired && !actorPresent) {
            List<CardInfo> actorCandidates = deployable.stream()
                .filter(card -> objectiveAnalyzer.matchesFlipGateActorRequirement(
                    currentGame, currentPlayerId, card.card, gate.location))
                .collect(Collectors.toList());
            if (actorCandidates.isEmpty()) {
                CardInfo enabler = deployable.stream()
                    .filter(card -> {
                        Integer futureActorCost =
                            objectiveAnalyzer.getFlipGateActorEnablerFutureDeployCost(
                                currentGame, currentPlayerId, card.card);
                        return futureActorCost != null
                            && card.cost + futureActorCost <= forceAvailable;
                    })
                    .max(Comparator
                        .comparingInt((CardInfo card) -> card.power + card.ability)
                        .thenComparingDouble(CardInfo::getValueRatio)
                        .thenComparingInt(card -> -card.cost))
                    .orElse(null);
                if (enabler == null) return null;
                selected.add(enabler);
                LOG.warn("OBJECTIVE.DEPLOY.ENABLER_PLAN: {} -> {} with future actor funded",
                    enabler.name, gateTitle);
            } else if (friendlyCharacters == 0) {
                selected.addAll(selectBestFormationPair(
                    actorCandidates, deployable, forceAvailable));
                if (selected.size() != 2) return null;
            } else {
                CardInfo actor = selectBestFormationCard(
                    actorCandidates, forceAvailable);
                if (actor == null) return null;
                selected.add(actor);
            }
        } else {
            int bodiesNeeded = Math.max(0, 2 - friendlyCharacters);
            if (bodiesNeeded == 0 && !controlsGate) bodiesNeeded = 1;
            if (bodiesNeeded == 0) return null;
            if (bodiesNeeded == 1) {
                CardInfo buddy = selectBestFormationCard(
                    deployable, forceAvailable);
                if (buddy == null) return null;
                selected.add(buddy);
            } else {
                selected.addAll(selectBestFormationPair(
                    deployable, deployable, forceAvailable));
                if (selected.size() != 2) return null;
            }
        }

        int selectedPower = selected.stream().mapToInt(card -> card.power).sum();
        int selectedAbility = selected.stream().mapToInt(card -> card.ability).sum();
        float projectedAbility = gate.ourAbility + selectedAbility;
        boolean opponentPresent = gate.theirCardCount > 0
            || gate.theirPower > 0.0f || gate.theirAbility > 0.0f;
        if (opponentPresent) {
            CardInfo lead = selected.get(0);
            int formationBodies = selected.size() + friendlyCharacters;
            DeployTacticalPolicy.ContactAssessment contact =
                DeployTacticalPolicy.assessV171V172Contact(
                    new DeployTacticalPolicy.ContactFacts(
                        "objective-formation-plan", gateTitle,
                        true, true, formationBodies, gate.ourPower,
                        lead.power, selectedPower - lead.power,
                        Math.max(0, formationBodies - 1), 0.0f,
                        gate.theirPower,
                        selected.stream().mapToInt(card -> card.power).max().orElse(0),
                        0, projectedAbility));
            if (!contact.viable() || projectedAbility < 4.0f) {
                LOG.warn("OBJECTIVE FORMATION REJECTED: {} at {} projects power {}/ability {} "
                        + "into power {}/ability {} (V171/V172 contact={}, battle destiny={})",
                    selected.stream().map(card -> card.name).collect(Collectors.joining(" + ")),
                    gateTitle, contact.projectedPower(), projectedAbility,
                    gate.theirPower, gate.theirAbility,
                    contact.viable(), projectedAbility >= 4.0f);
                return null;
            }
        }

        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "V297 objective flip-gate formation at " + gateTitle);
        int priority = 1;
        for (CardInfo card : selected) {
            addCardToPlan(plan, card.card, gate, priority++,
                "V297 secure objective gate with actor-and-buddy formation");
            plan.getInstructions().get(plan.getInstructions().size() - 1)
                .setAbilityContribution(card.ability);
        }
        LOG.warn("V297 OBJECTIVE FORMATION: {} -> {} with {} funded character(s), {} already present",
            selected.stream().map(card -> card.name).collect(Collectors.joining(" + ")),
            gateTitle, selected.size(), friendlyCharacters);
        return plan;
    }

    private List<CardInfo> selectBestFormationPair(
            List<CardInfo> firstCandidates, List<CardInfo> allCandidates,
            int budget) {
        CardInfo bestFirst = null;
        CardInfo bestSecond = null;
        int bestScore = Integer.MIN_VALUE;
        for (CardInfo first : firstCandidates) {
            for (CardInfo second : allCandidates) {
                if (first.card == second.card || first.cost + second.cost > budget) continue;
                int combinedAbility = first.ability + second.ability;
                int combinedPower = first.power + second.power;
                int score = (combinedAbility >= 4 ? 100000 : 0)
                    + combinedPower * 100 + combinedAbility * 25
                    - first.cost - second.cost;
                if (score > bestScore) {
                    bestScore = score;
                    bestFirst = first;
                    bestSecond = second;
                }
            }
        }
        return bestFirst == null
            ? Collections.emptyList() : List.of(bestFirst, bestSecond);
    }

    private CardInfo selectBestFormationCard(
            List<CardInfo> candidates, int budget) {
        return candidates.stream()
            .filter(card -> card.cost <= budget)
            .max(Comparator
                .comparingInt((CardInfo card) -> card.ability >= 4 ? 1 : 0)
                .thenComparingInt(card -> card.power + card.ability)
                .thenComparingDouble(CardInfo::getValueRatio)
                .thenComparingInt(card -> -card.cost))
            .orElse(null);
    }

    /**
     * Generate space deployment plans.
     */
    private List<ScoredPlan> generateSpacePlans(List<CardInfo> starships, List<CardInfo> characters,
                                                 LocationCategories categories, int forceAvailable,
                                                 int threshold, List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                 int turn, DrainGapResult drainGap,
                                                 SwccgGame game, String playerId,
                                                 DeploymentPlan endorSystemPlan) {
        List<ScoredPlan> plans = new ArrayList<>();

        if (starships.isEmpty()) {
            return plans;
        }

        // Get pilots for ship combos
        List<CardInfo> pilots = characters.stream()
            .filter(c -> c.isPilot)
            .collect(Collectors.toList());

        if (endorSystemPlan != null
                && !endorSystemPlan.getInstructions().isEmpty()) {
            float score = scorePlan(endorSystemPlan, allLocations, turn,
                    DeployPlanRankingPolicy.evaluateEndorAdjustment(
                        "eop-endor-system", "V193-eop-endor-system-plan",
                        EndorOperationsTacticalPolicy.POST_FLIP_REINFORCE_BONUS,
                        "EOP funded Endor system package preference"));
            plans.add(new ScoredPlan(
                    endorSystemPlan, score, "objective_endor_system"));
            LOG.warn("EOP SPACE FIRST: added funded Endor system package at +{}",
                    EndorOperationsTacticalPolicy.POST_FLIP_REINFORCE_BONUS);
        }

        // Plan 1: Stop bleeding in space
        List<AiBoardAnalyzer.LocationAnalysis> spaceBleed = categories.bleedLocations.stream()
            .filter(AiBoardAnalyzer.LocationAnalysis::isSpace)
            .collect(Collectors.toList());

        if (drainGap.isLosing() && !spaceBleed.isEmpty()) {
            DeploymentPlan bleedPlan = generateStopBleedingPlan(
                new ArrayList<>(starships), spaceBleed, forceAvailable, threshold, "space");
            if (!bleedPlan.getInstructions().isEmpty()) {
                float score = scorePlan(bleedPlan, allLocations, turn);
                plans.add(new ScoredPlan(bleedPlan, score, "space_bleed"));
            }
        }

        // Plan 2: Reinforce losing space
        List<AiBoardAnalyzer.LocationAnalysis> spaceLosing = categories.losingLocations.stream()
            .filter(AiBoardAnalyzer.LocationAnalysis::isSpace)
            .collect(Collectors.toList());

        if (!spaceLosing.isEmpty()) {
            DeploymentPlan reinforcePlan = generateReinforcePlan(
                new ArrayList<>(starships), spaceLosing, forceAvailable, "space");
            if (!reinforcePlan.getInstructions().isEmpty()) {
                float score = scorePlan(reinforcePlan, allLocations, turn);
                plans.add(new ScoredPlan(reinforcePlan, score, "space_reinforce"));
            }
        }

        // Plan 3: Establish in space
        List<AiBoardAnalyzer.LocationAnalysis> spaceEstablish = categories.establishTargets.stream()
            .filter(AiBoardAnalyzer.LocationAnalysis::isSpace)
            .filter(location -> !SpaceDeploymentAllocationPolicy.isDeferred(
                    SpaceDeploymentAllocationPolicy.evaluate(
                            new SpaceDeploymentAllocationPolicy.Facts(
                                    "planner-space-establish-"
                                            + location.location.getCardId(),
                                    true, location.ourAbility,
                                    location.ourAbility,
                                    location.theirCardCount > 0,
                                    false, false, false, false))))
            .collect(Collectors.toList());

        if (!spaceEstablish.isEmpty()) {
            DeploymentPlan establishPlan = generateEstablishPlan(
                new ArrayList<>(starships), spaceEstablish, forceAvailable, threshold, "space");
            if (!establishPlan.getInstructions().isEmpty()) {
                float score = scorePlan(establishPlan, allLocations, turn);
                plans.add(new ScoredPlan(establishPlan, score, "space_establish"));
            }
        }

        // Plan 4: RE-PILOT unpiloted ships in play (Item #6)
        DeploymentPlan repilotPlan = generateRepilotPlan(pilots, game, playerId, forceAvailable);
        if (!repilotPlan.getInstructions().isEmpty()) {
            float score = scorePlan(repilotPlan, allLocations, turn);
            plans.add(new ScoredPlan(repilotPlan, score, "space_repilot"));
        }

        // V22: OBJECTIVE CAPITAL SHIP PRIORITY
        // If the objective wants Bespin control, prioritize deploying a capital ship there.
        if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
            Set<String> fragments = objectiveAnalyzer.getFlipConditionLocationFragments();
            boolean objectiveWantsBespin = fragments.contains("bespin") || fragments.contains("cloud city");
            if (objectiveWantsBespin) {
                DeploymentPlan executorPlan = generateObjectiveCapitalPlan(
                    starships, characters, allLocations, forceAvailable, game, playerId);
                if (!executorPlan.getInstructions().isEmpty()) {
                    float score = scoreObjectiveCapitalPlan(executorPlan, allLocations, turn);
                    plans.add(new ScoredPlan(executorPlan, score, "objective_capital_bespin"));
                    LOG.warn("📋 V22: Added objective capital ship plan for Bespin (+300 bounded objective preference)");
                }
            }
        }

        return plans;
    }

    private DeploymentPlan generateEopEndorSystemPlan(
            List<CardInfo> starships,
            List<CardInfo> pilots,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        DeploymentPlan plan = new DeploymentPlan(
                DeployStrategy.ESTABLISH,
                "EOP: occupy Endor system before further ground expansion");
        if (!endorSystemOccupationPending(allLocations)
                || currentGame == null
                || currentGame.getGameState() == null
                || currentGame.getModifiersQuerying() == null
                || currentPlayerId == null) {
            return plan;
        }
        AiBoardAnalyzer.LocationAnalysis endor = allLocations.stream()
                .filter(loc -> loc != null && loc.isSpace()
                        && loc.location != null
                        && "endor".equalsIgnoreCase(loc.location.getTitle())
                        && loc.theirPower <= 0)
                .findFirst().orElse(null);
        if (endor == null) {
            return plan;
        }
        CardInfo reservedBunkerGarrison =
                findReservedEopBunkerGarrison(pilots, allLocations);
        EndorSystemPackage best = null;
        for (CardInfo ship : starships) {
            Integer soloCost = exactDeployCostAt(ship.card, endor.location);
            float shipAbility = exactAbility(ship.card, true);
            if (soloCost != null
                    && soloCost <= forceAvailable
                    && shipAbility > 0.0f
                    && canDeployPaidDirectly(ship.card, endor.location)) {
                EndorSystemPackage candidate = new EndorSystemPackage(
                        ship, null, soloCost, shipAbility,
                        ship.power, 0);
                if (isBetterEndorSystemPackage(candidate, best)) {
                    best = candidate;
                }
            }
            for (CardInfo pilot : pilots) {
                if (!SpaceDeploymentAllocationFactsReader
                        .isPlannerPilotEligible(
                                currentGame, pilot.card, ship.card)) {
                    continue;
                }
                if (reservedBunkerGarrison != null
                        && isSamePhysicalCard(
                                pilot.card, reservedBunkerGarrison.card)) {
                    continue;
                }
                float pilotAbility = exactAbility(pilot.card, false);
                if (pilotAbility <= 0.0f
                        || !canDeployShipAndPilotTogether(
                                ship.card, pilot.card, endor.location)) {
                    continue;
                }
                Integer pairCost = exactSimultaneousDeployCost(
                        ship.card, pilot.card, endor.location);
                if (pairCost == null || pairCost > forceAvailable) {
                    continue;
                }
                EndorSystemPackage candidate = new EndorSystemPackage(
                        ship, pilot, pairCost,
                        shipAbility + pilotAbility,
                        ship.power + pilotAddedPower(pilot, ship.card),
                        pilotQualityTier(pilot, ship.card));
                if (isBetterEndorSystemPackage(candidate, best)) {
                    best = candidate;
                }
            }
        }
        if (best == null) {
            return plan;
        }

        addCardToPlan(plan, best.ship.card, endor, 1,
                "EOP: deploy a legal ship package to occupy Endor system");
        DeploymentInstruction shipInstruction =
                plan.getInstructions().get(plan.getInstructions().size() - 1);
        shipInstruction.setDeployCost(best.cost);
        shipInstruction.setAbilityContribution(
                instructionAbilityContribution(best.ship.card));
        if (best.pilot != null) {
            shipInstruction.setVerifiedCrewPackage(true);
            addCardToPlan(plan, best.pilot.card, endor, 2,
                    "EOP: deploy the legal pilot aboard the Endor system ship");
            DeploymentInstruction pilotInstruction =
                    plan.getInstructions().get(plan.getInstructions().size() - 1);
            pilotInstruction.setDeployCost(0);
            pilotInstruction.setAbilityContribution(
                    instructionAbilityContribution(best.pilot.card));
            pilotInstruction.setPowerContribution(
                    pilotAddedPower(best.pilot, best.ship.card));
            pilotInstruction.setAboardShipName(best.ship.name);
            pilotInstruction.setAboardShipBlueprintId(best.ship.blueprintId);
            pilotInstruction.setAboardShipCardId(
                    String.valueOf(best.ship.card.getCardId()));
        }
        return plan;
    }

    private DeploymentPlan generateEopBunkerGarrisonPlan(
            List<CardInfo> characters,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        DeploymentPlan plan = new DeploymentPlan(
                DeployStrategy.ESTABLISH,
                EndorOperationsTacticalPolicy.bunkerGarrisonPlanReason());
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || objectiveAnalyzer.isFlipped()
                || !EndorOperationsTacticalPolicy.isEndorOperations(
                        objectiveAnalyzer.getObjectiveBlueprintId(),
                        objectiveAnalyzer.getObjectiveTitle())
                || currentGame == null
                || currentGame.getGameState() == null
                || currentPlayerId == null
                || forceAvailable <= 0
                || hasReadyMissingRequiredCardInHand()) {
            return plan;
        }
        AiBoardAnalyzer.LocationAnalysis bunker = allLocations.stream()
                .filter(loc -> loc != null && loc.isGround()
                        && loc.location != null
                        && "endor: bunker".equalsIgnoreCase(
                                loc.location.getTitle())
                        && loc.theirCardCount == 0
                        && loc.theirAbility <= 0)
                .findFirst().orElse(null);
        if (bunker == null) {
            return plan;
        }
        CardInfo garrison =
                findReservedEopBunkerGarrison(
                        characters, allLocations);
        if (garrison == null) {
            return plan;
        }
        Integer exactCost = exactDeployCostAt(
                garrison.card, bunker.location);
        if (exactCost == null || exactCost > forceAvailable) {
            return plan;
        }
        addCardToPlan(
                plan, garrison.card, bunker, 1,
                "EOP: cheap Imperial admiral holds Bunker while scouts occupy other Endor sites");
        plan.getInstructions().get(0).setDeployCost(exactCost);
        return plan;
    }

    private boolean hasReadyMissingRequiredCardInHand() {
        List<PhysicalCard> hand =
                currentGame.getGameState().getHand(currentPlayerId);
        if (hand == null || hand.isEmpty()) {
            return false;
        }
        for (PhysicalCard card : hand) {
            if (card != null
                    && objectiveAnalyzer.isRequiredCardForFlip(card)
                    && !objectiveAnalyzer.isRequiredCardActiveOnTable(
                            currentGame, card.getTitle())
                    && objectiveAnalyzer
                        .isRequiredOnTableCardPullRouteReady(
                            currentGame, currentPlayerId, card)) {
                return true;
            }
        }
        return false;
    }

    private CardInfo findReservedEopBunkerGarrison(
            List<CardInfo> candidates,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations) {
        AiBoardAnalyzer.LocationAnalysis bunker = allLocations.stream()
                .filter(loc -> loc != null && loc.location != null
                        && "endor: bunker".equalsIgnoreCase(
                                loc.location.getTitle()))
                .findFirst().orElse(null);
        if (bunker == null) {
            return null;
        }
        List<PhysicalCard> cardsAtBunker =
                currentGame.getGameState().getCardsAtLocation(bunker.location);
        if (cardsAtBunker != null && cardsAtBunker.stream()
                .anyMatch(card -> currentPlayerId.equals(card.getOwner())
                        && isImperialAdmiral(card))) {
            return null;
        }
        CardInfo best = null;
        int bestCost = Integer.MAX_VALUE;
        for (CardInfo candidate : candidates) {
            Integer cost = exactDeployCostAt(
                    candidate.card, bunker.location);
            if (cost == null || cost > 2
                    || !isImperialAdmiral(candidate.card)
                    || !canDeployDirectly(
                            candidate.card, bunker.location)) {
                continue;
            }
            int permanentId = candidate.card.getPermanentCardId();
            int bestPermanentId = best == null
                    ? Integer.MAX_VALUE
                    : best.card.getPermanentCardId();
            if (cost < bestCost
                    || cost == bestCost
                        && permanentId < bestPermanentId) {
                best = candidate;
                bestCost = cost;
            }
        }
        return best;
    }

    private boolean isSamePhysicalCard(
            PhysicalCard first, PhysicalCard second) {
        return first != null && second != null
                && first.getPermanentCardId()
                        == second.getPermanentCardId()
                && first.getCardId() == second.getCardId();
    }

    private boolean isImperialAdmiral(PhysicalCard card) {
        try {
            return Filters.and(Filters.Imperial, Filters.admiral)
                    .accepts(currentGame.getGameState(),
                            currentGame.getModifiersQuerying(), card);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDeployDirectly(
            PhysicalCard card, PhysicalCard location) {
        try {
            return Filters.deployableToLocation(
                    card, Filters.sameCardId(location), true, 0.0f)
                    .accepts(currentGame.getGameState(),
                            currentGame.getModifiersQuerying(), card);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDeployPaidDirectly(
            PhysicalCard card, PhysicalCard location) {
        try {
            return Filters.deployableToLocation(
                    card, Filters.sameCardId(location), false, 0.0f)
                    .accepts(currentGame.getGameState(),
                            currentGame.getModifiersQuerying(), card);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDeployShipAndPilotTogether(
            PhysicalCard ship,
            PhysicalCard pilot,
            PhysicalCard location) {
        try {
            GameState gameState = currentGame.getGameState();
            if (!ship.getBlueprint()
                    .getValidPilotFilter(
                            currentPlayerId, currentGame, ship, true)
                    .accepts(gameState,
                            currentGame.getModifiersQuerying(), pilot)
                    || !Filters.hasAvailablePilotCapacity(pilot)
                    .accepts(gameState,
                            currentGame.getModifiersQuerying(), ship)) {
                return false;
            }
            return Filters.deployableToLocationSimultaneouslyWith(
                    ship, pilot, false, 0.0f,
                    Filters.sameCardId(location), false, 0.0f)
                    .accepts(gameState,
                            currentGame.getModifiersQuerying(), ship);
        } catch (Exception e) {
            return false;
        }
    }

    private Integer exactDeployCostAt(
            PhysicalCard card, PhysicalCard location) {
        try {
            float cost = currentGame.getModifiersQuerying().getDeployCost(
                    currentGame.getGameState(), card, card, location,
                    false, null, false, 0.0f, null, true);
            if (!Float.isFinite(cost)) {
                return null;
            }
            return (int) Math.ceil(Math.max(0.0f, cost));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer exactSimultaneousDeployCost(
            PhysicalCard ship,
            PhysicalCard pilot,
            PhysicalCard location) {
        try {
            float cost = currentGame.getModifiersQuerying()
                    .getSimultaneousDeployCost(
                            currentGame.getGameState(),
                            ship, ship, false, 0.0f,
                            pilot, false, 0.0f,
                            location, null, true);
            return (int) Math.ceil(Math.max(0.0f, cost));
        } catch (Exception e) {
            return null;
        }
    }

    private float exactAbility(
            PhysicalCard card, boolean includePermanentPilots) {
        try {
            return currentGame.getModifiersQuerying().getAbility(
                    currentGame.getGameState(), card,
                    includePermanentPilots);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private boolean isBetterEndorSystemPackage(
            EndorSystemPackage candidate,
            EndorSystemPackage current) {
        int candidateCards = candidate.pilot == null ? 1 : 2;
        int currentCards = current == null || current.pilot == null ? 1 : 2;
        if (current != null && candidateCards == currentCards
                && candidate.pilotQualityTier
                        != current.pilotQualityTier) {
            return candidate.pilotQualityTier
                    > current.pilotQualityTier;
        }
        if (current == null || candidate.cost != current.cost) {
            return current == null || candidate.cost < current.cost;
        }
        if (candidateCards != currentCards) {
            return candidateCards < currentCards;
        }
        if (candidate.pilotQualityTier
                != current.pilotQualityTier) {
            return candidate.pilotQualityTier
                    > current.pilotQualityTier;
        }
        int abilityOrder = Float.compare(
                candidate.combinedAbility, current.combinedAbility);
        if (abilityOrder != 0) {
            return abilityOrder > 0;
        }
        if (candidate.combinedPower != current.combinedPower) {
            return candidate.combinedPower > current.combinedPower;
        }
        int shipOrder = Integer.compare(
                candidate.ship.card.getPermanentCardId(),
                current.ship.card.getPermanentCardId());
        if (shipOrder != 0) {
            return shipOrder < 0;
        }
        int candidatePilotId = candidate.pilot == null
                ? Integer.MAX_VALUE
                : candidate.pilot.card.getPermanentCardId();
        int currentPilotId = current.pilot == null
                ? Integer.MAX_VALUE
                : current.pilot.card.getPermanentCardId();
        return candidatePilotId < currentPilotId;
    }

    private boolean endorSystemOccupationPending(
            List<AiBoardAnalyzer.LocationAnalysis> allLocations) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || !objectiveAnalyzer.isFlipped()
                || !EndorOperationsTacticalPolicy.isEndorOperations(
                        objectiveAnalyzer.getObjectiveBlueprintId(),
                        objectiveAnalyzer.getObjectiveTitle())) {
            return false;
        }
        AiBoardAnalyzer.LocationAnalysis endor = allLocations.stream()
                .filter(loc -> loc != null && loc.isSpace()
                        && loc.location != null
                        && "endor".equalsIgnoreCase(loc.location.getTitle()))
                .findFirst().orElse(null);
        return EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                true, true, endor != null,
                endor != null && endor.weControl(),
                endor == null ? 0.0f : endor.theirPower);
    }

    private boolean targetsEmptyEndorSite(DeploymentPlan plan) {
        return plan.getInstructions().stream()
                .map(DeploymentInstruction::getTargetLocationName)
                .filter(Objects::nonNull)
                .anyMatch(title -> title.toLowerCase(Locale.ROOT).startsWith("endor:"));
    }

    private boolean occupiesEndorBattlegroundSite(
            List<AiBoardAnalyzer.LocationAnalysis> allLocations) {
        return allLocations.stream()
                .anyMatch(loc -> loc != null
                        && loc.isGround()
                        && loc.isBattleground
                        && loc.location != null
                        && loc.location.getTitle() != null
                        && loc.location.getTitle().toLowerCase(Locale.ROOT)
                                .startsWith("endor:")
                        && loc.weControl());
    }

    private static final class EndorSystemPackage {
        private final CardInfo ship;
        private final CardInfo pilot;
        private final int cost;
        private final float combinedAbility;
        private final int combinedPower;
        private final int pilotQualityTier;

        private EndorSystemPackage(
                CardInfo ship,
                CardInfo pilot,
                int cost,
                float combinedAbility,
                int combinedPower,
                int pilotQualityTier) {
            this.ship = ship;
            this.pilot = pilot;
            this.cost = cost;
            this.combinedAbility = combinedAbility;
            this.combinedPower = combinedPower;
            this.pilotQualityTier = pilotQualityTier;
        }
    }

    /**
     * Generate combined ground+space plans.
     */
    private List<ScoredPlan> generateCombinedPlans(List<CardInfo> characters, List<CardInfo> starships,
                                                    List<CardInfo> vehicles, LocationCategories categories,
                                                    int forceAvailable, int groundThreshold, int spaceThreshold,
                                                    List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                    int turn, DrainGapResult drainGap) {
        List<ScoredPlan> plans = new ArrayList<>();

        // Combined: Best ground + best space within budget
        List<CardInfo> groundCards = new ArrayList<>(characters);
        groundCards.addAll(vehicles);

        // Find best single ground target
        AiBoardAnalyzer.LocationAnalysis bestGroundTarget = findBestTarget(
            categories, true, groundThreshold);
        AiBoardAnalyzer.LocationAnalysis bestSpaceTarget = findBestTarget(
            categories, false, spaceThreshold);

        if (bestGroundTarget != null && bestSpaceTarget != null) {
            DeploymentPlan combinedPlan = new DeploymentPlan(
                DeployStrategy.COMPREHENSIVE, "Combined ground + space deployment");

            int remaining = forceAvailable;

            // Try ground first - must BEAT enemy power, not just have cards
            int groundPowerNeeded = (int) bestGroundTarget.theirPower + RandoConfig.BATTLE_FAVORABLE_THRESHOLD;
            OptimalCombination groundCombo = findOptimalCombination(
                groundCards, remaining / 2, groundPowerNeeded, true);

            // CRITICAL: Only add if we can actually beat them!
            if (groundCombo.achievesGoal && !groundCombo.isEmpty()) {
                for (PhysicalCard card : groundCombo.cards) {
                    addCardToPlan(combinedPlan, card, bestGroundTarget, 1,
                        "Combined: ground attack");
                }
                remaining -= groundCombo.totalCost;
            }

            // Then space - must BEAT enemy power
            int spacePowerNeeded = (int) bestSpaceTarget.theirPower + RandoConfig.BATTLE_FAVORABLE_THRESHOLD;
            OptimalCombination spaceCombo = findOptimalCombination(
                starships, remaining, spacePowerNeeded, true);

            // CRITICAL: Only add if we can actually beat them!
            float spacePlannedAbility = (float) spaceCombo.cards.stream()
                    .mapToDouble(card -> exactAbility(card, true))
                    .sum();
            boolean spaceAllocationAdmissible =
                    !SpaceDeploymentAllocationPolicy.isDeferred(
                            SpaceDeploymentAllocationPolicy.evaluate(
                                    new SpaceDeploymentAllocationPolicy.Facts(
                                            "planner-combined-space-"
                                                + bestSpaceTarget.location
                                                    .getCardId(),
                                            true,
                                            bestSpaceTarget.ourAbility,
                                            bestSpaceTarget.ourAbility
                                                + (float) spacePlannedAbility,
                                            bestSpaceTarget.theirCardCount > 0
                                                || bestSpaceTarget.theirPower > 0,
                                            spaceCombo.achievesGoal
                                                && bestSpaceTarget.theirPower > 0,
                                            false, false, false)));
            if (spaceCombo.achievesGoal && !spaceCombo.isEmpty()
                    && spaceAllocationAdmissible) {
                for (PhysicalCard card : spaceCombo.cards) {
                    addCardToPlan(combinedPlan, card, bestSpaceTarget, 1,
                        "Combined: space attack");
                }
            } else if (!spaceAllocationAdmissible) {
                LOG.info("V298 GROUND FIRST: combined plan omits quiet space leg at {} because actual ability is {}",
                        bestSpaceTarget.location.getTitle(),
                        bestSpaceTarget.ourAbility);
            }

            if (!combinedPlan.getInstructions().isEmpty()) {
                float score = scorePlan(combinedPlan, allLocations, turn);
                plans.add(new ScoredPlan(combinedPlan, score, "combined"));
            }
        }

        return plans;
    }

    // =========================================================================
    // SPECIFIC PLAN GENERATORS
    // =========================================================================

    /**
     * Generate stop-bleeding plan (Item #8 - presence-only).
     */
    private DeploymentPlan generateStopBleedingPlan(List<CardInfo> cards,
                                                     List<AiBoardAnalyzer.LocationAnalysis> bleedLocations,
                                                     int forceAvailable, int threshold, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Stop bleeding in " + domain);

        // Sort by our icons (highest drain first)
        bleedLocations.sort((a, b) -> Integer.compare(b.ourForceIcons, a.ourForceIcons));

        int remaining = forceAvailable;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : bleedLocations) {
            if (remaining <= 0 || available.isEmpty()) break;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            OptimalCombination combo;
            boolean winnable;
            if (loc.isSite) {
                // Ability 4 permits one normal battle-destiny draw. Extra
                // aggregate ability does not create extra draws, so sites use
                // the shared V181 bounded-contest rule.
                combo = findWinnableSiteCombination(
                    deployableHere, remaining, loc);
                winnable = !combo.isEmpty();
            } else {
                // Preserve the legacy space boundary. CardInfo does not model
                // permanent-pilot ability or starship forfeit well enough for
                // the character-based V181 site predicate.
                float abilityPenalty = Math.max(0, loc.theirAbility - 4) * 2.5f;
                int effectivePowerNeeded = (int) (loc.theirPower + abilityPenalty);
                combo = findOptimalCombination(
                    deployableHere, remaining, effectivePowerNeeded, true);
                boolean abilityOk = combo.totalAbility >= loc.theirAbility
                    || combo.totalPower >= loc.theirPower + 3;
                winnable = !combo.isEmpty() && combo.achievesGoal && abilityOk;
            }

            if (winnable) {
                for (PhysicalCard card : combo.cards) {
                    CardInfo info = findCardInfo(available, card);
                    if (info != null) {
                        addCardToPlan(plan, card, loc, 1,
                            String.format("Stop bleed at %s (prevent %d drain)",
                                loc.location.getTitle(), loc.ourForceIcons));
                        remaining -= info.cost;
                        available.removeIf(c -> c.card == card);
                    }
                }
            } else if (!combo.isEmpty()) {
                LOG.info("📋 Skipping stop-bleed at {} - stop-bleed gate rejects wave power/ability {}/{} against {}/{}",
                    loc.location.getTitle(), combo.totalPower,
                    combo.totalAbility, (int) loc.theirPower,
                    (int) loc.theirAbility);
            }
        }

        return plan;
    }

    /**
     * Generate reinforce plan for losing locations.
     */
    private DeploymentPlan generateReinforcePlan(List<CardInfo> cards,
                                                  List<AiBoardAnalyzer.LocationAnalysis> losingLocations,
                                                  int forceAvailable, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Reinforce losing " + domain);

        // Sort by power deficit (worst first)
        losingLocations.sort((a, b) -> Double.compare(a.getPowerAdvantage(), b.getPowerAdvantage()));

        int remaining = forceAvailable;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : losingLocations) {
            if (remaining <= 0 || available.isEmpty()) break;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            int deficit = (int) (loc.theirPower - loc.ourPower);

            // Find cards to close the gap
            OptimalCombination combo = findOptimalCombination(deployableHere, remaining, deficit, false);

            if (!combo.isEmpty()) {
                for (PhysicalCard card : combo.cards) {
                    CardInfo info = findCardInfo(available, card);
                    if (info != null) {
                        addCardToPlan(plan, card, loc, 1,
                            String.format("Reinforce %s (deficit: %d)",
                                loc.location.getTitle(), deficit));
                        remaining -= info.cost;
                        available.removeIf(c -> c.card == card);
                    }
                }
            }
        }

        return plan;
    }

    /**
     * Generate establish plan for uncontested locations.
     */
    private DeploymentPlan generateLateGroundEstablishPlan(
            List<CardInfo> cards,
            List<AiBoardAnalyzer.LocationAnalysis> establishTargets,
            int forceAvailable, int threshold, String domain,
            int currentTurn) {
        int opponentLostPileSize = -1;
        try {
            String opponentId = currentGame.getOpponent(currentPlayerId);
            opponentLostPileSize = currentGame.getGameState()
                    .getLostPile(opponentId).size();
        } catch (Exception ignored) {
            // Unknown Lost Pile state keeps every legacy restriction.
        }
        return generateEstablishPlan(cards, establishTargets,
                forceAvailable, threshold, domain,
                currentTurn, opponentLostPileSize);
    }

    private DeploymentPlan generateEstablishPlan(List<CardInfo> cards,
                                                  List<AiBoardAnalyzer.LocationAnalysis> establishTargets,
                                                  int forceAvailable, int threshold, String domain) {
        return generateEstablishPlan(cards, establishTargets,
                forceAvailable, threshold, domain, -1, -1);
    }

    private DeploymentPlan generateEstablishPlan(List<CardInfo> cards,
                                                  List<AiBoardAnalyzer.LocationAnalysis> establishTargets,
                                                  int forceAvailable, int threshold, String domain,
                                                  int currentTurn, int opponentLostPileSize) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.ESTABLISH,
            "Establish in " + domain);

        // Rank the executable package, not just printed icons. A conditional
        // drain site is promoted only when the exact selected package contains
        // the character that turns its bonus on.
        Map<Integer, Float> projectedDrainByTarget = new HashMap<>();
        for (AiBoardAnalyzer.LocationAnalysis target : establishTargets) {
            float projected = ForceDrainProjection.projectedDamage(
                    currentGame.getGameState(), currentGame,
                    target.location, currentPlayerId);
            EstablishDrainPackage conditional =
                    selectConditionalDrainPackage(
                            filterDeployableCards(cards, target.location),
                            target.location, forceAvailable);
            if (conditional != null) {
                projected = Math.max(projected,
                        conditional.projectedDrain());
            }
            projectedDrainByTarget.put(
                    target.location.getCardId(), projected);
        }

        establishTargets.sort((a, b) -> {
            float drainA = projectedDrainByTarget.getOrDefault(
                    a.location.getCardId(), 0.0f);
            float drainB = projectedDrainByTarget.getOrDefault(
                    b.location.getCardId(), 0.0f);
            int drainOrder = Float.compare(drainB, drainA);
            if (drainOrder != 0) return drainOrder;
            boolean ownA = currentPlayerId.equals(a.location.getOwner());
            boolean ownB = currentPlayerId.equals(b.location.getOwner());
            if (ownA != ownB) return ownA ? -1 : 1;
            int iconOrder = Integer.compare(
                    b.theirForceIcons, a.theirForceIcons);
            if (iconOrder != 0) return iconOrder;
            return a.location.getTitle().compareToIgnoreCase(
                    b.location.getTitle());
        });

        int remaining = forceAvailable;
        int establishCount = 0;
        List<CardInfo> available = new ArrayList<>(cards);
        int establishLimit = LateEstablishPolicy.groundEstablishLimit(
                currentTurn, opponentLostPileSize,
                RandoConfig.MAX_ESTABLISH_LOCATIONS);

        for (AiBoardAnalyzer.LocationAnalysis loc : establishTargets) {
            if (remaining <= 0 || available.isEmpty()) break;
            if (establishCount >= establishLimit) break;
            if (loc.theirForceIcons <= 0
                    && projectedDrainByTarget.getOrDefault(
                        loc.location.getCardId(), 0.0f) <= 0.0f) continue;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            // Steve 2026-08-22: from turn five, while the opponent has fewer
            // than 20 cards lost, permit one additional drain platform. The
            // third site must be one exact, legal, affordable ability-4 body.
            if (establishCount
                    >= RandoConfig.MAX_ESTABLISH_LOCATIONS) {
                CardInfo lateBest = null;
                LateGroundCandidate lateFacts = null;
                for (CardInfo candidate : deployableHere) {
                    LateGroundCandidate assessed = assessLateGroundCandidate(
                            candidate, loc, remaining, currentTurn,
                            opponentLostPileSize);
                    if (!assessed.allowed()) {
                        continue;
                    }
                    if (lateBest == null
                            || candidate.getValueRatio()
                            > lateBest.getValueRatio()) {
                        lateBest = candidate;
                        lateFacts = assessed;
                    }
                }
                if (lateBest != null && lateFacts != null) {
                    addCardToPlan(plan, lateBest.card, loc, 2,
                            String.format(
                                    "Late establish at %s (%d icons, exact ability %.0f)",
                                    loc.location.getTitle(),
                                    loc.theirForceIcons,
                                    lateFacts.projectedAbility()),
                            lateFacts.exactCost());
                    remaining -= lateFacts.exactCost();
                    available.remove(lateBest);
                    establishCount++;
                }
                continue;
            }

            EstablishDrainPackage conditionalDrainPackage =
                    selectConditionalDrainPackage(
                            deployableHere, loc.location, remaining);
            if (conditionalDrainPackage != null) {
                for (int index = 0;
                        index < conditionalDrainPackage.cards().size();
                        index++) {
                    CardInfo card = conditionalDrainPackage.cards().get(index);
                    int exactCost = conditionalDrainPackage.costs().get(index);
                    addCardToPlan(plan, card.card, loc, 2,
                            String.format(
                                "Establish at %s (projected drain %.0f)",
                                loc.location.getTitle(),
                                conditionalDrainPackage.projectedDrain()),
                            exactCost);
                    remaining -= exactCost;
                    available.remove(card);
                }
                establishCount++;
                continue;
            }

            // Find cards with good ability (can defend against counter-deploy)
            List<CardInfo> withAbility = deployableHere.stream()
                .filter(c -> c.ability >= RandoConfig.ABILITY_THRESHOLD)
                .collect(Collectors.toList());

            if (withAbility.isEmpty()) {
                // No ability cards - need optimal combination
                OptimalCombination combo = findOptimalCombination(deployableHere, remaining, threshold, false);
                if (combo.hasAbility && !combo.isEmpty()) {
                    for (PhysicalCard card : combo.cards) {
                        CardInfo info = findCardInfo(available, card);
                        if (info != null) {
                            addCardToPlan(plan, card, loc, 2,
                                String.format("Establish at %s (%d icons)",
                                    loc.location.getTitle(), loc.theirForceIcons));
                            remaining -= info.cost;
                            available.removeIf(c -> c.card == card);
                        }
                    }
                    establishCount++;
                }
            } else {
                // Pick best ability card
                final int budget = remaining;  // Capture for lambda
                CardInfo best = withAbility.stream()
                    .filter(c -> c.cost <= budget)
                    .max(Comparator.comparingDouble(CardInfo::getValueRatio))
                    .orElse(null);

                if (best != null) {
                    // SOLO DEPLOY GUARD: Only send a character alone if they're powerful enough
                    // to survive a counter-deploy + battle. Characters below MIN_SOLO_DEPLOY_POWER
                    // (e.g., Jango at 4, Mara at 5) will get isolated and overwhelmed.
                    // Instead, fall through to find an optimal multi-character combo.
                    if (best.power >= RandoConfig.MIN_SOLO_DEPLOY_POWER) {
                        addCardToPlan(plan, best.card, loc, 2,
                            String.format("Establish at %s (%d icons, ability %d, power %d - solo OK)",
                                loc.location.getTitle(), loc.theirForceIcons, best.ability, best.power));
                        remaining -= best.cost;
                        available.remove(best);
                        establishCount++;
                    } else {
                        // Character is too weak to stand alone — try a group combo instead
                        LOG.info("📋 SOLO GUARD: {} (power {}) below MIN_SOLO_DEPLOY_POWER {} at {} — seeking group",
                            best.name, best.power, RandoConfig.MIN_SOLO_DEPLOY_POWER,
                            loc.location.getTitle());
                        OptimalCombination combo = findOptimalCombination(
                            deployableHere, remaining, RandoConfig.MIN_SOLO_DEPLOY_POWER, false);
                        if (!combo.isEmpty() && combo.totalPower >= RandoConfig.MIN_SOLO_DEPLOY_POWER) {
                            for (PhysicalCard card : combo.cards) {
                                CardInfo info = findCardInfo(available, card);
                                if (info != null) {
                                    addCardToPlan(plan, card, loc, 2,
                                        String.format("Establish at %s (group, power %d)",
                                            loc.location.getTitle(), combo.totalPower));
                                    remaining -= info.cost;
                                    available.removeIf(c -> c.card == card);
                                }
                            }
                            establishCount++;
                        } else {
                            // V59 SOLO GUARD FALLBACK: Before giving up on this weak character,
                            // try to route them to an OWN location where we already have a friendly.
                            // Deploying alongside an existing friendly converts a "solo deploy"
                            // into a reinforcement — the weak character arrives with backup.
                            // FIXES Issue #5 from peaceful-pike replay: Obi-Wan (power 5) stayed
                            // in hand all game because Mustafar was the only establish target
                            // and no group could deploy there.
                            PhysicalCard reinforcementSite = findFriendlyReinforcementSite(
                                best.card, loc.location);
                            if (reinforcementSite != null) {
                                AiBoardAnalyzer.LocationAnalysis reinLoc = null;
                                try {
                                    String oppId = currentGame.getOpponent(currentPlayerId);
                                    reinLoc = AiBoardAnalyzer.analyzeLocation(
                                        currentGame, currentPlayerId, oppId,
                                        reinforcementSite, currentSide);
                                } catch (Exception e) { /* ignore */ }
                                if (reinLoc != null) {
                                    addCardToPlan(plan, best.card, reinLoc, 2,
                                        String.format("V59 REINFORCE: %s joining friendlies at %s",
                                            best.name, reinforcementSite.getTitle()));
                                    remaining -= best.cost;
                                    available.remove(best);
                                    establishCount++;
                                    LOG.warn("📋 V59 SOLO GUARD REROUTE: {} (power {}) → {} (own site with friendlies)",
                                        best.name, best.power, reinforcementSite.getTitle());
                                } else {
                                    LOG.info("📋 SOLO GUARD: No valid group for {} — skipping location to avoid lone deployment",
                                        loc.location.getTitle());
                                }
                            } else {
                                LOG.info("📋 SOLO GUARD: No valid group for {} — skipping location to avoid lone deployment",
                                    loc.location.getTitle());
                                // Skip — deploying solo here is too risky
                            }
                        }
                    }
                }
            }
        }

        return plan;
    }

    private EstablishDrainPackage selectConditionalDrainPackage(
            List<CardInfo> candidates, PhysicalCard location, int budget) {
        if (candidates == null || candidates.isEmpty()
                || location == null || budget <= 0) return null;
        float currentDrain = ForceDrainProjection.projectedDamage(
                currentGame.getGameState(), currentGame,
                location, currentPlayerId);
        EstablishDrainPackage best = null;
        for (CardInfo activator : candidates) {
            if (activator.card == null || activator.card.getBlueprint() == null
                    || activator.card.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER
                    || activator.card.isUndercover()) {
                continue;
            }
            float projected = ForceDrainProjection
                    .projectedDamageAfterCharacterDeploy(
                        currentGame.getGameState(), currentGame,
                        location, currentPlayerId, activator.card);
            if (projected <= currentDrain) continue;
            Integer activatorCost = exactDeployCostAt(
                    activator.card, location);
            if (activatorCost == null || activatorCost > budget
                    || !canDeployPaidDirectly(
                        activator.card, location)) {
                continue;
            }

            if (activator.power >= RandoConfig.MIN_SOLO_DEPLOY_POWER
                    && activator.ability
                        >= RandoConfig.ABILITY_THRESHOLD) {
                EstablishDrainPackage solo = new EstablishDrainPackage(
                        List.of(activator), List.of(activatorCost),
                        projected, activatorCost,
                        activator.power, activator.ability);
                if (isBetterEstablishDrainPackage(solo, best)) best = solo;
            }

            for (CardInfo buddy : candidates) {
                if (buddy == activator || buddy.card == null
                        || buddy.card.getBlueprint() == null
                        || buddy.card.getBlueprint().getCardCategory()
                            != CardCategory.CHARACTER
                        || buddy.card.isUndercover()) {
                    continue;
                }
                Integer buddyCost = exactDeployCostAt(
                        buddy.card, location);
                int totalCost = buddyCost == null
                        ? Integer.MAX_VALUE : activatorCost + buddyCost;
                int totalPower = activator.power + buddy.power;
                int totalAbility = activator.ability + buddy.ability;
                if (buddyCost == null || totalCost > budget
                        || totalPower < RandoConfig.MIN_SOLO_DEPLOY_POWER
                        || totalAbility < RandoConfig.ABILITY_THRESHOLD
                        || !canDeployPaidDirectly(
                            buddy.card, location)) {
                    continue;
                }
                EstablishDrainPackage pair = new EstablishDrainPackage(
                        List.of(activator, buddy),
                        List.of(activatorCost, buddyCost), projected,
                        totalCost, totalPower, totalAbility);
                if (isBetterEstablishDrainPackage(pair, best)) best = pair;
            }
        }
        return best;
    }

    private boolean isBetterEstablishDrainPackage(
            EstablishDrainPackage candidate,
            EstablishDrainPackage current) {
        if (current == null
                || candidate.projectedDrain()
                    != current.projectedDrain()) {
            return current == null || candidate.projectedDrain()
                    > current.projectedDrain();
        }
        if (candidate.cards().size() != current.cards().size()) {
            return candidate.cards().size() < current.cards().size();
        }
        if (candidate.totalCost() != current.totalCost()) {
            return candidate.totalCost() < current.totalCost();
        }
        if (candidate.totalPower() != current.totalPower()) {
            return candidate.totalPower() > current.totalPower();
        }
        return candidate.totalAbility() > current.totalAbility();
    }

    private record EstablishDrainPackage(
            List<CardInfo> cards, List<Integer> costs,
            float projectedDrain, int totalCost,
            int totalPower, int totalAbility) {
    }

    private LateGroundCandidate assessLateGroundCandidate(
            CardInfo candidate,
            AiBoardAnalyzer.LocationAnalysis location,
            int remaining,
            int currentTurn,
            int opponentLostPileSize) {
        Integer exactCost = exactDeployCostAt(
                candidate.card, location.location);
        boolean exactEligible = exactCost != null
                && !AiCardHelper.isDeadCard(
                        candidate.card, currentGame, currentPlayerId)
                && canDeployPaidDirectly(candidate.card, location.location);
        float projectedAbility = exactAbility(candidate.card,
                candidate.isStarship || candidate.isVehicle);
        boolean allowed = LateEstablishPolicy.allowsWeakSolo(
                new LateEstablishPolicy.CandidateFacts(
                        currentTurn,
                        opponentLostPileSize,
                        location.isSite,
                        location.ourCardCount == 0
                                && location.theirCardCount == 0,
                        exactEligible,
                        exactCost != null && exactCost <= remaining,
                        projectedAbility));
        return allowed
                ? new LateGroundCandidate(
                        true, exactCost, projectedAbility)
                : LateGroundCandidate.denied();
    }

    private record LateGroundCandidate(
            boolean allowed, int exactCost, float projectedAbility) {
        private static LateGroundCandidate denied() {
            return new LateGroundCandidate(false, 0, 0.0f);
        }
    }

    /**
     * V59: Find an own location where we already have a friendly character,
     * where `card` can legally deploy. Used as a fallback when SOLO GUARD
     * fails at an establish target — reroute the weak character to where
     * it auto-joins an existing group.
     *
     * @param card the card we want to deploy
     * @param skipLocation skip this location (it's the failed establish target)
     * @return an own location with friendly presence, or null
     */
    private PhysicalCard findFriendlyReinforcementSite(PhysicalCard card, PhysicalCard skipLocation) {
        if (card == null || currentGame == null) return null;
        try {
            GameState gs = currentGame.getGameState();
            List<PhysicalCard> tops = gs.getTopLocations();
            if (tops == null) return null;
            PhysicalCard best = null;
            float bestOurPower = -1;
            for (PhysicalCard loc : tops) {
                if (loc == null || loc == skipLocation) continue;
                // Only sites (not systems) make sense for character reinforcement
                if (loc.getBlueprint() == null) continue;
                if (loc.getBlueprint().getCardSubtype() == null) continue;
                if (loc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SITE) continue;
                // Must have own friendly character here
                float ourPower = 0;
                boolean hasFriendly = false;
                List<PhysicalCard> atLoc = gs.getCardsAtLocation(loc);
                if (atLoc != null) {
                    for (PhysicalCard c : atLoc) {
                        if (c != null && currentPlayerId.equals(c.getOwner())
                            && c.getBlueprint() != null
                            && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                            hasFriendly = true;
                            if (c.getBlueprint().hasPowerAttribute()) {
                                Float p = c.getBlueprint().getPower();
                                if (p != null) ourPower += p;
                            }
                        }
                    }
                }
                if (!hasFriendly) continue;
                // Verify the deploying card can actually reach this location
                if (!canCardDeployHere(card, loc)) continue;
                // Prefer site with highest own power (most-consolidated group)
                if (ourPower > bestOurPower) {
                    bestOurPower = ourPower;
                    best = loc;
                }
            }
            return best;
        } catch (Exception e) {
            LOG.debug("V59 findFriendlyReinforcementSite error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * V59 helper: best-effort check that `card` can deploy to `location`.
     * Uses the same filter logic as filterDeployableCards but for a single card.
     */
    private boolean canCardDeployHere(PhysicalCard card, PhysicalCard location) {
        try {
            if (card == null || card.getBlueprint() == null) return false;
            List<CardInfo> single = new ArrayList<>();
            single.add(new CardInfo(card));
            List<CardInfo> deployable = filterDeployableCards(single, location);
            return !deployable.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate attack plan for enemy-held locations.
     */
    private DeploymentPlan generateAttackPlan(List<CardInfo> cards,
                                               List<AiBoardAnalyzer.LocationAnalysis> attackTargets,
                                               int forceAvailable, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Attack enemy " + domain);

        // Sort by our icons (stopping highest drains first)
        attackTargets.sort((a, b) -> Integer.compare(b.ourForceIcons, a.ourForceIcons));

        int remaining = forceAvailable;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : attackTargets) {
            if (remaining <= 0 || available.isEmpty()) break;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            // Need to beat enemy power with favorable threshold
            int powerNeeded = (int) loc.theirPower + RandoConfig.BATTLE_FAVORABLE_THRESHOLD;

            OptimalCombination combo = findOptimalCombination(deployableHere, remaining, powerNeeded, true);

            // SOLO GUARD ADJUSTED 2026-08-08 (passivity fix, m01683/panel): a SINGLE-card
            // attack combo must also satisfy weapon-adjusted dominance
            // (FormationSafety.DOMINANCE_MULTIPLE, the V172 standard) — a lone body that
            // merely clears the +BATTLE_FAVORABLE_THRESHOLD goal still gets isolated and
            // overwhelmed at an enemy-held location. Multi-card combos keep the existing
            // achievesGoal test unchanged.
            boolean comboApproved = combo.achievesGoal && !combo.isEmpty();
            if (comboApproved && combo.size() == 1) {
                float attackOppEff = loc.theirPower
                    + attackOppWeaponBonus(loc.location, currentPlayerId);
                boolean soloDominance = combo.totalPower + loc.ourPower
                    >= com.gempukku.swccgo.ai.models.common.strategy
                        .FormationSafety.DOMINANCE_MULTIPLE * attackOppEff;
                if (soloDominance) {
                    LOG.warn("📋 SOLO GUARD WAIVED: dominance ≥2x — deploy and battle ({} power {} + ours {} vs enemy eff {} at {})",
                        combo.cards.get(0).getTitle(), combo.totalPower,
                        (int) loc.ourPower, (int) attackOppEff, loc.location.getTitle());
                } else {
                    comboApproved = false;
                    LOG.info("📋 SOLO GUARD: single-card attack combo ({} power {}) below 2x enemy eff {} at {} — skip",
                        combo.cards.get(0).getTitle(), combo.totalPower,
                        (int) attackOppEff, loc.location.getTitle());
                }
            }
            // if (combo.achievesGoal && !combo.isEmpty()) {  // ADJUSTED 2026-08-08 (passivity fix, m01683/panel): single-card dominance floor above
            if (comboApproved) {
                for (PhysicalCard card : combo.cards) {
                    CardInfo info = findCardInfo(available, card);
                    if (info != null) {
                        addCardToPlan(plan, card, loc, 1,
                            String.format("Attack %s (%d vs %d power)",
                                loc.location.getTitle(), combo.totalPower, (int)loc.theirPower));
                        remaining -= info.cost;
                        available.removeIf(c -> c.card == card);
                    }
                }
            }
        }

        return plan;
    }

    private PilotPlanChoice selectBestPilotForShip(
            List<CardInfo> pilots,
            PhysicalCard ship,
            PhysicalCard simultaneousLocation,
            int forceAvailable,
            boolean simultaneousDeploy) {
        PilotPlanChoice best = null;
        for (CardInfo pilot : pilots) {
            if (!pilot.isPilot) continue;
            if (!SpaceDeploymentAllocationFactsReader
                    .isPlannerPilotEligible(
                            currentGame, pilot.card, ship)) {
                continue;
            }
            Integer exactCost;
            if (simultaneousDeploy) {
                if (simultaneousLocation == null
                        || !canDeployShipAndPilotTogether(
                                ship, pilot.card,
                                simultaneousLocation)) {
                    continue;
                }
                exactCost = exactSimultaneousDeployCost(
                        ship, pilot.card, simultaneousLocation);
            } else {
                if (!canDeployPilotAboard(pilot.card, ship)) {
                    continue;
                }
                exactCost = exactDeployCostAt(pilot.card, ship);
            }
            if (exactCost == null || exactCost > forceAvailable) {
                continue;
            }
            PilotPlanChoice candidate = new PilotPlanChoice(
                    pilot, exactCost,
                    pilotQualityTier(pilot, ship));
            if (isBetterPilotPlanChoice(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private int pilotQualityTier(CardInfo pilot, PhysicalCard ship) {
        return SpaceDeploymentAllocationFactsReader
                .plannerPilotQualityTier(
                        currentGame, pilot.card, ship);
    }

    private int pilotAddedPower(CardInfo pilot, PhysicalCard ship) {
        Float added = SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(currentGame, pilot.card, ship);
        return added != null && Float.isFinite(added) && added > 0.0f
                ? Math.round(added) : 0;
    }

    private boolean isBetterPilotPlanChoice(
            PilotPlanChoice candidate,
            PilotPlanChoice current) {
        if (current == null
                || candidate.qualityTier != current.qualityTier) {
            return current == null
                    || candidate.qualityTier > current.qualityTier;
        }
        if (candidate.pilot.ability != current.pilot.ability) {
            return candidate.pilot.ability > current.pilot.ability;
        }
        if (candidate.cost != current.cost) {
            return candidate.cost < current.cost;
        }
        if (candidate.pilot.power != current.pilot.power) {
            return candidate.pilot.power > current.pilot.power;
        }
        return candidate.pilot.card.getPermanentCardId()
                < current.pilot.card.getPermanentCardId();
    }

    private boolean canDeployPilotAboard(
            PhysicalCard pilot, PhysicalCard ship) {
        if (currentGame == null || currentPlayerId == null
                || pilot == null || ship == null
                || ship.getBlueprint() == null) {
            return false;
        }
        try {
            GameState gameState = currentGame.getGameState();
            return ship.getBlueprint()
                        .getValidPilotFilter(
                                currentPlayerId, currentGame,
                                ship, true)
                        .accepts(gameState,
                                currentGame.getModifiersQuerying(), pilot)
                    && Filters.hasAvailablePilotCapacity(pilot)
                        .accepts(gameState,
                                currentGame.getModifiersQuerying(), ship)
                    && Filters.deployableToTarget(
                                pilot, Filters.sameCardId(ship),
                                false, 0.0f)
                        .accepts(gameState,
                                currentGame.getModifiersQuerying(), pilot);
        } catch (Exception e) {
            return false;
        }
    }

    private static final class PilotPlanChoice {
        private final CardInfo pilot;
        private final int cost;
        private final int qualityTier;

        private PilotPlanChoice(
                CardInfo pilot, int cost, int qualityTier) {
            this.pilot = pilot;
            this.cost = cost;
            this.qualityTier = qualityTier;
        }
    }

    /**
     * V22: Generate plan to deploy a capital ship to the objective-relevant system (e.g., Bespin).
     * For TDIGWATT, getting the Executor to Bespin system is a top strategic priority.
     */
    private DeploymentPlan generateObjectiveCapitalPlan(List<CardInfo> starships,
                                                         List<CardInfo> characters,
                                                         List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                         int forceAvailable,
                                                         SwccgGame game, String playerId) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.ESTABLISH,
            "Deploy capital ship to objective system (Bespin)");

        // Find Bespin system on the board
        AiBoardAnalyzer.LocationAnalysis bespinSystem = null;
        for (AiBoardAnalyzer.LocationAnalysis loc : allLocations) {
            String title = loc.location.getTitle();
            if (title != null && (title.toLowerCase().contains("bespin") ||
                                   title.toLowerCase().contains("cloud city")) && loc.isSpace()) {
                bespinSystem = loc;
                break;
            }
        }

        if (bespinSystem == null) {
            LOG.warn("📋 V22 CAPITAL: Bespin system not found on board yet - skipping capital plan");
            return plan;
        }

        List<CardInfo> preferredShips = starships.stream()
            .sorted(Comparator.comparingInt((CardInfo s) -> s.power).reversed())
            .collect(Collectors.toList());

        if (preferredShips.isEmpty()) {
            LOG.warn("📋 V22 CAPITAL: No affordable capital ships in hand");
            return plan;
        }

        CardInfo bestShip = null;
        PilotPlanChoice bestPilot = null;
        int packageCost = 0;
        for (CardInfo ship : preferredShips) {
            float shipAbility = exactAbility(ship.card, true);
            PilotPlanChoice pilot = shipAbility < 4.0f
                    ? selectBestPilotForShip(
                            characters, ship.card,
                            bespinSystem.location, forceAvailable, true)
                    : null;
            Integer soloCost = exactDeployCostAt(
                    ship.card, bespinSystem.location);
            boolean legalSolo = soloCost != null
                    && soloCost <= forceAvailable
                    && canDeployPaidDirectly(
                            ship.card, bespinSystem.location);
            if (pilot != null || legalSolo) {
                bestShip = ship;
                bestPilot = pilot;
                packageCost = pilot != null ? pilot.cost : soloCost;
                break;
            }
        }
        if (bestShip == null) {
            LOG.warn("📋 V22 CAPITAL: No legal affordable capital package for Bespin");
            return plan;
        }

        addCardToPlan(plan, bestShip.card, bespinSystem, 1,
            String.format("V22: Deploy %s to Bespin for objective (power %d)", bestShip.name, bestShip.power));
        DeploymentInstruction shipInstruction =
                plan.getInstructions().get(0);
        shipInstruction.setDeployCost(packageCost);
        shipInstruction.setAbilityContribution(
                instructionAbilityContribution(bestShip.card));

        if (bestPilot != null) {
            CardInfo pilot = bestPilot.pilot;
            shipInstruction.setVerifiedCrewPackage(true);
            addCardToPlan(plan, pilot.card, bespinSystem, 2,
                String.format("V22: Deploy pilot %s for %s", pilot.name,
                    bestShip.name));
            DeploymentInstruction pilotInstruction =
                    plan.getInstructions().get(1);
            pilotInstruction.setDeployCost(0);
            pilotInstruction.setAbilityContribution(
                    instructionAbilityContribution(pilot.card));
            pilotInstruction.setPowerContribution(
                    pilotAddedPower(pilot, bestShip.card));
            pilotInstruction.setAboardShipName(bestShip.name);
            pilotInstruction.setAboardShipBlueprintId(
                    bestShip.blueprintId);
            pilotInstruction.setAboardShipCardId(
                    String.valueOf(bestShip.card.getCardId()));
            LOG.warn("📋 V22 CAPITAL: Planning {} + legal power pilot {} to Bespin",
                    bestShip.name, pilot.name);
        } else {
            LOG.warn("📋 V22 CAPITAL: Planning {} to Bespin (no pilot needed or legal)",
                    bestShip.name);
        }

        return plan;
    }

    /**
     * Generate RE-PILOT plan for unpiloted ships (Item #6).
     */
    private DeploymentPlan generateRepilotPlan(List<CardInfo> pilots, SwccgGame game,
                                                String playerId, int forceAvailable) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Re-pilot unpiloted ships");

        if (pilots.isEmpty()) return plan;

        // Find unpiloted ships in play
        List<PhysicalCard> unpilotedShips = findUnpilotedShipsInPlay(game, playerId);
        if (unpilotedShips.isEmpty()) return plan;

        int remaining = forceAvailable;
        List<CardInfo> availablePilots = new ArrayList<>(pilots);

        for (PhysicalCard ship : unpilotedShips) {
            if (remaining <= 0 || availablePilots.isEmpty()) break;

            PilotPlanChoice best = selectBestPilotForShip(
                    availablePilots, ship, null,
                    remaining, false);

            if (best != null) {
                CardInfo bestPilot = best.pilot;
                // Create instruction to deploy pilot to ship
                DeploymentInstruction inst = new DeploymentInstruction(
                    bestPilot.blueprintId, bestPilot.name,
                    String.valueOf(ship.getCardId()), ship.getTitle(),
                    1, String.format("Re-pilot %s", ship.getTitle())
                );
                inst.setCardPermanentCardId(bestPilot.card.getPermanentCardId());
                inst.setCardCurrentCardId(bestPilot.card.getCardId());
                inst.setDeployCost(best.cost);
                inst.setPowerContribution(
                        pilotAddedPower(bestPilot, ship));
                inst.setAbilityContribution(
                    instructionAbilityContribution(bestPilot.card));
                inst.setAboardShipName(ship.getTitle());
                inst.setAboardShipBlueprintId(
                        ship.getBlueprintId(true));
                inst.setAboardShipCardId(
                        String.valueOf(ship.getCardId()));
                plan.addInstruction(inst);

                remaining -= best.cost;
                availablePilots.remove(bestPilot);
            }
        }

        return plan;
    }

    private record PersistentPlanSelection(
            ScoredPlan scoredPlan,
            PersistentResponsePolicy.Obligation obligation) {
    }

    private boolean persistentResponseOverridesEarlyHold(
            PersistentPlanSelection selection) {
        return selection != null
            && selection.obligation().kind()
                == PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET;
    }

    /**
     * Thin mirror adapter. All engine facts, formation proof, and typed ranking
     * live in the shared PersistentResponsePlanAdapter.
     */
    private PersistentPlanSelection selectPersistentResponsePlan(
            List<ScoredPlan> allPlans,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int ordinaryBudget,
            int objectiveBudget,
            PersistentResponsePolicy.Snapshot snapshot) {
        if (allPlans == null || allPlans.isEmpty()
                || allLocations == null || snapshot == null
                || currentGame == null || currentPlayerId == null) {
            return null;
        }
        List<PersistentResponsePlanAdapter.PlanView<ScoredPlan>> views =
            allPlans.stream().map(this::persistentPlanView).toList();
        Optional<PersistentResponsePlanAdapter.Selection<ScoredPlan>>
            selected = PersistentResponsePlanAdapter.select(
                new PersistentResponsePlanAdapter.Input<>(
                    currentGame, currentPlayerId, objectiveAnalyzer,
                    snapshot, allLocations, ordinaryBudget,
                    objectiveBudget, views));
        return selected.map(value -> new PersistentPlanSelection(
                value.source(), value.obligation())).orElse(null);
    }

    private PersistentResponsePlanAdapter.PlanView<ScoredPlan>
            persistentPlanView(ScoredPlan scoredPlan) {
        List<PersistentResponsePlanAdapter.InstructionView> instructions =
            scoredPlan.plan.getInstructions().stream()
                .map(this::persistentInstructionView).toList();
        return new PersistentResponsePlanAdapter.PlanView<>(
            scoredPlan, scoredPlan.domain,
            String.valueOf(scoredPlan.plan.getStrategy()),
            instructions);
    }

    private PersistentResponsePlanAdapter.InstructionView
            persistentInstructionView(
                    DeploymentInstruction instruction) {
        return new PersistentResponsePlanAdapter.InstructionView(
            instruction.getCardPermanentCardId(),
            instruction.getCardCurrentCardId(),
            instruction.getTargetLocationId(),
            instruction.getPriority());
    }

    // PLAN SCORING (Item #21)
    // =========================================================================

    /**
     * Score a deployment plan.
     *
     * Ported from Python _score_plan().
     */
    private float scorePlan(DeploymentPlan plan, List<AiBoardAnalyzer.LocationAnalysis> locations, int turn) {
        return scorePlan(plan, locations, turn, null);
    }

    private float scorePlan(DeploymentPlan plan,
                            List<AiBoardAnalyzer.LocationAnalysis> locations,
                            int turn, PolicyResult... objectiveAdjuncts) {
        if (plan == null || plan.getInstructions().isEmpty()) {
            return 0.0f;
        }

        List<DeployPlanRankingPolicy.InstructionFacts> instructionFacts = new ArrayList<>();
        Map<String, Integer> powerByLocation = new HashMap<>();
        Map<String, Float> abilityByLocation = new HashMap<>();
        Map<String, List<PhysicalCard>> plannedCardsByLocation =
                new HashMap<>();
        List<PolicyResult> automaticAdjuncts = new ArrayList<>();
        boolean battleSynergyAwarded = false;

        int instructionIndex = 0;
        for (DeploymentInstruction inst : plan.getInstructions()) {
            instructionFacts.add(new DeployPlanRankingPolicy.InstructionFacts(
                "instruction-" + instructionIndex++, inst.getPowerContribution()));

            // Track by location
            if (inst.getTargetLocationId() != null) {
                powerByLocation.merge(inst.getTargetLocationId(), inst.getPowerContribution(), Integer::sum);
                abilityByLocation.merge(inst.getTargetLocationId(),
                    inst.getAbilityContribution(), Float::sum);
                if (inst.getCardCurrentCardId() != null
                        && currentGame != null
                        && currentGame.getGameState() != null) {
                    PhysicalCard plannedCard = currentGame.getGameState()
                            .findCardById(inst.getCardCurrentCardId());
                    if (plannedCard != null) {
                        plannedCardsByLocation
                            .computeIfAbsent(inst.getTargetLocationId(),
                                ignored -> new ArrayList<>())
                            .add(plannedCard);
                    }
                }
            }
        }

        // Analyze each target location
        List<DeployPlanRankingPolicy.LocationFacts> locationFacts = new ArrayList<>();
        int locationIndex = 0;
        for (Map.Entry<String, Integer> entry : powerByLocation.entrySet()) {
            String locId = entry.getKey();
            int plannedPower = entry.getValue();
            float plannedAbility = abilityByLocation.getOrDefault(
                locId, 0.0f);

            // Find location
            AiBoardAnalyzer.LocationAnalysis targetLoc = null;
            for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                if (String.valueOf(loc.location.getCardId()).equals(locId)) {
                    targetLoc = loc;
                    break;
                }
            }

            if (targetLoc == null) continue;

            float postOurPower = targetLoc.ourPower + plannedPower;
            float postOurAbility = targetLoc.ourAbility + plannedAbility;

            // V311: prefer, but never require, a complete source-proven
            // extra-draw pair at the one battle this plan is building.
            boolean battleRelevant = targetLoc.theirPower > 0.0f
                    || locId.equals(
                        plan.getPlannedBattleTargetLocationId());
            if (!battleSynergyAwarded && battleRelevant
                    && currentGame != null
                    && currentPlayerId != null) {
                ProspectiveBattleSynergy.Projection before =
                    ProspectiveBattleSynergy.projectAtLocation(
                        currentGame, currentPlayerId,
                        targetLoc.location, List.of());
                ProspectiveBattleSynergy.Projection after =
                    ProspectiveBattleSynergy.projectAtLocation(
                        currentGame, currentPlayerId,
                        targetLoc.location,
                        plannedCardsByLocation.getOrDefault(
                            locId, Collections.emptyList()));
                int gained = ProspectiveBattleSynergy
                    .additionalDrawsGained(currentGame, before, after);
                PolicyResult synergy = DeployPlanRankingPolicy
                    .evaluateBattleDestinySynergy(
                        "battle-synergy-" + locationIndex,
                        true, gained);
                if (!synergy.operations().isEmpty()) {
                    automaticAdjuncts.add(synergy);
                    battleSynergyAwarded = true;
                    LOG.info("V311 PLAN PAIR: {} gains {} source-proven battle destiny draw(s), +100 plan preference",
                        targetLoc.location.getTitle(), gained);
                }
            }

            // V22: Objective-relevant location bonus for plan scoring
            boolean objectiveRelevant = false;
            float objectiveBonus = 0.0f;
            if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
                String locTitle = targetLoc.location.getTitle();
                float objBonus = objectiveAnalyzer
                        .isIWantThatMapObjectiveFamily()
                        && !objectiveAnalyzer.isFlipped()
                        ? objectiveAnalyzer
                            .getDeployLocationObjectiveBonus(
                                    currentGame, currentPlayerId,
                                    plannedCardsByLocation.get(locId),
                                    targetLoc.location)
                        : objectiveAnalyzer.getLocationObjectiveBonus(
                                targetLoc.location, currentGame,
                                currentPlayerId);
                if (locTitle != null && objBonus != 0.0f) {
                    objectiveRelevant = true;
                    objectiveBonus = objBonus;
                    LOG.warn("V22 PLAN SCORE: {} is objective-relevant, +{} to plan score", locTitle, objBonus);
                }
            }

            float projectedDrain = targetLoc.theirForceIcons;
            if (currentGame != null
                    && currentGame.getGameState() != null) {
                projectedDrain = ForceDrainProjection.projectedDamage(
                        currentGame.getGameState(), currentGame,
                        targetLoc.location, currentPlayerId);
                for (PhysicalCard plannedCard
                        : plannedCardsByLocation.getOrDefault(
                            locId, Collections.emptyList())) {
                    projectedDrain = Math.max(projectedDrain,
                            ForceDrainProjection
                                .projectedDamageAfterCharacterDeploy(
                                    currentGame.getGameState(), currentGame,
                                    targetLoc.location, currentPlayerId,
                                    plannedCard));
                }
            }
            if (projectedDrain <= 0.0f) {
                projectedDrain = targetLoc.theirForceIcons;
            }
            int projectedDrainDamage = Math.max(
                    0, Math.round(projectedDrain));

            boolean formationPenaltyExempt = hasPlannedSpyAtTarget(plan, locId);
            boolean triggerKnowable = false;
            boolean exposureProven = false;
            float strongestReactPower = 0.0f;
            if (targetLoc.theirCardCount == 0
                    && !formationPenaltyExempt
                    && currentGame != null
                    && currentPlayerId != null) {
                PublicImmediateReactAnalyzer.Exposure exposure =
                    PublicImmediateReactAnalyzer.analyze(
                        currentGame, currentPlayerId,
                        targetLoc.location, true);
                triggerKnowable = exposure.triggerKnowable();
                exposureProven = exposure.exposureProven();
                strongestReactPower =
                    exposure.strongestMoverEffectivePower();
            }

            locationFacts.add(new DeployPlanRankingPolicy.LocationFacts(
                "location-" + locationIndex++, postOurPower,
                postOurAbility, targetLoc.theirPower,
                targetLoc.ourForceIcons, projectedDrainDamage,
                targetLoc.theirCardCount, objectiveRelevant, objectiveBonus,
                triggerKnowable, exposureProven, strongestReactPower,
                formationPenaltyExempt));
        }

        PolicyResult core = DeployPlanRankingPolicy.evaluate(
                instructionFacts, locationFacts);
        PolicyResult[] adjuncts = objectiveAdjuncts != null
                ? objectiveAdjuncts : new PolicyResult[0];
        List<PolicyResult> results = new ArrayList<>();
        results.add(core);
        results.addAll(Arrays.asList(adjuncts));
        results.addAll(automaticAdjuncts);
        return DeployPlanRankingPolicy.apply(
                0.0f, results.toArray(new PolicyResult[0]));
    }

    private float scoreObjectiveCapitalPlan(DeploymentPlan plan,
                                             List<AiBoardAnalyzer.LocationAnalysis> locations,
                                             int turn) {
        return scorePlan(plan, locations, turn,
            DeployPlanRankingPolicy.evaluateAdjunct(
                new DeployPlanRankingPolicy.AdjunctFacts(
                    "objective-capital-bespin", true)));
    }

    private float scoreObjectiveFlipGateFormationPlan(
            DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations,
            int turn) {
        ObjectiveAnalyzer.ObjectivePlaybook playbook =
            objectiveAnalyzer != null ? objectiveAnalyzer.getActivePlaybook() : null;
        float objectiveBonus = playbook != null
            ? playbook.weights.deployFlipGateSite : 0.0f;
        return scorePlan(plan, locations, turn,
            DeployPlanRankingPolicy.evaluateFlipGateFormation(
                new DeployPlanRankingPolicy.FlipGateFormationFacts(
                    "objective-flip-gate-formation", true, objectiveBonus)));
    }

    private boolean isObjectiveFlipGateFormationPlan(DeploymentPlan plan) {
        return plan != null && plan.getReason() != null
            && (plan.getReason().startsWith(
                    "V297 objective flip-gate formation")
                || plan.getReason().startsWith(
                    "Objective counted-operative formations")
                || plan.getReason().startsWith(
                    "Objective counted-type completion"));
    }

    private boolean hasPlannedSpyAtTarget(
            DeploymentPlan plan, String targetLocationId) {
        if (plan == null || targetLocationId == null
                || currentGame == null
                || currentGame.getGameState() == null) {
            return false;
        }
        for (DeploymentInstruction instruction : plan.getInstructions()) {
            if (!targetLocationId.equals(instruction.getTargetLocationId())
                    || instruction.getCardCurrentCardId() == null) {
                continue;
            }
            try {
                PhysicalCard card = currentGame.getGameState().findCardById(
                    instruction.getCardCurrentCardId());
                if (card != null && card.getBlueprint() != null
                        && card.getBlueprint().hasKeyword(Keyword.SPY)) {
                    return true;
                }
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Select the best plan from all generated plans.
     */
    private DeploymentPlan selectBestPlan(List<ScoredPlan> allPlans,
                                           List<CardInfo> locationDeploys,
                                           int turn, int lifeForce,
                                           PersistentPlanSelection persistentSelection) {
        if (allPlans.isEmpty()) {
            // Just deploy locations if nothing else
            if (!locationDeploys.isEmpty()) {
                DeploymentPlan locationPlan = new DeploymentPlan(
                    DeployStrategy.DEPLOY_LOCATIONS, "Deploy locations");
                for (CardInfo loc : locationDeploys) {
                    DeploymentInstruction inst = new DeploymentInstruction(
                        loc.blueprintId, loc.name, null, null, 0, "Deploy location");
                    inst.setCardPermanentCardId(loc.card.getPermanentCardId());
                    inst.setCardCurrentCardId(loc.card.getCardId());
                    inst.setDeployCost(loc.cost);
                    locationPlan.addInstruction(inst);
                }
                return locationPlan;
            }
            return null;
        }

        // Sort by score (highest first)
        Collections.sort(allPlans);

        LOG.info("📋 PLAN COMPARISON ({} plans generated):", allPlans.size());
        for (int i = 0; i < Math.min(5, allPlans.size()); i++) {
            ScoredPlan sp = allPlans.get(i);
            LOG.info("   {}. {} - score: {:.0f}, cards: {}",
                i + 1, sp.domain, sp.score, sp.plan.getInstructions().size());
        }

        ScoredPlan best = persistentSelection != null
            ? persistentSelection.scoredPlan() : allPlans.get(0);

        // Add location deploys to the best plan
        DeploymentPlan finalPlan = new DeploymentPlan(best.plan.getStrategy(), best.plan.getReason());
        for (CardInfo loc : locationDeploys) {
            DeploymentInstruction inst = new DeploymentInstruction(
                loc.blueprintId, loc.name, null, null, 0, "Deploy location first");
            inst.setCardPermanentCardId(loc.card.getPermanentCardId());
            inst.setCardCurrentCardId(loc.card.getCardId());
            inst.setDeployCost(loc.cost);
            finalPlan.addInstruction(inst);
        }
        for (DeploymentInstruction inst : best.plan.getInstructions()) {
            finalPlan.addInstruction(inst);
        }
        finalPlan.copyCountedDeployPackageFrom(best.plan);
        finalPlan.copyBattleIntentFrom(best.plan);
        if (persistentSelection != null) {
            finalPlan.setPersistentResponseObligation(
                persistentSelection.obligation());
        }

        return finalPlan;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void addCardToPlan(DeploymentPlan plan, PhysicalCard card,
                               AiBoardAnalyzer.LocationAnalysis loc, int priority, String reason) {
        addCardToPlan(plan, card, loc, priority, reason, null);
    }

    private void addCardToPlan(DeploymentPlan plan, PhysicalCard card,
                               AiBoardAnalyzer.LocationAnalysis loc, int priority,
                               String reason, Integer exactDeployCost) {
        SwccgCardBlueprint bp = card.getBlueprint();
        int cost = 0;
        int power = 0;

        if (bp != null) {
            try {
                Float c = bp.getDeployCost();
                cost = c != null ? c.intValue() : 0;
            } catch (UnsupportedOperationException e) {}

            if (bp.hasPowerAttribute()) {
                Float p = bp.getPower();
                power = p != null ? p.intValue() : 0;
            }
        }
        if (exactDeployCost != null) {
            cost = exactDeployCost;
        }

        DeploymentInstruction inst = new DeploymentInstruction(
            card.getBlueprintId(true), card.getTitle(),
            String.valueOf(loc.location.getCardId()), loc.location.getTitle(),
            priority, reason
        );
        inst.setCardPermanentCardId(card.getPermanentCardId());
        inst.setCardCurrentCardId(card.getCardId());
        inst.setDeployCost(cost);
        inst.setPowerContribution(power);
        inst.setAbilityContribution(
            instructionAbilityContribution(card));
        plan.addInstruction(inst);
    }

    private float instructionAbilityContribution(PhysicalCard card) {
        if (card == null || card.getBlueprint() == null) {
            return 0;
        }
        CardCategory category = card.getBlueprint().getCardCategory();
        boolean includePermanentPilots = category == CardCategory.STARSHIP
            || category == CardCategory.VEHICLE;
        float ability = exactAbility(card, includePermanentPilots);
        return Float.isFinite(ability) && ability > 0.0f
            ? ability : 0.0f;
    }

    private CardInfo findCardInfo(List<CardInfo> cards, PhysicalCard card) {
        return cards.stream().filter(c -> c.card == card).findFirst().orElse(null);
    }

    private AiBoardAnalyzer.LocationAnalysis findBestTarget(LocationCategories categories,
                                                             boolean isGround, int threshold) {
        // Priority: bleed > losing > attack > establish
        List<AiBoardAnalyzer.LocationAnalysis> targets = new ArrayList<>();
        targets.addAll(categories.bleedLocations);
        targets.addAll(categories.losingLocations);
        targets.addAll(categories.attackTargets);
        targets.addAll(categories.establishTargets);

        return targets.stream()
            .filter(loc -> isGround ? loc.isGround() : loc.isSpace())
            .max(Comparator.comparingInt(loc -> loc.theirForceIcons + loc.ourForceIcons))
            .orElse(null);
    }

    private List<PhysicalCard> findUnpilotedShipsInPlay(SwccgGame game, String playerId) {
        List<PhysicalCard> unpiloted = new ArrayList<>();

        // Check all locations for unpiloted ships
        GameState gameState = game.getGameState();

        // Get all locations in play and check cards at each
        for (PhysicalCard location : gameState.getLocationsInOrder()) {
            if (location == null) continue;

            // Get all cards at this location
            List<PhysicalCard> cardsAtLocation = gameState.getCardsAtLocation(location);
            for (PhysicalCard card : cardsAtLocation) {
                if (card.getOwner().equals(playerId) &&
                    card.getBlueprint() != null &&
                    card.getBlueprint().getCardCategory() == CardCategory.STARSHIP) {

                    try {
                        if (Filters.unpiloted.accepts(
                                gameState,
                                game.getModifiersQuerying(), card)) {
                            unpiloted.add(card);
                        }
                    } catch (Exception ignored) {
                        // An unreadable ship is not proof that it needs a pilot.
                    }
                }
            }
        }

        return unpiloted;
    }

    private void logHandDetails(List<CardInfo> characters, List<CardInfo> starships, List<CardInfo> vehicles) {
        if (!characters.isEmpty()) {
            StringBuilder sb = new StringBuilder("   📋 Characters: [");
            for (CardInfo c : characters) {
                sb.append(c.toString()).append(", ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
        if (!starships.isEmpty()) {
            StringBuilder sb = new StringBuilder("   🚀 Starships: [");
            for (CardInfo s : starships) {
                sb.append(s.toString()).append(", ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
        if (!vehicles.isEmpty()) {
            StringBuilder sb = new StringBuilder("   🚗 Vehicles: [");
            for (CardInfo v : vehicles) {
                sb.append(v.toString()).append(", ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
    }

    private void logBoardAnalysis(List<AiBoardAnalyzer.LocationAnalysis> locations) {
        LOG.info("   📍 Board locations ({}):", locations.size());
        for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
            String domain = loc.isSpace() ? "SPACE" : "GROUND";
            String status = "";
            if (loc.ourPower > 0 && loc.theirPower > 0) {
                status = loc.ourPower > loc.theirPower ? "WINNING" : "LOSING";
            } else if (loc.ourPower > 0) {
                status = "CONTROLLED";
            } else if (loc.theirPower > 0) {
                status = "ENEMY";
            } else {
                status = "EMPTY";
            }
            LOG.info("      {} [{}] {} - us:{} them:{} icons:{}/{}",
                loc.location.getTitle(), domain, status,
                (int)loc.ourPower, (int)loc.theirPower,
                loc.ourForceIcons, loc.theirForceIcons);
        }
    }

    private void logFinalPlan(DeploymentPlan plan) {
        String strategyName = plan.getStrategy() != null ?
            plan.getStrategy().getValue().toLowerCase() : "unknown";
        LOG.info("📋 ═══════════════════════════════════════════════════════════════");
        LOG.info("📋 FINAL PLAN: {} ({} deployments)", strategyName, plan.getInstructions().size());
        LOG.info("📋 ═══════════════════════════════════════════════════════════════");

        int num = 1;
        for (DeploymentInstruction inst : plan.getInstructions()) {
            String target = inst.getTargetLocationName() != null ?
                inst.getTargetLocationName() : "table";
            LOG.info("   {}. {} -> {} : {}", num++, inst.getCardName(), target, inst.getReason());
        }
    }

    private DeploymentPlan createHoldBackPlan(String reason) {
        return new DeploymentPlan(DeployStrategy.HOLD_BACK, reason);
    }

    /**
     * Generate all combinations of size k from list.
     */
    private <T> List<List<T>> combinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        combinationsHelper(list, k, 0, new ArrayList<>(), result);
        return result;
    }

    private <T> void combinationsHelper(List<T> list, int k, int start,
                                         List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            combinationsHelper(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // =========================================================================
    // PUBLIC API (for DeployEvaluator)
    // =========================================================================

    /**
     * Get score for a specific card deployment.
     */
    public PlanScore getCardScore(String blueprintId, int currentForce, List<String> availableBlueprints) {
        if (currentPlan == null) {
            return new PlanScore(0.0f, "No active plan");
        }

        DeploymentInstruction instruction = currentPlan.getInstructionForCard(blueprintId);
        if (instruction != null) {
            float score = 100.0f - (instruction.getPriority() * 10);
            return new PlanScore(score, "IN PLAN: " + instruction.getReason());
        }

        if (currentPlan.isPlanComplete() || currentPlan.isForceAllowExtras()) {
            return new PlanScore(25.0f, "Extra action (plan complete)");
        }

        return new PlanScore(-50.0f, "NOT in plan - saving force for planned cards");
    }

    /**
     * Record that a card was deployed.
     */
    public void recordDeployment(String blueprintId) {
        if (currentPlan != null) {
            currentPlan.recordDeployment(blueprintId);
        }
    }

    public void recordDeployment(int permanentCardId, int currentCardId, String blueprintId) {
        if (currentPlan != null) {
            currentPlan.recordDeployment(permanentCardId, currentCardId, blueprintId);
        }
    }

    /**
     * Get plan summary.
     */
    public String getPlanSummary() {
        if (currentPlan == null) {
            return "No plan";
        }
        return String.format("%s: %s (%d deployments)",
            currentPlan.getStrategy().getValue(),
            currentPlan.getReason(),
            currentPlan.getInstructions().size());
    }

    /**
     * Check if a card can deploy to a specific location based on its gametext restrictions.
     * Returns true if the card appears to have no restrictions OR if it can deploy to this location type.
     * Returns false if the card has "Deploys only" restrictions that don't match the location.
     */
    private boolean canDeployToLocation(PhysicalCard card, PhysicalCard location) {
        if (card == null || card.getBlueprint() == null) return true;
        if (location == null) return true;

        // V312: printed 'deploys only' text is not engine legality. In replay
        // 72361 it admitted Chewie to The Works without Light icons/presence,
        // stranding the whole buddy packet behind a nonexistent first target.
        // Let the engine retain spy/presence exceptions; do not ban zero-icon sites.
        if (card.getBlueprint().getCardCategory() == CardCategory.CHARACTER
                && currentGame != null) {
            return canDeployPaidDirectly(card, location);
        }

        String gametext = card.getBlueprint().getGameText();
        if (gametext == null || gametext.isEmpty()) return true;

        String gametextLower = gametext.toLowerCase();

        // If no deployment restriction, card can deploy anywhere appropriate
        if (!gametextLower.contains("deploys only")) {
            return true;
        }

        // Card has deployment restrictions - check if location matches
        String locationTitle = location.getTitle();
        if (locationTitle == null) return false;
        String locationLower = locationTitle.toLowerCase();

        // Extract the restriction part (e.g., "Deploys only on Falcon, Hoth or Cloud City")
        int deploysOnlyIdx = gametextLower.indexOf("deploys only");
        String restrictionPart = gametextLower.substring(deploysOnlyIdx);
        int periodIdx = restrictionPart.indexOf('.');
        if (periodIdx > 0) {
            restrictionPart = restrictionPart.substring(0, periodIdx);
        }

        LOG.debug("📋 Checking deploy restriction for {}: '{}' vs location '{}'",
            card.getTitle(), restrictionPart, locationTitle);

        // =======================================================
        // SPECIAL CASE: Cards that deploy ON characters/ships, not TO locations
        // These cards (like Elom, weapons, devices) deploy on other cards,
        // not directly to locations. Allow them - the game engine will present
        // the correct deployment options (e.g., deploy on a character at location).
        // =======================================================
        if (restrictionPart.contains("deploys only on")) {
            String afterOn = restrictionPart.substring(restrictionPart.indexOf("deploys only on") + 15).trim();
            // Check if it's deploying on a character/ship type rather than a location
            // Character types: rebel, alien, imperial, droid, jedi, sith, warrior, pilot, etc.
            // Ship types: starship, capital, squadron, etc.
            boolean deploysOnCard = afterOn.startsWith("a ") || afterOn.startsWith("an ") ||
                afterOn.startsWith("your ") || afterOn.startsWith("opponent") ||
                afterOn.contains("rebel") || afterOn.contains("alien") ||
                afterOn.contains("imperial") || afterOn.contains("droid") ||
                afterOn.contains("jedi") || afterOn.contains("sith") ||
                afterOn.contains("character") || afterOn.contains("warrior") ||
                afterOn.contains("pilot") || afterOn.contains("smuggler") ||
                afterOn.contains("starship") || afterOn.contains("vehicle") ||
                afterOn.contains("capital") || afterOn.contains("squadron");

            if (deploysOnCard) {
                // This card deploys on another card, not directly to a location
                // Return true to allow it - game engine will handle the actual deployment
                LOG.debug("📋 {} deploys ON a card - allowing (game engine will handle)",
                    card.getTitle());
                return true;
            }
        }

        // Check common location types
        if (restrictionPart.contains("hoth") && !locationLower.contains("hoth")) {
            LOG.info("📋 {} cannot deploy to {} (requires Hoth)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("cloud city") && !locationLower.contains("cloud city")) {
            LOG.info("📋 {} cannot deploy to {} (requires Cloud City)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("tatooine") && !locationLower.contains("tatooine")) {
            LOG.info("📋 {} cannot deploy to {} (requires Tatooine)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("endor") && !locationLower.contains("endor")) {
            LOG.info("📋 {} cannot deploy to {} (requires Endor)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("dagobah") && !locationLower.contains("dagobah")) {
            LOG.info("📋 {} cannot deploy to {} (requires Dagobah)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("death star") && !locationLower.contains("death star")) {
            LOG.info("📋 {} cannot deploy to {} (requires Death Star)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("coruscant") && !locationLower.contains("coruscant")) {
            LOG.info("📋 {} cannot deploy to {} (requires Coruscant)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("naboo") && !locationLower.contains("naboo")) {
            LOG.info("📋 {} cannot deploy to {} (requires Naboo)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("bespin") && !locationLower.contains("bespin")) {
            LOG.info("📋 {} cannot deploy to {} (requires Bespin)", card.getTitle(), locationTitle);
            return false;
        }

        // Check for starship-only deployments (can't deploy to site)
        if ((restrictionPart.contains("falcon") || restrictionPart.contains("starship") ||
             restrictionPart.contains("capital starship")) &&
            !restrictionPart.contains("hoth") && !restrictionPart.contains("cloud city") &&
            !restrictionPart.contains("tatooine") && !restrictionPart.contains("site")) {
            // Card only deploys to starships, not locations
            LOG.info("📋 {} cannot deploy to {} (requires starship)", card.getTitle(), locationTitle);
            return false;
        }

        // If we get here and the card has restrictions we didn't recognize,
        // be conservative and allow it (let the game engine handle it)
        return true;
    }

    /**
     * Filter cards to only those that can deploy to the target location.
     */
    private List<CardInfo> filterDeployableCards(List<CardInfo> cards, PhysicalCard targetLocation) {
        List<CardInfo> deployable = new ArrayList<>();
        for (CardInfo info : cards) {
            if (canDeployToLocation(info.card, targetLocation)) {
                deployable.add(info);
            }
        }
        return deployable;
    }

    /**
     * Simple score/reason holder.
     */
    public static class PlanScore {
        public final float score;
        public final String reason;

        public PlanScore(float score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }

    /**
     * Location categories for planning.
     */
    private static class LocationCategories {
        List<AiBoardAnalyzer.LocationAnalysis> losingLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> winningLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> bleedLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> establishTargets = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> attackTargets = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> weakPresenceLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> crushableLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> drainingLocations = new ArrayList<>();
    }
}
