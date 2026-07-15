package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceActivationRoutingParityTest {

    @Test
    public void genericIntegerIsNotClaimedByEitherBot() {
        assertClaimed(false, "Choose amount of Force to activate", false);
        assertClaimed(false, "Choose how many cards to draw", false);
    }

    @Test
    public void latchedAmountIsClaimedByBothBots() {
        assertClaimed(true, "Choose amount of Force to activate", true);
    }

    @Test
    public void exactOpponentAllowanceIsClaimedWithoutLatch() {
        assertClaimed(true, ActivateDecisionRouting.OPPONENT_ALLOWANCE_PROMPT, false);
        assertClaimed(false, "Allow opponent to activate Force for a card effect", false);
    }

    private static void assertClaimed(boolean expected, String text, boolean latched) {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext =
            new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "rando", "INTEGER", text, "1", Phase.ACTIVATE);
        randoContext.setActivationAmountDecision(latched);
        boolean rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ForceActivationEvaluator()
            .canEvaluate(randoContext);

        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext =
            new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "chosen", "INTEGER", text, "1", Phase.ACTIVATE);
        chosenContext.setActivationAmountDecision(latched);
        boolean chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ForceActivationEvaluator()
            .canEvaluate(chosenContext);

        if (expected) {
            assertTrue(rando);
            assertTrue(chosen);
        } else {
            assertFalse(rando);
            assertFalse(chosen);
        }
    }
}
