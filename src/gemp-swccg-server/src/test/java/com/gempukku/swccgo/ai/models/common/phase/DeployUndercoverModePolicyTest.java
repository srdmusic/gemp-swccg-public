package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployUndercoverModePolicyTest {

    @Test
    public void mereReferenceToUndercoverSpyDoesNotChangeDeployMode() {
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard ysanne = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getGameText()).thenReturn(
                "May break cover of an Undercover spy here.");

        assertFalse(DeployUndercoverModePolicy.isUndercoverDeploy(
                game, ysanne, blueprint, "Choose where to deploy Ysanne"));
        assertFalse(DeployUndercoverModePolicy.isUndercoverDeploy(
                null, null, null,
                "Choose where to deploy with an Undercover spy"));
        assertFalse(DeployUndercoverModePolicy.isUndercoverDeploy(
                null, null, null, "Target an Undercover spy"));
        assertFalse(DeployUndercoverModePolicy.isUndercoverDeploy(
                null, null, null,
                "Revert deployment as an Undercover spy?"));
    }

    @Test
    public void forcedAndExplicitUndercoverModesRemainDetected() {
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard spy = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.isOnlyDeploysAsUndercoverSpy(game, spy))
                .thenReturn(true);
        assertTrue(DeployUndercoverModePolicy.mustDeployUndercover(
                game, spy, blueprint));
        assertTrue(DeployUndercoverModePolicy.isUndercoverDeploy(
                game, spy, blueprint, "Choose where to deploy"));

        when(blueprint.isOnlyDeploysAsUndercoverSpy(game, spy))
                .thenReturn(false);
        assertTrue(DeployUndercoverModePolicy.predictsUndercoverDeploy(
                null, null, null,
                "Do you want to deploy Jyn Erso as an Undercover spy?"));
    }

    @Test
    public void optionalCapabilityUsesTheSameDrainChoiceAsV170() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard spy = mock(PhysicalCard.class);
        PhysicalCard site = mock(PhysicalCard.class);
        PhysicalCard opponentCard = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(spy.getOwner()).thenReturn("rando");
        when(gameState.getOpponent("rando")).thenReturn("steve");
        when(gameState.getTopLocations()).thenReturn(List.of(site));
        when(gameState.getCardsAtLocation(site))
                .thenReturn(List.of(opponentCard));
        when(opponentCard.getOwner()).thenReturn("steve");
        when(blueprint.mayDeployAsUndercoverSpy(game, spy))
                .thenReturn(true);

        assertFalse("Optional capability is not a mandatory deploy mode",
                DeployUndercoverModePolicy.mustDeployUndercover(
                    game, spy, blueprint));
        assertFalse("V170 will choose a normal deployment when no drain exists",
                DeployUndercoverModePolicy.predictsUndercoverDeploy(
                    game, spy, blueprint, "Choose where to deploy"));
        when(modifiers.getForceDrainAmount(
                gameState, site, "steve")).thenReturn(2.0f);
        assertTrue("V170 will choose Undercover when it can stop a drain",
                DeployUndercoverModePolicy.predictsUndercoverDeploy(
                    game, spy, blueprint, "Choose where to deploy"));
    }

    @Test
    public void modifierGrantedUndercoverMovementRemainsDetected() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard seeker = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(modifiers.isDeploysAndMovesLikeUndercoverSpy(
                gameState, seeker)).thenReturn(true);

        assertTrue(DeployUndercoverModePolicy.mustDeployUndercover(
                game, seeker, blueprint));

        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.WEAPON);
        assertFalse("A Seeker weapon is not a character spy",
                DeployUndercoverModePolicy.mustDeployUndercover(
                    game, seeker, blueprint));
    }
}
