package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponsePolicyTest {
    @Test
    public void recognizedRoutesPreserveLegacyPrecedence() {
        assertEquals(ResponsePolicy.Route.OPTIONAL_FORFEIT,
                ResponsePolicy.classify(
                        "CARD_SELECTION",
                        "Choose a card to forfeit, if desired"));
        assertEquals(ResponsePolicy.Route.REVERT_APPROVAL,
                ResponsePolicy.classify(
                        "MULTIPLE_CHOICE",
                        "Will you allow the opponent to revert?"));
        assertEquals(ResponsePolicy.Route.UNDERCOVER_SPY,
                ResponsePolicy.classify(
                        "MULTIPLE_CHOICE",
                        "Deploy Jyn Erso as an Undercover spy?"));
        assertEquals(ResponsePolicy.Route.OPTIONAL_FORFEIT,
                ResponsePolicy.classify(
                        "MULTIPLE_CHOICE",
                        "Revert optional forfeit, if desired"));
        assertEquals(ResponsePolicy.Route.REVERT_APPROVAL,
                ResponsePolicy.classify(
                        "MULTIPLE_CHOICE",
                        "Revert deployment as an Undercover spy?"));
    }

    @Test
    public void unknownAndWrongTypeShapesStayLegacy() {
        assertEquals(ResponsePolicy.Route.LEGACY,
                ResponsePolicy.classify(
                        "ACTION_CHOICE", "Allow revert?"));
        assertEquals(ResponsePolicy.Route.LEGACY,
                ResponsePolicy.classify(
                        "ACTION_CHOICE", "Deploy as an Undercover spy?"));
        assertEquals(ResponsePolicy.Route.LEGACY,
                ResponsePolicy.classify("MULTIPLE_CHOICE", null));
    }

    @Test
    public void revertApprovalPreservesFirstPositiveAndFallback() {
        ResponsePolicy.IndexedChoice allow =
                ResponsePolicy.revertApproval(
                        new String[]{"No", "Allow revert", "Accept"});
        assertEquals(1, allow.index());
        assertEquals("Allow revert", allow.label());

        ResponsePolicy.IndexedChoice fallback =
                ResponsePolicy.revertApproval(new String[]{"No", "Decline"});
        assertEquals(0, fallback.index());
        assertEquals("(default index 0)", fallback.label());

        assertEquals(0, ResponsePolicy.revertApproval(null).index());
    }

    @Test
    public void yesNoIndexesPreserveReorderedAndMissingDefaults() {
        ResponsePolicy.YesNoIndexes reordered =
                ResponsePolicy.yesNoIndexes(new String[]{"No", "Yes"});
        assertEquals(1, reordered.yesIndex());
        assertEquals(0, reordered.noIndex());
        assertEquals(1, reordered.choose(true));
        assertEquals(0, reordered.choose(false));

        ResponsePolicy.YesNoIndexes fallback =
                ResponsePolicy.yesNoIndexes(new String[]{"Maybe"});
        assertEquals(0, fallback.yesIndex());
        assertEquals(1, fallback.noIndex());
    }

    @Test
    public void undercoverDrainBoundaryIsUnchanged() {
        assertFalse(ResponsePolicy.shouldDeployUndercover(-1));
        assertFalse(ResponsePolicy.shouldDeployUndercover(0));
        assertTrue(ResponsePolicy.shouldDeployUndercover(1));
        assertTrue(ResponsePolicy.shouldDeployUndercover(6));
    }

    @Test
    public void fallbackPriorityScoresRetainExactLegacyValues() {
        assertEquals(100, ResponsePolicy.scorePriorityCards(
                "play houjix", "cancel battle damage"));
        assertEquals(100, ResponsePolicy.scorePriorityCards(
                "play ghhhk", "cancel"));
        assertEquals(80, ResponsePolicy.scorePriorityCards(
                "play imperial barrier", "character deploy response"));
        assertEquals(35, ResponsePolicy.scorePriorityCards(
                "play sense to cancel", "routine response"));
        assertEquals(90, ResponsePolicy.scorePriorityCards(
                "play sense to cancel", "Cancel Jedi Levitation"));
        assertEquals(270, ResponsePolicy.scorePriorityCards(
                "houjix barrier sense cancel",
                "cancel battle damage from Jedi Levitation character deploy"));
        assertEquals(0, ResponsePolicy.scorePriorityCards(
                "play interrupt", "routine response"));
    }

    @Test
    public void fixedActionTextPolicyMatrixPreservesExactOperations() {
        List<ExpectedOperation> matrix = List.of(
                expected(ResponsePolicy.scoreWhenDeployedFreeTrigger(
                                "free", "retrieve Force from Lost Pile"),
                        "free", "V184-when-deployed-trigger", 300.0f,
                        "V184 WHEN-DEPLOYED TRIGGER: free value (retrieve Force from Lost Pile) — fire it, don't pass"),
                expected(ResponsePolicy.scoreSenseRedraw(
                                "redraw-hand", true, false),
                        "redraw-hand", "V29.8-sense-redraw-hand", -600.0f,
                        "V29.8 SENSE REDRAW BLOCKED: NEVER redraw hand — save Sense for canceling opponent interrupts! Costs 3 Force AND helps opponent!"),
                expected(ResponsePolicy.scoreSenseRedraw(
                                "mutual-redraw", false, true),
                        "mutual-redraw", "V29.8-sense-mutual-redraw", -600.0f,
                        "V29.8 SENSE UNCERTAIN BLOCKED: Don't make both players redraw — helps opponent!"),
                expected(ResponsePolicy.scoreSaveJedi("save-jedi"),
                        "save-jedi", "V53b-save-jedi", 500.0f,
                        "V53b SAVE JEDI: Stack Jedi on Fallen Order — lose 1 force to save them!"),
                expected(ResponsePolicy.scoreReact("react"),
                        "react", "RESPONSE-react", -30.0f,
                        "Avoid reacts (bot doesn't understand timing)"),
                expected(ResponsePolicy.scoreCancelOwn("cancel-own"),
                        "cancel-own", "RESPONSE-cancel-own", -50.0f,
                        "Never cancel own cards"),
                expected(ResponsePolicy.scoreRemainingBattleDamageCancel(
                                "damage-cancel"),
                        "damage-cancel", "RESPONSE-houjix-ghhhk", 30.0f,
                        "Cancel battle damage - valuable survival card"));

        for (ExpectedOperation expected : matrix) {
            assertEquals("RESPONSE_ACTION_TEXT_POLICY",
                    expected.result().producerId());
            assertEquals(1, expected.result().operations().size());
            assertOperation(expected.result().operations().get(0), expected);
        }
    }

    @Test
    public void senseRedrawContributionsRemainIndependentOrderedAndAdditive() {
        PolicyResult both = ResponsePolicy.scoreSenseRedraw(
                "sense", true, true);

        assertEquals(2, both.operations().size());
        assertOperation(both.operations().get(0), new ExpectedOperation(
                both, "sense", "V29.8-sense-redraw-hand", -600.0f,
                "V29.8 SENSE REDRAW BLOCKED: NEVER redraw hand — save Sense for canceling opponent interrupts! Costs 3 Force AND helps opponent!"));
        assertOperation(both.operations().get(1), new ExpectedOperation(
                both, "sense", "V29.8-sense-mutual-redraw", -600.0f,
                "V29.8 SENSE UNCERTAIN BLOCKED: Don't make both players redraw — helps opponent!"));
        assertEquals(-1200.0f,
                both.operations().get(0).delta()
                        + both.operations().get(1).delta(),
                0.0f);
        assertTrue(ResponsePolicy.scoreSenseRedraw(
                "sense", false, false).operations().isEmpty());
    }

    @Test
    public void senseCancelBoundariesPreserveExactLegacyBandsAndReasons() {
        List<ExpectedOperation> matrix = List.of(
                expected(ResponsePolicy.scoreSenseSelfCancel("self"),
                        "self", "V37.3-sense-self-cancel", -9999.0f,
                        "V37.3 SENSE SELF-CANCEL: NEVER cancel our OWN interrupt!"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "destiny-80", true, true, 80,
                                "barrier", false, true).result(),
                        "destiny-80", "RESPONSE-sense-destiny-critical", 10.0f,
                        "Destiny cancel critical target: barrier"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "destiny-79", true, true, 79,
                                "near", false, false).result(),
                        "destiny-79", "RESPONSE-sense-destiny-skip", -10.0f,
                        "Destiny-based cancel (unreliable, skip)"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "critical-80", false, true, 80,
                                "barrier", false, true).result(),
                        "critical-80", "RESPONSE-sense-critical", 70.0f,
                        "Cancel CRITICAL target: barrier!"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "high-79", false, true, 79,
                                "near", false, true).result(),
                        "high-79", "RESPONSE-sense-high-value", 50.0f,
                        "Cancel high-value target: near"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "high-60", false, true, 60,
                                "nabrun leids", false, false).result(),
                        "high-60", "RESPONSE-sense-high-value", 50.0f,
                        "Cancel high-value target: nabrun leids"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "valuable-59", false, true, 59,
                                "alter", false, false).result(),
                        "valuable-59", "RESPONSE-sense-valuable", 45.0f,
                        "Cancel valuable target: alter"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "drain-opponent", false, false, 100,
                                "ignored", true, false).result(),
                        "drain-opponent", "RESPONSE-sense-force-drain-opponent", 35.0f,
                        "Cancel opponent's force drain"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "their-turn", false, false, 0,
                                "", false, false).result(),
                        "their-turn", "RESPONSE-sense-opponent-turn", 30.0f,
                        "Cancel opponent interrupt (their turn)"),
                expected(ResponsePolicy.scoreSenseCancel(
                                "our-turn", false, false, 0,
                                "", false, true).result(),
                        "our-turn", "RESPONSE-sense-own-turn", 15.0f,
                        "Cancel opponent interrupt (our turn)"));

        for (ExpectedOperation expected : matrix) {
            assertEquals("RESPONSE_ACTION_TEXT_POLICY",
                    expected.result().producerId());
            assertEquals(1, expected.result().operations().size());
            assertOperation(expected.result().operations().get(0), expected);
        }
    }

    @Test
    public void senseCancelPrecedenceAndDrainDelegationStayExact() {
        ResponsePolicy.CancelEvaluation ownDrain =
                ResponsePolicy.scoreSenseCancel(
                        "own-drain", false, false, 0, "", true, true);
        assertTrue(ownDrain.delegatesSelfCancelDrain());
        assertTrue(ownDrain.result().operations().isEmpty());

        ResponsePolicy.CancelEvaluation destinyOwnDrain =
                ResponsePolicy.scoreSenseCancel(
                        "destiny-drain", true, true, 80,
                        "barrier", true, true);
        assertFalse(destinyOwnDrain.delegatesSelfCancelDrain());
        assertEquals(10.0f,
                destinyOwnDrain.result().operations().get(0).delta(), 0.0f);

        ResponsePolicy.CancelEvaluation valuableOwnDrain =
                ResponsePolicy.scoreSenseCancel(
                        "valuable-drain", false, true, 60,
                        "nabrun leids", true, true);
        assertFalse(valuableOwnDrain.delegatesSelfCancelDrain());
        assertEquals(50.0f,
                valuableOwnDrain.result().operations().get(0).delta(), 0.0f);

        ResponsePolicy.CancelEvaluation lateOwn =
                ResponsePolicy.scoreLateForceDrainCancel("late-own", true);
        assertTrue(lateOwn.delegatesSelfCancelDrain());
        assertTrue(lateOwn.result().operations().isEmpty());

        ResponsePolicy.CancelEvaluation lateOpponent =
                ResponsePolicy.scoreLateForceDrainCancel("late-opponent", false);
        assertFalse(lateOpponent.delegatesSelfCancelDrain());
        assertOperation(lateOpponent.result().operations().get(0),
                expected(lateOpponent.result(), "late-opponent",
                        "RESPONSE-late-force-drain-opponent", 30.0f,
                        "Cancel opponent's force drain"));
    }

    @Test
    public void barrierBandsPreserveExactOrderReasonsAndRememberSignal() {
        List<ResponsePolicy.BarrierEvaluation> matrix = List.of(
                ResponsePolicy.scoreBarrier("already", "Vader", true, false,
                        true, true, 5.0f, 3.0f, 3.0f),
                ResponsePolicy.scoreBarrier("own", "Vader", false, true,
                        true, true, 5.0f, 3.0f, 3.0f),
                ResponsePolicy.scoreBarrier("none", "Vader", false, false,
                        false, true, 5.0f, 3.0f, 3.0f),
                ResponsePolicy.scoreBarrier("quiet", "Vader", false, false,
                        true, false, 5.0f, 3.0f, 3.0f),
                ResponsePolicy.scoreBarrier("dominant", "Vader", false, false,
                        true, true, 5.0f, 12.0f, 4.0f),
                ResponsePolicy.scoreBarrier("high", "Vader", false, false,
                        true, true, 5.0f, 4.0f, 5.0f),
                ResponsePolicy.scoreBarrier("behind", "Vader", false, false,
                        true, true, 4.0f, 4.0f, 4.0f),
                ResponsePolicy.scoreBarrier("ahead", "Vader", false, false,
                        true, true, 4.0f, 5.0f, 4.0f));
        List<ExpectedOperation> expected = List.of(
                expected(matrix.get(0).result(), "already", "RESPONSE-barrier-already", -50.0f,
                        "Already barriered Vader this turn - wasteful!"),
                expected(matrix.get(1).result(), "own", "RESPONSE-barrier-own", -9999.0f,
                        "V35.1 SELF-BARRIER BLOCK: Vader is OUR character — NEVER prevent our own from battling!"),
                expected(matrix.get(2).result(), "none", "RESPONSE-barrier-no-presence", -9999.0f,
                        "V48 BARRIER USELESS: No friendly presence at location — serves no purpose!"),
                expected(matrix.get(3).result(), "quiet", "RESPONSE-barrier-not-contested", -30.0f,
                        "Save barrier - location not contested"),
                expected(matrix.get(4).result(), "dominant", "RESPONSE-barrier-dominating", -30.0f,
                        "Save barrier - already dominating (12 vs 4)"),
                expected(matrix.get(5).result(), "high", "RESPONSE-barrier-high-target", 50.0f,
                        "Barrier on HIGH POWER target (5)!"),
                expected(matrix.get(6).result(), "behind", "RESPONSE-barrier-protect", 40.0f,
                        "Barrier to protect (losing 4 vs 4)"),
                expected(matrix.get(7).result(), "ahead", "RESPONSE-barrier-contested", 30.0f,
                        "Barrier at contested location"));

        for (int i = 0; i < matrix.size(); i++) {
            assertOperation(matrix.get(i).result().operations().get(0), expected.get(i));
        }
        assertTrue(matrix.get(0).terminal());
        assertTrue(matrix.get(1).terminal());
        assertFalse(matrix.get(2).rememberTarget());
        assertTrue(matrix.get(5).rememberTarget());
        assertTrue(matrix.get(6).rememberTarget());
        assertTrue(matrix.get(7).rememberTarget());
    }

    @Test
    public void grabAndCancelSelectionPolicyPreserveExactOutcomes() {
        ResponsePolicy.GrabEvaluation confirmedBoth = ResponsePolicy.scoreGrab(
                "both", true, true, Side.DARK, false, false, true);
        ResponsePolicy.GrabEvaluation confirmedOwn = ResponsePolicy.scoreGrab(
                "own", true, false, Side.DARK, false, false, true);
        ResponsePolicy.GrabEvaluation namedOwn = ResponsePolicy.scoreGrab(
                "named-own", false, false, Side.LIGHT, true, false, true);
        ResponsePolicy.GrabEvaluation unknownOpponent = ResponsePolicy.scoreGrab(
                "unknown-opponent", false, false, Side.DARK, false, false, false);
        ResponsePolicy.GrabEvaluation unknownOwn = ResponsePolicy.scoreGrab(
                "unknown-own", false, false, Side.DARK, false, false, true);

        assertOperation(confirmedBoth.result().operations().get(0),
                expected(confirmedBoth.result(), "both", "RESPONSE-grab-confirmed-opponent", 30.0f,
                        "V53 GRAB OPPONENT: Confirmed opponent's interrupt — grab it!"));
        assertTrue(confirmedBoth.terminal());
        assertFalse(confirmedBoth.setScoreBeforeAdd());
        assertOperation(confirmedOwn.result().operations().get(0),
                expected(confirmedOwn.result(), "own", "RESPONSE-grab-confirmed-own", -9999.0f,
                        "V53 NEVER GRAB OWN: Grabbing own interrupt is suicide!"));
        assertTrue(confirmedOwn.terminal());
        assertTrue(confirmedOwn.setScoreBeforeAdd());
        assertTrue(namedOwn.setScoreBeforeAdd());
        assertEquals(ResponsePolicy.GrabOutcome.NAME_OWN_LIGHT, namedOwn.outcome());
        assertEquals(30.0f, unknownOpponent.result().operations().get(0).delta(), 0.0f);
        assertEquals(-200.0f, unknownOwn.result().operations().get(0).delta(), 0.0f);

        assertTrue(ResponsePolicy.scoreCancelSelection("unresolved", false, false)
                .operations().isEmpty());
        assertOperation(ResponsePolicy.scoreCancelSelection("opponent", true, true)
                        .operations().get(0),
                expected(ResponsePolicy.scoreCancelSelection("opponent", true, true),
                        "opponent", "RESPONSE-cancel-selection-opponent", 100.0f,
                        "Opponent's card - cancel!"));
        assertOperation(ResponsePolicy.scoreCancelSelection("own-cancel", true, false)
                        .operations().get(0),
                expected(ResponsePolicy.scoreCancelSelection("own-cancel", true, false),
                        "own-cancel", "RESPONSE-cancel-selection-own", -200.0f,
                        "Our card - don't cancel!"));
    }

    @Test(expected = NullPointerException.class)
    public void legacyPriorityScoringStillFailsOnNullActionText() {
        ResponsePolicy.scorePriorityCards(null, "routine response");
    }

    private static ExpectedOperation expected(
            PolicyResult result,
            String actionId,
            String ruleId,
            float delta,
            String reason) {
        return new ExpectedOperation(
                result, actionId, ruleId, delta, reason);
    }

    private static void assertOperation(
            PolicyOperation operation,
            ExpectedOperation expected) {
        assertEquals(expected.actionId(), operation.actionId());
        assertEquals(expected.ruleId(), operation.ruleArmId().id());
        assertEquals(TraceDomainId.RESPONSE_ROUTING, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(expected.delta(), operation.delta(), 0.0f);
        assertEquals(expected.reason(), operation.reason());
    }

    private record ExpectedOperation(
            PolicyResult result,
            String actionId,
            String ruleId,
            float delta,
            String reason) {
    }
}
