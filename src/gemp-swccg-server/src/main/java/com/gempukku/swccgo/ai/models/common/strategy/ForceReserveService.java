package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════
// ═══ T2 MOVE #1 COMMIT-2 (2026-07-06): SHARED FORCE-RESERVE FACTS ═══
// The ONE per-decision maintenance/DTF/interrupt-reserve computation for BOTH
// bots (rando + chosenone — shared file, precedent CharacterDeploySiteEvaluator
// / MaintenanceFacts / ShieldFacts). Before this class, FIVE consumers each ran
// their own getAllPermanentCards scan per evaluated action:
//   DrawEvaluator.calculateForceToReserve  (V58/V67w/V78/V79 family)
//   PassEvaluator                          (V27.1 DTF + V27 maintenance)
//   MoveEvaluator                          (V29 DTF/grabber + V27 maintenance)
//   DeployEvaluator                        (V24.5 + V29.13 + V38/V53)
//   DeployPhasePlanner                     (V22.3 maintenanceReserve)
// Each caller's inline copy is commented out at its site (feedback_comment_out_
// old_rules) and consumes DecisionContext.getForceReserveFacts() — one cached
// compute() per decision. DeployPhasePlanner has no DecisionContext and calls
// compute() directly at plan creation (already once-per-turn).
//
// SCORE-NEUTRAL BY CONSTRUCTION: detection below is the in-play-gated
// title/icon logic copied from the five sites (post-T2-COMMIT-1 text, i.e.
// MaintenanceFacts.maintainCost basis). Three documented unifications where
// the old sites disagreed with each other:
//   1. DTF/First Strike/IAO ownership: DrawEvaluator treated ANY non-friendly
//      owner as the opponent; PassEvaluator/MoveEvaluator/DeployEvaluator
//      matched gameState.getOpponent(playerId) exactly. Facts uses the exact
//      opponent match (identical in a 2-player game; fails closed on null).
//   2. grabberUnused: MoveEvaluator V29 kept scanning until it found ANY
//      unused grabber; DeployEvaluator V29.13 broke on the FIRST grabber card
//      regardless of state. Facts uses the MoveEvaluator semantic (any unused
//      grabber counts) — identical unless a deck fields 2+ grabbers with mixed
//      used/unused state, which no current deck does.
//   3. undercoverSpyCount: the V38/V53 spy scan had no Zone gate. Facts gates
//      it like everything else — isUndercover() is only ever true for in-play
//      cards, so the gate is a no-op in practice.
// NOT moved here: DeployEvaluator's V48/V79 combined Vader+Verge scan
// (DeployEvaluator ~:226-283) — the Vader-position half is not a shared fact;
// left local per the designer spec's edit list. V59's plan-aware
// pendingDeployCost also stays local (per-candidate, not a shared fact).
//
// SOAK INSTRUMENT (remove after 2 clean full games): on every 20th decision,
// each cache read re-runs compute() and soakCompare() logs
// "MAINT CACHE MISMATCH" at WARN if any field diverges from the cached copy.
// A mismatch means game state changed mid-decision (or nondeterminism) — i.e.
// the cache would NOT have matched a fresh per-caller inline scan.
// ═══════════════════════════════════════════════════════════
public final class ForceReserveService {

    private static final Logger LOG = LogManager.getLogger(ForceReserveService.class);

    private ForceReserveService() { /* static-only */ }

    /**
     * Immutable per-decision force-reserve facts. One instance per
     * DecisionContext (lazy) — consumers read fields directly.
     */
    public static final class Facts {
        /** Opponent has Draw Their Fire in play (V27.1/V29/V29.13/V38/V58 input). */
        public final boolean dtfActive;
        /** Opponent has First Strike in play (V58 input). */
        public final boolean firstStrikeActive;
        /** Opponent has Imperial Arrest Order / Secret Plans in play (V78 input). */
        public final boolean iaoActive;
        /** We have a grabber (Icon.GRABBER) in play with nothing stacked yet (V29/V29.13 input). */
        public final boolean grabberUnused;
        /** Σ MaintenanceFacts.maintainCost over our in-play Icon.MAINTENANCE cards (V22.3/V24.5/V27/V38/V67w input). */
        public final int maintenanceObligation;
        /** Count of our in-play Icon.MAINTENANCE cards. */
        public final int maintenanceCardCount;
        /** Count of our undercover spies in play (V38/V53 input). */
        public final int undercoverSpyCount;
        /** Verge of Greatness active + our Death Star location in play but not at Scarif (V79 input). */
        public final boolean vergeNeedsDeathStarMove;

