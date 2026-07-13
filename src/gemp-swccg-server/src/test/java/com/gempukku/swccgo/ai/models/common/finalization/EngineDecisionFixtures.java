package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.logic.decisions.ActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.ArbitraryCardsSelectionDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.CardsSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.timing.Action;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * F0 ENGINE-TRUTH FIXTURE HOME (2026-07-13).
 *
 * Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F0 —
 * "EngineDecisionFixtures owns fresh-decision suppliers, recording subclasses, and
 * the minimum Action and PhysicalCard helpers. Every assertion must use a fresh
 * decision instance."
 *
 * The decision instances are the REAL engine classes (concrete anonymous/recording
 * subclasses over the real abstract decision classes), so {@code decisionMade} runs
 * the REAL engine validators — the audit's law: "The engine decision classes, not
 * prompt text, define valid wire responses"
 * (Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md). No VirtualTableScenario:
 * these seven contracts are pure response-boundary checks (packet F0 rule).
 *
 * Action/PhysicalCard stand-ins are reflective java.lang.reflect.Proxy stubs with
 * IDENTITY equality, not Mockito: the pinned mockito-core 4.7.0 (byte-buddy 1.12.x)
 * cannot instrument Java 21 class files (major 65), so Mockito mocks fail to
 * initialize under this build's JDK. The proxies satisfy exactly the calls the
 * decision constructors/validators make (getCardId, getBlueprint,
 * getActionAttachedToCard, ...) and nothing else — same "minimum helper" intent.
 * Identity equality is REQUIRED: ArbitraryCardsSelectionDecision validates responses
 * with contains() over the same instances it was constructed with.
 */
final class EngineDecisionFixtures {

    private EngineDecisionFixtures() {
    }

    // ═══ Minimum reflective Action / PhysicalCard helpers ═══

