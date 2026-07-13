package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE HOOK (2026-07-13): consumer of finalized decision traces.
 *
 * The sink receives exactly ONE complete DecisionTrace per decision, only after
 * finalization. Production default is NoOpTraceSink (isEnabled false), in which case no
 * trace session is opened at all and the instrumentation cost is a cheap guard per
 * choke point. Implementations must not mutate game or decision state.
 */
public interface TraceSink {

    /** When false, no trace session is opened and no operations are recorded. */
    boolean isEnabled();

    /** Receives the one complete record for a decision, after finalization. */
    void accept(DecisionTrace trace);
}
