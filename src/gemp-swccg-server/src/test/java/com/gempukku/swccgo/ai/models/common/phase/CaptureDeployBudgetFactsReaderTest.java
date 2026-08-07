package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.actions.PlayCharacterAction;
import com.gempukku.swccgo.cards.actions.PlayStarshipOrVehicleAction;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.StandardEffect;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureDeployBudgetFactsReaderTest {
    private static final String PLAYER = "player";

    @Test
    public void usesLiveTargetBaseAndExtraInsteadOfPrintedCost() {
        Fixture fixture = fixture(2.0f);
        fixture.allow(fixture.first, 2.5f, 1);

        assertEquals(
                Integer.valueOf(4),
                fixture.read());
    }

    @Test
    public void usesMaximumPaymentAcrossLegalTargets() {
        Fixture fixture = fixture(2.0f);
        fixture.allow(fixture.first, 1.25f, 0);
        fixture.allow(fixture.second, 3.1f, 2);

        assertEquals(
                Integer.valueOf(6),
                fixture.read());
    }

    @Test
    public void invalidCandidateFailsClosed() {
        Fixture fixture = fixture(2.0f);
        when(fixture.card.getOwner())
                .thenReturn("opponent");

        assertNull(fixture.read());

        when(fixture.card.getOwner())
                .thenReturn(PLAYER);
        when(fixture.card.getBlueprint()
                .getCardCategory())
                .thenReturn(CardCategory.EFFECT);

        assertNull(fixture.read());
    }

    @Test
    public void noLegalTargetFailsClosed() {
        Fixture fixture = fixture(2.0f);

        assertNull(fixture.read());
    }

    @Test
    public void nonFiniteLiveCostFailsClosed() {
        Fixture fixture = fixture(2.0f);
        fixture.allow(
                fixture.first, Float.NaN, 0);

        assertNull(fixture.read());
    }

    @Test
    public void snapshotsOnlyExactOrdinarySelfSourceAction() {
        Fixture fixture = fixture(2.0f);
        fixture.allow(fixture.first, 2.5f, 1);
        Action normal = new PlayCharacterAction(
                fixture.game,
                fixture.card,
                fixture.card,
                false, 0.0f,
                null, null, Filters.any);

        Object snapshot =
                CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision(normal),
                        fixture.game,
                        PLAYER);
        assertEquals(
                Integer.valueOf(4),
                CaptureDeployBudgetFactsReader
                    .actionPayment(snapshot, "0"));

        when(fixture.card.getBlueprint()
                .getSpecialDeployCostEffect(
                    same(normal), eq(PLAYER),
                    same(fixture.game),
                    same(fixture.card),
                    same(fixture.first),
                    isNull()))
                .thenReturn(mock(StandardEffect.class));
        Object specialCostUnknown =
                CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision(normal),
                        fixture.game,
                        PLAYER);
        assertNull(
                CaptureDeployBudgetFactsReader
                    .actionPayment(
                        specialCostUnknown, "0"));

        PhysicalCard otherSource = card(
                20, PLAYER, Zone.AT_LOCATION,
                CardCategory.EFFECT);
        Action specialSource =
                new PlayCharacterAction(
                    fixture.game,
                    otherSource,
                    fixture.card,
                    false, 0.0f,
                    null, null, Filters.any);
        Object unknown =
                CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision(specialSource),
                        fixture.game,
                        PLAYER);
        assertNull(
                CaptureDeployBudgetFactsReader
                    .actionPayment(unknown, "0"));

        Action changedCost =
                new PlayCharacterAction(
                    fixture.game,
                    fixture.card,
                    fixture.card,
                    false, 1.0f,
                    null, null, Filters.any);
        Object changedCostUnknown =
                CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision(changedCost),
                        fixture.game,
                        PLAYER);
        assertNull(
                CaptureDeployBudgetFactsReader
                    .actionPayment(
                        changedCostUnknown, "0"));
    }

    @Test
    public void starshipDeployFailsClosedBeforeAnyPilotChoice() {
        Fixture fixture = fixture(2.0f);
        when(fixture.card.getBlueprint()
                .getCardCategory())
                .thenReturn(CardCategory.STARSHIP);
        Action starship = new PlayStarshipOrVehicleAction(
                fixture.game,
                fixture.card,
                fixture.card,
                false, 0.0f,
                null, null,
                Filters.any, Filters.any,
                List.of());

        Object snapshot =
                CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision(starship),
                        fixture.game,
                        PLAYER);

        assertNull(CaptureDeployBudgetFactsReader
                .actionPayment(snapshot, "0"));
    }

    private static Fixture fixture(float printedCost) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        PhysicalCard card = card(
                10, PLAYER, Zone.HAND,
                CardCategory.CHARACTER);
        PhysicalCard first = card(
                101, null, Zone.LOCATIONS,
                CardCategory.LOCATION);
        PhysicalCard second = card(
                102, null, Zone.AT_LOCATION,
                CardCategory.VEHICLE);
        Set<PhysicalCard> legal = new HashSet<>();

        when(card.getBlueprint().getDeployCost())
                .thenReturn(printedCost);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    card, first, second));
        when(modifiers.isDeployableToTarget(
                same(gameState),
                same(card),
                same(card),
                eq(false),
                any(Filter.class),
                eq(false),
                eq(0.0f),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(false),
                eq(0.0f)))
                .thenAnswer(invocation -> {
                    Filter targetFilter =
                            invocation.getArgument(4);
                    for (PhysicalCard target : legal) {
                        if (targetFilter.accepts(
                                gameState, modifiers,
                                target)) {
                            return true;
                        }
                    }
                    return false;
                });

        return new Fixture(
                game, gameState, modifiers,
                card, first, second, legal);
    }

    private static PhysicalCard card(
            int id,
            String owner,
            Zone zone,
            CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(card.getPermanentCardId())
                .thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds())
                .thenReturn(List.of());
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle()).thenReturn("Card " + id);
        when(blueprint.getTitle()).thenReturn("Card " + id);
        when(blueprint.getCardCategory())
                .thenReturn(category);
        return card;
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

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard card,
            PhysicalCard first,
            PhysicalCard second,
            Set<PhysicalCard> legal) {
        private void allow(
                PhysicalCard target,
                float base,
                int extra) {
            legal.add(target);
            when(modifiers.getDeployCost(
                    gameState,
                    card,
                    card,
                    target,
                    false,
                    null,
                    false,
                    0.0f,
                    null,
                    false))
                    .thenReturn(base);
            when(modifiers
                    .getExtraForceRequiredToDeployToTarget(
                        gameState,
                        card,
                        target,
                        null,
                        card,
                        false))
                    .thenReturn(extra);
        }

        private Integer read() {
            return CaptureDeployBudgetFactsReader
                    .maximumExactNormalDeployPayment(
                        game, PLAYER, card);
        }
    }
}
