package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveSitingPolicyTest {
    @Test
    public void emitsObjectiveSenatorTextAndGuardScoresInLegacyOrder() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, true, true, 150.0f,
                true, true, true, true,
                true, false, false, "galactic senate",
                0.0f, 0.0f));

        assertEquals(3, operations.size());
        assertOperation(operations.get(0), "V22-objective-location", 150.0f);
        assertOperation(operations.get(1), "V88-CS", 1500.0f);
        assertOperation(operations.get(2), "V88-text-named", 500.0f);
    }

    @Test
    public void wrongSiteSenatorKeepsDominantPenalty() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, true, false, 0.0f,
                true, true, true, false,
                false, false, false, "landing platform",
                0.0f, 0.0f));

        assertEquals(1, operations.size());
        assertOperation(operations.get(0), "V88-CS", -2000.0f);
    }

    @Test
    public void undercoverSpyDoesNotReceiveObjectivePresenceScore() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", true, true, true, 500.0f,
                false, false, true, false,
                false, false, false, "audience chamber",
                0.0f, 0.0f));
        assertTrue(operations.isEmpty());
    }

    @Test
    public void negativeTextAlwaysPenalizesButDoomedPositiveIsWithheld() {
        List<PolicyOperation> negative = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, false,
                true, true, true, "audience chamber",
                0.0f, 0.0f));
        assertEquals(1, negative.size());
        assertOperation(negative.get(0), "V88-text-named", -500.0f);

        List<PolicyOperation> doomed = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, false,
                true, false, true, "audience chamber",
                0.0f, 0.0f));
        assertTrue(doomed.isEmpty());
    }

    @Test
    public void senateGuardBlocksOnlyWhenDefenseIsNotNeeded() {
        List<PolicyOperation> blocked = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, true,
                false, false, false, "galactic senate",
                4.0f, 4.0f));
        assertEquals(1, blocked.size());
        assertOperation(blocked.get(0), "V99-CS", -1500.0f);

        List<PolicyOperation> defense = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, true,
                false, false, false, "galactic senate",
                5.0f, 4.0f));
        assertTrue(defense.isEmpty());
    }

    private static List<PolicyOperation> evaluate(DeployObjectiveSitingPolicy.Facts facts) {
        return DeployObjectiveSitingPolicy.evaluate(facts).operations();
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId, float delta) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(delta, operation.delta(), 0.0f);
    }
}
