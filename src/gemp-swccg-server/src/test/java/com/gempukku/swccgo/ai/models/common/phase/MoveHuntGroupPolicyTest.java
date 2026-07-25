package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MoveHuntGroupPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void hunterMovesTowardStrongestAlliesAtExactEightBoundary() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard weak = location("Weak Camp");
        PhysicalCard strong = location("Strong Camp");
        PhysicalCard vader = character(
                PLAYER, "Darth Vader", 6.0f, Zone.AT_LOCATION, source);
        PhysicalCard allyOne = character(
                PLAYER, "Ally One", 3.0f, Zone.AT_LOCATION, weak);
        PhysicalCard allyTwo = character(
                PLAYER, "Ally Two", 8.0f, Zone.AT_LOCATION, strong);
        harness.topLocations(source, weak, strong);
        harness.cardsAt(weak, allyOne);
        harness.cardsAt(strong, allyTwo);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, vader, "MOVE TO STRONG CAMP",
                card -> card == vader);

        assertEquals(MoveHuntGroupPolicy.Branch.HUNTER_TOWARD_ALLIES,
                result.branch());
        assertSame(strong, result.anchorLocation());
        assertEquals(2, result.totalAllyCharacters());
        assertFloat(8.0f, result.bestAllyPower());
        assertFloat(250.0f, result.contribution().delta());
        assertEquals(
                "V29.13 HUNT GROUP MOVE: Vader moving TOWARD 2 allies at Strong Camp (power 8) — group up!",
                result.contribution().reason());
    }

    @Test
    public void hunterBonusStaysTwoHundredBelowEightPower() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard camp = location("Camp");
        PhysicalCard dooku = character(
                PLAYER, "Count Dooku", 6.0f, Zone.AT_LOCATION, source);
        PhysicalCard ally = character(
                PLAYER, "Ally", 7.99f, Zone.AT_LOCATION, camp);
        harness.topLocations(source, camp);
        harness.cardsAt(camp, ally);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, dooku, "move to camp",
                card -> card == dooku);

        assertEquals(MoveHuntGroupPolicy.Branch.HUNTER_TOWARD_ALLIES,
                result.branch());
        assertFloat(200.0f, result.contribution().delta());
    }

    @Test
    public void strongestAllyTieKeepsFirstLocation() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard first = location("First Camp");
        PhysicalCard second = location("Second Camp");
        PhysicalCard hunter = character(
                PLAYER, "Lord Tyranus", 6.0f, Zone.AT_LOCATION, source);
        harness.topLocations(source, first, second);
        harness.cardsAt(first, character(
                PLAYER, "First Ally", 8.0f, Zone.AT_LOCATION, first));
        harness.cardsAt(second, character(
                PLAYER, "Second Ally", 8.0f, Zone.AT_LOCATION, second));

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, hunter, "move to second camp",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.HUNTER_AWAY_FROM_ALLIES,
                result.branch());
        assertSame(first, result.anchorLocation());
        assertFloat(-200.0f, result.contribution().delta());
        assertEquals(
                "V29.13 HUNT GROUP: Vader moving AWAY from 2 allies — stay together!",
                result.contribution().reason());
    }

    @Test
    public void zeroPowerAlliesDoNotCreateAnAnchor() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard camp = location("Camp");
        PhysicalCard hunter = character(
                PLAYER, "Darth Vader", 6.0f, Zone.AT_LOCATION, source);
        harness.topLocations(source, camp);
        harness.cardsAt(camp, character(
                PLAYER, "Powerless Ally", null, Zone.AT_LOCATION, camp));

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, hunter, "move to camp",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.NONE, result.branch());
        assertFalse(result.contribution().applies());
        assertNull(result.anchorLocation());
    }

    @Test
    public void hunterAwayPenaltyIsSuppressedWhenDestinationHasOpponents() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard allies = location("Allies");
        PhysicalCard target = location("Target");
        PhysicalCard hunter = character(
                PLAYER, "Darth Vader", 6.0f, Zone.AT_LOCATION, source);
        harness.topLocations(source, allies, target);
        harness.cardsAt(allies, character(
                PLAYER, "Ally", 5.0f, Zone.AT_LOCATION, allies));
        harness.cardsAt(target);
        harness.opponentPower(target, 1.0f);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, hunter, "move to target",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.NONE, result.branch());
        assertTrue(result.huntingOpponents());
        assertFalse(result.contribution().applies());
        assertSame(allies, result.anchorLocation());
    }

    @Test
    public void firstTextualDestinationFailureStillBreaksAndPenalizes() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard allies = location("Allies");
        PhysicalCard first = location("First Target");
        PhysicalCard second = location("Second Target");
        PhysicalCard hunter = character(
                PLAYER, "Darth Vader", 6.0f, Zone.AT_LOCATION, source);
        harness.topLocations(source, allies, first, second);
        harness.cardsAt(allies, character(
                PLAYER, "Ally", 5.0f, Zone.AT_LOCATION, allies));
        harness.cardsAt(first);
        harness.cardsAt(second);
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, first, OPPONENT, false, false))
                .thenThrow(new RuntimeException("injected"));
        harness.opponentPower(second, 10.0f);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, hunter,
                "move through first target to second target",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.HUNTER_AWAY_FROM_ALLIES,
                result.branch());
        assertFloat(-200.0f, result.contribution().delta());
        verify(harness.modifiers, never()).getTotalPowerAtLocation(
                harness.gameState, second, OPPONENT, false, false);
    }

    @Test
    public void predicateCanClassifyAnUnnamedHunter() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard camp = location("Camp");
        PhysicalCard hunter = character(
                PLAYER, "Mysterious Sith", 6.0f,
                Zone.AT_LOCATION, source);
        harness.topLocations(source, camp);
        harness.cardsAt(camp, character(
                PLAYER, "Ally", 4.0f, Zone.AT_LOCATION, camp));

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, hunter, "move to camp", card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.HUNTER_TOWARD_ALLIES,
                result.branch());
        assertFloat(200.0f, result.contribution().delta());
    }

    @Test
    public void predicateFailureFallsThroughToNonHunterPath() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard mover = character(
                PLAYER, "Mysterious Character", 4.0f,
                Zone.AT_LOCATION, source);
        harness.permanents();

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, mover, "move elsewhere", card -> {
                    throw new RuntimeException("injected");
                });

        assertEquals(MoveHuntGroupPolicy.Branch.NONE, result.branch());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void allyLeavingHunterGetsExactPenaltyAndReason() {
        Harness harness = new Harness();
        PhysicalCard hunterLocation = location("Hunter Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f,
                Zone.AT_LOCATION, hunterLocation);
        PhysicalCard hunter = character(
                PLAYER, "Darth Vader", 6.0f,
                Zone.AT_LOCATION, hunterLocation);
        harness.permanents(hunter);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                hunterLocation, mover, "move elsewhere",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.ALLY_AWAY_FROM_HUNTER,
                result.branch());
        assertFloat(-250.0f, result.contribution().delta());
        assertEquals(
                "V29.13 HUNT GROUP: Brother moving AWAY from Vader at Hunter Camp — stay together!",
                result.contribution().reason());
    }

    @Test
    public void allyMovingTowardHunterGetsExactBonus() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard hunterLocation = location("Hunter Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f, Zone.AT_LOCATION, source);
        PhysicalCard hunter = character(
                PLAYER, "Darth Vader", 6.0f,
                Zone.AT_LOCATION, hunterLocation);
        harness.permanents(hunter);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, mover, "move to hunter camp",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.ALLY_TOWARD_HUNTER,
                result.branch());
        assertFloat(250.0f, result.contribution().delta());
        assertSame(hunterLocation, result.anchorLocation());
        assertEquals(
                "V29.13 HUNT GROUP MOVE: Brother moving TOWARD Vader at Hunter Camp — group up!",
                result.contribution().reason());
    }

    @Test
    public void allyMovingElsewhereGetsMildPenalty() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard hunterLocation = location("Hunter Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f, Zone.AT_LOCATION, source);
        PhysicalCard hunter = character(
                PLAYER, "Count Dooku", 6.0f,
                Zone.AT_LOCATION, hunterLocation);
        harness.permanents(hunter);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, mover, "move somewhere else",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.ALLY_ELSEWHERE,
                result.branch());
        assertFloat(-100.0f, result.contribution().delta());
        assertEquals(
                "V29.13 HUNT GROUP: Brother moving but NOT toward Vader at Hunter Camp — group up instead!",
                result.contribution().reason());
    }

    @Test
    public void allyAlreadyWithHunterAndMovingThereHasNoAdjustment() {
        Harness harness = new Harness();
        PhysicalCard hunterLocation = location("Hunter Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f,
                Zone.AT_LOCATION, hunterLocation);
        PhysicalCard hunter = character(
                PLAYER, "Darth Vader", 6.0f,
                Zone.AT_LOCATION, hunterLocation);
        harness.permanents(hunter);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                hunterLocation, mover, "move to hunter camp",
                card -> card == hunter);

        assertEquals(MoveHuntGroupPolicy.Branch.NONE, result.branch());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void firstHunterAnchorStopsEvenWhenItHasNoLocation() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard usefulLocation = location("Useful Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f, Zone.AT_LOCATION, source);
        PhysicalCard firstHunter = character(
                PLAYER, "Darth Vader", 6.0f,
                Zone.AT_LOCATION, null);
        PhysicalCard laterHunter = character(
                PLAYER, "Count Dooku", 6.0f,
                Zone.AT_LOCATION, usefulLocation);
        harness.permanents(firstHunter, laterHunter);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, mover, "move to useful camp",
                card -> card == firstHunter
                        || card == laterHunter);

        assertEquals(MoveHuntGroupPolicy.Branch.NONE, result.branch());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void anchorScanSkipsOpponentOutOfPlayAndNonCharacterCards() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard hunterLocation = location("Hunter Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f, Zone.AT_LOCATION, source);
        PhysicalCard opponentHunter = character(
                OPPONENT, "Darth Vader", 6.0f,
                Zone.AT_LOCATION, hunterLocation);
        PhysicalCard outOfPlayHunter = character(
                PLAYER, "Darth Vader", 6.0f,
                Zone.LOST_PILE, hunterLocation);
        PhysicalCard effectHunter = card(
                PLAYER, "Darth Vader Effect", CardCategory.EFFECT,
                null, Zone.AT_LOCATION, hunterLocation);
        PhysicalCard usableHunter = character(
                PLAYER, "Darth Vader", 6.0f,
                Zone.AT_LOCATION, hunterLocation);
        harness.permanents(
                opponentHunter, outOfPlayHunter,
                effectHunter, usableHunter);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, mover, "move to hunter camp",
                card -> card == opponentHunter
                        || card == outOfPlayHunter
                        || card == effectHunter
                        || card == usableHunter);

        assertEquals(MoveHuntGroupPolicy.Branch.ALLY_TOWARD_HUNTER,
                result.branch());
        assertSame(hunterLocation, result.anchorLocation());
    }

    @Test
    public void vaderTitleImpostorDoesNotBecomeTheGroupAnchor() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard brokerLocation = location("Broker Camp");
        PhysicalCard mover = character(
                PLAYER, "Brother", 4.0f,
                Zone.AT_LOCATION, source);
        PhysicalCard broker = character(
                PLAYER,
                "Lando Calrissian, Vader's Broker",
                3.0f, Zone.AT_LOCATION,
                brokerLocation);
        harness.permanents(broker);

        MoveHuntGroupPolicy.Evaluation result = harness.evaluate(
                source, mover, "move to broker camp",
                card -> false);

        assertEquals(
                MoveHuntGroupPolicy.Branch.NONE,
                result.branch());
        assertFalse(result.contribution().applies());
    }

    private static PhysicalCard location(String title) {
        PhysicalCard location = mock(PhysicalCard.class);
        when(location.getTitle()).thenReturn(title);
        return location;
    }

    private static PhysicalCard character(
            String owner, String title, Float power,
            Zone zone, PhysicalCard atLocation) {
        return card(
                owner, title, CardCategory.CHARACTER,
                power, zone, atLocation);
    }

    private static PhysicalCard card(
            String owner, String title, CardCategory category,
            Float power, Zone zone, PhysicalCard atLocation) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getTitle()).thenReturn(title);
        when(card.getZone()).thenReturn(zone);
        when(card.getAtLocation()).thenReturn(atLocation);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.getPower()).thenReturn(power);
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
            when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        }

        private void topLocations(PhysicalCard... locations) {
            when(gameState.getTopLocations()).thenReturn(List.of(locations));
        }

        private void cardsAt(
                PhysicalCard location, PhysicalCard... cards) {
            when(gameState.getCardsAtLocation(location))
                    .thenReturn(List.of(cards));
        }

        private void permanents(PhysicalCard... cards) {
            when(gameState.getAllPermanentCards())
                    .thenReturn(List.of(cards));
        }

        private void opponentPower(PhysicalCard location, float power) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, OPPONENT,
                    false, false)).thenReturn(power);
        }

        private MoveHuntGroupPolicy.Evaluation evaluate(
                PhysicalCard source, PhysicalCard mover,
                String actionText,
                Predicate<PhysicalCard> darkJediClassifier) {
            return MoveHuntGroupPolicy.evaluate(
                    gameState, game, source, mover, PLAYER,
                    () -> actionText, darkJediClassifier);
        }
    }
}
