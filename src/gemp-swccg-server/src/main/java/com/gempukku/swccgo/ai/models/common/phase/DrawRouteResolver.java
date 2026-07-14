package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.List;
import java.util.Objects;

/** Pure ownership resolver for the canonical top-level Force-Pile draw action. */
public final class DrawRouteResolver {

    private DrawRouteResolver() {
    }

    public static DrawRoute resolve(DrawRouteInput input) {
        Objects.requireNonNull(input, "input");
        if (input.origin() != DecisionOrigin.PHASE_ACTION
                || input.phase() != Phase.DRAW
                || !Boolean.TRUE.equals(input.yourTurn())
                || input.decisionType() != AwaitingDecisionType.CARD_ACTION_CHOICE
                || !hasCompleteCandidateMetadata(input)) {
            return DrawRoute.LEGACY_UNOWNED;
        }

        boolean hasCanonicalDraw = false;
        for (int i = 0; i < input.actionSemantics().size(); i++) {
            DecisionActionSemantic semantic = DecisionActionSemantic.fromWire(input.actionSemantics().get(i));
            if (semantic == null) {
                return DrawRoute.LEGACY_UNOWNED;
            }
            if (semantic == DecisionActionSemantic.DRAW_CARD_INTO_HAND_FROM_FORCE_PILE) {
                String actionId = input.actionIds().get(i);
                if (actionId == null || actionId.isBlank()) {
                    return DrawRoute.LEGACY_UNOWNED;
                }
                hasCanonicalDraw = true;
            }
        }
        return hasCanonicalDraw
                ? DrawRoute.DRAW_TOP_LEVEL
                : DrawRoute.LEGACY_UNOWNED;
    }

    private static boolean hasCompleteCandidateMetadata(DrawRouteInput input) {
        @SuppressWarnings("unchecked")
        List<String>[] arrays = new List[] {
                input.actionIds(), input.cardIds(), input.blueprintIds(), input.actionTexts(),
                input.testingTexts(), input.backSideTestingTexts(), input.horizontals(),
                input.actionSemantics()
        };
        int size = arrays[0] != null ? arrays[0].size() : 0;
        if (size == 0) {
            return false;
        }
        for (List<String> array : arrays) {
            if (array == null || array.size() != size) {
                return false;
            }
        }
        return true;
    }
}
