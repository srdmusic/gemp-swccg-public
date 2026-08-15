package com.gempukku.swccgo.ai.models.common.phase;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure BATTLE-1 initiation decisions. The coordinator retains card and board
 * reads, prediction calls, FormationSafety checks, logging, and score mutation.
 */
public final class BattleInitiationPolicy {
    private static final int FAVORABLE_THRESHOLD = 5;
    private static final int MARGINAL_THRESHOLD = 2;
    private static final float ABILITY_BATTLE_MAX_POWER_DEFICIT = 2.0f;
    private static final int MIN_RESERVE_FOR_BATTLE = 3;

    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public enum PredictionBranch {
        NONE,
        PROBABLE_DEFEAT,
        PYRRHIC
    }

    public record PredictionDecision(
            PredictionBranch branch,
            Contribution contribution) {
    }

    public enum SpecificBattleBranch {
        NONE,
        NO_OPPONENT,
        OUTGUNNED,
        WEAPONS_NOT_ENOUGH,
        FAVORABLE,
        ARMED_MARGINAL,
        UNARMED_MARGINAL,
        UNFAVORABLE
    }

    public record SpecificBattleDecision(
            SpecificBattleBranch branch,
            Contribution contribution,
            boolean favorable) {
    }

    public record FallbackDecision(
            List<Contribution> contributions,
            boolean favorable) {
        public FallbackDecision {
            contributions = List.copyOf(contributions);
        }
    }

    public enum ReserveBranch {
        NONE,
        OVERPOWER,
        EMPTY,
        CRITICAL,
        LOW,
        BELOW_MINIMUM
    }

    public record ReserveDecision(
            ReserveBranch branch,
            Contribution contribution) {
    }

    public enum InterruptBranch {
        DTF_BLOCKED,
        DTF_READY,
        STANDARD_CRITICAL,
        STANDARD_LOW,
        NONE
    }

    public record InterruptDecision(
            InterruptBranch branch,
            Contribution contribution) {
    }

    private BattleInitiationPolicy() {
    }

    public static Contribution barrierRisk(
            boolean vaderPresent,
            float ourPower,
            float theirPower,
            float powerWithoutVader,
            int charactersWithoutVader,
            boolean huntDownV,
            float vaderExpendabilityFactor) {
        if (!vaderPresent || !(ourPower > 0.0f) || !(theirPower > 0.0f)) {
            return Contribution.none();
        }

        float deficit = theirPower - powerWithoutVader;
        if (!(deficit > 5.0f)) {
            return Contribution.none();
        }

        float penalty = charactersWithoutVader <= 1 ? -250.0f : -150.0f;
        if (deficit > 10.0f) {
            penalty -= 100.0f;
        }
        if (huntDownV) {
            penalty *= vaderExpendabilityFactor;
        }

        return new Contribution(
                true,
                String.format(
                        "V29.9 BARRIER RISK: If opponent Barriers Vader, remaining power %.0f vs %.0f — %s!",
                        powerWithoutVader,
                        theirPower,
                        charactersWithoutVader == 0
                                ? "NO ONE LEFT"
                                : "crushed"),
                penalty);
    }

    public static Contribution huntAggression(
            boolean vaderPresent,
            boolean vaderArmed,
            boolean huntDownV,
            boolean lukePresent) {
        if (!vaderPresent || !vaderArmed || !huntDownV) {
            return Contribution.none();
        }
        float bonus = lukePresent ? 200.0f : 80.0f;
        return new Contribution(
                true,
                String.format(
                        "V29.9 HUNT DOWN: Armed Vader should FIGHT! %s (+%.0f)",
                        lukePresent
                                ? "LUKE IS HERE — THIS IS THE OBJECTIVE!"
                                : "Vader hunts and destroys!",
                        bonus),
                bonus);
    }

