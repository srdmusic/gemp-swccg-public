package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for the shared BATTLE-2 policy. */
public final class BattleWeaponsFacts {

    public enum ForcePushMode {
        NONE,
        BATTLE_EXCLUSION,
        FORCE_PILE_EXCHANGE
    }

    public enum FireMode {
        NONE,
        VALID_TARGET_IN_BATTLE,
        VALID_TARGET_OUTSIDE_BATTLE,
        NO_VALID_TARGET
    }

    public enum ThrowMode {
        NONE,
        IN_BATTLE,
        OUTSIDE_BATTLE
    }

    public enum RedrawMode {
        NONE,
        KNOWN_DESTINY,
        UNKNOWN_DESTINY,
        READ_FAILED
    }

    public record RedrawFacts(RedrawMode mode,
                              float currentDestiny,
                              double averageReserveDestiny) {
        public RedrawFacts {
            Objects.requireNonNull(mode, "mode");
        }

        public static RedrawFacts none() {
            return new RedrawFacts(RedrawMode.NONE, -1.0f, 3.0d);
        }

        public static RedrawFacts known(float currentDestiny, double averageReserveDestiny) {
            return new RedrawFacts(RedrawMode.KNOWN_DESTINY,
                    currentDestiny, averageReserveDestiny);
        }

        public static RedrawFacts unknown(double averageReserveDestiny) {
            return new RedrawFacts(RedrawMode.UNKNOWN_DESTINY,
                    -1.0f, averageReserveDestiny);
        }

        public static RedrawFacts readFailed() {
            return new RedrawFacts(RedrawMode.READ_FAILED, -1.0f, 3.0d);
        }
    }

    public record ActionTextFacts(String actionId,
                                  ForcePushMode forcePushMode,
                                  FireMode fireMode,
                                  ThrowMode throwMode,
                                  RedrawFacts redraw) {
        public ActionTextFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(forcePushMode, "forcePushMode");
            Objects.requireNonNull(fireMode, "fireMode");
            Objects.requireNonNull(throwMode, "throwMode");
            Objects.requireNonNull(redraw, "redraw");
        }
    }

    public record ForceLightningFacts(String actionId,
                                      boolean opponentCharacterInPlay) {
        public ForceLightningFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record BlasterRackFacts(String actionId,
                                   boolean duringBattleDamage,
                                   boolean weaponCharacterAtBattle) {
        public BlasterRackFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public enum CancelBattleMode {
        NONE,
        OWN_INITIATED,
        OPPONENT_INITIATED_WITH_POWER
    }

    public record CancelBattleFacts(CancelBattleMode mode,
                                    float ourPower,
                                    float theirPower) {
        public CancelBattleFacts {
            Objects.requireNonNull(mode, "mode");
        }

        public static CancelBattleFacts none() {
            return new CancelBattleFacts(CancelBattleMode.NONE, 0.0f, 0.0f);
        }

        public static CancelBattleFacts ownInitiated() {
            return new CancelBattleFacts(CancelBattleMode.OWN_INITIATED, 0.0f, 0.0f);
        }

        public static CancelBattleFacts opponentInitiated(float ourPower, float theirPower) {
            return new CancelBattleFacts(CancelBattleMode.OPPONENT_INITIATED_WITH_POWER,
                    ourPower, theirPower);
        }
    }

    public record BattleEvaluatorFacts(String actionId,
                                       boolean fireAction,
                                       boolean characterTargetText,
                                       boolean uniqueTargetText,
                                       CancelBattleFacts cancelBattle,
                                       boolean battlePhase,
                                       boolean drawDestinyAction) {
        public BattleEvaluatorFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(cancelBattle, "cancelBattle");
        }
    }

    public record DestinyAssessment(boolean available,
                                    float defenseValue,
                                    float expectedDestinyTotal) {
        public static DestinyAssessment unavailable() {
            return new DestinyAssessment(false, 0.0f, 0.0f);
        }

        public static DestinyAssessment available(float defenseValue,
                                                   float expectedDestinyTotal) {
            return new DestinyAssessment(true, defenseValue, expectedDestinyTotal);
        }
    }

    /**
     * Target classifications are supplied by the adapter for this exact physical target.
     * The policy deliberately does not resolve ids or infer priority from the title.
     */
    public record TargetFacts(String actionId,
                              String targetTitle,
                              boolean alreadyHit,
                              DestinyAssessment destiny,
                              boolean gameTextCancelerPriority,
                              boolean battleDestinyAdderPriority,
                              boolean jediOrPadawanPriority,
                              boolean ownTargetWithHarmfulEffect) {
        public TargetFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(destiny, "destiny");
            targetTitle = targetTitle != null ? targetTitle : "?";
        }
    }

    private BattleWeaponsFacts() {
    }
}
