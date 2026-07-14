package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceReserveService;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DrawReserveLegacyReaderTest {

    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final Logger LOGGER = LogManager.getLogger(DrawReserveLegacyReaderTest.class);

    @Test
    public void combinesFactsBoardAndTurnComponentsInLegacySequence() {
        PhysicalCard contested = location("Contested Site");
        RecordingGameState state = state(List.of(contested))
                .cardsAt(contested, card(PLAYER, null), card(OPPONENT, null));

        int reserve = calculate(state, 4, facts(true, true, true, 2, true), false);

        assertEquals(9, reserve);
    }

    @Test
    public void multipleContestedLocationsCollapseToOneReserve() {
        PhysicalCard first = location("First Contested Site");
        PhysicalCard second = location("Second Contested Site");
        RecordingGameState state = state(List.of(first, second))
                .cardsAt(first, card(PLAYER, null), card(OPPONENT, null))
                .cardsAt(second, card(PLAYER, null), card(OPPONENT, null));

        int reserve = calculate(state, 1, facts(false, false, false, 0, false), false);

        assertEquals(1, reserve);
    }

    @Test
    public void corridorCountsEveryFriendlyCharacterAfterBaseCap() {
        PhysicalCard firstCorridor = location("Mapuzo: Underground Corridor");
        PhysicalCard street = location("Mapuzo: Streets");
        PhysicalCard secondCorridor = location("Underground Corridor: Exit");
        RecordingGameState state = state(List.of(firstCorridor, street, secondCorridor))
                .cardsAt(firstCorridor,
                        card(PLAYER, CardCategory.CHARACTER),
                        card(PLAYER, CardCategory.CHARACTER),
                        card(PLAYER, CardCategory.VEHICLE),
                        card(OPPONENT, CardCategory.CHARACTER))
                .cardsAt(street, card(PLAYER, CardCategory.CHARACTER))
                .cardsAt(secondCorridor,
                        card(PLAYER, CardCategory.CHARACTER),
                        card(PLAYER, CardCategory.CHARACTER),
                        card(PLAYER, null));

        int reserve = calculate(state, 1, facts(false, false, false, 20, false), true);

        assertEquals(14, reserve);
        assertEquals(2, state.locationReadCount);
    }

    @Test
    public void hiddenPathReadsLocationsAgainAndUsesEachReadOrder() {
        PhysicalCard first = location("First Scan A");
        PhysicalCard second = location("First Scan B");
        PhysicalCard corridorB = location("Underground Corridor B");
        PhysicalCard corridorA = location("Underground Corridor A");
        RecordingGameState state = state(
                List.of(first, second),
                List.of(corridorB, corridorA))
                .cardsAt(corridorB, card(PLAYER, CardCategory.CHARACTER))
                .cardsAt(corridorA, card(PLAYER, CardCategory.CHARACTER));

        int reserve = calculate(state, 1, facts(false, false, false, 0, false), true);

        assertEquals(2, reserve);
        assertEquals(2, state.locationReadCount);
        assertEquals(List.of(
                "First Scan A",
                "First Scan B",
                "Underground Corridor B",
                "Underground Corridor A"), state.cardsAtLocationOrder);
    }

    @Test
    public void corridorExceptionPreservesCappedBase() {
        PhysicalCard site = location("Ordinary Site");
        RecordingGameState state = state(List.of(site)).failOnLocationRead(2);

        int reserve = calculate(state, 1, facts(false, false, false, 20, false), true);

        assertEquals(10, reserve);
        assertEquals(2, state.locationReadCount);
    }

    @Test
    public void outerExceptionReturnsFallbackOne() {
        RecordingGameState state = state(List.of()).failOnLocationRead(1);
        AtomicBoolean factsRead = new AtomicBoolean();
        AtomicBoolean hiddenPathRead = new AtomicBoolean();
        Supplier<ForceReserveService.Facts> reserveFacts = () -> {
            factsRead.set(true);
            return facts(false, false, false, 0, false);
        };
        BooleanSupplier hiddenPath = () -> {
            hiddenPathRead.set(true);
            return true;
        };

        int reserve = DrawReserveLegacyReader.calculate(
                state, PLAYER, 1, reserveFacts, hiddenPath, LOGGER);

        assertEquals(1, reserve);
        assertFalse(factsRead.get());
        assertFalse(hiddenPathRead.get());
    }

    private static int calculate(RecordingGameState state,
                                 int turnNumber,
                                 ForceReserveService.Facts reserveFacts,
                                 boolean hiddenPathUnflipped) {
        return DrawReserveLegacyReader.calculate(
                state,
                PLAYER,
                turnNumber,
                () -> reserveFacts,
                () -> hiddenPathUnflipped,
                LOGGER);
    }

    private static ForceReserveService.Facts facts(boolean drawTheirFire,
                                                   boolean firstStrike,
                                                   boolean imperialArrestOrder,
                                                   int maintenanceObligation,
                                                   boolean vergeNeedsDeathStarMove) {
        try {
            Constructor<ForceReserveService.Facts> constructor =
                    ForceReserveService.Facts.class.getDeclaredConstructor(
                            boolean.class,
                            boolean.class,
                            boolean.class,
                            boolean.class,
                            int.class,
                            int.class,
                            int.class,
                            boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(
                    drawTheirFire,
                    firstStrike,
                    imperialArrestOrder,
                    false,
                    maintenanceObligation,
                    0,
                    0,
                    vergeNeedsDeathStarMove);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to construct reserve facts", e);
        }
    }

    private static PhysicalCard location(String title) {
        return physicalCard(title, null, null);
    }

    private static PhysicalCard card(String owner, CardCategory category) {
        return physicalCard("Card", owner, category);
    }

    private static PhysicalCard physicalCard(String title,
                                             String owner,
                                             CardCategory category) {
        SwccgCardBlueprint blueprint = category == null ? null : blueprint(category);
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getTitle":
                    return title;
                case "getOwner":
                    return owner;
                case "getBlueprint":
                    return blueprint;
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return title;
                default:
                    throw new UnsupportedOperationException("Unexpected PhysicalCard call: "
                            + method.getName());
            }
        };
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                handler);
    }

    private static SwccgCardBlueprint blueprint(CardCategory category) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getCardCategory".equals(method.getName())) {
                return category;
            }
            throw new UnsupportedOperationException("Unexpected blueprint call: "
                    + method.getName());
        };
        return (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                handler);
    }

    @SafeVarargs
    private static RecordingGameState state(List<PhysicalCard>... locationReads) {
        return new RecordingGameState(Arrays.asList(locationReads));
    }

    private static final class RecordingGameState extends GameState {
        private final List<List<PhysicalCard>> locationReads;
        private final Map<PhysicalCard, List<PhysicalCard>> cardsByLocation =
                new IdentityHashMap<>();
        private final List<String> cardsAtLocationOrder = new ArrayList<>();
        private int locationReadCount;
        private int failingLocationRead = -1;

        private RecordingGameState(List<List<PhysicalCard>> locationReads) {
            this.locationReads = locationReads;
        }

        private RecordingGameState cardsAt(PhysicalCard location, PhysicalCard... cards) {
            cardsByLocation.put(location, Arrays.asList(cards));
            return this;
        }

        private RecordingGameState failOnLocationRead(int readNumber) {
            failingLocationRead = readNumber;
            return this;
        }

        @Override
        public List<PhysicalCard> getLocationsInOrder() {
            locationReadCount++;
            if (locationReadCount == failingLocationRead) {
                throw new IllegalStateException("location read " + locationReadCount + " failed");
            }
            int responseIndex = Math.min(locationReadCount - 1, locationReads.size() - 1);
            return locationReads.get(responseIndex);
        }

        @Override
        public List<PhysicalCard> getCardsAtLocation(PhysicalCard location) {
            cardsAtLocationOrder.add(location.getTitle());
            return cardsByLocation.getOrDefault(location, List.of());
        }
    }
}
