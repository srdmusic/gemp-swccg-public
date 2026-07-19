package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared pure owner of the bounded DEPLOY-2 siting score stream. */
public final class DeploySitingPolicy {
    private static final float V89_BAD_SITE_SCORE = -1500.0f;
    private static final float FORMATION_DEFER_SCORE = -800.0f;
    private static final float V193_DESTINATION_OFFSET = 1600.0f;

    public enum FormationState {
        ALLOW,
        HARD_BLOCK,
        DEFER_UNSUPPORTED_SOLO,
        UNKNOWN
    }

    public record Facts(
            String actionId,
            String cardTitle,
            String siteTitle,
            boolean evazanWithoutArmedFriend,
            FormationState formationState,
            String formationReason,
            float v136Score,
            boolean v193Eligible,
            float v193PlaybookWeight,
            String v193GateCardTitle,
            boolean v96Applicable,
            float friendlyPower,
            float opponentPower) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
            siteTitle = siteTitle == null ? "" : siteTitle;
            formationState = formationState == null ? FormationState.ALLOW : formationState;
            formationReason = formationReason == null ? "" : formationReason;
            v193GateCardTitle = v193GateCardTitle == null ? "" : v193GateCardTitle;
            if (formationState != FormationState.ALLOW && formationReason.isBlank()) {
                throw new IllegalArgumentException("formationReason must be nonblank for an active formation state");
            }
        }
    }

    private DeploySitingPolicy() {
    }

    /** Preserves the direct-action order: V89, V136, V193, then V96. */
    public static PolicyResult evaluateDirect(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.evazanWithoutArmedFriend()) {
            operations.add(addSiting(facts.actionId(), "V89", TraceOutputKind.VETO,
                    V89_BAD_SITE_SCORE,
                    "V89 DR. EVAZAN: '" + facts.cardTitle() + "' deploying to '"
                            + facts.siteTitle()
                            + "' with no armed friend — block (will get sniped)"));
        }

        addV136(operations, facts.actionId(), "V136", facts.siteTitle(),
                facts.v136Score(), false);

        if (facts.v193Eligible()) {
            operations.add(addSiting(facts.actionId(), "V193", TraceOutputKind.BANDED,
                    facts.v193PlaybookWeight(),
                    "V193 FLIP-GATE CONTROL: steer one body to '" + facts.siteTitle()
                            + "' to enable '" + facts.v193GateCardTitle()
                            + "' (objective flip gate)"));
        }

        addV96(operations, facts);
        return new PolicyResult("DEPLOY_SITING_DIRECT_POLICY", operations);
    }

    /** Preserves the destination order: V89-CS, formation, V136-CS, then V193-CS. */
    public static PolicyResult evaluateDestination(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.evazanWithoutArmedFriend()) {
            operations.add(addSiting(facts.actionId(), "V89-CS", TraceOutputKind.VETO,
                    V89_BAD_SITE_SCORE,
                    "V89-CS DR. EVAZAN: '" + facts.cardTitle() + "' → '"
                            + facts.siteTitle()
                            + "' with no armed friend — block (will get sniped)"));
        }

        addFormation(operations, facts);
        addV136(operations, facts.actionId(), "V136-CS", facts.siteTitle(),
                facts.v136Score(), true);

        if (facts.v193Eligible()) {
            operations.add(addSiting(facts.actionId(), "V193-CS", TraceOutputKind.BANDED,
                    facts.v193PlaybookWeight() + V193_DESTINATION_OFFSET,
                    "V193 (CS) FLIP-GATE CONTROL: steer one ability body to '"
                            + facts.siteTitle() + "' to enable '"
                            + facts.v193GateCardTitle() + "' (objective flip gate)"));
        }

        return new PolicyResult("DEPLOY_SITING_DESTINATION_POLICY", operations);
    }

    private static void addFormation(List<PolicyOperation> operations,
                                     Facts facts) {
        switch (facts.formationState()) {
            case HARD_BLOCK -> operations.add(PolicyOperation.hardVeto(
                    facts.actionId(), TraceRuleId.of("FS-L3-solo-deploy-hard"),
                    TraceDomainId.SOLO_FORMATION, TraceOutputKind.VETO,
                    facts.formationReason()));
            case DEFER_UNSUPPORTED_SOLO -> operations.add(PolicyOperation.defer(
                    facts.actionId(), TraceRuleId.of("V201-deploy-siting"),
                    TraceDomainId.SOLO_FORMATION, TraceOutputKind.VETO,
                    FORMATION_DEFER_SCORE, facts.formationReason()));
            case UNKNOWN -> operations.add(PolicyOperation.add(
                    facts.actionId(), TraceRuleId.of("V201-deploy-siting-unknown"),
                    TraceDomainId.SOLO_FORMATION, TraceOutputKind.BANDED,
                    0.0f,
                    "V201 formation assessment unknown: " + facts.formationReason()));
            default -> {
            }
        }
    }

    private static void addV136(List<PolicyOperation> operations, String actionId,
                                String rule, String siteTitle, float score,
                                boolean destinationRoute) {
        if (score == 0.0f) {
            return;
        }
        String route = destinationRoute ? " (CS)" : "";
        operations.add(addSiting(actionId, rule, TraceOutputKind.BANDED, score,
                "V136 unified deploy-site score" + route + " → "
                        + siteTitle + ": " + score));
    }

    private static void addV96(List<PolicyOperation> operations, Facts facts) {
        if (!facts.v96Applicable() || facts.opponentPower() <= 0.0f) {
            return;
        }
        float difference = facts.friendlyPower() - facts.opponentPower();
        if (difference >= -10.0f && difference <= 10.0f) {
            operations.add(addSiting(facts.actionId(), "V96", TraceOutputKind.BANDED,
                    500.0f,
                    String.format("V96 CONCENTRATE: %s contested (us %.0f vs them %.0f) — pile on for overflow battle damage!",
                            facts.siteTitle(), facts.friendlyPower(), facts.opponentPower())));
        } else if (difference > 10.0f) {
            operations.add(addSiting(facts.actionId(), "V96", TraceOutputKind.BANDED,
                    100.0f,
                    String.format("V96 CONCENTRATE: %s contested, already winning by %.0f — finish them",
                            facts.siteTitle(), difference)));
        }
    }

    private static PolicyOperation addSiting(String actionId, String rule,
                                             TraceOutputKind outputKind,
                                             float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.DEPLOY_SITING, outputKind, delta, reason);
    }
}
