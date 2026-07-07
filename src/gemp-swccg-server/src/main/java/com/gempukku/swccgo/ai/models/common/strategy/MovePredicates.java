package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

// ═══════════════════════════════════════════════════════════
// ═══ T4.1 (2026-07-06): SHARED MOVE PREDICATES ═══
// The ONE home for the graded winnability predicate (V137 move-side +
// V181 deploy-side, the MOVE/DEPLOY-2 parity pair) and the engine-based
// drain metric (drainAt/drainDelta) that V85/V29.13 route through.
// Homed in common/strategy per orchestrator ruling so CharacterDeploySiteEvaluator
// and BOTH bots (rando + chosenone) reach it — this is the class the T2 helper
// spec reserved as "WinnabilityCalculator.canWinAt(graded overload)"; the T4.1
// session implemented it here first (spec name RandoMovePredicates, re-homed).
// If/when T2's WinnabilityCalculator lands, its graded overload should DELEGATE
// here (or absorb this class) — do NOT fork the predicate (T2 handshake,
// T4_Boundary_Tables_2026-07-06.md last bullet).
//
// canWinAt semantics (behavior-identical port, see parity note in
// CharacterDeploySiteEvaluator V181 block):
//   clean win  = ourPower >= oppPower AND ourAbility >= 4      (CDSE:253-254 parity)
//   V181 tolerance = 0 < gap <= 3 AND opp drain at site >= 2 AND ability >= 4
//                    AND forfeit parity (theirForfeit <= 0 OR ourForfeit <= theirForfeit*1.25)
//                    (ported from CharacterDeploySiteEvaluator:366-391)
// ═══════════════════════════════════════════════════════════
public final class MovePredicates {

    private static final Logger LOG = LogManager.getLogger(MovePredicates.class);

    // V181 tolerance constants (lifted verbatim from the CDSE V181 block;
    // T2 spec names them Tolerance{powerGapMax=3, drainMin=2, forfeitParityFactor=1.25})
    public static final float POWER_GAP_MAX = 3f;
    public static final float DRAIN_MIN = 2f;
    public static final float FORFEIT_PARITY_FACTOR = 1.25f;

    private MovePredicates() { /* static-only */ }

    /**
     * The shared graded winnability predicate (V137 move arm + V181 deploy arm).
     *
     * @param game               game handle
     * @param gs                 game state
     * @param playerId           OUR playerId (opponent derived internally)
     * @param site               the contested destination/site being evaluated
     * @param ourProjectedPower  projected friendly power at the site (caller-computed,
     *                           e.g. mover group + friendlies already there)
     * @param ourProjectedAbility projected friendly ability at the site
     * @param ourForfeitTotal    projected friendly forfeit total (mover-side sum)
     * @return true if the fight is winnable (clean win) or worth taking (V181 tolerance)
     */
    public static boolean canWinAt(SwccgGame game, GameState gs, String playerId, PhysicalCard site,
                                   float ourProjectedPower, float ourProjectedAbility, float ourForfeitTotal) {
        if (game == null || gs == null || playerId == null || site == null) {
            return true; // fail-open: never veto blind
        }
        try {
            return canWinAt(gs, game.getModifiersQuerying(), playerId, game.getOpponent(playerId),
                site, ourProjectedPower, ourProjectedAbility, ourForfeitTotal);
        } catch (Exception e) {
            LOG.debug("canWinAt error (fail-open true): {}", e.getMessage());
            return true;
        }
    }

