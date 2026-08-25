package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.set3.light.Card3_071;
import com.gempukku.swccgo.cards.set207.light.Card207_005;
import com.gempukku.swccgo.cards.set224.light.Card224_011;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeckOracleArmedWeaponHostTest {
    private static final String PLAYER = "light";
    private static final List<String> CUNNING_WARRIOR_TARGETS =
            List.of("anakin's lightsaber", "cloud city corridor");

    @Test
    public void armedLeiaDoesNotMakeCunningWarriorParentPullSafe() {
        Fixture fixture = fixture(true);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle();
        rando.analyze(fixture.game(), PLAYER, Side.LIGHT);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle();
        chosen.analyze(fixture.game(), PLAYER, Side.LIGHT);

        assertTrue(rando.reserveTargetsAreAllUnattachableWeapons(
                fixture.game(), PLAYER, CUNNING_WARRIOR_TARGETS));
        assertTrue(chosen.reserveTargetsAreAllUnattachableWeapons(
                fixture.game(), PLAYER, CUNNING_WARRIOR_TARGETS));
    }

    @Test
    public void unarmedLeiaStillMakesCunningWarriorParentPullSafe() {
        Fixture fixture = fixture(false);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle();
        rando.analyze(fixture.game(), PLAYER, Side.LIGHT);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle();
        chosen.analyze(fixture.game(), PLAYER, Side.LIGHT);

        assertFalse(rando.reserveTargetsAreAllUnattachableWeapons(
                fixture.game(), PLAYER, CUNNING_WARRIOR_TARGETS));
        assertFalse(chosen.reserveTargetsAreAllUnattachableWeapons(
                fixture.game(), PLAYER, CUNNING_WARRIOR_TARGETS));
    }

    @Test
    public void armedLeiaAndUnarmedBenMakeCunningWarriorSafeAfterBenDeploys() {
        Fixture fixture = fixture(true, true);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle();
        rando.analyze(fixture.game(), PLAYER, Side.LIGHT);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle();
        chosen.analyze(fixture.game(), PLAYER, Side.LIGHT);

        assertFalse(rando.reserveTargetsAreAllUnattachableWeapons(
                fixture.game(), PLAYER, CUNNING_WARRIOR_TARGETS));
        assertFalse(chosen.reserveTargetsAreAllUnattachableWeapons(
                fixture.game(), PLAYER, CUNNING_WARRIOR_TARGETS));
    }

    private static Fixture fixture(boolean armed) {
        return fixture(armed, false);
    }

    private static Fixture fixture(boolean armed, boolean includeBen) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard saber = card(
                1, "Anakin's Lightsaber", "3_71", new Card3_071(),
                Zone.RESERVE_DECK);
        PhysicalCard leia = card(
                2, "General Leia Organa", "207_5", new Card207_005(),
                Zone.AT_LOCATION);
        PhysicalCard ben = card(
                3, "Ben Solo", "224_11", new Card224_011(),
                Zone.AT_LOCATION);
        List<PhysicalCard> characters = includeBen
                ? List.of(leia, ben) : List.of(leia);
        PhysicalCard existingWeapon = mock(PhysicalCard.class);
        SwccgCardBlueprint existingBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getReserveDeck(PLAYER)).thenReturn(List.of(saber));
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getLostPile(PLAYER)).thenReturn(List.of());
        when(gameState.getForcePile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(characters);
        when(gameState.getAttachedCards(leia)).thenReturn(
                armed ? List.of(existingWeapon) : List.of());
        when(existingWeapon.getBlueprint()).thenReturn(existingBlueprint);
        when(existingBlueprint.getCardCategory()).thenReturn(CardCategory.WEAPON);
        when(gameState.isCardInPlayActive(eq(existingWeapon),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(true);
        when(modifiers.hasAbilityMoreThan(gameState, leia, 3.0f, false))
                .thenReturn(true);
        when(modifiers.hasPersona(gameState, leia, Persona.LEIA))
                .thenReturn(true);
        when(modifiers.hasAbilityMoreThan(gameState, ben, 3.0f, false))
                .thenReturn(true);
        when(modifiers.hasPersona(gameState, ben, Persona.BEN_SOLO))
                .thenReturn(true);

        doAnswer(invocation -> {
            PhysicalCardVisitor visitor = invocation.getArgument(0);
            for (PhysicalCard character : characters) {
                if (visitor.visitPhysicalCard(character)) {
                    return true;
                }
            }
            return false;
        }).when(gameState).iterateActiveCards(
                any(PhysicalCardVisitor.class), eq(modifiers),
                nullable(PhysicalCard.class), isNull(), isNull());

        return new Fixture(game);
    }

    private static PhysicalCard card(
            int cardId, String title, String blueprintId,
            SwccgCardBlueprint blueprint, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        return card;
    }

    private record Fixture(SwccgGame game) {
    }
}
