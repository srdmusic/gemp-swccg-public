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
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, Title.How_Liberty_Dies, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on Galactic Senate. Your Political Effects are canceled. Twice per turn, may target your agenda here: Justice: During battle, subtract 1 from a just drawn weapon destiny. Order: During any move phase, peek at the top 2 cards of any Reserve Deck and replace in any order. Peace: Subtract 1 from attrition against you. Taxation: Place an [Episode I] character from hand on Used Pile; the next [Episode I] character you deploy this turn is cumulatively deploy -1.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.VIRTUAL_SET_25);
        setTestingText("How Liberty Dies");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Galactic_Senate;
    }

}
