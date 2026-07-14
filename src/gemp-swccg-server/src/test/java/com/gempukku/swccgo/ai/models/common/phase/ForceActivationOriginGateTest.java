package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceActivationOriginGateTest {

    @Test
    public void onlyTypedActivateIntegerOriginsAreOwnedByEitherBot() {
        for (DecisionOrigin origin : DecisionOrigin.values()) {
            boolean expected = origin == DecisionOrigin.ACTIVATE_AMOUNT
                    || origin == DecisionOrigin.ACTIVATE_ALLOWANCE;
            assertEquals(origin.name(), expected, rando().canEvaluate(
                    randoContext(origin, "INTEGER", "unrelated prompt")));
            assertEquals(origin.name(), expected, chosen().canEvaluate(
                    chosenContext(origin, "INTEGER", "unrelated prompt")));
        }
        assertFalse(rando().canEvaluate(
                randoContext(null, "INTEGER", "allow opponent to activate")));
        assertFalse(chosen().canEvaluate(
                chosenContext(null, "INTEGER", "allow opponent to activate")));
    }

    @Test
    public void wrongWireTypeNeverReachesForceActivation() {
        assertFalse(rando().canEvaluate(
                randoContext(DecisionOrigin.ACTIVATE_AMOUNT, "MULTIPLE_CHOICE", "amount")));
        assertFalse(chosen().canEvaluate(
                chosenContext(DecisionOrigin.ACTIVATE_ALLOWANCE, "CARD_ACTION_CHOICE", "allowance")));
    }

    @Test
    public void passEvaluatorNeverClaimsIntegerValues() {
        var randoContext = randoContext(null, "INTEGER", "Draw up to three cards");
        randoContext.setMin(0);
        var chosenContext = chosenContext(null, "INTEGER", "Draw up to three cards");
        chosenContext.setMin(0);

        assertFalse(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .canEvaluate(randoContext));
        assertFalse(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .canEvaluate(chosenContext));
    }

    @Test
    public void allowanceUsesOriginInsteadOfPromptText() {
        var randoContext = randoContext(DecisionOrigin.ACTIVATE_ALLOWANCE,
                "INTEGER", "prompt text deliberately has no activation words");
        randoContext.setMin(1);
        randoContext.setMax(4);
        var chosenContext = chosenContext(DecisionOrigin.ACTIVATE_ALLOWANCE,
                "INTEGER", "prompt text deliberately has no activation words");
        chosenContext.setMin(1);
        chosenContext.setMax(4);

        var randoActions = rando().evaluate(randoContext);
        var chosenActions = chosen().evaluate(chosenContext);

        assertEquals("4", randoActions.get(0).getActionId());
        assertEquals("4", chosenActions.get(0).getActionId());
        assertTrue(randoActions.get(0).getDisplayText().contains("Allow opponent"));
        assertTrue(chosenActions.get(0).getDisplayText().contains("Allow opponent"));
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.ForceActivationEvaluator
    rando() {
        return new com.gempukku.swccgo.ai.models.rando.evaluators.ForceActivationEvaluator();
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.ForceActivationEvaluator
    chosen() {
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators.ForceActivationEvaluator();
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext
    randoContext(DecisionOrigin origin, String type, String text) {
        return new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "player", type, text, "1", Phase.ACTIVATE, origin);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext
    chosenContext(DecisionOrigin origin, String type, String text) {
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "player", type, text, "1", Phase.ACTIVATE, origin);
    }
}
