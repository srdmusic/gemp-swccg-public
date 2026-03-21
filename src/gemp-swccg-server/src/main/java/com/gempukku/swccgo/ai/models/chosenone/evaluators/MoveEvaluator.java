package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.chosenone.ChosenOneConfig;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Evaluates movement decisions.
 *
 * FULLY PORTED from Python move_evaluator.py with:
 * - Threat level calculation (CRUSH, FAVORABLE, RISKY, DANGEROUS, RETREAT)
 * - Flee analysis with destination checking
 * - Offensive attack opportunity detection
 * - Spread viability analysis with icon bonuses
 *
 * Decision factors:
 * - Power differential at current location (fleeing from danger)
 * - Power differential at destination (moving to advantageous positions)
 * - Spreading out vs consolidating forces
 * - Strategic retreat from dangerous locations
 * - Offensive attacks from uncontested strongholds
 */
public class MoveEvaluator extends ActionEvaluator {

    // Move keywords to identify move actions
    private static final String[] MOVE_KEYWORDS = {
        "Move using", "Shuttle", "Docking bay transit", "Transport",
        "Take off", "Land", "Move to", "Move from"
    };

    // Thresholds (from Python config)
    private static final int POWER_DIFF_FOR_FLEE = 2;
    private static final int OVERKILL_THRESHOLD = 4;
    private static final int ESTABLISH_THRESHOLD = 6;
    private static final int CONTEST_MARGIN = 4;
    private static final int ATTACK_POWER_ADVANTAGE = 4;
    private static final int ATTACK_MIN_POWER = 6;
    private static final float ICON_BONUS = 15.0f;

    // Score deltas (from Python)
    private static final float VERY_GOOD_DELTA = 150.0f;
    private static final float GOOD_DELTA = 10.0f;
    private static final float BAD_DELTA = -10.0f;
    private static final float VERY_BAD_DELTA = -150.0f;

    // Threat levels (matching Python ThreatLevel enum)
    private enum ThreatLevel {
        CRUSH, FAVORABLE, RISKY, DANGEROUS, RETREAT
    }

    // Track cards we've already tried moving this turn
    private Set<String> pendingMoveCardIds = new HashSet<>();
    private int lastTurnNumber = -1;

    public MoveEvaluator() {
        super("Move");
    }

