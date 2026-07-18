package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Canonical ordered scoring owner for top-level DRAW actions. */
public final class DrawPhasePolicy {
    private static final float GOOD_DELTA = 10.0f;
    private static final float BAD_DELTA = -10.0f;
    private static final float VERY_BAD_DELTA = -150.0f;
    private static final int TARGET_HAND_SIZE = 7;
    private static final int LOW_RESERVE_THRESHOLD = 6;
    private static final int SMALL_HAND_THRESHOLD = 5;
    private static final int AGGRESSIVE_FORCE_THRESHOLD = 10;
    private static final int LATE_GAME_LIFE_FORCE = 12;
    private static final int CRITICAL_LIFE_FORCE = 6;
    private static final int HOLD_BACK_DRAW_FORCE_THRESHOLD = 6;
    private static final int HOLD_BACK_DRAW_LIFE_THRESHOLD = 10;
    private static final int HOLD_BACK_DRAW_FORCE_FLOOR = 6;
    private static final int FORCE_STARVED_ACTIVATION = 8;
    private static final int FORCE_STARVED_POWER_THRESHOLD = 6;
    private static final int FORCE_STARVED_MAX_HAND = 8;

    private DrawPhasePolicy() {
    }

    public interface Facts {
        boolean hasBoardState();
        int handSize();
        int reserveDeckSize();
        int usedPileSize();
        int forcePileSize();
        int turnNumber();
        int maxHandSize();
        int handSoftCap();
        int maintenanceObligation();
        int forceGeneration();
        int offensiveBank(int forcePile, int forceGeneration);
        HoldBack holdBack();
        DrawPhaseFactsReader.ExpensiveCards expensiveCards(int forcePile);
        DrawPhaseFactsReader.ForceStarved forceStarved();
        boolean piettNeedsDig();
        int forceToReserve();
    }

    public record HoldBack(boolean active, String reason) {
        public HoldBack {
            reason = reason == null ? "" : reason;
        }

        public static HoldBack none() {
            return new HoldBack(false, "");
        }
    }

    public static PolicyResult assess(String actionId, String actionText, boolean blocked,
                                      Facts facts, Logger logger) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(actionText, "actionText");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(logger, "logger");
        List<PolicyOperation> operations = new ArrayList<>();

        if (blocked) {
            add(operations, actionId, "V167-draw-soft", TraceDomainId.LOOP_SAFETY,
                    TraceOutputKind.BANDED, -200.0f,
                    "BLOCKED (loop prevention) — soft (V167: draws never hard-vetoed)");
            logger.warn("V167: soft-block (not hard veto) on draw action: {}", actionText);
        }

        if (!facts.hasBoardState()) {
            add(operations, actionId, "DRAW-no-board", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, 0.0f,
                    "No board state - neutral draw");
            return result(operations);
        }

        int handSize = facts.handSize();
        int reserveDeck = facts.reserveDeckSize();
        int usedPile = facts.usedPileSize();
        int forcePile = facts.forcePileSize();
        int turnNumber = facts.turnNumber();
        int remainingLifeForce = reserveDeck + usedPile + forcePile;
        int forceGeneration = facts.forceGeneration();

        int maintenanceFloor = facts.maintenanceObligation();
        if (maintenanceFloor > 0 && forcePile <= maintenanceFloor && handSize > 1) {
            add(operations, actionId, "V58-maintenance-floor", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, VERY_BAD_DELTA,
                    "V58 MAINTENANCE FLOOR (hard): force pile " + forcePile
                            + " <= upkeep " + maintenanceFloor
                            + " — hold it or a maintenance card is sacrificed at end of turn");
            logger.warn("V58 MAINTENANCE FLOOR (hard): forcePile={} <= maintObligation={}, hand={} — suppress draw, PASS wins",
                    forcePile, maintenanceFloor, handSize);
            return result(operations);
        }

