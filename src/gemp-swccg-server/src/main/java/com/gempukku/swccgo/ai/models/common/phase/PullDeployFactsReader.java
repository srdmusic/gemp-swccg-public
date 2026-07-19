package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads DeployEvaluator-side stock state into immutable PULL guard facts. */
public final class PullDeployFactsReader {

    private static final Pattern NAMED_TARGET = Pattern.compile(
            "Deploy ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) from Reserve Deck");
    private static final Pattern GENERIC_TARGET = Pattern.compile(
            "Deploy an? ([a-z][a-z ]*?) from Reserve Deck");
    private static final Pattern MEMORY_NAMED_TARGET = Pattern.compile(
            "(?:Deploy|Take) ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) "
                    + "(?:from Reserve|from Lost|from Used|from Force|into hand from)");
    private static final Pattern MEMORY_GENERIC_TARGET = Pattern.compile(
            "(?:Deploy|Take) an? ([a-z]+) (?:from|into hand from)");
    private static final Set<String> MEMORY_STOPWORDS = Set.of(
            "location", "site", "battleground", "system", "sector",
            "ship", "starship", "vehicle", "transport", "fighter",
            "weapon", "lightsaber", "blaster", "bowcaster", "device",
            "character", "alien", "droid", "jedi", "sith", "padawan",
            "inquisitor", "senator", "pilot", "warrior", "soldier",
            "leader", "admiral", "general", "trooper", "officer",
            "rebel", "imperial", "scout", "spy",
            "effect", "interrupt", "objective", "epic", "shield", "card");

    public record Context(
            SwccgGame game,
            GameState gameState,
            String playerId,
            Side side,
            PullOracleView oracle) {
    }

    private PullDeployFactsReader() {
    }

    public static PullDeployFacts read(
            String actionId,
            String actionText,
            String sourceCardId,
            Context context) {
        String text = actionText == null ? "" : actionText;
        PullOracleView oracle = context != null ? context.oracle() : null;
        GameState gameState = context != null ? context.gameState() : null;
        String playerId = context != null ? context.playerId() : null;
        int reserveSize = -1;
        try {
            if (gameState != null && playerId != null) {
                reserveSize = gameState.getReserveDeckSize(playerId);
            }
        } catch (Exception ignored) {
            // Fail open.
        }

        PullOracleView.Validation memory = unknownValidation();
        PullOracleView.Validation sourceValidation = unknownValidation();
        if (reserveSize >= 0 && reserveSize <= 2) {
            return facts(actionId, text, reserveSize, "", "", "",
                    memory, sourceValidation, "?", false, false);
        }

        String namedMissing = "";
        if (oracle != null && oracle.isAvailable()) {
            try {
                Matcher named = NAMED_TARGET.matcher(text);
                if (named.find()) {
                    String target = named.group(1).trim();
                    if (!oracle.hasTargetInReserve(target.split(" "))) {
                        namedMissing = target;
                    }
                }
            } catch (Exception ignored) {
                // Fail open.
            }
        }
        if (!namedMissing.isEmpty()) {
            return facts(actionId, text, reserveSize, namedMissing, "", "",
                    memory, sourceValidation, "?", false, false);
        }

        String genericTypedMiss = "";
        String genericUntypedMiss = "";
        if (oracle != null && oracle.isAvailable()) {
            try {
                Matcher generic = GENERIC_TARGET.matcher(text);
                if (generic.find()) {
                    String noun = generic.group(1).trim();
                    PullOracleView.TypedReserveMatch typed = context != null
                            ? oracle.typedReserveMatch(context.game(), playerId, noun)
                            : PullOracleView.TypedReserveMatch.UNRECOGNIZED;
                    if (typed == PullOracleView.TypedReserveMatch.MISS) {
                        genericTypedMiss = noun;
                    } else if (typed == PullOracleView.TypedReserveMatch.UNRECOGNIZED
                            && noun.length() >= 3
                            && !oracle.hasTargetInReserve(noun)) {
                        genericUntypedMiss = noun;
                    }
                }
            } catch (Exception ignored) {
                // Fail open.
            }
        }
        if (!genericTypedMiss.isEmpty() || !genericUntypedMiss.isEmpty()) {
            return facts(actionId, text, reserveSize, "", genericTypedMiss,
                    genericUntypedMiss, memory, sourceValidation, "?", false,
                    false);
        }

        memory = memoryValidation(text, oracle);
        if (memory.outcome() == PullOracleView.Outcome.WILL_FAIL) {
            return facts(actionId, text, reserveSize, "", "", "", memory,
                    sourceValidation, "?", false, false);
        }

        PhysicalCard source = sourceCard(gameState, sourceCardId);
        String sourceTitle = sourceTitle(source);
        String sourceText = sourceText(source, context, oracle);
        Zone sourceZone = sourceZone(text, oracle);
        if (oracle != null && oracle.isAnalyzed()
                && sourceText != null && sourceZone != null) {
            sourceValidation = sourceValidation(sourceZone, sourceText, oracle);
        }
        if (sourceValidation.outcome() == PullOracleView.Outcome.WILL_FAIL) {
            return facts(actionId, text, reserveSize, "", "", "", memory,
                    sourceValidation, sourceTitle, false, false);
        }

        List<String> targets = pullTargets(sourceText, oracle);
        boolean noWeaponHolder = sourceValidation.outcome()
                == PullOracleView.Outcome.WILL_SUCCEED
                && context != null && context.game() != null && playerId != null
                && oracle != null && safeNoWeaponHolder(
                        oracle, context.game(), playerId, targets);
        if (noWeaponHolder) {
            return facts(actionId, text, reserveSize, "", "", "", memory,
                    sourceValidation, sourceTitle, true, false);
        }

        boolean starshipOnlyNoSpace = sourceValidation.outcome()
                == PullOracleView.Outcome.WILL_SUCCEED
                && sourceText != null && oracle != null
                && safeStarshipOnlyNoSpace(oracle, sourceText);

        return facts(actionId, text, reserveSize, "", "", "", memory,
                sourceValidation, sourceTitle, false, starshipOnlyNoSpace);
    }

