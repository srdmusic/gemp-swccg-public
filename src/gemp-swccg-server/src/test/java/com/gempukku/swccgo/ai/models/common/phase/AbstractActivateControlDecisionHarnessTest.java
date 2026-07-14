package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ACTIVATE + CONTROL Decide-Equivalent Harness
 * (packet Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_B_PACKET_2026-07-13.md). Freezes the
 * live typed owner boundary and the documented zero-confirmation correction.
 *
 * ONE abstract JUnit 4 contract (this class) owns: the scripted immutable AwaitingDecision
 * helper, a configurable minimal GameState subclass, the inherited tests, and the
 * structured trace assertions. Each bot adapter (rando + chosenone) overrides ONLY
 * {@link #runDecision}, because {@code setDecisionTraceSinkForTesting} is intentionally
 * package-visible. The six tests live ONCE, here.
 *
 * Every scripted decision carries the real engine {@code decisionOrigin} wire parameter
 * (the exact shapes are the real-engine {@code ActivateControlEngineContractTest} shapes,
 * reproduced as pure scripted decisions — no VirtualTableScenario, no game start, no
 * replay, no log). Assertions are EXACT and structured (never decimal-tolerant, never
 * winner-only): decision identity, origin stamp retained in the raw parameter map, ordered
 * raw/merge candidate arrays, selected route, every operation field-by-field in order with
 * raw float bits, pre-safety winner, pass eligibility, corrections, and the final response
 * — frozen to the packet's table. A COMPLETE fixture uses the StrictFixtureSink (the bot
 * adapter), so an INCOMPLETE capture can never become evidence. Rando and ChosenOne share
 * this ONE contract with the SAME frozen expectations, so any candidate/score/veto/route/
 * response divergence fails one adapter's run (a packet hard stop).
 */
public abstract class AbstractActivateControlDecisionHarnessTest {

    // ── discovery switch ──────────────────────────────────────────────────────
    // FROZEN false for the committed evidence. Flip true LOCALLY only to re-dump the
    // captured current-source traces to stdout (see dump()) and re-freeze the expected
    // records below after a DELIBERATE source change; a committed true would silence the
    // operation assertions, so it must stay false.
    private static final boolean DUMP = false;

    /** The player id each bot adapter passes to decide() as the decision recipient. Shared
     *  so the allowance fixture can prove the recipient differs from the current turn player. */
    protected static final String DECIDING_PLAYER = "tester";

    // =========================================================================
    // Bot seam — the ONLY per-bot operation (package-visible setter lives per bot)
    // =========================================================================

    /** One captured decision: the traced wire response and the sink's single trace.
     *  Public so each bot adapter (a different package) can construct it. */
    public record CapturedDecision(String response, DecisionTrace trace) {}

    /**
     * Run the identical scripted decision twice on two fresh bots: first with the
     * production-default no-op sink, then with a StrictFixtureSink; assert the two wire
     * responses are identical and neither leaks a trace session; return the traced
     * response plus the sink's single DecisionTrace. Implemented per bot because the
     * trace-sink setter is package-visible.
     */
    protected abstract CapturedDecision runDecision(AwaitingDecision decision, GameState gameState);

    /** Run the same decision through the mediator-facing typed boundary. */
    protected abstract AiDecisionResult runEngineDecision(
        AwaitingDecision decision, GameState gameState, RejectionHistory history);

    // =========================================================================
    // Expected operation record (structured; never a formatted-decimal/hash compare)
    // =========================================================================

    protected record ExpectedOp(TraceOp op, int candidateOrdinal, String syntheticSource,
                                String actionId, String evaluatorId, TraceRuleId ruleId,
                                TraceDomainId domainId, TraceOutputKind outputKind,
                                Integer beforeBits, Integer deltaBits, Integer afterBits,
                                boolean vetoed, String vetoReason, String detail) {}

    // =========================================================================
    // SIX fixtures (packet §Pure Fixtures table — reproduced exactly)
    // =========================================================================

