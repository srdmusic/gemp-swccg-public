package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveSequencingPolicyTest {

    @Test
    public void earlyLocationClassificationUsesCategoryBeforeDestinationText() {
        assertTrue(earlyLocationRoute("Deploy character to a site", true, true));
        assertFalse(earlyLocationRoute("Deploy character to a site", true, false));
        assertFalse(earlyLocationRoute("Deploy character to Bespin system", true, false));
    }

    @Test
    public void unresolvedEarlyLocationFallbackRequiresDeployedSubjectPhrase() {
        assertTrue(earlyLocationRoute("Deploy a location", false, false));
        assertTrue(earlyLocationRoute("Deploy Bespin system location", false, false));
        assertTrue(earlyLocationRoute("Deploy a battleground site from Reserve Deck", false, false));
        assertFalse(earlyLocationRoute("Deploy character to a site", false, false));
        assertFalse(earlyLocationRoute("Deploy to a site", false, false));
        assertFalse(earlyLocationRoute("Deploy device aboard a starship at system", false, false));
    }

    @Test
    public void earlyLocationPreservesBasePiettAndTurnOneBespinOrder() {
        DeployObjectiveSequencingPolicy.EarlyLocationEvaluation result =
                DeployObjectiveSequencingPolicy.evaluateEarlyLocation(
                        early(true, false, false, 4,
                                true, true, 1, true, false));

        assertOperations(result.result().operations(),
                new String[] {"deploy-location-text-priority",
                        "V24.10-piett-location-priority",
                        "V24.15-bespin-priority"},
                new float[] {200.0f, 150.0f, 300.0f});
        assertEquals(DeployObjectiveSequencingPolicy.AdapterStep.CONTINUE_ACTION,
                result.adapterStep());
        assertEquals(true, result.piettPriorityApplied());
        assertRaw(300.0f, result.bespinBoost());
    }

    @Test
    public void earlyLocationPreservesEveryPiettAndBespinBoundary() {
        assertOperations(evaluate(early(false, false, false, 1,
                        false, false, 1, false, false)),
                new float[] {200.0f});
        assertOperations(evaluate(early(true, true, false, 4,
                        false, false, 1, false, false)),
                new float[] {200.0f});
        assertOperations(evaluate(early(true, false, true, 4,
                        false, false, 1, false, false)),
                new float[] {200.0f});
        assertOperations(evaluate(early(true, false, false, 5,
                        false, false, 1, false, false)),
                new float[] {200.0f});
        assertOperations(evaluate(early(true, false, false, 4,
                        true, true, 2, true, false)),
                new float[] {200.0f, 150.0f, 300.0f});
        assertOperations(evaluate(early(true, false, false, 4,
                        true, true, 3, true, true)),
                new float[] {200.0f, 150.0f});
        assertOperations(evaluate(early(true, false, false, 4,
                        true, true, 4, true, false)),
                new float[] {200.0f, 150.0f});
    }

    @Test
    public void bespinFirstClassificationPreservesEveryBroadExemption() {
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE,
                route("deploy character", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                route("deploy location", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE,
                route("deploy to site", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                route("use amsd", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                route("deploy executor", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE,
                route("deploy character to bespin", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                route("deploy starship", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                route("deploy capital star destroyer", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE,
                route("deploy character aboard executor", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE,
                route("deploy character aboard star destroyer", false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                route("deploy bespin", false, false));
    }

    @Test
    public void resolvedBespinFirstClassificationUsesCardCategory() {
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE,
                resolvedRoute("deploy character to bespin system", true, false, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                resolvedRoute("deploy to a site", false, true, false));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                resolvedRoute("deploy capital star destroyer", false, false, true));
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstRoute.EXEMPT,
                resolvedRoute("deploy effect at bespin", false, false, false));
    }

    @Test
    public void bespinFirstPreservesReleaseAndUnavailableOraclePenalty() {
        DeployObjectiveSequencingPolicy.BespinFirstEvaluation forbidden =
                decision(true, false, false);
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstOutcome.RELEASED,
                forbidden.outcome());
        assertEquals(DeployObjectiveSequencingPolicy.AdapterStep.FALL_THROUGH,
                forbidden.adapterStep());
        assertEquals("objective game text forbids deploying Executor",
                forbidden.releaseReason());
        assertEquals(0, forbidden.result().operations().size());

        DeployObjectiveSequencingPolicy.BespinFirstEvaluation noCapital =
                decision(false, true, false);
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstOutcome.RELEASED,
                noCapital.outcome());
        assertEquals(DeployObjectiveSequencingPolicy.AdapterStep.FALL_THROUGH,
                noCapital.adapterStep());
        assertEquals("no capital starship in hand/reserve/force/used \u2014 no live path to occupy Bespin space",
                noCapital.releaseReason());
        assertEquals(0, noCapital.result().operations().size());

        DeployObjectiveSequencingPolicy.BespinFirstEvaluation unavailable =
                decision(false, false, false);
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstOutcome.PENALIZED,
                unavailable.outcome());
        assertEquals(DeployObjectiveSequencingPolicy.AdapterStep.FALL_THROUGH,
                unavailable.adapterStep());
        assertNull(unavailable.releaseReason());
        assertOperations(unavailable.result().operations(),
                new float[] {-300.0f});
        assertEquals("V29 BESPIN-FIRST: prefer the Bespin -> Executor/AMSD sequence before characters "
                        + "(-300 objective preference).",
                unavailable.result().operations().get(0).reason());

        DeployObjectiveSequencingPolicy.BespinFirstEvaluation capital =
                decision(false, true, true);
        assertEquals(DeployObjectiveSequencingPolicy.BespinFirstOutcome.PENALIZED,
                capital.outcome());
        assertEquals(DeployObjectiveSequencingPolicy.AdapterStep.FALL_THROUGH,
                capital.adapterStep());
        assertOperations(capital.result().operations(),
                new float[] {-300.0f});
    }

    private static DeployObjectiveSequencingFacts.EarlyLocation early(
            boolean oracleAnalyzed,
            boolean piettAccessible,
            boolean piettLost,
            int piettTurn,
            boolean objectiveAnalyzed,
            boolean needsBespin,
            int objectiveTurn,
            boolean bespinDeploy,
            boolean bespinOnTable) {
        return new DeployObjectiveSequencingFacts.EarlyLocation(
                "a", oracleAnalyzed, piettAccessible, piettLost,
                piettTurn, objectiveAnalyzed, needsBespin,
                objectiveTurn, bespinDeploy, bespinOnTable);
    }

    private static boolean earlyLocationRoute(
            String text, boolean resolved, boolean location) {
        return DeployObjectiveSequencingPolicy.isEarlyLocationCandidate(
                new DeployObjectiveSequencingFacts.EarlyLocationCandidate(
                        text, resolved, location));
    }

    private static List<PolicyOperation> evaluate(
            DeployObjectiveSequencingFacts.EarlyLocation facts) {
        return DeployObjectiveSequencingPolicy.evaluateEarlyLocation(facts)
                .result().operations();
    }

    private static DeployObjectiveSequencingPolicy.BespinFirstRoute route(
            String text, boolean location, boolean ship) {
        return DeployObjectiveSequencingPolicy.classifyBespinFirst(
                new DeployObjectiveSequencingFacts.BespinFirstCandidate(
                        text, false, false, location, ship));
    }

    private static DeployObjectiveSequencingPolicy.BespinFirstRoute resolvedRoute(
            String text, boolean character, boolean location, boolean ship) {
        return DeployObjectiveSequencingPolicy.classifyBespinFirst(
                new DeployObjectiveSequencingFacts.BespinFirstCandidate(
                        text, true, character, location, ship));
    }

    private static DeployObjectiveSequencingPolicy.BespinFirstEvaluation decision(
            boolean objectiveForbids,
            boolean oracleAnalyzed,
            boolean capitalAccessible) {
        return DeployObjectiveSequencingPolicy.evaluateBespinFirst(
                new DeployObjectiveSequencingFacts.BespinFirstDecision(
                        "a", objectiveForbids, oracleAnalyzed,
                        capitalAccessible));
    }

    private static void assertOperations(
            List<PolicyOperation> operations, String[] rules, float[] deltas) {
        assertEquals(rules.length, operations.size());
        for (int i = 0; i < rules.length; i++) {
            assertEquals(rules[i], operations.get(i).ruleArmId().id());
            assertRaw(deltas[i], operations.get(i).delta());
        }
    }

    private static void assertOperations(
            List<PolicyOperation> operations, float[] deltas) {
        assertEquals(deltas.length, operations.size());
        for (int i = 0; i < deltas.length; i++) {
            assertRaw(deltas[i], operations.get(i).delta());
        }
    }

    private static void assertRaw(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