    public static Contribution inquisitorDestiny(
            boolean huntDownV,
            boolean inquisitorPresent,
            boolean hatredPresent,
            boolean jediPresent) {
        if (!huntDownV || !inquisitorPresent) {
            return Contribution.none();
        }
        float bonus = hatredPresent ? 250.0f : 120.0f;
        if (jediPresent) {
            bonus += 100.0f;
        }
        bonus = Math.min(300.0f, bonus);
        return new Contribution(
                true,
                String.format(
                        "V35 HUNT DESTINY: Inquisitor in battle%s%s — +%d total battle destiny!",
                        hatredPresent ? " + HATRED" : "",
                        jediPresent ? " vs JEDI" : "",
                        hatredPresent ? 2 : 1),
                bonus);
    }

    public static PredictionDecision prediction(
            String locationTitle,
            float winProbability,
            float expectedDamageTaken) {
        if (winProbability < 0.35f) {
            return new PredictionDecision(
                    PredictionBranch.PROBABLE_DEFEAT,
                    new Contribution(
                            true,
                            String.format(
                                    "V76 BATTLE PREDICT: winRate %.0f%% at %s — probable defeat, don't initiate!",
                                    winProbability * 100.0f,
                                    locationTitle),
                            -800.0f));
        }
        if (expectedDamageTaken >= 10.0f) {
            return new PredictionDecision(
                    PredictionBranch.PYRRHIC,
                    new Contribution(
                            true,
                            String.format(
                                    "V76 BATTLE PREDICT: avg damage taken %.1f at %s — pyrrhic, don't initiate!",
                                    expectedDamageTaken,
                                    locationTitle),
                            -500.0f));
        }
        return new PredictionDecision(
                PredictionBranch.NONE,
                Contribution.none());
    }

    // ADJUSTED 2026-08-08 (passivity fix, m01683): legacy 10-arg overload —
    // reserveHealthy defaults true (the proportional favorable arm applies).
    public static SpecificBattleDecision specificBattle(
            String locationTitle,
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            float weaponBonus,
            float weaponEffectiveDiff,
            boolean vaderPresent,
            boolean lukePresent,
            boolean hasIhyn) {
        return specificBattle(locationTitle, ourPower, theirPower, ourAbility,
                theirAbility, weaponBonus, weaponEffectiveDiff, vaderPresent,
                lukePresent, hasIhyn, true);
    }

