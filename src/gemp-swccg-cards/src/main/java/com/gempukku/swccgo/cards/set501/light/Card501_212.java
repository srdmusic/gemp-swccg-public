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
 * Title: Return Of The Jedi
 */
public class Card501_212 extends AbstractNormalEffect {
    public Card501_212() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Return Of The Jedi", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'I know there is good in you. The Emperor hasn't driven it from you fully.'");
        setGameText("Deploy on table if [Death Star II] Anakin ‘communing.’ Opponent must lose 1 Force each time they lose a battle (or, once per battle, if they just lost a card with ability). At the end of every turn, if you occupy more battlegrounds than opponent, opponent loses 1 Force. (Immune to Alter.)");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Return Of The Jedi");
        hideFromDeckBuilder();
    }
}
