package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.CancelForceIconsModifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Gameplay-facing proof for the Local Uprising operative twins. */
public class LocalUprisingTwinsObjectiveBehaviorTest {

    private static final StartingSetup LOCAL_UPRISING_OPEN_SYSTEM_CHOICE =
            new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_137");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            if (scn.LSDecisionAvailable("On which side")) {
                scn.LSChoose("Left");
            }
        }
    };

    private static final StartingSetup LOCAL_UPRISING = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_137");
                put("system", "6_87");
                put("desert", "7_119");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("Subjugated planet")
                        && scn.LSHasCardChoiceAvailable(
                            scn.GetLSCard("system"))) {
                    scn.LSChooseCard(scn.GetLSCard("system"));
                } else if (scn.LSDecisionAvailable("site to deploy")
                        && scn.LSHasCardChoiceAvailable(
                            scn.GetLSCard("desert"))) {
                    scn.LSChooseCard(scn.GetLSCard("desert"));
                }
            }
        }
    };

    private static final StartingSetup
            IMPERIAL_OCCUPATION_OPEN_SYSTEM_CHOICE = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_298");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
            }
        }
    };

    private static final StartingSetup IMPERIAL_OCCUPATION =
            new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_298");
                put("system", "1_282");
                put("desert", "7_281");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("Renegade planet")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("system"))) {
                    scn.DSChooseCard(scn.GetDSCard("system"));
                } else if (scn.DSDecisionAvailable("site to deploy")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("desert"))) {
                    scn.DSChooseCard(scn.GetDSCard("desert"));
                }
            }
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("forest", "7_121");
                    put("jungle", "7_122");
                    put("farm", "7_120");
                    put("farm2", "7_120");
                    put("op1", "7_47");
                    put("op2", "7_47");
                    put("op3", "7_47");
                    put("wrongOp", "7_2");
                    put("buddy1", "1_28");
                    put("buddy2", "1_28");
                    put("buddy3", "1_28");
                    put("ability3Companion", "1_17");
                    put("ability4Companion", "1_19");
                    put("battleDistractor", "1_28");
                    put("conditionalCompanion", "215_20");
                    put("lossFodder", "1_153");
                    put("forfeitFodder", "1_148");
                    put("vehicleCompanion", "7_156");
                    put("emptyVehicle", "3_66");
                    put("spaceDistractor", "1_146");
                    put("tatooine", "1_127");
                    put("dagobah", "4_84");
                }},
                new HashMap<>() {{
                    put("stormtrooper", "1_194");
                    put("armedOpponent", "109_11");
                }},
                24,
                24,
                LOCAL_UPRISING,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private VirtualTableScenario ioScenario() {
        return new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("forest", "7_284");
                    put("jungle", "7_285");
                    put("op1", "7_174");
                    put("op2", "7_174");
                    put("op3", "7_174");
                    put("buddy1", "1_194");
                    put("buddy2", "1_194");
                    put("buddy3", "1_194");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                IMPERIAL_OCCUPATION,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(VirtualTableScenario scn) {
            var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
            var chosen = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideLightBoth(VirtualTableScenario scn) {
            var decision = scn.GetAwaitingDecision(VirtualTableScenario.LS);
            assertNotNull("Expected a Light Side decision in phase "
                    + scn.gameState().getCurrentPhase(), decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS, decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS, decision, scn.gameState());
            assertEquals("Rando/Chosen parity for: " + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }

        private String decideDarkBoth(VirtualTableScenario scn) {
            var decision = scn.GetAwaitingDecision(VirtualTableScenario.DS);
            assertNotNull("Expected a Dark Side decision in phase "
                    + scn.gameState().getCurrentPhase(), decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.DS, decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.DS, decision, scn.gameState());
            assertEquals("Rando/Chosen parity for: " + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private String decideLightBoth(VirtualTableScenario scn) {
        return PublicBots.forGame(scn).decideLightBoth(scn);
    }

    private Object buildPrivateEvaluatorContext(
            Object ai, Class<?> aiType,
            VirtualTableScenario scn,
            String playerId) throws Exception {
        var builder = aiType.getDeclaredMethod(
                "buildEvaluatorContext",
                String.class,
                com.gempukku.swccgo.logic.decisions.AwaitingDecision.class,
                com.gempukku.swccgo.game.state.GameState.class,
                boolean.class);
        builder.setAccessible(true);
        return builder.invoke(
                ai, playerId,
                scn.GetAwaitingDecision(playerId),
                scn.gameState(), false);
    }

    private String chooseCardBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer,
            String prompt, List<PhysicalCard> cards) {
        List<String> cardIds = cards.stream()
                .map(card -> Integer.toString(card.getCardId()))
                .toList();
        List<String> blueprints = cards.stream()
                .map(card -> card.getBlueprintId(true)).toList();
        List<String> titles = cards.stream()
                .map(PhysicalCard::getTitle).toList();

        var randoContext = new com.gempukku.swccgo.ai.models.rando
                .evaluators.DecisionContext(
                    scn.gameState(), VirtualTableScenario.LS,
                    "CARD_SELECTION", prompt,
                    "operative-objective-loss", Phase.BATTLE);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.LIGHT);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setCardIds(cardIds);
        randoContext.setBlueprints(blueprints);
        randoContext.setTestingTexts(titles);
        randoContext.setSelectable(cards.stream()
                .map(ignored -> true).toList());
        randoContext.setNoPass(true);
        randoContext.setMin(1);
        randoContext.setMax(1);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DecisionContext(
                    scn.gameState(), VirtualTableScenario.LS,
                    "CARD_SELECTION", prompt,
                    "operative-objective-loss", Phase.BATTLE);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.LIGHT);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setCardIds(cardIds);
        chosenContext.setBlueprints(blueprints);
        chosenContext.setTestingTexts(titles);
        chosenContext.setSelectable(cards.stream()
                .map(ignored -> true).toList());
        chosenContext.setNoPass(true);
        chosenContext.setMin(1);
        chosenContext.setMax(1);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CombinedEvaluator().evaluateDecision(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        assertEquals(rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        return rando.getActionId();
    }

    @Test
    public void bothPublicBotsChooseThePlanetSupportedByTheirOperatives() {
        var scn = new VirtualTableScenario(
                new HashMap<>() {{
                    put("tibrin", "6_87");
                    put("dantooine", "1_122");
                    put("desert", "7_119");
                    put("forest", "7_121");
                    put("jungle", "7_122");
                    put("op1", "7_47");
                    put("op2", "7_47");
                    put("op3", "7_47");
                }},
                new HashMap<>(),
                24,
                24,
                LOCAL_UPRISING_OPEN_SYSTEM_CHOICE,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);

        scn.StartGame(false);
        assertTrue(scn.LSDecisionAvailable("Subjugated planet"));
        var setupAnalyzer = randoAnalyzer(scn);
        assertEquals(3,
                setupAnalyzer.countMatchingOperativesAvailableForSetupSystem(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("tibrin").getBlueprint()));
        assertEquals(0,
                setupAnalyzer.countMatchingOperativesAvailableForSetupSystem(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("dantooine").getBlueprint()));
        scn.LSDecided(decideLightBoth(scn));
        if (scn.LSDecisionAvailable("On which side")) {
            scn.LSChoose("Left");
            scn.PassAllResponses();
        }
        assertEquals(Title.Tibrin,
                scn.gameState().getSubjugatedPlanet());
        assertTrue("Expected setup-site choice, got: "
                    + scn.GetCurrentDecision().getText(),
                scn.LSDecisionAvailable("site to deploy"));
        List<String> offeredIds = scn.LSGetCardChoices();
        List<String> offeredBlueprints = scn.LSGetBPChoices();
        String selectedSite = decideLightBoth(scn);
        int selectedIndex = offeredIds.indexOf(selectedSite);
        assertTrue("Public setup choice must identify an offered site",
                selectedIndex >= 0
                    && selectedIndex < offeredBlueprints.size());
        assertTrue("Public setup choice must project to a legal battleground",
                setupAnalyzer.isCountedOperativeSetupSiteRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    offeredBlueprints.get(selectedIndex)));
        scn.LSDecided(selectedSite);
    }

    @Test
    public void darkPublicBotsChooseDantooineAndAProjectedSetupSite() {
        var scn = new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("dantooine", "1_282");
                    put("tatooine", "1_289");
                    put("desert", "7_281");
                    put("forest", "7_284");
                    put("jungle", "7_285");
                    put("op1", "7_174");
                    put("op2", "7_174");
                    put("op3", "7_174");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                IMPERIAL_OCCUPATION_OPEN_SYSTEM_CHOICE,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);

        scn.StartGame(false);
        assertTrue(scn.DSDecisionAvailable("Renegade planet"));
        var setupAnalyzer = ioRandoAnalyzer(scn);
        assertEquals(3,
                setupAnalyzer.countMatchingOperativesAvailableForSetupSystem(
                    scn.game(), VirtualTableScenario.DS,
                    scn.GetDSCard("dantooine").getBlueprint()));
        assertEquals(0,
                setupAnalyzer.countMatchingOperativesAvailableForSetupSystem(
                    scn.game(), VirtualTableScenario.DS,
                    scn.GetDSCard("tatooine").getBlueprint()));
        var bots = PublicBots.forGame(scn);
        scn.DSDecided(bots.decideDarkBoth(scn));
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Left");
            scn.PassAllResponses();
        }
        assertEquals(Title.Dantooine,
                scn.gameState().getRenegadePlanet());
        assertTrue("Expected Dark setup-site choice, got: "
                    + scn.GetCurrentDecision().getText(),
                scn.DSDecisionAvailable("site to deploy"));
        List<String> offeredIds = scn.DSGetCardChoices();
        List<String> offeredBlueprints = scn.DSGetBPChoices();
        String selectedSite = bots.decideDarkBoth(scn);
        int selectedIndex = offeredIds.indexOf(selectedSite);
        assertTrue("Dark public setup must identify an offered site",
                selectedIndex >= 0
                    && selectedIndex < offeredBlueprints.size());
        assertTrue("Dark public setup must choose a projected battleground",
                setupAnalyzer.isCountedOperativeSetupSiteRouteCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    offeredBlueprints.get(selectedIndex)));
        scn.DSDecided(selectedSite);
    }

    @Test
    public void publicBotsStartAndCompleteTheNativeSitePull() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var operative = scn.GetLSCard("op1");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");

        scn.StartGame();
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToBottomOfLSReserveDeck(forest, jungle);
        keepOnlyLightHandCards(scn);
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.DEPLOY);

        var bots = PublicBots.forGame(scn);
        Set<Integer> pullSourcesUsed = new HashSet<>();
        for (int pull = 0; pull < 2; pull++) {
            if (scn.AwaitingDSDeployPhaseActions()) {
                scn.DSPass();
            }
            String objectivePull = scn.GetCardActionId(
                    VirtualTableScenario.LS, objective,
                    "Deploy site from Reserve Deck");
            String operativePull = scn.GetCardActionId(
                    VirtualTableScenario.LS, operative,
                    "Deploy site from Reserve Deck");
            assertTrue("One native pull source must remain unused",
                    objectivePull != null || operativePull != null);

            String selectedPull = bots.decideLightBoth(scn);
            if (selectedPull.equals(objectivePull)) {
                pullSourcesUsed.add(objective.getPermanentCardId());
            } else if (selectedPull.equals(operativePull)) {
                pullSourcesUsed.add(operative.getPermanentCardId());
            } else {
                throw new AssertionError(
                        "The public bot abandoned the two-source fast pull chain");
            }
            scn.LSDecided(selectedPull);

            String selectedSite = bots.decideLightBoth(scn);
            scn.LSDecided(selectedSite);
            scn.PassAllResponses();
            if (scn.LSDecisionAvailable("next to (or convert)")) {
                scn.LSDecided(scn.LSGetCardChoices().getFirst());
                scn.PassAllResponses();
            }
            if (scn.LSDecisionAvailable("On which side")) {
                scn.LSChoose("Left");
                scn.PassAllResponses();
            }
        }
        assertEquals("Objective and operative must each supply one site",
                Set.of(objective.getPermanentCardId(),
                    operative.getPermanentCardId()),
                pullSourcesUsed);
        var analyzer = randoAnalyzer(scn);
        long routeSites = scn.gameState().getLocationsInOrder().stream()
                .filter(location -> analyzer
                    .isCountedOperativeFormationLocation(
                        scn.game(), VirtualTableScenario.LS, location))
                .count();
        assertEquals("Both public pulls reach the three-site flip geography",
                3L, routeSites);
    }

    @Test
    public void darkPublicBotsUseBothNativeSitePullSources()
            throws Exception {
        var scn = ioScenario();
        var objective = scn.GetDSCard("objective");
        var operative = scn.GetDSCard("op1");
        var desert = scn.GetDSCard("desert");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");

        scn.StartGame();
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToBottomOfDSReserveDeck(forest, jungle);
        keepOnlyDarkHandCards(scn);
        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.DEPLOY);

        var bots = PublicBots.forGame(scn);
        Set<Integer> pullSourcesUsed = new HashSet<>();
        for (int pull = 0; pull < 2; pull++) {
            if (scn.AwaitingLSDeployPhaseActions()) {
                scn.LSPass();
            }
            String objectivePull = scn.GetCardActionId(
                    VirtualTableScenario.DS, objective,
                    "Deploy site from Reserve Deck");
            String operativePull = scn.GetCardActionId(
                    VirtualTableScenario.DS, operative,
                    "Deploy site from Reserve Deck");
            assertTrue("One Dark native pull source must remain unused",
                    objectivePull != null || operativePull != null);

            String selectedPull = bots.decideDarkBoth(scn);
            if (selectedPull.equals(objectivePull)) {
                pullSourcesUsed.add(objective.getPermanentCardId());
            } else if (selectedPull.equals(operativePull)) {
                pullSourcesUsed.add(operative.getPermanentCardId());
            } else {
                var failureDecision = scn.GetAwaitingDecision(
                        VirtualTableScenario.DS);
                var failureContext =
                    (com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext) buildPrivateEvaluatorContext(
                            bots.rando(),
                            com.gempukku.swccgo.ai.models.rando
                                .RandoCalAi.class,
                            scn, VirtualTableScenario.DS);
                var deployOutcome = new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DeployEvaluator().evaluate(failureContext)
                    .stream().filter(action -> operativePull.equals(
                        action.getActionId())).findFirst().orElse(null);
                var textOutcome = new com.gempukku.swccgo.ai.models.rando
                    .evaluators.ActionTextEvaluator().evaluate(failureContext)
                    .stream().filter(action -> operativePull.equals(
                        action.getActionId())).findFirst().orElse(null);
                throw new AssertionError(
                        "The Dark public bot abandoned the two-source pull chain: "
                            + selectedPull + " objective=" + objectivePull
                            + " operative=" + operativePull
                            + " phase=" + scn.gameState().getCurrentPhase()
                            + " text=" + (failureDecision != null
                                ? failureDecision.getText() : "null")
                            + " actions=" + (failureDecision != null
                                ? java.util.Arrays.toString(
                                    failureDecision.getDecisionParameters()
                                        .get("actionText")) : "null")
                            + " forest=" + forest.getZone()
                            + " jungle=" + jungle.getZone()
                            + " routeCandidate=" + ioRandoAnalyzer(scn)
                                .hasObjectiveLocationRouteCandidateInReserve(
                                    scn.game(), VirtualTableScenario.DS,
                                    operative)
                            + " deployOutcome=" + (deployOutcome != null
                                ? deployOutcome.getVetoReason() + "/"
                                    + deployOutcome.getReasoningString()
                                    + "/" + deployOutcome.getScore()
                                : "null")
                            + " textOutcome=" + (textOutcome != null
                                ? textOutcome.getVetoReason() + "/"
                                    + textOutcome.getReasoningString()
                                    + "/" + textOutcome.getScore()
                                : "null"));
            }
            scn.DSDecided(selectedPull);

            String selectedSite = bots.decideDarkBoth(scn);
            scn.DSDecided(selectedSite);
            scn.PassAllResponses();
            if (scn.DSDecisionAvailable("next to (or convert)")) {
                scn.DSDecided(scn.DSGetCardChoices().getFirst());
                scn.PassAllResponses();
            }
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
                scn.PassAllResponses();
            }
        }
        assertEquals("Objective and Dantooine Operative each supply one site",
                Set.of(objective.getPermanentCardId(),
                    operative.getPermanentCardId()),
                pullSourcesUsed);
        var analyzer = ioRandoAnalyzer(scn);
        long routeSites = scn.gameState().getLocationsInOrder().stream()
                .filter(location -> analyzer
                    .isCountedOperativeFormationLocation(
                        scn.game(), VirtualTableScenario.DS, location))
                .count();
        assertEquals(3L, routeSites);
    }

    @Test
    public void darkPublicBotsContinueWithTheOperativeAfterObjectivePullsFirst() {
        var scn = ioScenario();
        var objective = scn.GetDSCard("objective");
        var operative = scn.GetDSCard("op1");
        var desert = scn.GetDSCard("desert");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");

        scn.StartGame();
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToBottomOfDSReserveDeck(forest, jungle);
        keepOnlyDarkHandCards(scn);
        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.DEPLOY);
        if (scn.AwaitingLSDeployPhaseActions()) scn.LSPass();

        String objectivePull = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy site from Reserve Deck");
        assertNotNull(objectivePull);
        scn.DSDecided(objectivePull);
        assertTrue(scn.DSHasCardChoiceAvailable(forest));
        scn.DSChooseCard(forest);
        scn.PassAllResponses();
        if (scn.DSDecisionAvailable("next to (or convert)")) {
            scn.DSDecided(scn.DSGetCardChoices().getFirst());
            scn.PassAllResponses();
        }
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Left");
            scn.PassAllResponses();
        }
        if (scn.AwaitingLSDeployPhaseActions()) scn.LSPass();

        String operativePull = scn.GetCardActionId(
                VirtualTableScenario.DS, operative,
                "Deploy site from Reserve Deck");
        assertNotNull(operativePull);
        assertTrue(ioRandoAnalyzer(scn)
                .hasObjectiveLocationRouteCandidateInReserve(
                    scn.game(), VirtualTableScenario.DS,
                    operative));
        assertEquals("The validated operative route must dominate the stale "
                        + "'site to Dantooine' text-parser false negative",
                operativePull,
                PublicBots.forGame(scn).decideDarkBoth(scn));
    }

    @Test
    public void publicBotsUseAnOperativePullToBufferTheTwoSiteBackHold() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var farm = scn.GetLSCard("farm");
        var operative = scn.GetLSCard("op1");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, operative, scn.GetLSCard("buddy1"));
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();

        scn.MoveOutOfPlay(scn.GetLSCard("op3"));
        scn.MoveOutOfPlay(scn.GetLSCard("buddy3"));
        scn.MoveOutOfPlay(jungle);
        scn.MoveOutOfPlay(scn.GetLSCard("farm2"));
        scn.MoveCardsToBottomOfLSReserveDeck(farm);
        assertTrue("Two occupied sites keep Liberation on its back",
                objective.isFlipped());

        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);
        assertTrue(rando.usesObjectiveLocationPullSequence());
        assertFalse("The flipped Objective has no site action; only an "
                        + "operative may supply the back-side buffer pull",
                rando.isCountedOperativeSiteRouteSource(
                    scn.game(), VirtualTableScenario.LS, objective));
        assertTrue(rando.isCountedOperativeSiteRouteAction(
                scn.game(), VirtualTableScenario.LS, operative,
                "Deploy site from Reserve Deck"));
        assertTrue(chosen.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.LS, operative, farm));

        String operativePull = scn.GetCardActionId(
                VirtualTableScenario.LS, operative,
                "Deploy site from Reserve Deck");
        assertNotNull(operativePull);
        var bots = PublicBots.forGame(scn);
        assertEquals(operativePull, bots.decideLightBoth(scn));
        scn.LSDecided(operativePull);
        var siteDecision = scn.GetAwaitingDecision(VirtualTableScenario.LS);
        String selectedSite = bots.decideLightBoth(scn);
        List<String> offeredIds = List.of(
                siteDecision.getDecisionParameters().get("cardId"));
        int selectedIndex = offeredIds.indexOf(selectedSite);
        assertTrue(selectedIndex >= 0);
        assertEquals(farm.getBlueprintId(true),
                siteDecision.getDecisionParameters()
                    .get("blueprintId")[selectedIndex]);
    }

    @Test
    public void realForceLossSpendsFodderBeforeTheFinalPair() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var finalOperative = scn.GetLSCard("op3");
        var finalCompanion = scn.GetLSCard("buddy3");
        var fodder = scn.GetLSCard("lossFodder");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op1"),
                scn.GetLSCard("buddy1"));
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLSHand(
                finalOperative, finalCompanion, fodder);
        keepOnlyLightHandCards(
                scn, finalOperative, finalCompanion, fodder);
        scn.MoveCardsToLocation(
                scn.GetDSStartingLocation(),
                scn.GetDSCard("stormtrooper"));

        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.DSForceDrainAt(scn.GetDSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("Choose Force to lose"));
        String loss = decideLightBoth(scn);
        assertFalse(Integer.toString(finalOperative.getCardId())
                .equals(loss));
        assertFalse(Integer.toString(finalCompanion.getCardId())
                .equals(loss));
        scn.LSDecided(loss);
        scn.PassAllResponses();
        assertEquals(Zone.HAND, finalOperative.getZone());
        assertEquals(Zone.HAND, finalCompanion.getZone());
    }

    @Test
    public void publicBattleContestsTheSafeBlockedFlipSite() {
        var scn = scenario();
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("ability4Companion");
        var stormtrooper = scn.GetDSCard("stormtrooper");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                forest, operative, companion, stormtrooper);
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.BATTLE);

        String battleAction = scn.GetCardActionId(
                VirtualTableScenario.LS, forest,
                "Initiate battle");
        assertNotNull(battleAction);
        assertEquals(battleAction, decideLightBoth(scn));
    }

    @Test
    public void publicBotsPayForAllSixBodiesAndTriggerTheNativeFlip() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var op1 = scn.GetLSCard("op1");
        var op2 = scn.GetLSCard("op2");
        var op3 = scn.GetLSCard("op3");
        var buddy1 = scn.GetLSCard("buddy1");
        var buddy2 = scn.GetLSCard("buddy2");
        var buddy3 = scn.GetLSCard("buddy3");
        List<PhysicalCard> packageCards = List.of(
                op1, op2, op3, buddy1, buddy2, buddy3);

        scn.MoveCardsToLSHand(
                op1, op2, op3, buddy1, buddy2, buddy3);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        keepOnlyLightHandCards(
                scn, op1, op2, op3, buddy1, buddy2, buddy3);
        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 6) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals("The public chain starts with the exact six-Force budget",
                6, scn.GetLSForcePileCount());
        var bots = PublicBots.forGame(scn);

        int paidDeploys = 0;
        while (!objective.isFlipped() && paidDeploys < 6) {
            if (scn.AwaitingDSDeployPhaseActions()) {
                scn.DSPass();
            }
            assertNotNull("No deploy decision after " + paidDeploys
                            + " paid deployments; Force="
                            + scn.GetLSForcePileCount() + "; cards="
                            + packageCards.stream()
                                .map(card -> card.getTitle() + "@" + card.getZone())
                                .collect(Collectors.joining(", "))
                            + "; current="
                            + (scn.GetCurrentDecision() != null
                                ? scn.GetCurrentDecision().getText()
                                : "none")
                            + "; decider=" + scn.GetDecidingPlayer(),
                    scn.GetAwaitingDecision(VirtualTableScenario.LS));
            String deployAction = bots.decideLightBoth(scn);
            var parent = scn.GetAwaitingDecision(VirtualTableScenario.LS);
            List<String> actionIds = List.of(
                    parent.getDecisionParameters().get("actionId"));
            int actionIndex = actionIds.indexOf(deployAction);
            assertTrue("The public bot must select a package deployment",
                    actionIndex >= 0);
            String[] cardIds = parent.getDecisionParameters().get("cardId");
            assertNotNull(cardIds);
            PhysicalCard selectedCard = scn.gameState().findCardById(
                    Integer.parseInt(cardIds[actionIndex]));
            assertTrue(packageCards.contains(selectedCard));
            String actionText = parent.getDecisionParameters()
                    .get("actionText")[actionIndex];

            scn.LSDecided(deployAction);
            var child = scn.GetAwaitingDecision(VirtualTableScenario.LS);
            assertNotNull(child);
            String destination = bots.decideLightBoth(scn);
            scn.LSDecided(destination);
            scn.PassAllResponses();
            assertFalse("Selected package action did not deploy; action="
                            + actionText + "; child=" + child.getText()
                            + "; response=" + destination,
                    selectedCard.getZone() == Zone.HAND);
            paidDeploys++;
        }

        assertEquals(6, paidDeploys);
        String deploymentMap = packageCards.stream()
                .map(card -> card.getTitle() + "@"
                    + (scn.game().getModifiersQuerying()
                        .getLocationThatCardIsAt(
                            scn.gameState(), card) != null
                                ? scn.game().getModifiersQuerying()
                                    .getLocationThatCardIsAt(
                                        scn.gameState(), card).getTitle()
                                : card.getZone()))
                .collect(Collectors.joining(", "));
        assertTrue("The real six-body deploy chain must fire card Java's flip; "
                        + deploymentMap,
                objective.isFlipped());
        assertEquals(0, packageCards.stream()
                .filter(card -> card.getZone() == Zone.HAND).count());
        assertEquals("All six one-Force deployments must consume the budget",
                0, scn.GetLSForcePileCount());
    }

    @Test
    public void darkPublicBotsFundAllSixBodiesAndTriggerTheNativeFlip() {
        var scn = ioScenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var op1 = scn.GetDSCard("op1");
        var op2 = scn.GetDSCard("op2");
        var op3 = scn.GetDSCard("op3");
        var buddy1 = scn.GetDSCard("buddy1");
        var buddy2 = scn.GetDSCard("buddy2");
        var buddy3 = scn.GetDSCard("buddy3");
        List<PhysicalCard> packageCards = List.of(
                op1, op2, op3, buddy1, buddy2, buddy3);

        scn.MoveCardsToDSHand(
                op1, op2, op3, buddy1, buddy2, buddy3);
        scn.StartGame();
        moveSiteToDantooine(scn, forest);
        moveSiteToDantooine(scn, jungle);
        keepOnlyDarkHandCards(
                scn, op1, op2, op3, buddy1, buddy2, buddy3);
        scn.DSActivateForceCheat(6);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 6) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        assertEquals("The Dark public chain starts with its exact budget",
                6, scn.GetDSForcePileCount());
        assertEquals(6,
                ioRandoAnalyzer(scn)
                    .getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));
        var bots = PublicBots.forGame(scn);

        int paidDeploys = 0;
        while (!objective.isFlipped() && paidDeploys < 6) {
            if (scn.AwaitingLSDeployPhaseActions()) {
                scn.LSPass();
            }
            assertNotNull("No Dark deploy decision after " + paidDeploys
                            + " paid deployments; Force="
                            + scn.GetDSForcePileCount() + "; cards="
                            + packageCards.stream()
                                .map(card -> card.getTitle() + "@" + card.getZone())
                                .collect(Collectors.joining(", "))
                            + "; current="
                            + (scn.GetCurrentDecision() != null
                                ? scn.GetCurrentDecision().getText()
                                : "none")
                            + "; decider=" + scn.GetDecidingPlayer(),
                    scn.GetAwaitingDecision(VirtualTableScenario.DS));
            String deployAction = bots.decideDarkBoth(scn);
            var parent = scn.GetAwaitingDecision(VirtualTableScenario.DS);
            List<String> actionIds = List.of(
                    parent.getDecisionParameters().get("actionId"));
            int actionIndex = actionIds.indexOf(deployAction);
            assertTrue("The Dark public bot must select a package deployment",
                    actionIndex >= 0);
            String[] cardIds = parent.getDecisionParameters().get("cardId");
            assertNotNull(cardIds);
            PhysicalCard selectedCard = scn.gameState().findCardById(
                    Integer.parseInt(cardIds[actionIndex]));
            assertTrue(packageCards.contains(selectedCard));

            scn.DSDecided(deployAction);
            String destination = bots.decideDarkBoth(scn);
            scn.DSDecided(destination);
            scn.PassAllResponses();
            assertFalse("Selected Dark package action did not deploy",
                    selectedCard.getZone() == Zone.HAND);
            paidDeploys++;
        }

        assertEquals(6, paidDeploys);
        assertTrue("The real Dark deploy chain must fire 7_298 card Java",
                objective.isFlipped());
        assertEquals(0, packageCards.stream()
                .filter(card -> card.getZone() == Zone.HAND).count());
        assertEquals("Six one-Force bodies must consume the exact budget",
                0, scn.GetDSForcePileCount());
    }

    @Test
    public void publicMovementCompletesARealPairAndRejectsAnEmptySite() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToLocation(forest, companion);
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);
        assertTrue(rando.advancesPreFlipActorAtRuntimeLocation(
                scn.game(), VirtualTableScenario.LS,
                operative, forest));
        assertFalse(rando.qualifiesPreFlipRuntimeActorAtLocation(
                scn.game(), VirtualTableScenario.LS,
                operative, jungle));
        assertEquals(
                rando.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, forest),
                chosen.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, forest));

        scn.LSActivateForceCheat(4);
        if (scn.gameState().getCurrentPhase() != Phase.MOVE) {
            scn.SkipToLSTurn(Phase.MOVE);
        }
        assertTrue(scn.LSCardActionAvailable(
                operative, "Move using landspeed"));
        scn.LSUseCardAction(operative, "Move using landspeed");
        scn.LSDecided(decideLightBoth(scn));
        scn.PassAllResponses();
        assertAtLocation(forest, operative, companion);
    }

    @Test
    public void publicBotPassesADeployDistractionToReserveTheProgressMove() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");
        var distractor = scn.GetLSCard("spaceDistractor");

        scn.MoveCardsToLSHand(distractor);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToLocation(forest, companion);
        keepOnlyLightHandCards(scn, distractor);
        scn.LSActivateForceCheat(2);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 2) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
        var bots = PublicBots.forGame(scn);

        assertTrue(scn.LSCardActionAvailable(distractor, "Deploy"));
        String distraction = scn.GetCardActionId(
                VirtualTableScenario.LS, distractor, "Deploy");
        String deployChoice = bots.decideLightBoth(scn);
        assertFalse("The two-Force ship may not consume the one-Force move",
                distraction.equals(deployChoice));
        scn.LSDecided(deployChoice);
        scn.PassAllResponses();
        assertEquals(2, scn.GetLSForcePileCount());
    }

    @Test
    public void publicBotStartsAndCompletesANetProgressMove() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToLocation(forest, companion);
        keepOnlyLightHandCards(scn);
        scn.SkipToLSTurn(Phase.MOVE);
        while (scn.GetLSForcePileCount() > 1) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.GetLSForcePileCount() < 1) {
            scn.LSActivateForceCheat(1);
        }
        if (scn.AwaitingDSMovePhaseActions()) scn.DSPass();
        var bots = PublicBots.forGame(scn);
        var moveParent = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(moveParent);
        assertTrue("Expected the public move parent, got "
                        + moveParent.getDecisionType() + ": "
                        + moveParent.getText(),
                moveParent.getText().contains(
                    "Choose Move action or Pass"));
        String publicMove = bots.decideLightBoth(scn);
        PhysicalCard selectedMover =
                AiActionSourceProvenance.selectedActionSource(
                    moveParent, publicMove);
        assertTrue("Either half may make the net-progress join, got "
                        + (selectedMover != null
                            ? selectedMover.getTitle() : "null"),
                selectedMover != null
                    && (selectedMover.getPermanentCardId()
                            == operative.getPermanentCardId()
                        || selectedMover.getPermanentCardId()
                            == companion.getPermanentCardId()));
        scn.LSDecided(publicMove);
        scn.LSDecided(bots.decideLightBoth(scn));
        scn.PassAllResponses();
        PhysicalCard joinedLocation = scn.game().getModifiersQuerying()
                .getLocationThatCardIsPresentAt(
                    scn.gameState(), operative);
        assertEquals(joinedLocation,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), companion));
        assertTrue(randoAnalyzer(scn)
                .isCountedOperativeFormationCompleteAt(
                    scn.game(), VirtualTableScenario.LS,
                    joinedLocation));
        assertEquals(0, scn.GetLSForcePileCount());
    }

    @Test
    public void publicVehicleMovementCanSupplyTheRealControlHalf() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var vehicle = scn.GetLSCard("vehicleCompanion");
        var operative = scn.GetLSCard("op1");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(desert, vehicle);
        scn.MoveCardsToLocation(forest, operative);
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);
        assertTrue(rando.advancesPreFlipActorAtRuntimeLocation(
                scn.game(), VirtualTableScenario.LS,
                vehicle, forest));
        assertEquals(
                rando.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    vehicle, forest),
                chosen.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    vehicle, forest));

        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.MOVE);
        assertTrue(scn.LSCardActionAvailable(
                vehicle, "Move using landspeed"));
        scn.LSUseCardAction(vehicle, "Move using landspeed");
        scn.LSDecided(decideLightBoth(scn));
        scn.PassAllResponses();
        assertAtLocation(forest, vehicle, operative);
    }

    @Test
    public void movementDoesNotCallAFormationSwapNewProgress() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var originCompanion = scn.GetLSCard("buddy1");
        var destinationCompanion = scn.GetLSCard("buddy2");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, operative, originCompanion);
        scn.MoveCardsToLocation(forest, destinationCompanion);
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue("A lateral swap preserves one real formation",
                rando.qualifiesPreFlipRuntimeActorAtLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, forest));
        assertFalse("Breaking the origin makes the swap net-zero, not progress",
                rando.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, forest));
        assertFalse("An empty site is not a safe operative destination",
                rando.qualifiesPreFlipRuntimeActorAtLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, jungle));
        assertEquals(
                rando.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, forest),
                chosen.advancesPreFlipActorAtRuntimeLocation(
                    scn.game(), VirtualTableScenario.LS,
                    operative, forest));
    }

    @Test
    public void publicLossChoicesPreserveBothRequiredFormationHalves() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");
        var lossFodder = scn.GetLSCard("lossFodder");
        var forfeitFodder = scn.GetLSCard("forfeitFodder");

        scn.MoveCardsToLSHand(
                operative, companion, lossFodder, forfeitFodder);
        scn.StartGame();
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);
        assertEquals(Integer.toString(lossFodder.getCardId()),
                chooseCardBoth(
                    scn, rando, chosen,
                    "Choose Force to lose",
                    List.of(operative, companion, lossFodder)));

        scn.MoveCardsToLocation(
                desert, operative, companion, forfeitFodder);
        rando = randoAnalyzer(scn);
        chosen = chosenAnalyzer(scn);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    operative));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_BUDDY,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    companion));
        assertEquals(Integer.toString(forfeitFodder.getCardId()),
                chooseCardBoth(
                    scn, rando, chosen,
                    "Choose a card from battle to forfeit",
                    List.of(operative, companion, forfeitFodder)));
    }

    @Test
    public void permanentPilotVehicleCountsOnBoardButNotAsAHandSitePlan() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var vehicle = scn.GetLSCard("vehicleCompanion");
        var nonmatchingOperative = scn.GetLSCard("wrongOp");
        var spaceDistractor = scn.GetLSCard("spaceDistractor");
        var pulse = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(spaceDistractor, pulse);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op1"), vehicle);
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("op2"),
                nonmatchingOperative);
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue("A permanent pilot supplies the real control ability",
                rando.hasCountedOperativeCompanionAtLocation(
                    scn.game(), VirtualTableScenario.LS, desert));
        assertTrue(rando.isCountedOperativeFormationCompleteAt(
                scn.game(), VirtualTableScenario.LS, desert));
        assertTrue("A nonmatching operative retains its real ability here",
                rando.hasCountedOperativeCompanionAtLocation(
                    scn.game(), VirtualTableScenario.LS, forest));
        assertTrue(rando.isCountedOperativeFormationCompleteAt(
                scn.game(), VirtualTableScenario.LS, forest));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_BUDDY,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, vehicle));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, vehicle),
                chosen.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, vehicle));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_BUDDY,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    nonmatchingOperative));
        assertEquals("A space-only ship must not become site-package bait",
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                rando.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    spaceDistractor));

        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("The unchanged card trigger must accept vehicle presence",
                objective.isFlipped());
    }

    @Test
    public void matchingOperativePilotDoesNotMakeItsVehicleACompanion() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var operative = scn.GetLSCard("op1");
        var vehicle = scn.GetLSCard("emptyVehicle");

        scn.StartGame();
        scn.MoveCardsToLocation(desert, vehicle);
        scn.RemoveCardZone(operative);
        scn.gameState().attachCardInPilotCapacitySlot(
                operative, vehicle);
        scn.game().getModifiersEnvironment().addUntilEndOfGameModifier(
                new IconModifier(operative, operative, Icon.PILOT));
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue(com.gempukku.swccgo.filters.Filters.piloted.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(),
                vehicle));
        assertTrue("The attached matching operative remains the actor",
                rando.hasCountedOperativeActorAtLocation(
                    scn.game(), VirtualTableScenario.LS, desert));
        assertFalse("Its excluded pilot ability cannot make the vehicle "
                        + "the second control source",
                rando.hasCountedOperativeCompanionAtLocation(
                    scn.game(), VirtualTableScenario.LS, desert));
        assertFalse(rando.isCountedOperativeFormationCompleteAt(
                scn.game(), VirtualTableScenario.LS, desert));
        assertEquals(
                rando.hasCountedOperativeCompanionAtLocation(
                    scn.game(), VirtualTableScenario.LS, desert),
                chosen.hasCountedOperativeCompanionAtLocation(
                    scn.game(), VirtualTableScenario.LS, desert));
    }

    @Test
    public void matchingOperativeAbilityNeverFakesBattleDestinySafety() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var operative = scn.GetLSCard("op1");
        var ability3 = scn.GetLSCard("ability3Companion");
        var ability4 = scn.GetLSCard("ability4Companion");

        scn.MoveCardsToLSHand(operative, ability3, ability4);
        scn.StartGame();
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertEquals(3.0f,
                rando.getCountedOperativeProjectedBattleDestinyAbility(
                    scn.game(), VirtualTableScenario.LS, desert,
                    List.of(operative, ability3)), 0.0f);
        assertEquals(4.0f,
                rando.getCountedOperativeProjectedBattleDestinyAbility(
                    scn.game(), VirtualTableScenario.LS, desert,
                    List.of(operative, ability4)), 0.0f);
        assertEquals(
                rando.getCountedOperativeProjectedBattleDestinyAbility(
                    scn.game(), VirtualTableScenario.LS, desert,
                    List.of(operative, ability4)),
                chosen.getCountedOperativeProjectedBattleDestinyAbility(
                    scn.game(), VirtualTableScenario.LS, desert,
                    List.of(operative, ability4)), 0.0f);
    }

    private void moveSiteToTibrin(
            VirtualTableScenario scn, PhysicalCardImpl site) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, Title.Tibrin, null);
        assertFalse(placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    private void moveSiteToDantooine(
            VirtualTableScenario scn, PhysicalCardImpl site) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, Title.Dantooine, null);
        assertFalse(placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    private void keepOnlyLightHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        Set<PhysicalCard> protectedCards = Set.of(keep);
        List<PhysicalCardImpl> toReserve = new ArrayList<>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.LS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                toReserve.add(physical);
            }
        }
        for (PhysicalCardImpl card : toReserve) {
            scn.MoveCardsToBottomOfLSReserveDeck(card);
        }
    }

    private void keepOnlyDarkHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        Set<PhysicalCard> protectedCards = Set.of(keep);
        List<PhysicalCardImpl> toReserve = new ArrayList<>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                toReserve.add(physical);
            }
        }
        for (PhysicalCardImpl card : toReserve) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
            randoAnalyzer(VirtualTableScenario scn) {
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        return analyzer;
    }

    private com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
            chosenAnalyzer(VirtualTableScenario scn) {
        var analyzer = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        return analyzer;
    }

    private com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
            ioRandoAnalyzer(VirtualTableScenario scn) {
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        return analyzer;
    }

    @Test
    public void bothBotsRequireTheOperativeAndItsRealControlCompanion() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var op1 = scn.GetLSCard("op1");
        var op2 = scn.GetLSCard("op2");
        var wrongOp = scn.GetLSCard("wrongOp");
        var buddy1 = scn.GetLSCard("buddy1");
        var buddy2 = scn.GetLSCard("buddy2");

        scn.MoveCardsToLSHand(op1, wrongOp, buddy2);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(forest, buddy1);
        scn.MoveCardsToLocation(jungle, op2);

        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);
        assertTrue(rando.hasCountedOperativeFormationRule());
        assertTrue(chosen.hasCountedOperativeFormationRule());
        assertTrue("The runtime-chosen operative site is objective geography",
                rando.isObjectiveRelevantLocation(
                    forest, scn.game(), VirtualTableScenario.LS));
        assertTrue(chosen.isObjectiveRelevantLocation(
                forest, scn.game(), VirtualTableScenario.LS));

        assertFalse("A lone matching operative cannot project control",
                rando.advancesPreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.LS,
                    op1, desert));
        assertFalse(chosen.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.LS,
                op1, desert));
        assertTrue("The operative completes a companion-only site",
                rando.advancesPreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.LS,
                    op1, forest));
        assertTrue(chosen.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.LS,
                op1, forest));
        assertTrue("The non-operative companion completes an operative-only site",
                rando.advancesPreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.LS,
                    buddy2, jungle));
        assertTrue(chosen.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.LS,
                buddy2, jungle));
        assertFalse("A wrong-system operative is not a proactive package card",
                rando.advancesPreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.LS,
                    wrongOp, forest));
    }

    @Test
    public void objectiveAndOperativePullOnlyTheTwoMissingRouteSites() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var farm = scn.GetLSCard("farm");
        var operative = scn.GetLSCard("op1");

        scn.StartGame();
        scn.MoveCardsToLocation(desert, operative);
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue(rando.usesObjectiveLocationPullSequence());
        assertTrue(rando.isCountedOperativeSiteRouteAction(
                scn.game(), VirtualTableScenario.LS,
                objective, "Deploy site from Reserve Deck"));
        assertTrue(rando.isCountedOperativeSiteRouteAction(
                scn.game(), VirtualTableScenario.LS,
                operative, "Deploy site from Reserve Deck"));
        assertTrue(rando.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.LS,
                objective, forest));
        assertTrue(chosen.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.LS,
                operative, forest));

        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        assertFalse("A fourth site is geography, not flip progress",
                rando.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    objective, farm));
        assertFalse(rando.hasObjectiveLocationRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.LS, objective));
        assertFalse(chosen.isCountedOperativeSiteRouteAction(
                scn.game(), VirtualTableScenario.LS,
                operative, "Deploy site from Reserve Deck"));
    }

    @Test
    public void contestedThirdSiteKeepsPullOpenUntilThreeUsableSites() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var farm = scn.GetLSCard("farm");
        var farm2 = scn.GetLSCard("farm2");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                jungle, scn.GetDSCard("stormtrooper"));
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue("A contested incomplete third site must not strand the "
                        + "three-formation route",
                rando.isCountedOperativeSiteRouteAction(
                    scn.game(), VirtualTableScenario.LS,
                    objective, "Deploy site from Reserve Deck"));
        assertTrue(rando.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.LS,
                objective, farm));
        assertTrue(chosen.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.LS,
                objective, farm));

        moveSiteToTibrin(scn, farm);
        assertFalse("Three uncontested route sites are enough; a fifth is "
                        + "decorative geography",
                rando.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    objective, farm2));
        assertFalse(chosen.isCountedOperativeSiteRouteAction(
                scn.game(), VirtualTableScenario.LS,
                objective, "Deploy site from Reserve Deck"));
    }

    @Test
    public void operativePullSourceMayStandAtMatchingNonBattlegroundSite() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var operative = scn.GetLSCard("op1");
        var forest = scn.GetLSCard("forest");
        var farm = scn.GetLSCard("farm");

        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        scn.MoveCardsToLocation(forest, operative);
        scn.game().getModifiersEnvironment().addUntilEndOfGameModifier(
                new CancelForceIconsModifier(
                    objective,
                    com.gempukku.swccgo.filters.Filters.sameCardId(forest),
                    VirtualTableScenario.DS));
        assertFalse("The source site is deliberately not a battleground",
                com.gempukku.swccgo.filters.Filters.battleground.accepts(
                    scn.gameState(), scn.game().getModifiersQuerying(),
                    forest));
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue("The operative source law requires a matching site, not "
                        + "a battleground",
                rando.isCountedOperativeSiteRouteSource(
                    scn.game(), VirtualTableScenario.LS, operative));
        assertTrue(chosen.isCountedOperativeSiteRouteAction(
                scn.game(), VirtualTableScenario.LS,
                operative, "Deploy site from Reserve Deck"));
        assertTrue("The downloaded destination still must project as a "
                        + "qualifying battleground",
                rando.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    operative, farm));

        scn.LSActivateForceCheat(2);
        scn.SkipToLSTurn(Phase.DEPLOY);
        assertNotNull("Unchanged operative card Java must offer the real pull",
                scn.GetCardActionId(
                    VirtualTableScenario.LS, operative,
                    "Deploy site from Reserve Deck"));
    }

    @Test
    public void nativePullProjectsLegalityAndBattlegroundStatusAtChosenPlanet() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var jungle = scn.GetLSCard("jungle");
        var tatooine = scn.GetLSCard("tatooine");
        var dagobah = scn.GetLSCard("dagobah");

        scn.StartGame();
        scn.MoveLocationToTable(tatooine);
        scn.MoveLocationToTable(dagobah);

        scn.gameState().setSubjugatedPlanet(Title.Tatooine);
        var tatooineAnalyzer = randoAnalyzer(scn);
        assertTrue("Farm is a legal setup battleground at Tatooine",
                tatooineAnalyzer.isCountedOperativeSetupSiteRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("farm").getBlueprintId(true)));
        assertFalse("Jungle is not a legal setup site at Tatooine",
                tatooineAnalyzer.isCountedOperativeSetupSiteRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    jungle.getBlueprintId(true)));
        assertFalse("Jungle is barred from Tatooine by its card source",
                tatooineAnalyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    objective, jungle));

        scn.gameState().setSubjugatedPlanet(Title.Dagobah);
        var dagobahAnalyzer = randoAnalyzer(scn);
        assertTrue("Jungle may legally deploy to Dagobah",
                com.gempukku.swccgo.filters.Filters.deployableToSystem(
                    objective, Title.Dagobah, null, false, 0.0f)
                    .accepts(scn.gameState(),
                        scn.game().getModifiersQuerying(), jungle));
        assertFalse("Dagobah locations never count as battlegrounds",
                dagobahAnalyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    objective, jungle));
    }

    @Test
    public void bothPlannersFundAndDistributeThreeCompletePairs() {
        var scn = scenario();
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var op1 = scn.GetLSCard("op1");
        var op2 = scn.GetLSCard("op2");
        var op3 = scn.GetLSCard("op3");
        var buddy1 = scn.GetLSCard("buddy1");
        var buddy2 = scn.GetLSCard("buddy2");
        var buddy3 = scn.GetLSCard("buddy3");

        scn.MoveCardsToLSHand(
                op1, op2, op3, buddy1, buddy2, buddy3);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        keepOnlyLightHandCards(
                scn, op1, op2, op3, buddy1, buddy2, buddy3);
        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);

        var randoAnalyzer = randoAnalyzer(scn);
        var chosenAnalyzer = chosenAnalyzer(scn);
        assertEquals(6,
                randoAnalyzer.getCountedObjectivePresenceForceReserve(
                    scn.game(), VirtualTableScenario.LS, null));

        var randoPlanner = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhasePlanner();
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var chosenPlanner = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .DeployPhasePlanner();
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);

        var randoPlan = randoPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenPlan = chosenPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertNotNull(randoPlan);
        assertNotNull(chosenPlan);
        assertTrue(randoPlan.getReason().startsWith(
                "Objective counted-operative formations"));
        assertEquals(randoPlan.getReason(), chosenPlan.getReason());
        assertEquals(6, randoPlan.getInstructions().size());
        assertEquals(6, chosenPlan.getInstructions().size());
        assertEquals(6, randoPlan.getInstructions().stream()
                .mapToInt(i -> i.getDeployCost()).sum());

        Map<String, List<String>> randoBySite = randoPlan.getInstructions()
                .stream().collect(Collectors.groupingBy(
                    i -> i.getTargetLocationId(),
                    Collectors.mapping(
                        i -> i.getCardBlueprintId(), Collectors.toList())));
        Map<String, List<String>> chosenBySite = chosenPlan.getInstructions()
                .stream().collect(Collectors.groupingBy(
                    i -> i.getTargetLocationId(),
                    Collectors.mapping(
                        i -> i.getCardBlueprintId(), Collectors.toList())));
        assertEquals(randoBySite, chosenBySite);
        assertEquals(3, randoBySite.size());
        for (List<String> pair : randoBySite.values()) {
            assertEquals(2, pair.size());
            assertTrue(pair.contains("7_47"));
            assertTrue(pair.contains("1_28"));
        }
        assertEquals(6, randoPlan.getInstructions().stream()
                .map(i -> i.getCardPermanentCardId())
                .collect(Collectors.toCollection(HashSet::new)).size());
    }

    @Test
    public void countedPlannerRejectsACompanionThatEngineCannotDeploy() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var conditional = scn.GetLSCard("conditionalCompanion");

        scn.MoveCardsToLSHand(operative, conditional);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        keepOnlyLightHandCards(scn, operative, conditional);
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertFalse("TK-422 (V) requires Cell 2187 on table",
                com.gempukku.swccgo.filters.Filters.deployableToLocation(
                    conditional,
                    com.gempukku.swccgo.filters.Filters.sameCardId(jungle),
                    false, 0.0f).accepts(
                        scn.gameState(),
                        scn.game().getModifiersQuerying(), conditional));

        var randoPlanner = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhasePlanner();
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer(scn));
        var chosenPlanner = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .DeployPhasePlanner();
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer(scn));

        var randoPlan = randoPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenPlan = chosenPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertFalse("An illegal conditional companion cannot form the counted plan",
                randoPlan != null && randoPlan.getReason().startsWith(
                    "Objective counted-operative formations"));
        assertFalse("Chosen One must reject the same illegal pair",
                chosenPlan != null && chosenPlan.getReason().startsWith(
                    "Objective counted-operative formations"));
    }

    @Test
    public void contestedFormationCannotConsumeItsBattleForce() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("ability4Companion");

        scn.MoveCardsToLSHand(operative, companion);
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetLSCard("farm"));
        scn.MoveOutOfPlay(scn.GetLSCard("farm2"));
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        scn.MoveCardsToLocation(
                forest, scn.GetDSCard("stormtrooper"));
        keepOnlyLightHandCards(scn, operative, companion);
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 4) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }

        var randoPlanner = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhasePlanner();
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer(scn));
        var chosenPlanner = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .DeployPhasePlanner();
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer(scn));

        var randoPlan = randoPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenPlan = chosenPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        int randoFormationCards = randoPlan != null
                && randoPlan.getReason().startsWith(
                    "Objective counted-operative formations")
                    ? randoPlan.getInstructions().size() : 0;
        int chosenFormationCards = chosenPlan != null
                && chosenPlan.getReason().startsWith(
                    "Objective counted-operative formations")
                    ? chosenPlan.getInstructions().size() : 0;
        assertTrue("A four-Force pair may not strand the battle at zero",
                randoFormationCards < 2);
        assertEquals(randoFormationCards, chosenFormationCards);
    }

    @Test
    public void armedOpponentHitDiscountRejectsAnUnsafeCountedPair() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("ability4Companion");

        scn.MoveCardsToLSHand(operative, companion);
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetLSCard("farm"));
        scn.MoveOutOfPlay(scn.GetLSCard("farm2"));
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        scn.MoveCardsToLocation(
                forest, scn.GetDSCard("armedOpponent"));
        keepOnlyLightHandCards(scn, operative, companion);
        scn.LSActivateForceCheat(10);
        scn.SkipToLSTurn(Phase.DEPLOY);

        var randoPlanner = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhasePlanner();
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer(scn));
        var chosenPlanner = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .DeployPhasePlanner();
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer(scn));
        var randoPlan = randoPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenPlan = chosenPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        assertFalse("The permanent weapon can hit away the apparent power "
                        + "margin, so this pair is not a safe contact plan",
                randoPlan != null && randoPlan.getReason().startsWith(
                    "Objective counted-operative formations"));
        assertFalse(chosenPlan != null
                && chosenPlan.getReason().startsWith(
                    "Objective counted-operative formations"));
    }

    @Test
    public void publicBotsDeployTheContestedPairAndKeepOneForceToBattle() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("ability4Companion");
        var distractor = scn.GetLSCard("battleDistractor");
        var stormtrooper = scn.GetDSCard("stormtrooper");
        List<PhysicalCard> pair = List.of(operative, companion);

        scn.MoveCardsToLSHand(operative, companion);
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetLSCard("farm"));
        scn.MoveOutOfPlay(scn.GetLSCard("farm2"));
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        scn.MoveCardsToLocation(forest, stormtrooper);
        keepOnlyLightHandCards(scn, operative, companion);
        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 5) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertFalse("Three route sites must exhaust the objective site pull",
                randoAnalyzer(scn)
                    .hasObjectiveLocationRouteCandidateInReserve(
                        scn.game(), VirtualTableScenario.LS,
                        scn.GetLSCard("objective")));
        var initialDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        var scriptResult = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhaseScript().selectAllowedActions(
                    initialDecision, scn.gameState(), scn.game(),
                    VirtualTableScenario.LS, randoAnalyzer(scn));
        assertNotNull(scriptResult);
        String exhaustedPull = scn.GetCardActionId(
                VirtualTableScenario.LS,
                scn.GetLSCard("objective"),
                "Deploy site from Reserve Deck");
        assertFalse("The deploy script must exclude an exhausted site pull",
                scriptResult.allowedActionIds.contains(exhaustedPull));
        var contestedPlanner = new com.gempukku.swccgo.ai.models.rando
                .strategy.DeployPhasePlanner();
        contestedPlanner.setObjectiveAnalyzer(randoAnalyzer(scn));
        var contestedPlan = contestedPlanner.createPlan(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertNotNull(contestedPlan);
        assertEquals("Five Force must fund the four-Force pair plus battle; plan="
                        + contestedPlan.getReason(),
                2, contestedPlan.getInstructions().size());
        var bots = PublicBots.forGame(scn);

        for (int paidDeploys = 0; paidDeploys < 2; paidDeploys++) {
            if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
            var parent = scn.GetAwaitingDecision(VirtualTableScenario.LS);
            assertNotNull(parent);
            String deployAction = bots.decideLightBoth(scn);
            List<String> actionIds = List.of(
                    parent.getDecisionParameters().get("actionId"));
            int actionIndex = actionIds.indexOf(deployAction);
            assertTrue("Bot returned '" + deployAction
                            + "' outside offered actions "
                            + String.join(" | ", parent
                                .getDecisionParameters()
                                .get("actionText"))
                            + "; cardIds=" + String.join(",",
                                parent.getDecisionParameters()
                                    .get("cardId"))
                            + "; operative=" + operative.getCardId()
                            + "; companion=" + companion.getCardId(),
                    actionIndex >= 0);
            PhysicalCard selectedCard = scn.gameState().findCardById(
                    Integer.parseInt(parent.getDecisionParameters()
                        .get("cardId")[actionIndex]));
            assertTrue("Expected contested formation card, got "
                            + (selectedCard != null
                                ? selectedCard.getTitle() : "null")
                            + " from " + parent.getDecisionParameters()
                                .get("actionText")[actionIndex],
                    pair.contains(selectedCard));
            scn.LSDecided(deployAction);
            scn.LSDecided(bots.decideLightBoth(scn));
            if (paidDeploys == 1) {
                scn.MoveCardsToLSHand(distractor);
            }
            scn.PassAllResponses();
        }

        assertAtLocation(forest, operative, companion, stormtrooper);
        assertEquals(1, randoAnalyzer(scn)
                .getCountedOperativeBattleForceReserve(
                    scn.game(), VirtualTableScenario.LS));
        assertEquals(1, chosenAnalyzer(scn)
                .getCountedOperativeBattleForceReserve(
                    scn.game(), VirtualTableScenario.LS));
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
        String distractorDeploy = scn.GetCardActionId(
                VirtualTableScenario.LS, distractor, "Deploy");
        assertNotNull("The one-Force distraction must be a real offered deploy",
                distractorDeploy);
        String postPairChoice = bots.decideLightBoth(scn);
        assertFalse("The completed pair must not release the reserved battle "
                        + "Force to an unrelated deploy",
                distractorDeploy.equals(postPairChoice));
        scn.LSDecided(postPairChoice);
        scn.PassAllResponses();
        assertEquals("The contested package must retain battle Force",
                1, scn.GetLSForcePileCount());
        scn.SkipToLSTurn(Phase.BATTLE);
        String battleAction = scn.GetCardActionId(
                VirtualTableScenario.LS, forest, "Initiate battle");
        assertEquals(battleAction, bots.decideLightBoth(scn));
    }

    @Test
    public void netProgressLandspeedMoveReservesItsExactForce() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");
        var distractor = scn.GetLSCard("spaceDistractor");
        var replacementOperative = scn.GetLSCard("op2");

        scn.MoveCardsToLSHand(distractor, replacementOperative);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToLocation(forest, companion);
        scn.LSActivateForceCheat(1);
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue(rando.advancesPreFlipActorAtRuntimeLocation(
                scn.game(), VirtualTableScenario.LS,
                operative, forest));
        assertTrue(rando.isSafePreFlipRuntimeActorLandspeedDestination(
                scn.game(), VirtualTableScenario.LS,
                operative, forest));
        assertEquals(1,
                rando.getCountedOperativeFormationMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS, distractor));
        assertEquals(0,
                rando.getCountedOperativeFormationMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS,
                    replacementOperative));
        assertEquals(
                rando.getCountedOperativeFormationMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS, distractor),
                chosen.getCountedOperativeFormationMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS, distractor));
    }

    @Test
    public void modifiedDeployCostCannotSpendTheExactProgressMoveForce()
            throws Exception {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");
        var distractor = scn.GetLSCard("wrongOp");

        scn.MoveCardsToLSHand(distractor);
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetLSCard("farm"));
        scn.MoveOutOfPlay(scn.GetLSCard("farm2"));
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(desert, operative);
        scn.MoveCardsToLocation(forest, companion);
        keepOnlyLightHandCards(scn, distractor);
        scn.game().getModifiersEnvironment().addUntilEndOfGameModifier(
                new DeployCostModifier(
                    scn.GetLSCard("objective"),
                    com.gempukku.swccgo.filters.Filters.sameCardId(
                        distractor),
                    3));
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 4) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();

        assertEquals(1, randoAnalyzer(scn)
                .getCountedOperativeFormationMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS,
                    distractor));
        assertTrue(scn.LSCardActionAvailable(distractor, "Deploy"));
        String distractorAction = scn.GetCardActionId(
                VirtualTableScenario.LS, distractor, "Deploy");

        var randoAi = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        randoAi.setGame(scn.game());
        randoAi.decide(
                VirtualTableScenario.LS,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                scn.gameState());
        var randoContext = (com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext) buildPrivateEvaluatorContext(
                    randoAi,
                    com.gempukku.swccgo.ai.models.rando.RandoCalAi.class,
                    scn, VirtualTableScenario.LS);
        assertEquals(Integer.valueOf(4),
                com.gempukku.swccgo.ai.models.common.phase
                    .CaptureDeployBudgetFactsReader.actionPayment(
                        randoContext.getExtra(
                            com.gempukku.swccgo.ai.models.common.phase
                                .CaptureDeployBudgetFactsReader
                                .ACTION_PAYMENTS_EXTRA),
                        distractorAction));
        assertEquals(1, randoContext.getObjectiveAnalyzer()
                .getCountedOperativeFormationMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS,
                    distractor));
        var randoOutcome = new com.gempukku.swccgo.ai.models.rando.evaluators
                .DeployEvaluator().evaluate(randoContext).stream()
                .filter(action -> distractorAction.equals(
                    action.getActionId()))
                .findFirst().orElseThrow();

        var chosenAi = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        chosenAi.setGame(scn.game());
        chosenAi.decide(
                VirtualTableScenario.LS,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                scn.gameState());
        var chosenContext = (com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DecisionContext) buildPrivateEvaluatorContext(
                    chosenAi,
                    com.gempukku.swccgo.ai.models.chosenone
                        .TheChosenOneAi.class,
                    scn, VirtualTableScenario.LS);
        var chosenOutcome = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DeployEvaluator()
                .evaluate(chosenContext).stream()
                .filter(action -> distractorAction.equals(
                    action.getActionId()))
                .findFirst().orElseThrow();

        assertTrue("Expected exact-payment veto; texts="
                        + randoContext.getActionTexts()
                        + "; outcome=" + randoOutcome.getReasoningString()
                        + "; score=" + randoOutcome.getScore(),
                randoOutcome.isHardVetoed());
        assertEquals(
                "OBJECTIVE.COUNTED_OPERATIVE.MOVE_FORCE_RESERVE: "
                    + "preserve the exact net-progress landspeed payment",
                randoOutcome.getVetoReason());
        assertEquals(randoOutcome.getVetoReason(),
                chosenOutcome.getVetoReason());
    }

    @Test
    public void casualtyLogicProtectsBothRealHalvesOfAQualifiedSite() {
        var scn = scenario();
        var desert = scn.GetLSCard("desert");
        var operative = scn.GetLSCard("op1");
        var companion = scn.GetLSCard("buddy1");

        scn.StartGame();
        scn.MoveCardsToLocation(desert, operative, companion);
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, operative));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_BUDDY,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, companion));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, companion),
                chosen.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, companion));
    }

    @Test
    public void plainTwoSiteOccupationIsTheAuthoritativeBackHold() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op1"),
                scn.GetLSCard("buddy1"));
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetLSCard("op1"));
        scn.MoveOutOfPlay(scn.GetLSCard("op2"));
        scn.MoveOutOfPlay(scn.GetLSCard("op3"));
        scn.MoveOutOfPlay(scn.GetLSCard("buddy3"));
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        var desertRisk = rando.assessPostFlipLocationRisk(
                scn.game(), VirtualTableScenario.LS, desert);
        assertTrue(desertRisk.applies());
        assertTrue(desertRisk.inScope());
        assertTrue(desertRisk.criticalIfSelfControlLost());
        assertTrue(rando.wouldDepartureTriggerFlipBack(
                scn.game(), VirtualTableScenario.LS,
                scn.GetLSCard("buddy1")));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("buddy1")));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("buddy2")),
                chosen.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("buddy2")));
    }

    @Test
    public void contestedTwoSiteBackHoldStillProtectsItsLastOccupier() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var pulse = scn.GetLSFiller(4);
        var lastOccupier = scn.GetLSCard("buddy1");

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveSiteToTibrin(scn, forest);
        moveSiteToTibrin(scn, jungle);
        scn.MoveCardsToLocation(
                desert, scn.GetLSCard("op1"), lastOccupier);
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("op2"),
                scn.GetLSCard("buddy2"));
        scn.MoveCardsToLocation(
                jungle, scn.GetLSCard("op3"),
                scn.GetLSCard("buddy3"));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetLSCard("op1"));
        scn.MoveOutOfPlay(scn.GetLSCard("op2"));
        scn.MoveOutOfPlay(scn.GetLSCard("op3"));
        scn.MoveOutOfPlay(scn.GetLSCard("buddy3"));
        scn.MoveCardsToLocation(
                desert, scn.GetDSCard("stormtrooper"));
        var rando = randoAnalyzer(scn);
        var chosen = chosenAnalyzer(scn);

        assertTrue(rando.wouldDepartureTriggerFlipBack(
                scn.game(), VirtualTableScenario.LS, lastOccupier));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, lastOccupier));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, lastOccupier),
                chosen.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, lastOccupier));
    }
}
