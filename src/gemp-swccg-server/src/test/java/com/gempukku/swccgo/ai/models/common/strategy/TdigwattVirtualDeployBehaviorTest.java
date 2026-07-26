package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectivePolicy;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
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

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Native deploy-and-flip proof for virtual TDIGWATT.
 */
public class TdigwattVirtualDeployBehaviorTest {
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
                    put("site2", "5_166");
                    put("site3", "5_167");
                    put("vader", "1_168");
                    put("emperor", "10_51");
                    put("maul", "11_55");
                    put("stormtrooper", "1_194");
                    put("seBespin", "223_8");
                    put("darkDeal", "223_9");
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
    public void botsDeployToThirdControlledBespinLocationAndNativeFlipFires() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl setupSite = scn.GetDSCard("setupSite");
        PhysicalCardImpl site2 = scn.GetDSCard("site2");
        PhysicalCardImpl site3 = scn.GetDSCard("site3");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");
        PhysicalCardImpl maul = scn.GetDSCard("maul");
        PhysicalCardImpl stormtrooper =
                scn.GetDSCard("stormtrooper");
        PhysicalCardImpl seBespin =
                scn.GetDSCard("seBespin");
        PhysicalCardImpl darkDeal =
                scn.GetDSCard("darkDeal");

        scn.MoveCardsToDSHand(
                site2, site3, vader, emperor, maul, stormtrooper,
                seBespin, darkDeal);
        scn.StartGame();
        scn.MoveLocationToTable(seBespin);
        scn.MoveLocationToTable(site2);
        scn.MoveLocationToTable(site3);
        scn.MoveCardsToLocation(setupSite, vader);
        scn.MoveCardsToLocation(site2, emperor);
        moveOtherDarkHandCardsToReserve(
                scn, maul, stormtrooper, darkDeal);
        enterDarkDeployWithExactForce(scn, 9);

        assertFalse(objective.isFlipped());
        assertEquals(2, controlledBespinLocations(scn, DS));
        assertEquals(0, controlledBespinLocations(scn, LS));
        assertTrue(TdigwattObjectiveFactsReader
                .readVirtualState(scn.game(), DS)
                .map(state ->
                    !TdigwattObjectivePolicy.virtualFlipReady(state))
                .orElse(false));
        assertTrue("Darth Maul must be a legal deploy candidate",
                scn.DSDeployAvailable(maul));
        assertTrue("Stormtrooper must be a competing legal deploy action",
                scn.DSDeployAvailable(stormtrooper));
        assertTrue(
                "The exact virtual Dark Deal must be source-legal at two controlled Cloud City sites",
                scn.DSDeployAvailable(darkDeal));

        PublicBots bots = PublicBots.forGame(scn);
        AwaitingDecision parent = scn.GetAwaitingDecision(DS);
        ActionRef maulDeploy = exactAction(
                parent, maul.getCardId(), "deploy");
        ActionRef stormDeploy = exactAction(
                parent, stormtrooper.getCardId(), "deploy");
        ActionRef darkDealDeploy = exactAction(
                parent, darkDeal.getCardId(), "deploy");
        assertFalse(maulDeploy.actionId().equals(
                stormDeploy.actionId()));
        assertFalse(
                "Dark Deal must be a distinct legal engine-effect alternative",
                maulDeploy.actionId().equals(
                    darkDealDeploy.actionId()));

        String parentResponse = bots.decideBoth(scn);
        assertEquals(
                "Both bots must choose the viable high-ability deploy",
                maulDeploy.actionId(), parentResponse);
        scn.DSDecided(parentResponse);

        AwaitingDecision destination =
                scn.GetAwaitingDecision(DS);
        assertNotNull(destination);
        assertEquals("CARD_SELECTION",
                destination.getDecisionType().name());
        assertTrue(destination.getText()
                .toLowerCase(Locale.ROOT)
                .contains("where to deploy"));
        Raw offered = raw(destination);
        assertTrue(
                "The already-controlled site must remain a legal distractor",
                selectable(offered, setupSite));
        assertTrue(
                "The second controlled site must remain a legal distractor",
                selectable(offered, site2));
        assertTrue(
                "The third Bespin location must be legal",
                selectable(offered, site3));

