package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Real battle-forfeit coverage for They Have No Idea We're Coming
 * (209_29). Card Java remains unchanged.
 */
public class TheyHaveNoIdeaBattleForfeitEngineContractTest {

    private static final StartingSetup THEY_HAVE_NO_IDEA =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "209_29");
                        put("system", "209_23");
                        put("dataVault", "209_25");
                        put("stardust", "209_18");
                        put("warRoom", "1_139");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    for (int i = 0; i < 10; i++) {
                        if (scn.LSDecisionAvailable("On which side")) {
                            scn.LSChoose("Left");
                        } else if (scn.LSDecisionAvailable("to deploy")) {
                            scn.LSChooseCard(scn.GetLSCard("dataVault"));
                        }
                    }
                }
            };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("rogueOne", "206_7");
                    put("bodhi", "206_1");
                    put("passenger", "1_28");
                    put("vaultHolder", "1_28");
                }},
                new HashMap<>() {{
                    put("devastator", "1_301");
                    put("tie", "1_304");
                }},
                24,
                24,
                THEY_HAVE_NO_IDEA,
                StartingSetup.DefaultDSGroundLocation,
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
                    VirtualTableScenario.LS);
            assertNotNull("Expected a Light Side forfeit decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS, decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS, decision, scn.gameState());
            assertEquals("Rando and Chosen One must choose the same forfeit",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    @Test
    public void publicBotsPreserveSoleFlipBackCarrierBeforeCrew() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var dataVault = scn.GetLSCard("dataVault");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var passenger = scn.GetLSCard("passenger");
        var vaultHolder = scn.GetLSCard("vaultHolder");
        var devastator = scn.GetDSCard("devastator");
        var tie = scn.GetDSCard("tie");
        var pulse = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        scn.MoveCardsToLocation(dataVault, vaultHolder);
        scn.MoveCardsToLocation(system, rogueOne);
        scn.BoardAsPilot(rogueOne, bodhi);
        scn.BoardAsPassenger(rogueOne, passenger);
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("The native objective must flip before the battle",
                objective.isFlipped());

        scn.MoveCardsToLocation(system, devastator, tie);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                randoAnalyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, rogueOne));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                randoAnalyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, passenger));
        assertEquals("The passenger remains an independent presence source "
                        + "if Bodhi is forfeited",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                randoAnalyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, bodhi));
        assertEquals(
                randoAnalyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, rogueOne),
                chosenAnalyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, rogueOne));

        scn.DSActivateForceCheat(1);
        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        scn.SkipToDSTurn(Phase.BATTLE);
        scn.DSInitiateBattle(system);
        scn.SkipToDamageSegment(true);

        assertTrue("The real battle must require a Light Side forfeiture",
                scn.AwaitingLSAttritionPayment()
                    || scn.AwaitingLSBattleDamagePayment());
        AwaitingDecision forfeit = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(forfeit);
        assertTrue("Expected a real battle-forfeit prompt; got "
                        + forfeit.getText(),
                forfeit.getText().contains("battle to forfeit"));
        String[] offered = forfeit.getDecisionParameters().get("cardId");
        assertNotNull("The forfeit prompt must expose physical cards", offered);
        assertTrue("Rogue One must be a legal forfeit candidate",
                Arrays.asList(offered).contains(
                    Integer.toString(rogueOne.getCardId())));
        assertTrue("The passenger must be a legal disposable alternative",
                Arrays.asList(offered).contains(
                    Integer.toString(passenger.getCardId())));

        String selected = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("Both bots must preserve Rogue One while attached "
                        + "characters can satisfy the real forfeit",
                Integer.toString(rogueOne.getCardId()).equals(selected));
        assertTrue("The real decision must spend a character before its "
                        + "carrier; selected=" + selected,
                Integer.toString(bodhi.getCardId()).equals(selected)
                    || Integer.toString(passenger.getCardId()).equals(
                        selected));
    }
}
