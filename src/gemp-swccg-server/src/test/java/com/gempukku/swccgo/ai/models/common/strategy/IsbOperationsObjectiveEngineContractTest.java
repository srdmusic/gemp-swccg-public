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
 * Batch Nineteen (2026-07-27): native engine contract for ISB Operations /
 * Empire's Sinister Agents (7_299, DARK). Card Java unchanged. NOT a twin
 * of the Local Uprising pair — batched by set adjacency only; its flip
 * machinery is a two-route anyOf that the twins do not share.
 *
 * Law (Card7_299.java L75-L76): flips when four ISB agents are spottable
 * anywhere on table OR ISB agents control two Rebel Base locations
 * (partOfSystem Yavin 4 or Hoth — a computed membership, battleground not
 * required). Keyword.ISB_AGENT exists only as this objective's own grant
 * to your characters with ISB/Rebel/Rebellion lore (both sides re-grant
 * it). Back (Card7_299_BACK.java L85): flips back when NO ISB agent is
 * spottable — pure absence, no location. Recorded hole, untested here:
 * the objective's own SPY grant makes agents undercover-capable, and
 * undercover agents are invisible to all legs including the back hold.
 * No hard-loss on either side. Fixture agents are Outer Rim Scouts
 * (7_195, non-unique, "employed by the ISB ... Rebel activity" lore);
 * stormtrooper fillers carry no qualifying lore and stay non-agents.
 */
public class IsbOperationsObjectiveEngineContractTest {

    private static final StartingSetup ISB_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_299");
                put("corusDb", "7_276");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // One required deploy: any Coruscant location, free.
            for (int i = 0; i < 6; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("corusDb"));
                }
            }
        }
    };

    private VirtualTableScenario isboScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("agent1", "7_195");
                    put("agent2", "7_195");
                    put("agent3", "7_195");
                    put("agent4", "7_195");
                    put("yavinDb", "1_297");
                    put("yavinJungle", "1_298");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                ISB_OPERATIONS,
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
    public void isboRouteAFlipsOnFourAgentsAnywhere() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Three lore-qualified agents: the objective's own keyword grant
        // makes them ISB agents, one short of route A.
        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"), scn.GetDSCard("agent3"));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Three ISB agents must not flip (stormtrooper pulses do not count)",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent4"));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("A fourth ISB agent anywhere must flip via route A",
                objective.isFlipped());
    }

    @Test
    public void isboRouteBFlipsOnTwoRebelBaseLocationsControlledWithAgents() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent2 = scn.GetDSCard("agent2");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        // Two agents only — route A stays far out of reach; one Rebel Base
        // site is controlled, the other still empty.
        scn.MoveCardsToLocation(yavinDb, scn.GetDSCard("agent1"));
        scn.MoveCardsToLocation(corusDb, agent2);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("One agent-controlled Rebel Base location must not flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(yavinJungle, agent2);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Two agent-controlled Rebel Base locations must flip via route B with only two agents on table",
                objective.isFlipped());
    }

    @Test
    public void isboBackFlipsBackWhenNoAgentRemains() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"), scn.GetDSCard("agent3"));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent4"));
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Four agents must flip", objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(scn.GetDSCard("agent1"));
        scn.MoveOutOfPlay(scn.GetDSCard("agent2"));
        scn.MoveOutOfPlay(scn.GetDSCard("agent3"));
        scn.MoveOutOfPlay(scn.GetDSCard("agent4"));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("No spottable ISB agent must flip the back to front (non-agent troopers do not hold it)",
                objective.isFlipped());
    }

    @Test
    public void isboProfileRulesTrackTheEngineLaw() {
        var scn = isboScenario();
        var corusDb = scn.GetDSCard("corusDb");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 7_299", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("ISBO front encodes one two-route anyOf rule", 1,
                preFlip.size());
        assertFalse("With no agents the encoded law is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"), scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Four agents complete route A of the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one pure-absence rule", 1,
                postFlip.size());
        assertFalse("With agents on table the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }
}
