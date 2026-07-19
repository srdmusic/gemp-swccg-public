package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployWeaponPolicyTest {
    @Test
    public void criteriaGateDominatesOtherDirectWeaponArms() {
        List<PolicyOperation> operations = DeployWeaponPolicy.evaluateDirectEligibility(
                direct("Dooku", true, 4, 2, 0, 0, 2)).operations();

        assertOne(operations, "V158", -9999.0f, TraceOutputKind.VETO);
        assertTrue(operations.get(0).reason().contains("no 'Dooku' friendly"));
    }

    @Test
    public void directWeaponGatePreservesLightsaberAndAllArmedBoundaries() {
        assertOne(DeployWeaponPolicy.evaluateDirectEligibility(
                        direct("", true, 2, 1, 0, 0, 0)).operations(),
                "V158", -9999.0f, TraceOutputKind.VETO);
        assertOne(DeployWeaponPolicy.evaluateDirectEligibility(
                        direct("", false, 2, 0, 0, 0, 0)).operations(),
                "V158", -9999.0f, TraceOutputKind.VETO);
        assertOne(DeployWeaponPolicy.evaluateDirectEligibility(
                        direct("", false, 1, 2, 0, 0, 0)).operations(),
                "V158", 300.0f, TraceOutputKind.BANDED);
        assertTrue(DeployWeaponPolicy.evaluateDirectEligibility(
                direct("", false, 0, 0, 0, 0, 0)).operations().isEmpty());
    }

    @Test
    public void namedWeaponPriorityPreservesNamedAndGenericBranches() {
        assertOne(DeployWeaponPolicy.evaluateNamedPriority(
                        new DeployWeaponPolicy.NamedPriorityFacts(
                                "a", true, "", "")).operations(),
                "V33-named-weapon", 200.0f, TraceOutputKind.BANDED);
        assertOne(DeployWeaponPolicy.evaluateNamedPriority(
                        new DeployWeaponPolicy.NamedPriorityFacts(
                                "a", false, "darth vader", "Vader's Lightsaber")).operations(),
                "V33-named-weapon-wait", -400.0f, TraceOutputKind.BANDED);
        assertTrue(DeployWeaponPolicy.evaluateNamedPriority(
                new DeployWeaponPolicy.NamedPriorityFacts(
                        "a", false, "darth vader", "")).operations().isEmpty());
    }

    @Test
    public void reserveWeaponGuardsPreserveAdditiveHardBlocks() {
        assertOne(DeployWeaponPolicy.evaluateReserveTarget(
                        new DeployWeaponPolicy.ReserveTargetFacts(
                                "a", "Lord Vader", true)).operations(),
                "V158-reserve-target", -9999.0f, TraceOutputKind.VETO);
        assertOne(DeployWeaponPolicy.evaluateReserveWielder(
                        new DeployWeaponPolicy.ReserveWielderFacts(
                                "a", "vader", false)).operations(),
                "V158-no-wielder", -9999.0f, TraceOutputKind.VETO);
        assertTrue(DeployWeaponPolicy.evaluateReserveTarget(
                new DeployWeaponPolicy.ReserveTargetFacts(
                        "a", "Lord Vader", false)).operations().isEmpty());
        assertTrue(DeployWeaponPolicy.evaluateReserveWielder(
                new DeployWeaponPolicy.ReserveWielderFacts(
                        "a", "vader", true)).operations().isEmpty());
    }

    @Test
    public void v120OnlyBlocksKnownCriteriaWithoutUnarmedMatch() {
        assertOne(DeployWeaponPolicy.evaluatePullCriteria(
                        new DeployWeaponPolicy.PullCriteriaFacts(
                                "a", "Vader's Lightsaber", "Vader", 1, 0)).operations(),
                "V120", -9999.0f, TraceOutputKind.VETO);
        assertTrue(DeployWeaponPolicy.evaluatePullCriteria(
                new DeployWeaponPolicy.PullCriteriaFacts(
                        "a", "Vader's Lightsaber", "Vader", 1, 1)).operations().isEmpty());
        assertTrue(DeployWeaponPolicy.evaluatePullCriteria(
                new DeployWeaponPolicy.PullCriteriaFacts(
                        "a", "Vader's Lightsaber", "", 0, 0)).operations().isEmpty());
    }

    private static DeployWeaponPolicy.DirectEligibilityFacts direct(
            String criteria, boolean lightsaber, int armed, int unarmed,
            int matchingArmed, int matchingUnarmed, int warriorFour) {
        return new DeployWeaponPolicy.DirectEligibilityFacts(
                "a", criteria, lightsaber, armed, unarmed,
                matchingArmed, matchingUnarmed, warriorFour);
    }

    private static void assertOne(List<PolicyOperation> operations,
                                  String rule, float delta,
                                  TraceOutputKind outputKind) {
        assertEquals(1, operations.size());
        assertEquals(rule, operations.get(0).ruleArmId().id());
        assertEquals(delta, operations.get(0).delta(), 0.0f);
        assertEquals(outputKind, operations.get(0).outputKind());
        assertEquals(TraceDomainId.DEPLOY_ATTACH, operations.get(0).domainId());
    }
}
