package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.DestinyType;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.DrawDestinyState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.GameTextAction;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierType;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Conservative live-engine adapter for the two TDIGWATT printings.
 *
 * <p>Every physical identity in this reader is a permanent card id. Methods
 * return empty when an exact source fact cannot be read. In particular, this
 * reader does not invent hypothetical post-deploy, post-move, or post-forfeit
 * control states.</p>
 */
public final class TdigwattObjectiveFactsReader {
    public static final String
            LANDO_ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA =
                "tdigwattLandoActionSourcePermanentCardId";
    public static final String
            LANDO_MOVER_PERMANENT_CARD_ID_EXTRA =
                "tdigwattLandoMoverPermanentCardId";
    public static final String
            LANDO_DESTINATION_PERMANENT_CARD_ID_EXTRA =
                "tdigwattLandoDestinationPermanentCardId";
    public static final String
            DESTINY_ADJUSTMENT_ACTION_SOURCES_EXTRA =
                "tdigwattDestinyAdjustmentActionSources";
    public static final String
            PULL_ACTION_SOURCES_EXTRA =
                "tdigwattPullActionSources";

    /**
     * Explicit caller proof for facts that the current engine APIs do not
     * expose as a reliable snapshot value.
     */
    public enum Proof {
        PROVEN,
        DISPROVEN,
        UNKNOWN
    }

    public record VirtualLandoLandspeedRoute(
            int landoPermanentCardId,
            int originPermanentCardId,
            int destinationPermanentCardId,
            TdigwattObjectiveFacts.LandoMoveFacts moveFacts) {
    }

    public record VirtualDeployProjection(
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState after) {
    }

    private TdigwattObjectiveFactsReader() {
    }

