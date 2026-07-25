package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForceLossFactsTest {

    @Test
    public void nullReadsPreserveLegacyZeroAndOtherFallbacks() {
        assertEquals(new ForceLossFacts.DecisionFacts(0, 0, 0, 0, 7, false),
                ForceLossFacts.readDecision(null, "tester", 7));

        ForceLossFacts.CandidateFacts candidate =
                ForceLossFacts.readCandidate(null, "tester", null);
        assertNull(candidate.title());
        assertEquals("", candidate.zoneName());
        assertEquals(ForceLossFacts.ZoneBand.OTHER, candidate.zoneBand());
        assertNull(candidate.category());
        assertFalse(candidate.duplicate());
        assertFalse(candidate.senator());
        assertFalse(candidate.battleInterrupt());
        assertFalse(candidate.hasWielder());
        assertFalse(candidate.priorityCard());
    }

    @Test
    public void decisionReadCountsLifeForceAndOpponentDrawTheirFire() {
        GameState gameState = mock(GameState.class);
        when(gameState.getHand("tester")).thenReturn(cards(2));
        when(gameState.getReserveDeckSize("tester")).thenReturn(5);
        when(gameState.getUsedPile("tester")).thenReturn(cards(3));
        when(gameState.getForcePileSize("tester")).thenReturn(4);
        when(gameState.getOpponent("tester")).thenReturn("opponent");

        SwccgCardBlueprint drawTheirFire = blueprint(
                "Draw Their Fire", CardCategory.EFFECT, null, false);
        PhysicalCard activeEffect = card(
                "Draw Their Fire", Zone.SIDE_OF_TABLE, drawTheirFire);
        when(activeEffect.getOwner()).thenReturn("opponent");
        when(gameState.getAllPermanentCards()).thenReturn(List.of(activeEffect));

        assertEquals(new ForceLossFacts.DecisionFacts(2, 5, 12, 4, 6, true),
                ForceLossFacts.readDecision(gameState, "tester", 6));
    }

    @Test
    public void candidateReadKeepsDuplicateSenatorBattleInterruptAndPriorityFacts() {
        GameState gameState = mock(GameState.class);
        SwccgCardBlueprint blueprint = blueprint(
                "Houjix", CardCategory.INTERRUPT, "During battle, cancel battle destiny.", true);
        PhysicalCard candidate = card("Houjix", Zone.HAND, blueprint);
        PhysicalCard secondCopy = card("Houjix", Zone.HAND, blueprint);
        when(gameState.getHand("tester")).thenReturn(List.of(candidate, secondCopy));
        when(gameState.getAllPermanentCards()).thenReturn(List.of());

        ForceLossFacts.CandidateFacts facts =
                ForceLossFacts.readCandidate(gameState, "tester", candidate);

        assertEquals("Houjix", facts.title());
        assertEquals("HAND", facts.zoneName());
        assertEquals(ForceLossFacts.ZoneBand.HAND, facts.zoneBand());
        assertEquals(CardCategory.INTERRUPT, facts.category());
        assertTrue(facts.duplicate());
        assertTrue(facts.senator());
        assertTrue(facts.battleInterrupt());
        assertFalse(facts.hasWielder());
        assertTrue(facts.priorityCard());
    }

    @Test
    public void weaponWielderFactDeliberatelyAcceptsAnyCharacterInHand() {
        GameState gameState = mock(GameState.class);
        PhysicalCard weapon = card("Unmatched Blaster", Zone.HAND,
                blueprint("Unmatched Blaster", CardCategory.WEAPON, null, false));
        PhysicalCard unrelatedCharacter = card("Unrelated Character", Zone.HAND,
                blueprint("Unrelated Character", CardCategory.CHARACTER, null, false));
        when(gameState.getHand("tester")).thenReturn(List.of(weapon, unrelatedCharacter));
        when(gameState.getAllPermanentCards()).thenReturn(List.of());

        ForceLossFacts.CandidateFacts facts =
                ForceLossFacts.readCandidate(gameState, "tester", weapon);

        assertFalse(facts.duplicate());
        assertTrue(facts.hasWielder());
    }

    @Test
    public void forceLossZoneClassifierIncludesEveryEngineOfferedBand() {
        assertTrue(ForceLossFacts.isForceLossZone(zoneCard(Zone.HAND)));
        assertTrue(ForceLossFacts.isForceLossZone(zoneCard(Zone.USED_PILE)));
        assertTrue(ForceLossFacts.isForceLossZone(zoneCard(Zone.RESERVE_DECK)));
        assertTrue(ForceLossFacts.isForceLossZone(zoneCard(Zone.FORCE_PILE)));
        assertEquals(ForceLossFacts.ZoneBand.UNRESOLVED_DESTINY,
                ForceLossFacts.readCandidate(
                        null, "tester",
                        zoneCard(Zone.TOP_OF_UNRESOLVED_DESTINY_DRAW))
                        .zoneBand());
        assertEquals(ForceLossFacts.ZoneBand.SABACC,
                ForceLossFacts.readCandidate(
                        null, "tester", zoneCard(Zone.SABACC_HAND))
                        .zoneBand());
        assertEquals(ForceLossFacts.ZoneBand.SABACC,
                ForceLossFacts.readCandidate(
                        null, "tester",
                        zoneCard(Zone.REVEALED_SABACC_HAND))
                        .zoneBand());
        assertTrue(ForceLossFacts.isForceLossZone(
                zoneCard(Zone.TOP_OF_UNRESOLVED_DESTINY_DRAW)));
        assertTrue(ForceLossFacts.isForceLossZone(
                zoneCard(Zone.SABACC_HAND)));
        assertTrue(ForceLossFacts.isForceLossZone(
                zoneCard(Zone.REVEALED_SABACC_HAND)));
        assertFalse(ForceLossFacts.isForceLossZone(zoneCard(Zone.LOST_PILE)));
        assertFalse(ForceLossFacts.isForceLossZone(zoneCard(Zone.AT_LOCATION)));
        assertFalse(ForceLossFacts.isForceLossZone(null));
    }

    private static List<PhysicalCard> cards(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> mock(PhysicalCard.class))
                .toList();
    }

    private static PhysicalCard zoneCard(Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getZone()).thenReturn(zone);
        return card;
    }

    private static PhysicalCard card(String title, Zone zone,
                                     SwccgCardBlueprint blueprint) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        return card;
    }

    private static SwccgCardBlueprint blueprint(String title,
                                                CardCategory category,
                                                String gameText,
                                                boolean senator) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.getGameText()).thenReturn(gameText);
        when(blueprint.hasKeyword(Keyword.SENATOR)).thenReturn(senator);
        return blueprint;
    }
}
