package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirstOrderReignsObjectiveBehaviorTest {
    private static final String PLAYER_ID = "player";
    private static final String OPPONENT_ID = "opponent";

    @Test
    public void bothPrintingsHydrateTheSharedRouteForBothFacades() {
        for (String blueprintId : List.of("225_32", "501_60")) {
            for (ObjectiveAnalyzer analyzer : facades()) {
                Fixture fixture = fixture(
                        analyzer, blueprintId, false);

                assertTrue(analyzer.isHydratedFromJson());
                assertNotNull(analyzer.getActivePlaybook());
                assertEquals("The First Order Reigns",
                        analyzer.getActivePlaybook().label);
                assertTrue(analyzer.getActivePlaybook()
                        .identity.matches(blueprintId, null));
                assertTrue(analyzer.getRequiredCardsOnTable().isEmpty());
                assertTrue(analyzer.getPullableCards().isEmpty());

                List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                        analyzer.assessFlipLocationRules(
                                fixture.game, PLAYER_ID,
                                "preFlip", "flip");
                assertEquals(1, states.size());
                assertEquals(
                        "first-order-reigns-tracked-fleet-control",
                        states.get(0).ruleId());
            }
        }
    }

    @Test
    public void movingActiveFleetHostResolvesAtQueryTimeAndUsesOrdinaryControl() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", false);

            assertSame(fixture.host,
                    analyzer.getFirstOrderReignsTrackedFleetHostSystem(
                            fixture.game, PLAYER_ID));
            when(fixture.trackedFleet.getAttachedTo())
                    .thenReturn(fixture.relocatedHost);
            assertSame(fixture.relocatedHost,
                    analyzer.getFirstOrderReignsTrackedFleetHostSystem(
                            fixture.game, PLAYER_ID));
            assertTrue(analyzer.preFlipRequirementUsesOrdinaryControl(
                    fixture.game, PLAYER_ID, fixture.relocatedHost));

            when(fixture.modifiers.controlsLocation(
                    fixture.gameState, fixture.relocatedHost,
                    PLAYER_ID)).thenReturn(true);
            assertTrue(analyzer.isFirstOrderReignsHostControlled(
                    fixture.game, PLAYER_ID));
            assertFalse(analyzer.isFirstOrderReignsRouteOpen(
                    fixture.game, PLAYER_ID));

            List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                    analyzer.assessFlipLocationRules(
                            fixture.game, PLAYER_ID,
                            "preFlip", "flip");
            assertEquals(1, states.size());
            assertTrue(states.get(0).conditionSatisfied());
        }
    }

    @Test
    public void routePullsAnExactStageBeforeItProtectsPhysicalSupremacy() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", false);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_LOCATION,
                    analyzer.classifyPreFlipProgressCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.stage));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    analyzer.classifyPreFlipProgressCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.supremacy));

            when(fixture.stage.getZone())
                    .thenReturn(Zone.LOCATIONS);
            fixture.locations.add(fixture.stage);
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_ACTOR,
                    analyzer.classifyPreFlipProgressCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.supremacy));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    analyzer.classifyPreFlipProgressCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.trackedFleet));

            assertFalse(analyzer.isRequiredCardForFlip(
                    fixture.trackedFleet));
            assertFalse(analyzer.isPullableCard(
                    fixture.trackedFleet));
            assertFalse(analyzer
                    .isPreferredFirstOrderReignsRouteForceLossCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.trackedFleet));
        }
    }

    @Test
    public void undeployedRouteReservesSevenAndProtectsOnlyTheSoleSupremacy() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", false);
            fixture.locations.add(fixture.stage);
            fixture.reserve.add(fixture.supremacy);

            assertEquals(7,
                    analyzer.getFirstOrderReignsRouteForceReserve(
                            fixture.game, PLAYER_ID,
                            fixture.trackedFleet));
            assertEquals(0,
                    analyzer.getFirstOrderReignsRouteForceReserve(
                            fixture.game, PLAYER_ID,
                            fixture.supremacy));
            assertEquals(0,
                    analyzer.getFirstOrderReignsCurrentMoveForceReserve(
                            fixture.game, PLAYER_ID));
            assertTrue(analyzer
                    .isPreferredFirstOrderReignsRouteForceLossCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.supremacy));

            PhysicalCard secondSupremacy = card(
                    "Supremacy", PLAYER_ID, Zone.HAND,
                    CardCategory.STARSHIP, null, 205);
            when(fixture.modifiers.getCardTypes(
                    fixture.gameState, secondSupremacy))
                    .thenReturn(Set.of(CardType.STARSHIP));
            fixture.hand.add(secondSupremacy);
            assertFalse(analyzer
                    .isPreferredFirstOrderReignsRouteForceLossCandidate(
                            fixture.game, PLAYER_ID,
                            fixture.supremacy));
        }
    }

    @Test
    public void deployedSupremacyAdvancesOnlyByLegalStrictlyCloserSystemHops() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", false);
            PhysicalCard origin = system(
                    "Crait", 8, 301);
            PhysicalCard nonEpisodeSevenCloser = system(
                    "Kijimi", 6, 302);
            PhysicalCard sameDistance = system(
                    "Bespin", 8, 303);
            PhysicalCard farther = system(
                    "Tatooine", 9, 304);
            PhysicalCard illegalCloser = system(
                    "Coruscant", 7, 305);

            putSupremacyOnTable(fixture, origin, 10.0f);
            when(fixture.modifiers
                    .mayNotMoveFromLocationToLocationUsingHyperspeed(
                            fixture.gameState, fixture.supremacy,
                            origin, illegalCloser, false))
                    .thenReturn(true);

            assertFalse(fixture.modifiers.hasIcon(
                    fixture.gameState, nonEpisodeSevenCloser,
                    Icon.EPISODE_VII));
            assertTrue(analyzer.advancesPreFlipRequirementAt(
                    fixture.game, PLAYER_ID, fixture.supremacy,
                    nonEpisodeSevenCloser));
            assertTrue(analyzer
                    .advancesPreFlipActorAtRuntimeLocation(
                            fixture.game, PLAYER_ID,
                            fixture.supremacy,
                            nonEpisodeSevenCloser));
            assertFalse(analyzer.advancesPreFlipRequirementAt(
                    fixture.game, PLAYER_ID, fixture.supremacy,
                    sameDistance));
            assertFalse(analyzer.advancesPreFlipRequirementAt(
                    fixture.game, PLAYER_ID, fixture.supremacy,
                    farther));
            assertFalse(analyzer.advancesPreFlipRequirementAt(
                    fixture.game, PLAYER_ID, fixture.supremacy,
                    illegalCloser));
        }
    }

    @Test
    public void exactRelocatedHostWinsWhenParsecsTieButAnotherSystemDoesNot() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", false);
            PhysicalCard sameParsecNonHost = system(
                    "Bespin", 5, 306);
            when(fixture.relocatedHost.getParsec())
                    .thenReturn(5);
            when(fixture.trackedFleet.getAttachedTo())
                    .thenReturn(fixture.relocatedHost);
            putSupremacyOnTable(
                    fixture, fixture.host, 10.0f);

            assertTrue(analyzer.advancesPreFlipRequirementAt(
                    fixture.game, PLAYER_ID, fixture.supremacy,
                    fixture.relocatedHost));
            assertFalse(analyzer.advancesPreFlipRequirementAt(
                    fixture.game, PLAYER_ID, fixture.supremacy,
                    sameParsecNonHost));
        }
    }

    @Test
    public void relocationReserveFundsTheNextPaidHopButNotTheFreeFinalHop() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", false);
            PhysicalCard crait = system(
                    "Crait", 8, 311);
            PhysicalCard kijimi = system(
                    "Kijimi", 6, 312);
            fixture.locations.add(crait);
            fixture.locations.add(kijimi);
            putSupremacyOnTable(fixture, crait, 2.0f);
            when(fixture.modifiers.getMoveUsingHyperspeedCost(
                    fixture.gameState, fixture.supremacy,
                    crait, kijimi, false, 0.0f))
                    .thenReturn(1.0f);

            assertEquals(1,
                    analyzer.getFirstOrderReignsRouteForceReserve(
                            fixture.game, PLAYER_ID,
                            fixture.trackedFleet));
            assertEquals(1,
                    analyzer.getFirstOrderReignsCurrentMoveForceReserve(
                            fixture.game, PLAYER_ID));

            when(fixture.supremacy.getAtLocation())
                    .thenReturn(kijimi);
            when(fixture.modifiers.getLocationThatCardIsAt(
                    fixture.gameState, fixture.supremacy))
                    .thenReturn(kijimi);
            when(fixture.modifiers.getMoveUsingHyperspeedCost(
                    fixture.gameState, fixture.supremacy,
                    kijimi, fixture.host, false, 0.0f))
                    .thenReturn(0.0f);
            assertEquals(0,
                    analyzer.getFirstOrderReignsRouteForceReserve(
                            fixture.game, PLAYER_ID,
                            fixture.trackedFleet));
            assertEquals(0,
                    analyzer.getFirstOrderReignsCurrentMoveForceReserve(
                            fixture.game, PLAYER_ID));
        }
    }

    @Test
    public void postFlipKyloGetsNoEmptySiteBonusAndHeroConjunctionIsVetoed() {
        for (ObjectiveAnalyzer analyzer : facades()) {
            Fixture fixture = fixture(
                    analyzer, "225_32", true);

            assertFalse(analyzer
                    .isFirstOrderReignsTerminalDeploymentHazardAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, fixture.saltPlateau));
            boolean emptySiteDefense =
                    analyzer.advancesObjectiveHardLossDefenseAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, fixture.saltPlateau);
            assertFalse(emptySiteDefense);
            assertTrue(DeployObjectiveSitingPolicy
                    .scoreObjectiveHardLossDefense(
                            "empty-salt", emptySiteDefense)
                    .operations().isEmpty());

            fixture.permanents.add(fixture.han);
            when(fixture.modifiers.getLocationThatCardIsPresentAt(
                    fixture.gameState, fixture.han))
                    .thenReturn(fixture.saltPlateau);
            assertTrue(analyzer
                    .isFirstOrderReignsTerminalDeploymentHazardAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, fixture.saltPlateau));

            PhysicalCard hostAtThreatenedSalt = card(
                    "First Order Shuttle", PLAYER_ID,
                    Zone.AT_LOCATION,
                    CardCategory.STARSHIP, null, 401);
            PhysicalCard otherSalt = card(
                    "Crait: Salt Plateau", PLAYER_ID,
                    Zone.LOCATIONS,
                    CardCategory.LOCATION,
                    CardSubtype.SITE, 402);
            PhysicalCard hostAtOtherSalt = card(
                    "First Order Transport", PLAYER_ID,
                    Zone.AT_LOCATION,
                    CardCategory.STARSHIP, null, 403);
            fixture.permanents.add(hostAtThreatenedSalt);
            fixture.permanents.add(hostAtOtherSalt);
            when(fixture.modifiers.getLocationThatCardIsAt(
                    fixture.gameState, hostAtThreatenedSalt))
                    .thenReturn(fixture.saltPlateau);
            when(fixture.modifiers.getLocationThatCardIsAt(
                    fixture.gameState, hostAtOtherSalt))
                    .thenReturn(otherSalt);
            assertTrue(analyzer
                    .isFirstOrderReignsTerminalDeploymentHazardAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, hostAtThreatenedSalt));
            assertFalse(analyzer
                    .isFirstOrderReignsTerminalDeploymentHazardAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, hostAtOtherSalt));
            assertFalse(analyzer
                    .isFirstOrderReignsTerminalDeploymentHazardAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, otherSalt));

            PolicyResult veto =
                    DeployObjectiveSitingPolicy
                            .blockTerminalObjectiveExposure(
                                    "hero-at-salt", true);
            assertEquals(1, veto.operations().size());
            assertEquals(PolicyOperationKind.HARD_VETO,
                    veto.operations().get(0).kind());

            when(fixture.kylo.getZone())
                    .thenReturn(Zone.AT_LOCATION);
            fixture.permanents.add(fixture.kylo);
            when(fixture.modifiers.getLocationThatCardIsPresentAt(
                    fixture.gameState, fixture.kylo))
                    .thenReturn(fixture.host);
            boolean moveExposure =
                    analyzer.isFirstOrderReignsTerminalExposureAt(
                            fixture.game, PLAYER_ID,
                            fixture.kylo, fixture.saltPlateau);
            MoveDestinationPolicy.CompanionVeto moveVeto =
                    MoveDestinationPolicy.terminalObjectiveExposure(
                            moveExposure);
            assertTrue(moveExposure);
            assertTrue(moveVeto.hardVeto());
            assertFalse(MoveDestinationPolicy
                    .terminalObjectiveExposure(
                            analyzer
                                .isFirstOrderReignsTerminalExposureAt(
                                    fixture.game, PLAYER_ID,
                                    fixture.kylo, otherSalt))
                    .hardVeto());

            when(fixture.modifiers.getLocationThatCardIsPresentAt(
                    fixture.gameState, fixture.kylo))
                    .thenReturn(fixture.saltPlateau);
            assertTrue(analyzer
                    .isFirstOrderReignsTerminalBattleHazardAt(
                            fixture.game, PLAYER_ID,
                            fixture.saltPlateau));
        }
    }

    private static List<ObjectiveAnalyzer> facades() {
        return List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer());
    }

    private static Fixture fixture(
            ObjectiveAnalyzer analyzer,
            String blueprintId,
            boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front =
                mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back =
                mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER_ID))
                .thenReturn(OPPONENT_ID);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER_ID))
                .thenReturn(OPPONENT_ID);

        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone())
                .thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint())
                .thenReturn(flipped ? back : front);
        when(objective.getOtherSideBlueprint())
                .thenReturn(flipped ? front : back);
        when(objective.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(objective.getPermanentCardId()).thenReturn(1);
        when(objective.getCardId()).thenReturn(1);
        when(objective.getAdditionalCardIds())
                .thenReturn(List.of());
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle())
                .thenReturn("The First Order Reigns");
        when(front.getGameText()).thenReturn(
                "Deploy D'Qar and Crait systems, Salt Plateau, "
                        + "and Tracked Fleet. Once per turn, may deploy "
                        + "Supremacy card or battleground. Flip this card "
                        + "if Tracked Fleet is 'annihilated'.");
        when(front.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle())
                .thenReturn("The Resistance Is Doomed");
        when(back.getGameText()).thenReturn(
                "If you just lost a battle at Salt Plateau where "
                        + "opponent's Han, Leia, or Luke was present, "
                        + "and your Kylo was just forfeited, place this "
                        + "objective out of play.");
        when(back.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);

        PhysicalCard host = system(
                "D'Qar", 5, 101);
        PhysicalCard relocatedHost = system(
                "Jakku", 7, 102);
        PhysicalCard stage = system(
                "Ahch-To", 7, 103);
        when(stage.getZone())
                .thenReturn(Zone.RESERVE_DECK);
        when(modifiers.hasIcon(
                gameState, stage, Icon.EPISODE_VII))
                .thenReturn(true);
        when(modifiers.isBattleground(
                gameState, stage, null))
                .thenReturn(true);

        PhysicalCard trackedFleet = card(
                "Tracked Fleet", PLAYER_ID,
                Zone.SIDE_OF_TABLE,
                CardCategory.EPIC_EVENT, null, 201);
        when(trackedFleet.getAttachedTo())
                .thenReturn(host);
        PhysicalCard supremacy = card(
                "Supremacy", PLAYER_ID,
                Zone.RESERVE_DECK,
                CardCategory.STARSHIP, null, 202);
        PhysicalCard kylo = card(
                "Kylo Ren", PLAYER_ID,
                Zone.HAND,
                CardCategory.CHARACTER, null, 203);
        PhysicalCard han = card(
                "Han Solo", OPPONENT_ID,
                Zone.AT_LOCATION,
                CardCategory.CHARACTER, null, 204);
        PhysicalCard saltPlateau = card(
                "Crait: Salt Plateau", PLAYER_ID,
                Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SITE, 105);
        when(modifiers.hasPersona(
                gameState, kylo, Persona.KYLO))
                .thenReturn(true);
        when(modifiers.hasPersona(
                gameState, han, Persona.HAN))
                .thenReturn(true);
        when(modifiers.hasAbility(
                gameState, kylo, true))
                .thenReturn(true);
        when(modifiers.getCardTypes(
                gameState, supremacy))
                .thenReturn(Set.of(CardType.STARSHIP));

        setActive(gameState, trackedFleet, true);
        setActive(gameState, supremacy, true);
        setActive(gameState, kylo, true);
        setActive(gameState, han, true);

        List<PhysicalCard> permanents =
                new ArrayList<>(List.of(
                        objective, trackedFleet));
        List<PhysicalCard> locations =
                new ArrayList<>(List.of(host));
        List<PhysicalCard> hand = new ArrayList<>();
        List<PhysicalCard> reserve = new ArrayList<>();
        when(gameState.getAllPermanentCards())
                .thenAnswer(invocation ->
                        new ArrayList<>(permanents));
        when(gameState.getLocationsInOrder())
                .thenAnswer(invocation ->
                        new ArrayList<>(locations));
        when(gameState.getTopLocations())
                .thenAnswer(invocation ->
                        new ArrayList<>(locations));
        when(gameState.getHand(PLAYER_ID))
                .thenAnswer(invocation ->
                        new ArrayList<>(hand));
        when(gameState.getReserveDeck(PLAYER_ID))
                .thenAnswer(invocation ->
                        new ArrayList<>(reserve));
        when(gameState.findCardByPermanentId(202))
                .thenReturn(supremacy);

        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Fixture(
                analyzer, game, gameState, modifiers,
                host, relocatedHost, stage, saltPlateau,
                trackedFleet, supremacy, kylo, han,
                permanents, locations, hand, reserve);
    }

    private static void putSupremacyOnTable(
            Fixture fixture,
            PhysicalCard origin,
            float hyperspeed) {
        when(fixture.supremacy.getZone())
                .thenReturn(Zone.AT_LOCATION);
        when(fixture.supremacy.getAtLocation())
                .thenReturn(origin);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, fixture.supremacy))
                .thenReturn(origin);
        when(fixture.modifiers.isPiloted(
                fixture.gameState, fixture.supremacy, false))
                .thenReturn(true);
        when(fixture.modifiers.hasAstromechOrNavComputer(
                fixture.gameState, fixture.supremacy))
                .thenReturn(true);
        when(fixture.modifiers.getHyperspeed(
                eq(fixture.gameState),
                eq(fixture.supremacy),
                any(PhysicalCard.class),
                any(PhysicalCard.class)))
                .thenReturn(hyperspeed);
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER_ID))
                .thenReturn(10);
        fixture.permanents.add(fixture.supremacy);
    }

    private static void setActive(
            GameState gameState,
            PhysicalCard card,
            boolean active) {
        when(gameState.isCardInPlayActive(
                card, false, false, false,
                false, false, false, false, false))
                .thenReturn(active);
        when(gameState.isCardInPlayActive(
                card, true, false, false,
                false, false, false, false, false))
                .thenReturn(active);
    }

    private static PhysicalCard system(
            String title, int parsec, int id) {
        PhysicalCard system = card(
                title, PLAYER_ID, Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SYSTEM, id);
        when(system.getParsec()).thenReturn(parsec);
        return system;
    }

    private static PhysicalCard card(
            String title,
            String owner,
            Zone zone,
            CardCategory category,
            CardSubtype subtype,
            int id) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds())
                .thenReturn(List.of());
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory())
                .thenReturn(category);
        if (subtype != null) {
            when(blueprint.getCardSubtype())
                    .thenReturn(subtype);
        }
        return card;
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard host,
            PhysicalCard relocatedHost,
            PhysicalCard stage,
            PhysicalCard saltPlateau,
            PhysicalCard trackedFleet,
            PhysicalCard supremacy,
            PhysicalCard kylo,
            PhysicalCard han,
            List<PhysicalCard> permanents,
            List<PhysicalCard> locations,
            List<PhysicalCard> hand,
            List<PhysicalCard> reserve) { }
}
