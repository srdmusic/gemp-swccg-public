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

public class MoveTransitSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void transitRulesHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.pilotLock("));
        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.movementTypes("));
        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.hiddenPathTransit("));
        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.capacitySlot("));
        assertFalse(move.contains(
                "if (cardToMove != null && cardToMove.isPilotOf())"));
        assertFalse(move.contains(
                "if (actionLower.contains(\"shuttle\") || actionLower.contains(\"transport\"))"));
        assertTrue(policy.contains("public static PilotLock pilotLock("));
        assertTrue(policy.contains(
                "public static MovementTypes movementTypes("));
        assertTrue(policy.contains(
                "public static HiddenPathTransit hiddenPathTransit("));
        assertTrue(policy.contains(
                "public static CapacitySlot capacitySlot("));

        int hiddenPathStart = move.indexOf(
                "// === V53b: HIDDEN PATH JEDI TRANSIT PREFERENCE ===");
        int hiddenPathEnd = move.indexOf(
                "// T4.1 (2026-07-06): LADDER FINALIZER", hiddenPathStart);
        String hiddenPathRegion = move.substring(
                hiddenPathStart, hiddenPathEnd);
        assertFalse(hiddenPathRegion.contains(
                "PhysicalCard srcLoc = cardToMove.getAtLocation()"));
        assertFalse(hiddenPathRegion.contains(
                "srcName.contains(\"safehouse\")"));
    }

    @Test
    public void adapterRetainsScoreAndLoggingOwnership() throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "action.addReasoning(pilotLock.contribution().reason(),"));
        assertTrue(move.contains(
                "action.addReasoning(defensiveShuttle.contribution().reason(),"));
        assertTrue(move.contains(
                "movementTypes.dockingBayTransit().reason()"));
        assertTrue(move.contains("movementTypes.takeOff().reason()"));
        assertTrue(move.contains(
                "V25 PILOT LOCK: {} is piloting {}"));
        assertTrue(move.contains(
                "V25 Defensive shuttle to {}"));
        assertTrue(move.contains(
                "V25 Shuttle without defensive need"));
        assertTrue(move.contains(
                "hpMoveAnalyzer.isHiddenPathObjectiveFamily()"));
        assertTrue(move.contains(
                "!hpMoveAnalyzer.isFlipped()"));
        assertTrue(move.contains(
                "Keyword.JEDI_SURVIVOR"));
        assertFalse(move.contains(
                "hpMoveAnalyzer != null && hpMoveAnalyzer.isAnalyzed()"));
        assertTrue(move.contains(
                "hiddenPath.contribution().reason()"));
        assertTrue(move.contains("hiddenPath.hardVeto()"));
        assertTrue(move.contains(
                "MOVE.OBJECTIVE.HIDDEN_PATH.TRANSIT"));
        assertTrue(move.contains(
                "V53b HIDDEN PATH: {} Safehouse to Corridor objective preference +300"));
        assertTrue(move.contains(
                "V60 HIDDEN PATH: {} receives a bounded -300 preference against landspeed from Corridor"));
        assertTrue(move.contains(
                "V53b HIDDEN PATH: {} leaving Mapuzo via landspeed, objective preference +300"));
        assertFalse(move.contains(
                "ladderClaimR4Transit(hiddenPath.claimIdentity())"));
        assertTrue(move.contains(
                "capacitySlot.contribution().reason()"));
        assertTrue(move.contains(
                "[MoveEvaluator] SKIP passenger slot move"));
        assertTrue(move.contains(
                "[MoveEvaluator] Strongly prefer pilot capacity slot move"));
    }

    @Test
    public void adapterCallsRemainInLegacyOrder() throws IOException {
        String move = evaluatorSource("rando");
        int deathStar = move.indexOf("// === V79 (Steve");
        int pilot = move.indexOf("MoveTransitPolicy.pilotLock(", deathStar);
        int lando = move.indexOf("// === V47: LANDO", pilot);
        int movementTypes = move.indexOf(
                "MoveTransitPolicy.movementTypes(", lando);
        int docking = move.indexOf(
                "movementTypes.dockingBayTransit().applies()", movementTypes);
        int takeOff = move.indexOf(
                "movementTypes.takeOff().applies()", docking);
        int landing = move.indexOf("MoveLandingPolicy.evaluate(", takeOff);
        int spy = move.indexOf("MoveSpyFollowPolicy.evaluate(", landing);
        int hiddenPath = move.indexOf(
                "MoveTransitPolicy.hiddenPathTransit(", spy);
        int hiddenPathScore = move.indexOf(
                "addObjectiveContribution(", hiddenPath);
        int hiddenPathRule = move.indexOf(
                "MOVE.OBJECTIVE.HIDDEN_PATH.TRANSIT", hiddenPathScore);
        int hiddenPathVeto = move.indexOf(
                "if (hiddenPath.hardVeto())", hiddenPathRule);
        int hiddenPathSafehouseLog = move.indexOf(
                "V53b HIDDEN PATH: {} Safehouse to Corridor objective preference +300",
                hiddenPathVeto);
        int hiddenPathCorridorLog = move.indexOf(
                "V60 HIDDEN PATH: {} receives a bounded -300 preference against landspeed from Corridor",
                hiddenPathSafehouseLog);
        int finalizer = move.indexOf(
                "ladderFinalize(action)", hiddenPathCorridorLog);

        assertTrue(deathStar >= 0);
        assertTrue(pilot > deathStar);
        assertTrue(lando > pilot);
        assertTrue(movementTypes > lando);
        assertTrue(docking > movementTypes);
        assertTrue(takeOff > docking);
        assertTrue(landing > takeOff);
        assertTrue(spy > landing);
        assertTrue(hiddenPath > spy);
        assertTrue(hiddenPathScore > hiddenPath);
        assertTrue(hiddenPathRule > hiddenPathScore);
        assertTrue(hiddenPathVeto > hiddenPathRule);
        assertTrue(hiddenPathSafehouseLog > hiddenPathVeto);
        assertTrue(hiddenPathCorridorLog > hiddenPathSafehouseLog);
        assertTrue(finalizer > hiddenPathCorridorLog);
    }

    @Test
    public void capacitySlotCallRemainsBeforeGeneralMoveConstruction()
            throws IOException {
        String move = evaluatorSource("rando");
        int blockedGate = move.indexOf(
                "MoveBlockedResponsePolicy.matches(");
        int capacity = move.indexOf(
                "MoveTransitPolicy.capacitySlot(", blockedGate);
        int passenger = move.indexOf(
                "CapacitySlotBranch.PASSENGER_SKIP", capacity);
        int pilot = move.indexOf(
                "CapacitySlotBranch.PILOT_PREFER", passenger);
        int pilotConstruction = move.indexOf(
                "EvaluatedAction pilotAction = new EvaluatedAction(", pilot);
        int generalConstruction = move.indexOf(
                "EvaluatedAction action = new EvaluatedAction(",
                pilotConstruction);

        assertTrue(blockedGate >= 0);
        assertTrue(capacity > blockedGate);
        assertTrue(passenger > capacity);
        assertTrue(pilot > passenger);
        assertTrue(pilotConstruction > pilot);
        assertTrue(generalConstruction > pilotConstruction);
    }

    @Test
    public void capacitySlotOwnerPreservesPassengerFirstAndPilotScores()
            throws IOException {
        String policy = policySource();
        int method = policy.indexOf(
                "public static CapacitySlot capacitySlot(");
        int passenger = policy.indexOf(
                "actionLower.contains(\"passenger capacity slot\")",
                method);
        int pilot = policy.indexOf(
                "actionLower.contains(\"pilot capacity slot\")",
                passenger);
        int baseScore = policy.indexOf("100.0f", pilot);
        int reason = policy.indexOf(
                "\"Move to pilot slot - adds power!\"", baseScore);
        int delta = policy.indexOf("50.0f", reason);

        assertTrue(method >= 0);
        assertTrue(passenger > method);
        assertTrue(pilot > passenger);
        assertTrue(baseScore > pilot);
        assertTrue(reason > baseScore);
        assertTrue(delta > reason);
    }

    @Test
    public void actionTextNoSwapUsesSharedOwnerAndRetainsTerminalContinue()
            throws IOException {
        String policy = policySource();
        assertTrue(policy.contains(
                "public static Contribution capacitySlotSwap("));
        assertTrue(policy.contains(
                "V87 NO SWAP: pilot↔passenger capacity slot rearrangement is pointless — hard block"));

        for (String bot : new String[]{"rando", "chosenone"}) {
            String actionText = evaluatorSource(
                    bot, "ActionTextEvaluator.java");
            assertEquals(1, countOccurrences(actionText,
                    "MoveTransitPolicy.capacitySlotSwap("));
            assertFalse(actionText.contains(
                    "textLower.contains(\"move to passenger capacity slot\")"));
            assertFalse(actionText.contains(
                    "textLower.contains(\"move to pilot capacity slot\")"));

            int call = actionText.indexOf(
                    "MoveTransitPolicy.capacitySlotSwap(");
            int score = actionText.indexOf(
                    "action.addReasoning(v87CapacitySwap.reason()", call);
            int append = actionText.indexOf("actions.add(action);", score);
            int terminalContinue = actionText.indexOf("continue;", append);
            int odin = actionText.indexOf("// === V134", terminalContinue);
            assertTrue(call >= 0);
            assertTrue(score > call);
            assertTrue(append > score);
            assertTrue(terminalContinue > append);
            assertTrue(odin > terminalContinue);
        }
    }

    @Test
    public void policyPreservesFirstMatchAndPrintedPowerGates()
            throws IOException {
        String policy = policySource();
        int locations = policy.indexOf(
                "for (PhysicalCard location : gameState.getLocationsInOrder())");
        int powerAttribute = policy.indexOf(
                "!blueprint.hasPowerAttribute()", locations);
        int threshold = policy.indexOf(
                "ourPower > 0 && theirPower >= ourPower * 2", powerAttribute);
        int firstMatchBreak = policy.indexOf("break;", threshold);

        assertTrue(locations >= 0);
        assertTrue(powerAttribute > locations);
        assertTrue(threshold > powerAttribute);
        assertTrue(firstMatchBreak > threshold);
    }

    @Test
    public void policyPreservesHiddenPathBranchOrderAndBoundedWeights()
            throws IOException {
        String policy = policySource();
        int objectiveGate = policy.indexOf(
                ".contains(\"hidden path\")");
        int sourceRead = policy.indexOf(
                "PhysicalCard sourceLocation = cardToMove.getAtLocation()",
                objectiveGate);
        int landspeed = policy.indexOf(
                "actionLower.contains(\"move using landspeed\")",
                sourceRead);
        int safehouse = policy.indexOf(
                "sourceName.contains(\"safehouse\") && landspeed",
                landspeed);
        int corridor = policy.indexOf(
                "sourceName.contains(\"underground corridor\")",
                safehouse);
        int mapuzo = policy.indexOf(
                "sourceName.contains(\"mapuzo\") && landspeed",
                corridor);

        assertTrue(objectiveGate >= 0);
        assertTrue(sourceRead > objectiveGate);
        assertTrue(landspeed > sourceRead);
        assertTrue(safehouse > landspeed);
        assertTrue(corridor > safehouse);
        assertTrue(mapuzo > corridor);
        assertFalse(policy.contains("800.0f"));
        assertTrue(policy.contains("V53b SAFEHOUSE→CORRIDOR"));
        assertTrue(policy.contains("V53b MAPUZO EXIT"));
        assertTrue(policy.contains(
                "V60 HIDDEN PATH LANDSPEED PREFERENCE:"));
        assertTrue(policy.contains(
                "prefer the Corridor transit text instead of moving back to Mapuzo"));
    }

    @Test
    public void positiveHiddenPathActionTextTransitUsesSharedOwner()
            throws IOException {
        String policy = policySource();
        assertTrue(policy.contains(
                "public static boolean isPositiveHiddenPathTransitAction("));
        assertTrue(policy.contains(
                "public static Contribution positiveHiddenPathTransit("));
        assertTrue(policy.contains(
                "V60 HIDDEN PATH TRANSIT: Move Jedi OUT of Corridor to advance the objective"));
        assertTrue(policy.contains(
                "Move Jedi transit action — tactical mobility"));

        for (String bot : new String[]{"rando", "chosenone"}) {
            String actionText = evaluatorSource(
                    bot, "ActionTextEvaluator.java");
            assertEquals(1, countOccurrences(actionText,
                    "MoveTransitPolicy.isPositiveHiddenPathTransitAction("));
            assertEquals(1, countOccurrences(actionText,
                    "MoveTransitPolicy.positiveHiddenPathTransit("));
            assertFalse(actionText.contains(
                    "textLower.contains(\"move jedi survivor here to a site\")"));
            assertFalse(actionText.contains(
                    "Move Jedi transit action — tactical mobility\", 200.0f"));

            int rack = actionText.indexOf(
                    "V29.6 BLASTER RACK: BLOCKED proactive racking");
            int classifier = actionText.indexOf(
                    "MoveTransitPolicy.isPositiveHiddenPathTransitAction(", rack);
            int objectiveRead = actionText.indexOf(
                    "context.getObjectiveAnalyzer()", classifier);
            int contribution = actionText.indexOf(
                    "MoveTransitPolicy.positiveHiddenPathTransit(",
                    objectiveRead);
            int score = actionText.indexOf(
                    "addObjectiveContribution(", contribution);
            int rule = actionText.indexOf(
                    "MOVE.OBJECTIVE.HIDDEN_PATH.TRANSIT_ACTION", score);
            int log = actionText.indexOf(
                    "V60 HIDDEN PATH TRANSIT: '{}' -> +300 objective preference",
                    rule);
            int pull = actionText.indexOf("// === REGION: PULL ===", log);
            String branch = actionText.substring(classifier, pull);

            assertTrue(rack >= 0);
            assertTrue(classifier > rack);
            assertTrue(objectiveRead > classifier);
            assertTrue(contribution > objectiveRead);
            assertTrue(score > contribution);
            assertTrue(rule > score);
            assertTrue(log > rule);
            assertTrue(pull > log);
            assertFalse(branch.contains("action.setActionType("));
        }
    }

    @Test
    public void policyContainsNoScoringTransportOrEngineMetadata()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "PolicyOperation", "PolicyResult", "DecisionContext",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot) throws IOException {
        return evaluatorSource(bot, "MoveEvaluator.java");
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
                .resolve("MoveTransitPolicy.java"));
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
