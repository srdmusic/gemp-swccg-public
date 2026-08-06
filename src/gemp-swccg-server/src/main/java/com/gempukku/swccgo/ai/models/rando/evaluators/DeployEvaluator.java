package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy;
import com.gempukku.swccgo.ai.models.rando.strategy.CardKnowledge;
import com.gempukku.swccgo.ai.models.common.phase.BhbmSetupPayoffFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.CaptureDeployBudgetFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.DeployBudgetPolicy;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionEnvelopeFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionEnvelopePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployCardValueFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployCardValuePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployFormationSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSequencingFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSequencingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployPlanPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeploySequencingFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeploySequencingFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.DeploySequencingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeploySitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployPilotShipPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployWeaponPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployPolicy;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveScoringPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.EndorOperationsTacticalPolicy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates deployment decisions during Deploy phase.
 *
 * Handles:
 * - CARD_ACTION_CHOICE decisions with "Deploy" actions
 * - Scoring based on card value (power + ability) vs deploy cost
 * - Strategic deployment prioritization (locations first, reinforce losing)
 * - Affordability checking
 *
 * Ported from Python deploy_evaluator.py (simplified)
 */
public class DeployEvaluator extends ActionEvaluator {
    private static final Logger LOG = RandoLogger.getEvaluatorLogger();

    // Track cards we've already tried deploying this turn to avoid retry loops
    private Set<String> pendingDeployCardIds = new HashSet<>();
    private int lastTurnNumber = -1;

    public DeployEvaluator() {
        super("Deploy");
    }

    /**
     * Reset pending deploy tracking (call at turn start)
     */
    public void resetPendingDeploys() {
        pendingDeployCardIds.clear();
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // Only evaluate CARD_ACTION_CHOICE during Deploy phase
        if (!"CARD_ACTION_CHOICE".equals(context.getDecisionType())) {
            LOG.debug("[DeployEvaluator] canEvaluate=false: not CARD_ACTION_CHOICE (got {})", context.getDecisionType());
            return false;
        }

        // Must be our turn
        if (context.getGameState() != null && !context.isMyTurn()) {
            LOG.debug("[DeployEvaluator] canEvaluate=false: not our turn");
            return false;
        }

        // Must be Deploy phase
        Phase phase = context.getPhase();
        if (phase != Phase.DEPLOY) {
            LOG.debug("[DeployEvaluator] canEvaluate=false: not Deploy phase (got {})", phase);
            return false;
        }

        // Must have at least one deploy action
        List<String> actionTexts = context.getActionTexts();
        if (actionTexts == null || actionTexts.isEmpty()) {
            LOG.debug("[DeployEvaluator] canEvaluate=false: no action texts");
            return false;
        }

        boolean hasDeployAction = false;
        for (String actionText : actionTexts) {
            if (actionText != null && actionText.toLowerCase(Locale.ROOT).contains("deploy")) {
                hasDeployAction = true;
                break;
            }
        }

        if (hasDeployAction) {
            LOG.info("[DeployEvaluator] canEvaluate=TRUE - will evaluate deploy decision");
        } else {
            LOG.warn("[DeployEvaluator] canEvaluate=false: no deploy actions found in {} action texts", actionTexts.size());

            // V59 DIAGNOSTIC for Issue #2: when Deploy phase presents 0 deploy-from-hand
            // actions despite having hand cards + force, dump state so we can diagnose.
            // FIXES visibility of Turn 5 peaceful-pike bug where Rando had Obi-Wan, YS,
            // Luke's Lightsaber in hand, 13F in pile, but only 'Break cover' was offered.
            try {
                int force = context.getForcePileSize();
                List<PhysicalCard> h = context.getHand();
                int handSize = h != null ? h.size() : 0;
                // Count potentially deployable cards in hand
                int potentialDeploys = 0;
                StringBuilder handDetail = new StringBuilder();
                if (h != null) {
                    for (PhysicalCard hc : h) {
                        if (hc == null || hc.getBlueprint() == null) continue;
                        CardCategory cat = hc.getBlueprint().getCardCategory();
                        if (cat == null) continue;
                        int cost = 0;
                        try { Float c = hc.getBlueprint().getDeployCost(); if (c != null) cost = c.intValue(); }
                        catch (UnsupportedOperationException uoe) { /* not deployable */ }
                        boolean deployable = (cat == CardCategory.CHARACTER || cat == CardCategory.STARSHIP
                            || cat == CardCategory.VEHICLE || cat == CardCategory.WEAPON
                            || cat == CardCategory.DEVICE || cat == CardCategory.LOCATION
                            || cat == CardCategory.EFFECT);
                        if (deployable && cost <= force) potentialDeploys++;
                        handDetail.append(hc.getTitle()).append("(").append(cat).append(",c=").append(cost).append(") ");
                    }
                }
                LOG.warn("V59 DIAGNOSTIC NO-DEPLOYS: phase={} force={} handSize={} affordable={} | offered: {} | hand: [{}]",
                    context.getPhase(), force, handSize, potentialDeploys,
                    actionTexts, handDetail.toString().trim());
                if (potentialDeploys > 0) {
                    LOG.warn("V59 DIAGNOSTIC: {} affordable deploys in hand but GEMP offered NONE — engine-side restriction?",
                        potentialDeploys);
                }
            } catch (Exception e) {
                LOG.debug("V59 DIAGNOSTIC: Error dumping state: {}", e.getMessage());
            }
        }

        return hasDeployAction;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        // ========== CRITICAL DEBUG LOGGING ==========
        // This MUST show to verify JAR is deployed correctly
        LOG.warn("🚀🚀🚀 [DeployEvaluator.evaluate] ENTRY POINT - JAR VERSION 2026-02-23-V21 🚀🚀🚀");
        LOG.warn("🔍 Decision type: {}", context.getDecisionType());
        LOG.warn("🔍 Decision text: {}", context.getDecisionText());

        // Log ALL context data we have access to
        List<String> ctxCardIds = context.getCardIds();
        List<String> ctxBlueprints = context.getBlueprints();
        List<String> ctxActionIds = context.getActionIds();
        List<String> ctxActionTexts = context.getActionTexts();
        List<Boolean> ctxSelectable = context.getSelectable();

        LOG.warn("🔍 Context cardIds: {} items -> {}",
            ctxCardIds != null ? ctxCardIds.size() : "null",
            ctxCardIds != null ? ctxCardIds : "null");
        LOG.warn("🔍 Context blueprints: {} items -> {}",
            ctxBlueprints != null ? ctxBlueprints.size() : "null",
            ctxBlueprints != null ? ctxBlueprints : "null");
        LOG.warn("🔍 Context actionIds: {} items -> {}",
            ctxActionIds != null ? ctxActionIds.size() : "null",
            ctxActionIds != null ? ctxActionIds : "null");
        LOG.warn("🔍 Context actionTexts: {} items -> {}",
            ctxActionTexts != null ? ctxActionTexts.size() : "null",
            ctxActionTexts != null ? ctxActionTexts : "null");
        LOG.warn("🔍 Context selectable: {} items -> {}",
            ctxSelectable != null ? ctxSelectable.size() : "null",
            ctxSelectable != null ? ctxSelectable : "null");
        List<String> ctxTestingTexts = context.getTestingTexts();
        LOG.warn("🔍 Context testingTexts (CARD TITLES): {} items -> {}",
            ctxTestingTexts != null ? ctxTestingTexts.size() : "null",
            ctxTestingTexts != null ? ctxTestingTexts : "null");

        // Log hand cards
        List<PhysicalCard> debugHand = context.getHand();
        LOG.warn("🔍 Hand size: {}", debugHand != null ? debugHand.size() : "null");
        if (debugHand != null) {
            StringBuilder handStr = new StringBuilder();
            for (PhysicalCard card : debugHand) {
                if (card != null) {
                    handStr.append(card.getTitle()).append(" (id=").append(card.getCardId())
                           .append(", bp=").append(card.getBlueprintId(true)).append("), ");
                }
            }
            LOG.warn("🔍 Hand cards: {}", handStr);
        }
        // ========== END CRITICAL DEBUG LOGGING ==========

        LOG.info("[DeployEvaluator] Evaluating deploy phase decision");

        // Reset pending deploy tracking at the start of each turn
        if (context.getTurnNumber() != lastTurnNumber) {
            resetPendingDeploys();
            lastTurnNumber = context.getTurnNumber();
            LOG.debug("[DeployEvaluator] Reset pending deploys for turn {}", lastTurnNumber);
        }

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();

        if (actionIds == null || actionTexts == null) {
            LOG.warn("[DeployEvaluator] No action IDs or texts available");
            return actions;
        }

        // Get available force
        int availableForce = context.getForcePileSize();
        int lifeForce = context.getLifeForce();
        List<PhysicalCard> hand = context.getHand();

        // V48: Check if Vader needs force reserved for movement
        // In Hunt Down, Vader starts at Vader's Castle and MUST move to fight Jedi.
        // If bot spends all force on deploys, Vader is stuck at Castle doing nothing.
        int vaderMoveReserve = 0;
        // V79 (Steve, 2026-05-15): VERGE OF GREATNESS DEATH-STAR MOVE RESERVE
        // When Rando plays the Krennic/Scarif deck (objective: On The Verge Of
        // Greatness) AND Death Star (V) is on table but NOT orbiting Scarif,
        // reserve 1 Force per turn so the Death Star can move 2 parsecs toward
        // Scarif during the Move phase (hyperspeed 2, costs 1 Force per move).
        int v79VergeMoveReserve = 0;
        GameState vaderCheckGs = context.getGameState();
        SwccgGame vaderCheckGame = context.getGame();
        if (vaderCheckGs != null && vaderCheckGame != null) {
            try {
                String vPlayerId = context.getPlayerId();
                var v48Objective = context.getObjectiveAnalyzer();
                boolean v48HuntActorRoute =
                    v48Objective != null
                    && v48Objective.isAnalyzed()
                    && v48Objective.isHuntDownV()
                    && v48Objective.hasPreFlipRuntimeActorRule();
                if (v48HuntActorRoute) {
                    vaderMoveReserve =
                        v48Objective.getVaderCastleOutboundMoveReserve(
                            vaderCheckGame, vPlayerId);
                    if (vaderMoveReserve > 0) {
                        LOG.warn("V48 VADER MOVE RESERVE: legal Vader's Castle outbound route needs {} Force",
                            vaderMoveReserve);
                    }
                }
                // V79: track Verge of Greatness + Death Star state across the same scan
                boolean v79VergeActive = v48Objective != null
                    && v48Objective.isOnTheVergeObjectiveFront();
                PhysicalCard v79DeathStar = null;
                boolean v79DeathStarAtScarif = false;
                for (PhysicalCard pCard : vaderCheckGs.getAllPermanentCards()) {
                    if (pCard == null || !vPlayerId.equals(pCard.getOwner())) continue;
                    com.gempukku.swccgo.common.Zone pZone = pCard.getZone();
                    if (pZone == null || !pZone.isInPlay()) continue;
                    if (pCard.getBlueprint() == null || pCard.getTitle() == null) continue;
                    String pTitle = pCard.getTitle().toLowerCase(Locale.ROOT);
                    // V79: detect Death Star (title only — (V) marker is Rarity not title)
                    if (pTitle.contains("death star")
                            && pCard.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
                        v79DeathStar = pCard;
                        // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681):
                        // getAtLocation() is ALWAYS null for the Death Star mobile-system LOCATION
                        // card, so this 1-Force move reserve fired EVERY turn forever — including
                        // while the DS was parked in Scarif orbit — and at 06:02 suppressed a real
                        // Mara Jade With Lightsaber deploy (cost 5, leaves 0) to hoard Force for a
                        // move that is unnecessary once Scarif orbit is established. Use
                        // the engine's orbit primitive getSystemOrbited() (same check as the flip
                        // condition, Filters.isOrbiting(Title.Scarif), Card216_011:122).
                        // PhysicalCard dsLoc = pCard.getAtLocation();
                        // if (dsLoc != null && dsLoc.getTitle() != null
                        //         && dsLoc.getTitle().toLowerCase(Locale.ROOT).contains("scarif")) {
                        //     v79DeathStarAtScarif = true;
                        // }
                        String dsOrbited = pCard.getSystemOrbited();
                        if (dsOrbited != null && dsOrbited.toLowerCase(Locale.ROOT).contains("scarif")) {
                            v79DeathStarAtScarif = true;
                        }
                    }
                }
                // V79: if Verge of Greatness + Death Star not yet at Scarif, reserve 1 Force
                if (v79VergeActive && v79DeathStar != null && !v79DeathStarAtScarif) {
                    v79VergeMoveReserve = 1;
                    LOG.warn("V79 VERGE MOVE RESERVE: Verge front active + Death Star not at Scarif, reserve 1 Force for Move phase");
                }
            } catch (Exception e) {
                LOG.debug("V48 VADER MOVE RESERVE: Error: {}", e.getMessage());
            }
        }

        // === V67z DEPLOY TRANSIT RESERVE (Steve, 2026-06): exact Hidden Path
        // Corridor exits still needed for the two-site flip gate. The shared
        // analyzer mirrors the physical action: Jedi Survivor, active at the
        // exact Corridor, not already moved, and a legal unfilled destination.
        int v67zTransitReserve = 0;
        if (vaderCheckGs != null) {
            try {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v67zObj =
                    context.getObjectiveAnalyzer();
                if (v67zObj != null) {
                    v67zTransitReserve = v67zObj
                        .getHiddenPathMoveForceReserve(
                            context.getGame(), context.getPlayerId());
                    if (v67zTransitReserve > 0) {
                        LOG.warn("V67z HIDDEN PATH MOVE RESERVE: hold {} Force for the exact safe route that advances the current face",
                            v67zTransitReserve);
                    }
                }
            } catch (Exception e) { LOG.debug("V67z deploy transit-reserve error: {}", e.getMessage()); }
        }

        LOG.debug("[DeployEvaluator] Resources: force={}, lifeForce={}, handSize={}, actions={}",
            availableForce, lifeForce, hand != null ? hand.size() : 0, actionIds.size());

        // === USE DEPLOY PHASE PLANNER ===
        DeploymentPlan plan = null;
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        SwccgGame game = context.getGame();
        Side side = context.getSide();
        String playerId = context.getPlayerId();

        // DEBUG: Log which context values are available
        LOG.info("[DeployEvaluator] Context check: planner={}, game={}, side={}, playerId={}",
            planner != null ? "SET" : "NULL",
            game != null ? "SET" : "NULL",
            side != null ? side : "NULL",
            playerId);

        if (planner != null && game != null && side != null) {
            LOG.info("[DeployEvaluator] Calling planner.createPlan()...");
            plan = planner.createPlan(game, playerId, side);
            if (plan != null) {
                LOG.info("[DeployEvaluator] ✅ Got deployment plan: strategy={}, instructions={}",
                    plan.getStrategy(), plan.getInstructions().size());

                // === AUTO-CLEANUP: Detect deployed cards ===
                // Check if any planned cards are no longer in hand - they were deployed!
                // This fixes the STALE PLAN bug where recordDeployment() was never called
                if (!plan.getInstructions().isEmpty() && hand != null) {
                    Set<String> handBlueprintIds = new HashSet<>();
                    Set<Integer> handPermanentIds = new HashSet<>();
                    for (PhysicalCard card : hand) {
                        if (card != null) {
                            handPermanentIds.add(card.getPermanentCardId());
                            String bpId = card.getBlueprintId(true);
                            if (bpId != null) {
                                handBlueprintIds.add(bpId);
                            }
                        }
                    }

                    // Find instructions for cards no longer in hand
                    List<DeploymentInstruction> deployedCards = new ArrayList<>();
                    for (DeploymentInstruction instruction : plan.getInstructions()) {
                        boolean stillInHand = instruction.getCardPermanentCardId() != null
                            ? handPermanentIds.contains(instruction.getCardPermanentCardId())
                            : handBlueprintIds.contains(instruction.getCardBlueprintId());
                        if (!stillInHand) {
                            deployedCards.add(instruction);
                        }
                    }

                    // Record deployments for cards that left hand
                    for (DeploymentInstruction instruction : deployedCards) {
                        LOG.info("📋 Auto-detected deployment: {} left hand", instruction.getCardName());
                        if (instruction.getCardPermanentCardId() != null
                                && instruction.getCardCurrentCardId() != null) {
                            planner.recordDeployment(
                                instruction.getCardPermanentCardId(),
                                instruction.getCardCurrentCardId(),
                                instruction.getCardBlueprintId());
                        } else {
                            planner.recordDeployment(instruction.getCardBlueprintId());
                        }
                    }
                }
                plan = plan.assessmentCopy();

                // Log plan status
                if (plan.isPlanComplete()) {
                    LOG.info("📋 Deploy plan: COMPLETE ({} deployed)", plan.getDeploymentsMade());
                } else {
                    int remaining = plan.getInstructions().size();
                    int done = plan.getDeploymentsMade();
                    LOG.info("📋 Deploy plan: {} - {} ({} remaining, {} done)",
                        plan.getStrategy().getValue(), plan.getReason(), remaining, done);
                }
            } else {
                LOG.warn("[DeployEvaluator] ⚠️ Planner returned null plan");
            }
        } else {
            LOG.warn("[DeployEvaluator] ⚠️ Cannot call planner - missing: {}{}{}",
                planner == null ? "planner " : "",
                game == null ? "game " : "",
                side == null ? "side " : "");
        }

        boolean plannedDeployActionOffered = false;

        // === STALE PLAN DETECTION ===
        // Check if available deploy actions match the plan
        // If none match and we have deploy actions, check WHY before marking stale
        if (plan != null && !plan.getInstructions().isEmpty() && !plan.isPlanComplete()) {
            boolean planCardsStillInHand = false;
            List<String> cardIdList = context.getCardIds();
            List<String> blueprintList = context.getBlueprints();

            // First, check if any planned cards are still in hand
            Set<String> handBlueprintIds = new HashSet<>();
            Set<Integer> handPermanentIds = new HashSet<>();
            if (hand != null) {
                for (PhysicalCard handCard : hand) {
                    if (handCard != null) {
                        handPermanentIds.add(handCard.getPermanentCardId());
                        String bpId = handCard.getBlueprintId(true);
                        if (bpId != null) {
                            handBlueprintIds.add(bpId);
                        }
                    }
                }
            }

            StringBuilder planInHandCards = new StringBuilder();
            StringBuilder planNotInHandCards = new StringBuilder();
            for (DeploymentInstruction inst : plan.getInstructions()) {
                boolean inHand = inst.getCardPermanentCardId() != null
                    ? handPermanentIds.contains(inst.getCardPermanentCardId())
                    : handBlueprintIds.contains(inst.getCardBlueprintId());
                if (inHand) {
                    planCardsStillInHand = true;
                    planInHandCards.append(inst.getCardName()).append(" (").append(inst.getCardBlueprintId()).append("), ");
                } else {
                    planNotInHandCards.append(inst.getCardName()).append(" (").append(inst.getCardBlueprintId()).append("), ");
                }
            }
            if (planCardsStillInHand) {
                LOG.warn("📋 Plan cards IN HAND but not deployable: {}", planInHandCards);
            }
            if (planNotInHandCards.length() > 0) {
                LOG.warn("📋 Plan cards NOT in hand (already deployed?): {}", planNotInHandCards);
            }

            for (int i = 0; i < actionTexts.size(); i++) {
                String actionText = actionTexts.get(i);
                if (actionText == null || !actionText.toLowerCase(Locale.ROOT).contains("deploy")) {
                    continue;
                }

                // Get blueprint ID using cardId lookup (most reliable)
                String bpId = null;
                PhysicalCard actionCard = null;
                String cardIdStr = (cardIdList != null && i < cardIdList.size()) ? cardIdList.get(i) : null;

                // Method 1: Look up card by cardId in game state to get its blueprint
                if (cardIdStr != null && !cardIdStr.isEmpty() && gameState != null) {
                    try {
                        int cardIdNum = Integer.parseInt(cardIdStr);
                        actionCard = gameState.findCardById(cardIdNum);
                        if (actionCard != null) {
                            bpId = actionCard.getBlueprintId(true);
                        }
                    } catch (NumberFormatException e) {
                        // Not a number - ignore
                    }
                }

                // Method 2: Use blueprint from params (for virtual/off-table actions)
                if (bpId == null && blueprintList != null && i < blueprintList.size()) {
                    String paramBp = blueprintList.get(i);
                    if (paramBp != null && !paramBp.isEmpty() && !"inPlay".equals(paramBp)) {
                        bpId = paramBp;
                    }
                }

                // Check if this blueprint is in the plan
                DeploymentInstruction matchingInstruction = actionCard != null
                    ? plan.getInstructionForPhysicalCard(
                        actionCard.getPermanentCardId(), actionCard.getCardId(), bpId)
                    : bpId != null ? plan.getInstructionForCard(bpId) : null;
                if (matchingInstruction != null) {
                    plannedDeployActionOffered = true;
                    LOG.debug("   Found plan card {} in action: {}", bpId, actionText.substring(0, Math.min(60, actionText.length())));
                    break;
                }
            }

            if (!plannedDeployActionOffered) {
                if (planCardsStillInHand) {
                    // Plan cards are in hand but not deployable - probably can't afford them
                    // Set flag so we apply HUGE penalty to non-plan deploys
                    LOG.warn("📋 Plan cards in hand but not affordable - will heavily penalize off-plan deploys");
                    plan.setWaitingForPlannedCards(true);
                } else {
                    // Plan cards are NOT in hand at all - plan is truly stale
                    LOG.warn("⚠️ STALE PLAN: Plan has {} cards but NONE are in hand or deploy actions!",
                        plan.getInstructions().size());
                    StringBuilder planCards = new StringBuilder();
                    for (DeploymentInstruction inst : plan.getInstructions()) {
                        planCards.append(inst.getCardName()).append(", ");
                    }
                    LOG.warn("   Plan cards: {}", planCards);
                    // Mark plan as allowing extra actions since planned cards are truly gone
                    plan.setForceAllowExtras(true);
                }
            }
        }
        // Check if we're behind on board (need to deploy more aggressively)
        boolean needsReinforcement = false;
        if (gameState != null) {
            String opponentId = gameState.getOpponent(playerId);
            // Simple check: compare card counts
            int ourCards = countCardsInPlay(gameState, playerId);
            int theirCards = opponentId != null ? countCardsInPlay(gameState, opponentId) : 0;
            needsReinforcement = ourCards < theirCards;
        }

        // DEBUG: Log ALL available actions to understand the format
        LOG.info("[DeployEvaluator] === Available actions ({} total) ===", actionIds.size());
        for (int idx = 0; idx < actionIds.size(); idx++) {
            String id = actionIds.get(idx);
            String txt = idx < actionTexts.size() ? actionTexts.get(idx) : "(no text)";
            LOG.info("   Action[{}]: id='{}', text='{}'", idx, id, txt);
        }

        // 2026-05-29 FIX (Steve, U-3PO stuck loop after cancel-loop fires):
        // DecisionTracker.blockLastActionOnCancel adds the offending action ID
        // to blockedResponses for the outer CARD_ACTION_CHOICE. ActionTextEvaluator
        // already honors it (line 96). DeployEvaluator did NOT, so when the
        // cancel-loop blocked '1' for "Choose Deploy action or Pass", DeployEvaluator
        // re-scored Deploy at -50 and picked it anyway, re-entering the loop.
        // Now: any actionId / actionText in blockedResponses is hard-blocked
        // -9999 so Rando picks something else (Play a card or Pass).
        java.util.Set<String> v159DeployBlocked = context.getBlockedResponses();
        String decisionId = context.getDecisionId();
        PolicyContributionLedger pullLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "pull-deploy-decision" : decisionId + "-pull-deploy");
        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle deploy-related actions (including persona replace)
            if (!actionLower.contains("deploy") && !actionLower.contains("persona replace")) {
                continue;
            }

            String sourceCardId =
                    ctxCardIds != null && i < ctxCardIds.size()
                        ? ctxCardIds.get(i) : null;
            PhysicalCard deployActionSource = null;
            try {
                if (gameState != null && sourceCardId != null) {
                    deployActionSource = gameState.findCardById(
                            Integer.parseInt(sourceCardId));
                }
            } catch (NumberFormatException ignored) {
                // Unresolved sources do not receive the narrow stale-id release.
            }
            var deployObjectiveAnalyzer =
                    context.getObjectiveAnalyzer();
            boolean exactShieldCannonDeploy =
                    deployObjectiveAnalyzer != null
                    && deployObjectiveAnalyzer
                        .isShieldMainGeneratorPriorityCannonDeployAction(
                            game, playerId, deployActionSource, actionText);
            boolean exactShieldFreeWarrior =
                    deployObjectiveAnalyzer != null
                    && deployObjectiveAnalyzer
                        .isShieldBlizzardFourWarriorDeployActionSource(
                            game, playerId, deployActionSource, actionText);
            boolean exactHiddenPathJabiimRoute =
                    deployObjectiveAnalyzer != null
                    && deployObjectiveAnalyzer
                        .isHiddenPathJabiimRouteAction(
                            game, playerId,
                            deployActionSource, actionText);
            boolean blockedResponse = !exactShieldCannonDeploy
                    && !exactHiddenPathJabiimRoute
                    && v159DeployBlocked != null
                    && !v159DeployBlocked.isEmpty()
                    && (v159DeployBlocked.contains(actionId)
                    || v159DeployBlocked.contains(actionText));
            boolean personaReplace = actionLower.contains("persona replace");
            DeployActionEnvelopePolicy.Evaluation parentEnvelope =
                    DeployActionEnvelopePolicy.evaluateParent(
                            new DeployActionEnvelopeFacts.ParentAction(
                                    actionId, blockedResponse, personaReplace));
            EvaluatedAction action = new EvaluatedAction(
                    actionId, ActionType.DEPLOY,
                    parentEnvelope.initialScore(), actionText);
            PolicyContributionLedger parentEnvelopeLedger =
                    new PolicyContributionLedger(
                            (decisionId == null || decisionId.isBlank()
                                    ? "deploy-parent-envelope"
                                    : decisionId + "-deploy-parent-envelope")
                                    + "-" + actionId);
            parentEnvelopeLedger.register(parentEnvelope.result());
            PolicyOperationAdapter.apply(action, parentEnvelopeLedger);
            if (blockedResponse) {
                LOG.warn("DeployEvaluator: actionId='{}' is in blockedResponses → -9999 (cancel-loop block)", actionId);
            } else if (personaReplace) {
                LOG.warn("V38.4 PERSONA REPLACE BLOCKED: '{}'", actionText);
            }
            if (parentEnvelope.adapterStep()
                    == DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION) {
                actions.add(action);
                continue;
            }
            if (deployObjectiveAnalyzer != null
                    && deployObjectiveAnalyzer
                        .wouldHiddenPathRouteActionConsumeTransitReserve(
                            game, playerId, deployActionSource,
                            actionText, availableForce)) {
                action.hardVeto(
                    "OBJECTIVE.HIDDEN_PATH.TRANSIT_FORCE_RESERVE: preserve the exact Force needed for ready Underground Corridor exits");
                actions.add(action);
                continue;
            }
            if (exactShieldFreeWarrior) {
                // The action deploys a warrior for free. Blizzard 4 is only
                // the source, so none of its own deploy or maintenance facts
                // apply here. ActionTextPolicy owns the positive priority.
                actions.add(action);
                continue;
            }
            if (deployObjectiveAnalyzer != null
                    && deployObjectiveAnalyzer
                        .isExhaustedCountedOperativeSiteRouteAction(
                            game, playerId,
                            deployActionSource, actionText)) {
                action.hardVeto(
                    "OBJECTIVE.COUNTED_OPERATIVE.SITE_ROUTE_EXHAUSTED: the three-site route is complete or no legal battleground candidate remains");
                actions.add(action);
                continue;
            }
            if (deployObjectiveAnalyzer != null
                    && deployObjectiveAnalyzer
                        .isExhaustedHiddenPathJabiimRouteAction(
                            game, playerId,
                            deployActionSource, actionText)) {
                action.hardVeto(
                    "OBJECTIVE.HIDDEN_PATH.JABIIM_ROUTE_EXHAUSTED: no legal Jabiim location remains in Reserve Deck");
                actions.add(action);
                continue;
            }

            // V209 PULL deploy-side guards. The shared policy keeps this evaluator's
            // historical additive veto layer separate from ActionText's parent scorer.
            boolean firstOrderReignsObjectiveDownload = false;
            if (sourceCardId != null && gameState != null
                    && context.getObjectiveAnalyzer() != null) {
                try {
                    PhysicalCard sourceCard =
                            gameState.findCardById(
                                Integer.parseInt(sourceCardId));
                    firstOrderReignsObjectiveDownload =
                            context.getObjectiveAnalyzer()
                                .isFirstOrderReignsDownloadAction(
                                    sourceCard, actionText);
                } catch (NumberFormatException ignored) {
                    // Non-card source ids cannot be the objective action.
                }
            }
            boolean reservePull =
                    actionLower.contains("from reserve deck")
                    || actionLower.contains("[download]")
                    || firstOrderReignsObjectiveDownload;
            if (reservePull) {
                PullDeployPolicy.Evaluation pull = PullDeployPolicy.evaluate(
                        PullPolicyAdapter.readDeploy(
                                context, actionId, actionText, sourceCardId));
                pullLedger.register(pull.result());
                PolicyOperationAdapter.apply(action, pullLedger);
                if (pull.adapterStep()
                        == PullDeployPolicy.AdapterStep.CONTINUE_ACTION) {
                    actions.add(action);
                    continue;
                }
                LOG.info("V60 RESERVE PULL guards passed for '{}' "
                        + "— baseline owned by V192 (ActionTextEvaluator)", actionText);
            }

            // DEPLOY-1 phase envelope: one shared owner computes urgency and cross-phase
            // obligations from stock board facts. MOVE/BATTLE provide facts only.
            DeploySequencingFacts.PowerGap endangered = null;
            DeploySequencingFacts.PowerGap winnableBattle = null;
            try {
                endangered = DeploySequencingFactsReader.firstEndangeredLocation(
                        context.getGameState(), context.getGame(), context.getPlayerId());
            } catch (Exception sequencingError) {
                LOG.debug("V169 umbrella error: {}", sequencingError.getMessage());
            }
            try {
                if (context.getForcePileSize() <= 2) {
                    winnableBattle = DeploySequencingFactsReader.firstWinnableBattle(
                            context.getGameState(), context.getGame(), context.getPlayerId());
                }
            } catch (Exception sequencingError) {
                LOG.debug("V176 error: {}", sequencingError.getMessage());
            }
            if (winnableBattle != null
                    && context.getObjectiveAnalyzer() != null
                    && context.getObjectiveAnalyzer()
                        .isIsbFlipCompletionDeployCandidate(
                            context.getGame(), context.getPlayerId(),
                            deployActionSource)) {
                winnableBattle = null;
            }
            DeploySequencingPolicy.Evaluation envelope = DeploySequencingPolicy.phaseEnvelope(
                    actionId, hand != null ? hand.size() : 0, availableForce,
                    context.getForcePileSize(), endangered, winnableBattle);
            PolicyContributionLedger envelopeLedger = new PolicyContributionLedger(
                    (decisionId == null || decisionId.isBlank()
                            ? "deploy-envelope" : decisionId + "-deploy-envelope")
                            + "-" + actionId);
            envelopeLedger.register(envelope.result());
            PolicyOperationAdapter.apply(action, envelopeLedger);

