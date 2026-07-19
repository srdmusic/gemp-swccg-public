package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.PassPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: SVC-SAFETY (adjacent — pass-side Force reservations) (reorg 2026-07-06) ═══
// Owns: Pass baseline scoring + pass-side Force reservations: V27 battle-interrupt reserve,
// V27.1 Draw Their Fire tax reservation, V37.4 deploy-phase pass penalty / penalty-reduction.
// Hub: common/phase/PassPolicy. KIND mix: BANDED only, small magnitudes — the Pass baseline (~5-8) is a boundary
// other sections deliberately score against (e.g. V61c lands BELOW Pass by design).
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: SVC-SAFETY (V148 all-bad pass in DecisionSafety; NO-PASS damage-segment context),
// DEPLOY-1 (V37.4 pairs with DeployEvaluator), ACTIVATE (V61c/V168 boundary). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
/**
 * Pass Evaluator
 *
 * Simple evaluator that creates a PASS action.
 * Ported from Python base.py PassEvaluator (~210 lines)
 *
 * Used when we want to pass/cancel instead of taking an action.
 * Score is typically low (5-10) unless we really want to pass.
 *
 * IMPORTANT: For ACTION_CHOICE decisions, empty string may not be valid!
 * We need to find a "Cancel" or "Done" action from available options instead.
 */
public class PassEvaluator extends ActionEvaluator {

    // Priority keywords for finding cancel actions (in order of preference)
    private static final String[] CANCEL_KEYWORDS = {
        "cancel", "done", "pass", "decline", "no response", "no further"
    };

    public PassEvaluator() {
        super("Pass");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // V194: INTEGER responses are values. An empty pass response silently means zero.
        if ("INTEGER".equals(context.getDecisionType())) {
            return false;
        }

        // Can only pass if:
        // 1. noPass=false (passing is allowed)
        // 2. AND min=0 (no minimum selection required)
        // 3. For "Required responses", only pass if there's an explicit cancel action
        int minRequired = context.getMin();

        // Basic requirement: noPass must be false and no minimum selection
        if (context.isNoPass() || minRequired > 0) {
            return false;
        }

        // Check for "Required responses" in decision text
        String decisionText = context.getDecisionText();
        if (decisionText != null && decisionText.toLowerCase().contains("required")) {
            // Only allow "passing" if there's a cancel action we can select
            String cancelId = findCancelAction(context);
            if (cancelId == null) {
                return false;  // No cancel option = can't pass on required responses
            }
        }

        return true;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        // For ACTION_CHOICE, we may need to use a "Cancel" action instead of empty string
        String passActionId = "";
        String passDisplay = "Pass / Do nothing";

        // Check if this is an ACTION_CHOICE decision with available actions
        if ("ACTION_CHOICE".equals(context.getDecisionType()) && !context.getActionTexts().isEmpty()) {
            String cancelId = findCancelAction(context);
            if (cancelId != null) {
                passActionId = cancelId;
                // Find the display text for this action
                List<String> actionIds = context.getActionIds();
                List<String> actionTexts = context.getActionTexts();
                for (int i = 0; i < actionIds.size(); i++) {
                    if (actionIds.get(i).equals(cancelId) && i < actionTexts.size()) {
                        passDisplay = "Cancel: " + actionTexts.get(i);
                        break;
                    }
                }
                logger.trace("ACTION_CHOICE: Using cancel action '{}' instead of empty string", cancelId);
            }
        }

        EvaluatedAction action = new EvaluatedAction(
                passActionId, ActionType.PASS, PassPolicy.BASE_SCORE, passDisplay,
                PassPolicy.BASE_RULE_ID, PassPolicy.BASE_DOMAIN_ID,
                PassPolicy.BASE_OUTPUT_KIND, PassPolicy.BASE_REASON);

        // Get game state for resource checks
        GameState gameState = context.getGameState();
        String decisionTextLower = (context.getDecisionText() != null ? context.getDecisionText() : "").toLowerCase();
        Phase phase = context.getPhase();
        int turnNumber = context.getTurnNumber();

        boolean isActivateDecision = decisionTextLower.contains("activate");
        boolean isDrawDecision = decisionTextLower.contains("draw") && decisionTextLower.contains("action");
        boolean isControlDecision = phase == Phase.CONTROL && decisionTextLower.contains("control action");
        boolean isInitiateBattleDecision = decisionTextLower.contains("initiate battle");
        boolean isBattlePhaseAction = phase == Phase.BATTLE && decisionTextLower.contains("battle action");
        boolean isFollowthroughDecision = decisionTextLower.contains("choose where to move") ||
                                           decisionTextLower.contains("choose where to deploy");
        boolean terminalDecision = isInitiateBattleDecision || isBattlePhaseAction
                || isFollowthroughDecision;

        int forcePile = 0;
        int reserveDeck = 0;
        int handSize = 0;
        boolean dtfActive = false;
        int maintenanceObligation = 0;
        if (gameState != null && !terminalDecision) {
            forcePile = context.getForcePileSize();
            reserveDeck = context.getReserveDeckSize();
            handSize = context.getHandSize();
            if (!isActivateDecision && !isInitiateBattleDecision) {
                try {
                    dtfActive = context.getForceReserveFacts().dtfActive;
                } catch (Exception e) {
                    // Preserve the legacy best-effort reserve check.
                }
            }
            if (!isActivateDecision) {
                try {
                    maintenanceObligation = context.getForceReserveFacts().maintenanceObligation;
                } catch (Exception e) {
                    // Preserve the legacy best-effort maintenance check.
                }
            }
        }

        PolicyResult passResult = PassPolicy.evaluate(new PassPolicy.Facts(
                passActionId, turnNumber, phase, isActivateDecision, isDrawDecision,
                isControlDecision, isInitiateBattleDecision, isBattlePhaseAction,
                isFollowthroughDecision, gameState != null, forcePile, reserveDeck,
                handSize, dtfActive, maintenanceObligation));
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "pass-" + passActionId);
        ledger.register(passResult);
        PolicyOperationAdapter.apply(action, ledger);

