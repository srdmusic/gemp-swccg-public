package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Reads exact engine facts for the V308 Kylo battle-kit preference. */
public final class KyloBattleKitFactsReader {
    private KyloBattleKitFactsReader() {
    }

    public static Optional<DeployWeaponPolicy.KyloBattleKitActionFacts>
            readAction(
            String actionId,
            SwccgGame game,
            String playerId,
            PhysicalCard sourceCard,
            PhysicalCard saberCard,
            String plannedBattleTargetLocationId,
            boolean forFree,
            float changeInCost) {
        Context context = context(
                actionId, game, playerId, sourceCard, saberCard);
        if (context == null) {
            return Optional.empty();
        }

        try {
            Collection<PhysicalCard> cards =
                    context.gameState().getAllPermanentCards();
            if (cards == null) {
                return Optional.empty();
            }

            boolean hasEngineLegalKyloTarget = false;
            boolean hasEngineLegalGroundSiteKyloTarget = false;
            boolean hasKyloAtPlannedBattleTarget = false;
            boolean hasEngineLegalPlannedBattleTarget = false;
            boolean battleCanInitiate = false;
            Budget leastExpensivePlannedRoute = null;

            for (PhysicalCard target : cards) {
                if (!isOwnedActiveKylo(context, target)) {
                    continue;
                }
                TargetPosition position = position(context, target);
                boolean atPlannedTarget = matchesPlannedTarget(
                        position.location(),
                        plannedBattleTargetLocationId);
                if (atPlannedTarget) {
                    hasKyloAtPlannedBattleTarget = true;
                }

                boolean deployLegal = engineDeployLegal(
                        context, target, forFree, changeInCost);
                if (!deployLegal) {
                    continue;
                }
                hasEngineLegalKyloTarget = true;

                boolean groundSiteTarget = position.atSite()
                        && !position.aboardStarship()
                        && isUnarmed(context.gameState(), target);
                if (!groundSiteTarget) {
                    continue;
                }
                hasEngineLegalGroundSiteKyloTarget = true;
                if (!atPlannedTarget) {
                    continue;
                }

                hasEngineLegalPlannedBattleTarget = true;
                battleCanInitiate |= canInitiateOwnTurnBattle(
                        context, position.location());
                Budget budget = exactBudget(
                        context, target, position.location(),
                        forFree, changeInCost);
                if (budget != null
                        && (leastExpensivePlannedRoute == null
                        || budget.totalPayment()
                        < leastExpensivePlannedRoute.totalPayment())) {
                    leastExpensivePlannedRoute = budget;
                }
            }

            int deployPayment = leastExpensivePlannedRoute == null
                    ? -1 : leastExpensivePlannedRoute.deployPayment();
            int battlePayment = leastExpensivePlannedRoute == null
                    ? -1 : leastExpensivePlannedRoute.battlePayment();
            int forceAvailable = leastExpensivePlannedRoute == null
                    ? -1 : leastExpensivePlannedRoute.forceAvailable();
            return Optional.of(
                    new DeployWeaponPolicy.KyloBattleKitActionFacts(
                            actionId, true,
                            hasEngineLegalKyloTarget,
                            hasEngineLegalGroundSiteKyloTarget,
                            hasKyloAtPlannedBattleTarget,
                            hasEngineLegalPlannedBattleTarget,
                            battleCanInitiate,
                            deployPayment,
                            battlePayment,
                            forceAvailable));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<DeployWeaponPolicy.KyloBattleKitDestinationFacts>
            readDestination(
            String actionId,
            SwccgGame game,
            String playerId,
            PhysicalCard sourceCard,
            PhysicalCard saberCard,
            PhysicalCard target,
            String plannedBattleTargetLocationId,
            boolean forFree,
            float changeInCost) {
        Context context = context(
                actionId, game, playerId, sourceCard, saberCard);
        if (context == null) {
            return Optional.empty();
        }

        try {
            boolean kyloTarget = isOwnedActiveKylo(context, target);
            if (!kyloTarget) {
                return Optional.of(
                        new DeployWeaponPolicy.KyloBattleKitDestinationFacts(
                                actionId, true, false,
                                false, false, false, false,
                                false, false, -1, -1, -1));
            }

            TargetPosition position = position(context, target);
            boolean unarmed = isUnarmed(context.gameState(), target);
            boolean deployLegal = engineDeployLegal(
                    context, target, forFree, changeInCost);
            boolean atPlannedTarget = matchesPlannedTarget(
                    position.location(),
                    plannedBattleTargetLocationId);
            boolean validGroundRoute = deployLegal
                    && position.atSite()
                    && !position.aboardStarship()
                    && unarmed;
            boolean battleCanInitiate = validGroundRoute
                    && atPlannedTarget
                    && canInitiateOwnTurnBattle(
                            context, position.location());
            Budget budget = validGroundRoute && atPlannedTarget
                    ? exactBudget(context, target, position.location(),
                            forFree, changeInCost)
                    : null;

            return Optional.of(
                    new DeployWeaponPolicy.KyloBattleKitDestinationFacts(
                            actionId, true, true,
                            deployLegal,
                            position.atSite(),
                            position.aboardStarship(),
                            unarmed,
                            atPlannedTarget,
                            battleCanInitiate,
                            budget == null ? -1 : budget.deployPayment(),
                            budget == null ? -1 : budget.battlePayment(),
                            budget == null ? -1 : budget.forceAvailable()));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Context context(
            String actionId,
            SwccgGame game,
            String playerId,
            PhysicalCard sourceCard,
            PhysicalCard saberCard) {
        if (actionId == null || game == null || playerId == null
                || saberCard == null
                || !playerId.equals(saberCard.getOwner())
                || saberCard.getBlueprint() == null
                || saberCard.getBlueprint().getCardCategory()
                    != CardCategory.WEAPON
                || !saberCard.getBlueprint()
                    .hasPersona(Persona.KYLOS_LIGHTSABER)
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        return new Context(
                game, game.getGameState(), game.getModifiersQuerying(),
                playerId, sourceCard == null ? saberCard : sourceCard,
                saberCard);
    }

    private static boolean isOwnedActiveKylo(
            Context context,
            PhysicalCard target) {
        return target != null
                && context.playerId().equals(target.getOwner())
                && target.getZone() != null
                && target.getZone().isInPlay()
                && target.getBlueprint() != null
                && target.getBlueprint().getCardCategory()
                    == CardCategory.CHARACTER
                && Filters.Kylo.accepts(
                    context.gameState(), context.modifiers(), target);
    }

    private static TargetPosition position(
            Context context,
            PhysicalCard target) {
        PhysicalCard location = context.modifiers()
                .getLocationThatCardIsAt(
                        context.gameState(), target);
        boolean atSite = location != null
                && location.getZone() == Zone.LOCATIONS
                && location.getBlueprint() != null
                && location.getBlueprint().getCardSubtype()
                    == CardSubtype.SITE;
        boolean aboardStarship = Filters.aboardAnyStarship.accepts(
                context.gameState(), context.modifiers(), target);
        return new TargetPosition(
                location, atSite, aboardStarship);
    }

    private static boolean isUnarmed(
            GameState gameState,
            PhysicalCard target) {
        if (target.getBlueprint().hasIcon(Icon.PERMANENT_WEAPON)) {
            return false;
        }
        List<PhysicalCard> attachments =
                gameState.getAttachedCards(target);
        if (attachments == null) {
            return false;
        }
        for (PhysicalCard attachment : attachments) {
            if (attachment != null
                    && attachment.getBlueprint() != null
                    && attachment.getBlueprint().getCardCategory()
                        == CardCategory.WEAPON) {
                return false;
            }
        }
        return true;
    }

    private static boolean engineDeployLegal(
            Context context,
            PhysicalCard target,
            boolean forFree,
            float changeInCost) {
        return context.modifiers().isDeployableToTarget(
                context.gameState(),
                context.sourceCard(),
                context.saberCard(),
                false,
                Filters.sameCardId(target),
                forFree,
                changeInCost,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                0.0f);
    }

    private static boolean matchesPlannedTarget(
            PhysicalCard location,
            String plannedBattleTargetLocationId) {
        return location != null
                && plannedBattleTargetLocationId != null
                && !plannedBattleTargetLocationId.isBlank()
                && plannedBattleTargetLocationId.equals(
                    String.valueOf(location.getCardId()));
    }

    private static boolean canInitiateOwnTurnBattle(
            Context context,
            PhysicalCard location) {
        if (location == null
                || !context.playerId().equals(
                    context.gameState().getCurrentPlayerId())) {
            return false;
        }
        return GameConditions.canInitiateBattleAtLocation(
                    context.playerId(), context.game(), location,
                    true, true)
                || GameConditions.canInitiateBattleAtLocation(
                    context.playerId(), context.game(), location,
                    false, true);
    }

    private static Budget exactBudget(
            Context context,
            PhysicalCard target,
            PhysicalCard location,
            boolean forFree,
            float changeInCost) {
        float rawDeploy = context.modifiers().getDeployCost(
                context.gameState(),
                context.sourceCard(),
                context.saberCard(),
                target,
                false,
                null,
                forFree,
                changeInCost,
                null,
                true);
        float rawBattle = context.modifiers().getInitiateBattleCost(
                context.gameState(), location,
                context.playerId(), true);
        Integer deployPayment = exactPayment(rawDeploy);
        Integer battlePayment = exactPayment(rawBattle);
        if (deployPayment == null || battlePayment == null) {
            return null;
        }
        int forceAvailable = GameConditions.forceAvailableToUse(
                context.game(), context.playerId());
        return forceAvailable < 0 ? null
                : new Budget(
                    deployPayment, battlePayment, forceAvailable);
    }

    private static Integer exactPayment(float rawCost) {
        if (!Float.isFinite(rawCost)) {
            return null;
        }
        double rounded = Math.ceil(Math.max(0.0f, rawCost));
        return rounded > Integer.MAX_VALUE
                ? null : (int) rounded;
    }

    private record Context(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            String playerId,
            PhysicalCard sourceCard,
            PhysicalCard saberCard) {
    }

    private record TargetPosition(
            PhysicalCard location,
            boolean atSite,
            boolean aboardStarship) {
    }

    private record Budget(
            int deployPayment,
            int battlePayment,
            int forceAvailable) {
        private long totalPayment() {
            return (long) deployPayment + battlePayment;
        }
    }
}
