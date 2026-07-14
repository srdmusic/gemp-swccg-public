package com.gempukku.swccgo.logic.effects.choose;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.common.PullDeployRef;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.communication.UserFeedback;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.ActionsEnvironment;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.SystemQueueAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.ArbitraryCardsSelectionDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.modifiers.CantSearchCardPileModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersEnvironment;
import com.gempukku.swccgo.logic.timing.Action;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused engine tests for the typed PULL decision metadata seam. */
public class PullMetadataSeamTest {
    private static final long TRANSACTION_ID = 9001L;
    private static final String PLAYER = "asdf";
    private static final String OPPONENT = "opponent";

    @Test
    public void cardActionDecisionAlignsPullMetadataAndRecordsAcceptedIdentity() throws Exception {
        PhysicalCard deploySource = card(1001, 101, Zone.AT_LOCATION);
        PhysicalCard ordinarySource = card(1002, 102, Zone.AT_LOCATION);
        PhysicalCard takeSource = card(1003, 103, Zone.AT_LOCATION);

        TopLevelGameTextAction deploy = pullAction(deploySource,
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD, "Deploy pull");
        SourceAction ordinary = new SourceAction(ordinarySource, "Ordinary action");
        TopLevelGameTextAction take = pullAction(takeSource,
                DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE,
                GameTextActionId.CAPTAIN_BEWIL__UPLOAD_INTERRUPT_FROM_FORCE_PILE, "Take pull");

        RecordingCardActionDecision decision =
                new RecordingCardActionDecision(73, List.of(deploy, ordinary, take));
        Map<String, String[]> params = decision.getDecisionParameters();

        assertArrayEquals(new String[]{"0", "1", "2"}, params.get("actionId"));
        assertArrayEquals(new String[]{
                        DecisionActionSemantic.PULL_DEPLOY_FROM_PILE.name(),
                        DecisionActionSemantic.UNKNOWN.name(),
                        DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE.name()},
                params.get(DecisionActionSemantic.WIRE_PARAMETER));
        assertArrayEquals(new String[]{"101", "102", "103"},
                params.get(PullDecisionWire.SOURCE_CARD_ID));
        assertArrayEquals(new String[]{"1001", "1002", "1003"},
                params.get(PullDecisionWire.SOURCE_PERMANENT_CARD_ID));
        assertArrayEquals(new String[]{
                        GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD.name(),
                        "",
                        GameTextActionId.CAPTAIN_BEWIL__UPLOAD_INTERRUPT_FROM_FORCE_PILE.name()},
                params.get(PullDecisionWire.GAME_TEXT_ACTION_ID));

        RecordingCardActionDecision ordinaryDecision =
                new RecordingCardActionDecision(74, List.of(ordinary));
        ordinaryDecision.decisionMade("0");
        assertNull(ordinary.getAcceptedDecisionId());
        assertNull(ordinary.getAcceptedDecisionOrdinal());

        decision.decisionMade("2");
        assertSame(take, decision.selected);
        assertEquals(73, take.getAcceptedDecisionId());
        assertEquals(2, take.getAcceptedDecisionOrdinal());
        Long acceptedTransactionId = take.getAcceptedPullTransactionId();
        assertNotNull(acceptedTransactionId);
        assertTrue(acceptedTransactionId > 0);
        assertNull(deploy.getAcceptedDecisionId());
        assertNull(deploy.getAcceptedPullTransactionId());
        assertNull(ordinary.getAcceptedPullTransactionId());
    }

    @Test
    public void pullStampedNonGameTextActionFailsClosedWithoutUnsupportedIdentityRead() {
        SourceAction malformedPull = new SourceAction(
                card(1001, 101, Zone.AT_LOCATION), "Malformed pull");
        malformedPull.setDecisionActionSemantic(
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);

        RecordingCardActionDecision decision =
                new RecordingCardActionDecision(74, List.of(malformedPull));

        assertArrayEquals(new String[]{""}, decision.getDecisionParameters()
                .get(PullDecisionWire.GAME_TEXT_ACTION_ID));
    }

