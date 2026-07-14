package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Proves board reads and the predictor result freeze once per initiation target. */
public class BattleAssessmentCaptureTest {

    @Test
    public void freezesTargetFactsAndInvokesPredictorOnce() {
        BattleFacts facts = BattleTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation(), BattleWindowRoute.INITIATE);

        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getTitle()).thenReturn("Battle Site");

        GameState gameState = mock(GameState.class);
        when(gameState.getOpponent(BattleTestFixtures.PLAYER)).thenReturn("light-player");
        when(gameState.findCardById(301)).thenReturn(target);
        when(gameState.getTopLocations()).thenReturn(List.of(target));
        when(gameState.getCardsAtLocation(target)).thenReturn(List.of());

        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(modifiers.getTotalPowerAtLocation(
                gameState, target, BattleTestFixtures.PLAYER, false, false))
                .thenReturn(12f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, target, "light-player", false, false))
                .thenReturn(7f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, BattleTestFixtures.PLAYER, target)).thenReturn(4f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "light-player", target)).thenReturn(2f);

        SwccgGame game = mock(SwccgGame.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);

        AtomicInteger predictorCalls = new AtomicInteger();
        BattleAssessment assessment = BattleAssessment.capture(
                facts, BattleDeployIntent.none(), game, gameState,
                BattleTestFixtures.PLAYER,
                (ourPower, ourWeaponBonus, ourDraws,
                 opponentPower, opponentWeaponBonus, opponentDraws) -> {
                    predictorCalls.incrementAndGet();
                    return new BattlePredictionAssessment(true, 0.75f, 5f, 1f);
                });

        BattleLocationAssessment location = assessment.initiationAt(1).location();
        assertEquals(1, predictorCalls.get());
        assertTrue(location.known());
        assertEquals(Float.floatToRawIntBits(12f),
                Float.floatToRawIntBits(location.ourPower()));
        assertEquals(Float.floatToRawIntBits(7f),
                Float.floatToRawIntBits(location.opponentPower()));
        assertEquals(Float.floatToRawIntBits(0.75f),
                Float.floatToRawIntBits(location.prediction().winProbability()));

        when(modifiers.getTotalPowerAtLocation(
                gameState, target, BattleTestFixtures.PLAYER, false, false))
                .thenReturn(99f);
        assertEquals(Float.floatToRawIntBits(12f),
                Float.floatToRawIntBits(location.ourPower()));
        assertEquals(1, predictorCalls.get());
    }

    @Test
    public void separatesArmedVaderFromOpponentPermanentLightsaberWeight() {
        BattleFacts facts = BattleTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation(), BattleWindowRoute.INITIATE);

        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getTitle()).thenReturn("Battle Site");
        PhysicalCard vader = character(
                "Lord Vader With Lightsaber", BattleTestFixtures.PLAYER,
                "test_vader", true);
        PhysicalCard luke = character(
                "Luke Skywalker With Lightsaber", "light-player",
                "test_luke", true);

        GameState gameState = mock(GameState.class);
        when(gameState.getOpponent(BattleTestFixtures.PLAYER)).thenReturn("light-player");
        when(gameState.findCardById(301)).thenReturn(target);
        when(gameState.getTopLocations()).thenReturn(List.of(target));
        when(gameState.getCardsAtLocation(target)).thenReturn(List.of(vader, luke));
        when(gameState.getAttachedCards(vader)).thenReturn(List.of());
        when(gameState.getAttachedCards(luke)).thenReturn(List.of());
        when(gameState.getHand(BattleTestFixtures.PLAYER)).thenReturn(List.of());

        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(modifiers.getTotalPowerAtLocation(
                gameState, target, BattleTestFixtures.PLAYER, false, false))
                .thenReturn(10f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, target, "light-player", false, false))
                .thenReturn(10f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, BattleTestFixtures.PLAYER, target)).thenReturn(6f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "light-player", target)).thenReturn(6f);

        SwccgGame game = mock(SwccgGame.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        float[] predictorOpponentWeaponBonus = {-1f};

        BattleAssessment assessment = BattleAssessment.capture(
                facts, BattleDeployIntent.none(), game, gameState,
                BattleTestFixtures.PLAYER,
                (ourPower, ourWeaponBonus, ourDraws,
                 opponentPower, opponentWeaponBonus, opponentDraws) -> {
                    predictorOpponentWeaponBonus[0] = opponentWeaponBonus;
                    return new BattlePredictionAssessment(true, 0.5f, 0f, 0f);
                });

        BattleLocationAssessment location = assessment.initiationAt(1).location();
        assertTrue(location.vaderArmed());
        assertEquals(Float.floatToRawIntBits(0f),
                Float.floatToRawIntBits(location.ourWeaponBonus()));
        assertEquals(Float.floatToRawIntBits(5f),
                Float.floatToRawIntBits(location.opponentWeaponBonus()));
        assertEquals(Float.floatToRawIntBits(5f),
                Float.floatToRawIntBits(predictorOpponentWeaponBonus[0]));
    }

    private static PhysicalCard character(String title, String owner,
                                          String blueprintId,
                                          boolean permanentWeapon) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasIcon(Icon.PERMANENT_WEAPON)).thenReturn(permanentWeapon);
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        return card;
    }
}
