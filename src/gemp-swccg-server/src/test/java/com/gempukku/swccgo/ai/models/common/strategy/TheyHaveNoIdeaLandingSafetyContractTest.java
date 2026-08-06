package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Adversarial safety boundary for the exact THNI Rogue One landing route.
 * Card Java remains unchanged.
 */
public class TheyHaveNoIdeaLandingSafetyContractTest {

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
                    put("landingPad", "209_26");
                    put("rogueOne", "206_7");
                    put("bodhi", "206_1");
                    put("trooper", "1_28");
                }},
                new HashMap<>() {{
                    put("vader", "1_168");
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

    private void moveLocationToScarif(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Scarif, null);
        assertFalse("Expected a legal Scarif placement", placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    @Test
    public void exactLandingOverrideClosesAtEnemyOccupiedLandingPad() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var dataVault = scn.GetLSCard("dataVault");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");
        var vader = scn.GetDSCard("vader");
        var pulse = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToLocation(dataVault, trooper);
        scn.MoveCardsToLocation(system, rogueOne);
        scn.BoardAsPilot(rogueOne, bodhi);
        scn.MoveCardsToLocation(landingPad, vader);
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("The native objective must be on its back",
                objective.isFlipped());

        scn.SkipToPhase(Phase.MOVE);
        if (scn.AwaitingDSMovePhaseActions()) {
            scn.DSPass();
        }
        String land = scn.GetCardActionId(
                VirtualTableScenario.LS, rogueOne, "Land");
        assertNotNull("Rogue One must expose the real Land action", land);
        scn.LSDecided(land);
        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull("Land must open the real destination prompt", destination);
        assertTrue("Expected the real landing prompt; got "
                        + destination.getText(),
                destination.getText().contains("Choose where to land"));
        String landingPadId = Integer.toString(landingPad.getCardId());
        assertTrue("Enemy occupation does not make the destination illegal",
                Arrays.asList(destination.getDecisionParameters()
                        .get("cardId"))
                    .contains(landingPadId));
        assertTrue("The adversarial destination must really be occupied by DS",
                scn.game().getModifiersQuerying().occupiesLocation(
                    scn.gameState(), landingPad,
                    VirtualTableScenario.DS, null));

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        assertFalse("Rando must not activate the THNI landing override for "
                        + "enemy-occupied Landing Pad Nine",
                rando.isTheyHaveNoIdeaRogueOneLandingDestination(
                    scn.game(), VirtualTableScenario.LS,
                    rogueOne, landingPad));
        assertFalse("Chosen One must preserve the same safety boundary",
                chosen.isTheyHaveNoIdeaRogueOneLandingDestination(
                    scn.game(), VirtualTableScenario.LS,
                    rogueOne, landingPad));
    }
}
