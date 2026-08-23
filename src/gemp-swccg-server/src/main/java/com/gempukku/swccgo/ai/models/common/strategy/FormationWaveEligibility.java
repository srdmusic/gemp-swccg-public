package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Collection;
import java.util.Set;

/** Shared admissibility gate for companions counted in a contact wave. */
public final class FormationWaveEligibility {

    private FormationWaveEligibility() {
    }

    public static boolean isAdmissible(SwccgGame game,
                                       GameState gameState,
                                       String playerId,
                                       PhysicalCard deployingCard,
                                       PhysicalCard candidate,
                                       PhysicalCard destination,
                                       Collection<PhysicalCard> selected,
                                       boolean blacklisted) {
        if (game == null || gameState == null || playerId == null
                || candidate == null || candidate.getBlueprint() == null
                || destination == null || blacklisted
                || candidate.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER) {
            return false;
        }
        try {
            if (AiCardHelper.isDeadCard(candidate, game, playerId)
                    || conflicts(candidate, deployingCard)) {
                return false;
            }
            if (selected != null) {
                for (PhysicalCard alreadySelected : selected) {
                    if (conflicts(candidate, alreadySelected)) return false;
                }
            }
            return Filters.deployableToLocation(
                    candidate, Filters.sameCardId(destination),
                    false, 0.0f)
                    .accepts(gameState,
                            game.getModifiersQuerying(), candidate);
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean conflicts(PhysicalCard first, PhysicalCard second) {
        if (first == null || second == null || first == second
                || first.getBlueprint() == null
                || second.getBlueprint() == null) {
            return first != null && first == second;
        }
        SwccgCardBlueprint firstBlueprint = first.getBlueprint();
        SwccgCardBlueprint secondBlueprint = second.getBlueprint();
        if (firstBlueprint.getUniqueness() == Uniqueness.UNIQUE
                && secondBlueprint.getUniqueness() == Uniqueness.UNIQUE
                && first.getTitle() != null && second.getTitle() != null
                && first.getTitle().equalsIgnoreCase(second.getTitle())) {
            return true;
        }
        Set<Persona> firstPersonas = firstBlueprint.getPersonas();
        Set<Persona> secondPersonas = secondBlueprint.getPersonas();
        if (firstPersonas == null || secondPersonas == null
                || firstPersonas.isEmpty() || secondPersonas.isEmpty()) {
            return false;
        }
        for (Persona persona : firstPersonas) {
            if (secondPersonas.contains(persona)) return true;
        }
        return false;
    }
}
