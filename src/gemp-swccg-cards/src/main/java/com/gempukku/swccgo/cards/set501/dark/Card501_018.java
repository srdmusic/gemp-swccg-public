package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Labria (V)
 */
public class Card501_018 extends AbstractAlien {
    public Card501_018() {
        super(Side.DARK, 3, 2, 2, 2, 4, "Labria", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Information broker. Spy. Devaronian males instinctively have 'wanderlust.' Frustrated that he must be reclusive due to shady past. Suffers from prejudice due to devilish appearance.");
        setGameText("During opponent’s draw phase, if at a battleground and opponent did not move a character to or from this location this turn, they* lose 1 Force. If opponent just searched their Reserve Deck, after replacing, you may peek at the top card of that deck.");
        addIcon(Icon.VIRTUAL_SET_24);
        addKeywords(Keyword.INFORMATION_BROKER, Keyword.SPY);
        setSpecies(Species.DEVARONIAN);
        setVirtualSuffix(true);
        setTestingText("Labria (V)");
        hideFromDeckBuilder();
    }
}
