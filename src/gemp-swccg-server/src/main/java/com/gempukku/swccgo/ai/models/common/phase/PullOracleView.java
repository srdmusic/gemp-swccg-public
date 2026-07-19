package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;

import java.util.List;
import java.util.Set;

/** Bot-neutral, read-only Deck Oracle surface used by the shared PULL facts reader. */
public interface PullOracleView {

    enum Outcome {
        UNKNOWN,
        WILL_FAIL,
        WASTEFUL,
        WILL_SUCCEED
    }

    enum TypedReserveMatch {
        UNRECOGNIZED,
        MISS,
        MATCH
    }

    record Validation(Outcome outcome, String reason) {
    }

    record NamedDeckCard(String title, Zone zone) {
    }

    boolean isAvailable();

    boolean isAnalyzed();

    boolean hasTargetInReserve(String... keywords);

    boolean hasTargetInZone(Zone zone, String target);

    boolean isGenericTypeWord(String word);

    Zone parseSourceZone(String actionText);

    List<String> parseSourceCardPullTargets(String gameText);

    String sourceCardFullGameText(SwccgCardBlueprint blueprint, Side side);

    Validation validatePull(Zone zone, String... keywords);

    Validation validatePullFromSourceCard(Zone zone, String gameText);

    TypedReserveMatch typedReserveMatch(SwccgGame game, String playerId, String noun);

    boolean reserveTargetsAreAllUnattachableWeapons(
            SwccgGame game, String playerId, List<String> targets);

    boolean reservePullFetchesOnlyStarships(String gameText);

    boolean spaceLocationOnTable();

    int countMatchingInDeck(SwccgGame game, String playerId, String noun);

    int countMatchingInHandOrTable(SwccgGame game, String playerId, String noun);

    List<NamedDeckCard> namedDeckCardsInText(String gameText, String sourceBlueprintId);

    boolean personaNamedInText(Set<?> personas, String text);

    default boolean sourceTargetsAnyTitle(List<String> targets, PhysicalCard card) {
        if (card == null || card.getTitle() == null) {
            return false;
        }
        String title = card.getTitle().toLowerCase(java.util.Locale.ROOT);
        for (String target : targets) {
            if (target != null && !target.isEmpty()
                    && (title.contains(target) || target.contains(title))) {
                return true;
            }
        }
        return false;
    }
}
