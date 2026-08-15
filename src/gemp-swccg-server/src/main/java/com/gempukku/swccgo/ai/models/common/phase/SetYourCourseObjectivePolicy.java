package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Locale;

/** Pure policy for the classic SYCFA Death Star and CPI Alderaan route. */
public final class SetYourCourseObjectivePolicy {

    public enum Stage {
        INACTIVE_OR_UNSUPPORTED,
        WAITING_FOR_SUPERLASER,
        READY_AT_ZERO,
        READY_AT_ONE,
        RECOVER_AT_TWO_DEEP_SPACE,
        ORBITING_ALDERAAN,
        BROKEN_OR_UNSUPPORTED
    }

    public record RouteFacts(
            boolean frontActive,
            boolean classicPackageSupported,
            boolean deathStarPresent,
            int deathStarParsec,
            String systemOrbited,
            boolean compatibleSuperlaserAccessible,
            boolean compatibleSuperlaserInHand,
            boolean compatibleSuperlaserAttached,
            boolean compatibleSuperlaserDeployableHere,
            boolean classicCpiAccessible,
            boolean alderaanOnTable) {
    }

    public record Evaluation(
            boolean applies,
            boolean mandatory,
            boolean hardVeto,
            float delta,
            String reason) {
        private static Evaluation none() {
            return new Evaluation(false, false, false, 0.0f, "");
        }

        private static Evaluation mandatory(float delta, String reason) {
            return new Evaluation(true, true, false, delta, reason);
        }

        private static Evaluation preferencePenalty(String reason) {
            return new Evaluation(true, false, false, -300.0f, reason);
        }
    }

    private SetYourCourseObjectivePolicy() {
    }

    public static Stage classify(RouteFacts facts) {
        if (facts == null
                || !facts.frontActive()
                || !facts.classicPackageSupported()
                || !facts.deathStarPresent()
                || !facts.compatibleSuperlaserAccessible()
                    && !facts.compatibleSuperlaserAttached()
                || !facts.classicCpiAccessible()
                || !facts.alderaanOnTable()) {
            return Stage.INACTIVE_OR_UNSUPPORTED;
        }
        if (!facts.compatibleSuperlaserAttached()) {
            return facts.compatibleSuperlaserAccessible()
                    && facts.compatibleSuperlaserDeployableHere()
                    ? Stage.WAITING_FOR_SUPERLASER
                    : Stage.BROKEN_OR_UNSUPPORTED;
        }
        if (isAlderaan(facts.systemOrbited())) {
            return Stage.ORBITING_ALDERAAN;
        }
        return switch (facts.deathStarParsec()) {
            case 0 -> Stage.READY_AT_ZERO;
            case 1 -> Stage.READY_AT_ONE;
            case 2 -> Stage.RECOVER_AT_TWO_DEEP_SPACE;
            default -> Stage.BROKEN_OR_UNSUPPORTED;
        };
    }

    public static int nextRouteForceReserve(RouteFacts facts) {
        Stage stage = classify(facts);
        if (stage == Stage.WAITING_FOR_SUPERLASER
                && facts.compatibleSuperlaserInHand()) {
            return isAlderaan(facts.systemOrbited()) ? 0 : 1;
        }
        if (stage == Stage.READY_AT_ZERO
                || stage == Stage.READY_AT_ONE
                || stage == Stage.RECOVER_AT_TWO_DEEP_SPACE) {
            return 1;
        }
        return 0;
    }

    public static Evaluation scoreSuperlaserDeploy(
            Stage stage, boolean exactCompatibleSuperlaser) {
        if (stage != Stage.WAITING_FOR_SUPERLASER
                || !exactCompatibleSuperlaser) {
            return Evaluation.none();
        }
        return Evaluation.mandatory(300.0f,
                "OBJECTIVE.SET_YOUR_COURSE.ARM_DEATH_STAR: attach a CPI-compatible Superlaser before continuing the Alderaan route");
    }

    public static Evaluation scoreDeathStarSitePull(
            boolean exactObjectiveAction, boolean deathStarSite,
            boolean centralCore) {
        if (!exactObjectiveAction || !deathStarSite) {
            return Evaluation.none();
        }
        return Evaluation.mandatory(300.0f,
                "OBJECTIVE.SET_YOUR_COURSE.PACKAGE_PULL: add a Death Star site to improve the classic CPI total");
    }

    public static Evaluation scoreSetupLocationChoice(
            boolean classicRouteOpen, boolean exactAlderaan) {
        if (!classicRouteOpen) return Evaluation.none();
        if (exactAlderaan) {
            return Evaluation.mandatory(300.0f,
                    "OBJECTIVE.SET_YOUR_COURSE.SETUP_ALDERAAN: choose Alderaan so classic CPI can blow away the native flip target");
        }
        return Evaluation.none();
    }

