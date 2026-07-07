package com.gempukku.swccgo.ai.models.rando.strategy;

import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

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
    private static final Logger LOG = RandoLogger.getStrategyLogger();

    private final Set<String> flipConditionLocations = new HashSet<>();
    private final Set<String> flipConditionLocationFragments = new HashSet<>();
    private final Set<String> requiredCardsOnTable = new HashSet<>();
    private final Set<String> pullableCards = new HashSet<>();
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
    private String flipCriticalControlCard = null;
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

            SwccgCardBlueprint blueprint = objectiveCard.getBlueprint();
            if (blueprint == null) return;

            String title = objectiveCard.getTitle();
            String gameText = blueprint.getGameText();

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

            parseGameText(gameText);
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

        if (flipConditionLocations.contains(titleLower)) return true;

        for (String fragment : flipConditionLocationFragments) {
            if (titleLower.contains(fragment)) return true;
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

    public Set<String> getFlipConditionLocationFragments() {
        return Collections.unmodifiableSet(flipConditionLocationFragments);
    }

    public boolean isAnalyzed() { return analyzed; }
    public boolean isFlipped() { return isFlipped; }
    public String getObjectiveTitle() { return objectiveTitle; }
    public String getFlipConditionText() { return flipConditionText; }
    // V193 (Steve, 2026-07-07): the site Rando must CONTROL to enable an objective flip
    // (e.g. Endor: Bunker for Establish Secret Base (V)). null when the objective has none.
    public String getFlipCriticalControlSite() { return flipCriticalControlSite; }
    public String getFlipCriticalControlCard() { return flipCriticalControlCard; }
    public Set<String> getRequiredCardsOnTable() { return Collections.unmodifiableSet(requiredCardsOnTable); }
    public Set<String> getPullableCards() { return Collections.unmodifiableSet(pullableCards); }
    public boolean requiresOccupy() { return requiresOccupy; }
    public boolean requiresControl() { return requiresControl; }

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
                    notes.add(new ScoreNote(-2000.0f,
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
                notes.add(new ScoreNote(-2000.0f,
                    "V110 MY LORD: HOLD non-senator '" + card.getTitle()
                        + "' — no non-Senate site on table yet, would land at Senate"));
                LOG.warn("V110 MY LORD: HOLD deploy non-senator {} → -2000 (no non-Senate site)",
                    card.getTitle());
            }
        }

        // === V108: MY LORD — prioritize deploying senators from hand ===
        if (analyzed && isMyLord && isCharacter && isSenatorCard(blueprint)) {
            notes.add(new ScoreNote(500.0f,
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
            notes.add(new ScoreNote(1500.0f,
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

    private void updateFlipStatus(PhysicalCard objectiveCard) {
        try {
            this.isFlipped = objectiveCard.isFlipped();
            LOG.debug("[ObjectiveAnalyzer] Objective flipped = {}", isFlipped);
        } catch (Exception e) {
            LOG.debug("[ObjectiveAnalyzer] Could not determine flip status: {}", e.getMessage());
        }
    }

    private void parseGameText(String gameText) {
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
        parseBackSideText(gameText);
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
        // location, so the regex never marks Starkiller Base relevant. Two named picks for
        // this deck's turn-0 setup:
        //   - Starkiller Base SYSTEM (208_51) is the "any other [Episode VII] location" the
        //     objective deploys. It has NO battleground icon (a battleground heuristic would
        //     miss it), but its once-per-turn [download] fetches Starkiller Base battleground
        //     sites, feeding the 2-battleground flip. Naming it here grants the +150 objective
        //     location bonus so it wins the "Choose [Episode VII] location to deploy" pick.
        //   - The First Order Was Just The Beginning: marked required/pullable so it is
        //     protected + objective-critical. The WINNING starting-effect preference lives in
        //     CardSelectionEvaluator (V186).
        if (objectiveTitle != null
                && objectiveTitle.toLowerCase(Locale.ROOT).contains("i want that map")) {
            addLocationFragment("starkiller base");
            requiredCardsOnTable.add("the first order was just the beginning");
            pullableCards.add("the first order was just the beginning");
            LOG.warn("[ObjectiveAnalyzer] V186: I Want That Map detected - naming Starkiller Base (location) + The First Order Was Just The Beginning (effect).");
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
        if (objectiveTitle != null
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
        name = name.replaceFirst("^(?:a |an |the )(?=[A-Z])", "");
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
     * SWCCG objectives have two sides separated by "[Back Side]" or "\\[Back Side]" in game text.
     * The back side tells us what conditions would cause the objective to flip BACK —
     * which means we lose our advantage. We need to prevent that.
     *
     * Common flip-back patterns:
     *   "Flip this card if opponent controls [locations]"
     *   "Flip this card if you do not occupy [locations]"
     *   "Place out of play if [condition]"
     */
    private void parseBackSideText(String gameText) {
        if (gameText == null) return;

        // Find the back side text
        String backText = null;
        int backIdx = gameText.indexOf("[Back Side]");
        if (backIdx >= 0) {
            backText = gameText.substring(backIdx + "[Back Side]".length()).trim();
        } else {
            backIdx = gameText.indexOf("\\[Back Side]");
            if (backIdx >= 0) {
                backText = gameText.substring(backIdx + "\\[Back Side]".length()).trim();
            }
        }

        if (backText == null || backText.isEmpty()) {
            LOG.warn("[ObjectiveAnalyzer] No [Back Side] text found — single-sided objective?");
            return;
        }

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
