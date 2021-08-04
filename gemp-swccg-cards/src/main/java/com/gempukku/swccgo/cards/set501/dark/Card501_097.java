package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.common.*;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 16
 * Type: Starship
 * Subtype: Capital
 * Title: Avenger (V)
 */
public class Card501_097 extends AbstractCapitalStarship {
    public Card501_097() {
        super(Side.DARK, 1, 8, 6, 7, null, 3, 9, Title.Avenger, Uniqueness.UNIQUE);
        setLore("Key starship used to subjugate Outer Rim worlds. Reassigned to Death Squadron under the command of Captain Needa. Communications ship at the Battle of Endor.");
        setGameText("May add 4 pilots and 4 TIEs. Permanent pilot provides ability of 2. Once per game, may deploy a captain (or Imperial with armor) aboard (deploy -2) from Reserve Deck; reshuffle.");
        addIcons(Icon.DAGOBAH, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_16);
        addModelType(ModelType.IMPERIAL_CLASS_STAR_DESTROYER);
        addKeywords(Keyword.DEATH_SQUADRON);
        setPilotCapacity(4);
        setTIECapacity(4);
        setVirtualSuffix(true);
        setTestingText("Avenger (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }
}
