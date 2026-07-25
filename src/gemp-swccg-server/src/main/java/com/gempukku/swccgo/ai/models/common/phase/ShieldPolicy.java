package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldFacts;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/** Shared SHIELDS decision tree for parent actions and shield candidates. */
public final class ShieldPolicy {

    public enum CandidateRoute {
        DEDICATED,
        RESERVE
    }

    public enum FourthSlotTrigger {
        CLOSED,
        BATTLE_ORDER_PLAN,
        DRAIN_CAP,
        NON_BATTLEGROUND_DRAIN
    }

    public record FourthSlotPick(String preferred,
                                 boolean pursue,
                                 FourthSlotTrigger trigger) {
        public FourthSlotPick {
            Objects.requireNonNull(trigger, "trigger");
        }
    }

    private ShieldPolicy() {
    }

    public static boolean isStackedPileShieldSource(String sourceTitle) {
        if (sourceTitle == null) {
            return false;
        }
        String lower = sourceTitle.toLowerCase(Locale.ROOT);
        return lower.contains("knowledge and defense")
                || lower.contains("anger, fear, aggression");
    }

    /** Returns the physical source-card id for one validated, aligned top-level Play-a-card action. */
    public static String selectedTopLevelPlayCardSourceId(String decisionType,
                                                          String[] actionIds,
                                                          String[] actionTexts,
                                                          String[] sourceCardIds,
                                                          String selectedActionId) {
        if (!("ACTION_CHOICE".equals(decisionType)
                || "CARD_ACTION_CHOICE".equals(decisionType))
                || actionIds == null || actionTexts == null || sourceCardIds == null
                || actionIds.length == 0
                || actionIds.length != actionTexts.length
                || actionIds.length != sourceCardIds.length
                || selectedActionId == null) {
            return null;
        }

        int selectedMatches = 0;
        String sourceCardId = null;
        for (int i = 0; i < actionIds.length; i++) {
            if (!selectedActionId.equals(actionIds[i])) {
                continue;
            }
            selectedMatches++;
            String actionText = actionTexts[i] != null ? actionTexts[i].trim() : null;
            if ("Play a card".equals(actionText)) {
                sourceCardId = sourceCardIds[i];
            }
        }
        return selectedMatches == 1 && sourceCardId != null && !sourceCardId.isBlank()
                ? sourceCardId : null;
    }

    public static boolean isShieldSelection(int shieldCount, int candidateCount) {
        return shieldCount > 0 && shieldCount >= candidateCount * 0.5d;
    }

    public static FourthSlotPick fourthSlotPick(Side side,
                                                ShieldFacts.FourthSlotFacts facts,
                                                Predicate<String> preferredOnMenu) {
        Objects.requireNonNull(facts, "facts");
        if (side == null) {
            return new FourthSlotPick(null, false, FourthSlotTrigger.CLOSED);
        }

        String preferred = null;
        FourthSlotTrigger trigger = FourthSlotTrigger.CLOSED;
        if (facts.occupiesBothTheaters()) {
            preferred = side == Side.DARK ? "Battle Order" : "Battle Plan";
            trigger = FourthSlotTrigger.BATTLE_ORDER_PLAN;
        } else if (facts.opponentCanDrainThreePlus()
                && (facts.ownBattlegroundCount() >= 3
                    || facts.opponentBattlegroundCount() == 0)) {
            preferred = side == Side.DARK ? "Resistance" : "Ultimatum";
            trigger = FourthSlotTrigger.DRAIN_CAP;
        } else if (facts.occupiesAnyBattleground()
                && facts.opponentBattlegroundCount() < 2
                && facts.opponentHasDrainBonus()
                && facts.opponentDrainsNonBattleground()) {
            preferred = side == Side.DARK
                    ? "Come Here You Big Coward"
                    : "Simple Tricks And Nonsense";
            trigger = FourthSlotTrigger.NON_BATTLEGROUND_DRAIN;
        }

        boolean pursue = preferred != null
                && (preferredOnMenu == null || preferredOnMenu.test(preferred));
        return new FourthSlotPick(preferred, pursue, trigger);
    }

