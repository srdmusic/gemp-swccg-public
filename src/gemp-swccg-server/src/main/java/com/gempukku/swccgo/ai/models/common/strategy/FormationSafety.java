package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: SVC-SAFETY / FORMATION SAFETY (2026-07-11c) ═══
// Steve's FOUR LAWS as non-additive constraints, enforced identically on every scoring route.
// Born from the Codex root-cause audit (Handoffs/CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md)
// + council review: ~20 prior fixes coded these basics as -150..-500 additive penalties that the
// R2 +6000 move band and +600 bonus stacks outvoted (H2), or that lived on only one of the five
// scoring routes (H1). This class is SHARED by both bots (like CharacterDeploySiteEvaluator), so
// it cannot mirror-drift. Callers map the closed deploy verdict to hardVeto or defer;
// CombinedEvaluator applies constraint order before additive score.
//
// THE FOUR LAWS:
//  L1 never leave a solo character behind          -> vetoMoveOrigin
//  L2 never voluntarily battle with zero battle-destiny draws -> vetoInitiateBattle
//  L3 defer a weak solo without an exact same-destination plan -> deploy verdict
//  L4 never move a weak solo into an enemy-held site           -> vetoMoveDestination
//
// EXEMPTIONS (explicit, testable — from Steve's rulings):
//  - SOLO DOMINANCE (Steve 2026-07-11): >=2x weapon-adjusted power, characters only.
//  - Objective flip-gate one-body steer (V193-class): caller passes flipGateSiteTitle.
//  - Undercover spies (solo by design).
//  - Destiny-eligible strong solo (ability >= 4) at an UNCONTESTED location.
//  - Survival retreat from a doomed origin (gap >= 6 weapon-adjusted; V33 standard).
//
// ENGINE TRUTH (BattleDestiny.java:31-91): normal battle destiny requires TOTAL ability >= 4
// at the battle location. Ability parity does NOT grant a draw. Codex verified 2026-07-11.
//
// WEAPON TRUTH (Codex m00172 + CODEX_PERMANENT_WEAPON_AUDIT): permanent-weapon ownership is the
// TYPED Icon.PERMANENT_WEAPON, NOT a game-text substring ("mentions" cards false-positived).
// All weapon-adjustment math here uses the typed icon.
// Returns from veto*(): null = allowed; non-null = human-readable veto reason.
// Deploy assessment instead returns ALLOW, DEFER, HARD_BLOCK, or UNKNOWN.
// Partial information rule (council): if a route lacks the facts, DON'T call — never veto blind.
// ═══════════════════════════════════════════════════════════
public final class FormationSafety {

    private FormationSafety() {}

    /** Engine-aligned normal-battle-destiny threshold (BattleDestiny.java abilityBasicThreshold). */
    public static final float DESTINY_ABILITY_THRESHOLD = 4.0f;
    /** Dominance multiple for the Steve-2026-07-11 solo-dominance exemption. */
    public static final float DOMINANCE_MULTIPLE = 2.0f;
    /** Doomed-origin gap (V33 BUDDY BREAK standard) for the survival-retreat exemption. */
    public static final float DOOMED_GAP = 6.0f;

    /** BATCH1b-CORR (2026-07-13, Codex m00225 #2 / m00262 fixtures): count friendly
     *  non-undercover CHARACTERS among the cards at a location. Presence test for the
     *  empty-site gates — total power misreads a present power-0 friendly as absence.
     *  Takes the card collection directly so pure JUnit can drive it without a GameState. */
    public static int countFriendlyNonUndercoverCharacters(Iterable<PhysicalCard> cardsAtLocation, String playerId) {
        int n = 0;
        if (cardsAtLocation == null || playerId == null) return 0;
        for (PhysicalCard c : cardsAtLocation) {
            if (c == null || c.getBlueprint() == null) continue;
            if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
            if (!playerId.equals(c.getOwner())) continue;
            if (c.isUndercover()) continue;
            n++;
        }
        return n;
    }

