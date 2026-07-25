package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: BATTLE-1 (Initiation) (reorg 2026-07-06) ═══
// Owns: whether/where to initiate battle: V22.4 power math (-800 danger tiers), V76 predictor veto,
// V61/V61b low-reserve destiny guards (-800/-400/-200), V164a +40 favorable, V34 must-fight raises
// (+100 base / +150-200), V27/V27.1 interrupt-fee Force reservations, V29.9 Hunt Down aggro + Barrier risk.
// Hub: none. KIND mix: 14 BANDED / 3 VETO.
// TRAP: initiation is ALSO scored by ActionTextEvaluator's V25 power-tier block (SUICIDE/CRUSH/FAVORABLE/
// MARGINAL tiers) — the SUM of the two is the behavior; preserve it when changing either side.
// Creature ATTACKS are OUT of scope (different rulebook construct — obligations lane, SVC-SAFETY);
// battle-keyed rules must never fire on attacks.
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: BATTLE-2 (ATE weapons-segment suite), BATTLE-3 (forfeit/damage in CardSelectionEvaluator),
// ACTIVATE (V61c battle-intent predicate). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
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
public final class BattleDecisionPolicy {

    public interface Context {
        String getDecisionType();
        Phase getPhase();
        String getDecisionText();
        List<String> getActionIds();
        List<String> getActionTexts();
        List<String> getCardIds();
        GameState getGameState();
        SwccgGame getGame();
        String getPlayerId();
        int getReserveDeckSize();
        int getLifeForce();
        int getForcePileSize();
        int getHandSize();
        ObjectiveAnalyzer getObjectiveAnalyzer();
        float getVaderExpendabilityFactor();
        int getCriticalLifeForce();
        Prediction predictBattle(int myPower, int myDestinyDraws,
                                 int opponentPower, int opponentDestinyDraws);
        Logger getLogger();
    }

    public static final class Prediction {
        public final float winProbability;
        public final float expectedDamageDealt;
        public final float expectedDamageTaken;

        public Prediction(float winProbability, float expectedDamageDealt,
                          float expectedDamageTaken) {
            this.winProbability = winProbability;
            this.expectedDamageDealt = expectedDamageDealt;
            this.expectedDamageTaken = expectedDamageTaken;
        }
    }

    public record Contribution(String reason, float delta, boolean hardVeto,
                               TraceRuleId ruleArmId, TraceDomainId domainId,
                               TraceOutputKind outputKind) {
    }

    public record ScoredAction(String actionId, String actionText, float baseScore,
                               List<Contribution> contributions) {
        public ScoredAction {
            contributions = List.copyOf(contributions);
        }
    }

    private static final class ScoreCollector {
        private final String actionId;
        private final String actionText;
        private final float baseScore;
        private final List<Contribution> contributions = new ArrayList<>();
        private float score;

        private ScoreCollector(String actionId, String actionText, float baseScore) {
            this.actionId = actionId;
            this.actionText = actionText;
            this.baseScore = baseScore;
            this.score = baseScore;
        }

        private void addReasoning(String reason, float delta) {
            contributions.add(new Contribution(reason, delta, false, null, null, null));
            score += delta;
        }

        private void hardVeto(String reason) {
            contributions.add(new Contribution(reason, 0.0f, true, null, null, null));
        }

        private void apply(BattleInitiationPolicy.Contribution contribution) {
            if (contribution.applies()) {
                addReasoning(contribution.reason(), contribution.delta());
            }
        }

        private void apply(List<BattleInitiationPolicy.Contribution> ordered) {
            for (BattleInitiationPolicy.Contribution contribution : ordered) {
                apply(contribution);
            }
        }

        private void apply(PolicyResult result) {
            for (PolicyOperation operation : result.operations()) {
                switch (operation.kind()) {
                    case ADD -> {
                        contributions.add(new Contribution(
                            operation.reason(), operation.delta(), false,
                            operation.ruleArmId(), operation.domainId(), operation.outputKind()));
                        score += operation.delta();
                    }
                    case HARD_VETO -> contributions.add(new Contribution(
                        operation.reason(), 0.0f, true,
                        operation.ruleArmId(), operation.domainId(), operation.outputKind()));
                    case DEFER -> throw new IllegalStateException(
                        "BATTLE-1 does not support deferred BattleEvaluator contributions");
                }
            }
        }

        private float getScore() {
            return score;
        }

        private ScoredAction result() {
            return new ScoredAction(actionId, actionText, baseScore, contributions);
        }
    }

    private BattleDecisionPolicy() {
    }

