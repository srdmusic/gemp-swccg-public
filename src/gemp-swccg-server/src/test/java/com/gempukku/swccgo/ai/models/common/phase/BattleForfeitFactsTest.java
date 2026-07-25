package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BattleForfeitFactsTest {

    @Test
    public void formationAlternativesRespectOptionalPassAndAttrition() {
        GameState gameState = mock(GameState.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);
        PhysicalCard actor = mock(PhysicalCard.class);
        PhysicalCard forceLoss = mock(PhysicalCard.class);
        when(gameState.findCardById(1)).thenReturn(actor);
        when(gameState.findCardById(2)).thenReturn(forceLoss);
        when(forceLoss.getZone()).thenReturn(Zone.RESERVE_DECK);
        when(analyzer.classifyGateFormationPieceIfRemoved(
                null, "player", actor)).thenReturn(
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR);
        when(analyzer.classifyGateFormationPieceIfRemoved(
                null, "player", forceLoss)).thenReturn(
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE);

        BattleForfeitFacts.FlipGateFormationSelectionFacts attrition =
                BattleForfeitFacts.readFlipGateFormationSelection(
                    List.of("1", "2"), gameState, null, "player",
                    analyzer, false, 3);
        BattleForfeitFacts.FlipGateFormationSelectionFacts pureDamage =
                BattleForfeitFacts.readFlipGateFormationSelection(
                    List.of("1", "2"), gameState, null, "player",
                    analyzer, false, 0);
        BattleForfeitFacts.FlipGateFormationSelectionFacts optional =
                BattleForfeitFacts.readFlipGateFormationSelection(
                    List.of("1"), gameState, null, "player",
                    analyzer, true, 3);

        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                .LAST_REQUIRED_ACTOR, attrition.roleFor("1"));
        assertFalse(attrition.hasUnprotectedLegalAlternative());
        assertTrue(pureDamage.hasUnprotectedLegalAlternative());
        assertTrue(optional.hasUnprotectedLegalAlternative());
    }

    @Test
    public void candidateSetKeepsFirstLowestForfeitHitAndIndependentDeadFact() {
        BattleForfeitFacts.CandidateSetFacts facts = BattleForfeitFacts.readCandidateSet(
                List.of(
                        candidate("hit-high", true, 7.0f, true, false),
                        candidate("dead", true, 3.0f, false, true),
                        candidate("hit-low-first", true, 2.0f, true, false),
                        candidate("hit-low-tie", true, 2.0f, true, false)));

        assertTrue(facts.hasHitCandidates());
        assertTrue(facts.hasDeadCandidates());
        assertEquals("hit-low-first", facts.bestHitActionId().orElseThrow());
        assertBits(2.0f, facts.bestHitForfeit());
    }

    @Test
    public void missingBlueprintUsesLegacyZeroForfeitDuringBestHitScan() {
        BattleForfeitFacts.CandidateSetFacts facts = BattleForfeitFacts.readCandidateSet(
                List.of(
                        candidate("normal-hit", true, 2.0f, true, false),
                        candidate("missing-blueprint", false, 99.0f, true, false)));

        assertEquals("missing-blueprint", facts.bestHitActionId().orElseThrow());
        assertBits(0.0f, facts.bestHitForfeit());
    }

    @Test
    public void exactImmunityTakesPrecedenceOverLessThanImmunity() {
        BattleForfeitFacts.ImmunityFacts exact =
                new BattleForfeitFacts.ImmunityFacts(4.0f, 10.0f);
        assertTrue(exact.immuneTo(4));
        assertFalse(exact.immuneTo(3));

        BattleForfeitFacts.ImmunityFacts lessThan =
                new BattleForfeitFacts.ImmunityFacts(0.0f, 4.0f);
        assertTrue(lessThan.immuneTo(3));
        assertFalse(lessThan.immuneTo(4));
        assertFalse(BattleForfeitFacts.ImmunityFacts.none().immuneTo(1));
    }

    @Test
    public void smallDamageFactRequiresPureDamageOneOrTwo() {
        BattleForfeitFacts.CandidateSetFacts empty =
                BattleForfeitFacts.CandidateSetFacts.empty();
        assertTrue(new BattleForfeitFacts.DecisionFacts(0, 1, empty)
                .smallPureDamage());
        assertTrue(new BattleForfeitFacts.DecisionFacts(0, 2, empty)
                .smallPureDamage());
        assertFalse(new BattleForfeitFacts.DecisionFacts(1, 2, empty)
                .smallPureDamage());
        assertFalse(new BattleForfeitFacts.DecisionFacts(0, 3, empty)
                .smallPureDamage());
    }

    @Test
    public void combinedForceLossCandidateSkipsUnusedBoardQueries() {
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenThrow(new AssertionError("unexpected board read"));
        PhysicalCard card = card(CardCategory.CHARACTER);

        BattleForfeitFacts.CandidateFacts facts = BattleForfeitFacts.readCandidate(
                "force-loss", card, game, "player", true, 0, 2, true);

        assertTrue(facts.forceLossOption());
        assertEquals(CardCategory.CHARACTER, facts.category());
        assertFalse(facts.soloPower().available());
    }

    @Test
    public void combinedWeaponCandidateSkipsUnusedBoardQueries() {
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenThrow(new AssertionError("unexpected board read"));
        PhysicalCard card = card(CardCategory.WEAPON);

        BattleForfeitFacts.CandidateFacts facts = BattleForfeitFacts.readCandidate(
                "weapon", card, game, "player", false, 4, 8, true);

        assertTrue(facts.weapon());
        assertFalse(facts.soloPower().available());
    }

    private static BattleForfeitFacts.CandidateFacts candidate(
            String actionId, boolean blueprintPresent, float forfeit,
            boolean hit, boolean dead) {
        return new BattleForfeitFacts.CandidateFacts(
                actionId, blueprintPresent, CardCategory.CHARACTER, forfeit,
                hit, dead, false, false, false,
                BattleForfeitFacts.ImmunityFacts.none(),
                BattleForfeitFacts.SoloPowerFacts.unavailable(),
                3.0f, 3.0f, false, false);
    }

    private static PhysicalCard card(CardCategory category) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.hasForfeitAttribute()).thenReturn(false);
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isHit()).thenReturn(false);
        return card;
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
