package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.ObjectiveSideBlueprints;
import com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;

import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: SVC-INTEL (reorg 2026-07-06) ═══
// Owns: the LIVE objective brain: objective detection (V25-detector), flip-condition intel (V22.2),
// objective-text parsing (V29-objtext), V67ak flip-critical intel, V186 I Want That Map, V160 SWBD.
// Owns objective DECISION scores as of 2026-07-07 (Steve: "singular ObjectiveAnalyzer — the deploy
// evaluator should check it"): getDeployObjectiveAdjustments() returns the objective deploy score
// notes the deploy phase applies. This REPLACES the reorg's original "owns NO scores" contract.
// Hub: none. KIND mix (SVC-INTEL overall): 4 ORDERING / 2 BANDED (intel plumbing).
// LOUD NOTE: strategy/ObjectiveHandler.java is NOT this brain — it is dead code; do not wire it.
// V207: this implementation is shared. Bot-local ObjectiveAnalyzer classes are compatibility
// facades that preserve their no-argument API, logger, and separate mutable analyzer instances.
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: PLAYBOOKS (deck scripts consume this intel), SETUP (turn-0 bootstrap), SVC-INTEL peers
// StrategyController / BattlePredictor / OpponentDeckTracker (V24.7). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
/**
 * V21 ObjectiveAnalyzer - Runtime parser for objective game text.
 *
 * Reads the game text from Rando's own objective card and extracts:
 * 1. FLIP CONDITIONS - what locations to occupy/control, what cards need to be on table
 * 2. PRIORITY LOCATIONS - location names from flip conditions (where to deploy)
 * 3. KEY CARDS - card names that must be "on table" to flip
 * 4. PULLABLE CARDS - cards the objective can take from Reserve Deck
 * 5. FLIPPED STATUS - whether the objective is currently flipped
 *
 * Uses pattern matching on standard SWCCG objective text conventions:
 *   "Flip this card if [condition]"
 *   "occupy [location]"  /  "control [location]"
 *   "[Card Name] on table"
 *   "may take [Card Name] into hand from Reserve Deck"
 *
 * This is Approach B: universal, no hardcoded per-objective knowledge needed.
 */
public class ObjectiveAnalyzer {
    private final Logger LOG;

    protected ObjectiveAnalyzer(Logger logger) {
        this.LOG = Objects.requireNonNull(logger, "logger");
    }

    private final Set<String> flipConditionLocations = new HashSet<>();
    private final Set<String> flipConditionLocationFragments = new HashSet<>();
    private final Set<String> requiredCardsOnTable = new HashSet<>();
    private final Set<String> pullableCards = new HashSet<>();
    // NEW setup slots (2026-07-08, JSON playbook): starting locations/effects/interrupts NAMED by the
    // objective (e.g. Luke Saga starting location, IWTM preferred starting effect). Hydrated from
    // objective_playbooks.json; consumers (CardSelection starting-chooser) wire per objective. Ids +
    // title fragments, virtual-reprint proof. Empty until a profile populates them.
    private final Set<String> startingLocationIds = new HashSet<>();
    private final Set<String> startingLocationFragments = new HashSet<>();
    private final Set<String> startingEffectIds = new HashSet<>();
    private final Set<String> startingEffectFragments = new HashSet<>();
    private final Set<String> startingInterruptIds = new HashSet<>();
    private final Set<String> startingInterruptFragments = new HashSet<>();
    private boolean hydratedFromJson = false;   // true once a JSON profile hydrated this objective
    // Loader-extension step 3b (2026-07-10): the active loaderEnabled profile's rules, stored so the
    // filter-based objective-relevance overload can evaluate them against real location cards at runtime.
    // Null unless a loaderEnabled profile carried rules (none do yet → behavior-neutral).
    private List<FlipLocationRule> activeFlipLocationRules = null;
    private List<ActorLocationRule> activeActorLocationRules = null;
    // V22.2: Back side / flip-back protection
    private final Set<String> flipBackLocations = new HashSet<>();
    private final Set<String> flipBackLocationFragments = new HashSet<>();
    private String flipBackConditionText = null;
    private boolean flipBackRequiresOccupy = false;
    private boolean flipBackRequiresControl = false;
    private String flipConditionText = null;
    // V193 (Steve, 2026-07-07): Endor Operations flip-gate control site. Establish Secret
    // Base (V) (207_25) "Deploy on Bunker if you control that site" — the last flip-card can
    // only reach the table once Rando CONTROLS Endor: Bunker. A title-keyed playbook block
    // (parseFlipCondition) names that gate site so DeployEvaluator can steer one body there.
    // null for every objective without an explicit flip-gate control site.
    private String flipCriticalControlSite = null;
    // The card whose deploy REQUIRES controlling flipCriticalControlSite (e.g. Establish
    // Secret Base needs Endor: Bunker). Kept HERE (objective logic) so DeployEvaluator's
    // flip-gate steer is fully general — no card names hardcoded in the deploy scorer.
    // This is the DISPLAY name (log/reasoning); DETECTION uses flipCriticalControlCardIds.
    private String flipCriticalControlCard = null;
    // FIX (Steve, 2026-07-07): the EXACT blueprint ids whose deploy is gated on controlling
    // flipCriticalControlSite. "Establish Secret Base" has three printings that SHARE the
    // title but NOT the deploy gate: base 8_124 deploys on Endor SYSTEM gated on controlling
    // 3 Endor sites (NOT Bunker); V 207_25 deploys on Bunker gated on Bunker; Legacy V 601_260
    // deploys on Endor system but is ALSO gated on controlling Bunker. So the Bunker steer is
    // correct ONLY for {207_25, 601_260}. Matching by the shared title would misfire the +400
    // when a deck runs the base 8_124. When non-empty, DeployEvaluator detects by these ids
    // (blueprint-id exact match) instead of the title. Empty → fall back to the title name.
    private final Set<String> flipCriticalControlCardIds = new HashSet<>();
    private String objectiveTitle = null;
    private String objectiveBlueprintId = null;
    // V29 UPDATED 2026-07-06: keep the raw objective game text so evaluators can check clauses
    // the fragment parsers don't capture (e.g. "may not deploy ... Executor" on TDIGWATT (V)).
    private String objectiveGameText = null;
    private boolean analyzed = false;
    private boolean isFlipped = false;
    private boolean requiresOccupy = false;
    private boolean requiresControl = false;

    // V86/V88/V121 CONSOLIDATED (2026-07-07): objective-IDENTITY flags. Title-derived, set in
    // analyze() right after objectiveTitle so every evaluator reads ONE definition instead of
    // re-matching the objective title string inline. The Deploy/CardSelection scoring bodies stay
    // in place (additive-score ORDERING unchanged — moving a branch past a veto/early-return would
    // silently flip decisions) but now gate on these getters. Steve 2026-07-07: "move the version
    // logic to ObjectiveAnalyzer — that's what the deploy evaluator should be looking at."
    private boolean isInvasion = false;   // objective title contains "invasion"
    private boolean isMyLord = false;      // title contains "my lord" OR "make it legal" (MLITL)
    private boolean isEndor = false;       // title contains "endor operations" (ENDOR_PLAYBOOK pilot 2026-07-07)
    private boolean isTdigwatt = false;    // title contains "this deal is getting worse all the time"
    // ObjectivePlaybook pilot (2026-07-07): the active objective's typed profile, or null. Selected
    // in analyze() when the objective matches a known playbook; consumed via getActivePlaybook().
    private ObjectivePlaybook activePlaybook = null;
    // V186 CONSOLIDATED (2026-07-07): I Want That Map identity + TYPED steer data. The +400
    // Starkiller-system pick and +1000 preferred-starting-effect pick in CardSelectionEvaluator
    // used to hardcode these ids/titles inline; they now read these slots. Populated title-derived
    // in analyze() (NOT the flip block) so they're set whether or not the objective exposes a flip
    // pattern — matching the old evaluator's title-derived gate exactly (Codex review 2026-07-07).
    private boolean isWantThatMap = false;
    private final java.util.Set<String> iwtmSystemBpIds = new java.util.LinkedHashSet<>(); // {208_51, 208_051}
    private String iwtmSystemTitleFragment = null;      // "starkiller base" (SYSTEM; caller keeps its no-colon guard)
    private String iwtmPreferredStartingEffect = null;  // "the first order was just the beginning"

    // V25: ISB Operations awareness
    private boolean isISBOperations = false;
    private int isbFlipAgentCount = 0;         // How many ISB agents needed on table to flip (e.g. 4)
    private int isbFlipLocationCount = 0;      // How many Rebel Base locations ISB agents must control (e.g. 2)
    private boolean isbFlipBackNoAgents = false; // Flips back if no ISB agents on table

    // V25: Hunt Down V awareness
    private boolean isHuntDownV = false;
    private boolean huntDownNeedsVader = false;       // Vader required at battleground to flip
    private boolean huntDownFlipBackNoVader = false;   // Flips back if Vader not on table

    // V160 (Steve, 2026-05-29): Shield Will Be Down In Moments awareness
    // Dark Hoth invasion deck. Flip condition: Main Power Generators "blown away."
    // Win path: deploy Target The Main Generator (Epic Event) on Ice Plains; position
    // AT-AT Cannon at 3rd Marker or lower; fire each deploy phase until destiny > 8.
    private boolean isShieldWillBeDown = false;

    private static final Pattern FLIP_PATTERN = Pattern.compile(
        "Flip this card if (.+?)(?:\\.|\\\\|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern OCCUPY_LOCATION_PATTERN = Pattern.compile(
        "(?:you )?occupy\\s+(.+?)(?:\\s+and\\s+|,\\s+|\\.|\\\\|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern CONTROL_LOCATION_PATTERN = Pattern.compile(
        "(?:you )?control\\s+(.+?)(?:\\s+and\\s+|,\\s+|\\.|\\\\|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern ON_TABLE_PATTERN = Pattern.compile(
        "([A-Z][\\w\\s',!\\-()]+?)\\s+(?:is )?on table", Pattern.CASE_INSENSITIVE);

    private static final Pattern TAKE_FROM_RESERVE_PATTERN = Pattern.compile(
        "may take\\s+(.+?)\\s+into hand from Reserve Deck", Pattern.CASE_INSENSITIVE);

    // V21 ADJUSTED 2026-07-11b (replay kxn8bvydcd803p2j, TDIGWATT (V) 226_12): virtual objectives
    // write the pull as "may [upload] Dark Deal, Vader's Bounty, or [Special Edition] Bespin" —
    // the take-into-hand pattern never matched, so pullableCards stayed EMPTY, the V21 objective-
    // critical never-lose protection never armed, and Rando pitched Bespin + Dark Deal as Force-
    // loss fodder. [upload] = take into hand from Reserve Deck (icon shorthand).
    private static final Pattern UPLOAD_FROM_RESERVE_PATTERN = Pattern.compile(
        "may \\[upload\\]\\s+([^.;]+?)(?=\\.|;|$)", Pattern.CASE_INSENSITIVE);

    /**
     * Runtime truth for one structured location rule from objective_playbooks.json.
     * Alternative ids are stable within a rule and identify which branch is met or missing.
     */
    public record FlipLocationRuleState(
            String ruleId,
            String mode,
            boolean conditionSatisfied,
            Set<String> satisfiedAlternatives,
            Set<String> missingAlternatives) { }

    /** Structured objective role lost if this card leaves play. */
    public enum FlipGateFormationRole {
        LAST_REQUIRED_ACTOR,
        LAST_REQUIRED_BUDDY,
        LAST_FLIP_BACK_BLOCKER,
        NONE
    }

    /** How one Reserve Deck or hand candidate advances a counted pre-flip rule. */
    public enum ObjectiveProgressCandidateRole {
        REQUIRED_LOCATION,
        REQUIRED_ACTOR,
        NONE
    }

    /** Current counterfactual risk for one counted post-flip location rule. */
    public record PostFlipLocationRisk(
            boolean applies,
            boolean inScope,
            boolean flipBackNow,
            boolean opponentControlsHere,
            boolean criticalIfSelfControlLost,
            boolean criticalIfOpponentGainsControl,
            int selfControlCount,
            int opponentControlCount,
            int adverseStepsRemaining) {
        public boolean requiresProtection() {
            return applies && inScope
                    && (opponentControlsHere
                        || criticalIfSelfControlLost
                        || criticalIfOpponentGainsControl);
        }

        private static PostFlipLocationRisk none() {
            return new PostFlipLocationRisk(
                    false, false, false, false,
                    false, false, 0, 0, 0);
        }
    }

    public void analyze(SwccgGame game, String playerId, Side side) {
        LOG.warn("[ObjectiveAnalyzer] analyze() CALLED - game={}, player={}, side={}", game != null, playerId, side);
        if (game == null || playerId == null) return;

        GameState gameState = game.getGameState();
        if (gameState == null) return;

        try {
            PhysicalCard objectiveCard = findOurObjective(gameState, playerId);
            if (objectiveCard == null) {
                LOG.warn("[ObjectiveAnalyzer] No objective found for {}", playerId);
                return;
            }

            ObjectiveSideBlueprints sides = ObjectiveSideBlueprints.resolve(objectiveCard);
            if (sides == null) return;

            String title = sides.front().getTitle();
            String gameText = sides.front().getGameText();
            String backGameText = sides.back() != null ? sides.back().getGameText() : null;

            if (gameText == null || gameText.isEmpty()) {
                LOG.warn("[ObjectiveAnalyzer] Objective '{}' has no game text", title);
                return;
            }

            String bpId = objectiveCard.getBlueprintId(true);
            if (analyzed && bpId != null && bpId.equals(objectiveBlueprintId)) {
                updateFlipStatus(objectiveCard);
                return;
            }

            this.objectiveTitle = title;
            this.objectiveBlueprintId = bpId;
            // V86/V88/V121 CONSOLIDATED (2026-07-07): compute objective-identity flags from the
            // title HERE (before parseGameText's no-flip early return) so they are set for EVERY
            // objective, flip-parsed or not. Exact substring semantics preserved verbatim from the
            // old inline checks in Deploy/CardSelection (title.toLowerCase().contains(...)).
            String titleLowerId = (title != null) ? title.toLowerCase(Locale.ROOT) : "";
            this.isInvasion = titleLowerId.contains("invasion");
            this.isMyLord = titleLowerId.contains("my lord") || titleLowerId.contains("make it legal");
            this.isEndor = titleLowerId.contains("endor operations");
            this.isTdigwatt = titleLowerId.contains("this deal is getting worse all the time");
            // ObjectivePlaybook (2026-07-08): when the objective has a loaderEnabled JSON profile, the ACTIVE
            // playbook is BUILT FROM THE JSON (analyzer = pointer to the data). Otherwise fall back to the
            // compiled statics (My Lord/Endor) — the loaderEnabled=false path. prof/loaderOn reused post-parse
            // for slot hydration (one lookup). buildPlaybookFromProfile weights == the compiled statics for the
            // two enabled pilots (boundary-verified), so this is behavior-neutral today.
            JsonProfile prof = findProfile(bpId, title);
            boolean loaderOn = prof != null && Boolean.TRUE.equals(prof.loaderEnabled);
            this.activePlaybook = loaderOn ? buildPlaybookFromProfile(prof)
                                : this.isMyLord ? MY_LORD_PLAYBOOK
                                : this.isEndor ? ENDOR_PLAYBOOK
                                : null;
            // V186 CONSOLIDATED (2026-07-07): I Want That Map identity + typed steer data (title-derived).
            this.isWantThatMap = titleLowerId.contains("i want that map");
            iwtmSystemBpIds.clear();
            iwtmSystemTitleFragment = null;
            iwtmPreferredStartingEffect = null;
            if (this.isWantThatMap) {
                iwtmSystemBpIds.add("208_51");
                iwtmSystemBpIds.add("208_051");
                iwtmSystemTitleFragment = "starkiller base";
                iwtmPreferredStartingEffect = "the first order was just the beginning";
            }
            // V29 UPDATED 2026-07-06: store raw text for objectiveForbidsDeployingExecutor()
            this.objectiveGameText = gameText;
            LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Analyzing objective: '{}'", title);
            LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Game text: {}", gameText);

            parseGameText(gameText, backGameText);
            // ObjectivePlaybook JSON hydration (2026-07-08): pull scoring-slot data from the single
            // runtime source (objective_playbooks.json) for the active objective. ADDITIVE + idempotent
            // — runs AFTER the text parser / hardcoded blocks, so where both fill a slot the values are
            // identical. Hard fallback: no profile / empty registry → parser output stands unchanged.
            // GATED per-objective by loaderEnabled (computed above): the canonical file carries all 58
            // profiles, but only boundary-math-VERIFIED objectives (My Lord + Endor today) hydrate. This
            // prevents the 56 un-verified profiles from silently altering behavior.
            if (loaderOn) hydrateFromProfile(prof);
            updateFlipStatus(objectiveCard);
            this.analyzed = true;

            logAnalysisResults();

        } catch (Exception e) {
            LOG.warn("[ObjectiveAnalyzer] Error analyzing objective: {}", e.getMessage());
        }
    }

    public boolean isObjectiveRelevantLocation(String locationTitle) {
        if (!analyzed || locationTitle == null) return false;
        String titleLower = locationTitle.toLowerCase(Locale.ROOT);

        Boolean structuredMatch = exactStructuredTitleMatch(
                titleLower, "preFlip", "flip");
        if (structuredMatch != null) return structuredMatch;

        if (flipConditionLocations.contains(titleLower)) return true;

        for (String fragment : flipConditionLocationFragments) {
            if (titleLower.contains(fragment)) return true;
        }

        return false;
    }

    /**
     * Evaluates typed objective location conditions against current table state.
     * Unknown relations and filters fail closed.
     */
    public List<FlipLocationRuleState> assessFlipLocationRules(
            SwccgGame game, String playerId, String phase, String purpose) {
        if (!analyzed || game == null || playerId == null
                || phase == null || purpose == null
                || activeFlipLocationRules == null) {
            return Collections.emptyList();
        }

        List<FlipLocationRuleState> states = new ArrayList<>();
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule == null || !phase.equals(rule.phase)
                    || !purpose.equals(rule.purpose)
                    || rule.alternatives == null) {
                continue;
            }

            Set<String> satisfied = new LinkedHashSet<>();
            Set<String> missing = new LinkedHashSet<>();
            for (int i = 0; i < rule.alternatives.size(); i++) {
                FlipLocationAlternative alternative = rule.alternatives.get(i);
                String alternativeId = rule.id + ":" + (i + 1);
                if (isFlipLocationAlternativeSatisfied(
                        game, playerId, alternative)) {
                    satisfied.add(alternativeId);
                } else {
                    missing.add(alternativeId);
                }
            }

