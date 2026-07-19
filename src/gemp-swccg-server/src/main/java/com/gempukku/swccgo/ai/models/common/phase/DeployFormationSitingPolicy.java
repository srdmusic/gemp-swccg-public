package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure DEPLOY-2 formation and destination scoring over adapter-produced facts. */
public final class DeployFormationSitingPolicy {

    private static final String PRODUCER_ID = "DEPLOY_FORMATION_SITING_POLICY";
    private DeployFormationSitingPolicy() {
    }

    /** Emits the live formation operations in legacy source order. */
    public static PolicyResult evaluate(String actionId,
                                        CharacterFormationFacts formation) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(formation, "formation");

        List<PolicyOperation> operations = new ArrayList<>(3);
        addCommittedReinforcement(operations, actionId, formation);
        addBuddyTopology(operations, actionId, formation);
        addSoloVulnerability(operations, actionId, formation);
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static PolicyResult evaluateCommittedReinforcement(
            String actionId, CharacterFormationFacts formation) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(formation, "formation");

        List<PolicyOperation> operations = new ArrayList<>(1);
        addCommittedReinforcement(operations, actionId, formation);
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static PolicyResult evaluateBuddyTopology(
            String actionId, CharacterFormationFacts formation) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(formation, "formation");

        List<PolicyOperation> operations = new ArrayList<>(2);
        addBuddyTopology(operations, actionId, formation);
        addSoloVulnerability(operations, actionId, formation);
        return new PolicyResult(PRODUCER_ID, operations);
    }

    private static void addCommittedReinforcement(
            List<PolicyOperation> operations, String actionId,
            CharacterFormationFacts facts) {
        if (!facts.eligible()) {
            return;
        }

        float deficit = facts.opponentPower() - facts.ourPower();
        boolean outgunned = deficit >= 4.0f && deficit <= 5.0f;
        if (facts.ourPower() > 0.0f
                && facts.friendlyCharactersHere() >= 1
                && outgunned
                && !facts.committedFormationCanEscape()) {
            addDeploySiting(operations, actionId, "V67bn", 800.0f,
                    String.format(
                            "V67bn REINFORCE OUTGUNNED (Braveheart): %d friendly char(s) at %s (our %d vs opp %d, deficit %d) \u2014 NO ESCAPE, DEPLOY HERE to minimize overflow!",
                            facts.friendlyCharactersHere(), facts.destinationTitle(),
                            (int) facts.ourPower(), (int) facts.opponentPower(),
                            (int) deficit));
        }
    }

    private static void addBuddyTopology(List<PolicyOperation> operations,
                                         String actionId,
                                         CharacterFormationFacts facts) {
        if (!facts.eligible()) {
            return;
        }

        if (facts.ourLocation()) {
            addSoloFormation(operations, actionId, "V29.5-buddy", 40.0f,
                    "V29.5 BUDDY: Own location \u2014 home field advantage");
        } else if (facts.opponentLocation()) {
            if (facts.emptyTable()) {
                addSoloFormation(operations, actionId, "V29.5-buddy", -20.0f,
                        "V29.6 BUDDY: Opponent's location but empty table \u2014 must deploy somewhere!");
            } else if (facts.friendlyCharactersHere() == 0
                    && facts.opponentCharactersHere() == 0) {
                addSoloFormation(operations, actionId, "V29.5-buddy", -150.0f,
                        "V29.5 BUDDY: Opponent's location, deploying ALONE \u2014 risky!");
            } else if (facts.friendlyCharactersHere() == 0
                    && facts.opponentCharactersHere() > 0) {
                addSoloFormation(operations, actionId, "V29.5-buddy", -100.0f,
                        "V29.5 BUDDY: Opponent's location with enemies, NO friendlies \u2014 dangerous!");
            } else if (facts.friendlyCharactersHere() > 0) {
                addSoloFormation(operations, actionId, "V29.5-buddy", 10.0f,
                        "V29.5 BUDDY: Opponent's location but friendlies present");
            }
        }
    }

    private static void addSoloVulnerability(
            List<PolicyOperation> operations, String actionId,
            CharacterFormationFacts facts) {
        if (facts.eligible()
                && facts.friendlyCharactersHere() == 0
                && !facts.emptyTable()
                && facts.deployingAbility() >= 3.0f) {
            addSoloFormation(operations, actionId, "V113", -300.0f,
                    String.format(
                            "V113 SOLO VULNERABILITY: %s (ability %.0f) alone at %s \u2014 opponent can overwhelm next turn!",
                            facts.deployingCardName(), facts.deployingAbility(),
                            facts.destinationTitle()));
        }
    }

    /** Facts for V67bn/V67bu, V29.5 buddy topology, and V113. */
    public record CharacterFormationFacts(
            boolean eligible,
            String destinationTitle,
            boolean ourLocation,
            boolean opponentLocation,
            int friendlyCharactersHere,
            int opponentCharactersHere,
            boolean emptyTable,
            String deployingCardName,
            float deployingAbility,
            float ourPower,
            float opponentPower,
            boolean committedFormationCanEscape) {
        public CharacterFormationFacts {
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
            deployingCardName = deployingCardName == null ? "" : deployingCardName;
        }
    }

    private static void addDeploySiting(List<PolicyOperation> operations,
                                        String actionId, String ruleId,
                                        float delta, String reason) {
        add(operations, actionId, ruleId, TraceDomainId.DEPLOY_SITING,
                delta, reason);
    }

    private static void addSoloFormation(List<PolicyOperation> operations,
                                         String actionId, String ruleId,
                                         float delta, String reason) {
        add(operations, actionId, ruleId, TraceDomainId.SOLO_FORMATION,
                delta, reason);
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId, String ruleId,
                            TraceDomainId domainId, float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                domainId, TraceOutputKind.BANDED, delta, reason));
    }
}
