package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Interrupt
 * Subtype: Used
 * Title: Out Of Commission (V)
 */
public class Card501_036 extends AbstractUsedInterrupt {
    public Card501_036() {
        super(Side.DARK, 4, Title.Probe_Telemetry, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Probe droids use electromagnetic, seismic, acoustic, olfactory and optical sensors. They report their findings using an omnisignal unicode.");
        setGameText("If Systems Will Slip Through Your Fingers on table, may reveal from hand and place face down under a system to 'probe' there. Take a probe droid into hand from Reserve Deck; reshuffle. OR Cancel Alternatives To Fighting or It Can Wait. OR Subtract 3 from an attempt to 'liberate' a system.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("Probe Telemetry (V)");
        hideFromDeckBuilder();
    }
}
