package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ZeroHourObjectiveEngineContractTest {
    private static final StartingSetup ZERO_HOUR = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("zeroHour", "219_48");
                put("lothal", "219_38");
                put("capital", "219_39");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            scn.LSChooseCard(scn.GetLSCard("capital"));
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("rebel1", "1_28");
                    put("rebel2", "1_28");
                    put("rebel3", "1_28");
                    put("unrelatedRebel", "1_28");
                    put("sabine", "207_9");
                    put("zeb", "208_13");
                    put("chopper", "208_2");
                    put("downloadSite", "219_42");
                }},
                new HashMap<>() {{
                    put("complex", "219_13");
                    put("mines", "219_14");
                    put("laboratory", "219_11");
                    put("storm1", "1_194");
                    put("storm2", "1_194");
                    put("storm3", "1_194");
                    put("storm4", "1_194");
                    put("storm5", "1_194");
                    put("storm6", "1_194");
                }},
                24,
                24,
                ZERO_HOUR,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void addRemainingLothalSites(VirtualTableScenario scn) {
        scn.MoveLocationToTable(scn.GetDSCard("complex"));
        scn.MoveLocationToTable(scn.GetDSCard("mines"));
        scn.MoveLocationToTable(scn.GetDSCard("laboratory"));
    }

    private void deployThreeRebelsToFlip(VirtualTableScenario scn) {
        var capital = scn.GetLSCard("capital");
        var mines = scn.GetDSCard("mines");
        var laboratory = scn.GetDSCard("laboratory");
        var rebel1 = scn.GetLSCard("rebel1");
        var rebel2 = scn.GetLSCard("rebel2");
        var rebel3 = scn.GetLSCard("rebel3");

        scn.LSDeployCardAndPassResponses(rebel1, capital);
        assertFalse(scn.GetLSCard("zeroHour").isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(rebel2, mines);
        assertFalse(scn.GetLSCard("zeroHour").isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(rebel3, laboratory);
        assertTrue(scn.GetLSCard("zeroHour").isFlipped());
    }

    private VirtualTableScenario startFlipped() {
        var scn = scenario();
        scn.MoveCardsToLSHand(
                scn.GetLSCard("rebel1"),
                scn.GetLSCard("rebel2"),
                scn.GetLSCard("rebel3"));
        scn.MoveCardsToDSHand(
                scn.GetDSCard("storm1"),
                scn.GetDSCard("storm2"));
        scn.StartGame();
        addRemainingLothalSites(scn);
        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);
        deployThreeRebelsToFlip(scn);

        // Restore Light Side's top-level action decision after the required flip.
        scn.DSPass();
        return scn;
    }

    @Test
    public void objectiveDownloadsARequiredLothalSiteWithoutForce() {
        var scn = scenario();
        var zeroHour = scn.GetLSCard("zeroHour");
        var downloadSite = scn.GetLSCard("downloadSite");

        scn.StartGame();
        scn.MoveCardsToBottomOfLSReserveDeck(downloadSite);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfLSForcePile());
        }

        assertEquals(0, scn.GetLSForcePileCount());
        assertTrue(scn.LSCardActionAvailable(
                zeroHour, "Deploy a Lothal site from Reserve Deck"));
        scn.LSUseCardAction(
                zeroHour, "Deploy a Lothal site from Reserve Deck");
        assertTrue(scn.LSHasCardChoiceAvailable(downloadSite));
        scn.LSChooseCard(downloadSite);
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("On which side"));
        scn.LSChoose("Left");
        scn.PassAllResponses();

        assertEquals("A site has zero deploy cost",
                0, scn.GetLSForcePileCount());
        assertSame(Zone.LOCATIONS, downloadSite.getZone());
    }

    @Test
    public void frontRouteARequiresThreeLothalLocationsControlledWithRebels() {
        var scn = scenario();
        scn.MoveCardsToLSHand(
                scn.GetLSCard("rebel1"),
                scn.GetLSCard("rebel2"),
                scn.GetLSCard("rebel3"));
        scn.StartGame();
        addRemainingLothalSites(scn);
        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);

        deployThreeRebelsToFlip(scn);
    }

    @Test
    public void frontRouteAIsBlockedWhenOpponentControlsAnyLothalLocation() {
        var scn = scenario();
        scn.MoveCardsToLSHand(
                scn.GetLSCard("rebel1"),
                scn.GetLSCard("rebel2"),
                scn.GetLSCard("rebel3"));
        scn.StartGame();
        addRemainingLothalSites(scn);
        scn.MoveCardsToLocation(scn.GetDSCard("complex"), scn.GetDSCard("storm1"));
        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(scn.GetLSCard("rebel1"), scn.GetLSCard("capital"));
        scn.DSPass();
        scn.LSDeployCardAndPassResponses(scn.GetLSCard("rebel2"), scn.GetDSCard("mines"));
        scn.DSPass();
        scn.LSDeployCardAndPassResponses(scn.GetLSCard("rebel3"), scn.GetDSCard("laboratory"));

        assertFalse(scn.GetLSCard("zeroHour").isFlipped());
    }

    @Test
    public void frontRouteBRequiresThreePhoenixOccupiedLothalLocations() {
        var scn = scenario();
        var zeroHour = scn.GetLSCard("zeroHour");
        var sabine = scn.GetLSCard("sabine");
        var zeb = scn.GetLSCard("zeb");
        var chopper = scn.GetLSCard("chopper");
        var unrelatedRebel = scn.GetLSCard("unrelatedRebel");
        var capital = scn.GetLSCard("capital");
        var complex = scn.GetDSCard("complex");
        var mines = scn.GetDSCard("mines");

        scn.MoveCardsToLSHand(unrelatedRebel, chopper);
        scn.StartGame();
        addRemainingLothalSites(scn);

        assertFalse(chopper.getBlueprint().hasKeyword(Keyword.PHOENIX_SQUADRON));
        assertTrue(scn.HasKeyword(chopper, Keyword.PHOENIX_SQUADRON));

        // Exact ability ties mean neither player controls these locations.
        scn.MoveCardsToLocation(capital, sabine, scn.GetDSCard("storm1"), scn.GetDSCard("storm2"));
        scn.MoveCardsToLocation(complex, zeb, scn.GetDSCard("storm3"), scn.GetDSCard("storm4"));
        scn.MoveCardsToLocation(mines, scn.GetDSCard("storm5"));

        scn.LSActivateForceCheat(6);
        scn.SkipToLSTurn(Phase.DEPLOY);

        // An unrelated Rebel establishes occupation, but is not a third Phoenix member.
        scn.LSDeployCardAndPassResponses(unrelatedRebel, mines);
        assertFalse(zeroHour.isFlipped());
        scn.DSPass();

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), LS, Side.LIGHT);
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR,
                analyzer.classifyPreFlipProgressCandidate(
                        scn.game(), LS, chopper));
        assertTrue(analyzer.advancesPreFlipRequirementAt(
                scn.game(), LS, chopper, mines));
        assertTrue(analyzer.wouldCompletePreFlipRequirementAt(
                scn.game(), LS, chopper, mines));
        assertFalse("Chopper cannot establish occupation at an empty site",
                analyzer.advancesPreFlipRequirementAt(
                        scn.game(), LS, chopper,
                        scn.GetDSCard("laboratory")));

        // Zero Hour grants Chopper Phoenix Squadron, completing the alternate route.
        scn.LSDeployCardAndPassResponses(chopper, mines);
        assertTrue(zeroHour.isFlipped());
    }

    @Test
    public void frontRouteBIsBlockedWhenOpponentControlsAnyLothalLocation() {
        var scn = scenario();
        var chopper = scn.GetLSCard("chopper");
        var unrelatedRebel = scn.GetLSCard("unrelatedRebel");

        scn.MoveCardsToLSHand(chopper);
        scn.StartGame();
        addRemainingLothalSites(scn);

        scn.MoveCardsToLocation(
                scn.GetLSCard("capital"),
                scn.GetLSCard("sabine"),
                scn.GetDSCard("storm1"),
                scn.GetDSCard("storm2"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("complex"),
                scn.GetLSCard("zeb"),
                scn.GetDSCard("storm3"),
                scn.GetDSCard("storm4"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("mines"),
                unrelatedRebel,
                scn.GetDSCard("storm5"));
        scn.MoveCardsToLocation(scn.GetDSCard("laboratory"), scn.GetDSCard("storm6"));

        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(chopper, scn.GetDSCard("mines"));

        assertFalse(scn.GetLSCard("zeroHour").isFlipped());
    }

    @Test
    public void backStaysFlippedAtControlTieAndFlipsBackOnlyWhenOpponentIsAhead() {
        var scn = startFlipped();
        var zeroHour = scn.GetLSCard("zeroHour");

        // Leave Light Side controlling only Capital City.
        scn.MoveOutOfPlay(scn.GetLSCard("rebel2"));
        scn.MoveOutOfPlay(scn.GetLSCard("rebel3"));
        scn.DSActivateForceCheat(3);
        scn.SkipToDSTurn(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(scn.GetDSCard("storm1"), scn.GetDSCard("mines"));
        assertTrue(zeroHour.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(scn.GetDSCard("storm2"), scn.GetDSCard("laboratory"));
        assertFalse(zeroHour.isFlipped());
    }
}
