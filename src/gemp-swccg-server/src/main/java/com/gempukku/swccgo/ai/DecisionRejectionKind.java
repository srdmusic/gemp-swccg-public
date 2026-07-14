package com.gempukku.swccgo.ai;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §2): the closed classification carried by {@code onDecisionRejected}. It names WHY the
 * mediator is reporting a non-accepted disposition, distinct from the finalizer's typed
 * {@link com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse.RejectReason}
 * (which describes why a proposed intent was unsendable).
 *
 * <ul>
 *   <li>{@code ENGINE_REJECTED} — the engine threw a checked
 *       {@code DecisionResultInvalidException} for the submitted wire (the F2 retry driver).</li>
 *   <li>{@code TYPED_REJECTION} — the AI returned an {@code AiDecisionResult} of status
 *       {@code TYPED_REJECTION}; {@code decisionMade} was never called.</li>
 *   <li>{@code ATTEMPT_FAILED} — a runtime fault occurred AFTER a result existed (an unchecked
 *       exception from {@code decisionMade}); no retry follows.</li>
 * </ul>
 */
public enum DecisionRejectionKind {
    ENGINE_REJECTED,
    TYPED_REJECTION,
    ATTEMPT_FAILED
}