    public static PolicyResult stackedPileParent(String actionId,
                                                 int shieldsOnTable,
                                                 boolean occupiesBothTheaters,
                                                 FourthSlotPick fourthSlot,
                                                 boolean atActivationCap,
                                                 int activationCount,
                                                 boolean atPacingCap,
                                                 int turnNumber) {
        return stackedPileParent(actionId, shieldsOnTable, occupiesBothTheaters,
                fourthSlot, atActivationCap, activationCount, atPacingCap,
                turnNumber, false);
    }

    // EOP-era compatibility overload (no occupiesBothTheaters): the V112
    // third-slot hold cannot fire through this path — pass true so only the
    // EOP reserve semantics apply, matching the EOP lineage's behavior.
    public static PolicyResult stackedPileParent(String actionId,
                                                 int shieldsOnTable,
                                                 FourthSlotPick fourthSlot,
                                                 boolean atActivationCap,
                                                 int activationCount,
                                                 boolean atPacingCap,
                                                 int turnNumber,
                                                 boolean reserveForBattleOrder) {
        return stackedPileParent(actionId, shieldsOnTable, true, fourthSlot,
                atActivationCap, activationCount, atPacingCap, turnNumber,
                reserveForBattleOrder);
    }

    public static PolicyResult stackedPileParent(String actionId,
                                                 int shieldsOnTable,
                                                 boolean occupiesBothTheaters,
                                                 FourthSlotPick fourthSlot,
                                                 boolean atActivationCap,
                                                 int activationCount,
                                                 boolean atPacingCap,
                                                 int turnNumber,
                                                 boolean reserveForBattleOrder) {
        Objects.requireNonNull(fourthSlot, "fourthSlot");
        List<PolicyOperation> operations = new ArrayList<>();

        if (shieldsOnTable == 2 && !occupiesBothTheaters) {
            add(operations, actionId, "V112-third-slot-reserve",
                    TraceOutputKind.ORDERING, -3000.0f,
                    "V112 3RD SLOT HOLD: reserve the third shield for Battle Order");
        }
        if (reserveForBattleOrder) {
            add(operations, actionId, "SHIELDS-EOP-BATTLE-ORDER-RESERVE",
                    TraceOutputKind.ORDERING, -3000.0f,
                    "EOP SHIELD HOLD: preserve this slot until the funded battleground-system deployment makes Battle Order live");
        }
        if (shieldsOnTable >= 3 && !fourthSlot.pursue()) {
            add(operations, actionId, "V124", TraceOutputKind.ORDERING, -3000.0f,
                    "V124 K&D 4TH-SLOT BLOCK: " + shieldsOnTable
                            + " shields already on table, no V105/V107 trigger — don't activate K&D for 4th shield");
        }

        if (atActivationCap) {
            add(operations, actionId, "V102", TraceOutputKind.VETO, -2000.0f,
                    "V102 K&D ACTIVATION CAP: " + activationCount
                            + " activations already this turn (turn " + turnNumber
                            + ") — hold remaining");
        } else if (atPacingCap) {
            add(operations, actionId, "V29.1-stacked-pile", TraceOutputKind.BANDED, -40.0f,
                    "V29.1 K&D SHIELD PACING: Holding shield slot — scout opponent first (turn "
                            + turnNumber + ")");
        } else {
            add(operations, actionId, "SHIELDS-stacked-pile-available",
                    TraceOutputKind.BANDED, 50.0f,
                    "K&D: Play defensive shield (slots available)");
        }
        return result("SHIELD_PARENT_POLICY", operations);
    }

