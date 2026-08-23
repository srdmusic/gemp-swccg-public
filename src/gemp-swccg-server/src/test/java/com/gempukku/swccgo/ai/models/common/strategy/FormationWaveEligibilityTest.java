package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormationWaveEligibilityTest {

    @Test
    public void duplicateUniqueTitleCannotBeCountedTwice() {
        PhysicalCard first = character(
                "Salacious Crumb", Uniqueness.UNIQUE, Set.of());
        PhysicalCard second = character(
                "Salacious Crumb", Uniqueness.UNIQUE, Set.of());

        assertTrue(FormationWaveEligibility.conflicts(first, second));
    }

    @Test
    public void twoVersionsOfOnePersonaCannotShareAProjectedWave() {
        PhysicalCard first = character(
                "Luke Skywalker", Uniqueness.UNIQUE, Set.of(Persona.LUKE));
        PhysicalCard second = character(
                "Luke With Lightsaber", Uniqueness.UNIQUE,
                Set.of(Persona.LUKE));

        assertTrue(FormationWaveEligibility.conflicts(first, second));
    }

    @Test
    public void distinctCharactersRemainEligibleForOneFormation() {
        PhysicalCard han = character(
                "Han Solo", Uniqueness.UNIQUE, Set.of(Persona.HAN));
        PhysicalCard chewie = character(
                "Chewbacca", Uniqueness.UNIQUE, Set.of(Persona.CHEWIE));

        assertFalse(FormationWaveEligibility.conflicts(han, chewie));
    }

    @Test
    public void blacklistFailsClosedBeforeEngineDeployabilityRead() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        PhysicalCard candidate = character(
                "Admiral Ozzel", Uniqueness.UNIQUE, Set.of());
        PhysicalCard destination = mock(PhysicalCard.class);

        assertFalse(FormationWaveEligibility.isAdmissible(
                game, gameState, "player", null, candidate,
                destination, Set.of(), true));
    }

    @Test
    public void engineIllegalCompanionAddsNoProjectedContactWaveAbility() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard candidate = character(
                "Admiral Ozzel", Uniqueness.UNIQUE, Set.of());
        PhysicalCard destination = mock(PhysicalCard.class);
        when(candidate.getBlueprint().hasAbilityAttribute()).thenReturn(true);
        when(candidate.getBlueprint().getAbility()).thenReturn(2.0f);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(false);

        boolean admissible = FormationWaveEligibility.isAdmissible(
                game, gameState, "player", null, candidate,
                destination, Set.of(), false);
        float projectedAbility = admissible
                ? candidate.getBlueprint().getAbility() : 0.0f;

        assertFalse(admissible);
        assertEquals(0.0f, projectedAbility, 0.0f);
    }

    private static PhysicalCard character(
            String title, Uniqueness uniqueness, Set<Persona> personas) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.getUniqueness()).thenReturn(uniqueness);
        when(blueprint.getPersonas()).thenReturn(personas);
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        return card;
    }
}
