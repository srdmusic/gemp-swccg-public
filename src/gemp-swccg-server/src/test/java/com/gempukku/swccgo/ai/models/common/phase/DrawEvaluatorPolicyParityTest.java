package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DrawEvaluatorPolicyParityTest {

    @Test
    public void mirroredAdaptersProduceIdenticalTypedDrawActions() {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        null, "tester", "CARD_ACTION_CHOICE", "Choose Draw action or Pass",
                        "draw-parity", Phase.DRAW);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        null, "tester", "CARD_ACTION_CHOICE", "Choose Draw action or Pass",
                        "draw-parity", Phase.DRAW);

        List<String> ids = List.of("draw", "other", "destiny");
        List<String> texts = List.of("Draw card into hand from Force Pile",
                "Other action", "Draw destiny");
        randoContext.setActionIds(ids);
        randoContext.setActionTexts(texts);
        randoContext.setBlockedResponses(Set.of("draw"));
        chosenContext.setActionIds(ids);
        chosenContext.setActionTexts(texts);
        chosenContext.setBlockedResponses(Set.of("draw"));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.DrawEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DrawEvaluator()
                .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertEquals(1, chosen.size());
        assertEquals(rando.get(0).getActionId(), chosen.get(0).getActionId());
        assertEquals(Float.floatToRawIntBits(rando.get(0).getScore()),
                Float.floatToRawIntBits(chosen.get(0).getScore()));
        assertEquals(rando.get(0).getReasoningString(), chosen.get(0).getReasoningString());
        assertEquals(rando.get(0).isHardVetoed(), chosen.get(0).isHardVetoed());
        assertEquals(rando.get(0).isDeferred(), chosen.get(0).isDeferred());
    }

    @Test
    public void eachDrawCandidateReceivesOnlyItsOwnPolicyOperations() {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        null, "tester", "CARD_ACTION_CHOICE", "Choose Draw action or Pass",
                        "draw-isolation", Phase.DRAW);
        context.setActionIds(List.of("draw-one", "draw-two"));
        context.setActionTexts(List.of("Draw one card", "Draw another card"));
        context.setBlockedResponses(Set.of("draw-one"));

        var actions = new com.gempukku.swccgo.ai.models.rando.evaluators.DrawEvaluator()
                .evaluate(context);

        assertEquals(2, actions.size());
        assertEquals(-200.0f, actions.get(0).getScore(), 0.0f);
        assertEquals(0.0f, actions.get(1).getScore(), 0.0f);
        assertFalse(actions.get(1).getReasoningString().contains("BLOCKED"));
    }
}
