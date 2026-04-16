package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates force activation decisions (INTEGER type).
 *
 * Determines the optimal amount of force to activate based on:
 * - Current force pile
 * - Reserve deck size
 * - Turn number and strategy
 * - Future turn planning (save for expensive cards)
 * - Late-game life force preservation
 *
 * Ported from Python ForceActivationEvaluator
 */
public class ForceActivationEvaluator extends ActionEvaluator {

    // Config constants
    private static final int MAX_FORCE_PILE = 25;
    private static final int RESERVE_FOR_DESTINY_CONTESTED = 4;
    private static final int RESERVE_FOR_DESTINY_SAFE = 1;
    private static final int LATE_GAME_LIFE_FORCE = 12;
    private static final int CRITICAL_LIFE_FORCE = 6;

    public ForceActivationEvaluator() {
        super("ForceActivation");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // Handle all INTEGER decisions - force activation is the most common
        return "INTEGER".equals(context.getDecisionType());
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String textLower = context.getDecisionText().toLowerCase();

        // Parse min/max from context
        int minVal = context.getMin();
        int maxVal = context.getMax();

        // Ensure we have valid bounds
        if (maxVal == 0) {
            maxVal = 1;
            logger.warn("No max value found, using fallback: {}", maxVal);
        }

        // Special case: "allow opponent to activate" - just let them activate max
        if (textLower.contains("allow opponent to activate") || textLower.contains("opponent to activate")) {
            EvaluatedAction action = new EvaluatedAction(
                String.valueOf(maxVal),
                ActionType.ACTIVATE_FORCE,
                50.0f,
                String.format("Allow opponent to activate %d force", maxVal)
            );
            action.addReasoning("Allowing opponent max activation (they'll waste force)");
            actions.add(action);
            return actions;
        }

        if (gameState == null) {
            // No game state - use max value
            EvaluatedAction action = new EvaluatedAction(
                String.valueOf(maxVal),
                ActionType.ACTIVATE_FORCE,
                50.0f,
                String.format("INTEGER response: %d (no game state)", maxVal)
            );
            action.addReasoning("No game state available, defaulting to max");
            actions.add(action);
            return actions;
        }

        // V42: Use calculateActivationAmount which ALWAYS reserves cards for destiny draws.
        // Old V38.2 logic only saved reserve when reserveDeck-maxVal < 4, which meant
        // early game activated everything and depleted reserve before the threshold kicked in.
        int amount = calculateActivationAmount(context, maxVal);
        logger.warn("V42 FORCE ACTIVATION: activating {} of {} (reserve={}, forcePile={}, hand={}, lifeForce={})",
            amount, maxVal, context.getReserveDeckSize(), context.getForcePileSize(),
            context.getHandSize(), context.getLifeForce());

        // V43: ALWAYS activate at least 1 Force when asked. Activating 0
        // causes the engine to re-ask the same question, creating an infinite loop.
        // The game engine only asks this question when activation is possible.
        if (amount <= 0 && maxVal > 0) {
            amount = 1;
            logger.warn("V43 FORCE ACTIVATION: Forced minimum 1 (was 0, reserve preservation too aggressive)");
        }

        // Ensure amount is within bounds
        amount = Math.max(minVal, Math.min(amount, maxVal));

        // Build action with reasoning
        EvaluatedAction action = new EvaluatedAction(
            String.valueOf(amount),
            ActionType.ACTIVATE_FORCE,
            50.0f,
            String.format("Activate %d of %d force", amount, maxVal)
        );

        // Add reasoning based on decision factors
        int forcePile = context.getForcePileSize();
        if (forcePile > 12) {
            action.addReasoning(String.format("Force pile high (%d) - conserving", forcePile));
        }

        int reserveTotal = context.getLifeForce();
        if (reserveTotal <= 20) {
            action.addReasoning(String.format("Reserve low (%d) - saving for destiny", reserveTotal));
        }

        if (amount == maxVal) {
            action.addReasoning("Activating full amount available", 10.0f);
        } else if (amount == 0) {
            action.addReasoning("Skipping activation this turn", -10.0f);
        } else {
            action.addReasoning(String.format("Activating partial (%d/%d)", amount, maxVal));
        }

        actions.add(action);
        return actions;
    }

    /**
     * Calculate optimal force activation amount.
     *
     * V57 FIX 19 (2026-04-16): Removed throttling rules entirely. See the
     * matching Rando ForceActivationEvaluator for the full rationale. In
     * short: Rules 1 and 3 were collapsing activation to 0 (then V43 forced
     * 1) whenever reserve or life force got low, causing Rando to activate
     * 1 of 14 Force generation on Turn 9 of the 2026-04-16 replay. Steve's
     * philosophy is to activate all force every turn, so we now just return
     * the max.
     */
    private int calculateActivationAmount(DecisionContext context, int maxAvailable) {
        int currentForce = context.getForcePileSize();
        int reserveDeck = context.getReserveDeckSize();
        int lifeForce = context.getLifeForce();
        int handSize = context.getHandSize();

        logger.warn("V57 ACTIVATE FULL: activating {} (reserve={}, forcePile={}, hand={}, lifeForce={})",
            maxAvailable, reserveDeck, currentForce, handSize, lifeForce);

        return maxAvailable;
    }
}
