package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;

import java.util.Objects;

/** Immutable parent-action facts consumed by the shared PULL policy. */
public final class PullActionFacts {

    public enum EarlyGate {
        NONE,
        V177_DEAD_SEARCH,
        V183_DEAD_SEARCH
    }

    public enum V131State {
        OPEN,
        CLOSED,
        HARD_BLOCK,
        DOWNGRADE
    }

    public enum FormationState {
        NONE,
        HARD_BLOCK,
        DEFER,
        UNKNOWN,
        FLIP_EXEMPT
    }

    public record EarlySearch(
            String actionId,
            EarlyGate gate,
            String reason) {
        public EarlySearch {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(gate, "gate");
            reason = reason == null ? "" : reason;
        }
    }

    public record Parent(
            String actionId,
            String actionText,
            int reserveSize,
            boolean failedTwice,
            String namedMissingTarget,
            PullOracleView.Validation memoryValidation,
            PullOracleView.Validation sourceValidation,
            String sourceTitle,
            boolean allReserveTargetsUnattachableWeapons,
            Integer cheapestTargetCost,
            int availableForce,
            boolean deadInterrupt,
            String deadInterruptTargets,
            int deadInterruptReserves,
            boolean locationPull,
            String locationReason,
            CardCategory sourceCategory,
            V131State v131State,
            String v131Reason,
            boolean weaponPull,
            String weaponReason,
            boolean lightsaberPull,
            int unarmedCharacters,
            int armedCharacters,
            int capableLightsaberWielders,
            boolean devicePull,
            String deviceReason,
            int deviceUnarmedCharacters,
            int deviceArmedCharacters,
            String keyCharacterToken,
            boolean keyCharacterAlreadyFilled,
            Phase phase,
            boolean freeDownload,
            boolean charactersOrVehiclesInHand,
            boolean battlePlausible,
            FormationState formationState,
            String formationReason,
            boolean requiredOnTableCardPull,
            boolean requiredOnTableCardPullVetoBypass,
            boolean objectiveRoutePullVetoBypass) {
        public Parent {
            Objects.requireNonNull(actionId, "actionId");
            actionText = actionText == null ? "" : actionText;
            namedMissingTarget = namedMissingTarget == null ? "" : namedMissingTarget;
            memoryValidation = memoryValidation == null
                    ? new PullOracleView.Validation(PullOracleView.Outcome.UNKNOWN, "")
                    : memoryValidation;
            sourceValidation = sourceValidation == null
                    ? new PullOracleView.Validation(PullOracleView.Outcome.UNKNOWN, "")
                    : sourceValidation;
            sourceTitle = sourceTitle == null ? "?" : sourceTitle;
            deadInterruptTargets = deadInterruptTargets == null ? "[]" : deadInterruptTargets;
            locationReason = locationReason == null ? "" : locationReason;
            v131State = v131State == null ? V131State.CLOSED : v131State;
            v131Reason = v131Reason == null ? "" : v131Reason;
            weaponReason = weaponReason == null ? "" : weaponReason;
            deviceReason = deviceReason == null ? "" : deviceReason;
            keyCharacterToken = keyCharacterToken == null ? "" : keyCharacterToken;
            formationState = formationState == null ? FormationState.NONE : formationState;
            formationReason = formationReason == null ? "" : formationReason;
        }
    }

    private PullActionFacts() {
    }
}
