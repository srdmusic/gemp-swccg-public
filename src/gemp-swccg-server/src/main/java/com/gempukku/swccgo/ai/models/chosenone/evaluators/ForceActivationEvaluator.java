package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.ActivateAmountPolicy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: ACTIVATE (reorg 2026-07-06) ═══
// Owns: how MUCH Force to activate (INTEGER amount): V57 economy curve, V67at tuning, V42 pacing,
// V43 floor, V61c keep-3 destiny cap. Hub: none. KIND mix (ACTIVATE overall): 4 VETO / 3 ORDERING;
// key magnitudes live in ActionTextEvaluator: the V61c -6000 / V168 +5000 / V38.3 +500 triangle is ONE boundary.
// V61c battle-intent gate uses the shared predicate DecisionContext.isBattlePlausibleThisTurn() —
// THREE sites must agree: the keep-3 cap here, ATE's V168/V61c activate block, ATE's V38.3 confirm carve-out.
// Absorbs (dead, commented below/nearby — revert path, do not delete): none in this file.
// Cross-refs: ACTIVATE region in ActionTextEvaluator (whether/interleave), PULL-ENGINE (V97: pulls fire
// BEFORE activating), BATTLE-1 (V61b shares the battle-plausible scan). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
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

        // Special case: "allow opponent to activate" - just let them activate max.
        // Per Steve (2026-05-25): keep peer-priority with self-activate. Allowing
        // opponent to activate force is NORMAL play (standard SWCCG rule), not a
        // last-resort. V132 (which had dropped this to 10) reverted.
        if (textLower.contains("allow opponent to activate") || textLower.contains("opponent to activate")) {
            EvaluatedAction action = new EvaluatedAction(
                String.valueOf(maxVal),
                ActionType.ACTIVATE_FORCE,
                50.0f,
                String.format("Allow opponent to activate %d force", maxVal)
            );
            action.addReasoning("Allowing opponent max activation (normal SWCCG rule)");
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
     * V57 FIX 19 (2026-04-16): Removed throttling rules entirely.
     *
     * Previously this method applied three throttling rules BEFORE logging
     * "V52 ACTIVATE 100% (no throttling)" — which was misleading, because
     * Rules 1 and 3 had already clamped `amount` by the time that log fired:
     *
     *   Rule 1 (destiny reserve):  amount = min(amount, reserveDeck - 4)
     *       → On Turn 9 in the 2026-04-16 replay, reserveDeck was low,
     *         so amount collapsed to 0, then V43 forced it to 1. Rando
     *         activated 1 of 14 Force generation — completely passive.
     *
     *   Rule 3 (critical life force < 6):
     *       amount = min(amount, max(1, 6 - currentForce))
     *       → Same catastrophic under-activation in late game.
     *
     * Steve's philosophy is explicit: activate ALL force every turn, deploy
     * everything, draw the rest into hand. Hoarding force pile or "saving"
     * reserve deck just makes Rando die passively instead of fighting.
     *
     * V43 still guarantees minimum 1 (prevents engine loops). If reserve
     * deck truly runs out, the normal loss condition takes over — but we
     * don't artificially hobble ourselves on the way there.
     */
    private int calculateActivationAmount(DecisionContext context, int maxAvailable) {
        int reserveDeck = context.getReserveDeckSize();
        int lifeForce = context.getLifeForce();
        boolean battlePlausible = context.isBattlePlausibleThisTurn();
        ActivateAmountPolicy.Result result = ActivateAmountPolicy.assess(
            new ActivateAmountPolicy.Input(
                context.getMin(), maxAvailable, reserveDeck, lifeForce, battlePlausible));

        if (!battlePlausible && reserveDeck - 3 < maxAvailable) {
            logger.warn("V61c BATTLE-INTENT: no contested location, activating full");
        }
        String mode = switch (result.mode()) {
            case ACTIVATE_FULL -> "V57 ACTIVATE FULL";
            case KEEP_THREE_FOR_BATTLE -> "V61c DESTINY BUFFER (keep 3 in reserve)";
            case KEEP_TWO_AT_LOW_LIFE -> "V67at END-GAME RESERVE-2 (lifeForce <= 10)";
        };
        logger.warn("{}: activating {} of {} (reserve={}, forcePile={}, hand={}, lifeForce={})",
            mode, result.amount(), maxAvailable, reserveDeck, context.getForcePileSize(),
            context.getHandSize(), lifeForce);
        return result.amount();
    }
}