        for (PolicyOperation operation : passResult.operations()) {
            if (operation.ruleArmId().id().equals("V37.4-pass")) {
                logger.warn("V37.4 HAND BLOAT: hand={}, force={} — pass penalty {}",
                        handSize, forcePile, (int) operation.delta());
            }
        }

        actions.add(action);
        return actions;
    }

    /**
     * Find a "Cancel" or "Done" action from available actions.
     *
     * For ACTION_CHOICE decisions, we can't use empty string to pass.
     * Instead, we need to find and select a cancel/done action.
     *
     * @return the action_id of the cancel action, or null if not found
     */
    private String findCancelAction(DecisionContext context) {
        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();

        // Priority 1: Actions that START with cancel/done keywords
        for (int i = 0; i < actionTexts.size(); i++) {
            String textLower = actionTexts.get(i).toLowerCase().trim();
            for (String keyword : CANCEL_KEYWORDS) {
                if (textLower.startsWith(keyword)) {
                    if (i < actionIds.size()) {
                        return actionIds.get(i);
                    }
                }
            }
        }

        // Priority 2: Actions that contain "- cancel" or "- done" patterns
        for (int i = 0; i < actionTexts.size(); i++) {
            String textLower = actionTexts.get(i).toLowerCase();
            if (textLower.contains(" - cancel") || textLower.contains(" - done") || textLower.contains(" - no ")) {
                if (i < actionIds.size()) {
                    return actionIds.get(i);
                }
            }
        }

        // Priority 3: Actions that start with keyword and don't have " or "
        for (int i = 0; i < actionTexts.size(); i++) {
            String textLower = actionTexts.get(i).toLowerCase().trim();
            for (String keyword : CANCEL_KEYWORDS) {
                if (textLower.startsWith(keyword) && !textLower.contains(" or ")) {
                    if (i < actionIds.size()) {
                        return actionIds.get(i);
                    }
                }
            }
        }

        return null;
    }
}
