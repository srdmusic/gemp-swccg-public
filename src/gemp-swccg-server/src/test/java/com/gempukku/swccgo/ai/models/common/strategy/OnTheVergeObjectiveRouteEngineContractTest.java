package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression proof for the On The Verge mobile-system route touched by
 * the shared Set Your Course multiple-choice transport.
 */
public class OnTheVergeObjectiveRouteEngineContractTest {

    private static final StartingSetup ON_THE_VERGE = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "216_11");
                put("deathStar", "216_7");
                put("scarif", "216_13");
                put("citadelTower", "216_15");
                put("shieldGate", "216_18");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int guard = 0; guard < 20; guard++) {
                if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("deathStar"))) {
                    scn.DSChooseCard(scn.GetDSCard("deathStar"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("scarif"))) {
                    scn.DSChooseCard(scn.GetDSCard("scarif"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("citadelTower"))) {
                    scn.DSChooseCard(scn.GetDSCard("citadelTower"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("shieldGate"))) {
                    scn.DSChooseCard(scn.GetDSCard("shieldGate"));
                } else if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else {
                    break;
                }
            }
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("convertedScarif", "209_23");
                }},
                new HashMap<>() {{
                    put("orbitDecoy", "3_151");
                    put("krennic", "207_20");
                    put("scarifCommand", "216_16");
                    put("postFlipDeploy", "200_86");
                }},
                16,
                16,
                StartingSetup.DefaultLSGroundLocation,
                ON_THE_VERGE,
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

        private String decideBoth(VirtualTableScenario scn) {
            AwaitingDecision decision = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull("Expected Dark Side decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            assertEquals("Rando/Chosen parity for " + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private static void chooseResultBoth(
            VirtualTableScenario scn, PublicBots bots,
            String textFragment, String expectedResult) {
        AwaitingDecision decision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(decision);
        assertTrue(decision.getText().contains(textFragment));
        String response = bots.decideBoth(scn);
        String[] results = decision.getDecisionParameters().get("results");
        assertNotNull(results);
        int index = Integer.parseInt(response);
        assertTrue(index >= 0 && index < results.length);
        assertEquals("results=" + Arrays.toString(results),
                expectedResult, results[index]);
        scn.DSDecided(response);
    }

    private static void leaveOneDarkForce(VirtualTableScenario scn) {
        leaveDarkForce(scn, 1);
    }

    private static void leaveDarkForce(
            VirtualTableScenario scn, int amount) {
        while (scn.GetDSForcePileCount() > amount) {
            scn.MoveCardsToTopOfDSUsedPile(scn.GetTopOfDSForcePile());
        }
        if (scn.GetDSForcePileCount() < amount) {
            scn.DSActivateForceCheat(
                    amount - scn.GetDSForcePileCount());
        }
    }

    @Test
    public void publicBotsChooseScarifThroughGenericOrbitAndCardSelection() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var scarif = scn.GetDSCard("scarif");
        var convertedScarif = scn.GetLSCard("convertedScarif");
        var citadelTower = scn.GetDSCard("citadelTower");
        var shieldGate = scn.GetDSCard("shieldGate");
        var orbitDecoy = scn.GetDSCard("orbitDecoy");
        var krennic = scn.GetDSCard("krennic");
        var scarifCommand = scn.GetDSCard("scarifCommand");
        var postFlipDeploy = scn.GetDSCard("postFlipDeploy");

        scn.StartGame();
        assertTrue(scn.IsAttachedTo(scarif, shieldGate));
        assertEquals(Title.Scarif, citadelTower.getPartOfSystem());
        assertEquals(4, deathStar.getParsec());
        assertTrue(objective.getZone().isInPlay());

        scn.MoveLocationToTable(convertedScarif);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS,
                com.gempukku.swccgo.common.Side.DARK);
        assertTrue("Source-title routing must survive a converted Scarif system",
                analyzer.isOnTheVergeScarifOrbitCandidate(
                        convertedScarif));
        scn.MoveLocationToTable(orbitDecoy);
        scn.MoveCardsToLocation(citadelTower, krennic);
        scn.SkipToPhase(Phase.MOVE);
        leaveOneDarkForce(scn);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }

        PublicBots bots = PublicBots.forGame(scn);
        String firstMove = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(firstMove);
        assertEquals(firstMove, bots.decideBoth(scn));
        scn.DSDecided(firstMove);
        chooseResultBoth(scn, bots,
                "Choose parsec to move to", "6");
        scn.PassAllResponses();
        assertEquals(6, deathStar.getParsec());
        assertEquals(null, deathStar.getSystemOrbited());
        assertEquals(0, scn.GetDSForcePileCount());

        scn.SkipToDSTurn(Phase.MOVE);
        leaveOneDarkForce(scn);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }
        String secondMove = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(secondMove);
        assertEquals(secondMove, bots.decideBoth(scn));
        scn.DSDecided(secondMove);

        chooseResultBoth(scn, bots,
                "Choose parsec to move to", "7");
        chooseResultBoth(scn, bots,
                "Choose destination for", "Orbit a system");
        assertTrue(scn.DSHasCardChoiceAvailable(convertedScarif));
        assertTrue(scn.DSHasCardChoiceAvailable(orbitDecoy));
        assertEquals(Integer.toString(convertedScarif.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(convertedScarif.getCardId()));
        scn.PassAllResponses();

        assertEquals(7, deathStar.getParsec());
        assertEquals(Title.Scarif, deathStar.getSystemOrbited());
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The native actor plus orbit gate must flip On The Verge",
                objective.isFlipped());

        // The back no longer needs Death Star movement. With exactly the five
        // Force needed for Mara, both public bots must deploy her rather than
        // hoard one Force for the retired front-side Scarif route.
        scn.MoveCardsToDSHand(postFlipDeploy);
        scn.MoveCardsToBottomOfDSReserveDeck(scarifCommand);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, 5);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        String postFlipObjectiveAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy a Scarif battleground");
        assertNotNull(postFlipObjectiveAction);
        scn.DSDecided(postFlipObjectiveAction);
        scn.PassAllResponses();
        if (scn.DSHasCardChoiceAvailable(scarifCommand)) {
            scn.DSChooseCard(scarifCommand);
            scn.PassAllResponses();
        }
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Left");
        }
        scn.PassAllResponses();
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        assertTrue("phase=" + scn.gameState().getCurrentPhase()
                        + "; DS=" + (scn.GetAwaitingDecision(
                            VirtualTableScenario.DS) != null
                            ? scn.GetAwaitingDecision(
                                VirtualTableScenario.DS).getText()
                            : "none")
                        + "; LS=" + (scn.GetAwaitingDecision(
                            VirtualTableScenario.LS) != null
                            ? scn.GetAwaitingDecision(
                                VirtualTableScenario.LS).getText()
                            : "none"),
                scn.AwaitingDSDeployPhaseActions());
        String postFlipDeployAction = scn.GetCardActionId(
                VirtualTableScenario.DS, postFlipDeploy, "Deploy");
        assertNotNull(postFlipDeployAction);
        assertEquals("The back face must release the obsolete move reserve",
                postFlipDeployAction, bots.decideBoth(scn));
        scn.DSDecided(postFlipDeployAction);
        if (scn.DSHasCardChoiceAvailable(citadelTower)) {
            scn.DSChooseCard(citadelTower);
        }
        scn.PassAllResponses();

        // Once the native objective is on its back, Scarif orbit is no longer
        // part of its survival condition. Recreate the old adversarial 4 ->
        // {2, 6} child prompt and prove neither public bot keeps applying the
        // front-only V79 route score after the flip.
        deathStar.setSystemOrbited(null);
        deathStar.setParsec(4);
        scn.SkipToDSTurn(Phase.MOVE);
        leaveOneDarkForce(scn);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }
        String postFlipMove = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(postFlipMove);
        assertEquals("The back face must not initiate obsolete Scarif movement",
                "", bots.decideBoth(scn));
        scn.DSDecided(postFlipMove);

        AwaitingDecision postFlipPrompt = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(postFlipPrompt);
        assertTrue(postFlipPrompt.getText().contains(
                "Choose parsec to move to"));
        String[] postFlipResults = postFlipPrompt
                .getDecisionParameters().get("results");
        assertNotNull(postFlipResults);
        assertEquals("The fixture must keep the tempting parsec 6 away from option zero: "
                        + Arrays.toString(postFlipResults),
                "2", postFlipResults[0]);
        String postFlipResponse = bots.decideBoth(scn);
        assertEquals("The back face must release the old Scarif child score: "
                        + Arrays.toString(postFlipResults),
                "0", postFlipResponse);
    }
}
