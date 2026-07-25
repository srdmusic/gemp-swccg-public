package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure ordered BATTLE-3 damage and forfeit policy shared by both bots. */
public final class BattleForfeitPolicy {

    private static final String PRODUCER_ID = "BATTLE_FORFEIT_POLICY";

    public enum Route {
        OPTIONAL_FORFEIT(true),
        COMBINED_MANDATORY(false);

        private final boolean passLegal;

        Route(boolean passLegal) {
            this.passLegal = passLegal;
        }

        public boolean passLegal() {
            return passLegal;
        }
    }

    /** The exact control-flow seam the unchanged evaluator adapter must perform next. */
    public enum AdapterStep {
        CONTINUE_CANDIDATE,
        APPLY_FORCE_LOSS_THEN_AFTER_ROUTE,
        APPLY_FORFEIT_AFTER_ROUTE
    }

    /**
     * BATTLE operations before and after the route seam. Shared FORCE-LOSS operations belong
     * between these results when {@link AdapterStep#APPLY_FORCE_LOSS_THEN_AFTER_ROUTE} is set.
     */
    public record Evaluation(Route route,
                             AdapterStep adapterStep,
                             PolicyResult beforeRoute,
                             PolicyResult afterRoute) {
        public Evaluation {
            Objects.requireNonNull(route, "route");
            Objects.requireNonNull(adapterStep, "adapterStep");
            Objects.requireNonNull(beforeRoute, "beforeRoute");
            Objects.requireNonNull(afterRoute, "afterRoute");
        }

        public boolean passLegal() {
            return route.passLegal();
        }
    }

    private record V159Score(String ruleArmId,
                             TraceOutputKind outputKind,
                             float delta) {
    }

    /**
     * Adapter-read standalone forfeit values. Engine and card reads remain in the
     * evaluator so their legacy ordering and failure boundaries stay unchanged.
     */
    public record StandaloneResidualFacts(String actionId,
                                          boolean deadCard,
                                          boolean pilotOnShip,
                                          Float forfeitValue,
                                          Float power,
                                          boolean unique,
                                          Float uniqueAbility,
                                          Float uniquePower,
                                          BattleForfeitFacts.ObjectiveFlags objective) {
        public StandaloneResidualFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
        }

