package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FormationSafetyDeployTest {

    private static final String ME = "me";
    private static final String OPPONENT = "opponent";

    private static final class EmptyLocationState extends GameState {
        @Override
        public String getOpponent(String playerId) {
            return OPPONENT;
        }

        @Override
        public List<PhysicalCard> getCardsAtLocation(PhysicalCard location) {
            return Collections.emptyList();
        }
    }

    private static PhysicalCard card(String title) {
        return (PhysicalCard) Proxy.newProxyInstance(
            PhysicalCard.class.getClassLoader(),
            new Class<?>[]{PhysicalCard.class},
            (proxy, method, args) -> {
                if ("getTitle".equals(method.getName())) return title;
                throw new UnsupportedOperationException("test fake: " + method.getName());
            });
    }

    private static SwccgGame game(float ourPower, float opponentPower) {
        ModifiersQuerying modifiers = (ModifiersQuerying) Proxy.newProxyInstance(
            ModifiersQuerying.class.getClassLoader(),
            new Class<?>[]{ModifiersQuerying.class},
            (proxy, method, args) -> {
                if ("getTotalPowerAtLocation".equals(method.getName())) {
                    return OPPONENT.equals(args[2]) ? opponentPower : ourPower;
                }
                throw new UnsupportedOperationException("test fake: " + method.getName());
            });
        return (SwccgGame) Proxy.newProxyInstance(
            SwccgGame.class.getClassLoader(),
            new Class<?>[]{SwccgGame.class},
            (proxy, method, args) -> {
                if ("getModifiersQuerying".equals(method.getName())) return modifiers;
                throw new UnsupportedOperationException("test fake: " + method.getName());
            });
    }

    private static FormationSafety.DeployVerdict assess(float ability, float force,
                                                         Float firstCost, Float buddyCost,
                                                         boolean undercover, float opponentPower) {
        return FormationSafety.assessCharacterDeploy(
            game(0, opponentPower), new EmptyLocationState(), ME,
            card("Candidate"), 2f, ability, undercover, card("Site"),
            force, firstCost, buddyCost, null);
    }

    @Test
    public void unsupportedWeakSoloIsDeferred() {
        assertEquals(FormationSafety.DeployConstraint.DEFER_UNSUPPORTED_SOLO,
            assess(3, 8, 2f, null, false, 0).constraint());
    }

    @Test
    public void exactAffordableCompanionAllowsFirstBody() {
        assertEquals(FormationSafety.DeployConstraint.ALLOW,
            assess(3, 8, 2f, 3f, false, 0).constraint());
    }

    @Test
    public void unaffordableCompanionHardBlocksWrongOrder() {
        assertEquals(FormationSafety.DeployConstraint.HARD_BLOCK,
            assess(3, 4, 2f, 3f, false, 0).constraint());
    }

    @Test
    public void contestedAffordableExactCompanionAllowsFirstBody() {
        assertEquals(FormationSafety.DeployConstraint.ALLOW,
            assess(3, 8, 2f, 3f, false, 4).constraint());
    }

    @Test
    public void contestedUnaffordableCompanionHardBlocks() {
        assertEquals(FormationSafety.DeployConstraint.HARD_BLOCK,
            assess(3, 4, 2f, 3f, false, 4).constraint());
    }

    @Test
    public void contestedWeakSoloWithoutCompanionHardBlocks() {
        assertEquals(FormationSafety.DeployConstraint.HARD_BLOCK,
            assess(3, 8, 2f, null, false, 4).constraint());
    }

    @Test
    public void destinyEligibleOrUndercoverSoloIsAllowed() {
        assertEquals(FormationSafety.DeployConstraint.ALLOW,
            assess(4, 8, 2f, null, false, 0).constraint());
        assertEquals(FormationSafety.DeployConstraint.ALLOW,
            assess(2, 8, 2f, null, true, 0).constraint());
    }

    @Test
    public void incompleteFactsStayUnknown() {
        assertEquals(FormationSafety.DeployConstraint.UNKNOWN,
            FormationSafety.assessCharacterDeploy(null, null, null, null,
                null, null, false, null, 0, null, null, null).constraint());
    }
}
