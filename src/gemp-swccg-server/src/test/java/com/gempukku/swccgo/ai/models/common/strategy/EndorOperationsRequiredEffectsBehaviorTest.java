package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;
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

public class EndorOperationsRequiredEffectsBehaviorTest {
    private static final String PLAYER_ID = "player";
    private static final String OMINOUS_RUMORS = "Ominous Rumors";
    private static final String ESTABLISH_SECRET_BASE =
            "Establish Secret Base";

    @Test
    public void bothActiveRequiredEffectsSatisfyTheFrontForBothBots() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false);
            addInPlayEffect(fixture, OMINOUS_RUMORS, true);
            addInPlayEffect(fixture, ESTABLISH_SECRET_BASE, true);
            PhysicalCard unrelated = reserveEffect(
                    fixture, "An Unrelated Effect");

            ObjectiveProgressAssessment assessment =
                    fixture.analyzer().assessDeployChild(
                            fixture.game(), PLAYER_ID, unrelated,
                            fixture.destination());

            assertTrue(label(bot), fixture.analyzer()
                    .isRequiredCardActiveOnTable(
                            fixture.game(), OMINOUS_RUMORS));
            assertTrue(label(bot), fixture.analyzer()
                    .isRequiredCardActiveOnTable(
                            fixture.game(), ESTABLISH_SECRET_BASE));
            assertEquals(label(bot),
                    ObjectiveProgressAssessment.Outcome.NEUTRAL,
                    assessment.outcome());
            assertEquals(label(bot),
                    Set.of("ominous rumors", "establish secret base"),
                    assessment.satisfiedRequirements());
            assertTrue(label(bot), assessment.missingRequirements().isEmpty());
            assertTrue(label(bot), assessment.advancedRequirements().isEmpty());
        }
    }

    @Test
    public void inactiveRequiredEffectRemainsMissingAndExactCandidateCompletes() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false);
            addInPlayEffect(fixture, OMINOUS_RUMORS, true);
            addInPlayEffect(fixture, ESTABLISH_SECRET_BASE, false);
            PhysicalCard candidate = reserveEffect(
                    fixture, ESTABLISH_SECRET_BASE);

            assertTrue(label(bot), Filters.Establish_Secret_Base.accepts(
                    fixture.gameState(),
                    fixture.game().getModifiersQuerying(), candidate));
            List<ObjectiveAnalyzer.FlipLocationRuleState> frontStates =
                    fixture.analyzer().assessFlipLocationRules(
                            fixture.game(), PLAYER_ID, "preFlip", "flip");
            assertEquals(label(bot), 1, frontStates.size());
            assertFalse(label(bot), frontStates.get(0).conditionSatisfied());
            assertFalse(label(bot), fixture.analyzer()
                    .isRequiredCardActiveOnTable(
                            fixture.game(), ESTABLISH_SECRET_BASE));
            assertEquals(label(bot),
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_ON_TABLE_CARD,
                    fixture.analyzer().classifyPreFlipProgressCandidate(
                            fixture.game(), PLAYER_ID, candidate));

            ObjectiveProgressAssessment assessment =
                    fixture.analyzer().assessDeployChild(
                            fixture.game(), PLAYER_ID, candidate,
                            fixture.destination());
            assertEquals(label(bot),
                    ObjectiveProgressAssessment.Outcome.COMPLETES_FLIP_NOW,
                    assessment.outcome());
            assertEquals(label(bot), Set.of("ominous rumors"),
                    assessment.satisfiedRequirements());
            assertEquals(label(bot), Set.of("establish secret base"),
                    assessment.missingRequirements());
            assertEquals(label(bot), Set.of("establish secret base"),
                    assessment.advancedRequirements());
        }
    }

    @Test
    public void exactMissingRequiredEffectIsClassifiedAsObjectiveProgress() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false);
            PhysicalCard candidate = reserveEffect(
                    fixture, OMINOUS_RUMORS);

            assertEquals(label(bot),
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_ON_TABLE_CARD,
                    fixture.analyzer().classifyPreFlipProgressCandidate(
                            fixture.game(), PLAYER_ID, candidate));

            ObjectiveProgressAssessment assessment =
                    fixture.analyzer().assessDeployChild(
                            fixture.game(), PLAYER_ID, candidate,
                            fixture.destination());
            assertEquals(label(bot),
                    ObjectiveProgressAssessment.Outcome
                            .ADVANCES_MISSING_REQUIREMENT,
                    assessment.outcome());
            assertTrue(label(bot),
                    assessment.satisfiedRequirements().isEmpty());
            assertEquals(label(bot),
                    Set.of("ominous rumors", "establish secret base"),
                    assessment.missingRequirements());
            assertEquals(label(bot), Set.of("ominous rumors"),
                    assessment.advancedRequirements());
        }
    }

    @Test
    public void postFlipRequiredEffectsAreSoleBlockersButDuplicatesAreNot() {
        for (Bot bot : Bot.values()) {
            Fixture soleCopies = fixture(bot, true);
            PhysicalCard ominous = addInPlayEffect(
                    soleCopies, OMINOUS_RUMORS, true);
            PhysicalCard establish = addInPlayEffect(
                    soleCopies, ESTABLISH_SECRET_BASE, true);

            assertTrue(label(bot), soleCopies.analyzer().isFlipped());
            List<ObjectiveAnalyzer.FlipLocationRuleState> backStates =
                    soleCopies.analyzer().assessFlipLocationRules(
                            soleCopies.game(), PLAYER_ID,
                            "postFlip", "flipBack");
            assertEquals(label(bot), 1, backStates.size());
            assertFalse(label(bot), backStates.get(0).conditionSatisfied());
            assertEquals(label(bot),
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_FLIP_BACK_BLOCKER,
                    soleCopies.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    soleCopies.game(), PLAYER_ID, ominous));
            assertEquals(label(bot),
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_FLIP_BACK_BLOCKER,
                    soleCopies.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    soleCopies.game(), PLAYER_ID, establish));

            Fixture duplicate = fixture(bot, true);
            PhysicalCard firstOminous = addInPlayEffect(
                    duplicate, OMINOUS_RUMORS, true);
            addInPlayEffect(duplicate, OMINOUS_RUMORS, true);
            PhysicalCard soleEstablish = addInPlayEffect(
                    duplicate, ESTABLISH_SECRET_BASE, true);

            assertEquals(label(bot),
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                    duplicate.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    duplicate.game(), PLAYER_ID,
                                    firstOminous));
            assertEquals(label(bot),
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_FLIP_BACK_BLOCKER,
                    duplicate.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    duplicate.game(), PLAYER_ID,
                                    soleEstablish));
        }
    }

    @Test
    public void endorLocationHardLossDefenderRoleWorksOnBothSidesForBothBots() {
        for (Bot bot : Bot.values()) {
            for (boolean flipped : List.of(false, true)) {
                Fixture fixture = fixture(bot, flipped);
                PhysicalCard defender = addPresenceSourceAt(
                        fixture, "Biker Scout Trooper");

                assertTrue(stateLabel(bot, flipped),
                        fixture.analyzer().isObjectiveHardLossLocation(
                                fixture.game(), PLAYER_ID,
                                fixture.destination()));
                assertFalse(stateLabel(bot, flipped),
                        fixture.analyzer()
                                .isObjectiveHardLossDefenseLocation(
                                        fixture.game(), PLAYER_ID,
                                        fixture.destination()));
                assertEquals(stateLabel(bot, flipped),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER_ID,
                                        defender));

                addHardLossThreat(fixture);
                assertTrue(stateLabel(bot, flipped),
                        fixture.analyzer()
                                .isObjectiveHardLossDefenseLocation(
                                        fixture.game(), PLAYER_ID,
                                        fixture.destination()));
                assertEquals(stateLabel(bot, flipped),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .HARD_LOSS_LOCATION_DEFENDER,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER_ID,
                                        defender));

                PhysicalCard reinforcement = addPresenceSourceAt(
                        fixture, "Corporal Avarik");
                assertEquals(stateLabel(bot, flipped),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER_ID,
                                        defender));
                assertEquals(stateLabel(bot, flipped),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER_ID,
                                        reinforcement));
            }
        }
    }

    @Test
    public void hardLossDefenderPoliciesHoldDefensibleRetreatAndAvoidableForfeit() {
        ObjectiveAnalyzer.FlipGateFormationRole role =
                ObjectiveAnalyzer.FlipGateFormationRole
                        .HARD_LOSS_LOCATION_DEFENDER;

        MoveObjectiveGateHoldPolicy.Evaluation held =
                MoveObjectiveGateHoldPolicy
                        .evaluateHardLossLocationDefender(
                                role, 8.0f, 14.0f);
        MoveObjectiveGateHoldPolicy.Evaluation retreat =
                MoveObjectiveGateHoldPolicy
                        .evaluateHardLossLocationDefender(
                                role, 8.0f, 15.0f);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch
                        .HOLD_HARD_LOSS_DEFENDER,
                held.branch());
        assertTrue(held.hardVeto());
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE,
                retreat.branch());
        assertFalse(retreat.hardVeto());

        var avoidable = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "endor-defender", role, true);
        assertEquals(1, avoidable.operations().size());
        var protection = avoidable.operations().get(0);
        assertEquals("BATTLE.OBJECTIVE.HARD_LOSS_LOCATION_HOLD",
                protection.ruleArmId().id());
        assertEquals(-9999.0f, protection.delta(), 0.0f);
        assertTrue(protection.reason().contains(
                "sole defender of a terminal-loss location"));
        assertTrue(BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                        "endor-defender", role, false)
                .operations().isEmpty());
    }

    private static Fixture fixture(Bot bot, boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        List<PhysicalCard> permanents = new ArrayList<>();
        PhysicalCard objective = objective(flipped);
        PhysicalCard destination = card(
                "Endor: Bunker", CardCategory.LOCATION, PLAYER_ID,
                Zone.LOCATIONS);
        when(destination.getPartOfSystem()).thenReturn("Endor");
        permanents.add(objective);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getTopLocations()).thenReturn(
                List.of(destination));
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of());
        when(gameState.getCardPile(
                PLAYER_ID, Zone.RESERVE_DECK)).thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());

        ObjectiveAnalyzer analyzer = bot.analyzer();
        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Fixture(
                analyzer, game, gameState, permanents, destination);
    }

    private static PhysicalCard addInPlayEffect(
            Fixture fixture, String title, boolean active) {
        PhysicalCard card = card(
                title, CardCategory.EFFECT, PLAYER_ID,
                Zone.SIDE_OF_TABLE);
        fixture.permanents().add(card);
        setActive(fixture.gameState(), card, active);
        return card;
    }

    private static PhysicalCard reserveEffect(
            Fixture fixture, String title) {
        PhysicalCard candidate = card(
                title, CardCategory.EFFECT, PLAYER_ID,
                Zone.RESERVE_DECK);
        when(fixture.gameState().getCardPile(
                PLAYER_ID, Zone.RESERVE_DECK))
                .thenReturn(List.of(candidate));
        return candidate;
    }

    private static PhysicalCard addPresenceSourceAt(
            Fixture fixture, String title) {
        PhysicalCard card = card(
                title, CardCategory.CHARACTER, PLAYER_ID,
                Zone.AT_LOCATION);
        fixture.permanents().add(card);
        ModifiersQuerying modifiers =
                fixture.game().getModifiersQuerying();
        when(modifiers.hasAbility(
                fixture.gameState(), card, true)).thenReturn(true);
        when(modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState(), card))
                .thenReturn(fixture.destination());
        return card;
    }

    private static PhysicalCard addHardLossThreat(
            Fixture fixture) {
        PhysicalCard threat = card(
                "Deactivate The Shield Generator",
                CardCategory.EPIC_EVENT, "opponent",
                Zone.ATTACHED);
        when(threat.getAttachedTo())
                .thenReturn(fixture.destination());
        fixture.permanents().add(threat);
        setActive(fixture.gameState(), threat, true);
        return threat;
    }

    private static PhysicalCard objective(boolean flipped) {
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn("8_167");
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn("Endor Operations");
        when(front.getGameText()).thenReturn(
                "Deploy Endor system, Bunker and Landing Platform. "
                        + "Flip this card if Ominous Rumors and "
                        + "Establish Secret Base are both on table.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn("Imperial Outpost");
        when(back.getGameText()).thenReturn(
                "Flip this card if Ominous Rumors and Establish "
                        + "Secret Base are not both on table.");
        when(back.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        return objective;
    }

    private static PhysicalCard card(
            String title, CardCategory category, String owner, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isBlownAway()).thenReturn(false);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
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

    private static String label(Bot bot) {
        return "Endor Operations " + bot;
    }

    private static String stateLabel(Bot bot, boolean flipped) {
        return label(bot) + (flipped ? " back" : " front");
    }

    private enum Bot {
        RANDO {
            @Override
            ObjectiveAnalyzer analyzer() {
                return new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
            }
        },
        CHOSEN_ONE {
            @Override
            ObjectiveAnalyzer analyzer() {
                return new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
            }
        };

        abstract ObjectiveAnalyzer analyzer();
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            List<PhysicalCard> permanents,
            PhysicalCard destination) { }
}
