package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 17
 * Type: Character
 * Subtype: Alien
 * Title: Grummgar
 */
public class Card501_098 extends AbstractAlien {
    public Card501_098() {
        super(Side.DARK, 2, 4, 6, 2, 5, "Grummgar", Uniqueness.UNIQUE);
        setLore("Dowutin.");
        setGameText("If with an information broker or creature during a battle or attack, adds one destiny to total power. Once per game, may deploy a creature (or card with 'creature' in lore or game text if it is a blaster, a rifle, or a non-[Permanent Weapon] non-weapon card) here from Reserve Deck, reshuffle.");
        setSpecies(Species.DOWUTIN);
        addIcons(Icon.WARRIOR, Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
        setTestingText("Grummgar");
        hideFromDeckBuilder();
    }
}
