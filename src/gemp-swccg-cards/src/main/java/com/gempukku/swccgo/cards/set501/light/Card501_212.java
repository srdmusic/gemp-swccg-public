package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
  * Title: Launching The Assault (V)
 */

public class Card501_212 extends AbstractNormalEffect {
    public Card501_212() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Launching_The_Assault, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("By recruiting the Mon Calamari, a race of master shipwrights, the Rebel starfleet gained capital starships rivaling the dreaded Imperial Star Destroyers.");
        setGameText("Deploy on table. Once per game, may [upload] Home One. May [download] Sullust. Home One is deploy = 8. Once per game at the start of your deploy phase, if Home One at Sullust, each of your starships there may make a regular move. [Immune to Alter.]");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_26);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("Launching The Assault (V)");
        hideFromDeckBuilder();
    }
    
}
