package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HuntDownLegacyPullContractTest {
    private static final String FRONT_TEXT =
            "While this side up, may take Rogue Shadow into hand from"
                    + " Reserve Deck; reshuffle.";
    private static final String BACK_TEXT =
            "During your control phase, may use 2 Force to take any one"
                    + " card without ability into hand from Reserve Deck;"
                    + " reshuffle.";

    @Test
    public void bothBotParsersSeparateTheNamedFrontPullFromTheGenericBackTutor() {
        List<String> randoFront = com.gempukku.swccgo.ai.models.rando
                .strategy.DeckOracle.parseSourceCardPullTargets(FRONT_TEXT);
        List<String> chosenFront = com.gempukku.swccgo.ai.models.chosenone
                .strategy.DeckOracle.parseSourceCardPullTargets(FRONT_TEXT);
        List<String> randoBack = com.gempukku.swccgo.ai.models.rando
                .strategy.DeckOracle.parseSourceCardPullTargets(BACK_TEXT);
        List<String> chosenBack = com.gempukku.swccgo.ai.models.chosenone
                .strategy.DeckOracle.parseSourceCardPullTargets(BACK_TEXT);

        assertEquals(List.of("rogue shadow"), randoFront);
        assertEquals(randoFront, chosenFront);
        assertEquals(List.of("any one card without ability"), randoBack);
        assertEquals(randoBack, chosenBack);
        assertTrue(randoBack.get(0).length() > 25);
    }

    @Test
    public void genericBackTutorRemainsFailOpenForTheDeadSearchGuard() {
        GameState gameState = mock(GameState.class);
        PhysicalCard source = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        PullOracleView oracle = mock(PullOracleView.class);
        List<String> targets = com.gempukku.swccgo.ai.models.rando
                .strategy.DeckOracle.parseSourceCardPullTargets(BACK_TEXT);

        when(gameState.findCardById(60187)).thenReturn(source);
        when(source.getBlueprint()).thenReturn(blueprint);
        when(oracle.isAnalyzed()).thenReturn(true);
        when(oracle.sourceCardFullGameText(blueprint, Side.DARK))
                .thenReturn(BACK_TEXT);
        when(oracle.parseSourceCardPullTargets(BACK_TEXT))
                .thenReturn(targets);
        when(oracle.hasTargetInZone(
                eq(Zone.RESERVE_DECK), anyString())).thenReturn(false);
        when(oracle.isGenericTypeWord(anyString())).thenReturn(false);
        when(oracle.validatePullFromSourceCard(
                Zone.RESERVE_DECK, BACK_TEXT))
                .thenReturn(new PullOracleView.Validation(
                        PullOracleView.Outcome.WILL_FAIL,
                        "generic predicate cannot be exhausted by title"));

        PullActionFacts.EarlySearch facts =
                PullActionFactsReader.readEarlySearch(
                        "legacy-back-tutor",
                        "Take a card into hand from Reserve Deck",
                        "60187",
                        new PullActionFactsReader.Context(
                                null, gameState, "dark", Side.DARK,
                                Phase.CONTROL, oracle, null, null));

        assertEquals(PullActionFacts.EarlyGate.NONE, facts.gate());
    }
}