    @Test
    public void arbitraryChildrenKeepTempIdsSeparateFromPhysicalPairsAndStampBothOrigins() {
        PhysicalCard source = card(1001, 101, Zone.AT_LOCATION);
        List<PhysicalCard> duplicateCopies = List.of(
                card(2001, 201, Zone.RESERVE_DECK),
                card(2002, 202, Zone.RESERVE_DECK));
        SourceAction parent = new SourceAction(source, "Parent pull");
        parent.setAcceptedDecisionIdentity(73, 1);
        parent.setAcceptedPullTransactionId(TRANSACTION_ID);

        for (DecisionOrigin origin : List.of(
                DecisionOrigin.PULL_DEPLOY_CHILD, DecisionOrigin.PULL_TAKE_CHILD)) {
            PullArbitraryDecision decision = new PullArbitraryDecision(parent, duplicateCopies, origin);
            Map<String, String[]> params = decision.getDecisionParameters();

            assertArrayEquals(new String[]{"temp0", "temp1"}, params.get("cardId"));
            assertArrayEquals(new String[]{"201", "202"},
                    params.get(PullDecisionWire.PHYSICAL_CARD_ID));
            assertArrayEquals(new String[]{"2001", "2002"},
                    params.get(PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID));
            assertArrayEquals(new String[]{origin.name()}, params.get(DecisionOrigin.WIRE_PARAMETER));
            assertArrayEquals(new String[]{"73"}, params.get(PullDecisionWire.PARENT_DECISION_ID));
            assertArrayEquals(new String[]{"1"}, params.get(PullDecisionWire.PARENT_ACTION_ORDINAL));
            assertArrayEquals(new String[]{String.valueOf(TRANSACTION_ID)},
                    params.get(PullDecisionWire.TRANSACTION_ID));
            assertArrayEquals(new String[]{PLAYER}, params.get(PullDecisionWire.PLAYER_ID));
            assertArrayEquals(new String[]{"101"}, params.get(PullDecisionWire.SOURCE_CARD_ID));
            assertArrayEquals(new String[]{"1001"},
                    params.get(PullDecisionWire.SOURCE_PERMANENT_CARD_ID));
            assertEquals(AwaitingDecisionType.ARBITRARY_CARDS.name(), origin.requiredWireTypeName());
        }
    }

    @Test
    public void standardDeployAndTakeConstructorsMarkDistinctParentSemantics() {
        SystemQueueAction deployParent = new SystemQueueAction();
        deployParent.setPerformingPlayer(PLAYER);
        new DeployCardFromReserveDeckEffect(deployParent, Filters.any, false);

        SystemQueueAction takeParent = new SystemQueueAction();
        takeParent.setPerformingPlayer(PLAYER);
        new TakeCardsIntoHandFromReserveDeckEffect(
                takeParent, PLAYER, 1, 1, Filters.any, false);

        assertEquals(DecisionActionSemantic.PULL_DEPLOY_FROM_PILE,
                deployParent.getDecisionActionSemantic());
        assertEquals(DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE,
                takeParent.getDecisionActionSemantic());
    }

