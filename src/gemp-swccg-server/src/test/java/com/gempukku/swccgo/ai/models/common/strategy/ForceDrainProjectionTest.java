package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForceDrainProjectionTest {

    @Test
    public void shieldControlProjectionRequiresUncanceledNewLeaderBonus() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard shieldControl = mock(PhysicalCard.class);
        PhysicalCard candidate = mock(PhysicalCard.class);
        SwccgCardBlueprint candidateBlueprint =
                mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("dark")).thenReturn("light");
        when(shieldControl.getBlueprintId(true)).thenReturn("208_55");
        when(shieldControl.getOwner()).thenReturn("dark");
        when(candidate.getOwner()).thenReturn("dark");
        when(candidate.getBlueprint()).thenReturn(candidateBlueprint);
        when(candidateBlueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(modifiers.hasIcon(gameState, candidate, Icon.FIRST_ORDER))
                .thenReturn(true);
        when(modifiers.hasKeyword(gameState, candidate, Keyword.LEADER))
                .thenReturn(true);
        when(gameState.getCardsAtLocation(shieldControl))
                .thenReturn(List.of());
        when(modifiers.getForceDrainAmount(
                gameState, shieldControl, "dark")).thenReturn(1.0f);
        when(modifiers.getForceToLoseFromForceDrainLimit(
                gameState, "light", shieldControl)).thenReturn(99.0f);

        assertEquals(2.0f, ForceDrainProjection
                .projectedDamageAfterCharacterDeploy(
                    gameState, game, shieldControl, "dark", candidate),
                0.0f);

        when(modifiers.hasKeyword(
                gameState, candidate, Keyword.LEADER)).thenReturn(false);
        assertEquals(1.0f, ForceDrainProjection
                .projectedDamageAfterCharacterDeploy(
                    gameState, game, shieldControl, "dark", candidate),
                0.0f);

        when(modifiers.hasKeyword(
                gameState, candidate, Keyword.LEADER)).thenReturn(true);
        when(modifiers.isForceDrainModifierCanceled(
                gameState, shieldControl, shieldControl,
                "dark", "dark", 1.0f)).thenReturn(true);
        assertEquals(1.0f, ForceDrainProjection
                .projectedDamageAfterCharacterDeploy(
                    gameState, game, shieldControl, "dark", candidate),
                0.0f);

        when(modifiers.isForceDrainModifierCanceled(
                gameState, shieldControl, shieldControl,
                "dark", "dark", 1.0f)).thenReturn(false);
        when(modifiers.getForceToLoseFromForceDrainLimit(
                gameState, "light", shieldControl)).thenReturn(1.0f);
        assertEquals(1.0f, ForceDrainProjection
                .projectedDamageAfterCharacterDeploy(
                    gameState, game, shieldControl, "dark", candidate),
                0.0f);

        PhysicalCard existingLeader = mock(PhysicalCard.class);
        SwccgCardBlueprint existingBlueprint =
                mock(SwccgCardBlueprint.class);
        when(existingLeader.getOwner()).thenReturn("dark");
        when(existingLeader.getZone()).thenReturn(Zone.AT_LOCATION);
        when(existingLeader.isUndercover()).thenReturn(false);
        when(existingLeader.getBlueprint()).thenReturn(existingBlueprint);
        when(existingBlueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(modifiers.hasIcon(
                gameState, existingLeader, Icon.FIRST_ORDER))
                .thenReturn(true);
        when(modifiers.hasKeyword(
                gameState, existingLeader, Keyword.LEADER))
                .thenReturn(true);
        when(gameState.getCardsAtLocation(shieldControl))
                .thenReturn(List.of(existingLeader));
        when(modifiers.getForceDrainAmount(
                gameState, shieldControl, "dark")).thenReturn(2.0f);
        when(modifiers.getForceToLoseFromForceDrainLimit(
                gameState, "light", shieldControl)).thenReturn(99.0f);
        assertEquals(2.0f, ForceDrainProjection
                .projectedDamageAfterCharacterDeploy(
                    gameState, game, shieldControl, "dark", candidate),
                0.0f);
    }
}
