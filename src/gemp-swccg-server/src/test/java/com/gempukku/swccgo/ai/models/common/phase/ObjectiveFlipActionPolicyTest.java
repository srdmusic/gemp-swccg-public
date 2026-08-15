package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectiveFlipActionPolicyTest {

    @Test
    public void exactOwnedInPlayFrontFlipGetsOneBandedObjectiveContribution() {
        PolicyResult result = ObjectiveFlipActionPolicy.score(
                "flip",
                new ObjectiveFlipActionPolicy.Facts(
                        "225_53", true, true, false,
                        "  Flip  ", false));

        assertEquals("OBJECTIVE_FLIP_ACTION_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals("flip", operation.actionId());
        assertEquals(ObjectiveFlipActionPolicy.MWYHL_FLIP_RULE_ID,
                operation.ruleArmId().id());
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(Float.floatToRawIntBits(300.0f),
                Float.floatToRawIntBits(operation.delta()));
    }

    @Test
    public void exactSourceStateAndExactCandidateTextAreAllRequired() {
        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_54", true, true, false, "Flip", false));
        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_53", false, true, false, "Flip", false));
        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_53", true, false, false, "Flip", false));
        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_53", true, true, true, "Flip", false));
        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_53", true, true, false, "Flip objective", false));
        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_53", true, true, false, null, false));
    }

    @Test
    public void exactUsefulFrontOnlySetupActionSuppressesFlip() {
        assertEquals(
                ObjectiveFlipActionPolicy.FrontSetupKind.EFFECT,
                ObjectiveFlipActionPolicy
                        .classifyPriorityFrontSetupAction(
                                "Deploy Effect from Reserve Deck"));
        assertEquals(
                ObjectiveFlipActionPolicy.FrontSetupKind
                        .DAGOBAH_LOCATION,
                ObjectiveFlipActionPolicy
                        .classifyPriorityFrontSetupAction(
                                " Deploy Dagobah location from Reserve Deck "));

        assertNoScore(new ObjectiveFlipActionPolicy.Facts(
                "225_53", true, true, false, "Flip", true));
    }

    @Test
    public void bespinPullNeverSuppressesBecauseItSurvivesFlipAndCanRecur() {
        assertEquals(
                ObjectiveFlipActionPolicy.FrontSetupKind.NONE,
                ObjectiveFlipActionPolicy
                        .classifyPriorityFrontSetupAction(
                                "Deploy Bespin location from Reserve Deck"));
    }

    @Test
    public void unrelatedOrInexactActionsDoNotClassifyAsFrontSetup() {
        assertEquals(
                ObjectiveFlipActionPolicy.FrontSetupKind.NONE,
                ObjectiveFlipActionPolicy
                        .classifyPriorityFrontSetupAction(
                                "Choose Deploy Effect from Reserve Deck"));
        assertEquals(
                ObjectiveFlipActionPolicy.FrontSetupKind.NONE,
                ObjectiveFlipActionPolicy
                        .classifyPriorityFrontSetupAction("Flip"));
        assertEquals(
                ObjectiveFlipActionPolicy.FrontSetupKind.NONE,
                ObjectiveFlipActionPolicy
                        .classifyPriorityFrontSetupAction(null));
    }

    private static void assertNoScore(
            ObjectiveFlipActionPolicy.Facts facts) {
        assertTrue(ObjectiveFlipActionPolicy.score("flip", facts)
                .operations().isEmpty());
    }
}
