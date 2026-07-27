package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
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
 * Batch Nine (2026-07-27): native engine contract for the Bespin control
 * family. Both objectives' card Java is UNCHANGED; these tests prove the real
 * flip and flip-back law that the new profile flipLocationRules encode.
 *
 * Card law (BATCH_NINE_SOURCE_LAW_2026-07-27.md):
 * - QMC 109_4 front: no opponent control of any Bespin location, you control
 *   the Bespin: Cloud City SECTOR, and (two controlled Cloud City sites OR
 *   Lando/Lobot on Cloud City with one controlled Cloud City site).
 * - QMC back: flips back on opponent control of Bespin system OR opponent
 *   control of three Cloud City sites and/or Bespin cloud sectors.
 * - CITC 301_2 front: two controlled Cloud City battleground sites, occupy
 *   Bespin system, no opponent-controlled Cloud City site.
 * - CITC back: flips back when opponent controls strictly more Cloud City
 *   sites than you.
 */
public class BespinObjectiveEngineContractTest {

    private static final StartingSetup QUIET_MINING_COLONY = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "109_4");
                put("system", "5_76");
                put("carbonite", "5_78");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // The required site deploy searches the whole Reserve Deck; the
            // extra Cloud City sites in the test deck make it a multi-match
            // ARBITRARY_CARDS prompt ("Choose Cloud City ... to deploy").
            // Always pick the designated starting site, then answer side
            // placement; decline the optional Weather Vane deploy.
            for (int i = 0; i < 6; i++) {
                if (scn.LSDecisionAvailable("site to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("carbonite"));
                }
                if (scn.LSDecisionAvailable("Weather Vane")) {
                    scn.LSDecided("");
                }
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        }
    };

    private static final StartingSetup CITY_IN_THE_CLOUDS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "301_2");
                put("system", "5_76");
                put("carbonite", "5_78");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // The required site deploy searches the whole Reserve Deck; the
            // extra Cloud City sites in the test deck make it a multi-match
            // ARBITRARY_CARDS prompt ("Choose Cloud City ... to deploy").
            // Always pick the designated starting site, then answer side
            // placement; decline the optional Weather Vane deploy.
            for (int i = 0; i < 6; i++) {
                if (scn.LSDecisionAvailable("site to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("carbonite"));
                }
                if (scn.LSDecisionAvailable("Weather Vane")) {
                    scn.LSDecided("");
                }
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        }
    };

    private VirtualTableScenario qmcScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("guest", "5_80");
                    put("incinerator", "5_81");
                    put("sector", "5_77");
                    put("lando", "5_5");
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                }},
                24,
                24,
                QUIET_MINING_COLONY,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private VirtualTableScenario citcScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("guest", "5_80");
                    put("incinerator", "5_81");
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                }},
                24,
                24,
                CITY_IN_THE_CLOUDS,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToBespin(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Bespin, null);
        assertFalse("Expected a legal placement at Bespin",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    // ==================== QMC 109_4 front ====================

    @Test
    public void qmcFrontRequiresSectorControlAndTwoCloudCitySites() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);
        var secondSiteBody = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulseOne, secondSiteBody);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(sector, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, sector));
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Sector control plus one Cloud City site must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(secondSiteBody, guest);
        assertTrue("Sector control plus two controlled Cloud City sites must flip",
                objective.isFlipped());
    }

    @Test
    public void qmcFrontIsBlockedByOpponentControlOfAnyBespinLocation() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetLSFiller(3);
        var pulseTwo = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(sector, xwing);
        scn.MoveCardsToLocation(system, tie);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, system));
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Opponent control of Bespin system must block the flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(tie);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Removing the opponent's Bespin control must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void qmcFrontLandoOnCloudCityReducesTheSiteCountToOne() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var lando = scn.GetLSCard("lando");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, lando);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        // Contest guest quarters so Lando's presence there does NOT create a
        // second controlled site: the flip must come from the Lando
        // alternative, not the two-site leg.
        scn.MoveCardsToLocation(guest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(sector, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("One controlled site without Lando or Lobot must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(lando, guest);
        assertFalse("Guest quarters stays contested",
                GameConditions.controls(
                        scn.game(), VirtualTableScenario.LS, guest));
        assertTrue("Lando on Cloud City with one controlled site must flip",
                objective.isFlipped());
    }

    // ==================== QMC 109_4 back ====================

    @Test
    public void qmcBackFlipsBackWhenOpponentControlsBespinSystem() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);
        var tie = scn.GetDSCard("tie");

        scn.MoveCardsToLSHand(pulseOne);
        scn.MoveCardsToDSHand(tie);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(sector, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Front conditions complete: the objective must flip",
                objective.isFlipped());
        scn.DSPass();

        // Give the opponent sole control of Bespin system.
        scn.MoveOutOfPlay(xwing);
        scn.DSActivateForceCheat(8);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(tie, system);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, system));
        assertFalse("Opponent control of Bespin system must flip the back to front",
                objective.isFlipped());
    }

    // ==================== CITC 301_2 front ====================

    @Test
    public void citcFrontRequiresTwoBattlegroundSitesAndBespinOccupation() {
        var scn = citcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, xwing);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Two battleground sites without Bespin occupation must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(xwing, system);
        assertTrue("Occupying Bespin system with two controlled battleground sites must flip",
                objective.isFlipped());
    }

    @Test
    public void citcFrontIsBlockedByAnOpponentControlledCloudCitySite() {
        var scn = citcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var incinerator = scn.GetLSCard("incinerator");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);
        var pulseTwo = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, incinerator);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, xwing);
        scn.MoveCardsToLocation(incinerator, scn.GetDSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, incinerator));
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("An opponent-controlled Cloud City site must block the flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Clearing the opponent's Cloud City control must allow the flip",
                objective.isFlipped());
    }

    // ==================== CITC 301_2 back ====================

    @Test
    public void citcBackFlipsBackOnlyWhenOpponentControlsStrictlyMoreSites() {
        var scn = citcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var incinerator = scn.GetLSCard("incinerator");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);
        var firstTrooper = scn.GetDSFiller(1);
        var secondTrooper = scn.GetDSFiller(2);

        scn.MoveCardsToLSHand(pulseOne);
        scn.MoveCardsToDSHand(firstTrooper, secondTrooper);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, incinerator);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Front conditions complete: the objective must flip",
                objective.isFlipped());
        scn.DSPass();

        // Drop to one controlled site, then let the opponent take two.
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(firstTrooper, guest);
        assertTrue("Equal site counts (1-1) must keep the back stable",
                objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(secondTrooper, incinerator);
        assertFalse("Strictly more opponent-controlled Cloud City sites must flip back",
                objective.isFlipped());
    }

    // ==================== profile facts layer ====================

    @Test
    public void qmcProfileRulesTrackTheEngineLaw() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(sector, xwing);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 109_4", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("QMC front encodes two rules", 2, preFlip.size());
        // Rule 1 (sector control + no opponent Bespin control) is satisfied;
        // rule 2 (two sites or Lando alternative) is still missing.
        long satisfied = preFlip.stream()
                .filter(state -> state.conditionSatisfied()).count();
        assertEquals("Exactly the sector-control rule is satisfied at one site",
                1, satisfied);

        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("Both rules satisfied at two controlled sites", 2,
                complete.stream().filter(
                        state -> state.conditionSatisfied()).count());
        assertFalse("Facts assessment must not itself flip the card",
                objective.isFlipped());
    }

    @Test
    public void citcProfileRulesTrackTheEngineLaw() {
        var scn = citcScenario();
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var system = scn.GetLSCard("system");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 301_2", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("CITC front encodes one rule", 1, preFlip.size());
        assertFalse("Two sites without Bespin occupation leave the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(system, xwing);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Occupying Bespin completes the encoded law",
                complete.get(0).conditionSatisfied());
    }
}
