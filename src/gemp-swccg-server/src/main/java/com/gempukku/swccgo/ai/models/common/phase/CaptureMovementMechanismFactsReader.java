package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Source-typed movement facts for There Is Good In Him and Bring Him Before
 * Me. This reader recognizes only exact stock action text paired with the
 * source blueprint that creates non-rule movement.
 *
 * <p>Unknown source, legality, destination, or formation state stays neutral.
 * The reader never turns a text fragment into objective credit.
 */
public final class CaptureMovementMechanismFactsReader {
    private static final Set<String> ALL_SITE_TRANSPORT_SOURCES =
            Set.of("1_97", "1_243");
    private static final Set<String> EXTERIOR_TRANSPORT_SOURCES =
            Set.of("12_65", "12_150");
    private static final Set<String>
            EXTERIOR_OR_BATTLEGROUND_TRANSPORT_SOURCES =
                Set.of("209_21", "209_48");
    private static final Set<String> TRANSPORT_SOURCES =
            Set.of(
                "1_97", "1_243",
                "12_65", "12_150",
                "209_21", "209_48");
    private static final String VADERS_CASTLE = "209_50";
    private static final String RISE_MY_FRIEND = "9_140";
    private static final String VADERS_MACHINATION = "222_16";

    public static final String SELECTED_ORIGIN_CARD_ID_EXTRA =
            "captureMovementSelectedOriginCardId";

    public enum Mechanism {
        LANDSPEED,
        SHUTTLE,
        EMBARK,
        DISEMBARK,
        DOCKING_BAY_TRANSIT,
        VADERS_CASTLE,
        RISE_RECALL,
        RISE_RELOCATE,
        MACHINATION_RELOCATE,
        TRANSPORT,
        UNKNOWN
    }

    public enum ChoiceStep {
        ORIGIN,
        DESTINATION,
        MOVER,
        UNKNOWN
    }

    public record Route(
            PhysicalCard mover,
            PhysicalCard origin,
            PhysicalCard destination,
            PhysicalCard effectiveDestination,
            boolean objectiveRelevantMover,
            boolean formationKnown,
            boolean formationSafe,
            boolean guaranteesImmediateCapture,
            boolean advancesCaptureApproach,
            boolean breaksStableBack) {

        public boolean admissible() {
            return formationKnown && formationSafe
                    && !breaksStableBack;
        }

        public boolean formationBlocked() {
            return formationKnown && !formationSafe;
        }

        public boolean hardBlocked() {
            return breaksStableBack || formationBlocked();
        }
    }

    public record Assessment(
            Mechanism mechanism,
            boolean factsKnown,
            boolean forceBudgetReady,
            List<Route> routes) {
        public Assessment {
            routes = routes == null
                    ? List.of() : List.copyOf(routes);
        }

        public boolean hasAdmissibleCaptureRoute() {
            return forceBudgetReady
                    && routes.stream().anyMatch(route ->
                        route.objectiveRelevantMover()
                            && route.admissible()
                            && route.guaranteesImmediateCapture());
        }

        public boolean hasAdmissibleApproachRoute() {
            return forceBudgetReady
                    && routes.stream().anyMatch(route ->
                        route.objectiveRelevantMover()
                            && route.admissible()
                            && route.advancesCaptureApproach());
        }
    }

    private CaptureMovementMechanismFactsReader() {
    }

