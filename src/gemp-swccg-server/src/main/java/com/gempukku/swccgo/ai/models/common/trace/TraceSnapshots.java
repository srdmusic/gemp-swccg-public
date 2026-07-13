package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.ActionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "One immutable envelope" field 3 + "Frozen input and candidate order"): SHADOW builder
 * of the shared {@link DecisionSnapshot} from the complete raw decision input, at the
 * trace boundary.
 *
 * ONE shared implementation for both bots (mirror-drift-proof, like FormationSafety):
 * the per-bot call sites only pass raw values. Rules:
 *  - Candidate rows are built one per ORIGINAL ordinal, never sorted or rebuilt.
 *  - Values the builder did not observe are UNKNOWN FactValues with producer,
 *    provenance, and reason — never fabricated defaults (facts-contract law). Full
 *    resolution (refs, measurements, objective identity, service facts) is the
 *    facts-builder increment; this shadow builder records raw input verbatim.
 *  - Parallel raw arrays of different lengths are RETAINED (short arrays yield absent
 *    fields, no padding with false/zero/empty) and reported as issues so the collector
 *    marks the trace INCOMPLETE.
 *  - Pure-read discipline: callers may pass only already-parsed params and pure
 *    GameState getter observations. No evaluator, strategy service, or
 *    getForceReserveFacts() call is permitted on this path.
 */
public final class TraceSnapshots {

    private TraceSnapshots() {
        // static access only
    }

    /** Snapshot plus the ordered construction issues (issues => collector marks INCOMPLETE). */
    public record Result(DecisionSnapshot snapshot, List<String> issues) {
        public Result {
            issues = List.copyOf(issues);
        }
    }

    /** Raw-input carrier so the two call sites stay readable. Mutable staging only —
     *  everything is copied/frozen inside build(). */
    public static final class Input {
        public String producerId = "trace-shadow-builder";
        public String decisionId;
        public String decisionTypeName;
        public String decisionText;
        public Phase phase;
        public int turn;
        public String currentPlayer;
        public Side side;
        public Boolean noPassParam;           // null = param absent in the raw decision
        public Integer minParam;              // null = param absent
        public Integer maxParam;              // null = param absent
        public Set<String> blockedResponses;  // null = none
        public List<String> actionIds;
        public List<String> actionTexts;
        public List<String> cardIds;
        public List<String> blueprintIds;
        public List<String> testingTexts;
        public List<Boolean> selectable;
        public List<String> multipleChoiceResults;
        public Integer forcePileSize;         // null = not observed
        public Integer lifeForceCardCount;    // null = not observed
        public Integer handSize;              // null = not observed
        public Integer reserveDeckSize;       // null = not observed
    }

    /**
     * Build the shadow snapshot. Never throws: any construction failure is returned as
     * a null snapshot plus an issue entry (the collector converts issues to typed
     * SNAPSHOT capture failures and the trace becomes INCOMPLETE).
     */
    public static Result build(Input in) {
        List<String> issues = new ArrayList<>();
        try {
            return new Result(buildOrThrow(in, issues), issues);
        } catch (Throwable t) {
            issues.add("snapshot construction failed: " + t.getClass().getName()
                + ": " + t.getMessage());
            return new Result(null, issues);
        }
    }

