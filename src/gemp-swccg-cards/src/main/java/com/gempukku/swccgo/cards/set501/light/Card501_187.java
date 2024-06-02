package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.conditions.DuringPlayersTurnNumberCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToDeployCardForFreeExceptByOwnGametextModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: Goldenrod (V)
 */
public class Card501_187 extends AbstractDefensiveShield {
    public Card501_187() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Goldenrod, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setGameText("Plays on table. They Must Never Again Leave This City is suspended during opponent's first turn. For opponent to deploy a character, starship, or vehicle for free (unless that card is unique (•) and using its own game text), opponent must first use 2 Force.");
        addIcons(Icon.REFLECTIONS_III, Icon.VIRTUAL_DEFENSIVE_SHIELD);
        setTestingText("Goldenrod (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ExtraForceCostToDeployCardForFreeExceptByOwnGametextModifier(self, Filters.and(Filters.opponents(self), Filters.or(Filters.character, Filters.starship, Filters.vehicle), Filters.not(Filters.unique)), 2));
        modifiers.add(new SuspendsCardModifier(self, Filters.title(Title.They_Must_Never_Again_Leave_This_City), new DuringPlayersTurnNumberCondition(game.getOpponent(self.getOwner()), 1)));
        return modifiers;
    }
}
