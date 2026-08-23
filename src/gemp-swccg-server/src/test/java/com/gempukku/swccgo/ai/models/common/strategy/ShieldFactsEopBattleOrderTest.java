package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.set8.dark.Card8_157;
import com.gempukku.swccgo.cards.set8.dark.Card8_166;
import com.gempukku.swccgo.cards.set8.dark.Card8_167;
import com.gempukku.swccgo.cards.set13.dark.Card13_054;
import com.gempukku.swccgo.cards.set13.light.Card13_008;
import com.gempukku.swccgo.cards.set200.dark.Card200_110;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShieldFactsEopBattleOrderTest {
    private static final String PLAYER = "tester";
    private static final String OPPONENT = "opponent";

    @Test
    public void openEndorRouteReservesSlotFourAfterThreeShields() {
        Fixture fixture = new Fixture();

        assertTrue("objective owner", Filters.owner(PLAYER).accepts(
                fixture.gameState, fixture.modifiers, fixture.objective));
        assertTrue("objective type", Filters.Objective.accepts(
                fixture.gameState, fixture.modifiers, fixture.objective));
        assertTrue("objective title", Filters.title("Endor Operations").accepts(
                fixture.gameState, fixture.modifiers, fixture.objective));
        assertTrue("EOP objective", Filters.canSpot(fixture.game, null,
                Filters.and(Filters.owner(PLAYER), Filters.Objective,
                        Filters.title("Endor Operations"))));
        assertTrue("occupied Endor site", Filters.canSpot(fixture.game, null,
                Filters.and(Filters.occupies(PLAYER), Filters.battleground_site,
                        Filters.Endor_site)));
        assertTrue("Endor system on table", Filters.canSpot(fixture.game, null,
                Filters.and(Filters.Endor_system, Filters.battleground_system)));
        assertTrue(ShieldFacts.shouldReserveEopBattleOrderSlot(
                fixture.game, PLAYER, fixture.knowledgeAndDefense, 3));
    }

    @Test
    public void everyReservationBoundaryFailsClosed() {
        assertFalse(new Fixture().reserveWithShieldCount(1));
        assertFalse(new Fixture().reserveWithShieldCount(2));
        assertFalse(new Fixture().reserveWithShieldCount(4));

        Fixture noObjective = new Fixture();
        noObjective.active.remove(noObjective.objective);
        assertFalse(noObjective.reserve());

        Fixture enemyEndor = new Fixture();
        when(enemyEndor.modifiers.occupiesLocation(
                enemyEndor.gameState, enemyEndor.endor, OPPONENT))
                .thenReturn(true);
        assertFalse(enemyEndor.reserve());

        Fixture equivalentActive = new Fixture();
        equivalentActive.active.add(equivalentActive.activeBattlePlan);
        assertFalse(equivalentActive.reserve());

        Fixture noBattleOrder = new Fixture();
        when(noBattleOrder.gameState.getStackedCards(
                noBattleOrder.knowledgeAndDefense)).thenReturn(List.of());
        assertFalse(noBattleOrder.reserve());

        Fixture noEndorSite = new Fixture();
        when(noEndorSite.modifiers.occupiesLocation(
                noEndorSite.gameState, noEndorSite.landingPlatform, PLAYER))
                .thenReturn(false);
        assertFalse(noEndorSite.reserve());
    }

    @Test
    public void ownEndorSystemOccupationReleasesReservation() {
        Fixture fixture = new Fixture();
        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, fixture.endor, PLAYER)).thenReturn(true);

        assertFalse(fixture.reserve());
    }

    @Test
    public void battleOrderLiveUsesTaxValueWhenSelfLacksBothTheaters() {
        Fixture fixture = new Fixture();

        // Callers pass true only once the post-turn-2 actual net drain gap is
        // at least 2. Battle Order has no play prerequisite. It taxes both
        // players, so that pressure is worth accepting while the opponent is
        // not exempt even when we are not exempt either.
        assertTrue("Actual net drain gap 2+ justifies accepting our own tax",
                ShieldFacts.battleOrderLive(fixture.game, PLAYER, true));
        assertFalse("Gap below 2 stays closed while we also lack both theaters",
                ShieldFacts.battleOrderLive(fixture.game, PLAYER, false));

        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, fixture.endor, PLAYER)).thenReturn(true);
        assertTrue("Self-exemption keeps Battle Order live without a drain gap",
                ShieldFacts.battleOrderLive(fixture.game, PLAYER, false));

        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, fixture.landingPlatform, OPPONENT)).thenReturn(true);
        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, fixture.endor, OPPONENT)).thenReturn(true);
        assertFalse("Do not play Battle Order once opponent also occupies both theaters",
                ShieldFacts.battleOrderLive(fixture.game, PLAYER, true));
    }

    private static final class Fixture {
        private final GameState gameState = mock(GameState.class);
        private final ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        private final SwccgGame game = mock(SwccgGame.class);
        private final List<PhysicalCard> active = new ArrayList<>();

        private final PhysicalCard objective =
                active(new Card8_167(), "8_167", PLAYER, Zone.SIDE_OF_TABLE);
        private final PhysicalCard landingPlatform =
                active(new Card8_166(), "8_166", PLAYER, Zone.LOCATIONS);
        private final PhysicalCard endor =
                active(new Card8_157(), "8_157", PLAYER, Zone.LOCATIONS);
        private final PhysicalCard knowledgeAndDefense =
                active(new Card200_110(), "200_110", PLAYER, Zone.SIDE_OF_TABLE);
        private final PhysicalCard battleOrder =
                card(new Card13_054(), "13_54", PLAYER, Zone.STACKED);
        private final PhysicalCard activeBattlePlan =
                card(new Card13_008(), "13_8", OPPONENT, Zone.SIDE_OF_TABLE);

        private Fixture() {
            when(game.getGameState()).thenReturn(gameState);
            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
            when(gameState.getStackedCards(knowledgeAndDefense))
                    .thenReturn(List.of(battleOrder));
            when(gameState.getHand(PLAYER)).thenReturn(List.of());

            when(modifiers.isBattleground(gameState, landingPlatform, null))
                    .thenReturn(true);
            when(modifiers.isBattleground(gameState, endor, null))
                    .thenReturn(true);
            when(modifiers.occupiesLocation(
                    gameState, landingPlatform, PLAYER)).thenReturn(true);
            when(modifiers.occupiesLocation(
                    gameState, endor, PLAYER)).thenReturn(false);
            when(modifiers.occupiesLocation(
                    gameState, endor, OPPONENT)).thenReturn(false);

            when(gameState.iterateActiveCards(
                    any(PhysicalCardVisitor.class),
                    any(ModifiersQuerying.class),
                    any(), any(), any()))
                    .thenAnswer(invocation -> {
                        PhysicalCardVisitor visitor = invocation.getArgument(0);
                        for (PhysicalCard card : new ArrayList<>(active)) {
                            if (visitor.visitPhysicalCard(card)) {
                                return true;
                            }
                        }
                        return false;
                    });
        }

        private boolean reserve() {
            return reserveWithShieldCount(3);
        }

        private boolean reserveWithShieldCount(int shieldCount) {
            return ShieldFacts.shouldReserveEopBattleOrderSlot(
                    game, PLAYER, knowledgeAndDefense, shieldCount);
        }

        private PhysicalCard active(
                SwccgCardBlueprint blueprint, String blueprintId,
                String owner, Zone zone) {
            PhysicalCard card = card(blueprint, blueprintId, owner, zone);
            active.add(card);
            return card;
        }

        private PhysicalCard card(
                SwccgCardBlueprint blueprint, String blueprintId,
                String owner, Zone zone) {
            PhysicalCard card = mock(PhysicalCard.class);
            when(card.getBlueprint()).thenReturn(blueprint);
            when(modifiers.getCardTypes(gameState, card))
                    .thenReturn(blueprint.getCardTypes());
            when(card.getBlueprintId(true)).thenReturn(blueprintId);
            when(card.getTitle()).thenReturn(blueprint.getTitle());
            when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
            when(card.getOwner()).thenReturn(owner);
            when(card.getZone()).thenReturn(zone);
            if ("8_166".equals(blueprintId)) {
                when(card.getPartOfSystem()).thenReturn(Title.Endor);
            }
            return card;
        }
    }
}
