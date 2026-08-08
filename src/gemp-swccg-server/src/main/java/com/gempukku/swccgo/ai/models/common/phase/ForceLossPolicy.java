package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Canonical ordered FORCE-LOSS payment tree shared by Rando and ChosenOne. */
public final class ForceLossPolicy {

    public enum ActionTextChoice {
        PLACE_IN_LOST_PILE,
        MAINTENANCE_PAY,
        MAINTENANCE_OUT_OF_PLAY,
        MAINTENANCE_USED_PILE,
        MAINTENANCE_SACRIFICE,
        GENERIC_USE_UPKEEP,
        GENERIC_USE,
        GENERIC_LOSE,
        GENERIC_SACRIFICE
    }

    public enum Route {
        STANDALONE,
        COMBINED_BATTLE
    }

    public record ObjectiveFlags(boolean myLordSenatorProtection,
                                 boolean huntDown,
                                 boolean requiredForFlip,
                                 boolean pullable,
                                 boolean preferredPayoffActor) {
        public ObjectiveFlags(
                boolean myLordSenatorProtection,
                boolean huntDown,
                boolean requiredForFlip,
                boolean pullable) {
            this(myLordSenatorProtection, huntDown,
                    requiredForFlip, pullable, false);
        }

        public static ObjectiveFlags none() {
            return new ObjectiveFlags(
                    false, false, false, false, false);
        }
    }

    private ForceLossPolicy() {
    }

