package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.PullActionFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullActionFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployCandidateFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullOracleView;
import com.gempukku.swccgo.ai.models.common.phase.PullTakeCandidateFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullTakeCandidateFactsReader;
import com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle;
import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Facts-only bridge from bot compatibility services to shared PULL policies. */
final class PullPolicyAdapter {

    private PullPolicyAdapter() {
    }

    static PullActionFacts.EarlySearch readEarlySearch(
            DecisionContext context,
            String actionId,
            String actionText,
            String sourceCardId) {
        return PullActionFactsReader.readEarlySearch(
                actionId, actionText, sourceCardId, actionContext(context));
    }

    static PullActionFacts.Parent readParent(
            DecisionContext context,
            String actionId,
            String actionText,
            String sourceCardId) {
        return PullActionFactsReader.readParent(
                actionId, actionText, sourceCardId, actionContext(context));
    }

    static PullDeployFacts readDeploy(
            DecisionContext context,
            String actionId,
            String actionText,
            String sourceCardId) {
        return PullDeployFactsReader.read(
                actionId,
                actionText,
                sourceCardId,
                new PullDeployFactsReader.Context(
                        context.getGame(),
                        context.getGameState(),
                        context.getPlayerId(),
                        context.getSide(),
                        new OracleView(
                            context.getDeckOracle(),
                            context.getGameState()),
                        objectiveView(
                            context.getObjectiveAnalyzer())));
    }

    static PullTakeCandidateFacts readTakeCandidate(
            DecisionContext context,
            String actionId,
            String cardTitle,
            String blueprintId,
            SwccgCardBlueprint blueprint) {
        return PullTakeCandidateFactsReader.read(
                actionId,
                cardTitle,
                blueprintId,
                blueprint,
                new PullTakeCandidateFactsReader.Context(
                        context.getGame(),
                        context.getGameState(),
                        context.getPlayerId(),
                        context.getDecisionText(),
                        context.getTurnNumber(),
                        context.getForcePileSize(),
                        takeObjectiveView(context.getObjectiveAnalyzer())));
    }

    static PullDeployCandidateFacts readDeployCandidate(
            DecisionContext context,
            String actionId,
            String candidateCardId,
            String blueprintId,
            String displayTitle,
            com.gempukku.swccgo.common.CardCategory category,
            SwccgCardBlueprint blueprint) {
        String weaponDeviceBlock =
                CardSelectionEvaluator.v70CheckWeaponDeviceBlock(
                        context.getGame(), context.getPlayerId(),
                        category, blueprint);
        if (weaponDeviceBlock != null
                && isReadyRequiredObjectiveCandidate(
                    context, candidateCardId,
                    blueprintId)) {
            weaponDeviceBlock = null;
        }
        return new PullDeployCandidateFacts(
                actionId,
                displayTitle,
                weaponDeviceBlock);
    }

    private static boolean isReadyRequiredObjectiveCandidate(
            DecisionContext context,
            String candidateCardId,
            String blueprintId) {
        if (context == null || candidateCardId == null
                || context.getGame() == null
                || context.getGameState() == null
                || context.getPlayerId() == null
                || context.getObjectiveAnalyzer() == null
                || !context.getObjectiveAnalyzer().isAnalyzed()
                || context.getObjectiveAnalyzer().isFlipped()) {
            return false;
        }
        PhysicalCard candidate =
                CardSelectionEvaluator.findPullCandidate(
                    context,
                    candidateCardId,
                    blueprintId);
        return candidate != null
                && context.getObjectiveAnalyzer()
                    .isRequiredCardForFlip(candidate)
                && context.getObjectiveAnalyzer()
                    .isRequiredOnTableCardPullRouteReady(
                        context.getGame(),
                        context.getPlayerId(),
                        candidate);
    }

    private static PullActionFactsReader.Context actionContext(DecisionContext context) {
        return new PullActionFactsReader.Context(
                context.getGame(),
                context.getGameState(),
                context.getPlayerId(),
                context.getSide(),
                context.getPhase(),
                new OracleView(context.getDeckOracle(), context.getGameState()),
                objectiveView(context.getObjectiveAnalyzer()),
                new PullActionFactsReader.LateView() {
                    @Override
                    public List<com.gempukku.swccgo.game.PhysicalCard> hand() {
                        return context.getHand();
                    }

                    @Override
                    public boolean battlePlausible() {
                        return context.isBattlePlausibleThisTurn();
                    }
                });
    }

