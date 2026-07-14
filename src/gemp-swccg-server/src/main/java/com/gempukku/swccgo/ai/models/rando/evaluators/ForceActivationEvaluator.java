package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.ActivateAmountPolicy;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: ACTIVATE (reorg 2026-07-06) ═══
// Owns: how MUCH Force to activate (INTEGER amount): V57 economy curve, V67at tuning, V42 pacing,
// V43 floor, V61c keep-3 destiny cap. Hub: none. KIND mix (ACTIVATE overall): 4 VETO / 3 ORDERING;
// key magnitudes live in ActionTextEvaluator: the V61c -6000 / V168 +5000 / V38.3 +500 triangle is ONE boundary.
// V61c battle-intent gate uses the shared predicate DecisionContext.isBattlePlausibleThisTurn().
// Three sites must agree: ActivateAmountPolicy, ATE's V168/V61c action block, and the typed
// ACTIVATE_ZERO_CONFIRM owner.
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

    public ForceActivationEvaluator() {
        super("ForceActivation");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        if (!"INTEGER".equals(context.getDecisionType())) {
            return false;
        }
        DecisionOrigin origin = context.getDecisionOrigin();
        return origin == DecisionOrigin.ACTIVATE_AMOUNT
            || origin == DecisionOrigin.ACTIVATE_ALLOWANCE;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
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
        if (context.getDecisionOrigin() == DecisionOrigin.ACTIVATE_ALLOWANCE) {
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
        int currentForce = context.getForcePileSize();
        int reserveDeck = context.getReserveDeckSize();
        int lifeForce = context.getLifeForce();
        int handSize = context.getHandSize();
        ActivateAmountPolicy.Result amountResult = ActivateAmountPolicy.assess(
            new ActivateAmountPolicy.Input(minVal, maxVal, reserveDeck, lifeForce,
                context.isBattlePlausibleThisTurn()));
        int amount = amountResult.amount();
        String mode = switch (amountResult.mode()) {
            case KEEP_THREE_FOR_BATTLE -> "V61c DESTINY BUFFER (keep 3 in reserve)";
            case KEEP_TWO_AT_LOW_LIFE -> "V67at END-GAME RESERVE-2 (lifeForce <= 10)";
            case ACTIVATE_FULL -> "V57 ACTIVATE FULL";
        };
        logger.warn("{}: activating {} of {} (reserve={}, forcePile={}, hand={}, lifeForce={})",
            mode, amount, maxVal, reserveDeck, currentForce, handSize, lifeForce);
        logger.warn("V42 FORCE ACTIVATION: activating {} of {} (reserve={}, forcePile={}, hand={}, lifeForce={})",
            amount, maxVal, context.getReserveDeckSize(), context.getForcePileSize(),
            context.getHandSize(), context.getLifeForce());

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
        } else {
            action.addReasoning(String.format("Activating partial (%d/%d)", amount, maxVal));
        }

        actions.add(action);
        return actions;
    }

}
