package com.gempukku.swccgo.ai.models.common.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DeploymentPlanAssessmentCopyPurityTest {

    @Test
    public void randoAssessmentCopyDeepCopiesInstructions() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE, "base plan");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction original =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                "bp-1", "Stormtrooper", "loc-1", "Docking Bay", 1, "reinforce");
        plan.addInstruction(original);

        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan copy = plan.assessmentCopy();
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction copied =
            copy.getInstructions().get(0);

        assertNotSame(plan.getInstructions(), copy.getInstructions());
        assertNotSame(original, copied);

        copied.setCardPermanentCardId(999);
        copied.setCardCurrentCardId(888);
        copied.setCardName("MUTATED");
        copied.setTargetLocationId("MUTATED-LOC");

        assertNull(original.getCardPermanentCardId());
        assertNull(original.getCardCurrentCardId());
        assertEquals("Stormtrooper", original.getCardName());
        assertEquals("loc-1", original.getTargetLocationId());
        assertEquals("bp-1", copied.getCardBlueprintId());
        assertEquals(1, copied.getPriority());
    }

    @Test
    public void chosenOneAssessmentCopyDeepCopiesInstructions() {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.REINFORCE, "base plan");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction original =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                "bp-1", "Stormtrooper", "loc-1", "Docking Bay", 1, "reinforce");
        plan.addInstruction(original);

        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan copy = plan.assessmentCopy();
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction copied =
            copy.getInstructions().get(0);

        assertNotSame(plan.getInstructions(), copy.getInstructions());
        assertNotSame(original, copied);

        copied.setCardPermanentCardId(999);
        copied.setCardCurrentCardId(888);
        copied.setCardName("MUTATED");
        copied.setTargetLocationId("MUTATED-LOC");

        assertNull(original.getCardPermanentCardId());
        assertNull(original.getCardCurrentCardId());
        assertEquals("Stormtrooper", original.getCardName());
        assertEquals("loc-1", original.getTargetLocationId());
        assertEquals("bp-1", copied.getCardBlueprintId());
        assertEquals(1, copied.getPriority());
    }
}
