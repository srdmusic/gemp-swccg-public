package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DeployPlannerCharacterizationTest {
    @Test
    public void randoPlanUsesExactPhysicalIdentityAndUniqueLegacyFallback() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.ESTABLISH,
                        "test");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction first =
                randoInstruction("1_1", 10, 20);
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction second =
                randoInstruction("1_1", 11, 21);
        plan.addInstruction(first);
        plan.addInstruction(second);

        assertSame(first, plan.getInstructionForPhysicalCard(10, 20, "1_1"));
        assertSame(second, plan.getInstructionForPhysicalCard(11, 21, "1_1"));
        assertNull(plan.getInstructionForPhysicalCard(99, 99, "1_1"));
    }

    @Test
    public void chosenPlanUsesExactPhysicalIdentityAndUniqueLegacyFallback() {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.ESTABLISH,
                        "test");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction first =
                chosenInstruction("1_1", 10, 20);
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction second =
                chosenInstruction("1_1", 11, 21);
        plan.addInstruction(first);
        plan.addInstruction(second);

        assertSame(first, plan.getInstructionForPhysicalCard(10, 20, "1_1"));
        assertSame(second, plan.getInstructionForPhysicalCard(11, 21, "1_1"));
        assertNull(plan.getInstructionForPhysicalCard(99, 99, "1_1"));
    }

    @Test
    public void assessmentCopyDoesNotLeakMutations() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                        "test");
        plan.addInstruction(randoInstruction("1_1", 10, 20));
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan copy =
                plan.assessmentCopy();
        copy.getInstructions().get(0).setDeployCost(9);
        copy.getInstructions().clear();

        assertEquals(1, plan.getInstructions().size());
        assertEquals(3, plan.getInstructions().get(0).getDeployCost());
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction
    randoInstruction(String blueprint, int permanentId, int currentId) {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction instruction =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                        blueprint, "Card", "loc", "Site", 1, "test");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(3);
        return instruction;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction
    chosenInstruction(String blueprint, int permanentId, int currentId) {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction instruction =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                        blueprint, "Card", "loc", "Site", 1, "test");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(3);
        return instruction;
    }
}
