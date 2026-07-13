package com.gempukku.swccgo.ai.models.common.finalization;

import java.util.List;
import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FINALIZER LANE F3 / RESPONSE INTENT (2026-07-13) ═══
// Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F3.
// Audit: Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md §"Smallest
// consolidation seam" — ResponseIntent has ONLY these variants:
//
//   | Variant          | Payload                          |
//   | Pass             | No payload.                      |
//   | CandidateOrdinal | One typed candidate ordinal.     |
//   | CardOrdinals     | Ordered typed card ordinals.     |
//   | IntegerValue     | One integer value.               |
//   | Acknowledge      | Explicit empty/acknowledgement intent for engine
//   |                  | types that use it.                |
//
// An intent is WHAT a strategy lane wants, decoupled from wire encoding.
// Ordinals are ORIGINAL candidate ordinals into the frozen snapshot's candidate
// order (never sorted or rebuilt — DecisionSnapshot law), so an intent survives
// result-array permutation, unlike the raw index strings the legacy direct
// interceptors return (audit P0 #3).
// ═══════════════════════════════════════════════════════════
public sealed interface ResponseIntent {

    /** Strategy intentionally declines (phase pass / decline-to-act). Whether the
     *  wire accepts it is the finalizer's job, not the intent's. */
    record Pass() implements ResponseIntent {
    }

    /** One candidate by ORIGINAL ordinal — an offered action (ACTION_CHOICE /
     *  CARD_ACTION_CHOICE) or a multiple-choice result ordinal. */
    record CandidateOrdinal(int ordinal) implements ResponseIntent {
        public CandidateOrdinal {
            // Negative ordinals are representable on purpose: the finalizer must
            // produce the TYPED rejection for them (they are the audit's P0 #1
            // out-of-bounds case), not an unchecked construction failure.
        }
    }

    /** Ordered card candidates by ORIGINAL ordinal (CARD_SELECTION /
     *  ARBITRARY_CARDS). Order is preserved into the wire response. */
    record CardOrdinals(List<Integer> ordinals) implements ResponseIntent {
        public CardOrdinals {
            Objects.requireNonNull(ordinals, "ordinals");
            for (Integer ordinal : ordinals) {
                Objects.requireNonNull(ordinal, "ordinals must not contain null");
            }
            ordinals = List.copyOf(ordinals);
        }
    }

    /** One integer value (INTEGER decisions). */
    record IntegerValue(int value) implements ResponseIntent {
    }

    /** Explicit empty/acknowledgement for engine types whose empty response IS the
     *  acknowledgement (EMPTY; CARD_ACTION_CHOICE "no selected action"). Distinct
     *  from Pass: Acknowledge is not a policy decline, so the pass-legality
     *  semantic does not apply to it. */
    record Acknowledge() implements ResponseIntent {
    }
}
