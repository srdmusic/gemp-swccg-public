package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControlDrainFactsTest {
    private static final String PLAYER = "player";

    @Test
    public void strandingCheckDoesNotLetCheapAlternativeHideCostlyOne() {
        assertFalse(ControlDrainFacts.wouldStrand(3.0f, 6, 2));
        assertTrue(ControlDrainFacts.wouldStrand(3.0f, 6, 5));
    }

    @Test
    public void absentStockGameObjectsFailClosedWithoutInventingFacts() {
        ControlDrainFacts facts = new ControlDrainFacts(
                null, null, "player", null, 4,
                () -> true, () -> true, () -> true);

        assertNull(facts.primary());
        assertFalse(facts.simpleTricksBlocks());
        ControlDrainAssessment.Economy economy = facts.economy();
        assertTrue(economy.underBattleOrder());
        assertEquals(0, economy.forceAvailable());
        assertFalse(economy.hasDeployableCard());
        assertEquals(4, economy.turnNumber());
        ControlDrainAssessment.DownstreamUses downstream =
                facts.downstreamUses(3.0f);
        assertFalse(downstream.complete());
        assertEquals(Integer.MAX_VALUE, downstream.reserveDeckSize());
        assertFalse(facts.battleOrderCostWaived());
        assertFalse(facts.classicHuntExecutorHardLoss());
        assertNull(facts.battleOrderDrainValue());
        assertNull(facts.multiDrain());
        ControlDrainAssessment.HuntDown huntDown = facts.huntDown();
        assertTrue(huntDown.active());
        assertEquals(0, huntDown.opponentIcons());
    }

    @Test
    public void exactTargetSurchargeStrandsEvenWhenGenericAndOtherTargetStayCheap() {
        Fixture fixture = fixture(CardCategory.CHARACTER,
                true, true, true, true, 2.0f, 5.0f);

        assertEquals(1, fixture.facts().economy().cheapestDeployCost());
        ControlDrainAssessment.DownstreamUses uses =
                fixture.facts().downstreamUses(3.0f);

        assertTrue(uses.complete());
        assertTrue(uses.deployWouldBeStranded());
        assertEquals(6, uses.usableForce());
        assertEquals(12, uses.reserveDeckSize());
    }

    @Test
    public void cardWithNoDeployActionAndNoLegalTargetIsKnownNoUse() {
        Fixture fixture = fixture(CardCategory.CHARACTER,
                false, false, false, true, 2.0f, 5.0f);

        ControlDrainAssessment.DownstreamUses uses =
                fixture.facts().downstreamUses(3.0f);

        assertTrue(uses.complete());
        assertFalse(uses.deployWouldBeStranded());
    }

    @Test
    public void deployActionWithoutEnumerableTargetFailsClosed() {
        Fixture fixture = fixture(CardCategory.CHARACTER,
                true, false, false, true, 2.0f, 5.0f);

        ControlDrainAssessment.DownstreamUses uses =
                fixture.facts().downstreamUses(3.0f);

        assertFalse(uses.complete());
        assertFalse(uses.deployWouldBeStranded());
    }

    @Test
    public void nonFiniteExactTargetCostFailsClosed() {
        Fixture fixture = fixture(CardCategory.CHARACTER,
                true, true, false, true, Float.NaN, 5.0f);

        ControlDrainAssessment.DownstreamUses uses =
                fixture.facts().downstreamUses(3.0f);

        assertFalse(uses.complete());
        assertFalse(uses.deployWouldBeStranded());
    }

    @Test
    public void unpilotedStarshipPackageFailsClosedUntilPairCostIsKnown() {
        Fixture fixture = fixture(CardCategory.STARSHIP,
                true, true, false, false, 2.0f, 5.0f);

        ControlDrainAssessment.DownstreamUses uses =
                fixture.facts().downstreamUses(3.0f);

        assertFalse(uses.complete());
        assertFalse(uses.deployWouldBeStranded());
    }

    private static Fixture fixture(
            CardCategory category,
            boolean hasDeployAction,
            boolean cheapTargetLegal,
            boolean costlyTargetLegal,
            boolean permanentPilot,
            float cheapTargetCost,
            float costlyTargetCost) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        PhysicalCard cheapTarget = target(201);
        PhysicalCard costlyTarget = target(202);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent(PLAYER)).thenReturn("opponent");
        when(gameState.getHand(PLAYER)).thenReturn(List.of(card));
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(cheapTarget, costlyTarget));
        when(gameState.getAllPermanentCards()).thenReturn(
                List.of(cheapTarget, costlyTarget));
        when(gameState.getTopLocations()).thenReturn(List.of());
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(12);
        when(gameState.findCardByPermanentId(101)).thenReturn(card);

        when(card.getPermanentCardId()).thenReturn(101);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.getPersonas()).thenReturn(Collections.emptySet());

        when(modifiers.getForceAvailableToUse(gameState, PLAYER))
                .thenReturn(6);
        when(modifiers.getDeployCost(gameState, card)).thenReturn(1.0f);
        when(modifiers.getDeployCost(gameState, card, card, cheapTarget,
                false, null, false, 0.0f, null, true))
                .thenReturn(cheapTargetCost);
        when(modifiers.getDeployCost(gameState, card, card, costlyTarget,
                false, null, false, 0.0f, null, true))
                .thenReturn(costlyTargetCost);
        when(modifiers.hasPermanentPilot(gameState, card))
                .thenReturn(permanentPilot);
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), anyBoolean(),
                anyFloat())).thenReturn(hasDeployAction);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenAnswer(invocation -> {
            Filter targetFilter = invocation.getArgument(4);
            return cheapTargetLegal
                    && targetFilter.accepts(
                            gameState, modifiers, cheapTarget)
                    || costlyTargetLegal
                    && targetFilter.accepts(
                            gameState, modifiers, costlyTarget);
        });

        ControlDrainFacts facts = new ControlDrainFacts(
                gameState, game, PLAYER, null, 4,
                () -> true, () -> false, () -> false);
        return new Fixture(facts);
    }

    private static PhysicalCard target(int cardId) {
        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getCardId()).thenReturn(cardId);
        when(target.getAdditionalCardIds()).thenReturn(List.of());
        when(target.getZone()).thenReturn(Zone.LOCATIONS);
        return target;
    }

    private record Fixture(ControlDrainFacts facts) {
    }
}
