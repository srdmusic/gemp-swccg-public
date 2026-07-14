package com.gempukku.swccgo.cards.actions;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.PullDeployRef;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.communication.UserFeedback;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.AbstractPlayCardAction;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.timing.Effect;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** PULL metadata handoff through a multi-option play action. */
public class PlayChoiceActionPullMetadataTest {
    private static final long TRANSACTION_ID = 9001L;

    @Test
    public void acceptedPlayChoiceReceivesTheSameImmutablePullReference() throws Exception {
        PhysicalCard source = card(1001, 101, Zone.AT_LOCATION);
        PhysicalCard cardToPlay = card(2002, 902, Zone.RESERVE_DECK);
        RecordingPlayAction first = new RecordingPlayAction(cardToPlay, source, "First option");
        RecordingPlayAction second = new RecordingPlayAction(cardToPlay, source, "Second option");
        PullDeployRef ref = new PullDeployRef(
                TRANSACTION_ID, 73, 2, "asdf", new PullPhysicalCardRef(1001, 101),
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD,
                Zone.RESERVE_DECK, "asdf", new PullPhysicalCardRef(2002, 902),
                List.of(new PullPhysicalCardRef(3001, 301)),
                new PullPhysicalCardRef(3001, 301));
        PlayChoiceAction choice = new PlayChoiceAction(
                "asdf", source, cardToPlay, List.of(first, second));
        choice.setPullDeployRef(ref);

        Effect effect = choice.nextEffect(null);
        assertTrue(effect instanceof PlayoutDecisionEffect);
        CapturingFeedback feedback = new CapturingFeedback();
        ((PlayoutDecisionEffect) effect).doPlayEffect(game(feedback));
        feedback.decision.decisionMade("1");

        assertNull(first.getPullDeployRef());
        assertSame(ref, second.getPullDeployRef());
        assertEquals(TRANSACTION_ID, second.getPullDeployRef().transactionId());
        assertThrows(UnsupportedOperationException.class,
                () -> second.getPullDeployRef().orderedDestinationCards()
                        .add(new PullPhysicalCardRef(3002, 302)));
    }

    private static PhysicalCard card(int permanentCardId, int currentCardId, Zone zone) {
        SwccgCardBlueprint blueprint = stub(SwccgCardBlueprint.class, Map.of(
                "getCardCategory", CardCategory.CHARACTER,
                "getTitle", "Test Card",
                "getTitles", List.of("Test Card"),
                "getTestingText", "Test Card"));
        return stub(PhysicalCard.class, Map.of(
                "getPermanentCardId", permanentCardId,
                "getCardId", currentCardId,
                "getBlueprint", blueprint,
                "getBlueprintId", "1_1",
                "getTestingText", "Test Card",
                "getTitle", "Test Card",
                "getOwner", "asdf",
                "getZone", zone));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> values) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (values.containsKey(method.getName())) return values.get(method.getName());
            Class<?> result = method.getReturnType();
            if (result == boolean.class) return false;
            if (result == int.class) return 0;
            if (result == long.class) return 0L;
            if (result == float.class) return 0f;
            if (result == double.class) return 0d;
            return null;
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static SwccgGame game(UserFeedback feedback) {
        return stub(SwccgGame.class, Map.of("getUserFeedback", feedback));
    }

    private static final class CapturingFeedback implements UserFeedback {
        private AwaitingDecision decision;

        @Override
        public void sendAwaitingDecision(String playerId, AwaitingDecision awaitingDecision) {
            decision = awaitingDecision;
        }

        @Override
        public void sendWarning(String playerId, String warning) {
        }

        @Override
        public boolean hasPendingDecisions() {
            return decision != null;
        }
    }

    private static final class RecordingPlayAction extends AbstractPlayCardAction {
        private RecordingPlayAction(PhysicalCard cardToPlay, PhysicalCard source, String text) {
            super(cardToPlay, source);
            setText(text);
        }

        @Override
        public Effect nextEffect(SwccgGame game) {
            return null;
        }
    }
}