    public void resetPendingMoves() {
        pendingMoveCardIds.clear();
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();
        if (!"CARD_ACTION_CHOICE".equals(decisionType) && !"ACTION_CHOICE".equals(decisionType)) {
            return false;
        }

        // Must be our turn
        if (context.getGameState() != null && !context.isMyTurn()) {
            return false;
        }

        // Check if any action is a move action
        List<String> actionTexts = context.getActionTexts();
        if (actionTexts != null) {
            for (String actionText : actionTexts) {
                if (isMoveAction(actionText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMoveAction(String actionText) {
        if (actionText == null) return false;
        for (String keyword : MOVE_KEYWORDS) {
            if (actionText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        Side mySide = context.getSide();

        logger.info("[MoveEvaluator] Evaluating move decision");

        // Reset pending move tracking at the start of each turn
        if (context.getTurnNumber() != lastTurnNumber) {
            resetPendingMoves();
            lastTurnNumber = context.getTurnNumber();
        }

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();

        if (actionIds == null || actionTexts == null) {
            return actions;
        }

        logger.debug("[MoveEvaluator] Phase={}, actions={}", context.getPhase(), actionIds.size());

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);
            String cardIdStr = (cardIds != null && i < cardIds.size()) ? cardIds.get(i) : null;

            // Only handle move-related actions
            if (!isMoveAction(actionText)) {
                continue;
            }

            // === SPECIAL CASES: Passenger/Pilot capacity slots ===
            if (actionLower.contains("passenger capacity slot")) {
                logger.info("[MoveEvaluator] SKIP passenger slot move - NEVER good");
                continue;  // Let ActionTextEvaluator's -100 apply
            }

            if (actionLower.contains("pilot capacity slot")) {
                EvaluatedAction pilotAction = new EvaluatedAction(
                    actionId, ActionType.MOVE, 100.0f, actionText
                );
                pilotAction.addReasoning("Move to pilot slot - adds power!", 50.0f);
                actions.add(pilotAction);
                logger.info("[MoveEvaluator] Strongly prefer pilot capacity slot move");
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.MOVE,
                0.0f,  // Start at 0 - let analysis determine score
                actionText
            );

            // === Get the card being moved ===
            PhysicalCard cardToMove = null;
            if (cardIdStr != null && game != null) {
                try {
                    int cardId = Integer.parseInt(cardIdStr);
                    cardToMove = gameState.findCardById(cardId);
                } catch (Exception e) {
                    logger.debug("[MoveEvaluator] Could not find card: {}", e.getMessage());
                }
            }

            // === V25: NEVER MOVE A PILOT OFF THEIR SHIP ===
            // Pilots aboard ships (especially capital ships like Executor) should NEVER shuttle off.
            // Removing the pilot unpilots the ship, losing system control and making it vulnerable.
            // This was catastrophic in testing: Piett shuttled off Executor, got killed alone at CC,
            // and Rando lost 16 Force including the entire TDIGWATT engine from hand.
            if (cardToMove != null && cardToMove.isPilotOf()) {
                PhysicalCard ship = cardToMove.getAttachedTo();
                String shipName = (ship != null && ship.getTitle() != null) ? ship.getTitle() : "unknown ship";
                String pilotName = (cardToMove.getTitle() != null) ? cardToMove.getTitle() : "pilot";
                action.addReasoning("V25 PILOT LOCK: " + pilotName + " is piloting " + shipName
                    + " — NEVER leave the ship!", -500.0f);
                logger.warn("V25 PILOT LOCK: {} is piloting {} — blocking move (-500)", pilotName, shipName);
            }

            // === STRATEGIC ANALYSIS ===
            if (cardToMove != null && gameState != null && game != null) {
                PhysicalCard currentLocation = cardToMove.getAtLocation();

                if (currentLocation != null) {
                    // Analyze if we should move FROM this location
                    rankMoveFromLocation(action, gameState, game, playerId, mySide,
                                        cardToMove, currentLocation);

                    // V22.5: PRE-FLIP CONSOLIDATION — don't leave characters alone to die!
                    // Even before flipping, if a lone character is badly outgunned at a location,
                    // they should move to join allies instead of staying to get slaughtered.
                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer moveObjAnalyzer =
                        context.getObjectiveAnalyzer();
                    if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed() && !moveObjAnalyzer.isFlipped()) {
                        String preFlipLocTitle = currentLocation.getTitle();
                        String preFlipOpponent = game.getOpponent(playerId);

                        // Count our characters at this location
                        int preFlipOurChars = 0;
                        float preFlipOurPower = 0;
                        for (PhysicalCard card : gameState.getCardsAtLocation(currentLocation)) {
                            if (card != null && playerId.equals(card.getOwner())
                                && card.getBlueprint() != null && card.getBlueprint().hasPowerAttribute()) {
                                preFlipOurChars++;
                                Float p = card.getBlueprint().getPower();
                                preFlipOurPower += (p != null ? p : 0);
                            }
                        }

                        // Get opponent power here
                        float preFlipTheirPower = 0;
                        try {
                            preFlipTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, currentLocation, preFlipOpponent, false, false);
                        } catch (Exception e) {
                            // Ignore
                        }

                        // V22.5: Lone character badly outgunned — should move to join allies
                        if (preFlipOurChars == 1 && preFlipTheirPower > preFlipOurPower * 2 && preFlipTheirPower > 6) {
                            // Find a friendly location with allies to join
                            String bestAllyLoc = null;
                            float bestAllyPower = 0;
                            try {
                                for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                    if (loc == null || loc == currentLocation) continue;
                                    float allyPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, playerId, false, false);
                                    if (allyPower > bestAllyPower) {
                                        bestAllyPower = allyPower;
                                        bestAllyLoc = loc.getTitle();
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore
                            }

                            float consolidateBonus = 100.0f;
                            if (preFlipTheirPower > preFlipOurPower * 3) consolidateBonus = 160.0f;
                            action.addReasoning("V22.5 PRE-FLIP: LONE & OUTGUNNED (" + (int)preFlipOurPower +
                                " vs " + (int)preFlipTheirPower + ") - move to join allies" +
                                (bestAllyLoc != null ? " at " + bestAllyLoc : ""), consolidateBonus);
                            logger.warn("V22.5 CONSOLIDATE PRE-FLIP: {} alone at {} ({}v{}) should join allies{}",
                                cardToMove.getTitle(), preFlipLocTitle,
                                (int)preFlipOurPower, (int)preFlipTheirPower,
                                bestAllyLoc != null ? " at " + bestAllyLoc : "");
                        } else if (preFlipOurChars <= 2 && preFlipTheirPower > preFlipOurPower * 1.5f && preFlipTheirPower > 8) {
                            // Small group outgunned — moderate consolidation pressure
                            action.addReasoning("V22.5 PRE-FLIP: Outgunned at " + preFlipLocTitle +
                                " (" + (int)preFlipOurPower + " vs " + (int)preFlipTheirPower + ")", 60.0f);
                        }
                    }

                    // V22.2: POST-FLIP OBJECTIVE PROTECTION
                    // After objective flips, protect flip-back locations at all costs.
                    // Scale required power based on opponent's threat level.
                    if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed() && moveObjAnalyzer.isFlipped()) {
                        String curLocTitle = currentLocation.getTitle();
                        boolean atProtectionLoc = moveObjAnalyzer.isFlipBackProtectionLocation(curLocTitle);
                        String opponent = game.getOpponent(playerId);

                        // Count our characters and power at current location
                        int ourCharsHere = 0;
                        float ourPowerHere = 0;
                        for (PhysicalCard card : gameState.getCardsAtLocation(currentLocation)) {
                            if (card != null && playerId.equals(card.getOwner())
                                && card.getBlueprint() != null && card.getBlueprint().hasPowerAttribute()) {
                                ourCharsHere++;
                                Float p = card.getBlueprint().getPower();
                                ourPowerHere += (p != null ? p : 0);
                            }
                        }

                        // Check opponent total power on table (measure of threat)
                        float opponentTotalPower = 0;
                        try {
                            for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null) {
                                    opponentTotalPower += game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, opponent, false, false);
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Could not sum opponent power: {}", e.getMessage());
                        }

                        // Find the most vulnerable protection location (lowest our power vs their power)
                        float worstDeficit = 0;
                        String weakestLoc = null;
                        try {
                            for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (!moveObjAnalyzer.isFlipBackProtectionLocation(loc.getTitle())) continue;
                                float ourPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, playerId, false, false);
                                float theirPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, opponent, false, false);
                                float deficit = (theirPwr + 4.0f) - ourPwr;
                                if (deficit > worstDeficit) {
                                    worstDeficit = deficit;
                                    weakestLoc = loc.getTitle();
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Could not analyze protection locations: {}", e.getMessage());
                        }

                        if (atProtectionLoc) {
                            // AT a protection location — DO NOT LEAVE unless massively overkill
                            if (ourCharsHere >= 3 && ourPowerHere > 12) {
                                // Strong presence, can afford to move one character
                                action.addReasoning("V22.2 POST-FLIP: Strong at protection loc - can move", -30.0f);
                            } else {
                                // Must stay and defend! Penalty scales with opponent threat
                                float stayPenalty = -80.0f;
                                if (opponentTotalPower > 15) stayPenalty = -120.0f;
                                if (opponentTotalPower > 25) stayPenalty = -160.0f;
                                action.addReasoning("V22.2 POST-FLIP: STAY at protection location! Opponent power=" +
                                    (int)opponentTotalPower, stayPenalty);
                                logger.warn("V22.2 PROTECT: {} must stay at {} (our power={}, opponent total={})",
                                    cardToMove.getTitle(), curLocTitle, (int)ourPowerHere, (int)opponentTotalPower);
                            }
                        } else {
                            // NOT at a protection location — encourage moving to one that needs help
                            if (ourCharsHere == 1) {
                                float moveBonus = 80.0f;
                                if (worstDeficit > 4) moveBonus = 120.0f;
                                if (worstDeficit > 8) moveBonus = 160.0f;
                                action.addReasoning("V22.2 POST-FLIP: Lone char should reinforce " +
                                    (weakestLoc != null ? weakestLoc : "protection locs"), moveBonus);
                                logger.warn("V22.2 CONSOLIDATE: {} alone at {} - move to reinforce (worst deficit={})",
                                    cardToMove.getTitle(), curLocTitle, (int)worstDeficit);
                            } else if (worstDeficit > 6) {
                                // Even non-lone characters should move if protection locs are severely underguarded
                                action.addReasoning("V22.2 POST-FLIP: Protection locations severely under-guarded!", 60.0f);
                                logger.warn("V22.2 CONSOLIDATE: {} at {} but {} needs help (deficit={})",
                                    cardToMove.getTitle(), curLocTitle, weakestLoc, (int)worstDeficit);
                            }
                        }
                    }

                    // === V32: ABILITY >= 4 MOVE PROTECTION ===
                    // SWCCG requires total ability >= 4 at a site to draw battle destiny.
                    // NEVER move a character away if it leaves remaining ability < 4.
                    {
                        boolean isSite = currentLocation.getBlueprint() != null
                            && currentLocation.getBlueprint().getCardSubtype() != null
                            && currentLocation.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE;

                        if (isSite) {
                            float totalAbilityHere = 0;
                            float moverAbility = 0;
                            int friendlyCharsHere = 0;

                            if (cardToMove.getBlueprint() != null && cardToMove.getBlueprint().hasAbilityAttribute()) {
                                Float ma = cardToMove.getBlueprint().getAbility();
                                moverAbility = ma != null ? ma : 0;
                            }

                            for (PhysicalCard c : gameState.getCardsAtLocation(currentLocation)) {
                                if (c == null || !playerId.equals(c.getOwner())) continue;
                                if (c.getBlueprint() == null) continue;
                                if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                friendlyCharsHere++;
                                if (c.getBlueprint().hasAbilityAttribute()) {
                                    Float cAb = c.getBlueprint().getAbility();
                                    totalAbilityHere += (cAb != null ? cAb : 0);
                                }
                            }

                            float abilityAfterMove = totalAbilityHere - moverAbility;

                            if (friendlyCharsHere > 1 && abilityAfterMove > 0 && abilityAfterMove < 4.0f) {
                                float abilityPenalty = -300.0f;
                                String v32Opponent = game.getOpponent(playerId);
                                float theirPower = 0;
                                try {
                                    theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, v32Opponent, false, false);
                                } catch (Exception e) { /* ignore */ }

                                if (theirPower > 0) {
                                    abilityPenalty = -500.0f;
                                }

                                action.addReasoning(String.format(
                                    "V32 ABILITY DANGER: Moving %s drops ability from %.0f to %.0f (< 4) at %s!%s",
                                    cardToMove.getTitle(), totalAbilityHere, abilityAfterMove,
                                    currentLocation.getTitle(),
                                    theirPower > 0 ? " ENEMY POWER=" + (int)theirPower : ""),
                                    abilityPenalty);
                                logger.warn("V32 ABILITY MOVE BLOCK: {} from {} leaves ability {} < 4!",
                                    cardToMove.getTitle(), currentLocation.getTitle(), abilityAfterMove);
                            } else if (friendlyCharsHere == 1 && totalAbilityHere < 4.0f) {
                                action.addReasoning(String.format(
                                    "V32 ABILITY SOLO ESCAPE: %s alone with ability %.0f < 4 — move to join allies!",
                                    cardToMove.getTitle(), totalAbilityHere), 50.0f);
                            }
                        }
                    }

                    // === V33: ABILITY 7 BUDDY MOVE PROTECTION ===
                    // Don't move a character away from a site if it drops friendly ability
                    // below the buddy threshold (7).
                    {
                        boolean v33IsSite = currentLocation.getBlueprint() != null
                            && currentLocation.getBlueprint().getCardSubtype() != null
                            && currentLocation.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SITE;

                        if (v33IsSite) {
                            float v33TotalAbility = 0;
                            float v33MoverAbility = 0;
                            int v33FriendlyChars = 0;

                            if (cardToMove.getBlueprint() != null && cardToMove.getBlueprint().hasAbilityAttribute()) {
                                Float v33Ma = cardToMove.getBlueprint().getAbility();
                                v33MoverAbility = v33Ma != null ? v33Ma : 0;
                            }

                            for (PhysicalCard c : gameState.getCardsAtLocation(currentLocation)) {
                                if (c == null || !playerId.equals(c.getOwner())) continue;
                                if (c.getBlueprint() == null) continue;
                                if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                v33FriendlyChars++;
                                if (c.getBlueprint().hasAbilityAttribute()) {
                                    Float cAb = c.getBlueprint().getAbility();
                                    v33TotalAbility += (cAb != null ? cAb : 0);
                                }
                            }

                            float v33AbilityAfterMove = v33TotalAbility - v33MoverAbility;

                            if (v33FriendlyChars > 1 && v33TotalAbility >= ChosenOneConfig.ABILITY_BUDDY_THRESHOLD
                                && v33AbilityAfterMove < ChosenOneConfig.ABILITY_BUDDY_THRESHOLD && v33AbilityAfterMove >= 4.0f) {
                                action.addReasoning(String.format(
                                    "V33 BUDDY BREAK: Moving %s drops ability from %.0f to %.0f (< %d) at %s",
                                    cardToMove.getTitle(), v33TotalAbility, v33AbilityAfterMove,
                                    ChosenOneConfig.ABILITY_BUDDY_THRESHOLD, currentLocation.getTitle()), -150.0f);
                                logger.warn("V33 BUDDY BREAK: {} from {} would drop ability {} → {} (< {})",
                                    cardToMove.getTitle(), currentLocation.getTitle(),
                                    v33TotalAbility, v33AbilityAfterMove, ChosenOneConfig.ABILITY_BUDDY_THRESHOLD);
                            }
                        }
                    }

                    // === V31: POST-FLIP MOVE CONSOLIDATION ===
                    // After objective flips, if we occupy 3+ obj locations but only need 2,
                    // move characters from weakest location to reinforce stronger ones.
                    {
                        if (moveObjAnalyzer != null && moveObjAnalyzer.isAnalyzed()
                            && moveObjAnalyzer.isFlipped()) {
                            try {
                                java.util.Set<String> objFrags = moveObjAnalyzer.getFlipConditionLocationFragments();
                                String curLocTitle = currentLocation.getTitle();
                                boolean atObjLoc = false;
                                if (curLocTitle != null) {
                                    for (String frag : objFrags) {
                                        if (curLocTitle.toLowerCase(Locale.ROOT).contains(frag.toLowerCase(Locale.ROOT))) {
                                            atObjLoc = true;
                                            break;
                                        }
                                    }
                                }

                                java.util.Map<String, Float> objPowerMap = new java.util.LinkedHashMap<>();
                                for (PhysicalCard loc : gameState.getTopLocations()) {
                                    if (loc == null || loc.getTitle() == null) continue;
                                    String lt = loc.getTitle().toLowerCase(Locale.ROOT);
                                    boolean isObj = false;
                                    for (String frag : objFrags) {
                                        if (lt.contains(frag.toLowerCase(Locale.ROOT))) { isObj = true; break; }
                                    }
                                    if (!isObj) continue;
                                    float pwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, loc, playerId, false, false);
                                    if (pwr > 0) objPowerMap.put(loc.getTitle(), pwr);
                                }

                                if (objPowerMap.size() >= 3 && atObjLoc) {
                                    String weakestObjLoc = null;
                                    float weakestPwr = Float.MAX_VALUE;
                                    for (java.util.Map.Entry<String, Float> entry : objPowerMap.entrySet()) {
                                        if (entry.getValue() < weakestPwr) {
                                            weakestPwr = entry.getValue();
                                            weakestObjLoc = entry.getKey();
                                        }
                                    }

                                    if (weakestObjLoc != null && curLocTitle.equals(weakestObjLoc)) {
                                        action.addReasoning(String.format(
                                            "V31 POST-FLIP CONSOLIDATE: At weakest obj loc %s — move to reinforce!",
                                            weakestObjLoc), 200.0f);
                                        logger.warn("V31 CONSOLIDATE: {} should leave {} to reinforce stronger position",
                                            cardToMove.getTitle(), weakestObjLoc);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V31 MOVE CONSOLIDATE: Error: {}", e.getMessage());
                            }
                        }
                    }
                } else {
                    action.addReasoning("Card not at a location", BAD_DELTA);
                }
            }

            // === MOVEMENT TYPE BONUSES ===
            // V25: Shuttle bonus only when defending — opponent has 2x our power at destination
            if (actionLower.contains("shuttle") || actionLower.contains("transport")) {
                boolean defensiveShuttle = false;
                if (gameState != null) {
                    String opponentId = gameState.getOpponent(playerId);
                    for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                        String locTitle = loc.getTitle();
                        if (locTitle != null && actionLower.contains(locTitle.toLowerCase(Locale.ROOT))) {
                            // Found a location mentioned in action text — check power
                            float ourPower = 0, theirPower = 0;
                            for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                if (c == null) continue;
                                SwccgCardBlueprint bp = c.getBlueprint();
                                if (bp == null || !bp.hasPowerAttribute()) continue;
                                Float pw = bp.getPower();
                                if (pw == null) pw = 0f;
                                if (playerId.equals(c.getOwner())) ourPower += pw;
                                else if (opponentId != null && opponentId.equals(c.getOwner())) theirPower += pw;
                            }
                            if (ourPower > 0 && theirPower >= ourPower * 2) {
                                defensiveShuttle = true;
                                action.addReasoning("V25 Defensive shuttle — opponent has " + (int)theirPower
                                    + " vs our " + (int)ourPower + " at " + locTitle, 20.0f);
                                logger.info("[MoveEvaluator] V25 Defensive shuttle to {} (them={}, us={})",
                                    locTitle, (int)theirPower, (int)ourPower);
                            }
                            break;
                        }
                    }
                }
                if (!defensiveShuttle) {
                    // No bonus for non-defensive shuttles — let strategic analysis decide
                    logger.debug("[MoveEvaluator] V25 Shuttle without defensive need — no bonus");
                }
            }
            if (actionLower.contains("docking bay")) {
                action.addReasoning("Docking bay transit", 15.0f);
            }
            if (actionLower.contains("take off")) {
                action.addReasoning("Take off (space deployment)", 10.0f);
            }

