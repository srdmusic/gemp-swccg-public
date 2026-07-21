package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployPlanRankingAdapterParityTest {

    private static final int LOCATION_ID = 293;

    @Test
    public void unresolvedTargetKeepsBasePowerOnlyInBothBots() throws Exception {
        var randoPlan = randoPlan(randoInstruction("missing", 4, 4));
        var chosenPlan = chosenPlan(chosenInstruction("missing", 4, 4));

        float rando = scoreRando(newRandoPlanner(), randoPlan, List.of());
        float chosen = scoreChosen(newChosenPlanner(), chosenPlan, List.of());

        assertBits(8.0f, rando);
        assertBits(rando, chosen);
    }

    @Test
    public void sameTargetAggregatesTacticalScoreExactlyOnce() throws Exception {
        var randoPlan = randoPlan(
                randoInstruction(String.valueOf(LOCATION_ID), 2, 2),
                randoInstruction(String.valueOf(LOCATION_ID), 2, 2));
        var chosenPlan = chosenPlan(
                chosenInstruction(String.valueOf(LOCATION_ID), 2, 2),
                chosenInstruction(String.valueOf(LOCATION_ID), 2, 2));
        List<AiBoardAnalyzer.LocationAnalysis> locations =
                List.of(location("Aggregate", 4.0f, 0, 0));

        float rando = scoreRando(newRandoPlanner(), randoPlan, locations);
        float chosen = scoreChosen(newChosenPlanner(), chosenPlan, locations);

        assertBits(38.0f, rando);
        assertBits(rando, chosen);
    }

    @Test
    public void v32FallbackRemainsPerInstructionBeforeAggregation()
            throws Exception {
        var randoPlan = randoPlan(
                randoInstruction(String.valueOf(LOCATION_ID), 2, 0),
                randoInstruction(String.valueOf(LOCATION_ID), 2, 0));
        var chosenPlan = chosenPlan(
                chosenInstruction(String.valueOf(LOCATION_ID), 2, 0),
                chosenInstruction(String.valueOf(LOCATION_ID), 2, 0));
        List<AiBoardAnalyzer.LocationAnalysis> locations =
                List.of(location("Fallback", 4.0f, 0, 0));

        float rando = scoreRando(newRandoPlanner(), randoPlan, locations);
        float chosen = scoreChosen(newChosenPlanner(), chosenPlan, locations);

        assertBits(38.0f, rando);
        assertBits(rando, chosen);
    }

    @Test
    public void bothBotsKeepFullRawVectorAndSortedDomains() throws Exception {
        List<Scenario> scenarios = List.of(
                new Scenario("adv0", 4, 4, 4.0f, false, false),
                new Scenario("adv1", 5, 4, 4.0f, false, false),
                new Scenario("adv4", 8, 4, 4.0f, false, false),
                new Scenario("establish_4_3", 4, 3, 0.0f, false, false),
                new Scenario("establish_5_3", 5, 3, 0.0f, false, false),
                new Scenario("establish_5_4", 5, 4, 0.0f, false, false),
                new Scenario("objective_capital", 8, 4, 4.0f, true, true));
        float[] expected = {38.0f, 65.0f, 131.0f, -452.0f,
                50.0f, 75.0f, 481.0f};

        var randoPlanner = newRandoPlanner();
        var chosenPlanner = newChosenPlanner();
        configureObjective(randoPlanner);
        configureObjective(chosenPlanner);
        List<com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan>
                randoPlans = new ArrayList<>();
        List<com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan>
                chosenPlans = new ArrayList<>();

        for (int i = 0; i < scenarios.size(); i++) {
            Scenario scenario = scenarios.get(i);
            float rando = scoreRandoScenario(randoPlanner, scenario);
            float chosen = scoreChosenScenario(chosenPlanner, scenario);
            assertBits(expected[i], rando);
            assertBits(rando, chosen);
            randoPlans.add(new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                    randoPlan(), rando, scenario.domain()));
            chosenPlans.add(new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                    chosenPlan(), chosen, scenario.domain()));
        }

        Scenario objective = scenarios.get(scenarios.size() - 1);
        assertBits(281.0f, scoreRandoCoreScenario(randoPlanner, objective));
        assertBits(281.0f, scoreChosenCoreScenario(chosenPlanner, objective));

        Collections.sort(randoPlans);
        Collections.sort(chosenPlans);
        List<String> randoDomains = randoPlans.stream()
                .map(plan -> plan.domain).toList();
        List<String> chosenDomains = chosenPlans.stream()
                .map(plan -> plan.domain).toList();
        assertEquals(List.of("objective_capital", "adv4", "establish_5_4",
                "adv1", "establish_5_3", "adv0", "establish_4_3"),
                randoDomains);
        assertEquals(randoDomains, chosenDomains);
    }

    @Test
    public void bothBotsPlanTheExactActorAndBuddyAtTheFlipGate() throws Exception {
        FormationFixture fixture = formationFixture();

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, fixture.game());
        var randoPlan = (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeFormationPlan(randoPlanner,
                        List.of(
                                new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                        fixture.actor()),
                                new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                        fixture.buddy())),
                        fixture.locations());

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, fixture.game());
        var chosenPlan = (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeFormationPlan(chosenPlanner,
                        List.of(
                                new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                        fixture.actor()),
                                new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                        fixture.buddy())),
                        fixture.locations());

        assertEquals(2, randoPlan.getInstructions().size());
        assertEquals(2, chosenPlan.getInstructions().size());
        assertEquals("Neimoidian", randoPlan.getInstructions().get(0).getCardName());
        assertEquals("Neimoidian", chosenPlan.getInstructions().get(0).getCardName());
        assertEquals("293", randoPlan.getInstructions().get(0).getTargetLocationId());
        assertEquals("293", randoPlan.getInstructions().get(1).getTargetLocationId());
        assertEquals("293", chosenPlan.getInstructions().get(0).getTargetLocationId());
        assertEquals("293", chosenPlan.getInstructions().get(1).getTargetLocationId());
        assertEquals(2, randoPlan.getInstructions().get(0).getAbilityContribution());
        assertEquals(2, randoPlan.getInstructions().get(1).getAbilityContribution());
        assertEquals(randoPlan.getReason(), chosenPlan.getReason());
        assertTrue(scoreRandoFormation(randoPlanner, randoPlan, fixture.locations())
                >= com.gempukku.swccgo.ai.models.rando.RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD);
        assertTrue(scoreChosenFormation(chosenPlanner, chosenPlan, fixture.locations())
                >= com.gempukku.swccgo.ai.models.chosenone.RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD);

        assertNull(invokeFormationPlan(randoPlanner,
                List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                        fixture.actor())), fixture.locations()));
        assertNull(invokeFormationPlan(chosenPlanner,
                List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                        fixture.actor())), fixture.locations()));
    }

    @Test
    public void existingBuddyAllowsTheExactActorToJoinAloneInBothBots()
            throws Exception {
        FormationFixture fixture = formationFixture();
        PhysicalCard existingBuddy = character(
                "14_202", "Existing Buddy", 103, 3, 2, 2);
        when(existingBuddy.getOwner()).thenReturn("player");
        when(fixture.game().getGameState().getCardsAtLocation(fixture.location()))
                .thenReturn(List.of(existingBuddy));

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, fixture.game());
        var randoPlan = (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeFormationPlan(randoPlanner,
                        List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                fixture.actor())), fixture.locations());

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, fixture.game());
        var chosenPlan = (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeFormationPlan(chosenPlanner,
                        List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                fixture.actor())), fixture.locations());

        assertNotNull(randoPlan);
        assertNotNull(chosenPlan);
        assertEquals(1, randoPlan.getInstructions().size());
        assertEquals(1, chosenPlan.getInstructions().size());
        assertEquals("Neimoidian", randoPlan.getInstructions().get(0).getCardName());
        assertEquals("Neimoidian", chosenPlan.getInstructions().get(0).getCardName());
    }

    @Test
    public void postFlipGateWithoutControlStillReceivesReinforcementInBothBots()
            throws Exception {
        FormationFixture fixture = formationFixture();
        PhysicalCard first = character("14_202", "First", 103, 3, 2, 2);
        PhysicalCard second = character("14_203", "Second", 104, 3, 2, 2);
        when(first.getOwner()).thenReturn("player");
        when(second.getOwner()).thenReturn("player");
        when(fixture.game().getGameState().getCardsAtLocation(fixture.location()))
                .thenReturn(List.of(first, second));

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(randoAnalyzer, fixture);
        when(randoAnalyzer.isFlipped()).thenReturn(true);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, fixture.game());
        var randoPlan = (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeFormationPlan(randoPlanner,
                        List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                fixture.buddy())), fixture.locations());

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(chosenAnalyzer, fixture);
        when(chosenAnalyzer.isFlipped()).thenReturn(true);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, fixture.game());
        var chosenPlan = (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeFormationPlan(chosenPlanner,
                        List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                fixture.buddy())), fixture.locations());

        assertNotNull(randoPlan);
        assertNotNull(chosenPlan);
        assertEquals(1, randoPlan.getInstructions().size());
        assertEquals(1, chosenPlan.getInstructions().size());
        assertEquals("Buddy", randoPlan.getInstructions().get(0).getCardName());
        assertEquals("Buddy", chosenPlan.getInstructions().get(0).getCardName());
    }

    private static float scoreRandoScenario(
            com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner planner,
            Scenario scenario) throws Exception {
        var plan = randoPlan(randoInstruction(String.valueOf(LOCATION_ID),
                scenario.power(), scenario.ability()));
        List<AiBoardAnalyzer.LocationAnalysis> locations = List.of(
                location(scenario.objective() ? "Objective" : scenario.domain(),
                        scenario.theirPower(), 0, 0));
        return scenario.capital()
                ? scoreRandoCapital(planner, plan, locations)
                : scoreRando(planner, plan, locations);
    }

    private static float scoreChosenScenario(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner,
            Scenario scenario) throws Exception {
        var plan = chosenPlan(chosenInstruction(String.valueOf(LOCATION_ID),
                scenario.power(), scenario.ability()));
        List<AiBoardAnalyzer.LocationAnalysis> locations = List.of(
                location(scenario.objective() ? "Objective" : scenario.domain(),
                        scenario.theirPower(), 0, 0));
        return scenario.capital()
                ? scoreChosenCapital(planner, plan, locations)
                : scoreChosen(planner, plan, locations);
    }

    private static float scoreRandoCoreScenario(
            com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner planner,
            Scenario scenario) throws Exception {
        return scoreRando(planner,
                randoPlan(randoInstruction(String.valueOf(LOCATION_ID),
                        scenario.power(), scenario.ability())),
                List.of(location("Objective", scenario.theirPower(), 0, 0)));
    }

    private static float scoreChosenCoreScenario(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner,
            Scenario scenario) throws Exception {
        return scoreChosen(planner,
                chosenPlan(chosenInstruction(String.valueOf(LOCATION_ID),
                        scenario.power(), scenario.ability())),
                List.of(location("Objective", scenario.theirPower(), 0, 0)));
    }

    private static void configureObjective(
            com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner planner) {
        var analyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isObjectiveRelevantLocation("Objective")).thenReturn(true);
        when(analyzer.getLocationObjectiveBonus("Objective")).thenReturn(150.0f);
        planner.setObjectiveAnalyzer(analyzer);
    }

    private static void configureFormationAnalyzer(
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer analyzer,
            FormationFixture fixture) {
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.hasFlipGateActorRequirement()).thenReturn(true);
        when(analyzer.getFlipCriticalControlSite()).thenReturn(
                "Naboo: Theed Palace Throne Room");
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.hasFlipGateActorAtLocation(
                fixture.game(), "player", fixture.location())).thenReturn(false);
        when(analyzer.matchesFlipGateActorRequirement(
                fixture.game(), "player", fixture.actor(), fixture.location()))
                .thenReturn(true);
        when(analyzer.getActivePlaybook()).thenReturn(flipGatePlaybook());
    }

    private static void configureFormationAnalyzer(
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer analyzer,
            FormationFixture fixture) {
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.hasFlipGateActorRequirement()).thenReturn(true);
        when(analyzer.getFlipCriticalControlSite()).thenReturn(
                "Naboo: Theed Palace Throne Room");
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.hasFlipGateActorAtLocation(
                fixture.game(), "player", fixture.location())).thenReturn(false);
        when(analyzer.matchesFlipGateActorRequirement(
                fixture.game(), "player", fixture.actor(), fixture.location()))
                .thenReturn(true);
        when(analyzer.getActivePlaybook()).thenReturn(flipGatePlaybook());
    }

    private static com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectivePlaybook
    flipGatePlaybook() {
        return new com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectivePlaybook(
                "Invasion", null, null, null,
                new com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectiveWeights(
                        0.0f, 0.0f, 0.0f, 0.0f, 1600.0f));
    }

    private static void configurePlannerState(Object planner, SwccgGame game)
            throws Exception {
        Field gameField = planner.getClass().getDeclaredField("currentGame");
        gameField.setAccessible(true);
        gameField.set(planner, game);
        Field playerField = planner.getClass().getDeclaredField("currentPlayerId");
        playerField.setAccessible(true);
        playerField.set(planner, "player");
    }

    private static Object invokeFormationPlan(
            Object planner, List<?> characters,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        Method method = planner.getClass().getDeclaredMethod(
                "generateFlipGateFormationPlan", List.class, List.class, int.class);
        method.setAccessible(true);
        return method.invoke(planner, characters, locations, 8);
    }

    private static FormationFixture formationFixture() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint locationBlueprint = mock(SwccgCardBlueprint.class);
        when(game.getGameState()).thenReturn(gameState);
        when(location.getBlueprint()).thenReturn(locationBlueprint);
        when(locationBlueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(location.getCardId()).thenReturn(LOCATION_ID);
        when(location.getTitle()).thenReturn("Naboo: Theed Palace Throne Room");
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());

        PhysicalCard actor = character("14_200", "Neimoidian", 101, 4, 2, 2);
        PhysicalCard buddy = character("14_201", "Buddy", 102, 3, 2, 2);
        AiBoardAnalyzer.LocationAnalysis analysis =
                new AiBoardAnalyzer.LocationAnalysis(
                        location, 0.0f, 0.0f, 0.0f, 0.0f,
                        1, 1, 0, 0,
                        AiBoardAnalyzer.ContestStatus.UNCONTESTED, true);
        return new FormationFixture(
                game, location, actor, buddy, List.of(analysis));
    }

    private static PhysicalCard character(
            String blueprintId, String title, int permanentId,
            int power, int ability, int cost) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(title);
        when(card.getPermanentCardId()).thenReturn(permanentId);
        when(card.getCardId()).thenReturn(permanentId + 1000);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn((float) power);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn((float) ability);
        when(blueprint.getDeployCost()).thenReturn((float) cost);
        return card;
    }

    private static void configureObjective(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner) {
        var analyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isObjectiveRelevantLocation("Objective")).thenReturn(true);
        when(analyzer.getLocationObjectiveBonus("Objective")).thenReturn(150.0f);
        planner.setObjectiveAnalyzer(analyzer);
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner
    newRandoPlanner() {
        return new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner(5, 2);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner
    newChosenPlanner() {
        return new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner(5, 2);
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan
    randoPlan(com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction... instructions) {
        var plan = new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                "test");
        for (var instruction : instructions) {
            plan.addInstruction(instruction);
        }
        return plan;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan
    chosenPlan(com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction... instructions) {
        var plan = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.REINFORCE,
                "test");
        for (var instruction : instructions) {
            plan.addInstruction(instruction);
        }
        return plan;
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction
    randoInstruction(String targetId, int power, int ability) {
        var instruction = new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                "1_1", "card", targetId, "location", 1, "test");
        instruction.setPowerContribution(power);
        instruction.setAbilityContribution(ability);
        return instruction;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction
    chosenInstruction(String targetId, int power, int ability) {
        var instruction = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                "1_1", "card", targetId, "location", 1, "test");
        instruction.setPowerContribution(power);
        instruction.setAbilityContribution(ability);
        return instruction;
    }

    private static AiBoardAnalyzer.LocationAnalysis location(
            String title, float theirPower, int ourIcons, int theirIcons) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(LOCATION_ID);
        when(card.getTitle()).thenReturn(title);
        return new AiBoardAnalyzer.LocationAnalysis(
                card, 0.0f, theirPower, 0.0f, 0.0f,
                ourIcons, theirIcons, 0, 0,
                AiBoardAnalyzer.ContestStatus.UNCONTESTED, true);
    }

    private static float scoreRando(
            com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner planner,
            com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        return invokeScore(planner, "scorePlan",
                com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan.class,
                plan, locations);
    }

    private static float scoreChosen(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner,
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        return invokeScore(planner, "scorePlan",
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan.class,
                plan, locations);
    }

    private static float scoreRandoCapital(
            com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner planner,
            com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        return invokeScore(planner, "scoreObjectiveCapitalPlan",
                com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan.class,
                plan, locations);
    }

    private static float scoreChosenCapital(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner,
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        return invokeScore(planner, "scoreObjectiveCapitalPlan",
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan.class,
                plan, locations);
    }

    private static float scoreRandoFormation(
            com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner planner,
            com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        return invokeScore(planner, "scoreObjectiveFlipGateFormationPlan",
                com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan.class,
                plan, locations);
    }

    private static float scoreChosenFormation(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner,
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations) throws Exception {
        return invokeScore(planner, "scoreObjectiveFlipGateFormationPlan",
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan.class,
                plan, locations);
    }

    private static float invokeScore(Object planner, String methodName,
                                     Class<?> planType, Object plan,
                                     List<AiBoardAnalyzer.LocationAnalysis> locations)
            throws Exception {
        Method method = planner.getClass().getDeclaredMethod(
                methodName, planType, List.class, int.class);
        method.setAccessible(true);
        return (Float) method.invoke(planner, plan, locations, 2);
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private record Scenario(String domain, int power, int ability,
                            float theirPower, boolean objective,
                            boolean capital) {
    }

    private record FormationFixture(
            SwccgGame game, PhysicalCard location, PhysicalCard actor,
            PhysicalCard buddy,
            List<AiBoardAnalyzer.LocationAnalysis> locations) {
    }
}
