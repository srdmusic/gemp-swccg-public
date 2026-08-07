package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.ai.models.common.phase.BattleDecisionPolicy;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.ArbitraryCardsSelectionDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Native engine contract for 208_57 / 208_57_BACK. */
public class IWantThatMapObjectiveEngineContractTest {
    private enum Route {
        TWO_SITES,
        SITE_AND_SYSTEM,
        STARKILLER_PULL
    }

    private static StartingSetup objectiveSetup(Route route) {
        return new StartingSetup() {
            @Override
            public HashMap<String, String> Cards() {
                return new HashMap<>() {{
                    put("objective", "208_57");
                    put("tuanul", "204_53");
                    put("secondBattleground",
                            route == Route.TWO_SITES
                                    ? "208_53"
                                    : route == Route.SITE_AND_SYSTEM
                                            ? "204_51" : "208_51");
                    put("iWillFinish", "208_40");
                }};
            }

            @Override
            public void Setup(VirtualTableScenario scn) {
                for (int i = 0; i < 8; i++) {
                    if (scn.DSDecisionAvailable(
                            "Choose [Episode VII] location")) {
                        scn.DSChooseCard(
                                scn.GetDSCard("secondBattleground"));
                    } else if (scn.LSDecisionAvailable(
                            "Choose Resistance Agent")) {
                        scn.LSChooseCard(scn.GetLSCard("beru"));
                    }
                }
            }
        };
    }

