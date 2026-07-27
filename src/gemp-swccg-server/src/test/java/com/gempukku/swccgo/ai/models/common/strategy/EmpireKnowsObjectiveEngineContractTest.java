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
 * Batch Fifteen (2026-07-27): native engine contract for The Empire Knows
 * We're Here / Prepare For Ground Assault (222_27, LIGHT). Card Java
 * unchanged. This batch also ACTIVATES the profile: it shipped with no
 * loaderEnabled key and was fully inert before tonight.
 *
 * Law (Card222_027.java L135-L140): flips when the opponent occupies a Hoth
 * location YOU OWN. Back (Card222_027_BACK.java L218-L230): flips back the
 * moment they no longer do — a pure oscillator.
 */
public class EmpireKnowsObjectiveEngineContractTest {

    private static final StartingSetup THE_EMPIRE_KNOWS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "222_27");
                put("system", "3_55");
                put("generators", "3_61");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Hoth system and Main Power Generators.
            // The engine's marker-ordering rule auto-deploys a 4th Marker
            // from Reserve first (MPG needs a 4th/5th/6th Marker on table).
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("4th Marker")) {
                    scn.LSChooseCard(scn.GetLSCard("northRidge"));
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("generators"));
                }
            }
        }
    };

    private VirtualTableScenario tekwrhScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("snowTrench", "3_63");
                    put("northRidge", "3_62");
                }},
                new HashMap<>() {{
                    put("icePlains", "3_148");
                }},
                24,
                24,
                THE_EMPIRE_KNOWS,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToHoth(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Hoth, null);
        assertFalse("Expected a legal placement at Hoth",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    @Test
    public void tekwrhFlipsOnlyWhenOpponentOccupiesAHothLocationYouOwn() {
        var scn = tekwrhScenario();
        var objective = scn.GetLSCard("objective");
        var generators = scn.GetLSCard("generators");
        var icePlains = scn.GetDSCard("icePlains");
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToHoth(scn, icePlains);
        // Opponent presence at their OWN dark-printed Hoth site must not
        // flip: the ownership qualifier is the whole law.
        scn.MoveCardsToLocation(icePlains, scn.GetDSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Opponent presence at their own Hoth location must not flip",
                objective.isFlipped());
        scn.DSPass();

        // Opponent presence at YOUR Main Power Generators flips it.
        scn.MoveCardsToLocation(generators, scn.GetDSFiller(2));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Opponent occupying your Hoth location must flip",
                objective.isFlipped());
    }

    @Test
    public void tekwrhOscillatesWithTheInvadersPresence() {
        var scn = tekwrhScenario();
        var objective = scn.GetLSCard("objective");
        var generators = scn.GetLSCard("generators");
        var pulseOne = scn.GetLSFiller(1);
        var pulseTwo = scn.GetLSFiller(2);
        var pulseThree = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();

        scn.LSActivateForceCheat(16);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(generators, scn.GetDSFiller(1));
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Invasion must flip", objective.isFlipped());
        scn.DSPass();

        // Clearing the invader flips it straight back.
        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertFalse("Clearing every invader must flip the back to front",
                objective.isFlipped());
        scn.DSPass();

        // A returning invader flips it again.
        scn.MoveCardsToLocation(generators, scn.GetDSFiller(2));
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        assertTrue("A returning invader must flip it again",
                objective.isFlipped());
    }

    @Test
    public void tekwrhProfileHydratesAndTracksTheOscillator() {
        var scn = tekwrhScenario();
        var generators = scn.GetLSCard("generators");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The newly activated profile must hydrate for 222_27",
                analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("TEKWRH front encodes one rule", 1, preFlip.size());
        assertFalse("No invader: the front rule is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(generators, scn.GetDSFiller(1));
        var invaded = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("An invader at your Hoth location satisfies the front rule",
                invaded.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one negation rule", 1, postFlip.size());
    }
}
