package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceFinalization;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Shared direct and mediator lifecycle contract for the typed DEPLOY owner. */
public abstract class AbstractDeployCutoverIntegrationTest {
    protected static final String PLAYER = "tester";

    private static final String PARENT_WIRE = "deploy-parent-wire";
    private static final String ATTEMPT = "DEPLOY-direct-attempt";
    private static final String PULL_WIRE = "temp7";
    private static final long PULL_TRANSACTION = 90001L;

    public record DirectCapture(String wire, DeployTransaction transaction) {
    }

    public record MediatorCapture(AiDecisionResult result,
                                  DeployTransaction beforeDisposition,
                                  DeployTransaction afterDisposition) {
    }

    public enum V170Path {
        DIRECT,
        MEDIATOR_ACCEPTED,
        MEDIATOR_REJECTED
    }

    public record V170Capture(String wire,
                              AiDecisionResult result,
                              DecisionTrace trace,
                              DeployTransaction beforeDisposition,
                              DeployTransaction afterDisposition) {
    }

    public record LegacyV170Capture(String wire,
                                    AiDecisionResult result,
                                    DecisionTrace trace,
                                    DeployTransaction beforeDecision,
                                    DeployTransaction afterDisposition) {
    }

    public record BlockedReplayCapture(List<String> wires,
                                       List<DeployTransaction> cursors,
                                       List<DecisionTrace> traces) {
    }

    protected abstract DirectCapture runDirect(AwaitingDecision decision,
                                                GameState gameState);

    protected abstract MediatorCapture runMediatorAccepted(
            AwaitingDecision decision, GameState gameState,
            RejectionHistory history);

    protected abstract MediatorCapture runMediatorRejected(
            AwaitingDecision decision, GameState gameState,
            RejectionHistory history);

    protected abstract V170Capture runV170(
            V170Path path,
            AwaitingDecision parentDecision,
            AwaitingDecision v170Decision,
            GameState gameState,
            SwccgGame game,
            RejectionHistory history);

    protected abstract LegacyV170Capture runLegacyV170(
            boolean mediator,
            AwaitingDecision parentDecision,
            AwaitingDecision v170Decision,
            GameState gameState,
            SwccgGame game);

    protected abstract BlockedReplayCapture runBlockedReplay(
            AwaitingDecision first,
            AwaitingDecision second,
            GameState gameState);

    @After
    public void clearAnyLeakedTrace() {
        if (TraceSession.isActive()) {
            TraceSession.abandon();
        }
    }

    @Test
    public void directParentAppliesAcceptedCursorInline() {
        DirectCapture capture = runDirect(parentDecision(401), deployState());

        assertEquals(PARENT_WIRE, capture.wire());
        assertParentCursor(capture.transaction());
    }

    @Test
    public void mediatorParentWaitsForAcceptedDisposition() {
        MediatorCapture capture = runMediatorAccepted(
                parentDecision(402), deployState(), RejectionHistory.empty());

        assertTrue(capture.result().fromTypedFinalizer());
        assertEquals(PARENT_WIRE, capture.result().wireResponse());
        assertNull(capture.beforeDisposition());
        assertParentCursor(capture.afterDisposition());
    }

    @Test
    public void mediatorParentRejectionNeverCreatesCursor() {
        MediatorCapture capture = runMediatorRejected(
                parentDecision(403), deployState(), RejectionHistory.empty());

        assertTrue(capture.result().fromTypedFinalizer());
        assertNull(capture.beforeDisposition());
        assertNull(capture.afterDisposition());
    }

    @Test
    public void directPullDeployChildStartsPhysicalCursorInline() {
        DirectCapture capture = runDirect(pullDeployChildDecision(404), deployState());

        assertEquals(PULL_WIRE, capture.wire());
        DeployTransaction transaction = capture.transaction();
        assertNotNull(transaction);
        assertEquals(DeployTransaction.Stage.PARENT_PENDING, transaction.stage());
        assertEquals("PULL-" + PULL_TRANSACTION, transaction.key().attemptId());
        assertEquals(new DeployPhysicalCardRef(7001, 701), transaction.sourceCard());
        assertEquals(Zone.RESERVE_DECK, transaction.sourceZone());
        assertEquals(java.util.List.of(PULL_WIRE), transaction.acceptedWires());
    }