    @Test
    public void failedVerifyStampsTypedOriginZeroBoundsAndCandidatePairs() {
        List<PhysicalCard> pile = List.of(
                card(2001, 201, Zone.RESERVE_DECK),
                card(2002, 202, Zone.RESERVE_DECK));
        SourceAction parent = new SourceAction(card(1001, 101, Zone.AT_LOCATION), "Parent pull");
        parent.setDecisionActionSemantic(DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        parent.setAcceptedDecisionIdentity(73, 0);
        parent.setAcceptedPullTransactionId(TRANSACTION_ID);
        List<AwaitingDecision> captured = new ArrayList<>();

        ChooseCardsFromPileEffect effect = new ChooseCardsFromPileEffect(
                parent, PLAYER, Zone.RESERVE_DECK, PLAYER,
                1, 1, 1, false, false, Filters.none) {
            @Override
            protected DecisionOrigin getPullDecisionOrigin() {
                return DecisionOrigin.PULL_DEPLOY_CHILD;
            }

            @Override
            protected void cardsSelected(SwccgGame game, Collection<PhysicalCard> selectedCards) {
            }
        };

        effect.playEffect(game(new PileGameState(pile), captured));

        assertEquals(2, captured.size());
        AwaitingDecision searcherVerification = captured.get(0);
        Map<String, String[]> params = searcherVerification.getDecisionParameters();
        assertEquals(AwaitingDecisionType.ARBITRARY_CARDS,
                searcherVerification.getDecisionType());
        assertArrayEquals(new String[]{DecisionOrigin.PULL_FAILED_VERIFY.name()},
                params.get(DecisionOrigin.WIRE_PARAMETER));
        assertArrayEquals(new String[]{"0"}, params.get("min"));
        assertArrayEquals(new String[]{"0"}, params.get("max"));
        assertArrayEquals(new String[]{String.valueOf(TRANSACTION_ID)},
                params.get(PullDecisionWire.TRANSACTION_ID));
        assertArrayEquals(new String[]{"temp0", "temp1"}, params.get("cardId"));
        assertArrayEquals(new String[]{"201", "202"},
                params.get(PullDecisionWire.PHYSICAL_CARD_ID));
        assertArrayEquals(new String[]{"2001", "2002"},
                params.get(PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID));
        assertArrayEquals(new String[]{"101"}, params.get(PullDecisionWire.SOURCE_CARD_ID));
        assertArrayEquals(new String[]{"1001"},
                params.get(PullDecisionWire.SOURCE_PERMANENT_CARD_ID));

        AwaitingDecision opponentVerification = captured.get(1);
        assertEquals(AwaitingDecisionType.ARBITRARY_CARDS,
                opponentVerification.getDecisionType());
        assertNull(opponentVerification.getDecisionParameters()
                .get(DecisionOrigin.WIRE_PARAMETER));
        assertNull(opponentVerification.getDecisionParameters()
                .get(PullDecisionWire.TRANSACTION_ID));
    }

    @Test
    public void failedStandardSearchInstallsTheEngineEndOfTurnProhibition() {
        PhysicalCard source = card(1001, 101, Zone.AT_LOCATION);
        TopLevelGameTextAction parent = pullAction(source,
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD, "Parent pull");
        parent.setAcceptedDecisionIdentity(73, 0);
        parent.setAcceptedPullTransactionId(TRANSACTION_ID);
        AtomicReference<Modifier> installed = new AtomicReference<>();
        ModifiersEnvironment modifiers = (ModifiersEnvironment) Proxy.newProxyInstance(
                ModifiersEnvironment.class.getClassLoader(),
                new Class<?>[]{ModifiersEnvironment.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addUntilEndOfTurnModifier")) {
                        installed.set((Modifier) args[0]);
                    }
                    return null;
                });

        ChooseCardsFromPileEffect effect = new ChooseCardsFromPileEffect(
                parent, PLAYER, Zone.RESERVE_DECK, PLAYER,
                1, 1, 1, false, false, Filters.none) {
            @Override
            protected DecisionOrigin getPullDecisionOrigin() {
                return DecisionOrigin.PULL_DEPLOY_CHILD;
            }

            @Override
            protected void cardsSelected(SwccgGame game, Collection<PhysicalCard> selectedCards) {
            }
        };

        GameState state = new PileGameState(List.of(card(2001, 201, Zone.RESERVE_DECK)));
        effect.playEffect(game(state, new ArrayList<>(), modifiers));

        assertTrue(installed.get() instanceof CantSearchCardPileModifier);
        CantSearchCardPileModifier prohibition =
                (CantSearchCardPileModifier) installed.get();
        assertTrue(prohibition.isProhibitedFromSearchingCardPile(
                state, null, PLAYER, Zone.RESERVE_DECK, PLAYER,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD));
        assertFalse(prohibition.isProhibitedFromSearchingCardPile(
                state, null, OPPONENT, Zone.RESERVE_DECK, PLAYER,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD));
    }

