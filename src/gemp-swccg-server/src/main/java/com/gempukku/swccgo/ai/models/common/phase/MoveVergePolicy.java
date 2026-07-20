package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared V79/V79b Death Star movement classification.
 * Adapters retain all game-state reads, score application, ladder mutation,
 * and logging.
 */
public final class MoveVergePolicy {
    private static final Pattern PARSEC_PATTERN =
            Pattern.compile("parsec\\s+(\\d+)");

    public enum Branch {
        NONE,
        ORBIT_SCARIF,
        PARSEC_SEVEN,
        ONE_HOP_FROM_SCARIF,
        TOWARD_SCARIF,
        WRONG_DIRECTION,
        DEFAULT_MOVE,
        POST_FLIP_HOLD,
        PRE_FLIP_HOLD
    }

    public enum ParsecChoiceBranch {
        NONE,
        PARSEC_SEVEN,
        ONE_HOP_FROM_SCARIF,
        TOWARD_SCARIF,
        WRONG_DIRECTION,
        ORBIT_SCARIF,
        OTHER_DESTINATION,
        FALLBACK_PARSEC
    }

    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record Evaluation(
            Branch branch,
            Contribution contribution,
            Integer destinationParsec,
            String actionLower,
            boolean hardVeto,
            String hardVetoReason) {
        private static Evaluation none() {
            return new Evaluation(
                    Branch.NONE, Contribution.none(), null, "",
                    false, null);
        }
    }

    public record ParsecChoiceEvaluation(
            ParsecChoiceBranch branch,
            Contribution contribution,
            Integer parsec,
            Integer distanceFromScarif) {
        private static ParsecChoiceEvaluation none() {
            return new ParsecChoiceEvaluation(
                    ParsecChoiceBranch.NONE, Contribution.none(),
                    null, null);
        }
    }

    private MoveVergePolicy() {
    }

    public static Evaluation evaluate(
            boolean vergePresent,
            boolean atScarif,
            boolean flipped,
            String displayText) {
        if (!vergePresent) {
            return Evaluation.none();
        }

        if (atScarif) {
            if (flipped) {
                return new Evaluation(
                        Branch.POST_FLIP_HOLD, Contribution.none(), null, "",
                        true,
                        "V79b FLIP-BACK GUARD: objective flipped + Death Star orbiting Scarif"
                                + " — leaving orbit un-satisfies 'Death Star orbiting Scarif'; stay parked");
            }
            return new Evaluation(
                    Branch.PRE_FLIP_HOLD, Contribution.none(), null, "",
                    false, null);
        }

        String actionLower = displayText != null
                ? displayText.toLowerCase(Locale.ROOT) : "";
        if (actionLower.contains("orbit")
                && actionLower.contains("scarif")) {
            return steering(
                    Branch.ORBIT_SCARIF,
                    "V79 DEATH STAR ORBIT SCARIF: arrive at Scarif — must take this!",
                    1500.0f, null, actionLower);
        }

        Matcher matcher = PARSEC_PATTERN.matcher(actionLower);
        Integer destinationParsec = null;
        while (matcher.find()) {
            try {
                destinationParsec = Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {
                // Preserve the last successfully parsed destination.
            }
        }

        if (destinationParsec == null) {
            return steering(
                    Branch.DEFAULT_MOVE,
                    "V79 DEATH STAR MOVE: Verge active, default move",
                    500.0f, null, actionLower);
        }

        int distanceFromScarif = Math.abs(destinationParsec - 7);
        if (distanceFromScarif == 0) {
            return steering(
                    Branch.PARSEC_SEVEN,
                    "V79 DEATH STAR → parsec 7 (Scarif's parsec) — take orbit option next!",
                    1200.0f, destinationParsec, actionLower);
        }
        if (distanceFromScarif == 1) {
            return steering(
                    Branch.ONE_HOP_FROM_SCARIF,
                    "V79 DEATH STAR → parsec " + destinationParsec
                            + " (1 hop from Scarif at 7)",
                    1000.0f, destinationParsec, actionLower);
        }
        if (destinationParsec > 4) {
            return steering(
                    Branch.TOWARD_SCARIF,
                    "V79 DEATH STAR → parsec " + destinationParsec
                            + " (toward Scarif)",
                    700.0f, destinationParsec, actionLower);
        }
        return steering(
                Branch.WRONG_DIRECTION,
                "V79 DEATH STAR → parsec " + destinationParsec
                        + " — WRONG DIRECTION (Scarif is at 7)",
                -300.0f, destinationParsec, actionLower);
    }

    public static ParsecChoiceEvaluation evaluateParsecChoice(
            Integer parsec) {
        if (parsec == null) {
            return ParsecChoiceEvaluation.none();
        }

        int distanceFromScarif = Math.abs(parsec - 7);
        if (distanceFromScarif == 0) {
            return parsecChoice(
                    ParsecChoiceBranch.PARSEC_SEVEN,
                    "V79 PARSEC 7 (Scarif!) — pick this",
                    1500.0f, parsec, distanceFromScarif);
        }
        if (distanceFromScarif == 1) {
            return parsecChoice(
                    ParsecChoiceBranch.ONE_HOP_FROM_SCARIF,
                    "V79 PARSEC " + parsec + " (1 hop from Scarif)",
                    1200.0f, parsec, distanceFromScarif);
        }
        if (parsec > 4) {
            return parsecChoice(
                    ParsecChoiceBranch.TOWARD_SCARIF,
                    "V79 PARSEC " + parsec + " (toward Scarif)",
                    800.0f, parsec, distanceFromScarif);
        }
        return parsecChoice(
                ParsecChoiceBranch.WRONG_DIRECTION,
                "V79 PARSEC " + parsec + " — WRONG DIRECTION",
                -800.0f, parsec, distanceFromScarif);
    }

    public static ParsecChoiceEvaluation evaluateDestinationChoice(
            boolean scarifDestination) {
        if (scarifDestination) {
            return parsecChoice(
                    ParsecChoiceBranch.ORBIT_SCARIF,
                    "V79 ORBIT SCARIF — must take!",
                    1500.0f, null, null);
        }
        return parsecChoice(
                ParsecChoiceBranch.OTHER_DESTINATION,
                "V79 destination not Scarif — avoid",
                -200.0f, null, null);
    }

    public static ParsecChoiceEvaluation evaluateParsecFallback(
            Integer parsec) {
        if (parsec == null) {
            return ParsecChoiceEvaluation.none();
        }

        int distanceFromScarif = Math.abs(parsec - 7);
        float bonus = Math.max(0, 300 - (distanceFromScarif * 50));
        return parsecChoice(
                ParsecChoiceBranch.FALLBACK_PARSEC,
                String.format(
                        "V103 PARSEC FALLBACK: parsec %d (dist %d to Scarif) → +%.0f",
                        parsec, distanceFromScarif, bonus),
                bonus, parsec, distanceFromScarif);
    }

    private static ParsecChoiceEvaluation parsecChoice(
            ParsecChoiceBranch branch,
            String reason,
            float delta,
            Integer parsec,
            Integer distanceFromScarif) {
        return new ParsecChoiceEvaluation(
                branch, new Contribution(true, reason, delta),
                parsec, distanceFromScarif);
    }

    private static Evaluation steering(
            Branch branch,
            String reason,
            float delta,
            Integer destinationParsec,
            String actionLower) {
        return new Evaluation(
                branch, new Contribution(true, reason, delta),
                destinationParsec, actionLower, false, null);
    }
}
