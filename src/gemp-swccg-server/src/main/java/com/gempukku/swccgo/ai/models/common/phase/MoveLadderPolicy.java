package com.gempukku.swccgo.ai.models.common.phase;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared MOVE rank-ladder decisions. Bot adapters retain game-state reads,
 * action mutation, logging, and the rule-specific claim call sites.
 */
public final class MoveLadderPolicy {
    private static final int RANK_R4 = 4;
    private static final int RANK_R3 = 3;
    private static final int RANK_R2 = 2;
    private static final int RANK_R1 = 1;

    private static final float RANK_R4_SCORE = 20000.0f;
    private static final float RANK_R3_SCORE = 12000.0f;
    private static final float RANK_R2_SCORE = 6000.0f;
    private static final float LADDER_VETO = -100000.0f;
    private static final float FINE_CLAMP = 2800.0f;
    private static final float R2_CLAIM_MIN_FINE = 200.0f;
    private static final float R2_CLAIM_MIN_DRAIN_DELTA = 2.0f;
    private static final float ATE_CROSS_NEG = 550.0f;
    private static final float ATE_CROSS_POS = 250.0f;
    private static final float R1_FINE_CEILING = 1670.0f;

    public enum StepKind {
        HARD_VETO,
        WRONG_DIRECTION_SUPPRESSED,
        WRONG_DIRECTION_VETO,
        CAN_WIN_VETO,
        CAN_WIN_RETAINED,
        POSITIVE_CLAMP,
        NEGATIVE_CLAMP,
        DEMOTE,
        RANK_BASE,
        DEFAULT_PENALTY
    }

    public record State(
            int rank,
            boolean hardVeto,
            String hardVetoReason,
            boolean canWinVeto,
            String canWinVetoReason,
            boolean battleSeekingClaim,
            boolean mandatoryTransit,
            boolean wrongDirectionVeto,
            String wrongDirectionVetoReason,
            boolean rankMoveRan) {
    }

    public record RankTwoClaim(
            int rank,
            boolean battleSeekingClaim,
            boolean accepted,
            float requiredFine,
            float requiredDrainDelta) {
    }

    public record BandIntegrity(
            float r2Floor,
            float r1Ceiling,
            float margin,
            boolean inverted,
            float rankR2Score,
            float fineClamp,
            float actionTextCrossNegative,
            float r1FineCeiling,
            float actionTextCrossPositive) {
    }

    public record Step(
            StepKind kind,
            String reasoning,
            float delta,
            float observedFines,
            int rankBefore,
            int rankAfter,
            String detail) {
        private static Step contribution(
                StepKind kind, String reasoning, float delta,
                float observedFines, int rankBefore, int rankAfter,
                String detail) {
            return new Step(
                    kind, reasoning, delta, observedFines,
                    rankBefore, rankAfter, detail);
        }

        private static Step logOnly(StepKind kind, int rank, String detail) {
            return new Step(kind, null, 0.0f, 0.0f, rank, rank, detail);
        }

        public boolean contributesReasoning() {
            return reasoning != null;
        }
    }

    public record Finalization(List<Step> steps) {
        public Finalization {
            steps = List.copyOf(steps);
        }
    }

    private MoveLadderPolicy() {
    }

    public static int claimR4(int currentRank) {
        return Math.max(currentRank, RANK_R4);
    }

    public static int claimR3(int currentRank) {
        return Math.max(currentRank, RANK_R3);
    }

    public static RankTwoClaim claimR2(
            int currentRank,
            boolean currentBattleSeekingClaim,
            float ownFine,
            float drainDelta,
            boolean battleSeeking) {
        boolean accepted = ownFine >= R2_CLAIM_MIN_FINE
                || drainDelta >= R2_CLAIM_MIN_DRAIN_DELTA;
        return new RankTwoClaim(
                accepted ? Math.max(currentRank, RANK_R2) : currentRank,
                currentBattleSeekingClaim || (accepted && battleSeeking),
                accepted,
                R2_CLAIM_MIN_FINE,
                R2_CLAIM_MIN_DRAIN_DELTA);
    }

