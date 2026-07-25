package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Persona;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WeHaveAPlanObjectiveBehaviorTest {
    private static final String PLAYER_ID = "player";
    private static final String OPPONENT_ID = "opponent";

    @Test
    public void profileHydratesExactAmidalaGateAndSetupLocationsForBothFacades() {
        for (ObjectiveAnalyzer analyzer : List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer())) {
            Fixture fixture = fixture(analyzer, false, false, false);

            assertEquals("We Have A Plan",
                    analyzer.getActivePlaybook().label);
            assertEquals("naboo: theed palace throne room",
                    analyzer.getFlipCriticalControlSite());
            assertTrue(analyzer.hasFlipGateActorRequirement());
            assertFalse(analyzer.hasCountedPreFlipActorRule());
            assertTrue(analyzer.getStartingLocationIds().containsAll(
                    List.of("12_83", "12_174", "14_51", "14_112",
                            "201_17", "12_81", "12_172")));
            assertTrue(analyzer.matchesFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.padme));
            assertFalse(analyzer.matchesFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.panaka));
            assertFalse(analyzer.matchesFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.neimoidian));
            assertTrue(analyzer.isFlipGateLocation(
                    fixture.game, PLAYER_ID, fixture.throne));
            assertFalse(analyzer.isFlipGateLocation(
                    fixture.game, PLAYER_ID, fixture.hallway));
        }
    }

    @Test
    public void structuredFrontAndBackRulesUseOnlyExactSourceConditions() {
        Fixture empty = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, false, false);
        assertFalse(onlyState(empty, "preFlip", "flip")
                .conditionSatisfied());

        Fixture complete = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, true, false);
        assertTrue(onlyState(complete, "preFlip", "flip")
                .conditionSatisfied());

        Fixture backSafe = fixture(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer(),
                true, false, false);
        assertFalse(onlyState(backSafe, "postFlip", "flipBack")
                .conditionSatisfied());

        Fixture backLost = fixture(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer(),
                true, false, true);
        assertTrue(onlyState(backLost, "postFlip", "flipBack")
                .conditionSatisfied());
    }

    @Test
    public void countOnePullFindsAmidalaButRejectsWrongActorsAndDuplicates() {
        Fixture missing = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, false, false);
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR,
                missing.analyzer.classifyPreFlipProgressCandidate(
                        missing.game, PLAYER_ID, missing.padme));
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                missing.analyzer.classifyPreFlipProgressCandidate(
                        missing.game, PLAYER_ID, missing.panaka));
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                missing.analyzer.classifyPreFlipProgressCandidate(
                        missing.game, PLAYER_ID, missing.neimoidian));

        PhysicalCard duplicateInHand = amidala(
                missing.gameState, missing.modifiers, "Queen Amidala");
        when(missing.gameState.getHand(PLAYER_ID))
                .thenReturn(List.of(duplicateInHand));
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                missing.analyzer.classifyPreFlipProgressCandidate(
                        missing.game, PLAYER_ID, missing.padme));

        Fixture alreadyOnTable = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, true, false);
        assertEquals(
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                alreadyOnTable.analyzer.classifyPreFlipProgressCandidate(
                        alreadyOnTable.game, PLAYER_ID, alreadyOnTable.padme));
    }

    @Test
    public void actorRouteAcceptsOnlyDestinationsTowardTheExactGate() {
        Fixture fixture = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, false, false);

        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, fixture.padme)).thenReturn(fixture.courtyard);
        assertTrue(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme, fixture.hallway));
        assertTrue(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme, fixture.throne));
        assertFalse(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme, fixture.unrelated));
        assertFalse(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.panaka, fixture.hallway));

        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, fixture.padme)).thenReturn(fixture.hallway);
        assertTrue(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme, fixture.throne));
        assertFalse(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme, fixture.courtyard));

        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, fixture.padme)).thenReturn(fixture.throne);
        assertFalse(fixture.analyzer.advancesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme, fixture.throne));
    }

    @Test
    public void deployStagesOnlyTheTypedActorAtAnExactSetupRouteSite() {
        Fixture fixture = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, false, false);

        assertTrue(fixture.analyzer.stagesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme,
                fixture.courtyard));
        assertTrue(fixture.analyzer.stagesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme,
                fixture.hallway));
        assertFalse(fixture.analyzer.stagesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme,
                fixture.throne));
        assertFalse(fixture.analyzer.stagesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.padme,
                fixture.unrelated));
        assertFalse(fixture.analyzer.stagesPreFlipActorRoute(
                fixture.game, PLAYER_ID, fixture.panaka,
                fixture.courtyard));

        PhysicalCard alternateAmidala = amidala(
                fixture.gameState, fixture.modifiers, "Queen Amidala");
        when(fixture.gameState.getHand(PLAYER_ID))
                .thenReturn(List.of(fixture.padme, alternateAmidala));
        assertTrue("Copies in hand must not suppress every legal deployment",
                fixture.analyzer.stagesPreFlipActorRoute(
                        fixture.game, PLAYER_ID, fixture.padme,
                        fixture.courtyard));

        PhysicalCard reserveAmidala = amidala(
                fixture.gameState, fixture.modifiers,
                "Queen Amidala In Reserve");
        when(reserveAmidala.getZone()).thenReturn(Zone.RESERVE_DECK);
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(reserveAmidala));
        assertTrue("A different copy in Reserve Deck is not already in play",
                fixture.analyzer.stagesPreFlipActorRoute(
                        fixture.game, PLAYER_ID, fixture.padme,
                        fixture.courtyard));

        when(reserveAmidala.getZone()).thenReturn(Zone.AT_LOCATION);
        setActive(fixture.gameState, reserveAmidala, true);
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, reserveAmidala))
                .thenReturn(fixture.courtyard);
        assertFalse("An Amidala already in play closes duplicate staging",
                fixture.analyzer.stagesPreFlipActorRoute(
                        fixture.game, PLAYER_ID, fixture.padme,
                        fixture.courtyard));
        when(reserveAmidala.isCaptive()).thenReturn(true);
        setActive(fixture.gameState, reserveAmidala, false);
        assertTrue("A captive Amidala cannot execute the route",
                fixture.analyzer.stagesPreFlipActorRoute(
                        fixture.game, PLAYER_ID, fixture.padme,
                        fixture.courtyard));
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of());

        assertTrue(fixture.analyzer.hasLegalPreFlipActorRouteStage(
                fixture.game, PLAYER_ID, fixture.padme));
        List<ObjectiveAnalyzer.ScoreNote> padmeNotes =
                fixture.analyzer.getDeployObjectiveAdjustments(
                        fixture.game, fixture.gameState, PLAYER_ID,
                        fixture.padme, fixture.padme.getBlueprint(),
                        "Deploy Padme Naberrie");
        assertTrue(padmeNotes.stream().anyMatch(
                note -> note.score == 600.0f
                        && note.reason.contains("stage")));

        List<ObjectiveAnalyzer.ScoreNote> panakaNotes =
                fixture.analyzer.getDeployObjectiveAdjustments(
                        fixture.game, fixture.gameState, PLAYER_ID,
                        fixture.panaka, fixture.panaka.getBlueprint(),
                        "Deploy Captain Panaka");
        assertFalse(panakaNotes.stream().anyMatch(
                note -> note.reason.contains("stage")));

        setDeployable(fixture.modifiers, false);
        assertFalse(fixture.analyzer.hasLegalPreFlipActorRouteStage(
                fixture.game, PLAYER_ID, fixture.padme));
        assertFalse(fixture.analyzer.getDeployObjectiveAdjustments(
                        fixture.game, fixture.gameState, PLAYER_ID,
                        fixture.padme, fixture.padme.getBlueprint(),
                        "Deploy Padme Naberrie").stream()
                .anyMatch(note -> note.reason.contains("stage")));
    }

    @Test
    public void actorRouteClosesAfterFlipOrWhenTheGateActorIsAlreadyThere() {
        Fixture satisfied = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, true, false);
        when(satisfied.modifiers.getLocationThatCardIsPresentAt(
                satisfied.gameState, satisfied.padme))
                .thenReturn(satisfied.courtyard);
        assertFalse(satisfied.analyzer.advancesPreFlipActorRoute(
                satisfied.game, PLAYER_ID, satisfied.padme,
                satisfied.hallway));

        Fixture flipped = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                true, false, false);
        when(flipped.modifiers.getLocationThatCardIsPresentAt(
                flipped.gameState, flipped.padme)).thenReturn(flipped.courtyard);
        assertFalse(flipped.analyzer.advancesPreFlipActorRoute(
                flipped.game, PLAYER_ID, flipped.padme, flipped.hallway));
    }

    @Test
    public void lastAmidalaAtTheGateIsProtectedAsTheTypedFormationActor() {
        Fixture complete = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, true, false);

        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                complete.analyzer.classifyGateFormationPieceIfRemoved(
                        complete.game, PLAYER_ID, complete.deployedAmidala));
    }

    @Test
    public void forceLossProtectsTheExactAmidalaPersonaBeforeFlip() {
        Fixture fixture = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                false, false, false);
        when(fixture.padme.getZone()).thenReturn(Zone.HAND);
        when(fixture.gameState.getHand(PLAYER_ID))
                .thenReturn(List.of(fixture.padme));

        boolean requiredActor =
                fixture.analyzer.matchesFlipGateActorRequirement(
                        fixture.game, PLAYER_ID, fixture.padme);
        assertTrue(requiredActor);

        ForceLossFacts.DecisionFacts decision =
                new ForceLossFacts.DecisionFacts(
                        5, 10, 15, 0, 1, false);
        ForceLossFacts.CandidateFacts candidate =
                ForceLossFacts.readCandidate(
                        fixture.gameState, PLAYER_ID, fixture.padme);
        PolicyResult protectedLoss = ForceLossPolicy.score(
                "padme",
                ForceLossPolicy.Route.STANDALONE,
                decision, candidate,
                new ForceLossPolicy.ObjectiveFlags(
                        false, false, requiredActor, false));
        PolicyResult ordinaryLoss = ForceLossPolicy.score(
                "padme",
                ForceLossPolicy.Route.STANDALONE,
                decision, candidate,
                ForceLossPolicy.ObjectiveFlags.none());

        assertTrue(protectedLoss.operations().stream().anyMatch(
                operation -> operation.ruleArmId().id()
                        .equals("V21-objective")
                        && operation.delta() == -9999.0f));
        assertFalse(ordinaryLoss.operations().stream().anyMatch(
                operation -> operation.ruleArmId().id()
                        .equals("V21-objective")));
    }

    @Test
    public void battleTargetsOnlyTheExactMissingThroneRoomWhenSafe() {
        Fixture fixture = fixture(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer(),
                false, false, false);

        boolean exactTarget =
                fixture.analyzer.isPreFlipFlipRequirementLocation(
                        fixture.game, PLAYER_ID, fixture.throne);
        boolean missingControl =
                fixture.analyzer.isMissingPreFlipRequirementAt(
                        fixture.game, PLAYER_ID, fixture.throne);
        assertTrue(exactTarget);
        assertTrue(missingControl);

        PolicyResult safeContest = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-throne",
                        exactTarget, missingControl, true,
                        false, true,
                        0.0f, 5, 7.0f, 5.0f));
        assertEquals(1, safeContest.operations().size());
        assertEquals(
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                safeContest.operations().get(0).delta(), 0.0f);

        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-unrelated",
                        fixture.analyzer
                                .isPreFlipFlipRequirementLocation(
                                        fixture.game, PLAYER_ID,
                                        fixture.unrelated),
                        true, true, false, true,
                        0.0f, 5, 7.0f, 5.0f))
                .operations().isEmpty());
        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle-unsafe",
                        exactTarget, missingControl, true,
                        true, true,
                        0.0f, 5, 7.0f, 5.0f))
                .operations().isEmpty());
    }

    @Test
    public void postFlipForfeitProtectsOnlyTheSoleContestedThroneBlocker() {
        Fixture contested = fixture(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                true, false, false);
        when(contested.gameState.getAllPermanentCards())
                .thenReturn(List.of(contested.deployedAmidala));
        when(contested.modifiers.occupiesLocation(
                contested.gameState, contested.throne, OPPONENT_ID,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);

        boolean departureTriggersFlipBack =
                contested.analyzer.wouldDepartureTriggerFlipBack(
                        contested.game, PLAYER_ID,
                        contested.deployedAmidala);
        assertTrue(departureTriggersFlipBack);
        assertTrue(MoveObjectiveGateHoldPolicy
                .evaluatePostFlipBlocker(
                        departureTriggersFlipBack, 8.0f, 12.0f)
                .hardVeto());
        ObjectiveAnalyzer.FlipGateFormationRole contestedRole =
                contested.analyzer.classifyGateFormationPieceIfRemoved(
                        contested.game, PLAYER_ID,
                        contested.deployedAmidala);
        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                contestedRole);
        PolicyResult protectedForfeit =
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        "padme", contestedRole, true);
        assertTrue(protectedForfeit.operations().stream().anyMatch(
                operation -> operation.ruleArmId().id().equals(
                        "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD")
                        && operation.delta() == -9999.0f
                        && operation.reason().contains(
                                "sole flip-back blocker")));
        assertTrue(BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "padme", contestedRole, false)
                .operations().isEmpty());

        Fixture uncontested = fixture(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer(),
                true, false, false);
        when(uncontested.gameState.getAllPermanentCards())
                .thenReturn(List.of(uncontested.deployedAmidala));
        assertFalse(uncontested.analyzer.wouldDepartureTriggerFlipBack(
                uncontested.game, PLAYER_ID,
                uncontested.deployedAmidala));
        assertFalse(MoveObjectiveGateHoldPolicy
                .evaluatePostFlipBlocker(
                        uncontested.analyzer.wouldDepartureTriggerFlipBack(
                                uncontested.game, PLAYER_ID,
                                uncontested.deployedAmidala),
                        8.0f, 0.0f)
                .hardVeto());
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                uncontested.analyzer.classifyGateFormationPieceIfRemoved(
                        uncontested.game, PLAYER_ID,
                        uncontested.deployedAmidala));
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState onlyState(
            Fixture fixture, String phase, String purpose) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER_ID, phase, purpose);
        assertEquals(1, states.size());
        return states.get(0);
    }

    private static Fixture fixture(
            ObjectiveAnalyzer analyzer,
            boolean flipped,
            boolean completeFront,
            boolean opponentControlsThrone) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        setDeployable(modifiers, true);
        when(game.getOpponent(PLAYER_ID)).thenReturn(OPPONENT_ID);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER_ID)).thenReturn(OPPONENT_ID);
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of());
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn("14_52");
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn("We Have A Plan");
        when(front.getGameText()).thenReturn(
                "Deploy Theed Palace Throne Room, Hallway, and Courtyard. "
                        + "Flip this card if you control Theed Palace Throne "
                        + "Room with Amidala there.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn("They Will Be Lost And Confused");
        when(back.getGameText()).thenReturn(
                "Flip this card if opponent controls Theed Palace Throne Room.");

        PhysicalCard throne = site(
                "Naboo: Theed Palace Throne Room", 101);
        PhysicalCard hallway = site(
                "Naboo: Theed Palace Hallway", 102);
        PhysicalCard courtyard = site(
                "Naboo: Theed Palace Courtyard", 103);
        PhysicalCard unrelated = site("Tatooine: Mos Espa", 104);
        PhysicalCard padme = amidala(gameState, modifiers, "Padme Naberrie");
        PhysicalCard deployedAmidala =
                amidala(gameState, modifiers, "Queen Amidala");
        when(deployedAmidala.getZone()).thenReturn(Zone.AT_LOCATION);
        setActive(gameState, deployedAmidala, true);
        PhysicalCard panaka = character("Captain Panaka");
        PhysicalCard neimoidian = character("Daultay Dofine");

        when(modifiers.controlsLocation(
                gameState, throne, PLAYER_ID,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(completeFront);
        when(modifiers.controlsLocation(
                gameState, throne, OPPONENT_ID,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(opponentControlsThrone);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, deployedAmidala)).thenReturn(throne);

        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        if (completeFront) permanents.add(deployedAmidala);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(throne, hallway, courtyard, unrelated));
        when(gameState.getTopLocations()).thenReturn(
                List.of(throne, hallway, courtyard, unrelated));
        when(gameState.getCardsAtLocation(throne)).thenReturn(
                completeFront ? List.of(deployedAmidala) : List.of());
        when(gameState.getCardsAtLocation(hallway)).thenReturn(List.of());
        when(gameState.getCardsAtLocation(courtyard)).thenReturn(List.of());
        when(gameState.getCardsAtLocation(unrelated)).thenReturn(List.of());
        when(gameState.findCardByPermanentId(101)).thenReturn(throne);
        when(gameState.findCardByPermanentId(102)).thenReturn(hallway);
        when(gameState.findCardByPermanentId(103)).thenReturn(courtyard);
        when(gameState.findCardByPermanentId(104)).thenReturn(unrelated);
        when(modifiers.getLocationHere(gameState, throne))
                .thenReturn(throne);
        when(modifiers.getLocationHere(gameState, hallway))
                .thenReturn(hallway);
        when(modifiers.getLocationHere(gameState, courtyard))
                .thenReturn(courtyard);
        when(modifiers.getLocationHere(gameState, unrelated))
                .thenReturn(unrelated);
        when(modifiers.getSitesBetween(gameState, courtyard, throne))
                .thenReturn(List.of(hallway));
        when(modifiers.getSitesBetween(gameState, throne, courtyard))
                .thenReturn(List.of(hallway));
        when(modifiers.getSitesBetween(gameState, hallway, throne))
                .thenReturn(List.of());
        when(modifiers.getSitesBetween(gameState, throne, hallway))
                .thenReturn(List.of());

        distance(modifiers, gameState, courtyard, hallway, 1);
        distance(modifiers, gameState, hallway, throne, 1);
        distance(modifiers, gameState, courtyard, throne, 2);
        distance(modifiers, gameState, throne, throne, 0);
        distance(modifiers, gameState, hallway, hallway, 0);
        distance(modifiers, gameState, courtyard, courtyard, 0);

        analyzer.analyze(game, PLAYER_ID, Side.LIGHT);
        return new Fixture(
                analyzer, game, gameState, modifiers,
                throne, hallway, courtyard, unrelated,
                padme, deployedAmidala, panaka, neimoidian);
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

    private static void distance(
            ModifiersQuerying modifiers,
            GameState gameState,
            PhysicalCard first,
            PhysicalCard second,
            int distance) {
        when(modifiers.getDistanceBetweenSites(gameState, first, second))
                .thenReturn(distance);
        when(modifiers.getDistanceBetweenSites(gameState, second, first))
                .thenReturn(distance);
    }

    private static void setDeployable(
            ModifiersQuerying modifiers, boolean deployable) {
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
    }

    private static PhysicalCard site(String title, int permanentId) {
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(site.getTitle()).thenReturn(title);
        when(site.getTitles()).thenReturn(List.of(title));
        when(site.getPermanentCardId()).thenReturn(permanentId);
        when(site.getCardId()).thenReturn(permanentId);
        when(site.getAdditionalCardIds()).thenReturn(List.of());
        when(site.getBlueprint()).thenReturn(blueprint);
        when(site.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        return site;
    }

    private static PhysicalCard amidala(
            GameState gameState,
            ModifiersQuerying modifiers,
            String title) {
        PhysicalCard card = character(title);
        when(modifiers.hasPersona(gameState, card, Persona.AMIDALA))
                .thenReturn(true);
        when(modifiers.hasAbility(gameState, card, true))
                .thenReturn(true);
        return card;
    }

    private static PhysicalCard character(String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(PLAYER_ID);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isUndercover()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(3.0f);
        return card;
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard throne,
            PhysicalCard hallway,
            PhysicalCard courtyard,
            PhysicalCard unrelated,
            PhysicalCard padme,
            PhysicalCard deployedAmidala,
            PhysicalCard panaka,
            PhysicalCard neimoidian) { }
}