    public static SpecificBattleDecision specificBattle(
            String locationTitle,
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            float weaponBonus,
            float weaponEffectiveDiff,
            boolean vaderPresent,
            boolean lukePresent,
            boolean hasIhyn,
            boolean reserveHealthy) {
        if (ourPower > 0.0f && theirPower == 0.0f) {
            return new SpecificBattleDecision(
                    SpecificBattleBranch.NO_OPPONENT,
                    new Contribution(true, "No opponent here", -20.0f),
                    false);
        }
        if (!(ourPower > 0.0f) || !(theirPower > 0.0f)) {
            return new SpecificBattleDecision(
                    SpecificBattleBranch.NONE,
                    Contribution.none(),
                    false);
        }

        if (theirPower > ourPower && weaponBonus == 0.0f) {
            float penalty = theirPower > ourPower * 2.0f
                    ? -600.0f
                    : -300.0f;
            return new SpecificBattleDecision(
                    SpecificBattleBranch.OUTGUNNED,
                    new Contribution(
                            true,
                            String.format(
                                    "V29 DON'T INITIATE: %.0f vs %.0f power — we're outgunned!",
                                    ourPower,
                                    theirPower),
                            penalty),
                    false);
        }
        if (theirPower > ourPower && weaponBonus > 0.0f
                && weaponEffectiveDiff < MARGINAL_THRESHOLD) {
            return new SpecificBattleDecision(
                    SpecificBattleBranch.WEAPONS_NOT_ENOUGH,
                    new Contribution(
                            true,
                            String.format(
                                    "V29.7 WEAPONS NOT ENOUGH: power %.0f+weapons vs %.0f — still risky",
                                    ourPower,
                                    theirPower),
                            -150.0f),
                    false);
        }
        if (weaponEffectiveDiff >= FAVORABLE_THRESHOLD) {
            // V29.7 ADJUSTED 2026-08-08 (passivity fix, m01683): flat +150 paid a +20
            // crush the same as a +5 edge, so big advantages still lost the score race to
            // stacked penalties (live log: unarmed +3 edge netted ~+15 vs Pass). Pay +25
            // per point of weapon-adjusted advantage over the threshold; total capped at
            // +400 below to stay inside the band.
            // float bonus = 150.0f;
            // ADJUSTED 2026-08-08 (passivity fix, m01683, capture-harness gate): the
            // proportional extra applies only with a HEALTHY reserve (>=3, the V61 "<3
            // risky" boundary). At a critical reserve one bad destiny can be fatal and
            // the V61 -400/-800 brake must keep winning — the harness pinned exactly
            // that (13v8 crush at 1-card reserve must still Pass). The passivity
            // evidence was all healthy-reserve positions.
            float bonus = 150.0f
                    + (reserveHealthy
                        ? 25.0f * (weaponEffectiveDiff - FAVORABLE_THRESHOLD)
                        : 0.0f);
            String reason;
            if (weaponBonus > 0.0f) {
                bonus += weaponBonus * 10.0f;
                if (vaderPresent && lukePresent) {
                    bonus += 100.0f;
                    reason = String.format(
                            "V29.7 VADER vs LUKE at %s! Power %.0f + weapons vs %.0f — CHALLENGE!",
                            locationTitle,
                            ourPower,
                            theirPower);
                } else {
                    reason = String.format(
                            "V29.7 ARMED BATTLE at %s (power %.0f + weapons vs %.0f, effective diff=%.0f)",
                            locationTitle,
                            ourPower,
                            theirPower,
                            weaponEffectiveDiff);
                }
                if (hasIhyn) {
                    reason += " + IHYN!";
                }
            } else {
                reason = String.format(
                        "Favorable battle at %s (power %.0f vs %.0f, ability %.0f vs %.0f)",
                        locationTitle,
                        ourPower,
                        theirPower,
                        ourAbility,
                        theirAbility);
            }
            // V29.7 ADJUSTED 2026-08-08 (passivity fix, m01683): cap the advantage-
            // proportional total (incl. weapon/Vader adders) at +400 to stay in band.
            bonus = Math.min(bonus, 400.0f);
            return new SpecificBattleDecision(
                    SpecificBattleBranch.FAVORABLE,
                    new Contribution(true, reason, bonus),
                    true);
        }
        if (weaponEffectiveDiff >= MARGINAL_THRESHOLD) {
            if (weaponBonus > 0.0f) {
                return new SpecificBattleDecision(
                        SpecificBattleBranch.ARMED_MARGINAL,
                        new Contribution(
                                true,
                                String.format(
                                        "V34 ARMED MARGINAL at %s (power %.0f + weapons vs %.0f) — weapons help!",
                                        locationTitle,
                                        ourPower,
                                        theirPower),
                                80.0f),
                        false);
            }
            // V29 ADJUSTED 2026-08-08 (passivity fix, m01683): an unarmed +2..+4 edge
            // was scored NEGATIVE (-50, favorable=false — which then stacked the -60
            // no-favorable scanOutcome on top). Live log: an unarmed +3-power edge netted
            // ~+15 vs Pass, so any small penalty flipped the bot to passing. A real power
            // edge is a fight worth taking: +30 and favorable=true.
            // return new SpecificBattleDecision(
            //         SpecificBattleBranch.UNARMED_MARGINAL,
            //         new Contribution(
            //                 true,
            //                 String.format(
            //                         "V29 MARGINAL at %s (power %.0f vs %.0f) — risky with weapons",
            //                         locationTitle,
            //                         ourPower,
            //                         theirPower),
            //                 -50.0f),
            //         false);
            return new SpecificBattleDecision(
                    SpecificBattleBranch.UNARMED_MARGINAL,
                    new Contribution(
                            true,
                            String.format(
                                    "V29 UNARMED MARGINAL at %s (power %.0f vs %.0f) — small edge, take the fight",
                                    locationTitle,
                                    ourPower,
                                    theirPower),
                            30.0f),
                    true);
        }

        float penalty = -100.0f;
        if (weaponEffectiveDiff < -8.0f) {
            penalty = -200.0f;
        }
        if (weaponEffectiveDiff < -15.0f) {
            penalty = -400.0f;
        }
        return new SpecificBattleDecision(
                SpecificBattleBranch.UNFAVORABLE,
                new Contribution(
                        true,
                        String.format(
                                "V29: UNFAVORABLE at %s (power %.0f vs %.0f) - don't initiate!",
                                locationTitle,
                                ourPower,
                                theirPower),
                        penalty),
                false);
    }

