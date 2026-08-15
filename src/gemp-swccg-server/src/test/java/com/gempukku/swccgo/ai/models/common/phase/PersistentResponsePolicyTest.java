package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.ForceDrainState;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PersistentResponsePolicyTest {
    private static final String BOT = "bot";
    private static final String OPPONENT = "opponent";

    @Test
    public void ledgerRecordsFinalPaidNotAnnouncedOrRemainingLiability() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        ForceDrainState drain = drain(117, "Carbonite Chamber", 0);
        when(drain.getForceTotal()).thenReturn(5);
        when(drain.getForceRemaining()).thenReturn(2);

        ledger.observe(game.set(drain, OPPONENT, 12), BOT);
        when(drain.getForcePaid()).thenReturn(1);
        ledger.observe(game.set(null, OPPONENT, 12), BOT);
        assertEquals(11, ledger.snapshot().completedOpponentTurn());
        ledger.observe(game.set(null, BOT, 12), BOT);

        PersistentResponsePolicy.DrainHistory history =
                ledger.snapshot().historyAt(117).orElseThrow();
        assertEquals(1, history.latestDamage());
        assertEquals(2, history.projectedTwoTurnDamage());
        verify(drain, never()).getForceTotal();
        verify(drain, never()).getForceRemaining();
    }

    @Test
    public void nullBetweenTwoDrainsDoesNotCloseTurnAndDamageAggregates() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        ForceDrainState first = drain(82, "Lower Corridor", 0);

        ledger.observe(game.set(first, OPPONENT, 20), BOT);
        ledger.observe(game.set(first, OPPONENT, 20), BOT);
        when(first.getForcePaid()).thenReturn(1);
        ledger.observe(game.set(null, OPPONENT, 20), BOT);
        assertEquals(19, ledger.snapshot().completedOpponentTurn());
        assertTrue(ledger.snapshot().repeatedThreatAt(82).isEmpty());

        ForceDrainState second = drain(82, "Lower Corridor", 0);
        ledger.observe(game.set(second, OPPONENT, 20), BOT);
        when(second.getForcePaid()).thenReturn(2);
        ledger.observe(game.set(null, OPPONENT, 20), BOT);
        assertEquals(19, ledger.snapshot().completedOpponentTurn());

        ledger.observe(game.set(null, BOT, 20), BOT);
        PersistentResponsePolicy.DrainHistory history =
                ledger.snapshot().historyAt(82).orElseThrow();
        assertEquals(3, history.latestDamage());
        assertEquals(1, history.consecutiveOpponentTurns());
        assertEquals(6, history.projectedTwoTurnDamage());
    }

    @Test
    public void firstBotTurnAndCurrentOpponentTurnUseCompletedBoundary() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();

        ledger.observe(game.set(null, BOT, 0), BOT);
        assertEquals(0, ledger.snapshot().completedOpponentTurn());
        long firstBotRevision = ledger.snapshot().revision();

        ledger.observe(game.set(null, OPPONENT, 1), BOT);
        ledger.observe(game.set(null, OPPONENT, 1), BOT);
        assertEquals(0, ledger.snapshot().completedOpponentTurn());
        assertEquals(firstBotRevision, ledger.snapshot().revision());
    }

    @Test
    public void pregameOpponentTurnZeroClampsCompletedBoundaryToZero() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();

        ledger.observe(game.set(null, OPPONENT, 0), BOT);

        assertEquals(0, ledger.snapshot().completedOpponentTurn());
        assertEquals(0, ledger.snapshot().revision());
    }

    @Test
    public void nextOpponentTurnClosesThroughSkippedBotDecision() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        ForceDrainState firstTurn = drain(82, "Lower Corridor", 0);
        ledger.observe(game.set(firstTurn, OPPONENT, 1), BOT);
        when(firstTurn.getForcePaid()).thenReturn(2);
        ledger.observe(game.set(null, OPPONENT, 1), BOT);
        assertEquals(0, ledger.snapshot().completedOpponentTurn());

        ledger.observe(game.set(null, OPPONENT, 2), BOT);

        assertEquals(1, ledger.snapshot().completedOpponentTurn());
        assertEquals(2, ledger.snapshot().historyAt(82)
                .orElseThrow().latestDamage());
        assertTrue(ledger.snapshot().repeatedThreatAt(82).isEmpty());
    }

    @Test
    public void consecutiveCompletedTurnsUseActualFivePlusFourDamage() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();

        finishAndClose(ledger, game, 12, 117, "Carbonite Chamber", 5);
        finishAndClose(ledger, game, 13, 117, "Carbonite Chamber", 4);

        PersistentResponsePolicy.DrainHistory history =
                ledger.snapshot().repeatedThreatAt(117).orElseThrow();
        assertEquals(2, history.consecutiveOpponentTurns());
        assertEquals(4, history.latestDamage());
        assertEquals(9, history.projectedTwoTurnDamage());
    }

    @Test
    public void completedDryTurnExpiresOnlyThatLocationOnce() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        finishAndClose(ledger, game, 12, 117, "Carbonite Chamber", 5);
        finishAndClose(ledger, game, 13, 117, "Carbonite Chamber", 4);
        long beforeDry = ledger.snapshot().revision();

        ledger.observe(game.set(null, BOT, 14), BOT);
        long afterDry = ledger.snapshot().revision();
        ledger.observe(game.set(null, BOT, 14), BOT);

        PersistentResponsePolicy.DrainHistory history =
                ledger.snapshot().historyAt(117).orElseThrow();
        assertEquals(0, history.consecutiveOpponentTurns());
        assertEquals(0, history.latestDamage());
        assertEquals(0, history.projectedTwoTurnDamage());
        assertEquals(beforeDry + 1, afterDry);
        assertEquals(afterDry, ledger.snapshot().revision());
    }

    @Test
    public void zeroPaidCreatesNoHistoryAndIdsDoNotMerge() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        finishAndClose(ledger, game, 3, 82, "Lower Corridor", 0);
        assertFalse(ledger.snapshot().historyAt(82).isPresent());

        finishAndClose(ledger, game, 4, 82, "Shared Title", 1);
        finishAndClose(ledger, game, 5, 83, "Shared Title", 2);
        assertEquals(0, ledger.snapshot().historyAt(82)
                .orElseThrow().latestDamage());
        assertEquals(2, ledger.snapshot().historyAt(83)
                .orElseThrow().latestDamage());
    }

    @Test
    public void resetClearsPerGameEvidenceAndAdvancesRevision() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        finishAndClose(ledger, game, 4, 117, "Carbonite Chamber", 3);
        long beforeReset = ledger.snapshot().revision();

        ledger.reset();

        assertTrue(ledger.snapshot().histories().isEmpty());
        assertEquals(0, ledger.snapshot().completedOpponentTurn());
        assertEquals(beforeReset + 1, ledger.snapshot().revision());
    }

    @Test
    public void replacementGameStateDiscardsRetainedHistory() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView original = new MutableGameView();
        finishAndClose(ledger, original, 12, 117,
                "Carbonite Chamber", 5);
        finishAndClose(ledger, original, 13, 117,
                "Carbonite Chamber", 4);
        assertTrue(ledger.snapshot().repeatedThreatAt(117).isPresent());
        long beforeReplacement = ledger.snapshot().revision();
        MutableGameView replacement = new MutableGameView();

        ledger.observe(replacement.set(null, BOT, 13), BOT);

        assertTrue(ledger.snapshot().histories().isEmpty());
        assertTrue(ledger.snapshot().repeatedThreatAt(117).isEmpty());
        assertTrue(ledger.snapshot().revision() > beforeReplacement);
    }

    @Test
    public void regressedOpponentCounterDiscardsRetainedHistory() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        finishAndClose(ledger, game, 4, 82, "Lower Corridor", 2);

        ledger.observe(game.set(null, OPPONENT, 3), BOT);

        assertTrue(ledger.snapshot().histories().isEmpty());
        assertTrue(ledger.snapshot().repeatedThreatAt(82).isEmpty());
        assertEquals(2, ledger.snapshot().completedOpponentTurn());
    }

    @Test
    public void activeDrainPastCompletedBoundaryResetsWithoutCrashing() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();
        ForceDrainState drain = drain(82, "Lower Corridor", 0);
        ledger.observe(game.set(drain, OPPONENT, 1), BOT);
        long before = ledger.snapshot().revision();

        ledger.observe(game.set(drain, BOT, 1), BOT);

        assertTrue(ledger.snapshot().histories().isEmpty());
        assertEquals(1, ledger.snapshot().completedOpponentTurn());
        assertTrue(ledger.snapshot().revision() > before);
        when(drain.getForcePaid()).thenReturn(9);
        ledger.observe(game.set(null, BOT, 1), BOT);
        assertTrue(ledger.snapshot().histories().isEmpty());
    }

    @Test
    public void unhitLaneExpiresWhileOtherLaneAdvances() {
        PersistentResponsePolicy.State ledger =
                new PersistentResponsePolicy.State();
        MutableGameView game = new MutableGameView();

        finishEvent(ledger, game, 10, 117, "Carbonite Chamber", 1);
        finishEvent(ledger, game, 10, 82, "Lower Corridor", 1);
        ledger.observe(game.set(null, BOT, 10), BOT);
        finishEvent(ledger, game, 11, 117, "Carbonite Chamber", 1);
        finishEvent(ledger, game, 11, 82, "Lower Corridor", 1);
        ledger.observe(game.set(null, BOT, 11), BOT);
        assertTrue(ledger.snapshot().repeatedThreatAt(117).isPresent());
        assertTrue(ledger.snapshot().repeatedThreatAt(82).isPresent());

        finishEvent(ledger, game, 12, 117, "Carbonite Chamber", 2);
        ledger.observe(game.set(null, BOT, 12), BOT);

        PersistentResponsePolicy.DrainHistory advancing =
                ledger.snapshot().repeatedThreatAt(117).orElseThrow();
        assertEquals(2, advancing.latestDamage());
        assertEquals(3, advancing.projectedTwoTurnDamage());
        assertTrue(ledger.snapshot().repeatedThreatAt(82).isEmpty());
        PersistentResponsePolicy.DrainHistory expired =
                ledger.snapshot().historyAt(82).orElseThrow();
        assertEquals(0, expired.latestDamage());
        assertEquals(0, expired.projectedTwoTurnDamage());
    }

    @Test
    public void twoConsecutiveDrainOneHasNoGlobalDamageFloor() {
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(response(
                        "lower-corridor", 82, "Lower Corridor",
                        PersistentResponsePolicy.Mode.CONTEST,
                        PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                        2, 2, 0,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        false))).orElseThrow();

        assertEquals("lower-corridor", selected.candidateKey().value());
        assertEquals(300, selected.persistentBonus());
        assertEquals(0, selected.criticalBonus());
    }

    @Test
    public void oneOffAndUnprovenOrdinaryOwnSiteRemainSilent() {
        PersistentResponsePolicy.CandidateFacts oneOff = response(
                "one-off", 117, "Carbonite Chamber",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                1, 100, 5,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        PersistentResponsePolicy.CandidateFacts ordinary = response(
                "ordinary-own-site", 500, "Naboo: Swamp",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.NONE,
                0, 0, 0,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);

        assertTrue(PersistentResponsePolicy.select(
                List.of(oneOff, ordinary)).isEmpty());
    }

    @Test
    public void ordinaryEnemyHeldDrainOneDoesNotBecomeStrategicIncome() {
        PersistentResponsePolicy.CandidateFacts ordinaryDrainOne = response(
                "ordinary-enemy-drain-one", 510,
                "Cloud City: Guest Quarters",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.NONE,
                1, 1, 1,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);

        assertTrue(PersistentResponsePolicy.select(
                List.of(ordinaryDrainOne)).isEmpty());
    }

    @Test
    public void ordinaryPersistentLaneGetsNoObjectiveCriticalBonus() {
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(response(
                        "ordinary-persistent", 500, "Naboo: Swamp",
                        PersistentResponsePolicy.Mode.CONTEST,
                        PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                        2, 4, 0,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        false))).orElseThrow();

        assertEquals(300, selected.persistentBonus());
        assertEquals(0, selected.criticalBonus());
    }

    @Test
    public void equalExecutableRaceAlternativeWinsWithoutSummingValues() {
        PersistentResponsePolicy.CandidateFacts response = response(
                "response", 700, "Night Club",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 4, 1,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        PersistentResponsePolicy.CandidateFacts race = alternative(
                "race", 700, "Night Club", 701, "Safe Drain Lane",
                PersistentResponsePolicy.Mode.RACE,
                2, 3, false);

        assertEquals("race", PersistentResponsePolicy.select(
                List.of(response, race)).orElseThrow()
                .candidateKey().value());

        PersistentResponsePolicy.CandidateFacts stronger = response(
                "stronger-response", 700, "Night Club",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 5, 1,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        assertEquals("stronger-response", PersistentResponsePolicy.select(
                List.of(stronger, race)).orElseThrow()
                .candidateKey().value());

    }

    @Test
    public void ordinaryObjectivePlansDoNotPreemptButTerminalDefenseDoes() {
        PersistentResponsePolicy.CandidateFacts lowerCorridor = response(
                "lower-corridor", 82, "Lower Corridor",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 6, 1,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        PersistentResponsePolicy.CandidateFacts invasion = mandatoryPlan(
                "invasion-naboo", 82, "Lower Corridor", 400, "Naboo");

        assertEquals("lower-corridor", PersistentResponsePolicy.select(
                List.of(lowerCorridor, invasion)).orElseThrow()
                .candidateKey().value());

        PersistentResponsePolicy.CandidateFacts mandatoryEviction = response(
                "throne-eviction", 401,
                "Naboo: Theed Palace Throne Room",
                PersistentResponsePolicy.Mode.REINFORCE,
                PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                0, 0, 2,
                DeployTacticalPolicy.ResponseFormationRoute
                        .EXISTING_FORMATION_REINFORCEMENT,
                true);
        assertEquals("lower-corridor", PersistentResponsePolicy.select(
                List.of(lowerCorridor, mandatoryEviction)).orElseThrow()
                .candidateKey().value());

        PersistentResponsePolicy.CandidateFacts terminalDefense =
                terminalMandatoryPlan(
                        "terminal-defense", 82, "Lower Corridor",
                        403, "Terminal objective site");
        assertEquals("terminal-defense", PersistentResponsePolicy.select(
                List.of(lowerCorridor, terminalDefense)).orElseThrow()
                .candidateKey().value());
    }

    @Test
    public void ordinaryObjectiveRoleDoesNotBypassStableEqualValueOrder() {
        PersistentResponsePolicy.CandidateFacts ordinary = response(
                "a-ordinary", 117, "Carbonite Chamber",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 4, 2,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        PersistentResponsePolicy.CandidateFacts critical = response(
                "z-critical", 401, "Theed Palace Throne Room",
                PersistentResponsePolicy.Mode.REINFORCE,
                PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                2, 4, 2,
                DeployTacticalPolicy.ResponseFormationRoute
                        .EXISTING_FORMATION_REINFORCEMENT,
                false);

        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(
                        List.of(ordinary, critical)).orElseThrow();
        assertEquals("a-ordinary", selected.candidateKey().value());
        assertEquals(300, selected.totalNewBonus());

        PersistentResponsePolicy.CandidateFacts tieB = response(
                "b-key", 501, "Lane B",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 4, 2,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        PersistentResponsePolicy.CandidateFacts tieA = response(
                "a-key", 502, "Lane A",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 4, 2,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        assertEquals("a-key", PersistentResponsePolicy.select(
                List.of(tieB, tieA)).orElseThrow().candidateKey().value());
    }

    @Test
    public void equallyEffectiveResponsePrefersTheExactLowerCostPlan() {
        PersistentResponsePolicy.CandidateFacts focused = withExecution(
                response("focused", 401, "Critical lane",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole
                                .ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute
                                .V171_WAVE,
                        false),
                new PersistentResponsePolicy.ExecutionProof(
                        true, true, true, true, 4, 2));
        PersistentResponsePolicy.CandidateFacts bloated = withExecution(
                response("bloated", 401, "Critical lane",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole
                                .ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute
                                .V171_WAVE,
                        false),
                new PersistentResponsePolicy.ExecutionProof(
                        true, true, true, true, 7, 3));

        assertEquals("focused", PersistentResponsePolicy.select(
                List.of(bloated, focused)).orElseThrow()
                .candidateKey().value());

        PersistentResponsePolicy.CandidateFacts focusedCount = withExecution(
                response("z-focused-count", 401, "Critical lane",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole
                                .ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute
                                .V171_WAVE,
                        false),
                new PersistentResponsePolicy.ExecutionProof(
                        true, true, true, true, 4, 2));
        PersistentResponsePolicy.CandidateFacts bloatedCount = withExecution(
                response("a-bloated-count", 401, "Critical lane",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole
                                .ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute
                                .V171_WAVE,
                        false),
                new PersistentResponsePolicy.ExecutionProof(
                        true, true, true, true, 4, 3));
        assertEquals("z-focused-count", PersistentResponsePolicy.select(
                List.of(bloatedCount, focusedCount)).orElseThrow()
                .candidateKey().value());
    }

    @Test
    public void executionAndFormationProofAreHardEligibilityBoundaries() {
        PersistentResponsePolicy.CandidateFacts viable = response(
                "throne", 401, "Theed Palace Throne Room",
                PersistentResponsePolicy.Mode.REINFORCE,
                PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                0, 0, 2,
                DeployTacticalPolicy.ResponseFormationRoute
                        .EXISTING_FORMATION_REINFORCEMENT,
                false);
        for (PersistentResponsePolicy.ExecutionProof proof : List.of(
                new PersistentResponsePolicy.ExecutionProof(
                        false, true, true, true, 0, 1),
                new PersistentResponsePolicy.ExecutionProof(
                        true, false, true, true, 0, 1),
                new PersistentResponsePolicy.ExecutionProof(
                        true, true, false, true, 0, 1),
                new PersistentResponsePolicy.ExecutionProof(
                        true, true, true, false, 0, 1))) {
            assertTrue(PersistentResponsePolicy.select(List.of(
                    withExecution(viable, proof))).isEmpty());
        }
        assertTrue(PersistentResponsePolicy.select(List.of(
                withFormation(viable,
                        DeployTacticalPolicy.ResponseFormationRoute.NONE,
                        false))).isEmpty());
        assertTrue(PersistentResponsePolicy.select(List.of(
                withFormation(viable,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        true))).isEmpty());
    }

    @Test
    public void strategicIncomeAloneIsSilentWithoutPersistenceOrCriticality() {
        PersistentResponsePolicy.CandidateFacts maxDrainOne = response(
                "max-drain-one", 600, "Ordinary drain one",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.NONE,
                0, 0, 1,
                DeployTacticalPolicy.ResponseFormationRoute.V172_SOLO,
                false);
        PersistentResponsePolicy.CandidateFacts ordinaryDrainTwo = response(
                "ordinary-drain-two", 601, "Ordinary drain two",
                PersistentResponsePolicy.Mode.CONTEST,
                PersistentResponsePolicy.TargetRole.NONE,
                0, 0, 2,
                DeployTacticalPolicy.ResponseFormationRoute.V172_SOLO,
                false);

        assertTrue(PersistentResponsePolicy.select(
                List.of(maxDrainOne)).isEmpty());
        assertTrue(PersistentResponsePolicy.select(
                List.of(ordinaryDrainTwo)).isEmpty());
    }

    @Test
    public void v170PassesThroughWithNoBatchOneStack() {
        PersistentResponsePolicy.CandidateFacts spy = response(
                "spy", 800, "Persistent Lane",
                PersistentResponsePolicy.Mode.SPY,
                PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                2, 6, 1,
                DeployTacticalPolicy.ResponseFormationRoute.V170_SPY,
                false);

        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(spy)).orElseThrow();
        assertEquals(0, selected.totalNewBonus());
        assertTrue(PersistentResponsePolicy.scoreSelectedResponse(
                "action", selected, false,
                true, true, true).operations().isEmpty());
    }

    @Test
    public void exactResponseBandsApplyOnceAndV166SuppressesPersistentOverlap() {
        PersistentResponsePolicy.CandidateFacts critical = response(
                "critical", 401, "Theed Palace Throne Room",
                PersistentResponsePolicy.Mode.REINFORCE,
                PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                2, 6, 2,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                false);
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(critical))
                        .orElseThrow();

        PolicyResult full = PersistentResponsePolicy.scoreSelectedResponse(
                "action", selected, false, true, true, true);
        assertEquals(2, full.operations().size());
        assertEquals(PersistentResponsePolicy.PERSISTENT_RULE_ID,
                full.operations().get(0).ruleArmId().id());
        assertEquals(300.0f, full.operations().get(0).delta(), 0.0f);
        assertEquals(TraceDomainId.DEPLOY_SITING,
                full.operations().get(0).domainId());
        assertEquals(PersistentResponsePolicy.CRITICAL_RULE_ID,
                full.operations().get(1).ruleArmId().id());
        assertEquals(300.0f, full.operations().get(1).delta(), 0.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                full.operations().get(1).domainId());
        assertTrue(full.operations().stream().allMatch(operation ->
                operation.kind() == PolicyOperationKind.ADD));
        assertTrue(full.operations().stream().allMatch(operation ->
                operation.reason().contains(
                        "target=Theed Palace Throne Room#401")));

        PolicyResult v166Covered =
                PersistentResponsePolicy.scoreSelectedResponse(
                        "action", selected, true, true, true, true);
        assertEquals(1, v166Covered.operations().size());
        assertEquals(PersistentResponsePolicy.CRITICAL_RULE_ID,
                v166Covered.operations().get(0).ruleArmId().id());
        assertEquals(300.0f, v166Covered.operations().get(0).delta(), 0.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                v166Covered.operations().get(0).domainId());
        assertTrue(v166Covered.operations().get(0).reason().contains(
                "target=Theed Palace Throne Room#401"));
    }

    @Test
    public void selectedResponseActionRequiresExactPhysicalCardAndTarget() {
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(response(
                        "exact-action", 401,
                        "Theed Palace Throne Room",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        false))).orElseThrow();

        assertTrue(PersistentResponsePolicy.matchesSelectedResponseAction(
                selected, 9001, 10001, 401));
        assertFalse(PersistentResponsePolicy.matchesSelectedResponseAction(
                selected, 9002, 10001, 401));
        assertFalse(PersistentResponsePolicy.matchesSelectedResponseAction(
                selected, 9001, 10002, 401));
        assertFalse(PersistentResponsePolicy.matchesSelectedResponseAction(
                selected, 9001, 10001, 402));
    }

    @Test
    public void onlyExactOriginallyProvedWaveMembersReceiveBands() {
        PersistentResponsePolicy.CandidateFacts wave =
                withResponseActions(response(
                        "two-card-wave", 401,
                        "Theed Palace Throne Room",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        false),
                        List.of(
                                new PersistentResponsePolicy
                                        .DeployActionKey(9001, 10001),
                                new PersistentResponsePolicy
                                        .DeployActionKey(9002, 10002)));
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(wave))
                        .orElseThrow();

        PolicyResult anchored =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "anchor", selected, 9001, 10001, 401,
                        true, true, true, false, true, true, true);
        PolicyResult secondBody =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "second", selected, 9002, 10002, 401,
                        true, true, true, false, true, true, true);
        PolicyResult locationPrelude =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "location", selected, 8001, 8002, 401,
                        true, false, true, false, true, true, true);
        PolicyResult unrelatedAtSameTarget =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "unrelated", selected, 9003, 10003, 401,
                        true, true, true, false, true, true, true);

        assertEquals(2, anchored.operations().size());
        assertEquals(2, secondBody.operations().size());
        assertTrue(locationPrelude.operations().isEmpty());
        assertTrue(unrelatedAtSameTarget.operations().isEmpty());
        assertEquals(List.of(
                        new PersistentResponsePolicy.DeployActionKey(
                                9001, 10001),
                        new PersistentResponsePolicy.DeployActionKey(
                                9002, 10002)),
                selected.responseActions());
        assertEquals(600.0f, anchored.operations().stream()
                .map(operation -> operation.delta())
                .reduce(0.0f, Float::sum), 0.0f);
    }

    @Test
    public void responseBucketSelectsOnlyExactNextMemberAndAdvances() {
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(
                        withResponseActions(response(
                                "two-card-wave", 401,
                                "Theed Palace Throne Room",
                                PersistentResponsePolicy.Mode.REINFORCE,
                                PersistentResponsePolicy.TargetRole
                                        .ACTIVE_FLIP_GATE,
                                2, 6, 2,
                                DeployTacticalPolicy
                                        .ResponseFormationRoute.V171_WAVE,
                                false),
                                List.of(
                                        new PersistentResponsePolicy
                                                .DeployActionKey(
                                                9001, 10001),
                                        new PersistentResponsePolicy
                                                .DeployActionKey(
                                                9002, 10002)))))
                        .orElseThrow();
        List<Set<String>> legacyBuckets = List.of(
                Set.of("objective"), Set.of("ordinary"));
        List<String> legacyLabels = List.of("OBJECTIVE", "ORDINARY");
        List<PersistentResponsePolicy.OfferedOuterAction> shuffled =
                List.of(
                        offered("action-B", 9002, 10002,
                                false, false),
                        offered("unrelated-C", 9003, 10003,
                                false, false),
                        offered("action-A", 9001, 10001,
                                false, false));

        PersistentResponsePolicy.ResponseBucket first =
                PersistentResponsePolicy.prependResponseBucket(
                        selected, shuffled, legacyBuckets, legacyLabels)
                        .orElseThrow();
        assertEquals("action-A", first.actionId());
        assertEquals(Set.of("action-A"), first.buckets().get(0));
        assertEquals(PersistentResponsePolicy.RESPONSE_BUCKET_LABEL,
                first.labels().get(0));
        assertEquals(legacyBuckets, first.buckets().subList(
                1, first.buckets().size()));
        assertEquals(legacyLabels, first.labels().subList(
                1, first.labels().size()));

        PersistentResponsePolicy.Obligation afterA =
                selected.withRemainingResponseActions(List.of(
                        new PersistentResponsePolicy.DeployActionKey(
                                9002, 10002)));
        PersistentResponsePolicy.ResponseBucket second =
                PersistentResponsePolicy.prependResponseBucket(
                        afterA, shuffled.reversed(), legacyBuckets,
                        legacyLabels).orElseThrow();
        assertEquals("action-B", second.actionId());
        assertEquals(Set.of("action-B"), second.buckets().get(0));
    }

    @Test
    public void responseBucketFallsThroughForMissingBlockedOrAlternative() {
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(response(
                        "response", 401, "Critical lane",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        false))).orElseThrow();
        List<Set<String>> legacyBuckets = List.of(Set.of("legacy"));
        List<String> legacyLabels = List.of("LEGACY");

        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                selected,
                List.of(offered("exact", 9001, 10001,
                        false, false)),
                List.of(), List.of()).isEmpty());
        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                selected,
                List.of(new PersistentResponsePolicy.OfferedOuterAction(
                        "unselectable",
                        new PersistentResponsePolicy.DeployActionKey(
                                9001, 10001),
                        true, false, true, true, true,
                        false, false)),
                legacyBuckets, legacyLabels).isEmpty());
        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                selected,
                List.of(offered("wrong", 9002, 10002,
                        false, false)),
                legacyBuckets, legacyLabels).isEmpty());
        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                selected,
                List.of(offered("hard", 9001, 10001,
                        true, false)),
                legacyBuckets, legacyLabels).isEmpty());
        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                selected,
                List.of(offered("deferred", 9001, 10001,
                        false, true)),
                legacyBuckets, legacyLabels).isEmpty());
        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                selected,
                List.of(
                        offered("duplicate-a", 9001, 10001,
                                false, false),
                        offered("duplicate-b", 9001, 10001,
                                false, false)),
                legacyBuckets, legacyLabels).isEmpty());

        PersistentResponsePolicy.Obligation mandatoryAlternative =
                PersistentResponsePolicy.select(List.of(terminalMandatoryPlan(
                        "mandatory", 401, "Critical lane",
                        402, "Objective target"))).orElseThrow();
        assertEquals(PersistentResponsePolicy.CandidateKind.EXISTING_PLAN,
                mandatoryAlternative.kind());
        assertTrue(PersistentResponsePolicy.prependResponseBucket(
                mandatoryAlternative,
                List.of(offered("action", 9001, 10001,
                        false, false)),
                legacyBuckets, legacyLabels).isEmpty());
    }

    @Test
    public void opponentRemovalBeforeDestinationScoringDropsBothBands() {
        PersistentResponsePolicy.Obligation selected =
                PersistentResponsePolicy.select(List.of(response(
                        "stale-squatter", 401,
                        "Theed Palace Throne Room",
                        PersistentResponsePolicy.Mode.REINFORCE,
                        PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                        2, 6, 2,
                        DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                        false))).orElseThrow();

        PolicyResult stale =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "anchor", selected, 9001, 10001, 401,
                        true, true, true, false,
                        false, true, true);

        PolicyResult persistentExpired =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "anchor", selected, 9001, 10001, 401,
                        true, true, true, false,
                        true, false, true);
        PolicyResult criticalExpired =
                PersistentResponsePolicy.scoreSelectedResponseAction(
                        "anchor", selected, 9001, 10001, 401,
                        true, true, true, false,
                        true, true, false);

        assertTrue(stale.operations().isEmpty());
        assertEquals(1, persistentExpired.operations().size());
        assertEquals(PersistentResponsePolicy.CRITICAL_RULE_ID,
                persistentExpired.operations().get(0).ruleArmId().id());
        assertEquals(1, criticalExpired.operations().size());
        assertEquals(PersistentResponsePolicy.PERSISTENT_RULE_ID,
                criticalExpired.operations().get(0).ruleArmId().id());
    }

    private void finishAndClose(PersistentResponsePolicy.State ledger,
                                MutableGameView game, int turn,
                                int locationId, String title, int finalPaid) {
        finishEvent(ledger, game, turn, locationId, title, finalPaid);
        ledger.observe(game.set(null, BOT, turn), BOT);
    }

    private void finishEvent(PersistentResponsePolicy.State ledger,
                             MutableGameView game, int turn,
                             int locationId, String title, int finalPaid) {
        ForceDrainState drain = drain(locationId, title, 0);
        ledger.observe(game.set(drain, OPPONENT, turn), BOT);
        when(drain.getForcePaid()).thenReturn(finalPaid);
        ledger.observe(game.set(null, OPPONENT, turn), BOT);
    }

    private ForceDrainState drain(int locationId, String title, int paid) {
        PhysicalCard location = mock(PhysicalCard.class);
        when(location.getPermanentCardId()).thenReturn(locationId);
        when(location.getTitle()).thenReturn(title);
        ForceDrainState drain = mock(ForceDrainState.class);
        when(drain.getPlayerId()).thenReturn(OPPONENT);
        when(drain.getLocation()).thenReturn(location);
        when(drain.getForcePaid()).thenReturn(paid);
        return drain;
    }

    private static final class MutableGameView {
        private final GameState gameState = mock(GameState.class);
        private final AtomicReference<ForceDrainState> drain =
                new AtomicReference<>();
        private final AtomicReference<String> currentPlayer =
                new AtomicReference<>(BOT);
        private final AtomicInteger opponentTurn = new AtomicInteger();

        private MutableGameView() {
            when(gameState.getOpponent(BOT)).thenReturn(OPPONENT);
            when(gameState.getForceDrainState())
                    .thenAnswer(ignored -> drain.get());
            when(gameState.getCurrentPlayerId())
                    .thenAnswer(ignored -> currentPlayer.get());
            when(gameState.getPlayersLatestTurnNumber(OPPONENT))
                    .thenAnswer(ignored -> opponentTurn.get());
        }

        private GameState set(ForceDrainState currentDrain,
                              String currentPlayerId,
                              int currentOpponentTurn) {
            drain.set(currentDrain);
            currentPlayer.set(currentPlayerId);
            opponentTurn.set(currentOpponentTurn);
            return gameState;
        }
    }

    private PersistentResponsePolicy.CandidateFacts response(
            String key, int locationId, String title,
            PersistentResponsePolicy.Mode mode,
            PersistentResponsePolicy.TargetRole role,
            int turns, int damage, int strategicIncome,
            DeployTacticalPolicy.ResponseFormationRoute route,
            boolean mandatory) {
        PersistentResponsePolicy.LocationKey location =
                new PersistentResponsePolicy.LocationKey(locationId, title);
        return new PersistentResponsePolicy.CandidateFacts(
                new PersistentResponsePolicy.CandidateKey(key),
                PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET,
                List.of(new PersistentResponsePolicy.DeployActionKey(
                        9001, 10001)),
                location, location, mode, role, turns, damage,
                strategicIncome, 0, 0,
                true, mandatory, executable(),
                new PersistentResponsePolicy.FormationProof(
                        2.0f, 2.0f, 6.0f, 2.0f, route, false));
    }

    private PersistentResponsePolicy.CandidateFacts alternative(
            String key, int threatId, String threatTitle,
            int targetId, String targetTitle,
            PersistentResponsePolicy.Mode mode,
            int currentIncome, int raceValue,
            boolean mandatory) {
        return new PersistentResponsePolicy.CandidateFacts(
                new PersistentResponsePolicy.CandidateKey(key),
                PersistentResponsePolicy.CandidateKind.EXISTING_PLAN,
                List.of(),
                new PersistentResponsePolicy.LocationKey(
                        threatId, threatTitle),
                new PersistentResponsePolicy.LocationKey(
                        targetId, targetTitle),
                mode, PersistentResponsePolicy.TargetRole.NONE,
                0, 0, 0, currentIncome, raceValue,
                false, mandatory, executable(),
                new PersistentResponsePolicy.FormationProof(
                        0.0f, 0.0f, 0.0f, 0.0f,
                        DeployTacticalPolicy.ResponseFormationRoute
                                .EXISTING_LEGAL_ALTERNATIVE,
                        false));
    }

    private PersistentResponsePolicy.CandidateFacts mandatoryPlan(
            String key, int threatId, String threatTitle,
            int targetId, String targetTitle) {
        PersistentResponsePolicy.CandidateFacts plan = alternative(
                key, threatId, threatTitle, targetId, targetTitle,
                PersistentResponsePolicy.Mode.MASS_TOWARD,
                0, 0, true);
        return withFormation(plan,
                DeployTacticalPolicy.ResponseFormationRoute.NOT_APPLICABLE,
                false);
    }

    private PersistentResponsePolicy.CandidateFacts terminalMandatoryPlan(
            String key, int threatId, String threatTitle,
            int targetId, String targetTitle) {
        PersistentResponsePolicy.CandidateFacts plan = mandatoryPlan(
                key, threatId, threatTitle, targetId, targetTitle);
        return new PersistentResponsePolicy.CandidateFacts(
                plan.candidateKey(), plan.kind(), plan.responseActions(),
                plan.threatLocation(), plan.responseTargetLocation(),
                plan.mode(),
                PersistentResponsePolicy.TargetRole
                        .OBJECTIVE_HARD_LOSS_DEFENSE,
                plan.consecutiveOpponentTurns(),
                plan.projectedAvoidedDamage(), plan.strategicIncome(),
                plan.currentFriendlyDrainIncome(), plan.raceValue(),
                plan.opponentPresent(), plan.fundedMandatoryObjective(),
                plan.execution(), plan.formation());
    }

    private PersistentResponsePolicy.CandidateFacts withExecution(
            PersistentResponsePolicy.CandidateFacts candidate,
            PersistentResponsePolicy.ExecutionProof execution) {
        return copy(candidate, execution, candidate.formation());
    }

    private PersistentResponsePolicy.CandidateFacts withResponseActions(
            PersistentResponsePolicy.CandidateFacts candidate,
            List<PersistentResponsePolicy.DeployActionKey> actions) {
        return new PersistentResponsePolicy.CandidateFacts(
                candidate.candidateKey(), candidate.kind(), actions,
                candidate.threatLocation(),
                candidate.responseTargetLocation(), candidate.mode(),
                candidate.role(), candidate.consecutiveOpponentTurns(),
                candidate.projectedAvoidedDamage(),
                candidate.strategicIncome(),
                candidate.currentFriendlyDrainIncome(),
                candidate.raceValue(),
                candidate.opponentPresent(),
                candidate.fundedMandatoryObjective(),
                candidate.execution(), candidate.formation());
    }

    private PersistentResponsePolicy.CandidateFacts withFormation(
            PersistentResponsePolicy.CandidateFacts candidate,
            DeployTacticalPolicy.ResponseFormationRoute route,
            boolean alreadyWinning) {
        PersistentResponsePolicy.FormationProof old = candidate.formation();
        return copy(candidate, candidate.execution(),
                new PersistentResponsePolicy.FormationProof(
                        old.existingFriendlyPower(),
                        old.existingFriendlyAbility(),
                        old.plannedWavePower(), old.plannedWaveAbility(),
                        route, alreadyWinning));
    }

    private PersistentResponsePolicy.CandidateFacts copy(
            PersistentResponsePolicy.CandidateFacts candidate,
            PersistentResponsePolicy.ExecutionProof execution,
            PersistentResponsePolicy.FormationProof formation) {
        return new PersistentResponsePolicy.CandidateFacts(
                candidate.candidateKey(), candidate.kind(),
                candidate.responseActions(),
                candidate.threatLocation(),
                candidate.responseTargetLocation(), candidate.mode(),
                candidate.role(), candidate.consecutiveOpponentTurns(),
                candidate.projectedAvoidedDamage(),
                candidate.strategicIncome(),
                candidate.currentFriendlyDrainIncome(),
                candidate.raceValue(),
                candidate.opponentPresent(),
                candidate.fundedMandatoryObjective(), execution, formation);
    }

    private PersistentResponsePolicy.ExecutionProof executable() {
        return new PersistentResponsePolicy.ExecutionProof(
                true, true, true, true, 0, 1);
    }

    private PersistentResponsePolicy.OfferedOuterAction offered(
            String actionId, int permanentId, int currentId,
            boolean hardVetoed, boolean deferred) {
        return new PersistentResponsePolicy.OfferedOuterAction(
                actionId,
                new PersistentResponsePolicy.DeployActionKey(
                        permanentId, currentId),
                true, true, true, true, true,
                hardVetoed, deferred);
    }
}
