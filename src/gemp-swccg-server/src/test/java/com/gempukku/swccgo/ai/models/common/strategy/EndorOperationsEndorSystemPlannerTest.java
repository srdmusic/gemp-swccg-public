package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.ContestStatus;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndorOperationsEndorSystemPlannerTest {
    private static final String OBJECTIVE_BP = "8_167";
    private static final String ENDOR_BP = "8_157";
    private static final String ENDOR_BUNKER_BP = "8_160";
    private static final String CANTINA_BP = "1_290";
    private static final String SLAVE_I_BP = "201_40";
    private static final String ADMIRAL_OZZEL_BP = "3_82";
    private static final String CAPTAIN_LENNOX_BP = "3_84";
    private static final String DS_61_3_BP = "1_174";
    private static final String COUNT_DOOKU_BP = "200_76";
    private static final String TROOPER_DAVIN_FELTH_BP = "2_106";

    private static final int ENDOR_ID = 101;
    private static final int BUNKER_ID = 102;
    private static final int CANTINA_ID = 103;
    private static final int SLAVE_I_ID = 201;
    private static final int ADMIRAL_OZZEL_ID = 202;
    private static final int CAPTAIN_LENNOX_ID = 204;
    private static final int DS_61_3_ID = 206;
    private static final int COUNT_DOOKU_ID = 205;
    private static final int TROOPER_DAVIN_FELTH_ID = 207;

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void exactSlaveIPackageUsesAndReservesTheFundedPhysicalPilot()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);
            assertTrue(bot.name(), harness.endor.isSpace());
            assertTrue(bot.name(), invokeEndorSystemOccupationPending(harness));
            Object slaveI =
                    harness.cardInfo(SLAVE_I_BP, SLAVE_I_ID);
            assertEquals(bot.name(), SLAVE_I_BP,
                    field(slaveI, "blueprintId"));
            assertEquals(bot.name(), 3, field(slaveI, "cost"));
            Object packagePlan = invokeEopPlan(
                    harness,
                    List.of(slaveI),
                    List.of(
                            harness.cardInfo(
                                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID),
                            harness.cardInfo(
                                    CAPTAIN_LENNOX_BP, CAPTAIN_LENNOX_ID)),
                    5);

            List<?> instructions = instructions(packagePlan);
            assertEquals(bot.name(), 2, instructions.size());
            Object shipInstruction = instructions.get(0);
            Object pilotInstruction = instructions.get(1);
            assertEquals(bot.name(), "Slave I, Symbol Of Fear",
                    invokeString(shipInstruction, "getCardName"));
            assertEquals(bot.name(), ADMIRAL_OZZEL_ID,
                    invokeInt(pilotInstruction, "getCardPermanentCardId"));
            assertEquals(bot.name(), "Admiral Ozzel",
                    invokeString(pilotInstruction, "getCardName"));
            assertEquals(bot.name(), "Slave I, Symbol Of Fear",
                    invokeString(pilotInstruction, "getAboardShipName"));
            assertEquals(bot.name(), SLAVE_I_BP,
                    invokeString(pilotInstruction, "getAboardShipBlueprintId"));
            assertEquals(bot.name(), String.valueOf(SLAVE_I_ID),
                    invokeString(pilotInstruction, "getAboardShipCardId"));
            assertEquals(bot.name(), 1,
                    invokeInt(shipInstruction, "getPriority"));
            assertEquals(bot.name(), 2,
                    invokeInt(pilotInstruction, "getPriority"));

            List<?> fundedGroundPlans = invokeGroundPlans(
                    harness,
                    List.of(
                            harness.cardInfo(
                                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID),
                            harness.cardInfo(
                                    CAPTAIN_LENNOX_BP, CAPTAIN_LENNOX_ID)),
                    List.of(harness.cardInfo(SLAVE_I_BP, SLAVE_I_ID)),
                    harness.cantina,
                    5);
            assertFalse(bot.name()
                            + " must not spend the exact funded Ozzel copy on ground",
                    hasGroundEstablishPlan(fundedGroundPlans));

            List<?> noShipGroundPlans = invokeGroundPlans(
                    harness,
                    List.of(
                            harness.cardInfo(
                                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID),
                            harness.cardInfo(
                                    CAPTAIN_LENNOX_BP, CAPTAIN_LENNOX_ID)),
                    Collections.emptyList(),
                    harness.cantina,
                    5);
            assertTrue(bot.name()
                            + " must restore the same ground formation without a ship package",
                    hasGroundEstablishPlan(noShipGroundPlans));
            assertEquals(bot.name(),
                    List.of("Admiral Ozzel", "Captain Lennox"),
                    groundEstablishCardNames(noShipGroundPlans));
        }
    }

    @Test
    public void impossibleSpacePackagesLeaveEndorGroundEstablishAvailable()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);

            assertNoPackageAndGroundStillAvailable(
                    harness,
                    "no ship",
                    List.of(
                            harness.cardInfo(
                                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID),
                            harness.cardInfo(
                                    CAPTAIN_LENNOX_BP, CAPTAIN_LENNOX_ID)),
                    Collections.emptyList(),
                    5);

            assertNoPackageAndGroundStillAvailable(
                    harness,
                    "no pilot",
                    List.of(
                            harness.cardInfo(
                                    COUNT_DOOKU_BP, COUNT_DOOKU_ID),
                            harness.cardInfo(
                                    TROOPER_DAVIN_FELTH_BP,
                                    TROOPER_DAVIN_FELTH_ID)),
                    List.of(harness.cardInfo(SLAVE_I_BP, SLAVE_I_ID)),
                    7);

            assertNoPackageAndGroundStillAvailable(
                    harness,
                    "short Force",
                    List.of(
                            harness.cardInfo(
                                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID),
                            harness.cardInfo(
                                    DS_61_3_BP, DS_61_3_ID)),
                    List.of(harness.cardInfo(SLAVE_I_BP, SLAVE_I_ID)),
                    2);
        }
    }

    private static void assertNoPackageAndGroundStillAvailable(
            Harness harness,
            String boundary,
            List<?> characters,
            List<?> starships,
            int forceAvailable) throws Exception {
        List<Object> pilots = new ArrayList<>();
        for (Object character : characters) {
            if ((boolean) field(character, "isPilot")) {
                pilots.add(character);
            }
        }
        Object packagePlan = invokeEopPlan(
                harness, starships, pilots, forceAvailable);
        boolean fundedPackage = !instructions(packagePlan).isEmpty();
        assertFalse(harness.bot + " " + boundary, fundedPackage);
        assertFalse(harness.bot + " " + boundary,
                EndorOperationsTacticalPolicy
                        .shouldSuppressEmptyEndorGroundEstablish(
                                fundedPackage, true));

        List<?> groundPlans = invokeGroundPlans(
                harness, characters, starships,
                harness.endorBunker, forceAvailable);
        assertTrue(harness.bot + " " + boundary
                        + " must not suppress Endor ground establishment",
                hasGroundEstablishPlan(groundPlans));
    }

    private static Object invokeEopPlan(
            Harness harness,
            List<?> starships,
            List<?> pilots,
            int forceAvailable) throws Exception {
        Method method = harness.planner.getClass().getDeclaredMethod(
                "generateEopEndorSystemPlan",
                List.class, List.class, List.class, int.class, int.class);
        method.setAccessible(true);
        return method.invoke(
                harness.planner,
                starships,
                pilots,
                List.of(harness.endor),
                forceAvailable,
                4);
    }

    private static boolean invokeEndorSystemOccupationPending(
            Harness harness) throws Exception {
        Method method = harness.planner.getClass().getDeclaredMethod(
                "endorSystemOccupationPending", List.class);
        method.setAccessible(true);
        return (boolean) method.invoke(
                harness.planner, List.of(harness.endor));
    }

    private static List<?> invokeGroundPlans(
            Harness harness,
            List<?> characters,
            List<?> starships,
            AiBoardAnalyzer.LocationAnalysis target,
            int forceAvailable) throws Exception {
        Class<?> categoriesClass = Class.forName(
                harness.planner.getClass().getName() + "$LocationCategories");
        Constructor<?> categoriesConstructor =
                categoriesClass.getDeclaredConstructor();
        categoriesConstructor.setAccessible(true);
        Object categories = categoriesConstructor.newInstance();
        Field establishTargets =
                categoriesClass.getDeclaredField("establishTargets");
        establishTargets.setAccessible(true);
        ((List<AiBoardAnalyzer.LocationAnalysis>)
                establishTargets.get(categories)).add(target);

        Class<?> drainGapClass = Class.forName(
                harness.bot.strategyPackage + ".DrainGapResult");
        Object drainGap = drainGapClass
                .getConstructor(int.class, int.class, int.class, List.class)
                .newInstance(0, 0, 0, Collections.emptyList());
        Method method = harness.planner.getClass().getDeclaredMethod(
                "generateGroundPlans",
                List.class,
                List.class,
                List.class,
                categoriesClass,
                int.class,
                int.class,
                int.class,
                List.class,
                int.class,
                drainGapClass);
        method.setAccessible(true);
        return (List<?>) method.invoke(
                harness.planner,
                characters,
                Collections.emptyList(),
                starships,
                categories,
                forceAvailable,
                forceAvailable,
                4,
                List.of(harness.endor, target),
                3,
                drainGap);
    }

    private static boolean hasGroundEstablishPlan(List<?> plans)
            throws Exception {
        for (Object plan : plans) {
            if ("ground_establish".equals(field(plan, "domain"))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> groundEstablishCardNames(List<?> plans)
            throws Exception {
        for (Object scoredPlan : plans) {
            if (!"ground_establish".equals(field(scoredPlan, "domain"))) {
                continue;
            }
            Object plan = field(scoredPlan, "plan");
            List<String> names = new ArrayList<>();
            for (Object instruction : instructions(plan)) {
                names.add(invokeString(instruction, "getCardName"));
            }
            return names;
        }
        return Collections.emptyList();
    }

    private static List<?> instructions(Object plan) throws Exception {
        return (List<?>) plan.getClass()
                .getMethod("getInstructions")
                .invoke(plan);
    }

    private static Object field(Object target, String fieldName)
            throws Exception {
        Field field = target.getClass().getField(fieldName);
        return field.get(target);
    }

    private static String invokeString(Object target, String methodName)
            throws Exception {
        return (String) target.getClass().getMethod(methodName).invoke(target);
    }

    private static int invokeInt(Object target, String methodName)
            throws Exception {
        return (Integer) target.getClass()
                .getMethod(methodName)
                .invoke(target);
    }

    private static Harness harness(Bot bot) throws Exception {
        PhysicalCard endor = card(ENDOR_BP, ENDOR_ID, Zone.LOCATIONS);
        PhysicalCard bunker =
                card(ENDOR_BUNKER_BP, BUNKER_ID, Zone.LOCATIONS);
        PhysicalCard cantina =
                card(CANTINA_BP, CANTINA_ID, Zone.LOCATIONS);
        Object planner = bot.newPlanner();
        return new Harness(
                bot,
                planner,
                analysis(endor, 0, 0, 0, 0, false, true),
                analysis(bunker, 0, 0, 0, 1, true, false),
                analysis(cantina, 0, 0, 0, 1, true, false));
    }

    private static AiBoardAnalyzer.LocationAnalysis analysis(
            PhysicalCard location,
            float ourPower,
            float theirPower,
            float ourAbility,
            int theirForceIcons,
            boolean site,
            boolean system) {
        return new AiBoardAnalyzer.LocationAnalysis(
                location,
                ourPower,
                theirPower,
                ourAbility,
                0,
                0,
                theirForceIcons,
                0,
                0,
                ContestStatus.EMPTY,
                true,
                site,
                site,
                site,
                system);
    }

    private static PhysicalCard card(
            String blueprintId,
            int cardId,
            Zone zone) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(blueprintId);
        assertNotNull("Missing real blueprint " + blueprintId, blueprint);
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getZone()).thenReturn(zone);
        return card;
    }

    private enum Bot {
        RANDO("com.gempukku.swccgo.ai.models.rando.strategy") {
            @Override
            Object newPlanner() {
                var analyzer = mock(
                        com.gempukku.swccgo.ai.models.rando.strategy
                                .ObjectiveAnalyzer.class);
                configure(analyzer);
                var planner =
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeployPhasePlanner();
                planner.setObjectiveAnalyzer(analyzer);
                return planner;
            }
        },
        CHOSEN_ONE("com.gempukku.swccgo.ai.models.chosenone.strategy") {
            @Override
            Object newPlanner() {
                var analyzer = mock(
                        com.gempukku.swccgo.ai.models.chosenone.strategy
                                .ObjectiveAnalyzer.class);
                configure(analyzer);
                var planner =
                        new com.gempukku.swccgo.ai.models.chosenone.strategy
                                .DeployPhasePlanner();
                planner.setObjectiveAnalyzer(analyzer);
                return planner;
            }
        };

        private final String strategyPackage;

        Bot(String strategyPackage) {
            this.strategyPackage = strategyPackage;
        }

        abstract Object newPlanner();

        private static void configure(
                com.gempukku.swccgo.ai.models.common.strategy
                        .ObjectiveAnalyzer analyzer) {
            when(analyzer.isAnalyzed()).thenReturn(true);
            when(analyzer.isFlipped()).thenReturn(true);
            when(analyzer.getObjectiveBlueprintId())
                    .thenReturn(OBJECTIVE_BP);
            when(analyzer.getObjectiveTitle())
                    .thenReturn("Imperial Outpost");
        }
    }

    private static final class Harness {
        private final Bot bot;
        private final Object planner;
        private final AiBoardAnalyzer.LocationAnalysis endor;
        private final AiBoardAnalyzer.LocationAnalysis endorBunker;
        private final AiBoardAnalyzer.LocationAnalysis cantina;

        private Harness(
                Bot bot,
                Object planner,
                AiBoardAnalyzer.LocationAnalysis endor,
                AiBoardAnalyzer.LocationAnalysis endorBunker,
                AiBoardAnalyzer.LocationAnalysis cantina) {
            this.bot = bot;
            this.planner = planner;
            this.endor = endor;
            this.endorBunker = endorBunker;
            this.cantina = cantina;
        }

        private Object cardInfo(String blueprintId, int cardId)
                throws Exception {
            Class<?> cardInfoClass =
                    Class.forName(bot.strategyPackage + ".CardInfo");
            return cardInfoClass
                    .getConstructor(PhysicalCard.class)
                    .newInstance(card(blueprintId, cardId, Zone.HAND));
        }
    }
}
