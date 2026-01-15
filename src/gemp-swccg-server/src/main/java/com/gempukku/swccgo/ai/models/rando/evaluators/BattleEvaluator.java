package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates battle initiation decisions.
 *
 * Decision factors (from Python battle_evaluator.py):
 * - Power differential (my power - their power)
 * - Reserve deck (need cards for destiny draws)
 * - Strategic situation (ahead/behind on board/life force)
 *
 * Threat levels (conservative thresholds to account for attrition):
 * - CRUSH: Power advantage 8+ -> definitely battle
 * - FAVORABLE: Power advantage 6-7 -> battle recommended
 * - MARGINAL: Power advantage 4-5 -> battle if no weapons
 * - RISKY: Power diff 0 to +3 -> avoid unless necessary
 * - DANGEROUS: Power disadvantage -> avoid/retreat
 *
 * Ported from Python battle_evaluator.py
 */
public class BattleEvaluator extends ActionEvaluator {

    // Battle thresholds
    private static final int CRUSH_THRESHOLD = 8;
    private static final int FAVORABLE_THRESHOLD = 6;
    private static final int MARGINAL_THRESHOLD = 4;
    private static final int RISKY_THRESHOLD = 0;

    // Minimum reserve deck for destiny draws
    private static final int MIN_RESERVE_FOR_BATTLE = 3;

    public BattleEvaluator() {
        super("Battle");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // Handle CARD_ACTION_CHOICE with battle-related actions
        if (!"CARD_ACTION_CHOICE".equals(context.getDecisionType())) {
            return false;
        }

        // Check for battle phase or battle-related decision
        Phase phase = context.getPhase();
        String decisionText = context.getDecisionText();
        String decisionLower = decisionText != null ? decisionText.toLowerCase(Locale.ROOT) : "";

        // During battle phase
        if (phase == Phase.BATTLE) {
            return true;
        }

        // Or if decision text mentions battle/initiate
        if (decisionLower.contains("battle") || decisionLower.contains("initiate")) {
            return true;
        }

        // Check if any action mentions battle
        List<String> actionTexts = context.getActionTexts();
        if (actionTexts != null) {
            for (String actionText : actionTexts) {
                if (actionText != null) {
                    String actionLower = actionText.toLowerCase(Locale.ROOT);
                    if (actionLower.contains("initiate battle") || actionLower.contains("battle")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        logger.info("[BattleEvaluator] Evaluating battle decision");

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();

        if (actionIds == null || actionTexts == null) {
            logger.warn("[BattleEvaluator] No action IDs or texts available");
            return actions;
        }

        logger.debug("[BattleEvaluator] Phase={}, actions={}", context.getPhase(), actionIds.size());

        // Get game state info
        int reserveDeck = context.getReserveDeckSize();
        int lifeForce = context.getLifeForce();
        int forcePile = context.getForcePileSize();

        // Calculate board position
        boolean isBehindOnLifeForce = false;
        boolean isAheadOnLifeForce = false;
        if (gameState != null) {
            String playerId = context.getPlayerId();
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId != null) {
                int opponentLifeForce = gameState.getPlayerLifeForce(opponentId);
                isBehindOnLifeForce = lifeForce < opponentLifeForce - 5;
                isAheadOnLifeForce = lifeForce > opponentLifeForce + 5;
            }
        }

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle battle-related actions
            if (!actionLower.contains("battle") && !actionLower.contains("fire")) {
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.BATTLE,
                50.0f,  // Base score
                actionText
            );

            // === INITIATE BATTLE SCORING ===
            if (actionLower.contains("initiate battle")) {
                // Check if we have enough reserve for destiny draws
                if (reserveDeck < MIN_RESERVE_FOR_BATTLE) {
                    action.addReasoning(
                        String.format("Low reserve deck (%d) - risky destiny draws", reserveDeck),
                        -50.0f
                    );
                }

                // Strategic position adjustments
                if (isBehindOnLifeForce) {
                    // When behind, need to battle to catch up
                    action.addReasoning("Behind on life force - must be aggressive!", 40.0f);
                } else if (isAheadOnLifeForce) {
                    // When ahead, be more conservative
                    action.addReasoning("Ahead on life force - can afford to wait", -20.0f);
                }

                // Life force critical - must battle
                if (lifeForce <= RandoConfig.CRITICAL_LIFE_FORCE) {
                    action.addReasoning("CRITICAL life force - must battle!", 60.0f);
                }

                // Default encouragement to battle (we're here to fight!)
                action.addReasoning("Battle opportunity", 30.0f);
            }

            // === WEAPON FIRING ===
            if (actionLower.contains("fire")) {
                action.addReasoning("Fire weapon", 40.0f);

                // Target selection bonuses
                if (actionLower.contains("character")) {
                    action.addReasoning("Target character", 10.0f);
                }
                if (actionLower.contains("unique") || actionLower.contains("•")) {
                    action.addReasoning("Target unique card", 20.0f);
                }
            }

            // === BATTLE TACTICS (during battle) ===
            if (context.getPhase() == Phase.BATTLE) {
                // Fire before forfeit
                if (actionLower.contains("fire")) {
                    action.addReasoning("Fire weapons during battle", 50.0f);
                }

                // Draw battle destiny
                if (actionLower.contains("draw") && actionLower.contains("destiny")) {
                    action.addReasoning("Draw battle destiny", 30.0f);
                }
            }

            logger.debug("[BattleEvaluator] Scored '{}' -> {}",
                actionText.length() > 40 ? actionText.substring(0, 40) + "..." : actionText,
                String.format("%.1f", action.getScore()));

            actions.add(action);
        }

        logger.info("[BattleEvaluator] Evaluated {} battle actions", actions.size());
        return actions;
    }
}