        Facts(boolean dtfActive, boolean firstStrikeActive, boolean iaoActive,
              boolean grabberUnused, int maintenanceObligation, int maintenanceCardCount,
              int undercoverSpyCount, boolean vergeNeedsDeathStarMove) {
            this.dtfActive = dtfActive;
            this.firstStrikeActive = firstStrikeActive;
            this.iaoActive = iaoActive;
            this.grabberUnused = grabberUnused;
            this.maintenanceObligation = maintenanceObligation;
            this.maintenanceCardCount = maintenanceCardCount;
            this.undercoverSpyCount = undercoverSpyCount;
            this.vergeNeedsDeathStarMove = vergeNeedsDeathStarMove;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Facts)) return false;
            Facts f = (Facts) o;
            return dtfActive == f.dtfActive
                && firstStrikeActive == f.firstStrikeActive
                && iaoActive == f.iaoActive
                && grabberUnused == f.grabberUnused
                && maintenanceObligation == f.maintenanceObligation
                && maintenanceCardCount == f.maintenanceCardCount
                && undercoverSpyCount == f.undercoverSpyCount
                && vergeNeedsDeathStarMove == f.vergeNeedsDeathStarMove;
        }

        @Override
        public int hashCode() {
            int h = (dtfActive ? 1 : 0);
            h = 31 * h + (firstStrikeActive ? 1 : 0);
            h = 31 * h + (iaoActive ? 1 : 0);
            h = 31 * h + (grabberUnused ? 1 : 0);
            h = 31 * h + maintenanceObligation;
            h = 31 * h + maintenanceCardCount;
            h = 31 * h + undercoverSpyCount;
            h = 31 * h + (vergeNeedsDeathStarMove ? 1 : 0);
            return h;
        }

        @Override
        public String toString() {
            return "Facts{dtf=" + dtfActive + ", firstStrike=" + firstStrikeActive
                + ", iao=" + iaoActive + ", grabberUnused=" + grabberUnused
                + ", maintObligation=" + maintenanceObligation
                + ", maintCards=" + maintenanceCardCount
                + ", spies=" + undercoverSpyCount
                + ", vergeNeedsDSMove=" + vergeNeedsDeathStarMove + "}";
        }
    }

    private static final Facts EMPTY =
        new Facts(false, false, false, false, 0, 0, 0, false);

    /**
     * ONE in-play-gated pass over getAllPermanentCards producing the shared
     * facts. game is accepted for signature parity with the other common/
     * helpers (all current detection is GameState-based). Fails closed (EMPTY)
     * on null inputs; per-card exceptions are swallowed like the old inline
     * scans (try/ignore semantics).
     */
    public static Facts compute(SwccgGame game, GameState gameState, String playerId) {
        if (gameState == null || playerId == null) return EMPTY;

        boolean dtfActive = false;
        boolean firstStrikeActive = false;
        boolean iaoActive = false;
        boolean grabberUnused = false;
        int maintenanceObligation = 0;
        int maintenanceCardCount = 0;
        int undercoverSpyCount = 0;
        boolean vergeActive = false;
        boolean deathStarFound = false;
        boolean deathStarAtScarif = false;

        try {
            String opponentId = gameState.getOpponent(playerId);
            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                if (pc == null) continue;
                // T2 COMMIT-1 gate: getAllPermanentCards returns EVERY card ever
                // created including RESERVE_DECK (GameState.java:2800-2802) —
                // only in-play cards may produce reserve obligations.
                Zone z = pc.getZone();
                if (z == null || !z.isInPlay()) continue;

                SwccgCardBlueprint bp = null;
                try {
                    bp = pc.getBlueprint();
                } catch (Exception ignore) { /* treat as no blueprint */ }
                String title = null;
                try {
                    title = pc.getTitle();
                } catch (Exception ignore) { /* fall back to blueprint title */ }
                if (title == null && bp != null) title = bp.getTitle();
                String titleLower = title != null ? title.toLowerCase(Locale.ROOT) : "";

                String owner = pc.getOwner();
                if (opponentId != null && opponentId.equals(owner)) {
                    // ── Opponent-owned threats (V27.1/V58/V78 detections, verbatim) ──
                    if (titleLower.contains("draw their fire"))  dtfActive = true;
                    if (titleLower.contains("first strike"))     firstStrikeActive = true;
                    if (titleLower.contains("imperial arrest")
                            || titleLower.contains("secret plans")) iaoActive = true;
                } else if (playerId.equals(owner)) {
                    // ── Our obligations (V22.3/V27/V29/V38/V53/V67w/V79 detections) ──
                    try {
                        if (bp != null && bp.hasIcon(Icon.MAINTENANCE)) {
                            maintenanceCardCount++;
                            maintenanceObligation += MaintenanceFacts.maintainCost(bp);
                        }
                    } catch (Exception ignore) { /* ignore, like the old inline scans */ }
                    try {
                        if (!grabberUnused && bp != null && bp.hasIcon(Icon.GRABBER)) {
                            List<PhysicalCard> stacked = gameState.getStackedCards(pc);
                            if (stacked == null || stacked.isEmpty()) {
                                grabberUnused = true; // hasn't grabbed yet — needs 1 Force
                            }
                        }
                    } catch (Exception ignore) { /* ignore */ }
                    try {
                        if (pc.isUndercover()) undercoverSpyCount++;
                    } catch (Exception ignore) { /* ignore */ }
                    if (titleLower.contains("on the verge of greatness")
                            || titleLower.contains("taking control of the weapon")) {
                        vergeActive = true;
                    }
                    // V79: detect our Death Star LOCATION (title only — the (V)
                    // marker is Rarity, not title) and whether it orbits Scarif.
                    // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681):
                    // getAtLocation() is ALWAYS null for a mobile-system LOCATION card, so
                    // deathStarAtScarif stayed false forever and the draw-phase 1-Force reserve
                    // (DrawEvaluator V79, both bots) fired every turn even with the DS parked in
                    // Scarif orbit. Use the engine's orbit primitive getSystemOrbited() (same
                    // check as the flip condition, Filters.isOrbiting(Title.Scarif)).
                    if (titleLower.contains("death star") && bp != null
                            && bp.getCardCategory() == CardCategory.LOCATION) {
                        deathStarFound = true;
                        try {
                            // PhysicalCard dsLoc = pc.getAtLocation();
                            // if (dsLoc != null && dsLoc.getTitle() != null
                            //         && dsLoc.getTitle().toLowerCase(Locale.ROOT).contains("scarif")) {
                            //     deathStarAtScarif = true;
                            // }
                            String dsOrbited = pc.getSystemOrbited();
                            if (dsOrbited != null
                                    && dsOrbited.toLowerCase(Locale.ROOT).contains("scarif")) {
                                deathStarAtScarif = true;
                            }
                        } catch (Exception ignore) { /* ignore */ }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("ForceReserveService.compute error: {}", e.getMessage());
        }

        return new Facts(dtfActive, firstStrikeActive, iaoActive, grabberUnused,
            maintenanceObligation, maintenanceCardCount, undercoverSpyCount,
            vergeActive && deathStarFound && !deathStarAtScarif);
    }

    /**
     * SOAK INSTRUMENT (T2 COMMIT-2, 2026-07-06): assert-equal debug log.
     * DecisionContext calls this on every cache READ of every 20th decision
     * with a freshly recomputed Facts. Any divergence between the cached copy
     * and the fresh compute means the cache would not have matched a fresh
     * per-caller inline scan — logged loudly for the 2-game soak, then this
     * method and the DecisionContext hook get commented out.
     */
    public static void soakCompare(Facts cached, Facts fresh, String playerId) {
        if (cached == null || fresh == null) return;
        if (!cached.equals(fresh)) {
            LOG.warn("MAINT CACHE MISMATCH: player={} cached={} fresh={}",
                playerId, cached, fresh);
        }
    }
}
