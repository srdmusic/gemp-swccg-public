package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

public class Card501_194 extends AbstractRebel {
    public Card501_194() {
        super(Side.LIGHT, 1, 4, 4, 4, 6, Title.Boushh, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leia obtained the armor of a notorious mercenary to sneak onto Coruscant. She later assumed the same role to spy on Jabba. Fearless and inventive. Jabba's kind of scum.");
        setGameText("At Jabba's Palace sites where you have no Jedi, you may Force drain regardless of your [Premium] objective restrictions. Characters may not be excluded from battle here. While alone with frozen Han, Leia's ability = 0 and her game text may not be canceled.");
        setArmor(5);
        addIcons(Icon.PREMIUM, Icon.JABBAS_PALACE, Icon.PILOT, Icon.WARRIOR);
        addKeywords(Keyword.SPY, Keyword.FEMALE);
        addPersona(Persona.LEIA);
        setSpecies(Species.ALDERAANIAN);
        setVirtualSuffix(true);
        setTestingText("Boushh (V)");
        hideFromDeckBuilder();
    }
}
