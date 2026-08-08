package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.EndorOperationsTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    public record WeaponOrderEvaluation(PolicyResult result,
                                        WeaponOrderOutcome outcome) {
        public WeaponOrderEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum WeaponOrderOutcome {
        NONE,
        ALL_ARMED,
        NO_CHARACTER,
        NO_LIGHTSABER_WIELDER,
        READY
    }

    public enum TakeIntoHandKind {
        PALPATINE,
        BOUNCE,
        RESERVE_LOG_ONLY,
        LOST_PILE_NO_MATCH,
        LOST_PILE_MATCH,
        GENERIC
    }

    public record TakeIntoHandFacts(String actionId, TakeIntoHandKind kind) {
        public TakeIntoHandFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(kind, "kind");
        }
    }

    public record WeaponOrderFacts(
            String actionId, boolean weaponPull, boolean locationPull,
            int unarmedCharacters, int armedCharacters,
            boolean lightsaberPull, int capableLightsaberWielders) {
        public WeaponOrderFacts {
            Objects.requireNonNull(actionId, "actionId");
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

    public static WeaponOrderEvaluation evaluateWeaponOrder(
            WeaponOrderFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        WeaponOrderOutcome outcome = WeaponOrderOutcome.NONE;

        if (facts.weaponPull() && !facts.locationPull()) {
            if (facts.unarmedCharacters() == 0 && facts.armedCharacters() > 0) {
                operations.add(add(facts.actionId(), "V67ar-weapon",
                        TraceOutputKind.VETO, -9999.0f, String.format(
                                "V67ar UNIVERSAL BLOCK: every Rando character (%d) already armed \u2014 pulled weapon would stack a 2nd weapon (forbidden)!",
                                facts.armedCharacters())));
                outcome = WeaponOrderOutcome.ALL_ARMED;
            } else if (facts.unarmedCharacters() == 0) {
                operations.add(add(facts.actionId(), "V67ao-weapon",
                        TraceOutputKind.VETO, -9999.0f,
                        "V67ao ORDER GATE: weapon pull blocked \u2014 no Rando character on table to hold the weapon. Deploy a character first!"));
                outcome = WeaponOrderOutcome.NO_CHARACTER;
            } else if (facts.lightsaberPull()
                    && facts.capableLightsaberWielders() == 0) {
                operations.add(add(facts.actionId(), "V149",
                        TraceOutputKind.VETO, -2000.0f,
                        "V149 NO LIGHTSABER WIELDER: no unarmed [Warrior] ability-4+ character on table \u2014 don't pull a lightsaber nobody can wield"));
                outcome = WeaponOrderOutcome.NO_LIGHTSABER_WIELDER;
            } else {
                outcome = WeaponOrderOutcome.READY;
            }
        }

        return new WeaponOrderEvaluation(
                new PolicyResult(PRODUCER, operations), outcome);
    }

    public static PolicyResult scoreTakeIntoHand(TakeIntoHandFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = switch (facts.kind()) {
            case PALPATINE -> List.of(add(facts.actionId(),
                    "PULL-take-palpatine", TraceOutputKind.ORDERING,
                    -30.0f, "Avoid taking Palpatine"));
            case BOUNCE -> List.of(add(facts.actionId(), "V29.7-bounce",
                    TraceOutputKind.ORDERING, -300.0f,
                    "V29.7 BOUNCE: Return own card from table to hand \u2014 DON'T undo your deploy!"));
            case RESERVE_LOG_ONLY -> List.of();
            case LOST_PILE_NO_MATCH -> List.of(add(facts.actionId(),
                    "V63-lost-pile-no-match", TraceOutputKind.VETO,
                    -9999.0f,
                    "V63 LOST PILE EMPTY: no matching target in Lost Pile \u2014 search will FAIL and reveal our pile!"));
            case LOST_PILE_MATCH -> List.of(add(facts.actionId(),
                    "PULL-take-lost-pile", TraceOutputKind.ORDERING,
                    30.0f, "Take card into hand from Lost Pile"));
            case GENERIC -> List.of(add(facts.actionId(), "PULL-take-generic",
                    TraceOutputKind.ORDERING, 30.0f,
                    "Take card into hand"));
        };
        return new PolicyResult(PRODUCER, operations);
    }

    public static PolicyResult scoreImperialEntanglementsBackSiteAction(
            String actionId, boolean atRiskRouteReady) {
        Objects.requireNonNull(actionId, "actionId");
        return new PolicyResult(
                PRODUCER,
                atRiskRouteReady
                    ? List.of(add(
                        actionId,
                        "OBJECTIVE.IMPERIAL_ENTANGLEMENTS.BACK_SITE_ROUTE",
                        TraceOutputKind.ORDERING,
                        1800.0f,
                        "IMPERIAL ENTANGLEMENTS BACK: use the free Tatooine battleground route before the relative count slips"))
                    : List.of());
    }

    public static PolicyResult scoreNoMoneyNoPartsWattoRoute(
            String actionId, boolean routeReady) {
        Objects.requireNonNull(actionId, "actionId");
        return new PolicyResult(
                PRODUCER,
                routeReady
                    ? List.of(add(
                        actionId,
                        "OBJECTIVE.NO_MONEY.WATTO_ROUTE",
                        TraceOutputKind.BANDED,
                        2000.0f,
                        "NO MONEY: use Watto's Junkyard's exact free Watto route before unrelated deploys"))
                    : List.of());
    }

    public static PolicyResult scoreShadowCollectiveRoutePull(
            String actionId, boolean routeReady) {
        Objects.requireNonNull(actionId, "actionId");
        return new PolicyResult(
                PRODUCER,
                routeReady
                    ? List.of(add(
                        actionId,
                        "OBJECTIVE.SHADOW_COLLECTIVE.ROUTE_PULL",
                        TraceOutputKind.BANDED,
                        2000.0f,
                        "SHADOW COLLECTIVE: pull a legal blaster or First Light route card before the unrelated Maul route"))
                    : List.of());
    }

    public static Evaluation evaluateParent(PullActionFacts.Parent facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        boolean hardBlocked = false;

        if (!facts.requiredOnTableCardPullVetoBypass()
                && !facts.objectiveRoutePullVetoBypass()
                && facts.reserveSize() >= 0
                && facts.reserveSize() <= 2) {
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

        // WMAOP 2026-08-08 (Steve directive): second net behind the V142 pre-pass
        // gate — the directive holds on EVERY adapter route to the pull engine:
        //   FODDER_HOLD   — Blockade Flagship site already on table: never play
        //                   WMAOP again; hold it in hand as force-loss fodder.
        //   DEPLOY_ONLY   — WMAOP fires only during our own DEPLOY phase.
        //   BLOCKADE_ONLY — only the Blockade Flagship site pull is sanctioned;
        //                   the Effect and Podracer modes waste the card.
        // WMAOP 2026-08-08 FIX-FORWARD (post-ship review N3): also require the
        // action text to be one of the card's three Reserve-Deck searches — the
        // 2026-05-28 V142 incident showed WMAOP's cardId leaking onto unrelated
        // actions ("Activate Force"); a bare source-title key repeats that class.
        boolean wmaopSource = facts.sourceTitle() != null
                && facts.sourceTitle().toLowerCase(Locale.ROOT)
                    .contains("accelerate our plans")
                && facts.actionText() != null
                && facts.actionText().toLowerCase(Locale.ROOT)
                    .contains("from reserve deck");
        if (!hardBlocked && wmaopSource) {
            boolean wmaopLocationMode = facts.actionText()
                    .toLowerCase(Locale.ROOT).contains("blockade flagship");
            if (facts.wmaopBlockadeSiteOnTable()) {
                operations.add(add(facts.actionId(), "WMAOP.FODDER_HOLD",
                        TraceOutputKind.VETO, -2000.0f,
                        "WMAOP.FODDER_HOLD: Blockade Flagship site already on table — never play We Must Accelerate Our Plans again; hold it in hand as preferred force-loss fodder"));
                hardBlocked = true;
            } else if (facts.phase() != Phase.DEPLOY) {
                operations.add(add(facts.actionId(), "WMAOP.DEPLOY_ONLY",
                        TraceOutputKind.VETO, -2000.0f,
                        "WMAOP.DEPLOY_ONLY: We Must Accelerate Our Plans fires only during our DEPLOY phase — hold the interrupt"));
                hardBlocked = true;
            } else if (!wmaopLocationMode) {
                operations.add(add(facts.actionId(), "WMAOP.BLOCKADE_ONLY",
                        TraceOutputKind.VETO, -2000.0f,
                        "WMAOP.BLOCKADE_ONLY: only the Blockade Flagship site pull is sanctioned — the Effect/Podracer modes waste the card"));
                hardBlocked = true;
            }
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

        if (!hardBlocked
                && !facts.requiredOnTableCardPullVetoBypass()
                && !facts.objectiveRoutePullVetoBypass()
                && facts.cheapestTargetCost() != null
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

        if (!hardBlocked
                && !facts.requiredOnTableCardPullVetoBypass()
                && facts.weaponPull() && !facts.locationPull()) {
            WeaponOrderEvaluation weaponOrder = evaluateWeaponOrder(
                    new WeaponOrderFacts(facts.actionId(), facts.weaponPull(),
                            facts.locationPull(), facts.unarmedCharacters(),
                            facts.armedCharacters(), facts.lightsaberPull(),
                            facts.capableLightsaberWielders()));
            operations.addAll(weaponOrder.result().operations());
            if (weaponOrder.outcome() == WeaponOrderOutcome.READY) {
                typeTier = 600.0f;
                typeDescription = String.format("WEAPON (V67am value, %d unarmed target(s) \u2014 %s)",
                        facts.unarmedCharacters(), facts.weaponReason());
            } else if (weaponOrder.outcome() != WeaponOrderOutcome.NONE) {
                hardBlocked = true;
            }
        }

        if (!hardBlocked
                && !facts.requiredOnTableCardPullVetoBypass()
                && facts.devicePull()
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
        if (!hardBlocked && !downgraded
                && facts.requiredOnTableCardPull()) {
            operations.add(add(facts.actionId(),
                    "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                    TraceOutputKind.BANDED, 1000.0f,
                    "Use the objective's source-verified upload for a missing required on-table card"));
        }
        if (!hardBlocked && !downgraded) {
            float endorShieldBootstrap =
                    EndorOperationsTacticalPolicy
                        .endorShieldBootstrapAdjustment(
                            facts.sourceTitle(), facts.actionText());
            if (endorShieldBootstrap != 0.0f) {
                operations.add(add(facts.actionId(),
                        "EOP-ENDOR-SHIELD-BOOTSTRAP",
                        TraceOutputKind.BANDED, endorShieldBootstrap,
                        "EOP bootstrap: deploy Endor Shield from the Bunker before assigning its cheap garrison"));
            }
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
