package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Epic Event
 * Title: Communing
 */
public class Card501_037 extends AbstractEpicEventDeployable {
    public Card501_037() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Communing, Uniqueness.UNIQUE);
        setGameText("Deploy on table.\n" +
                "One With The Force: If a Jedi was just lost (or placed out of play) from table, may stack that card here.\n" +
                "The Living Force: Jedi stacked here are 'communing' and are considered out of play. Your total Force generation is +1 for each Jedi stacked here.\n" +
                "The Cosmic Force: Once per turn, if a Jedi is 'communing,' may use 1 Force to peek at the top card of Reserve Deck, Force Pile, and/or Used Pile; return one card to each deck or pile.");
        addIcons(Icon.VIRTUAL_SET_15, Icon.EPISODE_I);
        setTestingText("Communing");
        hideFromDeckBuilder();
    }
}