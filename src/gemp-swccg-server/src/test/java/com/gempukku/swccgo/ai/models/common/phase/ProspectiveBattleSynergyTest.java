package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.set204.light.Card204_011;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProspectiveBattleSynergyTest {
    private static final String DARK = "dark";
    private static final String LIGHT = "light";

    @Test
    public void tarkinVirtualPrintsRecognizeVaderExactlyOnce() {
        Fixture fixture = fixture();
        PhysicalCard vader = fixture.personaCard(
                3, "1_168", DARK, Persona.VADER);

        for (String tarkinBlueprint : List.of("200_82", "601_86")) {
            PhysicalCard tarkin = fixture.card(
                    1, tarkinBlueprint, DARK);
            ProspectiveBattleSynergy.Assessment assessment =
                    fixture.assess(
                            List.of(tarkin, tarkin, vader),
                            List.of(tarkin, vader));

            assertEquals(1,
                    assessment.additionalBattleDestinyDraws());
            assertEquals(1, assessment.matches().size());
            assertEquals(
                    ProspectiveBattleSynergy.RuleId
                        .TARKIN_V_WITH_VADER_OR_IMPERIAL_LEADER,
                    assessment.matches().get(0).ruleId());
        }
    }

    @Test
    public void tarkinDoesNotMatchHimselfOrWrongPrinting() {
        Fixture fixture = fixture();
        PhysicalCard tarkin = fixture.card(1, "200_82", DARK);
        PhysicalCard originalTarkin = fixture.card(2, "1_179", DARK);
        when(fixture.modifiers.hasIcon(
                fixture.gameState, tarkin,
                com.gempukku.swccgo.common.Icon.IMPERIAL))
                .thenReturn(true);
        when(fixture.modifiers.hasKeyword(
                fixture.gameState, tarkin,
                com.gempukku.swccgo.common.Keyword.LEADER))
                .thenReturn(true);

        assertEquals(0, fixture.assess(
                List.of(tarkin), List.of(tarkin))
                .additionalBattleDestinyDraws());
        assertEquals(0, fixture.assess(
                List.of(originalTarkin), List.of(originalTarkin))
                .additionalBattleDestinyDraws());
    }

    @Test
    public void soloVirtualCountsOnlyItsAddedDrawNotFallback() {
        Fixture fixture = fixture();
        PhysicalCard solo = fixture.card(1, "204_11", LIGHT);
        PhysicalCard chewie = fixture.personaCard(
                2, "1_1", LIGHT, Persona.CHEWIE);

        assertEquals(0, fixture.assessFor(
                LIGHT, List.of(solo), List.of(solo))
                .additionalBattleDestinyDraws());
        assertEquals(1, fixture.assessFor(
                LIGHT, List.of(solo), List.of(solo, chewie))
                .additionalBattleDestinyDraws());
    }

    @Test
    public void colocatedPartnerNeedNotBeAProjectedParticipant() {
        Fixture fixture = fixture();
        PhysicalCard leia = fixture.card(1, "201_5", LIGHT);
        PhysicalCard opponentVader = fixture.personaCard(
                2, "1_168", DARK, Persona.VADER);

        assertEquals(1, fixture.assessFor(
                LIGHT, List.of(leia),
                List.of(leia, opponentVader))
                .additionalBattleDestinyDraws());
    }

    @Test
    public void marginalCountsOnlyANewlyCompletedPair() {
        Fixture fixture = fixture();
        PhysicalCard tarkin = fixture.card(1, "200_82", DARK);
        PhysicalCard vader = fixture.personaCard(
                2, "1_168", DARK, Persona.VADER);
        PhysicalCard unrelated = fixture.card(3, "1_42", DARK);

        ProspectiveBattleSynergy.Projection empty = fixture.projection(
                DARK, List.of(tarkin), List.of(tarkin));
        ProspectiveBattleSynergy.Projection complete = fixture.projection(
                DARK, List.of(tarkin, vader), List.of(tarkin, vader));
        ProspectiveBattleSynergy.Projection unchanged = fixture.projection(
                DARK, List.of(tarkin, vader, unrelated),
                List.of(tarkin, vader, unrelated));

        assertEquals(1, ProspectiveBattleSynergy
                .additionalDrawsGained(
                        fixture.game, empty, complete));
        assertEquals(0, ProspectiveBattleSynergy
                .additionalDrawsGained(
                        fixture.game, complete, unchanged));
    }

    @Test
    public void canceledOrPreventedSourceStaysNeutral() {
        Fixture fixture = fixture();
        PhysicalCard tarkin = fixture.card(
                1, "200_82", DARK, Zone.AT_LOCATION);
        PhysicalCard vader = fixture.personaCard(
                2, "1_168", DARK, Persona.VADER);
        when(fixture.modifiers.isGameTextCanceled(
                fixture.gameState, tarkin)).thenReturn(true);

        assertEquals(0, fixture.assess(
                List.of(tarkin, vader), List.of(tarkin, vader))
                .additionalBattleDestinyDraws());
    }

    @Test
    public void passiveRegistryEntryFailsClosedIfBlueprintModifierDrifts() {
        Fixture fixture = fixture();
        PhysicalCard solo = fixture.card(1, "204_11", LIGHT);
        PhysicalCard chewie = fixture.personaCard(
                2, "1_1", LIGHT, Persona.CHEWIE);
        when(solo.getBlueprint().getWhileInPlayModifiers(
                fixture.game, solo)).thenReturn(List.of());

        assertEquals(0, fixture.assessFor(
                LIGHT, List.of(solo), List.of(solo, chewie))
                .additionalBattleDestinyDraws());
    }

    @Test
    public void passiveEntryIsBoundToTheRealCardModifierContract() {
        Fixture fixture = fixture();
        PhysicalCard solo = fixture.card(1, "204_11", LIGHT);
        PhysicalCard chewie = fixture.personaCard(
                2, "1_1", LIGHT, Persona.CHEWIE);
        when(solo.getBlueprint()).thenReturn(new Card204_011());

        assertEquals(1, fixture.assessFor(
                LIGHT, List.of(solo), List.of(solo, chewie))
                .additionalBattleDestinyDraws());
    }

    private static Fixture fixture() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        return new Fixture(game, gameState, modifiers);
    }

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers) {
        private PhysicalCard card(
                int permanentId,
                String blueprintId,
                String owner) {
            return card(permanentId, blueprintId, owner, Zone.HAND);
        }

        private PhysicalCard card(
                int permanentId,
                String blueprintId,
                String owner,
                Zone zone) {
            PhysicalCard card = mock(PhysicalCard.class);
            SwccgCardBlueprint blueprint =
                    mock(SwccgCardBlueprint.class);
            Modifier addedDraw = mock(Modifier.class);
            when(card.getPermanentCardId()).thenReturn(permanentId);
            when(card.getBlueprintId(true)).thenReturn(blueprintId);
            when(card.getOwner()).thenReturn(owner);
            when(card.getZone()).thenReturn(zone);
            when(card.getBlueprint()).thenReturn(blueprint);
            when(blueprint.getWhileInPlayModifiers(
                    game, card)).thenReturn(List.of(addedDraw));
            when(addedDraw.getModifierType()).thenReturn(
                    ModifierType.NUM_BATTLE_DESTINY_DRAWS);
            when(addedDraw.getValue(
                    gameState, modifiers,
                    (PhysicalCard) null)).thenReturn(1.0f);
            return card;
        }

        private PhysicalCard personaCard(
                int permanentId,
                String blueprintId,
                String owner,
                Persona persona) {
            PhysicalCard card = card(
                    permanentId, blueprintId, owner);
            when(modifiers.hasPersona(
                    gameState, card, persona)).thenReturn(true);
            return card;
        }

        private ProspectiveBattleSynergy.Assessment assess(
                List<PhysicalCard> participants,
                List<PhysicalCard> colocated) {
            return assessFor(DARK, participants, colocated);
        }

        private ProspectiveBattleSynergy.Assessment assessFor(
                String playerId,
                List<PhysicalCard> participants,
                List<PhysicalCard> colocated) {
            return ProspectiveBattleSynergy.assess(
                    game, projection(
                            playerId, participants, colocated));
        }

        private ProspectiveBattleSynergy.Projection projection(
                String playerId,
                List<PhysicalCard> participants,
                List<PhysicalCard> colocated) {
            return new ProspectiveBattleSynergy.Projection(
                    playerId, participants, colocated);
        }
    }
}
