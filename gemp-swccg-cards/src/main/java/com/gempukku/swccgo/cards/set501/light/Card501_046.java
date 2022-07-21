package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterDevice;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 20
 * Type: Device
 * Title: Mercenary Armor (V)
 */
public class Card501_046 extends AbstractCharacterDevice {
    public Card501_046() {
        super(Side.LIGHT, 3, Title.Mercenary_Armor, Uniqueness.RESTRICTED_2);
        setLore("Worn by hired guns throughout the galaxy. Often used by Rebels when infiltrating underworld organizations. Leia wore Boushh's armor when she infiltrated Black Sun.");
        setGameText("Deploy on your Rebel or Alien. Imperial Barrier is canceled. In battles here, opponent may not cancel destinies. If on Leia or Chewie, they are power and defense value +2, and may use 1 Force to return this card to hand.\n");
        addIcons(Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        setTestingText("Mercenary Armor (V)");
        hideFromDeckBuilder();
    }
}
