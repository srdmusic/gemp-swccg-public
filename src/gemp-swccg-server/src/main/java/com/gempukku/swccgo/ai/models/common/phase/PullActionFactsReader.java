package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads stock game state into immutable PULL parent facts without scoring. */
public final class PullActionFactsReader {

    private static final Pattern NAMED_ACTION_TARGET = Pattern.compile(
            "(?:Deploy|Take) ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) "
                    + "(?:from Reserve|into hand from Reserve)");
    private static final Pattern MEMORY_NAMED_TARGET = Pattern.compile(
            "(?:Deploy|Take) ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) "
                    + "(?:from Reserve|from Lost|from Used|from Force|into hand from)");
    private static final Pattern MEMORY_GENERIC_TARGET = Pattern.compile(
            "(?:Deploy|Take|\\[Download\\]) an? ([a-z]+) ?");
    private static final Pattern LOCATION_SOURCE_PATTERN = Pattern.compile(
            "\\b(site|location|battleground|docking\\s+bay|system|sector)\\b[^.;]*?\\bfrom\\s+reserve",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> MEMORY_STOPWORDS = Set.of(
            "location", "site", "battleground", "system", "sector",
            "ship", "starship", "vehicle", "transport", "fighter",
            "weapon", "lightsaber", "blaster", "bowcaster", "device",
            "character", "alien", "droid", "jedi", "sith", "padawan",
            "inquisitor", "senator", "pilot", "warrior", "soldier",
            "leader", "admiral", "general", "trooper", "officer",
            "rebel", "imperial", "scout", "spy",
            "effect", "interrupt", "objective", "epic", "shield", "card");

    private static final String[] LOCATION_KEYWORDS = {
            "site", "battleground", "location", "system", "farm",
            "cantina", "mos eisley", "tatooine", "endor", "hoth",
            "dagobah", "naboo", "yavin", "bespin", "cloud city",
            "mustafar", "malachor", "mapuzo", "jabiim", "coruscant",
            "kashyyyk", "kessel", "kamino", "geonosis", "alderaan",
            "docking bay", "spaceport", "city", "palace", "temple",
            "safehouse", "corridor", "village", "outpost", "sector", "planet"
    };
    private static final String[] WEAPON_KEYWORDS = {
            "weapon", "lightsaber", "saber", "blaster", "rifle", "pistol",
            "cannon", "bowcaster", "thermal detonator", "vibroblade", "vibro-",
            "force pike", "electrostaff"
    };
    private static final String[] DEVICE_KEYWORDS = {
            "device", "comlink", "bionic", "sensor", "lockblade", "restraints",
            "macrobinoculars", "scanner", "datapad", "tool kit", "fusion cutter",
            "bowcaster"
    };
    private static final Set<String> V131_LOCATION_NOUNS = Set.of(
            "location", "site", "system", "sector", "battleground", "docking bay",
            "outpost", "spaceport", "palace", "temple", "safehouse", "corridor",
            "village");
    private static final Set<String> V131_WEAPON_NOUNS = Set.of(
            "weapon", "lightsaber", "blaster", "bowcaster", "rifle",
            "vibroblade", "vibro-blade");

    public interface ObjectiveView {
        boolean isAnalyzed();

        boolean isFlipped();

        String flipConditionText();

        Set<String> strategyCharacterTokens(SwccgGame game, String playerId);

        boolean hasTypedStrategyKeyCharacter();

        boolean isStrategyKeyCharacter(
                SwccgGame game, String playerId,
                PhysicalCard candidate);

        default boolean objectivePullAdvancesRequiredOnTableCard(
                SwccgGame game, String playerId,
                String sourceTitle) {
            return false;
        }

        default boolean objectivePullAdvancesRequiredOnTableCard(
                SwccgGame game, String playerId,
                PhysicalCard source) {
            return objectivePullAdvancesRequiredOnTableCard(
                    game, playerId,
                    source != null ? source.getTitle() : null);
        }

        default boolean objectiveRoutePullVetoBypass(
                SwccgGame game, String playerId,
                PhysicalCard source, String actionText) {
            return false;
        }

        default boolean objectiveRoutePullOracleValidationBypass(
                SwccgGame game, String playerId,
                PhysicalCard source, String actionText) {
            return false;
        }

        default boolean objectivePullFormationExempt(
                SwccgGame game, String playerId,
                PhysicalCard source, String actionText) {
            return false;
        }
    }

    public interface LateView {
        List<PhysicalCard> hand();

        boolean battlePlausible();
    }

    public record Context(
            SwccgGame game,
            GameState gameState,
            String playerId,
            Side side,
            Phase phase,
            PullOracleView oracle,
            ObjectiveView objective,
            LateView lateView) {
    }

    private PullActionFactsReader() {
    }

    public static PullActionFacts.EarlySearch readEarlySearch(
            String actionId,
            String actionText,
            String sourceCardId,
            Context context) {
        if (context == null || context.oracle() == null
                || !context.oracle().isAnalyzed()
                || context.gameState() == null || sourceCardId == null) {
            return none(actionId);
        }
        try {
            PhysicalCard source = context.gameState().findCardById(
                    Integer.parseInt(sourceCardId));
            if (source == null || source.getBlueprint() == null) {
                return none(actionId);
            }
            if (context.objective() != null
                    && context.objective()
                        .objectiveRoutePullOracleValidationBypass(
                            context.game(), context.playerId(),
                            source, actionText)) {
                return none(actionId);
            }
            String gameText = context.oracle().sourceCardFullGameText(
                    source.getBlueprint(), context.side());
            List<String> targets = context.oracle().parseSourceCardPullTargets(gameText);
            if (!targets.isEmpty()) {
                boolean alive = false;
                boolean anyDead = false;
                boolean anyJunk = false;
                for (String target : targets) {
                    if (context.oracle().hasTargetInZone(Zone.RESERVE_DECK, target)) {
                        alive = true;
                        break;
                    }
                    boolean wordHit = false;
                    for (String word : target.split("[^a-zA-Z']+")) {
                        if (word.length() >= 6 && !context.oracle().isGenericTypeWord(word)
                                && context.oracle().hasTargetInZone(
                                        Zone.RESERVE_DECK, word)) {
                            wordHit = true;
                            break;
                        }
                    }
                    if (wordHit) {
                        alive = true;
                        break;
                    }
                    if (target.length() > 25 || target.matches(".*\\d.*")) {
                        anyJunk = true;
                    } else {
                        anyDead = true;
                    }
                }
                if (!alive && anyDead && !anyJunk) {
                    PullOracleView.Validation validation =
                            context.oracle().validatePullFromSourceCard(
                                    Zone.RESERVE_DECK, gameText);
                    if (validation.outcome() != PullOracleView.Outcome.WILL_SUCCEED) {
                        return new PullActionFacts.EarlySearch(
                                actionId,
                                PullActionFacts.EarlyGate.V177_DEAD_SEARCH,
                                "V177 DEAD SEARCH: none of " + targets
                                        + " remain in Reserve Deck \u2014 a real player never searches for what he knows isn't there");
                    }
                }
            } else {
                String blueprintId = source.getBlueprintId(true);
                List<PullOracleView.NamedDeckCard> named =
                        context.oracle().namedDeckCardsInText(gameText, blueprintId);
                if (!named.isEmpty()) {
                    boolean anyInReserve = false;
                    Set<String> titles = new LinkedHashSet<>();
                    for (PullOracleView.NamedDeckCard card : named) {
                        titles.add(card.title());
                        if (card.zone() == Zone.RESERVE_DECK) {
                            anyInReserve = true;
                        }
                    }
                    if (!anyInReserve) {
                        return new PullActionFacts.EarlySearch(
                                actionId,
                                PullActionFacts.EarlyGate.V183_DEAD_SEARCH,
                                "V183 DEAD SEARCH (title+zone): " + titles
                                        + " named in source text but none is in Reserve \u2014 already in hand/play/lost");
                    }
                }
            }
        } catch (Exception ignored) {
            // Fail open exactly like the predecessor.
        }
        return none(actionId);
    }

    public static PullActionFacts.Parent readParent(
            String actionId,
            String actionText,
            String sourceCardId,
            Context context) {
        String text = actionText == null ? "" : actionText;
        String lower = text.toLowerCase(Locale.ROOT);
        PullOracleView oracle = context != null ? context.oracle() : null;
        GameState gameState = context != null ? context.gameState() : null;
        int reserveSize = safeReserveSize(context);
        PullOracleView.Validation memoryValidation = unknownValidation();
        PullOracleView.Validation sourceValidation = unknownValidation();
        DeadInterrupt deadInterrupt = DeadInterrupt.none();
        LocationAssessment location = LocationAssessment.none();
        HostAssessment hosts = HostAssessment.none();
        KeyCharacter keyCharacter = KeyCharacter.none();
        FormationAssessment formation = FormationAssessment.none();
        boolean lowReserve =
                reserveSize >= 0 && reserveSize <= 2;
        boolean objectiveExceptionCanApply =
                context != null
                && context.objective() != null
                && context.game() != null
                && context.playerId() != null;
        if (lowReserve && !objectiveExceptionCanApply) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, "?", null, false,
                    null, 0, deadInterrupt, location, hosts, keyCharacter,
                    context, false, formation);
        }
        PhysicalCard source = sourceCard(gameState, sourceCardId);
        String sourceTitle = sourceTitle(source);
        CardCategory sourceCategory = sourceCategory(source);
        // WMAOP 2026-08-08 (Steve directive): probe the table only for We Must
        // Accelerate Our Plans sources — the shared policy turns this into the
        // WMAOP.FODDER_HOLD veto (never play WMAOP once its Blockade site is out).
        boolean wmaopBlockadeSiteOnTable =
                sourceTitle.toLowerCase(Locale.ROOT).contains("accelerate our plans")
                && blockadeFlagshipSiteOnTable(
                        context != null ? context.game() : null, gameState);
        boolean requiredOnTableCardPull =
                context != null
                && context.objective() != null
                && context.objective()
                    .objectivePullAdvancesRequiredOnTableCard(
                        context.game(), context.playerId(),
                        source);
        boolean requiredOnTableCardPullVetoBypass =
                requiredOnTableCardPull
                && source != null
                && "209_42".equals(
                    source.getBlueprintId(true));
        boolean objectiveRoutePullVetoBypass =
                context != null
                && context.objective() != null
                && context.objective()
                    .objectiveRoutePullVetoBypass(
                        context.game(), context.playerId(),
                        source, text);
        boolean objectiveRoutePullOracleValidationBypass =
                context != null
                && context.objective() != null
                && context.objective()
                    .objectiveRoutePullOracleValidationBypass(
                        context.game(), context.playerId(),
                        source, text);

