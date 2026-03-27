package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action Text Evaluator
 *
 * Handles text-based action ranking by pattern matching action text.
 * Ported from Python action_text_evaluator.py (~1350 lines)
 *
 * This evaluator provides baseline rankings for common SWCCG actions
 * based on analyzing the action text.
 */
public class ActionTextEvaluator extends ActionEvaluator {

    // Rank deltas (from Python)
    private static final float VERY_GOOD_DELTA = 50.0f;
    private static final float GOOD_DELTA = 30.0f;
    private static final float BAD_DELTA = -30.0f;
    private static final float VERY_BAD_DELTA = -50.0f;

    // Pattern for extracting blueprint ID from action text HTML
    private static final Pattern BLUEPRINT_PATTERN = Pattern.compile("value='([^']+)'");

    // Track barriered targets to avoid playing multiple barriers on same card
    private Set<String> barrieredTargets = new HashSet<>();
    private int barrierTurn = 0;

    // V39: Track IHYN plays per battle — only play once per battle
    private boolean ihynPlayedThisBattle = false;
    private int ihynBattleId = -1;

    public ActionTextEvaluator() {
        super("ActionText");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();

        // Handle CARD_ACTION_CHOICE and ACTION_CHOICE
        if ("CARD_ACTION_CHOICE".equals(decisionType) || "ACTION_CHOICE".equals(decisionType)) {
            return true;
        }

        // Also handle MULTIPLE_CHOICE for capacity slot decisions, Epic Event choices,
        // and the critical "not activated Force" confirmation
        if ("MULTIPLE_CHOICE".equals(decisionType)) {
            String decisionText = context.getDecisionText();
            if (decisionText != null) {
                String dtLower = decisionText.toLowerCase();
                if (dtLower.contains("capacity slot") || dtLower.contains("choose an option")
                    || dtLower.contains("not activated force") || dtLower.contains("have not activated")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();
        Set<String> blocked = context.getBlockedResponses();

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String cardId = i < cardIds.size() ? cardIds.get(i) : null;
            String textLower = actionText.toLowerCase();

            EvaluatedAction action = new EvaluatedAction(actionId, ActionType.UNKNOWN, 0.0f, actionText);

            // Check if this response is blocked (loop prevention)
            if (blocked.contains(actionId) || blocked.contains(actionText)) {
                action.addReasoning("BLOCKED (loop prevention)", -200.0f);
                logger.debug("Blocked action: {}", actionText);
            }

            // ========== V38.3: "Not activated Force" — ALWAYS go back and activate ==========
            // The game asks "You have not activated Force. Do you want to Pass?"
            // Options: "Yes" (pass without activating) and "No" (go back and activate)
            // ALWAYS choose "No" — Force is essential for deploying characters.
            {
                String decisionTextCheck = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase() : "";
                if (decisionTextCheck.contains("not activated force") || decisionTextCheck.contains("have not activated")) {
                    if (textLower.equals("no")) {
                        action.addReasoning("V38.3 MUST ACTIVATE: Go back and activate Force!", 9999.0f);
                        logger.warn("V38.3 MUST ACTIVATE: Choosing 'No' to go back and activate Force");
                    } else if (textLower.equals("yes")) {
                        action.addReasoning("V38.3 NEVER SKIP ACTIVATION: Do not pass without activating!", -9999.0f);
                        logger.warn("V38.3 BLOCKED: Refusing to skip Force activation");
                    }
                }
            }

            // ========== Skip ALL Deploy Actions ==========
            // Deploy actions should be handled EXCLUSIVELY by DeployEvaluator.
            if (actionText.equals("Deploy") ||
                (actionText.startsWith("Deploy ") && !textLower.contains("from"))) {
                // Skip this action - let DeployEvaluator handle it
                continue;
            }

            // ========== V24.4: LOCATIONS FIRST — DEPLOY LOCATIONS BEFORE ANYTHING ELSE ==========
            // Locations MUST be deployed before activating effects (AMSD, K&D, etc.).
            // If the bot has ANY location in hand, penalize all non-deploy actions heavily
            // so that deploy actions (handled by DeployEvaluator) always win priority.
            if (gameState != null && context.getPhase() == Phase.DEPLOY) {
                java.util.List<com.gempukku.swccgo.game.PhysicalCard> hand = context.getHand();
                if (hand != null) {
                    boolean hasLocationInHand = false;
                    for (com.gempukku.swccgo.game.PhysicalCard handCard : hand) {
                        if (handCard != null && handCard.getBlueprint() != null &&
                            handCard.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.LOCATION) {
                            hasLocationInHand = true;
                            break;
                        }
                    }
                    if (hasLocationInHand) {
                        // Check if this action is a search that PULLS locations (TDIGWATT, I'm Sorry, etc.)
                        // Those are OK — they help GET locations. But effect activations like AMSD should wait.
                        // V24.9: Added "sorry" — I'm Sorry deploys interior CC sites from reserve!
                        boolean isLocationSearch = textLower.contains("bespin") || textLower.contains("location")
                            || textLower.contains("cloud city") || textLower.contains("site")
                            || textLower.contains("sorry");
                        // V24.15: Exempt AMSD from LOCATIONS FIRST penalty!
                        // AMSD deploys a Star Destroyer — it's effectively a deploy action, not an "effect".
                        // When Bespin is already on the table, AMSD should fire immediately to get Executor there.
                        boolean isAmsdAction = textLower.contains("alert my star destroyer") ||
                            textLower.contains("amsd") ||
                            (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")) ||
                            (textLower.contains("star destroyer") && textLower.contains("deploy both"));
                        if (!isLocationSearch && !isAmsdAction) {
                            action.addReasoning("V24.4 LOCATIONS FIRST: Deploy locations in hand before activating effects!", -800.0f);
                            logger.warn("V24.4 LOCATIONS FIRST: Penalizing '{}' — location in hand needs deploying first! (-800)", actionText);
                        } else if (isAmsdAction) {
                            logger.warn("V24.15 AMSD EXEMPT: Not penalizing AMSD with LOCATIONS FIRST — AMSD deploys a Star Destroyer!");
                        }
                    }
                }
            }

            // ========== V23: EMPTY PILE GUARD ==========
            // Block interrupts/actions that search piles which are empty.
            // Sith Fury on turn 1 wastes 4 force searching an empty Lost Pile.
            if (gameState != null) {
                String pid = context.getPlayerId();

                // === V29.14: NO ESCAPE — "Take top card of Lost Pile into hand" ===
                // This is FREE card advantage (not a search), works with any pile size >= 1.
                // Must be checked BEFORE the V23 empty pile guard so it doesn't get penalized.
                if (textLower.contains("take top card") && textLower.contains("lost pile")) {
                    int lostSize = gameState.getLostPile(pid).size();
                    if (lostSize > 0) {
                        action.addReasoning("V29.14 NO ESCAPE: Free card from Lost Pile — always take it!", 200.0f);
                        logger.warn("V29.14 NO ESCAPE: '{}' — Lost Pile has {} cards, taking top card!", actionText, lostSize);
                        actions.add(action);
                        continue;
                    }
                }

                // Lost Pile searches
                if (textLower.contains("lost pile") && (textLower.contains("take") ||
                    textLower.contains("search") || textLower.contains("retrieve"))) {
                    int lostSize = gameState.getLostPile(pid).size();
                    if (lostSize == 0) {
                        action.addReasoning("V23 EMPTY PILE: Lost Pile is empty — search will fail!", -300.0f);
                        logger.warn("V23 EMPTY PILE GUARD: Blocking '{}' — Lost Pile is empty!", actionText);
                        actions.add(action);
                        continue;
                    } else if (lostSize <= 2) {
                        action.addReasoning("V23 LOW PILE: Lost Pile only has " + lostSize + " cards — risky search", -100.0f);
                        logger.warn("V23 LOW PILE: '{}' — Lost Pile only has {} cards", actionText, lostSize);
                    }
                }
                // Used Pile searches
                if (textLower.contains("used pile") && (textLower.contains("take") ||
                    textLower.contains("search"))) {
                    int usedSize = gameState.getUsedPile(pid).size();
                    if (usedSize == 0) {
                        action.addReasoning("V23 EMPTY PILE: Used Pile is empty — search will fail!", -300.0f);
                        logger.warn("V23 EMPTY PILE GUARD: Blocking '{}' — Used Pile is empty!", actionText);
                        actions.add(action);
                        continue;
                    }
                }
            }

            // ========== V24: AMSD BESPIN GATE ==========
            // Alert My Star Destroyer needs a system location to deploy the Star Destroyer to.
            // If Bespin isn't on the table yet, AMSD has nowhere to send the ship — block it.
            if (gameState != null && (textLower.contains("alert my star destroyer") ||
                textLower.contains("amsd") ||
                (textLower.contains("star destroyer") && textLower.contains("deploy both")) ||
                (textLower.contains("star destroyer") && textLower.contains("pilot") && textLower.contains("deploy")) ||
                (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")))) {
                boolean bespinSystemOnTable = false;
                try {
                    for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                        if (loc != null && loc.getTitle() != null &&
                            loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                            loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                            loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                            bespinSystemOnTable = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V24 AMSD gate: Error checking Bespin: {}", e.getMessage());
                }
                if (!bespinSystemOnTable) {
                    action.addReasoning("V24 AMSD BLOCKED: No Bespin system on table — Star Destroyer has nowhere to deploy!", -9999.0f);
                    logger.warn("V24 AMSD GATE: HARD BLOCKING AMSD — Bespin system not on table yet! (-9999)");
                    actions.add(action);
                    continue;
                }
            }

            // ========== V24.10: AMSD — PIETT + EXECUTOR ONLY ==========
            // AMSD should ONLY fire when Piett is the pilot AND Executor is in reserve.
            // No other pilot (Chiraneau, Ozzel, Motti, Evazan, etc.) should use AMSD.
            // If Piett isn't the target or Executor isn't in reserve, block AMSD entirely.
            // AMSD can only be used TWICE per game — never waste an attempt!
            if (gameState != null && (textLower.contains("alert my star destroyer") ||
                textLower.contains("amsd") ||
                (textLower.contains("star destroyer") && textLower.contains("deploy both")) ||
                (textLower.contains("star destroyer") && textLower.contains("pilot") && textLower.contains("deploy")) ||
                (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")))) {

                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle amsdOracle = context.getDeckOracle();
                int currentTurn = context.getTurnNumber();

                // V24.10: Check if AMSD already failed this turn — don't waste a second attempt.
                // AMSD can only be used twice per game, so every attempt must count.
                // If it failed, Piett/Executor aren't in the right zones yet.
                // Wait for recirculation on the next turn.
                if (amsdOracle != null && amsdOracle.hasAmsdFailedThisTurn(currentTurn)) {
                    action.addReasoning("V24.10 AMSD BLOCKED: Already failed this turn — save for next turn after recirculation!", -9999.0f);
                    logger.warn("V24.10 AMSD RETRY BLOCK: AMSD already failed on turn {} — don't waste another attempt!", currentTurn);
                    actions.add(action);
                    continue;
                }

                // V24.10: AMSD pilot check — two scenarios:
                // 1. Action text names a specific pilot (e.g., "deploy Piett's matching Star Destroyer")
                //    → Check if it's Piett. Block if not.
                // 2. Action text is generic (e.g., "Reveal pilot or Star Destroyer from hand")
                //    → Check DeckOracle: is Piett in hand AND Executor in reserve? If so, ALLOW.
                //    The actual pilot selection happens in CardSelectionEvaluator's AMSD guard.
                boolean isGenericReveal = textLower.contains("reveal") && !textLower.contains("piett")
                    && !textLower.contains("vader") && !textLower.contains("chiraneau")
                    && !textLower.contains("ozzel") && !textLower.contains("motti");

                if (isGenericReveal) {
                    // Generic "Reveal pilot or Star Destroyer from hand" — use DeckOracle to decide
                    if (amsdOracle != null && amsdOracle.isAnalyzed()) {
                        boolean piettInHand = amsdOracle.isCardInHand("Admiral Piett") || amsdOracle.isCardInHand("Piett");
                        boolean executorInReserve = amsdOracle.isCardInReserve("Executor") ||
                            amsdOracle.isCardInReserve("Flagship Executor");
                        // V29.4: AMSD deploys Star Destroyer from HAND or RESERVE DECK!
                        // Previous code blocked when Executor was in hand — that was WRONG.
                        // AMSD is actually the BEST way to deploy Executor from hand because
                        // it deploys Piett+Executor simultaneously to the same system.
                        boolean executorInHand = amsdOracle.isCardInHand("Executor") ||
                            amsdOracle.isCardInHand("Flagship Executor");
                        boolean executorAvailable = executorInReserve || executorInHand;

                        // V29.4: Diagnostic logging — trace exactly what DeckOracle sees
                        logger.warn("V29.4 AMSD DIAGNOSTIC: piettInHand={}, executorInReserve={}, executorInHand={}, executorAvailable={}",
                            piettInHand, executorInReserve, executorInHand, executorAvailable);

                        if (piettInHand && executorAvailable) {
                            // Piett + Executor available (in hand or reserve). ALLOW AMSD, boost it!
                            // V24.15: On turn 1-2, AMSD is CRITICAL — must fire immediately after Bespin!
                            // Later turns: still high priority but less urgent.
                            float amsdBoost = 500.0f;
                            String source = executorInHand ? "hand" : "reserve";
                            if (currentTurn <= 2) {
                                amsdBoost = 1500.0f;  // V24.15: Mega-boost on early turns — Executor MUST deploy ASAP!
                                action.addReasoning("V24.15 AMSD MEGA PRIORITY: Turn " + currentTurn + " — Executor (from " + source + ") MUST deploy NOW to control Bespin!", amsdBoost);
                                logger.warn("V24.15 AMSD MEGA PRIORITY: Turn {} — Piett in hand + Executor in {} — mega-boost +{} to ensure AMSD fires!", currentTurn, source, amsdBoost);
                            } else {
                                action.addReasoning("V24.10 AMSD APPROVED: Piett + Executor (from " + source + ") ready — fire AMSD!", amsdBoost);
                                logger.warn("V24.10 AMSD: Generic reveal — Piett in hand, Executor in {} — APPROVED (+{})!", source, amsdBoost);
                            }
                        } else if (!piettInHand) {
                            action.addReasoning("V24.10 AMSD BLOCKED: Piett NOT in hand — can't use AMSD!", -9999.0f);
                            logger.warn("V24.10 AMSD BLOCK: Generic reveal but Piett not in hand — block!");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        } else {
                            // V29.4: Executor not in hand OR reserve — truly unavailable
                            // Could be in force pile, used pile, lost pile, or not in deck
                            action.addReasoning("V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve (may be in force/used pile)!", -9999.0f);
                            logger.warn("V29.4 AMSD BLOCK: Piett in hand but Executor not available (not in hand or reserve) — might be activated to force pile!");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        }
                    }
                    // If oracle unavailable, allow generic reveal (best guess)
                } else if (!textLower.contains("piett")) {
                    // Specific pilot named in action text but it's NOT Piett — hard block
                    action.addReasoning("V24.10 AMSD BLOCKED: Only Piett may use AMSD — " +
                        "this action targets a different pilot!", -9999.0f);
                    logger.warn("V24.10 AMSD HARD BLOCK: Action does NOT target Piett — only Piett + Executor allowed!");
                    if (amsdOracle != null) {
                        amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                    }
                    actions.add(action);
                    continue;
                } else {
                    // Action specifically names Piett — verify Piett in hand AND Executor available
                    if (amsdOracle != null && amsdOracle.isAnalyzed()) {
                        boolean piettInHand = amsdOracle.isCardInHand("Admiral Piett") || amsdOracle.isCardInHand("Piett");
                        boolean executorInReserve = amsdOracle.isCardInReserve("Executor") ||
                            amsdOracle.isCardInReserve("Flagship Executor");
                        // V29.4: AMSD deploys from HAND or RESERVE — check both!
                        boolean executorInHand = amsdOracle.isCardInHand("Executor") ||
                            amsdOracle.isCardInHand("Flagship Executor");
                        boolean executorAvailable = executorInReserve || executorInHand;

                        // V29.4: Diagnostic logging
                        logger.warn("V29.4 AMSD DIAGNOSTIC (specific): piettInHand={}, executorInReserve={}, executorInHand={}, executorAvailable={}",
                            piettInHand, executorInReserve, executorInHand, executorAvailable);

                        if (!piettInHand) {
                            action.addReasoning("V24.10 AMSD BLOCKED: Piett is NOT in hand — can't use AMSD!", -9999.0f);
                            logger.warn("V24.10 AMSD GATE: Piett not in hand — HARD BLOCK");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        }
                        if (!executorAvailable) {
                            // V29.4: Executor not in hand OR reserve — truly unavailable
                            action.addReasoning("V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve!", -9999.0f);
                            logger.warn("V29.4 AMSD GATE: Piett in hand but Executor not available — HARD BLOCK");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        }
                        // Both confirmed — boost AMSD priority!
                        // V24.15: On turn 1-2, mega-boost to ensure Executor deploys ASAP
                        String source = executorInHand ? "hand" : "reserve";
                        float amsdBoostSpecific = (currentTurn <= 2) ? 1500.0f : 500.0f;
                        if (currentTurn <= 2) {
                            action.addReasoning("V24.15 AMSD MEGA PRIORITY: Turn " + currentTurn + " — Executor (from " + source + ") MUST deploy NOW!", amsdBoostSpecific);
                            logger.warn("V24.15 AMSD MEGA PRIORITY (specific): Turn {} — Executor in {} — +{} mega-boost!", currentTurn, source, amsdBoostSpecific);
                        } else {
                            action.addReasoning("V24.10 AMSD APPROVED: Piett + Executor (from " + source + ") ready!", amsdBoostSpecific);
                            logger.warn("V24.10 AMSD APPROVED: Piett in hand + Executor in {} — +{}!", source, amsdBoostSpecific);
                        }
                    }
                    // V29.4: If oracle unavailable, allow AMSD (best guess — don't block without data)
                }
            }

            // ========== V24: TDIGWATT EXHAUSTED SEARCH GUARD ==========
            // TDIGWATT searches for "Cloud City Occupation, Dark Deal, Vader's Bounty, or Bespin".
            // Once all targets have been pulled, every search fails — stop wasting the action.
            if (textLower.contains("cloud city occupation") && textLower.contains("dark deal") &&
                textLower.contains("bespin")) {
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle tdigOracle = context.getDeckOracle();
                if (tdigOracle != null && tdigOracle.isAnalyzed()) {
                    boolean anyTargetInReserve =
                        tdigOracle.isCardInReserve("Bespin") ||
                        tdigOracle.isCardInReserve("Dark Deal") ||
                        tdigOracle.isCardInReserve("Cloud City Occupation") ||
                        tdigOracle.isCardInReserve("Vader's Bounty");
                    if (!anyTargetInReserve) {
                        action.addReasoning("V24 TDIGWATT: All targets already pulled — search will fail!", -400.0f);
                        logger.warn("V24 TDIGWATT EXHAUSTED: All 4 targets (Bespin, Dark Deal, CC Occupation, Vader's Bounty) already pulled — blocking search!");
                        actions.add(action);
                        continue;
                    } else {
                        logger.info("V24 TDIGWATT: Targets still in reserve — search OK");
                    }
                }
            }

            // ========== V24.6B: I'M SORRY LOCATION PULL — USE UNTIL CC SITES EXHAUSTED ==========
            // I'm Sorry (V) deploys interior Cloud City sites from reserve deck.
            // Use EVERY turn until all CC interior sites are pulled from reserve.
            // DeckOracle tracks what's left — stop wasting the action when reserve is empty.
            if (textLower.contains("sorry") || textLower.contains("i'm sorry") ||
                (textLower.contains("interior") && textLower.contains("cloud city") && textLower.contains("site"))) {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer sorryObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (sorryObjAnalyzer != null && sorryObjAnalyzer.isAnalyzed()
                    && sorryObjAnalyzer.needsBespinSystemPresence()) {
                    // Use DeckOracle to check if any CC interior sites remain in reserve
                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle sorryOracle = context.getDeckOracle();
                    boolean ccSitesInReserve = true; // default to true if oracle unavailable
                    if (sorryOracle != null && sorryOracle.isAnalyzed()) {
                        ccSitesInReserve = sorryOracle.isCardInReserve("Cloud City: Upper Walkway")
                            || sorryOracle.isCardInReserve("Cloud City: Carbonite Chamber")
                            || sorryOracle.isCardInReserve("Cloud City: Dining Room")
                            || sorryOracle.isCardInReserve("Cloud City: Lower Corridor")
                            || sorryOracle.isCardInReserve("Cloud City: Security Tower")
                            || sorryOracle.isCardInReserve("Cloud City: West Gallery")
                            || sorryOracle.isCardInReserve("Cloud City: North Corridor")
                            || sorryOracle.isCardInReserve("Cloud City: Platform")
                            || sorryOracle.isCardInReserve("Cloud City: Incinerator")
                            || sorryOracle.isCardInReserve("Cloud City: Guest Quarters")
                            || sorryOracle.isCardInReserve("Cloud City")  // partial match catches any CC site
                            ;
                        logger.warn("V24.6 I'M SORRY: CC interior sites still in reserve? {}", ccSitesInReserve);
                    }
                    if (ccSitesInReserve) {
                        action.addReasoning("V24.6 I'M SORRY: CC sites still in reserve — pull one NOW for more drains + occupation!", 250.0f);
                        logger.warn("V24.6 I'M SORRY: Boosting +250 — CC interior sites available in reserve!");
                    } else {
                        action.addReasoning("V24.6 I'M SORRY: All CC interior sites already pulled — search will fail!", -300.0f);
                        logger.warn("V24.6 I'M SORRY: BLOCKING — no more CC interior sites in reserve deck! (-300)");
                    }
                }
            }

            // ========== V29.7: WE MUST ACCELERATE OUR PLANS ==========
            // Card text: "Use 3 Force to take one Effect... OR Deploy a Blockade Flagship site...
            //             OR Take one Interrupt with 'Podracer(s)'..."
            // RULES:
            //   1. Deploy Blockade Flagship site = the ONLY good use
            //   2. Once that site is already on table, ALL uses of Accelerate are wasteful
            //   3. Effect/interrupt pulls cost 3 Force for minimal value — NEVER use
            //   4. If grabber has grabbed this card, each copy costs +1 more — even worse
            // V29.7 FIX: The action texts from this card are:
            //   "Take Effect into hand from Reserve Deck"
            //   "Deploy a Blockade Flagship site from Reserve Deck"
            //   "Take Interrupt into hand from Reserve Deck"
            // These do NOT contain "accelerate"! Must also identify by source card title.
            boolean isAccelerateCard = textLower.contains("accelerate our plans") || textLower.contains("accelerate");
            if (!isAccelerateCard && cardId != null && gameState != null) {
                // V29.7: Look up the source card to check if it's Accelerate Plans
                try {
                    PhysicalCard sourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (sourceCard != null && sourceCard.getTitle() != null
                        && sourceCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("accelerate")) {
                        isAccelerateCard = true;
                    }
                } catch (Exception e) { /* ignore parse errors */ }
            }
            if (isAccelerateCard) {
                boolean isLocationPull = textLower.contains("location") || textLower.contains("site")
                    || textLower.contains("system") || textLower.contains("deploy a blockade")
                    || textLower.contains("blockade flagship");
                boolean isEffectPull = textLower.contains("effect");
                boolean isInterruptPull = textLower.contains("interrupt");

                // V29: Check if the Blockade Flagship site is already on table
                boolean blockadeSiteOnTable = false;
                GameState accelGs = context.getGameState();
                if (accelGs != null) {
                    try {
                        for (PhysicalCard loc : accelGs.getTopLocations()) {
                            if (loc == null || loc.getBlueprint() == null) continue;
                            String locTitle = loc.getTitle();
                            if (locTitle != null && locTitle.toLowerCase(java.util.Locale.ROOT).contains("blockade flagship")) {
                                blockadeSiteOnTable = true;
                                break;
                            }
                        }
                    } catch (Exception e) { /* ignore */ }
                }

                if (blockadeSiteOnTable) {
                    // Site already deployed — NO reason to use Accelerate anymore
                    action.addReasoning("V29.7 ACCELERATE: Blockade Flagship site already on table — "
                        + "don't waste 3+ Force on effect/interrupt pull!", -400.0f);
                    logger.warn("V29.7 ACCELERATE BLOCKED: BFS already on table — all uses wasteful (-400)");
                } else if (isLocationPull) {
                    // Location/site pull — the only good use
                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle accelOracle = context.getDeckOracle();
                    if (accelOracle != null && accelOracle.isAnalyzed()) {
                        java.util.List<com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard> locsInReserve =
                            accelOracle.getCardsByCategory(com.gempukku.swccgo.common.CardCategory.LOCATION,
                                com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                        if (locsInReserve.isEmpty()) {
                            action.addReasoning("V29.7 ACCELERATE: No locations in reserve — search will FAIL!", -500.0f);
                            logger.warn("V29.7 ACCELERATE BLOCKED: Location pull but NO locations in reserve! (-500)");
                        } else {
                            action.addReasoning("V29.7 ACCELERATE: Deploy Blockade Flagship site — good use!", 100.0f);
                            logger.info("V29.7 ACCELERATE: Location pull — {} locations in reserve", locsInReserve.size());
                        }
                    } else {
                        action.addReasoning("V29.7 ACCELERATE: Location pull (no oracle)", 50.0f);
                    }
                } else {
                    // Effect, interrupt, or any other pull — ALWAYS bad. Costs 3+ Force for low value.
                    action.addReasoning("V29.7 ACCELERATE: DON'T use 3 Force for "
                        + (isEffectPull ? "effect" : isInterruptPull ? "interrupt" : "non-location")
                        + " pull — Accelerate is for the Blockade Flagship site ONLY!", -400.0f);
                    logger.warn("V29.7 ACCELERATE BLOCKED: Non-location pull — waste of 3+ Force (-400)");
                }
            }

            // ========== V29: FORCE PUSH — BATTLE USE ONLY ==========
            // Force Push has two modes:
            //   1. BATTLE: "use 2 Force to target your Dark Jedi and opponent's character...
            //      Both targets are excluded from battle" — GOOD, removes threat
            //   2. FORCE PILE EXCHANGE: "Exchange two cards from hand with any one card
            //      from Force Pile" — BAD, wasteful. You draw Force Pile cards anyway.
            // Also applies to "Force Push & Podracer Collision" combo card.
            // ALWAYS prefer battle use. NEVER use for Force Pile exchange.
            if (textLower.contains("force push")) {
                boolean isBattleUse = textLower.contains("exclude") || textLower.contains("battle")
                    || textLower.contains("target your") || textLower.contains("dark jedi");
                boolean isExchangeUse = textLower.contains("exchange") || textLower.contains("force pile");

                if (isBattleUse && !isExchangeUse) {
                    // Battle exclusion — good use! Removes a threat from battle
                    action.addReasoning("V29 FORCE PUSH: Battle exclusion — remove threat! Good use.", 80.0f);
                    logger.info("V29 FORCE PUSH: Battle use — exclude characters from battle (+80)");
                } else if (isExchangeUse) {
                    // Force Pile exchange — wasteful. You draw those cards anyway.
                    action.addReasoning("V29 FORCE PUSH: DON'T exchange with Force Pile — you draw those cards anyway! Save for battle.", -300.0f);
                    logger.warn("V29 FORCE PUSH BLOCKED: Force Pile exchange is wasteful — save for battle (-300)");
                }
            }

            // ========== V29.8: IAYF VADER-ON-TABLE CHECK (ANY SOURCE) ==========
            // IAYF can deploy Vader's Lightsaber from RESERVE or LOST PILE.
            // The reserve-only check below misses the Lost Pile case.
            // This broader check catches both: if source is IAYF and action involves
            // lightsaber, Vader MUST be on table.
            if (textLower.contains("lightsaber") && cardId != null && gameState != null) {
                try {
                    PhysicalCard iaySourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (iaySourceCard != null && iaySourceCard.getTitle() != null
                        && iaySourceCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("i am your father")) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer iayObj = context.getObjectiveAnalyzer();
                        boolean vaderPresent = iayObj != null && iayObj.isVaderOnTable(gameState, context.getPlayerId());
                        if (!vaderPresent) {
                            action.addReasoning("V29.8 IAYF: Vader NOT on table — can't deploy lightsaber from ANY source!", -500.0f);
                            logger.warn("V29.8 IAYF BLOCKED: Vader not on table — lightsaber deploy from {} impossible!",
                                textLower.contains("lost") ? "Lost Pile" : "Reserve/other");
                        }
                    }
                } catch (Exception iayE) {
                    logger.debug("V29.8: Error checking IAYF vader: {}", iayE.getMessage());
                }
            }

            // ========== V29.8: SENSE & UNCERTAIN — BLOCK REDRAW HAND USAGE ==========
            // Sense & Uncertain Is The Future has two functions:
            //   1. As Sense: cancel an opponent's interrupt (GOOD — save for this!)
            //   2. As Uncertain: make each player redraw hand (TERRIBLE — helps opponent too,
            //      costs 3 Force, loses cards currently in hand, is a Lost Interrupt)
            // Rando must NEVER use the redraw hand function. Save Sense for defense.
            if (textLower.contains("redraw") && textLower.contains("hand")) {
                action.addReasoning("V29.8 SENSE REDRAW BLOCKED: NEVER redraw hand — save Sense for canceling opponent interrupts! Costs 3 Force AND helps opponent!", -600.0f);
                logger.warn("V29.8 SENSE REDRAW BLOCKED: Attempted to redraw hand — massive penalty (-600)");
            }
            // Also catch the "make each player" variant
            if (textLower.contains("each player") && (textLower.contains("redraw") || textLower.contains("shuffle"))) {
                action.addReasoning("V29.8 SENSE UNCERTAIN BLOCKED: Don't make both players redraw — helps opponent!", -600.0f);
                logger.warn("V29.8 SENSE UNCERTAIN BLOCKED: Attempted mutual redraw — massive penalty (-600)");
            }

            // ========== V29.7: UNIVERSAL RESERVE DECK PULL VALIDATION ==========
            // PROBLEM: Many cards produce GENERIC action texts like "Deploy card from Reserve Deck"
            // or "Take card into hand from Reserve Deck". The V25 checks looked for card names
            // like "crush the rebellion" in action text — but those names were NEVER in the text!
            // FIX: Look up the SOURCE CARD via cardId to identify what's generating the action,
            // then check DeckOracle for valid targets based on the source card's identity.
            if (textLower.contains("from reserve") && cardId != null && gameState != null) {
                String sourceTitle = null;
                try {
                    PhysicalCard sourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (sourceCard != null && sourceCard.getTitle() != null) {
                        sourceTitle = sourceCard.getTitle();
                    }
                } catch (Exception e) { /* ignore parse errors */ }

                if (sourceTitle != null) {
                    String sourceLower = sourceTitle.toLowerCase(java.util.Locale.ROOT);
                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle pullOracle = context.getDeckOracle();

                    // --- CRUSH THE REBELLION: pulls I Have You Now or Evader ---
                    if (sourceLower.contains("crush") && sourceLower.contains("rebellion")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.isCardInReserve("I Have You Now")
                                || pullOracle.isCardInReserve("Evader");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 CRUSH: No I Have You Now or Evader in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 CRUSH BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                            // V29.9: Check if IHYN/Evader already in hand — don't pull duplicates!
                            boolean ihynInHand = pullOracle.isCardInHand("I Have You Now");
                            boolean evaderInHand = pullOracle.isCardInHand("Evader");
                            boolean ihynInReserve = pullOracle.isCardInReserve("I Have You Now");
                            boolean evaderInReserve = pullOracle.isCardInReserve("Evader");
                            if (ihynInHand && evaderInHand) {
                                // Both targets already in hand — this pull is useless
                                action.addReasoning("V29.9 CRUSH DUPLICATE: Both IHYN and Evader already in hand — pulling another is wasteful!", -300.0f);
                                logger.warn("V29.9 CRUSH DUPLICATE: Both targets in hand — blocking (-300)");
                            } else if (ihynInHand && !evaderInReserve) {
                                // IHYN in hand and no Evader in reserve — would pull a second IHYN
                                action.addReasoning("V29.9 CRUSH DUPLICATE: IHYN already in hand, no Evader in reserve — save Crush!", -250.0f);
                                logger.warn("V29.9 CRUSH DUPLICATE: IHYN in hand, no Evader in reserve — blocking (-250)");
                            } else if (evaderInHand && !ihynInReserve) {
                                // Evader in hand and no IHYN in reserve — would pull a second Evader
                                action.addReasoning("V29.9 CRUSH DUPLICATE: Evader already in hand, no IHYN in reserve — save Crush!", -250.0f);
                                logger.warn("V29.9 CRUSH DUPLICATE: Evader in hand, no IHYN in reserve — blocking (-250)");
                            }
                        }
                    }

                    // --- I AM YOUR FATHER: deploys Vader's Lightsaber ---
                    // V35.8: IAYF can pull from Reserve Deck (free) OR Lost Pile (lose 1 Force).
                    // Both should score EXTREMELY high when Vader is on table unarmed.
                    // The Lost Pile retrieval is a KEY mechanic of Hunt Down — Vader throws
                    // his lightsaber every battle, then retrieves it for the next battle.
                    else if (sourceLower.contains("i am your father")) {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objA = context.getObjectiveAnalyzer();
                        boolean vaderOnTable = objA != null && objA.isVaderOnTable(gameState, context.getPlayerId());

                        if (!vaderOnTable && textLower.contains("lightsaber")) {
                            action.addReasoning("V29.7 IAYF: Vader NOT on table — can't deploy lightsaber!", -500.0f);
                            logger.warn("V29.7 IAYF BLOCKED: Vader not on table");
                        } else if (vaderOnTable && textLower.contains("lightsaber")) {
                            // V37: USE DECKORACLE to check WHERE the lightsaber actually is!
                            // IAYF can pull from Reserve Deck (free) or Lost Pile (lose 1 Force).
                            // The action text tells us which zone — don't try Reserve if it's in Lost.
                            boolean pullFromReserve = textLower.contains("reserve");
                            boolean pullFromLost = textLower.contains("lost");

                            boolean saberInReserve = false;
                            boolean saberInLost = false;
                            try {
                                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle iayOracle = context.getDeckOracle();
                                if (iayOracle != null && iayOracle.isAnalyzed()) {
                                    saberInReserve = iayOracle.isCardInReserve("Darth Vader's Lightsaber");
                                    saberInLost = iayOracle.isCardLost("Darth Vader's Lightsaber");
                                    logger.info("V37 IAYF ZONE CHECK: saber in reserve={}, in lost={}, action={}",
                                        saberInReserve, saberInLost, pullFromReserve ? "RESERVE" : pullFromLost ? "LOST" : "UNKNOWN");
                                }
                            } catch (Exception e) { /* ignore */ }

                            // V37: Block if trying to pull from wrong zone
                            if (pullFromReserve && !saberInReserve) {
                                action.addReasoning("V37 IAYF: Lightsaber NOT in Reserve Deck — WILL FAIL! Check Lost Pile instead.", -600.0f);
                                logger.warn("V37 IAYF BLOCKED: Trying reserve but saber not there! (in lost={})", saberInLost);
                            } else if (pullFromLost && !saberInLost) {
                                action.addReasoning("V37 IAYF: Lightsaber NOT in Lost Pile — check Reserve instead.", -400.0f);
                                logger.warn("V37 IAYF BLOCKED: Trying lost pile but saber not there! (in reserve={})", saberInReserve);
                            } else {
                                // Lightsaber IS in the target zone — check if Vader is armed
                                boolean vaderArmed = false;
                                try {
                                    String iayPid = context.getPlayerId();
                                    for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                        if (tc == null || !iayPid.equals(tc.getOwner())) continue;
                                        if (tc.getBlueprint() == null) continue;
                                        String tcTitle = tc.getTitle() != null ? tc.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (!tcTitle.contains("vader")) continue;
                                        if (tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        com.gempukku.swccgo.common.Zone tcZ = tc.getZone();
                                        if (tcZ == null || !tcZ.isInPlay()) continue;
                                        java.util.List<PhysicalCard> atts = gameState.getAttachedCards(tc);
                                        if (atts != null) {
                                            for (PhysicalCard att : atts) {
                                                if (att != null && att.getBlueprint() != null
                                                    && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                    vaderArmed = true;
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if (!vaderArmed) {
                                    action.addReasoning(String.format(
                                        "V37 IAYF: Vader UNARMED — retrieve lightsaber from %s NOW!",
                                        pullFromLost ? "Lost Pile" : "Reserve"), 600.0f);
                                    logger.warn("V37 IAYF: Vader unarmed, saber in {} — TOP PRIORITY (+600)",
                                        pullFromLost ? "Lost" : "Reserve");
                                } else {
                                    action.addReasoning("V35.8 IAYF: Vader armed — spare lightsaber retrieval", 50.0f);
                                }
                            }
                        }
                    }

                    // --- YOU ARE BEATEN: pulls IAYF or specific card from reserve ---
                    else if (sourceLower.contains("you are beaten")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasIAYF = pullOracle.isCardInReserve("I Am Your Father");
                            if (!hasIAYF) {
                                action.addReasoning("V29.7 YOU ARE BEATEN: No I Am Your Father in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 YOU ARE BEATEN BLOCKED: No IAYF in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- BLAST POINTS: pulls Ghhhk or Hyperwave Scan ---
                    else if (sourceLower.contains("blast points")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.isCardInReserve("Ghhhk")
                                || pullOracle.isCardInReserve("Hyperwave Scan");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 BLAST POINTS: No Ghhhk or Hyperwave Scan in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 BLAST POINTS BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- HUNT DOWN (objective): deploys location from reserve ---
                    else if (sourceLower.contains("hunt down") && textLower.contains("location")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            java.util.List<com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard> locsInReserve =
                                pullOracle.getCardsByCategory(com.gempukku.swccgo.common.CardCategory.LOCATION,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            if (locsInReserve.isEmpty()) {
                                action.addReasoning("V29.7 HUNT DOWN: No locations left in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 HUNT DOWN BLOCKED: No locations in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- IMPERIAL COMMAND: pulls admiral or general ---
                    else if (sourceLower.contains("imperial command")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("admiral", "general");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 IMPERIAL COMMAND: No admirals/generals in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 IMPERIAL COMMAND BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- ENDOR SHIELD: pulls admiral ---
                    else if (sourceLower.contains("endor shield")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("admiral");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 ENDOR SHIELD: No admirals in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 ENDOR SHIELD BLOCKED: No admirals in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- VISAGE OF THE EMPEROR: pulls lightsaber ---
                    else if (sourceLower.contains("visage") && textLower.contains("lightsaber")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("lightsaber");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 VISAGE: No lightsabers in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 VISAGE BLOCKED: No lightsabers in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- KIR KANOS: pulls Royal Guard ---
                    else if (sourceLower.contains("kir kanos")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("royal guard", "kanos", "kyneugh");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 KIR KANOS: No Royal Guards in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 KIR KANOS BLOCKED: No Royal Guards in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // === V37: UNIVERSAL RESERVE SEARCH SAFETY NET ===
                    // Any "from reserve" action that wasn't caught by a specific rule above
                    // should still be cautious. Failed searches give opponent free deck intel.
                    // If DeckOracle shows reserve deck is very small, penalize searches
                    // because they reveal more information proportionally.
                    if (pullOracle != null && pullOracle.isAnalyzed()) {
                        java.util.List<com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard> reserveCards =
                            pullOracle.getCardsInZone(com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                        if (reserveCards.size() <= 3) {
                            action.addReasoning("V37 RESERVE INTEL RISK: Only " + reserveCards.size() +
                                " cards in reserve — search reveals almost everything to opponent!", -200.0f);
                            logger.warn("V37 RESERVE RISK: {} cards in reserve — search gives opponent too much intel (-200)",
                                reserveCards.size());
                        } else if (reserveCards.size() <= 8) {
                            action.addReasoning("V37 RESERVE CAUTION: " + reserveCards.size() +
                                " cards in reserve — opponent will see deck composition", -50.0f);
                        }
                    }
                }
            }

            // ========== V24.9: MASTERFUL MOVE EARLY-GAME GUARD ==========
            // Masterful Move searches reserve for Ghhhk (damage cancel combo card).
            // On turns 1-3, force should go to deploying Executor + characters, NOT searching for Ghhhk.
            // Only play Masterful Move when characters are on the table and need protecting.
            if (textLower.contains("masterful move")) {
                int mmTurn = context.getTurnNumber();
                boolean hasCharsOnTable = false;
                if (gameState != null) {
                    try {
                        for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                            java.util.List<PhysicalCard> cardsHere = gameState.getCardsAtLocation(loc);
                            if (cardsHere != null) {
                                for (PhysicalCard c : cardsHere) {
                                    if (c != null && context.getPlayerId().equals(c.getOwner()) &&
                                        c.getBlueprint() != null &&
                                        c.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                        hasCharsOnTable = true;
                                        break;
                                    }
                                }
                            }
                            if (hasCharsOnTable) break;
                        }
                    } catch (Exception e) {
                        logger.debug("V24.9 MM guard: Error scanning for characters: {}", e.getMessage());
                    }
                }
                if (!hasCharsOnTable) {
                    action.addReasoning("V24.9 MASTERFUL MOVE: No characters on table — Ghhhk has nothing to protect! Save force for deployment!", -500.0f);
                    logger.warn("V24.9 MASTERFUL MOVE: BLOCKED — no characters on table, save force for Executor! (-500)");
                } else if (mmTurn <= 2) {
                    action.addReasoning("V24.9 MASTERFUL MOVE: Too early (turn " + mmTurn + ") — prioritize getting Executor out!", -300.0f);
                    logger.warn("V24.9 MASTERFUL MOVE: Penalized on turn {} — save force for Executor deployment! (-300)", mmTurn);
                }
            }

            // ========== Capacity Slot Selection (Pilot vs Passenger) ==========
            if (textLower.contains("capacity slot")) {
                if (textLower.contains("pilot capacity slot")) {
                    action.setScore(100.0f);
                    action.addReasoning("Pilot slot adds power to ship!", 100.0f);
                    action.setActionType(ActionType.MOVE);
                    logger.info("PILOT SLOT: Strongly preferring pilot capacity (+100)");
                } else if (textLower.contains("passenger capacity slot")) {
                    action.setScore(VERY_BAD_DELTA);
                    action.addReasoning("Passenger gives NO power bonus!", VERY_BAD_DELTA);
                    action.setActionType(ActionType.MOVE);
                    logger.warn("PASSENGER SLOT: Penalizing - no power contribution ({})", VERY_BAD_DELTA);
                }
                actions.add(action);
                continue;
            }

            // ========== V29.15 Epic Event Saga Choice ==========
            // "The Force Is Strong In My Family" presents choices:
            //   "My Father Has It", "I Have It", "You Have That Power, Too"
            // The correct choice depends on the deck name:
            //   Luke deck → "I Have It"
            //   Anakin deck → "My Father Has It"
            //   Rey deck → "You Have That Power, Too"
            if (textLower.contains("i have it") || textLower.contains("my father has it")
                || textLower.contains("you have that power")) {
                String deckName = context.getDeckName();
                String deckLower = (deckName != null) ? deckName.toLowerCase(java.util.Locale.ROOT) : "";
                boolean isCorrectChoice = false;

                if (deckLower.contains("luke") && textLower.contains("i have it")
                    && !textLower.contains("my father has it")) {
                    isCorrectChoice = true;
                } else if (deckLower.contains("anakin") && textLower.contains("my father has it")) {
                    isCorrectChoice = true;
                } else if (deckLower.contains("rey") && textLower.contains("you have that power")) {
                    isCorrectChoice = true;
                }

                if (isCorrectChoice) {
                    action.addReasoning("V29.15 EPIC EVENT: Correct saga choice for '" + deckName + "' deck!", 1000.0f);
                    logger.warn("V29.15 EPIC EVENT: Choosing '{}' — correct for deck '{}'", actionText, deckName);
                } else if (!deckLower.isEmpty()) {
                    action.addReasoning("V29.15 EPIC EVENT: Wrong saga choice for '" + deckName + "' deck", -500.0f);
                    logger.warn("V29.15 EPIC EVENT: Penalizing '{}' — wrong for deck '{}'", actionText, deckName);
                } else {
                    // No deck name available — default to "I Have It" (most common Luke deck)
                    if (textLower.contains("i have it") && !textLower.contains("my father has it")) {
                        action.addReasoning("V29.15 EPIC EVENT: Default to 'I Have It' (no deck name)", 500.0f);
                        logger.warn("V29.15 EPIC EVENT: No deck name — defaulting to 'I Have It'");
                    }
                }
                actions.add(action);
                continue;
            }

            // ========== Force Activation ==========
            if (actionText.equals("Activate Force")) {
                action.setActionType(ActionType.ACTIVATE_FORCE);
                try {
                    evaluateActivateForce(action, context);
                } catch (Exception e) {
                    // V29.13: NEVER skip activation due to exceptions.
                    // Default to high score so Rando always activates Force.
                    logger.warn("V29.13: Exception in evaluateActivateForce, defaulting to ACTIVATE: {}", e.getMessage());
                    action.addReasoning("V29.13 SAFE DEFAULT: Always activate Force", VERY_GOOD_DELTA);
                }
            }

            // ========== Force Drain ==========
            else if (actionText.equals("Force drain")) {
                action.setActionType(ActionType.FORCE_DRAIN);
                evaluateForceDrain(action, context, cardId);
            }

            // ========== Race Destiny ==========
            else if (actionText.equals("Draw race destiny")) {
                action.setActionType(ActionType.RACE_DESTINY);
                action.addReasoning("Race destiny always high priority", VERY_GOOD_DELTA);
            }

            // ========== Play a Card ==========
            // V29.1: If the source card is Knowledge And Defense (V), this is a shield play.
            // Apply shield pacing — play 2 shields on turn 1, hold the rest to scout opponent.
            else if (actionText.equals("Play a card")) {
                action.setActionType(ActionType.PLAY_CARD);
                boolean isKnDShieldPlay = false;
                if (cardId != null && gameState != null) {
                    try {
                        PhysicalCard sourceCard = gameState.findCardById(Integer.parseInt(cardId));
                        if (sourceCard != null) {
                            String sourceTitle = sourceCard.getTitle();
                            if (sourceTitle != null && sourceTitle.toLowerCase().contains("knowledge and defense")) {
                                isKnDShieldPlay = true;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore — fall through to generic evaluation
                    }
                }
                if (isKnDShieldPlay) {
                    com.gempukku.swccgo.ai.models.rando.strategy.ShieldStrategy shieldStrat = context.getShieldStrategy();
                    int turnNum = context.getTurnNumber();
                    if (shieldStrat != null && shieldStrat.atPacingCap(turnNum)) {
                        action.addReasoning("V29.1 K&D SHIELD PACING: Holding shield slot — scout opponent first (turn " + turnNum + ")", -40.0f);
                    } else {
                        action.addReasoning("K&D: Play defensive shield (slots available)", VERY_GOOD_DELTA);
                    }
                } else {
                    evaluatePlayCard(action, context);
                }
            }

            // ========== Fire Weapons ==========
            // V29.12: Fire MUST score higher than throw (250) so Rando fires the
            // lightsaber BEFORE throwing it. Throwing sacrifices the weapon (places it
            // in Lost Pile), so if throw happens first, fire becomes impossible.
            // Fire first = hit target + THEN throw for attrition destiny = double trouble.
            else if (actionText.contains("Fire")) {
                action.setActionType(ActionType.FIRE_WEAPON);
                // Check if there are valid (non-HIT) targets before firing
                // Ported from Python action_text_evaluator.py - don't fire at already-hit targets
                boolean hasValidTargets = checkForValidWeaponTargets(context);
                if (hasValidTargets) {
                    if (context.getPhase() == Phase.BATTLE) {
                        // V29.12: In battle, fire weapons BEFORE throw — score must beat throw's 200
                        action.addReasoning("V29.12 FIRE WEAPON: Fire FIRST in battle — hit target before throwing!", 300.0f);
                        logger.warn("V29.12 FIRE WEAPON: Battle phase fire — must happen before throw (+300)");
                    } else {
                        action.addReasoning("Firing weapons at valid targets", VERY_GOOD_DELTA);
                    }
                } else {
                    action.addReasoning("All targets already HIT - save weapon", BAD_DELTA);
                    logger.debug("Skipping weapon fire - no valid (unhit) targets");
                }
            }

            // ========== Add Battle Destiny ==========
            else if (textLower.contains("add") && textLower.contains("battle destiny")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                action.addReasoning("Adding battle destiny is great", VERY_GOOD_DELTA);
            }

            // ========== V29.10/V29.12: LIGHTSABER THROW — ADD DESTINY TO ATTRITION ==========
            // After firing a lightsaber, Vader can also 'throw' it to add destiny to attrition.
            // This is a SEPARATE action from firing — both can be done in the same battle.
            // The throw adds extra attrition damage which can be decisive.
            // Action text: "'Throw' to add destiny to attrition"
            //
            // V29.12 CRITICAL: Throw MUST score LOWER than Fire (300).
            // Throwing places the lightsaber in Lost Pile — if Rando throws first,
            // he can NEVER fire it. The correct sequence is:
            //   1. FIRE lightsaber at target (hit them, reduce forfeit) — score 300
            //   2. THROW lightsaber (sacrifice it for attrition destiny) — score 200
            // This gives "double trouble" — hit + extra attrition in the same battle.
            if (textLower.contains("throw") && textLower.contains("add destiny to attrition")) {
                if (context.getPhase() == Phase.BATTLE) {
                    action.addReasoning("V29.12 LIGHTSABER THROW: Add destiny to attrition — do AFTER firing!", 200.0f);
                    logger.warn("V29.12 LIGHTSABER THROW: Battle phase throw (+200, below fire's +300)");
                } else {
                    action.addReasoning("V29.10 LIGHTSABER THROW: Throw lightsaber to add destiny to attrition!", 150.0f);
                }
            }

            // ========== V29.10: HATRED CARD — CANCEL OPPONENT GAME TEXT ==========
            // Stacking a Hatred Card on an opponent's character cancels their game text.
            // This is CRITICAL because it removes attrition immunity and other protections.
            // Without Hatred, winning a battle does NOTHING if opponent is immune to attrition.
            // Action text variants:
            //   "Stack a 'Hatred Card'" (previous game)
            //   "USED: Stack 'Hatred' card on opponent's character" (this game)
            // BEST TIMING: Deploy phase — stack Hatred BEFORE initiating battle.
            // This way opponent's immunities are already gone when battle starts.
            if (textLower.contains("hatred")) {
                // V37.1: Only place hatred on OUR turn — placing during opponent's turn
                // wastes it because we can't follow up with a battle this turn.
                if (gameState != null && !context.isMyTurn()) {
                    action.addReasoning("V37.1 HATRED: Not our turn — save hatred for our deploy phase!", -600.0f);
                    logger.warn("V37.1 HATRED: Opponent's turn — blocking hatred placement (-600)");
                } else {

                String decisionText = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase(Locale.ROOT) : "";
                boolean isDeployPhase = context.getPhase() == Phase.DEPLOY
                    || decisionText.contains("deploy");
                boolean isBattlePhase = context.getPhase() == Phase.BATTLE
                    || decisionText.contains("battle") || decisionText.contains("weapons segment");

                // V35.3: STRICT hatred scoring — ONLY place hatred when Vader or Inquisitor
                // is at the SAME SITE as an opponent character. No proactive/remote hatred.
                boolean v35VaderOrInqWithOpponents = false;
                boolean v35InqOnTable = false;
                boolean v35JediAtSameSite = false;
                try {
                    if (gameState != null) {
                        String v35Pid = context.getPlayerId();
                        String v35Oid = gameState.getOpponent(v35Pid);
                        for (PhysicalCard tCard : gameState.getAllPermanentCards()) {
                            if (tCard == null || !v35Pid.equals(tCard.getOwner())) continue;
                            if (tCard.getBlueprint() == null) continue;
                            if (tCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                            com.gempukku.swccgo.common.Zone tz = tCard.getZone();
                            if (tz == null || !tz.isInPlay()) continue;
                            String tTitle = tCard.getTitle() != null ? tCard.getTitle().toLowerCase(Locale.ROOT) : "";
                            // V35.7: Hatred requires INQUISITOR only (NOT Vader alone).
                            // The card "There Are Many Hunting You Now" requires "your Inquisitor"
                            // at the same location. Vader alone cannot use hatred.
                            if (isInquisitor(tTitle)) {
                                v35InqOnTable = true;
                                PhysicalCard charLoc = tCard.getAtLocation();
                                if (charLoc != null) {
                                    float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, charLoc, v35Oid, false, false);
                                    if (oppPower > 0) {
                                        v35VaderOrInqWithOpponents = true;
                                        for (PhysicalCard lc : gameState.getCardsAtLocation(charLoc)) {
                                            if (lc == null || !v35Oid.equals(lc.getOwner())) continue;
                                            String lcT = lc.getTitle() != null ? lc.getTitle().toLowerCase(Locale.ROOT) : "";
                                            if (isJediOrPadawan(lcT)) { v35JediAtSameSite = true; break; }
                                        }
                                    }
                                }
                                if (v35VaderOrInqWithOpponents) break;
                            }
                        }
                    }
                } catch (Exception e) { /* ignore */ }

                if (!v35InqOnTable) {
                    // V35.7: No Inquisitor on table — hatred requires Inquisitor, BLOCK
                    action.addReasoning("V35.7 HATRED: No Inquisitor on table — hatred requires Inquisitor!", -500.0f);
                    logger.warn("V35.7 HATRED: No Inquisitor — hard block (-500)");
                } else if (v35VaderOrInqWithOpponents) {
                    // V35.7: Inquisitor AT SAME SITE as opponent — hatred is useful!
                    float hatredScore = isDeployPhase ? (float) RandoConfig.SCORE_HATRED_WITH_INQUISITOR : 350.0f;
                    if (v35JediAtSameSite) hatredScore += 150.0f;
                    action.addReasoning(String.format(
                        "V35.7 HATRED: Inquisitor WITH opponents%s — cancel game text! (+%.0f)",
                        v35JediAtSameSite ? " + JEDI" : "", hatredScore), hatredScore);
                    logger.warn("V35.7 HATRED: Inquisitor with opponents (jedi={}) — score +{}",
                        v35JediAtSameSite, (int)hatredScore);
                } else {
                    // V35.3: Vader/Inquisitor NOT at same site as any opponent — DON'T waste hatred
                    action.addReasoning("V35.3 HATRED: Vader/Inquisitor not at same site as opponents — save for later!", -300.0f);
                    logger.warn("V35.3 HATRED: No Vader/Inq co-located with opponents — blocked (-300)");
                }
            } // end V37.1 isMyTurn else block
            }

            // ========== V29.9: I HAVE YOU NOW — PLAY DURING BATTLE ==========
            // IHYN adds extra battle destiny draws when Vader is in the battle.
            // This is DEVASTATING — 2-3 extra destiny draws can turn any battle into a win.
            // Must be played DURING a battle. Check if we're in battle phase and Vader is present.
            // Also catch "i have you now" in source card check for generic action texts.
            if (textLower.contains("i have you now") || textLower.contains("ihyn")) {
                if (context.getPhase() == Phase.BATTLE) {
                    // V39: Only play IHYN ONCE per battle
                    int currentBattleHash = gameState != null && gameState.getBattleState() != null
                        ? System.identityHashCode(gameState.getBattleState()) : 0;
                    if (currentBattleHash != ihynBattleId) {
                        // New battle — reset tracker
                        ihynPlayedThisBattle = false;
                        ihynBattleId = currentBattleHash;
                    }
                    if (ihynPlayedThisBattle) {
                        action.addReasoning("V39 IHYN: Already played once this battle — BLOCK second play!", -9999.0f);
                        logger.warn("V39 IHYN: BLOCKED second play — once per battle limit");
                        actions.add(action);
                        continue;
                    }
                    // Mark as played (will be set when this action is chosen)
                    // Note: we set it here optimistically — if this action wins, it's played
                    ihynPlayedThisBattle = true;

                    // In battle — check if Vader is participating
                    boolean vaderInBattle = false;
                    try {
                        if (gameState != null && gameState.getBattleState() != null) {
                            PhysicalCard battleLoc = gameState.getBattleState().getBattleLocation();
                            if (battleLoc != null) {
                                String ihynPlayerId = context.getPlayerId();
                                for (PhysicalCard bCard : gameState.getCardsAtLocation(battleLoc)) {
                                    if (bCard == null || !ihynPlayerId.equals(bCard.getOwner())) continue;
                                    if (bCard.getTitle() != null && bCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("vader")) {
                                        vaderInBattle = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V29.9 IHYN: Error checking Vader in battle: {}", e.getMessage());
                    }

                    if (vaderInBattle) {
                        action.addReasoning("V29.9 IHYN: Vader in battle — PLAY I HAVE YOU NOW for devastating extra destiny draws!", 300.0f);
                        logger.warn("V29.9 IHYN: Vader in battle — mega boost (+300) for I Have You Now!");
                    } else {
                        // Still good even without Vader — adds destiny draws
                        action.addReasoning("V29.9 IHYN: Play I Have You Now for extra battle destiny!", 100.0f);
                        logger.info("V29.9 IHYN: Playing during battle without Vader (+100)");
                    }
                } else {
                    // Not in battle — save IHYN for when we need it
                    action.addReasoning("V29.9 IHYN: Save I Have You Now for battle!", -200.0f);
                    logger.info("V29.9 IHYN: Not in battle — save for later (-200)");
                }
            }
            // Also check source card for IHYN when action text is generic
            else if (context.getPhase() == Phase.BATTLE && cardId != null && gameState != null) {
                try {
                    PhysicalCard ihynSource = gameState.findCardById(Integer.parseInt(cardId));
                    if (ihynSource != null && ihynSource.getTitle() != null
                        && ihynSource.getTitle().toLowerCase(java.util.Locale.ROOT).contains("i have you now")) {
                        action.addReasoning("V29.9 IHYN: Play I Have You Now during battle — extra destiny draws!", 200.0f);
                        logger.warn("V29.9 IHYN (source): I Have You Now detected via source card — boost +200");
                    }
                } catch (Exception e) { /* ignore */ }
            }

            // ========== V35: FAR MORE FRIGHTENING THAN DEATH ==========
            // FMFTD has two modes:
            // USED: Stack hatred on opponent's leader/ability>3 at battleground
            // LOST: Add 1-2 battle destiny if Inquisitor with Jedi/Padawan/Hatred
            // Detect via testingTexts or action text containing "far more frightening"
            if (textLower.contains("far more frightening") || textLower.contains("fmftd")) {
                boolean isFmftdBattle = context.getPhase() == Phase.BATTLE;
                boolean isFmftdUsedMode = textLower.contains("stack") || (textLower.contains("hatred") && !textLower.contains("destiny"));
                boolean isFmftdLostMode = textLower.contains("destiny") || textLower.contains("add");

                if (isFmftdLostMode && isFmftdBattle) {
                    // LOST mode during battle — check for Inquisitor + Jedi + Hatred synergy
                    boolean v35FmInq = false;
                    boolean v35FmJedi = false;
                    boolean v35FmHatred = false;
                    try {
                        if (gameState != null && gameState.getBattleState() != null) {
                            PhysicalCard fmBattleLoc = gameState.getBattleState().getBattleLocation();
                            if (fmBattleLoc != null) {
                                String fmPid = context.getPlayerId();
                                String fmOid = gameState.getOpponent(fmPid);
                                for (PhysicalCard bc : gameState.getCardsAtLocation(fmBattleLoc)) {
                                    if (bc == null) continue;
                                    String bcTitle = bc.getTitle() != null ? bc.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (fmPid.equals(bc.getOwner()) && isInquisitor(bcTitle)) v35FmInq = true;
                                    if (fmOid != null && fmOid.equals(bc.getOwner())) {
                                        if (isJediOrPadawan(bcTitle)) v35FmJedi = true;
                                        java.util.List<PhysicalCard> st = gameState.getStackedCards(bc);
                                        if (st != null && !st.isEmpty()) v35FmHatred = true;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    int synCount = (v35FmInq ? 1 : 0) + (v35FmJedi ? 1 : 0) + (v35FmHatred ? 1 : 0);
                    if (synCount >= 3) {
                        action.addReasoning("V35 FMFTD LOST: Inquisitor + Jedi + Hatred — ADD 2 BATTLE DESTINY!", (float) RandoConfig.SCORE_FMFTD_FULL_SYNERGY);
                        logger.warn("V35 FMFTD: Full synergy! +{}", RandoConfig.SCORE_FMFTD_FULL_SYNERGY);
                    } else if (synCount >= 2) {
                        action.addReasoning("V35 FMFTD LOST: Inquisitor with Jedi or Hatred — add 1 battle destiny!", 350.0f);
                    } else if (v35FmInq) {
                        action.addReasoning("V35 FMFTD LOST: Inquisitor in battle — add destiny!", 200.0f);
                    } else {
                        action.addReasoning("V35 FMFTD LOST: No Inquisitor in battle — limited value", 50.0f);
                    }
                } else if (isFmftdUsedMode) {
                    // USED mode — place hatred card
                    if (context.getPhase() == Phase.DEPLOY || context.getPhase() == Phase.MOVE) {
                        action.addReasoning("V35 FMFTD USED: Place hatred on opponent — cancel game text!", 350.0f);
                    } else {
                        action.addReasoning("V35 FMFTD USED: Place hatred — decent timing", 150.0f);
                    }
                } else if (isFmftdBattle) {
                    // Generic FMFTD during battle — likely the LOST mode
                    action.addReasoning("V35 FMFTD: Play during battle for extra destiny!", 250.0f);
                } else {
                    action.addReasoning("V35 FMFTD: Save for battle if possible", -100.0f);
                }
            }

            // ========== V35: VADER SELF-RECALL (Hunt Down V once-per-game) ==========
            // "Take Vader into hand" — allows redeploying Vader to hunt Jedi elsewhere
            // "Return an Inquisitor here to hand" — Eighth Brother repositioning
            else if (textLower.contains("take vader into hand") || textLower.contains("return") && textLower.contains("inquisitor") && textLower.contains("hand")) {
                if (textLower.contains("vader")) {
                    // Vader self-recall — check if there are Jedi elsewhere to hunt
                    boolean v35JediElsewhere = false;
                    try {
                        if (gameState != null) {
                            String v35Oid = gameState.getOpponent(context.getPlayerId());
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null) continue;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c == null || !v35Oid.equals(c.getOwner())) continue;
                                    String ct = c.getTitle() != null ? c.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (isJediOrPadawan(ct)) { v35JediElsewhere = true; break; }
                                }
                                if (v35JediElsewhere) break;
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (v35JediElsewhere) {
                        action.addReasoning("V35 VADER RECALL: Take Vader into hand — Jedi elsewhere to hunt! Redeploy!", 300.0f);
                        logger.warn("V35 VADER RECALL: Jedi detected elsewhere — recalling Vader to redeploy (+300)");
                    } else {
                        action.addReasoning("V35 VADER RECALL: Take Vader into hand — no clear target, keep him deployed", -100.0f);
                    }
                } else {
                    // V35.1: Inquisitor recall — DON'T recall if opponents are nearby!
                    // Eighth Brother's ability returns an Inquisitor to hand. Only do this
                    // if there are NO opponents at adjacent sites. If opponents are nearby,
                    // keep the Inquisitor to fight!
                    boolean opponentsNearby = false;
                    try {
                        if (gameState != null) {
                            String recallPid = context.getPlayerId();
                            String recallOid = gameState.getOpponent(recallPid);
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null) continue;
                                float oppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, recallOid, false, false);
                                if (oppPwr > 0) { opponentsNearby = true; break; }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (opponentsNearby) {
                        action.addReasoning("V35.1 INQUISITOR RECALL BLOCK: Opponents on the board — KEEP Inquisitor to fight!", -400.0f);
                        logger.warn("V35.1 INQUISITOR RECALL BLOCKED: Opponents present — don't pull back (-400)");
                    } else {
                        action.addReasoning("V35 INQUISITOR RECALL: No opponents on board — safe to reposition", 100.0f);
                    }
                }
            }

            // ========== V37.2: STUNNING LEADER — DEFENSIVE ONLY ==========
            // Stunning Leader excludes characters from battle. Good when DEFENDING
            // against a stronger opponent (saves Vader from certain death).
            // BAD when WE initiated (we started the fight to WIN).
            else if (textLower.contains("stunning leader") || textLower.contains("exclude") && textLower.contains("from battle")) {
                if (context.getPhase() == Phase.BATTLE && gameState != null) {
                    try {
                        com.gempukku.swccgo.game.state.BattleState bState = gameState.getBattleState();
                        if (bState != null) {
                            String slPlayerId = context.getPlayerId();
                            String slInitiator = bState.getPlayerInitiatedBattle();
                            boolean weInitiated = slPlayerId != null && slPlayerId.equals(slInitiator);

                            if (weInitiated) {
                                // WE started this battle — NEVER cancel our own attack!
                                action.addReasoning("V37.2 STUNNING LEADER: WE initiated — fight to WIN!", -9999.0f);
                                logger.warn("V37.2 STUNNING LEADER: HARD BLOCK — we initiated this battle!");
                            } else {
                                // Opponent initiated — check if we're outmatched
                                PhysicalCard slBattleLoc = bState.getBattleLocation();
                                if (slBattleLoc != null) {
                                    String slOpp = gameState.getOpponent(slPlayerId);
                                    float slOurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, slBattleLoc, slPlayerId, false, false);
                                    float slTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, slBattleLoc, slOpp, false, false);
                                    if (slTheirPower > slOurPower * 1.5f) {
                                        // Badly outmatched — Stunning Leader saves our characters!
                                        action.addReasoning(String.format(
                                            "V37.2 STUNNING LEADER: Outmatched %.0f vs %.0f — exclude to survive!",
                                            slOurPower, slTheirPower), 300.0f);
                                        logger.warn("V37.2 STUNNING LEADER: Defensive use — saving characters from {} vs {}",
                                            (int)slOurPower, (int)slTheirPower);
                                    } else {
                                        // Close fight — fight it out instead of excluding
                                        action.addReasoning("V37.2 STUNNING LEADER: Close fight — battle instead!", -300.0f);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V37.2 STUNNING LEADER: Error: {}", e.getMessage());
                    }
                } else {
                    action.addReasoning("V37.2 STUNNING LEADER: Not in battle — save!", -200.0f);
                }
            }

            // ========== V35.4: YOU ARE BEATEN — DON'T WASTE ON UNDERCOVER SPIES ==========
            // You Are Beaten targets opponent characters. But undercover spies appear on OUR side
            // and aren't valid targets for combat effects. Don't waste this interrupt.
            // Also: only use during battle or when it will lead to meaningful attrition.
            else if (textLower.contains("you are beaten")) {
                if (context.getPhase() == Phase.BATTLE) {
                    action.addReasoning("V35.4 YOU ARE BEATEN: During battle — use for attrition!", 150.0f);
                } else {
                    // Outside battle — this is usually a waste
                    action.addReasoning("V35.4 YOU ARE BEATEN: Not in battle — save for combat!", -200.0f);
                    logger.info("V35.4 YOU ARE BEATEN: Not in battle — penalizing (-200)");
                }
            }

            // ========== Battle Destiny Modifier (+1 to battle destiny) ==========
            else if ((actionText.contains("+1") || actionText.contains("+ 1") || textLower.contains("add 1"))
                     && textLower.contains("battle destiny")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                action.addReasoning("+1 to battle destiny - always use!", VERY_GOOD_DELTA);
            }

            // ========== V24.2: Force Drain Modifier (+1 to force drain) ==========
            // Cards like Lord Maul With Lightsaber add +1 to force drain as an optional response.
            // This should ALWAYS be accepted — free extra damage!
            else if ((actionText.contains("+1") || actionText.contains("+ 1") || textLower.contains("add 1"))
                     && textLower.contains("force drain")) {
                action.setActionType(ActionType.FORCE_DRAIN);
                action.addReasoning("V24.2 FORCE DRAIN BONUS: +1 to force drain — always use!", VERY_GOOD_DELTA + 30.0f);
                logger.warn("V24.2 DRAIN BONUS: Accepting +1 force drain — '{}'", actionText);
            }

            // ========== Weapon Destiny Modifier ==========
            else if (textLower.contains("weapon destiny") &&
                     (actionText.contains("+3") || actionText.contains("+2") || textLower.contains("add"))) {
                action.setActionType(ActionType.FIRE_WEAPON);
                action.addReasoning("Boost weapon destiny - increases hit chance!", VERY_GOOD_DELTA);
            }

            // ========== Protect Battle Destiny Draws ==========
            else if (textLower.contains("prevent") && textLower.contains("cancel") &&
                     textLower.contains("battle destiny") && textLower.contains("draw")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                evaluateDestinyProtection(action, context);
            }

            // ========== Prevent Opponent Adding Battle Destiny ==========
            else if (textLower.contains("prevent") && textLower.contains("battle destiny") &&
                     !textLower.contains("cancel")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                action.addReasoning("Prevent opponent battle destiny - denies their draw!", VERY_GOOD_DELTA);
            }

            // ========== Take Admiral/General Into Hand ==========
            // For TDIGWATT/Bespin objectives: an Imperial admiral pulled here is likely
            // a pilot (e.g., Admiral Chiraneau). That pilot enables deploying the Executor
            // to Bespin cheaply — the Executor + pilot simultaneous deploy is the critical
            // Turn 1 play for Cloud City objectives. Prioritise VERY highly when we have
            // no ship at Bespin yet.
            else if (textLower.contains("take") && textLower.contains("into hand") &&
                     (textLower.contains("admiral") || textLower.contains("general"))) {
                // Check if we're running a Bespin/Cloud City objective with no ship there yet
                boolean bespinChainActive = false;
                try {
                    com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer objAnalyzer =
                        context.getObjectiveAnalyzer();
                    if (objAnalyzer != null && objAnalyzer.isAnalyzed() &&
                        objAnalyzer.needsBespinSystemPresence()) {
                        // Check if we already have a ship at Bespin system
                        boolean hasBespinShip = false;
                        if (gameState != null) {
                            String pid = context.getPlayerId();
                            for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null && loc.getTitle() != null &&
                                    loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                    loc.getBlueprint() != null &&
                                    loc.getBlueprint().getCardSubtype() ==
                                        com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                    float ourPower = context.getGame().getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, loc, pid, false, false);
                                    if (ourPower > 0) hasBespinShip = true;
                                    break;
                                }
                            }
                        }
                        if (!hasBespinShip) {
                            bespinChainActive = true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore — fall back to default scoring
                }

                // V29.7: Check if there are actually admirals/generals left in Reserve
                boolean hasValidTarget = true;
                try {
                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle oracle = context.getDeckOracle();
                    if (oracle != null && oracle.isAnalyzed()) {
                        hasValidTarget = oracle.hasTargetInReserve("admiral", "general");
                        if (!hasValidTarget) {
                            logger.warn("V29.7 PULL CHECK: No admirals/generals left in Reserve — blocking pull!");
                        }
                    }
                } catch (Exception pullCheckE) {
                    // Can't check — assume target exists
                }

                if (!hasValidTarget) {
                    // No valid targets in Reserve — don't waste Force on this!
                    action.addReasoning("V29.7 NO TARGET: No admirals/generals in Reserve Deck — skip!", -300.0f);
                } else if (bespinChainActive) {
                    // Admiral pilot → Executor chain: this is Turn 1 critical for TDIGWATT.
                    // Score it as high as AMSD itself so we never skip this pull.
                    action.addReasoning(
                        "CRITICAL: Admiral pilot enables Executor deploy to Bespin — must pull T1!", 300.0f);
                    logger.warn("EXECUTOR CHAIN: Admiral pull with no Bespin ship — boosting to 300 (enables Executor pipeline)");
                } else {
                    // V29.7: Pulls ALWAYS fire before locations and characters.
                    // Getting cards into hand first means better deploy choices later.
                    action.addReasoning("V29.7 PULL FIRST: Retrieve admiral/general into hand before deploying!", 250.0f);
                }
            }

            // ========== Substitute Destiny ==========
            else if (textLower.contains("substitute destiny")) {
                action.setActionType(ActionType.SUBSTITUTE_DESTINY);
                action.addReasoning("Substituting destiny is good", GOOD_DELTA);
            }

            // ========== React ==========
            else if (textLower.contains("react")) {
                action.setActionType(ActionType.REACT);
                action.addReasoning("Avoid reacts (bot doesn't understand timing)", BAD_DELTA);
            }

            // ========== Steal ==========
            else if (textLower.contains("steal")) {
                action.setActionType(ActionType.STEAL);
                action.addReasoning("Stealing is good", GOOD_DELTA);
            }

            // ========== Sabacc ==========
            else if (textLower.contains("play sabacc")) {
                action.setActionType(ActionType.SABACC);
                action.addReasoning("Playing sabacc", GOOD_DELTA);
            }

            // ========== Cancel Own Cards (Bad!) ==========
            else if (textLower.contains("cancel your")) {
                action.setActionType(ActionType.CANCEL);
                action.addReasoning("Never cancel own cards", VERY_BAD_DELTA);
            }

            // ========== Cancel Opponent's Interrupt (Sense/Control) ==========
            else if (textLower.contains("cancel") &&
                     (textLower.contains("interrupt") || textLower.contains("sense") ||
                      textLower.contains("alter") || textLower.contains("effect") ||
                      textLower.contains("force drain")) &&
                     !textLower.contains("your")) {
                action.setActionType(ActionType.CANCEL);
                evaluateSenseCancel(action, context, actionText);
            }

            // ========== V37: Cancel/Redraw Destiny — CHECK CURRENT VALUE FIRST ==========
            // Imperial Enforcement and similar cards cancel a destiny draw and cause a redraw.
            // Only use if the current destiny is LOW (< 3). A 6-destiny character draw is
            // essentially the best possible — NEVER cancel that.
            // Use DeckOracle average to decide if redraw is likely to improve.
            else if (textLower.contains("cancel") && textLower.contains("redraw") && textLower.contains("destiny")) {
                // Try to extract the current destiny value from the action text
                // Format often includes the drawn card name — check for high destiny numbers
                float currentDestinyDrawn = -1;
                try {
                    // The action text often says "cancel X's battle destiny draw of <CardName>"
                    // We can check DeckOracle for average destiny to decide
                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle redrawOracle = context.getDeckOracle();
                    double avgDest = 3.0;
                    if (redrawOracle != null && redrawOracle.isAnalyzed()) {
                        avgDest = redrawOracle.getAverageDestinyInReserve();
                    }

                    // Extract destiny number from action text if present (e.g., "draw of X as a 6")
                    java.util.regex.Matcher destMatcher = java.util.regex.Pattern.compile("as a (\\d+)").matcher(textLower);
                    if (destMatcher.find()) {
                        currentDestinyDrawn = Float.parseFloat(destMatcher.group(1));
                    }

                    if (currentDestinyDrawn >= 0) {
                        if (currentDestinyDrawn >= 3) {
                            // Good destiny draw — do NOT cancel! Average would likely be worse.
                            action.addReasoning(String.format(
                                "V37 DON'T REDRAW: Current destiny %.0f is GOOD (avg %.1f) — keep it!",
                                currentDestinyDrawn, avgDest), -300.0f);
                            logger.warn("V37 REDRAW BLOCKED: Destiny {} is >= 3 (avg {}) — don't cancel!",
                                (int)currentDestinyDrawn, String.format("%.1f", avgDest));
                        } else {
                            // Low destiny — redraw is likely to improve
                            action.addReasoning(String.format(
                                "V37 REDRAW: Current destiny %.0f is LOW (avg %.1f) — try for better!",
                                currentDestinyDrawn, avgDest), 100.0f);
                        }
                    } else {
                        // Couldn't determine current value — use average as guide
                        if (avgDest >= 3.5) {
                            action.addReasoning("Redraw destiny — good average in reserve", GOOD_DELTA);
                        } else {
                            action.addReasoning("Redraw destiny — risky, low average in reserve", -50.0f);
                        }
                    }
                } catch (Exception e) {
                    action.addReasoning("Redraw destiny", GOOD_DELTA);
                }
            }

            // ========== Cancel Weapon Targeting ==========
            else if (textLower.contains("cancel") && textLower.contains("weapon") && textLower.contains("target")) {
                action.setActionType(ActionType.CANCEL);
                action.addReasoning("Cancel weapon targeting - protect our characters!", VERY_GOOD_DELTA);
            }

            // ========== Immune to Attrition ==========
            else if (textLower.contains("immune to attrition")) {
                action.addReasoning("Make character immune to attrition - valuable protection!", VERY_GOOD_DELTA);
            }

            // ========== Protect Forfeit ==========
            else if (textLower.contains("forfeit") &&
                     (textLower.contains("protect") || textLower.contains("preserved"))) {
                action.addReasoning("Protect forfeit value during battle", GOOD_DELTA + 10.0f);
            }

            // ========== Re-target Weapon ==========
            else if (textLower.contains("re-target") || textLower.contains("retarget")) {
                action.addReasoning("Re-target weapon at enemy - turn their weapon against them!", VERY_GOOD_DELTA);
            }

            // ========== Cancel Battle Damage (Houjix/Ghhhk) ==========
            else if (actionText.contains("Cancel all remaining battle damage")) {
                action.setActionType(ActionType.CANCEL_DAMAGE);
                evaluateHoujixGhhhk(action, context);
            }

            // ========== Take Card Into Hand ==========
            else if (actionText.contains("Take") && actionText.contains("into hand")) {
                evaluateTakeIntoHand(action, context, actionText, textLower);
            }

            // ========== Prevent Battle/Move (Barrier Cards) ==========
            else if (actionText.contains("Prevent") && actionText.contains("from battling or moving")) {
                evaluateBarrier(action, context, actionText);
            }

            // ========== Monnok-type (Reveal Hand) ==========
            else if (actionText.contains("LOST: Reveal opponent's hand")) {
                int theirHandSize = gameState != null ? gameState.getHand(context.getOpponentId()).size() : 0;
                if (theirHandSize > 6) {
                    action.addReasoning("Opponent has many cards - reveal worth it", VERY_GOOD_DELTA);
                } else {
                    action.addReasoning("Opponent has few cards - save reveal", VERY_BAD_DELTA);
                }
            }

            // ========== Dangerous Cards ==========
            else if (textLower.contains("stardust") || textLower.contains("on the edge")) {
                action.addReasoning("Known dangerous card", VERY_BAD_DELTA);
            }

            // ========== Draw Card Into Hand ==========
            else if (actionText.equals("Draw card into hand from Force Pile")) {
                action.setActionType(ActionType.DRAW);
                action.addReasoning("Draw option (see DrawEvaluator)", 0.0f);
            }

            // ========== Movement Actions ==========
            else if (actionText.contains("Move using") || actionText.contains("Shuttle") ||
                     actionText.contains("Docking bay transit") || actionText.contains("Transport")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Movement option (see MoveEvaluator)", 0.0f);

                // === V35.4: BOOST MOVEMENT WHEN ENEMY SPY/PRESENCE BLOCKS OUR DRAIN ===
                // If our character is at ANY location where an opponent (including undercover spy)
                // has presence, our force drain is blocked. Moving away lets us drain elsewhere.
                // Undercover spies deploy on OUR side but count as opponent presence!
                if (gameState != null && context.getPlayerId() != null) {
                    try {
                        String opponentId = gameState.getOpponent(context.getPlayerId());
                        for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                            if (loc == null || loc.getTitle() == null) continue;

                            boolean weHavePresence = false;
                            boolean oppHasPresence = false;
                            boolean oppHasUndercoverSpy = false;
                            for (com.gempukku.swccgo.game.PhysicalCard card : gameState.getCardsAtLocation(loc)) {
                                if (card == null) continue;
                                if (context.getPlayerId().equals(card.getOwner())) {
                                    weHavePresence = true;
                                    // V35.4: Check if this is actually an opponent's undercover spy
                                    // Undercover spies appear on our side but are opponent cards
                                    if (card.isUndercover()) {
                                        oppHasUndercoverSpy = true;
                                    }
                                } else if (opponentId != null && opponentId.equals(card.getOwner())) {
                                    oppHasPresence = true;
                                }
                            }
                            // If opponent has presence (or undercover spy) at our location, drain is blocked
                            if (weHavePresence && (oppHasPresence || oppHasUndercoverSpy)) {
                                float spyBonus = oppHasUndercoverSpy ? 250.0f : 150.0f;
                                action.addReasoning(String.format(
                                    "V35.4: %s blocking drain at %s — move away to drain elsewhere!",
                                    oppHasUndercoverSpy ? "UNDERCOVER SPY" : "Enemy presence",
                                    loc.getTitle()), spyBonus);
                                logger.warn("V35.4: {} at {} blocking our drain — boosting movement (+{})",
                                    oppHasUndercoverSpy ? "UNDERCOVER SPY" : "Enemy",
                                    loc.getTitle(), (int)spyBonus);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V35.4: Error checking spy-blocked sites: {}", e.getMessage());
                    }
                }

                // === V29.7: VADER'S CASTLE RETREAT PENALTY ===
                // Mustafar: Vader's Castle can teleport Vader back to Mustafar.
                // This is TERRIBLE when Vader is at a location where he can force drain!
                // Mustafar has 0 opponent icons = no drain value. Moving Vader there
                // means losing a turn of draining at the current location.
                // Only allow Castle retreat if Vader is outnumbered and about to die.
                if ((textLower.contains("vader") && textLower.contains("castle")) ||
                    textLower.contains("mustafar")) {
                    try {
                        // Find Vader's current location and check drain potential
                        String pid = context.getPlayerId();
                        if (gameState != null && pid != null) {
                            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                                if (card == null || !pid.equals(card.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone zone = card.getZone();
                                if (zone == null || !zone.isInPlay()) continue;
                                String cTitle = card.getTitle();
                                if (cTitle == null || !cTitle.toLowerCase(java.util.Locale.ROOT).contains("vader")) continue;
                                if (card.getBlueprint() == null || card.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;

                                // Found Vader — check his current location
                                PhysicalCard vaderLoc = card.getAtLocation();
                                if (vaderLoc == null && card.getAttachedTo() != null) {
                                    // Vader might be aboard a vehicle/starship — get the vehicle's location
                                    vaderLoc = card.getAttachedTo().getAtLocation();
                                }
                                if (vaderLoc != null && vaderLoc.getTitle() != null) {
                                    String vLocTitle = vaderLoc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                    if (vLocTitle.contains("mustafar")) {
                                        // Vader is already at Mustafar — this is a move OUT, which is fine
                                        break;
                                    }
                                    // Vader is at a non-Mustafar location — check if it has drain value
                                    SwccgCardBlueprint locBp = vaderLoc.getBlueprint();
                                    if (locBp != null) {
                                        int oppIcons = 0;
                                        if (context.getSide() == Side.DARK) {
                                            oppIcons = locBp.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                        } else {
                                            oppIcons = locBp.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE);
                                        }
                                        if (oppIcons > 0) {
                                            // Vader is at a location with drain value — DON'T retreat!
                                            action.addReasoning("V29.7 VADER RETREAT: Vader is draining " + oppIcons +
                                                " at " + vaderLoc.getTitle() + " — DON'T retreat to Mustafar!", -300.0f);
                                            logger.warn("V29.7 VADER RETREAT BLOCKED: Vader at {} with {} drain — retreating to Mustafar is terrible! (-300)",
                                                vaderLoc.getTitle(), oppIcons);
                                        }
                                    }
                                }
                                break; // Found Vader, done
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V29.7: Error checking Vader retreat: {}", e.getMessage());
                    }
                }
            }
            else if (actionText.equals("Take off") || actionText.equals("Land")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Take off/Land option (see MoveEvaluator)", 0.0f);
            }

            // ========== Make Opponent Lose Force ==========
            else if (actionText.contains("Make opponent lose")) {
                action.addReasoning("Making opponent lose force", GOOD_DELTA);
            }

            // ========== V29.7: Deploy Docking Bay — Smart Strategy ==========
            // Docking bays are SHARED — opponent can deploy characters to YOUR docking bays!
            // Only deploy a docking bay if we don't already have empty ones on the table.
            // Empty docking bays = free locations for the opponent.
            else if (actionText.contains("Deploy docking bay") || textLower.contains("deploy a docking bay")) {
                boolean hasEmptyDockingBay = false;
                int emptyBayCount = 0;
                int totalOurBays = 0;
                GameState bayGs = context.getGameState();
                String bayPlayerId = context.getPlayerId();
                if (bayGs != null && bayPlayerId != null) {
                    try {
                        for (PhysicalCard loc : bayGs.getTopLocations()) {
                            if (loc == null || loc.getTitle() == null) continue;
                            String locTitle = loc.getTitle().toLowerCase(java.util.Locale.ROOT);
                            // Check if this is a docking bay we own
                            if (locTitle.contains("docking bay") || locTitle.contains("landing platform")) {
                                // Check if we control it (our card)
                                if (bayPlayerId.equals(loc.getOwner())) {
                                    totalOurBays++;
                                    // Check if any of OUR characters are there
                                    boolean hasFriendlyChar = false;
                                    java.util.List<PhysicalCard> cardsHere = bayGs.getCardsAtLocation(loc);
                                    if (cardsHere != null) {
                                        for (PhysicalCard pc : cardsHere) {
                                            if (pc != null && bayPlayerId.equals(pc.getOwner())
                                                && pc.getBlueprint() != null
                                                && pc.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                                hasFriendlyChar = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!hasFriendlyChar) {
                                        hasEmptyDockingBay = true;
                                        emptyBayCount++;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }
                }

                if (hasEmptyDockingBay) {
                    // Already have empty docking bays — deploying MORE just gives opponent more free locations!
                    action.addReasoning("V29.7 DOCKING BAY: Already have " + emptyBayCount
                        + " empty bay(s) — deploy characters there first, don't give opponent more locations!", -200.0f);
                    logger.warn("V29.7 DOCKING BAY BLOCKED: {} empty bay(s) on table — don't deploy more (-200)", emptyBayCount);
                } else if (totalOurBays >= 2) {
                    // Already have 2+ bays with characters — probably don't need more
                    action.addReasoning("V29.7 DOCKING BAY: Already have " + totalOurBays + " bays — enough for transit", -50.0f);
                } else if (totalOurBays == 0) {
                    // V29.7: FIRST docking bay — VERY high priority! This creates a battleground
                    // location where our characters can safely deploy. Must fire BEFORE character
                    // deploys so characters have a friendly BG location to go to.
                    action.addReasoning("V29.7 FIRST DOCKING BAY: Deploy FIRST to create battleground for characters!", 200.0f);
                    logger.warn("V29.7 FIRST BAY: No bays on table — high priority deploy (+200)");
                } else {
                    // Have 1 manned bay — second bay OK for transit network
                    action.addReasoning("V29.7 DOCKING BAY: Deploy second bay for transit network", GOOD_DELTA);
                }
            }

            // ========== V25: HUNT DOWN V — VADER CASTLE DEPLOY ACTION ==========
            // If the action deploys Vader from Reserve Deck (via Vader's Castle), and
            // Hunt Down V is the objective, this is THE most important action in the game.
            // Vader must be on table for the deck to function.
            else if (actionText.contains("Deploy Vader from Reserve Deck") || actionText.contains("Deploy Vader here")) {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer vaderObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (vaderObjAnalyzer != null && vaderObjAnalyzer.isAnalyzed() && vaderObjAnalyzer.isHuntDownV()) {
                    boolean vaderOnTable = false;
                    int forceAvailable = 0;
                    if (context.getGame() != null && context.getGame().getGameState() != null) {
                        com.gempukku.swccgo.game.state.GameState vaderGs = context.getGame().getGameState();
                        vaderOnTable = vaderObjAnalyzer.isVaderOnTable(vaderGs, context.getPlayerId());
                        forceAvailable = vaderGs.getForcePileSize(context.getPlayerId());
                    }
                    if (!vaderOnTable) {
                        // V25: Don't attempt Vader Castle deploy if not enough Force
                        // Vader's deploy cost is typically 6. This is a once-per-game action
                        // so we must NOT waste it when we can't afford to deploy him.
                        if (forceAvailable < 6) {
                            action.addReasoning("V25 HUNT DOWN: NOT ENOUGH FORCE for Vader! Need 6, have " + forceAvailable + ". SAVE Castle action!", -500.0f);
                            logger.warn("V25 HUNT DOWN: Vader Castle deploy BLOCKED — only {} Force available (need 6)", forceAvailable);
                        } else {
                            action.addReasoning("V25 HUNT DOWN: DEPLOY VADER NOW! Have " + forceAvailable + " Force, deck cannot function without him!", VERY_GOOD_DELTA + 500.0f);
                            logger.warn("V25 HUNT DOWN: Vader Castle deploy action — TOP PRIORITY (+{}) with {} Force", (int)(VERY_GOOD_DELTA + 500.0f), forceAvailable);
                        }
                    } else {
                        action.addReasoning("Vader already on table — Castle deploy not urgent", 0.0f);
                    }
                } else {
                    action.addReasoning("Deploy Vader from reserve", VERY_GOOD_DELTA);
                }
            }

            // ========== V26/V29.6: Dining Room — Deploy Lando (TDIGWATT) ==========
            // Dining Room's game text deploys Lando from Reserve Deck — a key TDIGWATT piece.
            // DeployEvaluator can't find the card (it's in reserve, not hand), so we boost
            // here in ActionTextEvaluator.
            //
            // V29.6 FIX: Check if Lando would be ALONE at Dining Room. If no friendly
            // characters are already there, deploying Lando alone is suicide — opponent
            // will drop a character + weapon and kill him immediately. Defer until we
            // have a buddy at Dining Room first.
            else if ((textLower.contains("dining room") || textLower.contains("deploy lando"))
                     && textLower.contains("reserve")) {
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer drLandoAnalyzer =
                    context.getObjectiveAnalyzer();

                // V29.6: Check if there are friendly characters at Dining Room
                boolean friendliesAtDiningRoom = false;
                int friendlyCountAtDR = 0;
                try {
                    GameState drGameState = context.getGameState();
                    String drPlayerId = context.getPlayerId();
                    if (drGameState != null && drPlayerId != null) {
                        // Find Dining Room on the table
                        java.util.List<PhysicalCard> allLocs = drGameState.getTopLocations();
                        PhysicalCard diningRoomCard = null;
                        if (allLocs != null) {
                            for (PhysicalCard loc : allLocs) {
                                if (loc != null && loc.getTitle() != null
                                    && loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("dining room")) {
                                    diningRoomCard = loc;
                                    break;
                                }
                            }
                        }
                        if (diningRoomCard != null) {
                            java.util.List<PhysicalCard> cardsAtDR = drGameState.getCardsAtLocation(diningRoomCard);
                            if (cardsAtDR != null) {
                                for (PhysicalCard c : cardsAtDR) {
                                    if (c != null && drPlayerId.equals(c.getOwner())
                                        && c.getBlueprint() != null
                                        && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        friendlyCountAtDR++;
                                    }
                                }
                            }
                            friendliesAtDiningRoom = (friendlyCountAtDR > 0);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V29.6 DINING ROOM: Error checking friendlies at DR: {}", e.getMessage());
                }

                if (drLandoAnalyzer != null && drLandoAnalyzer.isAnalyzed()
                    && drLandoAnalyzer.needsBespinSystemPresence()) {
                    if (friendliesAtDiningRoom) {
                        // Buddies present — safe to deploy Lando!
                        action.addReasoning("V29.6 DINING ROOM: Deploy Lando with " + friendlyCountAtDR + " friendlies — safe!", 150.0f);
                        logger.warn("V29.6 DINING ROOM: Lando deploy with {} friendlies at DR — +150", friendlyCountAtDR);
                    } else {
                        // Lando would be ALONE — defer until we have a buddy there.
                        // Small positive so it's still considered but won't beat deploying a character first.
                        action.addReasoning("V29.6 DINING ROOM: Lando would be ALONE — deploy a buddy first!", -30.0f);
                        logger.warn("V29.6 DINING ROOM: Lando deploy DEFERRED — no friendlies at DR, penalty -30");
                    }
                } else {
                    if (friendliesAtDiningRoom) {
                        action.addReasoning("Dining Room: Deploy Lando from reserve (friendlies present)", GOOD_DELTA);
                    } else {
                        action.addReasoning("V29.6 Dining Room: Lando alone — risky!", -20.0f);
                        logger.info("V29.6 DINING ROOM: Non-TDIGWATT Lando deploy deferred — alone at DR");
                    }
                }
            }

            // ========== Deploy From Reserve (Risky) ==========
            else if (actionText.contains("Deploy") && actionText.contains("from")) {
                action.addReasoning("Deploying from reserve - mild caution", -10.0f);
            }

            // ========== Embark ==========
            else if (actionText.contains("Embark")) {
                action.setActionType(ActionType.MOVE);
                evaluateEmbark(action, context, actionText);
            }

            // ========== Disembark/Relocate/Transfer ==========
            else if (actionText.contains("Disembark") || actionText.contains("Relocate") ||
                     actionText.contains("Transfer")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Usually avoid disembark/relocate/transfer", VERY_BAD_DELTA);
            }

            // ========== Ship-dock ==========
            else if (actionText.contains("Ship-dock")) {
                action.addReasoning("Avoid ship-docking", VERY_BAD_DELTA);
            }

            // ========== Place in Lost Pile ==========
            else if (actionText.contains("Place in Lost Pile")) {
                action.addReasoning("Avoid losing cards", VERY_BAD_DELTA);
            }

            // ========== Grab ==========
            else if (actionText.contains("Grab")) {
                evaluateGrab(action, context, actionText);
            }

            // ========== Break Cover ==========
            else if (actionText.contains("Break cover")) {
                evaluateBreakCover(action, context, actionText);
            }

            // ========== Retrieve Force ==========
            else if (textLower.contains("retrieve") || actionText.contains("Place out of play to retrieve")) {
                int lostPileSize = gameState != null ? gameState.getLostPile(context.getPlayerId()).size() : 0;
                if (lostPileSize > 15) {
                    action.addReasoning("High lost pile - retrieve worth it", GOOD_DELTA);
                } else {
                    action.addReasoning("Low lost pile - save retrieve", BAD_DELTA);
                }
            }

            // ========== Defensive Shields ==========
            // V29.1: Shield pacing — don't burn all 4 shield slots immediately.
            // Play 2 shields on turn 1 to get basic protection, then WAIT to see
            // what the opponent is running before committing the remaining slots.
            // This lets us pick targeted counters instead of generic shields.
            else if (actionText.contains("Play a Defensive Shield")) {
                if (!context.isMyTurn()) {
                    action.addReasoning("Defensive shield during opponent's turn - prefer pass", -10.0f);
                } else {
                    // Check shield pacing via ShieldStrategy
                    com.gempukku.swccgo.ai.models.rando.strategy.ShieldStrategy shieldStrat = context.getShieldStrategy();
                    int turnNum = context.getTurnNumber();
                    if (shieldStrat != null && shieldStrat.atPacingCap(turnNum)) {
                        // We've played enough shields for this turn — hold remaining slots
                        action.addReasoning("V29.1 SHIELD PACING: Holding shield slot — wait to scout opponent (turn " + turnNum + ")", -40.0f);
                    } else {
                        action.addReasoning("Defensive shield", VERY_GOOD_DELTA);
                    }
                }
            }

            // ========== Deploy on table/location ==========
            else if (actionText.startsWith("Deploy on")) {
                if (textLower.contains("projection") && textLower.contains("side")) {
                    action.addReasoning("Never put projection on side of table", VERY_BAD_DELTA);
                } else {
                    action.addReasoning("Deploy on location/table", GOOD_DELTA);
                }
            }

            // ========== Deploy unique ==========
            else if (actionText.startsWith("Deploy unique")) {
                action.addReasoning("Special battleground deploy", GOOD_DELTA);
            }

            // ========== USED: Peek at top ==========
            else if (actionText.startsWith("USED: Peek at top")) {
                action.addReasoning("Peek for card advantage", GOOD_DELTA);
            }

            // ========== Force Drain Cancellation ==========
            else if (actionText.contains("Cancel Force drain")) {
                if (context.isMyTurn()) {
                    action.addReasoning("Don't cancel own force drain", VERY_BAD_DELTA);
                } else {
                    action.addReasoning("Cancel opponent's force drain", GOOD_DELTA);
                }
            }

            // ========== V22.3: Maintenance Cost Satisfaction ==========
            // When a maintenance card's upkeep is due, Rando gets a choice:
            //   "Use X Force" (pay maintenance — KEEP the card)
            //   "Place out of play" / "Sacrifice" (lose the card forever)
            // ALWAYS prefer paying over sacrificing — the card was deployed for a reason!
            else if ((textLower.contains("maintenance") || textLower.contains("satisfy"))
                     && (textLower.contains("use") && textLower.contains("force"))) {
                // This is the PAY option — strongly prefer it
                action.addReasoning("V22.3 MAINTENANCE: Pay to keep card alive!", 200.0f);
                logger.warn("V22.3 MAINTENANCE PAY: Choosing to pay maintenance - '{}'", actionText);
            }
            else if ((textLower.contains("sacrifice") || textLower.contains("place out of play")
                      || textLower.contains("lost pile"))
                     && (textLower.contains("maintenance") || textLower.contains("satisfy"))) {
                // This is the SACRIFICE option — heavily penalize
                action.addReasoning("V22.3 MAINTENANCE: DON'T sacrifice - pay instead!", -300.0f);
                logger.warn("V22.3 MAINTENANCE SACRIFICE: Avoiding sacrifice - '{}'", actionText);
            }

            // ========== Use/Lose Force Actions ==========
            else if (textLower.startsWith("use ") && textLower.contains(" force ")) {
                // V22.3: Check if this might be a maintenance payment
                // Maintenance decisions often just say "Use X Force" without "maintenance" keyword
                // If the decision context involves a maintenance card, prefer paying
                if (textLower.contains("cost") || textLower.contains("upkeep")) {
                    action.addReasoning("V22.3 MAINTENANCE: Pay upkeep cost!", 150.0f);
                    logger.warn("V22.3 MAINTENANCE: Likely upkeep payment - '{}'", actionText);
                } else {
                    // V24.5: No randomness — generic use force should be avoided
                    action.addReasoning("'Use Force' action — prefer not to use force unnecessarily", -20.0f);
                }
            }
            else if (textLower.startsWith("lose ") && textLower.contains(" force ")) {
                // V24.5: No randomness — losing force is almost always bad
                action.addReasoning("'Lose Force' action — avoid losing force", -30.0f);
            }
            // V22.3: Catch generic sacrifice options that aren't tagged as maintenance
            else if (textLower.contains("sacrifice") || textLower.contains("place out of play")) {
                action.addReasoning("V22.3: Avoid sacrificing cards — prefer alternatives", -150.0f);
                logger.info("V22.3 SACRIFICE PENALTY: '{}'", actionText);
            }

            // ========== V22.5: Alert My Star Destroyer / Ship Deployment Priority ==========
            // "Alert My Star Destroyer" deploys Executor + pilot for cheap.
            // This is CRITICAL for TDIGWATT — Bespin system occupation enables Dark Deal
            // and Cloud City Occupation, which are the deck's primary damage engines.
            else if (textLower.contains("reveal") && (textLower.contains("star destroyer") || textLower.contains("pilot"))) {
                // Check if we have a ship at Bespin system already
                boolean hasBespinShip = false;
                if (gameState != null) {
                    try {
                        String pid = context.getPlayerId();
                        for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                            if (loc != null && loc.getTitle() != null &&
                                loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                float ourPower = context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, pid, false, false);
                                if (ourPower > 0) hasBespinShip = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                if (!hasBespinShip) {
                    action.addReasoning("V22.5 CRITICAL: Deploy ship to Bespin! Enables Dark Deal + CC Occupation!", 300.0f);
                    logger.warn("V22.5 BESPIN PRIORITY: Alert My Star Destroyer — no ship at Bespin yet! (+300)");
                } else {
                    action.addReasoning("V22.5: Deploy ship (Bespin already occupied)", 100.0f);
                    logger.info("V22.5: Alert My Star Destroyer — Bespin already has ship presence");
                }
            }
            // V22.5: Generic "deploy simultaneously" or ship+pilot combos
            else if (textLower.contains("deploy") && textLower.contains("simultaneously")) {
                action.addReasoning("V22.5: Deploy pilot+ship combo - efficient!", 120.0f);
                logger.info("V22.5: Simultaneous deploy detected");
            }

            // ========== V25: INITIATE BATTLE ==========
            // Battle initiation was previously unhandled (fell to default 0.0f) which
            // meant Rando NEVER chose to initiate battles because other actions always
            // outscored them. Now we evaluate the specific location's power differential.
            else if (actionText.contains("Initiate battle") || actionText.contains("initiate battle")) {
                action.setActionType(ActionType.BATTLE);
                boolean battleScored = false;

                SwccgGame battleGame = context.getGame();
                if (battleGame != null && context.getGame().getGameState() != null) {
                    com.gempukku.swccgo.game.state.GameState bGs = battleGame.getGameState();
                    String bPlayerId = context.getPlayerId();
                    String bOpponentId = bGs.getOpponent(bPlayerId);

                    if (bOpponentId != null) {
                        try {
                            // Find which location this battle targets
                            for (PhysicalCard bLoc : bGs.getTopLocations()) {
                                String bLocTitle = bLoc.getTitle();
                                if (bLocTitle != null && actionText.contains(bLocTitle)) {
                                    float ourPower = battleGame.getModifiersQuerying().getTotalPowerAtLocation(
                                        bGs, bLoc, bPlayerId, false, false);
                                    float theirPower = battleGame.getModifiersQuerying().getTotalPowerAtLocation(
                                        bGs, bLoc, bOpponentId, false, false);
                                    float ourAbility = battleGame.getModifiersQuerying().getTotalAbilityAtLocation(
                                        bGs, bPlayerId, bLoc);
                                    float theirAbility = battleGame.getModifiersQuerying().getTotalAbilityAtLocation(
                                        bGs, bOpponentId, bLoc);
                                    float powerDiff = ourPower - theirPower;
                                    float abilityDiff = ourAbility - theirAbility;
                                    // Ability matters: each point of ability = roughly 2.5 power via destiny draws
                                    float effectiveDiff = powerDiff + (abilityDiff * 2.5f);

                                    logger.warn("V25 BATTLE EVAL at {}: our power={} ability={}, their power={} ability={}, effectiveDiff={}",
                                        bLocTitle, (int)ourPower, (int)ourAbility, (int)theirPower, (int)theirAbility, (int)effectiveDiff);

                                    if (theirPower <= 0) {
                                        // No opponent here — can't battle
                                        action.addReasoning("V25 BATTLE: No opponent at " + bLocTitle, -100.0f);
                                    } else if (theirPower > ourPower * 2 && theirPower > 6) {
                                        // Suicidal — hard block
                                        action.addReasoning(String.format("V25 BATTLE SUICIDE: %.0f vs %.0f at %s — NEVER!",
                                            ourPower, theirPower, bLocTitle), -500.0f);
                                    } else if (effectiveDiff >= 8) {
                                        // Crushing advantage
                                        action.addReasoning(String.format("V25 BATTLE CRUSH at %s: %.0f vs %.0f — ATTACK!",
                                            bLocTitle, ourPower, theirPower), 200.0f);
                                    } else if (effectiveDiff >= 5) {
                                        // Strong advantage
                                        action.addReasoning(String.format("V25 BATTLE FAVORABLE at %s: %.0f vs %.0f",
                                            bLocTitle, ourPower, theirPower), 120.0f);
                                    } else if (effectiveDiff >= 2) {
                                        // Marginal advantage
                                        action.addReasoning(String.format("V25 BATTLE MARGINAL at %s: %.0f vs %.0f",
                                            bLocTitle, ourPower, theirPower), 60.0f);
                                    } else if (effectiveDiff >= -2) {
                                        // Even — slight positive to encourage aggression
                                        action.addReasoning(String.format("V25 BATTLE EVEN at %s: %.0f vs %.0f — risky but worth trying",
                                            bLocTitle, ourPower, theirPower), 20.0f);
                                    } else {
                                        // Unfavorable
                                        float penalty = -60.0f;
                                        if (effectiveDiff < -8) penalty = -120.0f;
                                        if (effectiveDiff < -15) penalty = -250.0f;
                                        action.addReasoning(String.format("V25 BATTLE UNFAVORABLE at %s: %.0f vs %.0f — avoid!",
                                            bLocTitle, ourPower, theirPower), penalty);
                                    }
                                    battleScored = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("V25 BATTLE: Error evaluating battle: {}", e.getMessage());
                        }
                    }
                }

                if (!battleScored) {
                    // Fallback: give modest positive score to encourage battling
                    action.addReasoning("V25 BATTLE: Initiate battle (no location data)", 30.0f);
                }

                // Check reserve for destiny draws
                int battleReserve = 0;
                if (context.getGame() != null && context.getGame().getGameState() != null) {
                    battleReserve = context.getGame().getGameState().getReserveDeckSize(context.getPlayerId());
                }
                if (battleReserve < 3) {
                    action.addReasoning("V25 BATTLE: Low reserve (" + battleReserve + ") — bad destiny draws!", -50.0f);
                }

                logger.warn("V25 BATTLE: '{}' scored {}", actionText.length() > 60 ? actionText.substring(0,60) + "..." : actionText,
                    String.format("%.1f", action.getScore()));
            }

            // ========== V29.6/V29.11: BLASTER RACK — ONLY RACK TO SAVE WEAPONS FROM DYING CHARACTERS ==========
            // Blaster Rack stacks a weapon on it. This is ONLY useful at the END of a battle
            // when a character carrying the weapon has been HIT or is about to be forfeited
            // to satisfy attrition/battle damage. Proactively racking weapons outside of battle
            // damage resolution is terrible — it strips characters of weapons before they can fire.
            // Example: Vader had lightsaber, Rando racked it, Vader went to battle unarmed.
            // Action text can be "Stack character weapon" OR contain "rack" + "stack"
            else if ((textLower.contains("rack") && textLower.contains("stack"))
                || (textLower.contains("stack") && textLower.contains("character weapon"))) {
                Phase rackPhase = context.getPhase();
                // Check if we're in battle damage/attrition resolution
                // During battle damage, the decision text often references damage/attrition/forfeit
                boolean duringBattleDamage = false;
                try {
                    GameState rackGs = context.getGameState();
                    if (rackGs != null && rackGs.isDuringBattle()) {
                        // We're in a battle — check if damage is being resolved
                        // If the game is asking us to use rack during battle, it's likely
                        // because we're about to lose the character carrying the weapon.
                        duringBattleDamage = true;
                    }
                } catch (Exception e) {
                    logger.debug("V29.6 RACK: Error checking battle state: {}", e.getMessage());
                }

                if (duringBattleDamage) {
                    // V35.2: During battle — but ONLY rack weapons from characters AT the battle!
                    // Bug: Rando racked Vader's Lightsaber from Mustafar while battle was at Mos Eisley.
                    boolean weaponCharAtBattle = false;
                    try {
                        GameState rackGs2 = context.getGameState();
                        if (rackGs2 != null && rackGs2.getBattleState() != null) {
                            PhysicalCard battleLoc = rackGs2.getBattleState().getBattleLocation();
                            if (battleLoc != null) {
                                String rackPid = context.getPlayerId();
                                for (PhysicalCard tableCard : rackGs2.getAllPermanentCards()) {
                                    if (tableCard == null || !rackPid.equals(tableCard.getOwner())) continue;
                                    if (tableCard.getBlueprint() == null) continue;
                                    if (tableCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.WEAPON) continue;
                                    com.gempukku.swccgo.common.Zone wz = tableCard.getZone();
                                    if (wz == null || !wz.isInPlay()) continue;
                                    String wTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (wTitle.isEmpty() || !textLower.contains(wTitle)) continue;
                                    PhysicalCard parentChar = tableCard.getAttachedTo();
                                    if (parentChar != null) {
                                        PhysicalCard charLoc = parentChar.getAtLocation();
                                        if (charLoc != null && charLoc == battleLoc) {
                                            weaponCharAtBattle = true;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V35.2 RACK: Error checking weapon location: {}", e.getMessage());
                        weaponCharAtBattle = true; // Default to allow if check fails
                    }

                    if (weaponCharAtBattle) {
                        action.addReasoning("V35.2 RACK: Character in battle — save weapon!", 80.0f);
                        logger.warn("V35.2 RACK: Weapon's character AT battle — saving '{}'", actionText);
                    } else {
                        action.addReasoning("V35.2 RACK: Character NOT in this battle — do NOT rack!", -500.0f);
                        logger.warn("V35.2 RACK: BLOCKED — weapon's character not at battle! '{}'", actionText);
                    }
                } else {
                    // Outside battle — proactive racking is TERRIBLE
                    action.addReasoning("V29.6 BLASTER RACK: Do NOT rack weapons outside battle — characters need them!", -500.0f);
                    logger.warn("V29.6 BLASTER RACK: BLOCKED proactive racking outside battle — '{}'", actionText);
                }
            }

            // ========== Default/Unknown ==========
            else {
                action.addReasoning("Unknown action type", 0.0f);
                logger.trace("Unrecognized action: {}", actionText);
            }

            actions.add(action);
        }

        return actions;
    }

    // ========== Helper Methods ==========

    private void evaluateActivateForce(EvaluatedAction action, DecisionContext context) {
        // V38.3: ALWAYS activate Force. ALWAYS. No exceptions.
        // Force is the currency for deploying characters. Without Force, Rando
        // can't deploy, can't fight, and slowly loses by attrition.
        // The old code had a Force pile cap of 20 and reserve-low checks that
        // caused Rando to skip activation entirely, leading to death spirals.
        // The ForceActivationEvaluator (INTEGER handler) now manages how MUCH
        // to activate. This function just needs to score the ACTION highly.
        action.addReasoning("V38.3 ALWAYS ACTIVATE: Force is currency — activate it!", 500.0f);
        logger.info("V38.3 ACTIVATE FORCE: Scored +500 — always activate");
    }

    private void evaluateForceDrain(EvaluatedAction action, DecisionContext context, String locationCardId) {
        // Force drains are generally good unless under Battle Order rules
        // Ported from Python action_text_evaluator.py lines 351-493

        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        // ========== V24.15: NEVER force drain at 0! ==========
        // Draining for 0 does nothing but opens us up to Surprise Assault and other traps.
        // Check the actual drain amount at the location before committing.
        if (gameState != null && locationCardId != null) {
            try {
                PhysicalCard drainLocation = gameState.findCardById(Integer.parseInt(locationCardId));
                if (drainLocation != null) {
                    SwccgGame drainGame = context.getGame();
                    if (drainGame != null) {
                        float drainAmount = drainGame.getModifiersQuerying().getForceDrainAmount(
                            gameState, drainLocation, playerId);
                        if (drainAmount <= 0) {
                            action.addReasoning("V24.15 DRAIN BLOCK: Force drain would be 0 — pointless and opens us to Surprise Assault!", -9999.0f);
                            logger.warn("V24.15 DRAIN BLOCK: Force drain at {} would be {} — HARD BLOCKING to avoid Surprise Assault trap!",
                                drainLocation.getTitle(), drainAmount);
                            return;
                        } else {
                            logger.info("V24.15 DRAIN CHECK: Force drain at {} will be {} — proceeding", drainLocation.getTitle(), drainAmount);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V24.15: Error checking drain amount: {}", e.getMessage());
            }
        }

        // === V25: SIMPLE TRICKS AND NONSENSE — avoid draining at non-battleground sites ===
        // Simple Tricks cancels Force drains at non-battleground sites. If opponent has it
        // on table, draining at non-battleground sites is pointless (gets cancelled).
        // Check for this BEFORE spending resources on the drain.
        if (gameState != null && locationCardId != null) {
            try {
                PhysicalCard drainLoc = gameState.findCardById(Integer.parseInt(locationCardId));
                if (drainLoc != null) {
                    SwccgCardBlueprint locBp = drainLoc.getBlueprint();
                    // Check if the drain location is a non-battleground site
                    boolean isBattlegroundSite = false;
                    if (locBp != null) {
                        // A battleground site typically has force icons from both sides
                        isBattlegroundSite = locBp.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                            && locBp.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                    }

                    if (!isBattlegroundSite) {
                        // Check if opponent has Simple Tricks And Nonsense on table
                        String opponentId = gameState.getOpponent(playerId);
                        boolean simpleTricksOnTable = false;
                        if (opponentId != null) {
                            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                                if (card == null || !opponentId.equals(card.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone zone = card.getZone();
                                if (zone == null || !zone.isInPlay()) continue;
                                String cardTitle = card.getTitle();
                                if (cardTitle != null && cardTitle.contains("Simple Tricks")) {
                                    simpleTricksOnTable = true;
                                    break;
                                }
                            }
                        }

                        if (simpleTricksOnTable) {
                            action.addReasoning("V25 SIMPLE TRICKS: Non-battleground drain will be CANCELLED by Simple Tricks And Nonsense!", -9999.0f);
                            logger.warn("V25 SIMPLE TRICKS: BLOCKING drain at non-battleground {} — opponent has Simple Tricks!",
                                drainLoc.getTitle());
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V25 Simple Tricks check error: {}", e.getMessage());
            }
        }

        // Check if we're under Battle Order rules (force drains cost +3 extra)
        // Battle Order is typically triggered when opponent has mains + specific cards
        boolean underBattleOrder = false;
        com.gempukku.swccgo.ai.models.rando.strategy.StrategyController strategyController = context.getStrategyController();
        if (strategyController != null) {
            underBattleOrder = strategyController.isUnderBattleOrderRules();
        }

        // Get available force
        int forceAvailable = 0;
        if (gameState != null) {
            forceAvailable = gameState.getForcePileSize(playerId);
        }

        // Check if we have any deployable cards in hand
        boolean hasDeployableCard = false;
        int cheapestDeployCost = Integer.MAX_VALUE;
        if (gameState != null) {
            List<PhysicalCard> hand = gameState.getHand(playerId);
            if (hand != null) {
                for (PhysicalCard card : hand) {
                    if (card.getBlueprint() != null) {
                        CardCategory category = card.getBlueprint().getCardCategory();
                        if (category == CardCategory.CHARACTER || category == CardCategory.STARSHIP ||
                            category == CardCategory.VEHICLE) {
                            hasDeployableCard = true;
                            Float deployCost = card.getBlueprint().getDeployCost();
                            if (deployCost != null && deployCost < cheapestDeployCost) {
                                cheapestDeployCost = deployCost.intValue();
                            }
                        }
                    }
                }
            }
        }

        if (underBattleOrder) {
            // Under Battle Order rules - force drains cost extra (+3)
            int battleOrderCost = 3;

            // If we can't afford the drain (need 3+ force), skip it
            if (forceAvailable < battleOrderCost) {
                action.addReasoning("Under Battle Order but can't afford drain (need " + battleOrderCost + ", have " + forceAvailable + ")", VERY_BAD_DELTA);
                return;
            }

            // Check if we have deployable cards - if yes, save force for them
            if (hasDeployableCard && cheapestDeployCost < Integer.MAX_VALUE) {
                int forceAfterDrain = forceAvailable - battleOrderCost;
                if (forceAfterDrain < cheapestDeployCost) {
                    action.addReasoning("Under Battle Order - saving force for deploy (cost " + cheapestDeployCost + ")", VERY_BAD_DELTA);
                    return;
                }
            }

            // If NO deployable cards - drains are our only pressure! Boost them!
            if (!hasDeployableCard) {
                action.addReasoning("Under Battle Order but NO deployable cards - drain is our only pressure!", VERY_GOOD_DELTA + 20.0f);
                logger.info("🔥 FORCE DRAIN BOOST: No deployable cards under Battle Order");
                return;
            }

            // We can afford drain and have some force left - moderate score
            action.addReasoning("Under Battle Order - drain costs extra but affordable", GOOD_DELTA);

        } else {
            // Not under Battle Order - drain is generally good
            if (!hasDeployableCard) {
                // NO deployable cards - drains are our only pressure!
                action.addReasoning("Force drain (no deployable cards - our only pressure!)", VERY_GOOD_DELTA + 20.0f);
                logger.info("🔥 FORCE DRAIN BOOST: No deployable cards");
            } else {
                action.addReasoning("Force drain is good", VERY_GOOD_DELTA);
            }
        }

        // === V29.9: HUNT DOWN FORCE DRAIN PRIORITY ===
        // For Hunt Down (V or regular), force drains are extra valuable because:
        // 1. Visage Of The Emperor adds +1 to each drain while we occupy a battleground
        // 2. Vader's presence at battleground locations enables draining
        // 3. Hunt Down V gives bonus force loss from lightsaber combat
        // Boost force drains significantly when running Hunt Down objective.
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer drainObjAnalyzer = context.getObjectiveAnalyzer();
        if (drainObjAnalyzer != null && drainObjAnalyzer.isAnalyzed() && drainObjAnalyzer.isHuntDownV()) {
            // Check if we're draining at a location with opponent icons (actual drain value)
            boolean highValueDrain = false;
            if (gameState != null && locationCardId != null) {
                try {
                    PhysicalCard drainLoc = gameState.findCardById(Integer.parseInt(locationCardId));
                    if (drainLoc != null && drainLoc.getBlueprint() != null) {
                        int oppIcons = drainLoc.getBlueprint().getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                        if (oppIcons >= 2) {
                            highValueDrain = true;
                            action.addReasoning("V29.9 HUNT DOWN DRAIN: High-value drain location (" + oppIcons + " opponent icons)!", 40.0f);
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }
            // General Hunt Down drain boost
            action.addReasoning("V29.9 HUNT DOWN: Force drains are critical — Visage adds +1, keep pressure on!", 30.0f);
        }
    }

    private void evaluatePlayCard(EvaluatedAction action, DecisionContext context) {
        int forcePile = context.getForcePileSize();
        if (forcePile == 0) {
            action.addReasoning("No Force available - can't play cards!", VERY_BAD_DELTA);
        } else if (forcePile <= 1) {
            action.addReasoning("Very low Force (" + forcePile + ") - unlikely to afford cards", BAD_DELTA);
        } else {
            // V24.5: No randomness — slight positive for playing cards when force available
            action.addReasoning("Generic play card — moderate priority", 5.0f);
        }
    }

    private void evaluateDestinyProtection(EvaluatedAction action, DecisionContext context) {
        Phase phase = context.getPhase();
        int turnNumber = context.getTurnNumber();

        // These cards only useful if battle is coming
        if (turnNumber <= 1) {
            action.addReasoning("SAVE for battle turn! Turn 1 rarely battles", VERY_BAD_DELTA);
        } else if (phase == Phase.BATTLE) {
            action.addReasoning("Protect destiny draws - IN BATTLE NOW!", VERY_GOOD_DELTA);
        } else if (phase == Phase.ACTIVATE || phase == Phase.CONTROL || phase == Phase.DEPLOY) {
            action.addReasoning("Protect destiny draws - battle opportunity exists", GOOD_DELTA);
        } else {
            action.addReasoning("Save destiny protection for clear battle turn", BAD_DELTA);
        }
    }

    private void evaluateSenseCancel(EvaluatedAction action, DecisionContext context, String actionText) {
        String textLower = actionText.toLowerCase();
        boolean isDestinyBased = textLower.contains("draw destiny") || textLower.contains("if destiny");

        // V37.3: NEVER cancel your OWN interrupts!
        // Rando played FMFTD then Sensed his own FMFTD — self-sabotage.
        // Check if the interrupt being canceled was played by US.
        // Clue: if the action text mentions a card that we just played this turn,
        // or if we're the active player and the interrupt belongs to us.
        GameState senseGs = context.getGameState();
        if (senseGs != null) {
            try {
                String sensePid = context.getPlayerId();
                // Check if the interrupt target name matches one of OUR cards
                // Hunt Down specific: FMFTD, Force Lightning, Force Push are ours
                String[] ourInterrupts = {"far more frightening", "force lightning", "force push",
                    "stunning leader", "i have you now", "sniper", "dark strike",
                    "we must accelerate", "ghhhk", "force field", "no escape"};
                for (String ourInt : ourInterrupts) {
                    if (textLower.contains(ourInt)) {
                        action.addReasoning("V37.3 SENSE SELF-CANCEL: NEVER cancel our OWN interrupt!", -9999.0f);
                        logger.warn("V37.3 SENSE SELF-CANCEL: Tried to cancel our own '{}' — HARD BLOCKED!", ourInt);
                        return;
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }

        // Check priority cards system for target value
        AiPriorityCards.SenseTargetResult senseResult = AiPriorityCards.getSenseTargetValue(actionText);

        if (isDestinyBased) {
            if (senseResult.isHighValue && senseResult.score >= 80) {
                action.addReasoning("Destiny cancel critical target: " + senseResult.matchedPattern, 10.0f);
            } else {
                action.addReasoning("Destiny-based cancel (unreliable, skip)", -10.0f);
            }
        } else if (senseResult.isHighValue && senseResult.score >= 80) {
            action.addReasoning("Cancel CRITICAL target: " + senseResult.matchedPattern + "!", VERY_GOOD_DELTA + 20.0f);
        } else if (senseResult.isHighValue && senseResult.score >= 60) {
            action.addReasoning("Cancel high-value target: " + senseResult.matchedPattern, VERY_GOOD_DELTA);
        } else if (senseResult.isHighValue) {
            action.addReasoning("Cancel valuable target: " + senseResult.matchedPattern, GOOD_DELTA + 15.0f);
        } else if (textLower.contains("force drain")) {
            action.addReasoning("Cancel force drain", GOOD_DELTA + 5.0f);
        } else if (!context.isMyTurn()) {
            action.addReasoning("Cancel opponent interrupt (their turn)", GOOD_DELTA);
        } else {
            action.addReasoning("Cancel opponent interrupt (our turn)", 15.0f);
        }
    }

    private void evaluateHoujixGhhhk(EvaluatedAction action, DecisionContext context) {
        // These are CRITICAL survival cards
        // For now, give moderate positive score - ideally we'd check damage remaining
        action.addReasoning("Cancel battle damage - valuable survival card", GOOD_DELTA);

        // TODO: Add proper damage analysis when we have access to battle state
        // Check attrition/damage remaining and cards available to forfeit
    }

    private void evaluateTakeIntoHand(EvaluatedAction action, DecisionContext context, String actionText, String textLower) {
        if (textLower.contains("palpatine")) {
            action.addReasoning("Avoid taking Palpatine", BAD_DELTA);
            return;
        }

        // V29.7: Detect RETURN-TO-HAND (bouncing own card from table) vs RETRIEVE (from deck).
        // Retrieval actions always specify the source: "from Reserve Deck", "from Force Pile", etc.
        // If no source pile is mentioned, the card is being RETURNED from table — that's BAD!
        // Example: Corporal Vandolay's "Take an ISB agent into hand" = bounce deployed character.
        // EXCEPTION: "destiny" / "re-draw" actions are battle destiny management, NOT bounces.
        boolean isFromDeck = textLower.contains("from reserve") || textLower.contains("from force pile")
            || textLower.contains("from used pile") || textLower.contains("from lost pile");
        boolean isDestinyAction = textLower.contains("destiny") || textLower.contains("re-draw")
            || textLower.contains("redraw");

        if (!isFromDeck && !isDestinyAction) {
            // This is a bounce/return from table — VERY bad! We just paid to deploy that character.
            action.addReasoning("V29.7 BOUNCE: Return own card from table to hand — DON'T undo your deploy!", -300.0f);
            logger.warn("V29.7 BOUNCE BLOCKED: '{}' would return deployed card to hand (-300)", actionText);
        } else if (isFromDeck && textLower.contains("from reserve")) {
            // V29.7: PULL FIRST RULE — retrievals from Reserve Deck are FREE actions
            // from effects like Endor Shield, Mobilization Points, etc.
            // These should ALWAYS fire before locations (+200) and characters.
            // Getting cards into hand first = better deploy decisions.
            action.addReasoning("V29.7 PULL FIRST: Get cards into hand before deploying!", 250.0f);
        } else {
            // From force pile, used pile, or destiny management — normal priority
            action.addReasoning("Take card into hand", GOOD_DELTA);
        }
    }

    /**
     * Evaluate barrier card (Imperial/Rebel Barrier) usage.
     * Ported from Python action_text_evaluator.py lines 973-1055
     *
     * Use barriers when:
     *   - Location IS contested (both players present)
     *   - Target is a significant threat (high power)
     *   - We're not already winning overwhelmingly
     * Save barriers when:
     *   - Location not contested (no point)
     *   - We're already dominating the location
     *   - Target already has a barrier on it this turn!
     */
    private void evaluateBarrier(EvaluatedAction action, DecisionContext context, String actionText) {
        String targetCardName = extractCardNameFromPreventText(actionText);
        int currentTurn = context.getTurnNumber();

        // Reset barrier tracking on new turn
        if (currentTurn != barrierTurn) {
            barrieredTargets.clear();
            barrierTurn = currentTurn;
        }

        // Check if we already barriered this target
        if (targetCardName != null && barrieredTargets.contains(targetCardName.toLowerCase())) {
            action.addReasoning("Already barriered " + targetCardName + " this turn - wasteful!", VERY_BAD_DELTA);
            return;
        }

        // V35.1: NEVER barrier our OWN characters! "You Are Beaten" can target any character,
        // but preventing our OWN character from battling/moving is self-sabotage.
        // Check if the target belongs to us — if so, HARD BLOCK.
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        if (gameState != null && playerId != null && targetCardName != null) {
            String targetLower = targetCardName.toLowerCase();
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || card.getTitle() == null) continue;
                if (card.getTitle().toLowerCase().contains(targetLower) || targetLower.contains(card.getTitle().toLowerCase())) {
                    if (playerId.equals(card.getOwner())) {
                        action.addReasoning(String.format(
                            "V35.1 SELF-BARRIER BLOCK: %s is OUR character — NEVER prevent our own from battling!",
                            targetCardName), -9999.0f);
                        logger.warn("V35.1 SELF-BARRIER: Blocking barrier on OWN character {} (-9999)", targetCardName);
                        return;
                    }
                    break;
                }
            }
        }
        float targetPower = 0;
        float ourPower = 0;
        float theirPower = 0;
        boolean locationContested = false;

        if (gameState != null && playerId != null && targetCardName != null) {
            String opponentId = gameState.getOpponent(playerId);

            // Find the target card and analyze location
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;
                String title = card.getTitle();
                if (title == null) continue;

                // Match by name
                if (title.toLowerCase().contains(targetCardName.toLowerCase()) ||
                    targetCardName.toLowerCase().contains(title.toLowerCase())) {

                    // Found the target - check its power
                    SwccgCardBlueprint blueprint = card.getBlueprint();
                    if (blueprint != null && blueprint.hasPowerAttribute()) {
                        Float power = blueprint.getPower();
                        if (power != null) {
                            targetPower = power;
                        }
                    }

                    // Find location and calculate power
                    PhysicalCard location = card.getAtLocation();
                    if (location != null) {
                        boolean hasOurPresence = false;
                        boolean hasTheirPresence = false;

                        for (PhysicalCard locCard : gameState.getCardsAtLocation(location)) {
                            if (locCard == null) continue;
                            String owner = locCard.getOwner();
                            SwccgCardBlueprint bp = locCard.getBlueprint();
                            if (bp == null) continue;

                            // Check presence
                            if (playerId.equals(owner)) {
                                hasOurPresence = true;
                                if (bp.hasPowerAttribute()) {
                                    Float power = bp.getPower();
                                    if (power != null) ourPower += power;
                                }
                            } else if (opponentId != null && opponentId.equals(owner)) {
                                hasTheirPresence = true;
                                if (bp.hasPowerAttribute()) {
                                    Float power = bp.getPower();
                                    if (power != null) theirPower += power;
                                }
                            }
                        }
                        locationContested = hasOurPresence && hasTheirPresence;
                    }
                    break;
                }
            }
        }

        logger.debug("🚧 Barrier analysis: {} (power {}) contested={}, our={}, their={}",
            targetCardName, targetPower, locationContested, ourPower, theirPower);

        // Apply scoring based on situation
        if (!locationContested) {
            // Location NOT contested - save barrier for when we need it
            action.addReasoning("Save barrier - location not contested", BAD_DELTA);
        } else if (ourPower >= theirPower + 8) {
            // We're already dominating - don't waste the barrier
            action.addReasoning("Save barrier - already dominating (" + (int)ourPower + " vs " + (int)theirPower + ")", BAD_DELTA);
        } else if (targetPower >= 5) {
            // High-power target at contested location - VERY valuable!
            action.addReasoning("Barrier on HIGH POWER target (" + (int)targetPower + ")!", VERY_GOOD_DELTA);
            if (targetCardName != null) {
                barrieredTargets.add(targetCardName.toLowerCase());
            }
        } else if (theirPower >= ourPower) {
            // They're winning or tied - barrier is valuable
            action.addReasoning("Barrier to protect (losing " + (int)ourPower + " vs " + (int)theirPower + ")", GOOD_DELTA + 10.0f);
            if (targetCardName != null) {
                barrieredTargets.add(targetCardName.toLowerCase());
            }
        } else {
            // We're ahead but not dominating - still useful
            action.addReasoning("Barrier at contested location", GOOD_DELTA);
            if (targetCardName != null) {
                barrieredTargets.add(targetCardName.toLowerCase());
            }
        }
    }

    private void evaluateEmbark(EvaluatedAction action, DecisionContext context, String actionText) {
        String blueprintId = extractBlueprintFromText(actionText);

        // Embarking pilots onto ships is good, non-pilots is usually bad
        // For now, give neutral score - could be improved with pilot detection
        action.addReasoning("Embark action", 0.0f);
    }

    private void evaluateGrab(EvaluatedAction action, DecisionContext context, String actionText) {
        // Grabbing opponent's card is good, our own is VERY bad
        // CRITICAL: Grabbing own interrupts is a big player complaint - hard block it!
        // Ported from Python action_text_evaluator.py lines 1169-1210

        Side mySide = context.getSide();

        // Determine side from card name patterns in action text
        // Look for known Light/Dark side indicator patterns
        String textLower = actionText.toLowerCase();
        boolean looksLightSide = textLower.contains("rebel") || textLower.contains("jedi") ||
                                  textLower.contains("alliance") || textLower.contains("luke") ||
                                  textLower.contains("leia") || textLower.contains("han solo") ||
                                  textLower.contains("chewie") || textLower.contains("yoda") ||
                                  textLower.contains("obi-wan") || textLower.contains("padme");
        boolean looksDarkSide = textLower.contains("imperial") || textLower.contains("sith") ||
                                 textLower.contains("vader") || textLower.contains("emperor") ||
                                 textLower.contains("stormtrooper") || textLower.contains("death star") ||
                                 textLower.contains("maul") || textLower.contains("dooku") ||
                                 textLower.contains("boba fett") || textLower.contains("jango");

        if (mySide == Side.DARK && looksLightSide) {
            action.addReasoning("Grab Light side card (we are Dark)", GOOD_DELTA);
        } else if (mySide == Side.LIGHT && looksDarkSide) {
            action.addReasoning("Grab Dark side card (we are Light)", GOOD_DELTA);
        } else if (mySide == Side.DARK && looksDarkSide) {
            // Same side - likely our card! HARD BLOCK!
            action.setScore(-500.0f);
            action.addReasoning("🚫 BLOCKED: Likely grabbing own Dark card!", -500.0f);
            logger.warn("🚫 BLOCKED GRAB of likely own Dark card: {}", actionText);
        } else if (mySide == Side.LIGHT && looksLightSide) {
            // Same side - likely our card! HARD BLOCK!
            action.setScore(-500.0f);
            action.addReasoning("🚫 BLOCKED: Likely grabbing own Light card!", -500.0f);
            logger.warn("🚫 BLOCKED GRAB of likely own Light card: {}", actionText);
        } else {
            // Truly unknown - be cautious, don't grab
            action.addReasoning("Grab card (owner unknown - avoiding)", BAD_DELTA);
            logger.info("⚠️ Grab owner unknown, avoiding: {}", actionText);
        }
    }

    private void evaluateBreakCover(EvaluatedAction action, DecisionContext context, String actionText) {
        // Breaking opponent's spy is good, our own is VERY bad
        // CRITICAL: Breaking own spy cover is a big player complaint - hard block it!
        // Ported from Python action_text_evaluator.py lines 1212-1246

        Side mySide = context.getSide();

        // Determine side from card name patterns in action text
        String textLower = actionText.toLowerCase();
        boolean looksLightSide = textLower.contains("rebel") || textLower.contains("bothan") ||
                                  textLower.contains("alliance") || textLower.contains("leia") ||
                                  textLower.contains("mon mothma") || textLower.contains("orrimaarko");
        boolean looksDarkSide = textLower.contains("imperial") || textLower.contains("ism-agent") ||
                                 textLower.contains("empire") || textLower.contains("probe droid") ||
                                 textLower.contains("mara jade");

        if (mySide == Side.DARK && looksLightSide) {
            action.addReasoning("Break Light side spy cover (we are Dark)", GOOD_DELTA);
        } else if (mySide == Side.LIGHT && looksDarkSide) {
            action.addReasoning("Break Dark side spy cover (we are Light)", GOOD_DELTA);
        } else if (mySide == Side.DARK && looksDarkSide) {
            // Same side - our spy! HARD BLOCK!
            action.setScore(-500.0f);
            action.addReasoning("🚫 BLOCKED: Likely breaking own Dark spy cover!", -500.0f);
            logger.warn("🚫 BLOCKED break cover of likely own Dark spy: {}", actionText);
        } else if (mySide == Side.LIGHT && looksLightSide) {
            // Same side - our spy! HARD BLOCK!
            action.setScore(-500.0f);
            action.addReasoning("🚫 BLOCKED: Likely breaking own Light spy cover!", -500.0f);
            logger.warn("🚫 BLOCKED break cover of likely own Light spy: {}", actionText);
        } else {
            // Unknown spy - be cautious, default to not doing it
            action.addReasoning("Break cover (spy owner unknown - cautious)", BAD_DELTA);
            logger.info("⚠️ Break cover owner unknown, avoiding: {}", actionText);
        }
    }

    // ========== Utility Methods ==========

    private String extractBlueprintFromText(String actionText) {
        if (actionText == null) return null;
        Matcher matcher = BLUEPRINT_PATTERN.matcher(actionText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCardNameFromPreventText(String actionText) {
        // Pattern: "Prevent <CARD NAME> from battling or moving"
        if (actionText != null && actionText.contains("Prevent") &&
            actionText.contains("from battling or moving")) {
            int startIdx = actionText.indexOf("Prevent") + "Prevent ".length();
            int endIdx = actionText.indexOf(" from battling or moving");
            if (startIdx > 0 && endIdx > startIdx) {
                return actionText.substring(startIdx, endIdx).trim();
            }
        }
        return null;
    }

    /**
     * Check if there are valid (non-HIT) weapon targets at the battle location.
     *
     * In SWCCG, firing at already-hit targets is wasteful since they're
     * already damaged. This method returns true only if there are unhit
     * enemy cards at the battle location.
     *
     * Ported from Python action_text_evaluator.py valid target check.
     */
    private boolean checkForValidWeaponTargets(DecisionContext context) {
        GameState gameState = context.getGameState();
        if (gameState == null) {
            return true;  // Default to allowing fire if we can't check
        }

        try {
            // Get the battle location
            PhysicalCard battleLocation = gameState.getBattleLocation();
            if (battleLocation == null) {
                return true;  // Not in battle, allow fire
            }

            // Find enemy cards at battle location
            String playerId = context.getPlayerId();
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId == null) {
                return true;  // Can't determine opponent
            }

            // Check all enemy cards at battle location
            boolean foundUnhitEnemy = false;
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;

                // Must be enemy card
                if (!opponentId.equals(card.getOwner())) continue;

                // Must be at battle location
                PhysicalCard cardLocation = card.getAtLocation();
                if (cardLocation == null || !cardLocation.equals(battleLocation)) continue;

                // Must be a valid weapon target (character, starship, vehicle)
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null) continue;
                CardCategory cat = bp.getCardCategory();
                if (cat != CardCategory.CHARACTER && cat != CardCategory.STARSHIP && cat != CardCategory.VEHICLE) {
                    continue;
                }

                // Check if this card is NOT hit
                if (!card.isHit()) {
                    foundUnhitEnemy = true;
                    logger.debug("Found unhit enemy target: {}", card.getTitle());
                    break;  // Found at least one valid target
                }
            }

            if (!foundUnhitEnemy) {
                logger.info("🎯 All enemy targets at battle location are HIT - no valid weapon targets");
            }

            return foundUnhitEnemy;

        } catch (Exception e) {
            logger.debug("Error checking weapon targets: {}", e.getMessage());
            return true;  // Default to allowing fire on error
        }
    }
}
