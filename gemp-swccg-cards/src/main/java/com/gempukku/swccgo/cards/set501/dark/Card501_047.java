package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.MovesForFreeModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Character
 * Subtype: Imperial
 * Title: Corporal Avarik (V)
 */
public class Card501_047 extends AbstractImperial {
    public Card501_047() {
        super(Side.DARK, 3, 2, 2, 1, 4, Title.Avarik, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Frequently engaged in brawls at the local enlisted clubs on homeworld of Corulag. Stormtrooper assigned to biker scout unit. Monitors Yuzzum activity.");
        setGameText("Adds 3 to power of any speeder bike he pilots. While piloting a speeder bike, it moves for free and Avarik draws one battle destiny if unable to otherwise.");
        addIcons(Icon.ENDOR, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.BIKER_SCOUT);
        setTestingText("Corporal Avarik (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3, Filters.speeder_bike));
        modifiers.add(new MovesForFreeModifier(self, Filters.and(Filters.speeder_bike, Filters.hasPiloting(self))));
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, new PilotingCondition(self, Filters.speeder_bike), 1));
        return modifiers;
    }
}
