package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.PlayCardAction;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the worst-case exact Force payment for an ordinary character deploy.
 */
public final class CaptureDeployBudgetFactsReader {
    public static final String ACTION_PAYMENTS_EXTRA =
            "captureExactDeployActionPayments";

    private CaptureDeployBudgetFactsReader() {
    }

    public static Integer maximumExactNormalDeployPayment(
            SwccgGame game,
            String playerId,
            PhysicalCard card) {
        return maximumExactNormalDeployPayment(
                game, playerId, card, null);
    }

    public static Map<String, Integer>
            snapshotExactNormalDeployPayments(
            AwaitingDecision decision,
            SwccgGame game,
            String playerId) {
        if (decision == null || game == null
                || playerId == null
                || decision.getDecisionParameters() == null) {
            return Map.of();
        }
        String[] actionIds =
                decision.getDecisionParameters()
                    .get("actionId");
        if (actionIds == null) {
            return Map.of();
        }

        Map<String, Integer> payments =
                new LinkedHashMap<>();
        for (String actionId : actionIds) {
            Action action =
                    AiActionSourceProvenance.actionForId(
                        decision, actionId);
            PhysicalCard card =
                    exactNormalCharacter(action, playerId);
            if (card == null) {
                continue;
            }
            Integer payment =
                    maximumExactNormalDeployPayment(
                        game, playerId, card, action);
            if (payment != null) {
                payments.put(actionId, payment);
            }
        }
        return Map.copyOf(payments);
    }

    public static Integer actionPayment(
            Object snapshot,
            String actionId) {
        if (!(snapshot instanceof Map<?, ?> payments)
                || actionId == null) {
            return null;
        }
        Object payment = payments.get(actionId);
        return payment instanceof Integer
                ? (Integer) payment : null;
    }

    private static Integer maximumExactNormalDeployPayment(
            SwccgGame game,
            String playerId,
            PhysicalCard card,
            Action action) {
        if (game == null || playerId == null || card == null
                || !playerId.equals(card.getOwner())
                || card.getZone() != Zone.HAND
                || card.getBlueprint() == null
                || card.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }

        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            Collection<PhysicalCard> table =
                    gameState.getAllPermanentCards();
            if (table == null) {
                return null;
            }

            Integer maximum = null;
            for (PhysicalCard target : table) {
                if (target == null || target.getZone() == null
                        || !target.getZone().isInPlay()
                        || !modifiers.isDeployableToTarget(
                            gameState,
                            card,
                            card,
                            false,
                            Filters.sameCardId(target),
                            false,
                            0.0f,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                        false,
                        0.0f)) {
                    continue;
                }
                if (action != null
                        && card.getBlueprint()
                            .getSpecialDeployCostEffect(
                                action, playerId, game,
                                card, target, null) != null) {
                    return null;
                }

                float base = modifiers.getDeployCost(
                        gameState,
                        card,
                        card,
                        target,
                        false,
                        null,
                        false,
                        0.0f,
                        null,
                        false);
                if (!Float.isFinite(base)) {
                    return null;
                }
                int extra =
                        modifiers
                            .getExtraForceRequiredToDeployToTarget(
                                gameState,
                                card,
                                target,
                                null,
                                card,
                                false);
                double payment = Math.ceil(base) + extra;
                if (!Double.isFinite(payment)
                        || payment < 0
                        || payment > Integer.MAX_VALUE) {
                    return null;
                }
                int exactPayment = (int) payment;
                maximum = maximum == null
                        ? exactPayment
                        : Math.max(maximum, exactPayment);
            }
            return maximum;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PhysicalCard exactNormalCharacter(
            Action action,
            String playerId) {
        if (!(action instanceof PlayCardAction play)
                || !"PlayCharacterAction".equals(
                    action.getClass().getSimpleName())
                || !"Deploy".equals(action.getText())
                || play.getOtherPlayedCard() != null
                || play.getPlayingFromZone() != Zone.HAND) {
            return null;
        }
        PhysicalCard card = play.getPlayedCard();
        if (card == null || action.getActionSource() != card
                || !playerId.equals(card.getOwner())
                || card.getBlueprint() == null
                || card.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER) {
            return null;
        }
        try {
            return !field(action, "_forFree", Boolean.class)
                    && Float.compare(
                        field(action, "_changeInCost",
                            Float.class),
                        0.0f) == 0
                    && field(action, "_reactActionOption",
                        Object.class) == null
                    ? card : null;
        } catch (ReflectiveOperationException
                | RuntimeException ignored) {
            return null;
        }
    }

    private static <T> T field(
            Action action,
            String name,
            Class<T> type)
            throws ReflectiveOperationException {
        Field field =
                action.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(action));
    }
}
