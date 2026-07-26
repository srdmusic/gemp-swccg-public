package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure card-specific PULL scoring shared by Rando and Chosen One. */
public final class PullSpecificActionPolicy {

    private static final String PRODUCER = "PULL_SPECIFIC_ACTION_POLICY";

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

    private PullSpecificActionPolicy() {
    }

    public static PolicyResult scoreWmaopGate(PullSpecificActionFacts.Gate facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.blocked()
                ? one(facts.actionId(), "V142", TraceOutputKind.VETO, -2000.0f,
                "V142 WMAOP BLOCK: " + facts.reason() + " — hold the interrupt")
                : empty();
    }

    public static PolicyResult scoreLostPileLightsaberGate(
            PullSpecificActionFacts.Gate facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.blocked()
                ? one(facts.actionId(), "V147", TraceOutputKind.VETO, -2000.0f,
                "V147 IAYF: Vader's Lightsaber NOT in Lost Pile — don't waste 1 Force on failed search, use free Reserve download")
                : empty();
    }

    public static PolicyResult scoreWelcomeHome(
            PullSpecificActionFacts.WelcomeHome facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.saveForBattle()
                ? one(facts.actionId(), "V155-welcome-home-save",
                TraceOutputKind.VETO, -2000.0f,
                "V155 WELCOME HOME: " + facts.why()
                        + " — SAVE this card for battle (Tyranus ability-number mode), don't waste the pull")
                : empty();
    }

    public static PolicyResult scoreYouAreBeatenSearch(
            PullSpecificActionFacts.YouAreBeatenSearch facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.searchMode()
                ? one(facts.actionId(), "V144-you-are-beaten-search",
                TraceOutputKind.VETO, -2000.0f,
                "V144 YOU ARE BEATEN: Mode 2 (IAYF search) — never use this mode, save for battle freeze or Cancel Uncontrollable Fury")
                : empty();
    }

    public static Evaluation scorePileSearch(
            PullSpecificActionFacts.PileSearch facts) {
        Objects.requireNonNull(facts, "facts");
        String pile = facts.pileKind() == PullSpecificActionFacts.PileKind.LOST
                ? "Lost" : "Used";
        if (facts.pileSize() == 0) {
            return evaluation(one(facts.actionId(), "V23-empty-"
                            + pile.toLowerCase(), TraceOutputKind.VETO, -300.0f,
                    "V23 EMPTY PILE: " + pile + " Pile is empty — search will fail!"),
                    AdapterStep.CONTINUE_ACTION);
        }
        if (facts.pileKind() == PullSpecificActionFacts.PileKind.LOST
                && facts.pileSize() <= 2) {
            return evaluation(one(facts.actionId(), "V23-low-lost",
                            TraceOutputKind.VETO, -100.0f,
                    "V23 LOW PILE: Lost Pile only has " + facts.pileSize()
                            + " cards — risky search"), AdapterStep.FALL_THROUGH);
        }
        return evaluation(empty(), AdapterStep.FALL_THROUGH);
    }