    public static boolean canEvaluate(Context context) {
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

    public static List<ScoredAction> evaluate(Context context) {
        List<ScoredAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        Logger logger = context.getLogger();
        logger.info("[BattleEvaluator] Evaluating battle decision");

        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();

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
            String actionCardId = cardIds != null && i < cardIds.size() ? cardIds.get(i) : null;
            String actionLower = actionText.toLowerCase(Locale.ROOT);

            // Only handle battle-related actions
            if (!actionLower.contains("battle") && !actionLower.contains("fire")) {
                continue;
            }

            ScoreCollector action = new ScoreCollector(
                actionId,
                actionText,
                100.0f  // V34: Raised base score from 50 — Rando needs to actually fight
            );

            // === INITIATE BATTLE SCORING ===
            if (actionLower.contains("initiate battle")) {
                // V22.4: LOCATION-SPECIFIC battle evaluation
                // OLD BUG: Checked ALL locations — if ANY was favorable, approved initiation.
                // But the action is for a SPECIFIC location! Rando initiated battle at Dining Room
                // (3 vs 26 power) because another location was favorable.
                // NEW: Resolve the stock aligned card id first; retain named-text fallback for legacy callers.

                SwccgGame game = context.getGame();
                boolean foundFavorableBattle = false;
                boolean foundAnyContestedLocation = false;
                boolean checkedSpecificLocation = false;
                Float selectedBattlePowerMargin = null;

                if (game != null && gameState != null) {
                    String playerId = context.getPlayerId();
                    String opponentId = gameState.getOpponent(playerId);

                    if (opponentId != null) {
                        try {
                            // Stock CardActionSelectionDecision aligns cardId with each action ordinal.
                            PhysicalCard targetLocation = BattleTargetResolver.resolve(
                                    gameState.getTopLocations(), actionCardId, actionText);

                            if (targetLocation != null) {
                                // V22.4: Evaluate THIS SPECIFIC location only
                                checkedSpecificLocation = true;
                                float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, targetLocation, playerId, false, false);
                                float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, targetLocation, opponentId, false, false);
                                float ourAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                    gameState, playerId, targetLocation);
                                float theirAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                    gameState, opponentId, targetLocation);
                                float powerDiff = ourPower - theirPower;
                                selectedBattlePowerMargin = powerDiff;
                                float abilityDiff = ourAbility - theirAbility;
                                float effectiveDiff = powerDiff + (abilityDiff * 2.5f);

                                logger.info("V22.4 [BattleEvaluator] SPECIFIC location {}: power={}/{}, ability={}/{}, effectiveDiff={}",
                                    targetLocation.getTitle(), ourPower, theirPower, ourAbility, theirAbility, effectiveDiff);

                                // === V29.7: WEAPON COMBAT AWARENESS ===
                                // Raw power comparison misses the massive advantage weapons provide.
                                // Vader (power 6) + lightsaber = hit + throw destiny = effectively +4-6 power.
                                // IHYN in hand adds 2-3 more battle destiny draws.
                                // Check our characters for weapons and adjust effective power.
                                float weaponBonus = 0;
                                // V29.7/V76 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c): count the
                                // OPPONENT's weapons too — the predictor got our sabers (+wb) but raw
                                // enemy power, systematically underestimating armed defenders.
                                float oppWeaponBonus = 0;
                                boolean ourVaderHere = false;
                                boolean ourVaderArmed = false;
                                PhysicalCard ourVaderCard = null;
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
                                            BattleWeaponProfile profile = BattleWeaponProfile.assess(game, gameState, locCard);
                                            weaponBonus += profile.bonus();
                                            if (Filters.Vader.accepts(game, locCard)) {
                                                ourVaderHere = true;
                                                ourVaderArmed |= profile.armed();
                                                if (ourVaderCard == null) {
                                                    ourVaderCard = locCard;
                                                }
                                            }
                                        } else if (opponentId != null && opponentId.equals(cardOwner)) {
                                            // Opponent character — check for key targets
                                            if (locCardTitle.contains("luke")) lukeHere = true;
                                            oppWeaponBonus += BattleWeaponProfile.assess(game, gameState, locCard).bonus();
                                        }
                                    }

                                    // Check for IHYN in hand (devastating with Vader)
                                    if (ourVaderHere) {
                                        java.util.List<PhysicalCard> hand = gameState.getHand(playerId);
                                        if (hand != null) {
                                            for (PhysicalCard hCard : hand) {
                                                if (hCard != null && hCard.getTitle() != null
                                                    && hCard.getTitle().toLowerCase(Locale.ROOT).contains("i have you now")) {
                                                    hasIHYN = true;
                                                    weaponBonus += 3.0f; // Extra destiny draws
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V29.7: Error checking weapons for battle: {}", e.getMessage());
                                }

                                // Adjust effective diff with weapon bonus
                                // V29.7 ADJUSTED 2026-07-10b (Codex m00137 hole 1): subtract the OPPONENT's
                                // weapon bonus too — it previously reached only the V76 predictor input,
                                // so favorable-battle SCORING still ignored enemy weapons.
                                float weaponEffectiveDiff = effectiveDiff + weaponBonus - oppWeaponBonus;
                                if (weaponBonus > 0 || oppWeaponBonus > 0) {
                                    logger.info("V29.7 WEAPON AWARENESS at {}: base effectiveDiff={}, weaponBonus=+{}, adjusted={}{}{}",
                                        targetLocation.getTitle(), effectiveDiff, weaponBonus, weaponEffectiveDiff,
                                        ourVaderHere ? " [VADER]" : "", hasIHYN ? " [IHYN]" : "");
                                }

                                // === V29.9: REBEL BARRIER RISK ASSESSMENT ===
                                // If opponent might have Rebel Barrier, they can EXCLUDE our strongest
                                // character from battle. If we initiate with Vader + Tarkin vs opponents,
                                // and they Barrier Vader, suddenly Tarkin fights ALONE vs everyone.
                                // When our strength is concentrated in one key character (Vader),
                                // initiating battle is very risky because Barrier negates that character.
                                float barrierRiskPenalty = 0;
                                if (ourVaderHere && ourPower > 0 && theirPower > 0 && cardsHere != null) {
                                    // Calculate power WITHOUT Vader to see what happens if he's Barriered
                                    float powerWithoutVader = 0;
                                    int charCountWithoutVader = 0;
                                    for (PhysicalCard locCard : cardsHere) {
                                        if (locCard == null || locCard.getBlueprint() == null) continue;
                                        if (locCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                        if (!playerId.equals(locCard.getOwner())) continue;
                                        if (Filters.Vader.accepts(
                                                game, locCard)) {
                                            continue;
                                        }
                                        Float pw = locCard.getBlueprint().getPower();
                                        powerWithoutVader += (pw != null ? pw : 0);
                                        charCountWithoutVader++;
                                    }

                                    float powerDeficitWithoutVader = theirPower - powerWithoutVader;
                                    ObjectiveAnalyzer expendAnalyzer = context.getObjectiveAnalyzer();
                                    boolean requiredObjectiveVader =
                                        expendAnalyzer != null
                                        && ourVaderCard != null
                                        && (expendAnalyzer
                                                .classifyGateFormationPieceIfRemoved(
                                                    game, playerId,
                                                    ourVaderCard)
                                                == ObjectiveAnalyzer
                                                    .FlipGateFormationRole
                                                    .LAST_REQUIRED_ACTOR
                                            || expendAnalyzer
                                                .classifyGateFormationPieceIfRemoved(
                                                    game, playerId,
                                                    ourVaderCard)
                                                == ObjectiveAnalyzer
                                                    .FlipGateFormationRole
                                                    .LAST_REQUIRED_ON_TABLE_ACTOR);
                                    boolean huntDownV = expendAnalyzer != null
                                        && expendAnalyzer.isAnalyzed()
                                        && expendAnalyzer.isHuntDownV()
                                        && !requiredObjectiveVader;
                                    BattleInitiationPolicy.Contribution barrierRisk =
                                        BattleInitiationPolicy.barrierRisk(
                                            ourVaderHere,
                                            ourPower,
                                            theirPower,
                                            powerWithoutVader,
                                            charCountWithoutVader,
                                            huntDownV,
                                            context.getVaderExpendabilityFactor());
                                    if (barrierRisk.applies()) {
                                        barrierRiskPenalty = barrierRisk.delta();
                                        if (huntDownV) {
                                            logger.warn("V35 VADER EXPENDABLE: Barrier risk reduced to {} (Hunt Down — Vader is replaceable)",
                                                (int)barrierRiskPenalty);
                                        }
                                        action.apply(barrierRisk);
                                        logger.warn("V29.9 BARRIER RISK at {}: Without Vader: {} vs {} (deficit {}), penalty {}",
                                            targetLocation.getTitle(), (int)powerWithoutVader, (int)theirPower,
                                            (int)powerDeficitWithoutVader, (int)barrierRiskPenalty);
                                    }
                                }

                                // === V29.9: HUNT DOWN VADER BATTLE AGGRESSIVENESS ===
                                // When playing Hunt Down with armed Vader, he SHOULD be fighting.
                                // Boost battle initiation significantly when Vader is armed and present.
                                if (ourVaderHere && ourVaderArmed) {
                                    ObjectiveAnalyzer battleObjAnalyzer = context.getObjectiveAnalyzer();
                                    boolean huntDownV = battleObjAnalyzer != null
                                        && battleObjAnalyzer.isAnalyzed()
                                        && battleObjAnalyzer.isHuntDownV();
                                    BattleInitiationPolicy.Contribution huntAggression =
                                        BattleInitiationPolicy.huntAggression(
                                            ourVaderHere, ourVaderArmed,
                                            huntDownV, lukeHere);
                                    if (huntAggression.applies()) {
                                        action.apply(huntAggression);
                                        logger.warn("V29.9 HUNT DOWN: Armed Vader aggressiveness boost +{} (Luke: {})",
                                            (int)huntAggression.delta(), lukeHere);
                                    }
                                }

                                // === V35: INQUISITOR BATTLE DESTINY BONUS ===
                                // Hunt Down V objective gives +1 total battle destiny where you have
                                // an Inquisitor (+2 if hatred card present). This is like 1-2 extra
                                // destiny draws — massive advantage. Also check for Jedi opponents.
                                {
                                    ObjectiveAnalyzer v35ObjAnalyzer = context.getObjectiveAnalyzer();
                                    if (v35ObjAnalyzer != null
                                        && v35ObjAnalyzer.isAnalyzed()
                                        && v35ObjAnalyzer.isVirtualHuntDownObjective()
                                        && cardsHere != null) {
                                        boolean inquisitorInBattle = false;
                                        boolean hatredAtLocation = false;
                                        boolean jediAtLocation = false;

                                        for (PhysicalCard bCard : cardsHere) {
                                            if (bCard == null || bCard.getBlueprint() == null) continue;
                                            String bTitle = bCard.getTitle() != null ? bCard.getTitle().toLowerCase(Locale.ROOT) : "";

                                            if (playerId.equals(bCard.getOwner())) {
                                                if (Filters.inquisitor.accepts(game, bCard)) {
                                                    inquisitorInBattle = true;
                                                    if (Filters.hasStacked(Filters.hatredCard).accepts(game, bCard)) {
                                                        hatredAtLocation = true;
                                                    }
                                                }
                                            } else {
                                                // Opponent characters — check for Jedi/Padawan.
                                                if (isJediOrPadawan(bTitle)) {
                                                    jediAtLocation = true;
                                                }
                                            }
                                        }

                                        BattleInitiationPolicy.Contribution inquisitorDestiny =
                                            BattleInitiationPolicy.inquisitorDestiny(
                                                true,
                                                inquisitorInBattle,
                                                hatredAtLocation,
                                                jediAtLocation);
                                        if (inquisitorDestiny.applies()) {
                                            action.apply(inquisitorDestiny);
                                            logger.warn("V35 HUNT DESTINY at {}: Inquisitor={}, hatred={}, jedi={} — bonus +{}",
                                                targetLocation.getTitle(), inquisitorInBattle, hatredAtLocation,
                                                jediAtLocation, (int)inquisitorDestiny.delta());
                                        }
                                    }
                                }

                                if (ourPower > 0 && theirPower > 0) {
                                    foundAnyContestedLocation = true;
                                    boolean formationSafetyVeto = false;
                                    boolean predictorSafe = false;

                                    // FORMATION SAFETY (2026-07-11c): L2 — never voluntarily battle with
                                    // zero normal battle-destiny draws (engine truth: total ability >= 4;
                                    // Codex audit: First Light ability 3 initiated vs Falcon+Han, drew
                                    // nothing, auto-lost; V164a's parity model was wrong).
                                    String fsL2 = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                        .vetoInitiateBattle(game, gameState, playerId, targetLocation);
                                    if (fsL2 != null) {
                                        formationSafetyVeto = true;
                                        action.hardVeto(fsL2);
                                        logger.warn("FORMATION SAFETY (battle): {}", fsL2);
                                    }

                                    // === V76 (Steve, 2026-05-15): BATTLE PREDICTION GATE ===
                                    // Use the Monte Carlo BattlePredictor BEFORE the power-tier
                                    // scoring. If the simulation projects bad outcomes:
                                    //   - winRate < 35% → hard block (probable defeat)
                                    //   - avgDamageTaken >= 10 → hard block (even if winning, too costly)
                                    // Otherwise, fall through to the V22.4/V29.7 power scoring.
                                    //
                                    // Replay May 15: Rando initiated battle at Lars Farm and took
                                    // 13 attrition + 23 battle damage. Raw power comparison passed
                                    // CRUSH/FAVORABLE; destiny variance crushed him. BattlePredictor
                                    // exists with 311 lines of simulation logic but was never
                                    // wired into BattleEvaluator. This wiring closes the gap.
                                    try {
                                        // Estimate destiny draws per side: count chars with ability >= 1
                                        // at the location, capped between 1 and 4 (typical SWCCG range).
                                        int myDraws = 1, oppDraws = 1;
                                        try {
                                            int myCh = 0, oppCh = 0;
                                            for (PhysicalCard c : cardsHere) {
                                                if (c == null || c.getBlueprint() == null) continue;
                                                if (c.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                                if (playerId.equals(c.getOwner())) myCh++;
                                                else if (opponentId.equals(c.getOwner())) oppCh++;
                                            }
                                            myDraws = Math.max(1, Math.min(4, myCh));
                                            oppDraws = Math.max(1, Math.min(4, oppCh));
                                        } catch (Exception e) { /* use defaults */ }

                                        // V76 ADJUSTED 2026-07-10: opponent side weapon-adjusted (was raw).
                                        Prediction v76Outcome = context.predictBattle(
                                            (int) (ourPower + weaponBonus), myDraws,
                                            (int) (theirPower + oppWeaponBonus), oppDraws);

                                        logger.warn("V76 BATTLE PREDICT at {}: winRate={} avgDamageTaken={} avgDamageDealt={} (myPow={}+wb{} draws={} vs oppPow={} draws={})",
                                            targetLocation.getTitle(),
                                            String.format("%.2f", v76Outcome.winProbability),
                                            String.format("%.1f", v76Outcome.expectedDamageTaken),
                                            String.format("%.1f", v76Outcome.expectedDamageDealt),
                                            (int) ourPower, (int) weaponBonus, myDraws,
                                            (int) theirPower, oppDraws);

                                        BattleInitiationPolicy.PredictionDecision prediction =
                                            BattleInitiationPolicy.prediction(
                                                targetLocation.getTitle(),
                                                v76Outcome.winProbability,
                                                v76Outcome.expectedDamageTaken);
                                        predictorSafe = prediction.branch()
                                                == BattleInitiationPolicy.PredictionBranch.NONE;
                                        action.apply(prediction.contribution());
                                        if (prediction.branch()
                                            == BattleInitiationPolicy.PredictionBranch.PROBABLE_DEFEAT) {
                                            logger.warn("V76 BATTLE BLOCK: predicted defeat at {} (winRate {})",
                                                targetLocation.getTitle(), String.format("%.2f", v76Outcome.winProbability));
                                        } else if (prediction.branch()
                                                == BattleInitiationPolicy.PredictionBranch.PYRRHIC) {
                                            logger.warn("V76 BATTLE COSTLY: predicted damage {} at {} — too high",
                                                String.format("%.1f", v76Outcome.expectedDamageTaken), targetLocation.getTitle());
                                        }
                                    } catch (Exception v76Ex) {
                                        logger.debug("V76 prediction error: {}", v76Ex.getMessage());
                                    }

                                    BattleInitiationPolicy.SpecificBattleDecision specificBattle =
                                        BattleInitiationPolicy.specificBattle(
                                            targetLocation.getTitle(),
                                            ourPower,
                                            theirPower,
                                            ourAbility,
                                            theirAbility,
                                            weaponBonus,
                                            weaponEffectiveDiff,
                                            ourVaderHere,
                                            lukeHere,
                                            hasIHYN);
                                    action.apply(specificBattle.contribution());

                                    boolean exactStructuredPreFlipTarget = false;
                                    boolean missingSelfControl = false;
                                    boolean requiredCardControlEnabler = false;
                                    boolean requiredCardRetention = false;
                                    boolean hardLossLocation = false;
                                    boolean terminalObjectiveHazard = false;
                                    boolean globalObjectiveBlocker = false;
                                    int objectiveMoveForceReserve = 0;
                                    int availableObjectiveMoveForce = 0;
                                    float battleInitiationCost = 0.0f;
                                    try {
                                        ObjectiveAnalyzer objectiveAnalyzer =
                                                context.getObjectiveAnalyzer();
                                        objectiveMoveForceReserve =
                                                objectiveAnalyzer != null
                                                ? objectiveAnalyzer
                                                    .getFirstOrderReignsCurrentMoveForceReserve(
                                                        game, playerId)
                                                : 0;
                                        availableObjectiveMoveForce =
                                                gameState.getForcePileSize(
                                                    playerId);
                                        battleInitiationCost =
                                                game.getModifiersQuerying()
                                                    .getInitiateBattleCost(
                                                        gameState,
                                                        targetLocation,
                                                        playerId, true);
                                        globalObjectiveBlocker =
                                                objectiveAnalyzer != null
                                                && objectiveAnalyzer
                                                    .isPreFlipBattleRemovableGlobalBlockerAt(
                                                        game, playerId,
                                                        targetLocation);
                                        exactStructuredPreFlipTarget =
                                                objectiveAnalyzer != null
                                                && objectiveAnalyzer
                                                    .isMissingPreFlipRequirementAt(
                                                        game, playerId,
                                                        targetLocation)
                                                && !globalObjectiveBlocker;
                                        requiredCardControlEnabler =
                                                objectiveAnalyzer != null
                                                && (objectiveAnalyzer
                                                        .isActiveRequiredCardControlEnablerLocation(
                                                            game, playerId,
                                                            targetLocation)
                                                    || objectiveAnalyzer
                                                        .isMissingRequiredCardDeployEnablerAt(
                                                            game, playerId,
                                                            targetLocation));
                                        requiredCardRetention =
                                                objectiveAnalyzer != null
                                                && objectiveAnalyzer
                                                    .isRequiredCardRetentionBattleLocation(
                                                        game, playerId,
                                                        targetLocation);
                                        hardLossLocation =
                                                objectiveAnalyzer != null
                                                && objectiveAnalyzer
                                                    .isObjectiveHardLossDefenseLocation(
                                                        game, playerId,
                                                        targetLocation);
                                        terminalObjectiveHazard =
                                                objectiveAnalyzer != null
                                                && objectiveAnalyzer
                                                    .isFirstOrderReignsTerminalBattleHazardAt(
                                                        game, playerId,
                                                        targetLocation);
                                        if (exactStructuredPreFlipTarget
                                                || requiredCardControlEnabler
                                                || requiredCardRetention
                                                || hardLossLocation) {
                                            boolean ordinaryControl =
                                                objectiveAnalyzer != null
                                                && objectiveAnalyzer
                                                    .preFlipRequirementUsesOrdinaryControl(
                                                        game, playerId,
                                                        targetLocation);
                                            missingSelfControl = ordinaryControl
                                                ? !game.getModifiersQuerying()
                                                    .controlsLocation(
                                                        gameState,
                                                        targetLocation,
                                                        playerId)
                                                : !game.getModifiersQuerying()
                                                    .controlsLocation(
                                                        gameState,
                                                        targetLocation,
                                                        playerId,
                                                        SpotOverride
                                                            .INCLUDE_EXCLUDED_FROM_BATTLE);
                                        }
                                    } catch (Exception objectiveBattleEx) {
                                        logger.debug(
                                            "Objective battle fact read failed: {}",
                                            objectiveBattleEx.getMessage());
                                    }
                                    action.apply(ObjectiveBattlePolicy
                                        .evaluateTerminalObjectiveHazard(
                                            actionId,
                                            terminalObjectiveHazard,
                                            predictorSafe,
                                            weaponEffectiveDiff));
                                    action.apply(ObjectiveBattlePolicy
                                        .preserveObjectiveMoveForce(
                                            actionId,
                                            objectiveMoveForceReserve,
                                            availableObjectiveMoveForce,
                                            battleInitiationCost));
                                    action.apply(ObjectiveBattlePolicy.evaluate(
                                        new ObjectiveBattlePolicy.Facts(
                                            actionId,
                                            exactStructuredPreFlipTarget,
                                            missingSelfControl,
                                            requiredCardControlEnabler,
                                            requiredCardRetention,
                                            hardLossLocation,
                                            globalObjectiveBlocker,
                                            true,
                                            formationSafetyVeto,
                                            predictorSafe,
                                            weaponEffectiveDiff,
                                            reserveDeck,
                                            ourPower,
                                            theirPower)));

                                    if (specificBattle.favorable()) {
                                        foundFavorableBattle = true;
                                    }
                                    if (specificBattle.branch()
                                            == BattleInitiationPolicy.SpecificBattleBranch.OUTGUNNED) {
                                        logger.warn("V29 BATTLE BLOCK at {}: our {} vs their {} — BLOCKED (penalty {})",
                                            targetLocation.getTitle(), (int)ourPower, (int)theirPower,
                                            (int)specificBattle.contribution().delta());
                                    }
                                } else if (ourPower > 0 && theirPower == 0) {
                                    BattleInitiationPolicy.SpecificBattleDecision specificBattle =
                                        BattleInitiationPolicy.specificBattle(
                                            targetLocation.getTitle(),
                                            ourPower,
                                            theirPower,
                                            ourAbility,
                                            theirAbility,
                                            weaponBonus,
                                            weaponEffectiveDiff,
                                            ourVaderHere,
                                            lukeHere,
                                            hasIHYN);
                                    action.apply(specificBattle.contribution());
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

                                        // V76 (V22.4 fallback) ADJUSTED 2026-07-10 (Rey replay
                                        // rbujmoc90br3uu4c, T5: "Initiate battle for free" had no
                                        // location data, skipped the specific branch — the ONLY place
                                        // V76 ran — and got "+40 Favorable 19v13" while 2-3 armed
                                        // opponents hit Ben Solo + Yoda + 3 sabers: won the power, lost
                                        // 6 cards to 2). The fallback now (a) counts opponent WEAPONS
                                        // into the power math (V29.7 heuristic) and (b) prices the HIT
                                        // economics: each armed opponent character ≈ one of our
                                        // characters forfeited at 0 (avg forfeit). Ruinous economics →
                                        // -500 pyrrhic (same bar as V76's specific branch) and never
                                        // "favorable".
                                        float v76fOppWeap = 0f; int v76fOppArmed = 0;
                                        int v76fOurChars = 0; float v76fOurForfeit = 0f;
                                        try {
                                            for (PhysicalCard fc : gameState.getCardsAtLocation(location)) {
                                                if (fc == null || fc.getBlueprint() == null) continue;
                                                if (fc.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                                if (playerId.equals(fc.getOwner())) {
                                                    v76fOurChars++;
                                                    Float ff = fc.getBlueprint().hasForfeitAttribute() ? fc.getBlueprint().getForfeit() : null;
                                                    v76fOurForfeit += (ff != null) ? ff : 2f;
                                                } else if (opponentId != null && opponentId.equals(fc.getOwner())) {
                                                    BattleWeaponProfile fProfile = BattleWeaponProfile.assess(game, gameState, fc);
                                                    v76fOppWeap += fProfile.bonus();
                                                    if (fProfile.armed()) v76fOppArmed++;
                                                }
                                            }
                                        } catch (Exception e) { /* fail-open */ }
                                        float v76fHitLoss = 0f;
                                        if (v76fOppArmed > 0 && v76fOurChars > 0) {
                                            float v76fAvgF = v76fOurForfeit / v76fOurChars;
                                            v76fHitLoss = Math.min(v76fOppArmed, v76fOurChars) * v76fAvgF;
                                        }
                                        effectiveDiff -= v76fOppWeap;
                                        // ADJUSTED 2026-07-10b (replay f27ws5lgy0g58k5p, T4: hitLoss 6
                                        // didn't trip the flat >=10 bar while Rando's WHOLE force was worth
                                        // 6): pyrrhic is also RELATIVE — losing more than half your committed
                                        // forfeit to hits is ruinous regardless of absolute size.
                                        boolean v76fPyrrhic = v76fHitLoss >= 10f
                                            || (v76fOurForfeit > 0f && v76fHitLoss > 0.5f * v76fOurForfeit);
                                        BattleInitiationPolicy.FallbackDecision fallback =
                                            BattleInitiationPolicy.fallbackLocation(
                                                location.getTitle(),
                                                ourPower,
                                                theirPower,
                                                ourAbility,
                                                theirAbility,
                                                effectiveDiff,
                                                v76fOppArmed,
                                                v76fHitLoss,
                                                v76fPyrrhic);
                                        int fallbackContributionIndex = 0;
                                        if (v76fPyrrhic) {
                                            action.apply(fallback.contributions().get(fallbackContributionIndex++));
                                            logger.warn("V76 FALLBACK PYRRHIC at {}: armed={} hitLoss={} oppWeap=+{}",
                                                location.getTitle(), v76fOppArmed, v76fHitLoss, v76fOppWeap);
                                        }

                                        logger.info("[BattleEvaluator] Checking {}: power={}/{} (diff={}), ability={}/{} (diff={}), oppWeap=+{}",
                                            location.getTitle(), ourPower, theirPower, powerDiff,
                                            ourAbility, theirAbility, abilityDiff, v76fOppWeap);

                                        while (fallbackContributionIndex < fallback.contributions().size()) {
                                            action.apply(fallback.contributions().get(fallbackContributionIndex++));
                                        }

                                        if (fallback.favorable()) {
                                            foundFavorableBattle = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("[BattleEvaluator] Could not check locations: {}", e.getMessage());
                        }
                    }
                }

                // FORMATION SAFETY (2026-07-11c): L2 on the LOCATIONLESS fallback route — if no
                // contested location gives us a normal battle-destiny draw, initiating is a
                // guaranteed destiny deficit; veto (the specific branch handles named locations).
                if (!checkedSpecificLocation && foundAnyContestedLocation && game != null && gameState != null) {
                    boolean fsAnyEligible = false;
                    try {
                        String fsPid = context.getPlayerId();
                        String fsOpp = gameState.getOpponent(fsPid);
                        for (PhysicalCard fsLoc : gameState.getTopLocations()) {
                            if (fsLoc == null) continue;
                            float fsOur = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, fsLoc, fsPid, false, false);
                            float fsTheir = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, fsLoc, fsOpp, false, false);
                            if (fsOur <= 0 || fsTheir <= 0) continue;
                            if (com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                    .battleDestinyEligible(game, gameState, fsPid, fsLoc)) { fsAnyEligible = true; break; }
                        }
                    } catch (Exception fsE) { fsAnyEligible = true; /* fail-open */ }
                    if (!fsAnyEligible) {
                        action.hardVeto("L2 NO-DESTINY BATTLE (fallback): no contested location reaches ability 4 — zero normal destiny draws everywhere");
                        logger.warn("FORMATION SAFETY (battle-fallback): vetoed — no destiny-eligible contested location");
                    }
                }

                action.apply(BattleInitiationPolicy.scanOutcome(
                    foundFavorableBattle,
                    foundAnyContestedLocation));

                // V22: STRATEGIC MUST-FIGHT OVERRIDE
                // If opponent is draining us from multiple uncontested locations and we're behind
                // on life force, inaction guarantees defeat. Force engagement.
                if (!foundFavorableBattle && game != null && gameState != null) {
                    try {
                        String playerId = context.getPlayerId();
                        String opponentId = gameState.getOpponent(playerId);
                        int theirDrain = 0;
                        for (PhysicalCard location : gameState.getTopLocations()) {
                            float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, location, playerId, false, false);
                            float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, location, opponentId, false, false);
                            if (theirPower > 0 && ourPower == 0) {
                                theirDrain += 1;
                            }
                        }
                        // V29: Only apply MUST-FIGHT if we have at least one location where
                        // we're NOT outpowered. Don't force a battle with solo Lando vs Rey.
                        boolean hasWinnableBattle = false;
                        for (PhysicalCard location : gameState.getTopLocations()) {
                            float ourP = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, location, playerId, false, false);
                            float theirP = game.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, location, opponentId, false, false);
                            if (ourP > 0 && theirP > 0 && ourP >= theirP) {
                                hasWinnableBattle = true;
                                break;
                            }
                        }
                        BattleInitiationPolicy.Contribution mustFight =
                            BattleInitiationPolicy.mustFight(
                                theirDrain,
                                hasWinnableBattle,
                                isBehindOnLifeForce);
                        action.apply(mustFight);
                        if (mustFight.applies()) {
                            logger.warn("[BattleEvaluator] V22 MUST-FIGHT override: drain threat={}, behind=true", theirDrain);
                        } else if (theirDrain >= 2 && !hasWinnableBattle && isBehindOnLifeForce) {
                            logger.warn("V29 MUST-FIGHT BLOCKED: Behind on life but outpowered everywhere — don't suicide!");
                        }
                    } catch (Exception e) {
                        logger.debug("[BattleEvaluator] V22 must-fight check failed: {}", e.getMessage());
                    }
                }

