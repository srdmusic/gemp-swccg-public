package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ADDED 2026-08-08 (passivity fix, m01683): the L2 battle veto now honors the
 * SOLO DOMINANCE exemption (>= 2x weapon-adjusted power) that was documented in
 * the FormationSafety header but implemented only on the move side — a dominant
 * force may voluntarily battle without a destiny draw.
 */
public class FormationSafetyBattleVetoTest {

    private static final String US = "bot";
    private static final String THEM = "opponent";

    @Test
    public void dominantNoDestinyForceMayInitiateBattle() {
        Fixture fixture = fixture(3.0f, 10.0f, 4.0f);
        assertNull("10 vs 4 is >= 2x — L2 must be waived",
                FormationSafety.vetoInitiateBattle(
                        fixture.game, fixture.gameState, US, fixture.location));
    }

    @Test
    public void nonDominantNoDestinyForceStaysVetoed() {
        Fixture fixture = fixture(3.0f, 7.0f, 4.0f);
        String veto = FormationSafety.vetoInitiateBattle(
                fixture.game, fixture.gameState, US, fixture.location);
        assertNotNull("7 vs 4 is < 2x — L2 must still veto", veto);
        assertTrue(veto, veto.contains("L2 NO-DESTINY BATTLE"));
    }

    @Test
    public void waiverRequiresARealOpponentPresence() {
        // oppEff 0 gives no meaningful dominance read — the veto must stand for
        // a no-destiny force against a zero-power (but possibly destiny-capable)
        // defender.
        Fixture fixture = fixture(3.0f, 10.0f, 0.0f);
        assertNotNull("no waiver against zero-power opposition",
                FormationSafety.vetoInitiateBattle(
                        fixture.game, fixture.gameState, US, fixture.location));
    }

    @Test
    public void destinyEligibleForceNeedsNoWaiver() {
        Fixture fixture = fixture(4.0f, 5.0f, 4.0f);
        assertNull(FormationSafety.vetoInitiateBattle(
                fixture.game, fixture.gameState, US, fixture.location));
    }

    private record Fixture(SwccgGame game, GameState gameState,
                           PhysicalCard location) {
    }

    private static Fixture fixture(
            float ourAbility, float ourPower, float theirPower) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent(US)).thenReturn(THEM);
        // No characters at the location — weaponBonusAt contributes 0 each side.
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(location.getTitle()).thenReturn("Contested Site");
        when(modifiers.getTotalAbilityAtLocation(any(), anyString(), any()))
                .thenAnswer(invocation ->
                        US.equals(invocation.getArgument(1)) ? ourAbility : 4.0f);
        when(modifiers.getTotalPowerAtLocation(
                any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                        US.equals(invocation.getArgument(2))
                                ? ourPower : theirPower);
        return new Fixture(game, gameState, location);
    }
}
