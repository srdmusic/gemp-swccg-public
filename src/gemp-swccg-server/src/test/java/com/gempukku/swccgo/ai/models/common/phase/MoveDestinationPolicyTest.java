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

    @Test
    public void sharedResolverKeepsFirstTextualLocationAndSkipsSource() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source", CardSubtype.SITE);
        PhysicalCard first = location("First", CardSubtype.SITE);
        PhysicalCard second = location("Second", CardSubtype.SITE);
        harness.locations(source, first, second);

        PhysicalCard result = MoveDestinationPolicy.resolveDestination(
                harness.gameState,
                source,
                "move through first toward second");

        assertSame(first, result);
    }

    @Test
    public void battlegroundRetreatUsesExactTransitionAndPenalty() {
        assertFalse(MoveDestinationPolicy.battlegroundRetreat(
                "Source", "Destination", false, false).applies());
        assertFalse(MoveDestinationPolicy.battlegroundRetreat(
                "Source", "Destination", false, true).applies());
        assertFalse(MoveDestinationPolicy.battlegroundRetreat(
                "Source", "Destination", true, true).applies());

        MoveDestinationPolicy.Contribution result =
                MoveDestinationPolicy.battlegroundRetreat(
                        "Source", "Destination", true, false);
        assertTrue(result.applies());
        assertFloat(-800.0f, result.delta());
        assertEquals(
                "V37 NO RETREAT: Moving from battleground Source"
                        + " to non-battleground Destination"
                        + " — lose drain and battle ability!",
                result.reason());
    }

    @Test
    public void retreatModeRequiresPositiveOpponentPowerExcess() {
        assertFalse(MoveDestinationPolicy.retreatMode(
                "Source", 0.0f).active());
        assertFalse(MoveDestinationPolicy.retreatMode(
                "Source", -1.0f).active());

        MoveDestinationPolicy.RetreatMode retreat =
                MoveDestinationPolicy.retreatMode("Source", 0.0001f);
        assertTrue(retreat.active());
        assertEquals("Source", retreat.originTitle());
        assertTrue(MoveDestinationPolicy.retreatExemptsWrongDirection(
                retreat));
    }

    @Test
    public void safeRetreatDestinationPreservesExactBonusAndReason() {
        MoveDestinationPolicy.RetreatMode retreat =
                MoveDestinationPolicy.retreatMode("Guest Quarters", 20.0f);

        MoveDestinationPolicy.Contribution safe =
                MoveDestinationPolicy.safeRetreatDestination(
                        retreat, "East Platform", 0.0f);
        assertTrue(safe.applies());
        assertFloat(600.0f, safe.delta());
        assertEquals(
                "V169 RETREAT: East Platform is safe (no opponent power) — get the endangered character out of Guest Quarters!",
                safe.reason());

        assertFalse(MoveDestinationPolicy.safeRetreatDestination(
                retreat, "Occupied", Math.nextUp(0.0f)).applies());
        assertFalse(MoveDestinationPolicy.safeRetreatDestination(
                MoveDestinationPolicy.retreatMode("Source", 0.0f),
                "Empty", 0.0f).applies());
    }

    @Test
    public void retreatToDrainPreservesStrictRouteAndExactBonus() {
        MoveDestinationPolicy.Contribution result =
                MoveDestinationPolicy.retreatToDrain(
                        "Contested Site", 8.0f, 5.0f, true,
                        "Drain Site", 0.0f, false, 2);

        assertTrue(result.applies());
        assertFloat(400.0f, result.delta());
        assertEquals(
                "V67au RETREAT-TO-DRAIN: Contested Site is over-contested (their 8 vs our 5) — move to safe adjacent Drain Site (no opp, 2 friendly icons) and drain there!",
                result.reason());

        assertFalse(MoveDestinationPolicy.retreatToDrain(
                "Source", 8.0f, 5.0f, false,
                "Destination", 0.0f, false, 1).applies());
        assertFalse(MoveDestinationPolicy.retreatToDrain(
                "Source", 5.0f, 5.0f, true,
                "Destination", 0.0f, false, 1).applies());
        assertFalse(MoveDestinationPolicy.retreatToDrain(
                "Source", 8.0f, 5.0f, true,
                "Destination", Math.nextUp(0.0f), false, 1).applies());
        assertFalse(MoveDestinationPolicy.retreatToDrain(
                "Source", 8.0f, 5.0f, true,
                "Destination", 0.0f, true, 1).applies());
        assertFalse(MoveDestinationPolicy.retreatToDrain(
                "Source", 8.0f, 5.0f, true,
                "Destination", 0.0f, false, 0).applies());
    }

    @Test
    public void hiddenPathPowerSafetyPreservesThresholdsAndScores() {
        assertFalse(MoveDestinationPolicy.powerAwareHiddenPathDestination(
                false, "Jabiim", 12.0f, 0.0f)
                .contribution().applies());
        assertFalse(MoveDestinationPolicy.powerAwareHiddenPathDestination(
                true, "Mapuzo: Safehouse", 12.0f, 0.0f)
                .contribution().applies());

        MoveDestinationPolicy.PowerAwareDestination seven =
                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                        true, "Jabiim", 7.0f, 0.0f);
        assertEquals(MoveDestinationPolicy.PowerAwareDisposition.SUICIDE,
                seven.disposition());
        assertFloat(6.0f, seven.projectedOurPower());
        assertFloat(-1500.0f, seven.contribution().delta());
        assertEquals(
                "V64 SUICIDE MOVE: Jabiim has enemy power 7 — solo Jedi will DIE on their next turn!",
                seven.contribution().reason());

        assertFloat(-1800.0f,
                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                        true, "Jabiim", 9.0f, 0.0f)
                        .contribution().delta());
        assertFloat(-2500.0f,
                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                        true, "Jabiim", 12.0f, 0.0f)
                        .contribution().delta());

        MoveDestinationPolicy.PowerAwareDestination safe =
                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                        true, "Jabiim", 0.0f, 0.0f);
        assertEquals(MoveDestinationPolicy.PowerAwareDisposition.SAFE_DRAIN,
                safe.disposition());
        assertFloat(150.0f, safe.contribution().delta());

        MoveDestinationPolicy.PowerAwareDestination favorable =
                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                        true, "Jabiim", 3.0f, 0.0f);
        assertEquals(MoveDestinationPolicy.PowerAwareDisposition.FAVORABLE,
                favorable.disposition());
        assertFloat(80.0f, favorable.contribution().delta());

        assertFalse(MoveDestinationPolicy.powerAwareHiddenPathDestination(
                true, "Jabiim", 7.0f, 3.0f)
                .contribution().applies());
    }

    @Test
    public void hiddenPathPreFlipSuicidePreservesHardBoundary() {
        assertFalse(MoveDestinationPolicy.hiddenPathPreFlipSuicide(
                false, "Hoth", 8.0f, 0.0f).applies());
        assertFalse(MoveDestinationPolicy.hiddenPathPreFlipSuicide(
                true, "Hoth", Math.nextDown(5.0f), 0.0f).applies());
        assertFalse(MoveDestinationPolicy.hiddenPathPreFlipSuicide(
                true, "Hoth", 5.0f, Math.nextUp(0.0f)).applies());

        MoveDestinationPolicy.Contribution result =
                MoveDestinationPolicy.hiddenPathPreFlipSuicide(
                        true, "Hoth", 5.0f, 0.0f);
        assertTrue(result.applies());
        assertFloat(-9999.0f, result.delta());
        assertEquals(
                "V67aa HIDDEN PATH SUICIDE BLOCK: Hoth has opp power 5 — pre-flip Jedi survivors are power 3, this is SUICIDE!",
                result.reason());
    }

    @Test
    public void spyAwareContestPreservesContestAndSpyOnlyScores() {
        assertFloat(300.0f, MoveDestinationPolicy.spyAwareContest(
                "Site", 4.0f, 0, 2.0f, false)
                .contribution().delta());
        assertFloat(500.0f, MoveDestinationPolicy.spyAwareContest(
                "Site", 4.0f, 0, 0.0f, false)
                .contribution().delta());

        MoveDestinationPolicy.SpyAwareContest jedi =
                MoveDestinationPolicy.spyAwareContest(
                        "Jedi Site", 4.0f, 1, 0.0f, true);
        assertEquals(MoveDestinationPolicy.ContestDisposition.CONTEST,
                jedi.disposition());
        assertFloat(700.0f, jedi.contribution().delta());
        assertEquals(
                "V41 CONTEST DEST: Opponents (power 4) at Jedi Site [JEDI!] — go fight!",
                jedi.contribution().reason());

        MoveDestinationPolicy.SpyAwareContest spyOnly =
                MoveDestinationPolicy.spyAwareContest(
                        "Mos Eisley", 0.0f, 2, 0.0f, false);
        assertEquals(MoveDestinationPolicy.ContestDisposition.SPY_ONLY,
                spyOnly.disposition());
        assertFloat(-1500.0f, spyOnly.contribution().delta());
        assertEquals(
                "V67f SPY-ONLY: Mos Eisley has only opponent spy (2) — drain blocked, prefer draining elsewhere",
                spyOnly.contribution().reason());

        assertEquals(MoveDestinationPolicy.ContestDisposition.NONE,
                MoveDestinationPolicy.spyAwareContest(
                        "Empty", 0.0f, 0, 0.0f, false)
                        .disposition());
    }

    @Test
    public void replaySpyOnlyPenaltyDominatesFalseContestAndDrainBonuses() {
        float replayScore = 15.0f + 15.0f + 350.0f + 15.0f
                + 10.0f + 40.0f
                + MoveDestinationPolicy.spyAwareContest(
                        "Malachor: Sith Temple Upper Chamber",
                        0.0f, 1, 18.0f, false)
                        .contribution().delta();

        assertFloat(-1055.0f, replayScore);
    }

    @Test
    public void drainThreatPreservesSpyAndSuicideExemptionOrder() {
        assertEquals(MoveDestinationPolicy.DrainThreatDisposition.NONE,
                MoveDestinationPolicy.drainThreat(0.0f, 0.0f, false));
        assertEquals(MoveDestinationPolicy.DrainThreatDisposition.NONE,
                MoveDestinationPolicy.drainThreat(4.0f, 1.0f, false));
        assertEquals(
                MoveDestinationPolicy.DrainThreatDisposition.SPY_NEUTRALIZED,
                MoveDestinationPolicy.drainThreat(9.0f, 0.0f, true));
        assertEquals(
                MoveDestinationPolicy.DrainThreatDisposition.TOO_DANGEROUS,
                MoveDestinationPolicy.drainThreat(7.0f, 0.0f, false));
        assertEquals(MoveDestinationPolicy.DrainThreatDisposition.ACTIVE,
                MoveDestinationPolicy.drainThreat(
                        Math.nextDown(7.0f), 0.0f, false));
    }

    @Test
    public void wrongDirectionPreservesExemptionPriorityAndVeto() {
        assertEquals(MoveDestinationPolicy.WrongDirectionDisposition.NONE,
                MoveDestinationPolicy.wrongDirection(
                        false, "Empty", "Opponent", false, false, false)
                        .disposition());
        assertEquals(
                MoveDestinationPolicy.WrongDirectionDisposition.HIDDEN_PATH_EXEMPT,
                MoveDestinationPolicy.wrongDirection(
                        true, "Empty", "Opponent", true, true, true)
                        .disposition());
        assertEquals(
                MoveDestinationPolicy.WrongDirectionDisposition.RETREAT_EXEMPT,
                MoveDestinationPolicy.wrongDirection(
                        true, "Empty", "Opponent", false, true, true)
                        .disposition());
        assertEquals(
                MoveDestinationPolicy.WrongDirectionDisposition.JOIN_GROUP_EXEMPT,
                MoveDestinationPolicy.wrongDirection(
                        true, "Empty", "Opponent", false, false, true)
                        .disposition());

        MoveDestinationPolicy.WrongDirectionEvaluation veto =
                MoveDestinationPolicy.wrongDirection(
                        true, "Empty", "Opponent", false, false, false);
        assertEquals(MoveDestinationPolicy.WrongDirectionDisposition.VETO,
                veto.disposition());
        assertFloat(-9999.0f, veto.contribution().delta());
        assertEquals(
                "V41 WRONG DIRECTION: Empty is empty — opponents draining at Opponent! Go there instead!",
                veto.contribution().reason());
    }

    @Test
    public void objectiveActorRouteHasBoundedParentAndDestinationScores() {
        MoveDestinationPolicy.Contribution parent =
                MoveDestinationPolicy.objectiveActorRouteStart(
                        true, "Padme Naberrie");
        MoveDestinationPolicy.Contribution destination =
                MoveDestinationPolicy.objectiveActorRouteDestination(
                        true, "Padme Naberrie",
                        "Naboo: Theed Palace Hallway");
        MoveDestinationPolicy.Contribution none =
                MoveDestinationPolicy.objectiveActorRouteDestination(
                        false, "Padme Naberrie",
                        "Naboo: Theed Palace Courtyard");

        assertTrue(parent.applies());
        assertFloat(600.0f, parent.delta());
        assertTrue(parent.reason().startsWith(
                "MOVE.OBJECTIVE.ACTOR_ROUTE_START:"));
        assertTrue(destination.applies());
        assertFloat(1000.0f, destination.delta());
        assertTrue(destination.reason().startsWith(
                "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION:"));
        assertFalse(none.applies());
    }

    @Test
    public void objectiveActorRouteExemptsOnlyItsCloserHopFromWrongDirection() {
        MoveDestinationPolicy.WrongDirectionEvaluation objectiveRoute =
                MoveDestinationPolicy.wrongDirection(
                        true, "Hallway", "Swamp",
                        false, false, false, true);
        MoveDestinationPolicy.WrongDirectionEvaluation ordinary =
                MoveDestinationPolicy.wrongDirection(
                        true, "Courtyard", "Swamp",
                        false, false, false);

        assertEquals(
                MoveDestinationPolicy.WrongDirectionDisposition
                        .OBJECTIVE_ROUTE_EXEMPT,
                objectiveRoute.disposition());
        assertFalse(objectiveRoute.contribution().applies());
        assertEquals(
                MoveDestinationPolicy.WrongDirectionDisposition.VETO,
                ordinary.disposition());
        assertFloat(-9999.0f, ordinary.contribution().delta());
    }

    @Test
    public void castleRetreatPreservesExactTitleGateAndVeto() {
        assertTrue(MoveDestinationPolicy.isCastleDestination(
                "Mustafar: Vader's Castle"));
        assertFalse(MoveDestinationPolicy.isCastleDestination(
                "Vader's Castle"));
        assertFalse(MoveDestinationPolicy.castleRetreat(
                "Mustafar: Vader's Castle", false).applies());
        assertFalse(MoveDestinationPolicy.castleRetreat(
                "Vader's Castle", true).applies());

        MoveDestinationPolicy.Contribution result =
                MoveDestinationPolicy.castleRetreat(
                        "Mustafar: Vader's Castle", true);
        assertTrue(result.applies());
        assertFloat(-9999.0f, result.delta());
        assertEquals(
                "V41 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!",
                result.reason());
    }

    @Test
    public void destinationSafetyContributionsRemainIndependentlyAdditive() {
        MoveDestinationPolicy.RetreatMode retreat =
                MoveDestinationPolicy.retreatMode("Contested", 3.0f);
        float retreatStack = MoveDestinationPolicy.safeRetreatDestination(
                retreat, "Drain Site", 0.0f).delta()
                + MoveDestinationPolicy.retreatToDrain(
                        "Contested", 8.0f, 5.0f, true,
                        "Drain Site", 0.0f, false, 1).delta();
        assertFloat(1000.0f, retreatStack);

        float hiddenPathStack =
                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                        true, "Hoth", 7.0f, 0.0f)
                        .contribution().delta()
                + MoveDestinationPolicy.hiddenPathPreFlipSuicide(
                        true, "Hoth", 7.0f, 0.0f).delta();
        assertFloat(-11499.0f, hiddenPathStack);

        float vetoStack = MoveDestinationPolicy.wrongDirection(
                true, "Mustafar: Vader's Castle", "Opponent Site",
                false, false, false).contribution().delta()
                + MoveDestinationPolicy.castleRetreat(
                        "Mustafar: Vader's Castle", true).delta();
        assertFloat(-19998.0f, vetoStack);

        MoveDestinationPolicy.SpyAwareContest mixed =
                MoveDestinationPolicy.spyAwareContest(
                        "Mixed Site", 3.0f, 1, 0.0f, false);
        assertEquals(MoveDestinationPolicy.ContestDisposition.CONTEST,
                mixed.disposition());
        assertFloat(500.0f, mixed.contribution().delta());
    }

    @Test
    public void selfMoveToFriendClassificationPreservesBothPhrases() {
        assertFalse(MoveDestinationPolicy.isSelfMoveToFriend(null));
        assertFalse(MoveDestinationPolicy.isSelfMoveToFriend(
                "May move as a regular move."));
        assertTrue(MoveDestinationPolicy.isSelfMoveToFriend(
                "May move to same site as a Jedi."));
        assertTrue(MoveDestinationPolicy.isSelfMoveToFriend(
                "MOVES TO SAME SITE AS LUKE."));
    }

    @Test
    public void selfMoveCompanionVetoRequiresZeroFriendlies() {
        assertFalse(MoveDestinationPolicy.companionVeto(
                "Yoda", "Dagobah: Bog Clearing", false, 0).hardVeto());
        assertFalse(MoveDestinationPolicy.companionVeto(
                "Yoda", "Dagobah: Bog Clearing", true, 1).hardVeto());

        MoveDestinationPolicy.CompanionVeto result =
                MoveDestinationPolicy.companionVeto(
                        "Yoda", "Dagobah: Bog Clearing", true, 0);
        assertTrue(result.hardVeto());
        assertEquals(
                "V135 SELF-MOVE-TO-FRIEND ALONE: 'Yoda' would land alone at"
                        + " Dagobah: Bog Clearing — no friendly characters there",
                result.reason());
    }

    @Test
    public void residualIconScoringPreservesOpponentThenOwnStack() {
        MoveDestinationPolicy.IconScoring both =
                MoveDestinationPolicy.icons(2, 3);
        MoveDestinationPolicy.IconScoring none =
                MoveDestinationPolicy.icons(0, 0);

        assertFloat(45.0f, both.opponentIcons().delta());
        assertFloat(15.0f, both.ownIcons().delta());
        assertFloat(60.0f,
                both.opponentIcons().delta() + both.ownIcons().delta());
        assertFalse(both.noIcons().applies());
        assertFloat(-10.0f, none.noIcons().delta());
    }

    @Test
    public void missingSourceLocationPreservesLegacyPenaltyAndReason() {
        MoveDestinationPolicy.Contribution result =
                MoveDestinationPolicy.missingSourceLocation();

        assertTrue(result.applies());
        assertEquals("Card not at a location", result.reason());
        assertFloat(-10.0f, result.delta());
    }

    @Test
    public void residualPowerScoringPreservesBoundaries() {
        assertFloat(10.0f,
                MoveDestinationPolicy.power(4.0f, 4.0f, 0, 0).delta());
        assertFloat(10.0f,
                MoveDestinationPolicy.power(3.0f, 5.0f, 0, 0).delta());
        assertFloat(-25.0f,
                MoveDestinationPolicy.power(2.99f, 5.0f, 0, 0).delta());
        assertFloat(20.0f,
                MoveDestinationPolicy.power(0.0f, 0.0f, 0, 1).delta());
        assertFloat(10.0f,
                MoveDestinationPolicy.power(0.0f, 0.0f, 1, 0).delta());
        assertFloat(0.0f,
                MoveDestinationPolicy.power(0.0f, 0.0f, 0, 0).delta());
    }

    @Test
    public void battlegroundTriStatePreservesEngineAndFallbackScores() {
        assertFloat(40.0f,
                MoveDestinationPolicy.battleground(true, false).delta());
        MoveDestinationPolicy.Contribution engineFalse =
                MoveDestinationPolicy.battleground(false, true);
        assertTrue(engineFalse.applies());
        assertFloat(0.0f, engineFalse.delta());
        assertFloat(15.0f,
                MoveDestinationPolicy.battleground(null, true).delta());
        assertFalse(MoveDestinationPolicy.battleground(
                null, false).applies());
    }

    @Test
    public void cloudCityResidualPreservesEmptyAndFriendlyTiers() {
        MoveObjectiveConsolidationPolicy.Contribution empty =
                MoveObjectiveConsolidationPolicy.cloudCityDestination(
                        true, true, 0.0f, 0.0f);
        MoveObjectiveConsolidationPolicy.Contribution friendly =
                MoveObjectiveConsolidationPolicy.cloudCityDestination(
                        true, true, 0.0f, 3.0f);

        assertFloat(200.0f, empty.delta());
        assertFloat(20.0f, friendly.delta());
        assertFalse(MoveObjectiveConsolidationPolicy.cloudCityDestination(
                true, true, 1.0f, 0.0f).applies());
    }

    @Test
    public void evazanResidualRequiresMoverAndPartner() {
        assertFloat(200.0f, MoveDestinationPolicy.evazanCombo(
                true, false, true).delta());
        assertFloat(200.0f, MoveDestinationPolicy.evazanCombo(
                false, true, true).delta());
        assertFalse(MoveDestinationPolicy.evazanCombo(
                true, false, false).applies());
        assertFalse(MoveDestinationPolicy.evazanCombo(
                false, false, true).applies());
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
