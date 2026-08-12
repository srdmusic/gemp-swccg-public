package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DeployPlanRankingPolicyTest {

    @Test
    public void emitsExactOrderedScalarContributionContract() {
        PolicyResult core = DeployPlanRankingPolicy.evaluate(
                List.of(
                        instruction("instruction-0", 2),
                        instruction("instruction-1", 3)),
                List.of(
                        location("location-favorable", 8, 4, 4.0f,
                                2, 3, true, 150.0f),
                        location("location-marginal", 5, 3, 4.0f,
                                0, 0, false, 0.0f),
                        location("location-losing", 4, 4, 4.0f,
                                2, 3, false, 0.0f),
                        location("location-establish", 4, 3, 0.0f,
                                0, 0, false, 0.0f)));
        PolicyResult adjunct = DeployPlanRankingPolicy.evaluateAdjunct(
                new DeployPlanRankingPolicy.AdjunctFacts(
                        "adjunct-capital", true));

        List<PolicyOperation> operations = new ArrayList<>(core.operations());
        operations.addAll(adjunct.operations());
        assertEquals("DEPLOY_PLAN_RANKING_POLICY", core.producerId());
        assertEquals("DEPLOY_PLAN_RANKING_POLICY", adjunct.producerId());
        assertOperations(operations,
                new String[]{
                        "instruction-0", "instruction-1",
                        "location-favorable", "location-favorable",
                        "location-favorable", "location-marginal",
                        "location-marginal", "location-losing",
                        "location-losing", "location-establish",
                        "adjunct-capital"},
                new String[]{
                        "deploy-plan-ranking-base-power",
                        "deploy-plan-ranking-base-power",
                        "V22-plan-ranking-objective-location",
                        "deploy-plan-ranking-favorable",
                        "deploy-plan-ranking-destiny",
                        "deploy-plan-ranking-marginal",
                        "deploy-plan-ranking-vulnerable",
                        "deploy-plan-ranking-losing",
                        "deploy-plan-ranking-destiny",
                        "deploy-plan-ranking-establish",
                        "V22-plan-ranking-objective-capital-bespin"},
                new float[]{
                        4.0f, 6.0f, 150.0f, 175.0f, 25.0f,
                        30.0f, -30.0f, 45.0f, 25.0f, -460.0f,
                        200.0f},
                new String[]{
                        "Base power value",
                        "Base power value",
                        "V22 objective-relevant location bonus",
                        "FAVORABLE FIGHT",
                        "Can draw destiny",
                        "MARGINAL FIGHT",
                        "Vulnerable",
                        "LOSING",
                        "Can draw destiny",
                        "EMPTY/ESTABLISH LOCATION",
                        "V22 objective capital ship priority for Bespin"});

        float coreScore = DeployPlanRankingPolicy.apply(0.0f, core);
        assertBits(-30.0f, coreScore);
        assertBits(170.0f, DeployPlanRankingPolicy.apply(coreScore, adjunct));
    }

    @Test
    public void preservesNoIconAndFractionalPresenceBoundaries() {
        assertBits(38.0f, score(4, 4, 4.0f, 0, 0, false, 0.0f));
        assertBits(65.0f, score(5, 4, 4.0f, 0, 0, false, 0.0f));
        assertBits(79.0f, score(7, 4, 4.0f, 0, 0, false, 0.0f));
        assertBits(131.0f, score(8, 4, 4.0f, 0, 0, false, 0.0f));
        assertBits(70.0f, score(8, 3, 4.0f, 0, 0, false, 0.0f));

        assertBits(73.0f, score(4, 4, 0.0f, 0, 0, false, 0.0f));
        assertBits(123.0f, score(4, 4, 0.5f, 0, 0, false, 0.0f));
    }

    @Test
    public void preservesEstablishIconAndObjectiveTotals() {
        assertBits(-452.0f, score(4, 3, 0.0f, 0, 0, false, 0.0f));
        assertBits(50.0f, score(5, 3, 0.0f, 0, 0, false, 0.0f));
        assertBits(75.0f, score(5, 4, 0.0f, 0, 0, false, 0.0f));

        assertBits(216.0f, score(8, 4, 4.0f, 2, 3, false, 0.0f));
        assertBits(78.0f, score(4, 4, 4.0f, 2, 3, false, 0.0f));

        float objective = score(8, 4, 4.0f, 0, 0, true, 150.0f);
        assertBits(281.0f, objective);
        PolicyResult capital = DeployPlanRankingPolicy.evaluateAdjunct(
                new DeployPlanRankingPolicy.AdjunctFacts("capital", true));
        assertBits(481.0f, DeployPlanRankingPolicy.apply(objective, capital));

        PolicyResult earlyRescore = DeployPlanRankingPolicy.evaluateAdjunct(
                new DeployPlanRankingPolicy.AdjunctFacts("early", false));
        assertEquals(0, earlyRescore.operations().size());
        assertBits(281.0f, DeployPlanRankingPolicy.apply(objective, earlyRescore));
    }

    @Test
    public void flipGateBonusRequiresCompleteActorAndBuddyFormation() {
        PolicyResult incomplete = DeployPlanRankingPolicy.evaluateFlipGateFormation(
                new DeployPlanRankingPolicy.FlipGateFormationFacts(
                        "flip-gate", false, 1600.0f));
        assertEquals(0, incomplete.operations().size());

        PolicyResult complete = DeployPlanRankingPolicy.evaluateFlipGateFormation(
                new DeployPlanRankingPolicy.FlipGateFormationFacts(
                        "flip-gate", true, 1600.0f));
        assertOperations(complete.operations(),
                new String[]{"flip-gate"},
                new String[]{"V297-plan-ranking-flip-gate-formation"},
                new float[]{1600.0f},
                new String[]{"V297 objective flip-gate actor and buddy formation"});
        assertBits(1700.0f, DeployPlanRankingPolicy.apply(100.0f, complete));
    }

    @Test
    public void existingFriendlyFormationCrossesLegacyDominanceWithoutNewScoreTower() {
        PolicyResult result = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("instruction", 4)),
                List.of(reactLocation("target", 8.0f, 4.0f, 4.0f,
                        1, true, true, 16.0f, false)));

        assertBits(123.0f, DeployPlanRankingPolicy.apply(0.0f, result));
        assertEquals(0, countRule(result,
                "deploy-plan-ranking-isolated-packet"));
    }

    @Test
    public void existingTokenDoesNotHideStillDominatedReinforcement() {
        PolicyResult small = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("small-instruction", 4)),
                List.of(reactLocation("small-target", 5.0f, 4.0f, 0.0f,
                        0, true, true, 10.0f, false)));
        PolicyResult mass = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("mass-instruction", 9)),
                List.of(reactLocation("mass-target", 10.0f, 4.0f, 0.0f,
                        0, true, true, 10.0f, false)));

        assertBits(-77.0f, DeployPlanRankingPolicy.apply(0.0f, small));
        assertEquals(1, countRule(small,
                "deploy-plan-ranking-isolated-packet"));
        assertBits(83.0f, DeployPlanRankingPolicy.apply(0.0f, mass));
        assertEquals(0, countRule(mass,
                "deploy-plan-ranking-isolated-packet"));
    }

    @Test
    public void equalTotalPowerConsolidationBeatsTwoReactExposedPackets() {
        List<DeployPlanRankingPolicy.InstructionFacts> instructions = List.of(
                instruction("instruction-0", 5),
                instruction("instruction-1", 5));
        PolicyResult consolidated = DeployPlanRankingPolicy.evaluate(
                instructions,
                List.of(reactLocation("target", 10.0f, 4.0f, 0.0f,
                        0, true, true, 10.0f, false)));
        PolicyResult split = DeployPlanRankingPolicy.evaluate(
                instructions,
                List.of(
                        reactLocation("target-0", 5.0f, 2.0f, 0.0f,
                                0, true, true, 10.0f, false),
                        reactLocation("target-1", 5.0f, 2.0f, 0.0f,
                                0, true, true, 10.0f, false)));

        assertBits(85.0f, DeployPlanRankingPolicy.apply(0.0f, consolidated));
        assertEquals(0, countRule(consolidated,
                "deploy-plan-ranking-isolated-packet"));
        assertBits(-200.0f, DeployPlanRankingPolicy.apply(0.0f, split));
        assertEquals(2, countRule(split,
                "deploy-plan-ranking-isolated-packet"));
    }

    @Test
    public void immediateReactDominanceBoundaryIsInclusiveAndUnrounded() {
        PolicyResult below = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("below-instruction", 4)),
                List.of(reactLocation("below-target", 5.0f, 4.0f, 0.0f,
                        0, true, true, 9.999f, false)));
        PolicyResult equal = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("equal-instruction", 4)),
                List.of(reactLocation("equal-target", 5.0f, 4.0f, 0.0f,
                        0, true, true, 10.0f, false)));
        PolicyResult friendlyDominant = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("dominant-instruction", 9)),
                List.of(reactLocation("dominant-target", 10.0f, 4.0f, 0.0f,
                        0, true, true, 5.0f, false)));

        assertEquals(0, countRule(below,
                "deploy-plan-ranking-isolated-packet"));
        assertEquals(1, countRule(equal,
                "deploy-plan-ranking-isolated-packet"));
        assertEquals(0, countRule(friendlyDominant,
                "deploy-plan-ranking-isolated-packet"));
    }

    @Test
    public void rejectsDuplicateContributionIdsBeforeLedgerRegistration() {
        assertThrows(IllegalArgumentException.class, () ->
                DeployPlanRankingPolicy.evaluate(
                        List.of(instruction("duplicate", 2),
                                instruction("duplicate", 3)),
                        List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                DeployPlanRankingPolicy.evaluate(
                        List.of(instruction("duplicate", 2)),
                        List.of(location("duplicate", 4, 4, 4.0f,
                                0, 0, false, 0.0f))));
    }

    private static float score(int ourPower, int ourAbility,
                               float theirPower, int ourForceIcons,
                               int theirForceIcons, boolean objectiveRelevant,
                               float objectiveBonus) {
        PolicyResult result = DeployPlanRankingPolicy.evaluate(
                List.of(instruction("instruction", ourPower)),
                List.of(location("location", ourPower, ourAbility,
                        theirPower, ourForceIcons, theirForceIcons,
                        objectiveRelevant, objectiveBonus)));
        return DeployPlanRankingPolicy.apply(0.0f, result);
    }

    private static DeployPlanRankingPolicy.InstructionFacts instruction(
            String contributionId, int power) {
        return new DeployPlanRankingPolicy.InstructionFacts(
                contributionId, power);
    }

    private static DeployPlanRankingPolicy.LocationFacts location(
            String contributionId, int ourPower, int ourAbility,
            float theirPower, int ourForceIcons, int theirForceIcons,
            boolean objectiveRelevant, float objectiveBonus) {
        return new DeployPlanRankingPolicy.LocationFacts(
                contributionId, ourPower, ourAbility, theirPower,
                ourForceIcons, theirForceIcons,
                objectiveRelevant, objectiveBonus);
    }

    private static DeployPlanRankingPolicy.LocationFacts reactLocation(
            String contributionId, float postOurPower, float postOurAbility,
            float theirPower, int theirCardCount, boolean triggerKnowable,
            boolean exposureProven, float strongestReactPower,
            boolean formationPenaltyExempt) {
        return new DeployPlanRankingPolicy.LocationFacts(
                contributionId, postOurPower, postOurAbility, theirPower,
                0, 0, theirCardCount, false, 0.0f,
                triggerKnowable, exposureProven, strongestReactPower,
                formationPenaltyExempt);
    }

    private static long countRule(PolicyResult result, String ruleId) {
        return result.operations().stream()
                .filter(operation -> ruleId.equals(operation.ruleArmId().id()))
                .count();
    }

    private static void assertOperations(
            List<PolicyOperation> operations,
            String[] contributionIds,
            String[] ruleIds,
            float[] deltas,
            String[] reasons) {
        assertEquals(ruleIds.length, operations.size());
        for (int i = 0; i < ruleIds.length; i++) {
            PolicyOperation operation = operations.get(i);
            assertEquals(contributionIds[i], operation.actionId());
            assertEquals(ruleIds[i], operation.ruleArmId().id());
            assertBits(deltas[i], operation.delta());
            assertEquals(reasons[i], operation.reason());
            assertEquals(TraceDomainId.DEPLOY_SEQUENCING,
                    operation.domainId());
            assertEquals(TraceOutputKind.ORDERING, operation.outputKind());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
        }
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
