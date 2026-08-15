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

public class MoveDestinationSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void moveDestinationAdaptersStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalize(cardSelectionSource("rando")),
                normalize(cardSelectionSource("chosenone")));
    }

    @Test
    public void destinationRulesHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.landedShipEscape("));
        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.destinationContest("));
        assertTrue(policy.contains(
                "public static LandedShipEscape landedShipEscape("));
        assertTrue(policy.contains(
                "public static DestinationContest destinationContest("));
    }

    @Test
    public void adaptersRetainScoreLadderVetoAndLoggingOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "escape.contribution().reason()"));
        assertTrue(move.contains(
                "ladderClaimR3(\"V91 ESCAPE LANDED SHIP\")"));
        assertTrue(move.contains(
                "destination.contestContribution().reason()"));
        assertTrue(move.contains(
                "ladderClaimR2(\"V34 CONTEST\""));
        assertTrue(move.contains(
                "ladderClaimR2(\"V111 BG ADVANCE\""));
        assertTrue(move.contains("ladderWrongDirVeto = true"));
        assertTrue(move.contains("ladderVetoHard = true"));
        assertTrue(move.contains(
                "V38.3 CASTLE RETREAT BLOCKED (LADDER VETO)"));
    }

    @Test
    public void callsRemainAtLegacyPositions() throws IOException {
        String move = evaluatorSource("rando");
        int explicitDrain = move.indexOf(
                "MoveDrainRoutingPolicy.explicitDestinationDrain(");
        int escape = move.indexOf(
                "MoveDestinationPolicy.landedShipEscape(", explicitDrain);
        int shuttle = move.indexOf(
                "MoveDrainRoutingPolicy.cantinaShuttle(", escape);
        int contest = move.indexOf(
                "MoveDestinationPolicy.destinationContest(", shuttle);
        int methodEnd = move.indexOf(
                "// Default: not a good time to move", contest);

        assertTrue(explicitDrain >= 0);
        assertTrue(escape > explicitDrain);
        assertTrue(shuttle > escape);
        assertTrue(contest > shuttle);
        assertTrue(methodEnd > contest);
    }

    @Test
    public void policyPreservesIndependentScansAndPredicates()
            throws IOException {
        String policy = policySource();

        assertEquals(1, countOccurrences(
                policy, "gameState.getAllPermanentCards()"));
        assertEquals(3, countOccurrences(
                policy, "gameState.getLocationsInOrder()"));
        assertTrue(policy.contains(
                "cardLocation == location"));
        assertTrue(policy.contains(
                "actionLower.contains(locationName)"));
        assertTrue(policy.contains(
                "opponentPowerAtDestination > 0"));
        assertTrue(policy.contains(
                "opponentPower > opponentUncontestedPower"));
        assertTrue(policy.contains(
                "destinationTitle.contains(\"mustafar\")"));
    }

    @Test
    public void v37AndV135HaveSharedDestinationPolicyOwners()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.resolveDestination("));
        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.battlegroundRetreat("));
        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.isSelfMoveToFriend("));
        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.companionVeto("));
        assertFalse(move.contains("for (PhysicalCard loc37"));
        assertFalse(move.contains("v135GtLower.contains("));
        assertFalse(move.contains(
                "if (currentIsBattleground && !destIsBattleground)"));
        assertTrue(policy.contains("public static PhysicalCard resolveDestination("));
        assertTrue(policy.contains("public static Contribution battlegroundRetreat("));
        assertTrue(policy.contains("public static boolean isSelfMoveToFriend("));
        assertTrue(policy.contains("public static CompanionVeto companionVeto("));
    }

    @Test
    public void v169RetreatScoreAndWrongDirectionExemptionHaveSharedOwner()
            throws IOException {
        String cardSelection = moveDestinationBlock(
                cardSelectionSource("rando"));
        String policy = policySource();

        assertEquals(2, countOccurrences(
                cardSelection, "MoveDestinationPolicy.retreatMode("));
        assertEquals(1, countOccurrences(
                cardSelection,
                "MoveDestinationPolicy.safeRetreatDestination("));
        assertEquals(1, countOccurrences(
                cardSelection,
                "MoveDestinationPolicy.retreatExemptsWrongDirection("));
        assertFalse(cardSelection.contains("v169RetreatMode"));
        assertFalse(cardSelection.contains("v169FromTitle"));
        assertFalse(cardSelection.contains(
                "get the endangered character out of"));
        assertTrue(policy.contains(
                "public static RetreatMode retreatMode("));
        assertTrue(policy.contains(
                "public static Contribution safeRetreatDestination("));
        assertTrue(policy.contains(
                "public static boolean retreatExemptsWrongDirection("));
        assertTrue(policy.contains("600.0f"));
    }

    @Test
    public void cardSelectionDestinationSafetyRulesHaveOneSharedOwner()
            throws IOException {
        String cardSelection = moveDestinationBlock(
                cardSelectionSource("rando"));
        String policy = policySource();

        for (String call : new String[]{
                "MoveDestinationPolicy.retreatToDrain(",
                "MoveDestinationPolicy.powerAwareHiddenPathDestination(",
                "MoveDestinationPolicy.hiddenPathPreFlipSuicide(",
                "MoveDestinationPolicy.spyAwareContest(",
                "MoveDestinationPolicy.drainThreat(",
                "MoveDestinationPolicy.wrongDirection(",
                "MoveDestinationPolicy.isCastleDestination(",
                "MoveDestinationPolicy.castleRetreat("}) {
            assertEquals(call, 1, countOccurrences(cardSelection, call));
        }
        for (String owner : new String[]{
                "public static Contribution retreatToDrain(",
                "public static PowerAwareDestination powerAwareHiddenPathDestination(",
                "public static Contribution hiddenPathPreFlipSuicide(",
                "public static SpyAwareContest spyAwareContest(",
                "public static DrainThreatDisposition drainThreat(",
                "public static WrongDirectionEvaluation wrongDirection(",
                "public static boolean isCastleDestination(",
                "public static Contribution castleRetreat("}) {
            assertTrue(owner, policy.contains(owner));
        }

        for (String duplicateReason : new String[]{
                "is over-contested (their %.0f vs our %.0f)",
                "solo Jedi will DIE on their next turn!",
                "pre-flip Jedi survivors are power 3, this is SUICIDE!",
                "has only opponent spy (",
                "is empty — opponents draining at %s! Go there instead!",
                "NEVER retreat to Castle while opponents exist!"}) {
            assertFalse(duplicateReason,
                    cardSelection.contains(duplicateReason));
            assertTrue(duplicateReason, policy.contains(duplicateReason));
        }
    }

    @Test
    public void objectiveActorRoutesUseOneAnalyzerFactAndSharedScores()
            throws IOException {
        String move = evaluatorSource("rando");
        String destination = moveDestinationBlock(
                cardSelectionSource("rando"));
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, ".advancesPreFlipActorRoute("));
        assertEquals(1, countOccurrences(
                destination, ".advancesPreFlipActorRoute("));
        assertEquals(1, countOccurrences(
                move, ".advancesPreFlipActorAtRuntimeLocation("));
        assertEquals(1, countOccurrences(
                destination, ".advancesPreFlipActorAtRuntimeLocation("));
        assertEquals(1, countOccurrences(
                move,
                ".advancesPreFlipPlainPresenceAtRequiredLocation("));
        assertEquals(1, countOccurrences(
                destination,
                ".advancesPreFlipPlainPresenceAtRequiredLocation("));
        assertEquals(1, countOccurrences(
                move,
                ".objectiveActorRouteStart("));
        assertEquals(2, countOccurrences(
                move,
                ".objectiveActorLocationStart("));
        assertEquals(1, countOccurrences(
                destination,
                ".objectiveActorRouteDestination("));
        assertEquals(1, countOccurrences(
                destination,
                ".objectiveActorLocationDestination("));
        assertTrue(policy.contains(
                "public static Contribution objectiveActorRouteStart("));
        assertTrue(policy.contains(
                "public static Contribution objectiveActorRouteDestination("));
        assertTrue(policy.contains(
                "public static Contribution objectiveActorLocationStart("));
        assertTrue(policy.contains(
                "public static Contribution objectiveActorLocationDestination("));
        assertTrue(policy.contains("TERMINAL_ESCAPE_EXEMPT"));
        assertFalse(policy.contains("OBJECTIVE_ROUTE_EXEMPT"));
        assertFalse(policy.contains("HIDDEN_PATH_EXEMPT"));
    }

    @Test
    public void objectiveActorRouteKeepsFormationSafetyAheadOfScoring()
            throws IOException {
        String move = evaluatorSource("rando");
        int parentFact = move.indexOf(
                ".advancesPreFlipActorRoute(");
        int parentDestinationVeto = move.indexOf(
                ".vetoMoveDestination(", parentFact);
        int parentOriginVeto = move.indexOf(
                ".vetoMoveOrigin(", parentDestinationVeto);
        int parentRouteScore = move.indexOf(
                ".objectiveActorRouteStart(",
                parentOriginVeto);
        int parentLocationScore = move.indexOf(
                ".objectiveActorLocationStart(",
                parentOriginVeto);
        int parentClaim = move.indexOf(
                "ladderClaimR2(", parentLocationScore);

        assertTrue(parentFact >= 0);
        assertTrue(parentDestinationVeto > parentFact);
        assertTrue(parentOriginVeto > parentDestinationVeto);
        assertTrue(parentRouteScore > parentOriginVeto);
        assertTrue(parentLocationScore > parentOriginVeto);
        assertTrue(parentClaim > parentRouteScore);
        assertTrue(parentClaim > parentLocationScore);

        String destination = moveDestinationBlock(
                cardSelectionSource("rando"));
        int childHardVeto = destination.indexOf("action.hardVeto(");
        int childFact = destination.indexOf(
                ".advancesPreFlipActorRoute(", childHardVeto);
        int childLocationFact = destination.indexOf(
                ".advancesPreFlipActorAtRuntimeLocation(", childFact);
        int childRouteScore = destination.indexOf(
                ".objectiveActorRouteDestination(", childFact);
        int childLocationScore = destination.indexOf(
                ".objectiveActorLocationDestination(", childRouteScore);
        int wrongDirection = destination.indexOf(
                "MoveDestinationPolicy.wrongDirection(",
                childLocationScore);

        assertTrue(childHardVeto >= 0);
        assertTrue(childFact > childHardVeto);
        assertTrue(childLocationFact > childFact);
        assertTrue(childRouteScore > childLocationFact);
        assertTrue(childLocationScore > childRouteScore);
        assertTrue(wrongDirection > childLocationScore);
    }

    @Test
    public void hiddenPathSuicideStillStopsBeforeContestScoring()
            throws IOException {
        String block = moveDestinationBlock(cardSelectionSource("rando"));
        int v169 = block.indexOf(
                "MoveDestinationPolicy.safeRetreatDestination(");
        int v67au = block.indexOf(
                "MoveDestinationPolicy.retreatToDrain(", v169);
        int v64 = block.indexOf(
                "MoveDestinationPolicy.powerAwareHiddenPathDestination(",
                v67au);
        int suicide = block.indexOf(
                "MoveDestinationPolicy.hiddenPathPreFlipSuicide(", v64);
        int append = block.indexOf("actions.add(action);", suicide);
        int stop = block.indexOf("continue;", append);
        int contest = block.indexOf(
                "MoveDestinationPolicy.spyAwareContest(", stop);
        int laterRules = block.indexOf(
                "// === V24.3C: DR. EVAZAN", contest);

        assertTrue(v169 >= 0);
        assertTrue(v67au > v169);
        assertTrue(v64 > v67au);
        assertTrue(suicide > v64);
        assertTrue(append > suicide);
        assertTrue(stop > append);
        assertTrue(contest > stop);
        assertTrue(laterRules > contest);
    }

    @Test
    public void adapterPreservesDestinationFactAndVetoOrder()
            throws IOException {
        String block = moveDestinationBlock(cardSelectionSource("rando"));
        int nonSpyScan = block.indexOf("hc.isUndercover()");
        int suicide = block.indexOf(
                "MoveDestinationPolicy.hiddenPathPreFlipSuicide(",
                nonSpyScan);
        int jediScan = block.indexOf(
                "ActionEvaluator.isJediOrPadawan(cTitle)", suicide);
        int contest = block.indexOf(
                "MoveDestinationPolicy.spyAwareContest(", jediScan);
        int threatSpyScan = block.indexOf("osc.isUndercover()", contest);
        int threat = block.indexOf(
                "MoveDestinationPolicy.drainThreat(", threatSpyScan);
        int strictHighest = block.indexOf(
                "oppPower > worstDrainPower", threat);
        int direction = block.indexOf(
                "MoveDestinationPolicy.wrongDirection(", strictHighest);
        int castleGate = block.indexOf(
                "MoveDestinationPolicy.isCastleDestination(", direction);
        int castle = block.indexOf(
                "MoveDestinationPolicy.castleRetreat(", castleGate);

        assertTrue(nonSpyScan >= 0);
        assertTrue(suicide > nonSpyScan);
        assertTrue(jediScan > suicide);
        assertTrue(contest > jediScan);
        assertTrue(threatSpyScan > contest);
        assertTrue(threat > threatSpyScan);
        assertTrue(strictHighest > threat);
        assertTrue(direction > strictHighest);
        assertTrue(castleGate > direction);
        assertTrue(castle > castleGate);
    }

    @Test
    public void v37AdapterRetainsActionAndBattlegroundReadOrder()
            throws IOException {
        String block = v37Block(evaluatorSource("rando"));
        int actionText = block.indexOf("action.getDisplayText()");
        int destination = block.indexOf(
                "MoveDestinationPolicy.resolveDestination(", actionText);
        int destinationBlueprint = block.indexOf(
                "destLoc37.getBlueprint()", destination);
        int destinationBg = block.indexOf(
                "isBattleground(gameState, destLoc37", destinationBlueprint);
        int currentBg = block.indexOf(
                "isBattleground(gameState, currentLocation", destinationBg);
        int decision = block.indexOf(
                "MoveDestinationPolicy.battlegroundRetreat(", currentBg);
        int apply = block.indexOf("action.addReasoning(", decision);
        int log = block.indexOf("V37 NO RETREAT: {}", apply);

        assertTrue(actionText >= 0);
        assertTrue(destination > actionText);
        assertTrue(destinationBlueprint > destination);
        assertTrue(destinationBg > destinationBlueprint);
        assertTrue(currentBg > destinationBg);
        assertTrue(decision > currentBg);
        assertTrue(apply > decision);
        assertTrue(log > apply);
    }

    @Test
    public void v135AdapterRetainsLazyTextFriendScanAndVetoOrder()
            throws IOException {
        String block = v37Block(evaluatorSource("rando"));
        int blueprint = block.indexOf(
                "cardToMove.getBlueprint() != null");
        int gameText = block.indexOf(
                "cardToMove.getBlueprint().getGameText()", blueprint);
        int classify = block.indexOf(
                "MoveDestinationPolicy.isSelfMoveToFriend(", gameText);
        int classifyGate = block.indexOf(
                "if (v135IsSelfMoveToFriend)", classify);
        int friendScan = block.indexOf(
                "gameState.getCardsAtLocation(destLoc37)", classifyGate);
        int identitySkip = block.indexOf("pc == cardToMove", friendScan);
        int ownerSkip = block.indexOf(
                "playerId.equals(pc.getOwner())", identitySkip);
        int categorySkip = block.indexOf(
                "CardCategory.CHARACTER", ownerSkip);
        int decision = block.indexOf(
                "MoveDestinationPolicy.companionVeto(", categorySkip);
        int hardVeto = block.indexOf("ladderVetoHard = true", decision);
        int reason = block.indexOf(
                "ladderVetoHardReason = v135Decision.reason()", hardVeto);
        int log = block.indexOf(
                "V135 SELF-MOVE-TO-FRIEND ALONE: {}", reason);

        assertTrue(blueprint >= 0);
        assertTrue(gameText > blueprint);
        assertTrue(classify > gameText);
        assertTrue(classifyGate > classify);
        assertTrue(friendScan > classifyGate);
        assertTrue(identitySkip > friendScan);
        assertTrue(ownerSkip > identitySkip);
        assertTrue(categorySkip > ownerSkip);
        assertTrue(decision > categorySkip);
        assertTrue(hardVeto > decision);
        assertTrue(reason > hardVeto);
        assertTrue(log > reason);
    }

    @Test
    public void protectedMoveMachineryRemainsUntouched()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "// V60: Corridor landspeed receives a bounded -300 objective preference."));
        assertTrue(move.contains("MovePredicates.canWinAt("));
        assertTrue(move.contains("oppWeaponBonusAt("));
        assertTrue(move.contains("ladderFinalize(action)"));
        assertFalse(move.contains("MovePhysicalCardResolver"));
    }

    @Test
    public void policyContainsNoAdapterOrEngineDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "addReasoning", "ladderClaim", "ladderVeto", "logger.",
                "PolicyOperation", "PolicyResult", "DecisionContext",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/MoveEvaluator.java"));
    }

    private static String cardSelectionSource(String bot)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators")
                .resolve("CardSelectionEvaluator.java"));
    }

    private static String moveDestinationBlock(String source) {
        int start = source.indexOf(
                "private List<EvaluatedAction> evaluateMoveDestination(");
        int end = source.indexOf(
                "private List<EvaluatedAction> evaluateStartingLocation(",
                start);
        return source.substring(start, end);
    }

    private static String v37Block(String move) {
        int start = move.indexOf("// === V37: NEVER MOVE");
        int end = move.indexOf("// === V29.12: HUNT DOWN", start);
        return move.substring(start, end);
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveDestinationPolicy.java"));
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
