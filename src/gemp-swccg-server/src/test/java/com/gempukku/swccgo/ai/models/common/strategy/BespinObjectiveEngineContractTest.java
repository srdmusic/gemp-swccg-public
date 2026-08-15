package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.modifiers.ResetAbilityModifier;
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
                    put("falconLando", "109_2");
                    put("boushh", "110_001");
                    put("xwing", "1_146");
                    put("emptyShip", "1_145");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                    put("darkLando", "5_99");
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
                    put("emptyShip", "1_145");
                    put("pilot", "1_003");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                    put("carrier", "2_155");
                    put("escort", "1_180");
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

    @Test
    public void qmcBackProtectsAndNativelyEnforcesTheThirdCombinedLocation() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var carboniteGuard = scn.GetLSFiller(1);
        var guestGuard = scn.GetLSFiller(2);
        var pulse = scn.GetLSFiller(3);
        var darkCarbonite = scn.GetDSFiller(1);
        var darkGuest = scn.GetDSFiller(2);
        var tie = scn.GetDSCard("tie");

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, carboniteGuard);
        scn.MoveCardsToLocation(guest, guestGuard);
        scn.MoveCardsToLocation(sector, xwing);
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());

        // Dark controls two members of the back-side combined pool and would
        // gain the third if the sole Carbonite Chamber guard disappeared.
        scn.MoveOutOfPlay(guestGuard);
        scn.MoveOutOfPlay(xwing);
        scn.MoveCardsToLocation(carbonite, darkCarbonite);
        scn.MoveCardsToLocation(guest, darkGuest);
        scn.MoveCardsToLocation(sector, tie);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, guest));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, sector));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, carbonite));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The sole third-location blocker must be retained",
                analyzer.wouldDepartureTriggerFlipBack(
                        scn.game(), VirtualTableScenario.LS,
                        carboniteGuard));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS,
                        carboniteGuard));

        scn.MoveOutOfPlay(carboniteGuard);
        scn.DSActivateForceCheat(8);
        scn.SkipToDSTurn(Phase.DEPLOY);
        assertFalse("Dark control of three combined sites/sectors flips QMC back",
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

    @Test
    public void citcNativeAndProfileCountACaptiveOccupyingBespin() {
        var scn = citcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var captive = scn.GetLSCard("pilot");
        var carboniteGuard = scn.GetLSFiller(2);
        var guestGuard = scn.GetLSFiller(3);
        var pulse = scn.GetLSFiller(4);
        var escort = scn.GetDSCard("escort");

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        scn.MoveCardsToLocation(carbonite, carboniteGuard);
        scn.MoveCardsToLocation(guest, guestGuard);
        // Isolate the source's captive spotting override at the system. The
        // ordinary legal occupation chain is covered separately above; this
        // fixture proves that the native trigger and profile use the same
        // inactive-card visibility rule.
        scn.MoveCardsToLocation(system, escort, captive);
        scn.CaptureCardWith(escort, captive);

        assertTrue(captive.isCaptive());
        assertFalse("Ordinary spotting excludes the captive",
                scn.game().getModifiersQuerying().occupiesLocation(
                        scn.gameState(), system, VirtualTableScenario.LS,
                        SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE));
        assertTrue("CITC's exact source override counts the captive",
                scn.game().getModifiersQuerying().occupiesLocation(
                        scn.gameState(), system, VirtualTableScenario.LS,
                        SpotOverride.INCLUDE_CAPTIVE_AND_EXCLUDED_FROM_BATTLE));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var states = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals(1, states.size());
        assertTrue("The runtime profile must use the same captive override",
                states.getFirst().conditionSatisfied());

        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("The unchanged card source flips with captive Bespin occupation",
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
        assertEquals("QMC front encodes three independent Boolean legs", 3,
                preFlip.size());
        // Sector control and the mandatory one-site floor are satisfied;
        // the two-sites-or-Lando alternative is still missing.
        long satisfied = preFlip.stream()
                .filter(state -> state.conditionSatisfied()).count();
        assertEquals("Exactly two QMC legs are satisfied at one site",
                2, satisfied);

        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("All rules satisfied at two controlled sites", 3,
                complete.stream().filter(
                        state -> state.conditionSatisfied()).count());
        assertFalse("Facts assessment must not itself flip the card",
                objective.isFlipped());
    }

    @Test
    public void qmcProfileMatchesNativeLandoAtContestedSiteShortcut() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var lando = scn.GetLSCard("lando");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(guest, lando);
        scn.MoveCardsToLocation(sector, xwing);

        assertFalse("The Lando site remains contested",
                GameConditions.controls(
                        scn.game(), VirtualTableScenario.LS, guest));
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        var states = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("QMC has three independent Boolean legs", 3,
                states.size());
        assertEquals("The profile must match the native Lando shortcut", 3,
                states.stream().filter(
                        state -> state.conditionSatisfied()).count());
        assertFalse("Facts assessment must not itself flip the card",
                objective.isFlipped());
    }

    @Test
    public void qmcNativeAndProfileAcceptOpponentLandoAtAContestedSite() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var darkLando = scn.GetDSCard("darkLando");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(guest, darkLando);
        scn.MoveCardsToLocation(sector, xwing);

        assertFalse("Opponent Lando's site must remain contested",
                GameConditions.controls(
                        scn.game(), VirtualTableScenario.DS, guest));
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("Any owner's Lando satisfies the source actor leg", 3,
                analyzer.assessFlipLocationRules(
                        scn.game(), VirtualTableScenario.LS,
                        "preFlip", "flip").stream()
                        .filter(state -> state.conditionSatisfied()).count());

        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("The unchanged card source must flip on opponent Lando",
                objective.isFlipped());
    }

    @Test
    public void qmcOpponentLandoRouteReleasesARedundantSecondSiteGuard() {
        var scn = qmcScenario();
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var incinerator = scn.GetLSCard("incinerator");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var darkLando = scn.GetDSCard("darkLando");
        var redundantGuard = scn.GetLSFiller(2);

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, incinerator);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(incinerator, redundantGuard);
        scn.MoveCardsToLocation(
                guest, scn.GetLSFiller(3), darkLando);
        scn.MoveCardsToLocation(sector, xwing);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("Any-owner Lando keeps the one-site route alive",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS,
                        redundantGuard));

        scn.MoveOutOfPlay(redundantGuard);
        assertEquals("One site plus opponent Lando remains source-complete", 3,
                analyzer.assessFlipLocationRules(
                        scn.game(), VirtualTableScenario.LS,
                        "preFlip", "flip").stream()
                        .filter(state -> state.conditionSatisfied()).count());
    }

    @Test
    public void qmcLandoShortcutRejectsTheSectorAndStillRequiresOneSite() {
        var sectorOnly = qmcScenario();
        var carbonite = sectorOnly.GetLSCard("carbonite");
        var guest = sectorOnly.GetLSCard("guest");
        var sector = sectorOnly.GetLSCard("sector");
        var lando = sectorOnly.GetLSCard("lando");
        var xwing = sectorOnly.GetLSCard("xwing");

        sectorOnly.StartGame();
        moveLocationToBespin(sectorOnly, guest);
        moveLocationToBespin(sectorOnly, sector);
        sectorOnly.MoveCardsToLocation(carbonite, sectorOnly.GetLSFiller(1));
        sectorOnly.MoveCardsToLocation(sector, xwing, lando);
        var sectorAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        sectorAnalyzer.analyze(
                sectorOnly.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("Lando at the sector is not on a Cloud City site", 2,
                sectorAnalyzer.assessFlipLocationRules(
                        sectorOnly.game(), VirtualTableScenario.LS,
                        "preFlip", "flip").stream()
                        .filter(state -> state.conditionSatisfied()).count());

        var noControlledSite = qmcScenario();
        carbonite = noControlledSite.GetLSCard("carbonite");
        guest = noControlledSite.GetLSCard("guest");
        sector = noControlledSite.GetLSCard("sector");
        lando = noControlledSite.GetLSCard("lando");
        xwing = noControlledSite.GetLSCard("xwing");
        noControlledSite.StartGame();
        moveLocationToBespin(noControlledSite, guest);
        moveLocationToBespin(noControlledSite, sector);
        noControlledSite.MoveCardsToLocation(
                guest, noControlledSite.GetDSFiller(1), lando);
        noControlledSite.MoveCardsToLocation(sector, xwing);
        var floorAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        floorAnalyzer.analyze(
                noControlledSite.game(), VirtualTableScenario.LS,
                Side.LIGHT);
        assertEquals("Lando cannot bypass the mandatory controlled-site floor",
                2, floorAnalyzer.assessFlipLocationRules(
                        noControlledSite.game(), VirtualTableScenario.LS,
                        "preFlip", "flip").stream()
                        .filter(state -> state.conditionSatisfied()).count());
    }

    @Test
    public void qmcProfileRecognizesPermanentAboardLandoAtACloudCitySite() {
        var scn = qmcScenario();
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var falconLando = scn.GetLSCard("falconLando");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetDSFiller(1), falconLando);
        scn.MoveCardsToLocation(sector, xwing);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("Permanent-aboard Lando matches OnCloudCityCondition", 3,
                analyzer.assessFlipLocationRules(
                        scn.game(), VirtualTableScenario.LS,
                        "preFlip", "flip").stream()
                        .filter(state -> state.conditionSatisfied()).count());
    }

    @Test
    public void qmcPlainPresenceAdvancesAndProtectsTheSectorControlLeg() {
        var scn = qmcScenario();
        var carbonite = scn.GetLSCard("carbonite");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var emptyShip = scn.GetLSCard("emptyShip");

        scn.MoveCardsToLSHand(xwing, emptyShip);
        scn.StartGame();
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var xwingCandidateRole = analyzer.classifyPreFlipProgressCandidate(
                scn.game(), VirtualTableScenario.LS, xwing);
        assertEquals("The last legal hand presence source is retained",
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR,
                xwingCandidateRole);
        assertEquals("An empty ship is never protected as objective presence",
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                analyzer.classifyPreFlipProgressCandidate(
                        scn.game(), VirtualTableScenario.LS, emptyShip));
        assertTrue("A permanent-pilot ship advances missing sector control",
                analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.LS,
                        xwing, sector));
        assertTrue("The parent deploy action receives the bounded route preference",
                analyzer.getDeployObjectiveAdjustments(
                        scn.game(), scn.gameState(),
                        VirtualTableScenario.LS, xwing,
                        xwing.getBlueprint(), "Deploy X-wing")
                        .stream().anyMatch(note -> note.score == 300.0f
                                && note.reason.contains(
                                    "OBJECTIVE ACTOR LOCATION")));
        assertEquals("Movement uses the bounded exact-route preference",
                300.0f,
                MoveDestinationPolicy.objectiveActorLocationDestination(
                        analyzer
                            .advancesPreFlipPlainPresenceAtRequiredLocation(
                                scn.game(), VirtualTableScenario.LS,
                                xwing, sector),
                        xwing.getTitle(), sector.getTitle()).delta(),
                0.0f);
        assertFalse("An empty unpiloted ship supplies no presence",
                analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.LS,
                        emptyShip, sector));

        scn.MoveCardsToLocation(sector, xwing);
        var role = analyzer.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.LS, xwing);
        assertEquals("The sole sector presence source is part of the flip formation",
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                role);
        assertTrue("Movement cannot dismantle the sole sector controller",
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        true, role, 5.0f, 0.0f).hardVeto());
        assertEquals("Battle forfeit prefers keeping that same formation source",
                -300.0f,
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        "qmc-sector", role, true)
                        .operations().getFirst().delta(), 0.0f);

        var forceLoss = ForceLossPolicy.score(
                "qmc-xwing", ForceLossPolicy.Route.STANDALONE,
                new ForceLossFacts.DecisionFacts(
                        2, 10, 15, 0, 2, false),
                new ForceLossFacts.CandidateFacts(
                        xwing.getTitle(), "HAND",
                        ForceLossFacts.ZoneBand.HAND,
                        com.gempukku.swccgo.common.CardCategory.STARSHIP,
                        false, false, false, false, false),
                new ForceLossPolicy.ObjectiveFlags(
                        false, false,
                        xwingCandidateRole
                            == ObjectiveAnalyzer
                                .ObjectiveProgressCandidateRole
                                .REQUIRED_ACTOR,
                        false));
        assertTrue("The selected last legal route ship receives bounded Force-loss preference",
                forceLoss.operations().stream().anyMatch(operation ->
                        operation.ruleArmId().id().equals("V21-objective")
                                && operation.domainId()
                                    == TraceDomainId.OBJECTIVE_INTENT
                                && operation.delta() == -300.0f));

        scn.MoveCardsToLocation(sector, scn.GetLSCard("falconLando"));
        assertEquals("Redundant sector presence is released",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, xwing));
    }

    @Test
    public void qmcPresenceProjectionRejectsLiveZeroAbilityAndUndercover() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var reduced = scn.GetLSFiller(2);
        var boushh = scn.GetLSCard("boushh");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(sector, xwing);
        scn.MoveCardsToLocation(guest, reduced, boushh);
        scn.MakeCardGoUndercover(boushh);
        scn.game().getModifiersEnvironment().addUntilEndOfGameModifier(
                new ResetAbilityModifier(objective, reduced, 0));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals(0.0f, scn.game().getModifiersQuerying().getAbility(
                scn.gameState(), reduced), 0.0f);
        assertFalse("Printed ability cannot override a live ability reset",
                analyzer.advancesPreFlipPlainPresenceAtRequiredLocation(
                        scn.game(), VirtualTableScenario.LS,
                        reduced, guest));
        assertFalse("An undercover character supplies no objective presence",
                analyzer.advancesPreFlipPlainPresenceAtRequiredLocation(
                        scn.game(), VirtualTableScenario.LS,
                        boushh, guest));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, reduced));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, boushh));
    }

    @Test
    public void qmcBattleTargetsTheMissingCloudCitySiteOnlyWhenSafe() {
        var scn = qmcScenario();
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest,
                scn.GetLSFiller(2), scn.GetDSFiller(1));
        scn.MoveCardsToLocation(sector, xwing);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        boolean exactTarget = analyzer.isPreFlipFlipRequirementLocation(
                scn.game(), VirtualTableScenario.LS, guest);
        boolean missingControl = analyzer.isMissingPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.LS, guest);
        assertTrue(exactTarget);
        assertTrue(missingControl);

        var safe = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "qmc-guest", exactTarget, missingControl,
                        true, false, true,
                        0.0f, 5, 7.0f, 5.0f));
        assertEquals(ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                safe.operations().getFirst().delta(), 0.0f);
        assertTrue("Objective urgency never rescues a suicidal battle",
                ObjectiveBattlePolicy.evaluate(
                        new ObjectiveBattlePolicy.Facts(
                                "qmc-guest-suicide",
                                exactTarget, missingControl,
                                true, false, true,
                                -18.0f, 5, 2.0f, 20.0f))
                        .operations().isEmpty());
    }

    @Test
    public void qmcPostFlipRetainsTheSoleBespinSystemBlocker() {
        var scn = qmcScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var sector = scn.GetLSCard("sector");
        var xwing = scn.GetLSCard("xwing");
        var tie = scn.GetDSCard("tie");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        moveLocationToBespin(scn, sector);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(sector, xwing);
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());

        scn.MoveCardsToLocation(system, xwing, tie);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Removing X-wing gives Dark sole Bespin system control",
                analyzer.wouldDepartureTriggerFlipBack(
                        scn.game(), VirtualTableScenario.LS, xwing));
        var role = analyzer.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.LS, xwing);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                .LAST_FLIP_BACK_BLOCKER, role);
        assertEquals(-300.0f,
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        "qmc-system", role, true)
                        .operations().getFirst().delta(), 0.0f);
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

    @Test
    public void citcPlainPresenceAdvancesAndProtectsBespinOccupation() {
        var scn = citcScenario();
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var xwing = scn.GetLSCard("xwing");
        var emptyShip = scn.GetLSCard("emptyShip");

        scn.MoveCardsToLSHand(xwing, emptyShip);
        scn.StartGame();
        moveLocationToBespin(scn, guest);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("The last legal Bespin occupier in hand is retained",
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR,
                analyzer.classifyPreFlipProgressCandidate(
                        scn.game(), VirtualTableScenario.LS, xwing));
        assertEquals("The empty ship remains ordinary Force-loss fodder",
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                analyzer.classifyPreFlipProgressCandidate(
                        scn.game(), VirtualTableScenario.LS, emptyShip));
        assertTrue("A permanent-pilot ship advances missing Bespin occupation",
                analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.LS,
                        xwing, system));
        assertTrue("CITC parent deployment gives the occupier bounded preference",
                analyzer.getDeployObjectiveAdjustments(
                        scn.game(), scn.gameState(),
                        VirtualTableScenario.LS, xwing,
                        xwing.getBlueprint(), "Deploy X-wing")
                        .stream().anyMatch(note -> note.score == 300.0f
                                && note.reason.contains(
                                    "OBJECTIVE ACTOR LOCATION")));
        assertFalse("An empty unpiloted ship does not occupy Bespin",
                analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.LS,
                        emptyShip, system));

        scn.MoveCardsToLocation(system, xwing);
        assertEquals("The sole Bespin occupier is part of the flip formation",
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, xwing));
        var role = analyzer.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.LS, xwing);
        assertTrue("Movement preserves the sole Bespin occupier",
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        true, role, 5.0f, 0.0f).hardVeto());
        assertEquals("Battle forfeit prefers keeping the same occupier",
                -300.0f,
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        "citc-bespin", role, true)
                        .operations().getFirst().delta(), 0.0f);

        var forceLoss = ForceLossPolicy.score(
                "citc-xwing", ForceLossPolicy.Route.STANDALONE,
                new ForceLossFacts.DecisionFacts(
                        2, 10, 15, 0, 2, false),
                new ForceLossFacts.CandidateFacts(
                        xwing.getTitle(), "HAND",
                        ForceLossFacts.ZoneBand.HAND,
                        com.gempukku.swccgo.common.CardCategory.STARSHIP,
                        false, false, false, false, false),
                new ForceLossPolicy.ObjectiveFlags(
                        false, false, true, false));
        assertTrue("The last legal Bespin route ship receives bounded Force-loss preference",
                forceLoss.operations().stream().anyMatch(operation ->
                        operation.ruleArmId().id().equals("V21-objective")
                                && operation.domainId()
                                    == TraceDomainId.OBJECTIVE_INTENT
                                && operation.delta() == -300.0f));
    }

    @Test
    public void citcPilotIsProtectedWhenHisShipDependsOnHimForPresence() {
        var scn = citcScenario();
        var system = scn.GetLSCard("system");
        var carbonite = scn.GetLSCard("carbonite");
        var guest = scn.GetLSCard("guest");
        var emptyShip = scn.GetLSCard("emptyShip");
        var pilot = scn.GetLSCard("pilot");

        scn.StartGame();
        moveLocationToBespin(scn, guest);
        scn.MoveCardsToLocation(carbonite, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(guest, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, emptyShip);
        scn.BoardAsPilot(emptyShip, pilot);
        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.LS,
                Filters.sameCardId(system)));
        assertEquals(system, scn.game().getModifiersQuerying()
                .getLocationThatCardIsAt(
                        scn.gameState(), pilot));
        assertTrue(Filters.hasAbilityOrHasPermanentPilotWithAbility.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(), pilot));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue(analyzer.isSolePresenceSourceAtRequiredLocation(
                scn.game(), VirtualTableScenario.LS, pilot, system));
        assertEquals("The pilot is the ship's only presence source",
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, pilot));
        assertEquals("Losing the host also loses its only pilot",
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.LS, emptyShip));
    }
}
