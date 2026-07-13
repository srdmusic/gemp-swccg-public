package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.trace.state.PendingDeployEvent;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TraceStateEventFailureTestSupportTest {

    @Test
    public void injectedAppendFailureIsTypedAndDoesNotLeakTheSession() {
        TraceStateEventFailureTestSupport.openThrowingStateEventSession();
        try {
            TraceSession.recordPendingDeploy(PendingDeployEvent.Operation.SET, null, "CHARACTER");

            DecisionTrace trace = TraceStateEventFailureTestSupport.close();
            assertNotNull(trace);
            assertTrue(trace.getStateEvents().isEmpty());
            assertTrue(trace.getCaptureFailures().stream().anyMatch(failure ->
                failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                    && failure.detail().contains("injected state-event append failure")));
            assertFalse(TraceSession.isActive());
        } finally {
            TraceSession.abandon();
        }
    }
}
