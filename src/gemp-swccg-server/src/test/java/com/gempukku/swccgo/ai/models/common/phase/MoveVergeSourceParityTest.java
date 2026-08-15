package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveVergeSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando", "MoveEvaluator.java")),
                normalize(evaluatorSource(
                        "chosenone", "MoveEvaluator.java")));
    }

    @Test
    public void deployReserveUsesTheExactFrontAndStaysMirrored()
            throws IOException {
        String rando = evaluatorSource("rando", "DeployEvaluator.java");
        String chosen = evaluatorSource(
                "chosenone", "DeployEvaluator.java");

        assertEquals(normalize(rando), normalize(chosen));
        assertTrue(rando.contains(
                "v48Objective.isOnTheVergeObjectiveFront()"));
        assertFalse(rando.contains(
                "pTitle.contains(\"taking control of the weapon\")"));
    }

    @Test
    public void vergeHasOneSharedClassifier() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveVergePolicy.evaluate("));
        assertFalse(move.contains("java.util.regex.Matcher v79m"));
        assertFalse(move.contains("int distFromScarif"));
        assertTrue(policy.contains("Pattern.compile(\"parsec\\\\s+(\\\\d+)\")"));
        assertTrue(policy.contains("destinationParsec = Integer.parseInt"));
        assertTrue(policy.contains("Math.abs(destinationParsec - 7)"));
    }

    @Test
    public void adapterRetainsAllLiveFactReadsAndMutation()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");

        assertTrue(move.contains("gameState.getAllPermanentCards()"));
        assertTrue(move.contains("playerId.equals(pc.getOwner())"));
        assertTrue(move.contains("pc.getBlueprint() == null"));
        assertTrue(move.contains("z == null || !z.isInPlay()"));
        assertTrue(move.contains("cardToMove.getSystemOrbited()"));
        assertTrue(move.contains("context.getObjectiveAnalyzer()"));
        assertTrue(move.contains("if (v79Verge)"));
        assertTrue(move.contains(
                "v79Analyzer.isOnTheVergeObjectiveFamily()"));
        assertFalse(move.contains("if (v79Verge && v79AtScarif)"));
        assertTrue(move.contains("V79b flip-state check error"));
        assertTrue(move.contains("V79 Death Star move check error"));
        assertTrue(move.contains("action.addReasoning("));
        assertTrue(move.contains("addObjectiveContribution("));
        assertTrue(move.contains(
                "MOVE.OBJECTIVE.ON_THE_VERGE.DEATH_STAR_ROUTE"));
        assertFalse(move.contains(
                "ladderVetoHard = v79Evaluation.hardVeto()"));
    }

    @Test
    public void adapterPreservesVergeOrderBeforePilotLock()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int cardResolve = move.indexOf("cardToMove = gameState.findCardById");
        int deathStarGate = move.indexOf(
                "contains(\"death star\")", cardResolve);
        int tableScan = move.indexOf(
                "gameState.getAllPermanentCards()", deathStarGate);
        int orbitRead = move.indexOf(
                "cardToMove.getSystemOrbited()", tableScan);
        int analyzerRead = move.indexOf(
                "context.getObjectiveAnalyzer()", orbitRead);
        int classify = move.indexOf(
                "MoveVergePolicy.evaluate(", analyzerRead);
        int add = move.indexOf(
                "addObjectiveContribution(", classify);
        int pilotLock = move.indexOf(
                "MoveTransitPolicy.pilotLock(", add);

        assertTrue(cardResolve >= 0);
        assertTrue(deathStarGate > cardResolve);
        assertTrue(tableScan > deathStarGate);
        assertTrue(orbitRead > tableScan);
        assertTrue(analyzerRead > orbitRead);
        assertTrue(classify > analyzerRead);
        assertTrue(add > classify);
        assertTrue(pilotLock > add);
    }

    @Test
    public void adapterKeepsExactBranchesAndBoundedLogs() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");

        for (String branch : new String[]{
                "ORBIT_SCARIF", "PARSEC_SEVEN", "ONE_HOP_FROM_SCARIF",
                "TOWARD_SCARIF", "WRONG_DIRECTION", "DEFAULT_MOVE",
                "POST_FLIP_RELEASE", "PRE_FLIP_HOLD"}) {
            assertTrue(branch, move.contains(
                    "MoveVergePolicy.Branch." + branch));
        }
        assertTrue(move.contains(
                "objective back does not require Scarif orbit"));
        assertTrue(move.contains(
                "Death Star orbiting Scarif pre-flip, bounded movement penalty"));
    }

    @Test
    public void actionTextRecognitionAndParsecWindowsStayMirrored()
            throws IOException {
        String rando = evaluatorSource(
                "rando", "ActionTextEvaluator.java");
        String chosenOne = evaluatorSource(
                "chosenone", "ActionTextEvaluator.java");

        assertEquals(normalize(canEvaluateSlice(rando)),
                normalize(canEvaluateSlice(chosenOne)));
        assertEquals(normalize(parsecWindow(rando)),
                normalize(parsecWindow(chosenOne)));
    }

    @Test
    public void actionTextAdaptersDelegateOnlyScoreArithmetic()
            throws IOException {
        String actionText = parsecWindow(evaluatorSource(
                "rando", "ActionTextEvaluator.java"));
        String policy = policySource();

        assertEquals(1, countOccurrences(
                actionText, "MoveVergePolicy.evaluateParsecChoice("));
        assertEquals(1, countOccurrences(
                actionText, "MoveVergePolicy.evaluateDestinationChoice("));
        assertEquals(1, countOccurrences(
                actionText, "MoveVergePolicy.evaluateParsecFallback("));
        assertFalse(actionText.contains("Math.abs(parsec - 7)"));
        assertFalse(actionText.contains("Math.abs(fparsec - 7)"));
        assertFalse(actionText.contains("Math.max(0, 300"));

        assertEquals(2, countOccurrences(
                actionText, "Integer.parseInt(actionText.trim())"));
        assertEquals(2, countOccurrences(
                actionText, ".compile(\"(\\\\d+)\")"));
        assertTrue(actionText.contains("gameState.getAllPermanentCards()"));
        assertTrue(actionText.contains("pOwner.replace(\"~\", \"\")"));
        assertTrue(actionText.contains("pZone == null || !pZone.isInPlay()"));
        assertTrue(actionText.contains("pc.getSystemOrbited()"));
        assertTrue(actionText.contains("catch (Exception e) { /* ignore */ }"));
        assertEquals(3, countOccurrences(
                actionText, "actions.add(action);"));
        assertEquals(7, countOccurrences(actionText, "continue;"));
        assertFalse(actionText.contains(".sort("));

        assertTrue(policy.contains(
                "V79 PARSEC 7 (Scarif!) — pick this"));
        assertTrue(policy.contains(
                "V79 ORBIT SCARIF: preferred objective destination (+300)"));
        assertTrue(policy.contains(
                "V79 destination not Scarif — avoid"));
        assertTrue(policy.contains(
                "V103 PARSEC FALLBACK: parsec %d (dist %d to Scarif) → +%.0f"));
    }

    @Test
    public void actionTextPreservesRecognitionAndControlOrder()
            throws IOException {
        String source = evaluatorSource(
                "rando", "ActionTextEvaluator.java");
        String recognition = canEvaluateSlice(source);
        String window = parsecWindow(source);

        int multipleChoice = recognition.indexOf(
                "\"MULTIPLE_CHOICE\".equals(decisionType)");
        int parsecRecognition = recognition.indexOf(
                "dtLower.contains(\"choose parsec to move to\")");
        int destinationRecognition = recognition.indexOf(
                "dtLower.contains(\"choose destination for\")",
                parsecRecognition);
        int recognitionReturn = recognition.indexOf(
                "return true;", destinationRecognition);

        int promptRead = window.indexOf("String v79DtLower =");
        int promptClassification = window.indexOf(
                "boolean v79IsParsecChoice", promptRead);
        int tableScan = window.indexOf(
                "gameState.getAllPermanentCards()", promptClassification);
        int scanCatch = window.indexOf(
                "catch (Exception e) { /* ignore */ }", tableScan);
        int detectLog = window.indexOf(
                "logger.warn(\"V103 PARSEC DETECT:", scanCatch);
        int impliedVerge = window.indexOf(
                "if (!v79Verge && v79HaveDeathStar", detectLog);
        int primaryGate = window.indexOf(
                "if (v79Verge && !v79AtScarif)", impliedVerge);
        int primaryParse = window.indexOf(
                "Integer.parseInt(actionText.trim())", primaryGate);
        int primaryCatch = window.indexOf(
                "catch (Exception e)", primaryParse);
        int primaryPolicy = window.indexOf(
                "MoveVergePolicy.evaluateParsecChoice(", primaryCatch);
        int destinationPolicy = window.indexOf(
                "MoveVergePolicy.evaluateDestinationChoice(", primaryPolicy);
        int primaryAdd = window.indexOf(
                "actions.add(action);", destinationPolicy);
        int primaryContinue = window.indexOf("continue;", primaryAdd);
        int fallbackGate = window.indexOf(
                "if (v79Verge && v79IsParsecChoice)", primaryContinue);
        int fallbackParse = window.indexOf(
                "Integer.parseInt(actionText.trim())", fallbackGate);
        int fallbackCatch = window.indexOf(
                "catch (Exception e)", fallbackParse);
        int fallbackPolicy = window.indexOf(
                "MoveVergePolicy.evaluateParsecFallback(", fallbackCatch);
        int fallbackAdd = window.indexOf(
                "actions.add(action);", fallbackPolicy);
        int fallbackContinue = window.indexOf("continue;", fallbackAdd);

        assertTrue(multipleChoice >= 0);
        assertTrue(parsecRecognition > multipleChoice);
        assertTrue(destinationRecognition > parsecRecognition);
        assertTrue(recognitionReturn > destinationRecognition);
        assertTrue(promptRead >= 0);
        assertTrue(promptClassification > promptRead);
        assertTrue(tableScan > promptClassification);
        assertTrue(scanCatch > tableScan);
        assertTrue(detectLog > scanCatch);
        assertTrue(impliedVerge > detectLog);
        assertTrue(primaryGate > impliedVerge);
        assertTrue(primaryParse > primaryGate);
        assertTrue(primaryCatch > primaryParse);
        assertTrue(primaryPolicy > primaryCatch);
        assertTrue(destinationPolicy > primaryPolicy);
        assertTrue(primaryAdd > destinationPolicy);
        assertTrue(primaryContinue > primaryAdd);
        assertTrue(fallbackGate > primaryContinue);
        assertTrue(fallbackParse > fallbackGate);
        assertTrue(fallbackCatch > fallbackParse);
        assertTrue(fallbackPolicy > fallbackCatch);
        assertTrue(fallbackAdd > fallbackPolicy);
        assertTrue(fallbackContinue > fallbackAdd);
    }

    @Test
    public void legacyRandoInterceptorIsRetiredAndBothEvaluatorsOwnTheRoute()
            throws IOException {
        String calAi = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/rando")
                .resolve("RandoCalAi.java"));
        assertTrue(calAi.contains(
                "V79b DEATH STAR PARSEC: choices={} -> index {} (parsec {}, closest to Scarif 7)"));
        assertTrue(calAi.contains(
                "direct interceptor V79b: evaluator lane never runs on this route"));
        assertTrue(calAi.contains(
                "boolean useLegacyV79bDirectInterceptor = false"));
        assertTrue(calAi.contains(
                "V79b FRONT FLIP HOLD: unflipped + orbiting Scarif"));
        assertFalse(calAi.contains("V79b FLIP-BACK GUARD"));

        for (String bot : new String[]{"rando", "chosenone"}) {
            String actionText = evaluatorSource(
                    bot, "ActionTextEvaluator.java");
            assertTrue(actionText.contains(
                    "V103 PARSEC FALLBACK: Verge implied by Death Star ownership + parsec prompt"));
            assertTrue(actionText.contains(
                    "V79 PARSEC CHOICE: parsec 7 (Scarif) -> +300"));
            assertFalse(actionText.contains(
                    "V79 PARSEC CHOICE: parsec 7 (Scarif) -> +1500"));
            assertTrue(actionText.contains(
                    "MoveVergePolicy.evaluateParsecFallback(fparsec)"));
            assertEquals(3, countOccurrences(actionText,
                    ".isOnTheVergeObjectiveFront()"));
            assertFalse(actionText.contains(
                    ".isOnTheVergeObjectiveFamily()"));
        }
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "EvaluatedAction", "addReasoning",
                "logger", "ladderVeto", "PolicyOperation", "PolicyResult",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String evaluatorSource(
            String bot, String evaluator) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(evaluator));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveVergePolicy.java"));
    }

    private static String canEvaluateSlice(String source) {
        int start = source.indexOf(
                "public boolean canEvaluate(DecisionContext context)");
        int end = source.indexOf(
                "public List<EvaluatedAction> evaluate(DecisionContext context)",
                start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static String parsecWindow(String source) {
        int start = source.indexOf(
                "// V79 (Steve, 2026-05-15): VERGE");
        int end = source.indexOf(
                "// V67bi FORCE LIGHTNING SELF-TARGET HARD-BLOCK", start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) {
                return repoLayout;
            }
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT")
                .lines()
                .map(line -> line.stripLeading().startsWith("//")
                        ? line.stripLeading() : line)
                .collect(Collectors.joining("\n"));
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
