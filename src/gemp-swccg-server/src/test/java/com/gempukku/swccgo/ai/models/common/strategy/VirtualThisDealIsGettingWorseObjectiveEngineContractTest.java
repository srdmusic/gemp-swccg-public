package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VirtualThisDealIsGettingWorseObjectiveEngineContractTest {
    private static final String LANDO_MOVE =
            "Have your Lando make a regular move";
    private static final String[] BESPIN_LOCATION_ALIASES = {
            "setupSite",
            "bespin2",
            "bespin3",
            "bespin4",
            "bespin5",
            "bespin6"
    };
    private static final String[] DARK_CONTROLLER_ALIASES = {
            "dark1",
            "dark2",
            "dark3"
    };
    private static final String[] LIGHT_CONTROLLER_ALIASES = {
            "light1",
            "light2",
            "light3"
    };

    private static final StartingSetup VIRTUAL_THIS_DEAL =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "226_12");
                        put("setupSite", "7_270");
                        put("imSorry", "226_6");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    // Each required setup filter has one matching candidate.
                }
            };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("light1", "1_28");
                    put("light2", "1_28");
                    put("light3", "1_28");
                }},
                new HashMap<>() {{
                    put("bespin2", "5_166");
                    put("bespin3", "5_167");
                    put("bespin4", "5_168");
                    put("bespin5", "5_170");
                    put("bespin6", "5_171");
                    put("triggerSite", "12_176");
                    put("lando", "5_99");
                    put("dark1", "1_194");
                    put("dark2", "1_194");
                    put("dark3", "1_194");
                    put("triggerStorm", "1_194");
                }},
                40,
                40,
                StartingSetup.DoNothingSetup,
                VIRTUAL_THIS_DEAL,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private VirtualTableScenario startScenario() {
        VirtualTableScenario scn = scenario();
        scn.MoveCardsToLSHand(
                scn.GetLSCard("light1"),
                scn.GetLSCard("light2"),
                scn.GetLSCard("light3"));
        scn.MoveCardsToDSHand(
                scn.GetDSCard("bespin2"),
                scn.GetDSCard("bespin3"),
                scn.GetDSCard("bespin4"),
                scn.GetDSCard("bespin5"),
                scn.GetDSCard("bespin6"),
                scn.GetDSCard("triggerSite"),
                scn.GetDSCard("lando"),
                scn.GetDSCard("dark1"),
                scn.GetDSCard("dark2"),
                scn.GetDSCard("dark3"),
                scn.GetDSCard("triggerStorm"));
        scn.StartGame();
        for (int index = 1;
             index < BESPIN_LOCATION_ALIASES.length;
             index++) {
            scn.MoveLocationToTable(
                    scn.GetDSCard(BESPIN_LOCATION_ALIASES[index]));
        }
        scn.MoveLocationToTable(scn.GetDSCard("triggerSite"));
        return scn;
    }

    private void enterDarkDeploy(VirtualTableScenario scn) {
        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.DEPLOY);
    }

    private void deployDark(
            VirtualTableScenario scn,
            String cardAlias,
            String locationAlias) {
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard(cardAlias),
                scn.GetDSCard(locationAlias));
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
    }

    private int controlledBespinLocations(
            VirtualTableScenario scn,
            String playerId) {
        return Filters.countTopLocationsOnTable(
                scn.game(),
                Filters.and(
                        Filters.Bespin_location,
                        Filters.controls(
                                playerId,
                                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE)));
    }

    private void clearDarkForce(VirtualTableScenario scn) {
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
        }
    }

    private void enterDarkControlWithForce(
            VirtualTableScenario scn,
            int force) {
        scn.SkipToDSTurn();
        clearDarkForce(scn);
        if (force > 0) {
            scn.DSActivateForceCheat(force);
        }
        scn.PassActivateActions();
        if (scn.DSDecisionAvailable(
                "You have not activated Force. Do you want to Pass?")) {
            scn.DSChooseYes();
            scn.PassActivateActions();
        }
        assertTrue(
                "Expected Dark Side control actions, but saw: "
                        + scn.GetCurrentDecision().getText(),
                scn.AwaitingDSControlPhaseActions());
        assertEquals(force, scn.GetDSForcePileCount());
    }

    private void executeLandoMove(
            VirtualTableScenario scn,
            String destinationAlias) {
        var objective = scn.GetDSCard("objective");
        var destination = scn.GetDSCard(destinationAlias);

        assertTrue(
                "One Force must make the source-granted control-phase move legal",
                scn.DSCardActionAvailable(objective, LANDO_MOVE));

        scn.DSUseCardAction(objective, LANDO_MOVE);
        if (scn.DSDecisionAvailable("Choose where to move")) {
            assertTrue(scn.DSHasCardChoiceAvailable(destination));
            scn.DSChooseCard(destination);
        } else {
            assertTrue(
                    "The sole legal destination should auto-select before the normal movement cost, but saw: "
                            + scn.GetCurrentDecision().getText(),
                    scn.GetCurrentDecision().getText()
                            .contains("Use 1 Force - Optional responses"));
        }
        scn.PassAllResponses();

        assertAtLocation(destination, scn.GetDSCard("lando"));
        assertEquals(
                "The source grants the timing exception, but ordinary landspeed movement still costs one Force",
                0, scn.GetDSForcePileCount());
    }

    private VirtualTableScenario startFlipped() {
        VirtualTableScenario scn = startScenario();
        scn.MoveCardsToLocation(
                scn.GetDSCard("setupSite"),
                scn.GetDSCard("dark1"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("bespin2"),
                scn.GetDSCard("dark2"));
        enterDarkDeploy(scn);
        deployDark(scn, "dark3", "bespin3");

        assertEquals(3, controlledBespinLocations(scn, DS));
        assertEquals(0, controlledBespinLocations(scn, LS));
        assertTrue(
                "A real third-location deployment must flip the virtual objective without Dark Deal",
                scn.GetDSCard("objective").isFlipped());
        return scn;
    }

    private void arrangeControlTie(
            VirtualTableScenario scn,
            int count) {
        scn.MoveCardsToDSHand(
                scn.GetDSCard("dark1"),
                scn.GetDSCard("dark2"),
                scn.GetDSCard("dark3"));
        scn.MoveCardsToLSHand(
                scn.GetLSCard("light1"),
                scn.GetLSCard("light2"),
                scn.GetLSCard("light3"));
        for (int index = 0; index < count; index++) {
            scn.MoveCardsToLocation(
                    scn.GetDSCard(BESPIN_LOCATION_ALIASES[index]),
                    scn.GetDSCard(DARK_CONTROLLER_ALIASES[index]));
            scn.MoveCardsToLocation(
                    scn.GetDSCard(BESPIN_LOCATION_ALIASES[index + 3]),
                    scn.GetLSCard(LIGHT_CONTROLLER_ALIASES[index]));
        }
        assertEquals(count, controlledBespinLocations(scn, DS));
        assertEquals(count, controlledBespinLocations(scn, LS));
    }

    @Test
    public void setupDeploysBothRequiredCardsFromTheirExactFilters() {
        var scn = startScenario();
        var objective = scn.GetDSCard("objective");
        var setupSite = scn.GetDSCard("setupSite");
        var imSorry = scn.GetDSCard("imSorry");

        assertInZone(Zone.SIDE_OF_TABLE, objective);
        assertInZone(Zone.LOCATIONS, setupSite);
        assertInZone(Zone.SIDE_OF_TABLE, imSorry);
        assertTrue(
                "The required location must really satisfy the Cloud City battleground-site filter",
                Filters.Cloud_City_battleground_site.accepts(
                        scn.game(), setupSite));
        assertTrue(
                "The other required setup card must really be Cloud City-icon I'm Sorry",
                Filters.and(Icon.CLOUD_CITY, Filters.Im_Sorry)
                        .accepts(scn.game(), imSorry));
        assertFalse(objective.isFlipped());
    }

    @Test
    public void bothSidesGrantTheControlPhaseLandoMoveAtNormalForceCost() {
        var frontAtZero = startScenario();
        frontAtZero.MoveCardsToLocation(
                frontAtZero.GetDSCard("setupSite"),
                frontAtZero.GetDSCard("lando"));
        enterDarkControlWithForce(frontAtZero, 0);
        assertFalse(
                "The front passes forFree=false, so zero Force must not expose the regular move",
                frontAtZero.DSCardActionAvailable(
                        frontAtZero.GetDSCard("objective"), LANDO_MOVE));

        var frontWithOne = startScenario();
        frontWithOne.MoveCardsToLocation(
                frontWithOne.GetDSCard("setupSite"),
                frontWithOne.GetDSCard("lando"));
        enterDarkControlWithForce(frontWithOne, 1);
        executeLandoMove(frontWithOne, "bespin2");
        assertFalse(frontWithOne.GetDSCard("objective").isFlipped());

        var backAtZero = startFlipped();
        backAtZero.MoveCardsToLocation(
                backAtZero.GetDSCard("setupSite"),
                backAtZero.GetDSCard("lando"));
        enterDarkControlWithForce(backAtZero, 0);
        assertFalse(
                "The back passes forFree=false, so zero Force must not expose the regular move",
                backAtZero.DSCardActionAvailable(
                        backAtZero.GetDSCard("objective"), LANDO_MOVE));

        var backWithOne = startFlipped();
        backWithOne.MoveCardsToLocation(
                backWithOne.GetDSCard("setupSite"),
                backWithOne.GetDSCard("lando"));
        enterDarkControlWithForce(backWithOne, 1);
        executeLandoMove(backWithOne, "bespin2");
        assertTrue(backWithOne.GetDSCard("objective").isFlipped());
    }

    @Test
    public void frontWaitsAtTwoThenFlipsAtThreeAgainstZeroOneOrTwo() {
        for (int lightCount = 0; lightCount <= 2; lightCount++) {
            var scn = startScenario();
            var objective = scn.GetDSCard("objective");
            scn.MoveCardsToLocation(
                    scn.GetDSCard("setupSite"),
                    scn.GetDSCard("dark1"));
            scn.MoveCardsToLocation(
                    scn.GetDSCard("bespin2"),
                    scn.GetDSCard("dark2"));
            for (int index = 0; index < lightCount; index++) {
                scn.MoveCardsToLocation(
                        scn.GetDSCard(
                                BESPIN_LOCATION_ALIASES[index + 3]),
                        scn.GetLSCard(
                                LIGHT_CONTROLLER_ALIASES[index]));
            }
            enterDarkDeploy(scn);

            deployDark(scn, "triggerStorm", "triggerSite");
            assertEquals(2, controlledBespinLocations(scn, DS));
            assertEquals(
                    lightCount, controlledBespinLocations(scn, LS));
            assertFalse(
                    "An actual table change at only two Dark Side-controlled Bespin locations must not flip",
                    objective.isFlipped());

            deployDark(scn, "dark3", "bespin3");
            assertEquals(3, controlledBespinLocations(scn, DS));
            assertEquals(
                    lightCount, controlledBespinLocations(scn, LS));
            assertNull(
                    "Virtual TDIGWATT must not require Dark Deal",
                    Filters.findFirstActive(
                            scn.game(), objective, Filters.Dark_Deal));
            assertTrue(
                    "The real third-control deployment must flip against zero, one, or two opposing controls",
                    objective.isFlipped());
        }
    }

    @Test
    public void frontDoesNotFlipWhenBothPlayersControlThree() {
        var scn = startScenario();
        var objective = scn.GetDSCard("objective");
        for (int index = 0; index < 3; index++) {
            scn.MoveCardsToLocation(
                    scn.GetDSCard(BESPIN_LOCATION_ALIASES[index]),
                    scn.GetDSCard(DARK_CONTROLLER_ALIASES[index]));
        }
        scn.MoveCardsToLocation(
                scn.GetDSCard("bespin4"),
                scn.GetLSCard("light1"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("bespin5"),
                scn.GetLSCard("light2"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("bespin6"),
                scn.GetLSCard("light3"));
        assertEquals(3, controlledBespinLocations(scn, DS));
        assertEquals(3, controlledBespinLocations(scn, LS));
        assertFalse(objective.isFlipped());
        enterDarkDeploy(scn);
        deployDark(scn, "triggerStorm", "triggerSite");

        assertEquals(3, controlledBespinLocations(scn, DS));
        assertEquals(3, controlledBespinLocations(scn, LS));
        assertFalse(
                "An actual table change must not fire the front trigger at a 3-3 tie",
                objective.isFlipped());
    }

    @Test
    public void backSurvivesMultipleTiesAndFlipsOnlyWhenLightIsAhead() {
        for (int tieCount = 0; tieCount <= 3; tieCount++) {
            var tied = startFlipped();
            arrangeControlTie(tied, tieCount);
            tied.DSActivateForceCheat(8);
            tied.SkipToDSTurn(Phase.DEPLOY);
            deployDark(tied, "triggerStorm", "triggerSite");

            assertTrue(
                    "A real table change must leave the back side up at every equal-control count",
                    tied.GetDSCard("objective").isFlipped());
        }

        var ahead = startFlipped();
        var objective = ahead.GetDSCard("objective");
        arrangeControlTie(ahead, 2);
        ahead.DSActivateForceCheat(8);
        ahead.SkipToDSTurn(Phase.DEPLOY);
        deployDark(ahead, "triggerStorm", "triggerSite");
        assertTrue(
                "The 2-2 table-change tie must leave the back side up",
                objective.isFlipped());

        ahead.LSActivateForceCheat(8);
        ahead.SkipToLSTurn(Phase.DEPLOY);
        ahead.LSDeployCardAndPassResponses(
                ahead.GetLSCard("light3"),
                ahead.GetDSCard("bespin6"));

        assertEquals(2, controlledBespinLocations(ahead, DS));
        assertEquals(3, controlledBespinLocations(ahead, LS));
        assertFalse(
                "The actual deployment that makes Light Side's count strictly greater must flip back",
                objective.isFlipped());
    }
}
