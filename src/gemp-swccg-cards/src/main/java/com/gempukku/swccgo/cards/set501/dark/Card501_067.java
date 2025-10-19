package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.MayNotPlayModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Effect
  * Title: I'm Sorry (V)
 */

public class Card501_067 extends AbstractNormalEffect {
    public Card501_067() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Im_Sorry, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'I'm sorry, too.'");
        setGameText("If your [Cloud City] objective on table, deploy on table. You may not play Imperial Barrier. Once per turn, may [download] an interior Cloud City site (or Lando to Dining Room). Your unique (•) characters of ability < 4 are forfeit +2 (limit +2). [Immune to Alter.]");
        addIcons(Icon.TATOOINE, Icon.CLOUD_CITY, Icon.VIRTUAL_SET_26);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("I'm Sorry (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(playerId), Icon.CLOUD_CITY, Filters.Objective));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotPlayModifier(self, Filters.Imperial_Barrier, playerId));
        return modifiers;
    }
    
}
