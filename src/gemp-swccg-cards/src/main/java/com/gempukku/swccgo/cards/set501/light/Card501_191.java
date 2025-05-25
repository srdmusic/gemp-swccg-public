package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: How Liberty Dies
 */
public class Card501_191 extends AbstractEpicEventDeployable {
    public Card501_191() {
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, "How Liberty Des", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on Galactic Senate. Your Political Effects are canceled. Twice per turn, may target your agenda here:\n" +
                    "justice: during battle, subtract 1 from a just drawn weapon destiny.\n" +
                    "order: during any move phase, peek at the top 2 cards of any Reserve Deck and replace in any order;\n" +
                    "peace: during battle, subtract 1 from attrition against you;\n" +
                    "taxation: place a card from hand on Used Pile to make the next [E1] character you deploy this turn deploy -1.");
        addIcons(Icon.CORUSCANT, Icon.VIRTUAL_SET_25);
        setTestingText("How Liberty Dies");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Galactic_Senate;
    }

}
