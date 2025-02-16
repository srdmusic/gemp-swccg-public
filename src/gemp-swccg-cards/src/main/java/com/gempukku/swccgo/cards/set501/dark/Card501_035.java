package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Bib Fortuna (V)
 */
public class Card501_035 extends AbstractAlien {
    public Card501_035() {
        super(Side.DARK, 1, 3, 3, 1, 4, Title.Bib, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Twi'lek leader and majordomo of Jabba's palace. Succeeded Jabba's last majordomo, Naroon Cuthus. Plotting to kill Jabba.");
        setGameText("While with Jabba, power +2, and unless 'hit', opponent may not target this site with I Must Be Allowed To Speak or your other characters here with blasters. If opponent just deployed a character here, you may place a card from hand on Force pile.");
        addPersona(Persona.BIB);
        addIcons(Icon.JABBAS_PALACE);
        setSpecies(Species.TWILEK);
        addKeywords(Keyword.LEADER);
        setVirtualSuffix(true);
        setTestingText("Bib Fortuna (V)");
        hideFromDeckBuilder();
    }
}
