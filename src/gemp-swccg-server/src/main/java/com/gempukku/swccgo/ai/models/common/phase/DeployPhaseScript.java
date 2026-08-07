package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: DEPLOY-1 (Sequencing & Budget) (reorg 2026-07-06) ═══
// Owns: the DPS bucket walk: V67bc bucket ordering, V67bb, locations-first doctrine, V179
// deploy-from-reserve step. Hub: none. KIND mix (DEPLOY-1 overall): 11 ORDERING / 9 BANDED / 4 VETO.
// PARITY PAIR WARNING: V179 (here) <-> V67ai (deploy-from-reserve magnitudes in DeployEvaluator/
// ActionTextEvaluator) must stay in lockstep — re-check both when touching either.
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: DEPLOY-1 twin DeployPhasePlanner, DEPLOY-2 (V136 siting consumes the walk),
// PULL-ENGINE (V60/V67ai scoring on the reserve routes). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
/**
 * V67bb DEPLOY PHASE SCRIPT (DPS) — REDESIGNED.
 *
 * Steve's principle (2026-05-08): every {@code decide()} during deploy phase
 * walks the FULL hierarchy fresh, top to bottom. Take the highest-priority
 * viable action that exists right now. Pass only when every step has nothing.
 *
 * Why re-walk every call: deploying UNLOCKS. A location lands → characters
 * needing that site become deployable. A character lands → matching weapons
 * become deployable, drain bonuses come online. Each {@code decide()} is a
 * fresh decision; the world has changed since last call.
 *
 * STRICT ORDER:
 *   STEP 1  LOCATIONS         — anything that puts a LOCATION on table
 *   STEP 2  OBJECTIVE ROUTE ASSETS — exact still-needed objective package pieces
 *   STEP 3  KEY CHARACTERS    — chars matching ObjectiveAnalyzer.getStrategyCharacterTokens
 *   STEP 4  OTHER CHARACTERS  — remaining char/starship/vehicle deploys
 *   STEP 5  WEAPONS           — weapon deploys / pulls
 *   STEP 6  DEVICES           — device deploys / pulls
 *   (PASS only when every step has 0 candidates)
 *
 * KEY MECHANIC — per-action card RESOLUTION:
 *
 *   Action text alone is unreliable (generic "Play a card" / bare "Deploy" reveal
 *   nothing). DPS resolves each action to the CARD it would put on table by:
 *
 *     A) Direct deploys (bare "Deploy" with cardId) → look up cardId in game
 *        state, read its blueprint category.
 *     B) Generic "Play a card" / "Deploy a card" → peek at the player's HAND;
 *        the action satisfies whichever steps the hand contents could satisfy.
 *        ONE entry-point action can satisfy MULTIPLE steps.
 *     C) Top-level pull effects (objective/effect game text) → parse the
 *        source card's game text via DeckOracle.parseSourceCardPullTargets to
 *        learn what categories it can pull.
 *     D) Action text mentions a specific card name ("Deploy Lift Tube ...")
 *        → blueprint hint extraction (existing).
 *     E) Action text keyword fallback (last resort).
 *
 *   "Take X into hand" / "Take X into Force Pile" actions are EXCLUDED — they
 *   don't put a card on the table, so they don't satisfy any deploy step.
 *
 * The walker iterates STEP 1 → STEP 5; for each step, collects every action
 * that lists this step in its resolved set; first non-empty step wins. The
 * existing scoring (V67* in DeployEvaluator etc.) picks WITHIN that set.
 */
public abstract class DeployPhaseScript {
    private final Logger log;

