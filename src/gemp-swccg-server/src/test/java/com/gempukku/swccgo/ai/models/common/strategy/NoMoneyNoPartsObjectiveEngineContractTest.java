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
 * Batch Eighteen (2026-07-27): native engine contract for No Money, No
 * Parts, No Deal! / You're A Slave? (12_180, DARK). Card Java unchanged.
 * FALSE TWIN of 110_4 (opposite sides, structurally different laws);
 * batched together, tested separately.
 *
 * Law (Card12_180.java L163-L164): flips when Watto (a TITLE filter — no
 * Persona.WATTO exists) is PRESENT AT Watto's Junkyard AND you OCCUPY Mos
 * Espa. Occupy, not control: opponent presence at Mos Espa does not block
 * it. Back (Card12_180_BACK.java L225-L226): the exact De Morgan negation —
 * losing EITHER leg flips it back. No hard-loss handler on either side.
 */
public class NoMoneyNoPartsObjectiveEngineContractTest {

    private static final StartingSetup NO_MONEY = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "12_180");
                put("junkyard", "12_178");
                put("mosEspa", "11_93");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required pulls in order: Watto's Junkyard, then Mos Espa.
            // Answer "On which side" first (its text also contains
            // "deploy"); pick whichever site is still in Reserve on the
            // card-choice prompts.
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    if (scn.GetDSCard("junkyard").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("junkyard"));
                    } else {
                        scn.DSChooseCard(scn.GetDSCard("mosEspa"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario noMoneyScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("watto", "11_65");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                NO_MONEY,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void nmnpndFrontRequiresBothLegs() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Watto present at the Junkyard, Mos Espa empty: leg 2 open, so the
        // board is not flip-complete before the phase skip.
        scn.MoveCardsToLocation(junkyard, watto);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Watto at the Junkyard without Mos Espa occupation must not flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Watto at the Junkyard plus Mos Espa occupation must flip",
                objective.isFlipped());
    }

    @Test
    public void nmnpndWattoMustBePresentAtTheJunkyardSpecifically() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Watto at the WRONG site (Mos Espa, which he also occupies for the
        // owner): leg 1 is site-exact and must hold the flip.
        scn.MoveCardsToLocation(mosEspa, watto);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Watto away from the Junkyard must not flip, even with Mos Espa occupied",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(junkyard, watto);
        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Watto restored to the Junkyard must flip",
                objective.isFlipped());
    }

    @Test
    public void nmnpndMosEspaLegIsOccupyNotControl() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var pulseOne = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne);
        scn.StartGame();
        // Opponent presence at Mos Espa denies CONTROL but not OCCUPATION.
        // Junkyard leg ready, Mos Espa contested but empty of ours: still
        // not flip-complete before the skip.
        scn.MoveCardsToLocation(junkyard, watto);
        scn.MoveCardsToLocation(mosEspa, scn.GetLSFiller(1));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Occupation under opponent presence must still flip (occupy, not control)",
                objective.isFlipped());
    }

    @Test
    public void nmnpndBackFlipsBackOnLosingEitherLeg() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var occupier = scn.GetDSFiller(1);
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);
        var pulseThree = scn.GetDSFiller(4);
        var pulseFour = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree, pulseFour);
        scn.StartGame();
        scn.MoveCardsToLocation(junkyard, watto);

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.MoveCardsToLocation(mosEspa, occupier);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Both legs together must flip", objective.isFlipped());
        scn.LSPass();

        // Leg 1 lost: Watto leaves the Junkyard.
        scn.MoveOutOfPlay(watto);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("Losing Watto at the Junkyard must flip the back to front",
                objective.isFlipped());
        scn.LSPass();

        // Both legs restored: the front flips again.
        scn.MoveCardsToLocation(junkyard, watto);
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertTrue("Restoring both legs must flip again",
                objective.isFlipped());
        scn.LSPass();

        // Leg 2 lost: Mos Espa occupation ends.
        scn.MoveOutOfPlay(occupier);
        scn.DSDeployCardAndPassResponses(
                pulseFour, scn.GetLSStartingLocation());
        assertFalse("Losing Mos Espa occupation must flip the back to front",
                objective.isFlipped());
    }

    @Test
    public void nmnpndProfileRulesTrackTheEngineLaw() {
        var scn = noMoneyScenario();
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 12_180", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("NMNPND front encodes one two-leg rule", 1,
                preFlip.size());
        assertFalse("With Watto off-site and Mos Espa empty the law is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(junkyard, watto);
        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Both legs complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one De Morgan anyOf rule", 1,
                postFlip.size());
        assertFalse("With both legs held the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }
}