    @Test
    public void allBlockedOptionalParentReplayNeverOpensAChildCursor() {
        BlockedReplayCapture capture = runBlockedReplay(
                blockedParentDecision(440), blockedParentDecision(441), deployState());

        assertEquals(List.of("", ""), capture.wires());
        assertEquals(java.util.Arrays.asList(null, null), capture.cursors());
        assertEquals(2, capture.traces().size());
        for (DecisionTrace trace : capture.traces()) {
            assertEquals(TraceStatus.COMPLETE, trace.getStatus());
            assertEquals(TraceRoute.DEPLOY_PARENT, trace.getRoute().selected());
            assertEquals("", trace.getFinalization().finalResponse());
            assertEquals(1L, trackerMutations(trace));
        }
        assertFalse(TraceSession.isActive());
    }

    @Test
    public void directV170UsesPositiveDrainAndReversedOriginalOrdinal() {
        V170Fixture fixture = v170Fixture(2f);
        V170Capture capture = runV170(
                V170Path.DIRECT,
                parentDecision(450),
                v170Decision(451, 450, "No", "Yes"),
                fixture.gameState(), fixture.game(), RejectionHistory.empty());

        assertEquals("1", capture.wire());
        assertNull(capture.result());
        assertParentCursor(capture.beforeDisposition());
        assertAcceptedV170Choice(capture.afterDisposition(), "1");
        assertDirectTrace(capture.trace(), "1");
    }

    @Test
    public void mediatorV170KnownZeroDrainAcceptsNoOnce() {
        V170Fixture fixture = v170Fixture(0f);
        V170Capture capture = runV170(
                V170Path.MEDIATOR_ACCEPTED,
                parentDecision(460),
                v170Decision(461, 460, "Yes", "No"),
                fixture.gameState(), fixture.game(), RejectionHistory.empty());

        assertTypedV170(capture.result(), "1");
        assertEquals("1", capture.wire());
        assertParentCursor(capture.beforeDisposition());
        assertAcceptedV170Choice(capture.afterDisposition(), "1");
        assertAcceptedTrace(capture.trace(), "1");
    }

    @Test
    public void mediatorV170RejectionClearsCursorWithoutMutation() {
        V170Fixture fixture = v170Fixture(3f);
        V170Capture capture = runV170(
                V170Path.MEDIATOR_REJECTED,
                parentDecision(470),
                v170Decision(471, 470, "Yes", "No"),
                fixture.gameState(), fixture.game(), RejectionHistory.empty());

        assertTypedV170(capture.result(), "0");
        assertEquals("0", capture.wire());
        assertParentCursor(capture.beforeDisposition());
        assertNull(capture.afterDisposition());
        assertRejectedTrace(capture.trace());
    }

    @Test
    public void directV170KnownZeroDrainUsesNoInReversedOriginalOrder() {
        V170Fixture fixture = v170Fixture(0f);
        V170Capture capture = runV170(
                V170Path.DIRECT,
                parentDecision(472),
                v170Decision(473, 472, "No", "Yes"),
                fixture.gameState(), fixture.game(), RejectionHistory.empty());

        assertEquals("0", capture.wire());
        assertAcceptedV170Choice(capture.afterDisposition(), "0");
        assertDirectTrace(capture.trace(), "0");
    }

    @Test
    public void mediatorV170PositiveDrainAcceptsYesInReversedOriginalOrder() {
        V170Fixture fixture = v170Fixture(2f);
        V170Capture capture = runV170(
                V170Path.MEDIATOR_ACCEPTED,
                parentDecision(474),
                v170Decision(475, 474, "No", "Yes"),
                fixture.gameState(), fixture.game(), RejectionHistory.empty());

        assertTypedV170(capture.result(), "1");
        assertAcceptedV170Choice(capture.afterDisposition(), "1");
        assertAcceptedTrace(capture.trace(), "1");
    }

    @Test
    public void unknownDrainFactsRemainLegacyUnownedOnTheDirectPath() {
        V170Fixture fixture = unknownV170Fixture();
        LegacyV170Capture capture = runLegacyV170(
                false, parentDecision(476),
                v170Decision(477, 476, "Yes", "No"),
                fixture.gameState(), fixture.game());

        assertLegacyUnownedV170(capture);
        assertNull(capture.result());
    }

    @Test
    public void malformedResultsRemainLegacyUnownedOnTheMediatorPath() {
        V170Fixture fixture = v170Fixture(2f);
        LegacyV170Capture capture = runLegacyV170(
                true, parentDecision(478),
                v170Decision(479, 478, "Yes", "Maybe"),
                fixture.gameState(), fixture.game());

        assertLegacyUnownedV170(capture);
        assertNotNull(capture.result());
    }

