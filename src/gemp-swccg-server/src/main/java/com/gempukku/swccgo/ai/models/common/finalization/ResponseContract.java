package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.models.common.decision.DecisionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FINALIZER LANE F3 / RESPONSE CONTRACT (2026-07-13) ═══
// Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F3 —
// "ResponseContract is derived from raw engine parameters and concrete decision
// shape. It preserves the CARD_ACTION_CHOICE empty-response contradiction as
// observed engine truth."
// Audit: Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md P0 #2 — the final
// contract must separate:
//   policyPassAllowed  — whether strategy may intentionally decline (ONE owner:
//                        the V148 semantic, below).
//   emptyWireAccepted  — whether the concrete engine decision accepts "".
//   minimum/maximum    — selection cardinality enforced by the engine.
//   preselected        — existing selected state for iterative decisions.
//   returnAnyChange    — one valid change may return before normal cardinality.
//   defaultIndex/bounds— concrete multiple-choice and integer defaults.
//
// Engine truth per concrete type (audit engine-truth table, verified in source):
//   ACTION_CHOICE       empty REJECTED     ActionSelectionDecision.java:130-132
//   CARD_ACTION_CHOICE  empty ACCEPTED     CardActionSelectionDecision.java:167-169
//                       (as "no selected action" EVEN when raw noPass=true — the
//                       preserved contradiction; do not normalize it away)
//   CARD_SELECTION      empty iff min==0   CardsSelectionDecision.java:63-67
//   ARBITRARY_CARDS     cardinality bypass ArbitraryCardsSelectionDecision.java:248
//                       when returnAnyChange=true; else min==0
//   MULTIPLE_CHOICE     empty REJECTED     MultipleChoiceAwaitingDecision.java:64-69
//   INTEGER             empty REJECTED     IntegerAwaitingDecision.java:35-47
//   EMPTY               anything ACCEPTED  EmptyAwaitingDecision.java:26-27
//
// Pure derived value type: built once from the frozen DecisionSnapshot, no
// engine objects, no mutation, no policy scoring.
// ═══════════════════════════════════════════════════════════
public record ResponseContract(
        AwaitingDecisionType decisionType,
        /** Whether the concrete engine decision accepts the empty wire response. */
        boolean emptyWireAccepted,
        /** THE one pass-legality semantic (V148, Steve 2026-05-28; audit P0 #2
         *  target "one owner"): strategy may decline iff min==0 (absent min counts
         *  as 0, matching DecisionSafety.mustChoose's parse default) AND either the
         *  engine did not send noPass=true OR the prompt offers
         *  Done/Cancel/"if desired"/optional (HTML-stripped — the hardened
         *  DecisionSafety.java:77-84 variant, not CombinedEvaluator's unstripped
         *  inline copy). ABSENT noPass counts as no prohibition (engine truth) —
         *  deliberately NOT DecisionContext's fabricated noPass=true default the
         *  audit flags. Advisory for strategy and comparison; wire legality is
         *  emptyWireAccepted. */
        boolean policyPassAllowed,
        /** Engine cardinality minimum; null = the engine sent no min param. */
        Integer minimum,
        /** Engine cardinality maximum; null = the engine sent no max param. */
        Integer maximum,
        /** ARBITRARY_CARDS iterative flag: one valid change may return before
         *  normal cardinality (ArbitraryCardsSelectionDecision.java:248,281). */
        boolean returnAnyChange,
        /** The wire id per ORIGINAL candidate ordinal: actionId array entries for
         *  action decisions, cardId array entries for card decisions, and the
         *  ordinal strings "0".."n-1" for MULTIPLE_CHOICE (the wire answers by
         *  index over the results array). Empty for INTEGER/EMPTY. */
        List<String> candidateWireIds,
        /** Per-ordinal selectable flags; null = the engine sent no selectable
         *  array (CARD_SELECTION: every offered id is selectable, engine validates
         *  membership only — CardsSelectionDecision.java:93-99). */
        List<Boolean> selectable,
        /** Per-ordinal preselected flags; null = the engine sent none. Locked
         *  preselected ids are shown state, NOT sendable delta: the engine rejects
         *  a response containing a non-selectable preselected id
         *  (ArbitraryCardsSelectionDecision.java:255). */
        List<Boolean> preselected,
        /** Raw INTEGER defaultValue when the engine sent one (packet F0:
         *  "Raw defaultValue remains available"). */
        Integer integerDefault,
        /** Raw MULTIPLE_CHOICE defaultIndex; -1 or null = no default. */
        Integer defaultIndex) {

    public ResponseContract {
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(candidateWireIds, "candidateWireIds");
        candidateWireIds = List.copyOf(candidateWireIds);
        selectable = selectable != null ? List.copyOf(selectable) : null;
        preselected = preselected != null ? List.copyOf(preselected) : null;
        if (minimum != null && minimum < 0) {
            throw new IllegalArgumentException("minimum must be >= 0 when present, was " + minimum);
        }
        if (selectable != null && selectable.size() != candidateWireIds.size()) {
            throw new IllegalArgumentException("selectable length " + selectable.size()
                    + " != candidate count " + candidateWireIds.size());
        }
        if (preselected != null && preselected.size() != candidateWireIds.size()) {
            throw new IllegalArgumentException("preselected length " + preselected.size()
                    + " != candidate count " + candidateWireIds.size());
        }
    }

    /** Ordinal is a legal index into the candidate wire ids. */
    public boolean inCandidateBounds(int ordinal) {
        return ordinal >= 0 && ordinal < candidateWireIds.size();
    }

    /** Whether the candidate at this ordinal may appear in a wire response:
     *  selectable[ordinal] where the engine sent the array, else membership alone.
     *  Preselected does NOT imply sendable (see {@link #preselected}). */
    public boolean isSendable(int ordinal) {
        if (!inCandidateBounds(ordinal)) {
            return false;
        }
        return selectable == null || Boolean.TRUE.equals(selectable.get(ordinal));
    }

    public boolean isPreselected(int ordinal) {
        return preselected != null && inCandidateBounds(ordinal)
                && Boolean.TRUE.equals(preselected.get(ordinal));
    }

    /**
     * Derive the contract from one frozen snapshot: concrete decision shape plus the
     * verbatim raw engine parameters ({@link DecisionSnapshot.RawDecision}). Pure.
     */
    public static ResponseContract from(DecisionSnapshot snapshot) {
        DecisionFacts facts = snapshot.decisionFacts();
        DecisionSnapshot.RawDecision raw = snapshot.rawDecision();
        AwaitingDecisionType type = facts.decisionType();

        Integer minimum = facts.minimum().isKnown() ? facts.minimum().value() : null;
        Integer maximum = facts.maximum().isKnown() ? facts.maximum().value() : null;
        boolean returnAnyChange = type == AwaitingDecisionType.ARBITRARY_CARDS
                && "true".equalsIgnoreCase(first(raw.values("returnAnyChange")));

        List<String> candidateWireIds;
        List<Boolean> selectable = null;
        List<Boolean> preselected = null;
        switch (type) {
            case ACTION_CHOICE:
            case CARD_ACTION_CHOICE:
                candidateWireIds = copyOrEmpty(raw.values("actionId"));
                break;
            case CARD_SELECTION:
            case ARBITRARY_CARDS:
                candidateWireIds = copyOrEmpty(raw.values("cardId"));
                selectable = boolListOrNull(raw.values("selectable"));
                preselected = boolListOrNull(raw.values("preselected"));
                break;
            case MULTIPLE_CHOICE:
                List<String> results = raw.values("results");
                int n = results != null ? results.size() : 0;
                List<String> ordinals = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    ordinals.add(String.valueOf(i));
                }
                candidateWireIds = ordinals;
                break;
            default: // INTEGER, EMPTY
                candidateWireIds = Collections.emptyList();
        }

        boolean emptyWireAccepted;
        switch (type) {
            case EMPTY:
                emptyWireAccepted = true;
                break;
            case CARD_ACTION_CHOICE:
                // The preserved contradiction (packet F3 rule): the engine maps ""
                // to "no selected action" REGARDLESS of raw noPass
                // (CardActionSelectionDecision.java:167-169) — observed engine truth,
                // recorded, not normalized away.
                emptyWireAccepted = true;
                break;
            case CARD_SELECTION:
                emptyWireAccepted = minimum != null && minimum == 0;
                break;
            case ARBITRARY_CARDS:
                emptyWireAccepted = returnAnyChange || (minimum != null && minimum == 0);
                break;
            default: // ACTION_CHOICE, MULTIPLE_CHOICE, INTEGER
                emptyWireAccepted = false;
        }

        boolean noPass = facts.noPass().isKnown() && facts.noPass().value();
        int minForPolicy = minimum != null ? minimum : 0;
        boolean policyPassAllowed = minForPolicy == 0
                && (!noPass || textOffersCancel(facts.decisionText()));

        return new ResponseContract(type, emptyWireAccepted, policyPassAllowed,
                minimum, maximum, returnAnyChange, candidateWireIds, selectable, preselected,
                intOrNull(first(raw.values("defaultValue"))),
                intOrNull(first(raw.values("defaultIndex"))));
    }

    /**
     * V148 cancel-text detection — the DecisionSafety.mustChoose variant
     * (DecisionSafety.java:77-84): HTML card-hint divs are stripped FIRST so a card
     * name containing "done"/"optional" cannot fake cancellability.
     */
    public static boolean textOffersCancel(String decisionText) {
        if (decisionText == null) {
            return false;
        }
        String clean = decisionText.replaceAll("<div[^>]*>.*?</div>", "")
                .replaceAll("<[^>]+>", "")
                .toLowerCase(Locale.ROOT);
        return clean.contains("done") || clean.contains("cancel")
                || clean.contains("if desired") || clean.contains("optional");
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values != null ? new ArrayList<>(values) : Collections.emptyList();
    }

    /** Parse a raw true/false array; null when the engine sent no such array.
     *  Never padded or truncated — a parallel-length mismatch throws in the
     *  compact constructor so a malformed contract cannot be built silently. */
    private static List<Boolean> boolListOrNull(List<String> values) {
        if (values == null) {
            return null;
        }
        List<Boolean> result = new ArrayList<>(values.size());
        for (String v : values) {
            result.add("true".equalsIgnoreCase(v));
        }
        return result;
    }

    private static Integer intOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String first(List<String> values) {
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
}
