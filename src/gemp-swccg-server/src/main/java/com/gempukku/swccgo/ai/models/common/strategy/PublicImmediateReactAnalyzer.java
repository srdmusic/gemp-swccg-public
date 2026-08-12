package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collection;
import java.util.List;

/** Reads only public in-play facts to prove an immediate move-as-react route. */
public final class PublicImmediateReactAnalyzer {

    private PublicImmediateReactAnalyzer() {
    }

    public static Exposure analyze(SwccgGame game,
                                   String botPlayerId,
                                   PhysicalCard target,
                                   boolean projectsOccupation) {
        if (game == null || botPlayerId == null || target == null) {
            return Exposure.unknown(false);
        }
        GameState gameState = game.getGameState();
        ModifiersQuerying modifiers = game.getModifiersQuerying();
        if (gameState == null || modifiers == null) {
            return Exposure.unknown(false);
        }
        if (!projectsOccupation) {
            return new Exposure(true, false, false, 0.0f, 0);
        }

        String opponentId = gameState.getOpponent(botPlayerId);
        if (opponentId == null) {
            return Exposure.unknown(false);
        }

        boolean triggerKnowable;
        try {
            triggerKnowable = AiBoardAnalyzer.getCardCountAtLocation(
                    game, opponentId, target) == 0
                    && modifiers.getForceDrainAmount(
                    gameState, target, botPlayerId) > 0.0f
                    && !modifiers.isProhibitedFromForceDrainingAtLocation(
                    gameState, target, botPlayerId);
        } catch (RuntimeException e) {
            return Exposure.unknown(false);
        }
        if (!triggerKnowable) {
            return new Exposure(true, false, false, 0.0f, 0);
        }

        ScanState scan = new ScanState();
        if (playerMayNotReactToTarget(
                gameState, modifiers, opponentId, target, scan)) {
            return new Exposure(scan.complete, true, false, 0.0f, 0);
        }

        Collection<PhysicalCard> movers = activePublicCards(
                game, opponentId,
                Filters.or(Filters.character, Filters.starship,
                        Filters.vehicle), scan);
        Collection<PhysicalCard> sources = activePublicCards(
                game, null, Filters.any, scan);

        float strongest = 0.0f;
        int legalMovers = 0;
        for (PhysicalCard mover : movers) {
            if (mover == null || mover.getBlueprint() == null
                    || cardMayNotReact(gameState, modifiers, mover, scan)) {
                continue;
            }

            boolean routeProven = intrinsicRouteProven(
                    game, gameState, modifiers, opponentId,
                    mover, target, scan);
            if (!routeProven) {
                routeProven = grantedRouteProven(
                        game, gameState, modifiers, opponentId,
                        mover, target, sources, scan);
            }
            if (!routeProven) {
                continue;
            }

            legalMovers++;
            try {
                float effectivePower = modifiers.getPower(gameState, mover);
                if (mover.getBlueprint().getCardCategory()
                        == CardCategory.CHARACTER) {
                    effectivePower += FormationSafety.weaponBonusOf(
                            gameState, mover);
                }
                if (!Float.isFinite(effectivePower)) {
                    scan.complete = false;
                    continue;
                }
                strongest = Math.max(strongest, effectivePower);
            } catch (RuntimeException e) {
                scan.complete = false;
            }
        }

        return new Exposure(scan.complete, true, legalMovers > 0,
                strongest, legalMovers);
    }

    private static Collection<PhysicalCard> activePublicCards(
            SwccgGame game,
            String owner,
            Filter cardFilter,
            ScanState scan) {
        try {
            Filter ownerFilter = owner != null
                    ? Filters.owner(owner) : Filters.any;
            return Filters.filterActive(game, null,
                    Filters.and(ownerFilter, Filters.in_play,
                            cardFilter));
        } catch (RuntimeException e) {
            scan.complete = false;
            return List.of();
        }
    }

