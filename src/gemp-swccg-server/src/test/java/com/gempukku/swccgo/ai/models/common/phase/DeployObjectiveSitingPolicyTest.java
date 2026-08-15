package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveSitingPolicyTest {
    @Test
    public void objectiveContributionsShareOnePositiveBudgetInRuleOrder() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, true, true, 150.0f,
                true, true, true, true,
                true, false, false, "galactic senate",
                0.0f, 0.0f));

        assertEquals(3, operations.size());
        assertOperation(operations.get(0), "V22-objective-location", 300.0f);
        assertOperation(operations.get(1), "V88-CS", 0.0f);
        assertOperation(operations.get(2), "V88-text-named", 0.0f);
    }

    @Test
    public void wrongSiteSenatorUsesBoundedPreferencePenalty() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, true, false, 0.0f,
                true, true, true, false,
                false, false, false, "landing platform",
                0.0f, 0.0f));

        assertEquals(1, operations.size());
        assertOperation(operations.get(0), "V88-CS", -300.0f);
    }

    @Test
    public void undercoverSpyDoesNotReceiveObjectivePresenceScore() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", true, true, true, 500.0f,
                false, false, true, false,
                false, false, false, "audience chamber",
                0.0f, 0.0f));
        assertTrue(operations.isEmpty());
    }

    @Test
    public void negativeTextUsesBoundedPenaltyButDoomedPositiveIsWithheld() {
        List<PolicyOperation> negative = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, false,
                true, true, true, "audience chamber",
                0.0f, 0.0f));
        assertEquals(1, negative.size());
        assertOperation(negative.get(0), "V88-text-named", -300.0f);

        List<PolicyOperation> doomed = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, false,
                true, false, true, "audience chamber",
                0.0f, 0.0f));
        assertTrue(doomed.isEmpty());
    }

    @Test
    public void senateGuardPenalizesOnlyWhenDefenseIsNotNeeded() {
        List<PolicyOperation> blocked = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, true,
                false, false, false, "galactic senate",
                4.0f, 4.0f));
        assertEquals(1, blocked.size());
        assertOperation(blocked.get(0), "V99-CS", -300.0f);

        List<PolicyOperation> defense = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, true,
                false, false, false, "galactic senate",
                5.0f, 4.0f));
        assertTrue(defense.isEmpty());
    }

    @Test
    public void directObjectivePrioritiesKeepDistinctRuleArmsAtSharedCeiling() {
        PolicyOperation army = DeployObjectiveSitingPolicy.scoreCloudCityArmy(
                new DeployObjectiveSitingPolicy.CloudCityArmyFacts(
                        "a", "Cloud City: Downtown Plaza"))
                .operations().get(0);
        PolicyOperation objective = DeployObjectiveSitingPolicy.scoreObjectiveFirst(
                new DeployObjectiveSitingPolicy.ObjectiveFirstFacts(
                        "a", "Cloud City: Downtown Plaza"))
                .operations().get(0);
        PolicyOperation key = DeployObjectiveSitingPolicy.scoreKeyCharacter(
                new DeployObjectiveSitingPolicy.KeyCharacterFacts(
                        "a", "Darth Vader"))
                .operations().get(0);

        assertOperation(army, "V51-CC-ARMY", 300.0f);
        assertOperation(objective, "V51-OBJ-FIRST", 300.0f);
        assertOperation(key, "V67ak", 300.0f);
    }

    @Test
    public void countedObjectiveDeployScoresOnlyMissingLocationProgress() {
        PolicyOperation progress =
                DeployObjectiveSitingPolicy.scoreCountedObjectiveProgress(
                        "a", true).operations().get(0);
        PolicyResult neutral =
                DeployObjectiveSitingPolicy.scoreCountedObjectiveProgress(
                        "a", false);

        assertOperation(progress,
                "DEPLOY.OBJECTIVE.COUNTED_REQUIRED_LOCATION", 300.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT, progress.domainId());
        assertEquals(TraceOutputKind.BANDED, progress.outputKind());
        assertEquals("DEPLOY_COUNTED_OBJECTIVE_PROGRESS_POLICY",
                neutral.producerId());
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void objectiveDeployPreferencesShareTheSameCeiling() {
        float progress =
                DeployObjectiveSitingPolicy.scoreCountedObjectiveProgress(
                        "a", true).operations().get(0).delta();
        float genericObjective =
                DeployObjectiveSitingPolicy.scoreObjectiveFirst(
                        new DeployObjectiveSitingPolicy.ObjectiveFirstFacts(
                                "a", "Ralltiir: Spaceport Prefect's Office"))
                        .operations().get(0).delta();
        float namedKeyCharacter =
                DeployObjectiveSitingPolicy.scoreKeyCharacter(
                        new DeployObjectiveSitingPolicy.KeyCharacterFacts(
                                "a", "Admiral Piett"))
                        .operations().get(0).delta();

        assertEquals(300.0f, progress, 0.0f);
        assertEquals(progress, genericObjective, 0.0f);
        assertEquals(progress, namedKeyCharacter, 0.0f);
    }

    @Test
    public void threeSiteCompletionUsesBoundedPreferenceOnlyAtCompletion() {
        PolicyOperation completion = DeployObjectiveSitingPolicy
                .scoreThreeSiteObjectiveCompletion("a", true)
                .operations().get(0);
        PolicyResult neutral = DeployObjectiveSitingPolicy
                .scoreThreeSiteObjectiveCompletion("a", false);
        assertOperation(completion,
                "DEPLOY.OBJECTIVE.THREE_SITE_COMPLETION", 300.0f);
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void shadowCollectiveCompletionUsesItsOwnTwoSiteRuleIdentity() {
        PolicyOperation completion = DeployObjectiveSitingPolicy
                .scoreShadowCollectiveRouteCompletion("a", true)
                .operations().get(0);
        PolicyResult neutral = DeployObjectiveSitingPolicy
                .scoreShadowCollectiveRouteCompletion("a", false);

        assertOperation(completion,
                "DEPLOY.OBJECTIVE.SHADOW_COLLECTIVE.ROUTE_COMPLETION",
                300.0f);
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void typedActorStagingUsesBoundedPreferenceOnlyOnItsRoute() {
        PolicyOperation staging =
                DeployObjectiveSitingPolicy.scoreActorRouteStaging(
                        "a", true, "Padme Naberrie",
                        "Naboo: Theed Palace Courtyard")
                        .operations().get(0);
        PolicyResult neutral =
                DeployObjectiveSitingPolicy.scoreActorRouteStaging(
                        "a", false, "Captain Panaka",
                        "Naboo: Theed Palace Courtyard");
        assertOperation(staging,
                "DEPLOY.OBJECTIVE.ACTOR_ROUTE_STAGING", 300.0f);
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void isbRouteCompletionUsesBoundedPreferenceOnlyAtCompletion() {
        PolicyOperation completion = DeployObjectiveSitingPolicy
                .scoreIsbRouteCompletion("a", true)
                .operations().get(0);
        PolicyResult neutral = DeployObjectiveSitingPolicy
                .scoreIsbRouteCompletion("a", false);

        assertOperation(completion,
                "OBJECTIVE.ISB.ROUTE_COMPLETION", 300.0f);
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void terminalObjectiveExposureIsAnExactHardVeto() {
        PolicyResult blocked =
                DeployObjectiveSitingPolicy
                    .blockTerminalObjectiveExposure(
                        "a", true);
        PolicyResult unrelated =
                DeployObjectiveSitingPolicy
                    .blockTerminalObjectiveExposure(
                        "a", false);

        assertEquals(1, blocked.operations().size());
        PolicyOperation operation =
                blocked.operations().get(0);
        assertEquals(
                "DEPLOY.OBJECTIVE.TERMINAL_ACTOR_EXPOSURE",
                operation.ruleArmId().id());
        assertEquals(
                PolicyOperationKind.HARD_VETO,
                operation.kind());
        assertEquals(
                TraceDomainId.DEPLOY_SITING,
                operation.domainId());
        assertTrue(unrelated.operations().isEmpty());
    }

    @Test
    public void iWantThatMapSelfBlockerIsABoundedObjectivePenalty() {
        PolicyResult blocked = DeployObjectiveSitingPolicy
                .blockIWantThatMapSelfBlocker("a", true);
        PolicyResult unrelated = DeployObjectiveSitingPolicy
                .blockIWantThatMapSelfBlocker("a", false);

        assertEquals(1, blocked.operations().size());
        PolicyOperation operation = blocked.operations().get(0);
        assertEquals("OBJECTIVE.I_WANT_THAT_MAP.SELF_BLOCKER",
                operation.ruleArmId().id());
        assertEquals(PolicyOperationKind.ADD,
                operation.kind());
        assertEquals(-300.0f, operation.delta(), 0.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                operation.domainId());
        assertEquals(TraceOutputKind.BANDED,
                operation.outputKind());
        assertTrue(unrelated.operations().isEmpty());
    }

    @Test
    public void postFlipPayoffRolesKeepDistinctRulesAtSharedCeiling() {
        PolicyOperation primary =
                DeployObjectiveSitingPolicy
                    .scorePostFlipObjectivePayoff(
                        "a",
                        ObjectiveAnalyzer
                            .ObjectivePostFlipPayoffRole.PRIMARY)
                    .operations().get(0);
        PolicyOperation secondary =
                DeployObjectiveSitingPolicy
                    .scorePostFlipObjectivePayoff(
                        "a",
                        ObjectiveAnalyzer
                            .ObjectivePostFlipPayoffRole.SECONDARY)
                    .operations().get(0);

        assertOperation(
                primary,
                "DEPLOY.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF",
                300.0f);
        assertOperation(
                secondary,
                "DEPLOY.OBJECTIVE.POST_FLIP_SECONDARY_PAYOFF",
                300.0f);
        assertEquals(primary.delta(), secondary.delta(), 0.0f);
        assertTrue(DeployObjectiveSitingPolicy
                .scorePostFlipObjectivePayoff(
                    "a",
                    ObjectiveAnalyzer
                        .ObjectivePostFlipPayoffRole.NONE)
                .operations().isEmpty());
    }

    @Test
    public void firstOrderDrainPairScoresOnlyTheThresholdCompletion() {
        PolicyResult completes =
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsDrainPair(
                        "a", true);
        PolicyResult neutral =
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsDrainPair(
                        "a", false);

        assertOperation(
                completes.operations().get(0),
                "DEPLOY.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR",
                300.0f);
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void firstOrderRouteCrewScoresOnlyDirectBoarding() {
        PolicyResult boardsChaseShip =
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsRouteCrew(
                        "a", true);
        PolicyResult neutral =
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsRouteCrew(
                        "a", false);

        assertEquals(
                "DEPLOY_FIRST_ORDER_ROUTE_CREW_POLICY",
                boardsChaseShip.producerId());
        assertOperation(
                boardsChaseShip.operations().get(0),
                "DEPLOY.OBJECTIVE.FIRST_ORDER_ROUTE_CREW",
                300.0f);
        assertEquals(
                TraceDomainId.OBJECTIVE_INTENT,
                boardsChaseShip.operations().get(0)
                    .domainId());
        assertEquals(
                PolicyOperationKind.ADD,
                boardsChaseShip.operations().get(0)
                    .kind());
        assertEquals(
                "DEPLOY_FIRST_ORDER_ROUTE_CREW_POLICY",
                neutral.producerId());
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void firstOrderNavyRouteScoresOnlyTheExecutableChain() {
        PolicyResult parent =
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsNavyRouteAction(
                        "a", true);
        PolicyResult candidate =
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsNavyRouteCandidate(
                        "b", true);

        assertOperation(
                parent.operations().get(0),
                "DEPLOY.OBJECTIVE.FIRST_ORDER_NAVY_ROUTE",
                300.0f);
        assertOperation(
                candidate.operations().get(0),
                "DEPLOY.OBJECTIVE.FIRST_ORDER_NAVY_ROUTE_CANDIDATE",
                300.0f);
        assertTrue(
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsNavyRouteAction(
                        "a", false)
                    .operations().isEmpty());
        assertTrue(
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsNavyRouteCandidate(
                        "b", false)
                    .operations().isEmpty());
    }

    @Test
    public void firstOrderPreFlipGroundIsABoundedPreferencePenalty() {
        PolicyResult penalized =
                DeployObjectiveSitingPolicy
                    .blockFirstOrderReignsPreFlipGround(
                        "a", true);
        PolicyResult neutral =
                DeployObjectiveSitingPolicy
                    .blockFirstOrderReignsPreFlipGround(
                        "a", false);

        assertEquals(
                "DEPLOY_FIRST_ORDER_PRE_FLIP_GROUND_POLICY",
                penalized.producerId());
        assertEquals(1, penalized.operations().size());
        PolicyOperation operation =
                penalized.operations().get(0);
        assertEquals(
                "DEPLOY.OBJECTIVE.FIRST_ORDER_PRE_FLIP_GROUND_HOLD",
                operation.ruleArmId().id());
        assertEquals(
                PolicyOperationKind.ADD,
                operation.kind());
        assertEquals(
                TraceDomainId.OBJECTIVE_INTENT,
                operation.domainId());
        assertEquals(
                TraceOutputKind.BANDED,
                operation.outputKind());
        assertEquals(-300.0f, operation.delta(), 0.0f);
        assertEquals(
                "DEPLOY_FIRST_ORDER_PRE_FLIP_GROUND_POLICY",
                neutral.producerId());
        assertTrue(neutral.operations().isEmpty());
    }

    @Test
    public void cloudCityEngineKeepsBranchesWithinObjectiveBudget() {
        DeployObjectiveSitingPolicy.CloudCityEngineEvaluation blocked =
                DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                        new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                "a", "Cloud City Occupation", false, false));
        DeployObjectiveSitingPolicy.CloudCityEngineEvaluation priority =
                DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                        new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                "a", "Cloud City Occupation", true, false));
        DeployObjectiveSitingPolicy.CloudCityEngineEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                        new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                "a", "Cloud City Occupation", true, true));

        assertEquals(DeployObjectiveSitingPolicy.CloudCityEngineOutcome.BLOCKED,
                blocked.outcome());
        assertHardVeto(blocked.result().operations().get(0), "V22.7");
        assertEquals(DeployObjectiveSitingPolicy.CloudCityEngineOutcome.ENGINE_PRIORITY,
                priority.outcome());
        assertOperation(priority.result().operations().get(0), "V24", 300.0f);
        assertEquals(DeployObjectiveSitingPolicy.CloudCityEngineOutcome.SAFE,
                safe.outcome());
        assertOperation(safe.result().operations().get(0), "V22.7", 300.0f);
    }

    @Test
    public void gherantKeepsExecutorSitePriority() {
        PolicyOperation operation = DeployObjectiveSitingPolicy.scoreGherant(
                new DeployObjectiveSitingPolicy.GherantFacts("a"))
                .operations().get(0);
        assertOperation(operation, "V24.1", 300.0f);
    }

    @Test
    public void objectiveCriticalSystemContestKeepsStrictPowerGateAndBoundedScore() {
        DeployObjectiveSitingPolicy.MustContestEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateMustContest(
                        new DeployObjectiveSitingPolicy.MustContestFacts(
                                "a", "Bespin", 1.0f, 1.25f));

        assertEquals(DeployObjectiveSitingPolicy.MustContestOutcome.MUST_CONTEST,
                evaluation.outcome());
        assertEquals("DEPLOY_MUST_CONTEST_POLICY",
                evaluation.result().producerId());
        PolicyOperation operation = evaluation.result().operations().get(0);
        assertEquals("a", operation.actionId());
        assertOperation(operation, "V22.7-MUST-CONTEST", 300.0f);
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceDomainId.OBJECTIVE_INTENT, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(
                "V22.7 CONTEST PREFERENCE: Opponent controls objective-critical Bespin; prefer deploying a ship to contest (+300)",
                operation.reason());
    }

    @Test
    public void objectiveCriticalContestStaysSilentWithoutEnemyControl() {
        for (DeployObjectiveSitingPolicy.MustContestFacts facts :
                new DeployObjectiveSitingPolicy.MustContestFacts[]{
                        new DeployObjectiveSitingPolicy.MustContestFacts(
                                "a", "Bespin", 0.0f, 0.0f),
                        new DeployObjectiveSitingPolicy.MustContestFacts(
                                "a", "Bespin", 0.0f, -1.0f),
                        new DeployObjectiveSitingPolicy.MustContestFacts(
                                "a", "Bespin", 3.0f, 3.0f),
                        new DeployObjectiveSitingPolicy.MustContestFacts(
                                "a", "Bespin", 4.0f, 3.0f)}) {
            DeployObjectiveSitingPolicy.MustContestEvaluation evaluation =
                    DeployObjectiveSitingPolicy.evaluateMustContest(facts);
            assertEquals(DeployObjectiveSitingPolicy.MustContestOutcome.NONE,
                    evaluation.outcome());
            assertTrue(evaluation.result().operations().isEmpty());
        }
    }

    @Test
    public void landoDiningRoomUsesBoundedObjectivePreference() {
        DeployObjectiveSitingPolicy.LandoDestinationEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateLandoDestination(
                        new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                                "a", true, "Cloud City: Dining Room"));

        assertEquals(DeployObjectiveSitingPolicy.LandoDestinationOutcome.DINING_ROOM,
                evaluation.outcome());
        assertEquals("DEPLOY_LANDO_DESTINATION_POLICY",
                evaluation.result().producerId());
        assertEquals(1, evaluation.result().operations().size());
        PolicyOperation operation = evaluation.result().operations().get(0);
        assertEquals("a", operation.actionId());
        assertOperation(operation, "V24.10-LANDO-DINING", 300.0f);
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceDomainId.OBJECTIVE_INTENT, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(
                "V24.10 LANDO TO DINING ROOM: Optimal deploy — establishes occupation, can move to other sites!",
                operation.reason());
    }

    @Test
    public void landoOtherCloudCitySitesKeepMildPenalty() {
        for (String title : new String[]{
                "Cloud City: Downtown Plaza", "Upper Walkway",
                "Carbonite Chamber", "Security Tower",
                "East Platform", "Lower Corridor"}) {
            DeployObjectiveSitingPolicy.LandoDestinationEvaluation evaluation =
                    DeployObjectiveSitingPolicy.evaluateLandoDestination(
                            new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                                    "a", true, title));
            assertEquals(title,
                    DeployObjectiveSitingPolicy.LandoDestinationOutcome.OTHER_CLOUD_CITY_SITE,
                    evaluation.outcome());
            PolicyOperation operation = evaluation.result().operations().get(0);
            assertOperation(operation, "V24.10-LANDO-CC", -50.0f);
            assertEquals(
                    "V24.10 LANDO: CC site but not Dining Room — Lando can move here later, deploy to Dining Room first!",
                    operation.reason());
        }
    }

    @Test
    public void landoDestinationNoMatchAndNonLandoStaySilent() {
        for (DeployObjectiveSitingPolicy.LandoDestinationFacts facts :
                new DeployObjectiveSitingPolicy.LandoDestinationFacts[]{
                        new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                                "a", false, "Cloud City: Dining Room"),
                        new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                                "a", true, "Tatooine: Cantina"),
                        new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                                "a", true, null)}) {
            DeployObjectiveSitingPolicy.LandoDestinationEvaluation evaluation =
                    DeployObjectiveSitingPolicy.evaluateLandoDestination(facts);
            assertEquals(DeployObjectiveSitingPolicy.LandoDestinationOutcome.NONE,
                    evaluation.outcome());
            assertTrue(evaluation.result().operations().isEmpty());
        }
    }

    @Test
    public void landoBranchKeepsPrecedenceWhenBothNamesAppear() {
        DeployObjectiveSitingPolicy.LandoLobotEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", true, true, true));
        DeployObjectiveSitingPolicy.LandoLobotEvaluation blocked =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", true, true, false));

        assertEquals(DeployObjectiveSitingPolicy.LandoLobotOutcome.LANDO_SAFE,
                safe.outcome());
        assertOperation(safe.result().operations().get(0),
                "V29.2-LANDO", 300.0f);
        assertEquals(DeployObjectiveSitingPolicy.LandoLobotOutcome.LANDO_BLOCKED,
                blocked.outcome());
        assertHardVeto(blocked.result().operations().get(0),
                "V47-LANDO");
    }

    @Test
    public void lobotKeepsSafeScoreAndSoloSafetyVeto() {
        DeployObjectiveSitingPolicy.LandoLobotEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", false, true, true));
        DeployObjectiveSitingPolicy.LandoLobotEvaluation blocked =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", false, true, false));

        assertOperation(safe.result().operations().get(0),
                "V29.2-LOBOT", 300.0f);
        assertHardVeto(blocked.result().operations().get(0),
                "V47-LOBOT");
    }

    @Test
    public void preFlipDefenseCapsHuntDownTiersAtObjectiveCeiling() {
        assertFlipScore(300.0f, 5, false, false);
        assertFlipScore(300.0f, 5, true, false);
        assertFlipScore(300.0f, 3, true, false);
        assertFlipScore(300.0f, 3, true, true);
    }

    @Test
    public void preFlipSpreadAndPostFlipHoldRemainExclusive() {
        DeployObjectiveSitingPolicy.FlipSitingEvaluation spread =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", false, 0, false, false,
                                1, 2, false, false, false));
        DeployObjectiveSitingPolicy.FlipSitingEvaluation hold =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", true, 0, false, false,
                                3, 0, false, true, true));

        assertEquals(DeployObjectiveSitingPolicy.FlipSitingOutcome.PREFLIP_SPREAD,
                spread.outcome());
        assertOperation(spread.result().operations().get(0),
                "V31-PREFLIP", -50.0f);
        assertEquals(DeployObjectiveSitingPolicy.FlipSitingOutcome.POSTFLIP_HOLD,
                hold.outcome());
        assertOperation(hold.result().operations().get(0),
                "V31-POSTFLIP", 300.0f);
    }

    @Test
    public void exactStructuredPostFlipHoldLocationsIgnoreRelativePower() {
        Set<String> exact = new LinkedHashSet<>(List.of(
                "Naboo",
                "Naboo: Theed Palace Throne Room"));
        Map<String, Float> powers = linkedPowers(
                "Naboo: Swamp", 20.0f,
                "Naboo", 2.0f,
                "Naboo: Theed Palace Throne Room", 1.0f);

        assertEquals(exact,
                DeployObjectiveSitingPolicy.selectPostFlipHoldLocations(
                        exact, powers));
    }

    @Test
    public void emptyStructuredSetPreservesLegacyStrongestTwoSelection() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 6.0f,
                "Site B", 6.0f,
                "Site C", 9.0f);

        assertEquals(new LinkedHashSet<>(List.of("Site C", "Site A")),
                DeployObjectiveSitingPolicy.selectPostFlipHoldLocations(
                        Set.of(), powers));
    }

    @Test
    public void authoritativeEmptyStructuredSetDoesNotFallBackToStrongestTwo() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 6.0f,
                "Site B", 6.0f,
                "Site C", 9.0f);

        assertTrue(DeployObjectiveSitingPolicy.selectPostFlipHoldLocations(
                true, Set.of(), powers).isEmpty());
    }

    @Test
    public void postFlipThirdObjectiveLocationStaysNeutral() {
        DeployObjectiveSitingPolicy.FlipSitingEvaluation neutral =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", true, 0, false, false,
                                3, 0, false, false, true));

        assertEquals(
                DeployObjectiveSitingPolicy.FlipSitingOutcome.POSTFLIP_THIRD_NEUTRAL,
                neutral.outcome());
        assertOperation(neutral.result().operations().get(0),
                "V40-POSTFLIP", 0.0f);
    }

    @Test
    public void isbAgentKeepsPreFlipUrgencyWithinObjectiveCeiling() {
        DeployObjectiveSitingPolicy.IsbAgentEvaluation first =
                DeployObjectiveSitingPolicy.evaluateIsbAgent(
                        new DeployObjectiveSitingPolicy.IsbAgentFacts(
                                "a", true, 2.0f, 0, 4, true, false));
        DeployObjectiveSitingPolicy.IsbAgentEvaluation thirdAtBattleground =
                DeployObjectiveSitingPolicy.evaluateIsbAgent(
                        new DeployObjectiveSitingPolicy.IsbAgentFacts(
                                "a", true, 5.0f, 3, 4, true, true));
        DeployObjectiveSitingPolicy.IsbAgentEvaluation thresholdMet =
                DeployObjectiveSitingPolicy.evaluateIsbAgent(
                        new DeployObjectiveSitingPolicy.IsbAgentFacts(
                                "a", true, 3.0f, 4, 4, true, false));

        assertOperation(first.result().operations().get(0), "V29.7-ISB", 300.0f);
        assertOperation(thirdAtBattleground.result().operations().get(0),
                "V29.7-ISB", 300.0f);
        assertOperation(thresholdMet.result().operations().get(0),
                "V29.7-ISB", 300.0f);
        assertEquals(1, thirdAtBattleground.agentsStillNeeded());
        assertTrue(thirdAtBattleground.result().operations().get(0).reason()
                .contains("NEED 1 MORE FOR FLIP!"));
    }

    @Test
    public void isbPostFlipAndNonAgentStayExact() {
        DeployObjectiveSitingPolicy.IsbAgentEvaluation postFlip =
                DeployObjectiveSitingPolicy.evaluateIsbAgent(
                        new DeployObjectiveSitingPolicy.IsbAgentFacts(
                                "a", true, 3.0f, 2, 4, false, true));
        DeployObjectiveSitingPolicy.IsbAgentEvaluation nonAgent =
                DeployObjectiveSitingPolicy.evaluateIsbAgent(
                        new DeployObjectiveSitingPolicy.IsbAgentFacts(
                                "a", false, 7.0f, 0, 4, true, true));

        assertOperation(postFlip.result().operations().get(0),
                "V29.7-ISB", 300.0f);
        assertEquals(DeployObjectiveSitingPolicy.IsbAgentOutcome.NON_ISB,
                nonAgent.outcome());
        assertTrue(nonAgent.result().operations().isEmpty());
    }

    @Test
    public void huntDownKeepsVaderAndSaveForcePreferencesBounded() {
        assertHuntDown(DeployObjectiveSitingPolicy.HuntDownOutcome.VADER,
                300.0f, true, false, false, true, true);
        assertHuntDown(DeployObjectiveSitingPolicy.HuntDownOutcome.VADER,
                300.0f, true, false, true, false, false);
        assertHuntDown(DeployObjectiveSitingPolicy.HuntDownOutcome.INQUISITOR,
                -80.0f, false, true, false, true, false);
        assertHuntDown(DeployObjectiveSitingPolicy.HuntDownOutcome.SAVE_FOR_VADER,
                -200.0f, false, false, false, true, false);

        DeployObjectiveSitingPolicy.HuntDownEvaluation postFlipNonVader =
                DeployObjectiveSitingPolicy.evaluateHuntDownCharacter(
                        new DeployObjectiveSitingPolicy.HuntDownFacts(
                                "a", false, false, false, false, true));
        assertEquals(DeployObjectiveSitingPolicy.HuntDownOutcome.NONE,
                postFlipNonVader.outcome());
        assertTrue(postFlipNonVader.result().operations().isEmpty());
    }

    @Test
    public void cloudCitySpreadKeepsAllSixExclusiveOutcomes() {
        assertCloudCitySpread(DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.LANDO_SUPPORT,
                300.0f, 0.0f, true, 2, 1, 0);
        assertCloudCitySpread(DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.REINFORCE,
                300.0f, 3.0f, false, 1, 1, 0);
        assertCloudCitySpread(DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SPREAD_DEFER,
                300.0f, 0.0f, false, 1, 2, 0);
        assertCloudCitySpread(DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SPREAD,
                300.0f, 0.0f, false, 1, 0, 2);
        assertCloudCitySpread(DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SECURE_REDIRECT,
                -40.0f, 6.0f, false, 1, 0, 1);
        assertCloudCitySpread(DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SECURE,
                300.0f, 7.0f, false, 0, 0, 2);
    }

    @Test
    public void landoSafetyKeepsExactDangerCategoricalAndCautionBounded() {
        assertLandoSafety(DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ENEMY,
                0.0f, 0, 2, 3, true);
        assertLandoSafety(DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ALONE,
                0.0f, 0, 0, 0, false);
        assertLandoSafety(DeployObjectiveSitingPolicy.LandoSafetyOutcome.CAUTION,
                -300.0f, 0, 0, 1, true);

        DeployObjectiveSitingPolicy.LandoSafetyEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateLandoSafety(
                        new DeployObjectiveSitingPolicy.LandoSafetyFacts(
                                "a", true, "Dining Room", 1, 3, 0, true));
        assertEquals(DeployObjectiveSitingPolicy.LandoSafetyOutcome.SAFE_FRIENDLY,
                safe.outcome());
        assertTrue(safe.result().operations().isEmpty());
    }

    @Test
    public void tdgwattTailSharesOneNegativeBudgetInRuleOrder() {
        DeployObjectiveSitingPolicy.TdgwattOffObjectiveEvaluation tdgwatt =
                DeployObjectiveSitingPolicy.evaluateTdgwattOffObjective(
                        new DeployObjectiveSitingPolicy.TdgwattOffObjectiveFacts(
                                "a", true, true, true));

        assertEquals(2, tdgwatt.result().operations().size());
        assertOperation(tdgwatt.result().operations().get(0),
                "V29-TDIGWATT-OFF-OBJECTIVE", -300.0f);
        assertOperation(tdgwatt.result().operations().get(1),
                "V29-OPPONENT-PLANET", 0.0f);
        assertTrue(tdgwatt.tdgwattBlocked());
        assertTrue(tdgwatt.opponentPlanet());
    }

    @Test
    public void objectiveTailKeepsDeficitBoundary() {
        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation exactSix =
                DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                        new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                "a", false, false, false, true, 6.0f));
        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation overSix =
                DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                        new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                "a", false, false, false, true,
                                Math.nextUp(6.0f)));

        assertEquals(1, exactSix.result().operations().size());
        assertOperation(exactSix.result().operations().get(0),
                "V22.2-OBJECTIVE-HELP", -120.0f);
        assertOperation(overSix.result().operations().get(0),
                "V22.2-OBJECTIVE-HELP", -160.0f);
    }

    @Test
    public void objectiveTailKeepsPostFlipBandsAndProtection() {
        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation mild =
                DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                        new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                "a", true, false, false, false, 0.0f));
        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation help =
                DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                        new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                "a", true, false, false, true, 7.0f));
        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation protect =
                DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                        new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                "a", true, true, true, false, 0.0f));

        assertOperation(mild.result().operations().get(0),
                "V22-NON-OBJECTIVE", -60.0f);
        assertOperation(help.result().operations().get(0),
                "V22.2-OBJECTIVE-HELP", -220.0f);
        assertOperation(protect.result().operations().get(0),
                "V22.2-POST-FLIP-PROTECT", 300.0f);
        assertTrue(protect.postFlipProtected());
    }

    private static List<PolicyOperation> evaluate(DeployObjectiveSitingPolicy.Facts facts) {
        return DeployObjectiveSitingPolicy.evaluate(facts).operations();
    }

    private static Map<String, Float> linkedPowers(Object... entries) {
        Map<String, Float> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], (Float) entries[index + 1]);
        }
        return result;
    }

    private static void assertFlipScore(float expected, int turnNumber,
                                        boolean huntDown, boolean inquisitor) {
        DeployObjectiveSitingPolicy.FlipSitingEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", false, turnNumber, huntDown, inquisitor,
                                1, 2, true, false, false));
        assertEquals(DeployObjectiveSitingPolicy.FlipSitingOutcome.PREFLIP_DEFEND,
                evaluation.outcome());
        assertOperation(evaluation.result().operations().get(0),
                "V36", expected);
    }

    private static void assertHuntDown(
            DeployObjectiveSitingPolicy.HuntDownOutcome expectedOutcome,
            float expectedScore, boolean vader, boolean inquisitor,
            boolean vaderOnTable, boolean preFlip, boolean battleground) {
        DeployObjectiveSitingPolicy.HuntDownEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateHuntDownCharacter(
                        new DeployObjectiveSitingPolicy.HuntDownFacts(
                                "a", vader, inquisitor, vaderOnTable,
                                preFlip, battleground));
        assertEquals(expectedOutcome, evaluation.outcome());
        assertEquals(expectedScore,
                evaluation.result().operations().get(0).delta(), 0.0f);
    }

    private static void assertCloudCitySpread(
            DeployObjectiveSitingPolicy.CloudCitySpreadOutcome expectedOutcome,
            float expectedScore, float abilityHere, boolean landoAlone,
            int empty, int insecure, int secure) {
        DeployObjectiveSitingPolicy.CloudCitySpreadEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateCloudCitySpread(
                        new DeployObjectiveSitingPolicy.CloudCitySpreadFacts(
                                "a", abilityHere, landoAlone,
                                empty, insecure, secure));
        assertEquals(expectedOutcome, evaluation.outcome());
        assertEquals(expectedScore,
                evaluation.result().operations().get(0).delta(), 0.0f);
    }

    private static void assertLandoSafety(
            DeployObjectiveSitingPolicy.LandoSafetyOutcome expectedOutcome,
            float expectedScore, int friendlyHere, int opponentHere,
            int charactersInHand, boolean opponentThreatens) {
        DeployObjectiveSitingPolicy.LandoSafetyEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateLandoSafety(
                        new DeployObjectiveSitingPolicy.LandoSafetyFacts(
                                "a", true, "Dining Room", friendlyHere,
                                opponentHere, charactersInHand,
                                opponentThreatens));
        assertEquals(expectedOutcome, evaluation.outcome());
        PolicyOperation operation = evaluation.result().operations().get(0);
        if (expectedOutcome
                == DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ENEMY
                || expectedOutcome
                == DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ALONE) {
            assertEquals(PolicyOperationKind.HARD_VETO, operation.kind());
            assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
            assertEquals(0.0f, operation.delta(), 0.0f);
        } else {
            assertOperation(operation, operation.ruleArmId().id(), expectedScore);
        }
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId, float delta) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                operation.domainId());
        assertTrue(operation.delta() >= -300.0f);
        assertTrue(operation.delta() <= 300.0f);
    }

    private static void assertHardVeto(
            PolicyOperation operation, String ruleId) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(PolicyOperationKind.HARD_VETO, operation.kind());
        assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
        assertEquals(TraceOutputKind.VETO, operation.outputKind());
        assertEquals(0.0f, operation.delta(), 0.0f);
    }
}
