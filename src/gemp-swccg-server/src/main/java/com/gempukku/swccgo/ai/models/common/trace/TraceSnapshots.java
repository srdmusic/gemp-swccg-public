package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.ActionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.objective.ObjectiveFacts;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        /** TRACE-V2 GATE P0-1 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): the COMPLETE
         *  verbatim engine parameter map (AwaitingDecision.getDecisionParameters()), set at
         *  the bot decide() boundary. Presence = key exists; present-empty arrays and blank
         *  entries are preserved verbatim. null = not available on this path (the pure
         *  CombinedEvaluator seam) — build() then stages a CONTEXT_EFFECTIVE raw record
         *  from the already-parsed fields above, honestly source-marked. */
        public Map<String, String[]> rawParameters;
        /** The one boundary-produced objective view. null is explicit UNKNOWN for
         *  unmediated legacy test seams only. */
        public ObjectiveFacts objectiveFacts;
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

        // ── route selection: one route per engine decision shape (increment-1 taxonomy).
        // B2 type-hardening gate item 2 (CODEX_B2_TYPE_HARDENING_GATE_FA0F254AC_2026-07-13.md):
        // the evidence record now carries the selected route itself, and DecisionFacts
        // rejects evidence whose route disagrees with selectedRoute. ──
        DecisionFacts.DecisionRoute route = DecisionFacts.DecisionRoute.valueOf(type.name());
        DecisionFacts.RouteSelectionEvidence evidence = new DecisionFacts.RouteSelectionEvidence(
            type, in.phase, null, obligations,
            new DecisionFacts.CandidateShape(size(in.actionIds), size(in.cardIds)), route);

        ObjectiveFacts objectiveFacts = in.objectiveFacts != null
            ? in.objectiveFacts
            : ObjectiveFacts.unknown("objective producer was not available on this unmediated seam");
        FactValue<String> objectiveIdentity = objectiveFacts.identity().isKnown()
            ? FactValue.known(
                objectiveFacts.identity().value().canonicalFrontBlueprintId(),
                ObjectiveFacts.PRODUCER,
                "ObjectiveFacts.Identity.canonicalFrontBlueprintId")
            : FactValue.unknown(
                ObjectiveFacts.PRODUCER,
                "ObjectiveFacts.Identity.canonicalFrontBlueprintId",
                objectiveFacts.identity().unknownReason());
        FactValue<Boolean> objectiveFlipped = objectiveFacts.identity().isKnown()
            ? FactValue.known(
                objectiveFacts.identity().value().flipped(),
                ObjectiveFacts.PRODUCER,
                "ObjectiveFacts.Identity.flipped")
            : FactValue.unknown(
                ObjectiveFacts.PRODUCER,
                "ObjectiveFacts.Identity.flipped",
                objectiveFacts.identity().unknownReason());

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
            .objectiveIdentity(objectiveIdentity)
            .objectiveFlipped(objectiveFlipped)
            .selectedRoute(route)
            .routeSelectionEvidence(evidence)
            .build();

        DecisionSnapshot.ServiceFacts serviceFacts = new DecisionSnapshot.ServiceFacts(
            FactValue.unknown(in.producerId, "ForceReserveService",
                "ForceReserveService not consulted at trace boundary (cache-mutation hazard)"));

        return new DecisionSnapshot(decisionFacts, actionFacts, serviceFacts, objectiveFacts,
            buildRawDecision(in), DecisionSnapshot.CURRENT_VERSION);
    }

    /**
     * TRACE-V2 GATE P0-1 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md "add one immutable
     * raw-decision component to DecisionSnapshot"): the verbatim raw evidence the
     * normalized ActionFacts rows are built FROM and never replace.
     *
     * Bot boundary (in.rawParameters set): every engine parameter key preserved
     * separately and verbatim — typed scalar presence (noPass/min/max/defaultIndex/
     * defaultValue/timeoutValue/...), every array (preselected, autoPassEligible,
     * backSideTestingText, horizontal, cardText, returnAnyChange, yourTurn, noLongDelay,
     * revertEligible, results, ...) including present-EMPTY versus absent, blank entries
     * blank. Seam (rawParameters null): the already-parsed effective values are re-staged
     * under Source.CONTEXT_EFFECTIVE — declared derivation, never fake raw evidence.
     */
    private static DecisionSnapshot.RawDecision buildRawDecision(Input in) {
        if (in.rawParameters != null) {
            LinkedHashMap<String, List<String>> verbatim = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : in.rawParameters.entrySet()) {
                String[] values = entry.getValue();
                // null-tolerant verbatim copy; a null value array is retained as
                // present-empty (the key WAS present on the raw decision)
                verbatim.put(entry.getKey(),
                    values != null ? new ArrayList<>(Arrays.asList(values)) : new ArrayList<>());
            }
            return new DecisionSnapshot.RawDecision(
                DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, verbatim);
        }
        LinkedHashMap<String, List<String>> effective = new LinkedHashMap<>();
        if (in.noPassParam != null) effective.put("noPass", List.of(String.valueOf(in.noPassParam)));
        if (in.minParam != null) effective.put("min", List.of(String.valueOf(in.minParam)));
        if (in.maxParam != null) effective.put("max", List.of(String.valueOf(in.maxParam)));
        putEffectiveList(effective, "actionId", in.actionIds);
        putEffectiveList(effective, "actionText", in.actionTexts);
        putEffectiveList(effective, "cardId", in.cardIds);
        putEffectiveList(effective, "blueprintId", in.blueprintIds);
        putEffectiveList(effective, "testingText", in.testingTexts);
        if (in.selectable != null) {
            List<String> sel = new ArrayList<>(in.selectable.size());
            for (Boolean b : in.selectable) {
                sel.add(b != null ? String.valueOf(b) : null);
            }
            effective.put("selectable", sel);
        }
        putEffectiveList(effective, "results", in.multipleChoiceResults);
        return new DecisionSnapshot.RawDecision(
            DecisionSnapshot.RawDecision.Source.CONTEXT_EFFECTIVE, effective);
    }

    private static void putEffectiveList(Map<String, List<String>> target, String key,
                                         List<String> values) {
        if (values != null) {
            target.put(key, new ArrayList<>(values));
        }
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
     *  absence as null, so blank maps to absent — routine, not a capture failure.
     *  TRACE-V2 GATE P0-1: the offered blank value itself is NOT lost — it stays
     *  verbatim in DecisionSnapshot.rawDecision() (and in rawCandidateOrder). */
    private static String blankToAbsent(String s) {
        return (s != null && s.isBlank()) ? null : s;
    }

    /**
     * TRACE-V2 GATE P0-1 seam helper: DecisionContext initializes its candidate arrays
     * to EMPTY lists, so it cannot represent PRESENT-EMPTY versus ABSENT. Seam callers
     * therefore stage an empty context list as ABSENT; the raw present-empty distinction
     * is owned by the bot boundary's rawParameters capture. Shared here so both bots'
     * openSeamSession stay mirror-identical.
     */
    public static <T> List<T> contextListOrAbsent(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
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

    /** Present parallel arrays must agree in length; a mismatch is retained AND reported.
     *  TRACE-V2 GATE P0-1 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): a PRESENT-EMPTY
     *  array counts as present with length 0 — paired with a present non-empty parallel
     *  array it IS a mismatch and the trace becomes INCOMPLETE. Only ABSENT (null)
     *  arrays impose no constraint. */
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
        if (list != null) {
            lengths.add(list.size());
            present.append(name).append('=').append(list.size()).append(' ');
        }
    }
}
