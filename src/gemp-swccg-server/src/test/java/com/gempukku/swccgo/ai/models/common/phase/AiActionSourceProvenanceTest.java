package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.timing.Action;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AiActionSourceProvenanceTest {

    @Test
    public void readsTrueSourceInsteadOfAttachedDeployCandidate() {
        PhysicalCard actionSource =
                mock(PhysicalCard.class);
        PhysicalCard attachedCandidate =
                mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(attachedCandidate.getBlueprint())
                .thenReturn(blueprint);
        when(attachedCandidate.getCardId())
                .thenReturn(17);
        when(attachedCandidate.getTestingText(
                any(), anyBoolean(), anyBoolean()))
                .thenReturn("attached candidate");

        Action action = mock(Action.class);
        when(action.getActionAttachedToCard())
                .thenReturn(attachedCandidate);
        when(action.getActionSource())
                .thenReturn(actionSource);
        when(action.getText()).thenReturn("Deploy candidate");

        CardActionSelectionDecision decision =
                decision(action);

        assertSame(
                actionSource,
                AiActionSourceProvenance
                    .selectedActionSource(
                        decision, "0"));
        assertNull(
                AiActionSourceProvenance
                    .selectedActionSource(
                        decision, "1"));
    }

    @Test
    public void ruleActionFallsBackToAssociatedMover() {
        PhysicalCard mover = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(mover.getBlueprint()).thenReturn(blueprint);
        when(mover.getCardId()).thenReturn(19);
        when(mover.getTestingText(
                any(), anyBoolean(), anyBoolean()))
                .thenReturn("associated mover");

        Action action = mock(Action.class);
        when(action.getActionAttachedToCard())
                .thenReturn(mover);
        when(action.getActionSource()).thenReturn(null);
        when(action.getText())
                .thenReturn("Move using landspeed");

        assertSame(
                mover,
                AiActionSourceProvenance
                    .selectedActionSource(
                        decision(action), "0"));
    }

    @Test
    public void unsupportedDecisionFailsClosed() {
        assertNull(
                AiActionSourceProvenance
                    .selectedActionSource(
                        mock(com.gempukku.swccgo.logic
                            .decisions.AwaitingDecision.class),
                        "0"));
    }

    private static CardActionSelectionDecision decision(
            Action action) {
        return new CardActionSelectionDecision(
                1, "Choose action",
                List.of(action),
                true, false,
                false, false, false) {
            @Override
            public void decisionMade(String result)
                    throws DecisionResultInvalidException {
            }
        };
    }
}
