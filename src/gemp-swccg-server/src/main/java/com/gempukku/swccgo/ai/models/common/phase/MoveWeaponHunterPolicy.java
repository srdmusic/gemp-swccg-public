package com.gempukku.swccgo.ai.models.common.phase;

import java.util.List;
import java.util.Locale;

/**
 * Shared V29.7 armed-character target selection and scoring.
 * Adapters retain all card, board, icon, adjacency, ladder, and log work.
 */
public final class MoveWeaponHunterPolicy {
    private static final float ICON_BONUS = 15.0f;

    public record WeaponFacts(
            boolean hasWeapon,
            boolean lightsaber,
            String weaponName) {
        private static WeaponFacts none() {
            return new WeaponFacts(false, false, null);
        }
    }

    public record HunterProfile(
            boolean active,
            String characterTitle,
            boolean vader,
            boolean hasIHaveYouNow,
            String weaponName,
            float effectivePower) {
        public boolean canBeat(int opponentCount, float opponentPower) {
            return active && opponentCount > 0
                    && effectivePower > opponentPower;
        }

        private static HunterProfile inactive() {
            return new HunterProfile(
                    false, null, false, false, null, 0.0f);
        }
    }

    public record TargetFact(
            int ordinal,
            String title,
            float opponentPower,
            int opponentIcons,
            boolean lukeHere) {
    }

    public record Evaluation(
            boolean applies,
            String reason,
            float delta,
            int selectedTargetOrdinal,
            String selectedTargetTitle,
            float effectivePower,
            boolean foundLuke) {
        private static Evaluation none() {
            return new Evaluation(
                    false, null, 0.0f, -1, null, 0.0f, false);
        }
    }

    private MoveWeaponHunterPolicy() {
    }

    public static WeaponFacts weaponFacts(
            List<String> attachedWeaponTitles) {
        if (attachedWeaponTitles == null || attachedWeaponTitles.isEmpty()) {
            return WeaponFacts.none();
        }

        boolean lightsaber = false;
        String weaponName = null;
        for (String title : attachedWeaponTitles) {
            weaponName = title;
            if (title != null && title.toLowerCase(Locale.ROOT)
                    .contains("lightsaber")) {
                lightsaber = true;
            }
        }
        return new WeaponFacts(true, lightsaber, weaponName);
    }

    public static HunterProfile profile(
            WeaponFacts weaponFacts,
            String characterTitle,
            float printedPower,
            boolean hasIHaveYouNow) {
        if (weaponFacts == null || !weaponFacts.hasWeapon()) {
            return HunterProfile.inactive();
        }

        String resolvedTitle = characterTitle != null
                ? characterTitle : "character";
        boolean vader = titleMarksVader(resolvedTitle);
        boolean effectiveIHaveYouNow = vader && hasIHaveYouNow;
        float effectivePower = printedPower
                + (weaponFacts.lightsaber() ? 4.0f : 2.0f)
                + (effectiveIHaveYouNow ? 3.0f : 0.0f);
        return new HunterProfile(
                true, resolvedTitle, vader, effectiveIHaveYouNow,
                weaponFacts.weaponName(), effectivePower);
    }

    public static boolean titleMarksVader(String characterTitle) {
        return characterTitle != null
                && characterTitle.toLowerCase(Locale.ROOT).contains("vader");
    }

    public static Evaluation select(
            HunterProfile profile,
            List<TargetFact> targetsInLocationOrder) {
        if (profile == null || !profile.active()
                || targetsInLocationOrder == null) {
            return Evaluation.none();
        }

        float bestAttackScore = 0.0f;
        TargetFact bestTarget = null;
        boolean foundLuke = false;

        for (TargetFact target : targetsInLocationOrder) {
            if (target == null) {
                continue;
            }

            float attackScore = 60.0f;
            float powerAdvantage = profile.effectivePower()
                    - target.opponentPower();
            if (powerAdvantage >= 6.0f) {
                attackScore += 40.0f;
            } else if (powerAdvantage >= 3.0f) {
                attackScore += 20.0f;
            }
            attackScore += target.opponentIcons() * ICON_BONUS;

            if (target.lukeHere() && profile.vader()) {
                attackScore += 150.0f;
                foundLuke = true;
            }

            if (attackScore > bestAttackScore) {
                bestAttackScore = attackScore;
                bestTarget = target;
            }
        }

        if (bestAttackScore <= 0.0f || bestTarget == null
                || bestTarget.title() == null) {
            return Evaluation.none();
        }

        String reason;
        if (foundLuke) {
            reason = String.format(
                    "V29.7 WEAPON HUNTER: %s + %s should CHALLENGE LUKE at %s! (effective power %.0f)",
                    profile.characterTitle(), profile.weaponName(),
                    bestTarget.title(), profile.effectivePower());
            if (profile.hasIHaveYouNow()) {
                reason += " + IHYN in hand!";
            }
        } else {
            reason = String.format(
                    "V29.7 WEAPON HUNTER: %s + %s should attack %s (effective power %.0f vs opponents)",
                    profile.characterTitle(), profile.weaponName(),
                    bestTarget.title(), profile.effectivePower());
        }

        return new Evaluation(
                true, reason, bestAttackScore, bestTarget.ordinal(),
                bestTarget.title(), profile.effectivePower(), foundLuke);
    }
}
