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

            // Only handle deploy-related actions (including persona replace)
            if (!actionLower.contains("deploy") && !actionLower.contains("persona replace")) {
                continue;
            }

            // V38.4: PERSONA REPLACE — usually BAD. Replacing Vader with a different
            // version puts the current one in Lost Pile (losing any attached weapons).
            if (actionLower.contains("persona replace")) {
                EvaluatedAction prAction = new EvaluatedAction(actionId, ActionType.DEPLOY, -50.0f, actionText);
                prAction.addReasoning("V39 PERSONA REPLACE: Loses armed character — caution (-50, was -500)", -50.0f);
                LOG.warn("V39 PERSONA REPLACE: '{}' — mild penalty (-50, was -500)", actionText);
                actions.add(prAction);
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.DEPLOY,
                50.0f,  // Base score
                actionText
            );

            // === V42: HAND PRESERVATION + DEPLOY URGENCY ===
            // Two competing concerns:
            // 1. Cards in hand do nothing — deploy them to generate value
            // 2. An empty hand is CATASTROPHIC — you lose a full turn drawing up,
            //    and battle damage/force drains eat directly into your reserve deck
            //    (losing key cards you'll never see again).
            // Rule: ALWAYS keep at least 4 cards in hand as a Force loss buffer.
            // Exception: Locations always deploy (they don't reduce hand presence).
            {
                int handSize = hand != null ? hand.size() : 0;
                float urgencyBonus = 0;

                // V42: HAND PRESERVATION — penalize deploys when hand is getting low
                // Cards from hand are the buffer against Force drains and battle damage.
                // Losing your entire hand means losing a whole turn just drawing up.
                // Use actionText to detect locations (card object not yet resolved at this scope)
                boolean isLocationDeploy = actionLower.contains("location") || actionLower.contains("site")
                    || actionLower.contains("system") || actionLower.contains("sector");

                if (!isLocationDeploy && handSize <= 3) {
                    // CRITICAL: Hand almost empty — DO NOT deploy!
                    action.addReasoning(String.format(
                        "V42 HAND CRITICAL: Only %d cards in hand — STOP deploying! Need buffer for Force loss!",
                        handSize), -500.0f);
                    LOG.warn("V42 HAND CRITICAL: hand size {} — blocked from deploying (-500) — '{}'",
                        handSize, actionText);
                } else if (!isLocationDeploy && handSize <= 5) {
                    // Low hand — mild penalty to discourage over-deployment
                    action.addReasoning(String.format(
                        "V42 HAND LOW: %d cards in hand — conserve hand, deploy only high-value cards (-100)",
                        handSize), -100.0f);
                    LOG.warn("V42 HAND LOW: hand size {} — caution (-100) — '{}'",
                        handSize, actionText);
                }

                // Mild base urgency — just enough to beat Pass, not enough to override
                // location-quality evaluation
                if (handSize >= 10) {
                    urgencyBonus = 30.0f + (handSize - 10) * 10.0f; // +30 at 10, +40 at 11, +50 at 12...
                } else if (handSize >= 7) {
                    urgencyBonus = 15.0f; // Mild nudge
                }

                // Extra nudge if lots of Force available — should be spending it
                if (availableForce >= 10 && handSize >= 8) {
                    urgencyBonus += 20.0f;
                }

                if (urgencyBonus > 0) {
                    action.addReasoning(String.format(
                        "V42 DEPLOY URGENCY: hand=%d, force=%d (+%.0f base)",
                        handSize, availableForce, urgencyBonus), urgencyBonus);
                }
            }

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
                // V40: Only apply hold-back for TDIGWATT (needs Bespin). All others deploy freely.
                boolean isTdigwattDeck = holdBackObjAnalyzer != null && holdBackObjAnalyzer.isAnalyzed()
                    && holdBackObjAnalyzer.needsBespinSystemPresence();
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
                        // Turn 3+: Hold back non-location deploys only (TDIGWATT)
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
                            // V40: HOLD_BACK penalty neutralized — score 0 instead of -150
                            action.addReasoning("V40 HOLD BACK: TDIGWATT hold-back (neutral)", 0.0f);
                            actions.add(action);
                            continue;  // Skip individual card evaluation - plan says don't deploy
                        }
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
                    && bespinFirstAnalyzer.needsBespinSystemPresence()
                    && !bespinFirstAnalyzer.isHuntDownV()) { // V39: TDIGWATT only — Hunt Down deploys freely
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
                            // V40.1: TDIGWATT — prefer Bespin/Executor but DON'T block other deploys.
                            // Bespin and Executor get POSITIVE bonuses instead of blocking characters.
                            // This way characters still deploy, just with slightly lower priority.
                            action.addReasoning("V40.1 TDIGWATT: Bespin/Executor preferred but deploying is fine", 0.0f);
                            LOG.info("V40.1 BESPIN-FIRST: Turn {} — no penalty, Bespin/Executor get bonus instead", bfTurn);
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
                                        // V29.7 SAFETY VALVE: After turn 1, allow freely.
                                        LOG.info("V29.7: DEPLOY_LOCATIONS turn {} — allowing character deploy: {}", context.getTurnNumber(), card.getTitle());
                                        action.addReasoning("V40: DEPLOY_LOCATIONS incomplete but turn " + context.getTurnNumber() + " — deploy freely!", 0.0f);
                                    } else {
                                        // V40: Turn 1 DEPLOY_LOCATIONS block only for TDIGWATT
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer dlObjAnalyzer =
                                            context.getObjectiveAnalyzer();
                                        boolean isTdigwattDL = dlObjAnalyzer != null && dlObjAnalyzer.isAnalyzed()
                                            && dlObjAnalyzer.needsBespinSystemPresence();
                                        if (isTdigwattDL) {
                                            // V39.2: RESTORE full penalty for TDIGWATT turn 1 location plan
                                            LOG.warn("V39.2 DEPLOY_LOCATIONS RESTORED: TDIGWATT turn 1 — locations first! Blocking: {}", card.getTitle());
                                            action.addReasoning("V29 DEPLOY_LOCATIONS: TDIGWATT turn 1 — deploy locations first!", -1000.0f);
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
                                action.addReasoning("V40 MAINTENANCE: need " + maintenanceCost +
                                    " Force for upkeep but only " + forceAfterDeploy + " left after deploy (mild caution)", -50.0f);
                                LOG.warn("V22.3 MAINTENANCE BLOCKED: {} costs {} to deploy, {} Force available, {} left but needs {} for upkeep!",
                                    blueprint.getTitle(), cost, totalForce, forceAfterDeploy, maintenanceCost);
                            } else if (forceAfterDeploy < maintenanceCost + 2) {
                                // Can barely pay maintenance — risky
                                action.addReasoning("V40 Maintenance card - tight on Force for upkeep (" +
                                    forceAfterDeploy + " left, need " + maintenanceCost + ") (mild caution)", -50.0f);
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
                                float maintPenalty = -5.0f; // V40.1: very mild caution
                                if (forceAfterThisDeploy <= 0) {
                                    maintPenalty = -20.0f; // V40.1: Zero Force — card may die but that's OK
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
                                    String.format("V39 INTERRUPT RESERVE: %s%s but 0 Force left for them after deploy (mild)",
                                        dtfOnTable ? "DTF active" : "",
                                        grabberUnused ? (dtfOnTable ? " + grabber ready" : "Grabber ready") : ""),
                                    -3.0f); // V39: was -30
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
                                        // V39: Mild penalty — still allow but it's risky (was -150)
                                        action.addReasoning(
                                            String.format("V39 OBJ-FLIP: %s solo at '%s' for flip but NO escape route — mild caution",
                                                card.getTitle(), targetLoc != null ? targetLoc.getTitle() : "?"), -15.0f);
                                        LOG.warn("V39 OBJ-FLIP: Solo {} for flip but no escape route — mild penalty (-15, was -150)", card.getTitle());
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
                                        // V39: Staging deploy — very mild penalty (was -80)
                                        action.addReasoning(String.format(
                                            "V39 STAGING: %s to non-battleground — move to buddy up next turn",
                                            card.getTitle()), -8.0f);
                                        LOG.info("V39 STAGING: {} deploying to staging site — can buddy next turn (-8, was -80)",
                                            card.getTitle());
                                    } else {
                                        // V39: Very mild solo caution (was -150)
                                        action.addReasoning(
                                            String.format("V39 SOLO CAUTION: %s (power %d) solo — acceptable",
                                                card.getTitle(), powerVal),
                                            -15.0f); // V39: was -150
                                        LOG.info("V39 SOLO CAUTION: {} (power {}) — mild penalty (-15, was -150)",
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

                    // === V36: CONTEST ALL DRAIN LOCATIONS ===
                    // Opponent drains are the #1 damage source. Every uncontested drain site
                    // bleeds 1-3 Force per turn. Deploying even one character stops the drain.
                    // This is UNIVERSAL — applies to ALL decks, not just Hunt Down.
                    // Scan all locations: if opponent has presence and we don't, that's a
                    // drain site we MUST contest. Bonus scales with drain amount.
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

                                if (v36OppPower > 0 && v36OurPower == 0) {
                                    // Opponent is draining here uncontested! STOP THE BLEEDING!
                                    float drainAmount = 1.0f;
                                    try {
                                        drainAmount = game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, v36Loc, v36Oid);
                                    } catch (Exception e) { /* default 1 */ }

                                    float contestDrainBonus = 200.0f + (drainAmount * 100.0f);
                                    action.addReasoning(String.format(
                                        "V36 CONTEST DRAIN: %s drains %.0f at %s UNCONTESTED — deploy to STOP the bleeding!",
                                        v36Oid, drainAmount, v36Loc.getTitle()), contestDrainBonus);
                                    LOG.warn("V36 CONTEST DRAIN: {} to {} — opponent drains {} uncontested (+{})",
                                        card.getTitle(), v36Loc.getTitle(), (int)drainAmount, (int)contestDrainBonus);
                                }
                                break; // Found target location
                            }
                        } catch (Exception e) {
                            LOG.debug("V36 CONTEST DRAIN: Error: {}", e.getMessage());
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
                    if (category == CardCategory.CHARACTER && gameState != null && game != null) {
                        try {
                            String v40Pid = context.getPlayerId();
                            String v40Oid = gameState.getOpponent(v40Pid);
                            String v40ActionLower = actionText.toLowerCase(Locale.ROOT);

                            // --- Deploy Vader/Emperor solo OK: +100 ---
                            String cardTitleLower = card.getTitle() != null ? card.getTitle().toLowerCase(Locale.ROOT) : "";
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

                                // Deploy to site with our characters already there (reinforcement): +150
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
                                    if (v40FriendlyCount > 0) {
                                        action.addReasoning(String.format(
                                            "V40 REINFORCE: Joining %d friendlies at %s!",
                                            v40FriendlyCount, v40Loc.getTitle()), 150.0f);
                                        LOG.warn("V40 REINFORCE: {} joins {} friendlies at {} — +150",
                                            card.getTitle(), v40FriendlyCount, v40Loc.getTitle());
                                    }

                                    // Deploy with buddy (ability total >= 7 at site after deploy): +150
                                    float v40CardAbility = 0;
                                    if (card.getBlueprint().hasAbilityAttribute()) {
                                        Float v40ab2 = card.getBlueprint().getAbility();
                                        v40CardAbility = (v40ab2 != null ? v40ab2 : 0);
                                    }
                                    if (v40FriendlyAbility + v40CardAbility >= 7.0f && v40FriendlyCount > 0) {
                                        action.addReasoning(String.format(
                                            "V40 BUDDY: Ability total %.0f >= 7 at %s after deploy!",
                                            v40FriendlyAbility + v40CardAbility, v40Loc.getTitle()), 150.0f);
                                        LOG.warn("V40 BUDDY: {} — total ability {} at {} — +150",
                                            card.getTitle(), (int)(v40FriendlyAbility + v40CardAbility), v40Loc.getTitle());
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
                            action.addReasoning("V39 BLOCKED: " + card.getTitle() +
                                " will SELF-CANCEL — we don't occupy Bespin system! (-80, was -800)", -80.0f);
                            LOG.warn("V39 V22.7: {} — we don't occupy Bespin, it will self-cancel (-80, was -800)!",
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

                    // === V33/V40: ONE WEAPON PER CHARACTER (HARD BLOCK) ===
                    // V40 FIX: The old V33 code tried to match character names in action text,
                    // but generic "Deploy" actions don't contain the target name. So it never fired.
                    // NEW approach: check if ALL our characters on table already have weapons.
                    // Also try to match character name in action text as before.
                    if (category == CardCategory.WEAPON && gameState != null) {
                        try {
                            String v33PlayerId = context.getPlayerId();
                            boolean foundUnarmedChar = false;
                            boolean foundArmedTarget = false;

                            for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                if (tableCard == null || !v33PlayerId.equals(tableCard.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone v33Zone = tableCard.getZone();
                                if (v33Zone == null || !v33Zone.isInPlay()) continue;
                                if (tableCard.getBlueprint() == null || tableCard.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;

                                boolean hasWeapon = false;
                                java.util.List<PhysicalCard> v33Attachments = gameState.getAttachedCards(tableCard);
                                if (v33Attachments != null) {
                                    for (PhysicalCard att : v33Attachments) {
                                        if (att != null && att.getBlueprint() != null
                                            && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                            hasWeapon = true;
                                            break;
                                        }
                                    }
                                }

                                if (!hasWeapon) {
                                    foundUnarmedChar = true;
                                }

                                // Also try action text matching (original V33 approach)
                                String v33CharTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                if (!v33CharTitle.isEmpty() && actionLower.contains(v33CharTitle) && hasWeapon) {
                                    foundArmedTarget = true;
                                    action.addReasoning(String.format(
                                        "V33 ONE WEAPON: %s already has a weapon — BLOCKED!",
                                        tableCard.getTitle()), -9999.0f);
                                    LOG.warn("V33 ONE WEAPON: {} on {} BLOCKED — character already armed!",
                                        card.getTitle(), tableCard.getTitle());
                                }
                            }

                            // V40: If action text didn't match but ALL characters are armed, block
                            if (!foundArmedTarget && !foundUnarmedChar) {
                                action.addReasoning("V40 ALL ARMED: Every character already has a weapon — no valid target!", -9999.0f);
                                LOG.warn("V40 ALL ARMED: {} — all characters have weapons, blocking deploy", card.getTitle());
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
                                    // V39: Character already armed — mild penalty (was -300)
                                    action.addReasoning("V39 LIGHTSABER: Character already has a weapon — other characters need it!", -30.0f);
                                    LOG.info("V39 LIGHTSABER: {} — target already armed (-30, was -300)", card.getTitle());
                                } else {
                                    // V39: No matching character on table — mild penalty (was -200)
                                    action.addReasoning("V39 LIGHTSABER: No matching character on table — save for later!", -20.0f);
                                    LOG.info("V39 LIGHTSABER: {} — no target character (-20, was -200)", card.getTitle());
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
                                                    "V39 NAMED WEAPON WAIT: %s has named weapon %s in hand — save the slot!",
                                                    targetCharName, hc.getTitle()), -40.0f); // V39: was -400
                                                LOG.warn("V39 NAMED WEAPON WAIT: Generic {} on {} — named {} in hand (-40, was -400)",
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

                                // V41: Check opponent presence at Dining Room before deploying Lando there
                                boolean opponentAtDiningRoom = false;
                                int opCharsAtDR = 0;
                                if (landoGs != null && landoGame != null) {
                                    try {
                                        String oppId = landoGame.getOpponent(context.getPlayerId());
                                        for (PhysicalCard loc : landoGs.getTopLocations()) {
                                            if (loc == null || loc.getTitle() == null) continue;
                                            if (!loc.getTitle().toLowerCase(Locale.ROOT).contains("dining room")) continue;
                                            for (PhysicalCard c : landoGs.getCardsAtLocation(loc)) {
                                                if (c != null && oppId != null && oppId.equals(c.getOwner())
                                                    && c.getBlueprint() != null
                                                    && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                    opponentAtDiningRoom = true;
                                                    opCharsAtDR++;
                                                }
                                            }
                                            break;
                                        }
                                    } catch (Exception e) { /* ignore */ }
                                }

                                if (isLandoDeploy) {
                                    if (opponentAtDiningRoom && !haveCharAtCCSite) {
                                        // V41: Opponents at Dining Room and no friendlies — Lando will die!
                                        action.addReasoning("V41 LANDO INTO ENEMY: " + opCharsAtDR
                                            + " opponents at Dining Room — Lando dies instantly! Wait for Vader!", -9999.0f);
                                        LOG.warn("V41 LANDO INTO ENEMY: {} opponents at DR, no backup — HARD BLOCK!", opCharsAtDR);
                                    } else if (haveCharAtCCSite) {
                                        action.addReasoning("V29.2 LANDO: Key piece + backup present — safe to deploy!", 200.0f);
                                        LOG.warn("V29.2 LANDO: +200 — has backup at CC site! (actionText='{}')", actionText);
                                    } else {
                                        action.addReasoning("V39 LANDO: Key piece — mild caution without backup (-10, was -100)", -10.0f);
                                        LOG.warn("V39 LANDO: mild penalty (-10, was -100) — no friendly chars at CC! (actionText='{}')", actionText);
                                    }
                                } else if (isLobotDeploy) {
                                    if (haveCharAtCCSite) {
                                        action.addReasoning("V29.2 LOBOT: Helps flip TDIGWATT + backup present!", 150.0f);
                                        LOG.warn("V29.2 LOBOT: +150 — has backup!");
                                    } else {
                                        action.addReasoning("V39 LOBOT: Deploy with mild caution — no backup (-10, was -100)", -10.0f);
                                        LOG.warn("V39 LOBOT: mild penalty (-10, was -100) — no backup at CC!");
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
                                        // V39: Very mild penalty for stacking when obj locs need presence (was -50)
                                        action.addReasoning(String.format(
                                            "V39 PRE-FLIP: %d obj locations still unoccupied — consider spreading out",
                                            unoccupiedObjLocs), -5.0f);
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

                    // === V42: UNIFIED BUDDY SYSTEM (replaces V32 ability + V33 buddy) ===
                    // CORE RULE: Never deploy a character alone. Always stack characters together.
                    // A lone character gets picked off instantly — the opponent just walks over
                    // and initiates battle with superior force. The buddy system ensures:
                    // 1. STRONG bonus for deploying to sites WITH existing friendlies (+400)
                    // 2. PENALTY for deploying alone when friendlies elsewhere need help (-400)
                    // 3. Ability >= 4 awareness (need it for battle destiny)
                    // 4. Opponent threat awareness (don't deploy alone where opponent can attack)
                    if (category == CardCategory.CHARACTER && card != null && card.getBlueprint() != null
                        && gameState != null && game != null) {
                        try {
                            float cardAbility = 0;
                            if (card.getBlueprint().hasAbilityAttribute()) {
                                Float ab = card.getBlueprint().getAbility();
                                cardAbility = ab != null ? ab : 0;
                            }

                            String v42PlayerId = context.getPlayerId();
                            String v42OpponentId = gameState.getOpponent(v42PlayerId);

                            // First pass: gather state of ALL sites
                            PhysicalCard targetSite = null;
                            float targetFriendlyAbility = 0;
                            int targetFriendlyCount = 0;
                            int targetOpponentCount = 0;
                            float targetOpponentPower = 0;
                            int targetSpyCount = 0;
                            boolean hasFriendlyCharAnywhere = false;
                            boolean hasUnderstaffedFriendlySite = false;

                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (loc.getBlueprint() == null || loc.getBlueprint().getCardSubtype() == null) continue;
                                if (loc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SITE) continue;

                                String siteTitle = loc.getTitle().toLowerCase(Locale.ROOT);
                                boolean isTarget = actionLower.contains(siteTitle);

                                // Count friendlies, opponents, and undercover spies at this site
                                float friendlyAbilityHere = 0;
                                int friendlyCountHere = 0;
                                int opponentCountHere = 0;
                                float opponentPowerHere = 0;
                                int spyCountHere = 0;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c == null || c.getBlueprint() == null) continue;
                                    if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    // V42: Detect undercover spies — they're enemy agents hiding at our locations
                                    if (c.isUndercover()) {
                                        spyCountHere++;
                                        opponentCountHere++; // Count spy as opponent threat
                                        continue; // Don't count spy as friendly even if owner matches
                                    }
                                    if (v42PlayerId.equals(c.getOwner())) {
                                        friendlyCountHere++;
                                        if (c.getBlueprint().hasAbilityAttribute()) {
                                            Float cAb = c.getBlueprint().getAbility();
                                            friendlyAbilityHere += (cAb != null ? cAb : 0);
                                        }
                                    } else if (v42OpponentId.equals(c.getOwner())) {
                                        opponentCountHere++;
                                        if (c.getBlueprint().hasPowerAttribute()) {
                                            Float cPow = c.getBlueprint().getPower();
                                            opponentPowerHere += (cPow != null ? cPow : 0);
                                        }
                                    }
                                }

                                if (friendlyCountHere > 0) {
                                    hasFriendlyCharAnywhere = true;
                                    if (friendlyAbilityHere < RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                        hasUnderstaffedFriendlySite = true;
                                    }
                                }

                                if (isTarget) {
                                    targetSite = loc;
                                    targetFriendlyAbility = friendlyAbilityHere;
                                    targetFriendlyCount = friendlyCountHere;
                                    targetOpponentCount = opponentCountHere;
                                    targetOpponentPower = opponentPowerHere;
                                    targetSpyCount = spyCountHere;
                                }
                            }

                            // Also check if opponent has characters ANYWHERE on board
                            boolean opponentHasCharsOnBoard = false;
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null) continue;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c != null && c.getBlueprint() != null
                                        && v42OpponentId.equals(c.getOwner())
                                        && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        opponentHasCharsOnBoard = true;
                                        break;
                                    }
                                }
                                if (opponentHasCharsOnBoard) break;
                            }

                            if (targetSite != null) {
                                float totalAfterDeploy = targetFriendlyAbility + cardAbility;
                                String siteName = targetSite.getTitle();

                                if (targetFriendlyCount > 0) {
                                    // === DEPLOYING TO SITE WITH FRIENDLIES — ALWAYS GOOD ===
                                    if (targetFriendlyAbility < 4.0f && totalAfterDeploy >= 4.0f) {
                                        // Fixes ability deficit — critical!
                                        action.addReasoning(String.format(
                                            "V42 BUDDY ABILITY FIX: Brings ability from %.0f to %.0f (>= 4) at %s!",
                                            targetFriendlyAbility, totalAfterDeploy, siteName), 500.0f);
                                        LOG.warn("V42 BUDDY ABILITY FIX: {} (ability {}) fixes deficit at {} (was {}, now {})",
                                            card.getTitle(), cardAbility, siteName, targetFriendlyAbility, totalAfterDeploy);
                                    } else if (targetFriendlyAbility < RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                        // Reinforcing an understaffed site
                                        float buddyBonus = 400.0f;
                                        if (totalAfterDeploy >= RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                            buddyBonus = 450.0f; // Reaches full buddy threshold
                                        }
                                        action.addReasoning(String.format(
                                            "V42 BUDDY REINFORCE: Join friendlies at %s (ability %.0f → %.0f, target %d)",
                                            siteName, targetFriendlyAbility, totalAfterDeploy,
                                            RandoConfig.ABILITY_BUDDY_THRESHOLD), buddyBonus);
                                        LOG.warn("V42 BUDDY REINFORCE: {} joining {} — ability {} → {} (+{})",
                                            card.getTitle(), siteName, targetFriendlyAbility, totalAfterDeploy, buddyBonus);
                                    } else {
                                        // Site already well-staffed — still good to join
                                        action.addReasoning(String.format(
                                            "V42 BUDDY STACK: Joining well-staffed site %s (ability %.0f)",
                                            siteName, targetFriendlyAbility), 200.0f);
                                    }
                                } else {
                                    // === DEPLOYING ALONE TO EMPTY SITE ===
                                    // Before penalizing, check if we can afford a buddy follow-up
                                    // this same turn. If another character in hand can deploy here
                                    // and we have enough Force for both, the solo deploy is temporary.
                                    boolean canAffordBuddy = false;
                                    float bestBuddyAbility = 0;
                                    String buddyName = "";
                                    int currentDeployCost = card.getBlueprint().getDeployCost() != null
                                        ? card.getBlueprint().getDeployCost().intValue() : 0;
                                    int forceAvailable = context.getForcePileSize();
                                    int forceAfterThis = forceAvailable - currentDeployCost;

                                    if (forceAfterThis > 0) {
                                        java.util.List<PhysicalCard> handCards = gameState.getHand(v42PlayerId);
                                        if (handCards != null) {
                                            for (PhysicalCard hc : handCards) {
                                                if (hc == null || hc == card) continue;
                                                if (hc.getBlueprint() == null) continue;
                                                if (hc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                int hcCost = hc.getBlueprint().getDeployCost() != null
                                                    ? hc.getBlueprint().getDeployCost().intValue() : 0;
                                                if (hcCost <= forceAfterThis) {
                                                    float hcAbility = 0;
                                                    if (hc.getBlueprint().hasAbilityAttribute()) {
                                                        Float hcAb = hc.getBlueprint().getAbility();
                                                        hcAbility = hcAb != null ? hcAb : 0;
                                                    }
                                                    if (hcAbility > bestBuddyAbility) {
                                                        bestBuddyAbility = hcAbility;
                                                        buddyName = hc.getTitle() != null ? hc.getTitle() : "unknown";
                                                        canAffordBuddy = true;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    float combinedAbility = cardAbility + bestBuddyAbility;

                                    if (targetOpponentCount > 0) {
                                        // Opponents AT this site — evaluate based on our strength
                                        Float cardPower = card.getBlueprint().hasPowerAttribute()
                                            ? card.getBlueprint().getPower() : null;
                                        float myPower = cardPower != null ? cardPower : 0;

                                        if (cardAbility >= 4.0f && myPower >= targetOpponentPower) {
                                            // V43: Strong character (ability 4+) vs weaker opponent — GO!
                                            // Vader (ability 6, power 6) vs a solo Rebel = easy kill
                                            float engageAloneBonus = 300.0f;
                                            if (myPower >= targetOpponentPower * 1.5f) engageAloneBonus = 400.0f;
                                            action.addReasoning(String.format(
                                                "V43 STRONG SOLO ENGAGE: %s (ability %.0f, power %.0f) vs %d opponents (power %.0f) at %s — we dominate!",
                                                card.getTitle(), cardAbility, myPower, targetOpponentCount, targetOpponentPower, siteName), engageAloneBonus);
                                            LOG.warn("V43 STRONG SOLO ENGAGE: {} (a{}/p{}) vs {} opponents (p{}) at {} — APPROVED (+{})",
                                                card.getTitle(), (int)cardAbility, (int)myPower, targetOpponentCount, (int)targetOpponentPower, siteName, (int)engageAloneBonus);
                                        } else if (cardAbility >= 4.0f) {
                                            // V43: Ability 4+ but outpowered — risky but not suicide
                                            action.addReasoning(String.format(
                                                "V43 STRONG SOLO RISKY: %s (ability %.0f, power %.0f) vs opponents (power %.0f) at %s — outpowered but can draw destiny",
                                                card.getTitle(), cardAbility, myPower, targetOpponentPower, siteName), -50.0f);
                                            LOG.warn("V43 STRONG SOLO RISKY: {} (a{}/p{}) vs opponents (p{}) at {} — ability OK but outpowered (-50)",
                                                card.getTitle(), (int)cardAbility, (int)myPower, (int)targetOpponentPower, siteName);
                                        } else if (canAffordBuddy && combinedAbility >= 4.0f) {
                                            action.addReasoning(String.format(
                                                "V42 BUDDY PLANNED CONTEST: %s + %s (ability %.0f) can contest %d opponents at %s!",
                                                card.getTitle(), buddyName, combinedAbility, targetOpponentCount, siteName), 100.0f);
                                            LOG.warn("V42 BUDDY PLANNED CONTEST: {} + {} (ability {}) vs {} opponents at {} — affordable follow-up!",
                                                card.getTitle(), buddyName, combinedAbility, targetOpponentCount, siteName);
                                        } else {
                                            // V43: Weak character alone into opponents — block, but not -9999
                                            // -9999 was too harsh and overrode ALL other bonuses
                                            action.addReasoning(String.format(
                                                "V43 BUDDY BLOCK: %s (ability %.0f) alone into %d opponents at %s — too weak!",
                                                card.getTitle(), cardAbility, targetOpponentCount, siteName), -500.0f);
                                            LOG.warn("V43 BUDDY BLOCK: {} (ability {}) alone into {} opponents at {} — BLOCKED (-500)",
                                                card.getTitle(), (int)cardAbility, targetOpponentCount, siteName);
                                        }
                                    } else if (canAffordBuddy && combinedAbility >= 4.0f) {
                                        // No opponents here, and we can afford a buddy — go for it!
                                        action.addReasoning(String.format(
                                            "V42 BUDDY PLANNED: %s (ability %.0f) + %s (ability %.0f) = %.0f — buddy incoming at %s!",
                                            card.getTitle(), cardAbility, buddyName, bestBuddyAbility, combinedAbility, siteName), 100.0f);
                                        LOG.warn("V42 BUDDY PLANNED: {} + {} = ability {} at {} — Force {}/{} covers both",
                                            card.getTitle(), buddyName, combinedAbility, siteName, currentDeployCost, forceAvailable);
                                    } else if (opponentHasCharsOnBoard && cardAbility < 4.0f
                                               && hasUnderstaffedFriendlySite) {
                                        // Can't afford buddy, opponent has chars, friendlies need help
                                        action.addReasoning(String.format(
                                            "V42 BUDDY SCATTER: Ability %.0f alone at %s, no affordable buddy — reinforce existing sites!",
                                            cardAbility, siteName), -400.0f);
                                        LOG.warn("V42 BUDDY SCATTER: {} (ability {}) alone at {} — no buddy affordable, friendlies need help! (-400)",
                                            card.getTitle(), cardAbility, siteName);
                                    } else if (opponentHasCharsOnBoard && cardAbility < 4.0f) {
                                        // Can't afford buddy, opponent has chars, but no understaffed friendlies
                                        action.addReasoning(String.format(
                                            "V42 BUDDY CAUTION: Ability %.0f alone at %s, no affordable buddy — vulnerable (-200)",
                                            cardAbility, siteName), -200.0f);
                                        LOG.warn("V42 BUDDY CAUTION: {} (ability {}) solo at {} — no buddy, opponents on board (-200)",
                                            card.getTitle(), cardAbility, siteName);
                                    } else if (hasFriendlyCharAnywhere && hasUnderstaffedFriendlySite
                                               && !canAffordBuddy) {
                                        // No buddy affordable, friendlies elsewhere need help
                                        action.addReasoning(String.format(
                                            "V42 BUDDY REGROUP: No affordable buddy — reinforce existing sites, don't spread to %s (-200)",
                                            siteName), -200.0f);
                                        LOG.warn("V42 BUDDY REGROUP: {} to empty {} — no buddy, understaffed sites need help (-200)",
                                            card.getTitle(), siteName);
                                    } else if (!hasFriendlyCharAnywhere) {
                                        // First character on the board — fine, has to go somewhere
                                        action.addReasoning(String.format(
                                            "V42 BUDDY PIONEER: First character on board — %s is fine",
                                            siteName), 0.0f);
                                    } else {
                                        // All friendly sites are well-staffed, or buddy is affordable — OK to expand
                                        action.addReasoning(String.format(
                                            "V42 BUDDY EXPAND: Existing sites secured — expanding to %s",
                                            siteName), 0.0f);
                                    }
                                }
                            }
                            // V42: SPY AVOIDANCE — deploy away from undercover spies
                            if (targetSite != null && targetSpyCount > 0) {
                                action.addReasoning(String.format(
                                    "V42 SPY AVOID: %d undercover spy(s) at %s — deploy elsewhere! Spy will break cover and ambush!",
                                    targetSpyCount, targetSite.getTitle()), -500.0f);
                                LOG.warn("V42 SPY AVOID: {} — {} spy(s) at {} — penalty -500",
                                    card.getTitle(), targetSpyCount, targetSite.getTitle());
                            }
                        } catch (Exception e) {
                            LOG.debug("V42 BUDDY SYSTEM: Error: {}", e.getMessage());
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
                                        // V39.2: Restore AMSD prefer for TDIGWATT, mild for Hunt Down
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer amsdObjCheck =
                                            context.getObjectiveAnalyzer();
                                        boolean isHuntDownAmsd = amsdObjCheck != null && amsdObjCheck.isAnalyzed()
                                            && amsdObjCheck.isHuntDownV();
                                        float amsdPenalty = isHuntDownAmsd ? -5.0f : -20.0f; // V40.1: very mild preference for AMSD
                                        action.addReasoning(String.format(
                                            "V39.2 AMSD: %s in reserve + AMSD on table — prefer AMSD pull (penalty %.0f)",
                                            matchingShipName, amsdPenalty), amsdPenalty);
                                        LOG.warn("V39.2 AMSD: {} in reserve — penalty {} (huntDown={})",
                                            matchingShipName, (int)amsdPenalty, isHuntDownAmsd);
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
                                    // V39: Ship vs overwhelming opponent — very mild caution (was -100)
                                    float shipPenalty = -10.0f;
                                    action.addReasoning(String.format(
                                        "V39 SHIP CAUTION: %s (power %.0f) vs opponent ships (power %.0f) at %s (very mild)",
                                        card.getTitle(), ourShipPower, oppShipPower, sysLoc.getTitle()), shipPenalty);
                                    LOG.warn("V39 SHIP CAUTION: {} power {} vs opponent {} at {} — very mild (-10, was -100)",
                                        card.getTitle(), (int)ourShipPower, (int)oppShipPower, sysLoc.getTitle());
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
                                && execObjAnalyzer.needsBespinSystemPresence()
                                && !execObjAnalyzer.isHuntDownV()) { // V39: TDIGWATT only

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
                                        // V40.1: Turns 1-2: ABSOLUTE priority — Executor is the engine
                                        action.addReasoning("V40.1 EXECUTOR: Bespin on table — deploy NOW!", 3000.0f);
                                        LOG.warn("V40.1 EXECUTOR: {} turn {} + Bespin on table — +3000!", card.getTitle(), execTurn);
                                    } else {
                                        action.addReasoning("V40.1 EXECUTOR: Key ship for TDIGWATT — deploy to Bespin!", 1500.0f);
                                        LOG.warn("V40.1 EXECUTOR: {} + Bespin on table — +1500!", card.getTitle());
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

                    // === V40.1/V41.2: PIETT DEPLOY — HOLD FOR AMSD ===
                    // Piett is the matching pilot for Executor. He should NEVER deploy to ground
                    // when AMSD is on the table and Executor is still available — AMSD needs Piett
                    // IN HAND to fire. Deploying Piett to ground wastes the AMSD + Executor combo.
                    // V41.2: HARD BLOCK ground deploy when AMSD + Executor available.
                    if (cardTitleLower.contains("piett") || cardTitleLower.contains("gherant")) {
                        boolean deployingAboardShip = actionLower.contains("aboard") || actionLower.contains("pilot")
                            || actionLower.contains("executor") || actionLower.contains("simultaneously");
                        if (deployingAboardShip) {
                            action.addReasoning("V40.1 PILOT ABOARD: Deploy aboard ship!", 300.0f);
                        } else if (cardTitleLower.contains("piett")) {
                            // V41.2: Check if AMSD is on table and Executor is available
                            // If so, Piett MUST stay in hand for AMSD — deploying to ground kills the combo
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer piettObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            boolean isTdigwatt = piettObjAnalyzer != null && piettObjAnalyzer.isAnalyzed()
                                && piettObjAnalyzer.needsBespinSystemPresence();
                            if (isTdigwatt) {
                                // Check if AMSD is on table
                                boolean amsdOnTable = false;
                                boolean executorOnTable = false;
                                try {
                                    for (com.gempukku.swccgo.game.PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                        if (tableCard == null || !playerId.equals(tableCard.getOwner())) continue;
                                        com.gempukku.swccgo.common.Zone tz = tableCard.getZone();
                                        if (tz == null || !tz.isInPlay()) continue;
                                        String tTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (tTitle.contains("alert my star destroyer")) amsdOnTable = true;
                                        if (tTitle.contains("executor") && tableCard.getBlueprint() != null
                                            && tableCard.getBlueprint().getCardCategory() == CardCategory.STARSHIP) executorOnTable = true;
                                    }
                                } catch (Exception e) { /* ignore */ }

                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle piettOracle = context.getDeckOracle();
                                boolean executorAvailable = false;
                                if (piettOracle != null && piettOracle.isAnalyzed()) {
                                    executorAvailable = piettOracle.isCardInReserve("Executor")
                                        || piettOracle.isCardInReserve("Flagship Executor")
                                        || piettOracle.isCardInHand("Executor")
                                        || piettOracle.isCardInHand("Flagship Executor");
                                }

                                if (amsdOnTable && !executorOnTable && executorAvailable) {
                                    // AMSD is ready, Executor available, not deployed yet — Piett MUST stay in hand!
                                    action.addReasoning("V41.2 PIETT HOLD FOR AMSD: AMSD on table + Executor available — keep Piett in hand for AMSD!", -9999.0f);
                                    LOG.warn("V41.2 PIETT GROUND BLOCK: AMSD on table, Executor in reserve/hand — Piett MUST stay in hand!");
                                } else if (!amsdOnTable && !executorOnTable) {
                                    // No AMSD — Piett can deploy to ground but prefer ships
                                    LOG.info("V41.2: No AMSD on table — Piett ground deploy OK");
                                }
                            }
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
