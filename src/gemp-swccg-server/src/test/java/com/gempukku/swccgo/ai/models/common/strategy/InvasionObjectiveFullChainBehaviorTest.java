package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployPilotShipPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployPlanPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeploySitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidateFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidatePolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Batch Zero behavioral proof for Invasion.
 *
 * Each test joins the typed objective facts to the public policy or planner
 * seam that consumes them. The final deploy test also exercises the real card
 * actions and the real Invasion flip trigger.
 */
public class InvasionObjectiveFullChainBehaviorTest {
    private static final String THRONE_ROOM =
            "Naboo: Theed Palace Throne Room";
    private static final String BLOCKADE_FLAGSHIP = "Blockade Flagship";

    private static final StartingSetup INVASION = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("invasion", "14_113");
                put("naboo", "12_169");
                put("flagship", "14_114");
                put("swamp", "12_171");
                put("racks", "14_96");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Every required starting card has one candidate.
        }
    };

    private VirtualTableScenario scenario() {
        return scenario(false);
    }

    private VirtualTableScenario scenario(boolean includeExtraPilot) {
        HashMap<String, String> darkCards = new HashMap<>() {{
            put("throne", "12_174");
            put("nute", "12_112");
            put("sidious", "208_35");
            put("securityDroid", "12_118");
            put("blaster", "1_317");
        }};
        if (includeExtraPilot) {
            darkCards.put("pilot", "12_111");
        }
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwing", "1_146");
                    put("trooper", "1_28");
                }},
                darkCards,
                20,
                20,
                StartingSetup.DefaultLSGroundLocation,
                INVASION,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void exactThroneRoomPullGetsTheObjectiveBonus() {
        var scn = scenario();
        scn.StartGame();
        ObjectiveAnalyzer analyzer = analyzeRando(scn);

        PolicyResult throne = PullSelectionCandidatePolicy.scoreUnknownPull(
                unknownLocationPull(
                        "throne",
                        THRONE_ROOM,
                        analyzer.isActiveFlipGateLocationTitle(THRONE_ROOM)));
        PolicyResult generator = PullSelectionCandidatePolicy.scoreUnknownPull(
                unknownLocationPull(
                        "generator",
                        "Naboo: Theed Palace Generator",
                        analyzer.isActiveFlipGateLocationTitle(
                                "Naboo: Theed Palace Generator")));

        assertEquals(310.0f, totalDelta(throne), 0.0f);
        assertEquals(10.0f, totalDelta(generator), 0.0f);
        PolicyOperation objectivePull =
                operation(throne, "PULL.OBJECTIVE.FLIP_GATE_SITE");
        assertEquals(300.0f, objectivePull.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, objectivePull.kind());
    }

    @Test
    public void neimoidianAboardFlagshipDoesNotSuppressAThroneActorPull() {
        var scn = scenario(true);
        var flagship = scn.GetDSCard("flagship");
        var throne = scn.GetDSCard("throne");
        var nute = scn.GetDSCard("nute");
        var pilot = scn.GetDSCard("pilot");

        scn.StartGame();
        scn.MoveLocationToTable(throne);
        scn.AttachCardsTo(flagship, nute);

        ObjectiveAnalyzer analyzer = analyzeRando(scn);
        assertSame(flagship, nute.getAttachedTo());
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                        .REQUIRED_ACTOR,
                analyzer.classifyPreFlipProgressCandidate(
                        scn.game(), DS, pilot));
    }

    @Test
    public void nineForceSidiousUploadReplansNuteToThroneAndReallyFlips()
            throws Exception {
        var scn = scenario();
        var invasion = scn.GetDSCard("invasion");
        var throne = scn.GetDSCard("throne");
        var nute = scn.GetDSCard("nute");
        var sidious = scn.GetDSCard("sidious");

        scn.MoveCardsToDSHand(sidious);
        scn.StartGame();
        scn.MoveLocationToTable(throne);
        scn.MoveCardsToBottomOfDSReserveDeck(nute);

        scn.DSActivateMaxForceAndPass();
        int missingForce = 9 - scn.GetDSForcePileCount();
        assertTrue("Natural activation unexpectedly exceeded the nine-Force boundary",
                missingForce >= 0);
        scn.DSActivateForceCheat(missingForce);
        assertEquals(9, scn.GetDSForcePileCount());

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        randoAnalyzer.analyze(scn.game(), DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer();
        chosenAnalyzer.analyze(scn.game(), DS, Side.DARK);

        assertEquals(Integer.valueOf(3),
                randoAnalyzer.getFlipGateActorEnablerFutureDeployCost(
                        scn.game(), DS, sidious));
        assertTrue(randoAnalyzer.isFlipGateActorUploadIntoHandAction(
                scn.game(), DS, sidious,
                "Take card into hand from Reserve Deck"));

        var randoPlanner =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner();
        randoPlanner.setObjectiveAnalyzer(randoAnalyzer);
        var randoInitial =
                randoPlanner.createPlan(scn.game(), DS, Side.DARK);
        var chosenPlanner =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner();
        chosenPlanner.setObjectiveAnalyzer(chosenAnalyzer);
        var chosenInitial =
                chosenPlanner.createPlan(scn.game(), DS, Side.DARK);

        assertEquals(1, randoInitial.getInstructions().size());
        assertEquals(1, chosenInitial.getInstructions().size());
        assertEquals("Lord Sidious",
                randoInitial.getInstructions().get(0).getCardName());
        assertEquals("Lord Sidious",
                chosenInitial.getInstructions().get(0).getCardName());
        assertEquals(THRONE_ROOM,
                randoInitial.getInstructions().get(0).getTargetLocationName());
        assertEquals(THRONE_ROOM,
                chosenInitial.getInstructions().get(0).getTargetLocationName());
        assertEquals(6,
                randoInitial.getInstructions().get(0).getDeployCost());

        scn.PassControlActions();
        scn.DSDeployCardAndPassResponses(sidious, throne);
        assertEquals(3, scn.GetDSForcePileCount());
        assertFalse(invasion.isFlipped());

        scn.LSPass();
        scn.DSUseCardAction(sidious, "Take card into hand");
        scn.DSChooseCard(nute);
        scn.PassAllResponses();
        assertSame(Zone.HAND, nute.getZone());
        assertEquals("Sidious upload must be free", 3,
                scn.GetDSForcePileCount());

        var randoRefreshed =
                randoPlanner.createPlan(scn.game(), DS, Side.DARK);
        var chosenRefreshed =
                chosenPlanner.createPlan(scn.game(), DS, Side.DARK);
        assertEquals(1, randoRefreshed.getInstructions().size());
        assertEquals(1, chosenRefreshed.getInstructions().size());
        assertEquals("Nute Gunray",
                randoRefreshed.getInstructions().get(0).getCardName());
        assertEquals("Nute Gunray",
                chosenRefreshed.getInstructions().get(0).getCardName());
        assertEquals(THRONE_ROOM,
                randoRefreshed.getInstructions().get(0)
                        .getTargetLocationName());
        assertEquals(THRONE_ROOM,
                chosenRefreshed.getInstructions().get(0)
                        .getTargetLocationName());
        assertEquals(3,
                randoRefreshed.getInstructions().get(0).getDeployCost());

        PolicyResult plannedThrone =
                DeployPlanPolicy.evaluateDestinationTarget(
                        new DeployPlanPolicy.DestinationTargetFacts(
                                "throne-destination", true, true,
                                randoRefreshed.getInstructions().get(0)
                                        .getTargetLocationName()));
        PolicyOperation throneMatch =
                operation(plannedThrone, "deploy-plan-target-match");
        assertEquals(PolicyOperationKind.ADD, throneMatch.kind());
        assertEquals(200.0f, throneMatch.delta(), 0.0f);

        PolicyResult offeredFlagship =
                DeployPlanPolicy.evaluateDestinationTarget(
                        new DeployPlanPolicy.DestinationTargetFacts(
                                "blockade-flagship-destination",
                                false, true,
                                randoRefreshed.getInstructions().get(0)
                                        .getTargetLocationName()));
        assertEquals(-100.0f,
                operation(offeredFlagship,
                        "deploy-plan-target-other").delta(), 0.0f);
        assertEquals(PolicyOperationKind.DEFER,
                operation(offeredFlagship,
                        "deploy-plan-target-defer").kind());

        PolicyOperation v193 = operation(
                DeploySitingPolicy.evaluateDirect(
                        new DeploySitingPolicy.Facts(
                                "neimoidian-throne",
                                "Neimoidian Pilot", THRONE_ROOM,
                                false,
                                DeploySitingPolicy.FormationState.ALLOW,
                                "", 0.0f,
                                true, true,
                                randoAnalyzer.getActivePlaybook()
                                        .weights.deployFlipGateSite,
                                "Neimoidian at " + THRONE_ROOM,
                                false, 0.0f, 0.0f)),
                "V193");
        PolicyOperation v121 = operation(
                DeployPilotShipPolicy.evaluateObjectivePilotDestination(
                        new DeployPilotShipPolicy.ObjectivePilotDestinationFacts(
                                "neimoidian-throne",
                                true, BLOCKADE_FLAGSHIP,
                                THRONE_ROOM, false)),
                "V121");
        assertEquals(1600.0f, v193.delta(), 0.0f);
        assertEquals(-1500.0f, v121.delta(), 0.0f);
        assertEquals(300.0f,
                throneMatch.delta() + v193.delta() + v121.delta(),
                0.0f);

        scn.LSPass();
        scn.DSDeployCardAndPassResponses(nute, throne);
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The real Invasion card must flip after the funded Nute deploy",
                invasion.isFlipped());
    }

    @Test
    public void soleBlockadeFlagshipControlAtNabooIsHeldBeforeFlip() {
        var scn = scenario();
        var naboo = scn.GetDSCard("naboo");
        var flagship = scn.GetDSCard("flagship");
        scn.StartGame();
        ObjectiveAnalyzer analyzer = analyzeRando(scn);

        var gameState = scn.game().getGameState();
        var modifiers = scn.game().getModifiersQuerying();
        var flagshipLocation = modifiers.getLocationThatCardIsPresentAt(
                gameState, flagship);
        assertSame(naboo, flagshipLocation);

        boolean requiredControl =
                analyzer.isPreFlipPlainControlRequirementLocation(
                        scn.game(), DS, naboo);
        boolean controls = modifiers.controlsLocation(
                gameState, naboo, DS,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE);
        boolean soleControlSource =
                analyzer.isSoleControlSourceAtRequiredLocation(
                        scn.game(), DS, flagship, naboo);

        MoveObjectiveGateHoldPolicy.Evaluation hold =
                MoveObjectiveGateHoldPolicy.evaluateRequiredControl(
                        requiredControl,
                        flagshipLocation == naboo,
                        controls,
                        soleControlSource);
        assertEquals(
                MoveObjectiveGateHoldPolicy.Branch.HOLD_LAST_CONTROL_SOURCE,
                hold.branch());
        assertTrue(hold.hardVeto());

        MoveObjectiveGateHoldPolicy.Evaluation unknown =
                MoveObjectiveGateHoldPolicy.evaluateRequiredControl(
                        requiredControl, true, controls, false);
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE,
                unknown.branch());
        assertFalse(unknown.hardVeto());
    }

    @Test
    public void battleBonusTargetsOnlyTheExactSafeMissingRequirement() {
        var scn = scenario();
        var throne = scn.GetDSCard("throne");
        var swamp = scn.GetDSCard("swamp");
        scn.StartGame();
        scn.MoveLocationToTable(throne);
        ObjectiveAnalyzer analyzer = analyzeRando(scn);

        boolean exactThrone =
                analyzer.isPreFlipFlipRequirementLocation(
                        scn.game(), DS, throne);
        boolean throneMissing =
                analyzer.isMissingPreFlipRequirementAt(
                        scn.game(), DS, throne);
        PolicyResult safeContest = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-throne",
                        exactThrone, throneMissing, true,
                        false, true,
                        0.0f, 5, 7.0f, 5.0f));
        PolicyOperation contest = operation(
                safeContest,
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_RULE_ID);
        assertEquals(ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                contest.delta(), 0.0f);

        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-swamp",
                        analyzer.isPreFlipFlipRequirementLocation(
                                scn.game(), DS, swamp),
                        true, true, false, true,
                        0.0f, 5, 7.0f, 5.0f))
                .operations().isEmpty());
        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-suicide",
                        exactThrone, throneMissing, true,
                        false, true,
                        0.0f, 10, 3.0f, 7.0f))
                .operations().isEmpty());
        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-breaks-formation",
                        exactThrone, throneMissing, true,
                        true, true,
                        0.0f, 10, 8.0f, 5.0f))
                .operations().isEmpty());
    }

    @Test
    public void forceLossProtectsAnalyzedNuteWhileDisposableAlternativeRemains() {
        var scn = scenario();
        var nute = scn.GetDSCard("nute");
        var blaster = scn.GetDSCard("blaster");
        scn.MoveCardsToDSHand(nute, blaster);
        scn.StartGame();
        ObjectiveAnalyzer analyzer = analyzeRando(scn);

        boolean requiredActor = !analyzer.isFlipped()
                && analyzer.matchesFlipGateActorRequirement(
                        scn.game(), DS, nute);
        assertTrue(requiredActor);

        ForceLossFacts.DecisionFacts decision =
                ForceLossFacts.readDecision(
                        scn.game().getGameState(), DS, 1);
        PolicyResult nuteLoss = ForceLossPolicy.score(
                Integer.toString(nute.getCardId()),
                ForceLossPolicy.Route.STANDALONE,
                decision,
                ForceLossFacts.readCandidate(
                        scn.game().getGameState(), DS, nute),
                new ForceLossPolicy.ObjectiveFlags(
                        false, false, requiredActor, false));
        PolicyResult blasterLoss = ForceLossPolicy.score(
                Integer.toString(blaster.getCardId()),
                ForceLossPolicy.Route.STANDALONE,
                decision,
                ForceLossFacts.readCandidate(
                        scn.game().getGameState(), DS, blaster),
                ForceLossPolicy.ObjectiveFlags.none());

        PolicyOperation objectiveHold =
                operation(nuteLoss, "V21-objective");
        assertEquals(-9999.0f, objectiveHold.delta(), 0.0f);
        assertFalse(hasOperation(blasterLoss, "V21-objective"));
        assertTrue(totalDelta(blasterLoss) > totalDelta(nuteLoss));
    }

    @Test
    public void battleForfeitPreservesTheLastActorAndBuddyWhenAnotherLossExists() {
        var scn = scenario();
        var flagship = scn.GetDSCard("flagship");
        var throne = scn.GetDSCard("throne");
        var nute = scn.GetDSCard("nute");
        var sidious = scn.GetDSCard("sidious");
        var blaster = scn.GetDSCard("blaster");
        var securityDroid = scn.GetDSCard("securityDroid");

        scn.StartGame();
        scn.MoveLocationToTable(throne);
        scn.MoveOutOfPlay(flagship);
        scn.MoveCardsToLocation(throne, nute, sidious);
        scn.AttachCardsTo(sidious, blaster);
        ObjectiveAnalyzer analyzer = analyzeRando(scn);

        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), DS, nute));
        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_BUDDY,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), DS, sidious));
        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), DS, blaster));

        List<String> offeredLosses = List.of(
                Integer.toString(nute.getCardId()),
                Integer.toString(sidious.getCardId()),
                Integer.toString(blaster.getCardId()));
        BattleForfeitFacts.FlipGateFormationSelectionFacts selection =
                BattleForfeitFacts.readFlipGateFormationSelection(
                        offeredLosses,
                        scn.game().getGameState(),
                        scn.game(), DS, analyzer,
                        false, 1);
        assertTrue(selection.hasUnprotectedLegalAlternative());

        PolicyOperation actorHold = onlyOperation(
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        offeredLosses.get(0),
                        selection.roleFor(offeredLosses.get(0)),
                        selection.hasUnprotectedLegalAlternative()));
        PolicyOperation buddyHold = onlyOperation(
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        offeredLosses.get(1),
                        selection.roleFor(offeredLosses.get(1)),
                        selection.hasUnprotectedLegalAlternative()));
        assertEquals("BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                actorHold.ruleArmId().id());
        assertEquals("BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                buddyHold.ruleArmId().id());
        assertEquals(-9999.0f, actorHold.delta(), 0.0f);
        assertEquals(-9999.0f, buddyHold.delta(), 0.0f);

        BattleForfeitFacts.FlipGateFormationSelectionFacts unavoidable =
                BattleForfeitFacts.readFlipGateFormationSelection(
                        offeredLosses.subList(0, 2),
                        scn.game().getGameState(),
                        scn.game(), DS, analyzer,
                        false, 1);
        assertFalse(unavoidable.hasUnprotectedLegalAlternative());
        assertTrue(BattleForfeitPolicy.scoreFlipGateFormationProtection(
                offeredLosses.get(0),
                unavoidable.roleFor(offeredLosses.get(0)),
                unavoidable.hasUnprotectedLegalAlternative())
                .operations().isEmpty());

        scn.MoveCardsToLocation(throne, securityDroid);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                        scn.game(), DS, sidious));
    }

    private static ObjectiveAnalyzer analyzeRando(
            VirtualTableScenario scn) {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), DS, Side.DARK);
        return analyzer;
    }

    private static PullSelectionCandidateFacts.UnknownPull unknownLocationPull(
            String actionId, String title, boolean activeObjectiveFlipGate) {
        return new PullSelectionCandidateFacts.UnknownPull(
                actionId, title, CardCategory.LOCATION,
                true, false, activeObjectiveFlipGate,
                PullSelectionCandidateFacts.CloudCityMode.NONE,
                PullSelectionCandidateFacts.CloudCitySite.OTHER,
                null,
                PullSelectionCandidateFacts.UnknownAmsdState.NONE);
    }

    private static float totalDelta(PolicyResult result) {
        return result.operations().stream()
                .map(PolicyOperation::delta)
                .reduce(0.0f, Float::sum);
    }

    private static PolicyOperation operation(
            PolicyResult result, String ruleId) {
        return result.operations().stream()
                .filter(operation -> operation.ruleArmId().id()
                        .equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing policy operation " + ruleId));
    }

    private static boolean hasOperation(
            PolicyResult result, String ruleId) {
        return result.operations().stream()
                .anyMatch(operation -> operation.ruleArmId().id()
                        .equals(ruleId));
    }

    private static PolicyOperation onlyOperation(PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }
}
