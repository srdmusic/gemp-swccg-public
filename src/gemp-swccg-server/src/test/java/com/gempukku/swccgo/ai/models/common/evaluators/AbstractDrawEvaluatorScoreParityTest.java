package com.gempukku.swccgo.ai.models.common.evaluators;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Shared score and operation contract for the duplicated DRAW evaluator stacks. */
public abstract class AbstractDrawEvaluatorScoreParityTest {

    protected static final String CANONICAL_DRAW_TEXT = "Draw card into hand from Force Pile";
    protected static final float PASS_SCORE = 2.0f;

    protected record Candidate(String actionId, String actionText) {
    }

    protected record Scenario(List<Candidate> candidates, Set<String> blockedResponses,
                              boolean noPass, boolean includeActionText,
                              boolean includePass, Float drawAdjustment) {
    }

    public record Captured(String winnerActionId, int winnerScoreBits,
                           boolean winnerHardVetoed, String winnerReasoning,
                           DecisionTrace trace) {
    }

    private record ExpectedOp(TraceOp op, int candidateOrdinal, String actionId,
                              String evaluatorId, Integer beforeBits, Integer deltaBits,
                              Integer afterBits, boolean vetoed) {
    }

    protected abstract Captured runScenario(Scenario scenario);

    @Test
    public void blockedCanonicalDrawKeepsBothLegacyPenaltiesWithoutHardVeto() {
        Captured captured = capture(scenario(
            List.of(draw("draw")), Set.of("draw"), true, true, false, null));

        assertEquals(bits(-100200.0f), captured.winnerScoreBits());
        assertFalse("legacy score block is not an EvaluatedAction hard veto",
            captured.winnerHardVetoed());
        assertTrue(captured.winnerReasoning().contains("-200.0"));
        assertTrue(captured.winnerReasoning().contains("-100000.0"));

        assertEquals(List.of(
            op(TraceOp.INITIAL, 0, "draw", "Draw", null, null, 0.0f, false),
            op(TraceOp.ADD, 0, "draw", "Draw", 0.0f, -200.0f, -200.0f, false),
            op(TraceOp.ADD, 0, "draw", "Draw", -200.0f, 0.0f, -200.0f, false),
            op(TraceOp.INITIAL, 0, "draw", "ActionText", null, null, 0.0f, false),
            op(TraceOp.ADD, 0, "draw", "ActionText", 0.0f, -100000.0f, -100000.0f, false),
            op(TraceOp.MERGE, 0, "draw", "ActionText", -200.0f, null, -100200.0f, false),
            op(TraceOp.RANK, 0, "draw", TraceOperation.PRODUCER_COMBINED_EVALUATOR,
                null, null, -100200.0f, false),
            op(TraceOp.SELECT, 0, "draw", TraceOperation.PRODUCER_COMBINED_EVALUATOR,
                null, null, -100200.0f, false)
        ), observedOps(captured.trace()));
    }

    @Test
    public void exactDrawTiePreservesFirstOfferedCandidate() {
        Captured first = capture(scenario(
            List.of(draw("draw-a"), draw("draw-b")), Set.of(), true, false, false, null));
        Captured reversed = capture(scenario(
            List.of(draw("draw-b"), draw("draw-a")), Set.of(), true, false, false, null));

        assertEquals("draw-a", first.winnerActionId());
        assertEquals("draw-b", reversed.winnerActionId());
        assertEquals(bits(0.0f), first.winnerScoreBits());
        assertEquals(List.of("draw-a", "draw-b"), first.trace().getRawCandidateOrder());
        assertEquals(List.of("draw-a", "draw-b"), first.trace().getMergeOrder());
    }

    @Test
    public void passEligibilityControlsNeutralDrawBoundary() {
        Captured passable = capture(scenario(
            List.of(draw("draw")), Set.of(), false, false, true, null));
        Captured noPass = capture(scenario(
            List.of(draw("draw")), Set.of(), true, false, true, null));

        assertEquals("", passable.winnerActionId());
        assertEquals(bits(PASS_SCORE), passable.winnerScoreBits());
        assertEquals(Boolean.TRUE, passable.trace().getFinalization().passEligible());
        assertEquals(List.of("draw", ""), passable.trace().getMergeOrder());

        assertEquals("draw", noPass.winnerActionId());
        assertEquals(bits(0.0f), noPass.winnerScoreBits());
        assertEquals(Boolean.FALSE, noPass.trace().getFinalization().passEligible());
        assertEquals(List.of("draw"), noPass.trace().getMergeOrder());
    }

