package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SurpriseAssaultPolicyTest {
    @Test
    public void replay72318NeverRisksSurpriseAssaultAtNalHutta() {
        PolicyOperation operation = onlyOperation(
                facts("replay-72318", SurpriseAssaultFactsReader.LocationKind.SYSTEM,
                        true, 3, 20, 3.25, 18.0f, false));

        assertEquals(PolicyOperationKind.HARD_VETO, operation.kind());
        assertEquals("V300-surprise-assault-space", operation.ruleArmId().id());
    }

    @Test
    public void sectorsAreAlsoSpaceAndNeverUseSurpriseAssault() {
        PolicyOperation operation = onlyOperation(
                facts("sector", SurpriseAssaultFactsReader.LocationKind.SECTOR,
                        true, 8, 20, 6.0, 1.0f, false));

        assertEquals(PolicyOperationKind.HARD_VETO, operation.kind());
        assertEquals("V300-surprise-assault-space", operation.ruleArmId().id());
    }

    @Test
    public void siteRequiresProjectedMarginOfAtLeastTwo() {
        PolicyOperation below = onlyOperation(
                facts("below", SurpriseAssaultFactsReader.LocationKind.SITE,
                        true, 3, 20, 5.0, 13.01f, false));
        PolicyOperation exact = onlyOperation(
                facts("exact", SurpriseAssaultFactsReader.LocationKind.SITE,
                        true, 3, 20, 5.0, 13.0f, false));
        PolicyOperation favorable = onlyOperation(
                facts("five-bodies", SurpriseAssaultFactsReader.LocationKind.SITE,
                        true, 5, 20, 5.0, 13.0f, false));

        assertEquals(PolicyOperationKind.DEFER, below.kind());
        assertEquals("V300-surprise-assault-margin", below.ruleArmId().id());
        assertEquals(PolicyOperationKind.ADD, exact.kind());
        assertEquals(0.0f, exact.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, favorable.kind());
    }

    @Test
    public void projectionCapsDrawsAtCurrentReserveSize() {
        PolicyOperation operation = onlyOperation(
                facts("reserve-cap", SurpriseAssaultFactsReader.LocationKind.SITE,
                        true, 5, 2, 5.0, 9.0f, false));

        assertEquals(PolicyOperationKind.DEFER, operation.kind());
        assertTrue(operation.reason().contains("projected margin 1.00"));
    }

    @Test
    public void incompleteFactsEmptyReserveAndUsableDarkForcesDefer() {
        PolicyOperation unknown = onlyOperation(
                facts("unknown", SurpriseAssaultFactsReader.LocationKind.UNKNOWN,
                        false, 0, 0, 0.0, 0.0f, false));
        PolicyOperation empty = onlyOperation(
                facts("empty", SurpriseAssaultFactsReader.LocationKind.SITE,
                        true, 3, 0, 0.0, 1.0f, false));
        PolicyOperation darkForces = onlyOperation(
                facts("dark-forces", SurpriseAssaultFactsReader.LocationKind.SITE,
                        true, 5, 20, 5.0, 10.0f, true));

        assertEquals(PolicyOperationKind.DEFER, unknown.kind());
        assertEquals("V300-surprise-assault-unknown", unknown.ruleArmId().id());
        assertEquals(PolicyOperationKind.DEFER, empty.kind());
        assertEquals("V300-surprise-assault-margin", empty.ruleArmId().id());
        assertEquals(PolicyOperationKind.DEFER, darkForces.kind());
        assertEquals("V300-surprise-assault-dark-forces",
                darkForces.ruleArmId().id());
    }

    private static SurpriseAssaultFactsReader.Facts facts(
            String actionId,
            SurpriseAssaultFactsReader.LocationKind locationKind,
            boolean complete,
            int opponentPresentCards,
            int reserveCards,
            double averageDestiny,
            float opponentPower,
            boolean opponentCanUseDarkForces) {
        return new SurpriseAssaultFactsReader.Facts(
                actionId, locationKind, complete, opponentPresentCards,
                reserveCards, averageDestiny, opponentPower,
                opponentCanUseDarkForces);
    }

    private static PolicyOperation onlyOperation(
            SurpriseAssaultFactsReader.Facts facts) {
        var operations = ResponsePolicy.scoreSurpriseAssault(facts).operations();
        assertEquals(1, operations.size());
        return operations.get(0);
    }
}