        if (handSize <= 2 && forcePile >= 1 && reserveDeck >= 2) {
            float emergencyBonus = (3 - handSize) * 200.0f;
            add(operations, actionId, "V42-draw", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, emergencyBonus,
                    String.format("V42 EMERGENCY DRAW: Hand has only %d cards — MUST draw to stay in the game!",
                            handSize));
            logger.warn("V42 EMERGENCY DRAW: hand={}, force={}, reserve={} — bonus +{}",
                    handSize, forcePile, reserveDeck, emergencyBonus);
        }

        if (remainingLifeForce < CRITICAL_LIFE_FORCE) {
            float penalty = VERY_BAD_DELTA * 0.8f;
            add(operations, actionId, "DRAW-critical-life", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, penalty,
                    "CRITICAL life force (" + remainingLifeForce + ") - minimize draws");
            if (handSize >= 2) {
                return result(operations);
            }
        }

        if (remainingLifeForce < LATE_GAME_LIFE_FORCE) {
            float penaltyScale = (LATE_GAME_LIFE_FORCE - remainingLifeForce)
                    / (float) LATE_GAME_LIFE_FORCE;
            add(operations, actionId, "DRAW-late-life", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, BAD_DELTA * 2 * penaltyScale,
                    "Late game (" + remainingLifeForce + " life force) - draw carefully");
        }

        int effectiveMaxHand = facts.maxHandSize();
        if (remainingLifeForce < facts.maxHandSize()) {
            effectiveMaxHand = Math.max(2, remainingLifeForce);
            logger.trace("Life force {} < {}: effective max hand = {}",
                    remainingLifeForce, facts.maxHandSize(), effectiveMaxHand);
        }
        if (handSize >= effectiveMaxHand) {
            add(operations, actionId, "DRAW-hand-limit", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, VERY_BAD_DELTA,
                    "CRITICAL: Hand " + handSize + " >= life force limit " + effectiveMaxHand);
            return result(operations);
        }

        if (handSize >= 4) {
            int bank = facts.offensiveBank(forcePile, forceGeneration);
            if (bank > 0 && forcePile < bank) {
                add(operations, actionId, "V182", TraceDomainId.DRAW_COUNT,
                        TraceOutputKind.VETO, VERY_BAD_DELTA * 2f,
                        String.format("V182 BANK FORCE: hand has the army to win a contested fight (need %d force, have %d) — hold it, don't draw it away",
                                bank, forcePile));
                logger.warn("V182 BANK FORCE: need={} forcePile={} gen={} — suppress draw, bank for next-turn army",
                        bank, forcePile, forceGeneration);
                return result(operations);
            }
        }

        HoldBack holdBack = facts.holdBack();
        if (holdBack != null && holdBack.active()) {
            String reason = holdBack.reason().toLowerCase();
            boolean strategic = reason.contains("crush") || reason.contains("bleed")
                    || reason.contains("early game") || reason.contains("saving")
                    || reason.contains("next-turn");
            if (!strategic && forcePile > HOLD_BACK_DRAW_FORCE_THRESHOLD
                    && remainingLifeForce > HOLD_BACK_DRAW_LIFE_THRESHOLD
                    && handSize < facts.maxHandSize()) {
                int drawsAffordable = forcePile - HOLD_BACK_DRAW_FORCE_FLOOR;
                if (drawsAffordable > 0) {
                    float boost = 50.0f + drawsAffordable * 5;
                    String summary = reason.length() > 50
                            ? reason.substring(0, 50) + "..." : reason;
                    add(operations, actionId, "DRAW-hold-back", TraceDomainId.DRAW_COUNT,
                            TraceOutputKind.BANDED, boost,
                            "HOLD-BACK DRAW: Couldn't deploy (" + summary + "), force "
                                    + forcePile + " > " + HOLD_BACK_DRAW_FORCE_THRESHOLD
                                    + ", drawing to find options");
                    logger.info("🎴 HOLD-BACK DRAW boost: +{} (reason: {})", boost, summary);
                }
            }
        }