    @Test
    public void acceptedTransactionAndPhysicalPairsStayAlignedAcrossTypedDestinations()
            throws Exception {
        PhysicalCard source = card(1001, 101, Zone.AT_LOCATION);
        TopLevelGameTextAction action = pullAction(source,
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD, "Deploy pull");
        RecordingCardActionDecision parentDecision =
                new RecordingCardActionDecision(73, List.of(action));
        parentDecision.decisionMade("0");
        Long transactionId = action.getAcceptedPullTransactionId();
        assertNotNull(transactionId);
        assertTrue(transactionId > 0);

        PullArbitraryDecision child = new PullArbitraryDecision(action, List.of(
                card(2001, 201, Zone.RESERVE_DECK),
                card(2002, 202, Zone.RESERVE_DECK)), DecisionOrigin.PULL_DEPLOY_CHILD);
        Map<String, String[]> childParams = child.getDecisionParameters();
        assertArrayEquals(new String[]{DecisionOrigin.PULL_DEPLOY_CHILD.name()},
                childParams.get(DecisionOrigin.WIRE_PARAMETER));
        assertArrayEquals(new String[]{"73"},
                childParams.get(PullDecisionWire.PARENT_DECISION_ID));
        assertArrayEquals(new String[]{"0"},
                childParams.get(PullDecisionWire.PARENT_ACTION_ORDINAL));
        assertArrayEquals(new String[]{String.valueOf(transactionId)},
                childParams.get(PullDecisionWire.TRANSACTION_ID));
        assertArrayEquals(new String[]{"101"}, childParams.get(PullDecisionWire.SOURCE_CARD_ID));
        assertArrayEquals(new String[]{"1001"},
                childParams.get(PullDecisionWire.SOURCE_PERMANENT_CARD_ID));
        assertArrayEquals(new String[]{GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD.name()},
                childParams.get(PullDecisionWire.GAME_TEXT_ACTION_ID));
        assertArrayEquals(new String[]{"201", "202"},
                childParams.get(PullDecisionWire.PHYSICAL_CARD_ID));
        assertArrayEquals(new String[]{"2001", "2002"},
                childParams.get(PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID));

        PullPhysicalCardRef selectedAfterZoneChange = new PullPhysicalCardRef(2002, 902);
        action.setPullDeployRef(new PullDeployRef(
                transactionId,
                action.getAcceptedDecisionId(),
                action.getAcceptedDecisionOrdinal(),
                PLAYER,
                new PullPhysicalCardRef(1001, 101),
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD,
                Zone.RESERVE_DECK,
                PLAYER,
                selectedAfterZoneChange,
                List.of(),
                null));

        AwaitingDecision ordered = openDestinationDecision(action, List.of(
                card(3002, 302, Zone.AT_LOCATION),
                card(3001, 301, Zone.AT_LOCATION)));
        Map<String, String[]> orderedParams = ordered.getDecisionParameters();

        assertEquals("Pick one", ordered.getText());
        assertFalse(ordered.getText().toLowerCase().contains("destination"));
        assertArrayEquals(new String[]{DecisionOrigin.PULL_DESTINATION.name()},
                orderedParams.get(DecisionOrigin.WIRE_PARAMETER));
        assertEquals(ordered.getDecisionType().name(),
                DecisionOrigin.PULL_DESTINATION.requiredWireTypeName());
        assertArrayEquals(new String[]{String.valueOf(transactionId)},
                orderedParams.get(PullDecisionWire.TRANSACTION_ID));
        assertArrayEquals(new String[]{"73"},
                orderedParams.get(PullDecisionWire.PARENT_DECISION_ID));
        assertArrayEquals(new String[]{"0"},
                orderedParams.get(PullDecisionWire.PARENT_ACTION_ORDINAL));
        assertArrayEquals(new String[]{"101"}, orderedParams.get(PullDecisionWire.SOURCE_CARD_ID));
        assertArrayEquals(new String[]{"1001"},
                orderedParams.get(PullDecisionWire.SOURCE_PERMANENT_CARD_ID));
        assertArrayEquals(new String[]{GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD.name()},
                orderedParams.get(PullDecisionWire.GAME_TEXT_ACTION_ID));
        assertArrayEquals(new String[]{"902"}, orderedParams.get(PullDecisionWire.SELECTED_CARD_ID));
        assertArrayEquals(new String[]{"2002"},
                orderedParams.get(PullDecisionWire.SELECTED_PERMANENT_CARD_ID));
        assertArrayEquals(new String[]{"302", "301"},
                orderedParams.get(PullDecisionWire.DESTINATION_CARD_ID));
        assertArrayEquals(new String[]{"3002", "3001"},
                orderedParams.get(PullDecisionWire.DESTINATION_PERMANENT_CARD_ID));
        assertNull(orderedParams.get(PullDecisionWire.FORCED_DESTINATION_CARD_ID));
        assertEquals(List.of(
                        new PullPhysicalCardRef(3002, 302),
                        new PullPhysicalCardRef(3001, 301)),
                action.getPullDeployRef().orderedDestinationCards());
        assertEquals(transactionId.longValue(), action.getPullDeployRef().transactionId());
        assertEquals(selectedAfterZoneChange, action.getPullDeployRef().selectedCard());

        AwaitingDecision optionalSingle = openDestinationDecision(action,
                List.of(card(3001, 301, Zone.AT_LOCATION)));
        Map<String, String[]> forcedParams = optionalSingle.getDecisionParameters();
        assertArrayEquals(new String[]{DecisionOrigin.PULL_DESTINATION.name()},
                forcedParams.get(DecisionOrigin.WIRE_PARAMETER));
        assertArrayEquals(new String[]{String.valueOf(transactionId)},
                forcedParams.get(PullDecisionWire.TRANSACTION_ID));
        assertArrayEquals(new String[]{"301"},
                forcedParams.get(PullDecisionWire.DESTINATION_CARD_ID));
        assertArrayEquals(new String[]{"3001"},
                forcedParams.get(PullDecisionWire.DESTINATION_PERMANENT_CARD_ID));
        assertNull(forcedParams.get(PullDecisionWire.FORCED_DESTINATION_CARD_ID));
        assertNull(forcedParams.get(PullDecisionWire.FORCED_DESTINATION_PERMANENT_CARD_ID));
        assertNull(action.getPullDeployRef().forcedDestinationCard());

        autoSelectDestination(action, card(3001, 301, Zone.AT_LOCATION));
        assertEquals(new PullPhysicalCardRef(3001, 301),
                action.getPullDeployRef().forcedDestinationCard());
        assertEquals(transactionId.longValue(), action.getPullDeployRef().transactionId());
        assertEquals(transactionId, action.getAcceptedPullTransactionId());
    }

