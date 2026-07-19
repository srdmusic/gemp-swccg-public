package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PullActionTextCharacterizationTest {
    private static final String PLAYER = "tester";
    private static final String ACTION_TEXT = "Take card into hand from Reserve Deck";
    private static final String V192_SUFFIX =
            "[absorbs V60-pull/V82/V95/V97/V100/V116/V67l/V67ai/V67am/V29.7]";

    @Test
    public void v192DeployGradeBaseIsExactAndMirrored() {
        Fixture fixture = fixture(10, Phase.DEPLOY, null);

        var rando = evaluateRando(fixture, null);
        var chosen = evaluateChosen(fixture, null);

        assertMirrored(rando, chosen);
        assertBits(150.0f, rando.getScore());
        assertEquals(List.of(
                "V192 PULL SCORER (DEPLOY-GRADE): base 150 + tier 0 [none] + ctx 0 = 150 "
                        + V192_SUFFIX + " (+150.0)"), rando.getReasoning());
    }

    @Test
    public void v192ActivateGradeBaseIsExactAndMirrored() {
        Fixture fixture = fixture(10, Phase.ACTIVATE, null);

        var rando = evaluateRando(fixture, null);
        var chosen = evaluateChosen(fixture, null);

        assertMirrored(rando, chosen);
        assertBits(5500.0f, rando.getScore());
        assertEquals(List.of(
                "V192 PULL SCORER (ACTIVATE): base 5500 + tier 0 [none] + ctx 0 = 5500 "
                        + V192_SUFFIX + " (+5500.0)"), rando.getReasoning());
    }

    @Test
    public void v61cReserveThreeBattlePlausibleStandsDownToDeployGrade() {
        Fixture fixture = fixture(3, Phase.ACTIVATE, null);

        var rando = evaluateRando(fixture, null);
        var chosen = evaluateChosen(fixture, null);

        assertMirrored(rando, chosen);
        assertBits(150.0f, rando.getScore());
        assertTrue(rando.getReasoning().get(0).startsWith(
                "V192 PULL SCORER (DEPLOY-GRADE): base 150"));
    }

    @Test
    public void v61cReserveThreeWithoutContestKeepsActivateGrade() {
        SwccgGame game = mock(SwccgGame.class);
        Fixture fixture = fixture(3, Phase.ACTIVATE, game);

        var rando = evaluateRando(fixture, null);
        var chosen = evaluateChosen(fixture, null);

        assertMirrored(rando, chosen);
        assertBits(5500.0f, rando.getScore());
    }

    @Test
    public void v67akRemainsAnExternalContributionBeforeTheV192Emit() {
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        when(fixture.blueprint.getGameText()).thenReturn("May [download] Luke Skywalker.");

        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer randoObjective =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.getStrategyCharacterTokens(isNull(), eq(PLAYER)))
                .thenReturn(Set.of("luke"));

        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer chosenObjective =
                mock(com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.getStrategyCharacterTokens(isNull(), eq(PLAYER)))
                .thenReturn(Set.of("luke"));

        var rando = evaluateRando(fixture, randoObjective);
        var chosen = evaluateChosen(fixture, chosenObjective);

        assertMirrored(rando, chosen);
        assertBits(950.0f, rando.getScore());
        assertEquals(2, rando.getReasoning().size());
        assertTrue(rando.getReasoning().get(0).startsWith("V67ak KEY-CHARACTER PULL:"));
        assertTrue(rando.getReasoning().get(1).startsWith("V192 PULL SCORER (DEPLOY-GRADE):"));
    }

    @Test
    public void v177DeadSearchExitsBeforeV192Positives() {
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        when(fixture.blueprint.getGameText()).thenReturn("May [download] Force Projection.");

        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        when(oracle.isAnalyzed()).thenReturn(true);
        when(oracle.hasTargetInZone(eq(Zone.RESERVE_DECK), any(String[].class)))
                .thenReturn(false);
        when(oracle.validatePullFromSourceCard(eq(Zone.RESERVE_DECK), anyString()))
                .thenReturn(new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullValidation(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_FAIL,
                        "no target"));

        var action = evaluateRando(fixture, oracle, null);

        assertBits(-2000.0f, action.getScore());
        assertEquals(1, action.getReasoning().size());
        assertTrue(action.getReasoning().get(0).startsWith("V177 DEAD SEARCH:"));
        assertTrue(action.getReasoning().get(0).endsWith("(-2000.0)"));
    }

    @Test
    public void v183TitleZoneDeadSearchExitsBeforeV192Positives() {
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        when(fixture.blueprint.getGameText()).thenReturn("Weather Vane remains a named option.");
        when(fixture.source.getBlueprintId(true)).thenReturn("source-blueprint");

        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard named =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard.class);
        when(named.getTitle()).thenReturn("Weather Vane");
        when(named.getCurrentZone()).thenReturn(Zone.HAND);

        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        when(oracle.isAnalyzed()).thenReturn(true);
        when(oracle.namedDeckCardsInText(anyString(), eq("source-blueprint")))
                .thenReturn(List.of(named));

        var action = evaluateRando(fixture, oracle, null);

        assertBits(-2000.0f, action.getScore());
        assertEquals(1, action.getReasoning().size());
        assertTrue(action.getReasoning().get(0).startsWith("V183 DEAD SEARCH (title+zone):"));
        assertTrue(action.getReasoning().get(0).endsWith("(-2000.0)"));
    }

    @Test
    public void deployAndActionTextV185GuardsRemainAdditiveAndUnequal() {
        SwccgGame game = mock(SwccgGame.class);
        Fixture fixture = fixture(10, Phase.DEPLOY, game);
        when(fixture.blueprint.getGameText()).thenReturn("May [download] Leia's Lightsaber.");

        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard reserveCard =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard.class);
        when(oracle.isAnalyzed()).thenReturn(true);
        when(oracle.getCardsInZone(Zone.RESERVE_DECK))
                .thenReturn(Collections.nCopies(10, reserveCard));
        when(oracle.hasTargetInZone(eq(Zone.RESERVE_DECK), any(String[].class)))
                .thenReturn(true);
        when(oracle.hasTargetInReserve(any(String[].class))).thenReturn(true);
        when(oracle.validatePullFromSourceCard(eq(Zone.RESERVE_DECK), anyString()))
                .thenReturn(new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullValidation(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_SUCCEED,
                        "target remains"));
        when(oracle.reserveTargetsAreAllUnattachableWeapons(
                same(game), eq(PLAYER), anyList())).thenReturn(true);
        when(oracle.hasFilterMatchInReserve(same(game), eq(PLAYER), any()))
                .thenReturn(true);

        var context = randoContext(fixture);
        context.setDeckOracle(oracle);
        context.setActionTexts(List.of("Deploy from Reserve Deck"));
        var deploy = new com.gempukku.swccgo.ai.models.rando.evaluators.DeployEvaluator()
                .evaluate(context).get(0);
        var actionText = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(context).get(0);

        assertBits(-1950.0f, deploy.getScore());
        assertBits(-9999.0f, actionText.getScore());
        assertBits(-11949.0f, deploy.getScore() + actionText.getScore());
        assertEquals(1, deploy.getReasoning().size());
        assertEquals(1, actionText.getReasoning().size());
        assertTrue(deploy.getReasoning().get(0).startsWith("V185 WEAPON, NO LEGAL HOLDER:"));
        assertTrue(actionText.getReasoning().get(0).startsWith("V185 (ATE mirror):"));
    }

    @Test
    public void reserveRiskGuardStillDuplicatesAcrossDeployAndActionText() {
        Fixture fixture = fixture(2, Phase.DEPLOY, null);
        var context = randoContext(fixture);
        context.setActionTexts(List.of("Deploy a weapon from Reserve Deck"));

        var deploy = new com.gempukku.swccgo.ai.models.rando.evaluators.DeployEvaluator()
                .evaluate(context).get(0);
        var actionText = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(context).get(0);

        assertBits(-9949.0f, deploy.getScore());
        assertBits(-9999.0f, actionText.getScore());
        assertBits(-19948.0f, deploy.getScore() + actionText.getScore());
        assertTrue(deploy.getReasoning().get(0).startsWith("V60 RESERVE RISK:"));
        assertTrue(actionText.getReasoning().get(0).startsWith("V60 RESERVE RISK:"));
    }

    @Test
    public void deployWastefulMemoryFallsThroughBeforeLaterTerminalGuard() {
        PullDeployFacts facts = new PullDeployFacts(
                "pull", ACTION_TEXT, false, 10, "", "", "",
                new PullOracleView.Validation(
                        PullOracleView.Outcome.WASTEFUL, "already satisfied"),
                new PullOracleView.Validation(
                        PullOracleView.Outcome.WILL_SUCCEED, "starship remains"),
                "Safe Pull Source", false, true);

        PullDeployPolicy.Evaluation evaluation = PullDeployPolicy.evaluate(facts);

        assertEquals(PullDeployPolicy.AdapterStep.CONTINUE_ACTION,
                evaluation.adapterStep());
        assertEquals(2, evaluation.result().operations().size());
        assertBits(-800.0f, evaluation.result().operations().get(0).delta());
        assertBits(-12000.0f, evaluation.result().operations().get(1).delta());
        assertEquals("V66-wasteful",
                evaluation.result().operations().get(0).ruleArmId().id());
        assertEquals("V190",
                evaluation.result().operations().get(1).ruleArmId().id());
    }

    @Test
    public void absentDeckOracleDoesNotBecomeAFalseNamedTargetMiss() {
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        var context = randoContext(fixture);
        context.setActionTexts(List.of("Deploy Luke Skywalker from Reserve Deck"));

        var deploy = new com.gempukku.swccgo.ai.models.rando.evaluators.DeployEvaluator()
                .evaluate(context);
        var actionText = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(context);

        assertFalse(deploy.isEmpty());
        assertFalse(actionText.isEmpty());
        assertFalse(deploy.get(0).getReasoning().stream()
                .anyMatch(reason -> reason.startsWith("V60 RESERVE MISS:")));
        assertFalse(actionText.get(0).getReasoning().stream()
                .anyMatch(reason -> reason.startsWith("V60 RESERVE MISS:")));
    }

    @Test
    public void unanalyzedDeckOracleDoesNotRunSourceValidation() {
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        when(oracle.isAnalyzed()).thenReturn(false);
        var context = randoContext(fixture);
        context.setDeckOracle(oracle);
        context.setActionTexts(List.of("Deploy from Reserve Deck"));

        new com.gempukku.swccgo.ai.models.rando.evaluators.DeployEvaluator()
                .evaluate(context);
        new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(context);

        verify(oracle, never()).validatePullFromSourceCard(any(), anyString());
    }

    @Test
    public void missingGameStateRemainsUnknownInsteadOfEmptyReserve() {
        PullActionFacts.Parent facts = PullActionFactsReader.readParent(
                "pull", ACTION_TEXT, "101",
                new PullActionFactsReader.Context(
                        null, null, PLAYER, null, Phase.DEPLOY,
                        null, null, null));

        assertEquals(-1, facts.reserveSize());
        PullActionPolicy.Evaluation evaluation = PullActionPolicy.evaluateParent(facts);
        assertFalse(evaluation.result().operations().stream()
                .anyMatch(operation -> operation.ruleArmId().id()
                        .equals("V60-reserve-risk")));
    }

    @Test
    public void reserveRiskShortCircuitsAllLaterFactReads() {
        GameState gameState = mock(GameState.class);
        PullOracleView oracle = mock(PullOracleView.class);
        PullActionFactsReader.LateView lateView =
                mock(PullActionFactsReader.LateView.class);
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(2);
        when(gameState.findCardById(anyInt()))
                .thenThrow(new AssertionError("source must not be read"));

        PullActionFacts.Parent parent = PullActionFactsReader.readParent(
                "pull", ACTION_TEXT, "101",
                new PullActionFactsReader.Context(
                        null, gameState, PLAYER, null, Phase.DEPLOY,
                        oracle, null, lateView));
        PullDeployFacts deploy = PullDeployFactsReader.read(
                "pull", "Deploy from Reserve Deck", "101",
                new PullDeployFactsReader.Context(
                        null, gameState, PLAYER, null, oracle));

        assertEquals(2, parent.reserveSize());
        assertEquals(2, deploy.reserveSize());
        verify(gameState, never()).findCardById(anyInt());
        verifyNoInteractions(oracle, lateView);
    }

    @Test
    public void emptyLostPileSearchStopsBeforeLaterActionScoringAndMirrors() {
        String actionText = "Take a character into hand from Lost Pile";
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        when(fixture.gameState.getLostPile(PLAYER)).thenReturn(Collections.emptyList());

        var rando = evaluateRando(fixture, actionText, null, null);
        var chosen = evaluateChosen(fixture, actionText, null, null);

        assertMirrored(rando, chosen);
        assertBits(-300.0f, rando.getScore());
        assertEquals(List.of(
                "V23 EMPTY PILE: Lost Pile is empty — search will fail! (-300.0)"),
                rando.getReasoning());
    }

    @Test
    public void oneCardLostPileFallsThroughToMatchingTakeAndMirrors() {
        String actionText = "Take a character into hand from Lost Pile";
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        PhysicalCard character = mock(PhysicalCard.class);
        SwccgCardBlueprint characterBlueprint = mock(SwccgCardBlueprint.class);
        when(character.getBlueprint()).thenReturn(characterBlueprint);
        when(characterBlueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(fixture.gameState.getLostPile(PLAYER)).thenReturn(List.of(character));

        var rando = evaluateRando(fixture, actionText, null, null);
        var chosen = evaluateChosen(fixture, actionText, null, null);

        assertMirrored(rando, chosen);
        assertBits(-70.0f, rando.getScore());
        assertEquals(2, rando.getReasoning().size());
        assertTrue(rando.getReasoning().get(0).startsWith("V23 LOW PILE:"));
        assertEquals("Take card into hand from Lost Pile (+30.0)",
                rando.getReasoning().get(1));
    }

    @Test
    public void sorryLocationReserveRiskAndV192StayAdditiveAndMirrored() {
        String actionText = "Deploy an interior Cloud City site from Reserve Deck";
        Fixture fixture = fixture(10, Phase.DEPLOY, null);

        com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle randoOracle =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        when(randoOracle.isAnalyzed()).thenReturn(true);
        when(randoOracle.isCardInReserve(anyString())).thenReturn(true);
        when(randoOracle.getCardsInZone(Zone.RESERVE_DECK))
                .thenReturn(Collections.nCopies(4,
                        mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard.class)));
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer randoObjective =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.needsBespinSystemPresence()).thenReturn(true);
        when(randoObjective.getStrategyCharacterTokens(isNull(), eq(PLAYER)))
                .thenReturn(Collections.emptySet());

        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle chosenOracle =
                mock(com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.class);
        when(chosenOracle.isAnalyzed()).thenReturn(true);
        when(chosenOracle.isCardInReserve(anyString())).thenReturn(true);
        when(chosenOracle.getCardsInZone(Zone.RESERVE_DECK))
                .thenReturn(Collections.nCopies(4,
                        mock(com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard.class)));
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer chosenObjective =
                mock(com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.needsBespinSystemPresence()).thenReturn(true);
        when(chosenObjective.getStrategyCharacterTokens(isNull(), eq(PLAYER)))
                .thenReturn(Collections.emptySet());

        var rando = evaluateRando(
                fixture, actionText, randoOracle, randoObjective);
        var chosen = evaluateChosen(
                fixture, actionText, chosenOracle, chosenObjective);

        assertMirrored(rando, chosen);
        assertBits(1750.0f, rando.getScore());
        assertEquals(3, rando.getReasoning().size());
        assertTrue(rando.getReasoning().get(0).startsWith("V24.6 I'M SORRY:"));
        assertTrue(rando.getReasoning().get(1).startsWith("V37 RESERVE CAUTION:"));
        assertTrue(rando.getReasoning().get(2).startsWith(
                "V192 PULL SCORER (DEPLOY-GRADE):"));
    }

    @Test
    public void woklingSearchKeepsLegacyPullStackAndMirrors() {
        String actionText = "Deploy an Effect with deploy cost from Reserve Deck";
        Fixture fixture = fixture(10, Phase.DEPLOY, null);
        when(fixture.source.getTitle()).thenReturn("Wokling (V)");

        var rando = evaluateRando(fixture, actionText, null, null);
        var chosen = evaluateChosen(fixture, actionText, null, null);

        assertMirrored(rando, chosen);
        assertBits(-9849.0f, rando.getScore());
        assertEquals(2, rando.getReasoning().size());
        assertEquals(
                "V53 BLOCK WOKLING: Don't waste 3 force searching for effects! (-9999.0)",
                rando.getReasoning().get(0));
        assertTrue(rando.getReasoning().get(1).startsWith(
                "V192 PULL SCORER (DEPLOY-GRADE):"));
    }

    @Test
    public void youAreBeatenSearchThenBattleFreezeKeepsExactLegacyOrder() {
        String actionText =
                "I Am Your Father; target a character present who cannot move or battle";
        Fixture fixture = fixture(10, Phase.BATTLE, null);
        when(fixture.source.getTitle()).thenReturn("You Are Beaten");

        var rando = evaluateRando(fixture, actionText, null, null);
        var chosen = evaluateChosen(fixture, actionText, null, null);

        assertMirrored(rando, chosen);
        assertBits(-1500.0f, rando.getScore());
        assertTrue(rando.getReasoning().get(0).startsWith(
                "V144 YOU ARE BEATEN: Mode 2 (IAYF search)"));
        assertTrue(rando.getReasoning().get(1).startsWith(
                "V144 YOU ARE BEATEN: Battle freeze in battle phase"));
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction evaluateRando(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objective) {
        return evaluateRando(fixture, null, objective);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction evaluateRando(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objective) {
        return evaluateRando(fixture, ACTION_TEXT, oracle, objective);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction evaluateRando(
            Fixture fixture,
            String actionText,
            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objective) {
        var context = randoContext(fixture, actionText);
        context.setDeckOracle(oracle);
        context.setObjectiveAnalyzer(objective);
        return new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(context).get(0);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction evaluateChosen(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer objective) {
        return evaluateChosen(fixture, ACTION_TEXT, null, objective);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction evaluateChosen(
            Fixture fixture,
            String actionText,
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle oracle,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer objective) {
        var context = chosenContext(fixture, actionText);
        context.setDeckOracle(oracle);
        context.setObjectiveAnalyzer(objective);
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(context).get(0);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            Fixture fixture) {
        return randoContext(fixture, ACTION_TEXT);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            Fixture fixture,
            String actionText) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                fixture.gameState, PLAYER, "ACTION_CHOICE", "Choose action",
                "pull-characterization", fixture.phase);
        context.setGame(fixture.game);
        context.setActionIds(List.of("pull"));
        context.setActionTexts(List.of(actionText));
        context.setCardIds(List.of("101"));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            Fixture fixture) {
        return chosenContext(fixture, ACTION_TEXT);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            Fixture fixture,
            String actionText) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                fixture.gameState, PLAYER, "ACTION_CHOICE", "Choose action",
                "pull-characterization", fixture.phase);
        context.setGame(fixture.game);
        context.setActionIds(List.of("pull"));
        context.setActionTexts(List.of(actionText));
        context.setCardIds(List.of("101"));
        return context;
    }

    private static Fixture fixture(int reserve, Phase phase, SwccgGame game) {
        GameState gameState = mock(GameState.class);
        PhysicalCard source = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);

        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getOpponent(PLAYER)).thenReturn("opponent");
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(reserve);
        when(gameState.getHand(PLAYER)).thenReturn(Collections.emptyList());
        when(gameState.getAllPermanentCards()).thenReturn(Collections.emptyList());
        when(gameState.getTopLocations()).thenReturn(Collections.emptyList());
        when(gameState.findCardById(101)).thenReturn(source);
        when(source.getTitle()).thenReturn("Safe Pull Source");
        when(source.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.EFFECT);
        return new Fixture(gameState, game, source, blueprint, phase);
    }

    private static void assertMirrored(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando,
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen) {
        assertEquals(rando.getActionId(), chosen.getActionId());
        assertEquals(Float.floatToRawIntBits(rando.getScore()),
                Float.floatToRawIntBits(chosen.getScore()));
        assertEquals(rando.getReasoning(), chosen.getReasoning());
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }

    private static final class Fixture {
        private final GameState gameState;
        private final SwccgGame game;
        private final PhysicalCard source;
        private final SwccgCardBlueprint blueprint;
        private final Phase phase;

        private Fixture(GameState gameState, SwccgGame game, PhysicalCard source,
                        SwccgCardBlueprint blueprint, Phase phase) {
            this.gameState = gameState;
            this.game = game;
            this.source = source;
            this.blueprint = blueprint;
            this.phase = phase;
        }
    }
}
