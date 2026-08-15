package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TwinSunsObjectivePolicyTest {

    @Test
    public void exactObjectiveActionsOwnTheirPriorityBands() {
        var site = TwinSunsObjectivePolicy.scoreFrontSiteRoute(
                "site", true, false);
        assertEquals(300.0f,
                site.operations().get(0).delta(), 0.0f);
        assertEquals("OBJECTIVE.TWIN_SUNS.SITE_ROUTE",
                site.operations().get(0).ruleArmId().id());

        var occupation = TwinSunsObjectivePolicy.scoreOccupationRoute(
                "occupation", true);
        assertEquals(300.0f,
                occupation.operations().get(0).delta(), 0.0f);

        var peek = TwinSunsObjectivePolicy.scorePeek(
                "peek", true);
        assertEquals(300.0f,
                peek.operations().get(0).delta(), 0.0f);
    }

    @Test
    public void exhaustedSiteRouteIsHardVetoed() {
        var exhausted = TwinSunsObjectivePolicy.scoreFrontSiteRoute(
                "site", false, true);
        assertEquals(1, exhausted.operations().size());
        assertEquals(PolicyOperationKind.HARD_VETO,
                exhausted.operations().get(0).kind());
        assertEquals("OBJECTIVE.TWIN_SUNS.SITE_ROUTE_EXHAUSTED",
                exhausted.operations().get(0).ruleArmId().id());

        assertTrue(TwinSunsObjectivePolicy.scoreFrontSiteRoute(
                "site", false, false).operations().isEmpty());
    }

    @Test
    public void ordinaryDeployGetsBoundedTwinSunsRoutePenalty() {
        var penalized = TwinSunsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", true, 2, 2, 1, 1);
        assertEquals(1, penalized.operations().size());
        assertEquals(PolicyOperationKind.ADD,
                penalized.operations().get(0).kind());
        assertEquals(-300.0f,
                penalized.operations().get(0).delta(), 0.0f);
        assertEquals("OBJECTIVE.TWIN_SUNS.ROUTE_FORCE_RESERVE",
                penalized.operations().get(0).ruleArmId().id());

        assertTrue(TwinSunsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", true, 2, 3, 1, 1)
                .operations().isEmpty());
        assertTrue(TwinSunsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", true, 2, 2, 0, 0)
                .operations().isEmpty());
        assertTrue(TwinSunsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", false, 2, 2, 1, 1)
                .operations().isEmpty());
    }
}
