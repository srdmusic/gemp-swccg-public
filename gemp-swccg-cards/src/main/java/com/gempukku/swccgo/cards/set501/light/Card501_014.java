package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterDevice;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;


/**
 * Set: Set 16
 * Type: Device
 * Title: Jedi Holocron
 */
public class Card501_014 extends AbstractCharacterDevice {
    public Card501_014() {
        super(Side.LIGHT, 2, "Jedi Holocron", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on your character of ability > 4. While present: adds 1 to training destiny draws and Force drains here; the first Force lost to a Force drain here is stacked here face down; opponent’s ability required to draw battle destiny here is +1 for each card stacked here.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setTestingText("Jedi Holocron");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.character, Filters.abilityMoreThan(4));
    }

    @Override
    protected Filter getGameTextValidToUseDeviceFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.and(Filters.character, Filters.abilityMoreThan(4));
    }
}