    private static PullDeployFacts facts(
            String actionId,
            String text,
            int reserveSize,
            String namedMissing,
            String genericTypedMiss,
            String genericUntypedMiss,
            PullOracleView.Validation memory,
            PullOracleView.Validation sourceValidation,
            String sourceTitle,
            boolean noWeaponHolder,
            boolean starshipOnlyNoSpace) {
        return new PullDeployFacts(
                actionId, text, false, reserveSize, namedMissing,
                genericTypedMiss, genericUntypedMiss, memory,
                sourceValidation, sourceTitle, noWeaponHolder,
                starshipOnlyNoSpace);
    }

    private static String sourceTitle(PhysicalCard source) {
        try {
            return source != null && source.getTitle() != null
                    ? source.getTitle() : "?";
        } catch (Exception ignored) {
            return "?";
        }
    }

    private static String sourceText(
            PhysicalCard source, Context context, PullOracleView oracle) {
        if (source == null || context == null || oracle == null) {
            return null;
        }
        try {
            return source.getBlueprint() != null
                    ? oracle.sourceCardFullGameText(
                            source.getBlueprint(), context.side())
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Zone sourceZone(String text, PullOracleView oracle) {
        if (oracle == null) {
            return null;
        }
        try {
            return oracle.parseSourceZone(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PullOracleView.Validation sourceValidation(
            Zone zone, String sourceText, PullOracleView oracle) {
        try {
            PullOracleView.Validation validation =
                    oracle.validatePullFromSourceCard(zone, sourceText);
            return validation != null ? validation : unknownValidation();
        } catch (Exception ignored) {
            return unknownValidation();
        }
    }

    private static List<String> pullTargets(
            String sourceText, PullOracleView oracle) {
        if (sourceText == null || oracle == null) {
            return List.of();
        }
        try {
            List<String> targets = oracle.parseSourceCardPullTargets(sourceText);
            return targets != null ? targets : List.of();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static boolean safeNoWeaponHolder(
            PullOracleView oracle,
            SwccgGame game,
            String playerId,
            List<String> targets) {
        try {
            return oracle.reserveTargetsAreAllUnattachableWeapons(
                    game, playerId, targets);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean safeStarshipOnlyNoSpace(
            PullOracleView oracle, String sourceText) {
        try {
            return oracle.reservePullFetchesOnlyStarships(sourceText)
                    && !oracle.spaceLocationOnTable();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static PullOracleView.Validation memoryValidation(
            String actionText, PullOracleView oracle) {
        if (oracle == null || !oracle.isAnalyzed()) {
            return unknownValidation();
        }
        Zone zone = oracle.parseSourceZone(actionText);
        if (zone == null) {
            return unknownValidation();
        }
        String[] keywords = null;
        Matcher named = MEMORY_NAMED_TARGET.matcher(actionText);
        if (named.find()) {
            keywords = named.group(1).trim().split(" ");
        } else {
            Matcher generic = MEMORY_GENERIC_TARGET.matcher(actionText);
            if (generic.find()) {
                String keyword = generic.group(1).trim();
                if (!MEMORY_STOPWORDS.contains(keyword.toLowerCase(Locale.ROOT))
                        && keyword.length() >= 3) {
                    keywords = new String[]{keyword};
                }
            }
        }
        if (keywords == null) {
            return unknownValidation();
        }
        try {
            PullOracleView.Validation validation = oracle.validatePull(zone, keywords);
            return validation != null ? validation : unknownValidation();
        } catch (Exception ignored) {
            return unknownValidation();
        }
    }

    private static PullOracleView.Validation unknownValidation() {
        return new PullOracleView.Validation(PullOracleView.Outcome.UNKNOWN, "");
    }

    private static PhysicalCard sourceCard(GameState gameState, String sourceCardId) {
        if (gameState == null || sourceCardId == null) {
            return null;
        }
        try {
            return gameState.findCardById(Integer.parseInt(sourceCardId));
        } catch (Exception ignored) {
            return null;
        }
    }
}
