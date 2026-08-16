package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BattleDecisionPolicyTest {

    @Test
    public void battleTacticsPreserveBaseScoreAndContributionOrder() {
        BattleDecisionPolicy.Context context = context(
            null, null, List.of("fire", "draw", "ignore"),
            List.of("Fire weapon at unique character", "Draw battle destiny", "Use an interrupt"),
            List.of("", "", ""), new AtomicInteger());

        List<BattleDecisionPolicy.ScoredAction> results = BattleDecisionPolicy.evaluate(context);

        assertEquals(2, results.size());
        assertEquals("fire", results.get(0).actionId());
        assertEquals(100.0f, results.get(0).baseScore(), 0.0f);
        assertEquals(List.of("Fire weapon", "Target character", "Target unique card", "Fire weapons during battle"),
            results.get(0).contributions().stream().map(BattleDecisionPolicy.Contribution::reason).toList());
        assertEquals(List.of(40.0f, 10.0f, 20.0f, 50.0f),
            results.get(0).contributions().stream().map(BattleDecisionPolicy.Contribution::delta).toList());
        assertEquals(List.of("Draw battle destiny"),
            results.get(1).contributions().stream().map(BattleDecisionPolicy.Contribution::reason).toList());
        assertEquals(30.0f, results.get(1).contributions().get(0).delta(), 0.0f);
    }

    @Test
    public void v76PredictorRunsExactlyOncePerCandidate() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        AtomicInteger predictions = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Test Site");
        when(modifiers.getTotalPowerAtLocation(any(), any(), anyString(), anyBoolean(), anyBoolean()))
            .thenAnswer(invocation -> "bot".equals(invocation.getArgument(2)) ? 10.0f : 8.0f);
        when(modifiers.getTotalAbilityAtLocation(any(), anyString(), any()))
            .thenReturn(4.0f);

        BattleDecisionPolicy.Context context = context(
            gameState, game, List.of("battle"), List.of("Initiate battle"),
            List.of("7"), predictions);

        List<BattleDecisionPolicy.ScoredAction> results = BattleDecisionPolicy.evaluate(context);

        assertEquals(1, results.size());
        assertEquals(1, predictions.get());
        assertFalse(results.get(0).contributions().isEmpty());
    }

    @Test
    public void opponentRawFivePlusOneActiveLightsaberReachesPredictorAsTenExactlyOnce() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard opponent = mock(PhysicalCard.class);
        PhysicalCard lightsaber = mock(PhysicalCard.class);
        SwccgCardBlueprint opponentBlueprint =
                mock(SwccgCardBlueprint.class);
        AtomicInteger predictions = new AtomicInteger();
        AtomicInteger predictedOpponentPower =
                new AtomicInteger(-1);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location))
                .thenReturn(List.of(opponent));
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(opponent));
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Armed Opponent Site");
        when(opponent.getOwner()).thenReturn("opponent");
        when(opponent.getBlueprint()).thenReturn(opponentBlueprint);
        when(opponentBlueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(gameState.getAttachedCards(opponent))
                .thenReturn(List.of(lightsaber));
        when(gameState.isCardInPlayActive(
                lightsaber, true, true, true,
                false, false, true, true, false))
                .thenReturn(true);
        when(modifiers.getCardTypes(gameState, lightsaber))
                .thenReturn(Set.of(CardType.WEAPON));
        when(modifiers.hasKeyword(
                gameState, lightsaber, Keyword.LIGHTSABER))
                .thenReturn(true);
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(),
                anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                        "bot".equals(invocation.getArgument(2))
                                ? 7.0f : 5.0f);
        when(modifiers.getTotalAbilityAtLocation(
                any(), anyString(), any()))
                .thenReturn(4.0f);

        BattleDecisionPolicy.Context base = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of("7"),
                predictions);
        BattleDecisionPolicy.Context capture =
                new DelegatingContext(base) {
                    @Override
                    public BattleDecisionPolicy.Prediction predictBattle(
                            int myPower, int myDestinyDraws,
                            int opponentPower,
                            int opponentDestinyDraws) {
                        predictions.incrementAndGet();
                        predictedOpponentPower.set(opponentPower);
                        return new BattleDecisionPolicy.Prediction(
                                0.75f, 3.0f, 1.0f);
                    }
                };

        List<BattleDecisionPolicy.ScoredAction> results =
                BattleDecisionPolicy.evaluate(capture);

        assertEquals(1, results.size());
        assertEquals(1, predictions.get());
        assertEquals(10, predictedOpponentPower.get());
    }

    @Test
    public void predictorAlsoGuardsPowerZeroPhysicalOpponent() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard opponentCard = mock(PhysicalCard.class);
        AtomicInteger predictions = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location))
                .thenReturn(List.of(opponentCard));
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(opponentCard));
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Power-Zero Site");
        when(opponentCard.getOwner()).thenReturn("opponent");
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(),
                anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                    "bot".equals(invocation.getArgument(2))
                        ? 10.0f : 0.0f);
        when(modifiers.getTotalAbilityAtLocation(
                any(), anyString(), any()))
                .thenReturn(4.0f);

        BattleDecisionPolicy.Context base = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"),
                List.of("7"), predictions);
        BattleDecisionPolicy.Context unsafe =
                new DelegatingContext(base) {
                    @Override
                    public BattleDecisionPolicy.Prediction
                            predictBattle(
                                int myPower,
                                int myDestinyDraws,
                                int opponentPower,
                                int opponentDestinyDraws) {
                        predictions.incrementAndGet();
                        return new BattleDecisionPolicy.Prediction(
                                0.20f, 1.0f, 4.0f);
                    }
                };

        BattleDecisionPolicy.ScoredAction result =
                BattleDecisionPolicy.evaluate(unsafe).get(0);

        assertEquals(1, predictions.get());
        assertTrue(contributionIndexByReason(
                result.contributions(),
                "V76 BATTLE PREDICT") >= 0);
        assertTrue(score(result) < 0.0f);
    }

    // ADDED 2026-08-08 (passivity fix, m01683): the incident this guards — a winRate
    // 0.98 battle came out net NEGATIVE because the ability-weighted V25/V29 band
    // (negative effectiveDiff) plus the -60 no-favorable scanOutcome outvoted the
    // predictor. A confident predictor (winRate >= 0.75) must neutralize the negative
    // band and leave the action net positive.
    @Test
    public void confidentPredictorNeutralizesNegativeAbilityBand() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        AtomicInteger predictions = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Confident Site");
        // Equal power + equal ability => weaponEffectiveDiff 0 => V29 UNFAVORABLE -100.
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(8.0f);
        when(modifiers.getTotalAbilityAtLocation(any(), anyString(), any()))
                .thenReturn(4.0f);

        BattleDecisionPolicy.Context base = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of("7"), predictions);
        BattleDecisionPolicy.Context confident = new DelegatingContext(base) {
            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower, int myDestinyDraws,
                    int opponentPower, int opponentDestinyDraws) {
                predictions.incrementAndGet();
                return new BattleDecisionPolicy.Prediction(0.98f, 6.0f, 0.5f);
            }
        };

        BattleDecisionPolicy.ScoredAction result =
                BattleDecisionPolicy.evaluate(confident).get(0);

        int bandIndex = contributionIndexByReason(
                result.contributions(), "V29: UNFAVORABLE");
        int confidentIndex = contributionIndexByReason(
                result.contributions(), "V76 PREDICTOR CONFIDENT");
        assertTrue(bandIndex >= 0);
        assertTrue(confidentIndex > bandIndex);
        assertEquals(
                -result.contributions().get(bandIndex).delta(),
                result.contributions().get(confidentIndex).delta(), 0.001f);
        assertEquals(-1, contributionIndexByReason(
                result.contributions(), "No favorable battles"));
        assertTrue("98%-win battle must be net positive", score(result) > 0.0f);
    }

    @Test
    public void exactMissingPreFlipTargetAddsObjectiveContestAfterSpecificBattle() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        ObjectiveAnalyzer objectiveAnalyzer = mock(ObjectiveAnalyzer.class);
        AtomicInteger predictions = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle())
                .thenReturn("Naboo: Theed Palace Throne Room");
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(8.0f);
        when(modifiers.getTotalAbilityAtLocation(any(), anyString(), any()))
                .thenReturn(4.0f);
        when(modifiers.controlsLocation(
                gameState, location, "bot",
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(false);
        when(objectiveAnalyzer.isMissingPreFlipRequirementAt(
                game, "bot", location)).thenReturn(true);

        BattleDecisionPolicy.Context base = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of("7"), predictions);
        BattleDecisionPolicy.Context context = new DelegatingContext(base) {
            @Override
            public ObjectiveAnalyzer getObjectiveAnalyzer() {
                return objectiveAnalyzer;
            }
        };

        BattleDecisionPolicy.ScoredAction result =
                BattleDecisionPolicy.evaluate(context).get(0);
        int specificIndex = contributionIndexByReason(
                result.contributions(), "V29: UNFAVORABLE");
        int objectiveIndex = contributionIndexByRule(
                result.contributions(),
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_RULE_ID);

        assertTrue(specificIndex >= 0);
        assertTrue(objectiveIndex > specificIndex);
        assertEquals(
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                result.contributions().get(objectiveIndex).delta(), 0.0f);
        assertEquals(1, predictions.get());
    }

    @Test
    public void objectiveContestFactChangesSafeRankingButCannotLiftUnsafeBattle() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard distractor = mock(PhysicalCard.class);
        PhysicalCard throne = mock(PhysicalCard.class);
        ObjectiveAnalyzer objectiveAnalyzer = mock(ObjectiveAnalyzer.class);

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations())
                .thenReturn(List.of(distractor, throne));
        when(gameState.getCardsAtLocation(distractor)).thenReturn(List.of());
        when(gameState.getCardsAtLocation(throne)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(distractor.getCardId()).thenReturn(7);
        when(distractor.getTitle()).thenReturn("Naboo: Swamp");
        when(throne.getCardId()).thenReturn(8);
        when(throne.getTitle())
                .thenReturn("Naboo: Theed Palace Throne Room");
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(8.0f);
        when(modifiers.getTotalAbilityAtLocation(any(), anyString(), any()))
                .thenReturn(4.0f);
        when(modifiers.controlsLocation(
                gameState, throne, "bot",
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(false);

        BattleDecisionPolicy.Context base = context(
                gameState, game,
                List.of("distractor", "throne"),
                List.of("Initiate battle", "Initiate battle"),
                List.of("7", "8"), new AtomicInteger());
        BattleDecisionPolicy.Context objectiveContext =
                new DelegatingContext(base) {
                    @Override
                    public ObjectiveAnalyzer getObjectiveAnalyzer() {
                        return objectiveAnalyzer;
                    }
                };

        when(objectiveAnalyzer.isMissingPreFlipRequirementAt(
                game, "bot", throne)).thenReturn(false);
        List<BattleDecisionPolicy.ScoredAction> baseline =
                BattleDecisionPolicy.evaluate(objectiveContext);
        assertEquals(score(action(baseline, "distractor")),
                score(action(baseline, "throne")), 0.0f);
        assertEquals("distractor", highestRanked(baseline).actionId());

        when(objectiveAnalyzer.isMissingPreFlipRequirementAt(
                game, "bot", throne)).thenReturn(true);
        List<BattleDecisionPolicy.ScoredAction> objectiveAware =
                BattleDecisionPolicy.evaluate(objectiveContext);
        assertEquals(
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                score(action(objectiveAware, "throne"))
                        - score(action(objectiveAware, "distractor")),
                0.0f);
        assertEquals("throne", highestRanked(objectiveAware).actionId());

        BattleDecisionPolicy.Context unsafeContext =
                new DelegatingContext(objectiveContext) {
                    @Override
                    public BattleDecisionPolicy.Prediction predictBattle(
                            int myPower, int myDestinyDraws,
                            int opponentPower, int opponentDestinyDraws) {
                        return new BattleDecisionPolicy.Prediction(
                                0.20f, 3.0f, 1.0f);
                    }
                };
        List<BattleDecisionPolicy.ScoredAction> unsafe =
                BattleDecisionPolicy.evaluate(unsafeContext);
        assertEquals(score(action(unsafe, "distractor")),
                score(action(unsafe, "throne")), 0.0f);
        assertEquals(-1, contributionIndexByRule(
                action(unsafe, "throne").contributions(),
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_RULE_ID));
        assertEquals("distractor", highestRanked(unsafe).actionId());
    }

    @Test
    public void setYourCourseBattleDiscouragesSpendingTheOnlyHyperspeedForce() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        ObjectiveAnalyzer objectiveAnalyzer = mock(ObjectiveAnalyzer.class);

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getForcePileSize("bot")).thenReturn(1);
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Test Battleground");
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(8.0f);
        when(modifiers.getTotalAbilityAtLocation(
                any(), anyString(), any())).thenReturn(4.0f);
        when(modifiers.getInitiateBattleCost(
                gameState, location, "bot", true)).thenReturn(1.0f);
        when(objectiveAnalyzer.getSetYourCourseNextRouteForceReserve(
                game, "bot")).thenReturn(1);

        BattleDecisionPolicy.Context base = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of("7"),
                new AtomicInteger());
        BattleDecisionPolicy.Context context =
                new DelegatingContext(base) {
                    @Override
                    public ObjectiveAnalyzer getObjectiveAnalyzer() {
                        return objectiveAnalyzer;
                    }
                };

        BattleDecisionPolicy.ScoredAction result =
                BattleDecisionPolicy.evaluate(context).get(0);
        int reserveVeto = contributionIndexByRule(
                result.contributions(),
                ObjectiveBattlePolicy
                    .OBJECTIVE_MOVE_FORCE_RESERVE_RULE_ID);

        assertTrue(reserveVeto >= 0);
        BattleDecisionPolicy.Contribution contribution =
                result.contributions().get(reserveVeto);
        assertFalse(contribution.hardVeto());
        assertEquals(-300.0f, contribution.delta(), 0.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                contribution.domainId());
    }

    @Test
    public void namedRetentionTelemetryUsesCachedPredictionAndScoresZero() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        AtomicInteger predictions = new AtomicInteger();
        AtomicInteger retentionReads = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Exact Test Site");
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(8.0f);
        when(modifiers.getTotalAbilityAtLocation(
                any(), anyString(), any())).thenReturn(4.0f);
        BattleDecisionPolicy.Context base = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of("7"), predictions);
        BattleDecisionPolicy.Context telemetry = new DelegatingContext(base) {
            @Override
            public BattleRetentionPolicy.Facts readBattleRetentionFacts(
                    String actionId,
                    PhysicalCard target,
                    BattleDecisionPolicy.PredictionGate predictionGate) {
                retentionReads.incrementAndGet();
                assertEquals(location, target);
                assertTrue(predictionGate != null);
                return rawRetentionFacts(actionId);
            }
        };

        BattleDecisionPolicy.ScoredAction baseline =
                BattleDecisionPolicy.evaluate(base).get(0);
        predictions.set(0);
        BattleDecisionPolicy.ScoredAction result =
                BattleDecisionPolicy.evaluate(telemetry).get(0);

        assertEquals(1, predictions.get());
        assertEquals(1, retentionReads.get());
        assertEquals(score(baseline), score(result), 0.0f);
        assertEquals(-1, contributionIndexByReason(
                result.contributions(), "B3"));
    }

    @Test
    public void locationlessAndPowerZeroRoutesNeverReadRetentionFacts() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard opponentCard = mock(PhysicalCard.class);
        AtomicInteger predictions = new AtomicInteger();
        AtomicInteger retentionReads = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location))
                .thenReturn(List.of(opponentCard));
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(opponentCard));
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Hidden Site");
        when(opponentCard.getOwner()).thenReturn("opponent");
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                        "bot".equals(invocation.getArgument(2))
                                ? 10.0f : 8.0f);
        when(modifiers.getTotalAbilityAtLocation(
                any(), anyString(), any())).thenReturn(4.0f);

        BattleDecisionPolicy.Context locationlessBase = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of(""), predictions);
        BattleDecisionPolicy.Context locationless =
                countingRetentionContext(
                        locationlessBase, retentionReads);
        BattleDecisionPolicy.evaluate(locationless);
        assertEquals(0, retentionReads.get());
        assertEquals(0, predictions.get());

        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                        "bot".equals(invocation.getArgument(2))
                                ? 10.0f : 0.0f);
        BattleDecisionPolicy.Context powerZeroBase = context(
                gameState, game, List.of("battle"),
                List.of("Initiate battle"), List.of("7"), predictions);
        BattleDecisionPolicy.Context powerZero =
                countingRetentionContext(powerZeroBase, retentionReads);
        BattleDecisionPolicy.evaluate(powerZero);

        assertEquals(0, retentionReads.get());
        assertEquals(1, predictions.get());
    }

    @Test
    public void routingMatchesBattleDecisionContract() {
        AtomicInteger predictions = new AtomicInteger();
        assertTrue(BattleDecisionPolicy.canEvaluate(context(
            null, null, List.of("0"), List.of("Initiate battle"), List.of(""), predictions)));

        BattleDecisionPolicy.Context nonBattle = context(
            null, null, List.of("0"), List.of("Use an interrupt"), List.of(""), predictions);
        assertFalse(BattleDecisionPolicy.canEvaluate(new DelegatingContext(nonBattle) {
            @Override
            public Phase getPhase() {
                return Phase.DEPLOY;
            }

            @Override
            public String getDecisionText() {
                return "Choose an action";
            }
        }));
    }

    private static BattleDecisionPolicy.ScoredAction action(
            List<BattleDecisionPolicy.ScoredAction> actions,
            String actionId) {
        for (BattleDecisionPolicy.ScoredAction action : actions) {
            if (actionId.equals(action.actionId())) {
                return action;
            }
        }
        throw new AssertionError("Missing action " + actionId);
    }

    private static BattleDecisionPolicy.ScoredAction highestRanked(
            List<BattleDecisionPolicy.ScoredAction> actions) {
        BattleDecisionPolicy.ScoredAction best = actions.get(0);
        for (int index = 1; index < actions.size(); index++) {
            BattleDecisionPolicy.ScoredAction candidate = actions.get(index);
            if (score(candidate) > score(best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static float score(BattleDecisionPolicy.ScoredAction action) {
        for (BattleDecisionPolicy.Contribution contribution
                : action.contributions()) {
            if (contribution.hardVeto()) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        float score = action.baseScore();
        for (BattleDecisionPolicy.Contribution contribution
                : action.contributions()) {
            score += contribution.delta();
        }
        return score;
    }

    private static int contributionIndexByReason(
            List<BattleDecisionPolicy.Contribution> contributions,
            String reasonPrefix) {
        for (int index = 0; index < contributions.size(); index++) {
            String reason = contributions.get(index).reason();
            if (reason != null && reason.startsWith(reasonPrefix)) {
                return index;
            }
        }
        return -1;
    }

    private static int contributionIndexByRule(
            List<BattleDecisionPolicy.Contribution> contributions,
            String ruleId) {
        for (int index = 0; index < contributions.size(); index++) {
            if (contributions.get(index).ruleArmId() != null
                    && ruleId.equals(
                            contributions.get(index).ruleArmId().id())) {
                return index;
            }
        }
        return -1;
    }

    private static BattleDecisionPolicy.Context countingRetentionContext(
            BattleDecisionPolicy.Context base,
            AtomicInteger retentionReads) {
        return new DelegatingContext(base) {
            @Override
            public BattleRetentionPolicy.Facts readBattleRetentionFacts(
                    String actionId,
                    PhysicalCard target,
                    BattleDecisionPolicy.PredictionGate predictionGate) {
                retentionReads.incrementAndGet();
                return rawRetentionFacts(actionId);
            }
        };
    }

    private static BattleRetentionPolicy.Facts rawRetentionFacts(
            String actionId) {
        return new BattleRetentionPolicy.Facts(
                actionId,
                BattleRetentionPolicy.Knowledge.RAW_PREDICTOR_ONLY,
                new BattleRetentionPolicy.PredictionTelemetry(
                        0.75f, 3.0f, 1.0f, 3.0f, 3.0f),
                "test-raw");
    }

    private static BattleDecisionPolicy.Context context(
            GameState gameState, SwccgGame game, List<String> actionIds,
            List<String> actionTexts, List<String> cardIds, AtomicInteger predictions) {
        return new BattleDecisionPolicy.Context() {
            @Override public String getDecisionType() { return "CARD_ACTION_CHOICE"; }
            @Override public Phase getPhase() { return Phase.BATTLE; }
            @Override public String getDecisionText() { return "Choose battle action"; }
            @Override public List<String> getActionIds() { return actionIds; }
            @Override public List<String> getActionTexts() { return actionTexts; }
            @Override public List<String> getCardIds() { return cardIds; }
            @Override public GameState getGameState() { return gameState; }
            @Override public SwccgGame getGame() { return game; }
            @Override public String getPlayerId() { return "bot"; }
            @Override public int getReserveDeckSize() { return 20; }
            @Override public int getLifeForce() { return 20; }
            @Override public int getForcePileSize() { return 5; }
            @Override public int getHandSize() { return 5; }
            @Override public ObjectiveAnalyzer getObjectiveAnalyzer() { return null; }
            @Override public float getVaderExpendabilityFactor() { return 0.3f; }
            @Override public int getCriticalLifeForce() { return 6; }
            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower, int myDestinyDraws, int opponentPower, int opponentDestinyDraws) {
                predictions.incrementAndGet();
                return new BattleDecisionPolicy.Prediction(0.75f, 3.0f, 1.0f);
            }
            @Override public Logger getLogger() { return LogManager.getLogger(BattleDecisionPolicyTest.class); }
        };
    }

    private abstract static class DelegatingContext implements BattleDecisionPolicy.Context {
        private final BattleDecisionPolicy.Context delegate;

        private DelegatingContext(BattleDecisionPolicy.Context delegate) {
            this.delegate = delegate;
        }

        @Override public String getDecisionType() { return delegate.getDecisionType(); }
        @Override public Phase getPhase() { return delegate.getPhase(); }
        @Override public String getDecisionText() { return delegate.getDecisionText(); }
        @Override public List<String> getActionIds() { return delegate.getActionIds(); }
        @Override public List<String> getActionTexts() { return delegate.getActionTexts(); }
        @Override public List<String> getCardIds() { return delegate.getCardIds(); }
        @Override public GameState getGameState() { return delegate.getGameState(); }
        @Override public SwccgGame getGame() { return delegate.getGame(); }
        @Override public String getPlayerId() { return delegate.getPlayerId(); }
        @Override public int getReserveDeckSize() { return delegate.getReserveDeckSize(); }
        @Override public int getLifeForce() { return delegate.getLifeForce(); }
        @Override public int getForcePileSize() { return delegate.getForcePileSize(); }
        @Override public int getHandSize() { return delegate.getHandSize(); }
        @Override public ObjectiveAnalyzer getObjectiveAnalyzer() { return delegate.getObjectiveAnalyzer(); }
        @Override public float getVaderExpendabilityFactor() { return delegate.getVaderExpendabilityFactor(); }
        @Override public int getCriticalLifeForce() { return delegate.getCriticalLifeForce(); }
        @Override public BattleDecisionPolicy.Prediction predictBattle(int a, int b, int c, int d) {
            return delegate.predictBattle(a, b, c, d);
        }
        @Override
        public BattleRetentionPolicy.Facts readBattleRetentionFacts(
                String actionId,
                PhysicalCard target,
                BattleDecisionPolicy.PredictionGate predictionGate) {
            return delegate.readBattleRetentionFacts(
                    actionId, target, predictionGate);
        }
        @Override public Logger getLogger() { return delegate.getLogger(); }
    }
}