    /**
     * Core overload for callers without an SwccgGame handle (CharacterDeploySiteEvaluator's
     * private methods receive only gs/mq/opponentId). Same semantics as the wrapper above.
     */
    public static boolean canWinAt(GameState gs, ModifiersQuerying mq, String playerId, String opponentId,
                                   PhysicalCard site,
                                   float ourProjectedPower, float ourProjectedAbility, float ourForfeitTotal) {
        if (gs == null || mq == null || playerId == null || opponentId == null || site == null) {
            return true; // fail-open: never veto blind
        }
        try {
            float oppPower = 0f;
            try {
                oppPower = mq.getTotalPowerAtLocation(gs, site, opponentId, false, false);
            } catch (Exception ignore) { /* 0 → clean arm passes → benign no-veto */ }

            // Clean win (CDSE:253-254 parity: powerPass && abilityPass)
            if (ourProjectedPower >= oppPower && ourProjectedAbility >= 4f) {
                return true;
            }

            // V181 tolerance (CDSE:366-391 port): a small gap is a coin-flip worth
            // taking when the drain we stop is >= 2 and the forfeit trade is even.
            float gap = oppPower - ourProjectedPower;
            if (gap > 0f && gap <= POWER_GAP_MAX && ourProjectedAbility >= 4f) {
                float oppDrain = drainAt(gs, mq, site, opponentId);
                if (oppDrain >= DRAIN_MIN) {
                    float theirForfeit = 0f;
                    try {
                        List<PhysicalCard> atSite = gs.getCardsAtLocation(site);
                        if (atSite != null) {
                            for (PhysicalCard pc : atSite) {
                                if (pc == null || pc.getBlueprint() == null) continue;
                                if (!opponentId.equals(pc.getOwner())) continue;
                                if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                theirForfeit += safeForfeit(pc);
                            }
                        }
                    } catch (Exception ignore) { /* 0 → parity passes (matches old favorable-case cap) */ }
                    boolean parity = theirForfeit <= 0f || ourForfeitTotal <= theirForfeit * FORFEIT_PARITY_FACTOR;
                    if (parity) {
                        LOG.warn("canWinAt TOLERANT COMMIT: {} gap={} oppDrain={} ourForfeit={} theirForfeit={}",
                            safeTitle(site), gap, oppDrain, ourForfeitTotal, theirForfeit);
                        return true;
                    }
                    LOG.info("canWinAt NO-PARITY: {} gap={} oppDrain={} ourForfeit={} theirForfeit={} — forfeit mismatch, hold",
                        safeTitle(site), gap, oppDrain, ourForfeitTotal, theirForfeit);
                }
            }
            return false;
        } catch (Exception e) {
            LOG.debug("canWinAt error (fail-open true): {}", e.getMessage());
            return true;
        }
    }

    /**
     * Engine-based force drain amount for {@code playerId} at {@code location}
     * (modifier-aware; the ONE drain metric — move-6 single-detection consolidation).
     * Returns 0 on any error.
     */
    public static float drainAt(SwccgGame game, GameState gs, PhysicalCard location, String playerId) {
        if (game == null) return 0f;
        try {
            return drainAt(gs, game.getModifiersQuerying(), location, playerId);
        } catch (Exception e) {
            return 0f;
        }
    }

    /** Core overload for callers without an SwccgGame handle. */
    public static float drainAt(GameState gs, ModifiersQuerying mq, PhysicalCard location, String playerId) {
        if (gs == null || mq == null || location == null || playerId == null) return 0f;
        try {
            return mq.getForceDrainAmount(gs, location, playerId);
        } catch (Exception e) {
            return 0f;
        }
    }

    /**
     * Engine-based drain delta for {@code playerId}: drainAt(to) - drainAt(from).
     * Positive = the move gains drain; negative = it loses drain.
     */
    public static float drainDelta(SwccgGame game, GameState gs, PhysicalCard from, PhysicalCard to, String playerId) {
        return drainAt(game, gs, to, playerId) - drainAt(game, gs, from, playerId);
    }

    /** V181 parity helper: forfeit value, 0 if the card has none / is unreadable. */
    private static float safeForfeit(PhysicalCard pc) {
        try {
            if (pc == null || pc.getBlueprint() == null) return 0f;
            if (!pc.getBlueprint().hasForfeitAttribute()) return 0f;
            Float f = pc.getBlueprint().getForfeit();
            return f == null ? 0f : f;
        } catch (Exception e) { return 0f; }
    }

    private static String safeTitle(PhysicalCard pc) {
        try { return pc.getTitle(); } catch (Exception e) { return "?"; }
    }
}
