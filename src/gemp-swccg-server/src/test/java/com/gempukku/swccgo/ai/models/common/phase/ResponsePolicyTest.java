package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

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
}
