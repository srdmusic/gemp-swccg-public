package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ControlDrainPolicyAdapterParityTest {
    @Test
    public void mirroredAdaptersApplyTheSameControlStreamOnce() {
        ControlDrainAssessment.Facts facts = new ControlDrainAssessment.Facts() {
            @Override public ControlDrainAssessment.Primary primary() {
                return new ControlDrainAssessment.Primary(3, 0, "site", 10, 0, 2);
            }

            @Override public boolean simpleTricksBlocks() { return false; }

            @Override public ControlDrainAssessment.Economy economy() {
                return new ControlDrainAssessment.Economy(false, 10, true, 2, 3);
            }

            @Override public boolean battleOrderCostWaived() { return false; }

            @Override public ControlDrainAssessment.DrainValue battleOrderDrainValue() {
                return new ControlDrainAssessment.DrainValue(3, "site");
            }

            @Override public ControlDrainAssessment.MultiDrain multiDrain() {
                return new ControlDrainAssessment.MultiDrain(3, 3, "site");
            }

            @Override public ControlDrainAssessment.HuntDown huntDown() {
                return new ControlDrainAssessment.HuntDown(true, 2);
            }
        };
        PolicyContributionLedger ledger = new PolicyContributionLedger("control-decision");
        ledger.register(ControlDrainAssessment.assess("A", facts));

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.FORCE_DRAIN,
                        0.0f, "Force drain");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenOne =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.FORCE_DRAIN,
                        0.0f, "Force drain");

        int randoCount = com.gempukku.swccgo.ai.models.rando.evaluators.PolicyOperationAdapter
                .apply(rando, ledger);
        int chosenCount = com.gempukku.swccgo.ai.models.chosenone.evaluators.PolicyOperationAdapter
                .apply(chosenOne, ledger);

        assertEquals(4, randoCount);
        assertEquals(randoCount, chosenCount);
        assertEquals(Float.floatToRawIntBits(420.0f),
                Float.floatToRawIntBits(rando.getScore()));
        assertEquals(Float.floatToRawIntBits(rando.getScore()),
                Float.floatToRawIntBits(chosenOne.getScore()));
        assertEquals(rando.getReasoning(), chosenOne.getReasoning());
    }
}