    private static DecisionSnapshot buildOrThrow(Input in, List<String> issues) {
        AwaitingDecisionType type = AwaitingDecisionType.valueOf(in.decisionTypeName);
        String provenanceBoundary = "not observed at trace boundary (oracle-v2 increment 2)";

        // ── group 2 facts: response constraints, never fabricated ──
        FactValue<Boolean> noPass = in.noPassParam != null
            ? FactValue.known(in.noPassParam, in.producerId, "decision.params[noPass]")
            : FactValue.unknown(in.producerId, "decision.params[noPass]", "noPass param absent from raw decision");
        FactValue<Integer> minimum = in.minParam != null
            ? FactValue.known(in.minParam, in.producerId, "decision.params[min]")
            : FactValue.unknown(in.producerId, "decision.params[min]", "min param absent from raw decision");
        FactValue<Integer> maximum = in.maxParam != null
            ? FactValue.known(in.maxParam, in.producerId, "decision.params[max]")
            : FactValue.unknown(in.producerId, "decision.params[max]", "max param absent from raw decision");

        // ── obligation flags derived from the noPass/minimum facts (DecisionFacts doc) ──
        FactValue<Set<DecisionFacts.ObligationFlag>> obligations;
        if (noPass.isKnown() && minimum.isKnown()) {
            Set<DecisionFacts.ObligationFlag> flags = EnumSet.noneOf(DecisionFacts.ObligationFlag.class);
            if (noPass.value()) flags.add(DecisionFacts.ObligationFlag.NO_PASS);
            if (minimum.value() > 0) flags.add(DecisionFacts.ObligationFlag.MANDATORY_SELECTION);
            obligations = FactValue.known(Set.copyOf(flags), in.producerId, "decision.params[noPass,min]");
        } else {
            obligations = FactValue.unknown(in.producerId, "decision.params[noPass,min]",
                "derivation input absent: " + (noPass.isUnknown() ? "noPass " : "")
                    + (minimum.isUnknown() ? "min" : "").trim());
        }

        // ── candidate rows, one per ORIGINAL ordinal; mismatched lengths retained ──
        int rows = maxSize(in.actionIds, in.actionTexts, in.cardIds, in.blueprintIds,
            in.testingTexts, in.selectable, in.multipleChoiceResults);
        checkParallelLengths(in, issues);
        List<ActionFacts> actionFacts = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            String actionId = blankToAbsent(entry(in.actionIds, i));
            String actionText = entry(in.actionTexts, i);
            if (actionText == null && in.multipleChoiceResults != null) {
                actionText = entry(in.multipleChoiceResults, i);
            }
            String cardId = blankToAbsent(entry(in.cardIds, i));
            String blueprintId = blankToAbsent(entry(in.blueprintIds, i));
            String testingText = entry(in.testingTexts, i);
            Boolean selectableEntry = entry(in.selectable, i);
            FactValue<Boolean> selectable = selectableEntry != null
                ? FactValue.known(selectableEntry, in.producerId, "decision.params[selectable][" + i + "]")
                : FactValue.unknown(in.producerId, "decision.params[selectable][" + i + "]",
                    "selectable array absent or shorter than candidate row");
            FactValue<ActionFacts.ActionRef> resolvedAction = actionId != null
                ? FactValue.known(new ActionFacts.ActionRef(actionId), in.producerId,
                    "decision.params[actionId][" + i + "]")
                : FactValue.unknown(in.producerId, "decision.params[actionId][" + i + "]",
                    "no actionId at this ordinal");
            actionFacts.add(ActionFacts.builder()
                .ordinal(i)
                .actionId(actionId)
                .actionText(actionText)
                .cardId(cardId)
                .blueprintId(blueprintId)
                .testingText(testingText)
                .selectable(selectable)
                .resolvedAction(resolvedAction)
                .resolvedCard(unknown(in, "card resolution", provenanceBoundary))
                .resolvedSource(unknown(in, "source resolution", provenanceBoundary))
                .resolvedDestination(unknown(in, "destination resolution", provenanceBoundary))
                .cost(unknown(in, "cost", provenanceBoundary))
                .basePower(unknown(in, "basePower", provenanceBoundary))
                .ability(unknown(in, "ability", provenanceBoundary))
                .friendlyNonUndercoverCharacterCount(unknown(in, "friendly character count", provenanceBoundary))
                .opposingNonUndercoverCharacterCount(unknown(in, "opposing character count", provenanceBoundary))
                .icons(unknown(in, "icons", provenanceBoundary))
                .weaponBonus(unknown(in, "weaponBonus", provenanceBoundary))
                .build());
        }

        // ── route selection: one route per engine decision shape (increment-1 taxonomy) ──
        DecisionFacts.DecisionRoute route = DecisionFacts.DecisionRoute.valueOf(type.name());
        DecisionFacts.RouteSelectionEvidence evidence = new DecisionFacts.RouteSelectionEvidence(
            type, in.phase, null, obligations,
            new DecisionFacts.CandidateShape(size(in.actionIds), size(in.cardIds)));

        DecisionFacts decisionFacts = DecisionFacts.builder()
            .decisionId(in.decisionId)
            .decisionType(type)
            .decisionText(in.decisionText != null ? in.decisionText : "")
            .phase(in.phase)
            .window(null)
            .turn(Math.max(in.turn, 0))
            .currentPlayer(in.currentPlayer)
            .side(in.side)
            .obligationFlags(obligations)
            .noPass(noPass)
            .minimum(minimum)
            .maximum(maximum)
            .blockedResponses(in.blockedResponses != null ? in.blockedResponses : Set.of())
            .forcePileSize(observed(in, in.forcePileSize, "GameState.getForcePileSize"))
            .lifeForceCardCount(observed(in, in.lifeForceCardCount, "GameState.getPlayerLifeForce"))
            .handSize(observed(in, in.handSize, "GameState.getHand().size"))
            .reserveDeckSize(observed(in, in.reserveDeckSize, "GameState.getReserveDeckSize"))
            .objectiveIdentity(FactValue.unknown(in.producerId, "ObjectiveAnalyzer",
                "objective analyzer not consulted at trace boundary (pure-read discipline)"))
            .objectiveFlipped(FactValue.unknown(in.producerId, "ObjectiveAnalyzer",
                "objective analyzer not consulted at trace boundary (pure-read discipline)"))
            .selectedRoute(route)
            .routeSelectionEvidence(evidence)
            .build();