    @Test
    public void oneUlpAbovePassIsTheSmallestWinningDrawMargin() {
        float justBelowPass = Math.nextDown(PASS_SCORE);
        float smallestWinningScore = Math.nextUp(PASS_SCORE);
        Captured below = capture(scenario(
            List.of(draw("draw")), Set.of(), false, false, true, justBelowPass));
        Captured tied = capture(scenario(
            List.of(draw("draw")), Set.of(), false, false, true, PASS_SCORE));
        Captured above = capture(scenario(
            List.of(draw("draw")), Set.of(), false, false, true, smallestWinningScore));

        assertEquals("", below.winnerActionId());
        assertEquals("draw", tied.winnerActionId());
        assertEquals(bits(PASS_SCORE), tied.winnerScoreBits());
        assertEquals("draw", above.winnerActionId());
        assertEquals(bits(smallestWinningScore), above.winnerScoreBits());
        assertEquals(bits(PASS_SCORE), bits(Math.nextDown(smallestWinningScore)));
        assertEquals(Boolean.TRUE, above.trace().getFinalization().passEligible());
    }

    @Test
    public void mixedDrawDecisionRetainsCompleteContributionOrder() {
        Captured captured = capture(scenario(
            List.of(draw("draw"), new Candidate("other", "Other action")),
            Set.of(), false, true, true, null));

        assertEquals("", captured.winnerActionId());
        assertEquals(bits(PASS_SCORE), captured.winnerScoreBits());
        assertEquals(List.of("draw", "other"), captured.trace().getRawCandidateOrder());
        assertEquals(List.of("draw", "other", ""), captured.trace().getMergeOrder());
        assertEquals(List.of(
            op(TraceOp.INITIAL, 0, "draw", "Draw", null, null, 0.0f, false),
            op(TraceOp.ADD, 0, "draw", "Draw", 0.0f, 0.0f, 0.0f, false),
            op(TraceOp.INITIAL, 0, "draw", "ActionText", null, null, 0.0f, false),
            op(TraceOp.ADD, 0, "draw", "ActionText", 0.0f, 0.0f, 0.0f, false),
            op(TraceOp.INITIAL, 1, "other", "ActionText", null, null, 0.0f, false),
            op(TraceOp.ADD, 1, "other", "ActionText", 0.0f, 0.0f, 0.0f, false),
            op(TraceOp.MERGE, 0, "draw", "ActionText", 0.0f, null, 0.0f, false),
            op(TraceOp.INITIAL, TraceOperation.ORDINAL_UNKNOWN, "", "Pass",
                null, null, 5.0f, false),
            op(TraceOp.ADD, TraceOperation.ORDINAL_UNKNOWN, "", "Pass",
                5.0f, 0.0f, 5.0f, false),
            op(TraceOp.ADD, TraceOperation.ORDINAL_UNKNOWN, "", "Pass",
                5.0f, -3.0f, PASS_SCORE, false),
            op(TraceOp.RANK, TraceOperation.ORDINAL_UNKNOWN, "",
                TraceOperation.PRODUCER_COMBINED_EVALUATOR,
                null, null, PASS_SCORE, false),
            op(TraceOp.SELECT, TraceOperation.ORDINAL_UNKNOWN, "",
                TraceOperation.PRODUCER_COMBINED_EVALUATOR,
                null, null, PASS_SCORE, false)
        ), observedOps(captured.trace()));
    }

    private Captured capture(Scenario scenario) {
        Captured captured = runScenario(scenario);
        assertNotNull(captured);
        assertNotNull(captured.trace());
        assertEquals(TraceStatus.COMPLETE, captured.trace().getStatus());
        assertEquals(TraceRoute.COMBINED_EVALUATOR, captured.trace().getRoute().selected());
        assertFalse("CombinedEvaluator seam must close its trace session", TraceSession.isActive());
        return captured;
    }

    private static Candidate draw(String actionId) {
        return new Candidate(actionId, CANONICAL_DRAW_TEXT);
    }

    private static Scenario scenario(List<Candidate> candidates, Set<String> blockedResponses,
                                     boolean noPass, boolean includeActionText,
                                     boolean includePass, Float drawAdjustment) {
        return new Scenario(List.copyOf(candidates), Set.copyOf(blockedResponses), noPass,
            includeActionText, includePass, drawAdjustment);
    }

    private static List<ExpectedOp> observedOps(DecisionTrace trace) {
        List<ExpectedOp> observed = new ArrayList<>();
        for (TraceOperation operation : trace.getOperations()) {
            observed.add(new ExpectedOp(operation.getOp(), operation.getCandidateOrdinal(),
                operation.getActionId(), operation.getEvaluatorId(), operation.getBeforeBits(),
                operation.getDeltaBits(), operation.getAfterBits(), operation.isVetoed()));
        }
        return observed;
    }

    private static ExpectedOp op(TraceOp op, int candidateOrdinal, String actionId,
                                 String evaluatorId, Float before, Float delta,
                                 Float after, boolean vetoed) {
        return new ExpectedOp(op, candidateOrdinal, actionId, evaluatorId,
            nullableBits(before), nullableBits(delta), nullableBits(after), vetoed);
    }

    private static Integer nullableBits(Float value) {
        return value == null ? null : bits(value);
    }

    private static int bits(float value) {
        return Float.floatToRawIntBits(value);
    }
}
