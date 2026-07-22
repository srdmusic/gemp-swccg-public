package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;

import java.util.Objects;

/** Immutable facts for route-specific PULL child selection. */
public final class PullSelectionCandidateFacts {

    public enum CloudCityMode {
        NONE,
        OBJECTIVE,
        IM_SORRY,
        SLIP_SLIDING
    }

    public enum CloudCitySite {
        UPPER_WALKWAY,
        DINING_ROOM,
        SECURITY_TOWER,
        CARBONITE_CHAMBER,
        OTHER
    }

    public enum PlanState {
        NONE,
        IN_PLAN,
        HOLD_BACK
    }

    public enum UnknownAmsdState {
        NONE,
        PIETT,
        NON_PIETT
    }

    public enum PilotAmsdState {
        NOT_AMSD,
        NON_PIETT,
        PIETT_EXECUTOR_MISSING,
        PIETT_EXECUTOR_PRESENT,
        PIETT_ORACLE_UNAVAILABLE
    }

    public record IwtmLocation(String actionId, boolean starkillerSystem) {
        public IwtmLocation {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record UnknownPull(String actionId, String title,
                              CardCategory category, boolean gainDecision,
                              boolean huntDownLightsaber,
                              boolean activeObjectiveFlipGate,
                              CloudCityMode cloudCityMode,
                              CloudCitySite cloudCitySite,
                              Integer priorityProtectionScore,
                              UnknownAmsdState amsdState) {
        public UnknownPull {
            Objects.requireNonNull(actionId, "actionId");
            title = title == null ? "" : title;
            Objects.requireNonNull(cloudCityMode, "cloudCityMode");
            Objects.requireNonNull(cloudCitySite, "cloudCitySite");
            Objects.requireNonNull(amsdState, "amsdState");
        }
    }

    public record BlueprintPull(String actionId,
                                CloudCityMode cloudCityMode,
                                CloudCitySite cloudCitySite,
                                PlanState planState,
                                String planStrategy) {
        public BlueprintPull {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(cloudCityMode, "cloudCityMode");
            Objects.requireNonNull(cloudCitySite, "cloudCitySite");
            Objects.requireNonNull(planState, "planState");
            planStrategy = planStrategy == null ? "" : planStrategy;
        }
    }

    public record AmsdPilot(String actionId, String title,
                            PilotAmsdState state) {
        public AmsdPilot {
            Objects.requireNonNull(actionId, "actionId");
            title = title == null ? "null" : title;
            Objects.requireNonNull(state, "state");
        }
    }

    private PullSelectionCandidateFacts() {
    }
}