    /** TYPED weapon bonus for one character: attached WEAPON cards + Icon.PERMANENT_WEAPON.
     *  Lightsaber +5 / other +3 (V29.7 heuristic). Never reads game text. */
    public static float weaponBonusOf(GameState gs, PhysicalCard character) {
        float bonus = 0f;
        if (gs == null || character == null || character.getBlueprint() == null) return 0f;
        try {
            List<PhysicalCard> atts = gs.getAttachedCards(character);
            if (atts != null) {
                for (PhysicalCard att : atts) {
                    if (att == null || att.getBlueprint() == null) continue;
                    if (att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                        String wt = att.getTitle() != null ? att.getTitle().toLowerCase(Locale.ROOT) : "";
                        bonus += wt.contains("lightsaber") ? 5.0f : 3.0f;
                    }
                }
            }
            boolean permWeapon = false;
            try { permWeapon = character.getBlueprint().hasIcon(Icon.PERMANENT_WEAPON); } catch (Exception e) { /* no icon api issue */ }
            if (permWeapon) {
                String ct = character.getTitle() != null ? character.getTitle().toLowerCase(Locale.ROOT) : "";
                bonus += ct.contains("lightsaber") ? 5.0f : 3.0f;
            }
        } catch (Exception e) { /* fail-open: 0 */ }
        return bonus;
    }

    /** Sum of typed weapon bonuses for owner's characters at a location. */
    public static float weaponBonusAt(GameState gs, PhysicalCard location, String ownerId) {
        float bonus = 0f;
        if (gs == null || location == null || ownerId == null) return 0f;
        try {
            for (PhysicalCard c : gs.getCardsAtLocation(location)) {
                if (c == null || c.getBlueprint() == null) continue;
                if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                if (!ownerId.equals(c.getOwner())) continue;
                bonus += weaponBonusOf(gs, c);
            }
        } catch (Exception e) { /* fail-open */ }
        return bonus;
    }

    /** L2 engine truth: can playerId draw a NORMAL battle destiny at this location right now? */
    public static boolean battleDestinyEligible(SwccgGame game, GameState gs, String playerId, PhysicalCard location) {
        try {
            float ability = game.getModifiersQuerying().getTotalAbilityAtLocation(gs, playerId, location);
            return ability >= DESTINY_ABILITY_THRESHOLD;
        } catch (Exception e) {
            return true; // fail-open: never veto blind
        }
    }

    /** L2: veto reason for voluntarily initiating battle at location, or null if allowed. */
    public static String vetoInitiateBattle(SwccgGame game, GameState gs, String playerId, PhysicalCard location) {
        if (game == null || gs == null || playerId == null || location == null) return null;
        try {
            if (!battleDestinyEligible(game, gs, playerId, location)) {
                float ability = game.getModifiersQuerying().getTotalAbilityAtLocation(gs, playerId, location);
                return String.format(
                    "L2 NO-DESTINY BATTLE: total ability %.0f < %.0f at %s — zero normal battle destiny draws (engine BattleDestiny threshold)",
                    ability, DESTINY_ABILITY_THRESHOLD, location.getTitle());
            }
        } catch (Exception e) { /* fail-open */ }
        return null;
    }

    /** L4: veto reason for MOVING mover to destination, or null. Uses the ACTUAL mover + existing
     *  destination allies (never assumes the origin group follows). */
    public static String vetoMoveDestination(SwccgGame game, GameState gs, String playerId,
                                             PhysicalCard mover, PhysicalCard destination) {
        if (game == null || gs == null || playerId == null || mover == null || destination == null) return null;
        if (mover.getBlueprint() == null
                || mover.getBlueprint().getCardCategory() != CardCategory.CHARACTER) return null;
        try {
            if (mover.isUndercover()) return null;  // spies are solo by design
            String opp = gs.getOpponent(playerId);
            float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(gs, destination, opp, false, false);
            if (oppPower <= 0) return null;  // uncontested destination — L4 doesn't apply
            float oppEff = oppPower + weaponBonusAt(gs, destination, opp);
            float ourThere = game.getModifiersQuerying().getTotalPowerAtLocation(gs, destination, playerId, false, false);
            Float mp = (mover.getBlueprint().hasPowerAttribute()) ? mover.getBlueprint().getPower() : null;
            float ourEff = ourThere + (mp != null ? mp : 0f) + weaponBonusOf(gs, mover);
            // Exemption: solo dominance (Steve 2026-07-11) — overwhelming force is always allowed.
            if (ourEff >= DOMINANCE_MULTIPLE * oppEff) return null;
            // Veto when the REAL assembled force (allies there + this mover, weapon-adjusted both
            // sides) is at/beyond the doomed gap. Between parity and doom, additive scoring decides.
            if (oppEff - ourEff >= DOOMED_GAP) {
                return String.format(
                    "L4 SOLO CHARGE: moving %s into %s — assembled %.0f vs their effective %.0f (gap >= %.0f)",
                    mover.getTitle(), destination.getTitle(), ourEff, oppEff, DOOMED_GAP);
            }
        } catch (Exception e) { /* fail-open */ }
        return null;
    }

