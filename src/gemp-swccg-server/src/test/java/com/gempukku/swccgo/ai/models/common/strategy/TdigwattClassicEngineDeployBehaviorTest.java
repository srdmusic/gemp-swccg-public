package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
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
 * Public-bot proof for the classic TDIGWATT Cloud City Occupation deploy
 * parent. The card is legally deployable from hand in both fixtures, but its
 * source cancels it only when Light controls Bespin.
 */
public class TdigwattClassicEngineDeployBehaviorTest {
    private static final String CCO_VETO =
            "Reject the exact Cloud City Occupation deploy because "
                + "opponent controls Bespin";
    private static final String CCO_ENGINE_REASON =
            "Prioritize the exact engine-offered classic "
                + "Cloud City Occupation deploy";

    private static final StartingSetup CLASSIC_TDIGWATT =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "109_12");
                        put("setupSite", "7_270");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    // The setup filter has one matching site.
                }
            };

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("red3", "1_145");
                    put("rebelPilot", "1_27");
                }},
                new HashMap<>() {{
                    put("bespin", "223_8");
                    put("site2", "5_166");
                    put("cco", "7_223");
                    put("storm1", "1_194");
                    put("storm2", "1_194");
                    put("black3", "1_300");
                    put("imperialPilot", "1_180");
                }},
                20,
                30,
                StartingSetup.DoNothingSetup,
                CLASSIC_TDIGWATT,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    @Test
    public void legalCcoIsVetoedWhenLightControlsBespinAndStaysInHand() {
        Fixture fixture = deployFixture(false);
        VirtualTableScenario scn = fixture.scn();
        PhysicalCardImpl cco = fixture.cco();
        PhysicalCardImpl bespin = fixture.bespin();

        assertTrue(
                "The unchanged CCO source must still offer this legal deploy",
                scn.DSDeployAvailable(cco));
        assertTrue(GameConditions.controls(
                scn.game(), LS, bespin));
        assertFalse(GameConditions.controls(
                scn.game(), DS, bespin));
        assertEquals(
                Boolean.FALSE,
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        scn.game(), DS, cco)
                    .orElseThrow());

        AwaitingDecision parent = scn.GetAwaitingDecision(DS);
        ActionRef ccoAction = exactAction(
                parent, cco.getCardId(), "deploy");
        Analyzers analyzers = Analyzers.forGame(scn);
        EvaluationView ccoEvaluation =
                evaluateDeployBoth(
                    scn, analyzers, ccoAction.actionId());
        assertTrue(
                "The exact legal CCO action must be categorically rejected",
                ccoEvaluation.hardVeto());
        assertEquals(CCO_VETO, ccoEvaluation.vetoReason());
        assertTrue(
                ccoEvaluation.reasoning().contains(
                    "HARD VETO: " + CCO_VETO));

        EvaluationView combined =
                evaluateCombinedBoth(scn, analyzers);
        assertFalse(
                "The combined decision must reject the self-canceling CCO",
                ccoAction.actionId().equals(
                    combined.actionId()));

        PublicBots bots = PublicBots.forGame(scn);
        String publicResponse = bots.decideBoth(scn);
        assertFalse(
                "Both public bots must reject the exact CCO deploy",
                ccoAction.actionId().equals(publicResponse));
        Raw offered = raw(parent);
        assertTrue(
                "The selected alternative must be a legal offered action or Pass",
                publicResponse.isEmpty()
                    || offered.actionIds().contains(
                        publicResponse));
        scn.DSDecided(publicResponse);

        assertEquals(
                "Rejected CCO must remain available in hand",
                Zone.HAND, cco.getZone());
    }

    @Test
    public void contestedBespinGetsExactClassicEngineBonusAndCcoPersists() {
        Fixture fixture = deployFixture(true);
        VirtualTableScenario scn = fixture.scn();
        PhysicalCardImpl cco = fixture.cco();
        PhysicalCardImpl bespin = fixture.bespin();

        assertTrue(scn.DSDeployAvailable(cco));
        assertTrue(GameConditions.occupies(
                scn.game(), DS, bespin));
        assertTrue(GameConditions.occupies(
                scn.game(), LS, bespin));
        assertFalse(
                "Contested Bespin is not Light-controlled",
                GameConditions.controls(
                    scn.game(), LS, bespin));
        assertEquals(
                Boolean.TRUE,
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        scn.game(), DS, cco)
                    .orElseThrow());

        AwaitingDecision parent = scn.GetAwaitingDecision(DS);
        ActionRef ccoAction = exactAction(
                parent, cco.getCardId(), "deploy");
        Analyzers analyzers = Analyzers.forGame(scn);
        EvaluationView ccoEvaluation =
                evaluateDeployBoth(
                    scn, analyzers, ccoAction.actionId());
        assertFalse(ccoEvaluation.hardVeto());
        assertTrue(
                "The exact classic engine action must receive +300: "
                    + ccoEvaluation.reasoning(),
                ccoEvaluation.reasoning().stream()
                    .anyMatch(reason ->
                        reason.contains(CCO_ENGINE_REASON)
                        && reason.contains("(+300.0)")));

        EvaluationView combined =
                evaluateCombinedBoth(scn, analyzers);
        assertEquals(
                "The bounded engine preference plus live tactics beats Pass",
                ccoAction.actionId(), combined.actionId());

        PublicBots bots = PublicBots.forGame(scn);
        String publicResponse = bots.decideBoth(scn);
        assertEquals(
                "Both public bots must choose the exact stable CCO deploy",
                ccoAction.actionId(), publicResponse);
        int forceBefore = scn.GetDSForcePileCount();
        scn.DSDecided(publicResponse);

        AwaitingDecision destination =
                scn.GetAwaitingDecision(DS);
        assertNotNull(destination);
        assertEquals("CARD_SELECTION",
                destination.getDecisionType().name());
        String destinationResponse =
                bots.decideBoth(scn);
        assertEquals(
                "Bespin is the source's exact attachment target",
                Integer.toString(bespin.getCardId()),
                destinationResponse);
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertEquals(
                "CCO's printed 4 is destiny, not a Force cost",
                forceBefore,
                scn.GetDSForcePileCount());
        assertTrue(scn.IsAttachedTo(bespin, cco));
        assertFalse(
                "The source-stable CCO must not cancel at contested Bespin",
                cco.getZone() == Zone.LOST_PILE
                    || cco.getZone() == Zone.USED_PILE
                    || cco.getZone() == Zone.HAND);
    }

    private static Fixture deployFixture(
            boolean contestedBespin) {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl bespin = scn.GetDSCard("bespin");
        PhysicalCardImpl setupSite =
                scn.GetDSCard("setupSite");
        PhysicalCardImpl site2 = scn.GetDSCard("site2");
        PhysicalCardImpl cco = scn.GetDSCard("cco");
        PhysicalCardImpl storm1 =
                scn.GetDSCard("storm1");
        PhysicalCardImpl storm2 =
                scn.GetDSCard("storm2");
        PhysicalCardImpl red3 = scn.GetLSCard("red3");
        PhysicalCardImpl rebelPilot =
                scn.GetLSCard("rebelPilot");
        PhysicalCardImpl black3 =
                scn.GetDSCard("black3");
        PhysicalCardImpl imperialPilot =
                scn.GetDSCard("imperialPilot");

        scn.MoveCardsToLSHand(red3, rebelPilot);
        scn.MoveCardsToDSHand(
                bespin, site2, cco, storm1, storm2,
                black3, imperialPilot);
        scn.StartGame();
        scn.MoveLocationToTable(bespin);
        scn.MoveLocationToTable(site2);
        scn.MoveCardsToLocation(setupSite, storm1);
        scn.MoveCardsToLocation(site2, storm2);
        scn.MoveCardsToLocation(bespin, red3);
        scn.BoardAsPilot(red3, rebelPilot);
        if (contestedBespin) {
            scn.MoveCardsToLocation(bespin, black3);
            scn.BoardAsPilot(black3, imperialPilot);
        }
        moveOtherDarkHandCardsToReserve(scn, cco);
        enterDarkDeployWithExactForce(scn, 4);

        assertTrue(GameConditions.occupies(
                scn.game(), DS, 2,
                Filters.Cloud_City_battleground_site));
        return new Fixture(scn, bespin, cco);
    }

    private static void moveOtherDarkHandCardsToReserve(
            VirtualTableScenario scn,
            PhysicalCardImpl... keep) {
        List<PhysicalCardImpl> kept = Arrays.asList(keep);
        List<PhysicalCardImpl> hand = new ArrayList<>();
        for (var card : scn.gameState().getHand(DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !kept.contains(physical)) {
                hand.add(physical);
            }
        }
        for (PhysicalCardImpl card : hand) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private static void enterDarkDeployWithExactForce(
            VirtualTableScenario scn,
            int force) {
        scn.SkipToDSTurn();
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToBottomOfDSReserveDeck(
                    scn.GetTopOfDSForcePile());
        }
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
        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(scn.AwaitingDSDeployPhaseActions());
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

    private static EvaluationView evaluateDeployBoth(
            VirtualTableScenario scn,
            Analyzers analyzers,
            String actionId) {
        var rando =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DeployEvaluator()
                    .evaluate(randoContext(
                        scn, analyzers.rando()))
                    .stream()
                    .filter(action ->
                        actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .orElseThrow();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DeployEvaluator()
                    .evaluate(chosenContext(
                        scn, analyzers.chosen()))
                    .stream()
                    .filter(action ->
                        actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .orElseThrow();
        EvaluationView randoView = view(rando);
        EvaluationView chosenView =
                new EvaluationView(
                    chosen.getActionId(),
                    chosen.getScore(),
                    chosen.isHardVetoed(),
                    chosen.getVetoReason(),
                    List.copyOf(
                        chosen.getReasoning()));
        assertEquals(
                "Mirrored DeployEvaluators must match exactly",
                randoView, chosenView);
        return randoView;
    }

    private static EvaluationView evaluateCombinedBoth(
            VirtualTableScenario scn,
            Analyzers analyzers) {
        var rando =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext(
                        scn, analyzers.rando()));
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenContext(
                        scn, analyzers.chosen()));
        assertNotNull(rando);
        assertNotNull(chosen);
        EvaluationView randoView = view(rando);
        EvaluationView chosenView =
                new EvaluationView(
                    chosen.getActionId(),
                    chosen.getScore(),
                    chosen.isHardVetoed(),
                    chosen.getVetoReason(),
                    List.copyOf(
                        chosen.getReasoning()));
        assertEquals(
                "Mirrored CombinedEvaluators must match exactly",
                randoView, chosenView);
        return randoView;
    }

    private static EvaluationView view(
            com.gempukku.swccgo.ai.models.rando.evaluators
                .EvaluatedAction action) {
        return new EvaluationView(
                action.getActionId(),
                action.getScore(),
                action.isHardVetoed(),
                action.getVetoReason(),
                List.copyOf(action.getReasoning()));
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
                        Phase.DEPLOY);
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
                        Phase.DEPLOY);
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

    private record Fixture(
            VirtualTableScenario scn,
            PhysicalCardImpl bespin,
            PhysicalCardImpl cco) {
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
            List<Boolean> selectable,
            boolean noPass,
            int min,
            int max) {
    }
}
