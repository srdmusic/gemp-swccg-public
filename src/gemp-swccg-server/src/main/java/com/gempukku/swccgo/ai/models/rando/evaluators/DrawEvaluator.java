package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Draw Evaluator
 *
 * Handles card draw decisions.
 * Ported from Python draw_evaluator.py (~620 lines)
 *
 * Decision factors:
 * - Hand size (target ~7-8 cards, soft cap 12, hard cap 16)
 * - Reserve deck status (don't deck out)
 * - Force pile available
 * - Strategy (deploy vs hold)
 * - Force generation deficit (draw to find locations if low gen)
 * - Future turn planning (save force for expensive cards)
 * - Late-game life force preservation
 */
public class DrawEvaluator extends ActionEvaluator {

    // Rank deltas
    private static final float VERY_GOOD_DELTA = 150.0f;
    private static final float GOOD_DELTA = 10.0f;
    private static final float BAD_DELTA = -10.0f;
    private static final float VERY_BAD_DELTA = -150.0f;

    // Draw thresholds
    private static final int TARGET_HAND_SIZE = 7;
    private static final int LOW_RESERVE_THRESHOLD = 6;
    private static final int SMALL_HAND_THRESHOLD = 5;
    private static final int AGGRESSIVE_FORCE_THRESHOLD = 10;
    private static final int LATE_GAME_LIFE_FORCE = 12;
    private static final int CRITICAL_LIFE_FORCE = 6;

    public DrawEvaluator() {
        super("Draw");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();

        // Only handle CARD_ACTION_CHOICE or ACTION_CHOICE
        if (!"CARD_ACTION_CHOICE".equals(decisionType) && !"ACTION_CHOICE".equals(decisionType)) {
            return false;
        }

        // CRITICAL: Only evaluate during OUR turn
        if (!context.isMyTurn()) {
            logger.trace("DrawEvaluator skipping - not our turn");
            return false;
        }

        // CRITICAL: Only evaluate during Draw phase
        // "Draw destiny" is NOT drawing cards - it's a random number mechanic
        Phase phase = context.getPhase();
        if (phase != Phase.DRAW) {
            logger.trace("DrawEvaluator skipping - not draw phase (phase={})", phase);
            return false;
        }

        // Check for draw actions in the decision
        String decisionLower = (context.getDecisionText() != null ? context.getDecisionText() : "").toLowerCase();
        if (decisionLower.contains("draw") && decisionLower.contains("action")) {
            logger.debug("DrawEvaluator triggered (our turn, draw phase): '{}'", context.getDecisionText());
            return true;
        }

        // Check if any action is a draw action (but not destiny draw)
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

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase();

            // Only evaluate draw actions (not destiny draws)
            if (!actionLower.contains("draw")) {
                continue;
            }
            if (actionLower.contains("destiny")) {
                logger.trace("Skipping destiny draw action: '{}'", actionText);
                continue;
            }

            logger.debug("Evaluating draw action: '{}' (id={})", actionText, actionId);

            EvaluatedAction action = new EvaluatedAction(actionId, ActionType.DRAW, 0.0f, actionText);

            // Check if this response is blocked (loop prevention)
            if (blocked.contains(actionId) || blocked.contains(actionText)) {
                action.addReasoning("BLOCKED (loop prevention)", -200.0f);
            }

            // Rank the draw action
            rankDrawAction(action, context);

            actions.add(action);
        }

        return actions;
    }

    /**
     * Rank the draw action based on game state.
     *
     * CRITICAL: When life force is low, reduce max hand size proportionally.
     * Having 12 cards in hand but only 2 force to spend is TERRIBLE.
     */
    private void rankDrawAction(EvaluatedAction action, DecisionContext context) {
        GameState gameState = context.getGameState();
        if (gameState == null) {
            action.addReasoning("No board state - neutral draw", 0.0f);
            return;
        }

        String playerId = context.getPlayerId();
        int handSize = context.getHandSize();
        int reserveDeck = context.getReserveDeckSize();
        int usedPile = context.getUsedPileSize();
        int forcePile = context.getForcePileSize();
        int turnNumber = context.getTurnNumber();

        // Total remaining life force
        int remainingLifeForce = reserveDeck + usedPile + forcePile;

        // === CRITICAL: LIFE FORCE BASED HAND LIMIT ===
        if (remainingLifeForce < CRITICAL_LIFE_FORCE) {
            float penalty = VERY_BAD_DELTA * 0.8f;
            action.addReasoning("CRITICAL life force (" + remainingLifeForce + ") - minimize draws", penalty);
            // Still allow draws if hand is truly empty
            if (handSize >= 2) {
                return;
            }
        }

        // Late game - be more strategic
        if (remainingLifeForce < LATE_GAME_LIFE_FORCE) {
            float penaltyScale = (LATE_GAME_LIFE_FORCE - remainingLifeForce) / (float) LATE_GAME_LIFE_FORCE;
            action.addReasoning("Late game (" + remainingLifeForce + " life force) - draw carefully",
                               BAD_DELTA * 2 * penaltyScale);
        }

        // Effective max hand based on life force
        int effectiveMaxHand = RandoConfig.MAX_HAND_SIZE;
        if (remainingLifeForce < RandoConfig.MAX_HAND_SIZE) {
            effectiveMaxHand = Math.max(2, remainingLifeForce);
            logger.trace("Life force {} < {}: effective max hand = {}",
                        remainingLifeForce, RandoConfig.MAX_HAND_SIZE, effectiveMaxHand);
        }

        // If hand already exceeds effective max, STRONGLY penalize drawing
        if (handSize >= effectiveMaxHand) {
            action.addReasoning("CRITICAL: Hand " + handSize + " >= life force limit " + effectiveMaxHand, VERY_BAD_DELTA);
            return;
        }

        // === BASELINE: DRAW TOWARDS DYNAMIC SOFT CAP ===
        String phaseNote = turnNumber <= 3 ? "early" : (turnNumber <= 6 ? "mid" : "late");

        // Dynamic soft cap based on turn
        int effectiveSoftCap;
        if (turnNumber <= 3) {
            effectiveSoftCap = RandoConfig.HAND_SOFT_CAP + 4;  // 16
        } else if (turnNumber <= 6) {
            effectiveSoftCap = RandoConfig.HAND_SOFT_CAP;  // 12
        } else {
            effectiveSoftCap = RandoConfig.HAND_SOFT_CAP - 4;  // 8
        }

        if (handSize < effectiveSoftCap && remainingLifeForce >= LATE_GAME_LIFE_FORCE) {
            int cardsBelowCap = effectiveSoftCap - handSize;
            // Strong baseline bonus for drawing toward cap
            float baselineBonus = Math.max(30.0f, 8.0f * cardsBelowCap);
            action.addReasoning("Hand " + handSize + " below " + phaseNote + "-game cap " + effectiveSoftCap + " - draw!",
                               baselineBonus);
            logger.debug("Draw baseline: hand {} < {} cap {}, +{}", handSize, phaseNote, effectiveSoftCap, baselineBonus);
        }

        // === FORCE RESERVATION FOR OPPONENT'S TURN ===
        int forceToReserve = handSize < 6 ? 1 : 2;
        if (turnNumber >= 4) {
            if (forcePile <= forceToReserve) {
                action.addReasoning("Turn " + turnNumber + ": reserve " + forceToReserve + " force for reactions",
                                   BAD_DELTA * 1.5f);
            }
        }

        // Don't draw if low reserve (avoid decking)
        if (reserveDeck <= LOW_RESERVE_THRESHOLD) {
            float penalty = BAD_DELTA * (LOW_RESERVE_THRESHOLD - reserveDeck);
            action.addReasoning("Low reserve (" + reserveDeck + ") - avoid drawing", penalty);
        }

        // Draw if hand is smaller than target and enough reserve
        if (handSize < TARGET_HAND_SIZE && reserveDeck > 10 && forcePile > 1) {
            if (remainingLifeForce >= LATE_GAME_LIFE_FORCE) {
                action.addReasoning("Hand size " + handSize + " < " + TARGET_HAND_SIZE + " - draw to fill", GOOD_DELTA);
            }
        }

        // Draw if hand is very small
        if (handSize <= SMALL_HAND_THRESHOLD && reserveDeck > 4 && forcePile > 1) {
            action.addReasoning("Small hand (" + handSize + ") - draw cards", GOOD_DELTA);
        }

        // Aggressive draw if high force and good life force
        if (forcePile > AGGRESSIVE_FORCE_THRESHOLD && remainingLifeForce >= LATE_GAME_LIFE_FORCE) {
            action.addReasoning("High force pile (" + forcePile + ") - YOLO draw", GOOD_DELTA);
        }

        // Weak hand - draw even on hold
        if (forcePile > 5 && handSize <= 4) {
            action.addReasoning("Weak hand - draw even on hold", GOOD_DELTA);
        }

        // Hand size penalty for exceeding soft cap
        if (handSize >= effectiveSoftCap) {
            int overflow = handSize - effectiveSoftCap;
            action.addReasoning("Hand above " + phaseNote + "-game cap (" + handSize + "/" + effectiveSoftCap + ")",
                               BAD_DELTA * overflow * 0.5f);
        }

        // Save last force
        if (forcePile == 1) {
            action.addReasoning("Last force - save it", BAD_DELTA);
        }
    }
}
