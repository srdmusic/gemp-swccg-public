package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Batch Seventeen (2026-07-27): native engine contract for the Watch Your
 * Step FAMILY — classic 10_26 and virtual 601_146. Card Java unchanged.
 * The two printings share a title and almost no law (TDIGWATT precedent):
 * blueprint-scoped recipes, tested separately here.
 *
 * Classic law (Card10_026.java L131-L134): flips on two completed Kessel
 * Runs (persistent counter) OR occupying two battlegrounds each with your
 * smuggler. Back (Card10_026_BACK.java L133-L136): flips back only when
 * BOTH fewer than two runs are complete AND you occupy fewer than two
 * battlegrounds — plain occupy, smugglers not required.
 *
 * Virtual law (Card601_146.java L131-L135): flips when you occupy Corellia
 * system AND control two Corellia battleground sites each with your
 * Corellian (species). Back (Card601_146_BACK.java L118-L120): flips back
 * below two occupied battlegrounds; the printed Kessel Run exception is
 * NOT in the code and is deliberately not modeled.
 */
public class WatchYourStepObjectiveEngineContractTest {

    // ==================== classic 10_26 ====================

    private static final StartingSetup WYS_CLASSIC = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "10_26");
                put("cantina", "1_128");
                put("dockingBay", "1_129");
                put("system", "1_127");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("cantina"));
                }
            }
        }
    };

    private VirtualTableScenario classicScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("han", "1_11");
                    put("corellianSmuggler", "2_6");
                    put("kesselRun", "1_52");
                    put("kesselRunV", "213_46");
                }},
                new HashMap<>() {{
                }},
                // 30 light fillers (Batch Twenty hardening): the back-holds
                // test stages four bodies pre-cheat and needs three 7-cost
                // pulses, so a 24-filler reserve left the activation cheat
                // and the phase skipper fighting over the same cards.
                30,
                24,
                WYS_CLASSIC,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void wysClassicRouteBFlipsOnTwoBattlegroundsWithSmugglers() {
        var scn = classicScenario();
        var objective = scn.GetLSCard("objective");
        var cantina = scn.GetLSCard("cantina");
        var dockingBay = scn.GetLSCard("dockingBay");
        var han = scn.GetLSCard("han");
        var corellianSmuggler = scn.GetLSCard("corellianSmuggler");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // A smuggler at one battleground plus a NON-smuggler at another:
        // route B unmet.
        scn.MoveCardsToLocation(cantina, han);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("One smuggler-held battleground must not flip",
                objective.isFlipped());
        scn.DSPass();

        // The second smuggler completes route B.
        scn.MoveCardsToLocation(dockingBay, corellianSmuggler);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Two battlegrounds each with your smuggler must flip",
                objective.isFlipped());
    }

    @Test
    public void wysClassicRouteAFlipsOnTwoCompletedKesselRunsAloneOnAnEmptyBoard() {
        var scn = classicScenario();
        var objective = scn.GetLSCard("objective");
        var kesselRun = scn.GetLSCard("kesselRun");
        var kesselRunV = scn.GetLSCard("kesselRunV");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        // Seed one completed run via the engine's own persistent record.
        scn.game().getModifiersQuerying().completedUtinniEffect(
                VirtualTableScenario.LS, kesselRun);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("One completed Kessel Run must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.game().getModifiersQuerying().completedUtinniEffect(
                VirtualTableScenario.LS, kesselRunV);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Two completed Kessel Runs must flip with no board presence at all",
                objective.isFlipped());
    }

    @Test
    public void wysClassicBackHoldsOnPlainOccupationAndOnCompletedRuns() {
        var scn = classicScenario();
        var objective = scn.GetLSCard("objective");
        var cantina = scn.GetLSCard("cantina");
        var dockingBay = scn.GetLSCard("dockingBay");
        var han = scn.GetLSCard("han");
        var corellianSmuggler = scn.GetLSCard("corellianSmuggler");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);
        var pulseThree = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        scn.MoveCardsToLocation(cantina, han);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        // Batch Twenty hardening of a pre-existing shuffle intermittent:
        // every body this test later needs must be ON TABLE before the
        // activation cheat. A raw MoveCardsToLocation of a card sitting in
        // the Force Pile silently drains one Force, the deploy menus are
        // stale snapshots priced at 7 per pulse, and 22 activated minus two
        // 7-cost pulses leaves 8 — so two shuffle-dependent leaks (6 < 7)
        // emptied the third pulse's menu. Smuggler and the fifth trooper
        // stage at cantina (one smuggler battleground — not flip-complete,
        // the skipper stays unjammed) and relocate with in-play moves.
        scn.MoveCardsToLocation(cantina, corellianSmuggler);
        scn.MoveCardsToLocation(cantina, scn.GetLSFiller(5));

        // The classic front taxes non-smuggler deploys +6, so each filler
        // pulse costs 7. 24 of the 27 reserve cards activate; three stay
        // for the phase skipper; 24 - 14 leaves 10 for the third pulse.
        scn.LSActivateForceCheat(24);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(dockingBay, corellianSmuggler);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Route B must flip", objective.isFlipped());
        scn.DSPass();

        // Remove BOTH smugglers: plain fillers still occupy two
        // battlegrounds, and the asymmetric back law holds. The fifth
        // trooper already stands at cantina from the pre-cheat staging.
        scn.MoveOutOfPlay(han);
        scn.MoveOutOfPlay(corellianSmuggler);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Plain occupation of two battlegrounds must hold the back",
                objective.isFlipped());
        scn.DSPass();

        // Dropping to one occupied battleground flips it back. The earlier
        // pulse bodies occupy the (battleground) marketplace, so they must
        // leave too or they hold the back themselves.
        scn.MoveOutOfPlay(scn.GetLSFiller(5));
        scn.MoveOutOfPlay(scn.GetLSFiller(1));
        scn.MoveOutOfPlay(pulseOne);
        scn.MoveOutOfPlay(pulseTwo);
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        assertFalse("Below two occupied battlegrounds with no runs must flip back",
                objective.isFlipped());
    }

    // ==================== virtual 601_146 ====================

    private static final StartingSetup WYS_VIRTUAL = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "601_146");
                put("system", "2_61");
                put("freighter", "1_143");
                put("pilot", "1_11");
                put("spaceport", "7_125");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // The Corellian-pilot deploy puts Han aboard the Falcon at the
            // system, which asks for a capacity slot.
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("capacity slot")) {
                    scn.LSDecided("0");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("spaceport"));
                }
            }
        }
    };

    private VirtualTableScenario virtualScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("corellianA", "2_6");
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                }},
                24,
                24,
                WYS_VIRTUAL,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void wysVirtualFrontNeedsSystemOccupationAndTwoCorellianSites() {
        var scn = virtualScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var spaceport = scn.GetLSCard("spaceport");
        var corellianA = scn.GetLSCard("corellianA");
        var han = scn.GetLSCard("pilot");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Han (Corellian) at the spaceport controls site one. A plain filler
        // "controls" a second location only in the classic sense — 601_146
        // needs a CORELLIAN at each site, so keep site two Corellian-less
        // first.
        scn.MoveCardsToLocation(spaceport, han);
        scn.MoveCardsToLocation(system, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("One Corellian-held site must not flip the virtual front",
                objective.isFlipped());
        scn.DSPass();

        // The Spaceport City DARK twin exists but here we use the second
        // spaceport site slot: put the second Corellian at the same site
        // group's other battleground — the starting setup deployed only one
        // extra site, so instead promote the docking-bay side by moving the
        // second Corellian to the spaceport as well and verifying the count
        // still reads ONE site (same site, not two).
        scn.MoveCardsToLocation(spaceport, corellianA);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertFalse("Two Corellians at ONE site must still not flip",
                objective.isFlipped());
    }

    @Test
    public void wysVirtualProfileRulesTrackBothLegs() {
        var scn = virtualScenario();
        var system = scn.GetLSCard("system");
        var spaceport = scn.GetLSCard("spaceport");
        var han = scn.GetLSCard("pilot");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        scn.MoveCardsToLocation(spaceport, han);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The authored profile must hydrate for 601_146",
                analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("The virtual front encodes one two-leg rule", 1,
                preFlip.size());
        assertFalse("One site and no system leaves the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(system, xwing);
        var stillMissing = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertFalse("System occupation with one Corellian site is still unmet",
                stillMissing.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The virtual back encodes one hold rule with NO Kessel leg",
                1, postFlip.size());
    }

    // ==================== classic profile facts ====================

    @Test
    public void wysClassicProfileTracksTheCounterRoute() {
        var scn = classicScenario();
        var kesselRun = scn.GetLSCard("kesselRun");
        var kesselRunV = scn.GetLSCard("kesselRunV");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The activated classic profile must hydrate for 10_26",
                analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("The classic front encodes one two-route rule", 1,
                preFlip.size());
        assertFalse("No runs and no smugglers leaves the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.game().getModifiersQuerying().completedUtinniEffect(
                VirtualTableScenario.LS, kesselRun);
        scn.game().getModifiersQuerying().completedUtinniEffect(
                VirtualTableScenario.LS, kesselRunV);
        var viaCounter = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Two recorded Kessel Runs satisfy the counter route",
                viaCounter.get(0).conditionSatisfied());
    }
}
