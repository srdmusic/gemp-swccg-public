package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

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

/** Public candidate and selected-action proof for Hunt Down safety guards. */
public class HuntDownActionSafetyDecisionTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    @Test
    public void classicHardLossActionsAreUnselectableAndPassWins() {
        for (Bot bot : Bot.values()) {
            Fixture scanning = fixture(bot);
            scanning.classicHunt();
            scanning.addSource(
                    10, source("Scanning Crew", "1_266"));
            assertHardVetoAndPass(
                    scanning,
                    new Decision(
                            Phase.DEPLOY,
                            "Play Scanning Crew",
                            "10"),
                    "CLASSIC HUNT DOWN HARD LOSS");

            Fixture duel = fixture(bot);
            duel.classicHunt();
            duel.addSource(
                    11, source("The Circle Is Now Complete", "4_134"));
            assertHardVetoAndPass(
                    duel,
                    new Decision(
                            Phase.BATTLE,
                            "Duel a Jedi",
                            "11"),
                    "CLASSIC HUNT DOWN HARD LOSS");

            Fixture drain = fixture(bot);
            drain.classicHunt();
            drain.addSource(12, executorSite());
            assertHardVetoAndPass(
                    drain,
                    new Decision(
                            Phase.CONTROL,
                            "Force drain",
                            "12"),
                    "CLASSIC HUNT DOWN HARD LOSS");
        }
    }

    @Test
    public void epicDuelAndTheExactMaulExceptionAreNotHardVetoed() {
        for (Bot bot : Bot.values()) {
            Fixture epic = fixture(bot);
            epic.classicHunt();
            epic.addSource(
                    20, source("Epic Duel", "5_129"));
            Outcome epicCandidate = epic.candidate(
                    new Decision(
                            Phase.BATTLE,
                            "Initiate Epic Duel",
                            "20"));
            assertFalse(epicCandidate.hardVeto());
            assertNotContains(
                    epicCandidate,
                    "CLASSIC HUNT DOWN HARD LOSS");

            Fixture maul = fixture(bot);
            maul.classicHunt();
            PhysicalCard maulSource =
                    source("Maul Strikes", "12_153");
            maul.addSource(21, maulSource);
            when(maul.analyzer
                    .hasClassicHuntDownMaulDuelException(
                            maul.game, PLAYER, maulSource))
                    .thenReturn(true);
            Outcome maulCandidate = maul.candidate(
                    new Decision(
                            Phase.BATTLE,
                            "Duel a Jedi",
                            "21"));
            assertFalse(maulCandidate.hardVeto());
            assertNotContains(
                    maulCandidate,
                    "CLASSIC HUNT DOWN HARD LOSS");
        }
    }

    @Test
    public void classicRecallDiscouragesClearGateButCanChaseRemoteBlocker() {
        for (Bot bot : Bot.values()) {
            Fixture hold = fixture(bot);
            hold.classicHunt();
            hold.addSource(
                    30,
                    source(
                            "Hunt Down And Destroy The Jedi",
                            "7_297"));
            when(hold.analyzer
                    .assessClassicHuntDownVaderRecall(
                            hold.game, PLAYER))
                    .thenReturn(
                            new ObjectiveAnalyzer
                                    .ClassicHuntDownRecallAssessment(
                                            false, false));
            assertBoundedPreferenceAndPass(
                    hold,
                    new Decision(
                            Phase.MOVE,
                            "Take Vader into hand",
                            "30"),
                    "V35 VADER RECALL DISFAVORED");

            Fixture chase = fixture(bot);
            chase.classicHunt();
            chase.addSource(
                    31,
                    source(
                            "Hunt Down And Destroy The Jedi",
                            "7_297"));
            when(chase.analyzer
                    .assessClassicHuntDownVaderRecall(
                            chase.game, PLAYER))
                    .thenReturn(
                            new ObjectiveAnalyzer
                                    .ClassicHuntDownRecallAssessment(
                                            true, true));
            Decision decision = new Decision(
                    Phase.MOVE,
                    "Take Vader into hand",
                    "31");
            Outcome candidate = chase.candidate(decision);
            assertFalse(candidate.hardVeto());
            assertContains(candidate, "Jedi elsewhere to hunt");
            assertEquals("danger",
                    chase.selected(decision).actionId());
        }
    }

    @Test
    public void virtualRecallDiscouragesRemovingTheLastPostFlipVader() {
        for (Bot bot : Bot.values()) {
            Fixture sole = fixture(bot);
            sole.virtualHunt();
            sole.addSource(
                    40,
                    source(
                            "Hunt Down And Destroy The Jedi (V)",
                            "213_31"));
            when(sole.analyzer
                    .hasSafeVirtualHuntDownVaderRecallTarget(
                            sole.game, PLAYER)).thenReturn(false);
            assertBoundedPreferenceAndPass(
                    sole,
                    new Decision(
                            Phase.MOVE,
                            "Take Vader into hand",
                            "40"),
                    "last Vader into hand");

            Fixture duplicate = fixture(bot);
            duplicate.virtualHunt();
            duplicate.addSource(
                    41,
                    source(
                            "Hunt Down And Destroy The Jedi (V)",
                            "213_31"));
            when(duplicate.analyzer
                    .hasSafeVirtualHuntDownVaderRecallTarget(
                            duplicate.game, PLAYER)).thenReturn(true);
            Outcome safe = duplicate.candidate(
                    new Decision(
                            Phase.MOVE,
                            "Take Vader into hand",
                            "41"));
            assertFalse(safe.hardVeto());
            assertContains(
                    safe,
                    "another Vader remains on table");
        }
    }

    @Test
    public void castleReturnDiscouragesUndoingTheOnlyRuntimeActorRoute() {
        for (Bot bot : Bot.values()) {
            Fixture hold = fixture(bot);
            hold.virtualHunt();
            PhysicalCard castle =
                    source("Mustafar: Vader's Castle", "209_50");
            hold.addSource(50, castle);
            when(hold.analyzer.hasPreFlipRuntimeActorRule())
                    .thenReturn(true);
            when(hold.analyzer
                    .mustHoldAllVaderCastleReturnMovers(
                            hold.game, PLAYER, castle))
                    .thenReturn(true);
            assertBoundedPreferenceAndPass(
                    hold,
                    new Decision(
                            Phase.MOVE,
                            "Move from other battleground site to here",
                            "50"),
                    "Castle return would evacuate");

            Fixture release = fixture(bot);
            release.virtualHunt();
            PhysicalCard safeCastle =
                    source("Mustafar: Vader's Castle", "209_50");
            release.addSource(51, safeCastle);
            when(release.analyzer.hasPreFlipRuntimeActorRule())
                    .thenReturn(true);
            when(release.analyzer
                    .mustHoldAllVaderCastleReturnMovers(
                            release.game, PLAYER, safeCastle))
                    .thenReturn(false);
            Outcome safe = release.candidate(
                    new Decision(
                            Phase.MOVE,
                            "Move from other battleground site to here",
                            "51"));
            assertFalse(safe.hardVeto());
        }
    }

    private static void assertHardVetoAndPass(
            Fixture fixture,
            Decision decision,
            String marker) {
        Outcome candidate = fixture.candidate(decision);
        assertTrue(candidate.reasoning().toString(),
                candidate.hardVeto());
        assertContains(candidate, marker);
        Outcome selected = fixture.selected(decision);
        assertEquals("pass", selected.actionId());
        assertFalse(selected.hardVeto());
    }

    private static void assertBoundedPreferenceAndPass(
            Fixture fixture,
            Decision decision,
            String marker) {
        Outcome candidate = fixture.candidate(decision);
        assertFalse(candidate.reasoning().toString(),
                candidate.hardVeto());
        assertContains(candidate, marker);
        assertTrue(candidate.reasoning().toString(),
                candidate.score() < 0.0f);
        Outcome selected = fixture.selected(decision);
        assertEquals("pass", selected.actionId());
        assertFalse(selected.hardVeto());
    }

    private static void assertContains(
            Outcome outcome, String marker) {
        assertTrue(
                "Expected '" + marker + "' in "
                        + outcome.reasoning(),
                outcome.reasoning().stream()
                        .anyMatch(reason ->
                                reason.contains(marker)));
    }

    private static void assertNotContains(
            Outcome outcome, String marker) {
        assertTrue(
                "Did not expect '" + marker + "' in "
                        + outcome.reasoning(),
                outcome.reasoning().stream()
                        .noneMatch(reason ->
                                reason.contains(marker)));
    }

    private static Fixture fixture(Bot bot) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        Map<Integer, PhysicalCard> sources =
                new LinkedHashMap<>();
        ObjectiveAnalyzer analyzer =
                bot == Bot.RANDO
                ? mock(com.gempukku.swccgo.ai.models.rando
                        .strategy.ObjectiveAnalyzer.class)
                : mock(com.gempukku.swccgo.ai.models.chosenone
                        .strategy.ObjectiveAnalyzer.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(3);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(10);
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(List.of());
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> sources.get(
                        invocation.getArgument(0, Integer.class)));
        when(analyzer.isAnalyzed()).thenReturn(true);

        return new Fixture(
                bot, gameState, game, analyzer, sources);
    }

    private static PhysicalCard source(
            String title, String blueprintId) {
        PhysicalCard source = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(source.getTitle()).thenReturn(title);
        when(source.getTitles()).thenReturn(List.of(title));
        when(source.getBlueprintId(true)).thenReturn(blueprintId);
        when(source.getBlueprint()).thenReturn(blueprint);
        when(source.isBlownAway()).thenReturn(false);
        when(blueprint.getTitle()).thenReturn(title);
        return source;
    }

    private static PhysicalCard executorSite() {
        PhysicalCard site =
                source("Executor: Meditation Chamber", "7_299");
        when(site.getBlueprint().hasIcon(Icon.STARSHIP_SITE))
                .thenReturn(true);
        when(site.getBlueprint()
                .getRelatedStarshipOrVehiclePersona())
                .thenReturn(Persona.EXECUTOR);
        return site;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Decision(
            Phase phase,
            String actionText,
            String sourceCardId) {
    }

    private record Outcome(
            String actionId,
            float score,
            List<String> reasoning,
            boolean hardVeto,
            String vetoReason) {
    }

    private static final class Fixture {
        private final Bot bot;
        private final GameState gameState;
        private final SwccgGame game;
        private final ObjectiveAnalyzer analyzer;
        private final Map<Integer, PhysicalCard> sources;

        private Fixture(
                Bot bot,
                GameState gameState,
                SwccgGame game,
                ObjectiveAnalyzer analyzer,
                Map<Integer, PhysicalCard> sources) {
            this.bot = bot;
            this.gameState = gameState;
            this.game = game;
            this.analyzer = analyzer;
            this.sources = sources;
        }

        private void classicHunt() {
            when(analyzer.isClassicHuntDownObjective())
                    .thenReturn(true);
            when(analyzer.isHuntDownV()).thenReturn(true);
        }

        private void virtualHunt() {
            when(analyzer.isVirtualHuntDownObjective())
                    .thenReturn(true);
            when(analyzer.isHuntDownV()).thenReturn(true);
        }

        private void addSource(
                int cardId, PhysicalCard source) {
            sources.put(cardId, source);
        }

        private Outcome candidate(Decision decision) {
            if (bot == Bot.RANDO) {
                List<com.gempukku.swccgo.ai.models.rando
                        .evaluators.EvaluatedAction> actions =
                        new com.gempukku.swccgo.ai.models.rando
                                .evaluators.ActionTextEvaluator()
                                .evaluate(randoContext(decision));
                return actions.stream()
                        .filter(action ->
                                "danger".equals(
                                        action.getActionId()))
                        .findFirst()
                        .map(Fixture::outcome)
                        .orElseThrow();
            }
            List<com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.EvaluatedAction> actions =
                    new com.gempukku.swccgo.ai.models.chosenone
                            .evaluators.ActionTextEvaluator()
                            .evaluate(chosenContext(decision));
            return actions.stream()
                    .filter(action ->
                            "danger".equals(
                                    action.getActionId()))
                    .findFirst()
                    .map(Fixture::outcome)
                    .orElseThrow();
        }

        private Outcome selected(Decision decision) {
            if (bot == Bot.RANDO) {
                return outcome(
                        new com.gempukku.swccgo.ai.models.rando
                                .evaluators.CombinedEvaluator()
                                .evaluateDecision(
                                        randoContext(decision)));
            }
            return outcome(
                    new com.gempukku.swccgo.ai.models.chosenone
                            .evaluators.CombinedEvaluator()
                            .evaluateDecision(
                                    chosenContext(decision)));
        }

        private com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext randoContext(
                        Decision decision) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando
                            .evaluators.DecisionContext(
                                    gameState, PLAYER,
                                    "CARD_ACTION_CHOICE",
                                    "Choose action",
                                    "hunt-safety",
                                    decision.phase());
            context.setGame(game);
            context.setActionIds(
                    List.of("danger", "pass"));
            context.setActionTexts(
                    List.of(decision.actionText(), "Pass"));
            context.setCardIds(
                    List.of(decision.sourceCardId(), ""));
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.rando
                            .strategy.ObjectiveAnalyzer) analyzer);
            return context;
        }

        private com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DecisionContext chosenContext(
                        Decision decision) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone
                            .evaluators.DecisionContext(
                                    gameState, PLAYER,
                                    "CARD_ACTION_CHOICE",
                                    "Choose action",
                                    "hunt-safety",
                                    decision.phase());
            context.setGame(game);
            context.setActionIds(
                    List.of("danger", "pass"));
            context.setActionTexts(
                    List.of(decision.actionText(), "Pass"));
            context.setCardIds(
                    List.of(decision.sourceCardId(), ""));
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.chosenone
                            .strategy.ObjectiveAnalyzer) analyzer);
            return context;
        }

        private static Outcome outcome(
                com.gempukku.swccgo.ai.models.rando.evaluators
                        .EvaluatedAction action) {
            assertNotNull(action);
            return new Outcome(
                    action.getActionId(),
                    action.getScore(),
                    List.copyOf(action.getReasoning()),
                    action.isHardVetoed(),
                    action.getVetoReason());
        }

        private static Outcome outcome(
                com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.EvaluatedAction action) {
            assertNotNull(action);
            return new Outcome(
                    action.getActionId(),
                    action.getScore(),
                    List.copyOf(action.getReasoning()),
                    action.isHardVetoed(),
                    action.getVetoReason());
        }
    }
}