                // V61 RESERVE DECK GUARD — battle destiny needs Reserve Deck cards!
                // FIXES is9j46shx6t0swby replay: Rando initiated battle with Reserve=0
                // (log: "No cards in Reserve Deck. Rando can't draw battle destiny")
                // auto-losing every destiny draw. This is a hard auto-lose trap.
                // Scale penalty to severity — 0 cards = hard block, 1-2 = heavy penalty.
                // V61b (Steve, 2026-06-28): UNLESS we OVERPOWER a contested site by a lot — then we
                // win on raw power and battle destiny is irrelevant, so the empty-reserve guard must
                // NOT veto a free win (replay: Hoth 26 vs 4, diff 22, V61 -800 blocked it and Rando
                // passed). Re-scan contested sites for the best overpower margin; if it clears a
                // strong opponent destiny swing, skip the reserve penalty entirely and take the battle.
                float v61BestOverpower = 0f;
                try {
                    if (selectedBattlePowerMargin != null) {
                        v61BestOverpower = selectedBattlePowerMargin;
                    } else {
                        String v61pid = context.getPlayerId();
                        String v61oid = gameState.getOpponent(v61pid);
                        for (PhysicalCard v61loc : gameState.getTopLocations()) {
                            float op = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, v61loc, v61pid, false, false);
                            float tp = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, v61loc, v61oid, false, false);
                            if (op > 0 && tp > 0 && (op - tp) > v61BestOverpower) v61BestOverpower = op - tp;
                        }
                    }
                } catch (Exception ignore) { /* 0 */ }
                BattleInitiationPolicy.ReserveDecision reserveDecision =
                    BattleInitiationPolicy.reserve(v61BestOverpower, reserveDeck);
                action.apply(reserveDecision.contribution());
                if (reserveDecision.branch() == BattleInitiationPolicy.ReserveBranch.OVERPOWER) {
                    logger.warn("V61b OVERPOWER: target margin {} >= 8 — reserve guard skipped, take the battle", (int) v61BestOverpower);
                } else if (reserveDecision.branch() == BattleInitiationPolicy.ReserveBranch.EMPTY) {
                    logger.warn("V61 RESERVE EMPTY: Blocking battle initiation — Reserve=0!");
                } else if (reserveDecision.branch() == BattleInitiationPolicy.ReserveBranch.CRITICAL) {
                    logger.warn("V61 RESERVE CRITICAL: 1 card in reserve — heavy penalty -400");
                }

                // === V27: BATTLE INTERRUPT FORCE RESERVATION ===
                // If opponent has "Draw Their Fire" on table, playing ANY interrupt
                // during battles THEY initiate costs 1 extra Force. This means Ghhhk
                // (Used Interrupt, normally free to play) needs 1 Force just from the tax.
                // Without Force in pile, ALL battle interrupts are unusable and we take
                // full attrition from heavy losses.
                // Also applies when WE initiate: defender (us) still loses 1 Force when
                // battle is initiated, and if opponent initiates, we need extra Force per interrupt.
                if (gameState != null) {
                    int battleForcePile = context.getForcePileSize();
                    int handSize = context.getHandSize();

                    // V27.1: Detect "Draw Their Fire" on opponent's table
                    boolean opponentHasDrawTheirFire = false;
                    try {
                        String opponentIdDtf = gameState.getOpponent(context.getPlayerId());
                        for (PhysicalCard dtfCard : gameState.getAllPermanentCards()) {
                            if (dtfCard == null) continue;
                            if (opponentIdDtf != null && opponentIdDtf.equals(dtfCard.getOwner())
                                && dtfCard.getBlueprint() != null
                                && dtfCard.getBlueprint().getTitle() != null) {
                                String dtfTitle = dtfCard.getBlueprint().getTitle().toLowerCase(Locale.ROOT);
                                if (dtfTitle.contains("draw their fire")) {
                                    com.gempukku.swccgo.common.Zone dtfZone = dtfCard.getZone();
                                    if (dtfZone != null && dtfZone.isInPlay()) {
                                        opponentHasDrawTheirFire = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V27.1: Error checking for Draw Their Fire: {}", e.getMessage());
                    }

                    BattleInitiationPolicy.InterruptDecision interruptDecision =
                        BattleInitiationPolicy.interruptForce(
                            opponentHasDrawTheirFire,
                            battleForcePile,
                            handSize);
                    action.apply(interruptDecision.contribution());
                    if (interruptDecision.branch() == BattleInitiationPolicy.InterruptBranch.DTF_BLOCKED) {
                        logger.warn("V27.1 DTF ACTIVE: Only {} Force, need {} for interrupt tax — battle interrupts blocked!",
                            battleForcePile, 3);
                    } else if (interruptDecision.branch()
                            == BattleInitiationPolicy.InterruptBranch.STANDARD_CRITICAL) {
                        logger.warn("V27 BATTLE FORCE: Only {} Force available — battle interrupts may be unusable!", battleForcePile);
                    }
                }

                action.apply(BattleInitiationPolicy.lifeForce(
                    isBehindOnLifeForce,
                    isAheadOnLifeForce,
                    lifeForce,
                    context.getCriticalLifeForce()));
            }

            BattleWeaponsFacts.CancelBattleFacts cancelBattle =
                BattleWeaponsFacts.CancelBattleFacts.none();
            if (actionLower.contains("cancel battle") || actionLower.contains("cancel the battle")) {
                SwccgGame game = context.getGame();
                if (game != null && gameState != null) {
                    BattleState battleState = gameState.getBattleState();
                    String playerId = context.getPlayerId();

                    if (battleState != null) {
                        String initiator = battleState.getPlayerInitiatedBattle();
                        boolean weInitiated = playerId != null && playerId.equals(initiator);

                        if (weInitiated) {
                            cancelBattle = BattleWeaponsFacts.CancelBattleFacts.ownInitiated();
                            logger.info("[BattleEvaluator] Penalizing cancel - WE initiated this battle");
                        } else {
                            String opponentId = gameState.getOpponent(playerId);
                            PhysicalCard battleLocation = battleState.getBattleLocation();

                            if (battleLocation != null && opponentId != null) {
                                try {
                                    float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, battleLocation, playerId, false, false);
                                    float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, battleLocation, opponentId, false, false);
                                    cancelBattle = BattleWeaponsFacts.CancelBattleFacts
                                        .opponentInitiated(ourPower, theirPower);
                                } catch (Exception e) {
                                    logger.warn("[BattleEvaluator] Could not get power for cancel decision: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            action.apply(BattleWeaponsPolicy.scoreBattleEvaluator(
                new BattleWeaponsFacts.BattleEvaluatorFacts(
                    actionId,
                    actionLower.contains("fire"),
                    actionLower.contains("character"),
                    actionLower.contains("unique") || actionLower.contains("•"),
                    cancelBattle,
                    context.getPhase() == Phase.BATTLE,
                    actionLower.contains("draw") && actionLower.contains("destiny"))));

            logger.debug("[BattleEvaluator] Scored '{}' -> {}",
                actionText.length() > 40 ? actionText.substring(0, 40) + "..." : actionText,
                String.format("%.1f", action.getScore()));

            actions.add(action.result());
        }

        logger.info("[BattleEvaluator] Evaluated {} battle actions", actions.size());
        return actions;
    }

    private static boolean isJediOrPadawan(String titleLower) {
        return titleLower.contains("jedi")
            || titleLower.contains("padawan")
            || titleLower.contains("luke")
            || titleLower.contains("obi-wan")
            || titleLower.contains("yoda")
            || titleLower.contains("ahsoka")
            || titleLower.contains("ezra")
            || titleLower.contains("kanan")
            || titleLower.contains("rey")
            || titleLower.contains("sabine");
    }

}
