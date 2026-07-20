package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TargetSelectionCardSelectionPolicyParityTest {

    @Test
    public void unresolvedCandidateRetainsTaggedBaseWithoutReasoning() {
        GameState gameState = gameState(Map.of());

        var rando = evaluateRando(gameState, null, "Choose target for weapon",
                List.of("not-a-number"), Phase.CONTROL);
        var chosen = evaluateChosen(gameState, null, "Choose target for weapon",
                List.of("not-a-number"), Phase.CONTROL);

        assertActionParity(rando, chosen);
        assertBits(50.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning());
    }

    @Test
    public void beneficialOwnAndOpponentRetainPowerFiveBoundaryAndTotals() {
        Map<Integer, PhysicalCard> cards = new LinkedHashMap<>();
        cards.put(1, card("Own Low", "tester", 4.99f, false, false, false));
        cards.put(2, card("Own High Unique", "tester", 5.0f, true, false, false));
        cards.put(3, card("Opponent High Unique", "opponent", 5.0f, true, false, false));
        GameState gameState = gameState(cards);
        String prompt = "Choose target for A Few Maneuvers";

        var rando = evaluateRando(gameState, null, prompt,
                List.of("1", "2", "3"), Phase.CONTROL);
        var chosen = evaluateChosen(gameState, null, prompt,
                List.of("1", "2", "3"), Phase.CONTROL);

        assertActionParity(rando, chosen);
        assertBits(100.0f, action(rando, "1").getScore());
        assertReasons(action(rando, "1").getReasoning(),
                "Beneficial effect on our card (+50.0)");
        assertBits(150.0f, action(rando, "2").getScore());
        assertReasons(action(rando, "2").getReasoning(),
                "Beneficial effect on our card (+50.0)",
                "High-power target for buff (+30.0)",
                "Unique target for buff (+20.0)");
        assertBits(-150.0f, action(rando, "3").getScore());
        assertReasons(action(rando, "3").getReasoning(),
                "Don't buff opponent's card! (-200.0)");
    }

    @Test
    public void harmfulOutsideBattlePinsHitSpyAndValueBoundaries() {
        Map<Integer, PhysicalCard> cards = new LinkedHashMap<>();
        cards.put(10, card("Ordinary", "opponent", 4.99f, false, false, false));
        cards.put(11, card("High Unique", "opponent", 5.0f, true, false, false));
        cards.put(12, card("Spy", "opponent", 4.99f, false, false, true));
        cards.put(13, card("High Unique Spy", "opponent", 5.0f, true, false, true));
        cards.put(14, card("Hit", "opponent", 4.99f, false, true, false));
        cards.put(15, card("High Unique Hit", "opponent", 5.0f, true, true, false));
        cards.put(16, card("Hit Spy", "opponent", 4.99f, false, true, true));
        cards.put(17, card("High Unique Hit Spy", "opponent", 5.0f, true, true, true));
        GameState gameState = gameState(cards);
        List<String> ids = List.of("10", "11", "12", "13", "14", "15", "16", "17");

        var rando = evaluateRando(gameState, null, "Choose target for weapon",
                ids, Phase.CONTROL);
        var chosen = evaluateChosen(gameState, null, "Choose target for weapon",
                ids, Phase.CONTROL);

        assertActionParity(rando, chosen);
        assertBits(100.0f, action(rando, "10").getScore());
        assertBits(150.0f, action(rando, "11").getScore());
        assertBits(600.0f, action(rando, "12").getScore());
        assertBits(650.0f, action(rando, "13").getScore());
        assertBits(-400.0f, action(rando, "14").getScore());
        assertBits(-350.0f, action(rando, "15").getScore());
        assertBits(100.0f, action(rando, "16").getScore());
        assertBits(150.0f, action(rando, "17").getScore());

        assertReasons(action(rando, "17").getReasoning(),
                "Target opponent's card (+50.0)",
                "V51 ALREADY HIT: Target already hit — don't waste weapon! (-500.0)",
                "V51 KILL SPY: Target is an undercover spy — eliminate it! (+500.0)",
                "High-power target (+30.0)",
                "Unique target (+20.0)");
    }

    @Test
    public void battleDestinyInterleavesBetweenSpyAndUniqueExactlyOnce() {
        PhysicalCard padme = card("Padme Naberrie", "opponent",
                5.0f, true, true, true);
        GameState gameState = gameState(Map.of(20, padme));
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(modifiers.getDefenseValue(gameState, padme)).thenReturn(3.0f);

        var rando = evaluateRando(gameState, game, "Choose target for weapon",
                List.of("20"), Phase.BATTLE);
        var chosen = evaluateChosen(gameState, game, "Choose target for weapon",
                List.of("20"), Phase.BATTLE);

        assertActionParity(rando, chosen);
        assertBits(620.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning(),
                "Target opponent's card (+50.0)",
                "V51 ALREADY HIT: Target already hit — don't waste weapon! (-500.0)",
                "V51 KILL SPY: Target is an undercover spy — eliminate it! (+500.0)",
                "V36 EASY HIT: Padme Naberrie defense 3, expected destiny 6.0 — HIGH hit chance! (+200.0)",
                "V36 PRIORITY: Padme cancels Vader's game text — REMOVE HER! (+300.0)",
                "Unique target (+20.0)");
        assertFalse(rando.get(0).getReasoning().stream()
                .anyMatch(reason -> reason.startsWith("High-power target ")));
    }

    @Test
    public void harmfulSelfRetainsAdditiveVetoTotalAndBotParity() {
        PhysicalCard own = card("Own Character", "tester",
                5.0f, true, false, false);
        GameState gameState = gameState(Map.of(30, own));

        var rando = evaluateRando(gameState, null, "Choose target for weapon",
                List.of("30"), Phase.CONTROL);
        var chosen = evaluateChosen(gameState, null, "Choose target for weapon",
                List.of("30"), Phase.CONTROL);

        assertActionParity(rando, chosen);
        assertBits(-9949.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning(),
                "V38.3 SELF-TARGET: NEVER target own card with harmful effect! (-9999.0)");
    }

    private static GameState gameState(Map<Integer, PhysicalCard> cards) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(2);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> cards.get(invocation.getArgument(0, Integer.class)));
        return gameState;
    }

    private static PhysicalCard card(String title,
                                     String owner,
                                     float power,
                                     boolean unique,
                                     boolean hit,
                                     boolean undercover) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        when(blueprint.getUniqueness()).thenReturn(
                unique ? Uniqueness.UNIQUE : Uniqueness.UNRESTRICTED);

        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isHit()).thenReturn(hit);
        when(card.isUndercover()).thenReturn(undercover);
        return card;
    }

    private static List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
    evaluateRando(GameState gameState, SwccgGame game, String prompt,
                  List<String> cardIds, Phase phase) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "CARD_SELECTION", prompt,
                "target-selection", phase);
        context.setGame(game);
        context.setCardIds(cardIds);
        context.setSelectable(java.util.Collections.nCopies(cardIds.size(), true));
        return new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(context);
    }

    private static List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction>
    evaluateChosen(GameState gameState, SwccgGame game, String prompt,
                   List<String> cardIds, Phase phase) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "CARD_SELECTION", prompt,
                "target-selection", phase);
        context.setGame(game);
        context.setCardIds(cardIds);
        context.setSelectable(java.util.Collections.nCopies(cardIds.size(), true));
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(context);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> actions,
            String actionId) {
        return actions.stream()
                .filter(action -> actionId.equals(action.getActionId()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertActionParity(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> chosen) {
        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertBits(rando.get(i).getScore(), chosen.get(i).getScore());
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }

    private static void assertReasons(List<String> actual, String... expected) {
        assertEquals(List.of(expected), actual);
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
