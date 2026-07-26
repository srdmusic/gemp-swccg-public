package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Source-backed table facts for There Is Good In Him and Bring Him Before Me.
 * Unknown or incomplete engine state fails closed.
 */
public final class CaptureObjectiveFacts {
    private static final String TIGIH_VIRTUAL_HUT = "214_19";
    private static final String BHBM_VADERS_CASTLE = "209_50";

    private CaptureObjectiveFacts() {
    }

    public static CaptureObjectivePolicy.ObjectiveKind objectiveKind(
            ObjectiveAnalyzer analyzer) {
        if (analyzer == null || !analyzer.isAnalyzed()) {
            return null;
        }
        String blueprintId = normalizeBlueprintId(
                analyzer.getObjectiveBlueprintId());
        if ("9_61".equals(blueprintId)) {
            return CaptureObjectivePolicy.ObjectiveKind.TIGIH;
        }
        if ("9_151".equals(blueprintId)) {
            return CaptureObjectivePolicy.ObjectiveKind.BHBM;
        }
        return null;
    }

    public static boolean isCaptureObjective(
            ObjectiveAnalyzer analyzer) {
        return objectiveKind(analyzer) != null;
    }

    /**
     * True only when moving or deploying this exact actor to this exact site
     * causes the objective's mandatory capture trigger to become legal.
     * Off-table deploy candidates additionally require a known
     * FormationSafety ALLOW verdict for this exact destination.
     */
    public static boolean guaranteesImmediateCaptureAt(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor,
            PhysicalCard destination) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || analyzer.isFlipped()
                || game == null || playerId == null
                || actor == null || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || !playerId.equals(actor.getOwner())) {
            return false;
        }
        try {
            PhysicalCard objective =
                    findObjective(game, playerId, kind);
            PhysicalCard target =
                    findTarget(game, playerId, kind, objective);
            if (objective == null || target == null
                    || target.isCaptive()) {
                return false;
            }

            GameState gameState = game.getGameState();
            if (kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH) {
                return samePhysicalCard(actor, target)
                        && Filters.Luke.accepts(
                            gameState,
                            game.getModifiersQuerying(), actor)
                        && hasEligibleImperialAt(
                            game, target, destination)
                        && captureDeployFormationSafeIfNeeded(
                            game, playerId, actor,
                            destination);
            }

            PhysicalCard vader =
                    BhbmSetupPayoffFactsReader
                        .projectedOwnedVader(
                            game, playerId, actor);
            if (vader == null
                    || vader.isLeavingTable()
                    || vader.getCardsEscorting() != null
                        && !vader.getCardsEscorting().isEmpty()
                    || captureOfDefaultLukeIsProhibited(
                        game, objective)
                    || !Filters.canBeTargetedBy(
                            objective,
                            TargetingReason.TO_BE_CAPTURED)
                        .accepts(
                            gameState,
                            game.getModifiersQuerying(), target)) {
                return false;
            }
            PhysicalCard projectedVaderPresence =
                    BhbmSetupPayoffFactsReader
                        .proposedPresentAt(
                            game, destination);
            PhysicalCard targetPresence =
                    game.getModifiersQuerying()
                        .getCardIsPresentAt(
                            gameState, target);
            return samePhysicalCard(
                        targetPresence,
                        projectedVaderPresence)
                    && captureDeployFormationSafeIfNeeded(
                        game, playerId, vader,
                        projectedVaderPresence);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasLegalImmediateCaptureDeployDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor) {
        if (game == null || playerId == null || actor == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            Set<PhysicalCard> destinations =
                    Collections.newSetFromMap(
                        new IdentityHashMap<>());
            Collection<PhysicalCard> locations =
                    game.getGameState()
                        .getLocationsInOrder();
            Collection<PhysicalCard> permanents =
                    game.getGameState()
                        .getAllPermanentCards();
            if (locations != null) {
                destinations.addAll(locations);
            }
            if (permanents != null) {
                destinations.addAll(permanents);
            }
            for (PhysicalCard destination : destinations) {
                if (destination == null
                        || !guaranteesImmediateCaptureAt(
                            game, playerId, analyzer,
                            actor, destination)
                        || !Filters.deployableToTarget(
                            actor,
                            Filters.sameCardId(destination),
                            false, 0.0f).accepts(
                                game.getGameState(),
                                game.getModifiersQuerying(),
                                actor)) {
                    continue;
                }
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean hasLegalImmediateCaptureMoveDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor) {
        if (game == null || playerId == null || actor == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            Filter legal = Filters.canMoveToUsingLandspeed(
                    playerId, actor,
                    false, false, false,
                    0.0f, null);
            for (PhysicalCard destination
                    : game.getGameState()
                        .getLocationsInOrder()) {
                if (destination != null
                        && legal.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)
                        && guaranteesImmediateCaptureAt(
                            game, playerId, analyzer,
                            actor, destination)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * The parent movement decision may claim the mandatory capture band only
     * when at least one exact legal capture destination also survives the same
     * FormationSafety checks enforced by the destination child.
     */
    public static boolean hasFormationSafeLegalImmediateCaptureMoveDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor) {
        if (game == null || playerId == null || actor == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            PhysicalCard origin = locationOf(game, actor);
            if (origin == null) {
                return false;
            }
            Filter legal = Filters.canMoveToUsingLandspeed(
                    playerId, actor,
                    false, false, false,
                    0.0f, null);
            for (PhysicalCard destination
                    : game.getGameState()
                        .getLocationsInOrder()) {
                if (destination != null
                        && legal.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)
                        && guaranteesImmediateCaptureAt(
                            game, playerId, analyzer,
                            actor, destination)
                        && formationSafeMoveDestination(
                            game, playerId, actor,
                            origin, destination)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * True when this legal landspeed destination is a strictly closer step
     * toward the exact source-backed capture condition. Immediate capture
     * destinations are owned by the stronger capture-route policy.
     */
    public static boolean advancesCaptureApproachByLandspeed(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor,
            PhysicalCard destination) {
        if (game == null || playerId == null
                || actor == null || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            PhysicalCard origin = locationOf(game, actor);
            return origin != null
                    && Filters.canMoveToUsingLandspeed(
                        playerId, actor,
                        false, false, false,
                        0.0f, null)
                        .accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)
                    && advancesCaptureApproachAt(
                        game, playerId, analyzer,
                        actor, origin, destination);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Source-backed capture geometry after the caller has proved the exact
     * movement mechanism's own legality. This deliberately does not assume
     * landspeed, so Castle, relocation, and transport routes share the same
     * TIGIH/BHBM target and endpoint contract.
     */
    public static boolean advancesCaptureApproachAt(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor,
            PhysicalCard origin,
            PhysicalCard destination) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || analyzer.isFlipped()
                || game == null || playerId == null
                || actor == null || origin == null
                || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || !playerId.equals(actor.getOwner())
                || !isActive(game, actor, false)) {
            return false;
        }
        try {
            PhysicalCard objective =
                    findObjective(game, playerId, kind);
            PhysicalCard target =
                    findTarget(game, playerId, kind, objective);
            if (objective == null || target == null
                    || target.isCaptive()
                    || kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                        && virtualHutActionGuaranteesCapture(
                            game, playerId, analyzer)) {
                return false;
            }

            GameState gameState = game.getGameState();
            if (kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH) {
                if (!samePhysicalCard(actor, target)
                        || !Filters.Luke.accepts(
                            gameState,
                            game.getModifiersQuerying(), actor)) {
                    return false;
                }
            } else {
                PhysicalCard vader =
                        BhbmSetupPayoffFactsReader
                            .projectedOwnedVader(
                                game, playerId, actor);
                if (vader == null
                        || vader.isLeavingTable()
                        || vader.getCardsEscorting() != null
                            && !vader.getCardsEscorting().isEmpty()
                        || captureOfDefaultLukeIsProhibited(
                            game, objective)
                        || !Filters.canBeTargetedBy(
                                objective,
                                TargetingReason.TO_BE_CAPTURED)
                            .accepts(
                                gameState,
                                game.getModifiersQuerying(),
                                target)) {
                    return false;
                }
            }

            if (samePhysicalCard(origin, destination)
                    || guaranteesImmediateCaptureAt(
                        game, playerId, analyzer,
                        actor, destination)) {
                return false;
            }

            if (kind == CaptureObjectivePolicy.ObjectiveKind.BHBM) {
                PhysicalCard targetLocation =
                        locationOf(game, target);
                return targetLocation != null
                        && Filters.toward(
                            origin, targetLocation)
                        .accepts(
                            gameState,
                            game.getModifiersQuerying(),
                            destination);
            }

            List<PhysicalCard> locations =
                    gameState.getLocationsInOrder();
            if (locations == null) {
                return false;
            }
            for (PhysicalCard captureSite : locations) {
                if (captureSite != null
                        && hasEligibleImperialAt(
                            game, target, captureSite)
                        && Filters.toward(
                            origin, captureSite)
                            .accepts(
                                gameState,
                                game.getModifiersQuerying(),
                                destination)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean hasLegalCaptureApproachMoveDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor) {
        if (game == null || game.getGameState() == null) {
            return false;
        }
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (locations == null) {
            return false;
        }
        for (PhysicalCard destination : locations) {
            if (advancesCaptureApproachByLandspeed(
                    game, playerId, analyzer,
                    actor, destination)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasLegalStableBackMoveDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actor) {
        if (!stableBackState(game, playerId, analyzer)
                || game == null || playerId == null
                || actor == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            Filter legal = Filters.canMoveToUsingLandspeed(
                    playerId, actor,
                    false, false, false,
                    0.0f, null);
            for (PhysicalCard destination
                    : game.getGameState()
                        .getLocationsInOrder()) {
                if (destination != null
                        && legal.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)
                        && !wouldBreakStableBackByMovingTo(
                            game, playerId, analyzer,
                            actor, destination)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isOwnedExactSource(
            PhysicalCard source,
            String playerId,
            String expectedBlueprintId) {
        return source != null
                && playerId != null
                && playerId.equals(source.getOwner())
                && expectedBlueprintId != null
                && expectedBlueprintId.equals(
                    normalizeBlueprintId(
                        source.getBlueprintId(true)));
    }

    /**
     * True only when BHBM can currently deploy at least one Emperor from
     * Reserve Deck through its source-defined deploy -2.
     * The engine deployability query owns legal destinations, live deploy
     * modifiers, extra costs, and available Force. Before the flip, paying
     * for Emperor must also leave the next capture-move Force reserve intact:
     * available Force - rounded effective deploy cost >= capture reserve.
     */
    public static boolean canAffordBhbmEmperorDownload(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard source) {
        if (objectiveKind(analyzer)
                    != CaptureObjectivePolicy.ObjectiveKind.BHBM
                || game == null || playerId == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || !isOwnedExactSource(
                    source, playerId, "9_151")) {
            return false;
        }
        try {
            List<PhysicalCard> reserve =
                    game.getGameState()
                        .getReserveDeck(playerId);
            if (reserve == null) {
                return false;
            }
            Filter affordableEmperor = Filters.and(
                    Filters.Emperor,
                    Filters.deployable(
                        source, null, false, -2.0f));
            for (PhysicalCard candidate : reserve) {
                if (candidate != null
                        && affordableEmperor.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            candidate)) {
                    if (analyzer.isFlipped()) {
                        return true;
                    }
                    int captureReserve =
                            nextCaptureMoveForceReserve(
                                game, playerId,
                                analyzer, candidate);
                    if (captureReserve <= 0) {
                        return true;
                    }
                    float effectiveCost =
                            game.getModifiersQuerying()
                                .getDeployCost(
                                    game.getGameState(),
                                    source, candidate, null,
                                    false, null, false,
                                    -2.0f, null, true);
                    if (!Float.isFinite(effectiveCost)) {
                        continue;
                    }
                    int roundedCost = Math.max(
                            0, (int) Math.ceil(effectiveCost));
                    int availableForce =
                            game.getModifiersQuerying()
                                .getForceAvailableToUse(
                                    game.getGameState(),
                                    playerId);
                    if (availableForce - roundedCost
                            >= captureReserve) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isExactObjectiveTarget(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || game == null
                || playerId == null || candidate == null) {
            return false;
        }
        PhysicalCard objective =
                findObjective(game, playerId, kind);
        return samePhysicalCard(
                findTarget(
                    game, playerId, kind, objective),
                candidate);
    }

    public static boolean isVirtualHutOrigin(
            PhysicalCard candidate) {
        return candidate != null
                && TIGIH_VIRTUAL_HUT.equals(
                    normalizeBlueprintId(
                        candidate.getBlueprintId(true)));
    }

    public static boolean isGuaranteedVirtualHutDestination(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard destination) {
        if (objectiveKind(analyzer)
                    != CaptureObjectivePolicy.ObjectiveKind.TIGIH
                || analyzer.isFlipped()
                || game == null || playerId == null
                || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        PhysicalCard objective = findObjective(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);
        PhysicalCard target = findTarget(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                objective);
        return target != null
                && !target.isCaptive()
                && Filters.Landing_Platform.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(),
                    destination)
                && Filters.canMoveToUsingLocationText(
                    target, true, 0.0f, 0.0f).accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        destination)
                && hasEligibleImperialAt(
                    game, target, destination);
    }

    /**
     * The virtual Hut action exists for any Imperial at Landing Platform, but
     * it is objective-mandatory only when one of them can actually escort Luke.
     */
    public static boolean virtualHutActionGuaranteesCapture(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        if (objectiveKind(analyzer)
                    != CaptureObjectivePolicy.ObjectiveKind.TIGIH
                || analyzer.isFlipped()
                || game == null || playerId == null) {
            return false;
        }
        PhysicalCard objective = findObjective(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);
        PhysicalCard target = findTarget(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                objective);
        PhysicalCard origin = locationOf(game, target);
        if (target == null || target.isCaptive()
                || !isVirtualHutOrigin(origin)
                || game.getGameState() == null) {
            return false;
        }
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (locations == null) {
            return false;
        }
        for (PhysicalCard destination : locations) {
            if (isGuaranteedVirtualHutDestination(
                    game, playerId, analyzer,
                    destination)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSoleVirtualHutCaptureEnablerLocation(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard battleLocation) {
        if (battleLocation == null
                || !virtualHutActionGuaranteesCapture(
                    game, playerId, analyzer)
                || !isGuaranteedVirtualHutDestination(
                    game, playerId, analyzer,
                    battleLocation)
                || game == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        PhysicalCard objective = findObjective(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);
        PhysicalCard target = findTarget(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                objective);
        Collection<PhysicalCard> cards =
                game.getGameState()
                    .getAllPermanentCards();
        if (target == null || cards == null) {
            return false;
        }
        Filter eligible = Filters.and(
                Filters.Imperial,
                Filters.canEscortCaptive(
                    target, true));
        PhysicalCard soleEnablerLocation = null;
        for (PhysicalCard card : cards) {
            if (!isActive(game, card, false)
                    || !eligible.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), card)) {
                continue;
            }
            PhysicalCard location = locationOf(
                    game, card);
            if (!isGuaranteedVirtualHutDestination(
                    game, playerId, analyzer,
                    location)) {
                continue;
            }
            if (soleEnablerLocation == null) {
                soleEnablerLocation = location;
            } else if (!samePhysicalCard(
                    soleEnablerLocation, location)) {
                return false;
            }
        }
        return samePhysicalCard(
                soleEnablerLocation,
                battleLocation);
    }

    public static boolean stableBackState(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || !analyzer.isFlipped()
                || game == null || playerId == null) {
            return false;
        }
        PhysicalCard objective =
                findObjective(game, playerId, kind);
        PhysicalCard target =
                findStableBackTarget(
                    game, playerId, kind, objective);
        return target != null
                && (target.isCaptive()
                    || hasVaderPresentWith(
                        game, target, null));
    }

    public static boolean virtualHutActionWouldBreakStableBack(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        if (objectiveKind(analyzer)
                    != CaptureObjectivePolicy.ObjectiveKind.TIGIH
                || analyzer == null || !analyzer.isFlipped()) {
            return false;
        }
        PhysicalCard objective = findObjective(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);
        PhysicalCard target = findTarget(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                objective, true);
        if (target == null || game == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (locations == null) {
            return false;
        }
        boolean foundLandingPlatform = false;
        Filter legalDestination =
                Filters.canMoveToUsingLocationText(
                    target, true, 0.0f, 0.0f);
        for (PhysicalCard destination : locations) {
            if (destination == null
                    || !Filters.Landing_Platform.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        destination)
                    || !legalDestination.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        destination)) {
                continue;
            }
            foundLandingPlatform = true;
            if (!wouldBreakStableBackByMovingTo(
                    game, playerId, analyzer,
                    target, destination)) {
                return false;
            }
        }
        return foundLandingPlatform;
    }

    /**
     * Counterfactual used for battle forfeits and other removals. Losing an
     * escort releases its captive, so an escort can be the last stable piece.
     */
    public static boolean wouldBreakStableBackIfRemoved(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || !analyzer.isFlipped()
                || game == null || playerId == null
                || candidate == null
                || !playerId.equals(candidate.getOwner())) {
            return false;
        }
        PhysicalCard objective =
                findObjective(game, playerId, kind);
        PhysicalCard target =
                findStableBackTarget(
                    game, playerId, kind, objective);
        if (target == null) {
            return false;
        }

        if (target.isCaptive()) {
            if (samePhysicalCard(target, candidate)) {
                return true;
            }
            PhysicalCard escort = target.getEscort();
            if (samePhysicalCard(escort, candidate)
                    || isInPhysicalCarrierGroup(
                        escort, candidate)) {
                return !hasVaderPresentWith(
                        game, target, candidate);
            }
            return false;
        }

        if (isInPhysicalCarrierGroup(
                target, candidate)) {
            return true;
        }

        if (!hasVaderPresentWith(
                game, target, null)) {
            return false;
        }
        return hasVaderPresentWithInGroup(
                    game, target, candidate)
                && !hasVaderPresentWith(
                    game, target, candidate);
    }

    /**
     * Counterfactual for a selected movement destination. Captives travel with
     * their escort, so an escort move does not itself break the captive leg.
     */
    public static boolean wouldBreakStableBackByMovingTo(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard mover,
            PhysicalCard destination) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || !analyzer.isFlipped()
                || game == null || playerId == null
                || mover == null || destination == null
                || !playerId.equals(mover.getOwner())) {
            return false;
        }
        PhysicalCard objective =
                findObjective(game, playerId, kind);
        PhysicalCard target =
                findStableBackTarget(
                    game, playerId, kind, objective);
        if (target == null || target.isCaptive()) {
            return false;
        }
        if (!hasVaderPresentWith(
                game, target, null)) {
            return false;
        }

        boolean targetMovesWithGroup =
                isInPhysicalCarrierGroup(
                    target, mover);
        boolean vaderMovesWithGroup =
                hasVaderPresentWithInGroup(
                    game, target, mover);
        if (targetMovesWithGroup) {
            if (vaderMovesWithGroup) {
                return false;
            }
            return !hasVaderPresentAt(
                    game, destination, mover);
        }
        return vaderMovesWithGroup
                && !hasVaderPresentWith(
                    game, target, mover);
    }

    public static ObjectiveAnalyzer.FlipGateFormationRole
            classifyStableBackRemoval(
                    SwccgGame game,
                    String playerId,
                    ObjectiveAnalyzer analyzer,
                    PhysicalCard candidate) {
        return wouldBreakStableBackIfRemoved(
                    game, playerId, analyzer, candidate)
                ? ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_OBJECTIVE_SURVIVAL_ACTOR
                : ObjectiveAnalyzer.FlipGateFormationRole.NONE;
    }

    public static boolean iFeelTheConflictActive(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        return objectiveKind(analyzer)
                    == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                && findActive(
                    game, playerId,
                    Filters.I_Feel_The_Conflict) != null;
    }

    public static ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
            bhbmDuelPayoffRoleAt(
                    SwccgGame game,
                    String playerId,
                    ObjectiveAnalyzer analyzer,
                    PhysicalCard candidate,
                    PhysicalCard destination) {
        if (objectiveKind(analyzer)
                    != CaptureObjectivePolicy.ObjectiveKind.BHBM
                || analyzer == null || !analyzer.isFlipped()
                || game == null || playerId == null
                || candidate == null || destination == null
                || !playerId.equals(candidate.getOwner())
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return ObjectiveAnalyzer
                    .ObjectivePostFlipPayoffRole.NONE;
        }
        try {
            if (!Filters.Throne_Room.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        destination)) {
                return ObjectiveAnalyzer
                        .ObjectivePostFlipPayoffRole.NONE;
            }
            PhysicalCard objective = findObjective(
                    game, playerId,
                    CaptureObjectivePolicy.ObjectiveKind.BHBM);
            PhysicalCard target = findTarget(
                    game, playerId,
                    CaptureObjectivePolicy.ObjectiveKind.BHBM,
                    objective);
            if (target == null) {
                return ObjectiveAnalyzer
                        .ObjectivePostFlipPayoffRole.NONE;
            }
            if (isVader(game, candidate)
                    && !target.isFrozen()) {
                boolean targetMovesWithVader =
                        target.isCaptive()
                        && samePhysicalCard(
                            target.getEscort(), candidate);
                boolean targetAlreadyAtThroneRoom =
                        samePhysicalCard(
                            locationOf(game, target),
                            destination);
                if (targetMovesWithVader
                        || targetAlreadyAtThroneRoom) {
                    return ObjectiveAnalyzer
                            .ObjectivePostFlipPayoffRole.PRIMARY;
                }
            }
            if (Filters.Emperor.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(),
                    candidate)) {
                return ObjectiveAnalyzer
                        .ObjectivePostFlipPayoffRole.SECONDARY;
            }
        } catch (Exception ignored) {
            return ObjectiveAnalyzer
                    .ObjectivePostFlipPayoffRole.NONE;
        }
        return ObjectiveAnalyzer
                .ObjectivePostFlipPayoffRole.NONE;
    }

    public static ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
            advancesBhhmDuelPayoffAt(
                    SwccgGame game,
                    String playerId,
                    ObjectiveAnalyzer analyzer,
                    PhysicalCard candidate,
                    PhysicalCard destination) {
        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole destinationRole =
                bhbmDuelPayoffRoleAt(
                    game, playerId, analyzer,
                    candidate, destination);
        if (destinationRole
                == ObjectiveAnalyzer
                    .ObjectivePostFlipPayoffRole.NONE) {
            return destinationRole;
        }
        PhysicalCard origin = locationOf(game, candidate);
        return samePhysicalCard(origin, destination)
                ? ObjectiveAnalyzer
                    .ObjectivePostFlipPayoffRole.NONE
                : destinationRole;
    }

    public static float guaranteedTigihCrossoverTotal(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        if (objectiveKind(analyzer)
                    != CaptureObjectivePolicy.ObjectiveKind.TIGIH
                || game == null || playerId == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return 0.0f;
        }
        PhysicalCard objective = findObjective(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);
        PhysicalCard target = findTarget(
                game, playerId,
                CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                objective);
        if (target == null) {
            return 0.0f;
        }
        PhysicalCard vader = null;
        Collection<PhysicalCard> cards =
                game.getGameState().getAllPermanentCards();
        if (cards != null) {
            for (PhysicalCard candidate : cards) {
                if (isActive(game, candidate, false)
                        && isVader(game, candidate)
                        && isPresentWith(
                            game, candidate, target)) {
                    vader = candidate;
                    break;
                }
            }
        }
        if (vader == null) return 0.0f;
        try {
            return game.getModifiersQuerying()
                    .getCrossoverAttemptTotal(
                        game.getGameState(), vader, 0.0f);
        } catch (Exception ignored) {
            return 0.0f;
        }
    }

    public static boolean tigihCrossoverTimingReached(
            Phase phase) {
        return phase == Phase.MOVE
                || phase == Phase.DRAW
                || phase == Phase.END_OF_TURN;
    }

    /**
     * Protect one executable physical copy, not every duplicate sharing a
     * persona or title.
     */
    public static CaptureObjectivePolicy.CriticalRole
            preferredCriticalLossRole(
                    SwccgGame game,
                    String playerId,
                    ObjectiveAnalyzer analyzer,
                    PhysicalCard candidate) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || game == null || playerId == null
                || candidate == null
                || !playerId.equals(candidate.getOwner())
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        Filter capturePiece = kind
                == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                ? Filters.Luke : Filters.Vader;
        Filter payoffPiece = kind
                == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                ? Filters.I_Feel_The_Conflict : Filters.Emperor;
        if (isPreferredOwnedCopy(
                game, playerId, analyzer,
                candidate, capturePiece,
                CaptureObjectivePolicy.CriticalRole
                    .CAPTURE_PIECE)) {
            return CaptureObjectivePolicy.CriticalRole
                    .CAPTURE_PIECE;
        }
        if (isPreferredOwnedCopy(
                game, playerId, analyzer,
                candidate, payoffPiece,
                CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD)) {
            return CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD;
        }
        return null;
    }

    /**
     * Force that must remain after unrelated deploys for the cheapest next
     * exact or strictly closer capture step whose legality, formation, and
     * cost are all currently known. This includes landspeed and deterministic
     * paid movement mechanisms. Transport is deliberately excluded because
     * its cost is established only after a destiny draw. The virtual Hut
     * route costs zero.
     */
    public static int nextCaptureMoveForceReserve(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard spendingCandidate) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                objectiveKind(analyzer);
        if (kind == null || analyzer.isFlipped()
                || game == null || playerId == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return 0;
        }
        PhysicalCard objective =
                findObjective(game, playerId, kind);
        if (kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                && virtualHutActionGuaranteesCapture(
                    game, playerId, analyzer)) {
            return 0;
        }
        int minimum = Integer.MAX_VALUE;
        Collection<PhysicalCard> actors;
        if (kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH) {
            PhysicalCard target =
                    findTarget(game, playerId, kind, objective);
            actors = target == null
                    ? List.of() : List.of(target);
        } else {
            Collection<PhysicalCard> all =
                    game.getGameState()
                        .getAllPermanentCards();
            actors = all != null ? all : List.of();
        }
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (locations == null) {
            return 0;
        }
        for (PhysicalCard actor : actors) {
            if (actor == null || actor.isCaptive()
                    || !playerId.equals(actor.getOwner())
                    || samePhysicalCard(
                        actor, spendingCandidate)
                    || kind == CaptureObjectivePolicy.ObjectiveKind.BHBM
                        && BhbmSetupPayoffFactsReader
                            .projectedOwnedVader(
                                game, playerId, actor) == null) {
                continue;
            }
            PhysicalCard origin = locationOf(game, actor);
            if (origin == null) continue;
            Filter legal = Filters.canMoveToUsingLandspeed(
                    playerId, actor,
                    false, false, false,
                    0.0f, null);
            for (PhysicalCard destination : locations) {
                try {
                    if (destination == null
                            || !legal.accepts(
                                game.getGameState(),
                                game.getModifiersQuerying(),
                                destination)
                            || !formationSafeMoveDestination(
                                game, playerId, actor,
                                origin, destination)
                            || (!guaranteesImmediateCaptureAt(
                                    game, playerId, analyzer,
                                    actor, destination)
                                && !advancesCaptureApproachByLandspeed(
                                    game, playerId, analyzer,
                                    actor, destination))) {
                        continue;
                    }
                    float cost = game.getModifiersQuerying()
                            .getMoveUsingLandspeedCost(
                                game.getGameState(), actor,
                                origin, destination,
                                false, 0.0f);
                    Integer knownCost =
                            finiteRoundedCost(cost);
                    if (knownCost != null) {
                        minimum = Math.min(
                                minimum, knownCost);
                    }
                } catch (Exception ignored) {
                    // Unknown destinations do not create a reserve obligation.
                }
            }
        }
        minimum = Math.min(
                minimum,
                minimumKnownSpecialCaptureMoveCost(
                    game, playerId, analyzer,
                    actors, spendingCandidate));
        return minimum == Integer.MAX_VALUE
                ? 0 : minimum;
    }

    private static int minimumKnownSpecialCaptureMoveCost(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            Collection<PhysicalCard> actors,
            PhysicalCard spendingCandidate) {
        int minimum = Integer.MAX_VALUE;
        for (PhysicalCard actor : actors) {
            if (actor == null
                    || samePhysicalCard(
                        actor, spendingCandidate)) {
                continue;
            }
            for (CaptureMovementMechanismFactsReader.Mechanism
                    mechanism : List.of(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.SHUTTLE,
                        CaptureMovementMechanismFactsReader
                            .Mechanism.EMBARK,
                        CaptureMovementMechanismFactsReader
                            .Mechanism.DISEMBARK)) {
                minimum = minimumKnownRouteCost(
                        game, spendingCandidate,
                        mechanism,
                        CaptureMovementMechanismFactsReader
                            .assess(
                                game, playerId, analyzer,
                                mechanism, actor, ""),
                        minimum);
            }
        }

        Collection<PhysicalCard> all =
                game.getGameState().getAllPermanentCards();
        if (all == null) {
            return minimum;
        }
        for (PhysicalCard source : all) {
            if (source == null) {
                continue;
            }
            if (Filters.docking_bay.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), source)) {
                minimum = minimumKnownRouteCost(
                        game, spendingCandidate,
                        CaptureMovementMechanismFactsReader
                            .Mechanism.DOCKING_BAY_TRANSIT,
                        CaptureMovementMechanismFactsReader
                            .assess(
                                game, playerId, analyzer,
                                CaptureMovementMechanismFactsReader
                                    .Mechanism
                                    .DOCKING_BAY_TRANSIT,
                                source,
                                "Docking bay transit"),
                        minimum);
            }
            if (!BHBM_VADERS_CASTLE.equals(
                    normalizeBlueprintId(
                        source.getBlueprintId(true)))) {
                continue;
            }
            for (String actionText : List.of(
                    "Move from here to other battleground site",
                    "Move from other battleground site to here")) {
                minimum = minimumKnownRouteCost(
                        game, spendingCandidate,
                        CaptureMovementMechanismFactsReader
                            .Mechanism.VADERS_CASTLE,
                        CaptureMovementMechanismFactsReader
                            .assess(
                                game, playerId, analyzer,
                                CaptureMovementMechanismFactsReader
                                    .Mechanism.VADERS_CASTLE,
                                source, actionText),
                        minimum);
            }
        }
        return minimum;
    }

    private static int minimumKnownRouteCost(
            SwccgGame game,
            PhysicalCard spendingCandidate,
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism,
            CaptureMovementMechanismFactsReader.Assessment
                    assessment,
            int currentMinimum) {
        if (assessment == null
                || !assessment.factsKnown()
                || !assessment.forceBudgetReady()) {
            return currentMinimum;
        }
        int minimum = currentMinimum;
        for (CaptureMovementMechanismFactsReader.Route route
                : assessment.routes()) {
            if (route == null
                    || samePhysicalCard(
                        route.mover(), spendingCandidate)
                    || !route.objectiveRelevantMover()
                    || !route.admissible()
                    || (!route.guaranteesImmediateCapture()
                        && !route.advancesCaptureApproach())) {
                continue;
            }
            Integer cost = exactKnownMoveCost(
                    game, mechanism, route);
            if (cost != null) {
                minimum = Math.min(minimum, cost);
            }
        }
        return minimum;
    }

    private static Integer exactKnownMoveCost(
            SwccgGame game,
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism,
            CaptureMovementMechanismFactsReader.Route route) {
        if (game == null || mechanism == null
                || route == null || route.mover() == null
                || route.origin() == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        try {
            float cost = switch (mechanism) {
                case SHUTTLE ->
                        route.effectiveDestination() != null
                            ? game.getModifiersQuerying()
                                .getShuttleCost(
                                    game.getGameState(),
                                    route.mover(),
                                    route.origin(),
                                    route.effectiveDestination(),
                                    0.0f)
                            : Float.NaN;
                case EMBARK ->
                        route.destination() != null
                            ? game.getModifiersQuerying()
                                .getEmbarkingCost(
                                    game.getGameState(),
                                    route.mover(),
                                    route.destination(),
                                    0.0f)
                            : Float.NaN;
                case DISEMBARK ->
                        route.destination() != null
                            ? game.getModifiersQuerying()
                                .getDisembarkingCost(
                                    game.getGameState(),
                                    route.mover(),
                                    route.destination(),
                                    0.0f)
                            : Float.NaN;
                case DOCKING_BAY_TRANSIT ->
                        route.destination() != null
                            ? game.getModifiersQuerying()
                                .getDockingBayTransitCost(
                                    game.getGameState(),
                                    route.mover(),
                                    route.origin(),
                                    route.destination(),
                                    0.0f)
                            : Float.NaN;
                case VADERS_CASTLE ->
                        route.destination() != null
                            ? game.getModifiersQuerying()
                                .getMoveUsingLocationTextCost(
                                    game.getGameState(),
                                    route.mover(),
                                    route.origin(),
                                    route.destination(),
                                    1.0f, 0.0f)
                            : Float.NaN;
                default -> Float.NaN;
            };
            return finiteRoundedCost(cost);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * A deploy parent may receive mandatory capture credit only when the same
     * exact destination child has a known FormationSafety ALLOW verdict.
     * Active movers are assessed by movement safety at their movement call
     * sites and retain the source-law result here.
     */
    private static boolean captureDeployFormationSafeIfNeeded(
            SwccgGame game,
            String playerId,
            PhysicalCard actor,
            PhysicalCard destination) {
        Zone zone = actor != null ? actor.getZone() : null;
        boolean inPlay = zone != null
                ? zone.isInPlay()
                : isActive(game, actor, true, true);
        if (inPlay) {
            return true;
        }
        if (game == null || game.getGameState() == null
                || playerId == null || actor == null
                || actor.getBlueprint() == null
                || actor.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER
                || destination == null) {
            return false;
        }
        try {
            var blueprint = actor.getBlueprint();
            FormationSafety.DeployVerdict verdict =
                    FormationSafety.assessCharacterDeploy(
                        game, game.getGameState(),
                        playerId, actor,
                        blueprint.hasPowerAttribute()
                            ? blueprint.getPower() : null,
                        blueprint.hasAbilityAttribute()
                            ? blueprint.getAbility() : null,
                        actor.isUndercover(),
                        destination,
                        game.getGameState()
                            .getForcePileSize(playerId),
                        blueprint.getDeployCost(),
                        null, null);
            return verdict.constraint()
                    == FormationSafety.DeployConstraint.ALLOW;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean formationSafeMoveDestination(
            SwccgGame game,
            String playerId,
            PhysicalCard actor,
            PhysicalCard origin,
            PhysicalCard destination) {
        if (game == null || game.getGameState() == null
                || playerId == null || actor == null
                || origin == null || destination == null) {
            return false;
        }
        if (BhbmSetupPayoffFactsReader
                .projectedOwnedVader(
                    game, playerId, actor) != null) {
            return BhbmSetupPayoffFactsReader
                    .projectedVaderMoveFormationSafe(
                        game, playerId, actor,
                        destination);
        }
        return FormationSafety.vetoMoveOrigin(
                    game, game.getGameState(),
                    playerId, actor, origin) == null
                && FormationSafety.vetoMoveDestination(
                    game, game.getGameState(),
                    playerId, actor, destination) == null;
    }

    private static boolean hasEligibleImperialAt(
            SwccgGame game,
            PhysicalCard target,
            PhysicalCard destination) {
        if (game == null || target == null
                || destination == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        Filter eligible = Filters.and(
                Filters.Imperial,
                Filters.canEscortCaptive(
                    target, true));
        for (PhysicalCard card
                : game.getGameState()
                    .getAllPermanentCards()) {
            if (!isActive(game, card, false)
                    || !eligible.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), card)
                    || !samePhysicalCard(
                        locationOf(game, card),
                        destination)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean captureOfDefaultLukeIsProhibited(
            SwccgGame game,
            PhysicalCard objective) {
        if (game == null || objective == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            boolean retargeted =
                    game.getModifiersQuerying()
                        .hasGameTextModification(
                            game.getGameState(),
                            objective,
                            ModifyGameTextType
                                .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE)
                    || game.getModifiersQuerying()
                        .hasGameTextModification(
                            game.getGameState(),
                            objective,
                            ModifyGameTextType
                                .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE);
            return !retargeted
                    && game.getModifiersQuerying()
                        .hasGameTextModification(
                            game.getGameState(),
                            objective,
                            ModifyGameTextType
                                .BRING_HIM_BEFORE_ME__MAY_NOT_CAPTURE_LUKE);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static PhysicalCard findTarget(
            SwccgGame game,
            String playerId,
            CaptureObjectivePolicy.ObjectiveKind kind,
            PhysicalCard objective) {
        return findTarget(
                game, playerId, kind, objective, false);
    }

    private static PhysicalCard findStableBackTarget(
            SwccgGame game,
            String playerId,
            CaptureObjectivePolicy.ObjectiveKind kind,
            PhysicalCard objective) {
        return findTarget(
                game, playerId, kind, objective, true);
    }

    private static PhysicalCard findTarget(
            SwccgGame game,
            String playerId,
            CaptureObjectivePolicy.ObjectiveKind kind,
            PhysicalCard objective,
            boolean includeExcludedFromBattle) {
        Filter targetFilter = Filters.Luke;
        if (kind == CaptureObjectivePolicy.ObjectiveKind.BHBM
                && objective != null
                && game != null
                && game.getGameState() != null
                && game.getModifiersQuerying() != null) {
            try {
                if (game.getModifiersQuerying()
                        .hasGameTextModification(
                            game.getGameState(),
                            objective,
                            ModifyGameTextType
                                .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE)) {
                    targetFilter = Filters.Leia;
                } else if (game.getModifiersQuerying()
                        .hasGameTextModification(
                            game.getGameState(),
                            objective,
                            ModifyGameTextType
                                .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE)) {
                    targetFilter = Filters.Kanan;
                }
            } catch (Exception ignored) {
                targetFilter = Filters.Luke;
            }
        }
        return findActive(
                game, null, targetFilter,
                true, includeExcludedFromBattle);
    }

    private static PhysicalCard findObjective(
            SwccgGame game,
            String playerId,
            CaptureObjectivePolicy.ObjectiveKind kind) {
        if (game == null || playerId == null
                || game.getGameState() == null) {
            return null;
        }
        String expected = kind
                == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                ? "9_61" : "9_151";
        for (PhysicalCard card
                : game.getGameState()
                    .getAllPermanentCards()) {
            if (card == null
                    || !playerId.equals(card.getOwner())
                    || card.getBlueprint() == null
                    || card.getBlueprint().getCardCategory()
                        != CardCategory.OBJECTIVE
                    || !expected.equals(
                        normalizeBlueprintId(
                            card.getBlueprintId(true)))) {
                continue;
            }
            return card;
        }
        return null;
    }

    private static PhysicalCard findActive(
            SwccgGame game,
            String owner,
            Filter filter) {
        return findActive(game, owner, filter, false);
    }

    private static PhysicalCard findActive(
            SwccgGame game,
            String owner,
            Filter filter,
            boolean includeCaptives) {
        return findActive(
                game, owner, filter,
                includeCaptives, false);
    }

    private static PhysicalCard findActive(
            SwccgGame game,
            String owner,
            Filter filter,
            boolean includeCaptives,
            boolean includeExcludedFromBattle) {
        if (game == null || filter == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        for (PhysicalCard card
                : game.getGameState()
                    .getAllPermanentCards()) {
            if (owner != null
                    && (card == null
                        || !owner.equals(card.getOwner()))) {
                continue;
            }
            if (isActive(
                    game, card, includeCaptives,
                    includeExcludedFromBattle)
                    && filter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), card)) {
                return card;
            }
        }
        return null;
    }

    private static boolean hasVaderPresentWith(
            SwccgGame game,
            PhysicalCard target,
            PhysicalCard excludedGroupRoot) {
        if (game == null || target == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        for (PhysicalCard card
                : game.getGameState()
                    .getAllPermanentCards()) {
            if (isInPhysicalCarrierGroup(
                        card, excludedGroupRoot)
                    || !isActive(game, card, false, true)
                    || !isVader(game, card)
                    || !isPresentWith(game, card, target)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean hasVaderPresentWithInGroup(
            SwccgGame game,
            PhysicalCard target,
            PhysicalCard groupRoot) {
        if (game == null || target == null
                || groupRoot == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        for (PhysicalCard card
                : game.getGameState()
                    .getAllPermanentCards()) {
            if (!isInPhysicalCarrierGroup(card, groupRoot)
                    || !isActive(game, card, false, true)
                    || !isVader(game, card)
                    || !isPresentWith(game, card, target)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean hasVaderPresentAt(
            SwccgGame game,
            PhysicalCard location,
            PhysicalCard excludedGroupRoot) {
        if (game == null || location == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        for (PhysicalCard card
                : game.getGameState()
                    .getAllPermanentCards()) {
            if (isInPhysicalCarrierGroup(
                        card, excludedGroupRoot)
                    || !isActive(game, card, false, true)
                    || !isVader(game, card)
                    || !samePhysicalCard(
                        locationOf(game, card),
                        location)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isInPhysicalCarrierGroup(
            PhysicalCard card,
            PhysicalCard groupRoot) {
        if (card == null || groupRoot == null) {
            return false;
        }
        if (samePhysicalCard(card, groupRoot)) {
            return true;
        }
        Set<PhysicalCard> seen =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        PhysicalCard host = card.getAttachedTo();
        while (host != null && seen.add(host)) {
            if (samePhysicalCard(host, groupRoot)) {
                return true;
            }
            host = host.getAttachedTo();
        }
        return false;
    }

    private static boolean isVader(
            SwccgGame game,
            PhysicalCard card) {
        return game != null && card != null
                && Filters.Vader.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), card);
    }

    private static boolean isPresentWith(
            SwccgGame game,
            PhysicalCard first,
            PhysicalCard second) {
        if (game == null || first == null || second == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            return game.getModifiersQuerying()
                    .isPresentWith(
                        game.getGameState(),
                        first, second);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static PhysicalCard locationOf(
            SwccgGame game,
            PhysicalCard card) {
        if (game == null || card == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        try {
            return game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        game.getGameState(), card);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isActive(
            SwccgGame game,
            PhysicalCard card,
            boolean includeCaptives) {
        return isActive(
                game, card, includeCaptives, false);
    }

    private static boolean isActive(
            SwccgGame game,
            PhysicalCard card,
            boolean includeCaptives,
            boolean includeExcludedFromBattle) {
        if (game == null || card == null
                || game.getGameState() == null) {
            return false;
        }
        try {
            return game.getGameState()
                    .isCardInPlayActive(
                        card,
                        includeExcludedFromBattle,
                        false,
                        includeCaptives,
                        false,
                        false,
                        false,
                        false,
                        false);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isPreferredOwnedCopy(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard candidate,
            Filter roleFilter,
            CaptureObjectivePolicy.CriticalRole role) {
        if (game == null || playerId == null
                || analyzer == null || candidate == null
                || roleFilter == null || role == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || !roleFilter.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(),
                    candidate)) {
            return false;
        }
        PhysicalCard preferred = null;
        int preferredRouteRank = Integer.MAX_VALUE;
        int preferredCost = Integer.MAX_VALUE;
        int preferredZoneRank = Integer.MAX_VALUE;
        Collection<PhysicalCard> cards =
                game.getGameState()
                    .getAllPermanentCards();
        if (cards == null) {
            return false;
        }
        if (role == CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD
                && objectiveKind(analyzer)
                    == CaptureObjectivePolicy.ObjectiveKind.BHBM) {
            PhysicalCard activeSidious = findActive(
                    game, playerId,
                    Filters.persona(Persona.SIDIOUS),
                    false);
            if (activeSidious != null) {
                return samePhysicalCard(
                            activeSidious, candidate)
                        && roleFilter.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            activeSidious);
            }
        }
        for (PhysicalCard card : cards) {
            if (card == null
                    || !playerId.equals(card.getOwner())
                    || card.getZone() == null
                    || !roleFilter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), card)) {
                continue;
            }
            CriticalCopyRoute route = criticalCopyRoute(
                    game, playerId, analyzer, card, role);
            if (route == null) {
                continue;
            }
            int zoneRank = lossCandidateRank(
                    game, card);
            if (zoneRank == Integer.MAX_VALUE) {
                continue;
            }
            if (preferred == null
                    || route.rank < preferredRouteRank
                    || route.rank == preferredRouteRank
                        && route.cost < preferredCost
                    || route.rank == preferredRouteRank
                        && route.cost == preferredCost
                        && zoneRank < preferredZoneRank
                    || route.rank == preferredRouteRank
                        && route.cost == preferredCost
                        && zoneRank == preferredZoneRank
                        && card.getPermanentCardId()
                            < preferred.getPermanentCardId()) {
                preferred = card;
                preferredRouteRank = route.rank;
                preferredCost = route.cost;
                preferredZoneRank = zoneRank;
            }
        }
        return samePhysicalCard(
                preferred, candidate);
    }

    /**
     * Role-specific executable routes dominate live effective cost, then
     * static zone order. Unknown or blocked routes do not receive protection.
     */
    private static CriticalCopyRoute criticalCopyRoute(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard card,
            CaptureObjectivePolicy.CriticalRole role) {
        boolean active = isActive(game, card, true);
        if (role == CaptureObjectivePolicy.CriticalRole
                .CAPTURE_PIECE) {
            boolean usable = wouldBreakStableBackIfRemoved(
                        game, playerId, analyzer, card)
                    || (objectiveKind(analyzer)
                            == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                        && isExactObjectiveTarget(
                            game, playerId, analyzer, card)
                        && virtualHutActionGuaranteesCapture(
                            game, playerId, analyzer))
                    || hasLegalImmediateCaptureMoveDestination(
                        game, playerId, analyzer, card)
                    || hasLegalCaptureApproachMoveDestination(
                        game, playerId, analyzer, card);
            if (active && usable) {
                return new CriticalCopyRoute(0, 0);
            }
            Integer cost = card.getZone() == Zone.HAND
                    && hasLegalImmediateCaptureDeployDestination(
                        game, playerId, analyzer, card)
                    ? liveNormalDeployCost(game, card)
                    : null;
            return cost != null
                    ? new CriticalCopyRoute(1, cost)
                    : null;
        }
        if (active) {
            return new CriticalCopyRoute(0, 0);
        }
        return executablePayoffDeployRoute(
                game, playerId, analyzer, card);
    }

    private static CriticalCopyRoute
            executablePayoffDeployRoute(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard card) {
        if (game == null || playerId == null
                || analyzer == null || card == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || card.getZone() == null) {
            return null;
        }
        try {
            CaptureObjectivePolicy.ObjectiveKind kind =
                    objectiveKind(analyzer);
            if (kind
                    == CaptureObjectivePolicy.ObjectiveKind.BHBM
                    && (card.getZone() == Zone.RESERVE_DECK
                        || card.getZone()
                            == Zone.TOP_OF_RESERVE_DECK)) {
                PhysicalCard objective = findObjective(
                        game, playerId, kind);
                List<PhysicalCard> reserve =
                        game.getGameState()
                            .getReserveDeck(playerId);
                if (isOwnedExactSource(
                            objective, playerId, "9_151")
                        && containsExactPhysicalCard(
                            reserve, card)
                        && Filters.deployable(
                            objective, null, false, -2.0f)
                            .accepts(
                                game.getGameState(),
                                game.getModifiersQuerying(),
                                card)) {
                    Integer cost = liveObjectiveDeployCost(
                            game, objective, card, -2.0f);
                    return cost != null
                            ? new CriticalCopyRoute(0, cost)
                            : null;
                }
            }
            if (card.getZone() == Zone.HAND
                    && Filters.deployable(
                        null, null, false, 0.0f)
                        .accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            card)) {
                Integer cost = liveNormalDeployCost(
                        game, card);
                if (cost == null) {
                    return null;
                }
                int rank = kind
                        == CaptureObjectivePolicy
                            .ObjectiveKind.BHBM
                        ? 1 : 0;
                return new CriticalCopyRoute(rank, cost);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static Integer liveNormalDeployCost(
            SwccgGame game,
            PhysicalCard card) {
        try {
            float cost = game.getModifiersQuerying()
                    .getDeployCost(
                        game.getGameState(), card);
            return finiteRoundedCost(cost);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer liveObjectiveDeployCost(
            SwccgGame game,
            PhysicalCard source,
            PhysicalCard card,
            float changeInCost) {
        try {
            float cost = game.getModifiersQuerying()
                    .getDeployCost(
                        game.getGameState(),
                        source, card, null,
                        false, null, false,
                        changeInCost, null, true);
            return finiteRoundedCost(cost);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer finiteRoundedCost(
            float cost) {
        return Float.isFinite(cost)
                ? Math.max(0,
                    (int) Math.ceil(cost))
                : null;
    }

    private static boolean containsExactPhysicalCard(
            Collection<PhysicalCard> cards,
            PhysicalCard candidate) {
        if (cards == null || candidate == null) {
            return false;
        }
        for (PhysicalCard card : cards) {
            if (samePhysicalCard(card, candidate)) {
                return true;
            }
        }
        return false;
    }

    private record CriticalCopyRoute(
            int rank,
            int cost) {
    }

    private static int lossCandidateRank(
            SwccgGame game,
            PhysicalCard card) {
        Zone zone = card.getZone();
        if (zone == null) {
            return Integer.MAX_VALUE;
        }
        if (zone.isInPlay()) {
            return isActive(game, card, true)
                    ? 0 : Integer.MAX_VALUE;
        }
        return switch (zone) {
            case HAND -> 1;
            case RESERVE_DECK, TOP_OF_RESERVE_DECK -> 2;
            case FORCE_PILE, TOP_OF_FORCE_PILE -> 3;
            case USED_PILE, TOP_OF_USED_PILE -> 4;
            case UNRESOLVED_DESTINY_DRAW,
                 TOP_OF_UNRESOLVED_DESTINY_DRAW -> 5;
            default -> Integer.MAX_VALUE;
        };
    }

    private static boolean samePhysicalCard(
            PhysicalCard first,
            PhysicalCard second) {
        if (first == null || second == null) {
            return false;
        }
        if (first == second) {
            return true;
        }
        int firstId = first.getPermanentCardId();
        int secondId = second.getPermanentCardId();
        return firstId > 0 && firstId == secondId;
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
