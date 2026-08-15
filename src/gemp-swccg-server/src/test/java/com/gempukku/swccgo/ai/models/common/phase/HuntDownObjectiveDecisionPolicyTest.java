package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HuntDownObjectiveDecisionPolicyTest {

    @Test
    public void deployScoresOnlyAQualifyingActorRuntimeDestination() {
        PolicyResult qualifying =
                DeployObjectiveSitingPolicy.scoreActorRuntimeLocation(
                        "deploy-vader", true);
        assertObjectiveOperation(qualifying, 0,
                "DEPLOY.OBJECTIVE.ACTOR_RUNTIME_LOCATION",
                300.0f, TraceOutputKind.BANDED);

        assertEmpty(DeployObjectiveSitingPolicy
                .scoreActorRuntimeLocation("deploy-vader", false));
    }

    @Test
    public void moveScoresOnlyAnAdvancingActorLocationStartAndDestination() {
        MoveDestinationPolicy.Contribution start =
                MoveDestinationPolicy.objectiveActorLocationStart(
                        true, "Darth Vader");
        assertTrue(start.applies());
        assertEquals(300.0f, start.delta(), 0.0f);
        assertEquals("MOVE.OBJECTIVE.ACTOR_LOCATION_START",
                moveRuleId(start));

        MoveDestinationPolicy.Contribution noStart =
                MoveDestinationPolicy.objectiveActorLocationStart(
                        false, "Darth Vader");
        assertFalse(noStart.applies());
        assertEquals(0.0f, noStart.delta(), 0.0f);
        assertNull(noStart.reason());

        MoveDestinationPolicy.Contribution destination =
                MoveDestinationPolicy.objectiveActorLocationDestination(
                        true, "Darth Vader", "Tatooine: Cantina");
        assertTrue(destination.applies());
        assertEquals(300.0f, destination.delta(), 0.0f);
        assertEquals("MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION",
                moveRuleId(destination));

        MoveDestinationPolicy.Contribution noDestination =
                MoveDestinationPolicy.objectiveActorLocationDestination(
                        false, "Darth Vader", "Tatooine: Cantina");
        assertFalse(noDestination.applies());
        assertEquals(0.0f, noDestination.delta(), 0.0f);
        assertNull(noDestination.reason());
    }

    @Test
    public void globalBlockerBattleRequiresEverySafetyGate() {
        PolicyResult safe = blockerBattle(
                true, false, true, -2.0f,
                3, 6.0f, 6.0f);
        assertObjectiveOperation(safe, 0,
                ObjectiveBattlePolicy.GLOBAL_BLOCKER_REMOVAL_RULE_ID,
                300.0f, TraceOutputKind.BANDED);

        assertEmpty(blockerBattle(
                false, false, true, -2.0f,
                3, 6.0f, 6.0f));
        assertEmpty(blockerBattle(
                true, true, true, -2.0f,
                3, 6.0f, 6.0f));
        assertEmpty(blockerBattle(
                true, false, false, -2.0f,
                3, 6.0f, 6.0f));
        assertEmpty(blockerBattle(
                true, false, true, -2.01f,
                3, 6.0f, 6.0f));
        assertEmpty(blockerBattle(
                true, false, true, -2.0f,
                2, 6.0f, 6.0f));
        assertEmpty(blockerBattle(
                true, false, true, -2.0f,
                3, 3.0f, 7.0f));
    }

    @Test
    public void ordinaryControlContestAndGlobalBlockerStayIndependent() {
        PolicyResult control = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-control",
                        true, true, false,
                        true, false, true,
                        0.0f, 3, 6.0f, 6.0f));
        assertEquals(1, control.operations().size());
        assertObjectiveOperation(control, 0,
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_RULE_ID,
                300.0f, TraceOutputKind.BANDED);

        PolicyResult blocker = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-blocker",
                        false, false, true,
                        true, false, true,
                        0.0f, 3, 6.0f, 6.0f));
        assertEquals(1, blocker.operations().size());
        assertObjectiveOperation(blocker, 0,
                ObjectiveBattlePolicy.GLOBAL_BLOCKER_REMOVAL_RULE_ID,
                300.0f, TraceOutputKind.BANDED);

        PolicyResult both = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-both",
                        true, true, true,
                        true, false, true,
                        0.0f, 3, 6.0f, 6.0f));
        assertEquals(2, both.operations().size());
        assertObjectiveOperation(both, 0,
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_RULE_ID,
                300.0f, TraceOutputKind.BANDED);
        assertObjectiveOperation(both, 1,
                ObjectiveBattlePolicy.GLOBAL_BLOCKER_REMOVAL_RULE_ID,
                0.0f, TraceOutputKind.BANDED);
        assertEquals(300.0f, total(both), 0.0f);
    }

    @Test
    public void lastOnTableActorForfeitPreferenceNeedsAnotherLegalLoss() {
        PolicyResult protectedActor = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "forfeit-vader",
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ON_TABLE_ACTOR,
                        true);
        assertObjectiveOperation(protectedActor, 0,
                "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                -300.0f, TraceOutputKind.BANDED);
        assertTrue(protectedActor.operations().get(0).reason()
                .contains("last required actor on table"));

        assertEmpty(BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "forfeit-vader",
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ON_TABLE_ACTOR,
                        false));
    }

    @Test
    public void castleDownloadRequiresAnExactLegalReserveCandidate() {
        PolicyResult unavailable = DeployActionTextPolicy.scoreVaderCastle(
                new DeployActionTextFacts.VaderCastleFacts(
                        "castle-download", true, true,
                        false, false, false, 8));
        assertEquals(1, unavailable.operations().size());
        assertOperation(unavailable, 0,
                "V25-vader-castle-no-legal-candidate",
                -500.0f, TraceOutputKind.VETO);
        assertEquals(-500.0f, total(unavailable), 0.0f);

        PolicyResult legal = DeployActionTextPolicy.scoreVaderCastle(
                new DeployActionTextFacts.VaderCastleFacts(
                        "castle-download", true, true,
                        false, true, true, 7));
        assertEquals(1, legal.operations().size());
        assertObjectiveOperation(legal, 0,
                "V25-vader-castle-priority",
                300.0f, TraceOutputKind.ORDERING);
        assertEquals(300.0f, total(legal), 0.0f);
    }

    @Test
    public void virtualLocationDownloadScoresOnlyWithAnExactLegalTarget() {
        PolicyResult available =
                PullSpecificActionPolicy.scoreHuntDownLocationDownload(
                        new PullSpecificActionFacts.HuntDownLocationDownload(
                                "download-site", true));
        assertObjectiveOperation(available, 0,
                "PULL.OBJECTIVE.HUNT_DOWN_LOCATION_DOWNLOAD",
                300.0f, TraceOutputKind.ORDERING);

        assertEmpty(PullSpecificActionPolicy.scoreHuntDownLocationDownload(
                new PullSpecificActionFacts.HuntDownLocationDownload(
                        "download-site", false)));
    }

    @Test
    public void v51RequiresTheAdapterToProveAllLiveConditions() {
        assertEmpty(DeployTacticalPolicy.scoreV51VaderFlip(
                new DeployTacticalPolicy.VaderFlipFacts(
                        "deploy-vader", "Tatooine: Cantina", false)));

        PolicyResult complete = DeployTacticalPolicy.scoreV51VaderFlip(
                new DeployTacticalPolicy.VaderFlipFacts(
                        "deploy-vader", "Tatooine: Cantina", true));
        assertObjectiveOperation(complete, 0,
                "V51", 300.0f, TraceOutputKind.BANDED);
    }

    private static PolicyResult blockerBattle(
            boolean bothSidesPresent,
            boolean formationSafetyVeto,
            boolean predictorSafe,
            float effectiveDiff,
            int reserveDeckSize,
            float ourPower,
            float theirPower) {
        return ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-blocker",
                        false, false, true,
                        bothSidesPresent, formationSafetyVeto,
                        predictorSafe, effectiveDiff,
                        reserveDeckSize, ourPower, theirPower));
    }

    private static void assertOperation(
            PolicyResult result,
            int index,
            String ruleId,
            float delta,
            TraceOutputKind outputKind) {
        PolicyOperation operation = result.operations().get(index);
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(outputKind, operation.outputKind());
    }

    private static void assertObjectiveOperation(
            PolicyResult result,
            int index,
            String ruleId,
            float delta,
            TraceOutputKind outputKind) {
        assertOperation(result, index, ruleId, delta, outputKind);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                result.operations().get(index).domainId());
    }

    private static void assertEmpty(PolicyResult result) {
        assertTrue(result.operations().isEmpty());
    }

    private static float total(PolicyResult result) {
        float total = 0.0f;
        for (PolicyOperation operation : result.operations()) {
            total += operation.delta();
        }
        return total;
    }

    private static String moveRuleId(
            MoveDestinationPolicy.Contribution contribution) {
        int separator = contribution.reason().indexOf(':');
        return contribution.reason().substring(0, separator);
    }
}
