package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
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
    public void bothBotsPlanAFreeActorEnablerOnlyWhenTheFutureActorIsFunded()
            throws Exception {
        FormationFixture fixture = formationFixture();
        PhysicalCard sidious = character(
                "208_35", "Lord Sidious", 120, 5, 7, 6);

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(randoAnalyzer, fixture);
        when(randoAnalyzer.getFlipGateActorEnablerFutureDeployCost(
                fixture.game(), "player", sidious)).thenReturn(3);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, fixture.game());

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(chosenAnalyzer, fixture);
        when(chosenAnalyzer.getFlipGateActorEnablerFutureDeployCost(
                fixture.game(), "player", sidious)).thenReturn(3);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, fixture.game());

        assertNull(invokeFormationPlan(randoPlanner,
                List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                        sidious)), fixture.locations(), 8));
        assertNull(invokeFormationPlan(chosenPlanner,
                List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                        sidious)), fixture.locations(), 8));

        var randoPlan =
                (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeFormationPlan(randoPlanner,
                        List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                sidious)), fixture.locations(), 9);
        var chosenPlan =
                (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeFormationPlan(chosenPlanner,
                        List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                sidious)), fixture.locations(), 9);

        assertNotNull(randoPlan);
        assertNotNull(chosenPlan);
        assertEquals(1, randoPlan.getInstructions().size());
        assertEquals(1, chosenPlan.getInstructions().size());
        assertEquals("Lord Sidious",
                randoPlan.getInstructions().get(0).getCardName());
        assertEquals("Lord Sidious",
                chosenPlan.getInstructions().get(0).getCardName());
        assertEquals("293",
                randoPlan.getInstructions().get(0).getTargetLocationId());
        assertEquals("293",
                chosenPlan.getInstructions().get(0).getTargetLocationId());
    }

    @Test
    public void bothBotsRefreshTheSameTurnSwampPlanWhenThePulledGateAppears()
            throws Exception {
        PlanRefreshFixture fixture = planRefreshFixture();

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var randoBefore = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        fixture.locations().set(List.of(fixture.swamp(), fixture.throneRoom()));
        var randoAfter = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        fixture.locations().set(List.of(fixture.swamp()));
        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        var chosenBefore = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        fixture.locations().set(List.of(fixture.swamp(), fixture.throneRoom()));
        var chosenAfter = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        assertEquals("178", randoBefore.getInstructions().get(0).getTargetLocationId());
        assertEquals("178", chosenBefore.getInstructions().get(0).getTargetLocationId());
        assertNotSame(randoBefore, randoAfter);
        assertNotSame(chosenBefore, chosenAfter);
        assertEquals(2, randoAfter.getInstructions().size());
        assertEquals(2, chosenAfter.getInstructions().size());
        assertTrue(randoAfter.getReason().startsWith(
                "V297 objective flip-gate formation"));
        assertEquals(randoAfter.getReason(), chosenAfter.getReason());
        for (var instruction : randoAfter.getInstructions()) {
            assertEquals("293", instruction.getTargetLocationId());
        }
        for (var instruction : chosenAfter.getInstructions()) {
            assertEquals("293", instruction.getTargetLocationId());
        }
    }

    @Test
    public void bothBotsKeepTheCachedPlanWhenTheGateTopologyDoesNotChange() {
        PlanRefreshFixture fixture = planRefreshFixture();

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var randoBefore = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        var randoAfter = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        var chosenBefore = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        var chosenAfter = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        assertSame(randoBefore, randoAfter);
        assertSame(chosenBefore, chosenAfter);
    }

    @Test
    public void bothBotsRefreshSameTurnWhenStructuredPreFlipProgressChanges() {
        PlanRefreshFixture fixture = planRefreshFixture();
        AtomicReference<String> randoFingerprint =
                new AtomicReference<>("regional=0/0;");

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(randoAnalyzer, fixture);
        when(randoAnalyzer.getPreFlipProgressFingerprint(
                fixture.game(), "player")).thenAnswer(
                ignored -> randoFingerprint.get());
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var randoBefore = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        randoFingerprint.set("regional=1/0;");
        var randoAfter = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        AtomicReference<String> chosenFingerprint =
                new AtomicReference<>("regional=0/0;");
        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(chosenAnalyzer, fixture);
        when(chosenAnalyzer.getPreFlipProgressFingerprint(
                fixture.game(), "player")).thenAnswer(
                ignored -> chosenFingerprint.get());
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        var chosenBefore = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        chosenFingerprint.set("regional=1/0;");
        var chosenAfter = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        assertNotSame(randoBefore, randoAfter);
        assertNotSame(chosenBefore, chosenAfter);
    }

    @Test
    public void bothBotsKeepCachedPlanWhenStructuredPreFlipProgressIsStable() {
        PlanRefreshFixture fixture = planRefreshFixture();

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(randoAnalyzer, fixture);
        when(randoAnalyzer.getPreFlipProgressFingerprint(
                fixture.game(), "player")).thenReturn("regional=1/0;");
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var randoBefore = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        var randoAfter = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(chosenAnalyzer, fixture);
        when(chosenAnalyzer.getPreFlipProgressFingerprint(
                fixture.game(), "player")).thenReturn("regional=1/0;");
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        var chosenBefore = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        var chosenAfter = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        assertSame(randoBefore, randoAfter);
        assertSame(chosenBefore, chosenAfter);
    }

    @Test
    public void bothBotsRefreshSameTurnWhenPersistentEvidenceRevisionChanges() {
        PlanRefreshFixture fixture = planRefreshFixture();
        AtomicReference<PersistentResponsePolicy.Snapshot> randoSnapshot =
                new AtomicReference<>(new PersistentResponsePolicy.Snapshot(
                        1, 1, java.util.Map.of()));
        var randoStrategy = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.StrategyController.class);
        when(randoStrategy.getPersistentResponseSnapshot()).thenAnswer(
                ignored -> randoSnapshot.get());
        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        randoPlanner.setStrategyController(randoStrategy);
        var randoBefore = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        assertSame(randoBefore, randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK));
        randoSnapshot.set(new PersistentResponsePolicy.Snapshot(
                2, 2, java.util.Map.of()));
        var randoAfter = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        AtomicReference<PersistentResponsePolicy.Snapshot> chosenSnapshot =
                new AtomicReference<>(new PersistentResponsePolicy.Snapshot(
                        1, 1, java.util.Map.of()));
        var chosenStrategy = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.StrategyController.class);
        when(chosenStrategy.getPersistentResponseSnapshot()).thenAnswer(
                ignored -> chosenSnapshot.get());
        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        chosenPlanner.setStrategyController(chosenStrategy);
        var chosenBefore = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);
        assertSame(chosenBefore, chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK));
        chosenSnapshot.set(new PersistentResponsePolicy.Snapshot(
                2, 2, java.util.Map.of()));
        var chosenAfter = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        assertNotSame(randoBefore, randoAfter);
        assertNotSame(chosenBefore, chosenAfter);
        assertEquals(randoAfter.getReason(), chosenAfter.getReason());
    }

    @Test
    public void bothPlanScoresIgnoreActionOnlyResponseBands()
            throws Exception {
        var randoPlan = randoPlan(
                randoInstruction(String.valueOf(LOCATION_ID), 4, 4));
        var chosenPlan = chosenPlan(
                chosenInstruction(String.valueOf(LOCATION_ID), 4, 4));
        PersistentResponsePolicy.Obligation obligation =
                responseObligation(LOCATION_ID, 300, 250,
                        PersistentResponsePolicy.Mode.REINFORCE);
        randoPlan.setPersistentResponseObligation(obligation);
        chosenPlan.setPersistentResponseObligation(obligation);
        List<AiBoardAnalyzer.LocationAnalysis> locations =
                List.of(location("Objective gate", 4.0f, 0, 0));

        float rando = scoreRando(newRandoPlanner(), randoPlan, locations);
        float chosen = scoreChosen(newChosenPlanner(), chosenPlan, locations);

        assertBits(38.0f, rando);
        assertBits(rando, chosen);
    }

    @Test
    public void preservedAlternativeDoesNotBypassLegacyEarlyHold()
            throws Exception {
        var randoScored =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoPlan(randoInstruction(
                                String.valueOf(LOCATION_ID), 2, 2)),
                        10.0f, "race-alternative");
        var chosenScored =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                        chosenPlan(chosenInstruction(
                                String.valueOf(LOCATION_ID), 2, 2)),
                        10.0f, "race-alternative");

        assertTrue(invokeEarlyHoldOverride(
                newRandoPlanner(), randoScored,
                responseObligation(LOCATION_ID, 300, 0,
                        PersistentResponsePolicy.Mode.CONTEST)));
        assertTrue(invokeEarlyHoldOverride(
                newChosenPlanner(), chosenScored,
                responseObligation(LOCATION_ID, 300, 0,
                        PersistentResponsePolicy.Mode.CONTEST)));
        assertTrue(!invokeEarlyHoldOverride(
                newRandoPlanner(), randoScored,
                alternativeObligation()));
        assertTrue(!invokeEarlyHoldOverride(
                newChosenPlanner(), chosenScored,
                alternativeObligation()));
    }

    @Test
    public void v170PlanCarriesNoBatchOnePlanBand() throws Exception {
        var randoPlan = randoPlan(
                randoInstruction(String.valueOf(LOCATION_ID), 4, 4));
        var chosenPlan = chosenPlan(
                chosenInstruction(String.valueOf(LOCATION_ID), 4, 4));
        PersistentResponsePolicy.Obligation spy = responseObligation(
                LOCATION_ID, 0, 0, PersistentResponsePolicy.Mode.SPY);
        randoPlan.setPersistentResponseObligation(spy);
        chosenPlan.setPersistentResponseObligation(spy);
        List<AiBoardAnalyzer.LocationAnalysis> locations =
                List.of(location("Persistent lane", 4.0f, 0, 0));

        float rando = scoreRando(newRandoPlanner(), randoPlan, locations);
        float chosen = scoreChosen(newChosenPlanner(), chosenPlan, locations);

        assertBits(38.0f, rando);
        assertBits(rando, chosen);
    }

    @Test
    public void commonLocationPreludeDoesNotSuppressSelectedResponse()
            throws Exception {
        PhysicalCard prelude = locationCard(
                "5_999", "Shared Prelude", 600, CardSubtype.SITE);
        var obligation = responseObligation(
                LOCATION_ID, 300, 250,
                PersistentResponsePolicy.Mode.REINFORCE);

        var randoResponseInstruction = randoInstruction(
                String.valueOf(LOCATION_ID), 6, 4);
        randoResponseInstruction.setCardPermanentCardId(10);
        randoResponseInstruction.setCardCurrentCardId(20);
        var randoResponsePlan = randoPlan(randoResponseInstruction);
        var randoResponse =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoResponsePlan, 10.0f, "ground-response");
        var randoDistraction =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoPlan(randoInstruction(
                                String.valueOf(LOCATION_ID), 2, 2)),
                        100.0f, "remote-development");
        var randoSelected =
                (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeSelectBestPlan(newRandoPlanner(),
                        new ArrayList<>(List.of(
                                randoDistraction, randoResponse)),
                        List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                prelude)), randoResponse, obligation);

        var chosenResponseInstruction = chosenInstruction(
                String.valueOf(LOCATION_ID), 6, 4);
        chosenResponseInstruction.setCardPermanentCardId(10);
        chosenResponseInstruction.setCardCurrentCardId(20);
        var chosenResponsePlan = chosenPlan(chosenResponseInstruction);
        var chosenResponse =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                        chosenResponsePlan, 10.0f, "ground-response");
        var chosenDistraction =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                        chosenPlan(chosenInstruction(
                                String.valueOf(LOCATION_ID), 2, 2)),
                        100.0f, "remote-development");
        var chosenSelected =
                (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeSelectBestPlan(newChosenPlanner(),
                        new ArrayList<>(List.of(
                                chosenDistraction, chosenResponse)),
                        List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                prelude)), chosenResponse, obligation);

        assertEquals(2, randoSelected.getInstructions().size());
        assertEquals(2, chosenSelected.getInstructions().size());
        assertEquals("Shared Prelude",
                randoSelected.getInstructions().get(0).getCardName());
        assertEquals("Shared Prelude",
                chosenSelected.getInstructions().get(0).getCardName());
        assertEquals(Integer.valueOf(10), randoSelected.getInstructions()
                .get(1).getCardPermanentCardId());
        assertEquals(Integer.valueOf(10), chosenSelected.getInstructions()
                .get(1).getCardPermanentCardId());
        assertSame(obligation,
                randoSelected.getPersistentResponseObligation());
        assertSame(obligation,
                chosenSelected.getPersistentResponseObligation());
    }

    @Test
    public void nonemptyPreludeReachesTheRealPersistentCandidateSelector()
            throws Exception {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard targetCard = locationCard(
                "5_78", "Carbonite Chamber", LOCATION_ID,
                CardSubtype.SITE);
        PhysicalCard prelude = locationCard(
                "5_999", "Shared Prelude", 600, CardSubtype.SITE);
        PhysicalCard remoteEndor = locationCard(
                "8_145", "Endor: Landing Platform", 294,
                CardSubtype.SITE);
        PhysicalCard lead = character(
                "1_101", "Lead", 10, 4, 2, 2);
        PhysicalCard buddy = character(
                "1_102", "Buddy", 11, 4, 2, 2);
        PhysicalCard remoteDeveloper = character(
                "1_103", "Remote EOPS Developer", 12, 6, 3, 3);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getCurrentPlayerId()).thenReturn("player");
        when(gameState.getCurrentPhase()).thenReturn(Phase.DEPLOY);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getHand("player")).thenReturn(
                List.of(lead, buddy, remoteDeveloper));
        when(gameState.findCardByPermanentId(10)).thenReturn(lead);
        when(gameState.findCardByPermanentId(11)).thenReturn(buddy);
        when(gameState.findCardByPermanentId(12))
                .thenReturn(remoteDeveloper);
        when(gameState.findCardById(LOCATION_ID)).thenReturn(targetCard);
        when(gameState.findCardById(294)).thenReturn(remoteEndor);
        when(gameState.getCardsAtLocation(targetCard)).thenReturn(List.of());
        when(gameState.getCardsAtLocation(remoteEndor)).thenReturn(List.of());
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(gameState, lead, lead, targetCard,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);
        when(modifiers.getDeployCost(gameState, buddy, buddy, targetCard,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);
        when(modifiers.getDeployCost(gameState, remoteDeveloper,
                remoteDeveloper, remoteEndor, false, null, false,
                0.0f, null, true)).thenReturn(3.0f);
        AiBoardAnalyzer.LocationAnalysis target =
                new AiBoardAnalyzer.LocationAnalysis(
                        targetCard, 0.0f, 4.0f, 0.0f, 2.0f,
                        1, 2, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        AiBoardAnalyzer.LocationAnalysis remote =
                new AiBoardAnalyzer.LocationAnalysis(
                        remoteEndor, 0.0f, 0.0f, 0.0f, 0.0f,
                        1, 1, 0, 0,
                        AiBoardAnalyzer.ContestStatus.UNCONTESTED, true);
        PersistentResponsePolicy.Snapshot snapshot =
                new PersistentResponsePolicy.Snapshot(3, 2, Map.of(
                        LOCATION_ID,
                        new PersistentResponsePolicy.DrainHistory(
                                LOCATION_ID, "Carbonite Chamber",
                                2, 2, 2, 4)));

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoAnalyzer.isAnalyzed()).thenReturn(true);
        when(randoAnalyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(true);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, game);
        var randoLead = randoInstruction(
                String.valueOf(LOCATION_ID), 4, 2);
        randoLead.setCardPermanentCardId(10);
        randoLead.setCardCurrentCardId(1010);
        var randoBuddy = randoInstruction(
                String.valueOf(LOCATION_ID), 4, 2);
        randoBuddy.setCardPermanentCardId(11);
        randoBuddy.setCardCurrentCardId(1011);
        var randoUnrelated = randoInstruction("294", 1, 1);
        randoUnrelated.setCardPermanentCardId(13);
        randoUnrelated.setCardCurrentCardId(1013);
        var randoScored =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoPlan(randoLead, randoBuddy, randoUnrelated),
                        10.0f, "ground-response");
        var randoRemoteInstruction = randoInstruction("294", 6, 3);
        randoRemoteInstruction.setCardPermanentCardId(12);
        randoRemoteInstruction.setCardCurrentCardId(1012);
        var randoRemote =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoPlan(randoRemoteInstruction),
                        900.0f, "remote-eops-development");
        Object randoSelection = invokePersistentSelection(
                randoPlanner, List.of(randoRemote, randoScored),
                List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                        prelude)), List.of(target, remote), snapshot);
        assertNotNull(randoSelection);
        when(gameState.getHand("player")).thenReturn(
                List.of(lead, remoteDeveloper));
        assertNull(invokePersistentSelection(
                randoPlanner, List.of(randoRemote, randoScored),
                List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                        prelude)), List.of(target, remote), snapshot));
        when(gameState.getHand("player")).thenReturn(
                List.of(lead, buddy, remoteDeveloper));
        var randoFinal =
                (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeSelectBestPlanWithSelection(randoPlanner,
                        new ArrayList<>(List.of(randoRemote, randoScored)),
                        List.of(new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                prelude)), randoSelection);

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenAnalyzer.isAnalyzed()).thenReturn(true);
        when(chosenAnalyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(true);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, game);
        var chosenLead = chosenInstruction(
                String.valueOf(LOCATION_ID), 4, 2);
        chosenLead.setCardPermanentCardId(10);
        chosenLead.setCardCurrentCardId(1010);
        var chosenBuddy = chosenInstruction(
                String.valueOf(LOCATION_ID), 4, 2);
        chosenBuddy.setCardPermanentCardId(11);
        chosenBuddy.setCardCurrentCardId(1011);
        var chosenUnrelated = chosenInstruction("294", 1, 1);
        chosenUnrelated.setCardPermanentCardId(13);
        chosenUnrelated.setCardCurrentCardId(1013);
        var chosenScored =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                        chosenPlan(chosenBuddy, chosenUnrelated, chosenLead),
                        10.0f, "ground-response");
        var chosenRemoteInstruction = chosenInstruction("294", 6, 3);
        chosenRemoteInstruction.setCardPermanentCardId(12);
        chosenRemoteInstruction.setCardCurrentCardId(1012);
        var chosenRemote =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                        chosenPlan(chosenRemoteInstruction),
                        900.0f, "remote-eops-development");
        Object chosenSelection = invokePersistentSelection(
                chosenPlanner, List.of(chosenRemote, chosenScored),
                List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                        prelude)), List.of(target, remote), snapshot);
        assertNotNull(chosenSelection);
        var chosenFinal =
                (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeSelectBestPlanWithSelection(chosenPlanner,
                        new ArrayList<>(List.of(chosenRemote, chosenScored)),
                        List.of(new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                prelude)), chosenSelection);

        assertEquals(4, randoFinal.getInstructions().size());
        assertEquals(4, chosenFinal.getInstructions().size());
        assertEquals("Shared Prelude",
                randoFinal.getInstructions().get(0).getCardName());
        assertEquals("Shared Prelude",
                chosenFinal.getInstructions().get(0).getCardName());
        assertNotNull(randoFinal.getPersistentResponseObligation());
        assertNotNull(chosenFinal.getPersistentResponseObligation());
        assertEquals(10, randoFinal
                .getPersistentResponseObligation().responseAction()
                .permanentCardId());
        assertEquals(List.of(
                        new PersistentResponsePolicy.DeployActionKey(
                                10, 1010),
                        new PersistentResponsePolicy.DeployActionKey(
                                11, 1011)),
                randoFinal.getPersistentResponseObligation()
                        .responseActions());
        assertEquals(randoFinal.getPersistentResponseObligation(),
                chosenFinal.getPersistentResponseObligation());
    }

    @Test
    public void typedHardLossDefenseRequiresParticipantAndViableWave() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard targetCard = locationCard(
                "1_999", "Typed hard-loss site", LOCATION_ID,
                CardSubtype.SITE);
        PhysicalCard lead = character(
                "1_101", "Lead", 10, 4, 2, 2);
        PhysicalCard buddy = character(
                "1_102", "Buddy", 11, 4, 2, 2);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getCurrentPlayerId()).thenReturn("player");
        when(gameState.getCurrentPhase()).thenReturn(Phase.DEPLOY);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getHand("player")).thenReturn(List.of(lead, buddy));
        when(gameState.findCardById(LOCATION_ID)).thenReturn(targetCard);
        when(gameState.findCardById(1010)).thenReturn(lead);
        when(gameState.findCardById(1011)).thenReturn(buddy);
        when(gameState.getCardsAtLocation(targetCard)).thenReturn(List.of());
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(gameState, lead, lead, targetCard,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);
        when(modifiers.getDeployCost(gameState, buddy, buddy, targetCard,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);

        var analyzer = mock(
                com.gempukku.swccgo.ai.models.common.strategy
                        .ObjectiveAnalyzer.class);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isObjectiveHardLossDefenseLocation(
                game, "player", targetCard)).thenReturn(true);
        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(true);
        var plan = new PersistentResponsePlanAdapter.PlanView<>(
                "hard-loss-response", "ground_response", "reinforce",
                List.of(
                        new PersistentResponsePlanAdapter.InstructionView(
                                10, 1010, String.valueOf(LOCATION_ID), 1),
                        new PersistentResponsePlanAdapter.InstructionView(
                                11, 1011, String.valueOf(LOCATION_ID), 2)));
        AiBoardAnalyzer.LocationAnalysis viable =
                new AiBoardAnalyzer.LocationAnalysis(
                        targetCard, 0.0f, 4.0f, 0.0f, 2.0f,
                        1, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        var input = new PersistentResponsePlanAdapter.Input<>(
                game, "player", analyzer,
                PersistentResponsePolicy.Snapshot.empty(),
                List.of(viable), 10, 10, List.of(plan));

        var selected = PersistentResponsePlanAdapter.select(input)
                .orElseThrow();
        assertEquals(PersistentResponsePolicy.TargetRole
                        .OBJECTIVE_HARD_LOSS_DEFENSE,
                selected.obligation().role());
        assertEquals(0, selected.obligation().persistentBonus());
        assertEquals(250, selected.obligation().criticalBonus());

        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(false);
        assertTrue(PersistentResponsePlanAdapter.select(input).isEmpty());

        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(true);
        AiBoardAnalyzer.LocationAnalysis nonviable =
                new AiBoardAnalyzer.LocationAnalysis(
                        targetCard, 0.0f, 30.0f, 0.0f, 8.0f,
                        1, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        var nonviableInput = new PersistentResponsePlanAdapter.Input<>(
                game, "player", analyzer,
                PersistentResponsePolicy.Snapshot.empty(),
                List.of(nonviable), 10, 10, List.of(plan));
        assertTrue(PersistentResponsePlanAdapter.select(
                nonviableInput).isEmpty());

        var offered = List.of(
                new PersistentResponsePlanAdapter.OuterActionView(
                        "wrong", "Deploy Buddy", "1011",
                        true, false, false),
                new PersistentResponsePlanAdapter.OuterActionView(
                        "exact", "Deploy Lead", "1010",
                        true, false, false));
        var exactOuter = PersistentResponsePlanAdapter
                .findNextOfferedResponseAction(
                        game, "player", analyzer,
                        PersistentResponsePolicy.Snapshot.empty(),
                        selected.obligation(),
                        plan.instructions().get(0), 10, offered)
                .orElseThrow();
        assertEquals("exact", exactOuter.actionId());
        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(false);
        assertTrue(PersistentResponsePlanAdapter
                .findNextOfferedResponseAction(
                        game, "player", analyzer,
                        PersistentResponsePolicy.Snapshot.empty(),
                        selected.obligation(), plan.instructions().get(0),
                        10, offered).isEmpty());
        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", targetCard)).thenReturn(true);
        assertTrue(PersistentResponsePlanAdapter
                .findNextOfferedResponseAction(
                        game, "player", analyzer,
                        PersistentResponsePolicy.Snapshot.empty(),
                        selected.obligation(),
                        plan.instructions().get(0), 1, offered)
                .isEmpty());
        assertTrue(PersistentResponsePlanAdapter
                .findNextOfferedResponseAction(
                        game, "player", analyzer,
                        PersistentResponsePolicy.Snapshot.empty(),
                        selected.obligation(),
                        plan.instructions().get(0), 10,
                        List.of(offered.get(0))).isEmpty());
        assertTrue(PersistentResponsePlanAdapter
                .findNextOfferedResponseAction(
                        game, "player", analyzer,
                        PersistentResponsePolicy.Snapshot.empty(),
                        selected.obligation(),
                        plan.instructions().get(0), 10,
                        List.of(new PersistentResponsePlanAdapter
                                .OuterActionView(
                                "forged", "Deploy Lead", "1011",
                                true, false, false))).isEmpty());
    }

    @Test
    public void responseBankRevalidatesExactCardsTargetCostTurnThreatAndRoute() {
        ResponseBankFixture fixture = responseBankFixture();
        fixture.phase().set(Phase.DRAW);

        assertNotNull(fixture.obligation().responseBank());
        assertEquals(4, fixture.obligation().responseBank()
                .wholeResponseForceCost());
        assertNull(fixture.obligation().withRemainingResponseActions(List.of(
                new PersistentResponsePolicy.DeployActionKey(
                        11, 1011))).responseBank());
        assertTrue(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                PersistentResponsePolicy.Snapshot.empty(),
                fixture.obligation(), 3));

        fixture.hand().set(List.of(fixture.lead()));
        assertFalse(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                PersistentResponsePolicy.Snapshot.empty(),
                fixture.obligation(), 3));
        fixture.hand().set(List.of(fixture.lead(), fixture.buddy()));

        when(fixture.modifiers().getDeployCost(
                fixture.gameState(), fixture.buddy(), fixture.buddy(),
                fixture.target(), false, null, false, 0.0f, null, true))
                .thenReturn(3.0f);
        assertFalse(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                PersistentResponsePolicy.Snapshot.empty(),
                fixture.obligation(), 3));
        when(fixture.modifiers().getDeployCost(
                fixture.gameState(), fixture.buddy(), fixture.buddy(),
                fixture.target(), false, null, false, 0.0f, null, true))
                .thenReturn(2.0f);

        when(fixture.gameState().findCardByPermanentId(LOCATION_ID))
                .thenReturn(null);
        assertFalse(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                PersistentResponsePolicy.Snapshot.empty(),
                fixture.obligation(), 3));
        when(fixture.gameState().findCardByPermanentId(LOCATION_ID))
                .thenReturn(fixture.target());

        assertFalse(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                new PersistentResponsePolicy.Snapshot(1, 0, Map.of()),
                fixture.obligation(), 3));
        assertFalse(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                PersistentResponsePolicy.Snapshot.empty(),
                fixture.obligation(), 4));

        PersistentResponsePolicy.ResponseBankDetails bank =
                fixture.obligation().responseBank();
        PersistentResponsePolicy.Obligation wrongRoute = fixture.obligation()
                .withResponseBank(new PersistentResponsePolicy
                        .ResponseBankDetails(
                        bank.selectionTurn(), bank.threatRevision(),
                        bank.wholeResponseForceCost(),
                        DeployTacticalPolicy.ResponseFormationRoute.V172_SOLO,
                        bank.planDomain()));
        assertFalse(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.analyzer(),
                PersistentResponsePolicy.Snapshot.empty(), wrongRoute, 3));
    }

    @Test
    public void autoAdvanceRequiresExactCardInPlayAtExactTarget() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard member = mock(PhysicalCard.class);
        PhysicalCard target = locationCard(
                "1_999", "Planned target", LOCATION_ID,
                CardSubtype.SITE);
        PhysicalCard wrongTarget = locationCard(
                "1_998", "Wrong target", LOCATION_ID + 1,
                CardSubtype.SITE);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(10)).thenReturn(member);
        when(gameState.findCardById(LOCATION_ID)).thenReturn(target);
        when(member.getCardId()).thenReturn(1010);
        when(member.getZone()).thenReturn(Zone.AT_LOCATION);
        when(modifiers.getLocationThatCardIsAt(gameState, member))
                .thenReturn(target);

        assertTrue(PersistentResponsePlanAdapter
                .isExactCardInPlayAtPlannedTarget(
                        game, 10, 1010,
                        String.valueOf(LOCATION_ID), LOCATION_ID));
        when(modifiers.getLocationThatCardIsAt(gameState, member))
                .thenReturn(wrongTarget);
        assertFalse(PersistentResponsePlanAdapter
                .isExactCardInPlayAtPlannedTarget(
                        game, 10, 1010,
                        String.valueOf(LOCATION_ID), LOCATION_ID));
        when(modifiers.getLocationThatCardIsAt(gameState, member))
                .thenReturn(target);
        when(member.getZone()).thenReturn(Zone.LOST_PILE);
        assertFalse(PersistentResponsePlanAdapter
                .isExactCardInPlayAtPlannedTarget(
                        game, 10, 1010,
                        String.valueOf(LOCATION_ID), LOCATION_ID));
    }

    @Test
    public void fundedInvasionNeedWinsLowerCorridorAndMayRespondAtItself() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard corridor = locationCard(
                "5_82", "Lower Corridor", 82, CardSubtype.SITE);
        PhysicalCard naboo = locationCard(
                "14_100", "Naboo", 400, CardSubtype.SYSTEM);
        PhysicalCard corridorLead = character(
                "1_101", "Corridor Lead", 10, 4, 2, 2);
        PhysicalCard corridorBuddy = character(
                "1_102", "Corridor Buddy", 11, 4, 2, 2);
        PhysicalCard nabooActor = character(
                "14_80", "Neimoidian", 12, 4, 2, 2);
        PhysicalCard nabooBuddy = character(
                "14_81", "Naboo Buddy", 13, 4, 2, 2);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getCurrentPlayerId()).thenReturn("player");
        when(gameState.getCurrentPhase()).thenReturn(Phase.DEPLOY);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getHand("player")).thenReturn(List.of(
                corridorLead, corridorBuddy, nabooActor, nabooBuddy));
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        for (PhysicalCard card : List.of(
                corridorLead, corridorBuddy, nabooActor, nabooBuddy)) {
            when(modifiers.getDeployCost(
                    gameState, card, card, corridor,
                    false, null, false, 0.0f, null, true))
                    .thenReturn(2.0f);
            when(modifiers.getDeployCost(
                    gameState, card, card, naboo,
                    false, null, false, 0.0f, null, true))
                    .thenReturn(2.0f);
        }
        var analyzer = mock(
                com.gempukku.swccgo.ai.models.common.strategy
                        .ObjectiveAnalyzer.class);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", corridor)).thenReturn(true);
        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", naboo)).thenReturn(false);
        when(analyzer.isMissingPreFlipRequirementAt(
                game, "player", naboo)).thenReturn(true);
        when(analyzer.wouldCompletePreFlipRequirementAt(
                game, "player", nabooActor, naboo)).thenReturn(true);

        AiBoardAnalyzer.LocationAnalysis corridorFacts =
                new AiBoardAnalyzer.LocationAnalysis(
                        corridor, 0.0f, 4.0f, 0.0f, 2.0f,
                        2, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        AiBoardAnalyzer.LocationAnalysis nabooOpen =
                new AiBoardAnalyzer.LocationAnalysis(
                        naboo, 0.0f, 0.0f, 0.0f, 0.0f,
                        1, 1, 0, 0,
                        AiBoardAnalyzer.ContestStatus.UNCONTESTED, true);
        var corridorPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "corridor-response", "ground_response", "attack",
                List.of(
                        new PersistentResponsePlanAdapter.InstructionView(
                                10, 1010, "82", 1),
                        new PersistentResponsePlanAdapter.InstructionView(
                                11, 1011, "82", 2)));
        var nabooPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "invasion-naboo", "objective", "flip",
                List.of(new PersistentResponsePlanAdapter.InstructionView(
                        12, 1012, "400", 1)));
        PersistentResponsePolicy.Snapshot corridorThreat =
                new PersistentResponsePolicy.Snapshot(2, 2, Map.of(
                        82, new PersistentResponsePolicy.DrainHistory(
                                82, "Lower Corridor", 2, 2, 1, 2)));

        var mandatoryWinner = PersistentResponsePlanAdapter.select(
                new PersistentResponsePlanAdapter.Input<>(
                        game, "player", analyzer, corridorThreat,
                        List.of(corridorFacts, nabooOpen),
                        10, 10, List.of(corridorPlan, nabooPlan)))
                .orElseThrow();
        assertEquals("invasion-naboo", mandatoryWinner.source());
        assertEquals(PersistentResponsePolicy.CandidateKind.EXISTING_PLAN,
                mandatoryWinner.obligation().kind());
        assertEquals("funded-mandatory-objective",
                mandatoryWinner.obligation().reasonCode());
        assertNull(mandatoryWinner.obligation().responseBank());

        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", naboo)).thenReturn(true);
        when(analyzer.wouldCompletePreFlipRequirementAt(
                game, "player", nabooBuddy, naboo)).thenReturn(false);
        AiBoardAnalyzer.LocationAnalysis nabooOccupied =
                new AiBoardAnalyzer.LocationAnalysis(
                        naboo, 0.0f, 4.0f, 0.0f, 2.0f,
                        1, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        var nabooResponsePlan =
                new PersistentResponsePlanAdapter.PlanView<>(
                        "invasion-naboo-response", "objective", "flip",
                        List.of(
                                new PersistentResponsePlanAdapter
                                        .InstructionView(
                                        12, 1012, "400", 1),
                                new PersistentResponsePlanAdapter
                                        .InstructionView(
                                        13, 1013, "400", 2)));
        var sameTargetWinner = PersistentResponsePlanAdapter.select(
                new PersistentResponsePlanAdapter.Input<>(
                        game, "player", analyzer, corridorThreat,
                        List.of(nabooOccupied), 10, 10,
                        List.of(nabooResponsePlan))).orElseThrow();
        assertEquals(PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET,
                sameTargetWinner.obligation().kind());
        assertEquals(PersistentResponsePolicy.TargetRole
                        .MISSING_REQUIRED_LOCATION,
                sameTargetWinner.obligation().role());
        assertEquals(250, sameTargetWinner.obligation().criticalBonus());
        assertNull(sameTargetWinner.obligation().responseBank());
    }

    @Test
    public void sharedAdapterMapsV170FormationToSpyMode() {
        AiBoardAnalyzer.LocationAnalysis target =
                location("Persistent lane", 3.0f, 0, 1);
        PersistentResponsePolicy.FormationProof spyFormation =
                new PersistentResponsePolicy.FormationProof(
                        0.0f, 0.0f, 1.0f, 1.0f,
                        DeployTacticalPolicy.ResponseFormationRoute.V170_SPY,
                        false);

        assertEquals(PersistentResponsePolicy.Mode.SPY,
                PersistentResponsePlanAdapter.responseMode(
                        target, spyFormation));
    }

    @Test
    public void sharedAdapterUsesPrintedGroundFactsAndRejectsUnsupportedCards() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard site = locationCard(
                "1_999", "Public-fact site", LOCATION_ID,
                CardSubtype.SITE);
        PhysicalCard lead = character(
                "1_101", "Printed Lead", 101, 4, 2, 2);
        PhysicalCard buddy = character(
                "1_102", "Printed Buddy", 102, 3, 2, 2);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getHand("player")).thenReturn(
                List.of(lead, buddy));
        when(gameState.getCardsAtLocation(site)).thenReturn(List.of());
        when(modifiers.getAbility(gameState, lead, true))
                .thenReturn(99.0f);
        when(modifiers.getAbility(gameState, buddy, true))
                .thenReturn(99.0f);
        AiBoardAnalyzer.LocationAnalysis target =
                new AiBoardAnalyzer.LocationAnalysis(
                        site, 0.0f, 6.0f, 0.0f, 2.0f,
                        1, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        var leadInstruction = new PersistentResponsePlanAdapter
                .InstructionView(101, 1101,
                String.valueOf(LOCATION_ID), 1);
        var buddyInstruction = new PersistentResponsePlanAdapter
                .InstructionView(102, 1102,
                String.valueOf(LOCATION_ID), 2);
        var plan = new PersistentResponsePlanAdapter.PlanView<>(
                "ground", "ground_attack", "TEST",
                List.of(leadInstruction, buddyInstruction));

        PersistentResponsePolicy.FormationProof printed =
                PersistentResponsePlanAdapter.assessFormation(
                        game, "player", target, plan,
                        plan.instructions(), List.of(lead, buddy), true);

        assertEquals(7.0f, printed.plannedWavePower(), 0.0f);
        assertEquals(4.0f, printed.plannedWaveAbility(), 0.0f);
        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                printed.route());

        PhysicalCard effect = mock(PhysicalCard.class);
        SwccgCardBlueprint effectBlueprint = mock(SwccgCardBlueprint.class);
        when(effect.getBlueprint()).thenReturn(effectBlueprint);
        when(effect.getPermanentCardId()).thenReturn(103);
        when(effect.getCardId()).thenReturn(1103);
        when(effectBlueprint.getCardCategory()).thenReturn(
                CardCategory.EFFECT);
        when(effectBlueprint.hasPowerAttribute()).thenReturn(true);
        when(effectBlueprint.getPower()).thenReturn(50.0f);
        when(effectBlueprint.hasAbilityAttribute()).thenReturn(true);
        when(effectBlueprint.getAbility()).thenReturn(50.0f);
        when(gameState.getHand("player")).thenReturn(List.of(effect));
        var effectInstruction = new PersistentResponsePlanAdapter
                .InstructionView(103, 1103,
                String.valueOf(LOCATION_ID), 1);
        var unsupportedPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "effect", "ground_attack", "TEST",
                List.of(effectInstruction));

        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.NONE,
                PersistentResponsePlanAdapter.assessFormation(
                        game, "player", target, unsupportedPlan,
                        unsupportedPlan.instructions(), List.of(effect),
                        true).route());
    }

    @Test
    public void sharedAdapterAdmitsOnlyPrintedCrewSafeV296SpaceResponse() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard system = locationCard(
                "1_999", "Kessel", LOCATION_ID,
                CardSubtype.SYSTEM);
        PhysicalCard ship = starship(
                "1_998", "Response Ship", 901, 10, 4, 5);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getHand("player")).thenReturn(List.of(ship));
        when(gameState.getCardsAtLocation(system)).thenReturn(List.of());
        when(modifiers.getForceDrainAmount(
                gameState, system, "opponent")).thenReturn(2.0f);
        when(modifiers.getAbility(gameState, ship, true))
                .thenReturn(4.0f);
        when(modifiers.hasPermanentPilot(gameState, ship))
                .thenReturn(true);
        AiBoardAnalyzer.LocationAnalysis target =
                new AiBoardAnalyzer.LocationAnalysis(
                        system, 0.0f, 10.0f, 0.0f, 4.0f,
                        1, 2, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);

        var shipInstruction = new PersistentResponsePlanAdapter
                .InstructionView(901, 1901,
                String.valueOf(LOCATION_ID), 1);
        var shipPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "ship", "space_bleed", "TEST",
                List.of(shipInstruction));
        PersistentResponsePolicy.FormationProof admitted =
                PersistentResponsePlanAdapter.assessFormation(
                        game, "player", target, shipPlan,
                        shipPlan.instructions(), List.of(ship), true);

        assertEquals(DeployTacticalPolicy.ResponseFormationRoute
                .V296_SPACE_CONTACT, admitted.route());
        assertTrue(admitted.responseViable());
        assertEquals(10.0f, admitted.projectedFriendlyPower(), 0.0f);
        assertEquals(4.0f, admitted.projectedFriendlyAbility(), 0.0f);

        PhysicalCard unpiloted = starship(
                "1_997", "Unpiloted Ship", 902, 12, 0, 4);
        when(gameState.getHand("player")).thenReturn(List.of(unpiloted));
        when(modifiers.getAbility(gameState, unpiloted, true))
                .thenReturn(99.0f);
        var unpilotedInstruction = new PersistentResponsePlanAdapter
                .InstructionView(902, 1902,
                String.valueOf(LOCATION_ID), 1);
        var unpilotedPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "unpiloted", "space_bleed", "TEST",
                List.of(unpilotedInstruction));
        PersistentResponsePolicy.FormationProof rejectedUnpiloted =
                PersistentResponsePlanAdapter.assessFormation(
                        game, "player", target, unpilotedPlan,
                        unpilotedPlan.instructions(), List.of(unpiloted),
                        true);
        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.NONE,
                rejectedUnpiloted.route());

        PhysicalCard pilot = character(
                "1_996", "Pilot In Hand", 903, 5, 2, 2);
        PhysicalCard losingUnpiloted = starship(
                "1_995", "Losing Unpiloted Ship", 904, 7, 0, 3);
        when(gameState.getHand("player"))
                .thenReturn(List.of(losingUnpiloted, pilot));
        var losingShipInstruction = new PersistentResponsePlanAdapter
                .InstructionView(904, 1904,
                String.valueOf(LOCATION_ID), 1);
        var pilotInstruction = new PersistentResponsePlanAdapter
                .InstructionView(903, 1903,
                String.valueOf(LOCATION_ID), 2);
        var mixedPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "mixed", "space_bleed", "TEST",
                List.of(losingShipInstruction, pilotInstruction));
        PersistentResponsePolicy.FormationProof rejectedMixed =
                PersistentResponsePlanAdapter.assessFormation(
                        game, "player", target, mixedPlan,
                        mixedPlan.instructions(),
                        List.of(losingUnpiloted, pilot), true);
        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.NONE,
                rejectedMixed.route());

        PhysicalCard firstShip = starship(
                "1_994", "First Losing Ship", 905, 6, 2, 3);
        PhysicalCard secondShip = starship(
                "1_993", "Second Losing Ship", 906, 6, 2, 3);
        when(gameState.getHand("player"))
                .thenReturn(List.of(firstShip, secondShip));
        when(modifiers.hasPermanentPilot(gameState, firstShip))
                .thenReturn(true);
        when(modifiers.hasPermanentPilot(gameState, secondShip))
                .thenReturn(true);
        when(modifiers.getAbility(gameState, firstShip, true))
                .thenReturn(2.0f);
        when(modifiers.getAbility(gameState, secondShip, true))
                .thenReturn(2.0f);
        var firstShipInstruction = new PersistentResponsePlanAdapter
                .InstructionView(905, 1905,
                String.valueOf(LOCATION_ID), 1);
        var secondShipInstruction = new PersistentResponsePlanAdapter
                .InstructionView(906, 1906,
                String.valueOf(LOCATION_ID), 2);
        var multiShipPlan = new PersistentResponsePlanAdapter.PlanView<>(
                "multi", "space_bleed", "TEST",
                List.of(firstShipInstruction, secondShipInstruction));
        PersistentResponsePolicy.FormationProof rejectedShipSum =
                PersistentResponsePlanAdapter.assessFormation(
                        game, "player", target, multiShipPlan,
                        multiShipPlan.instructions(),
                        List.of(firstShip, secondShip), true);
        assertEquals(DeployTacticalPolicy.ResponseFormationRoute.NONE,
                rejectedShipSum.route());
    }

    @Test
    public void bothPlannersSelectOnlyAnExactOperationalSpaceBleedShip()
            throws Exception {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard system = locationCard(
                "1_999", "Kessel", LOCATION_ID, CardSubtype.SYSTEM);
        PhysicalCard ship = starship(
                "1_998", "Exact Response Ship", 901, 10, 4, 5);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getCurrentPlayerId()).thenReturn("player");
        when(gameState.getCurrentPhase()).thenReturn(Phase.DEPLOY);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getHand("player")).thenReturn(List.of(ship));
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(gameState, ship, ship, system,
                false, null, false, 0.0f, null, true))
                .thenReturn(5.0f);
        when(modifiers.getForceDrainAmount(
                gameState, system, "opponent")).thenReturn(2.0f);
        when(modifiers.hasPermanentPilot(gameState, ship))
                .thenReturn(true);
        when(modifiers.getAbility(gameState, ship, true))
                .thenReturn(4.0f);
        AiBoardAnalyzer.LocationAnalysis target =
                new AiBoardAnalyzer.LocationAnalysis(
                        system, 0.0f, 10.0f, 0.0f, 4.0f,
                        1, 2, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        PersistentResponsePolicy.Snapshot snapshot =
                new PersistentResponsePolicy.Snapshot(3, 2, Map.of(
                        LOCATION_ID,
                        new PersistentResponsePolicy.DrainHistory(
                                LOCATION_ID, "Kessel", 2, 2, 2, 4)));

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoAnalyzer.isAnalyzed()).thenReturn(true);
        when(randoAnalyzer.hasOpponentBattleParticipantAt(
                game, "player", system)).thenReturn(true);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, game);
        var randoInstruction = randoInstruction(
                String.valueOf(LOCATION_ID), 10, 4);
        randoInstruction.setCardPermanentCardId(901);
        randoInstruction.setCardCurrentCardId(1901);
        var randoScored =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoPlan(randoInstruction), 10.0f,
                        "space_bleed");
        Object randoSelection = invokePersistentSelection(
                randoPlanner, List.of(randoScored), List.of(),
                List.of(target), snapshot);

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenAnalyzer.isAnalyzed()).thenReturn(true);
        when(chosenAnalyzer.hasOpponentBattleParticipantAt(
                game, "player", system)).thenReturn(true);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, game);
        var chosenInstruction = chosenInstruction(
                String.valueOf(LOCATION_ID), 10, 4);
        chosenInstruction.setCardPermanentCardId(901);
        chosenInstruction.setCardCurrentCardId(1901);
        var chosenScored =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ScoredPlan(
                        chosenPlan(chosenInstruction), 10.0f,
                        "space_bleed");
        Object chosenSelection = invokePersistentSelection(
                chosenPlanner, List.of(chosenScored), List.of(),
                List.of(target), snapshot);

        assertNotNull(randoSelection);
        assertNotNull(chosenSelection);
        var randoFinal =
                (com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan)
                invokeSelectBestPlanWithSelection(randoPlanner,
                        new ArrayList<>(List.of(randoScored)),
                        List.of(), randoSelection);
        var chosenFinal =
                (com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan)
                invokeSelectBestPlanWithSelection(chosenPlanner,
                        new ArrayList<>(List.of(chosenScored)),
                        List.of(), chosenSelection);
        assertEquals(901, randoFinal.getPersistentResponseObligation()
                .responseAction().permanentCardId());
        assertEquals(randoFinal.getPersistentResponseObligation(),
                chosenFinal.getPersistentResponseObligation());

        PhysicalCard unpilotedShip = starship(
                "1_997", "Unpiloted Ship", 902, 7, 0, 3);
        PhysicalCard pilot = character(
                "1_996", "Pilot Power Does Not Count", 903, 5, 3, 2);
        when(gameState.getHand("player"))
                .thenReturn(List.of(unpilotedShip, pilot));
        when(modifiers.getDeployCost(gameState, unpilotedShip,
                unpilotedShip, system, false, null, false,
                0.0f, null, true)).thenReturn(3.0f);
        when(modifiers.getDeployCost(gameState, pilot, pilot, system,
                false, null, false, 0.0f, null, true))
                .thenReturn(2.0f);
        var rejectedShip = randoInstruction(
                String.valueOf(LOCATION_ID), 7, 0);
        rejectedShip.setCardPermanentCardId(902);
        rejectedShip.setCardCurrentCardId(1902);
        var rejectedPilot = randoInstruction(
                String.valueOf(LOCATION_ID), 5, 3);
        rejectedPilot.setCardPermanentCardId(903);
        rejectedPilot.setCardCurrentCardId(1903);
        var guessedCrew =
                new com.gempukku.swccgo.ai.models.rando.strategy.ScoredPlan(
                        randoPlan(rejectedShip, rejectedPilot), 10.0f,
                        "space_bleed");
        assertNull(invokePersistentSelection(
                randoPlanner, List.of(guessedCrew), List.of(),
                List.of(target), snapshot));
    }

    @Test
    public void bothBotsSpendTheBattleReserveToCompleteTheExactFlipFormation() {
        PlanRefreshFixture fixture = planRefreshFixture();
        fixture.locations().set(List.of(fixture.swamp(), fixture.throneRoom()));
        GameState gameState = fixture.game().getGameState();
        PhysicalCard existingBuddy = character(
                "208_35", "Lord Sidious", 130, 5, 7, 6);
        when(existingBuddy.getOwner()).thenReturn("player");
        when(gameState.getForcePileSize("player")).thenReturn(3);
        when(gameState.getHand("player")).thenReturn(List.of(fixture.actor()));
        when(gameState.getCardsAtLocation(fixture.throneRoom()))
                .thenReturn(List.of(existingBuddy));

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var randoPlan = randoPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureRefreshAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        var chosenPlan = chosenPlanner.createPlan(
                fixture.game(), "player", Side.DARK);

        assertTrue(randoPlan.getReason().startsWith(
                "V297 objective flip-gate formation"));
        assertTrue(chosenPlan.getReason().startsWith(
                "V297 objective flip-gate formation"));
        assertEquals(1, randoPlan.getInstructions().size());
        assertEquals(1, chosenPlan.getInstructions().size());
        assertEquals("Neimoidian",
                randoPlan.getInstructions().get(0).getCardName());
        assertEquals("Neimoidian",
                chosenPlan.getInstructions().get(0).getCardName());
        assertEquals(3,
                randoPlan.getInstructions().get(0).getDeployCost());
        assertEquals(3,
                chosenPlan.getInstructions().get(0).getDeployCost());
    }

    @Test
    public void bothBotsRejectTheReplayUnderpoweredThroneRoomRecovery()
            throws Exception {
        FormationFixture base = formationFixture();
        PhysicalCard dofine = character(
                "14_76", "Captain Daultay Dofine", 105, 3, 3, 3);
        PhysicalCard infantry = character(
                "14_80", "Infantry Battle Droid", 106, 2, 0, 3);
        AiBoardAnalyzer.LocationAnalysis occupiedGate =
                new AiBoardAnalyzer.LocationAnalysis(
                        base.location(), 0.0f, 9.0f, 0.0f, 6.0f,
                        2, 1, 0, 2,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        FormationFixture fixture = new FormationFixture(
                base.game(), base.location(), dofine, infantry,
                List.of(occupiedGate));

        var randoPlanner = newRandoPlanner();
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(randoAnalyzer, fixture);
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        configurePlannerState(randoPlanner, fixture.game());

        var chosenPlanner = newChosenPlanner();
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        configureFormationAnalyzer(chosenAnalyzer, fixture);
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        configurePlannerState(chosenPlanner, fixture.game());

        assertNull(invokeFormationPlan(randoPlanner,
                List.of(
                        new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                fixture.actor()),
                        new com.gempukku.swccgo.ai.models.rando.strategy.CardInfo(
                                fixture.buddy())), fixture.locations()));
        assertNull(invokeFormationPlan(chosenPlanner,
                List.of(
                        new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                fixture.actor()),
                        new com.gempukku.swccgo.ai.models.chosenone.strategy.CardInfo(
                                fixture.buddy())), fixture.locations()));
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
        when(analyzer.isObjectiveRelevantLocation(
                any(PhysicalCard.class),
                nullable(SwccgGame.class),
                nullable(String.class))).thenAnswer(invocation ->
                "Objective".equals(
                        ((PhysicalCard) invocation.getArgument(0)).getTitle()));
        when(analyzer.getLocationObjectiveBonus(
                any(PhysicalCard.class),
                nullable(SwccgGame.class),
                nullable(String.class))).thenAnswer(invocation ->
                "Objective".equals(
                        ((PhysicalCard) invocation.getArgument(0)).getTitle())
                        ? 150.0f : 0.0f);
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

    private static void configureRefreshAnalyzer(
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer analyzer,
            PlanRefreshFixture fixture) {
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.hasFlipGateActorRequirement()).thenReturn(true);
        when(analyzer.getFlipCriticalControlSite()).thenReturn(
                "Naboo: Theed Palace Throne Room");
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.isFlipGateLocation(
                fixture.game(), "player", fixture.swamp())).thenReturn(false);
        when(analyzer.isFlipGateLocation(
                fixture.game(), "player", fixture.throneRoom())).thenReturn(true);
        when(analyzer.hasFlipGateActorAtLocation(
                fixture.game(), "player", fixture.throneRoom())).thenReturn(false);
        when(analyzer.matchesFlipGateActorRequirement(
                fixture.game(), "player",
                fixture.actor(), fixture.throneRoom())).thenReturn(true);
        when(analyzer.matchesFlipGateActorRequirement(
                fixture.game(), "player", fixture.actor())).thenReturn(true);
        when(analyzer.isObjectiveRelevantLocation(
                "Naboo: Theed Palace Throne Room")).thenReturn(true);
        when(analyzer.getLocationObjectiveBonus(
                "Naboo: Theed Palace Throne Room")).thenReturn(150.0f);
        when(analyzer.getActivePlaybook()).thenReturn(flipGatePlaybook());
    }

    private static void configureRefreshAnalyzer(
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer analyzer,
            PlanRefreshFixture fixture) {
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.hasFlipGateActorRequirement()).thenReturn(true);
        when(analyzer.getFlipCriticalControlSite()).thenReturn(
                "Naboo: Theed Palace Throne Room");
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.isFlipGateLocation(
                fixture.game(), "player", fixture.swamp())).thenReturn(false);
        when(analyzer.isFlipGateLocation(
                fixture.game(), "player", fixture.throneRoom())).thenReturn(true);
        when(analyzer.hasFlipGateActorAtLocation(
                fixture.game(), "player", fixture.throneRoom())).thenReturn(false);
        when(analyzer.matchesFlipGateActorRequirement(
                fixture.game(), "player",
                fixture.actor(), fixture.throneRoom())).thenReturn(true);
        when(analyzer.matchesFlipGateActorRequirement(
                fixture.game(), "player", fixture.actor())).thenReturn(true);
        when(analyzer.isObjectiveRelevantLocation(
                "Naboo: Theed Palace Throne Room")).thenReturn(true);
        when(analyzer.getLocationObjectiveBonus(
                "Naboo: Theed Palace Throne Room")).thenReturn(150.0f);
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
        return invokeFormationPlan(planner, characters, locations, 8);
    }

    private static Object invokeFormationPlan(
            Object planner, List<?> characters,
            List<AiBoardAnalyzer.LocationAnalysis> locations,
            int forceAvailable) throws Exception {
        Method method = planner.getClass().getDeclaredMethod(
                "generateFlipGateFormationPlan", List.class, List.class, int.class);
        method.setAccessible(true);
        return method.invoke(planner, characters, locations, forceAvailable);
    }

    private static Object invokeSelectBestPlan(
            Object planner, List<?> allPlans, List<?> locationDeploys,
            Object selectedScoredPlan,
            PersistentResponsePolicy.Obligation obligation)
            throws Exception {
        Class<?> selectionType = null;
        for (Class<?> nested : planner.getClass().getDeclaredClasses()) {
            if ("PersistentPlanSelection".equals(nested.getSimpleName())) {
                selectionType = nested;
                break;
            }
        }
        assertNotNull(selectionType);
        var constructor = selectionType.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object selection = constructor.newInstance(
                selectedScoredPlan, obligation);
        Method method = planner.getClass().getDeclaredMethod(
                "selectBestPlan", List.class, List.class,
                int.class, int.class, selectionType);
        method.setAccessible(true);
        return method.invoke(planner, allPlans, locationDeploys,
                4, 20, selection);
    }

    private static Object invokePersistentSelection(
            Object planner, List<?> allPlans, List<?> locationDeploys,
            List<AiBoardAnalyzer.LocationAnalysis> locations,
            PersistentResponsePolicy.Snapshot snapshot)
            throws Exception {
        Method method = planner.getClass().getDeclaredMethod(
                "selectPersistentResponsePlan", List.class, List.class,
                int.class, int.class,
                PersistentResponsePolicy.Snapshot.class);
        method.setAccessible(true);
        return method.invoke(planner, allPlans, locations,
                10, 12, snapshot);
    }

    private static boolean invokeEarlyHoldOverride(
            Object planner, Object scoredPlan,
            PersistentResponsePolicy.Obligation obligation)
            throws Exception {
        Class<?> selectionType = null;
        for (Class<?> nested : planner.getClass().getDeclaredClasses()) {
            if ("PersistentPlanSelection".equals(
                    nested.getSimpleName())) {
                selectionType = nested;
                break;
            }
        }
        assertNotNull(selectionType);
        var constructor = selectionType.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object selection = constructor.newInstance(scoredPlan, obligation);
        Method method = planner.getClass().getDeclaredMethod(
                "persistentResponseOverridesEarlyHold", selectionType);
        method.setAccessible(true);
        return (Boolean) method.invoke(planner, selection);
    }

    private static Object invokeSelectBestPlanWithSelection(
            Object planner, List<?> allPlans, List<?> locationDeploys,
            Object selection) throws Exception {
        Method method = planner.getClass().getDeclaredMethod(
                "selectBestPlan", List.class, List.class,
                int.class, int.class, selection.getClass());
        method.setAccessible(true);
        return method.invoke(planner, allPlans, locationDeploys,
                4, 20, selection);
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

    private static PlanRefreshFixture planRefreshFixture() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard swamp = locationCard(
                "12_171", "Naboo: Swamp", 178, CardSubtype.SITE);
        PhysicalCard throneRoom = locationCard(
                "12_174", "Naboo: Theed Palace Throne Room",
                LOCATION_ID, CardSubtype.SITE);
        PhysicalCard actor = character(
                "14_76", "Neimoidian", 101, 6, 2, 3);
        PhysicalCard buddy = character(
                "12_118", "Buddy", 102, 5, 2, 3);
        AtomicReference<List<PhysicalCard>> locations =
                new AtomicReference<>(List.of(swamp));

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("player")).thenReturn("opponent");
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getPlayersLatestTurnNumber("player")).thenReturn(2);
        when(gameState.getForcePileSize("player")).thenReturn(8);
        when(gameState.getPlayerLifeForce("player")).thenReturn(30);
        when(gameState.getPlayerLifeForce("opponent")).thenReturn(30);
        when(gameState.getHand("player")).thenReturn(List.of(actor, buddy));
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenAnswer(
                ignored -> locations.get());
        when(gameState.getCardsAtLocation(any(PhysicalCard.class)))
                .thenReturn(List.of());
        doAnswer(invocation -> {
            PhysicalCardVisitor visitor = invocation.getArgument(0);
            for (PhysicalCard location : locations.get()) {
                if (visitor.visitPhysicalCard(location)) return true;
            }
            return false;
        }).when(gameState).iterateLocationsOnTable(
                any(PhysicalCardVisitor.class), anyBoolean());
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "player", swamp)).thenReturn(0.0f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "opponent", swamp)).thenReturn(0.0f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "player", throneRoom)).thenReturn(0.0f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "opponent", throneRoom)).thenReturn(0.0f);
        when(modifiers.getIconCount(
                gameState, swamp, Icon.DARK_FORCE)).thenReturn(1);
        when(modifiers.getIconCount(
                gameState, swamp, Icon.LIGHT_FORCE)).thenReturn(1);
        when(modifiers.getIconCount(
                gameState, throneRoom, Icon.DARK_FORCE)).thenReturn(1);
        when(modifiers.getIconCount(
                gameState, throneRoom, Icon.LIGHT_FORCE)).thenReturn(1);
        when(modifiers.isBattleground(
                gameState, swamp, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, throneRoom, null)).thenReturn(true);

        return new PlanRefreshFixture(
                game, locations, swamp, throneRoom, actor);
    }

    private static PhysicalCard locationCard(
            String blueprintId, String title, int cardId,
            CardSubtype subtype) {
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(location.getBlueprint()).thenReturn(blueprint);
        when(location.getBlueprintId(true)).thenReturn(blueprintId);
        when(location.getCardId()).thenReturn(cardId);
        when(location.getPermanentCardId()).thenReturn(cardId);
        when(location.getTitle()).thenReturn(title);
        when(location.getTitles()).thenReturn(List.of(title));
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return location;
    }

    private static ResponseBankFixture responseBankFixture() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard target = locationCard(
                "1_999", "Current hard-loss site", LOCATION_ID,
                CardSubtype.SITE);
        PhysicalCard lead = character(
                "1_101", "Lead", 10, 4, 2, 2);
        PhysicalCard buddy = character(
                "1_102", "Buddy", 11, 4, 2, 2);
        AtomicReference<Phase> phase = new AtomicReference<>(Phase.DEPLOY);
        AtomicReference<List<PhysicalCard>> hand = new AtomicReference<>(
                List.of(lead, buddy));
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getCurrentPlayerId()).thenReturn("player");
        when(gameState.getCurrentPhase()).thenAnswer(
                ignored -> phase.get());
        when(gameState.getPlayersLatestTurnNumber("player")).thenReturn(3);
        when(gameState.getOpponent("player")).thenReturn("opponent");
        when(gameState.getSide("player")).thenReturn(Side.DARK);
        when(gameState.getForcePileSize("player")).thenReturn(4);
        when(gameState.getHand("player")).thenAnswer(
                ignored -> hand.get());
        when(gameState.findCardByPermanentId(LOCATION_ID))
                .thenReturn(target);
        when(gameState.getCardsAtLocation(target)).thenReturn(List.of());
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(gameState, lead, lead, target,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);
        when(modifiers.getDeployCost(gameState, buddy, buddy, target,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);

        var analyzer = mock(
                com.gempukku.swccgo.ai.models.common.strategy
                        .ObjectiveAnalyzer.class);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isObjectiveHardLossDefenseLocation(
                game, "player", target)).thenReturn(true);
        when(analyzer.hasOpponentBattleParticipantAt(
                game, "player", target)).thenReturn(true);
        var plan = new PersistentResponsePlanAdapter.PlanView<>(
                "hard-loss-response", "ground_response", "reinforce",
                List.of(
                        new PersistentResponsePlanAdapter.InstructionView(
                                10, 1010, String.valueOf(LOCATION_ID), 1),
                        new PersistentResponsePlanAdapter.InstructionView(
                                11, 1011, String.valueOf(LOCATION_ID), 2)));
        AiBoardAnalyzer.LocationAnalysis viable =
                new AiBoardAnalyzer.LocationAnalysis(
                        target, 0.0f, 4.0f, 0.0f, 2.0f,
                        1, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        PersistentResponsePolicy.Obligation obligation =
                PersistentResponsePlanAdapter.select(
                        new PersistentResponsePlanAdapter.Input<>(
                                game, "player", analyzer,
                                PersistentResponsePolicy.Snapshot.empty(),
                                List.of(viable), 10, 10, List.of(plan)))
                        .orElseThrow().obligation();
        return new ResponseBankFixture(
                gameState, game, modifiers, analyzer, target, lead, buddy,
                phase, hand, obligation);
    }

    private record ResponseBankFixture(
            GameState gameState,
            SwccgGame game,
            ModifiersQuerying modifiers,
            com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer
                    analyzer,
            PhysicalCard target,
            PhysicalCard lead,
            PhysicalCard buddy,
            AtomicReference<Phase> phase,
            AtomicReference<List<PhysicalCard>> hand,
            PersistentResponsePolicy.Obligation obligation) {
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

    private static PhysicalCard starship(
            String blueprintId, String title, int permanentId,
            int power, int ability, int cost) {
        PhysicalCard card = character(
                blueprintId, title, permanentId, power, ability, cost);
        when(card.getBlueprint().getCardCategory()).thenReturn(
                CardCategory.STARSHIP);
        return card;
    }

    private static void configureObjective(
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner planner) {
        var analyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isObjectiveRelevantLocation("Objective")).thenReturn(true);
        when(analyzer.getLocationObjectiveBonus("Objective")).thenReturn(150.0f);
        when(analyzer.isObjectiveRelevantLocation(
                any(PhysicalCard.class),
                nullable(SwccgGame.class),
                nullable(String.class))).thenAnswer(invocation ->
                "Objective".equals(
                        ((PhysicalCard) invocation.getArgument(0)).getTitle()));
        when(analyzer.getLocationObjectiveBonus(
                any(PhysicalCard.class),
                nullable(SwccgGame.class),
                nullable(String.class))).thenAnswer(invocation ->
                "Objective".equals(
                        ((PhysicalCard) invocation.getArgument(0)).getTitle())
                        ? 150.0f : 0.0f);
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

    private static PersistentResponsePolicy.Obligation responseObligation(
            int locationId, int persistentBonus, int criticalBonus,
            PersistentResponsePolicy.Mode mode) {
        PersistentResponsePolicy.LocationKey location =
                new PersistentResponsePolicy.LocationKey(
                        locationId, "Persistent lane");
        return new PersistentResponsePolicy.Obligation(
                new PersistentResponsePolicy.CandidateKey("test-response"),
                PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET,
                List.of(new PersistentResponsePolicy.DeployActionKey(
                        10, 20)),
                location, location,
                PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE,
                mode, persistentBonus, criticalBonus,
                "selected-executable-response");
    }

    private static PersistentResponsePolicy.Obligation
    alternativeObligation() {
        PersistentResponsePolicy.LocationKey threat =
                new PersistentResponsePolicy.LocationKey(
                        LOCATION_ID, "Persistent lane");
        PersistentResponsePolicy.LocationKey alternative =
                new PersistentResponsePolicy.LocationKey(
                        LOCATION_ID + 1, "Race lane");
        return new PersistentResponsePolicy.Obligation(
                new PersistentResponsePolicy.CandidateKey(
                        "test-alternative"),
                PersistentResponsePolicy.CandidateKind.EXISTING_PLAN,
                List.of(), threat, alternative,
                PersistentResponsePolicy.TargetRole.NONE,
                PersistentResponsePolicy.Mode.RACE,
                0, 0, "executable-alternative-preserved");
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

    private record PlanRefreshFixture(
            SwccgGame game,
            AtomicReference<List<PhysicalCard>> locations,
            PhysicalCard swamp,
            PhysicalCard throneRoom,
            PhysicalCard actor) {
    }
}
