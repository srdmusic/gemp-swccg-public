package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Canonical ordered FORCE-LOSS payment tree shared by Rando and ChosenOne. */
public final class ForceLossPolicy {

    public enum Route {
        STANDALONE,
        COMBINED_BATTLE
    }

    public record ObjectiveFlags(boolean myLordSenatorProtection,
                                 boolean huntDown,
                                 boolean requiredForFlip,
                                 boolean pullable) {
        public static ObjectiveFlags none() {
            return new ObjectiveFlags(false, false, false, false);
        }
    }

    private ForceLossPolicy() {
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

        addZoneScore(operations, actionId, decision, candidate, true);

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

        if (candidate.fromHand()) {
            if (objective.requiredForFlip()) {
                add(operations, actionId, "V21-objective", TraceOutputKind.VETO,
                        -9999.0f, "OBJECTIVE CRITICAL IN HAND - NEVER LOSE!");
            } else if (objective.pullable()) {
                add(operations, actionId, "V21-objective", TraceOutputKind.VETO,
                        -9999.0f, "OBJECTIVE PULLABLE IN HAND - NEVER LOSE!");
            }
            if (objective.huntDown() && isLightsaber(title)) {
                add(operations, actionId, "V25", TraceOutputKind.ORDERING,
                        -500.0f, "V25 HUNT DOWN: PROTECT LIGHTSABER IN HAND!");
            }
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

        addZoneScore(operations, actionId, decision, candidate, false);

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
