package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure common-model laws. Bot adapters may reuse this corpus without changing its facts. */
public class ObjectiveFactsContractTest {

    @Test
    public void canonicalIdentityStaysFixedWhileCurrentOrientationSwaps() {
        ObjectiveFacts.Identity front = ObjectiveFactsFixtures.identity(false);
        ObjectiveFacts.Identity back = ObjectiveFactsFixtures.identity(true);

        assertCanonicalIdentity(front);
        assertCanonicalIdentity(back);
        assertEquals(front.objectivePermanentCardId(), back.objectivePermanentCardId());
        assertEquals(front.objectiveCurrentCardId(), back.objectiveCurrentCardId());

        assertOrientation(
                front,
                ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                ObjectiveFactsFixtures.FRONT_TITLE,
                ObjectiveFactsFixtures.BACK_TITLE,
                ObjectiveFactsFixtures.FRONT_GAME_TEXT,
                ObjectiveFactsFixtures.BACK_GAME_TEXT,
                false);
        assertOrientation(
                back,
                ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                ObjectiveFactsFixtures.BACK_TITLE,
                ObjectiveFactsFixtures.FRONT_TITLE,
                ObjectiveFactsFixtures.BACK_GAME_TEXT,
                ObjectiveFactsFixtures.FRONT_GAME_TEXT,
                true);
    }

    @Test
    public void physicalObjectiveProjectionUsesCurrentAndOppositeBlueprintsInBothOrientations() {
        FactValue<ObjectiveFacts.Identity> front = ObjectiveFactsProducer.projectIdentity(
                physicalObjective(false));
        FactValue<ObjectiveFacts.Identity> back = ObjectiveFactsProducer.projectIdentity(
                physicalObjective(true));

        assertTrue(front.isKnown());
        assertTrue(back.isKnown());
        assertEquals(ObjectiveFactsFixtures.identity(false), front.value());
        assertEquals(ObjectiveFactsFixtures.identity(true), back.value());
        assertEquals(front.value().canonicalFrontBlueprintId(),
                back.value().canonicalFrontBlueprintId());
        assertEquals(front.value().canonicalBackBlueprintId(),
                back.value().canonicalBackBlueprintId());
    }

    @Test
    public void identityRejectsBlueprintAndTitleOrientationContradictions() {
        assertIllegalArgument("unflipped current blueprint cannot be the back",
                () -> ObjectiveFactsFixtures.identity(
                        false,
                        ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.FRONT_TITLE,
                        ObjectiveFactsFixtures.BACK_TITLE,
                        ObjectiveFactsFixtures.FRONT_GAME_TEXT,
                        ObjectiveFactsFixtures.BACK_GAME_TEXT));
        assertIllegalArgument("flipped current title cannot be the front",
                () -> ObjectiveFactsFixtures.identity(
                        true,
                        ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.FRONT_TITLE,
                        ObjectiveFactsFixtures.BACK_TITLE,
                        ObjectiveFactsFixtures.BACK_GAME_TEXT,
                        ObjectiveFactsFixtures.FRONT_GAME_TEXT));
    }

    @Test
    public void identityRejectsGameTextOrientationContradictions() {
        assertIllegalArgument("unflipped current text must be canonical front text",
                () -> ObjectiveFactsFixtures.identity(
                        false,
                        ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.FRONT_TITLE,
                        ObjectiveFactsFixtures.BACK_TITLE,
                        ObjectiveFactsFixtures.BACK_GAME_TEXT,
                        ObjectiveFactsFixtures.FRONT_GAME_TEXT));
        assertIllegalArgument("flipped current text must be canonical back text",
                () -> ObjectiveFactsFixtures.identity(
                        true,
                        ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                        ObjectiveFactsFixtures.BACK_TITLE,
                        ObjectiveFactsFixtures.FRONT_TITLE,
                        ObjectiveFactsFixtures.FRONT_GAME_TEXT,
                        ObjectiveFactsFixtures.BACK_GAME_TEXT));
    }

