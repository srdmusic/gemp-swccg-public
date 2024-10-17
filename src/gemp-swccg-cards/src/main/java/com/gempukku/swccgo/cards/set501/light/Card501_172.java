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
import com.gempukku.swccgo.filters.Filters;

/*
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Han Solo (V)
 */

public class Card501_172 extends AbstractRebel {
    public Card501_172() {
        super(Side.LIGHT, 1, 3, 3, 3, 6, Title.Han_Solo, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Smuggler, gambler and 'freelance law-bender.' Crafty Corellian pirate. Rebel hero. Owns Millennium Falcon. Co-pilot Chewbacca promised him 'life-debt.' Has bounty on head.");
        setGameText("Adds 3 to power of anything he pilots. Draws one battle destiny if unable to otherwise. Adds one battle destiny with Chewie. While piloting Falcon, adds 2 to maneuver and, once per game, may choose: Cancel and redraw any destiny, or re-circulate.");
        addIcons(Icon.A_NEW_HOPE, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_24);
        addKeywords(Keyword.SMUGGLER, Keyword.GAMBLER, Keyword.PIRATE);
        addPersona(Persona.HAN);
        setSpecies(Species.CORELLIAN);
        setMatchingStarshipFilter(Filters.Falcon);
        setVirtualSuffix(true);
        setTestingText("Han Solo (V)");
        hideFromDeckBuilder();
    }
}
