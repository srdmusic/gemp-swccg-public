package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PassEvaluatorIntegerGuardTest {

    @Test
    public void bothBotsLeaveIntegerValuesToTheValuePicker() {
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "bot", "INTEGER", "Draw up to three cards", "1", Phase.DRAW);
        rando.setMin(0);
        rando.setNoPass(false);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "bot", "INTEGER", "Draw up to three cards", "1", Phase.DRAW);
        chosen.setMin(0);
        chosen.setNoPass(false);

        assertFalse(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .canEvaluate(rando));
        assertFalse(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .canEvaluate(chosen));
    }

    @Test
    public void nonIntegerOptionalChoicesRemainPassable() {
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "bot", "MULTIPLE_CHOICE", "Choose one", "1", Phase.DRAW);
        rando.setMin(0);
        rando.setNoPass(false);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "bot", "MULTIPLE_CHOICE", "Choose one", "1", Phase.DRAW);
        chosen.setMin(0);
        chosen.setNoPass(false);

        assertTrue(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .canEvaluate(rando));
        assertTrue(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .canEvaluate(chosen));
    }
}
