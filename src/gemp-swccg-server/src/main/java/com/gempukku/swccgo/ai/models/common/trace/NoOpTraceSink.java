package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE HOOK (2026-07-13): the production-default sink. Disabled, allocation-free,
 * behavior-free. It stays the default until shadow capture is explicitly enabled.
 */
public final class NoOpTraceSink implements TraceSink {

    public static final NoOpTraceSink INSTANCE = new NoOpTraceSink();

    private NoOpTraceSink() {
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void accept(DecisionTrace trace) {
        // no-op
    }
}
