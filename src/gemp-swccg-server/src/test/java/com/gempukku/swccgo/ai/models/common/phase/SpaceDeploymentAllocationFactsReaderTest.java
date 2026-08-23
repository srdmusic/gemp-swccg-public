package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Engine and actual-card-source contracts for V298 space allocation. */
public class SpaceDeploymentAllocationFactsReaderTest {

    @Test
    public void permanentPilotsBuildBuddyAbilityToFourThenGroundWins() {
        VirtualTableScenario scenario = scenario();
        PhysicalCardImpl system = scenario.GetLSStartingLocation();
        PhysicalCardImpl xwingOne = scenario.GetLSCard("xwingOne");
        PhysicalCardImpl xwingTwo = scenario.GetLSCard("xwingTwo");
        PhysicalCardImpl xwingThree = scenario.GetLSCard("xwingThree");
        PhysicalCardImpl xwingFour = scenario.GetLSCard("xwingFour");
        PhysicalCardImpl falcon = scenario.GetLSCard("falcon");

        scenario.MoveCardsToLocation(
                system, xwingOne, xwingTwo, xwingThree);
        assertEquals(3.0f, scenario.game().getModifiersQuerying()
                .getTotalAbilityAtLocation(
                        scenario.gameState(), VirtualTableScenario.LS,
                        system), 0.0f);

        SpaceDeploymentAllocationPolicy.Evaluation completes =
                SpaceDeploymentAllocationFactsReader.evaluateDestination(
                        "fourth-permanent-pilot", scenario.game(),
                        VirtualTableScenario.LS, xwingFour,
                        xwingFour.getBlueprint(), system,
                        false, false, 20, 5)
                    .orElseThrow();
        assertEquals(SpaceDeploymentAllocationPolicy.Outcome.BUDDY_COMPLETE,
                completes.outcome());

        scenario.MoveCardsToLocation(system, xwingFour, falcon);
        assertEquals(4.0f, scenario.game().getModifiersQuerying()
                .getTotalAbilityAtLocation(
                        scenario.gameState(), VirtualTableScenario.LS,
                        system), 0.0f);

        SpaceDeploymentAllocationPolicy.Evaluation quietExtra =
                SpaceDeploymentAllocationFactsReader.evaluateDestination(
                        "quiet-falcon-crew", scenario.game(),
                        VirtualTableScenario.LS,
                        scenario.GetLSCard("luke"),
                        scenario.GetLSCard("luke").getBlueprint(), falcon,
                        false, false, 20, 5)
                    .orElseThrow();
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome
                        .GROUND_FIRST_AFTER_FOUR,
                quietExtra.outcome());
        assertTrue(SpaceDeploymentAllocationPolicy.isDeferred(quietExtra));
    }

    @Test
    public void actualFalconPilotSourcePrefersPowerAddingPilotsOverLuke() {
        VirtualTableScenario scenario = scenario();
        PhysicalCardImpl luke = scenario.GetLSCard("luke");
        PhysicalCardImpl solo = scenario.GetLSCard("solo");
        PhysicalCardImpl chewie = scenario.GetLSCard("chewie");
        PhysicalCardImpl falcon = scenario.GetLSCard("falcon");

        assertFalse(SpaceDeploymentAllocationFactsReader
                .readsAddsPowerWhenPiloting(scenario.game(), luke));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .readsAddsPowerWhenPiloting(scenario.game(), solo));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .readsAddsPowerWhenPiloting(scenario.game(), chewie));
        assertFalse(SpaceDeploymentAllocationFactsReader.isMatchingPilot(
                scenario.game(), luke, falcon));
        assertTrue(SpaceDeploymentAllocationFactsReader.isMatchingPilot(
                scenario.game(), solo, falcon));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .plannerPilotQualityTier(
                        scenario.game(), solo, falcon)
                > SpaceDeploymentAllocationFactsReader
                    .plannerPilotQualityTier(
                            scenario.game(), luke, falcon));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .plannerPilotQualityTier(
                        scenario.game(), chewie, falcon)
                > SpaceDeploymentAllocationFactsReader
                    .plannerPilotQualityTier(
                            scenario.game(), luke, falcon));
        assertSame(falcon, SpaceDeploymentAllocationFactsReader
                .findSimultaneousShip(
                        scenario.gameState(),
                        "The Falcon, Junkyard Garbage"));
    }

    @Test
    public void nonPilotPassengerCannotClaimOrphanRepilotException() {
        VirtualTableScenario scenario = scenario();
        PhysicalCardImpl system = scenario.GetLSStartingLocation();
        PhysicalCardImpl falcon = scenario.GetLSCard("falcon");
        scenario.MoveCardsToLocation(
                system,
                scenario.GetLSCard("xwingOne"),
                scenario.GetLSCard("xwingTwo"),
                scenario.GetLSCard("xwingThree"),
                scenario.GetLSCard("xwingFour"),
                falcon);

        assertEquals(4.0f, scenario.game().getModifiersQuerying()
                .getTotalAbilityAtLocation(
                        scenario.gameState(), VirtualTableScenario.LS,
                        system), 0.0f);

        SpaceDeploymentAllocationPolicy.Evaluation passenger =
                SpaceDeploymentAllocationFactsReader.evaluateParent(
                        "quiet-passenger", scenario.game(),
                        VirtualTableScenario.LS,
                        scenario.GetLSCard("leia"),
                        "Deploy Leia Organa aboard The Falcon, Junkyard Garbage",
                        String.valueOf(falcon.getCardId()),
                        false, false, 20, 5)
                    .orElseThrow();
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome
                        .GROUND_FIRST_AFTER_FOUR,
                passenger.outcome());

        SpaceDeploymentAllocationPolicy.Evaluation realPilot =
                SpaceDeploymentAllocationFactsReader.evaluateParent(
                        "real-repilot", scenario.game(),
                        VirtualTableScenario.LS,
                        scenario.GetLSCard("solo"),
                        "Deploy Solo aboard The Falcon, Junkyard Garbage",
                        String.valueOf(falcon.getCardId()),
                        false, false, 20, 5)
                    .orElseThrow();
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.REPILOT_EXCEPTION,
                realPilot.outcome());
    }

    @Test
    public void repilotPlannerDoesNotBindLukeAheadOfFalconCrew()
            throws Exception {
        VirtualTableScenario scenario = scenario();
        PhysicalCardImpl falcon = scenario.GetLSCard("falcon");
        scenario.LSActivateForceCheat(9);
        scenario.MoveCardsToLSHand(
                scenario.GetLSCard("luke"),
                scenario.GetLSCard("solo"),
                scenario.GetLSCard("chewie"));
        scenario.MoveCardsToLocation(
                scenario.GetLSStartingLocation(), falcon);

        for (String bot : new String[]{"rando", "chosenone"}) {
            Class<?> plannerClass = Class.forName(
                    "com.gempukku.swccgo.ai.models." + bot
                            + ".strategy.DeployPhasePlanner");
            Class<?> cardInfoClass = Class.forName(
                    "com.gempukku.swccgo.ai.models." + bot
                            + ".strategy.CardInfo");
            Constructor<?> cardInfoConstructor =
                    cardInfoClass.getConstructor(PhysicalCard.class);
            List<Object> pilots = List.of(
                    cardInfoConstructor.newInstance(
                            scenario.GetLSCard("luke")),
                    cardInfoConstructor.newInstance(
                            scenario.GetLSCard("solo")),
                    cardInfoConstructor.newInstance(
                            scenario.GetLSCard("chewie")));

            Object planner = plannerClass.getConstructor().newInstance();
            setField(plannerClass, planner,
                    "currentGame", scenario.game());
            setField(plannerClass, planner,
                    "currentPlayerId", VirtualTableScenario.LS);
            Method generate = plannerClass.getDeclaredMethod(
                    "generateRepilotPlan", List.class,
                    com.gempukku.swccgo.game.SwccgGame.class,
                    String.class, int.class);
            generate.setAccessible(true);
            Object plan = generate.invoke(
                    planner, pilots, scenario.game(),
                    VirtualTableScenario.LS, 20);
            List<?> instructions = (List<?>) plan.getClass()
                    .getMethod("getInstructions").invoke(plan);

            assertEquals(bot, 1, instructions.size());
            assertEquals(bot, "204_11", instructions.get(0).getClass()
                    .getMethod("getCardBlueprintId")
                    .invoke(instructions.get(0)));
            assertEquals(bot, "204_35", instructions.get(0).getClass()
                    .getMethod("getAboardShipBlueprintId")
                    .invoke(instructions.get(0)));
            assertEquals(bot, String.valueOf(falcon.getCardId()),
                    instructions.get(0).getClass()
                            .getMethod("getAboardShipCardId")
                            .invoke(instructions.get(0)));
        }
    }

    private static void setField(
            Class<?> owner, Object target,
            String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static VirtualTableScenario scenario() {
        VirtualTableScenario scenario = new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwingOne", "1_146");
                    put("xwingTwo", "1_146");
                    put("xwingThree", "1_146");
                    put("xwingFour", "1_146");
                    put("falcon", "204_35");
                    put("luke", "210_20");
                    put("leia", "1_17");
                    put("solo", "204_11");
                    put("chewie", "10_3");
                }},
                new HashMap<>(),
                10, 10,
                StartingSetup.DefaultLSSpaceSystem,
                StartingSetup.DefaultDSSpaceSystem,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
        scenario.StartGame();
        return scenario;
    }
}
