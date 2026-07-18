package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.chosenone.RandoConfig;
import com.gempukku.swccgo.ai.models.common.phase.BattleDecisionPolicy;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** ChosenOne adapter for the shared BATTLE-1 decision policy. */
public class BattleEvaluator extends ActionEvaluator {

    public BattleEvaluator() {
        super("Battle");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        return BattleDecisionPolicy.canEvaluate(adapt(context));
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        for (BattleDecisionPolicy.ScoredAction result : BattleDecisionPolicy.evaluate(adapt(context))) {
            EvaluatedAction action = new EvaluatedAction(
                result.actionId(), ActionType.BATTLE, result.baseScore(), result.actionText());
            for (BattleDecisionPolicy.Contribution contribution : result.contributions()) {
                if (contribution.hardVeto()) {
                    if (contribution.ruleArmId() == null) {
                        action.hardVeto(contribution.reason());
                    } else {
                        action.hardVeto(contribution.reason(), contribution.ruleArmId(),
                            contribution.domainId(), contribution.outputKind());
                    }
                } else {
                    if (contribution.ruleArmId() == null) {
                        action.addReasoning(contribution.reason(), contribution.delta());
                    } else {
                        action.addReasoning(contribution.reason(), contribution.delta(),
                            contribution.ruleArmId(), contribution.domainId(), contribution.outputKind());
                    }
                }
            }
            actions.add(action);
        }
        return actions;
    }

    private BattleDecisionPolicy.Context adapt(DecisionContext context) {
        return new BattleDecisionPolicy.Context() {
            @Override
            public String getDecisionType() {
                return context.getDecisionType();
            }

            @Override
            public Phase getPhase() {
                return context.getPhase();
            }

            @Override
            public String getDecisionText() {
                return context.getDecisionText();
            }

            @Override
            public List<String> getActionIds() {
                return context.getActionIds();
            }

            @Override
            public List<String> getActionTexts() {
                return context.getActionTexts();
            }

            @Override
            public List<String> getCardIds() {
                return context.getCardIds();
            }

            @Override
            public GameState getGameState() {
                return context.getGameState();
            }

            @Override
            public SwccgGame getGame() {
                return context.getGame();
            }

            @Override
            public String getPlayerId() {
                return context.getPlayerId();
            }

            @Override
            public int getReserveDeckSize() {
                return context.getReserveDeckSize();
            }

            @Override
            public int getLifeForce() {
                return context.getLifeForce();
            }

            @Override
            public int getForcePileSize() {
                return context.getForcePileSize();
            }

            @Override
            public int getHandSize() {
                return context.getHandSize();
            }

            @Override
            public ObjectiveAnalyzer getObjectiveAnalyzer() {
                return context.getObjectiveAnalyzer();
            }

            @Override
            public float getVaderExpendabilityFactor() {
                return RandoConfig.VADER_EXPENDABILITY_FACTOR;
            }

            @Override
            public int getCriticalLifeForce() {
                return RandoConfig.CRITICAL_LIFE_FORCE;
            }

            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower, int myDestinyDraws, int opponentPower, int opponentDestinyDraws) {
                BattlePredictor.BattleOutcome outcome = BattlePredictor.predictBattle(
                    myPower, myDestinyDraws, opponentPower, opponentDestinyDraws,
                    context.getDeckOracle(), context.getOpponentDeckTracker());
                return new BattleDecisionPolicy.Prediction(
                    outcome.winProbability, outcome.expectedDamageDealt, outcome.expectedDamageTaken);
            }

            @Override
            public Logger getLogger() {
                return logger;
            }
        };
    }
}
