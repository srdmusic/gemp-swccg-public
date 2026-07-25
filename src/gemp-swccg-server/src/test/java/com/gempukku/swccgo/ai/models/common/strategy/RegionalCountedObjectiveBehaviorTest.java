package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegionalCountedObjectiveBehaviorTest {
    private static final String PLAYER_ID = "player";
    private static final String OPPONENT_ID = "opponent";

    private static final List<ObjectiveFamily> FAMILIES = List.of(
            new ObjectiveFamily(
                    "Ralltiir Operations",
                    "In The Hands Of The Empire",
                    "7_300",
                    "Ralltiir",
                    "Imperials",
                    Icon.IMPERIAL,
                    Side.DARK),
            new ObjectiveFamily(
                    "Dantooine Base Operations",
                    "More Dangerous Than You Realize",
                    "7_135",
                    "Dantooine",
                    "Rebels",
                    Icon.REBEL,
                    Side.LIGHT));

    @Test
    public void frontRuleRequiresThreeActorQualifiedControlledSitesAndNoOpponentControl() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture complete = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1, 2),
                    Set.of());
            assertTrue(family.frontTitle,
                    onlyState(complete, "preFlip", "flip")
                            .conditionSatisfied());

            Fixture missingActor = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1),
                    Set.of());
            assertFalse(family.frontTitle,
                    onlyState(missingActor, "preFlip", "flip")
                            .conditionSatisfied());

            Fixture opponentPresent = fixture(
                    family, false, 4,
                    Set.of(0, 1, 2),
                    Set.of(0, 1, 2),
                    Set.of(3));
            assertFalse(family.frontTitle,
                    onlyState(opponentPresent, "preFlip", "flip")
                            .conditionSatisfied());
        }
    }

    @Test
    public void backRuleRequiresOpponentControlOfTwoRegionalLocations() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture oneLocation = fixture(
                    family, true, 3,
                    Set.of(),
                    Set.of(),
                    Set.of(0));
            assertFalse(family.backTitle,
                    onlyState(oneLocation, "postFlip", "flipBack")
                            .conditionSatisfied());

            Fixture twoLocations = fixture(
                    family, true, 3,
                    Set.of(),
                    Set.of(),
                    Set.of(0, 1));
            assertTrue(family.backTitle,
                    onlyState(twoLocations, "postFlip", "flipBack")
                            .conditionSatisfied());
        }
    }

    @Test
    public void progressCandidateClassificationDistinguishesMissingSiteAndActor() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture twoSites = fixture(
                    family, false, 2,
                    Set.of(0, 1),
                    Set.of(0, 1),
                    Set.of());
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_LOCATION,
                    twoSites.analyzer.classifyPreFlipProgressCandidate(
                            twoSites.game, PLAYER_ID,
                            twoSites.relatedSiteCandidate));
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    twoSites.analyzer.classifyPreFlipProgressCandidate(
                            twoSites.game, PLAYER_ID,
                            twoSites.unrelatedSite));

            Fixture actorMissing = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1),
                    Set.of());
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR,
                    actorMissing.analyzer.classifyPreFlipProgressCandidate(
                            actorMissing.game, PLAYER_ID,
                            actorMissing.typedActorCandidate));
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    actorMissing.analyzer.classifyPreFlipProgressCandidate(
                            actorMissing.game, PLAYER_ID,
                            actorMissing.wrongActorCandidate));

            Fixture complete = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1, 2),
                    Set.of());
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    complete.analyzer.classifyPreFlipProgressCandidate(
                            complete.game, PLAYER_ID,
                            complete.typedActorCandidate));
        }
    }

    @Test
    public void actorAdvanceIsExactToActorTypeSiteAndUnmetThreshold() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture actorMissing = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1),
                    Set.of());

            assertTrue(family.frontTitle,
                    actorMissing.analyzer.advancesPreFlipRequirementAt(
                            actorMissing.game, PLAYER_ID,
                            actorMissing.typedActorCandidate,
                            actorMissing.sites.get(2)));
            assertFalse(family.frontTitle,
                    actorMissing.analyzer.advancesPreFlipRequirementAt(
                            actorMissing.game, PLAYER_ID,
                            actorMissing.typedActorCandidate,
                            actorMissing.sites.get(0)));
            assertFalse(family.frontTitle,
                    actorMissing.analyzer.advancesPreFlipRequirementAt(
                            actorMissing.game, PLAYER_ID,
                            actorMissing.wrongActorCandidate,
                            actorMissing.sites.get(2)));
            assertFalse(family.frontTitle,
                    actorMissing.analyzer.advancesPreFlipRequirementAt(
                            actorMissing.game, PLAYER_ID,
                            actorMissing.typedActorCandidate,
                            actorMissing.unrelatedSite));

            Fixture thresholdAlreadyMet = fixture(
                    family, false, 4,
                    Set.of(0, 1, 2, 3),
                    Set.of(0, 1, 2),
                    Set.of());
            assertFalse(family.frontTitle,
                    thresholdAlreadyMet.analyzer.advancesPreFlipRequirementAt(
                            thresholdAlreadyMet.game, PLAYER_ID,
                            thresholdAlreadyMet.typedActorCandidate,
                            thresholdAlreadyMet.sites.get(3)));
        }
    }

    @Test
    public void formationProtectionPreservesPartialAndExactProgressButReleasesRedundancy() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture oneQualified = fixture(
                    family, false, 1,
                    Set.of(0),
                    Set.of(0),
                    Set.of());
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                    oneQualified.analyzer.classifyGateFormationPieceIfRemoved(
                            oneQualified.game, PLAYER_ID,
                            oneQualified.deployedActors.get(0)));

            Fixture twoQualified = fixture(
                    family, false, 2,
                    Set.of(0, 1),
                    Set.of(0, 1),
                    Set.of());
            ObjectiveAnalyzer.FlipGateFormationRole partialRole =
                    twoQualified.analyzer.classifyGateFormationPieceIfRemoved(
                            twoQualified.game, PLAYER_ID,
                            twoQualified.deployedActors.get(0));
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                    partialRole);
            var partialHold =
                    MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                            true, partialRole, 5.0f, 0.0f);
            assertTrue(family.frontTitle, partialHold.hardVeto());

            Fixture exactlyThree = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1, 2),
                    Set.of());
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                    exactlyThree.analyzer.classifyGateFormationPieceIfRemoved(
                            exactlyThree.game, PLAYER_ID,
                            exactlyThree.deployedActors.get(0)));

            Fixture fourQualified = fixture(
                    family, false, 4,
                    Set.of(0, 1, 2, 3),
                    Set.of(0, 1, 2, 3),
                    Set.of());
            assertEquals(family.frontTitle,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                    fourQualified.analyzer.classifyGateFormationPieceIfRemoved(
                            fourQualified.game, PLAYER_ID,
                            fourQualified.deployedActors.get(0)));
        }
    }

    @Test
    public void missingRequirementIdentifiesOnlyAQualificationThatAdvancesTheCount() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture oneActorMissing = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1),
                    Set.of());
            assertFalse(family.frontTitle,
                    oneActorMissing.analyzer.isMissingPreFlipRequirementAt(
                            oneActorMissing.game, PLAYER_ID,
                            oneActorMissing.sites.get(0)));
            assertFalse(family.frontTitle,
                    oneActorMissing.analyzer.isMissingPreFlipRequirementAt(
                            oneActorMissing.game, PLAYER_ID,
                            oneActorMissing.sites.get(1)));
            assertTrue(family.frontTitle,
                    oneActorMissing.analyzer.isMissingPreFlipRequirementAt(
                            oneActorMissing.game, PLAYER_ID,
                            oneActorMissing.sites.get(2)));
            assertFalse(family.frontTitle,
                    oneActorMissing.analyzer.isMissingPreFlipRequirementAt(
                            oneActorMissing.game, PLAYER_ID,
                            oneActorMissing.unrelatedSite));

            Fixture thresholdAlreadyMet = fixture(
                    family, false, 4,
                    Set.of(0, 1, 2, 3),
                    Set.of(0, 1, 2),
                    Set.of());
            assertFalse(family.frontTitle,
                    thresholdAlreadyMet.analyzer.isMissingPreFlipRequirementAt(
                            thresholdAlreadyMet.game, PLAYER_ID,
                            thresholdAlreadyMet.sites.get(3)));
        }
    }

    @Test
    public void battleBonusTargetsOnlyTheSafeMissingQualifiedSite() {
        for (ObjectiveFamily family : FAMILIES) {
            Fixture oneActorMissing = fixture(
                    family, false, 3,
                    Set.of(0, 1, 2),
                    Set.of(0, 1),
                    Set.of());
            PhysicalCard missingSite = oneActorMissing.sites.get(2);

            var safeContest = ObjectiveBattlePolicy.evaluate(
                    new ObjectiveBattlePolicy.Facts(
                            "battle-" + family.blueprintId,
                            oneActorMissing.analyzer
                                    .isPreFlipFlipRequirementLocation(
                                            oneActorMissing.game,
                                            PLAYER_ID,
                                            missingSite),
                            oneActorMissing.analyzer
                                    .isMissingPreFlipRequirementAt(
                                            oneActorMissing.game,
                                            PLAYER_ID,
                                            missingSite),
                            true, false, true,
                            0.0f, 5, 7.0f, 5.0f));

            assertEquals(family.frontTitle, 1,
                    safeContest.operations().size());
            assertEquals(family.frontTitle,
                    ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                    safeContest.operations().get(0).delta(), 0.0f);

            PhysicalCard alreadyQualified = oneActorMissing.sites.get(0);
            assertTrue(family.frontTitle,
                    ObjectiveBattlePolicy.evaluate(
                            new ObjectiveBattlePolicy.Facts(
                                    "battle-qualified-" + family.blueprintId,
                                    true,
                                    oneActorMissing.analyzer
                                            .isMissingPreFlipRequirementAt(
                                                    oneActorMissing.game,
                                                    PLAYER_ID,
                                                    alreadyQualified),
                                    true, false, true,
                                    0.0f, 5, 7.0f, 5.0f))
                            .operations().isEmpty());
        }
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState onlyState(
            Fixture fixture, String phase, String purpose) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER_ID, phase, purpose);
        assertEquals(fixture.family.frontTitle, 1, states.size());
        return states.get(0);
    }

    private static Fixture fixture(
            ObjectiveFamily family,
            boolean flipped,
            int siteCount,
            Set<Integer> selfControlled,
            Set<Integer> actorQualified,
            Set<Integer> opponentControlled) {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER_ID)).thenReturn(OPPONENT_ID);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER_ID)).thenReturn(OPPONENT_ID);
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn(family.blueprintId);
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn(family.frontTitle);
        when(front.getGameText()).thenReturn(frontGameText(family));
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn(family.backTitle);
        when(back.getGameText()).thenReturn(backGameText(family));

        List<PhysicalCard> sites = new ArrayList<>();
        List<PhysicalCard> deployedActors = new ArrayList<>();
        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        for (int i = 0; i < siteCount; i++) {
            PhysicalCard site = site(
                    family.systemName + ": Site " + (i + 1),
                    family.systemName);
            sites.add(site);
            when(modifiers.controlsLocation(
                    gameState, site, PLAYER_ID,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(selfControlled.contains(i));
            when(modifiers.controlsLocation(
                    gameState, site, OPPONENT_ID,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(opponentControlled.contains(i));

            if (actorQualified.contains(i)) {
                PhysicalCard actor = actor(
                        gameState, modifiers, family.actorIcon, PLAYER_ID);
                when(actor.getZone()).thenReturn(Zone.AT_LOCATION);
                setActive(gameState, actor, true);
                deployedActors.add(actor);
                permanents.add(actor);
                when(modifiers.getLocationThatCardIsPresentAt(
                        gameState, actor)).thenReturn(site);
                when(gameState.getCardsAtLocation(site))
                        .thenReturn(List.of(actor));
            } else {
                when(gameState.getCardsAtLocation(site))
                        .thenReturn(List.of());
            }
        }

        PhysicalCard relatedSiteCandidate = site(
                family.systemName + ": Candidate Site",
                family.systemName);
        PhysicalCard unrelatedSite = site(
                "Tatooine: Unrelated Site", "Tatooine");
        PhysicalCard typedActorCandidate = actor(
                gameState, modifiers, family.actorIcon, PLAYER_ID);
        PhysicalCard wrongActorCandidate = actor(
                gameState, modifiers, family.actorIcon, PLAYER_ID);
        when(modifiers.hasIcon(
                gameState, wrongActorCandidate, family.actorIcon))
                .thenReturn(false);

        List<PhysicalCard> locations = new ArrayList<>(sites);
        locations.add(unrelatedSite);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);
        when(gameState.getCardsAtLocation(unrelatedSite)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(permanents);

        analyzer.analyze(game, PLAYER_ID, family.side);
        return new Fixture(
                family,
                analyzer,
                game,
                sites,
                deployedActors,
                relatedSiteCandidate,
                unrelatedSite,
                typedActorCandidate,
                wrongActorCandidate);
    }

    private static void setActive(
            GameState gameState, PhysicalCard card, boolean active) {
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(active);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(active);
    }

    private static PhysicalCard site(String title, String systemName) {
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(site.getTitle()).thenReturn(title);
        when(site.getTitles()).thenReturn(List.of(title));
        when(site.getPartOfSystem()).thenReturn(systemName);
        when(site.getBlueprint()).thenReturn(blueprint);
        when(site.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        return site;
    }

    private static PhysicalCard actor(
            GameState gameState,
            ModifiersQuerying modifiers,
            Icon actorIcon,
            String owner) {
        PhysicalCard actor = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(actor.getOwner()).thenReturn(owner);
        when(actor.isUndercover()).thenReturn(false);
        when(actor.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(1.0f);
        when(modifiers.hasIcon(gameState, actor, actorIcon)).thenReturn(true);
        when(modifiers.hasAbility(gameState, actor, true)).thenReturn(true);
        return actor;
    }

    private static String frontGameText(ObjectiveFamily family) {
        return "Deploy " + family.systemName + " system. Flip this card if "
                + family.actorLabel + " control at least three "
                + family.systemName + " sites and opponent controls no "
                + family.systemName + " locations.";
    }

    private static String backGameText(ObjectiveFamily family) {
        return "Flip this card if opponent controls at least two "
                + family.systemName + " locations.";
    }

    private record ObjectiveFamily(
            String frontTitle,
            String backTitle,
            String blueprintId,
            String systemName,
            String actorLabel,
            Icon actorIcon,
            Side side) { }

    private record Fixture(
            ObjectiveFamily family,
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            List<PhysicalCard> sites,
            List<PhysicalCard> deployedActors,
            PhysicalCard relatedSiteCandidate,
            PhysicalCard unrelatedSite,
            PhysicalCard typedActorCandidate,
            PhysicalCard wrongActorCandidate) { }
}
