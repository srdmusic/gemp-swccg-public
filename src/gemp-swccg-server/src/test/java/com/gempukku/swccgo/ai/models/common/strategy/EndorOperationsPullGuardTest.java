package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.PullActionFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullActionFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.PullActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullOracleView;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndorOperationsPullGuardTest {
    private static final String PLAYER = "tester";

    @Test
    public void bunkerEndorShieldBypassesBothFalseWeaponVetoes() {
        Evaluation evaluation = evaluate(
                "Endor: Bunker",
                "Deploy Endor Shield from Reserve Deck",
                "Once per game, may deploy Endor Shield from Reserve Deck.",
                List.of("endor shield"));

        assertFalse(hasRule(evaluation.deploy(), "V185"));
        assertFalse(hasRule(evaluation.actionText(), "V185-ate"));
        assertTrue(hasRule(
                evaluation.actionText(), "EOP-ENDOR-SHIELD-BOOTSTRAP"));
    }

    @Test
    public void realWeaponStillReceivesBothV185Vetoes() {
        Evaluation evaluation = evaluate(
                "Weapon Pull Source",
                "Deploy a weapon from Reserve Deck",
                "May deploy a weapon from Reserve Deck.",
                List.of("weapon"));

        assertTrue(hasRule(evaluation.deploy(), "V185"));
        assertTrue(hasRule(evaluation.actionText(), "V185-ate"));
    }

    private static Evaluation evaluate(
            String sourceTitle,
            String actionText,
            String sourceText,
            List<String> targets) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        PhysicalCard source = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        PullOracleView oracle = mock(PullOracleView.class);

        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(10);
        when(gameState.findCardById(101)).thenReturn(source);
        when(source.getTitle()).thenReturn(sourceTitle);
        when(source.getBlueprint()).thenReturn(blueprint);
        when(oracle.isAvailable()).thenReturn(true);
        when(oracle.isAnalyzed()).thenReturn(true);
        when(oracle.hasTargetInReserve(anyString(), anyString()))
                .thenReturn(true);
        when(oracle.hasTargetInReserve(anyString())).thenReturn(true);
        when(oracle.parseSourceZone(actionText)).thenReturn(Zone.RESERVE_DECK);
        when(oracle.sourceCardFullGameText(blueprint, Side.DARK))
                .thenReturn(sourceText);
        when(oracle.validatePullFromSourceCard(
                Zone.RESERVE_DECK, sourceText))
                .thenReturn(new PullOracleView.Validation(
                        PullOracleView.Outcome.WILL_SUCCEED,
                        "target remains"));
        when(oracle.parseSourceCardPullTargets(sourceText))
                .thenReturn(targets);
        when(oracle.reserveTargetsAreAllUnattachableWeapons(
                same(game), eq(PLAYER), anyList())).thenReturn(true);

        PullDeployFacts deployFacts = PullDeployFactsReader.read(
                "pull", actionText, "101",
                new PullDeployFactsReader.Context(
                        game, gameState, PLAYER, Side.DARK, oracle, null));
        PullActionFacts.Parent actionFacts = PullActionFactsReader.readParent(
                "pull", actionText, "101",
                new PullActionFactsReader.Context(
                        game, gameState, PLAYER, Side.DARK, Phase.DEPLOY,
                        oracle, null, null));
        return new Evaluation(
                PullDeployPolicy.evaluate(deployFacts),
                PullActionPolicy.evaluateParent(actionFacts));
    }

    private static boolean hasRule(
            PullDeployPolicy.Evaluation evaluation,
            String rule) {
        return evaluation.result().operations().stream()
                .anyMatch(operation -> rule.equals(
                        operation.ruleArmId().id()));
    }

    private static boolean hasRule(
            PullActionPolicy.Evaluation evaluation,
            String rule) {
        return evaluation.result().operations().stream()
                .anyMatch(operation -> rule.equals(
                        operation.ruleArmId().id()));
    }

    private record Evaluation(
            PullDeployPolicy.Evaluation deploy,
            PullActionPolicy.Evaluation actionText) {
    }
}