    public static Evaluation scoreTdigwatt(
            PullSpecificActionFacts.ExhaustedSearch facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.targetAvailable()) {
            return evaluation(empty(), AdapterStep.FALL_THROUGH);
        }
        return evaluation(one(facts.actionId(), "V24-tdigwatt-exhausted",
                        TraceOutputKind.VETO, -400.0f,
                        "V24 TDIGWATT: All targets already pulled — search will fail!"),
                AdapterStep.CONTINUE_ACTION);
    }

    public static PolicyResult scoreSorryLocation(
            PullSpecificActionFacts.SorryLocation facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.targetAvailable()
                ? one(facts.actionId(), "V24.6-sorry-ready",
                TraceOutputKind.ORDERING, 250.0f,
                "V24.6 I'M SORRY: CC sites still in reserve — pull one NOW for more drains + occupation!")
                : one(facts.actionId(), "V24.6-sorry-exhausted",
                TraceOutputKind.VETO, -300.0f,
                "V24.6 I'M SORRY: All CC interior sites already pulled — search will fail!");
    }

    public static PolicyResult scoreIayfPresence(
            PullSpecificActionFacts.IayfPresence facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.applies() && !facts.vaderOnTable()
                ? one(facts.actionId(), "V29.8-iayf-presence",
                TraceOutputKind.VETO, -500.0f,
                "V29.8 IAYF: Vader NOT on table — can't deploy lightsaber from ANY source!")
                : empty();
    }

    public static PolicyResult scoreIayfReserve(
            PullSpecificActionFacts.IayfReserve facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.vaderOnTable()) {
            return one(facts.actionId(), "V29.7-iayf-presence",
                    TraceOutputKind.VETO, -500.0f,
                    "V29.7 IAYF: Vader NOT on table — can't deploy lightsaber!");
        }
        if (facts.reserveMode() && !facts.saberInReserve()) {
            return one(facts.actionId(), "V37-iayf-wrong-reserve",
                    TraceOutputKind.VETO, -600.0f,
                    "V37 IAYF: Lightsaber NOT in Reserve Deck — WILL FAIL! Check Lost Pile instead.");
        }
        if (facts.lostMode() && !facts.saberInLost()) {
            return one(facts.actionId(), "V37-iayf-wrong-lost",
                    TraceOutputKind.VETO, -400.0f,
                    "V37 IAYF: Lightsaber NOT in Lost Pile — check Reserve instead.");
        }
        if (!facts.vaderArmed()) {
            return one(facts.actionId(), "V37-iayf-unarmed",
                    TraceOutputKind.ORDERING, 600.0f,
                    "V37 IAYF: Vader UNARMED — retrieve lightsaber from "
                            + (facts.lostMode() ? "Lost Pile" : "Reserve") + " NOW!");
        }
        return one(facts.actionId(), "V35.8-iayf-spare",
                TraceOutputKind.ORDERING, 50.0f,
                "V35.8 IAYF: Vader armed — spare lightsaber retrieval");
    }

    public static PolicyResult scoreNamedReserveSource(
            PullSpecificActionFacts.NamedReserveSource facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        switch (facts.sourceKind()) {
            case CRUSH_THE_REBELLION -> {
                if (!facts.targetAvailable()) {
                    operations.add(add(facts.actionId(), "V29.7-crush-empty",
                            TraceOutputKind.VETO, -400.0f,
                            "V29.7 CRUSH: No I Have You Now or Evader in reserve — WILL FAIL!"));
                }
                switch (facts.duplicateState()) {
                    case BOTH_IN_HAND -> operations.add(add(facts.actionId(),
                            "V29.9-crush-both", TraceOutputKind.VETO, -300.0f,
                            "V29.9 CRUSH DUPLICATE: Both IHYN and Evader already in hand — pulling another is wasteful!"));
                    case FIRST_IN_HAND_SECOND_MISSING -> operations.add(add(
                            facts.actionId(), "V29.9-crush-ihyn", TraceOutputKind.VETO,
                            -250.0f, "V29.9 CRUSH DUPLICATE: IHYN already in hand, no Evader in reserve — save Crush!"));
                    case SECOND_IN_HAND_FIRST_MISSING -> operations.add(add(
                            facts.actionId(), "V29.9-crush-evader", TraceOutputKind.VETO,
                            -250.0f, "V29.9 CRUSH DUPLICATE: Evader already in hand, no IHYN in reserve — save Crush!"));
                    default -> { }
                }
            }
            case YOU_ARE_BEATEN -> addMissing(operations, facts,
                    "V29.7-you-are-beaten-empty",
                    "V29.7 YOU ARE BEATEN: No I Am Your Father in reserve — WILL FAIL!");
            case BLAST_POINTS -> addMissing(operations, facts,
                    "V29.7-blast-points-empty",
                    "V29.7 BLAST POINTS: No Ghhhk or Hyperwave Scan in reserve — WILL FAIL!");
            case HUNT_DOWN -> addMissing(operations, facts,
                    "V29.7-hunt-down-empty",
                    "V29.7 HUNT DOWN: No locations left in reserve — WILL FAIL!");
            case IMPERIAL_COMMAND -> addMissing(operations, facts,
                    "V29.7-imperial-command-empty",
                    "V29.7 IMPERIAL COMMAND: No admirals/generals in reserve — WILL FAIL!");
            case ENDOR_SHIELD -> addMissing(operations, facts,
                    "V29.7-endor-shield-empty",
                    "V29.7 ENDOR SHIELD: No admirals in reserve — WILL FAIL!");
            case VISAGE -> addMissing(operations, facts,
                    "V29.7-visage-empty",
                    "V29.7 VISAGE: No lightsabers in reserve — WILL FAIL!");
            case KIR_KANOS -> addMissing(operations, facts,
                    "V29.7-kir-kanos-empty",
                    "V29.7 KIR KANOS: No Royal Guards in reserve — WILL FAIL!");
            default -> { }
        }
        return result(operations);
    }

    public static PolicyResult scoreHuntDownLocationDownload(
            PullSpecificActionFacts.HuntDownLocationDownload facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.targetAvailable()
                ? one(facts.actionId(),
                    "PULL.OBJECTIVE.HUNT_DOWN_LOCATION_DOWNLOAD",
                    TraceOutputKind.ORDERING, 300.0f,
                    "Use the objective action to deploy an eligible battleground site")
                : empty();
    }

    public static PolicyResult scoreVeersHothUpload(
            PullSpecificActionFacts.VeersHothUpload facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.exactSourceAndAction() && !facts.targetAvailable()
                ? one(facts.actionId(), "V29.7-veers-hoth-upload-empty",
                    TraceOutputKind.VETO, -2000.0f,
                    "V29.7 VEERS: No Blizzard 1 or 6th Marker in Reserve Deck, so the reveal would fail")
                : empty();
    }

    public static PolicyResult scoreReserveRisk(
            PullSpecificActionFacts.ReserveRisk facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.reserveSize() <= 3) {
            return one(facts.actionId(), "V37-reserve-intel",
                    TraceOutputKind.VETO, -200.0f,
                    "V37 RESERVE INTEL RISK: Only " + facts.reserveSize()
                            + " cards in reserve — search reveals almost everything to opponent!");
        }
        if (facts.reserveSize() <= 8) {
            return one(facts.actionId(), "V37-reserve-caution",
                    TraceOutputKind.VETO, -50.0f,
                    "V37 RESERVE CAUTION: " + facts.reserveSize()
                            + " cards in reserve — opponent will see deck composition");
        }
        return empty();
    }

    public static PolicyResult scoreMasterfulMove(
            PullSpecificActionFacts.MasterfulMove facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.characterOnTable()) {
            return one(facts.actionId(), "V24.9-masterful-no-character",
                    TraceOutputKind.VETO, -500.0f,
                    "V24.9 MASTERFUL MOVE: No characters on table — Ghhhk has nothing to protect! Save force for deployment!");
        }
        if (facts.turnNumber() <= 2) {
            return one(facts.actionId(), "V24.9-masterful-early",
                    TraceOutputKind.VETO, -300.0f,
                    "V24.9 MASTERFUL MOVE: Too early (turn " + facts.turnNumber()
                            + ") — prioritize getting Executor out!");
        }
        return empty();
    }

    public static PolicyResult scoreEffectSearch(
            PullSpecificActionFacts.EffectSearch facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.wokling()
                ? one(facts.actionId(), "V53-wokling-search",
                    TraceOutputKind.VETO, -9999.0f,
                    "V53 BLOCK WOKLING: Don't waste 3 force searching for effects!")
                : one(facts.actionId(), "PULL-effect-search",
                    TraceOutputKind.ORDERING, 30.0f,
                    "Search for Effect from Reserve Deck");
    }

    public static PolicyResult scoreAdmiralGeneralPull(
            PullSpecificActionFacts.AdmiralGeneralPull facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.targetAvailable()) {
            return one(facts.actionId(), "V29.7-admiral-general-empty",
                    TraceOutputKind.VETO, -300.0f,
                    "V29.7 NO TARGET: No admirals/generals in Reserve Deck — skip!");
        }
        if (facts.bespinChainActive()) {
            return one(facts.actionId(), "PULL-executor-chain",
                    TraceOutputKind.ORDERING, 300.0f,
                    "CRITICAL: Admiral pilot enables Executor deploy to Bespin — must pull T1!");
        }
        return one(facts.actionId(), "V29.7-admiral-general-first",
                TraceOutputKind.ORDERING, 250.0f,
                "V29.7 PULL FIRST: Retrieve admiral/general into hand before deploying!");
    }

    private static void addMissing(List<PolicyOperation> operations,
                                   PullSpecificActionFacts.NamedReserveSource facts,
                                   String rule, String reason) {
        if (!facts.targetAvailable()) {
            operations.add(add(facts.actionId(), rule, TraceOutputKind.VETO,
                    -400.0f, reason));
        }
    }

    private static Evaluation evaluation(PolicyResult result, AdapterStep step) {
        return new Evaluation(result, step);
    }

    private static PolicyResult empty() {
        return result(List.of());
    }

    private static PolicyResult one(String actionId, String rule,
                                    TraceOutputKind outputKind,
                                    float delta, String reason) {
        return result(List.of(add(actionId, rule, outputKind, delta, reason)));
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult(PRODUCER, operations);
    }

    private static PolicyOperation add(String actionId, String rule,
                                       TraceOutputKind outputKind,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.PULL_SEARCH, outputKind, delta, reason);
    }
}
