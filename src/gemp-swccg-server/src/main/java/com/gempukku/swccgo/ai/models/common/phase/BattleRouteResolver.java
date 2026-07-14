package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure BATTLE resolver. Phase and prompt text never establish ownership. */
public final class BattleRouteResolver {
    private BattleRouteResolver() {
    }

    public static BattleWindowRoute resolve(BattleRouteInput input) {
        Objects.requireNonNull(input, "input");
        DecisionOrigin origin = singletonOrigin(input.originValues());
        if (origin == null || input.decisionType() == null
                || !origin.requiredWireTypeName().equals(input.decisionType().name())) {
            return BattleWindowRoute.LEGACY_UNOWNED;
        }
        return switch (origin) {
            case PHASE_ACTION -> validInitiationChoice(input)
                    ? BattleWindowRoute.INITIATE : BattleWindowRoute.LEGACY_UNOWNED;
            case BATTLE_ACTION -> validBattleActionChoice(input)
                    ? routeBattleActions(input) : BattleWindowRoute.LEGACY_UNOWNED;
            case BATTLE_FORFEIT -> validForfeitChoice(input)
                    ? BattleWindowRoute.TACTIC : BattleWindowRoute.LEGACY_UNOWNED;
            case BATTLE_POWER -> validChoice(input)
                    ? BattleWindowRoute.ADD_DESTINY : BattleWindowRoute.LEGACY_UNOWNED;
            case BATTLE_DESTINY_SELECTION -> validCardChoice(input)
                    ? BattleWindowRoute.TACTIC : BattleWindowRoute.LEGACY_UNOWNED;
            default -> BattleWindowRoute.LEGACY_UNOWNED;
        };
    }

    private static boolean validInitiationChoice(BattleRouteInput input) {
        return input.phase() == Phase.BATTLE
                && input.decisionType() == AwaitingDecisionType.CARD_ACTION_CHOICE
                && validActionArrays(input)
                && input.actionSemantics().stream()
                    .map(DecisionActionSemantic::fromWire)
                    .anyMatch(DecisionActionSemantic.BATTLE_INITIATE::equals);
    }

    private static boolean validBattleActionChoice(BattleRouteInput input) {
        return input.phase() == Phase.BATTLE
                && input.decisionType() == AwaitingDecisionType.CARD_ACTION_CHOICE
                && validActionArrays(input);
    }

    private static BattleWindowRoute routeBattleActions(BattleRouteInput input) {
        return input.actionSemantics().stream()
                .map(DecisionActionSemantic::fromWire)
                .anyMatch(DecisionActionSemantic.BATTLE_FIRE::equals)
                ? BattleWindowRoute.FIRE : BattleWindowRoute.TACTIC;
    }

    private static boolean validActionArrays(BattleRouteInput input) {
        int size = size(input.actionIds());
        if (size == 0 || size(input.actionSemantics()) != size
                || size(input.cardIds()) != size) {
            return false;
        }
        Set<String> actionIds = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (blank(input.actionIds().get(i))
                    || !actionIds.add(input.actionIds().get(i))
                    || blank(input.cardIds().get(i))
                    || DecisionActionSemantic.fromWire(input.actionSemantics().get(i)) == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean validForfeitChoice(BattleRouteInput input) {
        return input.phase() == Phase.BATTLE
                && input.decisionType() == AwaitingDecisionType.CARD_SELECTION
                && validCardChoice(input)
                && singletonBoolean(input.optionalImmuneForfeitValues()) != null;
    }

    private static boolean validCardChoice(BattleRouteInput input) {
        if (input.phase() != Phase.BATTLE || size(input.cardIds()) == 0) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (String id : input.cardIds()) {
            if (blank(id) || !ids.add(id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validChoice(BattleRouteInput input) {
        return input.phase() == Phase.BATTLE
                && input.decisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                && size(input.results()) >= 2;
    }

    static DecisionOrigin singletonOrigin(List<String> values) {
        return size(values) == 1 ? DecisionOrigin.fromWire(values.get(0)) : null;
    }

    static Boolean singletonBoolean(List<String> values) {
        if (size(values) != 1) {
            return null;
        }
        if ("true".equalsIgnoreCase(values.get(0))) {
            return true;
        }
        if ("false".equalsIgnoreCase(values.get(0))) {
            return false;
        }
        return null;
    }

    private static int size(List<?> values) {
        return values != null ? values.size() : 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