            // Land - penalize starfighters
            if (actionLower.contains("land")) {
                handleLandAction(action, actionLower, cardToMove);
            }

            // Move phase - no automatic bonus, moves should be strategic
            // The old +5 bonus caused wasteful moves
            if (context.getPhase() == Phase.MOVE) {
                // Only add reasoning without bonus - moves need strategic justification
                action.addReasoning("Move phase", 0.0f);
            }

            logger.debug("[MoveEvaluator] Scored '{}' -> {}",
                actionText.length() > 40 ? actionText.substring(0, 40) + "..." : actionText,
                String.format("%.1f", action.getScore()));

            actions.add(action);
        }

        logger.info("[MoveEvaluator] Evaluated {} move actions", actions.size());
        return actions;
    }

    /**
     * Rank moving FROM a specific location.
     * Ported from Python move_evaluator.py _rank_move_from_location
     */
    private void rankMoveFromLocation(EvaluatedAction action, GameState gameState,
                                       SwccgGame game, String playerId, Side mySide,
                                       PhysicalCard cardToMove, PhysicalCard location) {
        String opponentId = gameState.getOpponent(playerId);

        // Calculate power at current location
        float myPower = 0;
        float theirPower = 0;
        int myCardCount = 0;
        int theirCardCount = 0;

        List<PhysicalCard> cardsAtLocation = gameState.getCardsAtLocation(location);
        for (PhysicalCard card : cardsAtLocation) {
            if (card == null) continue;
            String owner = card.getOwner();
            SwccgCardBlueprint bp = card.getBlueprint();
            if (bp == null || !bp.hasPowerAttribute()) continue;

            Float power = bp.getPower();
            if (power == null) power = 0f;

            if (playerId.equals(owner)) {
                myPower += power;
                myCardCount++;
            } else if (opponentId != null && opponentId.equals(owner)) {
                theirPower += power;
                theirCardCount++;
            }
        }

        float powerDiff = myPower - theirPower;
        boolean theirHasCards = theirCardCount > 0;

        logger.debug("[MoveEvaluator] At {}: myPower={}, theirPower={}, diff={}",
            location.getTitle(), myPower, theirPower, powerDiff);

        // === THREAT LEVEL ANALYSIS ===
        if (theirPower > 0) {
            ThreatLevel threat = calculateThreatLevel(powerDiff);

            switch (threat) {
                case RETREAT:
                    action.addReasoning("Strategic retreat - badly outmatched (" + (int)powerDiff + ")",
                                       VERY_GOOD_DELTA);
                    logger.info("[MoveEvaluator] RETREAT recommended - outmatched by {}", -powerDiff);
                    return;

                case DANGEROUS:
                    action.addReasoning("Dangerous location - retreat recommended (" + (int)powerDiff + ")",
                                       GOOD_DELTA * 2);
                    return;

                case CRUSH:
                case FAVORABLE:
                    action.addReasoning("Power advantage (" + (int)powerDiff + ") - stay and fight!",
                                       BAD_DELTA * 2);
                    return;

                case RISKY:
                    // Contested - could go either way
                    action.addReasoning("Contested location - risky to leave", BAD_DELTA);
                    break;
            }
        }

        // === FLEE LOGIC ===
        if (theirPower - myPower > POWER_DIFF_FOR_FLEE && theirPower > 0) {
            float disadvantage = theirPower - myPower;
            action.addReasoning("Outmatched by " + (int)disadvantage + " - should flee",
                               GOOD_DELTA * Math.min(disadvantage / 2, 5));
            return;
        }

        // === OFFENSIVE ATTACK OPPORTUNITY ===
        // If we're at an uncontested location with significant power, look for attack targets
        // NOTE: We can't verify reachability here, so be conservative - only recommend if:
        // 1. We have overwhelming force AND
        // 2. There are high-value targets (opponent icons for force drain)
        if (!theirHasCards && myPower >= ATTACK_MIN_POWER && myCardCount >= 2) {
            AttackAnalysis attack = analyzeAttackOpportunity(gameState, game, playerId,
                                                             mySide, location, myPower, myCardCount);
            // Only recommend attack if there's force drain potential (icons > 0)
            // and we have a significant power advantage
            if (attack != null && attack.viable && attack.hasForcedrainPotential) {
                action.addReasoning(attack.reason, attack.score);
                logger.info("[MoveEvaluator] ⚔️ ATTACK opportunity: {}", attack.reason);
                return;
            } else if (attack != null && attack.viable) {
                // Attack possible but no force drain - much smaller bonus
                // Don't waste moves just to attack weak positions
                action.addReasoning("Possible attack (no drain icons)", 15.0f);
                logger.debug("[MoveEvaluator] Weak attack opportunity (no icons): {}", attack.reason);
                return;
            }
        }

        // === SPREAD VIABILITY ===
        // Check if we have excess power we can redistribute
        float powerNeededToStay = Math.max(theirPower + OVERKILL_THRESHOLD, ESTABLISH_THRESHOLD);
        float excessPower = myPower - powerNeededToStay;

        if (excessPower >= 2 && myCardCount >= 2) {
            SpreadAnalysis spread = analyzeSpreadViability(gameState, game, playerId, mySide,
                                                           location, myPower, myCardCount, theirPower);
            if (spread != null && spread.viable) {
                action.addReasoning(spread.reason, spread.score);
                return;
            } else if (spread != null) {
                action.addReasoning("Can't spread: " + spread.reason, BAD_DELTA);
                return;
            }
        }

        // === V34: DESTINATION-AWARE CONTEST BONUS ===
        // Check if the specific destination of this move has opponents.
        // Moving TOWARD opponents = good (can battle next turn, block their drains).
        // Moving to empty location while opponents drain uncontested elsewhere = bad.
        {
            String v34ActionText = action.getDisplayText() != null
                ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
            PhysicalCard v34Dest = null;

            for (PhysicalCard locCard : gameState.getLocationsInOrder()) {
                if (locCard == null || locCard == location) continue;
                String locName = locCard.getTitle() != null
                    ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";
                if (!locName.isEmpty() && v34ActionText.contains(locName)) {
                    v34Dest = locCard;
                    break;
                }
            }

            if (v34Dest != null) {
                float destOppPower = 0;
                try {
                    destOppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                        gameState, v34Dest, opponentId, false, false);
                } catch (Exception e) { /* ignore */ }

                if (destOppPower > 0) {
                    // Moving TO a location with opponents — CONTEST their drain!
                    float contestBonus = 250.0f;
                    // Extra bonus if we're armed
                    if (cardToMove != null) {
                        try {
                            java.util.List<PhysicalCard> v34Att = gameState.getAttachedCards(cardToMove);
                            if (v34Att != null) {
                                for (PhysicalCard att : v34Att) {
                                    if (att != null && att.getBlueprint() != null
                                        && att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                        contestBonus += 100.0f;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    action.addReasoning(String.format(
                        "V34 CONTEST: Moving to %s where opponents have power %.0f — block their drain and fight!",
                        v34Dest.getTitle(), destOppPower), contestBonus);
                    logger.warn("V34 CONTEST: {} moving to {} (opponent power {}) — bonus +{}",
                        cardToMove != null ? cardToMove.getTitle() : "?",
                        v34Dest.getTitle(), (int)destOppPower, (int)contestBonus);
                } else {
                    // Moving to empty location — check if opponents are draining uncontested elsewhere
                    boolean opponentsUncontested = false;
                    String opUncontestedLoc = null;
                    float opUncontestedPower = 0;
                    try {
                        for (PhysicalCard otherLoc : gameState.getLocationsInOrder()) {
                            if (otherLoc == null || otherLoc == location || otherLoc == v34Dest) continue;
                            float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, otherLoc, opponentId, false, false);
                            float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, otherLoc, playerId, false, false);
                            if (oppPower > 0 && ourPowerThere == 0) {
                                opponentsUncontested = true;
                                if (oppPower > opUncontestedPower) {
                                    opUncontestedPower = oppPower;
                                    opUncontestedLoc = otherLoc.getTitle();
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (opponentsUncontested) {
                        action.addReasoning(String.format(
                            "V34 WRONG DIRECTION: Moving to empty %s while opponents drain uncontested at %s (power %.0f)!",
                            v34Dest.getTitle(), opUncontestedLoc, opUncontestedPower), -150.0f);
                        logger.warn("V34 WRONG DIRECTION: {} to empty {} while opponents at {} (power {})",
                            cardToMove != null ? cardToMove.getTitle() : "?",
                            v34Dest.getTitle(), opUncontestedLoc, (int)opUncontestedPower);
                    }
                }
            }
        }

        // Default: not a good time to move - strong penalty to avoid wasteful moves
        // Moves cost force and can leave positions vulnerable
        action.addReasoning("No strategic reason to move", -50.0f);
    }

    /**
     * Calculate threat level based on power differential.
     * Ported from Python move_evaluator.py threat level logic.
     */
    private ThreatLevel calculateThreatLevel(float powerDiff) {
        int favorable = ChosenOneConfig.BATTLE_FAVORABLE_THRESHOLD;
        int danger = ChosenOneConfig.BATTLE_DANGER_THRESHOLD;

        if (powerDiff >= favorable + 4) {
            return ThreatLevel.CRUSH;
        } else if (powerDiff >= favorable) {
            return ThreatLevel.FAVORABLE;
        } else if (powerDiff >= -favorable) {
            return ThreatLevel.RISKY;
        } else if (powerDiff >= danger) {
            return ThreatLevel.DANGEROUS;
        } else {
            return ThreatLevel.RETREAT;
        }
    }

    /**
     * Analyze attack opportunities at adjacent locations.
     * Ported from Python move_evaluator.py _analyze_attack_opportunity
     */
    private AttackAnalysis analyzeAttackOpportunity(GameState gameState, SwccgGame game,
                                                     String playerId, Side mySide,
                                                     PhysicalCard currentLocation,
                                                     float ourPowerHere, int ourCardCount) {
        String opponentId = gameState.getOpponent(playerId);
        float avgPowerPerCard = ourPowerHere / Math.max(ourCardCount, 1);

        // Get all locations
        List<PhysicalCard> allLocations = gameState.getLocationsInOrder();
        AttackAnalysis bestAttack = null;
        float bestScore = 0;

        for (PhysicalCard adjLocation : allLocations) {
            if (adjLocation == currentLocation) continue;

            // Calculate enemy power at this location
            float theirPower = 0;
            int theirCount = 0;
            float ourPowerThere = 0;

            List<PhysicalCard> cardsAtAdj = gameState.getCardsAtLocation(adjLocation);
            for (PhysicalCard card : cardsAtAdj) {
                if (card == null) continue;
                String owner = card.getOwner();
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null || !bp.hasPowerAttribute()) continue;

                Float power = bp.getPower();
                if (power == null) power = 0f;

                if (opponentId != null && opponentId.equals(owner)) {
                    theirPower += power;
                    theirCount++;
                } else if (playerId.equals(owner)) {
                    ourPowerThere += power;
                }
            }

            // Skip empty locations (use spread logic for those)
            if (theirCount == 0 || theirPower == 0) continue;

            // Get opponent icons at target
            int theirIcons = getOpponentIcons(adjLocation.getBlueprint(), mySide);

            // Calculate attack viability
            float potentialPower = ourPowerThere + ourPowerHere;  // If we move everyone
            float advantage = potentialPower - theirPower;

            if (advantage >= ATTACK_POWER_ADVANTAGE) {
                float score = 50.0f;  // Base attack score

                // Bonus for crushing attacks
                if (potentialPower >= theirPower * 2) {
                    score += 25.0f;
                }

                // Bonus for opponent icons
                score += theirIcons * ICON_BONUS;

                // Bonus for bigger enemy forces
                score += theirPower / 2;

                String reason = String.format("ATTACK %d enemies with %d power (+%d advantage)",
                    (int)theirPower, (int)potentialPower, (int)advantage);
                if (theirIcons > 0) {
                    reason += " - deny " + theirIcons + " icon drain!";
                }

                boolean hasForcedrainPotential = theirIcons > 0;
                if (score > bestScore) {
                    bestScore = score;
                    bestAttack = new AttackAnalysis(true, reason, score, hasForcedrainPotential);
                }
            }
        }

        return bestAttack;
    }

    /**
     * Analyze if spreading out from this location is viable.
     * Ported from Python move_evaluator.py _analyze_spread_viability
     */
    private SpreadAnalysis analyzeSpreadViability(GameState gameState, SwccgGame game,
                                                   String playerId, Side mySide,
                                                   PhysicalCard currentLocation,
                                                   float ourPowerHere, int ourCardCount,
                                                   float theirPowerHere) {
        String opponentId = gameState.getOpponent(playerId);
        int forceAvailable = 0;  // TODO: Get from context if available

        // Calculate power we need to retain at source
        float powerToRetain = Math.max(theirPowerHere + CONTEST_MARGIN, ESTABLISH_THRESHOLD);
        float avgPowerPerCard = ourPowerHere / Math.max(ourCardCount, 1);
        float powerWeCanSpare = ourPowerHere - powerToRetain;

        if (powerWeCanSpare < 2) {
            return new SpreadAnalysis(false,
                String.format("need %d power to retain control, only have %d",
                    (int)powerToRetain, (int)ourPowerHere), 0);
        }

        // Get all locations and find spread opportunities
        List<PhysicalCard> allLocations = gameState.getLocationsInOrder();
        SpreadAnalysis bestOpportunity = null;
        float bestScore = 0;

        for (PhysicalCard adjLocation : allLocations) {
            if (adjLocation == currentLocation) continue;

            // Calculate power at this location
            float theirPower = 0;
            float ourPowerThere = 0;

            List<PhysicalCard> cardsAtAdj = gameState.getCardsAtLocation(adjLocation);
            for (PhysicalCard card : cardsAtAdj) {
                if (card == null) continue;
                String owner = card.getOwner();
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null || !bp.hasPowerAttribute()) continue;

                Float power = bp.getPower();
                if (power == null) power = 0f;

                if (opponentId != null && opponentId.equals(owner)) {
                    theirPower += power;
                } else if (playerId.equals(owner)) {
                    ourPowerThere += power;
                }
            }

            // Skip if we already have good presence
            if (ourPowerThere >= ESTABLISH_THRESHOLD && theirPower == 0) {
                continue;
            }

            // Get icons at destination
            int theirIcons = getOpponentIcons(adjLocation.getBlueprint(), mySide);
            int myIcons = getMyIcons(adjLocation.getBlueprint(), mySide);

            float potentialPower = ourPowerThere + powerWeCanSpare;

            // Empty location - can we establish?
            if (theirPower == 0) {
                if (potentialPower >= ESTABLISH_THRESHOLD) {
                    float score = GOOD_DELTA * 2;
                    score += theirIcons * ICON_BONUS;  // Bonus for opponent icons

                    String reason = "Can establish at empty location";
                    if (theirIcons > 0) {
                        reason += " - " + theirIcons + " opponent icon(s) = force drain!";
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestOpportunity = new SpreadAnalysis(true, reason, score);
                    }
                }
            } else {
                // Contested - can we beat them with margin?
                float powerNeeded = theirPower + CONTEST_MARGIN;
                if (potentialPower >= powerNeeded) {
                    float score = GOOD_DELTA * 3 + theirPower / 2;
                    score += theirIcons * ICON_BONUS;

                    String reason = String.format("Can contest location with %d enemies", (int)theirPower);
                    if (theirIcons > 0) {
                        reason += " - " + theirIcons + " opponent icon(s) = force drain!";
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestOpportunity = new SpreadAnalysis(true, reason, score);
                    }
                }
            }
        }

        if (bestOpportunity != null) {
            return bestOpportunity;
        }

        return new SpreadAnalysis(false, "no good adjacent locations", 0);
    }

    /**
     * Handle Land action - penalize starfighters.
     */
    private void handleLandAction(EvaluatedAction action, String actionLower, PhysicalCard card) {
        boolean isStarfighter = false;
        String cardName = "unknown";

        if (card != null) {
            cardName = card.getTitle();
            CardSubtype subtype = card.getBlueprint().getCardSubtype();
            if (subtype == CardSubtype.STARFIGHTER) {
                isStarfighter = true;
            }
        }

        // Fallback to name-based detection
        if (!isStarfighter) {
            isStarfighter = actionLower.contains("x-wing") ||
                actionLower.contains("y-wing") ||
                actionLower.contains("a-wing") ||
                actionLower.contains("b-wing") ||
                actionLower.contains("tie") ||
                actionLower.contains("starfighter");
        }

        if (isStarfighter) {
            action.addReasoning("AVOID: Landing starfighter (" + cardName + ") wastes combat power!", -100.0f);
            logger.info("[MoveEvaluator] BLOCKED: Landing starfighter {}", cardName);
        } else {
            action.addReasoning("Land (ground deployment)", 10.0f);
        }
    }

    /**
     * Get opponent icons at a location.
     */
    private int getOpponentIcons(SwccgCardBlueprint bp, Side mySide) {
        if (bp == null) return 0;
        if (mySide == Side.LIGHT) {
            return bp.getIconCount(Icon.DARK_FORCE);
        } else {
            return bp.getIconCount(Icon.LIGHT_FORCE);
        }
    }

    /**
     * Get our icons at a location.
     */
    private int getMyIcons(SwccgCardBlueprint bp, Side mySide) {
        if (bp == null) return 0;
        if (mySide == Side.LIGHT) {
            return bp.getIconCount(Icon.LIGHT_FORCE);
        } else {
            return bp.getIconCount(Icon.DARK_FORCE);
        }
    }

    // Helper classes for analysis results
    private static class AttackAnalysis {
        boolean viable;
        String reason;
        float score;
        boolean hasForcedrainPotential;  // True if target has opponent icons

        AttackAnalysis(boolean viable, String reason, float score, boolean hasForcedrainPotential) {
            this.viable = viable;
            this.reason = reason;
            this.score = score;
            this.hasForcedrainPotential = hasForcedrainPotential;
        }
    }

    private static class SpreadAnalysis {
        boolean viable;
        String reason;
        float score;

        SpreadAnalysis(boolean viable, String reason, float score) {
            this.viable = viable;
            this.reason = reason;
            this.score = score;
        }
    }
}
