package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;

import java.util.ArrayList;
import java.util.List;

/** AI-only routing facts for the unchanged engine's activation decisions. */
public final class ActivateDecisionRouting {
    public static final String OPPONENT_ALLOWANCE_PROMPT =
        "Choose amount of Force to allow opponent to activate without you performing a top-level action";
    public static final String ZERO_CONFIRMATION_PROMPT =
        "You have not activated Force. Do you want to Pass?";

    private ActivateDecisionRouting() {
    }

    public static boolean isOpponentAllowancePrompt(String decisionText) {
        return OPPONENT_ALLOWANCE_PROMPT.equals(trim(decisionText));
    }

    public static ChoiceLabels zeroConfirmationChoices(String decisionType,
                                                       String decisionText,
                                                       String[] results) {
        if (!"MULTIPLE_CHOICE".equals(decisionType)
                || !ZERO_CONFIRMATION_PROMPT.equals(trim(decisionText))
                || results == null || results.length != 2) {
            return ChoiceLabels.empty();
        }

        int yesCount = 0;
        int noCount = 0;
        List<String> actionIds = new ArrayList<>(results.length);
        List<String> actionTexts = new ArrayList<>(results.length);
        for (int i = 0; i < results.length; i++) {
            String label = results[i] != null ? results[i] : "";
            if ("yes".equalsIgnoreCase(label.trim())) {
                yesCount++;
            } else if ("no".equalsIgnoreCase(label.trim())) {
                noCount++;
            }
            actionIds.add(String.valueOf(i));
            actionTexts.add(label);
        }
        return yesCount == 1 && noCount == 1
            ? new ChoiceLabels(actionIds, actionTexts)
            : ChoiceLabels.empty();
    }

    public static boolean selectedTopLevelActivate(Phase phase,
                                                   String decisionType,
                                                   String[] actionIds,
                                                   String[] actionTexts,
                                                   String selectedActionId) {
        if (phase != Phase.ACTIVATE
                || !("CARD_ACTION_CHOICE".equals(decisionType)
                    || "ACTION_CHOICE".equals(decisionType))
                || actionIds == null || actionTexts == null
                || actionIds.length != actionTexts.length
                || selectedActionId == null) {
            return false;
        }

        int selectedMatches = 0;
        boolean exactActivate = false;
        for (int i = 0; i < actionIds.length; i++) {
            if (!selectedActionId.equals(actionIds[i])) {
                continue;
            }
            selectedMatches++;
            exactActivate = "Activate Force".equals(trim(actionTexts[i]));
        }
        return selectedMatches == 1 && exactActivate;
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }

    public record ChoiceLabels(List<String> actionIds, List<String> actionTexts) {
        public ChoiceLabels {
            actionIds = List.copyOf(actionIds);
            actionTexts = List.copyOf(actionTexts);
        }

        public static ChoiceLabels empty() {
            return new ChoiceLabels(List.of(), List.of());
        }

        public boolean isPresent() {
            return !actionIds.isEmpty();
        }
    }

    /** One-shot correlation between a selected top-level action and its next decision. */
    public static final class AmountLatch {
        private boolean armed;
        private Object gameIdentity;
        private String playerId;
        private int turn;
        private Phase phase;

        public void arm(Object gameIdentity, String playerId, int turn, Phase phase) {
            if (gameIdentity == null || playerId == null || phase != Phase.ACTIVATE) {
                reset();
                return;
            }
            this.armed = true;
            this.gameIdentity = gameIdentity;
            this.playerId = playerId;
            this.turn = turn;
            this.phase = phase;
        }

        public boolean consume(Object gameIdentity, String playerId, int turn,
                               Phase phase, String decisionType) {
            boolean matches = armed
                && gameIdentity != null
                && this.gameIdentity == gameIdentity
                && java.util.Objects.equals(this.playerId, playerId)
                && this.turn == turn
                && this.phase == phase
                && "INTEGER".equals(decisionType);
            reset();
            return matches;
        }

        public void reset() {
            armed = false;
            gameIdentity = null;
            playerId = null;
            turn = 0;
            phase = null;
        }
    }
}
