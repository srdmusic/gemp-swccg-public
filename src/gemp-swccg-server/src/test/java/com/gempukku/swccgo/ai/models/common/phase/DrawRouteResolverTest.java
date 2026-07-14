package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Pure ownership matrix for the canonical top-level DRAW decision. */
public class DrawRouteResolverTest {

    private static final String DRAW =
            DecisionActionSemantic.DRAW_CARD_INTO_HAND_FROM_FORCE_PILE.name();
    private static final String UNKNOWN = DecisionActionSemantic.UNKNOWN.name();

    @Test
    public void canonicalOwnTurnCardActionWithDrawSemanticIsOwned() {
        DrawRouteInput input = input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Play another action", "Draw card into hand from Force Pile"),
                List.of(UNKNOWN, DRAW));

        assertEquals(DrawRoute.DRAW_TOP_LEVEL, DrawRouteResolver.resolve(input));
    }

    @Test
    public void wrongPhaseIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DEPLOY, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), List.of(DRAW))));
    }

    @Test
    public void opponentTurnIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, false, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), List.of(DRAW))));
    }

    @Test
    public void actionChoiceShapeIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), List.of(DRAW))));
    }

    @Test
    public void missingSemanticArrayIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), null)));
    }

    @Test
    public void blankSemanticIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), List.of("   "))));
    }

    @Test
    public void unrecognizedSemanticIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), List.of("DRAW_FROM_FORCE_PILE"))));
    }

    @Test
    public void semanticArrayMisalignedWithCandidateOrderIsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Play another action", "Draw card into hand from Force Pile"),
                List.of(DRAW))));
    }

    @Test
    public void repeatedUnknownSemanticsRemainLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Play action one", "Play action two"),
                List.of(UNKNOWN, UNKNOWN))));
    }

    @Test
    public void repeatedRecognizedDrawSemanticsAreOwned() {
        assertEquals(DrawRoute.DRAW_TOP_LEVEL, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile", "Draw another card into hand"),
                List.of(DRAW, DRAW))));
    }

    @Test
    public void destinyOnlyUnknownActionsRemainLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw battle destiny", "Draw weapon destiny"),
                List.of(UNKNOWN, UNKNOWN))));
    }

    @Test
    public void optionalResponseWithoutPhaseActionOriginRemainsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                null, Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("Draw card into hand from Force Pile"), List.of(DRAW))));
    }

    @Test
    public void failedSearchVerificationWithoutSemanticRemainsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                null, Phase.DRAW, true, AwaitingDecisionType.CARD_ACTION_CHOICE,
                List.of("No matching card was found"), null)));
    }

    @Test
    public void cardSelectionChildRemainsLegacyUnowned() {
        assertEquals(DrawRoute.LEGACY_UNOWNED, DrawRouteResolver.resolve(input(
                DecisionOrigin.PHASE_ACTION, Phase.DRAW, true,
                AwaitingDecisionType.CARD_SELECTION,
                List.of("Choose a card"), List.of(DRAW))));
    }

    private static DrawRouteInput input(Phase phase,
                                        boolean yourTurn,
                                        AwaitingDecisionType decisionType,
                                        List<String> actionTexts,
                                        List<String> actionSemantics) {
        return input(DecisionOrigin.PHASE_ACTION, phase, yourTurn, decisionType,
                actionTexts, actionSemantics);
    }

    private static DrawRouteInput input(DecisionOrigin origin,
                                        Phase phase,
                                        boolean yourTurn,
                                        AwaitingDecisionType decisionType,
                                        List<String> actionTexts,
                                        List<String> actionSemantics) {
        int candidateCount = actionTexts.size();
        return new DrawRouteInput(
                phase,
                origin,
                decisionType,
                yourTurn,
                ordinalIds(candidateCount),
                values("card", candidateCount),
                values("blueprint", candidateCount),
                actionTexts,
                values("testing", candidateCount),
                values("back", candidateCount),
                values("false", candidateCount),
                actionSemantics);
    }

    private static List<String> ordinalIds(int size) {
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(String.valueOf(i));
        }
        return result;
    }

    private static List<String> values(String prefix, int size) {
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(prefix + i);
        }
        return result;
    }
}
