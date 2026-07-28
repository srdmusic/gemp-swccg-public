package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Batch Eighteen (2026-07-27): native engine contract for You Can Either
 * Profit By This... / Or Be Destroyed (110_4, LIGHT). Card Java unchanged.
 * FALSE TWIN of 12_180 (opposite sides, structurally different laws);
 * batched together, tested separately.
 *
 * Law (Card110_004.java L142): flips when a non-captive Han can be spotted
 * on Tatooine — a single-leg, ownership-free character-state spot. The
 * objective's own setup deploys Han as an unattended FROZEN captive at
 * Audience Chamber (L79), so the front is a rescue gate. Back
 * (Card110_004_BACK.java L100): flips back only when NO non-captive Han is
 * spottable anywhere — the on-Tatooine leg is DROPPED, so a free Han off
 * Tatooine holds the back. Hard-loss (both sides): Tatooine system blown
 * away puts the objective out of play — inexpressible in the schema,
 * recorded, untested here.
 */
public class ProfitByThisObjectiveEngineContractTest {

    private static final StartingSetup PROFIT = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "110_4");
                put("palace", "7_131");
                put("chamber", "6_81");
                put("han", "1_11");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required pulls in order: Jabba's Palace, Audience Chamber,
            // then Han as an unattended frozen captive. Answer "On which
            // side" first (its text also contains "deploy"); pick whichever
            // required card is still in Reserve on the card-choice prompts.
            // The opponent's optional 0-2 alien deploy never prompts here:
            // the dark deck is stormtrooper fillers only.
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    if (scn.GetLSCard("palace").getZone() == Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("palace"));
                    } else if (scn.GetLSCard("chamber").getZone() == Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("chamber"));
                    } else {
                        scn.LSChooseCard(scn.GetLSCard("han"));
                    }
                } else if (scn.DSDecisionAvailable("alien")) {
                    scn.DSDecided("");
                }
            }
        }
    };

    private VirtualTableScenario profitScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                }},
                24,
                24,
                PROFIT,
                // Endor: Dark Forest — a NON-Tatooine site, so freed Han can
                // stand somewhere that must NOT satisfy the front's
                // on(Tatooine) leg. One light force icon admits the pulses.
                StartingSetup.DSStartingLocation("8_161"),
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    /**
     * Raw release: clears the frozen flag (isCaptive derives from
     * frozen/imprisoned/escorted) and normalizes Han as an active presence
     * at the given site. Raw manipulation never pulses isTableChanged; every
     * assertion that follows rides a real deploy pulse.
     */
    private void releaseHanTo(
            VirtualTableScenario scn,
            com.gempukku.swccgo.game.PhysicalCardImpl han,
            com.gempukku.swccgo.game.PhysicalCardImpl site) {
        han.setFrozen(false);
        scn.MoveCardsToLocation(site, han);
    }

    @Test
    public void ycepbtFrontIsARescueGateWhileHanStaysFrozen() {
        var scn = profitScenario();
        var objective = scn.GetLSCard("objective");
        var chamber = scn.GetLSCard("chamber");
        var han = scn.GetLSCard("han");
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // The fixture must BE what it claims: setup left Han a frozen
        // captive at the objective's own Audience Chamber.
        assertTrue("Setup must deploy Han as a frozen captive",
                han.isFrozen() && han.isCaptive());

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("A frozen Han on Tatooine must not flip the front",
                objective.isFlipped());
        scn.DSPass();

        releaseHanTo(scn, han, chamber);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("A freed Han at a Tatooine site must flip the front",
                objective.isFlipped());
    }

    @Test
    public void ycepbtFrontNeedsHanOnTatooineNotJustFree() {
        var scn = profitScenario();
        var objective = scn.GetLSCard("objective");
        var chamber = scn.GetLSCard("chamber");
        var han = scn.GetLSCard("han");
        var darkForest = scn.GetDSStartingLocation();
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        // Free Han OFF Tatooine: the on(Tatooine) leg must hold the front.
        releaseHanTo(scn, han, darkForest);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("A free Han off Tatooine must not flip the front",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(chamber, han);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Returning the free Han to a Tatooine site must flip",
                objective.isFlipped());
    }

    @Test
    public void ycepbtBackHoldsWithFreeHanAnywhereAndFlipsBackWhenHanIsGone() {
        var scn = profitScenario();
        var objective = scn.GetLSCard("objective");
        var chamber = scn.GetLSCard("chamber");
        var han = scn.GetLSCard("han");
        var darkForest = scn.GetDSStartingLocation();
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);
        var pulseThree = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        releaseHanTo(scn, han, chamber);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("A freed Han at a Tatooine site must flip the front",
                objective.isFlipped());
        scn.DSPass();

        // The back DROPS the on-Tatooine leg: a free Han off the planet
        // holds it. This is the asymmetry that distinguishes the back law
        // from the front law.
        scn.MoveCardsToLocation(darkForest, han);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("A free Han off Tatooine must hold the back",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(han);
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        assertFalse("No spottable non-captive Han must flip the back to front",
                objective.isFlipped());
    }

    @Test
    public void ycepbtProfileRulesTrackTheEngineLaw() {
        var scn = profitScenario();
        var chamber = scn.GetLSCard("chamber");
        var han = scn.GetLSCard("han");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 110_4", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("YCEPBT front encodes one single-leg rule", 1,
                preFlip.size());
        assertFalse("A frozen Han must leave the encoded law unmet",
                preFlip.get(0).conditionSatisfied());

        han.setFrozen(false);
        scn.MoveCardsToLocation(chamber, han);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("A freed Han at a Tatooine site completes the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one dropped-leg hold rule", 1,
                postFlip.size());
        assertFalse("A free Han anywhere keeps the flip-back condition unmet",
                postFlip.get(0).conditionSatisfied());
    }
}
