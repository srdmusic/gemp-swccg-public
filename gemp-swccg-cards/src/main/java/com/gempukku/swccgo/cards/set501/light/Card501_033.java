package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.AbstractPermanentWeapon;
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
 * Set: Set 17
 * Type: Character
 * Subtype: Alien
 * Title: Cara Dune With Heavy Blaster Rifle
 */
public class Card501_033 extends AbstractAlien {
    public Card501_033() {
        super(Side.LIGHT, 2, 4, 4, 2, 5, "Cara Dune With Heavy Blaster Rifle", Uniqueness.UNIQUE);
        setLore("Female Alderaanian. Gambler. Trooper.");
        setGameText("Permanent weapon is •Cara's Heavy Blaster Rifle (may target a character for free; draw destiny; target hit and cumulatively forfeit -3 if destiny +1 > defense value; may fire repeatedly for 2 Force each time).");
        addKeywords(Keyword.FEMALE, Keyword.GAMBLER, Keyword.TROOPER);
        setSpecies(Species.ALDERAANIAN);
        addIcons(Icon.WARRIOR, Icon.PERMANENT_WEAPON, Icon.VIRTUAL_SET_17);
        setTestingText("[Set 18] Cara Dune With Heavy Blaster Rifle");
        hideFromDeckBuilder();
    }

    @Override
    protected AbstractPermanentWeapon getGameTextPermanentWeapon() {
        AbstractPermanentWeapon permanentWeapon = new AbstractPermanentWeapon("Cara's Heavy Blaster Rifle") {
            @Override
            public List<FireWeaponAction> getGameTextFireWeaponActions(String playerId, SwccgGame game, PhysicalCard self, boolean forFree, int extraForceRequired, PhysicalCard sourceCard, boolean repeatedFiring, Filter targetedAsCharacter, Float defenseValueAsCharacter, Filter fireAtTargetFilter, boolean ignorePerAttackOrBattleLimit) {
                FireWeaponActionBuilder actionBuilder = FireWeaponActionBuilder.startBuildPrep(playerId, game, sourceCard, self, this, forFree, extraForceRequired, repeatedFiring, targetedAsCharacter, defenseValueAsCharacter, fireAtTargetFilter, ignorePerAttackOrBattleLimit)
                        .targetForFree(Filters.or(Filters.character, targetedAsCharacter, Filters.creature), TargetingReason.TO_BE_HIT).finishBuildPrep();
                if (actionBuilder != null) {

                    // Build action using common utility
                    FireWeaponAction action = actionBuilder.buildFireWeaponWithHitAction(1, 1, Statistic.DEFENSE_VALUE, false, -3);
                    return Collections.singletonList(action);
                }
                return null;
            }
        };
        permanentWeapon.addKeyword(Keyword.BLASTER_RIFLE);
        return permanentWeapon;
    }
}