    @Test
    public void unknownFactsStayUnknownAcrossTheSnapshotBoundary() {
        String reason = "objective fixture is unavailable";
        ObjectiveFacts facts = ObjectiveFacts.unknown(reason);

        assertUnknown(facts.identity(), reason);
        assertUnknown(facts.profileResolution(), reason);
        assertUnknown(facts.strategy(), reason);
        assertUnknown(facts.board(), reason);

        DecisionSnapshot snapshot = snapshot(facts);
        assertSame(facts, snapshot.objectiveFacts());
        assertUnknown(snapshot.decisionFacts().objectiveIdentity(), reason);
        assertUnknown(snapshot.decisionFacts().objectiveFlipped(), reason);
    }

    @Test
    public void nestedSetInputsAreDefensivelyCopiedAndUnmodifiable() {
        Set<String> mutableStrings = new LinkedHashSet<>(Set.of("seed"));
        Set<Integer> mutableCardIds = new LinkedHashSet<>(Set.of(7));
        ObjectiveFacts facts = ObjectiveFactsFixtures.factsFromMutableCollections(
                mutableStrings, mutableCardIds);

        mutableStrings.add("late mutation");
        mutableCardIds.add(8);

        ObjectiveFacts.StrategyFacts strategy = facts.strategy().value();
        ObjectiveFacts.StartingRefs starting = strategy.startingRefs();
        ObjectiveFacts.TypedBoardFacts board = facts.board().value();

        Set<?>[] frozenStrings = {
                strategy.flipConditionLocationFragments(),
                strategy.flipBackLocationFragments(),
                strategy.requiredCardsOnTable(),
                strategy.pullableCards(),
                strategy.flipCriticalControlCardIds(),
                strategy.strategyCharacterTokens(),
                starting.locationBlueprintIds(),
                starting.locationTitleFragments(),
                starting.effectBlueprintIds(),
                starting.effectTitleFragments(),
                starting.interruptBlueprintIds(),
                starting.interruptTitleFragments()
        };
        for (Set<?> frozen : frozenStrings) {
            assertFrozenSet(Set.of("seed"), frozen);
        }

        Set<?>[] frozenCardIds = {
                board.senatorCardIds(),
                board.jediSurvivorCardIds(),
                board.inquisitorCardIds(),
                board.inquisitorWithHatredCardIds(),
                board.galacticSenateLocationIds(),
                board.objectiveRelevantLocationIds(),
                board.flipBackProtectionLocationIds()
        };
        for (Set<?> frozen : frozenCardIds) {
            assertFrozenSet(Set.of(7), frozen);
        }
    }

    @Test
    public void profileResolutionPreservesEveryMatchKind() {
        ObjectiveFacts.ProfileResolution blueprint = ObjectiveFactsFixtures.profileResolution(
                ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID);
        ObjectiveFacts.ProfileResolution title = ObjectiveFactsFixtures.profileResolution(
                ObjectiveFacts.ProfileResolution.MatchKind.TITLE_COMPATIBILITY);
        ObjectiveFacts.ProfileResolution none = ObjectiveFactsFixtures.profileResolution(
                ObjectiveFacts.ProfileResolution.MatchKind.NONE);

        assertEquals(ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID,
                blueprint.matchKind());
        assertEquals(ObjectiveFacts.ProfileResolution.MatchKind.TITLE_COMPATIBILITY,
                title.matchKind());
        assertEquals(ObjectiveFacts.ProfileResolution.MatchKind.NONE, none.matchKind());
        assertFalse(blueprint.label().isBlank());
        assertFalse(title.label().isBlank());
        assertEquals("", none.label());

        ObjectiveFacts.ProfileResolution compiledFallback =
                new ObjectiveFacts.ProfileResolution(
                        ObjectiveFacts.ProfileResolution.MatchKind.NONE,
                        "compiled legacy profile",
                        false,
                        false,
                        true);
        assertTrue(compiledFallback.compiledFallbackUsed());
        assertFalse(compiledFallback.hydratedFromJson());
    }

