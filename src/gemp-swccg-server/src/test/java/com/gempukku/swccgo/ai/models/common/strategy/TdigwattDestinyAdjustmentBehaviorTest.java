package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Native battle proof for the virtual TDIGWATT back-side Lando destiny text.
 */
public class TdigwattDestinyAdjustmentBehaviorTest {
    private static final String ADJUST_DESTINY =
            "Add or subtract 1 from destiny draw";

    private static final StartingSetup VIRTUAL_TDIGWATT =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "226_12");
                        put("setupSite", "7_270");
                        put("imSorry", "226_6");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    // Both setup filters have exactly one matching card.
                }
            };

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("luke", "1_19");
                }},
                new HashMap<>() {{
                    put("site2", "5_166");
                    put("site3", "5_167");
                    put("battleSite", "5_168");
                    put("lando", "5_99");
                    put("lobot", "7_187");
                    put("djas", "1_171");
                    put("controller1", "1_194");
                    put("controller2", "1_194");
                    put("controller3", "1_194");
                }},
                30,
                30,
                StartingSetup.DoNothingSetup,
                VIRTUAL_TDIGWATT,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    @Test
    public void botsUseBothLobotEnabledDirectionsAndChangeRealDestinies() {
        VirtualTableScenario scn = startFlippedBattle();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl battleSite = scn.GetDSCard("battleSite");

        scn.SkipToDSTurn(Phase.BATTLE);
        scn.PrepareDSDestiny(3);
        scn.PrepareLSDestiny(5);
        scn.DSInitiateBattle(battleSite);
        scn.SkipToPowerSegment();

        assertEquals(1, scn.GetDSBattleDestinyCount());
        assertEquals(1, scn.GetLSBattleDestinyCount());

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(scn.game(), DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(scn.game(), DS, Side.DARK);
        PublicBots bots = PublicBots.forGame(scn);

        scn.DSChooseYes();
        passDestinyPreludeForDarkDraw(scn);
        assertTrue(scn.LSDecisionAvailable(
                "DESTINY_DRAWN - Optional responses"));
        scn.LSPass();

        assertDestinyChoiceAndResolve(
                scn, objective, randoAnalyzer, chosenAnalyzer,
                bots, "0",
                TdigwattObjectiveFacts.DestinyDrawOwner.YOURS);
        finishDestinyDraw(scn);
        assertEquals(
                "The real Dark Side destiny 3 must become 4",
                4, scn.GetDSTotalDestiny());

        scn.PassResponses(
                "BATTLE_DESTINY_DRAWS_COMPLETE_FOR_PLAYER");
        assertTrue(scn.LSDecisionAvailable("battle destiny?"));
        scn.LSChooseYes();
        passDestinyPreludeForLightDraw(scn);

        assertDestinyChoiceAndResolve(
                scn, objective, randoAnalyzer, chosenAnalyzer,
                bots, "1",
                TdigwattObjectiveFacts.DestinyDrawOwner.OPPONENTS);
        finishDestinyDraw(scn);
        assertEquals(
                "The real Light Side destiny 5 must become 4",
                4, scn.GetLSTotalDestiny());
        assertTrue(
                "The native virtual objective must remain back-side up",
                objective.isFlipped());
    }

    private static VirtualTableScenario startFlippedBattle() {
        VirtualTableScenario scn = scenario();
        scn.MoveCardsToLSHand(scn.GetLSCard("luke"));
        scn.MoveCardsToDSHand(
                scn.GetDSCard("site2"),
                scn.GetDSCard("site3"),
                scn.GetDSCard("battleSite"),
                scn.GetDSCard("lando"),
                scn.GetDSCard("lobot"),
                scn.GetDSCard("djas"),
                scn.GetDSCard("controller1"),
                scn.GetDSCard("controller2"),
                scn.GetDSCard("controller3"));
        scn.StartGame();

        scn.MoveLocationToTable(scn.GetDSCard("site2"));
        scn.MoveLocationToTable(scn.GetDSCard("site3"));
        scn.MoveLocationToTable(scn.GetDSCard("battleSite"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("setupSite"),
                scn.GetDSCard("controller1"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("site2"),
                scn.GetDSCard("controller2"));
        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("controller3"),
                scn.GetDSCard("site3"));
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }

        assertEquals(3, controlledBespinLocations(scn, DS));
        assertEquals(0, controlledBespinLocations(scn, LS));
        assertTrue(
                "The native third-control deployment must flip 226_12",
                scn.GetDSCard("objective").isFlipped());

        scn.MoveCardsToLocation(
                scn.GetDSCard("battleSite"),
                scn.GetDSCard("lando"),
                scn.GetDSCard("lobot"),
                scn.GetDSCard("djas"),
                scn.GetLSCard("luke"));
        return scn;
    }

    private static int controlledBespinLocations(
            VirtualTableScenario scn,
            String playerId) {
        return Filters.countTopLocationsOnTable(
                scn.game(),
                Filters.and(
                    Filters.Bespin_location,
                    Filters.controls(
                        playerId,
                        SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE)));
    }

    private static void passDestinyPreludeForDarkDraw(
            VirtualTableScenario scn) {
        assertTrue(scn.LSDecisionAvailable(
                "COST_TO_DRAW_DESTINY_CARD - Optional responses"));
        scn.LSPass();
        scn.DSPass();
        assertTrue(scn.LSDecisionAvailable(
                "ABOUT_TO_DRAW_DESTINY_CARD - Optional responses"));
        scn.LSPass();
        scn.DSPass();
    }

    private static void passDestinyPreludeForLightDraw(
            VirtualTableScenario scn) {
        assertTrue(scn.DSDecisionAvailable(
                "COST_TO_DRAW_DESTINY_CARD - Optional responses"));
        scn.DSPass();
        scn.LSPass();
        assertTrue(scn.DSDecisionAvailable(
                "ABOUT_TO_DRAW_DESTINY_CARD - Optional responses"));
        scn.DSPass();
        scn.LSPass();
    }

    private static void finishDestinyDraw(
            VirtualTableScenario scn) {
        scn.PassResponses("DESTINY_DRAWN");
        scn.PassResponses("COMPLETE_DESTINY_DRAW");
        scn.PassResponses("DRAWING_DESTINY_COMPLETE");
    }

    private static void assertDestinyChoiceAndResolve(
            VirtualTableScenario scn,
            PhysicalCardImpl objective,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer,
            PublicBots bots,
            String expectedDirection,
            TdigwattObjectiveFacts.DestinyDrawOwner expectedOwner) {
        AwaitingDecision parent = scn.GetAwaitingDecision(DS);
        assertNotNull(parent);
        assertEquals("CARD_ACTION_CHOICE",
                parent.getDecisionType().name());
        assertFalse(
                "The exact optional-response decision must retain Pass",
                raw(parent).noPass());
        ActionRef exactParent = exactAction(
                parent, objective.getCardId(),
                ADJUST_DESTINY);

        TdigwattObjectiveFacts.DestinyAdjustmentFacts facts =
                TdigwattObjectiveFactsReader
                    .readLiveDestinyAdjustmentFacts(
                        scn.game(), DS, objective)
                    .orElseThrow();
        assertEquals(expectedOwner, facts.drawOwner());
        assertEquals(
                "Owned Lobot in the native battle must grant two uses",
                2, facts.usesPerBattle());
        assertTrue(facts.battle().yourLandoInBattle());
        assertTrue(facts.battle().anyLobotParticipating());

        Choice combined = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer);
        String publicParent = bots.decideBoth(scn);
        assertEquals(
                "No-arg evaluators and public bots must agree",
                combined.actionId(), publicParent);
        assertEquals(
                "The exact objective parent must beat Pass",
                exactParent.actionId(), combined.actionId());
        assertTrue(
                "The objective rule must contribute a positive score",
                combined.score() > 0.0f);
        assertFalse(combined.hardVeto());
        scn.DSDecided(publicParent);

        AwaitingDecision nested = scn.GetAwaitingDecision(DS);
        assertNotNull(nested);
        assertEquals("MULTIPLE_CHOICE",
                nested.getDecisionType().name());
        assertEquals("Choose an option",
                nested.getText());
        assertEquals(List.of("Add 1", "Subtract 1"),
                strings(nested, "results"));

        Choice nestedCombined = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer);
        String publicNested = bots.decideBoth(scn);
        assertEquals(nestedCombined.actionId(),
                publicNested);
        assertEquals(
                expectedDirection, publicNested);
        assertEquals(
                "0".equals(expectedDirection)
                    ? "Add 1" : "Subtract 1",
                nestedCombined.text());
        assertFalse(nestedCombined.hardVeto());
        scn.DSDecided(publicNested);
    }

    private static ActionRef exactAction(
            AwaitingDecision decision,
            int sourceCardId,
            String actionText) {
        Raw raw = raw(decision);
        String expectedCardId =
                Integer.toString(sourceCardId);
        String expectedText =
                actionText.toLowerCase(Locale.ROOT);
        for (int index = 0;
                index < raw.actionIds().size();
                index++) {
            String cardId = value(
                    raw.cardIds(), index);
            String text = value(
                    raw.actionTexts(), index);
            if (expectedCardId.equals(cardId)
                    && text.toLowerCase(Locale.ROOT)
                        .contains(expectedText)) {
                return new ActionRef(
                        raw.actionIds().get(index),
                        text,
                        cardId);
            }
        }
        throw new AssertionError(
                "Exact source action not offered: cardId="
                    + expectedCardId + " text='"
                    + actionText + "' in "
                    + raw.actionTexts());
    }

    private static Choice evaluateBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        assertNotNull(decision);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        populate(randoContext, decision);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setDeckOracle(mock(
                com.gempukku.swccgo.ai.models.rando.strategy
                    .DeckOracle.class));
        randoContext.setExtra(
                TdigwattObjectiveFactsReader
                    .DESTINY_ADJUSTMENT_ACTION_SOURCES_EXTRA,
                TdigwattObjectiveFactsReader
                    .readDestinyAdjustmentActionSources(
                        decision, scn.game(), DS));

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        populate(chosenContext, decision);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setDeckOracle(mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy
                    .DeckOracle.class));
        chosenContext.setExtra(
                TdigwattObjectiveFactsReader
                    .DESTINY_ADJUSTMENT_ACTION_SOURCES_EXTRA,
                TdigwattObjectiveFactsReader
                    .readDestinyAdjustmentActionSources(
                        decision, scn.game(), DS));

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        Choice randoChoice = choice(
                decision, rando.getActionId(),
                rando.getScore(),
                rando.isHardVetoed());
        Choice chosenChoice = choice(
                decision, chosen.getActionId(),
                chosen.getScore(),
                chosen.isHardVetoed());
        assertEquals(
                "Rando and Chosen One no-arg evaluators must match",
                randoChoice, chosenChoice);
        return randoChoice;
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            AwaitingDecision decision) {
        Raw raw = raw(decision);
        context.setActionIds(raw.actionIds());
        context.setActionTexts(raw.actionTexts());
        context.setCardIds(raw.cardIds());
        context.setBlueprints(raw.blueprints());
        context.setTestingTexts(raw.testingTexts());
        context.setSelectable(raw.selectable());
        context.setNoPass(raw.noPass());
        context.setMin(raw.min());
        context.setMax(raw.max());
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            AwaitingDecision decision) {
        Raw raw = raw(decision);
        context.setActionIds(raw.actionIds());
        context.setActionTexts(raw.actionTexts());
        context.setCardIds(raw.cardIds());
        context.setBlueprints(raw.blueprints());
        context.setTestingTexts(raw.testingTexts());
        context.setSelectable(raw.selectable());
        context.setNoPass(raw.noPass());
        context.setMin(raw.min());
        context.setMax(raw.max());
    }

    private static Raw raw(
            AwaitingDecision decision) {
        Map<String, String[]> params =
                decision.getDecisionParameters();
        List<String> actionIds =
                strings(params, "actionId");
        List<String> actionTexts =
                strings(params, "actionText");
        List<String> results =
                strings(params, "results");
        if ("MULTIPLE_CHOICE".equals(
                    decision.getDecisionType().name())
                && results.equals(
                    List.of("Add 1", "Subtract 1"))) {
            actionIds = List.of("0", "1");
            actionTexts = results;
        }
        return new Raw(
                actionIds,
                actionTexts,
                strings(params, "cardId"),
                strings(params, "blueprintId"),
                strings(params, "testingText"),
                booleans(params, "selectable"),
                bool(params, "noPass", true),
                integer(params, "min", 0),
                integer(params, "max", 1));
    }

    private static Choice choice(
            AwaitingDecision decision,
            String actionId,
            float score,
            boolean hardVeto) {
        Raw raw = raw(decision);
        int index = raw.actionIds().indexOf(actionId);
        if (index < 0) {
            index = raw.cardIds().indexOf(actionId);
        }
        return new Choice(
                actionId,
                score,
                hardVeto,
                value(raw.actionTexts(), index),
                value(raw.cardIds(), index));
    }

    private static List<String> strings(
            AwaitingDecision decision,
            String key) {
        return strings(
                decision.getDecisionParameters(),
                key);
    }

    private static List<String> strings(
            Map<String, String[]> params,
            String key) {
        String[] values = params != null
                ? params.get(key) : null;
        return values == null
                ? List.of() : Arrays.asList(values);
    }

    private static List<Boolean> booleans(
            Map<String, String[]> params,
            String key) {
        return strings(params, key).stream()
                .map(Boolean::parseBoolean)
                .toList();
    }

    private static boolean bool(
            Map<String, String[]> params,
            String key,
            boolean fallback) {
        List<String> values = strings(params, key);
        return values.isEmpty()
                ? fallback
                : Boolean.parseBoolean(values.get(0));
    }

    private static int integer(
            Map<String, String[]> params,
            String key,
            int fallback) {
        List<String> values = strings(params, key);
        if (values.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(values.get(0));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String value(
            List<String> values,
            int index) {
        return index >= 0 && index < values.size()
                ? values.get(index) : "";
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(
                VirtualTableScenario scn) {
            var rando =
                    new com.gempukku.swccgo.ai.models.rando
                        .RandoCalAi();
            var chosen =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(
                VirtualTableScenario scn) {
            AwaitingDecision decision =
                    scn.GetAwaitingDecision(DS);
            assertNotNull(decision);
            String randoResponse = rando.decide(
                    DS, decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    DS, decision, scn.gameState());
            assertEquals(
                    "Public Rando and Chosen One must match",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private record ActionRef(
            String actionId,
            String text,
            String cardId) {
    }

    private record Raw(
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<Boolean> selectable,
            boolean noPass,
            int min,
            int max) {
    }

    private record Choice(
            String actionId,
            float score,
            boolean hardVeto,
            String text,
            String cardId) {
    }
}
