package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure parent-action PULL policy shared by Rando and Chosen One. */
public final class PullActionPolicy {

    private static final String PRODUCER = "PULL_ACTION_POLICY";

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep) {
        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    private PullActionPolicy() {
    }

    public static Evaluation evaluateEarlySearch(PullActionFacts.EarlySearch facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.gate() == PullActionFacts.EarlyGate.NONE) {
            return evaluation(List.of(), AdapterStep.FALL_THROUGH);
        }
        String rule = facts.gate() == PullActionFacts.EarlyGate.V177_DEAD_SEARCH
                ? "V177" : "V183";
        return evaluation(List.of(add(facts.actionId(), rule,
                TraceOutputKind.VETO, -2000.0f, facts.reason())),
                AdapterStep.CONTINUE_ACTION);
    }

    public static Evaluation evaluateParent(PullActionFacts.Parent facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        boolean hardBlocked = false;

        if (facts.reserveSize() >= 0 && facts.reserveSize() <= 2) {
            operations.add(add(facts.actionId(), "V60-reserve-risk",
                    TraceOutputKind.VETO, -9999.0f,
                    "V60 RESERVE RISK: Reserve deck has " + facts.reserveSize()
                            + " cards \u2014 pull would reveal almost everything!"));
            hardBlocked = true;
        }
        if (!hardBlocked && facts.failedTwice()) {
            operations.add(add(facts.actionId(), "V60-fail-stop",
                    TraceOutputKind.VETO, -9999.0f,
                    "V60 RESERVE FAIL-STOP: '" + facts.actionText()
                            + "' failed 2x \u2014 stop trying this game!"));
            hardBlocked = true;
        }
        if (!hardBlocked && !facts.namedMissingTarget().isEmpty()) {
            operations.add(add(facts.actionId(), "V60-named-miss",
                    TraceOutputKind.VETO, -9999.0f,
                    "V60 RESERVE MISS: '" + facts.namedMissingTarget()
                            + "' is NOT in Reserve Deck \u2014 pull will fail and reveal deck!"));
            hardBlocked = true;
        }

        PullOracleView.Validation memory = facts.memoryValidation();
        if (!hardBlocked && memory.outcome() == PullOracleView.Outcome.WILL_FAIL) {
            operations.add(add(facts.actionId(), "V66",
                    TraceOutputKind.VETO, -9999.0f,
                    "V66 MEMORY: " + memory.reason()));
            hardBlocked = true;
        } else if (!hardBlocked && memory.outcome() == PullOracleView.Outcome.WASTEFUL) {
            operations.add(add(facts.actionId(), "V66-wasteful",
                    TraceOutputKind.VETO, -800.0f,
                    "V66 MEMORY: " + memory.reason()));
        }

        PullOracleView.Validation source = facts.sourceValidation();
        if (!hardBlocked && source.outcome() == PullOracleView.Outcome.WILL_FAIL) {
            operations.add(add(facts.actionId(), "V67h",
                    TraceOutputKind.VETO, -9999.0f,
                    "V67h MEMORY (game-text): " + source.reason()));
            hardBlocked = true;
        } else if (!hardBlocked && source.outcome() == PullOracleView.Outcome.WILL_SUCCEED
                && facts.allReserveTargetsUnattachableWeapons()) {
            operations.add(add(facts.actionId(), "V185-ate",
                    TraceOutputKind.VETO, -9999.0f,
                    "V185 (ATE mirror): all Reserve targets are weapons with no legal holder \u2014 dead pull"));
            hardBlocked = true;
        }

        if (!hardBlocked && facts.cheapestTargetCost() != null
                && facts.cheapestTargetCost() > facts.availableForce()) {
            operations.add(add(facts.actionId(), "V67ac",
                    TraceOutputKind.VETO, -9999.0f,
                    String.format("V67ac CAN'T AFFORD: '%s' would deploy a target costing %d Force, only %d available \u2014 search reveals reserve!",
                            facts.sourceTitle(), facts.cheapestTargetCost(), facts.availableForce())));
            hardBlocked = true;
        }

        if (!hardBlocked && facts.deadInterrupt()) {
            operations.add(add(facts.actionId(), "V95",
                    TraceOutputKind.VETO, -2000.0f,
                    String.format("V95 DEAD INTERRUPT SAVE: '%s' pull targets %s all on table, reserves=%d \u2014 save for force-loss fodder",
                            facts.sourceTitle(), facts.deadInterruptTargets(),
                            facts.deadInterruptReserves())));
            hardBlocked = true;
        }

        if (!hardBlocked && facts.v131State() == PullActionFacts.V131State.HARD_BLOCK) {
            operations.add(add(facts.actionId(), "V131-hard",
                    TraceOutputKind.VETO, -9999.0f,
                    "V131 DECK-AWARE HARD BLOCK: " + facts.v131Reason()
                            + " \u2014 pull would fail and reveal reserve"));
            hardBlocked = true;
        }

        boolean locationTier = facts.locationPull()
                && facts.v131State() == PullActionFacts.V131State.OPEN;
        float typeTier = 0.0f;
        String typeDescription = "none";
        if (!hardBlocked && locationTier) {
            int tier = 0;
            String tierName = "unclassified";
            if (facts.sourceCategory() == CardCategory.OBJECTIVE) {
                tier = 1;
                tierName = "OBJECTIVE";
                typeTier = 1500.0f;
            } else if (facts.sourceCategory() == CardCategory.EFFECT
                    || facts.sourceCategory() == CardCategory.LOCATION) {
                tier = 2;
                tierName = facts.sourceCategory() == CardCategory.LOCATION
                        ? "LOCATION-EFFECT" : "EFFECT";
                typeTier = 1400.0f;
            } else if (facts.sourceCategory() == CardCategory.INTERRUPT) {
                tier = 3;
                tierName = "INTERRUPT";
                typeTier = 1300.0f;
            } else {
                typeTier = 1200.0f;
            }
            typeDescription = String.format("LOCATION Tier %d %s \u2014 %s",
                    tier, tierName, facts.locationReason());
        }

        if (!hardBlocked && facts.weaponPull() && !facts.locationPull()) {
            if (facts.unarmedCharacters() == 0 && facts.armedCharacters() > 0) {
                operations.add(add(facts.actionId(), "V67ar-weapon",
                        TraceOutputKind.VETO, -9999.0f,
                        String.format("V67ar UNIVERSAL BLOCK: every Rando character (%d) already armed \u2014 pulled weapon would stack a 2nd weapon (forbidden)!",
                                facts.armedCharacters())));
                hardBlocked = true;
            } else if (facts.unarmedCharacters() == 0) {
                operations.add(add(facts.actionId(), "V67ao-weapon",
                        TraceOutputKind.VETO, -9999.0f,
                        "V67ao ORDER GATE: weapon pull blocked \u2014 no Rando character on table to hold the weapon. Deploy a character first!"));
                hardBlocked = true;
            } else if (facts.lightsaberPull() && facts.capableLightsaberWielders() == 0) {
                operations.add(add(facts.actionId(), "V149",
                        TraceOutputKind.VETO, -2000.0f,
                        "V149 NO LIGHTSABER WIELDER: no unarmed [Warrior] ability-4+ character on table \u2014 don't pull a lightsaber nobody can wield"));
                hardBlocked = true;
            } else {
                typeTier = 600.0f;
                typeDescription = String.format("WEAPON (V67am value, %d unarmed target(s) \u2014 %s)",
                        facts.unarmedCharacters(), facts.weaponReason());
            }
        }

        if (!hardBlocked && facts.devicePull()
                && !facts.locationPull() && !facts.weaponPull()) {
            if (facts.deviceUnarmedCharacters() == 0
                    && facts.deviceArmedCharacters() > 0) {
                operations.add(add(facts.actionId(), "V67ar-device",
                        TraceOutputKind.VETO, -9999.0f,
                        String.format("V67ar UNIVERSAL BLOCK: every Rando character (%d) already has a device \u2014 second device on ANY character is wasteful!",
                                facts.deviceArmedCharacters())));
                hardBlocked = true;
            } else if (facts.deviceUnarmedCharacters() == 0) {
                operations.add(add(facts.actionId(), "V67ao-device",
                        TraceOutputKind.VETO, -9999.0f,
                        "V67ao ORDER GATE: device pull blocked \u2014 no Rando character on table to host the device. Deploy a character first!"));
                hardBlocked = true;
            } else {
                typeTier = 400.0f;
                typeDescription = "DEVICE (V67am value \u2014 " + facts.deviceReason() + ")";
            }
        }

        boolean downgraded = facts.v131State() == PullActionFacts.V131State.DOWNGRADE;
        if (!hardBlocked && !downgraded && !facts.keyCharacterToken().isEmpty()
                && !facts.keyCharacterAlreadyFilled()) {
            operations.add(add(facts.actionId(), "V67ak-pull",
                    TraceOutputKind.ORDERING, 800.0f,
                    String.format("V67ak KEY-CHARACTER PULL: '%s' would pull '%s' (named in objective/epic-event) \u2014 flip-critical!",
                            facts.sourceTitle(), facts.keyCharacterToken())));
        }

        if (!hardBlocked && downgraded) {
            operations.add(add(facts.actionId(), "V192-downgrade",
                    TraceOutputKind.VETO, -200.0f,
                    "V192 PULL SCORER: V131 already-satisfied \u2192 positives suppressed ("
                            + facts.v131Reason() + ")"));
        } else if (!hardBlocked) {
            boolean activateBase = facts.phase() == Phase.ACTIVATE
                    && isStaticPullSource(facts.sourceCategory())
                    && !facts.sourceTitle().contains("Knowledge And Defense")
                    && !facts.sourceTitle().contains("Anger, Fear, Aggression")
                    && !(facts.reserveSize() <= 3 && facts.battlePlausible());
            float base = activateBase ? 5500.0f : 150.0f;
            float context = facts.freeDownload() ? 50.0f : 0.0f;
            if (locationTier && facts.phase() == Phase.DEPLOY
                    && facts.charactersOrVehiclesInHand()) {
                context += 25.0f;
            }
            float total = Math.min(base + typeTier + context,
                    activateBase ? 7100.0f : 1750.0f);
            operations.add(add(facts.actionId(), "V192",
                    TraceOutputKind.ORDERING, total,
                    String.format("V192 PULL SCORER (%s): base %d + tier %d [%s] + ctx %d = %d [absorbs V60-pull/V82/V95/V97/V100/V116/V67l/V67ai/V67am/V29.7]",
                            activateBase ? "ACTIVATE" : "DEPLOY-GRADE",
                            (int) base, (int) typeTier, typeDescription,
                            (int) context, (int) total)));
            addFormation(operations, facts);
        }

        return evaluation(operations, AdapterStep.FALL_THROUGH);
    }

    private static void addFormation(List<PolicyOperation> operations,
                                     PullActionFacts.Parent facts) {
        switch (facts.formationState()) {
            case HARD_BLOCK -> operations.add(PolicyOperation.hardVeto(
                    facts.actionId(), TraceRuleId.of("FS-pull-hard"),
                    TraceDomainId.PULL_SEARCH, TraceOutputKind.VETO,
                    facts.formationReason()));
            case DEFER -> operations.add(PolicyOperation.defer(
                    facts.actionId(), TraceRuleId.of("V201-pull"),
                    TraceDomainId.PULL_SEARCH, TraceOutputKind.VETO,
                    -800.0f, facts.formationReason()));
            case UNKNOWN -> operations.add(add(facts.actionId(), "V201-pull-unknown",
                    TraceOutputKind.BANDED, 0.0f,
                    "V201 pull formation assessment unknown: " + facts.formationReason()));
            default -> {
            }
        }
    }

    private static boolean isStaticPullSource(CardCategory category) {
        return category == CardCategory.EFFECT
                || category == CardCategory.EPIC_EVENT
                || category == CardCategory.INTERRUPT
                || category == CardCategory.OBJECTIVE;
    }

    private static PolicyOperation add(String actionId, String rule,
                                       TraceOutputKind outputKind,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.PULL_SEARCH, outputKind, delta, reason);
    }

    private static Evaluation evaluation(List<PolicyOperation> operations,
                                         AdapterStep step) {
        return new Evaluation(new PolicyResult(PRODUCER, operations), step);
    }
}
