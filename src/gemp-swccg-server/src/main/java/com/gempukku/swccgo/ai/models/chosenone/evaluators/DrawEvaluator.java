package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.chosenone.RandoConfig;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.phase.DrawPhaseFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.DrawPhasePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DrawReserveLegacyReader;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: DRAW (V203 shared policy owner) ═══
// Owns ordered end-of-turn draw scoring. This adapter owns only recognition,
// bot-specific fact adaptation, and typed operation application.
// ═══════════════════════════════════════════════════════════
public class DrawEvaluator extends ActionEvaluator {

    public DrawEvaluator() {
        super("Draw");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();
        if (!"CARD_ACTION_CHOICE".equals(decisionType)
                && !"ACTION_CHOICE".equals(decisionType)) {
            return false;
        }
        if (!context.isMyTurn()) {
            logger.trace("DrawEvaluator skipping - not our turn");
            return false;
        }
        Phase phase = context.getPhase();
        if (phase != Phase.DRAW) {
            logger.trace("DrawEvaluator skipping - not draw phase (phase={})", phase);
            return false;
        }

        String decisionLower = (context.getDecisionText() != null
                ? context.getDecisionText() : "").toLowerCase();
        if (decisionLower.contains("draw") && decisionLower.contains("action")) {
            logger.debug("DrawEvaluator triggered (our turn, draw phase): '{}'",
                    context.getDecisionText());
            return true;
        }
        for (String actionText : context.getActionTexts()) {
            String actionLower = actionText.toLowerCase();
            if (actionLower.contains("draw") && !actionLower.contains("destiny")) {
                logger.debug("DrawEvaluator triggered by action: '{}'", actionText);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        Set<String> blocked = context.getBlockedResponses();
        String decisionId = context.getDecisionId();
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank() ? "draw-decision" : decisionId);

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase();
            if (!actionLower.contains("draw")) {
                continue;
            }
            if (actionLower.contains("destiny")) {
                logger.trace("Skipping destiny draw action: '{}'", actionText);
                continue;
            }

            logger.debug("Evaluating draw action: '{}' (id={})", actionText, actionId);
            EvaluatedAction action = new EvaluatedAction(
                    actionId, ActionType.DRAW, 0.0f, actionText);
            ledger.register(DrawPhasePolicy.assess(actionId, actionText,
                    blocked.contains(actionId) || blocked.contains(actionText),
                    facts(context), logger));
            PolicyOperationAdapter.apply(action, ledger);
            actions.add(action);
        }
        return actions;
    }

    private DrawPhasePolicy.Facts facts(DecisionContext context) {
        return new DrawPhasePolicy.Facts() {
            @Override public boolean hasBoardState() {
                return context.getGameState() != null;
            }

            @Override public int handSize() {
                return context.getHandSize();
            }

            @Override public int reserveDeckSize() {
                return context.getReserveDeckSize();
            }

            @Override public int usedPileSize() {
                return context.getUsedPileSize();
            }

            @Override public int forcePileSize() {
                return context.getForcePileSize();
            }

            @Override public int turnNumber() {
                return context.getTurnNumber();
            }

            @Override public int maxHandSize() {
                return RandoConfig.MAX_HAND_SIZE;
            }

            @Override public int handSoftCap() {
                return RandoConfig.HAND_SOFT_CAP;
            }

            @Override public int maintenanceObligation() {
                return context.getForceReserveFacts().maintenanceObligation;
            }

            @Override public int forceGeneration() {
                return DrawPhaseFactsReader.calculateForceGeneration(context.getGame(),
                        context.getGameState(), context.getSide(), logger);
            }

            @Override public int offensiveBank(int forcePile, int forceGeneration) {
                return DrawPhaseFactsReader.computeOffensiveBank(context.getGame(),
                        context.getGameState(), context.getPlayerId(), context.getHand(),
                        forcePile, forceGeneration, logger);
            }

            @Override public DrawPhasePolicy.HoldBack holdBack() {
                return readHoldBack(context);
            }

            @Override public DrawPhaseFactsReader.ExpensiveCards expensiveCards(int forcePile) {
                return DrawPhaseFactsReader.inspectExpensiveCards(context.getHand(), forcePile);
            }

            @Override public DrawPhaseFactsReader.ForceStarved forceStarved() {
                return DrawPhaseFactsReader.inspectForceStarved(context.getHand());
            }

            @Override public boolean piettNeedsDig() {
                return DrawEvaluator.this.piettNeedsDig(context);
            }

            @Override public int forceToReserve() {
                return calculateForceToReserve(context);
            }
        };
    }

    private int calculateForceToReserve(DecisionContext context) {
        SwccgGame game = context.getGame();
        if (game == null) {
            return 1;
        }
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        int turnNumber = context.getTurnNumber();
        return DrawReserveLegacyReader.calculate(gameState, playerId, turnNumber,
                context::getForceReserveFacts,
                () -> {
                    ObjectiveAnalyzer objective = context.getObjectiveAnalyzer();
                    return objective != null
                            ? objective
                                .getHiddenPathMoveForceReserve(
                                    game, playerId)
                            : 0;
                }, logger);
    }

    private DrawPhasePolicy.HoldBack readHoldBack(DecisionContext context) {
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        if (planner == null) {
            return DrawPhasePolicy.HoldBack.none();
        }
        DeploymentPlan plan = planner.getCurrentPlan();
        if (plan == null || plan.getStrategy() != DeployStrategy.HOLD_BACK) {
            return DrawPhasePolicy.HoldBack.none();
        }
        return new DrawPhasePolicy.HoldBack(true,
                plan.getReason() == null ? "" : plan.getReason());
    }

    private boolean piettNeedsDig(DecisionContext context) {
        DeckOracle oracle = context.getDeckOracle();
        if (oracle == null || !oracle.isAnalyzed()) {
            return false;
        }
        boolean inHand = oracle.isCardInHand("Admiral Piett")
                || oracle.isCardInHand("Piett");
        boolean inReserve = oracle.isCardInReserve("Admiral Piett")
                || oracle.isCardInReserve("Piett");
        boolean inPlay = oracle.isCardInPlay("Admiral Piett")
                || oracle.isCardInPlay("Piett");
        boolean lost = oracle.isCardLost("Admiral Piett")
                || oracle.isCardLost("Piett");
        boolean inPiles = oracle.isCardInZone("Admiral Piett", Zone.FORCE_PILE)
                || oracle.isCardInZone("Piett", Zone.FORCE_PILE)
                || oracle.isCardInZone("Admiral Piett", Zone.USED_PILE)
                || oracle.isCardInZone("Piett", Zone.USED_PILE);
        return inPiles && !inHand && !inReserve && !inPlay && !lost;
    }
}
