package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 20
 * Type: Effect
 * Title: Lightsaber Proficiency (V)
 */
public class Card501_050 extends AbstractNormalEffect {
    public Card501_050() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, Title.Lightsaber_Proficiency, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("A Jedi learns not only to wield a lightsaber, but how to channel the Force to increase skill and control the lightsaber's damage.");
        setGameText("Deploy on any character with ability > 2 and a lightsaber. That character is power +3 in battles or may add 1 to Force drain where present. Effect is lost if character loses lightsaber.");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcon(Icon.VIRTUAL_SET_20);
        setTestingText("Lightsaber Proficiency (V)");
        hideFromDeckBuilder();
    }
}