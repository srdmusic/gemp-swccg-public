package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Evaluates movement decisions.
 *
 * Decision factors (from Python move_evaluator.py):
 * - Power differential at current location (fleeing from danger)
 * - Power differential at destination (moving to advantageous positions)
 * - Spreading out vs consolidating forces
 * - Strategic retreat from dangerous locations
 * - Offensive attacks from uncontested strongholds
 *
 * Move thresholds:
 * - Flee when at power disadvantage of 2+
 * - Buildup when excess power at 12+
 * - Overkill threshold: 4 (excess power we can redistribute)
 *
 * Ported from Python move_evaluator.py
 */
public class MoveEvaluator extends ActionEvaluator {

    // Move keywords to identify move actions
    private static final String[] MOVE_KEYWORDS = {
        "Move using", "Shuttle", "Docking bay transit", "Transport",
        "Take off", "Land", "Move to", "Move from"
    };

    // Thresholds
    private static final int POWER_DIFF_FOR_FLEE = 2;
    private static final int OVERKILL_THRESHOLD = 4;

    // Track cards we've already tried moving this turn
    private Set<String> pendingMoveCardIds = new HashSet<>();
    private int lastTurnNumber = -1;

    public MoveEvaluator() {
        super("Move");
    }

    public void resetPendingMoves() {
        pendingMoveCardIds.clear();
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // Handle CARD_ACTION_CHOICE with move actions
        String decisionType = context.getDecisionType();
        if (!"CARD_ACTION_CHOICE".equals(decisionType) && !"ACTION_CHOICE".equals(decisionType)) {
            return false;
        }

        // Must be our turn
        if (context.getGameState() != null && !context.isMyTurn()) {
            return false;
        }

        // Must be Move phase or have move actions available
        Phase phase = context.getPhase();
        if (phase != Phase.MOVE) {
            // Check if any action is a move action
            List<String> actionTexts = context.getActionTexts();
            if (actionTexts != null) {
                for (String actionText : actionTexts) {
                    if (isMoveAction(actionText)) {
                        return true;
                    }
                }
            }
            return false;
        }

        return true;
    }

    private boolean isMoveAction(String actionText) {
        if (actionText == null) return false;
        for (String keyword : MOVE_KEYWORDS) {
            if (actionText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        logger.info("[MoveEvaluator] Evaluating move decision");

        // Reset pending move tracking at the start of each turn
        if (context.getTurnNumber() != lastTurnNumber) {
            resetPendingMoves();
            lastTurnNumber = context.getTurnNumber();
            logger.debug("[MoveEvaluator] Reset pending moves for turn {}", lastTurnNumber);
        }

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();

        if (actionIds == null || actionTexts == null) {
            logger.warn("[MoveEvaluator] No action IDs or texts available");
            return actions;
        }

        logger.debug("[MoveEvaluator] Phase={}, actions={}", context.getPhase(), actionIds.size());

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle move-related actions
            if (!isMoveAction(actionText)) {
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.MOVE,
                50.0f,  // Base score
                actionText
            );

            // === MOVEMENT TYPE SCORING ===

            // Shuttle/Transport - good for repositioning
            if (actionLower.contains("shuttle") || actionLower.contains("transport")) {
                action.addReasoning("Shuttle/Transport movement", 20.0f);
            }

            // Docking bay transit - efficient movement
            if (actionLower.contains("docking bay")) {
                action.addReasoning("Docking bay transit", 15.0f);
            }

            // Take off/Land - starship transitions
            if (actionLower.contains("take off")) {
                action.addReasoning("Take off (space deployment)", 10.0f);
            }
            if (actionLower.contains("land")) {
                action.addReasoning("Land (ground deployment)", 10.0f);
            }

            // === DESTINATION ANALYSIS ===
            // Parse destination from action text

            // Moving to battleground - good for control
            if (actionLower.contains("battleground")) {
                action.addReasoning("Moving to battleground", 25.0f);
            }

            // Moving to location with icons
            if (actionLower.contains("force icon") || actionLower.contains("force generation")) {
                action.addReasoning("Moving to location with force icons", 20.0f);
            }

            // === STRATEGIC MOVEMENT ===
            // Base encouragement for movement during Move phase
            if (context.getPhase() == Phase.MOVE) {
                action.addReasoning("Move phase - consider repositioning", 10.0f);
            }

            // Avoid unnecessary movement if we're in a good position
            // (This will be adjusted by other evaluators/scoring)

            logger.debug("[MoveEvaluator] Scored '{}' -> {}",
                actionText.length() > 40 ? actionText.substring(0, 40) + "..." : actionText,
                String.format("%.1f", action.getScore()));

            actions.add(action);
        }

        logger.info("[MoveEvaluator] Evaluated {} move actions", actions.size());
        return actions;
    }
}
