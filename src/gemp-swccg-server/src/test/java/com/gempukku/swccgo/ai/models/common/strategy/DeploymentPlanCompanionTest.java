package com.gempukku.swccgo.ai.models.common.strategy;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeploymentPlanCompanionTest {

    @Test
    public void randoPlanRequiresExactAvailableCompanionAtSameDestination() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.ESTABLISH,
                "formation plan");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction first =
            randoInstruction("1_1", 101, 201, "10", 2);
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction buddy =
            randoInstruction("1_2", 102, 202, "10", 3);
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction wrongDestination =
            randoInstruction("1_3", 103, 203, "11", 1);
        plan.addInstruction(first);
        plan.addInstruction(buddy);
        plan.addInstruction(wrongDestination);

        Set<Integer> charactersInHand = new HashSet<>(Arrays.asList(101, 102, 103, 999));
        assertEquals(Float.valueOf(3f),
            plan.getCheapestPlannedCharacterBuddyCost(first, "10", charactersInHand));
        assertNull(plan.getCheapestPlannedCharacterBuddyCost(first, "11", charactersInHand));
        assertNull(plan.getCheapestPlannedCharacterBuddyCost(first, "10",
            new HashSet<>(Arrays.asList(101, 103, 999))));
    }

    @Test
    public void chosenOnePlanRequiresExactAvailableCompanionAtSameDestination() {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.ESTABLISH,
                "formation plan");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction first =
            chosenInstruction("1_1", 101, 201, "10", 2);
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction buddy =
            chosenInstruction("1_2", 102, 202, "10", 3);
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction wrongDestination =
            chosenInstruction("1_3", 103, 203, "11", 1);
        plan.addInstruction(first);
        plan.addInstruction(buddy);
        plan.addInstruction(wrongDestination);

        Set<Integer> charactersInHand = new HashSet<>(Arrays.asList(101, 102, 103, 999));
        assertEquals(Float.valueOf(3f),
            plan.getCheapestPlannedCharacterBuddyCost(first, "10", charactersInHand));
        assertNull(plan.getCheapestPlannedCharacterBuddyCost(first, "11", charactersInHand));
        assertNull(plan.getCheapestPlannedCharacterBuddyCost(first, "10",
            new HashSet<>(Arrays.asList(101, 103, 999))));
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction randoInstruction(
            String blueprint, int permanentId, int currentId, String target, int cost) {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction instruction =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                blueprint, blueprint, target, "Site", 1, "plan");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(cost);
        return instruction;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction chosenInstruction(
            String blueprint, int permanentId, int currentId, String target, int cost) {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction instruction =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                blueprint, blueprint, target, "Site", 1, "plan");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(cost);
        return instruction;
    }
}