    /** activateTopLevel: ACTIVATE CARD_ACTION_CHOICE, one Activate Force action + optional
     *  Pass -> offered action id, route ACTIVATE_TOP_LEVEL. */
    @Test
    public void activateTopLevel() {
        AwaitingDecision decision = decision(101, AwaitingDecisionType.CARD_ACTION_CHOICE,
            "Choose Activate action or Pass",
            params(
                "actionId", arr("A1"),
                "actionText", arr("Activate Force"),
                "min", arr("0"),
                "noPass", arr("false"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.PHASE_ACTION.name())));
        CapturedDecision cap = runDecision(decision, stub(Phase.ACTIVATE));
        verifyEvaluatorLane("activateTopLevel", cap, decision, Phase.ACTIVATE,
            TraceRoute.ACTIVATE_TOP_LEVEL,
            DecisionOrigin.PHASE_ACTION, "A1",
            list("A1"), list("A1", ""),
            /* obligations known */ true, Set.of(),
            /* preSafetyWinner */ "A1", /* passEligible */ Boolean.TRUE,
            ACTIVATE_TOP_LEVEL_OPS);
    }

    /** controlTopLevel: CONTROL CARD_ACTION_CHOICE, one Force drain action (no location
     *  card) + optional Pass → offered action id, route COMBINED_EVALUATOR. */
    @Test
    public void controlTopLevel() {
        AwaitingDecision decision = decision(102, AwaitingDecisionType.CARD_ACTION_CHOICE,
            "Choose Control action or Pass",
            params(
                "actionId", arr("C1"),
                "actionText", arr("Force drain"),
                // one aligned nonblank source cardId: the real engine always serializes the
                // attached action card (CardActionSelectionDecision:69). The pure stub resolves
                // this id to null on purpose — CONTROL is a top-level routing/merge smoke, not a
                // location-dependent drain oracle (those guards are deferred).
                "cardId", arr("Cdrain1"),
                "min", arr("0"),
                "noPass", arr("false"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.PHASE_ACTION.name())));
        CapturedDecision cap = runDecision(decision, stub(Phase.CONTROL));
        verifyEvaluatorLane("controlTopLevel", cap, decision, Phase.CONTROL,
            TraceRoute.CONTROL_TOP_LEVEL,
            DecisionOrigin.PHASE_ACTION, "C1",
            list("C1"), list("C1", ""),
            true, Set.of(),
            "C1", Boolean.TRUE,
            CONTROL_TOP_LEVEL_OPS);
    }

    /** activateAmount: ACTIVATE INTEGER real engine min 0, max/default 3 → 3, route COMBINED_EVALUATOR. */
    @Test
    public void activateAmount() {
        AwaitingDecision decision = decision(103, AwaitingDecisionType.INTEGER,
            "Choose amount of Force to activate",
            params(
                // real engine ACTIVATE_AMOUNT min is 0 (AbstractSwccgCardBlueprint:2243)
                "min", arr("0"),
                "max", arr("3"),
                "defaultValue", arr("3"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_AMOUNT.name())));
        CapturedDecision cap = runDecision(decision, stub(Phase.ACTIVATE));
        // obligationFlags UNKNOWN: the bot boundary derives them only when the noPass
        // parameter is present, and a real INTEGER amount decision carries no noPass.
        verifyEvaluatorLane("activateAmount", cap, decision, Phase.ACTIVATE,
            TraceRoute.ACTIVATE_AMOUNT,
            DecisionOrigin.ACTIVATE_AMOUNT, "3",
            list(), list("3"),
            false, null,
            "3", Boolean.FALSE,
            ACTIVATE_AMOUNT_OPS);
    }

    /** activateAllowance: ACTIVATE INTEGER min 1 max/default 3, recipient != turn player
     *  (the opponent-allowance branch) → 3, route COMBINED_EVALUATOR. */
    @Test
    public void activateAllowance() {
        AwaitingDecision decision = decision(104, AwaitingDecisionType.INTEGER,
            "Choose amount of Force to allow opponent to activate without you performing a top-level action",
            params(
                "min", arr("1"),
                "max", arr("3"),
                "defaultValue", arr("3"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_ALLOWANCE.name())));
        // The ALLOWANCE decision is answered by the NON-turn player: encode a concrete
        // current turn player ("opponent") that DIFFERS from the decider (DECIDING_PLAYER),
        // and prove the distinction is real (not merely claimed in prose).
        GameState gameState = stubWithTurnPlayer(Phase.ACTIVATE, "opponent");
        assertNotEquals("allowance recipient (decider) must differ from the current turn player",
            DECIDING_PLAYER, gameState.getCurrentPlayerId());
        CapturedDecision cap = runDecision(decision, gameState);
        verifyEvaluatorLane("activateAllowance", cap, decision, Phase.ACTIVATE,
            TraceRoute.ACTIVATE_ALLOWANCE,
            DecisionOrigin.ACTIVATE_ALLOWANCE, "3",
            list(), list("3"),
            false, null,
            "3", Boolean.FALSE,
            ACTIVATE_ALLOWANCE_OPS);
    }