        if (!requiredOnTableCardPullVetoBypass
                && !objectiveRoutePullVetoBypass
                && lowReserve) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, "?", null, false,
                    null, 0, deadInterrupt, location, hosts, keyCharacter,
                    context, false, formation);
        }

        String namedMissingTarget = objectiveRoutePullOracleValidationBypass
                ? "" : namedMissingTarget(text, oracle);
        if (!namedMissingTarget.isEmpty()) {
            return buildParent(actionId, text, reserveSize, namedMissingTarget,
                    memoryValidation, sourceValidation, "?", null, false,
                    null, 0, deadInterrupt, location, hosts, keyCharacter,
                    context, false, formation);
        }

        memoryValidation = objectiveRoutePullOracleValidationBypass
                ? unknownValidation()
                : memoryValidation(text, oracle);
        if (memoryValidation.outcome() == PullOracleView.Outcome.WILL_FAIL) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, "?", null, false,
                    null, 0, deadInterrupt, location, hosts, keyCharacter,
                    context, false, formation);
        }

        String sourceText = sourceText(source, context, oracle);
        List<String> targets = pullTargets(sourceText, oracle);
        Zone sourceZone = sourceZone(text, oracle);
        if (!objectiveRoutePullOracleValidationBypass
                && oracle != null && oracle.isAnalyzed()
                && sourceZone != null && sourceText != null) {
            sourceValidation = sourceValidation(sourceZone, sourceText, oracle);
        }
        if (sourceValidation.outcome() == PullOracleView.Outcome.WILL_FAIL) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, sourceTitle,
                    sourceCategory, false, null, 0, deadInterrupt, location,
                    hosts, keyCharacter, context, false, formation);
        }

        boolean sourceVerifiedEndorShieldEffect =
                "endor: bunker".equalsIgnoreCase(sourceTitle)
                && text.toLowerCase(Locale.ROOT)
                    .contains("deploy endor shield from reserve deck")
                && targets.stream().anyMatch(
                    target -> target != null
                        && target.contains("endor shield"));
        boolean allUnattachable = !sourceVerifiedEndorShieldEffect
                && sourceValidation.outcome()
                == PullOracleView.Outcome.WILL_SUCCEED
                && sourceZone == Zone.RESERVE_DECK
                && context != null && context.game() != null
                && !requiredOnTableCardPullVetoBypass
                && oracle != null && safeAllUnattachable(
                        oracle, context.game(), context.playerId(), targets);
        if (allUnattachable) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, sourceTitle,
                    sourceCategory, true, null, 0, deadInterrupt, location,
                    hosts, keyCharacter, context, false, formation);
        }

        Integer cheapestCost = null;
        int availableForce = 0;
        if (context != null && gameState != null && sourceText != null
                && !targets.isEmpty() && context.playerId() != null) {
            try {
                availableForce = gameState.getForcePileSize(context.playerId());
                List<PhysicalCard> reserve = gameState.getReserveDeck(context.playerId());
                if (reserve != null) {
                    for (PhysicalCard card : reserve) {
                        if (card == null || card.getBlueprint() == null
                                || card.getTitle() == null) {
                            continue;
                        }
                        String cardTitle = card.getTitle().toLowerCase(Locale.ROOT);
                        if (!matchesAnyTarget(cardTitle, targets)) {
                            continue;
                        }
                        Float deployCost = card.getBlueprint().getDeployCost();
                        if (deployCost == null) {
                            continue;
                        }
                        int cost = deployCost.intValue();
                        String sourceLower = sourceText.toLowerCase(Locale.ROOT);
                        if (sourceLower.contains("less force")
                                || sourceLower.contains("deploy -1")) {
                            cost = Math.max(0, cost - 1);
                        }
                        if (cheapestCost == null || cost < cheapestCost) {
                            cheapestCost = cost;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fail open.
            }
        }
        if (!requiredOnTableCardPullVetoBypass
                && !objectiveRoutePullVetoBypass
                && cheapestCost != null
                && cheapestCost > availableForce) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, sourceTitle,
                    sourceCategory, false, cheapestCost, availableForce,
                    deadInterrupt, location, hosts, keyCharacter, context,
                    false, formation);
        }

        deadInterrupt = deadInterrupt(
                source, targets, gameState, context != null ? context.playerId() : null);
        if (deadInterrupt.blocked()) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, sourceTitle,
                    sourceCategory, false, cheapestCost, availableForce,
                    deadInterrupt, location, hosts, keyCharacter, context,
                    false, formation);
        }

        try {
            location = locationAssessment(
                    lower, sourceTitle, sourceText, targets, sourceCategory,
                    context, oracle);
        } catch (Exception ignored) {
            location = LocationAssessment.none();
        }
        if (location.v131State() == PullActionFacts.V131State.HARD_BLOCK) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, sourceTitle,
                    sourceCategory, false, cheapestCost, availableForce,
                    deadInterrupt, location, hosts, keyCharacter, context,
                    false, formation);
        }

        hosts = hostAssessment(
                lower, sourceTitle, targets, gameState,
                context != null ? context.playerId() : null);
        if ((!requiredOnTableCardPullVetoBypass
                && hostHardBlocked(location, hosts))
                || location.v131State() == PullActionFacts.V131State.DOWNGRADE) {
            return buildParent(actionId, text, reserveSize, "",
                    memoryValidation, sourceValidation, sourceTitle,
                    sourceCategory, false, cheapestCost, availableForce,
                    deadInterrupt, location, hosts, keyCharacter, context,
                    false, formation);
        }

        try {
            keyCharacter = keyCharacter(
                    sourceTitle, targets, sourceZone,
                    gameState, context);
        } catch (Exception ignored) {
            keyCharacter = KeyCharacter.none();
        }
        formation = formation(
                source, text, sourceText, targets,
                gameState, context, oracle);

        return buildParent(actionId, text, reserveSize, "",
                memoryValidation, sourceValidation, sourceTitle,
                sourceCategory, false, cheapestCost, availableForce,
                deadInterrupt, location, hosts, keyCharacter, context,
                true, formation, requiredOnTableCardPull,
                requiredOnTableCardPullVetoBypass,
                objectiveRoutePullVetoBypass,
                wmaopBlockadeSiteOnTable);
    }

    private static PullActionFacts.Parent buildParent(
            String actionId,
            String text,
            int reserveSize,
            String namedMissingTarget,
            PullOracleView.Validation memoryValidation,
            PullOracleView.Validation sourceValidation,
            String sourceTitle,
            CardCategory sourceCategory,
            boolean allUnattachable,
            Integer cheapestCost,
            int availableForce,
            DeadInterrupt deadInterrupt,
            LocationAssessment location,
            HostAssessment hosts,
            KeyCharacter keyCharacter,
            Context context,
            boolean includeLateContext,
            FormationAssessment formation) {
        // WMAOP 2026-08-08 (Steve directive): early-return veto paths default the
        // WMAOP table probe to false — every such path is already a hard veto.
        // formation, false, false, false);
        return buildParent(
                actionId, text, reserveSize, namedMissingTarget,
                memoryValidation, sourceValidation, sourceTitle,
                sourceCategory, allUnattachable, cheapestCost,
                availableForce, deadInterrupt, location, hosts,
                keyCharacter, context, includeLateContext,
                formation, false, false, false, false);
    }

    private static PullActionFacts.Parent buildParent(
            String actionId,
            String text,
            int reserveSize,
            String namedMissingTarget,
            PullOracleView.Validation memoryValidation,
            PullOracleView.Validation sourceValidation,
            String sourceTitle,
            CardCategory sourceCategory,
            boolean allUnattachable,
            Integer cheapestCost,
            int availableForce,
            DeadInterrupt deadInterrupt,
            LocationAssessment location,
            HostAssessment hosts,
            KeyCharacter keyCharacter,
            Context context,
            boolean includeLateContext,
            FormationAssessment formation,
            boolean requiredOnTableCardPull,
            boolean requiredOnTableCardPullVetoBypass,
            boolean objectiveRoutePullVetoBypass,
            // WMAOP 2026-08-08 (Steve directive): see readParent probe above.
            boolean wmaopBlockadeSiteOnTable) {
        boolean charactersOrVehiclesInHand = false;
        boolean battlePlausible = false;
        if (includeLateContext && context != null && context.lateView() != null) {
            try {
                charactersOrVehiclesInHand = hasCharacterOrVehicle(
                        context.lateView().hand());
            } catch (Exception ignored) {
                // Fail open.
            }
            try {
                battlePlausible = context.lateView().battlePlausible();
            } catch (Exception ignored) {
                // Fail open.
            }
        }
        return new PullActionFacts.Parent(
                actionId,
                text,
                reserveSize,
                false,
                namedMissingTarget,
                memoryValidation,
                sourceValidation,
                sourceTitle,
                allUnattachable,
                cheapestCost,
                availableForce,
                deadInterrupt.blocked(),
                deadInterrupt.targets(),
                deadInterrupt.reserves(),
                location.locationPull(),
                location.reason(),
                sourceCategory,
                location.v131State(),
                location.v131Reason(),
                hosts.weaponPull(),
                hosts.weaponReason(),
                hosts.lightsaberPull(),
                hosts.unarmedCharacters(),
                hosts.armedCharacters(),
                hosts.capableLightsaberWielders(),
                hosts.devicePull(),
                hosts.deviceReason(),
                hosts.deviceUnarmedCharacters(),
                hosts.deviceArmedCharacters(),
                keyCharacter.token(),
                keyCharacter.filled(),
                context != null ? context.phase() : null,
                text.toLowerCase(Locale.ROOT).contains("[download]"),
                charactersOrVehiclesInHand,
                battlePlausible,
                formation.state(),
                formation.reason(),
                requiredOnTableCardPull,
                requiredOnTableCardPullVetoBypass,
                objectiveRoutePullVetoBypass,
                wmaopBlockadeSiteOnTable);
    }

    // WMAOP 2026-08-08 (Steve directive): shared "Blockade Flagship site on
    // table" probe. Primary idiom is the engine-typed spot with the card's OWN
    // deploy filter (Card12_163.java:83) — Filters.canSpot(game, null,
    // Filters.siteOfStarshipOrVehicle(Persona.BLOCKADE_FLAGSHIP, true)) — with
    // the historical V142 getTopLocations title loop kept as the fallback.
    // Used by the V142 pre-pass gate (both bot mirrors), the shared PULL parent
    // facts above, and the WMAOP.FODDER_HOLD read in ForceLossFacts.
    public static boolean blockadeFlagshipSiteOnTable(
            SwccgGame game, GameState gameState) {
        try {
            if (game != null && Filters.canSpot(game, null,
                    Filters.siteOfStarshipOrVehicle(
                            Persona.BLOCKADE_FLAGSHIP, true))) {
                return true;
            }
        } catch (Exception ignored) {
            // Fall through to the title probe.
        }
        try {
            if (gameState != null) {
                for (PhysicalCard location : gameState.getTopLocations()) {
                    if (location != null && location.getTitle() != null
                            && location.getTitle().toLowerCase(Locale.ROOT)
                                .contains("blockade flagship")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fail open exactly like the surrounding readers.
        }
        return false;
    }

    private static int safeReserveSize(Context context) {
        if (context == null || context.gameState() == null
                || context.playerId() == null) {
            return -1;
        }
        try {
            return context.gameState().getReserveDeckSize(context.playerId());
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static String sourceTitle(PhysicalCard source) {
        try {
            return source != null && source.getTitle() != null
                    ? source.getTitle() : "?";
        } catch (Exception ignored) {
            return "?";
        }
    }

    private static CardCategory sourceCategory(PhysicalCard source) {
        try {
            return source != null && source.getBlueprint() != null
                    ? source.getBlueprint().getCardCategory() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String sourceText(
            PhysicalCard source, Context context, PullOracleView oracle) {
        if (source == null || oracle == null || context == null) {
            return null;
        }
        try {
            return source.getBlueprint() != null
                    ? oracle.sourceCardFullGameText(
                            source.getBlueprint(), context.side())
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> pullTargets(
            String sourceText, PullOracleView oracle) {
        if (sourceText == null || oracle == null) {
            return List.of();
        }
        try {
            List<String> targets = oracle.parseSourceCardPullTargets(sourceText);
            return targets != null ? targets : List.of();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static Zone sourceZone(String text, PullOracleView oracle) {
        if (oracle == null) {
            return null;
        }
        try {
            return oracle.parseSourceZone(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PullOracleView.Validation sourceValidation(
            Zone zone, String sourceText, PullOracleView oracle) {
        try {
            PullOracleView.Validation validation =
                    oracle.validatePullFromSourceCard(zone, sourceText);
            return validation != null ? validation : unknownValidation();
        } catch (Exception ignored) {
            return unknownValidation();
        }
    }

    private static boolean safeAllUnattachable(
            PullOracleView oracle,
            SwccgGame game,
            String playerId,
            List<String> targets) {
        try {
            return oracle.reserveTargetsAreAllUnattachableWeapons(
                    game, playerId, targets);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hostHardBlocked(
            LocationAssessment location, HostAssessment hosts) {
        if (hosts.weaponPull() && !location.locationPull()) {
            return hosts.unarmedCharacters() == 0
                    || (hosts.lightsaberPull()
                    && hosts.capableLightsaberWielders() == 0);
        }
        return hosts.devicePull() && !location.locationPull()
                && !hosts.weaponPull()
                && hosts.deviceUnarmedCharacters() == 0;
    }

    private static PullActionFacts.EarlySearch none(String actionId) {
        return new PullActionFacts.EarlySearch(
                actionId, PullActionFacts.EarlyGate.NONE, "");
    }

    private static PhysicalCard sourceCard(GameState gameState, String sourceCardId) {
        if (gameState == null || sourceCardId == null) {
            return null;
        }
        try {
            return gameState.findCardById(Integer.parseInt(sourceCardId));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String namedMissingTarget(String actionText, PullOracleView oracle) {
        if (oracle == null || !oracle.isAvailable()) {
            return "";
        }
        try {
            Matcher matcher = NAMED_ACTION_TARGET.matcher(actionText);
            if (matcher.find()) {
                String target = matcher.group(1).trim();
                if (!oracle.hasTargetInReserve(target.split(" "))) {
                    return target;
                }
            }
        } catch (Exception ignored) {
            // Fail open.
        }
        return "";
    }

    private static PullOracleView.Validation memoryValidation(
            String actionText, PullOracleView oracle) {
        if (oracle == null || !oracle.isAnalyzed()) {
            return unknownValidation();
        }
        Zone zone = oracle.parseSourceZone(actionText);
        if (zone == null) {
            return unknownValidation();
        }
        String[] keywords = null;
        Matcher named = MEMORY_NAMED_TARGET.matcher(actionText);
        if (named.find()) {
            keywords = named.group(1).trim().split(" ");
        } else {
            Matcher generic = MEMORY_GENERIC_TARGET.matcher(actionText);
            if (generic.find()) {
                String keyword = generic.group(1).trim();
                if (!MEMORY_STOPWORDS.contains(keyword.toLowerCase(Locale.ROOT))
                        && keyword.length() >= 3) {
                    keywords = new String[]{keyword};
                }
            }
        }
        if (keywords == null) {
            return unknownValidation();
        }
        try {
            PullOracleView.Validation validation = oracle.validatePull(zone, keywords);
            return validation != null ? validation : unknownValidation();
        } catch (Exception ignored) {
            return unknownValidation();
        }
    }

    private static PullOracleView.Validation unknownValidation() {
        return new PullOracleView.Validation(PullOracleView.Outcome.UNKNOWN, "");
    }

    private static boolean matchesAnyTarget(String title, List<String> targets) {
        for (String target : targets) {
            if (target == null) {
                continue;
            }
            String lower = target.toLowerCase(Locale.ROOT);
            String stripped = lower.replaceAll("\\[[^\\]]*\\]", " ")
                    .replaceAll("\\s+", " ").trim();
            if (title.contains(lower)
                    || (!stripped.isEmpty() && title.contains(stripped))) {
                return true;
            }
        }
        return false;
    }

    private static DeadInterrupt deadInterrupt(
            PhysicalCard source,
            List<String> targets,
            GameState gameState,
            String playerId) {
        if (source == null || source.getBlueprint() == null
                || source.getBlueprint().getCardCategory() != CardCategory.INTERRUPT
                || targets.isEmpty() || gameState == null || playerId == null) {
            return new DeadInterrupt(false, targets.toString(), 0);
        }
        try {
            Collection<PhysicalCard> table = gameState.getAllPermanentCards();
            for (String target : targets) {
                boolean found = false;
                String lower = target.toLowerCase(Locale.ROOT);
                for (PhysicalCard card : table) {
                    if (card != null && card.getTitle() != null
                            && card.getTitle().toLowerCase(Locale.ROOT).contains(lower)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return new DeadInterrupt(false, targets.toString(), 0);
                }
            }
            int reserves = gameState.getForcePileSize(playerId)
                    + (gameState.getUsedPile(playerId) != null
                    ? gameState.getUsedPile(playerId).size() : 0)
                    + gameState.getReserveDeckSize(playerId);
            return new DeadInterrupt(reserves >= 15, targets.toString(), reserves);
        } catch (Exception ignored) {
            return new DeadInterrupt(false, targets.toString(), 0);
        }
    }

    private static LocationAssessment locationAssessment(
            String actionLower,
            String sourceTitle,
            String sourceText,
            List<String> targets,
            CardCategory sourceCategory,
            Context context,
            PullOracleView oracle) {
        boolean location = false;
        String reason = "";
        for (String keyword : LOCATION_KEYWORDS) {
            if (actionLower.contains(keyword)) {
                location = true;
                reason = "actionText contains location keyword '" + keyword + "'";
                break;
            }
        }
        if (!location) {
            outer:
            for (String target : targets) {
                for (String keyword : LOCATION_KEYWORDS) {
                    if (target != null && target.contains(keyword)) {
                        location = true;
                        reason = "source card '" + sourceTitle
                                + "' game text targets location-like '" + target + "'";
                        break outer;
                    }
                }
            }
        }
        if (!location && sourceText != null) {
            Matcher matcher = LOCATION_SOURCE_PATTERN.matcher(sourceText);
            if (matcher.find()) {
                location = true;
                reason = "V82 source-text regex: '" + sourceTitle
                        + "' pulls a " + matcher.group(1) + " from Reserve";
            }
        }

        PullActionFacts.V131State state = location
                ? PullActionFacts.V131State.OPEN
                : PullActionFacts.V131State.CLOSED;
        String v131Reason = "";
        if (location && oracle != null && oracle.isAnalyzed() && context != null
                && context.game() != null && context.playerId() != null) {
            boolean sawLocation = false;
            boolean sawWeapon = false;
            boolean downgrade = false;
            for (String target : targets) {
                if (target == null) {
                    continue;
                }
                String lower = target.toLowerCase(Locale.ROOT);
                boolean targetLocation = containsAny(lower, V131_LOCATION_NOUNS);
                boolean targetWeapon = containsAny(lower, V131_WEAPON_NOUNS);
                sawWeapon |= targetWeapon;
                if (targetLocation) {
                    sawLocation = true;
                    int inDeck = oracle.countMatchingInDeck(
                            context.game(), context.playerId(), target);
                    if (inDeck == 0) {
                        return new LocationAssessment(true, reason,
                                PullActionFacts.V131State.HARD_BLOCK,
                                "target '" + target + "' not in deck at all");
                    }
                    int inHandTable = oracle.countMatchingInHandOrTable(
                            context.game(), context.playerId(), target);
                    if (inHandTable >= 1) {
                        downgrade = true;
                        v131Reason = "target '" + target + "' already satisfied ("
                                + inHandTable + " in hand+table)";
                    }
                }
            }
            if (sawWeapon && !sawLocation) {
                state = PullActionFacts.V131State.CLOSED;
                v131Reason = "parsed targets are weapon-only; V67l mis-detected";
            } else if (downgrade) {
                state = PullActionFacts.V131State.DOWNGRADE;
            }
        }
        return new LocationAssessment(location, reason, state, v131Reason);
    }

    private static HostAssessment hostAssessment(
            String actionLower,
            String sourceTitle,
            List<String> targets,
            GameState gameState,
            String playerId) {
        KeywordMatch weapon = keywordMatch(
                actionLower, sourceTitle, targets, WEAPON_KEYWORDS, "weapon");
        KeywordMatch device = keywordMatch(
                actionLower, sourceTitle, targets, DEVICE_KEYWORDS, "device");
        int unarmed = 0;
        int armed = 0;
        int capable = 0;
        int deviceUnarmed = 0;
        int deviceArmed = 0;
        boolean lightsaber = weapon.reason().toLowerCase(Locale.ROOT).contains("lightsaber")
                || weapon.reason().toLowerCase(Locale.ROOT).contains("saber");
        if (gameState != null && playerId != null) {
            try {
                for (PhysicalCard card : gameState.getAllPermanentCards()) {
                    if (!isFriendlyInPlayCharacter(card, playerId)) {
                        continue;
                    }
                    boolean hasWeapon = hasAttachmentCategory(
                            gameState, card, CardCategory.WEAPON);
                    if (hasWeapon) {
                        armed++;
                    } else {
                        unarmed++;
                        if (lightsaber && card.getBlueprint().hasIcon(Icon.WARRIOR)
                                && card.getBlueprint().hasAbilityAttribute()) {
                            Float ability = card.getBlueprint().getAbility();
                            if (ability != null && ability >= 4.0f) {
                                capable++;
                            }
                        }
                    }
                    if (hasAttachmentCategory(gameState, card, CardCategory.DEVICE)) {
                        deviceArmed++;
                    } else {
                        deviceUnarmed++;
                    }
                }
            } catch (Exception ignored) {
                // Preserve fail-open behavior.
            }
        }
        return new HostAssessment(
                weapon.matched(), weapon.reason(), lightsaber,
                unarmed, armed, capable,
                device.matched(), device.reason(), deviceUnarmed, deviceArmed);
    }

    private static KeywordMatch keywordMatch(
            String actionLower,
            String sourceTitle,
            List<String> targets,
            String[] keywords,
            String family) {
        for (String keyword : keywords) {
            if (actionLower.contains(keyword)) {
                return new KeywordMatch(true,
                        "actionText contains " + family + " keyword '" + keyword + "'");
            }
        }
        for (String target : targets) {
            for (String keyword : keywords) {
                if (target != null && target.contains(keyword)) {
                    return new KeywordMatch(true,
                            "source card '" + sourceTitle + "' "
                                    + (family.equals("device") ? "targets" : "game text targets")
                                    + " " + family + "-like '" + target + "'");
                }
            }
        }
        return new KeywordMatch(false, "");
    }

    private static boolean isFriendlyInPlayCharacter(PhysicalCard card, String playerId) {
        return card != null && card.getBlueprint() != null
                && playerId.equals(card.getOwner())
                && card.getZone() != null && card.getZone().isInPlay()
                && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER;
    }

    private static boolean hasAttachmentCategory(
            GameState gameState, PhysicalCard card, CardCategory category) {
        List<PhysicalCard> attachments = gameState.getAttachedCards(card);
        if (attachments == null) {
            return false;
        }
        for (PhysicalCard attachment : attachments) {
            if (attachment != null && attachment.getBlueprint() != null
                    && attachment.getBlueprint().getCardCategory() == category) {
                return true;
            }
        }
        return false;
    }

    private static KeyCharacter keyCharacter(
            String sourceTitle,
            List<String> targets,
            Zone sourceZone,
            GameState gameState,
            Context context) {
        if (context == null || context.objective() == null
                || !context.objective().isAnalyzed()
                || context.playerId() == null) {
            return new KeyCharacter("", false);
        }
        String token = "";
        Set<String> strategyTokens = context.objective().strategyCharacterTokens(
                context.game(), context.playerId());
        if (strategyTokens != null) {
            outer:
            for (String target : targets) {
                String lower = target == null ? "" : target.toLowerCase(Locale.ROOT);
                for (String candidate : strategyTokens) {
                    if (candidate != null && lower.contains(candidate)) {
                        token = candidate;
                        break outer;
                    }
                }
            }
        }
        if (token.isEmpty() || gameState == null) {
            return new KeyCharacter(token, false);
        }
        boolean typedKeyRole =
                context.objective().hasTypedStrategyKeyCharacter();
        if (typedKeyRole) {
            Zone candidateZone = sourceZone != null
                    ? sourceZone : Zone.RESERVE_DECK;
            boolean typedTargetExists = false;
            Collection<PhysicalCard> pile =
                    gameState.getCardPile(
                            context.playerId(),
                            candidateZone);
            if (pile != null) {
                for (PhysicalCard card : pile) {
                    if (card == null || card.getTitle() == null
                            || !matchesAnyTarget(
                                card.getTitle()
                                    .toLowerCase(Locale.ROOT),
                                targets)) {
                        continue;
                    }
                    if (context.objective()
                            .isStrategyKeyCharacter(
                                context.game(),
                                context.playerId(),
                                card)) {
                        typedTargetExists = true;
                        break;
                    }
                }
            }
            if (!typedTargetExists) {
                return KeyCharacter.none();
            }
        }
        boolean filled = false;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (!isFriendlyInPlayCharacter(card, context.playerId())
                    || card.getTitle() == null) {
                continue;
            }
            boolean fillsSameRole = typedKeyRole
                    ? context.objective().isStrategyKeyCharacter(
                        context.game(), context.playerId(), card)
                    : card.getTitle()
                        .toLowerCase(Locale.ROOT)
                        .contains(token);
            if (fillsSameRole) {
                filled = true;
                break;
            }
        }
        return new KeyCharacter(token, filled);
    }

    private static FormationAssessment formation(
            PhysicalCard source,
            String actionText,
            String sourceText,
            List<String> targets,
            GameState gameState,
            Context context,
            PullOracleView oracle) {
        if (source == null || source.getBlueprint() == null
                || source.getBlueprint().getCardCategory() != CardCategory.LOCATION
                || sourceText == null || gameState == null || context == null
                || context.game() == null || context.playerId() == null || oracle == null) {
            return FormationAssessment.none();
        }
        if (!sourceText.toLowerCase(Locale.ROOT)
                .matches("(?s).*(?:\\[download\\]|deploy|take)[^.;]*\\bhere\\b.*")) {
            return FormationAssessment.none();
        }
        PhysicalCard pulled = null;
        try {
            for (PhysicalCard card : gameState.getReserveDeck(context.playerId())) {
                if (card != null && card.getBlueprint() != null
                        && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER
                        && oracle.sourceTargetsAnyTitle(targets, card)) {
                    pulled = card;
                    break;
                }
            }
        } catch (Exception ignored) {
            return FormationAssessment.none();
        }
        if (pulled == null) {
            return FormationAssessment.none();
        }

        boolean flipPlan = false;
        ObjectiveView objective = context.objective();
        if (objective != null
                && objective.objectivePullFormationExempt(
                    context.game(), context.playerId(),
                    source, actionText)) {
            return new FormationAssessment(
                    PullActionFacts.FormationState.FLIP_EXEMPT,
                    pulled.getTitle());
        }
        if (objective != null && objective.isAnalyzed() && !objective.isFlipped()
                && objective.flipConditionText() != null) {
            try {
                flipPlan = oracle.personaNamedInText(
                        pulled.getBlueprint().getPersonas(),
                        objective.flipConditionText().toLowerCase(Locale.ROOT));
            } catch (Exception ignored) {
                // No persona means no exemption.
            }
        }
        if (flipPlan) {
            return new FormationAssessment(
                    PullActionFacts.FormationState.FLIP_EXEMPT, pulled.getTitle());
        }

        try {
            SwccgCardBlueprint blueprint = pulled.getBlueprint();
            float force = gameState.getForcePileSize(context.playerId());
            FormationSafety.DeployVerdict verdict = FormationSafety.assessCharacterDeploy(
                    context.game(), gameState, context.playerId(), pulled,
                    blueprint.hasPowerAttribute() ? blueprint.getPower() : null,
                    blueprint.hasAbilityAttribute() ? blueprint.getAbility() : null,
                    false, source, force, blueprint.getDeployCost(), null, null);
            return switch (verdict.constraint()) {
                case HARD_BLOCK -> new FormationAssessment(
                        PullActionFacts.FormationState.HARD_BLOCK, verdict.reason());
                case DEFER_UNSUPPORTED_SOLO -> new FormationAssessment(
                        PullActionFacts.FormationState.DEFER, verdict.reason());
                case UNKNOWN -> new FormationAssessment(
                        PullActionFacts.FormationState.UNKNOWN, verdict.reason());
                default -> FormationAssessment.none();
            };
        } catch (Exception ignored) {
            return FormationAssessment.none();
        }
    }

    private static boolean containsAny(String text, Set<String> nouns) {
        for (String noun : nouns) {
            if (text.contains(noun)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCharacterOrVehicle(List<PhysicalCard> hand) {
        if (hand == null) {
            return false;
        }
        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) {
                continue;
            }
            CardCategory category = card.getBlueprint().getCardCategory();
            if (category == CardCategory.CHARACTER || category == CardCategory.VEHICLE) {
                return true;
            }
        }
        return false;
    }

    private record DeadInterrupt(boolean blocked, String targets, int reserves) {
        private static DeadInterrupt none() {
            return new DeadInterrupt(false, "[]", 0);
        }
    }

    private record LocationAssessment(
            boolean locationPull,
            String reason,
            PullActionFacts.V131State v131State,
            String v131Reason) {
        private static LocationAssessment none() {
            return new LocationAssessment(false, "",
                    PullActionFacts.V131State.CLOSED, "");
        }
    }

    private record KeywordMatch(boolean matched, String reason) {
    }

    private record HostAssessment(
            boolean weaponPull,
            String weaponReason,
            boolean lightsaberPull,
            int unarmedCharacters,
            int armedCharacters,
            int capableLightsaberWielders,
            boolean devicePull,
            String deviceReason,
            int deviceUnarmedCharacters,
            int deviceArmedCharacters) {
        private static HostAssessment none() {
            return new HostAssessment(false, "", false, 0, 0, 0,
                    false, "", 0, 0);
        }
    }

    private record KeyCharacter(String token, boolean filled) {
        private static KeyCharacter none() {
            return new KeyCharacter("", false);
        }
    }

    private record FormationAssessment(
            PullActionFacts.FormationState state,
            String reason) {
        private static FormationAssessment none() {
            return new FormationAssessment(PullActionFacts.FormationState.NONE, "");
        }
    }
}
