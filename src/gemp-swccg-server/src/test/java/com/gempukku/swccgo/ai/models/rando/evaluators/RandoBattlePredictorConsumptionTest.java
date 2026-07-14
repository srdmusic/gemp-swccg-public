package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.AbstractBattlePredictorConsumptionTest;
import com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle;
import com.gempukku.swccgo.common.Phase;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Rando adapter for frozen predictor consumption. */
public class RandoBattlePredictorConsumptionTest
        extends AbstractBattlePredictorConsumptionTest {

    @Override
    protected void evaluateAndAssertNoPredictorInputRead(Scenario scenario) {
        DecisionContext context = new DecisionContext(
                scenario.gameState(), "bot", "CARD_ACTION_CHOICE",
                "Choose battle action", "42", Phase.BATTLE);
        context.setActionIds(List.of("0"));
        context.setActionTexts(List.of("Initiate battle at Test Site"));
        context.setCardIds(List.of("301"));
        context.setGame(scenario.game());
        context.setBattleTransaction(scenario.facts(), scenario.assessment());
        DeckOracle deckOracle = mock(DeckOracle.class);
        context.setDeckOracle(deckOracle);

        new BattleEvaluator().evaluate(context);

        verifyNoInteractions(deckOracle);
    }
}
