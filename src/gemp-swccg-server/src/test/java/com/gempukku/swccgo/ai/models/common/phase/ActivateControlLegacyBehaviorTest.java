package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.TestBase;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Current evaluator behavior that the ACTIVATE and CONTROL migration starts from. */
public class ActivateControlLegacyBehaviorTest {

    private static final List<String> EVALUATOR_ORDER = List.of(
            "ForceActivation", "Deploy", "Battle", "Move", "Draw",
            "CardSelection", "ActionText", "Pass");

    @Test
    public void bothBotsRegisterTheExactEightEvaluatorOrder() {
        List<String> rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator()
                .getEvaluators().stream().map(evaluator -> evaluator.getName()).toList();
        List<String> chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CombinedEvaluator()
                .getEvaluators().stream().map(evaluator -> evaluator.getName()).toList();

        assertEquals(EVALUATOR_ORDER, rando);
        assertEquals(EVALUATOR_ORDER, chosen);
    }

    @Test
    public void activateForceScores5500AndBeatsPassForBothBots() {
        VirtualTableScenario scenario = scenario(20);
        scenario.StartGame();
        AwaitingDecision decision = scenario.DSGetDecision();

        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext rando =
                randoContext(scenario, decision);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosen =
                chosenContext(scenario, decision);

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoActivate =
                randoAction(new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                        .evaluate(rando), "Activate Force");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenActivate =
                chosenAction(new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                        .evaluate(chosen), "Activate Force");
        float randoPass = new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(rando).get(0).getScore();
        float chosenPass = new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosen).get(0).getScore();

