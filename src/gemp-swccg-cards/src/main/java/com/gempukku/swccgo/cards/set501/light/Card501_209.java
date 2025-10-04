package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.conditions.WithCondition;
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
import com.gempukku.swccgo.logic.modifiers.AddsBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Obi-Wan Kenobi, Jedi In Exile
 */

public class Card501_209 extends AbstractRebel {
    public Card501_209() {
        super(Side.LIGHT, 1, 8, 6, 6, 8, "Obi-Wan Kenobi, Jedi In Exile", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jedi survivor.");
        setGameText("Adds one battle destiny with Maul, Sidious, or Vader. Opponent's characters here are power and forfeit -1. Once per game, may [upload] (or retrieve into hand) Glancing Blow or Help Me Obi-Wan Kenobi. Immune to You Are Beaten and attrition < 6.");
        addKeyword(Keyword.JEDI_SURVIVOR);
        addPersona(Persona.OBIWAN);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_26);
        setTestingText("Obi-Wan Kenobi, Jedi In Exile");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsBattleDestinyModifier(self, new WithCondition(self, Filters.or(Filters.Maul, Filters.Sidious, Filters.Vader)), 1));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.opponents(self), Filters.character, Filters.here(self)), -1));
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.opponents(self), Filters.character, Filters.here(self)), -1));
        return modifiers;
    }
    
}
