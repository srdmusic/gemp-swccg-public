package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.CardCategory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PullSelectionCandidatePolicyTest {
    private static final String ACTION_ID = "candidate-7";

    @Test
    public void iwtmOnlyScoresTheStarkillerSystem() {
        assertEmpty(PullSelectionCandidatePolicy.scoreIwtmLocation(
                new PullSelectionCandidateFacts.IwtmLocation(
                        ACTION_ID, false)));
        PolicyOperation operation = only(
                PullSelectionCandidatePolicy.scoreIwtmLocation(
                        new PullSelectionCandidateFacts.IwtmLocation(
                                ACTION_ID, true)));
        assertOperation(operation, "V186-iwtm-system",
                TraceDomainId.SETUP_STARTING, TraceOutputKind.ORDERING, 400.0f,
                "V186 STARKILLER BASE SYSTEM - download engine for the 2-battleground flip");
    }

    @Test
    public void unknownGainStackPreservesLegacyOrder() {
        PolicyResult result = PullSelectionCandidatePolicy.scoreUnknownPull(
                new PullSelectionCandidateFacts.UnknownPull(
                        ACTION_ID, "Cloud City: Carbonite Chamber",
                        CardCategory.LOCATION, true, true, false,
                        PullSelectionCandidateFacts.CloudCityMode.IM_SORRY,
                        PullSelectionCandidateFacts.CloudCitySite.CARBONITE_CHAMBER,
                        100,
                        PullSelectionCandidateFacts.UnknownAmsdState.NON_PIETT));

        assertIds(result, "pull-unknown-location", "V25-hunt-down-lightsaber",
                "V24.13-im-sorry-carbonite", "pull-unknown-priority",
                "V24.10-amsd-safety-block");
        assertDomains(result, TraceDomainId.PULL_SEARCH,
                TraceDomainId.DECK_PLAYBOOK, TraceDomainId.DECK_PLAYBOOK,
                TraceDomainId.PULL_SEARCH, TraceDomainId.DECK_PLAYBOOK);
        assertKinds(result, TraceOutputKind.ORDERING, TraceOutputKind.BANDED,
                TraceOutputKind.BANDED, TraceOutputKind.BANDED,
                TraceOutputKind.VETO);
        assertDeltas(result, 10.0f, 200.0f, 150.0f, 30.000002f, -9999.0f);
    }

    @Test
    public void unknownLossSkipsOnlyGainSpecificValue() {
        PolicyResult result = PullSelectionCandidatePolicy.scoreUnknownPull(
                new PullSelectionCandidateFacts.UnknownPull(
                        ACTION_ID, "Cloud City: Dining Room",
                        CardCategory.LOCATION, false, false, false,
                        PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING,
                        PullSelectionCandidateFacts.CloudCitySite.DINING_ROOM,
                        null, PullSelectionCandidateFacts.UnknownAmsdState.NONE));

        assertIds(result, "V24.10-slip-dining");
        assertDomains(result, TraceDomainId.DECK_PLAYBOOK);
        assertKinds(result, TraceOutputKind.BANDED);
        assertDeltas(result, 300.0f);
    }

    @Test
    public void unknownLossPriorityUsesForceLossDomain() {
        PolicyResult result = PullSelectionCandidatePolicy.scoreUnknownPull(
                new PullSelectionCandidateFacts.UnknownPull(
                        ACTION_ID, "Protected card", CardCategory.EFFECT,
                        false, false, false,
                        PullSelectionCandidateFacts.CloudCityMode.NONE,
                        PullSelectionCandidateFacts.CloudCitySite.OTHER,
                        100, PullSelectionCandidateFacts.UnknownAmsdState.NONE));

        assertIds(result, "pull-unknown-priority");
        assertDomains(result, TraceDomainId.FORCE_LOSS_PAYMENT);
        assertKinds(result, TraceOutputKind.BANDED);
        assertDeltas(result, 30.000002f);
    }

    @Test
    public void exactOpenObjectiveGateDominatesGenericLocationTie() {
        PolicyResult result = PullSelectionCandidatePolicy.scoreUnknownPull(
                new PullSelectionCandidateFacts.UnknownPull(
                        ACTION_ID, "Naboo: Theed Palace Throne Room",
                        CardCategory.LOCATION, true, false, true,
                        PullSelectionCandidateFacts.CloudCityMode.NONE,
                        PullSelectionCandidateFacts.CloudCitySite.OTHER,
                        null, PullSelectionCandidateFacts.UnknownAmsdState.NONE));

        assertIds(result, "pull-unknown-location",
                "PULL.OBJECTIVE.FLIP_GATE_SITE");
        assertDomains(result, TraceDomainId.PULL_SEARCH,
                TraceDomainId.DECK_PLAYBOOK);
        assertKinds(result, TraceOutputKind.ORDERING,
                TraceOutputKind.BANDED);
        assertDeltas(result, 10.0f, 300.0f);
    }

    @Test
    public void blueprintObjectiveSiteOrderPreservesPlanStacking() {
        PolicyResult result = PullSelectionCandidatePolicy.scoreBlueprintPull(
                new PullSelectionCandidateFacts.BlueprintPull(
                        ACTION_ID,
                        PullSelectionCandidateFacts.CloudCityMode.OBJECTIVE,
                        PullSelectionCandidateFacts.CloudCitySite.UPPER_WALKWAY,
                        PullSelectionCandidateFacts.PlanState.IN_PLAN,
                        "Deploy locations first"));

        assertIds(result, "V26-objective-exterior", "pull-plan-match");
        assertDomains(result, TraceDomainId.DECK_PLAYBOOK,
                TraceDomainId.DEPLOY_SEQUENCING);
        assertKinds(result, TraceOutputKind.ORDERING,
                TraceOutputKind.ORDERING);
        assertDeltas(result, 500.0f, 100.0f);
        assertEquals("IN DEPLOYMENT PLAN: Deploy locations first",
                result.operations().get(1).reason());
    }

    @Test
    public void blueprintCloudCityModesPreserveEverySiteBand() {
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.OBJECTIVE,
                PullSelectionCandidateFacts.CloudCitySite.DINING_ROOM,
                "V26-objective-dining", TraceOutputKind.ORDERING, -400.0f);
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.OBJECTIVE,
                PullSelectionCandidateFacts.CloudCitySite.OTHER,
                "V26-objective-interior", TraceOutputKind.ORDERING, -200.0f);
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.IM_SORRY,
                PullSelectionCandidateFacts.CloudCitySite.DINING_ROOM,
                "V24.10-im-sorry-dining", TraceOutputKind.BANDED, -50.0f);
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.IM_SORRY,
                PullSelectionCandidateFacts.CloudCitySite.SECURITY_TOWER,
                "V24.13-im-sorry-security", TraceOutputKind.BANDED, -30.0f);
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.IM_SORRY,
                PullSelectionCandidateFacts.CloudCitySite.CARBONITE_CHAMBER,
                "V24.13-im-sorry-carbonite", TraceOutputKind.BANDED, 150.0f);
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.IM_SORRY,
                PullSelectionCandidateFacts.CloudCitySite.OTHER,
                "V24.10-im-sorry-other", TraceOutputKind.BANDED, 100.0f);
        assertSingleBlueprint(PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING,
                PullSelectionCandidateFacts.CloudCitySite.OTHER,
                "V24.10-slip-other", TraceOutputKind.BANDED, -50.0f);
    }

    @Test
    public void holdBackAndNoPlanRemainExclusive() {
        PolicyResult hold = PullSelectionCandidatePolicy.scoreBlueprintPull(
                new PullSelectionCandidateFacts.BlueprintPull(
                        ACTION_ID, PullSelectionCandidateFacts.CloudCityMode.NONE,
                        PullSelectionCandidateFacts.CloudCitySite.OTHER,
                        PullSelectionCandidateFacts.PlanState.HOLD_BACK, ""));
        assertIds(hold, "pull-plan-hold");
        assertDomains(hold, TraceDomainId.DEPLOY_SEQUENCING);
        assertKinds(hold, TraceOutputKind.ORDERING);
        assertDeltas(hold, -50.0f);

        assertEmpty(PullSelectionCandidatePolicy.scoreBlueprintPull(
                new PullSelectionCandidateFacts.BlueprintPull(
                        ACTION_ID, PullSelectionCandidateFacts.CloudCityMode.NONE,
                        PullSelectionCandidateFacts.CloudCitySite.OTHER,
                        PullSelectionCandidateFacts.PlanState.NONE, "")));
    }

    @Test
    public void amsdPilotBlocksRetainSetThenAddContract() {
        assertAmsdBlock(PullSelectionCandidateFacts.PilotAmsdState.NON_PIETT,
                "V24.10-amsd-pilot-block");
        assertAmsdBlock(
                PullSelectionCandidateFacts.PilotAmsdState.PIETT_EXECUTOR_MISSING,
                "V24.10-amsd-executor-missing");
    }

    @Test
    public void amsdPilotPositiveAndNeutralRoutesFallThrough() {
        PullSelectionCandidatePolicy.Evaluation present = evaluateAmsd(
                PullSelectionCandidateFacts.PilotAmsdState.PIETT_EXECUTOR_PRESENT);
        assertEquals(PullSelectionCandidatePolicy.AdapterStep.FALL_THROUGH,
                present.adapterStep());
        assertFalse(present.resetToAmsdBlockScore());
        assertIds(present.result(), "V24.10-amsd-approved");
        assertDomains(present.result(), TraceDomainId.DECK_PLAYBOOK);
        assertKinds(present.result(), TraceOutputKind.VETO);
        assertDeltas(present.result(), 300.0f);

        PullSelectionCandidatePolicy.Evaluation unavailable = evaluateAmsd(
                PullSelectionCandidateFacts.PilotAmsdState.PIETT_ORACLE_UNAVAILABLE);
        assertEquals(PullSelectionCandidatePolicy.AdapterStep.FALL_THROUGH,
                unavailable.adapterStep());
        assertIds(unavailable.result(), "V24.10-amsd-oracle-unavailable");
        assertDomains(unavailable.result(), TraceDomainId.DECK_PLAYBOOK);
        assertKinds(unavailable.result(), TraceOutputKind.VETO);
        assertDeltas(unavailable.result(), 200.0f);

        PullSelectionCandidatePolicy.Evaluation unrelated = evaluateAmsd(
                PullSelectionCandidateFacts.PilotAmsdState.NOT_AMSD);
        assertEquals(PullSelectionCandidatePolicy.AdapterStep.FALL_THROUGH,
                unrelated.adapterStep());
        assertFalse(unrelated.resetToAmsdBlockScore());
        assertEmpty(unrelated.result());
    }

    private static void assertAmsdBlock(
            PullSelectionCandidateFacts.PilotAmsdState state, String ruleId) {
        PullSelectionCandidatePolicy.Evaluation evaluation = evaluateAmsd(state);
        assertEquals(PullSelectionCandidatePolicy.AdapterStep.CONTINUE_CANDIDATE,
                evaluation.adapterStep());
        assertTrue(evaluation.resetToAmsdBlockScore());
        assertIds(evaluation.result(), ruleId);
        assertDomains(evaluation.result(), TraceDomainId.DECK_PLAYBOOK);
        assertKinds(evaluation.result(), TraceOutputKind.VETO);
        assertDeltas(evaluation.result(), -9999.0f);
        assertEquals(-9999.0f,
                PullSelectionCandidatePolicy.AMSD_BLOCK_SCORE, 0.0f);
    }

    private static PullSelectionCandidatePolicy.Evaluation evaluateAmsd(
            PullSelectionCandidateFacts.PilotAmsdState state) {
        return PullSelectionCandidatePolicy.evaluateAmsdPilot(
                new PullSelectionCandidateFacts.AmsdPilot(
                        ACTION_ID, "Admiral Piett", state));
    }

    private static void assertSingleBlueprint(
            PullSelectionCandidateFacts.CloudCityMode mode,
            PullSelectionCandidateFacts.CloudCitySite site,
            String ruleId, TraceOutputKind outputKind, float delta) {
        PolicyResult result = PullSelectionCandidatePolicy.scoreBlueprintPull(
                new PullSelectionCandidateFacts.BlueprintPull(
                        ACTION_ID, mode, site,
                        PullSelectionCandidateFacts.PlanState.NONE, ""));
        assertIds(result, ruleId);
        assertDomains(result, TraceDomainId.DECK_PLAYBOOK);
        assertKinds(result, outputKind);
        assertDeltas(result, delta);
    }

    private static PolicyOperation only(PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static void assertEmpty(PolicyResult result) {
        assertEquals("PULL_SELECTION_CANDIDATE_POLICY", result.producerId());
        assertTrue(result.operations().isEmpty());
    }

    private static void assertIds(PolicyResult result, String... ids) {
        assertEquals("PULL_SELECTION_CANDIDATE_POLICY", result.producerId());
        assertEquals(List.of(ids), result.operations().stream()
                .map(operation -> operation.ruleArmId().id()).toList());
        for (PolicyOperation operation : result.operations()) {
            assertEquals(ACTION_ID, operation.actionId());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
        }
    }

    private static void assertDomains(PolicyResult result,
                                      TraceDomainId... domains) {
        assertEquals(domains.length, result.operations().size());
        assertEquals(List.of(domains), result.operations().stream()
                .map(PolicyOperation::domainId).toList());
    }

    private static void assertKinds(PolicyResult result,
                                    TraceOutputKind... kinds) {
        assertEquals(kinds.length, result.operations().size());
        assertEquals(List.of(kinds), result.operations().stream()
                .map(PolicyOperation::outputKind).toList());
    }

    private static void assertDeltas(PolicyResult result, float... deltas) {
        assertEquals(deltas.length, result.operations().size());
        for (int i = 0; i < deltas.length; i++) {
            assertEquals(Float.floatToRawIntBits(deltas[i]),
                    Float.floatToRawIntBits(result.operations().get(i).delta()));
        }
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId, TraceDomainId domainId,
                                        TraceOutputKind outputKind, float delta,
                                        String reason) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(Float.floatToRawIntBits(delta),
                Float.floatToRawIntBits(operation.delta()));
        assertEquals(reason, operation.reason());
        assertEquals(ACTION_ID, operation.actionId());
        assertEquals(domainId, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
    }
}
