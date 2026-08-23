package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.FlipGateFormationRole;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Replay 70jll8yaavkpyy8h, game 72314: Tatooine formation coherence. */
public class Tatooine72314FormationCoherenceRegressionTest {

    @Test
    public void safeExactTarkinPlanKeepsJawaCampChildOverDevastator() {
        List<PolicyOperation> jawaCamp =
                DeployPlanPolicy.evaluateDestinationTarget(
                        new DeployPlanPolicy.DestinationTargetFacts(
                                "tarkin-to-jawa-camp", true, true,
                                "Tatooine: Jawa Camp", false))
                        .operations();
        assertEquals(1, jawaCamp.size());
        assertEquals("deploy-plan-target-match",
                jawaCamp.get(0).ruleArmId().id());
        assertEquals(PolicyOperationKind.ADD, jawaCamp.get(0).kind());

        List<PolicyOperation> devastator =
                DeployPlanPolicy.evaluateDestinationTarget(
                        new DeployPlanPolicy.DestinationTargetFacts(
                                "tarkin-aboard-devastator", false, true,
                                "Tatooine: Jawa Camp", false))
                        .operations();
        assertEquals("The unplanned child must remain only a mandatory "
                        + "fallback while the exact safe child is offered",
                1, devastator.stream()
                        .filter(operation -> operation.kind()
                                == PolicyOperationKind.DEFER)
                        .count());
    }

    @Test
    public void replayDevastatorStopsCollectingCrewAtActualAbilityFour() {
        SpaceDeploymentAllocationPolicy.Evaluation quietDevastator =
                SpaceDeploymentAllocationPolicy.evaluate(
                        new SpaceDeploymentAllocationPolicy.Facts(
                                "tarkin-aboard-devastator", true,
                                4.0f, 7.0f,
                                false, false, false, false, false));

        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.GROUND_FIRST_AFTER_FOUR,
                quietDevastator.outcome());
        assertTrue(quietDevastator.result().operations().stream()
                .anyMatch(operation ->
                        operation.kind() == PolicyOperationKind.DEFER));

