package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventPlayable;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Set 15
 * Type: Epic Event
 * Title: Emperor's Orders
 */
public class Card501_029 extends AbstractEpicEventPlayable {
    public Card501_029() {
        super(Side.DARK, "Emperor's Orders");
        setGameText("The Alliance Will Die...: Deploy on Executor if you have no objective. Flagship Operations may deploy regardless of deployment restrictions. You cards may not add more than 2 to the power, destiny and forfeit of a squadron." +
                "...As Will Your Friends: Where you have a TIE with a capital ship, your force drains = 3. Your TIE assault squadrons may deploy for 3 force (without replacement). If Executor lost, this card lost and you lose 3 force." +
                "'I'm Hit!:' During battle, opponent may place their A-wing with Executor in Lost Pile to cancel Executor’s immunity to attrition");
        addIcons(Icon.VIRTUAL_SET_15);
        setTestingText("Emperor's Orders");
        hideFromDeckBuilder();
    }
}
