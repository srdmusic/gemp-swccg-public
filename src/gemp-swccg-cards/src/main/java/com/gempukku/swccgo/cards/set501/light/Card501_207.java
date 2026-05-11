package com.gempukku.swccgo.cards.set501.light;

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
 * Title: Do, Or Do Not & Wise Advice (V)
 */
public class Card501_207 extends AbstractNormalEffect {
    public Card501_207() {
        super(Side.LIGHT, 1, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Do, Or Do Not & Wise Advice", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        addComboCardTitles(Title.Do_Or_Do_Not, Title.Wise_Advice);
        setGameText("Deploy on table. Sense and Alter are Lost Interrupts. When any player makes a destiny draw for Sense or Alter, and that destiny is successful, that player loses 2 Force (may not be reduced). Yoda and padawans deploy -1 (except to Lothal) and are immune to Imperial Barrier. [Dag] Luke and Grogu are Padawans. If your Padawan about to leave table, may lose 1 Force to place your cards deployed on them in your Used Pile. (Immune to Alter.)");
        addIcons(Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("Do, Or Do Not & Wise Advice (V)");
        hideFromDeckBuilder();
    }
}
