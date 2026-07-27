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
 * Batch Eleven (2026-07-27): native engine contract for My Lord, Is That
 * Legal? / I Will Make It Legal (12_179, DARK). Card Java unchanged.
 *
 * Law (Card12_179.java L165-L185): flips when three of your senators are at
 * the Galactic Senate, or two are and at least one of them carries a
 * blockade agenda. Presence semantics: keyword senators only, captives do
 * not count, no opponent constraint of any kind. Back (Card12_179_BACK.java
 * L167-L183): flips back when fewer than two of your senators remain there.
 */
public class MyLordObjectiveEngineContractTest {

    private static final StartingSetup MY_LORD_IS_THAT_LEGAL = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "12_179");
                put("senate", "12_167");
                put("dockingBay", "12_166");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: the Galactic Senate and one other
            // Episode I location (single matches; answer placement).
            // Both required deploys auto-select their single match; only the
            // side-placement prompt (whose text also contains "deploy")
            // needs answering, so check it FIRST.
            for (int i = 0; i < 6; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("senate"));
                }
            }
        }
    };

    private VirtualTableScenario mlitlScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("ornFreeTaa", "12_113");
                    put("tikkes", "12_122");
                    put("yebYeb", "12_126");
                    put("aksMoe", "12_97");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                MY_LORD_IS_THAT_LEGAL,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void mlitlFrontFlipsOnThreePlainSenatorsAtTheSenate() {
        var scn = mlitlScenario();
        var objective = scn.GetDSCard("objective");
        var senate = scn.GetDSCard("senate");
        var ornFreeTaa = scn.GetDSCard("ornFreeTaa");
        var tikkes = scn.GetDSCard("tikkes");
        var yebYeb = scn.GetDSCard("yebYeb");

        scn.MoveCardsToDSHand(ornFreeTaa, tikkes, yebYeb);
        scn.StartGame();
        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(ornFreeTaa, senate);
        assertFalse("One senator must not flip", objective.isFlipped());
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(tikkes, senate);
        assertFalse("Two agenda-less senators must not flip",
                objective.isFlipped());
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(yebYeb, senate);
        assertTrue("Three of your senators at the Senate must flip",
                objective.isFlipped());
    }

    @Test
    public void mlitlFrontFlipsOnTwoSenatorsWithABlockadeAgenda() {
        var scn = mlitlScenario();
        var objective = scn.GetDSCard("objective");
        var senate = scn.GetDSCard("senate");
        var tikkes = scn.GetDSCard("tikkes");
        var aksMoe = scn.GetDSCard("aksMoe");

        scn.MoveCardsToDSHand(tikkes, aksMoe);
        scn.StartGame();
        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(tikkes, senate);
        assertFalse("One senator must not flip", objective.isFlipped());
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(aksMoe, senate);
        assertTrue("Two senators with a blockade agenda among them must flip",
                objective.isFlipped());
    }

    @Test
    public void mlitlFrontIgnoresNonSenatorsAtTheSenate() {
        var scn = mlitlScenario();
        var objective = scn.GetDSCard("objective");
        var senate = scn.GetDSCard("senate");
        var ornFreeTaa = scn.GetDSCard("ornFreeTaa");
        var tikkes = scn.GetDSCard("tikkes");

        scn.MoveCardsToDSHand(ornFreeTaa, tikkes);
        scn.StartGame();
        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(ornFreeTaa, senate);
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(tikkes, senate);
        assertFalse("Two agenda-less senators must not flip",
                objective.isFlipped());
        scn.LSPass();

        // A non-senator body at the Senate contributes nothing.
        scn.MoveCardsToLocation(senate, scn.GetDSFiller(1));
        scn.DSPass();
        assertFalse("A non-senator at the Senate must not complete the count",
                objective.isFlipped());
    }

    @Test
    public void mlitlBackFlipsBackBelowTwoSenatorsAndHoldsAtTwo() {
        var scn = mlitlScenario();
        var objective = scn.GetDSCard("objective");
        var senate = scn.GetDSCard("senate");
        var dockingBay = scn.GetDSCard("dockingBay");
        var ornFreeTaa = scn.GetDSCard("ornFreeTaa");
        var tikkes = scn.GetDSCard("tikkes");
        var yebYeb = scn.GetDSCard("yebYeb");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(ornFreeTaa, tikkes, yebYeb, pulseOne, pulseTwo);
        scn.StartGame();
        scn.DSActivateForceCheat(20);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(ornFreeTaa, senate);
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(tikkes, senate);
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(yebYeb, senate);
        assertTrue("Three senators must flip", objective.isFlipped());
        scn.LSPass();

        // Losing one senator leaves two: pulse the table, the back holds.
        scn.MoveOutOfPlay(yebYeb);
        scn.DSDeployCardAndPassResponses(pulseOne, dockingBay);
        assertTrue("Two senators remaining must hold the back",
                objective.isFlipped());
        scn.LSPass();

        // Losing another drops below two: pulse again, the back flips.
        scn.MoveOutOfPlay(tikkes);
        scn.DSDeployCardAndPassResponses(pulseTwo, dockingBay);
        assertFalse("Fewer than two senators must flip the back to front",
                objective.isFlipped());
    }

    @Test
    public void mlitlProfileRulesTrackTheEngineLaw() {
        var scn = mlitlScenario();
        var senate = scn.GetDSCard("senate");
        var ornFreeTaa = scn.GetDSCard("ornFreeTaa");
        var tikkes = scn.GetDSCard("tikkes");
        var aksMoe = scn.GetDSCard("aksMoe");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 12_179", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("MLITL front encodes two CNF rules", 2, preFlip.size());
        assertEquals("Nothing satisfied with an empty Senate", 0,
                preFlip.stream().filter(
                        state -> state.conditionSatisfied()).count());

        // Two agenda-less senators: floor rule satisfied, alternative not.
        scn.MoveCardsToLocation(senate, ornFreeTaa, tikkes);
        var floorOnly = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("Two plain senators satisfy only the floor rule", 1,
                floorOnly.stream().filter(
                        state -> state.conditionSatisfied()).count());

        // Adding a blockade-agenda senator completes the alternative rule.
        scn.MoveCardsToLocation(senate, aksMoe);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("Blockade senator completes both rules", 2,
                complete.stream().filter(
                        state -> state.conditionSatisfied()).count());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one hold rule", 1, postFlip.size());
    }
}
