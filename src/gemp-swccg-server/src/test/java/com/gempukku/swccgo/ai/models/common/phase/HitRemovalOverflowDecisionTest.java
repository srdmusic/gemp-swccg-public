package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Both public evaluator chains preserve the last hit defender until battle damage. */
public class HitRemovalOverflowDecisionTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    @Test
    public void eachHitRemovalInterruptDefersAndPassWinsForSoloDefender() {
        for (String source : List.of("10_47", "5_135", "10_23", "5_35")) {
            Fixture fixture = new Fixture(source);
            assertDecision(fixture, "Make Kanan lost", true, true);
        }
    }

    @Test
    public void twoRemainingEnemyCharactersKeepTheKillShot() {
        Fixture fixture = new Fixture("10_47");
        fixture.battle.addLightCardParticipant(card(3, "Rey", OPPONENT, CardCategory.CHARACTER));
        assertDecision(fixture, "Make Kanan lost", false, false);
    }

    @Test
    public void attachedWeaponAndNonparticipantDoNotHideTheSoloDefender() {
        Fixture fixture = new Fixture("10_47");
        PhysicalCard weapon = card(3, "Kanan's Lightsaber", OPPONENT, CardCategory.WEAPON);
        when(weapon.getAttachedTo()).thenReturn(fixture.target);
        fixture.battle.addLightCardParticipant(weapon);
        PhysicalCard excluded = card(4, "Excluded Rey", OPPONENT, CardCategory.CHARACTER);
        when(fixture.state.getAllPermanentCards()).thenReturn(List.of(fixture.target, weapon, excluded));
        assertDecision(fixture, "Make Kanan lost", true, true);
    }

    @Test
    public void participatingShipOrVehicleMeansTheCharacterIsNotTheOnlyBattleUnit() {
        for (CardCategory category : List.of(CardCategory.STARSHIP, CardCategory.VEHICLE)) {
            Fixture fixture = new Fixture("10_47");
            fixture.battle.addLightCardParticipant(card(3, "Transport", OPPONENT, category));
            assertDecision(fixture, "Make Kanan lost", false, false);
        }
    }

    @Test
    public void outsideBattleHitRemovalStillWorks() {
        Fixture fixture = new Fixture("10_47");
        when(fixture.state.getBattleState()).thenReturn(null);
        when(fixture.state.getBattleLocation()).thenReturn(null);
        fixture.phase = Phase.CONTROL;
        assertDecision(fixture, "Make Kanan lost", false, false);
    }

    @Test
    public void virtualTargetUsesTheSameFullNameAsTheCardAction() {
        Fixture fixture = new Fixture("10_47");
        when(fixture.target.getBlueprint().hasVirtualSuffix()).thenReturn(true);
        assertDecision(fixture, "Make Kanan (V) lost", true, true);
    }

    @Test
    public void damageSegmentAndNonHitTargetsDoNotTriggerTheOverflowGuard() {
        Fixture damage = new Fixture("10_47");
        damage.battle.reachedDamageSegment();
        assertDecision(damage, "Make Kanan lost", false, false);

        Fixture notHit = new Fixture("10_47");
        when(notHit.target.isHit()).thenReturn(false);
        assertDecision(notHit, "Make Kanan lost", false, false);
    }

    @Test
    public void unrelatedMakeLostSourceDoesNotInheritTheSniperGuard() {
        Fixture fixture = new Fixture("unknown");
        assertDecision(fixture, "Make Kanan lost", false, false);
    }

    @Test
    public void weaponDestinyBoostRetainsItsSeparatePreservationRule() {
        Fixture fixture = new Fixture("10_47");
        assertDecision(fixture, "Add 3 to total weapon destiny", true, false);
    }

    private static void assertDecision(Fixture fixture, String text,
                                       boolean deferred, boolean overflowGuard) {
        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                fixture.state, PLAYER, "CARD_ACTION_CHOICE", "Choose an optional response",
                "solo-hit-removal", fixture.phase);
        randoContext.setGame(fixture.game);
        randoContext.setActionIds(List.of("kill"));
        randoContext.setActionTexts(List.of(text));
        randoContext.setCardIds(List.of("10"));

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                fixture.state, PLAYER, "CARD_ACTION_CHOICE", "Choose an optional response",
                "solo-hit-removal", fixture.phase);
        chosenContext.setGame(fixture.game);
        chosenContext.setActionIds(List.of("kill"));
        chosenContext.setActionTexts(List.of(text));
        chosenContext.setCardIds(List.of("10"));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext).get(0);
        assertEquals(deferred, rando.isDeferred());
        assertEquals(deferred, chosen.isDeferred());
        assertEquals(overflowGuard, rando.getReasoningString().contains("V313 PRESERVE OVERFLOW"));
        assertEquals(rando.getReasoning(), chosen.getReasoning());
        assertFalse(rando.isHardVetoed());
        assertFalse(chosen.isHardVetoed());

        var selectedRando = new com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator()
                .evaluateDecision(randoContext);
        var selectedChosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CombinedEvaluator()
                .evaluateDecision(chosenContext);
        if (deferred) {
            assertEquals(com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.PASS,
                    selectedRando.getActionType());
            assertEquals(com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.PASS,
                    selectedChosen.getActionType());
        } else {
            assertTrue(rando.getScore() > 400f);
            assertEquals("kill", selectedRando.getActionId());
            assertEquals("kill", selectedChosen.getActionId());
        }
    }

    private static PhysicalCard card(int id, String title, String owner, CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getCardId()).thenReturn(id);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(5f);
        when(blueprint.hasForfeitAttribute()).thenReturn(true);
        when(blueprint.getForfeit()).thenReturn(7f);
        return card;
    }

    private static class Fixture {
        final GameState state = mock(GameState.class);
        final SwccgGame game = mock(SwccgGame.class);
        final PhysicalCard target = card(2, "Kanan", OPPONENT, CardCategory.CHARACTER);
        final PhysicalCard location = card(1, "Site", PLAYER, CardCategory.LOCATION);
        final BattleState battle = new BattleState(game, PLAYER, location, false);
        Phase phase = Phase.BATTLE;

        Fixture(String sourceBlueprint) {
            when(game.getGameState()).thenReturn(state);
            when(game.getModifiersQuerying()).thenReturn(mock(ModifiersQuerying.class));
            when(game.getDarkPlayer()).thenReturn(PLAYER);
            when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
            when(state.getOpponent(PLAYER)).thenReturn(OPPONENT);
            when(state.getCurrentPlayerId()).thenReturn(PLAYER);
            when(state.getPlayersLatestTurnNumber(PLAYER)).thenReturn(4);
            when(state.getForcePileSize(PLAYER)).thenReturn(10);
            when(state.getReserveDeckSize(PLAYER)).thenReturn(20);
            when(state.getBattleState()).thenReturn(battle);
            when(state.getBattleLocation()).thenReturn(location);
            when(state.getAllPermanentCards()).thenReturn(List.of(target));
            when(state.getHand(PLAYER)).thenReturn(List.of());
            when(target.isHit()).thenReturn(true);
            PhysicalCard source = card(10, "Removal interrupt", PLAYER, CardCategory.INTERRUPT);
            when(source.getBlueprintId(true)).thenReturn(sourceBlueprint);
            when(state.findCardById(10)).thenReturn(source);
            battle.addDarkCardParticipant(card(11, "Dooku", PLAYER, CardCategory.CHARACTER));
            battle.addLightCardParticipant(target);
        }
    }
}