    private static TopLevelGameTextAction pullAction(
            PhysicalCard source, DecisionActionSemantic semantic,
            GameTextActionId gameTextActionId, String text) {
        TopLevelGameTextAction action = new TopLevelGameTextAction(
                source, PLAYER, source.getCardId(), gameTextActionId);
        action.setText(text);
        action.setDecisionActionSemantic(semantic);
        return action;
    }

    private static AwaitingDecision openDestinationDecision(
            Action action, List<PhysicalCard> destinations) {
        List<AwaitingDecision> captured = new ArrayList<>();
        ChooseCardsOnTableEffect effect = new ChooseCardsOnTableEffect(
                action, PLAYER, "Pick one", 0, destinations.size(), destinations, Filters.any) {
            @Override
            protected boolean isPullDestinationSelection() {
                return true;
            }

            @Override
            protected void cardsSelected(Collection<PhysicalCard> selectedCards) {
            }
        };
        effect.playEffect(game(new GameState(), captured));
        assertEquals(1, captured.size());
        return captured.get(0);
    }

    private static void autoSelectDestination(Action action, PhysicalCard destination) {
        List<AwaitingDecision> captured = new ArrayList<>();
        AtomicReference<Collection<PhysicalCard>> selected = new AtomicReference<>();
        ChooseCardsOnTableEffect effect = new ChooseCardsOnTableEffect(
                action, PLAYER, "Auto-select one", 1, 1,
                List.of(destination), Filters.any) {
            @Override
            protected boolean isPullDestinationSelection() {
                return true;
            }

            @Override
            protected void cardsSelected(Collection<PhysicalCard> selectedCards) {
                selected.set(selectedCards);
            }
        };
        effect.playEffect(game(new GameState(), captured));
        assertTrue(captured.isEmpty());
        assertEquals(List.of(destination), List.copyOf(selected.get()));
    }

