package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.game.PhysicalCard;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
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
}
