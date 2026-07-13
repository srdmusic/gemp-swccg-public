package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FINALIZER LANE F3 / PURE SHADOW FINALIZER (2026-07-13) ═══
// Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F3 —
// minimum pure seam:
//   finalize(DecisionSnapshot, ResponseContract, ResponseIntent,
//            RandomGenerator, RejectionHistory) -> FinalizedResponse
// Audit: Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md §"Smallest
// consolidation seam". Frozen order: Handoffs/CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md
// step 2. Shadow authority: Handoffs/CODEX_RANDO_RUNTIME_ROUTE_MAP_2026-07-13.md —
// legacy is the ONLY authority; the shadow makes no mutation, no unseeded RNG
// draw, and never calls decisionMade.
//
// PACKET RULES implemented here, verbatim:
//  - No game, tracker, strategy, planner, cache, chat, or mediator mutation:
//    static stateless methods over immutable inputs; the only outputs are value
//    records. The TrackerMutationRequest is a descriptor the caller applies
//    AFTER engine acceptance.
//  - Fixed RNG is injected. Neither mirrored DecisionSafety static Random may be
//    called: this class does not reference either DecisionSafety, and consumes
//    AT MOST ONE draw from the injected generator, only on the FORCED path
//    where a set of equally-legal fallback candidates exists. Draw metadata is
//    recorded (FinalizedResponse.RandomDraw) so a fixed seed reproduces it.
//  - ResponseContract preserves the CARD_ACTION_CHOICE empty contradiction as
//    engine truth (empty accepted even under raw noPass=true) — so a legal
//    Pass/Acknowledge is ACCEPTED here, never overwritten the way the outer
//    bots' raw-noPass emergency does (RandoCalAi.java:996-1010, audit P0 #2).
//  - ARBITRARY output contains only selectable delta ids. It never resends
//    locked preselected ids (engine truth: ArbitraryCardsSelectionDecision
//    .java:255 rejects non-selectable ids; the LEGACY clamp resends them —
//    DecisionSafety.java:222-227 — and produces engine-rejected wire).
//  - Rejection remains typed data. F3 does not perform retries itself: the
//    RejectionHistory input is stamped into the result (retry position) and the
//    bounded-retry OWNER (F2, engine change held for Steve) decides what's next.
//
// NO PRODUCTION CONSUMER. Nothing routes through this class at runtime; the
// fixture corpus (EngineDecisionFixtures / ResponseFinalizerContractTest) is
// the only caller. Both legacy DecisionSafety copies remain authoritative.
// ═══════════════════════════════════════════════════════════
public final class ResponseFinalizer {

    private ResponseFinalizer() {
        // static access only
    }

    /**
     * Finalize one typed intent against one frozen snapshot + derived contract.
     * Pure: same inputs and same RNG seed produce an equal FinalizedResponse.
     */
    public static FinalizedResponse finalize(DecisionSnapshot snapshot,
                                             ResponseContract contract,
                                             ResponseIntent intent,
                                             RandomGenerator random,
                                             RejectionHistory history) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(history, "history");
        String decisionId = snapshot.decisionFacts().decisionId();
        int prior = history.size();