        assertEquals(5500.0f, randoActivate.getScore(), 0.0f);
        assertEquals(5500.0f, chosenActivate.getScore(), 0.0f);
        assertEquals(2.0f, randoPass, 0.0f);
        assertEquals(2.0f, chosenPass, 0.0f);
        assertTrue(randoActivate.getScore() > randoPass);
        assertTrue(chosenActivate.getScore() > chosenPass);
        assertEquals("Activate Force",
                new com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator()
                        .evaluateDecision(rando).getDisplayText());
        assertEquals("Activate Force",
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.CombinedEvaluator()
                        .evaluateDecision(chosen).getDisplayText());
    }

    @Test
    public void activatePassScoresTheFourReachableLegacyValues() {
        VirtualTableScenario highReserve = scenario(20);
        highReserve.StartGame();
        assertActivatePassScore(highReserve, 2.0f);
        advanceCurrentPlayerToTurnFour(highReserve);
        assertActivatePassScore(highReserve, 5.0f);

        VirtualTableScenario lowReserve = scenario(20);
        lowReserve.StartGame();
        trimDarkReserveTo(lowReserve, 9);
        assertActivatePassScore(lowReserve, 3.5f);
        advanceCurrentPlayerToTurnFour(lowReserve);
        assertActivatePassScore(lowReserve, 8.0f);
    }

    @Test
    public void activationAmountPreservesFullKeepThreeCriticalKeepTwoAndBounds() {
        VirtualTableScenario full = scenario(20);
        full.StartGame();
        assertActivation(full, 0, 5, "5", 60.0f);

        VirtualTableScenario contested = scenario(20);
        contested.StartGame();
        contested.MoveCardsToLocation(contested.GetDSStartingLocation(),
                contested.GetDSFiller(1), contested.GetLSFiller(1));
        assertEquals(19, contested.GetDSReserveDeckCount());
        assertActivation(contested, 0, 19, "16", 50.0f);

        VirtualTableScenario critical = scenario(10);
        critical.StartGame();
        assertEquals(10, critical.GetDSReserveDeckCount());
        assertActivation(critical, 0, 5, "3", 50.0f);
        assertActivation(critical, 2, 3, "2", 50.0f);
    }

    @Test
    public void representativeControlDrainScores70AndOutranksPassForBothBots() {
        VirtualTableScenario scenario = scenario(20);
        scenario.StartGame();
        scenario.MoveCardsToLocation(scenario.GetLSStartingLocation(), scenario.GetDSFiller(1));
        scenario.SkipToPhase(Phase.CONTROL);
        assertTrue(scenario.AwaitingDSControlPhaseActions());

        AwaitingDecision decision = scenario.DSGetDecision();
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext rando =
                randoContext(scenario, decision);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosen =
                chosenContext(scenario, decision);

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoDrain =
                randoAction(new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                        .evaluate(rando), "Force drain");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenDrain =
                chosenAction(new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                        .evaluate(chosen), "Force drain");
        float randoPass = new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(rando).get(0).getScore();
        float chosenPass = new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosen).get(0).getScore();

        assertEquals(70.0f, randoDrain.getScore(), 0.0f);
        assertEquals(70.0f, chosenDrain.getScore(), 0.0f);
        assertTrue(randoDrain.getScore() > randoPass);
        assertTrue(chosenDrain.getScore() > chosenPass);
    }

    @Test
    public void controlPassScoresReachableLowerBoundsForBothBots() {
        VirtualTableScenario scenario = scenario(20);
        scenario.StartGame();
        scenario.MoveCardsToDSHand(
                scenario.GetDSFiller(1), scenario.GetDSFiller(2), scenario.GetDSFiller(3),
                scenario.GetDSFiller(4), scenario.GetDSFiller(5), scenario.GetDSFiller(6),
                scenario.GetDSFiller(7));
        scenario.MoveCardsToTopOfDSForcePile(
                scenario.GetDSFiller(8), scenario.GetDSFiller(9), scenario.GetDSFiller(10));

        assertControlPassScore(scenario, 2.0f);
        advanceCurrentPlayerToTurnFour(scenario);
        assertControlPassScore(scenario, 5.0f);
    }

    @Test
    public void zeroValueControlDrainIsTerminalMinus9999ForBothBots() {
        HashMap<String, String> darkCards = new HashMap<>();
        darkCards.put("zero-drain-site", "2_150");
        VirtualTableScenario scenario = scenario(20, darkCards);
        scenario.StartGame();
        scenario.MoveLocationToTable(scenario.GetDSCard("zero-drain-site"));
        scenario.MoveCardsToLocation(scenario.GetDSCard("zero-drain-site"),
                scenario.GetDSFiller(1));
        scenario.SkipToPhase(Phase.CONTROL);

        AwaitingDecision decision = scenario.DSGetDecision();
        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoDrain =
                randoAction(new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                        .evaluate(randoContext(scenario, decision)), "Force drain");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenDrain =
                chosenAction(new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                        .evaluate(chosenContext(scenario, decision)), "Force drain");

        assertEquals(-9999.0f, randoDrain.getScore(), 0.0f);
        assertEquals(-9999.0f, chosenDrain.getScore(), 0.0f);
    }

    private static VirtualTableScenario scenario(int fillerCount) {
        return scenario(fillerCount, new HashMap<>());
    }

    private static VirtualTableScenario scenario(int fillerCount, HashMap<String, String> darkCards) {
        return new VirtualTableScenario(new HashMap<>(), darkCards,
                fillerCount, fillerCount,
                StartingSetup.DefaultLSGroundLocation, StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts, StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields, StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            VirtualTableScenario scenario, AwaitingDecision decision) {
        Map<String, String[]> params = decision.getDecisionParameters();
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, decision.getDecisionType().name(),
                        decision.getText(), String.valueOf(decision.getAwaitingDecisionId()),
                        scenario.GetCurrentPhase());
        context.setGame(scenario.game());
        context.setSide(Side.DARK);
        context.setActionIds(values(params, "actionId"));
        context.setActionTexts(values(params, "actionText"));
        context.setCardIds(values(params, "cardId"));
        context.setNoPass(booleanValue(params, "noPass", true));
        context.setMin(intValue(params, "min", 0));
        context.setMax(intValue(params, "max", 1));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            VirtualTableScenario scenario, AwaitingDecision decision) {
        Map<String, String[]> params = decision.getDecisionParameters();
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, decision.getDecisionType().name(),
                        decision.getText(), String.valueOf(decision.getAwaitingDecisionId()),
                        scenario.GetCurrentPhase());
        context.setGame(scenario.game());
        context.setSide(Side.DARK);
        context.setActionIds(values(params, "actionId"));
        context.setActionTexts(values(params, "actionText"));
        context.setCardIds(values(params, "cardId"));
        context.setNoPass(booleanValue(params, "noPass", true));
        context.setMin(intValue(params, "min", 0));
        context.setMax(intValue(params, "max", 1));
        return context;
    }

    private static void assertActivatePassScore(VirtualTableScenario scenario, float expected) {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, "CARD_ACTION_CHOICE",
                        "Choose Activate action or Pass", "pass-rando", Phase.ACTIVATE);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, "CARD_ACTION_CHOICE",
                        "Choose Activate action or Pass", "pass-chosen", Phase.ACTIVATE);
        rando.setNoPass(false);
        chosen.setNoPass(false);
        rando.setGame(scenario.game());
        chosen.setGame(scenario.game());

        assertEquals(expected, new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(rando).get(0).getScore(), 0.0f);
        assertEquals(expected, new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosen).get(0).getScore(), 0.0f);
    }

    private static void assertControlPassScore(VirtualTableScenario scenario, float expected) {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, "CARD_ACTION_CHOICE",
                        "Choose Control action or Pass", "control-pass-rando", Phase.CONTROL);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, "CARD_ACTION_CHOICE",
                        "Choose Control action or Pass", "control-pass-chosen", Phase.CONTROL);
        rando.setNoPass(false);
        chosen.setNoPass(false);
        rando.setGame(scenario.game());
        chosen.setGame(scenario.game());

        assertEquals(expected, new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(rando).get(0).getScore(), 0.0f);
        assertEquals(expected, new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosen).get(0).getScore(), 0.0f);
    }

    private static void assertActivation(VirtualTableScenario scenario, int min, int max,
                                         String expectedId, float expectedScore) {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, "INTEGER",
                        "Choose amount of Force to activate", "amount-rando", Phase.ACTIVATE);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        scenario.gameState(), TestBase.DS, "INTEGER",
                        "Choose amount of Force to activate", "amount-chosen", Phase.ACTIVATE);
        rando.setGame(scenario.game());
        chosen.setGame(scenario.game());
        rando.setMin(min);
        chosen.setMin(min);
        rando.setMax(max);
        chosen.setMax(max);

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoResult =
                new com.gempukku.swccgo.ai.models.rando.evaluators.ForceActivationEvaluator()
                        .evaluate(rando).get(0);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenResult =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.ForceActivationEvaluator()
                        .evaluate(chosen).get(0);
        assertEquals(expectedId, randoResult.getActionId());
        assertEquals(expectedId, chosenResult.getActionId());
        assertEquals(expectedScore, randoResult.getScore(), 0.0f);
        assertEquals(expectedScore, chosenResult.getScore(), 0.0f);
    }

    private static void trimDarkReserveTo(VirtualTableScenario scenario, int target) {
        while (scenario.GetDSReserveDeckCount() > target) {
            scenario.MoveOutOfPlay(scenario.GetTopOfDSReserveDeck());
        }
    }

    private static void advanceCurrentPlayerToTurnFour(VirtualTableScenario scenario) {
        while (scenario.gameState().getPlayersLatestTurnNumber(TestBase.DS) < 4) {
            scenario.gameState().incrementAndGetCurrentTurnNumber();
        }
    }

    private static List<String> values(Map<String, String[]> params, String key) {
        String[] value = params.get(key);
        return value == null ? List.of() : Arrays.asList(value);
    }

    private static int intValue(Map<String, String[]> params, String key, int fallback) {
        String[] value = params.get(key);
        return value == null || value.length == 0 ? fallback : Integer.parseInt(value[0]);
    }

    private static boolean booleanValue(Map<String, String[]> params, String key, boolean fallback) {
        String[] value = params.get(key);
        return value == null || value.length == 0 ? fallback : Boolean.parseBoolean(value[0]);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoAction(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> actions,
            String displayText) {
        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction result = actions.stream()
                .filter(action -> displayText.equals(action.getDisplayText())).findFirst().orElse(null);
        assertNotNull("missing Rando action: " + displayText, result);
        return result;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenAction(
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> actions,
            String displayText) {
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction result = actions.stream()
                .filter(action -> displayText.equals(action.getDisplayText())).findFirst().orElse(null);
        assertNotNull("missing Chosen action: " + displayText, result);
        return result;
    }
}