    public static PolicyResult defensiveShieldWindow(String actionId,
                                                      boolean myTurn,
                                                      boolean atPacingCap,
                                                      int turnNumber) {
        if (!myTurn) {
            return one("SHIELD_WINDOW_POLICY", actionId, "SHIELDS-opponent-turn",
                    TraceOutputKind.BANDED, -10.0f,
                    "Defensive shield during opponent's turn - prefer pass");
        }
        if (atPacingCap) {
            return one("SHIELD_WINDOW_POLICY", actionId, "V29.1-shield-window",
                    TraceOutputKind.BANDED, -40.0f,
                    "V29.1 SHIELD PACING: Holding shield slot — wait to scout opponent (turn "
                            + turnNumber + ")");
        }
        return one("SHIELD_WINDOW_POLICY", actionId, "SHIELDS-window-available",
                TraceOutputKind.BANDED, 50.0f, "Defensive shield");
    }

    public static PolicyResult unknownBattleOrderGate(String actionId,
                                                      String cardTitle,
                                                      boolean occupiesBothTheaters) {
        if (!isBattleOrderOrPlan(cardTitle) || occupiesBothTheaters) {
            return result("SHIELD_UNKNOWN_POLICY", List.of());
        }
        return one("SHIELD_UNKNOWN_POLICY", actionId, "V112",
                TraceOutputKind.VETO, -9999.0f,
                "V112 BATTLE ORDER GATE: Need BOTH a BG site AND BG system occupied!");
    }

    public static PolicyResult unknownFourthSlot(String actionId,
                                                 int shieldsOnTable,
                                                 String cardTitle,
                                                 FourthSlotPick pick) {
        Objects.requireNonNull(pick, "pick");
        if (shieldsOnTable < 3) {
            return result("SHIELD_UNKNOWN_POLICY", List.of());
        }
        if (!pick.pursue()) {
            return one("SHIELD_UNKNOWN_POLICY", actionId, "V117",
                    TraceOutputKind.ORDERING, -9999.0f,
                    "V117 4TH SHIELD HOLD: " + shieldsOnTable
                            + " shields on table, no available preferred card — slot closed!");
        }
        if (!titleMatches(cardTitle, pick.preferred())) {
            return one("SHIELD_UNKNOWN_POLICY", actionId, "V117",
                    TraceOutputKind.ORDERING, -9999.0f,
                    "V117 4TH SHIELD: '" + cardTitle + "' not preferred (we want '"
                            + pick.preferred() + "') — block");
        }
        return one("SHIELD_UNKNOWN_POLICY", actionId, "V117",
                TraceOutputKind.ORDERING, 2000.0f,
                "V117 4TH SHIELD BOOST: matches preferred '" + pick.preferred() + "' +2000");
    }

