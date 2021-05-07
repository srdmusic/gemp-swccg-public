package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Effect
 * Title: Cell 2187 (V)
 */
public class Card501_043 extends AbstractNormalEffect {
    public Card501_043() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Cell_2187, Uniqueness.UNIQUE);
        setLore("");
        setGameText("If Leia imprisoned, deploy on table. [Set 8] Luke is a spy and stormtrooper. Han, Leia, and Luke are immune to Nevar Yalnal and Put All Sections On Alert!. Once per turn, may deploy a Death Star site from Reserve Deck; reshuffle. Immune to Alter and This Is Some Rescue!");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        addImmuneToCardTitle(Title.Alter);
        addImmuneToCardTitle(Title.This_Is_Some_Rescue);
        setTestingText("Cell 2187 (V)");
        hideFromDeckBuilder();
    }
}