            boolean anyOf = "anyOf".equals(rule.mode);
            boolean conditionSatisfied = anyOf
                    ? !satisfied.isEmpty()
                    : missing.isEmpty() && !rule.alternatives.isEmpty();
            states.add(new FlipLocationRuleState(
                    rule.id,
                    rule.mode,
                    conditionSatisfied,
                    Collections.unmodifiableSet(satisfied),
                    Collections.unmodifiableSet(missing)));
        }
        return Collections.unmodifiableList(states);
    }

    public boolean hasCountedPreFlipActorRule() {
        if (!analyzed || isFlipped || activeFlipLocationRules == null) {
            return false;
        }
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule == null || !"preFlip".equals(rule.phase)
                    || !"flip".equals(rule.purpose)
                    || rule.alternatives == null) {
                continue;
            }
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (alternative != null && alternative.actorFilterKey != null
                        && alternative.count != null
                        && alternative.count.value != null
                        && alternative.count.value > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Classifies a real candidate by whether it fills the next typed location
     * or actor leg of a counted pre-flip rule.
     */
    public ObjectiveProgressCandidateRole classifyPreFlipProgressCandidate(
            SwccgGame game, String playerId, PhysicalCard candidate) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || candidate == null || activeFlipLocationRules == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return ObjectiveProgressCandidateRole.NONE;
        }

        try {
            GameState gameState = game.getGameState();
            for (FlipLocationRule rule : activeFlipLocationRules) {
                if (!isActivePreFlipRule(rule) || isRuleSatisfied(
                        game, playerId, rule)) {
                    continue;
                }
                for (FlipLocationAlternative alternative : rule.alternatives) {
                    if (alternative == null || alternative.count == null
                            || alternative.count.value == null
                            || alternative.count.value <= 0) {
                        continue;
                    }
                    int required = expectedCount(
                            game, playerId, alternative.count);
                    int qualified = countAlternativeMatches(
                            game, playerId, playerId, alternative);
                    if (qualified >= required) continue;

                    com.gempukku.swccgo.filters.Filter locationFilter =
                            resolveLocationFilter(
                                    alternative.locationFilterKey, playerId);
                    if (locationFilter != null
                            && locationFilter.accepts(
                                    gameState, game.getModifiersQuerying(),
                                    candidate)
                            && countMatchingLocations(
                                    game, playerId, alternative) < required) {
                        return ObjectiveProgressCandidateRole
                                .REQUIRED_LOCATION;
                    }

                    com.gempukku.swccgo.filters.Filter actorFilter =
                            resolveFilter(alternative.actorFilterKey);
                    if (actorFilter != null
                            && actorFilter.accepts(
                                    gameState, game.getModifiersQuerying(),
                                    candidate)
                            && (required > 1
                                || !hasOtherMatchingActorInHand(
                                        game, playerId, candidate,
                                        actorFilter)
                                    && !hasOtherMatchingActorOnPreFlipRoute(
                                        game, playerId, candidate,
                                        actorFilter))) {
                        List<PhysicalCard> locations =
                                gameState.getLocationsInOrder();
                        if (locations == null) continue;
                        for (PhysicalCard location : locations) {
                            if (advancesAlternativeAt(
                                    game, playerId, candidate, location,
                                    alternative)) {
                                return ObjectiveProgressCandidateRole
                                        .REQUIRED_ACTOR;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Objective progress candidate assessment failed: {}",
                    e.getMessage());
        }
        return ObjectiveProgressCandidateRole.NONE;
    }

    private boolean hasOtherMatchingActorInHand(
            SwccgGame game,
            String playerId,
            PhysicalCard candidate,
            com.gempukku.swccgo.filters.Filter actorFilter) {
        GameState gameState = game.getGameState();
        if (gameState == null || actorFilter == null) return false;
        List<PhysicalCard> hand = gameState.getHand(playerId);
        if (hand == null) return false;
        for (PhysicalCard card : hand) {
            if (card != null && !samePhysicalLocation(card, candidate)
                    && playerId.equals(card.getOwner())
                    && actorFilter.accepts(
                            gameState, game.getModifiersQuerying(),
                            card)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A count-one actor is already actionable only when it is in play on a
     * site route to the exact gate. A matching pilot aboard a starship or an
     * actor stranded in unrelated geography must not suppress another pull.
     */
    private boolean hasOtherMatchingActorOnPreFlipRoute(
            SwccgGame game,
            String playerId,
            PhysicalCard candidate,
            com.gempukku.swccgo.filters.Filter actorFilter) {
        GameState gameState = game.getGameState();
        if (gameState == null || actorFilter == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        List<PhysicalCard> locations = gameState.getLocationsInOrder();
        Collection<PhysicalCard> permanents =
                gameState.getAllPermanentCards();
        if (locations == null || permanents == null) return false;

        for (PhysicalCard gate : locations) {
            if (gate == null || !isFlipGateLocation(
                    game, playerId, gate)) {
                continue;
            }
            for (PhysicalCard card : permanents) {
                if (card == null || samePhysicalLocation(card, candidate)
                        || !playerId.equals(card.getOwner())
                        || card.getZone() == null
                        || !card.getZone().isInPlay()
                        || card.isCaptive()
                        || card.isUndercover()
                        || !actorFilter.accepts(
                                gameState, game.getModifiersQuerying(),
                                card)) {
                    continue;
                }
                PhysicalCard actorLocation =
                        game.getModifiersQuerying()
                                .getLocationThatCardIsPresentAt(
                                        gameState, card);
                if (samePhysicalLocation(actorLocation, gate)) {
                    return true;
                }
                Integer distance = actorLocation != null
                        ? game.getModifiersQuerying()
                                .getDistanceBetweenSites(
                                        gameState, actorLocation, gate)
                        : null;
                if (distance != null && distance > 0) return true;
            }
        }
        return false;
    }

    /**
     * True when this typed candidate deployed to this exact location would add
     * one qualifying location to an unmet counted pre-flip alternative.
     */
    public boolean advancesPreFlipRequirementAt(
            SwccgGame game, String playerId, PhysicalCard candidate,
            PhysicalCard location) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || candidate == null || location == null
                || activeFlipLocationRules == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (!isActivePreFlipRule(rule) || isRuleSatisfied(
                    game, playerId, rule)) {
                continue;
            }
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (advancesAlternativeAt(
                        game, playerId, candidate, location, alternative)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean wouldCompletePreFlipRequirementAt(
            SwccgGame game, String playerId, PhysicalCard candidate,
            PhysicalCard location) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || candidate == null || location == null
                || activeFlipLocationRules == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (!isActivePreFlipRule(rule)) continue;
            boolean advanced = false;
            boolean allSatisfiedAfter = true;
            for (FlipLocationAlternative alternative : rule.alternatives) {
                boolean advances = advancesAlternativeAt(
                        game, playerId, candidate, location, alternative);
                if (advances) advanced = true;
                boolean satisfiedAfter = advances
                        ? alternativeSatisfiedAfterCandidate(
                                game, playerId, alternative)
                        : isFlipLocationAlternativeSatisfied(
                                game, playerId, alternative);
                if ("anyOf".equals(rule.mode) && satisfiedAfter && advances) {
                    return true;
                }
                allSatisfiedAfter &= satisfiedAfter;
            }
            if (!"anyOf".equals(rule.mode)
                    && advanced && allSatisfiedAfter) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stable cache key for typed pre-flip progress. It changes at each counted
     * location, actor, or opponent-blocker transition, not only at completion.
     */
    public String getPreFlipProgressFingerprint(
            SwccgGame game, String playerId) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || activeFlipLocationRules == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return "";
        }
        StringBuilder fingerprint = new StringBuilder();
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (!isActivePreFlipRule(rule)) continue;
            fingerprint.append(rule.id).append('=');
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (alternative == null) continue;
                fingerprint.append(countAlternativeMatches(
                        game, playerId, playerId, alternative)).append('/');
                fingerprint.append(countMatchingLocations(
                        game, playerId, alternative)).append('/');
                RuleOpponentConstraint constraint =
                        alternative.opponentConstraint;
                fingerprint.append(countOpponentConstraintMatches(
                        game, playerId, constraint)).append(';');
            }
        }
        return fingerprint.toString();
    }

    /**
     * True when this exact location owns an unmet structured pre-flip condition.
     */
    public boolean isMissingPreFlipRequirementAt(
            SwccgGame game, String playerId, PhysicalCard location) {
        if (!analyzed || isFlipped || game == null
                || playerId == null || location == null
                || activeFlipLocationRules == null) {
            return false;
        }

        GameState gameState = game.getGameState();
        if (gameState == null || game.getModifiersQuerying() == null) return false;

        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (!isActivePreFlipRule(rule)
                    || isRuleSatisfied(game, playerId, rule)) {
                continue;
            }
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (alternative == null) {
                    continue;
                }
                if (isOpponentConstraintBlockingAt(
                        game, playerId, location,
                        alternative.opponentConstraint)) {
                    return true;
                }
                if (!locationMatchesAlternative(
                        gameState, game, playerId, location, alternative)) {
                    continue;
                }
                int required = expectedCount(
                        game, playerId, alternative.count);
                int qualified = countAlternativeMatches(
                        game, playerId, playerId, alternative);
                if (qualified >= required
                        || relationSatisfiedAt(
                                game, playerId, playerId, location,
                                alternative.relation,
                                alternative.actorFilterKey,
                                alternative.includeExcludedFromBattle)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * True only for an exact typed pre-flip location whose relation is plain
     * self control. Actor-gated controlWith locations are handled separately.
     */
    public boolean isPreFlipPlainControlRequirementLocation(
            SwccgGame game, String playerId, PhysicalCard location) {
        return matchesStructuredRequirementLocation(
                game, playerId, location,
                "preFlip", "flip", "control", "self");
    }

    /**
     * True for any exact typed location named by the active pre-flip rule.
     */
    public boolean isPreFlipFlipRequirementLocation(
            SwccgGame game, String playerId, PhysicalCard location) {
        return matchesStructuredRequirementLocation(
                game, playerId, location,
                "preFlip", "flip", null, null);
    }

    public boolean hasStructuredFlipBackLocationRules() {
        if (!analyzed || activeFlipLocationRules == null) return false;
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule != null && "postFlip".equals(rule.phase)
                    && "flipBack".equals(rule.purpose)
                    && rule.alternatives != null
                    && !rule.alternatives.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Physical-card overload for exact post-flip protection. Structured rules
     * own the answer when present; parser fragments remain the legacy fallback.
     */
    public boolean isFlipBackProtectionLocation(
            PhysicalCard location, SwccgGame game, String playerId) {
        if (!analyzed || location == null) return false;
        if (hasStructuredFlipBackLocationRules()) {
            PostFlipLocationRisk risk = assessPostFlipLocationRisk(
                    game, playerId, location);
            if (risk.applies()) {
                return risk.requiresProtection();
            }
            return matchesStructuredRequirementLocation(
                    game, playerId, location,
                    "postFlip", "flipBack", null, null);
        }
        return isFlipBackProtectionLocation(location.getTitle());
    }

    /**
     * Evaluates the enabled counted back-side shapes without confusing
     * regional geography with an immediate hold requirement.
     */
    public PostFlipLocationRisk assessPostFlipLocationRisk(
            SwccgGame game, String playerId, PhysicalCard location) {
        if (!analyzed || !isFlipped || game == null || playerId == null
                || location == null || activeFlipLocationRules == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return PostFlipLocationRisk.none();
        }

        PostFlipLocationRisk supportedOutOfScope = null;
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule == null || !"postFlip".equals(rule.phase)
                    || !"flipBack".equals(rule.purpose)
                    || rule.alternatives == null) {
                continue;
            }
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (!isSupportedCountedFlipBackAlternative(alternative)) {
                    continue;
                }
                PostFlipLocationRisk risk = assessPostFlipLocationRisk(
                        game, playerId, location, alternative);
                if (risk.inScope()) return risk;
                if (supportedOutOfScope == null) {
                    supportedOutOfScope = risk;
                }
            }
        }
        return supportedOutOfScope != null
                ? supportedOutOfScope : PostFlipLocationRisk.none();
    }

    /**
     * True only when this exact mover is the last friendly presence source and
     * its departure would immediately satisfy the counted flip-back predicate.
     */
    public boolean wouldDepartureTriggerFlipBack(
            SwccgGame game, String playerId, PhysicalCard mover) {
        if (!analyzed || !isFlipped || game == null || playerId == null
                || mover == null || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            GameState gameState = game.getGameState();
            PhysicalCard location = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(gameState, mover);
            PostFlipLocationRisk risk = assessPostFlipLocationRisk(
                    game, playerId, location);
            if (!risk.applies() || !risk.inScope() || risk.flipBackNow()
                    || !isSolePresenceSourceAtLocation(
                            game, playerId, mover, location)) {
                return false;
            }

            FlipLocationAlternative alternative =
                    findSupportedPostFlipAlternative(
                            game, playerId, location);
            if (alternative == null) return false;
            String opponent = gameState.getOpponent(playerId);
            if (opponent == null) return false;
            Map<com.gempukku.swccgo.common.InactiveReason, Boolean> overrides =
                    Boolean.TRUE.equals(
                            alternative.includeExcludedFromBattle)
                            ? SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE
                            : null;
            boolean opponentOccupies =
                    game.getModifiersQuerying().occupiesLocation(
                            gameState, location, opponent, overrides);
            return opponentOccupies
                    ? risk.criticalIfOpponentGainsControl()
                    : risk.criticalIfSelfControlLost();
        } catch (Exception e) {
            LOG.debug("Objective flip-back departure assessment failed: {}",
                    e.getMessage());
            return false;
        }
    }

    private PostFlipLocationRisk assessPostFlipLocationRisk(
            SwccgGame game, String playerId, PhysicalCard location,
            FlipLocationAlternative alternative) {
        GameState gameState = game.getGameState();
        String opponent = gameState.getOpponent(playerId);
        if (opponent == null) return PostFlipLocationRisk.none();

        int selfCount = countAlternativeMatches(
                game, playerId, playerId, alternative);
        int opponentCount = countAlternativeMatches(
                game, playerId, opponent, alternative);
        boolean inScope = locationMatchesAlternative(
                gameState, game, playerId, location, alternative);
        boolean selfControlsHere = inScope && relationSatisfiedAt(
                game, playerId, playerId, location,
                alternative.relation, alternative.actorFilterKey,
                alternative.includeExcludedFromBattle);
        boolean opponentControlsHere = inScope && relationSatisfiedAt(
                game, playerId, opponent, location,
                alternative.relation, alternative.actorFilterKey,
                alternative.includeExcludedFromBattle);

        boolean flipBackNow = postFlipCountTriggered(
                game, playerId, alternative,
                opponentCount, selfCount);
        boolean selfLossTriggers = selfControlsHere
                && postFlipCountTriggered(
                        game, playerId, alternative,
                        opponentCount, Math.max(0, selfCount - 1));
        boolean opponentGainTriggers = inScope && !opponentControlsHere
                && postFlipCountTriggered(
                        game, playerId, alternative,
                        opponentCount + 1,
                        Math.max(0, selfCount
                                - (selfControlsHere ? 1 : 0)));

        int adverseSteps = 0;
        while (adverseSteps < 100
                && !postFlipCountTriggered(
                        game, playerId, alternative,
                        opponentCount + adverseSteps, selfCount)) {
            adverseSteps++;
        }
        return new PostFlipLocationRisk(
                true, inScope, flipBackNow, opponentControlsHere,
                selfLossTriggers, opponentGainTriggers,
                selfCount, opponentCount, adverseSteps);
    }

    private boolean postFlipCountTriggered(
            SwccgGame game, String playerId,
            FlipLocationAlternative alternative,
            int opponentCount, int selfCount) {
        RuleCount count = alternative.count;
        if (count.referenceController != null) {
            return compareCounts(
                    opponentCount, selfCount, count.comparator);
        }
        return compareCounts(
                opponentCount, expectedCount(game, playerId, count),
                count.comparator);
    }

    private static boolean isSupportedCountedFlipBackAlternative(
            FlipLocationAlternative alternative) {
        if (alternative == null
                || !"control".equals(alternative.relation)
                || !"opponent".equals(alternative.controller)
                || alternative.locationFilterKey == null
                || alternative.count == null) {
            return false;
        }
        return alternative.count.value != null
                || "self".equals(
                        alternative.count.referenceController);
    }

    private FlipLocationAlternative findSupportedPostFlipAlternative(
            SwccgGame game, String playerId, PhysicalCard location) {
        if (activeFlipLocationRules == null) return null;
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule == null || !"postFlip".equals(rule.phase)
                    || !"flipBack".equals(rule.purpose)
                    || rule.alternatives == null) {
                continue;
            }
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (isSupportedCountedFlipBackAlternative(alternative)
                        && locationMatchesAlternative(
                                game.getGameState(), game, playerId,
                                location, alternative)) {
                    return alternative;
                }
            }
        }
        return null;
    }

    /**
     * Counterfactual fact for moving a card from a required-control location.
     * It returns true only when the mover is a proven presence source and no
     * independent friendly presence source remains.
     */
    public boolean isSoleControlSourceAtRequiredLocation(
            SwccgGame game, String playerId, PhysicalCard mover,
            PhysicalCard location) {
        if (!isPreFlipPlainControlRequirementLocation(
                    game, playerId, location)
                || mover == null || game == null) {
            return false;
        }

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) {
                return false;
            }
            com.gempukku.swccgo.filters.Filter presenceSource =
                com.gempukku.swccgo.filters.Filters.or(
                    com.gempukku.swccgo.filters.Filters
                        .hasAbilityOrHasPermanentPilotWithAbility,
                    com.gempukku.swccgo.common.Icon.PRESENCE);
            if (!playerId.equals(mover.getOwner())
                    || mover.isUndercover()
                    || !presenceSource.accepts(
                        gameState, game.getModifiersQuerying(), mover)) {
                return false;
            }
            PhysicalCard moverLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(gameState, mover);
            if (!samePhysicalLocation(moverLocation, location)) return false;

            Collection<PhysicalCard> permanents =
                    gameState.getAllPermanentCards();
            if (permanents == null) return false;
            for (PhysicalCard card : permanents) {
                if (card == null || card == mover
                        || !playerId.equals(card.getOwner())
                        || card.isUndercover()
                        || attachedToOrAboard(card, mover)
                        || !presenceSource.accepts(
                            gameState, game.getModifiersQuerying(), card)) {
                    continue;
                }
                PhysicalCard cardLocation = game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(gameState, card);
                if (samePhysicalLocation(cardLocation, location)) return false;
            }
            return true;
        } catch (Exception e) {
            LOG.debug("Objective required-control source assessment failed: {}",
                    e.getMessage());
            return false;
        }
    }

    private boolean matchesStructuredRequirementLocation(
            SwccgGame game, String playerId, PhysicalCard location,
            String phase, String purpose, String relation,
            String controller) {
        if (!analyzed || game == null || playerId == null || location == null
                || activeFlipLocationRules == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule == null || !phase.equals(rule.phase)
                    || !purpose.equals(rule.purpose)
                    || rule.alternatives == null) {
                continue;
            }
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (alternative == null
                        || relation != null
                            && !relation.equals(alternative.relation)
                        || controller != null
                            && !controller.equals(alternative.controller)) {
                    continue;
                }
                if (locationMatchesAlternative(
                        game.getGameState(), game, playerId,
                        location, alternative)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean attachedToOrAboard(
            PhysicalCard card, PhysicalCard possibleHost) {
        PhysicalCard host = card.getAttachedTo();
        Set<PhysicalCard> seen = Collections.newSetFromMap(
                new IdentityHashMap<>());
        while (host != null && seen.add(host)) {
            if (host == possibleHost) return true;
            host = host.getAttachedTo();
        }
        return false;
    }

    private boolean isFlipLocationAlternativeSatisfied(
            SwccgGame game, String playerId,
            FlipLocationAlternative alternative) {
        if (alternative == null || alternative.locationFilterKey == null) {
            return false;
        }

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) {
                return false;
            }

            String controller = "opponent".equals(alternative.controller)
                    ? gameState.getOpponent(playerId) : playerId;
            if (controller == null) return false;

            int matchingLocations = countAlternativeMatches(
                    game, playerId, controller, alternative);
            boolean countSatisfied;
            RuleCount count = alternative.count;
            if (count != null && count.referenceController != null) {
                String referenceController =
                        "opponent".equals(count.referenceController)
                                ? gameState.getOpponent(playerId) : playerId;
                int referenceCount = referenceController == null ? 0
                        : countAlternativeMatches(
                                game, playerId, referenceController,
                                alternative);
                countSatisfied = compareCounts(
                        matchingLocations, referenceCount, count.comparator);
            } else {
                countSatisfied = compareCounts(
                        matchingLocations,
                        expectedCount(game, playerId, count),
                        count != null ? count.comparator : null);
            }
            return countSatisfied && opponentConstraintSatisfied(
                    game, playerId, alternative.opponentConstraint);
        } catch (Exception e) {
            LOG.debug("Structured objective location assessment failed: {}",
                    e.getMessage());
            return false;
        }
    }

    private int countAlternativeMatches(
            SwccgGame game, String playerId, String controller,
            FlipLocationAlternative alternative) {
        GameState gameState = game.getGameState();
        List<PhysicalCard> locations = gameState.getLocationsInOrder();
        if (locations == null) return 0;
        int matchingLocations = 0;
        for (PhysicalCard location : locations) {
            if (locationMatchesAlternative(
                    gameState, game, playerId, location, alternative)
                    && relationSatisfiedAt(
                            game, playerId, controller, location,
                            alternative.relation,
                            alternative.actorFilterKey,
                            alternative.includeExcludedFromBattle)) {
                matchingLocations++;
            }
        }
        return matchingLocations;
    }

    private static boolean isActivePreFlipRule(FlipLocationRule rule) {
        return rule != null && "preFlip".equals(rule.phase)
                && "flip".equals(rule.purpose)
                && rule.alternatives != null
                && !rule.alternatives.isEmpty();
    }

    private boolean isRuleSatisfied(
            SwccgGame game, String playerId, FlipLocationRule rule) {
        if (rule == null || rule.alternatives == null
                || rule.alternatives.isEmpty()) {
            return false;
        }
        if ("anyOf".equals(rule.mode)) {
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (isFlipLocationAlternativeSatisfied(
                        game, playerId, alternative)) {
                    return true;
                }
            }
            return false;
        }
        for (FlipLocationAlternative alternative : rule.alternatives) {
            if (!isFlipLocationAlternativeSatisfied(
                    game, playerId, alternative)) {
                return false;
            }
        }
        return true;
    }

    private int countMatchingLocations(
            SwccgGame game, String playerId,
            FlipLocationAlternative alternative) {
        com.gempukku.swccgo.filters.Filter filter =
                resolveLocationFilter(
                        alternative.locationFilterKey, playerId);
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (filter == null || locations == null) return 0;
        int count = 0;
        for (PhysicalCard location : locations) {
            if (filter.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), location)) {
                count++;
            }
        }
        return count;
    }

    private boolean advancesAlternativeAt(
            SwccgGame game, String playerId, PhysicalCard candidate,
            PhysicalCard location,
            FlipLocationAlternative alternative) {
        if (alternative == null || alternative.count == null
                || alternative.count.value == null
                || alternative.count.value <= 0
                || alternative.actorFilterKey == null
                || candidate == null || location == null
                || !locationMatchesAlternative(
                        game.getGameState(), game, playerId,
                        location, alternative)) {
            return false;
        }
        int required = expectedCount(game, playerId, alternative.count);
        int qualified = countAlternativeMatches(
                game, playerId, playerId, alternative);
        if (qualified >= required
                || relationSatisfiedAt(
                        game, playerId, playerId, location,
                        alternative.relation,
                        alternative.actorFilterKey,
                        alternative.includeExcludedFromBattle)) {
            return false;
        }

        com.gempukku.swccgo.filters.Filter actorFilter =
                resolveFilter(alternative.actorFilterKey);
        if (actorFilter == null || !actorFilter.accepts(
                game.getGameState(), game.getModifiersQuerying(),
                candidate)) {
            return false;
        }
        Map<com.gempukku.swccgo.common.InactiveReason, Boolean> overrides =
                Boolean.TRUE.equals(
                        alternative.includeExcludedFromBattle)
                        ? SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE
                        : null;
        if ("controlWith".equals(alternative.relation)
                && game.getModifiersQuerying().controlsLocation(
                        game.getGameState(), location, playerId,
                        overrides)) {
            return true;
        }
        if ("occupyWith".equals(alternative.relation)
                && game.getModifiersQuerying().occupiesLocation(
                        game.getGameState(), location, playerId,
                        overrides)) {
            return true;
        }
        SwccgCardBlueprint candidateBlueprint = candidate.getBlueprint();
        boolean providesPresence = candidateBlueprint != null
                && candidateBlueprint.hasAbilityAttribute()
                && candidateBlueprint.getAbility() != null
                && candidateBlueprint.getAbility() >= 1.0f;
        if (!providesPresence) {
            providesPresence = Filters.or(
                    Filters.hasAbilityOrHasPermanentPilotWithAbility,
                    com.gempukku.swccgo.common.Icon.PRESENCE).accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(), candidate);
        }
        if (!providesPresence) return false;

        if ("controlWith".equals(alternative.relation)) {
            String opponent =
                    game.getGameState().getOpponent(playerId);
            return opponent == null
                    || !game.getModifiersQuerying().occupiesLocation(
                            game.getGameState(), location,
                            opponent, overrides);
        }
        return "occupyWith".equals(alternative.relation);
    }

    private boolean alternativeSatisfiedAfterCandidate(
            SwccgGame game, String playerId,
            FlipLocationAlternative alternative) {
        int required = expectedCount(game, playerId, alternative.count);
        int qualified = countAlternativeMatches(
                game, playerId, playerId, alternative);
        return qualified + 1 >= required
                && opponentConstraintSatisfied(
                        game, playerId,
                        alternative.opponentConstraint);
    }

    private boolean relationSatisfiedAt(
            SwccgGame game, String playerId, String controller,
            PhysicalCard location, String relation, String actorFilterKey,
            Boolean includeExcludedFromBattle) {
        if (controller == null || location == null || relation == null) {
            return false;
        }
        GameState gameState = game.getGameState();
        Map<com.gempukku.swccgo.common.InactiveReason, Boolean> overrides =
                Boolean.TRUE.equals(includeExcludedFromBattle)
                        ? SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE : null;
        if ("control".equals(relation)) {
            return game.getModifiersQuerying().controlsLocation(
                    gameState, location, controller, overrides);
        }
        if ("occupy".equals(relation)) {
            return game.getModifiersQuerying().occupiesLocation(
                    gameState, location, controller, overrides);
        }
        if ("controlWith".equals(relation)
                || "occupyWith".equals(relation)) {
            boolean relationSatisfied = "controlWith".equals(relation)
                    ? game.getModifiersQuerying().controlsLocation(
                            gameState, location, controller, overrides)
                    : game.getModifiersQuerying().occupiesLocation(
                            gameState, location, controller, overrides);
            return relationSatisfied && hasMatchingActorAtLocation(
                    game, controller, location, actorFilterKey,
                    includeExcludedFromBattle);
        }
        if ("presentAt".equals(relation)) {
            return hasMatchingActorAtLocation(
                    game, controller, location, actorFilterKey);
        }
        return false;
    }

    private boolean opponentConstraintSatisfied(
            SwccgGame game, String playerId,
            RuleOpponentConstraint constraint) {
        if (constraint == null) return true;
        int actual = countOpponentConstraintMatches(
                game, playerId, constraint);
        return compareCounts(
                actual, expectedCount(game, playerId, constraint.count),
                constraint.count != null
                        ? constraint.count.comparator : null);
    }

    private int countOpponentConstraintMatches(
            SwccgGame game, String playerId,
            RuleOpponentConstraint constraint) {
        if (constraint == null || game == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return 0;
        }
        GameState gameState = game.getGameState();
        String opponent = gameState.getOpponent(playerId);
        com.gempukku.swccgo.filters.Filter locationFilter =
                resolveLocationFilter(
                        constraint.locationFilterKey, playerId);
        List<PhysicalCard> locations = gameState.getLocationsInOrder();
        if (opponent == null || locationFilter == null || locations == null) {
            return 0;
        }
        int actual = 0;
        for (PhysicalCard location : locations) {
            if (locationFilter.accepts(
                    gameState, game.getModifiersQuerying(), location)
                    && relationSatisfiedAt(
                            game, playerId, opponent, location,
                            constraint.relation, null,
                            constraint.includeExcludedFromBattle)) {
                actual++;
            }
        }
        return actual;
    }

    private boolean isOpponentConstraintBlockingAt(
            SwccgGame game, String playerId, PhysicalCard location,
            RuleOpponentConstraint constraint) {
        if (constraint == null || opponentConstraintSatisfied(
                game, playerId, constraint)) {
            return false;
        }
        com.gempukku.swccgo.filters.Filter locationFilter =
                resolveLocationFilter(
                        constraint.locationFilterKey, playerId);
        String opponent =
                game.getGameState().getOpponent(playerId);
        return locationFilter != null && opponent != null
                && locationFilter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), location)
                && relationSatisfiedAt(
                        game, playerId, opponent, location,
                        constraint.relation, null,
                        constraint.includeExcludedFromBattle);
    }

    private boolean locationMatchesAlternative(
            GameState gameState, SwccgGame game, String playerId,
            PhysicalCard location, FlipLocationAlternative alternative) {
        if (location == null || alternative == null) return false;
        com.gempukku.swccgo.filters.Filter filter =
                resolveLocationFilter(alternative.locationFilterKey, playerId);
        return filter != null && filter.accepts(
                gameState, game.getModifiersQuerying(), location);
    }

    private boolean hasMatchingActorAtLocation(
            SwccgGame game, String controller, PhysicalCard location,
            String actorFilterKey) {
        return hasMatchingActorAtLocation(
                game, controller, location, actorFilterKey, true);
    }

    private boolean hasMatchingActorAtLocation(
            SwccgGame game, String controller, PhysicalCard location,
            String actorFilterKey, Boolean includeExcludedFromBattle) {
        com.gempukku.swccgo.filters.Filter actorFilter =
                resolveFilter(actorFilterKey);
        if (actorFilter == null) return false;

        GameState gameState = game.getGameState();
        Collection<PhysicalCard> cards = gameState.getAllPermanentCards();
        if (cards == null) return false;
        for (PhysicalCard card : cards) {
            if (card == null || !controller.equals(card.getOwner())
                    || card.isUndercover()
                    || !actorFilter.accepts(
                            gameState, game.getModifiersQuerying(), card)) {
                continue;
            }
            if (!Boolean.TRUE.equals(includeExcludedFromBattle)
                    && gameState.isDuringBattle()
                    && game.getModifiersQuerying().isExcludedFromBattle(
                            gameState, card)) {
                continue;
            }
            PhysicalCard actorLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(gameState, card);
            if (samePhysicalLocation(actorLocation, location)) return true;
        }
        return false;
    }

    private int expectedCount(
            SwccgGame game, String playerId, RuleCount count) {
        int expected = count != null && count.value != null ? count.value : 1;
        if (count == null || count.gameTextModification == null
                || count.modifiedValue == null || game == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return expected;
        }
        try {
            PhysicalCard objective =
                    findOurObjective(game.getGameState(), playerId);
            ModifyGameTextType type = ModifyGameTextType.valueOf(
                    count.gameTextModification);
            if (objective != null
                    && game.getModifiersQuerying().hasGameTextModification(
                            game.getGameState(), objective, type)) {
                return count.modifiedValue;
            }
        } catch (Exception e) {
            LOG.debug("Objective count modifier assessment failed: {}",
                    e.getMessage());
        }
        return expected;
    }

    private static boolean compareCounts(
            int actual, int expected, String comparator) {
        String effectiveComparator =
                comparator != null ? comparator : ">=";
        return switch (effectiveComparator) {
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            case "<=" -> actual <= expected;
            case "=", "==" -> actual == expected;
            default -> actual >= expected;
        };
    }

    /**
     * Exact named typed rules override the legacy substring parser. null means
     * the rule set is not exact-title authoritative and legacy matching applies.
     */
    private Boolean exactStructuredTitleMatch(
            String titleLower, String phase, String purpose) {
        if (activeFlipLocationRules == null) return null;

        boolean foundRule = false;
        boolean matched = false;
        for (FlipLocationRule rule : activeFlipLocationRules) {
            if (rule == null || !phase.equals(rule.phase)
                    || !purpose.equals(rule.purpose)
                    || rule.alternatives == null) {
                continue;
            }
            foundRule = true;
            for (FlipLocationAlternative alternative : rule.alternatives) {
                if (alternative == null
                        || !isExactNamedLocationFilter(
                                alternative.locationFilterKey)
                        || alternative.locationFragments == null
                        || alternative.locationFragments.isEmpty()) {
                    return null;
                }
                for (String fragment : alternative.locationFragments) {
                    if (fragment != null
                            && titleLower.equals(
                                    fragment.toLowerCase(Locale.ROOT).trim())) {
                        matched = true;
                    }
                }
            }
        }
        return foundRule ? matched : null;
    }

    private static boolean isExactNamedLocationFilter(String key) {
        return "Bunker".equals(key)
                || "Bespin_system".equals(key)
                || "Theed_Palace_Throne_Room".equals(key)
                || "Naboo_system".equals(key)
                || "Galactic_Senate".equals(key)
                || "Wattos_Junkyard".equals(key);
    }

    // Loader-extension step 3b (2026-07-10): FILTER-based objective-relevance. Returns true if the location
    // matches the existing title/fragment path OR any of the active objective's flipLocationRules/
    // actorLocationRules resolved location filters (via the fail-closed resolveLocationFilter registry). This
    // is what count-refine/relation objectives whose relevant geography is NOT a simple title fragment
    // (generic battlegrounds, ownership-scoped, composite sites) need. Behavior-neutral for objectives with
    // no rules (activeFlipLocationRules/activeActorLocationRules null → same result as the title overload).
    public boolean isObjectiveRelevantLocation(PhysicalCard loc, SwccgGame game, String playerId) {
        if (!analyzed || loc == null) return false;
        if (loc.getTitle() != null && isObjectiveRelevantLocation(loc.getTitle())) return true;
        if (game == null) return false;
        GameState gs = game.getGameState();
        if (gs == null) return false;
        // V296: I Want That Map's live flip geography is any battleground. The
        // named Starkiller Base system is only a turn-0 setup choice and must
        // not make that non-battleground system a preferred deploy target.
        if (isWantThatMap) {
            try {
                if (game.getModifiersQuerying().isBattleground(gs, loc, null)) return true;
            } catch (Exception ignored) { /* fail closed */ }
        }
        if (activeFlipLocationRules != null) {
            for (FlipLocationRule rule : activeFlipLocationRules) {
                if (rule == null || rule.alternatives == null) continue;
                for (FlipLocationAlternative alt : rule.alternatives) {
                    if (alt == null || alt.locationFilterKey == null) continue;
                    com.gempukku.swccgo.filters.Filter f = resolveLocationFilter(alt.locationFilterKey, playerId);
                    if (f != null && f.accepts(gs, game.getModifiersQuerying(), loc)) return true;
                }
            }
        }
        if (activeActorLocationRules != null) {
            for (ActorLocationRule rule : activeActorLocationRules) {
                if (rule == null || rule.locationFilterKey == null) continue;
                com.gempukku.swccgo.filters.Filter f = resolveLocationFilter(rule.locationFilterKey, playerId);
                if (f != null && f.accepts(gs, game.getModifiersQuerying(), loc)) return true;
            }
        }
        return false;
    }

    public boolean isRequiredCardForFlip(String cardTitle) {
        if (!analyzed || cardTitle == null) return false;
        String titleLower = cardTitle.toLowerCase(Locale.ROOT);
        for (String required : requiredCardsOnTable) {
            if (titleLower.startsWith(required) || titleLower.equals(required)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPullableCard(String cardTitle) {
        if (!analyzed || cardTitle == null) return false;
        String titleLower = cardTitle.toLowerCase(Locale.ROOT);
        for (String pullable : pullableCards) {
            if (titleLower.startsWith(pullable) || titleLower.equals(pullable)) {
                return true;
            }
        }
        return false;
    }

    public float getLocationObjectiveBonus(String locationTitle) {
        if (!analyzed) return 0.0f;

        if (isFlipped) {
            // V22.2: POST-FLIP — protecting locations is MORE important, not less!
            // Returning 0 here was a critical bug: after flipping, Rando stopped caring
            // about objective locations and deployed elsewhere. Now we return a HIGHER
            // bonus because losing these locations means the objective flips BACK.
            if (isFlipBackProtectionLocation(locationTitle)) {
                return 200.0f;  // Higher than pre-flip 150 — defense is critical
            }
            return 0.0f;
        }

        // Pre-flip: standard objective location bonus
        if (isObjectiveRelevantLocation(locationTitle)) {
            return 150.0f;
        }
        return 0.0f;
    }

    /**
     * Game-aware objective bonus. Counted post-flip rules award defense only
     * to current recovery or one-transition risk locations.
     */
    public float getLocationObjectiveBonus(
            PhysicalCard location, SwccgGame game, String playerId) {
        if (!analyzed || location == null) return 0.0f;
        if (isFlipped && hasStructuredFlipBackLocationRules()) {
            PostFlipLocationRisk risk = assessPostFlipLocationRisk(
                    game, playerId, location);
            if (risk.applies()) {
                return risk.requiresProtection() ? 200.0f : 0.0f;
            }
        }
        if (!isFlipped && isObjectiveRelevantLocation(
                location, game, playerId)) {
            return 150.0f;
        }
        return getLocationObjectiveBonus(location.getTitle());
    }

    public Set<String> getFlipConditionLocationFragments() {
        return Collections.unmodifiableSet(flipConditionLocationFragments);
    }

    public boolean isAnalyzed() { return analyzed; }
    public boolean isFlipped() { return isFlipped; }
    public String getObjectiveTitle() { return objectiveTitle; }
    public String getObjectiveBlueprintId() { return objectiveBlueprintId; }
    public String getFlipConditionText() { return flipConditionText; }
    // V193 (Steve, 2026-07-07): the site Rando must CONTROL to enable an objective flip
    // (e.g. Endor: Bunker for Establish Secret Base (V)). null when the objective has none.
    public String getFlipCriticalControlSite() { return flipCriticalControlSite; }
    public String getFlipCriticalControlCard() { return flipCriticalControlCard; }
    // FIX 2026-07-07: the exact Bunker-gated Establish Secret Base ids (empty → detect by title).
    public Set<String> getFlipCriticalControlCardIds() { return Collections.unmodifiableSet(flipCriticalControlCardIds); }
    public boolean hasFlipGateActorRequirement() {
        return findFlipGateActorRule() != null;
    }
    public String getFlipGateActorRequirementLabel() {
        ActorLocationRule rule = findFlipGateActorRule();
        return rule != null ? rule.actorFilterKey + " at " + flipCriticalControlSite : null;
    }

    /** True only for the exact, still-open actor-gated objective control site. */
    public boolean isActiveFlipGateLocationTitle(String candidateTitle) {
        return analyzed && !isFlipped && candidateTitle != null
                && findFlipGateActorRule() != null
                && flipCriticalControlSite != null
                && flipCriticalControlSite.equalsIgnoreCase(candidateTitle.trim());
    }

    /** True when this physical card satisfies the active flip-gate actor filter. */
    public boolean matchesFlipGateActorRequirement(
            SwccgGame game, String playerId, PhysicalCard candidate) {
        if (!analyzed || game == null || playerId == null || candidate == null) {
            return false;
        }

        ActorLocationRule rule = findFlipGateActorRule();
        if (rule == null) return false;

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) return false;
            com.gempukku.swccgo.filters.Filter actorFilter =
                    resolveFilter(rule.actorFilterKey);
            return actorFilter != null && actorFilter.accepts(
                    gameState, game.getModifiersQuerying(), candidate);
        } catch (Exception e) {
            LOG.debug("V297 flip-gate actor-only match failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the cheapest typed gate actor a free enabler can upload from
     * Reserve Deck, or null when the card does not provide a proven path.
     */
    public Integer getFlipGateActorEnablerFutureDeployCost(
            SwccgGame game, String playerId, PhysicalCard enabler) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || enabler == null || enabler.getBlueprint() == null
                || findFlipGateActorRule() == null) {
            return null;
        }

        ActorLocationRule rule = findFlipGateActorRule();
        String actorToken = rule.actorFilterKey == null ? null
                : rule.actorFilterKey.replace('_', ' ')
                    .toLowerCase(Locale.ROOT);
        String gameText = enabler.getBlueprint().getGameText();
        if (actorToken == null || gameText == null) return null;
        String gameTextLower = gameText.toLowerCase(Locale.ROOT);
        if (!gameTextLower.contains("[upload]")
                || !gameTextLower.contains(actorToken)
                || gameTextLower.matches(
                    "(?s).*use\\s+\\d+\\s+force[^.]*\\[upload\\].*")) {
            return null;
        }

        try {
            GameState gameState = game.getGameState();
            if (gameState == null) return null;
            for (PhysicalCard location : gameState.getLocationsInOrder()) {
                if (isFlipGateLocation(game, playerId, location)
                        && hasFlipGateActorAtLocation(
                            game, playerId, location)) {
                    return null;
                }
            }

            Integer cheapest = null;
            List<PhysicalCard> reserve = gameState.getReserveDeck(playerId);
            if (reserve == null) return null;
            for (PhysicalCard candidate : reserve) {
                if (candidate == null || candidate.getBlueprint() == null
                        || !matchesFlipGateActorRequirement(
                            game, playerId, candidate)) {
                    continue;
                }
                Float deployCost = candidate.getBlueprint().getDeployCost();
                if (deployCost == null) continue;
                int cost = Math.max(0, deployCost.intValue());
                if (cheapest == null || cost < cheapest) cheapest = cost;
            }
            return cheapest;
        } catch (Exception e) {
            LOG.debug("Objective gate actor enabler assessment failed: {}",
                    e.getMessage());
            return null;
        }
    }

    /**
     * Narrow DPS exception for a free into-hand action that fetches the
     * still-missing typed flip-gate actor.
     */
    public boolean isFlipGateActorUploadIntoHandAction(
            SwccgGame game, String playerId, PhysicalCard sourceCard,
            String actionText) {
        if (actionText == null
                || !actionText.toLowerCase(Locale.ROOT).contains("into hand")) {
            return false;
        }
        return getFlipGateActorEnablerFutureDeployCost(
                game, playerId, sourceCard) != null;
    }

    /** True when this physical location satisfies the active flip-gate location filter. */
    public boolean isFlipGateLocation(
            SwccgGame game, String playerId, PhysicalCard location) {
        if (!analyzed || game == null || playerId == null || location == null) {
            return false;
        }

        ActorLocationRule rule = findFlipGateActorRule();
        if (rule == null) return false;

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) return false;
            com.gempukku.swccgo.filters.Filter locationFilter =
                    resolveLocationFilter(rule.locationFilterKey, playerId);
            return locationFilter != null && locationFilter.accepts(
                    gameState, game.getModifiersQuerying(), location);
        } catch (Exception e) {
            LOG.debug("V297 flip-gate location match failed: {}", e.getMessage());
            return false;
        }
    }

    /** True when the exact card and destination satisfy the active actor-at-gate rule. */
    public boolean matchesFlipGateActorRequirement(
            SwccgGame game, String playerId, PhysicalCard candidate,
            PhysicalCard destination) {
        return matchesFlipGateActorRequirement(game, playerId, candidate)
                && isFlipGateLocation(game, playerId, destination);
    }

    /** Count qualifying friendly actors present at this exact flip gate. */
    public int countFlipGateActorsAtLocation(
            SwccgGame game, String playerId, PhysicalCard destination) {
        if (!isFlipGateLocation(game, playerId, destination)) return 0;

        int actorsAtGate = 0;
        try {
            GameState gameState = game.getGameState();
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || !playerId.equals(card.getOwner())
                        || !matchesFlipGateActorRequirement(game, playerId, card)) {
                    continue;
                }
                PhysicalCard actorLocation = game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(gameState, card);
                if (samePhysicalLocation(actorLocation, destination)) actorsAtGate++;
            }
        } catch (Exception e) {
            LOG.debug("V297 flip-gate actor count failed: {}", e.getMessage());
            return 0;
        }
        return actorsAtGate;
    }

    /** True when a qualifying objective actor is already present at the exact gate. */
    public boolean hasFlipGateActorAtLocation(
            SwccgGame game, String playerId, PhysicalCard destination) {
        return countFlipGateActorsAtLocation(game, playerId, destination) > 0;
    }

    /**
     * Classifies one friendly card by what the active structured objective
     * would lose if that card left play.
     */
    public FlipGateFormationRole classifyGateFormationPieceIfRemoved(
            SwccgGame game, String playerId, PhysicalCard candidate) {
        if (!analyzed || game == null
                || playerId == null || candidate == null
                || !playerId.equals(candidate.getOwner())
                || candidate.isUndercover()
                || candidate.getBlueprint() == null) {
            return FlipGateFormationRole.NONE;
        }

        if (isFlipped) {
            return wouldDepartureTriggerFlipBack(game, playerId, candidate)
                    ? FlipGateFormationRole.LAST_FLIP_BACK_BLOCKER
                    : FlipGateFormationRole.NONE;
        }

        if (hasCountedPreFlipActorRule()) {
            return classifyCountedGatePieceIfRemoved(
                    game, playerId, candidate);
        }
        if (!hasFlipGateActorRequirement()
                || candidate.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER) {
            return FlipGateFormationRole.NONE;
        }

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) {
                return FlipGateFormationRole.NONE;
            }
            PhysicalCard gate = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(gameState, candidate);
            if (!isFlipGateLocation(game, playerId, gate)) {
                return FlipGateFormationRole.NONE;
            }

            int friendlyCharacters = 0;
            int actorsAtGate = 0;
            boolean candidateSeen = false;
            boolean candidateIsActor = matchesFlipGateActorRequirement(
                    game, playerId, candidate);
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || !playerId.equals(card.getOwner())
                        || card.isUndercover() || card.getBlueprint() == null
                        || card.getBlueprint().getCardCategory()
                            != CardCategory.CHARACTER) {
                    continue;
                }
                PhysicalCard cardLocation = game.getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(gameState, card);
                if (!samePhysicalLocation(cardLocation, gate)) continue;

                friendlyCharacters++;
                if (card == candidate) candidateSeen = true;
                if (matchesFlipGateActorRequirement(game, playerId, card)) {
                    actorsAtGate++;
                }
            }

            if (!candidateSeen || actorsAtGate < 1) {
                return FlipGateFormationRole.NONE;
            }
            if (candidateIsActor && actorsAtGate == 1) {
                return FlipGateFormationRole.LAST_REQUIRED_ACTOR;
            }

            int remainingCharacters = friendlyCharacters - 1;
            int remainingActors = actorsAtGate - (candidateIsActor ? 1 : 0);
            if (remainingActors >= 1 && remainingCharacters < 2) {
                return FlipGateFormationRole.LAST_REQUIRED_BUDDY;
            }
        } catch (Exception e) {
            LOG.debug("Typed flip-gate casualty classification failed: {}",
                    e.getMessage());
        }
        return FlipGateFormationRole.NONE;
    }

    private FlipGateFormationRole classifyCountedGatePieceIfRemoved(
            SwccgGame game, String playerId, PhysicalCard candidate) {
        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) {
                return FlipGateFormationRole.NONE;
            }
            PhysicalCard location = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(gameState, candidate);
            if (location == null) return FlipGateFormationRole.NONE;

            ruleLoop:
            for (FlipLocationRule rule : activeFlipLocationRules) {
                if (!isActivePreFlipRule(rule)) continue;
                for (FlipLocationAlternative alternative
                        : rule.alternatives) {
                    if (alternative == null
                            || alternative.actorFilterKey == null
                            || alternative.count == null
                            || alternative.count.value == null
                            || alternative.count.value <= 1
                            || !locationMatchesAlternative(
                                    gameState, game, playerId,
                                    location, alternative)) {
                        continue;
                    }
                    int required = expectedCount(
                            game, playerId, alternative.count);
                    int qualified = countAlternativeMatches(
                            game, playerId, playerId, alternative);
                    if (qualified <= 0 || qualified > required
                            || !relationSatisfiedAt(
                                    game, playerId, playerId, location,
                                    alternative.relation,
                                    alternative.actorFilterKey,
                                    alternative.includeExcludedFromBattle)) {
                        continue;
                    }

                    com.gempukku.swccgo.filters.Filter actorFilter =
                            resolveFilter(alternative.actorFilterKey);
                    boolean candidateIsActor = actorFilter != null
                            && actorFilter.accepts(
                                    gameState,
                                    game.getModifiersQuerying(), candidate);
                    if (candidateIsActor
                            && !hasOtherMatchingActorAtLocation(
                                    game, playerId, location,
                                    alternative, candidate)) {
                        if ("anyOf".equals(rule.mode)
                                && anyCountedAlternativeMeetsThresholdAfterRemoval(
                                    game, playerId, rule,
                                    candidate, location)) {
                            continue ruleLoop;
                        }
                        return FlipGateFormationRole.LAST_REQUIRED_ACTOR;
                    }

                    if (!candidateIsActor
                            && hasMatchingActorAtLocation(
                                    game, playerId, location,
                                    alternative.actorFilterKey,
                                    alternative.includeExcludedFromBattle)
                            && isSolePresenceSourceAtLocation(
                                    game, playerId, candidate, location)) {
                        if ("anyOf".equals(rule.mode)
                                && anyCountedAlternativeMeetsThresholdAfterRemoval(
                                    game, playerId, rule,
                                    candidate, location)) {
                            continue ruleLoop;
                        }
                        return FlipGateFormationRole.LAST_REQUIRED_BUDDY;
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Counted objective casualty classification failed: {}",
                    e.getMessage());
        }
        return FlipGateFormationRole.NONE;
    }

    private boolean anyCountedAlternativeMeetsThresholdAfterRemoval(
            SwccgGame game, String playerId, FlipLocationRule rule,
            PhysicalCard candidate, PhysicalCard candidateLocation) {
        if (rule == null || rule.alternatives == null) return false;
        for (FlipLocationAlternative alternative : rule.alternatives) {
            if (alternative == null || alternative.count == null
                    || alternative.count.value == null
                    || alternative.count.value <= 1
                    || alternative.actorFilterKey == null) {
                continue;
            }
            int qualified = countAlternativeMatches(
                    game, playerId, playerId, alternative);
            int required = expectedCount(
                    game, playerId, alternative.count);
            if (qualified < required) continue;
            if (!locationMatchesAlternative(
                    game.getGameState(), game, playerId,
                    candidateLocation, alternative)
                    || !relationSatisfiedAt(
                        game, playerId, playerId, candidateLocation,
                        alternative.relation,
                        alternative.actorFilterKey,
                        alternative.includeExcludedFromBattle)) {
                return true;
            }

            com.gempukku.swccgo.filters.Filter actorFilter =
                    resolveFilter(alternative.actorFilterKey);
            boolean candidateIsActor = actorFilter != null
                    && actorFilter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(), candidate);
            boolean breaksThisLocation = candidateIsActor
                    ? !hasOtherMatchingActorAtLocation(
                            game, playerId, candidateLocation,
                            alternative, candidate)
                    : isSolePresenceSourceAtLocation(
                            game, playerId, candidate,
                            candidateLocation);
            if (qualified - (breaksThisLocation ? 1 : 0) >= required) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOtherMatchingActorAtLocation(
            SwccgGame game, String playerId, PhysicalCard location,
            FlipLocationAlternative alternative, PhysicalCard excluded) {
        com.gempukku.swccgo.filters.Filter actorFilter =
                resolveFilter(alternative.actorFilterKey);
        if (actorFilter == null) return false;
        Collection<PhysicalCard> cards =
                game.getGameState().getAllPermanentCards();
        if (cards == null) return false;
        for (PhysicalCard card : cards) {
            if (card == null || card == excluded
                    || !playerId.equals(card.getOwner())
                    || card.isUndercover()
                    || !actorFilter.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(), card)) {
                continue;
            }
            PhysicalCard cardLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                            game.getGameState(), card);
            if (samePhysicalLocation(cardLocation, location)) return true;
        }
        return false;
    }

    private boolean isSolePresenceSourceAtLocation(
            SwccgGame game, String playerId, PhysicalCard candidate,
            PhysicalCard location) {
        com.gempukku.swccgo.filters.Filter presenceSource = Filters.or(
                Filters.hasAbilityOrHasPermanentPilotWithAbility,
                com.gempukku.swccgo.common.Icon.PRESENCE);
        if (!presenceSource.accepts(
                game.getGameState(), game.getModifiersQuerying(), candidate)) {
            return false;
        }
        Collection<PhysicalCard> cards =
                game.getGameState().getAllPermanentCards();
        if (cards == null) return false;
        for (PhysicalCard card : cards) {
            if (card == null || card == candidate
                    || !playerId.equals(card.getOwner())
                    || card.isUndercover()
                    || attachedToOrAboard(card, candidate)
                    || !presenceSource.accepts(
                            game.getGameState(),
                            game.getModifiersQuerying(), card)) {
                continue;
            }
            PhysicalCard cardLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                            game.getGameState(), card);
            if (samePhysicalLocation(cardLocation, location)) return false;
        }
        return true;
    }

    /**
     * V276: true only when this exact candidate can fill an active pre-flip actor-at-site gate
     * and no qualifying actor is already present there. The data lives in the objective profile;
     * deploy evaluators only consume this fact. Unknown filters or board reads fail closed.
     */
    public boolean advancesUnfilledFlipGateActorRequirement(
            SwccgGame game, String playerId, PhysicalCard candidate,
            PhysicalCard destination) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || candidate == null || destination == null) return false;

        ActorLocationRule rule = findFlipGateActorRule();
        if (rule == null) return false;

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) return false;
            if (!matchesFlipGateActorRequirement(
                            game, playerId, candidate, destination)) {
                return false;
            }
            int actorsAtGate = countFlipGateActorsAtLocation(
                    game, playerId, destination);
            int required = rule.count != null && rule.count.value != null
                    ? rule.count.value : 1;
            return actorsAtGate < Math.max(1, required);
        } catch (Exception e) {
            LOG.debug("V276 flip-gate actor assessment failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * True when a typed gate actor can legally be staged at one of the
     * objective's exact setup sites with a known site path to the gate.
     */
    public boolean stagesPreFlipActorRoute(
            SwccgGame game,
            String playerId,
            PhysicalCard actor,
            PhysicalCard destination) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || actor == null || destination == null
                || !playerId.equals(actor.getOwner())) {
            return false;
        }
        ActorLocationRule rule = findFlipGateActorRule();
        if (rule == null || rule.count == null
                || rule.count.value == null
                || rule.count.value != 1
                || !matchesFlipGateActorRequirement(
                        game, playerId, actor)
                || !matchesStartingLocation(destination)) {
            return false;
        }

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) {
                return false;
            }
            com.gempukku.swccgo.filters.Filter actorFilter =
                    resolveFilter(rule.actorFilterKey);
            if (actorFilter == null
                    || hasOtherMatchingActorOnPreFlipRoute(
                            game, playerId, actor, actorFilter)) {
                return false;
            }

            List<PhysicalCard> locations = gameState.getLocationsInOrder();
            if (locations == null) return false;
            for (PhysicalCard gate : locations) {
                if (gate == null
                        || !isFlipGateLocation(game, playerId, gate)
                        || samePhysicalLocation(gate, destination)
                        || countFlipGateActorsAtLocation(
                                game, playerId, gate) >= 1) {
                    continue;
                }
                Integer distance = game.getModifiersQuerying()
                        .getDistanceBetweenSites(
                                gameState, destination, gate);
                if (distance != null && distance > 0) return true;
            }
        } catch (Exception e) {
            LOG.debug("Objective actor staging assessment failed: {}",
                    e.getMessage());
        }
        return false;
    }

    /**
     * Parent deploy-action fact. Unlike the child destination fact, this must
     * prove that at least one route stage is currently legal and affordable.
     */
    public boolean hasLegalPreFlipActorRouteStage(
            SwccgGame game,
            String playerId,
            PhysicalCard actor) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || actor == null || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            GameState gameState = game.getGameState();
            List<PhysicalCard> locations = gameState.getLocationsInOrder();
            if (locations == null) return false;
            for (PhysicalCard location : locations) {
                if (!stagesPreFlipActorRoute(
                        game, playerId, actor, location)) {
                    continue;
                }
                if (Filters.deployableToLocation(
                        actor, Filters.sameCardId(location),
                        false, 0.0f).accepts(
                                gameState, game.getModifiersQuerying(),
                                actor)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.debug("Objective legal actor staging assessment failed: {}",
                    e.getMessage());
        }
        return false;
    }

    private boolean matchesStartingLocation(PhysicalCard destination) {
        try {
            String blueprintId = destination.getBlueprintId(true);
            if (blueprintId != null
                    && startingLocationIds.contains(blueprintId)) {
                return true;
            }
            String title = destination.getTitle();
            if (title == null) return false;
            String titleLower = title.toLowerCase(Locale.ROOT);
            for (String fragment : startingLocationFragments) {
                if (fragment != null && !fragment.isEmpty()
                        && titleLower.equals(fragment)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * True only when this exact typed gate actor moves along the engine's
     * toward relation to its still-unfilled pre-flip gate.
     */
    public boolean advancesPreFlipActorRoute(
            SwccgGame game,
            String playerId,
            PhysicalCard actor,
            PhysicalCard destination) {
        if (!analyzed || isFlipped || game == null || playerId == null
                || actor == null || destination == null
                || !playerId.equals(actor.getOwner())) {
            return false;
        }

        ActorLocationRule rule = findFlipGateActorRule();
        if (rule == null || rule.count == null
                || rule.count.value == null
                || rule.count.value != 1
                || !matchesFlipGateActorRequirement(
                        game, playerId, actor)) {
            return false;
        }

        try {
            GameState gameState = game.getGameState();
            if (gameState == null || game.getModifiersQuerying() == null) {
                return false;
            }
            PhysicalCard origin = game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(gameState, actor);
            if (origin == null || samePhysicalLocation(origin, destination)) {
                return false;
            }

            List<PhysicalCard> locations = gameState.getLocationsInOrder();
            if (locations == null) return false;
            for (PhysicalCard gate : locations) {
                if (gate == null
                        || !isFlipGateLocation(game, playerId, gate)
                        || countFlipGateActorsAtLocation(
                                game, playerId, gate) >= 1) {
                    continue;
                }
                if (Filters.toward(origin, gate).accepts(
                        gameState, game.getModifiersQuerying(),
                        destination)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.debug("Objective actor-route assessment failed: {}",
                    e.getMessage());
        }
        return false;
    }

    // NEW setup slots (2026-07-08, JSON playbook) — starting cards named by the objective.
    public Set<String> getStartingLocationIds() { return Collections.unmodifiableSet(startingLocationIds); }
    public Set<String> getStartingLocationFragments() { return Collections.unmodifiableSet(startingLocationFragments); }
    public Set<String> getStartingEffectIds() { return Collections.unmodifiableSet(startingEffectIds); }
    public Set<String> getStartingEffectFragments() { return Collections.unmodifiableSet(startingEffectFragments); }
    public Set<String> getStartingInterruptIds() { return Collections.unmodifiableSet(startingInterruptIds); }
    public Set<String> getStartingInterruptFragments() { return Collections.unmodifiableSet(startingInterruptFragments); }
    public boolean isHydratedFromJson() { return hydratedFromJson; }
    public Set<String> getRequiredCardsOnTable() { return Collections.unmodifiableSet(requiredCardsOnTable); }
    public Set<String> getPullableCards() { return Collections.unmodifiableSet(pullableCards); }
    public boolean requiresOccupy() { return requiresOccupy; }
    public boolean requiresControl() { return requiresControl; }

    /**
     * V214: Describe the objective effect of one physical deploy child without scoring it.
     *
     * This is deliberately narrower than the deploy policies. It records only facts that can
     * be proven from the active objective, the exact physical card, and the exact destination.
     * Unknown identity or unmodeled objective clauses fail closed as UNPROVEN.
     */
    public ObjectiveProgressAssessment assessDeployChild(
            GameState gameState,
            String playerId,
            PhysicalCard deployingCard,
            PhysicalCard destination) {
        if (gameState == null || playerId == null) {
            return ObjectiveProgressAssessment.unproven(
                    objectiveBlueprintId, isFlipped,
                    "Deploy objective assessment requires game state and player identity");
        }

        PhysicalCard objectiveCard = findOurObjective(gameState, playerId);
        if (objectiveCard == null) {
            return ObjectiveProgressAssessment.noObjective();
        }

        String activeBlueprintId = objectiveCard.getBlueprintId(true);
        boolean activeFlipped = objectiveCard.isFlipped();
        if (!analyzed || activeBlueprintId == null
                || !activeBlueprintId.equals(objectiveBlueprintId)) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, activeFlipped,
                    "Objective analyzer state does not match the active physical objective");
        }
        if (deployingCard == null || destination == null) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, activeFlipped,
                    "Deploy child lacks a unique physical card or destination");
        }
        if (!playerId.equals(deployingCard.getOwner())) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, activeFlipped,
                    "Deploying physical card is not owned by the objective player");
        }
        boolean knownDeployingCard = containsPhysicalCard(
                gameState.getHand(playerId), deployingCard)
                || containsPhysicalCard(
                    gameState.getCardPile(playerId, Zone.RESERVE_DECK), deployingCard)
                || containsPhysicalCard(gameState.getAllStackedCards(), deployingCard);
        if (!knownDeployingCard) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, activeFlipped,
                    "Deploying physical card is not an exact live child candidate");
        }
        if (!containsPhysicalCard(gameState.getTopLocations(), destination)) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, activeFlipped,
                    "Deploy destination is not an exact live top location");
        }

        // Endor Operations is the first fully source-verified deploy-progress pilot. Its
        // front-side flip condition is exactly two named cards on table. Other objective
        // families include counts, control, timing, or dynamic-location clauses that cannot
        // be proven by this child alone, so they remain UNPROVEN until modeled explicitly.
        if (!isEndor || activeFlipped) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, activeFlipped,
                    activeFlipped
                            ? "Post-flip deploy protection is not yet modeled"
                            : "Active objective deploy progress is not yet modeled");
        }

        Set<String> requirements = new LinkedHashSet<>(requiredCardsOnTable);
        if (requirements.isEmpty()) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, false,
                    "Endor Operations has no parsed required-card facts");
        }

        Set<String> satisfied = new LinkedHashSet<>();
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null || !playerId.equals(card.getOwner())) continue;
            Zone zone = card.getZone();
            if (zone == null || !zone.isInPlay()) continue;
            String title = card.getTitle();
            if (title == null) continue;
            String titleLower = title.toLowerCase(Locale.ROOT);
            for (String requirement : requirements) {
                if (titleLower.equals(requirement) || titleLower.startsWith(requirement)) {
                    satisfied.add(requirement);
                }
            }
        }

        Set<String> missing = new LinkedHashSet<>(requirements);
        missing.removeAll(satisfied);
        if (missing.isEmpty()) {
            return new ObjectiveProgressAssessment(
                    activeBlueprintId, false,
                    ObjectiveProgressAssessment.Outcome.NEUTRAL,
                    satisfied, missing, Set.of(),
                    "All modeled Endor Operations flip requirements are already on table");
        }

        String deployingTitle = deployingCard.getTitle();
        if (deployingTitle == null) {
            return ObjectiveProgressAssessment.unproven(
                    activeBlueprintId, false,
                    "Deploying physical card has no title");
        }
        String deployingTitleLower = deployingTitle.toLowerCase(Locale.ROOT);
        Set<String> advanced = new LinkedHashSet<>();
        for (String requirement : missing) {
            if (deployingTitleLower.equals(requirement)
                    || deployingTitleLower.startsWith(requirement)) {
                advanced.add(requirement);
            }
        }

        if (advanced.isEmpty()) {
            return new ObjectiveProgressAssessment(
                    activeBlueprintId, false,
                    ObjectiveProgressAssessment.Outcome.NEUTRAL,
                    satisfied, missing, advanced,
                    "Physical deploy child does not change a modeled Endor Operations requirement");
        }

        ObjectiveProgressAssessment.Outcome outcome = advanced.containsAll(missing)
                ? ObjectiveProgressAssessment.Outcome.COMPLETES_FLIP_NOW
                : ObjectiveProgressAssessment.Outcome.ADVANCES_MISSING_REQUIREMENT;
        return new ObjectiveProgressAssessment(
                activeBlueprintId, false, outcome,
                satisfied, missing, advanced,
                "Physical deploy child advances Endor Operations at "
                        + (destination.getTitle() != null ? destination.getTitle() : "an identified destination"));
    }

    /**
     * V29.7: Refresh just the flip status without re-analyzing the whole objective.
     * Called each evaluation to keep isFlipped current after the objective actually flips.
     */
    public void refreshFlipStatus(GameState gameState, String playerId) {
        if (!analyzed || gameState == null || playerId == null) return;
        PhysicalCard objCard = findOurObjective(gameState, playerId);
        if (objCard != null) {
            updateFlipStatus(objCard);
        }
    }

    // V25: ISB Operations accessors
    public boolean isISBOperations() { return isISBOperations; }
    public int getISBFlipAgentCount() { return isbFlipAgentCount; }
    public int getISBFlipLocationCount() { return isbFlipLocationCount; }
    public boolean isbFlipBackRequiresAgents() { return isbFlipBackNoAgents; }

    // V86/V88/V121 CONSOLIDATED accessors (2026-07-07): objective-identity, read by the Deploy +
    // CardSelection scoring branches that used to re-match the title string inline.
    //   isInvasion()  consumers: DeployEvaluator V86 (Neimoidian-pilot-aboard-capital-ship),
    //                            CardSelectionEvaluator V121 (same, deploy-target side)
    //   isMyLord()    consumers: DeployEvaluator V83/V108/V110/V88 (senator↔Galactic Senate),
    //                            CardSelectionEvaluator V88/V109 (senator deploy priority)
    //   (V99 SENATE GUARD is DELIBERATELY left ungated — it keys on Galactic Senate being on
    //    table, not on the objective, so it stays a typed-Filter check, NOT isMyLord()-gated.)
    public boolean isInvasion() { return isInvasion; }
    public boolean isMyLord() { return isMyLord; }
    public boolean isTdigwatt() { return isTdigwatt; }
    public boolean isTdigwattPreFlip() {
        return analyzed && isTdigwatt && !isFlipped;
    }
    // V186 CONSOLIDATED accessors (2026-07-07): consumers = CardSelectionEvaluator V186
    // (+400 Starkiller-SYSTEM starting-location pick, +1000 preferred starting-effect pick).
    public boolean isWantThatMap() { return isWantThatMap; }
    public java.util.Set<String> getIwtmSystemBpIds() { return iwtmSystemBpIds; }
    public String getIwtmSystemTitleFragment() { return iwtmSystemTitleFragment; }
    public String getIwtmPreferredStartingEffect() { return iwtmPreferredStartingEffect; }

    // ═══════════════════════════════════════════════════════════════════════════
    // ═══ OBJECTIVE DEPLOY DECISIONS (consolidated 2026-07-07 per Steve) ═══
    // The deploy phase CHECKS the objective brain here instead of scattering objective
    // scoring across DeployEvaluator. This method owns the objective-specific DEPLOY
    // score adjustments (V83/V110/V108/V86/V88 title-gated + V99 Senate-guard ungated).
    // DeployEvaluator calls it ONCE in the objective region and applies each ScoreNote via
    // action.addReasoning at that same spot — so additive-score ORDERING is unchanged (the
    // region has no outer control-flow escape between these rules; verified 2026-07-07).
    // NOTE: this evolves the reorg's original "SVC-INTEL owns no scores" contract — the
    // analyzer now owns objective DECISION scores, which is what the deploy evaluator reads.
    // Scores/reasons/conditions are transcribed verbatim from the old inline blocks.
    // ═══════════════════════════════════════════════════════════════════════════

    /** A single objective-driven score adjustment: apply via action.addReasoning(reason, score). */
    public static final class ScoreNote {
        public final float score;
        public final String reason;
        public ScoreNote(float score, String reason) { this.score = score; this.reason = reason; }
    }

    /** Senator detection: Keyword.SENATOR OR lore contains "senator" (only ~29/35 add the keyword). */
    private static boolean isSenatorCard(SwccgCardBlueprint bp) {
        if (bp == null) return false;
        if (bp.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)) return true;
        String lore = bp.getLore();
        return lore != null && lore.toLowerCase(Locale.ROOT).contains("senator");
    }

    // ═══ OBJECTIVE PLAYBOOK — analyzer-owned typed facts + weights (pilot 2026-07-07, Steve's ruling) ═══
    // Steve: ObjectiveAnalyzer owns objective identity, typed facts, and objective SCORING WEIGHTS;
    // evaluators consume via getActivePlaybook() at existing call sites (ordering unchanged). Facts
    // sourced from Codex batch resources/Objective_Playbook_Facts_2026-07-07.json, independently
    // source-verified 2026-07-07 (My Lord = GO). Weights REUSE existing V-tag magnitudes — no new balance.

    /** A card/objective reference matched by blueprint id OR title fragment (virtual-reprint proof). */
    public static final class NamedCardRef {
        public final java.util.Set<String> blueprintIds;
        public final java.util.List<String> titleFragments;   // lowercased
        public NamedCardRef(String[] ids, String[] titleFragments) {
            this.blueprintIds = new java.util.LinkedHashSet<>(java.util.Arrays.asList(ids));
            java.util.List<String> tf = new java.util.ArrayList<>();
            for (String t : titleFragments) tf.add(t.toLowerCase(Locale.ROOT));
            this.titleFragments = tf;
        }
        /** True if bpId is in the id set OR title contains any fragment. */
        public boolean matches(String bpId, String title) {
            if (bpId != null && blueprintIds.contains(bpId)) return true;
            if (title != null) {
                String tl = title.toLowerCase(Locale.ROOT);
                for (String f : titleFragments) if (tl.contains(f)) return true;
            }
            return false;
        }
    }

    /** Per-objective DEPLOY score magnitudes. Values REUSE existing V-tag numbers; no new balance. */
    public static final class ObjectiveWeights {
        public final float rewardKeyCharAtKeySite;    // My Lord V88: senator → Galactic Senate  +1500
        public final float penalizeKeyCharOffKeySite; // My Lord V83: senator → non-Senate        -2000
        public final float prioritizeKeyCharDeploy;   // My Lord V108: deploy a senator            +500
        public final float holdNonKeyCharNoSite;      // My Lord V110: hold non-senator, no site   -2000
        public final float deployFlipGateSite;        // Endor V193: steer one body to flip-gate site +400
        /** My Lord family (senator rules only; no flip-gate steer). */
        public ObjectiveWeights(float rewardKeyCharAtKeySite, float penalizeKeyCharOffKeySite,
                                float prioritizeKeyCharDeploy, float holdNonKeyCharNoSite) {
            this(rewardKeyCharAtKeySite, penalizeKeyCharOffKeySite,
                 prioritizeKeyCharDeploy, holdNonKeyCharNoSite, 0.0f);
        }
        /** Full: adds deployFlipGateSite (Endor V193). Unused categories pass 0 (score never applied). */
        public ObjectiveWeights(float rewardKeyCharAtKeySite, float penalizeKeyCharOffKeySite,
                                float prioritizeKeyCharDeploy, float holdNonKeyCharNoSite,
                                float deployFlipGateSite) {
            this.rewardKeyCharAtKeySite = rewardKeyCharAtKeySite;
            this.penalizeKeyCharOffKeySite = penalizeKeyCharOffKeySite;
            this.prioritizeKeyCharDeploy = prioritizeKeyCharDeploy;
            this.holdNonKeyCharNoSite = holdNonKeyCharNoSite;
            this.deployFlipGateSite = deployFlipGateSite;
        }
    }

    /** One objective's analyzer-owned profile: identity + key rules-truth Filters + weights. */
    public static final class ObjectivePlaybook {
        public final String label;
        public final NamedCardRef identity;
        public final com.gempukku.swccgo.filters.Filter keyCharacter;  // rules-truth (My Lord: Filters.senator)
        public final com.gempukku.swccgo.filters.Filter keySite;       // rules-truth (My Lord: Filters.Galactic_Senate)
        public final ObjectiveWeights weights;
        public ObjectivePlaybook(String label, NamedCardRef identity,
                com.gempukku.swccgo.filters.Filter keyCharacter,
                com.gempukku.swccgo.filters.Filter keySite, ObjectiveWeights weights) {
            this.label = label; this.identity = identity;
            this.keyCharacter = keyCharacter; this.keySite = keySite; this.weights = weights;
        }
    }

    /** MY LORD, IS THAT LEGAL? / I WILL MAKE IT LEGAL (12_179, DARK; no virtual reprint). Senators belong
     *  at Galactic Senate. Weights = the exact existing V-tag magnitudes (V88 +1500, V83 -2000, V108 +500,
     *  V110 -2000). Source-verified vs Card12_179.java 2026-07-07 (Codex batch, GO). */
    public static final ObjectivePlaybook MY_LORD_PLAYBOOK = new ObjectivePlaybook(
        "My Lord",
        new NamedCardRef(new String[]{"12_179", "12_179_BACK"},
                         new String[]{"my lord", "make it legal"}),
        com.gempukku.swccgo.filters.Filters.senator,
        com.gempukku.swccgo.filters.Filters.Galactic_Senate,
        new ObjectiveWeights(1500.0f, -2000.0f, 500.0f, -2000.0f));

    /** ENDOR OPERATIONS / IMPERIAL OUTPOST (8_167 / _BACK, DARK; virtual/Legacy reprints share the title).
     *  Flips once Ominous Rumors + Establish Secret Base are both on table. Establish Secret Base (V)
     *  (207_25) "Deploy on Bunker if you control that site", so the flip-gate is CONTROLLING Endor: Bunker.
     *  keyCharacter = biker scout (back-side Imperial Outpost drain-protect / draw-phase retrieve target,
     *  not yet scored); keySite = Bunker (the flip-gate control site). Weight = the exact existing V193
     *  magnitude (+400 one-shot Bunker steer); senator categories unused (0). Source-verified vs
     *  Card8_167.java / Card207_025.java / Card8_124.java 2026-07-07 (Codex facts batch, GO_WITH_FIXES). */
    public static final ObjectivePlaybook ENDOR_PLAYBOOK = new ObjectivePlaybook(
        "Endor Operations",
        new NamedCardRef(new String[]{"8_167", "8_167_BACK"},
                         new String[]{"endor operations", "imperial outpost"}),
        com.gempukku.swccgo.filters.Filters.biker_scout,
        com.gempukku.swccgo.filters.Filters.Bunker,
        new ObjectiveWeights(0.0f, 0.0f, 0.0f, 0.0f, 400.0f));

    /** The active objective's playbook, or null. Analyzer-owned API for evaluators/planners to consult. */
    public ObjectivePlaybook getActivePlaybook() { return activePlaybook; }

    // ═══ JSON PLAYBOOK LOADER (2026-07-08, Steve's ruling: ONE runtime data source) ═══
    // objective_playbooks.json (bundled jar resource) is the single source of objective scoring
    // inputs. Parsed ONCE (lazy, thread-safe). analyze() looks up the active objective's profile
    // by blueprint id (then title fragment) and hydrates the analyzer's existing scoring slots +
    // the new setup slots from it. HARD FALLBACK: missing/malformed file or unlisted objective →
    // registry is empty / findProfile null → the existing text parser output stands unchanged.
    // Descriptive fact fields in the JSON (sourceEvidence/notes/resolvedSample/…) are ignored;
    // Gson binds only the prescriptive fields below.
    static final class JsonCardRef {
        List<String> blueprintIds;
        List<String> titleFragments;
        String sourceVtag;
    }
    static final class JsonProfile {
        String label;
        Boolean loaderEnabled;   // hydrate ONLY when true — per-objective migration switch (verified equivalent)
        List<String> blueprintIds;
        List<String> titleFragments;
        List<String> locationFragments;
        List<String> requiredCardsOnTable;
        List<String> pullableCards;
        String flipGateSite;
        String flipGateCardName;
        List<String> flipGateCardIds;
        List<JsonCardRef> startingLocations;
        List<JsonCardRef> startingEffects;
        List<JsonCardRef> startingInterrupts;
        String keyCharacterFilter;
        String keySiteFilter;
        Map<String, Float> weights;
        // Loader extension schema. Structured location rules are consumed by
        // the analyzer and the phase-policy adapters.
        List<FlipLocationRule> flipLocationRules;
        List<ActorLocationRule> actorLocationRules;
        List<DynamicLocationRule> dynamicLocationRules;
    }
    static final class JsonRoot {
        List<JsonProfile> profiles;
    }

    // ─── Loader-extension rule DTOs (Gson-bound runtime data) ───
    static final class RuleCount {
        String comparator;
        Integer value;
        String referenceController;
        String gameTextModification;
        Integer modifiedValue;
    }
    static final class RuleOpponentConstraint {
        String relation;
        String locationFilterKey;
        Boolean includeExcludedFromBattle;
        RuleCount count;
    }
    static final class FlipLocationAlternative {
        String relation;              // control | occupy | presentAt | controlWith | occupyWith
        String controller;            // self | opponent
        String locationFilterKey;     // registry key or dynamic ref
        List<String> locationFragments;
        RuleCount count;
        String actorFilterKey;
        String requiredSide;          // LIGHT | DARK | null
        Boolean includeExcludedFromBattle;
        RuleOpponentConstraint opponentConstraint;
        String scoreRole;             // setupLocation | flipProgress | flipGate | stayFlipped
        String sourceText;            // audit only — runtime must not parse this
    }
    static final class FlipLocationRule {
        String id;
        String phase;                 // preFlip | postFlip
        String purpose;               // flip | stayFlipped | flipBack
        String mode;                  // allOf | anyOf
        List<FlipLocationAlternative> alternatives;
    }
    static final class ActorLocationRule {
        String id;
        String phase;
        String purpose;
        String relation;              // presentAt | absentFrom | controlsWith | occupiesWith | sameSiteAs
        String actorFilterKey;
        String locationFilterKey;
        String coActorFilterKey;
        String opponentActorFilterKey;
        RuleCount count;
        String scoreRole;             // keyActor | actorToSite | denyOpponentActor
        String sourceText;
    }
    static final class DynamicLocationRule {
        String id;
        String source;                // subjugatedPlanet | renegadePlanet | repSpecies | setupChoice
        String derivedLocationFilterKey;
        String matchingActorFilterKey;
        String sourceText;
    }

    private static volatile List<JsonProfile> PROFILES = null;
    private static final Object PROFILES_LOCK = new Object();

    private List<JsonProfile> profiles() {
        List<JsonProfile> p = PROFILES;
        if (p == null) {
            synchronized (PROFILES_LOCK) {
                p = PROFILES;
                if (p == null) { p = loadProfiles(); PROFILES = p; }
            }
        }
        return p;
    }

    private List<JsonProfile> loadProfiles() {
        try (java.io.InputStream in =
                 ObjectiveAnalyzer.class.getResourceAsStream("/objective_playbooks.json")) {
            if (in == null) {
                LOG.warn("[ObjectiveAnalyzer] objective_playbooks.json NOT on classpath — JSON hydration disabled, text-parser fallback active.");
                return Collections.emptyList();
            }
            JsonRoot root = new com.google.gson.Gson().fromJson(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), JsonRoot.class);
            if (root == null || root.profiles == null) return Collections.emptyList();
            LOG.warn("[ObjectiveAnalyzer] loaded {} objective playbook profile(s) from objective_playbooks.json", root.profiles.size());
            return root.profiles;
        } catch (Exception e) {
            LOG.warn("[ObjectiveAnalyzer] failed to load objective_playbooks.json ({}) — text-parser fallback active.", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Active objective's JSON profile, matched by blueprint id first then title fragment. null if none. */
    private JsonProfile findProfile(String bpId, String title) {
        List<JsonProfile> profs = profiles();
        if (profs.isEmpty()) return null;
        if (bpId != null) {
            for (JsonProfile p : profs) {
                if (p.blueprintIds != null && p.blueprintIds.contains(bpId)) return p;
            }
        }
        if (title != null) {
            String tl = title.toLowerCase(Locale.ROOT);
            for (JsonProfile p : profs) {
                if (p.titleFragments == null) continue;
                for (String f : p.titleFragments) if (f != null && !f.isEmpty() && tl.contains(f)) return p;
            }
        }
        return null;
    }

    /** Hydrate the analyzer's scoring/setup slots from a JSON profile. ADDITIVE + idempotent: never
     *  clears a slot; where the text parser already filled one, the same value is written again. */
    private void hydrateFromProfile(JsonProfile p) {
        if (p == null) return;
        if (p.locationFragments != null)
            for (String f : p.locationFragments) if (f != null && !f.isEmpty()) addLocationFragment(f.toLowerCase(Locale.ROOT));
        // AUTHORITATIVE (2026-07-08): when the profile names the flip cards, they fully specify the slot —
        // clear-then-set, replacing any parser output (e.g. Endor's junk "...are both" conjunction). This is
        // what lets the hardcoded Endor block (which did requiredCardsOnTable.clear()) be commented out.
        if (p.requiredCardsOnTable != null && !p.requiredCardsOnTable.isEmpty()) {
            requiredCardsOnTable.clear();
            for (String c : p.requiredCardsOnTable) if (c != null && !c.isEmpty()) requiredCardsOnTable.add(c.toLowerCase(Locale.ROOT));
        }
        // DEFERRED (2026-07-08): pullableCards hydration ADDS pull targets the text parser did not (e.g.
        // Endor: biker scout / bunker / endor system / landing platform), which CHANGES pull behavior.
        // Not behavior-neutral → needs per-objective boundary math before enabling. Kept off for now.
        // if (p.pullableCards != null)
        //     for (String c : p.pullableCards) if (c != null && !c.isEmpty()) pullableCards.add(c.toLowerCase(Locale.ROOT));
        if (p.flipGateSite != null && !p.flipGateSite.isEmpty() && flipCriticalControlSite == null)
            flipCriticalControlSite = p.flipGateSite.toLowerCase(Locale.ROOT);
        if (p.flipGateCardName != null && !p.flipGateCardName.isEmpty() && flipCriticalControlCard == null)
            flipCriticalControlCard = p.flipGateCardName.toLowerCase(Locale.ROOT);
        if (p.flipGateCardIds != null) flipCriticalControlCardIds.addAll(p.flipGateCardIds);
        addRefs(p.startingLocations, startingLocationIds, startingLocationFragments);
        addRefs(p.startingEffects, startingEffectIds, startingEffectFragments);
        addRefs(p.startingInterrupts, startingInterruptIds, startingInterruptFragments);
        // Loader-extension step 3a (2026-07-08): COARSE relevance from flipLocationRules — each rule
        // alternative's locationFragments feed the EXISTING +200 objective-relevance mechanism (same proven
        // lever as profile locationFragments). This makes the rule DTOs functional at the coarse-steer level.
        // The count/actor/opponent-aware SCORER (registry-filter based, step 3b) is the next increment and will
        // dominate, not replace, this coarse pass. Neutral until a profile carries flipLocationRules (none yet).
        if (p.flipLocationRules != null) {
            for (FlipLocationRule rule : p.flipLocationRules) {
                if (rule == null || rule.alternatives == null) continue;
                for (FlipLocationAlternative alt : rule.alternatives) {
                    if (alt == null || alt.locationFragments == null) continue;
                    for (String f : alt.locationFragments)
                        if (f != null && !f.isEmpty()) addLocationFragment(f.toLowerCase(Locale.ROOT));
                }
            }
        }
        // step 3b: store the rules for the filter-based relevance overload (runtime filter eval).
        activeFlipLocationRules = p.flipLocationRules;
        activeActorLocationRules = p.actorLocationRules;
        hydratedFromJson = true;
        LOG.warn("[ObjectiveAnalyzer] JSON hydrate '{}': locFrags={}, reqCards={}, flipGateSite={}, flipGateIds={}, startLoc={}, startEff={}, startInt={}",
            p.label, flipConditionLocationFragments, requiredCardsOnTable, flipCriticalControlSite,
            flipCriticalControlCardIds, startingLocationFragments, startingEffectFragments, startingInterruptFragments);
    }

    private void addRefs(List<JsonCardRef> refs, Set<String> ids, Set<String> frags) {
        if (refs == null) return;
        for (JsonCardRef r : refs) {
            if (r == null) continue;
            if (r.blueprintIds != null) ids.addAll(r.blueprintIds);
            if (r.titleFragments != null)
                for (String f : r.titleFragments) if (f != null && !f.isEmpty()) frags.add(f.toLowerCase(Locale.ROOT));
        }
    }

    private ActorLocationRule findFlipGateActorRule() {
        if (activeActorLocationRules == null) return null;
        for (ActorLocationRule rule : activeActorLocationRules) {
            if (rule == null || rule.actorFilterKey == null
                    || rule.locationFilterKey == null) continue;
            if ("preFlip".equals(rule.phase) && "flip".equals(rule.purpose)
                    && "actorToSite".equals(rule.scoreRole)) {
                return rule;
            }
        }
        return null;
    }

    private static boolean samePhysicalLocation(PhysicalCard first, PhysicalCard second) {
        if (first == null || second == null) return false;
        if (first == second) return true;
        int firstId = first.getPermanentCardId();
        int secondId = second.getPermanentCardId();
        return firstId > 0 && firstId == secondId;
    }

    // Curated string→Filter registry for JSON character/site keys. Keys are the exact
    // Filters.* constant names used by objective profiles (rules-truth, search-by-type). Expand as new
    // objectives are enabled. Unknown key → null + warn (playbook stores null; consumers null-guard).
    private com.gempukku.swccgo.filters.Filter resolveFilter(String key) {
        if (key == null || key.isEmpty()) return null;
        switch (key) {
            case "senator":          return com.gempukku.swccgo.filters.Filters.senator;
            case "Neimoidian":       return com.gempukku.swccgo.filters.Filters.Neimoidian;
            case "Amidala":          return com.gempukku.swccgo.filters.Filters.Amidala;
            case "Rebel":             return com.gempukku.swccgo.filters.Filters.Rebel;
            case "Imperial":          return com.gempukku.swccgo.filters.Filters.Imperial;
            case "Phoenix_Squadron_character":
                return com.gempukku.swccgo.filters.Filters.Phoenix_Squadron_character;
            case "Galactic_Senate":  return com.gempukku.swccgo.filters.Filters.Galactic_Senate;
            case "biker_scout":      return com.gempukku.swccgo.filters.Filters.biker_scout;
            case "Bunker":           return com.gempukku.swccgo.filters.Filters.Bunker;
            default:
                LOG.warn("[ObjectiveAnalyzer] unknown Filter key '{}' in objective_playbooks.json — playbook Filter left null.", key);
                return null;
        }
    }

    // Loader-EXTENSION location-filter registry (step 2, 2026-07-08). Maps flipLocationRules/actorLocationRules
    // locationFilterKey strings → real Filters.* (rules-truth, search-by-type; source-verified vs Filters.java +
    // Codex table Handoffs/OBJECTIVE_FILTER_REGISTRY_KEYS_2026-07-08.md). DYNAMIC (Subjugated/Renegade/Rep) and
    // STATE (blown-away/delivered/Kessel) keys are NOT here — they need their own runtime hooks (step 3b). Unknown
    // key → null + warn (FAIL-CLOSED: no guessed score, per Steve's no-fabrication rule). CONSUMED by the step-3b
    // flipLocationRules/actorLocationRules scorer; unused until then.
    private com.gempukku.swccgo.filters.Filter resolveLocationFilter(String key, String playerId) {
        if (key == null || key.isEmpty()) return null;
        switch (key) {
            // DIRECT constants
            case "Endor_location":               return com.gempukku.swccgo.filters.Filters.Endor_location;
            case "Bunker":                       return com.gempukku.swccgo.filters.Filters.Bunker;
            case "Bespin_system":                return com.gempukku.swccgo.filters.Filters.Bespin_system;
            case "Cloud_City_site":              return com.gempukku.swccgo.filters.Filters.Cloud_City_site;
            case "Cloud_City_battleground_site": return com.gempukku.swccgo.filters.Filters.Cloud_City_battleground_site;
            case "Tatooine_location":            return com.gempukku.swccgo.filters.Filters.Tatooine_location;
            case "Yavin_4_location":             return com.gempukku.swccgo.filters.Filters.Yavin_4_location;
            case "Hoth_location":                return com.gempukku.swccgo.filters.Filters.Hoth_location;
            case "Theed_Palace_Throne_Room":     return com.gempukku.swccgo.filters.Filters.Theed_Palace_Throne_Room;
            case "Naboo_system":                 return com.gempukku.swccgo.filters.Filters.Naboo_system;
            case "Dantooine_site":               return com.gempukku.swccgo.filters.Filters.Dantooine_site;
            case "Dantooine_location":           return com.gempukku.swccgo.filters.Filters.Dantooine_location;
            case "Ralltiir_site":                return com.gempukku.swccgo.filters.Filters.Ralltiir_site;
            case "Ralltiir_location":            return com.gempukku.swccgo.filters.Filters.Ralltiir_location;
            case "Lothal_location":              return com.gempukku.swccgo.filters.Filters.Lothal_location;
            case "Lothal_site":                  return com.gempukku.swccgo.filters.Filters.Lothal_site;
            case "Galactic_Senate":              return com.gempukku.swccgo.filters.Filters.Galactic_Senate;
            case "Rebel_Base_location":          return com.gempukku.swccgo.filters.Filters.Rebel_Base_location;
            case "Wattos_Junkyard":              return com.gempukku.swccgo.filters.Filters.Wattos_Junkyard;
            // ALIAS (schema spelling → runtime constant)
            case "Ahch_To_location":             return com.gempukku.swccgo.filters.Filters.AhchTo_location;
            // COMPOSITE
            case "Alderaan_location":
                return com.gempukku.swccgo.filters.Filters.partOfSystem(com.gempukku.swccgo.common.Title.Alderaan);
            case "interior_Naboo_battleground_site":
                return com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.interior_Naboo_site,
                    com.gempukku.swccgo.filters.Filters.battleground_site);
            // CONTEXT-COMPOSITE (needs player ownership)
            case "your_Hoth_location":
                return (playerId == null) ? null : com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.your(playerId),
                    com.gempukku.swccgo.filters.Filters.Hoth_location);
            default:
                LOG.warn("[ObjectiveAnalyzer] unknown/dynamic location filter key '{}' — fail-closed (no score).", key);
                return null;
        }
    }

    private static float weight(Map<String, Float> w, String key) {
        if (w == null) return 0.0f;
        Float v = w.get(key);
        return v != null ? v : 0.0f;
    }

    /** Build an ObjectivePlaybook from a JSON profile: identity + registry-resolved Filters + weights.
     *  This is how a loaderEnabled objective becomes the active playbook (analyzer = pointer to the data). */
    private ObjectivePlaybook buildPlaybookFromProfile(JsonProfile p) {
        String[] ids = (p.blueprintIds != null) ? p.blueprintIds.toArray(new String[0]) : new String[0];
        String[] frags = (p.titleFragments != null) ? p.titleFragments.toArray(new String[0]) : new String[0];
        ObjectiveWeights w = new ObjectiveWeights(
            weight(p.weights, "rewardKeyCharAtKeySite"),
            weight(p.weights, "penalizeKeyCharOffKeySite"),
            weight(p.weights, "prioritizeKeyCharDeploy"),
            weight(p.weights, "holdNonKeyCharNoSite"),
            weight(p.weights, "deployFlipGateSite"));
        return new ObjectivePlaybook(
            p.label != null ? p.label : "(json)",
            new NamedCardRef(ids, frags),
            resolveFilter(p.keyCharacterFilter),
            resolveFilter(p.keySiteFilter),
            w);
    }

    /**
     * Objective-driven DEPLOY adjustments for one candidate card. Returns the score notes the
     * deploy evaluator should apply (in place, at its objective region). Empty when nothing applies.
     * Consolidates DeployEvaluator V83/V110/V108/V86/V88 (My Lord / Invasion, objective-gated) and
     * V99 (Senate guard, DELIBERATELY ungated — keys on Galactic Senate on table, not the objective).
     */
    public java.util.List<ScoreNote> getDeployObjectiveAdjustments(
            SwccgGame game, GameState gameState, String playerId,
            PhysicalCard card, SwccgCardBlueprint blueprint, String actionText) {
        java.util.List<ScoreNote> notes = new java.util.ArrayList<>();
        // NOTE: do NOT early-return on !analyzed. The objective arms (V83/V110/V108/V86/V88) are
        // already gated by isMyLord/isInvasion (false unless analyze() ran), but V99 SENATE GUARD is
        // DELIBERATELY ungated in the original code — it must fire whenever a Galactic Senate is on
        // table even with no analyzed objective. Gating the whole method on analyzed narrowed V99
        // (caught by Codex 2026-07-07). Only the null-safety checks belong here.
        if (game == null || gameState == null || playerId == null
                || card == null || blueprint == null || actionText == null) return notes;
        String actionLower = actionText.toLowerCase(Locale.ROOT);
        boolean isCharacter = blueprint.getCardCategory() == CardCategory.CHARACTER;

        if (analyzed && !isFlipped && isCharacter
                && hasFlipGateActorRequirement()) {
            boolean stagingRoute = hasLegalPreFlipActorRouteStage(
                    game, playerId, card);
            if (stagingRoute) {
                notes.add(new ScoreNote(
                        600.0f,
                        "OBJECTIVE ACTOR STAGING: deploy '"
                                + card.getTitle()
                                + "' to stage its route to the flip gate"));
            }
        }

        // My Lord DEPLOY magnitudes now read from the ACTIVE playbook (JSON-built when loaderEnabled, else the
        // compiled MY_LORD_PLAYBOOK fallback). Inside the isMyLord arms activePlaybook is non-null (the ternary
        // in analyze() always yields MY_LORD_PLAYBOOK or the JSON build for My Lord); the ?: is defensive only.
        // Boundary-neutral: the JSON My Lord weights == the compiled statics (1500/-2000/500/-2000).
        ObjectivePlaybook mlPb = (activePlaybook != null) ? activePlaybook : MY_LORD_PLAYBOOK;

        // === V83: MY LORD — senators only at Galactic Senate (penalize senator → non-Senate) ===
        if (analyzed && isMyLord && com.gempukku.swccgo.filters.Filters.senator.accepts(
                gameState, game.getModifiersQuerying(), card)) {
            PhysicalCard mlTargetLoc = null;
            for (PhysicalCard loc : gameState.getTopLocations()) {
                if (loc == null || loc.getTitle() == null) continue;
                if (actionLower.contains(loc.getTitle().toLowerCase(Locale.ROOT))) { mlTargetLoc = loc; break; }
            }
            if (mlTargetLoc != null) {
                boolean atSenate = com.gempukku.swccgo.filters.Filters.Galactic_Senate.accepts(
                    gameState, game.getModifiersQuerying(), mlTargetLoc);
                if (!atSenate) {
                    notes.add(new ScoreNote(mlPb.weights.penalizeKeyCharOffKeySite,
                        "V83 MY LORD: senator '" + card.getTitle() + "' → '" + mlTargetLoc.getTitle()
                            + "' — must deploy to Galactic Senate (dies elsewhere)"));
                    LOG.warn("V83 MY LORD: blocking senator {} → {} (only Galactic Senate is safe)",
                        card.getTitle(), mlTargetLoc.getTitle());
                }
            }
        }

        // === V110: MY LORD — hold non-senator until a non-Senate SITE exists ===
        if (analyzed && isMyLord && isCharacter && !isSenatorCard(blueprint)) {
            boolean hasNonSenateSite = false;
            try {
                for (PhysicalCard loc : gameState.getTopLocations()) {
                    if (loc == null || loc.getBlueprint() == null) continue;
                    if (com.gempukku.swccgo.filters.Filters.Galactic_Senate.accepts(
                            gameState, game.getModifiersQuerying(), loc)) continue;
                    if (loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE) {
                        hasNonSenateSite = true; break;
                    }
                }
            } catch (Exception ignore) { /* */ }
            if (!hasNonSenateSite) {
                notes.add(new ScoreNote(mlPb.weights.holdNonKeyCharNoSite,
                    "V110 MY LORD: HOLD non-senator '" + card.getTitle()
                        + "' — no non-Senate site on table yet, would land at Senate"));
                LOG.warn("V110 MY LORD: HOLD deploy non-senator {} → -2000 (no non-Senate site)",
                    card.getTitle());
            }
        }

        // === V108: MY LORD — prioritize deploying senators from hand ===
        if (analyzed && isMyLord && isCharacter && isSenatorCard(blueprint)) {
            notes.add(new ScoreNote(mlPb.weights.prioritizeKeyCharDeploy,
                "V108 MY LORD: senator '" + card.getTitle() + "' in hand — prioritize deploy (flip target)"));
            LOG.warn("V108 MY LORD: BOOST deploy senator {} → +500", card.getTitle());
        }

        // === V86: INVASION — Neimoidian pilots aboard capital ship ===
        if (analyzed && isInvasion
                && com.gempukku.swccgo.filters.Filters.Neimoidian.accepts(
                    gameState, game.getModifiersQuerying(), card)
                && com.gempukku.swccgo.filters.Filters.pilot.accepts(
                    gameState, game.getModifiersQuerying(), card)) {
            PhysicalCard friendlyCapital = null;
            for (PhysicalCard pCard : gameState.getAllPermanentCards()) {
                if (pCard == null) continue;
                if (!playerId.equals(pCard.getOwner())) continue;
                if (com.gempukku.swccgo.filters.Filters.capital_starship.accepts(
                        gameState, game.getModifiersQuerying(), pCard)) { friendlyCapital = pCard; break; }
            }
            if (friendlyCapital != null) {
                String capitalTitleLower = friendlyCapital.getTitle() != null
                    ? friendlyCapital.getTitle().toLowerCase(Locale.ROOT) : "";
                boolean targetExplicit = actionLower.contains("aboard")
                    || actionLower.contains(" to ") || actionLower.contains(" on ");
                if (targetExplicit) {
                    boolean aboardCapital = !capitalTitleLower.isEmpty()
                        && actionLower.contains(capitalTitleLower);
                    if (!aboardCapital) {
                        notes.add(new ScoreNote(-1500.0f,
                            "V86 INVASION: Neimoidian pilot '" + card.getTitle()
                                + "' must deploy aboard friendly capital ship '"
                                + friendlyCapital.getTitle() + "' (vulnerable on ground sites)"));
                        LOG.warn("V86 INVASION: blocking Neimoidian pilot {} (target not aboard {}) → -1500",
                            card.getTitle(), friendlyCapital.getTitle());
                    } else {
                        notes.add(new ScoreNote(300.0f,
                            "V86 INVASION: Neimoidian pilot '" + card.getTitle()
                                + "' deploying aboard capital ship — correct placement!"));
                        LOG.info("V86 INVASION: Neimoidian pilot {} aboard {} → +300",
                            card.getTitle(), friendlyCapital.getTitle());
                    }
                }
            }
        }

        // === V88: MY LORD — senator → Galactic Senate bonus ===
        if (analyzed && isMyLord && com.gempukku.swccgo.filters.Filters.senator.accepts(
                gameState, game.getModifiersQuerying(), card)
                && actionLower.contains("galactic senate")) {
            notes.add(new ScoreNote(mlPb.weights.rewardKeyCharAtKeySite,
                "V88 MY LORD: senator '" + card.getTitle()
                    + "' → Galactic Senate (flip condition + weapon destiny -6 protection)"));
            LOG.warn("V88 MY LORD: BOOST senator {} → Galactic Senate → +1500", card.getTitle());
        }

        // === V99: NON-SENATOR AT GALACTIC SENATE BLOCK (DELIBERATELY ungated — keys on Senate on table) ===
        if (isCharacter && !isSenatorCard(blueprint)) {
            PhysicalCard v99Senate = null;
            for (PhysicalCard loc : gameState.getTopLocations()) {
                if (loc == null) continue;
                if (com.gempukku.swccgo.filters.Filters.Galactic_Senate.accepts(
                        gameState, game.getModifiersQuerying(), loc)) { v99Senate = loc; break; }
            }
            if (v99Senate != null && v99Senate.getTitle() != null
                    && actionLower.contains(v99Senate.getTitle().toLowerCase(Locale.ROOT))) {
                float friendlySenatorPower = 0f;
                for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                    if (pc == null) continue;
                    if (!playerId.equals(pc.getOwner())) continue;
                    PhysicalCard pcLoc = null;
                    try {
                        pcLoc = game.getModifiersQuerying().getLocationThatCardIsAt(gameState, pc);
                    } catch (Exception ignore) { /* */ }
                    if (pcLoc != v99Senate) continue;
                    if (pc.getBlueprint() == null) continue;
                    if (!isSenatorCard(pc.getBlueprint())) continue;
                    Float p = pc.getBlueprint().getPower();
                    if (p != null) friendlySenatorPower += p;
                }
                String v99Opp = game.getOpponent(playerId);
                float v99OpponentPower = (v99Opp != null)
                    ? game.getModifiersQuerying().getTotalPowerAtLocation(
                        gameState, v99Senate, v99Opp, false, false)
                    : 0f;
                if (v99OpponentPower <= friendlySenatorPower) {
                    notes.add(new ScoreNote(-1500.0f, String.format(
                        "V99 SENATE GUARD: non-senator '%s' → Galactic Senate"
                            + " (opp %.0f <= my senator %.0f) — wasted, deploy elsewhere",
                        card.getTitle(), v99OpponentPower, friendlySenatorPower)));
                    LOG.warn("V99 SENATE GUARD: BLOCK non-senator {} → Galactic Senate"
                            + " (opp={} my-senators={}) -1500",
                        card.getTitle(), (int) v99OpponentPower, (int) friendlySenatorPower);
                } else {
                    LOG.info("V99 SENATE GUARD: ALLOW non-senator {} → Galactic Senate"
                            + " (opp={} > my-senators={}) — defensive reinforcement",
                        card.getTitle(), (int) v99OpponentPower, (int) friendlySenatorPower);
                }
            }
        }

        return notes;
    }

    // V25: Hunt Down V accessors
    public boolean isHuntDownV() { return isHuntDownV; }
    public boolean isShieldWillBeDown() { return isShieldWillBeDown; }
    public boolean huntDownNeedsVader() { return huntDownNeedsVader; }
    public boolean huntDownFlipBackNoVader() { return huntDownFlipBackNoVader; }

    /**
     * V67ak (Steve, 2026-05-07): UNIVERSAL KEY-CHARACTER TOKEN EXTRACTOR.
     *
     * <p>Extracts strategy-relevant character/persona tokens from:
     * <ul>
     * <li>Active objective's full game text (not just flip condition)</li>
     * <li>Any Epic Event cards on Rando's side of table (helper cards for objective)</li>
     * <li>Any persistent Effect cards on Rando's side of table</li>
     * </ul>
     *
     * <p>Tokens are lowercased capitalized phrases matching persona-name-like patterns,
     * with generic words filtered out. Use the result to bonus deploy/pull actions for
     * cards whose title contains a token (e.g. Hunt Down V's text mentions "Vader" → any
     * Vader card in hand or pullable from reserve gets a deploy priority bonus).
     *
     * <p>Avoids hardcoding character lists per deck — one universal scan, applicable to
     * any objective that names characters in flip conditions or game text.
     */
    public Set<String> getStrategyCharacterTokens(SwccgGame game, String playerId) {
        Set<String> tokens = new HashSet<>();
        if (game == null || playerId == null) return tokens;
        GameState gs = game.getGameState();
        if (gs == null) return tokens;

        // Pattern: capitalized words / phrases (proper-noun-ish), 3-30 chars, not generic.
        // E.g. "Vader" (one word), "Lord Sidious" (two), "General Grievous" (two).
        Pattern personaPattern = Pattern.compile(
            "\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2})\\b");

        // Source 1: active objective's full game text
        try {
            PhysicalCard obj = findOurObjective(gs, playerId);
            if (obj != null && obj.getBlueprint() != null) {
                String gt = obj.getBlueprint().getGameText();
                if (gt != null) extractTokensInto(gt, personaPattern, tokens);
            }
        } catch (Exception e) { /* ignore */ }

        // Source 2: Epic Event cards + Effects on Rando's side of table
        try {
            for (PhysicalCard pc : gs.getAllPermanentCards()) {
                if (pc == null || pc.getBlueprint() == null) continue;
                if (!playerId.equals(pc.getOwner())) continue;
                Zone z = pc.getZone();
                if (z == null || !z.isInPlay()) continue;
                CardCategory cat = pc.getBlueprint().getCardCategory();
                // Epic events (Fallen Order, etc.) and persistent Effects
                if (cat == CardCategory.EPIC_EVENT || cat == CardCategory.EFFECT) {
                    String gt = pc.getBlueprint().getGameText();
                    if (gt != null) extractTokensInto(gt, personaPattern, tokens);
                }
            }
        } catch (Exception e) { /* ignore */ }

        return tokens;
    }

    private void extractTokensInto(String text, Pattern personaPattern, Set<String> out) {
        Matcher m = personaPattern.matcher(text);
        while (m.find()) {
            String tok = m.group(1).trim();
            // Reject generic / structural words even in capitalized form.
            String lower = tok.toLowerCase(Locale.ROOT);
            if (lower.length() < 4) continue;
            if (isGenericWord(lower)) continue;
            // Skip pure-grammar capitals like sentence starts:
            // "Deploy", "While", "If", "Once", "May", "Your", "When", "Until"
            if (lower.equals("deploy") || lower.equals("while") || lower.equals("if")
                    || lower.equals("once") || lower.equals("may") || lower.equals("your")
                    || lower.equals("when") || lower.equals("until") || lower.equals("flip")
                    || lower.equals("during") || lower.equals("after") || lower.equals("for")
                    || lower.equals("from") || lower.equals("here") || lower.equals("then")
                    || lower.equals("this") || lower.equals("that") || lower.equals("each")
                    || lower.equals("force") || lower.equals("battle") || lower.equals("with")
                    || lower.equals("just") || lower.equals("must") || lower.equals("does")
                    || lower.equals("you") || lower.equals("opponent") || lower.equals("opponents")
                    || lower.equals("destiny") || lower.equals("starts") || lower.equals("ends")
                    || lower.equals("turn") || lower.equals("character") || lower.equals("characters")
                    || lower.equals("location") || lower.equals("locations")
                    || lower.equals("permanent") || lower.equals("immune") || lower.equals("attrition")
                    || lower.equals("immediately") || lower.equals("simultaneously")
                    || lower.equals("episode") || lower.equals("reserve") || lower.equals("hand")
                    || lower.equals("table") || lower.equals("control") || lower.equals("occupy")
                    || lower.equals("deploy as if from hand")) continue;
            out.add(lower);
        }
    }

    /**
     * V67ak: Convenience — does the candidate card's title contain ANY of the strategy
     * tokens extracted from objective + epic events? Used by DeployEvaluator and
     * ActionTextEvaluator to give key characters a strong deploy priority.
     */
    public boolean isStrategyKeyCharacter(SwccgGame game, String playerId, String cardTitle) {
        if (cardTitle == null) return false;
        String tl = cardTitle.toLowerCase(Locale.ROOT);
        for (String tok : getStrategyCharacterTokens(game, playerId)) {
            if (tl.contains(tok)) return true;
        }
        return false;
    }

    /**
     * V25: Check if Vader is currently on table for a player.
     */
    public boolean isVaderOnTable(GameState gameState, String playerId) {
        if (gameState == null || playerId == null) return false;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) continue;
            if (!playerId.equals(card.getOwner())) continue;
            Zone zone = card.getZone();
            if (zone == null || !zone.isInPlay()) continue;
            String title = card.getTitle();
            if (title != null && title.toLowerCase(Locale.ROOT).contains("vader")
                && card.getBlueprint() != null
                && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                return true;
            }
        }
        return false;
    }

    /**
     * V25: Check if a character card is Vader (any version).
     */
    public static boolean isVaderCard(SwccgCardBlueprint bp) {
        if (bp == null) return false;
        if (bp.getCardCategory() != CardCategory.CHARACTER) return false;
        String title = bp.getTitle();
        return title != null && title.toLowerCase(Locale.ROOT).contains("vader");
    }

    /**
     * V25: Check if a character is an ISB agent based on lore.
     * ISB Operations makes characters with 'ISB', 'Rebel', or 'Rebellion' in lore into ISB agents.
     */
    public static boolean isISBAgentByLore(PhysicalCard card) {
        if (card == null) return false;
        SwccgCardBlueprint bp = card.getBlueprint();
        if (bp == null || bp.getCardCategory() != CardCategory.CHARACTER) return false;
        String lore = bp.getLore();
        if (lore == null) return false;
        String loreLower = lore.toLowerCase(Locale.ROOT);
        return loreLower.contains("isb") || loreLower.contains("rebel") || loreLower.contains("rebellion");
    }

    /**
     * V25: Count ISB agents currently on table for a player.
     */
    public int countISBAgentsOnTable(GameState gameState, String playerId) {
        if (gameState == null || playerId == null) return 0;
        int count = 0;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) continue;
            if (!playerId.equals(card.getOwner())) continue;
            Zone zone = card.getZone();
            if (zone == null || !zone.isInPlay()) continue;
            if (isISBAgentByLore(card)) {
                count++;
            }
        }
        return count;
    }

    public void reset() {
        flipConditionLocations.clear();
        flipConditionLocationFragments.clear();
        requiredCardsOnTable.clear();
        pullableCards.clear();
        flipBackLocations.clear();
        flipBackLocationFragments.clear();
        flipBackConditionText = null;
        flipBackRequiresOccupy = false;
        flipBackRequiresControl = false;
        flipConditionText = null;
        // V193 (Steve, 2026-07-07): clear the flip-gate control site with the rest.
        flipCriticalControlSite = null;
        flipCriticalControlCard = null;
        flipCriticalControlCardIds.clear();
        // NEW setup slots (2026-07-08, JSON playbook): clear with the rest of the analysis.
        startingLocationIds.clear();
        startingLocationFragments.clear();
        startingEffectIds.clear();
        startingEffectFragments.clear();
        startingInterruptIds.clear();
        startingInterruptFragments.clear();
        hydratedFromJson = false;
        activeFlipLocationRules = null;
        activeActorLocationRules = null;
        objectiveTitle = null;
        objectiveBlueprintId = null;
        // V29 UPDATED 2026-07-06: clear stored game text with the rest of the analysis
        objectiveGameText = null;
        analyzed = false;
        isFlipped = false;
        requiresOccupy = false;
        requiresControl = false;
        // V86/V88/V121 CONSOLIDATED: objective-identity flags (mirror objectiveTitle's lifecycle)
        isInvasion = false;
        isMyLord = false;
        isEndor = false;
        isTdigwatt = false;
        activePlaybook = null;
        // V186 CONSOLIDATED: I Want That Map identity + typed steer data
        isWantThatMap = false;
        iwtmSystemBpIds.clear();
        iwtmSystemTitleFragment = null;
        iwtmPreferredStartingEffect = null;
        // V25: ISB Operations
        isISBOperations = false;
        isbFlipAgentCount = 0;
        isbFlipLocationCount = 0;
        isbFlipBackNoAgents = false;
        // V25: Hunt Down V
        isHuntDownV = false;
        huntDownNeedsVader = false;
        huntDownFlipBackNoVader = false;
        // V160: Shield Will Be Down In Moments
        isShieldWillBeDown = false;
    }

    /**
     * V25: Convert English number words to integers (for parsing "four ISB agents").
     */
    private static int wordToNumber(String word) {
        if (word == null) return 0;
        switch (word.toLowerCase(Locale.ROOT)) {
            case "one": case "1": return 1;
            case "two": case "2": return 2;
            case "three": case "3": return 3;
            case "four": case "4": return 4;
            case "five": case "5": return 5;
            case "six": case "6": return 6;
            case "seven": case "7": return 7;
            case "eight": case "8": return 8;
            default:
                try { return Integer.parseInt(word); }
                catch (NumberFormatException e) { return 0; }
        }
    }

    private PhysicalCard findOurObjective(GameState gameState, String playerId) {
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) continue;
            if (!playerId.equals(card.getOwner())) continue;

            Zone zone = card.getZone();
            if (zone == null || !zone.isInPlay()) continue;

            SwccgCardBlueprint bp = card.getBlueprint();
            if (bp != null && bp.getCardCategory() == CardCategory.OBJECTIVE) {
                return card;
            }
        }
        return null;
    }

    private static boolean containsPhysicalCard(
            Iterable<PhysicalCard> cards, PhysicalCard target) {
        if (cards == null || target == null) return false;
        for (PhysicalCard card : cards) {
            if (card == target) return true;
        }
        return false;
    }

    private void updateFlipStatus(PhysicalCard objectiveCard) {
        try {
            this.isFlipped = objectiveCard.isFlipped();
            LOG.debug("[ObjectiveAnalyzer] Objective flipped = {}", isFlipped);
        } catch (Exception e) {
            LOG.debug("[ObjectiveAnalyzer] Could not determine flip status: {}", e.getMessage());
        }
    }

    private void parseGameText(String gameText, String backGameText) {
        flipConditionLocations.clear();
        flipConditionLocationFragments.clear();
        requiredCardsOnTable.clear();
        pullableCards.clear();
        flipBackLocations.clear();
        flipBackLocationFragments.clear();
        flipBackConditionText = null;
        flipBackRequiresOccupy = false;
        flipBackRequiresControl = false;
        // V193 (Steve, 2026-07-07): reset the flip-gate control site before re-parsing.
        flipCriticalControlSite = null;
        flipCriticalControlCard = null;

        parseFlipCondition(gameText);
        parsePullableCards(gameText);
        parseLocationReferences(gameText);
        parseBackSideText(backGameText);
    }

    /**
     * V21: Scan full objective text for planet/location references.
     * Catches things like "Cloud City battleground site" that appear
     * outside the flip condition but indicate where the deck operates.
     */
    private void parseLocationReferences(String gameText) {
        // Pattern: "[Planet Name] battleground/site/location/system"
        // or "[Planet Name]: [Site Name]" format
        Pattern planetRefPattern = Pattern.compile(
            "([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+(?:battleground|site|location|system)",
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = planetRefPattern.matcher(gameText);
        while (matcher.find()) {
            String planet = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (!isGenericWord(planet) && planet.length() >= 3) {
                if (!flipConditionLocationFragments.contains(planet)) {
                    addLocationFragment(planet);
                    LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Location ref from game text: '{}'", planet);
                }
            }
        }

        // Also catch "Site: Name" patterns like "Cloud City: Dining Room"
        Pattern colonPattern = Pattern.compile(
            "([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*):");
        Matcher colonMatcher = colonPattern.matcher(gameText);
        while (colonMatcher.find()) {
            String prefix = colonMatcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (!isGenericWord(prefix) && prefix.length() >= 3) {
                if (!flipConditionLocationFragments.contains(prefix)) {
                    addLocationFragment(prefix);
                    LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Colon-prefix location: '{}'", prefix);
                }
            }
        }
    }

    private void parseFlipCondition(String gameText) {
        Matcher flipMatcher = FLIP_PATTERN.matcher(gameText);
        if (!flipMatcher.find()) {
            LOG.warn("[ObjectiveAnalyzer] No 'Flip this card if' found in game text");
            return;
        }

        flipConditionText = flipMatcher.group(1).trim();
        LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Flip condition: '{}'", flipConditionText);
        String condLower = flipConditionText.toLowerCase(Locale.ROOT);

        if (condLower.contains("occupy")) {
            requiresOccupy = true;
            extractLocationsFromCondition(flipConditionText, OCCUPY_LOCATION_PATTERN);
        }
        if (condLower.contains("control")) {
            requiresControl = true;
            extractLocationsFromCondition(flipConditionText, CONTROL_LOCATION_PATTERN);
        }

        Matcher onTableMatcher = ON_TABLE_PATTERN.matcher(flipConditionText);
        while (onTableMatcher.find()) {
            String cardName = cleanCardName(onTableMatcher.group(1));
            if (cardName != null && !cardName.isEmpty()) {
                requiredCardsOnTable.add(cardName.toLowerCase(Locale.ROOT));
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Required on table: '{}'", cardName);
            }
        }

        // V25: ISB Operations — detect "four ISB agents are on table" or
        // "ISB agents control at least two Rebel Base locations"
        if (condLower.contains("isb agent")) {
            isISBOperations = true;
            LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] V25: ISB Operations objective detected!");

            // Parse "four ISB agents are on table" → need 4 agents
            Pattern isbCountPattern = Pattern.compile(
                "(\\w+)\\s+ISB\\s+agents?\\s+(?:are\\s+)?on\\s+table", Pattern.CASE_INSENSITIVE);
            Matcher isbCountMatcher = isbCountPattern.matcher(flipConditionText);
            if (isbCountMatcher.find()) {
                isbFlipAgentCount = wordToNumber(isbCountMatcher.group(1));
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] V25: Need {} ISB agents on table to flip", isbFlipAgentCount);
            }

            // Parse "ISB agents control at least two Rebel Base locations"
            Pattern isbLocPattern = Pattern.compile(
                "ISB\\s+agents?\\s+control\\s+(?:at\\s+least\\s+)?(\\w+)\\s+(.+?)\\s+locations?",
                Pattern.CASE_INSENSITIVE);
            Matcher isbLocMatcher = isbLocPattern.matcher(flipConditionText);
            if (isbLocMatcher.find()) {
                isbFlipLocationCount = wordToNumber(isbLocMatcher.group(1));
                String locType = isbLocMatcher.group(2).trim().toLowerCase(Locale.ROOT);
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] V25: Need ISB agents controlling {} {} locations",
                    isbFlipLocationCount, locType);
                // Add "rebel base" as a location fragment so we prioritize those locations
                if (locType.contains("rebel base")) {
                    addLocationFragment("rebel base");
                }
            }
        }

        // V25: Hunt Down V — detect "Vader is at a battleground site" flip condition
        // The flip condition text is: "Vader is at a battleground site unless Luke, a Jedi, or a Padawan at a battleground site"
        if (condLower.contains("vader") && condLower.contains("battleground")) {
            isHuntDownV = true;
            huntDownNeedsVader = true;
            LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] V25: Hunt Down V objective detected! Vader MUST be deployed to flip.");
        }

        // V160 (Steve, 2026-05-29): Shield Will Be Down In Moments \u2014 Hoth invasion deck.
        // Flip condition: Main Power Generators "blown away." The path: deploy Target The
        // Main Generator (Epic Event) on Ice Plains, position AT-AT Cannon at 3rd Marker or
        // lower, fire each deploy phase until destiny > 8 -> MPG blown away -> objective
        // flips to Imperial Troops Have Entered The Base! Without this recognition Rando
        // ignored Target The Main Generator entirely (last replay).
        if (objectiveTitle != null
                && objectiveTitle.toLowerCase(Locale.ROOT).contains("shield will be down")) {
            isShieldWillBeDown = true;
            // Target The Main Generator is the engine card; mark as required-for-flip.
            requiredCardsOnTable.add("target the main generator");
            // Push the supporting deploys (the epic event itself, AT-AT Cannon, and the
            // start-effect Prepare For A Surface Attack) via the pullable list.
            pullableCards.add("target the main generator");
            pullableCards.add("at-at cannon");
            pullableCards.add("prepare for a surface attack");
            // Objective-relevant locations: the four marker sites (Hoth: Defensive Perimeter),
            // Ice Plains (Target The Main Generator's deploy target), and Main Power
            // Generators (the flip target).
            addLocationFragment("hoth: defensive perimeter");
            addLocationFragment("hoth: ice plains");
            addLocationFragment("hoth: main power generators");
            LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] V160: Shield Will Be Down In Moments detected \u2014 pushing Target The Main Generator and Hoth invasion locations.");
        }

        // V186 (Steve, 2026-06-23): I Want That Map / And Now You'll Give It To Me (208_57).
        // The flip text ("First Order characters control two battlegrounds") names no
        // location. The physical-card relevance path above handles those battlegrounds.
        // Two named picks remain for this deck's turn-0 setup:
        //   - Starkiller Base SYSTEM (208_51) is the "any other [Episode VII] location" the
        //     objective deploys. It has NO battleground icon (a battleground heuristic would
        //     miss it), but its once-per-turn [download] fetches Starkiller Base battleground
        //     sites, feeding the 2-battleground flip. CardSelectionEvaluator selects it by
        //     exact blueprint/temp ID during setup, so it must not become live deploy geography.
        //   - The First Order Was Just The Beginning: marked required/pullable so it is
        //     protected + objective-critical. The WINNING starting-effect preference lives in
        //     CardSelectionEvaluator (V186).
        if (objectiveTitle != null
                && objectiveTitle.toLowerCase(Locale.ROOT).contains("i want that map")) {
            requiredCardsOnTable.add("the first order was just the beginning");
            pullableCards.add("the first order was just the beginning");
            LOG.warn("[ObjectiveAnalyzer] V296: I Want That Map detected - battlegrounds are live objective geography; Starkiller Base remains setup-only.");
        }

        // V193 (Steve, 2026-07-07): Endor Operations (dark, Card8_167) — Establish Secret Base
        // flip gate. Objective text: "Deploy Endor system, Bunker and Landing Platform. ...
        // Flip this card if Ominous Rumors and Establish Secret Base are both on table. Place
        // out of play if an Endor location is 'blown away'." Two generic-parser bugs left Rando
        // blind to the flip path (diagnosed replay qgdridfo166f27r3): (1) parseLocationReferences
        // captured only garbage fragments ("deploy endor", "place out of play if an endor"), so
        // isObjectiveRelevantLocation() was FALSE for every Endor site and no Endor site ever got
        // the CharacterDeploySiteEvaluator +100 BG / +200 objective bonus; (2) ON_TABLE_PATTERN
        // grabbed the whole conjunction "ominous rumors and establish secret base are both" as a
        // SINGLE required card, so neither flip-card was recognized. Fix both for THIS objective
        // and expose the flip-gate control site. NOTE (verified against the real blueprint
        // src/.../set207/dark/Card207_025.java): the deck plays Establish Secret Base (V) =
        // "Deploy on Bunker if you control that site" — the flip gate is simply CONTROLLING
        // Endor: Bunker with ANY of Rando's cards. No biker scouts / AT-STs are involved.
        // SUPERSEDED 2026-07-08: this hardcoded Endor block is now REPLACED by the loaderEnabled Endor
        // profile in objective_playbooks.json (hydrateFromProfile sets the identical slots: locationFragment
        // "endor", requiredCardsOnTable {ominous rumors, establish secret base} via AUTHORITATIVE clear-then-set,
        // flipCriticalControlSite "endor: bunker", flipCriticalControlCard "establish secret base", and
        // flipGateCardIds {207_25,207_025,601_260}). Byte-identical (boundary-verified). Kept compiled-out
        // (if(false)) as the revert path per comment-out-superseded discipline. V193 tag preserved for grep.
        if (false /* SUPERSEDED 2026-07-08 — Endor now JSON-hydrated (loaderEnabled profile) */
                && objectiveTitle != null
                && objectiveTitle.toLowerCase(Locale.ROOT).contains("endor operations")) {
            // (a) make all Endor sites objective-relevant (Bunker, Landing Platform, Dark Forest,
            //     Endor system) so v136ObjRelevant -> +100 BG / +200 obj in CharacterDeploySiteEvaluator.
            addLocationFragment("endor");
            // (b) V21 required-cards parser fix (per feedback_update_old_rule_not_new_version this
            //     ADJUSTS the existing parser, not a new rule): drop the junk "...are both"
            //     conjunction entry and name the two real flip-cards.
            requiredCardsOnTable.clear();
            requiredCardsOnTable.add("ominous rumors");
            requiredCardsOnTable.add("establish secret base");
            // (c) expose the flip-gate: the control SITE and the CARD whose deploy needs it.
            //     Both live here (objective logic) so DeployEvaluator's V193 steer is general.
            flipCriticalControlSite = "endor: bunker";
            flipCriticalControlCard = "establish secret base";
            // FIX 2026-07-07: scope the Bunker steer to the Bunker-GATED Establish Secret Base
            // printings only. Base 8_124 deploys on Endor SYSTEM gated on controlling 3 Endor
            // sites (verified Card8_124.java) — a single Bunker body does NOT enable it, so the
            // +400 must NOT fire for it. V 207_25 (Card207_025.java: deploy on Bunker) and Legacy
            // V 601_260 (Card601_260.java: "If you control Bunker, deploy on Endor system") are
            // both gated on Bunker control → steer is correct. Hedge both id forms for 207_25
            // (same as the IWTM 208_51/208_051 hedge) since runtime blueprint-id padding varies.
            flipCriticalControlCardIds.clear();
            flipCriticalControlCardIds.add("207_25");
            flipCriticalControlCardIds.add("207_025");
            flipCriticalControlCardIds.add("601_260");
            LOG.warn("🎯 [ObjectiveAnalyzer] V193: Endor Operations detected — Endor sites objective-relevant, required=[ominous rumors, establish secret base], flip-gate: control 'Endor: Bunker' to deploy 'Establish Secret Base'.");
        }

        if (!requiresOccupy && !requiresControl && !isISBOperations && !isHuntDownV) {
            extractLocationsDirectly(flipConditionText);
        }
    }

    private void extractLocationsFromCondition(String conditionText, Pattern pattern) {
        String[] parts = conditionText.split("\\s+and\\s+");

        for (String part : parts) {
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                String locText = matcher.group(1).trim();
                addLocationFromText(locText);
            } else {
                extractLocationsDirectly(part);
            }
        }
    }

    private void extractLocationsDirectly(String text) {
        Pattern systemPattern = Pattern.compile(
            "([A-Z][\\w\\s']+?)\\s+(?:S|s)ystem", Pattern.CASE_INSENSITIVE);
        Matcher systemMatcher = systemPattern.matcher(text);
        while (systemMatcher.find()) {
            String systemName = systemMatcher.group(1).trim();
            addLocationFromText(systemName + " System");
            addLocationFragment(systemName.toLowerCase(Locale.ROOT));
        }

        Pattern sitePattern = Pattern.compile(
            "([A-Z][\\w\\s']+?:\\s*[A-Z][\\w\\s'()]+)");
        Matcher siteMatcher = sitePattern.matcher(text);
        while (siteMatcher.find()) {
            addLocationFromText(siteMatcher.group(1).trim());
        }

        Pattern planetLocPattern = Pattern.compile(
            "(?:at |related to |to )([A-Z][\\w\\s']+?)\\s+(?:locations?|sites?|battlegrounds?)",
            Pattern.CASE_INSENSITIVE);
        Matcher planetLocMatcher = planetLocPattern.matcher(text);
        while (planetLocMatcher.find()) {
            String planet = planetLocMatcher.group(1).trim();
            if (!isGenericWord(planet)) {
                addLocationFragment(planet.toLowerCase(Locale.ROOT));
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Planet location group: '{}'", planet);
            }
        }
    }

    private void parsePullableCards(String gameText) {
        String frontText = gameText;
        int backIdx = gameText.indexOf("[Back Side]");
        if (backIdx > 0) {
            frontText = gameText.substring(0, backIdx);
        }
        int backslashIdx = gameText.indexOf("\\[Back Side]");
        if (backslashIdx > 0 && backslashIdx < frontText.length()) {
            frontText = gameText.substring(0, backslashIdx);
        }

        Matcher takeMatcher = TAKE_FROM_RESERVE_PATTERN.matcher(frontText);
        while (takeMatcher.find()) {
            String cardList = takeMatcher.group(1).trim();
            String[] cards = cardList.split("\\s*(?:,|\\bor\\b)\\s*");
            for (String cardName : cards) {
                String cleaned = cleanCardName(cardName.trim());
                if (cleaned != null && !cleaned.isEmpty() && !isGenericWord(cleaned)) {
                    pullableCards.add(cleaned.toLowerCase(Locale.ROOT));
                    LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Pullable from Reserve: '{}'", cleaned);
                }
            }
        }
        // V21 ADJUSTED 2026-07-11b: same extraction for the "[upload] X, Y, or Z" icon form.
        Matcher uploadMatcher = UPLOAD_FROM_RESERVE_PATTERN.matcher(frontText);
        while (uploadMatcher.find()) {
            String cardList = uploadMatcher.group(1).trim();
            String[] cards = cardList.split("\\s*(?:,|\\bor\\b)\\s*");
            for (String cardName : cards) {
                String cleaned = cleanCardName(cardName.trim());
                if (cleaned != null && !cleaned.isEmpty() && !isGenericWord(cleaned)) {
                    pullableCards.add(cleaned.toLowerCase(Locale.ROOT));
                    LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Pullable from Reserve (upload): '{}'", cleaned);
                }
            }
        }
    }

    private void addLocationFromText(String locationText) {
        if (locationText == null || locationText.isEmpty()) return;

        String locLower = locationText.toLowerCase(Locale.ROOT);
        if (isGenericWord(locLower)) return;

        flipConditionLocations.add(locLower);
        LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Flip location (exact): '{}'", locationText);

        if (locLower.contains(":")) {
            String[] parts = locLower.split(":\\s*");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !isGenericWord(trimmed)) {
                    addLocationFragment(trimmed);
                }
            }
        } else if (locLower.endsWith(" system")) {
            String planet = locLower.replace(" system", "").trim();
            addLocationFragment(planet);
        } else {
            // Strip leading numbers (e.g. "3 Bespin locations" -> "Bespin locations")
            String cleaned = locLower.replaceFirst("^\\d+\\s+", "");
            // Strip trailing generic location words
            cleaned = cleaned.replaceAll("\\s+(?:locations?|sites?|battlegrounds?|systems?)\\s*$", "").trim();
            if (!cleaned.isEmpty() && !isGenericWord(cleaned)) {
                addLocationFragment(cleaned);
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Cleaned fragment: '{}' (from '{}')", cleaned, locLower);
            }
        }
    }

    private void addLocationFragment(String fragment) {
        if (fragment != null && !fragment.isEmpty() && fragment.length() >= 3) {
            flipConditionLocationFragments.add(fragment);
            // Cloud City is on Bespin - if one is referenced, add the other
            if (fragment.equals("bespin") && !flipConditionLocationFragments.contains("cloud city")) {
                flipConditionLocationFragments.add("cloud city");
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Auto-added 'cloud city' (Bespin planet)");
            } else if (fragment.equals("cloud city") && !flipConditionLocationFragments.contains("bespin")) {
                flipConditionLocationFragments.add("bespin");
                LOG.warn("\uD83C\uDFAF [ObjectiveAnalyzer] Auto-added 'bespin' (Cloud City planet)");
            }
        }
    }

    private String cleanCardName(String name) {
        if (name == null) return null;
        name = name.trim();
        name = name.replace("\u2022", "").trim();
        // V21 ADJUSTED 2026-07-12 (Codex m00177 blocker c): strip bracketed ICON tokens —
        // "[Special Edition] Bespin" must normalize to "Bespin" or isPullableCard("Bespin")
        // misses and the objective-critical never-lose protection stays unarmed (the exact
        // hole that let Bespin be pitched as Force-loss fodder in replay kxn8bvydcd803p2j).
        name = name.replaceAll("\\[[^\\]]*\\]", " ").replaceAll("\\s+", " ").trim();
        name = name.replaceFirst("^(?:a |an |the )(?=[A-Z])", "");
        // "one Ominous Rumors" style quantifiers survive some paths — strip a leading count word.
        name = name.replaceFirst("^(?:one|two|three) (?=[A-Z\\[])", "");
        return name;
    }

    private boolean isGenericWord(String word) {
        if (word == null) return true;
        String lower = word.toLowerCase(Locale.ROOT).trim();
        Set<String> generic = new HashSet<>(Arrays.asList(
            "you", "your", "opponent", "opponent's", "that", "this", "the",
            "a", "an", "at", "to", "from", "with", "two", "three", "four",
            "battleground", "battlegrounds", "site", "sites", "location", "locations",
            "system", "systems", "character", "characters", "card", "cards",
            "least", "more", "each", "all", "any", "where", "there",
            "if", "and", "or", "not", "no", "is", "are", "has", "have",
            "dark", "light", "side", "force"
        ));
        return generic.contains(lower) || lower.length() < 3;
    }

    /**
     * V22.2: Parse the back side of the objective card to understand flip-back conditions.
     * The back side tells us what conditions would cause the objective to flip BACK —
     * which means we lose our advantage. We need to prevent that.
     *
     * Common flip-back patterns:
     *   "Flip this card if opponent controls [locations]"
     *   "Flip this card if you do not occupy [locations]"
     *   "Place out of play if [condition]"
     */
    private void parseBackSideText(String backGameText) {
        if (backGameText == null || backGameText.trim().isEmpty()) {
            LOG.debug("[ObjectiveAnalyzer] No back-side game text available");
            return;
        }
        String backText = backGameText.trim();

        LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Back side text: {}", backText);

        // Look for flip-back conditions: "Flip this card if..."
        Matcher flipBackMatcher = FLIP_PATTERN.matcher(backText);
        if (flipBackMatcher.find()) {
            flipBackConditionText = flipBackMatcher.group(1).trim();
            LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] FLIP-BACK condition: '{}'", flipBackConditionText);

            String condLower = flipBackConditionText.toLowerCase(Locale.ROOT);

            // Parse what the opponent must do to flip us back
            // "opponent controls" = they need to control our locations
            // "you do not occupy" = we need to keep occupying
            if (condLower.contains("do not occupy") || condLower.contains("don't occupy")
                    || condLower.contains("does not occupy") || condLower.contains("doesn't occupy")) {
                flipBackRequiresOccupy = true;
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Flip-back if WE DON'T OCCUPY locations");
                extractFlipBackLocations(flipBackConditionText, OCCUPY_LOCATION_PATTERN);
            }
            if (condLower.contains("do not control") || condLower.contains("don't control")
                    || condLower.contains("does not control") || condLower.contains("doesn't control")) {
                flipBackRequiresControl = true;
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Flip-back if WE DON'T CONTROL locations");
                extractFlipBackLocations(flipBackConditionText, CONTROL_LOCATION_PATTERN);
            }
            // V25: ISB Operations back side — "no ISB agents are on table"
            if (condLower.contains("no isb agent") || condLower.contains("no isb agents")) {
                isbFlipBackNoAgents = true;
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] V25: Flips BACK if no ISB agents on table — must maintain ISB presence!");
            }

            // V25: Hunt Down V back side — "Vader not on table"
            if (condLower.contains("vader not on table") || condLower.contains("vader is not on table")) {
                huntDownFlipBackNoVader = true;
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] V25: Hunt Down V flips BACK if Vader not on table — Vader is critical!");
            }

            // Also check for "opponent controls X" which implies we must defend X
            if (condLower.contains("opponent controls") || condLower.contains("opponent occupies")) {
                flipBackRequiresControl = true;
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Flip-back if OPPONENT CONTROLS our locations");
                extractFlipBackLocations(flipBackConditionText, CONTROL_LOCATION_PATTERN);
                extractFlipBackLocations(flipBackConditionText, OCCUPY_LOCATION_PATTERN);
            }

            // If we found flip-back conditions but no specific locations,
            // assume the same locations as the front side flip conditions
            if (flipBackLocations.isEmpty() && flipBackLocationFragments.isEmpty()) {
                flipBackLocations.addAll(flipConditionLocations);
                flipBackLocationFragments.addAll(flipConditionLocationFragments);
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] No specific flip-back locations - using front-side locations as protection targets");
            }
        } else {
            LOG.warn("[ObjectiveAnalyzer] No 'Flip this card if' found on back side");
            // Even without explicit flip-back conditions, protect the same locations
            // Most objectives flip back if you lose presence at key locations
            flipBackLocations.addAll(flipConditionLocations);
            flipBackLocationFragments.addAll(flipConditionLocationFragments);
            LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Defaulting flip-back protection to front-side locations");
        }

        // Also scan back side for additional location references we might have missed
        parseBackSideLocationReferences(backText);
    }

    /**
     * Extract locations from flip-back condition text.
     */
    private void extractFlipBackLocations(String conditionText, Pattern pattern) {
        String[] parts = conditionText.split("\\s+and\\s+");
        for (String part : parts) {
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                String locText = matcher.group(1).trim();
                addFlipBackLocation(locText);
            }
        }
        // Also try direct extraction
        extractFlipBackLocationsDirectly(conditionText);
    }

    /**
     * Direct location extraction for flip-back conditions (same approach as front side).
     */
    private void extractFlipBackLocationsDirectly(String text) {
        Pattern planetLocPattern = Pattern.compile(
            "(?:at |related to |to )([A-Z][\\w\\s']+?)\\s+(?:locations?|sites?|battlegrounds?)",
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = planetLocPattern.matcher(text);
        while (matcher.find()) {
            String planet = matcher.group(1).trim();
            if (!isGenericWord(planet)) {
                String planetLower = planet.toLowerCase(Locale.ROOT);
                flipBackLocationFragments.add(planetLower);
                // Auto-link Cloud City <-> Bespin
                if (planetLower.equals("bespin") && !flipBackLocationFragments.contains("cloud city")) {
                    flipBackLocationFragments.add("cloud city");
                } else if (planetLower.equals("cloud city") && !flipBackLocationFragments.contains("bespin")) {
                    flipBackLocationFragments.add("bespin");
                }
                LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Flip-back protection planet: '{}'", planet);
            }
        }
    }

    /**
     * Scan back side text for additional location references.
     */
    private void parseBackSideLocationReferences(String backText) {
        // Look for "Cloud City" or other location planet references
        Pattern planetRefPattern = Pattern.compile(
            "([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+(?:battleground|site|location|system)",
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = planetRefPattern.matcher(backText);
        while (matcher.find()) {
            String planet = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (!isGenericWord(planet) && planet.length() >= 3) {
                if (!flipBackLocationFragments.contains(planet)) {
                    flipBackLocationFragments.add(planet);
                    // Auto-link Cloud City <-> Bespin
                    if (planet.equals("bespin") && !flipBackLocationFragments.contains("cloud city")) {
                        flipBackLocationFragments.add("cloud city");
                    } else if (planet.equals("cloud city") && !flipBackLocationFragments.contains("bespin")) {
                        flipBackLocationFragments.add("bespin");
                    }
                    LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Back-side location ref: '{}'", planet);
                }
            }
        }
    }

    private void addFlipBackLocation(String locationText) {
        if (locationText == null || locationText.isEmpty()) return;
        String locLower = locationText.toLowerCase(Locale.ROOT);
        if (isGenericWord(locLower)) return;

        flipBackLocations.add(locLower);
        LOG.warn("\uD83D\uDEE1 [ObjectiveAnalyzer] Flip-back location (exact): '{}'", locationText);

        // Extract fragments just like addLocationFromText does
        if (locLower.contains(":")) {
            String[] parts = locLower.split(":\\s*");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !isGenericWord(trimmed)) {
                    flipBackLocationFragments.add(trimmed);
                    // Auto-link Cloud City <-> Bespin
                    if (trimmed.equals("bespin") && !flipBackLocationFragments.contains("cloud city")) {
                        flipBackLocationFragments.add("cloud city");
                    } else if (trimmed.equals("cloud city") && !flipBackLocationFragments.contains("bespin")) {
                        flipBackLocationFragments.add("bespin");
                    }
                }
            }
        } else {
            String cleaned = locLower.replaceFirst("^\\d+\\s+", "");
            cleaned = cleaned.replaceAll("\\s+(?:locations?|sites?|battlegrounds?|systems?)\\s*$", "").trim();
            if (!cleaned.isEmpty() && !isGenericWord(cleaned)) {
                flipBackLocationFragments.add(cleaned);
                if (cleaned.equals("bespin") && !flipBackLocationFragments.contains("cloud city")) {
                    flipBackLocationFragments.add("cloud city");
                } else if (cleaned.equals("cloud city") && !flipBackLocationFragments.contains("bespin")) {
                    flipBackLocationFragments.add("bespin");
                }
            }
        }
    }

    /**
     * V22.2: Check if a location is relevant for flip-back protection (post-flip defense).
     * After objective flips, these are the locations we MUST hold to prevent flip-back.
     */
    public boolean isFlipBackProtectionLocation(String locationTitle) {
        if (!analyzed || locationTitle == null) return false;
        String titleLower = locationTitle.toLowerCase(Locale.ROOT);

        Boolean structuredMatch = exactStructuredTitleMatch(
                titleLower, "postFlip", "flipBack");
        if (structuredMatch != null) return structuredMatch;

        if (flipBackLocations.contains(titleLower)) return true;

        for (String fragment : flipBackLocationFragments) {
            if (titleLower.contains(fragment)) return true;
        }

        // Fallback: if no specific flip-back locations found, use front-side locations
        if (flipBackLocations.isEmpty() && flipBackLocationFragments.isEmpty()) {
            return isObjectiveRelevantLocation(locationTitle);
        }

        return false;
    }

    /**
     * V22.2: Get the set of location fragments that need protection post-flip.
     */
    public Set<String> getFlipBackLocationFragments() {
        return Collections.unmodifiableSet(flipBackLocationFragments);
    }

    /**
     * V22.2: Does the objective flip back if we don't occupy locations?
     */
    public boolean flipBackRequiresOccupy() { return flipBackRequiresOccupy; }

    /**
     * V22.2: Does the objective flip back if we don't control locations?
     */
    public boolean flipBackRequiresControl() { return flipBackRequiresControl; }

    /**
     * V22.2: Get the raw flip-back condition text for logging.
     */
    public String getFlipBackConditionText() { return flipBackConditionText; }

    /**
     * V22.5: Check if this objective references Bespin/Cloud City.
     * If so, occupying Bespin system with a ship is critical for enabling
     * Dark Deal and Cloud City Occupation effects.
     */
    public boolean needsBespinSystemPresence() {
        if (!analyzed) return false;
        return flipConditionLocationFragments.contains("bespin") ||
               flipConditionLocationFragments.contains("cloud city") ||
               flipBackLocationFragments.contains("bespin") ||
               flipBackLocationFragments.contains("cloud city");
    }

    /**
     * V29 UPDATED 2026-07-06 (TDIGWATT bug B): does the active objective's OWN game text
     * forbid deploying Executor? TDIGWATT (V) (Card226_012) reads "For remainder of game,
     * you may not deploy Admiral's Orders or [Death Star II] Executor." — the V29
     * BESPIN-FIRST gate in DeployEvaluator must not hold character deploys hostage waiting
     * for a ship the objective itself bans. Universal sentence-scan of the blueprint text
     * (no card-name/id lists): a sentence containing "executor" plus a deploy-forbid phrase
     * counts. Classic TDIGWATT (Card109_012) has no such clause on either side, so this
     * stays false for it (its back-side "may not cancel"/"may not be modified" do not match).
     */
    public boolean objectiveForbidsDeployingExecutor() {
        if (!analyzed || objectiveGameText == null) return false;
        String textLower = objectiveGameText.toLowerCase(Locale.ROOT);
        for (String sentence : textLower.split("\\.")) {
            if (!sentence.contains("executor")) continue;
            if (sentence.contains("may not deploy") || sentence.contains("cannot deploy")
                || sentence.contains("may not be deployed")) {
                return true;
            }
        }
        return false;
    }

    private void logAnalysisResults() {
        LOG.warn("\uD83C\uDFAF ===================================================================");
        LOG.warn("\uD83C\uDFAF OBJECTIVE ANALYSIS COMPLETE: '{}'", objectiveTitle);
        LOG.warn("\uD83C\uDFAF ===================================================================");
        LOG.warn("\uD83C\uDFAF Flip condition: '{}'", flipConditionText != null ? flipConditionText : "NONE FOUND");
        LOG.warn("\uD83C\uDFAF Requires occupy: {}, control: {}", requiresOccupy, requiresControl);
        LOG.warn("\uD83C\uDFAF Flip locations (exact): {}", flipConditionLocations);
        LOG.warn("\uD83C\uDFAF Flip location fragments: {}", flipConditionLocationFragments);
        LOG.warn("\uD83C\uDFAF Required on table: {}", requiredCardsOnTable);
        LOG.warn("\uD83C\uDFAF Pullable from reserve: {}", pullableCards);
        LOG.warn("\uD83C\uDFAF Currently flipped: {}", isFlipped);
        LOG.warn("\uD83D\uDEE1 --- FLIP-BACK PROTECTION (V22.2) ---");
        LOG.warn("\uD83D\uDEE1 Flip-back condition: '{}'", flipBackConditionText != null ? flipBackConditionText : "NONE/DEFAULT");
        LOG.warn("\uD83D\uDEE1 Flip-back requires occupy: {}, control: {}", flipBackRequiresOccupy, flipBackRequiresControl);
        LOG.warn("\uD83D\uDEE1 Flip-back locations (exact): {}", flipBackLocations);
        LOG.warn("\uD83D\uDEE1 Flip-back location fragments: {}", flipBackLocationFragments);
        if (isISBOperations) {
            LOG.warn("\uD83D\uDD75 --- ISB OPERATIONS (V25) ---");
            LOG.warn("\uD83D\uDD75 ISB agents needed on table to flip: {}", isbFlipAgentCount);
            LOG.warn("\uD83D\uDD75 Rebel Base locations to control: {}", isbFlipLocationCount);
            LOG.warn("\uD83D\uDD75 Flips back if no ISB agents: {}", isbFlipBackNoAgents);
        }
        if (isHuntDownV) {
            LOG.warn("\u2694 --- HUNT DOWN V (V25) ---");
            LOG.warn("\u2694 Vader needed at battleground to flip: {}", huntDownNeedsVader);
            LOG.warn("\u2694 Flips back if Vader not on table: {}", huntDownFlipBackNoVader);
        }
        LOG.warn("\uD83C\uDFAF ===================================================================");
    }
}
