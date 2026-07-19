package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Locale;

/** Reads candidate and board facts for the shared PULL take-child policy. */
public final class PullTakeCandidateFactsReader {

    public interface ObjectiveView {
        boolean isAnalyzed();

        boolean needsBespinSystemPresence();
    }

    public record Context(
            SwccgGame game,
            GameState gameState,
            String playerId,
            String decisionText,
            int turnNumber,
            int availableForce,
            ObjectiveView objective) {
    }

    private PullTakeCandidateFactsReader() {
    }

    public static PullTakeCandidateFacts read(
            String actionId,
            String cardTitle,
            String blueprintId,
            SwccgCardBlueprint blueprint,
            Context context) {
        String title = cardTitle == null ? "" : cardTitle;
        String titleLower = title.toLowerCase(Locale.ROOT);
        String decisionLower = context != null && context.decisionText() != null
                ? context.decisionText().toLowerCase(Locale.ROOT) : "";

        Float destiny = safeDestiny(blueprint);
        Float power = blueprint != null && blueprint.hasPowerAttribute()
                ? blueprint.getPower() : null;
        Float ability = blueprint != null && blueprint.hasAbilityAttribute()
                ? blueprint.getAbility() : null;
        CardCategory category = blueprint != null ? blueprint.getCardCategory() : null;

        Integer priorityByTitle = null;
        if (!"Unknown".equals(title) && !title.startsWith("Card ")
                && AiPriorityCards.isPriorityCardByTitle(title)) {
            priorityByTitle = AiPriorityCards.getProtectionScoreByTitle(title);
        }
        Integer priorityByBlueprint = null;
        if (priorityByTitle == null && blueprintId != null
                && AiPriorityCards.isPriorityCard(blueprintId)) {
            priorityByBlueprint = AiPriorityCards.getProtectionScore(blueprintId);
        }

        boolean admiralPull = decisionLower.contains("admiral")
                && decisionLower.contains("reserve");
        if (!admiralPull) {
            admiralPull = containsAny(titleLower,
                    "admiral", "piett", "chiraneau", "ozzel", "motti", "firmus");
        }
        boolean commanderPull = (decisionLower.contains("commander")
                || decisionLower.contains("admiral's order"))
                && decisionLower.contains("reserve");

        ObjectiveView objective = context != null ? context.objective() : null;
        boolean needsBespin = objective != null && objective.isAnalyzed()
                && objective.needsBespinSystemPresence();
        BoardSupport support = needsBespin && titleLower.contains("lando")
                ? boardSupport(context) : BoardSupport.none();

        return new PullTakeCandidateFacts(
                actionId,
                title,
                blueprintId,
                destiny,
                power,
                ability,
                category,
                context != null ? context.turnNumber() : 0,
                priorityByTitle,
                priorityByBlueprint,
                admiralPull,
                commanderPull,
                needsBespin,
                support.friendlyAtCloudCity(),
                support.handBuddy(),
                support.availableForce(),
                isDownloadLocationEnabler(blueprint));
    }

    private static Float safeDestiny(SwccgCardBlueprint blueprint) {
        if (blueprint == null) {
            return null;
        }
        try {
            return blueprint.getDestiny();
        } catch (UnsupportedOperationException ignored) {
            return null;
        }
    }

    private static BoardSupport boardSupport(Context context) {
        if (context == null || context.gameState() == null || context.playerId() == null) {
            return BoardSupport.none();
        }
        GameState gameState = context.gameState();
        String playerId = context.playerId();
        boolean friendlyAtCloudCity = false;
        try {
            for (PhysicalCard location : gameState.getLocationsInOrder()) {
                if (location == null || location.getTitle() == null
                        || !isCloudCityLocation(location.getTitle())) {
                    continue;
                }
                List<PhysicalCard> cards = gameState.getCardsAtLocation(location);
                if (cards == null) {
                    continue;
                }
                for (PhysicalCard card : cards) {
                    if (card != null && playerId.equals(card.getOwner())
                            && card.getBlueprint() != null
                            && card.getBlueprint().getCardCategory()
                            == CardCategory.CHARACTER) {
                        friendlyAtCloudCity = true;
                        break;
                    }
                }
                if (friendlyAtCloudCity) {
                    break;
                }
            }
        } catch (Exception ignored) {
            // Preserve the predecessor's fail-open board scan.
        }

        int force = 0;
        boolean handBuddy = false;
        if (!friendlyAtCloudCity) {
            try {
                force = context.availableForce();
                List<PhysicalCard> hand = gameState.getHand(playerId);
                if (hand != null) {
                    for (PhysicalCard card : hand) {
                        if (card != null && card.getBlueprint() != null
                                && card.getBlueprint().getCardCategory()
                                == CardCategory.CHARACTER) {
                            handBuddy = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Preserve the predecessor's fail-open hand scan.
            }
        }
        return new BoardSupport(friendlyAtCloudCity, handBuddy, force);
    }

    private static boolean isCloudCityLocation(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        return containsAny(lower, "cloud city", "upper walkway", "carbonite",
                "security tower", "dining room", "platform", "lower corridor");
    }

    private static boolean isDownloadLocationEnabler(SwccgCardBlueprint blueprint) {
        if (blueprint == null) {
            return false;
        }
        StringBuilder text = new StringBuilder();
        append(text, safeText(blueprint, TextKind.BASE));
        append(text, safeText(blueprint, TextKind.LIGHT));
        append(text, safeText(blueprint, TextKind.DARK));
        String lower = text.toString().toLowerCase(Locale.ROOT);
        return lower.contains("[download]")
                && containsAny(lower, "site", "location", "system", "battleground");
    }

    private static String safeText(SwccgCardBlueprint blueprint, TextKind kind) {
        try {
            return switch (kind) {
                case BASE -> blueprint.getGameText();
                case LIGHT -> blueprint.getLocationLightSideGameText();
                case DARK -> blueprint.getLocationDarkSideGameText();
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void append(StringBuilder target, String value) {
        if (value != null) {
            target.append(value).append(' ');
        }
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private enum TextKind {
        BASE,
        LIGHT,
        DARK
    }

    private record BoardSupport(
            boolean friendlyAtCloudCity,
            boolean handBuddy,
            int availableForce) {
        private static BoardSupport none() {
            return new BoardSupport(false, false, 0);
        }
    }
}
