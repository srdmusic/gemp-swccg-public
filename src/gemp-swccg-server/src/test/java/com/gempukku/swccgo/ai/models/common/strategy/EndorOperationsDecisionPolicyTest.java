package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.DeployBudgetPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullActionFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullOracleView;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidatePolicy;
import com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndorOperationsDecisionPolicyTest {
    private static final String ACTION_ID = "endor-objective-action";
    private static final String PLAYER_ID = "tester";
    private static final String OMINOUS_RUMORS = "Ominous Rumors";

    @Test
    public void parentRequiredCardPullBonusIsSuppressedByHardBlock() {
        PolicyResult open = PullActionPolicy.evaluateParent(
                parentPull(10, true)).result();
        assertOperation(find(open,
                        "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD"),
                "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);

        PolicyResult blocked = PullActionPolicy.evaluateParent(
                parentPull(2, true)).result();
        assertEquals(1, blocked.operations().size());
        assertOperation(blocked.operations().get(0),
                "V60-reserve-risk",
                TraceDomainId.PULL_SEARCH,
                TraceOutputKind.VETO,
                -9999.0f);
        assertFalse(hasRule(blocked,
                "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD"));
    }

    @Test
    public void childPullScoresRequiredCardAndDeployEnablers() {
        assertOperation(only(
                        PullSelectionCandidatePolicy
                                .scoreRequiredOnTableCard(
                                        ACTION_ID, true)),
                "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);
        assertOperation(only(
                        PullSelectionCandidatePolicy
                                .scoreRequiredCardDeployEnabler(
                                        ACTION_ID, true, false)),
                "PULL.OBJECTIVE.REQUIRED_CARD_ENABLER_ACTOR",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);
        assertOperation(only(
                        PullSelectionCandidatePolicy
                                .scoreRequiredCardDeployEnabler(
                                        ACTION_ID, false, true)),
                "PULL.OBJECTIVE.REQUIRED_CARD_ENABLER_LOCATION",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);

        assertTrue(PullSelectionCandidatePolicy
                .scoreRequiredOnTableCard(ACTION_ID, false)
                .operations().isEmpty());
        assertTrue(PullSelectionCandidatePolicy
                .scoreRequiredCardDeployEnabler(
                        ACTION_ID, false, false)
                .operations().isEmpty());
        try {
            PullSelectionCandidatePolicy
                    .scoreRequiredCardDeployEnabler(
                            ACTION_ID, true, true);
            fail("Deploy-enabler actor and location roles must be exclusive");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(
                    "cannot be both actor and location"));
        }
    }

    @Test
    public void requiredCardDeploySignalsShareOneObjectivePreference() {
        PolicyOperation advances = only(
                DeployObjectiveSitingPolicy.scoreRequiredOnTableCard(
                        ACTION_ID,
                        ObjectiveProgressAssessment.Outcome
                                .ADVANCES_MISSING_REQUIREMENT));
        PolicyOperation completes = only(
                DeployObjectiveSitingPolicy.scoreRequiredOnTableCard(
                        ACTION_ID,
                        ObjectiveProgressAssessment.Outcome
                                .COMPLETES_FLIP_NOW));

        assertOperation(advances,
                "DEPLOY.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);
        assertOperation(completes,
                "DEPLOY.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);
        assertEquals(advances.delta(), completes.delta(), 0.0f);
        assertTrue(DeployObjectiveSitingPolicy
                .scoreRequiredOnTableCard(
                        ACTION_ID,
                        ObjectiveProgressAssessment.Outcome.NEUTRAL)
                .operations().isEmpty());
    }

    @Test
    public void requiredCardOrEnablerReserveOnlyPenalizesDistractorThatBreaksBudget() {
        PolicyResult breakingDistractor = requiredCardReserve(
                "breaking-distractor", 5, 3, 3);
        assertOperation(only(breakingDistractor),
                "DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f);

        PolicyResult equalityBoundary = requiredCardReserve(
                "affordable-distractor", 5, 2, 3);
        assertTrue(equalityBoundary.operations().isEmpty());

        PolicyResult objectiveProgressAction = requiredCardReserve(
                "required-card-or-enabler", 5, 4, 0);
        assertTrue(objectiveProgressAction.operations().isEmpty());
    }

    @Test
    public void requiredCardControlEnablerMovementHoldSelfCloses() {
        MoveObjectiveGateHoldPolicy.Evaluation held =
                MoveObjectiveGateHoldPolicy
                        .evaluateRequiredCardControlEnabler(
                                true, true, true, true);
        assertEquals(
                MoveObjectiveGateHoldPolicy.Branch
                        .HOLD_LAST_CONTROL_SOURCE,
                held.branch());
        assertTrue(held.hardVeto());
        assertEquals(
                "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_HOLD: keep the sole control source until the required card deploys",
                held.reason());

        assertNeutralControlEnabler(false, true, true, true);
        assertNeutralControlEnabler(true, false, true, true);
        assertNeutralControlEnabler(true, true, false, true);
        assertNeutralControlEnabler(true, true, true, false);
    }

    @Test
    public void requiredCardControlEnablerBattleBonusObeysEverySafetyGate() {
        PolicyOperation safeBoundary = only(battle(
                true, true, true, false, true,
                -2.0f, 3, 6.0f, 7.0f));
        assertOperation(safeBoundary,
                ObjectiveBattlePolicy
                        .REQUIRED_CARD_CONTROL_ENABLER_RULE_ID,
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);

        assertEmptyBattle(battle(
                false, true, true, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, false, true, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, true, false, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, true, true, true, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, true, true, false, false,
                0.0f, 3, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, true, true, false, true,
                -2.01f, 3, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, true, true, false, true,
                0.0f, 2, 6.0f, 6.0f));
        assertEmptyBattle(battle(
                true, true, true, false, true,
                -2.0f, 3, 3.0f, 7.0f));

        assertEquals(1, battle(
                true, true, true, false, true,
                8.0f, 2, 12.0f, 4.0f)
                .operations().size());
    }

    @Test
    public void hardLossLocationBattleBonusUsesTheSameSafetyBoundary() {
        PolicyOperation safeBoundary = only(hardLossBattle(
                true, true, false, true,
                -2.0f, 3, 6.0f, 7.0f));
        assertOperation(safeBoundary,
                ObjectiveBattlePolicy.HARD_LOSS_LOCATION_RULE_ID,
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f);

        assertEmptyBattle(hardLossBattle(
                true, false, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmptyBattle(hardLossBattle(
                true, true, false, true,
                -2.01f, 3, 6.0f, 6.0f));
        assertEmptyBattle(hardLossBattle(
                true, true, true, true,
                0.0f, 3, 6.0f, 6.0f));
    }

    @Test
    public void standaloneForceLossProtectsOminousRumorsAndChoosesDisposableWithBotParity() {
        PhysicalCard ominous = forceLossCard(
                OMINOUS_RUMORS, "8_127", Zone.HAND);
        PhysicalCard disposable = forceLossCard(
                "Disposable Effect", "test_disposable", Zone.HAND);
        List<PhysicalCard> hand = List.of(
                ominous,
                disposable,
                forceLossCard("Filler One", "test_filler_1", Zone.HAND),
                forceLossCard("Filler Two", "test_filler_2", Zone.HAND),
                forceLossCard("Filler Three", "test_filler_3", Zone.HAND));
        Map<Integer, PhysicalCard> candidates = Map.of(
                41, ominous,
                42, disposable);
        ForceLossFixture fixture = forceLossFixture(hand, candidates);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                fixture.game(), PLAYER_ID, Side.DARK);
        chosenAnalyzer.analyze(
                fixture.game(), PLAYER_ID, Side.DARK);

        assertTrue(randoAnalyzer.isAnalyzed());
        assertTrue(chosenAnalyzer.isAnalyzed());
        assertEquals("8_167",
                randoAnalyzer.getObjectiveBlueprintId());
        assertEquals("8_167",
                chosenAnalyzer.getObjectiveBlueprintId());
        assertTrue(randoAnalyzer.isRequiredCardForFlip(
                OMINOUS_RUMORS));
        assertTrue(chosenAnalyzer.isRequiredCardForFlip(
                OMINOUS_RUMORS));

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                fixture.gameState(),
                                PLAYER_ID,
                                "CARD_SELECTION",
                                "Choose Force to lose",
                                "endor-force-loss",
                                Phase.CONTROL);
        randoContext.setGame(fixture.game());
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setCardIds(List.of("41", "42"));
        randoContext.setSelectable(List.of(true, true));

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                fixture.gameState(),
                                PLAYER_ID,
                                "CARD_SELECTION",
                                "Choose Force to lose",
                                "endor-force-loss",
                                Phase.CONTROL);
        chosenContext.setGame(fixture.game());
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setCardIds(List.of("41", "42"));
        chosenContext.setSelectable(List.of(true, true));

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .CardSelectionEvaluator()
                        .evaluate(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CardSelectionEvaluator()
                        .evaluate(chosenContext);

        assertForceLossParity(rando, chosen);
        var protectedEffect = randoAction(rando, "41");
        var disposableEffect = randoAction(rando, "42");
        assertFloatBits(350.0f, protectedEffect.getScore());
        assertFloatBits(650.0f, disposableEffect.getScore());
        assertEquals(List.of(
                        "V153 ZONE (HAND, lifeForce=11, protectChars=true) (+600.0)",
                        "OBJECTIVE CRITICAL IN HAND: prefer to retain (-300.0)"),
                protectedEffect.getReasoning());
        assertEquals(List.of(
                        "V153 ZONE (HAND, lifeForce=11, protectChars=true) (+600.0)"),
                disposableEffect.getReasoning());
        assertEquals("42", rando.stream()
                .max((left, right) -> Float.compare(
                        left.getScore(), right.getScore()))
                .orElseThrow()
                .getActionId());
        assertEquals("42", chosen.stream()
                .max((left, right) -> Float.compare(
                        left.getScore(), right.getScore()))
                .orElseThrow()
                .getActionId());
    }

    private static PullActionFacts.Parent parentPull(
            int reserveSize, boolean requiredOnTableCardPull) {
        PullOracleView.Validation unknown =
                new PullOracleView.Validation(
                        PullOracleView.Outcome.UNKNOWN, "");
        return new PullActionFacts.Parent(
                ACTION_ID,
                "Deploy a required Effect from Reserve Deck",
                reserveSize,
                false,
                "",
                unknown,
                unknown,
                "Endor Operations",
                false,
                null,
                5,
                false,
                "",
                0,
                false,
                "",
                CardCategory.OBJECTIVE,
                PullActionFacts.V131State.OPEN,
                "",
                false,
                "",
                false,
                0,
                0,
                0,
                false,
                "",
                0,
                0,
                "",
                false,
                Phase.CONTROL,
                false,
                false,
                false,
                PullActionFacts.FormationState.NONE,
                "",
                requiredOnTableCardPull,
                false,
                false,
                // WMAOP 2026-08-08 (Steve directive): wmaopBlockadeSiteOnTable
                false);
    }

    private static PolicyResult requiredCardReserve(
            String actionId, int availableForce,
            int deployCost, int reserve) {
        return DeployBudgetPolicy.futureObligations(
                new DeployBudgetPolicy.FutureObligationFacts(
                        actionId,
                        availableForce,
                        deployCost,
                        0,
                        0,
                        0,
                        false,
                        0,
                        false,
                        false,
                        0,
                        reserve))
                .result();
    }

    private static void assertNeutralControlEnabler(
            boolean active,
            boolean exactLocation,
            boolean controls,
            boolean soleControlSource) {
        MoveObjectiveGateHoldPolicy.Evaluation result =
                MoveObjectiveGateHoldPolicy
                        .evaluateRequiredCardControlEnabler(
                                active,
                                exactLocation,
                                controls,
                                soleControlSource);
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE,
                result.branch());
        assertFalse(result.hardVeto());
    }

    private static PolicyResult battle(
            boolean requiredCardControlEnabler,
            boolean missingSelfControl,
            boolean bothSidesPresent,
            boolean formationSafetyVeto,
            boolean predictorSafe,
            float effectiveDiff,
            int reserveDeckSize,
            float ourPower,
            float theirPower) {
        return ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        ACTION_ID,
                        false,
                        missingSelfControl,
                        requiredCardControlEnabler,
                        false,
                        bothSidesPresent,
                        formationSafetyVeto,
                        predictorSafe,
                        effectiveDiff,
                        reserveDeckSize,
                        ourPower,
                        theirPower));
    }

    private static PolicyResult hardLossBattle(
            boolean hardLossLocation,
            boolean missingSelfControl,
            boolean formationSafetyVeto,
            boolean predictorSafe,
            float effectiveDiff,
            int reserveDeckSize,
            float ourPower,
            float theirPower) {
        return ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        ACTION_ID,
                        false,
                        missingSelfControl,
                        false,
                        hardLossLocation,
                        false,
                        true,
                        formationSafetyVeto,
                        predictorSafe,
                        effectiveDiff,
                        reserveDeckSize,
                        ourPower,
                        theirPower));
    }

    private static ForceLossFixture forceLossFixture(
            List<PhysicalCard> hand,
            Map<Integer, PhysicalCard> candidates) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = endorOperationsObjective();

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getAllPermanentCards()).thenReturn(
                List.of(objective));
        when(gameState.getHand(PLAYER_ID)).thenReturn(hand);
        when(gameState.getReserveDeckSize(PLAYER_ID)).thenReturn(11);
        when(gameState.getUsedPile(PLAYER_ID)).thenReturn(List.of());
        when(gameState.getForcePileSize(PLAYER_ID)).thenReturn(0);
        when(gameState.getPlayersLatestTurnNumber(PLAYER_ID))
                .thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER_ID);
        when(gameState.getOpponent(PLAYER_ID)).thenReturn("opponent");
        when(gameState.getCardPile(
                PLAYER_ID, Zone.RESERVE_DECK))
                .thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(List.of());
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> candidates.get(
                        invocation.getArgument(0, Integer.class)));

        return new ForceLossFixture(game, gameState);
    }

    private static PhysicalCard endorOperationsObjective() {
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn("8_167");
        when(objective.isFlipped()).thenReturn(false);
        when(front.getTitle()).thenReturn("Endor Operations");
        when(front.getGameText()).thenReturn(
                "Deploy Endor system, Bunker and Landing Platform. "
                        + "Flip this card if Ominous Rumors and "
                        + "Establish Secret Base are both on table.");
        when(front.getCardCategory()).thenReturn(
                CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn("Imperial Outpost");
        when(back.getGameText()).thenReturn(
                "Flip this card if Ominous Rumors and Establish "
                        + "Secret Base are not both on table.");
        when(back.getCardCategory()).thenReturn(
                CardCategory.OBJECTIVE);
        return objective;
    }

    private static PhysicalCard forceLossCard(
            String title, String blueprintId, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(PLAYER_ID);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(
                CardCategory.EFFECT);
        return card;
    }

    private static void assertForceLossParity(
            List<com.gempukku.swccgo.ai.models.rando.evaluators
                    .EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .EvaluatedAction> chosen) {
        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(),
                    chosen.get(i).getActionId());
            assertFloatBits(rando.get(i).getScore(),
                    chosen.get(i).getScore());
            assertEquals(rando.get(i).getReasoning(),
                    chosen.get(i).getReasoning());
            assertEquals(rando.get(i).getDisplayText(),
                    chosen.get(i).getDisplayText());
            assertEquals(rando.get(i).isHardVetoed(),
                    chosen.get(i).isHardVetoed());
            assertEquals(rando.get(i).isDeferred(),
                    chosen.get(i).isDeferred());
        }
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .EvaluatedAction randoAction(
                    List<com.gempukku.swccgo.ai.models.rando.evaluators
                            .EvaluatedAction> actions,
                    String actionId) {
        return actions.stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertFloatBits(
            float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private static PolicyOperation only(PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static PolicyOperation find(
            PolicyResult result, String ruleId) {
        return result.operations().stream()
                .filter(operation -> operation.ruleArmId().id()
                        .equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing rule " + ruleId));
    }

    private static boolean hasRule(
            PolicyResult result, String ruleId) {
        return result.operations().stream()
                .anyMatch(operation -> operation.ruleArmId().id()
                        .equals(ruleId));
    }

    private static void assertEmptyBattle(PolicyResult result) {
        assertTrue(result.operations().isEmpty());
    }

    private static void assertOperation(
            PolicyOperation operation,
            String ruleId,
            TraceDomainId domain,
            TraceOutputKind outputKind,
            float delta) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domain, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
    }

    private record ForceLossFixture(
            SwccgGame game, GameState gameState) { }
}
