package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Batch Fourteen (2026-07-27): native engine contract for They Have No Idea
 * We're Coming / Until We Win, Or The Chances Are Spent (209_29, LIGHT).
 * Card Java unchanged.
 *
 * Law (Card209_029.java L138-L156): flips when you control two Scarif
 * locations (system or sites). Back (Card209_029_BACK.java L176-L196):
 * flips back when you occupy fewer than two Scarif locations UNLESS a
 * Rogue One is at a Scarif site you occupy.
 */
public class TheyHaveNoIdeaObjectiveEngineContractTest {

    private static final StartingSetup THEY_HAVE_NO_IDEA = new StartingSetup() {
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
            // Four required start deploys (system, Data Vault, Stardust on
            // the vault, Massassi War Room). Side prompts first; any card
            // choice defaults to the designated Data Vault.
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("dataVault"));
                }
            }
        }
    };

    private VirtualTableScenario thniScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("beach", "209_24");
                    put("landingPad", "209_26");
                    put("rogueOne", "206_7");
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
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
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToScarif(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Scarif, null);
        assertFalse("Expected a legal placement at Scarif",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    @Test
    public void thniFrontFlipsOnTwoControlledScarifLocations() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var beach = scn.GetLSCard("beach");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToScarif(scn, beach);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("One controlled Scarif location must not flip",
                objective.isFlipped());
        scn.DSPass();

        // The system counts toward the LOCATION pool: site + system = two.
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Two controlled Scarif locations (site + system) must flip",
                objective.isFlipped());
    }

    @Test
    public void thniFrontRequiresControlNotMereOccupation() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var beach = scn.GetLSCard("beach");
        var landingPad = scn.GetLSCard("landingPad");
        var pulseOne = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToScarif(scn, beach);
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));
        // The second location is contested: occupied but not controlled.
        scn.MoveCardsToLocation(landingPad, scn.GetLSFiller(3));
        scn.MoveCardsToLocation(landingPad, scn.GetDSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("An occupied-but-contested second location must not flip",
                objective.isFlipped());
    }

    @Test
    public void thniBackRogueOneHoldsBelowTheOccupationFloor() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var beach = scn.GetLSCard("beach");
        var rogueOne = scn.GetLSCard("rogueOne");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);
        var pulseThree = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveLocationToScarif(scn, beach);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(16);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Two controlled Scarif locations must flip",
                objective.isFlipped());
        scn.DSPass();

        // Drop to ONE occupied Scarif location, but park Rogue One at the
        // occupied beach: the exception must hold the back.
        scn.MoveOutOfPlay(xwing);
        scn.MoveCardsToLocation(beach, rogueOne);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Rogue One at an occupied Scarif site must hold the back",
                objective.isFlipped());
        scn.DSPass();

        // Remove Rogue One: occupation floor unmet, no exception, flip back.
        scn.MoveOutOfPlay(rogueOne);
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        assertFalse("Without Rogue One the sub-floor occupation must flip back",
                objective.isFlipped());
    }

    @Test
    public void thniProfileRulesTrackTheEngineLaw() {
        var scn = thniScenario();
        var system = scn.GetLSCard("system");
        var beach = scn.GetLSCard("beach");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToScarif(scn, beach);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 209_29", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("THNI front encodes one rule", 1, preFlip.size());
        assertFalse("One controlled location leaves the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(system, xwing);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Two controlled locations complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one two-leg hold rule", 1,
                postFlip.size());
    }
}
