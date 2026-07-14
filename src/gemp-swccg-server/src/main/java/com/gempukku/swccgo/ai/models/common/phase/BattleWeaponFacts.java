package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.PhysicalCard;

import java.util.Set;

/** Separate ownership, hit capability, and zero-forfeit facts for permanent weapons. */
public record BattleWeaponFacts(
        boolean ownsPermanentWeapon,
        boolean canHitCharacter,
        boolean canSetForfeitToZero) {

    private static final Set<String> NON_HIT_PERMANENT_WEAPONS = Set.of(
            "109_6", "200_71", "601_177", "208_2", "211_7", "213_7");

    public static BattleWeaponFacts from(PhysicalCard character) {
        if (character == null || character.getBlueprint() == null) {
            return new BattleWeaponFacts(false, false, false);
        }
        boolean owns = character.getBlueprint().hasIcon(Icon.PERMANENT_WEAPON);
        String blueprintId = character.getBlueprintId(true);
        boolean canHit = owns && blueprintId != null
                && !NON_HIT_PERMANENT_WEAPONS.contains(blueprintId);
        return new BattleWeaponFacts(owns, canHit, canHit);
    }
}
