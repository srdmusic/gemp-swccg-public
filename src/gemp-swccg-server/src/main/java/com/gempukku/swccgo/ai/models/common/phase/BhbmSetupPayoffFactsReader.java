package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;

/**
 * Reads the two payoff effects deployed by Bring Him Before Me during setup.
 *
 * <p>Only the exact original physical effects receive objective credit.
 * Unknown source, target, suspension, or board state fails closed.
 */
public final class BhbmSetupPayoffFactsReader {
    private static final String INSIGNIFICANT_REBELLION = "9_127";
    private static final String YOUR_DESTINY = "9_134";

    private BhbmSetupPayoffFactsReader() {
    }

    /**
     * Mirrors Your Destiny's live 3-Force condition for a proposed direct
     * Vader or open-carrier destination. The target may be off table, but may
     * not be captured, out of play, or present at a battleground site.
     */
    public static boolean rewardsVaderAtBattleground(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate,
            PhysicalCard destination) {
        if (!isBhbm(analyzer)
                || game == null || playerId == null
                || candidate == null || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || projectedOwnedVader(
                    game, playerId, candidate) == null) {
            return false;
        }
        try {
            PhysicalCard yourDestiny =
                    exactActive(
                        game, playerId, YOUR_DESTINY);
            PhysicalCard presentAt =
                    proposedPresentAt(
                        game, destination);
            if (yourDestiny == null
                    || presentAt == null
                    || !Filters.battleground_site.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        presentAt)) {
                return false;
            }

            Filter target = targetFilter(
                    game, yourDestiny);
            PhysicalCard suppressingTarget =
                    Filters.findFirstActive(
                        game,
                        yourDestiny,
                        SpotOverride.INCLUDE_CAPTIVE,
                        Filters.and(
                            target,
                            Filters.or(
                                Filters.captive,
                                Filters.presentAt(
                                    Filters.battleground_site))));
            if (suppressingTarget != null) {
                return false;
            }

            Filter outOfPlayTarget = Filters.or(
                    target,
                    Filters.hasPermanentAboard(target),
                    Filters.hasPermanentWeapon(target));
            for (PhysicalCard card
                    : game.getGameState()
                        .getAllOutOfPlayCards()) {
                if (card != null
                        && outOfPlayTarget.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            card)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * A moving open vehicle projects the presence of its active Vader
     * passenger. Enclosed vehicles and starships deliberately do not.
     */
    static PhysicalCard projectedOwnedVader(
            SwccgGame game,
            String playerId,
            PhysicalCard candidate) {
        if (game == null || playerId == null
                || candidate == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || !playerId.equals(
                    candidate.getOwner())) {
            return null;
        }
        try {
            if (Filters.Vader.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(),
                    candidate)) {
                return candidate;
            }
            if (candidate.getBlueprint() == null
                    || candidate.getBlueprint()
                        .getCardCategory()
                        != CardCategory.VEHICLE
                    || game.getModifiersQuerying()
                        .hasKeyword(
                            game.getGameState(),
                            candidate,
                            Keyword.ENCLOSED)) {
                return null;
            }
            for (PhysicalCard vader
                    : Filters.filterActive(
                        game, candidate,
                        Filters.and(
                            Filters.owner(playerId),
                            Filters.Vader))) {
                if (vader != null
                        && Filters.hasAboard(vader)
                            .accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            candidate)) {
                    return vader;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static boolean hasOtherActiveFriendlyCharacterAboard(
            SwccgGame game,
            String playerId,
            PhysicalCard carrier,
            PhysicalCard vader) {
        if (game == null || playerId == null
                || carrier == null || vader == null
                || carrier.getBlueprint() == null
                || carrier.getBlueprint()
                    .getCardCategory()
                    != CardCategory.VEHICLE
                || game.getGameState() == null) {
            return false;
        }
        try {
            if (Filters.hasPermanentAboard(
                    Filters.character).accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        carrier)) {
                return true;
            }
            for (PhysicalCard aboard
                    : game.getGameState()
                        .getAboardCards(
                            carrier, false)) {
                if (aboard == null
                        || aboard.getPermanentCardId()
                            == vader.getPermanentCardId()
                        || aboard.getBlueprint() == null
                        || aboard.getBlueprint()
                            .getCardCategory()
                            != CardCategory.CHARACTER
                        || !playerId.equals(
                            aboard.getOwner())
                        || aboard.isUndercover()) {
                    continue;
                }
                if (game.getGameState()
                        .isCardInPlayActive(
                            aboard,
                            false, false, false,
                            false, false, false,
                            false, false)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }

    /**
     * Formation proof shared by movement parents and destination children.
     * Open carriers are assessed through their exact active Vader passenger,
     * because character-only FormationSafety deliberately ignores vehicles.
     */
    public static boolean projectedVaderMoveFormationSafe(
            SwccgGame game,
            String playerId,
            PhysicalCard mover,
            PhysicalCard destination) {
        if (game == null || playerId == null
                || mover == null || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            PhysicalCard vader =
                    projectedOwnedVader(
                        game, playerId, mover);
            if (vader == null
                    || (mover.getPermanentCardId()
                        != vader.getPermanentCardId()
                        && hasOtherActiveFriendlyCharacterAboard(
                            game, playerId, mover, vader))) {
                return false;
            }
            PhysicalCard origin =
                    game.getModifiersQuerying()
                        .getLocationThatCardIsAt(
                            game.getGameState(), mover);
            if (origin == null) {
                origin = game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(
                            game.getGameState(), mover);
            }
            PhysicalCard effectiveDestination =
                    proposedPresentAt(
                        game, destination);
            if (origin == null
                    || effectiveDestination == null) {
                return false;
            }
            if (origin.getPermanentCardId()
                    == effectiveDestination
                        .getPermanentCardId()) {
                PhysicalCard currentPresence =
                        game.getModifiersQuerying()
                            .getLocationThatCardIsPresentAt(
                                game.getGameState(), vader);
                if (currentPresence != null
                        && currentPresence
                            .getPermanentCardId()
                            == effectiveDestination
                                .getPermanentCardId()) {
                    return true;
                }
                return FormationSafety
                        .vetoMoveDestination(
                            game, game.getGameState(),
                            playerId, vader,
                            effectiveDestination) == null;
            }
            return FormationSafety.vetoMoveOrigin(
                        game, game.getGameState(),
                        playerId, vader, origin) == null
                    && FormationSafety.vetoMoveDestination(
                        game, game.getGameState(),
                        playerId, vader,
                        effectiveDestination) == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * True only when this exact mover already projects Vader presence that
     * satisfies Your Destiny's live battleground condition. Parent movement
     * scoring rewards creating the clock, not paying Force to preserve it.
     */
    public static boolean currentlyRewardsVader(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate) {
        if (game == null || candidate == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            PhysicalCard presentAt =
                    game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(
                            game.getGameState(),
                            candidate);
            return rewardsVaderAtBattleground(
                    game, playerId, analyzer,
                    candidate, presentAt);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Parent deploy credit requires one exact engine-legal destination whose
     * formation verdict is already known safe. This keeps an explicit deploy
     * action from receiving Your Destiny credit that its destination child
     * would later reject.
     */
    public static boolean rewardsVaderForDeployAt(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate,
            PhysicalCard destination) {
        if (!rewardsVaderAtBattleground(
                game, playerId, analyzer,
                candidate, destination)
                || game.getGameState() == null
                || candidate.getBlueprint() == null) {
            return false;
        }
        try {
            if (!Filters.deployableToTarget(
                    candidate,
                    Filters.sameCardId(destination),
                    false, 0.0f).accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        candidate)) {
                return false;
            }
            PhysicalCard effectiveSite =
                    proposedPresentAt(
                        game, destination);
            if (effectiveSite == null) {
                return false;
            }
            var blueprint = candidate.getBlueprint();
            FormationSafety.DeployVerdict verdict =
                    FormationSafety.assessCharacterDeploy(
                        game, game.getGameState(),
                        playerId, candidate,
                        blueprint.hasPowerAttribute()
                            ? blueprint.getPower() : null,
                        blueprint.hasAbilityAttribute()
                            ? blueprint.getAbility() : null,
                        candidate.isUndercover(),
                        effectiveSite,
                        game.getGameState()
                            .getForcePileSize(playerId),
                        blueprint.getDeployCost(),
                        null, null);
            return verdict.constraint()
                    == FormationSafety.DeployConstraint.ALLOW
                    && FormationSafety
                        .vetoMoveDestination(
                            game, game.getGameState(),
                            playerId, candidate,
                            effectiveSite) == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * True when a generic deploy parent has at least one legal, formation-safe
     * Your Destiny destination. Explicit-destination parents use
     * {@link #rewardsVaderForDeployAt} directly.
     */
    public static boolean hasLegalYourDestinyDeployDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate) {
        if (game == null || game.getGameState() == null) {
            return false;
        }
        try {
            for (PhysicalCard destination
                    : game.getGameState()
                        .getAllPermanentCards()) {
                if (destination != null
                        && destination.getZone() != null
                        && destination.getZone()
                            .isInPlay()
                        && rewardsVaderForDeployAt(
                            game, playerId, analyzer,
                            candidate, destination)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * The original Insignificant Rebellion pays off every projected-safe
     * battle win with 1 Force loss and a +3 crossover stack.
     */
    public static boolean insignificantRebellionActive(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        return isBhbm(analyzer)
                && exactActive(
                    game, playerId,
                    INSIGNIFICANT_REBELLION) != null;
    }

    private static Filter targetFilter(
            SwccgGame game,
            PhysicalCard objective) {
        if (game.getModifiersQuerying()
                .hasGameTextModification(
                    game.getGameState(),
                    objective,
                    ModifyGameTextType
                        .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE)) {
            return Filters.Leia;
        }
        if (game.getModifiersQuerying()
                .hasGameTextModification(
                    game.getGameState(),
                    objective,
                    ModifyGameTextType
                        .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE)) {
            return Filters.Kanan;
        }
        return Filters.Luke;
    }

    private static PhysicalCard exactActive(
            SwccgGame game,
            String playerId,
            String blueprintId) {
        if (game == null || playerId == null
                || blueprintId == null
                || game.getGameState() == null) {
            return null;
        }
        try {
            for (PhysicalCard card
                    : game.getGameState()
                        .getAllPermanentCards()) {
                if (card != null
                        && playerId.equals(card.getOwner())
                        && blueprintId.equals(
                            normalizeBlueprintId(
                                card.getBlueprintId(true)))
                        && game.getGameState()
                            .isCardInPlayActive(
                                card,
                                false, false, false,
                                false, false, false,
                                false, false)) {
                    return card;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    static PhysicalCard proposedPresentAt(
            SwccgGame game,
            PhysicalCard destination) {
        if (game == null || destination == null
                || destination.getBlueprint() == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        CardCategory category =
                destination.getBlueprint()
                    .getCardCategory();
        if (category == CardCategory.LOCATION) {
            return destination;
        }
        if (category != CardCategory.VEHICLE
                || game.getModifiersQuerying()
                    .hasKeyword(
                        game.getGameState(),
                        destination,
                        Keyword.ENCLOSED)) {
            return null;
        }
        return game.getModifiersQuerying()
                .getLocationThatCardIsPresentAt(
                    game.getGameState(),
                    destination);
    }

    private static boolean isBhbm(
            ObjectiveAnalyzer analyzer) {
        return CaptureObjectiveFacts.objectiveKind(analyzer)
                == CaptureObjectivePolicy.ObjectiveKind.BHBM;
    }

    private static String normalizeBlueprintId(
            String blueprintId) {
        if (blueprintId == null) {
            return null;
        }
        String normalized = blueprintId.endsWith("_BACK")
                ? blueprintId.substring(
                    0, blueprintId.length() - 5)
                : blueprintId;
        String[] parts = normalized.split("_");
        if (parts.length != 2) {
            return normalized;
        }
        try {
            return Integer.parseInt(parts[0])
                    + "_" + Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return normalized;
        }
    }
}
