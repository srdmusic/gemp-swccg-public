package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Source-law behavior proof for 208_57 / 208_57_BACK. Card Java is unchanged.
 */
public class IWantThatMapObjectiveBehaviorTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void frontRequiresTwoControlledBattlegroundsWithPhysicalFirstOrderCharacters() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false);
            PhysicalCard groundActor = character(
                    fixture, "First Order Stormtrooper", PLAYER, true);
            PhysicalCard spaceActor = character(
                    fixture, "General Hux", PLAYER, true);
            PhysicalCard carrier = carrier(fixture, "Finalizer", PLAYER);

            place(fixture, groundActor, fixture.firstSite);
            aboard(fixture, spaceActor, carrier, fixture.system);
            place(fixture, carrier, fixture.system);
            pilot(fixture, carrier, spaceActor);
            control(fixture, fixture.firstSite);
            control(fixture, fixture.system);

            assertTrue(label(bot), frontState(fixture).conditionSatisfied());

            PhysicalCard agentAtSystem = character(
                    fixture, "Resistance Liaison", OPPONENT, false);
            resistanceAgent(fixture, agentAtSystem);
            place(fixture, agentAtSystem, fixture.system);
            assertTrue("A Resistance Agent at a system does not block: "
                            + label(bot),
                    frontState(fixture).conditionSatisfied());

            PhysicalCard agentAtSite = character(
                    fixture, "Resistance Spy", OPPONENT, false);
            resistanceAgent(fixture, agentAtSite);
            place(fixture, agentAtSite, fixture.secondSite);
            assertFalse("Either player's Resistance Agent at a battleground"
                            + " site blocks: " + label(bot),
                    frontState(fixture).conditionSatisfied());
        }
    }

    @Test
    public void permanentPilotOrOrdinaryCharacterDoesNotReplacePhysicalFirstOrderActor() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false);
            PhysicalCard firstOrder = character(
                    fixture, "First Order Stormtrooper", PLAYER, true);
            PhysicalCard ordinary = character(
                    fixture, "Darth Vader", PLAYER, false);
            PhysicalCard permanentPilotShip = carrier(
                    fixture, "Command Shuttle", PLAYER);

            place(fixture, firstOrder, fixture.firstSite);
            place(fixture, ordinary, fixture.secondSite);
            place(fixture, permanentPilotShip, fixture.system);
            when(fixture.modifiers.hasAbility(
                    fixture.gameState, permanentPilotShip, true))
                    .thenReturn(true);
            control(fixture, fixture.firstSite);
            control(fixture, fixture.secondSite);
            control(fixture, fixture.system);

            assertFalse("A permanent pilot is not a physical First Order"
                            + " character: " + label(bot),
                    frontState(fixture).conditionSatisfied());
        }
    }

    @Test
    public void carrierProjectionAddsOnlyANetNewQualifiedBattleground() {
        for (Bot bot : Bot.values()) {
            Fixture advance = fixture(bot, false);
            PhysicalCard anchor = character(
                    advance, "First Order Stormtrooper", PLAYER, true);
            PhysicalCard aboardActor = character(
                    advance, "General Hux", PLAYER, true);
            PhysicalCard carrier = carrier(advance, "Finalizer", PLAYER);
            place(advance, anchor, advance.firstSite);
            aboard(advance, aboardActor, carrier, advance.nonBattleground);
            place(advance, carrier, advance.nonBattleground);
            pilot(advance, carrier, aboardActor);
            control(advance, advance.firstSite);

            assertTrue("Moving the carrier group from staging to a second"
                            + " battleground advances: " + label(bot),
                    advance.analyzer.advancesPreFlipActorAtRuntimeLocation(
                            advance.game, PLAYER, carrier,
                            advance.system));

            Fixture churn = fixture(bot, false);
            PhysicalCard churnActor = character(
                    churn, "General Hux", PLAYER, true);
            PhysicalCard churnCarrier = carrier(
                    churn, "Finalizer", PLAYER);
            aboard(churn, churnActor, churnCarrier, churn.firstSite);
            place(churn, churnCarrier, churn.firstSite);
            pilot(churn, churnCarrier, churnActor);
            control(churn, churn.firstSite);

            assertFalse("Trading the sole qualified battleground for another"
                            + " is net zero: " + label(bot),
                    churn.analyzer.advancesPreFlipActorAtRuntimeLocation(
                            churn.game, PLAYER, churnCarrier,
                            churn.system));
            assertEquals("Losing the carrier loses its aboard actor route: "
                            + label(bot),
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                    churn.analyzer.classifyGateFormationPieceIfRemoved(
                            churn.game, PLAYER, churnCarrier));
        }
    }

    @Test
    public void backProtectsExactTwoOccupiedBattlegroundsAndWholePilotGroup() {
        for (Bot bot : Bot.values()) {
            Fixture exactTwo = fixture(bot, true);
            PhysicalCard pilot = character(
                    exactTwo, "Imperial Pilot", PLAYER, false);
            PhysicalCard ship = carrier(exactTwo, "Star Destroyer", PLAYER);
            PhysicalCard ground = character(
                    exactTwo, "Stormtrooper", PLAYER, false);
            aboard(exactTwo, pilot, ship, exactTwo.system);
            place(exactTwo, ship, exactTwo.system);
            pilot(exactTwo, ship, pilot);
            place(exactTwo, ground, exactTwo.firstSite);
            occupy(exactTwo, exactTwo.system);
            occupy(exactTwo, exactTwo.firstSite);

            assertFalse(label(bot), backState(exactTwo).conditionSatisfied());
            assertTrue("Losing the sole physical pilot loses one occupied"
                            + " battleground: " + label(bot),
                    exactTwo.analyzer.wouldDepartureTriggerFlipBack(
                            exactTwo.game, PLAYER, pilot));

            PhysicalCard third = character(
                    exactTwo, "Snowtrooper", PLAYER, false);
            place(exactTwo, third, exactTwo.secondSite);
            occupy(exactTwo, exactTwo.secondSite);
            assertFalse("Three occupied battlegrounds may fall to two: "
                            + label(bot),
                    exactTwo.analyzer.wouldDepartureTriggerFlipBack(
                            exactTwo.game, PLAYER, pilot));

            PhysicalCard blocker = character(
                    exactTwo, "Resistance Agent", OPPONENT, false);
            resistanceAgent(exactTwo, blocker);
            place(exactTwo, blocker, exactTwo.secondSite);
            assertTrue("A site Agent independently satisfies native"
                            + " flip-back: " + label(bot),
                    backState(exactTwo).conditionSatisfied());
            assertFalse("Do not preserve an occupancy seam after flip-back"
                            + " is already pending: " + label(bot),
                    exactTwo.analyzer.wouldDepartureTriggerFlipBack(
                            exactTwo.game, PLAYER, pilot));
        }
    }

    @Test
    public void nativeBattlegroundPullIsPhysicalSourceExactAndStopsAtTwo() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false);
            when(fixture.modifiers.isBattleground(
                    fixture.gameState, fixture.secondSite, null))
                    .thenReturn(false);
            when(fixture.modifiers.isBattleground(
                    fixture.gameState, fixture.system, null))
                    .thenReturn(false);

            PhysicalCard source = card(
                    fixture, "Starkiller Base: Control Room",
                    PLAYER, Zone.AT_LOCATION,
                    CardCategory.LOCATION);
            when(source.getBlueprintId(true)).thenReturn("208_51");
            PhysicalCard candidate = location(
                    "Starkiller Base: Forest", 201,
                    CardSubtype.SITE);
            when(candidate.getOwner()).thenReturn(PLAYER);
            when(candidate.getZone()).thenReturn(Zone.RESERVE_DECK);
            when(candidate.getPartOfSystem())
                    .thenReturn(Title.Starkiller_Base);
            when(fixture.modifiers.isBattleground(
                    fixture.gameState, candidate, null))
                    .thenReturn(true);
            when(fixture.gameState.getReserveDeck(PLAYER))
                    .thenReturn(List.of(candidate));

            assertTrue(label(bot), fixture.analyzer
                    .isIWantThatMapBattlegroundRouteAction(
                        fixture.game, PLAYER, source,
                        "Deploy battleground from Reserve Deck"));
            assertTrue(label(bot), fixture.analyzer
                    .isIWantThatMapNativeBattlegroundRouteCandidate(
                        fixture.game, PLAYER, source, candidate));

            PhysicalCard falseSource = card(
                    fixture, "Unrelated Source", PLAYER,
                    Zone.AT_LOCATION, CardCategory.EFFECT);
            when(falseSource.getBlueprintId(true)).thenReturn("1_1");
            assertFalse("Same text from another source stays neutral: "
                            + label(bot),
                    fixture.analyzer
                        .isIWantThatMapBattlegroundRouteAction(
                            fixture.game, PLAYER, falseSource,
                            "Deploy battleground from Reserve Deck"));

            when(fixture.modifiers.isBattleground(
                    fixture.gameState, fixture.system, null))
                    .thenReturn(true);
            assertTrue("Uncontrolled battlegrounds do not exhaust the route: "
                            + label(bot),
                    fixture.analyzer
                        .isIWantThatMapBattlegroundRouteAction(
                            fixture.game, PLAYER, source,
                            "Deploy battleground from Reserve Deck"));

            PhysicalCard firstActor = character(
                    fixture, "First Order Stormtrooper", PLAYER, true);
            PhysicalCard secondActor = character(
                    fixture, "General Hux", PLAYER, true);
            place(fixture, firstActor, fixture.firstSite);
            place(fixture, secondActor, fixture.system);
            control(fixture, fixture.firstSite);
            control(fixture, fixture.system);
            assertFalse("Two qualified battlegrounds exhaust the route: "
                            + label(bot),
                    fixture.analyzer
                        .isIWantThatMapBattlegroundRouteAction(
                            fixture.game, PLAYER, source,
                            "Deploy battleground from Reserve Deck"));
            assertTrue(label(bot), fixture.analyzer
                    .isExhaustedIWantThatMapBattlegroundRouteAction(
                        fixture.game, PLAYER, source,
                        "Deploy battleground from Reserve Deck"));
        }
    }

    @Test
    public void selfLossAndBackInterruptActionsAreNarrowlyBounded() {
        for (Bot bot : Bot.values()) {
            Fixture front = fixture(bot, false);
            PhysicalCard oldDarkJedi = character(
                    front, "Darth Maul", PLAYER, false);
            when(oldDarkJedi.getZone()).thenReturn(Zone.HAND);
            when(front.modifiers.getAbility(
                    front.gameState, oldDarkJedi)).thenReturn(6.0f);
            assertTrue("A non-Episode-VII Dark Jedi is a self-loss deploy: "
                            + label(bot),
                    front.analyzer.isIWantThatMapSelfLosingDeployCandidate(
                            front.game, PLAYER, oldDarkJedi));
            when(front.modifiers.hasIcon(
                    front.gameState, oldDarkJedi,
                    Icon.EPISODE_VII)).thenReturn(true);
            assertFalse("Episode-VII Dark Jedi survives that rule: "
                            + label(bot),
                    front.analyzer.isIWantThatMapSelfLosingDeployCandidate(
                            front.game, PLAYER, oldDarkJedi));

            PhysicalCard ownAgent = character(
                    front, "Declared Agent", PLAYER, true);
            resistanceAgent(front, ownAgent);
            assertTrue(label(bot), front.analyzer
                    .isIWantThatMapSelfBlockingResistanceAgentAt(
                        front.game, PLAYER, ownAgent,
                        front.firstSite));
            assertFalse(label(bot), front.analyzer
                    .isIWantThatMapSelfBlockingResistanceAgentAt(
                        front.game, PLAYER, ownAgent,
                        front.system));

            Fixture back = fixture(bot, true);
            assertTrue(label(bot), back.analyzer
                    .isIWantThatMapBackInterruptAction(
                        back.game, PLAYER, back.objective,
                        "Stack Interrupt from Lost Pile"));
            assertTrue(label(bot), back.analyzer
                    .isIWantThatMapBackInterruptAction(
                        back.game, PLAYER, back.objective,
                        "Play stacked Interrupt"));
            assertFalse("Front side cannot claim the back payoff: "
                            + label(bot),
                    front.analyzer.isIWantThatMapBackInterruptAction(
                        front.game, PLAYER, front.objective,
                        "Stack Interrupt from Lost Pile"));
            assertFalse("Near text stays neutral: " + label(bot),
                    back.analyzer.isIWantThatMapBackInterruptAction(
                        back.game, PLAYER, back.objective,
                        "Stack a card from Lost Pile"));
        }
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState frontState(
            Fixture fixture) {
        return onlyState(fixture, "preFlip", "flip");
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState backState(
            Fixture fixture) {
        return onlyState(fixture, "postFlip", "flipBack");
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState onlyState(
            Fixture fixture, String phase, String purpose) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER, phase, purpose);
        assertEquals(1, states.size());
        return states.getFirst();
    }

    private static Fixture fixture(Bot bot, boolean flipped) {
        ObjectiveAnalyzer analyzer = bot.analyzer();
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getSide(PLAYER)).thenReturn(Side.DARK);
        when(gameState.getSide(OPPONENT)).thenReturn(Side.LIGHT);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());

        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(flipped ? back : front);
        when(objective.getOtherSideBlueprint()).thenReturn(
                flipped ? front : back);
        when(objective.getBlueprintId(true)).thenReturn("208_57");
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn("I Want That Map");
        when(front.getGameText()).thenReturn(
                "Flip this card if your First Order characters control two battlegrounds and no Resistance Agent is present at a battleground site.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn("And Now You'll Give It To Me");
        when(back.getGameText()).thenReturn(
                "Flip this card if you occupy fewer than two battlegrounds or a Resistance Agent is present at a battleground site.");
        when(back.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);

        PhysicalCard firstSite = location(
                "Tuanul Village", 101, CardSubtype.SITE);
        PhysicalCard secondSite = location(
                "Crait: Resistance Camp", 102, CardSubtype.SITE);
        PhysicalCard system = location(
                "Starkiller Base", 103, CardSubtype.SYSTEM);
        PhysicalCard nonBattleground = location(
                "Coruscant", 104, CardSubtype.SYSTEM);
        when(modifiers.isBattleground(
                gameState, firstSite, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, secondSite, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, system, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, nonBattleground, null)).thenReturn(false);

        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        List<PhysicalCard> locations = List.of(
                firstSite, secondSite, system, nonBattleground);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);

        analyzer.analyze(game, PLAYER, Side.DARK);
        return new Fixture(analyzer, game, gameState, modifiers,
                objective, permanents, firstSite, secondSite,
                system, nonBattleground);
    }

    private static PhysicalCard card(
            Fixture fixture, String title, String owner,
            Zone zone, CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        active(fixture, card);
        return card;
    }

    private static PhysicalCard location(
            String title, int id, CardSubtype subtype) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return card;
    }

    private static PhysicalCard character(
            Fixture fixture, String title, String owner,
            boolean firstOrder) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(fixture.modifiers.hasIcon(
                fixture.gameState, card, Icon.FIRST_ORDER))
                .thenReturn(firstOrder);
        when(fixture.modifiers.hasAbility(
                fixture.gameState, card, true)).thenReturn(true);
        active(fixture, card);
        return card;
    }

    private static PhysicalCard carrier(
            Fixture fixture, String title, String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isUndercover()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.STARSHIP);
        active(fixture, card);
        return card;
    }

    private static void active(Fixture fixture, PhysicalCard card) {
        when(fixture.gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(fixture.gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(true);
    }

    private static void place(
            Fixture fixture, PhysicalCard card,
            PhysicalCard location) {
        if (!fixture.permanents.contains(card)) {
            fixture.permanents.add(card);
        }
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, card)).thenReturn(location);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, card)).thenReturn(location);
    }

    private static void aboard(
            Fixture fixture, PhysicalCard card,
            PhysicalCard carrier, PhysicalCard location) {
        if (!fixture.permanents.contains(card)) {
            fixture.permanents.add(card);
        }
        when(card.getAttachedTo()).thenReturn(carrier);
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, card)).thenReturn(null);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, card)).thenReturn(location);
    }

    private static void pilot(
            Fixture fixture, PhysicalCard carrier,
            PhysicalCard pilot) {
        when(pilot.isPilotOf()).thenReturn(true);
        when(fixture.modifiers.isPiloted(
                fixture.gameState, carrier, false)).thenReturn(true);
        when(fixture.modifiers.getHighestAbilityPiloting(
                fixture.gameState, carrier, false, false))
                .thenReturn(2.0f);
        when(fixture.gameState.getPilotCardsAboard(
                fixture.modifiers, carrier, true))
                .thenReturn(List.of(pilot));
    }

    private static void resistanceAgent(
            Fixture fixture, PhysicalCard card) {
        when(fixture.modifiers.hasKeyword(
                fixture.gameState, card,
                Keyword.RESISTANCE_AGENT)).thenReturn(true);
    }

    private static void control(
            Fixture fixture, PhysicalCard location) {
        when(fixture.modifiers.controlsLocation(
                eq(fixture.gameState), eq(location),
                eq(PLAYER), any())).thenReturn(true);
    }

    private static void occupy(
            Fixture fixture, PhysicalCard location) {
        when(fixture.modifiers.occupiesLocation(
                eq(fixture.gameState), eq(location),
                eq(PLAYER), any())).thenReturn(true);
    }

    private static String label(Bot bot) {
        return "208_57 " + bot;
    }

    private enum Bot {
        RANDO {
            @Override
            ObjectiveAnalyzer analyzer() {
                return new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
            }
        },
        CHOSEN_ONE {
            @Override
            ObjectiveAnalyzer analyzer() {
                return new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
            }
        };

        abstract ObjectiveAnalyzer analyzer();
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard objective,
            List<PhysicalCard> permanents,
            PhysicalCard firstSite,
            PhysicalCard secondSite,
            PhysicalCard system,
            PhysicalCard nonBattleground) { }
}