    private static PhysicalCard card(int permanentCardId, int currentCardId, Zone zone) {
        SwccgCardBlueprint blueprint = stub(SwccgCardBlueprint.class, Map.of(
                "getCardCategory", CardCategory.CHARACTER,
                "getTitle", "Duplicate Card",
                "getTitles", List.of("Duplicate Card"),
                "getTestingText", "Duplicate Card"));
        return stub(PhysicalCard.class, Map.of(
                "getPermanentCardId", permanentCardId,
                "getCardId", currentCardId,
                "getBlueprint", blueprint,
                "getBlueprintId", "1_1",
                "getTestingText", "Duplicate Card",
                "getTitle", "Duplicate Card",
                "getOwner", PLAYER,
                "getZone", zone));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> values) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("toString")) return "stub-" + type.getSimpleName();
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

    private static SwccgGame game(GameState gameState, List<AwaitingDecision> captured) {
        return game(gameState, captured, stub(ModifiersEnvironment.class, Map.of()));
    }

    private static SwccgGame game(GameState gameState, List<AwaitingDecision> captured,
                                  ModifiersEnvironment modifiersEnvironment) {
        UserFeedback feedback = new UserFeedback() {
            @Override
            public void sendAwaitingDecision(String playerId, AwaitingDecision awaitingDecision) {
                captured.add(awaitingDecision);
            }

            @Override
            public void sendWarning(String playerId, String warning) {
            }

            @Override
            public boolean hasPendingDecisions() {
                return !captured.isEmpty();
            }
        };
        return stub(SwccgGame.class, Map.of(
                "getGameState", gameState,
                "getUserFeedback", feedback,
                "getActionsEnvironment", stub(ActionsEnvironment.class, Map.of()),
                "getModifiersEnvironment", modifiersEnvironment,
                "getOpponent", OPPONENT));
    }

    private static final class SourceAction extends SystemQueueAction {
        private final PhysicalCard source;
        private final String text;

        private SourceAction(PhysicalCard source, String text) {
            this.source = source;
            this.text = text;
            setPerformingPlayer(PLAYER);
        }

        @Override
        public PhysicalCard getActionSource() {
            return source;
        }

        @Override
        public String getText() {
            return text;
        }
    }

    private static final class RecordingCardActionDecision extends CardActionSelectionDecision {
        private Action selected;

        private RecordingCardActionDecision(int decisionId, List<Action> actions) {
            super(decisionId, "Choose action", actions, true, false, false, false, false);
        }

        @Override
        public void decisionMade(String result) throws DecisionResultInvalidException {
            selected = getSelectedAction(result);
        }
    }

    private static final class PullArbitraryDecision extends ArbitraryCardsSelectionDecision {
        private PullArbitraryDecision(
                Action parent, List<PhysicalCard> cards, DecisionOrigin origin) {
            super("Neutral child choice", cards, cards, 1, 1);
            setPullDecisionMetadata(parent, Zone.RESERVE_DECK, PLAYER, origin);
        }

        @Override
        public void decisionMade(String result) {
        }
    }

    private static final class PileGameState extends GameState {
        private final List<PhysicalCard> pile;

        private PileGameState(List<PhysicalCard> pile) {
            this.pile = pile;
        }

        @Override
        public List<PhysicalCard> getCardPile(String playerId, Zone zone) {
            return pile;
        }

        @Override
        public Phase getCurrentPhase() {
            return Phase.DEPLOY;
        }

        @Override
        public void sendMessage(String message) {
        }
    }
}
