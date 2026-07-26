package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.timing.Action;

import java.lang.reflect.Method;

/**
 * Reads the selected engine action's true source without changing the engine
 * decision wire contract. The public cardId parameter identifies the card the
 * action is attached to, which can differ from {@link Action#getActionSource()}
 * for deploy actions. Rule actions deliberately have no card source, so their
 * associated card is the source-equivalent fallback.
 */
public final class AiActionSourceProvenance {
    private AiActionSourceProvenance() {
    }

    public static PhysicalCard selectedActionSource(
            AwaitingDecision decision,
            String selectedActionId) {
        Action action = actionForId(
                decision, selectedActionId);
        if (action == null) {
            return null;
        }
        PhysicalCard source = action.getActionSource();
        return source != null
                ? source
                : action.getActionAttachedToCard();
    }

    public static Action actionForId(
            AwaitingDecision decision,
            String actionId) {
        if (!(decision
                instanceof CardActionSelectionDecision)
                || actionId == null
                || actionId.isBlank()) {
            return null;
        }
        try {
            Method getSelectedAction =
                    CardActionSelectionDecision.class
                        .getDeclaredMethod(
                            "getSelectedAction",
                            String.class);
            getSelectedAction.setAccessible(true);
            return (Action) getSelectedAction.invoke(
                    decision, actionId);
        } catch (ReflectiveOperationException
                | RuntimeException ignored) {
            return null;
        }
    }
}
