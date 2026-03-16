package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.RandoConfig;
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

            // === V29: FORCE RESERVE CHECK FOR MOVES ===
            // Moving costs Force. Save Force for:
            //   1. DTF interrupt tax (1 Force if Draw Their Fire on table)
            //   2. Grabber shield activation (1 Force if grabber in play and hasn't grabbed yet)
            //   3. Critical interrupts in hand (Ghhhk, Houjix, Out Of Nowhere)
            if (game != null && gameState != null) {
                try {
                    int forcePile = 0;
                    java.util.List<PhysicalCard> fpCards = gameState.getCardPile(playerId,
                        com.gempukku.swccgo.common.Zone.FORCE_PILE, false);
                    if (fpCards != null) forcePile = fpCards.size();

                    // Check if opponent has Draw Their Fire (makes interrupts cost +1)
                    boolean dtfActive = false;
                    String opponentId = game.getOpponent(playerId);
                    // Check for grabber shield (Allegations Of Corruption / A Tragedy Has Occurred)
                    boolean grabberNeedsForce = false;
                    for (PhysicalCard pCard : gameState.getAllPermanentCards()) {
                        if (pCard == null || pCard.getBlueprint() == null) continue;
                        com.gempukku.swccgo.common.Zone pZ = pCard.getZone();
                        if (pZ == null || !pZ.isInPlay()) continue;

                        // DTF check (opponent's card)
                        if (!dtfActive && opponentId != null && opponentId.equals(pCard.getOwner())
                            && pCard.getBlueprint().getTitle() != null
                            && pCard.getBlueprint().getTitle().toLowerCase(Locale.ROOT).contains("draw their fire")) {
                            dtfActive = true;
                        }

                        // Grabber check (our card, unused)
                        if (!grabberNeedsForce && playerId.equals(pCard.getOwner())
                            && pCard.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.GRABBER)) {
                            java.util.List<PhysicalCard> stacked = gameState.getStackedCards(pCard);
                            if (stacked == null || stacked.isEmpty()) {
                                grabberNeedsForce = true; // Hasn't grabbed yet — needs 1 Force
                            }
                        }
                    }

                    // Calculate total Force we should reserve
                    int reserveNeeded = 0;
                    if (dtfActive) reserveNeeded += 1;       // DTF interrupt tax
                    if (grabberNeedsForce) reserveNeeded += 1; // Grabber activation

                    // Check hand for critical interrupts (Ghhhk, etc.)
                    boolean hasCriticalInterrupt = false;
                    java.util.List<PhysicalCard> hand = gameState.getHand(playerId);
                    if (hand != null) {
                        for (PhysicalCard hCard : hand) {
                            if (hCard == null || hCard.getBlueprint() == null) continue;
                            String hTitle = hCard.getBlueprint().getTitle();
                            if (hTitle != null) {
                                String htl = hTitle.toLowerCase(Locale.ROOT);
                                if (htl.contains("ghhhk") || htl.contains("houjix") || htl.contains("out of nowhere")) {
                                    hasCriticalInterrupt = true;
                                    break;
                                }
                            }
                        }
                    }

                    // Apply penalties based on Force situation
                    // If we have things that need Force (DTF, grabber, interrupts), save it
                    if (reserveNeeded > 0 && forcePile <= reserveNeeded) {
                        float penalty = -100.0f;
                        if (hasCriticalInterrupt) penalty = -150.0f; // Even worse if we have Ghhhk
                        action.addReasoning(String.format(
                            "V29 FORCE RESERVE: Only %d Force, need %d (DTF=%s, grabber=%s) — save Force!",
                            forcePile, reserveNeeded, dtfActive, grabberNeedsForce), penalty);
                        logger.warn("V29 MOVE RESERVE: {} Force left, need {} (DTF={}, grabber={}, interrupt={}) — penalty {}",
                            forcePile, reserveNeeded, dtfActive, grabberNeedsForce, hasCriticalInterrupt, (int)penalty);
                    } else if (reserveNeeded > 0 && forcePile <= reserveNeeded + 1) {
                        action.addReasoning("V29 FORCE RESERVE: Low Force — move cautiously", -60.0f);
                        logger.info("V29 MOVE RESERVE: {} Force, need {} — mild penalty", forcePile, reserveNeeded);
                    }
                } catch (Exception e) {
                    logger.debug("V29 MOVE RESERVE: Error checking force: {}", e.getMessage());
                }
            }

            // === STRATEGIC ANALYSIS ===
            if (cardToMove != null && gameState != null && game != null) {
                PhysicalCard currentLocation = cardToMove.getAtLocation();

                if (currentLocation != null) {
                    // Analyze if we should move FROM this location
                    rankMoveFromLocation(action, gameState, game, playerId, mySide,
                                        cardToMove, currentLocation);

                    // === V27: BUDDY PROTECTION — NEVER leave a vulnerable ally solo ===
                    // Moving a character away from a location can leave their buddy alone
                    // and vulnerable (e.g., moving Emperor away leaves Lando solo).
                    // If removing this character would leave ANY remaining ally below
                    // buddy thresholds (power < 6 AND ability < 4) with NO other allies,
                    // apply a heavy penalty to prevent the move.
                    {
                        List<PhysicalCard> ourCharsHere = new ArrayList<>();
                        for (PhysicalCard card : gameState.getCardsAtLocation(currentLocation)) {
                            if (card != null && playerId.equals(card.getOwner())
                                && card.getBlueprint() != null
                                && card.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                ourCharsHere.add(card);
                            }
                        }
                        // Only matters if exactly 2 of our characters here (moving one leaves one solo)
                        if (ourCharsHere.size() == 2 && ourCharsHere.contains(cardToMove)) {
                            PhysicalCard remainingAlly = null;
                            for (PhysicalCard c : ourCharsHere) {
                                if (c != cardToMove) {
                                    remainingAlly = c;
                                    break;
                                }
                            }
                            if (remainingAlly != null && remainingAlly.getBlueprint() != null) {
                                SwccgCardBlueprint allyBp = remainingAlly.getBlueprint();
                                int allyPower = 0;
                                int allyAbility = 0;
                                if (allyBp.hasPowerAttribute()) {
                                    Float ap = allyBp.getPower();
                                    allyPower = ap != null ? ap.intValue() : 0;
                                }
                                if (allyBp.hasAbilityAttribute()) {
                                    Float aa = allyBp.getAbility();
                                    allyAbility = aa != null ? aa.intValue() : 0;
                                }
                                // V27: Check if opponent has ANY presence at or adjacent to this location
                                String opponentId = game.getOpponent(playerId);
                                float theirPowerHere = 0;
                                try {
                                    theirPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, currentLocation, opponentId, false, false);
                                } catch (Exception e) { /* ignore */ }

                                // V27.2: More permissive buddy protection for MOVES.
                                // During deploy phase, we use strict thresholds (power<6 AND ability<2)
                                // because deploying solo is sometimes necessary for tempo.
                                // But during MOVE phase, abandoning ANY character is bad because:
                                // 1. They're already deployed and in danger
                                // 2. The opponent can move to their location and attack
                                // 3. Solo characters draw unfavorable battles
                                // Protect if: ally power < 6 (even if ability is high like Thrawn's 4)
                                // OR if enemy is already present
                                boolean allyVulnerable = allyPower < RandoConfig.MIN_SOLO_DEPLOY_POWER;
                                boolean enemyThreat = theirPowerHere > 0;

                                if (allyVulnerable || enemyThreat) {
                                    float buddyPenalty = -150.0f;
                                    if (enemyThreat && allyPower < theirPowerHere) {
                                        // Enemy OVERPOWERS the ally — critical danger
                                        buddyPenalty = -400.0f;
                                    } else if (enemyThreat) {
                                        // Enemy present but ally can hold — still risky
                                        buddyPenalty = -250.0f;
                                    }
                                    action.addReasoning(String.format(
                                        "V27 BUDDY PROTECT: Moving away leaves %s (power %d) ALONE at %s!%s",
                                        remainingAlly.getTitle(), allyPower, currentLocation.getTitle(),
                                        enemyThreat ? " ENEMY POWER=" + (int)theirPowerHere + "!" : ""),
                                        buddyPenalty);
                                    logger.warn("V27 BUDDY PROTECT: {} moving from {} would leave {} (power {}) alone!{}",
                                        cardToMove.getTitle(), currentLocation.getTitle(),
                                        remainingAlly.getTitle(), allyPower,
                                        enemyThreat ? " ENEMY POWER=" + (int)theirPowerHere : "");
                                }
                            }
                        }
                    }

                    // === V29.12: HUNT DOWN — VADER MUST LEAVE CASTLE AND HUNT ===
                    // When playing Hunt Down V, armed Vader sitting at an uncontested location
                    // (like Vader's Castle) is WASTING turns. The whole point of Hunt Down is
                    // that Vader goes out to fight. If Vader is armed and there are no opponents
                    // at his location, give a massive bonus to move him toward the action.
                    // This overrides the natural tendency to "stay safe" at Castle.
                    {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer huntMoveAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (huntMoveAnalyzer != null && huntMoveAnalyzer.isAnalyzed() && huntMoveAnalyzer.isHuntDownV()
                            && cardToMove != null && cardToMove.getTitle() != null
                            && cardToMove.getTitle().toLowerCase(Locale.ROOT).contains("vader")) {

                            String opponentIdHunt = game.getOpponent(playerId);
                            float theirPowerHere = 0;
                            try {
                                theirPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, currentLocation, opponentIdHunt, false, false);
                            } catch (Exception e) { /* ignore */ }

                            // Check if Vader is armed
                            boolean vaderArmed = false;
                            try {
                                List<PhysicalCard> vAttach = gameState.getAttachedCards(cardToMove);
                                if (vAttach != null) {
                                    for (PhysicalCard att : vAttach) {
                                        if (att != null && att.getBlueprint() != null
                                            && att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                            vaderArmed = true;
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }

                            // If Vader is armed and no opponents here — GO HUNT!
                            if (vaderArmed && theirPowerHere == 0) {
                                // Find if any opponent characters are on the table at other locations
                                boolean opponentsElsewhere = false;
                                String bestTargetLoc = null;
                                float bestTargetPower = 0;
                                try {
                                    for (PhysicalCard loc : gameState.getTopLocations()) {
                                        if (loc == null || loc == currentLocation) continue;
                                        float opPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, loc, opponentIdHunt, false, false);
                                        if (opPower > 0) {
                                            opponentsElsewhere = true;
                                            if (opPower > bestTargetPower) {
                                                bestTargetPower = opPower;
                                                bestTargetLoc = loc.getTitle();
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if (opponentsElsewhere) {
                                    float huntMoveBonus = 200.0f;
                                    String locName = currentLocation.getTitle() != null
                                        ? currentLocation.getTitle() : "current location";
                                    action.addReasoning(String.format(
                                        "V29.12 HUNT DOWN: Armed Vader at %s with NO opponents — GO HUNT! Target: %s (power %.0f)",
                                        locName, bestTargetLoc != null ? bestTargetLoc : "?", bestTargetPower),
                                        huntMoveBonus);
                                    logger.warn("V29.12 HUNT DOWN MOVE: Armed Vader at {} (no opponents) — pushing toward {} (power {})",
                                        locName, bestTargetLoc, (int)bestTargetPower);
                                }
                            }
                        }
                    }

                    // === V29.13: HUNT DOWN — MOVE CHARACTERS WITH VADER (GROUPING) ===
                    // Mirror of V29.12 deploy grouping but for MOVE phase.
                    // Characters should move TOWARD Vader, never AWAY from Vader.
                    // Vader should move TOWARD his characters, never away from them.
                    // This prevents the "scatter" problem where Rando swaps locations
                    // (e.g., Vader moves to Cantina while brothers move FROM Cantina to Mos Eisley).
                    {
                        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer huntMoveGroupAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (huntMoveGroupAnalyzer != null && huntMoveGroupAnalyzer.isAnalyzed()
                            && huntMoveGroupAnalyzer.isHuntDownV()
                            && cardToMove != null && cardToMove.getTitle() != null
                            && gameState != null && game != null) {
                            try {
                                String movingCardTitle = cardToMove.getTitle().toLowerCase(Locale.ROOT);
                                boolean movingCardIsVader = movingCardTitle.contains("vader");
                                String moveActionLower = action.getDisplayText() != null
                                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";

                                if (movingCardIsVader) {
                                    // === VADER is moving — check if he's moving TOWARD or AWAY from his characters ===
                                    // Find locations with our characters (not Vader himself)
                                    PhysicalCard bestAllyLoc = null;
                                    float bestAllyPower = 0;
                                    int totalAllyChars = 0;
                                    for (PhysicalCard loc : gameState.getTopLocations()) {
                                        if (loc == null || loc == currentLocation) continue;
                                        float allyPowerHere = 0;
                                        int allyCountHere = 0;
                                        for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                            if (c == null || c == cardToMove) continue;
                                            if (!playerId.equals(c.getOwner())) continue;
                                            if (c.getBlueprint() == null) continue;
                                            if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            allyCountHere++;
                                            Float pw = c.getBlueprint().getPower();
                                            allyPowerHere += (pw != null ? pw : 0);
                                        }
                                        totalAllyChars += allyCountHere;
                                        if (allyPowerHere > bestAllyPower) {
                                            bestAllyPower = allyPowerHere;
                                            bestAllyLoc = loc;
                                        }
                                    }

                                    if (totalAllyChars > 0 && bestAllyLoc != null) {
                                        String bestAllyLocTitle = bestAllyLoc.getTitle() != null
                                            ? bestAllyLoc.getTitle().toLowerCase(Locale.ROOT) : "";
                                        boolean movingTowardAllies = !bestAllyLocTitle.isEmpty()
                                            && moveActionLower.contains(bestAllyLocTitle);

                                        if (movingTowardAllies) {
                                            // Vader moving TOWARD his characters — GOOD!
                                            float groupBonus = 200.0f;
                                            if (bestAllyPower >= 8) groupBonus += 50.0f;
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP MOVE: Vader moving TOWARD %d allies at %s (power %.0f) — group up!",
                                                totalAllyChars, bestAllyLoc.getTitle(), bestAllyPower), groupBonus);
                                            logger.warn("V29.13 HUNT GROUP: Vader moving to allies at {} (+{})",
                                                bestAllyLoc.getTitle(), (int)groupBonus);
                                        } else {
                                            // Vader moving AWAY from his characters — BAD!
                                            // Exception: moving toward opponents to hunt (already handled by HUNT DOWN block above)
                                            // Check if destination has opponents (hunting is OK)
                                            boolean huntingOpponents = false;
                                            String opponentIdGroup = game.getOpponent(playerId);
                                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                                if (loc == null || loc.getTitle() == null) continue;
                                                String locLower = loc.getTitle().toLowerCase(Locale.ROOT);
                                                if (!locLower.isEmpty() && moveActionLower.contains(locLower)) {
                                                    float opPower = 0;
                                                    try {
                                                        opPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                            gameState, loc, opponentIdGroup, false, false);
                                                    } catch (Exception e2) { /* ignore */ }
                                                    if (opPower > 0) {
                                                        huntingOpponents = true;
                                                    }
                                                    break;
                                                }
                                            }
                                            if (!huntingOpponents) {
                                                action.addReasoning(String.format(
                                                    "V29.13 HUNT GROUP: Vader moving AWAY from %d allies — stay together!",
                                                    totalAllyChars), -200.0f);
                                                logger.warn("V29.13 HUNT SCATTER: Vader moving away from allies at {} (-200)",
                                                    bestAllyLoc.getTitle());
                                            }
                                        }
                                    }
                                } else {
                                    // === NON-VADER character is moving — check if moving TOWARD or AWAY from Vader ===
                                    PhysicalCard vaderCard = null;
                                    PhysicalCard vaderLoc = null;
                                    for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                                        if (tableCard == null || !playerId.equals(tableCard.getOwner())) continue;
                                        com.gempukku.swccgo.common.Zone vz = tableCard.getZone();
                                        if (vz == null || !vz.isInPlay()) continue;
                                        if (tableCard.getBlueprint() == null
                                            || tableCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        String vTitle = tableCard.getTitle() != null
                                            ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (vTitle.contains("vader")) {
                                            vaderCard = tableCard;
                                            vaderLoc = tableCard.getAtLocation();
                                            break;
                                        }
                                    }

                                    if (vaderLoc != null && vaderLoc.getTitle() != null) {
                                        String vaderLocTitle = vaderLoc.getTitle().toLowerCase(Locale.ROOT);
                                        boolean currentlyWithVader = (currentLocation == vaderLoc);
                                        boolean movingToVader = !vaderLocTitle.isEmpty()
                                            && moveActionLower.contains(vaderLocTitle);

                                        if (currentlyWithVader && !movingToVader) {
                                            // Moving AWAY from Vader — BAD!
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP: %s moving AWAY from Vader at %s — stay together!",
                                                cardToMove.getTitle(), vaderLoc.getTitle()), -250.0f);
                                            logger.warn("V29.13 HUNT SCATTER: {} leaving Vader at {} (-250)",
                                                cardToMove.getTitle(), vaderLoc.getTitle());
                                        } else if (!currentlyWithVader && movingToVader) {
                                            // Moving TOWARD Vader — GREAT!
                                            float groupBonus = 250.0f;
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP MOVE: %s moving TOWARD Vader at %s — group up!",
                                                cardToMove.getTitle(), vaderLoc.getTitle()), groupBonus);
                                            logger.warn("V29.13 HUNT GROUP: {} moving to Vader at {} (+{})",
                                                cardToMove.getTitle(), vaderLoc.getTitle(), (int)groupBonus);
                                        } else if (!currentlyWithVader && !movingToVader) {
                                            // Moving but NOT toward Vader — mild penalty
                                            action.addReasoning(String.format(
                                                "V29.13 HUNT GROUP: %s moving but NOT toward Vader at %s — group up instead!",
                                                cardToMove.getTitle(), vaderLoc.getTitle()), -100.0f);
                                            logger.info("V29.13 HUNT SCATTER: {} not moving toward Vader at {} (-100)",
                                                cardToMove.getTitle(), vaderLoc.getTitle());
                                        }
                                        // If currentlyWithVader && movingToVader: shouldn't happen, no adjustment needed
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V29.13 HUNT GROUP MOVE: Error: {}", e.getMessage());
                            }
                        }
                    }

                    // V22.5: PRE-FLIP CONSOLIDATION — don't leave characters alone to die!
                    // Even before flipping, if a lone character is badly outgunned at a location,
                    // they should move to join allies instead of staying to get slaughtered.
                    com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer moveObjAnalyzer =
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

                // === V27: MAINTENANCE FORCE CONSERVATION DURING MOVES ===
                // Moving costs Force. If we have maintenance cards in play (Blizzard etc.)
                // and our Force pile is low, penalize non-critical moves to conserve Force
                // for maintenance payment at end of turn.
                if (gameState != null) {
                    try {
                        int maintenanceCost = 0;
                        java.util.List<PhysicalCard> allCards = gameState.getAllPermanentCards();
                        if (allCards != null) {
                            for (PhysicalCard mCard : allCards) {
                                if (mCard == null || !playerId.equals(mCard.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone mZone = mCard.getZone();
                                if (mZone == null || !mZone.isInPlay()) continue;
                                SwccgCardBlueprint mBp = mCard.getBlueprint();
                                if (mBp != null && mBp.hasIcon(Icon.MAINTENANCE)) {
                                    Float mCostVal = mBp.getDeployCost();
                                    maintenanceCost += (mCostVal != null) ? mCostVal.intValue() : 1;
                                }
                            }
                        }
                        if (maintenanceCost > 0) {
                            int forcePile = gameState.getForcePileSize(playerId);
                            if (forcePile <= maintenanceCost + 1) {
                                action.addReasoning(String.format(
                                    "V27 MAINTENANCE: Need %d Force for upkeep, only %d left — DON'T waste Force moving!",
                                    maintenanceCost, forcePile), -80.0f);
                                logger.warn("V27 MAINTENANCE MOVE BLOCK: {} Force in pile, need {} for maintenance — penalizing move!",
                                    forcePile, maintenanceCost);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V27: Error checking maintenance during move: {}", e.getMessage());
                    }
                }
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

        // === V29.7: WEAPON HUNTER — Armed characters should seek battle ===
        // Vader with lightsaber, or any weapon-equipped high-power character alone at
        // an uncontested location should move to engage opponents. A weapon-equipped
        // character like Vader (power 6 + lightsaber hit + throw + IHYN) is devastating
        // and worth far more than their base power suggests.
        // Bypasses the myCardCount >= 2 requirement for armed characters.
        //
        // V29.9: If character has NO weapon, penalize attack moves.
        // Vader without lightsaber should NOT be sent to fight — he needs to get armed first.
        // Deploy lightsaber on him BEFORE sending him into battle.
        if (!theirHasCards && myPower >= ATTACK_MIN_POWER && myCardCount == 1 && cardToMove != null) {
            // V29.9 PRE-CHECK: If this is Vader without a weapon, BLOCK aggressive moves
            try {
                String preCharTitle = cardToMove.getTitle() != null ? cardToMove.getTitle().toLowerCase(Locale.ROOT) : "";
                if (preCharTitle.contains("vader")) {
                    boolean vaderHasWeapon = false;
                    List<PhysicalCard> vaderAtt = gameState.getAttachedCards(cardToMove);
                    if (vaderAtt != null) {
                        for (PhysicalCard att : vaderAtt) {
                            if (att != null && att.getBlueprint() != null
                                && att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                vaderHasWeapon = true;
                                break;
                            }
                        }
                    }
                    if (!vaderHasWeapon) {
                        // Check if lightsaber is in hand — if so, equip first!
                        boolean saberInHand = false;
                        List<PhysicalCard> vHand = gameState.getHand(playerId);
                        if (vHand != null) {
                            for (PhysicalCard hCard : vHand) {
                                if (hCard != null && hCard.getTitle() != null
                                    && hCard.getTitle().toLowerCase(Locale.ROOT).contains("lightsaber")) {
                                    saberInHand = true;
                                    break;
                                }
                            }
                        }
                        if (saberInHand) {
                            action.addReasoning("V29.9 UNARMED VADER: Lightsaber in hand — EQUIP FIRST before attacking!", -250.0f);
                            logger.warn("V29.9 UNARMED VADER: Vader has no weapon but lightsaber in hand — blocking attack move (-250)");
                            return;
                        } else {
                            action.addReasoning("V29.9 UNARMED VADER: No weapon — vulnerable without lightsaber!", -100.0f);
                            logger.warn("V29.9 UNARMED VADER: Vader has no weapon and none in hand — penalizing attack move (-100)");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.9: Error checking Vader weapon status: {}", e.getMessage());
            }
            try {
                boolean hasWeapon = false;
                boolean isLightsaber = false;
                String weaponName = null;
                List<PhysicalCard> attached = gameState.getAttachedCards(cardToMove);
                if (attached != null) {
                    for (PhysicalCard att : attached) {
                        if (att == null || att.getBlueprint() == null) continue;
                        if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                            hasWeapon = true;
                            weaponName = att.getTitle();
                            if (weaponName != null && weaponName.toLowerCase(Locale.ROOT).contains("lightsaber")) {
                                isLightsaber = true;
                            }
                        }
                    }
                }

                if (hasWeapon) {
                    String charTitle = cardToMove.getTitle() != null ? cardToMove.getTitle() : "character";
                    String charLower = charTitle.toLowerCase(Locale.ROOT);
                    boolean isVader = charLower.contains("vader");

                    // Check for IHYN (I Have You Now) in hand — makes Vader even more devastating
                    boolean hasIHYN = false;
                    if (isVader) {
                        try {
                            List<PhysicalCard> hand = gameState.getHand(playerId);
                            if (hand != null) {
                                for (PhysicalCard hCard : hand) {
                                    if (hCard != null && hCard.getTitle() != null) {
                                        String hTitle = hCard.getTitle().toLowerCase(Locale.ROOT);
                                        if (hTitle.contains("i have you now")) {
                                            hasIHYN = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }

                    // Calculate effective power with weapon bonus
                    // Lightsaber: +4 (weapon hit power + throw destiny)
                    // Other weapon: +2
                    // IHYN in hand: +3 (extra destiny draws)
                    float effectivePower = myPower;
                    if (isLightsaber) effectivePower += 4.0f;
                    else effectivePower += 2.0f;
                    if (hasIHYN) effectivePower += 3.0f;

                    // Look for opponent locations to attack (opponentId already declared above)
                    float bestAttackScore = 0;
                    String bestTargetLoc = null;
                    boolean foundLuke = false;

                    for (PhysicalCard adjLocation : gameState.getLocationsInOrder()) {
                        if (adjLocation == location) continue;

                        float theirPowerThere = 0;
                        int theirCountThere = 0;
                        boolean lukeHere = false;

                        List<PhysicalCard> cardsAtAdj = gameState.getCardsAtLocation(adjLocation);
                        for (PhysicalCard card : cardsAtAdj) {
                            if (card == null) continue;
                            String owner = card.getOwner();
                            SwccgCardBlueprint bp = card.getBlueprint();
                            if (bp == null) continue;

                            if (opponentId != null && opponentId.equals(owner)) {
                                if (bp.hasPowerAttribute()) {
                                    Float pw = bp.getPower();
                                    theirPowerThere += (pw != null ? pw : 0);
                                    theirCountThere++;
                                }
                                // Check for Luke (Hunt Down target)
                                if (isVader && card.getTitle() != null
                                    && card.getTitle().toLowerCase(Locale.ROOT).contains("luke")) {
                                    lukeHere = true;
                                }
                            }
                        }

                        if (theirCountThere > 0 && effectivePower > theirPowerThere) {
                            // We can beat them with our weapon advantage
                            float attackScore = 60.0f;
                            float powerAdvantage = effectivePower - theirPowerThere;

                            // Bonus for bigger power advantage
                            if (powerAdvantage >= 6) attackScore += 40.0f;
                            else if (powerAdvantage >= 3) attackScore += 20.0f;

                            // Bonus for opponent icons (force drain value after winning)
                            SwccgCardBlueprint locBp = adjLocation.getBlueprint();
                            if (locBp != null) {
                                int oppIcons = (mySide == Side.DARK)
                                    ? locBp.getIconCount(Icon.LIGHT_FORCE)
                                    : locBp.getIconCount(Icon.DARK_FORCE);
                                attackScore += oppIcons * ICON_BONUS;
                            }

                            // HUGE bonus for Luke (Hunt Down objective target)
                            if (lukeHere && isVader) {
                                attackScore += 150.0f;
                                foundLuke = true;
                            }

                            if (attackScore > bestAttackScore) {
                                bestAttackScore = attackScore;
                                bestTargetLoc = adjLocation.getTitle();
                            }
                        }
                    }

                    if (bestAttackScore > 0 && bestTargetLoc != null) {
                        String reason;
                        if (foundLuke) {
                            reason = String.format("V29.7 WEAPON HUNTER: %s + %s should CHALLENGE LUKE at %s! (effective power %.0f)",
                                charTitle, weaponName, bestTargetLoc, effectivePower);
                            if (hasIHYN) reason += " + IHYN in hand!";
                        } else {
                            reason = String.format("V29.7 WEAPON HUNTER: %s + %s should attack %s (effective power %.0f vs opponents)",
                                charTitle, weaponName, bestTargetLoc, effectivePower);
                        }
                        action.addReasoning(reason, bestAttackScore);
                        logger.info("[MoveEvaluator] ⚔️ {} — score {}", reason, bestAttackScore);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.7: Error in weapon hunter check: {}", e.getMessage());
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

        // === V29.13: FORCE DRAIN MODIFIER CHECK — AVOID BAD DRAIN LOCATIONS ===
        // Rando was moving to locations with -1 force drain modifiers instead of better sites.
        // Check the force drain amount at the destination vs current location.
        // Penalize moves to locations where our force drain would be low/reduced.
        if (gameState != null && game != null && location != null) {
            try {
                // Extract destination location from action text
                // Format: "Move X from A to B using landspeed" or "Move X from A to B using card"
                String actionTextLowerFD = action.getDisplayText() != null
                    ? action.getDisplayText().toLowerCase(Locale.ROOT) : "";
                PhysicalCard destLocation = null;

                // Try to match destination by checking all locations
                for (PhysicalCard locCard : gameState.getLocationsInOrder()) {
                    if (locCard == null || locCard == location) continue;
                    String locName = locCard.getTitle() != null
                        ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";
                    if (!locName.isEmpty() && actionTextLowerFD.contains(locName)) {
                        destLocation = locCard;
                        break;
                    }
                }

                if (destLocation != null) {
                    float destDrainAmount = game.getModifiersQuerying().getForceDrainAmount(
                        gameState, destLocation, playerId);
                    float currentDrainAmount = game.getModifiersQuerying().getForceDrainAmount(
                        gameState, location, playerId);

                    if (destDrainAmount < currentDrainAmount && destDrainAmount < 1) {
                        // Moving to a WORSE drain location
                        float drainPenalty = -80.0f;
                        if (destDrainAmount <= 0) drainPenalty = -120.0f;
                        action.addReasoning(String.format(
                            "V29.13 BAD DRAIN SITE: %s has drain %.0f (current location has %.0f) — stay for better drain!",
                            destLocation.getTitle(), destDrainAmount, currentDrainAmount), drainPenalty);
                        logger.warn("V29.13 BAD DRAIN: Moving to {} drain={} vs current {} drain={} — penalty {}",
                            destLocation.getTitle(), (int)destDrainAmount,
                            location.getTitle(), (int)currentDrainAmount, (int)drainPenalty);
                    } else if (destDrainAmount > currentDrainAmount && destDrainAmount >= 2) {
                        // Moving to a BETTER drain location!
                        float drainBonus = 40.0f;
                        if (destDrainAmount >= 3) drainBonus = 80.0f;
                        action.addReasoning(String.format(
                            "V29.13 GOOD DRAIN SITE: %s has drain %.0f — better than current %.0f!",
                            destLocation.getTitle(), destDrainAmount, currentDrainAmount), drainBonus);
                        logger.info("V29.13 GOOD DRAIN: Moving to {} drain={} from {} drain={} — bonus {}",
                            destLocation.getTitle(), (int)destDrainAmount,
                            location.getTitle(), (int)currentDrainAmount, (int)drainBonus);
                    }
                }
            } catch (Exception e) {
                logger.debug("V29.13 DRAIN CHECK: Error: {}", e.getMessage());
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
        int favorable = RandoConfig.BATTLE_FAVORABLE_THRESHOLD;
        int danger = RandoConfig.BATTLE_DANGER_THRESHOLD;

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
