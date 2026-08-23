package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployPlannerCharacterizationTest {
    private static final String PLAYER = "~Rando_Cal";

    @Test
    public void randoPlanUsesExactPhysicalIdentityAndUniqueLegacyFallback() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.ESTABLISH,
                        "test");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction first =
                randoInstruction("1_1", 10, 20);
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction second =
                randoInstruction("1_1", 11, 21);
        plan.addInstruction(first);
        plan.addInstruction(second);

        assertSame(first, plan.getInstructionForPhysicalCard(10, 20, "1_1"));
        assertSame(second, plan.getInstructionForPhysicalCard(11, 21, "1_1"));
        assertNull(plan.getInstructionForPhysicalCard(99, 99, "1_1"));
    }

    @Test
    public void chosenPlanUsesExactPhysicalIdentityAndUniqueLegacyFallback() {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.ESTABLISH,
                        "test");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction first =
                chosenInstruction("1_1", 10, 20);
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction second =
                chosenInstruction("1_1", 11, 21);
        plan.addInstruction(first);
        plan.addInstruction(second);

        assertSame(first, plan.getInstructionForPhysicalCard(10, 20, "1_1"));
        assertSame(second, plan.getInstructionForPhysicalCard(11, 21, "1_1"));
        assertNull(plan.getInstructionForPhysicalCard(99, 99, "1_1"));
    }

    @Test
    public void assessmentCopyDoesNotLeakMutations() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                        "test");
        plan.addInstruction(randoInstruction("1_1", 10, 20));
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan copy =
                plan.assessmentCopy();
        copy.getInstructions().get(0).setDeployCost(9);
        copy.getInstructions().clear();

        assertEquals(1, plan.getInstructions().size());
        assertEquals(3, plan.getInstructions().get(0).getDeployCost());
    }

    @Test
    public void tatooineReplayRepilotDetectionRejectsPermanentPilotShip()
            throws Exception {
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard devastator = starship(
                351, "Devastator (V)");
        PhysicalCard unpiloted = starship(
                352, "Imperial-Class Star Destroyer");
        GameState state = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        var modifiers = mock(
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(state);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(state.getLocationsInOrder()).thenReturn(List.of(location));
        when(state.getCardsAtLocation(location)).thenReturn(
                List.of(devastator, unpiloted));
        when(state.getAboardCards(devastator, false)).thenReturn(List.of());
        when(state.getAboardCards(unpiloted, false)).thenReturn(List.of());
        when(modifiers.isPiloted(state, devastator, false)).thenReturn(true);
        when(modifiers.isPiloted(state, unpiloted, false)).thenReturn(false);

        // Replay 70jll8yaavkpyy8h (game 72314): Devastator (V) has a
        // permanent pilot in card source. A repilot plan must exclude it while
        // retaining a genuinely unpiloted ship. Assert both mirrored planners.
        assertEquals(List.of(unpiloted), findUnpilotedShips(
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner(),
                game));
        assertEquals(List.of(unpiloted), findUnpilotedShips(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner(),
                game));
    }

    @SuppressWarnings("unchecked")
    private static List<PhysicalCard> findUnpilotedShips(
            Object planner, SwccgGame game) throws Exception {
        Method method = planner.getClass().getDeclaredMethod(
                "findUnpilotedShipsInPlay", SwccgGame.class, String.class);
        method.setAccessible(true);
        return (List<PhysicalCard>) method.invoke(planner, game, PLAYER);
    }

    private static PhysicalCard starship(int id, String title) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.STARSHIP);
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(id);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        return card;
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction
    randoInstruction(String blueprint, int permanentId, int currentId) {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction instruction =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                        blueprint, "Card", "loc", "Site", 1, "test");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(3);
        return instruction;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction
    chosenInstruction(String blueprint, int permanentId, int currentId) {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction instruction =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                        blueprint, "Card", "loc", "Site", 1, "test");
        instruction.setCardPermanentCardId(permanentId);
        instruction.setCardCurrentCardId(currentId);
        instruction.setDeployCost(3);
        return instruction;
    }
}
