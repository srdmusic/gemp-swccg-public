package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.trace.state.TraceStateEvent;

import java.util.List;

/** Test-only seam for proving state-event append failures cannot affect legacy behavior. */
public final class TraceStateEventFailureTestSupport {

    private TraceStateEventFailureTestSupport() {
    }

    public static void openThrowingStateEventSession() {
        TraceCollector collector = new TraceCollector(
            "state-event-failure-test", "0", "EMPTY", "injected append failure",
            List.of(), null, List.of("test support intentionally omits the decision snapshot"),
            false) {
            @Override
            void recordStateEvent(TraceStateEvent event) {
                throw new IllegalStateException("injected state-event append failure");
            }
        };
        if (!TraceSession.openForTesting(collector)) {
            throw new IllegalStateException("unable to open test trace session");
        }
    }

    public static DecisionTrace close() {
        return TraceSession.close();
    }
}