    private static boolean playerMayNotReactToTarget(
            GameState gameState,
            ModifiersQuerying modifiers,
            String opponentId,
            PhysicalCard target,
            ScanState scan) {
        try {
            for (Modifier modifier : modifiers.getModifiersAffectingCard(
                    gameState, ModifierType.MAY_NOT_REACT_TO_LOCATION,
                    target)) {
                if (modifier.isForPlayer(opponentId)) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            scan.complete = false;
            return true;
        }
        return false;
    }

    private static boolean cardMayNotReact(
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard mover,
            ScanState scan) {
        try {
            return !modifiers.getModifiersAffectingCard(
                    gameState, ModifierType.MAY_NOT_REACT, mover).isEmpty();
        } catch (RuntimeException e) {
            scan.complete = false;
            return true;
        }
    }

    private static boolean intrinsicRouteProven(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            String opponentId,
            PhysicalCard mover,
            PhysicalCard target,
            ScanState scan) {
        try {
            for (Modifier modifier : modifiers.getModifiersAffectingCard(
                    gameState, ModifierType.MAY_MOVE_AS_REACT_TO_LOCATION,
                    mover)) {
                if (modifier.isAffectedTarget(gameState, modifiers, target)
                        && exactRouteExists(game, opponentId, mover, target,
                        modifier.isReactForFree(),
                        modifier.getChangeInCost(), scan)) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            scan.complete = false;
        }
        return false;
    }

    private static boolean grantedRouteProven(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            String opponentId,
            PhysicalCard mover,
            PhysicalCard target,
            Collection<PhysicalCard> sources,
            ScanState scan) {
        for (PhysicalCard source : sources) {
            try {
                for (Modifier modifier : modifiers.getModifiersAffectingCard(
                        gameState,
                        ModifierType.MAY_MOVE_OTHER_CARD_AS_REACT_TO_LOCATION,
                        source)) {
                    Filter moverFilter = modifier.getCardToReactFilter();
                    Filter targetFilter = modifier.getTargetFilter();
                    if (!modifier.isForPlayer(opponentId)
                            || moverFilter == null
                            || targetFilter == null
                            || !moverFilter.accepts(
                            gameState, modifiers, mover)
                            || !modifier.isAffectedTarget(
                            gameState, modifiers, target)) {
                        continue;
                    }
                    if (exactRouteExists(game, opponentId, mover, target,
                            modifier.isReactForFree(),
                            modifier.getChangeInCost(), scan)) {
                        return true;
                    }
                }
            } catch (RuntimeException e) {
                scan.complete = false;
            }
        }
        return false;
    }

    private static boolean exactRouteExists(
            SwccgGame game,
            String playerId,
            PhysicalCard mover,
            PhysicalCard target,
            boolean forFree,
            float changeInCost,
            ScanState scan) {
        SwccgCardBlueprint blueprint = mover.getBlueprint();
        Filter exactTarget = Filters.sameCardId(target);

        if (route(scan, () -> blueprint.getMoveUsingLandspeedAction(
                playerId, game, mover, forFree, changeInCost,
                true, false, true, false, null, exactTarget))) {
            return true;
        }
        if (route(scan, () -> blueprint.getMoveUsingHyperspeedAction(
                playerId, game, mover, forFree,
                true, false, true, false, exactTarget))) {
            return true;
        }
        if (route(scan, () -> blueprint.getMoveWithoutUsingHyperspeedAction(
                playerId, game, mover, forFree,
                true, false, true, false, exactTarget))) {
            return true;
        }
        if (route(scan, () -> blueprint.getMoveUsingSectorMovementAction(
                playerId, game, mover, forFree,
                true, false, true, false, exactTarget))) {
            return true;
        }
        if (route(scan, () -> blueprint.getLandAction(
                playerId, game, mover, forFree,
                true, true, false, false, exactTarget))) {
            return true;
        }
        if (route(scan, () -> blueprint.getTakeOffAction(
                playerId, game, mover, forFree,
                true, true, false, false, exactTarget))) {
            return true;
        }
        if (route(scan, () -> blueprint.getEnterStarshipOrVehicleSiteAction(
                playerId, game, mover, forFree,
                true, true, false, exactTarget))) {
            return true;
        }
        return route(scan, () ->
                blueprint.getExitStarshipOrVehicleSiteAction(
                        playerId, game, mover, forFree,
                        true, true, false, exactTarget));
    }

    private static boolean route(ScanState scan, RouteProbe probe) {
        try {
            Action action = probe.get();
            return action != null;
        } catch (UnsupportedOperationException e) {
            scan.complete = false;
            return false;
        } catch (RuntimeException e) {
            scan.complete = false;
            return false;
        }
    }

    public record Exposure(boolean scanComplete,
                           boolean triggerKnowable,
                           boolean exposureProven,
                           float strongestMoverEffectivePower,
                           int provenLegalMoverCount) {
        private static Exposure unknown(boolean triggerKnowable) {
            return new Exposure(false, triggerKnowable,
                    false, 0.0f, 0);
        }
    }

    @FunctionalInterface
    private interface RouteProbe {
        Action get();
    }

    private static final class ScanState {
        private boolean complete = true;
    }
}
