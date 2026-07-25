package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectiveHardLossPolicyTest {

    @Test
    public void scanningCrewAndNonEpicDuelAreTerminalForClassicHunt() {
        assertHardLoss(
                ObjectiveHardLossPolicy.Threat.SCANNING_CREW,
                false,
                "OBJECTIVE.HARD_LOSS.CLASSIC_HUNT_SCANNING_CREW");
        assertHardLoss(
                ObjectiveHardLossPolicy.Threat.NON_EPIC_DUEL,
                false,
                "OBJECTIVE.HARD_LOSS.CLASSIC_HUNT_NON_EPIC_DUEL");
    }

    @Test
    public void inactiveObjectiveAndMaulExceptionRemainNeutral() {
        assertTrue(ObjectiveHardLossPolicy.score(
                new ObjectiveHardLossPolicy.Facts(
                        "action", false,
                        ObjectiveHardLossPolicy.Threat
                                .SCANNING_CREW,
                        false)).operations().isEmpty());
        assertTrue(ObjectiveHardLossPolicy.score(
                new ObjectiveHardLossPolicy.Facts(
                        "action", true,
                        ObjectiveHardLossPolicy.Threat
                                .NON_EPIC_DUEL,
                        true)).operations().isEmpty());
    }

    @Test
    public void unsafeClassicAndVirtualRecallAreTerminal() {
        assertRecallHardLoss(
                ObjectiveHardLossPolicy.RecallKind.CLASSIC,
                "V35-vader-recall-objective-actor",
                TraceDomainId.BATTLE_WEAPONS,
                "V35 VADER RECALL BLOCKED: recalling the sole required"
                        + " battleground Vader would dismantle Hunt Down");
        assertRecallHardLoss(
                ObjectiveHardLossPolicy.RecallKind.VIRTUAL,
                "OBJECTIVE.FLIP_BACK.VIRTUAL_HUNT_VADER_RECALL",
                TraceDomainId.OBJECTIVE_INTENT,
                "VIRTUAL HUNT DOWN RECALL BLOCKED: taking the last Vader"
                        + " into hand would immediately satisfy the"
                        + " flip-back law");
    }

    @Test
    public void objectivePreservingRecallRemainsNeutral() {
        for (ObjectiveHardLossPolicy.RecallKind kind
                : ObjectiveHardLossPolicy.RecallKind.values()) {
            assertTrue(ObjectiveHardLossPolicy.scoreRecall(
                    new ObjectiveHardLossPolicy.RecallFacts(
                            "recall", kind, true))
                    .operations().isEmpty());
        }
    }

    private static void assertHardLoss(
            ObjectiveHardLossPolicy.Threat threat,
            boolean maulException,
            String ruleId) {
        PolicyResult result = ObjectiveHardLossPolicy.score(
                new ObjectiveHardLossPolicy.Facts(
                        "action", true, threat, maulException));
        assertEquals(1, result.operations().size());
        assertEquals(ruleId,
                result.operations().get(0).ruleArmId().id());
        assertEquals(PolicyOperationKind.HARD_VETO,
                result.operations().get(0).kind());
        assertEquals(Float.floatToRawIntBits(0.0f),
                Float.floatToRawIntBits(
                        result.operations().get(0).delta()));
    }

    private static void assertRecallHardLoss(
            ObjectiveHardLossPolicy.RecallKind kind,
            String ruleId,
            TraceDomainId domainId,
            String reason) {
        PolicyResult result = ObjectiveHardLossPolicy.scoreRecall(
                new ObjectiveHardLossPolicy.RecallFacts(
                        "recall", kind, false));
        assertEquals(1, result.operations().size());
        assertEquals(ruleId,
                result.operations().get(0).ruleArmId().id());
        assertEquals(domainId,
                result.operations().get(0).domainId());
        assertEquals(PolicyOperationKind.HARD_VETO,
                result.operations().get(0).kind());
        assertEquals(Float.floatToRawIntBits(0.0f),
                Float.floatToRawIntBits(
                        result.operations().get(0).delta()));
        assertEquals(reason, result.operations().get(0).reason());
    }
}
