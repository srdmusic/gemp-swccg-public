package com.gempukku.swccgo.ai.models.common.phase;

/** Pure boundary policy for the bounded turn-five ground spread release. */
public final class LateEstablishPolicy {
    public static final int FIRST_RELAXED_TURN = 5;
    public static final int OPPONENT_LOST_PILE_CUTOFF = 20;
    public static final int LEGACY_GROUND_ESTABLISH_LIMIT = 2;
    public static final float MIN_PROJECTED_ABILITY = 4.0f;

    public record CandidateFacts(
            int turnNumber,
            int opponentLostPileSize,
            boolean groundSite,
            boolean emptyDestination,
            boolean exactCandidateEligible,
            boolean affordable,
            float projectedAbility) {
    }

    private LateEstablishPolicy() {
    }

    public static int groundEstablishLimit(
            int turnNumber, int opponentLostPileSize) {
        return groundEstablishLimit(turnNumber, opponentLostPileSize,
                LEGACY_GROUND_ESTABLISH_LIMIT);
    }

    public static int groundEstablishLimit(
            int turnNumber, int opponentLostPileSize, int legacyLimit) {
        return inLateGroundWindow(turnNumber, opponentLostPileSize)
                ? legacyLimit + 1
                : legacyLimit;
    }

    public static boolean allowsWeakSolo(CandidateFacts facts) {
        return facts != null
                && inLateGroundWindow(
                        facts.turnNumber(), facts.opponentLostPileSize())
                && facts.groundSite()
                && facts.emptyDestination()
                && facts.exactCandidateEligible()
                && facts.affordable()
                && Float.isFinite(facts.projectedAbility())
                && facts.projectedAbility() >= MIN_PROJECTED_ABILITY;
    }

    private static boolean inLateGroundWindow(
            int turnNumber, int opponentLostPileSize) {
        return turnNumber >= FIRST_RELAXED_TURN
                && opponentLostPileSize >= 0
                && opponentLostPileSize < OPPONENT_LOST_PILE_CUTOFF;
    }
}
