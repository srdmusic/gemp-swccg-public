package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.FireWeaponAction;
import com.gempukku.swccgo.logic.actions.FireWeaponActionBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 15
 * Type: Weapon
 * Subtype: Character
 * Title: Uncivilized Blaster
 */
public class Card501_016 extends AbstractCharacterWeapon {
    public Card501_016() {
        super(Side.LIGHT, 3, "Uncivilized Blaster");
        setLore("");
        setGameText("Deploy on your warrior. May target a character, creature or vehicle for free. Draw destiny. If destiny + 2 > target’s defense value, target hit. If hit by Corran, Kanan or Obi-Wan, may not be used to satisfy attrition)");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_15);
        addKeywords(Keyword.BLASTER);
        setMatchingCharacterFilter(Filters.or(Filters.Corran_Horn, Filters.Kanan, Filters.ObiWan));
        setTestingText("Uncivilized Blaster");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.warrior);
    }

    @Override
    protected Filter getGameTextValidToUseWeaponFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.warrior;
    }

    @Override
    protected List<FireWeaponAction> getGameTextFireWeaponActions(String playerId, SwccgGame game, PhysicalCard self, boolean forFree, int extraForceRequired, PhysicalCard sourceCard, boolean repeatedFiring, Filter targetedAsCharacter, Float defenseValueAsCharacter, Filter fireAtTargetFilter, boolean ignorePerAttackOrBattleLimit) {
        FireWeaponActionBuilder actionBuilder = FireWeaponActionBuilder.startBuildPrep(playerId, game, sourceCard, self, forFree, extraForceRequired, repeatedFiring, targetedAsCharacter, defenseValueAsCharacter, fireAtTargetFilter, ignorePerAttackOrBattleLimit)
                .targetUsingForce(Filters.or(Filters.character, targetedAsCharacter, Filters.creature, Filters.vehicle), 0, TargetingReason.TO_BE_HIT).finishBuildPrep();
        if (actionBuilder != null) {

            // Build action using common utility
            FireWeaponAction action = actionBuilder.buildFireWeaponUncivilizedBlasterAction();
            return Collections.singletonList(action);
        }
        return null;
    }
}