    private VirtualTableScenario scenario(Route route) {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("beru", "1_2");
                    put("secondResistanceAgent", "216_35");
                }},
                new HashMap<>() {{
                    put("firstOrderOne", "204_40");
                    put("firstOrderTwo", "204_40");
                    put("pulseOne", "1_194");
                    put("pulseTwo", "1_194");
                    put("kylo", "204_43");
                    put("vadersTie", "105_5");
                    put("maul", "11_55");
                    put("safeFirstOrder", "204_41");
                    put("lostInterrupt", "208_43");
                    if (route == Route.SITE_AND_SYSTEM) {
                        put("finalizer", "204_54");
                        put("hux", "204_41");
                    }
                    if (route == Route.STARKILLER_PULL) {
                        put("routeBattleground", "208_53");
                    }
                }},
                20,
                20,
                StartingSetup.DefaultLSGroundLocation,
                objectiveSetup(route),
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void startDeployPhase(VirtualTableScenario scn) {
        scn.MoveCardsToDSHand(
                scn.GetDSCard("firstOrderOne"),
                scn.GetDSCard("firstOrderTwo"),
                scn.GetDSCard("pulseOne"),
                scn.GetDSCard("pulseTwo"));
        scn.StartGame();
        if (!scn.AwaitingDSActivatePhaseActions()) {
            String ds = scn.DSGetDecision() == null
                    ? "none" : scn.DSGetDecision().getText();
            String ls = scn.LSGetDecision() == null
                    ? "none" : scn.LSGetDecision().getText();
            throw new AssertionError(
                    "Expected DS Activate after setup; DS=" + ds
                            + "; LS=" + ls);
        }
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
    }

    @Test
    public void frontReallyFlipsOnlyAfterFirstOrderCharactersControlTwoBattlegrounds() {
        var scn = scenario(Route.TWO_SITES);
        var objective = scn.GetDSCard("objective");
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");

        startDeployPhase(scn);

        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("firstOrderOne"), tuanul);
        assertFalse(objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("firstOrderTwo"), forest);
        assertTrue(objective.isFlipped());
    }

    @Test
    public void bothBotsReserveTheExecutableActorDeployCostButReleaseItForThatActor() {
        var scn = scenario(Route.TWO_SITES);
        var tuanul = scn.GetDSCard("tuanul");
        var routeActor = scn.GetDSCard("firstOrderTwo");

        startDeployPhase(scn);
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("firstOrderOne"), tuanul);
        scn.LSPass();

        int expected = (int) Math.ceil(
                scn.game().getModifiersQuerying().getDeployCost(
                        scn.gameState(), routeActor));
        ObjectiveAnalyzer[] analyzers = {
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer()
        };
        for (ObjectiveAnalyzer analyzer : analyzers) {
            analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
            assertEquals(expected,
                    analyzer.getIWantThatMapCurrentRouteForceReserve(
                            scn.game(), VirtualTableScenario.DS, null,
                            location -> true));
            assertEquals(0,
                    analyzer.getIWantThatMapCurrentRouteForceReserve(
                            scn.game(), VirtualTableScenario.DS, routeActor,
                            location -> true));
        }
    }

    @Test
    public void bothAnalyzersRecognizeTheRealVaderPermanentPilotSelfLoss() {
        var scn = scenario(Route.TWO_SITES);
        var vadersTie = scn.GetDSCard("vadersTie");

        startDeployPhase(scn);
        ObjectiveAnalyzer[] analyzers = {
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer()
        };
        for (ObjectiveAnalyzer analyzer : analyzers) {
            analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
            assertTrue("I Will Finish must lose a ship whose permanent pilot"
                            + " is Vader",
                    analyzer.isIWantThatMapSelfLosingDeployCandidate(
                            scn.game(), VirtualTableScenario.DS,
                            vadersTie));
            assertFalse(analyzer.isIWantThatMapSelfLosingDeployCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    scn.GetDSCard("firstOrderOne")));
        }
    }

    @Test
    public void bothPublicBotsRejectARealReserveDeployChoiceThatWouldSelfLose() {
        var scn = scenario(Route.TWO_SITES);
        startDeployPhase(scn);
        var maul = scn.GetDSCard("maul");
        var safeFirstOrder = scn.GetDSCard("safeFirstOrder");
        scn.MoveCardsToTopOfDSReserveDeck(maul, safeFirstOrder);
        assertEquals(Zone.RESERVE_DECK, maul.getZone());
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.isIWantThatMapSelfLosingDeployCandidate(
                scn.game(), VirtualTableScenario.DS, maul));
        AwaitingDecision decision =
                new ArbitraryCardsSelectionDecision(
                        "Choose card to deploy from Reserve Deck",
                        List.of(maul, safeFirstOrder), 1, 1) {
                    @Override
                    public void decisionMade(String result)
                            throws DecisionResultInvalidException {
                        getSelectedCardsByResponse(result);
                    }
                };

        var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        rando.setGame(scn.game());
        chosen.setGame(scn.game());
        String randoResponse = rando.decide(
                VirtualTableScenario.DS, decision, scn.gameState());
        String chosenResponse = chosen.decide(
                VirtualTableScenario.DS, decision, scn.gameState());

        assertEquals(randoResponse, chosenResponse);
        assertEquals("The engine-shaped temp-id decision must reject Maul",
                "temp1", randoResponse);
    }

    @Test
    public void bothBotsReserveBattleForceForTheDeclaredAgentBlocker() {
        var scn = scenario(Route.TWO_SITES);
        var objective = scn.GetDSCard("objective");
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");
        var beru = scn.GetLSCard("beru");
        var distraction = scn.GetDSCard("pulseTwo");

        startDeployPhase(scn);
        scn.MoveCardsToLocation(
                tuanul, scn.GetDSCard("firstOrderOne"));
        scn.MoveCardsToLocation(
                forest, scn.GetDSCard("kylo"),
                scn.GetDSCard("firstOrderTwo"),
                scn.GetDSCard("pulseOne"),
                scn.GetDSFiller(1), scn.GetDSFiller(2),
                scn.GetDSFiller(3), scn.GetDSFiller(4));
        scn.MoveCardsToLocation(forest, beru);

        ObjectiveAnalyzer[] analyzers = {
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer()
        };
        for (ObjectiveAnalyzer analyzer : analyzers) {
            analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
            assertTrue(analyzer.isPreFlipBattleRemovableGlobalBlockerAt(
                    scn.game(), VirtualTableScenario.DS, forest));
            assertEquals(1,
                    analyzer.getIWantThatMapCurrentRouteForceReserve(
                            scn.game(), VirtualTableScenario.DS, null,
                            location -> true));
        }

        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 1) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        String distractionDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, distraction, "Deploy");
        assertNotNull(distractionDeploy);

        var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        rando.setGame(scn.game());
        chosen.setGame(scn.game());
        AwaitingDecision deployDecision = scn.DSGetDecision();
        String randoDeploy = rando.decide(
                VirtualTableScenario.DS, deployDecision, scn.gameState());
        String chosenDeploy = chosen.decide(
                VirtualTableScenario.DS, deployDecision, scn.gameState());
        assertEquals(randoDeploy, chosenDeploy);
        assertEquals("", randoDeploy);

        scn.DSPass();
        scn.LSPass();
        assertTrue(scn.AwaitingDSBattlePhaseActions());
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, forest, "Initiate battle");
        assertNotNull(battle);
        AwaitingDecision battleDecision = scn.DSGetDecision();
        String randoBattle = rando.decide(
                VirtualTableScenario.DS, battleDecision, scn.gameState());
        String chosenBattle = chosen.decide(
                VirtualTableScenario.DS, battleDecision, scn.gameState());
        assertEquals(randoBattle, chosenBattle);
        assertEquals(battle, randoBattle);

        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        scn.DSInitiateBattle(forest);
        int lightLostBeforeBattle = scn.GetLSLostPileCount();
        scn.SkipToDamageSegment(true);
        scn.PassAllResponses();
        assertTrue("Kylo must make the battle loser lose two Force",
                scn.AwaitingLSForceLossPayment());
        scn.LSPayForceLossFromReserveDeck();
        scn.LSPayForceLossFromReserveDeck();
        assertEquals(lightLostBeforeBattle + 2,
                scn.GetLSLostPileCount());
        scn.PassAllResponses();
        assertTrue("Expected LS battle damage; DS="
                        + (scn.DSGetDecision() == null
                            ? "none" : scn.DSGetDecision().getText())
                        + "; LS="
                        + (scn.LSGetDecision() == null
                            ? "none" : scn.LSGetDecision().getText()),
                scn.AwaitingLSBattleDamagePayment());
        scn.LSPayBattleDamageFromCardInPlay(beru);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        if (!objective.isFlipped()) {
            scn.DSChooseAction("Flip");
        }
        scn.PassAllResponses();
        assertTrue("Battle route must flip; Beru zone=" + beru.getZone()
                        + "; DS="
                        + (scn.DSGetDecision() == null
                            ? "none" : scn.DSGetDecision().getText())
                        + "; LS="
                        + (scn.LSGetDecision() == null
                            ? "none" : scn.LSGetDecision().getText()),
                objective.isFlipped());
    }

    @Test
    public void bothBotsReleaseBattleForceWhenTheirLivePredictorRejectsTheFight() {
        var scn = scenario(Route.TWO_SITES);
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");
        var beru = scn.GetLSCard("beru");

        startDeployPhase(scn);
        scn.MoveCardsToLocation(
                tuanul, scn.GetDSCard("firstOrderOne"));
        scn.MoveCardsToLocation(
                forest, scn.GetDSCard("kylo"),
                scn.GetDSCard("firstOrderTwo"));
        scn.MoveCardsToLocation(forest, beru);
        scn.MoveCardsToLocation(forest, scn.GetLSFillerRange(18));

        BattleDecisionPolicy.Predictor randoPredictor =
                (myPower, myDraws, opponentPower, opponentDraws) -> {
                    var outcome = com.gempukku.swccgo.ai.models.rando
                            .evaluators.BattlePredictor.predictBattle(
                                myPower, myDraws,
                                opponentPower, opponentDraws);
                    return new BattleDecisionPolicy.Prediction(
                            outcome.winProbability,
                            outcome.expectedDamageDealt,
                            outcome.expectedDamageTaken);
                };
        BattleDecisionPolicy.Predictor chosenPredictor =
                (myPower, myDraws, opponentPower, opponentDraws) -> {
                    var outcome = com.gempukku.swccgo.ai.models.chosenone
                            .evaluators.BattlePredictor.predictBattle(
                                myPower, myDraws,
                                opponentPower, opponentDraws);
                    return new BattleDecisionPolicy.Prediction(
                            outcome.winProbability,
                            outcome.expectedDamageDealt,
                            outcome.expectedDamageTaken);
                };
        assertFalse(BattleDecisionPolicy.isPredictorSafeAtLocation(
                scn.game(), scn.gameState(), VirtualTableScenario.DS,
                forest, randoPredictor));
        assertFalse(BattleDecisionPolicy.isPredictorSafeAtLocation(
                scn.game(), scn.gameState(), VirtualTableScenario.DS,
                forest, chosenPredictor));

        ObjectiveAnalyzer randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
        ObjectiveAnalyzer chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(0,
                randoAnalyzer.getIWantThatMapCurrentRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS, null,
                        location -> BattleDecisionPolicy
                            .isPredictorSafeAtLocation(
                                scn.game(), scn.gameState(),
                                VirtualTableScenario.DS,
                                location, randoPredictor)));
        assertEquals(0,
                chosenAnalyzer.getIWantThatMapCurrentRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS, null,
                        location -> BattleDecisionPolicy
                            .isPredictorSafeAtLocation(
                                scn.game(), scn.gameState(),
                                VirtualTableScenario.DS,
                                location, chosenPredictor)));
    }

    @Test
    public void battlePredictorRunsOnceAfterSelectingFromMultipleRoutes() {
        var scn = scenario(Route.TWO_SITES);
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");

        startDeployPhase(scn);
        scn.MoveCardsToLocation(
                tuanul, scn.GetDSCard("firstOrderOne"),
                scn.GetDSFiller(1), scn.GetDSFiller(2),
                scn.GetDSFiller(3), scn.GetDSFiller(4));
        scn.MoveCardsToLocation(
                forest, scn.GetDSCard("kylo"),
                scn.GetDSCard("firstOrderTwo"),
                scn.GetDSFiller(5), scn.GetDSFiller(6),
                scn.GetDSFiller(7), scn.GetDSFiller(8));
        scn.MoveCardsToLocation(tuanul, scn.GetLSCard("beru"));
        scn.MoveCardsToLocation(
                forest, scn.GetLSCard("secondResistanceAgent"));

        ObjectiveAnalyzer[] analyzers = {
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer()
        };
        for (ObjectiveAnalyzer analyzer : analyzers) {
            analyzer.analyze(
                    scn.game(), VirtualTableScenario.DS, Side.DARK);
            assertTrue(analyzer.isPreFlipBattleRemovableGlobalBlockerAt(
                    scn.game(), VirtualTableScenario.DS, tuanul));
            assertTrue(analyzer.isPreFlipBattleRemovableGlobalBlockerAt(
                    scn.game(), VirtualTableScenario.DS, forest));
            int[] calls = {0};
            assertEquals(1,
                    analyzer.getIWantThatMapCurrentRouteForceReserve(
                            scn.game(), VirtualTableScenario.DS, null,
                            location -> {
                                calls[0]++;
                                return true;
                            }));
            assertEquals("Predict only after selecting one route",
                    1, calls[0]);
        }
    }

    @Test
    public void bothPublicBotsChooseTheNativeBattlegroundPullOverPass() {
        var scn = scenario(Route.STARKILLER_PULL);
        var starkiller = scn.GetDSCard("secondBattleground");

        scn.MoveCardsToDSHand(
                scn.GetDSCard("pulseOne"),
                scn.GetDSCard("pulseTwo"));
        scn.StartGame();
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.MoveCardsToTopOfDSReserveDeck(
                scn.GetDSCard("routeBattleground"));
        String routeAction = scn.GetCardActionId(
                VirtualTableScenario.DS, starkiller,
                "Deploy battleground from Reserve Deck");
        assertNotNull(routeAction);
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        var routeBattleground = scn.GetDSCard("routeBattleground");
        assertTrue("source=" + starkiller.getZone()
                        + "; candidate=" + routeBattleground.getZone()
                        + "; nativeCandidate="
                        + analyzer.isIWantThatMapNativeBattlegroundRouteCandidate(
                            scn.game(), VirtualTableScenario.DS,
                            starkiller, routeBattleground),
                analyzer.isIWantThatMapBattlegroundRouteAction(
                    scn.game(), VirtualTableScenario.DS, starkiller,
                    "Deploy battleground from Reserve Deck"));

        AwaitingDecision decision = scn.DSGetDecision();
        var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        rando.setGame(scn.game());
        chosen.setGame(scn.game());
        String randoResponse = rando.decide(
                VirtualTableScenario.DS, decision, scn.gameState());
        String chosenResponse = chosen.decide(
                VirtualTableScenario.DS, decision, scn.gameState());

        assertEquals(randoResponse, chosenResponse);
        assertEquals("decision=" + decision.getText()
                        + "; actionIds=" + Arrays.toString(
                            decision.getDecisionParameters().get("actionId"))
                        + "; actionTexts=" + Arrays.toString(
                            decision.getDecisionParameters().get("actionText"))
                        + "; cardIds=" + Arrays.toString(
                            decision.getDecisionParameters().get("cardId"))
                        + "; blueprints=" + Arrays.toString(
                            decision.getDecisionParameters().get("blueprintId")),
                routeAction, randoResponse);
    }

    @Test
    public void bothPublicBotsSpendExactRemainingForceOnTheMissingFirstOrderActor() {
        var scn = scenario(Route.TWO_SITES);
        var tuanul = scn.GetDSCard("tuanul");
        var routeActor = scn.GetDSCard("firstOrderTwo");
        var distraction = scn.GetDSCard("pulseTwo");

        scn.MoveCardsToDSHand(
                scn.GetDSCard("firstOrderOne"),
                routeActor,
                scn.GetDSCard("pulseOne"),
                distraction);
        scn.StartGame();
        scn.MoveCardsToLocation(
                tuanul, scn.GetDSCard("firstOrderOne"));
        scn.MoveCardsToTopOfDSUsedPile(scn.GetDSCard("pulseOne"));
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        String routeAction = scn.GetCardActionId(
                VirtualTableScenario.DS, routeActor, "Deploy");
        String distractionAction = scn.GetCardActionId(
                VirtualTableScenario.DS, distraction, "Deploy");
        assertNotNull(routeAction);
        assertNotNull(distractionAction);
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(2,
                analyzer.getIWantThatMapCurrentRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS, null,
                        location -> true));
        assertEquals(0,
                analyzer.getIWantThatMapCurrentRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS, routeActor,
                        location -> true));
        assertEquals(2,
                analyzer.getIWantThatMapCurrentRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS, distraction,
                        location -> true));
        AwaitingDecision decision = scn.DSGetDecision();
        var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        rando.setGame(scn.game());
        chosen.setGame(scn.game());
        String randoResponse = rando.decide(
                VirtualTableScenario.DS, decision, scn.gameState());
        String chosenResponse = chosen.decide(
                VirtualTableScenario.DS, decision, scn.gameState());

        assertEquals(randoResponse, chosenResponse);
        assertEquals("force=" + scn.GetDSForcePileCount()
                        + "; routeCard=" + routeActor.getCardId()
                        + "; distractionCard="
                        + distraction.getCardId()
                        + "; actionIds=" + Arrays.toString(
                            decision.getDecisionParameters().get("actionId"))
                        + "; actionTexts=" + Arrays.toString(
                            decision.getDecisionParameters().get("actionText"))
                        + "; cardIds=" + Arrays.toString(
                            decision.getDecisionParameters().get("cardId")),
                routeAction, randoResponse);
        assertFalse(distractionAction.equals(randoResponse));
    }

    @Test
    public void bothPublicBotsChooseTheBackSideKyloStackActionOverPass() {
        var scn = scenario(Route.TWO_SITES);
        var objective = scn.GetDSCard("objective");
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");

        startDeployPhase(scn);
        scn.MoveCardsToLocation(tuanul, scn.GetDSCard("kylo"));
        scn.MoveCardsToLocation(
                forest, scn.GetDSCard("firstOrderOne"));
        scn.MoveCardsToTopOfDSLostPile(
                scn.GetDSCard("lostInterrupt"));
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("pulseOne"), tuanul);
        assertTrue(objective.isFlipped());
        for (var card : new ArrayList<>(
                scn.gameState().getHand(VirtualTableScenario.DS))) {
            scn.MoveCardsToTopOfDSUsedPile((PhysicalCardImpl) card);
        }
        scn.LSPass();

        String stackAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Stack Interrupt from Lost Pile");
        assertNotNull(stackAction);
        AwaitingDecision decision = scn.DSGetDecision();
        var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        rando.setGame(scn.game());
        chosen.setGame(scn.game());
        String randoResponse = rando.decide(
                VirtualTableScenario.DS, decision, scn.gameState());
        String chosenResponse = chosen.decide(
                VirtualTableScenario.DS, decision, scn.gameState());

        assertEquals(randoResponse, chosenResponse);
        assertEquals("decision=" + decision.getText()
                        + "; actionIds=" + Arrays.toString(
                            decision.getDecisionParameters().get("actionId"))
                        + "; actionTexts=" + Arrays.toString(
                            decision.getDecisionParameters().get("actionText"))
                        + "; cardIds=" + Arrays.toString(
                            decision.getDecisionParameters().get("cardId")),
                stackAction, randoResponse);
    }

    @Test
    public void declaredResistanceAgentAtBattlegroundSiteBlocksTheNativeFlip() {
        var scn = scenario(Route.TWO_SITES);
        var objective = scn.GetDSCard("objective");
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");
        var beru = scn.GetLSCard("beru");

        startDeployPhase(scn);
        scn.MoveCardsToLocation(tuanul, beru);

        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("firstOrderOne"), tuanul);
        assertFalse(objective.isFlipped());
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard("firstOrderTwo"), forest);
        assertFalse(objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(beru);
        scn.DSDeployCardAndPassResponses(scn.GetDSCard("pulseOne"), forest);
        assertTrue(objective.isFlipped());
    }

    @Test
    public void physicalFirstOrderPilotAboardCarrierCountsAtSystem() {
        var scn = scenario(Route.SITE_AND_SYSTEM);
        var objective = scn.GetDSCard("objective");
        var tuanul = scn.GetDSCard("tuanul");
        var system = scn.GetDSCard("secondBattleground");
        var finalizer = scn.GetDSCard("finalizer");
        var hux = scn.GetDSCard("hux");

        startDeployPhase(scn);
        scn.MoveCardsToLocation(tuanul, scn.GetDSCard("firstOrderOne"));
        scn.MoveCardsToLocation(system, finalizer);
        scn.BoardAsPilot(finalizer, hux);

        scn.DSDeployCardAndPassResponses(scn.GetDSCard("pulseOne"), tuanul);
        assertTrue(objective.isFlipped());
    }

    @Test
    public void backReallyFlipsWhenDarkNoLongerOccupiesTwoBattlegrounds() {
        var scn = scenario(Route.TWO_SITES);
        var objective = scn.GetDSCard("objective");
        var tuanul = scn.GetDSCard("tuanul");
        var forest = scn.GetDSCard("secondBattleground");
        var firstOrderOne = scn.GetDSCard("firstOrderOne");
        var firstOrderTwo = scn.GetDSCard("firstOrderTwo");

        startDeployPhase(scn);
        scn.DSDeployCardAndPassResponses(firstOrderOne, tuanul);
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(firstOrderTwo, forest);
        assertTrue(objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(firstOrderTwo);
        scn.DSDeployCardAndPassResponses(scn.GetDSCard("pulseOne"), tuanul);
        assertFalse(objective.isFlipped());
    }
}
