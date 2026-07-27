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
 * Batch Sixteen (2026-07-27): native engine contract for Twin Suns Of
 * Tatooine / Well Trained In The Jedi Arts (301_4, DARK — the takeover's
 * LIGHT listing was wrong). Card Java unchanged.
 *
 * Law (Card301_004.java L100-L122): flips when you control two Tatooine
 * battleground sites, at least one WITH a Dark Jedi (computed: dark
 * character of ability 6+), you occupy Tatooine system, and the opponent
 * controls zero Tatooine sites of any kind. Back (Card301_004_BACK.java
 * L97-L118): flips back when the opponent controls strictly more Tatooine
 * sites than you; ties hold.
 */
public class TwinSunsObjectiveEngineContractTest {

    private static final StartingSetup TWIN_SUNS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "301_4");
                put("system", "1_289");
                put("mosEisley", "1_295");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("mosEisley"));
                }
            }
        }
    };

    private VirtualTableScenario tsotScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("lsDockingBay", "1_129");
                }},
                new HashMap<>() {{
                    put("cantina", "1_290");
                    put("vader", "1_168");
                    put("tie", "1_304");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                TWIN_SUNS,
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
    public void tsotFrontRequiresAllFourLegs() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetDSFiller(3);
        var pulseTwo = scn.GetDSFiller(4);
        var pulseThree = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        // Two controlled battleground sites, no Dark Jedi, no system: no flip.
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Two sites without a Dark Jedi and system occupation must not flip",
                objective.isFlipped());
        scn.LSPass();

        // Add the Dark Jedi at a controlled site; still no system occupation.
        scn.MoveCardsToLocation(cantina, vader);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("A Dark Jedi without Tatooine system occupation must not flip",
                objective.isFlipped());
        scn.LSPass();

        // Occupy the system: all four legs complete.
        scn.MoveCardsToLocation(system, tie);
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertTrue("All four legs together must flip",
                objective.isFlipped());
    }

    @Test
    public void tsotFrontIsBlockedByAnyOpponentControlledTatooineSite() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var lsDockingBay = scn.GetLSCard("lsDockingBay");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetDSFiller(3);
        var pulseTwo = scn.GetDSFiller(4);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, lsDockingBay);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(cantina, vader);
        scn.MoveCardsToLocation(system, tie);
        // The opponent solely controls their light docking bay.
        scn.MoveCardsToLocation(lsDockingBay, scn.GetLSFiller(1));

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Any opponent-controlled Tatooine site must block the flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(scn.GetLSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Clearing the opponent's control must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void tsotBackHoldsAtTiesAndFlipsBackWhenOutcontrolled() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var lsDockingBay = scn.GetLSCard("lsDockingBay");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetDSFiller(3);
        var pulseTwo = scn.GetDSFiller(4);
        var pulseThree = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, lsDockingBay);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(cantina, vader);
        scn.MoveCardsToLocation(system, tie);

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("All four legs must flip", objective.isFlipped());
        scn.LSPass();

        // Owner collapses to one site; opponent takes one: 1-1 tie holds.
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(vader);
        scn.MoveCardsToLocation(lsDockingBay, scn.GetLSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("A 1-1 Tatooine site tie must hold the back",
                objective.isFlipped());
        scn.LSPass();

        // Opponent overtakes: strictly more flips the back.
        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertFalse("Strictly more opponent-controlled Tatooine sites must flip back",
                objective.isFlipped());
    }

    @Test
    public void tsotProfileRulesTrackTheEngineLaw() {
        var scn = tsotScenario();
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");

        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 301_4", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("TSOT front encodes one four-leg rule", 1,
                preFlip.size());
        assertFalse("Without the Dark Jedi and system the rule is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(cantina, vader);
        scn.MoveCardsToLocation(system, tie);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("All four legs complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one relative-count rule", 1,
                postFlip.size());
    }
}
