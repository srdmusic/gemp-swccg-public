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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Batch Twenty-One (2026-07-29): native engine contract for Set Your
 * Course For Alderaan / The Ultimate Power In The Universe (111_6, DARK —
 * the queue's LIGHT listing was wrong; no twin printings). Card Java
 * unchanged.
 *
 * This objective's law is EVENT-shaped, not board-state-shaped: the front
 * flips only when the Alderaan system is blown away (Card111_006.java
 * L190, isBlownAwayLastStep with includeBlownAway=true) — in practice by
 * the Commence Primary Ignition epic event; the back NEVER flips back by
 * its own text and is placed out of play if the Death Star system is
 * blown away (Card111_006_BACK.java L68-L76). The profile therefore
 * deliberately carries NO flipLocationRules (the blown-away event is the
 * deferred schema primitive) and ONE Endor-shaped hardLossLocationRule.
 * Known limits recorded, untested here: the external once-per-turn
 * Krennic flip route on An Inkling 209_41 (with its end-of-turn
 * auto-revert), and the back's native out-of-play (needs an Attack Run
 * fixture; the trigger/outcome pair is byte-shaped like the Endor-proven
 * one).
 */
public class SetYourCourseObjectiveEngineContractTest {

    private static final StartingSetup SYCFA = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "111_6");
                put("deathStar", "2_143");
                put("alderaan", "1_281");
                put("db327", "1_285");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Three required free deploys: Death Star system (parsec 0),
            // the Deployable_By_SYCFA slot (DARK Alderaan here), and
            // Docking Bay 327.
            for (int i = 0; i < 10; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    if (scn.GetDSCard("deathStar").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("deathStar"));
                    } else if (scn.GetDSCard("alderaan").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("alderaan"));
                    } else {
                        scn.DSChooseCard(scn.GetDSCard("db327"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario sycfaScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("superlaser", "2_161");
                    put("cpi", "2_130");
                    put("centralCore", "1_283");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                SYCFA,
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
    public void sycfaFrontFlipsWhenAlderaanIsBlownAwayByCommencePrimaryIgnition() {
        var scn = sycfaScenario();
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var alderaan = scn.GetDSCard("alderaan");
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");
        var centralCore = scn.GetDSCard("centralCore");

        scn.MoveCardsToDSHand(superlaser, cpi);
        scn.StartGame();
        assertFalse(objective.isFlipped());
        // Second opponent-free Death Star site: Y = 2, so a prepared
        // destiny of 7 makes (7 + 2 - 0) > 8. X = 0 (Alderaan has no
        // sites), and with no Rebel Base on table Z = 0.
        moveSiteToSystem(scn, centralCore, Title.Death_Star);
        // Raw orbit injection: the CPI availability check only reads the
        // mobile system's orbit string; the real epic event then runs
        // natively end to end.
        deathStar.setSystemOrbited(Title.Alderaan);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(superlaser, deathStar);
        assertTrue(scn.IsAttachedTo(deathStar, superlaser));

        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);
        scn.DSPlayCardAndPassResponses(cpi, "Attempt to 'blow away'");
        scn.PassAllResponses();

        assertTrue("A CPI total of 9 must actually blow away Alderaan",
                alderaan.isBlownAway());
        assertTrue("Alderaan blown away must flip the front natively",
                objective.isFlipped());
    }

    @Test
    public void sycfaProfileEncodesTheHardLossAndDeliberatelyNoFlipRules() {
        var scn = sycfaScenario();
        var deathStar = scn.GetDSCard("deathStar");
        var alderaan = scn.GetDSCard("alderaan");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 111_6", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("SYCFA deliberately encodes NO flip rules — the flip is the deferred blown-away primitive",
                preFlip.isEmpty());

        assertTrue("The Death Star system must be the encoded hard-loss location",
                analyzer.isObjectiveHardLossLocation(
                        scn.game(), VirtualTableScenario.DS, deathStar));
        assertFalse("Alderaan must not be a hard-loss location",
                analyzer.isObjectiveHardLossLocation(
                        scn.game(), VirtualTableScenario.DS, alderaan));
    }
}
