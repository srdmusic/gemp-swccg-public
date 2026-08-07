package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentsOfBlackSunObjectiveBehaviorTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void currentActorSwitchesExactlyFromXizorToLegacyShada() {
        for (Bot bot : Bot.values()) {
            Fixture classic = fixture(bot, false, false);
            place(classic, classic.xizor, classic.battleground);
            assertTrue(label(bot), frontState(classic).conditionSatisfied());

            Fixture classicShada = fixture(bot, false, false);
            place(classicShada, classicShada.shada,
                    classicShada.battleground);
            assertFalse("Classic Shada is not Xizor: " + label(bot),
                    frontState(classicShada).conditionSatisfied());

            Fixture legacy = fixture(bot, false, true);
            place(legacy, legacy.shada, legacy.battleground);
            assertTrue("Legacy Shada is the current actor: " + label(bot),
                    frontState(legacy).conditionSatisfied());

            Fixture legacyXizor = fixture(bot, false, true);
            place(legacyXizor, legacyXizor.xizor,
                    legacyXizor.battleground);
            assertFalse("Legacy replaces Xizor with Shada: " + label(bot),
                    frontState(legacyXizor).conditionSatisfied());
        }
    }

    @Test
    public void actorAndTargetUseAtAndAreOwnershipFree() {
        for (Bot bot : Bot.values()) {
            Fixture aboardActor = fixture(bot, false, false);
            addAboard(aboardActor, aboardActor.opponentXizor,
                    aboardActor.battleground);
            assertTrue("An aboard opponent-owned source actor counts: "
                            + label(bot),
                    frontState(aboardActor).conditionSatisfied());

            Fixture aboardTarget = fixture(bot, false, false);
            place(aboardTarget, aboardTarget.xizor,
                    aboardTarget.battleground);
            addAboard(aboardTarget, aboardTarget.ownLuke,
                    aboardTarget.otherBattleground);
            assertFalse("An aboard owner-matching target still blocks: "
                            + label(bot),
                    frontState(aboardTarget).conditionSatisfied());
        }
    }

    @Test
    public void targetPrecedenceIsReyThenAnakinThenLuke() {
        for (Bot bot : Bot.values()) {
            Fixture classic = fixture(bot, false, false);
            place(classic, classic.xizor, classic.battleground);
            place(classic, classic.anakin, classic.otherBattleground);
            place(classic, classic.rey, classic.otherBattleground);
            assertTrue("Classic ignores Anakin and Rey: " + label(bot),
                    frontState(classic).conditionSatisfied());
            place(classic, classic.luke, classic.otherBattleground);
            assertFalse("Classic targets Luke: " + label(bot),
                    frontState(classic).conditionSatisfied());

            Fixture anakin = fixture(bot, false, false);
            setModification(anakin,
                    ModifyGameTextType
                        .REFLECTIONS_II_OBJECTIVE__TARGETS_ANAKIN_INSTEAD_OF_LUKE,
                    true);
            place(anakin, anakin.xizor, anakin.battleground);
            place(anakin, anakin.luke, anakin.otherBattleground);
            assertTrue("Anakin retarget ignores Luke: " + label(bot),
                    frontState(anakin).conditionSatisfied());
            place(anakin, anakin.anakin,
                    anakin.otherBattleground);
            assertFalse("Anakin retarget blocks on Anakin: " + label(bot),
                    frontState(anakin).conditionSatisfied());

            Fixture rey = fixture(bot, false, false);
            setModification(rey,
                    ModifyGameTextType
                        .REFLECTIONS_II_OBJECTIVE__TARGETS_ANAKIN_INSTEAD_OF_LUKE,
                    true);
            setModification(rey,
                    ModifyGameTextType
                        .REFLECTIONS_II_OBJECTIVE__TARGETS_REY_INSTEAD_OF_LUKE,
                    true);
            place(rey, rey.xizor, rey.battleground);
            place(rey, rey.luke, rey.otherBattleground);
            place(rey, rey.anakin, rey.otherBattleground);
            assertTrue("Rey retarget takes precedence: " + label(bot),
                    frontState(rey).conditionSatisfied());
            place(rey, rey.rey, rey.otherBattleground);
            assertFalse("Rey retarget blocks on Rey: " + label(bot),
                    frontState(rey).conditionSatisfied());
        }
    }

    @Test
    public void backRequiresActorOnlyOnTableAndTracksTheCurrentTarget() {
        for (Bot bot : Bot.values()) {
            Fixture stable = fixture(bot, true, false);
            place(stable, stable.xizor, stable.nonBattleground);
            assertFalse("Xizor may leave the battleground after flipping: "
                            + label(bot),
                    backState(stable).conditionSatisfied());
            assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ON_TABLE_ACTOR,
                    stable.analyzer.classifyGateFormationPieceIfRemoved(
                            stable.game, PLAYER, stable.xizor));

            stable.permanents.remove(stable.xizor);
            assertTrue("Losing the current actor flips back: " + label(bot),
                    backState(stable).conditionSatisfied());

            Fixture target = fixture(bot, true, false);
            place(target, target.xizor, target.nonBattleground);
            place(target, target.luke, target.battleground);
            assertTrue("Current target at a battleground flips back: "
                            + label(bot),
                    backState(target).conditionSatisfied());
        }
    }

    @Test
    public void payoffTargetsTwoClassicLocationsButOneLegacyLocation() {
        for (Bot bot : Bot.values()) {
            Fixture classic = fixture(bot, true, false);
            place(classic, classic.xizor, classic.battleground);
            occupy(classic, classic.battleground);
            assertFalse("Classic route still wants Emperor elsewhere: "
                            + label(bot),
                    payoffState(classic).conditionSatisfied());
            place(classic, classic.emperor,
                    classic.otherBattleground);
            occupy(classic, classic.otherBattleground);
            assertTrue("Two distinct classic payoff locations complete target: "
                            + label(bot),
                    payoffState(classic).conditionSatisfied());

            Fixture stacked = fixture(bot, true, false);
            place(stacked, stacked.xizor, stacked.battleground);
            place(stacked, stacked.emperor, stacked.battleground);
            occupy(stacked, stacked.battleground);
            assertFalse("Two actors at one battleground count once: "
                            + label(bot),
                    payoffState(stacked).conditionSatisfied());

            Fixture legacy = fixture(bot, true, true);
            place(legacy, legacy.shada, legacy.battleground);
            occupy(legacy, legacy.battleground);
            assertTrue("No Bargain makes one Shada location the reachable target: "
                            + label(bot),
                    payoffState(legacy).conditionSatisfied());
        }
    }

    @Test
    public void payoffProjectionUsesDistinctLocationDelta() {
        for (Bot bot : Bot.values()) {
            Fixture split = fixture(bot, true, false);
            place(split, split.xizor, split.battleground);
            place(split, split.emperor, split.battleground);
            occupy(split, split.battleground);
            occupy(split, split.otherBattleground);
            assertEquals(
                    "Splitting Xizor and Emperor adds a distinct location: "
                            + label(bot),
                    ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                    split.analyzer.classifyPostFlipPayoffAt(
                            split.game, PLAYER, split.emperor,
                            split.otherBattleground));

            Fixture churn = fixture(bot, true, false);
            place(churn, churn.xizor, churn.battleground);
            occupy(churn, churn.battleground);
            occupy(churn, churn.otherBattleground);
            assertEquals(
                    "Moving the sole actor only trades locations: "
                            + label(bot),
                    ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                    churn.analyzer.classifyPostFlipPayoffAt(
                            churn.game, PLAYER, churn.xizor,
                            churn.otherBattleground));
            assertEquals(
                    "A neutral relocation remains a payoff-preserving role: "
                            + label(bot),
                    ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                    churn.analyzer.classifyPostFlipPayoffRoleAt(
                            churn.game, PLAYER, churn.xizor,
                            churn.otherBattleground));

            Fixture consolidate = fixture(bot, true, false);
            place(consolidate, consolidate.xizor,
                    consolidate.battleground);
            place(consolidate, consolidate.emperor,
                    consolidate.otherBattleground);
            occupy(consolidate, consolidate.battleground);
            occupy(consolidate, consolidate.otherBattleground);
            assertTrue(
                    "Consolidating two payoff locations loses one: "
                            + label(bot),
                    consolidate.analyzer
                        .wouldDowngradePostFlipPayoffIfMoved(
                            consolidate.game, PLAYER,
                            consolidate.emperor,
                            consolidate.battleground));

            Fixture aboard = fixture(bot, true, false);
            place(aboard, aboard.xizor, aboard.battleground);
            place(aboard, aboard.emperor, aboard.battleground);
            occupy(aboard, aboard.battleground);
            occupy(aboard, aboard.otherBattleground);
            PhysicalCard carrier = carrier("Stinger", PLAYER);
            when(aboard.gameState.getAboardCards(
                    carrier, true)).thenReturn(List.of(aboard.xizor));
            when(aboard.modifiers.getLocationThatCardIsAt(
                    aboard.gameState, carrier))
                    .thenReturn(aboard.battleground);
            assertEquals(
                    "A carrier moves its aboard actor contribution: "
                            + label(bot),
                    ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                    aboard.analyzer.classifyPostFlipPayoffAt(
                            aboard.game, PLAYER, carrier,
                            aboard.otherBattleground));

            Fixture deployAboard = fixture(bot, true, false);
            place(deployAboard, deployAboard.xizor,
                    deployAboard.battleground);
            occupy(deployAboard, deployAboard.battleground);
            occupy(deployAboard, deployAboard.otherBattleground);
            PhysicalCard systemCarrier = carrier("Stinger", PLAYER);
            when(deployAboard.modifiers.getLocationThatCardIsAt(
                    deployAboard.gameState, systemCarrier))
                    .thenReturn(deployAboard.otherBattleground);
            when(deployAboard.emperor.getZone()).thenReturn(Zone.HAND);
            assertEquals(
                    "Deploying Emperor aboard at a second battleground counts: "
                            + label(bot),
                    ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                    deployAboard.analyzer.classifyPostFlipPayoffAt(
                            deployAboard.game, PLAYER,
                            deployAboard.emperor, systemCarrier));
        }
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState frontState(
            Fixture fixture) {
        return onlyState(fixture, "preFlip", "flip");
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState backState(
            Fixture fixture) {
        return onlyState(fixture, "postFlip", "flipBack");
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState payoffState(
            Fixture fixture) {
        return onlyState(fixture, "postFlip", "payoff");
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState onlyState(
            Fixture fixture, String phase, String purpose) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER, phase, purpose);
        assertEquals(1, states.size());
        return states.getFirst();
    }

    private static Fixture fixture(
            Bot bot, boolean flipped, boolean legacy) {
        ObjectiveAnalyzer analyzer = bot.analyzer();
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getSide(PLAYER)).thenReturn(Side.DARK);
        when(gameState.getSide(OPPONENT)).thenReturn(Side.LIGHT);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(flipped ? back : front);
        when(objective.getOtherSideBlueprint()).thenReturn(
                flipped ? front : back);
        when(objective.getBlueprintId(true)).thenReturn("10_29");
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn("Agents Of Black Sun");
        when(front.getGameText()).thenReturn(
                "Flip this card if Xizor is at a battleground site and Luke is not at a battleground site.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn(
                "Vengeance Of The Dark Prince");
        when(back.getGameText()).thenReturn(
                "Flip this card if Luke is at a battleground site or Xizor is not on table.");
        when(back.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);

        PhysicalCard battleground = site("Tatooine: Cantina", 101);
        PhysicalCard otherBattleground = site(
                "Cloud City: Carbonite Chamber", 102);
        PhysicalCard nonBattleground = site(
                "Coruscant: Imperial City", 103);
        when(modifiers.isBattleground(
                gameState, battleground, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, otherBattleground, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, nonBattleground, null)).thenReturn(false);

        PhysicalCard xizor = character(
                gameState, modifiers, "Prince Xizor", PLAYER);
        PhysicalCard opponentXizor = character(
                gameState, modifiers, "Prince Xizor", OPPONENT);
        PhysicalCard shada = character(
                gameState, modifiers, "Shada", PLAYER);
        PhysicalCard emperor = character(
                gameState, modifiers, "Emperor Palpatine", PLAYER);
        when(modifiers.hasPersona(
                gameState, emperor, Persona.SIDIOUS)).thenReturn(true);
        PhysicalCard luke = character(
                gameState, modifiers, "Luke Skywalker", OPPONENT);
        when(modifiers.hasPersona(
                gameState, luke, Persona.LUKE)).thenReturn(true);
        PhysicalCard ownLuke = character(
                gameState, modifiers, "Luke Skywalker", PLAYER);
        when(modifiers.hasPersona(
                gameState, ownLuke, Persona.LUKE)).thenReturn(true);
        PhysicalCard anakin = character(
                gameState, modifiers, "Anakin Skywalker", OPPONENT);
        when(modifiers.hasPersona(
                gameState, anakin, Persona.ANAKIN)).thenReturn(true);
        PhysicalCard rey = character(
                gameState, modifiers, "Rey", OPPONENT);
        when(modifiers.hasPersona(
                gameState, rey, Persona.REY)).thenReturn(true);

        List<PhysicalCard> locations = List.of(
                battleground, otherBattleground, nonBattleground);
        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        if (legacy) {
            when(modifiers.hasGameTextModification(
                    gameState, objective,
                    ModifyGameTextType
                        .LEGACY__TREAT_XIZOR_AS_SHADA))
                    .thenReturn(true);
        }

        analyzer.analyze(game, PLAYER, Side.DARK);
        return new Fixture(
                analyzer, game, gameState, modifiers,
                objective, permanents,
                battleground, otherBattleground, nonBattleground,
                xizor, opponentXizor, shada, emperor,
                luke, ownLuke, anakin, rey);
    }

    private static void setModification(
            Fixture fixture, ModifyGameTextType type,
            boolean active) {
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective, type))
                .thenReturn(active);
    }

    private static void place(
            Fixture fixture, PhysicalCard card,
            PhysicalCard location) {
        if (!fixture.permanents.contains(card)) {
            fixture.permanents.add(card);
        }
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, card)).thenReturn(location);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, card)).thenReturn(location);
    }

    private static void addAboard(
            Fixture fixture, PhysicalCard card,
            PhysicalCard location) {
        if (!fixture.permanents.contains(card)) {
            fixture.permanents.add(card);
        }
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, card)).thenReturn(null);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, card)).thenReturn(location);
    }

    private static void occupy(
            Fixture fixture, PhysicalCard location) {
        when(fixture.modifiers.occupiesLocation(
                eq(fixture.gameState), eq(location),
                eq(PLAYER), any())).thenReturn(true);
    }

    private static PhysicalCard site(
            String title, int permanentId) {
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

    private static PhysicalCard character(
            GameState gameState, ModifiersQuerying modifiers,
            String title, String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(true);
        return card;
    }

    private static PhysicalCard carrier(
            String title, String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.STARSHIP);
        return card;
    }

    private static String label(Bot bot) {
        return "10_29 " + bot;
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
            ModifiersQuerying modifiers,
            PhysicalCard objective,
            List<PhysicalCard> permanents,
            PhysicalCard battleground,
            PhysicalCard otherBattleground,
            PhysicalCard nonBattleground,
            PhysicalCard xizor,
            PhysicalCard opponentXizor,
            PhysicalCard shada,
            PhysicalCard emperor,
            PhysicalCard luke,
            PhysicalCard ownLuke,
            PhysicalCard anakin,
            PhysicalCard rey) { }
}
