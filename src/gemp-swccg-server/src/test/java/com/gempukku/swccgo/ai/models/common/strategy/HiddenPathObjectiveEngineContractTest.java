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
 * Batch Twenty (2026-07-27): native engine contract for The Hidden Path /
 * Gather Allies And Train (226_28, LIGHT, no twin printings). Card Java
 * unchanged.
 *
 * Law (Card226_028.java L151): flips when YOUR Jedi occupy two non-Mapuzo
 * sites — occupiesWith count 2 (at-least), presence not control, owner's
 * Jedi only, undercover excluded, IEFB. Jedi is the computed class (LIGHT
 * character with ability >= 6), not the Jedi_Survivor keyword; battleground
 * is NOT required. Back (Card226_028_BACK.java L124): the exact negation —
 * flips back below two. No hard-loss on either side. Fixture Jedi: Obi-Wan
 * 226_24 sits exactly on the ability-6 boundary; Quinlan Vos 226_26 is 7.
 * Jabiim: Path Operations Center 226_15 is a NON-battleground non-Mapuzo
 * site, proving plain sites satisfy the gate.
 */
public class HiddenPathObjectiveEngineContractTest {

    private static final StartingSetup HIDDEN_PATH = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "226_28");
                put("village", "226_21");
                put("safehouse", "226_22");
                put("corridor", "226_23");
                put("fallenOrder", "226_14");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Four required free deploys: the three Mapuzo sites, then the
            // Fallen Order Epic Event to the owner's side of table. Each
            // filter is single-match in this deck; answer side placement
            // and any single-card choice prompts as they appear.
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    if (scn.GetLSCard("village").getZone() == com.gempukku.swccgo.common.Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("village"));
                    } else if (scn.GetLSCard("safehouse").getZone() == com.gempukku.swccgo.common.Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("safehouse"));
                    } else if (scn.GetLSCard("corridor").getZone() == com.gempukku.swccgo.common.Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("corridor"));
                    } else {
                        scn.LSChooseCard(scn.GetLSCard("fallenOrder"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario hiddenPathScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("obiwan", "226_24");
                    put("quinlan", "226_26");
                    put("poc", "226_15");
                }},
                new HashMap<>() {{
                }},
                24,
                24,
                HIDDEN_PATH,
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

    /**
     * Fallen Order makes the flip pulse noisy: pre-flip it cancels every
     * Jedi Survivor's game text, and the flip drops its Hidden-Path title
     * gate, so the flip spawns required RESTORE triggers for every survivor
     * on table (an ordering decision when more than one), followed by a
     * RESTORED_GAME_TEXT optional-response window. All orderings reach the
     * same state — answer required windows with the first action, pass
     * optional windows, until the deploy menu returns.
     */
    private void resolveRequiredWindows(VirtualTableScenario scn) {
        for (int i = 0; i < 8; i++) {
            var decision = scn.LSGetDecision();
            if (decision != null && decision.getText() != null
                    && decision.getText().contains("Required responses")) {
                scn.PlayerDecided(VirtualTableScenario.LS, "0");
                continue;
            }
            var current = scn.GetCurrentDecision();
            if (current != null && current.getText() != null
                    && current.getText().toLowerCase().contains("optional response")) {
                scn.PassAllResponses();
                continue;
            }
            return;
        }
    }

    @Test
    public void thpFrontNeedsTwoNonMapuzoSitesEachWithYourJedi() {
        var scn = hiddenPathScenario();
        var objective = scn.GetLSCard("objective");
        var safehouse = scn.GetLSCard("safehouse");
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        // Two active near misses: a Jedi at a MAPUZO site never counts, and
        // a non-Jedi trooper at a legal site never counts. Only Obi-Wan
        // (ability exactly 6 — the boundary) at Marketplace is live.
        scn.MoveCardsToLocation(scn.GetDSStartingLocation(), obiwan);
        scn.MoveCardsToLocation(safehouse, quinlan);
        scn.MoveCardsToLocation(poc, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("A Jedi on Mapuzo and a non-Jedi off Mapuzo must not flip",
                objective.isFlipped());
        scn.DSPass();

        // Quinlan leaves Mapuzo for the NON-battleground Jabiim site:
        // plain non-Mapuzo sites satisfy the gate.
        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Two non-Mapuzo sites each with your Jedi must flip (battleground not required)",
                objective.isFlipped());
    }

    @Test
    public void thpBackFlipsBackBelowTwoAndReFlipsSymmetrically() {
        var scn = hiddenPathScenario();
        var objective = scn.GetLSCard("objective");
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);
        var pulseThree = scn.GetLSFiller(3);

        var pulseFour = scn.GetLSFiller(4);
        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree, pulseFour);
        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);
        // Quinlan starts on Mapuzo so Fallen Order's cancel trigger
        // resolves on the first pulse, before the flip pulse.
        scn.MoveCardsToLocation(scn.GetDSStartingLocation(), obiwan);
        scn.MoveCardsToLocation(scn.GetLSCard("safehouse"), quinlan);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertFalse("One Jedi-occupied non-Mapuzo site must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertTrue("Two Jedi-occupied non-Mapuzo sites must flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertFalse("Losing the second Jedi site must flip the back to front (exact negation)",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(poc, quinlan);
        scn.LSDeployCardAndPassResponses(
                pulseFour, scn.GetDSStartingLocation());
        resolveRequiredWindows(scn);
        assertTrue("Restoring the second Jedi site must flip again (symmetric law)",
                objective.isFlipped());
    }

    @Test
    public void thpProfileRulesTrackTheEngineLaw() {
        var scn = hiddenPathScenario();
        var poc = scn.GetLSCard("poc");
        var obiwan = scn.GetLSCard("obiwan");
        var quinlan = scn.GetLSCard("quinlan");

        scn.StartGame();
        moveSiteToSystem(scn, poc, Title.Jabiim);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 226_28", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("THP front encodes one occupyWith rule", 1,
                preFlip.size());
        assertFalse("With no Jedi placed the encoded law is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(scn.GetDSStartingLocation(), obiwan);
        scn.MoveCardsToLocation(poc, quinlan);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Two Jedi at two non-Mapuzo sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one negated hold rule", 1,
                postFlip.size());
        assertFalse("With both sites held the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }
}