    /**
     * Reads the exact active Dark objective printing and side.
     */
    public static Optional<TdigwattObjectiveFacts.ObjectiveIdentity>
            readObjectiveIdentity(SwccgGame game, String playerId) {
        if (game == null || playerId == null) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            if (gameState == null
                    || !playerId.equals(game.getDarkPlayer())) {
                return Optional.empty();
            }
            PhysicalCard objective =
                    gameState.getObjectivePlayed(playerId);
            if (!isOwnedActiveObjective(objective, playerId)) {
                return Optional.empty();
            }
            String frontBlueprintId =
                    objective.getBlueprintId(true);
            if (!TdigwattObjectiveFacts.CLASSIC_BLUEPRINT_ID.equals(
                        frontBlueprintId)
                    && !TdigwattObjectiveFacts.VIRTUAL_BLUEPRINT_ID.equals(
                        frontBlueprintId)) {
                return Optional.empty();
            }
            int permanentCardId = objective.getPermanentCardId();
            if (permanentCardId <= 0) {
                return Optional.empty();
            }
            return Optional.of(
                    new TdigwattObjectiveFacts.ObjectiveIdentity(
                            permanentCardId,
                            frontBlueprintId,
                            objective.isFlipped()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Reads classic front-side board facts using the same active-card,
     * occupation, control, and spot overrides as the card source.
     *
     * <p>The event-only back-side fields are false because this method is
     * deliberately unavailable while the back side is up.</p>
     */
    public static Optional<TdigwattObjectiveFacts.ClassicState>
            readClassicFrontState(SwccgGame game, String playerId) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()) {
            return Optional.empty();
        }
        TdigwattObjectiveFacts.ObjectiveIdentity identity =
                identityRead.get();
        if (identity.printing()
                    != TdigwattObjectiveFacts.Printing.CLASSIC
                || identity.backSideUp()) {
            return Optional.empty();
        }

        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            PhysicalCard objective =
                    gameState.getObjectivePlayed(playerId);
            String opponentId = gameState.getOpponent(playerId);
            if (modifiers == null || opponentId == null) {
                return Optional.empty();
            }

            boolean darkDealOnTable = GameConditions.canSpot(
                    game, objective,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE,
                    Filters.Dark_Deal);
            boolean darkOccupiesBespinSystem = false;
            boolean darkOccupiesBespinCloudCity = false;
            boolean opponentControlsBespinSystem = false;
            List<PhysicalCard> topLocations =
                    gameState.getTopLocations();
            if (topLocations == null) {
                return Optional.empty();
            }
            for (PhysicalCard location : topLocations) {
                if (location == null) {
                    continue;
                }
                if (Filters.Bespin_system.accepts(
                        gameState, modifiers, location)) {
                    darkOccupiesBespinSystem |=
                            modifiers.occupiesLocation(
                                    gameState, location, playerId,
                                    SpotOverride
                                        .INCLUDE_EXCLUDED_FROM_BATTLE);
                    opponentControlsBespinSystem |=
                            modifiers.controlsLocation(
                                    gameState, location, opponentId,
                                    SpotOverride
                                        .INCLUDE_EXCLUDED_FROM_BATTLE);
                }
                if (Filters.Bespin_Cloud_City.accepts(
                        gameState, modifiers, location)) {
                    darkOccupiesBespinCloudCity |=
                            modifiers.occupiesLocation(
                                    gameState, location, playerId,
                                    SpotOverride
                                        .INCLUDE_EXCLUDED_FROM_BATTLE);
                }
            }

            return Optional.of(
                    new TdigwattObjectiveFacts.ClassicState(
                            identity,
                            darkDealOnTable,
                            darkOccupiesBespinSystem,
                            darkOccupiesBespinCloudCity,
                            false,
                            opponentControlsBespinSystem,
                            false));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Counts exact top Bespin locations controlled by each side. Ties remain
     * represented as equal counts.
     */
    public static Optional<TdigwattObjectiveFacts.VirtualState>
            readVirtualState(SwccgGame game, String playerId) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || identityRead.get().printing()
                    != TdigwattObjectiveFacts.Printing.VIRTUAL) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            String opponentId = gameState.getOpponent(playerId);
            List<PhysicalCard> topLocations =
                    gameState.getTopLocations();
            if (modifiers == null || opponentId == null
                    || topLocations == null) {
                return Optional.empty();
            }
            int darkControl = 0;
            int lightControl = 0;
            for (PhysicalCard location : topLocations) {
                if (location == null
                        || !Filters.Bespin_location.accepts(
                            gameState, modifiers, location)) {
                    continue;
                }
                if (modifiers.controlsLocation(
                        gameState, location, playerId,
                        SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE)) {
                    darkControl++;
                }
                if (modifiers.controlsLocation(
                        gameState, location, opponentId,
                        SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE)) {
                    lightControl++;
                }
            }
            return Optional.of(
                    new TdigwattObjectiveFacts.VirtualState(
                            identityRead.get(),
                            darkControl,
                            lightControl));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Projects one exact offered character destination only when the deploy
     * provably adds control of one Bespin location.
     *
     * <p>This deliberately excludes spies, operatives, opponent-presence
     * destinations, and unknown ability thresholds. Those routes remain with
     * the existing deploy evaluator rather than receiving guessed objective
     * progress.</p>
     */
    public static Optional<VirtualDeployProjection>
            readVirtualDeployProjection(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard deployingCard,
                    PhysicalCard destination) {
        Optional<TdigwattObjectiveFacts.VirtualState>
                beforeRead =
                    readVirtualState(game, playerId);
        if (beforeRead.isEmpty()
                || deployingCard == null
                || destination == null) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            String opponent =
                    gameState.getOpponent(playerId);
            if (modifiers == null || opponent == null
                    || !playerId.equals(
                        deployingCard.getOwner())
                    || !Filters.character.accepts(
                        gameState, modifiers,
                        deployingCard)
                    || Filters.spy.accepts(
                        gameState, modifiers,
                        deployingCard)
                    || Filters.operative.accepts(
                        gameState, modifiers,
                        deployingCard)
                    || deployingCard.isUndercover()
                    || !Filters.Bespin_location.accepts(
                        gameState, modifiers,
                        destination)
                    || modifiers.controlsLocation(
                        gameState, destination,
                        playerId,
                        SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE)
                    || modifiers.hasPresenceAt(
                        gameState, opponent,
                        destination, false, null,
                        SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE)) {
                return Optional.empty();
            }

            float deployingAbility =
                    modifiers.getAbility(
                        gameState, deployingCard);
            float projectedAbility =
                    modifiers.getTotalAbilityAtLocation(
                        gameState, playerId,
                        destination)
                    + deployingAbility;
            if (deployingAbility <= 0.0f
                    || !projectedAbilityControls(
                        gameState, modifiers,
                        playerId, destination,
                        projectedAbility)) {
                return Optional.empty();
            }

            TdigwattObjectiveFacts.VirtualState before =
                    beforeRead.get();
            TdigwattObjectiveFacts.VirtualState after =
                    new TdigwattObjectiveFacts.VirtualState(
                        before.objective(),
                        before.darkControlledBespinLocations()
                            + 1,
                        before.lightControlledBespinLocations());
            return Optional.of(
                    new VirtualDeployProjection(
                        before, after));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Reads whether one exact TDIGWATT engine effect will survive its own
     * source-defined cancellation check after the engine-offered deploy.
     *
     * <p>Empty means the print or board state could not be proved. Dark Deal
     * (V) has no automatic cancellation trigger, while the classic Dark Deal
     * and Cloud City Occupation use the same occupation, control, and spot
     * overrides as their card source.</p>
     */
    public static Optional<Boolean>
            readEngineEffectPersistsAfterDeploy(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard effect) {
        if (game == null || playerId == null
                || effect == null) {
            return Optional.empty();
        }
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity>
                identityRead =
                    readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || !playerId.equals(effect.getOwner())) {
            return Optional.empty();
        }
        try {
            String blueprintId =
                    effect.getBlueprintId(true);
            if ("223_9".equals(blueprintId)) {
                return Optional.of(true);
            }

            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            List<PhysicalCard> topLocations =
                    gameState != null
                        ? gameState.getTopLocations() : null;
            String opponent =
                    gameState != null
                        ? gameState.getOpponent(playerId) : null;
            if (modifiers == null || topLocations == null
                    || opponent == null) {
                return Optional.empty();
            }

            if ("5_115".equals(blueprintId)) {
                int cancelAt = 4
                        + GameConditions
                            .getGameTextModificationCount(
                                game, effect,
                                ModifyGameTextType
                                    .DARK_DEAL__ADDITIONAL_BESPIN_LOCATION_TO_CANCEL);
                int opponentOccupied = 0;
                for (PhysicalCard location : topLocations) {
                    if (location != null
                            && Filters.Bespin_location
                                .accepts(
                                    gameState, modifiers,
                                    location)
                            && modifiers.occupiesLocation(
                                gameState, location,
                                opponent,
                                SpotOverride
                                    .INCLUDE_EXCLUDED_FROM_BATTLE)) {
                        opponentOccupied++;
                    }
                }
                return Optional.of(
                        opponentOccupied < cancelAt);
            }

            if ("7_223".equals(blueprintId)) {
                boolean foundBespinSystem = false;
                for (PhysicalCard location : topLocations) {
                    if (location == null
                            || !Filters.Bespin_system
                                .accepts(
                                    gameState, modifiers,
                                    location)) {
                        continue;
                    }
                    foundBespinSystem = true;
                    if (modifiers.controlsLocation(
                            gameState, location,
                            opponent,
                            SpotOverride
                                .INCLUDE_EXCLUDED_FROM_BATTLE)) {
                        return Optional.of(false);
                    }
                }
                return foundBespinSystem
                        ? Optional.of(true)
                        : Optional.empty();
            }
            return Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Classifies one exact Reserve Deck candidate before applying the
     * printing-specific source filter. A present result whose policy legality
     * is false is a known decoy, while empty means the candidate could not be
     * classified from the exact source and Reserve Deck state.
     */
    public static Optional<TdigwattObjectiveFacts.PullFacts>
            readObjectivePullCandidate(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource,
                    PhysicalCard candidate) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || actionSource == null
                || candidate == null) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            TdigwattObjectiveFacts.ObjectiveIdentity identity =
                    identityRead.get();
            if (modifiers == null
                    || actionSource.getPermanentCardId()
                        != identity.physicalCardId()
                    || !playerId.equals(actionSource.getOwner())
                    || !playerId.equals(candidate.getOwner())
                    || !isReserveCandidate(
                        gameState, playerId, candidate)) {
                return Optional.empty();
            }

            TdigwattObjectiveFacts.PullTarget target =
                    classifyPullTarget(
                            gameState, modifiers, candidate);
            String blueprintId =
                    candidate.getBlueprintId(true);
            if (target == null || blueprintId == null
                    || blueprintId.isBlank()) {
                return Optional.empty();
            }
            boolean specialEditionPrint =
                    candidate.getBlueprint() != null
                    && candidate.getBlueprint()
                        .hasIcon(Icon.SPECIAL_EDITION);
            TdigwattObjectiveFacts.PullFacts facts =
                    new TdigwattObjectiveFacts.PullFacts(
                            identity,
                            actionSource.getPermanentCardId(),
                            candidate.getPermanentCardId(),
                            blueprintId,
                            target,
                            specialEditionPrint,
                            true);
            return Optional.of(facts);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Reads one exact Reserve Deck candidate only when the objective
     * printing's own source filter permits it.
     */
    public static Optional<TdigwattObjectiveFacts.PullFacts>
            readSourceLegalPullCandidate(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource,
                    PhysicalCard candidate) {
        return readObjectivePullCandidate(
                    game, playerId, actionSource, candidate)
                .filter(TdigwattObjectivePolicy::sourceLegalPull);
    }

    /**
     * Reads every exact source-legal target currently in Reserve Deck.
     * Empty list means known exhaustion; empty Optional means the source or
     * Reserve Deck state could not be proved.
     */
    public static Optional<List<TdigwattObjectiveFacts.PullFacts>>
            readSourceLegalPullCandidatesInReserve(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty() || actionSource == null) {
            return Optional.empty();
        }
        try {
            TdigwattObjectiveFacts.ObjectiveIdentity identity =
                    identityRead.get();
            if (actionSource.getPermanentCardId()
                        != identity.physicalCardId()
                    || !playerId.equals(actionSource.getOwner())) {
                return Optional.empty();
            }
            List<PhysicalCard> reserve =
                    game.getGameState()
                        .getReserveDeck(playerId);
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            if (reserve == null || modifiers == null) {
                return Optional.empty();
            }
            List<TdigwattObjectiveFacts.PullFacts> legal =
                    new ArrayList<>();
            for (PhysicalCard candidate : reserve) {
                if (candidate == null
                        || candidate.getPermanentCardId() <= 0
                        || candidate.getZone() == null) {
                    return Optional.empty();
                }
                if (!playerId.equals(candidate.getOwner())
                        || (candidate.getZone()
                                != Zone.RESERVE_DECK
                            && candidate.getZone()
                                != Zone.TOP_OF_RESERVE_DECK)) {
                    continue;
                }
                TdigwattObjectiveFacts.PullTarget target =
                        classifyPullTarget(
                                game.getGameState(),
                                modifiers, candidate);
                if (target == null) {
                    continue;
                }
                String blueprintId =
                        candidate.getBlueprintId(true);
                if (blueprintId == null
                        || blueprintId.isBlank()
                        || candidate.getBlueprint() == null) {
                    return Optional.empty();
                }
                TdigwattObjectiveFacts.PullFacts facts =
                        new TdigwattObjectiveFacts.PullFacts(
                                identity,
                                actionSource
                                    .getPermanentCardId(),
                                candidate
                                    .getPermanentCardId(),
                                blueprintId,
                                target,
                                candidate.getBlueprint()
                                    .hasIcon(
                                        Icon
                                            .SPECIAL_EDITION),
                                true);
                if (TdigwattObjectivePolicy
                        .sourceLegalPull(facts)) {
                    legal.add(facts);
                }
            }
            return Optional.of(List.copyOf(legal));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Distinguishes known source exhaustion from an unreadable source state.
     */
    public static Optional<Boolean>
            hasAnySourceLegalPullInReserve(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource) {
        return readSourceLegalPullCandidatesInReserve(
                    game, playerId, actionSource)
                .map(candidates -> !candidates.isEmpty());
    }

    /**
     * Reads exact participants from an already-started back-side battle.
     */
    public static Optional<TdigwattObjectiveFacts.BattleFacts>
            readLiveBackSideBattleFacts(
                    SwccgGame game, String playerId) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || !identityRead.get().backSideUp()) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            BattleState battle = gameState.getBattleState();
            if (modifiers == null || battle == null) {
                return Optional.empty();
            }
            Collection<PhysicalCard> ours =
                    battle.getCardsParticipating(playerId);
            Collection<PhysicalCard> all =
                    battle.getAllCardsParticipating();
            if (ours == null || all == null) {
                return Optional.empty();
            }

            return Optional.of(readBattleFacts(
                    identityRead.get(), playerId,
                    gameState, modifiers, ours, all));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Captures exact parent-action provenance from the engine decision.
     */
    public static Map<String, Integer>
            readDestinyAdjustmentActionSources(
                    AwaitingDecision decision,
                    SwccgGame game,
                    String playerId) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (decision == null
                || identityRead.isEmpty()
                || identityRead.get().printing()
                    != TdigwattObjectiveFacts.Printing.VIRTUAL
                || !identityRead.get().backSideUp()) {
            return Map.of();
        }
        Map<String, String[]> parameters =
                decision.getDecisionParameters();
        String[] actionIds = parameters != null
                ? parameters.get("actionId") : null;
        if (actionIds == null || actionIds.length == 0) {
            return Map.of();
        }
        Map<String, Integer> exactSources =
                new LinkedHashMap<>();
        for (String actionId : actionIds) {
            Action action =
                    AiActionSourceProvenance.actionForId(
                            decision, actionId);
            PhysicalCard source = action != null
                    ? action.getActionSource() : null;
            if (!(action instanceof GameTextAction)
                    || source == null
                    || source.getPermanentCardId()
                        != identityRead.get()
                            .physicalCardId()
                    || action.getGameTextActionId()
                        != GameTextActionId
                            .OTHER_CARD_ACTION_3
                    || !"Add or subtract 1 from destiny draw"
                            .equals(action.getText())) {
                continue;
            }
            exactSources.put(
                    actionId,
                    source.getPermanentCardId());
        }
        return Map.copyOf(exactSources);
    }

    /**
     * Captures exact classic or virtual objective-upload provenance.
     */
    public static Map<String, Integer>
            readPullActionSources(
                    AwaitingDecision decision,
                    SwccgGame game,
                    String playerId) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (decision == null || identityRead.isEmpty()) {
            return Map.of();
        }
        Map<String, String[]> parameters =
                decision.getDecisionParameters();
        String[] actionIds = parameters != null
                ? parameters.get("actionId") : null;
        if (actionIds == null || actionIds.length == 0) {
            return Map.of();
        }
        GameTextActionId expectedActionId =
                identityRead.get().printing()
                    == TdigwattObjectiveFacts.Printing.CLASSIC
                ? GameTextActionId
                    .THIS_DEAL_IS_GETTING_WORSE_ALL_THE_TIME__UPLOAD_CARD
                : GameTextActionId
                    .THIS_DEAL_IS_GETTING_WORSE_ALL_THE_TIME_V__UPLOAD_CARD;
        Map<String, Integer> exactSources =
                new LinkedHashMap<>();
        for (String actionId : actionIds) {
            Action action =
                    AiActionSourceProvenance.actionForId(
                            decision, actionId);
            PhysicalCard source = action != null
                    ? action.getActionSource() : null;
            if (!(action instanceof GameTextAction)
                    || source == null
                    || source.getPermanentCardId()
                        != identityRead.get()
                            .physicalCardId()
                    || action.getGameTextActionId()
                        != expectedActionId
                    || !"Take card into hand from Reserve Deck"
                            .equals(action.getText())) {
                continue;
            }
            exactSources.put(
                    actionId,
                    source.getPermanentCardId());
        }
        return Map.copyOf(exactSources);
    }

