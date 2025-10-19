package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlienImperial;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien/Imperial
 * Title: Mara Jade, The Emperor's Hand (V)
 */

public class Card501_070 extends AbstractAlienImperial {
    public Card501_070() {
        super(Side.DARK, 1, 5, 4, 5, 7, Title.Mara_Jade_The_Emperors_Hand, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Spy. Ordered to kill Luke Skywalker. Assumed the identity of a dancer named 'Arica' in order to sneak into Jabba's palace.");
        setGameText("Deploys +2 to non-battleground locations. May [download] Mara Jade's Lightsaber here. While Revenge Of The Sith on table, gains [Sith]. If you just won a battle here, may retrieve a card with 'Mara' in game text. Immune to attrition < 4 (< 5 if alone or no Jedi here, < 6 if both).");
        addPersona(Persona.MARA_JADE);
        addIcons(Icon.PREMIUM, Icon.PILOT, Icon.WARRIOR, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.SPY, Keyword.FEMALE);
        setVirtualSuffix(true);
        setTestingText("Mara Jade, The Emperor's Hand (V)");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, 2, Filters.non_battleground_location));
        return modifiers;
    }
}