    /** L1: veto reason for a move that leaves a lone vulnerable friendly at origin, or null. */
    public static String vetoMoveOrigin(SwccgGame game, GameState gs, String playerId,
                                        PhysicalCard mover, PhysicalCard origin) {
        if (game == null || gs == null || playerId == null || mover == null || origin == null) return null;
        try {
            PhysicalCard lastBuddy = null;
            int remaining = 0;
            for (PhysicalCard c : gs.getCardsAtLocation(origin)) {
                if (c == null || c.getBlueprint() == null) continue;
                if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                if (!playerId.equals(c.getOwner())) continue;
                if (c.getCardId() == mover.getCardId()) continue;
                if (c.isUndercover()) continue;  // spies don't need escorts
                remaining++;
                lastBuddy = c;
            }
            if (remaining != 1 || lastBuddy == null) return null;  // 0 = fine, 2+ = still a group
            float remainingAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(gs, playerId, origin);
            Float ma = mover.getBlueprint().hasAbilityAttribute() ? mover.getBlueprint().getAbility() : null;
            float abilityAfter = remainingAbility - (ma != null ? ma : 0f);
            if (abilityAfter >= DESTINY_ABILITY_THRESHOLD) return null;  // leftover can still fight
            // Exemption: survival retreat — origin already doomed (V33 gap standard); staying
            // together in a lost position is worse than escaping with one body.
            String opp = gs.getOpponent(playerId);
            float oppEff = game.getModifiersQuerying().getTotalPowerAtLocation(gs, origin, opp, false, false)
                + weaponBonusAt(gs, origin, opp);
            float ourEff = game.getModifiersQuerying().getTotalPowerAtLocation(gs, origin, playerId, false, false)
                + weaponBonusAt(gs, origin, playerId);
            if (oppEff - ourEff >= DOOMED_GAP) return null;
            // Only bite when the abandonment is CONSEQUENTIAL: an opponent presence at the origin
            // (they can initiate on the leftover). Uncontested origins defer to additive scoring.
            if (oppEff <= 0) return null;
            return String.format(
                "L1 ABANDON SOLO: moving %s leaves %s alone at %s (ability after %.0f < %.0f) vs enemy presence",
                mover.getTitle(), lastBuddy.getTitle(), origin.getTitle(), abilityAfter, DESTINY_ABILITY_THRESHOLD);
        } catch (Exception e) { /* fail-open */ }
        return null;
    }

    /** V201 deploy safety is a closed, non-additive result instead of two callers
     *  independently combining a hard veto and an outvotable scalar. */
    public enum DeployConstraint {
        ALLOW,
        DEFER_UNSUPPORTED_SOLO,
        HARD_BLOCK,
        UNKNOWN
    }

    public record DeployVerdict(DeployConstraint constraint, String reason) {}