    private static void assertLegacyUnownedV170(LegacyV170Capture capture) {
        assertNotNull(capture.beforeDecision());
        assertSame(capture.beforeDecision(), capture.afterDisposition());
        assertEquals(List.of(PARENT_WIRE),
                capture.afterDisposition().acceptedWires());
        assertNotNull(capture.trace());
        assertEquals(TraceStatus.COMPLETE, capture.trace().getStatus());
        assertNotEquals(TraceRoute.DEPLOY_UNDERCOVER,
                capture.trace().getRoute().selected());
        assertFalse(TraceSession.isActive());
    }

    private static void assertParentCursor(DeployTransaction transaction) {
        assertNotNull(transaction);
        assertEquals(DeployTransaction.Stage.PARENT_PENDING, transaction.stage());
        assertEquals(ATTEMPT, transaction.key().attemptId());
        assertEquals(0, transaction.parentActionOrdinal());
        assertEquals(PARENT_WIRE, transaction.parentActionWireId());
        assertEquals(java.util.List.of(PARENT_WIRE), transaction.acceptedWires());
    }

    private static void assertAcceptedV170Choice(DeployTransaction transaction,
                                                 String choiceWire) {
        assertNotNull(transaction);
        assertEquals(DeployTransaction.Stage.PARENT_PENDING, transaction.stage());
        assertEquals(List.of(PARENT_WIRE, choiceWire), transaction.acceptedWires());
    }

