package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * B0-TIE-DETERMINISM (2026-07-13, gate CODEX_TIE_DETERMINISM_GATE_5DF276C1B): focused
 * fixture coverage for the intentional winner-contract delta of commit 5df276c1b.
 * LinkedHashMap first-seen merge order + strict Float.compare(candidate, best) > 0
 * means EXACT score ties always keep the earliest-offered candidate, in the normal
 * final selection, in the V67bc DPS bucket walk, and in the all-vetoed forced-choice
 * fallback. Each case also proves the comparison is strict-greater, not first-always:
 * a later candidate one ULP higher DOES replace the incumbent.
 *
 * Pure JUnit through the package-visible CombinedEvaluator seam (55c22fdde), scripted
 * evaluators, NoOpTraceSink: winner selection only, no tracing, no server.
 */
public class CombinedEvaluatorTieTest {

    // =========================================================================
    // Scripted harness (same style as CombinedEvaluatorTraceTest)
    // =========================================================================

    private static final class ScriptedEvaluator extends ActionEvaluator {
        private final Function<DecisionContext, List<EvaluatedAction>> script;

        ScriptedEvaluator(String name, Function<DecisionContext, List<EvaluatedAction>> script) {
            super(name);
            this.script = script;
        }

        @Override
        public boolean canEvaluate(DecisionContext context) {
            return true;
        }

        @Override
        public List<EvaluatedAction> evaluate(DecisionContext context) {
            return script.apply(context);
        }
    }

    private static DecisionContext passableContext() {
        DecisionContext ctx = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose action to take", "d1", Phase.DEPLOY);
        ctx.setNoPass(false);
        ctx.setMin(0);
        return ctx;
    }

    /** Passing is illegal: noPass=true, min>0, and no Done/Cancel/optional prompt text. */
    private static DecisionContext forcedContext() {
        DecisionContext ctx = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose action to take", "d1", Phase.DEPLOY);
        ctx.setNoPass(true);
        ctx.setMin(1);
        return ctx;
    }

    private static EvaluatedAction action(String id, float score, String text) {
        return new EvaluatedAction(id, ActionType.UNKNOWN, score, text);
    }

    private static EvaluatedAction winner(List<ActionEvaluator> evaluators, DecisionContext ctx) {
        return new CombinedEvaluator(evaluators, NoOpTraceSink.INSTANCE).evaluateDecision(ctx);
    }

    private static List<ActionEvaluator> single(Function<DecisionContext, List<EvaluatedAction>> script) {
        return Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1", script));
    }

    /** Smallest float strictly greater than {@code f} — the tightest strict-> boundary. */
    private static float oneUlpUp(float f) {
        return Math.nextUp(f);
    }

    // =========================================================================
    // Case 1: normal final selection — equal merged scores keep the FIRST offered
    // =========================================================================

    @Test
    public void normalFinalSelectionEqualScoresChooseFirstOffered() {
        EvaluatedAction tied = winner(single(ctx -> Arrays.asList(
            action("A", 100.0f, "Deploy A"),
            action("B", 100.0f, "Deploy B"))), passableContext());
        assertNotNull(tied);
        assertEquals("exact tie must retain the first offered action", "A", tied.getActionId());
        assertEquals(Float.floatToRawIntBits(100.0f), Float.floatToRawIntBits(tied.getScore()));

        // strict->, not first-always: a later candidate one ULP higher replaces the incumbent
        float higher = oneUlpUp(100.0f);
        EvaluatedAction replaced = winner(single(ctx -> Arrays.asList(
            action("A", 100.0f, "Deploy A"),
            action("B", higher, "Deploy B"))), passableContext());
        assertNotNull(replaced);
        assertEquals("one-ULP-higher later candidate must win", "B", replaced.getActionId());
        assertEquals(Float.floatToRawIntBits(higher), Float.floatToRawIntBits(replaced.getScore()));
    }

    // =========================================================================
    // Case 2: duplicate action ids — later merges do not move the first insertion
    // =========================================================================

