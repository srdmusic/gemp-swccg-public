package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Salacious Crumb (V)
 */
public class Card501_017 extends AbstractAlien {
    public Card501_017() {
        super(Side.DARK, 3, 2, 1, 1, 3, Title.Salacious_Crumb, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Male Kowakian. Prankster. Humiliates others for Jabba's amusement. His life depends on making Jabba laugh at least once per day.");
        setGameText("When deployed, may draw top card of Reserve Deck or place a droid present in owner's Used Pile ('AH-hahahaha!'). Game text of your alien leaders here may not be canceled. If at a converted Jabba's Palace site, may raise yours to the top.");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setSpecies(Species.KOWAKIAN);
        setVirtualSuffix(true);
    }    
}
