package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.common.*;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 17
 * Type: Starship
 * Subtype: Capital
 * Title: Tyrant (V)
 */
public class Card501_025 extends AbstractCapitalStarship {
    public Card501_025() {
        super(Side.DARK, 1, 7, 8, 6, null, 3, 9, Title.Tyrant, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Assigned to Admiral Ozzel's Death Squadron. Attempted to capture Rebel starships fleeing the Hoth system.");
        setGameText("May add 6 pilots, 8 passengers, 2 vehicles, and 4 TIEs. Permanent pilot provides ability of 2. If Tyrant just moved to a system, may relocate an AT-AT aboard to a related site.");
        addIcons(Icon.HOTH, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_17);
        addModelType(ModelType.IMPERIAL_CLASS_STAR_DESTROYER);
        addKeywords(Keyword.DEATH_SQUADRON);
        setPilotCapacity(6);
        setPassengerCapacity(8);
        setVehicleCapacity(2);
        setTIECapacity(4);
        setTestingText("Tyrant (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }
}
