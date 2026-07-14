package com.gempukku.swccgo.ai.models.common.evaluators;

import com.gempukku.swccgo.ai.models.common.strategy.ForcedDestinationDeploySafety;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractForcedDestinationDeploySafetyMatrixTest {
    private static final FormationSafety.CharacterDeployCheck ALLOWED =
            new FormationSafety.CharacterDeployCheck(
                    FormationSafety.CharacterDeployState.ALLOWED, "allowed fixture");
    private static final FormationSafety.CharacterDeployCheck BLOCKED =
            new FormationSafety.CharacterDeployCheck(
                    FormationSafety.CharacterDeployState.VETOED, "hard block fixture");

    protected abstract ForcedDestinationDeploySafety.Assessment assess(
            boolean identityResolved,
            ForcedDestinationDeploySafety.ObjectiveState objectiveState,
            FormationSafety.CharacterDeployCheck formation,
            boolean weakSoloNoPlan);

    @Test
    public void unflippedFirstPullIsTheFlipPlan() {
        assertAssessment(assess(
                true,
                ForcedDestinationDeploySafety.ObjectiveState.UNFLIPPED_TARGET_NAMED,
                null, true),
                ForcedDestinationDeploySafety.Verdict.FLIP_PLAN_EXEMPT, 0f);
    }

    @Test
    public void postFlipUnsupportedRepeatKeepsExactNoPlanPenalty() {
        ForcedDestinationDeploySafety.Assessment assessment = assess(
                true, ForcedDestinationDeploySafety.ObjectiveState.FLIPPED,
                ALLOWED, true);
        assertAssessment(assessment,
                ForcedDestinationDeploySafety.Verdict.WEAK_SOLO_NO_PLAN, -800f);
        assertTrue(assessment.reason().contains("post-flip"));
    }

    @Test
    public void unresolvedPullIdentityRemainsUnknown() {
        assertAssessment(assess(
                false,
                ForcedDestinationDeploySafety.ObjectiveState.NOT_APPLICABLE,
                ALLOWED, false),
                ForcedDestinationDeploySafety.Verdict.UNKNOWN, 0f);
    }

    @Test
    public void weakSoloWithoutPlanKeepsExactPenalty() {
        assertAssessment(assess(
                true,
                ForcedDestinationDeploySafety.ObjectiveState.NOT_APPLICABLE,
                ALLOWED, true),
                ForcedDestinationDeploySafety.Verdict.WEAK_SOLO_NO_PLAN, -800f);
    }

    @Test
    public void trueFormationBlockRemainsHard() {
        ForcedDestinationDeploySafety.Assessment assessment = assess(
                true,
                ForcedDestinationDeploySafety.ObjectiveState.NOT_APPLICABLE,
                BLOCKED, true);
        assertAssessment(assessment,
                ForcedDestinationDeploySafety.Verdict.HARD_BLOCK, 0f);
        assertEquals("hard block fixture", assessment.reason());
    }

    private static void assertAssessment(
            ForcedDestinationDeploySafety.Assessment assessment,
            ForcedDestinationDeploySafety.Verdict verdict,
            float score) {
        assertEquals(verdict, assessment.verdict());
        assertEquals(Float.floatToRawIntBits(score),
                Float.floatToRawIntBits(assessment.scoreDelta()));
    }
}
