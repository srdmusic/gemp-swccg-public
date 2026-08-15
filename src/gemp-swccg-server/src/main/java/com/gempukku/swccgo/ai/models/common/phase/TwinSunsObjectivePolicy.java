package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;

/** Exact objective policy for 301_4 and 301_4_BACK. */
public final class TwinSunsObjectivePolicy {

    private TwinSunsObjectivePolicy() {
    }

    public static PolicyResult scoreFrontSiteRoute(
            String actionId, boolean routeReady,
            boolean routeExhausted) {
        if (routeExhausted) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of(
                            "OBJECTIVE.TWIN_SUNS.SITE_ROUTE_EXHAUSTED"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "TWIN SUNS: no usable Tatooine battleground site remains in Reserve Deck"));
        }
        return routeReady
                ? result(add(
                    actionId,
                    "OBJECTIVE.TWIN_SUNS.SITE_ROUTE",
                    TraceOutputKind.BANDED,
                    300.0f,
                    "TWIN SUNS: spend 1 Force on the exact battleground-site route before unrelated deploys"))
                : empty();
    }

    public static PolicyResult scoreOccupationRoute(
            String actionId, boolean routeReady) {
        return routeReady
                ? result(add(
                    actionId,
                    "OBJECTIVE.TWIN_SUNS.TATOOINE_OCCUPATION",
                    TraceOutputKind.BANDED,
                    300.0f,
                    "TWIN SUNS BACK: use the once-per-game Tatooine Occupation route"))
                : empty();
    }

    public static PolicyResult scorePeek(
            String actionId, boolean exactAction) {
        return exactAction
                ? result(add(
                    actionId,
                    "OBJECTIVE.TWIN_SUNS.PEEK",
                    TraceOutputKind.ORDERING,
                    300.0f,
                    "TWIN SUNS BACK: take the free control-phase Reserve Deck selection"))
                : empty();
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
                    ? result(add(
                        actionId,
                        "OBJECTIVE.TWIN_SUNS.ROUTE_PAYMENT_UNKNOWN",
                        TraceOutputKind.BANDED,
                        -300.0f,
                        "TWIN SUNS: cannot prove this deploy preserves the exact site and movement payments"))
                    : empty();
        }
        int payment = Math.max(0, exactPayment);
        if (payment == 0
                || forceAvailable - payment >= routeForceReserve) {
            return empty();
        }
        return result(add(
                actionId,
                "OBJECTIVE.TWIN_SUNS.ROUTE_FORCE_RESERVE",
                TraceOutputKind.BANDED,
                -300.0f,
                "TWIN SUNS: preserve exact Force for the next site pull or formation move"));
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
                "TWIN_SUNS_OBJECTIVE_POLICY",
                List.of(operation));
    }

    private static PolicyResult empty() {
        return new PolicyResult(
                "TWIN_SUNS_OBJECTIVE_POLICY",
                List.of());
    }
}
