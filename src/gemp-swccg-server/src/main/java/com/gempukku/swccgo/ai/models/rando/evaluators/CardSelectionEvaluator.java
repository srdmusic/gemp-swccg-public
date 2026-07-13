package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.rando.strategy.ShieldStrategy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Evaluates CARD_SELECTION and ARBITRARY_CARDS decisions.
 *
 * These are decisions where the player must select one or more cards
 * from a list (e.g., choosing where to deploy, which card to forfeit,
 * targeting for weapons, etc.).
 *
 * Decision types handled:
 * - "choose card to set sabacc value" -> Random selection
 * - "choose where to deploy" -> Pick best location
 * - "choose force to lose" -> Pick best card to lose
 * - "choose a card from battle to forfeit" -> Pick lowest forfeit value
 * - "choose a pilot" -> Pick best pilot
 * - "choose card to cancel" -> Cancel opponent's cards, not ours
 * - "choose target" -> Weapon/ability targeting
 *
 * Ported from Python card_selection_evaluator.py
 */
public class CardSelectionEvaluator extends ActionEvaluator {

    // Score constants
    private static final float VERY_GOOD_DELTA = 150.0f;
    private static final float GOOD_DELTA = 10.0f;
    private static final float BAD_DELTA = -10.0f;
    private static final float VERY_BAD_DELTA = -150.0f;
    private static final SwccgCardBlueprintLibrary FALLBACK_LIBRARY = new SwccgCardBlueprintLibrary();

    // V29: All unique starship names that appear in character game text.
    // Characters referencing these should deploy aboard the named ship, not to ground.
    // "avenger" excluded — false-matches "scavenger" in unrelated card text.
    // Generic pilot text ("adds X to power if piloting") is NOT a ship reference.
    // All unique starship names from the card database (171 unique ships, ~130 core names).
    // Used to detect characters whose game text references a specific ship.
    // Compound "X in Y" variants excluded — core ship name is already in list.
    private static final String[] UNIQUE_SHIP_NAMES = {
        // Major capital ships
        "executor", "home one", "chimaera", "devastator",
        "blockade flagship", "finalizer", "fulminatrix",
        "judicator", "tyrant", "thunderflare", "resolute",
        "accuser", "dominator", "endurance", "falleen's fist",
        "intimidator & persecutor", "liberator", "profundity",
        "defiance", "independence", "liberty", "redemption",
        "stalker", "conquest", "supremacy", "steadfast",
        "flagship executor",
        // Freighters, transports, shuttles, personal ships
        "slave i", "hound's tooth", "outrider", "punishing one",
        "meson martinet", "invisible hand", "tantive iv",
        "pulsar skate", "radiant vii", "azure angel",
        "bestoon legacy", "night buzzard", "queen's royal starship",
        "tydirium", "vader's custom tie", "vader's personal shuttle",
        "millennium falcon", "the falcon", "ig-2000", "virago",
        "wild karrde", "mist hunter", "rogue one",
        "emperor's personal shuttle", "jabba's space cruiser",
        "ghost", "phantom", "libertine", "luminous", "masanya",
        "quantum storm", "spiral", "first light", "bright hope",
        "lightmaker", "liswarr", "binder", "overseer", "visage",
        "din djarin's modified n-1", "odd ball's torrent starfighter",
        "plo koon's jedi starfighter", "bo-katan's gauntlet starfighter",
        "maul's sith infiltrator", "leia's resistance transport",
        "kylo ren's command shuttle", "kylo ren's tie silencer",
        "the emperor's shield", "the emperor's sword",
        "stolen first order tie fighter",
        "blockade support ship",
        // Starfighter squadrons — Red
        "red 1", "red 2", "red 3", "red 5", "red 6", "red 7",
        "red 8", "red 9", "red 10", "red 12",
        "red squadron 1", "red squadron 4", "red squadron 6", "red squadron 7",
        // Starfighter squadrons — Gold
        "gold 1", "gold 2", "gold 3", "gold 4", "gold 5", "gold 6",
        "gold squadron 1",
        // Starfighter squadrons — Blue, Green, Gray
        "blue squadron 1", "blue squadron 5",
        "green squadron 1", "green squadron 3",
        "gray squadron 1", "gray squadron 2",
        // Starfighter squadrons — Black, Obsidian, Onyx
        "black 2", "black 3", "black 4", "black 5", "black 6", "black 11",
        "obsidian 7", "obsidian 8", "obsidian 10",
        "onyx 1", "onyx 2",
        // Starfighter squadrons — Saber, Scimitar, Scythe, Bravo, Tala
        "saber 1", "saber 2", "saber 3", "saber 4",
        "scimitar 1", "scimitar 2",
        "scythe 1", "scythe 3",
        "bravo 1", "bravo 2", "bravo 3", "bravo 4", "bravo 5",
        "bravo fighter",
        "tala 1", "tala 2",
        // Special / misc
        "death star assault squadron", "dfs-1015", "dfs-1308", "dfs-327",
        "stinger", "avenger", "vengeance",
        // Generic ship type references (still mean "deploy aboard a ship")
        "capital starship", "star destroyer", "super star destroyer"
    };

    // False-positive phrases: game text containing these should NOT count as a ship reference.
    // "scavenger" contains "avenger", "poison stinger" contains "stinger", etc.
    private static final String[] SHIP_NAME_FALSE_POSITIVES = {
        "scavenger",           // contains "avenger" — Jawas, Dathcha, etc.
        "poison stinger",      // contains "stinger" — Florn Lamproid ability
        "vengeance of the dark prince"  // contains "vengeance" — unrelated card reference
    };

    private final Random random = new Random();

    public CardSelectionEvaluator() {
        super("CardSelection");
    }

    /**
     * Check if gameText contains a ship name, filtering out known false positives.
     * First checks if the text contains the ship name at all. If it does,
     * verifies the match isn't actually part of a false-positive phrase
     * (e.g., "avenger" inside "scavenger").
     */
    private static boolean gameTextContainsShipName(String gameText, String shipName) {
        if (!gameText.contains(shipName)) {
            return false;
        }
        // Check if every occurrence of shipName is inside a false-positive phrase
        for (String falsePositive : SHIP_NAME_FALSE_POSITIVES) {
            if (falsePositive.contains(shipName) && gameText.contains(falsePositive)) {
                // Remove all false-positive occurrences and re-check
                String cleaned = gameText.replace(falsePositive, "");
                if (!cleaned.contains(shipName)) {
                    return false;  // Only had false-positive matches
                }
            }
        }
        return true;
    }

    /**
     * Look up card name from blueprintId using the blueprint library.
     * This is the CORRECT way to get card info - proves the bot can actually look up cards.
     */
    private String getCardNameFromBlueprint(DecisionContext context, String blueprintId) {
        if (blueprintId == null || blueprintId.isEmpty() || "inPlay".equals(blueprintId)) {
            return null;
        }

        SwccgCardBlueprintLibrary library = FALLBACK_LIBRARY;
        if (library == null) {
            logger.warn("⚠️ Cannot look up blueprint '{}' - library is null", blueprintId);
            return null;
        }

        try {
            SwccgCardBlueprint blueprint = library.getSwccgoCardBlueprint(blueprintId);
            if (blueprint != null) {
                String title = blueprint.getTitle();
                logger.info("✅ BLUEPRINT LOOKUP SUCCESS: '{}' -> '{}'", blueprintId, title);
                return title;
            } else {
                logger.warn("⚠️ Blueprint '{}' not found in library", blueprintId);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Error looking up blueprint '{}': {}", blueprintId, e.getMessage());
        }
        return null;
    }

    /**
     * Get the blueprint object from blueprintId for accessing card properties.
     */
    private SwccgCardBlueprint getBlueprintFromId(DecisionContext context, String blueprintId) {
        if (blueprintId == null || blueprintId.isEmpty() || "inPlay".equals(blueprintId)) {
            return null;
        }

        SwccgCardBlueprintLibrary library = FALLBACK_LIBRARY;
        if (library == null) return null;

        try {
            return library.getSwccgoCardBlueprint(blueprintId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();
        return "CARD_SELECTION".equals(decisionType) || "ARBITRARY_CARDS".equals(decisionType);
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        String text = context.getDecisionText();
        String textLower = text != null ? text.toLowerCase(Locale.ROOT) : "";

        // ========== CRITICAL DEBUG LOGGING ==========
        logger.warn("🚀🚀🚀 [CardSelectionEvaluator.evaluate] ENTRY POINT - JAR VERSION 2026-02-23-V21 🚀🚀🚀");
        logger.warn("🔍 Decision type: {}", context.getDecisionType());
        logger.warn("🔍 Decision text (FULL): {}", text);

        // === V21: BAN CERTAIN EFFECTS AS STARTING EFFECTS ===
        // These should never be deployed via starting interrupt (turn 0)
        // They CAN still be deployed from hand during turn 1+ deploy phase
        if (context.getTurnNumber() <= 0) {
            java.util.Set<String> BANNED_STARTING_EFFECTS = new java.util.HashSet<>(java.util.Arrays.asList(
                "no escape", "no escape (v)",
                "coarse and rough and irritating",
                // V67p (Steve): Tentacle is not a useful starting interrupt — it's a
                // counter to Dianoga/garbage compactor scenarios, not a turn-0 setup card.
                // Picking it as starting effect wastes the turn-0 slot.
                "tentacle"
            ));

            // Check if any card in this selection is banned
            GameState startGameState = context.getGameState();
            List<String> startCardIds = context.getCardIds();
            boolean hasBannedCard = false;
            java.util.Map<String, Boolean> cardBanStatus = new java.util.HashMap<>();

            if (startGameState != null && startCardIds != null) {
                for (String cid : startCardIds) {
                    try {
                        PhysicalCard pc = startGameState.findCardById(Integer.parseInt(cid));
                        if (pc != null) {
                            String cardTitle = pc.getTitle();
                            if (cardTitle != null) {
                                String titleLower = cardTitle.toLowerCase(java.util.Locale.ROOT);
                                boolean banned = false;
                                for (String b : BANNED_STARTING_EFFECTS) {
                                    if (titleLower.contains(b)) {
                                        banned = true;
                                        break;
                                    }
                                }
                                cardBanStatus.put(cid, banned);
                                if (banned) {
                                    hasBannedCard = true;
                                    logger.warn("V21 STARTING BAN: '{}' is banned as starting effect", cardTitle);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            if (hasBannedCard) {
                List<EvaluatedAction> startBanActions = new ArrayList<>();
                for (String cid : startCardIds) {
                    Boolean banned = cardBanStatus.get(cid);
                    boolean isBanned = banned != null && banned;
                    EvaluatedAction action = new EvaluatedAction(
                        cid,
                        ActionType.UNKNOWN,
                        isBanned ? -500.0f : 100.0f,
                        isBanned ? "BANNED as starting effect" : "OK as starting effect"
                    );
                    startBanActions.add(action);
                }
                logger.warn("V21 STARTING BAN: Returning {} scored actions", startBanActions.size());
                return startBanActions;
            }
        }

        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<Boolean> selectable = context.getSelectable();

        logger.warn("🔍 cardIds: {} items", cardIds != null ? cardIds.size() : "null");
        logger.warn("🔍 blueprints: {} items", blueprints != null ? blueprints.size() : "null");
        logger.warn("🔍 selectable array: {} items -> {}",
            selectable != null ? selectable.size() : "null",
            selectable != null && selectable.size() <= 10 ? selectable : (selectable != null ? selectable.subList(0, Math.min(10, selectable.size())) + "..." : "null"));

        // Log min/max for selection
        int min = context.getMin();
        int max = context.getMax();
        boolean noPass = context.isNoPass();
        logger.warn("🔍 Selection min={}, max={}, noPass={}", min, max, noPass);

        // Log first few card IDs and blueprints for debugging
        if (cardIds != null && !cardIds.isEmpty()) {
            logger.warn("🔍 First 5 cardIds: {}", cardIds.subList(0, Math.min(5, cardIds.size())));
        }
        if (blueprints != null && !blueprints.isEmpty()) {
            logger.warn("🔍 First 5 blueprints: {}", blueprints.subList(0, Math.min(5, blueprints.size())));
        }
        // Log testingTexts (CARD TITLES from GEMP - most reliable!)
        List<String> testingTexts = context.getTestingTexts();
        if (testingTexts != null && !testingTexts.isEmpty()) {
            logger.warn("🔍 testingTexts (CARD TITLES!): {} items", testingTexts.size());
            logger.warn("🔍 First 5 testingTexts: {}", testingTexts.subList(0, Math.min(5, testingTexts.size())));
        } else {
            logger.warn("🔍 testingTexts: null or empty - card titles unavailable!");
        }
        // ========== END CRITICAL DEBUG LOGGING ==========

        logger.info("[CardSelectionEvaluator] Evaluating: {}",
            text != null && text.length() > 60 ? text.substring(0, 60) + "..." : text);

        // Log selectable info - CRITICAL for debugging GEMP rejection issues
        if (selectable != null && !selectable.isEmpty()) {
            int selectableCount = 0;
            for (Boolean s : selectable) {
                if (s != null && s) selectableCount++;
            }
            logger.info("[CardSelectionEvaluator] {} cards total, {} selectable",
                       cardIds != null ? cardIds.size() : 0, selectableCount);

            // If NOTHING is selectable, this is likely a "verify" decision or a bug
            if (selectableCount == 0 && cardIds != null && !cardIds.isEmpty()) {
                logger.warn("⚠️⚠️⚠️ ALL {} CARDS ARE NON-SELECTABLE! This may be a 'verify' decision or GEMP bug.", cardIds.size());
                logger.warn("    Decision contains 'verify': {}", textLower.contains("verify"));
                logger.warn("    Decision contains 'unsuccessful': {}", textLower.contains("unsuccessful"));

                // === V24.7: OPPONENT DECK INTEL — SCAN DESTINY VALUES ===
                // When verifying opponent's deck, scan all visible cards for destiny values.
                // This gives us real data for BattlePredictor instead of random 0-6 guesses.
                GameState peekGameState = context.getGameState();
                com.gempukku.swccgo.ai.models.rando.strategy.OpponentDeckTracker tracker =
                    context.getOpponentDeckTracker();
                if (peekGameState != null && tracker != null) {
                    try {
                        float[] destinyValues = new float[cardIds.size()];
                        int idx = 0;
                        for (String peekCardId : cardIds) {
                            try {
                                PhysicalCard peekCard = peekGameState.findCardById(Integer.parseInt(peekCardId));
                                if (peekCard != null && peekCard.getBlueprint() != null) {
                                    Float destiny = peekCard.getBlueprint().getDestiny();
                                    destinyValues[idx] = (destiny != null) ? destiny : -1.0f;
                                    if (destiny != null) {
                                        logger.info("V24.7 PEEK: {} — destiny {}", peekCard.getTitle(), destiny);
                                    }
                                } else {
                                    destinyValues[idx] = -1.0f;
                                }
                            } catch (NumberFormatException nfe) {
                                destinyValues[idx] = -1.0f;
                            }
                            idx++;
                        }
                        tracker.recordPeek(destinyValues, cardIds.size());
                        logger.warn("V24.7 OPPONENT INTEL: Scanned {} cards — average destiny: {}",
                            cardIds.size(), tracker.getOpponentDestinyAverage());
                    } catch (Exception e) {
                        logger.debug("V24.7: Error scanning opponent deck: {}", e.getMessage());
                    }
                }
            }
        }

        // For reserve deck selections, we may have blueprints but no cardIds
        if ((cardIds == null || cardIds.isEmpty()) &&
            (blueprints == null || blueprints.isEmpty())) {
            logger.warn("[CardSelectionEvaluator] No card IDs or blueprints in {} decision", context.getDecisionType());
            return new ArrayList<>();
        }

        // If we have blueprints but no cardIds, handle reserve deck selection
        if ((cardIds == null || cardIds.isEmpty()) && blueprints != null && !blueprints.isEmpty()) {
            logger.info("[CardSelectionEvaluator] Reserve deck selection with {} blueprints", blueprints.size());
            return evaluateReserveDeckSelection(context, textLower);
        }

        logger.debug("[CardSelectionEvaluator] {} cards to evaluate", cardIds.size());

        // Route to specific handlers based on decision text
        if (textLower.contains("choose card to set sabacc value")) {
            return evaluateSabaccSetValue(context);
        } else if (textLower.contains("choose") && textLower.contains("clone")) {
            return evaluateSabaccClone(context);
        } else if (textLower.contains("choose where to deploy")) {
            // V67v (Steve, 2026-05-03): Routing precedence bug. This branch caught
            // turn-0 starting-location decisions BEFORE V67r could route them to
            // evaluateStartingLocation. Result: all V67o/p/q/r + V29.14 Funeral Pyre +
            // V24.10 CC Exterior + V67q Sith logic was bypassed for the starting deploy.
            // Steve's symptom: 'Rando picked a Tatooine site as his Luke Saga starting
            // location instead of Endor: Funeral Pyre (V29.14 should give +1000).'
            if (context.getTurnNumber() <= 0) {
                logger.warn("V67v STARTING DEPLOY: turn 0 'where to deploy' → evaluateStartingLocation (was missed by precedence bug)");
                return evaluateStartingLocation(context);
            }
            return evaluateDeployLocation(context);
        } else if (textLower.contains("force to lose or") && textLower.contains("forfeit")) {
            // COMBINED decision: lose force OR forfeit card - MUST check before individual handlers!
            // Critical: Attrition MUST be satisfied by forfeiting, battle damage can be either
            return evaluateForceLossOrForfeit(context);
        } else if (textLower.contains("choose force to lose")) {
            return evaluateForceLoss(context);
        } else if (textLower.contains("choose a card from battle to forfeit") ||
                   textLower.contains("forfeit")) {
            return evaluateForfeit(context);
        } else if (textLower.contains("simultaneously deploy aboard")) {
            // Simultaneous pilot deployment - special handling
            return evaluateSimultaneousPilotSelection(context);
        } else if (textLower.contains("choose a pilot") ||
                   (textLower.contains("pilot") && (textLower.contains("choose") || textLower.contains("select"))) ||
                   (textLower.contains("matching") && textLower.contains("starship")
                        && !textLower.contains("into hand") && !textLower.contains("prison"))) {
            // V22.7: Broadened to catch AMSD pilot selection — GEMP text may say
            // "Choose a unique pilot character" which doesn't match "choose a pilot"
            // V22.7 ADJUSTED 2026-07-10 (AMN hang, replay 2jg1sj0l3qrlgy6a): the matching/starship
            // catch-all also matched "Choose a prison and a bounty hunter (may also choose a matching
            // weapon and/or starship)" — a take-INTO-HAND combination from Any Methods Necessary — and
            // routed it to pilot logic, which ignores selectable[] and answered a non-selectable card
            // (engine rejects, mediator swallows, game hangs). Exclude into-hand/prison texts; the
            // DecisionSafety SELECTABLE-CLAMP is the class-level backstop.
            return evaluatePilotSelection(context);
        } else if (textLower.contains("choose card to cancel")) {
            return evaluateCancelSelection(context);
        } else if (textLower.contains("move to,")
                   || textLower.contains("where to move")
                   || (textLower.contains("move") && textLower.contains("to")
                       && !textLower.contains("choose target")
                       && !textLower.contains("cardhint"))) {
            // V63 ROUTING FIX: "Choose card to move to, or click 'Done' to cancel"
            // is the DESTINATION-selection decision. It must route to
            // evaluateMoveDestination BEFORE the generic "click 'done' to cancel"
            // branch — otherwise move-destination decisions fall through to
            // evaluateTargetSelection (which scores them as "target opponent's
            // card" +50), bypassing V62 SPLIT SITE and V62 SPY DILUTION logic.
            //
            // V67d ADDITION: "Choose where to move <Luke> using landspeed" is
            // ALSO destination selection — the cardHint here is the CHARACTER
            // being moved, not the destination. The "where to move" prefix
            // distinguishes it from character-selection text "card to move to <X>".
            // FIXES awjc89tacm7cxvtv replay: Rando moved Luke STU↔STG repeatedly
            // because both options scored +120 (generic target +50 +20) instead
            // of running through evaluateMoveDestination's drain/BG-aware scoring.
            return evaluateMoveDestination(context);
        } else if (textLower.contains("choose target") ||
                   textLower.contains("click 'done' to cancel")) {
            // === V42: SHIELD CHECK — must come before other routing in this branch ===
            // K&D shield selection uses "Choose card, or click 'Done' to cancel" which
            // matches this branch. Check if all choices are shields FIRST.
            if (isShieldSelectionByContent(context)) {
                logger.warn("V42 SHIELD ROUTING FIX: 'click done to cancel' text but content is shields → evaluateShieldSelection");
                return evaluateShieldSelection(context);
            }
            // === V24.11: AMSD ROUTING — CHECK BEFORE evaluateTargetSelection ===
            // "Choose card from hand, or click 'Done' to cancel" matches this branch,
            // but when AMSD is active and we're picking characters in deploy phase,
            // this is actually an AMSD pilot selection. Route to evaluatePilotSelection
            // so Piett-only enforcement fires. Without this, Vader gets picked and
            // the AMSD action fails because Executor isn't his matching ship.
            if (context.getPhase() == Phase.DEPLOY) {
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle amsdOracle = context.getDeckOracle();
                if (amsdOracle != null && amsdOracle.isAnalyzed()) {
                    boolean amsdOnTable = amsdOracle.isCardInPlay("Alert My Star Destroyer")
                        || amsdOracle.isCardInPlay("Alert My Star Destroyer!")
                        || amsdOracle.isCardInPlay("Alert My Star Destroyer! (V)");
                    if (amsdOnTable) {
                        boolean hasCharacterChoices = false;
                        GameState amsdGs = context.getGameState();
                        if (amsdGs != null && context.getCardIds() != null) {
                            for (String cid : context.getCardIds()) {
                                try {
                                    PhysicalCard rc = amsdGs.findCardById(Integer.parseInt(cid));
                                    if (rc != null && rc.getBlueprint() != null &&
                                        rc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        hasCharacterChoices = true;
                                        break;
                                    }
                                } catch (Exception e) { /* skip */ }
                            }
                        }
                        if (hasCharacterChoices) {
                            logger.warn("V24.11 AMSD ROUTING FIX: 'click done to cancel' branch but AMSD active + deploy phase + characters → routing to evaluatePilotSelection!");
                            return evaluatePilotSelection(context);
                        }
                    }
                }
            }
            return evaluateTargetSelection(context);
        } else if (textLower.contains("move") && textLower.contains("to")) {
            // Move destination selection
            return evaluateMoveDestination(context);
        } else if (textLower.contains("transit") || textLower.contains("transport")) {
            // Transit/transport destination selection
            return evaluateMoveDestination(context);
        } else if (textLower.contains("starting interrupt")) {
            // V43: Route starting interrupt selection
            return evaluateStartingInterrupt(context);
        } else if (textLower.contains("starting location")) {
            return evaluateStartingLocation(context);
        } else if (context.getTurnNumber() <= 0
                   && textLower.contains("where to deploy")) {
            // V67r: At turn 0 (PLAY_STARTING_CARDS), the starting interrupt asks
            // "Choose where to deploy <card>" — NOT "starting location". Without
            // this routing, V67o/p/q never fire and Rando picks non-battleground
            // sites for Sith decks (Steve's Dooku deck bug, 2026-05-03).
            logger.warn("V67r STARTING DEPLOY: routing 'where to deploy' on turn 0 to evaluateStartingLocation");
            return evaluateStartingLocation(context);
        } else if (textLower.contains("site") && textLower.contains("deploy")
                   && (textLower.contains("choose") || textLower.contains("battleground"))) {
            // V26: Catch TDIGWATT objective "Choose Cloud City battleground site to deploy"
            // and similar site selection decisions. Route to starting location evaluator
            // which has exterior/interior preference logic for TDIGWATT.
            logger.warn("V26: Routing site deploy choice to evaluateStartingLocation: '{}'",
                context.getDecisionText() != null && context.getDecisionText().length() > 80
                    ? context.getDecisionText().substring(0, 80) : context.getDecisionText());
            return evaluateStartingLocation(context);
        } else if (textLower.contains("choose") && textLower.contains("location")) {
            return evaluateLocationSelection(context);
        } else if (textLower.contains("card to take into hand")) {
            return evaluateTakeIntoHand(context);
        } else if (textLower.contains("card to put on lost pile")) {
            return evaluateLostPileSelection(context);
        } else if (textLower.contains("defensive shield") ||
                   isShieldSelectionByContent(context)) {
            return evaluateShieldSelection(context);
        } else {
            // === V24.10: AMSD ROUTING CATCH ===
            // If AMSD is in play and we're choosing characters during deploy phase,
            // this is almost certainly an AMSD pilot selection that wasn't caught by
            // the regular pilot routing (decision text didn't contain "pilot").
            // Route to evaluatePilotSelection to get full Piett-only enforcement.
            if (context.getPhase() == Phase.DEPLOY) {
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle routeOracle = context.getDeckOracle();
                if (routeOracle != null && routeOracle.isAnalyzed()) {
                    boolean amsdOnTable = routeOracle.isCardInPlay("Alert My Star Destroyer")
                        || routeOracle.isCardInPlay("Alert My Star Destroyer!")
                        || routeOracle.isCardInPlay("Alert My Star Destroyer! (V)");
                    if (amsdOnTable) {
                        // Check if the choices include characters (i.e., pilot candidates)
                        boolean hasCharacterChoices = false;
                        GameState routeGs = context.getGameState();
                        if (routeGs != null && context.getCardIds() != null) {
                            for (String cid : context.getCardIds()) {
                                try {
                                    PhysicalCard rc = routeGs.findCardById(Integer.parseInt(cid));
                                    if (rc != null && rc.getBlueprint() != null &&
                                        rc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        hasCharacterChoices = true;
                                        break;
                                    }
                                } catch (Exception e) { /* skip */ }
                            }
                        }
                        if (hasCharacterChoices) {
                            logger.warn("V24.10 AMSD ROUTING CATCH: AMSD in play + deploy phase + character choices → routing to evaluatePilotSelection!");
                            return evaluatePilotSelection(context);
                        }
                    }
                }
            }
            // Unknown - create neutral scored actions
            return evaluateUnknown(context);
        }
    }

    /**
     * Sabacc value setting - random selection to break loops.
     */
    private List<EvaluatedAction> evaluateSabaccSetValue(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            // V24.5: No randomness — use deterministic score
            float sabaccScore = 0.0f;

            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                sabaccScore,
                "Set sabacc value (card " + cardId + ")"
            );
            action.addReasoning("Sabacc value (deterministic)", sabaccScore);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Sabacc clone - avoid cloning.
     */
    private List<EvaluatedAction> evaluateSabaccClone(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                VERY_BAD_DELTA,
                "Clone sabacc value"
            );
            action.addReasoning("Avoid cloning sabacc cards", VERY_BAD_DELTA);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose where to deploy - evaluate locations.
     *
     * CRITICAL RULES (ported from Python card_selection_evaluator.py lines 185-400):
     * 1. Starships should NEVER deploy to docking bays (0 power!)
     * 2. Starships without pilots (and no permanent pilot icon) are weak
     * 3. Always prefer space systems over docking bays for starships
     * 4. Vehicles need EXTERIOR ground locations
     * 5. Follow the deploy plan when available
     */
    private List<EvaluatedAction> evaluateDeployLocation(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();

        // =====================================================
        // Detect what type of card we're deploying
        // =====================================================
        boolean isStarship = false;
        boolean isVehicle = false;
        boolean isCharacter = false;
        boolean isWeapon = false;  // Weapon deployment (deploys ON a character)
        String deployingCardName = "card";

        // V29.3: Only use decision text for weapon detection ("as attached" is reliable).
        // Card type detection (character, starship, vehicle) is handled below by the
        // game state blueprint lookup — much more reliable than matching text keywords
        // like "alien" or "imperial" which appear in card names, not just type descriptions.
        String decisionText = context.getDecisionText() != null ? context.getDecisionText().toLowerCase() : "";

        if (decisionText.contains("as attached")) {
            isWeapon = true;
            deployingCardName = "weapon";
            logger.info("Detected WEAPON deployment (as attached)");
        }

        // =====================================================
        // Check deploy planner for target location
        // Extract the card being deployed from decision text HTML
        // Format: <div class='cardHint' value='8_35'>
        // =====================================================
        String plannedTargetId = null;
        String plannedTargetName = null;
        String deployingBlueprintId = extractBlueprintFromDecisionText(context.getDecisionText());

        // V29.3: BLUEPRINT-BASED CARD TYPE DETECTION
        // The decision text "Choose where to deploy •Lobot, Lando's Broker" does NOT contain
        // type keywords like "character", "alien", "droid". We need the card's actual blueprint.
        //
        // PRIMARY: Use gameState to find the card — the game engine already has all cards loaded
        //          with correct blueprints. Search hand, reserve deck, and stacked cards.
        // FALLBACK: Use the standalone FALLBACK_LIBRARY (which loads classes via reflection
        //           and may silently fail for some card sets).
        // LAST RESORT: If we're in a "Choose where to deploy" decision and nothing else matched,
        //              assume CHARACTER — the only other ground deploys are vehicles/weapons which
        //              always have distinctive keywords.
        if (deployingBlueprintId != null && !isWeapon && !isStarship && !isVehicle && !isCharacter) {
            CardCategory detectedCategory = null;
            String detectedName = null;
            String detectionMethod = null;

            // --- Method 1: Search gameState for the card by blueprint ID ---
            GameState gsForType = context.getGameState();
            String pidForType = context.getPlayerId();
            if (gsForType != null && pidForType != null) {
                try {
                    // Check hand first (most common for deploys)
                    for (PhysicalCard hc : gsForType.getHand(pidForType)) {
                        if (hc != null && hc.getBlueprint() != null) {
                            String hcBpId = hc.getBlueprintId(true);
                            if (deployingBlueprintId.equals(hcBpId)) {
                                detectedCategory = hc.getBlueprint().getCardCategory();
                                detectedName = hc.getTitle();
                                detectionMethod = "gameState.hand";
                                break;
                            }
                        }
                    }
                    // Check reserve deck (for "deploy from Reserve Deck" actions)
                    if (detectedCategory == null) {
                        for (PhysicalCard rc : gsForType.getCardPile(pidForType, com.gempukku.swccgo.common.Zone.RESERVE_DECK)) {
                            if (rc != null && rc.getBlueprint() != null) {
                                String rcBpId = rc.getBlueprintId(true);
                                if (deployingBlueprintId.equals(rcBpId)) {
                                    detectedCategory = rc.getBlueprint().getCardCategory();
                                    detectedName = rc.getTitle();
                                    detectionMethod = "gameState.reserveDeck";
                                    break;
                                }
                            }
                        }
                    }
                    // Check stacked cards (for cards deployed from under other cards, e.g. K&D shields)
                    if (detectedCategory == null) {
                        for (PhysicalCard sc : gsForType.getAllStackedCards()) {
                            if (sc != null && sc.getBlueprint() != null) {
                                String scBpId = sc.getBlueprintId(true);
                                if (deployingBlueprintId.equals(scBpId)) {
                                    detectedCategory = sc.getBlueprint().getCardCategory();
                                    detectedName = sc.getTitle();
                                    detectionMethod = "gameState.stacked";
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("V29.3 gameState card type lookup failed: {}", e.getMessage());
                }
            }

            // --- Method 2: FALLBACK_LIBRARY (standalone blueprint library, uses reflection) ---
            if (detectedCategory == null) {
                try {
                    SwccgCardBlueprint deployingBp = getBlueprintFromId(context, deployingBlueprintId);
                    if (deployingBp != null) {
                        detectedCategory = deployingBp.getCardCategory();
                        detectedName = deployingBp.getTitle();
                        detectionMethod = "FALLBACK_LIBRARY";
                    } else {
                        logger.warn("V29.3 FALLBACK_LIBRARY returned NULL for blueprint {}", deployingBlueprintId);
                    }
                } catch (Exception e) {
                    logger.warn("V29.3 FALLBACK_LIBRARY error for {}: {}", deployingBlueprintId, e.getMessage());
                }
            }

            // Apply detected category
            if (detectedCategory != null) {
                logger.warn("V29.3 CARD TYPE: {} ({}) is {} (via {})", detectedName, deployingBlueprintId, detectedCategory, detectionMethod);
                if (detectedCategory == CardCategory.CHARACTER) {
                    isCharacter = true;
                    deployingCardName = detectedName != null ? detectedName : "character";
                } else if (detectedCategory == CardCategory.STARSHIP) {
                    isStarship = true;
                    deployingCardName = detectedName != null ? detectedName : "starship";
                } else if (detectedCategory == CardCategory.VEHICLE) {
                    isVehicle = true;
                    deployingCardName = detectedName != null ? detectedName : "vehicle";
                } else if (detectedCategory == CardCategory.WEAPON) {
                    isWeapon = true;
                    deployingCardName = detectedName != null ? detectedName : "weapon";
                }
            } else {
                logger.warn("V29.3 CARD TYPE: ALL methods failed for blueprint {} — type unknown!", deployingBlueprintId);
            }
        }

        // V29.3 LAST RESORT: If we're in a "Choose where to deploy" decision and still no type,
        // assume CHARACTER. The only other ground deploys (vehicles, weapons) always have
        // distinctive keywords in the decision text.
        if (!isCharacter && !isStarship && !isVehicle && !isWeapon) {
            if (decisionText.contains("choose where to deploy") || decisionText.contains("choose location to deploy")) {
                if (!decisionText.contains("starship") && !decisionText.contains("capital ship")
                    && !decisionText.contains("vehicle") && !decisionText.contains("as attached")
                    && !decisionText.contains("effect") && !decisionText.contains("interrupt")) {
                    isCharacter = true;
                    deployingCardName = "character (V29.3 last-resort)";
                    logger.warn("V29.3 LAST RESORT: Assuming CHARACTER for deploy decision: {}",
                        context.getDecisionText() != null ? context.getDecisionText().substring(0, Math.min(100, context.getDecisionText().length())) : "?");
                }
            }
        }

        DeployPhasePlanner deployPhasePlanner = context.getDeployPhasePlanner();
        if (deployPhasePlanner != null) {
            DeploymentPlan currentPlan = deployPhasePlanner.getCurrentPlan();
            if (currentPlan != null && !currentPlan.getInstructions().isEmpty()) {
                // FIXED: Look up the instruction for the SPECIFIC card being deployed
                if (deployingBlueprintId != null) {
                    DeploymentInstruction matchingInstruction = currentPlan.getInstructionForCard(deployingBlueprintId);
                    if (matchingInstruction != null && matchingInstruction.getTargetLocationId() != null) {
                        plannedTargetId = matchingInstruction.getTargetLocationId();
                        plannedTargetName = matchingInstruction.getTargetLocationName();
                        logger.info("📋 Deploy plan says: {} ({}) -> {}",
                            matchingInstruction.getCardName(), deployingBlueprintId, plannedTargetName);
                    } else {
                        logger.info("📋 No matching instruction for blueprint {}", deployingBlueprintId);
                    }
                } else {
                    // Fallback: use first instruction if we can't determine the card
                    logger.warn("⚠️ Could not extract blueprint from decision text, using first instruction");
                    for (DeploymentInstruction instruction : currentPlan.getInstructions()) {
                        if (instruction.getTargetLocationId() != null) {
                            plannedTargetId = instruction.getTargetLocationId();
                            plannedTargetName = instruction.getTargetLocationName();
                            logger.info("📋 Deploy plan fallback: {} -> {}", deployingCardName, plannedTargetName);
                            break;
                        }
                    }
                }
            }
        }

        // V186 (Steve, 2026-06-23): I Want That Map starting LOCATION pick. This loop
        // resolves each candidate via findCardById(parseInt(cardId)) (~line 813), which
        // throws for ARBITRARY temp IDs ("temp0"...) — the form the objective's
        // "Choose [Episode VII] location to deploy" decision uses — so the normal +150
        // objective bonus (~line 1607) never fires and every candidate ties at the +50
        // base, making the pick arbitrary (the root cause of the wrong-location report).
        // Resolve temp-safely from the parallel blueprint / testing-text lists and steer
        // the pick to the Starkiller Base SYSTEM (208_51), whose once-per-turn [download]
        // fetches the SB battleground sites that feed the 2-battleground flip. Its sites
        // (208_52..55) are different blueprint IDs, so this names the download engine, not
        // a site.
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v186oa = context.getObjectiveAnalyzer();
        java.util.List<String> v186Ids = context.getCardIds();
        java.util.List<String> v186Bps = context.getBlueprints();
        java.util.List<String> v186Tts = context.getTestingTexts();
        // V186 CONSOLIDATED (2026-07-07): identity from ObjectiveAnalyzer.isWantThatMap().
        boolean v186IsWantThatMap = v186oa != null && v186oa.isAnalyzed() && v186oa.isWantThatMap();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Deploy to location " + cardId
            );

            // V186: temp-safe Starkiller Base SYSTEM preference (see note above the loop).
            // +400 over the +50 base is decisive vs the other [Episode VII] candidates (which
            // stay at +50 because their objective/battleground bonuses are also unreachable on
            // the temp-id path). Names ONLY the system (208_51 / title "Starkiller Base" with
            // no ":" site suffix), so its battleground sites are not picked here.
            // Gated to temp IDs (the ARBITRARY reserve-deck pick) so it does NOT fire for
            // later real-id "deploy where" decisions — otherwise it would over-prioritize
            // deploying onto the SYSTEM instead of the battleground SITES the flip needs.
            if (v186IsWantThatMap && cardId != null && cardId.startsWith("temp")) {
                int v186Idx = v186Ids != null ? v186Ids.indexOf(cardId) : -1;
                String v186Bp = (v186Bps != null && v186Idx >= 0 && v186Idx < v186Bps.size()) ? v186Bps.get(v186Idx) : null;
                String v186Tt = (v186Tts != null && v186Idx >= 0 && v186Idx < v186Tts.size()) ? v186Tts.get(v186Idx) : null;
                // V186 CONSOLIDATED (2026-07-07): system blueprint ids + title fragment come from
                // ObjectiveAnalyzer (was hardcoded "208_51"/"208_051"/"starkiller base" here).
                java.util.Set<String> v186SysIds = v186oa.getIwtmSystemBpIds();
                String v186SysFrag = v186oa.getIwtmSystemTitleFragment();
                boolean v186BpSystem = v186Bp != null && v186SysIds != null && v186SysIds.contains(v186Bp);
                boolean v186TtSystem = v186Tt != null && v186SysFrag != null
                        && v186Tt.toLowerCase(java.util.Locale.ROOT).contains(v186SysFrag)
                        && !v186Tt.contains(":");
                if (v186BpSystem || v186TtSystem) {
                    action.addReasoning("V186 STARKILLER BASE SYSTEM - download engine for the 2-battleground flip", 400.0f);
                    logger.warn("V186 STARKILLER SYSTEM: cardId={} bp={} title={} (+400)", cardId, v186Bp, v186Tt);
                }
            }

            // Try to get location info
            if (gameState != null) {
                try {
                    PhysicalCard location = gameState.findCardById(Integer.parseInt(cardId));
                    if (location != null) {
                        SwccgCardBlueprint blueprint = location.getBlueprint();
                        String title = location.getTitle();
                        String titleLower = title != null ? title.toLowerCase() : "";
                        action.setDisplayText("Deploy to " + (title != null ? title : "location"));

                        // === V166 (Steve, 2026-06): CONTEST THE OPPONENT'S DRAIN by deploying to it ===
                        // When the opponent out-drains us by net >= 2 (bonus-aware; verified to fire ~half
                        // the time in self-play), DEPLOY to their drain sites to create contested sites so
                        // V164a can battle and break the drain — instead of both sides parallel-draining for
                        // 20+ turns. Deploy (unlike move) can target the opponent's site directly, which is
                        // why the move-path version never fired. Prefer the SOFTEST site (fewest opponent
                        // cards = easiest to clear or spy-block). Weight is decisive over normal deploy
                        // scoring (planned target +200, uncontested +30) but stays under the -9999 hard
                        // blocks; V164a's own guards still stop a suicide battle once the site is contested.
                        if (game != null && playerId != null) {
                            try {
                                String v166Opp = gameState.getOpponent(playerId);
                                float v166TheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, location, v166Opp, false, false);
                                int v166OppDrain = (int) game.getModifiersQuerying()
                                    .getForceDrainAmount(gameState, location, v166Opp);
                                if (v166TheirPower > 0 && v166OppDrain > 0
                                        && computeNetDrainBalance(game, gameState, playerId) >= 2) {
                                    // V177 (Steve, 2026-06): SURVIVABILITY GATE. Replay aab2jiaa5sca:
                                    // V166 lured Wild Karrde to Tatooine to "contest a drain" with no
                                    // check it could hold the site — it couldn't, so it hyperspeed-moved
                                    // to Jakku next phase, wasting 1 Force. Only award the contest bonus
                                    // when our deploy can reach near-parity there: our power + this card +
                                    // the affordable wave >= their power - 2. Otherwise the contest is a
                                    // trap; skip it so a winnable/safe site wins the deploy instead.
                                    float v166OurPow = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, playerId, false, false);
                                    float v166ThisPow = 0f;
                                    SwccgCardBlueprint v166Bp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (v166Bp != null && v166Bp.hasPowerAttribute() && v166Bp.getPower() != null)
                                        v166ThisPow = v166Bp.getPower();
                                    // V177 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T2 Yoda solo into
                                    // Kylo+Mara+saber): the gate passed on OPTIMISTIC self math vs DISCOUNTED
                                    // enemy math — it counted a lightsaber-in-hand wave (+5) with ZERO affordable
                                    // character buddies, and used raw enemy power blind to their weapons (raw 10,
                                    // armed ~15). Two in-place corrections: (a) only count the wave when it has at
                                    // least one AFFORDABLE BUDDY (wave[1]>=1 — the same standard V172 uses);
                                    // (b) weapon-adjust their power (V29.7 heuristic: lightsaber +5, other +3,
                                    // attached or permanent). Boundary: T2 case now 0+3+0 vs (10+10)-2=18 → GATED
                                    // (was 8 vs 8 → "survivable" → +350 lure → Yoda died, 8 battle damage).
                                    float[] v166WaveArr = v173WaveProjection(gameState, playerId, deployingBlueprintId);
                                    float v166Wave = (v166WaveArr[1] >= 1f) ? v166WaveArr[0] : 0f;
                                    float v166OppWeapons = v177OppWeaponBonus(gameState, location, v166Opp);
                                    boolean v166Survivable = (v166OurPow + v166ThisPow + v166Wave)
                                        >= (v166TheirPower + v166OppWeapons) - 2f;
                                    if (v166Survivable) {
                                        int v166OppCards = 0;
                                        for (PhysicalCard c : gameState.getCardsAtLocation(location))
                                            if (c != null && v166Opp.equals(c.getOwner())) v166OppCards++;
                                        float v166 = 250.0f + Math.max(0f, 150f - (v166OppCards - 1) * 50f);
                                        action.addReasoning(String.format(
                                            "V166 CONTEST DRAIN: opponent out-draining (net>=2) — deploy to contest %s (their drain %d, %d opp cards)",
                                            title, v166OppDrain, v166OppCards), v166);
                                        logger.warn("V166 CONTEST DRAIN (deploy): target={} oppDrain={} oppCards={} oppPower={} -> +{}",
                                            title, v166OppDrain, v166OppCards, (int) v166TheirPower, (int) v166);
                                    } else {
                                        logger.warn("V177 V166 GATED: {} contest not survivable (proj {} vs {}) — deploy elsewhere",
                                            title, (int) (v166OurPow + v166ThisPow + v166Wave), (int) v166TheirPower);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V166 deploy error: {}", e.getMessage()); }
                        }

                        // === V169 (Steve, 2026-06): PROTECT ENDANGERED CHARACTERS — deploy buddies ===
                        // Replay lk6xgsokjcwrwxuu (Steve vs Rando), two fatal moves: (1) Asajj left
                        // solo at Guest Quarters with Luke AT her site — beaten 6v27 next turn;
                        // (2) Tyranus + Aurra on Hoth facing a 5-character strike team (16v37) while
                        // Rando deployed Savage + Nute to an OPEN Cloud City site. Steve: "he needed
                        // to deploy buddies to protect his characters" — EVEN into a losing battle.
                        // When our characters at this location are outpowered, deploying here gets a
                        // dominating bonus (+800..+1100): beats open-site totals (~315-600 observed),
                        // beats V166 contest (+250..400), loses only to the -9999 hard blocks.
                        if (game != null && playerId != null) {
                            try {
                                float v169Excess = v169OppPowerExcessAt(game, gameState, playerId, location);
                                if (v169Excess > 0) {
                                    // V172 (Steve, 2026-06): REINFORCEABILITY BRAKE. Unbraked, this
                                    // bonus was a corpse conveyor: each wave of "buddies" fed into a
                                    // site Steve's stack dominated, he initiated and wiped them, the
                                    // bonus re-fired (416x in one game), and the lost pile hit 30-to-0
                                    // before V67aw conceded. Only reinforce when reinforcement can
                                    // actually close the gap: this card + best remaining hand
                                    // character must bring the deficit within 4. Beyond that the site
                                    // is unsavable by deploys — the V169 RETREAT path (move phase)
                                    // handles it instead of feeding.
                                    // V173: reinforcement potential = this card + the WHOLE affordable
                                    // wave (every other hand character within the force budget, printed
                                    // deploy costs, plus weapon weights) — not just one buddy.
                                    float v172This = 0f;
                                    boolean v172SkippedSelf = false;
                                    for (PhysicalCard v172H : gameState.getHand(playerId)) {
                                        if (v172H != null && v172H.getBlueprint() != null
                                                && v172H.getBlueprint().getCardCategory() == CardCategory.CHARACTER
                                                && !v172SkippedSelf && deployingBlueprintId != null
                                                && deployingBlueprintId.equals(v172H.getBlueprintId(true))) {
                                            Float v172P = v172H.getBlueprint().hasPowerAttribute()
                                                ? v172H.getBlueprint().getPower() : null;
                                            v172This = v172P != null ? v172P : 0f;
                                            v172SkippedSelf = true;
                                        }
                                    }
                                    float[] v172WaveR = v173WaveProjection(gameState, playerId, deployingBlueprintId);
                                    float v172Wave = v172WaveR[0];
                                    if (v172This + v172Wave >= v169Excess - 4f) {
                                        float v169 = 800f + Math.min(300f, v169Excess * 30f);
                                        action.addReasoning(String.format(
                                            "V169 PROTECT: our characters at %s are outpowered by %.0f — deploy buddies to protect them (affordable reinforcement: +%.0f, reserves held: %.0f)",
                                            title, v169Excess, v172This + v172Wave, v172WaveR[2]), v169);
                                        logger.warn("V169 PROTECT (deploy): {} outpowered by {} -> +{} (wave={} reserved={})",
                                            title, (int) v169Excess, (int) v169, (int) v172Wave, (int) v172WaveR[2]);
                                    } else {
                                        logger.warn("V172 PROTECT GATED: {} outpowered by {} but only +{} affordable reinforcement (reserved={}) — unsavable by deploys, retreat instead",
                                            title, (int) v169Excess, (int) (v172This + v172Wave), (int) v172WaveR[2]);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V169 deploy error: {}", e.getMessage()); }
                        }

                        // === V170 (Steve, 2026-06): SPY -> BLOCK THEIR BEST DRAIN SITE ===
                        // Steve: "Spies cost much less to block a drain than deploying a bunch
                        // of characters to overpower opponent." When the card being deployed is
                        // a SPY, deploying it AT an opponent-occupied drain site is the cheap
                        // block (the V170 yes/no intercept in RandoCalAi answers the undercover
                        // prompt; undercover breaks their control -> drain stops). No power
                        // requirement — spies don't fight, undercover is safe. Scaled by the
                        // drain it denies, preferring their BIGGEST drain. Magnitude: beats
                        // V166 contest (+250..400) and open-site totals (~315-600) even after a
                        // V113 solo penalty (-300), but stays under V169 PROTECT (+800..1100) —
                        // endangered allies outrank a cheap block.
                        if (game != null && playerId != null && deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint v170Bp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v170Bp != null && v170Bp.hasKeyword(com.gempukku.swccgo.common.Keyword.SPY)) {
                                    String v170Opp = gameState.getOpponent(playerId);
                                    boolean v170OppHere = false;
                                    for (PhysicalCard v170C : gameState.getCardsAtLocation(location)) {
                                        if (v170C != null && v170Opp.equals(v170C.getOwner())) { v170OppHere = true; break; }
                                    }
                                    int v170Drain = (int) game.getModifiersQuerying()
                                        .getForceDrainAmount(gameState, location, v170Opp);
                                    if (v170OppHere && v170Drain >= 1) {
                                        float v170 = 600f + Math.min(300f, v170Drain * 75f);
                                        action.addReasoning(String.format(
                                            "V170 SPY BLOCK: deploy spy to %s — blocks opponent drain of %d (cheap denial)",
                                            title, v170Drain), v170);
                                        logger.warn("V170 SPY BLOCK: {} drain={} -> +{}", title, v170Drain, (int) v170);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V170 error: {}", e.getMessage()); }
                        }

                        // === V171 (Steve, 2026-06): DEPLOY TO CONTACT — don't deploy adjacent and march ===
                        // Replay 479h9miow1acggwb (Steve vs Rando): Rando repeatedly deployed
                        // Tyranus/Asajj/Savage to an EMPTY adjacent site (Guest Quarters/Beldon's, 340)
                        // and then landspeed-marched them into Steve's occupied site next phase —
                        // because the contested site ate first-mover penalties (V113 SOLO -300,
                        // V29.5 BUDDY -100, V136 danger ~-300) that V166's +400 couldn't beat (-200).
                        // Steve: "deployed and moved guys in front of my characters instead of just
                        // deploying to my occupied location. This is a waste of force."
                        // It's worse than force waste: SWCCG turn order is Deploy -> BATTLE -> Move,
                        // so arriving by move forfeits battle initiative every time (Steve
                        // out-initiated ~7-2). When a deploy WAVE is coming this same phase (another
                        // character in hand + force to land it), the first mover's loneliness is
                        // temporary — offset the first-mover penalties so the wave STARTS at the
                        // contested site. +600 flips the observed case (-200 -> +400 > empty 340);
                        // genuinely suicidal sites keep their extra danger terms (stack past -600)
                        // and still lose. Wave gate keeps solo-with-no-backup deploys penalized.
                        if (game != null && playerId != null) {
                            try {
                                String v171Opp = gameState.getOpponent(playerId);
                                boolean v171OppHere = false;
                                for (PhysicalCard v171C : gameState.getCardsAtLocation(location)) {
                                    if (v171C != null && v171Opp.equals(v171C.getOwner()) && !v171C.isUndercover()) {
                                        v171OppHere = true; break;
                                    }
                                }
                                // V171/V172 ADJUSTED 2026-07-11c (Codex audit H3: V171 fired on the
                                // STARSHIP First Light, borrowing hand-character wave power, +600 into
                                // Falcon+Han): contact steers are CHARACTER logic only.
                                SwccgCardBlueprint v171DeployBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v171DeployBp == null
                                        || v171DeployBp.getCardCategory() != CardCategory.CHARACTER) {
                                    v171OppHere = false;
                                }
                                if (v171OppHere) {
                                    // Wave check: at least one MORE deployable character in hand
                                    // beyond the one being deployed, and force to plausibly land it.
                                    int v171HandChars = 0;
                                    float v171ThisPower = 0f;
                                    float v171MaxHandPower = 0f;  // V171 ADJUSTED 2026-07-10b: biggest body for hit discount
                                    String v171ThisBp = deployingBlueprintId;
                                    boolean v171SkippedSelf = false;
                                    for (PhysicalCard v171H : gameState.getHand(playerId)) {
                                        if (v171H != null && v171H.getBlueprint() != null
                                                && v171H.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            v171HandChars++;
                                            Float v171HP = v171H.getBlueprint().hasPowerAttribute()
                                                ? v171H.getBlueprint().getPower() : null;
                                            if (v171HP != null && v171HP > v171MaxHandPower) v171MaxHandPower = v171HP;
                                            if (!v171SkippedSelf && v171ThisBp != null
                                                    && v171ThisBp.equals(v171H.getBlueprintId(true))) {
                                                v171ThisPower = v171HP != null ? v171HP : 0f;
                                                v171SkippedSelf = true;
                                            }
                                        }
                                    }
                                    // V172 (Steve, 2026-06): WINNABILITY GATE. V171 without this fed
                                    // characters piecemeal into Steve's superior stacks — he initiated
                                    // every battle and wiped each installment (lost pile 30-to-0,
                                    // V67aw conceded two games running). Only walk in the front door
                                    // when the projected wave reaches near-parity (their power - 2).
                                    // V173: the projection is the WHOLE affordable wave (all hand
                                    // characters within the force budget + weapon weights).
                                    // V174: the budget reserves maintenance upkeep (table + wave) and
                                    // 1-2 force for battle interrupts FIRST; the old flat force>=4
                                    // check is replaced by "at least one buddy is genuinely
                                    // affordable after reserves".
                                    float v171OurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, playerId, false, false);
                                    float v171TheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, v171Opp, false, false);
                                    float[] v171WaveR = v173WaveProjection(gameState, playerId, deployingBlueprintId);
                                    float v171Wave = v171WaveR[0];
                                    boolean v171WaveAffordable = v171WaveR[1] >= 1f;
                                    float v171Projected = v171OurPower + v171ThisPower + v171Wave;
                                    // V171 ADJUSTED 2026-07-10b (replay f27ws5lgy0g58k5p, T4 Savage+Nute →
                                    // Carbonite Chamber suicide): the projection was whole-hand optimistic vs
                                    // RAW enemy power — same hole wave 1 closed in V166. (a) weapon-adjust
                                    // their power (v177OppWeaponBonus); (b) discount the projection by expected
                                    // HITS: each armed enemy character deletes ~one of our bodies pre-destiny
                                    // (min(armedOpps, our wave bodies) × our biggest body). Boundary: T4 case
                                    // 16−8=8 vs (10+3)−2=11 → GATED (was 16 vs 8 → +600 lure → board wipe).
                                    float v171OppWeap = v177OppWeaponBonus(gameState, location, v171Opp);
                                    int v171ArmedOpps = 0;
                                    try {
                                        for (PhysicalCard v171E : gameState.getCardsAtLocation(location)) {
                                            if (v171E == null || v171E.getBlueprint() == null) continue;
                                            if (v171E.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                            if (!v171Opp.equals(v171E.getOwner())) continue;
                                            boolean v171EA = false;
                                            java.util.List<PhysicalCard> v171Atts = gameState.getAttachedCards(v171E);
                                            if (v171Atts != null) {
                                                for (PhysicalCard att : v171Atts) {
                                                    if (att != null && att.getBlueprint() != null
                                                            && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) { v171EA = true; break; }
                                                }
                                            }
                                            String v171EG = v171E.getBlueprint().getGameText();
                                            if (!v171EA && v171EG != null
                                                    && v171EG.toLowerCase(Locale.ROOT).contains("permanent weapon")) v171EA = true;
                                            if (v171EA) v171ArmedOpps++;
                                        }
                                    } catch (Exception e) { /* 0 armed */ }
                                    float v171TheirEff = v171TheirPower + v171OppWeap;
                                    float v171Biggest = Math.max(v171ThisPower, v171MaxHandPower);
                                    float v171HitDiscount = Math.min(v171ArmedOpps, (int) v171WaveR[1] + 1) * v171Biggest;
                                    // V172 SOLO DOMINANCE (Steve ruling 2026-07-11, replay f27ws5lgy0g58k5p T2:
                                    // Tyranus power 8 refused to contest a LONE Leia power 3 for lack of hand
                                    // buddies): "Rando should be taking every opportunity to overpower my
                                    // underpowered solo or low power sites — this should override other logic."
                                    // When THIS deploy alone (+ our power already there) reaches 2× their
                                    // weapon-adjusted power, the buddy/wave requirement is waived. Objective
                                    // hold rules are untouched (they score their own sites; additive).
                                    boolean v172SoloDominant = v171ThisPower > 0 && v171TheirEff > 0
                                            && (v171OurPower + v171ThisPower) >= 2f * v171TheirEff;
                                    if (v172SoloDominant) {
                                        action.addReasoning(String.format(
                                            "V172 SOLO DOMINANCE: %s — this body alone overpowers them (%.0f vs %.0f eff, 2x) — deploy and battle",
                                            title, v171OurPower + v171ThisPower, v171TheirEff), 600.0f);
                                        logger.warn("V172 SOLO DOMINANCE: {} ({}+{} vs eff {}) -> +600 (buddy gate waived, Steve 2026-07-11)",
                                            title, (int) v171OurPower, (int) v171ThisPower, (int) v171TheirEff);
                                    } else if (v171HandChars >= 2 && v171WaveAffordable
                                            && (v171Projected - v171HitDiscount) >= v171TheirEff - 2f) {
                                        action.addReasoning(String.format(
                                            "V171 DEPLOY TO CONTACT: %s opponent-occupied, affordable wave projects %.0f (hit-adj %.0f) vs %.0f eff (reserves held: %.0f) — deploy directly, battle THIS turn",
                                            title, v171Projected, v171Projected - v171HitDiscount, v171TheirEff, v171WaveR[2]), 600.0f);
                                        logger.warn("V171 DEPLOY TO CONTACT: {} (handChars={} projected={} hitDisc={} [wave={} buddies={} reserved={}] theirsEff={}) -> +600",
                                            title, v171HandChars, (int) v171Projected, (int) v171HitDiscount,
                                            (int) v171Wave, (int) v171WaveR[1], (int) v171WaveR[2], (int) v171TheirEff);
                                    } else if (v171HandChars >= 2) {
                                        logger.warn("V172 CONTACT GATED: {} projected {} hitDisc={} [wave={} buddies={} reserved={}] vs eff {} — can't match their stack (or wave unaffordable after reserves), assemble adjacent instead",
                                            title, (int) v171Projected, (int) v171HitDiscount, (int) v171Wave,
                                            (int) v171WaveR[1], (int) v171WaveR[2], (int) v171TheirEff);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V171 error: {}", e.getMessage()); }
                        }

                        // === V64 MAPUZO JEDI-ONLY RULE ===
                        // On Hidden Path, only Jedi Survivors can transit off Mapuzo via the
                        // Underground Corridor game text. Non-Jedi characters deployed to any
                        // Mapuzo location get STUCK there — they can't follow the Jedi out to
                        // support them at battleground sites. Block non-Jedi character deploys
                        // to Mapuzo UNLESS the opponent is actively threatening Mapuzo with a
                        // drain or presence (in which case we need defenders).
                        // Steve's feedback: "The jedi are the only ones that can move off of
                        // Mapuzo, so deploying any other character except the fallen order
                        // jedi will result in trapping those characters on Mapuzo."
                        if (isCharacter && titleLower.contains("mapuzo")
                            && game != null && playerId != null) {
                            // Check if opponent is present at Mapuzo — defenders needed
                            String v64Opp = gameState.getOpponent(playerId);
                            float oppPowerAtMapuzo = 0;
                            try {
                                oppPowerAtMapuzo = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, location, v64Opp, false, false);
                            } catch (Exception e) { /* ignore */ }

                            // V67b: Check if the deploying card is a TRUE Jedi Survivor.
                            // Drop the previous persona-name fallback (which incorrectly
                            // matched "Ahsoka Tano With Lightsabers", "Obi-Wan With Lightsaber",
                            // "Luke With Lightsaber", etc. — those are Jedi but NOT Jedi
                            // Survivors and CAN'T transit off Mapuzo via Underground Corridor).
                            // Authoritative test: game text contains the literal phrase
                            // "Jedi Survivor" (the keyword that lets Underground Corridor's
                            // transit action target the card).
                            // FIXES xxhj3qwhxzmhrdym replay: Ahsoka Tano With Lightsabers
                            // deployed to Mapuzo: Mining Village and got stuck.
                            boolean isJediSurvivor = false;
                            if (deployingBlueprintId != null) {
                                try {
                                    SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (deployBp != null) {
                                        String gt = deployBp.getGameText();
                                        if (gt != null && gt.toLowerCase(java.util.Locale.ROOT).contains("jedi survivor")) {
                                            isJediSurvivor = true;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }

                            if (!isJediSurvivor) {
                                if (oppPowerAtMapuzo > 0) {
                                    // Opponent is attacking/draining Mapuzo — defenders welcome
                                    action.addReasoning(
                                        "V64 MAPUZO DEFENSE: Opponent at " + title
                                            + " (power " + (int)oppPowerAtMapuzo
                                            + ") — non-Jedi defender OK here",
                                        30.0f);
                                    logger.info("V64 MAPUZO DEFENSE: {} needs defender vs opponent power {} (+30)",
                                        title, (int)oppPowerAtMapuzo);
                                } else {
                                    // Non-Jedi to empty Mapuzo = trapped forever. Hard block.
                                    action.addReasoning(
                                        "V64 MAPUZO TRAP: Non-Jedi character at " + title
                                            + " will be STUCK — only Jedi Survivors transit off Mapuzo!",
                                        -1500.0f);
                                    logger.warn("V64 MAPUZO TRAP: Non-Jedi deploy to empty {} BLOCKED (-1500)", title);
                                }
                            }
                        }

                        // =====================================================
                        // FOLLOW THE DEPLOY PLAN!
                        // =====================================================
                        if (plannedTargetId != null) {
                            if (cardId.equals(plannedTargetId)) {
                                action.addReasoning("PLANNED TARGET: " + plannedTargetName, 200.0f);
                                logger.info("✅ {} is the PLANNED target (+200)", title);
                            } else {
                                action.addReasoning("Not planned target (want " + plannedTargetName + ")", -100.0f);
                            }
                        }

                        // =====================================================
                        // V24.14B: EARLY SPY DETECTION (UNIVERSAL)
                        // Check the deploying card's blueprint game text for "undercover".
                        // This catches ALL undercover spy cards, not just hardcoded names.
                        // Sets earlySpyDetected flag — deeper spy scoring handles location logic
                        // (including allowing spy at CC sites where OPPONENT has presence).
                        // =====================================================
                        boolean earlySpyDetected = false;
                        // Primary: Check deploying card's blueprint game text for "undercover"
                        if (deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint deployingBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (deployingBp != null) {
                                    String gameTextCheck = deployingBp.getGameText();
                                    if (gameTextCheck != null && gameTextCheck.toLowerCase(java.util.Locale.ROOT).contains("undercover")) {
                                        earlySpyDetected = true;
                                        logger.warn("V24.14B SPY DETECT: Blueprint game text contains 'undercover' — spy deploy!");
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V24.14B: Error checking deploying blueprint: {}", e.getMessage());
                            }
                        }
                        // Fallback: Check decision text for spy-related keywords
                        if (!earlySpyDetected) {
                            if (decisionText.contains("undercover") || decisionText.contains("as a spy")) {
                                earlySpyDetected = true;
                                logger.warn("V24.14B SPY DETECT: Decision text contains spy keyword — spy deploy!");
                            }
                        }

                        // =====================================================
                        // CRITICAL: Check if target is a STARSHIP
                        // V29: Characters deploying ABOARD ships as pilot/passenger is GOOD
                        //      (especially admirals on Executor). But ships deploying INTO
                        //      cargo bays of other ships = 0 power, which is terrible.
                        // =====================================================
                        if (blueprint != null && blueprint.getCardCategory() == CardCategory.STARSHIP) {
                            if (isCharacter) {
                                // V29: CHARACTER deploying ABOARD a ship — this is pilot/passenger deploy!
                                // If character's game text mentions a UNIQUE starship name, they should
                                // deploy aboard that ship to activate their abilities.
                                String shipTitle = titleLower;
                                boolean isExecutor = shipTitle.contains("executor");

                                // Check character's game text for any unique ship name
                                String charGameText = "";
                                String matchedShipName = null;
                                if (deployingBlueprintId != null) {
                                    try {
                                        SwccgCardBlueprint charBp = getBlueprintFromId(context, deployingBlueprintId);
                                        if (charBp != null) {
                                            charGameText = charBp.getGameText() != null ? charBp.getGameText().toLowerCase(java.util.Locale.ROOT) : "";
                                            for (String shipName : UNIQUE_SHIP_NAMES) {
                                                if (gameTextContainsShipName(charGameText, shipName)) {
                                                    matchedShipName = shipName;
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception e) { /* ignore */ }
                                }

                                // Check if THIS ship matches the referenced ship name
                                boolean gameTextReferencesThisShip = false;
                                if (matchedShipName != null && shipTitle.contains(matchedShipName)) {
                                    gameTextReferencesThisShip = true;
                                }
                                // Also match generic types: "capital starship" matches any capital ship,
                                // "star destroyer" matches any SD
                                if (matchedShipName != null && !gameTextReferencesThisShip) {
                                    if (matchedShipName.equals("capital starship") || matchedShipName.equals("star destroyer")
                                        || matchedShipName.equals("super star destroyer")) {
                                        // Generic type — matches this ship if it's a capital starship
                                        com.gempukku.swccgo.common.CardSubtype shipSubtype = blueprint.getCardSubtype();
                                        if (shipSubtype == com.gempukku.swccgo.common.CardSubtype.CAPITAL) {
                                            gameTextReferencesThisShip = true;
                                        }
                                    }
                                }

                                if (gameTextReferencesThisShip) {
                                    // Character's game text references THIS ship — MAXIMUM bonus!
                                    float bonus = 600.0f;
                                    if (charGameText.contains("adds 1 to force drain") || charGameText.contains("add 1 to force drain")) {
                                        bonus = 650.0f; // Force drain bonus makes this even more valuable
                                    }
                                    action.addReasoning("V29 SHIP-REF: Game text mentions " + matchedShipName
                                        + " — abilities activate aboard this ship!", bonus);
                                    logger.warn("V29 SHIP-REF: {} references '{}' — deploying aboard {} (+{})",
                                        deployingCardName, matchedShipName, title, (int)bonus);
                                } else if (matchedShipName != null) {
                                    // Game text references a DIFFERENT ship — mild bonus for any ship boarding
                                    action.addReasoning("V29 ABOARD SHIP: Game text references " + matchedShipName
                                        + " (not this ship) — mild bonus for ship boarding", 50.0f);
                                    logger.info("V29 ABOARD: {} references '{}' but boarding {} instead (+50)",
                                        deployingCardName, matchedShipName, title);
                                } else if (isExecutor) {
                                    // No ship reference in game text but Executor is always good for characters
                                    action.addReasoning("V29 CHARACTER ABOARD EXECUTOR: Adds ability/power to flagship", 100.0f);
                                    logger.info("V29 ABOARD: {} boarding Executor (+100)", deployingCardName);
                                } else {
                                    // Other ship, no game text match — basic pilot/passenger bonus
                                    action.addReasoning("V29 CHARACTER ABOARD SHIP: Pilot/passenger deploy", 50.0f);
                                    logger.info("V29 ABOARD: {} boarding {} (+50)", deployingCardName, title);
                                }
                                // Continue evaluating — don't skip like cargo bay does
                            } else {
                                // Non-character (ship) deploying INTO another ship's cargo bay = 0 power
                                action.addReasoning("⚠️ DEPLOY TO CARGO BAY = 0 POWER!", -300.0f);
                                logger.warn("⚠️ BLOCKING deploy of {} into cargo bay of {} - ships in cargo contribute 0 power!",
                                    deployingCardName, title);
                                actions.add(action);
                                continue;
                            }
                        }

                        // =====================================================
                        // CRITICAL: WEAPON DEPLOYMENT - check if target already has weapon
                        // Don't deploy a second weapon on a character that already has one!
                        // =====================================================
                        if (isWeapon && blueprint != null && blueprint.getCardCategory() == CardCategory.CHARACTER) {
                            PhysicalCard targetCharacter = location;  // 'location' is actually the target character
                            boolean alreadyHasWeapon = false;
                            String existingWeaponName = null;

                            // Check cards attached to this character
                            List<PhysicalCard> attachedCards = gameState.getAttachedCards(targetCharacter);
                            if (attachedCards != null) {
                                for (PhysicalCard attached : attachedCards) {
                                    if (attached != null && attached.getBlueprint() != null) {
                                        CardCategory attachedCategory = attached.getBlueprint().getCardCategory();
                                        if (attachedCategory == CardCategory.WEAPON) {
                                            alreadyHasWeapon = true;
                                            existingWeaponName = attached.getTitle();
                                            break;
                                        }
                                    }
                                }
                            }

                            if (alreadyHasWeapon) {
                                // V25: HARD BLOCK deploying second weapon — characters can only use one!
                                // Previous -200 was too weak and got overridden by lightsaber priority.
                                action.addReasoning("⚠️ CHARACTER ALREADY HAS WEAPON: " + existingWeaponName + " — CANNOT USE TWO!", -9999.0f);
                                logger.warn("⚠️ V25 HARD BLOCK: {} already has weapon '{}' - NEVER deploy second weapon!",
                                    title, existingWeaponName);
                            } else {
                                // Good target - character has no weapon
                                action.addReasoning("Character needs weapon", 20.0f);
                            }
                        }

                        // =====================================================
                        // V25: HUNT DOWN V — LIGHTSABER DEPLOY PRIORITY
                        // Lightsabers are critical for the Hunt Down deck engine.
                        // Boost any card with "lightsaber" in the title when deploying.
                        // BUT: Never deploy a second lightsaber on same character!
                        // =====================================================
                        if (deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint lsDeployBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (lsDeployBp != null && lsDeployBp.getTitle() != null) {
                                    String lsDeployTitle = lsDeployBp.getTitle().toLowerCase(java.util.Locale.ROOT);
                                    if (lsDeployTitle.contains("lightsaber")) {
                                        // V25: Check if target character already has a lightsaber/weapon
                                        boolean targetHasLightsaber = false;
                                        if (blueprint != null && blueprint.getCardCategory() == CardCategory.CHARACTER) {
                                            PhysicalCard targetChar = location;
                                            List<PhysicalCard> targetAttached = gameState.getAttachedCards(targetChar);
                                            if (targetAttached != null) {
                                                for (PhysicalCard att : targetAttached) {
                                                    if (att != null && att.getBlueprint() != null) {
                                                        CardCategory attCat = att.getBlueprint().getCardCategory();
                                                        String attTitle = att.getTitle();
                                                        if (attCat == CardCategory.WEAPON ||
                                                            (attTitle != null && attTitle.toLowerCase(java.util.Locale.ROOT).contains("lightsaber"))) {
                                                            targetHasLightsaber = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (targetHasLightsaber) {
                                            action.addReasoning("V25 HUNT DOWN: Target ALREADY HAS lightsaber — NEVER deploy second!", -9999.0f);
                                            logger.warn("V25 HUNT DOWN: BLOCKED second lightsaber on {} — can only use one!", title);
                                        } else {
                                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer lsDeployOA =
                                                context.getObjectiveAnalyzer();
                                            if (lsDeployOA != null && lsDeployOA.isAnalyzed() && lsDeployOA.isHuntDownV()) {
                                                action.addReasoning("V25 HUNT DOWN: DEPLOYING LIGHTSABER — deck engine critical!", 150.0f);
                                                logger.warn("V25 HUNT DOWN: Lightsaber '{}' deploying — PRIORITY (+150)", lsDeployBp.getTitle());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V25 HUNT DOWN: Error checking lightsaber deploy: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // CRITICAL: Detect location type
                        // =====================================================
                        boolean isDockingBay = titleLower.contains("docking bay") || titleLower.contains("landing platform");
                        boolean isSpaceSystem = false;
                        boolean isGroundSite = false;

                        if (blueprint != null) {
                            com.gempukku.swccgo.common.CardSubtype subtype = blueprint.getCardSubtype();
                            isSpaceSystem = (subtype == com.gempukku.swccgo.common.CardSubtype.SYSTEM);
                            isGroundSite = (subtype == com.gempukku.swccgo.common.CardSubtype.SITE) && !isDockingBay;
                        }

                        // =====================================================
                        // V29: SHIP-REFERENCING CHARACTERS ON GROUND — PENALIZE
                        // If a character's game text mentions a unique starship name,
                        // they should be aboard that ship, not on a ground site.
                        // =====================================================
                        if (isCharacter && isGroundSite && deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint charBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (charBp != null) {
                                    String gt = charBp.getGameText() != null ? charBp.getGameText().toLowerCase(java.util.Locale.ROOT) : "";
                                    for (String shipName : UNIQUE_SHIP_NAMES) {
                                        if (gameTextContainsShipName(gt, shipName)) {
                                            action.addReasoning("V29 SHIP CHARACTER ON GROUND: Game text mentions "
                                                + shipName + " — should deploy to space!", -200.0f);
                                            logger.warn("V29 GROUND PENALTY: {} mentions '{}' but deploying to ground {} (-200)",
                                                deployingCardName, shipName, title);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V29 SHIP-REF GROUND: Error: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // CRITICAL: Starships at docking bays have 0 power!
                        // Ported from Python card_selection_evaluator.py lines 267-283
                        // =====================================================
                        if (isStarship) {
                            // V190 widening (Steve, 2026-07-04): "Only deploy starships to
                            // systems." The -1500 now covers ALL sites, not just title-matched
                            // docking bays (isGroundSite = SITE && !isDockingBay, so the union
                            // is every site; a null-blueprint docking bay still matches by
                            // title). Behavior change: non-docking-bay sites go from the old
                            // -10 "unusual" nudge (branch commented out below) to -1500.
                            // Sectors deliberately unpenalized pending Steve's ruling.
                            if (isDockingBay || isGroundSite) {
                                // NEVER deploy starships to docking bays!
                                // 2026-06-03 MAGNITUDE BUMP (Steve, Mustafar replay): the
                                // previous VERY_BAD_DELTA (-150) was the only block protecting
                                // ship-deploys from landing at a docking bay (0 power = free
                                // kill). Replay: Rando used Mustafar: Private Platform docking
                                // bay's "Deploy starfighter with 'Vader' in title here" ability,
                                // outer action scored +1530 because V67ai mis-applied +1400 (now
                                // fixed via stricter bare-deploy gate). Even so the -150 here
                                // was too weak to be a clean second line of defense — sub-decision
                                // totals were still about -100 per docking bay site, which beat
                                // many alternative deploys and let the ship land at 0 power.
                                // Bump to -1500: dominates ANY positive deploy-site score in
                                // CardSelectionEvaluator, so when a ship-deploy sub-decision
                                // resolves with only docking bay options, every option scores
                                // ~-1450, Rando picks the Done/cancel sub. The cancel-loop
                                // detector then blocks the outer action after 3 retries, and
                                // Rando stops invoking the docking bay's game text.
                                action.addReasoning("⚠️ STARSHIP TO SITE = 0 POWER! (V190: ships deploy to systems)", -1500.0f);
                                logger.warn("⚠️ V190: {} would have 0 power at site {} → -1500 (widened 2026-07-04 from docking-bays-only)",
                                    deployingCardName, title);
                            } else if (isSpaceSystem) {
                                // Space system - starship has power here (if piloted)
                                // BUT check if we'd be at a power disadvantage!
                                if (game != null) {
                                    try {
                                        float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            game.getGameState(), location, playerId, false, false);
                                        String opponent = game.getOpponent(playerId);
                                        float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            game.getGameState(), location, opponent, false, false);

                                        if (theirPower > 0) {
                                            // Contested space location - check power differential
                                            // Get ship's power from blueprint
                                            int shipPower = 0;
                                            SwccgCardBlueprint deployingBlueprint = getBlueprintFromId(context, deployingBlueprintId);
                                            if (deployingBlueprint != null && deployingBlueprint.hasPowerAttribute()) {
                                                Float power = deployingBlueprint.getPower();
                                                shipPower = power != null ? power.intValue() : 0;
                                            }

                                            float projectedPower = ourPower + shipPower;
                                            if (projectedPower < theirPower) {
                                                // We'd still be losing after deployment!
                                                action.addReasoning(String.format(
                                                    "⚠️ SPACE POWER DISADVANTAGE: %.0f vs %.0f after deploy",
                                                    projectedPower, theirPower), -80.0f);
                                                logger.warn("⚠️ Deploying {} to {} would leave us at power disadvantage ({} vs {})",
                                                    deployingCardName, title, (int)projectedPower, (int)theirPower);
                                            } else if (projectedPower >= theirPower + 3) {
                                                // Good advantage
                                                action.addReasoning(String.format(
                                                    "Good space position: %.0f vs %.0f after deploy",
                                                    projectedPower, theirPower), 30.0f);
                                            } else {
                                                // Close fight
                                                action.addReasoning(String.format(
                                                    "Close space fight: %.0f vs %.0f after deploy",
                                                    projectedPower, theirPower), 10.0f);
                                            }
                                        } else {
                                            // Uncontested - good target
                                            action.addReasoning("Uncontested space system", 30.0f);
                                        }
                                    } catch (Exception e) {
                                        // Fallback to basic bonus if we can't check power
                                        action.addReasoning("Starship to space system", GOOD_DELTA * 2);
                                        logger.debug("Could not check power at {}: {}", title, e.getMessage());
                                    }
                                } else {
                                    action.addReasoning("Starship to space system", GOOD_DELTA * 2);
                                }
                            }
                            // V190 (2026-07-04): dead branch commented out — isGroundSite now
                            // routes into the -1500 site block above (feedback_comment_out_old_rules).
                            // } else if (isGroundSite) {
                            //     // Ground location - starship can't deploy here normally
                            //     action.addReasoning("STARSHIP TO GROUND - unusual!", BAD_DELTA);
                            // }
                        }

                        // =====================================================
                        // CRITICAL: Vehicles need EXTERIOR ground locations
                        // Ported from Python card_selection_evaluator.py lines 287-302
                        // =====================================================
                        if (isVehicle) {
                            if (isSpaceSystem) {
                                // Space location - vehicles can't deploy here
                                action.addReasoning("VEHICLE TO SPACE - invalid!", VERY_BAD_DELTA);
                            } else if (isGroundSite || isDockingBay) {
                                // Check if location has exterior icon
                                boolean hasExterior = true;  // Default to true if unknown
                                boolean hasInteriorOnly = false;

                                if (blueprint != null) {
                                    // Use hasIcon() method instead of getIcons()
                                    boolean foundExterior = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.EXTERIOR_SITE);
                                    boolean foundInterior = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.INTERIOR_SITE);
                                    hasExterior = foundExterior;
                                    hasInteriorOnly = foundInterior && !foundExterior;
                                }

                                if (hasInteriorOnly) {
                                    action.addReasoning("VEHICLE TO INTERIOR-ONLY - can't deploy!", VERY_BAD_DELTA);
                                    logger.warn("⚠️ Vehicle cannot deploy to interior site {}", title);
                                } else if (hasExterior) {
                                    action.addReasoning("Vehicle to exterior ground - good", GOOD_DELTA);
                                }
                            }
                        }

                        // =====================================================
                        // V24.14B: WEAPON CHARACTERS/VEHICLES TO SPACE — PENALIZE
                        // Characters with weapons (lightsabers, blasters) can't fire them at
                        // system locations (space). They're mostly useless there.
                        // Also penalize vehicles going to space (already handled above with
                        // VERY_BAD_DELTA, but this adds reasoning for character+weapon combos).
                        // =====================================================
                        if (isCharacter && isSpaceSystem) {
                            // Check if this character has a permanent weapon.
                            // Characters with "permanent weapon" in game text have built-in weapons
                            // that can't fire at system (space) locations — they're useless there.
                            boolean hasPermanentWeapon = false;
                            // Primary: Check deploying card's blueprint game text for "permanent weapon"
                            if (deployingBlueprintId != null) {
                                try {
                                    SwccgCardBlueprint weaponCheckBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (weaponCheckBp != null) {
                                        String weaponGameText = weaponCheckBp.getGameText();
                                        if (weaponGameText != null) {
                                            String weaponTextLower = weaponGameText.toLowerCase(java.util.Locale.ROOT);
                                            if (weaponTextLower.contains("permanent weapon")) {
                                                hasPermanentWeapon = true;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V24.14B WEAPON CHECK: Error: {}", e.getMessage());
                                }
                            }
                            // Fallback: Check decision text for weapon keywords in card name
                            if (!hasPermanentWeapon) {
                                if (decisionText.contains("lightsaber") || decisionText.contains("blaster")
                                    || decisionText.contains("with rifle") || decisionText.contains("with cannon")) {
                                    hasPermanentWeapon = true;
                                }
                            }
                            if (hasPermanentWeapon) {
                                action.addReasoning("V24.14B WEAPON CHAR TO SPACE: Permanent weapon can't fire at system locations — useless in space!", -300.0f);
                                logger.warn("V24.14B WEAPON TO SPACE: Character with permanent weapon deploying to {} — penalized (-300)", title);
                            }
                        }
                        // V24.14B: Weapon characters are GREAT at ground sites — they win battles!
                        // Bonus for deploying to sites, especially objective locations or contested ones.
                        if (isCharacter && (isGroundSite || isDockingBay)) {
                            boolean hasPermanentWeaponGround = false;
                            if (deployingBlueprintId != null) {
                                try {
                                    SwccgCardBlueprint wgBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (wgBp != null) {
                                        String wgText = wgBp.getGameText();
                                        if (wgText != null && wgText.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                                            hasPermanentWeaponGround = true;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }
                            if (!hasPermanentWeaponGround) {
                                if (decisionText.contains("lightsaber") || decisionText.contains("blaster")
                                    || decisionText.contains("with rifle") || decisionText.contains("with cannon")) {
                                    hasPermanentWeaponGround = true;
                                }
                            }
                            if (hasPermanentWeaponGround) {
                                action.addReasoning("V24.14B WEAPON CHAR ON GROUND: Strong battle presence — weapon fires here!", 100.0f);
                                logger.info("V24.14B WEAPON GROUND: Character with permanent weapon at site {} — bonus (+100)", title);
                            }
                        }

                        // =====================================================
                        // V29.7: DOCKING BAY CHARACTER DEPLOY — Protect empty bays
                        // If we own a docking bay with NO friendly characters,
                        // the opponent can freely deploy there. Boost character
                        // deployment to our own empty docking bays.
                        // =====================================================
                        if (isCharacter && isDockingBay && location != null) {
                            try {
                                String bayOwner = location.getOwner();
                                if (bayOwner != null && bayOwner.equals(playerId)) {
                                    // Our docking bay — check if empty of friendly characters
                                    boolean bayHasFriendly = false;
                                    java.util.List<PhysicalCard> bayCards = gameState.getCardsAtLocation(location);
                                    if (bayCards != null) {
                                        for (PhysicalCard bc : bayCards) {
                                            if (bc != null && playerId.equals(bc.getOwner())
                                                && bc.getBlueprint() != null
                                                && bc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                bayHasFriendly = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bayHasFriendly) {
                                        action.addReasoning("V29.7 EMPTY BAY: Deploy character to protect our docking bay from opponent!", 80.0f);
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // =====================================================
                        // V29.6: Battleground bonus for LOCATION DEPLOY
                        // Use real game engine API for accurate detection.
                        // Note: When deploying a location card, we check if the
                        // location being deployed IS a battleground.
                        // =====================================================
                        if (blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION) {
                            boolean isBgLoc = false;
                            // For location deploy, the card isn't in play yet, so we
                            // fall back to game text / title heuristic since the game
                            // engine can't check a card that's not on the table yet.
                            String gameTextBg = blueprint.getGameText();
                            if (gameTextBg != null && gameTextBg.toLowerCase(java.util.Locale.ROOT).contains("battleground")) {
                                isBgLoc = true;
                            } else if (titleLower.contains("battleground")) {
                                isBgLoc = true;
                            } else {
                                // Most sites with both LS and DS force icons are battlegrounds.
                                try {
                                    if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE)
                                        && blueprint.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)) {
                                        isBgLoc = true;
                                    }
                                } catch (Exception e) {
                                    // Fall through
                                }
                            }
                            if (isBgLoc) {
                                action.addReasoning("V29.6 Battleground location — force drains!", 50.0f);
                            }
                        }

                        // V22: OBJECTIVE LOCATION BONUS (boosted from +50 to +150)
                        // Deploy to locations relevant to our objective - critical for flipping
                        // V24.15: SKIP for spies — they don't contribute presence to objectives while undercover!
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer locObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (!earlySpyDetected && locObjAnalyzer != null && locObjAnalyzer.isAnalyzed() && title != null) {
                            if (locObjAnalyzer.isObjectiveRelevantLocation(title)) {
                                float objLocBonus = locObjAnalyzer.getLocationObjectiveBonus(title);
                                action.addReasoning("OBJECTIVE LOCATION - deploy here helps flip!", objLocBonus);
                                logger.warn("V22 OBJECTIVE DEPLOY: {} is objective-relevant (+{})", title, objLocBonus);
                            }
                        }

                        // === V88 (Steve, 2026-05-19): MY LORD — SENATOR → GALACTIC SENATE BONUS ===
                        // CardSelection-side variant. Replay y6fo7f84hln9kuo8: Toonbuck Toora
                        // (senator) was in hand under My Lord objective, but the outer
                        // "Deploy" action goes through CardSelection for location pick.
                        // V88's DeployEvaluator variant only fires when actionText already
                        // contains "Galactic Senate" — not the case for generic "Deploy".
                        // This CardSelection variant boosts the Galactic Senate CANDIDATE
                        // when the card being deployed is a senator + My Lord active.
                        if (locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                                && locObjAnalyzer.getObjectiveTitle() != null
                                && title != null
                                && deployingBlueprintId != null) {
                            // V88 CONSOLIDATED (2026-07-07): identity from ObjectiveAnalyzer.isMyLord().
                            boolean v88IsMyLord = locObjAnalyzer.isMyLord();
                            if (v88IsMyLord) {
                                try {
                                    SwccgCardBlueprint v88DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                    // V88-CS-LORE (Steve, 2026-05-20): "senator" is identified in
                                    // LORE text, not via Keyword.SENATOR — only ~29 of 35 senator
                                    // cards add the keyword. Check both.
                                    boolean isSenator = false;
                                    if (v88DepBp != null) {
                                        if (v88DepBp.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                            isSenator = true;
                                        } else {
                                            String v88Lore = v88DepBp.getLore();
                                            if (v88Lore != null && v88Lore.toLowerCase(java.util.Locale.ROOT)
                                                    .contains("senator")) {
                                                isSenator = true;
                                            }
                                        }
                                    }
                                    if (isSenator) {
                                        String v88TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                                        if (v88TitleLower.contains("galactic senate")) {
                                            action.addReasoning(
                                                "V88 MY LORD: senator → Galactic Senate (flip target + weapon destiny -6 protection)",
                                                1500.0f);
                                            logger.warn("V88 MY LORD: senator location bonus +1500 for {}", title);
                                        } else {
                                            action.addReasoning(
                                                "V88 MY LORD: senator not at Galactic Senate — wrong site!",
                                                -2000.0f);
                                            logger.warn("V88 MY LORD: senator BLOCK -2000 for non-Senate target {}", title);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V88 MY LORD CardSelection error: {}", e.getMessage());
                                }
                            }
                        }

                        // === V88 GENERALIZED (Steve, 2026-06-03): TEXT-NAMED SITE BONUS ===
                        // Council-verified EDIT of V88 (engineer + rules_lawyer + voice_of_reason
                        // unanimous on "edits existing rule, not new"). The hardcoded senator+
                        // Galactic-Senate logic above runs only on My Lord. This universal clause
                        // runs for ANY character + ANY objective: scan the deploying character's
                        // game text and lore for a substring match against the bare candidate
                        // site name (everything after ":" in the site title).
                        // Steve, Jabba's Haven replay: V136 §A returned +500 for all four
                        // objective-relevant battlegrounds, tied at +1225 each, Rando picked
                        // Tatooine: Desert Heart by list order instead of Jabba's Palace:
                        // Audience Chamber. Jabba The Hutt (200_84) game text:
                        //   "While at Audience Chamber, may [download] Scum And Villainy
                        //    and immune to attrition < 4."
                        // The text-scan picks up "audience chamber" and adds +500 to the
                        // matching candidate — clean tie-break to the character's thematic
                        // home. Generalizes to any character/site pair (Vader/Death Star,
                        // Boushh/Jabba's Palace site, etc.) without card-name hardcoding.
                        //
                        // Guards (council false-positive concerns):
                        //   • Skip when bare site name < 5 chars (avoids "cave"/"hall"/"bay"
                        //     matching anywhere).
                        //   • Negative-phrase detection: "not at X" / "may not deploy at X" /
                        //     "cannot deploy at X" → -500 instead of +500.
                        //   • Match against the BARE site name (post-colon) — avoids
                        //     false-positive on a parent planet prefix.
                        if (deployingBlueprintId != null && title != null) {
                            try {
                                SwccgCardBlueprint v88GenBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v88GenBp != null) {
                                    String v88GenChar = ((v88GenBp.getGameText() != null ? v88GenBp.getGameText() : "")
                                        + " " + (v88GenBp.getLore() != null ? v88GenBp.getLore() : ""))
                                        .toLowerCase(java.util.Locale.ROOT);
                                    String v88GenFullSite = title.toLowerCase(java.util.Locale.ROOT);
                                    String v88GenBareSite = v88GenFullSite.contains(":")
                                        ? v88GenFullSite.substring(v88GenFullSite.indexOf(":") + 1).trim()
                                        : v88GenFullSite;
                                    if (v88GenBareSite.length() >= 5
                                            && v88GenChar.contains(v88GenBareSite)) {
                                        boolean v88GenNegative =
                                            v88GenChar.contains("not at " + v88GenBareSite)
                                            || v88GenChar.contains("may not deploy at " + v88GenBareSite)
                                            || v88GenChar.contains("cannot deploy at " + v88GenBareSite)
                                            || v88GenChar.contains("not at " + v88GenFullSite);
                                        if (v88GenNegative) {
                                            action.addReasoning(
                                                "V88 TEXT-NAMED SITE: character text says NOT at '"
                                                    + v88GenBareSite + "' — wrong site",
                                                -500.0f);
                                            logger.warn("V88 TEXT-NAMED NEG: '{}' text avoids '{}' → -500",
                                                v88GenBp.getTitle(), v88GenBareSite);
                                        } else {
                                            // 2026-06-04 DEFICIT GATE (Steve, Jabba-walks-into-
                                            // Luke replay): without this gate, my +500 home-site
                                            // bonus dragged Jabba into Audience Chamber where opp
                                            // had 23 power with lightsaber vs us 10 (gap 13).
                                            // Walking a character into a doomed fight for a
                                            // thematic bonus is bad play. Skip the +500 when the
                                            // candidate site is hopelessly outgunned (deficit ≥ 6,
                                            // matching the V67bn cap of 5 with a one-point
                                            // hysteresis buffer). The negative -500 branch above
                                            // still fires for "may not deploy at X" text — that
                                            // applies regardless of site contestation.
                                            boolean v88GenDoomed = false;
                                            if (game != null && playerId != null) {
                                                try {
                                                    String v88GenOpp = game.getOpponent(playerId);
                                                    float v88GenOppPwr = game.getModifiersQuerying()
                                                        .getTotalPowerAtLocation(game.getGameState(),
                                                            location, v88GenOpp, false, false);
                                                    float v88GenOurPwr = game.getModifiersQuerying()
                                                        .getTotalPowerAtLocation(game.getGameState(),
                                                            location, playerId, false, false);
                                                    v88GenDoomed = (v88GenOppPwr - v88GenOurPwr) >= 6f;
                                                    if (v88GenDoomed) {
                                                        logger.warn("V88 TEXT-NAMED SITE SKIP: '{}' wanted '{}' but site is hopelessly outgunned (opp {} us {} gap {}) — no +500",
                                                            v88GenBp.getTitle(), v88GenBareSite,
                                                            (int) v88GenOppPwr, (int) v88GenOurPwr,
                                                            (int) (v88GenOppPwr - v88GenOurPwr));
                                                    }
                                                } catch (Exception ignore) { /* allow fall-through */ }
                                            }
                                            if (!v88GenDoomed) {
                                                action.addReasoning(
                                                    "V88 TEXT-NAMED SITE: character text mentions '"
                                                        + v88GenBareSite + "' — home-site bonus",
                                                    500.0f);
                                                logger.warn("V88 TEXT-NAMED SITE: '{}' text mentions '{}' → +500",
                                                    v88GenBp.getTitle(), v88GenBareSite);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V88 TEXT-NAMED SITE error: {}", e.getMessage());
                            }
                        }

                        // === V99 (Steve, 2026-05-20): NON-SENATOR AT GALACTIC SENATE BLOCK ===
                        // CardSelection-side variant. The DeployEvaluator V99 fires only when
                        // actionText already contains "Galactic Senate" — but in practice the
                        // deploy action is generic ("Deploy") and the location is chosen here
                        // in CardSelectionEvaluator. So V99 must live where V88 lives.
                        // Block non-senators from picking Galactic Senate as their destination
                        // unless opponent power at Senate already exceeds friendly senator power
                        // (defensive reinforcement is allowed).
                        // NOTE (2026-07-07 consolidation): DELIBERATELY not isMyLord()-gated — this
                        // keys on the destination CANDIDATE being Galactic Senate, not the objective
                        // identity. Gating it would change behavior. Leave as a typed/title dest check.
                        if (title != null && deployingBlueprintId != null && gameState != null) {
                            String v99TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                            if (v99TitleLower.contains("galactic senate")) {
                                try {
                                    SwccgCardBlueprint v99DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                    boolean v99IsCharacter = v99DepBp != null
                                        && v99DepBp.getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER;
                                    // V99-LORE: senator detection via lore text + keyword (Steve, 2026-05-20)
                                    boolean v99IsSenator = false;
                                    if (v99DepBp != null) {
                                        if (v99DepBp.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                            v99IsSenator = true;
                                        } else {
                                            String v99DepLore = v99DepBp.getLore();
                                            if (v99DepLore != null && v99DepLore.toLowerCase(java.util.Locale.ROOT)
                                                    .contains("senator")) {
                                                v99IsSenator = true;
                                            }
                                        }
                                    }
                                    if (v99IsCharacter && !v99IsSenator) {
                                        // Find the actual Senate location to query power
                                        com.gempukku.swccgo.game.PhysicalCard v99SenateLoc = null;
                                        for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getTopLocations()) {
                                            if (loc == null || loc.getTitle() == null) continue;
                                            if (loc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                                    .contains("galactic senate")) {
                                                v99SenateLoc = loc;
                                                break;
                                            }
                                        }
                                        // Default-safe: if Senate isn't yet on table, block — Senate is being
                                        // deployed this same phase via My Lord and non-senators should wait.
                                        float v99FriendlySenatorPower = 0f;
                                        float v99OpponentPower = 0f;
                                        if (v99SenateLoc != null && context.getGame() != null) {
                                            String v99PlayerId = context.getPlayerId();
                                            String v99Opp = context.getGame().getOpponent(v99PlayerId);
                                            for (com.gempukku.swccgo.game.PhysicalCard pc :
                                                    gameState.getAllPermanentCards()) {
                                                if (pc == null) continue;
                                                if (!v99PlayerId.equals(pc.getOwner())) continue;
                                                com.gempukku.swccgo.game.PhysicalCard pcLoc = null;
                                                try {
                                                    pcLoc = context.getGame().getModifiersQuerying()
                                                        .getLocationThatCardIsAt(gameState, pc);
                                                } catch (Exception ignore) { /* */ }
                                                if (pcLoc != v99SenateLoc) continue;
                                                if (pc.getBlueprint() == null) continue;
                                                // V99-LORE: senator detection via lore + keyword
                                                boolean v99PcIsSenator = false;
                                                if (pc.getBlueprint().hasKeyword(
                                                        com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                                    v99PcIsSenator = true;
                                                } else {
                                                    String v99PcLore = pc.getBlueprint().getLore();
                                                    if (v99PcLore != null && v99PcLore.toLowerCase(
                                                            java.util.Locale.ROOT).contains("senator")) {
                                                        v99PcIsSenator = true;
                                                    }
                                                }
                                                if (!v99PcIsSenator) continue;
                                                Float p = pc.getBlueprint().getPower();
                                                if (p != null) v99FriendlySenatorPower += p;
                                            }
                                            if (v99Opp != null) {
                                                v99OpponentPower = context.getGame().getModifiersQuerying()
                                                    .getTotalPowerAtLocation(gameState, v99SenateLoc,
                                                        v99Opp, false, false);
                                            }
                                        }
                                        if (v99OpponentPower <= v99FriendlySenatorPower) {
                                            action.addReasoning(String.format(
                                                "V99 SENATE GUARD: non-senator → Galactic Senate "
                                                    + "(opp %.0f <= my senator %.0f) — wasted, deploy elsewhere",
                                                v99OpponentPower, v99FriendlySenatorPower),
                                                -1500.0f);
                                            logger.warn("V99 SENATE GUARD: BLOCK non-senator → Senate "
                                                + "(opp={} my-senators={}) -1500",
                                                (int)v99OpponentPower, (int)v99FriendlySenatorPower);
                                        } else {
                                            logger.info("V99 SENATE GUARD: ALLOW non-senator → Senate "
                                                + "(opp={} > my-senators={}) — defensive reinforcement",
                                                (int)v99OpponentPower, (int)v99FriendlySenatorPower);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V99 SENATE GUARD CardSelection error: {}", e.getMessage());
                                }
                            }
                        }

                        // === V89-CS (Steve, 2026-05-20): DR. EVAZAN — NEEDS ARMED PARTNER (CardSelection) ===
                        // The original V89 in DeployEvaluator only fires when actionText already
                        // contains the target location title — but in practice the deploy action
                        // is generic ("Deploy") and the location is chosen here. Same fix pattern
                        // as V99-CS.
                        if (title != null && deployingBlueprintId != null
                                && deployingCardName != null
                                && deployingCardName.startsWith("Dr. Evazan")
                                && gameState != null && context.getGame() != null) {
                            try {
                                com.gempukku.swccgo.game.PhysicalCard v89TargetLoc = null;
                                String v89TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                                for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getTopLocations()) {
                                    if (loc == null || loc.getTitle() == null) continue;
                                    if (loc.getTitle().toLowerCase(java.util.Locale.ROOT).equals(v89TitleLower)) {
                                        v89TargetLoc = loc;
                                        break;
                                    }
                                }
                                if (v89TargetLoc != null) {
                                    String v89PlayerId = context.getPlayerId();
                                    boolean armedFriendAtTarget = false;
                                    for (com.gempukku.swccgo.game.PhysicalCard pCard
                                            : gameState.getAllPermanentCards()) {
                                        if (pCard == null) continue;
                                        if (!v89PlayerId.equals(pCard.getOwner())) continue;
                                        com.gempukku.swccgo.game.PhysicalCard pCardLoc = null;
                                        try {
                                            pCardLoc = context.getGame().getModifiersQuerying()
                                                .getLocationThatCardIsAt(gameState, pCard);
                                        } catch (Exception ignore) { /* */ }
                                        if (pCardLoc != v89TargetLoc) continue;
                                        if (com.gempukku.swccgo.filters.Filters.character_with_a_weapon
                                                .accepts(gameState,
                                                    context.getGame().getModifiersQuerying(), pCard)) {
                                            armedFriendAtTarget = true;
                                            break;
                                        }
                                    }
                                    if (!armedFriendAtTarget) {
                                        action.addReasoning(
                                            "V89-CS DR. EVAZAN: '" + deployingCardName
                                                + "' → '" + v89TargetLoc.getTitle()
                                                + "' with no armed friend — block (will get sniped)",
                                            -1500.0f);
                                        logger.warn("V89-CS DR. EVAZAN: blocking {} → {} (no armed friend)",
                                            deployingCardName, v89TargetLoc.getTitle());
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V89-CS DR. EVAZAN CardSelection error: {}", e.getMessage());
                            }
                        }

                        // === V121 (Steve, 2026-05-22): V86 NEIMOIDIAN-PILOT MIRROR (CardSelection) ===
                        // V86 in DeployEvaluator only fires when actionText contains "aboard",
                        // " to ", or " on " (V86.1 identifiable-target guard). Generic "Deploy"
                        // action splits the deploy into action + location-pick; V86 silently
                        // skips the action step and the location-pick step has no V86 mirror.
                        // Result: Neimoidian pilots can still land on ground sites when the
                        // engine routes via "Deploy" → location-pick.
                        // V121 mirrors: under Invasion objective, if the deploying card is a
                        // Neimoidian pilot AND a friendly capital ship is on the table, penalize
                        // any candidate that ISN'T the capital ship.
                        if (deployingBlueprintId != null && title != null
                                && gameState != null && context.getGame() != null) {
                            try {
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v121Obj =
                                    context.getObjectiveAnalyzer();
                                // V121 CONSOLIDATED (2026-07-07): identity from ObjectiveAnalyzer.isInvasion().
                                if (v121Obj != null && v121Obj.isAnalyzed() && v121Obj.isInvasion()) {
                                    SwccgCardBlueprint v121DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (v121DepBp != null) {
                                        // Need a temp PhysicalCard view for Filters — fall back to
                                        // blueprint-level Species/Icon checks if no physical card yet.
                                        boolean v121IsNeimoidian = false;
                                        boolean v121IsPilot = false;
                                        try {
                                            v121IsNeimoidian = v121DepBp.getSpecies() != null
                                                && v121DepBp.getSpecies() == com.gempukku.swccgo.common.Species.NEIMOIDIAN;
                                            v121IsPilot = v121DepBp.hasIcon(com.gempukku.swccgo.common.Icon.PILOT);
                                        } catch (Exception ignore) { /* false */ }
                                        if (v121IsNeimoidian && v121IsPilot) {
                                            String v121TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                                            // Find a friendly capital ship title
                                            PhysicalCard v121CapShip = null;
                                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                                if (pc == null || pc.getBlueprint() == null) continue;
                                                if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                                try {
                                                    if (com.gempukku.swccgo.filters.Filters.capital_starship.accepts(
                                                            gameState, context.getGame().getModifiersQuerying(), pc)) {
                                                        v121CapShip = pc;
                                                        break;
                                                    }
                                                } catch (Exception ignore) { /* skip */ }
                                            }
                                            if (v121CapShip != null) {
                                                String v121CapName = v121CapShip.getTitle() != null
                                                    ? v121CapShip.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                if (!v121TitleLower.contains(v121CapName)) {
                                                    action.addReasoning(
                                                        "V121 INVASION (CS): Neimoidian pilot must deploy aboard '"
                                                            + v121CapShip.getTitle() + "', not '" + title + "'",
                                                        -1500.0f);
                                                    logger.warn("V121 INVASION CS: blocking Neimoidian pilot → {} (not aboard {}) -1500",
                                                        title, v121CapShip.getTitle());
                                                } else {
                                                    action.addReasoning(
                                                        "V121 INVASION (CS): aboard capital ship — correct placement",
                                                        300.0f);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V121 INVASION CS error: {}", e.getMessage());
                            }
                        }

                        // === V136 (Steve, 2026-05-26): UNIFIED CHARACTER DEPLOY SITE EVALUATOR (CardSelection route) ===
                        // Supersedes V122 (below) and V67as (later in this file).
                        // See CharacterDeploySiteEvaluator + V136_DEPLOY_LOG.md.
                        if (title != null && deployingBlueprintId != null && location != null
                                && gameState != null && context.getGame() != null
                                && context.getPlayerId() != null) {
                            try {
                                SwccgCardBlueprint v136DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v136DepBp != null
                                        && v136DepBp.getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                    // Find the actual PhysicalCard in hand by matching blueprintId
                                    PhysicalCard v136DeployingCard = null;
                                    for (PhysicalCard h : gameState.getHand(context.getPlayerId())) {
                                        if (h == null) continue;
                                        if (deployingBlueprintId.equals(h.getBlueprintId(false))) {
                                            v136DeployingCard = h;
                                            break;
                                        }
                                    }
                                    if (v136DeployingCard != null) {
                                        // FORMATION SAFETY (2026-07-11c): L3/L4 deploy vetoes — un-outvotable.
                                        // (Codex audit incident 1: Greedo ability 1 deployed solo at 1420 while
                                        // two affordable buddies sat in hand; every guard was additive.)
                                        try {
                                            SwccgCardBlueprint fsDepBp = v136DeployingCard.getBlueprint();
                                            if (fsDepBp != null && fsDepBp.getCardCategory() == CardCategory.CHARACTER
                                                    && context.getGame() != null) {
                                                // ADJUSTED 2026-07-12 (Codex m00194 P0#3): pair-budget facts —
                                                // cheapest OTHER deployable character in hand (null = no plan).
                                                float fsForce = gameState.getForcePileSize(context.getPlayerId());
                                                Float fsThisCost = fsDepBp.getDeployCost();
                                                Float fsBuddyCost = null;
                                                for (PhysicalCard fsH : gameState.getHand(context.getPlayerId())) {
                                                    if (fsH == null || fsH.getBlueprint() == null) continue;
                                                    if (fsH.getCardId() == v136DeployingCard.getCardId()) continue;
                                                    if (fsH.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                    Float fsC = fsH.getBlueprint().getDeployCost();
                                                    if (fsC == null) continue;
                                                    if (fsBuddyCost == null || fsC < fsBuddyCost) fsBuddyCost = fsC;
                                                }
                                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer fsObj =
                                                    context.getObjectiveAnalyzer();
                                                String fsFlipGate = (fsObj != null && fsObj.isAnalyzed())
                                                    ? fsObj.getFlipCriticalControlSite() : null;
                                                String fsV = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                                    .vetoCharacterDeploy(context.getGame(), gameState, context.getPlayerId(),
                                                        v136DeployingCard,
                                                        fsDepBp.hasPowerAttribute() ? fsDepBp.getPower() : null,
                                                        fsDepBp.hasAbilityAttribute() ? fsDepBp.getAbility() : null,
                                                        v136DeployingCard.isUndercover(),
                                                        location, fsForce, fsThisCost, fsBuddyCost, fsFlipGate);
                                                if (fsV != null) {
                                                    action.hardVeto(fsV);
                                                    logger.warn("FORMATION SAFETY (deploy-site): {}", fsV);
                                                } else if (com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                                        .weakSoloNoPlan(context.getGame(), gameState, context.getPlayerId(),
                                                            fsDepBp.hasAbilityAttribute() ? fsDepBp.getAbility() : null,
                                                            v136DeployingCard.isUndercover(), location, fsBuddyCost)) {
                                                    // L3 NO-PLAN (Steve 2026-07-12): weak solo with NO buddy plan —
                                                    // heavy penalty (holds unless it's genuinely the only useful play).
                                                    action.addReasoning(
                                                        "L3 NO-PLAN SOLO: weak body would land alone with no deployable buddy in hand",
                                                        -800.0f);
                                                    logger.warn("FORMATION SAFETY (deploy-site): L3 NO-PLAN SOLO -800 at {}", title);
                                                }
                                            }
                                        } catch (Exception fsE) { /* fail-open */ }
                                        int v136Turn = gameState.getPlayersLatestTurnNumber(context.getPlayerId());
                                        java.util.List<PhysicalCard> v136Hand = gameState.getHand(context.getPlayerId());
                                        int v136ForceAvail = gameState.getForcePileSize(context.getPlayerId());
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v136Obj =
                                            context.getObjectiveAnalyzer();
                                        // step 3b (2026-07-10): filter-based relevance overload (rules) — for
                                        // objectives WITHOUT rules this equals the old title check (neutral).
                                        boolean v136ObjRelevant = v136Obj != null && v136Obj.isAnalyzed()
                                            && title != null
                                            && v136Obj.isObjectiveRelevantLocation(location, context.getGame(), context.getPlayerId());
                                        float v136Score = com.gempukku.swccgo.ai.models.common.strategy
                                            .CharacterDeploySiteEvaluator.evaluateSite(
                                                context.getGame(), v136DeployingCard, location,
                                                context.getPlayerId(),
                                                v136ObjRelevant,
                                                v136Hand,
                                                v136ForceAvail,
                                                v136Turn,
                                                0 /* deckShipCount — TODO wire */,
                                                false /* perSiteEffectActive — TODO wire */);
                                        if (v136Score != 0f) {
                                            action.addReasoning(
                                                "V136 unified deploy-site score (CS) → " + title + ": " + v136Score,
                                                v136Score);
                                            logger.info("V136 CS [{}]: {} → {} score={}",
                                                context.getPlayerId(), v136DeployingCard.getTitle(), title, v136Score);
                                        }

                                        // === V193 (CS) (Steve, 2026-07-09): FLIP-GATE CONTROL STEER on the CardSelection route ===
                                        // V193 (see DeployEvaluator) steers ONE body to the objective's flip-gate
                                        // control site (Endor: Bunker for Endor Operations) so the control-gated
                                        // flip card (Establish Secret Base (V) 207_25, "Deploy on Bunker if you
                                        // control that site") becomes a legal deploy and the objective can flip.
                                        // The original V193 lives ONLY in DeployEvaluator, but Endor character
                                        // deploys resolve through THIS CardSelection route (logged "V136 CS"), so
                                        // V193 fired 0 times in replay somykkwjy449xul4 and the objective never
                                        // flipped. This mirror puts the steer where the deploys actually are.
                                        //
                                        // TWO CORRECTIONS vs the DeployEvaluator copy, both evidence-backed:
                                        //  1. ABILITY GATE. Control needs presence; presence needs ability >= 1.
                                        //     That game put 4-LOM With Concussion Rifle (V) (a DROID, ability 0) on
                                        //     Bunker turn 5 — no presence, no control, ESB stayed illegal. So only
                                        //     steer a real character with ability >= 1 (droids have
                                        //     hasAbilityAttribute()==false and are skipped) and prefer a CHEAP spare
                                        //     body (deployCost <= 3, e.g. Admiral Ozzel (V) cost 2) so a bomber
                                        //     (Thrawn) is not wasted on a 0-drain site. Endor Shield (V) uploads an
                                        //     Imperial admiral twice per game, so a cheap legal body is available.
                                        //  2. MAGNITUDE. The CS route stacks anti-hold penalties the DeployEvaluator
                                        //     boundary never saw: V67ah NON-BG -350, V113 SOLO -300, V24.15
                                        //     ZERO-DRAIN ~-80, plus (for a Star-Destroyer pilot like Ozzel) V29
                                        //     GROUND -200 and V29 CONCENTRATE -100 — all fighting a hold that IS the
                                        //     objective's win condition, not a mistake. And the competing drain site
                                        //     runs HOT when a friendly is already there to REINFORCE (+150). Two
                                        //     replays: somykkwjy449xul4 t3 Thrawn->Bunker 135 vs Landing 905; the
                                        //     RE-TUNE replay vugpape5lw1bc7rq t2 Ozzel->Bunker 1240 vs Landing 1250 —
                                        //     the first +730-offset steer LOST BY 10. So the steer must DOMINATE (not
                                        //     merely nudge): playbook weight (400) + CS penalty offset (1600) = ~2000
                                        //     (V136 §A team-viability scale — seizing the flip gate unlocks the whole
                                        //     objective flip, a game-deciding tempo swing worth more than any single
                                        //     drain). That lifts Bunker to ~2100 > the ~1430-1555 hottest observed
                                        //     drain competitors by ~550. The large magnitude is safe because the guard
                                        //     is narrow (Endor flip-gate + holds ESB + Bunker uncontrolled + cheap
                                        //     ability body) and self-limiting: fires only
                                        //     while (a) analyzer named a flip-gate site, (b) Rando does NOT control
                                        //     it, (c) Rando holds the gate card. Once one body lands Rando controls
                                        //     Bunker -> guard (b) closes -> no per-body stacking; the rest of the
                                        //     pile reverts to drain sites and the lone body holds.
                                        if (v136Obj != null && v136Obj.isAnalyzed() && title != null) {
                                            String v193csGateSite = v136Obj.getFlipCriticalControlSite();
                                            if (v193csGateSite != null && v193csGateSite.equalsIgnoreCase(title)) {
                                                Float v193csAbility = v136DepBp.hasAbilityAttribute() ? v136DepBp.getAbility() : null;
                                                Float v193csCost = v136DepBp.getDeployCost();
                                                boolean v193csGoodBody = v193csAbility != null && v193csAbility >= 1f
                                                    && v193csCost != null && v193csCost <= 4f;
                                                if (v193csGoodBody) {
                                                    boolean v193csControls = com.gempukku.swccgo.cards.GameConditions.controls(
                                                        context.getGame(), context.getPlayerId(), location);
                                                    java.util.Set<String> v193csGateIds = v136Obj.getFlipCriticalControlCardIds();
                                                    String v193csGateCard = v136Obj.getFlipCriticalControlCard();
                                                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle v193csOracle = context.getDeckOracle();
                                                    boolean v193csHoldsGate = false;
                                                    if (v193csOracle != null) {
                                                        if (v193csGateIds != null && !v193csGateIds.isEmpty()) {
                                                            for (String v193csId : v193csGateIds) {
                                                                if (v193csOracle.isCardInHand(v193csId)
                                                                        || v193csOracle.isCardInReserve(v193csId)) {
                                                                    v193csHoldsGate = true;
                                                                    break;
                                                                }
                                                            }
                                                        } else if (v193csGateCard != null) {
                                                            v193csHoldsGate = v193csOracle.isCardInHand(v193csGateCard)
                                                                || v193csOracle.isCardInReserve(v193csGateCard);
                                                        }
                                                    }
                                                    if (!v193csControls && v193csHoldsGate) {
                                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.ObjectivePlaybook
                                                            v193csPlaybook = v136Obj.getActivePlaybook();
                                                        float v193csWeight = (v193csPlaybook != null)
                                                            ? v193csPlaybook.weights.deployFlipGateSite : 400.0f;
                                                        // CS penalty offset: total steer ~= 2000 to DOMINATE the anti-hold
                                                        // stack (V67ah -350 + V113 -300 + V24.15 -80 + Ozzel V29 GROUND
                                                        // -200 + CONCENTRATE -100) AND a REINFORCED hot drain competitor.
                                                        // +730 lost Ozzel->Bunker by 10 (1240 vs 1250) in replay
                                                        // vugpape5lw1bc7rq t2; +1600 (total ~2000) wins by ~550.
                                                        float v193csBonus = v193csWeight + 1600.0f;
                                                        action.addReasoning(
                                                            "V193 (CS) FLIP-GATE CONTROL: steer one ability body to '"
                                                                + title + "' to enable '" + v193csGateCard
                                                                + "' (objective flip gate)",
                                                            v193csBonus);
                                                        logger.warn("V193 (CS) FLIP-GATE CONTROL [{}]: {} → {} +{} (seize flip-gate, card={})",
                                                            context.getPlayerId(), v136DeployingCard.getTitle(), title, v193csBonus, v193csGateCard);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V136 CS error: {}", e.getMessage());
                            }
                        }

                        // === V122 SUPERSEDED 2026-05-26: see V136 CharacterDeploySiteEvaluator §A ===
                        // Original: V90 mirror for hand-deploy CardSelection route.
                        // Block kept inert below for easy revert.
                        if (false /* V122 SUPERSEDED V136 */ && title != null && deployingBlueprintId != null
                                && gameState != null && context.getGame() != null) {
                            try {
                                SwccgCardBlueprint v122DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                boolean v122IsCharacter = v122DepBp != null
                                    && v122DepBp.getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER;
                                if (v122IsCharacter && location != null) {
                                    boolean v122EnemyArmed = false;
                                    boolean v122FriendlyArmed = false;
                                    String v122OppId = context.getGame().getOpponent(context.getPlayerId());
                                    for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                        if (pc == null) continue;
                                        PhysicalCard pcLoc = null;
                                        try {
                                            pcLoc = context.getGame().getModifiersQuerying()
                                                .getLocationThatCardIsAt(gameState, pc);
                                        } catch (Exception ignore) { /* skip */ }
                                        if (pcLoc != location) continue;
                                        boolean armed = false;
                                        try {
                                            armed = com.gempukku.swccgo.filters.Filters.character_with_a_weapon.accepts(
                                                gameState, context.getGame().getModifiersQuerying(), pc);
                                        } catch (Exception ignore) { /* false */ }
                                        if (!armed) continue;
                                        if (v122OppId != null && v122OppId.equals(pc.getOwner())) {
                                            v122EnemyArmed = true;
                                        } else if (context.getPlayerId().equals(pc.getOwner())) {
                                            v122FriendlyArmed = true;
                                        }
                                    }
                                    if (v122EnemyArmed && !v122FriendlyArmed) {
                                        action.addReasoning(
                                            "V122 NO SUICIDE (CS): '" + title
                                                + "' has enemy armed char + no friendly weapon — will be sniped",
                                            -1500.0f);
                                        logger.warn("V122 NO SUICIDE CS: blocking deploy → {} (enemy armed, no friendly weapon) -1500",
                                            title);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V122 NO SUICIDE CS error: {}", e.getMessage());
                            }
                        }

                        // === V133 DROPPED (Steve, 2026-05-26) ===
                        // Removed in favor of upcoming consolidated V136 master deploy
                        // rule (battle-math team viability + buddy detection in one
                        // evaluator). The narrow regex-on-game-text approach only caught
                        // the ~5% of cards with explicit "deploys to same site as X" text.
                        // The broader buddy concept Steve asked for (universal solo-low-
                        // ability gate) lives in V136 instead. V122 also slated for
                        // removal under V136 consolidation.

                        // === V24.10: EXECUTOR MUST DEPLOY TO BESPIN ===
                        // When deploying Executor/Flagship, Bespin system is the ONLY correct target
                        // for TDIGWATT. Deploying to any other system is catastrophic — the entire
                        // deck engine depends on Executor occupying Bespin for Dark Deal + CC Occupation.
                        if (isStarship && isSpaceSystem && deployingCardName != null) {
                            String deployingNameLower = deployingCardName.toLowerCase(java.util.Locale.ROOT);
                            if (deployingNameLower.contains("executor") || deployingNameLower.contains("flagship")) {
                                String locTitleLower = title != null ? title.toLowerCase(java.util.Locale.ROOT) : "";
                                if (locTitleLower.contains("bespin")) {
                                    action.addReasoning("V24.10 EXECUTOR TO BESPIN: This is THE correct system — entire TDIGWATT engine depends on it!", 500.0f);
                                    logger.warn("V24.10 EXECUTOR LOCATION: Bespin system selected — MASSIVE bonus (+500)!");
                                } else {
                                    // Any non-Bespin system is WRONG for Executor
                                    action.addReasoning("V24.10 EXECUTOR WRONG SYSTEM: Executor MUST go to Bespin, not " + title + "!", -9999.0f);
                                    logger.warn("V24.10 EXECUTOR LOCATION: {} is NOT Bespin — HARD BLOCK! Executor must deploy to Bespin!", title);
                                }
                            }
                        }

                        // === V22.7: OBJECTIVE-CRITICAL LOCATION CONTESTATION ===
                        // If the opponent occupies a location our objective NEEDS us to control,
                        // we MUST contest it — even if we're currently losing there.
                        // The objective bonus alone may not override the contest penalty,
                        // so add an explicit "MUST CONTEST" bonus for ships at critical systems.
                        if (locObjAnalyzer != null && locObjAnalyzer.isAnalyzed() && title != null
                            && locObjAnalyzer.isObjectiveRelevantLocation(title)
                            && isSpaceSystem && isStarship && game != null) {
                            try {
                                float ourP = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, playerId, false, false);
                                String opp = game.getOpponent(playerId);
                                float theirP = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, opp, false, false);
                                if (theirP > 0 && ourP < theirP) {
                                    // Opponent controls our objective-critical system!
                                    // Strong override to ensure we deploy here despite contest penalty.
                                    action.addReasoning("V22.7 MUST CONTEST: Opponent controls objective-critical " +
                                        title + "! Deploy ship to contest!", 300.0f);
                                    logger.warn("V22.7 MUST CONTEST: {} — opponent has {} power, we have {} — MUST deploy ship here!",
                                        title, (int)theirP, (int)ourP);
                                }
                            } catch (Exception e) {
                                logger.debug("V22.7: Could not check objective-critical contest: {}", e.getMessage());
                            }
                        }

                        // === V23: OPPONENT FORCE ICON PREFERENCE (ALL OBJECTIVES) ===
                        // Locations with opponent force icons are better force drain targets.
                        // For Dark Side: Light Side force icons = more drain damage.
                        if (blueprint != null) {
                            Side mySide = context.getSide();
                            int opponentIcons = 0;
                            if (mySide == Side.DARK) {
                                opponentIcons = blueprint.getIconCount(Icon.LIGHT_FORCE);
                            } else {
                                opponentIcons = blueprint.getIconCount(Icon.DARK_FORCE);
                            }
                            if (opponentIcons > 0) {
                                float iconBonus = opponentIcons * 30.0f;
                                action.addReasoning("V23 FORCE DRAIN: " + opponentIcons +
                                    " opponent force icon(s) — better drain target!", iconBonus);
                                logger.info("V23 FORCE ICONS: {} has {} opponent icons (+{})", title, opponentIcons, (int)iconBonus);
                            }
                        }

                        // === V24.15: AVOID DEPLOYING CHARACTERS TO WORTHLESS-DRAIN LOCATIONS ===
                        // (UPDATED 2026-07-07, Steve — CONSOLIDATED: now covers EFFECTIVE drain, not
                        // just literal raw 0, per feedback_update_old_rule_not_new_version. The old
                        // block only read raw drain, so under an opponent Battle Plan (+3 drain-
                        // INITIATION tax) Rando piled bodies onto Endor: Landing Platform — raw drain
                        // 1, net 1-3 = -2, a drain there is a net LOSS that V189 blocks every time —
                        // yet the deploy path still scored it high (mistake 4, replay qgdridfo166f27r3).
                        // The effective-drain check is folded IN HERE (one rule) instead of a separate
                        // contradictory penalty.)
                        // Characters at worthless-drain sites add no drain pressure and are Surprise-
                        // Assault bait. Penalty scales with power — don't waste your best characters.
                        if (isCharacter && !earlySpyDetected && game != null && location != null) {
                            try {
                                float v2415RawDrain = game.getModifiersQuerying().getForceDrainAmount(
                                    game.getGameState(), location, playerId);
                                float v2415InitCost = game.getModifiersQuerying().getInitiateForceDrainCost(
                                    game.getGameState(), location, playerId);
                                boolean v2415ZeroDrain = v2415RawDrain <= 0;
                                // Net-negative EFFECTIVE drain: drain capped below its cost by a tax
                                // (Battle Plan / Battle Order) — the EXACT V189 predicate. cost 0 (no
                                // tax = the vast majority of games) => this arm can NEVER fire, so every
                                // existing game is byte-identical to before this consolidation.
                                boolean v2415NetNeg = v2415InitCost > 0f && (v2415RawDrain - v2415InitCost) <= -2f;
                                Float v2415CharPower = (blueprint != null && blueprint.hasPowerAttribute()) ? blueprint.getPower() : null;
                                float v2415PowerVal = (v2415CharPower != null) ? v2415CharPower : 3.0f;
                                if (v2415ZeroDrain) {
                                    // ORIGINAL behavior, unchanged: gentle nudge off a literal 0-drain site.
                                    float zeroDrainPenalty = -50.0f - (v2415PowerVal * 10.0f);
                                    action.addReasoning("V24.15 ZERO DRAIN: Location has 0 drain — character wasted here!", zeroDrainPenalty);
                                    logger.warn("V24.15 ZERO DRAIN: {} has 0 drain — penalizing {} (power {}) by {}",
                                        title, decisionText, v2415PowerVal, zeroDrainPenalty);
                                } else if (v2415NetNeg) {
                                    // NEW arm (tax-capped drain = net loss). Exempt objective flip/seed
                                    // sites (e.g. Endor: Bunker) and legit V166 contests; else a stronger
                                    // deterrent so a genuinely drainable site wins the body instead.
                                    boolean v2415ObjRelevant = locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                                        && title != null && locObjAnalyzer.isObjectiveRelevantLocation(title);
                                    boolean v2415V166Contest = false;
                                    if (!v2415ObjRelevant) {
                                        String v2415Opp = gameState.getOpponent(playerId);
                                        float v2415TheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, location, v2415Opp, false, false);
                                        int v2415OppDrain = (int) game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, location, v2415Opp);
                                        v2415V166Contest = (v2415TheirPower > 0 && v2415OppDrain > 0
                                            && computeNetDrainBalance(game, gameState, playerId) >= 2);
                                    }
                                    if (!v2415ObjRelevant && !v2415V166Contest) {
                                        // -(300 + power*30) cap -700: stays above the -1500/-2000/-9999
                                        // hard blocks so a forced/objective deploy can still exceed it.
                                        float v2415Pen = Math.min(700.0f, 300.0f + v2415PowerVal * 30.0f);
                                        action.addReasoning(String.format(
                                            "V24.15 EFFECTIVE DRAIN: raw %.0f - initiate cost %.0f <= -2 at %s (a drain here is a net loss) — don't pile bodies",
                                            v2415RawDrain, v2415InitCost, title), -v2415Pen);
                                        logger.warn("V24.15 EFFECTIVE DRAIN: {} raw={} cost={} power={} -> {}",
                                            title, (int) v2415RawDrain, (int) v2415InitCost, (int) v2415PowerVal, (int) (-v2415Pen));
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V24.15: Error checking drain amount for deploy: {}", e.getMessage());
                            }
                        }

                        // === V29.7: ISB OPERATIONS DEPLOYMENT STRATEGY (enhanced from V25) ===
                        // FLIP CONDITION: 4 ISB agents on table OR ISB agents control 2 Rebel Base locations.
                        // FLIPPED BONUS: +1 drain at BG sites with non-Undercover ISB agent, -1 opponent drain.
                        // STRATEGY: Pre-flip, ISB agents get MASSIVE priority. Non-ISB agents heavily penalized.
                        //           Higher ability characters preferred for location control.
                        if (isCharacter && !earlySpyDetected && locObjAnalyzer != null
                            && locObjAnalyzer.isAnalyzed() && locObjAnalyzer.isISBOperations()) {
                            try {
                                // Check if the deploying card is an ISB agent and get its ability
                                boolean deployingIsISBAgent = false;
                                float deployAbility = 0.0f;
                                if (deployingBlueprintId != null) {
                                    SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (deployBp != null) {
                                        String lore = deployBp.getLore();
                                        if (lore != null) {
                                            String loreLower = lore.toLowerCase(Locale.ROOT);
                                            deployingIsISBAgent = loreLower.contains("isb")
                                                || loreLower.contains("rebel") || loreLower.contains("rebellion");
                                        }
                                        // V29.7: Get ability value for ability-based scoring
                                        if (deployBp.hasAbilityAttribute()) {
                                            deployAbility = deployBp.getAbility();
                                        }
                                    }
                                }

                                int isbOnTable = locObjAnalyzer.countISBAgentsOnTable(gameState, playerId);
                                int isbNeeded = locObjAnalyzer.getISBFlipAgentCount();
                                boolean preFlip = !locObjAnalyzer.isFlipped();
                                boolean needMoreAgents = preFlip && isbOnTable < isbNeeded;
                                int agentsStillNeeded = isbNeeded - isbOnTable;

                                // Check if this location is a battleground site (use real API)
                                boolean isBattleground = false;
                                try {
                                    if (location != null) {
                                        isBattleground = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                    }
                                } catch (Exception bgE) {
                                    // Fallback to icon check
                                    if (blueprint != null) {
                                        if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                            && blueprint.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE)) {
                                            isBattleground = true;
                                        }
                                    }
                                }

                                if (deployingIsISBAgent) {
                                    // V29.7: ISB agent — STRONG bonus pre-flip, scales with urgency
                                    float isbBonus;
                                    if (needMoreAgents) {
                                        // Pre-flip: massive priority. More urgency as we get closer to 4.
                                        isbBonus = 200.0f + (4 - agentsStillNeeded) * 30.0f;
                                    } else if (preFlip) {
                                        // Have enough for flip (should auto-flip), but still value ISB agents
                                        isbBonus = 100.0f;
                                    } else {
                                        // Post-flip: ISB agents at BG sites give drain bonuses
                                        isbBonus = 80.0f;
                                    }
                                    if (isBattleground) {
                                        isbBonus += 60.0f;  // BG = drain bonus after flip, good control spot
                                    }
                                    // V29.7: Ability bonus — higher ability ISB agents help control locations
                                    if (deployAbility >= 5) {
                                        isbBonus += 40.0f;  // High ability = strong presence
                                    } else if (deployAbility >= 3) {
                                        isbBonus += 15.0f;
                                    }
                                    action.addReasoning("V29.7 ISB AGENT: Deploy ISB agent (ability " +
                                        String.format("%.0f", deployAbility) + ", " + isbOnTable +
                                        "/" + isbNeeded + " on table)" +
                                        (isBattleground ? " to BATTLEGROUND" : "") +
                                        (needMoreAgents ? " — NEED " + agentsStillNeeded + " MORE FOR FLIP!" : ""), isbBonus);
                                    logger.warn("V29.7 ISB: {} is ISB agent (ability {}) at {} ({}/{} on table, bg={}, +{})",
                                        decisionText, (int)deployAbility, title, isbOnTable, isbNeeded, isBattleground, (int)isbBonus);
                                } else {
                                    // V29.7: Non-ISB character — no penalty, just no ISB bonus.
                                    // ISB agents naturally win via their +200 bonus. Non-ISB still deployable
                                    // if no ISB agents are in hand.
                                    logger.info("V29.7 ISB: {} is non-ISB character — no bonus (ISB agents get +200 priority)",
                                        decisionText);
                                }
                            } catch (Exception e) {
                                logger.debug("V29.7 ISB: Error in ISB Operations scoring: {}", e.getMessage());
                            }
                        }

                        // === V29.7: ABILITY-BASED CHARACTER SCORING ===
                        // Higher ability characters are more valuable for location control.
                        // Ability >= 1 = presence, ability > opponent = control.
                        // Prefer deploying high-ability characters, especially at battleground sites.
                        if (isCharacter && deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (deployBp != null && deployBp.hasAbilityAttribute()) {
                                    float charAbility = deployBp.getAbility();
                                    // Scale bonus with ability: 0-2 = no bonus, 3-4 = small, 5+ = good, 6+ = great
                                    if (charAbility >= 6) {
                                        action.addReasoning("V29.7 HIGH ABILITY: Ability " + (int)charAbility + " — strong location control!", 50.0f);
                                    } else if (charAbility >= 5) {
                                        action.addReasoning("V29.7 ABILITY: Ability " + (int)charAbility + " — good for control", 25.0f);
                                    } else if (charAbility >= 3) {
                                        action.addReasoning("V29.7 ABILITY: Ability " + (int)charAbility, 5.0f);
                                    } else if (charAbility < 1) {
                                        action.addReasoning("V29.7 LOW ABILITY: Ability " + (int)charAbility + " — weak presence", -30.0f);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V29.7 ABILITY: Error checking ability: {}", e.getMessage());
                            }
                        }

                        // === V25: HUNT DOWN V — VADER PRIORITY DEPLOYMENT ===
                        // When running Hunt Down V, Vader MUST be deployed to flip the objective.
                        // Without Vader: deck bleeds 1 Force/turn from Visage, can't flip, can't cancel drains.
                        // If Vader is not on table:
                        //   - Vader gets massive deploy bonus (+300) to any battleground site
                        //   - Non-Vader characters get heavy penalty (-200) to save Force for Vader
                        //   - Exception: Inquisitors still get a small allowance since they help battle destiny
                        if (isCharacter && !earlySpyDetected && locObjAnalyzer != null
                            && locObjAnalyzer.isAnalyzed() && locObjAnalyzer.isHuntDownV()) {
                            try {
                                boolean vaderOnTable = locObjAnalyzer.isVaderOnTable(gameState, playerId);
                                boolean preFlip = !locObjAnalyzer.isFlipped();

                                // Check if the deploying card IS Vader
                                boolean deployingIsVader = false;
                                boolean deployingIsInquisitor = false;
                                if (deployingBlueprintId != null) {
                                    SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (deployBp != null) {
                                        deployingIsVader = com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.isVaderCard(deployBp);
                                        // Check if Inquisitor — they have "inquisitor" in title or characteristics
                                        String depTitle = deployBp.getTitle();
                                        String depGameText = deployBp.getGameText();
                                        if (depTitle != null) {
                                            String depTitleLower = depTitle.toLowerCase(Locale.ROOT);
                                            deployingIsInquisitor = depTitleLower.contains("inquisitor")
                                                || depTitleLower.contains("fifth brother")
                                                || depTitleLower.contains("seventh sister")
                                                || depTitleLower.contains("eighth brother")
                                                || depTitleLower.contains("second sister")
                                                || depTitleLower.contains("grand inquisitor");
                                        }
                                    }
                                }

                                if (deployingIsVader) {
                                    // VADER — massive bonus, especially to battleground sites
                                    float vaderBonus = 300.0f;
                                    // Check if this is a battleground site (needed for flip)
                                    boolean isBattleground = false;
                                    if (blueprint != null) {
                                        isBattleground = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                            && blueprint.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                    }
                                    if (isBattleground) {
                                        vaderBonus += 100.0f;  // Extra bonus for battleground — enables flip!
                                    }
                                    action.addReasoning("V25 HUNT DOWN: DEPLOY VADER! Critical for flip!" +
                                        (isBattleground ? " BATTLEGROUND = CAN FLIP!" : ""), vaderBonus);
                                    logger.warn("V25 HUNT DOWN: Vader deploy to {} — MASSIVE PRIORITY (+{})",
                                        title, (int)vaderBonus);
                                } else if (!vaderOnTable && preFlip) {
                                    // Non-Vader character when Vader isn't on table yet
                                    // Inquisitors get a lighter penalty since they help with battle destiny
                                    if (deployingIsInquisitor) {
                                        float inqPenalty = -80.0f;
                                        action.addReasoning("V25 HUNT DOWN: Inquisitor OK but save Force for Vader first!", inqPenalty);
                                        logger.warn("V25 HUNT DOWN: {} is Inquisitor — mild penalty while Vader not on table",
                                            decisionText);
                                    } else {
                                        float nonVaderPenalty = -200.0f;
                                        action.addReasoning("V25 HUNT DOWN: SAVE FORCE FOR VADER! He must be deployed first!", nonVaderPenalty);
                                        logger.warn("V25 HUNT DOWN: {} is NOT Vader — heavy penalty (-200) to save Force for Vader",
                                            decisionText);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V25 HUNT DOWN: Error in Hunt Down scoring: {}", e.getMessage());
                            }
                        }

                        // === V25: CLOUD CITY ABILITY-BASED SPREAD STRATEGY (TDIGWATT) ===
                        // When TDIGWATT is active, spreading across Cloud City locations maximizes:
                        //   - Cloud City Occupation: +1 damage per CC location occupied
                        //   - Dark Deal: +1 to each force drain at CC locations
                        //   - Force drains at each occupied location
                        // V25: Use ABILITY (not character count) to decide when a site is secure.
                        // ~6 ability = can draw battle destiny and hold the site.
                        // Vader alone (ability 6-7) can hold a site. Lando alone (ability 2) cannot.
                        // V24.15: Skip CC spread scoring for spies — they don't contribute while undercover
                        if (isCharacter && !earlySpyDetected && locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                            && locObjAnalyzer.needsBespinSystemPresence()
                            && locObjAnalyzer.isObjectiveRelevantLocation(title)) {
                            try {
                                final float ABILITY_SECURE_THRESHOLD = 6.0f;

                                // Get our ability at THIS location
                                float ourAbilityHere = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                    gameState, playerId, location);

                                // V24.13: Check if Lando is alone at this location — he's a high-value target!
                                boolean landoAloneHere = false;
                                int ourCharsAtThisLoc = 0;
                                java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(location);
                                if (cardsAtLoc != null) {
                                    for (PhysicalCard c : cardsAtLoc) {
                                        if (c != null && playerId.equals(c.getOwner()) &&
                                            c.getBlueprint() != null &&
                                            c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            ourCharsAtThisLoc++;
                                            String charTitle = c.getTitle();
                                            if (ourCharsAtThisLoc == 1 && charTitle != null &&
                                                charTitle.toLowerCase(java.util.Locale.ROOT).contains("lando")) {
                                                landoAloneHere = true;
                                            }
                                        }
                                    }
                                }
                                // Reset lando flag if more than 1 char
                                if (ourCharsAtThisLoc > 1) landoAloneHere = false;

                                // Check ALL objective-relevant locations for ability status
                                int locsEmpty = 0;
                                int locsInsecure = 0;  // Have presence but ability < threshold
                                int locsSecure = 0;    // Ability >= threshold
                                java.util.List<PhysicalCard> allLocs = gameState.getLocationsInOrder();
                                for (PhysicalCard checkLoc : allLocs) {
                                    if (checkLoc == null || checkLoc.getTitle() == null) continue;
                                    if (!locObjAnalyzer.isObjectiveRelevantLocation(checkLoc.getTitle())) continue;
                                    float abilityThere = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                        gameState, playerId, checkLoc);
                                    if (abilityThere <= 0) {
                                        locsEmpty++;
                                    } else if (abilityThere < ABILITY_SECURE_THRESHOLD) {
                                        locsInsecure++;
                                    } else {
                                        locsSecure++;
                                    }
                                }

                                // Apply ability-based spread scoring
                                if (landoAloneHere) {
                                    // V24.13: LANDO IS ALONE — critical priority to reinforce!
                                    action.addReasoning("V24.13 LANDO SUPPORT: Lando is ALONE here — MUST reinforce!", 250.0f);
                                    logger.warn("V24.13 LANDO ALONE: {} — Lando needs backup! (+250)", title);
                                } else if (ourAbilityHere > 0 && ourAbilityHere < ABILITY_SECURE_THRESHOLD) {
                                    // REINFORCE: This location has presence but isn't secure yet
                                    float deficit = ABILITY_SECURE_THRESHOLD - ourAbilityHere;
                                    float reinforceBonus = 100.0f + (deficit * 15.0f);
                                    action.addReasoning("V25 REINFORCE: Site has ability " + String.format("%.0f", ourAbilityHere)
                                        + " — need " + String.format("%.0f", ABILITY_SECURE_THRESHOLD) + " to hold!", reinforceBonus);
                                    logger.warn("V25 ABILITY: {} has ability {} (need {}) — REINFORCE (+{})",
                                        title, String.format("%.0f", ourAbilityHere), String.format("%.0f", ABILITY_SECURE_THRESHOLD), (int)reinforceBonus);
                                } else if (ourAbilityHere <= 0) {
                                    // EMPTY: New unoccupied CC location — only spread if other locations are secure
                                    if (locsInsecure > 0) {
                                        // Other locations need ability reinforcement first
                                        action.addReasoning("V25 SPREAD: New CC location but " + locsInsecure + " site(s) need more ability first", 40.0f);
                                        logger.info("V25 ABILITY: {} unoccupied but {} sites insecure — moderate priority", title, locsInsecure);
                                    } else {
                                        // All occupied locations are secure — spread to new!
                                        action.addReasoning("V25 SPREAD: All held sites have 6+ ability — spread for more occupation damage!", 120.0f);
                                        logger.warn("V25 ABILITY: {} unoccupied, all {} sites secure — SPREAD (+120)", title, locsSecure);
                                    }
                                } else {
                                    // SECURE: This location already has 6+ ability
                                    if (locsInsecure > 0 || locsEmpty > 0) {
                                        // Other locations need attention — mild penalty for over-stacking
                                        action.addReasoning("V25 SECURE: Site already has " + String.format("%.0f", ourAbilityHere)
                                            + " ability — other sites need help", -40.0f);
                                        logger.info("V25 ABILITY: {} has ability {} (secure), {} sites need attention", title,
                                            String.format("%.0f", ourAbilityHere), (locsInsecure + locsEmpty));
                                    } else {
                                        // ALL locations are secure — extra stacking OK
                                        action.addReasoning("V25 SECURE: All CC sites have 6+ ability — extra defense OK", 20.0f);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V25: Could not evaluate CC ability spread: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // V59 UNIVERSAL SPY SCORING — runs regardless of ObjectiveAnalyzer state.
                        // FIXES Issue #1 from peaceful-pike replay: Jyn Erso deployed to empty
                        // Upper Chamber (+165) instead of Entrance where opponent drains 2/turn,
                        // because the spy-aware scoring at line ~2201 was trapped inside
                        // `if (deployObjAnalyzer.isAnalyzed())`. When Rando's deck doesn't have
                        // an analyzed objective (e.g., "Like My Father Before Me" variants),
                        // spy placement fell back to generic icon-count scoring which ties every BG.
                        // This block scores spies BEFORE the objective-gated block and sets a flag
                        // to prevent double-counting downstream.
                        // =====================================================
                        boolean spyScoringApplied = false;
                        if (earlySpyDetected && game != null && location != null) {
                            try {
                                float ourPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, playerId, false, false);
                                String opp = game.getOpponent(playerId);
                                float oppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, opp, false, false);

                                if (oppPwr > 0 && ourPwr == 0) {
                                    // BEST: Opponent actively draining/occupying — block them!
                                    action.addReasoning("V59 SPY UNIVERSAL: Opp has power " + (int)oppPwr
                                        + ", we have 0 — IDEAL spy site, blocks their drain!", 600.0f);
                                    logger.warn("V59 SPY UNIVERSAL: {} — opp {}, us 0 — IDEAL! (+600)", title, (int)oppPwr);
                                } else if (oppPwr > 0 && ourPwr > 0) {
                                    // Both sides — spy would block our own drain while undercover
                                    action.addReasoning("V59 SPY UNIVERSAL: Both sides present at " + title
                                        + " — spy blocks OWN drain while undercover", -200.0f);
                                    logger.warn("V59 SPY UNIVERSAL: {} — opp {}, us {} — hurts us (-200)",
                                        title, (int)oppPwr, (int)ourPwr);
                                } else if (oppPwr == 0 && ourPwr > 0) {
                                    // Only us — spy blocks OUR drain, catastrophic
                                    action.addReasoning("V59 SPY UNIVERSAL: Only we have presence at " + title
                                        + " — spy would block OWN drain!", -2000.0f);
                                    logger.warn("V59 SPY UNIVERSAL: {} — only us {} — BLOCKED (-2000)",
                                        title, (int)ourPwr);
                                } else {
                                    // Empty — no drain to block
                                    action.addReasoning("V59 SPY UNIVERSAL: " + title
                                        + " is empty — no drain to block", -300.0f);
                                    logger.warn("V59 SPY UNIVERSAL: {} — empty, wasted spy (-300)", title);
                                }
                                spyScoringApplied = true;
                            } catch (Exception e) {
                                logger.debug("V59 SPY UNIVERSAL: Error: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // CRITICAL: Check power at location
                        // Don't deploy characters to contested locations we're losing!
                        // V22: Prefer own objective locations over opponent locations
                        // V24.15: EXEMPT SPIES from contest penalties!
                        // Spies deploy undercover — they don't fight battles.
                        // Contest penalty is meaningless for them. Their scoring is
                        // handled by the V24.14B spy scoring block below.
                        // =====================================================
                        if (isCharacter && game != null && !earlySpyDetected) {
                            try {
                                float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, playerId, false, false);
                                String opponent = game.getOpponent(playerId);
                                float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, opponent, false, false);

                                if (theirPower > 0) {
                                    float powerDiff = theirPower - ourPower;
                                    if (ourPower < theirPower) {
                                        // V22.3: Contested penalty SCALES with how badly we're losing
                                        // Must be strong enough to override objective bonus (+200)
                                        // when deploying would just feed the opponent cards
                                        // V22.3 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c): tier
                                        // boundaries were exclusive (>), so a gap of EXACTLY 10 (0 vs 10,
                                        // Yoda into Kylo+Mara) only drew -150 while the contest-bonus stack
                                        // (+830) dwarfed it. Boundaries now inclusive (>=): gap 10 → -250.
                                        float contestPenalty = -80.0f;
                                        if (powerDiff >= 5) contestPenalty = -150.0f;   // Significantly outgunned
                                        if (powerDiff >= 10) contestPenalty = -250.0f;  // Massively outgunned — overrides obj bonus
                                        if (powerDiff >= 15) contestPenalty = -350.0f;  // Suicide — hard no

                                        // Check if deploying THIS card would actually close the gap meaningfully
                                        Float deployPower = (blueprint != null && blueprint.hasPowerAttribute()) ? blueprint.getPower() : null;
                                        float addedPower = (deployPower != null) ? deployPower : 0;
                                        if (addedPower > 0 && (ourPower + addedPower) >= theirPower) {
                                            // This character would tip the balance — reduce penalty
                                            contestPenalty = Math.min(contestPenalty + 100.0f, -20.0f);
                                            action.addReasoning("V22.3: Would tip balance at contested location (" +
                                                (int)(ourPower + addedPower) + " vs " + (int)theirPower + ")", 0.0f);
                                        }

                                        // V22.7: If this is an objective-critical location, reduce the
                                        // contest penalty — we NEED to fight here even at a disadvantage
                                        if (locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                                            && locObjAnalyzer.isObjectiveRelevantLocation(title)) {
                                            float objOverride = Math.min(200.0f, Math.abs(contestPenalty) * 0.6f);
                                            action.addReasoning("V22.7: Objective-critical location — must contest!", objOverride);
                                            logger.warn("V22.7 OBJ CONTEST: {} is objective-critical — reducing contest penalty by {}",
                                                title, (int)objOverride);
                                        }

                                        if (plannedTargetId == null || !cardId.equals(plannedTargetId)) {
                                            action.addReasoning("CONTESTED & LOSING (" + (int)ourPower + " vs " + (int)theirPower +
                                                " power, gap=" + (int)powerDiff + ")", contestPenalty);
                                            logger.warn("V22.3 CONTEST: {} at {} losing {}-vs-{} penalty={}",
                                                title, (int)ourPower, (int)theirPower, contestPenalty);
                                        }
                                    } else if (ourPower > theirPower + 4) {
                                        // We're already winning big - don't need more here
                                        action.addReasoning("Already winning big here", -20.0f);
                                    } else if (ourPower >= theirPower) {
                                        // We're winning or tied - reinforce is reasonable
                                        action.addReasoning("Can reinforce winning position", 10.0f);
                                    }
                                } else {
                                    // No opponent power - uncontested
                                    if (ourPower == 0) {
                                        // V29: CHARACTER CONCENTRATION — don't deploy alone to empty locations
                                        // if there are solo friendlies at other locations that need backup.
                                        // Spreading characters thin gets them killed one by one.
                                        int soloFriendlyLocations = 0;
                                        int contestedSoloLocations = 0;
                                        try {
                                            java.util.List<PhysicalCard> allLocations = gameState.getTopLocations();
                                            if (allLocations != null) {
                                                for (PhysicalCard loc : allLocations) {
                                                    if (loc == null || loc.equals(location)) continue;
                                                    float locOurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                        gameState, loc, playerId, false, false);
                                                    if (locOurPower > 0 && locOurPower <= 5) {
                                                        // Might be a solo character — count them
                                                        int charsHere = 0;
                                                        java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(loc);
                                                        if (cardsAtLoc != null) {
                                                            for (PhysicalCard c : cardsAtLoc) {
                                                                if (c != null && playerId.equals(c.getOwner())
                                                                    && c.getBlueprint() != null
                                                                    && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                                    charsHere++;
                                                                }
                                                            }
                                                        }
                                                        if (charsHere == 1) {
                                                            soloFriendlyLocations++;
                                                            String opponentId = game.getOpponent(playerId);
                                                            float locTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                                gameState, loc, opponentId, false, false);
                                                            if (locTheirPower > 0) contestedSoloLocations++;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Fallback — assume no solo friendlies
                                        }

                                        if (contestedSoloLocations > 0) {
                                            // Solo friendlies under threat! DON'T spread to empty locations.
                                            // V29b: Increased from -80 to -200 — Rando was still spreading thin.
                                            float concPenalty = -200.0f * contestedSoloLocations;
                                            action.addReasoning("V29 CONCENTRATE: " + contestedSoloLocations
                                                + " solo friendly(s) CONTESTED — reinforce them, don't spread!", concPenalty);
                                            logger.warn("V29 CONCENTRATE: Empty loc {} but {} contested solo friendlies — penalty {}", title, contestedSoloLocations, concPenalty);
                                        } else if (soloFriendlyLocations > 0) {
                                            // Solo friendlies need backup even if uncontested (opponent could move in)
                                            // V29b: Increased from -40 to -100
                                            float concPenalty = -100.0f * soloFriendlyLocations;
                                            action.addReasoning("V29 CONCENTRATE: " + soloFriendlyLocations
                                                + " solo friendly(s) need reinforcement — don't spread thin!", concPenalty);
                                            logger.info("V29 CONCENTRATE: Empty loc {} but {} solo friendlies elsewhere — penalty {}", title, soloFriendlyLocations, concPenalty);
                                        } else {
                                            // No vulnerable solo friendlies — safe to establish
                                            action.addReasoning("Establish at empty location (no solo friendlies elsewhere)", 20.0f);
                                        }
                                    }
                                }

                                // V22.4 + V67bn: LONELY CHARACTER REINFORCEMENT
                                //
                                // V67bn (Steve, 2026-05-11): Extended the OLD V29 REINFORCE rule
                                // beyond its `ourPower <= 5` weakness gate. The old gate missed
                                // STRONG-but-outgunned solo chars — Vader (power 6) alone vs 2
                                // Jedi (power 8-13) failed the gate, so Yularen got pulled to a
                                // spy site (+940) instead of joining Vader (+180). Steve's rule:
                                // "deploy them with Vader and overpower the 2 jedi instead of
                                //  spreading to bait Rey."
                                //
                                // V67bn fires whenever there's exactly ONE friendly char at the
                                // destination AND the opponent's power exceeds ours by 4+ (same
                                // deficit threshold V67bj uses). Bonus magnitude +800 dominates
                                // V24.14B SPY +300 and V67as OPEN-FRONT +300, ensuring REINFORCE
                                // wins over SPREAD when an ally needs help.
                                //
                                // V29 REINFORCE (weak char, no opponent or moderate opponent) kept
                                // as the secondary rule for the original case.
                                if (ourPower > 0) {
                                    int ourCharsHere = 0;
                                    try {
                                        java.util.List<PhysicalCard> cardsHere = gameState.getCardsAtLocation(location);
                                        if (cardsHere != null) {
                                            for (PhysicalCard c : cardsHere) {
                                                if (c != null && playerId.equals(c.getOwner())) {
                                                    SwccgCardBlueprint cBp = c.getBlueprint();
                                                    if (cBp != null && cBp.getCardCategory() == CardCategory.CHARACTER) {
                                                        ourCharsHere++;
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Fallback
                                    }

                                    // 2026-06-03 DEFICIT UPPER CAP (Steve, Mustafar/Jabba replay):
                                    // "Rando deployed guys to fight me but very underpowered."
                                    // Replay: Audience Chamber, our 3 vs opp 12, deficit 9 → V67bn
                                    // fired +800 "REINFORCE OUTGUNNED" → Rando piled MORE power-3
                                    // chars in, each ate forfeits in the overflow battle without
                                    // closing the gap. Adding +3 to a -9 deficit still loses by 6
                                    // AND costs the extra char's forfeit/ability — net loss vs
                                    // just losing the original battle.
                                    // Braveheart rationale only holds when reinforcement can
                                    // PLAUSIBLY close the gap: 1 mid-power char (~4) absorbs a
                                    // 4-deficit, maybe a 5. Beyond that, the gap is unclosable
                                    // and reinforcing only hands the opponent more forfeits. Cap
                                    // the gate at deficit ≤ 5: trapped + reasonably close → pile
                                    // on; trapped + hopelessly outgunned → don't double down,
                                    // let the existing battle just resolve.
                                    float v67bnDeficit = theirPower - ourPower;
                                    boolean v67bnOutgunned = v67bnDeficit >= 4f && v67bnDeficit <= 5f;
                                    // V67bu (Steve, 2026-05-11): extend V67bn to ANY committed-friendly
                                    // count (was solo-only). Steve's "Braveheart" rule: when chars are
                                    // already committed to an outgunned site AND can't escape, pile on
                                    // reinforcements to MINIMIZE overflow damage — 15+ force overflow is
                                    // game-ending, so even losing by less wins the war of attrition.
                                    //
                                    // Escape-route check: don't reinforce if outgunned chars can flee.
                                    //   1. Adjacent site on same planet has Rando friendlies (consolidate)
                                    //   2. Same parent system has Rando's starship (shuttle aboard)
                                    // If escape exists → Move evaluator (V67au) handles retreat next phase.
                                    boolean v67buCanEscape = false;
                                    if (ourCharsHere >= 1 && v67bnOutgunned) {
                                        try {
                                            String locTitleLower = location.getTitle() != null
                                                ? location.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                            // Extract planet prefix (e.g., "cloud city" from "cloud city: upper walkway")
                                            String planetPrefix = locTitleLower.contains(":")
                                                ? locTitleLower.substring(0, locTitleLower.indexOf(":")).trim()
                                                : locTitleLower;
                                            // Escape case 1: same-planet site has Rando friendlies
                                            // Escape case 2: same parent (e.g. system or matching planet) has Rando starship
                                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                                if (pc == null || pc.getBlueprint() == null) continue;
                                                if (!playerId.equals(pc.getOwner())) continue;
                                                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                                CardCategory pcCat = pc.getBlueprint().getCardCategory();
                                                // Friendly char at adjacent same-planet site
                                                if (pcCat == CardCategory.CHARACTER) {
                                                    PhysicalCard pcLoc = pc.getAtLocation();
                                                    if (pcLoc == null || pcLoc == location) continue;
                                                    String pcLocTitle = pcLoc.getTitle();
                                                    if (pcLocTitle == null) continue;
                                                    String pcLocLower = pcLocTitle.toLowerCase(java.util.Locale.ROOT);
                                                    if (!planetPrefix.isEmpty() && pcLocLower.startsWith(planetPrefix)) {
                                                        v67buCanEscape = true;
                                                        break;
                                                    }
                                                }
                                                // Friendly starship at same parent system (shuttle aboard)
                                                if (pcCat == CardCategory.STARSHIP) {
                                                    PhysicalCard pcLoc = pc.getAtLocation();
                                                    if (pcLoc == null) continue;
                                                    String pcLocTitle = pcLoc.getTitle();
                                                    if (pcLocTitle == null) continue;
                                                    String pcLocLower = pcLocTitle.toLowerCase(java.util.Locale.ROOT);
                                                    if (!planetPrefix.isEmpty() && pcLocLower.startsWith(planetPrefix)) {
                                                        v67buCanEscape = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (Exception eEsc) {
                                            logger.debug("V67bu escape-check error: {}", eEsc.getMessage());
                                        }
                                    }
                                    if (ourCharsHere >= 1 && v67bnOutgunned && !v67buCanEscape) {
                                        // V67bn (extended via V67bu) — committed-and-trapped friendlies
                                        // → BRAVEHEART: reinforce to minimize overflow damage.
                                        action.addReasoning(String.format(
                                            "V67bn REINFORCE OUTGUNNED (Braveheart): %d friendly char(s) at %s (our %d vs opp %d, deficit %d) — NO ESCAPE, DEPLOY HERE to minimize overflow!",
                                            ourCharsHere, title, (int)ourPower, (int)theirPower, (int)(theirPower-ourPower)),
                                            800.0f);
                                        logger.warn("V67bn REINFORCE OUTGUNNED: dest={} chars={} our={} opp={} deficit={} no-escape → +800",
                                            title, ourCharsHere, (int)ourPower, (int)theirPower, (int)(theirPower-ourPower));
                                    } else if (ourCharsHere >= 1 && v67bnOutgunned && v67buCanEscape) {
                                        // V67bu — escape available, let Move evaluator handle it
                                        logger.info("V67bu ESCAPE AVAILABLE at {} (our {} vs opp {}) — skip reinforce, Move evaluator will retreat",
                                            title, (int)ourPower, (int)theirPower);
                                    } else if (ourPower <= 5f && ourCharsHere == 1) {
                                        // V29 REINFORCE (legacy) — weak char alone, opponent moderate or absent
                                        float reinforceBonus = 150.0f;
                                        if (theirPower > 0) reinforceBonus = 250.0f;
                                        action.addReasoning("V29 REINFORCE SOLO CHARACTER (power " +
                                            (int)ourPower + ") - don't leave them alone!", reinforceBonus);
                                        logger.info("V29 REINFORCE: Solo char at {} (power {}), opponent power {}, bonus={}",
                                            title, (int)ourPower, (int)theirPower, reinforceBonus);
                                    } else if (ourCharsHere == 2 && theirPower > ourPower * 1.5f) {
                                        action.addReasoning("V29: Reinforce outnumbered pair at " + title, 100.0f);
                                    }
                                }

                                // === V29.5: GENERAL BUDDY SYSTEM — PREFER OWN LOCATIONS ===
                                // Characters should prefer deploying to locations they OWN or have
                                // friendly presence at. Deploying alone to opponent-controlled empty
                                // locations is bad — the opponent will likely reinforce and kill you.
                                // This applies to ALL decks, not just TDIGWATT.
                                //
                                // V29.6: EMPTY TABLE AWARENESS — If we have NO friendly characters
                                // anywhere on the table, someone has to go first! Reduce penalties
                                // so Rando doesn't stall. Still prefer own locations, but don't
                                // refuse to deploy just because only opponent locations exist.
                                if (isCharacter && location != null && playerId != null) {
                                    try {
                                        // Check location ownership
                                        String locOwner = location.getOwner();
                                        String opponentIdBuddy = gameState.getOpponent(playerId);
                                        boolean isOurLocation = playerId.equals(locOwner);
                                        boolean isOpponentLocation = opponentIdBuddy != null && opponentIdBuddy.equals(locOwner);

                                        // Count friendly and opponent characters at this location
                                        int friendlyCharsHereBuddy = 0;
                                        int opponentCharsHereBuddy = 0;
                                        java.util.List<PhysicalCard> buddyCards = gameState.getCardsAtLocation(location);
                                        if (buddyCards != null) {
                                            for (PhysicalCard bc : buddyCards) {
                                                if (bc != null && bc.getBlueprint() != null
                                                    && bc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                    if (playerId.equals(bc.getOwner())) {
                                                        friendlyCharsHereBuddy++;
                                                    } else {
                                                        opponentCharsHereBuddy++;
                                                    }
                                                }
                                            }
                                        }

                                        // V29.6: Count TOTAL friendly characters on the entire table.
                                        // If zero, this is the FIRST deploy — penalties must be softer.
                                        int totalFriendlyCharsOnTable = 0;
                                        try {
                                            java.util.List<PhysicalCard> allLocations = gameState.getTopLocations();
                                            if (allLocations != null) {
                                                for (PhysicalCard loc : allLocations) {
                                                    java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(loc);
                                                    if (cardsAtLoc != null) {
                                                        for (PhysicalCard pc : cardsAtLoc) {
                                                            if (pc != null && playerId.equals(pc.getOwner())
                                                                && pc.getBlueprint() != null
                                                                && pc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                                totalFriendlyCharsOnTable++;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V29.6 BUDDY: Error counting total friendlies: {}", e.getMessage());
                                        }
                                        boolean emptyTable = (totalFriendlyCharsOnTable == 0);

                                        if (isOurLocation) {
                                            // Deploying to our own location — small bonus
                                            action.addReasoning("V29.5 BUDDY: Own location — home field advantage", 40.0f);
                                        } else if (isOpponentLocation) {
                                            if (emptyTable) {
                                                // V29.6: Nobody on the table yet — someone HAS to go first!
                                                // Mild preference against opponent locations, but don't block.
                                                // Own locations still get +40, so they're preferred if available.
                                                action.addReasoning("V29.6 BUDDY: Opponent's location but empty table — must deploy somewhere!", -20.0f);
                                                logger.info("V29.6 BUDDY: {} — empty table, mild penalty -20 for opponent location (would be -150 normally)", title);
                                            } else if (friendlyCharsHereBuddy == 0 && opponentCharsHereBuddy == 0) {
                                                // Deploying ALONE to an EMPTY opponent location — risky!
                                                // Opponent will likely reinforce and we'll be outnumbered.
                                                action.addReasoning("V29.5 BUDDY: Opponent's location, deploying ALONE — risky!", -150.0f);
                                                logger.warn("V29.5 BUDDY: {} — deploying alone to empty OPPONENT location — penalty -150", title);
                                            } else if (friendlyCharsHereBuddy == 0 && opponentCharsHereBuddy > 0) {
                                                // Deploying ALONE to opponent location WITH enemies — very dangerous!
                                                // (Contest penalty already applies, but this stacks for extra deterrence)
                                                action.addReasoning("V29.5 BUDDY: Opponent's location with enemies, NO friendlies — dangerous!", -100.0f);
                                                logger.warn("V29.5 BUDDY: {} — deploying alone to ENEMY-OCCUPIED opponent location — penalty -100", title);
                                            } else if (friendlyCharsHereBuddy > 0) {
                                                // We have friendlies here — slightly less risky
                                                action.addReasoning("V29.5 BUDDY: Opponent's location but friendlies present", 10.0f);
                                            }
                                        }

                                        // === V113: SOLO HIGH-ABILITY CHARACTER VULNERABILITY ===
                                        // V29.5 only penalizes solo deploys to OPPONENT locations.
                                        // V113 extends this to ANY location: if an ability-3+
                                        // character would be alone at the target site, the opponent
                                        // can deploy 2+ next turn and overwhelm them. Directly
                                        // addresses "Dengar alone at Xizor's Palace → Anakin +
                                        // Chewie demolish him" pattern.
                                        if (friendlyCharsHereBuddy == 0 && !emptyTable) {
                                            float depAbility113 = 0;
                                            try {
                                                if (deployingBlueprintId != null) {
                                                    SwccgCardBlueprint depBp113 = getBlueprintFromId(context, deployingBlueprintId);
                                                    if (depBp113 != null) depAbility113 = depBp113.getAbility();
                                                }
                                            } catch (Exception e113) { /* ignore */ }
                                            if (depAbility113 >= 3.0f) {
                                                float v113Penalty = -300.0f;
                                                action.addReasoning(String.format(
                                                    "V113 SOLO VULNERABILITY: %s (ability %.0f) alone at %s — opponent can overwhelm next turn!",
                                                    deployingCardName, depAbility113, location.getTitle()), v113Penalty);
                                                logger.warn("V113 SOLO: {} ability {} alone at {} — penalty {}",
                                                    deployingCardName, (int)depAbility113,
                                                    location.getTitle(), (int)v113Penalty);
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.debug("V29.5 BUDDY: Error checking location ownership: {}", e.getMessage());
                                    }
                                }

                                // === V29.7: BATTLEGROUND PREFERENCE FOR CHARACTER DEPLOYMENT ===
                                // Characters prefer deploying to battleground sites for meaningful
                                // force drains and battles. BONUS for battlegrounds, but only apply
                                // a penalty for non-battlegrounds when battleground alternatives exist.
                                // V29.7 FIX: Many decks (ISB, TDIGWATT) operate at non-BG interior
                                // sites. Penalizing non-BG when NO BG sites are on the table blocks
                                // ALL deploys! Only penalize when the player has BG options.
                                if (isCharacter && location != null && game != null && gameState != null) {
                                    try {
                                        boolean isBattlegroundSite = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                        if (isBattlegroundSite) {
                                            // Strong bonus — battlegrounds are where the action happens
                                            action.addReasoning("V29.7 BATTLEGROUND: Deploy to battleground site — force drains and battles!", 80.0f);
                                        } else {
                                            // V29.7: Check if ANY battleground sites are accessible before penalizing.
                                            // If no BG sites exist on the table, don't penalize — deploy somewhere!
                                            boolean anyBattlegroundExists = false;
                                            try {
                                                for (PhysicalCard bgLoc : gameState.getTopLocations()) {
                                                    if (bgLoc != null) {
                                                        boolean bgCheck = game.getModifiersQuerying().isBattleground(gameState, bgLoc, null);
                                                        if (bgCheck) {
                                                            anyBattlegroundExists = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception bgE) { /* ignore */ }

                                            if (anyBattlegroundExists) {
                                                // V67ah (Steve, 2026-05-04): 'Deploying to non
                                                // battleground sites is mostly useless.' Old -60 was
                                                // too weak. But Sidious-to-The-Works is OK as drain
                                                // staging when the site has opp icons. Tiered penalty:
                                                //   - Non-BG with drain icons (opp force):  -100
                                                //     (acceptable first-character drain post; V67ag
                                                //     adds another -300 if a friendly is already there)
                                                //   - Non-BG with zero opp icons (truly useless): -350
                                                //     (no battles AND no drain — pure waste)
                                                int v67ahOppIcons = 0;
                                                try {
                                                    com.gempukku.swccgo.common.Side mySide67ah = context.getSide();
                                                    com.gempukku.swccgo.game.SwccgCardBlueprint locBp67ah =
                                                        location.getBlueprint();
                                                    if (locBp67ah != null) {
                                                        v67ahOppIcons = (mySide67ah == com.gempukku.swccgo.common.Side.LIGHT)
                                                            ? locBp67ah.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                                            : locBp67ah.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                                    }
                                                } catch (Exception e) { /* ignore */ }
                                                if (v67ahOppIcons > 0) {
                                                    action.addReasoning(
                                                        "V67ah NON-BG (with drain): mostly useless except as drain staging — mild penalty",
                                                        -100.0f);
                                                } else {
                                                    action.addReasoning(
                                                        "V67ah NON-BG (no drain): truly useless — no battles AND no drain potential",
                                                        -350.0f);
                                                }
                                            } else {
                                                // No BG on table — don't penalize, just note it
                                                action.addReasoning("V29.7 BATTLEGROUND: Non-BG but no battlegrounds on table — acceptable", 0.0f);
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.debug("V29.7 BATTLEGROUND: Error checking battleground status: {}", e.getMessage());
                                    }
                                }

                                // === V24.3B: DR. EVAZAN WEAPON COMBO — DEPLOY LOCATION PREFERENCE ===
                                // Deploy Evazan to sites with weapon chars, and weapon chars to sites with Evazan.
                                // Evazan converts weapon hits into immediate character loss — devastating combo.
                                if (isCharacter && decisionText != null) {
                                    boolean deployingEvazan = decisionText.contains("evazan");
                                    boolean deployingWeaponChar = (decisionText.contains("maul") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("vader") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("mara") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("jade") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("aurra") && decisionText.contains("blaster"))
                                        || (decisionText.contains("sing") && decisionText.contains("blaster"));

                                    if (deployingEvazan || deployingWeaponChar) {
                                        // Scan cards at this location for combo partner
                                        boolean comboPartnerHere = false;
                                        try {
                                            java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(location);
                                            if (cardsAtLoc != null) {
                                                for (PhysicalCard c : cardsAtLoc) {
                                                    if (c == null || !playerId.equals(c.getOwner())) continue;
                                                    String cTitle = c.getTitle();
                                                    if (cTitle == null) continue;
                                                    String cTitleLower = cTitle.toLowerCase();

                                                    if (deployingEvazan) {
                                                        // Looking for weapon characters
                                                        if ((cTitleLower.contains("maul") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("vader") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("mara") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("jade") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("aurra") && cTitleLower.contains("blaster"))
                                                            || (cTitleLower.contains("sing") && cTitleLower.contains("blaster"))) {
                                                            comboPartnerHere = true;
                                                            break;
                                                        }
                                                    } else {
                                                        // Deploying weapon char — looking for Evazan
                                                        if (cTitleLower.contains("evazan")) {
                                                            comboPartnerHere = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) { /* ignore */ }

                                        if (comboPartnerHere) {
                                            action.addReasoning("V24.3 EVAZAN COMBO: Deploy here — combo partner at this site for weapon kill combo!", 200.0f);
                                            logger.warn("V24.3 EVAZAN COMBO: {} — combo partner found at {} (+200)", decisionText, title);
                                        }
                                    }
                                }

                                // === V24.10: LANDO DEPLOY LOCATION — PREFER DINING ROOM ===
                                // Dining Room is Lando's optimal deploy site:
                                // - I'm Sorry pulls Dining Room which chain-pulls Lando
                                // - Lando at Dining Room establishes occupation for TDIGWATT engine
                                // - Lando can then MOVE to other CC sites at start of control phase
                                if (isCharacter && decisionText != null && decisionText.contains("lando")) {
                                    String locTitleLower = title != null ? title.toLowerCase(java.util.Locale.ROOT) : "";
                                    if (locTitleLower.contains("dining room")) {
                                        action.addReasoning("V24.10 LANDO TO DINING ROOM: Optimal deploy — establishes occupation, can move to other sites!", 300.0f);
                                        logger.warn("V24.10 LANDO: Dining Room +300 — ideal deploy location for Lando!");
                                    } else if (locTitleLower.contains("cloud city") || locTitleLower.contains("upper walkway")
                                               || locTitleLower.contains("carbonite") || locTitleLower.contains("security tower")
                                               || locTitleLower.contains("platform") || locTitleLower.contains("lower corridor")) {
                                        // Other CC sites are OK but not ideal — Lando can move here later
                                        action.addReasoning("V24.10 LANDO: CC site but not Dining Room — Lando can move here later, deploy to Dining Room first!", -50.0f);
                                        logger.warn("V24.10 LANDO: {} is CC but not Dining Room — mild penalty (-50)", title);
                                    }

                                    // === V25: LANDO ALONE PROTECTION ===
                                    // NEVER deploy Lando to a CC site where he'd be alone and unprotected.
                                    // Lando alone (ability 2, power 3) is an easy kill for any Jedi.
                                    // Rey killed Lando alone EVERY TURN in testing — catastrophic Force losses.
                                    // Only deploy Lando if:
                                    //   (a) friendlies already at the site, OR
                                    //   (b) we have characters in hand we can deploy alongside him, OR
                                    //   (c) Turn 1 and opponent has no CC presence yet (early establish OK)
                                    if (game != null && gameState != null) {
                                        try {
                                            int friendlyCharsAtSite = 0;
                                            int opponentCharsAtAnyCCSite = 0;
                                            int charsInHand = 0;
                                            String opponentId = gameState.getOpponent(playerId);

                                            // Count friendlies AND opponents at THIS location
                                            int opponentCharsAtThisSite = 0;
                                            java.util.List<PhysicalCard> siteCards = gameState.getCardsAtLocation(location);
                                            if (siteCards != null) {
                                                for (PhysicalCard sc : siteCards) {
                                                    if (sc != null && sc.getBlueprint() != null &&
                                                        sc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                        if (playerId.equals(sc.getOwner())) {
                                                            friendlyCharsAtSite++;
                                                        } else if (opponentId != null && opponentId.equals(sc.getOwner())) {
                                                            opponentCharsAtThisSite++;
                                                        }
                                                    }
                                                }
                                            }

                                            // Count characters in hand (potential protectors)
                                            java.util.List<PhysicalCard> hand = gameState.getHand(playerId);
                                            if (hand != null) {
                                                for (PhysicalCard hc : hand) {
                                                    if (hc != null && hc.getBlueprint() != null &&
                                                        hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                        charsInHand++;
                                                    }
                                                }
                                            }

                                            // Check opponent presence at CC sites
                                            for (PhysicalCard checkLoc : gameState.getLocationsInOrder()) {
                                                if (checkLoc == null || checkLoc.getTitle() == null) continue;
                                                if (checkLoc == location) continue; // skip this site
                                                String checkLocLower = checkLoc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                                boolean isCCsite = checkLocLower.contains("cloud city") || checkLocLower.contains("upper walkway")
                                                    || checkLocLower.contains("carbonite") || checkLocLower.contains("security tower")
                                                    || checkLocLower.contains("dining room") || checkLocLower.contains("platform")
                                                    || checkLocLower.contains("lower corridor");
                                                if (!isCCsite) continue;
                                                java.util.List<PhysicalCard> ccCards = gameState.getCardsAtLocation(checkLoc);
                                                if (ccCards != null) {
                                                    for (PhysicalCard cc : ccCards) {
                                                        if (cc != null && cc.getOwner() != null) {
                                                            if (opponentId != null && opponentId.equals(cc.getOwner()) &&
                                                                cc.getBlueprint() != null &&
                                                                cc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                                opponentCharsAtAnyCCSite++;
                                                            }
                                                            // (could track friendly chars at other CC sites here if needed)
                                                        }
                                                    }
                                                }
                                            }

                                            boolean hasProtection = friendlyCharsAtSite > 0;
                                            boolean canDeployProtector = charsInHand >= 1;
                                            boolean opponentAtThisSite = opponentCharsAtThisSite > 0;
                                            boolean opponentThreatens = (opponentCharsAtAnyCCSite + opponentCharsAtThisSite) > 0;

                                            if (opponentAtThisSite && !hasProtection) {
                                                // V41: HARD BLOCK — opponent characters AT THIS SITE and no friendlies!
                                                // Lando deployed alone into Luke+Rey = instant death.
                                                action.addReasoning("V41 LANDO INTO ENEMY: " + opponentCharsAtThisSite
                                                    + " opponents at " + title + " — Lando dies instantly! BLOCKED!", -9999.0f);
                                                logger.warn("V41 LANDO INTO ENEMY: {} opponents at {} — HARD BLOCK! Lando would die!",
                                                    opponentCharsAtThisSite, title);
                                            } else if (hasProtection) {
                                                // Friendlies at site — Lando is safe
                                                logger.info("V25 LANDO: {} — {} friendlies here — safe to deploy", title, friendlyCharsAtSite);
                                            } else if (!canDeployProtector) {
                                                action.addReasoning("V47 LANDO ALONE BLOCK: No protection at " + title
                                                    + " and no characters in hand — Lando dies alone!", -9999.0f);
                                                logger.warn("V47 LANDO ALONE: {} — no friendlies, no hand chars — HARD BLOCK!", title);
                                            } else if (opponentThreatens) {
                                                // V41: Stronger penalty — opponent nearby can easily kill Lando
                                                action.addReasoning("V41 LANDO CAUTION: Alone at " + title
                                                    + " — opponent at CC sites! Deploy protector first!", -400.0f);
                                                logger.warn("V25 LANDO: {} — alone + opponent at CC, but {} chars in hand (-100)",
                                                    title, charsInHand);
                                            } else {
                                                // Lando alone, no opponent at CC, but we have chars in hand
                                                // OK to deploy — we can protect him and opponent hasn't arrived yet
                                                logger.info("V25 LANDO: {} — alone but {} chars in hand and no CC threats — OK", title, charsInHand);
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V25 LANDO ALONE CHECK: Error: {}", e.getMessage());
                                        }
                                    }
                                }

                                // V22/V22.2: Strongly prefer deploying to objective locations
                                // Post-flip: scale required power based on opponent threat
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer deployObjAnalyzer =
                                    context.getObjectiveAnalyzer();
                                if (deployObjAnalyzer != null && deployObjAnalyzer.isAnalyzed() && title != null) {
                                    boolean isObjLocation = deployObjAnalyzer.isObjectiveRelevantLocation(title);
                                    boolean isFlipBackLocation = deployObjAnalyzer.isFlipBackProtectionLocation(title);
                                    boolean objectiveIsFlipped = deployObjAnalyzer.isFlipped();

                                    // V24.2E (V24.9/V24.14B fix): Detect undercover spy deployment.
                                    // UNIVERSAL: Check deploying card's blueprint game text for "undercover".
                                    // This works for ALL spy cards without hardcoding names.
                                    // Also uses early detection result from V24.14B if available.
                                    boolean isUndercoverSpy = earlySpyDetected;  // Reuse V24.14B early detection
                                    // Method 1 (UNIVERSAL): Check deploying card's blueprint game text
                                    if (!isUndercoverSpy && deployingBlueprintId != null) {
                                        try {
                                            SwccgCardBlueprint spyCheckBp = getBlueprintFromId(context, deployingBlueprintId);
                                            if (spyCheckBp != null) {
                                                String spyCheckText = spyCheckBp.getGameText();
                                                if (spyCheckText != null && spyCheckText.toLowerCase(java.util.Locale.ROOT).contains("undercover")) {
                                                    isUndercoverSpy = true;
                                                    logger.warn("V24.14B SPY DETECT Method 1: Blueprint game text contains 'undercover'!");
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V24.14B SPY DETECT Method 1: Error: {}", e.getMessage());
                                        }
                                    }
                                    // Method 1b (fallback): Decision text keywords
                                    if (!isUndercoverSpy) {
                                        if (decisionText.contains("undercover") || decisionText.contains("as a spy")) {
                                            isUndercoverSpy = true;
                                            logger.warn("V24.14B SPY DETECT Method 1b: Decision text spy keyword!");
                                        }
                                    }
                                    // V67bt (Steve, 2026-05-11): METHOD 2 REMOVED.
                                    //
                                    // The old heuristic was: "if location options include both our-side
                                    // and opponent-side sites, must be a spy deploy." That's wrong —
                                    // any non-pilot character is offered both-sides sites in normal
                                    // deploy decisions; it's not a spy signal at all.
                                    //
                                    // Method 2 false-positively tagged General Nevar, Myn Kyneugh,
                                    // and similar non-spies as spies. The spy scoring then applied
                                    // -2000 at Rando's concentration sites ("spy blocks our drain")
                                    // and +300 at opp-occupied sites ("ideal spy spot"). Result:
                                    // Rando deployed Nevar (power 3) solo at Cloud City: Lower
                                    // Corridor across from Rey (power 7) → 22 battle damage, concede.
                                    //
                                    // Steve's rule (and memory file feedback_card_search_by_type_not_text):
                                    // detect spies BY GAME TEXT or KEYWORD, never by heuristic.
                                    // Methods 1 (decision text "undercover"/"spy") and Method 3
                                    // (blueprint game text "undercover") are the correct typed checks.
                                    // Method 2 is permanently removed.
                                    // Method 3: Check deploying card's blueprint game text for "undercover" keyword.
                                    // User confirmed: spy cards have "undercover" in their game text.
                                    // deployingBlueprintId is extracted from the decision text HTML earlier in this method.
                                    if (!isUndercoverSpy && deployingBlueprintId != null) {
                                        try {
                                            SwccgCardBlueprint deployingBp = getBlueprintFromId(context, deployingBlueprintId);
                                            if (deployingBp != null) {
                                                String spyGameText = deployingBp.getGameText();
                                                if (spyGameText != null && spyGameText.toLowerCase(java.util.Locale.ROOT).contains("undercover")) {
                                                    isUndercoverSpy = true;
                                                    logger.info("V24.9 SPY DETECT Method 3: Blueprint game text contains 'undercover' — this is a spy deploy!");
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V24.9 SPY DETECT Method 3: Error checking blueprint: {}", e.getMessage());
                                        }
                                    }

                                    if (isUndercoverSpy && !spyScoringApplied) {
                                        // V24.14B: SPY LOCATION SCORING — check WHO has presence.
                                        // Spy blocks force drains for BOTH sides at a location.
                                        // GOOD: Deploy spy where opponent has presence and we DON'T → blocks their drain.
                                        // BAD: Deploy spy where only WE have presence → blocks OUR drain.
                                        // CC/objective locations need same logic — opponent CAN deploy to our CC sites!
                                        // V59: Skipped when spyScoringApplied=true (universal scoring already ran).
                                        float oppPowerHere = 0;
                                        try {
                                            oppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                game.getGameState(), location, opponent, false, false);
                                        } catch (Exception e) { /* ignore */ }

                                        // UPDATED (Steve, 2026-06): two gaps in this WHO-has-presence
                                        // scoring. (1) ourPower EXCLUDES undercover spies, so a 2nd spy
                                        // onto a site that already has a friendly spy read as "ideal"
                                        // (+300) — detect the existing spy and block the double-up
                                        // (Steve's mistake 2: 2nd spy stacked while Dooku drained 3
                                        // elsewhere). (2) the both-sides case was a flat -50, too weak
                                        // to stop a wasted spy on a site we already hold; condition it
                                        // on whether we could FLIP this spy to buddy-battle (Steve's
                                        // caveat): our character power + this spy's flipped power vs opp.
                                        boolean v24bSpyHere = false;
                                        try {
                                            for (PhysicalCard v24bC : game.getGameState().getCardsAtLocation(location)) {
                                                if (v24bC != null && playerId.equals(v24bC.getOwner())
                                                        && v24bC.isUndercover()) { v24bSpyHere = true; break; }
                                            }
                                        } catch (Exception ignore) { }
                                        float v24bSpyPow = 0f;
                                        try {
                                            SwccgCardBlueprint v24bBp = getBlueprintFromId(context, deployingBlueprintId);
                                            if (v24bBp != null && v24bBp.hasPowerAttribute() && v24bBp.getPower() != null)
                                                v24bSpyPow = v24bBp.getPower();
                                        } catch (Exception ignore) { }

                                        if (v24bSpyHere) {
                                            // A friendly spy already blocks here — a 2nd spy is wasted.
                                            action.addReasoning("V24.14B SPY DOUBLED: a friendly spy already blocks here — send this spy to an open enemy drain!", -1200.0f);
                                            logger.warn("V24.14B SPY DOUBLED: {} — friendly spy already here — -1200 (route to open drain)", title);
                                        } else if (oppPowerHere > 0 && ourPower == 0) {
                                            // BEST: Opponent controls, we don't — spy stays undercover, blocks THEIR drain!
                                            // Works at ANY site including CC if opponent moved in.
                                            action.addReasoning("V24.14B SPY: Opponent controls here, we don't — block their force drain!", 300.0f);
                                            logger.warn("V24.14B SPY: {} — opp power {}, our power 0 — IDEAL spy location! (+300)", title, oppPowerHere);
                                        } else if (oppPowerHere > 0 && ourPower > 0) {
                                            // Both sides present. OK ONLY if we can flip this spy and the
                                            // combined force contests (Steve's buddy-system caveat) —
                                            // otherwise the spy is wasted on a site we already hold.
                                            if ((ourPower + v24bSpyPow) >= oppPowerHere) {
                                                action.addReasoning("V24.14B SPY FLIP-BUDDY: our character + this spy can contest here — OK to break cover and fight!", 50.0f);
                                                logger.warn("V24.14B SPY FLIP-BUDDY: {} — char {} + spy {} >= opp {} — allow (flip to fight)", title, ourPower, v24bSpyPow, oppPowerHere);
                                            } else if (isObjLocation || isFlipBackLocation) {
                                                // CC site — we already drain here, breaking cover wastes spy AND blocks our drain while undercover
                                                action.addReasoning("V24.14B SPY: Both sides at CC, can't flip-and-win — spy blocks OUR drain undercover!", -500.0f);
                                                logger.warn("V24.14B SPY: {} — both sides at CC, can't flip-win — bad (-500)", title);
                                            } else {
                                                // Non-CC, can't flip-and-win — the spy is wasted here (Steve's mistake 1).
                                                action.addReasoning("V24.14B SPY: Both sides present, can't flip-and-win — spy wasted, route to an open drain!", -800.0f);
                                                logger.warn("V24.14B SPY: {} — both sides, can't flip-win — wasted (-800)", title);
                                            }
                                        } else if (oppPowerHere == 0 && ourPower > 0) {
                                            // BAD: Only WE have presence — spy blocks OUR drain!
                                            action.addReasoning("V24.14B SPY: Only we have presence — spy blocks OUR drain!", -2000.0f);
                                            logger.warn("V24.14B SPY: {} — only our power {} — spy HURTS us! (-2000)", title, ourPower);
                                        } else {
                                            // Empty location — spy doesn't help either side
                                            if (isObjLocation || isFlipBackLocation) {
                                                // Empty CC site — we want to drain here, not block it with a spy
                                                action.addReasoning("V24.14B SPY: Empty CC site — don't waste spy here!", -300.0f);
                                                logger.warn("V24.14B SPY: {} — empty CC site, spy wastes potential drain (-300)", title);
                                            } else {
                                                action.addReasoning("V24.14B SPY: Empty non-CC location — no drain to block", -100.0f);
                                            }
                                        }
                                    } else if (!isObjLocation && !isFlipBackLocation) {
                                        if (isCharacter && deployObjAnalyzer.needsBespinSystemPresence()) {
                                            // V29: TDIGWATT-specific hard block for non-objective character deploys (non-spies only)
                                            // Increased from -250 to -500 because Rando was still deploying Mara Jade
                                            // and Admiral Chiraneau to Tatooine: Mos Eisley instead of Cloud City sites.
                                            action.addReasoning("V29 TDIGWATT: Do NOT deploy characters to non-Cloud City locations!", -500.0f);
                                            logger.warn("V29 TDIGWATT: Blocking character deploy to non-objective location {} (-500)", title);

                                            // Extra penalty for opponent's planet — even worse than a random non-CC location
                                            String titleCheck = title.toLowerCase(java.util.Locale.ROOT);
                                            if (titleCheck.contains("tatooine") || titleCheck.contains("endor")
                                                || titleCheck.contains("dagobah") || titleCheck.contains("naboo")
                                                || titleCheck.contains("yavin") || titleCheck.contains("hoth")
                                                || titleCheck.contains("jakku") || titleCheck.contains("chandrila")) {
                                                action.addReasoning("V29 OPPONENT PLANET: This is the opponent's territory!", -300.0f);
                                                logger.warn("V29 OPPONENT PLANET: {} is opponent's territory — extra -300", title);
                                            }
                                        }
                                        // Non-objective location: penalize, scale by urgency
                                        boolean objLocationNeedsHelp = false;
                                        float worstDeficit = 0;
                                        java.util.List<PhysicalCard> allLocs = game.getGameState().getLocationsInOrder();
                                        // opponent already declared above in this scope
                                        for (PhysicalCard checkLoc : allLocs) {
                                            if (checkLoc == null) continue;
                                            String checkTitle = checkLoc.getTitle();
                                            if (checkTitle == null) continue;
                                            boolean needsProtection = objectiveIsFlipped
                                                ? deployObjAnalyzer.isFlipBackProtectionLocation(checkTitle)
                                                : deployObjAnalyzer.isObjectiveRelevantLocation(checkTitle);
                                            if (!needsProtection) continue;

                                            float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                game.getGameState(), checkLoc, playerId, false, false);
                                            float theirPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                game.getGameState(), checkLoc, opponent, false, false);

                                            // V22.2: Dynamic threshold — need MORE power when opponent is strong
                                            // Base threshold 8, plus match opponent power with a margin
                                            float requiredPower = Math.max(8.0f, theirPowerThere + 4.0f);
                                            if (ourPowerThere < requiredPower) {
                                                objLocationNeedsHelp = true;
                                                float deficit = requiredPower - ourPowerThere;
                                                if (deficit > worstDeficit) worstDeficit = deficit;
                                            }
                                        }
                                        if (objLocationNeedsHelp) {
                                            // V22.2: Penalty scales with how badly we need reinforcements
                                            // Post-flip penalty is MUCH stronger — losing locations = losing objective
                                            float penalty = objectiveIsFlipped ? -180.0f : -120.0f;
                                            if (worstDeficit > 6) penalty -= 40.0f;  // Extra urgency if severely outgunned
                                            action.addReasoning("V22.2: Objective locations need fortifying" +
                                                (objectiveIsFlipped ? " (POST-FLIP CRITICAL)" : "") +
                                                " - don't deploy elsewhere", penalty);
                                            logger.warn("V22.2 DEPLOY: Penalizing {} ({}), obj locs need +{} power{}",
                                                title, penalty, (int)worstDeficit,
                                                objectiveIsFlipped ? " [FLIPPED - PROTECT!]" : "");
                                        } else {
                                            float mildPenalty = objectiveIsFlipped ? -60.0f : -40.0f;
                                            action.addReasoning("V22: Non-objective location - prefer own locations", mildPenalty);
                                        }
                                    } else if (objectiveIsFlipped && isFlipBackLocation) {
                                        // V22.2: BONUS for deploying to flip-back protection locations post-flip
                                        action.addReasoning("V22.2 POST-FLIP: Deploying to protect flipped objective!", 60.0f);
                                        logger.warn("V22.2 PROTECT: {} is flip-back protection location - bonus for deploying here", title);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Could not get power at {}: {}", title, e.getMessage());
                            }
                        }

                        // === V67as (Steve, 2026-05-08): SPREAD-AWARE DEPLOY DESTINATION ===
                        //
                        // Mirrors V67aj+V67al (which live in DeployEvaluator) for the
                        // CardSelectionEvaluator path — needed because hand-deploy actions
                        // say "Deploy <character>" with NO location in the action text;
                        // the destination is picked here in evaluateDeployLocation via a
                        // sub-decision. V67aj/V67al never fired for hand-deploys because
                        // their actionText.contains(loc.getTitle()) check always failed.
                        //
                        // Steve's report: 59 of 59 character deploys went to one site
                        // (Hoth: Defensive Perimeter, 3rd Marker). Now scoring per
                        // candidate destination here:
                        //
                        //   stack count: friendly characters at this destination
                        //   power total: sum of friendly character power at destination
                        //
                        // Tiered spread bonus / anti-stack penalty:
                        //   Empty obj-req BG:                 +500
                        //   Empty BG (not obj-req):           +300
                        //   1-2 friendlies + BG:              +100
                        //   3+ friendlies + BG (not obj-req): -300  ← anti-stack
                        //   20-24 friendly power, non-obj:    -200  ← V67al-style
                        //   25-34 friendly power, non-obj:    -400
                        //   35+ friendly power, non-obj:      -700
                        //
                        // === V67as SUPERSEDED 2026-05-26: see V136 CharacterDeploySiteEvaluator §B ===
                        // Block kept inert below for easy revert.
                        if (false /* V67as SUPERSEDED V136 */ && game != null && playerId != null) {
                            try {
                                boolean v67asIsBg = game.getModifiersQuerying()
                                    .isBattleground(gameState, location, null);
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer asObj =
                                    context.getObjectiveAnalyzer();
                                boolean v67asIsObjReq = asObj != null && asObj.isAnalyzed()
                                    && !asObj.isFlipped()
                                    && asObj.isObjectiveRelevantLocation(title);

                                int v67asStack = 0;
                                float v67asPower = 0f;
                                java.util.List<PhysicalCard> v67asCards =
                                    gameState.getCardsAtLocation(location);
                                if (v67asCards != null) {
                                    for (PhysicalCard pc : v67asCards) {
                                        if (pc == null || pc.getBlueprint() == null) continue;
                                        if (!playerId.equals(pc.getOwner())) continue;
                                        if (pc.getBlueprint().getCardCategory()
                                                != CardCategory.CHARACTER) continue;
                                        v67asStack++;
                                        if (pc.getBlueprint().hasPowerAttribute()) {
                                            Float p = pc.getBlueprint().getPower();
                                            if (p != null) v67asPower += p;
                                        }
                                    }
                                }

                                String v67asLabel = null;
                                float v67asBonus = 0f;
                                if (v67asIsBg && v67asIsObjReq && v67asStack == 0) {
                                    v67asLabel = "OBJ-REQ BG, EMPTY";
                                    v67asBonus = 500f;
                                } else if (v67asIsBg && v67asIsObjReq && v67asStack <= 2) {
                                    v67asLabel = "OBJ-REQ BG, REINFORCE";
                                    v67asBonus = 250f;
                                } else if (v67asIsBg && v67asStack == 0) {
                                    v67asLabel = "BG, OPEN-FRONT";
                                    v67asBonus = 300f;
                                } else if (v67asIsBg && v67asStack <= 2) {
                                    v67asLabel = "BG, REINFORCE";
                                    v67asBonus = 100f;
                                } else if (v67asIsBg) {
                                    v67asLabel = "BG, OVER-STACK";
                                    v67asBonus = -300f;
                                }
                                if (v67asLabel != null) {
                                    action.addReasoning(String.format(
                                        "V67as DEPLOY DEST [%s, stack=%d]: %s",
                                        v67asLabel, v67asStack, title), v67asBonus);
                                    logger.warn("V67as [{}]: dest={} stack={} → {}{}",
                                        v67asLabel, title, v67asStack,
                                        v67asBonus > 0 ? "+" : "", (int) v67asBonus);
                                }

                                // V67as POWER-STACK PENALTY (mirrors V67al, non-objective only)
                                if (!v67asIsObjReq) {
                                    float v67asPwrPenalty = 0f;
                                    String v67asPwrLabel = null;
                                    if (v67asPower >= 35f) {
                                        v67asPwrPenalty = -700f;
                                        v67asPwrLabel = "POWER-STACK CATASTROPHIC";
                                    } else if (v67asPower >= 25f) {
                                        v67asPwrPenalty = -400f;
                                        v67asPwrLabel = "POWER-STACK HEAVY";
                                    } else if (v67asPower >= 20f) {
                                        v67asPwrPenalty = -200f;
                                        v67asPwrLabel = "POWER-STACK MILD";
                                    }
                                    if (v67asPwrLabel != null) {
                                        action.addReasoning(String.format(
                                            "V67as %s: %s already has %.0f friendly power — spread to threaten elsewhere!",
                                            v67asPwrLabel, title, v67asPower), v67asPwrPenalty);
                                        logger.warn("V67as {}: dest={} friendlyPower={} → {}",
                                            v67asPwrLabel, title, (int) v67asPower, (int) v67asPwrPenalty);
                                    }
                                }

                                // === V67br (Steve, 2026-05-11): TURN-BASED SPREAD DISCIPLINE ===
                                //
                                // Steve's rule: "We should likely not spread turn 1. Turn 2
                                // can cautiously spread, turn three is fully ok to spread."
                                //
                                // GROUND vs ABOARD-SHIP distinction (Steve, 2026-05-11):
                                //   "If on Executor he's safe. If by himself on a site he is not."
                                //   Chars aboard ships at a SYSTEM are protected by the ship —
                                //   they don't need ground reinforcement. So:
                                //     - Concentration site count: only friendlies at SITES (ground).
                                //       Friendlies at SYSTEMS (aboard ships) don't anchor V67br.
                                //     - Destination penalty: only applies when destination is a SITE.
                                //       SYSTEM destinations (deploying aboard a ship) are always safe.
                                //
                                // Turn 1: -800 to non-concentration SITE destinations.
                                // Turn 2: -300 (cautious).
                                // Turn 3+: no penalty.
                                // First deploy of turn 1 with no ground friendlies: unrestricted.
                                try {
                                    int v67brTurn = gameState.getPlayersLatestTurnNumber(playerId);
                                    boolean v67brDestIsSite = location != null
                                        && location.getBlueprint() != null
                                        && location.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE;
                                    if (v67brTurn <= 2 && v67brDestIsSite) {
                                        // Find concentration SITE (most friendly chars at any SITE).
                                        // Friendlies at SYSTEMS (on ships) don't count — they're safe.
                                        PhysicalCard v67brConcSite = null;
                                        int v67brMaxCount = 0;
                                        for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                            if (loc == null || loc.getBlueprint() == null) continue;
                                            if (loc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SITE) continue;
                                            int v67brCount = 0;
                                            java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(loc);
                                            if (cardsAtLoc != null) {
                                                for (PhysicalCard pc : cardsAtLoc) {
                                                    if (pc == null || pc.getBlueprint() == null) continue;
                                                    if (!playerId.equals(pc.getOwner())) continue;
                                                    if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                    v67brCount++;
                                                }
                                            }
                                            if (v67brCount > v67brMaxCount) {
                                                v67brMaxCount = v67brCount;
                                                v67brConcSite = loc;
                                            }
                                        }
                                        if (v67brConcSite != null && !v67brConcSite.equals(location)) {
                                            // V75 (Steve, 2026-05-15): KILL-BOX CHECK before applying V67br penalty.
                                            // If the concentration site is overwhelmed (opp power > our power + 4),
                                            // it's a sacrifice site — let Rando spread to fresh sites instead.
                                            // Replay May 15: Lars Farm became a kill box vs Vader + Mara + Death
                                            // Trooper, but V67br kept pushing Rando to deploy more chars there.
                                            float v75OurPower = 0f;
                                            float v75OppPower = 0f;
                                            try {
                                                java.util.List<PhysicalCard> ccAt = gameState.getCardsAtLocation(v67brConcSite);
                                                if (ccAt != null) {
                                                    for (PhysicalCard cc : ccAt) {
                                                        if (cc == null || cc.getBlueprint() == null) continue;
                                                        if (cc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                        Float pw = cc.getBlueprint().getPower();
                                                        if (pw == null) continue;
                                                        if (playerId.equals(cc.getOwner())) v75OurPower += pw;
                                                        else v75OppPower += pw;
                                                    }
                                                }
                                            } catch (Exception ex) { /* ignore */ }

                                            boolean v75KillBox = v75OppPower > (v75OurPower + 4f) && v75OppPower > 0;
                                            if (v75KillBox) {
                                                // Concentration site is a kill box. Don't penalize spreading.
                                                // Actually REWARD spreading to a fresh site (we need new ground).
                                                action.addReasoning(String.format(
                                                    "V75 KILL-BOX OVERRIDE: concentration site %s overwhelmed (opp %d > our %d + 4) — spread to fresh site!",
                                                    v67brConcSite.getTitle(), (int) v75OppPower, (int) v75OurPower), 200.0f);
                                                logger.warn("V75 KILL-BOX OVERRIDE: concSite={} opp={} our={} → spread bonus +200",
                                                    v67brConcSite.getTitle(), (int) v75OppPower, (int) v75OurPower);
                                            } else {
                                                float v67brPenalty = (v67brTurn == 1) ? -800.0f : -300.0f;
                                                String v67brLabel = (v67brTurn == 1) ? "TURN 1 NO-SPREAD" : "TURN 2 CAUTIOUS-SPREAD";
                                                action.addReasoning(String.format(
                                                    "V67br %s: concentration site is %s (%d friendly chars there). Deploy with them, not here!",
                                                    v67brLabel, v67brConcSite.getTitle(), v67brMaxCount), v67brPenalty);
                                                logger.warn("V67br {}: dest={} concSite={} ({} chars) → {}",
                                                    v67brLabel, title, v67brConcSite.getTitle(), v67brMaxCount, (int)v67brPenalty);
                                            }
                                        }
                                    }
                                } catch (Exception v67brEx) {
                                    logger.debug("V67br turn-spread check error: {}", v67brEx.getMessage());
                                }

                                // === V67bj (Steve, 2026-05-11): THREAT-AWARE DESTINATION ===
                                //
                                // Don't pick a destination where opponent's power on the site
                                // exceeds Rando's TOTAL available power (already-here + the
                                // char being deployed + chars in hand still deployable AND
                                // leaving 2 force for battle interrupts) by 4 or more.
                                //
                                // Replay 6fqi4jm1kkp7e9i8: Stormtrooper Patrol (power 2) solo
                                // at Guest Quarters across from Rey + Jedi (15 power). Hand
                                // had no chars to swing the matchup. Should have refused.
                                //
                                // -400 magnitude: bigger than typical destination bonuses
                                // (drain +30, V67as +500 obj-req, etc.) so this dominates
                                // when site is genuinely bad, but proportional to the post-
                                // V67bk score landscape (no V52 SPEND FORCE +300 anymore).
                                try {
                                    String opponentForBj = gameState.getOpponent(playerId);
                                    float oppPowerAtSite = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, location, opponentForBj, false, false);

                                    // Already-deployed friendly power at this site (excludes char being deployed since it's in hand)
                                    float myPowerAtSite = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, location, playerId, false, false);

                                    // Power of the char being deployed (from deployingBlueprintId / detected blueprint)
                                    float deployingPower = 0f;
                                    int deployingCost = 0;
                                    if (deployingBlueprintId != null) {
                                        for (PhysicalCard hc : gameState.getHand(playerId)) {
                                            if (hc != null && hc.getBlueprint() != null
                                                    && deployingBlueprintId.equals(hc.getBlueprintId(true))) {
                                                SwccgCardBlueprint dbp = hc.getBlueprint();
                                                if (dbp.hasPowerAttribute() && dbp.getPower() != null) {
                                                    deployingPower = dbp.getPower();
                                                }
                                                Float dCost = dbp.getDeployCost();
                                                if (dCost != null) deployingCost = dCost.intValue();
                                                break;
                                            }
                                        }
                                    }

                                    // Hand-deployable additional power: chars in hand affordable AFTER paying
                                    // for the one being deployed and reserving 2 force for battle interrupts.
                                    final int BATTLE_RESERVE = 2;
                                    int forcePileNow = 0;
                                    try { forcePileNow = gameState.getForcePileSize(playerId); } catch (Exception ignored) { }
                                    int forceLeftForOtherDeploys = Math.max(0, forcePileNow - deployingCost - BATTLE_RESERVE);

                                    float handDeployablePower = 0f;
                                    java.util.List<PhysicalCard> bjHand = gameState.getHand(playerId);
                                    if (bjHand != null) {
                                        // V67bj v3 (2026-05-11): FILTER FIRST, THEN SORT.
                                        // The original code sorted the WHOLE hand by getDeployCost(),
                                        // which throws UnsupportedOperationException for non-deployable
                                        // cards (Effects, Interrupts). That exception aborted V67bj
                                        // for every destination — the rule never actually fired in
                                        // production. Now we filter to CHARACTERs (which all have
                                        // a deploy cost) before any sort call.
                                        java.util.List<PhysicalCard> bjCharHand = new java.util.ArrayList<>();
                                        for (PhysicalCard hc : bjHand) {
                                            if (hc == null || hc.getBlueprint() == null) continue;
                                            if (deployingBlueprintId != null
                                                    && deployingBlueprintId.equals(hc.getBlueprintId(true))) continue;
                                            if (hc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                            bjCharHand.add(hc);
                                        }
                                        bjCharHand.sort((a, b) -> {
                                            float ca, cb;
                                            try { Float v = a.getBlueprint().getDeployCost(); ca = v != null ? v : 99f; }
                                            catch (Exception ex) { ca = 99f; }
                                            try { Float v = b.getBlueprint().getDeployCost(); cb = v != null ? v : 99f; }
                                            catch (Exception ex) { cb = 99f; }
                                            return Float.compare(ca, cb);
                                        });
                                        for (PhysicalCard hc : bjCharHand) {
                                            SwccgCardBlueprint hBp = hc.getBlueprint();
                                            int c;
                                            try {
                                                Float hCost = hBp.getDeployCost();
                                                c = (hCost != null) ? hCost.intValue() : 0;
                                            } catch (Exception ex) { continue; }
                                            if (c <= forceLeftForOtherDeploys) {
                                                if (hBp.hasPowerAttribute() && hBp.getPower() != null) {
                                                    handDeployablePower += hBp.getPower();
                                                }
                                                forceLeftForOtherDeploys -= c;
                                            }
                                        }
                                    }

                                    float totalMyAvailable = myPowerAtSite + deployingPower + handDeployablePower;
                                    float deficit = oppPowerAtSite - totalMyAvailable;

                                    // V67bu (Steve, 2026-05-11): V67bj only fires for UNCOMMITTED
                                    // destinations (zero friendlies). Once Rando has friendlies at a
                                    // site, V67bn handles the reinforce-or-retreat decision —
                                    // V67bj's "don't bait" advice is moot since the commitment is
                                    // already made.
                                    int v67buFriendliesHere = 0;
                                    try {
                                        java.util.List<PhysicalCard> bjCardsHere = gameState.getCardsAtLocation(location);
                                        if (bjCardsHere != null) {
                                            for (PhysicalCard pc : bjCardsHere) {
                                                if (pc == null || pc.getBlueprint() == null) continue;
                                                if (!playerId.equals(pc.getOwner())) continue;
                                                if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                v67buFriendliesHere++;
                                            }
                                        }
                                    } catch (Exception ignored) { }

                                    if (deficit >= 4f && v67buFriendliesHere == 0) {
                                        action.addReasoning(String.format(
                                            "V67bj DON'T BAIT (uncommitted): %s deficit %.0f (opp %.0f vs my available %.0f = site %.0f + deploying %.0f + hand-deployable %.0f, battle-reserve %d). Pick safer site!",
                                            title, deficit, oppPowerAtSite, totalMyAvailable,
                                            myPowerAtSite, deployingPower, handDeployablePower, BATTLE_RESERVE),
                                            -400.0f);
                                        logger.warn("V67bj THREAT BLOCK (uncommitted): dest={} opp={} myAvail={} (site={}, deploy={}, hand={}) deficit={} → -400",
                                            title, (int) oppPowerAtSite, (int) totalMyAvailable,
                                            (int) myPowerAtSite, (int) deployingPower,
                                            (int) handDeployablePower, (int) deficit);
                                    } else if (deficit >= 4f && v67buFriendliesHere > 0) {
                                        logger.info("V67bj SKIPPED (committed): {} friendlies at {} already — V67bn handles reinforce-or-retreat (deficit={})",
                                            v67buFriendliesHere, title, (int)deficit);
                                    }
                                } catch (Exception bjEx) {
                                    logger.debug("V67bj threat check error: {}", bjEx.getMessage());
                                }
                            } catch (Exception e) {
                                logger.debug("V67as: error scoring deploy destination: {}", e.getMessage());
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse cardId: {}", cardId);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ SECTION: FORCE-LOSS — Loss-Source Picker (reorg 2026-07-06) ═══
    // Owns: V153 two-tier zone order (protect characters when life force >= 4;
    // survival mode < 4) + bolt-ons V109 (senators -300), V175a (weapon-protect
    // turn-4 gate), V178-loss (wielded-weapon zone rerank 600→150), V28-DTF
    // (Draw Their Fire force-pile protect), V21/V25 protections. Hub: V153 LIVE.
    // KIND mix + key magnitudes: ORDERING via zone bands; HAND FLOOR -700,
    // PRIORITY CARD -100, V21 hard bans on flip-required/objective-pullable cards.
    // Absorbs (dead, commented below/nearby — revert path, do not delete): V127,
    // V101, V119, V29.8-zone (the old //-commented zone-scoring blocks).
    // NOTE: the zone order is DUPLICATED in evaluateForceLossOrForfeit (battle
    // handler, further down this file) — byte-identical parity pair, EDIT BOTH
    // TOGETHER until an extract-method pass.
    // Cross-refs: BATTLE-3 (forfeit side of the combined lose-or-forfeit prompt),
    // RESPONSE (pay-loss route). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 +
    // Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════
    /**
     * Choose force to lose - pick cards we want to lose least.
     */
    private List<EvaluatedAction> evaluateForceLoss(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        // V21: Count hand size and reserves for hand protection
        int handSize = 0;
        int totalReserves = 0;
        // V153 THIN RESERVE (Steve, 2026-07-07): reserve DECK size in isolation (NOT the
        // life-force sum totalReserves) — used by the thin-reserve guard below to spare
        // destiny-draw fuel when the deck runs low.
        int reserveDeckSize = 0;
        if (gameState != null && playerId != null) {
            try {
                handSize = gameState.getHand(playerId).size();
                reserveDeckSize = gameState.getReserveDeckSize(playerId);
                totalReserves = reserveDeckSize
                    + gameState.getUsedPile(playerId).size()
                    + gameState.getForcePileSize(playerId);
            } catch (Exception e) {
                // Fallback
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Lose force (card " + cardId + ")"
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        if (title != null) {
                            action.setDisplayText("Lose " + title);
                        }

                        // =======================================================
                        // V29.8: ZONE-AWARE FORCE LOSS — RESERVE/USED FIRST, HAND LAST
                        // When life force is healthy (reserve+used+force > 10):
                        //   STRONGLY prefer losing from Reserve/Used/Force Pile.
                        //   Cards in those piles can't be played — they're just life force.
                        //   Cards in hand = deploy options = your entire next turn.
                        //   Losing your whole hand = nothing to deploy = death spiral.
                        // When life force is critical (<= 10):
                        //   Reluctantly lose from hand to preserve life force.
                        //
                        // V29.8 FIX: Previous scoring was too weak (+30 reserve vs -100 hand).
                        // Card-specific penalties (destiny, unique, priority) applied to ALL zones
                        // equally, which swamped the zone preference. Now:
                        //   - Zone scoring is MASSIVE (+500 for reserve when healthy)
                        //   - Card-specific penalties only apply to hand cards (not pile cards)
                        // =======================================================
                        com.gempukku.swccgo.common.Zone zone = card.getZone();
                        boolean isFromHand = (zone != null && zone.name().contains("HAND"));
                        boolean isFromReserve = (zone != null && zone.name().contains("RESERVE"));
                        boolean isFromUsedPile = (zone != null && zone.name().contains("USED"));
                        boolean isFromForcePile = (zone != null && zone.name().contains("FORCE_PILE"));

                        // === V127 (Steve, 2026-05-22): FORCE-LOSS CONSOLIDATION ===
                        // V101 (May 20) DELETED. V101 added a blanket Used+500/Reserve+300/
                        // Hand-500 layer on top of V29.8 below, which silently dominated V29.8's
                        // conditional duplicate-detection and life-force-low logic. Net effect:
                        // a duplicate hand interrupt healthy scored -750, while any pile card
                        // scored +1000. Hand always lost by 1750 — the V13-era priority order
                        // (Duplicate Hand > Used > Reserve > Hand > Force Pile) was inverted.
                        //
                        // V127 collapses V101's used > reserve ordering INTO V29.8 below, where
                        // it can interact with the duplicate + life-force-low logic correctly.
                        // See V29.8 block immediately below for the new tiered magnitudes.
                        //
                        // V119 (V101's mirror in evaluateForceLossOrForfeit) is also deleted —
                        // V29.8 is now mirrored into that handler instead.

                        // === V109 (Steve, 2026-05-20): MY LORD — DON'T LOSE/COST SENATORS ===
                        // Per Steve: "Let's never put a senator in used pile or lost pile
                        // for this deck if we can avoid. hard block like -300"
                        // Applied here at every force-loss / cost / forfeit decision.
                        // Senator detection via lore + keyword.
                        if (card.getBlueprint() != null) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v109Obj =
                                context.getObjectiveAnalyzer();
                            if (v109Obj != null && v109Obj.isAnalyzed()
                                    && v109Obj.getObjectiveTitle() != null) {
                                // V109 CONSOLIDATED (2026-07-07): identity from ObjectiveAnalyzer.isMyLord().
                                boolean v109IsMyLord = v109Obj.isMyLord();
                                if (v109IsMyLord) {
                                    boolean v109IsSenator = false;
                                    if (card.getBlueprint().hasKeyword(
                                            com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                        v109IsSenator = true;
                                    } else {
                                        String v109Lore = card.getBlueprint().getLore();
                                        if (v109Lore != null && v109Lore.toLowerCase(
                                                java.util.Locale.ROOT).contains("senator")) {
                                            v109IsSenator = true;
                                        }
                                    }
                                    if (v109IsSenator) {
                                        action.addReasoning(
                                            "V109 MY LORD: PROTECT senator '" + title
                                                + "' — never discard/lose senators in this deck -300",
                                            -300.0f);
                                        logger.warn("V109 MY LORD: PROTECT senator {} from loss/cost → -300",
                                            title);
                                    }
                                }
                            }
                        }

                        // ================================================================
// ===================================================================
// OLD V127/V29.8 ZONE SCORING (regular evaluateForceLoss) — SUPERSEDED by V153 below.
// Steve's comment-out rule (2026-05-28): kept inline & commented for dev
// reference, not deleted. This is the EXACT pre-V153 logic (the inverted
// healthy/low zone magnitudes V153 fixed). A future clean build may strip it.
// ===================================================================
//                         // === V127 V29.8 ZONE SCORING — restores V13 priority order ===
//                         //   Healthy (reserves > 10): Used +400 > Reserve +300 > Force Pile +100 > Hand -300
//                         //   Low (reserves <= 10):    Hand +400 > Force Pile -100 > Used -200 > Reserve -250
//                         //   DUPLICATE in hand: +800 bonus applied separately below
//                         // Used > Reserve ordering (was V101's +500/+300 contribution) baked in here.
//                         if (isFromUsedPile) {
//                             if (lifeForceHealthy) {
//                                 action.addReasoning("V29.8 USED PILE (healthy): already-spent cards lost first +400", 400.0f);
//                             } else {
//                                 action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReserves + "): mild protect Used Pile -200", -200.0f);
//                                 logger.warn("V29.8 USED PROTECT: {} lifeForce={} — protect (-200)", title, totalReserves);
//                             }
//                         } else if (isFromReserve) {
//                             if (lifeForceHealthy) {
//                                 action.addReasoning("V29.8 RESERVE (healthy): second pile preference +300", 300.0f);
//                             } else {
//                                 action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReserves + "): PROTECT Reserve — biggest source -250", -250.0f);
//                                 logger.warn("V29.8 RESERVE PROTECT: {} lifeForce={} — protect (-250)", title, totalReserves);
//                             }
//                         } else if (isFromForcePile) {
//                             // V28: Check if Draw Their Fire is active — Force pile = interrupt ability!
//                             boolean dtfActive = false;
//                             try {
//                                 String dtfOpId = gameState.getOpponent(playerId);
//                                 for (PhysicalCard dtfC : gameState.getAllPermanentCards()) {
//                                     if (dtfC != null && dtfOpId != null && dtfOpId.equals(dtfC.getOwner())
//                                         && dtfC.getBlueprint() != null && dtfC.getBlueprint().getTitle() != null
//                                         && dtfC.getBlueprint().getTitle().toLowerCase(java.util.Locale.ROOT).contains("draw their fire")
//                                         && dtfC.getZone() != null && dtfC.getZone().isInPlay()) {
//                                         dtfActive = true;
//                                         break;
//                                     }
//                                 }
//                             } catch (Exception e) { }
// 
//                             if (dtfActive) {
//                                 int forcePileSize = 0;
//                                 try { forcePileSize = gameState.getForcePileSize(playerId); } catch (Exception e) { }
//                                 float dtfForcePenalty = (forcePileSize <= 3) ? -400.0f : -200.0f;
//                                 action.addReasoning(String.format("V28 DTF FORCE PILE PROTECT: Force pile=%d, DTF active — lose from reserve instead!", forcePileSize), dtfForcePenalty);
//                                 logger.warn("V28 DTF FORCE PILE PROTECT: {} from Force pile, DTF active, pile={} — HEAVY PENALTY ({})", title, forcePileSize, dtfForcePenalty);
//                             } else if (lifeForceHealthy) {
//                                 // V127: Force pile is LAST positive pile preference — force is precious for activation
//                                 action.addReasoning("V29.8 FORCE PILE (healthy): last positive pile preference +100", 100.0f);
//                             } else {
//                                 // Low — force pile losses still OK if we have to, slight protect
//                                 action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReserves + "): protect force pile -100", -100.0f);
//                             }
//                         } else if (isFromHand) {
//                             if (lifeForceHealthy) {
//                                 // V127: Hand penalty reduced from -500 (V29.8 old + V101) to -300.
//                                 // Smaller penalty lets duplicate (+800 below) and type bonuses
//                                 // (interrupt +50) influence the decision properly.
//                                 action.addReasoning("V29.8 HAND PROTECT (healthy " + totalReserves + " life force): hand cards are deploy options -300", -300.0f);
//                                 logger.warn("V29.8 HAND PROTECT (healthy): {} lifeForce={} → -300", title, totalReserves);
//                             } else {
//                                 // V127: Life force LOW (Steve's ≤10 threshold) — hand losses STRONGLY preferred.
//                                 // Was +80 in old code, dominated by V101's -500. Now +400 so reserves≤10 actually flips preference to hand.
//                                 action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReserves + "): prefer Hand loss — preserve piles +400", 400.0f);
//                                 logger.warn("V29.8 HAND PREFERRED (low life force): {} lifeForce={} → +400", title, totalReserves);
//                             }
// 
//                             // V25: CHARACTER PROTECTION IN HAND (only applies to hand cards)
//                             if (blueprint != null) {
//                                 CardCategory handCardCategory = blueprint.getCardCategory();
//                                 if (handCardCategory == CardCategory.CHARACTER) {
//                                     action.addReasoning("V29.8 HAND PROTECT: CHARACTER — needs to be deployed!", -150.0f);
//                                 } else if (handCardCategory == CardCategory.STARSHIP || handCardCategory == CardCategory.VEHICLE) {
//                                     action.addReasoning("V29.8 HAND PROTECT: Ship/vehicle needs deploying", -80.0f);
//                                 } else if (handCardCategory == CardCategory.INTERRUPT) {
//                                     // Interrupts are the least bad to lose from hand
//                                     action.addReasoning("V29.8 HAND: Interrupt — least bad hand loss", 50.0f);
//                                 }
//                             }
//                         }
                        // === V153 (Steve, 2026-05-28): UNIFIED FORCE-LOSS ORDER (char/life-force tiers) ==
                        // Replaces the old V127/V29.8 healthy/low zone scoring, which was
                        // INVERTED from Steve's intended order (it dumped HAND first when low,
                        // and lost RESERVE before HAND when healthy — backwards). Replay
                        // 6x8e5hyqgajpe045: Grievous lost off Reserve to a drain while spare
                        // interrupts sat in hand.
                        //
                        // MECHANIC (verified in engine): life force = Reserve + Force Pile +
                        // Used Pile (GameState.getPlayerLifeForce). HAND and TABLE are NOT life
                        // force, so losing from hand / forfeiting does NOT move you toward the
                        // lose condition; losing from reserve/used/force DOES. Defeat fires when
                        // life force <= 0 (checkLifeForceDepleted). So when critically low we
                        // dump hand to preserve the life-force piles; otherwise we keep our
                        // deployable hand (esp. characters) to mount the comeback.
                        //
                        // Steve's order (lose FIRST -> LAST), by life force tier:
                        //   >= 4 (protect characters): Dup hand > Used > hand junk > Reserve
                        //                              > HAND CHARACTERS > Force pile
                        //   <  4 (survival, save life-force piles): Dup hand > hand junk
                        //                              > HAND CHARACTERS > Used > Reserve > Force pile
                        // Within hand, every non-character is lost before a character (chars are
                        // the comeback). At >=4 we spend Reserve to keep characters; below 4 we
                        // dump the whole hand (junk then chars) to keep the life-force piles off
                        // the deck-out line. Force pile is ALWAYS last (deploy/activation fuel).
                        // Hand floor: keep >=4 cards in hand while life force >= 10.
                        //
                        // SCOPE: this block = regular force loss (drains, First Strike, "lose X
                        // Force"). The SAME order is mirrored into the battle handler
                        // evaluateForceLossOrForfeit (force-loss side).
                        //
                        // Preserved protections (bolted on below): V109 senator (above, all
                        // zones), V28 Draw Their Fire force-pile, V21 objective-critical, V25
                        // Hunt-Down lightsaber, AiPriorityCards (hand + used), duplicate bonus.
                        // ================================================================

                        // --- duplicate-in-hand detection (needed by the tier below) ---
                        boolean isDuplicate = false;
                        if (isFromHand && title != null) {
                            try {
                                int copiesInHand = 0;
                                boolean copyOnTable = false;
                                java.util.List<PhysicalCard> myHand = gameState.getHand(playerId);
                                if (myHand != null) {
                                    for (PhysicalCard hc : myHand) {
                                        if (hc != null && title.equals(hc.getTitle())) {
                                            copiesInHand++;
                                        }
                                    }
                                }
                                for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                    if (tc != null && playerId.equals(tc.getOwner())
                                        && title.equals(tc.getTitle())
                                        && tc.getZone() != null && tc.getZone().isInPlay()) {
                                        copyOnTable = true;
                                        break;
                                    }
                                }
                                isDuplicate = (copiesInHand >= 2 || copyOnTable);
                            } catch (Exception e) { /* ignore */ }
                        }

                        // --- V153 tier: >=4 protect characters; <4 survival (dump hand to save life force) ---
                        boolean v153ProtectChars = (totalReserves >= 4);
                        CardCategory v153Cat = (isFromHand && blueprint != null) ? blueprint.getCardCategory() : null;
                        boolean v153HandChar = (v153Cat == CardCategory.CHARACTER);
                        boolean v153HandShip = (v153Cat == CardCategory.STARSHIP || v153Cat == CardCategory.VEHICLE);

                        // Base zone score (higher = lose first).
                        float v153Zone;
                        if (isFromHand) {
                            if (isDuplicate) {
                                v153Zone = 1000.0f;                                  // redundant copy — lose first
                            } else if (v153HandChar) {
                                v153Zone = v153ProtectChars ? 100.0f : 700.0f;       // protected vs dumped
                            } else if (v153HandShip) {
                                v153Zone = v153ProtectChars ? 500.0f : 750.0f;
                            } else {
                                v153Zone = v153ProtectChars ? 600.0f : 850.0f;       // hand junk (interrupt/effect)
                            }
                        } else if (isFromUsedPile) {
                            v153Zone = v153ProtectChars ? 800.0f : 400.0f;           // spent cards; below hand when surviving
                        } else if (isFromReserve) {
                            v153Zone = v153ProtectChars ? 400.0f : 300.0f;
                        } else if (isFromForcePile) {
                            v153Zone = 50.0f;                                        // ALWAYS last
                        } else {
                            v153Zone = 100.0f;                                       // unknown zone fallback
                        }
                        action.addReasoning("V153 ZONE (" + (zone != null ? zone.name() : "?")
                            + ", lifeForce=" + totalReserves + ", protectChars=" + v153ProtectChars + ")", v153Zone);
                        if (isFromHand && v153HandChar) {
                            logger.warn("V153 HAND CHAR: {} protectChars={} lifeForce={} → zone {}", title, v153ProtectChars, totalReserves, v153Zone);
                        }

                        // === V153 THIN RESERVE (Steve, 2026-07-07): spare destiny fuel when the
                        // reserve DECK runs thin. PROTECT-CHARS tier only (life force >= 4). Mistake
                        // 3 (Endor Operations game): reserve deck was down near the last few destiny
                        // cards, yet the protect-chars order keeps reserve (base 400) ABOVE hand
                        // characters (100), so Rando kept eating reserve to protect deployable hand
                        // pieces — burning the destiny draws it needed for battles. This demotes a
                        // reserve loss BELOW hand characters (400 - 335 = 65) once the reserve DECK is
                        // <=10, but keeps it ABOVE the force pile (50) so activation fuel is still lost
                        // last. Keys on reserveDeckSize alone (life force can be comfortable while the
                        // deck is thin — the rest is in used/force). Survival tier (protectChars=false,
                        // life force <4) is intentionally NOT guarded: there reserve is already 300,
                        // below hand junk/chars, and we WANT to preserve the life-force piles to avoid
                        // decking out. Additive — nothing removed.
                        // -335 window: must exceed 400-100=300 to drop reserve under hand chars, and
                        // stay under 400-50=350 to remain above the force pile. Re-derive if the V153
                        // base magnitudes (400/100/50) ever move.
                        if (isFromReserve && v153ProtectChars && reserveDeckSize <= 10) {
                            action.addReasoning("V153 THIN RESERVE (deck=" + reserveDeckSize
                                + "): demote reserve below hand chars to preserve destiny", -335.0f);
                            logger.warn("V153 THIN RESERVE: reserve deck={} (protectChars) — demote reserve loss 400 -> 65", reserveDeckSize);
                        }

                        // === V175 (Steve, 2026-06): PROTECT BATTLE INTERRUPTS FROM THE FODDER PILE ===
                        // Log forensics (ROTS Dooku games): the force-loss picker repeatedly chose
                        // "Lose Welcome Home, Lord Tyranus" — the destiny-substitute died in the
                        // used pile before Tyranus ever battled, and the Sniper copies cycled into
                        // payments/loss. Battle-relevant interrupts in hand (battle destiny /
                        // 'hit' follow-ups / power pumps / substitutes) are the bot's only in-battle
                        // tricks; they rank as "hand junk" (600) in the V153 order. -450 drops them
                        // to 150 — right above HAND CHARACTERS (100), below Reserve (400): lost
                        // near-last, like characters. Survival tier (<4) unchanged — when dumping
                        // hand to stay alive, interrupts still go before characters.
                        // V175a (Steve, 2026-06): TURN-GATED — protection starts on turn 4. Turns
                        // 1-3, losing a known interrupt from hand BEATS blind reserve loss: the
                        // deck is still dense with undeployed key cards, so an early reserve hit
                        // has a much higher chance of killing something crucial. A hand interrupt
                        // is a known, replaceable quantity early. After turn 3, the engine is on
                        // the table and the in-battle tricks become the scarce resource — protect.
                        if (isFromHand && v153Cat == CardCategory.INTERRUPT && v153ProtectChars
                                && blueprint != null && !isDuplicate
                                && context.getTurnNumber() > 3) {
                            try {
                                String v175Gt = blueprint.getGameText();
                                String v175G = v175Gt != null ? v175Gt.toLowerCase(java.util.Locale.ROOT) : "";
                                if (v175G.contains("battle destiny") || v175G.contains("during battle")
                                        || v175G.contains("during a battle") || v175G.contains("'hit'")
                                        || v175G.contains("substitute") || v175G.contains("power +")) {
                                    action.addReasoning("V175 PROTECT BATTLE INTERRUPT: '" + title
                                        + "' is an in-battle trick — lose it near-last, like a character", -450.0f);
                                    logger.warn("V175 PROTECT BATTLE INTERRUPT: {} (zone 600 -> 150)", title);
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // === V178 (Steve, 2026-06): PROTECT WEAPONS THAT HAVE A WIELDER ===
                        // Replay aab2jiaa5sca: Luke's Lightsaber was lost from hand as force fodder
                        // (V153 scored it 650) while Young Skywalker (its Luke-persona wielder) was
                        // in play — the deck's signature weapon thrown away, so Luke fought
                        // bare-handed all game. A weapon in hand whose wielder exists (any
                        // non-undercover friendly character on table, or a character in hand to
                        // deploy it onto) is a key combat piece; rank it near-last like a character
                        // (-450: 600 hand-junk -> 150). Turn-gated > 3 (same early-game reserve-loss
                        // logic as V175a — turns 1-3 the deck is dense, lose the known weapon over a
                        // blind reserve hit). Survival tier (<4 life force) and duplicates unchanged.
                        if (isFromHand && v153Cat == CardCategory.WEAPON && v153ProtectChars
                                && !isDuplicate && context.getTurnNumber() > 3) {
                            try {
                                boolean v178Wielder = false;
                                for (PhysicalCard wp : gameState.getAllPermanentCards()) {
                                    if (wp != null && playerId.equals(wp.getOwner()) && !wp.isUndercover()
                                            && wp.getBlueprint() != null
                                            && wp.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        v178Wielder = true; break;
                                    }
                                }
                                if (!v178Wielder) {
                                    for (PhysicalCard hc : gameState.getHand(playerId)) {
                                        if (hc != null && hc.getBlueprint() != null
                                                && hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            v178Wielder = true; break;
                                        }
                                    }
                                }
                                if (v178Wielder) {
                                    action.addReasoning("V178 PROTECT WEAPON: '" + title
                                        + "' — we have a wielder; lose it near-last, like a character", -450.0f);
                                    logger.warn("V178 PROTECT WEAPON: {} (zone 600 -> 150)", title);
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // Hand floor: keep >=4 cards in hand while life force >= 10.
                        if (isFromHand && handSize <= 4 && totalReserves >= 10) {
                            action.addReasoning("V153 HAND FLOOR: only " + handSize
                                + " in hand (life force " + totalReserves + ">=10) — keep >=4, lose from piles instead -700", -700.0f);
                            logger.warn("V153 HAND FLOOR: handSize={} lifeForce={} — protect hand (-700)", handSize, totalReserves);
                        }

                        // --- V28: Draw Their Fire — Force pile = interrupt ability, protect it ---
                        if (isFromForcePile) {
                            boolean dtfActive = false;
                            try {
                                String dtfOpId = gameState.getOpponent(playerId);
                                for (PhysicalCard dtfC : gameState.getAllPermanentCards()) {
                                    if (dtfC != null && dtfOpId != null && dtfOpId.equals(dtfC.getOwner())
                                        && dtfC.getBlueprint() != null && dtfC.getBlueprint().getTitle() != null
                                        && dtfC.getBlueprint().getTitle().toLowerCase(java.util.Locale.ROOT).contains("draw their fire")
                                        && dtfC.getZone() != null && dtfC.getZone().isInPlay()) {
                                        dtfActive = true;
                                        break;
                                    }
                                }
                            } catch (Exception e) { }
                            if (dtfActive) {
                                int forcePileSize = 0;
                                try { forcePileSize = gameState.getForcePileSize(playerId); } catch (Exception e) { }
                                float dtfForcePenalty = (forcePileSize <= 3) ? -400.0f : -200.0f;
                                action.addReasoning(String.format("V28 DTF FORCE PILE PROTECT: Force pile=%d, DTF active — lose from reserve instead!", forcePileSize), dtfForcePenalty);
                                logger.warn("V28 DTF FORCE PILE PROTECT: {} from Force pile, DTF active, pile={} — HEAVY PENALTY ({})", title, forcePileSize, dtfForcePenalty);
                            }
                        }

                        // --- AiPriorityCards: protect known key cards in HAND or USED pile (V153 extends to Used) ---
                        if ((isFromHand || isFromUsedPile) && !isDuplicate && title != null
                                && AiPriorityCards.isPriorityCardByTitle(title)) {
                            action.addReasoning("V153 PRIORITY CARD: protect '" + title + "' (hand/used) -100", -100.0f);
                        }

                        // --- V21 / V25: objective-critical + Hunt-Down lightsaber protection (hand only) ---
                        if (isFromHand && title != null) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objAnalyzer = context.getObjectiveAnalyzer();
                            if (objAnalyzer != null && objAnalyzer.isAnalyzed()) {
                                if (objAnalyzer.isRequiredCardForFlip(title)) {
                                    action.addReasoning("OBJECTIVE CRITICAL IN HAND - NEVER LOSE!", -9999.0f);
                                } else if (objAnalyzer.isPullableCard(title)) {
                                    action.addReasoning("OBJECTIVE PULLABLE IN HAND - NEVER LOSE!", -9999.0f);
                                }
                                if (objAnalyzer.isHuntDownV()) {
                                    String titleLower = title.toLowerCase(java.util.Locale.ROOT);
                                    if (titleLower.contains("lightsaber")) {
                                        action.addReasoning("V25 HUNT DOWN: PROTECT LIGHTSABER IN HAND!", -500.0f);
                                    }
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose card to forfeit - smart forfeit selection.
     *
     * Priority order (highest first):
     * 1. Hit cards - MUST be forfeited anyway, might as well do it first
     * 2. Pilots on ships - forfeit pilot before ship (ship dying loses pilots too)
     * 3. Low forfeit value cards - satisfy damage efficiently
     * 4. Low power cards - keep high-power cards fighting
     *
     * Avoid:
     * - Ships with pilots aboard (forfeit pilots first!)
     * - High forfeit/power unique characters
     * - Cards with attrition immunity
     *
     * Ported from Python card_selection_evaluator.py _evaluate_forfeit()
     */
    private List<EvaluatedAction> evaluateForfeit(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String textLower = context.getDecisionText().toLowerCase(java.util.Locale.ROOT);
        boolean isOptional = textLower.contains("if desired");

        // V22.4 FIX: Get remaining damage directly from game state.
        // The decision text is always just "Choose a card from battle to forfeit (if desired)"
        // with NO damage numbers embedded, so text-parsing always returned 0 and optional
        // forfeits were always skipped. Query the battle state directly instead.
        int optionalDamageRemaining = 0;
        int optionalAttritionRemaining = 0;
        if (isOptional) {
            SwccgGame forfeitGame = context.getGame();
            String forfeitPlayerId = context.getPlayerId();
            if (forfeitGame != null && forfeitPlayerId != null) {
                try {
                    optionalDamageRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                        .getBattleDamageRemaining(forfeitGame, forfeitPlayerId);
                    optionalAttritionRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                        .getBattleAttritionRemaining(forfeitGame, forfeitPlayerId);
                } catch (Exception e) {
                    logger.debug("Could not read battle damage from game state: {}", e.getMessage());
                }
            }
            logger.info("V22.4 OPTIONAL FORFEIT (game state): isOptional={}, damageRemaining={}, attritionRemaining={}",
                isOptional, optionalDamageRemaining, optionalAttritionRemaining);
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Forfeit card " + cardId
            );

            // V22.4: Optional forfeit handling — COMPLETELY REWORKED
            // Old bug: ALL optional forfeits were avoided (-150). This meant Rando would
            // NEVER voluntarily forfeit characters to satisfy battle damage, leading to
            // massive hand/reserve losses (Emperor Palpatine not forfeited, losing 16 cards instead)
            //
            // NEW LOGIC: If there's battle damage remaining, optional forfeits are GOOD!
            // A character with forfeit=6 satisfies 6 damage in 1 action vs 6 cards from reserve.
            // Only avoid optional forfeits when there's NO damage to satisfy.
            if (isOptional && optionalDamageRemaining <= 0) {
                // V29.13: No battle damage remaining — truly optional forfeit, MUST avoid!
                // Previous bug: VERY_BAD_DELTA (-150) + base (50) = -100, which exactly equals
                // BAD_ACTION_THRESHOLD (-100). The pass check is "< -100", so -100 didn't trigger
                // pass, and Rando forfeited characters immune to remaining attrition!
                // Fix: Use -500 to guarantee score falls well below threshold.
                action.addReasoning("V29.13 IMMUNE/NO DAMAGE - never forfeit voluntarily!", -500.0f);
                logger.warn("V29.13 SKIP FORFEIT: Optional with no damage — PASS! (dmg={}, attr={})",
                    optionalDamageRemaining, optionalAttritionRemaining);
                actions.add(action);
                continue;
            } else if (isOptional && optionalDamageRemaining > 0) {
                // V22.4: Battle damage still remaining! Forfeiting is MUCH better than losing from reserve!
                // This character's forfeit value will satisfy multiple points of damage
                if (gameState != null) {
                    try {
                        PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                        if (card != null) {
                            SwccgCardBlueprint bp = card.getBlueprint();
                            Float forfeitVal = (bp != null && bp.hasForfeitAttribute()) ? bp.getForfeit() : null;
                            float fv = (forfeitVal != null) ? forfeitVal : 0;

                            // V159 unified forfeit picker (dominates old V143/V67bh/V67t/V139-small)
                            float v159 = v159ForfeitScore(card, optionalAttritionRemaining,
                                    optionalDamageRemaining, context.getGame(), context.getPlayerId());
                            if (v159 != 0f) {
                                action.addReasoning(String.format(
                                    "V159 FORFEIT (attr=%d dmg=%d fv=%.0f hit=%s)",
                                    optionalAttritionRemaining, optionalDamageRemaining, fv, card.isHit()), v159);
                                logger.warn("V159 FORFEIT (evaluateForfeit): {} attr={} dmg={} fv={} hit={} score={}",
                                    card.getTitle(), optionalAttritionRemaining, optionalDamageRemaining,
                                    fv, card.isHit(), v159);
                            }

                            // === V143 (Steve, 2026-05-26): HARD BLOCK SMALL-DAMAGE FORFEIT ===
                            // Steve's rule: "If there is 2 or less force to lose from battle
                            // damage and no needed attrition (because either immune or 0 left),
                            // NEVER lose character from site. Bypass all other forfeit scoring."
                            //
                            // This is a HARD BLOCK that dominates V67bd FORFEIT COVERS ALL,
                            // V22.3 FORFEIT FIRST, and any other forfeit-encouraging signals.
                            // Force loss from pile/hand/reserve is always preferred over
                            // forfeiting a character to satisfy ≤2 force of battle damage
                            // when no attrition is owed.
                            if (false /* V159 SUPERSEDED — step 3 handles damage<3 */
                                    && optionalAttritionRemaining == 0 && optionalDamageRemaining > 0
                                    && optionalDamageRemaining <= 2) {
                                action.addReasoning(String.format(
                                    "V143 HARD BLOCK: only %d battle damage, no attrition — NEVER forfeit a character, lose from pile instead",
                                    optionalDamageRemaining), -9999.0f);
                                logger.warn("V143 HARD BLOCK SMALL-DMG FORFEIT: {} dmg={} attr={} → -9999",
                                    card.getTitle(), optionalDamageRemaining, optionalAttritionRemaining);
                                actions.add(action);
                                continue;  // skip all other forfeit scoring for this card
                            }

                            if (false /* V159 SUPERSEDED — step 3 handles fv-based forfeit scoring */ && fv > 0) {
                                // V67t WASTE-AWARE FORFEIT SCORING:
                                // Steve's rule: "if only need to lose 2 or less force from a battle,
                                // keep characters on location and just lose force from reserves."
                                //
                                // Old formula: efficiencyBonus = fv * 20  (gave Sidious fv=7 a +140
                                // bonus for satisfying 1 damage — wasted 6 forfeit. Lost Sidious to
                                // pay 1 damage instead of losing 1 reserve card.)
                                //
                                // New formula: net = savings*20 - waste*50
                                //   savings = min(fv, damage_remaining)  — efficient damage covered
                                //   waste   = max(0, fv - damage_remaining) — over-payment
                                // Heavy waste penalty (-50/pt) outweighs savings (+20/pt) so high-fv
                                // characters never forfeit for tiny damage.
                                int savings = (int) Math.min(fv, optionalDamageRemaining);
                                int waste = (int) Math.max(0f, fv - optionalDamageRemaining);
                                float efficiencyBonus = savings * 20.0f - waste * 50.0f;
                                if (optionalDamageRemaining > 8 && waste == 0) efficiencyBonus += 50.0f;

                                // V67bh (Steve, 2026-05-10): SMALL-DAMAGE PROTECTION FOR
                                // VALUABLE UN-HIT CHARACTERS.
                                //
                                // Steve's rule: damage 1-3 with a high-value (fv ≥ 4)
                                // un-hit character → keep the char, lose from reserve/hand
                                // instead. A hit char already had its fv reset to 0 by the
                                // weapon hit — forfeit costs nothing strategic, no protection.
                                //
                                // Supersedes the earlier V67t SMALL DAMAGE rule (≤2 / fv≥2).
                                // Lower the bar to "fv ≥ 2 / damage ≤ 2" still applies as a
                                // secondary milder discouragement so even cheap chars aren't
                                // wasted on 1 damage.
                                boolean v67bhSmallDmg = optionalDamageRemaining <= 3;
                                boolean v67bhValuable = fv >= 4;
                                boolean v67bhNotHit   = !card.isHit();
                                if (v67bhSmallDmg && v67bhValuable && v67bhNotHit) {
                                    efficiencyBonus -= 400.0f;
                                    action.addReasoning(String.format(
                                        "V67bh PROTECT VALUABLE: %s (fv=%d, not hit) — only %d damage, lose from reserve/hand instead!",
                                        card.getTitle(), (int)fv, optionalDamageRemaining), 0.0f);
                                    logger.warn("V67bh PROTECT VALUABLE: {} fv={} damage={} not-hit → -400",
                                        card.getTitle(), (int)fv, optionalDamageRemaining);
                                } else if (optionalDamageRemaining <= 2 && fv >= 2) {
                                    // V67t secondary backstop for low-value chars vs ≤2 damage
                                    efficiencyBonus -= 250.0f;
                                    action.addReasoning("V67t SMALL DAMAGE: ≤2 damage — keep character on table, lose from reserve!", 0.0f);
                                    logger.warn("V67t SMALL DAMAGE: {} fv={} damage={} → -250 (prefer reserve loss)",
                                        card.getTitle(), (int)fv, optionalDamageRemaining);
                                }

                                action.addReasoning("V22.4/V67t OPTIONAL FORFEIT (savings=" + savings
                                    + " waste=" + waste + " of " + optionalDamageRemaining + " damage)",
                                    efficiencyBonus);
                                logger.info("V67t OPTIONAL FORFEIT: {} fv={} damage={} → savings={} waste={} bonus={}",
                                    card.getTitle(), (int)fv, optionalDamageRemaining, savings, waste, efficiencyBonus);

                                // V67t: Apply V37 PROTECT here too (was only in non-optional path)
                                Float charPower = bp != null && bp.hasPowerAttribute() ? bp.getPower() : null;
                                Float charAbility = bp != null && bp.hasAbilityAttribute() ? bp.getAbility() : null;
                                if (charPower != null && charPower >= 6 && charAbility != null && charAbility >= 4) {
                                    action.addReasoning(String.format(
                                        "V37/V67t PROTECT: %s (power %.0f, ability %.0f) — keep alive!",
                                        card.getTitle(), charPower, charAbility), -150.0f);
                                }
                            } else {
                                // Zero forfeit value — not worth it
                                action.addReasoning("Optional forfeit but zero forfeit value", -80.0f);
                            }

                            // V22.4: Check objective-critical protection even for optional forfeits
                            String fTitle = card.getTitle();
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer optObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (optObjAnalyzer != null && optObjAnalyzer.isAnalyzed() && fTitle != null) {
                                if (optObjAnalyzer.isRequiredCardForFlip(fTitle)) {
                                    action.addReasoning("OBJECTIVE CRITICAL - don't voluntarily forfeit", -9999.0f);
                                } else if (optObjAnalyzer.isPullableCard(fTitle)) {
                                    action.addReasoning("OBJECTIVE PULLABLE - don't voluntarily forfeit", -9999.0f);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
                actions.add(action);
                continue;  // Skip normal scoring — optional forfeit has its own scoring above
            }

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        if (title != null) {
                            action.setDisplayText("Forfeit " + title);
                        }

                        // =======================================================
                        // CRITICAL: Hit cards should ALWAYS be forfeited first!
                        // They're already damaged - no reason to keep them around
                        // =======================================================
                        if (false /* V159 SUPERSEDED — step 1 handles hit cards */ && card.isHit()) {
                            action.addReasoning("ALREADY HIT - forfeit first!", 150.0f);
                            logger.info("🎯 {} is HIT - prioritizing for forfeit", title);
                        }

                        // =======================================================
                        // CRITICAL: Dead cards (persona already deployed) should
                        // be forfeited - they can never be played anyway!
                        // =======================================================
                        SwccgGame game = context.getGame();
                        String playerId = context.getPlayerId();
                        if (game != null && playerId != null &&
                            AiCardHelper.isDeadCard(card, game, playerId)) {
                            action.addReasoning("☠️ DEAD CARD (persona on table) - forfeit!", 140.0f);
                            logger.info("☠️ {} is a DEAD CARD - prioritizing for forfeit", title);
                        }

                        // =======================================================
                        // Check if this is a pilot attached to a ship
                        // Pilots on ships should be forfeited BEFORE the ship!
                        // =======================================================
                        PhysicalCard attachedTo = card.getAttachedTo();
                        if (attachedTo != null) {
                            SwccgCardBlueprint attachedBlueprint = attachedTo.getBlueprint();
                            if (attachedBlueprint != null) {
                                CardCategory attachedCat = attachedBlueprint.getCardCategory();
                                if (attachedCat == CardCategory.STARSHIP || attachedCat == CardCategory.VEHICLE) {
                                    action.addReasoning("PILOT ON SHIP - forfeit first!", 50.0f);
                                }
                            }
                        }

                        // =======================================================
                        // Check if this is a ship/vehicle with cards aboard
                        // Should NOT be forfeited until pilots are gone!
                        // =======================================================
                        List<PhysicalCard> attachedCards = gameState.getAttachedCards(card);
                        if (attachedCards != null && !attachedCards.isEmpty()) {
                            boolean hasCharacterAboard = false;
                            for (PhysicalCard attached : attachedCards) {
                                if (attached.getBlueprint() != null &&
                                    attached.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                    hasCharacterAboard = true;
                                    break;
                                }
                            }
                            if (hasCharacterAboard) {
                                // V48: NEVER forfeit a ship with crew aboard — you lose the ship
                                // AND all its pilots/passengers. Forfeit individual crew instead.
                                // Executor + Piett + Gherant = 3 cards lost for 1 forfeit. Catastrophic.
                                int crewCount = 0;
                                for (PhysicalCard att : attachedCards) {
                                    if (att.getBlueprint() != null &&
                                        att.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        crewCount++;
                                    }
                                }
                                action.addReasoning(String.format(
                                    "V48 SHIP WITH CREW: %s has %d crew aboard — forfeit crew first, not the ship!",
                                    title, crewCount), -9999.0f);
                                logger.warn("V48 SHIP FORFEIT BLOCK: {} has {} crew — NEVER forfeit ship with crew aboard!",
                                    title, crewCount);
                            }
                        }

                        // === V37: HIGH-VALUE CHARACTER PROTECTION ===
                        // Characters with high power/ability should be protected from
                        // unnecessary forfeiting. Vader, Emperor, etc. are expendable in
                        // Hunt Down but only when there's actual damage to absorb.
                        // If a character "may not be used to satisfy attrition" (hit by weapon),
                        // the game forces them to be forfeited to battle damage instead.
                        // High-power unique characters that AREN'T hit should be kept alive.
                        // V139 (Steve, 2026-05-26): BUMP HIGH-VALUE CHARACTER PROTECTION
                        //
                        // Steve's standing rule: "Rando should always choose the LEAST
                        // VALUE characters to satisfy battle damage first." Previous
                        // magnitudes were too weak — Tyranus (power 6, ability 5, forfeit
                        // 5) was forfeited because +50 forfeit-efficiency bonus dominated
                        // -25 valuable-unique protection. New magnitudes ensure key
                        // characters are essentially never picked unless they're the
                        // ONLY option.
                        if (false /* V159 SUPERSEDED — step 3 (damage>=3) makes V139 yield; step 2 has release valve */
                                && blueprint != null && !card.isHit()) {
                            Float charPower = blueprint.hasPowerAttribute() ? blueprint.getPower() : null;
                            Float charAbility = blueprint.hasAbilityAttribute() ? blueprint.getAbility() : null;
                            if (charPower != null && charPower >= 6 && charAbility != null && charAbility >= 4) {
                                action.addReasoning(String.format(
                                    "V37/V139 PROTECT: %s (power %.0f, ability %.0f) — never forfeit unless only option!",
                                    title, charPower, charAbility), -400.0f);
                            } else if (charPower != null && charPower >= 5) {
                                action.addReasoning(String.format(
                                    "V139 PROTECT: %s (power %.0f) — high-power fighter, save for battle",
                                    title, charPower), -200.0f);
                            } else if (charAbility != null && charAbility >= 4) {
                                action.addReasoning(String.format(
                                    "V139 PROTECT: %s (ability %.0f) — destiny draw value, save",
                                    title, charAbility), -200.0f);
                            }
                        }

                        if (blueprint != null) {
                            // Forfeit value scoring - lower forfeit = better to forfeit (cheap loss).
                            // CRITICAL: hasForfeitAttribute() check first (weapons throw).
                            Float forfeit = blueprint.hasForfeitAttribute() ? blueprint.getForfeit() : null;
                            if (forfeit != null) {
                                // forfeit=0 -> +100, forfeit=7 -> +30, forfeit=10 -> 0
                                float forfeitScore = Math.max(0, 100 - (forfeit * 10));
                                action.addReasoning(
                                    String.format("Forfeit value %.0f", forfeit),
                                    forfeitScore
                                );
                            }

                            // Power scoring - bumped magnitudes (V139)
                            if (blueprint.hasPowerAttribute()) {
                                Float power = blueprint.getPower();
                                if (power != null) {
                                    if (power <= 2) {
                                        action.addReasoning("Low power - cheap loss, forfeit first", 50.0f);
                                    } else if (power >= 5) {
                                        action.addReasoning("V139 High power - prefer keeping for battle", -100.0f);
                                    }
                                }
                            }

                            // V139: Protect unique high-value characters HARDER
                            if (blueprint.getUniqueness() == Uniqueness.UNIQUE) {
                                Float ability = blueprint.hasAbilityAttribute() ? blueprint.getAbility() : null;
                                Float power = blueprint.hasPowerAttribute() ? blueprint.getPower() : null;
                                if ((ability != null && ability >= 5) || (power != null && power >= 5)) {
                                    // High-value unique: -300 ensures forfeit only as last resort
                                    action.addReasoning("V139 VALUABLE UNIQUE - never forfeit unless forced", -300.0f);
                                } else {
                                    // Generic unique: still avoid but less aggressively
                                    action.addReasoning("V139 Unique - avoid forfeiting", -100.0f);
                                }
                            }

                            // Characters with extra destiny draws are valuable
                            // TODO: Check for destiny draw bonuses when API available
                        }

                        // V21: OBJECTIVE-CRITICAL CARD PROTECTION (forfeit)
                        String fTitle = card.getTitle();
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer fObjAnalyzer = context.getObjectiveAnalyzer();
                        if (fObjAnalyzer != null && fObjAnalyzer.isAnalyzed() && fTitle != null) {
                            if (fObjAnalyzer.isRequiredCardForFlip(fTitle)) {
                                action.addReasoning("OBJECTIVE CRITICAL - NEVER FORFEIT!", -9999.0f);
                                logger.warn("V21 HARD BAN: {} is REQUIRED for flip - never forfeit!", fTitle);
                            } else if (fObjAnalyzer.isPullableCard(fTitle)) {
                                action.addReasoning("OBJECTIVE PULLABLE - NEVER FORFEIT!", -9999.0f);
                                logger.warn("V21 HARD BAN: {} is objective pullable - never forfeit!", fTitle);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Handle combined "Force to lose OR forfeit" decisions.
     *
     * CRITICAL SWCCG RULES:
     * - ATTRITION can ONLY be satisfied by forfeiting cards (not Force loss)
     * - Battle damage can be satisfied by EITHER Force loss OR forfeiting
     *
     * Strategy:
     * - If attrition is remaining, MUST forfeit (prioritize hit cards)
     * - If only battle damage, prefer losing Force (saves cards)
     * - Exception: if we have hit cards, forfeit them first anyway
     *
     * Ported from Python card_selection_evaluator.py _evaluate_force_loss_or_forfeit()
     */
    private List<EvaluatedAction> evaluateForceLossOrForfeit(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String text = context.getDecisionText();
        String textLower = text.toLowerCase(java.util.Locale.ROOT);

        // Get attrition and damage remaining directly from game state.
        // Text-parsing is unreliable — the decision text does not embed damage counts.
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        int attritionRemaining = 0;
        int damageRemaining = 0;
        if (game != null && playerId != null) {
            try {
                attritionRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                    .getBattleAttritionRemaining(game, playerId);
                damageRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                    .getBattleDamageRemaining(game, playerId);
            } catch (Exception e) {
                logger.debug("Could not read battle damage from game state: {}", e.getMessage());
            }
        }

        logger.info("🎯 Force loss OR forfeit (game state): attrition={}, damage={}", attritionRemaining, damageRemaining);

        // V153 (Steve, 2026-05-28): life force = reserve + used + force pile (the lose
        // condition; hand and table are excluded). Used by the V153 force-loss tier below.
        int totalReservesV127 = 0;
        // V153 THIN RESERVE (Steve, 2026-07-07): reserve DECK size in isolation (battle mirror).
        int reserveDeckSizeV127 = 0;
        if (gameState != null && playerId != null) {
            try {
                reserveDeckSizeV127 = gameState.getReserveDeckSize(playerId);
                totalReservesV127 = reserveDeckSizeV127
                    + gameState.getUsedPile(playerId).size()
                    + gameState.getForcePileSize(playerId);
            } catch (Exception e) { /* fallback to 0 */ }
        }

        // Track if we have any hit cards or dead cards available for forfeit
        boolean hasHitCards = false;
        boolean hasDeadCards = false;
        PhysicalCard bestHitCard = null;
        float bestHitForfeit = Float.MAX_VALUE;
        // Note: game and playerId already declared above for battle state queries

        // First pass: identify hit cards and dead cards
        for (String cardId : context.getCardIds()) {
            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        if (card.isHit()) {
                            hasHitCards = true;
                            SwccgCardBlueprint bp = card.getBlueprint();
                            // CRITICAL: Check hasForfeitAttribute() first - weapons throw exception!
                            float forfeit = bp != null && bp.hasForfeitAttribute() && bp.getForfeit() != null ? bp.getForfeit() : 0;
                            if (forfeit < bestHitForfeit) {
                                bestHitForfeit = forfeit;
                                bestHitCard = card;
                            }
                        }
                        // Check for dead cards (persona already deployed)
                        if (game != null && playerId != null &&
                            AiCardHelper.isDeadCard(card, game, playerId)) {
                            hasDeadCards = true;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Choose " + cardId
            );

            // === V154 (Steve, 2026-05-28): WEAPON-LOSS EDGE CASE ===
            // Some effects (e.g. in the Shadow Collective deck) let Rando lose a deployed
            // WEAPON to satisfy battle damage/attrition. If a weapon shows up as an option
            // in THIS decision, that effect is active — strip the weapon FIRST, ahead of
            // everything including hit characters. A weapon on a HIT host is the best case:
            // that character is forfeited anyway and its weapon would otherwise be lost for
            // free WITH it, so lose the weapon first for the extra coverage, then forfeit the
            // hit character separately next. Global detection via CardCategory.WEAPON — no
            // hardcoded card names. Scores above V146 hit-forfeit (+1500) so it always wins.
            if (gameState != null) {
                try {
                    PhysicalCard v154Card = gameState.findCardById(Integer.parseInt(cardId));
                    if (v154Card != null && v154Card.getBlueprint() != null
                            && v154Card.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                        PhysicalCard v154Host = v154Card.getAttachedTo();
                        boolean v154HostHit = (v154Host != null && v154Host.isHit());
                        float v154Boost = v154HostHit ? 2200.0f : 2000.0f;
                        action.setDisplayText("Lose weapon " + (v154Card.getTitle() != null ? v154Card.getTitle() : cardId));
                        action.addReasoning("V154 WEAPON-LOSS: strip weapon first for extra coverage"
                            + (v154HostHit ? " (host is HIT — lost anyway)" : "") + " — before hit chars", v154Boost);
                        logger.warn("V154 WEAPON-LOSS: {} hostHit={} → +{}", v154Card.getTitle(), v154HostHit, v154Boost);
                        actions.add(action);
                        continue;
                    }
                } catch (NumberFormatException e) { /* ignore */ }
            }

            // V22.4: Determine if this is a Force loss option or a Forfeit option
            // OLD BUG: Used cardId.startsWith("fp_") which NEVER matches GEMP's numeric IDs!
            // All cards were treated as forfeit options, and force loss penalty never applied.
            // NEW: Check the card's actual zone — hand/reserve/force pile = force loss, table = forfeit
            boolean isForceLosSOption = false;
            if (gameState != null) {
                try {
                    PhysicalCard zoneCheckCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (zoneCheckCard != null) {
                        com.gempukku.swccgo.common.Zone cardZone = zoneCheckCard.getZone();
                        if (cardZone != null) {
                            String zoneName = cardZone.name();
                            isForceLosSOption = zoneName.contains("HAND") ||
                                zoneName.contains("RESERVE") ||
                                zoneName.contains("FORCE_PILE") ||
                                zoneName.contains("USED_PILE");
                        }
                    }
                } catch (NumberFormatException e) {
                    // Fallback — assume forfeit option
                }
            }

            // V118 (Steve, 2026-05-22): SAVE CHARACTERS FROM SMALL BATTLE DAMAGE.
            // Per Steve: "Don't forfeit guys from site if battle damage is 2 or less.
            // Unless they are hit of course. Characters are typically worth more than
            // 2 force to save from dying."
            // Apply this BEFORE branching into force-loss vs forfeit handlers so both
            // sides see the same nudge: when battle damage (NOT attrition) is 1-2:
            //   - force-loss options get +200 (prefer reserve/hand loss)
            //   - non-hit character forfeit options get -500 (save the character)
            // Attrition damage MUST be satisfied by forfeit, so this only fires for
            // pure-damage situations.
            boolean v118SmallDamage = damageRemaining > 0 && damageRemaining <= 2 && attritionRemaining <= 0;
            if (v118SmallDamage && isForceLosSOption) {
                action.addReasoning(
                    "V118 SMALL DAMAGE: only " + damageRemaining
                        + " battle damage — lose from reserves instead of forfeiting a character",
                    200.0f);
                logger.info("V118 SMALL DAMAGE force-loss boost (+200) — damageRemaining={}", damageRemaining);
            } else if (v118SmallDamage && gameState != null) {
                try {
                    PhysicalCard v118Card = gameState.findCardById(Integer.parseInt(cardId));
                    if (v118Card != null && v118Card.getBlueprint() != null
                            && v118Card.getBlueprint().getCardCategory() == CardCategory.CHARACTER
                            && !v118Card.isHit()) {
                        action.addReasoning(
                            "V118 SAVE CHARACTER: only " + damageRemaining
                                + " battle damage — characters worth more than that, lose from reserves!",
                            -500.0f);
                        logger.info("V118 SAVE CHARACTER (-500) — {} not-hit, damage={}",
                            v118Card.getTitle(), damageRemaining);
                    }
                } catch (NumberFormatException e) { /* ignore */ }
            }

            if (isForceLosSOption) {
                // Force loss option — card from hand/reserve/force pile
                action.setDisplayText("Lose Force from pile");

                // V67be (Steve, 2026-05-09): V67y REMOVED from this combined prompt.
                //
                // Steve's clarification: "V67y was only meant for moments when force
                // is required to come from hand or reserves. In battle you still have
                // the option to forfeit from site. V67y outweighs a very important
                // logic [V22.3 forfeit-first]."
                //
                // V67y added +500 to pile-loss / -500 to hand-loss. That dominated
                // V22.3's -40/-80/-120 forfeit-first penalty, silently regressing
                // the original "forfeit before burning reserve" rule. Replay
                // jzhprmm64t32wz8g battles #1 & #2: Rando burned 4 reserve cards
                // before forfeiting Chiraneau anyway.
                //
                // FIX: V67y stays in the STANDALONE evaluateForceLoss method (V29.8
                // already there with the same zone-aware semantics) for non-battle
                // force-loss prompts. The combined battle prompt is governed by
                // V22.3 (forfeit-first) + V67bd (attrition forfeit bonus). Pile-vs-
                // hand within force-loss is handled by V25 character/ship hand
                // penalties further down — no zone bonus needed here.
                //
                // (No-op block: V67y deliberately not applied in this method.)

                // V25: HUNT DOWN V — Protect lightsabers from Force loss
                if (gameState != null) {
                    try {
                        PhysicalCard lossCard = gameState.findCardById(Integer.parseInt(cardId));
                        if (lossCard != null && lossCard.getTitle() != null) {
                            String lossTitle = lossCard.getTitle().toLowerCase(java.util.Locale.ROOT);
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer lossOA = context.getObjectiveAnalyzer();
                            if (lossOA != null && lossOA.isAnalyzed() && lossOA.isHuntDownV()
                                && lossTitle.contains("lightsaber")) {
                                action.addReasoning("V25 HUNT DOWN: PROTECT LIGHTSABER from loss!", -400.0f);
                                logger.warn("V25 HUNT DOWN COMBINED-LOSS: {} is a lightsaber — PROTECT (-400)", lossCard.getTitle());
                            }
                        }
                    } catch (NumberFormatException e) { /* ignore */ }
                }

// ===================================================================
// OLD V127/V29.8 ZONE SCORING (battle evaluateForceLossOrForfeit) — SUPERSEDED by V153 below.
// Steve's comment-out rule (2026-05-28): kept inline & commented for dev
// reference, not deleted. This is the EXACT pre-V153 logic (the inverted
// healthy/low zone magnitudes V153 fixed). A future clean build may strip it.
// ===================================================================
//                 // === V127 (Steve, 2026-05-22): V29.8 MIRRORED INTO COMBINED HANDLER ===
//                 // V119 (V101's mirror) deleted. V101's blanket Used+500/Reserve+300/Hand-500
//                 // silently dominated V29.8's duplicate + life-force-low logic in the standalone
//                 // handler — same regression would hit here. V127 mirrors V29.8's full conditional
//                 // tiered scoring instead, restoring V13 priority (Duplicate Hand > Used > Reserve
//                 // > Hand > Force Pile) with Steve's ≤10 reserves life-force-low threshold.
//                 if (gameState != null) {
//                     try {
//                         PhysicalCard v127Card = gameState.findCardById(Integer.parseInt(cardId));
//                         if (v127Card != null) {
//                             com.gempukku.swccgo.common.Zone v127Zone = v127Card.getZone();
//                             String v127ZoneName = v127Zone != null ? v127Zone.name() : "";
//                             String v127Title = v127Card.getTitle();
//                             SwccgCardBlueprint v127Bp = v127Card.getBlueprint();
//                             boolean v127FromUsed = v127ZoneName.contains("USED");
//                             boolean v127FromReserve = v127ZoneName.contains("RESERVE");
//                             boolean v127FromForcePile = v127ZoneName.contains("FORCE_PILE");
//                             boolean v127FromHand = v127ZoneName.contains("HAND");
// 
//                             // Zone scoring (same magnitudes as evaluateForceLoss V29.8)
//                             if (v127FromUsed) {
//                                 if (lifeForceHealthyV127) {
//                                     action.addReasoning("V29.8 USED PILE (healthy combined): already-spent cards lost first +400", 400.0f);
//                                 } else {
//                                     action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReservesV127 + " combined): mild protect Used -200", -200.0f);
//                                 }
//                             } else if (v127FromReserve) {
//                                 if (lifeForceHealthyV127) {
//                                     action.addReasoning("V29.8 RESERVE (healthy combined): second pile preference +300", 300.0f);
//                                 } else {
//                                     action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReservesV127 + " combined): PROTECT Reserve -250", -250.0f);
//                                 }
//                             } else if (v127FromForcePile) {
//                                 if (lifeForceHealthyV127) {
//                                     action.addReasoning("V29.8 FORCE PILE (healthy combined): last positive pile +100", 100.0f);
//                                 } else {
//                                     action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReservesV127 + " combined): protect force pile -100", -100.0f);
//                                 }
//                             } else if (v127FromHand) {
//                                 if (lifeForceHealthyV127) {
//                                     action.addReasoning("V29.8 HAND PROTECT (healthy combined " + totalReservesV127 + " life force): deploy options -300", -300.0f);
//                                 } else {
//                                     action.addReasoning("V29.8 LIFE FORCE LOW (" + totalReservesV127 + " combined): prefer Hand loss +400", 400.0f);
//                                 }
// 
//                                 // Duplicate detection (mirror of evaluateForceLoss block) — applies to hand only
//                                 try {
//                                     int v127CopiesInHand = 0;
//                                     boolean v127CopyOnTable = false;
//                                     java.util.List<PhysicalCard> v127Hand = gameState.getHand(playerId);
//                                     if (v127Hand != null && v127Title != null) {
//                                         for (PhysicalCard hc : v127Hand) {
//                                             if (hc != null && v127Title.equals(hc.getTitle())) v127CopiesInHand++;
//                                         }
//                                     }
//                                     if (v127Title != null) {
//                                         for (PhysicalCard tc : gameState.getAllPermanentCards()) {
//                                             if (tc != null && playerId.equals(tc.getOwner())
//                                                 && v127Title.equals(tc.getTitle())
//                                                 && tc.getZone() != null && tc.getZone().isInPlay()) {
//                                                 v127CopyOnTable = true;
//                                                 break;
//                                             }
//                                         }
//                                     }
//                                     boolean v127IsDuplicate = (v127CopiesInHand >= 2 || v127CopyOnTable);
//                                     if (v127IsDuplicate) {
//                                         action.addReasoning("V29.8 DUPLICATE (combined): another copy available — lose FIRST +800", 800.0f);
//                                         logger.warn("V29.8 DUPLICATE (combined): {} is a duplicate — PREFERRED loss (+800)", v127Title);
//                                     } else if (v127Bp != null) {
//                                         CardCategory v127Cat = v127Bp.getCardCategory();
//                                         if (v127Cat == CardCategory.CHARACTER) {
//                                             action.addReasoning("V29.8 HAND PROTECT (combined): CHARACTER — needs deploying -150", -150.0f);
//                                         } else if (v127Cat == CardCategory.STARSHIP || v127Cat == CardCategory.VEHICLE) {
//                                             action.addReasoning("V29.8 HAND PROTECT (combined): Ship/vehicle needs deploying -80", -80.0f);
//                                         } else if (v127Cat == CardCategory.INTERRUPT) {
//                                             action.addReasoning("V29.8 HAND (combined): Interrupt — least bad hand loss +50", 50.0f);
//                                         }
//                                     }
//                                 } catch (Exception dupE) { /* ignore */ }
//                             }
//                         }
//                     } catch (NumberFormatException e) { /* ignore */ }
//                 }
                // === V153 (Steve, 2026-05-28): UNIFIED FORCE-LOSS ORDER — mirrored from
                // evaluateForceLoss into the battle handler's force-loss side. Replaces the
                // OLD inverted V127/V29.8 mirror. Same char/life-force tiers:
                //   >= 4 (protect characters): Dup > Used > hand junk > Reserve > HAND CHARS > Force pile
                //   <  4 (survival, save life-force piles): Dup > hand junk > HAND CHARS > Used > Reserve > Force pile
                // life force = totalReservesV127 (reserve+used+force). Hand/forfeit are
                // life-force-free; reserve/used/force are the lose-condition. This block scores
                // only the force-loss (pile/hand) options; the forfeit side (V146/V67bd/V139/...)
                // below is unchanged and still decides forfeit-vs-loss. V150/V22.3 forfeit-first
                // nudges follow this block.
                if (gameState != null) {
                    try {
                        PhysicalCard v153Card = gameState.findCardById(Integer.parseInt(cardId));
                        if (v153Card != null) {
                            com.gempukku.swccgo.common.Zone v153ZoneObj = v153Card.getZone();
                            String v153ZoneName = v153ZoneObj != null ? v153ZoneObj.name() : "";
                            String v153Title = v153Card.getTitle();
                            SwccgCardBlueprint v153Bp = v153Card.getBlueprint();
                            boolean v153FromUsed = v153ZoneName.contains("USED");
                            boolean v153FromReserve = v153ZoneName.contains("RESERVE");
                            boolean v153FromForcePile = v153ZoneName.contains("FORCE_PILE");
                            boolean v153FromHand = v153ZoneName.contains("HAND");

                            // duplicate-in-hand detection
                            boolean v153IsDuplicate = false;
                            if (v153FromHand && v153Title != null) {
                                try {
                                    int copies = 0;
                                    boolean onTable = false;
                                    java.util.List<PhysicalCard> h = gameState.getHand(playerId);
                                    if (h != null) for (PhysicalCard hc : h) if (hc != null && v153Title.equals(hc.getTitle())) copies++;
                                    for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                        if (tc != null && playerId.equals(tc.getOwner()) && v153Title.equals(tc.getTitle())
                                            && tc.getZone() != null && tc.getZone().isInPlay()) { onTable = true; break; }
                                    }
                                    v153IsDuplicate = (copies >= 2 || onTable);
                                } catch (Exception e) { /* ignore */ }
                            }

                            boolean v153ProtectChars = (totalReservesV127 >= 4);
                            CardCategory v153Cat = v153Bp != null ? v153Bp.getCardCategory() : null;
                            boolean v153HandChar = (v153Cat == CardCategory.CHARACTER);
                            boolean v153HandShip = (v153Cat == CardCategory.STARSHIP || v153Cat == CardCategory.VEHICLE);

                            float v153Zone;
                            if (v153FromHand) {
                                if (v153IsDuplicate) v153Zone = 1000.0f;
                                else if (v153HandChar) v153Zone = v153ProtectChars ? 100.0f : 700.0f;
                                else if (v153HandShip) v153Zone = v153ProtectChars ? 500.0f : 750.0f;
                                else v153Zone = v153ProtectChars ? 600.0f : 850.0f;
                            } else if (v153FromUsed) {
                                v153Zone = v153ProtectChars ? 800.0f : 400.0f;
                            } else if (v153FromReserve) {
                                v153Zone = v153ProtectChars ? 400.0f : 300.0f;
                            } else if (v153FromForcePile) {
                                v153Zone = 50.0f;
                            } else {
                                v153Zone = 100.0f;
                            }
                            action.addReasoning("V153 ZONE (" + v153ZoneName + ", lifeForce=" + totalReservesV127
                                + ", protectChars=" + v153ProtectChars + ")", v153Zone);

                            // === V153 THIN RESERVE (Steve, 2026-07-07): battle-mirror of the regular
                            // block's thin-reserve guard. PROTECT-CHARS tier only. Reserve DECK <=10 ->
                            // demote reserve loss below hand chars (400 - 335 = 65), still above the
                            // force pile (50). Keys on reserveDeckSizeV127 alone. Survival tier
                            // untouched. Additive. See the regular evaluateForceLoss block for the full
                            // rationale + boundary window.
                            if (v153FromReserve && v153ProtectChars && reserveDeckSizeV127 <= 10) {
                                action.addReasoning("V153 THIN RESERVE (deck=" + reserveDeckSizeV127
                                    + "): demote reserve below hand chars to preserve destiny", -335.0f);
                                logger.warn("V153 THIN RESERVE (battle): reserve deck={} (protectChars) — demote reserve loss 400 -> 65", reserveDeckSizeV127);
                            }

                            // Hand floor: keep >=4 cards in hand while life force >= 10.
                            if (v153FromHand) {
                                int v153HandSize = 0;
                                try { v153HandSize = gameState.getHand(playerId).size(); } catch (Exception e) { }
                                if (v153HandSize <= 4 && totalReservesV127 >= 10) {
                                    action.addReasoning("V153 HAND FLOOR: only " + v153HandSize
                                        + " in hand (life force " + totalReservesV127 + ">=10) — keep >=4 -700", -700.0f);
                                }
                            }

                            // Protections (mirror regular evaluateForceLoss): objective-critical + priority cards
                            if (v153Title != null) {
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v153OA = context.getObjectiveAnalyzer();
                                if (v153OA != null && v153OA.isAnalyzed()) {
                                    if (v153OA.isRequiredCardForFlip(v153Title)) {
                                        action.addReasoning("OBJECTIVE CRITICAL - NEVER LOSE!", -9999.0f);
                                    } else if (v153OA.isPullableCard(v153Title)) {
                                        action.addReasoning("OBJECTIVE PULLABLE - NEVER LOSE!", -9999.0f);
                                    }
                                }
                                if ((v153FromHand || v153FromUsed) && !v153IsDuplicate
                                        && AiPriorityCards.isPriorityCardByTitle(v153Title)) {
                                    action.addReasoning("V153 PRIORITY CARD: protect '" + v153Title + "' -100", -100.0f);
                                }
                            }
                        }
                    } catch (NumberFormatException e) { /* ignore */ }
                }

                if (attritionRemaining > 0) {
                    // V150 (Steve, 2026-05-28): when attrition is owed, a forfeit is
                    // MANDATORY (pile loss can't satisfy attrition). That mandatory
                    // forfeit's value ALSO covers battle damage. So paying battle
                    // damage from the pile while attrition is still owed wastes pile
                    // cards — the forfeit you're forced to make would have covered them.
                    //
                    // Replay gzv8mrd0rbtvcm9r: Rando paid 11 battle damage card-by-card
                    // from piles, THEN forfeited for 5 attrition — bleeding ~10 cards.
                    // My V139/V143/V145 protection work (this session) over-shrank the
                    // forfeit score so pile loss edged it out even at huge damage.
                    //
                    // Fix: strong pile-loss penalty while attrition > 0 (was VERY_BAD_DELTA
                    // -150, now -500) so forfeits win until attrition is satisfied. V139
                    // still picks the CHEAPEST character among forfeit options; once
                    // attrition hits 0 (next decision cycle), normal V143/V139 pile
                    // preference resumes for any small remaining battle damage.
                    action.addReasoning("V150 CANNOT satisfy attrition with Force loss — forfeit covers attrition+damage together, don't waste pile!", -500.0f);
                } else if (damageRemaining > 0) {
                    // V22.3: ALWAYS prefer forfeiting characters over losing from hand/reserve!
                    // Forfeiting a character with forfeit=5 satisfies 5 damage with 1 card.
                    // Losing from hand/reserve satisfies only 1 damage per card.
                    // Example: 15 damage, forfeit 2 chars (forfeit 5 each) = 10 satisfied + 5 from hand = 7 cards total
                    // vs losing 15 from hand = 15 cards total. Forfeiting saves 8 cards!
                    if (hasHitCards) {
                        action.addReasoning("V22.3: Have hit cards to forfeit first - much more efficient!", -80.0f);
                    } else if (hasDeadCards) {
                        action.addReasoning("V22.3: Have dead cards to forfeit - they satisfy multiple damage!", -80.0f);
                    } else {
                        // V22.3: PENALIZE force loss — characters satisfy more damage per card
                        // The higher the remaining damage, the worse force loss is
                        float forceLossPenalty = -40.0f;
                        if (damageRemaining > 5) forceLossPenalty = -80.0f;
                        if (damageRemaining > 10) forceLossPenalty = -120.0f;
                        action.addReasoning("V22.3: FORFEIT CHARACTERS FIRST - they cover " +
                            "multiple damage points per card! (" + damageRemaining + " damage left)", forceLossPenalty);
                    }
                }
            } else {
                // Forfeit card option
                if (gameState != null) {
                    try {
                        PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                        if (card != null) {
                            String title = card.getTitle();
                            action.setDisplayText("Forfeit " + (title != null ? title : cardId));

                            // V146 (Steve, 2026-05-27): HIT cards MUST forfeit first
                            //
                            // Steve: "Rando lost force from piles when he had 4 force to lose
                            // but one of his characters was hit. He has to lose hit characters
                            // first."
                            //
                            // Original ALREADY HIT bonus was +200 — pile loss at ~+360 still
                            // beat it. Bumped to +1500 so hit characters always dominate over
                            // pile loss AND V139 protections for non-hit alternatives.
                            // Hit characters have fv=0 effective and are already wasted; forfeit
                            // is essentially free.
                            if (false /* V159 SUPERSEDED — step 1 handles hit cards */ && card.isHit()) {
                                action.addReasoning("V146 ALREADY HIT - forfeit immediately, hit chars are dead weight!", 1500.0f);
                                logger.info("🎯 V146 Prioritizing HIT card for forfeit: {} → +1500", title);
                            }

                            // Dead cards (persona already deployed) - high priority to forfeit!
                            if (false /* V159 SUPERSEDED — step 1 handles dead cards */
                                && game != null && playerId != null &&
                                AiCardHelper.isDeadCard(card, game, playerId)) {
                                action.addReasoning("☠️ V146 DEAD CARD - persona on table, forfeit!", 1200.0f);
                                logger.info("☠️ V146 Prioritizing DEAD CARD for forfeit: {} → +1200", title);
                            }

                            // === V143 (Steve, 2026-05-26): HARD BLOCK SMALL-DAMAGE FORFEIT (FLoF mirror) ===
                            // Steve's rule: "If there is 2 or less force to lose from battle
                            // damage and no needed attrition, NEVER lose character from site.
                            // Bypass all other forfeit scoring." Dominates V67bd FORFEIT
                            // COVERS ALL (+960) and any other forfeit-encouraging signal.
                            if (false /* V159 SUPERSEDED — step 3 (damage<3 -> protect) */
                                    && attritionRemaining == 0 && damageRemaining > 0 && damageRemaining <= 2) {
                                action.addReasoning(String.format(
                                    "V143 HARD BLOCK (FLoF): only %d battle damage, no attrition — NEVER forfeit, lose from pile",
                                    damageRemaining), -9999.0f);
                                logger.warn("V143 HARD BLOCK (FLoF): {} dmg={} attr={} → -9999",
                                    card.getTitle(), damageRemaining, attritionRemaining);
                                actions.add(action);
                                continue;
                            }

                            // FORFEIT EFFICIENCY: A character that covers attrition AND/OR battle damage
                            // in a single forfeit is far more efficient than losing one reserve card
                            // per point of damage. Always forfeit before burning reserve.
                            SwccgCardBlueprint bp = card.getBlueprint();
                            Float forfeitVal = bp != null && bp.hasForfeitAttribute() ? bp.getForfeit() : null;
                            float fv = forfeitVal != null ? forfeitVal : 0;
                            int totalRemaining = attritionRemaining + damageRemaining;

                            // V159 unified forfeit picker (dominates old V143/V67bh/V67t/V139-small;
                            // see helper at end of class). Replay l3wvdgfkfyd2gdl9 turn 5/6: pile loss
                            // beat forfeit when attrition==0 because V67bd/V150 only fire on attrition;
                            // V159 step-3 ("damage >= 3 -> forfeit considered") makes the efficient
                            // forfeit win for pure damage too.
                            float v159 = v159ForfeitScore(card, attritionRemaining, damageRemaining,
                                    game, playerId);
                            if (v159 != 0f) {
                                action.addReasoning(String.format(
                                    "V159 FORFEIT (attr=%d dmg=%d fv=%.0f hit=%s)",
                                    attritionRemaining, damageRemaining, fv, card.isHit()), v159);
                                logger.warn("V159 FORFEIT (FLoF): {} attr={} dmg={} fv={} hit={} score={}",
                                    card.getTitle(), attritionRemaining, damageRemaining, fv, card.isHit(), v159);
                            }

                            // V145 (Steve, 2026-05-26): immune-to-attrition check for V67bd.
                            // Characters immune to attrition CANNOT satisfy attrition by
                            // forfeit. Sidious (immune to attrition) was scored +960 by
                            // V67bd for "covers all 7" but he could only cover the 2
                            // damage portion, not the 5 attrition. Detect immunity via
                            // blueprint game text scan.
                            boolean v145ImmuneToAttrition = false;
                            try {
                                if (bp != null && bp.getGameText() != null) {
                                    String gtLower = bp.getGameText().toLowerCase(java.util.Locale.ROOT);
                                    if (gtLower.contains("immune to attrition")) {
                                        v145ImmuneToAttrition = true;
                                    }
                                }
                            } catch (Exception ignore) { /* false */ }

                            if (false /* V159 SUPERSEDED — step 2 handles attrition-mandatory forfeit */
                                    && attritionRemaining > 0 && fv > 0 && !v145ImmuneToAttrition) {
                                // V67bd (Steve, 2026-05-09): Attrition can ONLY be paid by
                                // forfeiting — reserve force cannot cover it. Effective
                                // coverage = min(fv, attrition+damage).
                                int coverage = (int) Math.min(fv, totalRemaining);
                                float attritionBonus = 200.0f + (coverage * 80.0f);
                                if (fv >= totalRemaining) {
                                    attritionBonus += 200.0f;  // Single forfeit wipes ALL — huge win
                                    action.addReasoning("V67bd FORFEIT COVERS ALL: attrition+" +
                                        "damage in one shot — forfeit before reserve! (fv=" + (int)fv
                                        + ", total=" + totalRemaining + ")", attritionBonus);
                                    logger.warn("🎯 V67bd FORFEIT WIPES ALL: {} fv={} covers {}/{} total damage → +{}",
                                        title, (int)fv, (int)fv, totalRemaining, attritionBonus);
                                } else {
                                    action.addReasoning("V67bd FORFEIT FOR ATTRITION: fv=" + (int)fv
                                        + " covers " + coverage + " of " + totalRemaining
                                        + " owed (attrition+damage) — must forfeit before losing force!",
                                        attritionBonus);
                                    logger.warn("🎯 V67bd FORFEIT-FIRST: {} fv={} coverage={}/{} → +{}",
                                        title, (int)fv, coverage, totalRemaining, attritionBonus);
                                }
                            } else if (false /* V159 SUPERSEDED — step 4 handles immune */
                                    && v145ImmuneToAttrition && damageRemaining > 0 && fv > 0) {
                                // V145 path: immune to attrition, score damage coverage ONLY.
                                int savings = (int) Math.min(fv, damageRemaining);
                                int waste = (int) Math.max(0f, fv - damageRemaining);
                                float v145Bonus = savings * 20.0f - waste * 50.0f;
                                action.addReasoning(String.format(
                                    "V145 IMMUNE-TO-ATTRITION FORFEIT: '%s' can't satisfy attrition — only damage %d of %d",
                                    title, savings, damageRemaining), v145Bonus);
                                logger.warn("V145 IMMUNE-TO-ATTRITION: {} fv={} dmg={} savings={} waste={} → {}",
                                    title, (int)fv, damageRemaining, savings, waste, v145Bonus);
                            } else if (false /* V159 SUPERSEDED — step 3 handles pure damage */
                                    && damageRemaining > 0 && fv > 0) {
                                // V67t WASTE-AWARE: same formula as evaluateForfeit.
                                int savings = (int) Math.min(fv, damageRemaining);
                                int waste = (int) Math.max(0f, fv - damageRemaining);
                                float efficiencyBonus = savings * 20.0f - waste * 50.0f;
                                if (damageRemaining > 5 && waste == 0) efficiencyBonus += 50.0f;

                                // V67bh: SMALL-DAMAGE PROTECTION FOR VALUABLE UN-HIT CHARS.
                                // (See evaluateForfeit V67bh comment for full rationale.)
                                boolean v67bhSmallDmg = damageRemaining <= 3;
                                boolean v67bhValuable = fv >= 4;
                                boolean v67bhNotHit   = !card.isHit();
                                if (v67bhSmallDmg && v67bhValuable && v67bhNotHit) {
                                    efficiencyBonus -= 400.0f;
                                    action.addReasoning(String.format(
                                        "V67bh PROTECT VALUABLE: %s (fv=%d, not hit) — only %d damage, lose from reserve/hand!",
                                        title, (int)fv, damageRemaining), 0.0f);
                                    logger.warn("V67bh PROTECT VALUABLE: {} fv={} damage={} not-hit → -400",
                                        title, (int)fv, damageRemaining);
                                } else if (damageRemaining <= 2 && fv >= 2) {
                                    efficiencyBonus -= 250.0f;
                                    action.addReasoning("V67t SMALL DAMAGE: ≤2 damage — keep character, lose from reserve!", 0.0f);
                                    logger.warn("V67t SMALL DAMAGE: {} fv={} damage={} → -250 (prefer reserve loss)",
                                        title, (int)fv, damageRemaining);
                                }

                                action.addReasoning("V67t FORFEIT (savings=" + savings + " waste=" + waste
                                    + " of " + damageRemaining + " damage)", efficiencyBonus);
                                logger.info("V67t FORFEIT: {} fv={} damage={} → savings={} waste={} bonus={}",
                                    title, (int)fv, damageRemaining, savings, waste, efficiencyBonus);
                            }

                            // Apply standard forfeit scoring
                            SwccgCardBlueprint blueprint = card.getBlueprint();
                            if (false /* V159 SUPERSEDED — step 3 (damage>=3) makes V139 yield; step 2 has release valve */
                                    && blueprint != null) {
                                // V139 (Steve, 2026-05-26): HIGH-VALUE CHARACTER PROTECTION
                                // (mirror of V139 in evaluateForfeit path).
                                // Replay etoxkyignk4dfmmj T2: Sidious forfeited for 1 battle
                                // damage because V67bd +960 dominated the old -10 unique
                                // protection. New magnitudes ensure Sidious-tier characters
                                // are essentially never forfeited unless they're the only
                                // option at the site.
                                // V139 v3 (Steve 2026-05-27): DAMAGE-AWARE PROTECTION SCALE
                                //
                                // v2 protections (-500 to -1200) were too aggressive at high
                                // damage. Steve: "in the last game two times, Rando lost a ton
                                // of force during a battle when he could have forfeited his
                                // characters to cover. It's only when the damage is 2 or less."
                                //
                                // New behavior: heavy V139 protections fire ONLY when damage
                                // (or total attrition+damage) is LOW (≤ 3). For higher damage,
                                // milder protections allow forfeit-efficiency (V67bd) to win
                                // when forfeit is genuinely more efficient than burning 5+
                                // reserve cards.
                                int v139DamageBurden = attritionRemaining + damageRemaining;
                                boolean v139LowDamage = v139DamageBurden <= 3;
                                Float charPower = blueprint.hasPowerAttribute() ? blueprint.getPower() : null;
                                Float charAbility = blueprint.hasAbilityAttribute() ? blueprint.getAbility() : null;
                                if (!card.isHit()) {
                                    float protectScale = v139LowDamage ? 1.0f : 0.25f;
                                    if (charPower != null && charPower >= 6 && charAbility != null && charAbility >= 4) {
                                        action.addReasoning(String.format(
                                            "V139 PROTECT (FLoF): %s (power %.0f, ability %.0f) — %s",
                                            title, charPower, charAbility,
                                            v139LowDamage ? "low damage, never forfeit unless only option" : "mild protection (damage > 3, efficiency may win)"),
                                            -1200.0f * protectScale);
                                    } else if (charPower != null && charPower >= 5) {
                                        action.addReasoning(String.format(
                                            "V139 PROTECT (FLoF): %s (power %.0f) — %s",
                                            title, charPower,
                                            v139LowDamage ? "high-power, save for battle" : "mild high-power protection"),
                                            -500.0f * protectScale);
                                    } else if (charAbility != null && charAbility >= 4) {
                                        action.addReasoning(String.format(
                                            "V139 PROTECT (FLoF): %s (ability %.0f) — %s",
                                            title, charAbility,
                                            v139LowDamage ? "destiny draw value, save" : "mild ability protection"),
                                            -500.0f * protectScale);
                                    }
                                }

                                // CRITICAL: Check hasForfeitAttribute() first - weapons throw exception!
                                // V146: All V139 penalties skipped when card.isHit() — hit
                                // characters get NO protection. They've already been broken
                                // by a weapon hit and must forfeit first.
                                Float forfeit = blueprint.hasForfeitAttribute() ? blueprint.getForfeit() : null;
                                if (forfeit != null && !card.isHit()) {
                                    if (forfeit <= 2) {
                                        action.addReasoning("V139 Low forfeit - cheap loss, forfeit first", 50.0f);
                                    } else if (forfeit >= 6) {
                                        // High forfeit penalty scales by damage burden too
                                        float fhVal = v139LowDamage ? -200.0f : -50.0f;
                                        action.addReasoning("V139 High forfeit character - prefer keeping", fhVal);
                                    }
                                }

                                if (blueprint.getUniqueness() == Uniqueness.UNIQUE && !card.isHit()) {
                                    if ((charAbility != null && charAbility >= 5)
                                            || (charPower != null && charPower >= 5)) {
                                        float vuVal = v139LowDamage ? -800.0f : -200.0f;
                                        action.addReasoning("V139 VALUABLE UNIQUE (FLoF) - " +
                                            (v139LowDamage ? "never forfeit unless forced" : "mild protection at high damage"),
                                            vuVal);
                                    } else {
                                        float guVal = v139LowDamage ? -250.0f : -50.0f;
                                        action.addReasoning("V139 Unique (FLoF) - avoid forfeiting", guVal);
                                    }
                                }

                            // V21: OBJECTIVE-CRITICAL CARD PROTECTION
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (objAnalyzer != null && objAnalyzer.isAnalyzed() && title != null) {
                                if (objAnalyzer.isRequiredCardForFlip(title)) {
                                    action.addReasoning("OBJECTIVE CRITICAL - NEVER LOSE!", -9999.0f);
                                    logger.warn("V21 HARD BAN: {} is REQUIRED for flip!", title);
                                } else if (objAnalyzer.isPullableCard(title)) {
                                    action.addReasoning("OBJECTIVE PULLABLE - NEVER LOSE!", -9999.0f);
                                    logger.warn("V21 HARD BAN: {} is objective pullable!", title);
                                }
                            }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Extract a number following a pattern in text.
     * E.g., "attrition remaining: 5" -> returns 5
     */
    private int extractNumberAfter(String text, String pattern) {
        int idx = text.indexOf(pattern);
        if (idx >= 0) {
            String afterPattern = text.substring(idx + pattern.length()).trim();
            // Extract first number
            StringBuilder num = new StringBuilder();
            for (char c : afterPattern.toCharArray()) {
                if (Character.isDigit(c)) {
                    num.append(c);
                } else if (num.length() > 0) {
                    break;
                }
            }
            if (num.length() > 0) {
                try {
                    return Integer.parseInt(num.toString());
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        return 0;
    }

    /**
     * Choose a pilot - pick best pilot by ability.
     * Enhanced with deploy cost consideration and matching pilot detection.
     * Ported from Python deploy_evaluator.py _evaluate_simultaneous_pilot_selection
     */
    private List<EvaluatedAction> evaluatePilotSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        // Detect if this pilot selection is for Alert My Star Destroyer (AMSD).
        // AMSD deploys a Star Destroyer — only Imperial/First Order pilots belong
        // on capital ships. Non-Imperial pilots like Jango Fett have matching ships
        // that are NOT Star Destroyers (Slave I, etc.), so they will always fail the
        // reserve deck search and waste the action entirely.
        String decisionText = context.getDecisionText() != null
            ? context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
        // V22.7: Broadened AMSD detection. GEMP may present pilot selection with text
        // like "Choose a unique pilot character" without mentioning "star destroyer".
        // If we're in Deploy phase choosing a unique pilot, it's likely AMSD.
        // V24.12: Also detect AMSD by checking if the card is actually on the table,
        // because the decision text for "Choose card from hand" doesn't mention AMSD at all.
        boolean isAmsdPilotChoice = decisionText.contains("alert my star destroyer")
            || decisionText.contains("star destroyer")
            || decisionText.contains("matching starship")
            || decisionText.contains("matching star destroyer")
            || (context.getPhase() == Phase.DEPLOY && decisionText.contains("unique")
                && decisionText.contains("pilot"));

        // V24.12: AMSD-on-table detection — if AMSD is deployed and we're choosing
        // characters during deploy phase, this IS an AMSD pilot pick even if the
        // decision text is generic ("Choose card from hand, or click 'Done' to cancel").
        if (!isAmsdPilotChoice && context.getPhase() == Phase.DEPLOY) {
            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle pilotOracle = context.getDeckOracle();
            if (pilotOracle != null && pilotOracle.isAnalyzed()) {
                boolean amsdDeployed = pilotOracle.isCardInPlay("Alert My Star Destroyer")
                    || pilotOracle.isCardInPlay("Alert My Star Destroyer!")
                    || pilotOracle.isCardInPlay("Alert My Star Destroyer! (V)");
                if (amsdDeployed) {
                    // Verify at least one choice is a character (not a location/effect)
                    GameState pilotGs = context.getGameState();
                    if (pilotGs != null && context.getCardIds() != null) {
                        for (String cid : context.getCardIds()) {
                            try {
                                PhysicalCard pc = pilotGs.findCardById(Integer.parseInt(cid));
                                if (pc != null && pc.getBlueprint() != null &&
                                    pc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                    isAmsdPilotChoice = true;
                                    logger.warn("V24.12 AMSD DETECTED: AMSD on table + deploy phase + character choices — forcing AMSD pilot mode!");
                                    break;
                                }
                            } catch (Exception e) { /* skip */ }
                        }
                    }
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Select pilot " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        if (blueprint != null) {
                            action.setDisplayText("Select pilot " + (title != null ? title : cardId));

                            // === V24.10: AMSD PILOT GUARD — PIETT ONLY ===
                            // AMSD should ONLY be used with Piett + Executor.
                            // Block ALL other pilots regardless of matching ships.
                            if (isAmsdPilotChoice) {
                                String pilotLower = (title != null) ? title.toLowerCase(java.util.Locale.ROOT) : "";

                                if (!pilotLower.contains("piett")) {
                                    // NOT Piett — hard block, no exceptions
                                    action.setScore(-9999.0f);
                                    action.addReasoning("V24.10 AMSD BLOCKED: Only Piett may use AMSD — " +
                                        title + " is not allowed!", -9999.0f);
                                    logger.warn("V24.10 AMSD HARD BLOCK: {} is NOT Piett — only Piett + Executor for AMSD!", title);
                                    actions.add(action);
                                    continue;
                                }

                                // It's Piett — verify Executor is in reserve
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle = context.getDeckOracle();
                                if (oracle != null && oracle.isAnalyzed()) {
                                    boolean executorInReserve = oracle.isCardInReserve("Executor") ||
                                        oracle.isCardInReserve("Flagship Executor");
                                    if (!executorInReserve) {
                                        action.setScore(-9999.0f);
                                        action.addReasoning("V24.10 AMSD: Piett selected but Executor NOT in reserve!", -9999.0f);
                                        logger.warn("V24.10 AMSD: Piett but Executor not in reserve — HARD BLOCK");
                                        actions.add(action);
                                        continue;
                                    }
                                    // Piett + Executor in reserve — approved!
                                    action.addReasoning("V24.10 AMSD: Piett + Executor in reserve — APPROVED!", 300.0f);
                                    logger.warn("V24.10 AMSD: Piett + Executor in reserve — APPROVED (+300)");
                                } else {
                                    // Oracle unavailable but it's Piett — allow (best guess)
                                    action.addReasoning("V24.10 AMSD: Piett selected (oracle unavailable — allowing)", 200.0f);
                                    logger.warn("V24.10 AMSD: Piett selected, oracle unavailable — allowing (+200)");
                                }
                            }

                            // Prefer high-ability pilots
                            if (blueprint.hasAbilityAttribute()) {
                                Float ability = blueprint.getAbility();
                                if (ability != null) {
                                    float abilityScore = ability * 10.0f;
                                    action.addReasoning("Ability " + ability.intValue(), abilityScore);
                                }
                            }

                            // Prefer pilots that add power
                            if (blueprint.hasPowerAttribute()) {
                                Float power = blueprint.getPower();
                                if (power != null && power >= 3) {
                                    action.addReasoning("Good power bonus (" + power.intValue() + ")", 20.0f);
                                }
                            }

                            // Lower deploy cost is better
                            // V43: Wrap in try-catch — some cards (Interrupts, Effects like
                            // "Hidden Weapons") don't support getDeployCost() and throw
                            // UnsupportedOperationException, crashing the cleanup thread.
                            try {
                                Float deployCost = blueprint.getDeployCost();
                                if (deployCost != null) {
                                    float costScore = Math.max(0, 30 - deployCost * 5);
                                    action.addReasoning("Deploy cost " + deployCost.intValue(), costScore);
                                }
                            } catch (UnsupportedOperationException e) {
                                // Card type doesn't support deployCost — skip
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Simultaneous pilot selection - when deploying a ship and choosing which pilot to put aboard.
     * The card_ids are pilot cards in hand, NOT locations.
     * Ported from Python deploy_evaluator.py _evaluate_simultaneous_pilot_selection lines 1193-1273
     */
    private List<EvaluatedAction> evaluateSimultaneousPilotSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String decisionText = context.getDecisionText();

        // Extract the ship name from decision text
        // Format: "Choose a pilot from hand to simultaneously deploy aboard •Ship Name"
        String shipName = extractShipNameFromText(decisionText);
        logger.info("🚀 Simultaneous pilot selection for {}", shipName != null ? shipName : "unknown ship");

        // Detect if the ship being piloted is a Star Destroyer (capital ship).
        // Only Imperial/First Order characters should pilot Star Destroyers.
        boolean isStarDestroyerDeploy = decisionText != null &&
            decisionText.toLowerCase(java.util.Locale.ROOT).contains("star destroyer");

        // Check deploy plan for a planned pilot for this ship
        String plannedPilotBlueprintId = null;
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        if (planner != null) {
            DeploymentPlan plan = planner.getCurrentPlan();
            if (plan != null) {
                for (DeploymentInstruction instruction : plan.getInstructions()) {
                    // Check if this instruction is for a pilot boarding a ship
                    String aboardShipName = instruction.getAboardShipName();
                    if (aboardShipName != null && shipName != null &&
                        aboardShipName.toLowerCase().contains(shipName.toLowerCase())) {
                        plannedPilotBlueprintId = instruction.getCardBlueprintId();
                        logger.info("   📋 Plan says pilot: {} (blueprint={})",
                            instruction.getCardName(), plannedPilotBlueprintId);
                        break;
                    }
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Deploy pilot (card " + cardId + ")"
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        String blueprintId = card.getBlueprintId(true);

                        if (blueprint != null) {
                            action.setDisplayText("Deploy pilot " + (title != null ? title : cardId));

                            // STAR DESTROYER GUARD: Only Imperial/First Order pilots belong
                            // on Star Destroyers. Block others to prevent failed searches.
                            if (isStarDestroyerDeploy) {
                                boolean isImperial = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.IMPERIAL);
                                boolean isFirstOrder = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.FIRST_ORDER);
                                if (!isImperial && !isFirstOrder) {
                                    action.setScore(-500.0f);
                                    action.addReasoning("SD BLOCKED: non-Imperial/FO can't pilot Star Destroyers!", -500.0f);
                                    logger.warn("🚫 SD GUARD: Blocking {} for Star Destroyer — not Imperial or FO", title);
                                    actions.add(action);
                                    continue;
                                } else {
                                    action.addReasoning("SD: Imperial/First Order pilot — valid!", 100.0f);
                                }
                            }

                            // Check if this is the planned pilot
                            if (plannedPilotBlueprintId != null && blueprintId != null &&
                                blueprintId.equals(plannedPilotBlueprintId)) {
                                action.addReasoning("PLANNED pilot for " + shipName, 200.0f);
                                logger.info("   ✅ {} is the PLANNED pilot (+200)", title);
                            } else {
                                // Score based on pilot quality

                                // Lower deploy cost is better (we're paying extra for this)
                                // V43: try-catch for cards that don't support getDeployCost()
                                try {
                                    Float deployCost = blueprint.getDeployCost();
                                    if (deployCost != null) {
                                        float costScore = Math.max(0, 30 - deployCost * 5);
                                        action.addReasoning("Deploy cost " + deployCost.intValue(), costScore);
                                    }
                                } catch (UnsupportedOperationException e) {
                                    // Card type doesn't support deployCost — skip
                                }

                                // Higher ability is better for piloting
                                if (blueprint.hasAbilityAttribute()) {
                                    Float ability = blueprint.getAbility();
                                    if (ability != null) {
                                        float abilityScore = ability * 10.0f;
                                        action.addReasoning("Ability " + ability.intValue(), abilityScore);
                                    }
                                }

                                // Check for matching pilot (pilot name contains ship name)
                                if (title != null && shipName != null) {
                                    String titleLower = title.toLowerCase();
                                    String shipNameLower = shipName.toLowerCase().replace("•", "").trim();
                                    if (titleLower.contains(shipNameLower) ||
                                        shipNameLower.contains(titleLower.replace(" ", ""))) {
                                        action.addReasoning("Matching pilot for " + shipName + "!", 50.0f);
                                        logger.info("   🎯 {} appears to be matching pilot for {}", title, shipName);
                                    }
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse cardId: {}", cardId);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Extract ship name from simultaneous deploy decision text.
     * Format: "...simultaneously deploy aboard •Ship Name" or "...aboard Ship Name"
     */
    private String extractShipNameFromText(String text) {
        if (text == null) return null;

        // Look for "aboard" followed by ship name
        int aboardIdx = text.toLowerCase().indexOf("aboard");
        if (aboardIdx >= 0) {
            String afterAboard = text.substring(aboardIdx + 6).trim();
            // Remove HTML tags if present
            afterAboard = afterAboard.replaceAll("<[^>]+>", " ").trim();
            // Take the first few words (ship names are usually 2-4 words)
            String[] words = afterAboard.split("\\s+");
            StringBuilder shipName = new StringBuilder();
            for (int i = 0; i < Math.min(words.length, 5); i++) {
                if (words[i].isEmpty()) continue;
                if (shipName.length() > 0) shipName.append(" ");
                shipName.append(words[i]);
            }
            return shipName.toString().trim();
        }
        return null;
    }

    /**
     * Evaluate move destination selection.
     * Ported from Python card_selection_evaluator.py _evaluate_move_destination
     *
     * Prefer:
     * - Locations with opponent icons (force drain potential!)
     * - Locations where we have power advantage
     * - Locations with our icons (force generation)
     * Avoid:
     * - Locations where enemy is much stronger
     * - Locations with fewer total icons than alternatives
     */
    /**
     * V166 (Steve, 2026-06): bonus-aware net force-drain balance = opponent's total drain
     * minus ours, across all locations where each side has presence. Uses getForceDrainAmount
     * so weapon/lightsaber/objective/Effect drain bonuses are counted (raw icon counts miss
     * them — the gap in the old icon-based calculateForceDrainGap). Positive = opponent is
     * out-draining us.
     */
    /**
     * V169 (Steve, 2026-06): how badly are OUR characters outpowered at this location?
     * Returns (opponent power - our power) when we have non-undercover presence there and
     * the opponent out-powers us; else 0. >0 = our characters are ENDANGERED — the opponent
     * can battle and beat them next turn (Asajj at Guest Quarters, Tyranus on Hoth).
     * Undercover spies don't count as presence to protect (they're safe undercover).
     */
    private static float v169OppPowerExcessAt(SwccgGame game, GameState gs, String playerId, PhysicalCard location) {
        try {
            String opp = gs.getOpponent(playerId);
            if (opp == null) return 0f;
            boolean weHere = false;
            for (PhysicalCard c : gs.getCardsAtLocation(location)) {
                if (c != null && playerId.equals(c.getOwner()) && !c.isUndercover()) { weHere = true; break; }
            }
            if (!weHere) return 0f;
            float our = game.getModifiersQuerying().getTotalPowerAtLocation(gs, location, playerId, false, false);
            float their = game.getModifiersQuerying().getTotalPowerAtLocation(gs, location, opp, false, false);
            return their > our ? (their - our) : 0f;
        } catch (Exception e) { return 0f; }
    }

    /**
     * V173 (Steve, 2026-06): whole-hand wave projection for the V172 winnability gates.
     * "Does the new logic account for the whole hand with power and weapons weights vs
     * force cost?" — now it does. Projects the ADDITIONAL power (beyond the card being
     * deployed) the bot could land this phase: every other hand character, taken in
     * descending power order, that fits the remaining force budget (force pile minus the
     * deploying card's printed cost, minus each buddy's printed cost as it's taken), plus
     * weapon weights for affordable character weapons in hand (lightsaber +5, other +3 —
     * the BattleEvaluator V29.7 precedent — max 2 counted, ~1 force each).
     * Estimates use PRINTED costs/power (no location cost modifiers) — a deliberate
     * approximation; the gates compare against live getTotalPowerAtLocation.
     */
    /**
     * V174 (Steve, 2026-06): the budget now RESERVES force before filling the wave.
     * Steve: "We need to account for saving force for maintenance cards on table / in
     * hand to deploy with the army and any interrupts that would be useful in battle."
     * Reserved off the top: (a) upkeep for our MAINTENANCE-icon cards already on table,
     * (b) the deploying card's own upkeep if IT is a maintenance card, (c) 1-2 force for
     * battle interrupts in hand (Steve's standing force-management rule). Maintenance
     * BUDDIES joining the wave consume deploy cost + upkeep from the budget.
     * T2 COMMIT-1 (2026-07-06, audit force-economy-1/-5, ruling H2): upkeep basis is now
     * the ENGINE's card-specific maintain cost (MaintenanceFacts, e.g. Lando 1 not 5) —
     * the old "maintenance cost = deploy cost, the V22.3/V59 rule" claim was refuted and
     * the buddy "double" (2x deploy cost) spend over-charged the wave; the table scan is
     * also Zone.isInPlay()-gated (getAllPermanentCards returns reserve-deck cards too).
     * Returns {wavePower, buddiesTaken, reservedForce}.
     */
    /** V177 helper (2026-07-10): estimate the opponent's WEAPON power at a location — the raw power
     *  totals are blind to weapons/hits, which is how Rando kept walking Jedi into armed stacks
     *  (Rey replay rbujmoc90br3uu4c). Same heuristic as BattleEvaluator V29.7: lightsaber +5,
     *  other weapon +3; counts ATTACHED WEAPON cards and PERMANENT weapons (game text). */
    private static float v177OppWeaponBonus(GameState gs, PhysicalCard location, String oppId) {
        float bonus = 0f;
        if (gs == null || location == null || oppId == null) return 0f;
        try {
            for (PhysicalCard c : gs.getCardsAtLocation(location)) {
                if (c == null || c.getBlueprint() == null) continue;
                if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                if (!oppId.equals(c.getOwner())) continue;
                java.util.List<PhysicalCard> atts = gs.getAttachedCards(c);
                if (atts != null) {
                    for (PhysicalCard att : atts) {
                        if (att == null || att.getBlueprint() == null) continue;
                        if (att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                            String wt = att.getTitle() != null ? att.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            bonus += wt.contains("lightsaber") ? 5.0f : 3.0f;
                        }
                    }
                }
                String gt = c.getBlueprint().getGameText();
                if (gt != null && gt.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                    String ct = c.getTitle() != null ? c.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    bonus += ct.contains("lightsaber") ? 5.0f : 3.0f;
                }
            }
        } catch (Exception e) { /* fail-open: 0 bonus */ }
        return bonus;
    }

    private static float[] v173WaveProjection(GameState gs, String playerId, String deployingBpId) {
        try {
            float thisCost = 0f;
            // T2 COMMIT-1 (2026-07-06): track the deploying card's ENGINE maintain cost
            // (was: boolean thisIsMaint reserving its full deploy cost).
            float thisMaintCost = 0f;
            boolean skippedSelf = false;
            java.util.List<float[]> buddies = new java.util.ArrayList<>(); // {power, cost, maintainCost}
            int sabers = 0, otherWeapons = 0, interrupts = 0;
            for (PhysicalCard h : gs.getHand(playerId)) {
                if (h == null || h.getBlueprint() == null) continue;
                SwccgCardBlueprint bp = h.getBlueprint();
                if (bp.getCardCategory() == CardCategory.CHARACTER) {
                    Float p = bp.hasPowerAttribute() ? bp.getPower() : null;
                    Float c = bp.getDeployCost();
                    float pv = p != null ? p : 0f, cv = c != null ? c : 0f;
                    // T2 COMMIT-1 (2026-07-06): engine maintain cost, not deploy cost
                    float maintCost = com.gempukku.swccgo.ai.models.common.strategy
                        .MaintenanceFacts.maintainCost(bp);
                    if (!skippedSelf && deployingBpId != null
                            && deployingBpId.equals(h.getBlueprintId(true))) {
                        thisCost = cv;
                        // thisIsMaint = maint;  // superseded T2 COMMIT-1 2026-07-06 (deploy-cost basis)
                        thisMaintCost = maintCost;
                        skippedSelf = true; // the deploying card is not its own buddy
                    } else {
                        // buddies.add(new float[]{pv, cv, maint ? 1f : 0f});  // superseded T2 COMMIT-1 2026-07-06 (flag → cost)
                        buddies.add(new float[]{pv, cv, maintCost});
                    }
                } else if (bp.getCardCategory() == CardCategory.WEAPON) {
                    String wt = h.getTitle() != null
                        ? h.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    if (wt.contains("lightsaber")) sabers++; else otherWeapons++;
                } else if (bp.getCardCategory() == CardCategory.INTERRUPT) {
                    interrupts++;
                }
            }
            // Upkeep for maintenance cards ALREADY on table (they die without it).
            float tableMaint = 0f;
            for (PhysicalCard t : gs.getAllPermanentCards()) {
                if (t == null) continue;
                // T2 COMMIT-1 (2026-07-06, audit force-economy-5): in-play gate —
                // getAllPermanentCards returns reserve-deck cards too; without this a
                // maintenance card still in Reserve Deck phantom-taxed the wave budget.
                com.gempukku.swccgo.common.Zone tZone = t.getZone();
                if (tZone == null || !tZone.isInPlay()) continue;
                if (playerId.equals(t.getOwner()) && t.getBlueprint() != null
                        && t.getBlueprint().hasIcon(Icon.MAINTENANCE)) {
                    // Float mc = t.getBlueprint().getDeployCost();  // superseded T2 COMMIT-1 2026-07-06 (deploy-cost basis)
                    // tableMaint += mc != null ? mc : 0f;  // superseded T2 COMMIT-1 2026-07-06
                    tableMaint += com.gempukku.swccgo.ai.models.common.strategy
                        .MaintenanceFacts.maintainCost(t.getBlueprint());
                }
            }
            float interruptReserve = interrupts >= 2 ? 2f : (interrupts >= 1 ? 1f : 0f);
            // V176 (Steve, 2026-06): +1 battle-initiation fee. Turn-5 vs Steve: the wave
            // deployed onto solo Yoda and then could NOT battle him — the deploys spent
            // the last force and battle initiation costs 1. A wave that exists to fight
            // must keep the fee to start the fight.
            // float reserved = tableMaint + interruptReserve + (thisIsMaint ? thisCost : 0f) + 1f;  // superseded T2 COMMIT-1 2026-07-06 (deploy-cost basis)
            float reserved = tableMaint + interruptReserve + thisMaintCost + 1f;
            // V177 (Steve, 2026-06): RESERVE CAP — never let upkeep reserves starve the wave
            // to zero. Replay aab2jiaa5sca (Luke vs Kylo): force=10 but reserved=12 (full table
            // maintenance + interrupt + fee), so budget=0, wave=0, buddies=0 — Young Skywalker
            // projected solo (6) vs Kylo (10), V172 gated the contest, and Rando left Luke +
            // Bionic Hand + 3PO + saber in hand instead of overpowering Kylo. Maintenance only
            // bites at end of turn and is already handled at deploy-score time (V59/V64); double-
            // counting it here is what locked out the winnable attack. Cap the reserve so the
            // projection always keeps >=3 force of budget to assemble the strike group when force
            // allows — full reserves still hold when there is genuine surplus.
            float v177ForcePile = gs.getForcePileSize(playerId);
            reserved = Math.min(reserved, Math.max(0f, v177ForcePile - thisCost - 3f));
            float budget = Math.max(0f, v177ForcePile - thisCost - reserved);
            buddies.sort((a, b) -> Float.compare(b[0], a[0])); // strongest first
            float addPower = 0f;
            int taken = 0;
            for (float[] ch : buddies) {
                // a maintenance buddy must bring its own upkeep: deploy cost + upkeep
                // T2 COMMIT-1 (2026-07-06): upkeep = engine maintain cost (ch[2]), fixing
                // the double-spend that charged 2x deploy cost for maintenance buddies.
                // float spend = ch[2] > 0f ? ch[1] * 2f : ch[1];  // superseded T2 COMMIT-1 2026-07-06 (2x deploy-cost double-spend)
                float spend = ch[1] + ch[2];
                if (spend <= budget) { addPower += ch[0]; budget -= spend; taken++; }
                // unaffordable big hitter: skip and try the next (cheaper) character
            }
            int weaponsCounted = 0;
            while (weaponsCounted < 2 && budget >= 1f && (sabers > 0 || otherWeapons > 0)) {
                if (sabers > 0) { addPower += 5f; sabers--; }
                else { addPower += 3f; otherWeapons--; }
                budget -= 1f; weaponsCounted++;
            }
            return new float[]{addPower, taken, reserved};
        } catch (Exception e) { return new float[]{0f, 0f, 0f}; }
    }

    private static int computeNetDrainBalance(SwccgGame game, GameState gs, String playerId) {
        String oppId = gs.getOpponent(playerId);
        if (oppId == null) return 0;
        int oppTotal = 0, ourTotal = 0;
        for (PhysicalCard loc : gs.getLocationsInOrder()) {
            if (loc == null) continue;
            boolean weHere = false, oppHere = false;
            for (PhysicalCard c : gs.getCardsAtLocation(loc)) {
                if (c == null) continue;
                if (playerId.equals(c.getOwner())) weHere = true;
                else if (oppId.equals(c.getOwner())) oppHere = true;
            }
            try {
                if (oppHere) oppTotal += (int) game.getModifiersQuerying().getForceDrainAmount(gs, loc, oppId);
                if (weHere) ourTotal += (int) game.getModifiersQuerying().getForceDrainAmount(gs, loc, playerId);
            } catch (Exception ignore) { /* skip this location */ }
        }
        return oppTotal - ourTotal;
    }

    private List<EvaluatedAction> evaluateMoveDestination(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        Side mySide = context.getSide();

        // Icon bonus constant (same as MoveEvaluator/Python)
        final float ICON_BONUS = 15.0f;

        // === V169 (Steve, 2026-06): RETREAT MODE — is the card being moved ENDANGERED? ===
        // Replay lk6xgsokjcwrwxuu fatal move 1: Asajj at Guest Quarters with Luke AT her site
        // could not retreat — every safe (empty) destination was V41-wrong-direction blocked
        // (-9999), the destination step cancelled out, the cancel-loop guard then hard-vetoed
        // the move action, and she was beaten 6v27 next turn. Steve: "Rando should have ...
        // moved Asajj to an adjacent safe site that I did not occupy."
        // Find the moving card from the decision-text blueprint hint; if its CURRENT location
        // is endangered (opponent out-powers us there), this whole decision is a RETREAT:
        // safe destinations get a big bonus and V41 is gated off (a retreat IS a move to an
        // empty site — V41's assumption is wrong for endangered movers).
        boolean v169RetreatMode = false;
        String v169FromTitle = null;
        if (game != null && gameState != null && playerId != null) {
            try {
                String v169MoverBp = extractBlueprintFromDecisionText(context.getDecisionText());
                if (v169MoverBp != null) {
                    for (PhysicalCard v169Pc : gameState.getAllPermanentCards()) {
                        if (v169Pc == null || !playerId.equals(v169Pc.getOwner())) continue;
                        if (!v169MoverBp.equals(v169Pc.getBlueprintId(true))) continue;
                        PhysicalCard v169Loc = v169Pc.getAtLocation();
                        if (v169Loc == null) continue;
                        if (v169OppPowerExcessAt(game, gameState, playerId, v169Loc) > 0) {
                            v169RetreatMode = true;
                            v169FromTitle = v169Loc.getTitle();
                            logger.warn("V169 RETREAT MODE: mover '{}' is endangered at {} — safe destinations boosted, V41 gated",
                                v169Pc.getTitle(), v169FromTitle);
                        }
                        break;
                    }
                }
            } catch (Exception e) { logger.debug("V169 retreat-mode error: {}", e.getMessage()); }
        }

        // === V156 JOIN-GROUP MODE (2026-07-07, destination arm; Fel-at-Beach loss, audit deploy-siting-2) ===
        // Twin of MoveEvaluator's V156 JOIN-GROUP R2 claim (same date). When the mover is a
        // weak (ability<4) SOLO character at an uncontested site, this destination decision
        // is a JOIN: friendly-stack destinations get a bonus below (largest stack preferred)
        // and V41 WRONG DIRECTION is gated off for them — a consolidate/join-allies move
        // toward OUR OWN stack is by definition not "wrong direction" (V41's 'empty' only
        // counts opponents; that -9999 is what stranded Fel at Scarif: Beach: the ladder's
        // R2 claim moved, the only join destination scored -10151, V160 broke the cancel
        // loop, and Fel rotted in place until battled and forfeited). Mirrors the V169
        // retreat-mode and V67z exemption pattern. Exempt: undercover spies (V170 parked
        // spies sit) and a solo doing READY objective work at a flip-relevant site (shared
        // CharacterDeploySiteEvaluator.isV156FlipNotReady predicate — same carve the deploy
        // side uses). Mutually exclusive with V169 retreat mode (that needs opponent excess
        // AT the mover's site; join mode needs opponent power 0 there).
        boolean v156JoinMode = false;
        String v156FromTitle = null;
        float v156MoverAbility = 0f;  // STACK-MATH (2026-07-07): mover's ability, for the defensible-join kicker below
        if (game != null && gameState != null && playerId != null) {
            try {
                String v156MoverBp = extractBlueprintFromDecisionText(context.getDecisionText());
                if (v156MoverBp != null) {
                    for (PhysicalCard v156Pc : gameState.getAllPermanentCards()) {
                        if (v156Pc == null || !playerId.equals(v156Pc.getOwner())) continue;
                        if (!v156MoverBp.equals(v156Pc.getBlueprintId(true))) continue;
                        if (v156Pc.getBlueprint() == null
                            || v156Pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) break;
                        if (v156Pc.isUndercover()) break;  // V170 parked spies sit
                        Float v156Ab = v156Pc.getBlueprint().hasAbilityAttribute()
                            ? v156Pc.getBlueprint().getAbility() : null;
                        if (v156Ab == null || v156Ab >= 4f) break;  // weak band only
                        PhysicalCard v156Loc = v156Pc.getAtLocation();
                        if (v156Loc == null) break;
                        boolean v156Alone = true;
                        for (PhysicalCard c : gameState.getCardsAtLocation(v156Loc)) {
                            if (c == null || c == v156Pc || !playerId.equals(c.getOwner())) continue;
                            if (c.getBlueprint() == null) continue;
                            if (c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                v156Alone = false;
                                break;
                            }
                        }
                        float v156OppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, v156Loc, gameState.getOpponent(playerId), false, false);
                        boolean v156AtReadyFlipSite = false;
                        try {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v156Oa =
                                context.getObjectiveAnalyzer();
                            v156AtReadyFlipSite = v156Oa != null && v156Oa.isAnalyzed()
                                && v156Loc.getTitle() != null
                                && v156Oa.isObjectiveRelevantLocation(v156Loc.getTitle())
                                && !com.gempukku.swccgo.ai.models.common.strategy.CharacterDeploySiteEvaluator
                                    .isV156FlipNotReady(gameState, playerId);
                        } catch (Exception ignore) { /* false */ }
                        if (v156Alone && v156OppPowerHere == 0f && !v156AtReadyFlipSite) {
                            v156JoinMode = true;
                            v156FromTitle = v156Loc.getTitle();
                            v156MoverAbility = v156Ab;
                            logger.warn("V156 JOIN-GROUP MODE: mover '{}' (ability {}) is a weak solo at {} — friendly-stack destinations boosted, V41 gated",
                                v156Pc.getTitle(), (int) v156Ab.floatValue(), v156FromTitle);
                        }
                        break;
                    }
                }
            } catch (Exception e) { logger.debug("V156 join-mode error: {}", e.getMessage()); }
        }

        // FORMATION SAFETY (2026-07-11c): resolve the ACTUAL mover once for L1/L4 vetoes below.
        // Same extraction the V169/V156 modes use; null mover => partial info => no vetoes (council
        // rule: never veto blind).
        PhysicalCard fsMover = null;
        PhysicalCard fsOrigin = null;
        if (game != null && gameState != null && playerId != null) {
            try {
                String fsMoverBp = extractBlueprintFromDecisionText(context.getDecisionText());
                if (fsMoverBp != null) {
                    for (PhysicalCard fsPc : gameState.getAllPermanentCards()) {
                        if (fsPc == null || !playerId.equals(fsPc.getOwner())) continue;
                        if (!fsMoverBp.equals(fsPc.getBlueprintId(true))) continue;
                        fsMover = fsPc;
                        fsOrigin = fsPc.getAtLocation();
                        break;
                    }
                }
            } catch (Exception e) { /* partial info — no veto */ }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.MOVE,
                0.0f,  // Start at 0 for move decisions
                "Move to location " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard location = gameState.findCardById(Integer.parseInt(cardId));
                    if (location != null) {
                        String title = location.getTitle();
                        action.setDisplayText("Move to " + (title != null ? title : "location"));

                        // FORMATION SAFETY (2026-07-11c): L4 solo-charge + L1 abandon-solo vetoes —
                        // un-outvotable (Codex audit: V32 -300 + V22.2 -120 lost to R2 +6000 here).
                        if (fsMover != null && game != null) {
                            String fsV = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                .vetoMoveDestination(game, gameState, playerId, fsMover, location);
                            if (fsV == null && fsOrigin != null
                                    && fsOrigin.getCardId() != location.getCardId()) {
                                fsV = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                    .vetoMoveOrigin(game, gameState, playerId, fsMover, fsOrigin);
                            }
                            if (fsV != null) {
                                action.hardVeto(fsV);
                                logger.warn("FORMATION SAFETY (move-dest): {}", fsV);
                            } else {
                                // BATCH1b (2026-07-12, Codex m00199/m00209 — Chiraneau empty-site split):
                                // a WEAK (ability<4) mover relocating SOLO to an uncontested empty site
                                // while leaving a lone weak buddy behind creates TWO weak solos — L1
                                // requires enemy at origin and L4 exits on empty destinations, so both
                                // guards miss it. Heavy penalty (-800: 327.5 -> -472.5 loses to Pass),
                                // NOT a veto — genuine repositioning must stay possible.
                                try {
                                    SwccgCardBlueprint fsMbp = fsMover.getBlueprint();
                                    Float fsMa = (fsMbp != null && fsMbp.hasAbilityAttribute()) ? fsMbp.getAbility() : null;
                                    if (fsMa != null && fsMa < 4f && fsOrigin != null
                                            && fsOrigin.getCardId() != location.getCardId()) {
                                        String fsOpp = gameState.getOpponent(playerId);
                                        float fsDestOpp = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, location, fsOpp, false, false);
                                        float fsDestOur = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, location, playerId, false, false);
                                        if (fsDestOpp <= 0 && fsDestOur <= 0) {
                                            int fsRemain = 0; float fsRemainMaxAb = 0f;
                                            for (PhysicalCard oc : gameState.getCardsAtLocation(fsOrigin)) {
                                                if (oc == null || oc.getBlueprint() == null) continue;
                                                if (oc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                if (!playerId.equals(oc.getOwner())) continue;
                                                if (oc.getCardId() == fsMover.getCardId()) continue;
                                                if (oc.isUndercover()) continue;
                                                fsRemain++;
                                                Float oa = oc.getBlueprint().hasAbilityAttribute() ? oc.getBlueprint().getAbility() : null;
                                                if (oa != null && oa > fsRemainMaxAb) fsRemainMaxAb = oa;
                                            }
                                            if (fsRemain == 1 && fsRemainMaxAb < 4f) {
                                                action.addReasoning(
                                                    "L1/L4 SPLIT (batch1b): weak mover to empty site would create TWO weak solos",
                                                    -800.0f);
                                                logger.warn("FORMATION SAFETY (move-dest): L1/L4 SPLIT -800 — {} to {} leaves lone weak buddy at {}",
                                                    fsMover.getTitle(), location.getTitle(), fsOrigin.getTitle());
                                            }
                                        }
                                    }
                                } catch (Exception fsSplitE) { /* fail-open */ }
                            }
                        }

                        // Get power at destination
                        float ourPower = 0;
                        float theirPower = 0;

                        if (game != null && playerId != null) {
                            String opponentId = gameState.getOpponent(playerId);

                            // Calculate our power at destination
                            for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
                                if (card == null) continue;
                                String owner = card.getOwner();
                                SwccgCardBlueprint bp = card.getBlueprint();
                                if (bp == null || !bp.hasPowerAttribute()) continue;

                                Float power = bp.getPower();
                                if (power == null) continue;

                                if (playerId.equals(owner)) {
                                    ourPower += power;
                                } else if (opponentId != null && opponentId.equals(owner)) {
                                    theirPower += power;
                                }
                            }
                        }

                        // V156 JOIN-GROUP (2026-07-07): friendly presence at this destination.
                        // v156DestFriendlyChars (count) gates the V41 wrong-direction exemption;
                        // v156DestAbilityTotal (STACK-MATH) ranks join destinations by the shared predicate.
                        int v156DestFriendlyChars = 0;
                        float v156DestAbilityTotal = 0f;
                        if (v156JoinMode) {
                            try {
                                for (PhysicalCard c : gameState.getCardsAtLocation(location)) {
                                    if (c == null || !playerId.equals(c.getOwner())) continue;
                                    if (c.getBlueprint() == null) continue;
                                    if (c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        v156DestFriendlyChars++;
                                    }
                                }
                                v156DestAbilityTotal = com.gempukku.swccgo.ai.models.common.strategy.MovePredicates
                                    .siteAbilityTotal(gameState, location, playerId);
                            } catch (Exception ignore) { /* 0 */ }
                        }

                        // === ICON-BASED SCORING ===
                        SwccgCardBlueprint bp = location.getBlueprint();
                        int myIcons = 0;
                        int theirIcons = 0;

                        if (bp != null) {
                            // Get force icons based on our side
                            int lightIcons = bp.getIconCount(Icon.LIGHT_FORCE);
                            int darkIcons = bp.getIconCount(Icon.DARK_FORCE);

                            if (mySide == Side.LIGHT) {
                                myIcons = lightIcons;
                                theirIcons = darkIcons;
                            } else {
                                myIcons = darkIcons;
                                theirIcons = lightIcons;
                            }

                            // Bonus for opponent icons (force drain potential!)
                            if (theirIcons > 0) {
                                float iconScore = theirIcons * ICON_BONUS;
                                action.addReasoning(theirIcons + " opponent icons = force drain potential!", iconScore);
                                logger.debug("Move dest {}: +{} for {} opponent icons",
                                    title, iconScore, theirIcons);
                            }

                            // Smaller bonus for our icons (force generation)
                            if (myIcons > 0) {
                                float iconScore = myIcons * (ICON_BONUS / 2);
                                action.addReasoning(myIcons + " of our icons = force generation", iconScore);
                            }

                            // Penalty for no icons at all
                            int totalIcons = myIcons + theirIcons;
                            if (totalIcons == 0) {
                                action.addReasoning("No icons at location - low value", -10.0f);
                            }

                            // === V169 RETREAT BONUS: endangered mover -> safe destination ===
                            // Only fires in retreat mode (mover's current site is outpowered).
                            // +600 dominates the zero-drain penalty (V67g -200) and normal drain
                            // tiebreakers (~+15-48), so the destination step picks a safe site
                            // instead of cancelling out (the cancel-out is what got Asajj killed).
                            if (v169RetreatMode && theirPower == 0) {
                                action.addReasoning(String.format(
                                    "V169 RETREAT: %s is safe (no opponent power) — get the endangered character out of %s!",
                                    title, v169FromTitle), 600.0f);
                                logger.warn("V169 RETREAT (move dest): {} -> +600 (fleeing {})", title, v169FromTitle);
                            }

                            // === V156 JOIN-GROUP DEST (2026-07-07, STACK-MATH refit): weak solo → defensible stack ===
                            // Rank join destinations by ABILITY-TOTAL, not headcount: base +250 for any
                            // friendly group, a small ability-total lean (up to +100), and a +150 kicker
                            // when this join makes the stack destiny-capable (dest total + mover ability >= 4),
                            // cap +450. Fel boundary preserved: Citadel Tower (Vader+Tagge+Trooper = ability 11,
                            // already defensible) scores the +450 cap, well above the ~-152 non-join PASS.
                            if (v156JoinMode && v156DestFriendlyChars > 0) {
                                boolean v156Defensible = (v156DestAbilityTotal + v156MoverAbility)
                                    >= com.gempukku.swccgo.ai.models.common.strategy.MovePredicates.DEFENSIBLE_ABILITY;
                                float v156JoinBonus = Math.min(450.0f,
                                    250.0f + Math.min(100.0f, v156DestAbilityTotal * 10.0f) + (v156Defensible ? 150.0f : 0.0f));
                                action.addReasoning(String.format(
                                    "V156 JOIN-GROUP DEST: %s reaches ability %.0f%s — join (weak solo leaving %s)!",
                                    title, v156DestAbilityTotal + v156MoverAbility,
                                    v156Defensible ? " (destiny-capable)" : "", v156FromTitle), v156JoinBonus);
                                logger.warn("V156 JOIN-GROUP DEST: {} (stack ability {}->{}) -> +{} (mover from {})",
                                    title, (int) v156DestAbilityTotal, (int) (v156DestAbilityTotal + v156MoverAbility),
                                    (int) v156JoinBonus, v156FromTitle);
                            }

                            // === V166 (Steve, 2026-06): CONTEST THE OPPONENT'S DRAIN when out-drained ===
                            // When the opponent out-drains us by net >= 2 (bonus-aware, weapons/objective/
                            // Effect drains included), MOVE cards to contest their drain sites — creating
                            // contested sites so V164a can battle and break the drain, instead of both sides
                            // parallel-draining for 20+ turns. Prefer the SOFTEST site (fewest opponent cards
                            // = easiest to clear or spy-block). Magnitude is decisive over a normal drain
                            // destination (V67e ~+24-48) but stays well under the safety rails (V67n +1500,
                            // battle reserve guards); V164a's own guards still stop suicide battles once the
                            // site is contested. Only computes the (O(locations)) balance for genuinely
                            // contested destinations (theirPower>0 && opp drains here).
                            if (game != null && playerId != null && theirPower > 0) {
                                try {
                                    String v166Opp = gameState.getOpponent(playerId);
                                    int v166OppDrainHere = (int) game.getModifiersQuerying()
                                        .getForceDrainAmount(gameState, location, v166Opp);
                                    if (v166OppDrainHere > 0
                                            && computeNetDrainBalance(game, gameState, playerId) >= 2) {
                                        int v166OppCards = 0;
                                        for (PhysicalCard c : gameState.getCardsAtLocation(location))
                                            if (c != null && v166Opp.equals(c.getOwner())) v166OppCards++;
                                        float v166 = 200.0f + Math.max(0f, 150f - (v166OppCards - 1) * 50f);
                                        action.addReasoning(String.format(
                                            "V166 CONTEST DRAIN: opponent out-draining (net>=2) — contest %s (their drain %d, %d opp cards)",
                                            title, v166OppDrainHere, v166OppCards), v166);
                                        logger.warn("V166 CONTEST DRAIN: target={} oppDrainHere={} oppCards={} -> +{}",
                                            title, v166OppDrainHere, v166OppCards, (int) v166);
                                    }
                                } catch (Exception e) { logger.debug("V166 error: {}", e.getMessage()); }
                            }

                            // === V67e/V67g EXPECTED FORCE LOSS — TIE-BREAKER + DRAIN-AWARE PENALTY ===
                            // Steve's rule: "When there is a tie for points the default scoring
                            // should re-look at whether the decision will make opponent lose more
                            // or less force. Less force drain should be considered a bad move."
                            // V67g STRENGTHENED: −25 zero-drain wasn't enough to dominate tactical
                            // bonuses — Luke + Leia moved Guest Quarters (drain) → Upper Plaza
                            // Corridor (no drain) then back. Now penalty is much stronger AND a
                            // new MOVE-FROM-DRAIN penalty fires when we're abandoning a draining
                            // site for a non-draining one.
                            float v67eExpectedDrain = theirIcons;
                            try {
                                if (game.getModifiersQuerying().isBattleground(gameState, location, null)) {
                                    v67eExpectedDrain *= 1.25f;
                                }
                            } catch (Exception e) { /* ignore */ }
                            // V67k: Some sites have 0 drain by design but are STRATEGIC
                            // staging sites — penalizing them blocks key plays. Currently
                            // recognized: Mapuzo: Underground Corridor (Hidden Path transit
                            // staging — Jedi MUST go here to fire "Move Jedi Survivor here
                            // to a site" and flip the objective).
                            // FIXES "Rando still moving from higher-drain to lower-drain":
                            // V67g was blocking Safehouse → Underground Corridor at −432
                            // and Rando went Safehouse → Spaceport Docking Bay instead,
                            // never reaching the transit hub.
                            String v67kTitleLower = title != null
                                ? title.toLowerCase(java.util.Locale.ROOT) : "";
                            boolean v67kIsTransitStagingSite =
                                v67kTitleLower.contains("underground corridor");

                            if (v67eExpectedDrain > 0) {
                                float v67eBonus = v67eExpectedDrain * 12.0f;
                                action.addReasoning(String.format(
                                    "V67e DRAIN POTENTIAL: drain %.1f at %s = +%.0f opponent force loss",
                                    v67eExpectedDrain, title, v67eBonus), v67eBonus);
                                logger.info("V67e DRAIN POTENTIAL: {} drain={} → +{} (tiebreaker: prefer max drain)",
                                    title, v67eExpectedDrain, (int)v67eBonus);
                            } else if (v67kIsTransitStagingSite) {
                                // V67n: Corridor needs to OUTSCORE other Mapuzo destinations,
                                // not just be exempt from penalty. Other Mapuzo sites have
                                // Dark icons (drain potential), giving them V67e + ICON_BONUS
                                // (~+30) — Corridor with 0 score loses to them. Then Rando
                                // ping-pongs Mining Village ↔ Safehouse and never reaches
                                // Corridor to flip Hidden Path / Fallen Order.
                                // +1500 dominates V67e/ICON_BONUS on other Mapuzo sites and
                                // matches V67l location-pull priority. Only fires when destination
                                // matches "underground corridor" — narrowly scoped.
                                action.addReasoning("V67n TRANSIT STAGING DEST: " + title
                                    + " is the Hidden Path transit hub — Jedi MUST channel through here!",
                                    1500.0f);
                                logger.warn("V67n TRANSIT STAGING DEST: {} → +1500 (dominates other Mapuzo destinations)", title);
                            } else {
                                // V67g: Zero-drain destination — STRONG penalty (was -25, now -200).
                                // Interior corridors / non-icon sites have no drain potential and
                                // characters parked there contribute nothing.
                                action.addReasoning("V67g ZERO DRAIN: " + title
                                    + " has no opponent force icons — wasted move!", -200.0f);
                                logger.warn("V67g ZERO DRAIN: {} no drain — strong penalty (-200)", title);
                            }

                            // V67g MOVE-FROM-DRAIN — additional penalty when this is a MOVE
                            // (not deploy) and we're leaving a draining site for a worse one.
                            // The decision text "Choose where to move <X>" tells us this is a move.
                            // V67k EXEMPTION: skip when destination is a transit staging site.
                            try {
                                if (v67kIsTransitStagingSite) {
                                    logger.info("V67k MOVE-FROM-DRAIN exempt: {} is transit staging site", title);
                                } else {
                                String dt = context.getDecisionText() != null
                                    ? context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
                                boolean isMoveDecision = dt.contains("where to move") || dt.contains("move to,");
                                if (isMoveDecision && playerId != null) {
                                    // Find the moving character's current location and that
                                    // location's drain potential — if higher than the destination,
                                    // penalize abandoning it.
                                    String dtForName = context.getDecisionText() != null
                                        ? context.getDecisionText() : "";
                                    java.util.regex.Matcher mvNameMatch = java.util.regex.Pattern.compile(
                                        "value='([^']+)'>").matcher(dtForName);
                                    if (mvNameMatch.find()) {
                                        String mvBp = mvNameMatch.group(1);
                                        // Find this character on the table
                                        for (PhysicalCard cur : gameState.getAllPermanentCards()) {
                                            if (cur == null || cur.getBlueprintId(true) == null) continue;
                                            if (!playerId.equals(cur.getOwner())) continue;
                                            if (!mvBp.equals(cur.getBlueprintId(true))) continue;
                                            PhysicalCard fromLoc = cur.getAtLocation();
                                            if (fromLoc == null || fromLoc == location) break;
                                            SwccgCardBlueprint fromBp = fromLoc.getBlueprint();
                                            if (fromBp == null) break;
                                            int fromTheirIcons = (mySide == Side.LIGHT)
                                                ? fromBp.getIconCount(Icon.DARK_FORCE)
                                                : fromBp.getIconCount(Icon.LIGHT_FORCE);
                                            if (fromTheirIcons > theirIcons) {
                                                int dropAmt = fromTheirIcons - theirIcons;
                                                float v67gPenalty = -250.0f * dropAmt;
                                                action.addReasoning(String.format(
                                                    "V67g MOVE-FROM-DRAIN: leaving %s (drain %d) for %s (drain %d) — losing %d drain!",
                                                    fromLoc.getTitle(), fromTheirIcons, title, theirIcons, dropAmt),
                                                    v67gPenalty);
                                                logger.warn("V67g MOVE-FROM-DRAIN: leaving {} drain {} for {} drain {} → {}",
                                                    fromLoc.getTitle(), fromTheirIcons, title, theirIcons, (int)v67gPenalty);
                                            }
                                            break;
                                        }
                                    }
                                }
                                }  // close else (v67kIsTransitStagingSite exemption)
                            } catch (Exception e) { /* ignore */ }
                        }

                        // === V67au (Steve, 2026-05-08): RETREAT-TO-DRAIN STRATEGY ===
                        //
                        // When Rando is at an over-contested battleground (enemy power
                        // exceeds Rando's), and the candidate move destination is a
                        // SAFE adjacent non-BG with friendly drain icons and no
                        // opponents, this is a 'deploy-then-move-to-drain' play:
                        // Rando deploys characters to a contested BG (because that's
                        // where his deck wants them), then moves them out next turn to
                        // an empty drainable adjacent site. Net effect: avoids battle
                        // suicide AND drains uncontested AND spreads pressure.
                        //
                        // Strict version (Steve's choice): only fire when there's a
                        // CONFIRMED escape route — destination has zero opponents AND
                        // friendly drain icons. Otherwise no bonus (don't reward
                        // arbitrary retreats).
                        try {
                            String dtForRetreat = context.getDecisionText() != null
                                ? context.getDecisionText() : "";
                            java.util.regex.Matcher mvForRetreatMatch = java.util.regex.Pattern.compile(
                                "value='([^']+)'>").matcher(dtForRetreat);
                            if (mvForRetreatMatch.find()) {
                                String retBp = mvForRetreatMatch.group(1);
                                PhysicalCard retFromLoc = null;
                                for (PhysicalCard cur : gameState.getAllPermanentCards()) {
                                    if (cur == null || cur.getBlueprintId(true) == null) continue;
                                    if (!playerId.equals(cur.getOwner())) continue;
                                    if (!retBp.equals(cur.getBlueprintId(true))) continue;
                                    retFromLoc = cur.getAtLocation();
                                    break;
                                }
                                if (retFromLoc != null && retFromLoc != location && game != null) {
                                    String oppId = gameState.getOpponent(playerId);
                                    float fromOppPower = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, retFromLoc, oppId, false, false);
                                    float fromOurPower = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, retFromLoc, playerId, false, false);
                                    boolean fromIsBg = game.getModifiersQuerying()
                                        .isBattleground(gameState, retFromLoc, null);
                                    boolean fromOverContested = fromIsBg
                                        && fromOppPower > 0
                                        && fromOppPower > fromOurPower;

                                    boolean destIsBg = game.getModifiersQuerying()
                                        .isBattleground(gameState, location, null);
                                    float destOppPower = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, location, oppId, false, false);
                                    int destFriendlyDrainIcons = 0;
                                    SwccgCardBlueprint destBp = location.getBlueprint();
                                    if (destBp != null) {
                                        // Friendly drain icons = MY side icons at destination
                                        if (mySide == Side.LIGHT) {
                                            destFriendlyDrainIcons = destBp.getIconCount(Icon.LIGHT_FORCE);
                                        } else {
                                            destFriendlyDrainIcons = destBp.getIconCount(Icon.DARK_FORCE);
                                        }
                                    }

                                    if (fromOverContested && !destIsBg && destOppPower == 0
                                            && destFriendlyDrainIcons > 0) {
                                        action.addReasoning(String.format(
                                            "V67au RETREAT-TO-DRAIN: %s is over-contested (their %0.f vs our %0.f) — move to safe adjacent %s (no opp, %d friendly icons) and drain there!",
                                            retFromLoc.getTitle(), fromOppPower, fromOurPower,
                                            title, destFriendlyDrainIcons), 400.0f);
                                        logger.warn("V67au RETREAT-TO-DRAIN: from={} (opp {}, ours {}) → to {} (non-BG, empty, {} icons) → +400",
                                            retFromLoc.getTitle(), (int) fromOppPower, (int) fromOurPower,
                                            title, destFriendlyDrainIcons);
                                    }
                                }
                            }
                        } catch (Exception e) { logger.debug("V67au error: {}", e.getMessage()); }

                        // === POWER-BASED SCORING ===
                        if (ourPower >= theirPower && theirPower > 0) {
                            action.addReasoning("We have power advantage here", GOOD_DELTA);
                        } else if (theirPower - ourPower <= 2 && theirPower > 0) {
                            action.addReasoning("Can help reinforce here", GOOD_DELTA);
                        } else if (theirPower == 0) {
                            // Unoccupied - good if it has icons
                            if (theirIcons > 0) {
                                action.addReasoning("Unoccupied with opponent icons - force drain!", GOOD_DELTA * 2);
                            } else if (myIcons > 0) {
                                action.addReasoning("Unoccupied with our icons - control", GOOD_DELTA);
                            } else {
                                action.addReasoning("Unoccupied but no icons - low priority", 0.0f);
                            }
                        } else {
                            // Enemy is much stronger - penalty scales with their power
                            float penalty = BAD_DELTA * (theirPower / 2);
                            action.addReasoning("Enemy too strong (" + (int)theirPower + " power)", penalty);
                        }

                        // V29.7: Bonus for battleground locations (move preference)
                        // Use real game engine API for accurate battleground detection.
                        // Only penalize non-BG if BG alternatives exist on the table.
                        if (location != null && game != null && gameState != null) {
                            try {
                                boolean isBG = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                if (isBG) {
                                    action.addReasoning("V29.7 Move to battleground — force drains!", 40.0f);
                                } else {
                                    // V29.7: Don't penalize non-BG moves when no BG exists
                                    action.addReasoning("V29.7 Non-battleground destination", 0.0f);
                                }
                            } catch (Exception e) {
                                // Fallback to old heuristic
                                if (bp != null) {
                                    String titleLowerBg = title != null ? title.toLowerCase() : "";
                                    if (titleLowerBg.contains("battleground")) {
                                        action.addReasoning("Battleground location", 15.0f);
                                    }
                                }
                            }
                        }

                        // === V24.9: PREFER UNOCCUPIED CC SITES (ESCAPE SPY-BLOCKED LOCATIONS) ===
                        // If the destination is an objective-relevant CC site with no opponent presence,
                        // moving here means we can force drain uncontested. Big bonus.
                        {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer moveObjCheck =
                                context.getObjectiveAnalyzer();
                            if (moveObjCheck != null && moveObjCheck.needsBespinSystemPresence()) {
                                String destTitle = title != null ? title : "";
                                boolean isObjLoc = moveObjCheck.isObjectiveRelevantLocation(destTitle);
                                if (isObjLoc && theirPower == 0 && ourPower == 0) {
                                    // Unoccupied CC site — moving here creates a new drain site!
                                    action.addReasoning("V24.9: Unoccupied CC site — free force drain if we move here!", 200.0f);
                                    logger.info("V24.9: Move dest {} is unoccupied CC — big bonus (+200)", title);
                                } else if (isObjLoc && theirPower == 0 && ourPower > 0) {
                                    // We already have presence — reinforcement is less critical
                                    action.addReasoning("V24.9: CC site with only our presence — already draining", 20.0f);
                                }
                            }
                        }

                        // === V64 POWER-AWARE MOVE DESTINATION — don't send Jedi to their death ===
                        // When transiting Jedi off Mapuzo, avoid sites where the opponent's
                        // total power exceeds what our available Jedi can match. Rando
                        // previously sent Kelleran (power 5) to Jabiim: Starship Hangar
                        // where Grand Inquisitor + Emperor Palpatine sat (combined 13+
                        // power) — instant kill. Hidden Path Jedi are ~6-7 power flipped,
                        // so destinations with opponent power ≥ 8 without our own support
                        // are suicide moves.
                        // FIXES z7qk4ap0b72e4uvm replay (msg 324): Kelleran moved into
                        // Grand Inquisitor + Emperor → Steve won battle at msg 451.
                        // Steve's preferred strategy: drain pressure via split-sites, not
                        // battle initiation into stronger enemies.
                        {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v64Obj =
                                context.getObjectiveAnalyzer();
                            boolean v64HiddenPath = v64Obj != null && v64Obj.isAnalyzed()
                                && v64Obj.getObjectiveTitle() != null
                                && v64Obj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path");
                            if (v64HiddenPath && game != null && gameState != null
                                && title != null
                                && !title.toLowerCase(java.util.Locale.ROOT).contains("mapuzo")) {
                                // V65: Tightened threshold from 8 to 7 — Lord Vader at printed
                                // power 7 with DVL slipped through. A lone Jedi vs Vader on
                                // opponent's next-turn deploy+battle phase is a guaranteed loss.
                                // Steve: "moving solo obiwan against vader when it's my turn
                                // next to deploy and battle is a very bad idea."
                                // Assume our Jedi move-in adds ~6 power (typical Jedi Survivor
                                // when flipped).
                                float assumedJediPower = 6.0f;
                                float projectedOurPower = ourPower + assumedJediPower;
                                if (theirPower >= 7 && projectedOurPower < theirPower + 2) {
                                    // Solo Jedi move: opponent will out-deploy and battle NEXT turn.
                                    // Require our projected power to exceed theirs by 2+ to call it safe.
                                    float deathPenalty = -1500.0f;
                                    if (theirPower >= 9) deathPenalty = -1800.0f;
                                    if (theirPower >= 12) deathPenalty = -2500.0f;
                                    action.addReasoning(
                                        "V64 SUICIDE MOVE: " + title + " has enemy power "
                                            + (int)theirPower + " — solo Jedi will DIE on their next turn!",
                                        deathPenalty);
                                    logger.warn("V64 SUICIDE MOVE: {} enemy={} our projected={} — HARD BLOCKED ({})",
                                        title, (int)theirPower, (int)projectedOurPower, (int)deathPenalty);
                                } else if (theirPower == 0) {
                                    // Empty site — excellent drain target
                                    action.addReasoning(
                                        "V64 SAFE DRAIN: " + title + " is empty — Jedi can drain without opposition!",
                                        150.0f);
                                    logger.info("V64 SAFE DRAIN: {} empty — ideal drain destination (+150)", title);
                                } else if (projectedOurPower >= theirPower + 3) {
                                    // We'll have clear power advantage
                                    action.addReasoning(
                                        "V64 FAVORABLE: " + title + " — Jedi arrival gives us power advantage",
                                        80.0f);
                                }
                            }
                        }

                        // === V62 HIDDEN PATH SPLIT-SITE ===
                        // Hidden Path flips when we have 2 Jedi Survivors at 2 DIFFERENT
                        // battleground/opponent sites outside Mapuzo. If we've already
                        // placed a Jedi at one non-Mapuzo battleground, the 2nd Jedi must
                        // go to a DIFFERENT battleground to trigger the flip. Moving both
                        // to the same site wastes a turn (no flip progress).
                        // FIXES fmz03bjz79k61img replay: Rando moved Kelleran + Quinlan
                        // from Corridor to the same Malachor: Sith Temple Upper Chamber,
                        // delaying the objective flip.
                        {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v62Obj =
                                context.getObjectiveAnalyzer();
                            boolean onHiddenPath = v62Obj != null && v62Obj.isAnalyzed()
                                && v62Obj.getObjectiveTitle() != null
                                && v62Obj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path")
                                && !v62Obj.isFlipped();
                            if (onHiddenPath && game != null && gameState != null && title != null
                                && !title.toLowerCase(java.util.Locale.ROOT).contains("mapuzo")) {
                                try {
                                    boolean isBGDest = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                    if (isBGDest) {
                                        // Count our OWN Jedi Survivors already at this destination
                                        int ourJediHere = 0;
                                        java.util.List<PhysicalCard> hereCards = gameState.getCardsAtLocation(location);
                                        if (hereCards != null) {
                                            for (PhysicalCard hc : hereCards) {
                                                if (hc != null && playerId.equals(hc.getOwner())
                                                    && hc.getBlueprint() != null
                                                    && hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                    String hcText = hc.getBlueprint().getGameText();
                                                    String hcTitle = hc.getTitle() != null
                                                        ? hc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                    boolean isJediSurv = (hcText != null
                                                            && hcText.toLowerCase(java.util.Locale.ROOT).contains("jedi survivor"))
                                                        || hcTitle.contains("obi-wan") || hcTitle.contains("kelleran")
                                                        || hcTitle.contains("quinlan") || hcTitle.contains("ahsoka")
                                                        || hcTitle.contains("cal kestis") || hcTitle.contains("cere");
                                                    if (isJediSurv) ourJediHere++;
                                                }
                                            }
                                        }
                                        if (ourJediHere >= 1) {
                                            action.addReasoning(
                                                "V62 SPLIT SITE: Already have " + ourJediHere
                                                    + " Jedi at " + title
                                                    + " — move 2nd Jedi to a DIFFERENT battleground to flip Hidden Path!",
                                                -500.0f);
                                            logger.warn("V62 SPLIT SITE: {} has {} friendly Jedi — penalize duplicate dest (-500)",
                                                title, ourJediHere);
                                        } else {
                                            // Empty BG outside Mapuzo — ideal split-site destination
                                            action.addReasoning(
                                                "V62 SPLIT SITE: No friendly Jedi at " + title
                                                    + " yet — great split-site target for Hidden Path flip!",
                                                200.0f);
                                            logger.info("V62 SPLIT SITE: {} is ideal split-site for Hidden Path (+200)",
                                                title);
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }
                        }

                        // === V62 DON'T DILUTE OUR OWN UNDERCOVER SPY ===
                        // Undercover spies block force drains at their location WHILE they
                        // stay undercover. If we move non-spy characters to the same site,
                        // the spy's purpose is wasted — we now occupy openly and can drain
                        // ourselves (if opponent has 0 power), OR if opponent still has
                        // power, the spy is redundant. Better to keep Jedi at safe sites
                        // and let the spy do its solo blocking job.
                        // FIXES fmz03bjz79k61img replay: Rando deployed Boushh as spy at
                        // Sith Temple Entrance (Emperor's location), then moved BOTH Jedi
                        // to the SAME site — making the spy useless.
                        if (game != null && gameState != null && location != null) {
                            try {
                                java.util.List<PhysicalCard> siteCards = gameState.getCardsAtLocation(location);
                                boolean ourSpyHere = false;
                                if (siteCards != null) {
                                    for (PhysicalCard sc : siteCards) {
                                        if (sc != null && playerId.equals(sc.getOwner())
                                            && sc.isUndercover()) {
                                            ourSpyHere = true;
                                            break;
                                        }
                                    }
                                }
                                // Only penalize if the card being moved is NOT itself a spy
                                // (spies moving to other spies is fine; non-spy joining a spy is bad)
                                boolean movingCardIsSpy = false;
                                // The card being moved isn't directly in context at this point,
                                // but since this is a destination-selection decision, it's a move
                                // of a SPECIFIC character chosen in a prior step. We approximate:
                                // check if the decision text mentions a known spy name.
                                String dt = context.getDecisionText() != null
                                    ? context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
                                if (dt.contains("jyn erso") || dt.contains("boushh")
                                    || dt.contains("orrimaarko")) {
                                    movingCardIsSpy = true;
                                }
                                if (ourSpyHere && !movingCardIsSpy) {
                                    // V65: Strengthened from -400 to -1500. Previous -400 was
                                    // getting overridden by +300 V41 CONTEST DEST + +300 contest
                                    // bonus from the spy's enemy presence. -1500 ensures spy
                                    // dilution is a near-hard block when safer alternatives exist.
                                    action.addReasoning(
                                        "V62 SPY DILUTION: Our undercover spy is at " + title
                                            + " — moving a non-spy here wastes the spy's drain-blocking!",
                                        -1500.0f);
                                    logger.warn("V62 SPY DILUTION: {} has our spy — don't dilute (-1500)", title);
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // === V24.13: LANDO ALONE DETECTION — MOVE TO SUPPORT ===
                        // If Lando is the only friendly character at this CC site, big bonus
                        // to move here and protect him. Lando alone = easy kill for opponent.
                        if (game != null && playerId != null) {
                            try {
                                java.util.List<PhysicalCard> destCards = gameState.getCardsAtLocation(location);
                                if (destCards != null) {
                                    boolean landoAlone = false;
                                    int ourCharCount = 0;
                                    for (PhysicalCard c : destCards) {
                                        if (c == null || !playerId.equals(c.getOwner())) continue;
                                        if (c.getBlueprint() == null) continue;
                                        if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        ourCharCount++;
                                        String cTitle = c.getTitle();
                                        if (cTitle != null && cTitle.toLowerCase(java.util.Locale.ROOT).contains("lando")) {
                                            landoAlone = true;
                                        }
                                    }
                                    if (landoAlone && ourCharCount == 1) {
                                        action.addReasoning(
                                            "V24.13 LANDO SUPPORT: Lando is ALONE here — move to protect him!", 250.0f);
                                        logger.warn("V24.13 LANDO ALONE AT {}: Moving here to support (+250)", title);
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // === V47: LANDO MOVEMENT — STAY AT DINING ROOM ===
                        // Lando should NOT move from Dining Room. He establishes occupation there
                        // and moving wastes force / loses presence. Only move if we have 3+ friendlies
                        // at his current location (he's redundant) and destination is unoccupied CC site.
                        String moveDecisionText = context.getDecisionText() != null
                            ? context.getDecisionText().toLowerCase() : "";
                        if (moveDecisionText.contains("lando")) {
                            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer moveObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            if (moveObjAnalyzer != null && moveObjAnalyzer.needsBespinSystemPresence()) {
                                // V47: Block most Lando moves — he stays where he is
                                action.addReasoning("V47 LANDO STAY: Lando should stay put — moving wastes force and loses occupation!", -9999.0f);
                                logger.warn("V47 LANDO STAY: Blocking Lando move to {} — stay at current location!", title);
                            }
                        }

                        // === V24.14B: WEAPON CHARACTERS TO SPACE — MOVEMENT PENALTY ===
                        // Characters with "permanent weapon" in game text shouldn't shuttle/move
                        // to system locations (space) — their weapons can't fire there.
                        {
                            boolean destIsSpace = false;
                            if (bp != null) {
                                com.gempukku.swccgo.common.CardSubtype destSubtype = bp.getCardSubtype();
                                destIsSpace = (destSubtype == com.gempukku.swccgo.common.CardSubtype.SYSTEM);
                            }
                            if (destIsSpace) {
                                // Check if the character being moved has a permanent weapon
                                boolean movingCharHasWeapon = false;
                                // Check decision text for weapon keywords in card name (fallback)
                                if (moveDecisionText.contains("lightsaber") || moveDecisionText.contains("blaster")
                                    || moveDecisionText.contains("with rifle") || moveDecisionText.contains("with cannon")) {
                                    movingCharHasWeapon = true;
                                }
                                // Check blueprint game text for "permanent weapon" (universal)
                                if (!movingCharHasWeapon) {
                                    // Try to extract the moving card's blueprint from decision text
                                    String moveBpId = extractBlueprintFromDecisionText(context.getDecisionText());
                                    if (moveBpId != null) {
                                        try {
                                            SwccgCardBlueprint moveBp = getBlueprintFromId(context, moveBpId);
                                            if (moveBp != null) {
                                                String moveGameText = moveBp.getGameText();
                                                if (moveGameText != null &&
                                                    moveGameText.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                                                    movingCharHasWeapon = true;
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V24.14B: Error checking move blueprint: {}", e.getMessage());
                                        }
                                    }
                                }
                                if (movingCharHasWeapon) {
                                    action.addReasoning("V24.14B WEAPON CHAR TO SPACE: Permanent weapon can't fire at system locations — don't shuttle here!", -300.0f);
                                    logger.warn("V24.14B WEAPON MOVE: Char with permanent weapon moving to space {} — penalized (-300)", title);
                                }
                                // Also penalize vehicles moving to space
                                if (moveDecisionText.contains("vehicle")) {
                                    action.addReasoning("V24.14B VEHICLE TO SPACE: Vehicles don't belong in space!", -300.0f);
                                    logger.warn("V24.14B VEHICLE MOVE: Vehicle moving to space {} — penalized (-300)", title);
                                }
                            }
                        }

                        // === V41: HUNT DOWN — MOVE DESTINATION AWARENESS ===
                        // When choosing a move destination (e.g., Vader's Castle ability),
                        // STRONGLY prefer locations with opponents, especially Jedi.
                        // This fixes Vader going to empty Mapuzo Safehouse instead of
                        // Malachor Entrance where Obi-Wan was draining 4 per turn.
                        // V67f2: Exclude UNDERCOVER SPIES from "go fight" bonus — a spy
                        // doesn't actively threaten us; moving Jedi to an opp-spy site
                        // wastes drain potential. FIXES uarc0hmiai1i594y replay: Ezra
                        // and Young Skywalker piled into Tatooine: Mos Eisley because
                        // V41 saw "+300 go fight" on Steve's U-3PO spy (power 1).
                        if (game != null && playerId != null) {
                            try {
                                String opponentId = gameState.getOpponent(playerId);

                                // V67f2: Recompute opponent power EXCLUDING undercover spies.
                                float v67fNonSpyOpponentPower = 0;
                                int v67fSpiesHere = 0;
                                try {
                                    java.util.List<PhysicalCard> hereCards =
                                        gameState.getCardsAtLocation(location);
                                    if (hereCards != null) {
                                        for (PhysicalCard hc : hereCards) {
                                            if (hc == null) continue;
                                            if (!opponentId.equals(hc.getOwner())) continue;
                                            if (hc.isUndercover()) {
                                                v67fSpiesHere++;
                                                continue;
                                            }
                                            SwccgCardBlueprint hcBp = hc.getBlueprint();
                                            if (hcBp != null && hcBp.hasPowerAttribute()) {
                                                Float p = hcBp.getPower();
                                                if (p != null) v67fNonSpyOpponentPower += p;
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                // V67aa (Steve, 2026-05-03): HIDDEN PATH JEDI SUICIDE BLOCK.
                                // When on Hidden Path pre-flip, Jedi survivors are power 3
                                // (Fallen Order Effect), and the V41 CONTEST DEST 'go fight'
                                // bonus would send them into power-8+ enemy sites where they
                                // get killed solo. Symptom: Rando moved both Jedi to Hoth
                                // (where Steve had power 8) instead of spreading to empty
                                // Jabiim, then sent solo Obi-Wan to Hoth and lost the game.
                                //
                                // Rule: on Hidden Path pre-flip, any destination with opp power
                                // ≥ 5 AND our power = 0 is suicide for the weak Jedi → hard-block.
                                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v67aaObj =
                                    context.getObjectiveAnalyzer();
                                boolean v67aaOnHiddenPath = v67aaObj != null && v67aaObj.isAnalyzed()
                                    && v67aaObj.getObjectiveTitle() != null
                                    && v67aaObj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path")
                                    && !v67aaObj.isFlipped();
                                if (v67aaOnHiddenPath && v67fNonSpyOpponentPower >= 5 && ourPower == 0) {
                                    action.addReasoning(String.format(
                                        "V67aa HIDDEN PATH SUICIDE BLOCK: %s has opp power %.0f — pre-flip Jedi survivors are power 3, this is SUICIDE!",
                                        title, v67fNonSpyOpponentPower), -9999.0f);
                                    logger.warn("V67aa SUICIDE BLOCK: {} opp={} our=0 on Hidden Path pre-flip — BLOCK transit (-9999)",
                                        title, v67fNonSpyOpponentPower);
                                    // Skip V41 CONTEST DEST bonus — we already hard-blocked
                                    actions.add(action);
                                    continue;
                                }

                                // Check if non-spy opponents are at this destination
                                if (v67fNonSpyOpponentPower > 0) {
                                    // Opponents here — GREAT move destination!
                                    float contestBonus = 300.0f;

                                    // Extra bonus if uncontested (we have no presence)
                                    if (ourPower == 0) {
                                        contestBonus += 200.0f;
                                        logger.warn("V41 MOVE DEST CONTEST: {} is UNCONTESTED by us — urgent! (+500)", title);
                                    }

                                    // Check for Jedi/Padawan at destination (Hunt Down priority)
                                    boolean jediAtDest = false;
                                    for (PhysicalCard c : gameState.getCardsAtLocation(location)) {
                                        if (c == null || playerId.equals(c.getOwner())) continue;
                                        String cTitle = c.getTitle() != null ? c.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                        if (ActionEvaluator.isJediOrPadawan(cTitle)) {
                                            jediAtDest = true;
                                            break;
                                        }
                                    }
                                    if (jediAtDest) {
                                        contestBonus += 200.0f;
                                        logger.warn("V41 HUNT JEDI DEST: Jedi at {} — Vader must go here! (+{})", title, (int)contestBonus);
                                    }

                                    action.addReasoning(String.format(
                                        "V41 CONTEST DEST: Opponents (power %.0f) at %s%s — go fight!",
                                        v67fNonSpyOpponentPower, title, jediAtDest ? " [JEDI!]" : ""), contestBonus);
                                } else if (v67fSpiesHere > 0) {
                                    // V67f2: Opponent's spy here but no real characters.
                                    // Don't dilute drain potential by piling characters into a spy site.
                                    action.addReasoning(
                                        "V67f SPY-ONLY: " + title + " has only opponent spy ("
                                            + v67fSpiesHere + ") — drain blocked, prefer draining elsewhere",
                                        -100.0f);
                                    logger.warn("V67f SPY-ONLY: {} has only opp spies (no real characters) — penalize move-in (-100)", title);
                                } else {
                                    // No opponents here — check if opponents are draining uncontested ELSEWHERE
                                    // V65 SMART WRONG-DIRECTION: Skip the hard-block when:
                                    //   (a) Our own undercover spy is at the "draining" site
                                    //       (spy neutralizes their drain — it's not actually a threat)
                                    //   (b) The "draining" site is suicide to enter
                                    //       (opponent power too high for our Jedi)
                                    // FIXES qi99bkot034gso86 replay: Obi-Wan forced to join Boushh
                                    // at Jabiim: Starship Hangar vs Lord Vader + DVL — spy was
                                    // already blocking the drain, other BGs were safer drain targets.
                                    boolean opponentsElsewhere = false;
                                    String worstDrainLoc = null;
                                    float worstDrainPower = 0;
                                    for (PhysicalCard otherLoc : gameState.getTopLocations()) {
                                        if (otherLoc == null || otherLoc == location) continue;
                                        float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, otherLoc, opponentId, false, false);
                                        float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, otherLoc, playerId, false, false);
                                        if (oppPower > 0 && ourPowerThere == 0) {
                                            // V65a: Our spy at the drain location blocks it. Skip.
                                            boolean ourSpyBlocksIt = false;
                                            try {
                                                java.util.List<PhysicalCard> cardsAtOther = gameState.getCardsAtLocation(otherLoc);
                                                if (cardsAtOther != null) {
                                                    for (PhysicalCard osc : cardsAtOther) {
                                                        if (osc != null && playerId.equals(osc.getOwner())
                                                            && osc.isUndercover()) {
                                                            ourSpyBlocksIt = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) { /* ignore */ }
                                            if (ourSpyBlocksIt) {
                                                logger.info("V65a SPY-NEUTRALIZED: Not marking {} as wrong-direction — our spy blocks {} drain",
                                                    title, otherLoc.getTitle());
                                                continue;  // don't count this as a drain threat
                                            }
                                            // V65b: Suicide destination — opponent too strong for single Jedi.
                                            // Treat Hidden Path flipped Jedi as ~6 power baseline.
                                            if (oppPower >= 7) {
                                                logger.info("V65b SUICIDE-WRONG-DIR: Not marking {} as wrong-direction — {} has enemy power {} (suicide for Jedi)",
                                                    title, otherLoc.getTitle(), (int)oppPower);
                                                continue;  // don't count this as a drain threat
                                            }
                                            opponentsElsewhere = true;
                                            if (oppPower > worstDrainPower) {
                                                worstDrainPower = oppPower;
                                                worstDrainLoc = otherLoc.getTitle();
                                            }
                                        }
                                    }
                                    if (opponentsElsewhere) {
                                        // V67z (Steve, 2026-05-03): EXEMPT Hidden Path split-sites.
                                        // V41 was hard-blocking Jabiim destinations because opponents
                                        // were at Coruscant. But on Hidden Path, the SMART play is to
                                        // move ONE Jedi to a non-Mapuzo battleground (Jabiim) for the
                                        // objective flip, even if opponents are elsewhere. Symptom:
                                        // Rando moved both Jedi to the SAME Jabiim site instead of
                                        // splitting because V41 -9999 swamped V62 SPLIT SITE +200.
                                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v67zObj =
                                            context.getObjectiveAnalyzer();
                                        boolean v67zOnHiddenPath = v67zObj != null && v67zObj.isAnalyzed()
                                            && v67zObj.getObjectiveTitle() != null
                                            && v67zObj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path")
                                            && !v67zObj.isFlipped();
                                        boolean v67zNonMapuzoBG = false;
                                        if (v67zOnHiddenPath && title != null
                                                && !title.toLowerCase(java.util.Locale.ROOT).contains("mapuzo")) {
                                            try {
                                                v67zNonMapuzoBG = game.getModifiersQuerying()
                                                    .isBattleground(gameState, location, null);
                                            } catch (Exception e) { /* ignore */ }
                                        }
                                        // V67z UPDATE (Steve, 2026-06-18): also exempt the Underground
                                        // Corridor transit HUB. The Hidden Path transit is two steps —
                                        // step 1 Safehouse -> Mapuzo: Underground Corridor (a MAPUZO
                                        // site), step 2 Corridor -> off-Mapuzo. The split-site exemption
                                        // above only covered step 2's non-Mapuzo BGs, so on the step-1
                                        // Corridor move V41 WRONG DIRECTION (-9999) still buried V67n
                                        // (+1500): net -8481 -> Rando PASSED and the crippled Jedi
                                        // survivors (power 3, forfeit 3, game text canceled per Fallen
                                        // Order) rotted at Mapuzo and got slaughtered (replay
                                        // aj816vuaxukwoie2). The Corridor is MANDATORY transit, not a
                                        // "wrong direction" — skip V41 here too so V67n's +1500 wins.
                                        boolean v67zTransitHub = v67zOnHiddenPath && title != null
                                                && title.toLowerCase(java.util.Locale.ROOT).contains("underground corridor");
                                        if (v67zNonMapuzoBG || v67zTransitHub) {
                                            logger.info("V67z HIDDEN PATH {} EXEMPT: {} on Hidden Path — V41 WRONG DIRECTION skipped",
                                                v67zTransitHub ? "TRANSIT-HUB" : "SPLIT", title);
                                        } else if (v169RetreatMode) {
                                            // V169 (Steve, 2026-06): a RETREAT is, by definition, a move to an
                                            // empty site while opponents are elsewhere — V41's block is exactly
                                            // wrong for an endangered mover. This -9999 is what trapped Asajj at
                                            // Guest Quarters (every safe destination blocked -> cancel-loop ->
                                            // move hard-vetoed -> beaten 6v27). Skip it in retreat mode.
                                            logger.warn("V169 RETREAT EXEMPT: {} — V41 wrong-direction skipped (mover fleeing {})",
                                                title, v169FromTitle);
                                        } else if (v156JoinMode && v156DestFriendlyChars > 0) {
                                            // V156 JOIN-GROUP EXEMPT (2026-07-07): a weak solo consolidating
                                            // onto OUR OWN stack is not "wrong direction" — V41's 'empty' only
                                            // counts opponents, so it -9999'd the one join destination and
                                            // stranded Fel at Scarif: Beach (cancel-loop -> V160 veto -> rot ->
                                            // forfeit). Skip it for friendly-character destinations in join
                                            // mode, same pattern as the V169/V67z exemptions above.
                                            logger.warn("V156 JOIN-GROUP EXEMPT: {} has {} friendly character(s) — V41 wrong-direction skipped (weak solo joining from {})",
                                                title, v156DestFriendlyChars, v156FromTitle);
                                        } else {
                                            // V41: WRONG DIRECTION — moving to empty site while opponents drain elsewhere
                                            action.addReasoning(String.format(
                                                "V41 WRONG DIRECTION: %s is empty — opponents draining at %s! Go there instead!",
                                                title, worstDrainLoc), -9999.0f);
                                            logger.warn("V41 WRONG DIRECTION: {} is empty, opponents at {} — BLOCKED", title, worstDrainLoc);
                                        }
                                    }
                                }

                                // V41: CASTLE RETREAT BLOCK — never move back to Castle when opponents exist
                                String destTitleLower = title != null ? title.toLowerCase(java.util.Locale.ROOT) : "";
                                if (destTitleLower.contains("mustafar") && destTitleLower.contains("castle")) {
                                    boolean anyOpponents = false;
                                    for (PhysicalCard otherLoc : gameState.getTopLocations()) {
                                        if (otherLoc == null) continue;
                                        float op = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, otherLoc, opponentId, false, false);
                                        if (op > 0) { anyOpponents = true; break; }
                                    }
                                    if (anyOpponents) {
                                        action.addReasoning("V41 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!", -9999.0f);
                                        logger.warn("V41 CASTLE RETREAT BLOCKED in move destination selection");
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V41 MOVE DEST: Error: {}", e.getMessage());
                            }
                        }

                        // === V24.3C: DR. EVAZAN WEAPON COMBO — MOVEMENT PREFERENCE ===
                        // Move Evazan toward weapon characters, and weapon chars toward Evazan.
                        boolean movingEvazan = moveDecisionText.contains("evazan");
                        boolean movingWeaponChar = (moveDecisionText.contains("maul") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("vader") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("mara") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("jade") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("aurra") && moveDecisionText.contains("blaster"))
                            || (moveDecisionText.contains("sing") && moveDecisionText.contains("blaster"));

                        if (movingEvazan || movingWeaponChar) {
                            boolean comboPartnerAtDest = false;
                            try {
                                java.util.List<PhysicalCard> destCards = gameState.getCardsAtLocation(location);
                                if (destCards != null) {
                                    for (PhysicalCard c : destCards) {
                                        if (c == null || !playerId.equals(c.getOwner())) continue;
                                        String cTitle = c.getTitle();
                                        if (cTitle == null) continue;
                                        String cLower = cTitle.toLowerCase();

                                        if (movingEvazan) {
                                            if ((cLower.contains("maul") && cLower.contains("lightsaber"))
                                                || (cLower.contains("vader") && cLower.contains("lightsaber"))
                                                || (cLower.contains("mara") && cLower.contains("lightsaber"))
                                                || (cLower.contains("jade") && cLower.contains("lightsaber"))
                                                || (cLower.contains("aurra") && cLower.contains("blaster"))
                                                || (cLower.contains("sing") && cLower.contains("blaster"))) {
                                                comboPartnerAtDest = true;
                                                break;
                                            }
                                        } else {
                                            if (cLower.contains("evazan")) {
                                                comboPartnerAtDest = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }

                            if (comboPartnerAtDest) {
                                action.addReasoning(
                                    "V24.3 EVAZAN COMBO: Move here — combo partner at this site for weapon kill combo!",
                                    200.0f);
                                logger.warn("V24.3 EVAZAN COMBO MOVE: Partner found at {} (+200)", title);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse cardId for move destination: {}", cardId);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose card to cancel - cancel opponent's cards.
     */
    private List<EvaluatedAction> evaluateCancelSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Cancel card " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String owner = card.getOwner();

                        // Cancel opponent's cards, not ours!
                        if (!playerId.equals(owner)) {
                            action.addReasoning("Opponent's card - cancel!", 100.0f);
                        } else {
                            action.addReasoning("Our card - don't cancel!", -200.0f);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Check if the decision text indicates we're playing a beneficial card
     * that should target our OWN cards (not opponent's).
     *
     * Examples:
     * - A Few Maneuvers (adds +2 hyperspeed and maneuver to your starship)
     * - Hyper Escape (allows your ship to escape)
     * - Various buff/enhancement cards
     */
    private boolean isBeneficialTargetingCard(String decisionText) {
        if (decisionText == null) return false;
        String textLower = decisionText.toLowerCase();

        // Cards that buff your own cards
        String[] beneficialCards = {
            "a few maneuvers",      // +2 hyperspeed and maneuver
            "hyper escape",         // Escape action
            "evasive action",       // Escape/dodge
            "rebel barrier",        // Defense
            "narrow escape",        // Escape
            "darklighter spin",     // Combat bonus
            "hear me baby",         // Buff
            "all power to weapons", // Attack buff
            "full throttle",        // Speed buff
            "punch it",             // Speed/escape
            "alert my star destroyer", // Defense buff
            "i have you now"        // Attack buff (targets your TIE)
        };

        for (String card : beneficialCards) {
            if (textLower.contains(card)) {
                logger.info("🎯 Detected beneficial card '{}' - targeting own cards", card);
                return true;
            }
        }

        return false;
    }

    /**
     * Target selection for weapons/abilities - must select, don't cancel.
     *
     * IMPORTANT: Some cards target your OWN cards (beneficial buffs like A Few Maneuvers)
     * while others target OPPONENT cards (weapons, disruptions). We detect this from
     * the decision text which shows what card is being played.
     */
    private List<EvaluatedAction> evaluateTargetSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        String decisionText = context.getDecisionText();

        // Check if we're playing a beneficial card that targets our own cards
        boolean targetOwnCards = isBeneficialTargetingCard(decisionText);

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Target " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String owner = card.getOwner();
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        boolean isOurCard = playerId.equals(owner);

                        if (targetOwnCards) {
                            // Beneficial card - target OUR cards, not opponent's
                            if (isOurCard) {
                                action.addReasoning("Beneficial effect on our card", 50.0f);

                                // Prefer high-value targets for buffs
                                if (blueprint != null) {
                                    if (blueprint.hasPowerAttribute()) {
                                        Float power = blueprint.getPower();
                                        if (power != null && power >= 5) {
                                            action.addReasoning("High-power target for buff", 30.0f);
                                        }
                                    }

                                    if (blueprint.getUniqueness() == Uniqueness.UNIQUE) {
                                        action.addReasoning("Unique target for buff", 20.0f);
                                    }
                                }
                            } else {
                                action.addReasoning("Don't buff opponent's card!", -200.0f);
                            }
                        } else {
                            // Harmful card (weapon, etc.) - target OPPONENT cards
                            if (!isOurCard) {
                                action.addReasoning("Target opponent's card", 50.0f);

                                // V51: Don't waste weapons on already-hit characters
                                if (card.isHit()) {
                                    action.addReasoning("V51 ALREADY HIT: Target already hit — don't waste weapon!", -500.0f);
                                    logger.warn("V51 ALREADY HIT: Weapon targeting {} but already hit — -500", card.getTitle());
                                }

                                // V51: Force Lightning / Trample — prioritize opponent spies
                                if (card.isUndercover()) {
                                    action.addReasoning("V51 KILL SPY: Target is an undercover spy — eliminate it!", 500.0f);
                                    logger.warn("V51 KILL SPY: Targeting spy {} — +500!", card.getTitle());
                                }

                                if (blueprint != null) {
                                    // === V36: DESTINY-BASED WEAPON TARGETING ===
                                    // Calculate hit probability: avgDestiny * numDraws vs defense value
                                    // Lightsaber draws 2 destiny. Other weapons draw 1-2.
                                    // Only fire at targets we can actually hit!
                                    SwccgGame targetGame = context.getGame();
                                    if (targetGame != null && gameState != null && context.getPhase() == Phase.BATTLE) {
                                        try {
                                            // Get target's defense value (ability for characters)
                                            float defenseValue = targetGame.getModifiersQuerying().getDefenseValue(gameState, card);

                                            // Get average destiny in reserve deck
                                            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle destOracle = context.getDeckOracle();
                                            double avgDestiny = 3.0; // fallback
                                            if (destOracle != null && destOracle.isAnalyzed()) {
                                                avgDestiny = destOracle.getAverageDestinyInReserve();
                                            }

                                            // Lightsaber draws 2 destiny, most other weapons draw 1
                                            int numDraws = 2; // assume lightsaber
                                            float expectedTotal = (float)(avgDestiny * numDraws);
                                            float hitMargin = expectedTotal - defenseValue;

                                            String targetTitle = card.getTitle() != null ? card.getTitle() : "?";
                                            String targetLower = targetTitle.toLowerCase(java.util.Locale.ROOT);

                                            if (hitMargin >= 3.0f) {
                                                // Easy hit — very likely to succeed
                                                action.addReasoning(String.format(
                                                    "V36 EASY HIT: %s defense %.0f, expected destiny %.1f — HIGH hit chance!",
                                                    targetTitle, defenseValue, expectedTotal), 200.0f);
                                            } else if (hitMargin >= 0.0f) {
                                                // Marginal hit — coin flip
                                                action.addReasoning(String.format(
                                                    "V36 MARGINAL HIT: %s defense %.0f, expected destiny %.1f — might hit",
                                                    targetTitle, defenseValue, expectedTotal), 50.0f);
                                            } else {
                                                // Likely miss — defense too high
                                                action.addReasoning(String.format(
                                                    "V36 LIKELY MISS: %s defense %.0f, expected destiny %.1f — probably won't hit!",
                                                    targetTitle, defenseValue, expectedTotal), -150.0f);
                                                logger.warn("V36 WEAPON TARGET: {} defense {} vs expected {} — LIKELY MISS",
                                                    targetTitle, (int)defenseValue, String.format("%.1f", expectedTotal));
                                            }

                                            // === V36: PRIORITY TARGETS ===
                                            // 1. Game-text cancelers (Padme cancels Vader!) — MUST REMOVE
                                            if (targetLower.contains("padme") || targetLower.contains("naberrie")) {
                                                action.addReasoning("V36 PRIORITY: Padme cancels Vader's game text — REMOVE HER!", 300.0f);
                                            }

                                            // 2. Characters that add battle destiny (Lando Scoundrel, etc.)
                                            // These draw extra destiny = extra attrition damage
                                            if (targetLower.contains("lando") || targetLower.contains("boba fett")
                                                || targetLower.contains("wedge") || targetLower.contains("chewie")) {
                                                action.addReasoning("V36 PRIORITY: Battle destiny adder — dangerous!", 100.0f);
                                            }

                                            // 3. Jedi/Padawan — Hunt Down bonus for killing them
                                            if (isJediOrPadawan(targetLower)) {
                                                action.addReasoning("V36 HUNT: Jedi/Padawan target — Hunt Down bonus!", 80.0f);
                                            }

                                        } catch (Exception e) {
                                            logger.debug("V36 WEAPON TARGET: Error calculating hit probability: {}", e.getMessage());
                                        }
                                    } else {
                                        // Not in battle — basic targeting
                                        if (blueprint.hasPowerAttribute()) {
                                            Float power = blueprint.getPower();
                                            if (power != null && power >= 5) {
                                                action.addReasoning("High-power target", 30.0f);
                                            }
                                        }
                                    }

                                    if (blueprint.getUniqueness() == Uniqueness.UNIQUE) {
                                        action.addReasoning("Unique target", 20.0f);
                                    }
                                }
                            } else {
                                // V38.3: HARD BLOCK targeting own cards with harmful effects!
                                // Force Lightning on own Vader, weapon fire at own characters, etc.
                                // -200 wasn't enough — other bonuses could override it.
                                action.addReasoning("V38.3 SELF-TARGET: NEVER target own card with harmful effect!", -9999.0f);
                                logger.warn("V38.3 SELF-TARGET BLOCKED: Harmful effect targeting own card!");
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Location selection - pick battlegrounds and force icon locations.
     */
    private List<EvaluatedAction> evaluateLocationSelection(DecisionContext context) {
        return evaluateDeployLocation(context);  // Same logic
    }

    /**
     * V43: Starting interrupt selection.
     * Prefer interrupts that deploy the Epic Event ("Force Is Strong In My Family")
     * over generic starting interrupts like "The Signal".
     */
    private List<EvaluatedAction> evaluateStartingInterrupt(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> cardIds = context.getCardIds();
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());
        GameState gameState = context.getGameState();

        logger.warn("V43 STARTING INTERRUPT: Evaluating {} choices", cardIds != null ? cardIds.size() : 0);

        if (cardIds == null) return actions;

        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            EvaluatedAction action = new EvaluatedAction(cardId, ActionType.UNKNOWN, 50.0f, "Starting interrupt candidate");

            try {
                SwccgCardBlueprint blueprint = null;
                String title = "?";

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    blueprint = getBlueprintFromId(context, blueprintIds.get(idx));
                } else if (gameState != null) {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) blueprint = card.getBlueprint();
                }

                if (blueprint != null) {
                    title = blueprint.getTitle() != null ? blueprint.getTitle() : "?";
                    String gameText = blueprint.getGameText() != null ? blueprint.getGameText().toLowerCase(java.util.Locale.ROOT) : "";

                    // HARD PREFER: interrupts that deploy or reference the Epic Event
                    if (gameText.contains("force is strong in my family")
                        || gameText.contains("force is strong")
                        || gameText.contains("epic")) {
                        action.addReasoning("V43 EPIC EVENT: Deploys saga Epic Event — MUST USE THIS!", 1500.0f);
                        logger.warn("V43 STARTING INTERRUPT: {} references Epic Event — HARD PREFER (+1500)", title);
                    } else {
                        // V43 UPDATED 2026-07-07 (take 2 — the first scan looked for "from reserve
                        // deck", a phrase NEITHER card contains; read the actual cards this time):
                        // Prepared Defenses STARTING = "Deploy up to three Effects if each of them
                        // deploys for free..." (Card9_139). Surface Defense (V) STARTING = "draw up
                        // to 12 cards instead of 8" (Card200_125) — deploys NOTHING at setup.
                        // Discriminator: the STARTING clause deploys Effects → prefer, scaled by count.
                        String startingClause = gameText;
                        int sIdx = gameText.indexOf("starting:");
                        if (sIdx >= 0) startingClause = gameText.substring(sIdx);
                        boolean startingDeploysEffects = startingClause.contains("deploy")
                            && startingClause.contains("effect");
                        if (startingDeploysEffects) {
                            float effectBonus = 200.0f;
                            if (startingClause.contains("three effects") || startingClause.contains("3 effects")) effectBonus = 300.0f;
                            else if (startingClause.contains("two effects") || startingClause.contains("2 effects")) effectBonus = 250.0f;
                            action.addReasoning("V43 EFFECT-DEPLOYER: STARTING clause deploys Effects", effectBonus);
                            logger.warn("V43 STARTING INTERRUPT: {} deploys Effects at setup — PREFER (+{})", title, effectBonus);
                        } else if (startingClause.contains("deploy")) {
                            action.addReasoning("V43: STARTING clause deploys something", 100.0f);
                            logger.warn("V43 STARTING INTERRUPT: {} deploys at setup — mild prefer (+100)", title);
                        } else {
                            action.addReasoning("V43: Generic starting interrupt — no Epic Event", 0.0f);
                            logger.warn("V43 STARTING INTERRUPT: {} is generic (no Epic Event reference)", title);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V43 STARTING INTERRUPT: Error evaluating card {}: {}", cardId, e.getMessage());
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Starting location selection.
     * V22: Objective-aware + bonus for locations that pull from reserve deck.
     * Base +50 only if the location is mentioned in the starting interrupt's text.
     */
    private List<EvaluatedAction> evaluateStartingLocation(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer startLocObjAnalyzer =
            context.getObjectiveAnalyzer();

        // V22: Get the decision text which should reference the starting interrupt
        String decisionText = context.getDecisionText();
        String decisionTextLower = decisionText != null ? decisionText.toLowerCase(java.util.Locale.ROOT) : "";

        // V28: Get blueprint IDs for ARBITRARY_CARDS decisions (temp IDs can't be parsed as ints)
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());

        List<String> cardIds = context.getCardIds();
        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                10.0f,
                "Starting location " + cardId
            );

            // V22/V28: Look up the card to check game text and title
            // V28: For ARBITRARY_CARDS, card IDs are "temp0" etc. — use blueprint lookup instead.
            try {
                String locTitle = null;
                String locTitleLower = "";
                SwccgCardBlueprint locBp = null;

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    // V28: ARBITRARY_CARDS path — look up by blueprint ID
                    String bpId = blueprintIds.get(idx);
                    locBp = getBlueprintFromId(context, bpId);
                    if (locBp != null) {
                        locTitle = locBp.getTitle();
                        locTitleLower = locTitle != null ? locTitle.toLowerCase(java.util.Locale.ROOT) : "";
                        logger.warn("V28 ARBITRARY_CARDS: Resolved card '{}' via blueprint '{}' → '{}'", cardId, bpId, locTitle);
                    } else {
                        logger.warn("V28 ARBITRARY_CARDS: Could not resolve blueprint '{}' for card '{}'", bpId, cardId);
                    }
                } else if (gameState != null) {
                    // Standard path — look up by integer card ID
                    PhysicalCard locCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (locCard != null) {
                        locTitle = locCard.getTitle();
                        locTitleLower = locTitle != null ? locTitle.toLowerCase(java.util.Locale.ROOT) : "";
                        locBp = locCard.getBlueprint();
                    }
                }

                if (locTitle != null && locBp != null) {
                    // V22: +50 base ONLY if this location is mentioned in the starting interrupt
                    if (decisionTextLower.contains(locTitleLower)) {
                        action.addReasoning("V22 MENTIONED IN STARTING INTERRUPT", 50.0f);
                        logger.warn("V22 STARTING LOC: {} is mentioned in interrupt text (+50)", locTitle);
                    }

                    // V22: Objective-relevant starting location gets big boost
                    if (startLocObjAnalyzer != null && startLocObjAnalyzer.isAnalyzed()) {
                        if (startLocObjAnalyzer.isObjectiveRelevantLocation(locTitle)) {
                            float objBonus = startLocObjAnalyzer.getLocationObjectiveBonus(locTitle);
                            action.addReasoning("V22 OBJECTIVE STARTING LOCATION: " + locTitle, objBonus);
                            logger.warn("V22 STARTING LOC: {} is objective-relevant (+{})", locTitle, objBonus);
                        }
                    }

                    // === V24.10: EXTERIOR CC SITE MUST BE STARTING LOCATION ===
                    if (locTitleLower.contains("cloud city")) {
                        boolean isExterior = locBp.hasIcon(com.gempukku.swccgo.common.Icon.EXTERIOR_SITE);
                        boolean isInterior = locBp.hasIcon(com.gempukku.swccgo.common.Icon.INTERIOR_SITE);
                        if (isExterior && !isInterior) {
                            action.addReasoning("V24.10 EXTERIOR CC STARTING LOCATION: Only way to deploy — I'm Sorry can't pull this!", 500.0f);
                            logger.warn("V24.10 STARTING LOC: {} is EXTERIOR — HARD PREFER as starting location (+500)", locTitle);
                        } else if (isInterior) {
                            action.addReasoning("V24.10 INTERIOR CC: Slip Sliding or I'm Sorry will pull this — save starting slot for exterior!", -500.0f);
                            logger.warn("V24.10 STARTING LOC: {} is INTERIOR — HARD BLOCK as starting location (-500)", locTitle);
                        }
                    }

                    // V22: Starting locations that pull cards from Reserve Deck
                    // V71 (Steve, 2026-05-15): For LOCATIONS, the base getGameText() often
                    // returns empty. The actual game text lives in
                    // getLocationLightSideGameText() and getLocationDarkSideGameText().
                    // Concat ALL three so keyword checks (reserve / epic / force gen /
                    // battleground) work for locations regardless of which side stores
                    // the text. This fixes the Ajan Kloss bug: its "Epic Event" mention
                    // is in the Light Side text, which V29.14 was missing.
                    String locBaseText = locBp.getGameText();
                    String locLightText = null;
                    String locDarkText = null;
                    try { locLightText = locBp.getLocationLightSideGameText(); } catch (Exception ignored) { }
                    try { locDarkText = locBp.getLocationDarkSideGameText(); } catch (Exception ignored) { }
                    StringBuilder locAllSb = new StringBuilder();
                    if (locBaseText != null) locAllSb.append(locBaseText).append(' ');
                    if (locLightText != null) locAllSb.append(locLightText).append(' ');
                    if (locDarkText != null) locAllSb.append(locDarkText).append(' ');
                    String locTextLower = locAllSb.toString().toLowerCase(java.util.Locale.ROOT);
                    if (!locTextLower.isEmpty()) {
                        if (locTextLower.contains("reserve")) {
                            action.addReasoning("V22 RESERVE PULL: starting location pulls from reserve deck", 75.0f);
                            logger.warn("V22 STARTING LOC: {} pulls from Reserve Deck (+75)", locTitle);
                        }
                        if (locTextLower.contains("force generation") || locTextLower.contains("force icon")
                            || locTextLower.contains("adds one to")) {
                            action.addReasoning("V22 FORCE GEN: starting location boosts force", 25.0f);
                            logger.warn("V22 STARTING LOC: {} boosts force generation (+25)", locTitle);
                        }

                        // === V29.14: EPIC EVENT STARTING LOCATION ===
                        // Locations whose game text mentions "epic" (e.g. Epic Event pull)
                        // are critical starting locations — without starting here the deck
                        // cannot pull its key starting effects.
                        // V71: now scans Light/Dark side texts (Ajan Kloss text was missed).
                        if (locTextLower.contains("epic")) {
                            action.addReasoning("V29.14 EPIC EVENT: game text mentions 'epic' — critical starting location!", 1000.0f);
                            logger.warn("V29.14 STARTING LOC: {} mentions 'epic' in game text — HARD PREFER (+1000)", locTitle);
                        }
                    }

                    // === V29.14: FUNERAL PYRE TITLE CHECK ===
                    // Belt-and-suspenders: also check card title for "Funeral Pyre"
                    // This is a key starting location for Luke Saga decks.
                    if (locTitleLower.contains("funeral pyre")) {
                        action.addReasoning("V29.14 FUNERAL PYRE: critical starting location for Luke Saga!", 1000.0f);
                        logger.warn("V29.14 STARTING LOC: {} title contains 'Funeral Pyre' — HARD PREFER (+1000)", locTitle);
                    }

                    // === V67o BATTLEGROUND STARTING LOCATION ===
                    // Steve's rule: starting location should be a BATTLEGROUND so force
                    // drains and battles can happen there from turn 1. Without this rule
                    // Rando picks non-battleground sites (e.g., Dooku deck starts at a
                    // non-BG site) and loses tempo from turn 1.
                    //
                    // Detection heuristic (matches V29.6 in the deploy path):
                    //   1. Game text contains "battleground"
                    //   2. Title contains "battleground"
                    //   3. Site has BOTH Light Force AND Dark Force icons
                    //      (most battlegrounds have both — drainable + drainable-against)
                    //
                    // Score: +300 for battleground, -150 for non-battleground.
                    // Below Funeral Pyre/Epic Event (+1000) and CC Exterior (+500) so
                    // those specific overrides still win; above Force Gen (+25), Reserve
                    // Pull (+75), and Mention-in-Interrupt (+50) so battleground wins
                    // when no specific override applies.
                    boolean v67oIsBg = false;
                    String v67oReason = null;
                    String v67oGt = locBp.getGameText();
                    if (v67oGt != null && v67oGt.toLowerCase(java.util.Locale.ROOT).contains("battleground")) {
                        v67oIsBg = true;
                        v67oReason = "game text contains 'battleground'";
                    } else if (locTitleLower.contains("battleground")) {
                        v67oIsBg = true;
                        v67oReason = "title contains 'battleground'";
                    } else {
                        try {
                            if (locBp.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE)
                                && locBp.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)) {
                                v67oIsBg = true;
                                v67oReason = "site has both LIGHT and DARK force icons";
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    if (v67oIsBg) {
                        action.addReasoning("V67o BATTLEGROUND STARTING LOC: " + locTitle
                            + " is a battleground (" + v67oReason
                            + ") — drains and battles from turn 1!", 300.0f);
                        logger.warn("V67o STARTING LOC: {} is BATTLEGROUND ({}) → +300", locTitle, v67oReason);
                    } else {
                        action.addReasoning("V67o NON-BATTLEGROUND STARTING LOC: " + locTitle
                            + " — no force drains/battles possible here, prefer battleground!",
                            -150.0f);
                        logger.warn("V67o STARTING LOC: {} is NON-BATTLEGROUND → -150", locTitle);
                    }

                    // === V67q SITH DECK SPECIFIC TIGHTENING ===
                    // Steve's Dooku deck uses Rise Of The Sith / Revenge Of The Sith.
                    // Those starting Effects only function at a NON-PALACE battleground.
                    // If the deck has either of those cards anywhere in the player's
                    // pool (hand, reserve, used/lost/force pile, in-play, stacked, etc.),
                    // tighten the starting-location preference:
                    //   - Non-Palace battleground: +600 ADDITIONAL (net ~+900 with V67o)
                    //   - Palace battleground:     -350 ADDITIONAL (net ~-50 — discouraged)
                    //   - Non-battleground:        -300 ADDITIONAL (net ~-450)
                    // This mirrors a previous K-2 session's design (lost when their session
                    // ended without committing).
                    boolean v67qHasSithStart = false;
                    try {
                        String gsPlayerId = context.getPlayerId();
                        if (gameState != null && gsPlayerId != null) {
                            String[] sithMarkers = new String[] {
                                "rise of the sith", "revenge of the sith"
                            };
                            // Scan every zone the card could be in
                            java.util.List<PhysicalCard> v67qScanCards = new java.util.ArrayList<>();
                            try { v67qScanCards.addAll(gameState.getHand(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getReserveDeck(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getForcePile(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getUsedPile(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getLostPile(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getOutOfPlayPile(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getOutsideOfDeck(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getSideOfTableFaceDown(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getVoid(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            try { v67qScanCards.addAll(gameState.getCardsRevealedAfterStartingEffect(gsPlayerId)); } catch (Exception e) { /* ignore */ }
                            // V67x (Steve, 2026-05-03): getAllPermanentCards() returns BOTH players'
                            // cards. If the OPPONENT plays Sith (Rise/Revenge Of The Sith), V67q
                            // wrongly fired for ME — adding +600 to Tatooine and -300 to Funeral
                            // Pyre, causing me to pick Tatooine as my Light-side starting location.
                            // Filter to only my cards.
                            try {
                                for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                    if (pc != null && gsPlayerId.equals(pc.getOwner())) {
                                        v67qScanCards.add(pc);
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                            for (PhysicalCard sc : v67qScanCards) {
                                if (sc == null) continue;
                                // V67x: defense in depth — also filter individual zone cards
                                // in case a zone returns opponent-owned cards (shouldn't happen
                                // with playerId-scoped getters, but guards against future regressions).
                                if (sc.getOwner() != null && !gsPlayerId.equals(sc.getOwner())) continue;
                                String t = sc.getTitle();
                                if (t == null) continue;
                                String tl = t.toLowerCase(java.util.Locale.ROOT);
                                for (String marker : sithMarkers) {
                                    if (tl.contains(marker)) {
                                        v67qHasSithStart = true;
                                        break;
                                    }
                                }
                                if (v67qHasSithStart) break;
                            }
                        }
                    } catch (Exception e) { logger.debug("V67q scan error: {}", e.getMessage()); }

                    if (v67qHasSithStart) {
                        boolean v67qIsPalace = locTitleLower.contains("palace");
                        if (v67oIsBg && !v67qIsPalace) {
                            action.addReasoning("V67q SITH START (RotS/RevotS): " + locTitle
                                + " is non-Palace battleground — starting Effect WILL trigger here!",
                                600.0f);
                            logger.warn("V67q SITH START: {} non-Palace BG → +600 (RotS/RevotS triggers)", locTitle);
                        } else if (v67qIsPalace) {
                            action.addReasoning("V67q SITH START PALACE: " + locTitle
                                + " is a Palace — RotS/RevotS Effect WON'T trigger here, avoid!",
                                -350.0f);
                            logger.warn("V67q SITH START: {} is PALACE → -350 (Effect won't trigger)", locTitle);
                        } else {
                            action.addReasoning("V67q SITH START NON-BG: " + locTitle
                                + " is not a battleground — RotS/RevotS Effect cannot trigger!",
                                -300.0f);
                            logger.warn("V67q SITH START: {} non-BG → -300 (Effect cannot trigger)", locTitle);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("V28 STARTING LOC: Error looking up card {}: {}", cardId, e.getMessage());
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Take into hand - prefer high-value cards.
     * Handles both in-play cards (by card ID) and reserve deck cards (by blueprint).
     * CRITICAL: Only score selectable cards - GEMP rejects non-selectable selections!
     */
    private List<EvaluatedAction> evaluateTakeIntoHand(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<String> testingTexts = context.getTestingTexts();  // CARD TITLES from GEMP!

        logger.info("🔍 evaluateTakeIntoHand: {} cards, {} blueprints, {} testingTexts",
                   cardIds != null ? cardIds.size() : 0,
                   blueprints != null ? blueprints.size() : 0,
                   testingTexts != null ? testingTexts.size() : 0);

        for (int i = 0; i < cardIds.size(); i++) {
            String cardId = cardIds.get(i);
            String blueprintId = (blueprints != null && i < blueprints.size()) ? blueprints.get(i) : null;

            // LOOK UP CARD NAME FROM BLUEPRINT LIBRARY - this PROVES we can identify cards!
            String cardTitle = null;
            SwccgCardBlueprint blueprint = null;

            // Method 1: For regular cardIds, look up the card in game state
            if (gameState != null && cardId != null && !cardId.startsWith("temp")) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        cardTitle = card.getTitle();
                        blueprint = card.getBlueprint();
                        logger.info("✅ CARD LOOKUP[{}]: cardId={} -> '{}'", i, cardId, cardTitle);
                    }
                } catch (NumberFormatException e) {
                    // Card ID is not a number - expected for temp IDs
                }
            }

            // Method 2: For temp IDs or if Method 1 failed, look up from blueprintId in library
            if (cardTitle == null && blueprintId != null && !blueprintId.isEmpty() && !"inPlay".equals(blueprintId)) {
                cardTitle = getCardNameFromBlueprint(context, blueprintId);
                if (cardTitle != null) {
                    blueprint = getBlueprintFromId(context, blueprintId);
                }
            }

            // Fallback: use blueprintId as display name if we still don't have a title
            if (cardTitle == null) {
                cardTitle = (blueprintId != null && !blueprintId.isEmpty()) ? "bp=" + blueprintId : cardId;
                logger.warn("⚠️ Could not look up card name for [{}]: cardId={}, bp={}", i, cardId, blueprintId);
            }

            // CRITICAL: Skip non-selectable cards! But still log the REAL card name
            if (!isCardSelectable(context, i)) {
                logger.info("⚠️ Skipping non-selectable[{}]: '{}' (cardId={}, bp={})", i, cardTitle, cardId, blueprintId);
                continue;
            }

            // Now we have card info
            Float destiny = null;
            Float power = null;
            Float ability = null;
            CardCategory category = null;

            // Log the final card title we determined
            logger.info("📋 evaluateTakeIntoHand[{}]: cardId='{}', blueprintId='{}', TITLE='{}'",
                i, cardId, blueprintId, cardTitle);

            // Extract card properties from blueprint (if we have one)
            if (blueprint != null) {
                try {
                    destiny = blueprint.getDestiny();
                } catch (UnsupportedOperationException e) {
                    // Card type doesn't support destiny
                }
                if (blueprint.hasPowerAttribute()) {
                    power = blueprint.getPower();
                }
                if (blueprint.hasAbilityAttribute()) {
                    ability = blueprint.getAbility();
                }
                category = blueprint.getCardCategory();
            }

            // Check for priority cards by blueprintId if we have one but couldn't get card info
            boolean isPriorityByBlueprint = false;
            int priorityScoreByBlueprint = 0;
            if (blueprintId != null) {
                isPriorityByBlueprint = AiPriorityCards.isPriorityCard(blueprintId);
                priorityScoreByBlueprint = AiPriorityCards.getProtectionScore(blueprintId);
            }

            // Create action with proper display text
            float baseScore = 50.0f;
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.SELECT_CARD,
                baseScore,
                "Take " + cardTitle + " into hand"
            );
            action.setCardName(cardTitle);
            if (blueprintId != null) {
                action.setBlueprintId(blueprintId);
            }

            // === SCORING LOGIC ===

            // High destiny cards are VERY valuable - they're used for destiny draws
            if (destiny != null) {
                if (destiny >= 6) {
                    action.addReasoning("Excellent destiny (" + destiny + ")", 60.0f);
                } else if (destiny >= 5) {
                    action.addReasoning("High destiny (" + destiny + ")", 40.0f);
                } else if (destiny >= 4) {
                    action.addReasoning("Good destiny (" + destiny + ")", 20.0f);
                } else if (destiny >= 3) {
                    action.addReasoning("Decent destiny (" + destiny + ")", 5.0f);
                } else if (destiny <= 1) {
                    action.addReasoning("Low destiny (" + destiny + ")", -20.0f);
                }
            }

            // Priority cards (Houjix, Sense, etc.) are always good
            // Check by title first (when we have the actual card)
            if (!cardTitle.equals("Unknown") && !cardTitle.startsWith("Card ") &&
                AiPriorityCards.isPriorityCardByTitle(cardTitle)) {
                int priorityScore = AiPriorityCards.getProtectionScoreByTitle(cardTitle);
                action.addReasoning("Priority card: " + cardTitle, priorityScore * 0.5f);
            } else if (isPriorityByBlueprint) {
                // Check by blueprint ID (when we only have the blueprintId)
                action.addReasoning("Priority card (by ID: " + blueprintId + ")", priorityScoreByBlueprint * 0.5f);
            }

            // Prefer characters with high power
            if (category == CardCategory.CHARACTER && power != null) {
                if (power >= 6) {
                    action.addReasoning("High power character (" + power + ")", 30.0f);
                } else if (power >= 4) {
                    action.addReasoning("Strong character (" + power + ")", 15.0f);
                }
            }

            // Prefer characters with high ability (can draw battle destiny)
            if (category == CardCategory.CHARACTER && ability != null && ability >= 4) {
                action.addReasoning("High ability (" + ability + ") - draws battle destiny", 25.0f);
            }

            // Locations are often good early game
            if (category == CardCategory.LOCATION) {
                int turnNumber = context.getTurnNumber();
                if (turnNumber <= 3) {
                    action.addReasoning("Location (good early game)", 20.0f);
                } else {
                    action.addReasoning("Location", 5.0f);
                }
            }

            // === V22.6: UNIVERSAL LOCATION PRIORITY FOR OBJECTIVE PULLS ===
            // When an objective offers multiple cards to pull from the reserve deck,
            // locations (systems, sites, sectors) should ALWAYS be pulled first.
            // Locations are prerequisites — effects and characters deploy ON locations,
            // so without the location on table first, those other pulls are wasted.
            // Example: Bespin system must be pulled before Alert My Star Destroyer,
            // because AMSD deploys on Bespin system.
            // This is universal for ALL objectives, not just Bespin-related ones.
            // If the location is NOT among the reserve deck options, the game engine
            // already filtered it out (it's in hand, on table, or not in deck), so
            // pulling other cards is fine — no extra hand/table checks needed here.
            if (category == CardCategory.LOCATION) {
                action.addReasoning("V22.6 LOCATION PRIORITY: locations are prerequisites — pull before effects/characters", 500.0f);
                logger.warn("🌍 V22.6 LOCATION PRIORITY: {} gets +500 (always pull locations first from objective)", cardTitle);
            }

            // === V22.6: FAILED PULL AVOIDANCE (DeckOracle) ===
            // If we've tried to pull this card 2+ times and failed, it's likely not in the
            // reserve deck. Stop wasting actions trying to pull it.
            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle = context.getDeckOracle();
            if (oracle != null && blueprintId != null && oracle.shouldAvoidPulling(blueprintId)) {
                action.addReasoning("V22.6 FAILED PULL: tried 2+ times, card likely unavailable — skipping", -500.0f);
                logger.warn("📚 V22.6 FAILED PULL BLOCK: {} has failed 2+ pull attempts — score crushed (-500)", cardTitle);
            } else if (oracle != null && cardTitle != null && oracle.shouldAvoidPullingByTitle(cardTitle)) {
                action.addReasoning("V22.6 FAILED PULL (by title): tried 2+ times, card likely unavailable", -500.0f);
                logger.warn("📚 V22.6 FAILED PULL BLOCK: '{}' has failed 2+ pull attempts by title — score crushed (-500)", cardTitle);
            }

            // === V24.1: CARD-SPECIFIC PULL PREFERENCES (TDIGWATT) ===
            // These priorities ensure the correct TDIGWATT setup sequence:
            // Endor Shield → Piett first (matches Executor for AMSD)
            // Piett → Gherant first (deploys an Executor site = free location)
            String pullDecisionText = context.getDecisionText() != null ?
                context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
            String cardTitleLower = cardTitle != null ? cardTitle.toLowerCase(java.util.Locale.ROOT) : "";

            // V24.1A: Endor Shield admiral pull — Piett first, Chiraneau backup
            // V24.12: GEMP decision text is just "Choose card to take into hand" — no "admiral"
            // in it. So also detect admiral pulls by checking if this card IS an admiral.
            // The Endor Shield action restricts choices to admirals, so if Piett/Chiraneau
            // are among the options, we know it's an admiral pull.
            boolean isAdmiralPull = (pullDecisionText.contains("admiral") && pullDecisionText.contains("reserve"));
            // V24.12: GEMP text is generic — detect admiral pulls by known admiral names in card title
            if (!isAdmiralPull) {
                if (cardTitleLower.contains("admiral") || cardTitleLower.contains("piett")
                    || cardTitleLower.contains("chiraneau") || cardTitleLower.contains("ozzel")
                    || cardTitleLower.contains("motti") || cardTitleLower.contains("firmus")) {
                    isAdmiralPull = true;
                }
            }
            if (isAdmiralPull) {
                if (cardTitleLower.contains("piett")) {
                    action.addReasoning("V24.12 ADMIRAL PULL: Piett is #1 pick — matches Executor for AMSD!", 300.0f);
                    logger.warn("V24.12 ADMIRAL PULL: Piett gets +300 — best AMSD pilot for Executor!");
                } else if (cardTitleLower.contains("chiraneau")) {
                    action.addReasoning("V24.12 ADMIRAL PULL: Chiraneau is backup — can pilot Executor manually", 150.0f);
                    logger.warn("V24.12 ADMIRAL PULL: Chiraneau gets +150 — backup pilot");
                } else if (cardTitleLower.contains("ozzel")) {
                    action.addReasoning("V24.12 ADMIRAL PULL: Ozzel matches Executor — decent AMSD option", 100.0f);
                    logger.warn("V24.12 ADMIRAL PULL: Ozzel gets +100");
                }
            }

            // V24.1B: Piett's commander pull — Gherant first (pulls Executor site = free location)
            if ((pullDecisionText.contains("commander") || pullDecisionText.contains("admiral's order")) &&
                pullDecisionText.contains("reserve")) {
                if (cardTitleLower.contains("gherant")) {
                    action.addReasoning("V24.1 PIETT PULL: Gherant deploys an Executor site — free location + force generation!", 400.0f);
                    logger.warn("V24.1 COMMANDER PULL: Gherant gets +400 — pulls Executor site on deploy!");
                }
            }

            // === V24.2B: LANDO/LOBOT PULL PRIORITY (TDIGWATT) ===
            // Lando and Lobot are key to flipping the TDIGWATT objective.
            // Lando can move to unoccupied CC sites at start of control phase = 3-site drains.
            // Both deploy cheap. Prioritize pulling them from reserve when available.
            // V47: BUT don't pull Lando if he'd be alone at CC — he gets clobbered!
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer pullObjAnalyzer =
                context.getObjectiveAnalyzer();
            if (pullObjAnalyzer != null && pullObjAnalyzer.isAnalyzed()
                && pullObjAnalyzer.needsBespinSystemPresence()) {
                if (cardTitleLower.contains("lando")) {
                    // V47: Check if we have ANY friendly characters at Cloud City sites
                    boolean friendlyAtCC = false;
                    String pullPlayerId = context.getPlayerId();
                    if (game != null && gameState != null && pullPlayerId != null) {
                        try {
                            for (PhysicalCard checkLoc : gameState.getLocationsInOrder()) {
                                if (checkLoc == null || checkLoc.getTitle() == null) continue;
                                String checkLocLower = checkLoc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                boolean isCCsite = checkLocLower.contains("cloud city") || checkLocLower.contains("upper walkway")
                                    || checkLocLower.contains("carbonite") || checkLocLower.contains("security tower")
                                    || checkLocLower.contains("dining room") || checkLocLower.contains("platform")
                                    || checkLocLower.contains("lower corridor");
                                if (!isCCsite) continue;
                                java.util.List<PhysicalCard> siteCards = gameState.getCardsAtLocation(checkLoc);
                                if (siteCards != null) {
                                    for (PhysicalCard sc : siteCards) {
                                        if (sc != null && pullPlayerId.equals(sc.getOwner()) && sc.getBlueprint() != null
                                            && sc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            friendlyAtCC = true;
                                            break;
                                        }
                                    }
                                }
                                if (friendlyAtCC) break;
                            }
                        } catch (Exception e) {
                            logger.debug("V47: Error checking CC friendlies: {}", e.getMessage());
                        }
                    }

                    // V47: Also check if we have chars in hand + enough force to deploy both
                    boolean hasHandBuddy = false;
                    int forceAvailable = 0;
                    if (!friendlyAtCC && game != null && gameState != null) {
                        try {
                            forceAvailable = context.getForcePileSize();
                            java.util.List<PhysicalCard> hand = gameState.getHand(pullPlayerId);
                            if (hand != null) {
                                for (PhysicalCard hc : hand) {
                                    if (hc != null && hc.getBlueprint() != null
                                        && hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        hasHandBuddy = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("V47: Error checking hand chars: {}", e.getMessage());
                        }
                    }

                    if (friendlyAtCC) {
                        action.addReasoning("V24.2 TDIGWATT: Lando is KEY — moves to 3rd CC site for extra drains + occupation!", 250.0f);
                        logger.warn("V24.2 PULL: Lando gets +250 — friendlies at CC, safe to pull!");
                    } else if (hasHandBuddy && forceAvailable >= 5) {
                        // Have a buddy in hand and enough force to deploy Lando (~2) + buddy (~3)
                        action.addReasoning("V47 LANDO PULL OK: No CC friendlies but have char in hand + force to deploy both!", 250.0f);
                        logger.warn("V47 LANDO PULL: {} force available + char in hand — OK to pull Lando with buddy!", forceAvailable);
                    } else {
                        action.addReasoning("V47 LANDO PULL BLOCK: No friendlies at CC, no buddy in hand or not enough force — Lando would die alone!", -9999.0f);
                        logger.warn("V47 LANDO PULL BLOCK: No CC friendlies, handBuddy={}, force={} — don't pull Lando!", hasHandBuddy, forceAvailable);
                    }
                } else if (cardTitleLower.contains("lobot")) {
                    action.addReasoning("V24.2 TDIGWATT: Lobot deploys cheap — helps flip objective!", 200.0f);
                    logger.warn("V24.2 PULL: Lobot gets +200 — cheap deploy, helps flip!");
                }
            }

            // === DOWNLOAD-ENABLER PULL PRIORITY (Steve, 2026-05-31) ===
            // Universal text-scan: when pulling from Reserve, prefer cards whose
            // game text contains "[download]" of a LOCATION target (site / location
            // / system / battleground). These are deploy-enablers — once on the
            // table, their [download] fetches MORE locations from Reserve, building
            // a value chain that puts 2-3 locations on table per turn for almost
            // no Force. Examples (none hardcoded — all detected by text):
            //   • Vigo (200_91): "may use 1 Force to [download] a non-war room
            //     battleground planet site (or system) not already on table" —
            //     turn-1 Coruscant: Xizor's Palace pull.
            //   • Coruscant: Xizor's Palace: "may [download] a Xizor's Palace
            //     site" — fetches Sewer / Uplink Station once on table.
            //   • Shadows Of The Empire: "may use 1 Force to [download] Imperial
            //     Square".
            // Detection mirrors the V71 location-text pattern at line 6337:
            // concat base + light-side + dark-side game text since LOCATION cards
            // store their text in the side-specific fields. The "[download]" token
            // is the canonical Reserve-pull marker. Magnitude +500 — on par with
            // the strongest TDIGWATT-specific pull boosts (V24.1 Gherant +400),
            // because the chain compounds: each enabler pull sets up 2-3 future
            // location deploys. Appended into existing V60/V24 take-into-hand
            // logic, no new V-tag per Steve's "avoid splintering" directive.
            if (blueprint != null) {
                StringBuilder enablerSb = new StringBuilder();
                try { String gt = blueprint.getGameText(); if (gt != null) enablerSb.append(gt).append(' '); } catch (Exception ignored) { }
                try { String lt = blueprint.getLocationLightSideGameText(); if (lt != null) enablerSb.append(lt).append(' '); } catch (Exception ignored) { }
                try { String dt = blueprint.getLocationDarkSideGameText(); if (dt != null) enablerSb.append(dt).append(' '); } catch (Exception ignored) { }
                String enablerTextLower = enablerSb.toString().toLowerCase(java.util.Locale.ROOT);
                if (enablerTextLower.contains("[download]")
                        && (enablerTextLower.contains("site")
                            || enablerTextLower.contains("location")
                            || enablerTextLower.contains("system")
                            || enablerTextLower.contains("battleground"))) {
                    action.addReasoning(
                        "DOWNLOAD ENABLER: [download] of a location in game text — pulling builds a deploy chain",
                        500.0f);
                    logger.warn("DOWNLOAD ENABLER PULL: '{}' has location-[download] in game text → +500 (universal deploy-chain rule)",
                        cardTitle);
                }
            }

            // Log the decision
            logger.debug("🎯 {} ({}): score={}, destiny={}, power={}",
                        cardTitle, blueprintId != null ? blueprintId : cardId,
                        action.getScore(),
                        destiny != null ? destiny : "?",
                        power != null ? power : "?");

            actions.add(action);
        }

        // Sort by score descending for logging
        actions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (!actions.isEmpty()) {
            logger.info("✅ Best take into hand: {} (score: {})",
                       actions.get(0).getCardName(), actions.get(0).getScore());
        }

        return actions;
    }

    /**
     * Lost pile selection - prefer low-value cards.
     */
    private List<EvaluatedAction> evaluateLostPileSelection(DecisionContext context) {
        return evaluateForceLoss(context);  // Same logic
    }

    /**
     * Unknown decision type - neutral scoring with card name lookup.
     * CRITICAL: Only score selectable cards - GEMP rejects non-selectable selections!
     *
     * Ported from Python _evaluate_unknown() which scores based on card type.
     */
    private List<EvaluatedAction> evaluateUnknown(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<String> testingTexts = context.getTestingTexts();  // CARD TITLES from GEMP!

        // Determine base score - higher for gain/select decisions
        String textLower = context.getDecisionText() != null ? context.getDecisionText().toLowerCase(Locale.ROOT) : "";
        boolean isLossDecision = textLower.contains("lose") || textLower.contains("lost") ||
                                 textLower.contains("place in") || textLower.contains("put on");

        logger.info("🔍 evaluateUnknown: {} cards, {} blueprints, {} testingTexts for '{}' (loss={})",
                   cardIds != null ? cardIds.size() : 0,
                   blueprints != null ? blueprints.size() : 0,
                   testingTexts != null ? testingTexts.size() : 0,
                   context.getDecisionText(),
                   isLossDecision);

        int selectableCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < cardIds.size(); i++) {
            String cardId = cardIds.get(i);
            String blueprintId = (blueprints != null && i < blueprints.size()) ? blueprints.get(i) : null;

            // LOOK UP CARD NAME FROM BLUEPRINT LIBRARY - this PROVES we can identify cards!
            String cardTitle = null;
            SwccgCardBlueprint blueprint = null;

            // Method 1: For regular cardIds, look up the card in game state
            if (gameState != null && cardId != null && !cardId.startsWith("temp")) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        cardTitle = card.getTitle();
                        blueprint = card.getBlueprint();
                        logger.info("✅ CARD LOOKUP[{}]: cardId={} -> '{}'", i, cardId, cardTitle);
                    }
                } catch (NumberFormatException e) {
                    // Card ID is not a number - expected for temp IDs
                }
            }

            // Method 2: For temp IDs or if Method 1 failed, look up from blueprintId in library
            if (cardTitle == null && blueprintId != null && !blueprintId.isEmpty() && !"inPlay".equals(blueprintId)) {
                cardTitle = getCardNameFromBlueprint(context, blueprintId);
                if (cardTitle != null) {
                    blueprint = getBlueprintFromId(context, blueprintId);
                }
            }

            // Fallback: use blueprintId as display name if we still don't have a title
            if (cardTitle == null) {
                cardTitle = (blueprintId != null && !blueprintId.isEmpty()) ? "bp=" + blueprintId : cardId;
                logger.warn("⚠️ Could not look up card name for [{}]: cardId={}, bp={}", i, cardId, blueprintId);
            }

            // CRITICAL: Skip non-selectable cards! But still log the REAL card name
            if (!isCardSelectable(context, i)) {
                skippedCount++;
                logger.info("⚠️ Skipping non-selectable[{}]: '{}' (cardId={}, bp={})", i, cardTitle, cardId, blueprintId);
                continue;
            }
            selectableCount++;

            // Now we have card info
            Float destiny = null;
            Float power = null;
            CardCategory category = null;

            // Log the final card title we determined
            logger.info("📋 evaluateUnknown[{}]: cardId='{}', blueprintId='{}', TITLE='{}'",
                i, cardId, blueprintId, cardTitle);

            // Extract card properties from blueprint (if we have one)
            if (blueprint != null) {
                try {
                    destiny = blueprint.getDestiny();
                } catch (UnsupportedOperationException e) {
                    // Card type doesn't support destiny
                }
                if (blueprint.hasPowerAttribute()) {
                    power = blueprint.getPower();
                }
                category = blueprint.getCardCategory();
            }

            // Base score beats PassEvaluator (~5-20)
            float baseScore = 30.0f;
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                baseScore,
                "Select " + cardTitle
            );
            action.setCardName(cardTitle);
            if (blueprintId != null) {
                action.setBlueprintId(blueprintId);
            }

            // V70 (Steve, 2026-05-12): ONE-WEAPON-PER-CHARACTER rule in
            // evaluateUnknown path. The Dooku replay showed Asajj Ventress'
            // Lightsabers landing on already-armed Lord Sidious via Evil Is
            // Everywhere's reserve-deck pull. The decision text "Choose card
            // to deploy from Reserve Deck" doesn't match any specific dispatch
            // pattern, so the decision flows here. V70 in
            // evaluateReserveDeckSelection never fires for cardIds-populated
            // decisions. This block fixes that path.
            //
            // Per Steve's standing rule: "No character should ever have two
            // weapons." Same helper as in evaluateReserveDeckSelection — see
            // v70CheckWeaponDeviceBlock. Comprehensive criteria search across
            // title, lore, gametext, dynamic card types (engine-aware), icons,
            // keywords, persona.
            String v70UnkReason = v70CheckWeaponDeviceBlock(
                game, context.getPlayerId(), category, blueprint);
            if (v70UnkReason != null) {
                action.addReasoning(
                    "V70 NO 2ND WEAPON: " + v70UnkReason + " — '" + cardTitle + "'",
                    -9999.0f);
                logger.warn("V70 BLOCK (evaluateUnknown, {}): '{}' (bp {}) — {}",
                    category, cardTitle, blueprintId, v70UnkReason);
                actions.add(action);
                continue;
            }

            // === V112: BATTLE ORDER / BATTLE PLAN GATE (evaluateUnknown path) ===
            // When K&D plays Battle Order/Plan from stacked, the decision may route
            // through evaluateUnknown rather than evaluateDefensiveShieldSelection
            // (where V51 lives). Mirror the BG-occupation check here to close that gap.
            // Battle Order/Plan are only useful if Rando already occupies BOTH a
            // battleground site AND a battleground system simultaneously. Otherwise
            // Battle Order also costs Rando 1 Force per drain — net negative.
            if (cardTitle != null) {
                String v112TitleLower = cardTitle.toLowerCase(java.util.Locale.ROOT);
                if (v112TitleLower.contains("battle order") || v112TitleLower.contains("battle plan")) {
                    // V112 UPDATED 2026-07-06: engine occupies predicate (occupiesBothTheaters)
                    // replaces the hand-rolled loop (old loop commented out below per
                    // feedback_comment_out_old_rules) — same V140-class fix as V51.
                    if (!occupiesBothTheaters(context.getGame(), context.getPlayerId())) {
                        action.addReasoning("V112 BATTLE ORDER GATE: Need BOTH a BG site AND BG system occupied!", -9999.0f);
                        logger.warn("V112 BATTLE ORDER GATE: '{}' blocked — occupiesBothTheaters=false", cardTitle);
                    }
                    // OLD hand-rolled occupation loop (superseded 2026-07-06):
                    // boolean v112BGSite = false; boolean v112BGSystem = false;
                    // try {
                    //     GameState gs112 = context.getGameState(); SwccgGame g112 = context.getGame(); String pid112 = context.getPlayerId();
                    //     if (g112 != null && gs112 != null && pid112 != null) {
                    //         for (PhysicalCard loc112 : gs112.getAllPermanentCards()) {
                    //             if (loc112 == null || loc112.getBlueprint() == null) continue;
                    //             com.gempukku.swccgo.common.Zone lz112 = loc112.getZone();
                    //             if (lz112 == null || lz112 != com.gempukku.swccgo.common.Zone.LOCATIONS) continue;
                    //             boolean isBG112 = false;
                    //             try { isBG112 = g112.getModifiersQuerying().isBattleground(gs112, loc112, null); } catch (Exception e2) { }
                    //             if (!isBG112) continue;
                    //             boolean weOccupy112 = false;
                    //             for (PhysicalCard atLoc : gs112.getCardsAtLocation(loc112)) {
                    //                 if (atLoc == null || !pid112.equals(atLoc.getOwner())) continue;
                    //                 CardCategory cat112 = atLoc.getBlueprint() != null ? atLoc.getBlueprint().getCardCategory() : null;
                    //                 if (cat112 == CardCategory.CHARACTER || cat112 == CardCategory.STARSHIP || cat112 == CardCategory.VEHICLE) { weOccupy112 = true; break; }
                    //             }
                    //             if (weOccupy112) {
                    //                 com.gempukku.swccgo.common.CardSubtype sub112 = loc112.getBlueprint().getCardSubtype();
                    //                 if (sub112 == com.gempukku.swccgo.common.CardSubtype.SYSTEM) v112BGSystem = true;
                    //                 else if (sub112 == com.gempukku.swccgo.common.CardSubtype.SITE) v112BGSite = true;
                    //             }
                    //         }
                    //     }
                    // } catch (Exception e) { logger.debug("V112 BATTLE ORDER: Error checking occupation: {}", e.getMessage()); }
                    // if (!v112BGSite || !v112BGSystem) { action.addReasoning("V112 BATTLE ORDER GATE: ...", -9999.0f); ... }
                }
            }

            // === V117 (Steve, 2026-05-22): UNIVERSAL 4TH-SHIELD HARD BLOCK (evaluateUnknown) ===
            // Per Steve: "We need to hard block deploy from Knowledge and Defense effect
            // when 3 shields already on table. The conditions we set for that fourth
            // shield must be met before deploying."
            //
            // V105/V107 at line ~7100 already does this for the defensive-shield-selection
            // path. But K&D plays a card from a MIXED stacked pile (shields + non-shields)
            // route through evaluateUnknown when isShieldSelectionByContent() returns false
            // (<50% shields in stacked). V112 covered Battle Order/Plan specifically.
            // V117 closes the gap for ALL shields: when 3 defensive shields are already on
            // Rando's table, hard-block any 4th shield unless ShieldStrategy.prefers4thSlot()
            // returns this specific shield title (V105 Battle Order/Plan or V107 Resistance/
            // Ultimatum trigger active).
            if (category == CardCategory.DEFENSIVE_SHIELD
                    && cardTitle != null) {
                try {
                    GameState gs117 = context.getGameState();
                    String pid117 = context.getPlayerId();
                    int v117ShieldsOnTable = 0;
                    if (gs117 != null && pid117 != null) {
                        for (PhysicalCard sc : gs117.getAllPermanentCards()) {
                            if (sc == null || sc.getBlueprint() == null) continue;
                            if (!pid117.equals(sc.getOwner())) continue;
                            if (sc.getBlueprint().getCardCategory() != CardCategory.DEFENSIVE_SHIELD) continue;
                            com.gempukku.swccgo.common.Zone sz = sc.getZone();
                            if (sz == null || !sz.isInPlay()) continue;
                            v117ShieldsOnTable++;
                        }
                    }
                    if (v117ShieldsOnTable >= 3) {
                        // 4th-slot territory — consult ShieldStrategy to see if this exact
                        // shield is the V105/V107-preferred one. If not, hard-block.
                        ShieldStrategy v117Strat = context.getShieldStrategy();
                        String v117Preferred = null;
                        if (v117Strat != null) {
                            try {
                                v117Preferred = v117Strat.prefers4thSlot(gs117, context.getGame(), pid117);
                            } catch (Exception e) {
                                logger.debug("V117 prefers4thSlot error: {}", e.getMessage());
                            }
                        }
                        // V117 UPDATED 2026-07-06 (Verge twin of the V105 deadlock): only
                        // pursue the preferred card if it is actually on the menu this
                        // decision; else HOLD the slot closed instead of hard-blocking every
                        // real shield for a card that can't be picked.
                        if (v117Preferred == null || !preferredShieldInCandidates(context, v117Preferred)) {
                            action.addReasoning(
                                "V117 4TH SHIELD HOLD: " + v117ShieldsOnTable
                                    + " shields on table, no available preferred card — slot closed!",
                                -9999.0f);
                            logger.warn("V117 4TH SHIELD HOLD: '{}' blocked — {} shields on table, preferred={} not on menu / no trigger",
                                cardTitle, v117ShieldsOnTable, v117Preferred);
                        } else {
                            String v117tLower = cardTitle.toLowerCase(java.util.Locale.ROOT);
                            String v117pLower = v117Preferred.toLowerCase(java.util.Locale.ROOT);
                            if (!v117tLower.contains(v117pLower)) {
                                action.addReasoning(
                                    "V117 4TH SHIELD: '" + cardTitle + "' not preferred (we want '"
                                        + v117Preferred + "') — block",
                                    -9999.0f);
                                logger.warn("V117 4TH SHIELD: '{}' blocked — prefer '{}' instead",
                                    cardTitle, v117Preferred);
                            } else {
                                action.addReasoning(
                                    "V117 4TH SHIELD BOOST: matches preferred '" + v117Preferred + "' +2000",
                                    2000.0f);
                                logger.warn("V117 4TH SHIELD BOOST: '{}' matches preferred '{}' +2000",
                                    cardTitle, v117Preferred);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V117 shield-count check error: {}", e.getMessage());
                }
            }

            // === V22 STARTING EFFECTS: BAN + OBJECTIVE-AWARE PREFERENCE ===
            if (context.getTurnNumber() <= 0 && cardTitle != null) {
                String titleCheck = cardTitle.toLowerCase(java.util.Locale.ROOT);
                if (titleCheck.contains("no escape") || titleCheck.contains("coarse and rough")) {
                    action.addReasoning("V22 STARTING BAN: " + cardTitle + " banned!", -600.0f);
                    logger.warn("V22 STARTING BAN: Blocking {} (-600)", cardTitle);
                    actions.add(action);
                    continue;
                }
                // === V187 (Steve, 2026-06-28): DUPLICATE STARTING-EFFECT PENALTY ===
                // Don't burn the turn-0 effect slot on an effect Rando has more than one of — the
                // other copy is still drawable/deployable later, so spend the slot on a SINGLETON
                // for more table variety. DeckOracle counts copies by title across the whole deck.
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle v187Oracle = context.getDeckOracle();
                if (v187Oracle != null && v187Oracle.isAnalyzed()) {
                    int v187Copies = v187Oracle.countCopiesByTitle(cardTitle);
                    if (v187Copies > 1) {
                        action.addReasoning("V187 DUPLICATE: Rando has " + v187Copies + " copies of '"
                            + cardTitle + "' — prefer a singleton starting effect", -300.0f);
                        logger.warn("V187 DUPLICATE STARTING EFFECT: '{}' x{} — -300", cardTitle, v187Copies);
                    }
                }
                // V22 (Steve, 2026-06-28): added Silence Is Golden to the preferred
                // starting effects (a dark Effect: Card3_110 / Card210_045 V).
                if (titleCheck.contains("endor shield") || titleCheck.contains("alert my star destroyer")
                        || titleCheck.contains("silence is golden")) {
                    action.addReasoning("V22 PREFERRED STARTING EFFECT: " + cardTitle, 200.0f);
                    logger.warn("V22 PREFERRED START: {} (+200)", cardTitle);
                }

                // V22 (Steve, 2026-06-19): Shadow Collective payoff effects. Always pick
                // these when the starting interrupt offers Effects from Reserve Deck.
                // You'll Be Dead!: opponent loses 1 Force per battleground site with a
                // non-permanent blaster present. Inconsequential Losses: weapon recycling
                // (forfeit a non-lightsaber weapon at value 3, weapons go to Used Pile).
                if (titleCheck.contains("you'll be dead") || titleCheck.contains("inconsequential losses")) {
                    action.addReasoning("V22 PREFERRED STARTING EFFECT (Shadow Collective payoff): " + cardTitle, 500.0f);
                    logger.warn("V22 PREFERRED START (Shadow Collective): {} (+500)", cardTitle);
                }

                // V186 (Steve, 2026-06-23): I Want That Map starting effect. "The First Order
                // Was Just The Beginning" is the immune-to-Alter Effect to deploy via You Know
                // What I've Come For (208_46). It downloads Jakku/Kijimi battlegrounds to feed
                // the 2-battleground flip. Gated on the active objective so it only fires for
                // this deck; +1000 matches the V80 magnitude so it beats any other matching
                // Effect in the same prompt. Pairs with the ObjectiveAnalyzer V186 block that
                // names Starkiller Base + marks this effect required/pullable.
                // V186 CONSOLIDATED (2026-07-07): identity AND preferred-effect title from analyzer
                // (getIwtmPreferredStartingEffect() is non-null only under I Want That Map, so it
                // double-gates exactly like the old title-contains + objective-contains pair).
                {
                    com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer v186Obj = context.getObjectiveAnalyzer();
                    String v186Eff = (v186Obj != null) ? v186Obj.getIwtmPreferredStartingEffect() : null;
                    if (v186Obj != null && v186Obj.isAnalyzed() && v186Obj.isWantThatMap()
                            && v186Eff != null && titleCheck.contains(v186Eff)) {
                        action.addReasoning("V186 PREFERRED STARTING EFFECT (I Want That Map): " + cardTitle, 1000.0f);
                        logger.warn("V186 PREFERRED START (I Want That Map): {} (+1000)", cardTitle);
                    }
                }

                // === V126 (Steve, 2026-05-22): EXPANDED STARTING-EFFECT BONUSES ===
                // Per Steve: "Evil Is Everywhere should deploy if Revenge of the Sith on
                // table. First Strike is good. Any effect that adds to force generation
                // should get a bump." Three new bonus paths, all type-by-API detection
                // where possible (game-text scan) instead of hardcoded title lists.

                // V126a: First Strike (free battle initiation + drain force gate).
                // Detection: game text mentions "initiate battles for free" OR specifically
                // "initiate a Force drain ... must first use" (the V Set 12 reissue).
                // Falls back to title check since both regular and V variants are intended.
                if (blueprint != null && blueprint.getGameText() != null) {
                    String v126aText = blueprint.getGameText().toLowerCase(java.util.Locale.ROOT);
                    boolean v126aIsBattleStarter =
                        v126aText.contains("initiate battles for free")
                        || titleCheck.contains("first strike");
                    if (v126aIsBattleStarter) {
                        action.addReasoning(
                            "V126a STARTING EFFECT: free battle initiation / drain gate — strong tempo",
                            500.0f);
                        logger.warn("V126a STARTING (battle starter): {} (+500)", cardTitle);
                    }
                }

                // V126b: Effects that boost FORCE GENERATION. Bumped from V22's +25 to
                // +400 per Steve. Detection by game-text scan — "Your force generation
                // is +N" / "+N to Force generation" / "during your activate phase, draw"
                // patterns. Note: V22's older +25 block below still fires for borderline
                // cases (just "force generation" anywhere in text); V126b stacks on top
                // when the text says it's a GENERATION INCREASE specifically.
                if (blueprint != null && blueprint.getGameText() != null) {
                    String v126bText = blueprint.getGameText().toLowerCase(java.util.Locale.ROOT);
                    // Strong signal: explicit force generation modifier.
                    java.util.regex.Matcher v126bMatch = java.util.regex.Pattern.compile(
                        "force\\s+generation\\s*(?:is|are|of|by)?\\s*[+]?\\s*[1-9]"
                    ).matcher(v126bText);
                    boolean v126bIsForceGen = v126bMatch.find()
                        || (v126bText.contains("force generation") && v126bText.contains("+"));
                    if (v126bIsForceGen) {
                        action.addReasoning(
                            "V126b STARTING EFFECT: increases Force generation — compounds every turn",
                            400.0f);
                        logger.warn("V126b STARTING (force gen +): {} (+400)", cardTitle);
                    }
                }

                // V126c: Evil Is Everywhere ↔ Revenge of the Sith synergy.
                // ROTS deploys at start of game and locks Dark Jedi to [Episode I] +
                // chosen apprentice. Evil Is Everywhere makes non-[Episode I] Dark Jedi
                // lost — perfect pairing.
                // Universal-ish detection: check if Revenge of the Sith (or any "[Episode
                // I]"-locking starting card) is already on the table, and the candidate
                // effect's game text references "[Episode I]" Dark Jedi restrictions.
                if (blueprint != null && blueprint.getGameText() != null && gameState != null) {
                    String v126cText = blueprint.getGameText().toLowerCase(java.util.Locale.ROOT);
                    boolean v126cMatchesROTSPairing =
                        (v126cText.contains("[episode i]") || v126cText.contains("episode i"))
                        && v126cText.contains("dark jedi");
                    if (v126cMatchesROTSPairing) {
                        boolean v126cROTSOnTable = false;
                        try {
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getBlueprint() == null) continue;
                                if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone z = pc.getZone();
                                if (z == null || !z.isInPlay()) continue;
                                String pcTitle = pc.getTitle();
                                if (pcTitle != null
                                        && pcTitle.toLowerCase(java.util.Locale.ROOT)
                                            .contains("revenge of the sith")) {
                                    v126cROTSOnTable = true;
                                    break;
                                }
                            }
                        } catch (Exception ignore) { /* leave false */ }
                        if (v126cROTSOnTable) {
                            action.addReasoning(
                                "V126c STARTING EFFECT: pairs with Revenge of the Sith ([Episode I] Dark Jedi lock)",
                                600.0f);
                            logger.warn("V126c STARTING (ROTS synergy): {} (+600)", cardTitle);
                        }
                    }
                }

                // V80 (Steve, 2026-05-15): SKYWALKER EPIC EVENT REQUIRED EFFECTS.
                // The Rise Of Skywalker deploys "two Effects that deploy for free
                // and are always immune to Alter." A Cunning Warrior and A Good
                // Friend are the must-picks here — they require the Skywalker
                // Epic Event on table to deploy, so they're meant for this slot.
                //
                // Cunning Warrior: "Where you have a Skywalker, you initiate
                //   battles for free" + Anakin's Lightsaber pull
                // Good Friend: built-in weapon redistribution (relocate Anakin's
                //   Lightsaber) + Ben Solo recovery + multi-card pull
                //
                // Detection by title (these are unique starting effects).
                if (titleCheck.contains("cunning warrior") || titleCheck.contains("good friend")) {
                    action.addReasoning("V80 SKYWALKER STARTING EFFECT: " + cardTitle + " — required for Rey/Luke Saga deck!", 1000.0f);
                    logger.warn("V80 SKYWALKER STARTING: {} → +1000 (required Skywalker Epic Event effect)", cardTitle);
                }

                // V25: HUNT DOWN V — Specific starting effects
                // The three effects that make this deck work:
                // 1. "There Are Many Hunting You Now" — hatred card engine
                // 2. "I Am Your Father" — key interrupt/effect for Vader synergy
                // 3. "Crush The Rebellion" — force drain enhancement
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer startHDAnalyzer =
                    context.getObjectiveAnalyzer();
                if (startHDAnalyzer != null && startHDAnalyzer.isAnalyzed() && startHDAnalyzer.isHuntDownV()) {
                    if (titleCheck.contains("there are many hunting you now")
                        || titleCheck.contains("i am your father")
                        || titleCheck.contains("crush the rebellion")) {
                        action.addReasoning("V25 HUNT DOWN STARTING EFFECT: " + cardTitle + " — REQUIRED!", 500.0f);
                        logger.warn("V25 HUNT DOWN START: {} is a REQUIRED starting effect (+500)", cardTitle);
                    } else {
                        // Penalize other effects so the three required ones always win
                        action.addReasoning("V25 HUNT DOWN: Not a required starting effect for Hunt Down", -300.0f);
                        logger.warn("V25 HUNT DOWN START: {} is NOT a required starting effect (-300)", cardTitle);
                    }
                }

                // V22: Check if this starting effect's game text references
                // objective-relevant locations. Effects that pull locations needed
                // for our objective should be strongly preferred.
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer startObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (startObjAnalyzer != null && startObjAnalyzer.isAnalyzed() && blueprint != null) {
                    String effectGameText = blueprint.getGameText();
                    if (effectGameText != null) {
                        String effectTextLower = effectGameText.toLowerCase(java.util.Locale.ROOT);
                        // Check if the effect's game text mentions objective-relevant location fragments
                        for (String fragment : startObjAnalyzer.getFlipConditionLocationFragments()) {
                            if (effectTextLower.contains(fragment)) {
                                action.addReasoning("V22 OBJECTIVE-SYNERGY STARTING EFFECT: references '" + fragment + "'", 250.0f);
                                logger.warn("V22 OBJECTIVE START: {} references objective location '{}' (+250)", cardTitle, fragment);
                                break;
                            }
                        }
                        // Check if it can pull cards needed for objective
                        for (String required : startObjAnalyzer.getRequiredCardsOnTable()) {
                            if (effectTextLower.contains(required)) {
                                action.addReasoning("V22 OBJECTIVE-SYNERGY: pulls required card '" + required + "'", 200.0f);
                                logger.warn("V22 OBJECTIVE START: {} can pull required card '{}' (+200)", cardTitle, required);
                                break;
                            }
                        }
                        // Check if it mentions "deploy" + "location", "site", or "system" (location-pulling effect)
                        if (effectTextLower.contains("deploy") && (effectTextLower.contains("location") || effectTextLower.contains("site") || effectTextLower.contains("system"))) {
                            action.addReasoning("V22 LOCATION-PULLING EFFECT: deploys locations", 100.0f);
                            logger.warn("V22 OBJECTIVE START: {} appears to deploy locations (+100)", cardTitle);
                        }
                        // V22: Any effect that interacts with Reserve Deck gets a bonus
                        if (effectTextLower.contains("reserve")) {
                            action.addReasoning("V22 RESERVE DECK ACCESS: can pull from reserve", 50.0f);
                            logger.warn("V22 OBJECTIVE START: {} references Reserve Deck (+50)", cardTitle);
                        }
                        // V22: Effects that boost force generation are valuable early game
                        if (effectTextLower.contains("force generation") || effectTextLower.contains("activate")
                            || effectTextLower.contains("force icon") || effectTextLower.contains("adds one to")) {
                            action.addReasoning("V22 FORCE GENERATION: boosts early force economy", 25.0f);
                            logger.warn("V22 OBJECTIVE START: {} boosts force generation (+25)", cardTitle);
                        }

                        // === V29.15: EPIC EVENT STARTING EFFECT/INTERRUPT ===
                        // Starting effects or interrupts whose game text mentions "epic"
                        // or deploys a key card like "Force Is Strong In My Family"
                        // are critical for decks built around Epic Events.
                        if (effectTextLower.contains("epic")
                            || effectTextLower.contains("force is strong in my family")
                            || effectTextLower.contains("force is strong")) {
                            action.addReasoning("V43 EPIC: starting card deploys Epic Event — critical for deck strategy!", 1500.0f);
                            logger.warn("V43 EPIC START: {} references Epic Event in game text — HARD PREFER (+1500)", cardTitle);
                        }
                    }
                }
            }
            // V24.5: No randomness — deterministic decisions only

            // Score based on card type (like Python)
            if (isLossDecision) {
                // For loss decisions: prefer effects/interrupts
                if (category == CardCategory.EFFECT || category == CardCategory.INTERRUPT) {
                    action.addReasoning("Effect/Interrupt - OK to lose", 25.0f);
                } else if (category == CardCategory.CHARACTER) {
                    action.addReasoning("Character - avoid losing", -15.0f);
                } else if (category == CardCategory.STARSHIP) {
                    action.addReasoning("Starship - avoid losing", -15.0f);
                } else if (category == CardCategory.VEHICLE) {
                    action.addReasoning("Vehicle - avoid losing", -10.0f);
                } else if (category == CardCategory.LOCATION) {
                    action.addReasoning("Location - avoid losing", -20.0f);
                }
            } else {
                // For gain/select decisions: prefer deployables
                if (category == CardCategory.CHARACTER) {
                    action.addReasoning("Character - valuable", 10.0f);
                } else if (category == CardCategory.STARSHIP) {
                    action.addReasoning("Starship - valuable", 8.0f);
                } else if (category == CardCategory.LOCATION) {
                    action.addReasoning("Location - valuable", 10.0f);
                }
            }

            // === V25: HUNT DOWN V — LIGHTSABER PRIORITY (evaluateUnknown path) ===
            // For Hunt Down V, lightsabers are critical for the deck engine:
            // - Vader + lightsaber cancels drain bonuses (back side)
            // - Hatred engine needs lightsabers stacked
            // - "I Am Your Father" pulls Vader's Lightsaber
            if (cardTitle != null) {
                String lsTitleLower = cardTitle.toLowerCase(java.util.Locale.ROOT);
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer lsObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (lsObjAnalyzer != null && lsObjAnalyzer.isAnalyzed() && lsObjAnalyzer.isHuntDownV()
                    && lsTitleLower.contains("lightsaber")) {
                    if (isLossDecision) {
                        action.addReasoning("V25 HUNT DOWN: PROTECT LIGHTSABER from loss!", -300.0f);
                        logger.warn("V25 HUNT DOWN UNKNOWN-LOSS: {} is a lightsaber — PROTECT (-300)", cardTitle);
                    } else {
                        action.addReasoning("V25 HUNT DOWN: LIGHTSABER — critical for deck engine!", 200.0f);
                        logger.warn("V25 HUNT DOWN UNKNOWN-GAIN: {} is a lightsaber — PRIORITY (+200)", cardTitle);
                    }
                }
            }

            // === V24.10: CC SITE SELECTION — CONTEXT-AWARE (evaluateUnknown path) ===
            // Slip Sliding GRABS Dining Room — guarantees Upper Walkway + Dining Room as starting sites.
            // I'm Sorry then pulls other interior CC sites in-game.
            if (cardTitle != null) {
                String ctLower = cardTitle.toLowerCase(java.util.Locale.ROOT);
                if (ctLower.contains("cloud city") &&
                    (textLower.contains("sorry") || textLower.contains("interior") ||
                     textLower.contains("cloud city") || textLower.contains("battleground"))) {

                    boolean isSlipSliding = textLower.contains("slip") || textLower.contains("battleground") ||
                        context.getTurnNumber() <= 0;
                    boolean isImSorry = textLower.contains("sorry") || textLower.contains("interior");

                    if (isImSorry && !isSlipSliding) {
                        // I'm Sorry pulls other interior CC sites (Dining Room already on table from Slip Sliding)
                        if (ctLower.contains("dining room")) {
                            // Dining Room should already be on table — low priority for I'm Sorry
                            action.addReasoning("V24.10 I'M SORRY: Dining Room likely already on table", -50.0f);
                        } else if (ctLower.contains("security tower")) {
                            // V24.13: Security Tower = force-gen only, deploy LAST for far-end placement
                            action.addReasoning("V24.13 I'M SORRY: Security Tower — force-gen only, deploy LAST!", -30.0f);
                        } else if (ctLower.contains("carbonite chamber")) {
                            // V24.13: Carbonite Chamber = key battleground, pull FIRST!
                            action.addReasoning("V24.13 I'M SORRY: Carbonite Chamber — priority battleground!", 150.0f);
                        } else {
                            action.addReasoning("V24.10 I'M SORRY: Pull interior CC site — expand drain sites!", 100.0f);
                        }
                    } else if (isSlipSliding) {
                        if (ctLower.contains("dining room")) {
                            // Slip Sliding GRABS Dining Room — guarantees best starting pair!
                            action.addReasoning("V24.10 SLIP SLIDING: Dining Room — guarantees best starting CC site pair!", 300.0f);
                            logger.warn("V24.10 SLIP SLIDING: Dining Room +300 — grab it as starting location!");
                        } else {
                            action.addReasoning("V24.10 SLIP SLIDING: Other CC site — Dining Room is better", -50.0f);
                        }
                    }
                }
            }

            // Check for priority cards
            if (blueprintId != null && AiPriorityCards.isPriorityCard(blueprintId)) {
                int priorityScore = AiPriorityCards.getProtectionScore(blueprintId);
                action.addReasoning("Priority card", priorityScore * 0.3f);
            }

            // === V24.10: AMSD PIETT-ONLY SAFETY NET (replaces V24.8) ===
            // If AMSD is in play and we're choosing characters during deploy phase,
            // enforce Piett-only regardless of decision text. This catches cases where
            // the AMSD routing catch above didn't fire (e.g., we're already in evaluateUnknown).
            if (context.getPhase() == Phase.DEPLOY && blueprint != null && category == CardCategory.CHARACTER) {
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle safetyOracle = context.getDeckOracle();
                if (safetyOracle != null && safetyOracle.isAnalyzed()) {
                    boolean amsdActive = safetyOracle.isCardInPlay("Alert My Star Destroyer")
                        || safetyOracle.isCardInPlay("Alert My Star Destroyer!");
                    if (amsdActive) {
                        String pilotNameLower = (cardTitle != null) ? cardTitle.toLowerCase(java.util.Locale.ROOT) : "";
                        if (pilotNameLower.contains("piett")) {
                            action.addReasoning("V24.10 AMSD SAFETY NET: PIETT — approved for AMSD!", 500.0f);
                            logger.warn("V24.10 AMSD SAFETY NET: Piett detected — APPROVED (+500)");
                        } else {
                            action.addReasoning("V24.10 AMSD SAFETY NET: " + cardTitle + " is NOT Piett — AMSD requires Piett only!", -9999.0f);
                            logger.warn("V24.10 AMSD SAFETY NET: {} is NOT Piett — HARD BLOCK (-9999)", cardTitle);
                        }
                    }
                }
            }

            // V28/V47 RESERVE SOLO BLOCK — RETIRED 2026-07-12 (batch 1d; Codex m00206 wrong-facts
            // audit CODEX_V47_WRONG_FACTS_AUDIT_2026-07-12.md): it applied Cloud City board facts to
            // EVERY "deploy...reserve" character prompt regardless of the real forced destination
            // (Krennic->Scarif, Praji, Snoke all false-blocked at -9999 on forced noPass nodes).
            // Replacement owner: FormationSafety pull-route guard (destination-aware, ActionText
            // V192 block). DELETED per Steve's 2026-07-12 migration ruling (backup + git = undo).

            actions.add(action);
        }

        logger.info("🔍 evaluateUnknown: {} selectable, {} skipped (non-selectable)",
                   selectableCount, skippedCount);

        if (actions.isEmpty()) {
            logger.warn("⚠️ evaluateUnknown: No selectable cards! Decision may fail.");
        }

        return actions;
    }

    /**
     * Check if a location is likely a battleground.
     */
    private boolean isLikelyBattleground(SwccgCardBlueprint blueprint) {
        // Sites and systems are often battlegrounds
        // This is a heuristic - actual battleground status comes from card data
        return blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION;
    }

    /**
     * Check if a card at given index is selectable.
     * CRITICAL: GEMP rejects selection of non-selectable cards!
     */
    private boolean isCardSelectable(DecisionContext context, int index) {
        List<Boolean> selectable = context.getSelectable();
        if (selectable == null || selectable.isEmpty()) {
            // No selectable info - assume all are selectable
            return true;
        }
        if (index >= selectable.size()) {
            // Index out of bounds - assume selectable
            return true;
        }
        Boolean isSelectable = selectable.get(index);
        return isSelectable == null || isSelectable;
    }

    /**
     * Reserve deck selection - evaluate cards by blueprint.
     * Used when selecting from Reserve Deck (e.g., deploying shields via starting effect).
     * Uses DeployPhasePlanner when available to select cards that fit the deployment plan.
     */
    private List<EvaluatedAction> evaluateReserveDeckSelection(DecisionContext context, String textLower) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> blueprints = context.getBlueprints();
        ShieldStrategy shieldStrategy = context.getShieldStrategy();
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        SwccgGame game = context.getGame();
        Side side = context.getSide();
        String playerId = context.getPlayerId();
        int turnNumber = context.getTurnNumber();

        logger.info("[CardSelectionEvaluator] Evaluating Reserve Deck selection: {}", textLower);

        // V67ay (Steve, 2026-05-08): UNIVERSAL ONE-WEAPON RULE for reserve-deck SELECT step.
        //
        // Rando's V67ar block in ActionTextEvaluator's "from reserve deck" branch
        // SKIPS weapon-pull actions whose source card ALSO offers a location pull
        // (because of the `!v67lAddsLocation` exclusion). Evil Is Everywhere is
        // the canonical case: '[download] a mobile hallway or [Episode I]
        // lightsaber'. V67ar lets the parent action through; Rando initiates it,
        // GEMP shuffles reserve, then asks "pick a card to deploy" with a list of
        // matching candidates (weapons + locations). At THAT prompt the weapon
        // option needs to be hard-blocked when every Rando character on table is
        // already armed — otherwise Rando picks Asajj's Lightsabers and stacks
        // it on Sidious (already wielding Sidious' Lightsaber).
        //
        // This check fires UNCONDITIONALLY before the per-blueprint loop
        // computes the all-friendly-chars-armed counters once, then the loop
        // applies -9999 to every weapon-category candidate. Locations (the
        // mobile hallway alternative) are unaffected — Rando still picks one if
        // available.
        boolean v67ayAllArmed = false;
        int v67ayUnarmed = 0;
        int v67ayArmed = 0;
        if (game != null && playerId != null) {
            try {
                GameState gs = game.getGameState();
                if (gs != null) {
                    for (PhysicalCard pc : gs.getAllPermanentCards()) {
                        if (pc == null || pc.getBlueprint() == null) continue;
                        if (!playerId.equals(pc.getOwner())) continue;
                        com.gempukku.swccgo.common.Zone z = pc.getZone();
                        if (z == null || !z.isInPlay()) continue;
                        if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                        boolean armed = false;
                        java.util.List<PhysicalCard> atts = gs.getAttachedCards(pc);
                        if (atts != null) {
                            for (PhysicalCard a : atts) {
                                if (a != null && a.getBlueprint() != null
                                        && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                    armed = true;
                                    break;
                                }
                            }
                        }
                        if (armed) v67ayArmed++; else v67ayUnarmed++;
                    }
                    v67ayAllArmed = (v67ayUnarmed == 0 && v67ayArmed > 0);
                }
            } catch (Exception e) {
                logger.debug("V67ay weapon-armed scan failed: {}", e.getMessage());
            }
        }
        if (v67ayAllArmed) {
            logger.warn("V67ay GUARD: every Rando character armed (armed={}, unarmed=0) — weapon picks from reserve will be hard-blocked",
                v67ayArmed);
        }

        // Get deployment plan if available
        DeploymentPlan plan = null;
        if (planner != null && game != null && side != null && playerId != null) {
            plan = planner.createPlan(game, playerId, side);
            if (plan != null) {
                logger.info("[CardSelectionEvaluator] Using deployment plan: strategy={}, instructions={}",
                    plan.getStrategy(), plan.getInstructions().size());
            }
        }

        // V24.10: Get card titles for smarter reserve deck selection
        List<String> reserveTestingTexts = context.getTestingTexts();

        for (int i = 0; i < blueprints.size(); i++) {
            String blueprintId = blueprints.get(i);

            // Get card title if available
            String cardTitle = null;
            if (reserveTestingTexts != null && i < reserveTestingTexts.size()) {
                cardTitle = reserveTestingTexts.get(i);
            }
            String cardTitleLower = cardTitle != null ? cardTitle.toLowerCase(java.util.Locale.ROOT) : "";

            // Use index as action ID for blueprint-based selections
            EvaluatedAction action = new EvaluatedAction(
                String.valueOf(i),
                ActionType.DEPLOY,
                50.0f,
                "Deploy " + (cardTitle != null ? cardTitle : blueprintId)
            );

            // V70 (Steve, 2026-05-12): ONE-WEAPON-PER-CHARACTER rule.
            // Per Steve's standing rule: "No character should ever have two weapons."
            // Uses comprehensive criteria search (helper). See v70CheckWeaponDeviceBlock.
            SwccgCardBlueprint v70Bp = null;
            CardCategory v70Cat = null;
            try {
                v70Bp = getBlueprintFromId(context, blueprintId);
                if (v70Bp != null) v70Cat = v70Bp.getCardCategory();
            } catch (Exception e) { /* ignore */ }
            String v70Reason = v70CheckWeaponDeviceBlock(game, playerId, v70Cat, v70Bp);
            if (v70Reason != null) {
                action.addReasoning(
                    "V70 NO 2ND WEAPON: " + v70Reason + " — '" + (cardTitle != null ? cardTitle : blueprintId) + "'",
                    -9999.0f);
                logger.warn("V70 BLOCK (reserve-pick, {}): '{}' (bp {}) — {}",
                    v70Cat, cardTitle, blueprintId, v70Reason);
                actions.add(action);
                continue;
            }

            // V80 (Steve, 2026-05-15): SKYWALKER EPIC EVENT REQUIRED EFFECTS.
            // Mirror of the V80 check in evaluateUnknown. Fires here when the
            // starting interrupt's "deploy N Effects for free" prompt routes
            // through evaluateReserveDeckSelection (cardIds empty, blueprints
            // populated). Works for "deploy 2 Effects for free" AND "deploy 3
            // Effects for free" variants — score is per-candidate, not per-prompt.
            if (cardTitleLower.contains("cunning warrior") || cardTitleLower.contains("good friend")) {
                action.addReasoning(
                    "V80 SKYWALKER STARTING EFFECT: " + cardTitle + " — required for Rey/Luke Saga deck!",
                    1000.0f);
                logger.warn("V80 SKYWALKER STARTING (reserve-pick): {} → +1000", cardTitle);
            }

            // V22 (Steve, 2026-06-19): Shadow Collective payoff effects. Mirror of the
            // V22 PREFERRED STARTING EFFECT check in evaluateUnknown, for when the starting
            // interrupt's "deploy N Effects for free" prompt routes through here.
            if (context.getTurnNumber() <= 0
                    && (cardTitleLower.contains("you'll be dead") || cardTitleLower.contains("inconsequential losses"))) {
                action.addReasoning("V22 PREFERRED STARTING EFFECT (Shadow Collective payoff): " + cardTitle, 500.0f);
                logger.warn("V22 PREFERRED START (Shadow Collective, reserve-pick): {} (+500)", cardTitle);
            }

            // === V24.10: CC SITE SELECTION — CONTEXT-AWARE ===
            // Two different effects pull CC sites from reserve:
            //   1. Slip Sliding Away (starting interrupt, turn 0): picks a CC battleground site
            //   2. I'm Sorry (V) (during game): pulls interior CC sites
            // Strategy: Slip Sliding GRABS Dining Room — guarantees Upper Walkway + Dining Room
            // as the starting CC site pair. I'm Sorry then pulls other interior sites in-game.
            if (cardTitleLower.contains("cloud city") &&
                (textLower.contains("sorry") || textLower.contains("interior") ||
                 textLower.contains("cloud city") || textLower.contains("battleground"))) {

                // V26 FIX: The TDIGWATT objective text also contains "battleground" in
                // "Choose Cloud City battleground site to deploy" — that's NOT Slip Sliding!
                // Only treat it as Slip Sliding if "slip" is in the text or if the original
                // decision text explicitly references Slip Sliding Away.
                // The objective choice should pick EXTERIOR (Upper Walkway), and Slip Sliding
                // picks INTERIOR (Dining Room).
                boolean isObjectivePick = textLower.contains("choose") && textLower.contains("site")
                    && textLower.contains("deploy") && !textLower.contains("slip");
                boolean isSlipSlidingPick = (textLower.contains("slip") || textLower.contains("sliding"))
                    && !isObjectivePick;
                boolean isImSorryPick = textLower.contains("sorry") || textLower.contains("interior");

                if (isObjectivePick) {
                    // V26: TDIGWATT OBJECTIVE deploys ONE CC battleground site.
                    // MUST be an EXTERIOR site (Upper Walkway) — I'm Sorry CAN'T pull exterior sites!
                    // Dining Room is INTERIOR and will be pulled by Slip Sliding Away.
                    // If we deploy Dining Room here, Slip Sliding has to pick something else,
                    // and we risk drawing Dining Room into hand/force pile before the interrupt fires.
                    if (cardTitleLower.contains("upper walkway") || cardTitleLower.contains("exterior walkway")) {
                        action.addReasoning("V26 OBJECTIVE: Upper Walkway is EXTERIOR — only way to get it out! I'm Sorry can't pull this!", 500.0f);
                        logger.warn("V26 OBJECTIVE SITE: {} is EXTERIOR — MUST deploy here (+500)!", cardTitle);
                    } else if (cardTitleLower.contains("dining room")) {
                        action.addReasoning("V26 OBJECTIVE: Dining Room is INTERIOR — save for Slip Sliding Away!", -400.0f);
                        logger.warn("V26 OBJECTIVE SITE: {} is INTERIOR — Slip Sliding will grab this (-400)!", cardTitle);
                    } else {
                        // Other CC sites — they're interior, save for I'm Sorry
                        action.addReasoning("V26 OBJECTIVE: Interior CC site — save for I'm Sorry, deploy Exterior first!", -200.0f);
                        logger.warn("V26 OBJECTIVE SITE: {} — not Exterior, deprioritized (-200)", cardTitle);
                    }
                } else if (isImSorryPick && !isSlipSlidingPick) {
                    // I'm Sorry pulls other interior CC sites (Dining Room already on table from Slip Sliding)
                    if (cardTitleLower.contains("dining room")) {
                        action.addReasoning("V24.10 I'M SORRY: Dining Room likely already on table from Slip Sliding", -50.0f);
                        logger.info("V24.10 I'M SORRY PULL: Dining Room -50 — should already be deployed");
                    } else if (cardTitleLower.contains("security tower")) {
                        action.addReasoning("V24.13 I'M SORRY: Security Tower is force-gen only — deploy LAST!", -30.0f);
                        logger.info("V24.13 I'M SORRY: Security Tower deprioritized (-30) — pull battleground sites first");
                    } else if (cardTitleLower.contains("carbonite chamber")) {
                        action.addReasoning("V24.13 I'M SORRY: Carbonite Chamber — key battleground, pull FIRST!", 150.0f);
                        logger.warn("V24.13 I'M SORRY PULL: Carbonite Chamber +150 — priority battleground!");
                    } else {
                        action.addReasoning("V24.10 I'M SORRY: Pull interior CC site — expand drain sites!", 100.0f);
                        logger.warn("V24.10 I'M SORRY PULL: {} +100 — new drain site!", cardTitle);
                    }
                } else if (isSlipSlidingPick) {
                    // Slip Sliding Away GRABS Dining Room — best starting pair!
                    if (cardTitleLower.contains("dining room")) {
                        action.addReasoning("V24.10 SLIP SLIDING: Dining Room — guarantees best starting CC site pair!", 300.0f);
                        logger.warn("V24.10 SLIP SLIDING: Dining Room +300 — grab as starting location!");
                    } else {
                        action.addReasoning("V24.10 SLIP SLIDING: Other CC site — Dining Room is better", -50.0f);
                        logger.info("V24.10 SLIP SLIDING: {} — not Dining Room (-50)", cardTitle);
                    }
                }
            }

            // === Check deployment plan first ===
            if (plan != null && !plan.getInstructions().isEmpty()) {
                DeploymentInstruction instruction = plan.getInstructionForCard(blueprintId);
                if (instruction != null) {
                    // Card is in deployment plan - high priority
                    action.addReasoning("IN DEPLOYMENT PLAN: " + plan.getStrategy(), 100.0f);
                    logger.info("[ReserveDeck] {} IN PLAN - high priority", blueprintId);
                } else if (plan.getHoldBackCards().contains(blueprintId)) {
                    // Card should be held back
                    action.addReasoning("HOLD BACK: save for later", -50.0f);
                    logger.debug("[ReserveDeck] {} should be held back", blueprintId);
                }
            }

            // === Shield scoring ===
            if (shieldStrategy != null) {
                // Check if this is a defensive shield by blueprint pattern
                float shieldScore = shieldStrategy.scoreShield(blueprintId, blueprintId, turnNumber);

                if (shieldScore > -50) {
                    // Likely a shield - use shield scoring
                    // Add to existing score rather than replacing
                    action.addReasoning("Shield scoring", shieldScore);
                    String description = shieldStrategy.getShieldDescription(blueprintId, blueprintId);
                    logger.info("[ReserveDeck] Shield {}: score={} ({})", blueprintId, shieldScore, description);
                }

                // === V51: BATTLE ORDER GATE ===
                // Battle Order requires occupying BOTH a battleground site AND a battleground system.
                // If we don't occupy both, deploying Battle Order is a waste — it does nothing.
                if (cardTitleLower.contains("battle order") || cardTitleLower.contains("battle plan")) {
                    // V51 UPDATED 2026-07-06 (Verge game): engine occupies predicate
                    // (occupiesBothTheaters) replaces the hand-rolled loop — old loop
                    // commented out below per feedback_comment_out_old_rules.
                    if (!occupiesBothTheaters(game, playerId)) {
                        action.addReasoning("V51 BATTLE ORDER GATE: Need BOTH a BG site AND BG system occupied!", -9999.0f);
                        logger.warn("V51 BATTLE ORDER GATE: occupiesBothTheaters=false — BLOCKED!");
                    } else {
                        action.addReasoning("V51 BATTLE ORDER: Occupy BG site + BG system — ready!", 50.0f);
                        logger.warn("V51 BATTLE ORDER: Requirements met — deploying!");
                        // V51 EARLY-DEPLOY (EXTENDED 2026-07-06, Steve): +200 so Battle Order/Plan
                        // deploys turns 1-2 the moment Rando occupies both theaters (tax compounds).
                        // Guard shieldScore > -50 so a V43 redundant / pacing / not-played rejection
                        // still wins. Occupy-only gate per Steve ("does not matter if opponent occupies").
                        if (shieldScore > -50f) {
                            action.addReasoning("V51 BATTLE ORDER EARLY-DEPLOY: occupy BG site + system — deploy now, tax compounds +200", 200.0f);
                            logger.warn("V51 BATTLE ORDER: both theaters — +200 EARLY-DEPLOY (base {})", shieldScore);
                        }
                    }
                    // OLD hand-rolled occupation loop (superseded 2026-07-06):
                    // boolean hasBGSite = false; boolean hasBGSystem = false;
                    // try {
                    //     GameState gsBO = (game != null) ? game.getGameState() : null;
                    //     if (game != null && gsBO != null && playerId != null) {
                    //         for (PhysicalCard loc : gsBO.getAllPermanentCards()) {
                    //             if (loc == null || loc.getBlueprint() == null) continue;
                    //             com.gempukku.swccgo.common.Zone locZone = loc.getZone();
                    //             if (locZone == null || locZone != com.gempukku.swccgo.common.Zone.LOCATIONS) continue;
                    //             SwccgCardBlueprint locBp = loc.getBlueprint();
                    //             boolean isBattleground = false;
                    //             try { com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying mq = game.getModifiersQuerying();
                    //                   if (mq != null) isBattleground = mq.isBattleground(gsBO, loc, null); } catch (Exception bgEx) { }
                    //             if (!isBattleground) continue;
                    //             boolean weOccupy = false;
                    //             for (PhysicalCard atLoc : gsBO.getCardsAtLocation(loc)) {
                    //                 if (atLoc != null && playerId.equals(atLoc.getOwner())) { weOccupy = true; break; }
                    //             }
                    //             if (weOccupy) {
                    //                 if (locBp.getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) hasBGSystem = true;
                    //                 else if (locBp.getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE) hasBGSite = true;
                    //             }
                    //         }
                    //     }
                    // } catch (Exception e) { logger.debug("V51 BATTLE ORDER: Error checking occupation: {}", e.getMessage()); }
                    // if (!hasBGSite || !hasBGSystem) { -9999 } else { +50 }
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Check if this is a shield selection by examining the available cards.
     * Similar to Python's approach of checking if majority of options are shields.
     * V29.5: Also handles ARBITRARY_CARDS decisions with temp IDs (e.g., K&D shield plays)
     * by looking up cards via blueprint IDs instead of integer card IDs.
     */
    private boolean isShieldSelectionByContent(DecisionContext context) {
        GameState gameState = context.getGameState();
        List<String> cardIds = context.getCardIds();
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());

        if (cardIds == null || cardIds.isEmpty()) {
            return false;
        }

        int shieldCount = 0;
        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            try {
                SwccgCardBlueprint blueprint = null;

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    // V29.5: ARBITRARY_CARDS — use blueprint ID lookup (temp IDs can't be parsed as ints)
                    String bpId = blueprintIds.get(idx);
                    blueprint = getBlueprintFromId(context, bpId);
                } else if (gameState != null) {
                    // Standard path — integer card ID
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        blueprint = card.getBlueprint();
                    }
                }

                if (blueprint != null &&
                    blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {
                    shieldCount++;
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // If majority are shields, treat as shield selection
        boolean isShield = shieldCount > 0 && shieldCount >= cardIds.size() * 0.5;
        if (isShield) {
            logger.warn("V29.5 isShieldSelectionByContent: YES — {}/{} cards are shields (isArbitrary={})",
                shieldCount, cardIds.size(), isArbitrary);
        }
        return isShield;
    }

    /**
     * Defensive shield selection - use ShieldStrategy scoring.
     */
    /**
     * V51/V112 (UPDATED 2026-07-06): mirror Battle Order's OWN occupation condition
     * via the engine instead of a hand-rolled owner-present loop (the V140-class
     * fix). Battle Order/Plan help Rando only while HE occupies both a battleground
     * site AND a battleground system (opponent's drains then cost +3; Rando's are
     * not taxed). canSpot(occupies + battleground_site/system) is exactly what the
     * card's OccupiesCondition checks, so gate and card can never disagree again
     * (the Verge game bug: the hand loop missed a Scarif SYSTEM occupation the
     * engine predicate catches). Fails closed on any error (no false deploy).
     */
    private boolean occupiesBothTheaters(com.gempukku.swccgo.game.SwccgGame game, String playerId) {
        if (game == null || playerId == null) return false;
        try {
            boolean site = com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.battleground_site));
            boolean sys = com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.battleground_system));
            return site && sys;
        } catch (Exception e) {
            logger.debug("occupiesBothTheaters error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * V105/V117 (UPDATED 2026-07-06): is the 4th-slot preferred shield title
     * actually offered among THIS decision's candidates? The Verge game held the
     * 4th shield slot hostage all game: prefers4thSlot returned "Battle Order"
     * (Rando occupied both theaters) but Battle Order was never in the candidate
     * list, so the old code hard-blocked every real shield at -5000 (2760 fires)
     * and the slot deployed nothing. When the preferred card is not on the menu,
     * the caller holds the slot closed instead of spamming a block for a card that
     * can't be picked. Scans the full candidate list from context (both the
     * ARBITRARY_CARDS blueprint path and the standard card-id path). Fails to
     * false (= treat as not offered = hold slot closed), the conservative side.
     */
    private boolean preferredShieldInCandidates(DecisionContext context, String preferredTitle) {
        if (context == null || preferredTitle == null) return false;
        String want = preferredTitle.toLowerCase(java.util.Locale.ROOT);
        try {
            List<String> pcIds = context.getCardIds();
            List<String> pcBps = context.getBlueprints();
            boolean arbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());
            GameState pcGs = context.getGameState();
            int n = pcIds != null ? pcIds.size() : 0;
            for (int i = 0; i < n; i++) {
                String t = null;
                if (arbitrary && pcBps != null && i < pcBps.size()) {
                    SwccgCardBlueprint bp = getBlueprintFromId(context, pcBps.get(i));
                    if (bp != null) t = bp.getTitle();
                } else if (pcGs != null) {
                    try {
                        PhysicalCard c = pcGs.findCardById(Integer.parseInt(pcIds.get(i)));
                        if (c != null) t = c.getTitle();
                    } catch (NumberFormatException nfe) { /* skip unparseable */ }
                }
                if (t != null && t.toLowerCase(java.util.Locale.ROOT).contains(want)) return true;
            }
        } catch (Exception e) {
            logger.debug("preferredShieldInCandidates error: {}", e.getMessage());
        }
        return false;
    }

    private List<EvaluatedAction> evaluateShieldSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        ShieldStrategy shieldStrategy = context.getShieldStrategy();
        int turnNumber = context.getTurnNumber();
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());

        logger.warn("[CardSelectionEvaluator] V29.5 Evaluating DEFENSIVE SHIELD selection (isArbitrary={}, shieldStrategy={})",
            isArbitrary, shieldStrategy != null ? "SET" : "NULL");

        List<String> cardIds = context.getCardIds();
        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,  // Base score
                "Deploy shield"
            );

            try {
                String title = null;
                String blueprintId = null;
                SwccgCardBlueprint blueprint = null;

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    // V29.5: ARBITRARY_CARDS path — use blueprint ID (temp IDs can't be parsed)
                    blueprintId = blueprintIds.get(idx);
                    blueprint = getBlueprintFromId(context, blueprintId);
                    if (blueprint != null) {
                        title = blueprint.getTitle();
                        logger.warn("V29.5 SHIELD ARBITRARY: Resolved '{}' → '{}' (bp={})", cardId, title, blueprintId);
                    }
                } else if (gameState != null) {
                    // Standard path — integer card ID
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        title = card.getTitle();
                        blueprintId = card.getBlueprintId(true);
                        blueprint = card.getBlueprint();
                    }
                }

                if (title != null) {
                    action.setDisplayText("Shield: " + title);
                }

                // Verify it's actually a defensive shield
                if (blueprint != null &&
                    blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {

                    // Use ShieldStrategy for scoring
                    if (shieldStrategy != null && blueprintId != null && title != null) {
                        float shieldScore = shieldStrategy.scoreShield(
                            blueprintId, title, turnNumber);

                        // Set score directly (ShieldStrategy fully controls priority)
                        action.setScore(shieldScore);
                        String description = shieldStrategy.getShieldDescription(blueprintId, title);
                        action.addReasoning("Shield: " + description, 0.0f);

                        logger.warn("V29.5 [Shield] {}: score={} ({})", title, shieldScore, description);

                        // === V105/V107 (Steve, 2026-05-20): 4TH-SLOT CONDITIONAL PICK ===
                        // 4th-shield slot stays CLOSED INDEFINITELY until V105 (Battle
                        // Order/Plan) OR V107 (Resistance/Ultimatum) trigger fires.
                        // V106 (CHYBC/Simple Tricks) dropped per Steve 2026-05-20.
                        if (shieldStrategy.shieldsRemaining() <= 1) {
                            String preferred = shieldStrategy.prefers4thSlot(
                                context.getGameState(), context.getGame(), context.getPlayerId());
                            // V105 UPDATED 2026-07-06 (Verge game unli50oa1ur8bdux): only pursue
                            // the preferred card if it is actually offered in THIS decision.
                            // Otherwise the 4th slot was held hostage all game — "prefer Battle
                            // Order" fired while Battle Order was never in the candidate list, so
                            // every real shield was hard-blocked at -5000 (2760 fires) and the
                            // slot deployed nothing. When the preferred card is absent, fall
                            // through to HOLD (slot stays closed, Steve's closed-by-default 4th
                            // slot) instead of spamming a block for a card that can't be picked.
                            boolean preferredOnMenu = preferred != null
                                && preferredShieldInCandidates(context, preferred);
                            if (preferredOnMenu) {
                                String tLower = title.toLowerCase(java.util.Locale.ROOT);
                                String pLower = preferred.toLowerCase(java.util.Locale.ROOT);
                                if (tLower.contains(pLower)) {
                                    action.addReasoning(
                                        "V105/V107 4TH SLOT BOOST: '" + title
                                            + "' matches preferred '" + preferred + "' +2000",
                                        2000.0f);
                                    logger.warn("V105/V107 4TH SLOT: BOOST '{}' (matches preferred '{}') +2000",
                                        title, preferred);
                                } else {
                                    action.addReasoning(
                                        "V105/V107 4TH SLOT: '" + title
                                            + "' not preferred (we want '" + preferred + "') -5000",
                                        -5000.0f);
                                    logger.warn("V105/V107 4TH SLOT: HARD-BLOCK '{}' (prefer '{}', on menu) -5000",
                                        title, preferred);
                                }
                            } else {
                                // No trigger active, OR the preferred card is not on the menu
                                // this decision — 4th slot stays CLOSED (hold), no spam.
                                action.addReasoning(
                                    "V105/V107 4TH SLOT HOLD: no available preferred card — keep slot closed -5000",
                                    -5000.0f);
                                logger.warn("V105/V107 4TH SLOT: HOLD '{}' — preferred={} not on menu / no trigger, slot closed -5000",
                                    title, preferred);
                            }
                        }

                        // === V51: BATTLE ORDER GATE (shield selection path) ===
                        // UPDATED 2026-07-06 (Verge game unli50oa1ur8bdux): detection swapped
                        // to the engine occupies predicate (occupiesBothTheaters) — the old
                        // hand-rolled owner-present loop below missed a Scarif SYSTEM occupation
                        // while V105's power-based scan caught it, so the two disagreed. Old
                        // loop commented out per feedback_comment_out_old_rules.
                        if (title.toLowerCase(java.util.Locale.ROOT).contains("battle order")
                                || title.toLowerCase(java.util.Locale.ROOT).contains("battle plan")) {
                            if (!occupiesBothTheaters(context.getGame(), context.getPlayerId())) {
                                action.addReasoning("V51 BATTLE ORDER GATE: Need BOTH a BG site AND BG system occupied!", -9999.0f);
                                logger.warn("V51 BATTLE ORDER GATE (shield): occupiesBothTheaters=false — BLOCKED!");
                            } else {
                                // V51 EARLY-DEPLOY (EXTENDED 2026-07-06, Steve): once Rando occupies
                                // both theaters, his own drains stop being taxed and the opponent's
                                // are taxed +3 — value that COMPOUNDS every turn, so deploy Battle
                                // Order/Plan EARLY (turns 1-2 too), not only in the turn-3 4th slot.
                                // Base SITUATIONAL_HIGH untriggered = 80, which loses to auto-play
                                // shields (200). +200 → 280 beats them and deploys within the turn's
                                // pacing budget; stays far under the 4th-slot V105 +2000 (rides on
                                // top there, harmless). Gated on occupy-only per Steve ("does not
                                // matter if opponent also occupies"). GUARD shieldScore > -50 so this
                                // never resurrects a V43 redundant-shield block (-100, opponent
                                // already has the equivalent), a pacing-cap -50, or a not-played -100.
                                if (shieldScore > -50f) {
                                    action.addReasoning("V51 BATTLE ORDER EARLY-DEPLOY: occupy BG site + system — deploy now, tax compounds +200", 200.0f);
                                    logger.warn("V51 BATTLE ORDER (shield): both theaters — +200 EARLY-DEPLOY (base {})", shieldScore);
                                } else {
                                    logger.warn("V51 BATTLE ORDER (shield): both theaters but base {} <= -50 (V43/pacing) — boost suppressed", shieldScore);
                                }
                            }
                            // OLD hand-rolled occupation loop (superseded 2026-07-06):
                            // boolean hasBGSite = false;
                            // boolean hasBGSystem = false;
                            // try {
                            //     GameState gs = context.getGameState();
                            //     SwccgGame g = context.getGame();
                            //     String pid = context.getPlayerId();
                            //     if (g != null && gs != null && pid != null) {
                            //         for (PhysicalCard loc : gs.getAllPermanentCards()) {
                            //             if (loc == null || loc.getBlueprint() == null) continue;
                            //             com.gempukku.swccgo.common.Zone locZone = loc.getZone();
                            //             if (locZone == null || locZone != com.gempukku.swccgo.common.Zone.LOCATIONS) continue;
                            //             SwccgCardBlueprint locBp = loc.getBlueprint();
                            //             boolean isBattleground = false;
                            //             try {
                            //                 com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying mq = g.getModifiersQuerying();
                            //                 if (mq != null) isBattleground = mq.isBattleground(gs, loc, null);
                            //             } catch (Exception bgEx) { /* fallback */ }
                            //             if (!isBattleground) continue;
                            //             boolean weOccupy = false;
                            //             for (PhysicalCard atLoc : gs.getCardsAtLocation(loc)) {
                            //                 if (atLoc != null && pid.equals(atLoc.getOwner())) { weOccupy = true; break; }
                            //             }
                            //             if (weOccupy) {
                            //                 if (locBp.getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) hasBGSystem = true;
                            //                 else if (locBp.getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE) hasBGSite = true;
                            //             }
                            //         }
                            //     }
                            // } catch (Exception e) { logger.debug("V51 BATTLE ORDER: Error checking occupation: {}", e.getMessage()); }
                            // if (!hasBGSite || !hasBGSystem) { action.addReasoning("V51 BATTLE ORDER GATE: Need BOTH a BG site AND BG system occupied!", -9999.0f); ... }
                        }
                    } else {
                        // Fallback if no shield strategy
                        action.addReasoning("Defensive shield (no strategy)", 50.0f);
                        logger.warn("V29.5 [Shield] {}: NO STRATEGY — fallback score 100", title);
                    }
                } else if (blueprint != null) {
                    // Not a shield - low priority
                    action.addReasoning("Not a defensive shield", -50.0f);
                } else {
                    logger.warn("V29.5 [Shield] Could not resolve card '{}' (bp={})", cardId, blueprintId);
                    action.addReasoning("Unresolved shield card", 0.0f);
                }
            } catch (NumberFormatException e) {
                // V29.5: This shouldn't happen anymore for ARBITRARY_CARDS
                logger.warn("V29.5 [Shield] NumberFormatException for cardId '{}' — should use blueprint path", cardId);
                action.addReasoning("Invalid card ID (should not happen with V29.5)", -100.0f);
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Extract blueprint ID from decision text HTML.
     * GEMP decision text for deploy location includes the card being deployed
     * in format: <div class='cardHint' value='8_35'>CardName</div>
     *
     * @param decisionText the decision text which may contain HTML
     * @return the blueprint ID, or null if not found
     */
    private String extractBlueprintFromDecisionText(String decisionText) {
        if (decisionText == null || decisionText.isEmpty()) {
            return null;
        }

        // Pattern: value='8_35' or value="8_35"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "value=['\"]([0-9]+_[0-9]+)['\"]",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(decisionText);
        if (matcher.find()) {
            String blueprintId = matcher.group(1);
            logger.debug("Extracted blueprint {} from decision text", blueprintId);
            return blueprintId;
        }

        return null;
    }

    // ====================================================================
    // V70 helpers (Steve, 2026-05-12) — Universal one-weapon-per-character.
    //
    // Per Steve: when a card's game text pulls or deploys a weapon or device,
    // extract the criteria from that game text (don't hardcode keyword maps).
    // Search Rando friendlies comprehensively (title, lore, gametext, dynamic
    // card types, subtype, icons, keywords, persona) and check whether the
    // pull would safely land on an unarmed character. If every "applicable"
    // friendly is armed → block.
    // ====================================================================

    /**
     * Extracts the deploy-restriction criteria word from a card's game text.
     * Matches patterns like "Deploy on a Sith.", "Deploys only on Vader."
     * Returns the lower-cased criteria word, or null if none found.
     */
    // V115 (Steve, 2026-05-22): widened visibility so DeployEvaluator's V67aq can
    // reuse the same criteria parser for hand-deploy weapon scoring.
    static String v70ExtractDeployCriteria(String gameText) {
        if (gameText == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?i)deploys?\\s+(?:only\\s+)?on\\s+(?:a|an|the|your)?\\s*([a-z][a-z\\s'-]{2,30}?)\\s*[.,;]"
        ).matcher(gameText);
        if (m.find()) {
            String c = m.group(1).trim();
            if (!c.isEmpty()) return c.toLowerCase(java.util.Locale.ROOT);
        }
        return null;
    }

    /**
     * Returns true if the given character's attributes contain the criteria
     * word in title, lore, game text, dynamic card types (engine-aware),
     * subtype, icons, keywords, or personas.
     */
    // V115 (Steve, 2026-05-22): widened visibility so DeployEvaluator's V67aq can
    // reuse the same criteria-matcher for hand-deploy weapon scoring.
    static boolean v70CharacterMatchesCriteria(SwccgGame game, GameState gs, PhysicalCard pc, String criteria) {
        if (pc == null || pc.getBlueprint() == null || criteria == null || criteria.isEmpty()) return false;
        SwccgCardBlueprint bp = pc.getBlueprint();
        String c = criteria.toLowerCase(java.util.Locale.ROOT);

        // 1. Title
        if (bp.getTitle() != null && bp.getTitle().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
        // 2. Lore
        if (bp.getLore() != null && bp.getLore().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
        // 3. Game text
        if (bp.getGameText() != null && bp.getGameText().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
        // 4. Dynamic card types (includes modifier-added types — e.g., Revenge Of
        //    The Sith adds CardType.SITH to Lord Sidious at runtime)
        java.util.Set<com.gempukku.swccgo.common.CardType> types = null;
        try {
            if (game != null && gs != null) {
                types = game.getModifiersQuerying().getCardTypes(gs, pc);
            }
        } catch (Exception ignored) { }
        if (types == null) types = bp.getCardTypes();
        if (types != null) {
            for (com.gempukku.swccgo.common.CardType ct : types) {
                String n = (ct.getHumanReadable() != null) ? ct.getHumanReadable() : ct.name();
                if (n.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').contains(c)) return true;
            }
        }
        // 5. Subtype
        com.gempukku.swccgo.common.CardSubtype st = bp.getCardSubtype();
        if (st != null) {
            String n = (st.getHumanReadable() != null) ? st.getHumanReadable() : st.name();
            if (n.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').contains(c)) return true;
        }
        // 6. Personas
        java.util.Set<com.gempukku.swccgo.common.Persona> personas = bp.getPersonas();
        if (personas != null) {
            for (com.gempukku.swccgo.common.Persona p : personas) {
                if (p.name().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
            }
        }
        // 7. Icons (iterate enum, check hasIcon)
        for (com.gempukku.swccgo.common.Icon icon : com.gempukku.swccgo.common.Icon.values()) {
            if (icon.name().toLowerCase(java.util.Locale.ROOT).contains(c)) {
                if (bp.hasIcon(icon)) return true;
            }
        }
        // 8. Keywords (iterate enum, check hasKeyword)
        for (com.gempukku.swccgo.common.Keyword kw : com.gempukku.swccgo.common.Keyword.values()) {
            if (kw.name().toLowerCase(java.util.Locale.ROOT).contains(c)) {
                if (bp.hasKeyword(kw)) return true;
            }
        }
        return false;
    }

    /**
     * Counts friendlies for the V70 weapon block.
     * Returns int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed}
     * where "matching" means the character's attributes contain the criteria
     * word (or, if criteria is null, every friendly counts as matching).
     */
    private static int[] v70CountFriendlies(SwccgGame game, String playerId, String criteria) {
        int matchingArmed = 0, matchingUnarmed = 0, totalArmed = 0, totalUnarmed = 0;
        if (game == null || playerId == null) {
            return new int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed};
        }
        GameState gs = game.getGameState();
        if (gs == null) {
            return new int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed};
        }
        try {
            for (PhysicalCard pc : gs.getAllPermanentCards()) {
                if (pc == null || pc.getBlueprint() == null) continue;
                if (!playerId.equals(pc.getOwner())) continue;
                com.gempukku.swccgo.common.Zone z = pc.getZone();
                if (z == null || !z.isInPlay()) continue;
                if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                boolean armed = false;
                java.util.List<PhysicalCard> atts = gs.getAttachedCards(pc);
                if (atts != null) {
                    for (PhysicalCard a : atts) {
                        if (a != null && a.getBlueprint() != null
                                && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                            armed = true;
                            break;
                        }
                    }
                }
                if (armed) totalArmed++; else totalUnarmed++;
                if (criteria == null || v70CharacterMatchesCriteria(game, gs, pc, criteria)) {
                    if (armed) matchingArmed++; else matchingUnarmed++;
                }
            }
        } catch (Exception ignored) { }
        return new int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed};
    }

    /**
     * Returns the V70 block reason if the candidate (weapon or device) should
     * be hard-blocked, else null. The candidate's game text is parsed for
     * deploy criteria; matching friendlies are checked for armed status.
     */
    private static String v70CheckWeaponDeviceBlock(SwccgGame game, String playerId,
                                                     CardCategory candidateCategory,
                                                     SwccgCardBlueprint candidateBp) {
        if (candidateCategory != CardCategory.WEAPON && candidateCategory != CardCategory.DEVICE) return null;
        if (candidateBp == null || game == null || playerId == null) return null;

        String criteria = v70ExtractDeployCriteria(candidateBp.getGameText());
        int[] counts = v70CountFriendlies(game, playerId, criteria);
        int matchingArmed = counts[0], matchingUnarmed = counts[1], totalArmed = counts[2], totalUnarmed = counts[3];

        // STRICT: criteria parsed AND all matching are armed → BLOCK
        if (criteria != null && matchingArmed > 0 && matchingUnarmed == 0) {
            return String.format("every applicable '%s' friendly (%d) already armed", criteria, matchingArmed);
        }
        // DEFENSIVE: criteria parsed AND no friendlies matched, but some armed (engine may have broader interp)
        if (criteria != null && matchingArmed == 0 && matchingUnarmed == 0 && totalArmed > 0) {
            return String.format("no '%s' friendly matched our comprehensive search but %d friendly char(s) armed (defensive — engine may have broader interpretation)", criteria, totalArmed);
        }
        // V72 (Steve, 2026-05-15): RELAXED criteria==null fallback.
        // Previous rule blocked ALL weapon pulls once any friendly was armed,
        // even when unarmed friendlies existed. That left Yoda/Leia/Hera/Obi
        // unarmed in the May 15 game once Sabine got Ahsoka's Shoto. Now:
        //   - If any unarmed friendly exists → ALLOW (weapon will land on
        //     an unarmed char, or if it lands on an armed char V72 redistribute
        //     transfers it to the unarmed buddy next turn)
        //   - If ALL friendlies are armed → BLOCK (truly no place to put it)
        if (criteria == null && totalUnarmed == 0 && totalArmed > 0) {
            return String.format("no parseable deploy criteria and ALL %d friendly char(s) armed — no unarmed target", totalArmed);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ SECTION: BATTLE-3 — Damage & Forfeit (reorg 2026-07-06) ═══
    // Owns: v159ForfeitScore 4-step picker (below) + V161/V178-forfeit/V154/V118/V150.
    // Callable from RESPONSE (Rando defends inside opponent battles). NO-PASS context:
    // the damage segment legally forbids passing with obligations pending.
    // Hub: V159 LIVE (this helper; called from BOTH forfeit handlers so the same
    // situation gets the same score). KIND mix + key magnitudes: ORDERING via
    // deliberately-strong additive bands — V154 lose-attached-weapon +2000/+2200,
    // hit-forfeit +1500 tier, V161 immune-forfeit 1500+savings*80-waste*30,
    // V118 +200/-500 small-damage, V178 -10 armed tiebreaker.
    // Absorbs (dead, commented near the two call sites — revert path, do not delete):
    // V67t, V67bd, V67bh, V143, V145, V146, V139-heavy (the eleven
    // `if (false /* V159 SUPERSEDED */)` taped-off branches).
    // Cross-refs: FORCE-LOSS (V153 owns the lose-Force side of the combined prompt),
    // BATTLE-1/BATTLE-2 (upstream), RESPONSE router.
    // See resources/RANDO_REORG_PLAN_2026-07-02.md §3 +
    // Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════
    // ====================================================================
    // === V159 (Steve, 2026-05-31): UNIFIED FORFEIT PICKER (FORFEIT_SPEC v3) ===
    // Replaces / dominates V143 / V67bh / V67t / V139-small / V146 (+150 drift)
    // via additive magnitude. Called by BOTH evaluateForfeit AND
    // evaluateForceLossOrForfeit so the same situation gets the same score
    // (kills the +150-vs-+1500 hit-first drift the helper review flagged).
    // Per /tmp/FORFEIT_SPEC.md v3 (Steve's "damage >= 3 -> forfeit considered"):
    //   Step 1: hit/dead -> forfeit first (defers to Step 2 when attrition owed)
    //   Step 2: attrition owed -> forfeit mandatory; release valve for game-winner +
    //           small attrition (lose Force instead so a Vader isn't sacrificed to 1 attrition)
    //   Step 3: pure damage (attrition=0):
    //             damage >= 3 -> forfeit on the table, V139 protection MUST yield
    //             damage <  3 -> protect, lose Force instead
    //   Step 4: immune-to-attrition (un-hit immune) -> damage coverage only
    // Magnitudes deliberately STRONG so this rule dominates without disabling old code.
    // Replay xifjb2j8dsn74kh1 (turn 5: 7 attr + 8 dmg; Rando burned 8 cards paying damage
    // BEFORE forfeiting for attrition) and l3wvdgfkfyd2gdl9 (pure-damage decisions where
    // pile +330 beat forfeit -135 because V139 over-protected Blizzard 1 fv=7) -- V159
    // makes the efficient forfeit win in both classes of bug.
    // ====================================================================
    private static float v159ForfeitScore(PhysicalCard card, int attrition, int damage,
            com.gempukku.swccgo.game.SwccgGame game, String playerId) {
        if (card == null || card.getBlueprint() == null) return 0f;
        SwccgCardBlueprint bp = card.getBlueprint();

        boolean isHit = card.isHit();
        boolean isDead = false;
        try {
            if (game != null && playerId != null) {
                isDead = com.gempukku.swccgo.ai.common.AiCardHelper.isDeadCard(card, game, playerId);
            }
        } catch (Exception ignore) { /* assume not dead */ }

        Float fvF = (bp.hasForfeitAttribute()) ? bp.getForfeit() : null;
        float fv = (fvF != null) ? fvF : 0f;

        // V178 (Steve, 2026-06): ARMED characters are slightly forfeit-protected.
        // Lightsabers kept dying with their carriers (Tyranus/Sidious forfeits), and
        // each lost saber costs the drain bonus + hit potential until re-pulled.
        // Steve: "maybe just a +10 weight though. Weapons are worth something but not
        // everything." -10 on the forfeit score = prefer forfeiting the unarmed body
        // when otherwise tied; never overrides real factors (fv/hit/immunity ~60-1500).
        float v178Armed = 0f;
        try {
            if (game != null && game.getGameState() != null) {
                for (PhysicalCard v178W : game.getGameState().getAllPermanentCards()) {
                    if (v178W != null && v178W.getAttachedTo() == card
                            && v178W.getBlueprint() != null
                            && v178W.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                        v178Armed = -10f;
                        break;
                    }
                }
            }
        } catch (Exception ignore) { }

        // 2026-06-02 ENGINE-BACKED IMMUNITY (Steve): replaced the regex/substring
        // detection on game text with the engine's live modifier-state query —
        // the SAME call GuiUtils.isImmuneToRemainingAttrition uses to change the
        // attrition icon in the UI. Advantages over the regex:
        //   • Catches dynamic immunity granted by other cards on the table
        //     (e.g., "while Vader present, all Sith immune to attrition" on a
        //     companion card). Regex only saw self-text and missed table-driven
        //     immunity.
        //   • Catches conditional immunity that already evaluated to true via
        //     engine condition resolution (e.g., "while X" or "if Y").
        //   • No phrase parsing needed — the engine has already normalized
        //     "Immune to attrition < N" / "Immune to attrition of exactly N" /
        //     "Immunity to attrition capped at N" into numeric modifier values.
        //   • No false positives from text mentioning ANOTHER character's
        //     immunity (e.g., Bib Fortuna's "Jabba is immune to attrition" used
        //     to make Bib wrongly immune; now it correctly attributes the
        //     immunity to Jabba and Bib reads as not-immune).
        // Logic mirrors GuiUtils.isImmuneToRemainingAttrition (logic/timing/
        // GuiUtils.java lines 158-171): exact-immunity takes precedence — if
        // the card is immune to EXACTLY N attrition, it's immune only when
        // current attrition == N. Otherwise fall back to the less-than
        // immunity threshold — immune when threshold > attrition.
        // Game-null fallback: assume not immune (no engine state = no proof of
        // immunity = forfeit branch is correct default).
        boolean isImmune = false;
        if (game != null) {
            try {
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying mq = game.getModifiersQuerying();
                com.gempukku.swccgo.game.state.GameState gs = game.getGameState();
                if (mq != null && gs != null) {
                    float exactImmunity = mq.getImmunityToAttritionOfExactly(gs, card);
                    if (exactImmunity > 0f) {
                        isImmune = (exactImmunity == attrition);
                    } else {
                        float lessThanImmunity = mq.getImmunityToAttritionLessThan(gs, card);
                        isImmune = lessThanImmunity > attrition;
                    }
                }
            } catch (Exception ignore) { /* assume not immune on error */ }
        }

        // STEP 1: hit/dead -> forfeit first (lost anyway). Slight defer when attrition is
        // owed so a Step-2 attrition-cheapest can win for a specific covering forfeit.
        if (isHit)  return attrition > 0 ? 1500f : 3000f;
        if (isDead) return attrition > 0 ? 1200f : 2500f;

        // STEP 4 short-circuit: immune un-hit + attrition owed (immune can only cover damage)
        if (isImmune && attrition > 0) {
            if (damage > 0 && fv > 0) {
                int savings = (int) Math.min(fv, damage);
                int waste = (int) Math.max(0, fv - damage);
                // V161 (Steve, 2026-05-29): damage >= 4 -> forfeit the immune char/ship to
                // cover. Steve's rule: "He should choose to lose character or ship from
                // battle to cover damage if damage is 4 or more EVEN IF immune to attrition."
                // Last game: Rando lost a lot of damage when he could have lost a ship to
                // cover most of it. Old -500 penalty made fv=7 vs damage=10 score net -80,
                // losing to pile +150. STEP 3-style score (1500 + savings*80 - waste*30)
                // now wins. Gated on savings >= 3 (mirrors STEP 3's coverage floor) so a
                // fv=1 immune card isn't forfeited for trivial coverage.
                if (damage >= 4 && savings >= 3) {
                    return 1500f + savings * 80f - waste * 30f;
                }
                // V161 UPDATE (Steve, 2026-06-17): the old cautious return scored a high-fv
                // SOLO immune character NEGATIVE on small damage (Yoda fv7 / dmg2 -> -580),
                // so Rando bled Force one point at a time across a losing battle instead of
                // forfeiting once. Immunity covers ATTRITION, but BATTLE DAMAGE is still
                // Force lost every turn the solo immune body sits in a fight it loses. So
                // forfeit a SOLO immune character scaled by how OUT-POWERED he is at the
                // site (gap = opp power - our power): solo-vs-army forfeits hard, solo-vs-
                // solo barely leans, not-out-powered keeps him. Grouped immune chars and
                // query failures fall through to the old cautious return unchanged. Replay
                // sb2xzfjfpk5jxt8v: solo immune Yoda bled ~4 Force to Dooku one point at a
                // time instead of one fv-7 forfeit ending it.
                try {
                    PhysicalCard v161Loc = card.getAtLocation();
                    if (v161Loc != null && game != null) {
                        com.gempukku.swccgo.game.state.GameState v161Gs = game.getGameState();
                        com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying v161Mq =
                            game.getModifiersQuerying();
                        String v161Opp = game.getOpponent(playerId);
                        if (v161Gs != null && v161Mq != null && v161Opp != null) {
                            int v161Friendly = 0;
                            for (PhysicalCard v161C : v161Gs.getCardsAtLocation(v161Loc)) {
                                if (v161C != null && playerId.equals(v161C.getOwner())
                                        && v161C.getBlueprint() != null
                                        && v161C.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                    v161Friendly++;
                                }
                            }
                            if (v161Friendly <= 1) {  // SOLO
                                float v161Gap = v161Mq.getTotalPowerAtLocation(v161Gs, v161Loc, v161Opp, false, false)
                                              - v161Mq.getTotalPowerAtLocation(v161Gs, v161Loc, playerId, false, false);
                                if (v161Gap > 0f) {
                                    // gap 1-2 stays at/below the +350 pile loss (keep, tiny lean);
                                    // gap 3+ beats it (forfeit); capped below the +1500 mandatory tier.
                                    return Math.min(1200f, 100f + v161Gap * 120f) + v178Armed;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) { /* fall through to cautious */ }
                // Grouped, not-out-powered, or query failed: stay cautious (immune can still
                // cover but the forfeit's not worth the board piece here).
                return savings * 60f - waste * 40f - 500f;
            }
            return -2500f;
        }

        // STEP 2: attrition owed (un-hit, non-immune) -> forfeit MANDATORY
        if (attrition > 0) {
            // Release valves: protect deck-defining investments. Replay 360z5sh5jruys8p7
            // (Gall battle): Rando forfeited First Light (Capital starship, deck centerpiece)
            // for 5 attrition because the old release valve only fired for game-winner
            // CHARACTERS (power>=6 + ability>=4). Capital ships have no ability attribute,
            // so the valve never engaged. Extended to:
            //   - Capital starship (CardSubtype.CAPITAL) -> strong protect (-1000) so any
            //     cheaper character forfeit wins; only forfeited when truly the sole option.
            //   - AiPriorityCards registered card -> strong protect (-1000).
            //   - Game-winner character + attrition <= 2 -> -1500 (existing).
            // The -1000 magnitude lets cheap characters (V159 step-2 ~+1700 with cheap bonus)
            // dominate, so the deck centerpiece is forfeited only when forced.
            Float power = bp.hasPowerAttribute() ? bp.getPower() : null;
            Float ability = bp.hasAbilityAttribute() ? bp.getAbility() : null;
            boolean gameWinner = (power != null && power >= 6f
                    && ability != null && ability >= 4f);
            boolean isCapitalShip = false;
            try {
                isCapitalShip = (bp.getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.CAPITAL);
            } catch (Exception ignore) { /* false */ }
            boolean isPriority = false;
            try {
                isPriority = card.getTitle() != null
                        && com.gempukku.swccgo.ai.common.AiPriorityCards.isPriorityCardByTitle(card.getTitle());
            } catch (Exception ignore) { /* false */ }

            if ((isCapitalShip || isPriority) && fv < attrition) {
                // 2026-06-02 RELEASE-VALVE NARROWING (Steve, Bossk replay):
                // the old check fired the -1000 release valve unconditionally
                // for any CAPITAL ship / priority card with attrition owed.
                // Result: Bossk (fv=6, attr=6) got -1000, pile loss +150 won,
                // Rando bled 9 force from reserve and STILL had to forfeit the
                // ship for the residual attrition next time. When fv >= attrition
                // the ship's forfeit fully covers attrition AND absorbs damage
                // in one move — that's the efficient play even for a deck
                // centerpiece. Only protect when the forfeit doesn't even cover
                // attrition (fv < attrition), which means the ship would die for
                // a partial payment and Rando STILL owes more attrition.
                return -1000f;  // partial-payment loss — protect, save the ship
            }
            if (gameWinner && attrition <= 2) {
                return -1500f;  // small attrition + game-winner char -> lose Force instead
            }
            int total = attrition + damage;
            int coverage = (int) Math.min(fv, total);
            float score = 1500f + coverage * 100f;
            if (fv >= total) score += 300f;         // covers all -> bonus
            if (fv >= 1 && fv <= 3) score += 200f;  // cheap forfeit preferred for attrition
            return score + v178Armed;
        }

        // STEP 3: pure damage (attrition == 0)
        if (damage <= 0) return 0f;
        if (damage < 3) return -3000f;  // damage too small to justify forfeit (V143-style hard protect)

        // damage >= 3: forfeit is ON THE TABLE; V139 protections MUST yield (per spec)
        int savings = (int) Math.min(fv, damage);
        int waste = (int) Math.max(0, fv - damage);
        if (savings < 3) return -800f;  // forfeit doesn't soak enough; prefer Force loss

        float score = 1500f + savings * 80f - waste * 30f;
        if (savings >= damage / 2) score += 200f;  // significant chunk of the damage
        return score + v178Armed;
    }
}