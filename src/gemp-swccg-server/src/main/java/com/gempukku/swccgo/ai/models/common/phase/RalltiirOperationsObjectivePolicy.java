package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;

/** Exact objective policy for 7_300 and 7_300_BACK. */
public final class RalltiirOperationsObjectivePolicy {

    private RalltiirOperationsObjectivePolicy() {
    }

    public static PolicyResult scoreFrontRoute(
            String actionId, boolean routeReady,
            boolean routeExhausted) {
        if (routeExhausted) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.RALLTIIR.ROUTE_EXHAUSTED"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "RALLTIIR OPERATIONS: no legal site or non-unique Imperial route remains in Reserve Deck"));
        }
        return routeReady
                ? result(add(
                    actionId,
                    "OBJECTIVE.RALLTIIR.FRONT_ROUTE",
                    TraceOutputKind.BANDED,
                    2000.0f,
                    "RALLTIIR OPERATIONS: use the exact site-or-Imperial route before unrelated deploys"))
                : empty();
    }

    public static PolicyResult scoreFrontPullCandidate(
            String actionId, int priority) {
        if (priority <= 0) return empty();
        boolean siteStage = priority >= 3;
        return result(add(
                actionId,
                siteStage
                    ? "OBJECTIVE.RALLTIIR.PULL_SITE_STAGE"
                    : "OBJECTIVE.RALLTIIR.PULL_IMPERIAL_STAGE",
                TraceOutputKind.BANDED,
                siteStage ? 1200.0f : 1000.0f,
                siteStage
                    ? "RALLTIIR OPERATIONS: build enough usable geography before pulling another Imperial"
                    : "RALLTIIR OPERATIONS: pull a non-unique Imperial that can qualify an open Ralltiir site"));
    }

    public static PolicyResult scoreBackAnyCardTutor(
            String actionId, boolean exactTutor) {
        return exactTutor
                ? result(add(
                    actionId,
                    "OBJECTIVE.RALLTIIR.BACK_ANY_CARD_TUTOR",
                    TraceOutputKind.BANDED,
                    2000.0f,
                    "IN THE HANDS OF THE EMPIRE: spend 2 Force for the source-verified any-card tutor"))
                : empty();
    }

    public static PolicyResult scoreBackTutorCandidate(
            String actionId, boolean urgentHoldCandidate) {
        return urgentHoldCandidate
                ? result(add(
                    actionId,
                    "OBJECTIVE.RALLTIIR.BACK_HOLD_REINFORCEMENT",
                    TraceOutputKind.BANDED,
                    1200.0f,
                    "IN THE HANDS OF THE EMPIRE: tutor legal presence for the one opponent-controlled Ralltiir location before the objective flips back"))
                : empty();
    }

    public static PolicyResult preserveFrontProgressDeployDestination(
            String actionId, boolean progressDestinationOffered,
            boolean thisDestinationAdvances) {
        if (!progressDestinationOffered || thisDestinationAdvances) {
            return empty();
        }
        return result(PolicyOperation.hardVeto(
                actionId,
                TraceRuleId.of(
                        "OBJECTIVE.RALLTIIR.WRONG_DEPLOY_DESTINATION"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.VETO,
                "RALLTIIR OPERATIONS: deploy the selected Imperial to an open objective site instead of reinforcing an already-qualified site"));
    }

    public static PolicyResult preserveRouteForceForOrdinaryDeploy(
            String actionId, boolean ordinaryDeploy,
            int routeForceReserve, int forceAvailable,
            Integer exactPayment, int fallbackPayment) {
        if (!ordinaryDeploy || routeForceReserve <= 0) {
            return empty();
        }
        int fallback = Math.max(0, fallbackPayment);
        if (exactPayment == null) {
            return fallback > 0
                    && forceAvailable - fallback < routeForceReserve
                    ? result(PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                            "OBJECTIVE.RALLTIIR.ROUTE_PAYMENT_UNKNOWN"),
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.VETO,
                        "RALLTIIR OPERATIONS: cannot prove this deploy preserves the exact route payments"))
                    : empty();
        }
        int payment = Math.max(0, exactPayment);
        if (payment == 0
                || forceAvailable - payment >= routeForceReserve) {
            return empty();
        }
        return result(PolicyOperation.hardVeto(
                actionId,
                TraceRuleId.of(
                        "OBJECTIVE.RALLTIIR.ROUTE_FORCE_RESERVE"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.VETO,
                "RALLTIIR OPERATIONS: preserve Force for the current pull, battle, and movement chain"));
    }

    private static PolicyOperation add(
            String actionId, String rule,
            TraceOutputKind kind, float delta, String reason) {
        return PolicyOperation.add(
                actionId,
                TraceRuleId.of(rule),
                TraceDomainId.OBJECTIVE_INTENT,
                kind, delta, reason);
    }

    private static PolicyResult result(PolicyOperation operation) {
        return new PolicyResult(
                "RALLTIIR_OPERATIONS_OBJECTIVE_POLICY",
                List.of(operation));
    }

    private static PolicyResult empty() {
        return new PolicyResult(
                "RALLTIIR_OPERATIONS_OBJECTIVE_POLICY",
                List.of());
    }
}
