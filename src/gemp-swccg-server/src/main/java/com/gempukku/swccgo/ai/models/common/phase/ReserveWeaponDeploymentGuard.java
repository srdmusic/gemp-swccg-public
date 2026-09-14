package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.AbstractAction;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromPileEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Read-only V313 inspection of concrete reserve-deploy effects, before commitment. */
public final class ReserveWeaponDeploymentGuard {
    public static final String BLOCKED_ACTIONS_EXTRA = "reserveWeaponBlockedActions";

    private ReserveWeaponDeploymentGuard() {
    }

    public static Set<String> readBlockedActions(
            AwaitingDecision decision, SwccgGame game, String playerId) {
        if (decision == null || game == null || playerId == null) return Set.of();
        var parameters = decision.getDecisionParameters();
        String[] ids = parameters == null ? null : parameters.get("actionId");
        if (ids == null) return Set.of();
        Set<String> blocked = new HashSet<>();
        for (String id : ids) {
            Action action = AiActionSourceProvenance.actionForId(decision, id);
            if (blocksAction(game, playerId, action)) blocked.add(id);
        }
        return Set.copyOf(blocked);
    }

    static boolean blocksAction(SwccgGame game, String playerId, Action action) {
        if (action == null || !playerId.equals(action.getPerformingPlayer())) return false;
        try {
            // Never call nextEffect/getSubAction: those consume or mutate live actions.
            for (Route route : routes(action, false)) {
                if (!route.isOurReserve(playerId)) continue;
                boolean armedOnlyWeapon = false;
                boolean usefulAlternative = false;
                for (PhysicalCard candidate : game.getGameState().getReserveDeck(playerId)) {
                    if (!route.matches(game, candidate) || !route.canDeploy(game, candidate, false)) continue;
                    if (candidate.getBlueprint().getCardCategory() != CardCategory.WEAPON
                            || route.canDeploy(game, candidate, true)) {
                        usefulAlternative = true;
                    } else {
                        armedOnlyWeapon = true;
                    }
                }
                if (armedOnlyWeapon && !usefulAlternative) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Deferred/custom effects have no proven route yet. Child guards still apply.
        }
        return false;
    }

    /** Empty Optional means unknown; an empty string proves a useful legal target exists. */
    public static Optional<String> candidateBlockReason(
            SwccgGame game, String playerId, PhysicalCard candidate) {
        if (game == null || playerId == null || candidate == null || candidate.getBlueprint() == null
                || candidate.getBlueprint().getCardCategory() != CardCategory.WEAPON
                || candidate.getZone() != Zone.RESERVE_DECK
                || !playerId.equals(candidate.getOwner())) return Optional.empty();
        try {
            var state = game.getGameState().getTopGameTextActionState();
            Action parent = state == null ? null : state.getGameTextAction();
            if (parent == null) return Optional.empty();
            List<Route> routes = routes(parent, true);
            // A multi-effect action may have a different child pending. Do not guess which.
            if (routes.size() != 1) return Optional.empty();
            Route route = routes.get(0);
            if (!route.isOurReserve(playerId) || !route.matches(game, candidate)) return Optional.empty();
            return Optional.of(route.canDeploy(game, candidate, true) ? ""
                    : "exact reserve-deploy route has no legal target without an existing weapon");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isArmedCharacter(SwccgGame game, PhysicalCard target) {
        if (game == null || target == null || target.getBlueprint() == null
                || target.getBlueprint().getCardCategory() != CardCategory.CHARACTER) return false;
        if (game.getModifiersQuerying().getPermanentWeapon(game.getGameState(), target) != null) return true;
        List<PhysicalCard> attached = game.getGameState().getAttachedCards(target);
        if (attached != null) {
            for (PhysicalCard card : attached) {
                if (card != null && card.getBlueprint() != null
                        && card.getBlueprint().getCardCategory() == CardCategory.WEAPON) return true;
            }
        }
        return false;
    }

    private static List<Route> routes(Action action, boolean includeProcessed)
            throws ReflectiveOperationException {
        List<Route> routes = new ArrayList<>();
        if (!(action instanceof AbstractAction)) return routes;
        if (!includeProcessed) {
            List<?> effects = (List<?>) field(AbstractAction.class, action, "_effects");
            List<?> after = (List<?>) field(AbstractAction.class, action, "_afterEffects");
            // Earlier result effects can create new wielders. Do not veto a future route
            // using the pre-action board or guess through deferred callbacks.
            if (effects.size() != 1 || !after.isEmpty()) return routes;
        }
        for (String name : includeProcessed
                ? List.of("_effects", "_processedEffects", "_afterEffects", "_processedAfterEffects")
                : List.of("_effects", "_afterEffects")) {
            for (Object effect : List.copyOf((List<?>) field(AbstractAction.class, action, name))) {
                if (effect instanceof DeployCardFromPileEffect deploy) routes.add(new Route(action, deploy));
            }
        }
        return routes;
    }

    private static Object field(Class<?> owner, Object object, String name)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static final class Route {
        private final Action action;
        private final DeployCardFromPileEffect effect;

        private Route(Action action, DeployCardFromPileEffect effect) {
            this.action = action;
            this.effect = effect;
        }

        private Object read(String name) throws ReflectiveOperationException {
            return field(DeployCardFromPileEffect.class, effect, name);
        }

        private boolean isOurReserve(String player) throws ReflectiveOperationException {
            return read("_cardPile") == Zone.RESERVE_DECK
                    && player.equals(read("_playerId")) && player.equals(read("_cardPileOwner"));
        }

        private boolean matches(SwccgGame game, PhysicalCard candidate) throws ReflectiveOperationException {
            PhysicalCard exact = (PhysicalCard) read("_cardToDeploy");
            Filter cards = (Filter) read("_cardFilter");
            return candidate != null && candidate.getBlueprint() != null
                    && (exact != null ? candidate == exact : cards != null && cards.accepts(game, candidate));
        }

        private boolean canDeploy(SwccgGame game, PhysicalCard candidate, boolean requireUnarmed)
                throws ReflectiveOperationException {
            // Mirror the engine's query flags without executing or rewriting its effect.
            Filter target = (Filter) read("_targetFilter");
            Filter ignoreTarget = (Filter) read("_ignoreTargetFilterCardFilter");
            String system = (String) read("_targetSystem");
            // Alternate-target and named-system routes need a separate exact preview.
            // Do not approximate those contracts or fabricate an all-armed verdict.
            if (ignoreTarget != null || system != null) {
                throw new IllegalStateException("special target route not materialized");
            }
            if (requireUnarmed) {
                Filter unarmed = new Filter() {
                    @Override
                    public boolean accepts(GameState state, ModifiersQuerying modifiers, PhysicalCard host) {
                        return !isArmedCharacter(game, host);
                    }
                };
                target = target == null ? unarmed : Filters.and(target, unarmed);
            }
            boolean forFree = (Boolean) read("_forFree");
            Filter freeCards = (Filter) read("_forFreeCardFilter");
            if (read("_cardToDeploy") != null && freeCards != null) {
                throw new IllegalStateException("exact-card free override not materialized");
            }
            float cost = (Float) read("_changeInCost");
            if (!forFree && freeCards != null && freeCards.accepts(game, candidate)) {
                forFree = true;
                cost = 0;
            }
            Filter changedCostCards = (Filter) read("_changeInCostCardFilter");
            DeploymentOption option = (DeploymentOption) read("_deploymentOption");
            DeploymentRestrictionsOption restrictions = (DeploymentRestrictionsOption) read("_deploymentRestrictionsOption");
            DeployAsCaptiveOption captive = (DeployAsCaptiveOption) read("_deployAsCaptiveOption");
            Filter locations = (Filter) read("_specialLocationConditions");
            // Reacts carry extra temporal restrictions; leave their unmaterialized parent unknown.
            if ((Boolean) read("_asReact")) throw new IllegalStateException("react route not materialized");
            Filter legal;
            if (target != null) {
                legal = Filters.deployableToTarget(action.getActionSource(), target, false,
                        forFree, cost, changedCostCards, option, restrictions, captive, null);
            } else {
                legal = Filters.deployable(action.getActionSource(), false, locations,
                        forFree, cost, changedCostCards, option, restrictions, null);
            }
            return legal.accepts(game, candidate);
        }
    }
}
