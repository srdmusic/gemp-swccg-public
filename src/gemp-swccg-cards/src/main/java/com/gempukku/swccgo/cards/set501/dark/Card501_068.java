package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlienImperial;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien/Imperial
 * Title: Garindan, Imperial Spy
 */

public class Card501_068 extends AbstractAlienImperial {
    public Card501_068() {
        super(Side.DARK, 4, 2, 1, 1, 3, "Garindan, Imperial Spy", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Long-nosed, male Kubaz from Kubindi. Spy. Squealed on Obi-Wan and Luke outside Docking Bay 94. Works for Jabba the Hutt or the highest bidder. Not particularly brave.");
        setGameText("May deploy as a 'react.' Imperials move to here for free using landspeed. Unless Garindan 'hit,' may place him in Used Pile to cancel a just drawn weapon destiny targeting another character here (or to make an Undercover spy here lost).");
        addIcons(Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.SPY);
        addPersona(Persona.GARINDAN);
        setSpecies(Species.KUBAZ);
        setTestingText("Garindan, Imperial Spy");
        hideFromDeckBuilder();
    }
    
}