    /**
     * L3/L4 deploy assessment for one exact candidate destination.
     *
     * plannedBuddyCost is not "some character in hand." It is supplied only
     * when the AI deployment plan contains another exact physical character at
     * this same destination. Movement is deliberately not an exemption because
     * the current AI has no exact move-mode, cost, and Force-reservation proof.
     */
    public static DeployVerdict assessCharacterDeploy(SwccgGame game, GameState gs, String playerId,
                                                       PhysicalCard cardBeingDeployed,
                                                       Float deployPower, Float deployAbility,
                                                       boolean deployIsUndercover,
                                                       PhysicalCard destination,
                                                       float forceAvailable, Float thisDeployCost,
                                                       Float plannedBuddyCost,
                                                       String flipGateSiteTitle) {
        if (game == null || gs == null || playerId == null || destination == null) {
            return new DeployVerdict(DeployConstraint.UNKNOWN, "deploy formation facts incomplete");
        }
        try {
            if (deployIsUndercover) {
                return new DeployVerdict(DeployConstraint.ALLOW, "undercover deployment is solo by design");
            }
            if (flipGateSiteTitle != null && destination.getTitle() != null
                    && flipGateSiteTitle.equalsIgnoreCase(destination.getTitle())) {
                return new DeployVerdict(DeployConstraint.ALLOW, "objective flip-gate deployment");
            }

            float power = deployPower != null ? deployPower : 0f;
            float ability = deployAbility != null ? deployAbility : 0f;
            String opp = gs.getOpponent(playerId);
            float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                gs, destination, opp, false, false);
            float ourThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                gs, destination, playerId, false, false);
            boolean landsSolo = countFriendlyNonUndercoverCharacters(
                gs.getCardsAtLocation(destination), playerId) == 0;

            if (oppPower > 0) {
                float oppEff = oppPower + weaponBonusAt(gs, destination, opp);
                float ourEff = ourThere + power;
                if (ourEff >= DOMINANCE_MULTIPLE * oppEff) {
                    return new DeployVerdict(DeployConstraint.ALLOW,
                        "solo-dominance deployment");
                }
                if (landsSolo && ability < DESTINY_ABILITY_THRESHOLD) {
                    if (plannedBuddyCost != null) {
                        if (thisDeployCost == null) {
                            return new DeployVerdict(DeployConstraint.UNKNOWN,
                                "planned contested companion exists but first deploy cost is unknown");
                        }
                        if (forceAvailable - thisDeployCost < plannedBuddyCost) {
                            return new DeployVerdict(DeployConstraint.HARD_BLOCK, String.format(
                                "L4 PAIR-BUDGET: deploying this ability-%.0f body (cost %.0f) leaves %.0f Force < planned contested companion cost %.0f",
                                ability, thisDeployCost, forceAvailable - thisDeployCost, plannedBuddyCost));
                        }
                        return new DeployVerdict(DeployConstraint.ALLOW,
                            "exact same-destination contested companion remains affordable");
                    }
                    return new DeployVerdict(DeployConstraint.HARD_BLOCK, String.format(
                        "L4 WEAK SOLO INTO CONTESTED: ability %.0f alone into %s (their eff %.0f)",
                        ability, destination.getTitle(), oppEff));
                }
                return new DeployVerdict(DeployConstraint.ALLOW,
                    "contested deployment has formation support");
            }

            if (!landsSolo || ability >= DESTINY_ABILITY_THRESHOLD) {
                return new DeployVerdict(DeployConstraint.ALLOW,
                    "deployment is supported or battle-destiny eligible");
            }

            if (plannedBuddyCost == null) {
                String cardName = cardBeingDeployed != null && cardBeingDeployed.getTitle() != null
                    ? cardBeingDeployed.getTitle() : "weak character";
                return new DeployVerdict(DeployConstraint.DEFER_UNSUPPORTED_SOLO,
                    "L3 NO-PLAN SOLO: " + cardName + " would land alone at "
                        + destination.getTitle() + " with no exact same-destination companion plan");
            }

            if (thisDeployCost == null) {
                return new DeployVerdict(DeployConstraint.UNKNOWN,
                    "planned companion exists but first deploy cost is unknown");
            }
            if (forceAvailable - thisDeployCost < plannedBuddyCost) {
                return new DeployVerdict(DeployConstraint.HARD_BLOCK, String.format(
                    "L3 PAIR-BUDGET: deploying this ability-%.0f body (cost %.0f) leaves %.0f Force < planned companion cost %.0f",
                    ability, thisDeployCost, forceAvailable - thisDeployCost, plannedBuddyCost));
            }
            return new DeployVerdict(DeployConstraint.ALLOW,
                "exact same-destination companion remains affordable");
        } catch (Exception e) {
            return new DeployVerdict(DeployConstraint.UNKNOWN,
                "deploy formation assessment failed: " + e.getClass().getSimpleName());
        }
    }
}
