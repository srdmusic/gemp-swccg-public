package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.MoveAbilityPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveBlockedResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveBuddyProtectionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDrainRoutingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveForceEconomyPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveHuntGroupPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveHuntTargetPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveLandingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveLandoStayPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveLadderPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveConsolidationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveOpportunityPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MovePostFlipConsolidationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveSpyFollowPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveThreatPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveTransitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveUnarmedVaderPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveVergePolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveWeaponHunterPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveWinnabilityPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.strategy.MovePredicates;
import com.gempukku.swccgo.ai.models.chosenone.RandoConfig;
import com.gempukku.swccgo.common.CardCategory;
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
    private static final int ATTACK_MIN_POWER = 6;

    // Score deltas (from Python)
    private static final float GOOD_DELTA = 10.0f;

    // T4.1 ladder thresholds, score bands, and veto order live in MoveLadderPolicy.
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
        ladderRank = MoveLadderPolicy.claimR4(ladderRank);
        ladderMandatoryTransit = true;
        logger.info("LADDER: R4 TRANSIT claim by {}", tag);
    }

    /** R3 claim — SURVIVAL (retreat/escape). Not subject to the L2 strength gate. */
    private void ladderClaimR3(String tag) {
        ladderRank = MoveLadderPolicy.claimR3(ladderRank);
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
        MoveLadderPolicy.RankTwoClaim claim = MoveLadderPolicy.claimR2(
            ladderRank, ladderBattleSeekingClaim, ownFine, drainDelta, battleSeeking);
        ladderRank = claim.rank();
        ladderBattleSeekingClaim = claim.battleSeekingClaim();
        if (claim.accepted()) {
            logger.info("LADDER: R2 DOCTRINE claim by {} (fine {}, drainDelta {}, battleSeeking={})",
                tag, (int) ownFine, (int) drainDelta, battleSeeking);
            return true;
        }
        logger.info("LADDER: R2 claim by {} REJECTED — weak claim (fine {} < +{}, drainDelta {} < {}) (ruling L2)",
            tag, (int) ownFine, (int) claim.requiredFine(), (int) drainDelta,
            (int) claim.requiredDrainDelta());
        return false;
    }

    /**
     * Ruling L4: first-use band-integrity assertion. Recomputes the R1 ceiling vs the
     * R2 floor from the live constants and logger.error's on inversion (no crash).
     */
    private void ladderAssertBandsOnce() {
        if (ladderBandsChecked) return;
        ladderBandsChecked = true;
        MoveLadderPolicy.BandIntegrity bands = MoveLadderPolicy.bandIntegrity();
        if (bands.inverted()) {
            logger.error("LADDER BAND INVERSION: R2 floor {} <= R1 ceiling {} — rank bands no longer separate "
                + "(RANK_R2={}, FINE_CLAMP={}, ATE_CROSS_NEG={}, R1_FINE_CEILING={}, ATE_CROSS_POS={}). "
                + "Rebalance before trusting MOVE decisions.",
                bands.r2Floor(), bands.r1Ceiling(), bands.rankR2Score(), bands.fineClamp(),
                bands.actionTextCrossNegative(), bands.r1FineCeiling(), bands.actionTextCrossPositive());
        } else {
            logger.info("LADDER BANDS OK: R2 floor {} > R1 ceiling {} (margin {})",
                bands.r2Floor(), bands.r1Ceiling(), bands.margin());
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

        MoveLadderPolicy.Finalization finalization = MoveLadderPolicy.finalizeAction(
            new MoveLadderPolicy.State(
                ladderRank,
                ladderVetoHard,
                ladderVetoHardReason,
                ladderCanWinVeto,
                ladderCanWinVetoReason,
                ladderBattleSeekingClaim,
                ladderMandatoryTransit,
                ladderWrongDirVeto,
                ladderWrongDirVetoReason,
                ladderRankMoveRan),
            action.getScore());

        for (MoveLadderPolicy.Step step : finalization.steps()) {
            if (step.contributesReasoning()) {
                action.addReasoning(step.reasoning(), step.delta());
            }
            switch (step.kind()) {
                case HARD_VETO -> logger.warn(
                    "LADDER VETO -100000 (hard): {}", step.detail());
                case WRONG_DIRECTION_SUPPRESSED -> logger.warn(
                    "V38.3 WRONG DIRECTION suppressed (R4 mandatory transit): {}", step.detail());
                case WRONG_DIRECTION_VETO -> logger.warn(
                    "LADDER VETO -100000 (V38.3): {}", step.detail());
                case CAN_WIN_VETO -> logger.warn(
                    "V137 UNWINNABLE (battle-seeking R2) — LADDER VETO -100000: {}", step.detail());
                case CAN_WIN_RETAINED -> logger.info(
                    "V137 canWinAt veto NOT applied (L3 matrix: rank=R{}, battleSeeking={}) — R1 weights already applied inline",
                    ladderRank, ladderBattleSeekingClaim);
                case POSITIVE_CLAMP -> logger.warn(
                    "LADDER CLAMP: fines {} clamped to +{} on '{}'",
                    (int) step.observedFines(), (int) Math.abs(step.observedFines() + step.delta()),
                    action.getDisplayText());
                case NEGATIVE_CLAMP -> logger.warn(
                    "LADDER CLAMP: fines {} clamped to -{} on '{}'",
                    (int) step.observedFines(), (int) Math.abs(step.observedFines() + step.delta()),
                    action.getDisplayText());
                case DEMOTE -> logger.warn(
                    "LADDER DEMOTE: negative clamp hit — R{} demoted to R{} (ruling L1)",
                    step.rankBefore(), step.rankAfter());
                case RANK_BASE, DEFAULT_PENALTY -> {
                    // Reasoning already applied above; no legacy log for these steps.
                }
            }
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
            boolean v160Blocked = MoveBlockedResponsePolicy.matches(
                v160MoveBlocked, actionId, actionText);
            if (v160Blocked) {
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
                boolean v169PowerFactsAvailable = false;
                float v169Our = 0.0f;
                float v169Their = 0.0f;
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
                            v169Our = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(context.getGameState(), v169At, v169Pid, false, false);
                            v169Their = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(context.getGameState(), v169At, v169Opp, false, false);
                            v169PowerFactsAvailable = true;
                        }
                    }
                } catch (Exception ignore) { }
                MoveBlockedResponsePolicy.Evaluation blockedMovePolicy =
                    MoveBlockedResponsePolicy.classify(
                        v160Blocked, v169PowerFactsAvailable,
                        v169Our, v169Their);
                if (blockedMovePolicy.outcome()
                        == MoveBlockedResponsePolicy.Outcome.ENDANGERED_FALLTHROUGH) {
                    // V169 UPDATED 2026-07-06 (audit cross-brain-1): no penalty here, no 'continue'.
                    logger.warn("V169 MoveEvaluator: endangered mover '{}' blocked-but-excused; soft penalty owned by ActionTextEvaluator (-250), falling through to retreat scoring", actionText);
                } else {
                    // V169 UPDATED 2026-07-06: hard block unchanged, wrapped in else so the
                    // endangered-mover path above falls through instead of hitting it.
                    // V160 UPDATED 2026-07-06 T4.1: cancel-loop veto raised from -9999 to
                    // ladder class -100000, above all score bands including R4 transit.
                    EvaluatedAction blockedMove = new EvaluatedAction(actionId, ActionType.MOVE, 0.0f, actionText);
                    blockedMove.addReasoning(
                        blockedMovePolicy.reason(),
                        blockedMovePolicy.delta());
                    logger.warn("MoveEvaluator: actionId='{}' is in blockedResponses → -100000 (V160 cancel-loop LADDER VETO)", actionId);
                    actions.add(blockedMove);
                    continue;
                }
            }

            // === SPECIAL CASES: Passenger/Pilot capacity slots ===
            MoveTransitPolicy.CapacitySlot capacitySlot =
                MoveTransitPolicy.capacitySlot(actionLower);
            if (capacitySlot.branch()
                    == MoveTransitPolicy.CapacitySlotBranch.PASSENGER_SKIP) {
                logger.info("[MoveEvaluator] SKIP passenger slot move - NEVER good");
                continue;  // Let ActionTextEvaluator's V87 -3000 apply
            }

            if (capacitySlot.branch()
                    == MoveTransitPolicy.CapacitySlotBranch.PILOT_PREFER) {
                EvaluatedAction pilotAction = new EvaluatedAction(
                    actionId, ActionType.MOVE,
                    capacitySlot.baseScore(), actionText
                );
                pilotAction.addReasoning(
                    capacitySlot.contribution().reason(),
                    capacitySlot.contribution().delta());
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
                    boolean v79Flipped = false;
                    if (v79Verge && v79AtScarif) {
                        try {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v79Analyzer =
                                context.getObjectiveAnalyzer();
                            v79Flipped = v79Analyzer != null && v79Analyzer.isAnalyzed() && v79Analyzer.isFlipped();
                        } catch (Exception ex) {
                            logger.debug("V79b flip-state check error: {}", ex.getMessage());
                        }
                    }

                    String v79DisplayText = v79Verge && !v79AtScarif
                        ? action.getDisplayText() : null;
                    MoveVergePolicy.Evaluation v79Evaluation =
                        MoveVergePolicy.evaluate(
                            v79Verge, v79AtScarif, v79Flipped, v79DisplayText);
                    if (v79Evaluation.contribution().applies()) {
                        action.addReasoning(
                            v79Evaluation.contribution().reason(),
                            v79Evaluation.contribution().delta());
                    }

                    if (v79Evaluation.branch() == MoveVergePolicy.Branch.ORBIT_SCARIF) {
                        logger.warn("V79 DEATH STAR ORBIT SCARIF: '{}' → +1500", v79Evaluation.actionLower());
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.PARSEC_SEVEN) {
                        logger.warn("V79 DEATH STAR → parsec 7 → +1200");
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.ONE_HOP_FROM_SCARIF) {
                        logger.warn("V79 DEATH STAR → parsec {} → +1000", v79Evaluation.destinationParsec());
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.TOWARD_SCARIF) {
                        logger.warn("V79 DEATH STAR → parsec {} → +700", v79Evaluation.destinationParsec());
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.WRONG_DIRECTION) {
                        logger.warn("V79 DEATH STAR WRONG WAY: parsec {} → -300", v79Evaluation.destinationParsec());
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.DEFAULT_MOVE) {
                        logger.warn("V79 DEATH STAR MOVE (no parsec parsed): '{}' → +500", v79Evaluation.actionLower());
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.POST_FLIP_HOLD) {
                        ladderVetoHard = v79Evaluation.hardVeto();
                        ladderVetoHardReason = v79Evaluation.hardVetoReason();
                        logger.warn("V79b FLIP-BACK GUARD: post-flip Death Star orbiting Scarif — hyperspeed move VETOED ('{}')", actionText);
                    } else if (v79Evaluation.branch() == MoveVergePolicy.Branch.PRE_FLIP_HOLD) {
                        logger.info("V79 DEATH STAR: orbiting Scarif pre-flip — no move bonus, holding for flip");
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
            MoveTransitPolicy.PilotLock pilotLock =
                MoveTransitPolicy.pilotLock(cardToMove);
            if (pilotLock.contribution().applies()) {
                action.addReasoning(pilotLock.contribution().reason(),
                    pilotLock.contribution().delta());
                logger.warn("V25 PILOT LOCK: {} is piloting {} — blocking move (-500)",
                    pilotLock.pilotName(), pilotLock.shipName());
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
                && MoveLandoStayPolicy.titleMarksLando(cardToMove.getTitle())) {
                PhysicalCard currentLoc = cardToMove.getAtLocation();
                if (currentLoc != null && currentLoc.getTitle() != null) {
                    // V47 UPDATED 2026-07-06: every Cloud City site title starts with
                    // "Cloud City: " (see Title.java), so the 'cloud city' fragment alone covers
                    // the whole CC site set incl. East Platform (Docking Bay). The generic
                    // 'platform' fragment matched non-CC sites and is commented out; the other
                    // CC-specific fragments are kept (redundant but harmless).
                    if (MoveLandoStayPolicy.isCloudCitySite(currentLoc.getTitle())) {
                        // V47 UPDATED 2026-07-06 gate (a): objective. Only lock when OUR analyzed
                        // objective is a Bespin/Cloud City occupation objective (V22.5 detector)
                        // and this site still serves it: pre-flip = flip-condition site (the
                        // occupation Lando is establishing), post-flip = flip-back protection
                        // site (the occupation Lando is defending). No CC objective, or a CC
                        // site the objective no longer cares about, = no lock.
                        boolean v47ObjectiveWantsLandoHere = false;
                        try {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v47Analyzer =
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
                        MoveLandoStayPolicy.Evaluation v47Decision =
                            MoveLandoStayPolicy.evaluate(
                                currentLoc.getTitle(),
                                v47ObjectiveWantsLandoHere,
                                v47Survivable);
                        if (v47Decision.hardVeto()) {
                            // V47 UPDATED 2026-07-06 T4.1: -9999 addReasoning converted to the ladder
                            // hard-veto class (-100000 at the finalizer). Today's gates (a)+(b) kept
                            // unchanged — semantics identical, magnitude now band-proof.
                            ladderVetoHard = true;
                            ladderVetoHardReason = v47Decision.reason();
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

                    MoveForceEconomyPolicy.Evaluation reserveEvaluation =
                        MoveForceEconomyPolicy.reserve(actionId, forcePile,
                            dtfActive, grabberNeedsForce, hasCriticalInterrupt);
                    PolicyContributionLedger reserveLedger = new PolicyContributionLedger(
                        "move-force-reserve-" + actionId);
                    reserveLedger.register(reserveEvaluation.result());
                    PolicyOperationAdapter.apply(action, reserveLedger);
                    if (reserveEvaluation.mode()
                            == MoveForceEconomyPolicy.Mode.HARD_RESERVE) {
                        float penalty = reserveEvaluation.result().operations().get(0).delta();
                        logger.warn("V29 MOVE RESERVE: {} Force left, need {} (DTF={}, grabber={}, interrupt={}) — penalty {}",
                            forcePile, reserveEvaluation.reserveNeeded(), dtfActive,
                            grabberNeedsForce, hasCriticalInterrupt, (int)penalty);
                    } else if (reserveEvaluation.mode()
                            == MoveForceEconomyPolicy.Mode.LOW_RESERVE) {
                        logger.info("V29 MOVE RESERVE: {} Force, need {} — mild penalty",
                            forcePile, reserveEvaluation.reserveNeeded());
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
                        if (MoveBuddyProtectionPolicy.hasBuddyPair(
                                ourCharsHere.size(),
                                ourCharsHere.contains(cardToMove))) {
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
                                if (MoveBuddyProtectionPolicy.needsPowerAnalysis(
                                        allyPower,
                                        RandoConfig.MIN_SOLO_DEPLOY_POWER,
                                        theirPowerHere)) {
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

                                    MoveBuddyProtectionPolicy.Evaluation buddyDecision =
                                        MoveBuddyProtectionPolicy.evaluate(
                                            currentLocation.getTitle(),
                                            remainingAlly.getTitle(),
                                            allyPower,
                                            RandoConfig.MIN_SOLO_DEPLOY_POWER,
                                            ourPowerHere,
                                            theirPowerHere);

                                    if (buddyDecision.branch()
                                            == MoveBuddyProtectionPolicy.Branch.DOOMED_ESCAPE) {
                                        // Location already lost — don't protect ally, ESCAPE the valuable one
                                        action.addReasoning(
                                            buddyDecision.reason(),
                                            buddyDecision.delta());
                                        // V59 UPDATED 2026-07-06 T4.1: DOOMED escape claims R3 SURVIVAL
                                        // (fine +200 kept; base applied at the finalizer).
                                        if (buddyDecision.claimSurvival()) {
                                            ladderClaimR3("V59 DOOMED ESCAPE");
                                        }
                                        logger.warn("V59 DOOMED: {} at {} is lost ({} vs {}) — buddy protect DISABLED, flee!",
                                            cardToMove.getTitle(), currentLocation.getTitle(),
                                            (int)ourPowerHere, (int)theirPowerHere);
                                    } else if (buddyDecision.branch()
                                            == MoveBuddyProtectionPolicy.Branch.BUDDY_PROTECT) {
                                        action.addReasoning(
                                            buddyDecision.reason(),
                                            buddyDecision.delta());
                                        logger.warn("V27 BUDDY PROTECT: {} moving from {} would leave {} (power {}) alone!{}",
                                            cardToMove.getTitle(), currentLocation.getTitle(),
                                            remainingAlly.getTitle(), allyPower,
                                            buddyDecision.enemyThreat()
                                                ? " ENEMY POWER=" + (int)theirPowerHere : "");
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

                            MoveAbilityPolicy.Analysis v32Analysis =
                                MoveAbilityPolicy.analyze(
                                    friendlyCharsHere,
                                    totalAbilityHere,
                                    moverAbility);
                            float abilityAfterMove = v32Analysis.abilityAfterMove();

                            // Only applies if there will be remaining characters after move
                            if (v32Analysis.branch()
                                    == MoveAbilityPolicy.Branch.DESTINY_DANGER) {
                                // Moving away drops ability below 4 — heavy penalty
                                // Check if opponent has presence (makes it even worse)
                                String v32Opponent = game.getOpponent(playerId);
                                float theirPower = 0;
                                try {
                                    theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, v32Opponent, false, false);
                                } catch (Exception e) { /* ignore */ }

                                MoveAbilityPolicy.Evaluation v32Danger =
                                    MoveAbilityPolicy.destinyDanger(
                                        cardToMove.getTitle(),
                                        currentLocation.getTitle(),
                                        totalAbilityHere,
                                        abilityAfterMove,
                                        theirPower);
                                action.addReasoning(
                                    v32Danger.reason(),
                                    v32Danger.delta());
                                logger.warn("V32 ABILITY MOVE BLOCK: {} moving from {} would leave ability {} < 4!{}",
                                    cardToMove.getTitle(), currentLocation.getTitle(),
                                    abilityAfterMove, theirPower > 0 ? " ENEMY=" + (int)theirPower : "");
                            } else if (v32Analysis.branch()
                                    == MoveAbilityPolicy.Branch.SOLO_ESCAPE) {
                                // This is the ONLY character and has < 4 ability — moving AWAY is actually GOOD
                                // because we should consolidate with allies who have more ability
                                MoveAbilityPolicy.Evaluation v32Solo =
                                    MoveAbilityPolicy.soloEscape(
                                        cardToMove.getTitle(),
                                        totalAbilityHere);
                                action.addReasoning(
                                    v32Solo.reason(),
                                    v32Solo.delta());

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
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v156Oa =
                                            context.getObjectiveAnalyzer();
                                        v156AtReadyFlipSite = v156Oa != null && v156Oa.isAnalyzed()
                                            && currentLocation.getTitle() != null
                                            && v156Oa.isObjectiveRelevantLocation(currentLocation.getTitle())
                                            && !com.gempukku.swccgo.ai.models.common.strategy.CharacterDeploySiteEvaluator
                                                .isV156FlipNotReady(gameState, playerId);
                                    } catch (Exception ignore) { /* false */ }
                                    if (MoveAbilityPolicy.isUncontested(v156OppPowerHere)
                                            && MoveAbilityPolicy.canJoinGroup(
                                                cardToMove.isUndercover(),
                                                v156AtReadyFlipSite)) {
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
                                            MoveAbilityPolicy.Evaluation v156Join =
                                                MoveAbilityPolicy.joinGroup(
                                                    cardToMove.getTitle(),
                                                    totalAbilityHere,
                                                    currentLocation.getTitle(),
                                                    v156JoinLoc.getTitle(),
                                                    v156DestTotal);
                                            action.addReasoning(
                                                v156Join.reason(),
                                                v156Join.delta());
                                            if (v156Join.claimDoctrine()) {
                                                ladderClaimR2(
                                                    "V156 JOIN-GROUP",
                                                    v156Join.delta(),
                                                    0.0f,
                                                    false);
                                            }
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
                            MoveAbilityPolicy.Evaluation v33Decision =
                                MoveAbilityPolicy.abilityBuddy(
                                    cardToMove.getTitle(),
                                    currentLocation.getTitle(),
                                    v33FriendlyChars,
                                    v33TotalAbility,
                                    v33AbilityAfterMove,
                                    RandoConfig.ABILITY_BUDDY_THRESHOLD,
                                    v33Gap);
                            if (v33Decision.branch()
                                    == MoveAbilityPolicy.Branch.ABILITY_BUDDY_BREAK) {
                                action.addReasoning(
                                    v33Decision.reason(),
                                    v33Decision.delta());
                                logger.warn("V33 BUDDY BREAK: {} from {} would drop ability {} → {} (< {})",
                                    cardToMove.getTitle(), currentLocation.getTitle(),
                                    v33TotalAbility, v33AbilityAfterMove, RandoConfig.ABILITY_BUDDY_THRESHOLD);
                            } else if (v33Decision.branch()
                                    == MoveAbilityPolicy.Branch.ABILITY_BUDDY_DOOMED_SKIP) {
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
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer moveConsolidateAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (moveConsolidateAnalyzer != null && moveConsolidateAnalyzer.isAnalyzed()
                            && moveConsolidateAnalyzer.isFlipped()) {
                            try {
                                java.util.Set<String> objFrags = moveConsolidateAnalyzer.getFlipConditionLocationFragments();
                                String curLocTitle = currentLocation.getTitle();
                                boolean atObjLoc =
                                    MovePostFlipConsolidationPolicy.isObjectiveLocation(
                                        curLocTitle,
                                        objFrags);

                                // Count occupied objective locations and find the weakest
                                java.util.Map<String, Float> objPowerMap = new java.util.LinkedHashMap<>();
                                for (PhysicalCard loc : gameState.getTopLocations()) {
                                    if (loc == null || loc.getTitle() == null) continue;
                                    if (!MovePostFlipConsolidationPolicy.isObjectiveLocation(
                                            loc.getTitle(), objFrags)) continue;
                                    float pwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, playerId, false, false);
                                    if (pwr > 0) objPowerMap.put(loc.getTitle(), pwr);
                                }

                                MovePostFlipConsolidationPolicy.Evaluation v31Decision =
                                    MovePostFlipConsolidationPolicy.evaluate(
                                        curLocTitle,
                                        atObjLoc,
                                        objPowerMap);
                                if (v31Decision.applies()) {
                                    action.addReasoning(
                                        v31Decision.reason(),
                                        v31Decision.delta());
                                    logger.warn("V31 POST-FLIP CONSOLIDATE: {} should leave {} (weakest, power={}) to reinforce",
                                        cardToMove.getTitle(),
                                        v31Decision.weakestLocationTitle(),
                                        (int)v31Decision.weakestPower());
                                    // V31 UPDATED 2026-07-06 T4.1: consolidation claims R2 DOCTRINE
                                    // (non-battle; fine +200 passes the L2 strength gate).
                                    if (v31Decision.claimDoctrine()) {
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
                        PhysicalCard destLoc37 = MoveDestinationPolicy.resolveDestination(
                            gameState, currentLocation, moveText37);

                        if (destLoc37 != null && destLoc37.getBlueprint() != null) {
                            boolean destIsBattleground = false;
                            try {
                                destIsBattleground = game.getModifiersQuerying().isBattleground(gameState, destLoc37, null);
                            } catch (Exception e) { /* ignore */ }

                            boolean currentIsBattleground = false;
                            try {
                                currentIsBattleground = game.getModifiersQuerying().isBattleground(gameState, currentLocation, null);
                            } catch (Exception e) { /* ignore */ }

                            MoveDestinationPolicy.Contribution v37Decision =
                                MoveDestinationPolicy.battlegroundRetreat(
                                    currentLocation.getTitle(),
                                    destLoc37.getTitle(),
                                    currentIsBattleground,
                                    destIsBattleground);
                            if (v37Decision.applies()) {
                                action.addReasoning(
                                    v37Decision.reason(),
                                    v37Decision.delta());
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
                                    boolean v135IsSelfMoveToFriend =
                                        MoveDestinationPolicy.isSelfMoveToFriend(v135Gt);
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
                                        MoveDestinationPolicy.CompanionVeto v135Decision =
                                            MoveDestinationPolicy.companionVeto(
                                                cardToMove.getTitle(),
                                                destLoc37.getTitle(),
                                                v135IsSelfMoveToFriend,
                                                v135FriendlyAtDest);
                                        if (v135Decision.hardVeto()) {
                                            // V135 UPDATED 2026-07-06 T4.1: -2000 (outbiddable by +2000+ stacks)
                                            // strengthened to the ladder hard-veto class — a self-move-to-friend
                                            // that lands ALONE is absolutely blocked.
                                            ladderVetoHard = true;
                                            ladderVetoHardReason = v135Decision.reason();
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
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer huntMoveAnalyzer =
                            context.getObjectiveAnalyzer();
                        MoveHuntTargetPolicy.Evaluation huntTarget =
                            MoveHuntTargetPolicy.evaluate(
                                gameState, game, currentLocation, cardToMove,
                                playerId,
                                () -> huntMoveAnalyzer != null
                                    && huntMoveAnalyzer.isAnalyzed()
                                    && huntMoveAnalyzer.isHuntDownV(),
                                card -> com.gempukku.swccgo.filters.Filters.Dark_Jedi
                                    .accepts(gameState, game.getModifiersQuerying(), card),
                                MoveEvaluator::isJediOrPadawan,
                                (float) RandoConfig.SCORE_VADER_SEEK_JEDI);
                        if (huntTarget.contribution().applies()) {
                            action.addReasoning(
                                huntTarget.contribution().reason(),
                                huntTarget.contribution().delta());
                            String huntBranch = huntTarget.branch()
                                == MoveHuntTargetPolicy.Branch.JEDI
                                ? "JEDI" : "DOWN";
                            logger.warn("V35 HUNT {}: Armed Vader at {} — target {} (power {}, bonus +{})",
                                huntBranch, huntTarget.currentLocationName(),
                                huntTarget.targetLocation(),
                                (int) huntTarget.targetPower(),
                                (int) huntTarget.contribution().delta());
                            // V35/V29.12: battle-seeking R2 doctrine claim.
                            ladderClaimR2("V35 HUNT " + huntBranch,
                                huntTarget.contribution().delta(),
                                0.0f, true);
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
                            if (MoveWinnabilityPolicy.actionTargetsLocation(
                                    v137MoveText, loc.getTitle())) {
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
                                    MoveWinnabilityPolicy.Evaluation v137Decision =
                                        MoveWinnabilityPolicy.contested(
                                            cardToMove.getTitle(),
                                            v137Dest.getTitle(),
                                            v137OurPower,
                                            v137OurAbility,
                                            v137OppPower,
                                            v137CanWin);
                                    if (v137Decision.applies()) {
                                        ladderCanWinVeto = v137Decision.canWinVeto();
                                        ladderCanWinVetoReason = v137Decision.vetoReason();
                                        action.addReasoning(
                                            v137Decision.reason(),
                                            v137Decision.delta());
                                        logger.warn("V137 UNWINNABLE MOVE: {} → {} (group pwr {} abil {} vs opp {}) → {} (+canWinAt veto flag)",
                                            cardToMove.getTitle(), v137Dest.getTitle(),
                                            (int)v137OurPower, (int)v137OurAbility, (int)v137OppPower,
                                            (int)v137Decision.delta());
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
                                        MoveWinnabilityPolicy.Evaluation v137AntiSolo =
                                            MoveWinnabilityPolicy.uncontestedBattleground(
                                                cardToMove.getTitle(),
                                                v137Dest.getTitle(),
                                                v137DestBG,
                                                v137DestFriendlies,
                                                v137MovingChars);
                                        if (v137AntiSolo.applies()) {
                                            action.addReasoning(
                                                v137AntiSolo.reason(),
                                                v137AntiSolo.delta());
                                            logger.warn("V137 ANTI-SOLO BG: {} → {} solo at BG (projected={}) → -500",
                                                cardToMove.getTitle(), v137Dest.getTitle(),
                                                v137AntiSolo.projectedCharactersAtDestination());
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
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer huntMoveGroupAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (huntMoveGroupAnalyzer != null && huntMoveGroupAnalyzer.isAnalyzed()
                            && huntMoveGroupAnalyzer.isHuntDownV()
                            && cardToMove != null && cardToMove.getTitle() != null
                            && gameState != null && game != null) {
                            try {
                                MoveHuntGroupPolicy.Evaluation huntGroup =
                                    MoveHuntGroupPolicy.evaluate(
                                        gameState, game, currentLocation,
                                        cardToMove, playerId,
                                        action::getDisplayText,
                                        candidate -> com.gempukku.swccgo.filters.Filters.Dark_Jedi
                                            .accepts(gameState, game.getModifiersQuerying(), candidate));
                                if (huntGroup.contribution().applies()) {
                                    action.addReasoning(
                                        huntGroup.contribution().reason(),
                                        huntGroup.contribution().delta());
                                }
                                switch (huntGroup.branch()) {
                                    case HUNTER_TOWARD_ALLIES -> {
                                        logger.warn("V29.13 HUNT GROUP: Vader moving to allies at {} (+{})",
                                            huntGroup.anchorLocation().getTitle(),
                                            (int)huntGroup.contribution().delta());
                                        // V29.13 UPDATED 2026-07-06 T4.1: toward-group claims R2 DOCTRINE
                                        // (non-battle; +200/+250 passes the L2 gate). Scatter arms stay R1 weights.
                                        ladderClaimR2("V29.13 HUNT GROUP MOVE (Vader→allies)",
                                            huntGroup.contribution().delta(), 0.0f, false);
                                    }
                                    case HUNTER_AWAY_FROM_ALLIES ->
                                        logger.warn("V29.13 HUNT SCATTER: Vader moving away from allies at {} (-200)",
                                            huntGroup.anchorLocation().getTitle());
                                    case ALLY_AWAY_FROM_HUNTER ->
                                        logger.warn("V29.13 HUNT SCATTER: {} leaving Vader at {} (-250)",
                                            cardToMove.getTitle(), huntGroup.anchorLocation().getTitle());
                                    case ALLY_TOWARD_HUNTER -> {
                                        logger.warn("V29.13 HUNT GROUP: {} moving to Vader at {} (+{})",
                                            cardToMove.getTitle(), huntGroup.anchorLocation().getTitle(),
                                            (int)huntGroup.contribution().delta());
                                        // V29.13 UPDATED 2026-07-06 T4.1: toward-group claims R2 DOCTRINE
                                        // (non-battle; +250 passes the L2 gate). Scatter arms stay R1 weights.
                                        ladderClaimR2("V29.13 HUNT GROUP MOVE (→Vader)",
                                            huntGroup.contribution().delta(), 0.0f, false);
                                    }
                                    case ALLY_ELSEWHERE ->
                                        logger.info("V29.13 HUNT SCATTER: {} not moving toward Vader at {} (-100)",
                                            cardToMove.getTitle(), huntGroup.anchorLocation().getTitle());
                                    case NONE -> { }
                                }
                            } catch (Exception e) {
                                logger.debug("V29.13 HUNT GROUP MOVE: Error: {}", e.getMessage());
                            }
                        }
                    }

                    // V22.5: PRE-FLIP CONSOLIDATION — don't leave characters alone to die!
                    // Even before flipping, if a lone character is badly outgunned at a location,
                    // they should move to join allies instead of staying to get slaughtered.
                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer moveObjAnalyzer =
                        context.getObjectiveAnalyzer();
                    if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed() && !moveObjAnalyzer.isFlipped()) {
                        MoveObjectiveConsolidationPolicy.Evaluation preFlip =
                            MoveObjectiveConsolidationPolicy.preFlip(
                                gameState, game, currentLocation, playerId);
                        if (preFlip.contribution().applies()) {
                            action.addReasoning(
                                preFlip.contribution().reason(),
                                preFlip.contribution().delta());
                        }
                        if (preFlip.branch()
                                == MoveObjectiveConsolidationPolicy.Branch.PRE_FLIP_LONE_OUTGUNNED) {
                            logger.warn("V22.5 CONSOLIDATE PRE-FLIP: {} alone at {} ({}v{}) should join allies{}",
                                cardToMove.getTitle(),
                                preFlip.currentLocationName(),
                                (int) preFlip.ownPower(),
                                (int) preFlip.opponentPowerAtCurrentLocation(),
                                preFlip.bestAllyLocation() != null
                                    ? " at " + preFlip.bestAllyLocation() : "");
                        }
                        if (preFlip.contribution().claimDoctrineRank()) {
                            ladderClaimR2(
                                "V22.5 PRE-FLIP CONSOLIDATE",
                                preFlip.contribution().delta(),
                                0.0f, false);
                        }
                    }

                    // V22.2: POST-FLIP OBJECTIVE PROTECTION
                    // After objective flips, protect flip-back locations at all costs.
                    // Scale required power based on opponent's threat level.
                    if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed() && moveObjAnalyzer.isFlipped()) {
                        MoveObjectiveConsolidationPolicy.Evaluation postFlip =
                            MoveObjectiveConsolidationPolicy.postFlip(
                                gameState, game, currentLocation, playerId,
                                moveObjAnalyzer::isFlipBackProtectionLocation,
                                e -> logger.debug(
                                    "Could not sum opponent power: {}",
                                    e.getMessage()),
                                e -> logger.debug(
                                    "Could not analyze protection locations: {}",
                                    e.getMessage()));
                        if (postFlip.contribution().applies()) {
                            action.addReasoning(
                                postFlip.contribution().reason(),
                                postFlip.contribution().delta());
                        }
                        switch (postFlip.branch()) {
                            case POST_FLIP_STAY ->
                                logger.warn("V22.2 PROTECT: {} must stay at {} (our power={}, opponent total={})",
                                    cardToMove.getTitle(),
                                    postFlip.currentLocationName(),
                                    (int) postFlip.ownPower(),
                                    (int) postFlip.opponentTotalPower());
                            case POST_FLIP_LONE_REINFORCE -> {
                                logger.warn("V22.2 CONSOLIDATE: {} alone at {} - move to reinforce (worst deficit={})",
                                    cardToMove.getTitle(),
                                    postFlip.currentLocationName(),
                                    (int) postFlip.worstProtectionDeficit());
                            }
                            case POST_FLIP_SEVERE_REINFORCE ->
                                logger.warn("V22.2 CONSOLIDATE: {} at {} but {} needs help (deficit={})",
                                    cardToMove.getTitle(),
                                    postFlip.currentLocationName(),
                                    postFlip.weakestProtectionLocation(),
                                    (int) postFlip.worstProtectionDeficit());
                            default -> { }
                        }
                        if (postFlip.contribution().claimDoctrineRank()) {
                            ladderClaimR2(
                                "V22.2 POST-FLIP REINFORCE",
                                postFlip.contribution().delta(),
                                0.0f, false);
                        }
                    }
                } else {
                    MoveDestinationPolicy.Contribution missingSource =
                        MoveDestinationPolicy.missingSourceLocation();
                    action.addReasoning(
                        missingSource.reason(), missingSource.delta());
                }
            }

            // === MOVEMENT TYPE BONUSES ===
            // V25: Shuttle bonus only when defending — opponent has 2x our power at destination
            MoveTransitPolicy.MovementTypes movementTypes =
                MoveTransitPolicy.movementTypes(
                    actionLower, gameState, playerId);
            MoveTransitPolicy.DefensiveShuttle defensiveShuttle =
                movementTypes.defensiveShuttle();
            if (defensiveShuttle.contribution().applies()) {
                action.addReasoning(defensiveShuttle.contribution().reason(),
                    defensiveShuttle.contribution().delta());
                logger.info("[MoveEvaluator] V25 Defensive shuttle to {} (them={}, us={})",
                    defensiveShuttle.locationTitle(),
                    (int)defensiveShuttle.theirPower(),
                    (int)defensiveShuttle.ourPower());
            } else if (movementTypes.shuttleAction()) {
                // No bonus for non-defensive shuttles — let strategic analysis decide
                logger.debug("[MoveEvaluator] V25 Shuttle without defensive need — no bonus");
            }
            if (movementTypes.dockingBayTransit().applies()) {
                action.addReasoning(
                    movementTypes.dockingBayTransit().reason(),
                    movementTypes.dockingBayTransit().delta());
            }
            if (movementTypes.takeOff().applies()) {
                action.addReasoning(movementTypes.takeOff().reason(),
                    movementTypes.takeOff().delta());
            }

            // Land - penalize starfighters
            if (actionLower.contains("land")) {
                MoveLandingPolicy.Evaluation landing =
                    MoveLandingPolicy.evaluate(actionLower, cardToMove, game);
                if (landing.passengerScanRan()) {
                    logger.info("[MoveEvaluator] V67f1: {} actual passengers aboard = {} (capital/transport)",
                        landing.cardName(), landing.actualPassengers());
                }
                switch (landing.route()) {
                    case HARD_VETO:
                        ladderVetoHard = true;
                        ladderVetoHardReason = landing.reason();
                        logger.warn("[MoveEvaluator] V49 LADDER VETO: {} landing at site with no passengers — power 0 death trap!",
                            landing.cardName());
                        break;
                    case STARFIGHTER_PENALTY:
                        action.addReasoning(landing.reason(), landing.delta());
                        logger.info("[MoveEvaluator] BLOCKED: Landing starfighter {}", landing.cardName());
                        break;
                    case PASSENGER_SHIP_ALLOWED:
                        action.addReasoning(landing.reason(), landing.delta());
                        logger.info("[MoveEvaluator] V49: {} landing with passengers — allowed", landing.cardName());
                        break;
                    case GROUND_ALLOWED:
                        action.addReasoning(landing.reason(), landing.delta());
                        break;
                }
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
                            MoveForceEconomyPolicy.Evaluation maintenanceEvaluation =
                                MoveForceEconomyPolicy.maintenance(
                                    actionId, maintenanceCost, forcePile);
                            PolicyContributionLedger maintenanceLedger =
                                new PolicyContributionLedger(
                                    "move-maintenance-" + actionId);
                            maintenanceLedger.register(maintenanceEvaluation.result());
                            PolicyOperationAdapter.apply(action, maintenanceLedger);
                            if (maintenanceEvaluation.mode()
                                    == MoveForceEconomyPolicy.Mode.MAINTENANCE_CONSERVE) {
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
                    MoveSpyFollowPolicy.Evaluation spyFollow =
                        MoveSpyFollowPolicy.evaluate(
                            gameState, game, cardToMove,
                            spyPid, actionLower);
                    if (spyFollow.contribution().applies()) {
                        action.addReasoning(
                            spyFollow.contribution().reason(),
                            spyFollow.contribution().delta());
                    }
                    switch (spyFollow.branch()) {
                        case FOLLOW ->
                        logger.warn("V53 SPY FOLLOW: {} following opponent to new location — +500!", cardToMove.getTitle());
                        case STAY ->
                        logger.warn("V53 SPY STAY: {} trying to leave opponent — -300!", cardToMove.getTitle());
                        case REPOSITION ->
                        logger.warn("V53 SPY REPOSITION: {} moving to opponent location — +400!", cardToMove.getTitle());
                        default -> { }
                    }
                    if (spyFollow.contribution().claimDoctrineRank()) {
                        String spyClaim = spyFollow.branch()
                            == MoveSpyFollowPolicy.Branch.FOLLOW
                            ? "V53 SPY FOLLOW" : "V53 SPY REPOSITION";
                        ladderClaimR2(
                            spyClaim,
                            spyFollow.contribution().delta(),
                            0.0f, false);
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
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer hpMoveAnalyzer =
                    context.getObjectiveAnalyzer();
                if (hpMoveAnalyzer != null && hpMoveAnalyzer.isAnalyzed()) {
                    // V60 FIX: Corridor landspeed stays vetoed; positive transit remains in ActionTextEvaluator.
                    MoveTransitPolicy.HiddenPathTransit hiddenPath =
                        MoveTransitPolicy.hiddenPathTransit(
                            hpMoveAnalyzer.getObjectiveTitle(),
                            cardToMove, actionLower);
                    if (hiddenPath.contribution().applies()) {
                        action.addReasoning(
                            hiddenPath.contribution().reason(),
                            hiddenPath.contribution().delta());
                    }
                    if (hiddenPath.hardVeto()) {
                        ladderVetoHard = true;
                        ladderVetoHardReason = hiddenPath.hardVetoReason();
                    }
                    if (hiddenPath.claimMandatoryTransit()) {
                        ladderClaimR4Transit(hiddenPath.claimIdentity());
                    }
                    switch (hiddenPath.branch()) {
                        case SAFEHOUSE_TO_CORRIDOR ->
                            logger.warn("V53b HIDDEN PATH: {} MUST landspeed Safehouse → Corridor (R4 transit +800 fine)!", hiddenPath.characterName());
                        case CORRIDOR_LANDSPEED_BLOCK ->
                            logger.warn("V60 HIDDEN PATH: {} BLOCKED landspeed from Corridor (LADDER VETO) — must use 'Move Jedi Survivor here to a site'!", hiddenPath.characterName());
                        case MAPUZO_EXIT ->
                            logger.warn("V53b HIDDEN PATH: {} leaving Mapuzo via landspeed — R4 transit +800!", hiddenPath.characterName());
                        default -> { }
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
        MoveThreatPolicy.Evaluation threat = MoveThreatPolicy.evaluate(
                theirPower, powerDiff,
                RandoConfig.BATTLE_FAVORABLE_THRESHOLD,
                RandoConfig.BATTLE_DANGER_THRESHOLD);
        if (threat.applies()) {
            action.addReasoning(threat.reason(), threat.delta());

            switch (threat.level()) {
                case RETREAT:
                    logger.info("[MoveEvaluator] RETREAT recommended - outmatched by {}", -powerDiff);
                    // T4.1 (2026-07-06): the RETREAT tier claims R3 SURVIVAL; the early
                    // return is removed so later blocks (V29.13 drain, V34 contest…) still run.
                    if (threat.claimSurvivalRank()) {
                        ladderClaimR3("THREAT RETREAT");
                    }
                    break;

                case DANGEROUS:
                    // T4.1 (2026-07-06): early return removed — R1 fine (+20), block falls through.
                    break;

                case CRUSH:
                    // V37.1 UPDATED 2026-07-06 T4.1: the cross-rank -9999 + return silently buried
                    // every doctrine/transit rule below (move-3a boundary: Hidden Path stalled
                    // forever at a FAVORABLE Mining Village). Now an R1-internal weight (-1500,
                    // no return): still buries every same-band R1 move (V37.1-protect boundary:
                    // -1550 < Pass), but an R2+ claim outranks it by band.
                    logger.warn("V37.1 STAY AND CRUSH at {}: power +{} — -1500 (weight)",
                        location.getTitle(), (int)powerDiff);
                    break;

                case FAVORABLE:
                    // V37.1 UPDATED 2026-07-06 T4.1: same conversion as CRUSH (-9999+return → -1500 weight).
                    logger.warn("V37.1 STAY AND FIGHT at {}: power +{} — -1500 (weight)",
                        location.getTitle(), (int)powerDiff);
                    break;

                case RISKY:
                    // V37.1: Even fight — very strong discouragement to leave
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
                MoveDrainRoutingPolicy.UncontestedDeparture v85 =
                    MoveDrainRoutingPolicy.uncontestedDeparture(
                        gameState, game, location, playerId);
                if (v85.contribution().applies()) {
                    // V85 UPDATED 2026-07-06 T4.1: the -2000 + bare return killed every
                    // doctrine rule below it (move-1 boundary: the Cantina↔Mos Eisley
                    // shuttle was dead; move-2: Vader farmed drain instead of hunting).
                    // Now an R1-internal weight (-800, NO return): still blocks every
                    // same-band R1 wander (V85-protect boundary: Rey stays at -720 < Pass),
                    // while an R2+ doctrine claim (V73 shuttle, V35 hunt…) outranks it by band.
                    action.addReasoning(
                        v85.contribution().reason(),
                        v85.contribution().delta());
                    logger.warn("V85 UNCONTESTED: {} drain {} → best adj {} drain {} → -800 (weight)",
                        location.getTitle(), (int)v85.currentDrain(),
                        v85.bestAdjacent().getTitle(), (int)v85.bestAdjacentDrain());
                }
            } catch (Exception e) {
                logger.debug("V85 UNCONTESTED CHECK: Error: {}", e.getMessage());
            }
        }

        // === FLEE LOGIC ===
        MoveThreatPolicy.FleeEvaluation flee = MoveThreatPolicy.flee(
            myPower, theirPower, POWER_DIFF_FOR_FLEE, GOOD_DELTA);
        if (flee.applies()) {
            action.addReasoning(flee.reason(), flee.delta());
            // T4.1 (2026-07-06): early return removed — R1 fine (≤ +50), block falls through.
        }

        // === OFFENSIVE ATTACK OPPORTUNITY ===
        // If we're at an uncontested location with significant power, look for attack targets
        // NOTE: We can't verify reachability here, so be conservative - only recommend if:
        // 1. We have overwhelming force AND
        // 2. There are high-value targets (opponent icons for force drain)
        if (!theirHasCards && myPower >= ATTACK_MIN_POWER && myCardCount >= 2) {
            MoveOpportunityPolicy.AttackAnalysis attack =
                MoveOpportunityPolicy.attack(
                    gameState, playerId, mySide, location, myPower);
            MoveOpportunityPolicy.Contribution attackContribution =
                MoveOpportunityPolicy.attackContribution(attack);
            // Only recommend attack if there's force drain potential (icons > 0)
            // and we have a significant power advantage
            if (attackContribution.applies()
                    && attack.hasForcedrainPotential) {
                action.addReasoning(
                    attackContribution.reason(), attackContribution.delta());
                logger.info("[MoveEvaluator] ⚔️ ATTACK opportunity: {}", attack.reason);
                // T4.1 (2026-07-06): early return removed. ATTACK claims R2 DOCTRINE
                // (battle-seeking) — NEWLY gated on isAdjacentSites reachability to the
                // best target (same engine call V85 uses) so the destination-blind Rey
                // class can't claim a band for an unreachable fight (V85-protect boundary).
                boolean attackTargetAdjacent = false;
                try {
                    attackTargetAdjacent = attack.targetLocation != null
                        && game.getModifiersQuerying().isAdjacentSites(
                            gameState, location, attack.targetLocation);
                } catch (Exception ignore) { /* false */ }
                if (attackTargetAdjacent) {
                    ladderClaimR2("ATTACK", attack.score, 0.0f, true);
                } else {
                    logger.info("LADDER: ATTACK no R2 claim (target not adjacent) — fine kept as R1 weight");
                }
            } else if (attackContribution.applies()) {
                // Attack possible but no force drain - much smaller bonus
                // Don't waste moves just to attack weak positions
                action.addReasoning(
                    attackContribution.reason(), attackContribution.delta());
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
                boolean v29TitleMarksVader = preCharTitle.contains("vader");
                boolean vaderHasWeapon = false;
                boolean saberInHand = false;
                if (v29TitleMarksVader) {
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
                    }
                }
                MoveUnarmedVaderPolicy.Evaluation v29Readiness =
                    MoveUnarmedVaderPolicy.evaluate(
                        v29TitleMarksVader, vaderHasWeapon, saberInHand);
                if (v29Readiness.applies()) {
                    action.addReasoning(
                        v29Readiness.reason(), v29Readiness.delta());
                    if (v29Readiness.branch()
                            == MoveUnarmedVaderPolicy.Branch.EQUIP_FIRST) {
                        logger.warn("V29.9 UNARMED VADER: Vader has no weapon but lightsaber in hand — blocking attack move (-250)");
                        // T4.1 (2026-07-06): early return removed — R1 weight, block falls through.
                    } else {
                        logger.warn("V29.9 UNARMED VADER: Vader has no weapon and none in hand — penalizing attack move (-100)");
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.9: Error checking Vader weapon status: {}", e.getMessage());
            }
            try {
                List<String> v297WeaponTitles = new ArrayList<>();
                List<PhysicalCard> attached = gameState.getAttachedCards(cardToMove);
                if (attached != null) {
                    for (PhysicalCard att : attached) {
                        if (att == null || att.getBlueprint() == null) continue;
                        if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                            v297WeaponTitles.add(att.getTitle());
                        }
                    }
                }

                MoveWeaponHunterPolicy.WeaponFacts v297WeaponFacts =
                    MoveWeaponHunterPolicy.weaponFacts(v297WeaponTitles);
                if (v297WeaponFacts.hasWeapon()) {
                    String charTitle = cardToMove.getTitle() != null ? cardToMove.getTitle() : "character";
                    boolean isVader =
                        MoveWeaponHunterPolicy.titleMarksVader(charTitle);

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

                    MoveWeaponHunterPolicy.HunterProfile v297Profile =
                        MoveWeaponHunterPolicy.profile(
                            v297WeaponFacts, charTitle, myPower, hasIHYN);

                    // Look for opponent locations to attack (opponentId already declared above)
                    List<PhysicalCard> v297TargetLocations = new ArrayList<>();
                    List<MoveWeaponHunterPolicy.TargetFact> v297Targets =
                        new ArrayList<>();

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

                        if (v297Profile.canBeat(
                                theirCountThere, theirPowerThere)) {
                            int oppIcons = 0;
                            SwccgCardBlueprint locBp = adjLocation.getBlueprint();
                            if (locBp != null) {
                                oppIcons = (mySide == Side.DARK)
                                    ? locBp.getIconCount(Icon.LIGHT_FORCE)
                                    : locBp.getIconCount(Icon.DARK_FORCE);
                            }
                            int ordinal = v297TargetLocations.size();
                            v297TargetLocations.add(adjLocation);
                            v297Targets.add(
                                new MoveWeaponHunterPolicy.TargetFact(
                                    ordinal, adjLocation.getTitle(),
                                    theirPowerThere, oppIcons, lukeHere));
                        }
                    }

                    MoveWeaponHunterPolicy.Evaluation v297Evaluation =
                        MoveWeaponHunterPolicy.select(
                            v297Profile, v297Targets);
                    if (v297Evaluation.applies()) {
                        action.addReasoning(
                            v297Evaluation.reason(), v297Evaluation.delta());
                        logger.info("[MoveEvaluator] ⚔️ {} — score {}",
                            v297Evaluation.reason(), v297Evaluation.delta());
                        // V29.7 UPDATED 2026-07-06 T4.1: early return removed. Claims R2 DOCTRINE
                        // (battle-seeking) ONLY when the best target is ADJACENT (same engine call
                        // V85 uses) — the destination-blind Rey class (+130 for an unreachable
                        // target) stays an R1 fine and V85's -800 weight holds her (V85-protect
                        // boundary: -720 < Pass). The L2 strength gate also applies (fine >= +200).
                        int v297TargetOrdinal =
                            v297Evaluation.selectedTargetOrdinal();
                        PhysicalCard bestTargetLocCard =
                            v297TargetOrdinal >= 0
                                    && v297TargetOrdinal
                                        < v297TargetLocations.size()
                                ? v297TargetLocations.get(v297TargetOrdinal)
                                : null;
                        boolean v297TargetAdjacent = false;
                        try {
                            v297TargetAdjacent = bestTargetLocCard != null
                                && game.getModifiersQuerying().isAdjacentSites(gameState, location, bestTargetLocCard);
                        } catch (Exception ignore) { /* false */ }
                        if (v297TargetAdjacent) {
                            ladderClaimR2("V29.7 WEAPON HUNTER",
                                v297Evaluation.delta(), 0.0f, true);
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
            MoveOpportunityPolicy.SpreadAnalysis spread =
                MoveOpportunityPolicy.spread(
                    gameState, playerId, mySide,
                    location, myPower, theirPower);
            MoveOpportunityPolicy.Contribution spreadContribution =
                MoveOpportunityPolicy.spreadContribution(spread);
            if (spreadContribution.applies() && spread.viable) {
                action.addReasoning(
                    spreadContribution.reason(), spreadContribution.delta());
                // T4.1 (2026-07-06): early return removed. SPREAD attempts an R2 DOCTRINE
                // claim (spec Table 2); its "contest" branch is battle-seeking. Typical
                // scores (< +200, no drain-delta) fail the L2 strength gate and stay R1.
                ladderClaimR2("SPREAD", spread.score, 0.0f,
                    spread.reason != null && spread.reason.startsWith("Can contest"));
            } else if (spreadContribution.applies()) {
                action.addReasoning(
                    spreadContribution.reason(), spreadContribution.delta());
                // T4.1 (2026-07-06): early return removed — R1 fine, block falls through.
            }
        }

        // === V29.13: FORCE DRAIN MODIFIER CHECK — AVOID BAD DRAIN LOCATIONS ===
        // Rando was moving to locations with -1 force drain modifiers instead of better sites.
        // Check the force drain amount at the destination vs current location.
        // Penalize moves to locations where our force drain would be low/reduced.
        if (gameState != null && game != null && location != null) {
            try {
                String actionTextLowerFD = action.getDisplayText() != null
                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                MoveDrainRoutingPolicy.ExplicitDestinationDrain drain =
                    MoveDrainRoutingPolicy.explicitDestinationDrain(
                        gameState, game, location, playerId,
                        actionTextLowerFD);
                if (drain.contribution().applies()) {
                    action.addReasoning(
                        drain.contribution().reason(),
                        drain.contribution().delta());
                    if (drain.direction()
                            == MoveDrainRoutingPolicy.DrainDirection.LOSS) {
                        logger.warn("V29.13 BAD DRAIN: Moving to {} drain={} vs current {} drain={} — penalty {}",
                            drain.destination().getTitle(), (int)drain.destinationDrain(),
                            location.getTitle(), (int)drain.currentDrain(),
                            (int)drain.contribution().delta());
                    } else if (drain.direction()
                            == MoveDrainRoutingPolicy.DrainDirection.GAIN) {
                        logger.info("V29.13 GOOD DRAIN: Moving to {} drain={} from {} drain={} — bonus {}",
                            drain.destination().getTitle(), (int)drain.destinationDrain(),
                            location.getTitle(), (int)drain.currentDrain(),
                            (int)drain.contribution().delta());
                        // V29.13 UPDATED 2026-07-06 T4.1: a positive drain-delta claims R2 DOCTRINE
                        // (non-battle). L2 strength gate: accepted only when drainDelta >= 2
                        // (fine 40*delta alone is < +200 for small deltas — exactly ruling L2's
                        // drain-delta arm).
                        ladderClaimR2("V29.13 GOOD DRAIN",
                            drain.contribution().delta(),
                            drain.drainDelta(), false);
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
                MoveDestinationPolicy.LandedShipEscape escape =
                    MoveDestinationPolicy.landedShipEscape(
                        gameState, game, location, playerId,
                        action::getDisplayText);
                if (escape.contribution().applies()) {
                    action.addReasoning(
                        escape.contribution().reason(),
                        escape.contribution().delta());
                    logger.warn("V91 ESCAPE LANDED SHIP: bonus {} for {} at landed site {}",
                        (int)escape.contribution().delta(),
                        escape.takeOff() ? "take-off" : "disembark",
                        location.getTitle());
                    // V91 UPDATED 2026-07-06 T4.1: landed-ship escape claims R3 SURVIVAL
                    // (fines +800/+600 kept; base applied at the finalizer).
                    ladderClaimR3("V91 ESCAPE LANDED SHIP");
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
                String actionDisplay = action.getDisplayText() != null
                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                MoveDrainRoutingPolicy.CantinaShuttle shuttle =
                    MoveDrainRoutingPolicy.cantinaShuttle(
                        gameState, location, cardToMove, playerId,
                        actionDisplay);
                if (shuttle.contribution().applies()) {
                    action.addReasoning(
                        shuttle.contribution().reason(),
                        shuttle.contribution().delta());
                    logger.warn("V73 SHUTTLE: {} → {} — drain BOTH Tatooine sites (+400)",
                        location.getTitle(), shuttle.destination().getTitle());
                    // V73 UPDATED 2026-07-06 T4.1: the shuttle claims R2 DOCTRINE (non-battle;
                    // +400 passes L2). This is the move-1 boundary fix: V85's old -2000+return
                    // buried the shuttle forever; now R2 base + fines = +5560 > Pass.
                    ladderClaimR2("V73 SHUTTLE",
                        shuttle.contribution().delta(), 0.0f, false);
                } else if (shuttle.pairMatched()) {
                    // Source becomes empty → not a shuttle, just a relocation
                    logger.debug("V73: Cantina ↔ Mos Eisley move but source goes empty — no shuttle bonus");
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
            MoveDestinationPolicy.DestinationContest destination =
                MoveDestinationPolicy.destinationContest(
                    gameState, game, location, cardToMove,
                    playerId, opponentId, v34ActionText,
                    MoveEvaluator::isJediOrPadawan,
                    uncontestedDestination -> logger.warn(
                        "V36 CONTEST DRAIN: {} — opponent drains UNCONTESTED at {} — extra urgency!",
                        cardToMove != null ? cardToMove.getTitle() : "?",
                        uncontestedDestination.getTitle()));

            if (destination.destination() != null) {
                if (destination.contestContribution().applies()) {
                    action.addReasoning(
                        destination.contestContribution().reason(),
                        destination.contestContribution().delta());
                    logger.warn("V34 CONTEST: {} moving to {} (opponent power {}{}) — bonus +{}",
                        cardToMove != null ? cardToMove.getTitle() : "?",
                        destination.destination().getTitle(),
                        (int)destination.opponentPowerAtDestination(),
                        destination.jediAtDestination() ? " JEDI" : "",
                        (int)destination.contestContribution().delta());
                    // V34/V36 UPDATED 2026-07-06 T4.1: the contest claims R2 DOCTRINE
                    // (battle-seeking — subject to the V137 canWinAt veto per ruling L3;
                    // move-4a/4b boundaries). Bonus >= +250 always passes the L2 gate.
                    ladderClaimR2("V34 CONTEST",
                        destination.contestContribution().delta(),
                        0.0f, true);
                } else {
                    if (destination.battlegroundAdvanceContribution().applies()) {
                        action.addReasoning(
                            destination.battlegroundAdvanceContribution().reason(),
                            destination.battlegroundAdvanceContribution().delta());
                        logger.info("V111 BG ADVANCE: {} from {} (non-BG) to {} (BG) — drain position +400",
                            cardToMove != null ? cardToMove.getTitle() : "?",
                            location.getTitle(),
                            destination.destination().getTitle());
                        // V111 UPDATED 2026-07-06 T4.1: the BG advance claims R2 DOCTRINE
                        // (non-battle: destination is EMPTY; +400 passes L2, existing V38.3 carve kept).
                        ladderClaimR2("V111 BG ADVANCE",
                            destination.battlegroundAdvanceContribution().delta(),
                            0.0f, false);
                    } else if (destination.wrongDirectionVeto()) {
                        // V38.3 UPDATED 2026-07-06 T4.1: the -9999 hard block is now a DEFERRED
                        // ladder veto (-100000 at the finalizer) so the V53b mandatory-transit
                        // claim identities can suppress it (ruling L3 carve-out; move-3f
                        // boundary: a Mapuzo exit to an EMPTY site must fire — the old code
                        // stalled at ~-9199). Non-transit movers still get the full veto.
                        ladderWrongDirVeto = true;
                        ladderWrongDirVetoReason =
                            destination.wrongDirectionReason();
                        logger.warn("V38.3 WRONG DIRECTION: {} to empty {} — opponents at {} — veto flagged (transit carve-out may suppress)",
                            cardToMove != null ? cardToMove.getTitle() : "?",
                            destination.destination().getTitle(),
                            destination.opponentUncontestedLocation());
                    }

                    if (destination.castleVeto()) {
                        // V38.3 UPDATED 2026-07-06 T4.1: Castle-retreat arm converted to the
                        // ladder hard-veto class (-100000 at the finalizer). NO transit
                        // carve-out here — the carve-out is keyed to the V53b transit claim
                        // identities and a Mapuzo exit never targets Mustafar Castle.
                        ladderVetoHard = true;
                        ladderVetoHardReason = destination.castleVetoReason();
                        logger.warn("V38.3 CASTLE RETREAT BLOCKED (LADDER VETO): {} trying to flee to Mustafar Castle!",
                            cardToMove != null ? cardToMove.getTitle() : "?");
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

}
