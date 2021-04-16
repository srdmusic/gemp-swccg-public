package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.FireWeaponAction;
import com.gempukku.swccgo.logic.actions.FireWeaponActionBuilder;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.modifiers.TotalWeaponDestinyModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Weapon
 * Subtype: Character
 * Title: Stolen Stormtrooper Blaster Rifle
 */
public class Card501_026 extends AbstractCharacterWeapon {
    public Card501_026() {
        super(Side.LIGHT, 3, "Stolen Stormtrooper Blaster Rifle");
        setLore("");
        setGameText("Deploy on your warrior (warrior is power +1 if a stormtrooper here). May target a character, creature, or vehicle for free. Draw destiny; add 1 if targeting a character, 2 if a vehicle. Target hit, and may not be used to satisfy attrition, if total destiny > defense value.");
        addIcons(Icon.VIRTUAL_SET_15);
        addKeywords(Keyword.BLASTER_RIFLE);
        setTestingText("Stolen Stormtrooper Blaster Rifle");
        hideFromDeckBuilder();
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
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, Filters.hasAttached(self), new HereCondition(self, Filters.stormtrooper), 1));
        modifiers.add(new TotalWeaponDestinyModifier(self, 1, Filters.character));
        modifiers.add(new TotalWeaponDestinyModifier(self, 2, Filters.vehicle));
        return modifiers;
    }

    @Override
    protected List<FireWeaponAction> getGameTextFireWeaponActions(String playerId, SwccgGame game, PhysicalCard self, boolean forFree, int extraForceRequired, PhysicalCard sourceCard, boolean repeatedFiring, Filter targetedAsCharacter, Float defenseValueAsCharacter, Filter fireAtTargetFilter, boolean ignorePerAttackOrBattleLimit) {
        FireWeaponActionBuilder actionBuilder = FireWeaponActionBuilder.startBuildPrep(playerId, game, sourceCard, self, forFree, extraForceRequired, repeatedFiring, targetedAsCharacter, defenseValueAsCharacter, fireAtTargetFilter, ignorePerAttackOrBattleLimit)
                .targetForFree(Filters.or(Filters.character, targetedAsCharacter, Filters.creature, Filters.vehicle), TargetingReason.TO_BE_HIT).finishBuildPrep();
        if (actionBuilder != null) {

            // Build action using common utility
            FireWeaponAction action = actionBuilder.buildBlasterRifleVAction(0);
            return Collections.singletonList(action);
        }
        return null;
    }
}
