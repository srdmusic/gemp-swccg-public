package com.gempukku.swccgo.ai.models.chosenone.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.common.phase.DeployPlanRankingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.phase.LateEstablishPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePlanAdapter;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.EndorOperationsTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.strategy.MovePredicates;
import com.gempukku.swccgo.ai.models.common.strategy.PublicImmediateReactAnalyzer;
import com.gempukku.swccgo.ai.models.chosenone.RandoConfig;
import com.gempukku.swccgo.ai.models.chosenone.RandoLogger;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;

import org.apache.logging.log4j.Logger;

import java.util.*;
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
            if (!lastPlanHadActiveFlipGate && hasActiveFlipGate) {
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

        currentPlan = bestPlan;
        lastPlanTurn = currentTurn;
        lastPlanHadActiveFlipGate = hasActiveFlipGate;
        lastPlanHadFlipGateActorInHand = hasFlipGateActorInHand;
        lastPlanPreFlipProgressFingerprint = preFlipProgressFingerprint;
        lastPlanPersistentResponseRevision = persistentRevision;
        logFinalPlan(bestPlan);

        return currentPlan;
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
                    && canDeployDirectly(ship.card, endor.location)) {
                EndorSystemPackage candidate = new EndorSystemPackage(
                        ship, null, soloCost, shipAbility,
                        ship.power);
                if (isBetterEndorSystemPackage(candidate, best)) {
                    best = candidate;
                }
            }
            for (CardInfo pilot : pilots) {
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
                        ship.power + pilot.power);
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
                    ship, pilot, true, 0.0f,
                    Filters.sameCardId(location), true, 0.0f)
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
        if (current == null || candidate.cost != current.cost) {
            return current == null || candidate.cost < current.cost;
        }
        int candidateCards = candidate.pilot == null ? 1 : 2;
        int currentCards = current.pilot == null ? 1 : 2;
        if (candidateCards != currentCards) {
            return candidateCards < currentCards;
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

        private EndorSystemPackage(
                CardInfo ship,
                CardInfo pilot,
                int cost,
                float combinedAbility,
                int combinedPower) {
            this.ship = ship;
            this.pilot = pilot;
            this.cost = cost;
            this.combinedAbility = combinedAbility;
            this.combinedPower = combinedPower;
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
            if (spaceCombo.achievesGoal && !spaceCombo.isEmpty()) {
                for (PhysicalCard card : spaceCombo.cards) {
                    addCardToPlan(combinedPlan, card, bestSpaceTarget, 1,
                        "Combined: space attack");
                }
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

        // Sort by opponent icons, then stable location title.
        establishTargets.sort((a, b) -> {
            int scoreA = a.theirForceIcons;
            int scoreB = b.theirForceIcons;
            int iconOrder = Integer.compare(scoreB, scoreA);
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
            if (loc.theirForceIcons <= 0) continue;

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

        int remaining = forceAvailable;
        final int budget = remaining;
        List<CardInfo> affordable = starships.stream()
            .filter(s -> s.cost <= budget)
            .sorted(Comparator.comparingInt((CardInfo s) -> s.power).reversed())
            .collect(Collectors.toList());

        if (affordable.isEmpty()) {
            LOG.warn("📋 V22 CAPITAL: No affordable capital ships in hand");
            return plan;
        }

        CardInfo bestShip = affordable.get(0);
        addCardToPlan(plan, bestShip.card, bespinSystem, 1,
            String.format("V22: Deploy %s to Bespin for objective (power %d)", bestShip.name, bestShip.power));
        remaining -= bestShip.cost;

        // Try to add a pilot
        final int pilotBudget = remaining;
        List<CardInfo> affordablePilots = characters.stream()
            .filter(c -> c.isPilot && c.cost <= pilotBudget)
            .sorted(Comparator.comparingInt((CardInfo c) -> c.ability).reversed())
            .collect(Collectors.toList());

        if (!affordablePilots.isEmpty()) {
            CardInfo pilot = affordablePilots.get(0);
            addCardToPlan(plan, pilot.card, bespinSystem, 1,
                String.format("V22: Deploy pilot %s for %s", pilot.name, bestShip.name));
            LOG.warn("📋 V22 CAPITAL: Planning {} + pilot {} to Bespin", bestShip.name, pilot.name);
        } else {
            LOG.warn("📋 V22 CAPITAL: Planning {} to Bespin (no affordable pilot)", bestShip.name);
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

            // Find best pilot for this ship
            final int budget = remaining;  // Capture for lambda
            CardInfo bestPilot = availablePilots.stream()
                .filter(p -> p.cost <= budget)
                .max(Comparator.comparingInt(p -> p.ability))
                .orElse(null);

            if (bestPilot != null) {
                // Create instruction to deploy pilot to ship
                DeploymentInstruction inst = new DeploymentInstruction(
                    bestPilot.blueprintId, bestPilot.name,
                    String.valueOf(ship.getCardId()), ship.getTitle(),
                    1, String.format("Re-pilot %s", ship.getTitle())
                );
                inst.setCardPermanentCardId(bestPilot.card.getPermanentCardId());
                inst.setCardCurrentCardId(bestPilot.card.getCardId());
                inst.setDeployCost(bestPilot.cost);
                inst.setPowerContribution(bestPilot.power);
                inst.setAbilityContribution(
                    instructionAbilityContribution(bestPilot.card));
                plan.addInstruction(inst);

                remaining -= bestPilot.cost;
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

        int instructionIndex = 0;
        for (DeploymentInstruction inst : plan.getInstructions()) {
            instructionFacts.add(new DeployPlanRankingPolicy.InstructionFacts(
                "instruction-" + instructionIndex++, inst.getPowerContribution()));

            // Track by location
            if (inst.getTargetLocationId() != null) {
                powerByLocation.merge(inst.getTargetLocationId(), inst.getPowerContribution(), Integer::sum);
                abilityByLocation.merge(inst.getTargetLocationId(),
                    inst.getAbilityContribution(), Float::sum);
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

            // V22: Objective-relevant location bonus for plan scoring
            boolean objectiveRelevant = false;
            float objectiveBonus = 0.0f;
            if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
                String locTitle = targetLoc.location.getTitle();
                if (locTitle != null && objectiveAnalyzer
                        .isObjectiveRelevantLocation(
                                targetLoc.location, currentGame,
                                currentPlayerId)) {
                    float objBonus = objectiveAnalyzer
                            .getLocationObjectiveBonus(
                                    targetLoc.location, currentGame,
                                    currentPlayerId);
                    objectiveRelevant = objBonus != 0.0f;
                    objectiveBonus = objBonus;
                    LOG.warn("V22 PLAN SCORE: {} is objective-relevant, +{} to plan score", locTitle, objBonus);
                }
            }

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
                targetLoc.ourForceIcons, targetLoc.theirForceIcons,
                targetLoc.theirCardCount, objectiveRelevant, objectiveBonus,
                triggerKnowable, exposureProven, strongestReactPower,
                formationPenaltyExempt));
        }

        PolicyResult core = DeployPlanRankingPolicy.evaluate(
                instructionFacts, locationFacts);
        PolicyResult[] adjuncts = objectiveAdjuncts != null
                ? objectiveAdjuncts : new PolicyResult[0];
        PolicyResult[] results = new PolicyResult[adjuncts.length + 1];
        results[0] = core;
        System.arraycopy(adjuncts, 0, results, 1, adjuncts.length);
        return DeployPlanRankingPolicy.apply(0.0f, results);
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
                    "Objective counted-operative formations"));
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
