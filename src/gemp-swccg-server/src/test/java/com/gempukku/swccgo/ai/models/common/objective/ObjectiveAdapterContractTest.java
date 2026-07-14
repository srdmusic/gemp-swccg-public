package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.phase.PullAssessment;
import com.gempukku.swccgo.ai.models.common.phase.PullFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Pure adapter laws over one immutable ObjectiveFacts decision snapshot. */
public class ObjectiveAdapterContractTest {

    private static final String PLAYER = "objective-adapter-player";
    private static final long PULL_TRANSACTION_ID = 7001L;
    private static final PullPhysicalCardRef OBJECTIVE_SOURCE = new PullPhysicalCardRef(
            ObjectiveFactsFixtures.OBJECTIVE_PERMANENT_CARD_ID,
            ObjectiveFactsFixtures.OBJECTIVE_CURRENT_CARD_ID);
    private static final PullPhysicalCardRef NON_OBJECTIVE_SOURCE =
            new PullPhysicalCardRef(9100, 510);
    private static final PullPhysicalCardRef LOCATION_CANDIDATE =
            new PullPhysicalCardRef(9200, 520);

    @Test
    public void deployContributionsPreserveExactLegalOrderAndValues() {
        DecisionSnapshot snapshot = snapshot(deployFacts());

        ObjectiveDeployAdapter.Result nonSenateSenator = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(0, ObjectiveDeployAdapter.Stage.PARENT_ACTION,
                        101, 402, true, 2.0f, 3.0f));
        assertSame(snapshot, nonSenateSenator.snapshot());
        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.MY_LORD_V83,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 0, -2000.0f),
                contribution(ObjectiveContribution.Rule.MY_LORD_V108,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 0, 500.0f),
                contribution(ObjectiveContribution.Rule.OBJECTIVE_SITE,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 0, 200.0f),
                contribution(ObjectiveContribution.Rule.V193_PARENT,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 0, 400.0f)),
                nonSenateSenator.contributions());

        ObjectiveDeployAdapter.Result senateSenator = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(1, ObjectiveDeployAdapter.Stage.PARENT_ACTION,
                        101, 401, true, 2.0f, 3.0f));
        assertSame(snapshot, senateSenator.snapshot());
        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.MY_LORD_V108,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 1, 500.0f),
                contribution(ObjectiveContribution.Rule.MY_LORD_V88,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 1, 1500.0f),
                contribution(ObjectiveContribution.Rule.OBJECTIVE_SITE,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 1, 200.0f),
                contribution(ObjectiveContribution.Rule.V193_PARENT,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 1, 400.0f)),
                senateSenator.contributions());

        ObjectiveDeployAdapter.Result nonSenator = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(2, ObjectiveDeployAdapter.Stage.PARENT_ACTION,
                        999, null, false, null, null));
        assertSame(snapshot, nonSenator.snapshot());
        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.MY_LORD_V110,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 2, -2000.0f)),
                nonSenator.contributions());

        ObjectiveDeployAdapter.Result childDestination = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(3, ObjectiveDeployAdapter.Stage.CHILD_DESTINATION,
                        101, 401, true, 1.0f, 4.0f));
        assertSame(snapshot, childDestination.snapshot());
        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.MY_LORD_V88,
                        ObjectiveContribution.Channel.DEPLOY_CHILD, 3, 1500.0f),
                contribution(ObjectiveContribution.Rule.OBJECTIVE_SITE,
                        ObjectiveContribution.Channel.DEPLOY_CHILD, 3, 200.0f),
                contribution(ObjectiveContribution.Rule.V193_CHILD,
                        ObjectiveContribution.Channel.DEPLOY_CHILD, 3, 2000.0f)),
                childDestination.contributions());

        assertExactBits(-2000.0f, contributionValue(
                nonSenateSenator, ObjectiveContribution.Rule.MY_LORD_V83));
        assertExactBits(1500.0f, contributionValue(
                senateSenator, ObjectiveContribution.Rule.MY_LORD_V88));
        assertExactBits(500.0f, contributionValue(
                senateSenator, ObjectiveContribution.Rule.MY_LORD_V108));
        assertExactBits(200.0f, contributionValue(
                senateSenator, ObjectiveContribution.Rule.OBJECTIVE_SITE));
        assertExactBits(-2000.0f, contributionValue(
                nonSenator, ObjectiveContribution.Rule.MY_LORD_V110));
        assertExactBits(400.0f, contributionValue(
                senateSenator, ObjectiveContribution.Rule.V193_PARENT));
        assertExactBits(2000.0f, contributionValue(
                childDestination, ObjectiveContribution.Rule.V193_CHILD));
    }

    @Test
    public void myLordSenatorPredicatesRemainRuleSpecific() {
        DecisionSnapshot snapshot = snapshot(deployFacts(false));

        ObjectiveDeployAdapter.Result filterOnlyParent = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(0, ObjectiveDeployAdapter.Stage.PARENT_ACTION,
                        101, 402, true, false, false, false, false,
                        2.0f, 3.0f));
        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.MY_LORD_V83,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 0, -2000.0f),
                contribution(ObjectiveContribution.Rule.MY_LORD_V110,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 0, -2000.0f)),
                filterOnlyParent.contributions());

        ObjectiveDeployAdapter.Result keywordOnlyParent = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(1, ObjectiveDeployAdapter.Stage.PARENT_ACTION,
                        101, 401, false, true, false, false, false,
                        2.0f, 3.0f));
        assertEquals(List.of(contribution(ObjectiveContribution.Rule.MY_LORD_V108,
                        ObjectiveContribution.Channel.DEPLOY_PARENT, 1, 500.0f)),
                keywordOnlyParent.contributions());

        ObjectiveDeployAdapter.Result keywordOnlyChild = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(2, ObjectiveDeployAdapter.Stage.CHILD_DESTINATION,
                        101, 401, false, true, false, false, false,
                        2.0f, 3.0f));
        assertEquals(List.of(contribution(ObjectiveContribution.Rule.MY_LORD_V88,
                        ObjectiveContribution.Channel.DEPLOY_CHILD, 2, 1500.0f)),
                keywordOnlyChild.contributions());
    }

    @Test
    public void v193ChildUsesExactLegacyBranchFactInsteadOfAbilityCostApproximation() {
        DecisionSnapshot snapshot = snapshot(deployFacts(false));

        ObjectiveDeployAdapter.Result branchEmits = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(0, ObjectiveDeployAdapter.Stage.CHILD_DESTINATION,
                        999, 402, false, false, false, false, true,
                        null, null));
        assertEquals(List.of(contribution(ObjectiveContribution.Rule.V193_CHILD,
                        ObjectiveContribution.Channel.DEPLOY_CHILD, 0, 2000.0f)),
                branchEmits.contributions());

        ObjectiveDeployAdapter.Result branchDoesNotEmit = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(1, ObjectiveDeployAdapter.Stage.CHILD_DESTINATION,
                        999, 402, false, false, false, false, false,
                        1.0f, 4.0f));
        assertTrue(branchDoesNotEmit.contributions().isEmpty());
    }

    @Test
    public void moveIntentsUseTypedHiddenPathTruthAndCorridorCostOne() {
        DecisionSnapshot snapshot = snapshot(ObjectiveFactsFixtures.facts(false));

        ObjectiveMoveAdapter.Result result = ObjectiveMoveAdapter.adapt(snapshot);

        assertSame(snapshot, result.snapshot());
        assertEquals(2, result.intents().size());
        assertEquals(new ObjectiveMoveAdapter.HiddenPathIntent(false, 2, 2, true),
                result.intents().get(0));
        assertEquals(new ObjectiveMoveAdapter.UndergroundCorridorIntent(
                        ObjectiveFactsFixtures.JEDI_SURVIVOR_CARD_IDS, 1),
                result.intents().get(1));
    }

    @Test
    public void battleIntentRetainsTypedInquisitorAndHatredMembership() {
        DecisionSnapshot snapshot = snapshot(huntFacts());

        ObjectiveBattleAdapter.Result result = ObjectiveBattleAdapter.adapt(snapshot);

        assertSame(snapshot, result.snapshot());
        assertEquals(List.of(new ObjectiveBattleAdapter.HuntIntent(
                        ObjectiveFactsFixtures.INQUISITOR_CARD_IDS,
                        ObjectiveFactsFixtures.INQUISITOR_WITH_HATRED_CARD_IDS)),
                result.intents());
    }

    @Test
    public void battleInitiationEmitsEachMigratedHuntRuleOnceInLegacyOrder() {
        DecisionSnapshot snapshot = snapshot(huntFacts());

        ObjectiveBattleAdapter.InitiationAssessment result =
                ObjectiveBattleAdapter.assessInitiation(
                        snapshot, 4, Set.of(302), true, true,
                        true, true, -250.0f, 0.4f);

        assertExactBits(-100.0f, result.adjustedBarrierRisk());
        assertTrue(result.vaderExpendabilityApplied());
        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.V35_VADER_EXPENDABLE,
                        ObjectiveContribution.Channel.BATTLE_INITIATE, 4, 150.0f),
                contribution(ObjectiveContribution.Rule.V29_9_HUNT_DOWN,
                        ObjectiveContribution.Channel.BATTLE_INITIATE, 4, 200.0f),
                contribution(ObjectiveContribution.Rule.V35_HUNT_DESTINY,
                        ObjectiveContribution.Channel.BATTLE_INITIATE, 4, 350.0f)),
                result.contributions());
        assertExactBits(150.0f, result.contribution(
                ObjectiveContribution.Rule.V35_VADER_EXPENDABLE));
        assertExactBits(200.0f, result.contribution(
                ObjectiveContribution.Rule.V29_9_HUNT_DOWN));
        assertExactBits(350.0f, result.contribution(
                ObjectiveContribution.Rule.V35_HUNT_DESTINY));
    }

    @Test
    public void battleInitiationPreservesHuntScoreMatrixAndNonHuntFallthrough() {
        DecisionSnapshot hunt = snapshot(huntFacts());

        ObjectiveBattleAdapter.InitiationAssessment vader =
                ObjectiveBattleAdapter.assessInitiation(
                        hunt, 0, Set.of(), true, true,
                        false, false, 0f, 0.4f);
        assertExactBits(80.0f, vader.contribution(
                ObjectiveContribution.Rule.V29_9_HUNT_DOWN));

        ObjectiveBattleAdapter.InitiationAssessment inquisitor =
                ObjectiveBattleAdapter.assessInitiation(
                        hunt, 1, Set.of(301), false, false,
                        false, false, 0f, 0.4f);
        assertExactBits(120.0f, inquisitor.contribution(
                ObjectiveContribution.Rule.V35_HUNT_DESTINY));

        ObjectiveBattleAdapter.InitiationAssessment hatred =
                ObjectiveBattleAdapter.assessInitiation(
                        hunt, 2, Set.of(302), false, false,
                        false, false, 0f, 0.4f);
        assertExactBits(250.0f, hatred.contribution(
                ObjectiveContribution.Rule.V35_HUNT_DESTINY));

        ObjectiveBattleAdapter.InitiationAssessment nonHunt =
                ObjectiveBattleAdapter.assessInitiation(
                        snapshot(deployFacts()), 3, Set.of(302), true, true,
                        true, true, -250.0f, 0.4f);
        assertExactBits(-250.0f, nonHunt.adjustedBarrierRisk());
        assertTrue(nonHunt.contributions().isEmpty());
    }

    @Test
    public void pullParentAddsExactlyOneContributionPerMatchingActionOrdinal() {
        DecisionSnapshot snapshot = snapshot(ObjectiveFactsFixtures.facts(false));
        PullFacts facts = parentPullFacts(List.of(
                parentCandidate(0, "objective-deploy", OBJECTIVE_SOURCE),
                parentCandidate(1, "other-deploy", NON_OBJECTIVE_SOURCE),
                parentCandidate(2, "objective-take", OBJECTIVE_SOURCE)));
        PullAssessment assessment = PullAssessment.defer(
                PullRoute.PULL_PARENT, PullAssessment.Evidence.POLICY_DEFER);
        List<ObjectiveContribution> contributions = new ArrayList<>();

        for (PullFacts.ParentCandidate candidate : facts.parentCandidates()) {
            ObjectivePullAdapter.Result result = ObjectivePullAdapter.adaptParent(
                    snapshot, facts, assessment, candidate.ordinal());
            assertSame(snapshot, result.snapshot());
            assertSame(facts, result.pullFacts());
            contributions.addAll(result.parentContributions());
        }

        assertEquals(List.of(
                contribution(ObjectiveContribution.Rule.V192_PULL_PARENT,
                        ObjectiveContribution.Channel.PULL_PARENT, 0, 1500.0f),
                contribution(ObjectiveContribution.Rule.V192_PULL_PARENT,
                        ObjectiveContribution.Channel.PULL_PARENT, 2, 1500.0f)),
                contributions);
        Set<Integer> seenOrdinals = new HashSet<>();
        for (ObjectiveContribution contribution : contributions) {
            assertTrue("one PULL objective contribution maximum per action ordinal",
                    seenOrdinals.add(contribution.candidateOrdinal()));
        }
    }

    @Test
    public void pullChildRanksTypedPhysicalIdentityAtArbitraryWireOrdinal() {
        DecisionSnapshot snapshot = snapshot(ObjectiveFactsFixtures.facts(false));
        PullFacts facts = childPullFacts(PullRoute.PULL_DEPLOY_CHILD,
                List.of(NON_OBJECTIVE_SOURCE, LOCATION_CANDIDATE));
        PullAssessment assessment = PullAssessment.defer(
                facts.route(), PullAssessment.Evidence.POLICY_DEFER);

        ObjectivePullAdapter.Result result = ObjectivePullAdapter.adaptChild(
                snapshot,
                facts,
                assessment,
                new ObjectivePullAdapter.ChildCandidate(
                        LOCATION_CANDIDATE, ObjectivePullAdapter.ChildKind.LOCATION));

        assertSame(snapshot, result.snapshot());
        assertSame(facts, result.pullFacts());
        assertEquals(List.of(new ObjectivePullAdapter.ChildRank(LOCATION_CANDIDATE, 500.0f)),
                result.childRanks());
        assertTrue(result.parentContributions().isEmpty());
        assertTrue(result.failedVerifyIntents().isEmpty());

        ObjectivePullAdapter.Result ordinalResult = ObjectivePullAdapter.adaptChildAtOrdinal(
                snapshot, facts, assessment, 1, ObjectivePullAdapter.ChildKind.LOCATION);
        assertEquals(result.childRanks(), ordinalResult.childRanks());
        assertTrue("an out-of-range temporary wire ordinal contributes nothing",
                ObjectivePullAdapter.adaptChildAtOrdinal(
                        snapshot, facts, assessment, 2,
                        ObjectivePullAdapter.ChildKind.LOCATION).childRanks().isEmpty());
    }

    @Test
    public void pullFailedVerifyRequiresCanonicalAssessmentEvidence() {
        DecisionSnapshot snapshot = snapshot(ObjectiveFactsFixtures.facts(false));
        PullFacts facts = childPullFacts(PullRoute.PULL_FAILED_VERIFY, List.of());
        PullAssessment canonical = PullAssessment.allow(
                facts.route(),
                PullAssessment.Evidence.ACCEPTED_PARENT_TRANSACTION_ID,
                PullAssessment.Evidence.EXACT_PERMANENT_AND_CURRENT_IDENTITY,
                PullAssessment.Evidence.FAILED_VERIFY_EMPTY_SELECTION);

        ObjectivePullAdapter.Result result = ObjectivePullAdapter.adaptFailedVerify(
                snapshot, facts, canonical);

        assertSame(snapshot, result.snapshot());
        assertSame(facts, result.pullFacts());
        assertEquals(List.of(new ObjectivePullAdapter.FailedVerifyIntent(
                        OBJECTIVE_SOURCE, PULL_TRANSACTION_ID)),
                result.failedVerifyIntents());

        PullAssessment missingCanonicalEvidence = PullAssessment.allow(
                facts.route(), PullAssessment.Evidence.POLICY_ALLOW);
        assertTrue(ObjectivePullAdapter.adaptFailedVerify(
                snapshot, facts, missingCanonicalEvidence).failedVerifyIntents().isEmpty());
    }

    @Test
    public void setupIntentsPreserveTypedReferenceKindsAndOrder() {
        ObjectiveFacts.StartingRefs refs = new ObjectiveFacts.StartingRefs(
                Set.of("loc-blueprint"),
                Set.of("location title"),
                Set.of("effect-blueprint"),
                Set.of("effect title"),
                Set.of("interrupt-blueprint"),
                Set.of("interrupt title"));
        DecisionSnapshot snapshot = snapshot(facts(
                new ObjectiveFacts.ObjectiveKind(false, false, false, false, false, false),
                refs,
                emptyBoard(),
                false));

        ObjectiveSetupAdapter.Result result = ObjectiveSetupAdapter.adapt(snapshot);

        assertSame(snapshot, result.snapshot());
        assertEquals(List.of(
                new ObjectiveSetupAdapter.StartingIntent(
                        ObjectiveSetupAdapter.Kind.LOCATION,
                        refs.locationBlueprintIds(), refs.locationTitleFragments()),
                new ObjectiveSetupAdapter.StartingIntent(
                        ObjectiveSetupAdapter.Kind.EFFECT,
                        refs.effectBlueprintIds(), refs.effectTitleFragments()),
                new ObjectiveSetupAdapter.StartingIntent(
                        ObjectiveSetupAdapter.Kind.INTERRUPT,
                        refs.interruptBlueprintIds(), refs.interruptTitleFragments())),
                result.intents());
    }

    @Test
    public void unknownFactsFallThroughEveryAdapterWithoutInventingOutput() {
        DecisionSnapshot snapshot = snapshot(ObjectiveFacts.unknown("objective unavailable"));
        PullFacts pullFacts = parentPullFacts(List.of(
                parentCandidate(0, "objective-deploy", OBJECTIVE_SOURCE)));
        PullAssessment pullAssessment = PullAssessment.defer(
                PullRoute.PULL_PARENT, PullAssessment.Evidence.POLICY_DEFER);

        ObjectiveDeployAdapter.Result deploy = ObjectiveDeployAdapter.adapt(
                snapshot,
                deployCandidate(0, ObjectiveDeployAdapter.Stage.PARENT_ACTION,
                        101, 401, true, 2.0f, 3.0f));
        ObjectiveMoveAdapter.Result move = ObjectiveMoveAdapter.adapt(snapshot);
        ObjectiveBattleAdapter.Result battle = ObjectiveBattleAdapter.adapt(snapshot);
        ObjectivePullAdapter.Result pull = ObjectivePullAdapter.adaptParent(
                snapshot, pullFacts, pullAssessment, 0);
        ObjectiveSetupAdapter.Result setup = ObjectiveSetupAdapter.adapt(snapshot);

        assertSame(snapshot, deploy.snapshot());
        assertSame(snapshot, move.snapshot());
        assertSame(snapshot, battle.snapshot());
        assertSame(snapshot, pull.snapshot());
        assertSame(snapshot, setup.snapshot());
        assertTrue(deploy.contributions().isEmpty());
        assertTrue(move.intents().isEmpty());
        assertTrue(battle.intents().isEmpty());
        assertTrue(pull.parentContributions().isEmpty());
        assertTrue(pull.childRanks().isEmpty());
        assertTrue(pull.failedVerifyIntents().isEmpty());
        assertTrue(setup.intents().isEmpty());
    }

    @Test
    public void unknownIdentityPreservesOnlyExplicitLegacyPullFallbacks() {
        DecisionSnapshot snapshot = snapshot(ObjectiveFacts.unknown("objective unavailable"));
        PullFacts parentFacts = parentPullFacts(List.of(
                parentCandidate(0, "objective-deploy", OBJECTIVE_SOURCE)));
        PullAssessment parentAssessment = PullAssessment.defer(
                PullRoute.PULL_PARENT, PullAssessment.Evidence.POLICY_DEFER);

        ObjectivePullAdapter.Result parentFallback = ObjectivePullAdapter.adaptParent(
                snapshot, parentFacts, parentAssessment, 0, true);
        assertEquals(List.of(contribution(
                ObjectiveContribution.Rule.V192_PULL_PARENT,
                ObjectiveContribution.Channel.PULL_PARENT, 0, 1500.0f)),
                parentFallback.parentContributions());
        assertTrue(ObjectivePullAdapter.adaptParent(
                snapshot, parentFacts, parentAssessment, 0, false)
                .parentContributions().isEmpty());
        assertTrue(ObjectivePullAdapter.adaptParent(
                snapshot, parentFacts,
                PullAssessment.block(PullRoute.PULL_PARENT,
                        PullAssessment.Evidence.POLICY_BLOCK),
                0, true).parentContributions().isEmpty());

        PullFacts childFacts = childPullFacts(PullRoute.PULL_DEPLOY_CHILD,
                List.of(NON_OBJECTIVE_SOURCE, LOCATION_CANDIDATE));
        PullAssessment childAssessment = PullAssessment.defer(
                PullRoute.PULL_DEPLOY_CHILD, PullAssessment.Evidence.POLICY_DEFER);
        ObjectivePullAdapter.Result childFallback = ObjectivePullAdapter.adaptChildAtOrdinal(
                snapshot, childFacts, childAssessment, 1,
                ObjectivePullAdapter.ChildKind.LOCATION, true, true);
        assertEquals(List.of(new ObjectivePullAdapter.ChildRank(
                LOCATION_CANDIDATE, 500.0f)), childFallback.childRanks());
        assertTrue(ObjectivePullAdapter.adaptChildAtOrdinal(
                snapshot, childFacts, childAssessment, 1,
                ObjectivePullAdapter.ChildKind.LOCATION, true, false)
                .childRanks().isEmpty());
        assertTrue(ObjectivePullAdapter.adaptChildAtOrdinal(
                snapshot, childFacts, childAssessment, 1,
                ObjectivePullAdapter.ChildKind.LOCATION, false, true)
                .childRanks().isEmpty());
        assertTrue(ObjectivePullAdapter.adaptChildAtOrdinal(
                snapshot, childFacts,
                PullAssessment.block(PullRoute.PULL_DEPLOY_CHILD,
                        PullAssessment.Evidence.POLICY_BLOCK),
                1, ObjectivePullAdapter.ChildKind.LOCATION, true, true)
                .childRanks().isEmpty());
    }

    @Test
    public void knownIdentityMismatchPreservesExplicitLegacyPullFallbacks() {
        DecisionSnapshot snapshot = snapshot(deployFacts());
        PullFacts parentFacts = parentPullFacts(List.of(
                parentCandidate(0, "objective-deploy", NON_OBJECTIVE_SOURCE)));
        PullAssessment parentAssessment = PullAssessment.defer(
                PullRoute.PULL_PARENT, PullAssessment.Evidence.POLICY_DEFER);

        assertEquals(List.of(contribution(
                ObjectiveContribution.Rule.V192_PULL_PARENT,
                ObjectiveContribution.Channel.PULL_PARENT, 0, 1500.0f)),
                ObjectivePullAdapter.adaptParent(
                        snapshot, parentFacts, parentAssessment, 0, true)
                        .parentContributions());

        PullFacts childFacts = childPullFacts(PullRoute.PULL_DEPLOY_CHILD,
                List.of(NON_OBJECTIVE_SOURCE, LOCATION_CANDIDATE),
                NON_OBJECTIVE_SOURCE);
        PullAssessment childAssessment = PullAssessment.defer(
                PullRoute.PULL_DEPLOY_CHILD, PullAssessment.Evidence.POLICY_DEFER);
        assertEquals(List.of(new ObjectivePullAdapter.ChildRank(
                LOCATION_CANDIDATE, 500.0f)),
                ObjectivePullAdapter.adaptChildAtOrdinal(
                        snapshot, childFacts, childAssessment, 1,
                        ObjectivePullAdapter.ChildKind.LOCATION, true, true)
                        .childRanks());
    }

    private static ObjectiveDeployAdapter.CandidateFacts deployCandidate(
            int ordinal,
            ObjectiveDeployAdapter.Stage stage,
            int cardId,
            Integer destinationId,
            boolean targetsFlipGate,
            Float ability,
            Float deployCost) {
        boolean senator = cardId == 101;
        boolean child = stage == ObjectiveDeployAdapter.Stage.CHILD_DESTINATION;
        return deployCandidate(
                ordinal, stage, cardId, destinationId,
                senator, senator, true, targetsFlipGate && !child,
                targetsFlipGate && child, ability, deployCost);
    }

    private static ObjectiveDeployAdapter.CandidateFacts deployCandidate(
            int ordinal,
            ObjectiveDeployAdapter.Stage stage,
            int cardId,
            Integer destinationId,
            boolean filtersSenator,
            boolean keywordOrLoreSenator,
            boolean objectiveSiteEligible,
            boolean v193ParentLegacyBranchEmits,
            boolean v193ChildLegacyBranchEmits,
            Float ability,
            Float deployCost) {
        return new ObjectiveDeployAdapter.CandidateFacts(
                ordinal,
                stage,
                cardId,
                true,
                filtersSenator,
                keywordOrLoreSenator,
                destinationId,
                v193ParentLegacyBranchEmits || v193ChildLegacyBranchEmits,
                true,
                objectiveSiteEligible,
                destinationId != null && Set.of(401, 402).contains(destinationId),
                v193ParentLegacyBranchEmits,
                v193ChildLegacyBranchEmits,
                ability,
                deployCost);
    }

    private static ObjectiveContribution contribution(ObjectiveContribution.Rule rule,
                                                       ObjectiveContribution.Channel channel,
                                                       int ordinal,
                                                       float value) {
        return new ObjectiveContribution(rule, channel, ordinal, value);
    }

    private static ObjectiveFacts deployFacts() {
        return deployFacts(false);
    }

    private static ObjectiveFacts deployFacts(boolean flipped) {
        ObjectiveFacts.TypedBoardFacts board = new ObjectiveFacts.TypedBoardFacts(
                Set.of(101),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(401),
                Set.of(401, 402),
                Set.of(401),
                false,
                0,
                false,
                false,
                false,
                false);
        return facts(
                new ObjectiveFacts.ObjectiveKind(true, false, false, false, false, false),
                emptyStartingRefs(),
                board,
                true,
                flipped);
    }

    private static ObjectiveFacts huntFacts() {
        ObjectiveFacts.TypedBoardFacts board = new ObjectiveFacts.TypedBoardFacts(
                Set.of(),
                Set.of(),
                ObjectiveFactsFixtures.INQUISITOR_CARD_IDS,
                ObjectiveFactsFixtures.INQUISITOR_WITH_HATRED_CARD_IDS,
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                0,
                false,
                false,
                false,
                false);
        return facts(
                new ObjectiveFacts.ObjectiveKind(false, false, false, false, true, false),
                emptyStartingRefs(),
                board,
                false);
    }

    private static ObjectiveFacts facts(ObjectiveFacts.ObjectiveKind kind,
                                        ObjectiveFacts.StartingRefs startingRefs,
                                        ObjectiveFacts.TypedBoardFacts board,
                                        boolean withFlipGate) {
        return facts(kind, startingRefs, board, withFlipGate, false);
    }

    private static ObjectiveFacts facts(ObjectiveFacts.ObjectiveKind kind,
                                        ObjectiveFacts.StartingRefs startingRefs,
                                        ObjectiveFacts.TypedBoardFacts board,
                                        boolean withFlipGate,
                                        boolean flipped) {
        FactValue<String> controlSite = withFlipGate
                ? known("flip-critical site", "typed control site")
                : unknown("flip-critical site", "no control gate");
        FactValue<String> controlCard = withFlipGate
                ? known("flip-critical card", "typed control card")
                : unknown("flip-critical card", "no control gate");
        ObjectiveFacts.StrategyFacts strategy = new ObjectiveFacts.StrategyFacts(
                kind,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                startingRefs,
                withFlipGate ? Set.of("flip-critical-card-id") : Set.of(),
                controlSite,
                controlCard,
                unknown("flip condition", "not required by adapter fixture"),
                unknown("flip-back condition", "not required by adapter fixture"),
                false,
                false,
                false,
                false,
                Set.of());
        return new ObjectiveFacts(
                known(ObjectiveFactsFixtures.identity(flipped), "physical objective orientation"),
                known(ObjectiveFactsFixtures.profileResolution(
                                ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID),
                        "blueprint-first objective profile"),
                known(strategy, "typed objective strategy"),
                known(board, "typed objective board"));
    }

    private static void assertExactBits(float expected, float actual) {
        assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(actual));
    }

    private static float contributionValue(ObjectiveDeployAdapter.Result result,
                                           ObjectiveContribution.Rule rule) {
        for (ObjectiveContribution contribution : result.contributions()) {
            if (contribution.rule() == rule) return contribution.value();
        }
        throw new AssertionError("Missing contribution " + rule);
    }

    private static ObjectiveFacts.StartingRefs emptyStartingRefs() {
        return new ObjectiveFacts.StartingRefs(
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }

    private static ObjectiveFacts.TypedBoardFacts emptyBoard() {
        return new ObjectiveFacts.TypedBoardFacts(
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                false, 0, false, false, false, false);
    }

    private static PullFacts.ParentCandidate parentCandidate(int ordinal,
                                                             String actionId,
                                                             PullPhysicalCardRef source) {
        DecisionActionSemantic semantic = actionId.contains("take")
                ? DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE
                : DecisionActionSemantic.PULL_DEPLOY_FROM_PILE;
        return new PullFacts.ParentCandidate(
                ordinal,
                actionId,
                semantic,
                source,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD);
    }

    private static PullFacts parentPullFacts(List<PullFacts.ParentCandidate> candidates) {
        return new PullFacts(
                "pull-parent",
                3,
                PLAYER,
                Phase.DEPLOY,
                PullRoute.PULL_PARENT,
                null,
                new PullFacts.ParentIdentity(41, null),
                candidates,
                unknown("source card", "candidate-specific before acceptance"),
                unknown("game-text action", "candidate-specific before acceptance"),
                unknown("source zone", "candidate-specific before acceptance"),
                unknown("source zone owner", "candidate-specific before acceptance"),
                List.of(),
                List.of(),
                null,
                List.of(),
                null,
                unknown("source filter", "engine filter is unavailable"));
    }

    private static PullFacts childPullFacts(PullRoute route,
                                            List<PullPhysicalCardRef> candidates) {
        return childPullFacts(route, candidates, OBJECTIVE_SOURCE);
    }

    private static PullFacts childPullFacts(PullRoute route,
                                            List<PullPhysicalCardRef> candidates,
                                            PullPhysicalCardRef source) {
        return new PullFacts(
                "pull-child",
                3,
                PLAYER,
                Phase.DEPLOY,
                route,
                PULL_TRANSACTION_ID,
                new PullFacts.ParentIdentity(41, 0),
                List.of(),
                known(source, "accepted objective source"),
                known(GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD,
                        "accepted game-text action"),
                known(Zone.RESERVE_DECK, "typed source zone"),
                known(PLAYER, "typed source zone owner"),
                candidates,
                candidates.stream()
                        .map(card -> String.valueOf(card.currentCardId()))
                        .toList(),
                null,
                List.of(),
                null,
                unknown("source filter", "engine filter is unavailable"));
    }

    private static DecisionSnapshot snapshot(ObjectiveFacts facts) {
        TraceSnapshots.Result result = TraceSnapshots.build(
                ObjectiveFactsFixtures.traceInput(facts));
        assertNotNull("shared trace builder must produce a snapshot: " + result.issues(),
                result.snapshot());
        assertTrue("shared trace builder reported issues: " + result.issues(),
                result.issues().isEmpty());
        return result.snapshot();
    }

    private static <T> FactValue<T> known(T value, String provenance) {
        return FactValue.known(value, "objective-adapter-test", provenance);
    }

    private static <T> FactValue<T> unknown(String provenance, String reason) {
        return FactValue.unknown("objective-adapter-test", provenance, reason);
    }
}