    public static PolicyResult shieldCandidateAdjustments(String actionId,
                                                          String cardTitle,
                                                          float shieldScore,
                                                          int minTurnToPlay,
                                                          int turnNumber,
                                                          int shieldsOnTable,
                                                          FourthSlotPick pick,
                                                          boolean occupiesBothTheaters,
                                                          CandidateRoute route) {
        Objects.requireNonNull(pick, "pick");
        Objects.requireNonNull(route, "route");
        List<PolicyOperation> operations = new ArrayList<>();

        boolean battleOrderTurnOneException = turnNumber == 1
                && isBattleOrderOrPlan(cardTitle) && occupiesBothTheaters;
        if (turnNumber < minTurnToPlay && !battleOrderTurnOneException) {
            add(operations, actionId, "V53-shield-min-turn",
                    TraceOutputKind.VETO, -5000.0f,
                    "V53 SHIELD MIN-TURN: '" + cardTitle + "' waits until turn "
                            + minTurnToPlay + " (current turn " + turnNumber + ") -5000");
        }

        if (shieldsOnTable == 2) {
            if (isBattleOrderOrPlan(cardTitle)
                    && occupiesBothTheaters && shieldScore > -50.0f) {
                add(operations, actionId, "V112-third-slot-selection",
                        TraceOutputKind.ORDERING, 2000.0f,
                        "V112 3RD SLOT BATTLE ORDER: both theaters occupied +2000");
            } else {
                add(operations, actionId, "V112-third-slot-selection",
                        TraceOutputKind.ORDERING, -5000.0f,
                        "V112 3RD SLOT RESERVED: hold for live Battle Order -5000");
            }
        }

        if (shieldsOnTable >= 3) {
            if (!pick.pursue()) {
                add(operations, actionId, "V105-V107-selection",
                        TraceOutputKind.ORDERING, -5000.0f,
                        "V105/V107 4TH SLOT HOLD: no available preferred card — keep slot closed -5000");
            } else if (titleMatches(cardTitle, pick.preferred())) {
                add(operations, actionId, "V105-V107-selection",
                        TraceOutputKind.ORDERING, 2000.0f,
                        "V105/V107 4TH SLOT BOOST: '" + cardTitle
                                + "' matches preferred '" + pick.preferred() + "' +2000");
            } else {
                add(operations, actionId, "V105-V107-selection",
                        TraceOutputKind.ORDERING, -5000.0f,
                        "V105/V107 4TH SLOT: '" + cardTitle
                                + "' not preferred (we want '" + pick.preferred() + "') -5000");
            }
        }

        addBattleOrderSelection(operations, actionId, cardTitle, shieldScore,
                occupiesBothTheaters, route == CandidateRoute.RESERVE);
        return result("SHIELD_CANDIDATE_POLICY", operations);
    }

    private static void addBattleOrderSelection(List<PolicyOperation> operations,
                                                String actionId,
                                                String cardTitle,
                                                float shieldScore,
                                                boolean occupiesBothTheaters,
                                                boolean includeReadyBonus) {
        if (!isBattleOrderOrPlan(cardTitle)) {
            return;
        }
        if (!occupiesBothTheaters) {
            add(operations, actionId, "V51-battle-order-gate",
                    TraceOutputKind.VETO, -9999.0f,
                    "V51 BATTLE ORDER GATE: Need BOTH a BG site AND BG system occupied!");
            return;
        }
        if (includeReadyBonus) {
            add(operations, actionId, "V51-battle-order-ready",
                    TraceOutputKind.BANDED, 50.0f,
                    "V51 BATTLE ORDER: Occupy BG site + BG system — ready!");
        }
        if (shieldScore > -50.0f) {
            add(operations, actionId, "V51-battle-order-early",
                    TraceOutputKind.BANDED, 200.0f,
                    "V51 BATTLE ORDER EARLY-DEPLOY: occupy BG site + system — deploy now, tax compounds +200");
        }
    }

    public static boolean isBattleOrderOrPlan(String cardTitle) {
        if (cardTitle == null) {
            return false;
        }
        String lower = cardTitle.toLowerCase(Locale.ROOT);
        return lower.contains("battle order") || lower.contains("battle plan");
    }

    private static boolean titleMatches(String cardTitle, String preferred) {
        return cardTitle != null && preferred != null
                && cardTitle.toLowerCase(Locale.ROOT)
                        .contains(preferred.toLowerCase(Locale.ROOT));
    }

    private static PolicyResult one(String producerId,
                                    String actionId,
                                    String ruleId,
                                    TraceOutputKind outputKind,
                                    float delta,
                                    String reason) {
        return result(producerId, List.of(operation(actionId, ruleId,
                outputKind, delta, reason)));
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId,
                            String ruleId,
                            TraceOutputKind outputKind,
                            float delta,
                            String reason) {
        operations.add(operation(actionId, ruleId, outputKind, delta, reason));
    }

    private static PolicyOperation operation(String actionId,
                                             String ruleId,
                                             TraceOutputKind outputKind,
                                             float delta,
                                             String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.SHIELDS, outputKind, delta, reason);
    }

    private static PolicyResult result(String producerId, List<PolicyOperation> operations) {
        return new PolicyResult(producerId, operations);
    }
}
