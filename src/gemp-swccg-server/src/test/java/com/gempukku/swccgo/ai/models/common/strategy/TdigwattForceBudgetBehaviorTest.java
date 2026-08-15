package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
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

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.TestBase.DS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Cross-phase Force-budget proof for the virtual TDIGWATT objective.
 *
 * <p>Battle Order makes the offered drain cost the entire three-card Force
 * pile. Both mirrored evaluators must apply the bounded objective reserve
 * preference, while the native move still spends its real one-Force cost.</p>
 */
public class TdigwattForceBudgetBehaviorTest {
    private static final String LANDO_MOVE =
            "Have your Lando make a regular move";
    private static final String FORCE_RESERVE_REASON =
            "Preserve the exact positive Force reserve for the "
                + "source-granted virtual Lando move";

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
                    // Both source setup filters have one legal candidate.
                }
            };

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("battleOrder", "13_54");
                    put("security", "601_202");
                    put("chasm", "5_167");
                    put("dining", "5_168");
                    put("lando", "5_99");
                    put("backup", "1_194");
                    put("secondController", "1_194");
                }},
                20,
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
    public void battleOrderDrainIsVetoedAndNativeLandoMoveKeepsItsForce()
            throws Exception {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl setupSite = scn.GetDSCard("setupSite");
        PhysicalCardImpl battleOrder =
                scn.GetDSCard("battleOrder");
        PhysicalCardImpl security = scn.GetDSCard("security");
        PhysicalCardImpl chasm = scn.GetDSCard("chasm");
        PhysicalCardImpl dining = scn.GetDSCard("dining");
        PhysicalCardImpl lando = scn.GetDSCard("lando");
        PhysicalCardImpl backup = scn.GetDSCard("backup");
        PhysicalCardImpl secondController =
                scn.GetDSCard("secondController");

        scn.MoveCardsToDSHand(
                battleOrder,
                security,
                chasm,
                dining,
                lando,
                backup,
                secondController);
        scn.StartGame();
        scn.MoveOutOfPlay(setupSite);
        scn.MoveCardsToDSSideOfTable(battleOrder);
        scn.MoveLocationToTable(security);
        scn.MoveLocationToTable(chasm);
        scn.MoveLocationToTable(dining);
        scn.MoveCardsToLocation(dining, lando, backup);
        scn.MoveCardsToLocation(
                security, secondController);
        enterDarkControlWithExactForce(scn, 3);

        assertFalse(objective.isFlipped());
        assertEquals(3, scn.GetDSForcePileCount());
        assertEquals(
                "The real 13_54 modifier must impose its full cost",
                3.0f,
                scn.game().getModifiersQuerying()
                    .getInitiateForceDrainCost(
                        scn.gameState(), security, DS),
                0.0f);
        assertTrue(
                "The source-granted Lando action must be offered beside drains",
                scn.DSCardActionAvailable(
                    objective, LANDO_MOVE));

        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        assertNotNull(decision);
        assertTrue(scn.AwaitingDSControlPhaseActions());
        ActionRef landoAction = exactAction(
                decision, objective.getCardId(), LANDO_MOVE);
        ActionRef drainAction = exactAction(
                decision, security.getCardId(), "force drain");

        Analyzers analyzers = Analyzers.forGame(scn);
        EvaluationView drain =
                evaluateActionTextBoth(
                    scn, analyzers, drainAction.actionId());
        assertFalse(
                "The exact three-Force drain has a bounded preference",
                drain.hardVeto());
        assertNull(drain.vetoReason());
        assertTrue(
                "The bounded reserve must remain visible in reasoning",
                drain.reasoning().contains(
                    FORCE_RESERVE_REASON + " (-300.0)"));

        EvaluationView combined =
                evaluateCombinedBoth(scn, analyzers);
        assertEquals(
                "The exact Lando route beats the bounded drain preference and Pass",
                landoAction.actionId(), combined.actionId());
        assertFalse(combined.hardVeto());

        PublicBots bots = PublicBots.forGame(scn);
        String publicResponse = bots.decideBoth(scn);
        assertEquals(
                "Public Rando and Chosen One must choose Lando, not drain",
                landoAction.actionId(), publicResponse);

        int forceBefore = scn.GetDSForcePileCount();
        scn.DSDecided(publicResponse);
        resolveLandoMove(scn, bots, chasm);
        scn.PassAllResponses();

        assertEquals(
                "The objective grants timing, not free movement",
                forceBefore - 1, scn.GetDSForcePileCount());
        assertAtLocation(chasm, lando);
        assertAtLocation(dining, backup);
        assertAtLocation(security, secondController);
        assertTrue(
                "The preserved Force must let the real move complete the flip",
                objective.isFlipped());
    }

    private static void resolveLandoMove(
            VirtualTableScenario scn,
            PublicBots bots,
            PhysicalCardImpl destination) {
        for (int guard = 0; guard < 4; guard++) {
            AwaitingDecision decision =
                    scn.GetAwaitingDecision(DS);
            if (decision == null
                    || scn.AwaitingDSControlPhaseActions()) {
                return;
            }
            String text = decision.getText() == null
                    ? ""
                    : decision.getText()
                        .toLowerCase(Locale.ROOT);
            if ("CARD_SELECTION".equals(
                    decision.getDecisionType().name())
                    && text.contains("where to move")) {
                String response = bots.decideBoth(scn);
                assertEquals(
                        "Both bots must keep the exact third-control destination",
                        Integer.toString(
                            destination.getCardId()),
                        response);
                scn.DSDecided(response);
                continue;
            }
            if ("MULTIPLE_CHOICE".equals(
                    decision.getDecisionType().name())
                    && text.contains(
                        "choose regular move action")) {
                String response = bots.decideBoth(scn);
                Raw offered = raw(decision);
                int index = Integer.parseInt(response);
                assertTrue(index >= 0
                        && index < offered.results().size());
                assertEquals(
                        "Move using landspeed",
                        offered.results().get(index));
                scn.DSDecided(response);
                continue;
            }
            throw new AssertionError(
                    "Unexpected Lando move decision: "
                        + decision.getDecisionType()
                        + " '" + decision.getText() + "'");
        }
        throw new AssertionError(
                "Lando move did not return to Control phase");
    }

    private static void enterDarkControlWithExactForce(
            VirtualTableScenario scn,
            int force) {
        scn.SkipToDSTurn();
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(
                    scn.GetTopOfDSForcePile());
        }
        scn.MoveCardsToBottomOfDSReserveDeck(
                scn.GetDSFiller(1));
        scn.DSActivateForceCheat(force);
        while (scn.GetDSReserveDeckCount() > 0) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSReserveDeck());
        }
        scn.PassActivateActions();
        if (scn.DSDecisionAvailable(
                "You have not activated Force. Do you want to Pass?")) {
            scn.DSChooseYes();
            scn.PassActivateActions();
        }
        assertTrue(
                "Expected Dark Side control actions",
                scn.AwaitingDSControlPhaseActions());
        assertEquals(force, scn.GetDSForcePileCount());
    }

    private static ActionRef exactAction(
            AwaitingDecision decision,
            int sourceCardId,
            String textFragment) {
        Raw raw = raw(decision);
        String expectedCardId =
                Integer.toString(sourceCardId);
        String expectedText =
                textFragment.toLowerCase(Locale.ROOT);
        for (int index = 0;
                index < raw.actionIds().size();
                index++) {
            if (expectedCardId.equals(
                    value(raw.cardIds(), index))
                    && value(raw.actionTexts(), index)
                        .toLowerCase(Locale.ROOT)
                        .contains(expectedText)) {
                return new ActionRef(
                        raw.actionIds().get(index),
                        value(raw.actionTexts(), index),
                        expectedCardId);
            }
        }
        throw new AssertionError(
                "Exact action not offered for cardId="
                    + expectedCardId + " text='"
                    + textFragment + "': "
                    + raw.actionTexts());
    }

    private static EvaluationView evaluateActionTextBoth(
            VirtualTableScenario scn,
            Analyzers analyzers,
            String actionId) {
        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.ActionTextEvaluator()
                .evaluate(randoContext(
                    scn, analyzers.rando())).stream()
                .filter(action ->
                    actionId.equals(action.getActionId()))
                .findFirst()
                .orElseThrow();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.ActionTextEvaluator()
                .evaluate(chosenContext(
                    scn, analyzers.chosen())).stream()
                .filter(action ->
                    actionId.equals(action.getActionId()))
                .findFirst()
                .orElseThrow();
        EvaluationView randoView =
                new EvaluationView(
                    rando.getActionId(),
                    rando.getActionType().name(),
                    rando.getScore(),
                    rando.isHardVetoed(),
                    rando.getVetoReason(),
                    List.copyOf(rando.getReasoning()));
        EvaluationView chosenView =
                new EvaluationView(
                    chosen.getActionId(),
                    chosen.getActionType().name(),
                    chosen.getScore(),
                    chosen.isHardVetoed(),
                    chosen.getVetoReason(),
                    List.copyOf(chosen.getReasoning()));
        assertEquals(
                "Mirrored ActionText evaluators must match exactly",
                randoView, chosenView);
        return randoView;
    }

    private static EvaluationView evaluateCombinedBoth(
            VirtualTableScenario scn,
            Analyzers analyzers) {
        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(randoContext(
                        scn, analyzers.rando()));
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(chosenContext(
                        scn, analyzers.chosen()));
        assertNotNull(rando);
        assertNotNull(chosen);
        EvaluationView randoView =
                new EvaluationView(
                    rando.getActionId(),
                    rando.getActionType().name(),
                    rando.getScore(),
                    rando.isHardVetoed(),
                    rando.getVetoReason(),
                    List.copyOf(rando.getReasoning()));
        EvaluationView chosenView =
                new EvaluationView(
                    chosen.getActionId(),
                    chosen.getActionType().name(),
                    chosen.getScore(),
                    chosen.isHardVetoed(),
                    chosen.getVetoReason(),
                    List.copyOf(chosen.getReasoning()));
        assertEquals(
                "Mirrored CombinedEvaluators must match exactly",
                randoView, chosenView);
        return randoView;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer analyzer) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        Phase.CONTROL);
        populate(context, decision);
        context.setGame(scn.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(analyzer);
        context.setDeckOracle(mock(
                com.gempukku.swccgo.ai.models.rando.strategy
                    .DeckOracle.class));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer analyzer) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        Phase.CONTROL);
        populate(context, decision);
        context.setGame(scn.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(analyzer);
        context.setDeckOracle(mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy
                    .DeckOracle.class));
        return context;
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

    private static Raw raw(AwaitingDecision decision) {
        Map<String, String[]> params =
                decision.getDecisionParameters();
        return new Raw(
                strings(params, "actionId"),
                strings(params, "actionText"),
                strings(params, "cardId"),
                strings(params, "blueprintId"),
                strings(params, "testingText"),
                strings(params, "results"),
                booleans(params, "selectable"),
                bool(params, "noPass", true),
                integer(params, "min", 0),
                integer(params, "max", 1));
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

    private record Analyzers(
            com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer rando,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer chosen) {
        private static Analyzers forGame(
                VirtualTableScenario scn) {
            var rando =
                    new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
            rando.analyze(scn.game(), DS, Side.DARK);
            var chosen =
                    new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
            chosen.analyze(scn.game(), DS, Side.DARK);
            return new Analyzers(rando, chosen);
        }
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
            assertNotNull(
                    "Dark Side must own the public bot decision",
                    decision);
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

    private record EvaluationView(
            String actionId,
            String actionType,
            float score,
            boolean hardVeto,
            String vetoReason,
            List<String> reasoning) {
    }

    private record Raw(
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<String> results,
            List<Boolean> selectable,
            boolean noPass,
            int min,
            int max) {
    }
}
