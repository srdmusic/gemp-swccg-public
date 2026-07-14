package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shared proof that typed BATTLE never reruns an unknown frozen prediction. */
public abstract class AbstractBattlePredictorConsumptionTest {

    protected record Scenario(
            GameState gameState,
            SwccgGame game,
            BattleFacts facts,
            BattleAssessment assessment) {
    }

    @Test
    public void unknownFrozenPredictionDoesNotReadPredictorInputsAgain() {
        evaluateAndAssertNoPredictorInputRead(scenario());
    }

    protected abstract void evaluateAndAssertNoPredictorInputRead(Scenario scenario);

    private static Scenario scenario() {
        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getTitle()).thenReturn("Test Site");

        GameState gameState = mock(GameState.class);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.findCardById(301)).thenReturn(target);
        when(gameState.getTopLocations()).thenReturn(List.of(target));
        when(gameState.getCardsAtLocation(target)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());

        SwccgGame game = mock(SwccgGame.class);
        BattleLocationAssessment location = new BattleLocationAssessment(
                true, 12f, 7f, 4f, 2f, 0f, 0f,
                1, 1, true, null, false, Set.of(),
                false, false, false, false, false,
                BattlePredictionAssessment.unknown());
        BattleFacts facts = new BattleFacts(
                "42", Phase.BATTLE, AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleWindowRoute.INITIATE,
                List.of(new BattleFacts.Candidate(
                        0, "0", "301", DecisionActionSemantic.BATTLE_INITIATE,
                        BattleCandidateRole.INITIATE)), false);
        BattleAssessment assessment = new BattleAssessment(
                BattleWindowRoute.INITIATE,
                List.of(new BattleInitiationAssessment(
                        0, 301, BattleDeployIntent.none(), location)), false);
        return new Scenario(gameState, game, facts, assessment);
    }
}
