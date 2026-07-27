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
 * Batch Twelve (2026-07-27): native engine contract for Imperial
 * Entanglements / No One To Stop Us This Time (201_39, DARK). Card Java
 * unchanged.
 *
 * Law (Card201_039.java L125-L145): flips when you control at least three
 * Tatooine sites while the opponent controls fewer than three. Back
 * (Card201_039_BACK.java L137-L158): flips back when the opponent controls
 * strictly more Tatooine sites than you; equal counts hold. All fixture
 * deploys by the owner respect the objective's own Imperial-only deploy ban
 * (fillers are stormtroopers), and all sites are dark printings to avoid
 * same-title conversion arithmetic.
 */
public class ImperialEntanglementsObjectiveEngineContractTest {

    private static final StartingSetup IMPERIAL_ENTANGLEMENTS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "201_39");
                put("system", "1_289");
                put("devastator", "1_301");
                put("mosEisley", "1_295");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Tatooine system, Devastator to it, and
            // one will-be-battleground Tatooine site. Side placement first
            // (its text also contains "deploy"), then site multi-match picks
            // the designated starting site.
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("site to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("mosEisley"));
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("mosEisley"));
                }
            }
        }
    };

    private VirtualTableScenario ieScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("cantina", "1_290");
                    put("dockingBay94", "1_291");
                    put("jawaCamp", "1_292");
                    put("jundland", "1_293");
                    put("larsFarm", "1_294");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                IMPERIAL_ENTANGLEMENTS,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveSiteToTatooine(
            VirtualTableScenario scn, PhysicalCardImpl site) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, Title.Tatooine, null);
        assertFalse("Expected a legal placement at Tatooine",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    @Test
    public void ieFrontRequiresThreeControlledTatooineSites() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var pulseOne = scn.GetDSFiller(4);
        var thirdTrooper = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, thirdTrooper);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Two controlled Tatooine sites must not flip",
                objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(thirdTrooper, jawaCamp);
        assertTrue("Three controlled Tatooine sites must flip",
                objective.isFlipped());
    }

    @Test
    public void ieFrontIsBlockedWhileOpponentAlsoControlsThreeSites() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var dockingBay94 = scn.GetDSCard("dockingBay94");
        var jundland = scn.GetDSCard("jundland");
        var larsFarm = scn.GetDSCard("larsFarm");
        var pulseOne = scn.GetDSFiller(4);
        var pulseTwo = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        moveSiteToTatooine(scn, dockingBay94);
        moveSiteToTatooine(scn, jundland);
        moveSiteToTatooine(scn, larsFarm);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(jawaCamp, scn.GetDSFiller(3));
        scn.MoveCardsToLocation(dockingBay94, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jundland, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(larsFarm, scn.GetLSFiller(3));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Opponent control of three Tatooine sites must block the flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(scn.GetLSFiller(3));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Opponent dropping to two controlled sites must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void ieBackHoldsAtEqualCountsAndFlipsBackWhenOutcontrolled() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var dockingBay94 = scn.GetDSCard("dockingBay94");
        var jundland = scn.GetDSCard("jundland");
        var larsFarm = scn.GetDSCard("larsFarm");
        var pulseOne = scn.GetDSFiller(4);
        var pulseTwo = scn.GetDSFiller(5);
        var pulseThree = scn.GetDSFiller(6);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        moveSiteToTatooine(scn, dockingBay94);
        moveSiteToTatooine(scn, jundland);
        moveSiteToTatooine(scn, larsFarm);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(jawaCamp, scn.GetDSFiller(3));

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Three controlled sites must flip", objective.isFlipped());
        scn.LSPass();

        // Owner collapses to one site; opponent takes one: 1-1 equality holds.
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.MoveCardsToLocation(dockingBay94, scn.GetLSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Equal Tatooine site counts must hold the back",
                objective.isFlipped());
        scn.LSPass();

        // Opponent takes a second site: strictly more flips the back.
        scn.MoveCardsToLocation(jundland, scn.GetLSFiller(2));
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertFalse("Strictly more opponent-controlled Tatooine sites must flip back",
                objective.isFlipped());
    }

    @Test
    public void ieProfileRulesTrackTheEngineLaw() {
        var scn = ieScenario();
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");

        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 201_39", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("IE front encodes one rule", 1, preFlip.size());
        assertFalse("Two controlled sites leave the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(jawaCamp, scn.GetDSFiller(3));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Three controlled sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one relative-count rule", 1,
                postFlip.size());
    }
}
