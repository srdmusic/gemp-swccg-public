package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Locale;

/**
 * Shared V47 Lando-at-Cloud-City stay policy.
 * Adapters retain objective, power, weapon, score application, and logging reads.
 */
public final class MoveLandoStayPolicy {
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
}