    @Test
    public void duplicateActionIdMergePreservesFirstInsertionOnTie() {
        // E1 inserts A first (60), then B (100); E2 merges +40 into A → A=100 ties B=100.
        // A keeps its earlier LinkedHashMap slot despite being completed by a LATER evaluator.
        ScriptedEvaluator e1 = new ScriptedEvaluator("E1", ctx -> Arrays.asList(
            action("A", 60.0f, "Deploy A"),
            action("B", 100.0f, "Deploy B")));
        ScriptedEvaluator e2 = new ScriptedEvaluator("E2", ctx -> Arrays.asList(
            action("A", 40.0f, "Deploy A")));
        EvaluatedAction tied = winner(Arrays.asList(e1, e2), passableContext());
        assertNotNull(tied);
        assertEquals("merged tie must retain the first-inserted action", "A", tied.getActionId());
        assertEquals(Float.floatToRawIntBits(100.0f), Float.floatToRawIntBits(tied.getScore()));

        // strict->: when the merge leaves A strictly below B, the later-inserted B wins
        ScriptedEvaluator e2Lower = new ScriptedEvaluator("E2", ctx -> Arrays.asList(
            action("A", 39.0f, "Deploy A")));
        EvaluatedAction other = winner(Arrays.asList(e1, e2Lower), passableContext());
        assertNotNull(other);
        assertEquals("strictly higher later insertion must win", "B", other.getActionId());
        assertEquals(Float.floatToRawIntBits(100.0f), Float.floatToRawIntBits(other.getScore()));
        assertNotEquals("control: merged A must be strictly below B", "A", other.getActionId());
    }

    // =========================================================================
    // Case 3: V67bc DPS bucket walk — equal scores in one bucket keep the first offered
    // =========================================================================

    private static DecisionContext bucketedContext() {
        DecisionContext ctx = passableContext();
        ctx.setStepBuckets(Arrays.asList(
            (java.util.Set<String>) new java.util.LinkedHashSet<>(Arrays.asList("A", "B"))));
        ctx.setStepBucketLabels(Arrays.asList("STEP1"));
        return ctx;
    }

    @Test
    public void dpsBucketEqualScoresChooseFirstOffered() {
        // Both above BAD_ACTION_THRESHOLD so the bucket winner is returned directly.
        EvaluatedAction tied = winner(single(ctx -> Arrays.asList(
            action("A", 200.0f, "Deploy A"),
            action("B", 200.0f, "Deploy B"))), bucketedContext());
        assertNotNull(tied);
        assertEquals("bucket tie must retain the first offered action", "A", tied.getActionId());
        assertEquals(Float.floatToRawIntBits(200.0f), Float.floatToRawIntBits(tied.getScore()));

        // strict->: one ULP higher later in the same bucket replaces the incumbent
        float higher = oneUlpUp(200.0f);
        EvaluatedAction replaced = winner(single(ctx -> Arrays.asList(
            action("A", 200.0f, "Deploy A"),
            action("B", higher, "Deploy B"))), bucketedContext());
        assertNotNull(replaced);
        assertEquals("one-ULP-higher later bucket candidate must win", "B", replaced.getActionId());
        assertEquals(Float.floatToRawIntBits(higher), Float.floatToRawIntBits(replaced.getScore()));
    }

    // =========================================================================
    // Case 4: all actions hard-vetoed, passing illegal — equal scores keep the first vetoed
    // =========================================================================

    private static List<ActionEvaluator> allVetoed(float scoreA, float scoreB) {
        return single(ctx -> {
            EvaluatedAction a = action("A", scoreA, "Deploy A");
            a.hardVeto("formation law A");
            EvaluatedAction b = action("B", scoreB, "Deploy B");
            b.hardVeto("formation law B");
            return Arrays.asList(a, b);
        });
    }

    @Test
    public void allVetoedForcedChoiceEqualScoresChooseFirstVetoed() {
        EvaluatedAction tied = winner(allVetoed(-500.0f, -500.0f), forcedContext());
        assertNotNull(tied);
        assertNotEquals("pass is illegal: must not synthesize a Pass",
            ActionType.PASS, tied.getActionType());
        assertTrue("least-bad fallback returns the vetoed action itself", tied.isHardVetoed());
        assertEquals("vetoed exact tie must retain the first offered action", "A", tied.getActionId());
        assertEquals(Float.floatToRawIntBits(-500.0f), Float.floatToRawIntBits(tied.getScore()));

        // strict->: a later vetoed candidate one ULP higher (toward zero) replaces the incumbent
        float higher = oneUlpUp(-500.0f);
        EvaluatedAction replaced = winner(allVetoed(-500.0f, higher), forcedContext());
        assertNotNull(replaced);
        assertTrue(replaced.isHardVetoed());
        assertEquals("one-ULP-higher later vetoed candidate must win", "B", replaced.getActionId());
        assertEquals(Float.floatToRawIntBits(higher), Float.floatToRawIntBits(replaced.getScore()));
    }
}
