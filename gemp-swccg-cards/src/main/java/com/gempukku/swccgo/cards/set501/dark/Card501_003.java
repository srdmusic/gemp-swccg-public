package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;

/**
 * Set: Set 17
 * Type: Device
 * Title: Observation Holocam (V)
 */
public class Card501_003 extends AbstractDevice {
    public Card501_003() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "Observation Holocam");
        setVirtualSuffix(true);
        setLore("Remote surveillance viewers with droid controllers supplement security. Can activate alarms and automated weapons when needed, bringing help to endangered locations.");
        setGameText("Deploy on your interior site. In a battle opponent initiates here, may place this card in Lost Pile to exchange a card in your hand with the top card of your Reserve Deck. May be targeted by weapons like a character (defense value = 2). If 'hit', device lost.");
        addIcons(Icon.VIRTUAL_SET_17);
        setTestingText("Observation Holocam (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.site;
    }
}