    private static Object stub(Class<?> iface, Map<String, Object> overrides) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "stub-" + iface.getSimpleName();
                default:
                    // fall through to overrides/defaults
            }
            if (overrides.containsKey(method.getName())) {
                return overrides.get(method.getName());
            }
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return false;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == float.class) return 0f;
            if (rt == double.class) return 0d;
            return null;
        };
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    /** A PhysicalCard whose getCardId() answers {@code cardId}; blueprint calls are
     *  satisfied so decision constructors can render their param arrays. */
    static PhysicalCard card(int cardId) {
        Object blueprint = stub(SwccgCardBlueprint.class, Map.of());
        return (PhysicalCard) stub(PhysicalCard.class, Map.of(
                "getCardId", cardId,
                "getBlueprintId", "1_1",
                "getBlueprint", blueprint,
                "getTestingText", "Stub Card " + cardId));
    }

    /** An Action attached to a stub card (CardActionSelectionDecision requires a
     *  non-null attached card to build its cardId param array). */
    static Action action(int attachedCardId) {
        return (Action) stub(Action.class, Map.of(
                "getActionAttachedToCard", card(attachedCardId),
                "getText", "Stub action on card " + attachedCardId));
    }

    static List<Action> actions(int count) {
        List<Action> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(action(100 + i));
        }
        return result;
    }

    static List<PhysicalCard> cards(int... cardIds) {
        List<PhysicalCard> result = new ArrayList<>();
        for (int id : cardIds) {
            result.add(card(id));
        }
        return result;
    }

    // ═══ Recording subclasses over the REAL engine decision classes ═══

    /** ACTION_CHOICE running the REAL validator (ActionSelectionDecision.java:130-143). */
    static class RecordingActionChoice extends ActionSelectionDecision {
        Action selected;
        int callbackCount;

        RecordingActionChoice(String text, List<Action> actions) {
            super(text, actions);
        }

        @Override
        public void decisionMade(String result) throws DecisionResultInvalidException {
            selected = getSelectedAction(result);
            callbackCount++;
        }
    }

    /** CARD_ACTION_CHOICE running the REAL validator (CardActionSelectionDecision.java:167-180).
     *  {@code selected == null} with {@code decided} set is the engine-accepted
     *  "no selected action" outcome. */
    static class RecordingCardActionChoice extends CardActionSelectionDecision {
        Action selected;
        boolean decided;
        int callbackCount;

        RecordingCardActionChoice(String text, List<Action> actions, boolean noPass) {
            super(1, text, actions, true, false, noPass, false, false);
        }

        @Override
        public void decisionMade(String result) throws DecisionResultInvalidException {
            selected = getSelectedAction(result);
            decided = true;
            callbackCount++;
        }
    }

    /** CARD_SELECTION running the REAL validator (CardsSelectionDecision.java:61-85). */
    static class RecordingCardSelection extends CardsSelectionDecision {
        List<PhysicalCard> selected;
        int callbackCount;

        RecordingCardSelection(String text, List<PhysicalCard> cards, int min, int max) {
            super(text, cards, min, max);
        }

        @Override
        public void decisionMade(String result) throws DecisionResultInvalidException {
            selected = getSelectedCardsByResponse(result);
            callbackCount++;
        }
    }

    /** ARBITRARY_CARDS running the REAL validator
     *  (ArbitraryCardsSelectionDecision.java:241-266), full-shape constructor with
     *  preselected/selectable/returnAnyChange. */
    static class RecordingArbitraryCards extends ArbitraryCardsSelectionDecision {
        List<PhysicalCard> selected;
        int callbackCount;

        RecordingArbitraryCards(String text, List<PhysicalCard> shown, List<PhysicalCard> preselected,
                                List<PhysicalCard> selectable, int min, int max, boolean returnAnyChange) {
            super(text, shown, preselected, selectable, min, max, returnAnyChange, null);
        }

        @Override
        public void decisionMade(String result) throws DecisionResultInvalidException {
            selected = getSelectedCardsByResponse(result);
            callbackCount++;
        }
    }

    /** MULTIPLE_CHOICE running the REAL validator (MultipleChoiceAwaitingDecision.java:59-71). */
    static class RecordingMultipleChoice extends MultipleChoiceAwaitingDecision {
        int chosenIndex = -1;
        String chosenResult;
        int callbackCount;

        RecordingMultipleChoice(String text, String[] possibleResults) {
            super(text, possibleResults);
        }

        @Override
        protected void validDecisionMade(int index, String result) {
            chosenIndex = index;
            chosenResult = result;
            callbackCount++;
        }
    }

    /** INTEGER running the REAL validator (IntegerAwaitingDecision.java:35-51). */
    static class RecordingInteger extends IntegerAwaitingDecision {
        Integer chosen;
        int callbackCount;

        RecordingInteger(String text, Integer min, Integer max, Integer defaultValue) {
            super(text, min, max, defaultValue);
        }

        @Override
        public void decisionMade(int result) {
            chosen = result;
            callbackCount++;
        }
    }

    // ═══ Fresh-decision suppliers (every assertion gets a FRESH instance) ═══

    static RecordingActionChoice freshActionChoice() {
        return new RecordingActionChoice("Optional responses", actions(3));
    }

    static RecordingCardActionChoice freshCardActionChoiceNoPass() {
        return new RecordingCardActionChoice("Optional responses", actions(2), /* noPass= */ true);
    }

    /** Raw noPass=true with a prompt that offers NO Done/Cancel/optional wording —
     *  the raw-noPass-versus-V148 divergence probe: the engine still accepts ""
     *  (the preserved contradiction), but every legacy must-choose owner reads raw
     *  noPass and forces a pick. */
    static RecordingCardActionChoice freshCardActionChoiceNoPassNoCancelText() {
        return new RecordingCardActionChoice("Choose action to perform", actions(2), /* noPass= */ true);
    }

    static RecordingCardSelection freshCardSelectionMin0() {
        return new RecordingCardSelection("Choose cards to target, or click Done to cancel",
                cards(101, 102), 0, 2);
    }

    static RecordingCardSelection freshCardSelectionMin2() {
        return new RecordingCardSelection("Choose 2 cards to lose", cards(101, 102, 103), 2, 2);
    }

    static RecordingCardSelection freshCardSelectionMax1() {
        return new RecordingCardSelection("Choose card to target", cards(101, 102, 103), 0, 1);
    }

    /** min0/noPass-free CARD_SELECTION whose prompt offers NO Done/Cancel text —
     *  the pass-policy probe shape. */
    static RecordingCardSelection freshCardSelectionMin0NoCancelText() {
        return new RecordingCardSelection("Choose starship", cards(101, 102), 0, 1);
    }

    /**
     * Locked-preselection iterative combination shape (the AMN family): shown
     * temp0..temp2; temp0 is PRESELECTED (locked, NOT selectable); temp1 is the
     * only selectable id; min=2 with returnAnyChange=true.
     */
    static RecordingArbitraryCards freshArbitraryLockedPreselection() {
        List<PhysicalCard> shown = cards(201, 202, 203);
        return new RecordingArbitraryCards("Choose cards for combination", shown,
                /* preselected= */ shown.subList(0, 1),
                /* selectable= */ shown.subList(1, 2),
                /* min= */ 2, /* max= */ 3, /* returnAnyChange= */ true);
    }

    static RecordingMultipleChoice freshMultipleChoice() {
        return new RecordingMultipleChoice("Barrier this starship?", new String[]{"Yes", "No"});
    }

    static RecordingInteger freshInteger1to5Default3() {
        return new RecordingInteger("Choose amount of Force to activate", 1, 5, 3);
    }

    // ═══ Engine decision -> committed DecisionSnapshot bridge (F3 corpus reuse) ═══

    /**
     * Stage the ACTUAL engine decision parameters through the shared shadow builder
     * ({@link TraceSnapshots#build}) — the same path the bot boundary uses — so
     * fixture snapshots cannot drift from the committed production snapshot shape.
     * The verbatim parameter map lands in {@link DecisionSnapshot.RawDecision} as
     * ENGINE_PARAMETERS evidence.
     */
    static DecisionSnapshot snapshotOf(AwaitingDecision decision) {
        Map<String, String[]> p = decision.getDecisionParameters();
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.producerId = "finalizer-fixture";
        in.decisionId = String.valueOf(decision.getAwaitingDecisionId());
        in.decisionTypeName = decision.getDecisionType().name();
        in.decisionText = decision.getText();
        in.phase = null;
        in.turn = 1;
        in.currentPlayer = "asdf";
        in.side = Side.DARK;
        in.noPassParam = boolParam(p.get("noPass"));
        in.minParam = intParam(p.get("min"));
        in.maxParam = intParam(p.get("max"));
        in.actionIds = listParam(p.get("actionId"));
        in.actionTexts = listParam(p.get("actionText"));
        in.cardIds = listParam(p.get("cardId"));
        in.blueprintIds = listParam(p.get("blueprintId"));
        in.testingTexts = listParam(p.get("testingText"));
        in.selectable = boolListParam(p.get("selectable"));
        in.multipleChoiceResults = listParam(p.get("results"));
        in.rawParameters = p; // verbatim ENGINE_PARAMETERS capture
        TraceSnapshots.Result result = TraceSnapshots.build(in);
        assertNotNull("shared shadow builder must produce a snapshot for a real engine decision"
                + (result.issues().isEmpty() ? "" : " — issues: " + result.issues()), result.snapshot());
        return result.snapshot();
    }

    private static Boolean boolParam(String[] values) {
        return (values != null && values.length > 0) ? Boolean.parseBoolean(values[0]) : null;
    }

    private static Integer intParam(String[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        try {
            return Integer.parseInt(values[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> listParam(String[] values) {
        return values != null ? new ArrayList<>(Arrays.asList(values)) : null;
    }

    private static List<Boolean> boolListParam(String[] values) {
        if (values == null) {
            return null;
        }
        List<Boolean> result = new ArrayList<>(values.length);
        for (String v : values) {
            result.add(v != null ? Boolean.parseBoolean(v) : null);
        }
        return result;
    }
}
