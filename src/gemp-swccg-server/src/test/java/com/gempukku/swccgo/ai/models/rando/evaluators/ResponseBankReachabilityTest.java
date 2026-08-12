package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionEnvelopeFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionEnvelopePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployBudgetPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePlanAdapter;
import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePolicy;
import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.rando.strategy.StrategyController;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseBankReachabilityTest {
    private static final int TARGET_ID = 293;

    @Test
    public void allBadResponseSurvivesPreludeDeploymentAndBanksAtDraw() {
        Fixture fixture = fixture();
        PersistentResponsePolicy.Obligation obligation =
                selectResponse(fixture);
        DeploymentPlan plan = plan(obligation);

        float responseScore = additiveMaintenanceResponseScore(4);
        assertEquals(-1450.0f, responseScore, 0.0f);
        var firstBuckets = PersistentResponsePolicy.prependResponseBucket(
                obligation, List.of(offered("response", obligation)),
                List.of(new LinkedHashSet<>(Set.of("prelude"))),
                List.of("LOCATION_PRELUDE")).orElseThrow();
        EvaluatedAction firstWinner = choose(
                deployContext(fixture.state(), firstBuckets),
                List.of(
                        action("response", responseScore),
                        action("prelude", 50.0f)));
        assertEquals("prelude", firstWinner.getActionId());

        plan.recordDeployment(20, 1020, "bp-prelude");
        assertNotNull(plan.getPersistentResponseObligation());
        assertNotNull(plan.getPersistentResponseObligation().responseBank());
        assertEquals(2, plan.getPersistentResponseObligation()
                .responseActions().size());

        var secondBuckets = PersistentResponsePolicy.prependResponseBucket(
                plan.getPersistentResponseObligation(),
                List.of(offered("response",
                        plan.getPersistentResponseObligation())),
                List.of(new LinkedHashSet<>(Set.of("response"))),
                List.of("CHARACTER_DEPLOY")).orElseThrow();
        EvaluatedAction secondWinner = choose(
                deployContext(fixture.state(), secondBuckets),
                List.of(action("response", responseScore)));
        assertEquals(ActionType.PASS, secondWinner.getActionType());

        fixture.phase().set(Phase.DRAW);
        fixture.hand().set(List.of(
                fixture.lead(), fixture.buddy(), mock(PhysicalCard.class)));
        List<PhysicalCard> board = List.of(
                boardUnit("player"), boardUnit("player"),
                boardUnit("opponent"), boardUnit("opponent"),
                boardUnit("opponent"), boardUnit("opponent"));
        when(fixture.state().getAllPermanentCards()).thenReturn(board);
        assertTrue(PersistentResponsePlanAdapter.isCurrentResponseBank(
                fixture.game(), "player", fixture.objective(),
                PersistentResponsePolicy.Snapshot.empty(),
                plan.getPersistentResponseObligation(), 3));

        DeployPhasePlanner planner = mock(DeployPhasePlanner.class);
        when(planner.getCurrentPlan()).thenReturn(plan);
        StrategyController controller = mock(StrategyController.class);
        when(controller.getPersistentResponseSnapshot()).thenReturn(
                PersistentResponsePolicy.Snapshot.empty());
        DecisionContext draw = new DecisionContext(
                fixture.state(), "player", "CARD_ACTION_CHOICE",
                "Choose Draw action or Pass", "draw-reachable",
                Phase.DRAW);
        draw.setGame(fixture.game());
        draw.setSide(Side.DARK);
        draw.setObjectiveAnalyzer(fixture.objective());
        draw.setStrategyController(controller);
        draw.setDeployPhasePlanner(planner);
        draw.setActionIds(List.of("draw"));
        draw.setActionTexts(List.of(
                "Draw card into hand from Force Pile"));

        List<EvaluatedAction> draws = new DrawEvaluator().evaluate(draw);

        assertEquals(1, draws.size());
        assertEquals(-300.0f, draws.get(0).getScore(), 0.0f);
        assertTrue(draws.get(0).getReasoningString().contains(
                "V182 RESPONSE BANK"));

        fixture.phase().set(Phase.DEPLOY);
        fixture.turn().set(4);
        assertEquals(4, fixture.force().get());
        PersistentResponsePolicy.Obligation nextTurn =
                selectResponse(fixture);
        assertEquals(4, nextTurn.responseBank().wholeResponseForceCost());
        assertEquals(4, nextTurn.responseBank().selectionTurn());
        fixture.force().set(9);
        assertEquals(50.0f, additiveMaintenanceResponseScore(
                fixture.force().get()), 0.0f);
    }

    private static float additiveMaintenanceResponseScore(int totalForce) {
        float score = DeployActionEnvelopePolicy.evaluateParent(
                new DeployActionEnvelopeFacts.ParentAction(
                        "response", false, false)).initialScore();
        var maintenance = DeployBudgetPolicy.newMaintenanceCard(
                new DeployBudgetPolicy.NewMaintenanceFacts(
                        "response", "Maintenance Lead", true,
                        totalForce, 2, 1, 2, 2, 2));
        assertEquals(DeployBudgetPolicy.AdapterStep.FALL_THROUGH,
                maintenance.adapterStep());
        for (var operation : maintenance.result().operations()) {
            score += operation.delta();
        }
        return score;
    }

    private static PersistentResponsePolicy.OfferedOuterAction offered(
            String actionId,
            PersistentResponsePolicy.Obligation obligation) {
        return new PersistentResponsePolicy.OfferedOuterAction(
                actionId, obligation.responseAction(),
                true, true, true, true, true, false, false);
    }

    private static DecisionContext deployContext(
            GameState state,
            PersistentResponsePolicy.ResponseBucket buckets) {
        DecisionContext context = new DecisionContext(
                state, "player", "CARD_ACTION_CHOICE",
                "Choose deploy action", "deploy-reachable", Phase.DEPLOY);
        context.setNoPass(false);
        context.setMin(0);
        context.setStepBuckets(buckets.buckets());
        context.setStepBucketLabels(buckets.labels());
        return context;
    }

    private static EvaluatedAction choose(
            DecisionContext context, List<EvaluatedAction> actions) {
        ActionEvaluator scripted = new ScriptedEvaluator(
                ignored -> actions);
        return new CombinedEvaluator(
                List.of(scripted), NoOpTraceSink.INSTANCE)
                .evaluateDecision(context);
    }

    private static EvaluatedAction action(String id, float score) {
        return new EvaluatedAction(
                id, ActionType.DEPLOY, score, "Deploy " + id);
    }

    private static DeploymentPlan plan(
            PersistentResponsePolicy.Obligation obligation) {
        DeploymentPlan plan = new DeploymentPlan(
                DeployStrategy.REINFORCE, "reachable response");
        plan.setPersistentResponseObligation(obligation);
        plan.addInstruction(instruction(
                "bp-prelude", "Location Prelude", 20, 1020, null, 0, 0));
        plan.addInstruction(instruction(
                "bp-lead", "Maintenance Lead", 10, 1010,
                String.valueOf(TARGET_ID), 1, 2));
        plan.addInstruction(instruction(
                "bp-buddy", "Buddy", 11, 1011,
                String.valueOf(TARGET_ID), 2, 2));
        return plan;
    }

    private static DeploymentInstruction instruction(
            String blueprintId, String title,
            int permanentId, int currentId,
            String targetId, int priority, int deployCost) {
        DeploymentInstruction instruction = new DeploymentInstruction(
                blueprintId, title, targetId, "Target", priority,
                "reachable response");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(deployCost);
        return instruction;
    }

    private static PersistentResponsePolicy.Obligation selectResponse(
            Fixture fixture) {
        var plan = new PersistentResponsePlanAdapter.PlanView<>(
                "response", "ground_response", "reinforce",
                List.of(
                        new PersistentResponsePlanAdapter.InstructionView(
                                10, 1010, String.valueOf(TARGET_ID), 1),
                        new PersistentResponsePlanAdapter.InstructionView(
                                11, 1011, String.valueOf(TARGET_ID), 2)));
        AiBoardAnalyzer.LocationAnalysis viable =
                new AiBoardAnalyzer.LocationAnalysis(
                        fixture.target(), 0.0f, 4.0f, 0.0f, 2.0f,
                        1, 1, 0, 1,
                        AiBoardAnalyzer.ContestStatus.LOSING, true);
        return PersistentResponsePlanAdapter.select(
                new PersistentResponsePlanAdapter.Input<>(
                        fixture.game(), "player", fixture.objective(),
                        PersistentResponsePolicy.Snapshot.empty(),
                        List.of(viable), fixture.force().get(),
                        fixture.force().get(), List.of(plan)))
                .orElseThrow().obligation();
    }

    private static Fixture fixture() {
        GameState state = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        AtomicReference<Phase> phase = new AtomicReference<>(Phase.DEPLOY);
        AtomicInteger turn = new AtomicInteger(3);
        AtomicInteger force = new AtomicInteger(4);
        PhysicalCard target = location();
        PhysicalCard lead = character(
                "Maintenance Lead", 10, 1010, true);
        PhysicalCard buddy = character("Buddy", 11, 1011, false);
        AtomicReference<List<PhysicalCard>> hand =
                new AtomicReference<>(List.of(lead, buddy));
        ObjectiveAnalyzer objective = mock(ObjectiveAnalyzer.class);

        when(game.getGameState()).thenReturn(state);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(state.getCurrentPlayerId()).thenReturn("player");
        when(state.getCurrentPhase()).thenAnswer(ignored -> phase.get());
        when(state.getPlayersLatestTurnNumber("player")).thenAnswer(
                ignored -> turn.get());
        when(state.getOpponent("player")).thenReturn("opponent");
        when(state.getSide("player")).thenReturn(Side.DARK);
        when(state.getForcePileSize("player")).thenAnswer(
                ignored -> force.get());
        when(state.getReserveDeckSize("player")).thenReturn(20);
        when(state.getUsedPile("player")).thenReturn(List.of());
        when(state.getHand("player")).thenAnswer(ignored -> hand.get());
        when(state.findCardById(TARGET_ID)).thenReturn(target);
        when(state.findCardByPermanentId(TARGET_ID)).thenReturn(target);
        when(state.getCardsAtLocation(target)).thenReturn(List.of());
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(state, lead, lead, target,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);
        when(modifiers.getDeployCost(state, buddy, buddy, target,
                false, null, false, 0.0f, null, true)).thenReturn(2.0f);
        when(objective.isAnalyzed()).thenReturn(true);
        when(objective.isObjectiveHardLossDefenseLocation(
                game, "player", target)).thenReturn(true);
        when(objective.hasOpponentBattleParticipantAt(
                game, "player", target)).thenReturn(true);
        return new Fixture(
                state, game, objective, target, lead, buddy, phase, hand,
                turn, force);
    }

    private static PhysicalCard location() {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getCardId()).thenReturn(TARGET_ID);
        when(card.getPermanentCardId()).thenReturn(TARGET_ID);
        when(card.getTitle()).thenReturn("Hard-loss site");
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        return card;
    }

    private static PhysicalCard character(
            String title, int permanentId, int currentId,
            boolean maintenance) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getPermanentCardId()).thenReturn(permanentId);
        when(card.getCardId()).thenReturn(currentId);
        when(card.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(4.0f);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(2.0f);
        when(blueprint.getDeployCost()).thenReturn(2.0f);
        when(blueprint.hasIcon(Icon.MAINTENANCE)).thenReturn(maintenance);
        return card;
    }

    private static PhysicalCard boardUnit(String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        return card;
    }

    private static final class ScriptedEvaluator extends ActionEvaluator {
        private final Function<DecisionContext, List<EvaluatedAction>> script;

        private ScriptedEvaluator(
                Function<DecisionContext, List<EvaluatedAction>> script) {
            super("response-bank-reachability");
            this.script = script;
        }

        @Override
        public boolean canEvaluate(DecisionContext context) {
            return true;
        }

        @Override
        public List<EvaluatedAction> evaluate(DecisionContext context) {
            return script.apply(context);
        }
    }

    private record Fixture(
            GameState state,
            SwccgGame game,
            ObjectiveAnalyzer objective,
            PhysicalCard target,
            PhysicalCard lead,
            PhysicalCard buddy,
            AtomicReference<Phase> phase,
            AtomicReference<List<PhysicalCard>> hand,
            AtomicInteger turn,
            AtomicInteger force) {
    }
}
