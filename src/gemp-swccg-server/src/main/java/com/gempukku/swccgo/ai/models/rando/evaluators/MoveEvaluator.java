package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.strategy.MovePredicates;
import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: MOVE (reorg 2026-07-06) ═══
// Owns: the stay/flee/hunt/transit ladder: V29.13 drain-delta (-40/pt, group +/-100..250), V73 hunt +400
// (must beat V29.13's penalty), V137 no-abandon veto, V169 retreat +600, V85/V32/V49 vetoes, V91 +600..800,
// V53 spy-follow +/-300..500. Hub: none. KIND mix (MOVE overall): 26 BANDED / 20 VETO / 3 ORDERING.
// PARITY PAIR: V137 (here) pairs with V136 (deploy-side siting) — change them together or the bot
// deploys to spots it immediately flees.
// NOTE: the V79 parsec PARSE in this file is INERT; live Verge parsec steering = V79b in RandoCalAi
// (+ the V103 fallback in ActionTextEvaluator). LIVE here (2026-07-07): the V79 +500 default-move
// arm (now orbit-gated via getSystemOrbited) and the V79b FLIP-BACK GUARD hard veto
// (post-flip + orbiting Scarif = never initiate the DS hyperspeed move).
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: DEPLOY-2 (V136 twin), MOVE region in ActionTextEvaluator (V67ae + Movement Actions dispatch),
// SVC-SAFETY (V163/V167/V169 loop trio). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
//
// T4.1 MOVE CLOBBER LADDER (2026-07-06, spec: T4_Boundary_Tables_2026-07-06.md §T4.1 + orchestrator
// rulings L1-L4): every move action is scored as fine-grained deltas as before, but now carries a
// RANK (R4 mandatory transit +20000 / R3 survival +12000 / R2 doctrine +6000 / R1 default 0) and
// veto flags, applied by a FINALIZER just before actions.add(). Fines are clamped to ±2800
// ("LADDER CLAMP"); a NEGATIVE clamp hit demotes the claim one band, R2→R1 / R3→R2 ("LADDER
// DEMOTE", ruling L1). R2 claims need strength: own fine >= +200 OR drain-delta >= 2 (ruling L2).
// Veto×rank matrix (ruling L3): cancel-loop (V160) veto beats everything; the canWinAt (V137)
// veto applies ONLY to battle-seeking R2 claims (hunt/contest/attack), never R4/R3/non-battle-R2;
// the V38.3 wrong-direction veto is suppressed by the SPECIFIC V53b transit claim identities.
// Old -9999 hard blocks are now veto-class -100000; old cross-rank early returns are gone —
// V37.1 and V85 survive as R1 weights (-1500 / -800). Winnability + drain metrics route through
// common/strategy/MovePredicates (shared with CharacterDeploySiteEvaluator V181 — parity pair).
// ═══════════════════════════════════════════════════════════
/**
 * Evaluates movement decisions.
 *
 * FULLY PORTED from Python move_evaluator.py with:
 * - Threat level calculation (CRUSH, FAVORABLE, RISKY, DANGEROUS, RETREAT)
 * - Flee analysis with destination checking
 * - Offensive attack opportunity detection
 * - Spread viability analysis with icon bonuses
 *
 * Decision factors:
 * - Power differential at current location (fleeing from danger)
 * - Power differential at destination (moving to advantageous positions)
 * - Spreading out vs consolidating forces
 * - Strategic retreat from dangerous locations
 * - Offensive attacks from uncontested strongholds
 */
public class MoveEvaluator extends ActionEvaluator {

    // Move keywords to identify move actions
    private static final String[] MOVE_KEYWORDS = {
        "Move using", "Shuttle", "Docking bay transit", "Transport",
        "Take off", "Land", "Move to", "Move from"
    };

    // Thresholds (from Python config)
    private static final int POWER_DIFF_FOR_FLEE = 2;
    private static final int OVERKILL_THRESHOLD = 4;
    private static final int ESTABLISH_THRESHOLD = 6;
    private static final int CONTEST_MARGIN = 4;
    private static final int ATTACK_POWER_ADVANTAGE = 4;
    private static final int ATTACK_MIN_POWER = 6;
    private static final float ICON_BONUS = 15.0f;

    // Score deltas (from Python)
    private static final float VERY_GOOD_DELTA = 150.0f;
    private static final float GOOD_DELTA = 10.0f;
    private static final float BAD_DELTA = -10.0f;
    private static final float VERY_BAD_DELTA = -150.0f;

    // ═══ T4.1 LADDER CONSTANTS (2026-07-06) ═══
    // Rank bands (rank-as-band-offset; CombinedEvaluator's additive merge untouched)
    private static final float RANK_R4 = 20000.0f;   // mandatory transit (V53b transit arms; ATE V60 transit shares the band)
    private static final float RANK_R3 = 12000.0f;   // survival (threat RETREAT, V59 DOOMED, V91 landed-ship escape)
    private static final float RANK_R2 = 6000.0f;    // doctrine (hunt/contest/shuttle/consolidate/spy-follow/…)
    private static final float RANK_R1 = 0.0f;       // default — behaves exactly as today
    private static final float LADDER_VETO = -100000.0f;
    private static final float FINE_CLAMP = 2800.0f; // non-base fines clamped to ±2800 ("LADDER CLAMP")
    // Ruling L2: an R2 claim needs strength — claiming rule's own fine >= +200 OR drain-delta >= 2
    private static final float R2_CLAIM_MIN_FINE = 200.0f;
    private static final float R2_CLAIM_MIN_DRAIN_DELTA = 2.0f;
    // Ruling L4 band-assert inputs (verified cross-evaluator bounds, T4_Boundary_Tables §Band check):
    // worst ATE co-sum stack -550 (V29.7-Castle/V67ae -300 + V169 soft -250), best +250 (V35.4);
    // largest R1 fine stack ≈ +1670 (V79 orbit-Scarif +1500 dominates).
    private static final float ATE_CROSS_NEG = 550.0f;
    private static final float ATE_CROSS_POS = 250.0f;
    private static final float R1_FINE_CEILING = 1670.0f;
    private static boolean ladderBandsChecked = false;

    // ═══ T4.1 per-action ladder state (reset via ladderResetForAction at each action) ═══
    private int ladderRank;                    // 1..4, max of matched rank-predicates, default 1
    private boolean ladderVetoHard;            // absolute veto class (V47/V49/V135/V60-landspeed/V38.3-Castle)
    private String ladderVetoHardReason;
    private boolean ladderCanWinVeto;          // V137 winnability failed shared canWinAt — L3: battle-seeking R2 only
    private String ladderCanWinVetoReason;
    private boolean ladderBattleSeekingClaim;  // an accepted hunt/contest/attack R2 claim exists (L3 veto scope)
    private boolean ladderMandatoryTransit;    // set ONLY by the specific V53b transit claim identities (L3 carve-out key)
    private boolean ladderWrongDirVeto;        // V38.3 wrong-direction — deferred so the transit carve-out can suppress it
    private String ladderWrongDirVetoReason;
    private boolean ladderRankMoveRan;         // rankMoveFromLocation executed (gates the finalizer's default -50)

    // Threat levels (matching Python ThreatLevel enum)
    private enum ThreatLevel {
        CRUSH, FAVORABLE, RISKY, DANGEROUS, RETREAT
    }

    // Track cards we've already tried moving this turn
    private Set<String> pendingMoveCardIds = new HashSet<>();
    private int lastTurnNumber = -1;

    public MoveEvaluator() {
        super("Move");
    }

    public void resetPendingMoves() {
        pendingMoveCardIds.clear();
    }

    // ═══════════════════════════════════════════════════════════
    // T4.1 LADDER MACHINERY (2026-07-06)
    // ═══════════════════════════════════════════════════════════

    /** Reset per-action ladder state. Called once per evaluated move action. */
    private void ladderResetForAction() {
        ladderRank = 1;
        ladderVetoHard = false;
        ladderVetoHardReason = null;
        ladderCanWinVeto = false;
        ladderCanWinVetoReason = null;
        ladderBattleSeekingClaim = false;
        ladderMandatoryTransit = false;
        ladderWrongDirVeto = false;
        ladderWrongDirVetoReason = null;
        ladderRankMoveRan = false;
    }

    /** R4 claim — MANDATORY TRANSIT. Keyed claim identity (V53b arms only) also arms the V38.3 carve-out (ruling L3). */
    private void ladderClaimR4Transit(String tag) {
        ladderRank = Math.max(ladderRank, 4);
        ladderMandatoryTransit = true;
        logger.info("LADDER: R4 TRANSIT claim by {}", tag);
    }

    /** R3 claim — SURVIVAL (retreat/escape). Not subject to the L2 strength gate. */
    private void ladderClaimR3(String tag) {
        ladderRank = Math.max(ladderRank, 3);
        logger.info("LADDER: R3 SURVIVAL claim by {}", tag);
    }

    /**
     * R2 claim — DOCTRINE. Ruling L2 strength gate: accepted only when the claiming
     * rule's own fine is >= +200 OR its drain-delta is >= 2. battleSeeking marks
     * hunt/contest/attack claims — the only claims the V137 canWinAt veto may kill (ruling L3).
     *
     * @return true if the claim was accepted
     */
    private boolean ladderClaimR2(String tag, float ownFine, float drainDelta, boolean battleSeeking) {
        if (ownFine >= R2_CLAIM_MIN_FINE || drainDelta >= R2_CLAIM_MIN_DRAIN_DELTA) {
            ladderRank = Math.max(ladderRank, 2);
            if (battleSeeking) ladderBattleSeekingClaim = true;
            logger.info("LADDER: R2 DOCTRINE claim by {} (fine {}, drainDelta {}, battleSeeking={})",
                tag, (int) ownFine, (int) drainDelta, battleSeeking);
            return true;
        }
        logger.info("LADDER: R2 claim by {} REJECTED — weak claim (fine {} < +{}, drainDelta {} < {}) (ruling L2)",
            tag, (int) ownFine, (int) R2_CLAIM_MIN_FINE, (int) drainDelta, (int) R2_CLAIM_MIN_DRAIN_DELTA);
        return false;
    }

    /**
     * Ruling L4: first-use band-integrity assertion. Recomputes the R1 ceiling vs the
     * R2 floor from the live constants and logger.error's on inversion (no crash).
     */
    private void ladderAssertBandsOnce() {
        if (ladderBandsChecked) return;
        ladderBandsChecked = true;
        float r2Floor = RANK_R2 - FINE_CLAMP - ATE_CROSS_NEG;
        float r1Ceiling = RANK_R1 + R1_FINE_CEILING + ATE_CROSS_POS;
        if (r2Floor <= r1Ceiling) {
            logger.error("LADDER BAND INVERSION: R2 floor {} <= R1 ceiling {} — rank bands no longer separate "
                + "(RANK_R2={}, FINE_CLAMP={}, ATE_CROSS_NEG={}, R1_FINE_CEILING={}, ATE_CROSS_POS={}). "
                + "Rebalance before trusting MOVE decisions.",
                r2Floor, r1Ceiling, RANK_R2, FINE_CLAMP, ATE_CROSS_NEG, R1_FINE_CEILING, ATE_CROSS_POS);
        } else {
            logger.info("LADDER BANDS OK: R2 floor {} > R1 ceiling {} (margin {})",
                r2Floor, r1Ceiling, r2Floor - r1Ceiling);
        }
    }

