package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AgendaModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Republic
 * Title: Yarua (V)
 */
public class Card501_083 extends AbstractRepublic {
    public Card501_083() {
        super(Side.LIGHT, 2, 4, 5, 2, 4, "Yarua", Uniqueness.UNIQUE);
        setPolitics(2);
        setLore("Kashyyyk's senior Wookiee senator. Believes that a thorough taxation plan will assist funding of other worthwhile Republic programs. Despises the corruption around him.");
        setGameText("If drawn for destiny, each of your Wookiees is power +1 for remainder of turn. Agenda: taxation. At sites where you have a Wookiee present and your total power > 10, your Force drains are +1.");
        setSpecies(Species.WOOKIEE);
        addKeywords(Keyword.SENATOR);
        addIcons(Icon.WARRIOR, Icon.EPISODE_I, Icon.CORUSCANT, Icon.VIRTUAL_SET_16);
        setVirtualSuffix(true);
        setTestingText("Yarua (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AgendaModifier(self, Agenda.TAXATION));
        return modifiers;
    }
}