    public static FallbackDecision fallbackLocation(
            String locationTitle,
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            float effectiveDiff,
            int opponentArmedCharacters,
            float expectedHitLoss,
            boolean pyrrhic) {
        List<Contribution> contributions = new ArrayList<>();
        if (pyrrhic) {
            contributions.add(new Contribution(
                    true,
                    String.format(
                            "V76 (fallback) HIT ECONOMICS at %s: %d armed opponents, expected card loss %.1f — pyrrhic, don't initiate!",
                            locationTitle,
                            opponentArmedCharacters,
                            expectedHitLoss),
                    -500.0f));
        }

        float powerDiff = ourPower - theirPower;
        float abilityDiff = ourAbility - theirAbility;
        boolean catastrophic = theirPower > ourPower * 2.0f
                && theirPower > 6.0f;
        if (catastrophic) {
            contributions.add(new Contribution(
                    true,
                    String.format(
                            "V22.4 DANGER at %s (%.0f vs %.0f) - might battle here!",
                            locationTitle,
                            ourPower,
                            theirPower),
                    -80.0f));
        }

        if (effectiveDiff >= MARGINAL_THRESHOLD && !pyrrhic) {
            contributions.add(new Contribution(
                    true,
                    String.format(
                            "Favorable battle at %s (power %.0f vs %.0f, ability %.0f vs %.0f)",
                            locationTitle,
                            ourPower,
                            theirPower,
                            ourAbility,
                            theirAbility),
                    40.0f));
            return new FallbackDecision(contributions, true);
        }
        if (!pyrrhic
                && abilityDiff >= 0.0f
                && powerDiff >= -ABILITY_BATTLE_MAX_POWER_DEFICIT
                && !catastrophic) {
            contributions.add(new Contribution(
                    true,
                    String.format(
                            "V164a ABILITY BATTLE at %s: ability %.0f >= %.0f (power %.0f vs %.0f) — fair trade, initiate to break drains",
                            locationTitle,
                            ourAbility,
                            theirAbility,
                            ourPower,
                            theirPower),
                    40.0f));
            return new FallbackDecision(contributions, true);
        }
        if (abilityDiff < -1.0f) {
            contributions.add(new Contribution(
                    true,
                    String.format(
                            "Ability disadvantage at %s (%.0f vs %.0f) - enemy draws more destiny",
                            locationTitle,
                            ourAbility,
                            theirAbility),
                    -25.0f));
        }
        return new FallbackDecision(contributions, false);
    }

    public static Contribution scanOutcome(
            boolean favorableBattle,
            boolean contestedLocation) {
        if (!favorableBattle && contestedLocation) {
            return new Contribution(
                    true,
                    "No favorable battles available - don't initiate",
                    -60.0f);
        }
        if (!contestedLocation) {
            return new Contribution(
                    true,
                    "No contested locations",
                    -20.0f);
        }
        return Contribution.none();
    }

