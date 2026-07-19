package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PassPolicyTest {

    @Test
    public void battleAndFollowthroughDecisionsAreTerminal() {
        PolicyResult battle = evaluate(facts(2, Phase.BATTLE,
                false, false, false, true, true, false,
                true, 0, 0, 0, true, 4));
        assertIds(battle, "PASS-early-game", "PASS-battle-action");
        assertDeltas(battle, -3.0f, -10.0f);

        PolicyResult followthrough = evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, true,
                true, 0, 0, 0, true, 4));
        assertIds(followthrough, "PASS-follow-through");
        assertDeltas(followthrough, -15.0f);
    }

    @Test
    public void earlyResourceStackPreservesOrderAndRawMagnitudes() {
        PolicyResult result = evaluate(facts(2, Phase.MOVE,
                false, false, false, false, false, false,
                true, 0, 9, 4, true, 2));

        assertIds(result, "PASS-early-game", "PASS-low-force",
                "PASS-low-reserve", "PASS-small-hand", "PASS-move-draw",
                "V27.1-pass-DTF", "V27-maintenance-pass");
        assertDeltas(result, -3.0f, 1.0f, 1.5f, 4.0f, 5.0f, 60.0f, 50.0f);
        assertDomain(result, "V27.1-pass-DTF", TraceDomainId.FORCE_BUDGET);
        assertDomain(result, "V27-maintenance-pass", TraceDomainId.FORCE_BUDGET);
    }

    @Test
    public void deployHandBloatPreservesForceSurchargeAndBoundary() {
        PolicyResult bloat = evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 8, 20, 15, false, 0));
        assertIds(bloat, "V37.4-pass");
        assertDeltas(bloat, -250.0f);
        assertEquals(
                "V37.4 HAND BLOAT: 15 cards in hand, 8 Force — DEPLOY SOMETHING!",
                bloat.operations().get(0).reason());

        PolicyResult below = evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 8, 20, 9, false, 0));
        assertFalse(hasRule(below, "V37.4-pass"));
    }

    @Test
    public void forceReservationBoundariesRemainExact() {
        assertEquals(20.0f, deltaById(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 3, 20, 7, true, 0)), "V27.1-pass-DTF"), 0.0f);
        assertEquals(40.0f, deltaById(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 1, 20, 7, true, 0)), "V27.1-pass-DTF"), 0.0f);
        assertEquals(60.0f, deltaById(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 0, 20, 7, true, 0)), "V27.1-pass-DTF"), 0.0f);
        assertFalse(hasRule(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 4, 20, 7, true, 0)), "V27.1-pass-DTF"));

        assertEquals(25.0f, deltaById(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 3, 20, 7, false, 2)), "V27-maintenance-pass"), 0.0f);
        assertEquals(50.0f, deltaById(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 1, 20, 7, false, 2)), "V27-maintenance-pass"), 0.0f);
        assertFalse(hasRule(evaluate(facts(4, Phase.DEPLOY,
                false, false, false, false, false, false,
                true, 4, 20, 7, false, 2)), "V27-maintenance-pass"));
    }

    @Test
    public void controlAndDrawSuppressionRemainNarrow() {
        PolicyResult control = evaluate(facts(4, Phase.CONTROL,
                false, false, true, false, false, false,
                true, 1, 5, 4, false, 0));
        assertIds(control, "PASS-small-hand");

        PolicyResult draw = evaluate(facts(4, Phase.DRAW,
                false, true, false, false, false, false,
                true, 1, 5, 4, false, 0));
        assertIds(draw, "PASS-low-reserve");
    }

    @Test
    public void passBaselineIsTypedWithoutChangingVisibleBehavior() {
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                "", com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.PASS,
                PassPolicy.BASE_SCORE, "Pass / Do nothing",
                PassPolicy.BASE_RULE_ID, PassPolicy.BASE_DOMAIN_ID,
                PassPolicy.BASE_OUTPUT_KIND, PassPolicy.BASE_REASON);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction(
                "", com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.PASS,
                PassPolicy.BASE_SCORE, "Pass / Do nothing",
                PassPolicy.BASE_RULE_ID, PassPolicy.BASE_DOMAIN_ID,
                PassPolicy.BASE_OUTPUT_KIND, PassPolicy.BASE_REASON);

        assertEquals(Float.floatToRawIntBits(5.0f),
                Float.floatToRawIntBits(rando.getScore()));
        assertEquals("Default pass option", rando.getReasoningString());
        assertEquals(rando.getReasoningString(), chosen.getReasoningString());
    }

    @Test
    public void passBaselineEmitsOneTypedInitialTraceOperation() {
        assertTrue(TraceSession.open("test-bot", "pass-decision",
                "CARD_ACTION_CHOICE", "Choose action", List.of("pass"),
                null, List.of("unit-test snapshot omitted"), false));
        DecisionTrace trace;
        try {
            new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                    "pass", com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.PASS,
                    PassPolicy.BASE_SCORE, "Pass / Do nothing",
                    PassPolicy.BASE_RULE_ID, PassPolicy.BASE_DOMAIN_ID,
                    PassPolicy.BASE_OUTPUT_KIND, PassPolicy.BASE_REASON);
            trace = TraceSession.close();
        } finally {
            TraceSession.abandon();
        }

        assertEquals(1, trace.getOperations().size());
        var initial = trace.getOperations().get(0);
        assertEquals(TraceOp.INITIAL, initial.getOp());
        assertEquals("PASS-baseline", initial.getRuleId().id());
        assertEquals(TraceDomainId.PASS_CANCEL, initial.getDomainId());
        assertEquals(TraceOutputKind.BANDED, initial.getOutputKind());
        assertEquals(PassPolicy.BASE_REASON, initial.getDetail());
    }

    @Test
    public void adaptersStayMirroredAndContainNoDirectPassArithmetic() throws IOException {
        String rando = modelSource("rando", "PassEvaluator.java");
        String chosen = modelSource("chosenone", "PassEvaluator.java");

        assertEquals(normalize(rando), normalize(chosen));
        assertEquals(normalize(modelSource("rando", "EvaluatedAction.java")),
                normalize(modelSource("chosenone", "EvaluatedAction.java")));
        assertEquals(1, occurrences(rando, "PassPolicy.evaluate("));
        assertFalse(rando.contains("action.addReasoning(\"Low on Force - prefer to pass\""));
        assertFalse(rando.contains("float bloatPenalty ="));
        assertTrue(rando.contains("PassPolicy.BASE_SCORE"));
    }

    private static PolicyResult evaluate(PassPolicy.Facts facts) {
        return PassPolicy.evaluate(facts);
    }

    private static PassPolicy.Facts facts(
            int turn, Phase phase,
            boolean activate, boolean draw, boolean control,
            boolean initiateBattle, boolean battleAction, boolean followthrough,
            boolean hasGameState, int force, int reserve, int hand,
            boolean dtf, int maintenance) {
        return new PassPolicy.Facts("", turn, phase, activate, draw, control,
                initiateBattle, battleAction, followthrough, hasGameState,
                force, reserve, hand, dtf, maintenance);
    }

    private static float deltaById(PolicyResult result, String id) {
        return result.operations().stream()
                .filter(operation -> operation.ruleArmId().id().equals(id))
                .findFirst().orElseThrow().delta();
    }

    private static boolean hasRule(PolicyResult result, String id) {
        return result.operations().stream()
                .anyMatch(operation -> operation.ruleArmId().id().equals(id));
    }

    private static void assertDomain(
            PolicyResult result, String id, TraceDomainId domainId) {
        var operation = result.operations().stream()
                .filter(candidate -> candidate.ruleArmId().id().equals(id))
                .findFirst().orElseThrow();
        assertEquals(domainId, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
    }

    private static void assertIds(PolicyResult result, String... ids) {
        assertEquals(List.of(ids), result.operations().stream()
                .map(operation -> operation.ruleArmId().id()).toList());
    }

    private static void assertDeltas(PolicyResult result, float... expected) {
        assertEquals(expected.length, result.operations().size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(Float.floatToRawIntBits(expected[i]),
                    Float.floatToRawIntBits(result.operations().get(i).delta()));
        }
    }

    private static String modelSource(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) {
                return repoLayout;
            }
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve("com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
