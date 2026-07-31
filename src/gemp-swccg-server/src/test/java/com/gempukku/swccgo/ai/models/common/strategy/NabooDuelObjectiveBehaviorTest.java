package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shared AI behavior contract for the mirrored Reflections III Naboo duels. */
public class NabooDuelObjectiveBehaviorTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    private static final List<Family> FAMILIES = List.of(
            new Family(
                    "We'll Handle This", "13_46", "13_46_BACK",
                    Side.LIGHT, "13_32", "13_31", "220_8", "13_24",
                    "1_21", "1_168", "1_28"),
            new Family(
                    "Let Them Make The First Move", "13_73", "13_73_BACK",
                    Side.DARK, "13_77", "13_76", null, "13_65",
                    "1_168", "1_21", "1_194"));

    @Test
    public void profilesHydrateSetupAndExactOpponentDrivenFlipLaw() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, false);

                assertTrue(label(bot, family), fixture.analyzer.isHydratedFromJson());
                assertNotNull(label(bot, family), fixture.analyzer.getActivePlaybook());
                assertEquals(label(bot, family), family.frontTitle,
                        fixture.analyzer.getActivePlaybook().label);
                assertTrue(label(bot, family),
                        fixture.analyzer.getStartingLocationIds().contains(family.core));
                assertTrue(label(bot, family),
                        fixture.analyzer.getStartingLocationIds().contains(family.generator));
                if (family.alternateGenerator != null) {
                    assertTrue(label(bot, family),
                            fixture.analyzer.getStartingLocationIds()
                                    .contains(family.alternateGenerator));
                }
                assertTrue(label(bot, family),
                        fixture.analyzer.getStartingEffectIds().contains(family.duelEvent));
                assertTrue(label(bot, family), fixture.analyzer
                        .isStrategyKeyCharacter(
                                fixture.game, PLAYER, fixture.ownDuelist));

                List<ObjectiveAnalyzer.FlipLocationRuleState> front =
                        fixture.analyzer.assessFlipLocationRules(
                                fixture.game, PLAYER, "preFlip", "flip");
                List<ObjectiveAnalyzer.FlipLocationRuleState> back =
                        fixture.analyzer.assessFlipLocationRules(
                                fixture.game, PLAYER, "postFlip", "flipBack");
                assertEquals(label(bot, family), 1, front.size());
                assertEquals(label(bot, family), 1, back.size());
                assertFalse(label(bot, family), front.get(0).conditionSatisfied());
                assertTrue(label(bot, family), back.get(0).conditionSatisfied());

                fixture.place(fixture.opponentTarget, fixture.gate);
                front = fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER, "preFlip", "flip");
                back = fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER, "postFlip", "flipBack");
                assertTrue(label(bot, family), front.get(0).conditionSatisfied());
                assertFalse(label(bot, family), back.get(0).conditionSatisfied());

                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                        fixture.analyzer.classifyPreFlipProgressCandidate(
                                fixture.game, PLAYER, fixture.ownDuelist));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                        fixture.analyzer.classifyPreFlipProgressCandidate(
                                fixture.game, PLAYER,
                                fixture.ownCopyOfOpponentType));
            }
        }
    }

    @Test
    public void postFlipPayoffStagesOnlyTheRightDuelistAtTheExactOpponentSite() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, true);

                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.ownDuelist, fixture.gate));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.ownDuelist, fixture.otherGate));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.wrongOwnActor, fixture.gate));
            }
        }
    }

    @Test
    public void establishedDuelistHoldsThePairingInsteadOfWalkingAway() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, true);
                PhysicalCard deployed = character(
                        fixture, family.ownDuelist, PLAYER,
                        Zone.AT_LOCATION, 230);
                fixture.place(deployed, fixture.gate);

                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                        fixture.analyzer.classifyPostFlipPayoffRoleAt(
                                fixture.game, PLAYER, deployed, fixture.gate));
                assertTrue(label(bot, family),
                        fixture.analyzer.wouldDowngradePostFlipPayoffIfMoved(
                                fixture.game, PLAYER, deployed, fixture.otherGate));
            }
        }
    }

    @Test
    public void payoffRequiresAnOrdinarilyActiveTargetableOpponent() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, true);

                when(fixture.gameState.isCardInPlayActive(
                        fixture.opponentTarget,
                        false, false, false, false,
                        false, false, false, false)).thenReturn(false);
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.ownDuelist, fixture.gate));
                assertFalse(label(bot, family), fixture.analyzer
                        .assessFlipLocationRules(
                                fixture.game, PLAYER,
                                "postFlip", "flipBack")
                        .get(0).conditionSatisfied());

                when(fixture.gameState.isCardInPlayActive(
                        fixture.opponentTarget,
                        false, false, false, false,
                        false, false, false, false)).thenReturn(true);
                when(fixture.modifiers.canBeTargetedBy(
                        eq(fixture.gameState),
                        eq(fixture.opponentTarget),
                        eq(fixture.objective),
                        anySet())).thenReturn(false);
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.ownDuelist, fixture.gate));
            }
        }
    }

    @Test
    public void postFlipPayoffRequiresTheObjectiveToTargetItsOwnDuelist() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, true);
                PhysicalCard deployed = character(
                        fixture, family.ownDuelist, PLAYER,
                        Zone.AT_LOCATION, 240);
                fixture.place(deployed, fixture.gate);

                when(fixture.modifiers.canBeTargetedBy(
                        eq(fixture.gameState), eq(deployed),
                        eq(fixture.objective), anySet())).thenReturn(false);
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                        fixture.analyzer.classifyPostFlipPayoffRoleAt(
                                fixture.game, PLAYER,
                                deployed, fixture.gate));

                when(fixture.modifiers.canBeTargetedBy(
                        eq(fixture.gameState), eq(deployed),
                        eq(fixture.objective), anySet())).thenReturn(true);
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                        fixture.analyzer.classifyPostFlipPayoffRoleAt(
                                fixture.game, PLAYER,
                                deployed, fixture.gate));
            }
        }
    }

    @Test
    public void frontPayoffIsSatisfiedOnlyDuringOurControlPhase() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false, true);
                PhysicalCard deployed = character(
                        fixture, family.ownDuelist, PLAYER,
                        Zone.AT_LOCATION, 241);
                fixture.place(deployed, fixture.gate);

                assertTrue(label(bot, family) + " location precondition",
                        Filters.interior_Theed_Palace_site.accepts(
                                fixture.gameState, fixture.modifiers,
                                fixture.gate));
                assertTrue(label(bot, family) + " target precondition",
                        Filters.canBeTargetedBy(
                                fixture.objective,
                                TargetingReason.TO_BE_LOST).accepts(
                                    fixture.gameState,
                                    fixture.modifiers,
                                    fixture.opponentTarget));
                assertTrue(label(bot, family) + " route precondition",
                        fixture.analyzer.qualifiesNabooDuelFrontTargetRouteAt(
                                fixture.game, PLAYER,
                                deployed, fixture.gate));

                setPhase(fixture, Phase.CONTROL, PLAYER);
                assertTrue(label(bot, family),
                        payoffSatisfied(fixture, "preFlip"));

                setPhase(fixture, Phase.DEPLOY, PLAYER);
                assertFalse(label(bot, family),
                        payoffSatisfied(fixture, "preFlip"));

                setPhase(fixture, Phase.CONTROL, OPPONENT);
                assertFalse(label(bot, family),
                        payoffSatisfied(fixture, "preFlip"));
            }
        }
    }

    @Test
    public void backPayoffIsSatisfiedOnlyDuringOurMovePhase() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, true);
                PhysicalCard deployed = character(
                        fixture, family.ownDuelist, PLAYER,
                        Zone.AT_LOCATION, 242);
                fixture.place(deployed, fixture.gate);

                setPhase(fixture, Phase.MOVE, PLAYER);
                assertTrue(label(bot, family),
                        payoffSatisfied(fixture, "postFlip"));

                setPhase(fixture, Phase.CONTROL, PLAYER);
                assertFalse(label(bot, family),
                        payoffSatisfied(fixture, "postFlip"));

                setPhase(fixture, Phase.MOVE, OPPONENT);
                assertFalse(label(bot, family),
                        payoffSatisfied(fixture, "postFlip"));
            }
        }
    }

    @Test
    public void payoffMayUseAnotherLocationWhileTheGateKeepsTheBackActive() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true, true);
                when(fixture.gameState.isCardInPlayActive(
                        fixture.opponentTarget,
                        false, false, false, false,
                        false, false, false, false)).thenReturn(false);
                PhysicalCard remoteTarget = character(
                        fixture, family.opponentTarget, OPPONENT,
                        Zone.AT_LOCATION, 204);
                fixture.place(remoteTarget, fixture.remoteSite);
                when(fixture.modifiers.canBeTargetedBy(
                        eq(fixture.gameState), eq(remoteTarget),
                        eq(fixture.objective), anySet())).thenReturn(true);

                assertFalse(label(bot, family), fixture.analyzer
                        .assessFlipLocationRules(
                                fixture.game, PLAYER,
                                "postFlip", "flipBack")
                        .get(0).conditionSatisfied());
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.ownDuelist, fixture.gate));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                        fixture.analyzer.classifyPostFlipPayoffAt(
                                fixture.game, PLAYER,
                                fixture.ownDuelist,
                                fixture.remoteSite));
            }
        }
    }

    private static Fixture fixture(
            Bot bot, Family family, boolean flipped,
            boolean includeOpponentTarget) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = objective(family, flipped);
        PhysicalCard gate = location(family.generator, 100);
        PhysicalCard otherGate = location(family.core, 101);
        PhysicalCard remoteSite = location("12_80", 102);
        when(remoteSite.getPartOfSystem()).thenReturn("Tatooine");

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getSide(PLAYER)).thenReturn(family.side);
        when(gameState.getSide(OPPONENT)).thenReturn(family.side == Side.LIGHT
                ? Side.DARK : Side.LIGHT);
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(gate, otherGate, remoteSite));
        when(gameState.getTopLocations()).thenReturn(
                List.of(gate, otherGate, remoteSite));
        when(modifiers.isBattleground(gameState, gate, null)).thenReturn(true);
        when(modifiers.isBattleground(gameState, otherGate, null)).thenReturn(true);
        when(modifiers.hasIcon(
                gameState, gate, Icon.INTERIOR_SITE)).thenReturn(true);
        when(modifiers.hasIcon(
                gameState, otherGate, Icon.INTERIOR_SITE)).thenReturn(true);
        when(modifiers.hasKeyword(
                gameState, gate,
                Keyword.THEED_PALACE_SITE)).thenReturn(true);
        when(modifiers.hasKeyword(
                gameState, otherGate,
                Keyword.THEED_PALACE_SITE)).thenReturn(true);

        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        permanents.add(gate);
        permanents.add(otherGate);
        permanents.add(remoteSite);
        when(gameState.getAllPermanentCards()).thenAnswer(
                ignored -> new ArrayList<>(permanents));

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        Fixture fixture = new Fixture(
                family, analyzer, game, gameState, modifiers,
                objective, gate, otherGate, remoteSite,
                permanents);
        fixture.ownDuelist = character(
                fixture, family.ownDuelist, PLAYER, Zone.HAND, 200);
        fixture.opponentTarget = character(
                fixture, family.opponentTarget, OPPONENT,
                Zone.AT_LOCATION, 201);
        fixture.ownCopyOfOpponentType = character(
                fixture, family.opponentTarget, PLAYER,
                Zone.HAND, 203);
        fixture.wrongOwnActor = character(
                fixture, family.wrongOwnActor, PLAYER, Zone.HAND, 202);
        when(gameState.findCardByPermanentId(1)).thenReturn(objective);
        when(modifiers.canBeTargetedBy(
                eq(gameState), any(PhysicalCard.class),
                eq(objective), anySet())).thenReturn(true);
        when(modifiers.canBeTargetedBy(
                eq(gameState), eq(fixture.opponentTarget),
                eq(objective), anySet())).thenReturn(true);
        if (includeOpponentTarget) {
            fixture.place(fixture.opponentTarget, gate);
        }
        analyzer.analyze(game, PLAYER, family.side);
        return fixture;
    }

    private static PhysicalCard objective(Family family, boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(family.front);
        SwccgCardBlueprint back = blueprint(family.back);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(flipped ? back : front);
        when(card.getOtherSideBlueprint()).thenReturn(flipped ? front : back);
        when(card.getBlueprintId(true)).thenReturn(family.front);
        when(card.getBlueprintId(false)).thenReturn(
                flipped ? family.back : family.front);
        when(card.getTitle()).thenReturn(
                (flipped ? back : front).getTitle());
        when(card.getTitles()).thenReturn(List.of(front.getTitle(), back.getTitle()));
        when(card.getCardId()).thenReturn(1);
        when(card.getPermanentCardId()).thenReturn(1);
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard location(
            String blueprintId, int id) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.LOCATIONS);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getPartOfSystem()).thenReturn("Naboo");
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static PhysicalCard character(
            Fixture fixture, String blueprintId, String owner,
            Zone zone, int id) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        when(fixture.modifiers.getAbility(
                fixture.gameState, card)).thenReturn(blueprint.getAbility());
        when(fixture.gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(fixture.gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(true);
        return card;
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint = CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
    }

    private static void setPhase(
            Fixture fixture, Phase phase, String currentPlayer) {
        when(fixture.gameState.getCurrentPhase()).thenReturn(phase);
        when(fixture.gameState.getCurrentPlayerId()).thenReturn(currentPlayer);
    }

    private static boolean payoffSatisfied(
            Fixture fixture, String objectivePhase) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER,
                        objectivePhase, "payoff");
        assertEquals(1, states.size());
        return states.get(0).conditionSatisfied();
    }

    private static String label(Bot bot, Family family) {
        return bot + " " + family.frontTitle;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Family(
            String frontTitle,
            String front,
            String back,
            Side side,
            String core,
            String generator,
            String alternateGenerator,
            String duelEvent,
            String ownDuelist,
            String opponentTarget,
            String wrongOwnActor) {
    }

    private static final class Fixture {
        private final Family family;
        private final ObjectiveAnalyzer analyzer;
        private final SwccgGame game;
        private final GameState gameState;
        private final ModifiersQuerying modifiers;
        private final PhysicalCard objective;
        private final PhysicalCard gate;
        private final PhysicalCard otherGate;
        private final PhysicalCard remoteSite;
        private final List<PhysicalCard> permanents;
        private PhysicalCard ownDuelist;
        private PhysicalCard opponentTarget;
        private PhysicalCard ownCopyOfOpponentType;
        private PhysicalCard wrongOwnActor;

        private Fixture(
                Family family,
                ObjectiveAnalyzer analyzer,
                SwccgGame game,
                GameState gameState,
                ModifiersQuerying modifiers,
                PhysicalCard objective,
                PhysicalCard gate,
                PhysicalCard otherGate,
                PhysicalCard remoteSite,
                List<PhysicalCard> permanents) {
            this.family = family;
            this.analyzer = analyzer;
            this.game = game;
            this.gameState = gameState;
            this.modifiers = modifiers;
            this.objective = objective;
            this.gate = gate;
            this.otherGate = otherGate;
            this.remoteSite = remoteSite;
            this.permanents = permanents;
        }

        private void place(PhysicalCard card, PhysicalCard location) {
            if (!permanents.contains(card)) {
                permanents.add(card);
            }
            when(card.getAtLocation()).thenReturn(location);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, card)).thenReturn(location);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, card)).thenReturn(location);
        }
    }
}
