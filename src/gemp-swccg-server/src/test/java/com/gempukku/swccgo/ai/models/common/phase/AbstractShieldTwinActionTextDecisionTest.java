package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Shared externally visible action-text decision contract for the two bot
 * adapters. The exact firing prompt comes from Card222_013.
 */
public abstract class AbstractShieldTwinActionTextDecisionTest {
    protected static final String LIVE_ATTEMPT =
            "Attempt to 'blow away' Main Power Generators";

    public record Candidate(
            String actionId, float score, boolean hardVeto,
            String reasoning) {
    }

    protected abstract ObjectiveAnalyzer analyze(
            String objectiveBlueprintId);

    protected abstract List<Candidate> evaluate(
            ObjectiveAnalyzer analyzer, List<String> actionIds,
            List<String> actionTexts, List<String> sourceBlueprintIds);

    @Test
    public void exactVirtualFiringPromptWinsForBothObjectivePrintings() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            ObjectiveAnalyzer analyzer = analyze(objectiveBlueprintId);
            List<Candidate> candidates = evaluate(
                    analyzer,
                    List.of("wait", "fire"),
                    List.of(
                            "Take another action",
                            LIVE_ATTEMPT),
                    List.of("1_1", "222_13"));

            Candidate wait = candidate(candidates, "wait");
            Candidate fire = candidate(candidates, "fire");
            Candidate winner = candidates.stream()
                    .max((left, right) ->
                            Float.compare(left.score(), right.score()))
                    .orElseThrow();

            assertEquals(objectiveBlueprintId, "fire", winner.actionId());
            assertEquals(objectiveBlueprintId, 800.0f,
                    fire.score() - wait.score(), 0.0f);
            assertTrue(objectiveBlueprintId,
                    fire.reasoning().contains(
                            "V160 PUSH TARGET THE MAIN GENERATOR"));
            assertFalse(objectiveBlueprintId, fire.hardVeto());
        }
    }

    @Test
    public void similarlyWordedNonSourceActionGetsNoObjectivePush() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            List<Candidate> candidates = evaluate(
                    analyze(objectiveBlueprintId),
                    List.of("real", "fake"),
                    List.of(
                            LIVE_ATTEMPT,
                            "Attempt to 'blow away' Shield Generator"),
                    List.of("222_13", "222_13"));

            Candidate real = candidate(candidates, "real");
            Candidate fake = candidate(candidates, "fake");
            assertEquals(objectiveBlueprintId, 800.0f,
                    real.score() - fake.score(), 0.0f);
            assertFalse(objectiveBlueprintId,
                    fake.reasoning().contains(
                            "V160 PUSH TARGET THE MAIN GENERATOR"));
        }
    }

    @Test
    public void classicCardWithSameTitleCannotBorrowTheVirtualRoutePush() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            List<Candidate> candidates = evaluate(
                    analyze(objectiveBlueprintId),
                    List.of("virtual", "classic"),
                    List.of(LIVE_ATTEMPT, LIVE_ATTEMPT),
                    List.of("222_13", "3_115"));

            Candidate virtual = candidate(candidates, "virtual");
            Candidate classic = candidate(candidates, "classic");
            assertEquals(objectiveBlueprintId, 800.0f,
                    virtual.score() - classic.score(), 0.0f);
            assertFalse(objectiveBlueprintId,
                    classic.reasoning().contains(
                            "V160 PUSH TARGET THE MAIN GENERATOR"));
        }
    }

    private static Candidate candidate(
            List<Candidate> candidates, String actionId) {
        return candidates.stream()
                .filter(candidate ->
                        actionId.equals(candidate.actionId()))
                .findFirst()
                .orElseThrow();
    }
}
