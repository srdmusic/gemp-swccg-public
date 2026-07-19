package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoveDestinationPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final Predicate<String> JEDI =
            title -> title.contains("jedi") || title.contains("padawan");

    @Test
    public void landedShipEscapePreservesTakeoffAndDisembarkScores() {
        Harness harness = new Harness();
        PhysicalCard site = location("Jakku: Niima Marketplace", CardSubtype.SITE);
        PhysicalCard ship = card(PLAYER, CardCategory.STARSHIP, "Command Shuttle");
        when(harness.gameState.getAllPermanentCards())
                .thenReturn(List.of(ship));
        when(harness.modifiers.getLocationThatCardIsAt(
                harness.gameState, ship)).thenReturn(site);

        MoveDestinationPolicy.LandedShipEscape takeOff =
                MoveDestinationPolicy.landedShipEscape(
                        harness.gameState, harness.game, site, PLAYER,
                        () -> "TAKE OFF");
        assertTrue(takeOff.contribution().applies());
        assertTrue(takeOff.takeOff());
        assertFloat(800.0f, takeOff.contribution().delta());
        assertEquals(
                "V91 ESCAPE LANDED SHIP: Take off at site Jakku: Niima Marketplace — lift to system to restore ship power / use character on ground",
                takeOff.contribution().reason());

        MoveDestinationPolicy.LandedShipEscape disembark =
                MoveDestinationPolicy.landedShipEscape(
                        harness.gameState, harness.game, site, PLAYER,
                        () -> "disembark");
        assertTrue(disembark.disembark());
        assertFalse(disembark.moveAboard());
        assertFloat(600.0f, disembark.contribution().delta());
        assertEquals(
                "V91 ESCAPE LANDED SHIP: Disembark at site Jakku: Niima Marketplace — drop pilot to ground to restore ship power / use character on ground",
                disembark.contribution().reason());
    }

    @Test
    public void landedShipEscapeRejectsSystemsAndEmbarkOnly() {
        Harness harness = new Harness();
        PhysicalCard system = location("Jakku", CardSubtype.SYSTEM);
        MoveDestinationPolicy.LandedShipEscape atSystem =
                MoveDestinationPolicy.landedShipEscape(
                        harness.gameState, harness.game, system, PLAYER,
                        () -> {
                            throw new AssertionError(
                                    "system must be classified before action text");
                        });
        assertFalse(atSystem.contribution().applies());

        PhysicalCard site = location("Jakku: Site", CardSubtype.SITE);
        MoveDestinationPolicy.LandedShipEscape embark =
                MoveDestinationPolicy.landedShipEscape(
                        harness.gameState, harness.game, site, PLAYER,
                        () -> "embark");
        assertFalse(embark.contribution().applies());
        assertTrue(embark.moveAboard());
    }

    @Test
    public void landedShipEscapeUsesLocationIdentityAndSkipsOneReadFailure() {
        Harness harness = new Harness();
        PhysicalCard site = location("Same Title", CardSubtype.SITE);
        PhysicalCard equalTitle = location("Same Title", CardSubtype.SITE);
        PhysicalCard brokenShip = card(
                PLAYER, CardCategory.STARSHIP, "Broken Ship");
        PhysicalCard wrongIdentityShip = card(
                PLAYER, CardCategory.STARSHIP, "Wrong Identity");
        PhysicalCard usableShip = card(
                PLAYER, CardCategory.STARSHIP, "Usable Ship");
        when(harness.gameState.getAllPermanentCards())
                .thenReturn(List.of(brokenShip, wrongIdentityShip, usableShip));
        when(harness.modifiers.getLocationThatCardIsAt(
                harness.gameState, brokenShip))
                .thenThrow(new RuntimeException("injected"));
        when(harness.modifiers.getLocationThatCardIsAt(
                harness.gameState, wrongIdentityShip)).thenReturn(equalTitle);
        when(harness.modifiers.getLocationThatCardIsAt(
                harness.gameState, usableShip)).thenReturn(site);

        MoveDestinationPolicy.LandedShipEscape result =
                MoveDestinationPolicy.landedShipEscape(
                        harness.gameState, harness.game, site, PLAYER,
                        () -> "take off");

        assertTrue(result.landedShipFound());
        assertTrue(result.contribution().applies());
    }

    @Test
    public void destinationContestPreservesBaseScoreAndReason() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard destination = location("Destination", CardSubtype.SITE);
        harness.locations(source, destination);
        harness.power(destination, OPPONENT, 5.0f);
        harness.power(destination, PLAYER, 2.0f);
        when(harness.gameState.getCardsAtLocation(destination))
                .thenReturn(List.of());

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null,
                        "move to destination");

        assertSame(destination, result.destination());
        assertFloat(250.0f, result.contestContribution().delta());
        assertFalse(result.destinationWasUncontested());
        assertEquals(
                "V34 CONTEST: Moving to Destination where opponents have power 5 — block their drain and fight!",
                result.contestContribution().reason());
    }

    @Test
    public void uncontestedObservationPrecedesWeaponAndJediScans() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard destination = location("Destination", CardSubtype.SITE);
        PhysicalCard mover = card(PLAYER, CardCategory.CHARACTER, "Mover");
        harness.locations(source, destination);
        List<String> events = new ArrayList<>();
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, destination, OPPONENT,
                false, false)).thenAnswer(invocation -> {
                    events.add("opponent-power");
                    return 4.0f;
                });
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, destination, PLAYER,
                false, false)).thenAnswer(invocation -> {
                    events.add("our-power");
                    return 0.0f;
                });
        when(harness.gameState.getAttachedCards(mover))
                .thenAnswer(invocation -> {
                    events.add("weapon-scan");
                    return List.of();
                });
        when(harness.gameState.getCardsAtLocation(destination))
                .thenAnswer(invocation -> {
                    events.add("jedi-scan");
                    return List.of();
                });

        MoveDestinationPolicy.destinationContest(
                harness.gameState, harness.game, source, mover,
                PLAYER, OPPONENT, "move to destination", JEDI,
                ignored -> events.add("v36-observation"));

        assertEquals(List.of(
                "opponent-power", "our-power", "v36-observation",
                "weapon-scan", "jedi-scan"), events);
    }

    @Test
    public void destinationContestPreservesUrgencyWeaponAndVaderJediStack() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard destination = location("Jedi Site", CardSubtype.SITE);
        PhysicalCard vader = card(PLAYER, CardCategory.CHARACTER, "Darth Vader");
        PhysicalCard weapon = card(PLAYER, CardCategory.WEAPON, "Lightsaber");
        PhysicalCard jedi = card(OPPONENT, CardCategory.CHARACTER, "Jedi Knight");
        harness.locations(source, destination);
        harness.power(destination, OPPONENT, 5.0f);
        harness.power(destination, PLAYER, 0.0f);
        when(harness.gameState.getAttachedCards(vader))
                .thenReturn(List.of(weapon));
        when(harness.gameState.getCardsAtLocation(destination))
                .thenReturn(List.of(jedi));

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, vader,
                        "move to jedi site");

        assertTrue(result.destinationWasUncontested());
        assertTrue(result.moverArmed());
        assertTrue(result.jediAtDestination());
        assertFloat(650.0f, result.contestContribution().delta());
        assertEquals(
                "V34 CONTEST: Moving to Jedi Site where opponents have power 5 [JEDI!] — block their drain and fight!",
                result.contestContribution().reason());
    }

    @Test
    public void destinationContestKeepsFirstTextualLocation() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard first = location("First", CardSubtype.SITE);
        PhysicalCard second = location("Second", CardSubtype.SITE);
        harness.locations(source, first, second);
        harness.power(first, OPPONENT, 2.0f);
        harness.power(first, PLAYER, 1.0f);
        when(harness.gameState.getCardsAtLocation(first))
                .thenReturn(List.of());

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null,
                        "move through first toward second");

        assertSame(first, result.destination());
    }

    @Test
    public void destinationContestPreservesAttachmentAndJediFailOpenReads() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard destination = location("Destination", CardSubtype.SITE);
        PhysicalCard mover = card(PLAYER, CardCategory.CHARACTER, "Mover");
        harness.locations(source, destination);
        harness.power(destination, OPPONENT, 3.0f);
        harness.power(destination, PLAYER, 1.0f);
        when(harness.gameState.getAttachedCards(mover))
                .thenThrow(new RuntimeException("attachment"));
        when(harness.gameState.getCardsAtLocation(destination))
                .thenThrow(new RuntimeException("cards"));

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, mover,
                        "move to destination");

        assertTrue(result.contestContribution().applies());
        assertFalse(result.moverArmed());
        assertFalse(result.jediAtDestination());
        assertFloat(250.0f, result.contestContribution().delta());
    }

    @Test
    public void emptyDestinationPreservesBattlegroundAdvance() {
        Harness harness = new Harness();
        PhysicalCard source = location("Imperial City", CardSubtype.SITE);
        PhysicalCard destination = location("Xizor's Palace", CardSubtype.SITE);
        PhysicalCard opponentSite = location("Opponent Site", CardSubtype.SITE);
        harness.locations(source, destination, opponentSite);
        harness.power(destination, OPPONENT, 0.0f);
        harness.power(opponentSite, OPPONENT, 6.0f);
        harness.power(opponentSite, PLAYER, 0.0f);
        harness.battleground(source, false);
        harness.battleground(destination, true);

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null,
                        "move to xizor's palace");

        assertTrue(result.battlegroundAdvanceContribution().applies());
        assertFloat(400.0f,
                result.battlegroundAdvanceContribution().delta());
        assertEquals(
                "V111 BG ADVANCE: Moving from non-battleground Imperial City to battleground Xizor's Palace — establish drain position!",
                result.battlegroundAdvanceContribution().reason());
        assertFalse(result.wrongDirectionVeto());
    }

    @Test
    public void emptyDestinationPreservesWrongDirectionReasonAndHighestTarget() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard destination = location("Empty", CardSubtype.SITE);
        PhysicalCard first = location("First Opponent", CardSubtype.SITE);
        PhysicalCard highest = location("Highest Opponent", CardSubtype.SITE);
        harness.locations(source, destination, first, highest);
        harness.power(destination, OPPONENT, 0.0f);
        harness.power(first, OPPONENT, 4.0f);
        harness.power(first, PLAYER, 0.0f);
        harness.power(highest, OPPONENT, 8.0f);
        harness.power(highest, PLAYER, 0.0f);
        harness.battleground(source, true);
        harness.battleground(destination, false);

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null, "move to empty");

        assertTrue(result.wrongDirectionVeto());
        assertEquals("Highest Opponent",
                result.opponentUncontestedLocation());
        assertEquals(
                "V38.3 WRONG DIRECTION: Moving to empty Empty while opponents at Highest Opponent",
                result.wrongDirectionReason());
    }

    @Test
    public void emptyDestinationPreservesPartialOpponentScan() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard destination = location("Empty", CardSubtype.SITE);
        PhysicalCard first = location("First Opponent", CardSubtype.SITE);
        PhysicalCard broken = location("Broken", CardSubtype.SITE);
        harness.locations(source, destination, first, broken);
        harness.power(destination, OPPONENT, 0.0f);
        harness.power(first, OPPONENT, 4.0f);
        harness.power(first, PLAYER, 0.0f);
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, broken, OPPONENT,
                false, false)).thenThrow(new RuntimeException("injected"));
        harness.battleground(source, true);

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null, "move to empty");

        assertTrue(result.wrongDirectionVeto());
        assertEquals("First Opponent",
                result.opponentUncontestedLocation());
    }

    @Test
    public void castleVetoPreservesIndependentBoardScan() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard castle = location(
                "Mustafar: Vader's Castle", CardSubtype.SITE);
        PhysicalCard opponentSite = location("Opponent Site", CardSubtype.SITE);
        harness.locations(source, castle, opponentSite);
        harness.power(castle, OPPONENT, 0.0f);
        harness.power(opponentSite, OPPONENT, 5.0f);
        harness.power(opponentSite, PLAYER, 1.0f);

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null,
                        "move to mustafar: vader's castle");

        assertTrue(result.castleVeto());
        assertFalse(result.wrongDirectionVeto());
        assertEquals(
                "V38.3 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!",
                result.castleVetoReason());
    }

    @Test
    public void castleAndWrongDirectionVetoCanCoexist() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard castle = location(
                "Mustafar: Vader's Castle", CardSubtype.SITE);
        PhysicalCard opponentSite = location("Opponent Site", CardSubtype.SITE);
        harness.locations(source, castle, opponentSite);
        harness.power(castle, OPPONENT, 0.0f);
        harness.power(opponentSite, OPPONENT, 5.0f);
        harness.power(opponentSite, PLAYER, 0.0f);
        harness.battleground(source, true);

        MoveDestinationPolicy.DestinationContest result =
                harness.destination(source, null,
                        "move to mustafar: vader's castle");

        assertTrue(result.castleVeto());
        assertTrue(result.wrongDirectionVeto());
    }

    private static PhysicalCard location(
            String title, CardSubtype subtype) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return card;
    }

    private static PhysicalCard card(
            String owner, CardCategory category, String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(expected, actual, 0.0001f);
    }

    private static final class Harness {
        private final GameState gameState = mock(GameState.class);
        private final SwccgGame game = mock(SwccgGame.class);
        private final ModifiersQuerying modifiers = mock(ModifiersQuerying.class);

        private Harness() {
            when(game.getModifiersQuerying()).thenReturn(modifiers);
        }

        private void locations(PhysicalCard... locations) {
            when(gameState.getLocationsInOrder())
                    .thenReturn(List.of(locations));
        }

        private void power(
                PhysicalCard location, String playerId, float power) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, playerId,
                    false, false)).thenReturn(power);
        }

        private void battleground(
                PhysicalCard location, boolean battleground) {
            when(modifiers.isBattleground(
                    gameState, location, null)).thenReturn(battleground);
        }

        private MoveDestinationPolicy.DestinationContest destination(
                PhysicalCard source, PhysicalCard mover,
                String actionLower) {
            return MoveDestinationPolicy.destinationContest(
                    gameState, game, source, mover,
                    PLAYER, OPPONENT, actionLower, JEDI,
                    ignored -> { });
        }
    }
}
