package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgBuiltInCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;

/** Stock weapon state translated to the retained battle scoring weights. */
public record BattleWeaponProfile(float bonus, boolean armed) {

    public static BattleWeaponProfile assess(
            SwccgGame game, GameState gameState, PhysicalCard character) {
        if (game == null || gameState == null || character == null) {
            return new BattleWeaponProfile(0f, false);
        }

        float bonus = 0f;
        boolean armed = false;

        SwccgBuiltInCardBlueprint permanentWeapon =
                game.getModifiersQuerying().getPermanentWeapon(gameState, character);
        if (permanentWeapon != null) {
            bonus += Filters.lightsaber.accepts(game, permanentWeapon) ? 5f : 3f;
            armed = true;
        }

        List<PhysicalCard> attachments = gameState.getAttachedCards(character);
        if (attachments != null) {
            for (PhysicalCard attachment : attachments) {
                if (attachment == null || !Filters.weapon.accepts(game, attachment)) {
                    continue;
                }
                if (!gameState.isCardInPlayActive(
                        attachment, true, true, true, false, false, true, true, false)) {
                    continue;
                }
                bonus += Filters.lightsaber.accepts(game, attachment) ? 5f : 3f;
                armed = true;
            }
        }
        return new BattleWeaponProfile(bonus, armed);
    }
}
