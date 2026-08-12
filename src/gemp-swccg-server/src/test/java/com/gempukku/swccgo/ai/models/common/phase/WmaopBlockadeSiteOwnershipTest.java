package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WmaopBlockadeSiteOwnershipTest {
    private static final String PLAYER = "tester";
    private static final String OPPONENT = "opponent";

    @Test
    public void titleFallbackCountsOnlyTheDecidingPlayersBlockadeSite() {
        GameState gameState = mock(GameState.class);
        PhysicalCard mine = location("Blockade Flagship: Bridge", PLAYER);
        PhysicalCard theirs = location(
                "Blockade Flagship: Docking Bay", OPPONENT);

        when(gameState.getTopLocations()).thenReturn(List.of(theirs));
        assertFalse(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                null, gameState, PLAYER));

        when(gameState.getTopLocations()).thenReturn(List.of(mine));
        assertTrue(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                null, gameState, PLAYER));

        when(gameState.getTopLocations()).thenReturn(List.of(theirs, mine));
        assertTrue(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                null, gameState, PLAYER));
        assertFalse(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                null, gameState, null));
    }

    @Test
    public void engineTypedSpotCountsOnlyTheMatchingOwner() {
        VirtualTableScenario opponentOnly = scenario();
        opponentOnly.StartGame();
        opponentOnly.MoveLocationToTable(
                opponentOnly.GetLSCard("lsBfdb"));

        assertFalse(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                opponentOnly.game(), opponentOnly.game().getGameState(), DS));
        assertTrue(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                opponentOnly.game(), opponentOnly.game().getGameState(), LS));

        VirtualTableScenario ownOnly = scenario();
        ownOnly.StartGame();
        ownOnly.MoveLocationToTable(
                ownOnly.GetDSCard("dsBridge"));

        assertTrue(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                ownOnly.game(), ownOnly.game().getGameState(), DS));
        assertFalse(PullActionFactsReader.blockadeFlagshipSiteOnTable(
                ownOnly.game(), ownOnly.game().getGameState(), LS));
    }

    private static PhysicalCard location(String title, String owner) {
        PhysicalCard location = mock(PhysicalCard.class);
        when(location.getTitle()).thenReturn(title);
        when(location.getOwner()).thenReturn(owner);
        return location;
    }

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("lsBfdb", "14_48");
                }},
                new HashMap<>() {{
                    put("dsBridge", "12_164");
                }},
                20,
                20,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }
}
