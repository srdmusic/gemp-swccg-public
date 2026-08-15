package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Terminal guards for choices that destroy the active objective. */
public final class ObjectiveHardLossPolicy {

    public enum Threat {
        NONE,
        SCANNING_CREW,
        NON_EPIC_DUEL
    }

    public enum RecallKind {
        CLASSIC,
        VIRTUAL
    }

    public record Facts(
            String actionId,
            boolean classicHuntDownActive,
            Threat threat,
            boolean maulDuelException) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(threat, "threat");
        }
    }

    public record RecallFacts(
            String actionId,
            RecallKind kind,
            boolean preservesObjective) {
        public RecallFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(kind, "kind");
        }
    }

    private ObjectiveHardLossPolicy() {
    }

    public static PolicyResult score(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.classicHuntDownActive()
                || facts.threat() == Threat.NONE
                || facts.threat() == Threat.NON_EPIC_DUEL
                    && facts.maulDuelException()) {
            return new PolicyResult(
                    "OBJECTIVE_HARD_LOSS_POLICY", List.of());
        }

        String ruleId;
        String reason;
        if (facts.threat() == Threat.SCANNING_CREW) {
            ruleId =
                    "OBJECTIVE.HARD_LOSS.CLASSIC_HUNT_SCANNING_CREW";
            reason = "CLASSIC HUNT DOWN HARD LOSS: playing Scanning Crew"
                    + " places the objective out of play";
        } else {
            ruleId =
                    "OBJECTIVE.HARD_LOSS.CLASSIC_HUNT_NON_EPIC_DUEL";
            reason = "CLASSIC HUNT DOWN HARD LOSS: initiating this"
                    + " non-Epic duel places the objective out of play";
        }
        return new PolicyResult(
                "OBJECTIVE_HARD_LOSS_POLICY",
                List.of(PolicyOperation.hardVeto(
                        facts.actionId(),
                        TraceRuleId.of(ruleId),
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.VETO,
                        reason)));
    }

    public static PolicyResult scoreRecall(RecallFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.preservesObjective()) {
            return new PolicyResult(
                    "OBJECTIVE_HARD_LOSS_POLICY", List.of());
        }

        if (facts.kind() == RecallKind.CLASSIC) {
            return new PolicyResult(
                    "OBJECTIVE_HARD_LOSS_POLICY",
                    List.of(PolicyOperation.add(
                            facts.actionId(),
                            TraceRuleId.of(
                                    "V35-vader-recall-objective-actor"),
                            TraceDomainId.OBJECTIVE_INTENT,
                            TraceOutputKind.BANDED,
                            -300.0f,
                            "V35 VADER RECALL DISFAVORED: recalling the sole"
                                    + " required battleground Vader would"
                                    + " dismantle Hunt Down")));
        }

        return new PolicyResult(
                "OBJECTIVE_HARD_LOSS_POLICY",
                List.of(PolicyOperation.add(
                        facts.actionId(),
                        TraceRuleId.of(
                                "OBJECTIVE.FLIP_BACK.VIRTUAL_HUNT_VADER_RECALL"),
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.BANDED,
                        -300.0f,
                        "VIRTUAL HUNT DOWN RECALL DISFAVORED: taking the"
                                + " last Vader into hand would immediately"
                                + " satisfy the flip-back law")));
    }

    public static PolicyResult scoreRalltiirSelfDestruct(
            String actionId, boolean exactSelfDestruct) {
        Objects.requireNonNull(actionId, "actionId");
        return exactSelfDestruct
                ? new PolicyResult(
                    "OBJECTIVE_HARD_LOSS_POLICY",
                    List.of(PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                            "OBJECTIVE.HARD_LOSS.RALLTIIR_SELF_DESTRUCT"),
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.VETO,
                        "RALLTIIR OPERATIONS HARD LOSS: do not use Commence Primary Ignition to blow away the active objective system")))
                : new PolicyResult(
                    "OBJECTIVE_HARD_LOSS_POLICY", List.of());
    }
}
