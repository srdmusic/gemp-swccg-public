package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployFormationPromisePolicyTest {

    @Test
    public void exactAffordableBuddyPersistsUntilFormationIsSafe() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard anchor = character(1, Zone.AT_LOCATION);
        PhysicalCard companion = character(2, Zone.HAND);
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint siteBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(1)).thenReturn(anchor);
        when(gameState.findCardByPermanentId(2)).thenReturn(companion);
        when(gameState.findCardById(100)).thenReturn(site);
        when(site.getCardId()).thenReturn(100);
        when(site.getBlueprint()).thenReturn(siteBlueprint);
        when(siteBlueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        when(modifiers.getLocationThatCardIsAt(gameState, anchor))
                .thenReturn(site);
        when(gameState.getCardsAtLocation(site)).thenReturn(List.of(anchor));
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "rando", site)).thenReturn(1.0f);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(
                gameState, companion, companion, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(5.0f);

        List<DeployFormationPromisePolicy.PlannedInstruction> plan = List.of(
                new DeployFormationPromisePolicy.PlannedInstruction(
                        1, 1, "100"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        2, 2, "100"));
        DeployFormationPromisePolicy.Promise promise =
                DeployFormationPromisePolicy.detect(
                        game, "rando", 5, plan);
        assertNotNull(promise);
        assertTrue(DeployFormationPromisePolicy.shouldContinue(
                game, "rando", 5, promise));
        assertFalse("The exact cost boundary is strict",
                DeployFormationPromisePolicy.shouldContinue(
                    game, "rando", 4, promise));

        when(modifiers.getTotalAbilityAtLocation(
                gameState, "rando", site)).thenReturn(4.0f);
        assertFalse("Ability four releases the package lock",
                DeployFormationPromisePolicy.shouldContinue(
                    game, "rando", 5, promise));

        when(modifiers.getTotalAbilityAtLocation(
                gameState, "rando", site)).thenReturn(1.0f);
        when(anchor.getCardId()).thenReturn(9);
        assertNull("A stale current-card id cannot inherit the promise",
                DeployFormationPromisePolicy.detect(
                    game, "rando", 5, plan));
    }

    private static PhysicalCard character(int id, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getOwner()).thenReturn("rando");
        when(card.getZone()).thenReturn(zone);
        when(card.isUndercover()).thenReturn(false);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        return card;
    }
}
