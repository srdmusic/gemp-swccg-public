package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy;
import com.gempukku.swccgo.ai.models.rando.strategy.CardKnowledge;
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
                // V79: track Verge of Greatness + Death Star state across the same scan
                boolean v79VergeActive = false;
                PhysicalCard v79DeathStar = null;
                boolean v79DeathStarAtScarif = false;
                for (PhysicalCard pCard : vaderCheckGs.getAllPermanentCards()) {
                    if (pCard == null || !vPlayerId.equals(pCard.getOwner())) continue;
                    com.gempukku.swccgo.common.Zone pZone = pCard.getZone();
                    if (pZone == null || !pZone.isInPlay()) continue;
                    if (pCard.getBlueprint() == null || pCard.getTitle() == null) continue;
                    String pTitle = pCard.getTitle().toLowerCase(Locale.ROOT);
                    // V79 detection: scan for Verge objective and Death Star (V)
                    if (pTitle.contains("on the verge of greatness")
                            || pTitle.contains("taking control of the weapon")) {
                        v79VergeActive = true;
                    }
                    // V79: detect Death Star (title only — (V) marker is Rarity not title)
                    if (pTitle.contains("death star")
                            && pCard.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
                        v79DeathStar = pCard;
                        // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681):
                        // getAtLocation() is ALWAYS null for the Death Star mobile-system LOCATION
                        // card, so this 1-Force move reserve fired EVERY turn forever — including
                        // while the DS was parked in Scarif orbit — and at 06:02 suppressed a real
                        // Mara Jade With Lightsaber deploy (cost 5, leaves 0) to hoard Force for a
                        // move the MoveEvaluator now vetoes post-flip (V79b FLIP-BACK GUARD). Use
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
                    if (pTitle.contains("vader") && pCard.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                        PhysicalCard vaderLoc = pCard.getAtLocation();
                        if (vaderLoc != null) {
                            // Check if there are opponents at Vader's current location
                            String vOppId = vaderCheckGame.getOpponent(vPlayerId);
                            boolean opponentsHere = false;
                            try {
                                float oppPower = vaderCheckGame.getModifiersQuerying().getTotalPowerAtLocation(
                                    vaderCheckGs, vaderLoc, vOppId, false, false);
                                opponentsHere = (oppPower > 0);
                            } catch (Exception e) { /* ignore */ }

                            if (!opponentsHere) {
                                // Vader is at a location with no opponents — needs to MOVE to fight
                                vaderMoveReserve = 2; // Reserve 2 force for movement (1-2 sites)
                                LOG.warn("V48 VADER MOVE RESERVE: Vader at {} with no opponents — reserving {} force for move!",
                                    vaderLoc.getTitle(), vaderMoveReserve);
                            }
                        }
                        // Don't break — let V79 scan continue
                    }
                }
                // V79: if Verge of Greatness + Death Star not yet at Scarif, reserve 1 Force
                if (v79VergeActive && v79DeathStar != null && !v79DeathStarAtScarif) {
                    v79VergeMoveReserve = 1;
                    LOG.warn("V79 VERGE MOVE RESERVE: Verge of Greatness active + Death Star not at Scarif — reserve 1 Force for Move phase");
                }
            } catch (Exception e) {
                LOG.debug("V48 VADER MOVE RESERVE: Error: {}", e.getMessage());
            }
        }

        // === V67z DEPLOY TRANSIT RESERVE (Steve, 2026-06): deploy-phase twin of the
        // DrawEvaluator V67z reserve. On Hidden Path (unflipped), each Jedi staged at
        // Mapuzo: Underground Corridor needs 1 Force in the MOVE phase to transit off
        // Mapuzo to a non-Mapuzo site (the objective's flip condition). V67z reserves it
        // in the DRAW phase, but DEPLOY runs before MOVE and was spending all Force on
        // Jedi Survivor / Jabiim deploys — the move phase had ~0 Force, so Rando never
        // transited and the objective never flipped (HIDDEN PATH CHARGE replay). Hold it
        // back here too (mirror of V48/V79). Capped at 3 (you only need ~2 transits to
        // flip; don't over-starve deploys).
        int v67zTransitReserve = 0;
        if (vaderCheckGs != null) {
            try {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v67zObj =
                    context.getObjectiveAnalyzer();
                if (v67zObj != null && v67zObj.isAnalyzed() && !v67zObj.isFlipped()
                        && v67zObj.getObjectiveTitle() != null
                        && v67zObj.getObjectiveTitle().toLowerCase(Locale.ROOT).contains("hidden path")) {
                    String v67zPid = context.getPlayerId();
                    int v67zCorridorJedi = 0;
                    for (PhysicalCard v67zLoc : vaderCheckGs.getLocationsInOrder()) {
                        if (v67zLoc == null || v67zLoc.getTitle() == null
                                || !v67zLoc.getTitle().toLowerCase(Locale.ROOT).contains("underground corridor")) continue;
                        for (PhysicalCard v67zC : vaderCheckGs.getCardsAtLocation(v67zLoc)) {
                            if (v67zC != null && v67zPid.equals(v67zC.getOwner())
                                    && v67zC.getBlueprint() != null
                                    && v67zC.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                v67zCorridorJedi++;
                            }
                        }
                    }
                    if (v67zCorridorJedi > 0) {
                        v67zTransitReserve = Math.min(v67zCorridorJedi, 3);
                        LOG.warn("V67z DEPLOY TRANSIT RESERVE: {} Jedi at Underground Corridor — hold {} Force in deploy for the move-phase transit off Mapuzo",
                            v67zCorridorJedi, v67zTransitReserve);
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

        // === STALE PLAN DETECTION ===
        // Check if available deploy actions match the plan
        // If none match and we have deploy actions, check WHY before marking stale
        if (plan != null && !plan.getInstructions().isEmpty() && !plan.isPlanComplete()) {
            boolean planCardsAvailable = false;
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
                    planCardsAvailable = true;
                    LOG.debug("   Found plan card {} in action: {}", bpId, actionText.substring(0, Math.min(60, actionText.length())));
                    break;
                }
            }

            if (!planCardsAvailable) {
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
        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle deploy-related actions (including persona replace)
            if (!actionLower.contains("deploy") && !actionLower.contains("persona replace")) {
                continue;
            }

            // Blocked-response gate: if the cancel-loop detector added this
            // actionId/actionText to the block set, hard-block here too.
            if (v159DeployBlocked != null && !v159DeployBlocked.isEmpty()
                    && (v159DeployBlocked.contains(actionId) || v159DeployBlocked.contains(actionText))) {
                EvaluatedAction blockedAct = new EvaluatedAction(actionId, ActionType.DEPLOY, -9999.0f, actionText);
                blockedAct.addReasoning("CANCEL-LOOP BLOCK: this action led to repeated Done-cancels — try something else", -9999.0f);
                LOG.warn("DeployEvaluator: actionId='{}' is in blockedResponses → -9999 (cancel-loop block)", actionId);
                actions.add(blockedAct);
                continue;
            }

            // V38.4: PERSONA REPLACE — usually BAD. Replacing Vader with a different
            // version puts the current one in Lost Pile (losing any attached weapons).
            if (actionLower.contains("persona replace")) {
                EvaluatedAction prAction = new EvaluatedAction(actionId, ActionType.DEPLOY, -500.0f, actionText);
                prAction.addReasoning("V38.4 PERSONA REPLACE: Loses armed character — blocked!", -500.0f);
                LOG.warn("V38.4 PERSONA REPLACE BLOCKED: '{}'", actionText);
                actions.add(prAction);
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.DEPLOY,
                50.0f,  // Base score
                actionText
            );

            // === V60 RESERVE DECK PULL GUARDS ===
            // FIXES Issue #B from peaceful-pike replay: Rando invoked "Deploy Tala Durith
            // from Reserve Deck" and "Deploy a Padawan" at force=0, search failed, opponent
            // saw Rando's entire Reserve Deck. NEVER invoke a Reserve pull unless:
            //   1. We can afford the deploy cost (tricky: unknown cost for "a Padawan")
            //   2. DeckOracle confirms a valid target exists (prevents reveal)
            //   3. This specific action hasn't failed 2x this game (shouldAvoidPulling)
            // For generic-target actions ("Deploy a Padawan"), guards #1-2 are best-effort.
            String v60ActionLower = actionText != null ? actionText.toLowerCase(Locale.ROOT) : "";
            boolean v60IsReservePull = v60ActionLower.contains("from reserve deck")
                || v60ActionLower.contains("[download]");
            if (v60IsReservePull) {
                // Guard: failed 2x — stop trying
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle v60Oracle = context.getDeckOracle();
                if (v60Oracle != null) {
                    String failKey = "action:" + actionText;
                    if (v60Oracle.shouldAvoidPulling(failKey)) {
                        action.addReasoning("V60 RESERVE FAIL-STOP: '" + actionText
                            + "' failed 2x — stop trying!", -9999.0f);
                        LOG.warn("V60 RESERVE FAIL-STOP: {} hard-blocked after 2+ failures", actionText);
                        actions.add(action);
                        continue;
                    }
                }
                // Guard: reserve deck critically small (reveal risk)
                GameState v60Gs = context.getGameState();
                if (v60Gs != null) {
                    try {
                        int v60ReserveSize = v60Gs.getReserveDeckSize(context.getPlayerId());
                        if (v60ReserveSize <= 2) {
                            action.addReasoning("V60 RESERVE RISK: " + v60ReserveSize
                                + " cards in Reserve — reveal almost the whole deck!", -9999.0f);
                            LOG.warn("V60 RESERVE RISK: {} blocked — only {} cards in reserve",
                                actionText, v60ReserveSize);
                            actions.add(action);
                            continue;
                        }
                    } catch (Exception e) { /* ignore */ }
                }
                // Guard: named target check — "Deploy [Name] from Reserve Deck"
                // Only blocks SPECIFIC MULTI-WORD proper-noun targets (e.g. "Tala Durith",
                // "Admiral Piett", "Padme Naberrie"). Generic placeholders like "card",
                // "a farm", "a Padawan" are NOT blocked — the game engine picks the actual
                // target from the card's filter list, and pulling for "any matching"
                // categories is still valuable even if our DeckOracle can't verify.
                // FIXES Issue from lft7u9prpd6q6r9v replay: Yarna's "Deploy card from
                // Reserve Deck" was hard-blocked every turn because regex extracted "card"
                // as a target name and DeckOracle couldn't find a card titled "card".
                // Case-sensitive regex — proper-noun targets start with uppercase.
                if (v60Oracle != null) {
                    java.util.regex.Matcher namedMatch = java.util.regex.Pattern.compile(
                        "Deploy ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) from Reserve Deck")
                        .matcher(actionText);
                    if (namedMatch.find()) {
                        String v60Target = namedMatch.group(1).trim();
                        if (!v60Oracle.hasTargetInReserve(v60Target.split(" "))) {
                            action.addReasoning("V60 RESERVE MISS: '" + v60Target
                                + "' not in Reserve — pull fails + reveals deck!", -9999.0f);
                            LOG.warn("V60 RESERVE MISS: {} not in reserve — hard-blocked", v60Target);
                            actions.add(action);
                            continue;
                        }
                    }
                }

                // Guard: generic category pull — "Deploy a farm from Reserve Deck",
                // "Deploy a Padawan from Reserve Deck". Regex: lowercase 'a'/'an' + noun.
                // If DeckOracle shows 0 cards match the keyword in Reserve, block.
                // FIXES Issue from lft7u9prpd6q6r9v replay: Rando fired IMBATS "Deploy a
                // farm from Reserve Deck" when LMF(V) was already in hand and no other
                // farms were in the deck. Search failed, revealed reserve to opponent.
                if (v60Oracle != null) {
                    java.util.regex.Matcher genMatch = java.util.regex.Pattern.compile(
                        "Deploy an? ([a-z][a-z ]*?) from Reserve Deck").matcher(actionText);
                    if (genMatch.find()) {
                        String v60Kw = genMatch.group(1).trim();
                        // V67bg (Steve, 2026-05-10): TYPE-AWARE pull validation.
                        //
                        // The old code substring-matched a generic noun ("location",
                        // "site", "weapon", "bay") against card TITLES. That always
                        // misses for category nouns because no card is literally
                        // titled "location" — the SWCCG vocabulary uses these words
                        // as TYPE indicators (CardCategory / CardSubtype / Icon /
                        // Keyword), not as titles. Symptom: Hunt Down's '[download]
                        // a Cloud City or Malachor battleground site' hard-blocked
                        // every turn. Same on IBS (docking bay).
                        //
                        // Fix: resolve the noun to a typed Filter via
                        // DeckOracle.resolveCommonNounToFilter(). The engine's own
                        // filter semantics then answer "is anything in reserve
                        // satisfying this filter?" — the same way the card's
                        // DeployCardFromReserveDeckEffect would search. Proper-noun
                        // targets like "Tala Durith" still hit the named-target
                        // matcher above (case-sensitive proper-noun regex).
                        //
                        // Memory: ~/.claude/projects/-Users-steve-gemp-swccg-public/
                        //   memory/feedback_card_search_by_type_not_text.md
                        com.gempukku.swccgo.filters.Filter v67bgTypedFilter =
                            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                .resolveCommonNounToFilter(v60Kw);
                        if (v67bgTypedFilter != null) {
                            // Type-aware reserve check using engine filter semantics.
                            boolean v67bgMatch = v60Oracle.hasFilterMatchInReserve(
                                context.getGame(), context.getPlayerId(), v67bgTypedFilter);
                            if (!v67bgMatch) {
                                action.addReasoning("V67bg RESERVE MISS (typed '" + v60Kw
                                    + "'): no card matching Filter in Reserve — pull will fail!",
                                    -9999.0f);
                                LOG.warn("V67bg RESERVE MISS: typed filter for '{}' has no match in reserve — hard-blocked",
                                    v60Kw);
                                actions.add(action);
                                continue;
                            } else {
                                LOG.warn("V67bg RESERVE OK: typed filter for '{}' has matches in reserve — pull valid",
                                    v60Kw);
                            }
                        } else if (v60Kw.length() >= 3 && !v60Oracle.hasTargetInReserve(v60Kw)) {
                            // Unknown noun — fall back to title-substring (legacy behavior).
                            // If this fires for a category noun, add it to
                            // DeckOracle.resolveCommonNounToFilter() and re-test.
                            action.addReasoning("V60 RESERVE MISS (generic, untyped): no '" + v60Kw
                                + "' in Reserve — pull fails + reveals deck!", -9999.0f);
                            LOG.warn("V60 RESERVE MISS (untyped): keyword '{}' not in reserve — hard-blocked (CONSIDER adding to resolveCommonNounToFilter)", v60Kw);
                            actions.add(action);
                            continue;
                        }
                    }
                }

                // V66 MEMORY AUDIT: Unified pull validation via DeckOracle.
                // Catches pulls that the named-target/generic regexes miss,
                // AND catches "WASTEFUL" pulls (target already in hand/play).
                // Steve's feedback: "Rando doesn't seem to remember what's in
                // his hand, force pile, reserve, used or lost pile."
                // This runs AFTER the older named/generic guards so those more
                // specific penalties still fire first.
                if (v60Oracle != null && v60Oracle.isAnalyzed()) {
                    com.gempukku.swccgo.common.Zone v66Zone =
                        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.parseSourceZone(actionText);
                    if (v66Zone != null) {
                        // Extract candidate target keyword(s) from action text.
                        // Prefer multi-word proper noun, fall back to generic "a X".
                        String[] v66Keywords = null;
                        java.util.regex.Matcher v66Named = java.util.regex.Pattern.compile(
                            "(?:Deploy|Take) ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) "
                                + "(?:from Reserve|from Lost|from Used|from Force|into hand from)")
                            .matcher(actionText);
                        if (v66Named.find()) {
                            v66Keywords = v66Named.group(1).trim().split(" ");
                        } else {
                            java.util.regex.Matcher v66Gen = java.util.regex.Pattern.compile(
                                "(?:Deploy|Take) an? ([a-z]+) (?:from|into hand from)")
                                .matcher(actionText);
                            if (v66Gen.find()) {
                                String kw = v66Gen.group(1).trim();
                                // V123-DEPLOY (Steve, 2026-05-22): V66 STOPWORD GUARD — same fix
                                // as ActionTextEvaluator V66 from earlier commit. This SECOND
                                // V66 block in DeployEvaluator was missed in the original V123,
                                // so the keyword-lookup version still fired here and
                                // hard-blocked Hunt Down V site pulls at -9999 even when
                                // V67bg correctly reported "RESERVE OK" with locations
                                // available. Replay 2026-05-22 confirmed: V67bg said locations
                                // are in reserve, V66 right after said "no match for 'location'"
                                // because no card is literally TITLED "location".
                                java.util.Set<String> v66Stopwords = new java.util.HashSet<>(java.util.Arrays.asList(
                                    "location", "site", "battleground", "system", "sector",
                                    "ship", "starship", "vehicle", "transport", "fighter",
                                    "weapon", "lightsaber", "blaster", "bowcaster", "device",
                                    "character", "alien", "droid", "jedi", "sith", "padawan",
                                    "inquisitor", "senator", "pilot", "warrior", "soldier",
                                    "leader", "admiral", "general", "trooper", "officer",
                                    "rebel", "imperial", "scout", "spy",
                                    "effect", "interrupt", "objective", "epic", "shield",
                                    "card"
                                ));
                                if (v66Stopwords.contains(kw.toLowerCase(java.util.Locale.ROOT))) {
                                    LOG.info("V123-DEPLOY V66 STOPWORD: '{}' is a generic category — skip V66 title lookup, defer to V67bg typed filter",
                                        kw);
                                } else if (kw.length() >= 3) {
                                    v66Keywords = new String[] { kw };
                                }
                            }
                        }
                        if (v66Keywords != null && v66Keywords.length > 0) {
                            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullValidation v66Result =
                                v60Oracle.validatePull(v66Zone, v66Keywords);
                            if (v66Result.outcome ==
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_FAIL) {
                                action.addReasoning("V66 MEMORY: " + v66Result.reason, -9999.0f);
                                LOG.warn("V66 MEMORY WILL_FAIL: '{}' — {}", actionText, v66Result.reason);
                                actions.add(action);
                                continue;
                            } else if (v66Result.outcome ==
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WASTEFUL) {
                                action.addReasoning("V66 MEMORY: " + v66Result.reason, -800.0f);
                                LOG.warn("V66 MEMORY WASTEFUL: '{}' — {} (-800)", actionText, v66Result.reason);
                            } else if (v66Result.outcome ==
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_SUCCEED) {
                                LOG.info("V66 MEMORY OK: {} — {}", actionText, v66Result.reason);
                            }
                        }

                        // V67h: When the action text is generic ("Choose card to deploy from
                        // Reserve Deck", "[Download] a matching weapon"), use the SOURCE CARD's
                        // game text to identify what filter the action targets. This catches
                        // the failures the regex-based V66 misses — e.g., Yarna's "[download]
                        // Arleil, Doallyn, Tessek, Wild Karrde, or a Tatooine battleground"
                        // when none of those is in Reserve.
                        // Steve's expectation: "Rando is already aware of what's in his deck
                        // at the start of game and would know when he would have a successful
                        // search."
                        try {
                            List<String> v67hCardIds = context.getCardIds();
                            String v67hCardIdStr = (v67hCardIds != null && i < v67hCardIds.size())
                                ? v67hCardIds.get(i) : null;
                            if (v67hCardIdStr != null && !v67hCardIdStr.isEmpty() && gameState != null) {
                                PhysicalCard v67hSrcCard =
                                    gameState.findCardById(Integer.parseInt(v67hCardIdStr));
                                if (v67hSrcCard != null && v67hSrcCard.getBlueprint() != null) {
                                    // BATCH1-CORR (2026-07-13, Codex m00229): side-aware owner — location
                                    // pull text lives in per-side getters, getGameText() alone is blind.
                                    String v67hGT = com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                        .getSourceCardFullGameText(v67hSrcCard.getBlueprint(), context.getSide());
                                    if (v67hGT != null) {
                                        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullValidation v67hResult =
                                            v60Oracle.validatePullFromSourceCard(v66Zone, v67hGT);
                                        if (v67hResult.outcome ==
                                            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_FAIL) {
                                            action.addReasoning("V67h MEMORY (game-text): " + v67hResult.reason, -9999.0f);
                                            LOG.warn("V67h MEMORY WILL_FAIL: source={} — {}",
                                                v67hSrcCard.getTitle(), v67hResult.reason);
                                            actions.add(action);
                                            continue;
                                        } else if (v67hResult.outcome ==
                                            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_SUCCEED) {
                                            LOG.info("V67h MEMORY OK: source={} — {}",
                                                v67hSrcCard.getTitle(), v67hResult.reason);
                                            // === V185 (Steve, 2026-06): WEAPON-DEPLOYABILITY GATE ===
                                            // V67h confirms the target is IN the Reserve Deck, but NOT that it
                                            // can be DEPLOYED. A Good Friend (225_37) deploys one of {location,
                                            // epic event, Leia's Lightsaber} from Reserve; once the location +
                                            // epic event are deployed, only the lightsaber remains — and a weapon
                                            // can only attach to the SPECIFIC characters its own matching-character
                                            // filter accepts (Leia's Lightsaber -> Leia/Ben Solo/Rey ability>4;
                                            // Anakin's Lightsaber -> Skywalker ability>3). If Rando has no such
                                            // character on the table the deploy has no legal target, so the pull
                                            // FAILS: wasted action + Reserve revealed/reshuffled. Steve: "no one was
                                            // on table that could hold a lightsaber, so the pull failed. If Rando had
                                            // waited for characters to deploy first he would have been successful.
                                            // This lost Rando the game." (Refined 2026-06-23: the first pass checked
                                            // "any character" — too crude; Rando can have bodies out yet none able to
                                            // hold THIS weapon. Now we read each weapon's own filter.) Block when
                                            // EVERY remaining Reserve target is a weapon with NO in-play character its
                                            // own filter accepts. A non-weapon target still pullable, a weapon with a
                                            // legal holder already down, or a game-text (Filters.none) weapon we can't
                                            // predict, all leave the pull untouched. -2000 matches V177's dead-pull
                                            // penalty (drops below the V60 +100 baseline; not the absolute -9999 of a
                                            // no-target search — a safety valve if this heuristic ever over-fires).
                                            List<String> v185Targets =
                                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                                    .parseSourceCardPullTargets(v67hGT);
                                            com.gempukku.swccgo.game.SwccgGame v185Game = context.getGame();
                                            if (v185Game != null
                                                    && v60Oracle.reserveTargetsAreAllUnattachableWeapons(
                                                        v185Game, context.getPlayerId(), v185Targets)) {
                                                action.addReasoning("V185 WEAPON, NO LEGAL HOLDER: every Reserve-Deck target left for '"
                                                    + actionText + "' is a weapon Rando has no in-play character to hold (per the weapon's own deploy filter) — deploy a valid character first",
                                                    -2000.0f);
                                                LOG.warn("V185 WEAPON-NO-HOLDER blocked: source={} targets={} — all weapons, no legal in-play holder",
                                                    v67hSrcCard.getTitle(), v185Targets);
                                                actions.add(action);
                                                continue;
                                            }
                                            // === V190 (Steve, 2026-07-04): STARSHIPS DEPLOY TO SYSTEMS ===
                                            // "He should not have deployed starships to a docking bay.
                                            // Only deploy starships to systems." Game 20jqtseod148of4y:
                                            // Court Of The Vile Gangster's pull fetched Elis In Hinthra
                                            // (then Dengar In Punishing One) and parked them at Executor:
                                            // Docking Bay at 0 power — the 4 Force it burned starved the
                                            // V29 PAIRED buddy deploy the same turn. When every fetchable
                                            // Reserve target left for this pull is a STARSHIP and no
                                            // space location is on table, the ship can only park at a
                                            // site: block the pull until a system/sector lands. Known
                                            // limits (see AI_CHANGELOG 2026-07-04): a space location the
                                            // ship can't legally deploy to still stands the gate down,
                                            // and the gate does not itself make Rando deploy the system
                                            // first (follow-up item). Boundary: the continue skips this
                                            // evaluator's later bonuses (V60 +100, V38.4, V100 +1500,
                                            // V67ai +2000), so the blocked action lands ~-6400 vs the
                                            // -100 viability floor — dominated, and no other rule reads
                                            // this action after a continue.
                                            if (v60Oracle.reservePullFetchesOnlyStarships(v67hGT)
                                                    && !com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
                                                        .spaceLocationOnTable(gameState)) {
                                                action.addReasoning("V190 STARSHIP PULL, NO SPACE LOCATION ON TABLE: every Reserve target left for '"
                                                    + actionText + "' is a starship and there is no system to deploy it to — it would park at a docking bay at 0 power; deploy a system first",
                                                    -12000.0f);
                                                LOG.warn("V190 STARSHIP-NO-SYSTEM blocked: source={} — starship-only fetch, no space location on table (-12000)",
                                                    v67hSrcCard.getTitle());
                                                actions.add(action);
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException nfe) { /* ignore */ }
                        catch (Exception e) { LOG.debug("V67h: error: {}", e.getMessage()); }
                    }
                }

                // Passed all guards — V192 (2026-07-06, T4.2 pull-engine merge): the +100
                // baseline is ABSORBED by the single V192 pull scorer in ActionTextEvaluator
                // (ds-3 same-tag drift: this DE twin used +100 while the ATE arm used +150 —
                // ONE baseline lives in the scorer now, and CombinedEvaluator's additive
                // merge means this line double-counted on every pull). The guards above
                // (V60 FAIL-STOP/RISK/MISS, V67bg, V66, V67h, V185, V190) STAY as vetoes —
                // duplicate -9999s are harmless. Superseded +100 baseline removed 2026-07-13; see git.
                LOG.info("V60 RESERVE PULL guards passed for '{}' — baseline owned by V192 (ActionTextEvaluator)", actionText);
            }

            // === V38.4 + V56 FIX 18: AGGRESSIVE DEPLOY — HAND SIZE + FORCE PILE URGENCY ===
            // Cards in hand do NOTHING. Cards on table drain/battle/occupy.
            // The more cards in hand and Force available, the more urgently we must deploy.
            // This counteracts the many -200 to -600 penalties that stack up and cause
            // Rando to pass with Force available and cards in hand.
            //
            // V56: Closed the mid/late-game urgency gap. Previously handSize < 9 gave
            // ZERO urgency bonus, so once we emptied our hand to ~8 cards, scores
            // crashed and Rando stopped deploying (see the "activated 8 force, deployed
            // nothing" pattern on Turn 8). Now there is a baseline floor any time we
            // have force to spend.
            {
                int handSize = hand != null ? hand.size() : 0;
                float urgencyBonus = 0;

                // Scale with hand size: bigger hand = more urgency to deploy
                if (handSize >= 12) {
                    urgencyBonus = 200.0f + (handSize - 12) * 50.0f; // +200 at 12, +250 at 13, +300 at 14...
                } else if (handSize >= 9) {
                    urgencyBonus = 100.0f + (handSize - 9) * 30.0f; // +100 at 9, +130 at 10, +160 at 11
                } else if (handSize >= 5) {
                    // V56: mid-hand baseline — still incentivize deploying
                    urgencyBonus = 80.0f;
                } else if (handSize >= 1) {
                    // V56: small-hand baseline — always deploy if we have anything left
                    urgencyBonus = 50.0f;
                }

                // Scale with Force available: more Force = less reason to hoard
                if (availableForce >= 10 && handSize >= 8) {
                    urgencyBonus += 100.0f; // Surplus Force — spend it!
                }
                // V56: even with small hand, if force is sitting unused, push deploys
                if (availableForce >= 6 && handSize >= 1 && handSize < 8) {
                    urgencyBonus += 80.0f;
                }

                if (urgencyBonus > 0) {
                    action.addReasoning(String.format(
                        "V38.4 DEPLOY URGENCY: hand=%d, force=%d — get cards on table! (+%.0f)",
                        handSize, availableForce, urgencyBonus), urgencyBonus);
                    LOG.warn("V38.4 DEPLOY URGENCY: hand={}, force={} — boost +{} (action='{}')",
                        handSize, availableForce, (int)urgencyBonus, actionText);
                }
            }

            // === V169 (Steve, 2026-06): PROTECT URGENT — allies endangered, MUST deploy ===
            // Replay lk6xgsokjcwrwxuu fatal move 1: on the turn Asajj stood alone at Guest
            // Quarters with Luke AT her site, the lone 'Deploy' action scored -140 (V64
            // maintenance penalty for Ap'lek dominated the urgency bonuses), fell below the
            // DPS bad-action threshold (-100), and the ENTIRE deploy phase was passed — with
            // 13 Force activated and Mara Jade in hand. Asajj was beaten 6v27 next turn.
            // When ANY location has our characters outpowered, the 'Deploy' umbrella action
            // gets +500 so maintenance/value worries can never sink the phase below the DPS
            // threshold; the location chooser (V169 PROTECT in CardSelectionEvaluator) then
            // steers the deploy to the endangered site.
            try {
                com.gempukku.swccgo.game.state.GameState v169Gs = context.getGameState();
                com.gempukku.swccgo.game.SwccgGame v169Game = context.getGame();
                String v169Pid = context.getPlayerId();
                if (v169Gs != null && v169Game != null && v169Pid != null) {
                    String v169Opp = v169Gs.getOpponent(v169Pid);
                    for (PhysicalCard v169Loc : v169Gs.getTopLocations()) {
                        if (v169Loc == null) continue;
                        boolean v169WeHere = false;
                        for (PhysicalCard v169C : v169Gs.getCardsAtLocation(v169Loc)) {
                            if (v169C != null && v169Pid.equals(v169C.getOwner()) && !v169C.isUndercover()) {
                                v169WeHere = true; break;
                            }
                        }
                        if (!v169WeHere) continue;
                        float v169Our = v169Game.getModifiersQuerying().getTotalPowerAtLocation(
                            v169Gs, v169Loc, v169Pid, false, false);
                        float v169Their = v169Game.getModifiersQuerying().getTotalPowerAtLocation(
                            v169Gs, v169Loc, v169Opp, false, false);
                        if (v169Their > v169Our) {
                            action.addReasoning(String.format(
                                "V169 PROTECT URGENT: our characters at %s outpowered (%.0f vs %.0f) — deploy buddies NOW",
                                v169Loc.getTitle(), v169Our, v169Their), 500.0f);
                            LOG.warn("V169 PROTECT URGENT: {} ({} vs {}) -> +500 on 'Deploy'",
                                v169Loc.getTitle(), (int) v169Our, (int) v169Their);
                            break;
                        }
                    }
                }
            } catch (Exception v169E) { LOG.debug("V169 umbrella error: {}", v169E.getMessage()); }

            // === V176 (Steve, 2026-06): SAVE THE BATTLE-INITIATION FORCE ===
            // Replay c8o8f5pnjp5244ao turn 5: Rando deployed Tyranus + Evazan&Ponda +
            // Dooku's Lightsaber onto SOLO Yoda — then could not battle him: the deploys
            // spent the last force, battle initiation costs 1, and the engine never
            // offered "Initiate battle" (BattleEvaluator saw 0 battle actions). Steve
            // reinforced next turn and the free kill became a 4-character brawl.
            // When a WINNABLE battle is already on the table (we are present and
            // out-power them at a shared location) and the force pile is nearly dry
            // (<= 2), STOP deploying — keep the initiation fee. -800 dominates the
            // urgency/momentum bonuses; the battle phase comes right after deploy.
            try {
                com.gempukku.swccgo.game.state.GameState v176Gs = context.getGameState();
                com.gempukku.swccgo.game.SwccgGame v176Game = context.getGame();
                String v176Pid = context.getPlayerId();
                if (v176Gs != null && v176Game != null && v176Pid != null
                        && v176Gs.getForcePileSize(v176Pid) <= 2) {
                    String v176Opp = v176Gs.getOpponent(v176Pid);
                    for (PhysicalCard v176Loc : v176Gs.getTopLocations()) {
                        if (v176Loc == null) continue;
                        boolean v176We = false, v176They = false;
                        for (PhysicalCard v176C : v176Gs.getCardsAtLocation(v176Loc)) {
                            if (v176C == null) continue;
                            if (v176Pid.equals(v176C.getOwner()) && !v176C.isUndercover()) v176We = true;
                            else if (v176Opp.equals(v176C.getOwner())) v176They = true;
                        }
                        if (!v176We || !v176They) continue;
                        float v176Our = v176Game.getModifiersQuerying().getTotalPowerAtLocation(
                            v176Gs, v176Loc, v176Pid, false, false);
                        float v176Their = v176Game.getModifiersQuerying().getTotalPowerAtLocation(
                            v176Gs, v176Loc, v176Opp, false, false);
                        if (v176Our > v176Their) {
                            action.addReasoning(String.format(
                                "V176 SAVE BATTLE FORCE: winnable battle waiting at %s (%.0f vs %.0f) and only %d force left — stop deploying, keep the initiation fee",
                                v176Loc.getTitle(), v176Our, v176Their,
                                v176Gs.getForcePileSize(v176Pid)), -800.0f);
                            LOG.warn("V176 SAVE BATTLE FORCE: {} ({} vs {}) pile={} -> -800 on further deploys",
                                v176Loc.getTitle(), (int) v176Our, (int) v176Their,
                                v176Gs.getForcePileSize(v176Pid));
                            break;
                        }
                    }
                }
            } catch (Exception v176E) { LOG.debug("V176 error: {}", v176E.getMessage()); }

            // === APPLY PHASE-LEVEL PLAN ===
            // V24.10: NEVER hold back on turns 1-2. The engine MUST be built ASAP:
            //   Locations → AMSD (Piett + Executor) → Lando/Lobot → everything else.
            // Holding back early wastes critical setup turns.
            // After turn 2, HOLD_BACK can apply to non-location cards only.
            // Locations are ALWAYS exempt from HOLD_BACK regardless of turn.
            if (plan != null && plan.getStrategy() == DeployStrategy.HOLD_BACK) {
                // V40: HOLD_BACK only applies to TDIGWATT (non-Hunt Down) decks.
                // Hunt Down and all other decks deploy freely — no hold back ever.
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer holdBackObjAnalyzer =
                    context.getObjectiveAnalyzer();
                boolean isHuntDownHoldBack = holdBackObjAnalyzer != null && holdBackObjAnalyzer.isAnalyzed()
                    && holdBackObjAnalyzer.isHuntDownV();
                // V40: Only apply hold-back for TDIGWATT (non-Hunt Down). All others deploy freely.
                boolean isTdigwattDeck = holdBackObjAnalyzer != null && holdBackObjAnalyzer.isAnalyzed()
                    && !holdBackObjAnalyzer.isHuntDownV();
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
            if (cardTitleFromGemp == null && cardIdStr != null && !cardIdStr.isEmpty() && gameState != null) {
                try {
                    earlyCard = gameState.findCardById(Integer.parseInt(cardIdStr));
                    if (earlyCard != null && earlyCard.getTitle() != null) {
                        cardTitleFromGemp = earlyCard.getTitle();
                        LOG.info("V29 EARLY LOOKUP: Resolved bare 'Deploy' via cardId {} → '{}'", cardIdStr, cardTitleFromGemp);
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
            if (CardKnowledge.shouldBlockDeployment(titleForRestrictionCheck, currentTurn)) {
                LOG.warn("🚫 BLOCKING turn-1 deploy of Effect '{}' (turn {})", titleForRestrictionCheck, currentTurn);
                action.addReasoning("BLOCKED: Do not deploy this Effect on turn 1", -9999.0f);
                actions.add(action);
                continue;
            }

            // === LOCATION DEPLOYMENT - Highest Priority ===
            // Deploying locations opens up deployment options
            if (actionLower.contains("location") || actionLower.contains("site") || actionLower.contains("system")) {
                action.addReasoning("LOCATION - deploy first!", 200.0f);

                // === V24.10: EXTRA LOCATION PRIORITY WHEN PIETT NEEDS FINDING ===
                // If Piett is stuck in the force pile, deploying more locations means
                // more force generation → bigger force pile → draw through faster to find him.
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle locOracle = context.getDeckOracle();
                if (locOracle != null && locOracle.isAnalyzed()) {
                    boolean piettAccessible = locOracle.isCardInHand("Admiral Piett") || locOracle.isCardInHand("Piett")
                        || locOracle.isCardInReserve("Admiral Piett") || locOracle.isCardInReserve("Piett")
                        || locOracle.isCardInPlay("Admiral Piett") || locOracle.isCardInPlay("Piett");
                    boolean piettLost = locOracle.isCardLost("Admiral Piett") || locOracle.isCardLost("Piett");
                    if (!piettAccessible && !piettLost && context.getTurnNumber() <= 4) {
                        action.addReasoning("V24.10 PIETT MISSING: Deploy locations to generate force — need to draw for Piett!", 150.0f);
                        LOG.warn("V24.10 PIETT DIG: Piett not accessible — extra location deploy priority (+150) to power force pile draws!");
                    }
                }

                // === V23: BESPIN SYSTEM EARLY DEPLOY PRIORITY ===
                // For TDIGWATT, Bespin system is the FOUNDATION of the entire objective.
                // Without Bespin on table, nothing works: no Dark Deal, no CC Occupation,
                // no AMSD deploy target. Deploy it IMMEDIATELY on turns 1-3.
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer bespinObjAnalyzer =
                    context.getObjectiveAnalyzer();
                int turnNum = context.getTurnNumber();
                if (bespinObjAnalyzer != null && bespinObjAnalyzer.isAnalyzed()
                    && bespinObjAnalyzer.needsBespinSystemPresence() && turnNum <= 3) {
                    // Check if this action deploys Bespin system specifically
                    boolean isBespinDeploy = actionLower.contains("bespin");
                    if (!isBespinDeploy && cardTitleFromGemp != null) {
                        isBespinDeploy = cardTitleFromGemp.toLowerCase(Locale.ROOT).contains("bespin");
                    }
                    if (isBespinDeploy) {
                        // Check Bespin isn't already on table
                        boolean bespinOnTable = false;
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
                        if (!bespinOnTable) {
                            // V24.15: Mega-boost on Turn 1 — Bespin MUST be absolute first deploy!
                            float bespinBoost = (turnNum <= 1) ? 800.0f : 400.0f;
                            action.addReasoning("V24.15 BESPIN PRIORITY: Deploy Bespin system FIRST — objective foundation!", bespinBoost);
                            LOG.warn("V24.15 BESPIN PRIORITY: Bespin system deploy gets +{} on turn {} — MUST deploy ASAP!", bespinBoost, turnNum);
                        }
                    }
                }

                actions.add(action);
                continue;
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
                    && bespinFirstAnalyzer.needsBespinSystemPresence()) {
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

                        // V29: If we resolved the card, check its category directly
                        boolean isLocationByCategory = false;
                        boolean isShipByCategory = false;
                        if (earlyCard != null && earlyCard.getBlueprint() != null) {
                            CardCategory earlyCategory = earlyCard.getBlueprint().getCardCategory();
                            isLocationByCategory = (earlyCategory == CardCategory.LOCATION);
                            com.gempukku.swccgo.common.CardSubtype earlySub = earlyCard.getBlueprint().getCardSubtype();
                            isShipByCategory = (earlyCategory == com.gempukku.swccgo.common.CardCategory.STARSHIP)
                                || (earlyCategory == com.gempukku.swccgo.common.CardCategory.VEHICLE);
                        }

                        // Exempt: location/site/system deploys (we WANT locations)
                        boolean isLocationDeploy = isLocationByCategory
                            || guardCheckText.contains("location") || guardCheckText.contains("site")
                            || guardCheckText.contains("system");
                        // Exempt: AMSD — this is HOW we get Executor out
                        boolean isAmsdAction = guardCheckText.contains("alert my star destroyer")
                            || guardCheckText.contains("amsd");
                        // Exempt: Executor deploy (the ship we need at Bespin)
                        boolean isExecutorDeploy = guardCheckText.contains("executor");
                        // Exempt: starship/vehicle deploys (ships go to Bespin)
                        boolean isShipDeploy = isShipByCategory
                            || guardCheckText.contains("starship") || guardCheckText.contains("capital")
                            || guardCheckText.contains("star destroyer");
                        // Exempt: Bespin system itself (we need to deploy it!)
                        boolean isBespinDeploy = guardCheckText.contains("bespin");

                        if (!isLocationDeploy && !isAmsdAction && !isExecutorDeploy && !isShipDeploy && !isBespinDeploy) {
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
                            boolean bfGateReleased = false;
                            String bfGateReleaseReason = null;
                            if (bespinFirstAnalyzer.objectiveForbidsDeployingExecutor()) {
                                bfGateReleased = true;
                                bfGateReleaseReason = "objective game text forbids deploying Executor";
                            } else {
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle bfOracle = context.getDeckOracle();
                                if (bfOracle != null && bfOracle.isAnalyzed()) {
                                    boolean bfCapitalAccessible = false;
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
                                    if (!bfCapitalAccessible) {
                                        bfGateReleased = true;
                                        bfGateReleaseReason = "no capital starship in hand/reserve/force/used — no live path to occupy Bespin space";
                                    }
                                }
                            }

                            if (bfGateReleased) {
                                LOG.info("V29 BESPIN-FIRST RELEASED: NOT blocking deploy '{}' on turn {} — {}",
                                    actionText.length() > 60 ? actionText.substring(0, 60) : actionText, bfTurn, bfGateReleaseReason);
                            } else {
                                // Original V29 penalty (unchanged) — still fires for classic TDIGWATT.
                                action.addReasoning(
                                    "V29 BESPIN-FIRST: Executor MUST deploy before characters! " +
                                    "Get Bespin → Executor/AMSD → THEN characters.", -500.0f);
                                LOG.warn("V29 BESPIN-FIRST: BLOCKING deploy '{}' on turn {} — Bespin not occupied, deploy Executor first!",
                                    actionText.length() > 60 ? actionText.substring(0, 60) : actionText, bfTurn);
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
                            for (com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.ScoreNote note
                                    : objDeploy.getDeployObjectiveAdjustments(
                                        game, gameState, playerId, card, blueprint, actionText)) {
                                action.addReasoning(note.reason, note.score);
                            }
                        }
                    }


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
                                    action.addReasoning(
                                        "V89 DR. EVAZAN: '" + cardTitleForEvazan
                                            + "' deploying to '" + evazanTargetLoc.getTitle()
                                            + "' with no armed friend — block (will get sniped)",
                                        -1500.0f);
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
                            boolean v136ObjRelevant = v136Obj != null && v136Obj.isAnalyzed()
                                && v136Candidate.getTitle() != null
                                && v136Obj.isObjectiveRelevantLocation(v136Candidate, game, playerId);
                            float v136Score = com.gempukku.swccgo.ai.models.common.strategy
                                .CharacterDeploySiteEvaluator.evaluateSite(
                                    game, card, v136Candidate, playerId,
                                    v136ObjRelevant,
                                    v136Hand,
                                    v136ForceAvail,
                                    v136Turn,
                                    0 /* deckShipCount — TODO wire */,
                                    false /* perSiteEffectActive — TODO wire */);
                            if (v136Score != 0f) {
                                action.addReasoning(
                                    "V136 unified deploy-site score → " + v136Candidate.getTitle()
                                        + ": " + v136Score,
                                    v136Score);
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
                                    boolean v193AlreadyControls =
                                        com.gempukku.swccgo.cards.GameConditions.controls(
                                            game, playerId, v136Candidate);
                                    // The flip-gate CARD (whose deploy needs this control) comes from
                                    // the objective logic too — no card name hardcoded here, so this
                                    // steer generalizes to any occupation objective the analyzer flags.
                                    String v193GateCard = v136Obj.getFlipCriticalControlCard();
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
                                    if (!v193AlreadyControls && v193HoldsGateCard) {
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
                                        action.addReasoning(
                                            "V193 FLIP-GATE CONTROL: steer one body to '"
                                                + v136Candidate.getTitle()
                                                + "' to enable '" + v193GateCard + "' (objective flip gate)",
                                            v193Bonus);
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
                                if (opponentPower > 0) {
                                    float diff = friendlyPower - opponentPower;
                                    if (diff >= -10f && diff <= 10f) {
                                        // Close battle. Adding more = overflow advantage.
                                        action.addReasoning(String.format(
                                            "V96 CONCENTRATE: %s contested (us %.0f vs them %.0f) — pile on for overflow battle damage!",
                                            v96TargetLoc.getTitle(), friendlyPower, opponentPower), 500.0f);
                                        LOG.warn("V96 CONCENTRATE: {} → {} (us={} them={}) +500",
                                            card.getTitle(), v96TargetLoc.getTitle(),
                                            (int)friendlyPower, (int)opponentPower);
                                    } else if (diff > 10f) {
                                        // Already crushing. Small bonus, still better than spreading away.
                                        action.addReasoning(String.format(
                                            "V96 CONCENTRATE: %s contested, already winning by %.0f — finish them",
                                            v96TargetLoc.getTitle(), diff), 100.0f);
                                    }
                                    // diff < -10 (we're badly behind) → no V96 bonus.
                                    // Other rules handle retreat decisions.
                                }
                                // opponentPower == 0 → uncontested, no V96 bonus. (Comment corrected
                                // 2026-07-06: V67al is DEAD; V136 §B's uncontested over-stack penalty
                                // owns this case.)
                            }
                        } catch (Exception e) {
                            LOG.debug("V96 CONCENTRATE: error: {}", e.getMessage());
                        }
                    }

                    // === DEPLOYMENT PLAN SCORING ===
                    // If we have a plan, score based on whether this card is in the plan
                    String blueprintId = card.getBlueprintId(true);

                    if (plan != null) {
                        if (!plan.getInstructions().isEmpty()) {
                            // Plan has pending instructions - check if this card is in plan
                            DeploymentInstruction instruction = plan.getInstructionForPhysicalCard(
                                card.getPermanentCardId(), card.getCardId(), blueprintId);

                            if (instruction != null) {
                                // Card is in plan - high priority!
                                action.addReasoning("IN DEPLOYMENT PLAN: " + plan.getStrategy().getValue(), 100.0f);

                                // Extra bonus based on instruction priority
                                int priority = instruction.getPriority();
                                if (priority <= 1) {
                                    action.addReasoning("Highest priority deployment", 50.0f);
                                } else if (priority <= 3) {
                                    action.addReasoning("High priority deployment", 25.0f);
                                }
                            } else if (!plan.isForceAllowExtras()) {
                                // Card is NOT in plan and we're not allowing extras
                                // CRITICAL: If plan is DEPLOY_LOCATIONS, block NON-LOCATION cards.
                                // But actual LOCATION cards (like Bespin from hand) should still be allowed!
                                if (plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS) {
                                    // V24.10 FIX: Check if card IS a location before blocking!
                                    // Bespin can be in hand (from objective pull) but not in the plan.
                                    CardCategory planCheckCategory = blueprint.getCardCategory();
                                    if (planCheckCategory == CardCategory.LOCATION) {
                                        LOG.warn("📋 V24.10: {} is a LOCATION not in plan — ALLOWING during DEPLOY_LOCATIONS (locations always welcome!)", card.getTitle());
                                        action.addReasoning("V24.10: Location not in plan but DEPLOY_LOCATIONS allows all locations!", 100.0f);
                                    } else if (context.getTurnNumber() >= 2) {
                                        // V29.7 SAFETY VALVE: After turn 1, allow freely.
                                        LOG.info("V29.7: DEPLOY_LOCATIONS turn {} — allowing character deploy: {}", context.getTurnNumber(), card.getTitle());
                                        action.addReasoning("V40: DEPLOY_LOCATIONS incomplete but turn " + context.getTurnNumber() + " — deploy freely!", 0.0f);
                                    } else {
                                        // V40: Turn 1 DEPLOY_LOCATIONS block only for TDIGWATT
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer dlObjAnalyzer =
                                            context.getObjectiveAnalyzer();
                                        boolean isTdigwattDL = dlObjAnalyzer != null && dlObjAnalyzer.isAnalyzed()
                                            && !dlObjAnalyzer.isHuntDownV();
                                        if (isTdigwattDL) {
                                            LOG.warn("V40 BLOCKING non-location deploy during DEPLOY_LOCATIONS plan (turn 1, TDIGWATT): {}", card.getTitle());
                                            action.addReasoning("BLOCKED: Plan is DEPLOY_LOCATIONS ONLY (turn 1, TDIGWATT) - deploy locations first!", -1000.0f);
                                            actions.add(action);
                                            continue;  // Skip all other scoring - this action is blocked
                                        } else {
                                            LOG.warn("V40: DEPLOY_LOCATIONS turn 1 but NOT TDIGWATT — allowing deploy: {}", card.getTitle());
                                            action.addReasoning("V40: Not TDIGWATT — deploy freely on turn 1!", 0.0f);
                                        }
                                    }
                                } else if (plan.isWaitingForPlannedCards()) {
                                    // V38.4: Plan cards in hand but not affordable.
                                    // OLD: Hard blocked all deploys (-200) to save force for plan.
                                    // NEW: Only block if Force is actually tight (< 8).
                                    // With 13+ Force, we can afford BOTH plan AND extra deploys.
                                    // Rando was hoarding 14 Force and deploying NOTHING.
                                    if (availableForce < 8) {
                                        LOG.warn("📋 Low force ({}) — saving for planned cards: {}", availableForce, card.getTitle());
                                        action.addReasoning("V40: Saving force for planned cards (neutral)", 0.0f);
                                        actions.add(action);
                                        continue;
                                    } else {
                                        LOG.warn("V38.4 FORCE SURPLUS: {} Force — allow off-plan deploy: {}", availableForce, card.getTitle());
                                        action.addReasoning("V40: Plenty of Force — deploy off-plan!", 0.0f);
                                    }
                                } else {
                                    action.addReasoning("V40: Not in deployment plan (neutral)", 0.0f);
                                }
                            } else {
                                // V29.7 FIX: Plan allows extras (stale plan). When the plan is stale,
                                // the planned locations are no longer available — we MUST allow character
                                // deploys! Previously this blocked characters with -1000 even when stale,
                                // which caused Rando to deploy NOTHING for entire games.
                                // DEPLOY_LOCATIONS means "locations FIRST" not "locations ONLY."
                                // When plan is stale, the "first" part is done (or impossible).
                                if (plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS) {
                                    LOG.info("V29.7: Stale DEPLOY_LOCATIONS plan — allowing character/ship deploys now");
                                    action.addReasoning("V29.7: Stale plan — deploy characters now!", 10.0f);
                                } else {
                                    action.addReasoning("Extra deploy (plan stale)", 0.0f);
                                }
                            }
                        } else if (plan.isPlanComplete()) {
                            // Plan is complete! All planned deployments are done.
                            // CRITICAL FIX: DEPLOY_LOCATIONS means "deploy locations FIRST", not "ONLY locations"
                            // Once locations are deployed, we should allow character/ship deploys normally.
                            // The strategy was just to ensure locations came first to open deployment options.
                            if (plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS) {
                                LOG.info("✅ DEPLOY_LOCATIONS plan complete - now allowing character/ship deploys");
                                action.addReasoning("DEPLOY_LOCATIONS complete - extra deploy allowed", 25.0f);
                            }

                            // For all strategies (DEPLOY_LOCATIONS, ESTABLISH, REINFORCE), allow extra deploys
                            int extraBudget = plan.getExtraForceBudget(availableForce);
                            if (extraBudget > 0) {
                                action.addReasoning("Plan COMPLETE - extra deploy allowed", 25.0f);
                            } else {
                                // Saving force for battle
                                action.addReasoning("V40: Plan complete — deploy freely!", 0.0f);
                            }
                        }

                        // Check if this card is in hold-back list
                        if (blueprintId != null && plan.getHoldBackCards().contains(blueprintId)) {
                            // V40: Hold-back card penalty neutralized
                            action.addReasoning("V40: Hold-back card (neutral)", 0.0f);
                        }
                    }

                    // Get card stats (with type checks for safety)
                    // DeployCost exists on most deployable cards
                    int cost = 0;
                    try {
                        Float deployCost = blueprint.getDeployCost();
                        cost = deployCost != null ? deployCost.intValue() : 0;
                        // V22.3: Maintenance card check - need enough Force for upkeep AFTER deploying
                        // T2 COMMIT-1 (2026-07-06, audit force-economy-1): upkeep is the ENGINE's
                        // card-specific maintain cost (MaintenanceFacts), NOT deploy cost — the old
                        // basis made Lando Scoundrel (deploy 5, maintain 1) essentially undeployable
                        // below pile ~10 (V59 HARD -2000 on a phantom 5F obligation).
                        if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                            int totalForce = context.getGameState() != null ?
                                context.getGameState().getForcePileSize(context.getPlayerId()) : 0;
                            int forceAfterDeploy = totalForce - cost;
                            int maintenanceCost = com.gempukku.swccgo.ai.models.common.strategy
                                .MaintenanceFacts.maintainCost(blueprint);

                            // V59 HOLISTIC MAINTENANCE: Account for other planned deploys AND
                            // battle reserve. FIXES Issue #4 from peaceful-pike replay: Lando
                            // deployed with 8F "post-deploy", but Rando then spent 4F on Jyn +
                            // 1F on battle = only 3F left for 5F maintenance → Lando sacrificed.
                            // Look at all pending deploys this turn from the plan and subtract
                            // their cost. Also reserve 2F for battle interrupts/draws.
                            int pendingDeployCost = 0;
                            int battleReserve = 2;
                            try {
                                DeployPhasePlanner maintPlanner = context.getDeployPhasePlanner();
                                if (maintPlanner != null) {
                                    DeploymentPlan maintPlan = maintPlanner.getCurrentPlan();
                                    if (maintPlan != null && blueprintId != null) {
                                        for (DeploymentInstruction ins : maintPlan.getInstructions()) {
                                            if (ins == null) continue;
                                            // Skip the card we're currently evaluating (its cost already subtracted)
                                            if (blueprintId.equals(ins.getCardBlueprintId())) continue;
                                            pendingDeployCost += ins.getDeployCost();
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOG.debug("V59 MAINTENANCE: Error reading plan: {}", e.getMessage());
                            }
                            int forceAfterAllDeploys = forceAfterDeploy - pendingDeployCost - battleReserve;

                            // V64 TIGHTER MAINTENANCE: drains by opponent, Visage losses, and
                            // force losses to effects will further reduce our pile between deploy
                            // and end-of-turn. Require a DRAIN BUFFER on top of maintenance.
                            // Steve's feedback: "Rando deployed Lando (maintenance card) and did
                            // not save enough force for him. Lost at the end of his turn."
                            // Previous -500/-600 weren't enough to override +300 V52 SPEND FORCE.
                            // Now -2000 hard block guarantees maintenance cards only deploy with
                            // comfortable headroom.
                            int drainBuffer = 2;  // opponent likely drains ~2/turn
                            int safeBuffer = maintenanceCost + drainBuffer;

                            if (forceAfterDeploy < maintenanceCost) {
                                // CANNOT pay maintenance even as first deploy — HARD BLOCK
                                action.addReasoning("V59 MAINTENANCE HARD: " + blueprint.getTitle() +
                                    " needs " + maintenanceCost + "F upkeep but only " +
                                    forceAfterDeploy + "F left — WILL die at end of turn!", -2000.0f);
                                LOG.warn("V59 MAINTENANCE HARD: {} costs {}, {}F available, {}F after deploy but needs {} for upkeep — HARD BLOCKED!",
                                    blueprint.getTitle(), cost, totalForce, forceAfterDeploy, maintenanceCost);
                            } else if (forceAfterAllDeploys < maintenanceCost) {
                                // Can pay if alone, but planned deploys + battle will consume too much
                                action.addReasoning("V59 MAINTENANCE HOLISTIC: " + blueprint.getTitle() +
                                    " needs " + maintenanceCost + "F but only " + forceAfterAllDeploys +
                                    "F after all planned deploys + battle reserve — WILL be sacrificed!", -1500.0f);
                                LOG.warn("V59 MAINTENANCE HOLISTIC: {} needs {}, only {}F after deploys({}) + reserve({}) = {}F — HARD BLOCKING!",
                                    blueprint.getTitle(), maintenanceCost, forceAfterAllDeploys,
                                    pendingDeployCost, battleReserve, forceAfterAllDeploys);
                            } else if (forceAfterAllDeploys < safeBuffer) {
                                // V64: Tight — opponent drain could push us below maintenance.
                                // Raised from -80 to -400 to clearly beat +300 V52 SPEND FORCE.
                                action.addReasoning("V64 MAINTENANCE TIGHT: " + blueprint.getTitle() +
                                    " — " + forceAfterAllDeploys + "F post-deploys, need "
                                    + maintenanceCost + "+" + drainBuffer + " drain buffer — likely sacrifice!",
                                    -400.0f);
                                LOG.warn("V64 MAINTENANCE TIGHT: {} — {}F post-deploys, need {} (maint) + {} (drain buffer)",
                                    blueprint.getTitle(), forceAfterAllDeploys, maintenanceCost, drainBuffer);
                            } else {
                                LOG.info("V59 MAINTENANCE OK: {} has {}F post-all-deploys, upkeep needs {} (+{}F drain buffer)",
                                    blueprint.getTitle(), forceAfterAllDeploys, maintenanceCost, drainBuffer);
                            }
                        }
                    } catch (UnsupportedOperationException e) {
                        // Card type doesn't support deployCost (e.g., Interrupt)
                    }

                    // === V24.5: RESERVE FORCE FOR EXISTING MAINTENANCE CARDS ===
                    // If cards with maintenance costs are already in play, deploying this card
                    // must leave enough Force to pay their upkeep. Otherwise they get sacrificed.
                    if (cost > 0 && gameState != null) {
                        try {
                            // T2 MOVE #1 COMMIT-2 (2026-07-06): maintenance obligation from
                            // the shared per-decision ForceReserveService cache (same
                            // owner, in-play, maintenance-icon, and MaintenanceFacts guards).
                            // V24.5 weights (-50/-50) are unchanged. Superseded inline scan
                            // removed 2026-07-13; git preserves it.
                            int existingMaintenanceCost = context.getForceReserveFacts().maintenanceObligation;
                            if (existingMaintenanceCost > 0) {
                                int totalForceNow = gameState.getForcePileSize(context.getPlayerId());
                                int forceAfterThisDeploy = totalForceNow - cost;
                                if (forceAfterThisDeploy < existingMaintenanceCost) {
                                    action.addReasoning("V40 MAINTENANCE RESERVE: Deploying this leaves only " +
                                        forceAfterThisDeploy + " Force but need " + existingMaintenanceCost +
                                        " for existing maintenance cards (mild caution)", -50.0f);
                                    LOG.warn("V24.5 MAINTENANCE RESERVE: {} costs {}, {} Force available, " +
                                        "only {} left but existing maintenance needs {} — BLOCKING!",
                                        blueprint.getTitle(), cost, totalForceNow, forceAfterThisDeploy, existingMaintenanceCost);
                                } else if (forceAfterThisDeploy < existingMaintenanceCost + 2) {
                                    action.addReasoning("V40 MAINTENANCE RESERVE: Tight on Force for existing maintenance (" +
                                        forceAfterThisDeploy + " left, need " + existingMaintenanceCost + ") (mild caution)", -50.0f);
                                    LOG.warn("V24.5 MAINTENANCE WARNING: {} — {} Force left after deploy, maintenance needs {}",
                                        blueprint.getTitle(), forceAfterThisDeploy, existingMaintenanceCost);
                                }
                            }
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

                    action.setDeployCost(cost);

                    // === AFFORDABILITY CHECK ===
                    if (cost > availableForce) {
                        action.addReasoning(
                            String.format("Can't afford! Need %d, have %d", cost, availableForce),
                            -1000.0f
                        );
                        actions.add(action);
                        continue;
                    }

                    // === V48: VADER MOVEMENT FORCE RESERVE ===
                    // If Vader needs to move (no opponents at his location), don't spend
                    // all force on deploys — reserve enough for Vader's landspeed move.
                    if (vaderMoveReserve > 0 && cost > 0) {
                        int forceAfterDeploy = availableForce - cost;
                        if (forceAfterDeploy < vaderMoveReserve) {
                            action.addReasoning(String.format(
                                "V48 VADER MOVE RESERVE: Deploy costs %d, leaves %d — need %d for Vader to move!",
                                cost, forceAfterDeploy, vaderMoveReserve), -500.0f);
                            LOG.warn("V48 VADER MOVE RESERVE: {} costs {} force, leaves {} — Vader needs {} to move!",
                                card != null ? card.getTitle() : actionText, cost, forceAfterDeploy, vaderMoveReserve);
                        }
                    }

                    // === V67z DEPLOY TRANSIT RESERVE (Steve, 2026-06) — mirror of V48/V79 ===
                    // Hold back the move-phase transit Force on Hidden Path so the Jedi
                    // staged at the Underground Corridor can transit off Mapuzo and flip
                    // the objective. -1500 (not -500 like V48/V79): Hidden Path Jedi
                    // Survivor deploys score ~950, so a -500 wouldn't stop them — the
                    // penalty must drop the offending deploy below Pass so Rando actually
                    // holds the Force. Only force-costing deploys that would dip below the
                    // reserve are hit; free [download] Jabiim locations (cost 0) are not.
                    if (v67zTransitReserve > 0 && cost > 0) {
                        int forceAfterDeployV67z = availableForce - cost;
                        if (forceAfterDeployV67z < v67zTransitReserve) {
                            action.addReasoning(String.format(
                                "V67z TRANSIT RESERVE: Deploy costs %d, leaves %d — need %d to transit Jedi off Mapuzo (flip the objective)!",
                                cost, forceAfterDeployV67z, v67zTransitReserve), -1500.0f);
                            LOG.warn("V67z DEPLOY TRANSIT RESERVE: {} costs {}, leaves {} — need {} for the move-phase transit!",
                                card != null ? card.getTitle() : actionText, cost, forceAfterDeployV67z, v67zTransitReserve);
                        }
                    }

                    // === V79 (Steve, 2026-05-15): VERGE DEATH-STAR MOVE RESERVE ===
                    // If Verge of Greatness active + Death Star not yet at Scarif,
                    // reserve 1 Force for the Move phase. Mirror of V48 pattern.
                    if (v79VergeMoveReserve > 0 && cost > 0) {
                        int forceAfterDeployV79 = availableForce - cost;
                        if (forceAfterDeployV79 < v79VergeMoveReserve) {
                            action.addReasoning(String.format(
                                "V79 VERGE MOVE RESERVE: Deploy costs %d, leaves %d — need %d for Death Star to move toward Scarif!",
                                cost, forceAfterDeployV79, v79VergeMoveReserve), -500.0f);
                            LOG.warn("V79 VERGE MOVE RESERVE: {} costs {}, leaves {} — Death Star needs {} for move!",
                                card != null ? card.getTitle() : actionText, cost, forceAfterDeployV79, v79VergeMoveReserve);
                        }
                    }

                    // === V29.13: FORCE RESERVATION (deploy-aggressive) ===
                    // Philosophy: ALWAYS deploy as much as possible. Board presence wins games.
                    // Maintenance is a future cost — handle it by activating more Force next turn,
                    // or accept losing a maintenance card (it might die as attrition anyway).
                    //
                    // Non-maintenance cards: ZERO maintenance penalty. Deploy freely.
                    // Maintenance cards: Small tiebreaker penalty if Force is very tight.
                    // DTF/grabber: Tiny soft penalty — nice to keep 1 Force, never a blocker.
                    if (cost > 0 && gameState != null) {
                        try {
                            boolean thisCardHasMaint = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE);
                            int forceAfterThisDeploy = availableForce - cost;
                            // T2 COMMIT-1 (2026-07-06, audit force-economy-1): compare against the
                            // ENGINE's maintain cost, not deploy cost (old basis over-penalized 2-5x).
                            int thisCardMaint = thisCardHasMaint
                                ? com.gempukku.swccgo.ai.models.common.strategy.MaintenanceFacts.maintainCost(blueprint)
                                : 0;

                            // --- Only apply maintenance awareness to maintenance card deploys ---
                            if (thisCardHasMaint && forceAfterThisDeploy < thisCardMaint) {
                                // Deploying a maintenance card but won't have enough Force to pay
                                // its own maintenance at end of turn. Small tiebreaker — NOT a blocker.
                                // The card can be lost as attrition in battle, or Rando activates
                                // more Force next turn to cover it.
                                float maintPenalty = -50.0f; // V40: mild caution for maintenance
                                if (forceAfterThisDeploy <= 0) {
                                    maintPenalty = -500.0f; // V40: Zero Force left — maintenance card will immediately die
                                }
                                action.addReasoning(
                                    String.format("V29.13 MAINT AWARENESS: This card costs %d maint at end of turn, " +
                                        "only %d Force left after deploy — plan to activate more next turn",
                                        thisCardMaint, forceAfterThisDeploy),
                                    maintPenalty);
                            }

                            // --- DTF / Grabber interrupt reserve (soft penalty) ---
                            // Nice to keep 1 Force for interrupts, but never block a deploy over it.
                            // T2 MOVE #1 COMMIT-2 (2026-07-06): DTF + grabber facts from the
                            // shared per-decision ForceReserveService cache. NOTE documented
                            // unification: the old scan below broke on the FIRST grabber card
                            // found regardless of state; the shared fact counts ANY unused
                            // grabber (MoveEvaluator V29 semantic) — identical unless a deck
                            // fields 2+ grabbers with mixed state. Superseded inline scans were
                            // removed 2026-07-13; the -30 weight is unchanged; git preserves them.
                            boolean dtfOnTable = context.getForceReserveFacts().dtfActive;
                            boolean grabberUnused = context.getForceReserveFacts().grabberUnused;
                            if ((dtfOnTable || grabberUnused) && forceAfterThisDeploy <= 0) {
                                action.addReasoning(
                                    String.format("V29.13 INTERRUPT RESERVE: %s%s but 0 Force left for them after deploy",
                                        dtfOnTable ? "DTF active" : "",
                                        grabberUnused ? (dtfOnTable ? " + grabber ready" : "Grabber ready") : ""),
                                    -30.0f);
                            }
                        } catch (Exception e) {
                            LOG.debug("V29: Error checking force reserve during deploy: {}", e.getMessage());
                        }
                    }

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
                                // --- Calculate Force reserve needed (maint + interrupts) ---
                                // T2 MOVE #1 COMMIT-2 (2026-07-06): on-table maintenance
                                // obligation from the shared per-decision ForceReserveService
                                // cache; THIS card's own maintain cost stays a local add
                                // below. V67bl removed the paired solo exception, so these facts now
                                // affect diagnostics only. Superseded inline scan removed; see git.
                                int maintObligation = context.getForceReserveFacts().maintenanceObligation;
                                // Add maintenance for THIS card if applicable
                                if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                                    maintObligation += com.gempukku.swccgo.ai.models.common.strategy
                                        .MaintenanceFacts.maintainCost(blueprint);
                                }
                                // Only reserve for interrupts when opponent has Draw Their Fire
                                // T2 MOVE #1 COMMIT-2 (2026-07-06): ForceReserveService preserves
                                // exact-opponent, in-play detection and the 1 Force fact. V67bl means
                                // pairedDeployPossible has no score consumer. Superseded scan removed;
                                // git preserves it.
                                int interruptReserve = context.getForceReserveFacts().dtfActive ? 1 : 0;
                                // V53: Reserve 1 force per undercover spy for movement next turn.
                                // If opponent moves away from our spy, we need force to follow them.
                                // T2 MOVE #1 COMMIT-2 (2026-07-06): ForceReserveService owns the
                                // count. Its in-play gate is behavior-neutral because GameState clears
                                // the undercover flag off table. V67bl leaves no score consumer.
                                // Superseded inline scan removed 2026-07-13; git preserves it.
                                // The diagnostic V53 log remains live.
                                int spyMoveReserve = context.getForceReserveFacts().undercoverSpyCount;
                                if (spyMoveReserve > 0) {
                                    LOG.info("V53 SPY RESERVE: Reserving {} force for {} undercover spy movement(s)",
                                        spyMoveReserve, spyMoveReserve);
                                }

                                int forceReserveNeeded = maintObligation + interruptReserve + spyMoveReserve;

                                // --- Exception 1: Paired deploy available ---
                                // Check if another character in hand can deploy to same location
                                // AND we can afford both + reserve
                                boolean pairedDeployPossible = false;
                                int forceAfterThis = availableForce - cost;
                                if (hand != null && forceAfterThis > forceReserveNeeded) {
                                    for (PhysicalCard handCard : hand) {
                                        if (handCard == null || handCard == card) continue;
                                        SwccgCardBlueprint hBp = handCard.getBlueprint();
                                        if (hBp == null) continue;
                                        if (hBp.getCardCategory() != CardCategory.CHARACTER) continue;
                                        Float hCostF = hBp.getDeployCost();
                                        int hCost = (hCostF != null) ? hCostF.intValue() : 0;
                                        // Add maintenance for the buddy if applicable
                                        int buddyMaint = 0;
                                        if (hBp.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                                            buddyMaint = hCost;
                                        }
                                        int totalNeeded = hCost + forceReserveNeeded + buddyMaint;
                                        if (forceAfterThis >= totalNeeded) {
                                            pairedDeployPossible = true;
                                            LOG.info("V29 PAIRED: {} can follow {} (cost {}, {} Force left after both + reserve)",
                                                hBp.getTitle(), card.getTitle(), hCost, forceAfterThis - hCost);
                                            break;
                                        }
                                    }
                                }

                                // --- Exception 2: Objective-flip deploy ---
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

                                // --- Apply penalties / allow exceptions ---
                                // V67bl (Steve, 2026-05-11): V29 PAIRED "solo OK" exception REMOVED.
                                //
                                // Old rule: if any character in hand had force-cost numbers
                                // that allowed it to "follow" the solo char, V29 PAIRED gave
                                // 0 penalty — full pass on solo deploy. But there's no
                                // guarantee the hypothetical buddy goes to the SAME site, and
                                // in replay 6fqi4jm1kkp7e9i8 Stormtrooper Patrol got a free
                                // solo pass to Guest Quarters because "Vader in hand could
                                // follow" — Vader went to Docking Bay instead, Stormtrooper
                                // died to a Jedi stack.
                                //
                                // V38 SOLO CAUTION (-150) below now applies regardless of
                                // hand contents. The "buddy follows" credit is properly
                                // earned later by V38 REINFORCE STRONG ALLY (+300) when the
                                // buddy actually deploys to where the solo char IS — paying
                                // for actual co-location, not hypothetical plans.
                                if (isObjectiveFlipDeploy) {
                                    // Exception 2: Allow solo for objective flip, penalize if no escape route
                                    if (hasEscapeRoute) {
                                        action.addReasoning(
                                            String.format("V29 OBJ-FLIP: %s solo at '%s' to help flip objective — escape route exists!",
                                                card.getTitle(), targetLoc != null ? targetLoc.getTitle() : "?"), 50.0f);
                                        LOG.info("V29 OBJ-FLIP: Allowing solo {} for objective flip — has escape route", card.getTitle());
                                    } else {
                                        // Strong preference penalty — still allow but it's risky
                                        action.addReasoning(
                                            String.format("V29 OBJ-FLIP: %s solo at '%s' for flip but NO escape route — risky!",
                                                card.getTitle(), targetLoc != null ? targetLoc.getTitle() : "?"), -150.0f);
                                        LOG.warn("V29 OBJ-FLIP: Solo {} for flip but no escape route — penalizing", card.getTitle());
                                    }
                                } else {
                                    // V38: Check if this is a STAGING deploy — non-battleground
                                    // adjacent to a battleground where we can buddy up next turn
                                    boolean isStagingDeploy = false;
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

                                    if (isStagingDeploy) {
                                        // V38: Staging deploy — mild penalty, can move to buddy next turn
                                        action.addReasoning(String.format(
                                            "V38 STAGING: %s to non-battleground — move to buddy up next turn",
                                            card.getTitle()), -80.0f);
                                        LOG.info("V38 STAGING: {} deploying to staging site — can buddy next turn (-80)",
                                            card.getTitle());
                                    } else {
                                        // V38: Softer penalty than old -300 — allow with mild discourage
                                        action.addReasoning(
                                            String.format("V38 SOLO CAUTION: %s (power %d) solo — vulnerable but acceptable",
                                                card.getTitle(), powerVal),
                                            -150.0f); // V38: Reduced from -300
                                        LOG.info("V38 SOLO CAUTION: {} (power {}) — mild penalty (-150)",
                                            card.getTitle(), powerVal);
                                    }
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
                                    String cTitle = c.getTitle() != null ? c.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (cTitle.contains("vader")) vaderHere = true;
                                    Float cAb = c.getBlueprint().getAbility();
                                    float cAbVal = cAb != null ? cAb : 0;
                                    allyAbilityHere += cAbVal;
                                    if (cAbVal >= 4) strongAllyHere = true;
                                }

                                if (vaderHere) {
                                    action.addReasoning(String.format(
                                        "V38 REINFORCE VADER: Deploy %s with Vader — buddy rotation!",
                                        card.getTitle()), 400.0f);
                                    LOG.warn("V38 REINFORCE VADER: {} deploying to Vader's site (+400)", card.getTitle());
                                } else if (strongAllyHere) {
                                    float reinBonus = 200.0f;
                                    if (allyAbilityHere + abilityVal >= RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                        reinBonus = 300.0f; // Reaches buddy threshold!
                                    }
                                    action.addReasoning(String.format(
                                        "V38 REINFORCE ALLY: Deploy %s to strong ally (ability %.0f + %.0f = %.0f)",
                                        card.getTitle(), allyAbilityHere, (float)abilityVal,
                                        allyAbilityHere + abilityVal), reinBonus);
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
                                            // V67ab (Steve, 2026-05-03): Only protect at BATTLEGROUNDS.
                                            // Solo allies at non-BG sites don't need protection (no
                                            // battles). Symptom: Mira deployed to The Works to
                                            // 'protect' Sidius — wasted Mira at non-BG.
                                            boolean v67abBgSeek = false;
                                            try {
                                                v67abBgSeek = game.getModifiersQuerying()
                                                    .isBattleground(gameState, loc, null);
                                            } catch (Exception e) { /* ignore */ }
                                            if (!v67abBgSeek) {
                                                LOG.info("V67ab BUDDY-SEEK SKIP: {} non-BG, {} doesn't need protection here",
                                                    locTitle, soloAlly.getTitle());
                                            } else {
                                                action.addReasoning(
                                                    String.format("V29 BUDDY-SEEK: Deploy to protect vulnerable %s (power %d) at %s!",
                                                        soloAlly.getTitle(), allyPower, locTitle),
                                                    200.0f);
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
                                    String vTitle = tableCard.getTitle() != null
                                        ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (vTitle.contains("vader")) {
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
                                    String deployCardTitle = card.getTitle() != null
                                        ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                                    boolean isNotVader = !deployCardTitle.contains("vader");

                                    if (deploysToVaderLoc && isNotVader) {
                                        // V35.1: Only give grouping bonus if opponents are at Vader's location
                                        // or nowhere else on the board. Don't group at empty locations!
                                        float oppAtVaderLoc = 0;
                                        try {
                                            String v351Oid = game.getOpponent(playerId);
                                            oppAtVaderLoc = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                gameState, vaderLoc, v351Oid, false, false);
                                        } catch (Exception e) { /* ignore */ }

                                        if (oppAtVaderLoc > 0) {
                                            // Opponents at Vader's location — GREAT, deploy to fight!
                                            float groupBonus = 350.0f; // V35.1: Raised from 250
                                            if (powerVal >= 5) groupBonus += 50.0f;
                                            action.addReasoning(String.format(
                                                "V35.1 HUNT GROUP+ENGAGE: Deploy %s WITH Vader at %s — opponents here (power %.0f)!",
                                                card.getTitle(), vaderLoc.getTitle(), oppAtVaderLoc), groupBonus);
                                            LOG.warn("V35.1 HUNT GROUP+ENGAGE: {} with Vader at {} — opponents power={} (+{})",
                                                card.getTitle(), vaderLoc.getTitle(), (int)oppAtVaderLoc, (int)groupBonus);
                                        } else {
                                            // Vader is at an EMPTY location — grouping here wastes deployment
                                            // Only mild bonus (better to deploy where opponents are)
                                            float groupBonus = 50.0f; // V35.1: Reduced from 250!
                                            action.addReasoning(String.format(
                                                "V35.1 HUNT GROUP (EMPTY): Deploy %s with Vader at %s — but NO opponents here!",
                                                card.getTitle(), vaderLoc.getTitle()), groupBonus);
                                            LOG.warn("V35.1 HUNT GROUP EMPTY: {} with Vader at {} — no opponents (only +{})",
                                                card.getTitle(), vaderLoc.getTitle(), (int)groupBonus);
                                        }
                                    } else if (isNotVader && !deploysToVaderLoc) {
                                        // Deploying AWAY from Vader — penalize
                                        // Exception: if deploying to a location where we need presence for objective
                                        boolean isObjRelevant = false;
                                        if (huntDeployAnalyzer.isFlipped()) {
                                            // Post-flip: protection locations are also important
                                            // Don't penalize deploying to flip-back protection locations
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

                                        if (!isObjRelevant) {
                                            // V40: Hunt scatter neutralized — deploy freely
                                            action.addReasoning(String.format(
                                                "V40 HUNT SCATTER: %s deploying away from Vader at %s (neutral)",
                                                card.getTitle(), vaderLoc.getTitle()), 0.0f);
                                            LOG.warn("V40 HUNT SCATTER: {} NOT at Vader's location ({}) — neutral (was -600)",
                                                card.getTitle(), vaderLoc.getTitle());
                                        }
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
                                    if (v53SpyPower > 0) {
                                        // Add spy power bonus — deploying here means we can flip the spy
                                        action.addReasoning(String.format(
                                            "V53 SPY ALLY: Our spy at %s has power %.0f — deploy here to flip and fight together!",
                                            v36Loc.getTitle(), v53SpyPower), 200.0f);
                                        LOG.warn("V53 SPY ALLY: Spy power {} at {} — +200 deploy bonus",
                                            (int)v53SpyPower, v36Loc.getTitle());
                                        // Count spy as our power for deploy decisions
                                        v36OurPower += v53SpyPower;
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if (v36OppPower > 0) {
                                    // Opponent has presence — check drain amount
                                    float drainAmount = 1.0f;
                                    try {
                                        drainAmount = game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, v36Loc, v36Oid);
                                    } catch (Exception e) { /* default 1 */ }

                                    if (drainAmount >= 3.0f && v36OurPower == 0) {
                                        // V51: EMERGENCY — drain 3+ uncontested, flood with everything
                                        action.addReasoning(String.format(
                                            "V51 DRAIN EMERGENCY: %s drains %.0f at %s — FLOOD this location!",
                                            v36Oid, drainAmount, v36Loc.getTitle()), 600.0f);
                                        LOG.warn("V51 DRAIN EMERGENCY: {} to {} — opponent drains {} uncontested (+600)",
                                            card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                    } else if (drainAmount >= 3.0f && v36OurPower > 0) {
                                        // V51: Drain 3+ and we already sent someone — keep piling on
                                        action.addReasoning(String.format(
                                            "V51 DRAIN REINFORCE: %s drains %.0f at %s — keep piling on!",
                                            v36Oid, drainAmount, v36Loc.getTitle()), 500.0f);
                                        LOG.warn("V51 DRAIN REINFORCE: {} to {} — opponent drains {} we have presence (+500)",
                                            card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                    } else if (drainAmount >= 2.0f && v36OurPower == 0) {
                                        // V51: Drain 2+ uncontested — this will be THE battle site
                                        action.addReasoning(String.format(
                                            "V51 CONTEST BATTLEGROUND: %s drains %.0f at %s — this is THE decisive fight!",
                                            v36Oid, drainAmount, v36Loc.getTitle()), 500.0f);
                                        LOG.warn("V51 CONTEST BATTLEGROUND: {} to {} — opponent drains {} uncontested (+500)",
                                            card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                    } else if (drainAmount >= 2.0f && v36OurPower > 0) {
                                        // V51: Drain 2+ and we have presence — reinforce for the big fight
                                        action.addReasoning(String.format(
                                            "V51 REINFORCE BATTLEGROUND: %s drains %.0f at %s — reinforce for battle!",
                                            v36Oid, drainAmount, v36Loc.getTitle()), 500.0f);
                                        LOG.warn("V51 REINFORCE BATTLEGROUND: {} to {} — opponent drains {} we have presence (+500)",
                                            card.getTitle(), v36Loc.getTitle(), (int)drainAmount);
                                    } else if (v36OurPower == 0) {
                                        // Drain 1 uncontested — still worth contesting
                                        float contestDrainBonus = 200.0f + (drainAmount * 100.0f);
                                        action.addReasoning(String.format(
                                            "V36 CONTEST DRAIN: %s drains %.0f at %s UNCONTESTED — deploy to stop the bleeding!",
                                            v36Oid, drainAmount, v36Loc.getTitle()), contestDrainBonus);
                                        LOG.warn("V36 CONTEST DRAIN: {} to {} — opponent drains {} uncontested (+{})",
                                            card.getTitle(), v36Loc.getTitle(), (int)drainAmount, (int)contestDrainBonus);
                                    }
                                }
                                break; // Found target location
                            }
                        } catch (Exception e) {
                            LOG.debug("V51 CONTEST DRAIN: Error: {}", e.getMessage());
                        }
                    }

                    // === V51: VADER AGGRESSIVE FLIP — Deploy Vader from hand to opponent battleground ===
                    // If Hunt Down V objective is NOT flipped AND Vader is in hand, deploying
                    // Vader to ANY opponent's battleground site immediately flips the objective.
                    // This is THE highest priority play — Steve does this Turn 1 every game.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER && actionLower.contains("vader")
                        && !actionLower.contains("bounty") && !actionLower.contains("lightsaber")
                        && gameState != null && game != null) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer vaderFlipAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (vaderFlipAnalyzer != null && vaderFlipAnalyzer.isAnalyzed()
                            && vaderFlipAnalyzer.isHuntDownV() && !vaderFlipAnalyzer.isFlipped()) {
                            // Hunt Down not flipped — check if deploying to opponent's battleground
                            String vfPid = context.getPlayerId();
                            String vfOid = game.getOpponent(vfPid);
                            for (PhysicalCard vfLoc : gameState.getTopLocations()) {
                                if (vfLoc == null || vfLoc.getTitle() == null) continue;
                                String vfLocLower = vfLoc.getTitle().toLowerCase(Locale.ROOT);
                                if (!actionLower.contains(vfLocLower)) continue;
                                // Check if it's an opponent's location with their presence or their icons
                                try {
                                    float vfOppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, vfLoc, vfOid, false, false);
                                    com.gempukku.swccgo.game.SwccgCardBlueprint vfLocBp = vfLoc.getBlueprint();
                                    boolean isOpponentSite = false;
                                    if (vfLocBp != null) {
                                        com.gempukku.swccgo.common.Side oppSide = (context.getSide() == com.gempukku.swccgo.common.Side.DARK)
                                            ? com.gempukku.swccgo.common.Side.LIGHT : com.gempukku.swccgo.common.Side.DARK;
                                        int oppIcons = (oppSide == com.gempukku.swccgo.common.Side.DARK)
                                            ? vfLocBp.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                            : vfLocBp.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                        if (oppIcons > 0 || vfOppPower > 0) isOpponentSite = true;
                                    }
                                    if (isOpponentSite) {
                                        action.addReasoning(String.format(
                                            "V51 VADER FLIP: Deploy Vader to %s — FLIPS OBJECTIVE IMMEDIATELY!",
                                            vfLoc.getTitle()), 900.0f);
                                        LOG.warn("V51 VADER FLIP: Vader to {} — Hunt Down flips! +900", vfLoc.getTitle());
                                    }
                                } catch (Exception e) { /* ignore */ }
                                break;
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
                                    if (v50Turn <= 3 && totalOurPowerAfterDeploy < oppPowerHere) {
                                        float disadvantagePenalty = -200.0f;
                                        action.addReasoning(String.format(
                                            "V50 EARLY DANGER: Turn %d — deploying %s to %s would leave us at power %.0f vs opponent %.0f — wait for backup!",
                                            v50Turn, card.getTitle(), locCard.getTitle(), totalOurPowerAfterDeploy, oppPowerHere), disadvantagePenalty);
                                        LOG.warn("V50 DEPLOY DANGER T{}: {} to {} — our power {}, opponent power {} — PENALIZED (turns 1-3 only)",
                                            v50Turn, card.getTitle(), locCard.getTitle(), (int)totalOurPowerAfterDeploy, (int)oppPowerHere);
                                        continue;
                                    } else if (v50Turn > 3 && totalOurPowerAfterDeploy < oppPowerHere) {
                                        // After turn 3: log the disadvantage but DEPLOY ANYWAY
                                        action.addReasoning(String.format(
                                            "V50 LATE DEPLOY: Turn %d — deploying %s to %s despite power %.0f vs %.0f — must stay active!",
                                            v50Turn, card.getTitle(), locCard.getTitle(), totalOurPowerAfterDeploy, oppPowerHere), 0.0f);
                                        LOG.warn("V50 LATE DEPLOY T{}: {} to {} — our power {}, opponent power {} — deploying anyway (past turn 3)",
                                            v50Turn, card.getTitle(), locCard.getTitle(), (int)totalOurPowerAfterDeploy, (int)oppPowerHere);
                                    }

                                    // V34: Opponents are HERE and we can compete — deploy directly to contest!
                                    float engageBonus = 250.0f;
                                    if (oppPowerHere >= 6) engageBonus += 100.0f;

                                    // V35: Check for Jedi at this location — Vader/Inquisitor bonuses
                                    boolean v35JediHere = false;
                                    boolean v35HatredHere = false;
                                    try {
                                        for (PhysicalCard lc : gameState.getCardsAtLocation(locCard)) {
                                            if (lc == null) continue;
                                            String lcTitle = lc.getTitle() != null ? lc.getTitle().toLowerCase(Locale.ROOT) : "";
                                            if (opponentIdDeploy.equals(lc.getOwner())) {
                                                if (isJediOrPadawan(lcTitle)) v35JediHere = true;
                                                java.util.List<PhysicalCard> stacked = gameState.getStackedCards(lc);
                                                if (stacked != null && !stacked.isEmpty()) v35HatredHere = true;
                                            }
                                        }
                                    } catch (Exception e) { /* ignore */ }

                                    String deployCardLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (v35JediHere && deployCardLower.contains("vader")) {
                                        // V35.8: Raised from +350 to +600 — killing Jedi is THE objective
                                        // of Hunt Down. Opponent loses extra Force when Jedi dies.
                                        engageBonus += 600.0f;
                                        LOG.warn("V35.8 HUNT JEDI DEPLOY: Vader to {} with JEDI! (+600)",
                                            locCard.getTitle());
                                    }
                                    if (v35JediHere && isInquisitor(deployCardLower)) {
                                        engageBonus += 250.0f; // Inquisitor vs Jedi = power bonuses + destiny
                                        LOG.warn("V35 INQUISITOR vs JEDI: {} to {} (+250)", card.getTitle(), locCard.getTitle());
                                    }
                                    if (v35HatredHere && isInquisitor(deployCardLower)) {
                                        engageBonus += (float) RandoConfig.SCORE_INQUISITOR_HATRED_SYNERGY; // +300
                                        LOG.warn("V35 INQUISITOR+HATRED: {} to {} with hatred (+{})",
                                            card.getTitle(), locCard.getTitle(), RandoConfig.SCORE_INQUISITOR_HATRED_SYNERGY);
                                    }

                                    action.addReasoning(String.format(
                                        "V34 DIRECT ENGAGE: Deploy %s to %s (opp power %.0f%s%s) — contest!",
                                        card.getTitle(), locCard.getTitle(), oppPowerHere,
                                        v35JediHere ? " JEDI" : "", v35HatredHere ? " HATRED" : ""), engageBonus);
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

                                        // V37.4: Check if we CAN actually deploy to any opponent location.
                                        // If not, empty site deploy is our ONLY option — reduce penalty.
                                        boolean canDeployToOpponents = false;
                                        try {
                                            for (PhysicalCard oppLoc : gameState.getTopLocations()) {
                                                if (oppLoc == null) continue;
                                                float oppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                    gameState, oppLoc, game.getOpponent(playerId), false, false);
                                                if (oppPwr > 0) {
                                                    // Check if this deploy action could target this location
                                                    String oppLocName = oppLoc.getTitle() != null
                                                        ? oppLoc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                    // We can't check all possible actions, but if opponent
                                                    // is at a location on a different planet, we probably can't deploy there
                                                    canDeployToOpponents = true; // Assume we can for now
                                                    break;
                                                }
                                            }
                                        } catch (Exception e) { /* ignore */ }

                                        // V40: Empty site penalties neutralized — deploy freely
                                        float emptyPenalty = 0.0f;

                                        action.addReasoning(String.format(
                                            "V36 EMPTY DEPLOY: %s to %s — no opponents here%s (penalty %.0f)",
                                            card.getTitle(), locCard.getTitle(),
                                            hasDrainValue ? " but has drain icons" : "", emptyPenalty),
                                            emptyPenalty);
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

                    // === CARD VALUE SCORING ===
                    // Score based on power + ability vs cost
                    int cardValue = powerVal + abilityVal;
                    float valueRatio = cost > 0 ? (float) cardValue / cost : cardValue;

                    if (valueRatio >= 2.0f) {
                        action.addReasoning(String.format("Excellent value (%.1f)", valueRatio), 40.0f);
                    } else if (valueRatio >= 1.5f) {
                        action.addReasoning(String.format("Good value (%.1f)", valueRatio), 20.0f);
                    } else if (valueRatio >= 1.0f) {
                        action.addReasoning(String.format("Average value (%.1f)", valueRatio), 0.0f);
                    } else {
                        action.addReasoning(String.format("V40: Below average value (%.1f) — deploy anyway", valueRatio), 0.0f);
                    }

                    // === HIGH DESTINY BONUS ===
                    if (destinyVal >= 5.0f) {
                        action.addReasoning(String.format("High destiny (%.0f)", destinyVal), 15.0f);
                    }

                    // === CARD TYPE BONUSES ===
                    CardCategory category = blueprint.getCardCategory();

                    // === V40: POSITIVE DEPLOY BONUSES ===
                    // Reward good deploys instead of penalizing questionable ones.
                    String cardTitleLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                    if (category == CardCategory.CHARACTER && gameState != null && game != null) {
                        try {
                            String v40Pid = context.getPlayerId();
                            String v40Oid = gameState.getOpponent(v40Pid);
                            String v40ActionLower = actionText.toLowerCase(Locale.ROOT);

                            // --- Deploy Vader/Emperor solo OK: +100 ---
                            if (cardTitleLower.contains("vader") || cardTitleLower.contains("emperor")
                                || cardTitleLower.contains("palpatine")) {
                                action.addReasoning("V40 ELITE: Vader/Emperor deploy bonus!", 100.0f);
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
                                        if (oppIcons >= 2) {
                                            action.addReasoning(String.format(
                                                "V40 HIGH DRAIN: %s has %d opponent force icons — high drain potential!",
                                                v40Loc.getTitle(), oppIcons), 200.0f);
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
                                            if (!hasDrainReduction) {
                                                action.addReasoning(String.format(
                                                    "V40 GOOD DRAIN SITE: %s has no drain reduction in game text!",
                                                    v40Loc.getTitle()), 100.0f);
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

                                    // V51: Reinforcement bonus scales with drain value
                                    if (v40FriendlyCount > 0 && v51OurDrain >= 2.0f) {
                                        // Drain 2+ site with friendlies — THIS is the battleground
                                        action.addReasoning(String.format(
                                            "V51 FORTIFY BATTLEGROUND: Joining %d friendlies at %s (our drain %.0f) — this is THE fight!",
                                            v40FriendlyCount, v40Loc.getTitle(), v51OurDrain), 500.0f);
                                        LOG.warn("V51 FORTIFY BATTLEGROUND: {} joins {} friendlies at {} (drain {}) — +500",
                                            card.getTitle(), v40FriendlyCount, v40Loc.getTitle(), (int)v51OurDrain);
                                    } else if (v40FriendlyCount == 0 && v51OurDrain >= 2.0f) {
                                        // Drain 2+ site, establishing first presence
                                        action.addReasoning(String.format(
                                            "V51 ESTABLISH BATTLEGROUND: First deploy to %s (our drain %.0f) — start the army!",
                                            v40Loc.getTitle(), v51OurDrain), 400.0f);
                                        LOG.warn("V51 ESTABLISH BATTLEGROUND: {} first to {} (drain {}) — +400",
                                            card.getTitle(), v40Loc.getTitle(), (int)v51OurDrain);
                                    } else if (v40FriendlyCount > 0) {
                                        // Non-drain-2 site but has friendlies — still good to reinforce
                                        action.addReasoning(String.format(
                                            "V51 REINFORCE: Joining %d friendlies at %s!",
                                            v40FriendlyCount, v40Loc.getTitle()), 300.0f);
                                        LOG.warn("V51 REINFORCE: {} joins {} friendlies at {} — +300",
                                            card.getTitle(), v40FriendlyCount, v40Loc.getTitle());
                                    }

                                    // V51: Buddy system — ability thresholds with higher bonuses
                                    float v40CardAbility = 0;
                                    if (card.getBlueprint().hasAbilityAttribute()) {
                                        Float v40ab2 = card.getBlueprint().getAbility();
                                        v40CardAbility = (v40ab2 != null ? v40ab2 : 0);
                                    }
                                    float totalAbilityAfter = v40FriendlyAbility + v40CardAbility;

                                    if (v40FriendlyAbility < 4.0f && totalAbilityAfter >= 4.0f && v40FriendlyCount > 0) {
                                        // V51: Deploy enables battle destiny at this site!
                                        action.addReasoning(String.format(
                                            "V51 BUDDY DESTINY: Ability %.0f → %.0f (>= 4) at %s — battle destiny ENABLED!",
                                            v40FriendlyAbility, totalAbilityAfter, v40Loc.getTitle()), 400.0f);
                                        LOG.warn("V51 BUDDY DESTINY: {} enables battle destiny at {} (ability {} → {}) — +400",
                                            card.getTitle(), v40Loc.getTitle(), (int)v40FriendlyAbility, (int)totalAbilityAfter);
                                    } else if (totalAbilityAfter >= 7.0f && v40FriendlyCount > 0) {
                                        // V51: Full buddy system — ideal ability threshold
                                        action.addReasoning(String.format(
                                            "V51 BUDDY FULL: Ability total %.0f >= 7 at %s — full buddy system!",
                                            totalAbilityAfter, v40Loc.getTitle()), 500.0f);
                                        LOG.warn("V51 BUDDY FULL: {} — total ability {} at {} — +500",
                                            card.getTitle(), (int)totalAbilityAfter, v40Loc.getTitle());
                                    } else if (totalAbilityAfter >= 4.0f && v40FriendlyCount > 0) {
                                        // V51: Ability already >= 4, reinforcing toward 7
                                        action.addReasoning(String.format(
                                            "V51 BUDDY REINFORCE: Ability %.0f → %.0f at %s — building toward 7!",
                                            v40FriendlyAbility, totalAbilityAfter, v40Loc.getTitle()), 200.0f);
                                        LOG.warn("V51 BUDDY REINFORCE: {} ability {} → {} at {} — +200",
                                            card.getTitle(), (int)v40FriendlyAbility, (int)totalAbilityAfter, v40Loc.getTitle());
                                    }

                                    // V51: Armed character bonus at drain 2+ sites
                                    if (v51OurDrain >= 2.0f || (v40FriendlyCount > 0)) {
                                        String v51CardLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (v51CardLower.contains("lightsaber") || v51CardLower.contains("blaster")
                                            || v51CardLower.contains("with lightsaber") || v51CardLower.contains("with blaster")) {
                                            action.addReasoning(String.format(
                                                "V51 ARMED: %s brings a weapon to %s — ready for battle!",
                                                card.getTitle(), v40Loc.getTitle()), 150.0f);
                                            LOG.warn("V51 ARMED: {} to {} — weapon bonus +150", card.getTitle(), v40Loc.getTitle());
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
                        // === V162 (Steve, 2026-06): LOCATIONS DEPLOY FIRST, life-force gated ===
                        // Steve's rule: "Never hold locations in hand unless life force <= 10
                        // (reserve deck + force pile + used pile combined). Early game, deploy
                        // locations BEFORE anything else in the deploy phase. Only when life
                        // force is low may Rando hold a location in hand as force-loss fodder."
                        // Fixes the Bespin-stuck-in-hand → AMSD loop: the Bespin system sat in
                        // hand undeployed while AMSD (which requires it on table) looped forever.
                        // A location in hand must ALWAYS be a strong positive deploy unless we
                        // are deliberately saving it as fodder (life force <= 10).
                        int v162LifeForce = 0;
                        try {
                            String v162Pid = context.getPlayerId();
                            if (gameState != null && v162Pid != null) {
                                v162LifeForce = gameState.getReserveDeckSize(v162Pid)
                                    + gameState.getForcePileSize(v162Pid)
                                    + gameState.getUsedPile(v162Pid).size();
                            }
                        } catch (Exception ignore) { /* treat as healthy */ v162LifeForce = 99; }

                        if (v162LifeForce <= 10) {
                            // Low life force: HOLD the location in hand for force-loss fodder.
                            // Suppress the deploy boost so it isn't force-deployed.
                            action.addReasoning("V162 HOLD LOCATION: life force " + v162LifeForce
                                + " <= 10 — keep '" + card.getTitle() + "' in hand as force-loss fodder",
                                -200.0f);
                            LOG.warn("V162 HOLD LOCATION: {} — life force {} <= 10, hold for fodder", card.getTitle(), v162LifeForce);
                        } else {
                            // Healthy: deploy locations FIRST, before anything else this phase.
                            // +1400 (V67ai tiering) already puts it ahead; V162 adds Steve's +500
                            // floor and the explicit "locations first" intent so it always wins.
                            action.addReasoning("V162 LOCATION FIRST: deploy locations before anything else (life force "
                                + v162LifeForce + " > 10) — foundation for drains/objective +500", 500.0f);
                            action.addReasoning("V67ai LOCATION DEPLOY ORDER [Tier 4 HAND]: deploy location from hand — force generation foundation!",
                                1400.0f);
                            LOG.warn("V162 LOCATION FIRST + V67ai TIER 4 HAND: {} (turn {}, life force {}) → +1900",
                                card.getTitle(), context.getTurnNumber(), v162LifeForce);
                        }
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
                        if (v67arUnarmed == 0 && v67arArmed > 0) {
                            action.addReasoning(String.format(
                                "V67ar UNIVERSAL BLOCK: every Rando character (%d) already armed — pulled weapon would stack a 2nd weapon (forbidden)!",
                                v67arArmed), -9999.0f);
                            LOG.warn("V67ar UNIVERSAL BLOCK (DeployEvaluator pull): '{}' — all {} chars armed",
                                actionText, v67arArmed);
                        } else if (v67arUnarmed == 0) {
                            action.addReasoning(
                                "V67ao ORDER GATE: weapon pull blocked — no Rando character on table to hold the weapon. Deploy a character first!",
                                -9999.0f);
                            LOG.warn("V67ao ORDER GATE (DeployEvaluator): weapon pull '{}' blocked (no chars on table)",
                                actionText);
                        } else if (v149IsLightsaberPull && v149AbilityCapableUnarmed == 0) {
                            // V149: pulling a lightsaber but no unarmed [Warrior] with
                            // ability >= 4 to wield it. Don't pull a lightsaber a cantina
                            // alien can't use.
                            action.addReasoning(
                                "V149 NO LIGHTSABER WIELDER: no unarmed [Warrior] ability-4+ character on table — don't pull a lightsaber nobody can wield",
                                -2000.0f);
                            LOG.warn("V149 NO LIGHTSABER WIELDER (DeployEvaluator): '{}' — 0 unarmed [Warrior] ability-4+ chars → -2000",
                                actionText);
                        } else {
                            // V67am +600 pull grant is owned by V192 in ActionTextEvaluator.
                            // The former DeployEvaluator grant double-counted weapon pulls and was
                            // removed 2026-07-13; git preserves it. Live V67ar/V67ao/V149
                            // vetoes above remain unchanged, and V192 repeats them structurally
                            // before emitting the same +600 weapon tier.
                            LOG.info("V67am weapon pull detected for '{}' — grant owned by V192 (ActionTextEvaluator)",
                                actionText);
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
                                    action.addReasoning(String.format(
                                        "V51 CC ARMY: Deploy to %s pre-flip — build Cloud City army!",
                                        ccLoc.getTitle()), 500.0f);
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
                                    action.addReasoning(String.format(
                                        "V51 OBJ FIRST: Deploy to %s — objective-relevant location pre-flip!",
                                        ofLoc.getTitle()), 300.0f);
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
                            if (akObj.isStrategyKeyCharacter(game, context.getPlayerId(), card.getTitle())) {
                                // Check the matched token is NOT already on table as a card
                                // that satisfies the same key-character role.
                                String akCardTitleLower = card.getTitle().toLowerCase(Locale.ROOT);
                                boolean alreadyOnTable = false;
                                for (PhysicalCard exist : gameState.getAllPermanentCards()) {
                                    if (exist == null || exist.getBlueprint() == null) continue;
                                    if (!context.getPlayerId().equals(exist.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone ez = exist.getZone();
                                    if (ez == null || !ez.isInPlay()) continue;
                                    if (exist.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    String et = exist.getTitle();
                                    if (et == null) continue;
                                    String etLower = et.toLowerCase(Locale.ROOT);
                                    // Persona-style match: any strategy token that appears in
                                    // BOTH the candidate card's title AND an existing-on-table
                                    // card's title means the role is already filled.
                                    for (String tok : akObj.getStrategyCharacterTokens(game, context.getPlayerId())) {
                                        if (akCardTitleLower.contains(tok) && etLower.contains(tok)) {
                                            alreadyOnTable = true;
                                            break;
                                        }
                                    }
                                    if (alreadyOnTable) break;
                                }
                                if (!alreadyOnTable) {
                                    action.addReasoning(String.format(
                                        "V67ak KEY CHARACTER: %s is named in objective/epic-event text — deploy first to enable flip!",
                                        card.getTitle()), 800.0f);
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

                    // === V22.7: CLOUD CITY OCCUPATION GUARD ===
                    // Cloud City Occupation self-cancels if we don't occupy Bespin system.
                    // Don't waste the deploy — block it until we actually occupy Bespin.
                    // Also check Dark Deal (V) which has similar Bespin requirements.
                    if (cardTitleLower.contains("cloud city occupation") || cardTitleLower.contains("dark deal")) {
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
                            action.addReasoning("V22.7 BLOCKED: " + card.getTitle() +
                                " will SELF-CANCEL — we don't occupy Bespin system!", -800.0f);
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
                            if (!effectAlreadyOnTable) {
                                action.addReasoning("V24 TDIGWATT ENGINE: Deploy " + card.getTitle() +
                                    " NOW — enables objective damage engine!", 300.0f);
                                LOG.warn("V24 TDIGWATT ENGINE: {} gets +300 — CRITICAL engine piece, deploy ASAP!", card.getTitle());
                            } else {
                                action.addReasoning("V22.7: We occupy Bespin — safe to deploy " + card.getTitle(), 50.0f);
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
                    if (category == CardCategory.WEAPON && gameState != null) {
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

                            if (v158Criteria != null && matchUnarmed == 0) {
                                // 2026-05-29 FIX (Steve, Dooku-deck stuck loop): the original
                                // gate required matchArmed > 0 too, which meant when criteria
                                // parsed but the persona wasn't on the table AT ALL (matchArmed
                                // == 0 AND matchUnarmed == 0), this branch didn't fire — Rando
                                // fell through to "unarmed wielder available +300" because some
                                // OTHER (non-matching) character was unarmed. The +300 made
                                // Rando commit to "Play Dooku's Lightsaber" as the outer pick,
                                // then the sub-decision asks "where to attach" with no legal
                                // criteria-matching targets, Rando hits Done, engine re-asks
                                // → infinite Done loop. Now: matchUnarmed==0 blocks regardless
                                // of matchArmed. Two sub-cases for the log message:
                                String v158BlockWhy = matchArmed > 0
                                    ? String.format("every '%s' wielder (%d) already armed", v158Criteria, matchArmed)
                                    : String.format("no '%s' friendly on table at all — deploy has no legal target", v158Criteria);
                                action.addReasoning(
                                    "V158 WEAPON BLOCK: " + v158BlockWhy + " — hold it", -9999.0f);
                                LOG.warn("V158 WEAPON BLOCK ({}): {} criteria='{}' matchArmed={} matchUnarmed=0 → -9999",
                                    matchArmed > 0 ? "criteria all armed" : "criteria absent",
                                    card.getTitle(), v158Criteria, matchArmed);
                            } else if (v158IsLightsaber && unarmedWarrior4 == 0) {
                                action.addReasoning(
                                    "V158 WEAPON BLOCK: lightsaber but no unarmed [Warrior] ability-4 wielder — hold it", -9999.0f);
                                LOG.warn("V158 WEAPON BLOCK (no lightsaber wielder): {} → -9999", card.getTitle());
                            } else if (totalUnarmed == 0 && totalArmed > 0) {
                                action.addReasoning(
                                    "V158 WEAPON BLOCK: every character already armed — no 2nd weapon", -9999.0f);
                                LOG.warn("V158 WEAPON BLOCK (all armed): {} totalArmed={} → -9999", card.getTitle(), totalArmed);
                            } else if (totalUnarmed > 0 || matchUnarmed > 0) {
                                action.addReasoning(
                                    "V158 WEAPON DEPLOY: unarmed wielder available — arm them", 300.0f);
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

                            if (isNamedWeapon) {
                                // Named weapon gets priority boost
                                action.addReasoning("V33 NAMED WEAPON: Character-specific weapon — deploy priority!", 200.0f);
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
                                                action.addReasoning(String.format(
                                                    "V33 NAMED WEAPON WAIT: %s has named weapon %s in hand — save the slot!",
                                                    targetCharName, hc.getTitle()), -400.0f);
                                                LOG.warn("V33 NAMED WEAPON WAIT: Generic {} blocked on {} — named {} in hand!",
                                                    card.getTitle(), targetCharName, hc.getTitle());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V33 NAMED WEAPON: Error: {}", e.getMessage());
                        }
                    }

                    // Characters with high ability are valuable
                    if (category == CardCategory.CHARACTER && abilityVal >= 4) {
                        action.addReasoning("High-ability character", 25.0f);
                    }

                    // === V24.1C: GHERANT DEPLOY BONUS ===
                    // Commander Gherant pulls an Executor site when deployed.
                    // That's a FREE location = force generation. Treat him almost like deploying a location.
                    if (category == CardCategory.CHARACTER && cardTitleLower.contains("gherant")) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer gherantObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (gherantObjAnalyzer != null && gherantObjAnalyzer.isAnalyzed()
                            && gherantObjAnalyzer.needsBespinSystemPresence()) {
                            action.addReasoning("V24.1 GHERANT: Deploys an Executor site — free location + force generation!", 150.0f);
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

                                if (isLandoDeploy) {
                                    if (haveCharAtCCSite) {
                                        action.addReasoning("V29.2 LANDO: Key piece + backup present — safe to deploy!", 200.0f);
                                        LOG.warn("V29.2 LANDO: +200 — has backup at CC site! (actionText='{}')", actionText);
                                    } else {
                                        // V47: Lando alone at CC gets clobbered EVERY TIME. HARD BLOCK.
                                        action.addReasoning("V47 LANDO SOLO BLOCK: No friendlies at CC — Lando dies alone!", -9999.0f);
                                        LOG.warn("V47 LANDO SOLO BLOCK: No friendly chars at CC — blocking Lando reserve deploy! (actionText='{}')", actionText);
                                    }
                                } else if (isLobotDeploy) {
                                    if (haveCharAtCCSite) {
                                        action.addReasoning("V29.2 LOBOT: Helps flip TDIGWATT + backup present!", 150.0f);
                                        LOG.warn("V29.2 LOBOT: +150 — has backup!");
                                    } else {
                                        // V47: Same as Lando — don't deploy Lobot alone either
                                        action.addReasoning("V47 LOBOT SOLO BLOCK: No friendlies at CC — Lobot dies alone!", -9999.0f);
                                        LOG.warn("V47 LOBOT SOLO BLOCK: No friendly chars at CC — blocking Lobot reserve deploy!");
                                    }
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

                                    if (unoccupiedObjLocs > 0 && deploysToUnoccupiedObjLoc) {
                                        // V36: DEFEND YOUR TERRITORY — objective sites left empty get
                                        // occupied by opponent Jedi who drain 2-3 per turn. Malachor sites
                                        // must have presence BEFORE the opponent gets there.
                                        // On turns 1-3, this is the #1 priority for Inquisitors.
                                        float defendBonus = 250.0f;
                                        int turnNum = context.getTurnNumber();
                                        boolean isHuntDown36 = flipObjAnalyzer.isHuntDownV();
                                        String deployCardLower36 = card.getTitle() != null
                                            ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                                        boolean isInquisitor36 = isInquisitor(deployCardLower36);

                                        if (isHuntDown36 && turnNum <= 3) {
                                            // Early game Hunt Down — CRITICAL to defend Malachor
                                            defendBonus = 800.0f; // V36: Overrides Hunt Block -2000
                                            if (isInquisitor36) defendBonus = 1000.0f; // Inquisitors are ideal defenders
                                            LOG.warn("V36 DEFEND MALACHOR: {} to empty obj site EARLY (turn {}) — must defend! (+{})",
                                                card.getTitle(), turnNum, (int)defendBonus);
                                        } else if (isHuntDown36) {
                                            // Later turns — still important but less urgent
                                            defendBonus = 500.0f;
                                            LOG.warn("V36 DEFEND TERRITORY: {} to empty obj site (turn {}) — +{}",
                                                card.getTitle(), turnNum, (int)defendBonus);
                                        }

                                        action.addReasoning(String.format(
                                            "V36 DEFEND TERRITORY: Deploy to unoccupied obj location! (%d/%d occupied%s)",
                                            occupiedObjLocs, occupiedObjLocs + unoccupiedObjLocs,
                                            isHuntDown36 && turnNum <= 3 ? " — EARLY DEFENSE CRITICAL" : ""), defendBonus);
                                        LOG.warn("V36 PRE-FLIP: {} to unoccupied obj loc (+{}) — {}/{} occupied",
                                            card.getTitle(), (int)defendBonus, occupiedObjLocs, occupiedObjLocs + unoccupiedObjLocs);
                                    } else if (unoccupiedObjLocs > 0) {
                                        // Mild penalty for deploying to already-occupied location when
                                        // unoccupied objective locations still need presence
                                        action.addReasoning(String.format(
                                            "V31 PRE-FLIP: %d obj locations still unoccupied — spread out instead of stacking!",
                                            unoccupiedObjLocs), -50.0f);
                                    }
                                } else {
                                    // === POST-FLIP: Consolidate to fewer locations ===
                                    // After flipping, we only need to HOLD enough locations to prevent flip-back.
                                    // For TDIGWATT: hold Bespin system + 1 CC site = 2 total (not 3).
                                    // Find the 2 strongest objective locations and reinforce those.
                                    java.util.List<PhysicalCard> occupiedObjLocCards = new java.util.ArrayList<>();
                                    java.util.Map<String, Float> objLocPower = new java.util.LinkedHashMap<>();
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
                                            occupiedObjLocCards.add(loc);
                                            objLocPower.put(loc.getTitle(), ourPower);
                                        }
                                    }

                                    // Find the 2 locations with most power (these are the ones to hold)
                                    java.util.Set<String> holdLocations = new java.util.HashSet<>();
                                    for (int holdIdx = 0; holdIdx < 2 && !objLocPower.isEmpty(); holdIdx++) {
                                        String bestLoc = null;
                                        float bestPwr = -1;
                                        for (java.util.Map.Entry<String, Float> e : objLocPower.entrySet()) {
                                            if (e.getValue() > bestPwr) {
                                                bestPwr = e.getValue();
                                                bestLoc = e.getKey();
                                            }
                                        }
                                        if (bestLoc != null) {
                                            holdLocations.add(bestLoc);
                                            objLocPower.remove(bestLoc);
                                        }
                                    }

                                    // Check if deploy target is one of the hold locations
                                    boolean deploysToHoldLoc = false;
                                    for (String holdLoc : holdLocations) {
                                        if (actionLower.contains(holdLoc.toLowerCase(Locale.ROOT))) {
                                            deploysToHoldLoc = true;
                                            break;
                                        }
                                    }

                                    if (deploysToHoldLoc) {
                                        action.addReasoning("V31 POST-FLIP: Reinforce key hold location!", 200.0f);
                                        LOG.warn("V31 POST-FLIP: {} reinforcing hold location (+200)", card.getTitle());
                                    } else {
                                        // Deploying to a non-hold objective location post-flip — mild penalty
                                        boolean deploysToAnyObjLoc = false;
                                        for (String frag : objLocFragments) {
                                            if (actionLower.contains(frag.toLowerCase(Locale.ROOT))) {
                                                deploysToAnyObjLoc = true;
                                                break;
                                            }
                                        }
                                        if (deploysToAnyObjLoc && occupiedObjLocCards.size() > 2) {
                                            action.addReasoning("V40 POST-FLIP: Deploying to 3rd obj loc (neutral)", 0.0f);
                                            LOG.warn("V40 POST-FLIP: {} deploy to 3rd obj loc — neutral (was -100)", card.getTitle());
                                        }
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

                                if (totalAfterDeploy >= 4.0f) {
                                    // Good — we'll have enough ability for battle destiny
                                    if (friendlyCharCount > 0 && currentAbilityAtSite < 4.0f) {
                                        // Even better — this deploy FIXES an ability deficit!
                                        action.addReasoning(String.format(
                                            "V32 ABILITY FIX: Deploy brings ability from %.0f to %.0f (>= 4) at %s!",
                                            currentAbilityAtSite, totalAfterDeploy, loc.getTitle()), 150.0f);
                                        LOG.warn("V32 ABILITY FIX: {} (ability {}) fixes deficit at {} (was {}, now {})",
                                            card.getTitle(), cardAbility, loc.getTitle(), currentAbilityAtSite, totalAfterDeploy);
                                    }
                                } else if (friendlyCharCount == 0) {
                                    // Solo deploy with ability < 4 — check hand for follow-up
                                    boolean canFollowUp = false;
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

                                    if (!canFollowUp) {
                                        // NO follow-up available — this deploy strands ability < 4
                                        action.addReasoning(String.format(
                                            "V40 ABILITY: Solo deploy with ability %.0f < 4 at %s — deploy anyway",
                                            cardAbility, loc.getTitle()), 0.0f);
                                        LOG.warn("V32 ABILITY RISK: {} (ability {}) solo at {} with no follow-up — penalized (-200)",
                                            card.getTitle(), cardAbility, loc.getTitle());
                                    } else {
                                        // Follow-up exists in hand — mild caution (deploy order matters)
                                        action.addReasoning(String.format(
                                            "V40 ABILITY: Solo ability %.0f < 4 at %s — follow-up in hand, deploy freely",
                                            cardAbility, loc.getTitle()), 0.0f);
                                    }
                                } else {
                                    // Deploying to a site with friendlies but total still < 4
                                    action.addReasoning(String.format(
                                        "V40 ABILITY: Total ability %.0f still < 4 at %s after deploy (neutral)",
                                        totalAfterDeploy, loc.getTitle()), 0.0f);
                                    LOG.warn("V32 ABILITY WARNING: {} to {} — total ability {} still < 4!",
                                        card.getTitle(), loc.getTitle(), totalAfterDeploy);
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
                                    if (v67agHasFriendly) {
                                        action.addReasoning(String.format(
                                            "V67ag NON-BG STACK PENALTY: %s already has %s — additional character at non-BG can't battle, deploys to a battleground instead!",
                                            loc.getTitle(), v67agExistingTitle), -300.0f);
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

                                if (v33CurrentAbility < RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                    if (v33TotalAfter >= RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                        // This deploy brings us to the buddy threshold!
                                        action.addReasoning(String.format(
                                            "V33 BUDDY FIX: Deploy brings ability from %.0f to %.0f (>= %d) at %s!",
                                            v33CurrentAbility, v33TotalAfter, RandoConfig.ABILITY_BUDDY_THRESHOLD,
                                            loc.getTitle()), 150.0f);
                                        LOG.warn("V33 BUDDY FIX: {} (ability {}) at {} — brings total from {} to {} (>= {})",
                                            card.getTitle(), v33CardAbility, loc.getTitle(),
                                            v33CurrentAbility, v33TotalAfter, RandoConfig.ABILITY_BUDDY_THRESHOLD);
                                    } else if (v33CurrentAbility > 0) {
                                        // Site has friendlies but still below 7 — bonus for reinforcing
                                        action.addReasoning(String.format(
                                            "V33 BUDDY BONUS: Reinforcing ability at %s (%.0f → %.0f, target %d)",
                                            loc.getTitle(), v33CurrentAbility, v33TotalAfter,
                                            RandoConfig.ABILITY_BUDDY_THRESHOLD), 100.0f);
                                        LOG.warn("V33 BUDDY BONUS: {} reinforcing {} — ability {} → {}",
                                            card.getTitle(), loc.getTitle(), v33CurrentAbility, v33TotalAfter);
                                    }
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

                                if (matchingShipInHand) {
                                    // CASE 1: Pilot + matching ship BOTH in hand → deploy together NOW!
                                    action.addReasoning(String.format(
                                        "V30 MATCHING COMBO: %s + %s both in hand — deploy together NOW!",
                                        card.getTitle(), matchingShipName), 1000.0f);
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
                                                    action.addReasoning(String.format(
                                                        "V30 OBJECTIVE SYSTEM: Deploy to %s — matches objective location!",
                                                        objLoc), 1000.0f);
                                                    LOG.warn("V30 OBJECTIVE SYSTEM: {} deploying to objective location '{}' — +1000!",
                                                        card.getTitle(), objLoc);
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                } else if (matchingShipInPlay) {
                                    // CASE 2: Matching ship already in play → deploy pilot to it!
                                    action.addReasoning(String.format(
                                        "V30 MATCHING SHIP IN PLAY: %s is deployed — get %s aboard!",
                                        matchingShipName, card.getTitle()), 300.0f);
                                    LOG.warn("V30 MATCHING SHIP: {} in play — deploy {} as pilot (+300)!",
                                        matchingShipName, card.getTitle());

                                } else if (matchingShipInReserve) {
                                    // CASE 3: Matching ship in reserve — check if AMSD can pull it
                                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle matchOracle = context.getDeckOracle();
                                    boolean amsdInPlay = false;
                                    if (matchOracle != null && matchOracle.isAnalyzed()) {
                                        amsdInPlay = matchOracle.isCardInPlay("Alert My Star Destroyer")
                                            || matchOracle.isCardInPlay("Alert My Star Destroyer!");
                                    }
                                    if (amsdInPlay) {
                                        // Prefer AMSD pull but allow manual fallback (soft penalty, NOT hard block)
                                        action.addReasoning(String.format(
                                            "V30 AMSD AVAILABLE: %s in reserve + AMSD on table — prefer AMSD pull, manual OK as fallback",
                                            matchingShipName), -500.0f);
                                        LOG.warn("V30 AMSD: {} in reserve — soft penalty (-500), prefer AMSD but not hard-blocked",
                                            matchingShipName);
                                    } else {
                                        // No AMSD — deploy pilot normally, ship will come later
                                        LOG.info("V30 MATCHING: {} in reserve but no AMSD — deploy {} normally",
                                            matchingShipName, card.getTitle());
                                    }
                                }
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

                            if (matchingPilotInHand) {
                                // Ship + matching pilot both in hand → deploy together!
                                action.addReasoning(String.format(
                                    "V30 MATCHING COMBO: %s + pilot %s both in hand — deploy together NOW!",
                                    card.getTitle(), matchingPilotName), 1000.0f);
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
                                                action.addReasoning(String.format(
                                                    "V30 OBJECTIVE SYSTEM: Deploy to %s — matches objective!",
                                                    objLoc), 1000.0f);
                                                LOG.warn("V30 OBJECTIVE SYSTEM: {} to objective location '{}' — +1000!",
                                                    card.getTitle(), objLoc);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
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
                    //   (A) Deploying a VEHICLE: soft-block (-1500) if no pilot-capable
                    //       character in hand AND no candidate pilot on table to crew it.
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
                                int availForce = context.getForcePileSize();
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
                                        // and other modifiers are not modeled here (conservative —
                                        // mirrors the existing V35.6 affordability check pattern at
                                        // line 4840-4846).
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
                                    // Distinguish the two failure modes in logs for triage:
                                    String reason;
                                    if (hasPilotInHand) {
                                        reason = String.format(
                                            "pilot in hand but unaffordable (vehicle=%d, force=%d) — wait for force",
                                            vehicleCost, availForce);
                                    } else {
                                        reason = "no Icon.PILOT or Trooper character available";
                                    }
                                    action.addReasoning(
                                        "VEHICLE/SHIP NEEDS PILOT: " + reason + " — useless solo",
                                        -1500.0f);
                                    LOG.warn("VEHICLE/SHIP NEEDS PILOT ({}): {} deploy blocked (-1500)",
                                        hasPilotInHand ? "pilot unaffordable" : "no pilot", card.getTitle());
                                }
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
                                    action.addReasoning(
                                        "PILOT FOR UNMANNED VEHICLE/SHIP: '" + unmannedTitle
                                        + "' on table without a pilot — get this pilot aboard!",
                                        400.0f);
                                    LOG.warn("PILOT FOR UNMANNED VEHICLE/SHIP: {} can pilot {} on table → +400",
                                        card.getTitle(), unmannedTitle);
                                }
                            }
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
                                // Matching pilot in hand and affordable — DEPLOY TOGETHER
                                action.addReasoning(String.format(
                                    "V35.6 NAMED PILOT: %s has matching pilot %s in hand (ability %.0f+%.0f=%.0f) — deploy together!",
                                    card.getTitle(), matchingPilotTitle, shipAbility, matchingPilotAbility, totalAbilityWithPilot),
                                    300.0f);
                                LOG.warn("V35.6 NAMED PILOT: {} + {} — total ability {} (+300)",
                                    card.getTitle(), matchingPilotTitle, totalAbilityWithPilot);
                            }

                            // V35.7: ALL ships with ability < 4 need a pilot. Period.
                            // Even if a pilot CAN help, deploying a ship solo is dangerous
                            // because Rando might not follow up with the pilot deploy.
                            if (shipAbility < 4.0f) {
                                boolean anyPilotHelps = false;
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
                                    // V40: Ship without ability 4 — mild warning
                                    action.addReasoning(String.format(
                                        "V40 SHIP ABILITY: %s ability %.0f — no pilot can reach 4 (mild warning)",
                                        card.getTitle(), shipAbility), -50.0f);
                                    LOG.warn("V40 SHIP ABILITY: {} ability {} — mild warning (-50, was -800)",
                                        card.getTitle(), shipAbility);
                                } else if (!matchingPilotAffordable) {
                                    // V40: Pilot exists but can't afford both — mild warning
                                    action.addReasoning(String.format(
                                        "V40 SHIP ABILITY: %s needs pilot but can't afford both (mild warning)",
                                        card.getTitle()), -50.0f);
                                    LOG.warn("V40 SHIP ABILITY: {} — pilot exists but unaffordable — mild warning (-50, was -400)",
                                        card.getTitle());
                                } else {
                                    // Pilot exists and affordable — mild warning to deploy together
                                    action.addReasoning(String.format(
                                        "V40 SHIP: %s needs %s aboard for ability 4 — deploy together!",
                                        card.getTitle(), matchingPilotTitle != null ? matchingPilotTitle : "a pilot"), -50.0f);
                                }
                            }
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
                                if (oppShipPower > 0 && oppShipPower > ourShipPower * 1.5f) {
                                    // V40: Ship vs overwhelming opponent — mild caution
                                    float shipPenalty = -100.0f;
                                    action.addReasoning(String.format(
                                        "V40 SHIP CAUTION: %s (power %.0f) vs opponent ships (power %.0f) at %s (mild caution)",
                                        card.getTitle(), ourShipPower, oppShipPower, sysLoc.getTitle()), shipPenalty);
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
                                        // V51: SPY AT DRAIN 2+ = BEST PLAY IN THE GAME
                                        action.addReasoning(String.format(
                                            "V51 SPY CRIPPLE: Spy at %s cuts drain from %.0f — opponent's army is WASTED!",
                                            loc.getTitle(), spyDrain), 1000.0f);
                                        LOG.warn("V51 SPY CRIPPLE: {} to {} (drain {}) — +1000! Best ROI in the game!",
                                            card.getTitle(), loc.getTitle(), (int)spyDrain);
                                    }
                                } else if (ourPower > 0) {
                                    deploysToFriendlyLoc = true;
                                }
                            }

                            if (deploysToOpponentLoc && !deploysToHighDrainSite) {
                                // Opponent location but drain < 2 — still useful
                                action.addReasoning("V43 SPY TO ENEMY: Deploy spy to opponent location — blocks their drain!", 200.0f);
                                LOG.warn("V43 SPY: {} to opponent location — +200", card.getTitle());
                            } else if (deploysToFriendlyLoc) {
                                action.addReasoning("V43 SPY WASTED: Spy at friendly location does NOTHING — send to opponent!", -500.0f);
                                LOG.warn("V43 SPY WASTED: {} to friendly location — -500", card.getTitle());
                            }

                            // V51: If opponent has NO drain 2+ sites, spy is low priority
                            if (!opponentHasDrain2Plus && !deploysToOpponentLoc) {
                                action.addReasoning("V51 SPY NO TARGET: Opponent has no drain 2+ sites — deploy a fighter instead!", -300.0f);
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

                            if (isEvazan) {
                                // Check if ANY weapon character is already in play
                                boolean weaponPartnerInPlay = comboOracle.isCardInPlay("Maul With Lightsaber")
                                    || comboOracle.isCardInPlay("Vader With Lightsaber")
                                    || comboOracle.isCardInPlay("Mara Jade With Lightsaber")
                                    || comboOracle.isCardInPlay("Jade With Lightsaber")
                                    || comboOracle.isCardInPlay("Aurra Sing With Blaster")
                                    || comboOracle.isCardInPlay("Sing With Blaster");
                                if (weaponPartnerInPlay) {
                                    action.addReasoning("V24.3 EVAZAN COMBO: Weapon character on table — deploy Evazan for kill combo!", 150.0f);
                                    LOG.warn("V24.3 EVAZAN: Weapon partner in play — +150 deploy priority!");
                                }
                            } else if (isWeaponChar) {
                                // Check if Evazan is already in play
                                if (comboOracle.isCardInPlay("Evazan")) {
                                    action.addReasoning("V24.3 EVAZAN COMBO: Dr. Evazan on table — deploy weapon character for kill combo!", 100.0f);
                                    LOG.warn("V24.3 WEAPON CHAR: Evazan in play — +100 deploy priority for {}!", card.getTitle());
                                }
                            }
                        }
                    }

                    // Starships and vehicles for board presence
                    if (category == CardCategory.STARSHIP || category == CardCategory.VEHICLE) {
                        action.addReasoning("Starship/Vehicle deployment", 15.0f);

                        // === V24.6A+V24.9: EXECUTOR DEPLOY PRIORITY ===
                        // Executor is THE key ship for TDIGWATT — it force drains at Bespin,
                        // enables Dark Deal + CC Occupation. If it's in hand, deploy it NOW.
                        // V24.9: MUST come out turn 1 or 2 at the latest. If AMSD didn't pull it
                        // from reserve, deploy it manually from hand — no excuses.
                        if (cardTitleLower.contains("executor") || cardTitleLower.contains("flagship")) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer execObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (execObjAnalyzer != null && execObjAnalyzer.isAnalyzed()
                                && execObjAnalyzer.needsBespinSystemPresence()) {

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
                                    // HARD BLOCK: Executor without Bespin is useless — deploy Bespin first!
                                    action.addReasoning("V24.10 EXECUTOR BLOCKED: Bespin system NOT on table — deploy Bespin FIRST!", -9999.0f);
                                    LOG.warn("V24.10 EXECUTOR BLOCKED: {} in hand but Bespin not on table — CANNOT deploy to wrong system!", card.getTitle());
                                } else {
                                    int execTurn = context.getTurnNumber();
                                    if (execTurn <= 2) {
                                        // Turns 1-2: MAXIMUM priority — Executor MUST come out now
                                        action.addReasoning("V24.9 EXECUTOR CRITICAL: Bespin on table — MUST deploy NOW!", 800.0f);
                                        LOG.warn("V24.9 EXECUTOR CRITICAL: {} on turn {} + Bespin on table — MAXIMUM priority (+800)!", card.getTitle(), execTurn);
                                    } else {
                                        action.addReasoning("V24.6 EXECUTOR: Key ship for TDIGWATT — deploy to Bespin!", 800.0f);
                                        LOG.warn("V24.6 EXECUTOR: {} in hand + Bespin on table — deploy priority (+800)!", card.getTitle());
                                    }
                                }
                            }
                        }

                        // V22.5: BESPIN SYSTEM SHIP PRIORITY
                        // For objectives that reference Bespin/Cloud City (like TDIGWATT),
                        // having a ship at Bespin system is critical for enabling Dark Deal
                        // and Cloud City Occupation. Prioritize ship deployment if no ship there yet.
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer shipObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (shipObjAnalyzer != null && shipObjAnalyzer.isAnalyzed() && shipObjAnalyzer.needsBespinSystemPresence()) {
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
                                    // Opponent controls Bespin — URGENT contestation needed
                                    action.addReasoning("V23 BESPIN CONTEST: Opponent controls Bespin — deploy ship to contest IMMEDIATELY!", 300.0f);
                                    LOG.warn("V23 BESPIN CONTEST: {} gets +300 — opponent has presence at Bespin!", card.getTitle());
                                } else {
                                    // No opponent but we still need ship presence for objective
                                    action.addReasoning("V23 BESPIN CRITICAL: Deploy ship to enable Dark Deal + CC Occupation!", 250.0f);
                                    LOG.warn("V23 BESPIN SHIP: {} gets +250 — no ship at Bespin system yet!", card.getTitle());
                                }
                            }
                        }
                    }

                    // === PILOT BONUS ===
                    if (AiCardHelper.isPilot(card)) {
                        action.addReasoning("Pilot character", 10.0f);
                    }

                    // === V41.2: PIETT DEPLOY — HOLD FOR AMSD ===
                    // Piett is the matching pilot for Executor. He should NEVER deploy to ground
                    // when AMSD is on the table and Executor is still available — AMSD needs Piett
                    // IN HAND to fire. Deploying Piett to ground wastes the AMSD + Executor combo.
                    if (cardTitleLower.contains("piett") || cardTitleLower.contains("gherant")) {
                        boolean deployingAboardShip = actionLower.contains("aboard") || actionLower.contains("pilot")
                            || actionLower.contains("executor") || actionLower.contains("simultaneously");
                        if (deployingAboardShip) {
                            action.addReasoning("V40.1 PILOT ABOARD: Deploy aboard ship!", 300.0f);
                        } else {
                            // V47: Executor pilots should NEVER deploy to ground solo — they're too weak
                            // alone and too valuable as Executor pilots. Block ALL ground deploys.
                            action.addReasoning("V47 EXECUTOR PILOT GROUND BLOCK: " + card.getTitle()
                                + " must deploy aboard a ship, not to ground!", -9999.0f);
                            LOG.warn("V47 EXECUTOR PILOT GROUND BLOCK: {} — blocking ground deploy, pilots belong on ships!",
                                card.getTitle());
                        }
                    }

                    // === MATCHING PILOT CHECK ===
                    if (actionLower.contains("matching")) {
                        action.addReasoning("Matching pilot/ship synergy", 30.0f);
                    }

                    // === STRATEGIC BONUSES ===
                    if (needsReinforcement) {
                        action.addReasoning("Need to reinforce board", 20.0f);
                    }

                    // Low life force - be more aggressive
                    if (lifeForce <= RandoConfig.CRITICAL_LIFE_FORCE) {
                        action.addReasoning("Critical life force - must deploy!", 30.0f);
                    }
                }
            } else {
                // Unknown card - check if we should block it
                // V29: NEVER block if earlyCard resolved to a LOCATION — locations always deploy
                boolean earlyCardIsLocation = (earlyCard != null && earlyCard.getBlueprint() != null
                    && earlyCard.getBlueprint().getCardCategory() == CardCategory.LOCATION);
                if (earlyCardIsLocation) {
                    LOG.info("V29: Unknown to main lookup but earlyCard is LOCATION '{}' — allowing!",
                        earlyCard.getTitle());
                    action.addReasoning("V29: Location deploy — always allowed!", 200.0f);
                } else if (plan != null && plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS && !plan.isForceAllowExtras()) {
                    // During DEPLOY_LOCATIONS, block unknown non-location actions on turn 1 only.
                    // V29.7: After turn 1, allow with penalty — blocking everything causes zero deploys!
                    if (context.getTurnNumber() <= 1) {
                        LOG.warn("🚫 BLOCKING unknown card deploy during DEPLOY_LOCATIONS plan (turn 1)");
                        action.addReasoning("V40: Unknown card during DEPLOY_LOCATIONS (neutral)", 0.0f);
                        actions.add(action);
                        continue;
                    } else {
                        LOG.info("V29.7: Unknown card during DEPLOY_LOCATIONS but turn {} — allowing with penalty", context.getTurnNumber());
                        action.addReasoning("V40: DEPLOY_LOCATIONS incomplete turn " + context.getTurnNumber() + " — deploy freely", 0.0f);
                    }
                }
                // V26: Unknown card from reserve deck — can't evaluate stats, buddy system,
                // maintenance, or plan alignment. Penalize enough to not auto-beat pass.
                // The base score is +50, so -60 brings it to -10 total, below pass (~2).
                // Specific reserve deploy actions (Vader Castle, I'm Sorry, etc.) get their
                // own scoring in ActionTextEvaluator to override this when appropriate.
                action.addReasoning("V40: Unknown card (deploy from reserve?) — deploy freely", 0.0f);
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

            // === V52 FIX 11: DEPLOY MOMENTUM — Bonus for deploying multiple cards same turn ===
            // Check how much force has been used this deploy phase. If we've already spent
            // force (meaning cards already deployed), give bonus to keep the momentum going.
            // Initial force = force pile + force already spent. Current = force pile now.
            // We approximate "force spent" by comparing current force pile to hand-implied max.
            {
                int currentForcePile = context.getForcePileSize();
                int handSizeMomentum = hand != null ? hand.size() : 0;
                // Heuristic: if force pile is much less than life force ratio, we've been spending
                // Use a simpler approach: count cards deployed this phase from plan
                int forceSpentApprox = 0;
                if (plan != null && plan.getDeploymentsMade() > 0) {
                    // Each deployment costs ~3-5 force on average
                    forceSpentApprox = plan.getDeploymentsMade() * 4;
                }
                // Also check: if force pile started higher (we can infer from activations)
                // Simpler: just check if we've already deployed cards this turn
                if (plan != null && plan.getDeploymentsMade() >= 1) {
                    float momentumBonus = 100.0f;
                    if (plan.getDeploymentsMade() >= 2) momentumBonus = 150.0f;
                    if (plan.getDeploymentsMade() >= 3) momentumBonus = 200.0f;
                    action.addReasoning(String.format(
                        "V52 MOMENTUM: Already deployed %d cards this turn — keep deploying! (+%.0f)",
                        plan.getDeploymentsMade(), momentumBonus), momentumBonus);
                    LOG.warn("V52 MOMENTUM: {} gets +{} — {} cards already deployed this turn",
                        card != null ? card.getTitle() : actionText, (int)momentumBonus, plan.getDeploymentsMade());
                }
            }

            // === V52 FIX 12: TDIGWATT TURN 1 SCRIPT ===
            // On turn 1 for TDIGWATT objective, specific cards get massive priority
            // to ensure the bot sets up its engine immediately.
            {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer tdigObjAnalyzer =
                    context.getObjectiveAnalyzer();
                int tdigTurn = context.getTurnNumber();
                if (tdigObjAnalyzer != null && tdigObjAnalyzer.isAnalyzed()
                    && tdigObjAnalyzer.needsBespinSystemPresence()
                    && !tdigObjAnalyzer.isHuntDownV() && tdigTurn <= 1) {
                    String tdigTitle = card != null && card.getTitle() != null
                        ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                    if (tdigTitle.isEmpty() && cardTitleFromGemp != null) {
                        tdigTitle = cardTitleFromGemp.toLowerCase(Locale.ROOT);
                    }
                    if (!tdigTitle.isEmpty()) {
                        if (tdigTitle.contains("bespin") && actionLower.contains("system")) {
                            action.addReasoning("V52 TDIGWATT T1: Bespin system — FOUNDATION!", 1500.0f);
                            LOG.warn("V52 TDIGWATT T1: Bespin system +1500");
                        } else if (tdigTitle.contains("cloud city") || actionLower.contains("i'm sorry")
                                   || actionLower.contains("i am sorry")) {
                            action.addReasoning("V52 TDIGWATT T1: Cloud City site via I'm Sorry!", 1200.0f);
                            LOG.warn("V52 TDIGWATT T1: Cloud City site +1200");
                        } else if (tdigTitle.contains("lando") && tdigTitle.contains("broker")) {
                            action.addReasoning("V52 TDIGWATT T1: Lando as Broker — key engine piece!", 1000.0f);
                            LOG.warn("V52 TDIGWATT T1: Lando as Broker +1000");
                        } else if (tdigTitle.contains("executor") || tdigTitle.contains("flagship")) {
                            action.addReasoning("V52 TDIGWATT T1: Executor/Flagship — Bespin control!", 900.0f);
                            LOG.warn("V52 TDIGWATT T1: Executor/Flagship +900");
                        } else if (tdigTitle.contains("chiraneau")) {
                            action.addReasoning("V52 TDIGWATT T1: Chiraneau — pilot for Executor!", 850.0f);
                            LOG.warn("V52 TDIGWATT T1: Chiraneau +850");
                        }
                    }
                }
            }

            // === V54 FIX 16: SKYWALKER SAGA EPIC EVENT T1-3 SCRIPT ===
            // Mirror of V52 TDIGWATT T1 block, but for the Skywalker Saga Epic Event
            // deck (also known by its key effect "Like My Father Before Me"). Rando
            // has been losing badly with this deck because no script drives the
            // turn-1 ramp. Priorities:
            //   PRIORITY 1 = Tatooine sites (Cantina/Mos Eisley/Lars' Moisture Farm)
            //   PRIORITY 2 = Young Skywalker (or any Luke persona)
            //   PRIORITY 3 = Luke's Lightsaber from hand
            //
            // DETECTION (V54.1): Skywalker Saga is an Epic Event deck — its
            // objective-slot card is Anger/Fear/Aggression (V), which has
            // cardType=EFFECT not OBJECTIVE. ObjectiveAnalyzer only detects true
            // OBJECTIVE cards, so we can't rely on getObjectiveTitle(). Detect
            // the deck by its unique starting-location signature instead: Endor:
            // Anakin's Funeral Pyre (217_34) on our side of the table.
            {
                int lsTurn = context.getTurnNumber();
                GameState lsGs = context.getGameState();
                boolean isLukeSaga = false;
                if (lsGs != null && lsTurn <= 3) {
                    try {
                        for (PhysicalCard loc : lsGs.getLocationsInOrder()) {
                            if (loc == null) continue;
                            String locTitle = loc.getTitle();
                            if (locTitle != null
                                && locTitle.toLowerCase(Locale.ROOT).contains("anakin's funeral pyre")) {
                                isLukeSaga = true;
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (isLukeSaga) {
                        String lsCardTitle = (card != null && card.getTitle() != null)
                            ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                        if (lsCardTitle.isEmpty() && cardTitleFromGemp != null) {
                            lsCardTitle = cardTitleFromGemp.toLowerCase(Locale.ROOT);
                        }
                        String lsActionLower = actionText.toLowerCase(Locale.ROOT);

                        // Turn-scaled: priority is highest on T1, still important T2-3
                        float turnMult = lsTurn == 1 ? 1.0f : (lsTurn == 2 ? 0.85f : 0.7f);

                        // PRIORITY 1: Tatooine SITES (drain engine). Cantina is the king
                        // (shuttle to/from Mos Eisley during Control phase).
                        if (lsCardTitle.contains("tatooine: cantina") || lsCardTitle.equals("cantina")) {
                            float s = 1500.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Tatooine: Cantina — drain engine!", s);
                            LOG.warn("V54 LMFBM T{}: Tatooine: Cantina +{}", lsTurn, (int)s);
                        } else if (lsCardTitle.contains("mos eisley")) {
                            float s = 1500.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Tatooine: Mos Eisley — Cantina shuttle!", s);
                            LOG.warn("V54 LMFBM T{}: Tatooine: Mos Eisley +{}", lsTurn, (int)s);
                        } else if (lsCardTitle.contains("lars") && lsCardTitle.contains("moisture")) {
                            float s = 1500.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Lars' Moisture Farm — Tatooine site!", s);
                            LOG.warn("V54 LMFBM T{}: Lars' Moisture Farm +{}", lsTurn, (int)s);
                        }
                        // Any other Tatooine battleground site
                        else if (lsCardTitle.startsWith("tatooine:") && !lsCardTitle.contains("jabba")) {
                            float s = 1300.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Tatooine battleground site!", s);
                            LOG.warn("V54 LMFBM T{}: {} (Tatooine site) +{}", lsTurn, lsCardTitle, (int)s);
                        }
                        // Tatooine SYSTEM — secondary, for ship presence (turn 2 target)
                        else if (lsCardTitle.equals("tatooine") && lsActionLower.contains("system")) {
                            float s = 900.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Tatooine system — ship presence!", s);
                            LOG.warn("V54 LMFBM T{}: Tatooine system +{}", lsTurn, (int)s);
                        }

                        // PRIORITY 2: Young Skywalker ("I have it" branch) — Luke persona
                        else if (lsCardTitle.contains("young skywalker")) {
                            float s = 1200.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Young Skywalker — Luke persona (I have it)!", s);
                            LOG.warn("V54 LMFBM T{}: Young Skywalker +{}", lsTurn, (int)s);
                        }
                        // Any Luke persona (covers Son Of Skywalker, Jedi Knight, etc.)
                        else if (lsCardTitle.contains("luke") && card != null && card.getBlueprint() != null
                                 && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                            float s = 1100.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Luke persona — deploy for drain power!", s);
                            LOG.warn("V54 LMFBM T{}: {} (Luke persona) +{}", lsTurn, lsCardTitle, (int)s);
                        }

                        // PRIORITY 3: Luke's Lightsaber — arm Luke from hand
                        else if (lsCardTitle.contains("luke's lightsaber")) {
                            float s = 1100.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Luke's Lightsaber — arm Luke NOW!", s);
                            LOG.warn("V54 LMFBM T{}: Luke's Lightsaber +{}", lsTurn, (int)s);
                        }
                        // NOTE: Lightsaber-from-Reserve pullers (e.g. Gift Of The Mentor) are
                        // NOT given a deploy-phase bonus here — that effect is a BATTLE combo
                        // (Obi-Wan/Yoda buddying Luke for +2 destiny) and should be scored by
                        // the battle/action-text layer, not force-pulled during deploy.
                        // Obi-Wan / Yoda as buddy Jedi for Luke (optional support, lower priority)
                        else if ((lsCardTitle.contains("obi-wan") || lsCardTitle.contains("yoda"))
                                 && card != null && card.getBlueprint() != null
                                 && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                            float s = 800.0f * turnMult;
                            action.addReasoning("V54 LMFBM T" + lsTurn + ": Jedi buddy for Luke!", s);
                            LOG.warn("V54 SKYWALKER SAGA T{}: {} (Jedi buddy) +{}", lsTurn, lsCardTitle, (int)s);
                        }
                }
            }

            // === V55 FIX 17: HIGH-ABILITY CHARACTER DEPLOY URGENCY ===
            // Generalized replacement for the earlier "Obi-Wan in hand" idea. Any
            // character with ability >= 6 (Jedi/Sith/Lord tier — Vader, Emperor,
            // Obi-Wan, Yoda, Luke, Mace, etc.) rotting in hand is wasted life force.
            // Give it a steady deploy urgency bonus, scaled up in the early game.
            // Side-agnostic, deck-agnostic.
            {
                if (card != null && card.getBlueprint() != null
                    && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                    Float abl = null;
                    try { abl = card.getBlueprint().getAbility(); } catch (Exception e) {}
                    if (abl != null && abl >= 6.0f) {
                        int v55Turn = context.getTurnNumber();
                        float v55Bonus;
                        if (v55Turn <= 3)      v55Bonus = 500.0f;  // early game: deploy now
                        else if (v55Turn <= 6) v55Bonus = 350.0f;  // mid game: still urgent
                        else                   v55Bonus = 200.0f;  // late game: baseline urgency
                        action.addReasoning(
                            "V55 HIGH-ABILITY: " + card.getTitle() + " (ability " + abl.intValue()
                                + ") in hand — deploy, don't hoard!", v55Bonus);
                        LOG.warn("V55 HIGH-ABILITY: {} (ability {}) T{} +{}",
                            card.getTitle(), abl.intValue(), v55Turn, (int)v55Bonus);
                    }
                }
            }

            // === V52b FIX 13: HIDDEN PATH JEDI FLOOD (turns 1-2) ===
            // Deploy Jedi FIRST and FAST. Check both card title AND action text,
            // because Fallen Order deploys Jedi via "Deploy a Jedi Survivor stacked here"
            // where the card is Fallen Order, not the Jedi itself.
            {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer hpObjAnalyzer =
                    context.getObjectiveAnalyzer();
                int hpTurn = context.getTurnNumber();
                if (hpObjAnalyzer != null && hpObjAnalyzer.isAnalyzed()
                    && hpTurn <= 2) {
                    String hpObjTitle = hpObjAnalyzer.getObjectiveTitle();
                    boolean isHiddenPath = hpObjTitle != null
                        && hpObjTitle.toLowerCase(Locale.ROOT).contains("hidden path");
                    if (isHiddenPath) {
                        String hpCardTitle = (card != null && card.getTitle() != null)
                            ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                        if (hpCardTitle.isEmpty() && cardTitleFromGemp != null) {
                            hpCardTitle = cardTitleFromGemp.toLowerCase(Locale.ROOT);
                        }
                        // Also check the ACTION TEXT — Fallen Order says "Deploy a Jedi Survivor"
                        String hpActionLower = actionText.toLowerCase(Locale.ROOT);

                        // Detect Jedi deploy via card OR action text
                        boolean isJediDeploy = false;
                        boolean isJediChar = false;
                        if (card != null && card.getBlueprint() != null
                            && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                            Float hpAbility = null;
                            try { hpAbility = card.getBlueprint().getAbility(); } catch (Exception e) {}
                            // Only count ability >= 6 as true Jedi (excludes Padawans like Sabine ability 4)
                            if (hpAbility != null && hpAbility >= 6) isJediChar = true;
                            // Named Jedi always qualify
                            if (hpCardTitle.contains("obi-wan") || hpCardTitle.contains("quinlan")
                                || hpCardTitle.contains("kelleran") || hpCardTitle.contains("cal kestis")
                                || hpCardTitle.contains("ezra") || hpCardTitle.contains("kanan")
                                || hpCardTitle.contains("ahsoka tano") || hpCardTitle.contains("cere")
                                || hpCardTitle.contains("luke") || hpCardTitle.contains("yoda")) {
                                isJediChar = true;
                            }
                        }
                        // Fallen Order "Deploy a Jedi Survivor" action
                        if (hpActionLower.contains("jedi survivor") || hpActionLower.contains("fallen order")) {
                            isJediDeploy = true;
                        }

                        if (isJediChar) {
                            action.addReasoning("V52b HIDDEN PATH: Jedi character — deploy FIRST!", 800.0f);
                            LOG.warn("V52b HIDDEN PATH: {} (Jedi char) +800 on turn {}", card.getTitle(), hpTurn);
                        } else if (isJediDeploy) {
                            action.addReasoning("V52b HIDDEN PATH: Fallen Order Jedi deploy — deploy FIRST!", 800.0f);
                            LOG.warn("V52b HIDDEN PATH: Fallen Order Jedi deploy +800 on turn {}", hpTurn);
                        } else if (hpCardTitle.contains("lightsaber") || hpCardTitle.contains("shoto")) {
                            action.addReasoning("V52b HIDDEN PATH: Lightsaber — arm the Jedi!", 700.0f);
                            LOG.warn("V52b HIDDEN PATH: {} (lightsaber) +700", hpCardTitle);
                        } else if (hpCardTitle.contains("holocron") || hpActionLower.contains("holocron")) {
                            action.addReasoning("V52b HIDDEN PATH: Jedi Holocron!", 600.0f);
                            LOG.warn("V52b HIDDEN PATH: {} (holocron) +600", hpCardTitle);
                        }
                    }
                }
            }

            // NOTE: Don't add cardIds to pendingDeployCardIds here during evaluation!
            // We used to do: pendingDeployCardIds.add(cardIdStr);
            // But that caused ALL evaluated cards to be marked as "already tried"
            // which broke deployment plans. We now only track when the action is actually chosen.
            // See line ~630 where we track the selected action's cardId.

            LOG.debug("[DeployEvaluator] Scored '{}' -> {} ({})",
                actionText.length() > 50 ? actionText.substring(0, 50) + "..." : actionText,
                String.format("%.1f", action.getScore()),
                action.getReasoningString());

            actions.add(action);
        }

        LOG.info("[DeployEvaluator] Evaluated {} deploy actions", actions.size());
        return actions;
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
