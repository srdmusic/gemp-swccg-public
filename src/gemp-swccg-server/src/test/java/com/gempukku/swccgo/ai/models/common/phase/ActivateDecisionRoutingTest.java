package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActivateDecisionRoutingTest {

    @Test
    public void exactTopLevelSelectionIsRequired() {
        String[] ids = {"pass", "activate"};
        String[] texts = {"Pass", "Activate Force"};

        assertTrue(ActivateDecisionRouting.selectedTopLevelActivate(
            Phase.ACTIVATE, "CARD_ACTION_CHOICE", ids, texts, "activate"));
        assertFalse(ActivateDecisionRouting.selectedTopLevelActivate(
            Phase.DEPLOY, "CARD_ACTION_CHOICE", ids, texts, "activate"));
        assertFalse(ActivateDecisionRouting.selectedTopLevelActivate(
            Phase.ACTIVATE, "CARD_ACTION_CHOICE", ids,
            new String[]{"Pass", "Draw destiny to activate Force"}, "activate"));
        assertFalse(ActivateDecisionRouting.selectedTopLevelActivate(
            Phase.ACTIVATE, "CARD_ACTION_CHOICE",
            new String[]{"activate", "activate"}, texts, "activate"));
    }

    @Test
    public void amountLatchOwnsOnlyTheImmediateMatchingInteger() {
        Object game = new Object();
        ActivateDecisionRouting.AmountLatch latch = new ActivateDecisionRouting.AmountLatch();
        latch.arm(game, "rando", 4, Phase.ACTIVATE);

        assertTrue(latch.consume(game, "rando", 4, Phase.ACTIVATE, "INTEGER"));
        assertFalse(latch.consume(game, "rando", 4, Phase.ACTIVATE, "INTEGER"));

        latch.arm(game, "rando", 4, Phase.ACTIVATE);
        assertFalse(latch.consume(game, "rando", 4, Phase.ACTIVATE, "MULTIPLE_CHOICE"));
        assertFalse(latch.consume(game, "rando", 4, Phase.ACTIVATE, "INTEGER"));
    }

    @Test
    public void amountLatchFailsClosedOnIdentityOrPhaseDrift() {
        Object game = new Object();
        ActivateDecisionRouting.AmountLatch latch = new ActivateDecisionRouting.AmountLatch();

        latch.arm(null, "rando", 4, Phase.ACTIVATE);
        assertFalse(latch.consume(null, "rando", 4, Phase.ACTIVATE, "INTEGER"));
        latch.arm(game, "rando", 4, Phase.ACTIVATE);
        assertFalse(latch.consume(new Object(), "rando", 4, Phase.ACTIVATE, "INTEGER"));
        latch.arm(game, "rando", 4, Phase.ACTIVATE);
        assertFalse(latch.consume(game, "rando", 5, Phase.ACTIVATE, "INTEGER"));
        latch.arm(game, "rando", 4, Phase.ACTIVATE);
        assertFalse(latch.consume(game, "rando", 4, Phase.CONTROL, "INTEGER"));
    }

    @Test
    public void exactOpponentAllowancePromptIsRecognized() {
        assertTrue(ActivateDecisionRouting.isOpponentAllowancePrompt(
            ActivateDecisionRouting.OPPONENT_ALLOWANCE_PROMPT));
        assertFalse(ActivateDecisionRouting.isOpponentAllowancePrompt(
            "Choose amount of Force to activate"));
        assertFalse(ActivateDecisionRouting.isOpponentAllowancePrompt(
            "Allow opponent to activate Force for a card effect"));
    }

    @Test
    public void zeroConfirmationUsesOnlyUnambiguousYesNoLabels() {
        ActivateDecisionRouting.ChoiceLabels choices =
            ActivateDecisionRouting.zeroConfirmationChoices(
                "MULTIPLE_CHOICE", ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT,
                new String[]{"No", "Yes"});

        assertTrue(choices.isPresent());
        assertEquals(List.of("0", "1"), choices.actionIds());
        assertEquals(List.of("No", "Yes"), choices.actionTexts());
        assertFalse(ActivateDecisionRouting.zeroConfirmationChoices(
            "MULTIPLE_CHOICE", ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT,
            new String[]{"Yes", "Yes", "No"}).isPresent());
        assertFalse(ActivateDecisionRouting.zeroConfirmationChoices(
            "MULTIPLE_CHOICE", ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT,
            new String[]{"Yes", "No", "Maybe"}).isPresent());
        assertFalse(ActivateDecisionRouting.zeroConfirmationChoices(
            "MULTIPLE_CHOICE", "Do you want to Pass?",
            new String[]{"Yes", "No"}).isPresent());
    }
}
