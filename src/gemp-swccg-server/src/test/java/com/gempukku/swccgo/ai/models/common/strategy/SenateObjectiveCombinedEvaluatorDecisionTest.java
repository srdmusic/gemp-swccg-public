package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Agenda;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Production evaluator proof for the mirrored Senate objective playbook. */
public class SenateObjectiveCombinedEvaluatorDecisionTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final int SENATE_ID = 100;
    private static final int OTHER_SITE_ID = 101;
    private static final int HAND_SENATOR_ID = 300;
    private static final int HAND_EFFECT_ID = 301;
    private static final int HAND_GENERIC_ID = 305;
    private static final int BATTLE_FODDER_ID = 400;
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    private static final List<Family> FAMILIES = List.of(
            new Family("My Lord, Is That Legal?", "12_179",
                    "12_179_BACK", "12_167", "12_166",
                    "12_113", "12_97", "12_99", Side.DARK,
                    Agenda.BLOCKADE),
            new Family("Plead My Case To The Senate", "12_88",
                    "12_88_BACK", "12_75", "12_74",
                    "12_34", "12_11", "12_1", Side.LIGHT,
                    Agenda.PEACE));

    @Test
    public void senatorDeployActionBeatsGenericCharacterAndPass() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, 0);
                Decision decision = Decision.deployChoice(
                        List.of("deploy-senator", "deploy-generic", "pass"),
                        List.of(
                                "Deploy <div class='cardHint' value='"
                                        + family.agendaSenatorBlueprint + "'>"
                                        + fixture.handSenator.getTitle()
                                        + "</div>",
                                "Deploy <div class='cardHint' value='"
                                        + family.genericCharacterBlueprint
                                        + "'>"
                                        + fixture.handGeneric.getTitle()
                                        + "</div>",
                                "Pass"),
                        List.of(String.valueOf(HAND_SENATOR_ID),
                                String.valueOf(HAND_GENERIC_ID), ""),
                        List.of(family.agendaSenatorBlueprint,
                                family.genericCharacterBlueprint, ""),
                        List.of(fixture.handSenator.getTitle(),
                                fixture.handGeneric.getTitle(), "Pass"));

                Outcome result = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        "deploy-senator", result.actionId);
                assertContains(result, "V108 MY LORD");
                outcomes.add(result);
            }
            assertParity(outcomes);
        }
    }

    @Test
    public void senatorObjectiveSitingDoesNotOverrideTacticalSoloPenalty() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, 0);
                Decision decision = Decision.cardSelection(
                        "Choose where to deploy "
                                + "<div class='cardHint' value='"
                                + family.agendaSenatorBlueprint + "'>"
                                + fixture.handSenator.getTitle() + "</div>",
                        Phase.DEPLOY,
                        List.of(String.valueOf(SENATE_ID),
                                String.valueOf(OTHER_SITE_ID)),
                        List.of(family.senateBlueprint,
                                family.otherSiteBlueprint),
                        List.of(fixture.senate.getTitle(),
                                fixture.otherSite.getTitle()));

                List<Outcome> candidates = cardSelectionAdapter(
                        bot, fixture, decision);
                Outcome senate = only(
                        candidates, String.valueOf(SENATE_ID));
                Outcome otherSite = only(
                        candidates, String.valueOf(OTHER_SITE_ID));
                Outcome result = combined(bot, fixture, decision);
                assertEquals(label(bot, family) + " " + result
                                + " candidates=" + candidates,
                        String.valueOf(OTHER_SITE_ID), result.actionId);
                assertFalse(label(bot, family) + " " + senate,
                        senate.hardVeto);
                assertFalse(label(bot, family) + " " + otherSite,
                        otherSite.hardVeto);
                assertContains(senate,
                        "OBJECTIVE LOCATION - deploy here helps flip! (+300.0)");
                assertContains(senate,
                        "V88 MY LORD: senator");
                assertContains(otherSite,
                        "wrong site! (-300.0)");
                assertTrue(label(bot, family) + " " + candidates,
                        otherSite.score > senate.score);
                outcomes.add(result);
            }
            assertParity(outcomes);
        }
    }

    @Test
    public void senateProgressDoesNotBypassAContestedWeakSoloHardBlock() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, 0);
                when(fixture.modifiers.getTotalPowerAtLocation(
                        fixture.gameState, fixture.senate,
                        OPPONENT, false, false)).thenReturn(10.0f);
                Decision decision = Decision.cardSelection(
                        "Choose where to deploy "
                                + "<div class='cardHint' value='"
                                + family.agendaSenatorBlueprint + "'>"
                                + fixture.handSenator.getTitle() + "</div>",
                        Phase.DEPLOY,
                        List.of(String.valueOf(SENATE_ID),
                                String.valueOf(OTHER_SITE_ID)),
                        List.of(family.senateBlueprint,
                                family.otherSiteBlueprint),
                        List.of(fixture.senate.getTitle(),
                                fixture.otherSite.getTitle()));

                Outcome senate = only(
                        cardSelectionAdapter(bot, fixture, decision),
                        String.valueOf(SENATE_ID));
                assertTrue(label(bot, family) + " " + senate,
                        senate.hardVeto);
                assertTrue(label(bot, family) + " " + senate,
                        senate.vetoReason != null
                                && senate.vetoReason.contains(
                                    "WEAK SOLO INTO CONTESTED"));

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(OTHER_SITE_ID), winner.actionId);
                outcomes.add(winner);
            }
            assertParity(outcomes);
        }
    }

    @Test
    public void postFlipSenatorHoldIsBoundedAndTacticsStillChoosePass() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, 2);
                PhysicalCard senator = fixture.deployedSenators.get(0);
                Decision decision = Decision.actionChoice(
                        "Choose move action", Phase.MOVE,
                        List.of("move-senator", "pass"),
                        List.of("Move using landspeed", "Pass"),
                        List.of(String.valueOf(senator.getCardId()), ""));

                Outcome move = only(
                        moveAdapter(bot, fixture, decision),
                        "move-senator");
                assertFalse(label(bot, family) + " " + move,
                        move.hardVeto);
                assertContains(move, "FLIP_BACK_BLOCKER_HOLD");
                assertContains(move,
                        "keep the sole blocker preventing immediate flip-back (-300.0)");

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family), "pass", winner.actionId);
                outcomes.add(winner);
            }
            assertParity(outcomes);
        }
    }

    @Test
    public void preFlipSenatorCountHoldIsBoundedAndTacticsStillChoosePass() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, 2);
                PhysicalCard senator = fixture.deployedSenators.get(0);
                Decision decision = Decision.actionChoice(
                        "Choose move action", Phase.MOVE,
                        List.of("move-senator", "pass"),
                        List.of("Move using landspeed", "Pass"),
                        List.of(String.valueOf(senator.getCardId()), ""));

                Outcome move = only(
                        moveAdapter(bot, fixture, decision),
                        "move-senator");
                assertFalse(label(bot, family) + " " + move,
                        move.hardVeto);
                assertContains(move, "COUNTED_FORMATION_HOLD");
                assertContains(move,
                        "prefer retaining the last required actor at the gate (-300) (-300.0)");

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family), "pass", winner.actionId);
                outcomes.add(winner);
            }
            assertParity(outcomes);
        }
    }

    @Test
    public void standaloneForceLossKeepsSenatorAndLosesDisposableCard() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, 0);
                Decision decision = Decision.cardSelection(
                        "Choose Force to lose", Phase.CONTROL,
                        List.of(String.valueOf(HAND_SENATOR_ID),
                                String.valueOf(HAND_EFFECT_ID)),
                        List.of(family.agendaSenatorBlueprint, "test_effect"),
                        List.of(fixture.handSenator.getTitle(),
                                fixture.handEffect.getTitle()));

                Outcome senator = only(
                        cardSelectionAdapter(bot, fixture, decision),
                        String.valueOf(HAND_SENATOR_ID));
                assertContains(senator, "V109 MY LORD");

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(HAND_EFFECT_ID), winner.actionId);
                outcomes.add(winner);
            }
            assertParity(outcomes);
        }
    }

    @Test
    public void battleForfeitKeepsLastTwoSenatorsAndLosesFodder() {
        for (Family family : FAMILIES) {
            List<Outcome> outcomes = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, 2);
                PhysicalCard senator = fixture.deployedSenators.get(0);
                Decision decision = Decision.cardSelection(
                        "Choose a card from battle to forfeit",
                        Phase.BATTLE,
                        List.of(String.valueOf(senator.getCardId()),
                                String.valueOf(BATTLE_FODDER_ID)),
                        List.of(family.plainSenatorBlueprint,
                                "test_fodder"),
                        List.of(senator.getTitle(),
                                fixture.battleFodder.getTitle()));

                Outcome protectedSenator = only(
                        cardSelectionAdapter(bot, fixture, decision),
                        String.valueOf(senator.getCardId()));
                assertContains(protectedSenator,
                        "FLIP_GATE_FORMATION_HOLD");

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(BATTLE_FODDER_ID),
                        winner.actionId);
                outcomes.add(winner);
            }
            assertParity(outcomes);
        }
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return outcome(new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext(fixture, decision)));
        }
        return outcome(new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator()
                .evaluateDecision(chosenContext(fixture, decision)));
    }

    private static List<Outcome> moveAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .MoveEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(SenateObjectiveCombinedEvaluatorDecisionTest
                            ::outcome).toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .MoveEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(SenateObjectiveCombinedEvaluatorDecisionTest
                        ::outcome).toList();
    }

    private static List<Outcome> cardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(SenateObjectiveCombinedEvaluatorDecisionTest
                            ::outcome).toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(SenateObjectiveCombinedEvaluatorDecisionTest
                        ::outcome).toList();
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext(
                        fixture.gameState, PLAYER, decision.type,
                        decision.text, "senate-production-decision",
                        decision.phase);
        context.setGame(fixture.game);
        context.setSide(fixture.family.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DecisionContext(
                        fixture.gameState, PLAYER, decision.type,
                        decision.text, "senate-production-decision",
                        decision.phase);
        context.setGame(fixture.game);
        context.setSide(fixture.family.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds);
        context.setActionTexts(decision.actionTexts);
        context.setCardIds(decision.cardIds);
        context.setBlueprints(decision.blueprints);
        context.setTestingTexts(decision.testingTexts);
        context.setSelectable(decision.cardIds.stream()
                .map(ignored -> true).toList());
        context.setNoPass(decision.noPass);
        context.setMin(decision.min);
        context.setMax(1);
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds);
        context.setActionTexts(decision.actionTexts);
        context.setCardIds(decision.cardIds);
        context.setBlueprints(decision.blueprints);
        context.setTestingTexts(decision.testingTexts);
        context.setSelectable(decision.cardIds.stream()
                .map(ignored -> true).toList());
        context.setNoPass(decision.noPass);
        context.setMin(decision.min);
        context.setMax(1);
    }

    private static Fixture fixture(
            Bot bot, Family family, boolean flipped,
            int deployedSenatorCount) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = objective(family, flipped);
        PhysicalCard senate = location(
                family.senateBlueprint, SENATE_ID);
        PhysicalCard otherSite = location(
                family.otherSiteBlueprint, OTHER_SITE_ID);
        PhysicalCard handSenator = card(
                family.agendaSenatorBlueprint,
                HAND_SENATOR_ID, Zone.HAND);
        PhysicalCard handEffect = simpleCard(
                "Disposable Effect", HAND_EFFECT_ID,
                Zone.HAND, CardCategory.EFFECT);
        PhysicalCard handGeneric = card(
                family.genericCharacterBlueprint,
                HAND_GENERIC_ID, Zone.HAND);
        PhysicalCard handThree = simpleCard(
                "Hand Three", 302, Zone.HAND,
                CardCategory.EFFECT);
        PhysicalCard handFour = simpleCard(
                "Hand Four", 303, Zone.HAND,
                CardCategory.EFFECT);
        PhysicalCard handFive = simpleCard(
                "Hand Five", 304, Zone.HAND,
                CardCategory.EFFECT);
        PhysicalCard battleFodder = simpleCard(
                "Battle Fodder", BATTLE_FODDER_ID,
                Zone.AT_LOCATION, CardCategory.CHARACTER);
        when(battleFodder.getBlueprint().hasForfeitAttribute())
                .thenReturn(true);
        when(battleFodder.getBlueprint().getForfeit())
                .thenReturn(2.0f);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(3);
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(10);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(senate, otherSite));
        when(gameState.getTopLocations()).thenReturn(
                List.of(senate, otherSite));
        when(gameState.getHand(PLAYER)).thenReturn(List.of(
                handSenator, handGeneric, handEffect,
                handThree, handFour, handFive));
        when(gameState.getCardPile(
                PLAYER, Zone.FORCE_PILE, false))
                .thenReturn(List.of(handEffect, handEffect, handEffect,
                        handEffect, handEffect, handEffect));
        when(modifiers.getForceAvailableToUse(gameState, PLAYER))
                .thenReturn(10);
        BattleState battleState = new BattleState();
        battleState.reachedDamageSegment();
        battleState.setBaseBattleDamage(PLAYER, 5);
        when(gameState.getBattleState()).thenReturn(battleState);
        when(modifiers.getTotalBattleDamage(gameState, PLAYER))
                .thenReturn(5.0f);

        List<PhysicalCard> deployedSenators = new ArrayList<>();
        Map<Integer, PhysicalCard> byId = new LinkedHashMap<>();
        byId.put(SENATE_ID, senate);
        byId.put(OTHER_SITE_ID, otherSite);
        byId.put(HAND_SENATOR_ID, handSenator);
        byId.put(HAND_EFFECT_ID, handEffect);
        byId.put(HAND_GENERIC_ID, handGeneric);
        byId.put(BATTLE_FODDER_ID, battleFodder);
        for (int index = 0; index < deployedSenatorCount; index++) {
            PhysicalCard senator = card(
                    family.plainSenatorBlueprint,
                    200 + index, Zone.AT_LOCATION);
            deployedSenators.add(senator);
            byId.put(senator.getCardId(), senator);
            when(senator.getAtLocation()).thenReturn(senate);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, senator)).thenReturn(senate);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, senator)).thenReturn(senate);
            when(modifiers.hasKeyword(
                    gameState, senator, Keyword.SENATOR))
                    .thenReturn(true);
            setActive(gameState, senator);
        }
        when(modifiers.hasKeyword(
                gameState, handSenator, Keyword.SENATOR))
                .thenReturn(true);
        when(modifiers.hasAgenda(
                gameState, handSenator, family.agenda))
                .thenReturn(true);
        when(battleFodder.getAtLocation()).thenReturn(otherSite);
        when(modifiers.getLocationThatCardIsAt(
                gameState, battleFodder)).thenReturn(otherSite);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, battleFodder)).thenReturn(otherSite);
        setActive(gameState, battleFodder);

        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        permanents.add(senate);
        permanents.add(otherSite);
        permanents.addAll(deployedSenators);
        permanents.add(battleFodder);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getCardsAtLocation(senate))
                .thenReturn(new ArrayList<>(deployedSenators));
        when(gameState.getCardsAtLocation(otherSite))
                .thenReturn(List.of(battleFodder));
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> byId.get(
                        invocation.getArgument(0, Integer.class)));
        when(gameState.findCardByPermanentId(anyInt())).thenAnswer(
                invocation -> byId.get(
                        invocation.getArgument(0, Integer.class)));
        when(modifiers.getTotalPowerAtLocation(
                gameState, senate, PLAYER, false, false))
                .thenReturn(6.0f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, senate, OPPONENT, false, false))
                .thenReturn(0.0f);

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, family.side);
        return new Fixture(
                family, analyzer, game, gameState,
                modifiers,
                senate, otherSite, deployedSenators,
                handSenator, handGeneric, handEffect,
                battleFodder);
    }

    private static PhysicalCard objective(
            Family family, boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(family.frontBlueprint);
        SwccgCardBlueprint back = blueprint(family.backBlueprint);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(front);
        when(card.getOtherSideBlueprint()).thenReturn(back);
        when(card.getBlueprintId(true))
                .thenReturn(family.frontBlueprint);
        when(card.getBlueprintId(false))
                .thenReturn(family.frontBlueprint);
        when(card.getTitle()).thenReturn(front.getTitle());
        when(card.getTitles()).thenReturn(List.of(front.getTitle()));
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard location(
            String blueprintId, int cardId) {
        PhysicalCard card = card(blueprintId, cardId, Zone.LOCATIONS);
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static PhysicalCard card(
            String blueprintId, int cardId, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isUndercover()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
        return card;
    }

    private static PhysicalCard simpleCard(
            String title, int cardId, Zone zone,
            CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
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
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome only(
            List<Outcome> actions, String actionId) {
        return actions.stream()
                .filter(action -> actionId.equals(action.actionId))
                .findFirst().orElseThrow();
    }

    private static void assertContains(
            Outcome outcome, String marker) {
        assertTrue(
                "Expected '" + marker + "' in " + outcome.reasoning,
                outcome.reasoning.stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertParity(List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        Outcome rando = outcomes.get(0);
        Outcome chosen = outcomes.get(1);
        assertEquals(rando.actionId, chosen.actionId);
        assertEquals(Float.floatToRawIntBits(rando.score),
                Float.floatToRawIntBits(chosen.score));
        assertEquals(rando.reasoning, chosen.reasoning);
        assertEquals(rando.hardVeto, chosen.hardVeto);
        assertEquals(rando.vetoReason, chosen.vetoReason);
    }

    private static String label(Bot bot, Family family) {
        return bot + " " + family.frontTitle;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Family(
            String frontTitle,
            String frontBlueprint,
            String backBlueprint,
            String senateBlueprint,
            String otherSiteBlueprint,
            String plainSenatorBlueprint,
            String agendaSenatorBlueprint,
            String genericCharacterBlueprint,
            Side side,
            Agenda agenda) {
    }

    private record Fixture(
            Family family,
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard senate,
            PhysicalCard otherSite,
            List<PhysicalCard> deployedSenators,
            PhysicalCard handSenator,
            PhysicalCard handGeneric,
            PhysicalCard handEffect,
            PhysicalCard battleFodder) {
    }

    private record Outcome(
            String actionId,
            float score,
            List<String> reasoning,
            boolean hardVeto,
            String vetoReason) {
    }

    private record Decision(
            String type,
            String text,
            Phase phase,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            boolean noPass,
            int min) {

        private static Decision cardSelection(
                String text, Phase phase, List<String> cardIds,
                List<String> blueprints, List<String> testingTexts) {
            return new Decision(
                    "CARD_SELECTION", text, phase,
                    List.of(), List.of(), cardIds,
                    blueprints, testingTexts, true, 1);
        }

        private static Decision actionChoice(
                String text, Phase phase, List<String> actionIds,
                List<String> actionTexts, List<String> cardIds) {
            return new Decision(
                    "ACTION_CHOICE", text, phase,
                    actionIds, actionTexts, cardIds,
                    List.of(), List.of(), false, 0);
        }

        private static Decision deployChoice(
                List<String> actionIds,
                List<String> actionTexts,
                List<String> cardIds,
                List<String> blueprints,
                List<String> testingTexts) {
            return new Decision(
                    "CARD_ACTION_CHOICE", "Choose deploy action",
                    Phase.DEPLOY, actionIds, actionTexts,
                    cardIds, blueprints, testingTexts, false, 0);
        }
    }
}