    public static BandIntegrity bandIntegrity() {
        float r2Floor = RANK_R2_SCORE - FINE_CLAMP - ATE_CROSS_NEG;
        float r1Ceiling = R1_FINE_CEILING + ATE_CROSS_POS;
        return new BandIntegrity(
                r2Floor,
                r1Ceiling,
                r2Floor - r1Ceiling,
                r2Floor <= r1Ceiling,
                RANK_R2_SCORE,
                FINE_CLAMP,
                ATE_CROSS_NEG,
                R1_FINE_CEILING,
                ATE_CROSS_POS);
    }

    public static Finalization finalizeAction(State state, float fines) {
        List<Step> steps = new ArrayList<>();
        int rank = state.rank();

        if (state.hardVeto()) {
            steps.add(Step.contribution(
                    StepKind.HARD_VETO,
                    "LADDER VETO: " + state.hardVetoReason(),
                    LADDER_VETO,
                    fines,
                    rank,
                    rank,
                    state.hardVetoReason()));
            return new Finalization(steps);
        }

        if (state.wrongDirectionVeto()) {
            if (state.mandatoryTransit()) {
                steps.add(Step.contribution(
                        StepKind.WRONG_DIRECTION_SUPPRESSED,
                        "V38.3 wrong-direction suppressed (R4 mandatory transit)",
                        0.0f,
                        fines,
                        rank,
                        rank,
                        state.wrongDirectionVetoReason()));
            } else {
                steps.add(Step.contribution(
                        StepKind.WRONG_DIRECTION_VETO,
                        "LADDER VETO: " + state.wrongDirectionVetoReason(),
                        LADDER_VETO,
                        fines,
                        rank,
                        rank,
                        state.wrongDirectionVetoReason()));
                return new Finalization(steps);
            }
        }

        if (state.canWinVeto()) {
            if (state.rank() == RANK_R2 && state.battleSeekingClaim()) {
                steps.add(Step.contribution(
                        StepKind.CAN_WIN_VETO,
                        "LADDER VETO: " + state.canWinVetoReason(),
                        LADDER_VETO,
                        fines,
                        rank,
                        rank,
                        state.canWinVetoReason()));
                return new Finalization(steps);
            }
            steps.add(Step.logOnly(
                    StepKind.CAN_WIN_RETAINED,
                    state.rank(),
                    state.canWinVetoReason()));
        }

        if (fines > FINE_CLAMP) {
            steps.add(Step.contribution(
                    StepKind.POSITIVE_CLAMP,
                    String.format(
                            "LADDER CLAMP: fines %+.0f clamped to %+.0f",
                            fines,
                            FINE_CLAMP),
                    FINE_CLAMP - fines,
                    fines,
                    rank,
                    rank,
                    null));
        } else if (fines < -FINE_CLAMP) {
            steps.add(Step.contribution(
                    StepKind.NEGATIVE_CLAMP,
                    String.format(
                            "LADDER CLAMP: fines %+.0f clamped to %+.0f",
                            fines,
                            -FINE_CLAMP),
                    -FINE_CLAMP - fines,
                    fines,
                    rank,
                    rank,
                    null));
            if (rank == RANK_R2 || rank == RANK_R3) {
                int rankBefore = rank;
                rank -= 1;
                steps.add(Step.contribution(
                        StepKind.DEMOTE,
                        "LADDER DEMOTE: negative clamp hit — claim demoted one band (ruling L1)",
                        0.0f,
                        fines,
                        rankBefore,
                        rank,
                        null));
            }
        }

        if (rank >= RANK_R4) {
            steps.add(Step.contribution(
                    StepKind.RANK_BASE,
                    "LADDER: R4 MANDATORY TRANSIT base",
                    RANK_R4_SCORE,
                    fines,
                    rank,
                    rank,
                    null));
        } else if (rank == RANK_R3) {
            steps.add(Step.contribution(
                    StepKind.RANK_BASE,
                    "LADDER: R3 SURVIVAL base",
                    RANK_R3_SCORE,
                    fines,
                    rank,
                    rank,
                    null));
        } else if (rank == RANK_R2) {
            steps.add(Step.contribution(
                    StepKind.RANK_BASE,
                    "LADDER: R2 DOCTRINE base",
                    RANK_R2_SCORE,
                    fines,
                    rank,
                    rank,
                    null));
        } else if (state.rankMoveRan() && state.rank() == RANK_R1) {
            steps.add(Step.contribution(
                    StepKind.DEFAULT_PENALTY,
                    "No strategic reason to move",
                    -50.0f,
                    fines,
                    rank,
                    rank,
                    null));
        }

        return new Finalization(steps);
    }
}
