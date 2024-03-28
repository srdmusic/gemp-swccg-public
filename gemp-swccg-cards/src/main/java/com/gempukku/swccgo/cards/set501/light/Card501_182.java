package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Quantum Storm
 */
public class Card501_182 extends AbstractCapitalStarship {
    public Card501_182() {
        super(Side.LIGHT, 3, 3, 2, 4, null, 4, 5, "Quantum Storm", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 2 pilots and 6 passengers. Permanent pilot provides ability of 2. Rebels on Hoth fire weapons for free. During battle, if a [H] Cannon at a related site, may cancel a character's game text here.");
        addIcons(Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK);
        addModelType(ModelType.TRANSPORT);
        setPilotCapacity(2);
        setPassengerCapacity(6);
        setTestingText("Quantum Storm");
    }
    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }

}
