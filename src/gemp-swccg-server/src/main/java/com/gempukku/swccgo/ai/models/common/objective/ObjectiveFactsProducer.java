package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Produces one deeply immutable objective view for one mediated decision. */
public final class ObjectiveFactsProducer {

    private static final String PRODUCER = ObjectiveFacts.PRODUCER;

    private ObjectiveFactsProducer() {
    }

    public static ObjectiveFacts produce(SwccgGame game,
                                         GameState gameState,
                                         String playerId,
                                         ObjectiveFactsSource source) {
        if (game == null || gameState == null || playerId == null || playerId.isBlank()
                || source == null) {
            return ObjectiveFacts.unknown("game, state, player, or analyzer source is unavailable");
        }

        try {
            PhysicalCard objective = findObjective(gameState, playerId);
            if (objective == null) {
                return ObjectiveFacts.unknown("no physical objective card is present for the deciding player");
            }

            FactValue<ObjectiveFacts.Identity> identity = projectIdentity(objective);
            if (identity.isUnknown()) {
                return new ObjectiveFacts(
                        identity,
                        unknown("objective profile", "physical objective identity is incomplete"),
                        unknown("objective strategy", "physical objective identity is incomplete"),
                        unknown("typed objective board scan", "physical objective identity is incomplete"));
            }

            if (!source.isAnalyzed()) {
                return new ObjectiveFacts(
                        identity,
                        unknown("objective profile", "ObjectiveAnalyzer was not prepared at the decision boundary"),
                        unknown("objective strategy", "ObjectiveAnalyzer was not prepared at the decision boundary"),
                        unknown("typed objective board scan", "ObjectiveAnalyzer was not prepared at the decision boundary"));
            }

            ObjectiveFacts.StrategyFacts strategy = strategy(game, playerId, identity.value(), source);
            ObjectiveFacts.TypedBoardFacts board = board(
                    game, gameState, playerId, objective, strategy, source);
            return new ObjectiveFacts(
                    identity,
                    FactValue.known(source.getProfileResolution(), PRODUCER,
                            "ObjectiveAnalyzer blueprint-first profile resolution"),
                    FactValue.known(strategy, PRODUCER, "ObjectiveAnalyzer immutable projection"),
                    FactValue.known(board, PRODUCER, "typed Filters and GameConditions board scan"));
        } catch (RuntimeException e) {
            return ObjectiveFacts.unknown("objective fact production failed: " + detail(e));
        }
    }

    static FactValue<ObjectiveFacts.Identity> projectIdentity(PhysicalCard objective) {
        try {
            if (!objective.isDoubleSided()) {
                return unknown("physical objective", "objective is not double-sided");
            }
            SwccgCardBlueprint current = objective.getBlueprint();
            SwccgCardBlueprint opposite = objective.getOtherSideBlueprint();
            String currentId = objective.getBlueprintId(false);
            String oppositeId = objective.getOtherSideBlueprintId();
            String frontId = objective.getBlueprintId(true);
            if (current == null || opposite == null || currentId == null
                    || oppositeId == null || frontId == null) {
                return unknown("physical objective", "current/opposite blueprint identity is incomplete");
            }
            boolean flipped = objective.isFlipped();
            SwccgCardBlueprint front = flipped ? opposite : current;
            SwccgCardBlueprint back = flipped ? current : opposite;
            String backId = flipped ? currentId : oppositeId;
            ObjectiveFacts.Identity facts = new ObjectiveFacts.Identity(
                    objective.getPermanentCardId(),
                    objective.getCardId(),
                    frontId,
                    backId,
                    currentId,
                    oppositeId,
                    front.getTitle(),
                    back.getTitle(),
                    current.getTitle(),
                    opposite.getTitle(),
                    front.getGameText(),
                    back.getGameText(),
                    current.getGameText(),
                    opposite.getGameText(),
                    flipped);
            return FactValue.known(facts, PRODUCER,
                    "PhysicalCard.getBlueprint/getOtherSideBlueprint + exact ids");
        } catch (RuntimeException e) {
            return unknown("physical objective", "identity read failed: " + detail(e));
        }
    }

    private static ObjectiveFacts.StrategyFacts strategy(SwccgGame game,
                                                          String playerId,
                                                          ObjectiveFacts.Identity identity,
                                                          ObjectiveFactsSource source) {
        String frontId = identity.canonicalFrontBlueprintId();
        String frontTitle = identity.canonicalFrontTitle().toLowerCase(Locale.ROOT);
        ObjectiveFacts.ObjectiveKind kind = new ObjectiveFacts.ObjectiveKind(
                source.isMyLord(),
                source.isEndorOperations(),
                source.isInvasion(),
                idOrTitle(frontId, frontTitle, "226_28", "the hidden path"),
                source.isHuntDownV(),
                source.isWantThatMap());
        return new ObjectiveFacts.StrategyFacts(
                kind,
                source.getFlipConditionLocationFragments(),
                source.getFlipBackLocationFragments(),
                source.getRequiredCardsOnTable(),
                source.getPullableCards(),
                new ObjectiveFacts.StartingRefs(
                        source.getStartingLocationIds(),
                        source.getStartingLocationFragments(),
                        source.getStartingEffectIds(),
                        source.getStartingEffectFragments(),
                        source.getStartingInterruptIds(),
                        source.getStartingInterruptFragments()),
                source.getFlipCriticalControlCardIds(),
                optional(source.getFlipCriticalControlSite(), "flip-critical control site"),
                optional(source.getFlipCriticalControlCard(), "flip-critical control card"),
                optional(source.getFlipConditionText(), "front-side flip condition"),
                optional(source.getFlipBackConditionText(), "opposite-side flip-back condition"),
                source.requiresOccupy(),
                source.requiresControl(),
                source.flipBackRequiresOccupy(),
                source.flipBackRequiresControl(),
                source.getStrategyCharacterTokens(game, playerId));
    }

