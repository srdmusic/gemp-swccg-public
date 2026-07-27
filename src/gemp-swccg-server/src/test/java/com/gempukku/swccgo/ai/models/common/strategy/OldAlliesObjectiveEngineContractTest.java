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
 * Batch Thirteen (2026-07-27): native engine contract for Old Allies / We
 * Need Your Help (204_32, LIGHT). Card Java unchanged.
 *
 * Law (Card204_032.java L130-L138): flips when you control Jakku system and
 * occupy two Jakku battleground sites, OR occupy the system and control two
 * such sites (cross-paired). Back (Card204_032_BACK.java L125-L127): flips
 * back when you occupy fewer than two battlegrounds ANYWHERE.
 */
public class OldAlliesObjectiveEngineContractTest {

    private static final StartingSetup OLD_ALLIES = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "204_32");
                put("system", "204_26");
                put("shipyard", "204_27");
                put("falcon", "206_8");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Jakku system, Niima Outpost Shipyard,
            // and the [EP VII] Falcon landed at the shipyard; the optional
            // Graveyard Of Giants has no match and skips.
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("shipyard"));
                }
            }
        }
    };

    private VirtualTableScenario oaScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("ravager", "204_28");
                    put("reysCamp", "204_29");
                    put("tuanul", "204_31");
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                }},
                24,
                24,
                OLD_ALLIES,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToJakku(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Jakku, null);
        assertFalse("Expected a legal placement at Jakku",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    @Test
    public void oaFrontLegAFlipsOnControlledSystemWithTwoOccupiedSites() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        // Both battleground sites merely OCCUPIED (contested by the
        // opponent); the system stays empty until after the phase skip so
        // the flip completes only at the assertion pulse.
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(shipyard, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(ravager, scn.GetDSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Controlled system plus two occupied battleground sites must flip (leg A)",
                objective.isFlipped());
    }

    @Test
    public void oaFrontLegBFlipsOnOccupiedSystemWithTwoControlledSites() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        // Both battleground sites solely CONTROLLED; the contested system
        // (occupy without control) is assembled after the phase skip.
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.MoveCardsToLocation(system, tie);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Occupied system plus two controlled battleground sites must flip (leg B)",
                objective.isFlipped());
    }

    @Test
    public void oaFrontIgnoresNonBattlegroundSitesAndSingleSites() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var reysCamp = scn.GetLSCard("reysCamp");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToJakku(scn, reysCamp);
        // One battleground site controlled plus Rey's Encampment (light-only
        // icons, NOT a battleground) occupied: the count-2 leg stays unmet.
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(reysCamp, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("A non-battleground site must not satisfy the two-site leg",
                objective.isFlipped());
    }

    @Test
    public void oaBackHoldsAtTwoBattlegroundsAndFlipsBackBelow() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var reysCamp = scn.GetLSCard("reysCamp");
        var pulseOne = scn.GetLSFiller(3);
        var pulseTwo = scn.GetLSFiller(4);
        var pulseThree = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        // Pulses must land on a NON-battleground (Rey's Encampment) or the
        // pulse bodies themselves keep the occupy-2 hold satisfied.
        moveLocationToJakku(scn, reysCamp);
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(16);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(pulseOne, reysCamp);
        assertTrue("Leg B must flip", objective.isFlipped());
        scn.DSPass();

        // Occupying exactly two battlegrounds (shipyard + ravager) after the
        // system empties still holds the back.
        scn.MoveOutOfPlay(xwing);
        scn.LSDeployCardAndPassResponses(pulseTwo, reysCamp);
        assertTrue("Two occupied battlegrounds must hold the back",
                objective.isFlipped());
        scn.DSPass();

        // Dropping to one occupied battleground flips the back to front.
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.LSDeployCardAndPassResponses(pulseThree, reysCamp);
        assertFalse("Occupying fewer than two battlegrounds must flip back",
                objective.isFlipped());
    }

    @Test
    public void oaProfileRulesTrackTheEngineLaw() {
        var scn = oaScenario();
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(system, xwing);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 204_32", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("OA front encodes the two cross-paired legs", 2,
                preFlip.size());
        assertEquals("One site plus the system satisfies neither leg", 0,
                preFlip.stream().filter(
                        state -> state.conditionSatisfied()).count());

        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Sole control of everything satisfies at least leg B",
                complete.stream().anyMatch(
                        state -> state.conditionSatisfied()));

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one hold rule", 1, postFlip.size());
    }
}
