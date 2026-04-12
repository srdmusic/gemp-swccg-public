package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.chosenone.ChosenOneConfig;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates battle initiation decisions.
 *
 * Decision factors (from Python battle_evaluator.py):
 * - Power differential (my power - their power)
 * - Reserve deck (need cards for destiny draws)
 * - Strategic situation (ahead/behind on board/life force)
 *
 * Threat levels:
 * - CRUSH: Power advantage 8+ -> definitely battle
 * - FAVORABLE: Power advantage 5-7 -> battle recommended
 * - MARGINAL: Power advantage 2-4 -> battle worth considering
 * - RISKY: Power diff 0 to +1 -> cautious
 * - DANGEROUS: Power disadvantage -> avoid/retreat
 *
 * Ported from Python battle_evaluator.py
 */
public class BattleEvaluator extends ActionEvaluator {

    // Battle thresholds (power advantage needed)
    private static final int CRUSH_THRESHOLD = 8;      // Overwhelming advantage
    private static final int FAVORABLE_THRESHOLD = 5;  // Strong advantage
    private static final int MARGINAL_THRESHOLD = 2;   // Worth initiating
    private static final int RISKY_THRESHOLD = 0;      // Even or slight advantage

    // Minimum reserve deck for destiny draws
    private static final int MIN_RESERVE_FOR_BATTLE = 3;

