package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Batch Nineteen (2026-07-27): native engine contract for the Local
 * Uprising TRUE TWINS — LIGHT 7_137 Local Uprising / Liberation and DARK
 * 7_298 Imperial Occupation / Imperial Control. Card Java unchanged. The
 * two printings differ by five tokens (side, titles, action id,
 * Subjugated/Renegade state, back companion filter); their flip machinery
 * is line-for-line identical, so 7_137 carries the full ladder and 7_298
 * proves the twin on the Renegade state.
 *
 * Law (Card7_137.java L110-L111 / Card7_298.java L110-L111): flips when
 * you control three battleground sites of the setup-chosen planet, each
 * WITH your matching operative. Back (both _BACK.java L128): flips back
 * when you occupy fewer than two of those sites — PLAIN occupy, operatives
 * not required: the control-3-with-operatives vs occupy-2-with-anything
 * asymmetry is the load-bearing fact. No hard-loss on any side.
 */
public class LocalUprisingTwinsObjectiveEngineContractTest {

    // ==================== LIGHT 7_137, Tibrin ====================

    private static final StartingSetup LOCAL_UPRISING = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_137");
                put("system", "6_87");
                put("desert", "7_119");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required deploys in order: any planet system (becomes the
            // Subjugated planet), then a generic site to it. Answer "On
            // which side" first; pick whichever required card the current
            // prompt actually offers.
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("Subjugated planet")
                        && scn.LSHasCardChoiceAvailable(
                            scn.GetLSCard("system"))) {
                    scn.LSChooseCard(scn.GetLSCard("system"));
                } else if (scn.LSDecisionAvailable("site to deploy")
                        && scn.LSHasCardChoiceAvailable(
                            scn.GetLSCard("desert"))) {
                    scn.LSChooseCard(scn.GetLSCard("desert"));
                }
            }
        }
    };

    private VirtualTableScenario luScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("forest", "7_121");
                    put("jungle", "7_122");
                    put("op1", "7_47");
                    put("op2", "7_47");
                    put("op3", "7_47");
                }},
                new HashMap<>() {{
                }},
                24,
                24,
                LOCAL_UPRISING,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveSiteToSystem(
            VirtualTableScenario scn, PhysicalCardImpl site, String system) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, system, null);
        assertFalse("Expected a legal placement at " + system,
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    @Test
    public void luFrontNeedsThreeSitesEachWithYourOperative() {
        var scn = luScenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, forest, Title.Tibrin);
        moveSiteToSystem(scn, jungle, Title.Tibrin);
        // ENGINE LAW ONE LEVEL DEEPER THAN THE CARD: an operative on its
        // matching planet cannot control a site alone — its ability is
        // excluded from the control total (LocationControl.java L372-L382).
        // Every site therefore holds operative + plain-trooper companion;
        // the third site starts with the companion only.
        scn.MoveCardsToLocation(desert, scn.GetLSCard("op1"), scn.GetLSFiller(1));
        scn.MoveCardsToLocation(forest, scn.GetLSCard("op2"), scn.GetLSFiller(6));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(7));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Three controlled sites with only two operatives must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(jungle, scn.GetLSCard("op3"));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Three sites each with operative plus companion must flip",
                objective.isFlipped());
    }

    @Test
    public void luFrontIsBlockedByOpponentPresence() {
        var scn = luScenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, forest, Title.Tibrin);
        moveSiteToSystem(scn, jungle, Title.Tibrin);
        scn.MoveCardsToLocation(desert, scn.GetLSCard("op1"), scn.GetLSFiller(3));
        scn.MoveCardsToLocation(forest, scn.GetLSCard("op2"), scn.GetLSFiller(4));
        scn.MoveCardsToLocation(jungle, scn.GetLSCard("op3"), scn.GetLSFiller(5));
        // Opponent presence at one site denies CONTROL there.
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Opponent presence at the third site must hold the flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Clearing the opponent must complete control and flip",
                objective.isFlipped());
    }

    @Test
    public void luBackHoldsOnPlainOccupationAndFlipsBackBelowTwo() {
        var scn = luScenario();
        var objective = scn.GetLSCard("objective");
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var op1 = scn.GetLSCard("op1");
        var op2 = scn.GetLSCard("op2");
        var op3 = scn.GetLSCard("op3");
        var pulseOne = scn.GetLSFiller(3);
        var pulseTwo = scn.GetLSFiller(4);
        var pulseThree = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToSystem(scn, forest, Title.Tibrin);
        moveSiteToSystem(scn, jungle, Title.Tibrin);
        // Operative + companion at each site (the operative rule: a lone
        // matching operative cannot control); the third operative arrives
        // after the phase skip so the board is not flip-complete early.
        scn.MoveCardsToLocation(desert, op1, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(forest, op2, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(6));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.MoveCardsToLocation(jungle, op3);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Three operative-plus-companion sites must flip",
                objective.isFlipped());
        scn.DSPass();

        // All operatives leave; three PLAIN troopers keep the sites
        // occupied. The back requires only occupation — it must hold.
        scn.MoveOutOfPlay(op1);
        scn.MoveOutOfPlay(op2);
        scn.MoveOutOfPlay(op3);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Plainly occupied sites must hold the back (no operatives needed)",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.MoveOutOfPlay(scn.GetLSFiller(6));
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        assertFalse("Occupying fewer than two sites must flip the back to front",
                objective.isFlipped());
    }

    @Test
    public void luProfileRulesTrackTheEngineLaw() {
        var scn = luScenario();
        var desert = scn.GetLSCard("desert");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");

        scn.StartGame();
        moveSiteToSystem(scn, forest, Title.Tibrin);
        moveSiteToSystem(scn, jungle, Title.Tibrin);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 7_137", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("LU front encodes one controlWith rule", 1,
                preFlip.size());
        assertFalse("Bare sites leave the encoded law unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(desert, scn.GetLSCard("op1"), scn.GetLSFiller(1));
        scn.MoveCardsToLocation(forest, scn.GetLSCard("op2"), scn.GetLSFiller(2));
        scn.MoveCardsToLocation(jungle, scn.GetLSCard("op3"), scn.GetLSFiller(3));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Three operative-plus-companion Tibrin sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one plain-occupy hold rule", 1,
                postFlip.size());
    }

    // ==================== DARK 7_298, Dantooine — twin proof ====================

    private static final StartingSetup IMPERIAL_OCCUPATION = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_298");
                put("system", "1_282");
                put("desert", "7_281");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("Renegade planet")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("system"))) {
                    scn.DSChooseCard(scn.GetDSCard("system"));
                } else if (scn.DSDecisionAvailable("site to deploy")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("desert"))) {
                    scn.DSChooseCard(scn.GetDSCard("desert"));
                }
            }
        }
    };

    private VirtualTableScenario ioScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("forest", "7_284");
                    put("jungle", "7_285");
                    put("op1", "7_174");
                    put("op2", "7_174");
                    put("op3", "7_174");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                IMPERIAL_OCCUPATION,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void ioTwinLawFlipsAndFlipsBackOnTheRenegadeState() {
        var scn = ioScenario();
        var objective = scn.GetDSCard("objective");
        var desert = scn.GetDSCard("desert");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var op3 = scn.GetDSCard("op3");
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);
        var pulseThree = scn.GetDSFiller(4);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToSystem(scn, forest, Title.Dantooine);
        moveSiteToSystem(scn, jungle, Title.Dantooine);
        // Operative + trooper companion at each site (a lone matching
        // operative cannot control — LocationControl.java L372-L382).
        scn.MoveCardsToLocation(desert, scn.GetDSCard("op1"), scn.GetDSFiller(1));
        scn.MoveCardsToLocation(forest, scn.GetDSCard("op2"), scn.GetDSFiller(5));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(6));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Two operative-held Dantooine sites must not flip the twin",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(jungle, op3);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Three operative-plus-companion Renegade-planet sites must flip the twin",
                objective.isFlipped());
        scn.LSPass();

        // Strip to one occupied site: below the occupy-2 hold.
        scn.MoveOutOfPlay(scn.GetDSCard("op1"));
        scn.MoveOutOfPlay(scn.GetDSCard("op2"));
        scn.MoveOutOfPlay(op3);
        scn.MoveOutOfPlay(scn.GetDSFiller(5));
        scn.MoveOutOfPlay(scn.GetDSFiller(6));
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertFalse("One occupied site must flip the twin's back to front",
                objective.isFlipped());
    }

    @Test
    public void ioProfileRulesTrackTheEngineLaw() {
        var scn = ioScenario();
        var desert = scn.GetDSCard("desert");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");

        scn.StartGame();
        moveSiteToSystem(scn, forest, Title.Dantooine);
        moveSiteToSystem(scn, jungle, Title.Dantooine);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 7_298", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("IO front encodes one controlWith rule", 1,
                preFlip.size());
        assertFalse("Bare sites leave the encoded law unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(desert, scn.GetDSCard("op1"), scn.GetDSFiller(1));
        scn.MoveCardsToLocation(forest, scn.GetDSCard("op2"), scn.GetDSFiller(2));
        scn.MoveCardsToLocation(jungle, scn.GetDSCard("op3"), scn.GetDSFiller(3));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Three operative-plus-companion Dantooine sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The twin's back encodes one plain-occupy hold rule", 1,
                postFlip.size());
    }
}
