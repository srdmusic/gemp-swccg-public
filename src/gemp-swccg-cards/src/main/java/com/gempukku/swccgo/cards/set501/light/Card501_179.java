package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: Patience!
 */
public class Card501_179 extends AbstractEpicEventDeployable {
    public Card501_179() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Patience!", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on table if your [Dag] objective on table. Stack up to 6 Jedi Tests face up from your Reserve Deck here. Only Luke may be an apprentice.\n  I Won't Fail You: You may play a face-up Jedi Test from here as if from hand.\n I Saw a City in the Clouds: Once per turn, may ▼ a Cloud City site (or Bespin).\n I've Got to Go to Them: Unless you occupy a battleground with a [CC] Rebel, once per turn, if you just lost a Force during opponent's control phase, flip the highest number Jedi Test here face down.");
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setTestingText("Patience!");
        hideFromDeckBuilder();
    }
}