    public static Mechanism classifyParent(
            PhysicalCard actionCard,
            String actionText) {
        String text = normalizeText(actionText);
        if ("move using landspeed".equals(text)) {
            return Mechanism.LANDSPEED;
        }
        if ("shuttle".equals(text)) {
            return Mechanism.SHUTTLE;
        }
        if ("embark".equals(text)) {
            return Mechanism.EMBARK;
        }
        if ("disembark".equals(text)) {
            return Mechanism.DISEMBARK;
        }
        if ("docking bay transit".equals(text)) {
            return Mechanism.DOCKING_BAY_TRANSIT;
        }

        String blueprint = normalizeBlueprintId(
                actionCard != null
                    ? actionCard.getBlueprintId(true) : null);
        if (VADERS_CASTLE.equals(blueprint)
                && ("move from here to other battleground site"
                        .equals(text)
                    || "move from other battleground site to here"
                        .equals(text))) {
            return Mechanism.VADERS_CASTLE;
        }
        if (RISE_MY_FRIEND.equals(blueprint)) {
            if ("take vader into hand".equals(text)) {
                return Mechanism.RISE_RECALL;
            }
            if ("relocate vader to death star ii: docking bay"
                    .equals(text)) {
                return Mechanism.RISE_RELOCATE;
            }
        }
        if (VADERS_MACHINATION.equals(blueprint)
                && "relocate vader to site".equals(text)) {
            return Mechanism.MACHINATION_RELOCATE;
        }
        if (blueprint != null
                && TRANSPORT_SOURCES.contains(blueprint)
                && "'transport' characters".equals(text)) {
            return Mechanism.TRANSPORT;
        }
        return Mechanism.UNKNOWN;
    }

    public static ChoiceStep classifyChild(
            Mechanism mechanism,
            String prompt) {
        String text = normalizeChildPrompt(prompt);
        if (mechanism == null) {
            return ChoiceStep.UNKNOWN;
        }
        return switch (mechanism) {
            case LANDSPEED ->
                    text.startsWith("choose where to move ")
                            && text.endsWith(" using landspeed")
                        ? ChoiceStep.DESTINATION
                        : ChoiceStep.UNKNOWN;
            case SHUTTLE ->
                    text.startsWith("choose where to shuttle ")
                        ? ChoiceStep.DESTINATION
                        : ChoiceStep.UNKNOWN;
            case EMBARK ->
                    text.startsWith("choose where to embark ")
                        ? ChoiceStep.DESTINATION
                        : ChoiceStep.UNKNOWN;
            case DISEMBARK ->
                    text.startsWith("choose where to disembark ")
                        ? ChoiceStep.DESTINATION
                        : ChoiceStep.UNKNOWN;
            case DOCKING_BAY_TRANSIT -> {
                if ("choose docking bay to transit to"
                        .equals(text)) {
                    yield ChoiceStep.DESTINATION;
                }
                yield text.startsWith(
                        "choose cards to docking bay transit to ")
                    ? ChoiceStep.MOVER : ChoiceStep.UNKNOWN;
            }
            case VADERS_CASTLE -> {
                if ("choose card to move from".equals(text)) {
                    yield ChoiceStep.ORIGIN;
                }
                if ("choose card to move to".equals(text)) {
                    yield ChoiceStep.DESTINATION;
                }
                yield text.startsWith("choose card to move to ")
                    ? ChoiceStep.MOVER : ChoiceStep.UNKNOWN;
            }
            case RISE_RECALL ->
                    "choose vader to take into hand".equals(text)
                        ? ChoiceStep.MOVER : ChoiceStep.UNKNOWN;
            case RISE_RELOCATE ->
                    "choose vader to relocate".equals(text)
                        ? ChoiceStep.MOVER : ChoiceStep.UNKNOWN;
            case MACHINATION_RELOCATE ->
                    "choose site to relocate vader to".equals(text)
                        ? ChoiceStep.DESTINATION
                        : ChoiceStep.UNKNOWN;
            case TRANSPORT -> {
                if ("choose site to 'transport' from"
                        .equals(text)) {
                    yield ChoiceStep.ORIGIN;
                }
                if ("choose site to 'transport' to"
                        .equals(text)) {
                    yield ChoiceStep.DESTINATION;
                }
                yield "choose characters to 'transport'"
                        .equals(text)
                    ? ChoiceStep.MOVER : ChoiceStep.UNKNOWN;
            }
            case UNKNOWN -> ChoiceStep.UNKNOWN;
        };
    }

