package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Peli Motto
 */

public class Card501_211 extends AbstractAlien {
    public Card501_211() {
        super(Side.LIGHT, 2, 2, 1, 3, 4, "Peli Motto", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Female mechanic and scavenger.");
        setGameText("While at Starship Graveyard, a Tatooine site, or a docking bay, power +3 and may cancel I’ve Lost Artoo! (or Restraining Bolt at a related location). During your control phase, if at a battleground and you have won a Podrace, may retrieve 1 Force (or Grogu).");
        addKeywords(Keyword.FEMALE, Keyword.SCAVENGER);
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Peli Motto");
        hideFromDeckBuilder();
    }
    
}
