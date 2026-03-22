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
            LOG.debug("[DeployEvaluator] canEvaluate=false: no deploy actions found in {} action texts", actionTexts.size());
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
                    for (PhysicalCard card : hand) {
                        if (card != null) {
                            String bpId = card.getBlueprintId(true);
                            if (bpId != null) {
                                handBlueprintIds.add(bpId);
                            }
                        }
                    }

                    // Find instructions for cards no longer in hand
                    List<DeploymentInstruction> deployedCards = new ArrayList<>();
                    for (DeploymentInstruction instruction : plan.getInstructions()) {
                        if (!handBlueprintIds.contains(instruction.getCardBlueprintId())) {
                            deployedCards.add(instruction);
                        }
                    }

                    // Record deployments for cards that left hand
                    for (DeploymentInstruction instruction : deployedCards) {
                        LOG.info("📋 Auto-detected deployment: {} left hand", instruction.getCardName());
                        planner.recordDeployment(instruction.getCardBlueprintId());
                    }
                }

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
            if (hand != null) {
                for (PhysicalCard handCard : hand) {
                    if (handCard != null) {
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
                if (handBlueprintIds.contains(inst.getCardBlueprintId())) {
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
                String cardIdStr = (cardIdList != null && i < cardIdList.size()) ? cardIdList.get(i) : null;

                // Method 1: Look up card by cardId in game state to get its blueprint
                if (cardIdStr != null && !cardIdStr.isEmpty() && gameState != null) {
                    try {
                        int cardIdNum = Integer.parseInt(cardIdStr);
                        PhysicalCard card = gameState.findCardById(cardIdNum);
                        if (card != null) {
                            bpId = card.getBlueprintId(true);
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
                if (bpId != null && plan.getInstructionForCard(bpId) != null) {
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

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle deploy-related actions
            if (!actionLower.contains("deploy")) {
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.DEPLOY,
                50.0f,  // Base score
                actionText
            );

            // === V29.6: HAND BLOAT — DEPLOY MORE AGGRESSIVELY WHEN HAND IS TOO LARGE ===
            // If Rando has 15+ cards in hand, he's hoarding and not deploying enough.
            // Something is wrong — boost all deploy actions to get cards on the table.
            // Cards in hand do nothing; cards on the table drain/battle/occupy.
            if (hand != null && hand.size() >= 15) {
                float bloatBonus = 50.0f + (hand.size() - 15) * 20.0f; // +50 at 15, +70 at 16, +90 at 17...
                action.addReasoning("V29.6 HAND BLOAT: " + hand.size() + " cards in hand — deploy more aggressively!", bloatBonus);
                LOG.warn("V29.6 HAND BLOAT: {} cards in hand — boosting deploy by +{} (action='{}')", hand.size(), bloatBonus, actionText);
            }

            // === APPLY PHASE-LEVEL PLAN ===
            // V24.10: NEVER hold back on turns 1-2. The engine MUST be built ASAP:
            //   Locations → AMSD (Piett + Executor) → Lando/Lobot → everything else.
            // Holding back early wastes critical setup turns.
            // After turn 2, HOLD_BACK can apply to non-location cards only.
            // Locations are ALWAYS exempt from HOLD_BACK regardless of turn.
            if (plan != null && plan.getStrategy() == DeployStrategy.HOLD_BACK) {
                int holdBackTurn = context.getTurnNumber();
                if (holdBackTurn <= 2) {
                    // Turns 1-2: IGNORE hold-back entirely — build the engine!
                    LOG.warn("V24.10 NO HOLD_BACK TURNS 1-2: Turn {} — ignoring hold-back, must build engine! Action: '{}'",
                        holdBackTurn, actionText);
                    // Fall through to normal scoring
                } else {
                    // Turn 3+: Hold back non-location deploys only
                    boolean isLocationAction = actionLower.contains("location") || actionLower.contains("site")
                        || actionLower.contains("system") || actionLower.contains("sorry");
                    if (!isLocationAction) {
                        List<String> hbCardIds = context.getCardIds();
                        String hbCardIdStr = (hbCardIds != null && i < hbCardIds.size()) ? hbCardIds.get(i) : null;
                        if (hbCardIdStr != null && gameState != null) {
                            try {
                                PhysicalCard hbCard = gameState.findCardById(Integer.parseInt(hbCardIdStr));
                                if (hbCard != null && hbCard.getBlueprint() != null
                                    && hbCard.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
                                    isLocationAction = true;
                                }
                            } catch (NumberFormatException e) { /* ignore */ }
                        }
                    }
                    if (isLocationAction) {
                        LOG.warn("V24.10 HOLD_BACK OVERRIDE: '{}' is a LOCATION deploy — locations ALWAYS deploy!", actionText);
                    } else {
                        action.addReasoning("HOLD BACK: " + plan.getReason(), -150.0f);
                        actions.add(action);
                        continue;  // Skip individual card evaluation - plan says don't deploy
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
                            action.addReasoning(
                                "V29 BESPIN-FIRST: Executor MUST deploy before characters! " +
                                "Get Bespin → Executor/AMSD → THEN characters.", -500.0f);
                            LOG.warn("V29 BESPIN-FIRST: BLOCKING deploy '{}' on turn {} — Bespin not occupied, deploy Executor first!",
                                actionText.length() > 60 ? actionText.substring(0, 60) : actionText, bfTurn);
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

                    // === DEPLOYMENT PLAN SCORING ===
                    // If we have a plan, score based on whether this card is in the plan
                    String blueprintId = card.getBlueprintId(true);

                    if (plan != null) {
                        if (!plan.getInstructions().isEmpty()) {
                            // Plan has pending instructions - check if this card is in plan
                            DeploymentInstruction instruction = plan.getInstructionForCard(blueprintId);

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
                                        // V29.7 SAFETY VALVE: After turn 1, if the DEPLOY_LOCATIONS plan
                                        // hasn't completed, the locations may not be in hand or affordable.
                                        // Don't permanently block characters — that causes Rando to deploy
                                        // NOTHING for the entire game! Allow chars with a small penalty
                                        // so locations are still preferred if available.
                                        LOG.info("V29.7: DEPLOY_LOCATIONS turn {} — allowing character deploy with mild penalty: {}", context.getTurnNumber(), card.getTitle());
                                        action.addReasoning("V29.7: DEPLOY_LOCATIONS incomplete but turn " + context.getTurnNumber() + " — deploy characters!", -20.0f);
                                    } else {
                                        LOG.warn("🚫 BLOCKING non-location deploy during DEPLOY_LOCATIONS plan (turn 1): {}", card.getTitle());
                                        action.addReasoning("BLOCKED: Plan is DEPLOY_LOCATIONS ONLY (turn 1) - deploy locations first!", -1000.0f);
                                        actions.add(action);
                                        continue;  // Skip all other scoring - this action is blocked
                                    }
                                } else if (plan.isWaitingForPlannedCards()) {
                                    // Plan cards are in hand but not affordable - HARD BLOCK non-plan deploys
                                    // We want to PASS and save force for the planned cards!
                                    LOG.warn("🚫 BLOCKING off-plan deploy - saving force for planned cards: {}", card.getTitle());
                                    action.addReasoning("BLOCKED: Saving force for planned cards!", -200.0f);
                                    actions.add(action);
                                    continue;  // Skip all other scoring
                                } else {
                                    action.addReasoning("NOT in deployment plan", -50.0f);
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
                                action.addReasoning("Plan complete but reserving force for battle", -30.0f);
                            }
                        }

                        // Check if this card is in hold-back list
                        if (blueprintId != null && plan.getHoldBackCards().contains(blueprintId)) {
                            action.addReasoning("HOLD BACK - waiting for better opportunity", -80.0f);
                        }
                    }

                    // Get card stats (with type checks for safety)
                    // DeployCost exists on most deployable cards
                    int cost = 0;
                    try {
                        Float deployCost = blueprint.getDeployCost();
                        cost = deployCost != null ? deployCost.intValue() : 0;
                        // V22.3: Maintenance card check - need enough Force for upkeep AFTER deploying
                        // Maintenance cost = card's deploy cost. Must have that much Force
                        // remaining in Force Pile after paying deploy cost, or card dies at end of turn.
                        if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                            int totalForce = context.getGameState() != null ?
                                context.getGameState().getForcePileSize(context.getPlayerId()) : 0;
                            int forceAfterDeploy = totalForce - cost;
                            // Maintenance cost = deploy cost (SWCCG rule)
                            int maintenanceCost = cost;
                            if (forceAfterDeploy < maintenanceCost) {
                                // CANNOT pay maintenance — card WILL be lost at end of turn
                                action.addReasoning("V22.3 MAINTENANCE BLOCKED: need " + maintenanceCost +
                                    " Force for upkeep but only " + forceAfterDeploy + " left after deploy!", -300.0f);
                                LOG.warn("V22.3 MAINTENANCE BLOCKED: {} costs {} to deploy, {} Force available, {} left but needs {} for upkeep!",
                                    blueprint.getTitle(), cost, totalForce, forceAfterDeploy, maintenanceCost);
                            } else if (forceAfterDeploy < maintenanceCost + 2) {
                                // Can barely pay maintenance — risky
                                action.addReasoning("V22.3 Maintenance card - tight on Force for upkeep (" +
                                    forceAfterDeploy + " left, need " + maintenanceCost + ")", -80.0f);
                                LOG.warn("V22.3 MAINTENANCE WARNING: {} only {} Force left after deploy, upkeep needs {}",
                                    blueprint.getTitle(), forceAfterDeploy, maintenanceCost);
                            } else {
                                LOG.info("V22.3 MAINTENANCE OK: {} has {} Force after deploy, upkeep needs {}",
                                    blueprint.getTitle(), forceAfterDeploy, maintenanceCost);
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
                            int existingMaintenanceCost = 0;
                            java.util.List<PhysicalCard> allInPlay = gameState.getAllPermanentCards();
                            if (allInPlay != null) {
                                for (PhysicalCard mCard : allInPlay) {
                                    if (mCard == null) continue;
                                    if (!context.getPlayerId().equals(mCard.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone mZone = mCard.getZone();
                                    if (mZone == null || !mZone.isInPlay()) continue;
                                    SwccgCardBlueprint mBp = mCard.getBlueprint();
                                    if (mBp != null && mBp.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                                        Float mCost = mBp.getDeployCost();
                                        int cardMaint = (mCost != null) ? mCost.intValue() : 1;
                                        existingMaintenanceCost += cardMaint;
                                    }
                                }
                            }
                            if (existingMaintenanceCost > 0) {
                                int totalForceNow = gameState.getForcePileSize(context.getPlayerId());
                                int forceAfterThisDeploy = totalForceNow - cost;
                                if (forceAfterThisDeploy < existingMaintenanceCost) {
                                    action.addReasoning("V24.5 MAINTENANCE RESERVE: Deploying this leaves only " +
                                        forceAfterThisDeploy + " Force but need " + existingMaintenanceCost +
                                        " for existing maintenance cards — they'll be sacrificed!", -400.0f);
                                    LOG.warn("V24.5 MAINTENANCE RESERVE: {} costs {}, {} Force available, " +
                                        "only {} left but existing maintenance needs {} — BLOCKING!",
                                        blueprint.getTitle(), cost, totalForceNow, forceAfterThisDeploy, existingMaintenanceCost);
                                } else if (forceAfterThisDeploy < existingMaintenanceCost + 2) {
                                    action.addReasoning("V24.5 MAINTENANCE RESERVE: Tight on Force for existing maintenance (" +
                                        forceAfterThisDeploy + " left, need " + existingMaintenanceCost + ")", -100.0f);
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

                            // --- Only apply maintenance awareness to maintenance card deploys ---
                            if (thisCardHasMaint && forceAfterThisDeploy < cost) {
                                // Deploying a maintenance card but won't have enough Force to pay
                                // its own maintenance at end of turn. Small tiebreaker — NOT a blocker.
                                // The card can be lost as attrition in battle, or Rando activates
                                // more Force next turn to cover it.
                                float maintPenalty = -40.0f;
                                if (forceAfterThisDeploy <= 0) {
                                    maintPenalty = -50.0f; // Zero Force left — slight extra caution
                                }
                                action.addReasoning(
                                    String.format("V29.13 MAINT AWARENESS: This card costs %d maint at end of turn, " +
                                        "only %d Force left after deploy — plan to activate more next turn",
                                        cost, forceAfterThisDeploy),
                                    maintPenalty);
                            }

                            // --- DTF / Grabber interrupt reserve (soft penalty) ---
                            // Nice to keep 1 Force for interrupts, but never block a deploy over it.
                            String dtfOpponentId = gameState.getOpponent(context.getPlayerId());
                            boolean dtfOnTable = false;
                            for (PhysicalCard dtfCard : gameState.getAllPermanentCards()) {
                                if (dtfCard == null) continue;
                                if (dtfOpponentId != null && dtfOpponentId.equals(dtfCard.getOwner())
                                    && dtfCard.getBlueprint() != null
                                    && dtfCard.getBlueprint().getTitle() != null) {
                                    String dtfT = dtfCard.getBlueprint().getTitle().toLowerCase(Locale.ROOT);
                                    if (dtfT.contains("draw their fire")) {
                                        com.gempukku.swccgo.common.Zone dtfZ = dtfCard.getZone();
                                        if (dtfZ != null && dtfZ.isInPlay()) {
                                            dtfOnTable = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            boolean grabberUnused = false;
                            for (PhysicalCard gCard : gameState.getAllPermanentCards()) {
                                if (gCard == null || gCard.getBlueprint() == null) continue;
                                if (!context.getPlayerId().equals(gCard.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone gZ = gCard.getZone();
                                if (gZ == null || !gZ.isInPlay()) continue;
                                if (gCard.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.GRABBER)) {
                                    java.util.List<PhysicalCard> stacked = gameState.getStackedCards(gCard);
                                    if (stacked == null || stacked.isEmpty()) {
                                        grabberUnused = true;
                                    }
                                    break;
                                }
                            }
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

                    // === V29 SMART SOLO DEPLOY CHECK ===
                    // Any character with power < 6 deploying solo gets a penalty UNLESS:
                    //   Exception 1: A second character in hand can deploy right after AND
                    //                we can afford both + interrupt reserve + maintenance.
                    //   Exception 2: The solo deploy helps flip the objective (Dark Deal,
                    //                Cloud City Occupation, TDIGWATT). Adds penalty if no
                    //                escape route (connected friendly location) exists.
                    // Also: V26 BUDDY-SEEK bonus still applies for strong chars protecting weak ones.
                    if (blueprint.getCardCategory() == CardCategory.CHARACTER
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
                                int maintObligation = 0;
                                for (PhysicalCard mCard : gameState.getAllPermanentCards()) {
                                    if (mCard == null) continue;
                                    if (!playerId.equals(mCard.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone mZone = mCard.getZone();
                                    if (mZone == null || !mZone.isInPlay()) continue;
                                    SwccgCardBlueprint mBp = mCard.getBlueprint();
                                    if (mBp != null && mBp.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                                        Float mCostF = mBp.getDeployCost();
                                        maintObligation += (mCostF != null) ? mCostF.intValue() : 1;
                                    }
                                }
                                // Add maintenance for THIS card if applicable
                                if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.MAINTENANCE)) {
                                    maintObligation += cost;
                                }
                                // Only reserve for interrupts when opponent has Draw Their Fire
                                int interruptReserve = 0;
                                String dtfOpId = gameState.getOpponent(playerId);
                                for (PhysicalCard dtfChk : gameState.getAllPermanentCards()) {
                                    if (dtfChk == null) continue;
                                    if (dtfOpId != null && dtfOpId.equals(dtfChk.getOwner())
                                        && dtfChk.getBlueprint() != null
                                        && dtfChk.getBlueprint().getTitle() != null
                                        && dtfChk.getBlueprint().getTitle().toLowerCase(Locale.ROOT).contains("draw their fire")) {
                                        com.gempukku.swccgo.common.Zone dtfChkZ = dtfChk.getZone();
                                        if (dtfChkZ != null && dtfChkZ.isInPlay()) {
                                            interruptReserve = 1; // 1 Force tax per interrupt
                                            break;
                                        }
                                    }
                                }
                                int forceReserveNeeded = maintObligation + interruptReserve;

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
                                if (pairedDeployPossible) {
                                    // Exception 1: Allow solo — buddy can follow immediately
                                    action.addReasoning(
                                        String.format("V29 PAIRED DEPLOY: %s solo OK — buddy in hand can follow with Force for reserve!",
                                            card.getTitle()), 0.0f);
                                    LOG.info("V29 PAIRED: Allowing solo {} — paired deploy affordable with reserve",
                                        card.getTitle());
                                } else if (isObjectiveFlipDeploy) {
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
                                    // No exception — block the solo deploy
                                    action.addReasoning(
                                        String.format("V29 SOLO BLOCK: %s (power %d) would deploy ALONE — too vulnerable! " +
                                            "No buddy affordable (need reserve %d) and not objective-relevant.",
                                            card.getTitle(), powerVal, forceReserveNeeded),
                                        -300.0f);
                                    LOG.warn("V29 SOLO BLOCK: {} (power {}) — no paired deploy, not obj flip — BLOCKING!",
                                        card.getTitle(), powerVal);
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V29 SOLO CHECK: Error: {}", e.getMessage());
                        }
                    }

                    // === V29: BUDDY-SEEK BONUS ===
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
                                            action.addReasoning(
                                                String.format("V29 BUDDY-SEEK: Deploy to protect vulnerable %s (power %d) at %s!",
                                                    soloAlly.getTitle(), allyPower, locTitle),
                                                200.0f);
                                            LOG.warn("V29 BUDDY-SEEK: {} deploying to protect vulnerable {} at {}!",
                                                card.getTitle(), soloAlly.getTitle(), locTitle);
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
                                            // V35.7: Raised from -150 to -600. Inquisitors MUST deploy with Vader.
                                            // The old -150 was easily overridden by DIRECT ENGAGE (+350) at spy locations.
                                            action.addReasoning(String.format(
                                                "V35.7 HUNT SCATTER: %s deploying AWAY from Vader at %s — MUST group with Vader!",
                                                card.getTitle(), vaderLoc.getTitle()), -600.0f);
                                            LOG.warn("V35.7 HUNT SCATTER: {} NOT at Vader's location ({}) — hard penalty (-600)",
                                                card.getTitle(), vaderLoc.getTitle());
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOG.debug("V29.12 HUNT DOWN GROUP: Error: {}", e.getMessage());
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
                                    // V34: Opponents are HERE — deploy directly to contest!
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
                                        // V35.1: Hunt Down V — HARD BLOCK deploying to empty sites
                                        // This deck hunts and destroys ALL opponents. Never deploy
                                        // to empty locations when opponents exist at battlegrounds.
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer emptyDeployAnalyzer =
                                            context.getObjectiveAnalyzer();
                                        boolean isHuntDown = emptyDeployAnalyzer != null
                                            && emptyDeployAnalyzer.isAnalyzed() && emptyDeployAnalyzer.isHuntDownV();
                                        float emptyPenalty = isHuntDown ? -1500.0f : -200.0f; // V35.2: -800 wasn't enough, raised to -1500
                                        action.addReasoning(String.format(
                                            "V35.1 EMPTY DEPLOY: %s to %s has NO opponents — %s!",
                                            card.getTitle(), locCard.getTitle(),
                                            isHuntDown ? "HUNT DOWN: GO WHERE OPPONENTS ARE" : "deploy where they ARE instead"),
                                            emptyPenalty);
                                        LOG.warn("V35.1 EMPTY DEPLOY: {} to {} — no opponents, opponents elsewhere (penalty {}{})",
                                            card.getTitle(), locCard.getTitle(), (int)emptyPenalty,
                                            isHuntDown ? " HUNT DOWN HARD BLOCK" : "");
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
                        action.addReasoning(String.format("Below average value (%.1f)", valueRatio), -20.0f);
                    }

                    // === HIGH DESTINY BONUS ===
                    if (destinyVal >= 5.0f) {
                        action.addReasoning(String.format("High destiny (%.0f)", destinyVal), 15.0f);
                    }

                    // === CARD TYPE BONUSES ===
                    CardCategory category = blueprint.getCardCategory();

                    // === V24: MEGA LOCATION PRIORITY ===
                    // Locations are the foundation of EVERYTHING — force generation, deploy targets, drain sites.
                    // In the first 3 turns, deploying locations should dominate all other actions.
                    if (category == CardCategory.LOCATION) {
                        int locTurn = context.getTurnNumber();
                        if (locTurn <= 3) {
                            action.addReasoning("V24 LOCATION PRIORITY: Locations first — force generation is everything!", 200.0f);
                            LOG.warn("V24 LOCATION PRIORITY: {} gets +200 on turn {} — locations are the foundation!", card.getTitle(), locTurn);
                        } else {
                            action.addReasoning("V24: Location deployment — extra force generation", 50.0f);
                        }
                    }

                    // === V22.7: CLOUD CITY OCCUPATION GUARD ===
                    // Cloud City Occupation self-cancels if we don't occupy Bespin system.
                    // Don't waste the deploy — block it until we actually occupy Bespin.
                    // Also check Dark Deal (V) which has similar Bespin requirements.
                    String cardTitleLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
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

                    // === V33: ONE WEAPON PER CHARACTER (HARD BLOCK) ===
                    // A character should only ever have one weapon. If the target character
                    // already has ANY weapon attached, hard-block this deploy (-9999).
                    // This is universal — applies to all weapons, not just lightsabers.
                    if (category == CardCategory.WEAPON && gameState != null) {
                        try {
                            String v33PlayerId = context.getPlayerId();
                            // Parse target character from action text (format: "on <Character Name>")
                            for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                if (tableCard == null || !v33PlayerId.equals(tableCard.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone v33Zone = tableCard.getZone();
                                if (v33Zone == null || !v33Zone.isInPlay()) continue;
                                if (tableCard.getBlueprint() == null || tableCard.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                String v33CharTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                if (v33CharTitle.isEmpty() || !actionLower.contains(v33CharTitle)) continue;

                                // Found likely target character — check for existing weapons
                                java.util.List<PhysicalCard> v33Attachments = gameState.getAttachedCards(tableCard);
                                if (v33Attachments != null) {
                                    for (PhysicalCard att : v33Attachments) {
                                        if (att != null && att.getBlueprint() != null
                                            && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                            action.addReasoning(String.format(
                                                "V33 ONE WEAPON: %s already has a weapon — BLOCKED!",
                                                tableCard.getTitle()), -9999.0f);
                                            LOG.warn("V33 ONE WEAPON: {} on {} BLOCKED — character already armed!",
                                                card.getTitle(), tableCard.getTitle());
                                            break;
                                        }
                                    }
                                }
                                break; // Only check first matching character
                            }
                        } catch (Exception e) {
                            LOG.debug("V33 ONE WEAPON: Error: {}", e.getMessage());
                        }
                    }

                    // === V29.9: LIGHTSABER DEPLOY PRIORITY (HUNT DOWN V) ===
                    // When Vader is on table WITHOUT a lightsaber, and we have a lightsaber in hand,
                    // deploying it ON Vader is CRITICAL. Vader with lightsaber is devastating
                    // (weapon hit + throw destiny = effectively +5 power). Without it, he's just
                    // power 6 who will die in the first serious battle.
                    // This applies to ANY weapon deploying on a character who needs it.
                    if (category == CardCategory.WEAPON && gameState != null) {
                        boolean isLightsaber = cardTitleLower.contains("lightsaber");
                        boolean isVadersLightsaber = cardTitleLower.contains("vader") && isLightsaber;
                        boolean isDarkJediLightsaber = cardTitleLower.contains("dark jedi") && isLightsaber;

                        if (isLightsaber) {
                            try {
                                String wepPlayerId = context.getPlayerId();
                                // V29.11: Smarter lightsaber targeting
                                // Vader's Lightsaber → ONLY on Vader
                                // Dark Jedi Lightsaber → on Inquisitors/other dark jedi, NOT Vader
                                // Character-specific sabers (Mara, Maul) → on their specific character
                                boolean matchingCharOnTable = false;
                                boolean charAlreadyHasWeapon = false;

                                for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                    if (tableCard == null || !wepPlayerId.equals(tableCard.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone wepZone = tableCard.getZone();
                                    if (wepZone == null || !wepZone.isInPlay()) continue;
                                    if (tableCard.getBlueprint() == null || tableCard.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;

                                    String tTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                    boolean isTargetChar = false;

                                    if (isVadersLightsaber) {
                                        // Vader's Lightsaber → ONLY Vader
                                        if (tTitle.contains("vader")) isTargetChar = true;
                                    } else if (cardTitleLower.contains("mara") && tTitle.contains("mara")) {
                                        isTargetChar = true;
                                    } else if (cardTitleLower.contains("maul") && tTitle.contains("maul")) {
                                        isTargetChar = true;
                                    } else if (isDarkJediLightsaber || !isVadersLightsaber) {
                                        // V29.11: Dark Jedi Lightsaber / generic lightsaber
                                        // Target Inquisitors and other dark jedi — NOT Vader
                                        // Vader should get Vader's Lightsaber, not this
                                        if (tTitle.contains("inquisitor") || tTitle.contains("sister")
                                            || tTitle.contains("brother") || tTitle.contains("maul")
                                            || tTitle.contains("mara") || tTitle.contains("dark jedi")) {
                                            isTargetChar = true;
                                        }
                                        // Only put on Vader as absolute last resort (no other targets)
                                        // This is handled below — we do NOT match Vader here
                                    }

                                    if (isTargetChar) {
                                        matchingCharOnTable = true;
                                        // Check if this character already has a weapon
                                        java.util.List<PhysicalCard> attachments = gameState.getAttachedCards(tableCard);
                                        if (attachments != null) {
                                            for (PhysicalCard att : attachments) {
                                                if (att != null && att.getBlueprint() != null
                                                    && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                    charAlreadyHasWeapon = true;
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }

                                if (matchingCharOnTable && !charAlreadyHasWeapon) {
                                    // Character on table with NO weapon — deploying lightsaber is TOP priority!
                                    float saberBoost = 400.0f;
                                    if (isVadersLightsaber) saberBoost = 500.0f; // Vader's lightsaber is even more critical
                                    action.addReasoning(String.format(
                                        "V29.11 LIGHTSABER: %s on table without weapon — deploy %s NOW! (+%.0f)",
                                        isVadersLightsaber ? "Vader" : "Dark Jedi",
                                        isVadersLightsaber ? "Vader's Lightsaber" : "lightsaber",
                                        saberBoost), saberBoost);
                                    LOG.warn("V29.11 LIGHTSABER: {} deploying on unarmed character — TOP PRIORITY (+{})!",
                                        card.getTitle(), (int)saberBoost);
                                } else if (matchingCharOnTable && charAlreadyHasWeapon) {
                                    // V29.11: Character already armed — STRONG block, don't double-weapon
                                    action.addReasoning("V29.11 LIGHTSABER: Character already has a weapon — other characters need it!", -300.0f);
                                    LOG.info("V29.11 LIGHTSABER: {} — target already armed, BLOCKED (-300)", card.getTitle());
                                } else {
                                    // No matching character on table — don't deploy orphan weapon
                                    action.addReasoning("V29.11 LIGHTSABER: No matching character on table — save for later!", -200.0f);
                                    LOG.info("V29.11 LIGHTSABER: {} — no target character, penalizing", card.getTitle());
                                }
                            } catch (Exception e) {
                                LOG.debug("V29.11 LIGHTSABER: Error checking weapon deploy: {}", e.getMessage());
                            }
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
                                        action.addReasoning("V29.2 LANDO: Key piece BUT would be ALONE — deploy backup first!", -100.0f);
                                        LOG.warn("V29.2 LANDO: PENALIZED -100 — no friendly chars at CC! (actionText='{}')", actionText);
                                    }
                                } else if (isLobotDeploy) {
                                    if (haveCharAtCCSite) {
                                        action.addReasoning("V29.2 LOBOT: Helps flip TDIGWATT + backup present!", 150.0f);
                                        LOG.warn("V29.2 LOBOT: +150 — has backup!");
                                    } else {
                                        action.addReasoning("V29.2 LOBOT: Would be ALONE at CC — deploy backup first!", -100.0f);
                                        LOG.warn("V29.2 LOBOT: PENALIZED -100 — no backup at CC!");
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
                                        // Big bonus for spreading to unoccupied objective locations pre-flip
                                        action.addReasoning(String.format(
                                            "V31 PRE-FLIP SPREAD: Deploy to unoccupied obj location! (%d/%d occupied, need more)",
                                            occupiedObjLocs, occupiedObjLocs + unoccupiedObjLocs), 250.0f);
                                        LOG.warn("V31 PRE-FLIP: {} deploying to unoccupied obj loc (+250) — {}/{} occupied",
                                            card.getTitle(), occupiedObjLocs, occupiedObjLocs + unoccupiedObjLocs);
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
                                            action.addReasoning("V31 POST-FLIP: Don't spread to 3+ obj locs — consolidate to 2!", -100.0f);
                                            LOG.warn("V31 POST-FLIP: {} deploy to 3rd obj loc penalized (-100)", card.getTitle());
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
                                            "V32 ABILITY RISK: Solo deploy with ability %.0f < 4 at %s — NO battle destiny! No follow-up in hand!",
                                            cardAbility, loc.getTitle()), -200.0f);
                                        LOG.warn("V32 ABILITY RISK: {} (ability {}) solo at {} with no follow-up — penalized (-200)",
                                            card.getTitle(), cardAbility, loc.getTitle());
                                    } else {
                                        // Follow-up exists in hand — mild caution (deploy order matters)
                                        action.addReasoning(String.format(
                                            "V32 ABILITY CAUTION: Solo ability %.0f < 4 at %s but follow-up in hand",
                                            cardAbility, loc.getTitle()), -30.0f);
                                    }
                                } else {
                                    // Deploying to a site with friendlies but total still < 4
                                    action.addReasoning(String.format(
                                        "V32 ABILITY WARNING: Total ability %.0f still < 4 at %s after deploy!",
                                        totalAfterDeploy, loc.getTitle()), -100.0f);
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
                                    // No pilot in hand can reach ability 4 — HARD BLOCK
                                    action.addReasoning(String.format(
                                        "V35.7 SHIP ABILITY: %s ability %.0f — no pilot can reach 4! BLOCKED!",
                                        card.getTitle(), shipAbility), -800.0f);
                                    LOG.warn("V35.7 SHIP ABILITY: {} ability {} — HARD BLOCK (-800)",
                                        card.getTitle(), shipAbility);
                                } else if (!matchingPilotAffordable) {
                                    // Pilot exists but can't afford ship+pilot together — risky
                                    action.addReasoning(String.format(
                                        "V35.7 SHIP ABILITY: %s needs pilot but can't afford both! (ship cost %d, Force %d)",
                                        card.getTitle(), shipCost, context.getForcePileSize()), -400.0f);
                                    LOG.warn("V35.7 SHIP ABILITY: {} — pilot exists but unaffordable (-400)",
                                        card.getTitle());
                                } else {
                                    // Pilot exists and affordable — mild warning to deploy together
                                    action.addReasoning(String.format(
                                        "V35.7 SHIP: %s needs %s aboard for ability 4 — deploy together!",
                                        card.getTitle(), matchingPilotTitle != null ? matchingPilotTitle : "a pilot"), -100.0f);
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
                                    float shipPenalty = -600.0f;
                                    if (oppShipPower > ourShipPower * 3) shipPenalty = -1000.0f; // Massive mismatch
                                    action.addReasoning(String.format(
                                        "V35.5 SHIP SUICIDE: %s (power %.0f) vs opponent ships (power %.0f) at %s — OUTGUNNED!",
                                        card.getTitle(), ourShipPower, oppShipPower, sysLoc.getTitle()), shipPenalty);
                                    LOG.warn("V35.5 SHIP SUICIDE: {} power {} vs opponent {} at {} — BLOCKED ({})",
                                        card.getTitle(), (int)ourShipPower, (int)oppShipPower, sysLoc.getTitle(), (int)shipPenalty);
                                }
                                break;
                            }
                        } catch (Exception e) {
                            LOG.debug("V35.5 SHIP CHECK: Error: {}", e.getMessage());
                        }
                    }

                    // === V24.2E: UNDERCOVER SPY DEPLOY PRIORITY (TDIGWATT) ===
                    // U-3PO and Keder The Black are undercover spies that deploy on opponent's side.
                    // They block opponent from controlling locations (preventing opponent force drains).
                    // They should NEVER go to Cloud City — their job is blocking opponent elsewhere.
                    if (category == CardCategory.CHARACTER) {
                        if (cardTitleLower.contains("u-3po") || cardTitleLower.contains("keder")) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer spyObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (spyObjAnalyzer != null && spyObjAnalyzer.isAnalyzed()
                                && spyObjAnalyzer.needsBespinSystemPresence()) {
                                action.addReasoning("V24.2 SPY: Deploy to block opponent force drains!", 100.0f);
                                LOG.warn("V24.2 SPY: {} gets +100 — blocks opponent from controlling locations!", card.getTitle());
                            }
                        }
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
                                        action.addReasoning("V24.9 EXECUTOR CRITICAL: Bespin on table — MUST deploy NOW!", 600.0f);
                                        LOG.warn("V24.9 EXECUTOR CRITICAL: {} on turn {} + Bespin on table — MAXIMUM priority (+600)!", card.getTitle(), execTurn);
                                    } else {
                                        action.addReasoning("V24.6 EXECUTOR: Key ship for TDIGWATT — deploy to Bespin!", 350.0f);
                                        LOG.warn("V24.6 EXECUTOR: {} in hand + Bespin on table — deploy priority (+350)!", card.getTitle());
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

                    // === V24.9: PIETT DEPLOY PRIORITY FOR TDIGWATT ===
                    // Piett is THE matching pilot for Executor. If TDIGWATT is active,
                    // deploying Piett is critical — he enables AMSD + Executor combo.
                    if (cardTitleLower.contains("piett")) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer piettObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (piettObjAnalyzer != null && piettObjAnalyzer.isAnalyzed()
                            && piettObjAnalyzer.needsBespinSystemPresence()) {
                            action.addReasoning("V24.9 PIETT: Key Executor pilot for TDIGWATT — deploy ASAP!", 200.0f);
                            LOG.warn("V24.9 PIETT: {} in hand — critical pilot for Executor (+200)!", card.getTitle());
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
                        action.addReasoning("BLOCKED: Unknown card during DEPLOY_LOCATIONS plan (turn 1)", -1000.0f);
                        actions.add(action);
                        continue;
                    } else {
                        LOG.info("V29.7: Unknown card during DEPLOY_LOCATIONS but turn {} — allowing with penalty", context.getTurnNumber());
                        action.addReasoning("V29.7: DEPLOY_LOCATIONS incomplete turn " + context.getTurnNumber() + " — allow unknown deploy", -30.0f);
                    }
                }
                // V26: Unknown card from reserve deck — can't evaluate stats, buddy system,
                // maintenance, or plan alignment. Penalize enough to not auto-beat pass.
                // The base score is +50, so -60 brings it to -10 total, below pass (~2).
                // Specific reserve deploy actions (Vader Castle, I'm Sorry, etc.) get their
                // own scoring in ActionTextEvaluator to override this when appropriate.
                action.addReasoning("V26: Unknown card (deploy from reserve?) — can't evaluate, prefer known deploys", -60.0f);
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