    /** Normal zero activation declines Pass by selecting the original ordinal of No. */
    @Test
    public void activateZeroConfirmSelectsNo() {
        AwaitingDecision decision = decision(105, AwaitingDecisionType.MULTIPLE_CHOICE,
            "You have not activated Force. Do you want to Pass?",
            params(
                "results", arr("Yes", "No"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_ZERO_CONFIRM.name())));
        CapturedDecision cap = runDecision(decision, stub(Phase.ACTIVATE));
        verifyOwnedLabelRoute("activateZeroConfirmSelectsNo", cap, decision, Phase.ACTIVATE,
            DecisionOrigin.ACTIVATE_ZERO_CONFIRM, TraceRoute.ACTIVATE_ZERO_CONFIRM,
            "1", list("0", "1"));
    }

    @Test
    public void activateZeroConfirmSelectsNoAtItsOriginalReversedOrdinal() {
        AwaitingDecision decision = decision(108, AwaitingDecisionType.MULTIPLE_CHOICE,
            "You have not activated Force. Do you want to Pass?",
            params(
                "results", arr("No", "Yes"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_ZERO_CONFIRM.name())));
        CapturedDecision cap = runDecision(decision, stubWithReserve(Phase.ACTIVATE, 4));
        verifyOwnedLabelRoute("activateZeroConfirmReversed", cap, decision, Phase.ACTIVATE,
            DecisionOrigin.ACTIVATE_ZERO_CONFIRM, TraceRoute.ACTIVATE_ZERO_CONFIRM,
            "0", list("0", "1"));
    }

    @Test
    public void activateZeroConfirmKeepsThreeAcrossBothLabelOrders() {
        AwaitingDecision yesFirst = decision(109, AwaitingDecisionType.MULTIPLE_CHOICE,
            "You have not activated Force. Do you want to Pass?",
            params(
                "results", arr("Yes", "No"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_ZERO_CONFIRM.name())));
        CapturedDecision yesFirstCap = runDecision(
            yesFirst, stubWithReserve(Phase.ACTIVATE, 3));
        verifyOwnedLabelRoute("activateZeroConfirmKeepThreeYesFirst", yesFirstCap,
            yesFirst, Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ZERO_CONFIRM,
            TraceRoute.ACTIVATE_ZERO_CONFIRM, "0", list("0", "1"));

        AwaitingDecision noFirst = decision(110, AwaitingDecisionType.MULTIPLE_CHOICE,
            "You have not activated Force. Do you want to Pass?",
            params(
                "results", arr("No", "Yes"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_ZERO_CONFIRM.name())));
        CapturedDecision noFirstCap = runDecision(
            noFirst, stubWithReserve(Phase.ACTIVATE, 3));
        verifyOwnedLabelRoute("activateZeroConfirmKeepThreeNoFirst", noFirstCap,
            noFirst, Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ZERO_CONFIRM,
            TraceRoute.ACTIVATE_ZERO_CONFIRM, "1", list("0", "1"));
    }

    /** ACTIVATE interruption acknowledgement selects the sole original OK ordinal. */
    @Test
    public void activateInterruptionAck() {
        AwaitingDecision decision = decision(106, AwaitingDecisionType.MULTIPLE_CHOICE,
            "Opponent chose to interrupt Force activation. Acknowledge to continue.",
            params(
                "results", arr("OK"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_INTERRUPTION_ACK.name())));
        CapturedDecision cap = runDecision(decision, stub(Phase.ACTIVATE));
        verifyOwnedLabelRoute("activateInterruptionAck", cap, decision, Phase.ACTIVATE,
            DecisionOrigin.ACTIVATE_INTERRUPTION_ACK, TraceRoute.ACTIVATE_ACK,
            "0", list("0"));
    }

