package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: DEPLOY-2 — Character Siting (reorg 2026-07-06) ═══
// Owns: character→site placement scoring — V136 §A–D + overlays (contest/protect/spy
// V166–V174), concentrate-vs-spread (V96), senators (V83/V88/V99), solo hold (V156),
// V188 Alderaan gate; callable from RESPONSE for react-deploys. Hub: V136 LIVE
// (evaluateSite() in THIS file is the hub; overlay arms live in each bot's
// DeployEvaluator/CardSelectionEvaluator). SHARED FILE — serves BOTH bots (rando AND
// chosenone); any code motion here needs Steve. KIND mix + key magnitudes: BANDED
// ±2000 (§A team viability) / ±700 (§B strategic position), overlay bands +250..+1100,
// vetoes to -2000 (V110) / -900 (V188).
// Absorbs (dead, commented below/nearby — revert path, do not delete): V90, V122,
// V67aj, V67al, V67as (+ V133 dropped pre-ship). Dead ledger travels with this hub.
// Cross-refs: DEPLOY-1 (V67bc feeds this hub), DEPLOY-3 (weapon gate), MOVE (V137
// winnability parity pair), RESPONSE. See resources/RANDO_REORG_PLAN_2026-07-02.md §3
// + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
/**
 * V136 — Unified scoring for "deploy character X to site Y" decisions.
 *
 * Supersedes V90, V122, V67aj, V67as, V67al by consolidating all
 * character→site placement signals into one score so §2A regressions
 * stop recurring when individual rules over-dominate each other.
 *
 * Author: Steve (spec) + K-2 (impl), 2026-05-26.
 *
 * Side-symmetric: lives in ai/models/common/. Both rando and chosenone
 * call into the same static method. All side-specific data (objective
 * relevance, hand contents, deck ship count, per-site effects) is
 * passed in as primitives so this class never imports rando.* or
 * chosenone.* packages.
 *
 * Returns a single score for the (deployingCard, candidateSite) pair.
 * Higher is better. Callers sum this with their own scoring (V51, V96,
 * V67br, V67bj, V67bn, V67bu, V75, V121).
 *
 * See /tmp/V136_SPEC_V3.md (or the V136_HANDOFF.md fallback) for the
 * dominance table and §-by-§ rationale.
 */
public class CharacterDeploySiteEvaluator {

    private static final Logger LOG = LogManager.getLogger(CharacterDeploySiteEvaluator.class);

    // §B ability cap by turn (turn ≥ 4 nullifies the cap)
    private static final int[] ABILITY_CAP_PER_TURN = {
        0,   // turn 0 unused
        10,  // turn 1
        13,  // turn 2
        17   // turn 3
        // turn 4+: cap nullifies, see logic below
    };

    private CharacterDeploySiteEvaluator() { /* static-only */ }

    /**
     * Score a deploy of {@code deployingCard} to {@code candidateSite}.
     *
     * @param game                   game handle (needed for ModifiersQuerying access)
     * @param deployingCard          the character/ship being deployed
     * @param candidateSite          the target location (site or system)
     * @param playerId               our side's playerId
     * @param isObjectiveRelevantSite  caller's side-specific ObjectiveAnalyzer flag
     * @param friendlyHand           caller's hand (for buddy-in-hand lookahead)
     * @param availableForceForDeploys caller's remaining-force estimate
     * @param currentTurn            current game turn (1-indexed)
     * @param deckShipCount          deck-snapshot ship count (§D2 override)
     * @param perSiteEffectActive    caller text-scanned per-site effect flag
     * @return combined score for this (card, site) pairing
     */
    public static float evaluateSite(
            SwccgGame game,
            PhysicalCard deployingCard,
            PhysicalCard candidateSite,
            String playerId,
            boolean isObjectiveRelevantSite,
            List<PhysicalCard> friendlyHand,
            int availableForceForDeploys,
            int currentTurn,
            int deckShipCount,
            boolean perSiteEffectActive) {
        return evaluateSite(
            game, deployingCard, candidateSite, playerId, isObjectiveRelevantSite,
            friendlyHand, availableForceForDeploys, currentTurn, deckShipCount,
            perSiteEffectActive, true);
    }

