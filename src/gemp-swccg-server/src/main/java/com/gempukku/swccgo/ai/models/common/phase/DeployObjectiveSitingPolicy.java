package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared pure owner of objective-specific DEPLOY-2 destination scores. */
public final class DeployObjectiveSitingPolicy {
    private DeployObjectiveSitingPolicy() {
    }

    public static PolicyResult evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (!facts.undercoverSpy() && facts.objectiveAnalyzed()
                && facts.objectiveRelevant()) {
            operations.add(add(facts.actionId(), "V22-objective-location",
                    TraceOutputKind.BANDED, facts.objectiveLocationBonus(),
                    "OBJECTIVE LOCATION - deploy here helps flip!"));
        }

        if (facts.objectiveAnalyzed() && facts.myLord() && facts.senator()) {
            if (facts.galacticSenateDestination()) {
                operations.add(add(facts.actionId(), "V88-CS",
                        TraceOutputKind.BANDED, 1500.0f,
                        "V88 MY LORD: senator → Galactic Senate (flip target + weapon destiny -6 protection)"));
            } else {
                operations.add(add(facts.actionId(), "V88-CS",
                        TraceOutputKind.VETO, -2000.0f,
                        "V88 MY LORD: senator not at Galactic Senate — wrong site!"));
            }
        }

        if (facts.textNamesDestination()) {
            if (facts.textRejectsDestination()) {
                operations.add(add(facts.actionId(), "V88-text-named",
                        TraceOutputKind.BANDED, -500.0f,
                        "V88 TEXT-NAMED SITE: character text says NOT at '"
                                + facts.bareSiteTitle() + "' — wrong site"));
            } else if (!facts.textNamedDestinationDoomed()) {
                operations.add(add(facts.actionId(), "V88-text-named",
                        TraceOutputKind.BANDED, 500.0f,
                        "V88 TEXT-NAMED SITE: character text mentions '"
                                + facts.bareSiteTitle() + "' — home-site bonus"));
            }
        }

        if (facts.galacticSenateDestination() && facts.character()
                && !facts.senator()
                && facts.opponentPower() <= facts.friendlySenatorPower()) {
            operations.add(add(facts.actionId(), "V99-CS",
                    TraceOutputKind.VETO, -1500.0f,
                    String.format("V99 SENATE GUARD: non-senator → Galactic Senate (opp %.0f <= my senator %.0f) — wasted, deploy elsewhere",
                            facts.opponentPower(), facts.friendlySenatorPower())));
        }

        return new PolicyResult("DEPLOY_OBJECTIVE_SITING_POLICY", operations);
    }

    public record Facts(String actionId, boolean undercoverSpy,
                        boolean objectiveAnalyzed, boolean objectiveRelevant,
                        float objectiveLocationBonus, boolean myLord,
                        boolean senator, boolean character,
                        boolean galacticSenateDestination,
                        boolean textNamesDestination,
                        boolean textRejectsDestination,
                        boolean textNamedDestinationDoomed,
                        String bareSiteTitle, float opponentPower,
                        float friendlySenatorPower) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            bareSiteTitle = bareSiteTitle == null ? "" : bareSiteTitle;
        }
    }

    private static PolicyOperation add(String actionId, String ruleId,
                                       TraceOutputKind outputKind,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, outputKind, delta, reason);
    }
}