    private static PullActionFactsReader.ObjectiveView objectiveView(
            ObjectiveAnalyzer objective) {
        if (objective == null) {
            return null;
        }
        return new PullActionFactsReader.ObjectiveView() {
            @Override
            public boolean isAnalyzed() {
                return objective.isAnalyzed();
            }

            @Override
            public boolean isFlipped() {
                return objective.isFlipped();
            }

            @Override
            public String flipConditionText() {
                return objective.getFlipConditionText();
            }

            @Override
            public Set<String> strategyCharacterTokens(
                    SwccgGame game, String playerId) {
                return objective.getStrategyCharacterTokens(game, playerId);
            }

            @Override
            public boolean hasTypedStrategyKeyCharacter() {
                return objective.hasTypedStrategyKeyCharacter();
            }

            @Override
            public boolean isStrategyKeyCharacter(
                    SwccgGame game, String playerId,
                    PhysicalCard candidate) {
                return objective.isStrategyKeyCharacter(
                        game, playerId, candidate);
            }

            @Override
            public boolean objectivePullAdvancesRequiredOnTableCard(
                    SwccgGame game, String playerId,
                    PhysicalCard source) {
                return objective
                        .objectivePullAdvancesRequiredOnTableCard(
                                game, playerId, source);
            }

            @Override
            public boolean objectiveRoutePullVetoBypass(
                    SwccgGame game, String playerId,
                    PhysicalCard source, String actionText) {
                boolean firstOrderRoute = objective
                        .isFirstOrderReignsDownloadAction(
                                source, actionText)
                        && objective
                            .hasFirstOrderReignsRouteProgressCandidateInReserve(
                                game, playerId);
                boolean imperialEntanglementsBackSiteRoute = objective
                        .isImperialEntanglementsBackSiteRouteAction(
                                game, playerId, source, actionText);
                boolean massassiFrontSiteRoute = objective
                        .isMassassiFrontSiteRouteAction(
                                game, playerId, source, actionText);
                boolean oldAlliesFrontLocationRoute = objective
                        .isOldAlliesFrontLocationRouteAction(
                                game, playerId, source, actionText);
                boolean countedOperativeSiteRoute = objective
                        .isCountedOperativeSiteRouteAction(
                                game, playerId, source, actionText);
                boolean noMoneyWattoRoute = objective
                        .isNoMoneyNoPartsWattoPullAction(
                                game, playerId, source, actionText);
                boolean bringHimBeforeMeEmperorRoute =
                        CaptureObjectiveFacts.objectiveKind(objective)
                            == CaptureObjectivePolicy.ObjectiveKind.BHBM
                        && CaptureObjectiveFacts.isOwnedExactSource(
                            source, playerId, "9_151")
                        && actionText != null
                        && "deploy emperor from reserve deck".equals(
                            actionText.trim().toLowerCase(Locale.ROOT))
                        && CaptureObjectiveFacts
                            .canAffordBhbmEmperorDownload(
                                game, playerId,
                                objective, source);
                return firstOrderRoute
                        || massassiFrontSiteRoute
                        || oldAlliesFrontLocationRoute
                        || imperialEntanglementsBackSiteRoute
                        || countedOperativeSiteRoute
                        || noMoneyWattoRoute
                        || bringHimBeforeMeEmperorRoute;
            }

            @Override
            public boolean objectiveRoutePullOracleValidationBypass(
                    SwccgGame game, String playerId,
                    PhysicalCard source, String actionText) {
                return objective.isCountedOperativeSiteRouteAction(
                            game, playerId, source, actionText)
                        || objective.isNoMoneyNoPartsWattoPullAction(
                            game, playerId, source, actionText);
            }

            @Override
            public boolean objectivePullFormationExempt(
                    SwccgGame game, String playerId,
                    PhysicalCard source, String actionText) {
                return objective.isNoMoneyNoPartsWattoPullAction(
                        game, playerId, source, actionText);
            }
        };
    }

    private static PullTakeCandidateFactsReader.ObjectiveView takeObjectiveView(
            ObjectiveAnalyzer objective) {
        if (objective == null) {
            return null;
        }
        return new PullTakeCandidateFactsReader.ObjectiveView() {
            @Override
            public boolean isAnalyzed() {
                return objective.isAnalyzed();
            }

            @Override
            public boolean needsBespinSystemPresence() {
                return objective.needsBespinSystemPresence();
            }
        };
    }

    private static final class OracleView implements PullOracleView {
        private final DeckOracle oracle;
        private final com.gempukku.swccgo.game.state.GameState gameState;