            // === APPLY PHASE-LEVEL PLAN ===
            // V24.10: NEVER hold back on turns 1-2. The engine MUST be built ASAP:
            //   Locations → AMSD (Piett + Executor) → Lando/Lobot → everything else.
            // Holding back early wastes critical setup turns.
            // After turn 2, HOLD_BACK can apply to non-location cards only.
            // Locations are ALWAYS exempt from HOLD_BACK regardless of turn.
            if (plan != null && plan.getStrategy() == DeployStrategy.HOLD_BACK) {
                // V40: HOLD_BACK only applies to TDIGWATT decks.
                // Hunt Down and all other decks deploy freely — no hold back ever.
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer holdBackObjAnalyzer =
                    context.getObjectiveAnalyzer();
                // V40: Only apply hold-back for TDIGWATT. All others deploy freely.
                boolean isTdigwattDeck = holdBackObjAnalyzer != null && holdBackObjAnalyzer.isAnalyzed()
                    && holdBackObjAnalyzer.isTdigwatt();
                if (!isTdigwattDeck) {
                    // V40: NOT TDIGWATT — NEVER hold back, deploy freely
                    LOG.warn("V40 NO HOLD_BACK: Not TDIGWATT deck — ignoring hold-back, deploy freely! Action: '{}'",
                        actionText);
                    // Fall through to normal scoring
                } else {
                    int holdBackTurn = context.getTurnNumber();
                    if (holdBackTurn <= 2) {
                        // Turns 1-2: IGNORE hold-back entirely — build the engine!
                        LOG.warn("V24.10 NO HOLD_BACK TURNS 1-2: Turn {} — ignoring hold-back, must build engine! Action: '{}'",
                            holdBackTurn, actionText);
                        // Fall through to normal scoring
                    } else {
                        // V46: Turn 3+: HOLD_BACK only at start, not end of game!
                        // Once past setup turns, deploy aggressively like any other deck.
                        LOG.warn("V46 HOLD_BACK EXPIRED: Turn {} — past setup phase, deploy freely!", holdBackTurn);
                        // Fall through to normal scoring
                    }
                }
            }

            // === Get card ID from decision parameters ===
            // For CARD_ACTION_CHOICE, each action has an associated cardId at the same index
            List<String> cardIdList = context.getCardIds();
            List<String> blueprintList = context.getBlueprints();
            String cardIdStr = (cardIdList != null && i < cardIdList.size()) ? cardIdList.get(i) : null;
            String blueprintIdFromParam = (blueprintList != null && i < blueprintList.size()) ? blueprintList.get(i) : null;

            // Get card title from testingText (MOST RELIABLE - directly from GEMP)
            String cardTitleFromGemp = context.getCardTitleAt(i);

