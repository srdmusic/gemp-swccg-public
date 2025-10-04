package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master
 * Title: Quinlan Vos
 */

public class Card501_208 extends AbstractJediMaster {
    public Card501_208() {
        super(Side.LIGHT, 1, 8, 7, 7, 8, "Quinlan Vos", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jedi survivor.");
        setGameText("[Pilot] 2. Adds one battle destiny with Dooku, Grievous, or Ventress. Once per turn, may peek at the top card of any Reserve Deck or subtract 1 from a weapon destiny here. Dark Approach is a Used interrupt. Immune to Sniper and attrition < 6 (< 8 if alone).");
        addKeyword(Keyword.JEDI_SURVIVOR);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_26);
        setTestingText("Quinlan Vos");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new AddsBattleDestinyModifier(self, new WithCondition(self, Filters.or(Filters.Dooku, Filters.Grievous, Filters.Ventress)), 1));
        return modifiers;
    }

}