    public static Assessment assess(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            Mechanism mechanism,
            PhysicalCard actionCard,
            String actionText) {
        if (game == null || playerId == null || analyzer == null
                || mechanism == null
                || mechanism == Mechanism.UNKNOWN
                || CaptureObjectiveFacts.objectiveKind(analyzer) == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return unknown(mechanism);
        }
        try {
            List<Route> routes = switch (mechanism) {
                case LANDSPEED, SHUTTLE, EMBARK, DISEMBARK ->
                        fixedMoverRoutes(
                            game, playerId, analyzer,
                            mechanism, actionCard);
                case DOCKING_BAY_TRANSIT ->
                        dockingBayRoutes(
                            game, playerId, analyzer,
                            actionCard);
                case VADERS_CASTLE ->
                        castleRoutes(
                            game, playerId, analyzer,
                            actionCard, actionText);
                case RISE_RECALL ->
                        riseRecallRoutes(
                            game, playerId, analyzer);
                case RISE_RELOCATE ->
                        riseRelocateRoutes(
                            game, playerId, analyzer);
                case MACHINATION_RELOCATE ->
                        machinationRoutes(
                            game, playerId, analyzer,
                            actionCard);
                case TRANSPORT ->
                        transportRoutes(
                            game, playerId, analyzer,
                            actionCard);
                case UNKNOWN -> List.of();
            };
            boolean budgetReady =
                    mechanism != Mechanism.TRANSPORT
                        || transportForceBudgetReady(
                            game.getGameState(), playerId);
            return new Assessment(
                    mechanism, true, budgetReady, routes);
        } catch (Exception ignored) {
            return unknown(mechanism);
        }
    }

    public static List<Route> bind(
            Assessment assessment,
            ChoiceStep step,
            PhysicalCard candidate,
            PhysicalCard selectedOrigin,
            PhysicalCard selectedDestination) {
        if (assessment == null || !assessment.factsKnown()
                || step == null || step == ChoiceStep.UNKNOWN
                || candidate == null) {
            return List.of();
        }
        List<Route> bound = new ArrayList<>();
        for (Route route : assessment.routes()) {
            boolean matches = switch (step) {
                case ORIGIN -> sameCard(
                        route.origin(), candidate);
                case DESTINATION -> sameCard(
                        route.destination(), candidate)
                        && (selectedOrigin == null
                            || sameCard(
                                route.origin(),
                                selectedOrigin));
                case MOVER -> sameCard(
                        route.mover(), candidate)
                        && (selectedOrigin == null
                            || sameCard(
                                route.origin(),
                                selectedOrigin))
                        && (selectedDestination == null
                            || sameCard(
                                route.destination(),
                                selectedDestination)
                            || sameCard(
                                route.effectiveDestination(),
                                selectedDestination));
                case UNKNOWN -> false;
            };
            if (matches) {
                bound.add(route);
            }
        }
        return List.copyOf(bound);
    }

    public static boolean parentCommitsObjectiveMover(
            Mechanism mechanism) {
        return mechanism == Mechanism.LANDSPEED
                || mechanism == Mechanism.SHUTTLE
                || mechanism == Mechanism.EMBARK
                || mechanism == Mechanism.DISEMBARK
                || mechanism == Mechanism.VADERS_CASTLE
                || mechanism == Mechanism.RISE_RECALL
                || mechanism == Mechanism.RISE_RELOCATE
                || mechanism == Mechanism.MACHINATION_RELOCATE;
    }