    private static ObjectiveFacts.TypedBoardFacts board(SwccgGame game,
                                                        GameState gameState,
                                                        String playerId,
                                                        PhysicalCard objective,
                                                        ObjectiveFacts.StrategyFacts strategy,
                                                        ObjectiveFactsSource source) {
        Set<Integer> senators = new LinkedHashSet<>();
        Set<Integer> survivors = new LinkedHashSet<>();
        Set<Integer> inquisitors = new LinkedHashSet<>();
        Set<Integer> inquisitorsWithHatred = new LinkedHashSet<>();
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null || !playerId.equals(card.getOwner())) {
                continue;
            }
            if (Filters.senator.accepts(gameState, game.getModifiersQuerying(), card)) {
                senators.add(card.getCardId());
            }
            if (Filters.Jedi_Survivor.accepts(gameState, game.getModifiersQuerying(), card)) {
                survivors.add(card.getCardId());
            }
            if (Filters.inquisitor.accepts(gameState, game.getModifiersQuerying(), card)) {
                inquisitors.add(card.getCardId());
                if (Filters.hasStacked(Filters.hatredCard).accepts(
                        gameState, game.getModifiersQuerying(), card)) {
                    inquisitorsWithHatred.add(card.getCardId());
                }
            }
        }

        Set<Integer> senateLocations = new LinkedHashSet<>();
        Set<Integer> objectiveLocations = new LinkedHashSet<>();
        Set<Integer> flipBackLocations = new LinkedHashSet<>();
        boolean nonSenateSite = false;
        boolean controlsFlipCritical = false;
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null || location.getBlueprint() == null) {
                continue;
            }
            boolean senate = Filters.Galactic_Senate.accepts(
                    gameState, game.getModifiersQuerying(), location);
            if (senate) {
                senateLocations.add(location.getCardId());
            } else if (location.getBlueprint().getCardCategory() == CardCategory.LOCATION
                    && location.getBlueprint().getCardSubtype() == CardSubtype.SITE) {
                nonSenateSite = true;
            }
            if (source.isObjectiveRelevantLocation(location, game, playerId)) {
                objectiveLocations.add(location.getCardId());
            }
            if (source.isFlipBackProtectionLocation(location.getTitle())) {
                flipBackLocations.add(location.getCardId());
            }
            if (strategy.flipCriticalControlSite().isKnown()
                    && location.getTitle() != null
                    && location.getTitle().toLowerCase(Locale.ROOT)
                    .contains(strategy.flipCriticalControlSite().value().toLowerCase(Locale.ROOT))
                    && game.getModifiersQuerying().controlsLocation(gameState, location, playerId,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE)) {
                controlsFlipCritical = true;
            }
        }

        int hiddenPathSites = countHiddenPathSites(game, gameState, playerId, objective);
        boolean invasionThrone = GameConditions.controlsWith(
                game, objective, playerId, Filters.Theed_Palace_Throne_Room,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.Neimoidian);
        boolean invasionNaboo = GameConditions.controls(
                game, playerId, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.Naboo_system);

        return new ObjectiveFacts.TypedBoardFacts(
                senators,
                survivors,
                inquisitors,
                inquisitorsWithHatred,
                senateLocations,
                objectiveLocations,
                flipBackLocations,
                nonSenateSite,
                hiddenPathSites,
                hiddenPathSites >= 2,
                invasionThrone,
                invasionNaboo,
                controlsFlipCritical);
    }

    private static int countHiddenPathSites(SwccgGame game,
                                            GameState gameState,
                                            String playerId,
                                            PhysicalCard objective) {
        int count = 0;
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null
                    || !Filters.site.accepts(gameState, game.getModifiersQuerying(), location)
                    || Filters.Mapuzo_location.accepts(gameState, game.getModifiersQuerying(), location)) {
                continue;
            }
            if (GameConditions.occupiesWith(game, objective, playerId,
                    Filters.sameCardId(location), SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE,
                    Filters.Jedi)) {
                count++;
            }
        }
        return count;
    }

    private static PhysicalCard findObjective(GameState gameState, String playerId) {
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null || !playerId.equals(card.getOwner())) {
                continue;
            }
            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (blueprint != null && blueprint.getCardCategory() == CardCategory.OBJECTIVE) {
                return card;
            }
        }
        return null;
    }

    private static boolean idOrTitle(String blueprintId,
                                     String titleLower,
                                     String expectedId,
                                     String titleFragment) {
        String id = blueprintId != null
                ? blueprintId.replace("_BACK", "").replace("*", "").replace("^", "")
                : "";
        return Objects.equals(id, expectedId) || titleLower.contains(titleFragment);
    }

    private static FactValue<String> optional(String value, String provenance) {
        return value != null && !value.isBlank()
                ? FactValue.known(value, PRODUCER, provenance)
                : FactValue.unknown(PRODUCER, provenance, "objective does not define this fact");
    }

    private static <T> FactValue<T> unknown(String provenance, String reason) {
        return FactValue.unknown(PRODUCER, provenance, reason);
    }

    private static String detail(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