        public static StandaloneResidualFacts priority(String actionId,
                                                       boolean deadCard,
                                                       boolean pilotOnShip) {
            return new StandaloneResidualFacts(
                    actionId, deadCard, pilotOnShip,
                    null, null, false, null, null,
                    BattleForfeitFacts.ObjectiveFlags.none());
        }
    }

    private BattleForfeitPolicy() {
    }

    /**
     * Preserves the exact unflipped Invasion actor-and-buddy formation only
     * when another legal loss can be selected. Mandatory unavoidable losses
     * remain neutral.
     */
    public static PolicyResult scoreFlipGateFormationProtection(
            String actionId,
            ObjectiveAnalyzer.FlipGateFormationRole role,
            boolean hasUnprotectedLegalAlternative) {
        Objects.requireNonNull(actionId, "actionId");
        List<PolicyOperation> operations = new ArrayList<>();
        if (!hasUnprotectedLegalAlternative || role == null
                || role == ObjectiveAnalyzer.FlipGateFormationRole.NONE) {
            return result(operations);
        }

        String reason;
        if (role == ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR) {
            reason = "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD: preserve the last required actor while another legal loss exists";
        } else {
            reason = "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD: preserve the required actor's last buddy while another legal loss exists";
        }
        add(operations, actionId,
                "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                TraceOutputKind.VETO, -9999.0f, reason);
        return result(operations);
    }

    public static Evaluation evaluateOptional(
            BattleForfeitFacts.DecisionFacts decision,
            BattleForfeitFacts.CandidateFacts candidate,
            BattleForfeitFacts.ObjectiveFlags objective) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(objective, "objective");

        List<PolicyOperation> operations = new ArrayList<>();
        if (decision.damageRemaining() <= 0) {
            add(operations, candidate.actionId(), "V29.13-forfeit",
                    TraceOutputKind.VETO, -500.0f,
                    "V29.13 IMMUNE/NO DAMAGE - never forfeit voluntarily!");
            return evaluation(Route.OPTIONAL_FORFEIT,
                    AdapterStep.CONTINUE_CANDIDATE, operations, List.of());
        }

        addV159(operations, candidate.actionId(), decision, candidate);
        add(operations, candidate.actionId(), "V22.4-zero-forfeit",
                TraceOutputKind.BANDED, -80.0f,
                "Optional forfeit but zero forfeit value");
        if (objective.requiredForFlip()) {
            add(operations, candidate.actionId(), "V21-optional-required",
                    TraceOutputKind.VETO, -9999.0f,
                    "OBJECTIVE CRITICAL - don't voluntarily forfeit");
        } else if (objective.pullable()) {
            add(operations, candidate.actionId(), "V21-optional-pullable",
                    TraceOutputKind.VETO, -9999.0f,
                    "OBJECTIVE PULLABLE - don't voluntarily forfeit");
        }
        return evaluation(Route.OPTIONAL_FORFEIT,
                AdapterStep.CONTINUE_CANDIDATE, operations, List.of());
    }

    /** V48 standalone additive protection after the evaluator counts attached crew. */
    public static PolicyResult scoreStandaloneShipWithCrew(String actionId,
                                                           String title,
                                                           int crewCount) {
        List<PolicyOperation> operations = new ArrayList<>();
        if (crewCount > 0) {
            add(operations, actionId, "V48-ship-with-crew", TraceOutputKind.VETO, -9999.0f,
                    String.format("V48 SHIP WITH CREW: %s has %d crew aboard \u2014 forfeit crew first, not the ship!",
                            title, crewCount));
        }
        return result(operations);
    }

    /** Fixed standalone priorities applied at their original adapter positions. */
    public static PolicyResult scoreStandalonePriority(StandaloneResidualFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.deadCard()) {
            add(operations, facts.actionId(), "BATTLE-forfeit-dead-card",
                    TraceOutputKind.ORDERING, 140.0f,
                    "☠️ DEAD CARD (persona on table) - forfeit!");
        }
        if (facts.pilotOnShip()) {
            add(operations, facts.actionId(), "BATTLE-forfeit-pilot-on-ship",
                    TraceOutputKind.ORDERING, 50.0f,
                    "PILOT ON SHIP - forfeit first!");
        }
        return result(operations);
    }

    /** Ordered V139 and V21 standalone-forfeit residual scoring. */
    public static PolicyResult scoreStandaloneResidual(StandaloneResidualFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.forfeitValue() != null) {
            add(operations, facts.actionId(), "V139-forfeit-value", TraceOutputKind.ORDERING,
                    Math.max(0, 100 - (facts.forfeitValue() * 10)),
                    String.format("Forfeit value %.0f", facts.forfeitValue()));
        }
        if (facts.power() != null) {
            if (facts.power() <= 2) {
                add(operations, facts.actionId(), "V139-low-power", TraceOutputKind.ORDERING,
                        50.0f, "Low power - cheap loss, forfeit first");
            } else if (facts.power() >= 5) {
                add(operations, facts.actionId(), "V139-high-power", TraceOutputKind.ORDERING,
                        -100.0f, "V139 High power - prefer keeping for battle");
            }
        }
        if (facts.unique()) {
            if ((facts.uniqueAbility() != null && facts.uniqueAbility() >= 5)
                    || (facts.uniquePower() != null && facts.uniquePower() >= 5)) {
                add(operations, facts.actionId(), "V139-valuable-unique",
                        TraceOutputKind.ORDERING, -300.0f,
                        "V139 VALUABLE UNIQUE - never forfeit unless forced");
            } else {
                add(operations, facts.actionId(), "V139-generic-unique",
                        TraceOutputKind.ORDERING, -100.0f,
                        "V139 Unique - avoid forfeiting");
            }
        }
        if (facts.objective().requiredForFlip()) {
            add(operations, facts.actionId(), "V21-forfeit-required",
                    TraceOutputKind.VETO, -9999.0f,
                    "OBJECTIVE CRITICAL - NEVER FORFEIT!");
        } else if (facts.objective().pullable()) {
            add(operations, facts.actionId(), "V21-forfeit-pullable",
                    TraceOutputKind.VETO, -9999.0f,
                    "OBJECTIVE PULLABLE - NEVER FORFEIT!");
        }
        return result(operations);
    }

    public static Evaluation evaluateCombined(
            BattleForfeitFacts.DecisionFacts decision,
            BattleForfeitFacts.CandidateFacts candidate) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(candidate, "candidate");

        List<PolicyOperation> beforeRoute = new ArrayList<>();
        if (candidate.blueprintPresent() && candidate.weapon()) {
            float boost = candidate.attachedHostHit() ? 2200.0f : 2000.0f;
            String reason = "V154 WEAPON-LOSS: strip weapon first for extra coverage"
                    + (candidate.attachedHostHit()
                        ? " (host is HIT \u2014 lost anyway)" : "")
                    + " \u2014 before hit chars";
            add(beforeRoute, candidate.actionId(),
                    candidate.attachedHostHit() ? "V154-hit-host" : "V154-weapon",
                    TraceOutputKind.ORDERING, boost, reason);
            return evaluation(Route.COMBINED_MANDATORY,
                    AdapterStep.CONTINUE_CANDIDATE, beforeRoute, List.of());
        }

        if (decision.smallPureDamage()) {
            if (candidate.forceLossOption()) {
                add(beforeRoute, candidate.actionId(), "V118-force-loss",
                        TraceOutputKind.BANDED, 200.0f,
                        "V118 SMALL DAMAGE: only " + decision.damageRemaining()
                                + " battle damage \u2014 lose from reserves instead of forfeiting a character");
            } else if (candidate.character() && !candidate.hit()) {
                add(beforeRoute, candidate.actionId(), "V118-save-character",
                        TraceOutputKind.BANDED, -500.0f,
                        "V118 SAVE CHARACTER: only " + decision.damageRemaining()
                                + " battle damage \u2014 characters worth more than that, lose from reserves!");
            }
        }

        List<PolicyOperation> afterRoute = new ArrayList<>();
        if (candidate.forceLossOption()) {
            addForceLossTail(afterRoute, decision, candidate.actionId());
            return evaluation(Route.COMBINED_MANDATORY,
                    AdapterStep.APPLY_FORCE_LOSS_THEN_AFTER_ROUTE,
                    beforeRoute, afterRoute);
        }

        addV159(afterRoute, candidate.actionId(), decision, candidate);
        return evaluation(Route.COMBINED_MANDATORY,
                AdapterStep.APPLY_FORFEIT_AFTER_ROUTE, beforeRoute, afterRoute);
    }

    private static void addForceLossTail(List<PolicyOperation> operations,
                                         BattleForfeitFacts.DecisionFacts decision,
                                         String actionId) {
        if (decision.attritionRemaining() > 0) {
            add(operations, actionId, "V150", TraceOutputKind.ORDERING, -500.0f,
                    "V150 CANNOT satisfy attrition with Force loss \u2014 forfeit covers attrition+damage together, don't waste pile!");
        } else if (decision.damageRemaining() > 0) {
            BattleForfeitFacts.CandidateSetFacts candidateSet = decision.candidateSet();
            if (candidateSet.hasHitCandidates()) {
                add(operations, actionId, "V22.3-hit", TraceOutputKind.ORDERING,
                        -80.0f,
                        "V22.3: Have hit cards to forfeit first - much more efficient!");
            } else if (candidateSet.hasDeadCandidates()) {
                add(operations, actionId, "V22.3-dead", TraceOutputKind.ORDERING,
                        -80.0f,
                        "V22.3: Have dead cards to forfeit - they satisfy multiple damage!");
            } else {
                float penalty = -40.0f;
                String ruleArmId = "V22.3-damage-1-5";
                if (decision.damageRemaining() > 5) {
                    penalty = -80.0f;
                    ruleArmId = "V22.3-damage-6-10";
                }
                if (decision.damageRemaining() > 10) {
                    penalty = -120.0f;
                    ruleArmId = "V22.3-damage-11-plus";
                }
                add(operations, actionId, ruleArmId, TraceOutputKind.ORDERING,
                        penalty,
                        "V22.3: FORFEIT CHARACTERS FIRST - they cover "
                                + "multiple damage points per card! ("
                                + decision.damageRemaining() + " damage left)");
            }
        }
    }

    private static void addV159(List<PolicyOperation> operations,
                                String actionId,
                                BattleForfeitFacts.DecisionFacts decision,
                                BattleForfeitFacts.CandidateFacts candidate) {
        V159Score score = v159Score(decision, candidate);
        if (score == null || score.delta() == 0.0f) {
            return;
        }
        add(operations, actionId, score.ruleArmId(), score.outputKind(), score.delta(),
                String.format("V159 FORFEIT (attr=%d dmg=%d fv=%.0f hit=%s)",
                        decision.attritionRemaining(), decision.damageRemaining(),
                        candidate.forfeitValue(), candidate.hit()));
    }

    private static V159Score v159Score(
            BattleForfeitFacts.DecisionFacts decision,
            BattleForfeitFacts.CandidateFacts candidate) {
        if (!candidate.blueprintPresent()) {
            return null;
        }

        int attrition = decision.attritionRemaining();
        int damage = decision.damageRemaining();
        float forfeit = candidate.forfeitValue();
        float armedDelta = candidate.armed() ? -10.0f : 0.0f;

        if (candidate.hit()) {
            return score("V159-hit", TraceOutputKind.ORDERING,
                    attrition > 0 ? 1500.0f : 3000.0f);
        }
        if (candidate.dead()) {
            return score("V159-dead", TraceOutputKind.ORDERING,
                    attrition > 0 ? 1200.0f : 2500.0f);
        }

        if (candidate.immunity().immuneTo(attrition) && attrition > 0) {
            if (damage > 0 && forfeit > 0.0f) {
                int savings = (int) Math.min(forfeit, damage);
                int waste = (int) Math.max(0.0f, forfeit - damage);
                if (damage >= 4 && savings >= 3) {
                    return score("V161-damage-cover", TraceOutputKind.BANDED,
                            1500.0f + savings * 80.0f - waste * 30.0f);
                }
                BattleForfeitFacts.SoloPowerFacts soloPower = candidate.soloPower();
                if (soloPower.isSolo() && soloPower.opponentPowerGap() > 0.0f) {
                    String ruleArmId = candidate.armed()
                            ? "V161-solo-gap+V178" : "V161-solo-gap";
                    return score(ruleArmId, TraceOutputKind.BANDED,
                            Math.min(1200.0f,
                                    100.0f + soloPower.opponentPowerGap() * 120.0f)
                                    + armedDelta);
                }
                return score("V159-immune-cautious", TraceOutputKind.ORDERING,
                        savings * 60.0f - waste * 40.0f - 500.0f);
            }
            return score("V159-immune-no-coverage", TraceOutputKind.ORDERING,
                    -2500.0f);
        }

        if (attrition > 0) {
            if ((candidate.capitalShip() || candidate.priorityCard())
                    && forfeit < attrition) {
                return score("V159-centerpiece-release", TraceOutputKind.ORDERING,
                        -1000.0f);
            }
            if (candidate.gameWinner() && attrition <= 2) {
                return score("V159-game-winner-release", TraceOutputKind.ORDERING,
                        -1500.0f);
            }
            int total = attrition + damage;
            int coverage = (int) Math.min(forfeit, total);
            float result = 1500.0f + coverage * 100.0f;
            if (forfeit >= total) {
                result += 300.0f;
            }
            if (forfeit >= 1.0f && forfeit <= 3.0f) {
                result += 200.0f;
            }
            String ruleArmId = candidate.armed()
                    ? "V159-attrition+V178" : "V159-attrition";
            return score(ruleArmId, TraceOutputKind.ORDERING,
                    result + armedDelta);
        }

        if (damage <= 0) {
            return null;
        }
        if (damage < 3) {
            return score("V159-pure-small", TraceOutputKind.ORDERING,
                    -3000.0f);
        }

        int savings = (int) Math.min(forfeit, damage);
        int waste = (int) Math.max(0.0f, forfeit - damage);
        if (savings < 3) {
            return score("V159-pure-low-coverage", TraceOutputKind.ORDERING,
                    -800.0f);
        }

        float result = 1500.0f + savings * 80.0f - waste * 30.0f;
        if (savings >= damage / 2) {
            result += 200.0f;
        }
        String ruleArmId = candidate.armed()
                ? "V159-pure-damage+V178" : "V159-pure-damage";
        return score(ruleArmId, TraceOutputKind.ORDERING, result + armedDelta);
    }

    private static V159Score score(String ruleArmId,
                                   TraceOutputKind outputKind,
                                   float delta) {
        return new V159Score(ruleArmId, outputKind, delta);
    }

    private static Evaluation evaluation(Route route,
                                         AdapterStep adapterStep,
                                         List<PolicyOperation> beforeRoute,
                                         List<PolicyOperation> afterRoute) {
        return new Evaluation(route, adapterStep,
                result(beforeRoute), result(afterRoute));
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult(PRODUCER_ID, operations);
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId,
                            String ruleArmId,
                            TraceOutputKind outputKind,
                            float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleArmId),
                TraceDomainId.BATTLE_FORFEIT, outputKind, delta, reason));
    }
}
