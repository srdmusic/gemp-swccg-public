package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.logic.evaluators.BaseEvaluator;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Engine and actual-card-source contracts for V298/V299 space allocation. */
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
        PhysicalCard chewieAttachment = chewie.getAttachedTo();
        com.gempukku.swccgo.common.Zone chewieZone = chewie.getZone();
        boolean chewieWasPilot = chewie.isPilotOf();

        assertFalse(SpaceDeploymentAllocationFactsReader
                .readsAddsPowerWhenPiloting(scenario.game(), luke));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .readsAddsPowerWhenPiloting(scenario.game(), solo));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .readsAddsPowerWhenPiloting(scenario.game(), chewie));
        assertEquals(0.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), luke, falcon), 0.0f);
        assertEquals(3.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), solo, falcon), 0.0f);
        assertEquals(3.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), chewie, falcon), 0.0f);
        assertEquals(2.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(
                        scenario.game(), chewie,
                        scenario.GetDSCard("devastator")), 0.0f);
        assertEquals(3.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(
                        scenario.game(), scenario.GetLSCard("tk422"),
                        falcon), 0.0f);
        assertSame("Prospective reads must not attach the pilot",
                chewieAttachment, chewie.getAttachedTo());
        assertEquals(chewieZone, chewie.getZone());
        assertEquals(chewieWasPilot, chewie.isPilotOf());
        assertNull("Attachment-dependent evaluators must fail open",
                SpaceDeploymentAllocationFactsReader.powerAddedIfPiloting(
                        scenario.game(), scenario.GetLSCard("chewieDynamic"),
                        falcon));
        assertFalse(SpaceDeploymentAllocationFactsReader.isMatchingPilot(
                scenario.game(), luke, falcon));
        assertTrue(SpaceDeploymentAllocationFactsReader.isMatchingPilot(
                scenario.game(), solo, falcon));
        DeployPilotShipPolicy.ExactPilotAssignmentFacts lukeFalcon =
                SpaceDeploymentAllocationFactsReader
                        .readExactPilotAssignmentFacts(
                                "luke-falcon", scenario.game(), luke,
                                falcon, false)
                        .orElseThrow();
        assertTrue("An unpiloted Falcon preserves the initial-pilot fallback",
                lukeFalcon.destinationNeedsPilot());
        assertFalse(DeployPilotShipPolicy
                .evaluateExactPilotAssignment(lukeFalcon)
                .operations().stream().anyMatch(operation ->
                        operation.kind()
                                == com.gempukku.swccgo.ai.models.common.policy
                                        .PolicyOperationKind.DEFER));
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

    /** Replay-shaped regression for supplemental DB 72314 at Tatooine. */
    @Test
    public void db72314DevastatorCrewUsesExactPowerAndKeepsScoutsGrounded() {
        VirtualTableScenario scenario = scenario();
        PhysicalCardImpl devastator = scenario.GetDSCard("devastator");
        PhysicalCardImpl executor = scenario.GetDSCard("executor");
        PhysicalCardImpl speederBike = scenario.GetDSCard("speederBike");
        PhysicalCardImpl thrawn = scenario.GetDSCard("thrawn");
        PhysicalCardImpl neimoidian = scenario.GetDSCard("neimoidian");
        PhysicalCardImpl avarik = scenario.GetDSCard("avarik");
        PhysicalCardImpl dooku = scenario.GetDSCard("dooku");
        PhysicalCardImpl piett = scenario.GetDSCard("piett");
        PhysicalCardImpl ozzel = scenario.GetDSCard("ozzel");
        PhysicalCardImpl ket = scenario.GetDSCard("ket");
        PhysicalCardImpl tk422 = scenario.GetLSCard("tk422");

        assertEquals(3.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), thrawn, devastator),
                0.0f);
        assertEquals(2.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(
                        scenario.game(), neimoidian, devastator), 0.0f);
        assertEquals(0.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), dooku, devastator),
                0.0f);
        assertEquals(0.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), avarik, devastator),
                0.0f);
        assertEquals(3.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), avarik, speederBike),
                0.0f);
        assertEquals(0.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), piett, devastator),
                0.0f);
        assertEquals(3.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), piett, executor),
                0.0f);
        assertTrue("Ket's printed pilot bonus is always-on card source",
                SpaceDeploymentAllocationFactsReader
                        .readsAddsPowerWhenPiloting(scenario.game(), ket));
        assertEquals(2.0f, SpaceDeploymentAllocationFactsReader
                .powerAddedIfPiloting(scenario.game(), ket, devastator),
                0.0f);

        assertTrue(SpaceDeploymentAllocationFactsReader
                .isStarDestroyer(scenario.game(), devastator));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .isStormtrooperFamily(scenario.game(), avarik));
        assertTrue(SpaceDeploymentAllocationFactsReader
                .isStormtrooperFamily(scenario.game(), tk422));
        assertFalse(SpaceDeploymentAllocationFactsReader
                .isStormtrooperFamily(scenario.game(), thrawn));
        assertEquals(300, SpaceDeploymentAllocationFactsReader
                .plannerPilotQualityTier(
                        scenario.game(), thrawn, devastator));
        assertEquals(200, SpaceDeploymentAllocationFactsReader
                .plannerPilotQualityTier(
                        scenario.game(), neimoidian, devastator));
        assertEquals(200, SpaceDeploymentAllocationFactsReader
                .plannerPilotQualityTier(
                        scenario.game(), ket, devastator));
        assertEquals(Integer.MIN_VALUE,
                SpaceDeploymentAllocationFactsReader
                        .plannerPilotQualityTier(
                                scenario.game(), avarik, devastator));
        assertEquals(0,
                SpaceDeploymentAllocationFactsReader
                        .plannerPilotQualityTier(
                                scenario.game(), dooku, devastator));
        DeployPilotShipPolicy.ExactPilotAssignmentFacts dookuBeforeFour =
                SpaceDeploymentAllocationFactsReader
                        .readExactPilotAssignmentFacts(
                                "dooku-before-four", scenario.game(), dooku,
                                devastator, false)
                        .orElseThrow();
        assertTrue("Permanent-pilot ability 2 still needs a buddy",
                dookuBeforeFour.abilityFourBuddyProgress());
        assertFalse(DeployPilotShipPolicy
                .evaluateExactPilotAssignment(dookuBeforeFour)
                .operations().stream().anyMatch(operation ->
                        operation.kind()
                                == com.gempukku.swccgo.ai.models.common.policy
                                        .PolicyOperationKind.DEFER));

        DeployPilotShipPolicy.ExactPilotAssignmentFacts avarikSpace =
                SpaceDeploymentAllocationFactsReader
                        .readExactPilotAssignmentFacts(
                                "avarik-space", scenario.game(), avarik,
                                devastator, false)
                        .orElseThrow();
        assertFalse("Devastator's permanent pilot closes the orphan fallback",
                avarikSpace.destinationNeedsPilot());
        assertTrue(DeployPilotShipPolicy
                .evaluateExactPilotAssignment(avarikSpace)
                .operations().stream().anyMatch(operation ->
                        operation.kind()
                                == com.gempukku.swccgo.ai.models.common.policy
                                        .PolicyOperationKind.DEFER));

        DeployPilotShipPolicy.ExactPilotAssignmentFacts avarikGround =
                SpaceDeploymentAllocationFactsReader
                        .readExactPilotAssignmentFacts(
                                "avarik-ground", scenario.game(), avarik,
                                speederBike, false)
                        .orElseThrow();
        assertFalse(DeployPilotShipPolicy
                .evaluateExactPilotAssignment(avarikGround)
                .operations().stream().anyMatch(operation ->
                        operation.kind()
                                == com.gempukku.swccgo.ai.models.common.policy
                                        .PolicyOperationKind.DEFER));

        PhysicalCardImpl system = scenario.GetDSStartingLocation();
        scenario.MoveCardsToLocation(system, devastator);
        scenario.BoardAsPilot(devastator, ozzel);
        assertEquals(4.0f, scenario.game().getModifiersQuerying()
                .getTotalAbilityAtLocation(
                        scenario.gameState(), VirtualTableScenario.DS,
                        system), 0.0f);
        DeployPilotShipPolicy.ExactPilotAssignmentFacts dookuAfterFour =
                SpaceDeploymentAllocationFactsReader
                        .readExactPilotAssignmentFacts(
                                "dooku-after-four", scenario.game(), dooku,
                                devastator, false)
                        .orElseThrow();
        assertFalse(dookuAfterFour.abilityFourBuddyProgress());
        assertTrue(DeployPilotShipPolicy
                .evaluateExactPilotAssignment(dookuAfterFour)
                .operations().stream().anyMatch(operation ->
                        operation.kind()
                                == com.gempukku.swccgo.ai.models.common.policy
                                        .PolicyOperationKind.DEFER));
        assertEquals(Integer.MIN_VALUE,
                SpaceDeploymentAllocationFactsReader
                        .plannerPilotQualityTier(
                                scenario.game(), dooku, devastator));
    }

    @Test
    public void rejectingTargetFilterProvesZeroBeforeUnsupportedEvaluator() {
        VirtualTableScenario scenario = scenario();
        AddsPowerToPilotedBySelfModifier modifier =
                new AddsPowerToPilotedBySelfModifier(
                        scenario.GetDSCard("avarik"),
                        new BaseEvaluator() {
                            @Override
                            public float evaluateExpression(
                                    GameState gameState,
                                    ModifiersQuerying modifiersQuerying,
                                    PhysicalCard affected) {
                                return 99.0f;
                            }
                        },
                        Filters.speeder_bike);

        assertEquals(0.0f,
                modifier.getProspectiveIntrinsicPowerModifier(
                        scenario.gameState(),
                        scenario.game().getModifiersQuerying(),
                        scenario.GetDSCard("devastator")),
                0.0f);
        assertNull(modifier.getProspectiveIntrinsicPowerModifier(
                scenario.gameState(),
                scenario.game().getModifiersQuerying(),
                scenario.GetDSCard("speederBike")));

        AddsPowerToPilotedBySelfModifier nonFinite =
                new AddsPowerToPilotedBySelfModifier(
                        scenario.GetDSCard("thrawn"),
                        new BaseEvaluator() {
                            @Override
                            public float evaluateExpression(
                                    GameState gameState,
                                    ModifiersQuerying modifiersQuerying,
                                    PhysicalCard affected) {
                                return Float.POSITIVE_INFINITY;
                            }

                            @Override
                            public boolean supportsProspectiveCardEvaluation() {
                                return true;
                            }
                        });
        assertNull("Non-finite source values remain unknown",
                nonFinite.getProspectiveIntrinsicPowerModifier(
                        scenario.gameState(),
                        scenario.game().getModifiersQuerying(),
                        scenario.GetDSCard("devastator")));
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
                    put("chewieDynamic", "213_36");
                    put("tk422", "215_20");
                }},
                new HashMap<>() {{
                    put("devastator", "216_8");
                    put("executor", "4_167");
                    put("speederBike", "8_169");
                    put("thrawn", "10_40");
                    put("neimoidian", "12_111");
                    put("avarik", "221_14");
                    put("dooku", "200_76");
                    put("piett", "215_22");
                    put("ozzel", "3_82");
                    put("ket", "601_17");
                }},
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
