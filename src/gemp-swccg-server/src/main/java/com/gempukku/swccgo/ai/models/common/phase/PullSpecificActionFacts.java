package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for card-specific PULL action scoring. */
public final class PullSpecificActionFacts {

    public enum PileKind {
        LOST,
        USED
    }

    public enum ReserveSourceKind {
        NONE,
        CRUSH_THE_REBELLION,
        YOU_ARE_BEATEN,
        BLAST_POINTS,
        HUNT_DOWN,
        IMPERIAL_COMMAND,
        ENDOR_SHIELD,
        VISAGE,
        KIR_KANOS
    }

    public enum DuplicateState {
        NONE,
        BOTH_IN_HAND,
        FIRST_IN_HAND_SECOND_MISSING,
        SECOND_IN_HAND_FIRST_MISSING
    }

    public record Gate(String actionId, boolean blocked, String reason) {
        public Gate {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record PileSearch(String actionId, PileKind pileKind, int pileSize) {
        public PileSearch {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(pileKind, "pileKind");
        }
    }

    public record ExhaustedSearch(String actionId, boolean targetAvailable) {
        public ExhaustedSearch {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record SorryLocation(String actionId, boolean targetAvailable) {
        public SorryLocation {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record WelcomeHome(String actionId, boolean saveForBattle, String why) {
        public WelcomeHome {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(why, "why");
        }
    }

    public record YouAreBeatenSearch(String actionId, boolean searchMode) {
        public YouAreBeatenSearch {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record IayfPresence(String actionId, boolean applies,
                               boolean vaderOnTable) {
        public IayfPresence {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record IayfReserve(String actionId, boolean vaderOnTable,
                              boolean reserveMode, boolean lostMode,
                              boolean saberInReserve, boolean saberInLost,
                              boolean vaderArmed) {
        public IayfReserve {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record NamedReserveSource(String actionId,
                                     ReserveSourceKind sourceKind,
                                     boolean targetAvailable,
                                     DuplicateState duplicateState) {
        public NamedReserveSource {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(sourceKind, "sourceKind");
            Objects.requireNonNull(duplicateState, "duplicateState");
        }
    }

    public record HuntDownLocationDownload(
            String actionId, boolean targetAvailable) {
        public HuntDownLocationDownload {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record VeersHothUpload(
            String actionId, boolean exactSourceAndAction,
            boolean targetAvailable) {
        public VeersHothUpload {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record ReserveRisk(String actionId, int reserveSize) {
        public ReserveRisk {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record MasterfulMove(String actionId, boolean characterOnTable,
                                int turnNumber) {
        public MasterfulMove {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record EffectSearch(String actionId, boolean wokling) {
        public EffectSearch {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record AdmiralGeneralPull(String actionId,
                                     boolean targetAvailable,
                                     boolean bespinChainActive) {
        public AdmiralGeneralPull {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    private PullSpecificActionFacts() {
    }
}