        DecisionSnapshot.ServiceFacts serviceFacts = new DecisionSnapshot.ServiceFacts(
            FactValue.unknown(in.producerId, "ForceReserveService",
                "ForceReserveService not consulted at trace boundary (cache-mutation hazard)"));

        return new DecisionSnapshot(decisionFacts, actionFacts, serviceFacts,
            DecisionSnapshot.CURRENT_VERSION);
    }

    /**
     * The raw candidate id list whose ordinals bind every trace operation: the id array
     * the engine actually accepts as the response for this decision shape.
     * MULTIPLE_CHOICE answers by index, so its raw ids are "0".."n-1" over the results
     * array — matching the wire response encoding, not a fabricated id.
     */
    public static List<String> rawCandidateIds(String decisionTypeName,
                                               List<String> actionIds,
                                               List<String> cardIds,
                                               List<String> multipleChoiceResults) {
        List<String> raw = new ArrayList<>();
        if (decisionTypeName == null) {
            return raw;
        }
        switch (decisionTypeName) {
            case "CARD_ACTION_CHOICE":
            case "ACTION_CHOICE":
                if (actionIds != null) raw.addAll(actionIds);
                break;
            case "CARD_SELECTION":
            case "ARBITRARY_CARDS":
                if (cardIds != null) raw.addAll(cardIds);
                break;
            case "MULTIPLE_CHOICE":
                int n = size(multipleChoiceResults);
                for (int i = 0; i < n; i++) raw.add(String.valueOf(i));
                break;
            default:
                // INTEGER / EMPTY: no candidate id array exists
                break;
        }
        return raw;
    }

    // ── helpers ──

    private static <T> FactValue<T> unknown(Input in, String what, String reason) {
        return FactValue.unknown(in.producerId, what, reason);
    }

    private static FactValue<Integer> observed(Input in, Integer value, String provenance) {
        return value != null
            ? FactValue.known(value, in.producerId, provenance)
            : FactValue.unknown(in.producerId, provenance, "not observed at trace boundary");
    }

    private static <T> T entry(List<T> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    /** Raw parallel arrays encode "no value for this ordinal" as an empty string
     *  (e.g. a CARD_ACTION_CHOICE action with no associated card). ActionFacts models
     *  absence as null, so blank maps to absent — routine, not a capture failure. */
    private static String blankToAbsent(String s) {
        return (s != null && s.isBlank()) ? null : s;
    }

    private static int size(List<?> list) {
        return list != null ? list.size() : 0;
    }

    private static int maxSize(List<?>... lists) {
        int max = 0;
        for (List<?> l : lists) {
            max = Math.max(max, size(l));
        }
        return max;
    }

    /** Present parallel arrays must agree in length; a mismatch is retained AND reported. */
    private static void checkParallelLengths(Input in, List<String> issues) {
        Set<Integer> lengths = new LinkedHashSet<>();
        StringBuilder present = new StringBuilder();
        recordLength(lengths, present, "actionIds", in.actionIds);
        recordLength(lengths, present, "actionTexts", in.actionTexts);
        recordLength(lengths, present, "cardIds", in.cardIds);
        recordLength(lengths, present, "blueprintIds", in.blueprintIds);
        recordLength(lengths, present, "testingTexts", in.testingTexts);
        recordLength(lengths, present, "selectable", in.selectable);
        recordLength(lengths, present, "results", in.multipleChoiceResults);
        if (lengths.size() > 1) {
            issues.add("parallel raw candidate arrays differ in length: " + present
                + "— mismatch retained without padding");
        }
    }

    private static void recordLength(Set<Integer> lengths, StringBuilder present,
                                     String name, List<?> list) {
        if (list != null && !list.isEmpty()) {
            lengths.add(list.size());
            present.append(name).append('=').append(list.size()).append(' ');
        }
    }
}
