package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure DEPLOY-3 weapon and attachment scoring over adapter-produced facts. */
public final class DeployWeaponPolicy {
    private DeployWeaponPolicy() {
    }

    public static PolicyResult evaluateDirectEligibility(DirectEligibilityFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (!facts.criteria().isBlank() && facts.matchingUnarmed() == 0) {
            String why = facts.matchingArmed() > 0
                    ? String.format("every '%s' wielder (%d) already armed",
                            facts.criteria(), facts.matchingArmed())
                    : String.format("no '%s' friendly on table at all - deploy has no legal target",
                            facts.criteria());
            add(operations, facts.actionId(), "V158", TraceOutputKind.VETO,
                    -9999.0f, "V158 WEAPON BLOCK: " + why + " - hold it");
        } else if (facts.lightsaber() && facts.unarmedWarriorAbilityFour() == 0) {
            add(operations, facts.actionId(), "V158", TraceOutputKind.VETO,
                    -9999.0f,
                    "V158 WEAPON BLOCK: lightsaber but no unarmed [Warrior] ability-4 wielder - hold it");
        } else if (facts.totalUnarmed() == 0 && facts.totalArmed() > 0) {
            add(operations, facts.actionId(), "V158", TraceOutputKind.VETO,
                    -9999.0f,
                    "V158 WEAPON BLOCK: every character already armed - no 2nd weapon");
        } else if (facts.totalUnarmed() > 0 || facts.matchingUnarmed() > 0) {
            add(operations, facts.actionId(), "V158", TraceOutputKind.BANDED,
                    300.0f,
                    "V158 WEAPON DEPLOY: unarmed wielder available - arm them");
        }
        return new PolicyResult("DEPLOY_WEAPON_ELIGIBILITY_POLICY", operations);
    }

    public static PolicyResult evaluateNamedPriority(NamedPriorityFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.namedWeapon()) {
            add(operations, facts.actionId(), "V33-named-weapon",
                    TraceOutputKind.BANDED, 200.0f,
                    "V33 NAMED WEAPON: Character-specific weapon - deploy priority!");
        } else if (!facts.targetCharacterName().isBlank()
                && !facts.namedWeaponInHandTitle().isBlank()) {
            add(operations, facts.actionId(), "V33-named-weapon-wait",
                    TraceOutputKind.BANDED, -400.0f,
                    String.format("V33 NAMED WEAPON WAIT: %s has named weapon %s in hand - save the slot!",
                            facts.targetCharacterName(), facts.namedWeaponInHandTitle()));
        }
        return new PolicyResult("DEPLOY_NAMED_WEAPON_POLICY", operations);
    }

    public static PolicyResult evaluateReserveTarget(ReserveTargetFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.targetAlreadyArmed()) {
            add(operations, facts.actionId(), "V158-reserve-target",
                    TraceOutputKind.VETO, -9999.0f,
                    String.format("V158 RESERVE-DEPLOY BLOCK: %s already armed - no 2nd weapon (reserve-deploy bypass guard)",
                            facts.targetTitle()));
        }
        return new PolicyResult("DEPLOY_RESERVE_WEAPON_TARGET_POLICY", operations);
    }

    public static PolicyResult evaluateReserveWielder(ReserveWielderFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (!facts.persona().isBlank() && !facts.personaOnTable()) {
            add(operations, facts.actionId(), "V158-no-wielder",
                    TraceOutputKind.VETO, -9999.0f,
                    String.format("V158 NO-WIELDER BLOCK: %s's Lightsaber from Reserve but no '%s' on table - wasted pull, the saber will sit in hand and bleed out",
                            facts.persona(), facts.persona()));
        }
        return new PolicyResult("DEPLOY_RESERVE_WEAPON_WIELDER_POLICY", operations);
    }

    public static PolicyResult evaluatePullCriteria(PullCriteriaFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (!facts.weaponName().isBlank() && !facts.criteria().isBlank()
                && facts.matchingUnarmed() == 0) {
            String why = facts.matchingArmed() > 0
                    ? String.format("every '%s' friendly (%d) already armed",
                            facts.criteria(), facts.matchingArmed())
                    : String.format("no '%s' friendly on table - deploy will fail",
                            facts.criteria());
            add(operations, facts.actionId(), "V120", TraceOutputKind.VETO,
                    -9999.0f,
                    "V120 WEAPON-PULL BLOCK: '" + facts.weaponName() + "' - "
                            + why + " (will reveal reserve)");
        }
        return new PolicyResult("DEPLOY_WEAPON_PULL_CRITERIA_POLICY", operations);
    }

    public record DirectEligibilityFacts(String actionId, String criteria,
                                         boolean lightsaber, int totalArmed,
                                         int totalUnarmed, int matchingArmed,
                                         int matchingUnarmed,
                                         int unarmedWarriorAbilityFour) {
        public DirectEligibilityFacts {
            Objects.requireNonNull(actionId, "actionId");
            criteria = criteria == null ? "" : criteria;
        }
    }

    public record NamedPriorityFacts(String actionId, boolean namedWeapon,
                                     String targetCharacterName,
                                     String namedWeaponInHandTitle) {
        public NamedPriorityFacts {
            Objects.requireNonNull(actionId, "actionId");
            targetCharacterName = targetCharacterName == null ? "" : targetCharacterName;
            namedWeaponInHandTitle = namedWeaponInHandTitle == null ? "" : namedWeaponInHandTitle;
        }
    }

    public record ReserveTargetFacts(String actionId, String targetTitle,
                                     boolean targetAlreadyArmed) {
        public ReserveTargetFacts {
            Objects.requireNonNull(actionId, "actionId");
            targetTitle = targetTitle == null ? "" : targetTitle;
        }
    }

    public record ReserveWielderFacts(String actionId, String persona,
                                      boolean personaOnTable) {
        public ReserveWielderFacts {
            Objects.requireNonNull(actionId, "actionId");
            persona = persona == null ? "" : persona;
        }
    }

    public record PullCriteriaFacts(String actionId, String weaponName,
                                    String criteria, int matchingArmed,
                                    int matchingUnarmed) {
        public PullCriteriaFacts {
            Objects.requireNonNull(actionId, "actionId");
            weaponName = weaponName == null ? "" : weaponName;
            criteria = criteria == null ? "" : criteria;
        }
    }

    private static void add(List<PolicyOperation> operations, String actionId,
                            String ruleId, TraceOutputKind outputKind,
                            float delta, String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_ATTACH, outputKind, delta, reason));
    }
}
