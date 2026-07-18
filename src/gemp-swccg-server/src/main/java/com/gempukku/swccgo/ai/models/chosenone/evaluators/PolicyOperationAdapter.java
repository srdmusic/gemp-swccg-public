package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;

import java.util.Objects;

/** Applies one shared AI policy stream to ChosenOne's existing score choke point. */
public final class PolicyOperationAdapter {
    private PolicyOperationAdapter() {
    }

    public static int apply(EvaluatedAction action, PolicyContributionLedger ledger) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(ledger, "ledger");
        int applied = 0;
        for (PolicyOperation operation : ledger.operationsFor(action.getActionId())) {
            apply(action, operation);
            applied++;
        }
        return applied;
    }

    private static void apply(EvaluatedAction action, PolicyOperation operation) {
        switch (operation.kind()) {
            case ADD -> action.addReasoning(operation.reason(), operation.delta(),
                    operation.ruleArmId(), operation.domainId(), operation.outputKind());
            case HARD_VETO -> action.hardVeto(operation.reason(), operation.ruleArmId(),
                    operation.domainId(), operation.outputKind());
            case DEFER -> action.defer(operation.reason(), operation.delta(),
                    operation.ruleArmId(), operation.domainId(), operation.outputKind());
        }
    }
}
