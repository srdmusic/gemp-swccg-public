package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Persistent-board behavior proof for I Can Save Him's opponent action.
 *
 * <p>The same board begins on the 9_61 front, captures Luke through the native
 * eligible-Imperial trigger, flips, then lets both public bots choose and
 * execute the exact back-side transfer before its end-turn loss can fire.
 */
public class TigihTransferLukeToVaderBehaviorTest {
    private static final String TRANSFER_ACTION =
            "Transfer Luke to Vader";

    private static StartingSetup tigihSetup() {
        return new StartingSetup() {
            @Override
            public HashMap<String, String> Cards() {
                HashMap<String, String> cards = new HashMap<>();
                cards.put("objective", "9_61");
                cards.put("hut", "8_71");
                cards.put("luke", "9_24");
                cards.put("lightsaber", "9_90");
                cards.put("platform", "8_76");
                cards.put("conflict", "9_34");
                return cards;
            }

            @Override
            public void Setup(VirtualTableScenario scn) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        };
    }

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("vader", "1_168");
                    put("imperial", "1_170");
                }},
                20,
                20,
                tigihSetup(),
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    @Test
    public void publicBotsChooseNativeTransferAndPreventEndTurnLoss()
            throws Exception {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PublicBots bots = PublicBots.forGame(scn);

        scn.StartGame();
        assertFalse(objective.isFlipped());

        scn.MoveCardsToLocation(hut, imperial);
        scn.SkipToPhase(Phase.CONTROL);
        scn.PassAllResponses();

        assertTrue("The eligible non-Vader Imperial must capture Luke",
                luke.isCaptive());
        assertEquals(imperial, luke.getEscort());
        assertTrue("The real 9_61 front must flip on that same board",
                objective.isFlipped());
        assertEquals("9_61_BACK", objective.getBlueprintId(false));

        scn.MoveCardsToLocation(hut, vader);
        moveDarkHandToReserve(scn);
        scn.SkipToPhase(Phase.DEPLOY);

        AwaitingDecision decision = scn.GetAwaitingDecision(DS);
        assertNotNull("Dark Side must receive the opponent action",
                decision);
        assertTrue(scn.DSCardActionAvailable(
                objective, TRANSFER_ACTION));
        assertFalse("Pass must remain a legal alternative",
                Boolean.parseBoolean(
                    decision.getDecisionParameters()
                        .get("noPass")[0]));

        String transferId = actionId(
                decision, TRANSFER_ACTION);
        assertNotNull(transferId);

        ScoredAction randoScore =
                scoreWithRando(decision, scn, transferId);
        ScoredAction chosenScore =
                scoreWithChosen(decision, scn, transferId);
        assertEquals("Mirrored evaluators must score identically",
                randoScore, chosenScore);
        assertFalse(randoScore.hardVeto());
        assertEquals(
                "The exact +250 objective arm must add to legacy +150",
                400.0f, randoScore.score(), 0.0f);
        assertTrue(randoScore.allReasoning().contains(
                "TIGIH BACK: transfer captive Luke to Vader"));
        assertTrue("The older generic transfer arm must remain additive",
                randoScore.allReasoning().contains(
                    "transfer action \u2014 usually a tactical swap"));

        String randoResponse = bots.rando().decide(
                DS, decision, scn.gameState());
        String chosenResponse = bots.chosen().decide(
                DS, decision, scn.gameState());
        assertEquals("Public Rando and Chosen One must agree",
                randoResponse, chosenResponse);
        assertEquals("The objective transfer must beat legal Pass",
                transferId, randoResponse);

        scn.DSDecided(randoResponse);
        scn.PassAllResponses();

        assertTrue(luke.isCaptive());
        assertEquals("Luke must transfer to the exact present Vader",
                vader, luke.getEscort());
        assertEquals(vader, luke.getAttachedTo());
        assertTrue(vader.getCardsEscorting().contains(luke));
        assertTrue("The old escort must release its exact captive",
                imperial.getCardsEscorting().isEmpty());
        assertTrue("Vader escorting Luke holds the real back side",
                objective.isFlipped());

        scn.SkipToPhase(Phase.DRAW);
        int lifeBeforeEndTurn =
                scn.GetDSLifeForceRemaining();
        scn.DSPass();
        scn.LSPass();

        assertFalse("Vader's escort must suppress the 2-Force trigger",
                scn.AwaitingDSForceLossPayment());
        assertFalse(scn.DSDecisionAvailable(
                "FORCE_LOSS_INITIATED - Optional responses"));
        assertEquals(lifeBeforeEndTurn,
                scn.GetDSLifeForceRemaining());
        assertTrue(objective.isFlipped());
    }

    @Test
    public void lethalCrossoverPressureVetoesTransferAndKeepsOldEscort()
            throws Exception {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl conflict = scn.GetLSCard("conflict");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PublicBots bots = PublicBots.forGame(scn);

        scn.StartGame();
        assertFalse(objective.isFlipped());

        scn.MoveCardsToLocation(hut, imperial);
        scn.SkipToPhase(Phase.CONTROL);
        scn.PassAllResponses();

        assertTrue(luke.isCaptive());
        assertEquals(imperial, luke.getEscort());
        assertTrue("The real 9_61 front must flip on this same board",
                objective.isFlipped());

        scn.MoveCardsToLocation(hut, vader);
        scn.StackCardsOn(
                conflict,
                scn.GetDSFiller(1),
                scn.GetDSFiller(2),
                scn.GetDSFiller(3));
        assertEquals("Three real I Feel The Conflict cards add 9",
                9.0f,
                scn.game().getModifiersQuerying()
                    .getCrossoverAttemptTotal(
                        scn.gameState(), vader, 0.0f),
                scn.epsilon);
        moveDarkHandToReserve(scn);
        scn.SkipToPhase(Phase.DEPLOY);

        AwaitingDecision decision = scn.GetAwaitingDecision(DS);
        assertNotNull(decision);
        assertTrue(scn.DSCardActionAvailable(
                objective, TRANSFER_ACTION));
        String transferId = actionId(
                decision, TRANSFER_ACTION);
        assertNotNull(transferId);

        ScoredAction randoScore =
                scoreWithRando(decision, scn, transferId);
        ScoredAction chosenScore =
                scoreWithChosen(decision, scn, transferId);
        assertEquals("Mirrored evaluators must veto identically",
                randoScore, chosenScore);
        assertTrue(randoScore.hardVeto());
        assertEquals("The legacy +150 remains visible under the veto",
                150.0f, randoScore.score(), 0.0f);
        assertTrue(randoScore.allReasoning().contains(
                "keep Luke with the non-Vader escort"));
        assertTrue("The older +150 rule remains present but dominated",
                randoScore.allReasoning().contains(
                    "transfer action \u2014 usually a tactical swap"));

        String randoResponse = bots.rando().decide(
                DS, decision, scn.gameState());
        String chosenResponse = bots.chosen().decide(
                DS, decision, scn.gameState());
        assertEquals(randoResponse, chosenResponse);
        assertEquals("The lethal transfer must lose to legal Pass",
                "", randoResponse);

        scn.DSDecided(randoResponse);
        scn.PassAllResponses();

        assertTrue(luke.isCaptive());
        assertEquals("Luke must remain with the old non-Vader escort",
                imperial, luke.getEscort());
        assertTrue(imperial.getCardsEscorting().contains(luke));
        assertFalse(vader.getCardsEscorting().contains(luke));
        assertTrue(objective.isFlipped());
    }

    private static void moveDarkHandToReserve(
            VirtualTableScenario scn) {
        List<PhysicalCardImpl> cards = new ArrayList<>();
        for (var card : scn.gameState().getHand(DS)) {
            if (card instanceof PhysicalCardImpl physical) {
                cards.add(physical);
            }
        }
        for (PhysicalCardImpl card : cards) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private static String actionId(
            AwaitingDecision decision,
            String exactText) {
        List<String> ids = strings(
                decision.getDecisionParameters(),
                "actionId");
        List<String> texts = strings(
                decision.getDecisionParameters(),
                "actionText");
        for (int i = 0; i < texts.size(); i++) {
            if (exactText.equals(texts.get(i))) {
                return ids.get(i);
            }
        }
        return null;
    }

    private static ScoredAction scoreWithRando(
            AwaitingDecision decision,
            VirtualTableScenario scn,
            String actionId) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        populate(context, decision);
        context.setGame(scn.game());
        context.setSide(Side.DARK);
        var action =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .ActionTextEvaluator()
                    .evaluate(context)
                    .stream()
                    .filter(candidate ->
                        actionId.equals(candidate.getActionId()))
                    .findFirst()
                    .orElseThrow();
        return new ScoredAction(
                action.getScore(),
                action.isHardVetoed(),
                List.copyOf(action.getReasoning()));
    }

    private static ScoredAction scoreWithChosen(
            AwaitingDecision decision,
            VirtualTableScenario scn,
            String actionId) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        populate(context, decision);
        context.setGame(scn.game());
        context.setSide(Side.DARK);
        var action =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .ActionTextEvaluator()
                    .evaluate(context)
                    .stream()
                    .filter(candidate ->
                        actionId.equals(candidate.getActionId()))
                    .findFirst()
                    .orElseThrow();
        return new ScoredAction(
                action.getScore(),
                action.isHardVetoed(),
                List.copyOf(action.getReasoning()));
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            AwaitingDecision decision) {
        Map<String, String[]> params =
                decision.getDecisionParameters();
        context.setActionIds(strings(params, "actionId"));
        context.setActionTexts(strings(params, "actionText"));
        context.setCardIds(strings(params, "cardId"));
        context.setNoPass(Boolean.parseBoolean(
                params.get("noPass")[0]));
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            AwaitingDecision decision) {
        Map<String, String[]> params =
                decision.getDecisionParameters();
        context.setActionIds(strings(params, "actionId"));
        context.setActionTexts(strings(params, "actionText"));
        context.setCardIds(strings(params, "cardId"));
        context.setNoPass(Boolean.parseBoolean(
                params.get("noPass")[0]));
    }

    private static List<String> strings(
            Map<String, String[]> params,
            String key) {
        String[] values = params.get(key);
        return values == null
                ? List.of()
                : Arrays.asList(values);
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
    }

    private record ScoredAction(
            float score,
            boolean hardVeto,
            List<String> reasoning) {
        private String allReasoning() {
            return String.join(" | ", reasoning);
        }
    }
}
