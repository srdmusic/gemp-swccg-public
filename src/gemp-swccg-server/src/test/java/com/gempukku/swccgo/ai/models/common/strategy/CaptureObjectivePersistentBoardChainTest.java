package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
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

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.TestBase.DS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Persistent-board behavior proof for Bring Him Before Me.
 *
 * Both public bot adapters evaluate each real engine decision. The shared
 * winner is then submitted to that same game. The resulting Vader deploy must
 * capture Luke and fire the native flip before the next-turn Emperor download
 * and exact duel payoff are evaluated.
 */
public class CaptureObjectivePersistentBoardChainTest {
    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("luke", "9_24");
                }},
                new HashMap<>() {{
                    put("vader", "1_168");
                    put("emperor", "9_109");
                }},
                30,
                30,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.BHBMObjective,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    @Test
    public void realAiWinnersCaptureFlipThenPursueEmperorAndDuel()
            throws Exception {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, luke);
        scn.MoveCardsToDSHand(vader);
        scn.MoveCardsToTopOfDSUsedPile(emperor);
        moveOtherDarkHandCardsToReserve(scn, vader);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(scn.game(), DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(scn.game(), DS, Side.DARK);
        PublicBots publicBots = PublicBots.forGame(scn);

        scn.DSActivateMaxForceAndPass();
        int missingVaderForce = 6 - scn.GetDSForcePileCount();
        assertTrue("Natural activation exceeded the Vader boundary",
                missingVaderForce >= 0);
        scn.DSActivateForceCheat(missingVaderForce);
        assertEquals(6, scn.GetDSForcePileCount());
        scn.SkipToPhase(Phase.DEPLOY);

        Choice vaderParent = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer, null);
        assertTrue(vaderParent.text().contains("Deploy")
                && vaderParent.cardId().equals(
                    Integer.toString(vader.getCardId())));
        assertTrue(vaderParent.allText().contains("CAPTURE DEPLOY"));
        assertEquals(vaderParent.actionId(),
                publicBots.decideBoth(scn));
        scn.DSDecided(vaderParent.actionId());

        AwaitingDecision vaderDeployDecision =
                scn.GetAwaitingDecision(DS);
        assertTrue("Expected Vader destination after parent choice, got "
                    + describe(vaderDeployDecision),
                scn.DSDecisionAvailable("Choose where to deploy")
                || scn.DSDecisionAvailable(
                    "Choose location where to deploy"));
        Choice vaderDestination = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer,
                vader.getPermanentCardId());
        assertEquals(Integer.toString(throne.getCardId()),
                vaderDestination.actionId());
        assertTrue(vaderDestination.allText()
                .contains("CAPTURE DEPLOY"));
        assertEquals(vaderDestination.actionId(),
                publicBots.decideBoth(scn));
        scn.DSDecided(vaderDestination.actionId());
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertAtLocation(throne, vader);
        assertTrue(luke.isCaptive());
        assertEquals(vader, luke.getEscort());
        assertTrue("The real BHBM source must flip after Vader seizes Luke",
                objective.isFlipped());

        finishDarkTurnAndRecirculate(scn);
        scn.SkipToDSTurn(Phase.ACTIVATE);
        assertEquals(Zone.RESERVE_DECK, emperor.getZone());
        scn.DSActivateMaxForceAndPass();
        if (scn.GetDSForcePileCount() < 3) {
            scn.DSActivateForceCheat(
                    3 - scn.GetDSForcePileCount());
        }
        if (scn.GetDSForcePileCount() > 3) {
            scn.DSUseForceCheat(
                    scn.GetDSForcePileCount() - 3);
        }
        assertEquals(3, scn.GetDSForcePileCount());
        scn.SkipToPhase(Phase.DEPLOY);

        randoAnalyzer.refreshFlipStatus(
                scn.gameState(), DS);
        chosenAnalyzer.refreshFlipStatus(
                scn.gameState(), DS);
        Choice emperorParent = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer, null);
        assertEquals("Deploy Emperor from Reserve Deck",
                emperorParent.text());
        assertTrue(emperorParent.allText().contains("BHBM PAYOFF"));
        assertEquals(emperorParent.actionId(),
                publicBots.decideBoth(scn));
        scn.DSDecided(emperorParent.actionId());

        if (scn.DSDecisionAvailable("Choose Emperor")
                || scn.DSDecisionAvailable(
                    "Choose card to deploy from Reserve Deck")) {
            Choice emperorCard = evaluateBoth(
                    scn, randoAnalyzer, chosenAnalyzer, null);
            assertEquals("9_109",
                    normalizeBlueprint(emperorCard.blueprintId()));
            assertEquals(emperorCard.actionId(),
                    publicBots.decideBoth(scn));
            scn.DSDecided(emperorCard.actionId());
        }
        if (!scn.DSAnyDecisionsAvailable()) {
            scn.PassResponses(
                    "LOOKED_AT_CARDS_IN_CARD_PILE");
        }
        if (!scn.DSAnyDecisionsAvailable()) {
            scn.PassCardPlayResponses();
        }

        AwaitingDecision emperorDeployDecision =
                scn.GetAwaitingDecision(DS);
        assertTrue("Expected Emperor destination after reserve choice, got "
                    + describe(emperorDeployDecision)
                    + "; current " + describe(
                        scn.GetCurrentDecision()),
                scn.DSDecisionAvailable("Choose where to deploy")
                || scn.DSDecisionAvailable(
                    "Choose location where to deploy"));
        Choice emperorDestination = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer,
                emperor.getPermanentCardId());
        assertEquals(Integer.toString(throne.getCardId()),
                emperorDestination.actionId());
        assertTrue("Expected BHBM Emperor staging reason, got "
                    + emperorDestination.allText(),
                emperorDestination.allText()
                    .contains("secondary back-side payoff"));
        assertEquals(emperorDestination.actionId(),
                publicBots.decideBoth(scn));
        scn.DSDecided(emperorDestination.actionId());
        scn.PassAllResponses();

        assertEquals("9_109's source deploy -2 must cost exactly three",
                0, scn.GetDSForcePileCount());
        assertAtLocation(throne, vader, emperor);
        assertTrue(luke.isCaptive());
        assertTrue(objective.isFlipped());
        if (!scn.DSAnyDecisionsAvailable()
                && scn.LSAnyDecisionsAvailable()) {
            scn.LSPass();
        }
        assertTrue("Expected Dark Side payoff decision after Emperor deploy,"
                    + " current " + describe(scn.GetCurrentDecision()),
                scn.DSAnyDecisionsAvailable());
        assertTrue(scn.DSCardActionAvailable(
                objective, "Initiate a Luke/Vader duel"));

        Choice duel = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer, null);
        assertEquals("Initiate a Luke/Vader duel", duel.text());
        assertTrue(duel.allText()
                .contains("initiate the legal Vader duel"));
        assertFalse(duel.hardVeto());
        assertEquals(duel.actionId(),
                publicBots.decideBoth(scn));
    }

    private static void moveOtherDarkHandCardsToReserve(
            VirtualTableScenario scn,
            PhysicalCardImpl keep) {
        List<PhysicalCardImpl> hand = new ArrayList<>();
        for (var card : scn.gameState().getHand(DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && physical != keep) {
                hand.add(physical);
            }
        }
        for (PhysicalCardImpl card : hand) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private static void finishDarkTurnAndRecirculate(
            VirtualTableScenario scn) {
        scn.SkipToPhase(Phase.DRAW);
        scn.DSPass();
        scn.LSPass();
        scn.PassAllResponses();
        if (scn.AwaitingDSForceLossPayment()) {
            scn.DSPayRemainingForceLossFromReserveDeck();
            scn.PassAllResponses();
        }
    }

    private static Choice evaluateBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer,
            Integer deployingPermanentId) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        assertTrue("Dark Side must own the live decision",
                decision != null);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(), DS,
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
        if (deployingPermanentId != null) {
            randoContext.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), DS,
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
        if (deployingPermanentId != null) {
            chosenContext.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(chosenContext);
        assertTrue(rando != null);
        assertTrue(chosen != null);
        Choice randoChoice = choice(
                decision, rando.getActionId(),
                rando.getActionType().name(),
                rando.getScore(),
                rando.isHardVetoed(),
                rando.getReasoning(),
                rando.getVetoReason());
        Choice chosenChoice = choice(
                decision, chosen.getActionId(),
                chosen.getActionType().name(),
                chosen.getScore(),
                chosen.isHardVetoed(),
                chosen.getReasoning(),
                chosen.getVetoReason());
        assertEquals("Rando and Chosen One must select the same response",
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

    private static Raw raw(AwaitingDecision decision) {
        Map<String, String[]> params =
                decision.getDecisionParameters();
        return new Raw(
                strings(params, "actionId"),
                strings(params, "actionText"),
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
            String actionType,
            float score,
            boolean hardVeto,
            List<String> reasoning,
            String vetoReason) {
        Raw raw = raw(decision);
        int index = raw.actionIds().indexOf(actionId);
        if (index < 0) {
            index = raw.cardIds().indexOf(actionId);
        }
        return new Choice(
                actionId,
                actionType,
                score,
                hardVeto,
                index >= 0 && index < raw.actionTexts().size()
                    ? raw.actionTexts().get(index) : "",
                index >= 0 && index < raw.cardIds().size()
                    ? raw.cardIds().get(index) : "",
                index >= 0 && index < raw.blueprints().size()
                    ? raw.blueprints().get(index) : "",
                List.copyOf(reasoning),
                vetoReason);
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

    private static String normalizeBlueprint(
            String blueprintId) {
        if (blueprintId == null) {
            return "";
        }
        return blueprintId.endsWith("_BACK")
                ? blueprintId.substring(
                    0, blueprintId.length() - 5)
                : blueprintId;
    }

    private static String describe(
            AwaitingDecision decision) {
        return decision == null
                ? "no Dark Side decision"
                : decision.getDecisionType() + " '"
                    + decision.getText() + "'";
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
            assertTrue("Dark Side must own the public bot decision",
                    decision != null);
            String randoResponse = rando.decide(
                    DS, decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    DS, decision, scn.gameState());
            assertEquals(
                    "Public Rando and Chosen One responses must match",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
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
            String actionType,
            float score,
            boolean hardVeto,
            String text,
            String cardId,
            String blueprintId,
            List<String> reasoning,
            String vetoReason) {
        private String allText() {
            return String.join(" | ", reasoning)
                    + " | "
                    + (vetoReason != null
                        ? vetoReason : "");
        }
    }
}