    /**
     * Reads the exact live draw direction and source-defined usage ceiling.
     */
    public static Optional<TdigwattObjectiveFacts
            .DestinyAdjustmentFacts>
            readLiveDestinyAdjustmentFacts(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        Optional<TdigwattObjectiveFacts.BattleFacts> battleRead =
                readLiveBackSideBattleFacts(
                        game, playerId);
        if (identityRead.isEmpty()
                || battleRead.isEmpty()
                || identityRead.get().printing()
                    != TdigwattObjectiveFacts.Printing.VIRTUAL
                || !identityRead.get().backSideUp()
                || actionSource == null
                || actionSource.getPermanentCardId()
                    != identityRead.get().physicalCardId()
                || !battleRead.get().yourLandoInBattle()) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            DrawDestinyState drawState =
                    gameState.getTopDrawDestinyState();
            if (drawState == null
                    || drawState.getDrawDestinyEffect() == null) {
                return Optional.empty();
            }
            String drawer = drawState.getDrawDestinyEffect()
                    .getPlayerDrawingDestiny();
            DestinyType destinyType =
                    drawState.getDrawDestinyEffect()
                        .getDestinyType();
            if (destinyType == null) {
                return Optional.empty();
            }
            String opponent =
                    gameState.getOpponent(playerId);
            TdigwattObjectiveFacts.DestinyDrawOwner owner;
            if (playerId.equals(drawer)) {
                owner = TdigwattObjectiveFacts
                        .DestinyDrawOwner.YOURS;
            } else if (opponent != null
                    && opponent.equals(drawer)) {
                owner = TdigwattObjectiveFacts
                        .DestinyDrawOwner.OPPONENTS;
            } else {
                return Optional.empty();
            }
            int usesPerBattle =
                    battleRead.get()
                            .anyLobotParticipating()
                    ? 2 : 1;
            return Optional.of(
                    new TdigwattObjectiveFacts
                        .DestinyAdjustmentFacts(
                            identityRead.get(),
                            actionSource
                                .getPermanentCardId(),
                            battleRead.get(),
                            owner,
                            destinyType,
                            usesPerBattle));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Conservatively predicts the participants eligible at one exact battle
     * target before BattleState exists. This uses the engine's participation
     * filter, so inactive, excluded, prohibited, and wrong-location cards do
     * not create objective payoff.
     */
    public static Optional<TdigwattObjectiveFacts.BattleFacts>
            readBackSideBattleFactsAtLocation(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard target) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || !identityRead.get().backSideUp()
                || target == null) {
            return Optional.empty();
        }
        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            if (modifiers == null
                    || gameState.getBattleState() != null
                    || !containsPermanentCard(
                        gameState.getTopLocations(), target)) {
                return Optional.empty();
            }
            PhysicalCard objective =
                    gameState.getObjectivePlayed(playerId);
            Collection<PhysicalCard> all =
                    Filters.filterActive(
                            game, objective,
                            Filters.canParticipateInBattleAt(
                                    target, playerId));
            if (all == null) {
                return Optional.empty();
            }
            List<PhysicalCard> ours = new ArrayList<>();
            for (PhysicalCard card : all) {
                if (card != null
                        && playerId.equals(card.getOwner())) {
                    ours.add(card);
                }
            }
            return Optional.of(readBattleFacts(
                    identityRead.get(), playerId,
                    gameState, modifiers, ours, all));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Reads the virtual back-side Vader condition with the exact source
     * ownership semantics: any Vader, not only your Vader.
     */
    public static Optional<Boolean> readAnyVaderAtBespin(
            SwccgGame game, String playerId) {
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || identityRead.get().printing()
                    != TdigwattObjectiveFacts.Printing.VIRTUAL
                || !identityRead.get().backSideUp()) {
            return Optional.empty();
        }
        try {
            PhysicalCard objective =
                    game.getGameState()
                        .getObjectivePlayed(playerId);
            Filter vaderAtBespin = Filters.and(
                    Filters.or(
                            Filters.Vader,
                            Filters.hasPermanentAboard(
                                    Filters.Vader),
                            Filters.hasPermanentWeapon(
                                    Filters.Vader)),
                    Filters.at(Filters.Bespin_location));
            return Optional.of(
                    Filters.canSpot(
                            game, objective,
                            vaderAtBespin));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Conservative seam for one exact virtual-objective Lando landspeed route.
     *
     * <p>The virtual objective grants a regular move, but a regular move can
     * use many mechanisms. This method intentionally supports only landspeed.
     * Source-action availability, objective usefulness, and formation safety
     * must be proved by the caller's exact decision context. Unknown proof
     * never creates Force reservation or movement credit.</p>
     */
    public static Optional<TdigwattObjectiveFacts.LandoMoveFacts>
            readVirtualLandoLandspeedRoute(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource,
                    PhysicalCard lando,
                    PhysicalCard origin,
                    PhysicalCard destination,
                    Proof sourceActionAvailable,
                    Proof advancesOrProtectsObjective,
                    Proof formationSafe) {
        if (sourceActionAvailable != Proof.PROVEN
                || advancesOrProtectsObjective != Proof.PROVEN
                || formationSafe != Proof.PROVEN) {
            return Optional.empty();
        }
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        if (identityRead.isEmpty()
                || identityRead.get().printing()
                    != TdigwattObjectiveFacts.Printing.VIRTUAL
                || actionSource == null
                || lando == null
                || origin == null
                || destination == null) {
            return Optional.empty();
        }
        try {
            TdigwattObjectiveFacts.ObjectiveIdentity identity =
                    identityRead.get();
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            if (modifiers == null
                    || actionSource.getPermanentCardId()
                        != identity.physicalCardId()
                    || !playerId.equals(actionSource.getOwner())
                    || !playerId.equals(lando.getOwner())
                    || lando.getZone() == null
                    || !lando.getZone().isInPlay()
                    || !Filters.Lando.accepts(
                        gameState, modifiers, lando)
                    || !containsPermanentCard(
                        gameState.getTopLocations(), origin)
                    || !containsPermanentCard(
                        gameState.getTopLocations(), destination)) {
                return Optional.empty();
            }
            PhysicalCard actualOrigin =
                    modifiers.getLocationThatCardIsAt(
                            gameState, lando);
            if (!samePermanentCard(
                    actualOrigin, origin)) {
                return Optional.empty();
            }

            Filter legalLandspeedDestination =
                    Filters.canMoveToUsingLandspeed(
                            playerId, lando,
                            false, false, false,
                            0.0f, null);
            if (!legalLandspeedDestination.accepts(
                    gameState, modifiers, destination)) {
                return Optional.empty();
            }
            float cost =
                    modifiers.getMoveUsingLandspeedCost(
                            gameState, lando,
                            origin, destination,
                            false, 0.0f);
            if (!Float.isFinite(cost)) {
                return Optional.empty();
            }
            int requiredForceCost =
                    Math.max(0, (int) Math.ceil(cost));
            return Optional.of(
                    new TdigwattObjectiveFacts.LandoMoveFacts(
                            identity,
                            actionSource
                                .getPermanentCardId(),
                            true,
                            true,
                            true,
                            true,
                            true,
                            requiredForceCost));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Finds the exact Lando selected by Card226_012's source filter and one
     * conservative landspeed route that increases Dark's Bespin control count.
     *
     * <p>The route is accepted only when the destination is an uncontested,
     * presently uncontrolled Bespin location, Lando supplies enough projected
     * ability to control it, any controlled Bespin origin remains held without
     * Lando, and both shared formation-safety checks pass. Unknown projection
     * details remain neutral.</p>
     */
    public static Optional<VirtualLandoLandspeedRoute>
            readUsefulVirtualLandoLandspeedRoute(
                    SwccgGame game,
                    String playerId,
                    PhysicalCard actionSource,
                    Proof sourceActionAvailable) {
        if (sourceActionAvailable != Proof.PROVEN) {
            return Optional.empty();
        }
        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identityRead =
                readObjectiveIdentity(game, playerId);
        Optional<TdigwattObjectiveFacts.VirtualState> stateRead =
                readVirtualState(game, playerId);
        if (identityRead.isEmpty()
                || stateRead.isEmpty()
                || actionSource == null
                || identityRead.get().printing()
                    != TdigwattObjectiveFacts.Printing.VIRTUAL
                || actionSource.getPermanentCardId()
                    != identityRead.get().physicalCardId()) {
            return Optional.empty();
        }

        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            PhysicalCard lando = Filters.findFirstActive(
                    game,
                    actionSource,
                    Filters.and(
                            Filters.your(playerId),
                            Filters.Lando,
                            Filters.movableAsRegularMove(
                                    playerId,
                                    false,
                                    0,
                                    false,
                                    Filters.any)));
            if (modifiers == null || lando == null
                    || lando.getPermanentCardId() <= 0) {
                return Optional.empty();
            }
            PhysicalCard origin =
                    modifiers.getLocationThatCardIsAt(
                            gameState, lando);
            if (origin == null
                    || origin.getPermanentCardId() <= 0) {
                return Optional.empty();
            }

            TdigwattObjectiveFacts.VirtualState state =
                    stateRead.get();
            if (!state.objective().backSideUp()
                    && (state.darkControlledBespinLocations() >= 3
                        || state.lightControlledBespinLocations() >= 3)) {
                return Optional.empty();
            }

            String opponentId =
                    gameState.getOpponent(playerId);
            if (opponentId == null) {
                return Optional.empty();
            }
            float landoAbility =
                    modifiers.getAbility(gameState, lando);
            if (landoAbility <= 0.0f) {
                return Optional.empty();
            }
            List<PhysicalCard> locations =
                    gameState.getLocationsInOrder();
            if (locations == null) {
                return Optional.empty();
            }

            for (PhysicalCard destination : locations) {
                if (destination == null
                        || destination.getPermanentCardId() <= 0
                        || samePermanentCard(
                            origin, destination)
                        || !Filters.Bespin_location.accepts(
                            gameState, modifiers,
                            destination)
                        || modifiers.controlsLocation(
                            gameState, destination,
                            playerId,
                            SpotOverride
                                .INCLUDE_EXCLUDED_FROM_BATTLE)
                        || modifiers.hasPresenceAt(
                            gameState, opponentId,
                            destination, false, null,
                            SpotOverride
                                .INCLUDE_EXCLUDED_FROM_BATTLE)
                        || !projectedAbilityControls(
                            gameState, modifiers,
                            playerId, destination,
                            modifiers
                                .getTotalAbilityAtLocation(
                                    gameState, playerId,
                                    destination)
                                + landoAbility)
                        || !controlledBespinOriginRemainsHeld(
                            gameState, modifiers,
                            playerId, lando,
                            landoAbility, origin)
                        || FormationSafety
                            .vetoMoveDestination(
                                game, gameState,
                                playerId, lando,
                                destination) != null
                        || FormationSafety
                            .vetoMoveOrigin(
                                game, gameState,
                                playerId, lando,
                                origin) != null) {
                    continue;
                }

                Optional<TdigwattObjectiveFacts.LandoMoveFacts>
                        moveRead =
                    readVirtualLandoLandspeedRoute(
                        game, playerId,
                        actionSource, lando,
                        origin, destination,
                        Proof.PROVEN,
                        Proof.PROVEN,
                        Proof.PROVEN);
                if (moveRead.isPresent()) {
                    return Optional.of(
                        new VirtualLandoLandspeedRoute(
                            lando.getPermanentCardId(),
                            origin.getPermanentCardId(),
                            destination
                                .getPermanentCardId(),
                            moveRead.get()));
                }
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static TdigwattObjectiveFacts.BattleFacts
            readBattleFacts(
                    TdigwattObjectiveFacts.ObjectiveIdentity identity,
                    String playerId,
                    GameState gameState,
                    ModifiersQuerying modifiers,
                    Collection<PhysicalCard> ours,
                    Collection<PhysicalCard> all) {
        boolean alien = false;
        boolean imperial = false;
        boolean ugnaught = false;
        boolean lando = false;
        for (PhysicalCard card : ours) {
            if (card == null
                    || !playerId.equals(card.getOwner())) {
                continue;
            }
            alien |= Filters.alien.accepts(
                    gameState, modifiers, card);
            imperial |= Filters.Imperial.accepts(
                    gameState, modifiers, card);
            ugnaught |= Filters.Ugnaught.accepts(
                    gameState, modifiers, card);
            lando |= Filters.Lando.accepts(
                    gameState, modifiers, card);
        }

        boolean exactPair = false;
        if (alien && imperial) {
            for (PhysicalCard alienCard : ours) {
                if (alienCard == null
                        || !playerId.equals(
                            alienCard.getOwner())
                        || !Filters.alien.accepts(
                            gameState, modifiers,
                            alienCard)) {
                    continue;
                }
                for (PhysicalCard imperialCard : ours) {
                    if (imperialCard != null
                            && playerId.equals(
                                imperialCard.getOwner())
                            && Filters.Imperial.accepts(
                                gameState, modifiers,
                                imperialCard)
                            && modifiers.isWith(
                                gameState, alienCard,
                                imperialCard)) {
                        exactPair = true;
                        break;
                    }
                }
                if (exactPair) {
                    break;
                }
            }
        }

        boolean anyLobot = false;
        for (PhysicalCard card : all) {
            if (card != null
                    && Filters.Lobot.accepts(
                        gameState, modifiers, card)) {
                anyLobot = true;
                break;
            }
        }
        return new TdigwattObjectiveFacts.BattleFacts(
                identity,
                exactPair,
                exactPair,
                ugnaught,
                lando,
                anyLobot);
    }

    private static boolean isOwnedActiveObjective(
            PhysicalCard objective, String playerId) {
        return objective != null
                && playerId.equals(objective.getOwner())
                && objective.getZone() != null
                && objective.getZone().isInPlay()
                && objective.isObjectiveDeploymentComplete();
    }

    private static TdigwattObjectiveFacts.PullTarget
            classifyPullTarget(
                    GameState gameState,
                    ModifiersQuerying modifiers,
                    PhysicalCard candidate) {
        if (Filters.Bespin_system.accepts(
                gameState, modifiers, candidate)) {
            return TdigwattObjectiveFacts.PullTarget
                    .BESPIN_SYSTEM;
        }
        if (Filters.Bespin_Cloud_City.accepts(
                gameState, modifiers, candidate)) {
            return TdigwattObjectiveFacts.PullTarget
                    .BESPIN_CLOUD_CITY;
        }
        if (Filters.Dark_Deal.accepts(
                gameState, modifiers, candidate)) {
            return TdigwattObjectiveFacts.PullTarget
                    .DARK_DEAL;
        }
        if (Filters.Cloud_City_Occupation.accepts(
                gameState, modifiers, candidate)) {
            return TdigwattObjectiveFacts.PullTarget
                    .CLOUD_CITY_OCCUPATION;
        }
        if (Filters.Vaders_Bounty.accepts(
                gameState, modifiers, candidate)) {
            return TdigwattObjectiveFacts.PullTarget
                    .VADERS_BOUNTY;
        }
        return null;
    }

    private static boolean isReserveCandidate(
            GameState gameState,
            String playerId,
            PhysicalCard candidate) {
        Zone zone = candidate.getZone();
        return (zone == Zone.RESERVE_DECK
                    || zone == Zone.TOP_OF_RESERVE_DECK)
                && containsPermanentCard(
                    gameState.getReserveDeck(playerId),
                    candidate);
    }

    private static boolean controlledBespinOriginRemainsHeld(
            GameState gameState,
            ModifiersQuerying modifiers,
            String playerId,
            PhysicalCard lando,
            float landoAbility,
            PhysicalCard origin) {
        if (!Filters.Bespin_location.accepts(
                    gameState, modifiers, origin)
                || !modifiers.controlsLocation(
                    gameState, origin, playerId,
                    SpotOverride
                        .INCLUDE_EXCLUDED_FROM_BATTLE)) {
            return true;
        }
        float projectedAbility = Math.max(
                0.0f,
                modifiers.getTotalAbilityAtLocation(
                    gameState, playerId, origin)
                    - landoAbility);
        if (!projectedAbilityControls(
                gameState, modifiers,
                playerId, origin,
                projectedAbility)) {
            return false;
        }
        for (PhysicalCard card
                : gameState.getCardsAtLocation(origin)) {
            if (card != null
                    && card.getPermanentCardId()
                        != lando.getPermanentCardId()
                    && playerId.equals(card.getOwner())
                    && card.getZone() != null
                    && card.getZone().isInPlay()
                    && !Filters
                        .operativeOnMatchingPlanet
                        .accepts(
                            gameState, modifiers,
                            card)
                    && (card.getBlueprint() != null
                        && card.getBlueprint()
                            .hasIcon(Icon.PRESENCE)
                        || modifiers.getAbility(
                            gameState, card) > 0.0f)) {
                return true;
            }
        }
        return false;
    }

    private static boolean projectedAbilityControls(
            GameState gameState,
            ModifiersQuerying modifiers,
            String playerId,
            PhysicalCard location,
            float projectedAbility) {
        float requiredAbility = 1.0f;
        for (Modifier modifier
                : modifiers.getModifiersAffectingCard(
                    gameState,
                    ModifierType
                        .ABILITY_REQUIRED_TO_CONTROL_LOCATION,
                    location)) {
            if (modifier.isForPlayer(playerId)) {
                requiredAbility = Math.max(
                    requiredAbility,
                    modifier.getValue(
                        gameState, modifiers,
                        location));
            }
        }
        return projectedAbility >= requiredAbility;
    }

    private static boolean containsPermanentCard(
            Collection<PhysicalCard> cards,
            PhysicalCard candidate) {
        if (cards == null || candidate == null) {
            return false;
        }
        for (PhysicalCard card : cards) {
            if (samePermanentCard(card, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean samePermanentCard(
            PhysicalCard first, PhysicalCard second) {
        return first != null
                && second != null
                && first.getPermanentCardId()
                    == second.getPermanentCardId();
    }
}
