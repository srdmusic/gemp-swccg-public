package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure scoring policy for stock "take card into hand" child decisions. */
public final class PullTakeCandidatePolicy {
    private PullTakeCandidatePolicy() {
    }

    public static PolicyResult evaluate(PullTakeCandidateFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        String id = facts.actionId();
        String title = facts.cardTitle();
        String lower = title.toLowerCase(Locale.ROOT);

        addDestiny(operations, facts);
        if (facts.priorityScoreByTitle() != null) {
            operations.add(add(id, "pull-priority-title",
                    facts.priorityScoreByTitle() * 0.5f,
                    "Priority card: " + title));
        } else if (facts.priorityScoreByBlueprint() != null) {
            operations.add(add(id, "pull-priority-blueprint",
                    facts.priorityScoreByBlueprint() * 0.5f,
                    "Priority card (by ID: " + facts.blueprintId() + ")"));
        }

        if (facts.category() == CardCategory.CHARACTER && facts.power() != null) {
            if (facts.power() >= 6.0f) {
                operations.add(add(id, "pull-power-high", 30.0f,
                        "High power character (" + facts.power() + ")"));
            } else if (facts.power() >= 4.0f) {
                operations.add(add(id, "pull-power-strong", 15.0f,
                        "Strong character (" + facts.power() + ")"));
            }
        }
        if (facts.category() == CardCategory.CHARACTER && facts.ability() != null
                && facts.ability() >= 4.0f) {
            operations.add(add(id, "pull-ability", 25.0f,
                    "High ability (" + facts.ability() + ") - draws battle destiny"));
        }

        if (facts.category() == CardCategory.LOCATION) {
            operations.add(add(id, "pull-location-turn",
                    facts.turnNumber() <= 3 ? 20.0f : 5.0f,
                    facts.turnNumber() <= 3 ? "Location (good early game)" : "Location"));
            operations.add(add(id, "V22.6-location", 500.0f,
                    "V22.6 LOCATION PRIORITY: locations are prerequisites "
                            + "\u2014 pull before effects/characters"));
        }

        if (facts.admiralPull()) {
            if (lower.contains("piett")) {
                operations.add(add(id, "V24.12-piett", 300.0f,
                        "V24.12 ADMIRAL PULL: Piett is #1 pick "
                                + "\u2014 matches Executor for AMSD!"));
            } else if (lower.contains("chiraneau")) {
                operations.add(add(id, "V24.12-chiraneau", 150.0f,
                        "V24.12 ADMIRAL PULL: Chiraneau is backup "
                                + "\u2014 can pilot Executor manually"));
            } else if (lower.contains("ozzel")) {
                operations.add(add(id, "V24.12-ozzel", 100.0f,
                        "V24.12 ADMIRAL PULL: Ozzel matches Executor "
                                + "\u2014 decent AMSD option"));
            }
        }
        if (facts.commanderPull() && lower.contains("gherant")) {
            operations.add(add(id, "V24.1-gherant", 400.0f,
                    "V24.1 PIETT PULL: Gherant deploys an Executor site "
                            + "\u2014 free location + force generation!"));
        }

        if (facts.objectiveNeedsBespinPresence()) {
            if (lower.contains("lando")) {
                if (facts.friendlyAtCloudCity()) {
                    operations.add(addObjective(id, "V24.2-lando", 250.0f,
                            "V24.2 TDIGWATT: Lando is KEY "
                                    + "\u2014 moves to 3rd CC site for extra drains + occupation!"));
                } else if (facts.handBuddy() && facts.availableForce() >= 5) {
                    operations.add(addObjective(id, "V47-lando-buddy", 250.0f,
                            "V47 LANDO PULL OK: No CC friendlies but have char in hand "
                                    + "+ force to deploy both!"));
                } else {
                    operations.add(add(id, "V47-lando-block", -9999.0f,
                            "V47 LANDO PULL BLOCK: No friendlies at CC, no buddy in hand "
                                    + "or not enough force \u2014 Lando would die alone!"));
                }
            } else if (lower.contains("lobot")) {
                operations.add(addObjective(id, "V24.2-lobot", 200.0f,
                        "V24.2 TDIGWATT: Lobot deploys cheap "
                                + "\u2014 helps flip objective!"));
            }
        }

        if (facts.downloadLocationEnabler()) {
            operations.add(add(id, "pull-download-enabler", 500.0f,
                    "DOWNLOAD ENABLER: [download] of a location in game text "
                            + "\u2014 pulling builds a deploy chain"));
        }
        return new PolicyResult("PULL_TAKE_CANDIDATE_POLICY", operations);
    }

    private static void addDestiny(List<PolicyOperation> operations,
                                   PullTakeCandidateFacts facts) {
        Float destiny = facts.destiny();
        if (destiny == null) {
            return;
        }
        if (destiny >= 6.0f) {
            operations.add(add(facts.actionId(), "pull-destiny-excellent", 60.0f,
                    "Excellent destiny (" + destiny + ")"));
        } else if (destiny >= 5.0f) {
            operations.add(add(facts.actionId(), "pull-destiny-high", 40.0f,
                    "High destiny (" + destiny + ")"));
        } else if (destiny >= 4.0f) {
            operations.add(add(facts.actionId(), "pull-destiny-good", 20.0f,
                    "Good destiny (" + destiny + ")"));
        } else if (destiny >= 3.0f) {
            operations.add(add(facts.actionId(), "pull-destiny-decent", 5.0f,
                    "Decent destiny (" + destiny + ")"));
        } else if (destiny <= 1.0f) {
            operations.add(add(facts.actionId(), "pull-destiny-low", -20.0f,
                    "Low destiny (" + destiny + ")"));
        }
    }

    private static PolicyOperation add(String actionId, String rule,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.PULL_SEARCH, TraceOutputKind.ORDERING, delta, reason);
    }

    private static PolicyOperation addObjective(
            String actionId, String rule, float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.ORDERING, delta, reason);
    }
}
