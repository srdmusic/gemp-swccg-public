package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.actions.GameTextActionState;
import com.gempukku.swccgo.logic.actions.GameTextAction;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WmaopCardSelectionBoundaryTest {
    private static final String PLAYER = "tester";
    private static final String OPPONENT = "opponent";
    private static final String TAKE_VETO =
            "WMAOP.BLOCKADE_ONLY: 'Battle Order' is not the Blockade Flagship site"
                    + " — WMAOP is never spent on Effect/Podracer pulls (-2000.0)";
    private static final String RESERVE_PREFERENCE =
            "WMAOP.BLOCKADE_ONLY: prefer the Blockade Flagship site"
                    + " — the only sanctioned WMAOP pull (+1000.0)";
    private static final String RESERVE_VETO =
            "WMAOP.BLOCKADE_ONLY: non-Blockade candidate offered by a WMAOP search"
                    + " — veto (-2000.0)";

    @Test
    public void ownedWmaopTakeActionIdVetoesTheUnsanctionedChildInBothMirrors() {
        assertTakeBoundary(
                GameTextActionId.WE_MUST_ACCELERATE_OUR_PLANS__UPLOAD_EFFECT,
                PLAYER, true);
    }

    @Test
    public void unrelatedActionIdDoesNotInheritTheWmaopChildVetoInEitherMirror() {
        assertTakeBoundary(
                GameTextActionId.OTHER_CARD_ACTION_DEFAULT,
                PLAYER, false);
    }

    @Test
    public void opponentsWmaopActionDoesNotVetoOurChildInEitherMirror() {
        assertTakeBoundary(
                GameTextActionId.WE_MUST_ACCELERATE_OUR_PLANS__DOWNLOAD_BLOCKADE_FLAGSHIP_SITE,
                OPPONENT, false);
    }

    @Test
    public void reserveChildPrefersBlockadeVetoesResolvedOtherAndLeavesUnknownNeutral() {
        GameState gameState = gameStateWithLiveWmaop(
                GameTextActionId.WE_MUST_ACCELERATE_OUR_PLANS__DOWNLOAD_BLOCKADE_FLAGSHIP_SITE,
                PLAYER);
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        gameState, PLAYER, "ARBITRARY_CARDS",
                        "Choose card to deploy from Reserve Deck",
                        "wmaop-reserve-rando", Phase.DEPLOY);
        configureReserve(randoContext, game);
        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        gameState, PLAYER, "ARBITRARY_CARDS",
                        "Choose card to deploy from Reserve Deck",
                        "wmaop-reserve-chosen", Phase.DEPLOY);
        configureReserve(chosenContext, game);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                        .evaluate(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                        .evaluate(chosenContext);

        assertEquals(List.of("0", "1", "2"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertEquals(rando.size(), chosen.size());
        assertTrue(rando.get(0).getReasoning().contains(RESERVE_PREFERENCE));
        assertTrue(rando.get(1).getReasoning().contains(RESERVE_VETO));
        assertFalse(hasWmaopReason(rando.get(2).getReasoning()));
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getReasoning(),
                    chosen.get(i).getReasoning());
        }
    }

    private static void assertTakeBoundary(
        GameTextActionId actionId, String sourceOwner,
            boolean expectWmaopVeto) {
        GameState gameState = gameStateWithLiveWmaop(actionId, sourceOwner);
        PhysicalCard effect = mock(PhysicalCard.class);
        SwccgCardBlueprint effectBlueprint = mock(SwccgCardBlueprint.class);
        when(effect.getTitle()).thenReturn("Battle Order");
        when(effect.getBlueprint()).thenReturn(effectBlueprint);
        when(effectBlueprint.getCardCategory()).thenReturn(CardCategory.EFFECT);
        when(effectBlueprint.getDestiny()).thenReturn(5.0f);
        when(gameState.findCardById(101)).thenReturn(effect);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        gameState, PLAYER, "CARD_SELECTION",
                        "Choose card to take into hand from Reserve Deck",
                        "wmaop-take-rando", Phase.DEPLOY);
        configureTake(randoContext);
        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        gameState, PLAYER, "CARD_SELECTION",
                        "Choose card to take into hand from Reserve Deck",
                        "wmaop-take-chosen", Phase.DEPLOY);
        configureTake(chosenContext);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                        .evaluate(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                        .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertEquals(1, chosen.size());
        assertEquals(rando.get(0).getReasoning(), chosen.get(0).getReasoning());
        if (expectWmaopVeto) {
            assertTrue(rando.get(0).getReasoning().contains(TAKE_VETO));
        } else {
            assertFalse(hasWmaopReason(rando.get(0).getReasoning()));
            assertFalse(rando.get(0).isHardVetoed());
            assertFalse(chosen.get(0).isHardVetoed());
        }
    }

    private static GameState gameStateWithLiveWmaop(
            GameTextActionId actionId, String sourceOwner) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getTopLocations()).thenReturn(List.of());

        GameTextAction live = mock(GameTextAction.class);
        PhysicalCard source = mock(PhysicalCard.class);
        when(source.getTitle()).thenReturn("We Must Accelerate Our Plans");
        when(source.getOwner()).thenReturn(sourceOwner);
        when(live.getActionSource()).thenReturn(source);
        when(live.getGameTextActionId()).thenReturn(actionId);
        when(gameState.getTopGameTextActionState())
                .thenReturn(new GameTextActionState(1, live));
        return gameState;
    }

    private static boolean hasWmaopReason(List<String> reasons) {
        return reasons.stream().anyMatch(
                reason -> reason.startsWith("WMAOP.BLOCKADE_ONLY"));
    }

    private static void configureTake(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context) {
        context.setCardIds(List.of("101"));
        context.setBlueprints(List.of("inPlay"));
        context.setSelectable(List.of(true));
        context.setTestingTexts(List.of("Battle Order"));
    }

    private static void configureTake(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context) {
        context.setCardIds(List.of("101"));
        context.setBlueprints(List.of("inPlay"));
        context.setSelectable(List.of(true));
        context.setTestingTexts(List.of("Battle Order"));
    }

    private static void configureReserve(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            SwccgGame game) {
        context.setGame(game);
        context.setBlueprints(List.of("12_164", "12_174", "999_999"));
        context.setSelectable(List.of(true, true, true));
        context.setTestingTexts(List.of(
                "Blockade Flagship: Bridge",
                "Naboo: Theed Palace Throne Room"));
    }

    private static void configureReserve(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            SwccgGame game) {
        context.setGame(game);
        context.setBlueprints(List.of("12_164", "12_174", "999_999"));
        context.setSelectable(List.of(true, true, true));
        context.setTestingTexts(List.of(
                "Blockade Flagship: Bridge",
                "Naboo: Theed Palace Throne Room"));
    }
}