        Analyzers analyzers = Analyzers.forGame(scn);
        Map<String, EvaluationView> scores =
                evaluateDestinationsBoth(
                    scn, analyzers, maul);
        EvaluationView third =
                scores.get(Integer.toString(site3.getCardId()));
        EvaluationView first =
                scores.get(Integer.toString(setupSite.getCardId()));
        EvaluationView second =
                scores.get(Integer.toString(site2.getCardId()));
        assertNotNull(third);
        assertNotNull(first);
        assertNotNull(second);
        String destinationResponse = bots.decideBoth(scn);
        assertTrue(
                "The exact virtual flip law must score the third site: "
                    + third.reasoning(),
                third.reasoning().stream()
                    .anyMatch(reason ->
                        reason.contains(
                            "Complete the exact source-defined front-side flip law")
                        && reason.contains("(+1200.0)")));
        assertFalse(first.reasoning().stream()
                .anyMatch(reason ->
                    reason.contains(
                        "Complete the exact source-defined front-side flip law")));
        assertFalse(second.reasoning().stream()
                .anyMatch(reason ->
                    reason.contains(
                        "Complete the exact source-defined front-side flip law")));

        assertEquals(
                "Both bots must choose the third-control Bespin location",
                Integer.toString(site3.getCardId()),
                destinationResponse);
        int forceBefore = scn.GetDSForcePileCount();
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertAtLocation(site3, maul);
        assertEquals(
                "The native deploy must spend Darth Maul's printed cost",
                forceBefore - 8, scn.GetDSForcePileCount());
        assertEquals(3, controlledBespinLocations(scn, DS));
        assertEquals(0, controlledBespinLocations(scn, LS));
        assertTrue(
                "The unchanged 226_12 source must flip after the bot deploy",
                objective.isFlipped());
    }

    private static boolean selectable(
            Raw raw,
            PhysicalCardImpl card) {
        int index = raw.cardIds().indexOf(
                Integer.toString(card.getCardId()));
        return index >= 0
                && (raw.selectable().isEmpty()
                    || index < raw.selectable().size()
                        && raw.selectable().get(index));
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

    private static Map<String, EvaluationView>
            evaluateDestinationsBoth(
                    VirtualTableScenario scn,
                    Analyzers analyzers,
                    PhysicalCardImpl deployingCard) {
        var randoContext =
                randoContext(
                    scn, analyzers.rando(), deployingCard);
        var chosenContext =
                chosenContext(
                    scn, analyzers.chosen(), deployingCard);
        var randoActions =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CardSelectionEvaluator()
                    .evaluate(randoContext);
        var chosenActions =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CardSelectionEvaluator()
                    .evaluate(chosenContext);
        Map<String, EvaluationView> rando =
                new HashMap<>();
        for (var action : randoActions) {
            rando.put(action.getActionId(),
                    new EvaluationView(
                        action.getScore(),
                        action.isHardVetoed(),
                        action.getVetoReason(),
                        List.copyOf(
                            action.getReasoning())));
        }
        Map<String, EvaluationView> chosen =
                new HashMap<>();
        for (var action : chosenActions) {
            chosen.put(action.getActionId(),
                    new EvaluationView(
                        action.getScore(),
                        action.isHardVetoed(),
                        action.getVetoReason(),
                        List.copyOf(
                            action.getReasoning())));
        }
        assertEquals(
                "Mirrored deploy-destination evaluators must match exactly",
                rando, chosen);
        return rando;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer analyzer,
                    PhysicalCardImpl deployingCard) {
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
        context.setExtra(
                ObjectiveAnalyzer
                    .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                deployingCard.getPermanentCardId());
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer analyzer,
                    PhysicalCardImpl deployingCard) {
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
        context.setExtra(
                ObjectiveAnalyzer
                    .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                deployingCard.getPermanentCardId());
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