        SpaceDeploymentAllocationPolicy.Evaluation neededBuddy =
                SpaceDeploymentAllocationPolicy.evaluate(
                        new SpaceDeploymentAllocationPolicy.Facts(
                                "needed-buddy-aboard", true,
                                2.0f, 4.0f,
                                false, false, false, false, false));
        assertEquals(SpaceDeploymentAllocationPolicy.Outcome.BUDDY_COMPLETE,
                neededBuddy.outcome());
        assertFalse(neededBuddy.result().operations().stream()
                .anyMatch(operation ->
                        operation.kind() == PolicyOperationKind.DEFER));
    }

    @Test
    public void dominantContactAlternativeStillEscapesExactPlanBinding() {
        List<PolicyOperation> dominantContact =
                DeployPlanPolicy.evaluateDestinationTarget(
                        new DeployPlanPolicy.DestinationTargetFacts(
                                "tarkin-dominant-contact", false, true,
                                "Tatooine: Jawa Camp", true))
                        .operations();

        assertFalse(dominantContact.stream().anyMatch(
                operation -> operation.kind()
                        == PolicyOperationKind.DEFER));
    }

    @Test
    public void replaySevenPowerFiveAbilityPacketCannotPromiseBattleIntoTwentyFour() {
        DeployTacticalPolicy.ResponseFormationAssessment assessment =
                DeployTacticalPolicy.assessPersistentResponseFormation(
                        contact(0.0f, 3.0f, 4.0f, 24.0f, 5.0f),
                        0.0f, 5.0f);

        assertEquals(7.0f, assessment.projectedFriendlyPower(), 0.0f);
        assertEquals(5.0f, assessment.projectedFriendlyAbility(), 0.0f);
        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.NONE,
                assessment.route());
        assertFalse(assessment.viable());
    }

    @Test
    public void nearParityContactWithoutBattleDestinyPathIsNotExecutable() {
        DeployTacticalPolicy.ResponseFormationAssessment assessment =
                DeployTacticalPolicy.assessPersistentResponseFormation(
                        contact(0.0f, 11.0f, 11.0f, 24.0f, 3.0f),
                        0.0f, 3.0f);

        assertEquals(22.0f, assessment.projectedFriendlyPower(), 0.0f);
        assertEquals(3.0f, assessment.projectedFriendlyAbility(), 0.0f);
        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.NONE,
                assessment.route());
        assertFalse(assessment.viable());
    }

    @Test
    public void fundedAbilityFourNearParityContactRemainsExecutable() {
        DeployTacticalPolicy.ResponseFormationAssessment assessment =
                DeployTacticalPolicy.assessPersistentResponseFormation(
                        contact(0.0f, 11.0f, 11.0f, 24.0f, 4.0f),
                        0.0f, 4.0f);

        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                assessment.route());
        assertTrue(assessment.viable());
    }

    @Test
    public void directContactOwnerRequiresProjectedBattleDestinyPath() {
        List<PolicyOperation> noDestiny =
                DeployTacticalPolicy.scoreV171V172Contact(
                        contact(0.0f, 11.0f, 11.0f, 24.0f, 3.0f))
                        .operations();
        assertTrue("V171 must not promise a same-turn battle below ability 4",
                noDestiny.isEmpty());

        List<PolicyOperation> destiny =
                DeployTacticalPolicy.scoreV171V172Contact(
                        contact(0.0f, 11.0f, 11.0f, 24.0f, 4.0f))
                        .operations();
        assertEquals(1, destiny.size());
        assertEquals("V171", destiny.get(0).ruleArmId().id());
    }

    @Test
    public void deliberateSoloDominanceDoesNotRequireWaveDestiny() {
        DeployTacticalPolicy.ContactFacts tyranus =
                new DeployTacticalPolicy.ContactFacts(
                        "tyranus-solo", "Tatooine: Jawa Camp",
                        true, true, 1, 0.0f, 10.0f, 0.0f,
                        0.0f, 2.0f, 5.0f, 10.0f, 0, 0.0f);
        List<PolicyOperation> operations =
                DeployTacticalPolicy.scoreV171V172Contact(tyranus)
                        .operations();

        assertEquals(1, operations.size());
        assertEquals("V172", operations.get(0).ruleArmId().id());
    }

    @Test
    public void doomedJawaCampDeploymentStillRetainsSafeRetreat() {
        MoveObjectiveGateHoldPolicy.Evaluation gateHold =
                MoveObjectiveGateHoldPolicy.evaluateRuntimeActorFormation(
                        true, FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                        7.0f, 14.0f);
        assertFalse("A seven-power deficit is already doomed, so an exact "
                        + "plan hold must not trap Tarkin there",
                gateHold.hardVeto());

        MoveDestinationPolicy.Contribution retreat =
                MoveDestinationPolicy.safeRetreatDestination(
                        MoveDestinationPolicy.retreatMode(
                                "Tatooine: Jawa Camp", 7.0f),
                        "Devastator", 0.0f);
        assertTrue(retreat.applies());
        assertEquals(600.0f, retreat.delta(), 0.0f);
    }

    @Test
    public void randoAndChosenDestinationAdaptersRemainMirrored()
            throws IOException {
        assertEquals(normalize(cardSelectionSource("rando")),
                normalize(cardSelectionSource("chosenone")));
    }

    private static DeployTacticalPolicy.ContactFacts contact(
            float ourPower, float leadPower, float buddyPower,
            float opponentEffectivePower,
            float projectedBattleDestinyAbility) {
        return new DeployTacticalPolicy.ContactFacts(
                "game-72314-contact", "Tatooine: Jawa Camp",
                true, true, 2, ourPower, leadPower, buddyPower,
                1.0f, 2.0f, opponentEffectivePower,
                Math.max(leadPower, buddyPower), 0,
                projectedBattleDestinyAbility);
    }

    private static String cardSelectionSource(String bot)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators")
                .resolve("CardSelectionEvaluator.java"));
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) return repoLayout;
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate gemp-swccg-server main/java");
    }
}
