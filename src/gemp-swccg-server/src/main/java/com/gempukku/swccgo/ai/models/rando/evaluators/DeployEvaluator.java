package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan;
import com.gempukku.swccgo.common.CardCategory;
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
        // If none match and we have deploy actions, the plan is stale
        if (plan != null && !plan.getInstructions().isEmpty() && !plan.isPlanComplete()) {
            boolean planCardsAvailable = false;
            for (int i = 0; i < actionTexts.size(); i++) {
                String actionText = actionTexts.get(i);
                if (actionText == null || !actionText.toLowerCase(Locale.ROOT).contains("deploy")) {
                    continue;
                }
                // Try to match action to a plan instruction
                PhysicalCard matchedCard = findCardInHand(hand, actionText);
                if (matchedCard != null) {
                    String bpId = matchedCard.getBlueprintId(true);
                    if (bpId != null && plan.getInstructionForCard(bpId) != null) {
                        planCardsAvailable = true;
                        break;
                    }
                }
            }

            if (!planCardsAvailable) {
                LOG.warn("⚠️ STALE PLAN: Plan has {} cards but NONE are in available deploy actions!",
                    plan.getInstructions().size());
                StringBuilder planCards = new StringBuilder();
                for (DeploymentInstruction inst : plan.getInstructions()) {
                    planCards.append(inst.getCardName()).append(", ");
                }
                LOG.warn("   Plan cards: {}", planCards);
                // Mark plan as allowing extra actions since planned cards aren't available
                plan.setForceAllowExtras(true);
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

            // === Get card ID from decision parameters ===
            // For CARD_ACTION_CHOICE, each action has an associated cardId at the same index
            List<String> cardIdList = context.getCardIds();
            List<String> blueprintList = context.getBlueprints();
            String cardIdStr = (cardIdList != null && i < cardIdList.size()) ? cardIdList.get(i) : null;
            String blueprintIdFromParam = (blueprintList != null && i < blueprintList.size()) ? blueprintList.get(i) : null;

            LOG.debug("[DeployEvaluator] Action[{}]: cardId='{}', blueprintId='{}', text='{}'",
                i, cardIdStr, blueprintIdFromParam, actionText);

            // Check if we already tried deploying this card (avoid loops)
            if (cardIdStr != null && !cardIdStr.isEmpty() && pendingDeployCardIds.contains(cardIdStr)) {
                action.addReasoning("Already tried deploying this card", -100.0f);
                actions.add(action);
                continue;
            }

            // === LOCATION DEPLOYMENT - Highest Priority ===
            // Deploying locations opens up deployment options
            if (actionLower.contains("location") || actionLower.contains("site") || actionLower.contains("system")) {
                action.addReasoning("LOCATION - deploy first!", 200.0f);
                actions.add(action);
                continue;
            }

            // === Look up the card using cardId from parameters ===
            PhysicalCard card = null;

            // First try to find card by cardId in game state
            if (cardIdStr != null && !cardIdStr.isEmpty() && gameState != null) {
                try {
                    int cardIdNum = Integer.parseInt(cardIdStr);
                    card = gameState.findCardById(cardIdNum);
                    if (card != null) {
                        LOG.debug("[DeployEvaluator] Found card by ID {}: {}", cardIdNum, card.getTitle());
                    }
                } catch (NumberFormatException e) {
                    LOG.debug("[DeployEvaluator] Could not parse cardId: {}", cardIdStr);
                }
            }

            // Fallback: try to match by title in action text
            if (card == null) {
                card = findCardInHand(hand, actionText);
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
                                // Card is NOT in plan and we're not allowing extras - penalty
                                action.addReasoning("NOT in deployment plan", -50.0f);
                            } else {
                                // Plan allows extras (stale plan) - neutral score for non-plan cards
                                action.addReasoning("Extra deploy (plan stale)", 0.0f);
                            }
                        } else if (plan.isPlanComplete()) {
                            // Plan is complete! Allow extra deploys with moderate bonus
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
                    } catch (UnsupportedOperationException e) {
                        // Card type doesn't support deployCost (e.g., Interrupt)
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

                    // Characters with high ability are valuable
                    if (category == CardCategory.CHARACTER && abilityVal >= 4) {
                        action.addReasoning("High-ability character", 25.0f);
                    }

                    // Starships and vehicles for board presence
                    if (category == CardCategory.STARSHIP || category == CardCategory.VEHICLE) {
                        action.addReasoning("Starship/Vehicle deployment", 15.0f);
                    }

                    // === PILOT BONUS ===
                    if (AiCardHelper.isPilot(card)) {
                        action.addReasoning("Pilot character", 10.0f);
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
                // Unknown card - modest penalty
                action.addReasoning("Unknown card", -10.0f);
            }

            // Track that we're considering this card (use cardIdStr from params)
            if (cardIdStr != null && !cardIdStr.isEmpty()) {
                pendingDeployCardIds.add(cardIdStr);
            }

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
     * Try to find a card in hand that matches the action text.
     */
    private PhysicalCard findCardInHand(List<PhysicalCard> hand, String actionText) {
        if (hand == null || actionText == null) {
            LOG.debug("[findCardInHand] Null input: hand={}, actionText={}", hand != null, actionText != null);
            return null;
        }

        String actionLower = actionText.toLowerCase(Locale.ROOT);

        // Log what we're searching for
        LOG.debug("[findCardInHand] Searching for card in action: '{}'",
            actionText.length() > 100 ? actionText.substring(0, 100) + "..." : actionText);

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

        // No match - log all card titles we checked
        if (LOG.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("[findCardInHand] ❌ No match. Hand cards: ");
            for (PhysicalCard card : hand) {
                if (card != null && card.getTitle() != null) {
                    sb.append("'").append(card.getTitle()).append("', ");
                }
            }
            LOG.debug(sb.toString());
        }

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
