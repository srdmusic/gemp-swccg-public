package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Behind Everything
 */
public class Card501_018 extends AbstractNormalEffect {
    public Card501_018() {
        super(Side.DARK, 5, PlayCardZoneOption.ATTACHED, "Behind Everything", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on a site if Revenge Of The Sith on table. While Sidous here: add one [LS] here, your total power and Force generation is +1 for each Dark Jedi Master on table and you lose no more than 1 Force to Force drains at Desert Landing Site or Vader's Castle. (Immune to Alter.)");
        addIcons(Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Behind Everything");
        hideFromDeckBuilder();
    }
}
