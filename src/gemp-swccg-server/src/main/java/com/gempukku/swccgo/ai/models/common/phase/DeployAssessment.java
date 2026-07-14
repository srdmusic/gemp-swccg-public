package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable compatibility assessment consumed by every owned DEPLOY stage. */
public record DeployAssessment(
        DeployRoute route,
        DeployFormationAssessment formation,
        ForceObligationVector forceObligations,
        Integer ownedChoiceOrdinal) {

    public DeployAssessment {
        Objects.requireNonNull(route, "route");
        if (route == DeployRoute.LEGACY_UNOWNED) {
            throw new IllegalArgumentException("LEGACY_UNOWNED has no DeployAssessment");
        }
        Objects.requireNonNull(formation, "formation");
        Objects.requireNonNull(forceObligations, "forceObligations");
        if (route != DeployRoute.DEPLOY_V170_UNDERCOVER
                && ownedChoiceOrdinal != null) {
            throw new IllegalArgumentException("only V170 owns a typed choice ordinal");
        }
    }

    public static DeployAssessment compatibility(DeployFacts facts,
                                                 DeployFormationAssessment formation,
                                                 ForceObligationVector forceObligations) {
        Objects.requireNonNull(facts, "facts");
        Integer ordinal = null;
        if (facts.route() == DeployRoute.DEPLOY_V170_UNDERCOVER) {
            if (facts.opponentActiveDrain().isUnknown()) {
                throw new IllegalArgumentException("unknown opponent drain is legacy-unowned");
            }
            int yes = uniqueResult(facts.results(), "yes");
            int no = uniqueResult(facts.results(), "no");
            if (yes < 0 || no < 0) {
                throw new IllegalArgumentException("V170 requires unique Yes and No results");
            }
            ordinal = facts.opponentActiveDrain().value() > 0f ? yes : no;
        }
        return new DeployAssessment(facts.route(), formation, forceObligations, ordinal);
    }

    private static int uniqueResult(List<String> results, String expected) {
        int found = -1;
        for (int i = 0; i < results.size(); i++) {
            String value = results.get(i);
            if (value != null && expected.equals(value.trim().toLowerCase(Locale.ROOT))) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }
}
