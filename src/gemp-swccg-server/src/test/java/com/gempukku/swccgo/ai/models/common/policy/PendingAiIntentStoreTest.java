package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PendingAiIntentStoreTest {
    private static final PendingAiIntent.ExpiryKey KEY =
            new PendingAiIntent.ExpiryKey("game-1", 4, Phase.DEPLOY);

    @Test
    public void exactPhysicalMatchConsumesIntent() {
        PendingAiIntentStore store = new PendingAiIntentStore();
        PendingAiIntent intent = physicalIntent(Set.of(217));
        store.remember(intent);

        PendingAiIntentStore.Resolution result = store.resolve(child(KEY,
                PendingAiIntent.ChildShape.CARD,
                new PendingAiIntent.ChildCandidate("7", 217),
                new PendingAiIntent.ChildCandidate("8", 226)));

        assertEquals(PendingAiIntentStore.ResolutionStatus.MATCHED, result.status());
        assertEquals(intent, result.consumedIntent());
        assertEquals(Integer.valueOf(217), result.matchedCandidate().physicalCardId());
        assertFalse(store.current().isPresent());
    }

    @Test
    public void duplicateExactMatchesClearAsAmbiguous() {
        PendingAiIntentStore store = new PendingAiIntentStore();
        store.remember(physicalIntent(Set.of(217)));

        PendingAiIntentStore.Resolution result = store.resolve(child(KEY,
                PendingAiIntent.ChildShape.CARD,
                new PendingAiIntent.ChildCandidate("7", 217),
                new PendingAiIntent.ChildCandidate("8", 217)));

        assertFallback(result, PendingAiIntentStore.ClearReason.AMBIGUOUS_MATCH);
        assertFalse(store.current().isPresent());
    }

    @Test
    public void missingCandidateClearsAndFallsBack() {
        PendingAiIntentStore store = new PendingAiIntentStore();
        store.remember(physicalIntent(Set.of(217)));

        PendingAiIntentStore.Resolution result = store.resolve(child(KEY,
                PendingAiIntent.ChildShape.CARD,
                new PendingAiIntent.ChildCandidate("8", 226)));

        assertFallback(result, PendingAiIntentStore.ClearReason.MISSING_CANDIDATE);
    }

    @Test
    public void expiryAndShapeChangesClearWithExactReason() {
        assertFallback(resolveAgainst(new PendingAiIntent.ExpiryKey("game-2", 4, Phase.DEPLOY),
                PendingAiIntent.ChildShape.CARD), PendingAiIntentStore.ClearReason.GAME_RESET);
        assertFallback(resolveAgainst(new PendingAiIntent.ExpiryKey("game-1", 5, Phase.DEPLOY),
                PendingAiIntent.ChildShape.CARD), PendingAiIntentStore.ClearReason.TURN_CHANGED);
        assertFallback(resolveAgainst(new PendingAiIntent.ExpiryKey("game-1", 4, Phase.MOVE),
                PendingAiIntent.ChildShape.CARD), PendingAiIntentStore.ClearReason.PHASE_CHANGED);
        assertFallback(resolveAgainst(KEY, PendingAiIntent.ChildShape.DESTINATION),
                PendingAiIntentStore.ClearReason.CHILD_SHAPE_MISMATCH);
    }

    @Test
    public void explicitTerminalReasonsClearIntent() {
        for (PendingAiIntentStore.ClearReason reason : List.of(
                PendingAiIntentStore.ClearReason.COMPLETED,
                PendingAiIntentStore.ClearReason.PASS_OR_NO,
                PendingAiIntentStore.ClearReason.FAILED_SEARCH,
                PendingAiIntentStore.ClearReason.GAME_RESET)) {
            PendingAiIntentStore store = new PendingAiIntentStore();
            store.remember(physicalIntent(Set.of(217)));
            assertTrue(store.clear(reason));
            assertFalse(store.current().isPresent());
        }
    }

    @Test
    public void blueprintOnlyIntentCannotBeCreated() {
        try {
            new PendingAiIntent(KEY, "parent-decision", "parent-action", null,
                    TraceDomainId.PULL_SEARCH, PendingAiIntent.ChildShape.CARD,
                    Set.of(), Set.of());
            fail("expected identity-free intent rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void exactActionIdSupportsYesNoWithoutPhysicalIdentity() {
        PendingAiIntentStore store = new PendingAiIntentStore();
        store.remember(new PendingAiIntent(KEY, "parent-decision", "parent-action", null,
                TraceDomainId.RESPONSE_ROUTING, PendingAiIntent.ChildShape.YES_NO,
                Set.of("1"), Set.of()));

        PendingAiIntentStore.Resolution result = store.resolve(child(KEY,
                PendingAiIntent.ChildShape.YES_NO,
                new PendingAiIntent.ChildCandidate("0", null),
                new PendingAiIntent.ChildCandidate("1", null)));

        assertEquals(PendingAiIntentStore.ResolutionStatus.MATCHED, result.status());
        assertEquals("1", result.matchedCandidate().actionId());
    }

    private static PendingAiIntent physicalIntent(Set<Integer> physicalIds) {
        return new PendingAiIntent(KEY, "parent-decision", "parent-action", 55,
                TraceDomainId.PULL_SEARCH, PendingAiIntent.ChildShape.CARD,
                Set.of(), physicalIds);
    }

    private static PendingAiIntent.ChildDecision child(PendingAiIntent.ExpiryKey key,
                                                        PendingAiIntent.ChildShape shape,
                                                        PendingAiIntent.ChildCandidate... candidates) {
        return new PendingAiIntent.ChildDecision(key, shape, List.of(candidates));
    }

    private static PendingAiIntentStore.Resolution resolveAgainst(
            PendingAiIntent.ExpiryKey key, PendingAiIntent.ChildShape shape) {
        PendingAiIntentStore store = new PendingAiIntentStore();
        store.remember(physicalIntent(Set.of(217)));
        PendingAiIntentStore.Resolution result = store.resolve(
                child(key, shape, new PendingAiIntent.ChildCandidate("7", 217)));
        assertFalse(store.current().isPresent());
        return result;
    }

    private static void assertFallback(PendingAiIntentStore.Resolution result,
                                       PendingAiIntentStore.ClearReason reason) {
        assertEquals(PendingAiIntentStore.ResolutionStatus.FALLBACK, result.status());
        assertEquals(reason, result.clearReason());
        assertEquals(null, result.consumedIntent());
        assertEquals(null, result.matchedCandidate());
    }
}