    /** Keeps objective relevance policy while allowing its numeric term to move owners. */
    public static float evaluateSite(
            SwccgGame game,
            PhysicalCard deployingCard,
            PhysicalCard candidateSite,
            String playerId,
            boolean isObjectiveRelevantSite,
            List<PhysicalCard> friendlyHand,
            int availableForceForDeploys,
            int currentTurn,
            int deckShipCount,
            boolean perSiteEffectActive,
            boolean includeObjectiveSiteContribution) {

        if (game == null || deployingCard == null || candidateSite == null || playerId == null) {
            return 0f;  // fail-open
        }
        GameState gs = game.getGameState();
        ModifiersQuerying mq = game.getModifiersQuerying();
        if (gs == null || mq == null) return 0f;

        String opponentId = game.getOpponent(playerId);

        // ─── V188: Set Your Course For Alderaan — drains canceled at Death Star sites ───
        // The objective's FRONT text: "At Death Star sites, your Force drains and battle damage
        // against you are canceled." An ability character parked at a Death Star site is wasted
        // there: it cannot Force drain (the point of an ability character). Steer ability characters
        // to drainable battlegrounds instead. Detection is front-only for free: PhysicalCardImpl
        // .getBlueprint() returns _backBlueprint once flipped, so getTitles() (and thus
        // Filters.title) stops matching "Set Your Course For Alderaan" the moment it flips to "The
        // Ultimate Power In The Universe" — at which point the Death Star becomes the win condition
        // and you WANT to be there. Only ability >= 1 is hit; ability-0 fodder may still hold a site.
        // No clean engine "drains canceled here" query exists, so we key on the objective + site.
        // Narrow early gate: fires ONLY for (ability char + Death Star site + this objective front),
        // so it cannot dominate the §A/§B/§C/§D scoring in any other situation.
        try {
            Float v188Ability = deployingCard.getBlueprint() != null
                ? deployingCard.getBlueprint().getAbility() : null;
            if (v188Ability != null && v188Ability >= 1f
                    && Filters.Death_Star_site.accepts(gs, mq, candidateSite)
                    && !Filters.filterActive(game, null, Filters.and(
                            Filters.owner(playerId),
                            Filters.title("Set Your Course For Alderaan"))).isEmpty()) {
                LOG.warn("V188 ALDERAAN DEATH-STAR: {} (ability {}) → {} — drains canceled at Death Star sites, wasted deploy (-900)",
                    safeTitle(deployingCard), v188Ability, safeTitle(candidateSite));
                return -900f;
            }
        } catch (Exception e) { /* fail-open */ }

        // ─── §A team viability ──────────────────────────────────────────
        float scoreA = computeTeamViability(
            gs, mq, deployingCard, candidateSite, playerId, opponentId,
            friendlyHand, availableForceForDeploys, currentTurn, isObjectiveRelevantSite);

        // ─── §B strategic position ─────────────────────────────────────
        float scoreB = computeStrategicPosition(
            gs, mq, deployingCard, candidateSite, playerId, opponentId,
            currentTurn, isObjectiveRelevantSite, perSiteEffectActive,
            includeObjectiveSiteContribution);

        // ─── §C modifiers ──────────────────────────────────────────────
        float scoreC = computeModifiers(
            gs, mq, deployingCard, candidateSite, playerId, opponentId);

        // ─── §D site-count gate ────────────────────────────────────────
        float scoreD = computeSiteCountGate(
            gs, mq, candidateSite, playerId, currentTurn,
            isObjectiveRelevantSite, deckShipCount);

        float total = scoreA + scoreB + scoreC + scoreD;

        if (LOG.isDebugEnabled()) {
            LOG.debug("V136 {} → {}: §A={} §B={} §C={} §D={} total={}",
                safeTitle(deployingCard), safeTitle(candidateSite),
                scoreA, scoreB, scoreC, scoreD, total);
        }
        return total;
    }

    // ═══════════════════════════════════════════════════════════════════
    // §A team viability (primary, ±2000)
    // ═══════════════════════════════════════════════════════════════════

