package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Demotion (V)
 */
public class Card501_172 extends AbstractNormalEffect {
    public Card501_172() {
        super(Side.LIGHT, 3, PlayCardZoneOption.ATTACHED, Title.Demotion, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Repercussions for failure are severe in the Imperial military. Many officers prefer demotion to 'alternative' punishment from Darth Vader.");
        setGameText("Deploy on an alien leader, Imperial, or senator (except Bib, Jabba, Sidious, Thrawn, Vader, or Xizor). Character's game text is canceled. Opponent's total battle destiny is -1 here (-2 if Kallus or Vader here).");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcon(Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Demotion (V)");
    }
}