    private static List<Route> fixedMoverRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            Mechanism mechanism,
            PhysicalCard mover) {
        if (mover == null || !playerId.equals(mover.getOwner())) {
            return List.of();
        }
        Collection<PhysicalCard> destinations =
                mechanism == Mechanism.LANDSPEED
                    ? locations(game)
                    : allCards(game);
        Filter legal = switch (mechanism) {
            case LANDSPEED ->
                    Filters.canMoveToUsingLandspeed(
                        playerId, mover,
                        false, false, false,
                        0.0f, null);
            case SHUTTLE ->
                    Filters.canShuttleTo(
                        playerId, mover,
                        false, 0.0f);
            case EMBARK ->
                    Filters.canEmbarkTo(
                        playerId, mover,
                        false, 0.0f);
            case DISEMBARK ->
                    Filters.canDisembarkTo(
                        playerId, mover,
                        false, 0.0f);
            default -> null;
        };
        if (legal == null) {
            return List.of();
        }
        List<Route> routes = new ArrayList<>();
        for (PhysicalCard destination : destinations) {
            if (destination != null
                    && legal.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        destination)) {
                routes.add(route(
                    game, playerId, analyzer,
                    mover, destination,
                    postActionCaptureRelationshipProven(
                        game, playerId, analyzer,
                        mechanism, destination),
                    false));
            }
        }
        return routes;
    }

    private static List<Route> dockingBayRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard source) {
        if (source == null || !Filters.docking_bay.accepts(
                game.getGameState(),
                game.getModifiersQuerying(), source)) {
            return List.of();
        }
        Filter atSource = Filters.and(
                Filters.your(playerId),
                Filters.hasNotPerformedRegularMove,
                Filters.or(
                    Filters.character,
                    Filters.vehicle,
                    Filters.movesLikeCharacter),
                Filters.atLocation(source));
        Filter attachedArtillery = Filters.and(
                Filters.your(playerId),
                Filters.hasNotPerformedRegularMove,
                Filters.artillery_weapon_that_may_use_db_transit,
                Filters.attachedTo(source));
        Filter moverFilter = Filters.or(
                atSource, attachedArtillery);
        if (playerId.equals(
                game.getGameState().getCurrentPlayerId())) {
            moverFilter = Filters.and(
                    moverFilter,
                    Filters.not(Filters.or(
                        Filters.undercover_spy,
                        Filters.deploysAndMovesLikeUndercoverSpy)));
        } else {
            moverFilter = Filters.and(
                    moverFilter,
                    Filters.or(
                        Filters.undercover_spy,
                        Filters.deploysAndMovesLikeUndercoverSpy));
        }
        Collection<PhysicalCard> movers =
                Filters.filterActive(
                    game, null,
                    SpotOverride.INCLUDE_UNDERCOVER,
                    moverFilter);
        List<Route> routes = new ArrayList<>();
        for (PhysicalCard mover : movers) {
            Filter legal =
                    Filters.canMoveToUsingDockingBayTransit(
                        mover, false, 0.0f);
            routes.addAll(routesAcceptedBy(
                    game, playerId, analyzer,
                    List.of(mover), locations(game), legal));
        }
        return routes;
    }

    private static List<Route> castleRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard castle,
            String actionText) {
        if (castle == null || !VADERS_CASTLE.equals(
                normalizeBlueprintId(
                    castle.getBlueprintId(true)))) {
            return List.of();
        }
        boolean outbound =
                "move from here to other battleground site"
                    .equals(normalizeText(actionText));
        boolean inbound =
                "move from other battleground site to here"
                    .equals(normalizeText(actionText));
        if (!outbound && !inbound) {
            return List.of();
        }
        List<Route> routes = new ArrayList<>();
        for (PhysicalCard mover : allCards(game)) {
            if (!ownedVader(game, playerId, mover)
                    || !Filters.hasNotPerformedRegularMove.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), mover)) {
                continue;
            }
            PhysicalCard origin = locationOf(game, mover);
            if (outbound && !sameCard(origin, castle)
                    || inbound && sameCard(origin, castle)) {
                continue;
            }
            Filter legal = Filters.canMoveToUsingLocationText(
                    mover, false, 1.0f, 0.0f);
            if (outbound) {
                for (PhysicalCard destination : locations(game)) {
                    if (destination != null
                            && !sameCard(destination, castle)
                            && Filters.battleground_site.accepts(
                                game.getGameState(),
                                game.getModifiersQuerying(),
                                destination)
                            && legal.accepts(
                                game.getGameState(),
                                game.getModifiersQuerying(),
                                destination)) {
                        routes.add(route(
                            game, playerId, analyzer,
                            mover, destination));
                    }
                }
            } else if (Filters.battleground_site.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), origin)
                    && legal.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), castle)) {
                routes.add(route(
                    game, playerId, analyzer,
                    mover, castle));
            }
        }
        return routes;
    }

    private static List<Route> riseRecallRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        List<Route> routes = new ArrayList<>();
        Filter legal = Filters.and(
                Filters.Vader,
                Filters.at(Filters.controls(playerId)));
        for (PhysicalCard mover : allCards(game)) {
            if (mover == null || !playerId.equals(mover.getOwner())
                    || !legal.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), mover)) {
                continue;
            }
            boolean relevant = objectiveRelevantMover(
                    game, playerId, analyzer, mover);
            boolean breaks = relevant
                    && CaptureObjectiveFacts
                        .wouldBreakStableBackIfRemoved(
                            game, playerId, analyzer, mover);
            PhysicalCard origin = locationOf(game, mover);
            boolean formationKnown = origin != null;
            boolean safe = formationKnown
                    && FormationSafety.vetoMoveOrigin(
                        game, game.getGameState(),
                        playerId, mover, origin) == null;
            routes.add(new Route(
                    mover, origin,
                    null, null, relevant,
                    formationKnown, safe,
                    false, false, breaks));
        }
        return routes;
    }

    private static List<Route> riseRelocateRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        PhysicalCard destination = null;
        for (PhysicalCard location : locations(game)) {
            if (location != null
                    && Filters.Death_Star_II_Docking_Bay
                        .accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            location)) {
                destination = location;
                break;
            }
        }
        if (destination == null) {
            return List.of();
        }
        List<Route> routes = new ArrayList<>();
        Filter legal = Filters.and(
                Filters.Vader,
                Filters.escorting(
                    Filters.or(Filters.Luke, Filters.Leia)),
                Filters.at(Filters.and(
                    Filters.site,
                    Filters.controls(playerId))),
                Filters.canBeRelocatedToLocation(
                    destination, false, true,
                    false, 0.0f, false));
        for (PhysicalCard mover : allCards(game)) {
            if (mover != null
                    && playerId.equals(mover.getOwner())
                    && legal.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), mover)) {
                routes.add(route(
                    game, playerId, analyzer,
                    mover, destination));
            }
        }
        return routes;
    }

    private static List<Route> machinationRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard source) {
        PhysicalCard mover = Filters.findFirstActive(
                game, source, Filters.Vader);
        PhysicalCard walkway = Filters.findFirstActive(
                game, source, Filters.Chasm_Walkway);
        if (!ownedVader(game, playerId, mover)
                || walkway == null) {
            return List.of();
        }
        PhysicalCard origin = locationOf(game, mover);
        if (origin == null) {
            return List.of();
        }
        Filter opponentBattleground = Filters.and(
                Filters.opponents(playerId),
                Filters.battleground_site);
        boolean fromWalkway = sameCard(origin, walkway);
        if (!fromWalkway
                && !opponentBattleground.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), origin)) {
            return List.of();
        }
        Filter destinationFilter = fromWalkway
                ? opponentBattleground
                : Filters.sameCardId(walkway);
        Filter legalDestination =
                Filters.locationCanBeRelocatedTo(
                    mover, true, 0.0f);
        List<Route> routes = new ArrayList<>();
        for (PhysicalCard destination : locations(game)) {
            if (destination != null
                    && destinationFilter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        destination)
                    && legalDestination.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)) {
                routes.add(route(
                    game, playerId, analyzer,
                    mover, destination));
            }
        }
        return routes;
    }

    private static List<Route> transportRoutes(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard source) {
        if (source == null
                || !playerId.equals(source.getOwner())
                || !TRANSPORT_SOURCES.contains(
                    normalizeBlueprintId(
                        source.getBlueprintId(true)))) {
            return List.of();
        }
        String blueprint = normalizeBlueprintId(
                source.getBlueprintId(true));
        Filter originFilter;
        Filter destinationFilter;
        if (EXTERIOR_TRANSPORT_SOURCES.contains(
                blueprint)) {
            originFilter = Filters.exterior_site;
            destinationFilter = Filters.exterior_site;
        } else if (
                EXTERIOR_OR_BATTLEGROUND_TRANSPORT_SOURCES
                    .contains(blueprint)) {
            originFilter = Filters.site;
            destinationFilter = Filters.or(
                    Filters.exterior_site,
                    Filters.battleground_site);
        } else if (ALL_SITE_TRANSPORT_SOURCES.contains(
                blueprint)) {
            boolean exteriorOnly =
                    game.getModifiersQuerying()
                        .hasGameTextModification(
                            game.getGameState(), source,
                            ModifyGameTextType
                                .NABRUN_LEIDS_ELIS_HELROT__LIMIT_USAGE);
            Filter elisSite = Filters.and(
                    exteriorOnly
                        ? Filters.exterior_site
                        : Filters.site,
                    Filters.notProhibitedFromUsingCardToTransportToOrFromLocation(
                        source));
            originFilter = elisSite;
            destinationFilter = elisSite;
        } else {
            return List.of();
        }
        List<Route> routes = new ArrayList<>();
        for (PhysicalCard mover : allCards(game)) {
            if (mover == null || !playerId.equals(mover.getOwner())
                    || !Filters.character.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), mover)) {
                continue;
            }
            PhysicalCard origin = locationOf(game, mover);
            if (origin == null || !originFilter.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), origin)) {
                continue;
            }
            Filter legalDestination =
                    Filters.locationCanBeRelocatedTo(
                        mover, true, 0.0f);
            for (PhysicalCard destination : locations(game)) {
                if (destination == null
                        || sameCard(origin, destination)
                        || !destinationFilter.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)
                        || !legalDestination.accepts(
                                game.getGameState(),
                                game.getModifiersQuerying(),
                                destination)) {
                    continue;
                }
                routes.add(route(
                    game, playerId, analyzer,
                    mover, destination,
                    false, true));
            }
        }
        return routes;
    }

    private static List<Route> routesAcceptedBy(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            Collection<PhysicalCard> movers,
            Collection<PhysicalCard> destinations,
            Filter legal) {
        if (legal == null || movers == null
                || destinations == null) {
            return List.of();
        }
        List<Route> routes = new ArrayList<>();
        for (PhysicalCard mover : movers) {
            if (mover == null) {
                continue;
            }
            for (PhysicalCard destination : destinations) {
                if (destination != null
                        && legal.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(),
                            destination)) {
                    routes.add(route(
                        game, playerId, analyzer,
                        mover, destination));
                }
            }
        }
        return routes;
    }

    private static Route route(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard mover,
            PhysicalCard destination) {
        return route(
                game, playerId, analyzer,
                mover, destination,
                true, false);
    }

    private static Route route(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard mover,
            PhysicalCard destination,
            boolean immediateCaptureGuaranteed,
            boolean captureEndpointMayBeApproach) {
        PhysicalCard origin = locationOf(game, mover);
        PhysicalCard effective =
                effectiveDestination(game, destination);
        boolean relevant = objectiveRelevantMover(
                game, playerId, analyzer, mover);
        boolean captureCapable = relevant && effective != null
                && CaptureObjectiveFacts
                    .guaranteesImmediateCaptureAt(
                        game, playerId, analyzer,
                        mover, effective);
        boolean capture = immediateCaptureGuaranteed
                && captureCapable;
        boolean approach = relevant
                && !capture
                && (captureEndpointMayBeApproach
                        && captureCapable
                    || advancesCaptureApproach(
                        game, playerId, analyzer,
                        mover, origin, effective));
        boolean breaks = relevant && effective != null
                && CaptureObjectiveFacts
                    .wouldBreakStableBackByMovingTo(
                        game, playerId, analyzer,
                        mover, effective);
        boolean formationKnown =
                origin != null && effective != null;
        boolean safe = formationKnown
                && formationSafe(
                    game, playerId, mover,
                    origin, effective);
        return new Route(
                mover, origin, destination, effective,
                relevant, formationKnown, safe,
                capture, approach, breaks);
    }

    /**
     * Proves the relationship the objective source will query after a fixed
     * mover is placed at or aboard the selected destination. The outer
     * location remains the correct FormationSafety endpoint, but it is not
     * interchangeable with being present on or in a carrier.
     */
    private static boolean postActionCaptureRelationshipProven(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            Mechanism mechanism,
            PhysicalCard destination) {
        if (mechanism != Mechanism.EMBARK
                && mechanism != Mechanism.SHUTTLE
                && mechanism != Mechanism.DISEMBARK) {
            return true;
        }
        CaptureObjectivePolicy.ObjectiveKind kind =
                CaptureObjectiveFacts.objectiveKind(analyzer);
        PhysicalCard effective =
                effectiveDestination(game, destination);
        if (kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH) {
            return effective != null
                    && Filters.site.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        effective);
        }
        if (kind != CaptureObjectivePolicy.ObjectiveKind.BHBM) {
            return false;
        }
        PhysicalCard target = exactObjectiveTarget(
                game, playerId, analyzer);
        PhysicalCard moverWillBePresentAt =
                presentPlaceAfterMove(game, destination);
        PhysicalCard targetIsPresentAt =
                presentPlaceOf(game, target);
        return sameCard(
                moverWillBePresentAt,
                targetIsPresentAt);
    }

    private static PhysicalCard exactObjectiveTarget(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer) {
        for (PhysicalCard candidate : allCards(game)) {
            if (CaptureObjectiveFacts.isExactObjectiveTarget(
                    game, playerId, analyzer, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static PhysicalCard presentPlaceAfterMove(
            SwccgGame game,
            PhysicalCard destination) {
        if (game == null || destination == null
                || destination.getBlueprint() == null) {
            return null;
        }
        CardCategory category =
                destination.getBlueprint().getCardCategory();
        if (category == CardCategory.LOCATION
                || category == CardCategory.STARSHIP) {
            return destination;
        }
        if (category == CardCategory.VEHICLE) {
            if (game.getModifiersQuerying().hasKeyword(
                    game.getGameState(), destination,
                    Keyword.ENCLOSED)) {
                return destination;
            }
            return presentPlaceOf(game, destination);
        }
        return null;
    }

    private static PhysicalCard presentPlaceOf(
            SwccgGame game,
            PhysicalCard card) {
        if (game == null || card == null) {
            return null;
        }
        try {
            return game.getModifiersQuerying()
                    .getCardIsPresentAt(
                        game.getGameState(), card);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Source-backed capture geometry after a mechanism has already proved its
     * own legality. The capture endpoint query reuses every TIGIH/BHBM target,
     * targeting, escort, and game-text restriction without pretending the
     * special route is landspeed.
     */
    private static boolean advancesCaptureApproach(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard mover,
            PhysicalCard origin,
            PhysicalCard destination) {
        return CaptureObjectiveFacts.advancesCaptureApproachAt(
                game, playerId, analyzer,
                mover, origin, destination);
    }

    private static boolean objectiveRelevantMover(
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard mover) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                CaptureObjectiveFacts.objectiveKind(analyzer);
        if (kind == null || mover == null
                || !playerId.equals(mover.getOwner())) {
            return false;
        }
        if (kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH) {
            return CaptureObjectiveFacts.isExactObjectiveTarget(
                    game, playerId, analyzer, mover);
        }
        return BhbmSetupPayoffFactsReader
                .projectedOwnedVader(
                    game, playerId, mover) != null;
    }

    private static boolean ownedVader(
            SwccgGame game,
            String playerId,
            PhysicalCard card) {
        return card != null && playerId.equals(card.getOwner())
                && Filters.Vader.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), card);
    }

    private static boolean formationSafe(
            SwccgGame game,
            String playerId,
            PhysicalCard mover,
            PhysicalCard origin,
            PhysicalCard destination) {
        if (origin == null || destination == null) {
            return false;
        }
        if (BhbmSetupPayoffFactsReader
                .projectedOwnedVader(
                    game, playerId, mover) != null) {
            return BhbmSetupPayoffFactsReader
                    .projectedVaderMoveFormationSafe(
                        game, playerId, mover,
                        destination);
        }
        if (sameCard(origin, destination)) {
            PhysicalCard currentPresence =
                    game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(
                            game.getGameState(), mover);
            if (sameCard(
                    currentPresence, destination)) {
                return true;
            }
            return FormationSafety.vetoMoveDestination(
                    game, game.getGameState(),
                    playerId, mover, destination) == null;
        }
        return FormationSafety.vetoMoveOrigin(
                    game, game.getGameState(),
                    playerId, mover, origin) == null
                && FormationSafety.vetoMoveDestination(
                    game, game.getGameState(),
                    playerId, mover, destination) == null;
    }

    private static boolean transportForceBudgetReady(
            GameState gameState,
            String playerId) {
        return gameState != null && playerId != null
                && gameState.getForcePileSize(playerId) >= 4
                && gameState.getReserveDeckSize(playerId) >= 1;
    }

    private static PhysicalCard effectiveDestination(
            SwccgGame game,
            PhysicalCard destination) {
        if (game == null || destination == null
                || destination.getBlueprint() == null) {
            return null;
        }
        if (destination.getBlueprint().getCardCategory()
                == CardCategory.LOCATION) {
            return destination;
        }
        try {
            return game.getModifiersQuerying().getLocationHere(
                    game.getGameState(), destination);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PhysicalCard locationOf(
            SwccgGame game,
            PhysicalCard card) {
        if (game == null || card == null) {
            return null;
        }
        try {
            PhysicalCard location =
                    game.getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        game.getGameState(), card);
            return location != null
                    ? location
                    : game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(
                            game.getGameState(), card);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Collection<PhysicalCard> allCards(
            SwccgGame game) {
        Collection<PhysicalCard> cards =
                game.getGameState().getAllPermanentCards();
        return cards != null ? cards : List.of();
    }

    private static Collection<PhysicalCard> locations(
            SwccgGame game) {
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        return locations != null ? locations : List.of();
    }

    private static Assessment unknown(Mechanism mechanism) {
        return new Assessment(
                mechanism != null
                    ? mechanism : Mechanism.UNKNOWN,
                false, false, List.of());
    }

    private static boolean sameCard(
            PhysicalCard first,
            PhysicalCard second) {
        return first != null && second != null
                && first.getPermanentCardId()
                    == second.getPermanentCardId();
    }

    private static String normalizeText(String text) {
        return text == null ? ""
                : text.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeChildPrompt(String text) {
        String normalized = normalizeText(text);
        String optionalCancel =
                ", or click 'done' to cancel";
        return normalized.endsWith(optionalCancel)
                ? normalized.substring(
                    0, normalized.length()
                        - optionalCancel.length()).trim()
                : normalized;
    }

    private static String normalizeBlueprintId(
            String blueprintId) {
        if (blueprintId == null) {
            return null;
        }
        String normalized = blueprintId.trim();
        if (normalized.endsWith("_BACK")) {
            normalized = normalized.substring(
                    0, normalized.length() - 5);
        }
        int underscore = normalized.indexOf('_');
        if (underscore <= 0
                || underscore >= normalized.length() - 1) {
            return normalized;
        }
        try {
            int set = Integer.parseInt(
                    normalized.substring(0, underscore));
            int card = Integer.parseInt(
                    normalized.substring(underscore + 1));
            return set + "_" + card;
        } catch (NumberFormatException ignored) {
            return normalized;
        }
    }
}