    /** Direct callers keep the frozen safe ordinal while mediator callers receive the
     *  typed rejection. Neither path re-enters the other after ownership is established. */
    @Test
    public void malformedZeroConfirmationSplitsDirectCompatibilityFromEngineRejection() {
        AwaitingDecision decision = decision(107, AwaitingDecisionType.MULTIPLE_CHOICE,
            "You have not activated Force. Do you want to Pass?",
            params(
                "results", arr("Yes", "Maybe"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.ACTIVATE_ZERO_CONFIRM.name())));
        GameState gameState = stub(Phase.ACTIVATE);

        CapturedDecision direct = runDecision(decision, gameState);
        verifyOwnedLabelRoute("malformedZeroConfirmationDirect", direct, decision,
            Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ZERO_CONFIRM,
            TraceRoute.ACTIVATE_ZERO_CONFIRM, "0", list("0", "1"));

        RejectionHistory history = RejectionHistory.empty();
        AiDecisionResult engine = runEngineDecision(decision, gameState, history);
        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, engine.status());
        assertEquals(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
            engine.rejectionCode());
        assertNotNull(engine.rejectionDetail());
        assertNull(engine.wireResponse());
        assertEquals(0, history.size());
    }

    // =========================================================================
    // Frozen expected operations for the four evaluator-backed owners. The two
    // MULTIPLE_CHOICE label owners do not run CombinedEvaluator and record no operations.
    //
    // FROZEN VERBATIM from the captured current-source traces (baseline 443248a65,
    // DUMP=true). Rando and ChosenOne produce byte-identical operation streams over the
    // pure stub, so one frozen list serves both bots. Raw float bits are exact
    // (Float.floatToRawIntBits). Re-dump and re-freeze on a deliberate source change; a
    // drift here is a regression signal, not a fixture to loosen.
    // =========================================================================

    protected static final List<ExpectedOp> ACTIVATE_TOP_LEVEL_OPS = List.of(
        new ExpectedOp(TraceOp.INITIAL, 0, null, "A1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, null, null, 0, false, null, "Activate Force"),
        new ExpectedOp(TraceOp.ADD, 0, null, "A1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 0, 1167867904, 1167867904, false, null, "V168 ALWAYS ACTIVATE: never pass Force activation while Force can be activated"),
        new ExpectedOp(TraceOp.ADD, 0, null, "A1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1167867904, 1140457472, 1168891904, false, null, "V38.3 ALWAYS ACTIVATE: Force is currency — activate it!"),
        new ExpectedOp(TraceOp.ADD, 0, null, "A1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1168891904, 0, 1168891904, false, null, "Unknown action type"),
        new ExpectedOp(TraceOp.INITIAL, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, null, null, 1084227584, false, null, "Pass / Do nothing"),
        new ExpectedOp(TraceOp.ADD, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1084227584, 0, 1084227584, false, null, "Default pass option"),
        new ExpectedOp(TraceOp.ADD, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1084227584, -1069547520, 1073741824, false, null, "Early game - reduced pass preference"),
        new ExpectedOp(TraceOp.RANK, 0, null, "A1", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1168891904, false, null, "pre-final best"),
        new ExpectedOp(TraceOp.SELECT, 0, null, "A1", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1168891904, false, null, "winner"));

    protected static final List<ExpectedOp> CONTROL_TOP_LEVEL_OPS = List.of(
        new ExpectedOp(TraceOp.INITIAL, 0, null, "C1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, null, null, 0, false, null, "Force drain"),
        new ExpectedOp(TraceOp.ADD, 0, null, "C1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 0, 1116471296, 1116471296, false, null, "Force drain (no deployable cards - our only pressure!)"),
        new ExpectedOp(TraceOp.ADD, 0, null, "C1", "ActionText", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1116471296, 0, 1116471296, false, null, "Unknown action type"),
        new ExpectedOp(TraceOp.INITIAL, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, null, null, 1084227584, false, null, "Pass / Do nothing"),
        new ExpectedOp(TraceOp.ADD, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1084227584, 0, 1084227584, false, null, "Default pass option"),
        new ExpectedOp(TraceOp.ADD, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1084227584, -1069547520, 1073741824, false, null, "Early game - reduced pass preference"),
        new ExpectedOp(TraceOp.ADD, -2, null, "", "Pass", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1073741824, 1082130432, 1086324736, false, null, "Small hand (0) - save force for drawing"),
        new ExpectedOp(TraceOp.RANK, 0, null, "C1", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1116471296, false, null, "pre-final best"),
        new ExpectedOp(TraceOp.SELECT, 0, null, "C1", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1116471296, false, null, "winner"));

    protected static final List<ExpectedOp> ACTIVATE_AMOUNT_OPS = List.of(
        new ExpectedOp(TraceOp.INITIAL, -2, null, "3", "ForceActivation", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, null, null, 1112014848, false, null, "Activate 3 of 3 force"),
        new ExpectedOp(TraceOp.ADD, -2, null, "3", "ForceActivation", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1112014848, 1092616192, 1114636288, false, null, "Activating full amount available"),
        new ExpectedOp(TraceOp.RANK, -2, null, "3", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1114636288, false, null, "pre-final best"),
        new ExpectedOp(TraceOp.SELECT, -2, null, "3", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1114636288, false, null, "winner"));

    protected static final List<ExpectedOp> ACTIVATE_ALLOWANCE_OPS = List.of(
        new ExpectedOp(TraceOp.INITIAL, -2, null, "3", "ForceActivation", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, null, null, 1112014848, false, null, "Allow opponent to activate 3 force"),
        new ExpectedOp(TraceOp.ADD, -2, null, "3", "ForceActivation", TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED, TraceOutputKind.LEGACY_UNTAGGED, 1112014848, 0, 1112014848, false, null, "Allowing opponent max activation (normal SWCCG rule)"),
        new ExpectedOp(TraceOp.RANK, -2, null, "3", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1112014848, false, null, "pre-final best"),
        new ExpectedOp(TraceOp.SELECT, -2, null, "3", "COMBINED_EVALUATOR", TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, null, null, 1112014848, false, null, "winner"));

    // =========================================================================
    // Shared structured assertions
    // =========================================================================

    /** COMBINED_EVALUATOR fixture: the evaluator lane ran and recorded the pre-safety
     *  winner, pass eligibility, and the operation stream. */
    private void verifyEvaluatorLane(String name, CapturedDecision cap, AwaitingDecision decision,
                                     Phase phase, TraceRoute expectedRoute,
                                     DecisionOrigin origin, String expectedResponse,
                                     List<String> expectedRawCandidateOrder, List<String> expectedMergeOrder,
                                     boolean obligationsKnown, Set<DecisionFacts.ObligationFlag> obligationsValue,
                                     String expectedPreSafetyWinnerActionId, Boolean expectedPassEligible,
                                     List<ExpectedOp> expectedOps) {
        DecisionTrace trace = commonHeader(name, cap, decision, phase, origin,
            expectedRoute, expectedResponse, expectedRawCandidateOrder,
            expectedMergeOrder, obligationsKnown, obligationsValue);

        var fin = trace.getFinalization();
        assertTrue(name + ": pre-safety winner must be RECORDED on the evaluator lane",
            fin.preSafetyWinnerRecorded());
        assertNull(name + ": pre-safety winner must not be marked n/a on the evaluator lane",
            fin.preSafetyWinnerNotApplicableReason());
        assertEquals(name + ": pre-safety winner action id",
            expectedPreSafetyWinnerActionId, fin.preSafetyWinnerActionId());
        assertEquals(name + ": pass eligibility", expectedPassEligible, fin.passEligible());
        assertNull(name + ": pass eligibility must not be marked n/a on the evaluator lane",
            fin.passEligibilityNotApplicableReason());

        assertOps(name, trace, expectedOps);
    }

    /** Typed label owner: no evaluator scoring and both evaluator-lane facts are n/a. */
    private void verifyOwnedLabelRoute(String name, CapturedDecision cap,
                                       AwaitingDecision decision, Phase phase,
                                       DecisionOrigin origin, TraceRoute route,
                                       String expectedResponse,
                                       List<String> expectedRawCandidateOrder) {
        DecisionTrace trace = commonHeader(name, cap, decision, phase, origin,
            route, expectedResponse, expectedRawCandidateOrder,
            list(), false, null);

        var fin = trace.getFinalization();
        String reason = "typed ACTIVATE label owner selects the original result ordinal";
        assertFalse(name + ": pre-safety winner is not an evaluator fact",
            fin.preSafetyWinnerRecorded());
        assertNull(name + ": pre-safety winner action id", fin.preSafetyWinnerActionId());
        assertEquals(name + ": pre-safety winner n/a reason",
            reason, fin.preSafetyWinnerNotApplicableReason());
        assertNull(name + ": pass eligibility", fin.passEligible());
        assertEquals(name + ": pass eligibility n/a reason",
            reason, fin.passEligibilityNotApplicableReason());

        assertTrue(name + ": label owner records no evaluator operations: " + trace.getOperations(),
            trace.getOperations().isEmpty());
    }

    /** Shared top-level envelope assertions common to both route families. */
    private DecisionTrace commonHeader(String name, CapturedDecision cap, AwaitingDecision decision,
                                       Phase phase, DecisionOrigin origin, TraceRoute expectedRoute,
                                       String expectedResponse, List<String> expectedRawCandidateOrder,
                                       List<String> expectedMergeOrder, boolean obligationsKnown,
                                       Set<DecisionFacts.ObligationFlag> obligationsValue) {
        assertNotNull(name + ": capture", cap);
        DecisionTrace trace = cap.trace();
        assertNotNull(name + ": trace", trace);
        if (DUMP) {
            dump(name, cap);
        }

        // traced==untraced wire response is asserted inside the adapter (both bots run it);
        // here the traced wire response must equal the frozen table value.
        assertEquals(name + ": traced wire response == frozen table", expectedResponse, cap.response());

        // no trace session leaks onto the thread after decide()
        assertFalse(name + ": decide() must not leak a trace session", TraceSession.isActive());

        // a COMPLETE envelope (StrictFixtureSink already rejects INCOMPLETE)
        assertEquals(name + ": status", TraceStatus.COMPLETE, trace.getStatus());
        assertTrue(name + ": captureFailures must be empty on COMPLETE",
            trace.getCaptureFailures().isEmpty());
        assertEquals(name + ": schemaVersion", DecisionTrace.SCHEMA_VERSION, trace.getSchemaVersion());
        assertTrue(name + ": botModel is the bot source-package identifier",
            trace.getBotModel().contains("models"));

        // decision identity — id/type/text exact
        assertEquals(name + ": decisionId", String.valueOf(decision.getAwaitingDecisionId()),
            trace.getDecisionId());
        assertEquals(name + ": decisionType", decision.getDecisionType().name(), trace.getDecisionType());
        assertEquals(name + ": decisionText", decision.getText(), trace.getDecisionText());

        // snapshot present; phase exact; decisionType matches the typed enum
        var snapshot = trace.getSnapshot();
        assertNotNull(name + ": snapshot", snapshot);
        assertEquals(name + ": snapshot phase", phase, snapshot.decisionFacts().phase());
        assertEquals(name + ": snapshot decisionType", decision.getDecisionType(),
            snapshot.decisionFacts().decisionType());

        // origin stamp retained VERBATIM in the raw parameter map
        assertTrue(name + ": raw parameter map retains the decisionOrigin key",
            snapshot.rawDecision().has(DecisionOrigin.WIRE_PARAMETER));
        assertEquals(name + ": decisionOrigin stamp retained",
            List.of(origin.name()), snapshot.rawDecision().values(DecisionOrigin.WIRE_PARAMETER));

        // FULL raw-decision fidelity (packet Required Assertions: ordered raw arrays exact).
        // Bot-boundary capture => Source.ENGINE_PARAMETERS with every scripted parameter key
        // preserved verbatim, in key set/order, blank/present-empty semantics intact (a null
        // value array is retained as present-empty).
        var rawDecision = snapshot.rawDecision();
        assertEquals(name + ": rawDecision source",
            DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, rawDecision.source());
        Map<String, String[]> scriptedParams = decision.getDecisionParameters();
        assertEquals(name + ": raw parameter key set/order",
            List.copyOf(scriptedParams.keySet()),
            List.copyOf(rawDecision.parameters().keySet()));
        for (Map.Entry<String, String[]> entry : scriptedParams.entrySet()) {
            String[] v = entry.getValue();
            List<String> expected = (v != null) ? Arrays.asList(v) : List.of();
            assertEquals(name + ": raw parameter values for key '" + entry.getKey() + "'",
                expected, rawDecision.parameters().get(entry.getKey()));
        }

        // obligation flags (typed, derived). KNOWN => exact value; else UNKNOWN.
        var flags = snapshot.decisionFacts().obligationFlags();
        assertEquals(name + ": obligationFlags known", obligationsKnown, flags.isKnown());
        if (obligationsKnown) {
            assertEquals(name + ": obligationFlags value", obligationsValue, flags.value());
        }

        // selected route exact
        assertNotNull(name + ": route", trace.getRoute());
        assertEquals(name + ": route", expectedRoute, trace.getRoute().selected());

        // raw candidate order + merge order exact and UNSORTED
        assertEquals(name + ": rawCandidateOrder", expectedRawCandidateOrder, trace.getRawCandidateOrder());
        assertEquals(name + ": mergeOrder", expectedMergeOrder, trace.getMergeOrder());

        // finalization: final response == frozen table, recorded, common finalizer NOT skipped
        var fin = trace.getFinalization();
        assertTrue(name + ": finalResponse recorded", fin.finalResponseRecorded());
        assertEquals(name + ": finalResponse", expectedResponse, fin.finalResponse());
        assertFalse(name + ": the bot boundary ran the common finalizer (not a direct interceptor)",
            fin.skippedCommonFinalizer());
        assertTrue(name + ": no DecisionSafety corrections on this frozen decision: " + fin.corrections(),
            fin.corrections().isEmpty());
        return trace;
    }

    /** Assert every operation in order, all fields, raw float bits exact. Always asserts
     *  — there is no discovery bypass; operation/score drift is tested on every run. */
    private void assertOps(String name, DecisionTrace trace, List<ExpectedOp> expected) {
        List<TraceOperation> ops = trace.getOperations();
        for (int i = 0; i < Math.min(ops.size(), expected.size()); i++) {
            TraceOperation a = ops.get(i);
            ExpectedOp e = expected.get(i);
            String at = name + " op[" + i + "].";
            assertEquals(at + "op", e.op(), a.getOp());
            assertEquals(at + "candidateOrdinal", e.candidateOrdinal(), a.getCandidateOrdinal());
            assertEquals(at + "syntheticSource", e.syntheticSource(), a.getSyntheticSource());
            assertEquals(at + "actionId", e.actionId(), a.getActionId());
            assertEquals(at + "evaluatorId", e.evaluatorId(), a.getEvaluatorId());
            assertEquals(at + "ruleId", e.ruleId(), a.getRuleId());
            assertEquals(at + "domainId", e.domainId(), a.getDomainId());
            assertEquals(at + "outputKind", e.outputKind(), a.getOutputKind());
            assertEquals(at + "beforeBits", e.beforeBits(), a.getBeforeBits());
            assertEquals(at + "deltaBits", e.deltaBits(), a.getDeltaBits());
            assertEquals(at + "afterBits", e.afterBits(), a.getAfterBits());
            assertEquals(at + "vetoed", e.vetoed(), a.isVetoed());
            assertEquals(at + "vetoReason", e.vetoReason(), a.getVetoReason());
            assertEquals(at + "detail", e.detail(), a.getDetail());
        }
        assertEquals(name + ": operation count", expected.size(), ops.size());
    }

    // =========================================================================
    // Scripted engine-shaped decision + minimal GameState stub
    // =========================================================================

    /** Minimal scripted AwaitingDecision; decisionMade is never called by the AI. */
    protected static AwaitingDecision decision(int id, AwaitingDecisionType type, String text,
                                               Map<String, String[]> params) {
        return new AwaitingDecision() {
            @Override public int getAwaitingDecisionId() { return id; }
            @Override public String getText() { return text; }
            @Override public AwaitingDecisionType getDecisionType() { return type; }
            @Override public Map<String, String[]> getDecisionParameters() { return params; }
            @Override public void decisionMade(String result) {
                throw new AssertionError("the trace must never call decisionMade");
            }
        };
    }

    protected static Map<String, String[]> params(Object... keyThenArray) {
        Map<String, String[]> map = new LinkedHashMap<>();
        for (int i = 0; i < keyThenArray.length; i += 2) {
            map.put((String) keyThenArray[i], (String[]) keyThenArray[i + 1]);
        }
        return map;
    }

    protected static String[] arr(String... values) {
        return values;
    }

    protected static List<String> list(String... values) {
        return Arrays.asList(values);
    }

    protected static GameState stub(Phase phase) {
        return new StubGameState(phase, DECIDING_PLAYER);
    }

    /** Stub whose current turn player is an explicit, concrete id — used by the allowance
     *  fixture to encode a recipient (the decider) that DIFFERS from the current turn
     *  player, exactly as the real ACTIVATE_ALLOWANCE decision is answered by the
     *  non-turn player. */
    protected static GameState stubWithTurnPlayer(Phase phase, String turnPlayerId) {
        return new StubGameState(phase, turnPlayerId);
    }

    protected static GameState stubWithReserve(Phase phase, int reserveDeckSize) {
        return new StubGameState(phase, DECIDING_PLAYER, reserveDeckSize);
    }

    /** Minimal real-GameState subclass (mirrors the trace-hook StubGameState) overriding
     *  only the getters the decide() path reads, with the phase supplied by the fixture.
     *  Fixed facts: side DARK, turn 1, empty hand/permanent/used piles, Force pile 4,
     *  Reserve Deck 20, life force 40, opponent id "opponent". getCurrentPlayerId returns
     *  the fixture's nonblank turn player. Another override is
     *  added ONLY when a captured failure names the exact missing read. */
    protected static class StubGameState extends GameState {
        private final Phase phase;
        private final String currentPlayerId;
        private final int reserveDeckSize;

        protected StubGameState(Phase phase, String currentPlayerId) {
            this(phase, currentPlayerId, 20);
        }

        protected StubGameState(Phase phase, String currentPlayerId, int reserveDeckSize) {
            this.phase = phase;
            this.currentPlayerId = currentPlayerId;
            this.reserveDeckSize = reserveDeckSize;
        }

        @Override public String getOpponent(String playerId) { return "opponent"; }
        @Override public Side getSide(String playerId) { return Side.DARK; }
        @Override public int getPlayersLatestTurnNumber(String playerId) { return 1; }
        @Override public Phase getCurrentPhase() { return phase; }
        @Override public String getCurrentPlayerId() { return currentPlayerId; }
        @Override public List<PhysicalCard> getHand(String playerId) { return List.of(); }
        @Override public List<PhysicalCard> getAllPermanentCards() { return List.of(); }
        @Override public int getForcePileSize(String playerId) { return 4; }
        @Override public int getReserveDeckSize(String playerId) { return reserveDeckSize; }
        @Override public int getPlayerLifeForce(String playerId) { return 40; }
        @Override public List<PhysicalCard> getUsedPile(String playerId) { return List.of(); }
        @Override public PhysicalCard getBattleLocation() { return null; }
    }

    // =========================================================================
    // Discovery dumper (DUMP only) — the re-freeze tool. Emits copy-pasteable
    // ExpectedOp records + every finalization/candidate field to stdout so the
    // frozen expectations above can be regenerated after a deliberate source change.
    // =========================================================================

    private static void dump(String name, CapturedDecision cap) {
        DecisionTrace t = cap.trace();
        StringBuilder sb = new StringBuilder();
        sb.append("\n===== DUMP ").append(name).append(" =====\n");
        sb.append("response=").append(qq(cap.response())).append('\n');
        sb.append("status=").append(t.getStatus())
          .append(" botModel=").append(t.getBotModel()).append('\n');
        sb.append("route=").append(t.getRoute() != null ? t.getRoute().selected() : null).append('\n');
        sb.append("rawCandidateOrder=").append(t.getRawCandidateOrder()).append('\n');
        sb.append("mergeOrder=").append(t.getMergeOrder()).append('\n');
        var f = t.getFinalization();
        sb.append("preSafetyWinnerActionId=").append(qq(f.preSafetyWinnerActionId()))
          .append(" recorded=").append(f.preSafetyWinnerRecorded())
          .append(" naReason=").append(qq(f.preSafetyWinnerNotApplicableReason())).append('\n');
        sb.append("passEligible=").append(f.passEligible())
          .append(" passNaReason=").append(qq(f.passEligibilityNotApplicableReason())).append('\n');
        sb.append("finalResponse=").append(qq(f.finalResponse()))
          .append(" recorded=").append(f.finalResponseRecorded())
          .append(" skippedCommonFinalizer=").append(f.skippedCommonFinalizer())
          .append(" corrections=").append(f.corrections()).append('\n');
        if (t.getSnapshot() != null) {
            var flags = t.getSnapshot().decisionFacts().obligationFlags();
            sb.append("obligationFlags known=").append(flags.isKnown())
              .append(" value=").append(flags.isKnown() ? flags.value() : "n/a").append('\n');
        }
        sb.append("operations (").append(t.getOperations().size()).append("):\n");
        for (TraceOperation op : t.getOperations()) {
            sb.append("  new ExpectedOp(TraceOp.").append(op.getOp())
              .append(", ").append(op.getCandidateOrdinal())
              .append(", ").append(qq(op.getSyntheticSource()))
              .append(", ").append(qq(op.getActionId()))
              .append(", ").append(qq(op.getEvaluatorId()))
              .append(", ").append(ruleExpr(op.getRuleId()))
              .append(", TraceDomainId.").append(op.getDomainId())
              .append(", TraceOutputKind.").append(op.getOutputKind())
              .append(", ").append(op.getBeforeBits())
              .append(", ").append(op.getDeltaBits())
              .append(", ").append(op.getAfterBits())
              .append(", ").append(op.isVetoed())
              .append(", ").append(qq(op.getVetoReason()))
              .append(", ").append(qq(op.getDetail()))
              .append("),\n");
        }
        sb.append("===== END ").append(name).append(" =====");
        System.out.println(sb);
    }

    private static String qq(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String ruleExpr(TraceRuleId id) {
        if (id == null) return "null";
        if (TraceRuleId.LEGACY_UNTAGGED.equals(id)) return "TraceRuleId.LEGACY_UNTAGGED";
        if (TraceRuleId.COMBINED_EVALUATOR.equals(id)) return "TraceRuleId.COMBINED_EVALUATOR";
        return "TraceRuleId.of(\"" + id.id() + "\")";
    }
}