        DrawPhaseFactsReader.ExpensiveCards expensive = facts.expensiveCards(forcePile);
        if (expensive.expensiveCardInHand() && expensive.maxDeployableCost() > forcePile) {
            int deficit = expensive.maxDeployableCost() - forcePile;
            int turnsToSave = Math.max(1,
                    (deficit + forceGeneration - 1) / Math.max(1, forceGeneration));
            if (turnsToSave <= 3 && remainingLifeForce >= expensive.maxDeployableCost()) {
                add(operations, actionId, "DRAW-expensive-save", TraceDomainId.DRAW_COUNT,
                        TraceOutputKind.BANDED, BAD_DELTA * 2,
                        "Saving for expensive card (cost " + expensive.maxDeployableCost()
                                + ", need " + deficit + " more, ~" + turnsToSave + " turns)");
            }
        }
        if (expensive.affordableCardsCount() == 0 && expensive.handCardCount() > 3
                && forcePile < 6) {
            add(operations, actionId, "DRAW-no-affordable", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, BAD_DELTA * 1.5f,
                    "No affordable cards (hand " + expensive.handCardCount() + ", force "
                            + forcePile + ") - save force for next turn");
        }

        if (forceGeneration < FORCE_STARVED_ACTIVATION) {
            DrawPhaseFactsReader.ForceStarved starved = facts.forceStarved();
            if (starved.deployablePower() >= FORCE_STARVED_POWER_THRESHOLD) {
                int nextTurnForce = forcePile + forceGeneration;
                int forceNeeded = starved.minCostForThresholdPower() + 2;
                logger.info("🎴 FORCE-STARVED check: activation={}, deployable_power={}, min_cost={}, next_turn_force={}, need={}",
                        forceGeneration, starved.deployablePower(),
                        starved.minCostForThresholdPower(), nextTurnForce, forceNeeded);
                if (nextTurnForce < forceNeeded) {
                    int shortfall = forceNeeded - nextTurnForce;
                    add(operations, actionId, "DRAW-force-starved-shortfall",
                            TraceDomainId.DRAW_COUNT, TraceOutputKind.BANDED,
                            VERY_BAD_DELTA * 0.6f,
                            "FORCE-STARVED: Save force! (" + starved.deployablePower()
                                    + "p ready, need " + forceNeeded + " force, will have "
                                    + nextTurnForce + " → short " + shortfall + ")");
                    logger.warn("🎴 FORCE-STARVED: Stopping draw to save force for deployment");
                    if (handSize >= 6) {
                        add(operations, actionId, "DRAW-force-starved-hand",
                                TraceDomainId.DRAW_COUNT, TraceOutputKind.BANDED,
                                BAD_DELTA * 2,
                                "Already have " + handSize + " cards - more won't help without force");
                        return result(operations);
                    }
                }
                if (handSize >= FORCE_STARVED_MAX_HAND) {
                    add(operations, actionId, "DRAW-force-starved-cap",
                            TraceDomainId.DRAW_COUNT, TraceOutputKind.BANDED,
                            BAD_DELTA * 3,
                            "Force-starved (" + forceGeneration + "/turn): hand "
                                    + handSize + " is enough");
                }
            }
        }

        if (facts.piettNeedsDig()) {
            float piettBonus = turnNumber <= 2 ? 200.0f : turnNumber <= 4 ? 150.0f : 80.0f;
            add(operations, actionId, "V24.10-dig", TraceDomainId.DECK_PLAYBOOK,
                    TraceOutputKind.BANDED, piettBonus,
                    "V24.10 DIG FOR PIETT: Not in hand/reserve — must be in force pile! Draw to find him!");
            logger.warn("V24.10 PIETT DIG: Piett not in hand/reserve/play/lost — drawing aggressively to find him! (+{})",
                    piettBonus);
        }

