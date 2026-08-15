package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;

/** Exact objective policy for 216_11 and 216_11_BACK. */
public final class OnTheVergeObjectivePolicy {

    private OnTheVergeObjectivePolicy() {
    }

    public static PolicyResult scoreScarifBattlegroundRoute(
            String actionId, boolean exactAction,
            boolean legalCandidateAvailable) {
        if (!exactAction) return empty();
        if (!legalCandidateAvailable) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.OTVOG.SCARIF_ROUTE_EXHAUSTED"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "ON THE VERGE: no legal Scarif battleground remains in Reserve Deck"));
        }
        return result(add(
                actionId,
                "OBJECTIVE.OTVOG.SCARIF_ROUTE",
                300.0f,
                "ON THE VERGE: use the native Scarif battleground route before unrelated deploys"));
    }

    public static PolicyResult scoreScarifBattlegroundCandidate(
            String actionId, boolean exactDecision,
            boolean commandCenterCandidate) {
        return exactDecision && commandCenterCandidate
                ? result(add(
                    actionId,
                    "OBJECTIVE.OTVOG.COMMAND_CENTER",
                    300.0f,
                    "ON THE VERGE: deploy Command Center to unlock the source-verified Krennic route"))
                : empty();
    }

    public static PolicyResult scoreKrennicRoute(
            String actionId, boolean exactAction,
            boolean legalKrennicAvailable,
            Integer forceAvailable, Integer deployCost,
            Integer moveReserve) {
        if (!exactAction) return empty();
        if (!legalKrennicAvailable) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.OTVOG.KRENNIC_ROUTE_EXHAUSTED"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "ON THE VERGE: no legal Krennic remains in Reserve Deck"));
        }
        if (deployCost == null) {
            return empty();
        }
        if (forceAvailable == null) {
            return empty();
        }
        if (moveReserve == null) {
            return empty();
        }
        int payment = Math.max(0, deployCost);
        if (forceAvailable < payment) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.OTVOG.KRENNIC_UNAFFORDABLE"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "ON THE VERGE: Krennic is not currently affordable"));
        }
        if (forceAvailable - payment < Math.max(0, moveReserve)) {
            return result(add(
                    actionId,
                    "OBJECTIVE.OTVOG.KRENNIC_MOVE_RESERVE",
                    -300.0f,
                    "ON THE VERGE: preserve the exact Death Star movement payment before deploying Krennic"));
        }
        return result(add(
                actionId,
                "OBJECTIVE.OTVOG.KRENNIC_ROUTE",
                300.0f,
                "ON THE VERGE: deploy Krennic while preserving the exact Death Star movement payment"));
    }

    public static PolicyResult scoreKrennicCandidate(
            String actionId, boolean exactDecision,
            boolean krennicCandidate) {
        return exactDecision && krennicCandidate
                ? result(add(
                    actionId,
                    "OBJECTIVE.OTVOG.KRENNIC_CANDIDATE",
                    300.0f,
                    "ON THE VERGE: select the source-legal Krennic printing"))
                : empty();
    }

    public static PolicyResult scoreBackRetrieval(
            String actionId, boolean exactAction,
            boolean legalTargetAvailable) {
        if (!exactAction) return empty();
        if (!legalTargetAvailable) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.OTVOG.RETRIEVAL_EXHAUSTED"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "TAKING CONTROL: no non-unique ability card is in Lost Pile"));
        }
        return result(add(
                actionId,
                "OBJECTIVE.OTVOG.BACK_RETRIEVAL",
                300.0f,
                "TAKING CONTROL: use the free once-per-draw retrieval"));
    }

    public static PolicyResult scoreVaderBattleReaction(
            String actionId, boolean exactAction,
            boolean safeCandidateAvailable) {
        if (!exactAction) return empty();
        if (!safeCandidateAvailable) {
            return result(PolicyOperation.add(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.OTVOG.VADER_REACTION_BREAKS_HOLD"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    -300.0f,
                    "TAKING CONTROL: prefer not to move the only Scarif leader away and flip the objective"));
        }
        return result(add(
                actionId,
                "OBJECTIVE.OTVOG.VADER_BATTLE_REACTION",
                300.0f,
                "TAKING CONTROL: move Vader to the battle offered by the objective"));
    }

    public static PolicyResult scoreVaderBattleReactionCandidate(
            String actionId, boolean exactDecision,
            boolean safeVaderCandidate) {
        if (!exactDecision) return empty();
        if (!safeVaderCandidate) {
            return result(PolicyOperation.add(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.OTVOG.VADER_CANDIDATE_BREAKS_HOLD"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    -300.0f,
                    "TAKING CONTROL: prefer a Vader move that preserves the Scarif leader hold"));
        }
        return result(add(
                actionId,
                "OBJECTIVE.OTVOG.VADER_BATTLE_REACTION_CANDIDATE",
                300.0f,
                "TAKING CONTROL: select Vader for the objective's battle reaction"));
    }

    private static PolicyOperation add(
            String actionId, String rule,
            float delta, String reason) {
        return PolicyOperation.add(
                actionId,
                TraceRuleId.of(rule),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                delta, reason);
    }

    private static PolicyResult result(PolicyOperation operation) {
        return new PolicyResult(
                "ON_THE_VERGE_OBJECTIVE_POLICY",
                List.of(operation));
    }

    private static PolicyResult empty() {
        return new PolicyResult(
                "ON_THE_VERGE_OBJECTIVE_POLICY",
                List.of());
    }
}
