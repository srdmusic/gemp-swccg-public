package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndorOperationsRetentionBehaviorTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    private static final String OBJECTIVE_BP = "8_167";
    private static final String ENDOR_BP = "8_157";
    private static final String BUNKER_BP = "8_160";
    private static final String PLATFORM_BP = "8_166";
    private static final String FOREST_BP = "8_164";
    private static final String DARK_FOREST_BP = "8_161";
    private static final String CANTINA_BP = "1_290";
    private static final String BIKER_SCOUT_BP = "8_92";
    private static final String REBEL_BP = "1_19";
    private static final String DYER_BP = "8_93";
    private static final String TEMPEST_ONE_BP = "8_170";

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void rumorsRetentionBecomesUrgentExactlyAtTwoOpponentEndorSites() {
        for (String blueprintId : List.of("8_127", "601_261")) {
            for (boolean flipped : List.of(false, true)) {
                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(bot, flipped);
                    PhysicalCard rumors = fixture.addActive(
                            card(blueprintId, 200,
                                    Zone.SIDE_OF_TABLE, PLAYER));
                    when(fixture.modifiers().mayNotBeCanceled(
                            fixture.gameState(), rumors))
                            .thenReturn(false);
                    fixture.setOpponentControls(
                            fixture.bunker(), true, true);

                    ObjectiveAnalyzer.RequiredCardRetentionRisk
                            oneSiteRisk = fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER,
                                            fixture.forest());
                    assertTrue(label(bot, flipped, blueprintId),
                            oneSiteRisk.applies());
                    assertFalse(label(bot, flipped, blueprintId),
                            oneSiteRisk.removalNow());
                    assertEquals(label(bot, flipped, blueprintId),
                            1, oneSiteRisk.opponentControlCount());
                    assertEquals(label(bot, flipped, blueprintId),
                            2, oneSiteRisk.adverseStepsRemaining());
                    assertFalse(label(bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .isRequiredCardRetentionBattleLocation(
                                            fixture.game(), PLAYER,
                                            fixture.forest()));

                    fixture.setOpponentControls(
                            fixture.platform(), true, true);
                    ObjectiveAnalyzer.RequiredCardRetentionRisk
                            twoSiteRisk = fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER,
                                            fixture.forest());
                    assertFalse(label(bot, flipped, blueprintId),
                            twoSiteRisk.removalNow());
                    assertEquals(label(bot, flipped, blueprintId),
                            2, twoSiteRisk.opponentControlCount());
                    assertEquals(label(bot, flipped, blueprintId),
                            1, twoSiteRisk.adverseStepsRemaining());
                    assertTrue(label(bot, flipped, blueprintId),
                            twoSiteRisk.criticalIfOpponentGainsControl());
                    assertTrue(label(bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .isRequiredCardRetentionBattleLocation(
                                            fixture.game(), PLAYER,
                                            fixture.forest()));

                    ObjectiveAnalyzer.RequiredCardRetentionRisk
                            outsideEndor = fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER,
                                            fixture.cantina());
                    assertTrue(label(bot, flipped, blueprintId),
                            outsideEndor.applies());
                    assertFalse(label(bot, flipped, blueprintId),
                            outsideEndor.inScope());
                    assertFalse(label(bot, flipped, blueprintId),
                            outsideEndor.requiresProtection());
                }
            }
        }
    }

    @Test
    public void departureHoldAppliesOnlyToSoleDefenderAtContestedThirdSite() {
        for (String blueprintId : List.of("8_127", "601_261")) {
            for (boolean flipped : List.of(false, true)) {
                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(bot, flipped);
                    PhysicalCard rumors = fixture.addActive(
                            card(blueprintId, 200,
                                    Zone.SIDE_OF_TABLE, PLAYER));
                    when(fixture.modifiers().mayNotBeCanceled(
                            fixture.gameState(), rumors))
                            .thenReturn(false);
                    fixture.setOpponentControls(
                            fixture.bunker(), true, true);
                    fixture.setOpponentControls(
                            fixture.platform(), true, true);
                    PhysicalCard defender = fixture.placePresence(
                            card(BIKER_SCOUT_BP, 301,
                                    Zone.AT_LOCATION, PLAYER),
                            fixture.forest());
                    fixture.placePresence(
                            card(REBEL_BP, 302,
                                    Zone.AT_LOCATION, OPPONENT),
                            fixture.forest());
                    fixture.setOpponentOccupies(
                            fixture.forest(), true);

                    assertTrue(label(bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .wouldDepartureTriggerRequiredCardRemoval(
                                            fixture.game(), PLAYER,
                                            defender));
                    assertEquals(label(bot, flipped, blueprintId),
                            ObjectiveAnalyzer.FlipGateFormationRole
                                    .REQUIRED_CARD_RETENTION_DEFENDER,
                            fixture.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            fixture.game(), PLAYER,
                                            defender));

                    fixture.setOpponentOccupies(
                            fixture.forest(), false);
                    assertFalse(label(bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .wouldDepartureTriggerRequiredCardRemoval(
                                            fixture.game(), PLAYER,
                                            defender));
                    assertEquals(label(bot, flipped, blueprintId),
                            ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                            fixture.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            fixture.game(), PLAYER,
                                            defender));
                }
            }
        }
    }

    @Test
    public void secretBaseRetentionUsesExactPrintingSpecificLocation() {
        for (RetentionCase retentionCase : List.of(
                new RetentionCase("8_124", Target.ENDOR),
                new RetentionCase("207_25", Target.BUNKER))) {
            for (boolean flipped : List.of(false, true)) {
                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(bot, flipped);
                    fixture.addActive(card(
                            retentionCase.blueprintId(), 200,
                            Zone.SIDE_OF_TABLE, PLAYER));
                    PhysicalCard exact = retentionCase.target()
                                    == Target.ENDOR
                            ? fixture.endor() : fixture.bunker();
                    PhysicalCard wrong = retentionCase.target()
                                    == Target.ENDOR
                            ? fixture.bunker() : fixture.endor();

                    ObjectiveAnalyzer.RequiredCardRetentionRisk
                            neutral = fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER, exact);
                    assertTrue(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            neutral.applies());
                    assertTrue(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            neutral.inScope());
                    assertFalse(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            neutral.removalNow());
                    assertTrue(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            neutral.criticalIfOpponentGainsControl());

                    ObjectiveAnalyzer.RequiredCardRetentionRisk
                            outOfScope = fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER, wrong);
                    assertTrue(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            outOfScope.applies());
                    assertFalse(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            outOfScope.inScope());

                    fixture.setOpponentControls(
                            exact, true, false);
                    ObjectiveAnalyzer.RequiredCardRetentionRisk
                            removal = fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER, exact);
                    assertTrue(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            removal.removalNow());
                    assertTrue(label(
                            bot, flipped,
                            retentionCase.blueprintId()),
                            removal.opponentControlsHere());
                }
            }
        }
    }

    @Test
    public void directDeployPrintingsHaveNoPostDeployRetentionRule() {
        for (String blueprintId : List.of("223_19", "601_260")) {
            for (boolean flipped : List.of(false, true)) {
                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(bot, flipped);
                    fixture.addActive(card(
                            blueprintId, 200,
                            Zone.SIDE_OF_TABLE, PLAYER));
                    fixture.setOpponentControls(
                            fixture.bunker(), true, true);
                    fixture.setOpponentControls(
                            fixture.platform(), true, true);
                    fixture.setOpponentControls(
                            fixture.forest(), true, true);
                    fixture.setOpponentControls(
                            fixture.endor(), true, false);

                    assertFalse(label(bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER,
                                            fixture.bunker())
                                    .applies());
                    assertFalse(label(bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .assessRequiredCardRetentionRisk(
                                            fixture.game(), PLAYER,
                                            fixture.endor())
                                    .applies());
                }
            }
        }
    }

    @Test
    public void dyerClosesCancellationRiskAndIsTheProtectedRetentionPiece() {
        for (boolean flipped : List.of(false, true)) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, flipped);
                PhysicalCard rumors = fixture.addActive(
                        card("8_127", 200,
                                Zone.SIDE_OF_TABLE, PLAYER));
                fixture.setOpponentControls(
                        fixture.bunker(), true, true);
                fixture.setOpponentControls(
                        fixture.platform(), true, true);
                fixture.setOpponentControls(
                        fixture.forest(), true, true);
                PhysicalCard dyer = fixture.placePresence(
                        card(DYER_BP, 303,
                                Zone.AT_LOCATION, PLAYER),
                        fixture.darkForest());
                when(fixture.modifiers().mayNotBeCanceled(
                        fixture.gameState(), rumors))
                        .thenReturn(true);

                assertFalse(label(bot, flipped, DYER_BP),
                        fixture.analyzer()
                                .assessRequiredCardRetentionRisk(
                                        fixture.game(), PLAYER,
                                        fixture.darkForest())
                                .applies());
                assertEquals(label(bot, flipped, DYER_BP),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .REQUIRED_CARD_RETENTION_DEFENDER,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER, dyer));
            }
        }
    }

    @Test
    public void dyerIsHeldWhenDepartureBothGrantsThirdControlAndReenablesCancel() {
        for (boolean flipped : List.of(false, true)) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, flipped);
                PhysicalCard rumors = fixture.addActive(
                        card("8_127", 200,
                                Zone.SIDE_OF_TABLE, PLAYER));
                fixture.setOpponentControls(
                        fixture.bunker(), true, true);
                fixture.setOpponentControls(
                        fixture.platform(), true, true);
                PhysicalCard dyer = fixture.placePresence(
                        card(DYER_BP, 303,
                                Zone.AT_LOCATION, PLAYER),
                        fixture.forest());
                fixture.placePresence(
                        card(REBEL_BP, 304,
                                Zone.AT_LOCATION, OPPONENT),
                        fixture.forest());
                fixture.setOpponentOccupies(
                        fixture.forest(), true);
                when(fixture.modifiers().mayNotBeCanceled(
                        fixture.gameState(), rumors))
                        .thenReturn(true);

                assertEquals(label(bot, flipped, DYER_BP),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .REQUIRED_CARD_RETENTION_DEFENDER,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER, dyer));
            }
        }
    }

    @Test
    public void soleDyerProtectionOpensAtTwoAndClosesWithSecondPreventer() {
        for (String blueprintId : List.of(
                "8_127", "601_261")) {
            for (boolean flipped : List.of(false, true)) {
                for (Bot bot : Bot.values()) {
                    Fixture urgent = fixture(
                            bot, flipped);
                    PhysicalCard urgentRumors =
                            urgent.addActive(card(
                                    blueprintId, 200,
                                    Zone.SIDE_OF_TABLE,
                                    PLAYER));
                    urgent.setOpponentControls(
                            urgent.bunker(), true, true);
                    urgent.setOpponentControls(
                            urgent.platform(), true, true);
                    PhysicalCard soleDyer =
                            urgent.placePresence(
                                    card(DYER_BP, 303,
                                            Zone.AT_LOCATION,
                                            PLAYER),
                                    urgent.forest());
                    when(urgent.modifiers()
                            .mayNotBeCanceled(
                                    urgent.gameState(),
                                    urgentRumors))
                            .thenReturn(true);

                    assertTrue(label(
                            bot, flipped, blueprintId),
                            urgent.analyzer()
                                    .isRequiredCardCancelPreventerDefenseLocation(
                                            urgent.game(),
                                            PLAYER,
                                            urgent.forest()));
                    assertEquals(label(
                            bot, flipped, blueprintId),
                            ObjectiveAnalyzer
                                    .FlipGateFormationRole
                                    .REQUIRED_CARD_RETENTION_DEFENDER,
                            urgent.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            urgent.game(),
                                            PLAYER,
                                            soleDyer));

                    PhysicalCard secondDyer =
                            urgent.placePresence(
                                    card(DYER_BP, 304,
                                            Zone.AT_LOCATION,
                                            PLAYER),
                                    urgent.darkForest());
                    assertFalse(label(
                            bot, flipped, blueprintId),
                            urgent.analyzer()
                                    .isRequiredCardCancelPreventerDefenseLocation(
                                            urgent.game(),
                                            PLAYER,
                                            urgent.forest()));
                    assertFalse(label(
                            bot, flipped, blueprintId),
                            urgent.analyzer()
                                    .isRequiredCardCancelPreventerDefenseLocation(
                                            urgent.game(),
                                            PLAYER,
                                            urgent.darkForest()));
                    assertEquals(label(
                            bot, flipped, blueprintId),
                            ObjectiveAnalyzer
                                    .FlipGateFormationRole.NONE,
                            urgent.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            urgent.game(),
                                            PLAYER,
                                            soleDyer));
                    assertEquals(label(
                            bot, flipped, blueprintId),
                            ObjectiveAnalyzer
                                    .FlipGateFormationRole.NONE,
                            urgent.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            urgent.game(),
                                            PLAYER,
                                            secondDyer));

                    Fixture early = fixture(
                            bot, flipped);
                    PhysicalCard earlyRumors =
                            early.addActive(card(
                                    blueprintId, 200,
                                    Zone.SIDE_OF_TABLE,
                                    PLAYER));
                    early.setOpponentControls(
                            early.bunker(), true, true);
                    PhysicalCard earlyDyer =
                            early.placePresence(
                                    card(DYER_BP, 303,
                                            Zone.AT_LOCATION,
                                            PLAYER),
                                    early.forest());
                    when(early.modifiers()
                            .mayNotBeCanceled(
                                    early.gameState(),
                                    earlyRumors))
                            .thenReturn(true);

                    assertFalse(label(
                            bot, flipped, blueprintId),
                            early.analyzer()
                                    .isRequiredCardCancelPreventerDefenseLocation(
                                            early.game(),
                                            PLAYER,
                                            early.forest()));
                    assertEquals(label(
                            bot, flipped, blueprintId),
                            ObjectiveAnalyzer
                                    .FlipGateFormationRole.NONE,
                            early.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            early.game(),
                                            PLAYER,
                                            earlyDyer));
                }
            }
        }
    }

    @Test
    public void dyerAboardVehicleProtectsAndClassifiesTheCarrier() {
        for (String blueprintId : List.of(
                "8_127", "601_261")) {
            for (boolean flipped : List.of(false, true)) {
                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(
                            bot, flipped);
                    PhysicalCard rumors =
                            fixture.addActive(card(
                                    blueprintId, 200,
                                    Zone.SIDE_OF_TABLE,
                                    PLAYER));
                    fixture.setOpponentControls(
                            fixture.bunker(), true, true);
                    fixture.setOpponentControls(
                            fixture.platform(), true, true);
                    PhysicalCard carrier =
                            fixture.placePresence(
                                    card(TEMPEST_ONE_BP, 303,
                                            Zone.AT_LOCATION,
                                            PLAYER),
                                    fixture.forest());
                    PhysicalCard dyer =
                            fixture.addActive(card(
                                    DYER_BP, 304,
                                    Zone.AT_LOCATION,
                                    PLAYER));
                    when(dyer.getAttachedTo())
                            .thenReturn(carrier);
                    when(fixture.modifiers()
                            .getLocationThatCardIsPresentAt(
                                    fixture.gameState(),
                                    dyer))
                            .thenReturn(null);
                    when(fixture.modifiers()
                            .getLocationThatCardIsAt(
                                    fixture.gameState(),
                                    dyer))
                            .thenReturn(fixture.forest());
                    when(fixture.modifiers()
                            .mayNotBeCanceled(
                                    fixture.gameState(),
                                    rumors))
                            .thenReturn(true);

                    assertTrue(label(
                            bot, flipped, blueprintId),
                            fixture.analyzer()
                                    .isRequiredCardCancelPreventerDefenseLocation(
                                            fixture.game(),
                                            PLAYER,
                                            fixture.forest()));
                    assertEquals(label(
                            bot, flipped, blueprintId),
                            ObjectiveAnalyzer
                                    .FlipGateFormationRole
                                    .REQUIRED_CARD_RETENTION_DEFENDER,
                            fixture.analyzer()
                                    .classifyGateFormationPieceIfRemoved(
                                            fixture.game(),
                                            PLAYER,
                                            carrier));
                    assertTrue(label(
                            bot, flipped, blueprintId),
                            MoveObjectiveGateHoldPolicy
                                    .evaluateRequiredCardRetentionDefender(
                                            fixture.analyzer()
                                                    .classifyGateFormationPieceIfRemoved(
                                                            fixture.game(),
                                                            PLAYER,
                                                            carrier),
                                            8.0f, 14.0f)
                                    .hardVeto());
                }
            }
        }
    }

    @Test
    public void retentionDefenderPoliciesReleaseOnlyBeyondSixPowerDeficit() {
        ObjectiveAnalyzer.FlipGateFormationRole role =
                ObjectiveAnalyzer.FlipGateFormationRole
                        .REQUIRED_CARD_RETENTION_DEFENDER;

        MoveObjectiveGateHoldPolicy.Evaluation held =
                MoveObjectiveGateHoldPolicy
                        .evaluateRequiredCardRetentionDefender(
                                role, 8.0f, 14.0f);
        MoveObjectiveGateHoldPolicy.Evaluation released =
                MoveObjectiveGateHoldPolicy
                        .evaluateRequiredCardRetentionDefender(
                                role, 8.0f, 15.0f);
        assertEquals(MoveObjectiveGateHoldPolicy.Branch
                        .HOLD_REQUIRED_CARD_RETENTION_DEFENDER,
                held.branch());
        assertTrue(held.hardVeto());
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE,
                released.branch());
        assertFalse(released.hardVeto());

        var avoidable = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "retention-defender", role, true);
        assertEquals(1, avoidable.operations().size());
        assertEquals(
                "BATTLE.OBJECTIVE.REQUIRED_CARD_RETENTION_HOLD",
                avoidable.operations().get(0).ruleArmId().id());
        assertEquals(-9999.0f,
                avoidable.operations().get(0).delta(), 0.0f);
        assertTrue(BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "retention-defender", role, false)
                .operations().isEmpty());
    }

    private static Fixture fixture(Bot bot, boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        List<PhysicalCard> permanents = new ArrayList<>();
        List<PhysicalCard> locations = new ArrayList<>();

        PhysicalCard objective = objective(flipped);
        PhysicalCard endor = location(ENDOR_BP, 101);
        PhysicalCard bunker = location(BUNKER_BP, 102);
        PhysicalCard platform = location(PLATFORM_BP, 103);
        PhysicalCard forest = location(FOREST_BP, 104);
        PhysicalCard darkForest = location(DARK_FOREST_BP, 105);
        PhysicalCard cantina = location(CANTINA_BP, 106);
        locations.addAll(List.of(
                endor, bunker, platform,
                forest, darkForest, cantina));
        permanents.add(objective);
        permanents.addAll(locations);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getReserveDeck(PLAYER)).thenReturn(List.of());
        when(gameState.getCardPile(
                PLAYER, Zone.RESERVE_DECK)).thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(modifiers.hasKeyword(
                any(GameState.class), any(PhysicalCard.class), any()))
                .thenAnswer(invocation -> {
                    PhysicalCard candidate =
                            invocation.getArgument(1);
                    return candidate != null
                            && candidate.getBlueprint() != null
                            && candidate.getBlueprint().hasKeyword(
                                    invocation.getArgument(2));
                });
        setActive(gameState, objective);
        for (PhysicalCard location : locations) {
            setActive(gameState, location);
            when(modifiers.getLocationHere(
                    gameState, location)).thenReturn(location);
            when(modifiers.isBattleground(
                    gameState, location, null)).thenReturn(true);
        }

        ObjectiveAnalyzer analyzer = bot.analyzer();
        analyzer.analyze(game, PLAYER, Side.DARK);
        assertEquals("Endor Operations",
                analyzer.getActivePlaybook().label);
        return new Fixture(
                analyzer, game, gameState, modifiers,
                permanents, endor, bunker, platform,
                forest, darkForest, cantina);
    }

    private static PhysicalCard objective(boolean flipped) {
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(OBJECTIVE_BP);
        SwccgCardBlueprint back =
                blueprint(OBJECTIVE_BP + "_BACK");
        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint())
                .thenReturn(flipped ? back : front);
        when(objective.getOtherSideBlueprint())
                .thenReturn(flipped ? front : back);
        when(objective.getBlueprintId(true))
                .thenReturn(OBJECTIVE_BP);
        when(objective.getBlueprintId(false))
                .thenReturn(flipped
                        ? OBJECTIVE_BP + "_BACK"
                        : OBJECTIVE_BP);
        when(objective.getTitle()).thenReturn(
                (flipped ? back : front).getTitle());
        when(objective.getTitles()).thenReturn(
                List.of((flipped ? back : front).getTitle()));
        when(objective.isFlipped()).thenReturn(flipped);
        return objective;
    }

    private static PhysicalCard location(
            String blueprintId, int cardId) {
        PhysicalCard location = card(
                blueprintId, cardId,
                Zone.LOCATIONS, PLAYER);
        String systemName = blueprint(blueprintId).getSystemName();
        when(location.getPartOfSystem()).thenReturn(
                systemName);
        return location;
    }

    private static PhysicalCard card(
            String blueprintId, int cardId,
            Zone zone, String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        return card;
    }

    private static void setActive(
            GameState gameState, PhysicalCard card) {
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, false, true, false, false,
                false, false, false, false)).thenReturn(true);
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
    }

    private static String label(
            Bot bot, boolean flipped, String blueprintId) {
        return bot + " " + (flipped ? "back" : "front")
                + " " + blueprintId;
    }

    private enum Target {
        ENDOR,
        BUNKER
    }

    private record RetentionCase(
            String blueprintId, Target target) {
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
            List<PhysicalCard> permanents,
            PhysicalCard endor,
            PhysicalCard bunker,
            PhysicalCard platform,
            PhysicalCard forest,
            PhysicalCard darkForest,
            PhysicalCard cantina) {

        private PhysicalCard addActive(PhysicalCard card) {
            permanents.add(card);
            setActive(gameState, card);
            return card;
        }

        private PhysicalCard placePresence(
                PhysicalCard card, PhysicalCard location) {
            addActive(card);
            when(card.getAtLocation()).thenReturn(location);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, card)).thenReturn(location);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, card)).thenReturn(location);
            when(modifiers.hasAbility(
                    gameState, card, true)).thenReturn(true);
            return card;
        }

        private void setOpponentControls(
                PhysicalCard location,
                boolean controls,
                boolean includeExcluded) {
            when(modifiers.controlsLocation(
                    gameState, location, OPPONENT,
                    includeExcluded
                            ? SpotOverride
                                    .INCLUDE_EXCLUDED_FROM_BATTLE
                            : null))
                    .thenReturn(controls);
        }

        private void setOpponentOccupies(
                PhysicalCard location, boolean occupies) {
            when(modifiers.occupiesLocation(
                    gameState, location, OPPONENT,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(occupies);
        }
    }
}