    @Test
    public void profileResolutionRejectsMissingMatchLabelsAndImpossibleHydration() {
        assertIllegalArgument("a matched profile requires a label",
                () -> new ObjectiveFacts.ProfileResolution(
                        ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID,
                        "",
                        false,
                        false,
                        false));
        assertIllegalArgument("JSON hydration requires an enabled loader",
                () -> new ObjectiveFacts.ProfileResolution(
                        ObjectiveFacts.ProfileResolution.MatchKind.TITLE_COMPATIBILITY,
                        "compatibility profile",
                        false,
                        true,
                        false));
    }

    @Test
    public void hiddenPathTruthIsExactlyTheTypedNonMapuzoJediSiteCountLaw() {
        assertHiddenPathCount(0, false);
        assertHiddenPathCount(1, false);
        assertHiddenPathCount(2, true);
        assertHiddenPathCount(3, true);

        assertIllegalArgument("negative typed site counts are impossible",
                () -> ObjectiveFactsFixtures.boardWithHiddenPathCount(-1, false));
        assertIllegalArgument("one typed site cannot satisfy Hidden Path",
                () -> ObjectiveFactsFixtures.boardWithHiddenPathCount(1, true));
        assertIllegalArgument("two typed sites must satisfy Hidden Path",
                () -> ObjectiveFactsFixtures.boardWithHiddenPathCount(2, false));
    }

    @Test
    public void typedBoardSetsRetainTheirExactCategoriesAndIds() {
        ObjectiveFacts.TypedBoardFacts board = ObjectiveFactsFixtures.typedBoardFacts();

        assertEquals(ObjectiveFactsFixtures.SENATOR_CARD_IDS, board.senatorCardIds());
        assertEquals(ObjectiveFactsFixtures.JEDI_SURVIVOR_CARD_IDS,
                board.jediSurvivorCardIds());
        assertEquals(ObjectiveFactsFixtures.INQUISITOR_CARD_IDS, board.inquisitorCardIds());
        assertEquals(ObjectiveFactsFixtures.INQUISITOR_WITH_HATRED_CARD_IDS,
                board.inquisitorWithHatredCardIds());
        assertEquals(ObjectiveFactsFixtures.GALACTIC_SENATE_LOCATION_IDS,
                board.galacticSenateLocationIds());
        assertEquals(ObjectiveFactsFixtures.OBJECTIVE_RELEVANT_LOCATION_IDS,
                board.objectiveRelevantLocationIds());
        assertEquals(ObjectiveFactsFixtures.FLIP_BACK_PROTECTION_LOCATION_IDS,
                board.flipBackProtectionLocationIds());
        assertTrue(board.nonSenateSiteOnTable());
        assertEquals(2, board.hiddenPathNonMapuzoJediSiteCount());
        assertTrue(board.hiddenPathFlipConditionMet());
        assertFalse(board.invasionThroneRoomControlledWithNeimoidian());
        assertFalse(board.invasionNabooSystemControlled());
        assertFalse(board.controlsFlipCriticalSite());
    }

    @Test
    public void traceSnapshotRetainsTheExactObjectiveFactsInstance() {
        ObjectiveFacts facts = ObjectiveFactsFixtures.facts(true);

        DecisionSnapshot snapshot = snapshot(facts);

        assertSame(facts, snapshot.objectiveFacts());
        assertSame(facts.identity(), snapshot.objectiveFacts().identity());
        assertEquals(ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                snapshot.decisionFacts().objectiveIdentity().value());
        assertEquals(Boolean.TRUE, snapshot.decisionFacts().objectiveFlipped().value());
        assertEquals(ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                snapshot.objectiveFacts().identity().value().currentBlueprintId());
    }

    private static void assertCanonicalIdentity(ObjectiveFacts.Identity identity) {
        assertEquals(ObjectiveFactsFixtures.OBJECTIVE_PERMANENT_CARD_ID,
                identity.objectivePermanentCardId());
        assertEquals(ObjectiveFactsFixtures.OBJECTIVE_CURRENT_CARD_ID,
                identity.objectiveCurrentCardId());
        assertEquals(ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID,
                identity.canonicalFrontBlueprintId());
        assertEquals(ObjectiveFactsFixtures.BACK_BLUEPRINT_ID,
                identity.canonicalBackBlueprintId());
        assertEquals(ObjectiveFactsFixtures.FRONT_TITLE, identity.canonicalFrontTitle());
        assertEquals(ObjectiveFactsFixtures.BACK_TITLE, identity.canonicalBackTitle());
        assertEquals(ObjectiveFactsFixtures.FRONT_GAME_TEXT,
                identity.canonicalFrontGameText());
        assertEquals(ObjectiveFactsFixtures.BACK_GAME_TEXT,
                identity.canonicalBackGameText());
    }

