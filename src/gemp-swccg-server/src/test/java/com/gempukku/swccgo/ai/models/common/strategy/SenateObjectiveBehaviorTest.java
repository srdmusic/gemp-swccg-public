package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.common.Agenda;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared behavioral contract for the Coruscant Senate objective pair. The
 * filters use real card blueprints; only the physical table is mocked.
 */
public class SenateObjectiveBehaviorTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    private static final List<Family> FAMILIES = List.of(
            new Family(
                    "My Lord, Is That Legal?", "12_179", "12_179_BACK",
                    "12_167", "12_166", Side.DARK, Agenda.BLOCKADE,
                    List.of("12_113", "12_122", "12_126", "12_105"),
                    "12_97"),
            new Family(
                    "Plead My Case To The Senate", "12_88", "12_88_BACK",
                    "12_75", "12_74", Side.LIGHT, Agenda.PEACE,
                    List.of("12_34", "202_1", "12_28", "12_29"),
                    "12_11"));

    @Test
    public void bothSenateProfilesHydrateTheExactThreeOrTwoWithAgendaLaw() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture empty = fixture(bot, family, false, List.of());
                assertTrue(label(bot, family), empty.analyzer.isAnalyzed());
                assertTrue(label(bot, family), empty.analyzer.isMyLord());
                assertNotNull(label(bot, family),
                        empty.analyzer.getActivePlaybook());
                assertEquals(label(bot, family), family.frontTitle,
                        empty.analyzer.getActivePlaybook().label);

                Fixture twoPlain = fixture(
                        bot, family, false,
                        family.plainSenators.subList(0, 2));
                assertSatisfiedRules(twoPlain, 1);

                Fixture threePlain = fixture(
                        bot, family, false,
                        family.plainSenators.subList(0, 3));
                assertSatisfiedRules(threePlain, 2);

                Fixture agendaShortcut = fixture(
                        bot, family, false,
                        List.of(family.plainSenators.get(0),
                                family.agendaSenator));
                assertSatisfiedRules(agendaShortcut, 2);
            }
        }
    }

    @Test
    public void oppositeSideAgendaDoesNotCompleteTheTwoSenatorShortcut() {
        for (int index = 0; index < FAMILIES.size(); index++) {
            Family family = FAMILIES.get(index);
            Family opposite = FAMILIES.get(1 - index);
            for (Bot bot : Bot.values()) {
                Fixture wrongAgenda = fixture(
                        bot, family, false,
                        List.of(family.plainSenators.get(0),
                                opposite.agendaSenator));
                assertSatisfiedRules(wrongAgenda, 1);
            }
        }
    }

    @Test
    public void preFlipFormationKeepsEveryCountedSenatorUntilTheGateSurvives() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture two = fixture(
                        bot, family, false,
                        family.plainSenators.subList(0, 2));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        two.role(0));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        two.role(1));

                Fixture three = fixture(
                        bot, family, false,
                        family.plainSenators.subList(0, 3));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        three.role(0));

                Fixture threeWithAgenda = fixture(
                        bot, family, false,
                        List.of(family.plainSenators.get(0),
                                family.plainSenators.get(1),
                                family.agendaSenator));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        threeWithAgenda.role(0));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        threeWithAgenda.role(2));

                Fixture four = fixture(
                        bot, family, false,
                        family.plainSenators);
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        four.role(0));
            }
        }
    }

    @Test
    public void postFlipLastTwoSenatorsAreProtectedButAThirdIsRedundant() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture two = fixture(
                        bot, family, true,
                        family.plainSenators.subList(0, 2));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ON_TABLE_ACTOR,
                        two.role(0));
                assertTrue(label(bot, family),
                        two.analyzer.wouldDepartureTriggerFlipBack(
                                two.game, PLAYER, two.senators.get(0)));

                Fixture three = fixture(
                        bot, family, true,
                        family.plainSenators.subList(0, 3));
                assertEquals(label(bot, family),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        three.role(0));
                assertFalse(label(bot, family),
                        three.analyzer.wouldDepartureTriggerFlipBack(
                                three.game, PLAYER,
                                three.senators.get(0)));
            }
        }
    }

    @Test
    public void battlePolicyConsumesTheRealSenateFormationRole() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(
                        bot, family, true,
                        family.plainSenators.subList(0, 2));
                ObjectiveAnalyzer.FlipGateFormationRole role =
                        fixture.role(0);

                String protectedId = String.valueOf(
                        fixture.senators.get(0).getCardId());
                String safeId = String.valueOf(fixture.safeCard.getCardId());
                BattleForfeitFacts.FlipGateFormationSelectionFacts facts =
                        BattleForfeitFacts.readFlipGateFormationSelection(
                                List.of(protectedId, safeId),
                                fixture.gameState, fixture.game, PLAYER,
                                fixture.analyzer, false, 0);
                assertEquals(label(bot, family), role,
                        facts.roleFor(protectedId));
                assertTrue(label(bot, family),
                        facts.hasUnprotectedLegalAlternative());

                var battle = BattleForfeitPolicy
                        .scoreFlipGateFormationProtection(
                                protectedId, facts.roleFor(protectedId),
                                facts.hasUnprotectedLegalAlternative());
                assertEquals(label(bot, family), 1,
                        battle.operations().size());
                assertEquals(label(bot, family), -9999.0f,
                        battle.operations().get(0).delta(), 0.0f);
            }
        }
    }

    private static void assertSatisfiedRules(
            Fixture fixture, int expected) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER, "preFlip", "flip");
        assertEquals(label(fixture.bot, fixture.family), 2,
                states.size());
        assertEquals(label(fixture.bot, fixture.family), expected,
                states.stream().filter(
                        ObjectiveAnalyzer.FlipLocationRuleState
                                ::conditionSatisfied).count());
    }

    private static Fixture fixture(
            Bot bot, Family family, boolean flipped,
            List<String> senatorBlueprintIds) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = objective(family, flipped);
        PhysicalCard senate = location(family.senateBlueprint, 100);
        PhysicalCard otherSite = location(family.otherSiteBlueprint, 101);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(senate, otherSite));
        when(gameState.getTopLocations()).thenReturn(
                List.of(senate, otherSite));

        List<PhysicalCard> senators = new ArrayList<>();
        Map<Integer, PhysicalCard> byId = new LinkedHashMap<>();
        byId.put(100, senate);
        byId.put(101, otherSite);
        int cardId = 200;
        for (String blueprintId : senatorBlueprintIds) {
            PhysicalCard senator = card(blueprintId, cardId++);
            senators.add(senator);
            byId.put(senator.getCardId(), senator);
            setActive(gameState, senator, true);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, senator)).thenReturn(senate);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, senator)).thenReturn(senate);
            when(modifiers.hasKeyword(
                    gameState, senator, Keyword.SENATOR))
                    .thenReturn(true);
            when(modifiers.hasAgenda(
                    gameState, senator, family.agenda))
                    .thenReturn(blueprintId.equals(
                            family.agendaSenator));
        }

        PhysicalCard safeCard = mock(PhysicalCard.class);
        SwccgCardBlueprint safeBlueprint = mock(SwccgCardBlueprint.class);
        when(safeCard.getOwner()).thenReturn(PLAYER);
        when(safeCard.getZone()).thenReturn(Zone.AT_LOCATION);
        when(safeCard.getBlueprint()).thenReturn(safeBlueprint);
        when(safeCard.getCardId()).thenReturn(900);
        when(safeCard.getPermanentCardId()).thenReturn(900);
        when(safeCard.isUndercover()).thenReturn(false);
        when(safeBlueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        byId.put(900, safeCard);
        setActive(gameState, safeCard, true);
        when(modifiers.getLocationThatCardIsAt(
                gameState, safeCard)).thenReturn(otherSite);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, safeCard)).thenReturn(otherSite);

        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        permanents.add(senate);
        permanents.add(otherSite);
        permanents.addAll(senators);
        permanents.add(safeCard);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getCardsAtLocation(senate))
                .thenReturn(new ArrayList<>(senators));
        when(gameState.getCardsAtLocation(otherSite))
                .thenReturn(List.of(safeCard));
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> byId.get(
                        invocation.getArgument(0, Integer.class)));
        when(gameState.findCardByPermanentId(anyInt())).thenAnswer(
                invocation -> byId.get(
                        invocation.getArgument(0, Integer.class)));

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, family.side);
        return new Fixture(
                bot, family, analyzer, game, gameState,
                senators, safeCard);
    }

    private static PhysicalCard objective(
            Family family, boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(family.frontBlueprint);
        SwccgCardBlueprint back = blueprint(family.backBlueprint);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(front);
        when(card.getOtherSideBlueprint()).thenReturn(back);
        when(card.getBlueprintId(true))
                .thenReturn(family.frontBlueprint);
        when(card.getBlueprintId(false))
                .thenReturn(family.frontBlueprint);
        when(card.getTitle()).thenReturn(front.getTitle());
        when(card.getTitles()).thenReturn(List.of(front.getTitle()));
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard location(
            String blueprintId, int cardId) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.LOCATIONS);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static PhysicalCard card(
            String blueprintId, int cardId) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.isUndercover()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
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

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
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
            String frontBlueprint,
            String backBlueprint,
            String senateBlueprint,
            String otherSiteBlueprint,
            Side side,
            Agenda agenda,
            List<String> plainSenators,
            String agendaSenator) {
    }

    private record Fixture(
            Bot bot,
            Family family,
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            List<PhysicalCard> senators,
            PhysicalCard safeCard) {

        private ObjectiveAnalyzer.FlipGateFormationRole role(int index) {
            return analyzer.classifyGateFormationPieceIfRemoved(
                    game, PLAYER, senators.get(index));
        }
    }
}
