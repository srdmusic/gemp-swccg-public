package com.gempukku.swccgo.ai.models.common.strategy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DeploymentPlanPhysicalIdentityTest {

    @Test
    public void randoPlanKeepsDuplicateBlueprintCopiesIndependent() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                "duplicate identity");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction first =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                "1_1", "First copy", "10", "Site", 1, "first");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction second =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                "1_1", "Second copy", "11", "Site", 2, "second");
        first.setCardPermanentCardId(101);
        first.setCardCurrentCardId(201);
        second.setCardPermanentCardId(102);
        second.setCardCurrentCardId(202);
        plan.addInstruction(first);
        plan.addInstruction(second);

        assertSame(first, plan.getInstructionForPhysicalCard(101, 201, "1_1"));
        assertSame(second, plan.getInstructionForPhysicalCard(102, 202, "1_1"));
        assertNull(plan.getInstructionForCard("1_1"));
        assertNull(plan.getInstructionForPhysicalCard(999, 999, "1_1"));

        plan.recordDeployment(999, 999, "1_1");
        assertEquals(2, plan.getInstructions().size());
        assertEquals(0, plan.getDeploymentsMade());

        plan.recordDeployment(101, 201, "1_1");

        assertEquals(1, plan.getInstructions().size());
        assertSame(second, plan.getInstructionForPhysicalCard(102, 202, "1_1"));
        assertEquals(1, plan.getDeploymentsMade());
    }

    @Test
    public void chosenOnePlanKeepsDuplicateBlueprintCopiesIndependent() {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.REINFORCE,
                "duplicate identity");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction first =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                "1_1", "First copy", "10", "Site", 1, "first");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction second =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                "1_1", "Second copy", "11", "Site", 2, "second");
        first.setCardPermanentCardId(101);
        first.setCardCurrentCardId(201);
        second.setCardPermanentCardId(102);
        second.setCardCurrentCardId(202);
        plan.addInstruction(first);
        plan.addInstruction(second);

        assertSame(first, plan.getInstructionForPhysicalCard(101, 201, "1_1"));
        assertSame(second, plan.getInstructionForPhysicalCard(102, 202, "1_1"));
        assertNull(plan.getInstructionForCard("1_1"));
        assertNull(plan.getInstructionForPhysicalCard(999, 999, "1_1"));

        plan.recordDeployment(999, 999, "1_1");
        assertEquals(2, plan.getInstructions().size());
        assertEquals(0, plan.getDeploymentsMade());

        plan.recordDeployment(101, 201, "1_1");

        assertEquals(1, plan.getInstructions().size());
        assertSame(second, plan.getInstructionForPhysicalCard(102, 202, "1_1"));
        assertEquals(1, plan.getDeploymentsMade());
    }

    @Test
    public void legacyInstructionRetainsBlueprintFallback() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                "legacy fixture");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction legacy =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                "2_2", "Legacy copy", null, null, 1, "legacy");
        plan.addInstruction(legacy);

        assertSame(legacy, plan.getInstructionForPhysicalCard(77, 88, "2_2"));
    }
}
