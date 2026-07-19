package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for the shared BATTLE action-text policy. */
public final class BattleActionTextFacts {

    public enum FmftdMode {
        LOST,
        USED,
        GENERIC
    }

    public enum StunningLeaderMode {
        OUTSIDE_BATTLE,
        OWN_INITIATED,
        DEFENDING,
        UNRESOLVED
    }

    public enum KillShotTarget {
        OPPONENT,
        OWN,
        UNRESOLVED
    }

    public enum SubstituteReadStatus {
        READ,
        READ_FAILED
    }

    public enum DestinyProtectionPhase {
        BATTLE,
        ACTIVATE,
        CONTROL,
        DEPLOY,
        OTHER
    }

    private BattleActionTextFacts() {
    }

    public record InitiationFacts(
            String actionId,
            boolean locationResolved,
            String locationTitle,
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            int reserveDeckSize) {

        public InitiationFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record ActionFacts(String actionId) {
        public ActionFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record WelcomeHomeFacts(String actionId, boolean saveForBattle, String why) {
        public WelcomeHomeFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(why, "why");
        }
    }

    public record YouAreBeatenModeFacts(String actionId,
                                        boolean iayfSearch,
                                        boolean battleFreeze,
                                        boolean battlePhase) {
        public YouAreBeatenModeFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record HatredFacts(String actionId,
                              boolean wrongTurn,
                              boolean inquisitorOnTable,
                              boolean sameSiteOpponent,
                              boolean sameSiteJedi,
                              boolean deployPhase) {
        public HatredFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record IHaveYouNowFacts(String actionId,
                                   boolean namedInActionText,
                                   boolean battlePhase,
                                   boolean vaderInBattle,
                                   boolean sourceCardMatches) {
        public IHaveYouNowFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record FmftdFacts(String actionId,
                             FmftdMode mode,
                             boolean battlePhase,
                             boolean deployOrMovePhase,
                             boolean inquisitorInBattle,
                             boolean jediInBattle,
                             boolean hatredOnOpponent) {
        public FmftdFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(mode, "mode");
        }
    }

    public record VaderRecallFacts(String actionId, boolean jediElsewhere) {
        public VaderRecallFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record InquisitorRecallFacts(String actionId, boolean opponentsOnBoard) {
        public InquisitorRecallFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record GenericYouAreBeatenFacts(String actionId, boolean battlePhase) {
        public GenericYouAreBeatenFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record StunningLeaderFacts(String actionId,
                                      StunningLeaderMode mode,
                                      float ourPower,
                                      float theirPower) {
        public StunningLeaderFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(mode, "mode");
        }
    }

    public record ProtectDestinyFacts(String actionId,
                                      int turnNumber,
                                      DestinyProtectionPhase phase) {
        public ProtectDestinyFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(phase, "phase");
        }
    }

    public record KillShotFacts(String actionId,
                                String targetTitle,
                                KillShotTarget target,
                                float power,
                                float forfeit) {
        public KillShotFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(targetTitle, "targetTitle");
            Objects.requireNonNull(target, "target");
        }
    }

    public record SubstituteDestinyFacts(String actionId,
                                         SubstituteReadStatus readStatus,
                                         float drawnDestiny,
                                         float bestAbility) {
        public SubstituteDestinyFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(readStatus, "readStatus");
        }
    }
}
