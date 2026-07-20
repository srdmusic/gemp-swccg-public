package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
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