    private static float computeTeamViability(
            GameState gs, ModifiersQuerying mq,
            PhysicalCard deployingCard, PhysicalCard candidateSite,
            String playerId, String opponentId,
            List<PhysicalCard> friendlyHand, int availableForceForDeploys,
            int currentTurn, boolean isObjectiveRelevantSite) {

        // Existing friendlies at the site
        List<PhysicalCard> friendliesAtSite;
        try {
            friendliesAtSite = gs.getCardsAtLocation(candidateSite);
        } catch (Exception e) { return 0f; /* fail-open */ }
        if (friendliesAtSite == null) friendliesAtSite = java.util.Collections.emptyList();

        float teamAbility = 0f;
        int teamBodyCount = 1;  // include the deploying card
        boolean isAboard = false;  // simplified: aboard-ship detection extension

        for (PhysicalCard pc : friendliesAtSite) {
            if (pc == null || pc.getBlueprint() == null) continue;
            if (!playerId.equals(pc.getOwner())) continue;
            if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
            Float ab = pc.getBlueprint().getAbility();
            if (ab != null) teamAbility += ab;
            teamBodyCount++;
        }
        if (deployingCard.getBlueprint() != null
                && deployingCard.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
            Float dAb = deployingCard.getBlueprint().getAbility();
            if (dAb != null) teamAbility += dAb;
        }

        // Power: engine value at site + deploying card blueprint power
        float baselinePower = 0f;
        try {
            baselinePower = mq.getTotalPowerAtLocation(gs, candidateSite, playerId, false, false);
        } catch (Exception e) { /* baseline 0 */ }
        float teamPower = baselinePower
            + (deployingCard.getBlueprint() != null ? deployingCard.getBlueprint().getPower() : 0f);

        float oppPower = 0f;
        try {
            oppPower = mq.getTotalPowerAtLocation(gs, candidateSite, opponentId, false, false);
        } catch (Exception e) { /* baseline 0 */ }

        // 2026-06-01 OPP UNDERCOVER DETECTION (Steve, Jabba Palace pile-in):
        // ModifiersQuerying.getTotalPowerAtLocation excludes undercover
        // opponent characters, so a site with only an opponent spy reports
        // oppPower == 0. The original §A logic below (powerPass && bodyPass
        // → +500) then treats this as an "uncontested win" — but Rando
        // can't actually drain here because the spy blocks it. Replay
        // (Jyn Erso at Jabba's Palace: Audience Chamber): Hondo, Mara
        // Jade, Jango all scored +800 V136 and piled in, +1155 total each
        // deploy, none drained. Pre-existing V67f SPY-ONLY only fires in
        // the move-destination block of evaluateLocationSelection, not in
        // V136 §A — gap.
        // Fix: scan permanents at the candidate site for opponent characters
        // whose isUndercover() is true. If found AND the engine reports
        // oppPower == 0 (i.e., only spies, no real opponents), short-
        // circuit §A with a heavy negative (-1000) so the spy-blocked
        // site drops below pass and Rando picks elsewhere. When oppPower
        // > 0, the spy is alongside real opponents → the contested-fight
        // logic below still runs normally (we're going to battle anyway).
        boolean oppUndercoverAtSite = false;
        try {
            for (PhysicalCard pc : friendliesAtSite) {
                if (pc == null) continue;
                if (!opponentId.equals(pc.getOwner())) continue;
                if (pc.isUndercover()) { oppUndercoverAtSite = true; break; }
            }
        } catch (Exception ignore) { /* false */ }
        if (oppUndercoverAtSite && oppPower == 0f) {
            LOG.warn("V136 §A SPY-BLOCKED: {} → {} has opp undercover (oppPower=0) — wasted deploy, drain blocked (-1000)",
                safeTitle(deployingCard), safeTitle(candidateSite));
            return -1000f;
        }

        // Opponent has any weapon at site?
        boolean oppHasWeapon = false;
        try {
            for (PhysicalCard pc : friendliesAtSite) {
                if (pc == null) continue;
                if (!opponentId.equals(pc.getOwner())) continue;
                if (Filters.character_with_a_weapon.accepts(gs, mq, pc)) {
                    oppHasWeapon = true;
                    break;
                }
            }
        } catch (Exception e) { /* false */ }

        boolean powerPass = teamPower >= oppPower;
        boolean abilityPass = teamAbility >= 4;
        boolean bodyPass = !oppHasWeapon || teamBodyCount >= 2 || isAboard;

        // Buddy-in-hand lookahead (Steve's "possibility of deploying buddy")
        //
        // BUG FIX 2026-05-26 PM: original iAmTheBuddy 0-score branch
        // assumed the higher-ability buddy in hand would follow to THIS site.
        // It didn't — Cardo deployed to Supremacy: Throne Room as "the buddy"
        // expecting Phasma to join, but Phasma chose Crait: Salt Plateau. Cardo
        // got left alone.
        //
        // New behavior: both low-ability characters score -200 when buddy in
        // hand. Force-saving. Higher-ability character will eventually deploy
        // somewhere (pushed by other rules), and lower-ability can join via
        // §A team-viability bonus on the NEXT decision cycle.
        boolean buddyInHand = false;
        if (!abilityPass && friendlyHand != null) {
            for (PhysicalCard inHand : friendlyHand) {
                if (inHand == null || inHand == deployingCard) continue;
                if (inHand.getBlueprint() == null) continue;
                if (inHand.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                float cost = safeDeployCost(inHand);
                if (cost > availableForceForDeploys) continue;
                Float buddyAb = inHand.getBlueprint().getAbility();
                if (buddyAb == null) continue;
                if (teamAbility + buddyAb >= 4f) {
                    buddyInHand = true;
                    break;
                }
            }
        }

        // V151 (Steve, 2026-05-28): CO-DEPLOY POWER LOOKAHEAD.
        // Steve: "Why not just deploy to the same site as opponent and attack?
        // Save the move force." V136 used to block a direct deploy into a
        // contested site whenever the SOLO unit was out-powered — forcing the
        // wasteful deploy-safe-then-move two-step. But if the hand holds enough
        // reinforcements to win the battle TOGETHER, deploying the whole strike
        // group straight to the enemy site is better (no move force wasted).
        //
        // When the site is contested (oppPower > 0), ability passes (we can draw
        // battle destiny), but solo power falls short, greedily project the hand
        // reinforcements we could afford to co-deploy here this turn. If the
        // combined power would win, score this as a coordinated-attack setup
        // (+400) so the group commits here directly. The other characters score
        // even higher on their own decisions once this one is here (team viable
        // +500), so the pack assembles in one deploy phase.
        if (!powerPass && abilityPass && oppPower > 0f && friendlyHand != null) {
            float projectedPower = teamPower;
            float forceLeft = availableForceForDeploys
                - (deployingCard.getBlueprint() != null
                    ? safeDeployCost(deployingCard) : 0f);
            for (PhysicalCard inHand : friendlyHand) {
                if (inHand == null || inHand == deployingCard) continue;
                if (inHand.getBlueprint() == null) continue;
                if (inHand.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                float cost = safeDeployCost(inHand);
                if (cost > forceLeft) continue;
                Float rp = inHand.getBlueprint().hasPowerAttribute()
                    ? inHand.getBlueprint().getPower() : null;
                if (rp == null) continue;
                projectedPower += rp;
                forceLeft -= cost;
                if (projectedPower >= oppPower) break;
            }
            // V177 (Steve, 2026-06): count the GEAR too, not just bodies. Replay
            // aab2jiaa5sca (Luke vs Kylo): Young Skywalker (power 6) vs Kylo (10) — V151
            // projected only characters, fell short, and hit the -2000 cap, so Rando never
            // committed. But Luke's Bionic Hand (a power-boosting Device) and Luke's Lightsaber
            // were in hand and would have crushed Kylo. Project the combat power of affordable
            // weapons/devices in hand that would arm the strike group: device +2 (e.g. Bionic
            // Hand's power boost), lightsaber +3, other weapon +2.
            if (projectedPower < oppPower) {
                for (PhysicalCard inHand : friendlyHand) {
                    if (inHand == null || inHand == deployingCard || inHand.getBlueprint() == null) continue;
                    CardCategory cat = inHand.getBlueprint().getCardCategory();
                    if (cat != CardCategory.WEAPON && cat != CardCategory.DEVICE) continue;
                    float cost = safeDeployCost(inHand);
                    if (cost > forceLeft) continue;
                    float gear;
                    if (cat == CardCategory.DEVICE) {
                        gear = 2f;
                    } else {
                        String gt = inHand.getTitle() != null
                            ? inHand.getTitle().toLowerCase(Locale.ROOT) : "";
                        gear = gt.contains("lightsaber") ? 3f : 2f;
                    }
                    projectedPower += gear;
                    forceLeft -= cost;
                    if (projectedPower >= oppPower) break;
                }
            }
            if (projectedPower >= oppPower) {
                return 400f;  // coordinated attack: deploy the group here, skip the move
            }

            // V181 (Steve, 2026-06): DRAIN-WEIGHTED FAIR-FIGHT COMMIT.
            // Even after projecting buddies + gear we fall a little short — but a
            // SMALL power gap is a coin-flip, not a loss: ability >= 4 means we draw
            // battle destiny (avg ~3.5), and the extra body is forfeit fodder (pay
            // attrition with a cheap card, not a key one) plus a weapon carrier.
            // So a close fight is worth taking IF (a) the gap is small (one destiny
            // draw flips it), (b) the drain at the site is worth contesting (drain 1
            // = juice < squeeze, let them have it), and (c) the forfeit trade is even
            // (neither side blown out). Raw power alone over-vetoed this — Steve's
            // Luke-empty-hand-vs-Kylo case. Bonus scales with the drain (the more we
            // bleed each turn, the harder we commit to stopping it), capped at 300 —
            // strictly below the +400 clean-win tier, so it never steals a real win,
            // and strictly above PASS, so a worth-it fight beats ceding the drain.
            // NOTE: this only fires when we're contested (oppPower > 0); the turn<=2
            // solo-protection (V156) is the uncontested case (oppPower == 0) and is
            // mutually exclusive, so nothing it guards is bypassed.
            //
            // V181 UPDATED 2026-07-06 T4.1: the gap/drain/parity condition chain now
            // lives in the SHARED predicate MovePredicates.canWinAt (same package) —
            // the same predicate MoveEvaluator's V137 consumes, killing the move-4a
            // deploy/move same-tag drift permanently (parity pair V136/V137).
            // PARITY NOTE (behavior-identical port): at this point projectedPower <
            // oppPower always (the V151 loop above returned +400 otherwise), so the
            // predicate's clean-win arm can never fire here; its tolerance arm is
            // byte-equivalent to the old inline chain (gap in (0,3], opp drain >= 2,
            // ability >= 4 — guaranteed by the enclosing abilityPass gate — and the
            // one-sided 1.25x forfeit cap, forfeits summed identically). All
            // deploy-side bonus math (+min(300, drain*100)) is kept exactly as was.
            // OLD (inline chain, kept for revert):
            // float v181Gap = oppPower - projectedPower;
            // if (v181Gap > 0f && v181Gap <= 3f) {
            //     float v181Drain = 0f;
            //     try { v181Drain = mq.getForceDrainAmount(gs, candidateSite, opponentId); }
            //     catch (Exception ignore) { /* treat as 0 → won't fire */ }
            //     if (v181Drain >= 2f) {
            //         float ourForfeit = safeForfeit(deployingCard);
            //         float theirForfeit = 0f;
            //         for (PhysicalCard pc : friendliesAtSite) {
            //             if (pc == null || pc.getBlueprint() == null) continue;
            //             if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
            //             if (playerId.equals(pc.getOwner())) ourForfeit += safeForfeit(pc);
            //             else if (opponentId.equals(pc.getOwner())) theirForfeit += safeForfeit(pc);
            //         }
            //         boolean v181Parity = theirForfeit <= 0f
            //             || ourForfeit <= theirForfeit * 1.25f;
            //         if (v181Parity) {
            //             float v181Bonus = Math.min(300f, v181Drain * 100f);
            //             LOG.warn("V181 FAIR-FIGHT COMMIT: ... → +{}", ...);
            //             return v181Bonus;
            //         }
            //         LOG.warn("V181 NO-PARITY: ... — forfeit mismatch, hold", ...);
            //     }
            // }
            // Forfeit trade over the bodies that will actually battle — our team at
            // the site + the deploying card vs their team here (their side is summed
            // inside the shared predicate from the same site list).
            float v181OurForfeit = safeForfeit(deployingCard);
            for (PhysicalCard pc : friendliesAtSite) {
                if (pc == null || pc.getBlueprint() == null) continue;
                if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                if (playerId.equals(pc.getOwner())) v181OurForfeit += safeForfeit(pc);
            }
            if (MovePredicates.canWinAt(gs, mq, playerId, opponentId, candidateSite,
                    projectedPower, teamAbility, v181OurForfeit)) {
                float v181Drain = MovePredicates.drainAt(gs, mq, candidateSite, opponentId);
                float v181Bonus = Math.min(300f, v181Drain * 100f);
                // Guard: a tolerance pass guarantees drain >= 2 → bonus >= 200. A bonus of 0
                // can only mean the predicate fail-opened on an engine error — fall through
                // to the normal scoring exactly like the old inline chain did.
                if (v181Bonus > 0f) {
                    LOG.warn("V181 FAIR-FIGHT COMMIT (shared canWinAt): {} → {} gap={} drain={} ourForfeit={} → +{}",
                        safeTitle(deployingCard), safeTitle(candidateSite),
                        oppPower - projectedPower, v181Drain, v181OurForfeit, v181Bonus);
                    return v181Bonus;
                }
            }
        }

        // Scoring (iAmTheBuddy removed — see bug-fix comment above)
        // 2026-06-25 (Steve, Fix #2): ability only matters when there's a BATTLE. At an
        // UNCONTESTED site (oppPower == 0) a low-ability fodder body (droids, Neimoidians)
        // just establishes presence/drain and seeds objective flip-sites — deploy it; fall
        // through to V156 (turn<=2 solo guard) then the +500 reward. Gate the ability penalty
        // to contested sites only. Boundary: uncontested ability<4 goes -1500 -> +500 (or
        // V156 -300); contested + ability>=4 unchanged; V151/V181 need oppPower>0. Shared
        // common/ file → fixes both bots.
        if (oppPower > 0f && !abilityPass) {
            if (buddyInHand) return -200f;  // contested + buddy coming: coordinate
            return -1500f;  // contested + weak solo can't win the battle: almost never
        }
        // V156 (Steve, 2026-05-28): DON'T LEAVE A WEAK CHARACTER SOLO ON TURN <= 2.
        // Steve: "turn two is dangerous to leave a 3-power 4-ability character by
        // themselves. Either save force for a larger deploy [or] bolster a
        // preexisting location." A low-power body alone at a fresh (uncontested)
        // site gets overwhelmed next turn. Without this, §A rewarded it +500
        // (power 3 >= opp 0, no opp weapon) — actively encouraging the lone spread.
        //   teamBodyCount == 1  -> no friendly character is already here (solo)
        //   oppPower == 0        -> fresh spread location, not a contested fight
        //   !v156AnyBuddyAvailable -> no buddy plan, in hand OR on the table
        // Returning negative makes this site lose to bolstering an existing group
        // (+500) and to PASS — so Rando reinforces or saves the force for a bigger
        // combined deploy instead of dribbling a body out alone.
        //
        // 2026-05-29 REVISED (Steve, after replay filx81 + Seventh Sister death):
        // The earlier "power <= 3 OR ability <= 4" gate still let strong characters
        // (Vader/Dooku/Sidious) deploy solo on the assumption they could defend
        // themselves. Steve corrected: "They should not deploy solo. They should
        // at minimum have a buddy move to them or deploy a buddy." So the weak-
        // defender gate is GONE. The penalty now fires on ANY solo deploy turn
        // <=2 when there is no buddy plan — where "buddy plan" means either
        //   (a) an affordable character in hand to co-deploy this turn, or
        //   (b) any friendly character already on the table at another site
        //       (a potential mover next phase).
        // If neither, even Vader gets held until a buddy is in place. Power/
        // ability are kept for logging only.
        float v156DeployPower = (deployingCard.getBlueprint() != null
                && deployingCard.getBlueprint().hasPowerAttribute()
                && deployingCard.getBlueprint().getPower() != null)
                ? deployingCard.getBlueprint().getPower() : 0f;
        float v156DeployAbility = (deployingCard.getBlueprint() != null
                && deployingCard.getBlueprint().hasAbilityAttribute()
                && deployingCard.getBlueprint().getAbility() != null)
                ? deployingCard.getBlueprint().getAbility() : 0f;

        boolean v156AnyBuddyAvailable = false;
        // (a) Affordable character in hand to co-deploy.
        if (friendlyHand != null) {
            for (PhysicalCard inHand : friendlyHand) {
                if (inHand == null || inHand == deployingCard) continue;
                if (inHand.getBlueprint() == null) continue;
                if (inHand.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                float cost = safeDeployCost(inHand);
                if (cost <= availableForceForDeploys) {
                    v156AnyBuddyAvailable = true;
                    break;
                }
            }
        }
        // (b) Any friendly character already on the table (potential mover).
        if (!v156AnyBuddyAvailable) {
            try {
                for (PhysicalCard pc : gs.getAllPermanentCards()) {
                    if (pc == null || pc.getBlueprint() == null) continue;
                    if (!playerId.equals(pc.getOwner())) continue;
                    if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                    if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                    v156AnyBuddyAvailable = true;
                    break;
                }
            } catch (Exception ignore) { /* false */ }
        }

        // === V156 UPDATED 2026-06-25 (Steve): smart solo-deploy hold (ability/weapon/BG aware) ===
        // Prior V156: turn<=2, solo, uncontested, no-buddy -> -300 at ANY site (OLD, commented below).
        // Now scoped to a ground BATTLEGROUND site + CHARACTER + NON-objective, and ability/weapon aware:
        //   solo allowed only if ability >= 6, OR ability >= 5 with an affordable matching weapon in hand;
        //   weak solos (<=4, or 5 unarmed) hold for a buddy (-600); an allowed strong solo still prefers a
        //   buddy when force is available (+250). Objective flip-sites are SKIPPED (deploy-to-flip, then buddy
        //   up after: Endor / Imperial Enforcements / TDIGWATT — generic via isObjectiveRelevantSite).
        //   Ships/systems excluded (CHARACTER + isBattleground site gate). Vetted by council + helper agent.
        // OLD:
        // if (currentTurn <= 2 && teamBodyCount == 1 && oppPower == 0f && !v156AnyBuddyAvailable) {
        //     LOG.warn("V156 SOLO-NO-BUDDY BLOCK: ... (-300)"); return -300f;
        // }
        boolean v156IsBG = false;
        try { v156IsBG = mq.isBattleground(gs, candidateSite, null); } catch (Exception ignore) { /* false */ }
        boolean v156IsChar = deployingCard.getBlueprint() != null
                && deployingCard.getBlueprint().getCardCategory() == CardCategory.CHARACTER;
        // 2026-06-28 (Steve, Verge interim): the flip-site SKIP above is too broad — it let a weak
        // lone Ozzel (ability 2) sit at a Scarif flip-site turn 1 and die before the flip was even
        // reachable. For On The Verge Of Greatness the flip needs the Death Star ORBITING Scarif;
        // until then the Scarif flip-sites are NOT "ready", so DON'T peel weak solos there — apply
        // the hold. (Mirrors the DrawEvaluator V79 dsAtScarif check. To be subsumed by Steve's
        // general "never leave a low-ability solo" rule.)
        // V156 UPDATED 2026-07-07: the flip-not-ready scan moved VERBATIM into the shared
        // static helper isV156FlipNotReady(...) below, so the NEW move-side V156 JOIN-GROUP
        // arm (MoveEvaluator + CardSelectionEvaluator, both bots) reuses the SAME predicate
        // instead of forking it. Behavior identical. OLD inline scan (kept for revert):
        // boolean v156FlipNotReady = false;
        // try {
        //     boolean vergeUp = false, dsOnTable = false, dsAtScarif = false;
        //     for (PhysicalCard pc : gs.getAllPermanentCards()) {
        //         if (pc == null || !playerId.equals(pc.getOwner()) || pc.getBlueprint() == null) continue;
        //         if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
        //         String t = pc.getTitle() != null ? pc.getTitle().toLowerCase(Locale.ROOT) : "";
        //         if (t.contains("on the verge of greatness") || t.contains("taking control of the weapon")) vergeUp = true;
        //         if (t.contains("death star") && pc.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
        //             dsOnTable = true;
        //             PhysicalCard dsLoc = pc.getAtLocation();
        //             if (dsLoc != null && dsLoc.getTitle() != null
        //                     && dsLoc.getTitle().toLowerCase(Locale.ROOT).contains("scarif")) dsAtScarif = true;
        //         }
        //     }
        //     if (vergeUp && dsOnTable && !dsAtScarif) v156FlipNotReady = true;
        // } catch (Exception ignore) { /* fall through */ }
        boolean v156FlipNotReady = isV156FlipNotReady(gs, playerId);
        // V156 UPDATED 2026-07-07 (Steve, Fel-at-Beach loss, audit rows deploy-siting-1/-2):
        // the turn<=2 cliff let the SAME deploy the hold correctly blocked on turn 2 sail
        // through on turn 3 — Baron Soontir Fel (ability 3, power 2) solo to Scarif: Beach,
        // battled and forfeited three minutes later. A weak solo at an uncontested BG is no
        // safer on turn 3+ than on turn 2, so for the ability<4 band the hold now applies on
        // ALL turns — but only when a buddy plan exists (another character on table or
        // affordable in hand, the existing v156AnyBuddyAvailable); with no buddy anywhere the
        // lone body is all we have, let it deploy. The ability>=4 class keeps the original
        // turn<=2 gate and ALL exemptions (ability>=6 solo, armed ability-5, objective
        // flip-site carve-back) exactly as before.
        // Boundary (Fel replay math): §A +500 -> -600 flips V136 CS 800 -> -300; Beach site
        // total 1065 -> -35, loses to Citadel Tower's 615 (join Vader's stack) — exactly the
        // turn-2 behavior (Tagge 905 -> Tower). -600 still beats §A's +500 uncontested reward
        // by construction; objective-forced deploys stay exempt via the carve-back.
        // OLD (turn-gate only, kept for revert):
        // if (currentTurn <= 2 && teamBodyCount == 1 && oppPower == 0f
        //         && v156IsBG && v156IsChar && (!isObjectiveRelevantSite || v156FlipNotReady)) {
        boolean v156WeakBandAllTurns = v156DeployAbility < 4f && v156AnyBuddyAvailable;
        if ((currentTurn <= 2 || v156WeakBandAllTurns) && teamBodyCount == 1 && oppPower == 0f
                && v156IsBG && v156IsChar && (!isObjectiveRelevantSite || v156FlipNotReady)) {
            boolean v156Armed = false;
            if (v156DeployAbility >= 5f && friendlyHand != null) {
                for (PhysicalCard w : friendlyHand) {
                    if (w == null || w.getBlueprint() == null) continue;
                    if (w.getBlueprint().getCardCategory() != CardCategory.WEAPON) continue;
                    if (safeDeployCost(w) > availableForceForDeploys) continue;
                    com.gempukku.swccgo.filters.Filter wm;
                    try { wm = w.getBlueprint().getMatchingCharacterFilter(); } catch (Exception e) { continue; }
                    if (wm == null || wm == Filters.none) continue;
                    try { if (wm.accepts(gs, mq, deployingCard)) { v156Armed = true; break; } } catch (Exception ignore) { /* */ }
                }
            }
            boolean v156CanSolo = v156DeployAbility >= 6f || v156Armed;
            if (!v156CanSolo) {
                LOG.warn("V156 SOLO HOLD: {} (ability {}) solo at battleground {} turn {} — too weak to solo, hold for a buddy (-600)",
                    safeTitle(deployingCard), v156DeployAbility, safeTitle(candidateSite), currentTurn);
                return -600f;
            }
            if (v156AnyBuddyAvailable) {
                LOG.warn("V156 SOLO OK, PREFER BUDDY: {} (ability {}) can solo {} but force for a buddy exists — mild (+250)",
                    safeTitle(deployingCard), v156DeployAbility, safeTitle(candidateSite));
                return 250f;
            }
            // strong enough + no buddy affordable: solo is fine — fall through to normal scoring.
        }

        if (powerPass && bodyPass) return 500f;
        if (powerPass && !bodyPass) return -500f;  // opp weapon, body=1
        if (!powerPass && bodyPass) {
            float gap = oppPower - teamPower;
            if (gap >= 4) return -500f;
            return -200f;
        }
        return -2000f;  // worst case cap: ability passes but power+body both fail
    }

    // ═══════════════════════════════════════════════════════════════════
    // §B strategic position (secondary, ±700)
    // ═══════════════════════════════════════════════════════════════════

    private static float computeStrategicPosition(
            GameState gs, ModifiersQuerying mq,
            PhysicalCard deployingCard, PhysicalCard candidateSite,
            String playerId, String opponentId,
            int currentTurn, boolean isObjectiveRelevantSite,
            boolean perSiteEffectActive,
            boolean includeObjectiveSiteContribution) {

        float score = 0f;

        boolean isBG = false;
        try {
            isBG = mq.isBattleground(gs, candidateSite, null);
        } catch (Exception e) { /* false */ }

        if (isBG) score += 100f;
        if (isObjectiveRelevantSite && includeObjectiveSiteContribution) score += 200f;

        // NBG penalty (two-tier, override-able)
        boolean nbgOverride = isObjectiveRelevantSite || perSiteEffectActive;
        if (!isBG && !nbgOverride) {
            score += (currentTurn <= 2) ? -500f : -300f;
        }

        float oppPower = 0f;
        try {
            oppPower = mq.getTotalPowerAtLocation(gs, candidateSite, opponentId, false, false);
        } catch (Exception e) { /* 0 */ }
        boolean isUncontested = (oppPower == 0f);

        float ourPowerHere = 0f;
        try {
            ourPowerHere = mq.getTotalPowerAtLocation(gs, candidateSite, playerId, false, false);
        } catch (Exception e) { /* 0 */ }

        // === V157 (Steve, 2026-05-28): CAP IS UNCONTESTED-ONLY; OVERWHELM WEAK CONTESTED SITES ===
        // Steve: "the cap should only be for uncontested sites. If opponent has 4 or
        // less ability at a battleground and Rando can deploy 20+ ability to win, we
        // should not limit him. As soon as opponent has characters at a site, try to
        // overthrow if it makes sense." Over-stacking an EMPTY site is wasteful, but
        // massing a CONTESTED site to overwhelm is the whole point — never cap a fight.
        // (§A team viability already rewards a winnable contested deploy +500 and
        // penalizes an out-powered one, so the win/lose judgment lives there; this
        // block just (a) confines the over-stack + ability cap to uncontested sites
        // and (b) adds an extra nudge to overthrow a weakly-defended contested site.)
        if (isUncontested) {
            // Over-stack penalty — don't pile an empty site.
            if (ourPowerHere >= 15f) score += -700f;
            else if (ourPowerHere >= 10f) score += -400f;
            else if (ourPowerHere >= 5f) score += -200f;

            // Per-site ability saturation cap (turn-tiered, off turn 4+) — UNCONTESTED ONLY.
            if (currentTurn >= 1 && currentTurn <= 3) {
                int cap = ABILITY_CAP_PER_TURN[currentTurn];
                float teamAbilityAfterDeploy = 0f;
                try {
                    List<PhysicalCard> friendliesAtSite = gs.getCardsAtLocation(candidateSite);
                    if (friendliesAtSite != null) {
                        for (PhysicalCard pc : friendliesAtSite) {
                            if (pc == null || pc.getBlueprint() == null) continue;
                            if (!playerId.equals(pc.getOwner())) continue;
                            if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                            Float ab = pc.getBlueprint().getAbility();
                            if (ab != null) teamAbilityAfterDeploy += ab;
                        }
                    }
                } catch (Exception e) { /* 0 */ }
                if (deployingCard.getBlueprint() != null
                        && deployingCard.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                    Float dAb = deployingCard.getBlueprint().getAbility();
                    if (dAb != null) teamAbilityAfterDeploy += dAb;
                }
                if (teamAbilityAfterDeploy > cap) score += -700f;
            }
        } else {
            // Contested: NO cap (never limit a fight). Extra nudge to overthrow a
            // WEAKLY-defended site (opponent's total character ability here <= 4).
            float oppAbilityHere = 0f;
            try {
                List<PhysicalCard> atSite = gs.getCardsAtLocation(candidateSite);
                if (atSite != null) {
                    for (PhysicalCard pc : atSite) {
                        if (pc == null || pc.getBlueprint() == null) continue;
                        if (!opponentId.equals(pc.getOwner())) continue;
                        if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                        Float ab = pc.getBlueprint().getAbility();
                        if (ab != null) oppAbilityHere += ab;
                    }
                }
            } catch (Exception e) { /* 0 */ }
            if (oppAbilityHere > 0f && oppAbilityHere <= 4f) {
                // +200 (council consensus 2026-05-28, 5/5: +300 risked over-committing one
                // site and starving Force-drain economy; lowered to nudge, not compel).
                score += 200f;
                LOG.warn("V157 OVERWHELM: opp ability {} <= 4 at {} — encourage massing to overthrow (+200)",
                    oppAbilityHere, safeTitle(candidateSite));
            }
        }

        return score;
    }

    // ═══════════════════════════════════════════════════════════════════
    // §C weapon modifiers (±50)
    // ═══════════════════════════════════════════════════════════════════

    private static float computeModifiers(
            GameState gs, ModifiersQuerying mq,
            PhysicalCard deployingCard, PhysicalCard candidateSite,
            String playerId, String opponentId) {

        float score = 0f;
        try {
            List<PhysicalCard> atSite = gs.getCardsAtLocation(candidateSite);
            if (atSite != null) {
                for (PhysicalCard pc : atSite) {
                    if (pc == null) continue;
                    if (Filters.character_with_a_weapon.accepts(gs, mq, pc)) {
                        if (opponentId.equals(pc.getOwner())) score += -10f;
                        else if (playerId.equals(pc.getOwner())) score += 10f;
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return score;
    }

    // ═══════════════════════════════════════════════════════════════════
    // §D site-count gate (turn 1-2 only)
    // ═══════════════════════════════════════════════════════════════════

    private static float computeSiteCountGate(
            GameState gs, ModifiersQuerying mq,
            PhysicalCard candidateSite, String playerId,
            int currentTurn, boolean isObjectiveRelevantSite,
            int deckShipCount) {

        if (currentTurn > 2) return 0f;  // gate releases turn 3+

        boolean candidateIsSystem = false;
        boolean candidateIsBG = false;
        try {
            candidateIsBG = mq.isBattleground(gs, candidateSite, null);
        } catch (Exception e) { /* false */ }
        try {
            // System detection: SwccgCardBlueprint has CardCategory.LOCATION;
            // SwccgCardBlueprint.getCardSubType() returns SYSTEM for systems.
            if (candidateSite.getBlueprint() != null) {
                String sub = candidateSite.getBlueprint().getCardSubtype() != null
                    ? candidateSite.getBlueprint().getCardSubtype().toString() : "";
                if ("SYSTEM".equalsIgnoreCase(sub)) candidateIsSystem = true;
            }
        } catch (Exception e) { /* false */ }
        boolean candidateIsGroundBG = candidateIsBG && !candidateIsSystem;

        // Is candidate new for us (no friendly here yet)?
        boolean candidateIsNewForUs = true;
        try {
            List<PhysicalCard> here = gs.getCardsAtLocation(candidateSite);
            if (here != null) {
                for (PhysicalCard pc : here) {
                    if (pc == null) continue;
                    if (playerId.equals(pc.getOwner())
                        && pc.getBlueprint() != null
                        && pc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                        candidateIsNewForUs = false;
                        break;
                    }
                }
            }
        } catch (Exception e) { /* assume new */ }

        if (!candidateIsNewForUs) return 0f;  // not a new occupation

        // Count current controlled BGs/systems
        int currentGroundBGs = 0;
        int currentSystems = 0;
        try {
            for (PhysicalCard loc : gs.getLocationsInOrder()) {
                if (loc == null || loc == candidateSite) continue;
                boolean weHave = false;
                List<PhysicalCard> atLoc = gs.getCardsAtLocation(loc);
                if (atLoc != null) {
                    for (PhysicalCard pc : atLoc) {
                        if (pc == null) continue;
                        if (playerId.equals(pc.getOwner())
                            && pc.getBlueprint() != null
                            && pc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                            weHave = true; break;
                        }
                    }
                }
                if (!weHave) continue;
                boolean locIsBG = false;
                try { locIsBG = mq.isBattleground(gs, loc, null); } catch (Exception e) { /* */ }
                boolean locIsSystem = false;
                if (loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null) {
                    locIsSystem = "SYSTEM".equalsIgnoreCase(loc.getBlueprint().getCardSubtype().toString());
                }
                if (locIsSystem) currentSystems++;
                else if (locIsBG) currentGroundBGs++;
            }
        } catch (Exception e) { /* fail-open */ return 0f; }

        boolean shipHeavyDeck = (deckShipCount >= 5);

        // Ground BG cap
        if (candidateIsGroundBG && currentGroundBGs >= 2 && !isObjectiveRelevantSite) {
            return -700f;
        }
        // System cap
        if (candidateIsSystem && currentSystems >= 2 && !isObjectiveRelevantSite && !shipHeavyDeck) {
            return -700f;
        }
        return 0f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * V156 flip-not-ready predicate (extracted 2026-07-07 from the inline scan in
     * computeTeamViability — body VERBATIM, behavior identical).
     *
     * 2026-06-28 (Steve, Verge interim): the flip-site SKIP is too broad when the flip
     * is not mechanically reachable — a weak lone Ozzel (ability 2) sat at a Scarif
     * flip-site turn 1 and died before the flip was even possible. For On The Verge Of
     * Greatness the flip needs the Death Star ORBITING Scarif; until then the Scarif
     * flip-sites are NOT "ready", so weak solos get no objective exemption there.
     * (Mirrors the DrawEvaluator V79 dsAtScarif check.)
     *
     * PUBLIC + shared (2026-07-07) so the move-side V156 JOIN-GROUP arm (MoveEvaluator
     * + CardSelectionEvaluator, both bots) tests the SAME "is the solo actually doing
     * ready objective work" carve-back the deploy side uses — one predicate, no fork.
     */
    public static boolean isV156FlipNotReady(GameState gs, String playerId) {
        if (gs == null || playerId == null) return false;
        try {
            boolean vergeUp = false, dsOnTable = false, dsAtScarif = false;
            for (PhysicalCard pc : gs.getAllPermanentCards()) {
                if (pc == null || !playerId.equals(pc.getOwner()) || pc.getBlueprint() == null) continue;
                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                String t = pc.getTitle() != null ? pc.getTitle().toLowerCase(Locale.ROOT) : "";
                if (t.contains("on the verge of greatness") || t.contains("taking control of the weapon")) vergeUp = true;
                if (t.contains("death star") && pc.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
                    dsOnTable = true;
                    // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681): getAtLocation()
                    // is ALWAYS null for a mobile-system LOCATION card, so dsAtScarif stayed false
                    // and this predicate returned 'flip not ready' even with the DS parked in Scarif
                    // orbit (or the objective already flipped). Use getSystemOrbited() — the engine's
                    // own orbit primitive (same check as the flip condition, Filters.isOrbiting).
                    // PhysicalCard dsLoc = pc.getAtLocation();
                    // if (dsLoc != null && dsLoc.getTitle() != null
                    //         && dsLoc.getTitle().toLowerCase(Locale.ROOT).contains("scarif")) dsAtScarif = true;
                    String dsOrbited = pc.getSystemOrbited();
                    if (dsOrbited != null
                            && dsOrbited.toLowerCase(Locale.ROOT).contains("scarif")) dsAtScarif = true;
                }
            }
            return vergeUp && dsOnTable && !dsAtScarif;
        } catch (Exception ignore) { return false; }
    }

    private static float safeDeployCost(PhysicalCard pc) {
        try {
            Float cost = pc.getBlueprint().getDeployCost();
            return cost == null ? 0f : cost;
        } catch (Exception e) { return 0f; }
    }

    /** V181: forfeit value, 0 if the card has none / is unreadable. */
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
