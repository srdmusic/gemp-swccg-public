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
 * Set: Set 19
 * Type: Weapon
 * Subtype: Character
 * Title: Ezra's Blaster Lightsaber
 */
public class Card501_076 extends AbstractCharacterWeapon {
    public Card501_076() {
        super(Side.LIGHT, 1, "Ezra's Blaster Lightsaber", Uniqueness.UNIQUE);
        setLore("");
        setGameText(" Deploy on Ezra. Once per turn, may return to hand to cancel and redraw a destiny targeting a Rebel here." +
                "May target a character for free. Draw destiny. " +
                "If destiny +3 > defense value, target hit, its forfeit = 0 and, if target’s ability > 4, " +
                "Ezra is power +2 until end of turn.");
        addPersona(Persona.EZRAS_BLADER_LIGHTSABER);
        addIcons(Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.BLASTER, Keyword.LIGHTSABER);
        setMatchingCharacterFilter(Filters.Ezra);
        setTestingText("Ezra's Blaster Lightsaber");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.Ezra);
    }

    @Override
    protected Filter getGameTextValidToUseWeaponFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.Ezra;
    }


    @Override
    protected List<FireWeaponAction> getGameTextFireWeaponActions(String playerId, final SwccgGame game, final PhysicalCard self, boolean forFree, int extraForceRequired, PhysicalCard sourceCard, boolean repeatedFiring, Filter targetedAsCharacter, Float defenseValueAsCharacter, Filter fireAtTargetFilter, boolean ignorePerAttackOrBattleLimit) {
        FireWeaponActionBuilder actionBuilder = FireWeaponActionBuilder.startBuildPrep(playerId, game, sourceCard, self, forFree, extraForceRequired, repeatedFiring, targetedAsCharacter, defenseValueAsCharacter, fireAtTargetFilter, ignorePerAttackOrBattleLimit)
                .target(Filters.or(Filters.character, targetedAsCharacter), TargetingReason.TO_BE_HIT).finishBuildPrep();
        if (actionBuilder != null) {

            // Build action using common utility
            FireWeaponAction action = actionBuilder.buildFireWeaponEzrasBlasterLightsaberAction();
            return Collections.singletonList(action);
        }
        return null;
    }
}