        if (intent instanceof ResponseIntent.Pass) {
            return finalizePass(contract, intent, random, decisionId, prior);
        }
        if (intent instanceof ResponseIntent.Acknowledge) {
            if (contract.emptyWireAccepted()) {
                return accepted(intent, "", decisionId, prior);
            }
            return rejected(intent, FinalizedResponse.RejectReason.EMPTY_WIRE_REJECTED,
                    contract.decisionType() + " rejects an empty wire response — Acknowledge does not apply",
                    prior);
        }
        if (intent instanceof ResponseIntent.CandidateOrdinal ordinal) {
            return finalizeCandidateOrdinal(contract, ordinal, decisionId, prior);
        }
        if (intent instanceof ResponseIntent.CardOrdinals cards) {
            return finalizeCardOrdinals(contract, cards, decisionId, prior);
        }
        if (intent instanceof ResponseIntent.IntegerValue integer) {
            return finalizeInteger(contract, integer, decisionId, prior);
        }
        // Sealed interface: unreachable. Typed rejection rather than silence.
        return rejected(intent, FinalizedResponse.RejectReason.INTENT_TYPE_MISMATCH,
                "unhandled intent variant " + intent.getClass().getSimpleName(), prior);
    }

    // ── Pass: legal empty, or a typed/deterministic/seeded fallback — never the
    // legacy unseeded random overwrite (audit P1 "Randomness has multiple owners"). ──
    private static FinalizedResponse finalizePass(ResponseContract contract, ResponseIntent intent,
                                                  RandomGenerator random, String decisionId, int prior) {
        if (contract.emptyWireAccepted()) {
            // Engine truth wins: a legal pass is ACCEPTED as-is. This is the exact
            // case the outer raw-noPass emergency wrongly overwrites today
            // (RandoCalAi.java:996-1010; audit P0 #2 "Can overwrite a legal empty
            // response for some engine decision types"). contract.policyPassAllowed
            // (the ONE V148 semantic) is advisory data for the strategy lane.
            return accepted(intent, "", decisionId, prior);
        }
        switch (contract.decisionType()) {
            case ACTION_CHOICE:
            case MULTIPLE_CHOICE: {
                // A set of equally-legal candidates: ONE seeded draw, recorded.
                List<String> candidates = contract.candidateWireIds();
                if (candidates.isEmpty()) {
                    return rejected(intent, FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK,
                            contract.decisionType() + " rejects empty and offers no candidates"
                                    + " — where legacy guesses '0' blind (DecisionSafety.java:166-174)",
                            prior);
                }
                int draw = random.nextInt(candidates.size());
                return new FinalizedResponse(intent, FinalizedResponse.Status.FORCED,
                        candidates.get(draw), List.of(), null,
                        new FinalizedResponse.RandomDraw(candidates.size(), draw),
                        new FinalizedResponse.TrackerMutationRequest(decisionId, candidates.get(draw)),
                        prior);
            }
            case INTEGER: {
                // Deterministic canonical fallback: engine default, else the nearest
                // engine bound. Fixes the always-0 legacy emergency the audit flags
                // as illegal (DecisionSafety.java:294-298, P1 "INTEGER emergency can
                // be illegal"). No draw.
                Integer value = contract.integerDefault();
                if (value == null || violatesIntegerBounds(contract, value)) {
                    value = contract.minimum() != null ? contract.minimum() : contract.maximum();
                }
                if (value == null || violatesIntegerBounds(contract, value)) {
                    return rejected(intent, FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK,
                            "no engine default and no usable bound for a forced INTEGER answer", prior);
                }
                String wire = String.valueOf(value);
                return new FinalizedResponse(intent, FinalizedResponse.Status.FORCED, wire,
                        List.of(), null, null,
                        new FinalizedResponse.TrackerMutationRequest(decisionId, wire), prior);
            }
            case CARD_SELECTION:
            case ARBITRARY_CARDS: {
                // Empty is wire-rejected here only when min>0 (and not bypassed):
                // deterministic first-N sendable fill, no draw.
                int min = contract.minimum() != null ? contract.minimum() : 0;
                List<String> fill = firstSendable(contract, min, new LinkedHashSet<>());
                if (fill.size() < min) {
                    return rejected(intent, FinalizedResponse.RejectReason.BELOW_MINIMUM_UNCORRECTABLE,
                            "only " + fill.size() + " sendable ids exist but the engine minimum is " + min,
                            prior);
                }
                String wire = String.join(",", fill);
                return new FinalizedResponse(intent, FinalizedResponse.Status.FORCED, wire,
                        List.of(), null, null,
                        new FinalizedResponse.TrackerMutationRequest(decisionId, wire), prior);
            }
            default:
                return rejected(intent, FinalizedResponse.RejectReason.EMPTY_WIRE_REJECTED,
                        contract.decisionType() + " rejects an empty wire response and no fallback"
                                + " family exists", prior);
        }
    }

    // ── CandidateOrdinal: the bounds guard the engine lacks for MULTIPLE_CHOICE
    // (MultipleChoiceAwaitingDecision.java:59-70 throws UNCHECKED on an
    // out-of-range ordinal — audit P0 #1; F1 owns the engine repair). ──
    private static FinalizedResponse finalizeCandidateOrdinal(ResponseContract contract,
                                                              ResponseIntent.CandidateOrdinal intent,
                                                              String decisionId, int prior) {
        switch (contract.decisionType()) {
            case ACTION_CHOICE:
            case CARD_ACTION_CHOICE:
            case MULTIPLE_CHOICE:
                break;
            default:
                return rejected(intent, FinalizedResponse.RejectReason.INTENT_TYPE_MISMATCH,
                        "CandidateOrdinal does not apply to " + contract.decisionType(), prior);
        }
        if (!contract.inCandidateBounds(intent.ordinal())) {
            return rejected(intent, FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                    "ordinal " + intent.ordinal() + " outside candidates [0.."
                            + (contract.candidateWireIds().size() - 1) + "]"
                            + (contract.decisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                                    ? " — engine would throw unchecked ArrayIndexOutOfBoundsException"
                                            + " (MultipleChoiceAwaitingDecision.java:70, audit P0 #1)"
                                    : ""),
                    prior);
        }
        return accepted(intent, contract.candidateWireIds().get(intent.ordinal()), decisionId, prior);
    }

    // ── CardOrdinals: selectable-delta clamp + min/max enforcement. ──
    private static FinalizedResponse finalizeCardOrdinals(ResponseContract contract,
                                                          ResponseIntent.CardOrdinals intent,
                                                          String decisionId, int prior) {
        switch (contract.decisionType()) {
            case CARD_SELECTION:
            case ARBITRARY_CARDS:
                break;
            default:
                return rejected(intent, FinalizedResponse.RejectReason.INTENT_TYPE_MISMATCH,
                        "CardOrdinals does not apply to " + contract.decisionType(), prior);
        }
        List<FinalizedResponse.Correction> corrections = new ArrayList<>();
        LinkedHashSet<String> kept = new LinkedHashSet<>();
        for (Integer ordinal : intent.ordinals()) {
            if (!contract.inCandidateBounds(ordinal)) {
                // A typed intent with an out-of-bounds ordinal is a caller fault,
                // not correctable data — typed rejection (consistent with
                // CandidateOrdinal), never a silent drop.
                return rejected(intent, FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                        "card ordinal " + ordinal + " outside candidates [0.."
                                + (contract.candidateWireIds().size() - 1) + "]", prior);
            }
            String wireId = contract.candidateWireIds().get(ordinal);
            if (!contract.isSendable(ordinal)) {
                // Locked preselected state or plain non-selectable id: drop it.
                // PACKET F3 RULE: ARBITRARY output contains only selectable delta
                // ids; it never resends locked preselected ids.
                corrections.add(new FinalizedResponse.Correction(
                        contract.isPreselected(ordinal)
                                ? FinalizedResponse.CorrectionReason.PRESELECTED_DELTA_ONLY
                                : FinalizedResponse.CorrectionReason.SELECTABLE_CLAMP,
                        "dropped ordinal " + ordinal + " (" + wireId + "): "
                                + (contract.isPreselected(ordinal)
                                        ? "locked preselected id — engine rejects resends"
                                                + " (ArbitraryCardsSelectionDecision.java:255)"
                                        : "not selectable")));
                continue;
            }
            if (!kept.add(wireId)) {
                corrections.add(new FinalizedResponse.Correction(
                        FinalizedResponse.CorrectionReason.DUPLICATE_DROPPED,
                        "duplicate ordinal " + ordinal + " (" + wireId + ") — engine rejects duplicates"
                                + " (CardsSelectionDecision.java:76-77)"));
            }
        }

        int min = contract.minimum() != null ? contract.minimum() : 0;
        Integer max = contract.maximum();
        if (!contract.returnAnyChange()) {
            // Max truncation: drop trailing ids (engine rejects count > max,
            // CardsSelectionDecision.java:70-71).
            if (max != null && kept.size() > max) {
                List<String> ordered = new ArrayList<>(kept);
                List<String> dropped = ordered.subList(max, ordered.size());
                corrections.add(new FinalizedResponse.Correction(
                        FinalizedResponse.CorrectionReason.MAX_CLAMP,
                        "dropped trailing " + dropped + " to satisfy engine maximum " + max));
                kept = new LinkedHashSet<>(ordered.subList(0, max));
            }
            // Min fill: deterministic first-sendable ids not already kept.
            if (kept.size() < min) {
                List<String> fill = firstSendable(contract, min - kept.size(), kept);
                if (kept.size() + fill.size() < min) {
                    return rejected(intent, FinalizedResponse.RejectReason.BELOW_MINIMUM_UNCORRECTABLE,
                            "only " + (kept.size() + fill.size()) + " sendable ids exist but the engine"
                                    + " minimum is " + min + " — where legacy would emit"
                                    + " SAFETY_CRITICAL_NO_OPTIONS (DecisionSafety.java:166-174)",
                            prior);
                }
                corrections.add(new FinalizedResponse.Correction(
                        FinalizedResponse.CorrectionReason.MANDATORY_REBUILD,
                        "added first-sendable " + fill + " to satisfy engine minimum " + min));
                kept.addAll(fill);
            }
        }

        String wire = String.join(",", kept);
        if (wire.isEmpty() && !contract.emptyWireAccepted()) {
            // Everything was dropped and empty is not wire-legal; with min==0 this
            // cannot happen (emptyWireAccepted would be true), so min>0 paths were
            // already handled above — this guards the ACTION-like impossibility.
            return rejected(intent, FinalizedResponse.RejectReason.EMPTY_WIRE_REJECTED,
                    "all ordinals dropped and the engine rejects an empty response", prior);
        }
        if (corrections.isEmpty()) {
            return accepted(intent, wire, decisionId, prior);
        }
        return new FinalizedResponse(intent, FinalizedResponse.Status.CORRECTED, wire,
                corrections, null, null,
                new FinalizedResponse.TrackerMutationRequest(decisionId, wire), prior);
    }

    // ── IntegerValue: engine bounds (IntegerAwaitingDecision.java:35-47);
    // absent bound = unbounded, exactly as the engine treats a missing param. ──
    private static FinalizedResponse finalizeInteger(ResponseContract contract,
                                                     ResponseIntent.IntegerValue intent,
                                                     String decisionId, int prior) {
        if (contract.decisionType() != AwaitingDecisionType.INTEGER) {
            return rejected(intent, FinalizedResponse.RejectReason.INTENT_TYPE_MISMATCH,
                    "IntegerValue does not apply to " + contract.decisionType(), prior);
        }
        if (violatesIntegerBounds(contract, intent.value())) {
            return rejected(intent, FinalizedResponse.RejectReason.INTEGER_OUT_OF_BOUNDS,
                    "value " + intent.value() + " outside engine bounds ["
                            + contract.minimum() + ".." + contract.maximum() + "]", prior);
        }
        return accepted(intent, String.valueOf(intent.value()), decisionId, prior);
    }

    // ── helpers ──

    private static boolean violatesIntegerBounds(ResponseContract contract, int value) {
        return (contract.minimum() != null && value < contract.minimum())
                || (contract.maximum() != null && value > contract.maximum());
    }

    /** First {@code count} sendable candidate wire ids not already in {@code exclude},
     *  in ORIGINAL ordinal order — deterministic, no RNG. */
    private static List<String> firstSendable(ResponseContract contract, int count,
                                              LinkedHashSet<String> exclude) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < contract.candidateWireIds().size() && result.size() < count; i++) {
            String wireId = contract.candidateWireIds().get(i);
            if (contract.isSendable(i) && !exclude.contains(wireId) && !result.contains(wireId)) {
                result.add(wireId);
            }
        }
        return result;
    }

    private static FinalizedResponse accepted(ResponseIntent intent, String wire,
                                              String decisionId, int prior) {
        return new FinalizedResponse(intent, FinalizedResponse.Status.ACCEPTED, wire,
                List.of(), null, null,
                new FinalizedResponse.TrackerMutationRequest(decisionId, wire), prior);
    }

    private static FinalizedResponse rejected(ResponseIntent intent,
                                              FinalizedResponse.RejectReason reason,
                                              String detail, int prior) {
        return new FinalizedResponse(intent, FinalizedResponse.Status.REJECTED, null,
                List.of(), new FinalizedResponse.Rejection(reason, detail), null, null, prior);
    }
}