    /**
     * T4.1 FINALIZER — applied once per action just before actions.add().
     * Order: hard veto → V38.3 deferred veto (with the L3 transit carve-out) →
     * canWinAt veto (L3 matrix: battle-seeking R2 only) → fine clamp (±2800, with
     * the L1 negative-clamp demote R2→R1 / R3→R2) → rank base → R1 default -50.
     */
    private void ladderFinalize(EvaluatedAction action) {
        ladderAssertBandsOnce();

        // 1. Hard veto class — absolute (cancel-loop V160 keeps its own -100000 short-circuit upstream).
        if (ladderVetoHard) {
            action.addReasoning("LADDER VETO: " + ladderVetoHardReason, LADDER_VETO);
            logger.warn("LADDER VETO -100000 (hard): {}", ladderVetoHardReason);
            return;
        }

        // 2. V38.3 wrong-direction deferred veto — suppressed by the SPECIFIC transit
        //    claim identities (V53b Safehouse→Corridor / Mapuzo-exit), not by rank==R4 (ruling L3).
        if (ladderWrongDirVeto) {
            if (ladderMandatoryTransit) {
                action.addReasoning("V38.3 wrong-direction suppressed (R4 mandatory transit)", 0.0f);
                logger.warn("V38.3 WRONG DIRECTION suppressed (R4 mandatory transit): {}", ladderWrongDirVetoReason);
            } else {
                action.addReasoning("LADDER VETO: " + ladderWrongDirVetoReason, LADDER_VETO);
                logger.warn("LADDER VETO -100000 (V38.3): {}", ladderWrongDirVetoReason);
                return;
            }
        }

        // 3. canWinAt veto — ruling L3 matrix: ONLY battle-seeking R2 claims (hunt/contest/attack).
        //    Never R4 transit, R3 survival, or non-battle R2. Non-vetoed unwinnable paths keep the
        //    old V137 -800/-1500 weights (applied inline at the V137 block).
        if (ladderCanWinVeto) {
            if (ladderRank == 2 && ladderBattleSeekingClaim) {
                action.addReasoning("LADDER VETO: " + ladderCanWinVetoReason, LADDER_VETO);
                logger.warn("V137 UNWINNABLE (battle-seeking R2) — LADDER VETO -100000: {}", ladderCanWinVetoReason);
                return;
            }
            logger.info("V137 canWinAt veto NOT applied (L3 matrix: rank=R{}, battleSeeking={}) — R1 weights already applied inline",
                ladderRank, ladderBattleSeekingClaim);
        }

        // 4. Fine clamp ±2800 + ruling L1 demote on a NEGATIVE clamp hit (R2→R1, R3→R2; R4 exempt).
        int rank = ladderRank;
        float fines = action.getScore();  // ctor base is 0 → current score == accumulated fines
        if (fines > FINE_CLAMP) {
            action.addReasoning(String.format("LADDER CLAMP: fines %+.0f clamped to %+.0f", fines, FINE_CLAMP),
                FINE_CLAMP - fines);
            logger.warn("LADDER CLAMP: fines {} clamped to +{} on '{}'", (int) fines, (int) FINE_CLAMP,
                action.getDisplayText());
        } else if (fines < -FINE_CLAMP) {
            action.addReasoning(String.format("LADDER CLAMP: fines %+.0f clamped to %+.0f", fines, -FINE_CLAMP),
                -FINE_CLAMP - fines);
            logger.warn("LADDER CLAMP: fines {} clamped to -{} on '{}'", (int) fines, (int) FINE_CLAMP,
                action.getDisplayText());
            if (rank == 2 || rank == 3) {
                rank -= 1;
                action.addReasoning("LADDER DEMOTE: negative clamp hit — claim demoted one band (ruling L1)", 0.0f);
                logger.warn("LADDER DEMOTE: negative clamp hit — R{} demoted to R{} (ruling L1)", rank + 1, rank);
            }
        }

        // 5. Rank base (band offset).
        if (rank >= 4) {
            action.addReasoning("LADDER: R4 MANDATORY TRANSIT base", RANK_R4);
        } else if (rank == 3) {
            action.addReasoning("LADDER: R3 SURVIVAL base", RANK_R3);
        } else if (rank == 2) {
            action.addReasoning("LADDER: R2 DOCTRINE base", RANK_R2);
        } else if (ladderRankMoveRan && ladderRank == 1) {
            // Default -50 moved here from the tail of rankMoveFromLocation (T4.1): applied only
            // when no rank claim was accepted (rank==R1; a demoted claim keeps its reason and is
            // NOT re-penalized) and only for actions that went through rankMoveFromLocation —
            // exactly the population that could reach the old line.
            action.addReasoning("No strategic reason to move", -50.0f);
        }
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();
        if (!"CARD_ACTION_CHOICE".equals(decisionType) && !"ACTION_CHOICE".equals(decisionType)) {
            return false;
        }

        // Must be our turn
        if (context.getGameState() != null && !context.isMyTurn()) {
            return false;
        }

        // Check if any action is a move action
        List<String> actionTexts = context.getActionTexts();
        if (actionTexts != null) {
            for (String actionText : actionTexts) {
                if (isMoveAction(actionText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMoveAction(String actionText) {
        if (actionText == null) return false;
        for (String keyword : MOVE_KEYWORDS) {
            if (actionText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        Side mySide = context.getSide();

        logger.info("[MoveEvaluator] Evaluating move decision");

        // Reset pending move tracking at the start of each turn
        if (context.getTurnNumber() != lastTurnNumber) {
            resetPendingMoves();
            lastTurnNumber = context.getTurnNumber();
        }

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();

        if (actionIds == null || actionTexts == null) {
            return actions;
        }

        logger.debug("[MoveEvaluator] Phase={}, actions={}", context.getPhase(), actionIds.size());

        // 2026-06-02 BLOCKED-RESPONSE GATE (Steve, Ponda Baba MOVE loop):
        // DecisionTracker.blockLastActionOnCancel adds the offending move action
        // to blockedResponses after the 3-strike cancel-loop detector trips.
        // Replay: Rando picked move 'Move using landspeed' (actionId='2'),
        // sub-decision "Choose where to move Ponda Baba" scored all destinations
        // negative → empty Done → cancel-loop fired → blocked '2'. But next
        // phase tick MoveEvaluator re-scored the same Move action positively
        // and Rando picked it again. ActionTextEvaluator already honors
        // context.getBlockedResponses() (line 86-99); DeployEvaluator was
        // patched 2026-05-31 (commit 5df527801); MoveEvaluator had the same
        // hole. Now: any actionId or actionText in the block set gets hard
        // blocked -9999 so Rando picks something else (different move target
        // or Pass).
        java.util.Set<String> v160MoveBlocked = context.getBlockedResponses();
        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);
            String cardIdStr = (cardIds != null && i < cardIds.size()) ? cardIds.get(i) : null;

            // Only handle move-related actions
            if (!isMoveAction(actionText)) {
                continue;
            }

            // Blocked-response gate: if the cancel-loop detector added this
            // actionId/actionText to the block set, hard-block here too.
            if (v160MoveBlocked != null && !v160MoveBlocked.isEmpty()
                    && (v160MoveBlocked.contains(actionId) || v160MoveBlocked.contains(actionText))) {
                // V169 (Steve, 2026-06): if the MOVER is ENDANGERED (outpowered at its current
                // site), keep the move attemptable — retreat is how it survives. Asajj's
                // landspeed move was cancel-blocked here (V41 had blocked her safe
                // destinations) and she was beaten 6v27 next turn.
                // V169 UPDATED 2026-07-06 (audit cross-brain-1): the soft penalty now lives
                // ONLY in ActionTextEvaluator (-250, retry-budgeted). The old code here added
                // a SECOND copy at double strength (EvaluatedAction ctor -400 PLUS
                // addReasoning -400 = -800, both add) on the same actionId, then 'continue'd
                // past rankMoveFromLocation so the endangered mover's own retreat bonuses
                // (+150 RETREAT tier) never attached. Merged ~-1050 vs Pass +5: the retry
                // this rule exists for was mathematically impossible. Now: endangered movers
                // fall through to normal scoring (retreat bonuses accrue); everyone else
                // keeps the hard block.
                boolean v169EndangeredMover = false;
                try {
                    if (cardIdStr != null && context.getGameState() != null && context.getGame() != null
                            && context.getPlayerId() != null) {
                        com.gempukku.swccgo.game.PhysicalCard v169Mover =
                            context.getGameState().findCardById(Integer.parseInt(cardIdStr));
                        com.gempukku.swccgo.game.PhysicalCard v169At =
                            v169Mover != null ? v169Mover.getAtLocation() : null;
                        if (v169At != null) {
                            String v169Pid = context.getPlayerId();
                            String v169Opp = context.getGameState().getOpponent(v169Pid);
                            float v169Our = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(context.getGameState(), v169At, v169Pid, false, false);
                            float v169Their = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(context.getGameState(), v169At, v169Opp, false, false);
                            v169EndangeredMover = v169Their > v169Our;
                        }
                    }
                } catch (Exception ignore) { }
                if (v169EndangeredMover) {
                    // V169 UPDATED 2026-07-06 (audit cross-brain-1): no penalty here, no 'continue'.
                    logger.warn("V169 MoveEvaluator: endangered mover '{}' blocked-but-excused; soft penalty owned by ActionTextEvaluator (-250), falling through to retreat scoring", actionText);
                } else {
                    // V169 UPDATED 2026-07-06: hard block unchanged, wrapped in else so the
                    // endangered-mover path above falls through instead of hitting it.
                    // V160 UPDATED 2026-07-06 T4.1: cancel-loop veto raised from -9999 to
                    // ladder class -100000, above all score bands including R4 transit.
                    EvaluatedAction blockedMove = new EvaluatedAction(actionId, ActionType.MOVE, 0.0f, actionText);
                    blockedMove.addReasoning("CANCEL-LOOP BLOCK: this move led to repeated Done-cancels — try something else (LADDER VETO)", -100000.0f);
                    logger.warn("MoveEvaluator: actionId='{}' is in blockedResponses → -100000 (V160 cancel-loop LADDER VETO)", actionId);
                    actions.add(blockedMove);
                    continue;
                }
            }

            // === SPECIAL CASES: Passenger/Pilot capacity slots ===
            if (actionLower.contains("passenger capacity slot")) {
                logger.info("[MoveEvaluator] SKIP passenger slot move - NEVER good");
                continue;  // Let ActionTextEvaluator's -100 apply
            }

            if (actionLower.contains("pilot capacity slot")) {
                EvaluatedAction pilotAction = new EvaluatedAction(
                    actionId, ActionType.MOVE, 100.0f, actionText
                );
                pilotAction.addReasoning("Move to pilot slot - adds power!", 50.0f);
                actions.add(pilotAction);
                logger.info("[MoveEvaluator] Strongly prefer pilot capacity slot move");
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.MOVE,
                0.0f,  // Start at 0 - let analysis determine score
                actionText
            );

            // T4.1 (2026-07-06): reset the per-action ladder state (rank/veto flags)
            // before any rule below can claim a band or set a veto.
            ladderResetForAction();

            // === Get the card being moved ===
            PhysicalCard cardToMove = null;
            if (cardIdStr != null && game != null) {
                try {
                    int cardId = Integer.parseInt(cardIdStr);
                    cardToMove = gameState.findCardById(cardId);
                } catch (Exception e) {
                    logger.debug("[MoveEvaluator] Could not find card: {}", e.getMessage());
                }
            }

            // === V79 (Steve, 2026-05-15): VERGE OF GREATNESS — MOVE DEATH STAR TOWARD SCARIF ===
            // Rando-as-Krennic must shepherd the Death Star from parsec 4 to orbit Scarif.
            // Death Star (V) starts at parsec 4 with hyperspeed 2. Scarif is at parsec 7.
            //   Turn 1: parsec 4 → 6 (closer to Scarif)
            //   Turn 2: parsec 6 → 7; engine then offers "orbit Scarif" option.
            //
            // Title check is just "death star" — the (V) is a Rarity.V marker, NOT in
            // the title string (Death Star and Death Star (V) share Title.Death_Star).
            // Since Verge of Greatness only enables the Set 16 Death Star, the title
            // match is sufficient to identify the Krennic deck's Death Star.
            if (cardToMove != null && cardToMove.getTitle() != null
                && cardToMove.getTitle().toLowerCase(Locale.ROOT).contains("death star")
                && gameState != null && playerId != null) {
                try {
                    // Verify Verge of Greatness is on Rando's table
                    boolean v79Verge = false;
                    boolean v79AtScarif = false;
                    for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                        if (pc == null || !playerId.equals(pc.getOwner())) continue;
                        if (pc.getBlueprint() == null) continue;
                        com.gempukku.swccgo.common.Zone z = pc.getZone();
                        if (z == null || !z.isInPlay()) continue;
                        String t = pc.getTitle() != null ? pc.getTitle().toLowerCase(Locale.ROOT) : "";
                        if (t.contains("on the verge of greatness")
                                || t.contains("taking control of the weapon")) {
                            v79Verge = true;
                        }
                    }
                    // Current location of Death Star
                    // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681): getAtLocation()
                    // is ALWAYS null for the Death Star mobile-system LOCATION card (every DS move
                    // logged 'Card not at a location'), so v79AtScarif stayed false forever and the
                    // +500 default-move bonus below fired every single turn — post-flip Rando
                    // hokey-pokeyed the DS out of/into Scarif orbit on turns 3-5. Use the engine's
                    // own orbit primitive instead: getSystemOrbited() holds the orbited system's
                    // TITLE ("Scarif"), the exact check the flip condition itself uses
                    // (Filters.isOrbiting(Title.Scarif), Card216_011:122).
                    String v79Orbited = cardToMove.getSystemOrbited();
                    if (v79Orbited != null && v79Orbited.toLowerCase(Locale.ROOT).contains("scarif")) {
                        v79AtScarif = true;
                    }
                    if (v79Verge && !v79AtScarif) {
                        // V79 (Steve, 2026-05-15 update): Scarif is at parsec 7.
                        // Death Star starts at parsec 4 with hyperspeed 2:
                        //   Turn 1: parsec 4 → 6 (closer to Scarif)
                        //   Turn 2: parsec 6 → 7 (the engine then offers "orbit Scarif")
                        // Penalize moves AWAY from parsec 7. Reward moves toward.
                        String v79ActionLower = action.getDisplayText() != null
                            ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                        if (v79ActionLower.contains("orbit") && v79ActionLower.contains("scarif")) {
                            // Orbit Scarif — finalize the move
                            action.addReasoning(
                                "V79 DEATH STAR ORBIT SCARIF: arrive at Scarif — must take this!",
                                1500.0f);
                            logger.warn("V79 DEATH STAR ORBIT SCARIF: '{}' → +1500", v79ActionLower);
                        } else {
                            // Parse destination parsec from action text (e.g., "parsec 6")
                            java.util.regex.Matcher v79m = java.util.regex.Pattern.compile(
                                "parsec\\s+(\\d+)").matcher(v79ActionLower);
                            Integer destParsec = null;
                            // 2026-06-28 (Steve) FIX: action text reads "...at parsec OLD to ...at
                            // parsec NEW", so .find() (first match) grabbed the SOURCE parsec (always
                            // the Death Star's current 4) and scored EVERY move -300 "wrong direction" —
                            // it never steered toward Scarif (replay: it wandered 4->2->0->1). Take the
                            // LAST match = the DESTINATION parsec. (dest-only text still works: last==only.)
                            // NOTE 2026-07-01: this branch is effectively INERT for Verge of Greatness —
                            // the live "Move using hyperspeed" action text carries NO parsec at all, so
                            // destParsec stays null here. The real steering is V79b in RandoCalAi (~692),
                            // which handles the separate MULTIPLE_CHOICE "Choose parsec to move to"
                            // decision. Keep this parse as a harmless fallback for texts that DO embed
                            // a parsec; do not spend time "fixing" it — fix V79b instead.
                            while (v79m.find()) {
                                try { destParsec = Integer.parseInt(v79m.group(1)); }
                                catch (Exception e) { /* ignore */ }
                            }
                            if (destParsec != null) {
                                int distFromScarif = Math.abs(destParsec - 7);
                                if (distFromScarif == 0) {
                                    // Parsec 7 — at Scarif; engine should offer orbit option
                                    action.addReasoning(
                                        "V79 DEATH STAR → parsec 7 (Scarif's parsec) — take orbit option next!",
                                        1200.0f);
                                    logger.warn("V79 DEATH STAR → parsec 7 → +1200");
                                } else if (distFromScarif == 1) {
                                    // Parsec 6 or 8 — one hop from Scarif
                                    action.addReasoning(
                                        "V79 DEATH STAR → parsec " + destParsec + " (1 hop from Scarif at 7)",
                                        1000.0f);
                                    logger.warn("V79 DEATH STAR → parsec {} → +1000", destParsec);
                                } else if (destParsec > 4) {
                                    // 5+, but not 6-8 — still moving toward higher parsecs
                                    action.addReasoning(
                                        "V79 DEATH STAR → parsec " + destParsec + " (toward Scarif)",
                                        700.0f);
                                    logger.warn("V79 DEATH STAR → parsec {} → +700", destParsec);
                                } else {
                                    // Parsec 0-4 — backward direction
                                    action.addReasoning(
                                        "V79 DEATH STAR → parsec " + destParsec
                                            + " — WRONG DIRECTION (Scarif is at 7)",
                                        -300.0f);
                                    logger.warn("V79 DEATH STAR WRONG WAY: parsec {} → -300", destParsec);
                                }
                            } else {
                                // No parsec parseable — default Death Star move bonus
                                action.addReasoning(
                                    "V79 DEATH STAR MOVE: Verge active, default move",
                                    500.0f);
                                logger.warn("V79 DEATH STAR MOVE (no parsec parsed): '{}' → +500", v79ActionLower);
                            }
                        }
                    }
                    // === V79b FLIP-BACK GUARD (Steve, 2026-07-07): VERGE POST-FLIP — STAY IN ORBIT ===
                    // Once On The Verge Of Greatness has flipped (Taking Control Of The Weapon),
                    // moving the Death Star OUT of Scarif orbit is pure self-harm: it un-satisfies
                    // the parsed flip condition ('Death Star orbiting Scarif') and drops the flipped
                    // side's 'At Death Star, system it orbits, and sites related to either, your
                    // total battle destiny is +1 (+2...)' umbrella over the Scarif sites
                    // (Card216_011_BACK — NOTE this objective's own printed flip-back is leader-based,
                    // 'Flip if you do not have a leader at a Scarif battleground site', not
                    // orbit-based; the guard is V22.2 protection-class regardless: the toggle wasted
                    // moves + 1-Force reservations all game in Game9f3c46b00681, and any orbit-based
                    // flip-back objective would lose outright). Hard veto (T4.1 ladder veto class) —
                    // the hyperspeed move is never initiated post-flip while orbiting. Pre-flip
                    // steering (4->6->7->orbit) is untouched by this branch, and a post-flip DS
                    // knocked into deep space still enters the steering branch above to re-orbit.
                    if (v79Verge && v79AtScarif) {
                        boolean v79Flipped = false;
                        try {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v79Analyzer =
                                context.getObjectiveAnalyzer();
                            v79Flipped = v79Analyzer != null && v79Analyzer.isAnalyzed() && v79Analyzer.isFlipped();
                        } catch (Exception ex) {
                            logger.debug("V79b flip-state check error: {}", ex.getMessage());
                        }
                        if (v79Flipped) {
                            ladderVetoHard = true;
                            ladderVetoHardReason = "V79b FLIP-BACK GUARD: objective flipped + Death Star orbiting Scarif"
                                + " — leaving orbit un-satisfies 'Death Star orbiting Scarif'; stay parked";
                            logger.warn("V79b FLIP-BACK GUARD: post-flip Death Star orbiting Scarif — hyperspeed move VETOED ('{}')", actionText);
                        } else {
                            // Pre-flip + already orbiting (waiting on Krennic/Tarkin at a Scarif
                            // battleground site): no steering bonus (v79AtScarif now detects orbit
                            // correctly), no veto — base fines (~-110) lose to Pass (~+28) on their
                            // own. Boundary math in the 2026-07-07 changelog entry.
                            logger.info("V79 DEATH STAR: orbiting Scarif pre-flip — no move bonus, holding for flip");
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V79 Death Star move check error: {}", e.getMessage());
                }
            }

            // === V25: NEVER MOVE A PILOT OFF THEIR SHIP ===
            // Pilots aboard ships (especially capital ships like Executor) should NEVER shuttle off.
            // Removing the pilot unpilots the ship, losing system control and making it vulnerable.
            // This was catastrophic in testing: Piett shuttled off Executor, got killed alone at CC,
            // and Rando lost 16 Force including the entire TDIGWATT engine from hand.
            if (cardToMove != null && cardToMove.isPilotOf()) {
                PhysicalCard ship = cardToMove.getAttachedTo();
                String shipName = (ship != null && ship.getTitle() != null) ? ship.getTitle() : "unknown ship";
                String pilotName = (cardToMove.getTitle() != null) ? cardToMove.getTitle() : "pilot";
                action.addReasoning("V25 PILOT LOCK: " + pilotName + " is piloting " + shipName
                    + " — NEVER leave the ship!", -500.0f);
                logger.warn("V25 PILOT LOCK: {} is piloting {} — blocking move (-500)", pilotName, shipName);
            }

            // === V47: LANDO AT CC — NEVER MOVE ===
            // Lando at a Cloud City site should STAY PUT. He establishes occupation for
            // the objective. V32 SOLO ESCAPE was moving him because ability < 4, but
            // that's wrong — Lando's JOB is to sit at CC sites for drains/occupation.
            // V47 UPDATED 2026-07-06 (audit move-8): the -9999 lock used to fire for ANY
            // *lando* card at ANY site whose title contained 'platform' (real false positives:
            // Endor: Landing Platform (Docking Bay), Coruscant: Private Platform, Kashyyyk:
            // Skyhook Platform), on any deck, with no danger exit, burying every retreat rule
            // (RETREAT +150, V22.5 +160, V53 +500). Now gated on (a) an active Cloud City
            // occupation objective that still wants Lando at THIS site, and (b) survivability.
            if (cardToMove != null && cardToMove.getTitle() != null
                && cardToMove.getTitle().toLowerCase(Locale.ROOT).contains("lando")) {
                PhysicalCard currentLoc = cardToMove.getAtLocation();
                if (currentLoc != null && currentLoc.getTitle() != null) {
                    String locLower = currentLoc.getTitle().toLowerCase(Locale.ROOT);
                    // V47 UPDATED 2026-07-06: every Cloud City site title starts with
                    // "Cloud City: " (see Title.java), so the 'cloud city' fragment alone covers
                    // the whole CC site set incl. East Platform (Docking Bay). The generic
                    // 'platform' fragment matched non-CC sites and is commented out; the other
                    // CC-specific fragments are kept (redundant but harmless).
                    boolean isAtCC = locLower.contains("cloud city") || locLower.contains("dining room")
                        || locLower.contains("upper walkway") || locLower.contains("carbonite")
                        || locLower.contains("security tower") // || locLower.contains("platform") // V47 UPDATED 2026-07-06: generic substring, false-positived on Endor/Coruscant/Kashyyyk platforms
                        || locLower.contains("lower corridor");
                    if (isAtCC) {
                        // V47 UPDATED 2026-07-06 gate (a): objective. Only lock when OUR analyzed
                        // objective is a Bespin/Cloud City occupation objective (V22.5 detector)
                        // and this site still serves it: pre-flip = flip-condition site (the
                        // occupation Lando is establishing), post-flip = flip-back protection
                        // site (the occupation Lando is defending). No CC objective, or a CC
                        // site the objective no longer cares about, = no lock.
                        boolean v47ObjectiveWantsLandoHere = false;
                        try {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v47Analyzer =
                                context.getObjectiveAnalyzer();
                            if (v47Analyzer != null && v47Analyzer.isAnalyzed()
                                    && v47Analyzer.needsBespinSystemPresence()) {
                                v47ObjectiveWantsLandoHere = v47Analyzer.isFlipped()
                                    ? v47Analyzer.isFlipBackProtectionLocation(currentLoc.getTitle())
                                    : v47Analyzer.isObjectiveRelevantLocation(currentLoc.getTitle());
                            }
                        } catch (Exception e) {
                            logger.debug("V47 objective gate error: {}", e.getMessage());
                        }
                        // V47 UPDATED 2026-07-06 gate (b): survivability. Skip the lock when
                        // Lando's side is outpowered past the RETREAT threshold used by
                        // calculateThreatLevel (powerDiff < RandoConfig.BATTLE_DANGER_THRESHOLD,
                        // -6): a dead Lando occupies nothing, let the retreat tier take over.
                        boolean v47Survivable = true;
                        float v47PowerDiff = 0;
                        try {
                            String v47Opp = gameState.getOpponent(playerId);
                            float v47Our = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, currentLoc, playerId, false, false);
                            float v47Their = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, currentLoc, v47Opp, false, false);
                            // V47 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T5 Lando):
                            // survivability said powerDiff=-2 (6v8 raw) while the armed reality was
                            // 6v11+ — weapon-adjust their power (V29.7 heuristic) so the RETREAT
                            // threshold sees the real fight. Threshold itself unchanged.
                            v47Their += oppWeaponBonusAt(gameState, currentLoc, v47Opp);
                            v47PowerDiff = v47Our - v47Their;
                            v47Survivable = v47PowerDiff >= RandoConfig.BATTLE_DANGER_THRESHOLD;
                        } catch (Exception e) {
                            logger.debug("V47 survivability gate error: {}", e.getMessage());
                        }
                        if (v47ObjectiveWantsLandoHere && v47Survivable) {
                            // V47 UPDATED 2026-07-06 T4.1: -9999 addReasoning converted to the ladder
                            // hard-veto class (-100000 at the finalizer). Today's gates (a)+(b) kept
                            // unchanged — semantics identical, magnitude now band-proof.
                            ladderVetoHard = true;
                            ladderVetoHardReason = "V47 LANDO STAY: Lando at " + currentLoc.getTitle()
                                + " — stay for occupation! Don't move!";
                            logger.warn("V47 LANDO STAY: Lando at {} — LADDER VETO on move!", currentLoc.getTitle());
                        } else {
                            logger.warn("V47 LANDO STAY skipped at {}: objectiveWantsHere={}, survivable={} (powerDiff={})",
                                currentLoc.getTitle(), v47ObjectiveWantsLandoHere, v47Survivable, (int)v47PowerDiff);
                        }
                    }
                }
            }

            // === V29: FORCE RESERVE CHECK FOR MOVES ===
            // Moving costs Force. Save Force for:
            //   1. DTF interrupt tax (1 Force if Draw Their Fire on table)
            //   2. Grabber shield activation (1 Force if grabber in play and hasn't grabbed yet)
            //   3. Critical interrupts in hand (Ghhhk, Houjix, Out Of Nowhere)
            if (game != null && gameState != null) {
                try {
                    int forcePile = 0;
                    java.util.List<PhysicalCard> fpCards = gameState.getCardPile(playerId,
                        com.gempukku.swccgo.common.Zone.FORCE_PILE, false);
                    if (fpCards != null) forcePile = fpCards.size();

                    // T2 MOVE #1 COMMIT-2 (2026-07-06): DTF + grabber facts from the shared
                    // per-decision ForceReserveService cache, which preserves this block's
                    // in-play-gated DTF detection and any-unused-grabber semantics. V29 weights
                    // (-100/-150/-60) unchanged. Old inline scan removed 2026-07-13; see git.
                    boolean dtfActive = context.getForceReserveFacts().dtfActive;
                    boolean grabberNeedsForce = context.getForceReserveFacts().grabberUnused;

                    // Calculate total Force we should reserve
                    int reserveNeeded = 0;
                    if (dtfActive) reserveNeeded += 1;       // DTF interrupt tax
                    if (grabberNeedsForce) reserveNeeded += 1; // Grabber activation

                    // Check hand for critical interrupts (Ghhhk, etc.)
                    boolean hasCriticalInterrupt = false;
                    java.util.List<PhysicalCard> hand = gameState.getHand(playerId);
                    if (hand != null) {
                        for (PhysicalCard hCard : hand) {
                            if (hCard == null || hCard.getBlueprint() == null) continue;
                            String hTitle = hCard.getBlueprint().getTitle();
                            if (hTitle != null) {
                                String htl = hTitle.toLowerCase(Locale.ROOT);
                                if (htl.contains("ghhhk") || htl.contains("houjix") || htl.contains("out of nowhere")) {
                                    hasCriticalInterrupt = true;
                                    break;
                                }
                            }
                        }
                    }

                    // Apply penalties based on Force situation
                    // If we have things that need Force (DTF, grabber, interrupts), save it
                    if (reserveNeeded > 0 && forcePile <= reserveNeeded) {
                        float penalty = -100.0f;
                        if (hasCriticalInterrupt) penalty = -150.0f; // Even worse if we have Ghhhk
                        action.addReasoning(String.format(
                            "V29 FORCE RESERVE: Only %d Force, need %d (DTF=%s, grabber=%s) — save Force!",
                            forcePile, reserveNeeded, dtfActive, grabberNeedsForce), penalty);
                        logger.warn("V29 MOVE RESERVE: {} Force left, need {} (DTF={}, grabber={}, interrupt={}) — penalty {}",
                            forcePile, reserveNeeded, dtfActive, grabberNeedsForce, hasCriticalInterrupt, (int)penalty);
                    } else if (reserveNeeded > 0 && forcePile <= reserveNeeded + 1) {
                        action.addReasoning("V29 FORCE RESERVE: Low Force — move cautiously", -60.0f);
                        logger.info("V29 MOVE RESERVE: {} Force, need {} — mild penalty", forcePile, reserveNeeded);
                    }
                } catch (Exception e) {
                    logger.debug("V29 MOVE RESERVE: Error checking force: {}", e.getMessage());
                }
            }

            // === STRATEGIC ANALYSIS ===
            if (cardToMove != null && gameState != null && game != null) {
                PhysicalCard currentLocation = cardToMove.getAtLocation();

                if (currentLocation != null) {
                    // Analyze if we should move FROM this location
                    rankMoveFromLocation(action, gameState, game, playerId, mySide,
                                        cardToMove, currentLocation);

                    // === V27: BUDDY PROTECTION — NEVER leave a vulnerable ally solo ===
                    // Moving a character away from a location can leave their buddy alone
                    // and vulnerable (e.g., moving Emperor away leaves Lando solo).
                    // If removing this character would leave ANY remaining ally below
                    // buddy thresholds (power < 6 AND ability < 4) with NO other allies,
                    // apply a heavy penalty to prevent the move.
                    {
                        List<PhysicalCard> ourCharsHere = new ArrayList<>();
                        for (PhysicalCard card : gameState.getCardsAtLocation(currentLocation)) {
                            if (card != null && playerId.equals(card.getOwner())
                                && card.getBlueprint() != null
                                && card.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                ourCharsHere.add(card);
                            }
                        }
                        // Only matters if exactly 2 of our characters here (moving one leaves one solo)
                        if (ourCharsHere.size() == 2 && ourCharsHere.contains(cardToMove)) {
                            PhysicalCard remainingAlly = null;
                            for (PhysicalCard c : ourCharsHere) {
                                if (c != cardToMove) {
                                    remainingAlly = c;
                                    break;
                                }
                            }
                            if (remainingAlly != null && remainingAlly.getBlueprint() != null) {
                                SwccgCardBlueprint allyBp = remainingAlly.getBlueprint();
                                int allyPower = 0;
                                int allyAbility = 0;
                                if (allyBp.hasPowerAttribute()) {
                                    Float ap = allyBp.getPower();
                                    allyPower = ap != null ? ap.intValue() : 0;
                                }
                                if (allyBp.hasAbilityAttribute()) {
                                    Float aa = allyBp.getAbility();
                                    allyAbility = aa != null ? aa.intValue() : 0;
                                }
                                // V27: Check if opponent has ANY presence at or adjacent to this location
                                String opponentId = game.getOpponent(playerId);
                                float theirPowerHere = 0;
                                try {
                                    theirPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, opponentId, false, false);
                                } catch (Exception e) { /* ignore */ }

                                // V27.2: More permissive buddy protection for MOVES.
                                // During deploy phase, we use strict thresholds (power<6 AND ability<2)
                                // because deploying solo is sometimes necessary for tempo.
                                // But during MOVE phase, abandoning ANY character is bad because:
                                // 1. They're already deployed and in danger
                                // 2. The opponent can move to their location and attack
                                // 3. Solo characters draw unfavorable battles
                                // Protect if: ally power < 6 (even if ability is high like Thrawn's 4)
                                // OR if enemy is already present
                                boolean allyVulnerable = allyPower < RandoConfig.MIN_SOLO_DEPLOY_POWER;
                                boolean enemyThreat = theirPowerHere > 0;

                                if (allyVulnerable || enemyThreat) {
                                    // V59 DOOMED LOCATION: When enemy power is catastrophically higher
                                    // (>= 2x ours OR diff >= +10), the location is already lost. Holding
                                    // both characters means losing BOTH to overflow damage. Forfeit one,
                                    // save the valuable one. FIXES Issue #3 from peaceful-pike replay:
                                    // Yoda + Threepio stuck at LMF(V), Steve attacked 32 vs 9 = 23 overflow.
                                    float ourPowerHere = 0;
                                    try {
                                        ourPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, currentLocation, playerId, false, false);
                                    } catch (Exception e) { /* ignore */ }

                                    boolean doomed = enemyThreat
                                        && (theirPowerHere >= ourPowerHere * 2.0f
                                            || (theirPowerHere - ourPowerHere) >= 10.0f);

                                    if (doomed) {
                                        // Location already lost — don't protect ally, ESCAPE the valuable one
                                        action.addReasoning(String.format(
                                            "V59 DOOMED: %s is a lost position (us %d vs enemy %d) — ESCAPE the valuable character!",
                                            currentLocation.getTitle(), (int)ourPowerHere, (int)theirPowerHere),
                                            200.0f);
                                        // V59 UPDATED 2026-07-06 T4.1: DOOMED escape claims R3 SURVIVAL
                                        // (fine +200 kept; base applied at the finalizer).
                                        ladderClaimR3("V59 DOOMED ESCAPE");
                                        logger.warn("V59 DOOMED: {} at {} is lost ({} vs {}) — buddy protect DISABLED, flee!",
                                            cardToMove.getTitle(), currentLocation.getTitle(),
                                            (int)ourPowerHere, (int)theirPowerHere);
                                    } else {
                                        float buddyPenalty = -150.0f;
                                        if (enemyThreat && allyPower < theirPowerHere) {
                                            // Enemy OVERPOWERS the ally — critical danger
                                            buddyPenalty = -400.0f;
                                        } else if (enemyThreat) {
                                            // Enemy present but ally can hold — still risky
                                            buddyPenalty = -250.0f;
                                        }
                                        action.addReasoning(String.format(
                                            "V27 BUDDY PROTECT: Moving away leaves %s (power %d) ALONE at %s!%s",
                                            remainingAlly.getTitle(), allyPower, currentLocation.getTitle(),
                                            enemyThreat ? " ENEMY POWER=" + (int)theirPowerHere + "!" : ""),
                                            buddyPenalty);
                                        logger.warn("V27 BUDDY PROTECT: {} moving from {} would leave {} (power {}) alone!{}",
                                            cardToMove.getTitle(), currentLocation.getTitle(),
                                            remainingAlly.getTitle(), allyPower,
                                            enemyThreat ? " ENEMY POWER=" + (int)theirPowerHere : "");
                                    }
                                }
                            }
                        }
                    }

                    // === V32: ABILITY >= 4 MOVE PROTECTION ===
                    // SWCCG requires total ability >= 4 at a site to draw battle destiny.
                    // NEVER move a character away from a site if it leaves remaining
                    // friendly ability < 4. This is even more important than V27 buddy
                    // protection because it directly affects battle destiny draws.
                    {
                        // Check if current location is a site (ability rule applies to sites)
                        boolean isSite = currentLocation.getBlueprint() != null
                            && currentLocation.getBlueprint().getCardSubtype() != null
                            && currentLocation.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE;

                        if (isSite) {
                            float totalAbilityHere = 0;
                            float moverAbility = 0;
                            int friendlyCharsHere = 0;

                            // Get mover's ability
                            if (cardToMove.getBlueprint() != null && cardToMove.getBlueprint().hasAbilityAttribute()) {
                                Float ma = cardToMove.getBlueprint().getAbility();
                                moverAbility = ma != null ? ma : 0;
                            }

                            // Sum all friendly character ability at this site
                            for (PhysicalCard c : gameState.getCardsAtLocation(currentLocation)) {
                                if (c == null || !playerId.equals(c.getOwner())) continue;
                                if (c.getBlueprint() == null) continue;
                                if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                friendlyCharsHere++;
                                if (c.getBlueprint().hasAbilityAttribute()) {
                                    Float cAb = c.getBlueprint().getAbility();
                                    totalAbilityHere += (cAb != null ? cAb : 0);
                                }
                            }

                            float abilityAfterMove = totalAbilityHere - moverAbility;

                            // Only applies if there will be remaining characters after move
                            if (friendlyCharsHere > 1 && abilityAfterMove > 0 && abilityAfterMove < 4.0f) {
                                // Moving away drops ability below 4 — heavy penalty
                                float abilityPenalty = -300.0f;

                                // Check if opponent has presence (makes it even worse)
                                String v32Opponent = game.getOpponent(playerId);
                                float theirPower = 0;
                                try {
                                    theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, v32Opponent, false, false);
                                } catch (Exception e) { /* ignore */ }

                                if (theirPower > 0) {
                                    abilityPenalty = -500.0f; // Enemy present + can't draw destiny = disaster
                                }

                                action.addReasoning(String.format(
                                    "V32 ABILITY DANGER: Moving %s away drops ability from %.0f to %.0f (< 4) at %s! NO BATTLE DESTINY!%s",
                                    cardToMove.getTitle(), totalAbilityHere, abilityAfterMove,
                                    currentLocation.getTitle(),
                                    theirPower > 0 ? " ENEMY POWER=" + (int)theirPower : ""),
                                    abilityPenalty);
                                logger.warn("V32 ABILITY MOVE BLOCK: {} moving from {} would leave ability {} < 4!{}",
                                    cardToMove.getTitle(), currentLocation.getTitle(),
                                    abilityAfterMove, theirPower > 0 ? " ENEMY=" + (int)theirPower : "");
                            } else if (friendlyCharsHere == 1 && totalAbilityHere < 4.0f) {
                                // This is the ONLY character and has < 4 ability — moving AWAY is actually GOOD
                                // because we should consolidate with allies who have more ability
                                action.addReasoning(String.format(
                                    "V32 ABILITY SOLO ESCAPE: %s alone with ability %.0f < 4 — move to join allies!",
                                    cardToMove.getTitle(), totalAbilityHere), 50.0f);

                                // === V156 JOIN-GROUP (2026-07-07, move arm; Fel-at-Beach loss, audit deploy-siting-2) ===
                                // The deploy-side V156 hold now blocks CREATING weak solos at BGs on all
                                // turns, but solos still arise (battle losses, forced deploys, pre-fix
                                // states). V32's +50 above was never enough to move them: the ladder needs
                                // an R2 claim (fine >= +200, ruling L2) or the R1 fines (V85 -800 STAY,
                                // V22.2 -120...) bury the move. Claim R2 DOCTRINE when the weak solo has
                                // an adjacent friendly group to join: fine +250 passes the L2 gate,
                                // NON-battle-seeking so the V137 canWinAt veto never applies (ruling L3),
                                // and R2 base 6000 < R3 12000 / R4 20000 so survival/transit still outrank.
                                // Exempt: undercover spies (V170's parked spies sit), opponent presence at
                                // the site (that's a battle/retreat problem, not a join), and a solo doing
                                // READY objective work at a flip-relevant site (same carve the deploy-side
                                // V156 uses, via the shared isV156FlipNotReady predicate).
                                // Destination arm: CardSelectionEvaluator's V156 JOIN-GROUP MODE (same
                                // date) boosts friendly-stack destinations and gates V41 WRONG DIRECTION —
                                // without it this claim moves and the destination surface vetoes (-9999),
                                // the exact V160 cancel-loop that stranded Fel.
                                try {
                                    String v156Opp = game.getOpponent(playerId);
                                    float v156OppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, v156Opp, false, false);
                                    boolean v156AtReadyFlipSite = false;
                                    try {
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v156Oa =
                                            context.getObjectiveAnalyzer();
                                        v156AtReadyFlipSite = v156Oa != null && v156Oa.isAnalyzed()
                                            && currentLocation.getTitle() != null
                                            && v156Oa.isObjectiveRelevantLocation(currentLocation.getTitle())
                                            && !com.gempukku.swccgo.ai.models.common.strategy.CharacterDeploySiteEvaluator
                                                .isV156FlipNotReady(gameState, playerId);
                                    } catch (Exception ignore) { /* false */ }
                                    if (v156OppPowerHere == 0f && !cardToMove.isUndercover() && !v156AtReadyFlipSite) {
                                        // STACK-MATH REFIT (2026-07-07): join by ABILITY-TOTAL, not headcount.
                                        // Pick the adjacent site that becomes destiny-capable (total ability >= 4)
                                        // when this body joins, via the shared MovePredicates.bestJoinDestination.
                                        // (Solo site total == mover ability, since friendlyCharsHere == 1.)
                                        float v156MoverAb = totalAbilityHere;
                                        PhysicalCard v156JoinLoc = com.gempukku.swccgo.ai.models.common.strategy.MovePredicates
                                            .bestJoinDestination(game, gameState, currentLocation, v156MoverAb, playerId);
                                        if (v156JoinLoc != null) {
                                            float v156DestTotal = com.gempukku.swccgo.ai.models.common.strategy.MovePredicates
                                                .siteAbilityTotal(gameState, v156JoinLoc, playerId) + v156MoverAb;
                                            action.addReasoning(String.format(
                                                "V156 JOIN-GROUP: %s (ability %.0f) solo at uncontested %s — join %s (stack reaches ability %.0f)!",
                                                cardToMove.getTitle(), totalAbilityHere, currentLocation.getTitle(),
                                                v156JoinLoc.getTitle(), v156DestTotal), 250.0f);
                                            ladderClaimR2("V156 JOIN-GROUP", 250.0f, 0.0f, false);
                                            logger.warn("V156 JOIN-GROUP: {} (ability {}) solo at {} — R2 claim to join {} (stack ability {})",
                                                cardToMove.getTitle(), (int) totalAbilityHere, currentLocation.getTitle(),
                                                v156JoinLoc.getTitle(), (int) v156DestTotal);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V156 JOIN-GROUP error: {}", e.getMessage());
                                }
                            }
                        }
                    }

                    // === V33: ABILITY 7 BUDDY MOVE PROTECTION ===
                    // Don't move a character away from a site if it drops friendly ability
                    // below the buddy threshold (7). This complements the V33 deploy bonus.
                    {
                        boolean v33IsSite = currentLocation.getBlueprint() != null
                            && currentLocation.getBlueprint().getCardSubtype() != null
                            && currentLocation.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE;

                        if (v33IsSite) {
                            float v33TotalAbility = 0;
                            float v33MoverAbility = 0;
                            int v33FriendlyChars = 0;

                            if (cardToMove.getBlueprint() != null && cardToMove.getBlueprint().hasAbilityAttribute()) {
                                Float v33Ma = cardToMove.getBlueprint().getAbility();
                                v33MoverAbility = v33Ma != null ? v33Ma : 0;
                            }

                            for (PhysicalCard c : gameState.getCardsAtLocation(currentLocation)) {
                                if (c == null || !playerId.equals(c.getOwner())) continue;
                                if (c.getBlueprint() == null) continue;
                                if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                v33FriendlyChars++;
                                if (c.getBlueprint().hasAbilityAttribute()) {
                                    Float cAb = c.getBlueprint().getAbility();
                                    v33TotalAbility += (cAb != null ? cAb : 0);
                                }
                            }

                            float v33AbilityAfterMove = v33TotalAbility - v33MoverAbility;

                            // Only penalize if currently >= 7 and would drop below 7
                            // 2026-06-04 DEFICIT GATE (Steve, Jabba-trapped-at-Audience-Chamber
                            // replay): without this gate, the -150 buddy-break penalty kept
                            // Jabba pinned at Audience Chamber (us 10 vs opp 23 gap 13) because
                            // moving him would drop site ability from 9 to 5. But the site is
                            // already doomed -- retreating saves the character. Skip the -150
                            // when (oppPower - ourPower) at currentLocation >= 6, matching the
                            // V67bn cap of 5 with a one-point hysteresis buffer so we don't
                            // oscillate. Existing 4-condition gate untouched; this just adds
                            // a "site is winnable enough to defend" prerequisite.
                            float v33Gap = 0f;
                            try {
                                String v33Opponent = gameState.getOpponent(playerId);
                                float v33OppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, currentLocation, v33Opponent, false, false);
                                float v33OurPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, currentLocation, playerId, false, false);
                                v33Gap = v33OppPwr - v33OurPwr;
                            } catch (Exception ignore) { /* allow fall-through; treat as 0 gap */ }
                            boolean v33SiteDoomed = v33Gap >= 6f;
                            if (v33FriendlyChars > 1 && v33TotalAbility >= RandoConfig.ABILITY_BUDDY_THRESHOLD
                                && v33AbilityAfterMove < RandoConfig.ABILITY_BUDDY_THRESHOLD && v33AbilityAfterMove >= 4.0f
                                && !v33SiteDoomed) {
                                action.addReasoning(String.format(
                                    "V33 BUDDY BREAK: Moving %s drops ability from %.0f to %.0f (< %d) at %s",
                                    cardToMove.getTitle(), v33TotalAbility, v33AbilityAfterMove,
                                    RandoConfig.ABILITY_BUDDY_THRESHOLD, currentLocation.getTitle()), -150.0f);
                                logger.warn("V33 BUDDY BREAK: {} from {} would drop ability {} → {} (< {})",
                                    cardToMove.getTitle(), currentLocation.getTitle(),
                                    v33TotalAbility, v33AbilityAfterMove, RandoConfig.ABILITY_BUDDY_THRESHOLD);
                            } else if (v33SiteDoomed && v33FriendlyChars > 1
                                && v33TotalAbility >= RandoConfig.ABILITY_BUDDY_THRESHOLD
                                && v33AbilityAfterMove < RandoConfig.ABILITY_BUDDY_THRESHOLD) {
                                logger.warn("V33 BUDDY BREAK SKIP: {} at {} - site hopelessly outgunned (gap {}) - allow retreat",
                                    cardToMove.getTitle(), currentLocation.getTitle(), (int) v33Gap);
                            }
                        }
                    }

                    // === V31: POST-FLIP MOVE CONSOLIDATION ===
                    // After objective flips, if we occupy 3+ objective locations but only
                    // need 2 to prevent flip-back, move characters from the weakest/3rd
                    // location to reinforce the 2 strongest. This reduces the defense burden.
                    {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer moveConsolidateAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (moveConsolidateAnalyzer != null && moveConsolidateAnalyzer.isAnalyzed()
                            && moveConsolidateAnalyzer.isFlipped()) {
                            try {
                                java.util.Set<String> objFrags = moveConsolidateAnalyzer.getFlipConditionLocationFragments();
                                String curLocTitle = currentLocation.getTitle();
                                boolean atObjLoc = false;
                                if (curLocTitle != null) {
                                    for (String frag : objFrags) {
                                        if (curLocTitle.toLowerCase(Locale.ROOT).contains(frag.toLowerCase(Locale.ROOT))) {
                                            atObjLoc = true;
                                            break;
                                        }
                                    }
                                }

                                // Count occupied objective locations and find the weakest
                                java.util.Map<String, Float> objPowerMap = new java.util.LinkedHashMap<>();
                                for (PhysicalCard loc : gameState.getTopLocations()) {
                                    if (loc == null || loc.getTitle() == null) continue;
                                    String lt = loc.getTitle().toLowerCase(Locale.ROOT);
                                    boolean isObj = false;
                                    for (String frag : objFrags) {
                                        if (lt.contains(frag.toLowerCase(Locale.ROOT))) { isObj = true; break; }
                                    }
                                    if (!isObj) continue;
                                    float pwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, playerId, false, false);
                                    if (pwr > 0) objPowerMap.put(loc.getTitle(), pwr);
                                }

                                if (objPowerMap.size() >= 3 && atObjLoc) {
                                    // Find the weakest objective location
                                    String weakestObjLoc = null;
                                    float weakestPwr = Float.MAX_VALUE;
                                    for (java.util.Map.Entry<String, Float> entry : objPowerMap.entrySet()) {
                                        if (entry.getValue() < weakestPwr) {
                                            weakestPwr = entry.getValue();
                                            weakestObjLoc = entry.getKey();
                                        }
                                    }

                                    // If we're AT the weakest location, encourage moving to reinforce a stronger one
                                    if (weakestObjLoc != null && curLocTitle.equals(weakestObjLoc)) {
                                        action.addReasoning(String.format(
                                            "V31 POST-FLIP CONSOLIDATE: At weakest obj loc %s (power %.0f) — move to reinforce stronger position!",
                                            weakestObjLoc, weakestPwr), 200.0f);
                                        logger.warn("V31 POST-FLIP CONSOLIDATE: {} should leave {} (weakest, power={}) to reinforce",
                                            cardToMove.getTitle(), weakestObjLoc, (int)weakestPwr);
                                        // V31 UPDATED 2026-07-06 T4.1: consolidation claims R2 DOCTRINE
                                        // (non-battle; fine +200 passes the L2 strength gate).
                                        ladderClaimR2("V31 POST-FLIP CONSOLIDATE", 200.0f, 0.0f, false);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V31 MOVE CONSOLIDATE: Error: {}", e.getMessage());
                            }
                        }
                    }

                    // === V37: NEVER MOVE FROM BATTLEGROUND TO NON-BATTLEGROUND ===
                    // Moving from a battleground (where you can drain/battle) to a non-battleground
                    // (like Mustafar: Vader's Castle) is almost always wrong. You lose drain potential
                    // and can't initiate battles at non-battleground sites.
                    // Exception: moving to a non-battleground to pick up a character (shuttle), but
                    // that's handled by specific shuttle logic elsewhere.
                    {
                        String moveText37 = action.getDisplayText() != null
                            ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                        // Find destination location
                        PhysicalCard destLoc37 = null;
                        for (PhysicalCard loc37 : gameState.getLocationsInOrder()) {
                            if (loc37 == null || loc37 == currentLocation) continue;
                            String locName37 = loc37.getTitle() != null ? loc37.getTitle().toLowerCase(Locale.ROOT) : "";
                            if (!locName37.isEmpty() && moveText37.contains(locName37)) {
                                destLoc37 = loc37;
                                break;
                            }
                        }

                        if (destLoc37 != null && destLoc37.getBlueprint() != null) {
                            boolean destIsBattleground = false;
                            try {
                                destIsBattleground = game.getModifiersQuerying().isBattleground(gameState, destLoc37, null);
                            } catch (Exception e) { /* ignore */ }

                            boolean currentIsBattleground = false;
                            try {
                                currentIsBattleground = game.getModifiersQuerying().isBattleground(gameState, currentLocation, null);
                            } catch (Exception e) { /* ignore */ }

                            if (currentIsBattleground && !destIsBattleground) {
                                action.addReasoning(String.format(
                                    "V37 NO RETREAT: Moving from battleground %s to non-battleground %s — lose drain and battle ability!",
                                    currentLocation.getTitle(), destLoc37.getTitle()), -800.0f);
                                logger.warn("V37 NO RETREAT: {} from battleground {} to non-battleground {} (-800)",
                                    cardToMove != null ? cardToMove.getTitle() : "?",
                                    currentLocation.getTitle(), destLoc37.getTitle());
                            }

                            // === V135 (Steve, 2026-05-25): SELF-MOVE-TO-FRIEND REQUIRES COMPANION ===
                            //
                            // Some characters' game text says "may move to same site as <Jedi/X>"
                            // — a self-move that should put them next to allies, not alone.
                            // Bug 7a: Rando moved Yoda to a destination with no friendly chars
                            // via this game-text move and put him in danger alone.
                            //
                            // Generalized beyond Yoda: any cardToMove whose game text contains
                            // the self-move-to-friend pattern is gated by destination occupancy.
                            // If destination has zero friendly characters (excluding cardToMove
                            // itself), score -2000 (strong discouragement, still allowed if
                            // other rules outscore it).
                            //
                            // FAIL-OPEN: if blueprint or game text missing, no penalty.
                            if (cardToMove != null && cardToMove.getBlueprint() != null) {
                                String v135Gt = cardToMove.getBlueprint().getGameText();
                                if (v135Gt != null) {
                                    String v135GtLower = v135Gt.toLowerCase(Locale.ROOT);
                                    boolean v135IsSelfMoveToFriend =
                                        v135GtLower.contains("may move to same site as")
                                        || v135GtLower.contains("moves to same site as");
                                    if (v135IsSelfMoveToFriend) {
                                        int v135FriendlyAtDest = 0;
                                        for (PhysicalCard pc : gameState.getCardsAtLocation(destLoc37)) {
                                            if (pc == null || pc.getBlueprint() == null) continue;
                                            if (pc == cardToMove) continue;
                                            if (!playerId.equals(pc.getOwner())) continue;
                                            if (pc.getBlueprint().getCardCategory()
                                                    != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            v135FriendlyAtDest++;
                                        }
                                        if (v135FriendlyAtDest == 0) {
                                            // V135 UPDATED 2026-07-06 T4.1: -2000 (outbiddable by +2000+ stacks)
                                            // strengthened to the ladder hard-veto class — a self-move-to-friend
                                            // that lands ALONE is absolutely blocked.
                                            ladderVetoHard = true;
                                            ladderVetoHardReason = String.format(
                                                "V135 SELF-MOVE-TO-FRIEND ALONE: '%s' would land alone at %s — no friendly characters there",
                                                cardToMove.getTitle(), destLoc37.getTitle());
                                            logger.warn("V135 SELF-MOVE-TO-FRIEND ALONE: {} → {} (0 friendlies) LADDER VETO",
                                                cardToMove.getTitle(), destLoc37.getTitle());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // === V29.12: HUNT DOWN — VADER MUST LEAVE CASTLE AND HUNT ===
                    // When playing Hunt Down V, armed Vader sitting at an uncontested location
                    // (like Vader's Castle) is WASTING turns. The whole point of Hunt Down is
                    // that Vader goes out to fight. If Vader is armed and there are no opponents
                    // at his location, give a massive bonus to move him toward the action.
                    // This overrides the natural tendency to "stay safe" at Castle.
                    {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer huntMoveAnalyzer =
                            context.getObjectiveAnalyzer();
                        // V137b (Steve, 2026-05-28): extend hunt to ALL Dark Jedi, not just
                        // Vader. Steve's Vader/Dooku Hunt Down deck — "both need to
                        // aggressively attack." Dark_Jedi = dark character, ability >= 6,
                        // which is exactly Vader + Dooku/Tyranus. Lower-ability Inquisitors
                        // (Third Sister) stay buddies. Title fallback keeps Vader working
                        // even if a modifier drops his ability below 6.
                        boolean v137bIsHunter = false;
                        if (cardToMove != null && cardToMove.getTitle() != null) {
                            String v137bT = cardToMove.getTitle().toLowerCase(Locale.ROOT);
                            if (v137bT.contains("vader") || v137bT.contains("tyranus")
                                    || v137bT.contains("dooku")) {
                                v137bIsHunter = true;
                            } else {
                                try {
                                    v137bIsHunter = com.gempukku.swccgo.filters.Filters.Dark_Jedi
                                        .accepts(gameState, game.getModifiersQuerying(), cardToMove);
                                } catch (Exception ignore) { /* false */ }
                            }
                        }
                        if (huntMoveAnalyzer != null && huntMoveAnalyzer.isAnalyzed() && huntMoveAnalyzer.isHuntDownV()
                            && v137bIsHunter) {

                            String opponentIdHunt = game.getOpponent(playerId);
                            float theirPowerHere = 0;
                            try {
                                theirPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, currentLocation, opponentIdHunt, false, false);
                            } catch (Exception e) { /* ignore */ }

                            // Check if Vader is armed
                            boolean vaderArmed = false;
                            try {
                                List<PhysicalCard> vAttach = gameState.getAttachedCards(cardToMove);
                                if (vAttach != null) {
                                    for (PhysicalCard att : vAttach) {
                                        if (att != null && att.getBlueprint() != null
                                            && att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                            vaderArmed = true;
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }

                            // If Vader is armed and no opponents here — GO HUNT!
                            if (vaderArmed && theirPowerHere == 0) {
                                // V35: Find opponents, but PRIORITIZE Jedi/Padawan targets
                                boolean opponentsElsewhere = false;
                                String bestTargetLoc = null;
                                float bestTargetPower = 0;
                                String bestJediLoc = null;
                                float bestJediPower = 0;
                                try {
                                    for (PhysicalCard loc : gameState.getTopLocations()) {
                                        if (loc == null || loc == currentLocation) continue;
                                        float opPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, loc, opponentIdHunt, false, false);
                                        if (opPower > 0) {
                                            opponentsElsewhere = true;
                                            if (opPower > bestTargetPower) {
                                                bestTargetPower = opPower;
                                                bestTargetLoc = loc.getTitle();
                                            }
                                            // V35: Check for Jedi/Padawan at this location
                                            for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                                if (c == null || !opponentIdHunt.equals(c.getOwner())) continue;
                                                String cTitle = c.getTitle() != null ? c.getTitle().toLowerCase(Locale.ROOT) : "";
                                                if (isJediOrPadawan(cTitle)) {
                                                    if (opPower > bestJediPower) {
                                                        bestJediPower = opPower;
                                                        bestJediLoc = loc.getTitle();
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if (opponentsElsewhere) {
                                    // V35: Prefer Jedi location over generic highest-power location
                                    String huntTarget = (bestJediLoc != null) ? bestJediLoc : bestTargetLoc;
                                    float huntTargetPower = (bestJediLoc != null) ? bestJediPower : bestTargetPower;
                                    float huntMoveBonus = (bestJediLoc != null)
                                        ? (float) RandoConfig.SCORE_VADER_SEEK_JEDI  // V35: +350 for Jedi
                                        : 200.0f; // Generic opponent
                                    String locName = currentLocation.getTitle() != null
                                        ? currentLocation.getTitle() : "current location";
                                    action.addReasoning(String.format(
                                        "V35 HUNT %s: Armed Vader at %s — GO HUNT! Target: %s (power %.0f)",
                                        bestJediLoc != null ? "JEDI" : "DOWN",
                                        locName, huntTarget != null ? huntTarget : "?", huntTargetPower),
                                        huntMoveBonus);
                                    logger.warn("V35 HUNT {}: Armed Vader at {} — target {} (power {}, bonus +{})",
                                        bestJediLoc != null ? "JEDI" : "DOWN",
                                        locName, huntTarget, (int)huntTargetPower, (int)huntMoveBonus);
                                    // V35/V29.12 UPDATED 2026-07-06 T4.1: the hunt claims R2 DOCTRINE
                                    // (battle-seeking — subject to the V137 canWinAt veto per ruling L3).
                                    // Fine +350 (Jedi) / +200 (generic) passes the L2 strength gate.
                                    ladderClaimR2("V35 HUNT " + (bestJediLoc != null ? "JEDI" : "DOWN"),
                                        huntMoveBonus, 0.0f, true);
                                }
                            }
                        }
                    }

                    // === V137 (Steve, 2026-05-28): MOVE-SIDE WINNABILITY GATE ===
                    // Steve: "It's odd that he deployed then moved to the site with
                    // enemies. Waste of move force. Could have deployed there directly
                    // and battled, or just left the guys there."
                    //
                    // Root cause: V136 (deploy) correctly refused to deploy Vader
                    // directly into Rey+Yoda (solo, can't win), so Vader deployed to a
                    // safe adjacent site. Then V35 HUNT (+350) moved him solo into
                    // Rey+Yoda anyway — wasting move force AND walking into a loss the
                    // deploy logic had already rejected. The two systems contradicted.
                    //
                    // Fix: when a move targets a CONTESTED destination (opp power > 0),
                    // check if the projected team there (mover + friendlies already
                    // present) can win (power >= opp AND total ability >= 4). If not,
                    // penalize hard enough to cancel the V35/V34 contest bonuses so the
                    // character stays put. If the team CAN win, no penalty — and V136
                    // would have allowed a direct deploy there, so no wasteful two-step.
                    {
                        String v137MoveText = action.getDisplayText() != null
                            ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                        PhysicalCard v137Dest = null;
                        for (PhysicalCard loc : gameState.getTopLocations()) {
                            if (loc == null || loc == currentLocation || loc.getTitle() == null) continue;
                            if (v137MoveText.contains(loc.getTitle().toLowerCase(Locale.ROOT))) {
                                v137Dest = loc;
                                break;
                            }
                        }
                        if (v137Dest != null && cardToMove != null && game != null && playerId != null) {
                            try {
                                String v137Opp = game.getOpponent(playerId);
                                float v137OppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, v137Dest, v137Opp, false, false);
                                if (v137OppPower > 0) {
                                    // Projected friendly team at the destination = friendlies
                                    // already there + the WHOLE group at the mover's current
                                    // location (they can move together this phase; the V29.13
                                    // grouping logic brings buddies along). Counting the
                                    // current-location buddies is the key fix per Steve:
                                    // "both times he deployed Vader with a buddy, those two
                                    // absolutely had a chance of winning" — the bug was Vader
                                    // moving SOLO and leaving the buddy. If the combined group
                                    // can win, allow the move (coordinated attack). Only a TRUE
                                    // solo charge (no buddy at current location) into a losing
                                    // fight gets blocked.
                                    float v137OurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, v137Dest, playerId, false, false);
                                    float v137OurAbility = 0f;
                                    for (PhysicalCard fc : gameState.getCardsAtLocation(v137Dest)) {
                                        if (fc == null || fc.getBlueprint() == null) continue;
                                        if (!playerId.equals(fc.getOwner())) continue;
                                        if (fc.getBlueprint().getCardCategory()
                                                != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        Float a = fc.getBlueprint().hasAbilityAttribute()
                                            ? fc.getBlueprint().getAbility() : null;
                                        if (a != null) v137OurAbility += a;
                                    }
                                    // Add the moving group at current location (includes cardToMove
                                    // + buddies that can move together). Power via engine, ability
                                    // summed from blueprints.
                                    v137OurPower += game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, playerId, false, false);
                                    for (PhysicalCard fc : gameState.getCardsAtLocation(currentLocation)) {
                                        if (fc == null || fc.getBlueprint() == null) continue;
                                        if (!playerId.equals(fc.getOwner())) continue;
                                        if (fc.getBlueprint().getCardCategory()
                                                != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        Float a = fc.getBlueprint().hasAbilityAttribute()
                                            ? fc.getBlueprint().getAbility() : null;
                                        if (a != null) v137OurAbility += a;
                                    }
                                    // V137 UPDATED 2026-07-06 T4.1: mover-side forfeit sum (projected team =
                                    // friendlies at dest + the whole group at the current location, parallel
                                    // to the power/ability projection above) for the shared parity check.
                                    float v137OurForfeit = 0f;
                                    for (PhysicalCard fc : gameState.getCardsAtLocation(v137Dest)) {
                                        if (fc == null || fc.getBlueprint() == null) continue;
                                        if (!playerId.equals(fc.getOwner())) continue;
                                        if (fc.getBlueprint().getCardCategory()
                                                != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        if (!fc.getBlueprint().hasForfeitAttribute()) continue;
                                        Float ff = fc.getBlueprint().getForfeit();
                                        if (ff != null) v137OurForfeit += ff;
                                    }
                                    for (PhysicalCard fc : gameState.getCardsAtLocation(currentLocation)) {
                                        if (fc == null || fc.getBlueprint() == null) continue;
                                        if (!playerId.equals(fc.getOwner())) continue;
                                        if (fc.getBlueprint().getCardCategory()
                                                != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        if (!fc.getBlueprint().hasForfeitAttribute()) continue;
                                        Float ff = fc.getBlueprint().getForfeit();
                                        if (ff != null) v137OurForfeit += ff;
                                    }
                                    // V137 UPDATED 2026-07-06 T4.1: winnability now decided by the SHARED
                                    // graded predicate MovePredicates.canWinAt (clean win OR the V181
                                    // fair-fight tolerance) — kills the move-4a deploy/move mirror where
                                    // V181 committed a deploy that V137 refused as a move. On failure the
                                    // canWinAt VETO flag is set (applied at the finalizer ONLY to
                                    // battle-seeking R2 claims — hunt/contest/attack — per ruling L3); the
                                    // old -800/-1500 magnitudes are RETAINED below as R1-band weights so
                                    // non-battle-seeking paths (R4 transit / R3 survival / non-battle R2 /
                                    // plain R1) keep today's deterrent instead of going unprotected.
                                    boolean v137CanWin = MovePredicates.canWinAt(game, gameState, playerId,
                                        v137Dest, v137OurPower, v137OurAbility, v137OurForfeit);
                                    if (!v137CanWin) {
                                        ladderCanWinVeto = true;
                                        ladderCanWinVetoReason = String.format(
                                            "V137 UNWINNABLE MOVE: %s → %s contested — even the full group (%.0f pwr/%.0f abil) loses to opp %.0f pwr (shared canWinAt false)",
                                            cardToMove.getTitle(), v137Dest.getTitle(),
                                            v137OurPower, v137OurAbility, v137OppPower);
                                        float v137Pen = -800.0f;
                                        if (v137OppPower - v137OurPower >= 6f) v137Pen = -1500.0f;
                                        action.addReasoning(String.format(
                                            "V137 UNWINNABLE MOVE: %s → %s contested — even the full group (%.0f pwr/%.0f abil) loses to opp %.0f pwr; don't waste move force",
                                            cardToMove.getTitle(), v137Dest.getTitle(),
                                            v137OurPower, v137OurAbility, v137OppPower), v137Pen);
                                        logger.warn("V137 UNWINNABLE MOVE: {} → {} (group pwr {} abil {} vs opp {}) → {} (+canWinAt veto flag)",
                                            cardToMove.getTitle(), v137Dest.getTitle(),
                                            (int)v137OurPower, (int)v137OurAbility, (int)v137OppPower, (int)v137Pen);
                                    }
                                } else {
                                    // V137 ANTI-SOLO-BG (Steve 2026-05-29, replay ss2jc7):
                                    // The original V137 only fires when oppPower>0 AT MOVE TIME.
                                    // Replay: Asajj deployed to Guest Quarters (drain), then moved
                                    // SOLO to Beldon's Corridor (uncontested at move time). asdf
                                    // reinforced Beldon's next turn and overran her — solo body, 0
                                    // vs 10 power, ability 4 vs 12, forfeit + battle damage = -7
                                    // force. The fix: even when the destination is currently
                                    // uncontested, a SOLO move to a BATTLEGROUND parks a body in
                                    // opp-reachable territory. Penalize -500 so Rando doesn't
                                    // dribble lone characters into BG sites that are easy to
                                    // overrun. (BG only — uncontested non-BGs aren't worth attacking.)
                                    boolean v137DestBG = false;
                                    try { v137DestBG = game.getModifiersQuerying()
                                        .isBattleground(gameState, v137Dest, null); }
                                    catch (Exception ignore) { /* false */ }
                                    if (v137DestBG) {
                                        int v137DestFriendlies = 0;
                                        for (PhysicalCard fc : gameState.getCardsAtLocation(v137Dest)) {
                                            if (fc == null || fc.getBlueprint() == null) continue;
                                            if (!playerId.equals(fc.getOwner())) continue;
                                            if (fc.getBlueprint().getCardCategory()
                                                    != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            v137DestFriendlies++;
                                        }
                                        int v137MovingChars = 0;
                                        for (PhysicalCard fc : gameState.getCardsAtLocation(currentLocation)) {
                                            if (fc == null || fc.getBlueprint() == null) continue;
                                            if (!playerId.equals(fc.getOwner())) continue;
                                            if (fc.getBlueprint().getCardCategory()
                                                    != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            v137MovingChars++;
                                        }
                                        int v137ProjectedAtDest = v137DestFriendlies + v137MovingChars;
                                        if (v137ProjectedAtDest <= 1) {
                                            action.addReasoning(String.format(
                                                "V137 ANTI-SOLO BG: %s → %s would be SOLO at a battleground (uncontested now, opp can reinforce/attack next turn) — don't park alone",
                                                cardToMove.getTitle(), v137Dest.getTitle()), -500.0f);
                                            logger.warn("V137 ANTI-SOLO BG: {} → {} solo at BG (projected={}) → -500",
                                                cardToMove.getTitle(), v137Dest.getTitle(), v137ProjectedAtDest);
                                        }
                                    }
                                }
                            } catch (Exception e) { logger.debug("V137 error: {}", e.getMessage()); }
                        }
                    }

                    // === V29.13: HUNT DOWN — MOVE CHARACTERS WITH VADER (GROUPING) ===
                    // Mirror of V29.12 deploy grouping but for MOVE phase.
                    // Characters should move TOWARD Vader, never AWAY from Vader.
                    // Vader should move TOWARD his characters, never away from them.
                    // This prevents the "scatter" problem where Rando swaps locations
                    // (e.g., Vader moves to Cantina while brothers move FROM Cantina to Mos Eisley).
                    {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer huntMoveGroupAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (huntMoveGroupAnalyzer != null && huntMoveGroupAnalyzer.isAnalyzed()
                            && huntMoveGroupAnalyzer.isHuntDownV()
                            && cardToMove != null && cardToMove.getTitle() != null
                            && gameState != null && game != null) {
                            try {
                                String movingCardTitle = cardToMove.getTitle().toLowerCase(Locale.ROOT);
                                // V137b: a "hunter" is any Dark Jedi (Vader, Dooku/Tyranus,
                                // ability >= 6) — they lead the strike; lower-ability buddies
                                // group toward them.
                                boolean movingCardIsVader = movingCardTitle.contains("vader")
                                    || movingCardTitle.contains("tyranus")
                                    || movingCardTitle.contains("dooku");
                                if (!movingCardIsVader) {
                                    try {
                                        movingCardIsVader = com.gempukku.swccgo.filters.Filters.Dark_Jedi
                                            .accepts(gameState, game.getModifiersQuerying(), cardToMove);
                                    } catch (Exception ignore) { /* false */ }
                                }
                                String moveActionLower = action.getDisplayText() != null
                                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";

                                if (movingCardIsVader) {
                                    // === VADER is moving — check if he's moving TOWARD or AWAY from his characters ===
                                    // Find locations with our characters (not Vader himself)
                                    PhysicalCard bestAllyLoc = null;
                                    float bestAllyPower = 0;
                                    int totalAllyChars = 0;
                                    for (PhysicalCard loc : gameState.getTopLocations()) {
                                        if (loc == null || loc == currentLocation) continue;
                                        float allyPowerHere = 0;
                                        int allyCountHere = 0;
                                        for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                            if (c == null || c == cardToMove) continue;
                                            if (!playerId.equals(c.getOwner())) continue;
                                            if (c.getBlueprint() == null) continue;
                                            if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            allyCountHere++;
                                            Float pw = c.getBlueprint().getPower();
                                            allyPowerHere += (pw != null ? pw : 0);
                                        }
                                        totalAllyChars += allyCountHere;
                                        if (allyPowerHere > bestAllyPower) {
                                            bestAllyPower = allyPowerHere;
                                            bestAllyLoc = loc;
                                        }
                                    }

                                    if (totalAllyChars > 0 && bestAllyLoc != null) {
                                        String bestAllyLocTitle = bestAllyLoc.getTitle() != null
                                            ? bestAllyLoc.getTitle().toLowerCase(Locale.ROOT) : "";
                                        boolean movingTowardAllies = !bestAllyLocTitle.isEmpty()
                                            && moveActionLower.contains(bestAllyLocTitle);

                                        if (movingTowardAllies) {
                                            // Vader moving TOWARD his characters — GOOD!
                                            float groupBonus = 200.0f;
                                            if (bestAllyPower >= 8) groupBonus += 50.0f;
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP MOVE: Vader moving TOWARD %d allies at %s (power %.0f) — group up!",
                                                totalAllyChars, bestAllyLoc.getTitle(), bestAllyPower), groupBonus);
                                            logger.warn("V29.13 HUNT GROUP: Vader moving to allies at {} (+{})",
                                                bestAllyLoc.getTitle(), (int)groupBonus);
                                            // V29.13 UPDATED 2026-07-06 T4.1: toward-group claims R2 DOCTRINE
                                            // (non-battle; +200/+250 passes the L2 gate). Scatter arms stay R1 weights.
                                            ladderClaimR2("V29.13 HUNT GROUP MOVE (Vader→allies)", groupBonus, 0.0f, false);
                                        } else {
                                            // Vader moving AWAY from his characters — BAD!
                                            // Exception: moving toward opponents to hunt (already handled by HUNT DOWN block above)
                                            // Check if destination has opponents (hunting is OK)
                                            boolean huntingOpponents = false;
                                            String opponentIdGroup = game.getOpponent(playerId);
                                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                                if (loc == null || loc.getTitle() == null) continue;
                                                String locLower = loc.getTitle().toLowerCase(Locale.ROOT);
                                                if (!locLower.isEmpty() && moveActionLower.contains(locLower)) {
                                                    float opPower = 0;
                                                    try {
                                                        opPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                            gameState, loc, opponentIdGroup, false, false);
                                                    } catch (Exception e2) { /* ignore */ }
                                                    if (opPower > 0) {
                                                        huntingOpponents = true;
                                                    }
                                                    break;
                                                }
                                            }
                                            if (!huntingOpponents) {
                                                action.addReasoning(String.format(
                                                    "V29.13 HUNT GROUP: Vader moving AWAY from %d allies — stay together!",
                                                    totalAllyChars), -200.0f);
                                                logger.warn("V29.13 HUNT SCATTER: Vader moving away from allies at {} (-200)",
                                                    bestAllyLoc.getTitle());
                                            }
                                        }
                                    }
                                } else {
                                    // === NON-VADER character is moving — check if moving TOWARD or AWAY from Vader ===
                                    // V137b: anchor on any Dark Jedi hunter (Vader OR Dooku),
                                    // not just Vader, so buddies group toward whichever Sith
                                    // leads the strike.
                                    PhysicalCard vaderCard = null;
                                    PhysicalCard vaderLoc = null;
                                    for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                        if (tableCard == null || !playerId.equals(tableCard.getOwner())) continue;
                                        com.gempukku.swccgo.common.Zone vz = tableCard.getZone();
                                        if (vz == null || !vz.isInPlay()) continue;
                                        if (tableCard.getBlueprint() == null
                                            || tableCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        String vTitle = tableCard.getTitle() != null
                                            ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                        boolean isHunterAnchor = vTitle.contains("vader")
                                            || vTitle.contains("tyranus") || vTitle.contains("dooku");
                                        if (!isHunterAnchor) {
                                            try {
                                                isHunterAnchor = com.gempukku.swccgo.filters.Filters.Dark_Jedi
                                                    .accepts(gameState, game.getModifiersQuerying(), tableCard);
                                            } catch (Exception ignore) { /* false */ }
                                        }
                                        if (isHunterAnchor) {
                                            vaderCard = tableCard;
                                            vaderLoc = tableCard.getAtLocation();
                                            break;
                                        }
                                    }

                                    if (vaderLoc != null && vaderLoc.getTitle() != null) {
                                        String vaderLocTitle = vaderLoc.getTitle().toLowerCase(Locale.ROOT);
                                        boolean currentlyWithVader = (currentLocation == vaderLoc);
                                        boolean movingToVader = !vaderLocTitle.isEmpty()
                                            && moveActionLower.contains(vaderLocTitle);

                                        if (currentlyWithVader && !movingToVader) {
                                            // Moving AWAY from Vader — BAD!
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP: %s moving AWAY from Vader at %s — stay together!",
                                                cardToMove.getTitle(), vaderLoc.getTitle()), -250.0f);
                                            logger.warn("V29.13 HUNT SCATTER: {} leaving Vader at {} (-250)",
                                                cardToMove.getTitle(), vaderLoc.getTitle());
                                        } else if (!currentlyWithVader && movingToVader) {
                                            // Moving TOWARD Vader — GREAT!
                                            float groupBonus = 250.0f;
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP MOVE: %s moving TOWARD Vader at %s — group up!",
                                                cardToMove.getTitle(), vaderLoc.getTitle()), groupBonus);
                                            logger.warn("V29.13 HUNT GROUP: {} moving to Vader at {} (+{})",
                                                cardToMove.getTitle(), vaderLoc.getTitle(), (int)groupBonus);
                                            // V29.13 UPDATED 2026-07-06 T4.1: toward-group claims R2 DOCTRINE
                                            // (non-battle; +250 passes the L2 gate). Scatter arms stay R1 weights.
                                            ladderClaimR2("V29.13 HUNT GROUP MOVE (→Vader)", groupBonus, 0.0f, false);
                                        } else if (!currentlyWithVader && !movingToVader) {
                                            // Moving but NOT toward Vader — mild penalty
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP: %s moving but NOT toward Vader at %s — group up instead!",
                                                cardToMove.getTitle(), vaderLoc.getTitle()), -100.0f);
                                            logger.info("V29.13 HUNT SCATTER: {} not moving toward Vader at {} (-100)",
                                                cardToMove.getTitle(), vaderLoc.getTitle());
                                        }
                                        // If currentlyWithVader && movingToVader: shouldn't happen, no adjustment needed
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V29.13 HUNT GROUP MOVE: Error: {}", e.getMessage());
                            }
                        }
                    }

                    // V22.5: PRE-FLIP CONSOLIDATION — don't leave characters alone to die!
                    // Even before flipping, if a lone character is badly outgunned at a location,
                    // they should move to join allies instead of staying to get slaughtered.
                    com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer moveObjAnalyzer =
                        context.getObjectiveAnalyzer();
                    if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed() && !moveObjAnalyzer.isFlipped()) {
                        String preFlipLocTitle = currentLocation.getTitle();
                        String preFlipOpponent = game.getOpponent(playerId);

                        // Count our characters at this location
                        int preFlipOurChars = 0;
                        float preFlipOurPower = 0;
                        for (PhysicalCard card : gameState.getCardsAtLocation(currentLocation)) {
                            if (card != null && playerId.equals(card.getOwner())
                                && card.getBlueprint() != null && card.getBlueprint().hasPowerAttribute()) {
                                preFlipOurChars++;
                                Float p = card.getBlueprint().getPower();
                                preFlipOurPower += (p != null ? p : 0);
                            }
                        }

                        // Get opponent power here
                        float preFlipTheirPower = 0;
                        try {
                            preFlipTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, currentLocation, preFlipOpponent, false, false);
                        } catch (Exception e) {
                            // Ignore
                        }

                        // V22.5: Lone character badly outgunned — should move to join allies
                        if (preFlipOurChars == 1 && preFlipTheirPower > preFlipOurPower * 2 && preFlipTheirPower > 6) {
                            // Find a friendly location with allies to join
                            String bestAllyLoc = null;
                            float bestAllyPower = 0;
                            try {
                                for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                    if (loc == null || loc == currentLocation) continue;
                                    float allyPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, playerId, false, false);
                                    if (allyPower > bestAllyPower) {
                                        bestAllyPower = allyPower;
                                        bestAllyLoc = loc.getTitle();
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore
                            }

                            float consolidateBonus = 100.0f;
                            if (preFlipTheirPower > preFlipOurPower * 3) consolidateBonus = 160.0f;
                            action.addReasoning("V22.5 PRE-FLIP: LONE & OUTGUNNED (" + (int)preFlipOurPower +
                                " vs " + (int)preFlipTheirPower + ") - move to join allies" +
                                (bestAllyLoc != null ? " at " + bestAllyLoc : ""), consolidateBonus);
                            logger.warn("V22.5 CONSOLIDATE PRE-FLIP: {} alone at {} ({}v{}) should join allies{}",
                                cardToMove.getTitle(), preFlipLocTitle,
                                (int)preFlipOurPower, (int)preFlipTheirPower,
                                bestAllyLoc != null ? " at " + bestAllyLoc : "");
                            // V22.5 UPDATED 2026-07-06 T4.1: consolidation attempts an R2 DOCTRINE claim
                            // (spec Table 2). NOTE: +100/+160 fails the L2 strength gate (< +200, no
                            // drain-delta) so today it stays R1 by ruling — the attempt is kept so a
                            // future magnitude bump promotes cleanly.
                            ladderClaimR2("V22.5 PRE-FLIP CONSOLIDATE", consolidateBonus, 0.0f, false);
                        } else if (preFlipOurChars <= 2 && preFlipTheirPower > preFlipOurPower * 1.5f && preFlipTheirPower > 8) {
                            // Small group outgunned — moderate consolidation pressure
                            action.addReasoning("V22.5 PRE-FLIP: Outgunned at " + preFlipLocTitle +
                                " (" + (int)preFlipOurPower + " vs " + (int)preFlipTheirPower + ")", 60.0f);
                        }
                    }

                    // V22.2: POST-FLIP OBJECTIVE PROTECTION
                    // After objective flips, protect flip-back locations at all costs.
                    // Scale required power based on opponent's threat level.
                    if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed() && moveObjAnalyzer.isFlipped()) {
                        String curLocTitle = currentLocation.getTitle();
                        boolean atProtectionLoc = moveObjAnalyzer.isFlipBackProtectionLocation(curLocTitle);
                        String opponent = game.getOpponent(playerId);

                        // Count our characters and power at current location
                        int ourCharsHere = 0;
                        float ourPowerHere = 0;
                        for (PhysicalCard card : gameState.getCardsAtLocation(currentLocation)) {
                            if (card != null && playerId.equals(card.getOwner())
                                && card.getBlueprint() != null && card.getBlueprint().hasPowerAttribute()) {
                                ourCharsHere++;
                                Float p = card.getBlueprint().getPower();
                                ourPowerHere += (p != null ? p : 0);
                            }
                        }

                        // Check opponent total power on table (measure of threat)
                        float opponentTotalPower = 0;
                        try {
                            for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null) {
                                    opponentTotalPower += game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, opponent, false, false);
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Could not sum opponent power: {}", e.getMessage());
                        }

                        // Find the most vulnerable protection location (lowest our power vs their power)
                        float worstDeficit = 0;
                        String weakestLoc = null;
                        try {
                            for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (!moveObjAnalyzer.isFlipBackProtectionLocation(loc.getTitle())) continue;
                                float ourPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, playerId, false, false);
                                float theirPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, opponent, false, false);
                                float deficit = (theirPwr + 4.0f) - ourPwr;
                                if (deficit > worstDeficit) {
                                    worstDeficit = deficit;
                                    weakestLoc = loc.getTitle();
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Could not analyze protection locations: {}", e.getMessage());
                        }

                        if (atProtectionLoc) {
                            // AT a protection location — DO NOT LEAVE unless massively overkill
                            if (ourCharsHere >= 3 && ourPowerHere > 12) {
                                // Strong presence, can afford to move one character
                                action.addReasoning("V22.2 POST-FLIP: Strong at protection loc - can move", -30.0f);
                            } else {
                                // Must stay and defend! Penalty scales with opponent threat
                                float stayPenalty = -80.0f;
                                if (opponentTotalPower > 15) stayPenalty = -120.0f;
                                if (opponentTotalPower > 25) stayPenalty = -160.0f;
                                action.addReasoning("V22.2 POST-FLIP: STAY at protection location! Opponent power=" +
                                    (int)opponentTotalPower, stayPenalty);
                                logger.warn("V22.2 PROTECT: {} must stay at {} (our power={}, opponent total={})",
                                    cardToMove.getTitle(), curLocTitle, (int)ourPowerHere, (int)opponentTotalPower);
                            }
                        } else {
                            // NOT at a protection location — encourage moving to one that needs help
                            if (ourCharsHere == 1) {
                                float moveBonus = 80.0f;
                                if (worstDeficit > 4) moveBonus = 120.0f;
                                if (worstDeficit > 8) moveBonus = 160.0f;
                                action.addReasoning("V22.2 POST-FLIP: Lone char should reinforce " +
                                    (weakestLoc != null ? weakestLoc : "protection locs"), moveBonus);
                                logger.warn("V22.2 CONSOLIDATE: {} alone at {} - move to reinforce (worst deficit={})",
                                    cardToMove.getTitle(), curLocTitle, (int)worstDeficit);
                                // V22.2 UPDATED 2026-07-06 T4.1: reinforce attempts an R2 DOCTRINE claim
                                // (spec Table 2). NOTE: +80/+120/+160 fails the L2 strength gate (< +200)
                                // so today it stays R1 by ruling; attempt kept for a future magnitude bump.
                                ladderClaimR2("V22.2 POST-FLIP REINFORCE", moveBonus, 0.0f, false);
                            } else if (worstDeficit > 6) {
                                // Even non-lone characters should move if protection locs are severely underguarded
                                action.addReasoning("V22.2 POST-FLIP: Protection locations severely under-guarded!", 60.0f);
                                logger.warn("V22.2 CONSOLIDATE: {} at {} but {} needs help (deficit={})",
                                    cardToMove.getTitle(), curLocTitle, weakestLoc, (int)worstDeficit);
                            }
                        }
                    }
                } else {
                    action.addReasoning("Card not at a location", BAD_DELTA);
                }
            }

            // === MOVEMENT TYPE BONUSES ===
            // V25: Shuttle bonus only when defending — opponent has 2x our power at destination
            if (actionLower.contains("shuttle") || actionLower.contains("transport")) {
                boolean defensiveShuttle = false;
                if (gameState != null) {
                    String opponentId = gameState.getOpponent(playerId);
                    for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                        String locTitle = loc.getTitle();
                        if (locTitle != null && actionLower.contains(locTitle.toLowerCase(Locale.ROOT))) {
                            // Found a location mentioned in action text — check power
                            float ourPower = 0, theirPower = 0;
                            for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                if (c == null) continue;
                                SwccgCardBlueprint bp = c.getBlueprint();
                                if (bp == null || !bp.hasPowerAttribute()) continue;
                                Float pw = bp.getPower();
                                if (pw == null) pw = 0f;
                                if (playerId.equals(c.getOwner())) ourPower += pw;
                                else if (opponentId != null && opponentId.equals(c.getOwner())) theirPower += pw;
                            }
                            if (ourPower > 0 && theirPower >= ourPower * 2) {
                                defensiveShuttle = true;
                                action.addReasoning("V25 Defensive shuttle — opponent has " + (int)theirPower
                                    + " vs our " + (int)ourPower + " at " + locTitle, 20.0f);
                                logger.info("[MoveEvaluator] V25 Defensive shuttle to {} (them={}, us={})",
                                    locTitle, (int)theirPower, (int)ourPower);
                            }
                            break;
                        }
                    }
                }
                if (!defensiveShuttle) {
                    // No bonus for non-defensive shuttles — let strategic analysis decide
                    logger.debug("[MoveEvaluator] V25 Shuttle without defensive need — no bonus");
                }
            }
            if (actionLower.contains("docking bay")) {
                action.addReasoning("Docking bay transit", 15.0f);
            }
            if (actionLower.contains("take off")) {
                action.addReasoning("Take off (space deployment)", 10.0f);
            }

            // Land - penalize starfighters
            if (actionLower.contains("land")) {
                handleLandAction(action, actionLower, cardToMove, game);
            }

            // Move phase - no automatic bonus, moves should be strategic
            // The old +5 bonus caused wasteful moves
            if (context.getPhase() == Phase.MOVE) {
                // Only add reasoning without bonus - moves need strategic justification
                action.addReasoning("Move phase", 0.0f);

                // === V27: MAINTENANCE FORCE CONSERVATION DURING MOVES ===
                // Moving costs Force. If we have maintenance cards in play (Blizzard etc.)
                // and our Force pile is low, penalize non-critical moves to conserve Force
                // for maintenance payment at end of turn.
                if (gameState != null) {
                    try {
                        // T2 MOVE #1 COMMIT-2 (2026-07-06): maintenance obligation from the
                        // shared per-decision ForceReserveService cache (authoritative). This
                        // site formerly used deploy cost; the consolidated MaintenanceFacts
                        // engine basis is intentional. -80 weight unchanged. History in git.
                        int maintenanceCost = context.getForceReserveFacts().maintenanceObligation;
                        if (maintenanceCost > 0) {
                            int forcePile = gameState.getForcePileSize(playerId);
                            if (forcePile <= maintenanceCost + 1) {
                                action.addReasoning(String.format(
                                    "V27 MAINTENANCE: Need %d Force for upkeep, only %d left — DON'T waste Force moving!",
                                    maintenanceCost, forcePile), -80.0f);
                                logger.warn("V27 MAINTENANCE MOVE BLOCK: {} Force in pile, need {} for maintenance — penalizing move!",
                                    forcePile, maintenanceCost);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V27: Error checking maintenance during move: {}", e.getMessage());
                    }
                }
            }

            // === V53: SPY FOLLOW — Undercover spy follows opponent when they move away ===
            // If our undercover spy is at a location where the opponent just left (no opponent
            // presence remaining), move the spy to follow them. The spy is a leech — it sticks
            // to the opponent's army to keep reducing their drain wherever they go.
            // +500 to move spy toward opponent characters.
            // -300 to move spy AWAY from opponent characters (defeats the purpose).
            if (cardToMove != null && cardToMove.isUndercover() && gameState != null && game != null) {
                try {
                    String spyPid = context.getPlayerId();
                    String spyOid = game.getOpponent(spyPid);
                    PhysicalCard spySrcLoc = cardToMove.getAtLocation();

                    // Check if opponent still has presence at spy's current location
                    float oppPowerHere = 0;
                    if (spySrcLoc != null) {
                        oppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, spySrcLoc, spyOid, false, false);
                    }

                    // Check if destination has opponent presence
                    boolean destHasOpponent = false;
                    for (PhysicalCard destLoc : gameState.getTopLocations()) {
                        if (destLoc == null || destLoc.getTitle() == null) continue;
                        String destTitle = destLoc.getTitle().toLowerCase(Locale.ROOT);
                        if (!actionLower.contains(destTitle)) continue;
                        float oppPowerDest = game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, destLoc, spyOid, false, false);
                        if (oppPowerDest > 0) destHasOpponent = true;
                        break;
                    }

                    if (oppPowerHere == 0 && destHasOpponent) {
                        // Opponent left this location — spy should follow them!
                        action.addReasoning("V53 SPY FOLLOW: Opponent moved away — follow them to keep reducing drain!", 500.0f);
                        logger.warn("V53 SPY FOLLOW: {} following opponent to new location — +500!", cardToMove.getTitle());
                        // V53 UPDATED 2026-07-06 T4.1: spy-follow claims R2 DOCTRINE (non-battle:
                        // the spy leeches drain, it does not seek battle; +500 passes L2).
                        ladderClaimR2("V53 SPY FOLLOW", 500.0f, 0.0f, false);
                    } else if (oppPowerHere > 0 && !destHasOpponent) {
                        // Moving spy AWAY from opponent — bad, defeats the purpose
                        action.addReasoning("V53 SPY STAY: Opponent is HERE — don't leave, keep reducing their drain!", -300.0f);
                        logger.warn("V53 SPY STAY: {} trying to leave opponent — -300!", cardToMove.getTitle());
                    } else if (destHasOpponent && oppPowerHere == 0) {
                        // Moving to opponent from empty location — good repositioning
                        action.addReasoning("V53 SPY REPOSITION: Move spy to opponent location — start reducing drain!", 400.0f);
                        logger.warn("V53 SPY REPOSITION: {} moving to opponent location — +400!", cardToMove.getTitle());
                        // V53 UPDATED 2026-07-06 T4.1: spy-reposition claims R2 DOCTRINE (non-battle; +400 passes L2).
                        ladderClaimR2("V53 SPY REPOSITION", 400.0f, 0.0f, false);
                    }
                } catch (Exception e) {
                    logger.debug("V53 SPY FOLLOW: Error: {}", e.getMessage());
                }
            }

            // === V53b: HIDDEN PATH MANDATORY JEDI TRANSIT ===
            // HARD RULE: If playing Hidden Path, characters at Safehouse MUST move to
            // Underground Corridor. Characters at Corridor MUST move OFF Mapuzo.
            // Jedi Survivors move FREE on Mapuzo — there is ZERO cost. No force reserve
            // excuses. The objective REQUIRES Jedi outside Mapuzo to flip.
            // T4.1 (2026-07-06): overrides via the R4 MANDATORY TRANSIT band (+20000 at the
            // finalizer), no longer via setScore(±9999) ordering hacks.
            {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer hpMoveAnalyzer =
                    context.getObjectiveAnalyzer();
                if (hpMoveAnalyzer != null && hpMoveAnalyzer.isAnalyzed()) {
                    String hpMoveObjTitle = hpMoveAnalyzer.getObjectiveTitle();
                    boolean isHiddenPathObj = hpMoveObjTitle != null
                        && hpMoveObjTitle.toLowerCase(Locale.ROOT).contains("hidden path");
                    if (isHiddenPathObj && cardToMove != null) {
                        PhysicalCard srcLoc = cardToMove.getAtLocation();
                        String srcName = (srcLoc != null && srcLoc.getTitle() != null) ?
                            srcLoc.getTitle().toLowerCase(Locale.ROOT) : "";
                        String charName = cardToMove.getTitle() != null ? cardToMove.getTitle() : "character";

                        // V60 FIX: The MoveEvaluator scores 'Move using landspeed' and 'Land'
                        // actions — but landspeed from Corridor only goes to ADJACENT Mapuzo
                        // sites (Safehouse/Mining Village), NOT outward. The CORRECT action
                        // for Corridor→Jabiim/opponent-BG is the location's game text
                        // "Move Jedi Survivor here to a site" — scored in ActionTextEvaluator,
                        // not here. So at Corridor, we BLOCK landspeed entirely (-9999).
                        // The transit action is positively scored in ActionTextEvaluator V60.
                        // FIXES Issue #C from 8d9jxayxqtp293l7 replay: Turn 2 all 3 Jedi moved
                        // Corridor → Safehouse via landspeed because V53b gave +9999 to ANY
                        // landspeed move from Corridor regardless of destination.
                        boolean isLandspeed = actionLower.contains("move using landspeed")
                            || actionLower.equals("move");

                        // ANY character at Safehouse → MUST move to Corridor (landspeed OK,
                        // only 1 adjacent battleground anyway)
                        if (srcName.contains("safehouse") && isLandspeed) {
                            // V53b UPDATED 2026-07-06 T4.1: the setScore(9999) ordering hack is GONE —
                            // any rule appended after it silently re-decided the pick. Replaced by an
                            // R4 MANDATORY TRANSIT claim (+20000 band at the finalizer, order-independent)
                            // plus a +800 fine (matches the Mapuzo-exit arm; move-3c boundary = +20800).
                            action.addReasoning("V53b HIDDEN PATH MANDATORY: Landspeed Safehouse → Corridor — FREE move, MUST flip objective!", 800.0f);
                            ladderClaimR4Transit("V53b SAFEHOUSE→CORRIDOR");
                            logger.warn("V53b HIDDEN PATH: {} MUST landspeed Safehouse → Corridor (R4 transit +800 fine)!", charName);
                        }
                        // ANY character at Corridor:
                        //   - Landspeed = BLOCKED (only adjacent is Mapuzo = going backwards)
                        //   - Transit action scored in ActionTextEvaluator
                        else if (srcName.contains("underground corridor") || srcName.contains("underground")) {
                            if (isLandspeed) {
                                // V60 UPDATED 2026-07-06 T4.1: setScore(-9999) ordering hack replaced by
                                // the ladder hard-veto class (-100000 at the finalizer, veto-class per
                                // move-3d boundary). Same trigger, band-proof magnitude.
                                ladderVetoHard = true;
                                ladderVetoHardReason = "V60 HIDDEN PATH LANDSPEED BLOCK: Landspeed from Corridor only goes back to Mapuzo — use the transit game text instead!";
                                logger.warn("V60 HIDDEN PATH: {} BLOCKED landspeed from Corridor (LADDER VETO) — must use 'Move Jedi Survivor here to a site'!", charName);
                            }
                        }
                        // Moving OFF any Mapuzo location to non-Mapuzo via landspeed
                        // (e.g., Jabiim Path Operations Center has interior path to Mapuzo)
                        else if (srcName.contains("mapuzo") && isLandspeed) {
                            action.addReasoning("V53b HIDDEN PATH: Leaving Mapuzo via landspeed — objective progress!", 800.0f);
                            // V53b UPDATED 2026-07-06 T4.1: the Mapuzo-exit claims R4 MANDATORY TRANSIT.
                            // The claim identity also suppresses the V38.3 wrong-direction veto (ruling
                            // L3 carve-out, move-3f: exit to an EMPTY site must fire) and outranks the
                            // V37.1/V85 R1 weights by band (move-3a/3b boundaries: +19300 / +20000).
                            ladderClaimR4Transit("V53b MAPUZO EXIT");
                            logger.warn("V53b HIDDEN PATH: {} leaving Mapuzo via landspeed — R4 transit +800!", charName);
                        }
                    }
                }
            }

            // T4.1 (2026-07-06): LADDER FINALIZER — applies veto flags (with the L3
            // veto×rank matrix), the ±2800 fine clamp (+L1 demote), the rank base
            // band, and the relocated default -50. Must stay the LAST scoring step
            // before actions.add so every rule above has already spoken.
            ladderFinalize(action);

            logger.debug("[MoveEvaluator] Scored '{}' -> {}",
                actionText.length() > 40 ? actionText.substring(0, 40) + "..." : actionText,
                String.format("%.1f", action.getScore()));

            actions.add(action);
        }

        logger.info("[MoveEvaluator] Evaluated {} move actions", actions.size());
        return actions;
    }

    /**
     * Rank moving FROM a specific location.
     * Ported from Python move_evaluator.py _rank_move_from_location
     */
    private void rankMoveFromLocation(EvaluatedAction action, GameState gameState,
                                       SwccgGame game, String playerId, Side mySide,
                                       PhysicalCard cardToMove, PhysicalCard location) {
        // T4.1 (2026-07-06): all early returns in this method are REMOVED — every block
        // always runs and the ladder finalizer (evaluate loop tail) decides the band.
        // This flag gates the finalizer's relocated default -50 to the same population
        // that could reach the old tail line.
        ladderRankMoveRan = true;
        String opponentId = gameState.getOpponent(playerId);

        // Calculate power at current location
        float myPower = 0;
        float theirPower = 0;
        int myCardCount = 0;
        int theirCardCount = 0;

        List<PhysicalCard> cardsAtLocation = gameState.getCardsAtLocation(location);
        for (PhysicalCard card : cardsAtLocation) {
            if (card == null) continue;
            String owner = card.getOwner();
            SwccgCardBlueprint bp = card.getBlueprint();
            if (bp == null || !bp.hasPowerAttribute()) continue;

            Float power = bp.getPower();
            if (power == null) power = 0f;

            if (playerId.equals(owner)) {
                myPower += power;
                myCardCount++;
            } else if (opponentId != null && opponentId.equals(owner)) {
                theirPower += power;
                theirCardCount++;
                // V37.1 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T5: Lando held at
                // 6v8 raw vs 6v11 armed — hit + forfeited + 5 damage next turn): the threat
                // tiers were blind to opponent WEAPONS. Weapon-adjust their power with the
                // V29.7 heuristic (lightsaber +5, other +3; attached or permanent) so
                // calculateThreatLevel sees the real fight. Thresholds themselves unchanged.
                if (bp.getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                    try {
                        List<PhysicalCard> wAtts = gameState.getAttachedCards(card);
                        if (wAtts != null) {
                            for (PhysicalCard att : wAtts) {
                                if (att == null || att.getBlueprint() == null) continue;
                                if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                    String wt = att.getTitle() != null ? att.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                    theirPower += wt.contains("lightsaber") ? 5.0f : 3.0f;
                                }
                            }
                        }
                        String wgt = bp.getGameText();
                        if (wgt != null && wgt.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                            String wct = card.getTitle() != null ? card.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            theirPower += wct.contains("lightsaber") ? 5.0f : 3.0f;
                        }
                    } catch (Exception e) { /* fail-open: raw power */ }
                }
            }
        }

        float powerDiff = myPower - theirPower;
        boolean theirHasCards = theirCardCount > 0;

        logger.debug("[MoveEvaluator] At {}: myPower={}, theirPower={}, diff={}",
            location.getTitle(), myPower, theirPower, powerDiff);

        // === THREAT LEVEL ANALYSIS ===
        if (theirPower > 0) {
            ThreatLevel threat = calculateThreatLevel(powerDiff);

            switch (threat) {
                case RETREAT:
                    action.addReasoning("Strategic retreat - badly outmatched (" + (int)powerDiff + ")",
                                       VERY_GOOD_DELTA);
                    logger.info("[MoveEvaluator] RETREAT recommended - outmatched by {}", -powerDiff);
                    // T4.1 (2026-07-06): the RETREAT tier claims R3 SURVIVAL; the early
                    // return is removed so later blocks (V29.13 drain, V34 contest…) still run.
                    ladderClaimR3("THREAT RETREAT");
                    break;

                case DANGEROUS:
                    action.addReasoning("Dangerous location - retreat recommended (" + (int)powerDiff + ")",
                                       GOOD_DELTA * 2);
                    // T4.1 (2026-07-06): early return removed — R1 fine (+20), block falls through.
                    break;

                case CRUSH:
                    // V37.1 UPDATED 2026-07-06 T4.1: the cross-rank -9999 + return silently buried
                    // every doctrine/transit rule below (move-3a boundary: Hidden Path stalled
                    // forever at a FAVORABLE Mining Village). Now an R1-internal weight (-1500,
                    // no return): still buries every same-band R1 move (V37.1-protect boundary:
                    // -1550 < Pass), but an R2+ claim outranks it by band.
                    action.addReasoning("V37.1 STAY AND CRUSH: Power +" + (int)powerDiff + " — DESTROY them!",
                                       -1500.0f);
                    logger.warn("V37.1 STAY AND CRUSH at {}: power +{} — -1500 (weight)",
                        location.getTitle(), (int)powerDiff);
                    break;

                case FAVORABLE:
                    // V37.1 UPDATED 2026-07-06 T4.1: same conversion as CRUSH (-9999+return → -1500 weight).
                    action.addReasoning("V37.1 STAY AND FIGHT: Power +" + (int)powerDiff + " — hold position!",
                                       -1500.0f);
                    logger.warn("V37.1 STAY AND FIGHT at {}: power +{} — -1500 (weight)",
                        location.getTitle(), (int)powerDiff);
                    break;

                case RISKY:
                    // V37.1: Even fight — very strong discouragement to leave
                    action.addReasoning("V37.1 CONTESTED: Even power (" + (int)powerDiff + ") — hold position!",
                                       -500.0f);
                    break;
            }
        }

        // === V85 (Steve, 2026-05-16): UNCONTESTED + LOWER-DRAIN = HARD BLOCK ===
        // Per Steve (asked multiple times): "No character should ever choose
        // to move when they have no contesting opponent on their site to a
        // site that has the potential for less force drain."
        //
        // V29.13 already does this when the action text includes the
        // destination — but for generic actions like "Move using landspeed"
        // the destination is selected in a SEPARATE CARD_SELECTION decision
        // (e.g., Rey at Cloud City: Lower Corridor moving to Upper Plaza
        // Corridor, drain 3 → 0). V29.13's destination-from-text loop
        // returns null and silently does nothing. V29.7 WEAPON HUNTER
        // then scores the move +130 because it sees ANY remote attack
        // target — without checking reachability or the drain we'd lose.
        //
        // V85 sidesteps the destination ambiguity by checking the BEST
        // (highest-drain) adjacent site. If even the best adjacent drain
        // is lower than current, ANY move from here is wrong → HARD BLOCK.
        // Fires BEFORE FLEE/ATTACK/V29.7 so the -2000 dominates their bonuses.
        if (gameState != null && game != null && location != null && !theirHasCards) {
            try {
                // V85 UPDATED 2026-07-06 T4.1: drain math routed through the shared engine
                // metric MovePredicates.drainAt (behavior-identical — this was already
                // engine-based; move-6 single-detection consolidation).
                float currentDrainV85 = MovePredicates.drainAt(game, gameState, location, playerId);
                if (currentDrainV85 > 0) {
                    float bestAdjDrain = Float.NEGATIVE_INFINITY;
                    PhysicalCard bestAdjLoc = null;
                    for (PhysicalCard adj : gameState.getLocationsInOrder()) {
                        if (adj == null || adj == location) continue;
                        try {
                            if (!game.getModifiersQuerying().isAdjacentSites(gameState, location, adj)) continue;
                            float adjDrain = MovePredicates.drainAt(game, gameState, adj, playerId);
                            if (adjDrain > bestAdjDrain) {
                                bestAdjDrain = adjDrain;
                                bestAdjLoc = adj;
                            }
                        } catch (Exception ie) { /* skip non-comparable */ }
                    }
                    if (bestAdjLoc != null && bestAdjDrain < currentDrainV85) {
                        // V85 UPDATED 2026-07-06 T4.1: the -2000 + bare return killed every
                        // doctrine rule below it (move-1 boundary: the Cantina↔Mos Eisley
                        // shuttle was dead; move-2: Vader farmed drain instead of hunting).
                        // Now an R1-internal weight (-800, NO return): still blocks every
                        // same-band R1 wander (V85-protect boundary: Rey stays at -720 < Pass),
                        // while an R2+ doctrine claim (V73 shuttle, V35 hunt…) outranks it by band.
                        action.addReasoning(String.format(
                            "V85 UNCONTESTED: at %s (drain %.0f) with no opponent — "
                                + "best adjacent %s only drains %.0f. STAY for the better drain!",
                            location.getTitle(), currentDrainV85,
                            bestAdjLoc.getTitle(), bestAdjDrain),
                            -800.0f);
                        logger.warn("V85 UNCONTESTED: {} drain {} → best adj {} drain {} → -800 (weight)",
                            location.getTitle(), (int)currentDrainV85,
                            bestAdjLoc.getTitle(), (int)bestAdjDrain);
                    }
                }
            } catch (Exception e) {
                logger.debug("V85 UNCONTESTED CHECK: Error: {}", e.getMessage());
            }
        }

        // === FLEE LOGIC ===
        if (theirPower - myPower > POWER_DIFF_FOR_FLEE && theirPower > 0) {
            float disadvantage = theirPower - myPower;
            action.addReasoning("Outmatched by " + (int)disadvantage + " - should flee",
                               GOOD_DELTA * Math.min(disadvantage / 2, 5));
            // T4.1 (2026-07-06): early return removed — R1 fine (≤ +50), block falls through.
        }

        // === OFFENSIVE ATTACK OPPORTUNITY ===
        // If we're at an uncontested location with significant power, look for attack targets
        // NOTE: We can't verify reachability here, so be conservative - only recommend if:
        // 1. We have overwhelming force AND
        // 2. There are high-value targets (opponent icons for force drain)
        if (!theirHasCards && myPower >= ATTACK_MIN_POWER && myCardCount >= 2) {
            AttackAnalysis attack = analyzeAttackOpportunity(gameState, game, playerId,
                                                             mySide, location, myPower, myCardCount);
            // Only recommend attack if there's force drain potential (icons > 0)
            // and we have a significant power advantage
            if (attack != null && attack.viable && attack.hasForcedrainPotential) {
                action.addReasoning(attack.reason, attack.score);
                logger.info("[MoveEvaluator] ⚔️ ATTACK opportunity: {}", attack.reason);
                // T4.1 (2026-07-06): early return removed. ATTACK claims R2 DOCTRINE
                // (battle-seeking) — NEWLY gated on isAdjacentSites reachability to the
                // best target (same engine call V85 uses) so the destination-blind Rey
                // class can't claim a band for an unreachable fight (V85-protect boundary).
                boolean attackTargetAdjacent = false;
                try {
                    attackTargetAdjacent = attack.targetLocation != null
                        && game.getModifiersQuerying().isAdjacentSites(gameState, location, attack.targetLocation);
                } catch (Exception ignore) { /* false */ }
                if (attackTargetAdjacent) {
                    ladderClaimR2("ATTACK", attack.score, 0.0f, true);
                } else {
                    logger.info("LADDER: ATTACK no R2 claim (target not adjacent) — fine kept as R1 weight");
                }
            } else if (attack != null && attack.viable) {
                // Attack possible but no force drain - much smaller bonus
                // Don't waste moves just to attack weak positions
                action.addReasoning("Possible attack (no drain icons)", 15.0f);
                logger.debug("[MoveEvaluator] Weak attack opportunity (no icons): {}", attack.reason);
                // T4.1 (2026-07-06): early return removed — R1 fine, block falls through.
            }
        }

        // === V29.7: WEAPON HUNTER — Armed characters should seek battle ===
        // Vader with lightsaber, or any weapon-equipped high-power character alone at
        // an uncontested location should move to engage opponents. A weapon-equipped
        // character like Vader (power 6 + lightsaber hit + throw + IHYN) is devastating
        // and worth far more than their base power suggests.
        // Bypasses the myCardCount >= 2 requirement for armed characters.
        //
        // V29.9: If character has NO weapon, penalize attack moves.
        // Vader without lightsaber should NOT be sent to fight — he needs to get armed first.
        // Deploy lightsaber on him BEFORE sending him into battle.
        if (!theirHasCards && myPower >= ATTACK_MIN_POWER && myCardCount == 1 && cardToMove != null) {
            // V29.9 PRE-CHECK: If this is Vader without a weapon, BLOCK aggressive moves
            try {
                String preCharTitle = cardToMove.getTitle() != null ? cardToMove.getTitle().toLowerCase(Locale.ROOT) : "";
                if (preCharTitle.contains("vader")) {
                    boolean vaderHasWeapon = false;
                    List<PhysicalCard> vaderAtt = gameState.getAttachedCards(cardToMove);
                    if (vaderAtt != null) {
                        for (PhysicalCard att : vaderAtt) {
                            if (att != null && att.getBlueprint() != null
                                && att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                vaderHasWeapon = true;
                                break;
                            }
                        }
                    }
                    if (!vaderHasWeapon) {
                        // Check if lightsaber is in hand — if so, equip first!
                        boolean saberInHand = false;
                        List<PhysicalCard> vHand = gameState.getHand(playerId);
                        if (vHand != null) {
                            for (PhysicalCard hCard : vHand) {
                                if (hCard != null && hCard.getTitle() != null
                                    && hCard.getTitle().toLowerCase(Locale.ROOT).contains("lightsaber")) {
                                    saberInHand = true;
                                    break;
                                }
                            }
                        }
                        if (saberInHand) {
                            action.addReasoning("V29.9 UNARMED VADER: Lightsaber in hand — EQUIP FIRST before attacking!", -250.0f);
                            logger.warn("V29.9 UNARMED VADER: Vader has no weapon but lightsaber in hand — blocking attack move (-250)");
                            // T4.1 (2026-07-06): early return removed — R1 weight, block falls through.
                        } else {
                            action.addReasoning("V29.9 UNARMED VADER: No weapon — vulnerable without lightsaber!", -100.0f);
                            logger.warn("V29.9 UNARMED VADER: Vader has no weapon and none in hand — penalizing attack move (-100)");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.9: Error checking Vader weapon status: {}", e.getMessage());
            }
            try {
                boolean hasWeapon = false;
                boolean isLightsaber = false;
                String weaponName = null;
                List<PhysicalCard> attached = gameState.getAttachedCards(cardToMove);
                if (attached != null) {
                    for (PhysicalCard att : attached) {
                        if (att == null || att.getBlueprint() == null) continue;
                        if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                            hasWeapon = true;
                            weaponName = att.getTitle();
                            if (weaponName != null && weaponName.toLowerCase(Locale.ROOT).contains("lightsaber")) {
                                isLightsaber = true;
                            }
                        }
                    }
                }

                if (hasWeapon) {
                    String charTitle = cardToMove.getTitle() != null ? cardToMove.getTitle() : "character";
                    String charLower = charTitle.toLowerCase(Locale.ROOT);
                    boolean isVader = charLower.contains("vader");

                    // Check for IHYN (I Have You Now) in hand — makes Vader even more devastating
                    boolean hasIHYN = false;
                    if (isVader) {
                        try {
                            List<PhysicalCard> hand = gameState.getHand(playerId);
                            if (hand != null) {
                                for (PhysicalCard hCard : hand) {
                                    if (hCard != null && hCard.getTitle() != null) {
                                        String hTitle = hCard.getTitle().toLowerCase(Locale.ROOT);
                                        if (hTitle.contains("i have you now")) {
                                            hasIHYN = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }

                    // Calculate effective power with weapon bonus
                    // Lightsaber: +4 (weapon hit power + throw destiny)
                    // Other weapon: +2
                    // IHYN in hand: +3 (extra destiny draws)
                    float effectivePower = myPower;
                    if (isLightsaber) effectivePower += 4.0f;
                    else effectivePower += 2.0f;
                    if (hasIHYN) effectivePower += 3.0f;

                    // Look for opponent locations to attack (opponentId already declared above)
                    float bestAttackScore = 0;
                    String bestTargetLoc = null;
                    PhysicalCard bestTargetLocCard = null; // T4.1: tracked for the R2 adjacency gate
                    boolean foundLuke = false;

                    for (PhysicalCard adjLocation : gameState.getLocationsInOrder()) {
                        if (adjLocation == location) continue;

                        float theirPowerThere = 0;
                        int theirCountThere = 0;
                        boolean lukeHere = false;

                        List<PhysicalCard> cardsAtAdj = gameState.getCardsAtLocation(adjLocation);
                        for (PhysicalCard card : cardsAtAdj) {
                            if (card == null) continue;
                            String owner = card.getOwner();
                            SwccgCardBlueprint bp = card.getBlueprint();
                            if (bp == null) continue;

                            if (opponentId != null && opponentId.equals(owner)) {
                                if (bp.hasPowerAttribute()) {
                                    Float pw = bp.getPower();
                                    theirPowerThere += (pw != null ? pw : 0);
                                    theirCountThere++;
                                }
                                // Check for Luke (Hunt Down target)
                                if (isVader && card.getTitle() != null
                                    && card.getTitle().toLowerCase(Locale.ROOT).contains("luke")) {
                                    lukeHere = true;
                                }
                            }
                        }

                        if (theirCountThere > 0 && effectivePower > theirPowerThere) {
                            // We can beat them with our weapon advantage
                            float attackScore = 60.0f;
                            float powerAdvantage = effectivePower - theirPowerThere;

                            // Bonus for bigger power advantage
                            if (powerAdvantage >= 6) attackScore += 40.0f;
                            else if (powerAdvantage >= 3) attackScore += 20.0f;

                            // Bonus for opponent icons (force drain value after winning)
                            SwccgCardBlueprint locBp = adjLocation.getBlueprint();
                            if (locBp != null) {
                                int oppIcons = (mySide == Side.DARK)
                                    ? locBp.getIconCount(Icon.LIGHT_FORCE)
                                    : locBp.getIconCount(Icon.DARK_FORCE);
                                attackScore += oppIcons * ICON_BONUS;
                            }

                            // HUGE bonus for Luke (Hunt Down objective target)
                            if (lukeHere && isVader) {
                                attackScore += 150.0f;
                                foundLuke = true;
                            }

                            if (attackScore > bestAttackScore) {
                                bestAttackScore = attackScore;
                                bestTargetLoc = adjLocation.getTitle();
                                bestTargetLocCard = adjLocation; // T4.1: for the adjacency gate
                            }
                        }
                    }

                    if (bestAttackScore > 0 && bestTargetLoc != null) {
                        String reason;
                        if (foundLuke) {
                            reason = String.format("V29.7 WEAPON HUNTER: %s + %s should CHALLENGE LUKE at %s! (effective power %.0f)",
                                charTitle, weaponName, bestTargetLoc, effectivePower);
                            if (hasIHYN) reason += " + IHYN in hand!";
                        } else {
                            reason = String.format("V29.7 WEAPON HUNTER: %s + %s should attack %s (effective power %.0f vs opponents)",
                                charTitle, weaponName, bestTargetLoc, effectivePower);
                        }
                        action.addReasoning(reason, bestAttackScore);
                        logger.info("[MoveEvaluator] ⚔️ {} — score {}", reason, bestAttackScore);
                        // V29.7 UPDATED 2026-07-06 T4.1: early return removed. Claims R2 DOCTRINE
                        // (battle-seeking) ONLY when the best target is ADJACENT (same engine call
                        // V85 uses) — the destination-blind Rey class (+130 for an unreachable
                        // target) stays an R1 fine and V85's -800 weight holds her (V85-protect
                        // boundary: -720 < Pass). The L2 strength gate also applies (fine >= +200).
                        boolean v297TargetAdjacent = false;
                        try {
                            v297TargetAdjacent = bestTargetLocCard != null
                                && game.getModifiersQuerying().isAdjacentSites(gameState, location, bestTargetLocCard);
                        } catch (Exception ignore) { /* false */ }
                        if (v297TargetAdjacent) {
                            ladderClaimR2("V29.7 WEAPON HUNTER", bestAttackScore, 0.0f, true);
                        } else {
                            logger.info("LADDER: V29.7 no R2 claim (target not adjacent) — fine kept as R1 weight");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.7: Error in weapon hunter check: {}", e.getMessage());
            }
        }

        // === SPREAD VIABILITY ===
        // Check if we have excess power we can redistribute
        float powerNeededToStay = Math.max(theirPower + OVERKILL_THRESHOLD, ESTABLISH_THRESHOLD);
        float excessPower = myPower - powerNeededToStay;

        if (excessPower >= 2 && myCardCount >= 2) {
            SpreadAnalysis spread = analyzeSpreadViability(gameState, game, playerId, mySide,
                                                           location, myPower, myCardCount, theirPower);
            if (spread != null && spread.viable) {
                action.addReasoning(spread.reason, spread.score);
                // T4.1 (2026-07-06): early return removed. SPREAD attempts an R2 DOCTRINE
                // claim (spec Table 2); its "contest" branch is battle-seeking. Typical
                // scores (< +200, no drain-delta) fail the L2 strength gate and stay R1.
                ladderClaimR2("SPREAD", spread.score, 0.0f,
                    spread.reason != null && spread.reason.startsWith("Can contest"));
            } else if (spread != null) {
                action.addReasoning("Can't spread: " + spread.reason, BAD_DELTA);
                // T4.1 (2026-07-06): early return removed — R1 fine, block falls through.
            }
        }

        // === V29.13: FORCE DRAIN MODIFIER CHECK — AVOID BAD DRAIN LOCATIONS ===
        // Rando was moving to locations with -1 force drain modifiers instead of better sites.
        // Check the force drain amount at the destination vs current location.
        // Penalize moves to locations where our force drain would be low/reduced.
        if (gameState != null && game != null && location != null) {
            try {
                // Extract destination location from action text
                // Format: "Move X from A to B using landspeed" or "Move X from A to B using card"
                String actionTextLowerFD = action.getDisplayText() != null
                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                PhysicalCard destLocation = null;

                // Try to match destination by checking all locations
                for (PhysicalCard locCard : gameState.getLocationsInOrder()) {
                    if (locCard == null || locCard == location) continue;
                    String locName = locCard.getTitle() != null
                        ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";
                    if (!locName.isEmpty() && actionTextLowerFD.contains(locName)) {
                        destLocation = locCard;
                        break;
                    }
                }

                if (destLocation != null) {
                    // V29.13 UPDATED 2026-07-06 T4.1: drain math routed through the shared engine
                    // metric MovePredicates.drainAt (behavior-identical — already engine-based;
                    // move-6 single-detection consolidation).
                    float destDrainAmount = MovePredicates.drainAt(game, gameState, destLocation, playerId);
                    float currentDrainAmount = MovePredicates.drainAt(game, gameState, location, playerId);

                    // V73 (Steve, 2026-05-15): Dropped the `< 1` and `>= 2` thresholds
                    // that left Cantina(2) → Lars Farm(1) un-penalized (and
                    // drain-0 → drain-1 un-bonused). Now any drain decrease is
                    // penalized, any drain increase is bonused, scaled by delta.
                    if (destDrainAmount < currentDrainAmount) {
                        float delta = currentDrainAmount - destDrainAmount;
                        float drainPenalty = -40.0f * delta;  // -40 per drain lost
                        if (destDrainAmount <= 0) drainPenalty -= 80.0f;  // extra penalty for drain-0
                        action.addReasoning(String.format(
                            "V29.13 BAD DRAIN SITE: %s has drain %.0f (current location has %.0f) — stay for better drain!",
                            destLocation.getTitle(), destDrainAmount, currentDrainAmount), drainPenalty);
                        logger.warn("V29.13 BAD DRAIN: Moving to {} drain={} vs current {} drain={} — penalty {}",
                            destLocation.getTitle(), (int)destDrainAmount,
                            location.getTitle(), (int)currentDrainAmount, (int)drainPenalty);
                    } else if (destDrainAmount > currentDrainAmount) {
                        float delta = destDrainAmount - currentDrainAmount;
                        float drainBonus = 40.0f * delta;  // +40 per drain gained
                        action.addReasoning(String.format(
                            "V29.13 GOOD DRAIN SITE: %s has drain %.0f — better than current %.0f!",
                            destLocation.getTitle(), destDrainAmount, currentDrainAmount), drainBonus);
                        logger.info("V29.13 GOOD DRAIN: Moving to {} drain={} from {} drain={} — bonus {}",
                            destLocation.getTitle(), (int)destDrainAmount,
                            location.getTitle(), (int)currentDrainAmount, (int)drainBonus);
                        // V29.13 UPDATED 2026-07-06 T4.1: a positive drain-delta claims R2 DOCTRINE
                        // (non-battle). L2 strength gate: accepted only when drainDelta >= 2
                        // (fine 40*delta alone is < +200 for small deltas — exactly ruling L2's
                        // drain-delta arm).
                        ladderClaimR2("V29.13 GOOD DRAIN", drainBonus, delta, false);
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.13 DRAIN CHECK: Error: {}", e.getMessage());
            }
        }

        // === V91 (Steve, 2026-05-19): ESCAPE LANDED-SHIP TRAP ===
        // Per Steve: replay d483o8y8rjen117p — Rando deployed Kylo Ren's
        // Command Shuttle to "Jakku: Niima Marketplace" (a SITE, not the
        // Jakku system), then deployed Kylo aboard as pilot. Ship at a
        // site is "landed" → contributes 0 power. Rando's move phase did
        // NOT disembark Kylo or take off to system. Asdf clobbered the
        // power-0 ship next turn.
        //
        // Rule: when our character is aboard a starship at a NON-SYSTEM
        // location (i.e., landed at a site), score "take off" / "disembark"
        // moves with a strong bonus so Rando escapes the trap. Either:
        //   - Take off → ship moves to related system, gets its power back
        //   - Disembark → pilot stays at site, uses ground combat
        //
        // Detected by action text patterns. SWCCG move actions for landed
        // ships use phrases like "Take off", "Disembark", "Move to system".
        if (location != null && location.getBlueprint() != null && game != null) {
            try {
                boolean currentIsSystem = false;
                try {
                    currentIsSystem = location.getBlueprint().getCardSubtype() == CardSubtype.SYSTEM;
                } catch (Exception ignore) { /* */ }
                if (!currentIsSystem) {
                    // Are we aboard a starship at this site? If the card being
                    // moved is itself a pilot/character aboard, or is a landed
                    // starship at the site, this rule applies.
                    String v91ActionLower = action.getDisplayText() != null
                        ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                    boolean isTakeOff = v91ActionLower.contains("take off");
                    boolean isDisembark = v91ActionLower.contains("disembark");
                    boolean isMoveAboard = v91ActionLower.contains("embark")
                        && !isDisembark;  // exclude disembark which contains "embark"
                    if (isTakeOff || isDisembark) {
                        // Check if any friendly character is currently aboard a
                        // landed starship at this site.
                        boolean weHaveLandedShipHere = false;
                        for (PhysicalCard pCard : gameState.getAllPermanentCards()) {
                            if (pCard == null) continue;
                            if (!playerId.equals(pCard.getOwner())) continue;
                            if (pCard.getBlueprint() == null) continue;
                            if (pCard.getBlueprint().getCardCategory() != CardCategory.STARSHIP) continue;
                            PhysicalCard pLoc = null;
                            try {
                                pLoc = game.getModifiersQuerying().getLocationThatCardIsAt(gameState, pCard);
                            } catch (Exception ignore) { /* */ }
                            if (pLoc == location) {
                                weHaveLandedShipHere = true;
                                break;
                            }
                        }
                        if (weHaveLandedShipHere) {
                            float bonus = isTakeOff ? 800.0f : 600.0f;
                            action.addReasoning(String.format(
                                "V91 ESCAPE LANDED SHIP: %s at site %s — %s to restore ship power / use character on ground",
                                isTakeOff ? "Take off" : "Disembark",
                                location.getTitle(),
                                isTakeOff ? "lift to system" : "drop pilot to ground"), bonus);
                            logger.warn("V91 ESCAPE LANDED SHIP: bonus {} for {} at landed site {}",
                                (int)bonus, isTakeOff ? "take-off" : "disembark", location.getTitle());
                            // V91 UPDATED 2026-07-06 T4.1: landed-ship escape claims R3 SURVIVAL
                            // (fines +800/+600 kept; base applied at the finalizer).
                            ladderClaimR3("V91 ESCAPE LANDED SHIP");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V91 ESCAPE LANDED SHIP: error: {}", e.getMessage());
            }
        }

        // === V73 (Steve, 2026-05-15): MULTI-DRAIN SHUTTLE PATTERN ===
        // Documented Cantina ↔ Mos Eisley shuttle: deploy chars at one,
        // move ONE to the other during Control phase via Mos Eisley's free-move
        // game text, drain at BOTH sites, move back.
        //
        // Net: +1 extra drain/turn from Tatooine. V29.13 alone would penalize
        // the move from Cantina(drain 2-3) → Mos Eisley(drain 1) as "bad drain
        // site", killing the shuttle. V73 detects the shuttle pattern by
        // title and overrides with a +400 bonus that beats V29.13's penalty.
        //
        // Generalizes: same logic applies to ANY two Rando-controlled sites
        // where the destination has its own drain value > 0 AND Rando still has
        // chars at the source (preserving the source drain).
        if (location != null && location.getTitle() != null && game != null) {
            try {
                String srcTitleLower = location.getTitle().toLowerCase(Locale.ROOT);
                String destTitleLower = "";
                PhysicalCard destLoc = null;
                String actionDisplay = action.getDisplayText() != null
                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                // Identify destination from action text
                for (PhysicalCard loc : gameState.getTopLocations()) {
                    if (loc == null || loc == location) continue;
                    String locTitle = loc.getTitle();
                    if (locTitle == null) continue;
                    String ltLower = locTitle.toLowerCase(Locale.ROOT);
                    if (!ltLower.isEmpty() && actionDisplay.contains(ltLower)) {
                        destLoc = loc;
                        destTitleLower = ltLower;
                        break;
                    }
                }

                if (destLoc != null) {
                    // Specific shuttle: Cantina ↔ Mos Eisley (Mos Eisley's text grants the free-move)
                    boolean cantinaMosEisleyShuttle =
                        (srcTitleLower.contains("cantina") && destTitleLower.contains("mos eisley"))
                        || (srcTitleLower.contains("mos eisley") && destTitleLower.contains("cantina"));

                    if (cantinaMosEisleyShuttle) {
                        // Check that source will RETAIN a character after this move
                        // (we don't want to abandon Cantina entirely)
                        int srcCharsRemainingAfterMove = 0;
                        for (PhysicalCard c : gameState.getCardsAtLocation(location)) {
                            if (c == null || c == cardToMove) continue;
                            if (!playerId.equals(c.getOwner())) continue;
                            if (c.getBlueprint() == null) continue;
                            if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                            srcCharsRemainingAfterMove++;
                        }
                        if (srcCharsRemainingAfterMove >= 1) {
                            // Source keeps a draining presence → shuttle is net-positive
                            action.addReasoning(String.format(
                                "V73 SHUTTLE: Cantina ↔ Mos Eisley shuttle — drain BOTH this turn (%d chars stay at %s)",
                                srcCharsRemainingAfterMove, location.getTitle()), 400.0f);
                            logger.warn("V73 SHUTTLE: {} → {} — drain BOTH Tatooine sites (+400)",
                                location.getTitle(), destLoc.getTitle());
                            // V73 UPDATED 2026-07-06 T4.1: the shuttle claims R2 DOCTRINE (non-battle;
                            // +400 passes L2). This is the move-1 boundary fix: V85's old -2000+return
                            // buried the shuttle forever; now R2 base + fines = +5560 > Pass.
                            ladderClaimR2("V73 SHUTTLE", 400.0f, 0.0f, false);
                        } else {
                            // Source becomes empty → not a shuttle, just a relocation
                            logger.debug("V73: Cantina ↔ Mos Eisley move but source goes empty — no shuttle bonus");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V73 SHUTTLE check error: {}", e.getMessage());
            }
        }

        // === V34: DESTINATION-AWARE CONTEST BONUS ===
        // Check if the specific destination of this move has opponents.
        // Moving TOWARD opponents = good (can battle next turn, block their drains).
        // Moving to empty location while opponents drain uncontested elsewhere = bad.
        // This fixes the bug where Hunt Down and weapon hunter bonuses applied equally
        // to ALL move actions regardless of where they actually go.
        {
            String v34ActionText = action.getDisplayText() != null
                ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
            PhysicalCard v34Dest = null;

            for (PhysicalCard locCard : gameState.getLocationsInOrder()) {
                if (locCard == null || locCard == location) continue;
                String locName = locCard.getTitle() != null
                    ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";
                if (!locName.isEmpty() && v34ActionText.contains(locName)) {
                    v34Dest = locCard;
                    break;
                }
            }

            if (v34Dest != null) {
                float destOppPower = 0;
                try {
                    destOppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                        gameState, v34Dest, opponentId, false, false);
                } catch (Exception e) { /* ignore */ }

                if (destOppPower > 0) {
                    // Moving TO a location with opponents — CONTEST their drain!
                    // V36: Extra bonus if they're draining there UNCONTESTED
                    float ourPowerAtDest = 0;
                    try {
                        ourPowerAtDest = game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, v34Dest, playerId, false, false);
                    } catch (Exception e) { /* ignore */ }
                    float contestBonus = 250.0f;
                    if (ourPowerAtDest == 0) {
                        // UNCONTESTED drain! Extra urgency
                        contestBonus += 150.0f;
                        logger.warn("V36 CONTEST DRAIN: {} — opponent drains UNCONTESTED at {} — extra urgency!",
                            cardToMove != null ? cardToMove.getTitle() : "?", v34Dest.getTitle());
                    }
                    // Extra bonus if we're armed (can battle effectively)
                    if (cardToMove != null) {
                        try {
                            List<PhysicalCard> v34Att = gameState.getAttachedCards(cardToMove);
                            if (v34Att != null) {
                                for (PhysicalCard att : v34Att) {
                                    if (att != null && att.getBlueprint() != null
                                        && att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                        contestBonus += 100.0f; // Armed = even better for contesting
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    // V35: Extra bonus if destination has Jedi/Padawan and we're Vader
                    boolean v35JediAtDest = false;
                    try {
                        for (PhysicalCard dc : gameState.getCardsAtLocation(v34Dest)) {
                            if (dc == null || playerId.equals(dc.getOwner())) continue;
                            String dcTitle = dc.getTitle() != null ? dc.getTitle().toLowerCase(Locale.ROOT) : "";
                            if (isJediOrPadawan(dcTitle)) {
                                v35JediAtDest = true;
                                break;
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (v35JediAtDest && cardToMove != null && cardToMove.getTitle() != null
                        && cardToMove.getTitle().toLowerCase(Locale.ROOT).contains("vader")) {
                        contestBonus += 150.0f; // V35: Vader hunting Jedi
                    }

                    action.addReasoning(String.format(
                        "V34 CONTEST: Moving to %s where opponents have power %.0f%s — block their drain and fight!",
                        v34Dest.getTitle(), destOppPower, v35JediAtDest ? " [JEDI!]" : ""), contestBonus);
                    logger.warn("V34 CONTEST: {} moving to {} (opponent power {}{}) — bonus +{}",
                        cardToMove != null ? cardToMove.getTitle() : "?",
                        v34Dest.getTitle(), (int)destOppPower,
                        v35JediAtDest ? " JEDI" : "", (int)contestBonus);
                    // V34/V36 UPDATED 2026-07-06 T4.1: the contest claims R2 DOCTRINE
                    // (battle-seeking — subject to the V137 canWinAt veto per ruling L3;
                    // move-4a/4b boundaries). Bonus >= +250 always passes the L2 gate.
                    ladderClaimR2("V34 CONTEST", contestBonus, 0.0f, true);
                } else {
                    // Moving to empty location — check if opponents are draining uncontested elsewhere
                    boolean opponentsUncontested = false;
                    String opUncontestedLoc = null;
                    float opUncontestedPower = 0;
                    try {
                        for (PhysicalCard otherLoc : gameState.getLocationsInOrder()) {
                            if (otherLoc == null || otherLoc == location || otherLoc == v34Dest) continue;
                            float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, otherLoc, opponentId, false, false);
                            float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, otherLoc, playerId, false, false);
                            if (oppPower > 0 && ourPowerThere == 0) {
                                opponentsUncontested = true;
                                if (oppPower > opUncontestedPower) {
                                    opUncontestedPower = oppPower;
                                    opUncontestedLoc = otherLoc.getTitle();
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (opponentsUncontested) {
                        // === V111: NON-BATTLEGROUND → BATTLEGROUND ADVANCE ===
                        // Exception to V38.3: moving from a non-battleground to an adjacent
                        // battleground is STRATEGIC, not wasteful. Pattern: deploy to non-BG
                        // (e.g. Imperial City via Reserve Deck pull), then advance to adjacent
                        // BG (e.g. Xizor's Palace) for force drain. Don't hard-block this.
                        boolean v111CurrentNonBG = false;
                        boolean v111DestBG = false;
                        try {
                            v111CurrentNonBG = !game.getModifiersQuerying().isBattleground(gameState, location, null);
                            v111DestBG = game.getModifiersQuerying().isBattleground(gameState, v34Dest, null);
                        } catch (Exception e) { /* ignore */ }

                        if (v111CurrentNonBG && v111DestBG) {
                            action.addReasoning(String.format(
                                "V111 BG ADVANCE: Moving from non-battleground %s to battleground %s — establish drain position!",
                                location.getTitle(), v34Dest.getTitle()), 400.0f);
                            logger.info("V111 BG ADVANCE: {} from {} (non-BG) to {} (BG) — drain position +400",
                                cardToMove != null ? cardToMove.getTitle() : "?",
                                location.getTitle(), v34Dest.getTitle());
                            // V111 UPDATED 2026-07-06 T4.1: the BG advance claims R2 DOCTRINE
                            // (non-battle: destination is EMPTY; +400 passes L2, existing V38.3 carve kept).
                            ladderClaimR2("V111 BG ADVANCE", 400.0f, 0.0f, false);
                        } else {
                            // V38.3 UPDATED 2026-07-06 T4.1: the -9999 hard block is now a DEFERRED
                            // ladder veto (-100000 at the finalizer) so the V53b mandatory-transit
                            // claim identities can suppress it (ruling L3 carve-out; move-3f
                            // boundary: a Mapuzo exit to an EMPTY site must fire — the old code
                            // stalled at ~-9199). Non-transit movers still get the full veto.
                            ladderWrongDirVeto = true;
                            ladderWrongDirVetoReason = String.format(
                                "V38.3 WRONG DIRECTION: Moving to empty %s while opponents at %s",
                                v34Dest.getTitle(), opUncontestedLoc);
                            logger.warn("V38.3 WRONG DIRECTION: {} to empty {} — opponents at {} — veto flagged (transit carve-out may suppress)",
                                cardToMove != null ? cardToMove.getTitle() : "?",
                                v34Dest.getTitle(), opUncontestedLoc);
                        }
                    }

                    // V38.3: CASTLE RETREAT BLOCK — NEVER move to Mustafar: Vader's Castle
                    // when there are opponents ANYWHERE on the board. Castle is a safe haven
                    // that contributes nothing to the fight.
                    String v34DestTitle = v34Dest.getTitle() != null
                        ? v34Dest.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    if (v34DestTitle.contains("mustafar") && v34DestTitle.contains("castle")) {
                        boolean anyOpponentsOnBoard = false;
                        try {
                            for (PhysicalCard otherLoc2 : gameState.getLocationsInOrder()) {
                                if (otherLoc2 == null) continue;
                                float op2 = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, otherLoc2, opponentId, false, false);
                                if (op2 > 0) { anyOpponentsOnBoard = true; break; }
                            }
                        } catch (Exception e) { /* ignore */ }
                        if (anyOpponentsOnBoard) {
                            // V38.3 UPDATED 2026-07-06 T4.1: Castle-retreat arm converted to the
                            // ladder hard-veto class (-100000 at the finalizer). NO transit
                            // carve-out here — the carve-out is keyed to the V53b transit claim
                            // identities and a Mapuzo exit never targets Mustafar Castle.
                            ladderVetoHard = true;
                            ladderVetoHardReason = "V38.3 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!";
                            logger.warn("V38.3 CASTLE RETREAT BLOCKED (LADDER VETO): {} trying to flee to Mustafar Castle!",
                                cardToMove != null ? cardToMove.getTitle() : "?");
                        }
                    }
                }
            }
        }

        // Default: not a good time to move - strong penalty to avoid wasteful moves
        // Moves cost force and can leave positions vulnerable
        // T4.1 (2026-07-06): moved to the ladder finalizer — applied there only when
        // rank==R1 (no accepted claim) so R2+ claims are not double-taxed.
    }

    /**
     * Calculate threat level based on power differential.
     * Ported from Python move_evaluator.py threat level logic.
     */
    /** V47/V37.1 helper (2026-07-10, Rey replay rbujmoc90br3uu4c): opponent WEAPON power at a
     *  location — raw power totals are blind to weapons/hits. V29.7 heuristic: lightsaber +5,
     *  other weapon +3; counts attached WEAPON cards and permanent weapons (game text). */
    private static float oppWeaponBonusAt(GameState gs, PhysicalCard location, String oppId) {
        float bonus = 0f;
        if (gs == null || location == null || oppId == null) return 0f;
        try {
            for (PhysicalCard c : gs.getCardsAtLocation(location)) {
                if (c == null || c.getBlueprint() == null) continue;
                if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                if (!oppId.equals(c.getOwner())) continue;
                List<PhysicalCard> atts = gs.getAttachedCards(c);
                if (atts != null) {
                    for (PhysicalCard att : atts) {
                        if (att == null || att.getBlueprint() == null) continue;
                        if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                            String wt = att.getTitle() != null ? att.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            bonus += wt.contains("lightsaber") ? 5.0f : 3.0f;
                        }
                    }
                }
                String gt = c.getBlueprint().getGameText();
                if (gt != null && gt.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                    String ct = c.getTitle() != null ? c.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    bonus += ct.contains("lightsaber") ? 5.0f : 3.0f;
                }
            }
        } catch (Exception e) { /* fail-open: 0 bonus */ }
        return bonus;
    }

    private ThreatLevel calculateThreatLevel(float powerDiff) {
        int favorable = RandoConfig.BATTLE_FAVORABLE_THRESHOLD;
        int danger = RandoConfig.BATTLE_DANGER_THRESHOLD;

        if (powerDiff >= favorable + 4) {
            return ThreatLevel.CRUSH;
        } else if (powerDiff >= favorable) {
            return ThreatLevel.FAVORABLE;
        } else if (powerDiff >= -favorable) {
            return ThreatLevel.RISKY;
        } else if (powerDiff >= danger) {
            return ThreatLevel.DANGEROUS;
        } else {
            return ThreatLevel.RETREAT;
        }
    }

    /**
     * Analyze attack opportunities at adjacent locations.
     * Ported from Python move_evaluator.py _analyze_attack_opportunity
     */
    private AttackAnalysis analyzeAttackOpportunity(GameState gameState, SwccgGame game,
                                                     String playerId, Side mySide,
                                                     PhysicalCard currentLocation,
                                                     float ourPowerHere, int ourCardCount) {
        String opponentId = gameState.getOpponent(playerId);
        float avgPowerPerCard = ourPowerHere / Math.max(ourCardCount, 1);

        // Get all locations
        List<PhysicalCard> allLocations = gameState.getLocationsInOrder();
        AttackAnalysis bestAttack = null;
        float bestScore = 0;

        for (PhysicalCard adjLocation : allLocations) {
            if (adjLocation == currentLocation) continue;

            // Calculate enemy power at this location
            float theirPower = 0;
            int theirCount = 0;
            float ourPowerThere = 0;

            List<PhysicalCard> cardsAtAdj = gameState.getCardsAtLocation(adjLocation);
            for (PhysicalCard card : cardsAtAdj) {
                if (card == null) continue;
                String owner = card.getOwner();
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null || !bp.hasPowerAttribute()) continue;

                Float power = bp.getPower();
                if (power == null) power = 0f;

                if (opponentId != null && opponentId.equals(owner)) {
                    // V67f3: Exclude opponent's undercover spies from "attack power" —
                    // a spy doesn't actively threaten us; piling characters into a spy
                    // site wastes drain potential. Spy stays undercover and keeps
                    // blocking our drain regardless of our character count.
                    if (card.isUndercover()) continue;
                    theirPower += power;
                    theirCount++;
                } else if (playerId.equals(owner)) {
                    ourPowerThere += power;
                }
            }

            // Skip empty locations (use spread logic for those)
            if (theirCount == 0 || theirPower == 0) continue;

            // Get opponent icons at target
            int theirIcons = getOpponentIcons(adjLocation.getBlueprint(), mySide);

            // Calculate attack viability
            float potentialPower = ourPowerThere + ourPowerHere;  // If we move everyone
            float advantage = potentialPower - theirPower;

            if (advantage >= ATTACK_POWER_ADVANTAGE) {
                float score = 50.0f;  // Base attack score

                // Bonus for crushing attacks
                if (potentialPower >= theirPower * 2) {
                    score += 25.0f;
                }

                // Bonus for opponent icons
                score += theirIcons * ICON_BONUS;

                // Bonus for bigger enemy forces
                score += theirPower / 2;

                String reason = String.format("ATTACK %d enemies with %d power (+%d advantage)",
                    (int)theirPower, (int)potentialPower, (int)advantage);
                if (theirIcons > 0) {
                    reason += " - deny " + theirIcons + " icon drain!";
                }

                boolean hasForcedrainPotential = theirIcons > 0;
                if (score > bestScore) {
                    bestScore = score;
                    // T4.1 (2026-07-06): carry the target location for the R2 adjacency gate.
                    bestAttack = new AttackAnalysis(true, reason, score, hasForcedrainPotential, adjLocation);
                }
            }
        }

        return bestAttack;
    }

    /**
     * Analyze if spreading out from this location is viable.
     * Ported from Python move_evaluator.py _analyze_spread_viability
     */
    private SpreadAnalysis analyzeSpreadViability(GameState gameState, SwccgGame game,
                                                   String playerId, Side mySide,
                                                   PhysicalCard currentLocation,
                                                   float ourPowerHere, int ourCardCount,
                                                   float theirPowerHere) {
        String opponentId = gameState.getOpponent(playerId);
        int forceAvailable = 0;  // TODO: Get from context if available

        // Calculate power we need to retain at source
        float powerToRetain = Math.max(theirPowerHere + CONTEST_MARGIN, ESTABLISH_THRESHOLD);
        float avgPowerPerCard = ourPowerHere / Math.max(ourCardCount, 1);
        float powerWeCanSpare = ourPowerHere - powerToRetain;

        if (powerWeCanSpare < 2) {
            return new SpreadAnalysis(false,
                String.format("need %d power to retain control, only have %d",
                    (int)powerToRetain, (int)ourPowerHere), 0);
        }

        // Get all locations and find spread opportunities
        List<PhysicalCard> allLocations = gameState.getLocationsInOrder();
        SpreadAnalysis bestOpportunity = null;
        float bestScore = 0;

        for (PhysicalCard adjLocation : allLocations) {
            if (adjLocation == currentLocation) continue;

            // Calculate power at this location
            float theirPower = 0;
            float ourPowerThere = 0;

            List<PhysicalCard> cardsAtAdj = gameState.getCardsAtLocation(adjLocation);
            for (PhysicalCard card : cardsAtAdj) {
                if (card == null) continue;
                String owner = card.getOwner();
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null || !bp.hasPowerAttribute()) continue;

                Float power = bp.getPower();
                if (power == null) power = 0f;

                if (opponentId != null && opponentId.equals(owner)) {
                    theirPower += power;
                } else if (playerId.equals(owner)) {
                    ourPowerThere += power;
                }
            }

            // Skip if we already have good presence
            if (ourPowerThere >= ESTABLISH_THRESHOLD && theirPower == 0) {
                continue;
            }

            // Get icons at destination
            int theirIcons = getOpponentIcons(adjLocation.getBlueprint(), mySide);
            int myIcons = getMyIcons(adjLocation.getBlueprint(), mySide);

            float potentialPower = ourPowerThere + powerWeCanSpare;

            // Empty location - can we establish?
            if (theirPower == 0) {
                if (potentialPower >= ESTABLISH_THRESHOLD) {
                    float score = GOOD_DELTA * 2;
                    score += theirIcons * ICON_BONUS;  // Bonus for opponent icons

                    String reason = "Can establish at empty location";
                    if (theirIcons > 0) {
                        reason += " - " + theirIcons + " opponent icon(s) = force drain!";
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestOpportunity = new SpreadAnalysis(true, reason, score);
                    }
                }
            } else {
                // Contested - can we beat them with margin?
                float powerNeeded = theirPower + CONTEST_MARGIN;
                if (potentialPower >= powerNeeded) {
                    float score = GOOD_DELTA * 3 + theirPower / 2;
                    score += theirIcons * ICON_BONUS;

                    String reason = String.format("Can contest location with %d enemies", (int)theirPower);
                    if (theirIcons > 0) {
                        reason += " - " + theirIcons + " opponent icon(s) = force drain!";
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestOpportunity = new SpreadAnalysis(true, reason, score);
                    }
                }
            }
        }

        if (bestOpportunity != null) {
            return bestOpportunity;
        }

        return new SpreadAnalysis(false, "no good adjacent locations", 0);
    }

    /**
     * Handle Land action - penalize starfighters.
     */
    private void handleLandAction(EvaluatedAction action, String actionLower, PhysicalCard card, SwccgGame game) {
        boolean isStarfighter = false;
        boolean isStarship = false;
        boolean hasPassengers = false;
        String cardName = "unknown";

        if (card != null) {
            cardName = card.getTitle();
            SwccgCardBlueprint bp = card.getBlueprint();
            CardSubtype subtype = bp != null ? bp.getCardSubtype() : null;
            if (subtype == CardSubtype.STARFIGHTER) {
                isStarfighter = true;
                isStarship = true;
            } else if (subtype == CardSubtype.CAPITAL || subtype == CardSubtype.TRANSPORT) {
                isStarship = true;
            }

            // V67f1: ACTUAL passenger check. The previous V49 logic ASSUMED any
            // capital/transport ship has passengers, which let Wild Karrde land
            // alone at sites with high enemy power → instant overflow death.
            // Fix: scan game state for any character "aboard" this ship via the
            // Filters.aboard filter — only "has passengers" if at least one is.
            // FIXES uarc0hmiai1i594y replay: Wild Karrde landed at Cloud City: Upper
            // Walkway (Steve's stack) with power 0 → overflow.
            if (isStarship && !isStarfighter) {
                int actualOnboard = 0;
                try {
                    if (game != null && card != null) {
                        java.util.Collection<PhysicalCard> aboard =
                            com.gempukku.swccgo.filters.Filters.filter(
                                game.getGameState().getAllPermanentCards(),
                                game,
                                com.gempukku.swccgo.filters.Filters.and(
                                    com.gempukku.swccgo.filters.Filters.character,
                                    com.gempukku.swccgo.filters.Filters.aboard(card)));
                        if (aboard != null) actualOnboard = aboard.size();
                    }
                } catch (Exception e) { /* ignore — fall through to no-passengers */ }
                hasPassengers = actualOnboard > 0;
                logger.info("[MoveEvaluator] V67f1: {} actual passengers aboard = {} (capital/transport)",
                    cardName, actualOnboard);
            }
        }

        // Fallback to name-based detection for starfighters
        if (!isStarfighter && !isStarship) {
            isStarfighter = actionLower.contains("x-wing") ||
                actionLower.contains("y-wing") ||
                actionLower.contains("a-wing") ||
                actionLower.contains("b-wing") ||
                actionLower.contains("tie") ||
                actionLower.contains("starfighter");
            if (isStarfighter) isStarship = true;

            // Name-based detection for capital/transport ships
            if (!isStarship) {
                isStarship = actionLower.contains("karrde") ||
                    actionLower.contains("falcon") ||
                    actionLower.contains("executor") ||
                    actionLower.contains("dreadnaught") ||
                    actionLower.contains("frigate") ||
                    actionLower.contains("cruiser") ||
                    actionLower.contains("corvette") ||
                    actionLower.contains("destroyer");
            }
        }

        // V49: NEVER land a starship at a site without characters to protect it.
        // A starship at a site has power 0 — anyone can attack for catastrophic overflow damage.
        // Only allow landing if the ship has passengers who can disembark and provide power.
        if (isStarship && !hasPassengers) {
            // V49 UPDATED 2026-07-06 T4.1: -9999 addReasoning converted to the ladder
            // hard-veto class (-100000 at the finalizer). Same gates, band-proof magnitude.
            ladderVetoHard = true;
            ladderVetoHardReason = String.format(
                "V49 BLOCKED: Landing %s at a site with NO passengers = power 0 = instant death from overflow! NEVER land unprotected!",
                cardName);
            logger.warn("[MoveEvaluator] V49 LADDER VETO: {} landing at site with no passengers — power 0 death trap!", cardName);
        } else if (isStarfighter) {
            action.addReasoning("AVOID: Landing starfighter (" + cardName + ") wastes combat power!", -100.0f);
            logger.info("[MoveEvaluator] BLOCKED: Landing starfighter {}", cardName);
        } else if (isStarship && hasPassengers) {
            action.addReasoning(String.format(
                "V49: Landing %s with %s passengers aboard — can disembark to protect", cardName, ""), 10.0f);
            logger.info("[MoveEvaluator] V49: {} landing with passengers — allowed", cardName);
        } else {
            action.addReasoning("Land (ground deployment)", 10.0f);
        }
    }

    /**
     * Get opponent icons at a location.
     */
    private int getOpponentIcons(SwccgCardBlueprint bp, Side mySide) {
        if (bp == null) return 0;
        if (mySide == Side.LIGHT) {
            return bp.getIconCount(Icon.DARK_FORCE);
        } else {
            return bp.getIconCount(Icon.LIGHT_FORCE);
        }
    }

    /**
     * Get our icons at a location.
     */
    private int getMyIcons(SwccgCardBlueprint bp, Side mySide) {
        if (bp == null) return 0;
        if (mySide == Side.LIGHT) {
            return bp.getIconCount(Icon.LIGHT_FORCE);
        } else {
            return bp.getIconCount(Icon.DARK_FORCE);
        }
    }

    // Helper classes for analysis results
    private static class AttackAnalysis {
        boolean viable;
        String reason;
        float score;
        boolean hasForcedrainPotential;  // True if target has opponent icons
        PhysicalCard targetLocation;     // T4.1 (2026-07-06): best target, for the R2 adjacency gate

        AttackAnalysis(boolean viable, String reason, float score, boolean hasForcedrainPotential,
                       PhysicalCard targetLocation) {
            this.viable = viable;
            this.reason = reason;
            this.score = score;
            this.hasForcedrainPotential = hasForcedrainPotential;
            this.targetLocation = targetLocation;
        }
    }

    private static class SpreadAnalysis {
        boolean viable;
        String reason;
        float score;

        SpreadAnalysis(boolean viable, String reason, float score) {
            this.viable = viable;
            this.reason = reason;
            this.score = score;
        }
    }
}