    public BattleEvaluator() {
        super("Battle");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // Handle CARD_ACTION_CHOICE with battle-related actions
        if (!"CARD_ACTION_CHOICE".equals(context.getDecisionType())) {
            return false;
        }

        // Check for battle phase or battle-related decision
        Phase phase = context.getPhase();
        String decisionText = context.getDecisionText();
        String decisionLower = decisionText != null ? decisionText.toLowerCase(Locale.ROOT) : "";

        // During battle phase
        if (phase == Phase.BATTLE) {
            return true;
        }

        // Or if decision text mentions battle/initiate
        if (decisionLower.contains("battle") || decisionLower.contains("initiate")) {
            return true;
        }

        // Check if any action mentions battle
        List<String> actionTexts = context.getActionTexts();
        if (actionTexts != null) {
            for (String actionText : actionTexts) {
                if (actionText != null) {
                    String actionLower = actionText.toLowerCase(Locale.ROOT);
                    if (actionLower.contains("initiate battle") || actionLower.contains("battle")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        logger.info("[BattleEvaluator] Evaluating battle decision");

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();

        if (actionIds == null || actionTexts == null) {
            logger.warn("[BattleEvaluator] No action IDs or texts available");
            return actions;
        }

        logger.debug("[BattleEvaluator] Phase={}, actions={}", context.getPhase(), actionIds.size());

        // Get game state info
        int reserveDeck = context.getReserveDeckSize();
        int lifeForce = context.getLifeForce();
        int forcePile = context.getForcePileSize();

        // Calculate board position
        boolean isBehindOnLifeForce = false;
        boolean isAheadOnLifeForce = false;
        if (gameState != null) {
            String playerId = context.getPlayerId();
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId != null) {
                int opponentLifeForce = gameState.getPlayerLifeForce(opponentId);
                isBehindOnLifeForce = lifeForce < opponentLifeForce - 5;
                isAheadOnLifeForce = lifeForce > opponentLifeForce + 5;
            }
        }

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle battle-related actions
            if (!actionLower.contains("battle") && !actionLower.contains("fire")) {
                continue;
            }

            EvaluatedAction action = new EvaluatedAction(
                actionId,
                ActionType.BATTLE,
                100.0f,  // V34: Raised base score from 50 — bot needs to actually fight
                actionText
            );

            // === INITIATE BATTLE SCORING ===
            if (actionLower.contains("initiate battle")) {
                // V22.4: LOCATION-SPECIFIC battle evaluation
                // OLD BUG: Checked ALL locations — if ANY was favorable, approved initiation.
                // But the action is for a SPECIFIC location! Rando initiated battle at Dining Room
                // (3 vs 26 power) because another location was favorable.
                // NEW: Try to extract the specific location from the action text first.

                SwccgGame game = context.getGame();
                boolean foundFavorableBattle = false;
                boolean foundAnyContestedLocation = false;
                boolean checkedSpecificLocation = false;

                if (game != null && gameState != null) {
                    String playerId = context.getPlayerId();
                    String opponentId = gameState.getOpponent(playerId);

                    if (opponentId != null) {
                        try {
                            // V22.4: First try to find the SPECIFIC location this action targets
                            PhysicalCard targetLocation = null;
                            for (PhysicalCard location : gameState.getTopLocations()) {
                                String locTitle = location.getTitle();
                                if (locTitle != null && actionLower.contains(locTitle.toLowerCase(Locale.ROOT))) {
                                    targetLocation = location;
                                    break;
                                }
                            }

                            if (targetLocation != null) {
                                // V22.4: Evaluate THIS SPECIFIC location only
                                checkedSpecificLocation = true;
                                float ourPowerRaw = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, targetLocation, playerId, false, false);
                                float theirPowerRaw = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, targetLocation, opponentId, false, false);

                                // === V42: EXCLUSION-AWARE POWER — subtract barriered/excluded characters ===
                                float ourExcludedPower = 0;
                                float theirExcludedPower = 0;
                                java.util.List<String> ourExcludedNames = new java.util.ArrayList<>();
                                java.util.List<String> theirExcludedNames = new java.util.ArrayList<>();
                                try {
                                    for (PhysicalCard locCard : gameState.getCardsAtLocation(targetLocation)) {
                                        if (locCard == null || locCard.getBlueprint() == null) continue;
                                        com.gempukku.swccgo.common.CardCategory cat = locCard.getBlueprint().getCardCategory();
                                        if (cat != com.gempukku.swccgo.common.CardCategory.CHARACTER &&
                                            cat != com.gempukku.swccgo.common.CardCategory.STARSHIP &&
                                            cat != com.gempukku.swccgo.common.CardCategory.VEHICLE) continue;
                                        boolean prohibited = game.getModifiersQuerying()
                                            .isProhibitedFromParticipatingInBattle(gameState, locCard, playerId);
                                        if (prohibited) {
                                            Float pw = locCard.getBlueprint().getPower();
                                            float cardPower = pw != null ? pw : 0;
                                            String cardTitle = locCard.getTitle() != null ? locCard.getTitle() : "?";
                                            if (playerId.equals(locCard.getOwner())) {
                                                ourExcludedPower += cardPower;
                                                ourExcludedNames.add(cardTitle);
                                            } else {
                                                theirExcludedPower += cardPower;
                                                theirExcludedNames.add(cardTitle);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V42: Error checking exclusions: {}", e.getMessage());
                                }
                                float ourPower = ourPowerRaw - ourExcludedPower;
                                float theirPower = theirPowerRaw - theirExcludedPower;
                                if (ourExcludedPower > 0 || theirExcludedPower > 0) {
                                    logger.warn("V42 BARRIER AWARENESS at {}: Our excluded={} ({}), Their excluded={} ({}). " +
                                        "Adjusted power: {}->{} vs {}->{}",
                                        targetLocation.getTitle(),
                                        ourExcludedNames, (int)ourExcludedPower,
                                        theirExcludedNames, (int)theirExcludedPower,
                                        (int)ourPowerRaw, (int)ourPower,
                                        (int)theirPowerRaw, (int)theirPower);
                                }
                                if (ourExcludedPower > 0) {
                                    action.addReasoning(String.format(
                                        "V42 BARRIERED: %s excluded from battle! Actual power %.0f (not %.0f)",
                                        ourExcludedNames, ourPower, ourPowerRaw), 0.0f);
                                }

                                float ourAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                    gameState, playerId, targetLocation);
                                float theirAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                    gameState, opponentId, targetLocation);
                                float powerDiff = ourPower - theirPower;
                                float abilityDiff = ourAbility - theirAbility;
                                float effectiveDiff = powerDiff + (abilityDiff * 2.5f);

                                logger.info("V22.4 [BattleEvaluator] SPECIFIC location {}: power={}/{}, ability={}/{}, effectiveDiff={}",
                                    targetLocation.getTitle(), ourPower, theirPower, ourAbility, theirAbility, effectiveDiff);

                                // === V29.7: WEAPON COMBAT AWARENESS ===
                                float weaponBonus = 0;
                                boolean ourVaderHere = false;
                                boolean lukeHere = false;
                                boolean hasIHYN = false;
                                java.util.List<PhysicalCard> cardsHere = null;
                                try {
                                    cardsHere = gameState.getCardsAtLocation(targetLocation);
                                    for (PhysicalCard locCard : cardsHere) {
                                        if (locCard == null || locCard.getBlueprint() == null) continue;
                                        if (locCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;

                                        String cardOwner = locCard.getOwner();
                                        String locCardTitle = locCard.getTitle() != null ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";

                                        if (playerId.equals(cardOwner)) {
                                            if (locCardTitle.contains("vader")) ourVaderHere = true;
                                            java.util.List<PhysicalCard> attachments = gameState.getAttachedCards(locCard);
                                            if (attachments != null) {
                                                for (PhysicalCard att : attachments) {
                                                    if (att == null || att.getBlueprint() == null) continue;
                                                    if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                                        String wepTitle = att.getTitle() != null ? att.getTitle().toLowerCase(Locale.ROOT) : "";
                                                        if (wepTitle.contains("lightsaber")) {
                                                            weaponBonus += 5.0f;
                                                        } else {
                                                            weaponBonus += 3.0f;
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (opponentId != null && opponentId.equals(cardOwner)) {
                                            if (locCardTitle.contains("luke")) lukeHere = true;
                                        }
                                    }

                                    // Check for IHYN in hand
                                    if (ourVaderHere) {
                                        java.util.List<PhysicalCard> hand = gameState.getHand(playerId);
                                        if (hand != null) {
                                            for (PhysicalCard hCard : hand) {
                                                if (hCard != null && hCard.getTitle() != null
                                                    && hCard.getTitle().toLowerCase(Locale.ROOT).contains("i have you now")) {
                                                    hasIHYN = true;
                                                    weaponBonus += 3.0f;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V29.7: Error checking weapons for battle: {}", e.getMessage());
                                }

                                float weaponEffectiveDiff = effectiveDiff + weaponBonus;
                                if (weaponBonus > 0) {
                                    logger.info("V29.7 WEAPON AWARENESS at {}: base effectiveDiff={}, weaponBonus=+{}, adjusted={}{}{}",
                                        targetLocation.getTitle(), effectiveDiff, weaponBonus, weaponEffectiveDiff,
                                        ourVaderHere ? " [VADER]" : "", hasIHYN ? " [IHYN]" : "");
                                }

                                // === V29.9: REBEL BARRIER RISK ASSESSMENT ===
                                float barrierRiskPenalty = 0;
                                if (ourVaderHere && ourPower > 0 && theirPower > 0 && cardsHere != null
                                    && theirPower >= 5) {  // V42: Only assess barrier risk vs real threats
                                    float powerWithoutVader = 0;
                                    int charCountWithoutVader = 0;
                                    for (PhysicalCard locCard : cardsHere) {
                                        if (locCard == null || locCard.getBlueprint() == null) continue;
                                        if (locCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        if (!playerId.equals(locCard.getOwner())) continue;
                                        String lcTitle = locCard.getTitle() != null ? locCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (lcTitle.contains("vader")) continue;
                                        Float pw = locCard.getBlueprint().getPower();
                                        powerWithoutVader += (pw != null ? pw : 0);
                                        charCountWithoutVader++;
                                    }

                                    float powerDeficitWithoutVader = theirPower - powerWithoutVader;
                                    if (powerDeficitWithoutVader > 5) {
                                        barrierRiskPenalty = -150.0f;
                                        if (charCountWithoutVader <= 1) barrierRiskPenalty = -250.0f;
                                        if (powerDeficitWithoutVader > 10) barrierRiskPenalty -= 100.0f;

                                        // V35: VADER EXPENDABILITY
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer expendAnalyzer =
                                            context.getObjectiveAnalyzer();
                                        if (expendAnalyzer != null && expendAnalyzer.isAnalyzed() && expendAnalyzer.isHuntDownV()) {
                                            barrierRiskPenalty = barrierRiskPenalty * ChosenOneConfig.VADER_EXPENDABILITY_FACTOR;
                                            logger.warn("V35 VADER EXPENDABLE: Barrier risk reduced to {} (Hunt Down — Vader is replaceable)",
                                                (int)barrierRiskPenalty);
                                        }

                                        action.addReasoning(String.format(
                                            "V29.9 BARRIER RISK: If opponent Barriers Vader, remaining power %.0f vs %.0f — %s!",
                                            powerWithoutVader, theirPower,
                                            charCountWithoutVader == 0 ? "NO ONE LEFT" : "crushed"),
                                            barrierRiskPenalty);
                                        logger.warn("V29.9 BARRIER RISK at {}: Without Vader: {} vs {} (deficit {}), penalty {}",
                                            targetLocation.getTitle(), (int)powerWithoutVader, (int)theirPower,
                                            (int)powerDeficitWithoutVader, (int)barrierRiskPenalty);
                                    }
                                }

                                // === V29.9: HUNT DOWN VADER BATTLE AGGRESSIVENESS ===
                                if (ourVaderHere && weaponBonus > 0) {
                                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer battleObjAnalyzer =
                                        context.getObjectiveAnalyzer();
                                    if (battleObjAnalyzer != null && battleObjAnalyzer.isAnalyzed() && battleObjAnalyzer.isHuntDownV()) {
                                        float huntBonus = 80.0f;
                                        if (lukeHere) huntBonus = 200.0f;
                                        action.addReasoning(String.format(
                                            "V29.9 HUNT DOWN: Armed Vader should FIGHT! %s (+%.0f)",
                                            lukeHere ? "LUKE IS HERE — THIS IS THE OBJECTIVE!" : "Vader hunts and destroys!",
                                            huntBonus), huntBonus);
                                        logger.warn("V29.9 HUNT DOWN: Armed Vader aggressiveness boost +{} (Luke: {})",
                                            (int)huntBonus, lukeHere);
                                    }
                                }

                                // === V35: INQUISITOR BATTLE DESTINY BONUS ===
                                {
                                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v35ObjAnalyzer =
                                        context.getObjectiveAnalyzer();
                                    if (v35ObjAnalyzer != null && v35ObjAnalyzer.isAnalyzed() && v35ObjAnalyzer.isHuntDownV()
                                        && cardsHere != null) {
                                        boolean inquisitorInBattle = false;
                                        boolean hatredAtLocation = false;
                                        boolean jediAtLocation = false;

                                        for (PhysicalCard bCard : cardsHere) {
                                            if (bCard == null || bCard.getBlueprint() == null) continue;
                                            String bTitle = bCard.getTitle() != null ? bCard.getTitle().toLowerCase(Locale.ROOT) : "";

                                            if (playerId.equals(bCard.getOwner())) {
                                                if (isInquisitor(bTitle)) {
                                                    inquisitorInBattle = true;
                                                }
                                            } else {
                                                if (isJediOrPadawan(bTitle)) {
                                                    jediAtLocation = true;
                                                }
                                                try {
                                                    java.util.List<PhysicalCard> stacked = gameState.getStackedCards(bCard);
                                                    if (stacked != null && !stacked.isEmpty()) {
                                                        hatredAtLocation = true;
                                                    }
                                                } catch (Exception e) { /* ignore */ }
                                            }
                                        }

                                        if (inquisitorInBattle) {
                                            float destinyBonus = 120.0f;
                                            if (hatredAtLocation) destinyBonus = 250.0f;
                                            if (jediAtLocation) destinyBonus += 100.0f;
                                            action.addReasoning(String.format(
                                                "V35 HUNT DESTINY: Inquisitor in battle%s%s — +%d total battle destiny!",
                                                hatredAtLocation ? " + HATRED" : "",
                                                jediAtLocation ? " vs JEDI" : "",
                                                hatredAtLocation ? 2 : 1), destinyBonus);
                                            logger.warn("V35 HUNT DESTINY at {}: Inquisitor={}, hatred={}, jedi={} — bonus +{}",
                                                targetLocation.getTitle(), inquisitorInBattle, hatredAtLocation,
                                                jediAtLocation, (int)destinyBonus);
                                        }
                                    }
                                }

                                // === V42: Count opponent characters for solo-target detection ===
                                int theirCharCount = 0;
                                if (cardsHere != null) {
                                    for (PhysicalCard countCard : cardsHere) {
                                        if (countCard == null || countCard.getBlueprint() == null) continue;
                                        if (countCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        if (opponentId != null && opponentId.equals(countCard.getOwner())) {
                                            try {
                                                if (!game.getModifiersQuerying().isProhibitedFromParticipatingInBattle(
                                                        gameState, countCard, playerId)) {
                                                    theirCharCount++;
                                                }
                                            } catch (Exception e) { theirCharCount++; }
                                        }
                                    }
                                }

                                if (ourPower > 0 && theirPower > 0) {
                                    foundAnyContestedLocation = true;

                                    // === V42: SOLO TARGET BONUS ===
                                    if (theirCharCount <= 1 && ourPower >= theirPower) {
                                        float soloBonus = 200.0f;
                                        if (ourPower >= theirPower * 1.5f) soloBonus = 300.0f;
                                        action.addReasoning(String.format(
                                            "V42 EASY TARGET: Solo opponent (power %.0f) at %s — ATTACK!",
                                            theirPower, targetLocation.getTitle()), soloBonus);
                                        logger.warn("V42 EASY TARGET at {}: our {} vs solo {} — bonus +{}",
                                            targetLocation.getTitle(), (int)ourPower, (int)theirPower, (int)soloBonus);
                                        foundFavorableBattle = true;
                                    }

                                    // V29.7: Use weapon-adjusted effective diff for battle decisions.
                                    if (theirPower > ourPower && weaponBonus == 0 && effectiveDiff < MARGINAL_THRESHOLD) {
                                        float penalty = -300.0f;
                                        if (theirPower > ourPower * 2) penalty = -600.0f;
                                        action.addReasoning(String.format("V29 DON'T INITIATE: %.0f vs %.0f power — we're outgunned!",
                                            ourPower, theirPower), penalty);
                                        logger.warn("V29 BATTLE BLOCK at {}: our {} vs their {} — BLOCKED (penalty {})",
                                            targetLocation.getTitle(), (int)ourPower, (int)theirPower, (int)penalty);
                                    } else if (theirPower > ourPower && weaponBonus > 0 && weaponEffectiveDiff < MARGINAL_THRESHOLD) {
                                        action.addReasoning(String.format("V29.7 WEAPONS NOT ENOUGH: power %.0f+weapons vs %.0f — still risky",
                                            ourPower, theirPower), -150.0f);
                                    } else if (weaponEffectiveDiff >= FAVORABLE_THRESHOLD) {
                                        foundFavorableBattle = true;
                                        float battleBonus = 150.0f;
                                        String battleReason;
                                        if (weaponBonus > 0) {
                                            battleBonus += weaponBonus * 10.0f;
                                            if (ourVaderHere && lukeHere) {
                                                battleBonus += 100.0f;
                                                battleReason = String.format("V29.7 VADER vs LUKE at %s! Power %.0f + weapons vs %.0f — CHALLENGE!",
                                                    targetLocation.getTitle(), ourPower, theirPower);
                                            } else {
                                                battleReason = String.format("V29.7 ARMED BATTLE at %s (power %.0f + weapons vs %.0f, effective diff=%.0f)",
                                                    targetLocation.getTitle(), ourPower, theirPower, weaponEffectiveDiff);
                                            }
                                            if (hasIHYN) battleReason += " + IHYN!";
                                        } else {
                                            battleReason = String.format("Favorable battle at %s (power %.0f vs %.0f, ability %.0f vs %.0f)",
                                                targetLocation.getTitle(), ourPower, theirPower, ourAbility, theirAbility);
                                        }
                                        action.addReasoning(battleReason, battleBonus);
                                    } else if (weaponEffectiveDiff >= MARGINAL_THRESHOLD) {
                                        if (weaponBonus > 0) {
                                            action.addReasoning(String.format("V34 ARMED MARGINAL at %s (power %.0f + weapons vs %.0f) — weapons help!",
                                                targetLocation.getTitle(), ourPower, theirPower), 80.0f);
                                        } else {
                                            // V42: Marginal without weapons — still worth trying
                                            action.addReasoning(String.format("V42 MARGINAL at %s (power %.0f vs %.0f, effective %.0f) — we have the edge",
                                                targetLocation.getTitle(), ourPower, theirPower, effectiveDiff), 30.0f);
                                        }
                                    } else if (effectiveDiff >= 0) {
                                        // V42: Even fight — slight positive to encourage aggression
                                        action.addReasoning(String.format("V42 EVEN FIGHT at %s (power %.0f vs %.0f) — coin flip",
                                            targetLocation.getTitle(), ourPower, theirPower), 10.0f);
                                    } else {
                                        // Unfavorable — don't initiate
                                        float penalty = -100.0f;
                                        if (weaponEffectiveDiff < -8) penalty = -200.0f;
                                        if (weaponEffectiveDiff < -15) penalty = -400.0f;
                                        action.addReasoning(String.format("V29: UNFAVORABLE at %s (power %.0f vs %.0f) - don't initiate!",
                                            targetLocation.getTitle(), ourPower, theirPower), penalty);
                                    }
                                } else if (ourPower > 0 && theirPower == 0) {
                                    // We're alone here - no battle possible
                                    action.addReasoning("No opponent here", -20.0f);
                                }
                            }

                            // V22.4: Fallback — if we couldn't identify the specific location,
                            // check all locations but be MORE conservative
                            if (!checkedSpecificLocation) {
                                for (PhysicalCard location : gameState.getTopLocations()) {
                                    float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, playerId, false, false);
                                    float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, opponentId, false, false);

                                    if (ourPower > 0 && theirPower > 0) {
                                        foundAnyContestedLocation = true;
                                        float powerDiff = ourPower - theirPower;
                                        float ourAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                            gameState, playerId, location);
                                        float theirAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                            gameState, opponentId, location);
                                        float abilityDiff = ourAbility - theirAbility;
                                        float effectiveDiff = powerDiff + (abilityDiff * 2.5f);

                                        logger.info("[BattleEvaluator] Checking {}: power={}/{} (diff={}), ability={}/{} (diff={})",
                                            location.getTitle(), ourPower, theirPower, powerDiff,
                                            ourAbility, theirAbility, abilityDiff);

                                        // V22.4: Check for any suicidal locations — if ANY location
                                        // has our power < 50% of theirs, add strong warning
                                        if (theirPower > ourPower * 2 && theirPower > 6) {
                                            action.addReasoning(String.format("V22.4 DANGER at %s (%.0f vs %.0f) - might battle here!",
                                                location.getTitle(), ourPower, theirPower), -80.0f);
                                        }

                                        if (effectiveDiff >= MARGINAL_THRESHOLD) {
                                            foundFavorableBattle = true;
                                            action.addReasoning(String.format("Favorable battle at %s (power %.0f vs %.0f, ability %.0f vs %.0f)",
                                                location.getTitle(), ourPower, theirPower, ourAbility, theirAbility), 40.0f);
                                            break;
                                        } else if (abilityDiff < -1) {
                                            action.addReasoning(String.format("Ability disadvantage at %s (%.0f vs %.0f) - enemy draws more destiny",
                                                location.getTitle(), ourAbility, theirAbility), -25.0f);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("[BattleEvaluator] Could not check locations: {}", e.getMessage());
                        }
                    }
                }

                if (!foundFavorableBattle && foundAnyContestedLocation) {
                    action.addReasoning("No favorable battles available - don't initiate", -60.0f);
                } else if (!foundAnyContestedLocation) {
                    action.addReasoning("No contested locations", -20.0f);
                }

                // Check if we have enough reserve for destiny draws
                if (reserveDeck < MIN_RESERVE_FOR_BATTLE) {
                    action.addReasoning(
                        String.format("Low reserve deck (%d) - risky destiny draws", reserveDeck),
                        -50.0f
                    );
                }

                // Strategic position adjustments (reduced impact - power diff is more important)
                if (isBehindOnLifeForce) {
                    // When behind, slight encouragement but power still matters most
                    action.addReasoning("Behind on life force - slightly more aggressive", 15.0f);
                } else if (isAheadOnLifeForce) {
                    // When ahead, be more conservative
                    action.addReasoning("Ahead on life force - can afford to wait", -20.0f);
                }

                // Life force critical - more aggressive but still check power
                if (lifeForce <= ChosenOneConfig.CRITICAL_LIFE_FORCE) {
                    action.addReasoning("Low life force - need to act", 30.0f);
                }
            }

            // === WEAPON FIRING ===
            if (actionLower.contains("fire")) {
                action.addReasoning("Fire weapon", 40.0f);

                // Target selection bonuses
                if (actionLower.contains("character")) {
                    action.addReasoning("Target character", 10.0f);
                }
                if (actionLower.contains("unique") || actionLower.contains("•")) {
                    action.addReasoning("Target unique card", 20.0f);
                }
            }

            // === CANCEL BATTLE (It's A Trap, etc.) ===
            if (actionLower.contains("cancel battle") || actionLower.contains("cancel the battle")) {
                SwccgGame game = context.getGame();
                if (game != null && gameState != null) {
                    BattleState battleState = gameState.getBattleState();
                    String playerId = context.getPlayerId();

                    if (battleState != null) {
                        String initiator = battleState.getPlayerInitiatedBattle();
                        boolean weInitiated = playerId != null && playerId.equals(initiator);

                        if (weInitiated) {
                            // NEVER cancel a battle we started - that's wasteful
                            action.addReasoning("DO NOT cancel our own battle! Waste of interrupt.", -150.0f);
                            logger.info("[BattleEvaluator] Penalizing cancel - WE initiated this battle");
                        } else {
                            // Opponent initiated - check if we should cancel
                            String opponentId = gameState.getOpponent(playerId);
                            PhysicalCard battleLocation = battleState.getBattleLocation();

                            if (battleLocation != null && opponentId != null) {
                                try {
                                    float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, battleLocation, playerId, false, false);
                                    float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, battleLocation, opponentId, false, false);
                                    float powerDiff = ourPower - theirPower;

                                    if (powerDiff < -FAVORABLE_THRESHOLD) {
                                        // We're badly losing - cancel is valuable
                                        action.addReasoning(String.format("Cancel losing battle (%.0f vs %.0f)", ourPower, theirPower), 60.0f);
                                    } else if (powerDiff < 0) {
                                        // Slight disadvantage - cancel might be worth it
                                        action.addReasoning(String.format("Cancel unfavorable battle (%.0f vs %.0f)", ourPower, theirPower), 20.0f);
                                    } else {
                                        // We're winning or even - don't waste the interrupt
                                        action.addReasoning(String.format("Don't cancel - we're not losing (%.0f vs %.0f)", ourPower, theirPower), -60.0f);
                                    }
                                } catch (Exception e) {
                                    logger.warn("[BattleEvaluator] Could not get power for cancel decision: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            // === BATTLE TACTICS (during battle) ===
            if (context.getPhase() == Phase.BATTLE) {
                // Fire before forfeit
                if (actionLower.contains("fire")) {
                    action.addReasoning("Fire weapons during battle", 50.0f);
                }

                // Draw battle destiny
                if (actionLower.contains("draw") && actionLower.contains("destiny")) {
                    action.addReasoning("Draw battle destiny", 30.0f);
                }
            }

            logger.debug("[BattleEvaluator] Scored '{}' -> {}",
                actionText.length() > 40 ? actionText.substring(0, 40) + "..." : actionText,
                String.format("%.1f", action.getScore()));

            actions.add(action);
        }

        logger.info("[BattleEvaluator] Evaluated {} battle actions", actions.size());
        return actions;
    }
}
