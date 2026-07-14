package com.gempukku.swccgo.ai;

import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

public interface SwccgAiController {
    String decide(String playerId, AwaitingDecision decision, GameState gameState);

    // ═══════════════════════════════════════════════════════════
    // ═══ FINALIZER RUNTIME (2026-07-13,
    //     Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md §2) ═══
    // The mediator-facing decision API. It returns a TYPED AiDecisionResult (a submittable
    // wire response OR a typed pre-engine rejection) instead of a raw String, and exposes
    // synchronous engine-disposition callbacks the mediator invokes on the SAME thread after
    // it learns whether the engine accepted, rejected, or the attempt failed.
    //
    // Backward compatibility: decide() is unchanged for direct callers. The three-argument
    // decideForEngine default wraps decide() as a legacy WIRE_RESPONSE with mutation mode
    // NONE; the four-argument (history-aware) default delegates to the three-argument method
    // so an existing controller override is not bypassed. Rando, ChosenOne, and Curator
    // override the history-aware method; their three-argument methods delegate with
    // RejectionHistory.empty(). All callbacks default to no-ops for legacy controllers.
    // ═══════════════════════════════════════════════════════════

    /**
     * Mediator-facing decision without rejection history. Default wraps {@link #decide} as a
     * legacy {@code WIRE_RESPONSE} with mutation mode {@code NONE}.
     */
    default AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                             GameState gameState) {
        return AiDecisionResult.legacyWire(decide(playerId, decision, gameState),
            String.valueOf(decision.getAwaitingDecisionId()));
    }

    /**
     * History-aware mediator-facing decision. The default delegates to the three-argument
     * method so an existing three-argument override is honored; the immutable
     * {@link RejectionHistory} is created and owned by the mediator retry loop and never
     * persisted by the controller.
     */
    default AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                             GameState gameState, RejectionHistory history) {
        return decideForEngine(playerId, decision, gameState);
    }

    /**
     * Synchronous engine-acceptance callback. Fires exactly once, on the mediator thread,
     * after the engine accepts the submitted wire and BEFORE clock credit, chat,
     * pending-action continuation, or child clock scheduling. The implementation owns its
     * outer mutation and trace close in try/finally. Default: no-op.
     */
    default void onDecisionAccepted(String playerId, AwaitingDecision decision,
                                    GameState gameState, AiDecisionResult result) {
        // Default: legacy controllers apply no post-acceptance mutation.
    }

    /**
     * Synchronous engine-rejection callback for a result that existed. {@code kind} classifies
     * the rejection ({@code ENGINE_REJECTED} checked rejection, {@code TYPED_REJECTION}
     * pre-engine, or {@code ATTEMPT_FAILED} runtime fault after a result). Performs no tracker
     * or strategic mutation; closes the attempt trace. Default: no-op.
     */
    default void onDecisionRejected(String playerId, AwaitingDecision decision,
                                    GameState gameState, AiDecisionResult result,
                                    DecisionRejectionKind kind, String detail) {
        // Default: legacy controllers hold no lifecycle state to clear.
    }

    /**
     * Synchronous callback for a computation or wrapper exception that escaped BEFORE an
     * {@link AiDecisionResult} existed. Receives no fabricated result; closes and emits any
     * active attempt trace. Default: no-op.
     */
    default void onDecisionAttemptFailed(String playerId, AwaitingDecision decision,
                                         GameState gameState, String detail) {
        // Default: legacy controllers hold no lifecycle state to clear.
    }

    /**
     * Set the current game reference for advanced AI features.
     * Default implementation does nothing for backward compatibility.
     */
    default void setGame(SwccgGame game) {
        // Default: no-op for AIs that don't need the full game reference
    }

    /**
     * Set the deck name this AI is playing with.
     * Called after AI creation so evaluators can make deck-aware decisions.
     * Default implementation does nothing for backward compatibility.
     */
    default void setDeckName(String deckName) {
        // Default: no-op for AIs that don't need deck name awareness
    }

    /**
     * Get the next chat message to send, if any.
     * Default implementation returns null (no chat).
     *
     * @return message to send, or null if no message
     */
    default String getChatMessage() {
        return null;
    }
}
