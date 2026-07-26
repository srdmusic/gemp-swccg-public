package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CaptureObjectivePullPolicyTest {

    @Test
    public void objectiveRouteBypassCoversPrintedCostAffordabilityGuard() {
        PullActionPolicy.Evaluation ordinary =
                PullActionPolicy.evaluateParent(
                        parent(false));
        PullActionPolicy.Evaluation objectiveRoute =
                PullActionPolicy.evaluateParent(
                        parent(true));

        assertTrue(hasRule(ordinary, "V67ac"));
        assertFalse(hasRule(objectiveRoute, "V67ac"));
    }

    private static PullActionFacts.Parent parent(
            boolean objectiveRouteBypass) {
        PullOracleView.Validation unknown =
                new PullOracleView.Validation(
                        PullOracleView.Outcome.UNKNOWN, "");
        return new PullActionFacts.Parent(
                "emperor-download",
                "Deploy Emperor from Reserve Deck",
                10,
                false,
                "",
                unknown,
                unknown,
                "Bring Him Before Me",
                false,
                8,
                6,
                false,
                "[]",
                10,
                false,
                "",
                CardCategory.OBJECTIVE,
                PullActionFacts.V131State.CLOSED,
                "",
                false,
                "",
                false,
                0,
                0,
                0,
                false,
                "",
                0,
                0,
                "",
                false,
                Phase.DEPLOY,
                false,
                false,
                false,
                PullActionFacts.FormationState.NONE,
                "",
                false,
                false,
                objectiveRouteBypass);
    }

    private static boolean hasRule(
            PullActionPolicy.Evaluation evaluation,
            String ruleId) {
        for (PolicyOperation operation
                : evaluation.result().operations()) {
            if (ruleId.equals(operation.ruleArmId().id())) {
                return true;
            }
        }
        return false;
    }
}
