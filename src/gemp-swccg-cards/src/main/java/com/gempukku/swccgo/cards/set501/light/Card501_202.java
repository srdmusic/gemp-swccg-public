package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
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

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: Fallen Order
 */

public class Card501_202 extends AbstractEpicEventDeployable {
    public Card501_202() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Fallen_Order, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If The Hidden Path on table, deploy on table and stack three Jedi Survivors from Reserve Deck here. During your deploy phase, may deploy a Jedi survivor from here as if from hand. If your Jedi Survivor is about to be lost from table, stack it here. The Light Will Fade: While The Hidden Path on table, your Jedi Survivors may not be targeted by weapons and their game text is canceled. But It Is Never Forgotten: During your draw phase, if Gather Allies And Train on table and no Jedi Survivors are stacked here, may retrieve 1 Force.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Fallen Order");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.The_Hidden_Path);
    }

}
