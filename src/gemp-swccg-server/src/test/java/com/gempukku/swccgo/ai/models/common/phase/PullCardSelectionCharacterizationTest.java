package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR;
import static com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_LOCATION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PullCardSelectionCharacterizationTest {
    private static final String PLAYER = "tester";

    @Test
    public void takeChildSortsDescendingKeepsStableTiesSkipsNonSelectableAndReturnsCandidateIds() {
        GameState gameState = mock(GameState.class);
        PhysicalCard alpha = card("Alpha", 6.0f);
        PhysicalCard beta = card("Beta", 5.0f);
        PhysicalCard skipped = card("Skipped", 6.0f);
        PhysicalCard gamma = card("Gamma", 5.0f);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.findCardById(101)).thenReturn(alpha);
        when(gameState.findCardById(102)).thenReturn(beta);
        when(gameState.findCardById(103)).thenReturn(skipped);
        when(gameState.findCardById(104)).thenReturn(gamma);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION", "Choose card to take into hand",
                "take-child-rando", Phase.DEPLOY);
        configure(randoContext);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION", "Choose card to take into hand",
                "take-child-chosen", Phase.DEPLOY);
        configure(chosenContext);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(List.of("101", "102", "104"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertEquals(List.of("Alpha", "Beta", "Gamma"),
                rando.stream().map(action -> action.getCardName()).toList());
        assertEquals(List.of(
                        Float.floatToRawIntBits(110.0f),
                        Float.floatToRawIntBits(90.0f),
                        Float.floatToRawIntBits(90.0f)),
                rando.stream().map(action -> Float.floatToRawIntBits(action.getScore())).toList());

        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }

    @Test
    public void v70DeployCandidateBlockKeepsLegacyMagnitudeAndStopsCandidate() {
        PullDeployCandidatePolicy.Evaluation evaluation =
                PullDeployCandidatePolicy.evaluate(new PullDeployCandidateFacts(
                        "101", "Leia's Lightsaber", "all legal holders already armed"));

        assertEquals(PullDeployCandidatePolicy.AdapterStep.CONTINUE_CANDIDATE,
                evaluation.adapterStep());
        assertEquals(1, evaluation.result().operations().size());
        assertEquals(Float.floatToRawIntBits(-9999.0f),
                Float.floatToRawIntBits(evaluation.result().operations().get(0).delta()));
        assertEquals("V70-reserve-candidate",
                evaluation.result().operations().get(0).ruleArmId().id());
        assertTrue(evaluation.result().operations().get(0).reason()
                .contains("Leia's Lightsaber"));
    }

    @Test
    public void v70DeployCandidateWithoutBlockFallsThroughWithoutContribution() {
        PullDeployCandidatePolicy.Evaluation evaluation =
                PullDeployCandidatePolicy.evaluate(new PullDeployCandidateFacts(
                        "101", "Leia's Lightsaber", ""));

        assertEquals(PullDeployCandidatePolicy.AdapterStep.FALL_THROUGH,
                evaluation.adapterStep());
        assertTrue(evaluation.result().operations().isEmpty());
    }

    @Test
    public void v186IwtmTempLocationScoresTheSystemInBothAdapters() {
        GameState gameState = gameState(1);
        var randoObjective = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.isWantThatMap()).thenReturn(true);
        when(randoObjective.getIwtmSystemBpIds()).thenReturn(Set.of("208_51"));
        when(randoObjective.getIwtmSystemTitleFragment()).thenReturn("starkiller base");

        var chosenObjective = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.isWantThatMap()).thenReturn(true);
        when(chosenObjective.getIwtmSystemBpIds()).thenReturn(Set.of("208_51"));
        when(chosenObjective.getIwtmSystemTitleFragment()).thenReturn("starkiller base");

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "ARBITRARY_CARDS",
                "Choose where to deploy an Episode VII location",
                "v186-rando", Phase.DEPLOY);
        randoContext.setCardIds(List.of("temp0", "temp1"));
        randoContext.setBlueprints(List.of("208_51", "208_52"));
        randoContext.setSelectable(List.of(true, true));
        randoContext.setTestingTexts(List.of(
                "Starkiller Base", "Starkiller Base: Forest"));
        randoContext.setObjectiveAnalyzer(randoObjective);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "ARBITRARY_CARDS",
                "Choose where to deploy an Episode VII location",
                "v186-chosen", Phase.DEPLOY);
        chosenContext.setCardIds(List.of("temp0", "temp1"));
        chosenContext.setBlueprints(List.of("208_51", "208_52"));
        chosenContext.setSelectable(List.of(true, true));
        chosenContext.setTestingTexts(List.of(
                "Starkiller Base", "Starkiller Base: Forest"));
        chosenContext.setObjectiveAnalyzer(chosenObjective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(List.of("temp0", "temp1"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertScores(rando.get(0).getScore(), rando.get(1).getScore(),
                450.0f, 50.0f);
        assertMirrored(rando, chosen);
    }

    @Test
    public void v297ReplayPullChoosesThroneRoomOverGeneratorInBothAdapters() {
        GameState gameState = gameState(1);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard generator = card(
                "Naboo: Theed Palace Generator", 0.0f,
                CardCategory.LOCATION);
        PhysicalCard throne = card(
                "Naboo: Theed Palace Throne Room", 0.0f,
                CardCategory.LOCATION);
        when(generator.getBlueprintId(true)).thenReturn("13_76");
        when(throne.getBlueprintId(true)).thenReturn("12_174");
        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getCardPile(PLAYER, Zone.RESERVE_DECK))
                .thenReturn(List.of(generator, throne));
        when(gameState.getHand(PLAYER)).thenReturn(List.of());

        var randoObjective = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.isActiveFlipGateLocationTitle(
                "Naboo: Theed Palace Throne Room")).thenReturn(true);
        when(randoObjective.classifyPreFlipProgressCandidate(
                game, PLAYER, throne)).thenReturn(REQUIRED_LOCATION);
        var chosenObjective = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.isActiveFlipGateLocationTitle(
                "Naboo: Theed Palace Throne Room")).thenReturn(true);
        when(chosenObjective.classifyPreFlipProgressCandidate(
                game, PLAYER, throne)).thenReturn(REQUIRED_LOCATION);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "ARBITRARY_CARDS",
                "Choose card to deploy from Reserve Deck",
                "v297.2-rando", Phase.DEPLOY);
        randoContext.setGame(game);
        randoContext.setCardIds(List.of("temp6", "temp12"));
        randoContext.setBlueprints(List.of("13_76", "12_174"));
        randoContext.setSelectable(List.of(true, true));
        randoContext.setObjectiveAnalyzer(randoObjective);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "ARBITRARY_CARDS",
                "Choose card to deploy from Reserve Deck",
                "v297.2-chosen", Phase.DEPLOY);
        chosenContext.setGame(game);
        chosenContext.setCardIds(List.of("temp6", "temp12"));
        chosenContext.setBlueprints(List.of("13_76", "12_174"));
        chosenContext.setSelectable(List.of(true, true));
        chosenContext.setObjectiveAnalyzer(chosenObjective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(List.of("temp6", "temp12"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertEquals(List.of(
                        "Naboo: Theed Palace Generator",
                        "Naboo: Theed Palace Throne Room"),
                rando.stream().map(action -> action.getCardName()).toList());
        assertScores(rando.get(0).getScore(), rando.get(1).getScore(),
                40.0f, 340.0f);
        assertMirrored(rando, chosen);
    }

    @Test
    public void countedObjectivePullScoresRequiredActorAboveRequiredLocationInBothAdapters() {
        GameState gameState = gameState(2);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard actor = card(
                "Phoenix Squadron Character", 3.0f, CardCategory.CHARACTER);
        PhysicalCard location = card(
                "Lothal: Required Site", 0.0f, CardCategory.LOCATION);
        when(actor.getBlueprintId(true)).thenReturn("207_9");
        when(location.getBlueprintId(true)).thenReturn("219_39");
        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getCardPile(PLAYER, Zone.RESERVE_DECK))
                .thenReturn(List.of(actor, location));
        when(gameState.getHand(PLAYER)).thenReturn(List.of());

        var randoObjective = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.classifyPreFlipProgressCandidate(
                game, PLAYER, actor)).thenReturn(REQUIRED_ACTOR);
        when(randoObjective.classifyPreFlipProgressCandidate(
                game, PLAYER, location)).thenReturn(REQUIRED_LOCATION);

        var chosenObjective = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.classifyPreFlipProgressCandidate(
                game, PLAYER, actor)).thenReturn(REQUIRED_ACTOR);
        when(chosenObjective.classifyPreFlipProgressCandidate(
                game, PLAYER, location)).thenReturn(REQUIRED_LOCATION);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "ARBITRARY_CARDS",
                "Choose card to deploy from Reserve Deck",
                "counted-objective-rando", Phase.DEPLOY);
        randoContext.setGame(game);
        randoContext.setBlueprints(List.of("207_9", "219_39"));
        randoContext.setSelectable(List.of(true, true));
        randoContext.setTestingTexts(List.of(
                "Phoenix Squadron Character", "Lothal: Required Site"));
        randoContext.setObjectiveAnalyzer(randoObjective);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "ARBITRARY_CARDS",
                "Choose card to deploy from Reserve Deck",
                "counted-objective-chosen", Phase.DEPLOY);
        chosenContext.setGame(game);
        chosenContext.setBlueprints(List.of("207_9", "219_39"));
        chosenContext.setSelectable(List.of(true, true));
        chosenContext.setTestingTexts(List.of(
                "Phoenix Squadron Character", "Lothal: Required Site"));
        chosenContext.setObjectiveAnalyzer(chosenObjective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(List.of("0", "1"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertScores(rando.get(0).getScore(), rando.get(1).getScore(),
                450.0f, 350.0f);
        assertTrue(rando.get(0).getReasoning().stream().anyMatch(
                reason -> reason.contains("typed actor required")));
        assertTrue(rando.get(1).getReasoning().stream().anyMatch(
                reason -> reason.contains("missing location required")));
        assertMirrored(rando, chosen);
    }

    @Test
    public void unknownCloudCityPullPreservesExactAdapterScoreAndParity() {
        GameState gameState = gameState(3);
        PhysicalCard carbonite = card(
                "Cloud City: Carbonite Chamber", 2.0f, CardCategory.LOCATION);
        when(gameState.findCardById(301)).thenReturn(carbonite);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION",
                "Select a card for I'm Sorry interior Cloud City",
                "unknown-rando", Phase.DEPLOY);
        configureSingle(randoContext, "301", "inPlay",
                "Cloud City: Carbonite Chamber");

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION",
                "Select a card for I'm Sorry interior Cloud City",
                "unknown-chosen", Phase.DEPLOY);
        configureSingle(chosenContext, "301", "inPlay",
                "Cloud City: Carbonite Chamber");

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertScores(rando.get(0).getScore(), 190.0f);
        assertMirrored(rando, chosen);
    }

    @Test
    public void reserveBlueprintObjectiveOrderPreservesExactAdapterScores() {
        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, PLAYER, "ARBITRARY_CARDS",
                "Choose Cloud City battleground site to deploy",
                "blueprint-rando", Phase.DEPLOY);
        configureReserve(randoContext);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, PLAYER, "ARBITRARY_CARDS",
                "Choose Cloud City battleground site to deploy",
                "blueprint-chosen", Phase.DEPLOY);
        configureReserve(chosenContext);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(List.of("0", "1"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertScores(rando.get(0).getScore(), rando.get(1).getScore(),
                550.0f, -350.0f);
        assertMirrored(rando, chosen);
    }

    @Test
    public void amsdNonPiettRetainsLegacySetAddAndStopsLaterPilotScoring() {
        GameState gameState = gameState(3);
        PhysicalCard pilot = pilot("Jango Fett", 6.0f, 6.0f, 1.0f);
        when(gameState.findCardById(401)).thenReturn(pilot);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION",
                "Choose a unique pilot character",
                "amsd-rando", Phase.DEPLOY);
        configureSingle(randoContext, "401", "inPlay", "Jango Fett");

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION",
                "Choose a unique pilot character",
                "amsd-chosen", Phase.DEPLOY);
        configureSingle(chosenContext, "401", "inPlay", "Jango Fett");

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertScores(rando.get(0).getScore(), -19998.0f);
        assertFalse(rando.get(0).getReasoning().contains("Ability 6"));
        assertFalse(rando.get(0).getReasoning().contains("Good power bonus"));
        assertFalse(rando.get(0).getReasoning().contains("Deploy cost"));
        assertMirrored(rando, chosen);
    }

    private static void configure(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context) {
        context.setCardIds(List.of("101", "102", "103", "104"));
        context.setBlueprints(List.of("inPlay", "inPlay", "inPlay", "inPlay"));
        context.setSelectable(List.of(true, true, false, true));
        context.setTestingTexts(List.of("Alpha", "Beta", "Skipped", "Gamma"));
    }

    private static void configureSingle(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            String cardId, String blueprint, String title) {
        context.setCardIds(List.of(cardId));
        context.setBlueprints(List.of(blueprint));
        context.setSelectable(List.of(true));
        context.setTestingTexts(List.of(title));
    }

    private static void configureSingle(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            String cardId, String blueprint, String title) {
        context.setCardIds(List.of(cardId));
        context.setBlueprints(List.of(blueprint));
        context.setSelectable(List.of(true));
        context.setTestingTexts(List.of(title));
    }

    private static void configureReserve(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context) {
        context.setBlueprints(List.of("7_273", "5_168"));
        context.setTestingTexts(List.of(
                "Cloud City: Upper Walkway", "Cloud City: Dining Room"));
    }

    private static void configureReserve(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context) {
        context.setBlueprints(List.of("7_273", "5_168"));
        context.setTestingTexts(List.of(
                "Cloud City: Upper Walkway", "Cloud City: Dining Room"));
    }

    private static void configure(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context) {
        context.setCardIds(List.of("101", "102", "103", "104"));
        context.setBlueprints(List.of("inPlay", "inPlay", "inPlay", "inPlay"));
        context.setSelectable(List.of(true, true, false, true));
        context.setTestingTexts(List.of("Alpha", "Beta", "Skipped", "Gamma"));
    }

    private static PhysicalCard card(String title, float destiny) {
        return card(title, destiny, CardCategory.EFFECT);
    }

    private static PhysicalCard card(String title, float destiny,
                                     CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getDestiny()).thenReturn(destiny);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
    }

    private static PhysicalCard pilot(String title, float ability, float power,
                                      float deployCost) {
        PhysicalCard card = card(title, 3.0f, CardCategory.CHARACTER);
        SwccgCardBlueprint blueprint = card.getBlueprint();
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(ability);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        when(blueprint.getDeployCost()).thenReturn(deployCost);
        return card;
    }

    private static GameState gameState(int turnNumber) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(turnNumber);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        return gameState;
    }

    private static void assertScores(float actual, float expected) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private static void assertScores(float actualA, float actualB,
                                     float expectedA, float expectedB) {
        assertScores(actualA, expectedA);
        assertScores(actualB, expectedB);
    }

    private static void assertMirrored(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> chosen) {
        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }
}
