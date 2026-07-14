package com.gempukku.swccgo.logic.timing.processes.pregame;

import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartingObjectiveDecisionOriginTest {

    @Test
    public void startingLocationDecisionCarriesTypedSetupOrigin() {
        SwccgGame game = game();
        PlayStartingLocationsAndObjectivesGameProcess process =
                new PlayStartingLocationsAndObjectivesGameProcess(game);

        assertOrigin(
                process.createChooseLocationDecision(game, "dark", List.of()),
                DecisionOrigin.SETUP_STARTING_LOCATION);
    }

    @Test
    public void startingInterruptDecisionCarriesTypedSetupOrigin() {
        SwccgGame game = game();
        PlayStartingInterruptsGameProcess process =
                new PlayStartingInterruptsGameProcess(game);

        assertOrigin(
                process.createChooseStartingInterruptDecision(game, "dark", List.of()),
                DecisionOrigin.SETUP_STARTING_INTERRUPT);
    }

    private static void assertOrigin(AwaitingDecision decision, DecisionOrigin origin) {
        assertEquals(AwaitingDecisionType.ARBITRARY_CARDS, decision.getDecisionType());
        assertEquals(AwaitingDecisionType.ARBITRARY_CARDS.name(), origin.requiredWireTypeName());
        assertArrayEquals(
                new String[]{origin.name()},
                decision.getDecisionParameters().get(DecisionOrigin.WIRE_PARAMETER));
    }

    private static SwccgGame game() {
        SwccgGame game = mock(SwccgGame.class);
        when(game.getDarkPlayer()).thenReturn("dark");
        when(game.getLightPlayer()).thenReturn("light");
        return game;
    }
}
