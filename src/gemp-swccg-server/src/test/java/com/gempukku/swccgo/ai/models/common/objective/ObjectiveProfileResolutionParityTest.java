package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectiveProfileResolutionParityTest {

    private static final String PLAYER = "objective-profile-player";

    @Test
    public void randoAndChosenOneUseTheSameBlueprintFirstProfileCorpus() {
        assertProfileCorpus(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer::new);
        assertProfileCorpus(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer::new);
    }

    private static void assertProfileCorpus(Supplier<ObjectiveFactsSource> factory) {
        ObjectiveFacts.ProfileResolution blueprint = analyze(
                factory.get(), "7_135", "Hidden Base");
        assertEquals(ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID,
                blueprint.matchKind());
        assertEquals("Dantooine Base Operations", blueprint.label());
        assertTrue(blueprint.loaderEnabled());
        assertTrue(blueprint.hydratedFromJson());
        assertFalse(blueprint.compiledFallbackUsed());

        ObjectiveFacts.ProfileResolution title = analyze(
                factory.get(), "test_unknown_profile", "Hidden Base");
        assertEquals(ObjectiveFacts.ProfileResolution.MatchKind.TITLE_COMPATIBILITY,
                title.matchKind());
        assertEquals("Hidden Base", title.label());
        assertFalse(title.loaderEnabled());
        assertFalse(title.hydratedFromJson());
        assertFalse(title.compiledFallbackUsed());

        ObjectiveFacts.ProfileResolution compiled = analyze(
                factory.get(), "test_missing_profile", "My Lord Compatibility Probe");
        assertEquals(ObjectiveFacts.ProfileResolution.MatchKind.NONE, compiled.matchKind());
        assertEquals("My Lord", compiled.label());
        assertFalse(compiled.loaderEnabled());
        assertFalse(compiled.hydratedFromJson());
        assertTrue(compiled.compiledFallbackUsed());
    }

    private static ObjectiveFacts.ProfileResolution analyze(
            ObjectiveFactsSource analyzer,
            String blueprintId,
            String title) {
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(front.getTitle()).thenReturn(title);
        when(front.getGameText()).thenReturn(
                "Deploy Test Site. Flip this card if you control Test Site.");
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);
        when(back.getTitle()).thenReturn("Back Side");
        when(back.getGameText()).thenReturn(
                "Flip this card if you do not control Test Site.");

        PhysicalCard objective = mock(PhysicalCard.class);
        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn(blueprintId);
        when(objective.isFlipped()).thenReturn(false);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);

        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(objective));
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);

        analyzer.analyze(game, PLAYER, Side.DARK);
        assertTrue(analyzer.isAnalyzed());
        return analyzer.getProfileResolution();
    }
}