    public static Evaluation scoreMoveParent(
            Stage stage, boolean exactClassicDeathStar) {
        if (!exactClassicDeathStar) return Evaluation.none();
        if (stage == Stage.WAITING_FOR_SUPERLASER) {
            return Evaluation.preferencePenalty(
                    "OBJECTIVE.SET_YOUR_COURSE.LASER_BEFORE_MOVE: arm the Death Star before continuing toward Alderaan");
        }
        if (stage == Stage.ORBITING_ALDERAAN) {
            return Evaluation.preferencePenalty(
                    "OBJECTIVE.SET_YOUR_COURSE.HOLD_ALDERAAN_ORBIT: remain in orbit so classic CPI stays executable");
        }
        if (stage == Stage.READY_AT_ZERO
                || stage == Stage.READY_AT_ONE
                || stage == Stage.RECOVER_AT_TWO_DEEP_SPACE) {
            return Evaluation.mandatory(300.0f,
                    "OBJECTIVE.SET_YOUR_COURSE.MOVE_TO_ALDERAAN: advance the armed Death Star toward Alderaan");
        }
        return Evaluation.none();
    }

    public static Evaluation scoreParsecChoice(Stage stage, Integer parsec) {
        if (parsec == null) return Evaluation.none();
        Integer expected = switch (stage) {
            case READY_AT_ZERO -> 1;
            case READY_AT_ONE, RECOVER_AT_TWO_DEEP_SPACE -> 2;
            default -> null;
        };
        if (expected == null) return Evaluation.none();
        if (parsec.equals(expected)) {
            return Evaluation.mandatory(300.0f,
                    "OBJECTIVE.SET_YOUR_COURSE.PARSEC_TO_ALDERAAN: choose parsec " + expected);
        }
        return Evaluation.preferencePenalty(
                "OBJECTIVE.SET_YOUR_COURSE.PARSEC_WRONG_WAY: prefer the armed Death Star's 0 -> 1 -> 2 route (-300 for another route)");
    }

    public static Evaluation scoreDestinationChoice(
            Stage stage, String actionText) {
        return scoreDestinationChoice(
                stage, "at parsec 2", actionText);
    }

    public static Evaluation scoreDestinationChoice(
            Stage stage, String decisionText, String actionText) {
        if (stage != Stage.READY_AT_ONE
                && stage != Stage.RECOVER_AT_TWO_DEEP_SPACE
                || parseTargetParsec(decisionText) != 2
                || actionText == null) {
            return Evaluation.none();
        }
        String lower = actionText.toLowerCase(Locale.ROOT);
        if (lower.contains("orbit")) {
            return Evaluation.mandatory(300.0f,
                    "OBJECTIVE.SET_YOUR_COURSE.ORBIT_ALDERAAN: enter orbit instead of remaining in deep space");
        }
        return Evaluation.preferencePenalty(
                "OBJECTIVE.SET_YOUR_COURSE.DEEP_SPACE_HOLD_BLOCK: Alderaan orbit is the CPI gate");
    }

    public static Evaluation scoreOrbitSystemChoice(
            Stage stage, boolean exactAlderaan) {
        if (stage != Stage.READY_AT_ONE
                && stage != Stage.RECOVER_AT_TWO_DEEP_SPACE) {
            return Evaluation.none();
        }
        return exactAlderaan
                ? Evaluation.mandatory(300.0f,
                    "OBJECTIVE.SET_YOUR_COURSE.CHOOSE_ALDERAAN: prefer Alderaan for classic CPI (+300 objective preference)")
                : Evaluation.preferencePenalty(
                    "OBJECTIVE.SET_YOUR_COURSE.NON_ALDERAAN_ORBIT_BLOCK: choose Alderaan for the native flip");
    }

    public static Evaluation scoreCpiAction(
            Stage stage, boolean exactClassicCpi,
            String actionText) {
        if (stage != Stage.ORBITING_ALDERAAN
                || !exactClassicCpi || actionText == null
                || !actionText.toLowerCase(Locale.ROOT)
                    .contains("attempt to 'blow away' alderaan")) {
            return Evaluation.none();
        }
        return Evaluation.mandatory(300.0f,
                "OBJECTIVE.SET_YOUR_COURSE.FIRE_CPI: execute the native Alderaan blow-away and flip trigger");
    }

    public static Evaluation preserveRouteForceDuringControl(
            int routeForceReserve, int forceAvailable,
            float actionCost) {
        if (routeForceReserve <= 0 || !Float.isFinite(actionCost)
                || actionCost < 0.0f) {
            return Evaluation.none();
        }
        int exactPayment = (int) Math.ceil(actionCost);
        if (forceAvailable - exactPayment < routeForceReserve) {
            return Evaluation.preferencePenalty(
                    "OBJECTIVE.SET_YOUR_COURSE.CONTROL_FORCE_RESERVE: preserve the exact Force needed for the next Death Star hyperspeed move");
        }
        return Evaluation.none();
    }

    private static boolean isAlderaan(String title) {
        return title != null
                && title.toLowerCase(Locale.ROOT).contains("alderaan");
    }

    private static int parseTargetParsec(String decisionText) {
        if (decisionText == null) return -1;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("parsec\\s+(\\d+)",
                    java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(decisionText);
        if (!matcher.find()) return -1;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
