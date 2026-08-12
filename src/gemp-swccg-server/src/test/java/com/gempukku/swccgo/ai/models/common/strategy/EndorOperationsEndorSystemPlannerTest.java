package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.ContestStatus;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndorOperationsEndorSystemPlannerTest {
    private static final String PLAYER = "dark";
    private static final String OBJECTIVE_BP = "8_167";
    private static final String ENDOR_BP = "8_157";
    private static final String ENDOR_BUNKER_BP = "8_160";
    private static final String ENDOR_PLATFORM_BP = "8_166";
    private static final String SELF_PILOTED_SHIP_BP = "1_303";
    private static final String EXTERNAL_PILOT_SHIP_BP = "1_306";
    private static final String ADMIRAL_OZZEL_BP = "3_82";
    private static final String ALTERNATE_PILOT_BP = "1_174";
    private static final String ESTABLISH_SECRET_BASE_BP = "207_25";

    private static final int ENDOR_ID = 101;
    private static final int BUNKER_ID = 102;
    private static final int PLATFORM_ID = 103;
    private static final int SELF_PILOTED_SHIP_ID = 201;
    private static final int EXTERNAL_PILOT_SHIP_ID = 202;
    private static final int ADMIRAL_OZZEL_ID = 203;
    private static final int ALTERNATE_PILOT_ID = 204;
    private static final int ESTABLISH_SECRET_BASE_ID = 205;

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void preFlipCheapAdmiralCreatesDominatingExactBunkerPlan()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot, false);
            Object ozzelInfo = harness.cardInfo(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID);
            PhysicalCard ozzel = physicalCard(ozzelInfo);
            harness.setDeployCost(
                    ozzel, harness.bunker.location, 2.0f);

            Object plan = invokeBunkerGarrisonPlan(
                    harness, List.of(ozzelInfo), 2);
            List<?> instructions = instructions(plan);
            assertEquals(bot.name(), 1, instructions.size());
            assertEquals(bot.name(), ADMIRAL_OZZEL_BP,
                    invokeString(instructions.get(0),
                            "getCardBlueprintId"));
            assertEquals(bot.name(), "Endor: Bunker",
                    invokeString(instructions.get(0),
                            "getTargetLocationName"));
            assertEquals(bot.name(), 2,
                    invokeInt(instructions.get(0), "getDeployCost"));

            Object noSpacePackage = invokeEopPlan(
                    harness, Collections.emptyList(),
                    Collections.emptyList(), 2);
            List<?> groundPlans = invokeGroundPlans(
                    harness, List.of(ozzelInfo), noSpacePackage,
                    harness.platform, 2);
            Object scored = groundPlans.stream()
                    .filter(candidate -> {
                        try {
                            return "ground_eop_bunker_garrison".equals(
                                    field(candidate, "domain"));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst().orElse(null);
            assertNotNull(bot.name(), scored);
            float garrisonScore = (Float) field(scored, "score");
            float bestOtherScore = Float.NEGATIVE_INFINITY;
            for (Object candidate : groundPlans) {
                if (candidate == scored) continue;
                bestOtherScore = Math.max(
                        bestOtherScore,
                        (Float) field(candidate, "score"));
            }
            assertTrue(bot.name(),
                    garrisonScore > bestOtherScore);
        }
    }

    @Test
    public void bunkerPlanClosesForEnemyPresenceOrReadyRequiredCard()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot, false);
            Object ozzelInfo = harness.cardInfo(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID);
            PhysicalCard ozzel = physicalCard(ozzelInfo);
            harness.setDeployCost(
                    ozzel, harness.bunker.location, 2.0f);

            AiBoardAnalyzer.LocationAnalysis enemyAtBunker = analysis(
                    harness.bunker.location,
                    2.0f, 0.0f, 1.0f, 1.0f,
                    1, 1, true, true, false);
            Object unsafe = invokeBunkerGarrisonPlan(
                    harness, List.of(ozzelInfo), 2,
                    List.of(harness.endor, enemyAtBunker,
                            harness.platform));
            assertTrue(bot.name(), instructions(unsafe).isEmpty());

            PhysicalCard establish = card(
                    ESTABLISH_SECRET_BASE_BP,
                    ESTABLISH_SECRET_BASE_ID, Zone.HAND);
            when(harness.gameState.getHand(PLAYER))
                    .thenReturn(List.of(establish));
            when(harness.analyzer.isRequiredCardForFlip(
                    same(establish))).thenReturn(true);
            when(harness.analyzer
                    .isRequiredOnTableCardPullRouteReady(
                            same(harness.game), eq(PLAYER),
                            same(establish))).thenReturn(true);
            Object finalCardReady = invokeBunkerGarrisonPlan(
                    harness, List.of(ozzelInfo), 2);
            assertTrue(bot.name(),
                    instructions(finalCardReady).isEmpty());
        }
    }

    @Test
    public void anyLegalSelfPilotedShipCanOccupyEndorAtExactForce()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);
            Object shipInfo = harness.cardInfo(
                    SELF_PILOTED_SHIP_BP, SELF_PILOTED_SHIP_ID);
            PhysicalCard ship = physicalCard(shipInfo);
            harness.setAbility(ship, true, 1.0f);
            harness.setDeployCost(ship, harness.endor.location, 3.0f);

            Object funded = invokeEopPlan(
                    harness, List.of(shipInfo), Collections.emptyList(), 3);
            List<?> instructions = instructions(funded);
            assertEquals(bot.name(), 1, instructions.size());
            assertEquals(bot.name(), SELF_PILOTED_SHIP_BP,
                    invokeString(instructions.get(0), "getCardBlueprintId"));
            assertEquals(bot.name(), "Endor",
                    invokeString(instructions.get(0), "getTargetLocationName"));
            assertEquals(bot.name(), 3,
                    invokeInt(instructions.get(0), "getDeployCost"));
            assertEquals(bot.name(), 1.0f,
                    invokeFloat(instructions.get(0),
                            "getAbilityContribution"), 0.0f);

            Object shortForce = invokeEopPlan(
                    harness, List.of(shipInfo), Collections.emptyList(), 2);
            assertTrue(bot.name(), instructions(shortForce).isEmpty());
        }
    }

    @Test
    public void genericCrewPackageKeepsCheapBunkerAdmiralOnGround()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);
            Object shipInfo = harness.cardInfo(
                    EXTERNAL_PILOT_SHIP_BP, EXTERNAL_PILOT_SHIP_ID);
            Object ozzelInfo = harness.cardInfo(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID);
            Object alternateInfo = harness.cardInfo(
                    ALTERNATE_PILOT_BP, ALTERNATE_PILOT_ID);
            PhysicalCard ship = physicalCard(shipInfo);
            PhysicalCard ozzel = physicalCard(ozzelInfo);
            PhysicalCard alternate = physicalCard(alternateInfo);

            harness.setAbility(ship, true, 0.0f);
            harness.setAbility(ozzel, false, 2.0f);
            harness.setAbility(alternate, false, 2.0f);
            harness.setDeployCost(
                    ozzel, harness.bunker.location, 2.0f);
            harness.setDeployCost(
                    alternate, harness.bunker.location, 3.0f);
            harness.setPairCost(
                    ship, ozzel, harness.endor.location, 4.0f);
            harness.setPairCost(
                    ship, alternate, harness.endor.location, 5.0f);

            Object funded = invokeEopPlan(
                    harness,
                    List.of(shipInfo),
                    List.of(ozzelInfo, alternateInfo),
                    5);
            List<?> instructions = instructions(funded);
            assertEquals(bot.name(), 2, instructions.size());
            assertEquals(bot.name(), EXTERNAL_PILOT_SHIP_BP,
                    invokeString(instructions.get(0), "getCardBlueprintId"));
            assertEquals(bot.name(), ALTERNATE_PILOT_BP,
                    invokeString(instructions.get(1), "getCardBlueprintId"));
            assertEquals(bot.name(), String.valueOf(EXTERNAL_PILOT_SHIP_ID),
                    invokeString(instructions.get(1), "getAboardShipCardId"));
            float shipAbility = invokeFloat(instructions.get(0),
                    "getAbilityContribution");
            float pilotAbility = invokeFloat(instructions.get(1),
                    "getAbilityContribution");
            assertEquals(bot.name(), 0.0f, shipAbility, 0.0f);
            assertEquals(bot.name(), 2.0f, pilotAbility, 0.0f);
            assertEquals(bot.name(), 2.0f,
                    shipAbility + pilotAbility, 0.0f);

            Object noPackage = invokeEopPlan(
                    harness,
                    Collections.emptyList(),
                    List.of(ozzelInfo, alternateInfo),
                    5);
            List<?> groundPlans = invokeGroundPlans(
                    harness,
                    List.of(ozzelInfo, alternateInfo),
                    noPackage,
                    harness.platform,
                    5);
            assertTrue(bot.name(), hasGroundEstablishPlan(groundPlans));
            assertTrue(bot.name(),
                    groundEstablishCardNames(groundPlans)
                            .contains("Admiral Ozzel"));
        }
    }

    @Test
    public void permanentPilotShipAndExternalPilotOwnAbilityOnceEach()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);
            Object shipInfo = harness.cardInfo(
                    SELF_PILOTED_SHIP_BP, SELF_PILOTED_SHIP_ID);
            Object pilotInfo = harness.cardInfo(
                    ALTERNATE_PILOT_BP, ALTERNATE_PILOT_ID);
            PhysicalCard ship = physicalCard(shipInfo);
            PhysicalCard pilot = physicalCard(pilotInfo);
            harness.setAbility(ship, true, 1.0f);
            harness.setAbility(pilot, false, 2.0f);
            harness.setDeployCost(
                    ship, harness.endor.location, 6.0f);
            harness.setPairCost(
                    ship, pilot, harness.endor.location, 5.0f);

            Object funded = invokeEopPlan(
                    harness, List.of(shipInfo), List.of(pilotInfo), 5);
            List<?> instructions = instructions(funded);
            assertEquals(bot.name(), 2, instructions.size());
            float shipAbility = invokeFloat(instructions.get(0),
                    "getAbilityContribution");
            float pilotAbility = invokeFloat(instructions.get(1),
                    "getAbilityContribution");
            assertEquals(bot.name(), 1.0f, shipAbility, 0.0f);
            assertEquals(bot.name(), 2.0f, pilotAbility, 0.0f);
            assertEquals(bot.name(), 3.0f,
                    shipAbility + pilotAbility, 0.0f);
        }
    }

    @Test
    public void viableEndorPackageSuppressesOnlyOptionalEndorSpread()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);
            Object shipInfo = harness.cardInfo(
                    SELF_PILOTED_SHIP_BP, SELF_PILOTED_SHIP_ID);
            Object ozzelInfo = harness.cardInfo(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID);
            Object alternateInfo = harness.cardInfo(
                    ALTERNATE_PILOT_BP, ALTERNATE_PILOT_ID);
            PhysicalCard ship = physicalCard(shipInfo);
            harness.setAbility(ship, true, 1.0f);
            harness.setDeployCost(ship, harness.endor.location, 3.0f);
            Object funded = invokeEopPlan(
                    harness, List.of(shipInfo), Collections.emptyList(), 3);

            List<?> suppressed = invokeGroundPlans(
                    harness, List.of(ozzelInfo, alternateInfo), funded,
                    harness.platform, 5);
            assertFalse(bot.name(), hasGroundEstablishPlan(suppressed));

            Object noPackage = invokeEopPlan(
                    harness, Collections.emptyList(),
                    Collections.emptyList(), 3);
            List<?> available = invokeGroundPlans(
                    harness, List.of(ozzelInfo, alternateInfo), noPackage,
                    harness.platform, 5);
            assertTrue(bot.name(), hasGroundEstablishPlan(available));
        }
    }

    @Test
    public void unsafeOrUnfundedSpaceRoutesDoNotCreateAPlan()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Harness harness = harness(bot);
            Object shipInfo = harness.cardInfo(
                    EXTERNAL_PILOT_SHIP_BP, EXTERNAL_PILOT_SHIP_ID);
            Object pilotInfo = harness.cardInfo(
                    ALTERNATE_PILOT_BP, ALTERNATE_PILOT_ID);
            PhysicalCard ship = physicalCard(shipInfo);
            PhysicalCard pilot = physicalCard(pilotInfo);
            harness.setAbility(ship, true, 0.0f);
            harness.setAbility(pilot, false, 2.0f);
            harness.setPairCost(
                    ship, pilot, harness.endor.location, 5.0f);

            assertTrue(bot.name(), instructions(invokeEopPlan(
                    harness, List.of(shipInfo),
                    Collections.emptyList(), 5)).isEmpty());
            assertTrue(bot.name(), instructions(invokeEopPlan(
                    harness, List.of(shipInfo),
                    List.of(pilotInfo), 4)).isEmpty());

            harness.blockPairRoute(ship, pilot);
            assertTrue(bot.name(), instructions(invokeEopPlan(
                    harness, List.of(shipInfo),
                    List.of(pilotInfo), 5)).isEmpty());

            AiBoardAnalyzer.LocationAnalysis enemyHeldEndor = analysis(
                    harness.endor.location,
                    0.0f, 5.0f, 0.0f, 1.0f,
                    0, 1, true, false, true);
            assertTrue(bot.name(), instructions(invokeEopPlan(
                    harness, List.of(shipInfo), List.of(pilotInfo), 5,
                    List.of(enemyHeldEndor, harness.bunker,
                            harness.platform))).isEmpty());
        }
    }

    private static Object invokeEopPlan(
            Harness harness,
            List<?> starships,
            List<?> pilots,
            int forceAvailable) throws Exception {
        return invokeEopPlan(
                harness, starships, pilots, forceAvailable,
                harness.allLocations());
    }

    private static Object invokeEopPlan(
            Harness harness,
            List<?> starships,
            List<?> pilots,
            int forceAvailable,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations)
            throws Exception {
        Method method = harness.planner.getClass().getDeclaredMethod(
                "generateEopEndorSystemPlan",
                List.class, List.class, List.class, int.class);
        method.setAccessible(true);
        return method.invoke(
                harness.planner,
                starships,
                pilots,
                allLocations,
                forceAvailable);
    }

    private static Object invokeBunkerGarrisonPlan(
            Harness harness,
            List<?> characters,
            int forceAvailable) throws Exception {
        return invokeBunkerGarrisonPlan(
                harness, characters, forceAvailable,
                harness.allLocations());
    }

    private static Object invokeBunkerGarrisonPlan(
            Harness harness,
            List<?> characters,
            int forceAvailable,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations)
            throws Exception {
        Method method = harness.planner.getClass().getDeclaredMethod(
                "generateEopBunkerGarrisonPlan",
                List.class, List.class, int.class);
        method.setAccessible(true);
        return method.invoke(
                harness.planner,
                characters,
                allLocations,
                forceAvailable);
    }

    private static List<?> invokeGroundPlans(
            Harness harness,
            List<?> characters,
            Object fundedEndorPackage,
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
        Class<?> planClass = Class.forName(
                harness.bot.strategyPackage + ".DeploymentPlan");
        Method method = harness.planner.getClass().getDeclaredMethod(
                "generateGroundPlans",
                List.class,
                List.class,
                planClass,
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
                fundedEndorPackage,
                categories,
                forceAvailable,
                forceAvailable,
                4,
                harness.allLocations(),
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

    private static PhysicalCard physicalCard(Object cardInfo)
            throws Exception {
        return (PhysicalCard) field(cardInfo, "card");
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

    private static float invokeFloat(Object target, String methodName)
            throws Exception {
        return (Float) target.getClass()
                .getMethod(methodName)
                .invoke(target);
    }

    private static Harness harness(Bot bot) throws Exception {
        return harness(bot, true);
    }

    private static Harness harness(
            Bot bot, boolean flipped) throws Exception {
        PhysicalCard endor = card(ENDOR_BP, ENDOR_ID, Zone.LOCATIONS);
        PhysicalCard bunker =
                card(ENDOR_BUNKER_BP, BUNKER_ID, Zone.LOCATIONS);
        PhysicalCard platform =
                card(ENDOR_PLATFORM_BP, PLATFORM_ID, Zone.LOCATIONS);
        Object planner = bot.newPlanner(flipped);
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        Map<Integer, PhysicalCard> cardsById = new HashMap<>();
        cardsById.put(ENDOR_ID, endor);
        cardsById.put(BUNKER_ID, bunker);
        cardsById.put(PLATFORM_ID, platform);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.findCardByPermanentId(anyInt()))
                .thenAnswer(invocation ->
                        cardsById.get(invocation.getArgument(0)));
        when(gameState.getCaptivesOfEscort(any(PhysicalCard.class)))
                .thenReturn(Collections.emptyList());
        when(gameState.getHand(PLAYER))
                .thenReturn(Collections.emptyList());
        when(gameState.getCardsAtLocation(any(PhysicalCard.class)))
                .thenReturn(Collections.emptyList());
        when(gameState.getAvailablePilotCapacity(
                same(modifiers), any(PhysicalCard.class),
                any(PhysicalCard.class))).thenReturn(1);
        when(modifiers.hasIcon(
                same(gameState), any(PhysicalCard.class), any(Icon.class)))
                .thenAnswer(invocation -> {
                    PhysicalCard candidate = invocation.getArgument(1);
                    Icon icon = invocation.getArgument(2);
                    return candidate != null
                            && candidate.getBlueprint() != null
                            && candidate.getBlueprint().hasIcon(icon);
                });
        when(modifiers.hasKeyword(
                same(gameState), any(PhysicalCard.class), any(Keyword.class)))
                .thenAnswer(invocation -> {
                    PhysicalCard candidate = invocation.getArgument(1);
                    Keyword keyword = invocation.getArgument(2);
                    return candidate != null
                            && candidate.getBlueprint() != null
                            && candidate.getBlueprint().hasKeyword(keyword);
                });
        setDeployable(modifiers);

        setPrivateField(planner, "currentGame", game);
        setPrivateField(planner, "currentPlayerId", PLAYER);
        ObjectiveAnalyzer analyzer =
                (ObjectiveAnalyzer) getPrivateField(
                        planner, "objectiveAnalyzer");
        return new Harness(
                bot,
                planner,
                analyzer,
                game,
                gameState,
                modifiers,
                cardsById,
                analysis(endor, 0.0f, 0.0f, 0.0f, 0.0f,
                        0, 0, true, false, true),
                analysis(bunker, 2.0f, 0.0f, 1.0f, 0.0f,
                        1, 0, true, true, false),
                analysis(platform, 0.0f, 0.0f, 0.0f, 0.0f,
                        0, 0, true, true, false));
    }

    private static void setDeployable(ModifiersQuerying modifiers) {
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);
    }

    private static void setPrivateField(
            Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getPrivateField(
            Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static AiBoardAnalyzer.LocationAnalysis analysis(
            PhysicalCard location,
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            int ourCards,
            int theirCards,
            boolean battleground,
            boolean site,
            boolean system) {
        return new AiBoardAnalyzer.LocationAnalysis(
                location,
                ourPower,
                theirPower,
                ourAbility,
                theirAbility,
                0,
                1,
                ourCards,
                theirCards,
                ContestStatus.EMPTY,
                battleground,
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
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        return card;
    }

    private enum Bot {
        RANDO("com.gempukku.swccgo.ai.models.rando.strategy") {
            @Override
            Object newPlanner(boolean flipped) {
                var analyzer = mock(
                        com.gempukku.swccgo.ai.models.rando.strategy
                                .ObjectiveAnalyzer.class);
                configure(analyzer, flipped);
                var planner =
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeployPhasePlanner();
                planner.setObjectiveAnalyzer(analyzer);
                return planner;
            }
        },
        CHOSEN_ONE("com.gempukku.swccgo.ai.models.chosenone.strategy") {
            @Override
            Object newPlanner(boolean flipped) {
                var analyzer = mock(
                        com.gempukku.swccgo.ai.models.chosenone.strategy
                                .ObjectiveAnalyzer.class);
                configure(analyzer, flipped);
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

        abstract Object newPlanner(boolean flipped);

        private static void configure(
                com.gempukku.swccgo.ai.models.common.strategy
                        .ObjectiveAnalyzer analyzer,
                boolean flipped) {
            when(analyzer.isAnalyzed()).thenReturn(true);
            when(analyzer.isFlipped()).thenReturn(flipped);
            when(analyzer.getObjectiveBlueprintId())
                    .thenReturn(OBJECTIVE_BP);
            when(analyzer.getObjectiveTitle())
                    .thenReturn(flipped
                            ? "Imperial Outpost"
                            : "Endor Operations");
        }
    }

    private static final class Harness {
        private final Bot bot;
        private final Object planner;
        private final ObjectiveAnalyzer analyzer;
        private final SwccgGame game;
        private final GameState gameState;
        private final ModifiersQuerying modifiers;
        private final Map<Integer, PhysicalCard> cardsById;
        private final AiBoardAnalyzer.LocationAnalysis endor;
        private final AiBoardAnalyzer.LocationAnalysis bunker;
        private final AiBoardAnalyzer.LocationAnalysis platform;

        private Harness(
                Bot bot,
                Object planner,
                ObjectiveAnalyzer analyzer,
                SwccgGame game,
                GameState gameState,
                ModifiersQuerying modifiers,
                Map<Integer, PhysicalCard> cardsById,
                AiBoardAnalyzer.LocationAnalysis endor,
                AiBoardAnalyzer.LocationAnalysis bunker,
                AiBoardAnalyzer.LocationAnalysis platform) {
            this.bot = bot;
            this.planner = planner;
            this.analyzer = analyzer;
            this.game = game;
            this.gameState = gameState;
            this.modifiers = modifiers;
            this.cardsById = cardsById;
            this.endor = endor;
            this.bunker = bunker;
            this.platform = platform;
        }

        private List<AiBoardAnalyzer.LocationAnalysis> allLocations() {
            return List.of(endor, bunker, platform);
        }

        private Object cardInfo(String blueprintId, int cardId)
                throws Exception {
            Class<?> cardInfoClass =
                    Class.forName(bot.strategyPackage + ".CardInfo");
            PhysicalCard physicalCard =
                    card(blueprintId, cardId, Zone.HAND);
            cardsById.put(cardId, physicalCard);
            return cardInfoClass
                    .getConstructor(PhysicalCard.class)
                    .newInstance(physicalCard);
        }

        private void setAbility(
                PhysicalCard card,
                boolean includePermanentPilots,
                float ability) {
            when(modifiers.getAbility(
                    same(gameState), same(card),
                    eq(includePermanentPilots))).thenReturn(ability);
        }

        private void setDeployCost(
                PhysicalCard card,
                PhysicalCard target,
                float cost) {
            when(modifiers.getDeployCost(
                    same(gameState),
                    same(card), same(card), same(target),
                    anyBoolean(), isNull(), anyBoolean(),
                    anyFloat(), isNull(), anyBoolean()))
                    .thenReturn(cost);
        }

        private void setPairCost(
                PhysicalCard ship,
                PhysicalCard pilot,
                PhysicalCard target,
                float cost) {
            when(modifiers.getSimultaneousDeployCost(
                    same(gameState),
                    same(ship), same(ship),
                    anyBoolean(), anyFloat(),
                    same(pilot), anyBoolean(), anyFloat(),
                    same(target), isNull(), anyBoolean()))
                    .thenReturn(cost);
        }

        private void blockPairRoute(
                PhysicalCard ship,
                PhysicalCard pilot) {
            when(modifiers.isDeployableToTarget(
                    same(gameState), same(ship), same(ship),
                    anyBoolean(), any(), anyBoolean(), anyFloat(),
                    any(), any(), any(), any(), any(), same(pilot),
                    anyBoolean(), anyFloat()))
                    .thenReturn(false);
        }
    }
}