            // V29: Early card-ID lookup for bare "Deploy" actions.
            // Many deploy actions have actionText="Deploy" with no card name, blueprintId="inPlay",
            // and testingText=null. Without this, all guards (BESPIN-FIRST, buddy check, etc.)
            // can't identify the card and may incorrectly block/penalize it.
            // Resolve via gameState.findCardById() which works for hand cards.
            PhysicalCard earlyCard = null;
            PhysicalCard earlyLocationCard = null;
            if (cardIdStr != null && !cardIdStr.isEmpty() && gameState != null) {
                try {
                    earlyLocationCard = gameState.findCardById(Integer.parseInt(cardIdStr));
                    if (cardTitleFromGemp == null) {
                        earlyCard = earlyLocationCard;
                        if (earlyCard != null && earlyCard.getTitle() != null) {
                            cardTitleFromGemp = earlyCard.getTitle();
                            LOG.info("V29 EARLY LOOKUP: Resolved bare 'Deploy' via cardId {} → '{}'", cardIdStr, cardTitleFromGemp);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Not an integer cardId — skip
                }
            }

            LOG.info("[DeployEvaluator] Action[{}]: cardId='{}', blueprintId='{}', CARD_TITLE='{}', actionText='{}'",
                i, cardIdStr, blueprintIdFromParam, cardTitleFromGemp, actionText);

            // NOTE: We used to check pendingDeployCardIds here to avoid loops,
            // but the tracking was broken (added during evaluation, not after selection).
            // Loop detection is now handled by DecisionTracker at a higher level.
            // If loops become an issue, we need to track selected actions in CombinedEvaluator.

            // === EARLY-GAME DEPLOYMENT RESTRICTION ===
            // Block certain Effects from being deployed on turn 1.
            // Uses title-based matching from CardKnowledge.shouldBlockDeployment().
            // The title can come from GEMP's testingText or from the action text itself.
            int currentTurn = context.getTurnNumber();
            String titleForRestrictionCheck = cardTitleFromGemp;
            if (titleForRestrictionCheck == null) {
                // Fallback: try to extract title from action text (strip HTML, bullets, etc.)
                titleForRestrictionCheck = actionText.replaceAll("<[^>]*>", "").replace("•", "").trim();
            }
            boolean blockTurnOneEffect = CardKnowledge.shouldBlockDeployment(
                    titleForRestrictionCheck, currentTurn);
            DeployActionEnvelopePolicy.Evaluation titleGate =
                    DeployActionEnvelopePolicy.evaluateTitleGate(
                            new DeployActionEnvelopeFacts.TitleGate(
                                    actionId, blockTurnOneEffect));
            PolicyContributionLedger titleGateLedger =
                    new PolicyContributionLedger(
                            (decisionId == null || decisionId.isBlank()
                                    ? "deploy-title-gate"
                                    : decisionId + "-deploy-title-gate")
                                    + "-" + actionId);
            titleGateLedger.register(titleGate.result());
            PolicyOperationAdapter.apply(action, titleGateLedger);
            if (blockTurnOneEffect) {
                LOG.warn("🚫 BLOCKING turn-1 deploy of Effect '{}' (turn {})",
                        titleForRestrictionCheck, currentTurn);
            }
            if (titleGate.adapterStep()
                    == DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION) {
                actions.add(action);
                continue;
            }

            // === LOCATION DEPLOYMENT - Highest Priority ===
            // Resolve the deployed card's category first. Text is only a fallback for
            // actions without card identity, and must name the deployed subject rather
            // than merely mentioning a destination such as "character to a site".
            boolean earlyCardResolved = earlyLocationCard != null
                && earlyLocationCard.getBlueprint() != null;
            boolean earlyLocationByCategory = earlyCardResolved
                && earlyLocationCard.getBlueprint().getCardCategory() == CardCategory.LOCATION;
            boolean earlyLocationCandidate =
                DeployObjectiveSequencingPolicy.isEarlyLocationCandidate(
                    new DeployObjectiveSequencingFacts.EarlyLocationCandidate(
                        actionText, earlyCardResolved, earlyLocationByCategory));
            boolean objectiveInactivationChecked = false;
            if (earlyLocationCandidate) {
                objectiveInactivationChecked = true;
                if (applyRequiredCardInactivationVeto(
                        context, action, decisionId, actionId,
                        earlyLocationCard)) {
                    actions.add(action);
                    continue;
                }
                // === V24.10: EXTRA LOCATION PRIORITY WHEN PIETT NEEDS FINDING ===
                // If Piett is stuck in the force pile, deploying more locations means
                // more force generation → bigger force pile → draw through faster to find him.
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle locOracle = context.getDeckOracle();
                boolean locOracleAnalyzed = locOracle != null && locOracle.isAnalyzed();
                boolean piettAccessible = false;
                boolean piettLost = false;
                int piettTurnNumber = 0;
                if (locOracleAnalyzed) {
                    piettAccessible = locOracle.isCardInHand("Admiral Piett") || locOracle.isCardInHand("Piett")
                        || locOracle.isCardInReserve("Admiral Piett") || locOracle.isCardInReserve("Piett")
                        || locOracle.isCardInPlay("Admiral Piett") || locOracle.isCardInPlay("Piett");
                    piettLost = locOracle.isCardLost("Admiral Piett") || locOracle.isCardLost("Piett");
                    if (!piettAccessible && !piettLost) {
                        piettTurnNumber = context.getTurnNumber();
                    }
                }

                // === V23: BESPIN SYSTEM EARLY DEPLOY PRIORITY ===
                // For TDIGWATT, Bespin system is the FOUNDATION of the entire objective.
                // Without Bespin on table, nothing works: no Dark Deal, no CC Occupation,
                // no AMSD deploy target. Deploy it IMMEDIATELY on turns 1-3.
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer bespinObjAnalyzer =
                    context.getObjectiveAnalyzer();
                int turnNum = context.getTurnNumber();
                boolean bespinObjectiveAnalyzed = bespinObjAnalyzer != null
                    && bespinObjAnalyzer.isAnalyzed();
                boolean needsBespinSystem = bespinObjectiveAnalyzed
                    && bespinObjAnalyzer.needsBespinSystemPresence();
                boolean isBespinDeploy = false;
                boolean bespinOnTable = false;
                if (needsBespinSystem && turnNum <= 3) {
                    // Check if this action deploys Bespin system specifically
                    isBespinDeploy = actionLower.contains("bespin");
                    if (!isBespinDeploy && cardTitleFromGemp != null) {
                        isBespinDeploy = cardTitleFromGemp.toLowerCase(Locale.ROOT).contains("bespin");
                    }
                    if (isBespinDeploy) {
                        // Check Bespin isn't already on table
                        if (gameState != null) {
                            for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null && loc.getTitle() != null &&
                                    loc.getTitle().toLowerCase(Locale.ROOT).contains("bespin") &&
                                    loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                    loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                    bespinOnTable = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                DeployObjectiveSequencingPolicy.EarlyLocationEvaluation earlyLocation =
                    DeployObjectiveSequencingPolicy.evaluateEarlyLocation(
                        new DeployObjectiveSequencingFacts.EarlyLocation(
                            actionId, locOracleAnalyzed, piettAccessible, piettLost,
                            piettTurnNumber, bespinObjectiveAnalyzed, needsBespinSystem,
                            turnNum, isBespinDeploy, bespinOnTable));
                PolicyContributionLedger earlyLocationLedger =
                    new PolicyContributionLedger(
                        (decisionId == null || decisionId.isBlank()
                            ? "deploy-early-location"
                            : decisionId + "-deploy-early-location")
                            + "-" + actionId);
                earlyLocationLedger.register(earlyLocation.result());
                PolicyOperationAdapter.apply(action, earlyLocationLedger);
                if (earlyLocation.piettPriorityApplied()) {
                    LOG.warn("V24.10 PIETT DIG: Piett not accessible — extra location deploy priority (+150) to power force pile draws!");
                }
                if (earlyLocation.bespinBoost() > 0.0f) {
                    LOG.warn("V24.15 BESPIN PRIORITY: Bespin system deploy gets +{} on turn {} — MUST deploy ASAP!",
                        earlyLocation.bespinBoost(), turnNum);
                }
                if (earlyLocation.adapterStep()
                        == DeployObjectiveSequencingPolicy.AdapterStep.CONTINUE_ACTION) {
                    actions.add(action);
                    continue;
                }
            }

            // === V29: TDIGWATT BESPIN-FIRST GUARD (rewritten) ===
            // For TDIGWATT objective, Executor MUST deploy before any characters.
            // Strategy: Bespin system → Executor (via AMSD or hand) → then characters.
            // V29 FIX: Removed turn limit — guard stays active until Bespin is actually
            // occupied. Previous V28 used bfTurn <= 2 which expired before Rando could
            // get Executor out, allowing Lando to deploy before Executor on turn 3.
            // Exempt: locations, sites, systems, AMSD, Executor, starships, vehicles, Bespin system.
            {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer bespinFirstAnalyzer =
                    context.getObjectiveAnalyzer();
                int bfTurn = context.getTurnNumber();
                if (bespinFirstAnalyzer != null && bespinFirstAnalyzer.isAnalyzed()
                    && bespinFirstAnalyzer.isTdigwatt()) {
                    // Check Bespin occupation FIRST — if we occupy it, guard is off permanently
                    boolean weOccupyBespinSpace = false;
                    GameState bfGs = context.getGameState();
                    SwccgGame bfGame = context.getGame();
                    if (bfGs != null && bfGame != null) {
                        try {
                            String bfPlayerId = context.getPlayerId();
                            for (PhysicalCard loc : bfGs.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                String locTitle = loc.getTitle().toLowerCase(Locale.ROOT);
                                if (locTitle.contains("bespin") && loc.getBlueprint() != null
                                    && loc.getBlueprint().getCardSubtype() != null
                                    && loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                    float ourPowerAtBespin = bfGame.getModifiersQuerying().getTotalPowerAtLocation(
                                        bfGs, loc, bfPlayerId, false, false);
                                    if (ourPowerAtBespin > 0) {
                                        weOccupyBespinSpace = true;
                                    }
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V29 BESPIN-FIRST: Error checking Bespin occupation: {}", e.getMessage());
                        }
                    }

                    if (!weOccupyBespinSpace) {
                        // Bespin NOT occupied — block character deploys
                        // V29: Use BOTH actionLower AND resolved card title/category from early lookup
                        // (bare "Deploy" actions have no info in actionText)
                        String guardCheckText = actionLower;
                        if (cardTitleFromGemp != null) {
                            guardCheckText = guardCheckText + " " + cardTitleFromGemp.toLowerCase(Locale.ROOT);
                        }

                        // V29: If we resolved the card, check its category directly.
                        // Resolved non-character support cards are not part of this character gate.
                        boolean isCardResolved = false;
                        boolean isCharacterByCategory = false;
                        boolean isLocationByCategory = false;
                        boolean isShipByCategory = false;
                        if (earlyLocationCard != null && earlyLocationCard.getBlueprint() != null) {
                            isCardResolved = true;
                            CardCategory earlyCategory = earlyLocationCard.getBlueprint().getCardCategory();
                            isCharacterByCategory = (earlyCategory == CardCategory.CHARACTER);
                            isLocationByCategory = (earlyCategory == CardCategory.LOCATION);
                            isShipByCategory = (earlyCategory == com.gempukku.swccgo.common.CardCategory.STARSHIP)
                                || (earlyCategory == com.gempukku.swccgo.common.CardCategory.VEHICLE);
                        }

                        DeployObjectiveSequencingPolicy.BespinFirstRoute bespinFirstRoute =
                            DeployObjectiveSequencingPolicy.classifyBespinFirst(
                                new DeployObjectiveSequencingFacts.BespinFirstCandidate(
                                    guardCheckText, isCardResolved, isCharacterByCategory,
                                    isLocationByCategory, isShipByCategory));
                        if (bespinFirstRoute
                                == DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE) {
                            // V29 UPDATED 2026-07-06 (TDIGWATT bug B): release the -500 gate when there is
                            // NO live path to ever satisfy it — otherwise every character deploy is blocked
                            // forever and Rando floods weak solos without flipping. Two universal checks
                            // (no card-name lists):
                            //   (a) the objective's OWN game text forbids deploying Executor (TDIGWATT (V)
                            //       Card226_012: "you may not deploy Admiral's Orders or [Death Star II]
                            //       Executor" — verified from the card source);
                            //   (b) DeckOracle sees no capital starship in hand/reserve/force/used — nothing
                            //       left that could establish Bespin space presence.
                            // Classic TDIGWATT keeps the gate: no forbid clause + Executor in deck.
                            boolean objectiveForbidsExecutor =
                                bespinFirstAnalyzer.objectiveForbidsDeployingExecutor();
                            boolean bfOracleAnalyzed = false;
                            boolean bfCapitalAccessible = false;
                            if (!objectiveForbidsExecutor) {
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle bfOracle = context.getDeckOracle();
                                if (bfOracle != null && bfOracle.isAnalyzed()) {
                                    bfOracleAnalyzed = true;
                                    com.gempukku.swccgo.common.Zone[] bfZones = {
                                        com.gempukku.swccgo.common.Zone.HAND,
                                        com.gempukku.swccgo.common.Zone.RESERVE_DECK,
                                        com.gempukku.swccgo.common.Zone.FORCE_PILE,
                                        com.gempukku.swccgo.common.Zone.USED_PILE };
                                    for (com.gempukku.swccgo.common.Zone bfZone : bfZones) {
                                        for (com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard bfDc
                                                : bfOracle.getCardsByCategory(CardCategory.STARSHIP, bfZone)) {
                                            if (bfDc.getSubtype() == com.gempukku.swccgo.common.CardSubtype.CAPITAL) {
                                                bfCapitalAccessible = true;
                                                break;
                                            }
                                        }
                                        if (bfCapitalAccessible) break;
                                    }
                                }
                            }

                            DeployObjectiveSequencingPolicy.BespinFirstEvaluation bespinFirst =
                                DeployObjectiveSequencingPolicy.evaluateBespinFirst(
                                    new DeployObjectiveSequencingFacts.BespinFirstDecision(
                                        actionId, objectiveForbidsExecutor,
                                        bfOracleAnalyzed, bfCapitalAccessible));
                            PolicyContributionLedger bespinFirstLedger =
                                new PolicyContributionLedger(
                                    (decisionId == null || decisionId.isBlank()
                                        ? "deploy-bespin-first"
                                        : decisionId + "-deploy-bespin-first")
                                        + "-" + actionId);
                            bespinFirstLedger.register(bespinFirst.result());
                            PolicyOperationAdapter.apply(action, bespinFirstLedger);
                            if (bespinFirst.outcome()
                                    == DeployObjectiveSequencingPolicy.BespinFirstOutcome.RELEASED) {
                                LOG.info("V29 BESPIN-FIRST RELEASED: NOT blocking deploy '{}' on turn {} — {}",
                                    actionText.length() > 60 ? actionText.substring(0, 60) : actionText,
                                    bfTurn, bespinFirst.releaseReason());
                            } else {
                                LOG.warn("V29 BESPIN-FIRST: BLOCKING deploy '{}' on turn {} — Bespin not occupied, deploy Executor first!",
                                    actionText.length() > 60 ? actionText.substring(0, 60) : actionText, bfTurn);
                            }
                            if (bespinFirst.adapterStep()
                                    == DeployObjectiveSequencingPolicy.AdapterStep.CONTINUE_ACTION) {
                                actions.add(action);
                                continue;
                            }
                        }
                    }
                }
            }

            // === Look up the card using multiple methods (like Python) ===
            PhysicalCard card = null;
            String blueprintIdFromHtml = null;

            LOG.warn("🔎 [Method 1] Trying extractBlueprintFromActionHtml for: '{}'", actionText);

            // Method 1: Extract blueprint from action text HTML (most reliable)
            // GEMP includes card hints like: <div class='cardHint' value='7_305'>•Card Name</div>
            blueprintIdFromHtml = extractBlueprintFromActionHtml(actionText);
            LOG.warn("🔎 [Method 1] Result: blueprintIdFromHtml = '{}'", blueprintIdFromHtml);

            if (blueprintIdFromHtml != null && hand != null) {
                LOG.warn("🔎 [Method 1] Searching hand ({} cards) for blueprint '{}'", hand.size(), blueprintIdFromHtml);
                // Find card in hand by blueprint ID
                for (PhysicalCard handCard : hand) {
                    if (handCard != null && blueprintIdFromHtml.equals(handCard.getBlueprintId(true))) {
                        card = handCard;
                        LOG.warn("🔎 [Method 1] ✅ Found card by HTML blueprint {}: {}", blueprintIdFromHtml, card.getTitle());
                        break;
                    }
                }
                if (card == null) {
                    LOG.warn("🔎 [Method 1] ❌ No card in hand matches blueprint '{}'", blueprintIdFromHtml);
                }
            } else {
                LOG.warn("🔎 [Method 1] Skipped: blueprintIdFromHtml={}, hand={}", blueprintIdFromHtml, hand != null ? "exists" : "null");
            }

            // Method 2: Try to find card by cardId in game state
            LOG.warn("🔎 [Method 2] Trying gameState.findCardById for cardIdStr='{}' (card still null={})", cardIdStr, card == null);
            if (card == null && cardIdStr != null && !cardIdStr.isEmpty() && gameState != null) {
                try {
                    int cardIdNum = Integer.parseInt(cardIdStr);
                    LOG.warn("🔎 [Method 2] Parsed cardId as int: {}", cardIdNum);
                    card = gameState.findCardById(cardIdNum);
                    if (card != null) {
                        LOG.warn("🔎 [Method 2] ✅ Found card by ID {}: {}", cardIdNum, card.getTitle());
                    } else {
                        LOG.warn("🔎 [Method 2] ❌ gameState.findCardById({}) returned null", cardIdNum);
                    }
                } catch (NumberFormatException e) {
                    LOG.warn("🔎 [Method 2] ❌ Could not parse cardId '{}' as integer", cardIdStr);
                }
            } else {
                LOG.warn("🔎 [Method 2] Skipped: card={}, cardIdStr='{}', gameState={}",
                    card != null ? "found" : "null", cardIdStr, gameState != null ? "exists" : "null");
            }

            // Method 3: Try to use blueprintId from decision params
            LOG.warn("🔎 [Method 3] Trying blueprintIdFromParam='{}' (card still null={})", blueprintIdFromParam, card == null);
            if (card == null && blueprintIdFromParam != null && !blueprintIdFromParam.isEmpty() &&
                !"inPlay".equals(blueprintIdFromParam) && hand != null) {
                LOG.warn("🔎 [Method 3] Searching hand for blueprint '{}'", blueprintIdFromParam);
                // Find card in hand by blueprint ID from params
                for (PhysicalCard handCard : hand) {
                    if (handCard != null && blueprintIdFromParam.equals(handCard.getBlueprintId(true))) {
                        card = handCard;
                        LOG.warn("🔎 [Method 3] ✅ Found card by param blueprint {}: {}", blueprintIdFromParam, card.getTitle());
                        break;
                    }
                }
                if (card == null) {
                    LOG.warn("🔎 [Method 3] ❌ No card in hand matches blueprint '{}'", blueprintIdFromParam);
                }
            } else {
                LOG.warn("🔎 [Method 3] Skipped: card={}, blueprintIdFromParam='{}', hand={}",
                    card != null ? "found" : "null", blueprintIdFromParam, hand != null ? "exists" : "null");
            }

            // Method 4: Fallback - try to match by title in action text (rarely needed)
            if (card == null) {
                card = findCardInHand(hand, actionText);
                if (card != null) {
                    LOG.info("🔎 [Method 4] ✅ Found card by title match: {}", card.getTitle());
                }
                // Don't log failure - findCardInHand already logs at debug level
            }

            // Final result
            if (card == null) {
                LOG.warn("❌❌❌ CARD LOOKUP FAILED for action '{}' - ALL 4 METHODS FAILED ❌❌❌",
                    actionText.length() > 80 ? actionText.substring(0, 80) + "..." : actionText);
                LOG.warn("    cardIdStr='{}', blueprintFromHtml='{}', blueprintFromParam='{}'",
                    cardIdStr, blueprintIdFromHtml, blueprintIdFromParam);
            } else {
                LOG.warn("✅✅✅ CARD LOOKUP SUCCESS: {} (bp={}) ✅✅✅", card.getTitle(), card.getBlueprintId(true));
            }
            if (card != null) {
                SwccgCardBlueprint blueprint = card.getBlueprint();
                if (blueprint != null) {
                    action.setCardName(card.getTitle());

                    // === OBJECTIVE DEPLOY DECISIONS (consolidated 2026-07-07 per Steve) ===
                    // The deploy phase CHECKS the objective brain (ObjectiveAnalyzer) for objective-specific
                    // deploy scoring instead of inlining it here. V83/V110/V108/V86/V88 (My Lord / Invasion,
                    // objective-gated) + V99 (Senate guard, deliberately ungated) now live in
                    // ObjectiveAnalyzer.getDeployObjectiveAdjustments(); each ScoreNote is applied HERE via
                    // action.addReasoning at the SAME position the old blocks fired, so additive-score ordering
                    // is unchanged. The superseded inline blocks were removed 2026-07-13 after source parity
                    // proof; ObjectiveAnalyzer is the sole owner of these six arms.
                    {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objDeploy =
                            context.getObjectiveAnalyzer();
                        if (objDeploy != null && gameState != null && game != null
                                && card != null && blueprint != null && actionText != null) {
                            if (!objectiveInactivationChecked
                                    && applyRequiredCardInactivationVeto(
                                        context, action, decisionId, actionId,
                                        card)) {
                                actions.add(action);
                                continue;
                            }
                            for (com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.ScoreNote note
                                    : objDeploy.getDeployObjectiveAdjustments(
                                        game, gameState, playerId, card, blueprint, actionText)) {
                                action.addReasoning(note.reason, note.score);
                            }
                        }
                    }

                    {
                        var captureAnalyzer =
                            context.getObjectiveAnalyzer();
                        CaptureObjectivePolicy.ObjectiveKind captureKind =
                            CaptureObjectiveFacts.objectiveKind(
                                captureAnalyzer);
                        if (captureKind != null
                                && gameState != null && game != null
                                && playerId != null) {
                            PhysicalCard explicitDestination = null;
                            int explicitDestinationTitleLength = -1;
                            String captureActionLower =
                                actionText.toLowerCase(Locale.ROOT);
                            for (PhysicalCard target
                                    : gameState
                                        .getAllPermanentCards()) {
                                CardCategory targetCategory =
                                    target != null
                                        && target.getBlueprint()
                                            != null
                                        ? target.getBlueprint()
                                            .getCardCategory()
                                        : null;
                                String targetTitle =
                                    target != null
                                        && target.getZone() != null
                                        && target.getZone()
                                            .isInPlay()
                                        && (targetCategory
                                                == CardCategory.LOCATION
                                            || targetCategory
                                                == CardCategory.VEHICLE
                                            || targetCategory
                                                == CardCategory.STARSHIP)
                                        ? target.getTitle() : null;
                                if (targetTitle != null
                                        && targetTitle.length()
                                            > explicitDestinationTitleLength
                                        && captureActionLower.contains(
                                            targetTitle.toLowerCase(
                                                Locale.ROOT))) {
                                    explicitDestination = target;
                                    explicitDestinationTitleLength =
                                        targetTitle.length();
                                }
                            }
                            boolean guaranteedCapture =
                                explicitDestination != null
                                ? CaptureObjectiveFacts
                                    .guaranteesImmediateCaptureAt(
                                        game, playerId,
                                        captureAnalyzer, card,
                                        explicitDestination)
                                : CaptureObjectiveFacts
                                    .hasLegalImmediateCaptureDeployDestination(
                                        game, playerId,
                                        captureAnalyzer, card);
                            boolean bhbmYourDestiny =
                                explicitDestination != null
                                ? BhbmSetupPayoffFactsReader
                                    .rewardsVaderForDeployAt(
                                        game, playerId,
                                        captureAnalyzer, card,
                                        explicitDestination)
                                : BhbmSetupPayoffFactsReader
                                    .hasLegalYourDestinyDeployDestination(
                                        game, playerId,
                                        captureAnalyzer, card);
                            PolicyContributionLedger captureDeployLedger =
                                new PolicyContributionLedger(
                                    (decisionId == null
                                        || decisionId.isBlank()
                                        ? "capture-deploy-parent"
                                        : decisionId
                                            + "-capture-deploy-parent")
                                    + "-" + actionId);
                            captureDeployLedger.register(
                                CaptureObjectivePolicy
                                    .scoreDeployCaptureRoute(
                                        new CaptureObjectivePolicy
                                            .DeployCaptureFacts(
                                                actionId,
                                                captureKind,
                                                CaptureObjectivePolicy
                                                    .CaptureRouteStep.PARENT,
                                                guaranteedCapture)));
                            captureDeployLedger.register(
                                CaptureObjectivePolicy
                                    .scoreBhbmYourDestiny(
                                        new CaptureObjectivePolicy
                                            .BhbmYourDestinyFacts(
                                                actionId,
                                                bhbmYourDestiny)));
                            PolicyOperationAdapter.apply(
                                action, captureDeployLedger);
                        }
                    }


                    boolean v212EvazanWithoutArmedFriend = false;
                    String v212SitingSiteTitle = "";
                    float v212V136Score = 0.0f;
                    boolean v212V193Eligible = false;
                    boolean v212V193FormationSupported = true;
                    float v212V193Weight = 400.0f;
                    String v212V193GateCard = "";
                    boolean v212V96Applicable = false;
                    float v212V96FriendlyPower = 0.0f;
                    float v212V96OpponentPower = 0.0f;

                    // === V89 (Steve, 2026-05-18): DR. EVAZAN — NEEDS ARMED PARTNER ===
                    // Per Steve: "Dr. Evazan should be deployed with another
                    // character with a weapon. Should never deploy alone. This
                    // can be with a character that has a permanent weapon on
                    // them or a weapon card deployed on them."
                    //
                    // Dr. Evazan has low forfeit/power. Without an armed friend
                    // at the same site, he gets sniped. Catches both the solo
                    // "Dr. Evazan" and paired "Dr. Evazan & Ponda Baba" cards
                    // via title-prefix check. Filters.character_with_a_weapon
                    // covers BOTH deployed weapon cards AND permanent weapons
                    // (armedWith() includes permanents per its javadoc).
                    {
                        String cardTitleForEvazan = card.getTitle();
                        if (cardTitleForEvazan != null
                                && cardTitleForEvazan.startsWith("Dr. Evazan")
                                && gameState != null && game != null) {
                            // Find target location from action text.
                            PhysicalCard evazanTargetLoc = null;
                            String evazanActionLower = actionText.toLowerCase(Locale.ROOT);
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (evazanActionLower.contains(loc.getTitle().toLowerCase(Locale.ROOT))) {
                                    evazanTargetLoc = loc;
                                    break;
                                }
                            }
                            if (evazanTargetLoc != null) {
                                // Look for any friendly armed character at the target location.
                                boolean armedFriendAtTarget = false;
                                for (PhysicalCard pCard : gameState.getAllPermanentCards()) {
                                    if (pCard == null) continue;
                                    if (!playerId.equals(pCard.getOwner())) continue;
                                    if (pCard == card) continue; // skip self
                                    // Must be present at target location
                                    PhysicalCard pCardLoc = null;
                                    try {
                                        pCardLoc = game.getModifiersQuerying().getLocationThatCardIsAt(gameState, pCard);
                                    } catch (Exception ignore) { /* skip if not at a location */ }
                                    if (pCardLoc != evazanTargetLoc) continue;
                                    if (com.gempukku.swccgo.filters.Filters.character_with_a_weapon.accepts(
                                            gameState, game.getModifiersQuerying(), pCard)) {
                                        armedFriendAtTarget = true;
                                        break;
                                    }
                                }
                                if (!armedFriendAtTarget) {
                                    v212EvazanWithoutArmedFriend = true;
                                    v212SitingSiteTitle = evazanTargetLoc.getTitle();
                                    LOG.warn("V89 DR. EVAZAN: blocking {} → {} (no armed friend present)",
                                        cardTitleForEvazan, evazanTargetLoc.getTitle());
                                }
                            }
                        }
                    }

                    // === V136 (Steve, 2026-05-26): UNIFIED CHARACTER DEPLOY SITE EVALUATOR ===
                    // Supersedes V90 (below), V67aj, V67al (later in this file), V122
                    // and V67as (CardSelectionEvaluator). One score per (card, site)
                    // pair via common/strategy/CharacterDeploySiteEvaluator. See
                    // V136_DEPLOY_LOG.md for revert plan and /tmp/V136_SPEC_V3.md for
                    // the dominance table.
                    if (card != null && blueprint != null
                            && blueprint.getCardCategory() == CardCategory.CHARACTER
                            && gameState != null && game != null) {
                        PhysicalCard v136Candidate = null;
                        String v136ActionLower = actionText.toLowerCase(Locale.ROOT);
                        for (PhysicalCard loc : gameState.getTopLocations()) {
                            if (loc == null || loc.getTitle() == null) continue;
                            if (v136ActionLower.contains(loc.getTitle().toLowerCase(Locale.ROOT))) {
                                v136Candidate = loc;
                                break;
                            }
                        }
                        if (v136Candidate != null) {
                            int v136Turn = gameState.getPlayersLatestTurnNumber(playerId);
                            java.util.List<PhysicalCard> v136Hand = gameState.getHand(playerId);
                            int v136ForceAvail = gameState.getForcePileSize(playerId);
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v136Obj =
                                context.getObjectiveAnalyzer();
                            // step 3b (2026-07-10): filter-based relevance overload (rules) — for objectives
                            // WITHOUT rules this equals the old title/fragment check (behavior-neutral).
                            boolean v136CountedFormationExemption = true;
                            if (v136Obj != null
                                    && v136Obj.hasCountedOperativeFormationRule()) {
                                boolean v136ExactPlannedPair = false;
                                if (plan != null && plan.getReason() != null
                                        && plan.getReason().startsWith(
                                            "Objective counted-operative formations")) {
                                    DeploymentInstruction v136Instruction =
                                        plan.getInstructionForPhysicalCard(
                                            card.getPermanentCardId(), card.getCardId(),
                                            card.getBlueprintId(true));
                                    if (v136Instruction != null) {
                                        java.util.Set<Integer> v136CharacterIdsInHand =
                                            new java.util.HashSet<>();
                                        for (PhysicalCard v136HandCard : v136Hand) {
                                            if (v136HandCard != null
                                                    && v136HandCard.getBlueprint() != null
                                                    && v136HandCard.getBlueprint().getCardCategory()
                                                        == CardCategory.CHARACTER) {
                                                v136CharacterIdsInHand.add(
                                                    v136HandCard.getPermanentCardId());
                                            }
                                        }
                                        Float v136BuddyCost =
                                            plan.getCheapestPlannedCharacterBuddyCost(
                                                v136Instruction,
                                                String.valueOf(v136Candidate.getCardId()),
                                                v136CharacterIdsInHand);
                                        v136ExactPlannedPair = v136BuddyCost != null
                                            && v136Instruction.getDeployCost()
                                                + v136BuddyCost <= v136ForceAvail;
                                    }
                                }
                                v136CountedFormationExemption =
                                    v136Obj.advancesPreFlipRequirementAt(
                                        game, playerId, card, v136Candidate)
                                    || v136ExactPlannedPair;
                            }
                            boolean v136ObjRelevant = v136Obj != null && v136Obj.isAnalyzed()
                                && v136Candidate.getTitle() != null
                                && v136Obj.isObjectiveRelevantLocation(v136Candidate, game, playerId);
                            float v136Score = com.gempukku.swccgo.ai.models.common.strategy
                                .CharacterDeploySiteEvaluator.evaluateSite(
                                    game, card, v136Candidate, playerId,
                                    v136ObjRelevant,
                                    v136ObjRelevant && v136CountedFormationExemption,
                                    v136Hand,
                                    v136ForceAvail,
                                    v136Turn,
                                    0 /* deckShipCount — TODO wire */,
                                    false /* perSiteEffectActive — TODO wire */);
                            v212SitingSiteTitle = v136Candidate.getTitle();
                            v212V136Score = v136Score;
                            if (v136Score != 0f) {
                                LOG.info("V136 [{}]: {} → {} score={}", playerId,
                                    card.getTitle(), v136Candidate.getTitle(), v136Score);
                            }

                            // === V193 (Steve, 2026-07-07): BUNKER-CONTROL BONUS (Endor Operations flip gate) ===
                            // Endor Operations (dark) flips once Ominous Rumors + Establish Secret Base are
                            // both on table. Establish Secret Base (V) (207_25) "Deploy on Bunker if you
                            // control that site" — so the LAST flip-card only reaches the table once Rando
                            // CONTROLS Endor: Bunker. In the diagnosed game (replay qgdridfo166f27r3) Bunker
                            // sat empty all game (us:0 them:0) while Rando piled every body onto Endor:
                            // Landing Platform, so the cost-0 Establish Secret Base in hand was never a legal
                            // deploy and the objective never flipped. Steer exactly ONE body to Bunker to
                            // seize control. Self-limiting/one-shot: fires only while (a) the objective
                            // analyzer named Bunker as the flip-critical control site (ObjectiveAnalyzer
                            // V193), (b) Rando does NOT already control it, and (c) Establish Secret Base is
                            // still in hand/reserve (flip is reachable). Once one body lands on the empty
                            // site Rando controls it -> guard (b) closes -> the bonus never stacks per-body,
                            // and the rest of the pile reverts to normal siting (Landing Platform for drains).
                            //
                            // BOUNDARY MATH (from the diagnosis of that game):
                            //   Landing Platform, first body  ≈ 660 (docking-bay empty-bay +80, V136 CS
                            //     350->600, V23 drain +30, high-ability +50, reinforce-solo +150, buddy +40,
                            //     battleground +80, V22 non-obj -40).
                            //   Endor: Bunker, first body, WITH ObjectiveAnalyzer V193 relevance but WITHOUT
                            //     this bonus ≈ 430-470 (BG bonuses + obj +200, no drain/empty-bay/reinforce).
                            //   +400 -> Bunker ≈ 830-870 > 660, so the FIRST body picks Bunker (beats the
                            //     pile by ~170-240). After that body lands Rando controls Bunker, guard (b)
                            //     closes, and Bunker drops back below Landing Platform for every later body.
                            if (v136Obj != null && v136Obj.isAnalyzed()
                                    && v136Candidate.getTitle() != null) {
                                String v193GateSite = v136Obj.getFlipCriticalControlSite();
                                if (v193GateSite != null
                                        && v193GateSite.equalsIgnoreCase(v136Candidate.getTitle())) {
                                    boolean v193ActorGateCandidate =
                                        v136Obj.advancesUnfilledFlipGateActorRequirement(
                                            game, playerId, card, v136Candidate);
                                    if (v136Obj.hasFlipGateActorRequirement()) {
                                        int v193FriendlyCharacters = com.gempukku.swccgo.ai.models.common.strategy
                                            .FormationSafety.countFriendlyNonUndercoverCharacters(
                                                gameState.getCardsAtLocation(v136Candidate), playerId);
                                        Float v193ThisCost = blueprint.getDeployCost();
                                        Float v193BuddyCost = null;
                                        if (plan != null) {
                                            DeploymentInstruction v193Instruction =
                                                plan.getInstructionForPhysicalCard(
                                                    card.getPermanentCardId(), card.getCardId(),
                                                    card.getBlueprintId(true));
                                            if (v193Instruction != null) {
                                                java.util.Set<Integer> v193CharacterIdsInHand =
                                                    new java.util.HashSet<>();
                                                for (PhysicalCard v193HandCard : gameState.getHand(playerId)) {
                                                    if (v193HandCard != null && v193HandCard.getBlueprint() != null
                                                            && v193HandCard.getBlueprint().getCardCategory()
                                                                == CardCategory.CHARACTER) {
                                                        v193CharacterIdsInHand.add(
                                                            v193HandCard.getPermanentCardId());
                                                    }
                                                }
                                                v193BuddyCost = plan.getCheapestPlannedCharacterBuddyCost(
                                                    v193Instruction,
                                                    String.valueOf(v136Candidate.getCardId()),
                                                    v193CharacterIdsInHand);
                                            }
                                        }
                                        boolean v193FundedBuddy = v193BuddyCost != null
                                            && v193ThisCost != null
                                            && v193ThisCost + v193BuddyCost <= v136ForceAvail;
                                        v212V193FormationSupported =
                                            v193FriendlyCharacters > 0 || v193FundedBuddy;
                                    }
                                    boolean v193AlreadyControls =
                                        com.gempukku.swccgo.cards.GameConditions.controls(
                                            game, playerId, v136Candidate);
                                    // The flip-gate CARD (whose deploy needs this control) comes from
                                    // the objective logic too — no card name hardcoded here, so this
                                    // steer generalizes to any occupation objective the analyzer flags.
                                    String v193GateCard = v136Obj.getFlipCriticalControlCard();
                                    if (v193GateCard == null && v193ActorGateCandidate) {
                                        v193GateCard = v136Obj.getFlipGateActorRequirementLabel();
                                    }
                                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle v193Oracle =
                                        context.getDeckOracle();
                                    // FIX 2026-07-07: detect by the analyzer's scoped Bunker-gated
                                    // blueprint ids when present (Establish Secret Base V 207_25 /
                                    // Legacy 601_260 — NOT the base 8_124, which gates on 3 Endor
                                    // sites, not Bunker). Empty set → fall back to the title name so
                                    // any future flip-gate objective that names only a card still works.
                                    java.util.Set<String> v193GateIds = v136Obj.getFlipCriticalControlCardIds();
                                    boolean v193HoldsGateCard = false;
                                    if (v193Oracle != null) {
                                        if (v193GateIds != null && !v193GateIds.isEmpty()) {
                                            for (String v193Id : v193GateIds) {
                                                if (v193Oracle.isCardInHand(v193Id)
                                                        || v193Oracle.isCardInReserve(v193Id)) {
                                                    v193HoldsGateCard = true;
                                                    break;
                                                }
                                            }
                                        } else if (v193GateCard != null) {
                                            v193HoldsGateCard = v193Oracle.isCardInHand(v193GateCard)
                                                || v193Oracle.isCardInReserve(v193GateCard);
                                        }
                                    }
                                    boolean v193CanAdvanceGate =
                                        (v193HoldsGateCard || v193ActorGateCandidate)
                                            && v212V193FormationSupported;
                                    // V297: actor-gated objectives receive the objective score only
                                    // when an exact buddy is funded or already present at the gate.
                                    if ((!v193AlreadyControls || v193ActorGateCandidate)
                                            && v193CanAdvanceGate) {
                                        // ObjectivePlaybook consolidation (2026-07-07): the +400 magnitude
                                        // is now analyzer-owned in ENDOR_PLAYBOOK.weights.deployFlipGateSite.
                                        // Behavior-preserving: V193 only fires when the analyzer named a
                                        // flip-gate site (Endor Operations today), which also selects
                                        // ENDOR_PLAYBOOK (weight = 400). Fall back to the literal if no
                                        // playbook is active (defensive; unreachable on the Endor path).
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.ObjectivePlaybook
                                            v193Playbook = v136Obj.getActivePlaybook();
                                        float v193Bonus = (v193Playbook != null)
                                            ? v193Playbook.weights.deployFlipGateSite : 400.0f;
                                        v212V193Eligible = true;
                                        v212V193Weight = v193Bonus;
                                        v212V193GateCard = v193GateCard;
                                        LOG.warn("V193 FLIP-GATE CONTROL [{}]: {} → {} +{} (seize flip-gate, card={})",
                                            playerId, card.getTitle(), v136Candidate.getTitle(), v193Bonus, v193GateCard);
                                    }
                                }
                            }
                        }
                    }

                    // V90 if(false) NO-SUICIDE-DEPLOY block DELETED 2026-07-12 batch 1.5 (V136 §A owns team viability) — see git history.

                    // === V96 (Steve, 2026-05-20): CONCENTRATE AT CONTESTED SITES ===
                    // Per Steve: "I basically bombard characters. When it comes to
                    // places where a battle is likely going to happen, those are
                    // great places to put maximum amount of characters/vehicles
                    // to overpower me. Overpowering is a quick way to win because
                    // that causes overflow damage."
                    //
                    // NOTE (updated 2026-07-12 batch 1.5): V67al is DEAD — superseded by
                    // V136 §B; its code (the old `if (false /* V67aj SUPERSEDED V136 */)`
                    // block in this file) was DELETED in batch 1.5 — revert path = git
                    // history. Historical context: V67al penalized ANY deploy
                    // when friendly power at a site exceeded 20, regardless of opponent
                    // power. That was wrong when opponent has comparable power: we should
                    // CONCENTRATE for overflow battle, not spread.
                    //
                    // Rule: if target location has opponent presence AND the
                    // friendly-vs-opponent power diff is close (within 10), give
                    // a STRONG bonus. If already crushing (diff > 10), small bonus.
                    // Uncontested sites get no V96 bonus (the uncontested over-stack
                    // penalty is V136 §B's job now, not dead V67al's).
                    if (card != null && blueprint != null
                            && blueprint.getCardCategory() == CardCategory.CHARACTER
                            && gameState != null && game != null) {
                        try {
                            PhysicalCard v96TargetLoc = null;
                            String v96ActionLower = actionText.toLowerCase(Locale.ROOT);
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (v96ActionLower.contains(loc.getTitle().toLowerCase(Locale.ROOT))) {
                                    v96TargetLoc = loc;
                                    break;
                                }
                            }
                            if (v96TargetLoc != null) {
                                float friendlyPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, v96TargetLoc, playerId, false, false);
                                String v96Opp = game.getOpponent(playerId);
                                float opponentPower = (v96Opp != null)
                                    ? game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, v96TargetLoc, v96Opp, false, false)
                                    : 0f;
                                v212SitingSiteTitle = v96TargetLoc.getTitle();
                                v212V96Applicable = true;
                                v212V96FriendlyPower = friendlyPower;
                                v212V96OpponentPower = opponentPower;
                                // opponentPower == 0 → uncontested, no V96 bonus. (Comment corrected
                                // 2026-07-06: V67al is DEAD; V136 §B's uncontested over-stack penalty
                                // owns this case.)
                            }
                        } catch (Exception e) {
                            LOG.debug("V96 CONCENTRATE: error: {}", e.getMessage());
                        }
                    }

                    DeploySitingPolicy.Facts v212SitingFacts = new DeploySitingPolicy.Facts(
                        actionId, card.getTitle(), v212SitingSiteTitle,
                        v212EvazanWithoutArmedFriend, DeploySitingPolicy.FormationState.ALLOW, "",
                        v212V136Score, v212V193Eligible, v212V193FormationSupported,
                        v212V193Weight,
                        v212V193GateCard, v212V96Applicable,
                        v212V96FriendlyPower, v212V96OpponentPower);
                    PolicyContributionLedger v212SitingLedger = new PolicyContributionLedger(
                        (decisionId == null || decisionId.isBlank()
                            ? "deploy-siting" : decisionId + "-deploy-siting") + "-" + actionId);
                    v212SitingLedger.register(DeploySitingPolicy.evaluateDirect(v212SitingFacts));
                    PolicyOperationAdapter.apply(action, v212SitingLedger);

                    // DEPLOY-1 plan application. The planner remains the fact producer;
                    // this shared policy is the sole score and terminal-flow owner.
                    String blueprintId = card.getBlueprintId(true);
                    DeploymentInstruction plannedInstruction = plan != null
                        ? plan.getInstructionForPhysicalCard(
                            card.getPermanentCardId(), card.getCardId(), blueprintId)
                        : null;
                    boolean locationStrategy = plan != null
                        && plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS;
                    boolean tdigwattPlan = context.getObjectiveAnalyzer() != null
                        && context.getObjectiveAnalyzer().isAnalyzed()
                        && context.getObjectiveAnalyzer().isTdigwatt();
                    boolean countedOperativeFormationPlan = plan != null
                        && plan.getReason() != null
                        && plan.getReason().startsWith(
                                "Objective counted-operative formations");
                    boolean objectiveFormationPlan = plan != null
                        && plan.getReason() != null
                        && (plan.getReason().startsWith(
                                "V297 objective flip-gate formation")
                            || countedOperativeFormationPlan);
                    boolean eopBunkerGarrisonPlan = plan != null
                        && EndorOperationsTacticalPolicy
                            .isBunkerGarrisonPlan(plan.getReason());
                    int objectiveFormationReserve = 0;
                    if (objectiveFormationPlan && plannedDeployActionOffered) {
                        for (DeploymentInstruction instruction : plan.getInstructions()) {
                            if (instruction != plannedInstruction) {
                                objectiveFormationReserve += instruction.getDeployCost();
                            }
                        }
                    }
                    int firstOrderReignsRouteReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getFirstOrderReignsRouteForceReserve(
                                    game, playerId, card)
                            : 0;
                    int captureMoveForceReserve =
                        context.getObjectiveAnalyzer() != null
                            ? CaptureObjectiveFacts
                                .nextCaptureMoveForceReserve(
                                    game, playerId,
                                    context.getObjectiveAnalyzer(),
                                    card)
                            : 0;
                    Integer exactNormalDeployPayment = !reservePull
                        ? CaptureDeployBudgetFactsReader.actionPayment(
                            context.getExtra(
                                CaptureDeployBudgetFactsReader
                                    .ACTION_PAYMENTS_EXTRA),
                            actionId)
                        : null;
                    Integer exactCaptureDeployPayment =
                        captureMoveForceReserve > 0
                            ? exactNormalDeployPayment : null;
                    boolean unknownCaptureDeployPayment =
                        captureMoveForceReserve > 0
                            && !reservePull
                            && exactCaptureDeployPayment == null;
                    int massassiPackageReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getMassassiAttackRunPackageForceReserve(
                                    game, playerId, card)
                            : 0;
                    int massassiMoveReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getMassassiAttackRunCarrierMoveForceReserve(
                                    game, playerId)
                            : 0;
                    int massassiRouteReserve =
                        massassiPackageReserve + massassiMoveReserve;
                    int setYourCourseRouteReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getSetYourCourseNextRouteForceReserve(
                                    game, playerId)
                            : 0;
                    int oldAlliesRouteReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getOldAlliesFutureRouteForceReserve(
                                    game, playerId, card)
                            : 0;
                    int theyHaveNoIdeaRouteReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getTheyHaveNoIdeaFutureRouteForceReserve(
                                    game, playerId, card)
                            : 0;
                    boolean exactSetYourCourseSuperlaserDeploy =
                        context.getObjectiveAnalyzer() != null
                            && context.getObjectiveAnalyzer()
                                .isSetYourCourseCompatibleSuperlaserDeployCandidate(
                                    game, playerId, card);
                    int objectiveRequiredCardReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getRequiredOnTableCardForceReserve(
                                    game, playerId, card)
                                + context.getObjectiveAnalyzer()
                                    .getRequiredCardDeployEnablerForceReserve(
                                        game, playerId, card)
                                + context.getObjectiveAnalyzer()
                                    .getCountedObjectivePresenceForceReserve(
                                        game, playerId, card)
                            : 0;
                    int shieldRouteMoveReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getShieldMainGeneratorRouteMoveForceReserve(
                                    game, playerId)
                            : 0;
                    int countedOperativeMoveReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getCountedOperativeFormationMoveForceReserve(
                                    game, playerId, card)
                            : 0;
                    int countedOperativeBattleReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getCountedOperativeBattleForceReserve(
                                    game, playerId)
                            : 0;
                    int isbRebelBaseMoveReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getIsbRebelBaseMoveForceReserve(
                                    game, playerId, card)
                            : 0;
                    int isbRebelBaseBattleReserve =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getIsbRebelBaseBattleForceReserve(
                                    game, playerId, card)
                            : 0;
                    DeployPlanPolicy.Evaluation planEvaluation = DeployPlanPolicy.evaluate(
                        new DeployPlanPolicy.Facts(
                            actionId, plan != null,
                            plan != null && !plan.getInstructions().isEmpty(),
                            plannedInstruction != null,
                            objectiveFormationPlan,
                            eopBunkerGarrisonPlan,
                            plannedInstruction != null
                                ? plannedInstruction.getPriority() : Integer.MAX_VALUE,
                            plan != null && plan.isForceAllowExtras(),
                            plan != null && plan.isWaitingForPlannedCards(),
                            locationStrategy,
                            blueprint.getCardCategory() == CardCategory.LOCATION,
                            tdigwattPlan, context.getTurnNumber(), availableForce,
                            plan != null && plan.isPlanComplete(),
                            plan != null && plan.isPlanComplete()
                                ? plan.getExtraForceBudget(availableForce) : 0,
                            plan != null && blueprintId != null
                                && plan.getHoldBackCards().contains(blueprintId),
                            plan != null ? plan.getStrategy().getValue() : ""));
                    PolicyContributionLedger planLedger = new PolicyContributionLedger(
                        (decisionId == null || decisionId.isBlank()
                            ? "deploy-plan" : decisionId + "-deploy-plan") + "-" + actionId);
                    planLedger.register(planEvaluation.result());
                    PolicyOperationAdapter.apply(action, planLedger);
                    if (planEvaluation.adapterStep()
                            == DeployPlanPolicy.AdapterStep.CONTINUE_ACTION) {
                        actions.add(action);
                        continue;
                    }

                    // Get card stats (with type checks for safety)
                    // DeployCost exists on most deployable cards
                    int cost = 0;
                    int futureObligationDeployCost;
                    try {
                        Float deployCost = blueprint.getDeployCost();
                        cost = deployCost != null ? deployCost.intValue() : 0;
                        if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                            int totalForce = context.getGameState() != null
                                ? context.getGameState().getForcePileSize(context.getPlayerId()) : 0;
                            int maintenanceCost = com.gempukku.swccgo.ai.models.common.strategy
                                .MaintenanceFacts.maintainCost(blueprint);
                            int pendingDeployCost = 0;
                            try {
                                DeployPhasePlanner maintenancePlanner = context.getDeployPhasePlanner();
                                DeploymentPlan maintenancePlan = maintenancePlanner != null
                                    ? maintenancePlanner.getCurrentPlan() : null;
                                if (maintenancePlan != null && blueprintId != null) {
                                    for (DeploymentInstruction instruction : maintenancePlan.getInstructions()) {
                                        if (instruction == null
                                            || blueprintId.equals(instruction.getCardBlueprintId())) {
                                            continue;
                                        }
                                        pendingDeployCost += instruction.getDeployCost();
                                    }
                                }
                            } catch (Exception maintenancePlanError) {
                                LOG.debug("V59 MAINTENANCE: Error reading plan: {}",
                                    maintenancePlanError.getMessage());
                            }
                            DeployBudgetPolicy.Evaluation maintenance =
                                DeployBudgetPolicy.newMaintenanceCard(
                                    new DeployBudgetPolicy.NewMaintenanceFacts(
                                        actionId, blueprint.getTitle(), true, totalForce, cost,
                                        maintenanceCost, pendingDeployCost, 2, 2));
                            PolicyContributionLedger maintenanceLedger = new PolicyContributionLedger(
                                (decisionId == null || decisionId.isBlank()
                                    ? "deploy-new-maintenance"
                                    : decisionId + "-deploy-new-maintenance") + "-" + actionId);
                            maintenanceLedger.register(maintenance.result());
                            PolicyOperationAdapter.apply(action, maintenanceLedger);
                        }
                    } catch (UnsupportedOperationException e) {
                        // Card type doesn't support deployCost (e.g., Interrupt)
                    }
                    int exactMassassiDeployPayment =
                        context.getObjectiveAnalyzer() != null
                            ? context.getObjectiveAnalyzer()
                                .getMassassiAttackRunPackageDeployForcePayment(
                                    game, playerId, card)
                            : 0;
                    int massassiDeployPayment = Math.max(
                            cost, exactMassassiDeployPayment);
                    int countedOperativeDeployPayment =
                        exactNormalDeployPayment != null
                            ? exactNormalDeployPayment
                            : massassiDeployPayment;
                    futureObligationDeployCost = massassiDeployPayment;
                    if (exactCaptureDeployPayment != null) {
                        futureObligationDeployCost =
                            Math.max(
                                futureObligationDeployCost,
                                exactCaptureDeployPayment);
                    } else if (unknownCaptureDeployPayment) {
                        action.hardVeto(
                            "CAPTURE.BUDGET.UNKNOWN: cannot prove this deploy preserves the exact capture move Force");
                    }

                    // === V24.5: RESERVE FORCE FOR EXISTING MAINTENANCE CARDS ===
                    // If cards with maintenance costs are already in play, deploying this card
                    // must leave enough Force to pay their upkeep. Otherwise they get sacrificed.
                    if (cost > 0 && gameState != null) {
                        try {
                            int existingMaintenanceCost = context.getForceReserveFacts().maintenanceObligation;
                            DeployBudgetPolicy.Evaluation existingMaintenance =
                                DeployBudgetPolicy.existingMaintenance(
                                    actionId, gameState.getForcePileSize(context.getPlayerId()),
                                    cost, existingMaintenanceCost);
                            PolicyContributionLedger existingMaintenanceLedger = new PolicyContributionLedger(
                                (decisionId == null || decisionId.isBlank()
                                    ? "deploy-existing-maintenance"
                                    : decisionId + "-deploy-existing-maintenance") + "-" + actionId);
                            existingMaintenanceLedger.register(existingMaintenance.result());
                            PolicyOperationAdapter.apply(action, existingMaintenanceLedger);
                        } catch (Exception e) {
                            LOG.debug("V24.5: Error checking maintenance reserve: {}", e.getMessage());
                        }
                    }

                    // Power only exists on Character, Starship, Vehicle
                    int powerVal = 0;
                    if (blueprint.hasPowerAttribute()) {
                        Float power = blueprint.getPower();
                        powerVal = power != null ? power.intValue() : 0;
                    }

                    // Ability only exists on Character, some Vehicles
                    int abilityVal = 0;
                    if (blueprint.hasAbilityAttribute()) {
                        Float ability = blueprint.getAbility();
                        abilityVal = ability != null ? ability.intValue() : 0;
                    }

                    // Destiny exists on most cards
                    float destinyVal = 0;
                    try {
                        Float destiny = blueprint.getDestiny();
                        destinyVal = destiny != null ? destiny : 0;
                    } catch (UnsupportedOperationException e) {
                        // Card type doesn't support destiny
                    }

                    action.setDeployCost(massassiDeployPayment);

                    // === AFFORDABILITY CHECK ===
                    DeployBudgetPolicy.Evaluation affordability =
                        DeployBudgetPolicy.affordability(
                            actionId, massassiDeployPayment,
                            availableForce);
                    PolicyContributionLedger affordabilityLedger = new PolicyContributionLedger(
                        (decisionId == null || decisionId.isBlank()
                            ? "deploy-affordability"
                            : decisionId + "-deploy-affordability") + "-" + actionId);
                    affordabilityLedger.register(affordability.result());
                    PolicyOperationAdapter.apply(action, affordabilityLedger);
                    if (affordability.adapterStep()
                            == DeployBudgetPolicy.AdapterStep.CONTINUE_ACTION) {
                        actions.add(action);
                        continue;
                    }
                    if (countedOperativeFormationPlan
                            && plannedInstruction != null
                            && winnableBattle != null
                            && plannedInstruction.getTargetLocationName()
                                != null
                            && plannedInstruction.getTargetLocationName()
                                .equals(winnableBattle.locationTitle())
                            && availableForce
                                - countedOperativeDeployPayment >= 1) {
                        action.addReasoning(
                            "OBJECTIVE.COUNTED_OPERATIVE.BATTLE_FORCE_CONTINUITY: complete the planned contested team while retaining the initiation fee",
                            800.0f);
                    }
                    if (massassiDeployPayment > 0
                            && "deploy".equals(actionLower.trim())
                            && massassiRouteReserve > 0
                            && availableForce - massassiDeployPayment
                                < massassiRouteReserve) {
                        action.hardVeto(
                            "OBJECTIVE.MASSASSI.ATTACK_RUN_FORCE_RESERVE: preserve exact Force for the remaining Attack Run package and carrier movement");
                        actions.add(action);
                        continue;
                    }
                    if (!exactSetYourCourseSuperlaserDeploy
                            && (exactNormalDeployPayment != null
                                || "deploy".equals(actionLower.trim()))
                            && (exactNormalDeployPayment != null
                                ? exactNormalDeployPayment : cost) > 0
                            && setYourCourseRouteReserve > 0
                            && availableForce
                                - (exactNormalDeployPayment != null
                                    ? exactNormalDeployPayment : cost)
                                < setYourCourseRouteReserve) {
                        action.hardVeto(
                            "OBJECTIVE.SET_YOUR_COURSE.NEXT_PAYMENT_RESERVE: preserve the exact next Death Star movement payment");
                        actions.add(action);
                        continue;
                    }
                    if (cost > 0
                            && firstOrderReignsRouteReserve > 0
                            && availableForce - cost
                                < firstOrderReignsRouteReserve) {
                        action.hardVeto(
                            "OBJECTIVE.FIRST_ORDER_REIGNS.RESERVE_7: preserve Force for the Tracked Fleet chase ship, crew, and movement");
                    }
                    if (cost > 0
                            && "deploy".equals(actionLower.trim())
                            && shieldRouteMoveReserve > 0
                            && availableForce - cost
                                < shieldRouteMoveReserve) {
                        action.hardVeto(
                            "HOTH.SHIELD.MOVE_FORCE_RESERVE: preserve the selected walker's exact next forward landspeed payment");
                        actions.add(action);
                        continue;
                    }
                    boolean countedOperativeOrdinaryDeploy =
                        !reservePull
                        && "deploy".equals(actionLower.trim());
                    if (countedOperativeOrdinaryDeploy
                            && countedOperativeMoveReserve > 0) {
                        if (exactNormalDeployPayment == null
                                && massassiDeployPayment > 0) {
                            action.hardVeto(
                                "OBJECTIVE.COUNTED_OPERATIVE.MOVE_PAYMENT_UNKNOWN: cannot prove this deploy preserves the exact net-progress landspeed payment");
                            actions.add(action);
                            continue;
                        }
                        if (exactNormalDeployPayment != null
                                && availableForce
                                    - exactNormalDeployPayment
                                    < countedOperativeMoveReserve) {
                            action.hardVeto(
                                "OBJECTIVE.COUNTED_OPERATIVE.MOVE_FORCE_RESERVE: preserve the exact net-progress landspeed payment");
                            actions.add(action);
                            continue;
                        }
                    }
                    if (countedOperativeOrdinaryDeploy
                            && countedOperativeBattleReserve > 0) {
                        if (exactNormalDeployPayment == null
                                && massassiDeployPayment > 0) {
                            action.hardVeto(
                                "OBJECTIVE.COUNTED_OPERATIVE.BATTLE_PAYMENT_UNKNOWN: cannot prove this deploy preserves the pending winnable battle payment");
                            actions.add(action);
                            continue;
                        }
                        if (exactNormalDeployPayment != null
                                && availableForce
                                    - exactNormalDeployPayment
                                    < countedOperativeBattleReserve) {
                            action.hardVeto(
                                "OBJECTIVE.COUNTED_OPERATIVE.BATTLE_FORCE_RESERVE: preserve the pending winnable battle payment after the formation plan completes");
                            actions.add(action);
                            continue;
                        }
                    }
                    if (countedOperativeOrdinaryDeploy
                            && isbRebelBaseMoveReserve > 0) {
                        if (exactNormalDeployPayment == null
                                && massassiDeployPayment > 0) {
                            action.hardVeto(
                                "OBJECTIVE.ISB.REBEL_BASE_MOVE_PAYMENT_UNKNOWN: cannot prove this deploy preserves the exact route move payment");
                            actions.add(action);
                            continue;
                        }
                        if (exactNormalDeployPayment != null
                                && availableForce
                                    - exactNormalDeployPayment
                                    < isbRebelBaseMoveReserve) {
                            action.hardVeto(
                                "OBJECTIVE.ISB.REBEL_BASE_MOVE_FORCE_RESERVE: preserve the exact safe landspeed payment that completes the second Rebel Base location");
                            actions.add(action);
                            continue;
                        }
                    }
                    if (countedOperativeOrdinaryDeploy
                            && isbRebelBaseBattleReserve > 0) {
                        if (exactNormalDeployPayment == null
                                && massassiDeployPayment > 0) {
                            action.hardVeto(
                                "OBJECTIVE.ISB.REBEL_BASE_BATTLE_PAYMENT_UNKNOWN: cannot prove this deploy preserves the exact route battle payment");
                            actions.add(action);
                            continue;
                        }
                        if (exactNormalDeployPayment != null
                                && availableForce
                                    - exactNormalDeployPayment
                                    < isbRebelBaseBattleReserve) {
                            action.hardVeto(
                                "OBJECTIVE.ISB.REBEL_BASE_BATTLE_FORCE_RESERVE: preserve the exact winnable battle payment that completes the second Rebel Base location");
                            actions.add(action);
                            continue;
                        }
                    }
                    if (countedOperativeOrdinaryDeploy
                            && oldAlliesRouteReserve > 0) {
                        if (exactNormalDeployPayment == null
                                && massassiDeployPayment > 0) {
                            action.hardVeto(
                                "OBJECTIVE.OLD_ALLIES.ROUTE_PAYMENT_UNKNOWN: cannot prove this deploy preserves the summed Jakku route payments");
                            actions.add(action);
                            continue;
                        }
                        if (exactNormalDeployPayment != null
                                && availableForce
                                    - exactNormalDeployPayment
                                    < oldAlliesRouteReserve) {
                            action.hardVeto(
                                "OBJECTIVE.OLD_ALLIES.ROUTE_FORCE_RESERVE: preserve the remaining system, two-site, move, and battle payments");
                            actions.add(action);
                            continue;
                        }
                    }
                    if (countedOperativeOrdinaryDeploy
                            && theyHaveNoIdeaRouteReserve > 0) {
                        if (exactNormalDeployPayment == null
                                && massassiDeployPayment > 0) {
                            action.hardVeto(
                                "OBJECTIVE.THNI.ROUTE_PAYMENT_UNKNOWN: cannot prove this deploy preserves the exact Rogue One, pilot, and Data Vault payments");
                            actions.add(action);
                            continue;
                        }
                        if (exactNormalDeployPayment != null
                                && availableForce
                                    - exactNormalDeployPayment
                                    < theyHaveNoIdeaRouteReserve) {
                            action.hardVeto(
                                "OBJECTIVE.THNI.ROUTE_FORCE_RESERVE: preserve the remaining Rogue One, pilot, and Data Vault payments");
                            actions.add(action);
                            continue;
                        }
                    }

                    boolean obligationMaintenance = false;
                    int obligationMaintenanceCost = 0;
                    if (gameState != null) {
                        try {
                            obligationMaintenance = blueprint.hasIcon(
                                com.gempukku.swccgo.common.Icon.MAINTENANCE);
                            obligationMaintenanceCost = obligationMaintenance
                                ? com.gempukku.swccgo.ai.models.common.strategy.MaintenanceFacts
                                    .maintainCost(blueprint)
                                : 0;
                        } catch (Exception obligationError) {
                            LOG.debug("V29: Error checking maintenance during deploy: {}",
                                obligationError.getMessage());
                        }
                    }
                    DeployBudgetPolicy.Evaluation futureObligations =
                        DeployBudgetPolicy.futureObligations(
                            new DeployBudgetPolicy.FutureObligationFacts(
                                actionId, availableForce,
                                futureObligationDeployCost,
                                vaderMoveReserve,
                                v67zTransitReserve, v79VergeMoveReserve,
                                obligationMaintenance, obligationMaintenanceCost,
                                gameState != null && context.getForceReserveFacts().dtfActive,
                                gameState != null && context.getForceReserveFacts().grabberUnused,
                                objectiveFormationReserve,
                                objectiveRequiredCardReserve,
                                captureMoveForceReserve));
                    PolicyContributionLedger futureObligationLedger = new PolicyContributionLedger(
                        (decisionId == null || decisionId.isBlank()
                            ? "deploy-future-obligation"
                            : decisionId + "-deploy-future-obligation") + "-" + actionId);
                    futureObligationLedger.register(futureObligations.result());
                    PolicyOperationAdapter.apply(action, futureObligationLedger);

                    // === V38: REWORKED SOLO DEPLOY — VADER/EMPEROR SOLO OK, OTHERS NEED BUDDY PATH ===
                    // Vader and Emperor (ability >= 6) can deploy solo anywhere.
                    // Other characters need a buddy PATH to 7 ability — either:
                    //   1. Deploy to a location with a friendly character (reinforce)
                    //   2. A paired deploy is affordable (deploy 2 chars this turn)
                    //   3. Deploy to non-battleground adjacent to battleground (staging)
                    //   4. Objective-flip deploy
                    // This replaces the old V29 power < 6 hard block.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER
                            && abilityVal < 6  // V38: Vader/Emperor (ability 6+) always pass through
                            && powerVal < RandoConfig.MIN_SOLO_DEPLOY_POWER
                            && gameState != null && game != null) {
                        try {
                            // Step 1: Find the target location from action text
                            PhysicalCard targetLoc = null;
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (actionText.toLowerCase(Locale.ROOT).contains(
                                        loc.getTitle().toLowerCase(Locale.ROOT))) {
                                    targetLoc = loc;
                                    break;
                                }
                            }

                            // Step 2: Check if we already have characters at target location
                            boolean wouldBeSolo = true;
                            if (targetLoc != null) {
                                float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, targetLoc, playerId, false, false);
                                if (ourPowerThere > 0) {
                                    wouldBeSolo = false;
                                }
                            }

                            if (wouldBeSolo) {
                                int spyMoveReserve = context.getForceReserveFacts().undercoverSpyCount;
                                if (spyMoveReserve > 0) {
                                    LOG.info("V53 SPY RESERVE: Reserving {} force for {} undercover spy movement(s)",
                                        spyMoveReserve, spyMoveReserve);
                                }

                                // Objective-flip and staging facts remain adapter-owned.
                                // Deploying to a location that helps flip the objective is strategically
                                // critical (e.g., occupying Bespin for Dark Deal/Cloud City Occupation).
                                boolean isObjectiveFlipDeploy = false;
                                boolean hasEscapeRoute = false;
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objAnalyzer =
                                    context.getObjectiveAnalyzer();
                                if (objAnalyzer != null && objAnalyzer.isAnalyzed() && !objAnalyzer.isFlipped()) {
                                    // Check if this character deploys to a location relevant to the flip
                                    if (targetLoc != null && targetLoc.getTitle() != null) {
                                        String targetLocLower = targetLoc.getTitle().toLowerCase(Locale.ROOT);
                                        if (objAnalyzer.isObjectiveRelevantLocation(targetLocLower)) {
                                            isObjectiveFlipDeploy = true;
                                            LOG.info("V29 OBJ-FLIP: {} deploying solo to objective-relevant '{}' for flip!",
                                                card.getTitle(), targetLoc.getTitle());

                                            // Check escape route: is there a connected location with our characters?
                                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                                if (loc == null || loc == targetLoc) continue;
                                                float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                    gameState, loc, playerId, false, false);
                                                if (ourPower > 0) {
                                                    // Rough adjacency check: Cloud City sites connect to each other
                                                    String locLower = loc.getTitle() != null ?
                                                        loc.getTitle().toLowerCase(Locale.ROOT) : "";
                                                    boolean sameSystem = (targetLocLower.contains("cloud city")
                                                            && locLower.contains("cloud city"))
                                                        || (targetLocLower.contains("bespin")
                                                            && locLower.contains("bespin"))
                                                        || (targetLocLower.contains("mapuzo")
                                                            && locLower.contains("mapuzo"));
                                                    if (sameSystem) {
                                                        hasEscapeRoute = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                boolean isStagingDeploy = false;
                                if (!isObjectiveFlipDeploy) {
                                    if (targetLoc != null && targetLoc.getBlueprint() != null) {
                                        boolean isNonBattleground = !targetLoc.getBlueprint().hasIcon(
                                            com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                            && !targetLoc.getBlueprint().hasIcon(
                                            com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                        // Check if any adjacent battleground exists
                                        // Simple heuristic: same planet prefix = adjacent
                                        if (isNonBattleground && targetLoc.getTitle() != null) {
                                            String stagingLocLower = targetLoc.getTitle().toLowerCase(Locale.ROOT);
                                            String stagingPlanet = stagingLocLower.contains(":")
                                                ? stagingLocLower.substring(0, stagingLocLower.indexOf(":")).trim() : "";
                                            if (!stagingPlanet.isEmpty()) {
                                                for (PhysicalCard adjLoc : gameState.getTopLocations()) {
                                                    if (adjLoc == null || adjLoc == targetLoc) continue;
                                                    String adjTitle = adjLoc.getTitle() != null
                                                        ? adjLoc.getTitle().toLowerCase(Locale.ROOT) : "";
                                                    if (adjTitle.startsWith(stagingPlanet)) {
                                                        // Same planet = likely adjacent
                                                        isStagingDeploy = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                DeployFormationSitingPolicy.LegacySoloEvaluation soloEvaluation =
                                    DeployFormationSitingPolicy.evaluateLegacySolo(
                                        new DeployFormationSitingPolicy.LegacySoloFacts(
                                            actionId, card.getTitle(), powerVal,
                                            targetLoc != null ? targetLoc.getTitle() : "?",
                                            true, true, isObjectiveFlipDeploy,
                                            hasEscapeRoute, isStagingDeploy));
                                applySharedPolicy(action, decisionId, actionId,
                                    "deploy-legacy-solo", soloEvaluation.result());
                                switch (soloEvaluation.outcome()) {
                                    case OBJECTIVE_WITH_ESCAPE:
                                        LOG.info("V29 OBJ-FLIP: Allowing solo {} for objective flip — has escape route",
                                            card.getTitle());
                                        break;
                                    case OBJECTIVE_NO_ESCAPE:
                                        LOG.warn("V29 OBJ-FLIP: Solo {} for flip but no escape route — penalizing",
                                            card.getTitle());
                                        break;
                                    case STAGING:
                                        LOG.info("V38 STAGING: {} deploying to staging site — can buddy next turn (-80)",
                                            card.getTitle());
                                        break;
                                    case CAUTION:
                                        LOG.info("V38 SOLO CAUTION: {} (power {}) — mild penalty (-150)",
                                            card.getTitle(), powerVal);
                                        break;
                                    case NONE:
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V29 SOLO CHECK: Error: {}", e.getMessage());
                        }
                    }

                    // === V38: REINFORCE STRONG ALLY ===
                    // If deploying ANY character to a site where Vader, Emperor, or another
                    // strong character is already present, big bonus. This enables the buddy
                    // rotation: deploy char A with Vader → next turn move char A, deploy char B.
                    // Vader is always the anchor buddy.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && gameState != null && game != null) {
                        try {
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (!actionLower.contains(loc.getTitle().toLowerCase(Locale.ROOT))) continue;

                                // Check if Vader or a strong ally is at this location
                                boolean vaderHere = false;
                                boolean strongAllyHere = false;
                                float allyAbilityHere = 0;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c == null || !playerId.equals(c.getOwner())) continue;
                                    if (c.getBlueprint() == null) continue;
                                    if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    if (isVader(game, gameState, c)) {
                                        vaderHere = true;
                                    }
                                    Float cAb = c.getBlueprint().getAbility();
                                    float cAbVal = cAb != null ? cAb : 0;
                                    allyAbilityHere += cAbVal;
                                    if (cAbVal >= 4) strongAllyHere = true;
                                }

                                DeployFormationSitingPolicy.StrongReinforcementEvaluation reinforcement =
                                    DeployFormationSitingPolicy.evaluateStrongReinforcement(
                                        new DeployFormationSitingPolicy.StrongReinforcementFacts(
                                            actionId, card.getTitle(), true, vaderHere,
                                            strongAllyHere, allyAbilityHere, abilityVal,
                                            RandoConfig.ABILITY_BUDDY_THRESHOLD));
                                applySharedPolicy(action, decisionId, actionId,
                                    "deploy-strong-reinforcement", reinforcement.result());
                                if (reinforcement.outcome()
                                        == DeployFormationSitingPolicy.StrongReinforcementOutcome.VADER) {
                                    LOG.warn("V38 REINFORCE VADER: {} deploying to Vader's site (+400)",
                                        card.getTitle());
                                }
                                break;
                            }
                        } catch (Exception e) { LOG.debug("V38 REINFORCE: Error: {}", e.getMessage()); }
                    }

                    // === V29: BUDDY-SEEK BONUS (legacy — strong char protects weak ally) ===
                    // If deploying a strong character (power >= 6) to a location
                    // where we have a vulnerable solo ally (power < 6 alone), give a big bonus.
                    // This steers Emperor/Vader/etc toward locations where Piett/Lando is alone.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER
                            && powerVal >= RandoConfig.MIN_SOLO_DEPLOY_POWER
                            && gameState != null) {
                        try {
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                String locTitle = loc.getTitle();
                                if (actionText.toLowerCase(Locale.ROOT).contains(locTitle.toLowerCase(Locale.ROOT))) {
                                    List<PhysicalCard> ourCardsHere = new ArrayList<>();
                                    for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                        if (c != null && context.getPlayerId().equals(c.getOwner())
                                            && c.getBlueprint() != null
                                            && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            ourCardsHere.add(c);
                                        }
                                    }
                                    if (ourCardsHere.size() == 1) {
                                        PhysicalCard soloAlly = ourCardsHere.get(0);
                                        SwccgCardBlueprint allyBp = soloAlly.getBlueprint();
                                        int allyPower = 0;
                                        if (allyBp.hasPowerAttribute()) {
                                            Float ap = allyBp.getPower();
                                            allyPower = ap != null ? ap.intValue() : 0;
                                        }
                                        if (allyPower < RandoConfig.MIN_SOLO_DEPLOY_POWER) {
                                            boolean v67abBgSeek = false;
                                            try {
                                                v67abBgSeek = game.getModifiersQuerying()
                                                    .isBattleground(gameState, loc, null);
                                            } catch (Exception e) { /* ignore */ }

                                            DeployFormationSitingPolicy.BuddySeekEvaluation buddySeek =
                                                DeployFormationSitingPolicy.evaluateBuddySeek(
                                                    new DeployFormationSitingPolicy.BuddySeekFacts(
                                                        actionId, true, true,
                                                        v67abBgSeek, soloAlly.getTitle(), allyPower,
                                                        locTitle));
                                            applySharedPolicy(action, decisionId, actionId,
                                                "deploy-buddy-seek", buddySeek.result());
                                            if (buddySeek.outcome()
                                                    == DeployFormationSitingPolicy.BuddySeekOutcome.NON_BATTLEGROUND_SKIP) {
                                                LOG.info("V67ab BUDDY-SEEK SKIP: {} non-BG, {} doesn't need protection here",
                                                    locTitle, soloAlly.getTitle());
                                            } else if (buddySeek.outcome()
                                                    == DeployFormationSitingPolicy.BuddySeekOutcome.PROTECT) {
                                                LOG.warn("V29 BUDDY-SEEK: {} deploying to protect vulnerable {} at {}!",
                                                    card.getTitle(), soloAlly.getTitle(), locTitle);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V29 BUDDY-SEEK: Error: {}", e.getMessage());
                        }
                    }

                    // === V29.12: HUNT DOWN — DEPLOY CHARACTERS WITH VADER ===
                    // When playing Hunt Down V, the strategy revolves around Vader battling.
                    // Characters (especially Inquisitors, dark jedi) should deploy at Vader's
                    // location to create overwhelming force. Scattered characters get picked
                    // off individually. Grouping with Vader lets them benefit from his weapons,
                    // I Have You Now, and the Hunt Down battle advantages.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && gameState != null) {
                        try {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer huntDeployAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (huntDeployAnalyzer != null && huntDeployAnalyzer.isAnalyzed()
                                && huntDeployAnalyzer.isHuntDownV()) {
                                // Find Vader's location on the table
                                PhysicalCard vaderLoc = null;
                                String vaderLocTitle = null;
                                for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                    if (tableCard == null || !playerId.equals(tableCard.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone vz = tableCard.getZone();
                                    if (vz == null || !vz.isInPlay()) continue;
                                    if (tableCard.getBlueprint() == null
                                        || tableCard.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    if (isVader(
                                            game, gameState,
                                            tableCard)) {
                                        vaderLoc = tableCard.getAtLocation();
                                        if (vaderLoc != null && vaderLoc.getTitle() != null) {
                                            vaderLocTitle = vaderLoc.getTitle().toLowerCase(Locale.ROOT);
                                        }
                                        break;
                                    }
                                }

                                if (vaderLocTitle != null) {
                                    // Check if this deploy action targets Vader's location
                                    String actionTextLower = actionText.toLowerCase(Locale.ROOT);
                                    boolean deploysToVaderLoc = actionTextLower.contains(vaderLocTitle);

                                    // Also check: is card being deployed NOT Vader himself?
                                    boolean isNotVader =
                                        !isVader(game, gameState, card);

                                    float oppAtVaderLoc = 0;
                                    boolean isObjRelevant = false;
                                    if (deploysToVaderLoc && isNotVader) {
                                        try {
                                            String v351Oid = game.getOpponent(playerId);
                                            oppAtVaderLoc = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                gameState, vaderLoc, v351Oid, false, false);
                                        } catch (Exception e) { /* ignore */ }
                                    } else if (isNotVader && !deploysToVaderLoc) {
                                        if (huntDeployAnalyzer.isFlipped()) {
                                            try {
                                                for (PhysicalCard loc : gameState.getTopLocations()) {
                                                    if (loc == null || loc.getTitle() == null) continue;
                                                    String locLower = loc.getTitle().toLowerCase(Locale.ROOT);
                                                    if (actionTextLower.contains(locLower)
                                                        && huntDeployAnalyzer.isFlipBackProtectionLocation(loc.getTitle())) {
                                                        isObjRelevant = true;
                                                        break;
                                                    }
                                                }
                                            } catch (Exception e) { /* ignore */ }
                                        }
                                    }

                                    DeployFormationSitingPolicy.HuntGroupingEvaluation huntGrouping =
                                        DeployFormationSitingPolicy.evaluateHuntGrouping(
                                            new DeployFormationSitingPolicy.HuntGroupingFacts(
                                                actionId, true, card.getTitle(), powerVal,
                                                vaderLoc.getTitle(), deploysToVaderLoc,
                                                !isNotVader, oppAtVaderLoc, isObjRelevant));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-hunt-grouping", huntGrouping.result());
                                    switch (huntGrouping.outcome()) {
                                        case GROUP_AND_ENGAGE:
                                            float groupBonus = huntGrouping.result().operations().get(0).delta();
                                            LOG.warn("V35.1 HUNT GROUP+ENGAGE: {} with Vader at {} — opponents power={} (+{})",
                                                card.getTitle(), vaderLoc.getTitle(), (int)oppAtVaderLoc,
                                                (int)groupBonus);
                                            break;
                                        case GROUP_EMPTY:
                                            LOG.warn("V35.1 HUNT GROUP EMPTY: {} with Vader at {} — no opponents (only +50)",
                                                card.getTitle(), vaderLoc.getTitle());
                                            break;
                                        case SCATTER_NEUTRAL:
                                            LOG.warn("V40 HUNT SCATTER: {} NOT at Vader's location ({}) — neutral (was -600)",
                                                card.getTitle(), vaderLoc.getTitle());
                                            break;
                                        case NONE:
                                            break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V29.12 HUNT DOWN GROUP: Error: {}", e.getMessage());
                        }
                    }

                    // === V51: CONTEST OPPONENT DRAIN LOCATIONS — DRAIN 2+ IS AN EMERGENCY ===
                    // Opponent drains are the #1 damage source. Drain 2+ sites are THE decisive
                    // battleground — both players will stack there, whoever wins that fight wins the game.
                    // Deploy aggressively to contest: flood the location with multiple characters.
                    // V51: Massively increased bonuses for drain 2+ sites. Every character sent
                    // to contest a high-drain site gets a large bonus, not just the first one.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && gameState != null) {
                        try {
                            String v36Pid = context.getPlayerId();
                            String v36Oid = game.getOpponent(v36Pid);
                            String v36ActionLower = actionText.toLowerCase(Locale.ROOT);

                            // Find which location this deploy targets
                            for (PhysicalCard v36Loc : gameState.getTopLocations()) {
                                if (v36Loc == null || v36Loc.getTitle() == null) continue;
                                String v36LocLower = v36Loc.getTitle().toLowerCase(Locale.ROOT);
                                if (v36LocLower.isEmpty() || !v36ActionLower.contains(v36LocLower)) continue;

                                float v36OppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, v36Loc, v36Oid, false, false);
                                float v36OurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, v36Loc, v36Pid, false, false);

                                // V53: Check for OUR undercover spies at this location.
                                // Spies aren't counted as "present" by the game engine, so our power
                                // reads as 0 even when we have a spy there. Count spy power as
                                // POTENTIAL power — we could flip the spy to our side.
                                float v53SpyPower = 0;
                                try {
                                    for (PhysicalCard v53c : gameState.getCardsAtLocation(v36Loc)) {
                                        if (v53c != null && v36Pid.equals(v53c.getOwner()) && v53c.isUndercover()) {
                                            float spPow = 0;
                                            if (v53c.getBlueprint() != null && v53c.getBlueprint().hasPowerAttribute()) {
                                                Float sp = v53c.getBlueprint().getPower();
                                                spPow = (sp != null ? sp : 0);
                                            }
                                            v53SpyPower += spPow;
                                            LOG.info("V53 SPY ASSET: Our spy {} (power {}) at {} — counting as potential power",
                                                v53c.getTitle(), (int)spPow, v36Loc.getTitle());
                                        }
                                    }
                                } catch (Exception e) {
                                    v53SpyPower = 0;
                                }

                                float drainAmount = 1.0f;
                                if (v36OppPower > 0) {
                                    // Opponent has presence — check drain amount
                                    try {
                                        drainAmount = game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, v36Loc, v36Oid);
                                    } catch (Exception e) { /* default 1 */ }

                                }

                                DeployTacticalPolicy.DrainContestEvaluation drainContest =
                                    DeployTacticalPolicy.evaluateV53V51Drain(
                                        new DeployTacticalPolicy.DrainContestFacts(
                                            actionId, v36Oid, v36Loc.getTitle(),
                                            v36OppPower, v36OurPower, v53SpyPower,
                                            drainAmount));
                                applySharedPolicy(action, decisionId, actionId,
                                    "deploy-v53-v51-drain", drainContest.result());
                                for (DeployTacticalPolicy.DrainContestOutcome outcome
                                        : drainContest.outcomes()) {
                                    switch (outcome) {
                                        case SPY_ALLY:
                                            LOG.warn("V53 SPY ALLY: Spy power {} at {} — +200 deploy bonus",
                                                (int)v53SpyPower, v36Loc.getTitle());
                                            break;
                                        case DRAIN_EMERGENCY:
                                            LOG.warn("V51 DRAIN EMERGENCY: {} to {} — opponent drains {} uncontested (+600)",
                                                card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                            break;
                                        case DRAIN_REINFORCE:
                                            LOG.warn("V51 DRAIN REINFORCE: {} to {} — opponent drains {} we have presence (+500)",
                                                card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                            break;
                                        case CONTEST_BATTLEGROUND:
                                            LOG.warn("V51 CONTEST BATTLEGROUND: {} to {} — opponent drains {} uncontested (+500)",
                                                card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                            break;
                                        case REINFORCE_BATTLEGROUND:
                                            LOG.warn("V51 REINFORCE BATTLEGROUND: {} to {} — opponent drains {} we have presence (+500)",
                                                card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                            break;
                                        case CONTEST_DRAIN:
                                            float contestDrainBonus = 200.0f + (drainAmount * 100.0f);
                                            LOG.warn("V36 CONTEST DRAIN: {} to {} — opponent drains {} uncontested (+{})",
                                                card.getTitle(), v36Loc.getTitle(), (int)drainAmount, (int)contestDrainBonus);
                                            break;
                                    }
                                }
                                break; // Found target location
                            }
                        } catch (Exception e) {
                            LOG.debug("V51 CONTEST DRAIN: Error: {}", e.getMessage());
                        }
                    }

                    // === V51: VADER AGGRESSIVE FLIP ===
                    // The bonus applies only when the exact deployment satisfies every
                    // source-backed live flip condition.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER
                        && isVader(game, gameState, card)
                        && !actionLower.contains("bounty") && !actionLower.contains("lightsaber")
                        && gameState != null && game != null) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer vaderFlipAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (vaderFlipAnalyzer != null && vaderFlipAnalyzer.isAnalyzed()
                            && vaderFlipAnalyzer.isHuntDownV() && !vaderFlipAnalyzer.isFlipped()) {
                            String vfPid = context.getPlayerId();
                            PhysicalCard vfTarget = null;
                            for (PhysicalCard vfLoc : gameState.getTopLocations()) {
                                if (vfLoc == null || vfLoc.getTitle() == null) continue;
                                String vfLocLower = vfLoc.getTitle().toLowerCase(Locale.ROOT);
                                if (!actionLower.contains(vfLocLower)) continue;
                                if (vfTarget == null
                                        || vfLoc.getTitle().length()
                                            > vfTarget.getTitle().length()) {
                                    vfTarget = vfLoc;
                                }
                            }
                            if (vfTarget != null) {
                                try {
                                    boolean completesObjective =
                                        vaderFlipAnalyzer
                                            .wouldCompletePreFlipRequirementAt(
                                                game, vfPid, card, vfTarget);
                                    PolicyResult vaderFlip = DeployTacticalPolicy.scoreV51VaderFlip(
                                        new DeployTacticalPolicy.VaderFlipFacts(
                                            actionId, vfTarget.getTitle(),
                                            completesObjective));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-vader-flip", vaderFlip);
                                    if (!vaderFlip.operations().isEmpty()) {
                                        LOG.warn("V51 VADER FLIP: Vader to {}, all live Hunt Down conditions met (+900)", vfTarget.getTitle());
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }
                        }
                    }

                    // === V29.13: DEPLOY DIRECTLY TO OPPONENTS — AVOID DEPLOY-THEN-MOVE WASTE ===
                    // Rando was deploying characters to empty locations then wasting Force to
                    // move them to where opponents are. Much better to deploy directly to the
                    // location with opponents and save the move Force cost.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && gameState != null) {
                        try {
                            String opponentIdDeploy = game.getOpponent(playerId);
                            String actionTextLowerDeploy = actionText.toLowerCase(Locale.ROOT);

                            // Check each location on the table for opponent presence
                            for (PhysicalCard locCard : gameState.getAllPermanentCards()) {
                                if (locCard == null || locCard.getBlueprint() == null) continue;
                                if (locCard.getBlueprint().getCardCategory() != CardCategory.LOCATION) continue;
                                com.gempukku.swccgo.common.Zone locZone = locCard.getZone();
                                if (locZone == null || !locZone.isInPlay()) continue;
                                String locTitleDeploy = locCard.getTitle() != null
                                    ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                if (locTitleDeploy.isEmpty()) continue;

                                // Does this deploy action target this location?
                                if (!actionTextLowerDeploy.contains(locTitleDeploy)) continue;

                                // Check opponent power at this location
                                float oppPowerHere = 0;
                                try {
                                    oppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, locCard, opponentIdDeploy, false, false);
                                } catch (Exception e) { /* ignore */ }

                                if (oppPowerHere > 0) {
                                    // V49: Check our existing power + deploying card's power vs opponent
                                    float ourPowerHere = 0;
                                    try {
                                        ourPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, locCard, playerId, false, false);
                                    } catch (Exception e) { /* ignore */ }
                                    float deployingPower = card.getBlueprint() != null ? card.getBlueprint().getPower() : 0;
                                    float totalOurPowerAfterDeploy = ourPowerHere + deployingPower;

                                    // V50: Deploy power-disadvantage penalty — turns 1-3 only, even-power threshold.
                                    // After turn 3, deploy everywhere no matter what — can't afford to sit idle.
                                    // Threshold: only penalize if we'd be at LESS than even power (was oppPowerHere - 3).
                                    int v50Turn = context.getTurnNumber();
                                    DeployTacticalPolicy.PowerDangerEvaluation powerDanger =
                                        DeployTacticalPolicy.evaluateV50PowerDanger(
                                            new DeployTacticalPolicy.PowerDangerFacts(
                                                actionId, v50Turn, card.getTitle(),
                                                locCard.getTitle(), totalOurPowerAfterDeploy,
                                                oppPowerHere));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-v50-power-danger", powerDanger.result());
                                    if (powerDanger.outcome()
                                            == DeployTacticalPolicy.PowerDangerOutcome.EARLY_DANGER) {
                                        LOG.warn("V50 DEPLOY DANGER T{}: {} to {} — our power {}, opponent power {} — PENALIZED (turns 1-3 only)",
                                            v50Turn, card.getTitle(), locCard.getTitle(), (int)totalOurPowerAfterDeploy, (int)oppPowerHere);
                                        continue;
                                    }
                                    if (powerDanger.outcome()
                                            == DeployTacticalPolicy.PowerDangerOutcome.LATE_DEPLOY) {
                                        LOG.warn("V50 LATE DEPLOY T{}: {} to {} — our power {}, opponent power {} — deploying anyway (past turn 3)",
                                            v50Turn, card.getTitle(), locCard.getTitle(), (int)totalOurPowerAfterDeploy, (int)oppPowerHere);
                                    }

                                    // V35: Check for Jedi at this location — Vader/Inquisitor bonuses
                                    com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                                        v35Objective =
                                            context.getObjectiveAnalyzer();
                                    boolean v35UsesObjectiveBlocker =
                                        v35Objective != null
                                        && v35Objective.isAnalyzed()
                                        && v35Objective.isHuntDownV()
                                        && !v35Objective.isFlipped();
                                    boolean v35JediHere =
                                        v35UsesObjectiveBlocker
                                        && v35Objective
                                            .isPreFlipGlobalBlockerAt(
                                                game, playerId,
                                                locCard);
                                    boolean v35HatredHere = false;
                                    try {
                                        for (PhysicalCard lc : gameState.getCardsAtLocation(locCard)) {
                                            if (lc == null) continue;
                                            String lcTitle = lc.getTitle() != null ? lc.getTitle().toLowerCase(Locale.ROOT) : "";
                                            if (opponentIdDeploy.equals(lc.getOwner())) {
                                                if (!v35UsesObjectiveBlocker
                                                        && isJediOrPadawan(
                                                            lcTitle)) {
                                                    v35JediHere = true;
                                                }
                                                java.util.List<PhysicalCard> stacked = gameState.getStackedCards(lc);
                                                if (stacked != null && !stacked.isEmpty()) v35HatredHere = true;
                                            }
                                        }
                                    } catch (Exception e) { /* ignore */ }

                                    String deployCardLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                                    boolean deploysVader =
                                        isVader(game, gameState, card);
                                    if (v35JediHere && deploysVader) {
                                        // V35.8: Raised from +350 to +600 — killing Jedi is THE objective
                                        // of Hunt Down. Opponent loses extra Force when Jedi dies.
                                        LOG.warn("V35.8 HUNT JEDI DEPLOY: Vader to {} with JEDI! (+600)",
                                            locCard.getTitle());
                                    }
                                    if (v35JediHere && isInquisitor(deployCardLower)) {
                                        LOG.warn("V35 INQUISITOR vs JEDI: {} to {} (+250)", card.getTitle(), locCard.getTitle());
                                    }
                                    if (v35HatredHere && isInquisitor(deployCardLower)) {
                                        LOG.warn("V35 INQUISITOR+HATRED: {} to {} with hatred (+{})",
                                            card.getTitle(), locCard.getTitle(), RandoConfig.SCORE_INQUISITOR_HATRED_SYNERGY);
                                    }

                                    PolicyResult directEngage =
                                        DeployTacticalPolicy.scoreV34DirectEngage(
                                            new DeployTacticalPolicy.DirectEngageFacts(
                                                actionId, card.getTitle(), locCard.getTitle(),
                                                oppPowerHere, v35JediHere, v35HatredHere,
                                                deploysVader,
                                                isInquisitor(deployCardLower),
                                                (float) RandoConfig.SCORE_INQUISITOR_HATRED_SYNERGY));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-v34-direct-engage", directEngage);
                                    float engageBonus = directEngage.operations().get(0).delta();
                                    LOG.warn("V34 DIRECT ENGAGE: {} to {} — opponents power={} (+{})",
                                        card.getTitle(), locCard.getTitle(), (int)oppPowerHere, (int)engageBonus);
                                } else {
                                    // No opponents here — deploying here would likely require a move
                                    // Only penalize if there ARE opponents elsewhere
                                    boolean opponentsElsewhere = false;
                                    for (PhysicalCard otherLoc : gameState.getAllPermanentCards()) {
                                        if (otherLoc == null || otherLoc.getBlueprint() == null) continue;
                                        if (otherLoc.getBlueprint().getCardCategory() != CardCategory.LOCATION) continue;
                                        if (otherLoc == locCard) continue;
                                        com.gempukku.swccgo.common.Zone oz = otherLoc.getZone();
                                        if (oz == null || !oz.isInPlay()) continue;
                                        try {
                                            float otherOppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                gameState, otherLoc, opponentIdDeploy, false, false);
                                            if (otherOppPower > 0) {
                                                opponentsElsewhere = true;
                                                break;
                                            }
                                        } catch (Exception e) { /* ignore */ }
                                    }
                                    if (opponentsElsewhere) {
                                        // V36: SMART EMPTY DEPLOY — penalty depends on context.
                                        // If we have enough Force AND characters to challenge opponents,
                                        // heavy penalty for empty site. But if we CAN'T challenge
                                        // (low Force, no characters in hand to pair up), deploying to
                                        // an empty drain site is acceptable for force economy.
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer emptyDeployAnalyzer =
                                            context.getObjectiveAnalyzer();
                                        boolean isHuntDown = emptyDeployAnalyzer != null
                                            && emptyDeployAnalyzer.isAnalyzed() && emptyDeployAnalyzer.isHuntDownV();

                                        // Check if this empty site has force drain icons (useful for our drains)
                                        boolean hasDrainValue = false;
                                        try {
                                            com.gempukku.swccgo.common.Side mySide36 = context.getSide();
                                            com.gempukku.swccgo.game.SwccgCardBlueprint locBp = locCard.getBlueprint();
                                            if (locBp != null) {
                                                int myIcons = (mySide36 == com.gempukku.swccgo.common.Side.DARK)
                                                    ? locBp.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                                    : locBp.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                                if (myIcons > 0) hasDrainValue = true;
                                            }
                                        } catch (Exception e) { /* ignore */ }

                                        // Count characters in hand that could deploy to opponent locations
                                        int charsInHand = 0;
                                        try {
                                            java.util.List<PhysicalCard> v36Hand = gameState.getHand(playerId);
                                            if (v36Hand != null) {
                                                for (PhysicalCard hc : v36Hand) {
                                                    if (hc != null && hc.getBlueprint() != null
                                                        && hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                        charsInHand++;
                                                    }
                                                }
                                            }
                                        } catch (Exception e) { /* ignore */ }

                                        // V40: Empty site penalties neutralized — deploy freely
                                        float emptyPenalty = 0.0f;

                                        applySharedPolicy(action, decisionId, actionId,
                                            "deploy-v36-empty",
                                            DeployTacticalPolicy.scoreV36EmptyDeploy(
                                                new DeployTacticalPolicy.EmptyDeployFacts(
                                                    actionId, card.getTitle(), locCard.getTitle(),
                                                    hasDrainValue)));
                                        LOG.warn("V36 EMPTY DEPLOY: {} to {} (hunt={}, charsInHand={}, drainIcons={}, penalty={})",
                                            card.getTitle(), locCard.getTitle(), isHuntDown, charsInHand, hasDrainValue, (int)emptyPenalty);
                                    }
                                }
                                break; // Found the target location
                            }
                        } catch (Exception e) {
                            LOG.debug("V29.13 DIRECT ENGAGE: Error: {}", e.getMessage());
                        }
                    }

                    PolicyContributionLedger cardValueLedger =
                            new PolicyContributionLedger(
                                    (decisionId == null || decisionId.isBlank()
                                            ? "deploy-card-value"
                                            : decisionId + "-deploy-card-value")
                                            + "-" + actionId);
                    cardValueLedger.register(DeployCardValuePolicy.scoreBase(
                            new DeployCardValueFacts.BaseValue(
                                    actionId, powerVal, abilityVal, cost,
                                    destinyVal)));
                    PolicyOperationAdapter.apply(action, cardValueLedger);

                    CardCategory category = blueprint.getCardCategory();
                    String cardTitleLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                    boolean eliteCharacter = category == CardCategory.CHARACTER
                            && gameState != null && game != null
                            && (isVader(game, gameState, card)
                            || cardTitleLower.contains("emperor")
                            || cardTitleLower.contains("palpatine"));
                    // === V40: POSITIVE DEPLOY BONUSES ===
                    // Reward good deploys instead of penalizing questionable ones.
                    if (category == CardCategory.CHARACTER && gameState != null && game != null) {
                        try {
                            String v40Pid = context.getPlayerId();
                            String v40Oid = gameState.getOpponent(v40Pid);
                            String v40ActionLower = actionText.toLowerCase(Locale.ROOT);

                            PolicyContributionLedger eliteValueLedger =
                                    new PolicyContributionLedger(
                                            (decisionId == null || decisionId.isBlank()
                                                    ? "deploy-elite-value"
                                                    : decisionId + "-deploy-elite-value")
                                                    + "-" + actionId);
                            eliteValueLedger.register(DeployCardValuePolicy.scoreElite(
                                    new DeployCardValueFacts.EliteValue(
                                            actionId, eliteCharacter)));
                            PolicyOperationAdapter.apply(action, eliteValueLedger);
                            if (eliteCharacter) {
                                LOG.warn("V40 ELITE: {} gets +100 deploy bonus", card.getTitle());
                            }

                            // --- Check target location for bonuses ---
                            for (PhysicalCard v40Loc : gameState.getTopLocations()) {
                                if (v40Loc == null || v40Loc.getTitle() == null) continue;
                                String v40LocLower = v40Loc.getTitle().toLowerCase(Locale.ROOT);
                                if (!v40ActionLower.contains(v40LocLower)) continue;

                                // Deploy to site with 2+ opponent force icons: +200
                                try {
                                    com.gempukku.swccgo.game.SwccgCardBlueprint v40LocBp = v40Loc.getBlueprint();
                                    if (v40LocBp != null) {
                                        com.gempukku.swccgo.common.Side oppSide40 = (context.getSide() == com.gempukku.swccgo.common.Side.DARK)
                                            ? com.gempukku.swccgo.common.Side.LIGHT : com.gempukku.swccgo.common.Side.DARK;
                                        int oppIcons = (oppSide40 == com.gempukku.swccgo.common.Side.DARK)
                                            ? v40LocBp.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                            : v40LocBp.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                        PolicyResult highDrainSite =
                                            DeployFormationSitingPolicy.scoreHighDrainSite(
                                                new DeployFormationSitingPolicy.HighDrainSiteFacts(
                                                    actionId, v40Loc.getTitle(), oppIcons));
                                        applySharedPolicy(action, decisionId, actionId,
                                            "deploy-high-drain-site", highDrainSite);
                                        if (!highDrainSite.operations().isEmpty()) {
                                            LOG.warn("V40 HIGH DRAIN: {} to {} ({} opp icons) — +200", card.getTitle(), v40Loc.getTitle(), oppIcons);
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                // Deploy to site where game text does NOT mention drain reduction: +100
                                try {
                                    com.gempukku.swccgo.game.SwccgCardBlueprint v40LocBp2 = v40Loc.getBlueprint();
                                    if (v40LocBp2 != null) {
                                        String gameText = v40LocBp2.getGameText();
                                        if (gameText != null) {
                                            String gtLower = gameText.toLowerCase(Locale.ROOT);
                                            boolean hasDrainReduction = gtLower.contains("-1 force drain") || gtLower.contains("reduce")
                                                || gtLower.contains("force drain -1") || gtLower.contains("drain here is -");
                                            PolicyResult goodDrainSite =
                                                DeployFormationSitingPolicy.scoreGoodDrainSite(
                                                    new DeployFormationSitingPolicy.GoodDrainSiteFacts(
                                                        actionId, v40Loc.getTitle(), true,
                                                        hasDrainReduction));
                                            applySharedPolicy(action, decisionId, actionId,
                                                "deploy-good-drain-site", goodDrainSite);
                                            if (!goodDrainSite.operations().isEmpty()) {
                                                LOG.warn("V40 GOOD DRAIN SITE: {} to {} — no drain reduction — +100",
                                                    card.getTitle(), v40Loc.getTitle());
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                // === V51: DRAIN 2+ SITE STACKING + BUDDY SYSTEM ===
                                // Drain 2+ sites are THE battleground. Stack characters there.
                                // Buddy system: ability >= 4 enables battle destiny, ability >= 7 is ideal.
                                try {
                                    int v40FriendlyCount = 0;
                                    float v40FriendlyAbility = 0;
                                    for (PhysicalCard v40c : gameState.getCardsAtLocation(v40Loc)) {
                                        if (v40c == null || !v40Pid.equals(v40c.getOwner())) continue;
                                        if (v40c.getBlueprint() == null || v40c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        v40FriendlyCount++;
                                        if (v40c.getBlueprint().hasAbilityAttribute()) {
                                            Float v40ab = v40c.getBlueprint().getAbility();
                                            v40FriendlyAbility += (v40ab != null ? v40ab : 0);
                                        }
                                    }

                                    // Check if this is a drain 2+ site (our drain potential)
                                    float v51OurDrain = 0;
                                    try {
                                        v51OurDrain = game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, v40Loc, v40Pid);
                                    } catch (Exception e) { /* default 0 */ }

                                    float v40CardAbility = 0;
                                    if (card.getBlueprint().hasAbilityAttribute()) {
                                        Float v40ab2 = card.getBlueprint().getAbility();
                                        v40CardAbility = (v40ab2 != null ? v40ab2 : 0);
                                    }
                                    float totalAbilityAfter = v40FriendlyAbility + v40CardAbility;

                                    DeployFormationSitingPolicy.PositiveFormationEvaluation positiveFormation =
                                        DeployFormationSitingPolicy.evaluatePositiveFormation(
                                            new DeployFormationSitingPolicy.PositiveFormationFacts(
                                                actionId, card.getTitle(), v40Loc.getTitle(),
                                                v40FriendlyCount, v40FriendlyAbility,
                                                v40CardAbility, v51OurDrain));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-positive-formation", positiveFormation.result());
                                    for (DeployFormationSitingPolicy.PositiveFormationOutcome outcome
                                            : positiveFormation.outcomes()) {
                                        switch (outcome) {
                                            case FORTIFY_BATTLEGROUND:
                                                LOG.warn("V51 FORTIFY BATTLEGROUND: {} joins {} friendlies at {} (drain {}) — +500",
                                                    card.getTitle(), v40FriendlyCount, v40Loc.getTitle(), (int)v51OurDrain);
                                                break;
                                            case ESTABLISH_BATTLEGROUND:
                                                LOG.warn("V51 ESTABLISH BATTLEGROUND: {} first to {} (drain {}) — +400",
                                                    card.getTitle(), v40Loc.getTitle(), (int)v51OurDrain);
                                                break;
                                            case REINFORCE:
                                                LOG.warn("V51 REINFORCE: {} joins {} friendlies at {} — +300",
                                                    card.getTitle(), v40FriendlyCount, v40Loc.getTitle());
                                                break;
                                            case BUDDY_DESTINY:
                                                LOG.warn("V51 BUDDY DESTINY: {} enables battle destiny at {} (ability {} → {}) — +400",
                                                    card.getTitle(), v40Loc.getTitle(), (int)v40FriendlyAbility,
                                                    (int)totalAbilityAfter);
                                                break;
                                            case BUDDY_FULL:
                                                LOG.warn("V51 BUDDY FULL: {} — total ability {} at {} — +500",
                                                    card.getTitle(), (int)totalAbilityAfter, v40Loc.getTitle());
                                                break;
                                            case BUDDY_REINFORCE:
                                                LOG.warn("V51 BUDDY REINFORCE: {} ability {} → {} at {} — +200",
                                                    card.getTitle(), (int)v40FriendlyAbility, (int)totalAbilityAfter,
                                                    v40Loc.getTitle());
                                                break;
                                            case ARMED:
                                                LOG.warn("V51 ARMED: {} to {} — weapon bonus +150",
                                                    card.getTitle(), v40Loc.getTitle());
                                                break;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                break; // Only check first matching location
                            }
                        } catch (Exception e) {
                            LOG.debug("V40 POSITIVE BONUSES: Error: {}", e.getMessage());
                        }
                    }

                    // === V24: MEGA LOCATION PRIORITY ===
                    // Locations are the foundation of EVERYTHING — force generation, deploy targets, drain sites.
                    // In the first 3 turns, deploying locations should dominate all other actions.
                    // V60 FIX: Only apply when the ACTION is actually deploying the location
                    // (source is a LOCATION card and actionText is a bare "Deploy" / "Deploy [location]"
                    // — NOT when the action invokes a location's game-text to pull a character
                    // like "Deploy a Padawan" or "Deploy Tala Durith from Reserve Deck".
                    // FIXES Issue #A from peaceful-pike replay: Rando invoked Malachor STE's
                    // "Deploy a Padawan" at force=0 because V24 thought it was a location deploy.
                    String v24ActionLower = actionText != null ? actionText.toLowerCase(Locale.ROOT) : "";
                    // 2026-06-03 STRICT BARE-DEPLOY GATE (Steve, Mustafar Docking Bay replay):
                    // GEMP sends BARE "Deploy" for legit location-from-hand deploys (the
                    // cardId resolves to the location — see "V29 EARLY LOOKUP: Resolved bare
                    // 'Deploy' via cardId NNN"). Every "Deploy <something>" variant is a
                    // game-text pull on the location: "Deploy a Padawan", "Deploy an alien
                    // from Reserve Deck", "Deploy starfighter with 'Vader' in title here",
                    // etc. The earlier denylist (padawan/jedi-survivor/tala-durith/from-reserve)
                    // missed "starfighter", "alien", and every other pull keyword — Rando
                    // shipped 0-power TIE Advanced into Mustafar Docking Bay because V67ai
                    // misread the starfighter pull as a location deploy and slapped +1400 on
                    // it. Dropping the startsWith branch fixes the whole class: ONLY bare
                    // "deploy" counts as a location deploy; ALL "deploy X" variants fall
                    // through to the existing "V60 V24 SKIP" else branch (which Rando's V67i
                    // / V60 / V67bg logic handles correctly for character/ship/alien pulls).
                    boolean isActualLocationDeploy = category == CardCategory.LOCATION
                        && v24ActionLower.equals("deploy");
                    if (isActualLocationDeploy) {
                        int locationLifeForce = 0;
                        try {
                            String locationPlayerId = context.getPlayerId();
                            if (gameState != null && locationPlayerId != null) {
                                locationLifeForce = gameState.getReserveDeckSize(locationPlayerId)
                                    + gameState.getForcePileSize(locationPlayerId)
                                    + gameState.getUsedPile(locationPlayerId).size();
                            }
                        } catch (Exception ignored) {
                            locationLifeForce = 99;
                        }
                        DeploySequencingPolicy.Evaluation locationOrder =
                            DeploySequencingPolicy.locationFromHand(
                                actionId, true, locationLifeForce, card.getTitle());
                        PolicyContributionLedger locationOrderLedger = new PolicyContributionLedger(
                            (decisionId == null || decisionId.isBlank()
                                ? "deploy-location-order"
                                : decisionId + "-deploy-location-order") + "-" + actionId);
                        locationOrderLedger.register(locationOrder.result());
                        PolicyOperationAdapter.apply(action, locationOrderLedger);
                    } else if (category == CardCategory.LOCATION) {
                        // Source is a location but action is a game-text pull — don't give +200
                        LOG.info("V60 V24 SKIP: '{}' on {} is a game-text pull, not a location deploy — no +200 bonus",
                            actionText, card.getTitle());
                    }

                    // === V67i GLOBAL LOCATION-FIRST PRIORITY ===
                    // Steve's rule: "this should be global. Deploy locations first so he has
                    // more options for deploying characters. He needs to deploy locations
                    // first then characters every turn. Especially if he has an effect that
                    // lets him pull locations."
                    //
                    // V24 only fired for "Deploy <Location>" from hand. But many decks
                    // pull/download locations via effects:
                    //   IMBATS [download] a farm
                    //   Yarna [download] a Tatooine battleground
                    //   I'm Sorry → Cloud City interior site
                    //   Hidden Path → Jabiim site
                    // These should ALL beat character deploys, because EACH new location
                    // expands future deploy options + force generation.
                    //
                    // Detection: parse the source card's game text for location keywords
                    // in the [download]/deploy/take target list. If any extracted target
                    // names a location category, this action puts a location on the table.
                    boolean v67iAddsLocation = false;
                    String v67iReason = null;
                    try {
                        // Direct text-level check: action text mentions location keyword
                        String v67iLower = v24ActionLower;
                        if (v67iLower.contains("from reserve deck") || v67iLower.contains("[download]")) {
                            // The target keywords from action text or source-card game text
                            String[] v67iLocationKeywords = new String[] {
                                "site", "battleground", "location", "system", "farm",
                                "cantina", "mos eisley", "tatooine", "endor", "hoth",
                                "dagobah", "naboo", "yavin", "bespin", "cloud city",
                                "mustafar", "malachor", "mapuzo", "jabiim", "coruscant",
                                "kashyyyk", "kessel", "kamino", "geonosis", "alderaan",
                                "docking bay", "spaceport", "city", "palace", "temple",
                                "safehouse", "corridor", "village", "outpost"
                            };
                            for (String kw : v67iLocationKeywords) {
                                if (v67iLower.contains(kw)) {
                                    v67iAddsLocation = true;
                                    v67iReason = "actionText contains location keyword '" + kw + "'";
                                    break;
                                }
                            }
                            // If actionText is generic (no keyword), fall back to game text
                            if (!v67iAddsLocation) {
                                List<String> v67iCardIds2 = context.getCardIds();
                                String v67iCardIdStr = (v67iCardIds2 != null && i < v67iCardIds2.size())
                                    ? v67iCardIds2.get(i) : null;
                                if (v67iCardIdStr != null && !v67iCardIdStr.isEmpty() && gameState != null) {
                                    PhysicalCard v67iSrc =
                                        gameState.findCardById(Integer.parseInt(v67iCardIdStr));
                                    if (v67iSrc != null && v67iSrc.getBlueprint() != null) {
                                        String gt = com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                            .getSourceCardFullGameText(v67iSrc.getBlueprint(), context.getSide());
                                        if (gt != null) {
                                            List<String> targets = com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                                .parseSourceCardPullTargets(gt);
                                            for (String t : targets) {
                                                for (String kw : v67iLocationKeywords) {
                                                    if (t.contains(kw)) {
                                                        v67iAddsLocation = true;
                                                        v67iReason = "source card '" + v67iSrc.getTitle()
                                                            + "' game text targets location-like '" + t + "'";
                                                        break;
                                                    }
                                                }
                                                if (v67iAddsLocation) break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { LOG.debug("V67i error: {}", e.getMessage()); }

                    // V67ai Tier 1-3 DE scorer was removed 2026-07-13. V192 in
                    // ActionTextEvaluator is the sole live owner, with V131 gating and
                    // one resized tier emit. Git preserves the superseded
                    // +2000/+1800/+1600/+1500 DeployEvaluator copy.
                    // The V67i detection above is KEPT LIVE as a predicate — the weapon-gate
                    // routing below (V67ar/V67ao/V149 need !v67iAddsLocation) still uses it.
                    // The V162/V67ai-Tier4-HAND block above (bare "deploy" of a LOCATION from
                    // hand, +1900 total) is NOT touched — it is the anchor the pull scorer
                    // must stay below (the V179 lesson).
                    if (v67iAddsLocation) {
                        LOG.info("V67i location-pull detected for '{}' ({}) — tier owned by V192 (ActionTextEvaluator)",
                            actionText, v67iReason);
                    }

                    // === V67m UNIVERSAL WEAPON-PULL PRIORITY ===
                    // Steve's rule: "There are other cards that pull weapons from reserve,
                    // after location pulls and character deploys, we should use those
                    // effects to deploy weapons from reserve with positive points."
                    //
                    // Score +200 — positive enough to fire over passing/idle, but well
                    // below character deploy peaks (+300-500) so chars deploy first.
                    // Mirrors V67l's dual-source detection (action text + game text fallback).
                    boolean v67mAddsWeapon = false;
                    String v67mReason = null;
                    try {
                        String v67mLower = v24ActionLower;
                        if (v67mLower.contains("from reserve deck") || v67mLower.contains("[download]")) {
                            String[] v67mWeaponKeywords = new String[] {
                                "weapon", "lightsaber", "saber", "blaster",
                                "rifle", "pistol", "cannon", "bowcaster",
                                "thermal detonator", "vibroblade", "vibro-",
                                "force pike", "electrostaff"
                            };
                            for (String kw : v67mWeaponKeywords) {
                                if (v67mLower.contains(kw)) {
                                    v67mAddsWeapon = true;
                                    v67mReason = "actionText contains weapon keyword '" + kw + "'";
                                    break;
                                }
                            }
                            // Fallback: source card game text
                            if (!v67mAddsWeapon) {
                                List<String> v67mCardIds = context.getCardIds();
                                String v67mCardIdStr = (v67mCardIds != null && i < v67mCardIds.size())
                                    ? v67mCardIds.get(i) : null;
                                if (v67mCardIdStr != null && !v67mCardIdStr.isEmpty() && gameState != null) {
                                    PhysicalCard v67mSrc =
                                        gameState.findCardById(Integer.parseInt(v67mCardIdStr));
                                    if (v67mSrc != null && v67mSrc.getBlueprint() != null) {
                                        String gt = com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                            .getSourceCardFullGameText(v67mSrc.getBlueprint(), context.getSide());
                                        if (gt != null) {
                                            List<String> targets = com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                                .parseSourceCardPullTargets(gt);
                                            for (String t : targets) {
                                                for (String kw : v67mWeaponKeywords) {
                                                    if (t.contains(kw)) {
                                                        v67mAddsWeapon = true;
                                                        v67mReason = "source card '" + v67mSrc.getTitle()
                                                            + "' game text targets weapon-like '" + t + "'";
                                                        break;
                                                    }
                                                }
                                                if (v67mAddsWeapon) break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { LOG.debug("V67m error: {}", e.getMessage()); }

                    // Don't double-bonus location pulls (V67l already gave +1500)
                    if (v67mAddsWeapon && !v67iAddsLocation) {
                        // V67ar (Steve, 2026-05-08): UNIVERSAL ONE-WEAPON RULE — DeployEvaluator path.
                        // Mirrors V67aq's logic. Count UNARMED Rando characters; if zero
                        // unarmed (every char already armed), hard-block. No hardcoded names.
                        // V149 (Steve, 2026-05-28, REVISED): LIGHTSABER PULL NEEDS A
                        // CAPABLE WARRIOR. Evil Is Everywhere pulled an Episode I
                        // lightsaber when only Dr. Evazan (a cantina alien) was on table
                        // — nobody could wield it. V67am counted "unarmed characters"
                        // generically with no wield check.
                        //
                        // Steve's rule: "lightsabers require specific warriors. Warrior
                        // type with ability >= 4." A wielder must have the [Warrior] icon
                        // AND ability >= 4. Jedi/Sith/Dark Jedi carry [Warrior]; cantina
                        // aliens (Dr. Evazan) don't. Global icon+ability check, no
                        // set/persona hardcoding. Replaces the earlier Episode-I icon match.
                        boolean v149IsLightsaberPull = false;
                        if (v67mReason != null) {
                            String v149r = v67mReason.toLowerCase(Locale.ROOT);
                            v149IsLightsaberPull = v149r.contains("lightsaber") || v149r.contains("saber");
                        }

                        int v67arUnarmed = 0;
                        int v67arArmed = 0;
                        int v149AbilityCapableUnarmed = 0;
                        if (gameState != null && context.getPlayerId() != null) {
                            try {
                                for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                    if (pc == null || pc.getBlueprint() == null) continue;
                                    if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone z = pc.getZone();
                                    if (z == null || !z.isInPlay()) continue;
                                    if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    boolean armed = false;
                                    java.util.List<PhysicalCard> atts = gameState.getAttachedCards(pc);
                                    if (atts != null) {
                                        for (PhysicalCard a : atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                armed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (armed) v67arArmed++;
                                    else {
                                        v67arUnarmed++;
                                        // V149: lightsaber wielder = [Warrior] icon AND ability >= 4
                                        if (v149IsLightsaberPull
                                                && pc.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.WARRIOR)
                                                && pc.getBlueprint().hasAbilityAttribute()) {
                                            Float v149ab = pc.getBlueprint().getAbility();
                                            if (v149ab != null && v149ab >= 4f) {
                                                v149AbilityCapableUnarmed++;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }
                        PullActionPolicy.WeaponOrderEvaluation weaponOrder =
                            PullActionPolicy.evaluateWeaponOrder(
                                new PullActionPolicy.WeaponOrderFacts(
                                    actionId, true, false, v67arUnarmed,
                                    v67arArmed, v149IsLightsaberPull,
                                    v149AbilityCapableUnarmed));
                        applySharedPolicy(action, decisionId, actionId,
                            "deploy-weapon-order", weaponOrder.result());
                        switch (weaponOrder.outcome()) {
                            case ALL_ARMED ->
                                LOG.warn("V67ar UNIVERSAL BLOCK (DeployEvaluator pull): '{}' — all {} chars armed",
                                    actionText, v67arArmed);
                            case NO_CHARACTER ->
                                LOG.warn("V67ao ORDER GATE (DeployEvaluator): weapon pull '{}' blocked (no chars on table)",
                                    actionText);
                            case NO_LIGHTSABER_WIELDER ->
                                LOG.warn("V149 NO LIGHTSABER WIELDER (DeployEvaluator): '{}' — 0 unarmed [Warrior] ability-4+ chars → -2000",
                                    actionText);
                            case READY ->
                                LOG.info("V67am weapon pull detected for '{}' — grant owned by V192 (ActionTextEvaluator)",
                                    actionText);
                            case NONE -> { }
                        }
                    }

                    // === V51: CLOUD CITY ARMY PRE-FLIP — Stack characters at CC sites ===
                    // For TDIGWATT/Dark Deal: before objective flips, build your Cloud City army.
                    // +500 for deploying characters to Cloud City sites pre-flip.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && gameState != null) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer ccAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (ccAnalyzer != null && ccAnalyzer.isAnalyzed()
                            && ccAnalyzer.needsBespinSystemPresence() && !ccAnalyzer.isFlipped()) {
                            // TDIGWATT pre-flip — bonus for Cloud City character deploys
                            for (PhysicalCard ccLoc : gameState.getTopLocations()) {
                                if (ccLoc == null || ccLoc.getTitle() == null) continue;
                                String ccLocLower = ccLoc.getTitle().toLowerCase(Locale.ROOT);
                                if (!actionLower.contains(ccLocLower)) continue;
                                if (ccLocLower.contains("cloud city")) {
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-cloud-city-army",
                                        DeployObjectiveSitingPolicy.scoreCloudCityArmy(
                                            new DeployObjectiveSitingPolicy.CloudCityArmyFacts(
                                                actionId, ccLoc.getTitle())));
                                    LOG.warn("V51 CC ARMY: {} to {} pre-flip — +500", card.getTitle(), ccLoc.getTitle());
                                }
                                break;
                            }
                        }
                    }

                    // === V51: OBJECTIVE-FIRST DEPLOYMENT — Bonus for objective locations pre-flip ===
                    // Before objective flips, deploying to objective-relevant locations gets a bonus.
                    // This applies to ALL objective decks.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && gameState != null) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objFirstAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (objFirstAnalyzer != null && objFirstAnalyzer.isAnalyzed()
                            && !objFirstAnalyzer.isFlipped()) {
                            for (PhysicalCard ofLoc : gameState.getTopLocations()) {
                                if (ofLoc == null || ofLoc.getTitle() == null) continue;
                                String ofLocLower = ofLoc.getTitle().toLowerCase(Locale.ROOT);
                                if (!actionLower.contains(ofLocLower)) continue;
                                if (objFirstAnalyzer.isObjectiveRelevantLocation(ofLoc.getTitle())) {
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-objective-first",
                                        DeployObjectiveSitingPolicy.scoreObjectiveFirst(
                                            new DeployObjectiveSitingPolicy.ObjectiveFirstFacts(
                                                actionId, ofLoc.getTitle())));
                                    LOG.warn("V51 OBJ FIRST: {} to {} — objective location pre-flip +300",
                                        card.getTitle(), ofLoc.getTitle());
                                }
                                break;
                            }
                        }
                    }

                    // V67ao (removed): per Steve, no soft penalties for character deploys
                    // when locations are still in hand. V67ai location tier bonuses
                    // (+1400 to +2000) already outscore character deploys; Combined
                    // Evaluator picks locations first naturally. The hard-block order
                    // gates only apply where the action would actually FAIL (weapon/device
                    // pull with no character on table).

                    // === V67ak (Steve, 2026-05-07): KEY-CHARACTER DEPLOY PRIORITY ===
                    //
                    // Steve's rule: 'If the objective or epic event states a specific
                    // character or character type, Rando should favor deploying those
                    // characters first. Hunt Down V mentions Vader being deployed to flip
                    // the objective, so Vader must come out first. Universal mechanism —
                    // no hardcoded character lists per deck.'
                    //
                    // Implementation: ObjectiveAnalyzer.getStrategyCharacterTokens scans
                    // objective text + Epic Event game text + Effect game text on Rando's
                    // side, extracts capitalized persona-name tokens (filtered for generic
                    // words). Any character whose title contains a token gets +800.
                    //
                    // Skip if this character (or persona) is ALREADY on table — once Vader
                    // is out, additional Vader-named cards (which would be unique-blocked
                    // anyway) don't need the priority.
                    if (category == CardCategory.CHARACTER && card != null && card.getTitle() != null
                            && context.getObjectiveAnalyzer() != null
                            && context.getObjectiveAnalyzer().isAnalyzed()) {
                        try {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer akObj =
                                context.getObjectiveAnalyzer();
                            if (akObj.isStrategyKeyCharacter(
                                    game, context.getPlayerId(), card)) {
                                boolean typedKeyRole =
                                        akObj.hasTypedStrategyKeyCharacter();
                                boolean needsAdditionalNabooDuelActor =
                                        typedKeyRole
                                        && akObj
                                            .needsAdditionalNabooDuelPayoffActor(
                                                game,
                                                context.getPlayerId(),
                                                card);
                                java.util.Set<String> candidateTokens =
                                        new java.util.HashSet<>();
                                if (!typedKeyRole) {
                                    String candidateTitle =
                                            card.getTitle()
                                                .toLowerCase(Locale.ROOT);
                                    for (String token
                                            : akObj
                                                .getStrategyCharacterTokens(
                                                    game,
                                                    context
                                                        .getPlayerId())) {
                                        if (token != null
                                                && candidateTitle
                                                    .contains(token)) {
                                            candidateTokens.add(token);
                                        }
                                    }
                                }
                                // Check the matched token is NOT already on table as a card
                                // that satisfies the same key-character role.
                                boolean alreadyOnTable = false;
                                for (PhysicalCard exist : gameState.getAllPermanentCards()) {
                                    if (exist == null
                                            || exist.getBlueprint() == null
                                            || exist.getTitle() == null) continue;
                                    if (!context.getPlayerId().equals(exist.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone ez = exist.getZone();
                                    if (ez == null || !ez.isInPlay()) continue;
                                    if (exist.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    boolean fillsSameRole = typedKeyRole
                                            ? akObj.isStrategyKeyCharacter(
                                                game,
                                                context.getPlayerId(),
                                                exist)
                                            : candidateTokens.stream()
                                                .anyMatch(token ->
                                                    exist.getTitle()
                                                        .toLowerCase(
                                                            Locale.ROOT)
                                                        .contains(token));
                                    if (fillsSameRole
                                            && !needsAdditionalNabooDuelActor) {
                                        alreadyOnTable = true;
                                        break;
                                    }
                                }
                                if (!alreadyOnTable) {
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-key-character",
                                        DeployObjectiveSitingPolicy.scoreKeyCharacter(
                                            new DeployObjectiveSitingPolicy.KeyCharacterFacts(
                                                actionId, card.getTitle())));
                                    LOG.warn("V67ak KEY CHARACTER: {} matches strategy token — +800 deploy priority",
                                        card.getTitle());
                                } else {
                                    LOG.info("V67ak KEY CHARACTER skip: {} role already filled by an on-table card",
                                        card.getTitle());
                                }
                            }
                        } catch (Exception e) { LOG.debug("V67ak error: {}", e.getMessage()); }
                    }

                    // === V67aj (Steve, 2026-05-07): SPREAD-AWARE CHARACTER DEPLOY DESTINATION ===
                    //
                    // Steve's rules:
                    //   1. Buddy system was over-firing: Rando stacked all characters on
                    //      one location all game.
                    //   2. Where Rando deploys is critical — must check objective for
                    //      flip-required locations (Endor Operations, Dark Deal, etc.).
                    //   3. ALWAYS favor battlegrounds (battles + drains).
                    //
                    // Tiered destination scoring layered on top of V51 OBJ FIRST:
                    //   Objective-required + BG, empty:        +500 (urgent — occupy now)
                    //   Objective-required + BG, stack 1-2:    +250 (reinforce)
                    //   Objective-required, stack 3+:          0    (sufficient — spread instead)
                    //   BG (not obj-required), empty:          +300 (open new front)
                    //   BG (not obj-required), stack 1-2:      +100 (mild reinforce)
                    //   BG (not obj-required), stack 3+:       -300 (V67aj OVER-STACK)
                    //   Non-BG: handled by V67ah (already in CardSelectionEvaluator)
                    //
                    // The OVER-STACK penalty fights the over-buddy clustering Steve called out.
                    // Combined with V51 OBJ FIRST (+300) and V29.7 (+80 for BG), an empty
                    // objective-required BG can score +880 across rules.
                    //
                    // V67aj + nested V67al if(false) block DELETED 2026-07-12 batch 1.5 (V136 §B owns site-stack scoring) — see git history.

                    boolean exactTdigwattEngineDeploy =
                        false;
                    if (game != null && gameState != null
                            && playerId != null
                            && card != null) {
                        var tdigwattIdentity =
                            TdigwattObjectiveFactsReader
                                .readObjectiveIdentity(
                                    game, playerId);
                        TdigwattObjectiveFacts.PullTarget
                            tdigwattTarget = null;
                        if (com.gempukku.swccgo.filters
                                .Filters.Dark_Deal
                                .accepts(
                                    gameState,
                                    game.getModifiersQuerying(),
                                    card)) {
                            tdigwattTarget =
                                TdigwattObjectiveFacts
                                    .PullTarget.DARK_DEAL;
                        } else if (com.gempukku.swccgo.filters
                                .Filters.Cloud_City_Occupation
                                .accepts(
                                    gameState,
                                    game.getModifiersQuerying(),
                                    card)) {
                            tdigwattTarget =
                                TdigwattObjectiveFacts
                                    .PullTarget
                                    .CLOUD_CITY_OCCUPATION;
                        }
                        if (tdigwattIdentity.isPresent()
                                && tdigwattTarget != null) {
                            var tdigwattPersistence =
                                TdigwattObjectiveFactsReader
                                    .readEngineEffectPersistsAfterDeploy(
                                        game, playerId, card);
                            var tdigwattEngine =
                                TdigwattObjectiveScoringPolicy
                                    .scoreEngineDeploy(
                                        new TdigwattObjectiveScoringPolicy
                                            .EngineDeployFacts(
                                                actionId,
                                                tdigwattIdentity
                                                    .get(),
                                                tdigwattIdentity
                                                    .get()
                                                    .physicalCardId(),
                                                tdigwattTarget,
                                                true,
                                                true,
                                                tdigwattPersistence
                                                    .isPresent(),
                                                tdigwattPersistence
                                                    .orElse(false)));
                            applySharedPolicy(
                                action, decisionId,
                                actionId,
                                "deploy-tdigwatt-engine",
                                tdigwattEngine.result());
                            exactTdigwattEngineDeploy =
                                tdigwattPersistence
                                    .orElse(false)
                                && (tdigwattTarget
                                        == TdigwattObjectiveFacts
                                            .PullTarget.DARK_DEAL
                                    || tdigwattIdentity.get()
                                        .printing()
                                        == TdigwattObjectiveFacts
                                            .Printing.CLASSIC
                                        && tdigwattTarget
                                            == TdigwattObjectiveFacts
                                                .PullTarget
                                                .CLOUD_CITY_OCCUPATION);

                            if (tdigwattTarget
                                    == TdigwattObjectiveFacts
                                        .PullTarget.DARK_DEAL) {
                                TdigwattObjectiveFactsReader
                                    .readClassicFrontState(
                                        game, playerId)
                                    .ifPresent(before -> {
                                        TdigwattObjectiveFacts
                                            .ClassicState after =
                                                new TdigwattObjectiveFacts
                                                    .ClassicState(
                                                        before
                                                            .objective(),
                                                        true,
                                                        before
                                                            .darkOccupiesBespinSystem(),
                                                        before
                                                            .darkOccupiesBespinCloudCity(),
                                                        false,
                                                        before
                                                            .opponentControlsBespinSystem(),
                                                        false);
                                        applySharedPolicy(
                                            action,
                                            decisionId,
                                            actionId,
                                            "deploy-tdigwatt-flip",
                                            TdigwattObjectiveScoringPolicy
                                                .scoreDeploy(
                                                    actionId,
                                                    before,
                                                    after,
                                                    true)
                                                .result());
                                    });
                            }
                        }
                    }

                    // === V22.7: CLOUD CITY OCCUPATION GUARD ===
                    // Cloud City Occupation self-cancels if we don't occupy Bespin system.
                    // Don't waste the deploy — block it until we actually occupy Bespin.
                    // Also check Dark Deal (V) which has similar Bespin requirements.
                    if (!exactTdigwattEngineDeploy
                            && (cardTitleLower.contains("cloud city occupation") || cardTitleLower.contains("dark deal"))) {
                        boolean weOccupyBespin = false;
                        try {
                            String pid = context.getPlayerId();
                            String opponentId = gameState.getOpponent(pid);
                            for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null && loc.getTitle() != null &&
                                    loc.getTitle().toLowerCase(Locale.ROOT).contains("bespin") &&
                                    loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                    loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                    float ourPower = context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, pid, false, false);
                                    float theirPower = opponentId != null ?
                                        context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, loc, opponentId, false, false) : 0;
                                    // "Occupy" = we have presence and opponent does NOT
                                    weOccupyBespin = (ourPower > 0 && theirPower == 0);
                                    LOG.info("V22.7 BESPIN CHECK: our power={}, their power={}, occupy={}",
                                        ourPower, theirPower, weOccupyBespin);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V22.7: Could not check Bespin occupation: {}", e.getMessage());
                        }
                        if (!weOccupyBespin) {
                            DeployObjectiveSitingPolicy.CloudCityEngineEvaluation
                                cloudCityEngine = DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                                    new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                        actionId, card.getTitle(), false, false));
                            applySharedPolicy(action, decisionId, actionId,
                                "deploy-cloud-city-engine", cloudCityEngine.result());
                            LOG.warn("🚫 V22.7: BLOCKING {} — we don't occupy Bespin, it will self-cancel!",
                                card.getTitle());
                            actions.add(action);
                            continue;
                        } else {
                            // V24: TDIGWATT ENGINE BOOST — Dark Deal and CC Occupation are the core damage engine
                            // When we occupy Bespin, deploying these is TOP PRIORITY
                            boolean effectAlreadyOnTable = false;
                            try {
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle effectOracle = context.getDeckOracle();
                                if (effectOracle != null) {
                                    effectAlreadyOnTable = effectOracle.isCardInPlay(card.getTitle());
                                }
                            } catch (Exception e) {
                                LOG.debug("V24: Error checking if effect is on table: {}", e.getMessage());
                            }
                            DeployObjectiveSitingPolicy.CloudCityEngineEvaluation
                                cloudCityEngine = DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                                    new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                        actionId, card.getTitle(), true,
                                        effectAlreadyOnTable));
                            applySharedPolicy(action, decisionId, actionId,
                                "deploy-cloud-city-engine", cloudCityEngine.result());
                            if (cloudCityEngine.outcome()
                                    == DeployObjectiveSitingPolicy.CloudCityEngineOutcome.ENGINE_PRIORITY) {
                                LOG.warn("V24 TDIGWATT ENGINE: {} gets +300 — CRITICAL engine piece, deploy ASAP!", card.getTitle());
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════════
                    // ═══ SECTION: DEPLOY-3 — Weapons, Pilots & Ships (reorg 2026-07-06) ═══
                    // Owns: V158 unified weapon deploy gate (below) + the deliberately-separate
                    // V120/V185 pull gates (ActionTextEvaluator/DeckOracle side), pilots (V30),
                    // vehicles/ships (V35.5/V35.6/V86). Hub: V158 LIVE. KIND mix + key
                    // magnitudes: VETO-heavy (-9999 one-weapon gate, V185 -2000 pull block) +
                    // ORDERING (V67m/V67am +600 weapon pulls, V33 named-weapon-first) + BANDED
                    // (V30 ±1000 pilot+ship, V86/V121 -1500/+300, V158 +300 arm-unarmed).
                    // Absorbed (V33-block, V67aq, V115): the old commented blocks were DELETED
                    // 2026-07-12 batch 1.5 — revert path = git history. (V180 persona-scan
                    // fix lives inside V158's NO-WIELDER guard.)
                    // Cross-refs: DEPLOY-2 (character siting), SVC-ORACLE (V185 attach gate),
                    // CardSelectionEvaluator wielder-pick (decides WHICH character gets the
                    // weapon). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 +
                    // Rando_Section_Manifest_2026-07-06.xlsx.
                    // ═══════════════════════════════════════════════════════════
                    // ============================================================
                    // === V158 (Steve, 2026-05-28): UNIFIED WEAPON DEPLOY GATE ===
                    // Combines V33 one-weapon-block + V67aq + V115 into ONE rule (the old
                    // V33/V67aq/V115 deploy-gate logic is removed — they conflicted and
                    // double-counted points). Decides WHETHER to deploy this weapon; the
                    // wielder-pick in CardSelectionEvaluator still decides WHICH character.
                    //
                    // CRITERIA-SAFE (Steve's caution): a mis-parsed "deploys on X" must NOT
                    // block every weapon. The criteria block fires ONLY when a matching ARMED
                    // wielder exists — proof the criteria parsed to a real attribute. Garbage
                    // criteria → 0 matching armed → falls through to the generic unarmed check,
                    // so weapons still deploy. (Old V115 blocked on matchUnarmed==0 even when
                    // matchArmed==0, which false-blocked on bad criteria — fixed here.)
                    //
                    // Hard-block -9999 (hold / Done) when ANY of:
                    //   1. criteria parsed AND >=1 matching ARMED AND 0 matching UNARMED
                    //      → every legal wielder already armed (e.g. 2nd Sidious' Lightsaber).
                    //   2. lightsaber AND 0 unarmed [Warrior] ability>=4 wielders (folds in V149
                    //      for hand-deploys — a saber needs a capable wielder).
                    //   3. 0 unarmed characters at all (every character armed).
                    // Else +300 — an unarmed wielder exists. No characters at all → V67ao order
                    // gate elsewhere handles it.
                    // ============================================================
                    boolean exactMassassiTorpedoesDeploy =
                        category == CardCategory.WEAPON
                            && context.getObjectiveAnalyzer() != null
                            && context.getObjectiveAnalyzer()
                                .isMassassiAttackRunPackageDeployCandidate(
                                    game, playerId, card)
                            && com.gempukku.swccgo.filters.Filters
                                .Proton_Torpedoes.accepts(
                                    gameState,
                                    game.getModifiersQuerying(), card);
                    if (category == CardCategory.WEAPON
                            && gameState != null
                            && !exactMassassiTorpedoesDeploy
                            && !exactSetYourCourseSuperlaserDeploy) {
                        try {
                            String wepPlayerId = context.getPlayerId();
                            String v158Criteria = null;
                            try {
                                v158Criteria = com.gempukku.swccgo.ai.models.rando.evaluators
                                    .CardSelectionEvaluator.v70ExtractDeployCriteria(card.getBlueprint().getGameText());
                            } catch (Exception ignore) { /* null */ }
                            boolean v158IsLightsaber = cardTitleLower.contains("lightsaber");

                            int totalArmed = 0, totalUnarmed = 0, matchArmed = 0, matchUnarmed = 0, unarmedWarrior4 = 0;
                            for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                if (tc == null || !wepPlayerId.equals(tc.getOwner())) continue;
                                if (tc.getBlueprint() == null
                                        || tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                com.gempukku.swccgo.common.Zone z = tc.getZone();
                                if (z == null || !z.isInPlay()) continue;
                                boolean armed = false;
                                java.util.List<PhysicalCard> atts = gameState.getAttachedCards(tc);
                                if (atts != null) {
                                    for (PhysicalCard a : atts) {
                                        if (a != null && a.getBlueprint() != null
                                                && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) { armed = true; break; }
                                    }
                                }
                                if (armed) totalArmed++; else totalUnarmed++;
                                if (v158Criteria != null) {
                                    boolean m = false;
                                    try {
                                        m = com.gempukku.swccgo.ai.models.rando.evaluators
                                            .CardSelectionEvaluator.v70CharacterMatchesCriteria(context.getGame(), gameState, tc, v158Criteria);
                                    } catch (Exception ignore) { }
                                    if (m) { if (armed) matchArmed++; else matchUnarmed++; }
                                }
                                if (v158IsLightsaber && !armed
                                        && tc.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.WARRIOR)) {
                                    Float ab = tc.getBlueprint().hasAbilityAttribute() ? tc.getBlueprint().getAbility() : null;
                                    if (ab != null && ab >= 4f) unarmedWarrior4++;
                                }
                            }

                            PolicyContributionLedger v213WeaponLedger = new PolicyContributionLedger(
                                "deploy-weapon-v158-" + actionId);
                            v213WeaponLedger.register(DeployWeaponPolicy.evaluateDirectEligibility(
                                new DeployWeaponPolicy.DirectEligibilityFacts(
                                    actionId, v158Criteria, v158IsLightsaber,
                                    totalArmed, totalUnarmed, matchArmed, matchUnarmed,
                                    unarmedWarrior4)));
                            PolicyOperationAdapter.apply(action, v213WeaponLedger);

                            if (v158Criteria != null && matchUnarmed == 0) {
                                LOG.warn("V158 WEAPON BLOCK ({}): {} criteria='{}' matchArmed={} matchUnarmed=0 → -9999",
                                    matchArmed > 0 ? "criteria all armed" : "criteria absent",
                                    card.getTitle(), v158Criteria, matchArmed);
                            } else if (v158IsLightsaber && unarmedWarrior4 == 0) {
                                LOG.warn("V158 WEAPON BLOCK (no lightsaber wielder): {} → -9999", card.getTitle());
                            } else if (totalUnarmed == 0 && totalArmed > 0) {
                                LOG.warn("V158 WEAPON BLOCK (all armed): {} totalArmed={} → -9999", card.getTitle(), totalArmed);
                            } else if (totalUnarmed > 0 || matchUnarmed > 0) {
                                LOG.info("V158 WEAPON DEPLOY: {} matchUnarmed={} totalUnarmed={} → +300",
                                    card.getTitle(), matchUnarmed, totalUnarmed);
                            } else {
                                LOG.info("V158 WEAPON: {} — no characters on table, V67ao order gate applies", card.getTitle());
                            }
                        } catch (Exception e) {
                            LOG.debug("V158 error: {}", e.getMessage());
                        }
                    }


                    // === V33: NAMED WEAPON PRIORITY ===
                    // Unique character-specific weapons (Vader's Lightsaber, Mara's Lightsaber, etc.)
                    // should deploy BEFORE generic weapons (Dark Jedi Lightsaber).
                    // If deploying a generic weapon on a character who has a named weapon available
                    // in hand, penalize the generic weapon to save the slot.
                    if (category == CardCategory.WEAPON && gameState != null) {
                        try {
                            // Determine if THIS weapon is character-specific (named) or generic
                            boolean isNamedWeapon = cardTitleLower.contains("vader") || cardTitleLower.contains("mara")
                                || cardTitleLower.contains("maul") || cardTitleLower.contains("palpatine")
                                || cardTitleLower.contains("emperor") || cardTitleLower.contains("luke")
                                || cardTitleLower.contains("obi-wan") || cardTitleLower.contains("ahsoka")
                                || cardTitleLower.contains("sabine") || cardTitleLower.contains("inquisitor")
                                || cardTitleLower.contains("tarkin") || cardTitleLower.contains("piett");

                            String v213TargetCharacterName = null;
                            String v213NamedWeaponInHandTitle = null;
                            if (isNamedWeapon) {
                                LOG.warn("V33 NAMED WEAPON: {} is character-specific — boosted (+200)", card.getTitle());
                            } else {
                                // Generic weapon — check if target character has a named weapon in hand
                                String v33wPlayerId = context.getPlayerId();
                                // Find which character this weapon targets from action text
                                String targetCharName = null;
                                for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                    if (tableCard == null || !v33wPlayerId.equals(tableCard.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone v33wZone = tableCard.getZone();
                                    if (v33wZone == null || !v33wZone.isInPlay()) continue;
                                    if (tableCard.getBlueprint() == null || tableCard.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    String v33wCharTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (!v33wCharTitle.isEmpty() && actionLower.contains(v33wCharTitle)) {
                                        targetCharName = v33wCharTitle;
                                        break;
                                    }
                                }

                                if (targetCharName != null) {
                                    // Check hand for a named weapon matching this character
                                    java.util.List<PhysicalCard> v33Hand = gameState.getHand(v33wPlayerId);
                                    if (v33Hand != null) {
                                        for (PhysicalCard hc : v33Hand) {
                                            if (hc == null || hc == card || hc.getBlueprint() == null) continue;
                                            if (hc.getBlueprint().getCardCategory() != CardCategory.WEAPON) continue;
                                            String hcTitle = hc.getTitle() != null ? hc.getTitle().toLowerCase(Locale.ROOT) : "";
                                            // Check if hand weapon is named for the target character
                                            if (hcTitle.contains(targetCharName.split(",")[0].split(" ")[0])) {
                                                v213TargetCharacterName = targetCharName;
                                                v213NamedWeaponInHandTitle = hc.getTitle();
                                                LOG.warn("V33 NAMED WEAPON WAIT: Generic {} blocked on {} — named {} in hand!",
                                                    card.getTitle(), targetCharName, hc.getTitle());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            PolicyContributionLedger v213NamedWeaponLedger = new PolicyContributionLedger(
                                "deploy-weapon-v33-" + actionId);
                            v213NamedWeaponLedger.register(DeployWeaponPolicy.evaluateNamedPriority(
                                new DeployWeaponPolicy.NamedPriorityFacts(
                                    actionId, isNamedWeapon, v213TargetCharacterName,
                                    v213NamedWeaponInHandTitle)));
                            PolicyOperationAdapter.apply(action, v213NamedWeaponLedger);
                        } catch (Exception e) {
                            LOG.debug("V33 NAMED WEAPON: Error: {}", e.getMessage());
                        }
                    }

                    PolicyContributionLedger typeValueLedger =
                            new PolicyContributionLedger(
                                    (decisionId == null || decisionId.isBlank()
                                            ? "deploy-type-value"
                                            : decisionId + "-deploy-type-value")
                                            + "-" + actionId);
                    typeValueLedger.register(DeployCardValuePolicy.scoreType(
                            new DeployCardValueFacts.TypeValue(
                                    actionId, category == CardCategory.CHARACTER
                                    && abilityVal >= 4)));
                    PolicyOperationAdapter.apply(action, typeValueLedger);

                    // === V24.1C: GHERANT DEPLOY BONUS ===
                    // Commander Gherant pulls an Executor site when deployed.
                    // That's a FREE location = force generation. Treat him almost like deploying a location.
                    if (category == CardCategory.CHARACTER && cardTitleLower.contains("gherant")) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer gherantObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (gherantObjAnalyzer != null && gherantObjAnalyzer.isAnalyzed()
                            && gherantObjAnalyzer.needsBespinSystemPresence()) {
                            applySharedPolicy(action, decisionId, actionId,
                                "deploy-gherant",
                                DeployObjectiveSitingPolicy.scoreGherant(
                                    new DeployObjectiveSitingPolicy.GherantFacts(actionId)));
                            LOG.warn("V24.1 GHERANT: {} gets +150 — pulls Executor site on deploy!", card.getTitle());
                        }
                    }

                    // === V29.2: LANDO/LOBOT DEPLOY PRIORITY (TDIGWATT) ===
                    // Lando and Lobot are critical for flipping TDIGWATT, BUT they should NOT
                    // deploy alone to a Cloud City site with no backup — they'll get killed.
                    // V29.2 FIX: Check BOTH the card title AND the action text for "lando"/"lobot".
                    // The action text is crucial because "Deploy Lando from Reserve Deck" comes from
                    // Dining Room (a LOCATION card), so the resolved card is Dining Room, not Lando.
                    // We can't rely on category == CHARACTER or cardTitleLower containing "lando".
                    {
                        // Check if this action involves deploying Lando or Lobot (from action text OR card title)
                        String actionTextLower = actionText != null ? actionText.toLowerCase(Locale.ROOT) : "";
                        boolean isLandoDeploy = cardTitleLower.contains("lando") || actionTextLower.contains("lando");
                        boolean isLobotDeploy = cardTitleLower.contains("lobot") || actionTextLower.contains("lobot");

                        if (isLandoDeploy || isLobotDeploy) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer landoObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (landoObjAnalyzer != null && landoObjAnalyzer.isAnalyzed()
                                && landoObjAnalyzer.needsBespinSystemPresence()) {

                                // V29.6: Check if we have friendly characters at Dining Room specifically.
                                // Previous V29 checked ANY CC site, but that's wrong — if friendlies are
                                // at Carbonite Chamber but nobody at Dining Room, Lando still deploys alone
                                // to DR and gets killed. Must check the actual deploy destination.
                                boolean haveCharAtCCSite = false;
                                GameState landoGs = context.getGameState();
                                SwccgGame landoGame = context.getGame();
                                if (landoGs != null && landoGame != null) {
                                    try {
                                        String landoPlayerId = context.getPlayerId();
                                        // Check Dining Room specifically for Lando deploys via DR action
                                        boolean isDiningRoomAction = actionTextLower.contains("dining room");
                                        for (PhysicalCard loc : landoGs.getTopLocations()) {
                                            if (loc == null || loc.getTitle() == null) continue;
                                            String locT = loc.getTitle().toLowerCase(Locale.ROOT);
                                            if (!locT.contains("cloud city")) continue;
                                            // V29.6: If this is a Dining Room action, only check Dining Room
                                            if (isDiningRoomAction && !locT.contains("dining room")) continue;
                                            java.util.List<PhysicalCard> cardsHere = landoGs.getCardsAtLocation(loc);
                                            if (cardsHere == null) continue;
                                            for (PhysicalCard c : cardsHere) {
                                                if (c != null && landoPlayerId.equals(c.getOwner())
                                                    && c.getBlueprint() != null
                                                    && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                    haveCharAtCCSite = true;
                                                    break;
                                                }
                                            }
                                            if (haveCharAtCCSite) break;
                                        }
                                    } catch (Exception e) {
                                        LOG.debug("V29.6 LANDO SOLO CHECK: Error: {}", e.getMessage());
                                    }
                                }

                                DeployObjectiveSitingPolicy.LandoLobotEvaluation
                                    landoLobot = DeployObjectiveSitingPolicy.evaluateLandoLobot(
                                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                            actionId, isLandoDeploy, isLobotDeploy,
                                            haveCharAtCCSite));
                                applySharedPolicy(action, decisionId, actionId,
                                    "deploy-lando-lobot", landoLobot.result());
                                switch (landoLobot.outcome()) {
                                    case LANDO_SAFE ->
                                        LOG.warn("V29.2 LANDO: +200 — has backup at CC site! (actionText='{}')", actionText);
                                    case LANDO_BLOCKED ->
                                        LOG.warn("V47 LANDO SOLO BLOCK: No friendly chars at CC — blocking Lando reserve deploy! (actionText='{}')", actionText);
                                    case LOBOT_SAFE ->
                                        LOG.warn("V29.2 LOBOT: +150 — has backup!");
                                    case LOBOT_BLOCKED ->
                                        LOG.warn("V47 LOBOT SOLO BLOCK: No friendly chars at CC — blocking Lobot reserve deploy!");
                                    case NONE -> { }
                                }
                            }
                        }
                    }

                    // === V31: PRE-FLIP vs POST-FLIP OBJECTIVE DEPLOYMENT STRATEGY ===
                    // PRE-FLIP: Spread characters across objective locations to meet flip condition.
                    //   - TDIGWATT needs to occupy 3 Bespin locations (system + 2 CC sites).
                    //   - Solo deploys to objective locations are OK pre-flip — we need presence fast.
                    //   - Bonus for deploying to unoccupied objective locations.
                    // POST-FLIP: Consolidate to fewer locations to hold.
                    //   - TDIGWATT only needs 2 locations to prevent flip-back (1 CC site + Bespin system).
                    //   - Penalize deploying to a 3rd objective location — consolidate to 2.
                    //   - Bonus for reinforcing the 2 strongest held objective locations.
                    if (category == CardCategory.CHARACTER && card != null) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer flipObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (flipObjAnalyzer != null && flipObjAnalyzer.isAnalyzed()
                            && gameState != null && game != null) {
                            try {
                                String flipPlayerId = context.getPlayerId();
                                java.util.Set<String> objLocFragments = flipObjAnalyzer.getFlipConditionLocationFragments();

                                if (!flipObjAnalyzer.isFlipped()) {
                                    // === PRE-FLIP: Spread to meet flip condition ===
                                    // Count how many objective-relevant locations we already occupy
                                    int occupiedObjLocs = 0;
                                    int unoccupiedObjLocs = 0;
                                    java.util.List<String> unoccupiedLocNames = new java.util.ArrayList<>();
                                    for (PhysicalCard loc : gameState.getTopLocations()) {
                                        if (loc == null || loc.getTitle() == null) continue;
                                        String locLower = loc.getTitle().toLowerCase(Locale.ROOT);
                                        boolean isObjLoc = false;
                                        for (String frag : objLocFragments) {
                                            if (locLower.contains(frag.toLowerCase(Locale.ROOT))) {
                                                isObjLoc = true;
                                                break;
                                            }
                                        }
                                        if (!isObjLoc) continue;
                                        float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, loc, flipPlayerId, false, false);
                                        if (ourPower > 0) {
                                            occupiedObjLocs++;
                                        } else {
                                            unoccupiedObjLocs++;
                                            unoccupiedLocNames.add(loc.getTitle());
                                        }
                                    }

                                    // Check if this deploy action targets an unoccupied objective location
                                    boolean deploysToUnoccupiedObjLoc = false;
                                    for (String unoccName : unoccupiedLocNames) {
                                        if (actionLower.contains(unoccName.toLowerCase(Locale.ROOT))) {
                                            deploysToUnoccupiedObjLoc = true;
                                            break;
                                        }
                                    }

                                    int turnNum = 0;
                                    boolean isHuntDown36 = false;
                                    boolean isInquisitor36 = false;
                                    if (unoccupiedObjLocs > 0 && deploysToUnoccupiedObjLoc) {
                                        turnNum = context.getTurnNumber();
                                        isHuntDown36 = flipObjAnalyzer.isHuntDownV();
                                        String deployCardLower36 = card.getTitle() != null
                                            ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                                        isInquisitor36 = isInquisitor(deployCardLower36);
                                    }
                                    DeployObjectiveSitingPolicy.FlipSitingEvaluation
                                        flipSiting = DeployObjectiveSitingPolicy.evaluateFlipSiting(
                                            new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                                actionId, false, turnNum,
                                                isHuntDown36, isInquisitor36,
                                                occupiedObjLocs, unoccupiedObjLocs,
                                                deploysToUnoccupiedObjLoc,
                                                false, false));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-flip-siting", flipSiting.result());
                                    if (flipSiting.outcome()
                                            == DeployObjectiveSitingPolicy.FlipSitingOutcome.PREFLIP_DEFEND) {
                                        float defendBonus = flipSiting.result().operations()
                                            .get(0).delta();
                                        if (isHuntDown36 && turnNum <= 3) {
                                            LOG.warn("V36 DEFEND MALACHOR: {} to empty obj site EARLY (turn {}) — must defend! (+{})",
                                                card.getTitle(), turnNum, (int)defendBonus);
                                        } else if (isHuntDown36) {
                                            LOG.warn("V36 DEFEND TERRITORY: {} to empty obj site (turn {}) — +{}",
                                                card.getTitle(), turnNum, (int)defendBonus);
                                        }
                                        LOG.warn("V36 PRE-FLIP: {} to unoccupied obj loc (+{}) — {}/{} occupied",
                                            card.getTitle(), (int)defendBonus,
                                            occupiedObjLocs,
                                            occupiedObjLocs + unoccupiedObjLocs);
                                    }
                                } else {
                                    // === POST-FLIP: Consolidate to fewer locations ===
                                    // After flipping, we only need to HOLD enough locations to prevent flip-back.
                                    // For TDIGWATT: hold Bespin system + 1 CC site = 2 total (not 3).
                                    // Find the 2 strongest objective locations and reinforce those.
                                    java.util.List<PhysicalCard> occupiedObjLocCards = new java.util.ArrayList<>();
                                    java.util.Map<String, Float> objLocPower = new java.util.LinkedHashMap<>();
                                    java.util.Set<String> exactStructuredHoldLocations =
                                        new java.util.LinkedHashSet<>();
                                    boolean structuredHoldLocationsAuthoritative =
                                        flipObjAnalyzer.hasStructuredFlipBackLocationRules();
                                    for (PhysicalCard loc : gameState.getTopLocations()) {
                                        if (loc == null || loc.getTitle() == null) continue;
                                        if (structuredHoldLocationsAuthoritative
                                                && flipObjAnalyzer.isFlipBackProtectionLocation(
                                                    loc, game, flipPlayerId)) {
                                            exactStructuredHoldLocations.add(loc.getTitle());
                                        }
                                        String locLower = loc.getTitle().toLowerCase(Locale.ROOT);
                                        boolean isObjLoc = false;
                                        for (String frag : objLocFragments) {
                                            if (locLower.contains(frag.toLowerCase(Locale.ROOT))) {
                                                isObjLoc = true;
                                                break;
                                            }
                                        }
                                        if (!isObjLoc) continue;
                                        float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, loc, flipPlayerId, false, false);
                                        if (ourPower > 0) {
                                            occupiedObjLocCards.add(loc);
                                            objLocPower.put(loc.getTitle(), ourPower);
                                        }
                                    }

                                    java.util.Set<String> holdLocations =
                                        DeployObjectiveSitingPolicy.selectPostFlipHoldLocations(
                                            structuredHoldLocationsAuthoritative,
                                            exactStructuredHoldLocations,
                                            objLocPower);

                                    // Check if deploy target is one of the hold locations
                                    boolean deploysToHoldLoc = false;
                                    for (String holdLoc : holdLocations) {
                                        if (actionLower.contains(holdLoc.toLowerCase(Locale.ROOT))) {
                                            deploysToHoldLoc = true;
                                            break;
                                        }
                                    }

                                    // Deploying to a non-hold objective location post-flip.
                                    boolean deploysToAnyObjLoc = false;
                                    if (!deploysToHoldLoc) {
                                        for (String frag : objLocFragments) {
                                            if (actionLower.contains(frag.toLowerCase(Locale.ROOT))) {
                                                deploysToAnyObjLoc = true;
                                                break;
                                            }
                                        }
                                    }
                                    DeployObjectiveSitingPolicy.FlipSitingEvaluation
                                        flipSiting = DeployObjectiveSitingPolicy.evaluateFlipSiting(
                                            new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                                actionId, true, 0, false, false,
                                                occupiedObjLocCards.size(), 0,
                                                false, deploysToHoldLoc,
                                                deploysToAnyObjLoc));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-flip-siting", flipSiting.result());
                                    if (flipSiting.outcome()
                                            == DeployObjectiveSitingPolicy.FlipSitingOutcome.POSTFLIP_HOLD) {
                                        LOG.warn("V31 POST-FLIP: {} reinforcing hold location (+200)", card.getTitle());
                                    } else if (flipSiting.outcome()
                                            == DeployObjectiveSitingPolicy.FlipSitingOutcome.POSTFLIP_THIRD_NEUTRAL) {
                                        LOG.warn("V40 POST-FLIP: {} deploy to 3rd obj loc — neutral (was -100)", card.getTitle());
                                    }
                                }
                            } catch (Exception e) {
                                LOG.debug("V31 PRE/POST-FLIP: Error: {}", e.getMessage());
                            }
                        }
                    }

                    // === V32: ABILITY >= 4 DEPLOYMENT RULE ===
                    // SWCCG requires total ability >= 4 at a site to draw battle destiny.
                    // Without battle destiny, you lose almost every battle. NEVER leave
                    // total friendly ability < 4 at a site after deploying.
                    //
                    // Rules:
                    // 1. If deploying solo (no other friendlies at site), check if this
                    //    character's ability >= 4 alone. If not, check if another character
                    //    in hand can follow up to reach >= 4 total.
                    // 2. If no follow-up available, penalize solo deploy of low-ability char.
                    // 3. Exception: pre-flip objective locations where solo presence is needed
                    //    to meet flip conditions (handled by V31 bonus overriding this).
                    if (category == CardCategory.CHARACTER && card != null && card.getBlueprint() != null
                        && gameState != null && game != null) {
                        try {
                            float cardAbility = 0;
                            if (card.getBlueprint().hasAbilityAttribute()) {
                                Float ab = card.getBlueprint().getAbility();
                                cardAbility = ab != null ? ab : 0;
                            }

                            // Try to figure out deploy destination from action text
                            String v32PlayerId = context.getPlayerId();
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                // Only check sites (not systems — systems use starships)
                                if (loc.getBlueprint() == null || loc.getBlueprint().getCardSubtype() == null) continue;
                                if (loc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SITE) continue;

                                String siteTitle = loc.getTitle().toLowerCase(Locale.ROOT);
                                if (!actionLower.contains(siteTitle)) continue;

                                // Found likely deploy destination — count current friendly ability here
                                float currentAbilityAtSite = 0;
                                int friendlyCharCount = 0;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c == null || !v32PlayerId.equals(c.getOwner())) continue;
                                    if (c.getBlueprint() == null) continue;
                                    if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    friendlyCharCount++;
                                    if (c.getBlueprint().hasAbilityAttribute()) {
                                        Float cAb = c.getBlueprint().getAbility();
                                        currentAbilityAtSite += (cAb != null ? cAb : 0);
                                    }
                                }

                                float totalAfterDeploy = currentAbilityAtSite + cardAbility;
                                boolean canFollowUp = false;
                                if (totalAfterDeploy < 4.0f && friendlyCharCount == 0) {
                                    java.util.List<PhysicalCard> handCards = gameState.getHand(v32PlayerId);
                                    if (handCards != null) {
                                        for (PhysicalCard hc : handCards) {
                                            if (hc == null || hc == card) continue;
                                            if (hc.getBlueprint() == null) continue;
                                            if (hc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                            float hcAbility = 0;
                                            if (hc.getBlueprint().hasAbilityAttribute()) {
                                                Float hcAb = hc.getBlueprint().getAbility();
                                                hcAbility = hcAb != null ? hcAb : 0;
                                            }
                                            if (cardAbility + hcAbility >= 4.0f) {
                                                // Found a follow-up character that reaches threshold
                                                canFollowUp = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                DeployFormationSitingPolicy.AbilityThresholdEvaluation
                                    abilityThreshold = DeployFormationSitingPolicy.evaluateAbilityThreshold(
                                        new DeployFormationSitingPolicy.AbilityThresholdFacts(
                                            actionId, loc.getTitle(), currentAbilityAtSite,
                                            friendlyCharCount, cardAbility, canFollowUp));
                                applySharedPolicy(action, decisionId, actionId,
                                    "deploy-ability-threshold", abilityThreshold.result());
                                switch (abilityThreshold.outcome()) {
                                    case FIXES_DEFICIT ->
                                        LOG.warn("V32 ABILITY FIX: {} (ability {}) fixes deficit at {} (was {}, now {})",
                                            card.getTitle(), cardAbility, loc.getTitle(), currentAbilityAtSite, totalAfterDeploy);
                                    case SOLO_NO_FOLLOW_UP ->
                                        LOG.warn("V32 ABILITY RISK: {} (ability {}) solo at {} with no follow-up — penalized (-200)",
                                            card.getTitle(), cardAbility, loc.getTitle());
                                    case SHARED_BELOW_THRESHOLD ->
                                        LOG.warn("V32 ABILITY WARNING: {} to {} — total ability {} still < 4!",
                                            card.getTitle(), loc.getTitle(), totalAfterDeploy);
                                    case NONE, SOLO_WITH_FOLLOW_UP -> { }
                                }
                                break; // Only check first matching location
                            }
                        } catch (Exception e) {
                            LOG.debug("V32 ABILITY CHECK: Error: {}", e.getMessage());
                        }
                    }

                    // === V33: ABILITY 7 BUDDY SYSTEM ===
                    // Encourage stacking ability at sites to reach 7+. This goes beyond the
                    // hard requirement of 4 (for battle destiny) — ability 7 means the bot
                    // can comfortably win battles even against decent opposition.
                    // Bonus for deploying to a site with < 7 friendly ability.
                    if (category == CardCategory.CHARACTER && card != null && card.getBlueprint() != null
                        && gameState != null && game != null) {
                        try {
                            float v33CardAbility = 0;
                            if (card.getBlueprint().hasAbilityAttribute()) {
                                Float v33Ab = card.getBlueprint().getAbility();
                                v33CardAbility = v33Ab != null ? v33Ab : 0;
                            }

                            String v33PlayerId = context.getPlayerId();
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (loc.getBlueprint() == null || loc.getBlueprint().getCardSubtype() == null) continue;
                                if (loc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SITE) continue;

                                String v33SiteTitle = loc.getTitle().toLowerCase(Locale.ROOT);
                                if (!actionLower.contains(v33SiteTitle)) continue;

                                // V67ab (Steve, 2026-05-03): Only stack ability at BATTLEGROUNDS.
                                // V33 BUDDY FIX/BONUS was firing for non-battleground sites where
                                // battles can't happen — wasting characters on places they can't
                                // contribute. Symptom: Mira deployed to Coruscant: The Works
                                // (non-BG) to "buddy" with Sidious — but Sidius doesn't need
                                // protection there (no battles), and Mira got trapped.
                                // The buddy ability >= 7 threshold exists for BATTLE destiny;
                                // non-BG sites don't have battles, so don't reward stacking there.
                                boolean v67abIsBg = false;
                                try {
                                    v67abIsBg = game.getModifiersQuerying().isBattleground(gameState, loc, null);
                                } catch (Exception e) { /* ignore */ }
                                if (!v67abIsBg) {
                                    // V67ag (Steve, 2026-05-04): NON-BG STACKING PENALTY.
                                    // V67ab skipped the buddy BONUS at non-BG, but didn't penalize
                                    // STACKING. Steve's report: 'Rando deployed Sidious to The Works
                                    // (good — drains for 1) but then loaded extra characters there
                                    // (useless — they can't battle anywhere they're stacked).'
                                    // Rule: if non-BG already has any of our characters, additional
                                    // characters wasted there.
                                    boolean v67agHasFriendly = false;
                                    String v67agExistingTitle = null;
                                    try {
                                        for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                            if (c == null || c.getBlueprint() == null) continue;
                                            if (!v33PlayerId.equals(c.getOwner())) continue;
                                            if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                            v67agHasFriendly = true;
                                            v67agExistingTitle = c.getTitle();
                                            break;
                                        }
                                    } catch (Exception e) { /* ignore */ }
                                    DeployFormationSitingPolicy.BuddyAbilityEvaluation
                                        nonBgBuddy = DeployFormationSitingPolicy.evaluateBuddyAbility(
                                            new DeployFormationSitingPolicy.BuddyAbilityFacts(
                                                actionId, loc.getTitle(), false,
                                                v67agHasFriendly, v67agExistingTitle,
                                                0.0f, v33CardAbility,
                                                RandoConfig.ABILITY_BUDDY_THRESHOLD));
                                    applySharedPolicy(action, decisionId, actionId,
                                        "deploy-buddy-ability", nonBgBuddy.result());
                                    if (nonBgBuddy.outcome()
                                            == DeployFormationSitingPolicy.BuddyAbilityOutcome.NON_BATTLEGROUND_STACK) {
                                        LOG.warn("V67ag NON-BG STACK PENALTY: {} already has friendly {} at non-BG {} — penalize additional deploy (-300)",
                                            card.getTitle(), v67agExistingTitle, loc.getTitle());
                                    } else {
                                        LOG.info("V67ab BUDDY SKIP: {} is non-battleground — V33 buddy bonus not applied (no battles here)",
                                            loc.getTitle());
                                    }
                                    break;  // Don't apply V33 to non-BG sites
                                }

                                // Count current friendly ability at this site
                                float v33CurrentAbility = 0;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c == null || !v33PlayerId.equals(c.getOwner())) continue;
                                    if (c.getBlueprint() == null) continue;
                                    if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    if (c.getBlueprint().hasAbilityAttribute()) {
                                        Float cAb = c.getBlueprint().getAbility();
                                        v33CurrentAbility += (cAb != null ? cAb : 0);
                                    }
                                }

                                float v33TotalAfter = v33CurrentAbility + v33CardAbility;
                                DeployFormationSitingPolicy.BuddyAbilityEvaluation
                                    buddyAbility = DeployFormationSitingPolicy.evaluateBuddyAbility(
                                        new DeployFormationSitingPolicy.BuddyAbilityFacts(
                                            actionId, loc.getTitle(), true, false,
                                            null, v33CurrentAbility,
                                            v33CardAbility,
                                            RandoConfig.ABILITY_BUDDY_THRESHOLD));
                                applySharedPolicy(action, decisionId, actionId,
                                    "deploy-buddy-ability", buddyAbility.result());
                                switch (buddyAbility.outcome()) {
                                    case REACHES_THRESHOLD ->
                                        LOG.warn("V33 BUDDY FIX: {} (ability {}) at {} — brings total from {} to {} (>= {})",
                                            card.getTitle(), v33CardAbility, loc.getTitle(),
                                            v33CurrentAbility, v33TotalAfter, RandoConfig.ABILITY_BUDDY_THRESHOLD);
                                    case REINFORCES ->
                                        LOG.warn("V33 BUDDY BONUS: {} reinforcing {} — ability {} → {}",
                                            card.getTitle(), loc.getTitle(), v33CurrentAbility, v33TotalAfter);
                                    case NONE, NON_BATTLEGROUND_STACK,
                                            NON_BATTLEGROUND_SKIP -> { }
                                }
                                break; // Only check first matching location
                            }
                        } catch (Exception e) {
                            LOG.debug("V33 BUDDY SYSTEM: Error: {}", e.getMessage());
                        }
                    }

                    // === V30: UNIVERSAL MATCHING PILOT + STARSHIP DEPLOY RULE ===
                    // If a pilot character and its matching starship are BOTH in hand,
                    // deploy them together NOW with maximum priority (+1000).
                    // This applies to ALL matching pilot/ship combos universally:
                    //   Piett + Executor, Han + Falcon, Wedge + Red Squadron, etc.
                    // Also: deploy them to the system mentioned in the objective (+1000).
                    //
                    // If only the pilot is in hand and matching ship is in reserve with
                    // AMSD on table, soft-prefer AMSD (-500) but allow manual fallback.
                    // If matching ship is already in play, boost deploying pilot to it (+300).
                    if (category == CardCategory.CHARACTER && card != null && card.getBlueprint() != null) {
                        Filter matchingShipFilter = card.getBlueprint().getMatchingStarshipFilter();
                        if (matchingShipFilter != null && gameState != null && game != null) {
                            try {
                                // Scan hand for matching starship
                                boolean matchingShipInHand = false;
                                boolean matchingShipInReserve = false;
                                boolean matchingShipInPlay = false;
                                String matchingShipName = null;

                                // Check hand
                                java.util.List<PhysicalCard> handCards = gameState.getHand(context.getPlayerId());
                                if (handCards != null) {
                                    for (PhysicalCard handCard : handCards) {
                                        if (handCard != null && matchingShipFilter.accepts(game.getGameState(),
                                                game.getModifiersQuerying(), handCard)) {
                                            matchingShipInHand = true;
                                            matchingShipName = handCard.getTitle();
                                            break;
                                        }
                                    }
                                }

                                // Check in play (on table)
                                if (!matchingShipInHand) {
                                    for (PhysicalCard inPlayCard : gameState.getAllPermanentCards()) {
                                        if (inPlayCard != null && context.getPlayerId().equals(inPlayCard.getOwner())
                                                && matchingShipFilter.accepts(game.getGameState(),
                                                    game.getModifiersQuerying(), inPlayCard)) {
                                            matchingShipInPlay = true;
                                            matchingShipName = inPlayCard.getTitle();
                                            break;
                                        }
                                    }
                                }

                                // Check reserve deck
                                if (!matchingShipInHand && !matchingShipInPlay) {
                                    java.util.List<PhysicalCard> reserveCards = gameState.getReserveDeck(context.getPlayerId());
                                    if (reserveCards != null) {
                                        for (PhysicalCard resCard : reserveCards) {
                                            if (resCard != null && matchingShipFilter.accepts(game.getGameState(),
                                                    game.getModifiersQuerying(), resCard)) {
                                                matchingShipInReserve = true;
                                                matchingShipName = resCard.getTitle();
                                                break;
                                            }
                                        }
                                    }
                                }

                                String v213PilotObjectiveLocation = "";
                                boolean v213AmsdInPlay = false;
                                if (matchingShipInHand) {
                                    // CASE 1: Pilot + matching ship BOTH in hand → deploy together NOW!
                                    LOG.warn("V30 MATCHING COMBO: {} + {} BOTH IN HAND — maximum priority (+1000)!",
                                        card.getTitle(), matchingShipName);

                                    // Also: if objective mentions a specific system, boost deploying THERE
                                    com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer matchObjAnalyzer =
                                        context.getObjectiveAnalyzer();
                                    if (matchObjAnalyzer != null && matchObjAnalyzer.isAnalyzed()) {
                                        java.util.Set<String> objLocations = matchObjAnalyzer.getFlipConditionLocationFragments();
                                        if (objLocations != null && actionText != null) {
                                            String actionLwr = actionText.toLowerCase(java.util.Locale.ROOT);
                                            for (String objLoc : objLocations) {
                                                if (actionLwr.contains(objLoc.toLowerCase(java.util.Locale.ROOT))) {
                                                    v213PilotObjectiveLocation = objLoc;
                                                    LOG.warn("V30 OBJECTIVE SYSTEM: {} deploying to objective location '{}' — +1000!",
                                                        card.getTitle(), objLoc);
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                } else if (matchingShipInPlay) {
                                    // CASE 2: Matching ship already in play → deploy pilot to it!
                                    LOG.warn("V30 MATCHING SHIP: {} in play — deploy {} as pilot (+300)!",
                                        matchingShipName, card.getTitle());

                                } else if (matchingShipInReserve) {
                                    // CASE 3: Matching ship in reserve — check if AMSD can pull it
                                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle matchOracle = context.getDeckOracle();
                                    if (matchOracle != null && matchOracle.isAnalyzed()) {
                                        v213AmsdInPlay = matchOracle.isCardInPlay("Alert My Star Destroyer")
                                            || matchOracle.isCardInPlay("Alert My Star Destroyer!");
                                    }
                                    if (v213AmsdInPlay) {
                                        // Prefer AMSD pull but allow manual fallback (soft penalty, NOT hard block)
                                        LOG.warn("V30 AMSD: {} in reserve — soft penalty (-500), prefer AMSD but not hard-blocked",
                                            matchingShipName);
                                    } else {
                                        // No AMSD — deploy pilot normally, ship will come later
                                        LOG.info("V30 MATCHING: {} in reserve but no AMSD — deploy {} normally",
                                            matchingShipName, card.getTitle());
                                    }
                                }
                                PolicyContributionLedger v213MatchingPilotLedger = new PolicyContributionLedger(
                                    "deploy-pilot-v30-" + actionId);
                                v213MatchingPilotLedger.register(DeployPilotShipPolicy.evaluateMatchingPilot(
                                    new DeployPilotShipPolicy.MatchingPilotFacts(
                                        actionId, card.getTitle(), matchingShipName,
                                        matchingShipInHand, matchingShipInPlay,
                                        matchingShipInReserve, v213AmsdInPlay,
                                        v213PilotObjectiveLocation)));
                                PolicyOperationAdapter.apply(action, v213MatchingPilotLedger);
                            } catch (Exception e) {
                                LOG.debug("V30 MATCHING PILOT CHECK: Error: {}", e.getMessage());
                            }
                        }
                    }

                    // === V30: UNIVERSAL MATCHING STARSHIP + PILOT DEPLOY RULE (reverse) ===
                    // Same logic but for when deploying a STARSHIP — check if matching pilot is in hand.
                    if ((category == CardCategory.STARSHIP || category == CardCategory.VEHICLE)
                            && card != null && card.getBlueprint() != null && gameState != null && game != null) {
                        try {
                            // Check hand for any character that has this ship as their matching starship
                            boolean matchingPilotInHand = false;
                            String matchingPilotName = null;
                            java.util.List<PhysicalCard> shipHandCards = gameState.getHand(context.getPlayerId());
                            if (shipHandCards != null) {
                                for (PhysicalCard handCard : shipHandCards) {
                                    if (handCard != null && handCard.getBlueprint() != null
                                            && handCard.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        Filter pilotMatchFilter = handCard.getBlueprint().getMatchingStarshipFilter();
                                        if (pilotMatchFilter != null && pilotMatchFilter.accepts(game.getGameState(),
                                                game.getModifiersQuerying(), card)) {
                                            matchingPilotInHand = true;
                                            matchingPilotName = handCard.getTitle();
                                            break;
                                        }
                                    }
                                }
                            }

                            String v213ShipObjectiveLocation = "";
                            if (matchingPilotInHand) {
                                // Ship + matching pilot both in hand → deploy together!
                                LOG.warn("V30 MATCHING COMBO: {} + {} BOTH IN HAND — maximum priority (+1000)!",
                                    card.getTitle(), matchingPilotName);

                                // Boost deploying to objective system
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer shipObjAnalyzer =
                                    context.getObjectiveAnalyzer();
                                if (shipObjAnalyzer != null && shipObjAnalyzer.isAnalyzed()) {
                                    java.util.Set<String> objLocations = shipObjAnalyzer.getFlipConditionLocationFragments();
                                    if (objLocations != null && actionText != null) {
                                        String actionLwr = actionText.toLowerCase(java.util.Locale.ROOT);
                                        for (String objLoc : objLocations) {
                                            if (actionLwr.contains(objLoc.toLowerCase(java.util.Locale.ROOT))) {
                                                v213ShipObjectiveLocation = objLoc;
                                                LOG.warn("V30 OBJECTIVE SYSTEM: {} to objective location '{}' — +1000!",
                                                    card.getTitle(), objLoc);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            PolicyContributionLedger v213MatchingShipLedger = new PolicyContributionLedger(
                                "deploy-ship-v30-" + actionId);
                            v213MatchingShipLedger.register(DeployPilotShipPolicy.evaluateMatchingShip(
                                new DeployPilotShipPolicy.MatchingShipFacts(
                                    actionId, card.getTitle(), matchingPilotName,
                                    matchingPilotInHand, v213ShipObjectiveLocation)));
                            PolicyOperationAdapter.apply(action, v213MatchingShipLedger);
                        } catch (Exception e) {
                            LOG.debug("V30 MATCHING SHIP CHECK: Error: {}", e.getMessage());
                        }
                    }

                    // === VEHICLE-PILOT GENERIC RULE (Steve, 2026-05-31; appended into V30) ===
                    // V30 above only fires for NAMED pilot/ship pairs (Wedge+X-Wing, Piett+
                    // Executor, etc.) via getMatchingStarshipFilter(). This block covers the
                    // GENERIC case: every vehicle needs a pilot, named or not.
                    // Steve (multiple games): "Rando deployed speeder bike but no pilot for
                    // the bike. Bike is useless. Rando deployed a walker without a pilot
                    // making the walker useless. Rando should try to deploy troopers to
                    // speeder bikes. And imperial pilots to Walkers. Of all other vehicles
                    // Rando should deploy a pilot on the vehicle." A solo vehicle has no
                    // move bonus, no protection, no piloting power — wasted Force.
                    //
                    // Two paths (universal — no card-name hardcoding):
                    //   (A) Deploying a VEHICLE/STARSHIP: soft-block (-1500) if it has no
                    //       permanent pilot, no planner-verified exact crew package, no
                    //       affordable pilot in hand, and no candidate pilot on table.
                    //       Soft so the engine fallback can still deploy if everything else
                    //       is worse, but Rando strongly prefers pilot-first.
                    //   (B) Deploying a PILOT-CAPABLE character: +400 if Rando has an
                    //       unmanned vehicle on table — pilot it.
                    //
                    // "Pilot-capable" detection: Icon.PILOT OR Keyword.TROOPER. Covers Imperial
                    // Pilots (Icon.PILOT, for Walkers), generic pilots (Icon.PILOT, for any
                    // vehicle), and Stormtroopers / Snowtroopers (Keyword.TROOPER, common
                    // speeder-bike riders even when missing Icon.PILOT). Engine-side
                    // getValidPilotFilter handles game-text exceptions; this AI heuristic
                    // gets 95%+ of cases right. Magnitudes: -1500 vehicle-needs-pilot block
                    // (dominates V67ai +1400 location-hand boost but leaves room for stronger
                    // overrides), +400 pilot-for-unmanned-vehicle (on par with V30 +300 MATCHING
                    // SHIP IN PLAY but slightly higher because the generic case is the BASE
                    // case Steve called out).
                    if (gameState != null && game != null && card != null && card.getBlueprint() != null) {
                        try {
                            String vpPlayerId = context.getPlayerId();
                            boolean v213DeployingAsset = false;
                            boolean v213AssetHasPermanentPilot = false;
                            boolean v213VerifiedCrewPackage = false;
                            boolean v213PilotInHand = false;
                            boolean v213AffordablePilotInHand = false;
                            boolean v213FreePilotOnTable = false;
                            int v213AssetCost = 0;
                            int v213AvailableForce = context.getForcePileSize();
                            boolean v213DeployingPilotCandidate = false;
                            String v213UnmannedAssetTitle = null;

                            // ----- Path A: VEHICLE/STARSHIP deploy without pilot available -----
                            // 2026-06-01 EXTENSION (Steve, First Light replay): STARSHIP needs the
                            // same gate as VEHICLE. First Light is CardCategory.STARSHIP (subtype
                            // CAPITAL), not VEHICLE — Path A wasn't firing on it, so Rando shipped
                            // a 5-power ship into a CONTESTED 6-power site solo and got crushed
                            // (battle attrition 13 dmg 12 ended Rando's run). Same logic applies
                            // to TIE Fighters, X-Wings, AT-ATs, AT-STs, speeder bikes etc. —
                            // they all want a pilot, the AI rule should not care about
                            // STARSHIP-vs-VEHICLE subtype distinction.
                            if (category == CardCategory.VEHICLE || category == CardCategory.STARSHIP) {
                                v213DeployingAsset = true;
                                v213AssetHasPermanentPilot =
                                    game.getModifiersQuerying().hasPermanentPilot(gameState, card);
                                v213VerifiedCrewPackage = plannedInstruction != null
                                    && plannedInstruction.isVerifiedCrewPackage();
                                // 2026-06-01 AFFORDABILITY EXTENSION (Steve, both losing games):
                                // "Rando had Walkers and did not put pilots on them. Easy targets
                                // and some of the walkers were powerless with no pilot." Replay
                                // shows Blizzard 2 deployed with a pilot in hand but Force already
                                // spent — V40 only flagged this as a mild -50 ("was -400"), the
                                // ship still scored +335 and went solo. A walker without a pilot
                                // is 0 power until next turn, opponent attacks it immediately.
                                // Fix: track "pilot in hand AND affordable to deploy together
                                // this turn" — if a pilot exists but Force can't cover the ship +
                                // a pilot deploy in the same phase, treat as if no pilot is
                                // available and apply the same -1500 block.
                                boolean hasPilotInHand = false;
                                boolean hasAffordablePilotInHand = false;
                                int vehicleCost = card.getBlueprint().getDeployCost() != null
                                    ? card.getBlueprint().getDeployCost().intValue() : 0;
                                int availForce = v213AvailableForce;
                                java.util.List<PhysicalCard> vpHand = gameState.getHand(vpPlayerId);
                                if (vpHand != null) {
                                    for (PhysicalCard hc : vpHand) {
                                        if (hc == null || hc.getBlueprint() == null) continue;
                                        if (hc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        if (!hc.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.PILOT)
                                                && !hc.getBlueprint().hasKeyword(com.gempukku.swccgo.common.Keyword.TROOPER)) continue;
                                        hasPilotInHand = true;
                                        // Affordability: can Rando pay for vehicle + this pilot
                                        // this turn? Use base deploy cost; matching-pilot reductions
                                        // and other modifiers are not modeled here (conservative,
                                        // mirrors the existing V35.6 affordability check pattern at
                                        // line 4840-4846). Exact planner-verified packages bypass
                                        // this legacy estimate through v213VerifiedCrewPackage.
                                        int pilotCost = hc.getBlueprint().getDeployCost() != null
                                            ? hc.getBlueprint().getDeployCost().intValue() : 0;
                                        if (availForce >= vehicleCost + pilotCost) {
                                            hasAffordablePilotInHand = true;
                                            break;
                                        }
                                    }
                                }
                                boolean hasFreePilotOnTable = false;
                                if (!hasAffordablePilotInHand) {
                                    for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                        if (tc == null || !vpPlayerId.equals(tc.getOwner())) continue;
                                        if (tc.getBlueprint() == null
                                                || tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        com.gempukku.swccgo.common.Zone z = tc.getZone();
                                        if (z == null || !z.isInPlay()) continue;
                                        if (tc.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.PILOT)
                                                || tc.getBlueprint().hasKeyword(com.gempukku.swccgo.common.Keyword.TROOPER)) {
                                            hasFreePilotOnTable = true;
                                            break;
                                        }
                                    }
                                }
                                if (!hasAffordablePilotInHand && !hasFreePilotOnTable) {
                                    LOG.warn("VEHICLE/SHIP NEEDS PILOT ({}): {} deploy blocked (-1500)",
                                        hasPilotInHand ? "pilot unaffordable" : "no pilot", card.getTitle());
                                }
                                v213PilotInHand = hasPilotInHand;
                                v213AffordablePilotInHand = hasAffordablePilotInHand;
                                v213FreePilotOnTable = hasFreePilotOnTable;
                                v213AssetCost = vehicleCost;
                            }

                            // ----- Path B: PILOT-CAPABLE character + unmanned vehicle on table -----
                            // 2026-06-01 POWER-3 GATE (Steve): "If pilot is power 4 or
                            // more let's leave them disembarked from vehicles. Likely
                            // better as ground troops. Regular pilots are usually power
                            // 3 or less." Skip the deploy-onto-vehicle boost for power-4+
                            // characters — they're more valuable on the ground than
                            // crewing a ship.
                            Float pathBPower = (card.getBlueprint().hasPowerAttribute()
                                ? card.getBlueprint().getPower() : null);
                            boolean pathBPowerOK = (pathBPower == null) || (pathBPower < 4f);
                            if (category == CardCategory.CHARACTER
                                    && pathBPowerOK
                                    && (card.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.PILOT)
                                        || card.getBlueprint().hasKeyword(com.gempukku.swccgo.common.Keyword.TROOPER))) {
                                v213DeployingPilotCandidate = true;
                                String unmannedTitle = null;
                                for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                    if (tc == null || !vpPlayerId.equals(tc.getOwner())) continue;
                                    if (tc.getBlueprint() == null
                                            || (tc.getBlueprint().getCardCategory() != CardCategory.VEHICLE
                                                && tc.getBlueprint().getCardCategory() != CardCategory.STARSHIP)) continue;
                                    com.gempukku.swccgo.common.Zone z = tc.getZone();
                                    if (z == null || !z.isInPlay()) continue;
                                    if (!com.gempukku.swccgo.filters.Filters.piloted.accepts(
                                            game.getGameState(), game.getModifiersQuerying(), tc)) {
                                        unmannedTitle = tc.getTitle();
                                        break;
                                    }
                                }
                                if (unmannedTitle != null) {
                                    v213UnmannedAssetTitle = unmannedTitle;
                                    LOG.warn("PILOT FOR UNMANNED VEHICLE/SHIP: {} can pilot {} on table → +400",
                                        card.getTitle(), unmannedTitle);
                                }
                            }
                            PolicyContributionLedger v213CrewLedger = new PolicyContributionLedger(
                                "deploy-crew-v30-" + actionId);
                            v213CrewLedger.register(DeployPilotShipPolicy.evaluateCrew(
                                new DeployPilotShipPolicy.CrewFacts(
                                    actionId, v213DeployingAsset,
                                    v213AssetHasPermanentPilot,
                                    v213VerifiedCrewPackage, v213PilotInHand,
                                    v213AffordablePilotInHand, v213FreePilotOnTable,
                                    v213AssetCost, v213AvailableForce,
                                    v213DeployingPilotCandidate, v213UnmannedAssetTitle)));
                            PolicyOperationAdapter.apply(action, v213CrewLedger);
                        } catch (Exception e) {
                            LOG.debug("VEHICLE-PILOT generic check error: {}", e.getMessage());
                        }
                    }

                    // === V35.6: SHIP ABILITY CHECK — NEED >= 4 ABILITY AT SYSTEM ===
                    // Just like sites need ability >= 4 to draw battle destiny, ships at
                    // systems need total ability >= 4. A ship's permanent pilot provides
                    // some ability (usually 1-2), but you need additional pilots to reach 4.
                    // Don't deploy a ship unless you have enough Force to also deploy pilots
                    // that bring total ability to >= 4, OR the ship's permanent pilot alone has >= 4.
                    // Also: if the ship has a matching pilot (Emperor for Emperor's Shuttle,
                    // Vader for Vader's Shuttle), strongly boost deploying that pilot aboard.
                    if ((category == CardCategory.STARSHIP || category == CardCategory.VEHICLE)
                        && card != null && card.getBlueprint() != null && gameState != null) {
                        try {
                            // Get ship's built-in ability (permanent pilot)
                            float shipAbility = 0;
                            if (card.getBlueprint().hasAbilityAttribute()) {
                                Float sa = card.getBlueprint().getAbility();
                                shipAbility = sa != null ? sa : 0;
                            }

                            // Check if matching pilot is in hand and affordable
                            String v36Pid = context.getPlayerId();
                            boolean matchingPilotAffordable = false;
                            float matchingPilotAbility = 0;
                            String matchingPilotTitle = null;
                            int shipCost = card.getBlueprint().getDeployCost() != null
                                ? card.getBlueprint().getDeployCost().intValue() : 0;

                            // Use the ship's matching pilot filter
                            Filter matchPilotFilter = card.getBlueprint().getMatchingPilotFilter();
                            java.util.List<PhysicalCard> v36Hand = gameState.getHand(v36Pid);
                            if (v36Hand != null && matchPilotFilter != null) {
                                for (PhysicalCard hc : v36Hand) {
                                    if (hc == null || hc.getBlueprint() == null) continue;
                                    if (hc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    if (matchPilotFilter.accepts(game.getGameState(), game.getModifiersQuerying(), hc)) {
                                        matchingPilotTitle = hc.getTitle();
                                        Float mpAb = hc.getBlueprint().getAbility();
                                        matchingPilotAbility = mpAb != null ? mpAb : 0;
                                        // Check if we can afford ship + pilot
                                        int pilotCost = hc.getBlueprint().getDeployCost() != null
                                            ? hc.getBlueprint().getDeployCost().intValue() : 0;
                                        // Matching pilot often deploys free or reduced aboard
                                        // Assume reduced cost (half) for matching pilot
                                        int totalCost = shipCost + Math.max(0, pilotCost / 2);
                                        int availForce = context.getForcePileSize();
                                        if (availForce >= totalCost) matchingPilotAffordable = true;
                                        break;
                                    }
                                }
                            }

                            float totalAbilityWithPilot = shipAbility + (matchingPilotAffordable ? matchingPilotAbility : 0);

                            if (matchingPilotAffordable && matchingPilotTitle != null) {
                                LOG.warn("V35.6 NAMED PILOT: {} + {} — total ability {} (+300)",
                                    card.getTitle(), matchingPilotTitle, totalAbilityWithPilot);
                            }

                            // V35.7: ALL ships with ability < 4 need a pilot. Period.
                            // Even if a pilot CAN help, deploying a ship solo is dangerous
                            // because Rando might not follow up with the pilot deploy.
                            boolean anyPilotHelps = false;
                            if (shipAbility < 4.0f) {
                                if (v36Hand != null) {
                                    for (PhysicalCard hc : v36Hand) {
                                        if (hc == null || hc.getBlueprint() == null) continue;
                                        if (hc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        if (!hc.getBlueprint().hasAbilityAttribute()) continue;
                                        Float hcAb = hc.getBlueprint().getAbility();
                                        if (hcAb != null && (shipAbility + hcAb) >= 4.0f) {
                                            anyPilotHelps = true;
                                            break;
                                        }
                                    }
                                }
                                if (!anyPilotHelps) {
                                    LOG.warn("V40 SHIP ABILITY: {} ability {} — mild warning (-50, was -800)",
                                        card.getTitle(), shipAbility);
                                } else if (!matchingPilotAffordable) {
                                    LOG.warn("V40 SHIP ABILITY: {} — pilot exists but unaffordable — mild warning (-50, was -400)",
                                        card.getTitle());
                                }
                            }
                            PolicyContributionLedger v213ShipAbilityLedger = new PolicyContributionLedger(
                                "deploy-ship-v35-6-" + actionId);
                            v213ShipAbilityLedger.register(DeployPilotShipPolicy.evaluateShipAbility(
                                new DeployPilotShipPolicy.ShipAbilityFacts(
                                    actionId, card.getTitle(), shipAbility,
                                    matchingPilotAffordable, matchingPilotTitle,
                                    matchingPilotAbility, totalAbilityWithPilot,
                                    anyPilotHelps)));
                            PolicyOperationAdapter.apply(action, v213ShipAbilityLedger);
                        } catch (Exception e) {
                            LOG.debug("V35.6 SHIP ABILITY: Error: {}", e.getMessage());
                        }
                    }

                    // === V35.5: DON'T DEPLOY WEAK STARSHIPS AGAINST STRONG OPPONENTS ===
                    // Emperor's Personal Shuttle (power 2) should NOT deploy to a system where
                    // Han, Chewie, And The Falcon (power 8+) is waiting. That's suicide.
                    // Check opponent ship power at the target system before deploying.
                    if ((category == CardCategory.STARSHIP || category == CardCategory.VEHICLE)
                        && gameState != null && game != null) {
                        try {
                            String v35ShipPid = context.getPlayerId();
                            String v35ShipOid = gameState.getOpponent(v35ShipPid);
                            String v35ShipActionLower = actionText.toLowerCase(Locale.ROOT);

                            // Get our ship's power
                            float ourShipPower = 0;
                            if (card.getBlueprint().hasPowerAttribute()) {
                                Float sp = card.getBlueprint().getPower();
                                ourShipPower = sp != null ? sp : 0;
                            }

                            // Find the target system in action text
                            for (PhysicalCard sysLoc : gameState.getLocationsInOrder()) {
                                if (sysLoc == null || sysLoc.getTitle() == null) continue;
                                if (sysLoc.getBlueprint() == null || sysLoc.getBlueprint().getCardSubtype() == null) continue;
                                if (sysLoc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SYSTEM
                                    && sysLoc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SECTOR) continue;
                                String sysTitle = sysLoc.getTitle().toLowerCase(Locale.ROOT);
                                if (sysTitle.isEmpty() || !v35ShipActionLower.contains(sysTitle)) continue;

                                // Found target system — check opponent ship power there
                                float oppShipPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, sysLoc, v35ShipOid, false, false);
                                PolicyContributionLedger v213ShipThreatLedger = new PolicyContributionLedger(
                                    "deploy-ship-v35-5-" + actionId);
                                v213ShipThreatLedger.register(DeployPilotShipPolicy.evaluateShipThreat(
                                    new DeployPilotShipPolicy.ShipThreatFacts(
                                        actionId, card.getTitle(), sysLoc.getTitle(),
                                        ourShipPower, oppShipPower)));
                                PolicyOperationAdapter.apply(action, v213ShipThreatLedger);
                                if (oppShipPower > 0 && oppShipPower > ourShipPower * 1.5f) {
                                    LOG.warn("V40 SHIP CAUTION: {} power {} vs opponent {} at {} — mild caution (-100, was -600/-1000)",
                                        card.getTitle(), (int)ourShipPower, (int)oppShipPower, sysLoc.getTitle());
                                }
                                break;
                            }
                        } catch (Exception e) {
                            LOG.debug("V35.5 SHIP CHECK: Error: {}", e.getMessage());
                        }
                    }

                    // === V51: UNDERCOVER SPY DEPLOY — HIGHEST PRIORITY AT DRAIN 2+ SITES ===
                    // Spies cost almost nothing and cripple the opponent's entire drain investment.
                    // Opponent spends 15-20 force deploying 3-4 characters to a drain 2 site.
                    // One spy for 1-2 force cuts that drain in half. Best ROI in the game.
                    // +1000 to deploy spy where opponent threatens drain >= 2.
                    // -300 if opponent has NO locations threatening drain >= 2 (spy is wasted).
                    if (actionLower.contains("undercover spy") || actionLower.contains("as a spy")
                        || actionLower.contains("undercover")) {
                        try {
                            String spyPlayerId = context.getPlayerId();
                            String spyOppId = game.getOpponent(spyPlayerId);

                            // First: scan ALL opponent locations for drain >= 2 threats
                            boolean opponentHasDrain2Plus = false;
                            for (PhysicalCard scanLoc : gameState.getTopLocations()) {
                                if (scanLoc == null || scanLoc.getTitle() == null) continue;
                                float scanOppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, scanLoc, spyOppId, false, false);
                                if (scanOppPower > 0) {
                                    float scanDrain = 1.0f;
                                    try {
                                        scanDrain = game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, scanLoc, spyOppId);
                                    } catch (Exception e) { /* default 1 */ }
                                    if (scanDrain >= 2.0f) {
                                        opponentHasDrain2Plus = true;
                                        break;
                                    }
                                }
                            }

                            // Now check which location this spy deploys to
                            boolean deploysToHighDrainSite = false;
                            boolean deploysToOpponentLoc = false;
                            boolean deploysToFriendlyLoc = false;
                            List<DeployTacticalPolicy.SpyDrainTarget> highDrainTargets =
                                new ArrayList<>();
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                String locTitle = loc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                if (!actionLower.contains(locTitle)) continue;

                                float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, spyOppId, false, false);
                                float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, spyPlayerId, false, false);

                                if (oppPower > 0) {
                                    deploysToOpponentLoc = true;
                                    float spyDrain = 1.0f;
                                    try {
                                        spyDrain = game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, loc, spyOppId);
                                    } catch (Exception e) { /* default 1 */ }
                                    if (spyDrain >= 2.0f) {
                                        deploysToHighDrainSite = true;
                                        highDrainTargets.add(
                                            new DeployTacticalPolicy.SpyDrainTarget(
                                                loc.getTitle(), spyDrain));
                                        // V51: SPY AT DRAIN 2+ = BEST PLAY IN THE GAME
                                        LOG.warn("V51 SPY CRIPPLE: {} to {} (drain {}) — +1000! Best ROI in the game!",
                                            card.getTitle(), loc.getTitle(), (int)spyDrain);
                                    }
                                } else if (ourPower > 0) {
                                    deploysToFriendlyLoc = true;
                                }
                            }

                            applySharedPolicy(action, decisionId, actionId,
                                "deploy-spy-placement",
                                DeployTacticalPolicy.scoreV51V43SpyPlacement(
                                    new DeployTacticalPolicy.SpyPlacementFacts(
                                        actionId, highDrainTargets,
                                        deploysToOpponentLoc, deploysToFriendlyLoc,
                                        opponentHasDrain2Plus)));

                            if (deploysToOpponentLoc && !deploysToHighDrainSite) {
                                // Opponent location but drain < 2 — still useful
                                LOG.warn("V43 SPY: {} to opponent location — +200", card.getTitle());
                            } else if (deploysToFriendlyLoc) {
                                LOG.warn("V43 SPY WASTED: {} to friendly location — -500", card.getTitle());
                            }

                            // V51: If opponent has NO drain 2+ sites, spy is low priority
                            if (!opponentHasDrain2Plus && !deploysToOpponentLoc) {
                                LOG.warn("V51 SPY NO TARGET: {} — no drain 2+ sites to cripple — -300", card.getTitle());
                            }
                        } catch (Exception e) { /* ignore */ }
                    }

                    // === V24.3A: DR. EVAZAN WEAPON COMBO DEPLOY PRIORITY ===
                    // Dr. Evazan converts weapon "hits" into immediate "lost" — devastating combo.
                    // Boost Evazan deploy when weapon characters are in play, and vice versa.
                    if (category == CardCategory.CHARACTER) {
                        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle comboOracle = context.getDeckOracle();
                        if (comboOracle != null) {
                            boolean isEvazan = cardTitleLower.contains("evazan");
                            boolean isWeaponChar = (cardTitleLower.contains("maul") && cardTitleLower.contains("lightsaber"))
                                || (cardTitleLower.contains("vader") && cardTitleLower.contains("lightsaber"))
                                || (cardTitleLower.contains("mara") && cardTitleLower.contains("lightsaber"))
                                || (cardTitleLower.contains("jade") && cardTitleLower.contains("lightsaber"))
                                || (cardTitleLower.contains("aurra") && cardTitleLower.contains("blaster"))
                                || (cardTitleLower.contains("sing") && cardTitleLower.contains("blaster"));

                            boolean weaponPartnerInPlay = false;
                            boolean evazanInPlay = false;
                            if (isEvazan) {
                                // Check if ANY weapon character is already in play
                                weaponPartnerInPlay = comboOracle.isCardInPlay("Maul With Lightsaber")
                                    || comboOracle.isCardInPlay("Vader With Lightsaber")
                                    || comboOracle.isCardInPlay("Mara Jade With Lightsaber")
                                    || comboOracle.isCardInPlay("Jade With Lightsaber")
                                    || comboOracle.isCardInPlay("Aurra Sing With Blaster")
                                    || comboOracle.isCardInPlay("Sing With Blaster");
                            } else if (isWeaponChar) {
                                // Check if Evazan is already in play
                                evazanInPlay = comboOracle.isCardInPlay("Evazan");
                            }

                            DeployTacticalPolicy.EvazanComboEvaluation evazanCombo =
                                DeployTacticalPolicy.scoreEvazanCombo(
                                    new DeployTacticalPolicy.EvazanComboFacts(
                                        actionId, isEvazan, isWeaponChar,
                                        weaponPartnerInPlay, evazanInPlay));
                            applySharedPolicy(action, decisionId, actionId,
                                "deploy-evazan-combo", evazanCombo.result());
                            switch (evazanCombo.outcome()) {
                                case DEPLOY_EVAZAN ->
                                    LOG.warn("V24.3 EVAZAN: Weapon partner in play — +150 deploy priority!");
                                case DEPLOY_WEAPON_CHARACTER ->
                                    LOG.warn("V24.3 WEAPON CHAR: Evazan in play — +100 deploy priority for {}!", card.getTitle());
                                case NONE -> { }
                            }
                        }
                    }

                    // Starships and vehicles for board presence
                    boolean v213StarshipOrVehicle = category == CardCategory.STARSHIP
                        || category == CardCategory.VEHICLE;
                    boolean v213ExecutorOrFlagship = cardTitleLower.contains("executor")
                        || cardTitleLower.contains("flagship");
                    boolean v213ObjectiveNeedsBespin = false;
                    boolean v213BespinOnTable = false;
                    boolean v213BespinPresence = false;
                    boolean v213OpponentAtBespin = false;
                    if (v213StarshipOrVehicle) {

                        // === V24.6A+V24.9: EXECUTOR DEPLOY PRIORITY ===
                        // Executor is THE key ship for TDIGWATT — it force drains at Bespin,
                        // enables Dark Deal + CC Occupation. If it's in hand, deploy it NOW.
                        // V24.9: MUST come out turn 1 or 2 at the latest. If AMSD didn't pull it
                        // from reserve, deploy it manually from hand — no excuses.
                        if (v213ExecutorOrFlagship) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer execObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (execObjAnalyzer != null && execObjAnalyzer.isAnalyzed()
                                && execObjAnalyzer.needsBespinSystemPresence()) {
                                v213ObjectiveNeedsBespin = true;

                                // V24.10: BESPIN MUST BE ON TABLE BEFORE EXECUTOR
                                // Executor needs to deploy TO Bespin system. If Bespin isn't on the
                                // table yet, deploying Executor sends it to Tatooine or another system
                                // where it's completely useless for TDIGWATT. HARD BLOCK until Bespin is out.
                                boolean bespinOnTable = false;
                                try {
                                    for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                                        if (loc != null && loc.getTitle() != null &&
                                            loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                            loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                            loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                            bespinOnTable = true;
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    LOG.debug("V24.10 Executor gate: Error checking Bespin: {}", e.getMessage());
                                }

                                if (!bespinOnTable) {
                                    LOG.warn("V24.10 EXECUTOR BLOCKED: {} in hand but Bespin not on table — CANNOT deploy to wrong system!", card.getTitle());
                                } else {
                                    int execTurn = context.getTurnNumber();
                                    if (execTurn <= 2) {
                                        LOG.warn("V24.9 EXECUTOR CRITICAL: {} on turn {} + Bespin on table — MAXIMUM priority (+800)!", card.getTitle(), execTurn);
                                    } else {
                                        LOG.warn("V24.6 EXECUTOR: {} in hand + Bespin on table — deploy priority (+800)!", card.getTitle());
                                    }
                                }
                                v213BespinOnTable = bespinOnTable;
                            }
                        }

                        // V22.5: BESPIN SYSTEM SHIP PRIORITY
                        // For objectives that reference Bespin/Cloud City (like TDIGWATT),
                        // having a ship at Bespin system is critical for enabling Dark Deal
                        // and Cloud City Occupation. Prioritize ship deployment if no ship there yet.
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer shipObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (shipObjAnalyzer != null && shipObjAnalyzer.isAnalyzed() && shipObjAnalyzer.needsBespinSystemPresence()) {
                            v213ObjectiveNeedsBespin = true;
                            boolean hasBespinPresence = false;
                            try {
                                for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                    if (loc != null && loc.getTitle() != null &&
                                        loc.getTitle().toLowerCase(Locale.ROOT).contains("bespin") &&
                                        loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                        loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                        String pid = context.getPlayerId();
                                        float ourSpacePower = context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, loc, pid, false, false);
                                        if (ourSpacePower > 0) hasBespinPresence = true;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                LOG.debug("Could not check Bespin presence: {}", e.getMessage());
                            }
                            v213BespinPresence = hasBespinPresence;
                            if (!hasBespinPresence) {
                                // V23: Check if opponent has presence at Bespin — contestation is even more urgent
                                boolean opponentAtBespin = false;
                                try {
                                    for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                        if (loc != null && loc.getTitle() != null &&
                                            loc.getTitle().toLowerCase(Locale.ROOT).contains("bespin") &&
                                            loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                            loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                            String oppId = context.getOpponentId();
                                            if (oppId != null) {
                                                float oppPower = context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                                    gameState, loc, oppId, false, false);
                                                opponentAtBespin = (oppPower > 0);
                                            }
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    LOG.debug("V23: Could not check opponent Bespin presence: {}", e.getMessage());
                                }

                                if (opponentAtBespin) {
                                    LOG.warn("V23 BESPIN CONTEST: {} gets +300 — opponent has presence at Bespin!", card.getTitle());
                                } else {
                                    LOG.warn("V23 BESPIN SHIP: {} gets +250 — no ship at Bespin system yet!", card.getTitle());
                                }
                                v213OpponentAtBespin = opponentAtBespin;
                            }
                        }
                    }

                    // === PILOT BONUS ===
                    boolean v213Pilot = AiCardHelper.isPilot(card);

                    // === V41.2: PIETT DEPLOY — HOLD FOR AMSD ===
                    // Piett is the matching pilot for Executor. He should NEVER deploy to ground
                    // when AMSD is on the table and Executor is still available — AMSD needs Piett
                    // IN HAND to fire. Deploying Piett to ground wastes the AMSD + Executor combo.
                    boolean v213ExecutorPilot = cardTitleLower.contains("piett")
                        || cardTitleLower.contains("gherant");
                    boolean v213DeployingAboardShip = false;
                    if (v213ExecutorPilot) {
                        v213DeployingAboardShip = actionLower.contains("aboard") || actionLower.contains("pilot")
                            || actionLower.contains("executor") || actionLower.contains("simultaneously");
                        if (!v213DeployingAboardShip) {
                            LOG.warn("V47 EXECUTOR PILOT GROUND BLOCK: {} — blocking ground deploy, pilots belong on ships!",
                                card.getTitle());
                        }
                    }

                    // === MATCHING PILOT CHECK ===
                    boolean v213MatchingAction = actionLower.contains("matching");
                    PolicyContributionLedger v213AssetTailLedger = new PolicyContributionLedger(
                        "deploy-asset-tail-" + actionId);
                    v213AssetTailLedger.register(DeployPilotShipPolicy.evaluateAssetTail(
                        new DeployPilotShipPolicy.AssetTailFacts(
                            actionId, card.getTitle(), v213StarshipOrVehicle,
                            v213ExecutorOrFlagship, v213ObjectiveNeedsBespin,
                            v213BespinOnTable, context.getTurnNumber(),
                            v213BespinPresence, v213OpponentAtBespin,
                            v213Pilot, v213ExecutorPilot,
                            v213DeployingAboardShip, v213MatchingAction)));
                    PolicyOperationAdapter.apply(action, v213AssetTailLedger);

                    PolicyContributionLedger strategicValueLedger =
                            new PolicyContributionLedger(
                                    (decisionId == null || decisionId.isBlank()
                                            ? "deploy-strategic-value"
                                            : decisionId + "-deploy-strategic-value")
                                            + "-" + actionId);
                    strategicValueLedger.register(
                            DeployCardValuePolicy.scoreStrategic(
                                    new DeployCardValueFacts.Strategic(
                                            actionId, needsReinforcement,
                                            lifeForce <= RandoConfig.CRITICAL_LIFE_FORCE)));
                    PolicyOperationAdapter.apply(action, strategicValueLedger);
                }
            } else {
                boolean earlyCardIsLocation = earlyCard != null
                        && earlyCard.getBlueprint() != null
                        && earlyCard.getBlueprint().getCardCategory()
                        == CardCategory.LOCATION;
                boolean deployLocationsPlanActive = false;
                int unknownTurn = 0;
                if (!earlyCardIsLocation && plan != null
                        && plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS
                        && !plan.isForceAllowExtras()) {
                    deployLocationsPlanActive = true;
                    unknownTurn = context.getTurnNumber();
                }
                if (earlyCardIsLocation) {
                    LOG.info("V29: Unknown to main lookup but earlyCard is LOCATION '{}' — allowing!",
                            earlyCard.getTitle());
                } else if (deployLocationsPlanActive && unknownTurn <= 1) {
                    LOG.warn("🚫 BLOCKING unknown card deploy during DEPLOY_LOCATIONS plan (turn 1)");
                } else if (deployLocationsPlanActive) {
                    LOG.info("V29.7: Unknown card during DEPLOY_LOCATIONS but turn {} — allowing with penalty",
                            unknownTurn);
                }
                DeployActionEnvelopePolicy.Evaluation unknownEnvelope =
                        DeployActionEnvelopePolicy.evaluateUnknown(
                                new DeployActionEnvelopeFacts.UnknownAction(
                                        actionId, earlyCardIsLocation,
                                        deployLocationsPlanActive,
                                        unknownTurn));
                PolicyContributionLedger unknownEnvelopeLedger =
                        new PolicyContributionLedger(
                                (decisionId == null || decisionId.isBlank()
                                        ? "deploy-unknown-envelope"
                                        : decisionId + "-deploy-unknown-envelope")
                                        + "-" + actionId);
                unknownEnvelopeLedger.register(unknownEnvelope.result());
                PolicyOperationAdapter.apply(action, unknownEnvelopeLedger);
                if (unknownEnvelope.adapterStep()
                        == DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION) {
                    actions.add(action);
                    continue;
                }
            }

            // === V67bk (Steve, 2026-05-11): V52 SPEND FORCE +300 REMOVED ===
            //
            // Old rule: when force pile > 3, every deployable card got +300
            // "deploy everything, don't hoard." Steve's complaint:
            //   "it sets him up for bad moves. Better to save force for
            //    interrupts, next turn, having force during opponent's turn
            //    to play interrupts."
            //
            // The +300 was overriding site-quality scoring, so Rando dumped
            // low-power chars into bad sites (e.g., Stormtrooper Patrol solo
            // at Guest Quarters across from a Jedi stack) just because force
            // was available. With this removed, weak-site deploys lose to
            // PASS naturally when no good destination exists, and saved
            // force is available for interrupts on the opponent's turn.
            //
            // V52 MOMENTUM (below) intentionally kept for now — Steve called
            // out the SPEND FORCE rule specifically. Revisit if same symptom.

            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer sequencingObjective =
                context.getObjectiveAnalyzer();
            boolean tdigwattPreFlip = sequencingObjective != null
                && sequencingObjective.isTdigwattPreFlip();
            String sequencingObjectiveTitle = sequencingObjective != null
                ? sequencingObjective.getObjectiveTitle() : null;
            boolean hiddenPath = sequencingObjectiveTitle != null
                && sequencingObjective.isAnalyzed()
                && sequencingObjectiveTitle.toLowerCase(Locale.ROOT).contains("hidden path");
            String sequencingCardTitle = card != null && card.getTitle() != null
                ? card.getTitle() : "";
            if (sequencingCardTitle.isEmpty() && cardTitleFromGemp != null) {
                sequencingCardTitle = cardTitleFromGemp;
            }
            boolean sequencingCharacter = card != null && card.getBlueprint() != null
                && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER;
            float sequencingAbility = 0.0f;
            if (sequencingCharacter && card.getBlueprint().hasAbilityAttribute()) {
                try {
                    Float ability = card.getBlueprint().getAbility();
                    sequencingAbility = ability != null ? ability : 0.0f;
                } catch (Exception ignored) {
                    sequencingAbility = 0.0f;
                }
            }
            boolean skywalkerSaga = false;
            if (context.getTurnNumber() <= 3) {
                try {
                    skywalkerSaga = DeploySequencingFactsReader.hasAnakinsFuneralPyre(gameState);
                } catch (Exception ignored) {
                    // Preserve the original fail-open script detection boundary.
                }
            }
            DeploySequencingPolicy.Evaluation tail = DeploySequencingPolicy.tailScripts(
                new DeploySequencingPolicy.TailFacts(
                    actionId, context.getTurnNumber(),
                    plan != null ? plan.getDeploymentsMade() : 0,
                    tdigwattPreFlip,
                    skywalkerSaga,
                    hiddenPath, sequencingCardTitle,
                    sequencingCardTitle.toLowerCase(Locale.ROOT), actionLower,
                    sequencingCharacter, sequencingAbility));
            PolicyContributionLedger tailLedger = new PolicyContributionLedger(
                (decisionId == null || decisionId.isBlank()
                    ? "deploy-tail" : decisionId + "-deploy-tail") + "-" + actionId);
            tailLedger.register(tail.result());
            PolicyOperationAdapter.apply(action, tailLedger);

            LOG.debug("[DeployEvaluator] Scored '{}' -> {} ({})",
                actionText.length() > 50 ? actionText.substring(0, 50) + "..." : actionText,
                String.format("%.1f", action.getScore()),
                action.getReasoningString());

            actions.add(action);
        }

        LOG.info("[DeployEvaluator] Evaluated {} deploy actions", actions.size());
        return actions;
    }

    private boolean applyRequiredCardInactivationVeto(
            DecisionContext context, EvaluatedAction action,
            String decisionId,
            String actionId, PhysicalCard card) {
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                objective = context.getObjectiveAnalyzer();
        SwccgGame game = context.getGame();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        if (objective == null || game == null || gameState == null
                || playerId == null || card == null) {
            return false;
        }
        boolean inactivatesRequiredCard =
                objective.wouldDeployPreventRequiredCardActivity(
                        game, playerId, card);
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                        (decisionId == null || decisionId.isBlank()
                            ? "deploy-objective-inactivation"
                            : decisionId + "-objective-inactivation")
                            + "-" + actionId);
        ledger.register(
                DeployObjectiveSitingPolicy
                    .blockRequiredCardInactivation(
                        actionId, inactivatesRequiredCard));
        PolicyOperationAdapter.apply(action, ledger);
        return inactivatesRequiredCard;
    }

    private void applySharedPolicy(EvaluatedAction action, String decisionId,
                                   String actionId, String owner,
                                   PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                (decisionId == null || decisionId.isBlank()
                        ? owner : decisionId + "-" + owner) + "-" + actionId);
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private static boolean isVader(
            SwccgGame game, GameState gameState,
            PhysicalCard card) {
        if (game == null || gameState == null || card == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            return com.gempukku.swccgo.filters.Filters.Vader
                    .accepts(
                            gameState,
                            game.getModifiersQuerying(),
                            card);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract card ID from action text (if present).
     */
    private String extractCardIdFromAction(String actionText) {
        if (actionText == null) return null;

        // Look for cardId pattern like "cardId='123'"
        Pattern pattern = Pattern.compile("cardId=['\"]?(\\d+)['\"]?");
        Matcher matcher = pattern.matcher(actionText);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Extract blueprint ID from action text HTML.
     * GEMP includes card hints in HTML like: <div class='cardHint' value='7_305'>•Card Name</div>
     *
     * Ported from Python deploy_evaluator.py _extract_blueprint_from_action
     */
    private String extractBlueprintFromActionHtml(String actionText) {
        if (actionText == null) return null;

        // Look for value='blueprint_id' pattern in HTML
        // Example: <div class='cardHint' value='7_305'>•OS-72-1</div>
        Pattern pattern = Pattern.compile("value=['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(actionText);
        if (matcher.find()) {
            String blueprintId = matcher.group(1);
            LOG.debug("[extractBlueprintFromActionHtml] Found blueprint '{}' in action text", blueprintId);
            return blueprintId;
        }

        return null;
    }

    /**
     * Try to find a card in hand that matches the action text.
     * NOTE: This is a fallback method - prefer using cardId lookup via gameState.findCardById()
     */
    private PhysicalCard findCardInHand(List<PhysicalCard> hand, String actionText) {
        if (hand == null || actionText == null) {
            return null;
        }

        String actionLower = actionText.toLowerCase(Locale.ROOT);

        for (PhysicalCard card : hand) {
            if (card == null) continue;

            // Match by title
            String title = card.getTitle();
            if (title != null) {
                String titleLower = title.toLowerCase(Locale.ROOT);
                if (actionLower.contains(titleLower)) {
                    LOG.debug("[findCardInHand] ✅ Found match: '{}' in action text", title);
                    return card;
                }
            }
        }

        // Only log failure at debug level - this method is a fallback and often won't find anything
        LOG.debug("[findCardInHand] No title match for action: '{}'",
            actionText.length() > 50 ? actionText.substring(0, 50) + "..." : actionText);

        return null;
    }

    /**
     * Count cards in play for a player.
     */
    private int countCardsInPlay(GameState gameState, String playerId) {
        if (gameState == null || playerId == null) return 0;

        int count = 0;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) continue;
            if (!playerId.equals(card.getOwner())) continue;
            if (card.getZone() == null || !card.getZone().isInPlay()) continue;

            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (blueprint == null) continue;

            CardCategory category = blueprint.getCardCategory();
            if (category == CardCategory.CHARACTER ||
                category == CardCategory.STARSHIP ||
                category == CardCategory.VEHICLE ||
                category == CardCategory.LOCATION) {
                count++;
            }
        }

        return count;
    }
}
