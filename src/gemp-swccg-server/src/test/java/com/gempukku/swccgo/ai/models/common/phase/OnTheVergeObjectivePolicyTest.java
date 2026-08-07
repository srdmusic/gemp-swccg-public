package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OnTheVergeObjectivePolicyTest {

    @Test
    public void exactScarifRouteAndCommandCenterOwnTheirBands() {
        var parent = OnTheVergeObjectivePolicy
                .scoreScarifBattlegroundRoute(
                    "parent", true, true);
        var command = OnTheVergeObjectivePolicy
                .scoreScarifBattlegroundCandidate(
                    "command", true, true);
        var beach = OnTheVergeObjectivePolicy
                .scoreScarifBattlegroundCandidate(
                    "beach", true, false);

        assertEquals(2000.0f,
                parent.operations().getFirst().delta(), 0.0f);
        assertEquals(1200.0f,
                command.operations().getFirst().delta(), 0.0f);
        assertTrue(beach.operations().isEmpty());
        assertTrue(OnTheVergeObjectivePolicy
                .scoreScarifBattlegroundRoute(
                    "other", false, true)
                .operations().isEmpty());
    }

    @Test
    public void exactThreeForceKrennicRoutePreservesMovement() {
        var blocked = OnTheVergeObjectivePolicy
                .scoreKrennicRoute(
                    "krennic", true, true,
                    3, 3, 1);
        assertEquals(PolicyOperationKind.HARD_VETO,
                blocked.operations().getFirst().kind());
        assertEquals("OBJECTIVE.OTVOG.KRENNIC_MOVE_RESERVE",
                blocked.operations().getFirst().ruleArmId().id());
    }

    @Test
    public void fourAndFiveForceKrennicRoutesRemainExecutable() {
        for (int force : new int[]{4, 5}) {
            var allowed = OnTheVergeObjectivePolicy
                    .scoreKrennicRoute(
                        "krennic-" + force,
                        true, true, force, 3, 1);
            assertEquals(2000.0f,
                    allowed.operations().getFirst().delta(), 0.0f);
            assertEquals("OBJECTIVE.OTVOG.KRENNIC_ROUTE",
                    allowed.operations().getFirst().ruleArmId().id());
        }
    }

    @Test
    public void orbitCompletionReleasesTheMoveReserve() {
        var allowed = OnTheVergeObjectivePolicy
                .scoreKrennicRoute(
                    "krennic", true, true,
                    3, 3, 0);
        assertEquals(2000.0f,
                allowed.operations().getFirst().delta(), 0.0f);
    }

    @Test
    public void missingCandidateAndUnknownBudgetFactsFailClosed() {
        assertEquals(PolicyOperationKind.HARD_VETO,
                OnTheVergeObjectivePolicy.scoreKrennicRoute(
                    "missing", true, false,
                    5, 3, 1)
                    .operations().getFirst().kind());
        assertEquals(PolicyOperationKind.HARD_VETO,
                OnTheVergeObjectivePolicy.scoreKrennicRoute(
                    "unknown", true, true,
                    5, null, 1)
                    .operations().getFirst().kind());
        assertEquals("OBJECTIVE.OTVOG.FORCE_UNKNOWN",
                OnTheVergeObjectivePolicy.scoreKrennicRoute(
                    "unknown-force", true, true,
                    null, 3, 1)
                    .operations().getFirst().ruleArmId().id());
        assertEquals("OBJECTIVE.OTVOG.MOVE_COST_UNKNOWN",
                OnTheVergeObjectivePolicy.scoreKrennicRoute(
                    "unknown-move", true, true,
                    5, 3, null)
                    .operations().getFirst().ruleArmId().id());
        assertEquals("OBJECTIVE.OTVOG.KRENNIC_UNAFFORDABLE",
                OnTheVergeObjectivePolicy.scoreKrennicRoute(
                    "unaffordable", true, true,
                    2, 3, 0)
                    .operations().getFirst().ruleArmId().id());
    }

    @Test
    public void exactKrennicChildRetrievalAndVaderReactionOwnBands() {
        assertEquals(1200.0f,
                OnTheVergeObjectivePolicy
                    .scoreKrennicCandidate(
                        "candidate", true, true)
                    .operations().getFirst().delta(), 0.0f);
        assertEquals(2000.0f,
                OnTheVergeObjectivePolicy
                    .scoreBackRetrieval(
                        "retrieve", true, true)
                    .operations().getFirst().delta(), 0.0f);
        assertEquals(2000.0f,
                OnTheVergeObjectivePolicy
                    .scoreVaderBattleReaction(
                        "vader", true, true)
                    .operations().getFirst().delta(), 0.0f);
        assertEquals(1200.0f,
                OnTheVergeObjectivePolicy
                    .scoreVaderBattleReactionCandidate(
                        "vader-target", true, true)
                    .operations().getFirst().delta(), 0.0f);
        assertEquals("OBJECTIVE.OTVOG.VADER_REACTION_BREAKS_HOLD",
                OnTheVergeObjectivePolicy
                    .scoreVaderBattleReaction(
                        "unsafe-vader", true, false)
                    .operations().getFirst().ruleArmId().id());
        assertEquals("OBJECTIVE.OTVOG.VADER_CANDIDATE_BREAKS_HOLD",
                OnTheVergeObjectivePolicy
                    .scoreVaderBattleReactionCandidate(
                        "unsafe-vader-target", true, false)
                    .operations().getFirst().ruleArmId().id());
    }
}