    private static void assertTypedV170(AiDecisionResult result, String wire) {
        assertNotNull(result);
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(wire, result.wireResponse());
        assertTrue(result.fromTypedFinalizer());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON,
                result.mutationMode());
    }

    private static void assertDirectTrace(DecisionTrace trace, String wire) {
        assertCompleteV170Trace(trace);
        assertEquals(wire, trace.getFinalization().finalResponse());
        assertEquals(1L, trackerMutations(trace));
        assertFalse(TraceSession.isActive());
    }

    private static void assertAcceptedTrace(DecisionTrace trace, String wire) {
        assertCompleteV170Trace(trace);
        assertEquals(wire, trace.getFinalization().finalResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED,
                trace.getFinalization().disposition());
        assertEquals(TraceFinalization.MutationMode.OUTER_COMMON,
                trace.getFinalization().acceptedMutationMode());
        assertTrue(trace.getFinalization().acceptedMutationCompleted());
        assertEquals(1L, trackerMutations(trace));
        assertFalse(TraceSession.isActive());
    }

    private static void assertRejectedTrace(DecisionTrace trace) {
        assertCompleteV170Trace(trace);
        assertEquals(TraceFinalization.Disposition.ENGINE_REJECTED,
                trace.getFinalization().disposition());
        assertEquals(0L, trackerMutations(trace));
        assertFalse(TraceSession.isActive());
    }

    private static void assertCompleteV170Trace(DecisionTrace trace) {
        assertNotNull(trace);
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        assertEquals(TraceRoute.DEPLOY_UNDERCOVER,
                trace.getRoute().selected());
    }

    private static long trackerMutations(DecisionTrace trace) {
        return trace.getStateEvents().stream()
                .filter(TrackerRecordResponseEvent.class::isInstance)
                .count();
    }

    private static GameState deployState() {
        return AbstractActivateControlDecisionHarnessTest.stubWithTurnPlayer(
                Phase.DEPLOY, PLAYER);
    }

    private static AwaitingDecision parentDecision(int id) {
        Map<String, String[]> parameters = params(
                DecisionOrigin.WIRE_PARAMETER,
                        arr(DecisionOrigin.PHASE_ACTION.name()),
                "actionId", arr(PARENT_WIRE),
                "actionText", arr("Deploy a character"),
                "cardId", arr("102"),
                "blueprintId", arr("7_1"),
                DecisionActionSemantic.WIRE_PARAMETER,
                        arr(DecisionActionSemantic.DEPLOY_CARD.name()),
                DeployDecisionWire.ATTEMPT_ID, arr(ATTEMPT),
                DeployDecisionWire.PLAYER_ID, arr(PLAYER),
                DeployDecisionWire.SOURCE_CARD_ID, arr("102"),
                DeployDecisionWire.SOURCE_PERMANENT_CARD_ID, arr("1002"),
                DeployDecisionWire.SOURCE_ZONE, arr(Zone.HAND.name()),
                DeployDecisionWire.DESTINATION_LEGALITY_KNOWN, arr("true"),
                DeployDecisionWire.LEGAL_DESTINATIONS, arr("CARD:3001:301"),
                DeployDecisionWire.LEGAL_BUDDIES, arr(""),
                DeployDecisionWire.SELECTED_BUDDY, arr(""),
                "noPass", arr("true"));
        return decision(id, AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Choose one deploy action", parameters);
    }

    private static AwaitingDecision blockedParentDecision(int id) {
        Map<String, String[]> parameters = params(
                DecisionOrigin.WIRE_PARAMETER,
                        arr(DecisionOrigin.PHASE_ACTION.name()),
                "actionId", arr(PARENT_WIRE),
                "actionText", arr("Deploy a character"),
                "cardId", arr("102"),
                "blueprintId", arr("7_1"),
                DecisionActionSemantic.WIRE_PARAMETER,
                        arr(DecisionActionSemantic.DEPLOY_CARD.name()),
                DeployDecisionWire.ATTEMPT_ID, arr(ATTEMPT),
                DeployDecisionWire.PLAYER_ID, arr(PLAYER),
                DeployDecisionWire.SOURCE_CARD_ID, arr("102"),
                DeployDecisionWire.SOURCE_PERMANENT_CARD_ID, arr("1002"),
                DeployDecisionWire.SOURCE_ZONE, arr(Zone.HAND.name()),
                DeployDecisionWire.DESTINATION_LEGALITY_KNOWN, arr("true"),
                DeployDecisionWire.LEGAL_DESTINATIONS, arr(""),
                DeployDecisionWire.LEGAL_BUDDIES, arr(""),
                DeployDecisionWire.SELECTED_BUDDY, arr(""),
                "noPass", arr("false"));
        return decision(id, AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Opaque blocked deploy parent", parameters);
    }

    private static AwaitingDecision pullDeployChildDecision(int id) {
        Map<String, String[]> parameters = params(
                DecisionOrigin.WIRE_PARAMETER,
                        arr(DecisionOrigin.PULL_DEPLOY_CHILD.name()),
                PullDecisionWire.TRANSACTION_ID,
                        arr(String.valueOf(PULL_TRANSACTION)),
                PullDecisionWire.PARENT_DECISION_ID, arr("300"),
                PullDecisionWire.PARENT_ACTION_ORDINAL, arr("0"),
                PullDecisionWire.PLAYER_ID, arr(PLAYER),
                PullDecisionWire.SOURCE_CARD_ID, arr("901"),
                PullDecisionWire.SOURCE_PERMANENT_CARD_ID, arr("9001"),
                PullDecisionWire.GAME_TEXT_ACTION_ID,
                        arr(GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD.name()),
                PullDecisionWire.SOURCE_ZONE, arr(Zone.RESERVE_DECK.name()),
                PullDecisionWire.SOURCE_ZONE_OWNER, arr(PLAYER),
                "cardId", arr(PULL_WIRE),
                "blueprintId", arr("7_1"),
                "min", arr("1"),
                "max", arr("1"),
                "selectable", arr("true"),
                "preselected", arr("false"),
                "returnAnyChange", arr("false"),
                PullDecisionWire.PHYSICAL_CARD_ID, arr("701"),
                PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID, arr("7001"));
        return decision(id, AwaitingDecisionType.ARBITRARY_CARDS,
                "Choose card to deploy from Reserve Deck", parameters);
    }

    private static AwaitingDecision v170Decision(int id,
                                                 int parentDecisionId,
                                                 String... results) {
        Map<String, String[]> parameters = params(
                DecisionOrigin.WIRE_PARAMETER,
                        arr(DecisionOrigin.DEPLOY_V170_UNDERCOVER.name()),
                DeployDecisionWire.ATTEMPT_ID, arr(ATTEMPT),
                DeployDecisionWire.PARENT_DECISION_ID,
                        arr(String.valueOf(parentDecisionId)),
                DeployDecisionWire.PARENT_ACTION_ORDINAL, arr("0"),
                DeployDecisionWire.PLAYER_ID, arr(PLAYER),
                DeployDecisionWire.SOURCE_CARD_ID, arr("102"),
                DeployDecisionWire.SOURCE_PERMANENT_CARD_ID, arr("1002"),
                DeployDecisionWire.SOURCE_ZONE, arr(Zone.HAND.name()),
                DeployDecisionWire.DESTINATION_LEGALITY_KNOWN, arr("true"),
                DeployDecisionWire.DESTINATION_CARD_ID, arr("301"),
                DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID, arr("3001"),
                DeployDecisionWire.BUDDY_CARD_ID, arr(),
                DeployDecisionWire.BUDDY_PERMANENT_CARD_ID, arr(),
                DeployDecisionWire.FORCED_DESTINATION, arr("false"),
                "results", arr(results));
        return decision(id, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Opaque PlayCharacterAction compatibility choice", parameters);
    }

    private static V170Fixture v170Fixture(float drain) {
        return v170Fixture(drain, false);
    }

    private static V170Fixture unknownV170Fixture() {
        return v170Fixture(0f, true);
    }

    private static V170Fixture v170Fixture(float drain, boolean failDrainQuery) {
        PhysicalCard source = physicalCard(1002, 102, PLAYER, Zone.HAND, "Source");
        PhysicalCard location = physicalCard(3001, 301, PLAYER,
                Zone.LOCATIONS, "Destination");
        PhysicalCard opponent = physicalCard(5001, 501, "opponent",
                Zone.AT_LOCATION, "Opponent body");
        GameState gameState = new V170GameState(source, location, opponent);
        ModifiersQuerying modifiers = (ModifiersQuerying) Proxy.newProxyInstance(
                ModifiersQuerying.class.getClassLoader(),
                new Class<?>[]{ModifiersQuerying.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getForceDrainAmount")) {
                        if (failDrainQuery) {
                            throw new IllegalStateException("unknown drain fixture");
                        }
                        return drain;
                    }
                    return defaultValue(method.getReturnType());
                });
        SwccgGame game = (SwccgGame) Proxy.newProxyInstance(
                SwccgGame.class.getClassLoader(),
                new Class<?>[]{SwccgGame.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGameState" -> gameState;
                    case "getModifiersQuerying" -> modifiers;
                    case "findCardByPermanentId" -> {
                        Integer permanentId = args != null && args.length == 1
                                ? (Integer) args[0] : null;
                        if (Integer.valueOf(1002).equals(permanentId)) {
                            yield source;
                        }
                        if (Integer.valueOf(3001).equals(permanentId)) {
                            yield location;
                        }
                        yield null;
                    }
                    case "getOpponent" -> "opponent";
                    case "getSide" -> Side.DARK;
                    case "getPlayer" -> PLAYER;
                    case "isTestEnvironment" -> true;
                    default -> defaultValue(method.getReturnType());
                });
        return new V170Fixture(gameState, game);
    }

    private static PhysicalCard physicalCard(int permanentId,
                                             int currentId,
                                             String owner,
                                             Zone zone,
                                             String title) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPermanentCardId" -> permanentId;
                    case "getCardId" -> currentId;
                    case "getOwner", "getZoneOwner" -> owner;
                    case "getZone" -> zone;
                    case "getTitle", "getTitleAbbreviated" -> title;
                    case "getTitles" -> List.of(title);
                    case "getBlueprintId" -> "fixture_" + permanentId;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> title + "#" + permanentId;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            if (List.class.isAssignableFrom(type)) {
                return List.of();
            }
            if (Set.class.isAssignableFrom(type)) {
                return Set.of();
            }
            if (Map.class.isAssignableFrom(type)) {
                return Map.of();
            }
            return null;
        }
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    private record V170Fixture(GameState gameState, SwccgGame game) {
    }

    private static final class V170GameState
            extends AbstractActivateControlDecisionHarnessTest.StubGameState {
        private final PhysicalCard source;
        private final PhysicalCard location;
        private final PhysicalCard opponent;

        private V170GameState(PhysicalCard source,
                              PhysicalCard location,
                              PhysicalCard opponent) {
            super(Phase.DEPLOY, PLAYER);
            this.source = source;
            this.location = location;
            this.opponent = opponent;
        }

        @Override
        public List<PhysicalCard> getAllPermanentCards() {
            return List.of(source, location, opponent);
        }

        @Override
        public List<PhysicalCard> getTopLocations() {
            return List.of(location);
        }

        @Override
        public List<PhysicalCard> getCardsAtLocation(PhysicalCard card) {
            return card == location ? List.of(opponent) : List.of();
        }

        @Override
        public PhysicalCard findCardByPermanentId(Integer permanentCardId) {
            if (Integer.valueOf(1002).equals(permanentCardId)) {
                return source;
            }
            if (Integer.valueOf(3001).equals(permanentCardId)) {
                return location;
            }
            return null;
        }

        @Override
        public List<PhysicalCard> getLostPile(String playerId) {
            return List.of();
        }

        @Override
        public List<PhysicalCard> getReserveDeck(String playerId) {
            return List.of();
        }

        @Override
        public List<PhysicalCard> getForcePile(String playerId) {
            return List.of();
        }
    }

    private static AwaitingDecision decision(int id,
                                             AwaitingDecisionType type,
                                             String text,
                                             Map<String, String[]> parameters) {
        return AbstractActivateControlDecisionHarnessTest.decision(
                id, type, text, parameters);
    }

    private static Map<String, String[]> params(Object... values) {
        return AbstractActivateControlDecisionHarnessTest.params(values);
    }

    private static String[] arr(String... values) {
        return AbstractActivateControlDecisionHarnessTest.arr(values);
    }
}