        private OracleView(
                DeckOracle oracle,
                com.gempukku.swccgo.game.state.GameState gameState) {
            this.oracle = oracle;
            this.gameState = gameState;
        }

        @Override
        public boolean isAvailable() {
            return oracle != null;
        }

        @Override
        public boolean isAnalyzed() {
            return oracle != null && oracle.isAnalyzed();
        }

        @Override
        public boolean hasTargetInReserve(String... keywords) {
            return oracle != null && oracle.hasTargetInReserve(keywords);
        }

        @Override
        public boolean hasTargetInZone(Zone zone, String target) {
            return oracle != null && oracle.hasTargetInZone(zone, target);
        }

        @Override
        public boolean isGenericTypeWord(String word) {
            return DeckOracle.mapTypeWordToCategory(word) != null;
        }

        @Override
        public Zone parseSourceZone(String actionText) {
            return DeckOracle.parseSourceZone(actionText);
        }

        @Override
        public List<String> parseSourceCardPullTargets(String gameText) {
            return DeckOracle.parseSourceCardPullTargets(gameText);
        }

        @Override
        public String sourceCardFullGameText(
                SwccgCardBlueprint blueprint, Side side) {
            return DeckOracle.getSourceCardFullGameText(blueprint, side);
        }

        @Override
        public Validation validatePull(Zone zone, String... keywords) {
            return validation(oracle != null ? oracle.validatePull(zone, keywords) : null);
        }

        @Override
        public Validation validatePullFromSourceCard(Zone zone, String gameText) {
            return validation(oracle != null
                    ? oracle.validatePullFromSourceCard(zone, gameText) : null);
        }

        @Override
        public TypedReserveMatch typedReserveMatch(
                SwccgGame game, String playerId, String noun) {
            Filter filter = DeckOracle.resolveCommonNounToFilter(noun);
            if (filter == null || oracle == null) {
                return TypedReserveMatch.UNRECOGNIZED;
            }
            return oracle.hasFilterMatchInReserve(game, playerId, filter)
                    ? TypedReserveMatch.MATCH : TypedReserveMatch.MISS;
        }

        @Override
        public boolean reserveTargetsAreAllUnattachableWeapons(
                SwccgGame game, String playerId, List<String> targets) {
            return oracle != null && oracle.reserveTargetsAreAllUnattachableWeapons(
                    game, playerId, targets);
        }

        @Override
        public boolean reservePullFetchesOnlyStarships(String gameText) {
            return oracle != null && oracle.reservePullFetchesOnlyStarships(gameText);
        }

        @Override
        public boolean spaceLocationOnTable() {
            return gameState != null && DeckOracle.spaceLocationOnTable(gameState);
        }

        @Override
        public int countMatchingInDeck(
                SwccgGame game, String playerId, String noun) {
            Filter filter = DeckOracle.resolveCommonNounToFilter(noun);
            return oracle != null && filter != null
                    ? oracle.countMatchingInDeck(game, playerId, filter) : -1;
        }

        @Override
        public int countMatchingInHandOrTable(
                SwccgGame game, String playerId, String noun) {
            Filter filter = DeckOracle.resolveCommonNounToFilter(noun);
            return oracle != null && filter != null
                    ? oracle.countMatchingInHandOrTable(game, playerId, filter) : -1;
        }

        @Override
        public List<NamedDeckCard> namedDeckCardsInText(
                String gameText, String sourceBlueprintId) {
            if (oracle == null) {
                return List.of();
            }
            List<NamedDeckCard> result = new ArrayList<>();
            for (DeckOracle.DeckCard card :
                    oracle.namedDeckCardsInText(gameText, sourceBlueprintId)) {
                result.add(new NamedDeckCard(card.getTitle(), card.getCurrentZone()));
            }
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean personaNamedInText(Set<?> personas, String text) {
            return DeckOracle.personaNamedInText(
                    (Set<com.gempukku.swccgo.common.Persona>) personas, text);
        }

        private static Validation validation(DeckOracle.PullValidation value) {
            if (value == null) {
                return new Validation(Outcome.UNKNOWN, "");
            }
            return new Validation(switch (value.outcome) {
                case WILL_FAIL -> Outcome.WILL_FAIL;
                case WASTEFUL -> Outcome.WASTEFUL;
                case WILL_SUCCEED -> Outcome.WILL_SUCCEED;
                default -> Outcome.UNKNOWN;
            }, value.reason);
        }
    }
}
