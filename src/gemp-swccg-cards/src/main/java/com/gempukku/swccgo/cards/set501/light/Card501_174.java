package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.OutOfPlayCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToPowerModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Resistance
 * Title: Finn, Resistance Leader
 */
public class Card501_174 extends AbstractResistance {
    public Card501_174() {
        super(Side.LIGHT, 1, 4, 4, 4, 6, "Finn, Resistance Hero", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader.");
        setGameText("During battle, if Luke out of play (or Rose or Jannah here), adds one destiny to total power. While alone, opponent may not cancel or reduce Force drains at same [E7] battleground. Jedi Lightsaber may deploy on Finn. Immune to attrition < 4.");
        addPersona(Persona.FINN);
        addIcons(Icon.EPISODE_VII, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.LEADER);
        setTestingText("Finn, Resistance Leader");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition lukeOutOfPlay = new OutOfPlayCondition(self, Filters.Luke);
        Condition roseOrJannahHere = new HereCondition(self, Filters.or(Filters.Rose, Filters.Jannah));
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsDestinyToPowerModifier(self, new OrCondition(lukeOutOfPlay, roseOrJannahHere), 1));
        return modifiers;
    }
}