    protected DeployPhaseScript(Logger log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    private static final Pattern BLUEPRINT_HINT_PATTERN =
        Pattern.compile("value=['\"]([^'\"]+)['\"]");

    // V179 (2026-06): location-keyword set MIRRORS DeployEvaluator V67ai
    // (DeployEvaluator.java ~line 3275). Generic category-pull actions
    // ("Deploy a farm from Reserve Deck") name the card only by category noun —
    // the actual card isn't resolved until the search runs, so we must text-infer.
    // This list MUST stay in sync with V67ai's list, or the DPS walk and the
    // scorer disagree: a location the scorer ranks +1800 gets dropped from the
    // LOCATIONS bucket and a key character deploys instead. Real bug (replay
    // aab2jiaa5sca): "Deploy a farm from Reserve Deck" → token 'farm' → not in
    // the old thin list → no LOCATIONS bucket → Young Skywalker (KEY_CHARACTERS)
    // deployed every turn while the free drain-site farm rotted in Reserve.
    private static final String[] LOCATION_HINT_KEYWORDS = new String[] {
        "site", "battleground", "location", "system", "farm",
        "cantina", "mos eisley", "tatooine", "endor", "hoth",
        "dagobah", "naboo", "yavin", "bespin", "cloud city",
        "mustafar", "malachor", "mapuzo", "jabiim", "coruscant",
        "kashyyyk", "kessel", "kamino", "geonosis", "alderaan",
        "docking bay", "spaceport", "city", "palace", "temple",
        "safehouse", "corridor", "village", "outpost",
        "hallway", "sector"
    };

    /** V179: true if the text names a location category (V67ai-parity). */
    private static boolean namesLocation(String lc) {
        if (lc == null) return false;
        for (String kw : LOCATION_HINT_KEYWORDS) {
            if (lc.contains(kw)) return true;
        }
        return false;
    }

    public enum Step {
        LOCATIONS,
        OBJECTIVE_ROUTE_ASSETS,
        KEY_CHARACTERS,
        OTHER_CHARACTERS,
        WEAPONS,
        DEVICES
    }

    public Result selectAllowedActions(AwaitingDecision decision, GameState gameState,
                                       SwccgGame game, String playerId,
                                       ObjectiveAnalyzer objectiveAnalyzer) {
        if (decision == null || gameState == null) return null;

        Map<String, String[]> params = decision.getDecisionParameters();
        if (params == null) return null;

        String[] actionIds = params.get("actionId");
        String[] actionTexts = params.get("actionText");
        String[] cardIds = params.get("cardId");
        if (actionIds == null || actionTexts == null
            || actionIds.length == 0 || actionIds.length != actionTexts.length) {
            return null;
        }

        int turn = gameState.getPlayersLatestTurnNumber(playerId);

        Set<String> keyCharTokens = objectiveAnalyzer != null
            ? objectiveAnalyzer.getStrategyCharacterTokens(game, playerId)
            : Collections.emptySet();

        // 1) RESOLVE every action to the SET of steps it could satisfy.
        Map<Step, LinkedHashSet<String>> byStep = new EnumMap<>(Step.class);
        for (Step s : Step.values()) byStep.put(s, new LinkedHashSet<>());

        int classifiedCount = 0;
        for (int i = 0; i < actionIds.length; i++) {
            String aid = actionIds[i];
            String txt = actionTexts[i] != null ? actionTexts[i] : "";
            if (aid == null || aid.isEmpty()) continue;

            String cidStr = (cardIds != null && i < cardIds.length) ? cardIds[i] : null;
            Set<Step> steps = resolveSteps(txt, cidStr, gameState, game, playerId,
                                            keyCharTokens, objectiveAnalyzer);
            if (steps != null && !steps.isEmpty()) {
                for (Step s : steps) byStep.get(s).add(aid);
                classifiedCount++;
            }
        }

        if (classifiedCount == 0) {
            log.warn("V67bb DPS NO-OPINION: 0 actions classified — falling through to normal scoring (turn={}, totalActions={})",
                turn, actionIds.length);
            return null;
        }

        // 2) V67bc HIERARCHY: build ORDERED list of step buckets that pass qualify().
        //    CombinedEvaluator walks them top to bottom, picks first action whose
        //    score is above the bad threshold. PASS only when ALL buckets exhausted
        //    with all-bad actions. This implements Steve's "walk full hierarchy
        //    every call, take first viable, only pass when nothing viable" rule.
        java.util.List<Set<String>> orderedBuckets = new ArrayList<>();
        java.util.List<String> bucketLabels = new ArrayList<>();
        Set<String> unionAll = new LinkedHashSet<>();

        for (Step step : Step.values()) {
            Set<String> ids = byStep.get(step);
            if (ids == null || ids.isEmpty()) continue;
            Set<String> qualified = qualify(step, ids, gameState, game, playerId, turn);
            if (qualified.isEmpty()) {
                log.warn("V67bc DPS step={} qualify-skip (no candidates pass step gate, turn={})",
                    step, turn);
                continue;
            }
            orderedBuckets.add(qualified);
            bucketLabels.add(step.name());
            unionAll.addAll(qualified);
            log.warn("V67bc DPS step={} bucket added: {} candidate(s) (turn={})",
                step, qualified.size(), turn);
        }

        if (orderedBuckets.isEmpty()) {
            // Every step was empty AFTER qualify (e.g. chars in hand but no BG turn ≤ 2)
            // → legitimate Steve-says-pass.
            log.warn("V67bc DPS: classified={} actions but no step qualified → PASS (turn={})",
                classifiedCount, turn);
            return new Result(null, Collections.emptyList(), Collections.emptyList(),
                Collections.emptySet(),
                String.format("V67bc DPS classified=%d but no step qualified", classifiedCount));
        }

        Step topStep = Step.valueOf(bucketLabels.get(0));
        String reason = String.format(
            "V67bc DPS hierarchy: %d buckets %s (top=%s, union=%d, turn=%d, classified=%d)",
            orderedBuckets.size(), bucketLabels, topStep, unionAll.size(), turn, classifiedCount);
        log.warn(reason);
        return new Result(topStep, orderedBuckets, bucketLabels, unionAll, reason);
    }

    /**
     * Step gate. Today only character steps gate on BG availability.
     * Other steps qualify the whole bucket — existing evaluators pick within.
     */
    private Set<String> qualify(Step step, Set<String> ids, GameState gameState,
                                SwccgGame game, String playerId, int turn) {
        switch (step) {
            case KEY_CHARACTERS:
            case OTHER_CHARACTERS: {
                boolean bgExists = anyBattlegroundOnTable(gameState, game);
                if (bgExists) return ids;
                if (turn <= 2) return Collections.emptySet();
                return ids;
            }
            default:
                return ids;
        }
    }

    /**
     * Resolve an action to the SET of steps it can satisfy.
     * Returns empty set if the action does not put a card on the table.
     */
    private Set<Step> resolveSteps(String actionText, String cardIdStr,
                                    GameState gameState, SwccgGame game, String playerId,
                                    Set<String> keyCharTokens) {
        return resolveSteps(actionText, cardIdStr, gameState, game, playerId,
                keyCharTokens, null);
    }

    private Set<Step> resolveSteps(String actionText, String cardIdStr,
                                    GameState gameState, SwccgGame game, String playerId,
                                    Set<String> keyCharTokens,
                                    ObjectiveAnalyzer objectiveAnalyzer) {
        EnumSet<Step> steps = EnumSet.noneOf(Step.class);
        if (actionText == null) return steps;
        String txt = actionText.toLowerCase(Locale.ROOT).trim();

        PhysicalCard sourceCard =
                findCardByIdSafe(
                    gameState, cardIdStr);
        boolean countedOperativeLocationSource =
                objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isCountedOperativeSiteRouteSource(
                        game, playerId, sourceCard);
        boolean exhaustedObjectiveLocationPull =
                objectiveAnalyzer != null
                && objectiveAnalyzer
                    .usesObjectiveLocationPullSequence()
                && (objectiveAnalyzer.isCurrentObjectiveSourceCard(
                        sourceCard)
                    || countedOperativeLocationSource)
                && !objectiveAnalyzer
                    .hasObjectiveLocationRouteCandidateInReserve(
                        game, playerId, sourceCard);
        if (objectiveAnalyzer != null
                && objectiveAnalyzer.isHiddenPathJabiimRouteAction(
                    game, playerId, sourceCard, actionText)) {
            if (objectiveAnalyzer
                    .isExhaustedHiddenPathJabiimRouteAction(
                        game, playerId, sourceCard, actionText)) {
                return steps;
            }
            steps.add(Step.LOCATIONS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isSetYourCourseDeathStarSitePullAction(
                        game, playerId, sourceCard, actionText)) {
            steps.add(Step.LOCATIONS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer.isHiddenPathSurvivorRouteAction(
                    game, playerId, sourceCard, actionText)) {
            steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer.isHiddenPathHolocronAction(
                    game, playerId, sourceCard, actionText)) {
            steps.add(Step.DEVICES);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isFirstOrderReignsNavyRouteAction(
                        game, playerId,
                        sourceCard, actionText)) {
            steps.add(Step.LOCATIONS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isMassassiAttackRunPackageUploadAction(
                        game, playerId,
                        sourceCard, actionText)) {
            steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isExhaustedRalltiirFrontRouteAction(
                        game, playerId,
                        sourceCard, actionText)) {
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isRalltiirFrontRouteAction(
                        game, playerId,
                        sourceCard, actionText)) {
            if (objectiveAnalyzer
                    .hasRalltiirFrontSiteRouteCandidateInReserve(
                        game, playerId, sourceCard)) {
                steps.add(Step.LOCATIONS);
            } else {
                steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            }
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isExhaustedTwinSunsFrontSiteRouteAction(
                        game, playerId,
                        sourceCard, actionText)) {
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isTwinSunsFrontSiteRouteAction(
                        game, playerId,
                        sourceCard, actionText)) {
            steps.add(Step.LOCATIONS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isTwinSunsOccupationPullAction(
                        game, playerId,
                        sourceCard, actionText)) {
            steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isNoMoneyNoPartsWattoPullAction(
                        game, playerId,
                        sourceCard, actionText)) {
            steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            return steps;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isNoMoneyNoPartsBackGambitAction(
                        game, playerId,
                        sourceCard, actionText)) {
            steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            return steps;
        }
        if (NoMoneyNoPartsObjectivePolicy
                .isExactOpponentWattoRemovalAction(
                    game, playerId, sourceCard, actionText)) {
            steps.add(Step.OBJECTIVE_ROUTE_ASSETS);
            return steps;
        }

        // EXCLUDE pulls that go INTO HAND / pile, not to table — these are not
        // deploy-step actions. The narrow exception is an upload that fetches a
        // still-missing typed objective actor needed by this phase's formation.
        if (txt.contains("into hand") || txt.contains("into your hand")
                || txt.contains("into used pile") || txt.contains("into reserve")
                || txt.contains("into force pile")) {
            if (objectiveAnalyzer != null
                    && objectiveAnalyzer.isFlipGateActorUploadIntoHandAction(
                        game, playerId, sourceCard, actionText)) {
                steps.add(Step.KEY_CHARACTERS);
            }
            return steps;
        }

        // === A) Bare deploy whose cardId IS the card being deployed ===
        // (e.g. action='Deploy', cardId=238 → card 238 is the card being put on table.)
        if (cardIdStr != null && (txt.equals("deploy") || txt.equals("deploy a card"))) {
            PhysicalCard card = findCardByIdSafe(gameState, cardIdStr);
            if (card != null && card.getBlueprint() != null) {
                Step s = stepForCard(
                        card, keyCharTokens,
                        objectiveAnalyzer, game, playerId);
                if (s != null) {
                    steps.add(s);
                    return steps;
                }
            }
        }

        // === B) Generic "Play a card" — peek at hand contents ===
        // ONE entry-point action satisfies WHICHEVER steps the hand can satisfy.
        if (txt.equals("play a card") || txt.equals("play card")
                || txt.equals("deploy from hand")) {
            if (gameState != null && playerId != null) {
                try {
                    List<PhysicalCard> hand = gameState.getHand(playerId);
                    if (hand != null) {
                        for (PhysicalCard hc : hand) {
                            if (hc == null || hc.getBlueprint() == null) continue;
                            Step s = stepForCard(
                                    hc, keyCharTokens,
                                    objectiveAnalyzer, game, playerId);
                            if (s != null) steps.add(s);
                        }
                    }
                } catch (Exception ignored) { }
            }
            if (!steps.isEmpty()) return steps;
            // If hand is empty / unresolvable, fall through to other classifiers
            // (some "play a card" prompts also offer reserve/used-pile sources).
        }

        // === C) Action text mentions a specific deployable target by NAME ===
        // ("Deploy Lift Tube from outside your deck.", "Deploy Bespin", etc.)
        // Use blueprint hint embedded in HTML if present, else regex on the name.
        String bpId = extractBlueprintId(actionText);
        if (bpId != null) {
            PhysicalCard bpCard = findCardByBlueprint(gameState, bpId);
            if (bpCard != null && bpCard.getBlueprint() != null) {
                Step s = stepForCard(
                        bpCard, keyCharTokens,
                        objectiveAnalyzer, game, playerId);
                if (s != null
                        && !(s == Step.LOCATIONS
                            && exhaustedObjectiveLocationPull)) {
                    steps.add(s);
                }
            }
        }

        // === D) Top-level pull effect — read source card's game text ===
        // Source card = the cardId that owns this action. Its game text tells us
        // what categories of cards it can pull. (Evil Is Everywhere → "[download]
        // a mobile hallway or [Episode I] lightsaber" → LOCATIONS + WEAPONS.)
        if (cardIdStr != null) {
            PhysicalCard srcCard = findCardByIdSafe(gameState, cardIdStr);
            if (srcCard != null && srcCard.getBlueprint() != null) {
                // BATCH1-CORR (2026-07-13, Codex m00229): side-aware owner.
                String gt = sourceCardFullGameText(
                    srcCard.getBlueprint(), gameState != null ? gameState.getSide(playerId) : null);
                if (gt != null) {
                    try {
                        java.util.List<String> tgts = parseSourceCardPullTargets(gt);
                        for (String t : tgts) {
                            Step s = stepForPullTargetText(t);
                            if (s == null) continue;
                            // V179 FIX (#4, Steve 2026-06-29): a [download]/pull that NAMES a specific
                            // location the bot already HOLDS must NOT classify as a LOCATIONS pull. You
                            // cannot download a location that is in your hand, so treating it as a
                            // high-priority location pull makes Rando fire the (often once-per-turn)
                            // download BEFORE deploying the actual location from hand — wasting it. In
                            // the 2026-06-29 replay this cost A Good Friend / A Cunning Warrior: they
                            // download Ahch-To + Be With Me, and Be With Me needs an Ahch-To location on
                            // table first, but Rando fired the download while Ahch-To sat in hand, so
                            // nothing was legal and the epic event never came out. Dropping the LOCATIONS
                            // tag lets the real hand-deploy win the LOCATIONS step and land first; the
                            // download still classifies by its OTHER targets (weapon/effect) and fires in
                            // a later step once the location is down. Generic targets ("a location",
                            // "a site") never match a specific hand-card title, so objective/generic
                            // location pulls are unaffected.
                            if (s == Step.LOCATIONS && namedLocationInHand(gameState, playerId, t)) {
                                continue;
                            }
                            if (s == Step.LOCATIONS
                                    && exhaustedObjectiveLocationPull) {
                                continue;
                            }
                            steps.add(s);
                        }
                    } catch (Exception ignored) { }
                }
            }
        }

        // === E) Action text keyword fallback ===
        if (steps.isEmpty()) {
            Step kw = classifyByKeywords(txt);
            if (kw != null
                    && !(kw == Step.LOCATIONS
                        && exhaustedObjectiveLocationPull)) {
                steps.add(kw);
            }
        }

        return steps;
    }

    /** Map a card's category → step bucket. Returns null for non-deploy categories. */
    private Step stepForCard(
            PhysicalCard card, Set<String> keyCharTokens,
            ObjectiveAnalyzer objectiveAnalyzer,
            SwccgGame game, String playerId) {
        if (card == null || card.getBlueprint() == null) return null;
        CardCategory cat = card.getBlueprint().getCardCategory();
        if (cat == null) return null;
        if (objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isSetYourCourseCompatibleSuperlaserDeployCandidate(
                        game, playerId, card)) {
            return Step.OBJECTIVE_ROUTE_ASSETS;
        }
        if (objectiveAnalyzer != null
                && (objectiveAnalyzer
                    .isMassassiAttackRunPackageDeployCandidate(
                        game, playerId, card)
                    || objectiveAnalyzer
                    .isMassassiAttackRunCarrierDeployCandidate(
                        game, playerId, card))) {
            return Step.OBJECTIVE_ROUTE_ASSETS;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer.isOldAlliesRouteDeployCandidate(
                    game, playerId, card)) {
            return Step.OBJECTIVE_ROUTE_ASSETS;
        }
        if (objectiveAnalyzer != null
                && objectiveAnalyzer.isTheyHaveNoIdeaRouteDeployCandidate(
                    game, playerId, card)) {
            return Step.OBJECTIVE_ROUTE_ASSETS;
        }
        if (cat == CardCategory.WEAPON
                && objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isShieldMainGeneratorPriorityCannonDeploy(
                        game, playerId, card)) {
            return Step.OBJECTIVE_ROUTE_ASSETS;
        }
        switch (cat) {
            case LOCATION: return Step.LOCATIONS;
            case CHARACTER: {
                boolean keyCharacter = objectiveAnalyzer != null
                        ? objectiveAnalyzer.isStrategyKeyCharacter(
                                game, playerId, card)
                        : isKeyCharacter(
                                card.getTitle(), keyCharTokens);
                return keyCharacter
                        ? Step.KEY_CHARACTERS
                        : Step.OTHER_CHARACTERS;
            }
            case STARSHIP:
            case VEHICLE:
            case CREATURE:
                return isKeyCharacter(card.getTitle(), keyCharTokens)
                    ? Step.KEY_CHARACTERS : Step.OTHER_CHARACTERS;
            case WEAPON: return Step.WEAPONS;
            case DEVICE: return Step.DEVICES;
            default:    return null;  // EFFECT/INTERRUPT/EPIC_EVENT/OBJECTIVE/etc.
        }
    }

    /** Map a parsed pull-target keyword → step. */
    private Step stepForPullTargetText(String t) {
        if (t == null) return null;
        String lc = t.toLowerCase(Locale.ROOT);
        // V179: V67ai-parity location set (adds "farm", planet names, "docking
        // bay", etc.) so generic category-pull tokens classify as LOCATIONS.
        if (namesLocation(lc)) {
            return Step.LOCATIONS;
        }
        if (lc.contains("weapon") || lc.contains("lightsaber") || lc.contains("blaster")
                || lc.contains("rifle") || lc.contains("pistol") || lc.contains("cannon")) {
            return Step.WEAPONS;
        }
        if (lc.contains("device")) return Step.DEVICES;
        if (lc.contains("character") || lc.contains("alien") || lc.contains("droid")
                || lc.contains("creature") || lc.contains("starship")
                || lc.contains("vehicle") || lc.contains("admiral") || lc.contains("officer")
                || lc.contains("jedi") || lc.contains("sith") || lc.contains("padawan")
                || lc.contains("imperial") || lc.contains("rebel")) {
            return Step.OTHER_CHARACTERS;
        }
        return null;
    }

    /** V179 FIX (#4): true if a LOCATION card in {@code playerId}'s hand has a title containing the
     *  named location target {@code t} — i.e. the pull would fetch a location the bot already holds,
     *  so the bot should deploy it from hand rather than fire the download. Bare generic category
     *  words are excluded so only specific named locations gate (objective/generic pulls untouched). */
    private boolean namedLocationInHand(GameState gameState, String playerId, String t) {
        if (gameState == null || playerId == null || t == null) return false;
        String tl = t.toLowerCase(Locale.ROOT).trim();
        if (tl.length() < 4 || tl.equals("location") || tl.equals("site") || tl.equals("system")
                || tl.equals("battleground") || tl.equals("docking bay") || tl.equals("mobile site")) {
            return false;
        }
        try {
            List<PhysicalCard> hand = gameState.getHand(playerId);
            if (hand == null) return false;
            for (PhysicalCard hc : hand) {
                if (hc == null || hc.getBlueprint() == null) continue;
                if (hc.getBlueprint().getCardCategory() != CardCategory.LOCATION) continue;
                String ht = hc.getTitle();
                if (ht != null && ht.toLowerCase(Locale.ROOT).contains(tl)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Last-resort action-text keyword classifier (E). */
    private Step classifyByKeywords(String lower) {
        if (lower == null) return null;
        boolean isDeployVerb = lower.contains("deploy") || lower.contains("place")
                                || lower.contains("transfer");
        boolean isPullVerb = lower.contains("take") || lower.contains("search")
                              || lower.contains("retrieve") || lower.contains("pull")
                              || lower.contains("download");
        if (!isDeployVerb && !isPullVerb) return null;

        // V179: V67ai-parity location set so "deploy a farm from reserve deck"
        // classifies as LOCATIONS (was falling through → no LOCATIONS bucket).
        if (namesLocation(lower)) return Step.LOCATIONS;
        if (lower.contains("weapon") || lower.contains("lightsaber")
                || lower.contains("blaster")) return Step.WEAPONS;
        if (lower.contains("device")) return Step.DEVICES;
        if (lower.contains("character") || lower.contains("starship")
                || lower.contains("alien") || lower.contains("creature")) {
            return Step.OTHER_CHARACTERS;
        }
        return null;
    }

    private boolean isKeyCharacter(String title, Set<String> keyTokens) {
        if (title == null || keyTokens == null || keyTokens.isEmpty()) return false;
        String t = title.toLowerCase(Locale.ROOT);
        for (String tok : keyTokens) {
            if (tok == null) continue;
            String tk = tok.toLowerCase(Locale.ROOT);
            if (!tk.isEmpty() && t.contains(tk)) return true;
        }
        return false;
    }

    private String extractBlueprintId(String actionText) {
        if (actionText == null) return null;
        Matcher m = BLUEPRINT_HINT_PATTERN.matcher(actionText);
        if (m.find()) return m.group(1);
        return null;
    }

    private PhysicalCard findCardByBlueprint(GameState gameState, String bpId) {
        if (gameState == null || bpId == null) return null;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) continue;
            String cardBp = card.getBlueprintId(true);
            if (bpId.equals(cardBp)) return card;
        }
        return null;
    }

    private PhysicalCard findCardByIdSafe(GameState gameState, String cardIdStr) {
        if (gameState == null || cardIdStr == null || cardIdStr.isEmpty()) return null;
        try {
            int cid = Integer.parseInt(cardIdStr);
            return gameState.findCardById(cid);
        } catch (NumberFormatException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean anyBattlegroundOnTable(GameState gameState, SwccgGame game) {
        if (gameState == null || game == null) return false;
        try {
            List<PhysicalCard> locs = gameState.getLocationsInOrder();
            if (locs == null) return false;
            for (PhysicalCard loc : locs) {
                if (loc == null) continue;
                try {
                    if (game.getModifiersQuerying().isBattleground(gameState, loc, null)) {
                        return true;
                    }
                } catch (Exception ignored) {
                    SwccgCardBlueprint bp = loc.getBlueprint();
                    if (bp != null) {
                        String gameText = bp.getGameText();
                        if (gameText != null && gameText.toLowerCase(Locale.ROOT).contains("battleground")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("V67bb DPS battleground scan failed: {}", e.getMessage());
        }
        return false;
    }

    protected abstract String sourceCardFullGameText(SwccgCardBlueprint blueprint, Side side);

    protected abstract List<String> parseSourceCardPullTargets(String gameText);

    /** DPS result: ordered hierarchy of step buckets + union of allowed actions. */
    public static class Result {
        public final Step step;                              // top-priority step (legacy)
        public final java.util.List<Set<String>> stepBuckets;  // V67bc: ordered, highest first
        public final java.util.List<String> stepBucketLabels;  // V67bc: labels for logging
        public final Set<String> allowedActionIds;           // union of all buckets
        public final String reason;

        public Result(Step step, java.util.List<Set<String>> stepBuckets,
                      java.util.List<String> stepBucketLabels,
                      Set<String> allowedActionIds, String reason) {
            this.step = step;
            this.stepBuckets = stepBuckets;
            this.stepBucketLabels = stepBucketLabels;
            this.allowedActionIds = allowedActionIds;
            this.reason = reason;
        }

        public boolean isEmptyPass() {
            return allowedActionIds == null || allowedActionIds.isEmpty();
        }
    }
}
