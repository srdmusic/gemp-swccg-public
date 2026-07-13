package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * F0 ENGINE-CONTRACT FIXTURES (2026-07-13).
 *
 * Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F0.
 * Audit: Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md — "The engine
 * decision classes, not prompt text, define valid wire responses."
 *
 * Every assertion uses a FRESH decision instance from {@link EngineDecisionFixtures}
 * and every accepted/rejected contract runs TWICE to expose retained fixture state
 * (packet F0 rules). All contracts here describe CURRENT engine behavior and pass;
 * the post-F1 multiple-choice bounds target is isolated and named below
 * ({@link #fcMultipleChoiceBounds_checkedAfterF1}) and NOT weakened — it stays
 * {@code @Ignore}d until the F1 engine repair (a separate commit) lands.
 *
 * ── Pinned mediator P0 evidence (SwccgGameMediator.maybeLetAiPlay, source-anchored
 * here because the F2 test seam — package-private mediator constructor — is an
 * ENGINE change held for Steve's explicit approval and is NOT part of this lane):
 *  - AI success path (SwccgGameMediator.java:1299-1325) calls
 *    carryOutPendingActionsUntilDecisionNeeded + startClocksForUsersPendingDecision
 *    but NEVER addTimeSpentOnDecisionToUserClock (defined :1269; called on the
 *    human paths :1032-:1145) — the AI's pending timer stays active.
 *  - A checked DecisionResultInvalidException (:1327-1329) only calls
 *    sendAwaitingDecision — the decision is requeued but NOTHING reschedules or
 *    retries the AI: the game strands until an external event.
 *  - MAX_AI_CHAIN exhaustion (:1285-1287) returns silently — no visible terminal
 *    failure.
 * The executable retry fixtures (SwccgGameMediatorAiRetryTest) belong to F2. ──
 */
public class EngineAwaitingDecisionContractTest {

    /** Packet F0 rule: run each accepted/rejected contract twice from fresh state. */
    private static final int RUNS = 2;

    // ── fcActionChoiceEmptyRejected ──────────────────────────────────────────
    // ACTION_CHOICE rejects "" with DecisionResultInvalidException, regardless of
    // any pass policy (ActionSelectionDecision.java:130-132).
    @Test
    public void fcActionChoiceEmptyRejected() {
        for (int run = 0; run < RUNS; run++) {
            EngineDecisionFixtures.RecordingActionChoice decision =
                    EngineDecisionFixtures.freshActionChoice();
            try {
                decision.decisionMade("");
                fail("run " + run + ": engine must reject an empty ACTION_CHOICE response"
                        + " (ActionSelectionDecision.java:130-132)");
            } catch (DecisionResultInvalidException expected) {
                // engine contract holds
            }
            assertEquals("no callback on rejection", 0, decision.callbackCount);
        }
    }

    // ── fcCardActionNoPassEmptyAccepted ──────────────────────────────────────
    // CARD_ACTION_CHOICE accepts "" and calls back with null, EVEN when raw
    // noPass=true (CardActionSelectionDecision.java:167-169). This contradiction is
    // RECORDED, not normalized away (packet F0 rule): raw noPass is not empty-wire
    // legality (audit P0 #2), and the outer bots' raw-noPass emergency that
    // overwrites this legal empty (RandoCalAi.java:996-1010) is the documented
    // legacy defect.
    @Test
    public void fcCardActionNoPassEmptyAccepted() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            EngineDecisionFixtures.RecordingCardActionChoice decision =
                    EngineDecisionFixtures.freshCardActionChoiceNoPass();
            assertEquals("fixture must model the raw noPass=true wire param",
                    "true", decision.getDecisionParameters().get("noPass")[0]);
            decision.decisionMade("");
            assertTrue("run " + run + ": engine must accept the empty response", decision.decided);
            assertNull("run " + run + ": empty maps to NO selected action (null callback value)",
                    decision.selected);
            assertEquals(1, decision.callbackCount);
        }
    }

    // ── fcCardSelectionMin0Empty ─────────────────────────────────────────────
    // CARD_SELECTION with minimum zero accepts empty and returns an empty list
    // (CardsSelectionDecision.java:63-65).
    @Test
    public void fcCardSelectionMin0Empty() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            EngineDecisionFixtures.RecordingCardSelection decision =
                    EngineDecisionFixtures.freshCardSelectionMin0();
            decision.decisionMade("");
            assertNotNull(decision.selected);
            assertTrue("run " + run + ": zero-card response accepted when minimum==0",
                    decision.selected.isEmpty());
            assertEquals(1, decision.callbackCount);
        }
    }

    // ── fcCardSelectionMin2Exact ─────────────────────────────────────────────
    // Two distinct offered ids pass. Empty, one, three, duplicate, and unknown ids
    // fail (CardsSelectionDecision.java:63-85: cardinality, membership, duplicates).
    @Test
    public void fcCardSelectionMin2Exact() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            EngineDecisionFixtures.RecordingCardSelection accepting =
                    EngineDecisionFixtures.freshCardSelectionMin2();
            accepting.decisionMade("101,102");
            assertEquals("run " + run + ": exactly two distinct offered ids pass",
                    2, accepting.selected.size());
            assertEquals(1, accepting.callbackCount);

            String[][] rejected = {
                    {"", "blank where min>0 (CardsSelectionDecision.java:63-67)"},
                    {"101", "one card where min==2 (:70-71)"},
                    {"101,102,103", "three cards where max==2 (:70-71)"},
                    {"101,101", "duplicate card (:76-77)"},
                    {"101,999", "unknown card id (:93-99)"},
            };
            for (String[] c : rejected) {
                EngineDecisionFixtures.RecordingCardSelection fresh =
                        EngineDecisionFixtures.freshCardSelectionMin2();
                try {
                    fresh.decisionMade(c[0]);
                    fail("run " + run + ": engine must reject '" + c[0] + "' — " + c[1]);
                } catch (DecisionResultInvalidException expected) {
                    // engine contract holds
                }
                assertEquals("no callback on rejection", 0, fresh.callbackCount);
            }
        }
    }

    // ── fcArbitraryReturnAnyChange ───────────────────────────────────────────
    // Locked preselection plus returnAnyChange=true accepts ONE selectable tempN
    // delta despite minimum two (cardinality bypass:
    // ArbitraryCardsSelectionDecision.java:248). The wire response must NOT resend
    // locked preselected ids: temp0 is preselected but NOT in the selectable
    // collection, and the validator rejects any response containing it (:255) —
    // which is exactly why the LEGACY clamp rebuild that resends preselected ids
    // (DecisionSafety.java:222-227) produces engine-rejected wire.
    @Test
    public void fcArbitraryReturnAnyChange() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            EngineDecisionFixtures.RecordingArbitraryCards decision =
                    EngineDecisionFixtures.freshArbitraryLockedPreselection();
            // one selectable delta, below minimum two: accepted via the bypass
            decision.decisionMade("temp1");
            assertEquals("run " + run + ": one valid change accepted below minimum",
                    1, decision.selected.size());
            assertEquals(1, decision.callbackCount);

            // resending the locked preselected id is REJECTED by the engine
            EngineDecisionFixtures.RecordingArbitraryCards resend =
                    EngineDecisionFixtures.freshArbitraryLockedPreselection();
            try {
                resend.decisionMade("temp0,temp1");
                fail("run " + run + ": engine must reject a resent locked preselected id"
                        + " (ArbitraryCardsSelectionDecision.java:255)");
            } catch (DecisionResultInvalidException expected) {
                // engine contract holds
            }
            assertEquals(0, resend.callbackCount);

            // empty is also accepted under the same bypass
            EngineDecisionFixtures.RecordingArbitraryCards empty =
                    EngineDecisionFixtures.freshArbitraryLockedPreselection();
            empty.decisionMade("");
            assertTrue(empty.selected.isEmpty());
            assertEquals(1, empty.callbackCount);
        }
    }

    // ── fcMultipleChoiceBounds (today's pinned defect) ───────────────────────
    // A valid ordinal maps to the result at that ordinal. TODAY, negative and
    // >=size ordinals throw UNCHECKED ArrayIndexOutOfBoundsException — the parsed
    // int indexes _possibleResults with NO range guard
    // (MultipleChoiceAwaitingDecision.java:59-70). Unchecked means it BYPASSES the
    // mediator's checked DecisionResultInvalidException catch
    // (SwccgGameMediator.java:1327-1329) and can strand scheduling (audit P0 #1).
    // This test PINS the defect so any engine change is caught; the checked target
    // is the named, isolated red contract below (packet F0 gate: "isolated and
    // named. Do not weaken their expected engine behavior").
    @Test
    public void fcMultipleChoiceBounds_todayUncheckedOrdinalPinned() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            EngineDecisionFixtures.RecordingMultipleChoice decision =
                    EngineDecisionFixtures.freshMultipleChoice();
            decision.decisionMade("1");
            assertEquals("run " + run + ": ordinal maps to the result at that ordinal",
                    1, decision.chosenIndex);
            assertEquals("No", decision.chosenResult);
            assertEquals(1, decision.callbackCount);

            for (String bad : new String[]{"2", "-1"}) { // exactly size, negative
                EngineDecisionFixtures.RecordingMultipleChoice fresh =
                        EngineDecisionFixtures.freshMultipleChoice();
                try {
                    fresh.decisionMade(bad);
                    fail("run " + run + ": today's engine must throw on ordinal '" + bad + "'");
                } catch (ArrayIndexOutOfBoundsException documentedDefect) {
                    // TODAY: unchecked — NOT DecisionResultInvalidException — escapes
                    // the mediator catch. F1 (separate commit) owns the repair.
                } catch (DecisionResultInvalidException e) {
                    fail("engine grew a range guard: promote fcMultipleChoiceBounds_checkedAfterF1"
                            + " and retire this pin (F1 landed?)");
                }
                assertEquals(0, fresh.callbackCount);
            }

            // non-numeric IS already checked today (:66-68)
            EngineDecisionFixtures.RecordingMultipleChoice nonNumeric =
                    EngineDecisionFixtures.freshMultipleChoice();
            try {
                nonNumeric.decisionMade("Yes");
                fail("non-numeric must be checked-rejected ('Unknown response number')");
            } catch (DecisionResultInvalidException expected) {
                // engine contract holds
            }
        }
    }

    // ── fcMultipleChoiceBounds (post-F1 target — ISOLATED AND NAMED, not weakened) ──
    // Packet F0: "Negative and size ordinals must become checked invalid results
    // after F1." F1 is a separate engine commit (MultipleChoiceAwaitingDecision
    // bounds guard) not delivered by this lane; un-ignore when it lands.
    @Ignore("RED until F1 lands: MultipleChoiceAwaitingDecision has no ordinal bounds guard"
            + " (packet CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F1)")
    @Test
    public void fcMultipleChoiceBounds_checkedAfterF1() throws Exception {
        for (String bad : new String[]{"2", "-1"}) {
            EngineDecisionFixtures.RecordingMultipleChoice fresh =
                    EngineDecisionFixtures.freshMultipleChoice();
            try {
                fresh.decisionMade(bad);
                fail("ordinal '" + bad + "' must be checked-rejected after F1");
            } catch (DecisionResultInvalidException expected) {
                // F1 target contract
            }
        }
    }

    // ── fcIntegerBounds ──────────────────────────────────────────────────────
    // Inclusive minimum/maximum pass. Outside and non-numeric values fail
    // (IntegerAwaitingDecision.java:35-51). Raw defaultValue remains available.
    @Test
    public void fcIntegerBounds() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            for (String good : new String[]{"1", "3", "5"}) { // inclusive min, mid, inclusive max
                EngineDecisionFixtures.RecordingInteger fresh =
                        EngineDecisionFixtures.freshInteger1to5Default3();
                fresh.decisionMade(good);
                assertEquals("run " + run + ": '" + good + "' accepted",
                        Integer.valueOf(good), fresh.chosen);
                assertEquals(1, fresh.callbackCount);
            }
            for (String bad : new String[]{"0", "6", "", "x"}) {
                EngineDecisionFixtures.RecordingInteger fresh =
                        EngineDecisionFixtures.freshInteger1to5Default3();
                try {
                    fresh.decisionMade(bad);
                    fail("run " + run + ": engine must reject INTEGER response '" + bad + "'");
                } catch (DecisionResultInvalidException expected) {
                    // engine contract holds
                }
                assertEquals(0, fresh.callbackCount);
            }
            // Raw defaultValue remains available on the wire params (packet F0).
            assertEquals("3", EngineDecisionFixtures.freshInteger1to5Default3()
                    .getDecisionParameters().get("defaultValue")[0]);
        }
    }
}
