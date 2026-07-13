package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.List;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * F3 PURE SHADOW FINALIZER CONTRACT (2026-07-13).
 *
 * Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F3 gates:
 *  - the shared fixture corpus passes through BOTH legacy (each bot's
 *    DecisionSafety) and shadow paths with declared contradictions visible;
 *  - fixed RNG makes every fallback deterministic;
 *  - no state-event count changes and no decision callback occurs twice;
 *  - Rando and ChosenOne results compare equal except declared personality routes;
 *  - trace capture remains disabled.
 *
 * Every shadow output wire is submitted to a FRESH real engine decision from
 * {@link EngineDecisionFixtures} (packet rule: "The output is submitted to a fresh
 * real decisionMade instance"), and every fixture runs twice from fresh state with
 * the same fixed seed, comparing the exact FinalizedResponse records.
 *
 * Legacy comparisons run the SAME cases through BOTH mirrored DecisionSafety
 * copies (rando + chosenone). Divergences are asserted as EXPECTED_DIVERGENCE with
 * their audit anchors (Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md) —
 * legacy behavior is NOT changed by this lane; legacy stays authoritative
 * (Handoffs/CODEX_RANDO_RUNTIME_ROUTE_MAP_2026-07-13.md "Shadow authority").
 */
public class ResponseFinalizerContractTest {

    private static final long SEED = 42L;

    /** Counts nextInt(bound) draws so tests can prove the at-most-one-draw law. */
    static final class CountingRandom implements RandomGenerator {
        private final java.util.Random delegate;
        int intDraws;

        CountingRandom(long seed) {
            this.delegate = new java.util.Random(seed);
        }

        @Override
        public long nextLong() {
            return delegate.nextLong();
        }

        @Override
        public int nextInt(int bound) {
            intDraws++;
            return delegate.nextInt(bound);
        }
    }

    /** Run the same finalize call twice from fresh state (fresh decision, fresh
     *  fixed-seed RNG), assert the two FinalizedResponse records are EQUAL, and
     *  return the first with its draw count checked. */
    private static FinalizedResponse finalizeTwiceDeterministic(
            Supplier<? extends AwaitingDecision> freshDecision,
            ResponseIntent intent, int expectedDraws) {
        FinalizedResponse first = null;
        for (int run = 0; run < 2; run++) {
            AwaitingDecision decision = freshDecision.get();
            DecisionSnapshot snapshot = EngineDecisionFixtures.snapshotOf(decision);
            ResponseContract contract = ResponseContract.from(snapshot);
            CountingRandom random = new CountingRandom(SEED);
            FinalizedResponse response = ResponseFinalizer.finalize(
                    snapshot, contract, intent, random, RejectionHistory.empty());
            assertEquals("RNG draw count (run " + run + ")", expectedDraws, random.intDraws);
            if (first == null) {
                first = response;
            } else {
                assertEquals("fixed seed + fresh state must reproduce the exact record", first, response);
            }
        }
        return first;
    }

    // ═══ MULTIPLE_CHOICE: ordinal bounds (audit P0 #1) ═══

    @Test
    public void multipleChoiceInBoundsOrdinalAcceptedAndEngineAccepts() throws Exception {
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshMultipleChoice,
                new ResponseIntent.CandidateOrdinal(1), 0);
        assertEquals(FinalizedResponse.Status.ACCEPTED, response.status());
        assertEquals("1", response.wireResponse());
        // Submit to a FRESH real engine decision: exactly one callback.
        EngineDecisionFixtures.RecordingMultipleChoice engine =
                EngineDecisionFixtures.freshMultipleChoice();
        engine.decisionMade(response.wireResponse());
        assertEquals(1, engine.callbackCount);
        assertEquals("No", engine.chosenResult);
    }

    @Test
    public void multipleChoiceOutOfBoundsOrdinalRejectedTyped() {
        for (int bad : new int[]{2, 5, -1}) {
            FinalizedResponse response = finalizeTwiceDeterministic(
                    EngineDecisionFixtures::freshMultipleChoice,
                    new ResponseIntent.CandidateOrdinal(bad), 0);
            assertEquals("ordinal " + bad + " must be REJECTED (the engine would throw"
                            + " unchecked AIOOBE — MultipleChoiceAwaitingDecision.java:59-70, audit P0 #1;"
                            + " F1 owns the engine repair)",
                    FinalizedResponse.Status.REJECTED, response.status());
            assertEquals(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                    response.rejection().reason());
            assertNull("a rejected finalization carries no wire and no tracker mutation",
                    response.trackerMutation());
        }
    }

    @Test
    public void multipleChoicePassIsSeededForcedInBoundsAndEngineAccepts() throws Exception {
        // MULTIPLE_CHOICE rejects "" — a Pass intent needs a fallback. The shadow
        // consumes EXACTLY ONE seeded draw (recorded), always in bounds — unlike the
        // legacy emergency's blind "0"/"1" text guess (DecisionSafety.java:300-311).
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshMultipleChoice, new ResponseIntent.Pass(), 1);
        assertEquals(FinalizedResponse.Status.FORCED, response.status());
        assertNotNull("draw metadata recorded for replay", response.randomDraw());
        assertEquals(2, response.randomDraw().bound());
        EngineDecisionFixtures.RecordingMultipleChoice engine =
                EngineDecisionFixtures.freshMultipleChoice();
        engine.decisionMade(response.wireResponse());
        assertEquals(1, engine.callbackCount);
    }

    // ═══ ACTION_CHOICE: invalid action id + wire-illegal pass ═══

    @Test
    public void actionChoiceOrdinalAcceptedAndOutOfBoundsRejected() throws Exception {
        FinalizedResponse ok = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshActionChoice,
                new ResponseIntent.CandidateOrdinal(2), 0);
        assertEquals(FinalizedResponse.Status.ACCEPTED, ok.status());
        assertEquals("2", ok.wireResponse());
        EngineDecisionFixtures.RecordingActionChoice engine =
                EngineDecisionFixtures.freshActionChoice();
        engine.decisionMade(ok.wireResponse());
        assertEquals(1, engine.callbackCount);
        assertNotNull(engine.selected);

        FinalizedResponse bad = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshActionChoice,
                new ResponseIntent.CandidateOrdinal(7), 0);
        assertEquals("invalid action ordinal must be typed-rejected — the engine throws checked"
                        + " DecisionResultInvalidException which the mediator swallows into a silent"
                        + " requeue with no rescheduling (SwccgGameMediator.java:1327-1329, audit P0 #1)",
                FinalizedResponse.Status.REJECTED, bad.status());
        assertEquals(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS, bad.rejection().reason());
    }

    @Test
    public void actionChoicePassIsSeededForcedBecauseEmptyIsWireRejected() throws Exception {
        // ACTION_CHOICE rejects "" ALWAYS (ActionSelectionDecision.java:130-132) —
        // pass policy cannot make empty legal here; the shadow forces a seeded pick.
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshActionChoice, new ResponseIntent.Pass(), 1);
        assertEquals(FinalizedResponse.Status.FORCED, response.status());
        assertNotNull(response.randomDraw());
        EngineDecisionFixtures.RecordingActionChoice engine =
                EngineDecisionFixtures.freshActionChoice();
        engine.decisionMade(response.wireResponse());
        assertEquals(1, engine.callbackCount);
    }

    // ═══ CARD_ACTION_CHOICE: the preserved empty contradiction +
    //     the raw-noPass-vs-V148 divergence pair (audit P0 #2) ═══

    @Test
    public void cardActionChoicePassAcceptedDespiteRawNoPass_thePreservedContradiction() throws Exception {
        // Engine truth: "" is accepted as "no selected action" EVEN with raw
        // noPass=true (CardActionSelectionDecision.java:167-169). ResponseContract
        // preserves the contradiction (emptyWireAccepted=true, packet F3 rule);
        // the shadow ACCEPTS the pass instead of overwriting it.
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardActionChoiceNoPassNoCancelText,
                new ResponseIntent.Pass(), 0);
        assertEquals(FinalizedResponse.Status.ACCEPTED, response.status());
        assertEquals("", response.wireResponse());
        EngineDecisionFixtures.RecordingCardActionChoice engine =
                EngineDecisionFixtures.freshCardActionChoiceNoPassNoCancelText();
        engine.decisionMade(response.wireResponse());
        assertTrue(engine.decided);
        assertNull("engine-accepted 'no selected action'", engine.selected);

        // Acknowledge (explicit empty for engine types that use it) is equivalent wire.
        FinalizedResponse ack = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardActionChoiceNoPassNoCancelText,
                new ResponseIntent.Acknowledge(), 0);
        assertEquals(FinalizedResponse.Status.ACCEPTED, ack.status());
        assertEquals("", ack.wireResponse());
    }

    @Test
    public void expectedDivergence_legacySafetyForcesPickOverEngineLegalEmpty() {
        // THE raw-noPass-vs-V148 divergence pair, executable half. Same decision as
        // above: min absent, raw noPass=true, prompt offers no Done/Cancel wording.
        //  - ENGINE truth: "" accepted (proven in the test above).
        //  - SHADOW: ACCEPTED "" (proven above).
        //  - LEGACY DecisionSafety.mustChoose: the V148 exemption needs cancel text;
        //    without it, raw noPass=true returns must-choose
        //    (DecisionSafety.java:87-93), so ensureValidResponse REPLACES the legal
        //    "" with a RANDOM pick (unseeded static Random, DecisionSafety.java:147-164)
        //    — audit P0 #2: "A valid ... pass can therefore be replaced by emergency
        //    selection", P1: "Randomness has multiple owners".
        //  - The outer bots' raw-noPass emergency (RandoCalAi.java:996-1010 and the
        //    chosenone mirror) does the same overwrite BEFORE DecisionSafety even
        //    runs; it is not unit-invokable here — documented as the same divergence
        //    family, same anchors.
        EngineDecisionFixtures.RecordingCardActionChoice decision =
                EngineDecisionFixtures.freshCardActionChoiceNoPassNoCancelText();
        String[] options = decision.getDecisionParameters().get("actionId");

        String[] rando = com.gempukku.swccgo.ai.models.rando.DecisionSafety
                .ensureValidResponse(decision, "", options);
        String[] chosen = com.gempukku.swccgo.ai.models.chosenone.DecisionSafety
                .ensureValidResponse(decision, "", options);
        for (String[] legacy : new String[][]{rando, chosen}) {
            assertNotEquals("EXPECTED_DIVERGENCE (audit P0 #2): legacy overwrites the"
                    + " engine-legal empty response", "", legacy[0]);
            assertTrue("legacy forced pick must at least be an offered option, got '" + legacy[0] + "'",
                    List.of(options).contains(legacy[0]));
            assertTrue("legacy records the forced-choice reason",
                    legacy[1].contains("SAFETY FORCED"));
        }
        // Mirror parity (F3 gate): both bots corrected, same reason family. The
        // forced VALUES are draws from two unseeded static Randoms and cannot be
        // compared — membership + reason parity is the mirror assertion here.
        assertEquals(rando[1].isEmpty(), chosen[1].isEmpty());
    }

    @Test
    public void agreement_v148LegalPassAcceptedByShadowAndLegacy() {
        // Pass where V148 says legal: min==0 AND Done/Cancel text (the
        // freshCardSelectionMin0 prompt carries "or click Done to cancel").
        // Shadow ACCEPTS ""; legacy DecisionSafety.mustChoose takes the V148
        // exemption (DecisionSafety.java:64-84) and returns "" UNCORRECTED — the
        // two paths AGREE (audit P0 #2 table row "DecisionSafety.mustChoose").
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardSelectionMin0, new ResponseIntent.Pass(), 0);
        assertEquals(FinalizedResponse.Status.ACCEPTED, response.status());
        assertEquals("", response.wireResponse());

        EngineDecisionFixtures.RecordingCardSelection decision =
                EngineDecisionFixtures.freshCardSelectionMin0();
        String[] options = decision.getDecisionParameters().get("cardId");
        String[] rando = com.gempukku.swccgo.ai.models.rando.DecisionSafety
                .ensureValidResponse(decision, "", options);
        String[] chosen = com.gempukku.swccgo.ai.models.chosenone.DecisionSafety
                .ensureValidResponse(decision, "", options);
        assertEquals("AGREEMENT: legacy keeps the V148-legal empty", "", rando[0]);
        assertEquals("AGREEMENT: legacy correction reason empty", "", rando[1]);
        assertEquals("mirror parity", rando[0], chosen[0]);
        assertEquals("mirror parity", rando[1], chosen[1]);
    }

    // ═══ CARD_SELECTION: blank where min>0 (P0), exceeds-max clamp ═══

    @Test
    public void expectedDivergence_blankWhereMin2_shadowForcesLegalFill_legacyPassesBlankThrough() throws Exception {
        // Shadow: Pass against min==2 → deterministic first-sendable fill (no RNG).
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardSelectionMin2, new ResponseIntent.Pass(), 0);
        assertEquals(FinalizedResponse.Status.FORCED, response.status());
        assertNull("deterministic fill consumes no draw", response.randomDraw());
        assertEquals("101,102", response.wireResponse());
        EngineDecisionFixtures.RecordingCardSelection engine =
                EngineDecisionFixtures.freshCardSelectionMin2();
        engine.decisionMade(response.wireResponse());
        assertEquals("engine accepts the forced fill", 2, engine.selected.size());

        // LEGACY: mustChoose never reads min beyond the V148 min==0 exemption
        // (DecisionSafety.java:61-122: noPass absent → results-length → prompt-text
        // heuristics), so a blank against min==2 is NOT corrected — it goes to the
        // engine, which rejects it, and the mediator silently requeues without
        // rescheduling (audit P0 #1 strand). EXPECTED_DIVERGENCE.
        EngineDecisionFixtures.RecordingCardSelection legacyProbe =
                EngineDecisionFixtures.freshCardSelectionMin2();
        String[] options = legacyProbe.getDecisionParameters().get("cardId");
        String[] rando = com.gempukku.swccgo.ai.models.rando.DecisionSafety
                .ensureValidResponse(legacyProbe, "", options);
        String[] chosen = com.gempukku.swccgo.ai.models.chosenone.DecisionSafety
                .ensureValidResponse(legacyProbe, "", options);
        assertEquals("EXPECTED_DIVERGENCE (audit P0 #2): legacy passes the blank through", "", rando[0]);
        assertEquals("mirror parity", rando[0], chosen[0]);
        try {
            legacyProbe.decisionMade(rando[0]);
            fail("the legacy-passed blank is engine-illegal (CardsSelectionDecision.java:63-67)");
        } catch (DecisionResultInvalidException expected) {
            // the strand ingredient, pinned
        }
    }

    @Test
    public void expectedDivergence_exceedsMax_shadowClamps_legacyClampSkipsCardSelection() throws Exception {
        // Shadow: two ordinals against max==1 → MAX_CLAMP correction, engine accepts.
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardSelectionMax1,
                new ResponseIntent.CardOrdinals(List.of(0, 1)), 0);
        assertEquals(FinalizedResponse.Status.CORRECTED, response.status());
        assertEquals("101", response.wireResponse());
        assertEquals(1, response.corrections().size());
        assertEquals(FinalizedResponse.CorrectionReason.MAX_CLAMP,
                response.corrections().get(0).reason());
        EngineDecisionFixtures.RecordingCardSelection engine =
                EngineDecisionFixtures.freshCardSelectionMax1();
        engine.decisionMade(response.wireResponse());
        assertEquals(1, engine.selected.size());

        // LEGACY: the SAFETY CLAMP requires a selectable[] array of equal length
        // (DecisionSafety.java:198) — engine CARD_SELECTION sends none, so the clamp
        // SILENTLY SKIPS and the over-max response goes to the engine, which rejects
        // it. EXPECTED_DIVERGENCE.
        EngineDecisionFixtures.RecordingCardSelection legacyProbe =
                EngineDecisionFixtures.freshCardSelectionMax1();
        String[] options = legacyProbe.getDecisionParameters().get("cardId");
        String[] rando = com.gempukku.swccgo.ai.models.rando.DecisionSafety
                .ensureValidResponse(legacyProbe, "101,102", options);
        String[] chosen = com.gempukku.swccgo.ai.models.chosenone.DecisionSafety
                .ensureValidResponse(legacyProbe, "101,102", options);
        assertEquals("EXPECTED_DIVERGENCE: legacy clamp skips CARD_SELECTION"
                + " (no selectable[] array)", "101,102", rando[0]);
        assertEquals("mirror parity", rando[0], chosen[0]);
        try {
            legacyProbe.decisionMade(rando[0]);
            fail("the legacy-unclamped response is engine-illegal (CardsSelectionDecision.java:70-71)");
        } catch (DecisionResultInvalidException expected) {
            // the strand ingredient, pinned
        }
    }

    @Test
    public void cardOrdinalsOutOfBoundsRejectedTyped() {
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardSelectionMax1,
                new ResponseIntent.CardOrdinals(List.of(0, 7)), 0);
        assertEquals(FinalizedResponse.Status.REJECTED, response.status());
        assertEquals(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                response.rejection().reason());
    }

    // ═══ ARBITRARY_CARDS: locked preselection is delta-only (packet F3 rule) ═══

    @Test
    public void expectedDivergence_arbitraryPreselection_shadowDeltaOnly_legacyResendsAndEngineRejects() throws Exception {
        // Intent includes the locked preselected ordinal 0 (temp0) plus the
        // selectable delta ordinal 1 (temp1).
        // SHADOW (packet F3 rule "ARBITRARY output contains only selectable delta
        // ids"): temp0 dropped with PRESELECTED_DELTA_ONLY, wire "temp1", engine
        // accepts.
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshArbitraryLockedPreselection,
                new ResponseIntent.CardOrdinals(List.of(0, 1)), 0);
        assertEquals(FinalizedResponse.Status.CORRECTED, response.status());
        assertEquals("temp1", response.wireResponse());
        assertEquals(FinalizedResponse.CorrectionReason.PRESELECTED_DELTA_ONLY,
                response.corrections().get(0).reason());
        EngineDecisionFixtures.RecordingArbitraryCards engine =
                EngineDecisionFixtures.freshArbitraryLockedPreselection();
        engine.decisionMade(response.wireResponse());
        assertEquals(1, engine.selected.size());
        assertEquals(1, engine.callbackCount);

        // LEGACY: the clamp keeps preselected tokens as valid (sel || pre,
        // DecisionSafety.java:213-217), so "temp0,temp1" survives UNCHANGED — and
        // the engine rejects it (ArbitraryCardsSelectionDecision.java:255). This is
        // the packet's "arbitrary preselection handling" red contract, pinned
        // against legacy. EXPECTED_DIVERGENCE.
        EngineDecisionFixtures.RecordingArbitraryCards legacyProbe =
                EngineDecisionFixtures.freshArbitraryLockedPreselection();
        String[] options = legacyProbe.getDecisionParameters().get("cardId");
        String[] rando = com.gempukku.swccgo.ai.models.rando.DecisionSafety
                .ensureValidResponse(legacyProbe, "temp0,temp1", options);
        String[] chosen = com.gempukku.swccgo.ai.models.chosenone.DecisionSafety
                .ensureValidResponse(legacyProbe, "temp0,temp1", options);
        assertEquals("EXPECTED_DIVERGENCE: legacy treats the locked preselected id as sendable",
                "temp0,temp1", rando[0]);
        assertEquals("mirror parity", rando[0], chosen[0]);
        try {
            legacyProbe.decisionMade(rando[0]);
            fail("the legacy-kept resend is engine-illegal (ArbitraryCardsSelectionDecision.java:255)");
        } catch (DecisionResultInvalidException expected) {
            // the engine-rejected resend, pinned
        }
    }

    // ═══ INTEGER: bounds + the illegal legacy emergency (audit P1) ═══

    @Test
    public void integerBoundsAcceptRejectAndForcedDefault() throws Exception {
        FinalizedResponse ok = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshInteger1to5Default3,
                new ResponseIntent.IntegerValue(3), 0);
        assertEquals(FinalizedResponse.Status.ACCEPTED, ok.status());
        EngineDecisionFixtures.RecordingInteger engine =
                EngineDecisionFixtures.freshInteger1to5Default3();
        engine.decisionMade(ok.wireResponse());
        assertEquals(Integer.valueOf(3), engine.chosen);

        for (int bad : new int[]{0, 7}) {
            FinalizedResponse rejectedValue = finalizeTwiceDeterministic(
                    EngineDecisionFixtures::freshInteger1to5Default3,
                    new ResponseIntent.IntegerValue(bad), 0);
            assertEquals(FinalizedResponse.Status.REJECTED, rejectedValue.status());
            assertEquals(FinalizedResponse.RejectReason.INTEGER_OUT_OF_BOUNDS,
                    rejectedValue.rejection().reason());
        }

        // Pass against INTEGER: deterministic engine-default fallback "3" — no draw.
        FinalizedResponse forced = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshInteger1to5Default3, new ResponseIntent.Pass(), 0);
        assertEquals(FinalizedResponse.Status.FORCED, forced.status());
        assertEquals("3", forced.wireResponse());
        assertNull(forced.randomDraw());
        EngineDecisionFixtures.RecordingInteger engine2 =
                EngineDecisionFixtures.freshInteger1to5Default3();
        engine2.decisionMade(forced.wireResponse());
        assertEquals(1, engine2.callbackCount);
    }

    @Test
    public void expectedDivergence_legacyIntegerEmergencyIsEngineIllegal() throws Exception {
        // LEGACY emergency for INTEGER always answers "0" without reading the engine
        // bounds (DecisionSafety.java:294-298; audit P1 "INTEGER emergency can be
        // illegal"). Against min==1 the engine rejects it. EXPECTED_DIVERGENCE from
        // the shadow's bounds-aware default fallback above.
        EngineDecisionFixtures.RecordingInteger decision =
                EngineDecisionFixtures.freshInteger1to5Default3();
        com.gempukku.swccgo.ai.models.rando.DecisionSafety.SafetyDecision rando =
                com.gempukku.swccgo.ai.models.rando.DecisionSafety
                        .getEmergencyResponse(decision, null, null);
        com.gempukku.swccgo.ai.models.chosenone.DecisionSafety.SafetyDecision chosen =
                com.gempukku.swccgo.ai.models.chosenone.DecisionSafety
                        .getEmergencyResponse(decision, null, null);
        assertEquals("legacy INTEGER emergency is the blind 0", "0", rando.value);
        assertEquals("mirror parity", rando.value, chosen.value);
        try {
            decision.decisionMade(rando.value);
            fail("legacy's blind 0 violates the engine minimum (IntegerAwaitingDecision.java:35-47)");
        } catch (DecisionResultInvalidException expected) {
            // pinned: the legacy emergency can strand an INTEGER decision
        }
    }

    // ═══ Intent/type mismatch + purity guards ═══

    @Test
    public void intentTypeMismatchIsTypedRejection() {
        FinalizedResponse response = finalizeTwiceDeterministic(
                EngineDecisionFixtures::freshCardSelectionMax1,
                new ResponseIntent.IntegerValue(1), 0);
        assertEquals(FinalizedResponse.Status.REJECTED, response.status());
        assertEquals(FinalizedResponse.RejectReason.INTENT_TYPE_MISMATCH,
                response.rejection().reason());
    }

    @Test
    public void rejectionHistoryIsStampedNotConsumed() {
        // F3 rule: "Rejection remains typed data. F3 does not perform retries
        // itself." The history rides through into the record for the (future) F2
        // caller; it does not change the verdict.
        EngineDecisionFixtures.RecordingMultipleChoice decision =
                EngineDecisionFixtures.freshMultipleChoice();
        DecisionSnapshot snapshot = EngineDecisionFixtures.snapshotOf(decision);
        ResponseContract contract = ResponseContract.from(snapshot);
        RejectionHistory history = new RejectionHistory(List.of(
                new RejectionHistory.Attempt("5",
                        FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS, "prior attempt")));
        FinalizedResponse response = ResponseFinalizer.finalize(snapshot, contract,
                new ResponseIntent.CandidateOrdinal(0), new CountingRandom(SEED), history);
        assertEquals(FinalizedResponse.Status.ACCEPTED, response.status());
        assertEquals(1, response.priorRejectionCount());
        assertTrue(history.containsWire("5"));
    }

    @Test
    public void traceCaptureStaysDisabledAcrossTheShadowPath() {
        // Packet F3 gate: "Trace capture remains disabled after the commit."
        assertFalse(TraceSession.isActive());
        finalizeTwiceDeterministic(EngineDecisionFixtures::freshMultipleChoice,
                new ResponseIntent.CandidateOrdinal(0), 0);
        assertFalse("the shadow path must not open a trace session", TraceSession.isActive());
    }
}
