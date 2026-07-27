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
 * Batch Ten (2026-07-27): native engine contract for Massassi Base
 * Operations / One In A Million (111_4, LIGHT). Card Java unchanged.
 *
 * Law (Card111_004.java L134-L147): flips when you control at least three
 * Yavin 4 sites while the opponent controls fewer than three. The back has
 * NO flip-back: once flipped it is permanent (no flip trigger exists in
 * Card111_004_BACK.java), so the profile deliberately omits any postFlip
 * rule.
 */
public class MassassiObjectiveEngineContractTest {

    private static final StartingSetup MASSASSI_BASE_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "111_4");
                put("system", "1_135");
                put("dockingBay", "1_136");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Yavin 4 system (promptless) and the
            // exact Yavin 4: Docking Bay (single match; answer placement).
            for (int i = 0; i < 6; i++) {
                if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("dockingBay"));
                }
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        }
    };

    private VirtualTableScenario mboScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("jungle", "1_137");
                    put("warRoom", "1_139");
                    put("briefing", "2_67");
                    put("ruins", "2_68");
                    put("hq", "7_134");
                }},
                new HashMap<>() {{
                }},
                24,
                24,
                MASSASSI_BASE_OPERATIONS,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToYavin(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Yavin_4, null);
        assertFalse("Expected a legal placement at Yavin 4",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    @Test
    public void mboFrontRequiresThreeControlledYavinSites() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var pulseOne = scn.GetLSFiller(4);
        var thirdBody = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, thirdBody);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Two controlled Yavin 4 sites must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(thirdBody, warRoom);
        assertTrue("Three controlled Yavin 4 sites must flip",
                objective.isFlipped());
    }

    @Test
    public void mboFrontIsBlockedWhileOpponentAlsoControlsThreeSites() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var briefing = scn.GetLSCard("briefing");
        var ruins = scn.GetLSCard("ruins");
        var hq = scn.GetLSCard("hq");
        var pulseOne = scn.GetLSFiller(4);
        var pulseTwo = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        moveLocationToYavin(scn, briefing);
        moveLocationToYavin(scn, ruins);
        moveLocationToYavin(scn, hq);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.MoveCardsToLocation(briefing, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(ruins, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(hq, scn.GetDSFiller(3));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Opponent control of three Yavin 4 sites must block the flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Opponent dropping to two controlled sites must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void mboBackIsPermanentOnceFlipped() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var briefing = scn.GetLSCard("briefing");
        var ruins = scn.GetLSCard("ruins");
        var hq = scn.GetLSCard("hq");
        var pulseOne = scn.GetLSFiller(4);
        var dsPulse = scn.GetDSFiller(4);

        scn.MoveCardsToLSHand(pulseOne);
        scn.MoveCardsToDSHand(dsPulse);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        moveLocationToYavin(scn, briefing);
        moveLocationToYavin(scn, ruins);
        moveLocationToYavin(scn, hq);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Three controlled sites must flip the objective",
                objective.isFlipped());
        scn.DSPass();

        // Collapse the owner's board and hand the opponent three sites: the
        // back must hold anyway.
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.MoveOutOfPlay(scn.GetLSFiller(3));
        scn.DSActivateForceCheat(16);
        scn.SkipToDSTurn(Phase.DEPLOY);
        // Raw placement sidesteps the light sites' deploy restrictions;
        // the separate legal deploy below supplies the table-changed pulse.
        scn.MoveCardsToLocation(briefing, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(ruins, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(hq, scn.GetDSFiller(3));
        scn.DSDeployCardAndPassResponses(dsPulse, scn.GetDSStartingLocation());
        assertTrue("One In A Million has no flip-back: the back must be permanent",
                objective.isFlipped());
    }

    @Test
    public void mboProfileRulesTrackTheEngineLaw() {
        var scn = mboScenario();
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");

        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 111_4", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("MBO front encodes one rule", 1, preFlip.size());
        assertFalse("Two controlled sites leave the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Three controlled sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertTrue("The permanent back must expose no flip-back rule",
                postFlip.isEmpty());
    }
}
