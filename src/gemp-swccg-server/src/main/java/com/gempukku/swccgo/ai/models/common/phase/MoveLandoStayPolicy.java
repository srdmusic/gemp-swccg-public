package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Locale;

/**
 * Shared V47 Lando-at-Cloud-City stay policy.
 * Adapters retain objective, power, weapon, score application, and logging reads.
 */
public final class MoveLandoStayPolicy {
    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record DestinationEvaluation(
            Contribution support,
            Contribution stay) {
        public float totalDelta() {
            return support.delta() + stay.delta();
        }
    }

    public record Evaluation(boolean hardVeto, String reason) {
        private static Evaluation none() {
            return new Evaluation(false, null);
        }
    }

    private MoveLandoStayPolicy() {
    }

    public static boolean titleMarksLando(String cardTitle) {
        return cardTitle != null
                && cardTitle.toLowerCase(Locale.ROOT).contains("lando");
    }

    public static boolean isCloudCitySite(String locationTitle) {
        if (locationTitle == null) {
            return false;
        }
        String lower = locationTitle.toLowerCase(Locale.ROOT);
        return lower.contains("cloud city")
                || lower.contains("dining room")
                || lower.contains("upper walkway")
                || lower.contains("carbonite")
                || lower.contains("security tower")
                || lower.contains("lower corridor");
    }

    public static Evaluation evaluate(
            String locationTitle,
            boolean objectiveWantsLandoHere,
            boolean survivable) {
        if (!objectiveWantsLandoHere || !survivable) {
            return Evaluation.none();
        }
        return new Evaluation(
                true,
                "V47 LANDO STAY: Lando at " + locationTitle
                        + " — stay for occupation! Don't move!");
    }

    public static DestinationEvaluation destination(
            boolean landoAtDestination,
            int friendlyCharacterCount,
            boolean landoMover,
            boolean bespinPresenceObjective) {
        return new DestinationEvaluation(
                destinationSupport(
                        landoAtDestination, friendlyCharacterCount),
                destinationStay(landoMover, bespinPresenceObjective));
    }

    public static Contribution destinationSupport(
            boolean landoAtDestination,
            int friendlyCharacterCount) {
        if (landoAtDestination && friendlyCharacterCount == 1) {
            return new Contribution(
                    true,
                    "V24.13 LANDO SUPPORT: Lando is ALONE here — move to protect him!",
                    250.0f);
        }
        return Contribution.none();
    }

    public static Contribution destinationStay(
            boolean landoMover,
            boolean bespinPresenceObjective) {
        if (landoMover && bespinPresenceObjective) {
            return new Contribution(
                    true,
                    "V47 LANDO STAY: Lando should stay put — moving wastes force and loses occupation!",
                    -9999.0f);
        }
        return Contribution.none();
    }
}
