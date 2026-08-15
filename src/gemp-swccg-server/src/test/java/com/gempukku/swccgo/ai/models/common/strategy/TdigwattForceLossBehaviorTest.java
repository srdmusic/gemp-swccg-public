package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.set5.dark.Card5_114;
import com.gempukku.swccgo.cards.set5.dark.Card5_115;
import com.gempukku.swccgo.cards.set109.dark.Card109_012;
import com.gempukku.swccgo.cards.set109.dark.Card109_012_BACK;
import com.gempukku.swccgo.cards.set226.dark.Card226_012;
import com.gempukku.swccgo.cards.set226.dark.Card226_012_BACK;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Source-backed behavior proof for the existing V21 Force-loss protection.
 *
 * <p>Classic 109_12 requires Dark Deal for its flip, while virtual 226_12
 * merely uploads it. These tests keep that distinction intact and deliberately
 * do not invent a TDIGWATT-specific board projection for losing off-table
 * Force.</p>
 */
public class TdigwattForceLossBehaviorTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";
    private static final int OBJECTIVE_ID = 90;
    private static final int DARK_DEAL_ID = 101;
    private static final int ORDINARY_EFFECT_ID = 102;
    private static final String DARK_DEAL_BP = "5_115";
    private static final String ORDINARY_EFFECT_BP = "5_114";
    private static final String STANDALONE_PROMPT =
            "Choose Force to lose";
    private static final String COMBINED_PROMPT =
            "Choose Force to lose or choose a card from battle to forfeit";

    @Test
    public void standaloneProtectsClassicReserveDarkDealWithoutInventingVirtualProjection() {
        Fixture classic = fixture(Printing.CLASSIC, false);
        Evaluation classicEvaluation =
                evaluateBoth(classic, STANDALONE_PROMPT);

        assertTrue(classic.randoAnalyzer
                .isRequiredCardForFlip(classic.darkDeal));
        assertTrue(classic.chosenAnalyzer
                .isRequiredCardForFlip(classic.darkDeal));
        assertContains(classicEvaluation.darkDeal(),
                "OBJECTIVE CRITICAL: prefer to retain");
        assertNotContains(classicEvaluation.darkDeal(),
                "OBJECTIVE PULLABLE");
        assertNoInventedTdigwattProjection(
                classicEvaluation.darkDeal());
        assertTrue(
                "The ordinary legal loss must outrank classic Dark Deal",
                classicEvaluation.ordinaryEffect().score()
                        > classicEvaluation.darkDeal().score());
        assertEquals(String.valueOf(ORDINARY_EFFECT_ID),
                classicEvaluation.winner().actionId());

        Fixture virtual = fixture(Printing.VIRTUAL, false);
        Evaluation virtualEvaluation =
                evaluateBoth(virtual, STANDALONE_PROMPT);

        assertFalse(virtual.randoAnalyzer
                .isRequiredCardForFlip(virtual.darkDeal));
        assertFalse(virtual.chosenAnalyzer
                .isRequiredCardForFlip(virtual.darkDeal));
        assertTrue(virtual.randoAnalyzer
                .isPullableCard(virtual.darkDeal));
        assertTrue(virtual.chosenAnalyzer
                .isPullableCard(virtual.darkDeal));
        assertNotContains(virtualEvaluation.darkDeal(), "OBJECTIVE");
        assertNoInventedTdigwattProjection(
                virtualEvaluation.darkDeal());
        assertEquals(
                "Standalone V21 only protects pullables already in hand; "
                        + "a Reserve Dark Deal is neutral for virtual 226_12",
                Float.floatToRawIntBits(
                        virtualEvaluation.ordinaryEffect().score()),
                Float.floatToRawIntBits(
                        virtualEvaluation.darkDeal().score()));
    }

    @Test
    public void combinedBattleKeepsClassicRequiredAndVirtualPullableReasonsDistinct() {
        Fixture classic = fixture(Printing.CLASSIC, true);
        Evaluation classicEvaluation =
                evaluateBoth(classic, COMBINED_PROMPT);

        assertContains(classicEvaluation.darkDeal(),
                "OBJECTIVE CRITICAL: prefer to retain");
        assertNotContains(classicEvaluation.darkDeal(),
                "OBJECTIVE PULLABLE");
        assertNoInventedTdigwattProjection(
                classicEvaluation.darkDeal());
        assertTrue(
                "The ordinary battle-damage loss must outrank classic Dark Deal",
                classicEvaluation.ordinaryEffect().score()
                        > classicEvaluation.darkDeal().score());
        assertEquals(String.valueOf(ORDINARY_EFFECT_ID),
                classicEvaluation.winner().actionId());

        Fixture virtual = fixture(Printing.VIRTUAL, true);
        Evaluation virtualEvaluation =
                evaluateBoth(virtual, COMBINED_PROMPT);

        assertContains(virtualEvaluation.darkDeal(),
                "OBJECTIVE PULLABLE: prefer to retain");
        assertNotContains(virtualEvaluation.darkDeal(),
                "OBJECTIVE CRITICAL");
        assertNoInventedTdigwattProjection(
                virtualEvaluation.darkDeal());
        assertTrue(
                "The ordinary battle-damage loss must outrank virtual Dark Deal",
                virtualEvaluation.ordinaryEffect().score()
                        > virtualEvaluation.darkDeal().score());
        assertEquals(String.valueOf(ORDINARY_EFFECT_ID),
                virtualEvaluation.winner().actionId());
    }

    private static Evaluation evaluateBoth(
            Fixture fixture,
            String prompt) {
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CardSelectionEvaluator()
                .evaluate(randoContext(fixture, prompt));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator()
                .evaluate(chosenContext(fixture, prompt));

        Map<String, ActionView> randoViews =
                rando.stream().collect(
                        java.util.stream.Collectors.toMap(
                                com.gempukku.swccgo.ai.models.rando
                                        .evaluators.EvaluatedAction
                                        ::getActionId,
                                action -> new ActionView(
                                        action.getActionId(),
                                        action.getScore(),
                                        action.isHardVetoed(),
                                        action.getVetoReason(),
                                        List.copyOf(
                                                action.getReasoning())),
                                (left, right) -> left,
                                LinkedHashMap::new));
        Map<String, ActionView> chosenViews =
                chosen.stream().collect(
                        java.util.stream.Collectors.toMap(
                                com.gempukku.swccgo.ai.models.chosenone
                                        .evaluators.EvaluatedAction
                                        ::getActionId,
                                action -> new ActionView(
                                        action.getActionId(),
                                        action.getScore(),
                                        action.isHardVetoed(),
                                        action.getVetoReason(),
                                        List.copyOf(
                                                action.getReasoning())),
                                (left, right) -> left,
                                LinkedHashMap::new));
        assertEquals(
                "Rando and Chosen One must expose the same V21 decision",
                randoViews, chosenViews);

        ActionView darkDeal =
                randoViews.get(String.valueOf(DARK_DEAL_ID));
        ActionView ordinary =
                randoViews.get(String.valueOf(ORDINARY_EFFECT_ID));
        ActionView winner = randoViews.values().stream()
                .max(Comparator.comparingDouble(ActionView::score))
                .orElseThrow();
        return new Evaluation(darkDeal, ordinary, winner);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture,
                    String prompt) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                fixture.gameState,
                                PLAYER,
                                "CARD_SELECTION",
                                prompt,
                                "tdigwatt-force-loss",
                                Phase.BATTLE);
        populate(context, fixture, fixture.randoAnalyzer);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture,
                    String prompt) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                fixture.gameState,
                                PLAYER,
                                "CARD_SELECTION",
                                prompt,
                                "tdigwatt-force-loss",
                                Phase.BATTLE);
        populate(context, fixture, fixture.chosenAnalyzer);
        return context;
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            Fixture fixture,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer analyzer) {
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(analyzer);
        context.setCardIds(List.of(
                String.valueOf(DARK_DEAL_ID),
                String.valueOf(ORDINARY_EFFECT_ID)));
        context.setBlueprints(List.of(
                DARK_DEAL_BP, ORDINARY_EFFECT_BP));
        context.setTestingTexts(List.of(
                fixture.darkDeal.getTitle(),
                fixture.ordinaryEffect.getTitle()));
        context.setSelectable(List.of(true, true));
        context.setMin(1);
        context.setMax(1);
        context.setNoPass(true);
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            Fixture fixture,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer analyzer) {
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(analyzer);
        context.setCardIds(List.of(
                String.valueOf(DARK_DEAL_ID),
                String.valueOf(ORDINARY_EFFECT_ID)));
        context.setBlueprints(List.of(
                DARK_DEAL_BP, ORDINARY_EFFECT_BP));
        context.setTestingTexts(List.of(
                fixture.darkDeal.getTitle(),
                fixture.ordinaryEffect.getTitle()));
        context.setSelectable(List.of(true, true));
        context.setMin(1);
        context.setMax(1);
        context.setNoPass(true);
    }

    private static Fixture fixture(
            Printing printing,
            boolean battleDamage) {
        SwccgCardBlueprint front =
                printing == Printing.CLASSIC
                        ? new Card109_012()
                        : new Card226_012();
        SwccgCardBlueprint back =
                printing == Printing.CLASSIC
                        ? new Card109_012_BACK()
                        : new Card226_012_BACK();
        String objectiveBlueprint =
                printing == Printing.CLASSIC
                        ? "109_12" : "226_12";

        PhysicalCard objective = card(
                OBJECTIVE_ID, objectiveBlueprint,
                front, back, Zone.SIDE_OF_TABLE);
        PhysicalCard darkDeal = card(
                DARK_DEAL_ID, DARK_DEAL_BP,
                new Card5_115(), null,
                Zone.RESERVE_DECK);
        PhysicalCard ordinaryEffect = card(
                ORDINARY_EFFECT_ID, ORDINARY_EFFECT_BP,
                new Card5_114(), null,
                Zone.RESERVE_DECK);

        Map<Integer, PhysicalCard> byId =
                Map.of(
                        OBJECTIVE_ID, objective,
                        DARK_DEAL_ID, darkDeal,
                        ORDINARY_EFFECT_ID, ordinaryEffect);
        GameState gameState = mock(GameState.class);
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> byId.get(
                        invocation.getArgument(
                                0, Integer.class)));
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(objective));
        when(gameState.getHand(PLAYER))
                .thenReturn(List.of());
        when(gameState.getReserveDeck(PLAYER))
                .thenReturn(List.of(
                        darkDeal, ordinaryEffect));
        when(gameState.getReserveDeckSize(PLAYER))
                .thenReturn(12);
        when(gameState.getForcePile(PLAYER))
                .thenReturn(List.of());
        when(gameState.getForcePileSize(PLAYER))
                .thenReturn(0);
        when(gameState.getUsedPile(PLAYER))
                .thenReturn(List.of());
        when(gameState.getUnresolvedDestinyDraw(PLAYER))
                .thenReturn(List.of());
        when(gameState.getSabaccHand(PLAYER))
                .thenReturn(List.of());
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(4);
        when(gameState.getCurrentPlayerId())
                .thenReturn(PLAYER);
        when(gameState.getOpponent(PLAYER))
                .thenReturn(OPPONENT);

        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);

        if (battleDamage) {
            BattleState battleState = new BattleState();
            battleState.reachedDamageSegment();
            battleState.setBaseBattleDamage(PLAYER, 6);
            when(gameState.getBattleState())
                    .thenReturn(battleState);
            when(modifiers.getTotalBattleDamage(
                    gameState, PLAYER)).thenReturn(6.0f);
        }

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        randoAnalyzer.analyze(game, PLAYER, Side.DARK);
        chosenAnalyzer.analyze(game, PLAYER, Side.DARK);

        return new Fixture(
                gameState, game, darkDeal,
                ordinaryEffect, randoAnalyzer,
                chosenAnalyzer);
    }

    private static PhysicalCard card(
            int cardId,
            String blueprintId,
            SwccgCardBlueprint blueprint,
            SwccgCardBlueprint otherSide,
            Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getBlueprintId(anyBoolean()))
                .thenReturn(blueprintId);
        when(card.getBlueprintId(
                nullable(GameState.class), anyBoolean()))
                .thenReturn(blueprintId);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getOtherSideBlueprint())
                .thenReturn(otherSide);
        when(card.getTitle()).thenReturn(
                blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.isFlipped()).thenReturn(false);
        return card;
    }

    private static void assertContains(
            ActionView action,
            String fragment) {
        assertTrue(
                action.reasoning().toString(),
                action.reasoning().stream()
                        .anyMatch(reason ->
                                reason.contains(fragment)));
    }

    private static void assertNotContains(
            ActionView action,
            String fragment) {
        assertFalse(
                action.reasoning().toString(),
                action.reasoning().stream()
                        .anyMatch(reason ->
                                reason.contains(fragment)));
    }

    private static void assertNoInventedTdigwattProjection(
            ActionView action) {
        assertNotContains(action,
                "Preserve positive exact objective margin");
        assertNotContains(action,
                "TDIGWATT.109_12.FORCE_LOSS");
        assertNotContains(action,
                "TDIGWATT.226_12.FORCE_LOSS");
    }

    private enum Printing {
        CLASSIC,
        VIRTUAL
    }

    private record Fixture(
            GameState gameState,
            SwccgGame game,
            PhysicalCard darkDeal,
            PhysicalCard ordinaryEffect,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer) {
    }

    private record ActionView(
            String actionId,
            float score,
            boolean hardVetoed,
            String vetoReason,
            List<String> reasoning) {
    }

    private record Evaluation(
            ActionView darkDeal,
            ActionView ordinaryEffect,
            ActionView winner) {
    }
}
