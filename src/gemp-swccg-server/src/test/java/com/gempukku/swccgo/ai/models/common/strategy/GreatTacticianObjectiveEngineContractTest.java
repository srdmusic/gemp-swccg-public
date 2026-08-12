package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Native source-law baseline for 219_1. No AI behavior is asserted here. */
public class GreatTacticianObjectiveEngineContractTest {
    private static final StartingSetup GREAT_TACTICIAN = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "219_1");
                put("lothal", "219_10");
                put("laboratory", "219_11");
                put("complex", "219_13");
                put("collection", "219_20");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int guard = 0; guard < 24; guard++) {
                if (scn.DSHasCardChoiceAvailable(scn.GetDSCard("lothal"))) {
                    scn.DSChooseCard(scn.GetDSCard("lothal"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("laboratory"))) {
                    scn.DSChooseCard(scn.GetDSCard("laboratory"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("complex"))) {
                    scn.DSChooseCard(scn.GetDSCard("complex"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("collection"))) {
                    scn.DSChooseCard(scn.GetDSCard("collection"));
                } else if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else {
                    break;
                }
            }
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("art1", "1_19");
                    put("art2", "1_28");
                    put("lightPulse", "1_29");
                }},
                new HashMap<>() {{
                    put("thrawn", "10_40");
                    put("pulse1", "1_194");
                    put("pulse2", "1_194");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                GREAT_TACTICIAN,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void exactThrawnAndTwoArtworkRouteFlipsDuringDeploy() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var laboratory = scn.GetDSCard("laboratory");
        var collection = scn.GetDSCard("collection");
        var thrawn = scn.GetDSCard("thrawn");

        scn.MoveCardsToDSHand(thrawn);
        scn.StartGame();
        assertInZone(Zone.LOCATIONS, laboratory);
        assertInZone(Zone.SIDE_OF_TABLE, objective, collection);
        assertTrue(Filters.battleground.accepts(
                scn.game().getGameState(),
                scn.game().getModifiersQuerying(), laboratory));

        scn.StackCardsOn(collection,
                scn.GetLSCard("art1"), scn.GetLSCard("art2"));
        scn.DSActivateForceCheat(10);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(thrawn, laboratory);

        assertTrue("Thrawn at a battleground plus two artwork must flip 219_1",
                objective.isFlipped());
    }

    @Test
    public void eachFrontGateNearMissRemainsOnFront() {
        var oneArtwork = scenario();
        var oneObjective = oneArtwork.GetDSCard("objective");
        var oneLaboratory = oneArtwork.GetDSCard("laboratory");
        var oneCollection = oneArtwork.GetDSCard("collection");
        var oneThrawn = oneArtwork.GetDSCard("thrawn");
        oneArtwork.MoveCardsToDSHand(oneThrawn);
        oneArtwork.StartGame();
        oneArtwork.StackCardsOn(oneCollection, oneArtwork.GetLSCard("art1"));
        oneArtwork.DSActivateForceCheat(10);
        oneArtwork.SkipToPhase(Phase.DEPLOY);
        oneArtwork.DSDeployCardAndPassResponses(oneThrawn, oneLaboratory);
        assertFalse("One artwork is below the native two-card threshold",
                oneObjective.isFlipped());

        var noThrawn = scenario();
        var noThrawnObjective = noThrawn.GetDSCard("objective");
        var noThrawnLaboratory = noThrawn.GetDSCard("laboratory");
        var noThrawnCollection = noThrawn.GetDSCard("collection");
        var pulse = noThrawn.GetDSCard("pulse1");
        noThrawn.MoveCardsToDSHand(pulse);
        noThrawn.StartGame();
        noThrawn.StackCardsOn(noThrawnCollection,
                noThrawn.GetLSCard("art1"), noThrawn.GetLSCard("art2"));
        noThrawn.DSActivateForceCheat(10);
        noThrawn.SkipToPhase(Phase.DEPLOY);
        noThrawn.DSDeployCardAndPassResponses(pulse, noThrawnLaboratory);
        assertFalse("Two artwork without a Thrawn persona at a battleground must not flip",
                noThrawnObjective.isFlipped());
    }

    @Test
    public void backStaysStableWithOneArtworkThenFlipsWhenArtworkIsGone() {
        var scn = startFlipped();
        var objective = scn.GetDSCard("objective");
        var collection = scn.GetDSCard("collection");
        var laboratory = scn.GetDSCard("laboratory");
        var art1 = scn.GetLSCard("art1");
        var art2 = scn.GetLSCard("art2");
        var pulse1 = scn.GetDSCard("pulse1");
        var pulse2 = scn.GetDSCard("pulse2");

        scn.MoveOutOfPlay(art2);
        assertTrue(scn.IsStackedOn(collection, art1));
        scn.DSDeployCardAndPassResponses(pulse1, laboratory);
        assertTrue("Thrawn plus one remaining artwork is stable on the back",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(art1);
        scn.DSDeployCardAndPassResponses(pulse2, laboratory);
        assertFalse("Zero artwork outside battle must fire the native back flip",
                objective.isFlipped());
    }

    @Test
    public void missingThrawnFlipsBackIndependentlyOfArtworkCount() {
        var scn = startFlipped();
        var objective = scn.GetDSCard("objective");
        var laboratory = scn.GetDSCard("laboratory");
        var thrawn = scn.GetDSCard("thrawn");
        var pulse = scn.GetDSCard("pulse1");

        scn.MoveOutOfPlay(thrawn);
        scn.DSDeployCardAndPassResponses(pulse, laboratory);

        assertFalse("Missing Thrawn is an independent native flip-back route",
                objective.isFlipped());
    }

    private VirtualTableScenario startFlipped() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var collection = scn.GetDSCard("collection");
        var laboratory = scn.GetDSCard("laboratory");
        var thrawn = scn.GetDSCard("thrawn");
        scn.MoveCardsToDSHand(
                thrawn, scn.GetDSCard("pulse1"), scn.GetDSCard("pulse2"));
        scn.StartGame();
        scn.StackCardsOn(collection,
                scn.GetLSCard("art1"), scn.GetLSCard("art2"));
        scn.DSActivateForceCheat(20);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(thrawn, laboratory);
        assertTrue(objective.isFlipped());
        scn.LSPass();
        return scn;
    }
}
