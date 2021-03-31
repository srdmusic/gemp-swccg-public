package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ExchangeCardInHandWithTopCardOfReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 15
 * Type: Device
 * Title: Observation Holocam (V)
 */
public class Card501_009 extends AbstractDevice {
    public Card501_009() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "Observation Holocam");
        setLore("Remote surveillance viewers with droid controllers supplement security. Can activate alarms and automated weapons when needed, bringing help to endangered locations.");
        setGameText("Deploy on a site. While you occupy this site, during battle here or at a related location, may place this device in Lost Pile to exchange a card from hand with the top card of Reserve Deck.");
        addIcon(Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        setTestingText("Observation Holocam (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.site;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (GameConditions.occupies(game, playerId, Filters.here(self))
                && GameConditions.isDuringBattleAt(game, Filters.sameOrRelatedLocation(self))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Exchange card in hand with top of Reserve Deck");
            // Perform result(s)
            action.appendCost(new LoseCardFromTableEffect(action, self));
            action.appendEffect(
                    new ExchangeCardInHandWithTopCardOfReserveDeckEffect(action, playerId));
            return Collections.singletonList(action);
        }
        return null;
    }
}