    public static PolicyResult scoreActionTextChoice(String actionId,
                                                     ActionTextChoice choice) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(choice, "choice");
        return switch (choice) {
            case PLACE_IN_LOST_PILE -> one(actionId, "FORCE-LOSS-place-lost",
                    TraceOutputKind.ORDERING, -50.0f, "Avoid losing cards");
            case MAINTENANCE_PAY -> one(actionId, "V74-maintenance-pay",
                    TraceOutputKind.ORDERING, 400.0f,
                    "V74 MAINTENANCE PAY: keep the card alive!");
            case MAINTENANCE_OUT_OF_PLAY -> one(actionId,
                    "V74-maintenance-out-of-play", TraceOutputKind.ORDERING,
                    -800.0f,
                    "V74 MAINTENANCE SACRIFICE: place out of play is PERMANENT loss!");
            case MAINTENANCE_USED_PILE -> one(actionId,
                    "V74-maintenance-used-pile", TraceOutputKind.ORDERING,
                    -200.0f,
                    "V74 MAINTENANCE USED-PILE: lose card to used pile, keep blueprint");
            case MAINTENANCE_SACRIFICE -> one(actionId,
                    "V74-maintenance-sacrifice", TraceOutputKind.ORDERING,
                    -800.0f, "V74 MAINTENANCE SACRIFICE: avoid");
            case GENERIC_USE_UPKEEP -> one(actionId, "V22.3-maintenance-use",
                    TraceOutputKind.ORDERING, 150.0f,
                    "V22.3 MAINTENANCE: Pay upkeep cost!");
            case GENERIC_USE -> one(actionId, "FORCE-LOSS-generic-use",
                    TraceOutputKind.ORDERING, -20.0f,
                    "'Use Force' action \u2014 prefer not to use force unnecessarily");
            case GENERIC_LOSE -> one(actionId, "FORCE-LOSS-generic-lose",
                    TraceOutputKind.ORDERING, -30.0f,
                    "'Lose Force' action \u2014 avoid losing force");
            case GENERIC_SACRIFICE -> one(actionId, "V22.3-generic-sacrifice",
                    TraceOutputKind.ORDERING, -150.0f,
                    "V22.3: Avoid sacrificing cards \u2014 prefer alternatives");
        };
    }

    public static PolicyResult scoreUnknownLoss(String actionId,
                                                CardCategory category,
                                                boolean huntDownLightsaber) {
        Objects.requireNonNull(actionId, "actionId");
        List<PolicyOperation> operations = new ArrayList<>();
        if (category == CardCategory.EFFECT || category == CardCategory.INTERRUPT) {
            add(operations, actionId, "FORCE-LOSS-unknown-effect-interrupt",
                    TraceOutputKind.ORDERING, 25.0f,
                    "Effect/Interrupt - OK to lose");
        } else if (category == CardCategory.CHARACTER) {
            add(operations, actionId, "FORCE-LOSS-unknown-character",
                    TraceOutputKind.ORDERING, -15.0f,
                    "Character - avoid losing");
        } else if (category == CardCategory.STARSHIP) {
            add(operations, actionId, "FORCE-LOSS-unknown-starship",
                    TraceOutputKind.ORDERING, -15.0f,
                    "Starship - avoid losing");
        } else if (category == CardCategory.VEHICLE) {
            add(operations, actionId, "FORCE-LOSS-unknown-vehicle",
                    TraceOutputKind.ORDERING, -10.0f,
                    "Vehicle - avoid losing");
        } else if (category == CardCategory.LOCATION) {
            add(operations, actionId, "FORCE-LOSS-unknown-location",
                    TraceOutputKind.ORDERING, -20.0f,
                    "Location - avoid losing");
        }
        if (huntDownLightsaber) {
            add(operations, actionId, "V25-unknown-loss",
                    TraceOutputKind.ORDERING, -300.0f,
                    "V25 HUNT DOWN: PROTECT LIGHTSABER from loss!");
        }
        return new PolicyResult("FORCE_LOSS_POLICY", operations);
    }

    public static PolicyResult scoreUnknownObjectiveRetention(
            String actionId, boolean selectedShieldRoutePackageCard) {
        Objects.requireNonNull(actionId, "actionId");
        if (!selectedShieldRoutePackageCard) {
            return new PolicyResult(
                    "FORCE_LOSS_POLICY", List.of());
        }
        return one(actionId, "HOTH.SHIELD.PACKAGE_RETAIN",
                TraceOutputKind.VETO, -9999.0f,
                "HOTH SHIELD PACKAGE: retain the selected host, pilot, and Cannon");
    }

    public static PolicyResult score(String actionId,
                                     Route route,
                                     ForceLossFacts.DecisionFacts decision,
                                     ForceLossFacts.CandidateFacts candidate,
                                     ObjectiveFlags objective) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(objective, "objective");

        List<PolicyOperation> operations = new ArrayList<>();
        if (route == Route.STANDALONE) {
            addStandalone(operations, actionId, decision, candidate, objective);
        } else {
            addCombinedBattle(operations, actionId, decision, candidate, objective);
        }
        return new PolicyResult("FORCE_LOSS_POLICY", operations);
    }

    private static void addStandalone(List<PolicyOperation> operations,
                                      String actionId,
                                      ForceLossFacts.DecisionFacts decision,
                                      ForceLossFacts.CandidateFacts candidate,
                                      ObjectiveFlags objective) {
        String title = candidate.title();
        if (objective.myLordSenatorProtection() && candidate.senator()) {
            add(operations, actionId, "V109", TraceOutputKind.ORDERING, -300.0f,
                    "V109 MY LORD: PROTECT senator '" + title
                            + "' — never discard/lose senators in this deck -300");
        }

        if (objective.preferredPayoffActor()) {
            add(operations, actionId,
                    "OBJECTIVE.NABOO_DUEL.DUELIST_RETAIN",
                    TraceOutputKind.BANDED, -700.0f,
                    "NABOO DUEL: retain exactly one typed Jedi or Dark Jedi for the objective's executable payoff");
        }

        addZoneScore(operations, actionId, decision, candidate, true);

        // WMAOP 2026-08-08 (Steve directive): Blockade site on table means the
        // in-hand WMAOP is dead — promote it as the PREFERRED force-loss fodder
        // (V95 precedent: the pull veto holds it in hand; this makes it the pick).
        if (candidate.fromHand() && candidate.wmaopFodderHold()) {
            add(operations, actionId, "WMAOP.FODDER_HOLD",
                    TraceOutputKind.ORDERING, 300.0f,
                    "WMAOP.FODDER_HOLD: '" + title
                            + "' is dead in hand (Blockade Flagship site on table) — preferred force-loss fodder +300");
        }

        boolean protectChars = decision.lifeForce() >= 4;
        if (candidate.fromReserve() && protectChars
                && decision.reserveDeckSize() <= 10) {
            add(operations, actionId, "V153-thin-reserve", TraceOutputKind.ORDERING,
                    -335.0f,
                    "V153 THIN RESERVE (deck=" + decision.reserveDeckSize()
                            + "): demote reserve below hand chars to preserve destiny");
        }

        if (candidate.fromHand() && candidate.battleInterrupt() && protectChars
                && !candidate.duplicate() && decision.turnNumber() > 3) {
            add(operations, actionId, "V175", TraceOutputKind.ORDERING, -450.0f,
                    "V175 PROTECT BATTLE INTERRUPT: '" + title
                            + "' is an in-battle trick — lose it near-last, like a character");
        }

        if (candidate.fromHand()
                && candidate.category() == com.gempukku.swccgo.common.CardCategory.WEAPON
                && protectChars && !candidate.duplicate()
                && decision.turnNumber() > 3 && candidate.hasWielder()) {
            add(operations, actionId, "V178-loss", TraceOutputKind.ORDERING, -450.0f,
                    "V178 PROTECT WEAPON: '" + title
                            + "' — we have a wielder; lose it near-last, like a character");
        }

        if (candidate.fromHand() && decision.handSize() <= 4
                && decision.lifeForce() >= 10) {
            add(operations, actionId, "V153-hand-floor", TraceOutputKind.BANDED,
                    -700.0f,
                    "V153 HAND FLOOR: only " + decision.handSize()
                            + " in hand (life force " + decision.lifeForce()
                            + ">=10) — keep >=4, lose from piles instead -700");
        }

        if (candidate.fromForcePile() && decision.drawTheirFireActive()) {
            float penalty = decision.forcePileSize() <= 3 ? -400.0f : -200.0f;
            add(operations, actionId, "V28-DTF", TraceOutputKind.ORDERING, penalty,
                    String.format("V28 DTF FORCE PILE PROTECT: Force pile=%d, DTF active — lose from reserve instead!",
                            decision.forcePileSize()));
        }

        if ((candidate.fromHand() || candidate.fromUsedPile())
                && !candidate.duplicate() && candidate.priorityCard()) {
            add(operations, actionId, "V153-priority", TraceOutputKind.ORDERING,
                    -100.0f,
                    "V153 PRIORITY CARD: protect '" + title
                            + "' (hand/used) -100");
        }

        if (objective.requiredForFlip()) {
            add(operations, actionId, "V21-objective", TraceOutputKind.VETO,
                    -9999.0f,
                    candidate.fromHand()
                            ? "OBJECTIVE CRITICAL IN HAND - NEVER LOSE!"
                            : "OBJECTIVE CRITICAL - NEVER LOSE!");
        } else if (candidate.fromHand() && objective.pullable()) {
            add(operations, actionId, "V21-objective", TraceOutputKind.VETO,
                    -9999.0f, "OBJECTIVE PULLABLE IN HAND - NEVER LOSE!");
        }
        if (candidate.fromHand()
                && objective.huntDown() && isLightsaber(title)) {
            add(operations, actionId, "V25", TraceOutputKind.ORDERING,
                    -500.0f, "V25 HUNT DOWN: PROTECT LIGHTSABER IN HAND!");
        }
    }

    private static void addCombinedBattle(List<PolicyOperation> operations,
                                          String actionId,
                                          ForceLossFacts.DecisionFacts decision,
                                          ForceLossFacts.CandidateFacts candidate,
                                          ObjectiveFlags objective) {
        String title = candidate.title();
        if (objective.huntDown() && isLightsaber(title)) {
            add(operations, actionId, "V25", TraceOutputKind.ORDERING,
                    -400.0f, "V25 HUNT DOWN: PROTECT LIGHTSABER from loss!");
        }

        if (objective.preferredPayoffActor()) {
            add(operations, actionId,
                    "OBJECTIVE.NABOO_DUEL.DUELIST_RETAIN",
                    TraceOutputKind.BANDED, -700.0f,
                    "NABOO DUEL: retain exactly one typed Jedi or Dark Jedi for the objective's executable payoff");
        }

        addZoneScore(operations, actionId, decision, candidate, false);

        // WMAOP 2026-08-08 (Steve directive): mirror of the standalone route —
        // dead in-hand WMAOP is the preferred fodder on combined decisions too.
        if (candidate.fromHand() && candidate.wmaopFodderHold()) {
            add(operations, actionId, "WMAOP.FODDER_HOLD",
                    TraceOutputKind.ORDERING, 300.0f,
                    "WMAOP.FODDER_HOLD: '" + title
                            + "' is dead in hand (Blockade Flagship site on table) — preferred force-loss fodder +300");
        }

        boolean protectChars = decision.lifeForce() >= 4;
        if (candidate.fromReserve() && protectChars
                && decision.reserveDeckSize() <= 10) {
            add(operations, actionId, "V153-thin-reserve", TraceOutputKind.ORDERING,
                    -335.0f,
                    "V153 THIN RESERVE (deck=" + decision.reserveDeckSize()
                            + "): demote reserve below hand chars to preserve destiny");
        }

        if (candidate.fromHand() && decision.handSize() <= 4
                && decision.lifeForce() >= 10) {
            add(operations, actionId, "V153-hand-floor", TraceOutputKind.BANDED,
                    -700.0f,
                    "V153 HAND FLOOR: only " + decision.handSize()
                            + " in hand (life force " + decision.lifeForce()
                            + ">=10) — keep >=4 -700");
        }

        if (objective.requiredForFlip()) {
            add(operations, actionId, "V21-objective", TraceOutputKind.VETO,
                    -9999.0f, "OBJECTIVE CRITICAL - NEVER LOSE!");
        } else if (objective.pullable()) {
            add(operations, actionId, "V21-objective", TraceOutputKind.VETO,
                    -9999.0f, "OBJECTIVE PULLABLE - NEVER LOSE!");
        }

        if ((candidate.fromHand() || candidate.fromUsedPile())
                && !candidate.duplicate() && candidate.priorityCard()) {
            add(operations, actionId, "V153-priority", TraceOutputKind.ORDERING,
                    -100.0f,
                    "V153 PRIORITY CARD: protect '" + title + "' -100");
        }
    }

    private static void addZoneScore(List<PolicyOperation> operations,
                                     String actionId,
                                     ForceLossFacts.DecisionFacts decision,
                                     ForceLossFacts.CandidateFacts candidate,
                                     boolean standalone) {
        boolean protectChars = decision.lifeForce() >= 4;
        float zoneScore;
        if (candidate.fromHand()) {
            if (candidate.duplicate()) {
                zoneScore = 1000.0f;
            } else if (candidate.handCharacter()) {
                zoneScore = protectChars ? 100.0f : 700.0f;
            } else if (candidate.handShipOrVehicle()) {
                zoneScore = protectChars ? 500.0f : 750.0f;
            } else {
                zoneScore = protectChars ? 600.0f : 850.0f;
            }
        } else if (candidate.fromUsedPile()) {
            zoneScore = protectChars ? 800.0f : 400.0f;
        } else if (candidate.fromReserve()) {
            zoneScore = protectChars ? 400.0f : 300.0f;
        } else if (candidate.fromForcePile()) {
            zoneScore = 50.0f;
        } else {
            zoneScore = 100.0f;
        }
        String zone = candidate.zoneName().isEmpty() && standalone
                ? "?" : candidate.zoneName();
        add(operations, actionId, "V153-zone", TraceOutputKind.ORDERING,
                zoneScore,
                "V153 ZONE (" + zone + ", lifeForce=" + decision.lifeForce()
                        + ", protectChars=" + protectChars + ")");
    }

    private static boolean isLightsaber(String title) {
        return title != null && title.toLowerCase(Locale.ROOT).contains("lightsaber");
    }

    private static PolicyResult one(String actionId,
                                    String ruleId,
                                    TraceOutputKind outputKind,
                                    float delta,
                                    String reason) {
        return new PolicyResult("FORCE_LOSS_POLICY", List.of(
                PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                        TraceDomainId.FORCE_LOSS_PAYMENT, outputKind,
                        delta, reason)));
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId,
                            String ruleId,
                            TraceOutputKind outputKind,
                            float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.FORCE_LOSS_PAYMENT, outputKind, delta, reason));
    }
}