    public static Contribution mustFight(
            int opponentUncontestedDrains,
            boolean winnableBattle,
            boolean behindOnLifeForce) {
        if (opponentUncontestedDrains < 2
                || !winnableBattle
                || !behindOnLifeForce) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                String.format(
                        "V34 MUST-FIGHT: Opponent draining from %d uncontested locations, we're behind - must engage!",
                        opponentUncontestedDrains),
                200.0f);
    }

    public static ReserveDecision reserve(
            float bestOverpowerMargin,
            int reserveDeckSize) {
        if (bestOverpowerMargin >= 8.0f) {
            return new ReserveDecision(
                    ReserveBranch.OVERPOWER,
                    Contribution.none());
        }
        if (reserveDeckSize == 0) {
            return new ReserveDecision(
                    ReserveBranch.EMPTY,
                    new Contribution(
                            true,
                            "V61 RESERVE EMPTY: 0 cards in Reserve — CANNOT draw battle destiny, auto-lose!",
                            -800.0f));
        }
        if (reserveDeckSize == 1) {
            return new ReserveDecision(
                    ReserveBranch.CRITICAL,
                    new Contribution(
                            true,
                            "V61 RESERVE CRITICAL: 1 card in Reserve — can draw 1 destiny max, very risky!",
                            -400.0f));
        }
        if (reserveDeckSize == 2) {
            return new ReserveDecision(
                    ReserveBranch.LOW,
                    new Contribution(
                            true,
                            "V61 RESERVE LOW: 2 cards in Reserve — weapon destiny + 1 battle destiny only",
                            -200.0f));
        }
        if (reserveDeckSize < MIN_RESERVE_FOR_BATTLE) {
            return new ReserveDecision(
                    ReserveBranch.BELOW_MINIMUM,
                    new Contribution(
                            true,
                            String.format(
                                    "Low reserve deck (%d) - risky destiny draws",
                                    reserveDeckSize),
                            -80.0f));
        }
        return new ReserveDecision(
                ReserveBranch.NONE,
                Contribution.none());
    }

    public static InterruptDecision interruptForce(
            boolean opponentHasDrawTheirFire,
            int forcePile,
            int handSize) {
        if (opponentHasDrawTheirFire) {
            int forceNeeded = 3;
            if (forcePile < forceNeeded) {
                float penalty = forcePile == 0 ? -100.0f : -60.0f;
                return new InterruptDecision(
                        InterruptBranch.DTF_BLOCKED,
                        new Contribution(
                                true,
                                String.format(
                                        "V27.1 DRAW THEIR FIRE: Opponent has DTF on table! Need %d Force for interrupts (tax+loss), only %d in pile — Ghhhk UNUSABLE!",
                                        forceNeeded,
                                        forcePile),
                                penalty));
            }
            return new InterruptDecision(
                    InterruptBranch.DTF_READY,
                    new Contribution(
                            true,
                            String.format(
                                    "V27.1 DRAW THEIR FIRE: DTF on table, %d Force available — interrupts usable but costly",
                                    forcePile),
                            0.0f));
        }
        if (forcePile < 2) {
            return new InterruptDecision(
                    InterruptBranch.STANDARD_CRITICAL,
                    new Contribution(
                            true,
                            String.format(
                                    "V27 BATTLE FORCE WARNING: Only %d Force in pile — limited interrupt capacity! Battle losses come from hand (%d cards)!",
                                    forcePile,
                                    handSize),
                            -40.0f));
        }
        if (forcePile < 4) {
            return new InterruptDecision(
                    InterruptBranch.STANDARD_LOW,
                    new Contribution(
                            true,
                            String.format(
                                    "V27 BATTLE FORCE: Low Force (%d) — limited interrupt capacity in battle",
                                    forcePile),
                            -15.0f));
        }
        return new InterruptDecision(
                InterruptBranch.NONE,
                Contribution.none());
    }

    public static List<Contribution> lifeForce(
            boolean behindOnLifeForce,
            boolean aheadOnLifeForce,
            int lifeForce,
            int criticalLifeForce) {
        List<Contribution> contributions = new ArrayList<>();
        if (behindOnLifeForce) {
            contributions.add(new Contribution(
                    true,
                    "Behind on life force - slightly more aggressive",
                    15.0f));
        } else if (aheadOnLifeForce) {
            contributions.add(new Contribution(
                    true,
                    "Ahead on life force - can afford to wait",
                    -20.0f));
        }
        if (lifeForce <= criticalLifeForce) {
            contributions.add(new Contribution(
                    true,
                    "Low life force - need to act",
                    30.0f));
        }
        return List.copyOf(contributions);
    }
}