    private static void assertOrientation(ObjectiveFacts.Identity identity,
                                          String currentBlueprintId,
                                          String oppositeBlueprintId,
                                          String currentTitle,
                                          String oppositeTitle,
                                          String currentGameText,
                                          String oppositeGameText,
                                          boolean flipped) {
        assertEquals(currentBlueprintId, identity.currentBlueprintId());
        assertEquals(oppositeBlueprintId, identity.oppositeBlueprintId());
        assertEquals(currentTitle, identity.currentTitle());
        assertEquals(oppositeTitle, identity.oppositeTitle());
        assertEquals(currentGameText, identity.currentGameText());
        assertEquals(oppositeGameText, identity.oppositeGameText());
        assertEquals(flipped, identity.flipped());
    }

    private static void assertHiddenPathCount(int count, boolean expectedTruth) {
        ObjectiveFacts.TypedBoardFacts board =
                ObjectiveFactsFixtures.boardWithHiddenPathCount(count, expectedTruth);
        assertEquals(count, board.hiddenPathNonMapuzoJediSiteCount());
        assertEquals(expectedTruth, board.hiddenPathFlipConditionMet());
    }

    private static void assertUnknown(FactValue<?> fact, String reason) {
        assertTrue(fact.isUnknown());
        assertFalse(fact.isKnown());
        assertEquals(ObjectiveFacts.PRODUCER, fact.producerId());
        assertEquals(reason, fact.unknownReason());
        try {
            fact.value();
            fail("UNKNOWN objective fact must not expose a fabricated value");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    private static PhysicalCard physicalObjective(boolean flipped) {
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        when(front.getTitle()).thenReturn(ObjectiveFactsFixtures.FRONT_TITLE);
        when(front.getGameText()).thenReturn(ObjectiveFactsFixtures.FRONT_GAME_TEXT);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);
        when(back.getTitle()).thenReturn(ObjectiveFactsFixtures.BACK_TITLE);
        when(back.getGameText()).thenReturn(ObjectiveFactsFixtures.BACK_GAME_TEXT);

        PhysicalCard card = mock(PhysicalCard.class);
        when(card.isDoubleSided()).thenReturn(true);
        when(card.getPermanentCardId()).thenReturn(
                ObjectiveFactsFixtures.OBJECTIVE_PERMANENT_CARD_ID);
        when(card.getCardId()).thenReturn(ObjectiveFactsFixtures.OBJECTIVE_CURRENT_CARD_ID);
        when(card.isFlipped()).thenReturn(flipped);
        when(card.getBlueprintId(true)).thenReturn(ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID);
        when(card.getBlueprintId(false)).thenReturn(flipped
                ? ObjectiveFactsFixtures.BACK_BLUEPRINT_ID
                : ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID);
        when(card.getOtherSideBlueprintId()).thenReturn(flipped
                ? ObjectiveFactsFixtures.FRONT_BLUEPRINT_ID
                : ObjectiveFactsFixtures.BACK_BLUEPRINT_ID);
        when(card.getBlueprint()).thenReturn(flipped ? back : front);
        when(card.getOtherSideBlueprint()).thenReturn(flipped ? front : back);
        return card;
    }

    private static void assertFrozenSet(Set<?> expected, Set<?> actual) {
        assertEquals(expected, actual);
        try {
            actual.clear();
            fail("ObjectiveFacts set components must be unmodifiable");
        } catch (UnsupportedOperationException expectedException) {
            // expected
        }
    }

    private static void assertIllegalArgument(String message, Runnable construction) {
        try {
            construction.run();
            fail(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
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
}