        String phaseNote = turnNumber <= 3 ? "early" : turnNumber <= 6 ? "mid" : "late";
        int effectiveSoftCap = turnNumber <= 3 ? facts.handSoftCap() + 4
                : turnNumber <= 6 ? facts.handSoftCap() : facts.handSoftCap() - 4;
        if (handSize < effectiveSoftCap && remainingLifeForce >= LATE_GAME_LIFE_FORCE) {
            int cardsBelowCap = effectiveSoftCap - handSize;
            float baselineBonus = Math.max(30.0f, 8.0f * cardsBelowCap);
            add(operations, actionId, "DRAW-baseline", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, baselineBonus,
                    "Hand " + handSize + " below " + phaseNote + "-game cap "
                            + effectiveSoftCap + " - draw!");
            logger.debug("Draw baseline: hand {} < {} cap {}, +{}",
                    handSize, phaseNote, effectiveSoftCap, baselineBonus);
        }

        int forceToReserve = facts.forceToReserve();
        int drawableSurplus = Math.max(0, forcePile - forceToReserve);
        if (drawableSurplus > 0 && handSize < effectiveMaxHand && reserveDeck > 2) {
            float surplusBonus = Math.min(400.0f, 80.0f * drawableSurplus);
            add(operations, actionId, "V58-draw-down", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, surplusBonus,
                    String.format("V58 DRAW-DOWN: force pile %d > reserve %d — draw %d surplus into hand!",
                            forcePile, forceToReserve, drawableSurplus));
            logger.warn("V58 DRAW-DOWN: pile={}, reserve={}, surplus={} → +{}",
                    forcePile, forceToReserve, drawableSurplus, (int) surplusBonus);
        }
        if (forcePile <= forceToReserve && turnNumber >= 4) {
            add(operations, actionId, "V58-hold-reserve", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, BAD_DELTA * 1.5f,
                    "V58 HOLD RESERVE: force pile " + forcePile
                            + " at/below reserve target " + forceToReserve + " — keep it");
        }
        if (reserveDeck <= LOW_RESERVE_THRESHOLD) {
            add(operations, actionId, "DRAW-low-reserve", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED,
                    BAD_DELTA * (LOW_RESERVE_THRESHOLD - reserveDeck),
                    "Low reserve (" + reserveDeck + ") - avoid drawing");
        }
        if (handSize < TARGET_HAND_SIZE && reserveDeck > 10 && forcePile > 1
                && remainingLifeForce >= LATE_GAME_LIFE_FORCE) {
            add(operations, actionId, "DRAW-target-hand", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, GOOD_DELTA,
                    "Hand size " + handSize + " < " + TARGET_HAND_SIZE + " - draw to fill");
        }
        if (handSize <= SMALL_HAND_THRESHOLD && reserveDeck > 4 && forcePile > 1) {
            add(operations, actionId, "DRAW-small-hand", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, GOOD_DELTA,
                    "Small hand (" + handSize + ") - draw cards");
        }
        if (forcePile > AGGRESSIVE_FORCE_THRESHOLD
                && remainingLifeForce >= LATE_GAME_LIFE_FORCE) {
            add(operations, actionId, "DRAW-high-force", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, GOOD_DELTA,
                    "High force pile (" + forcePile + ") - YOLO draw");
        }
        if (forcePile > 5 && handSize <= 4) {
            add(operations, actionId, "DRAW-weak-hand", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, GOOD_DELTA,
                    "Weak hand - draw even on hold");
        }
        if (handSize >= effectiveSoftCap) {
            int overflow = handSize - effectiveSoftCap;
            add(operations, actionId, "DRAW-soft-cap", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, BAD_DELTA * overflow * 0.5f,
                    "Hand above " + phaseNote + "-game cap (" + handSize + "/"
                            + effectiveSoftCap + ")");
        }
        if (forcePile == 1) {
            add(operations, actionId, "DRAW-last-force", TraceDomainId.DRAW_COUNT,
                    TraceOutputKind.BANDED, BAD_DELTA,
                    "Last force - save it");
        }
        return result(operations);
    }

    private static void add(List<PolicyOperation> operations, String actionId,
                            String ruleId, TraceDomainId domainId,
                            TraceOutputKind outputKind, float delta, String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                domainId, outputKind, delta, reason));
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult("DRAW_PHASE_POLICY", operations);
    }
}
