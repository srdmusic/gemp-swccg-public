package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractStartingInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ModifyNumCardsDrawnInStartingHandEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardsIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;


/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Starting
 * Title: The Sith Will Rule The Galaxy
 */
public class Card501_045 extends AbstractStartingInterrupt {
    public Card501_045() {
        super(Side.DARK, 0, "The Sith Will Rule The Galaxy", Uniqueness.UNIQUE);
        setGameText("Take Maul's Chambers, Sith Throne, Throne Room, or Vader's Castle, a unique (•) weapon, and two Effects that are always [Immune to Alter.] into hand from Reserve Deck. When you draw your starting hand, draw only six more cards. Place Interrupt in Lost Pile.");
        addIcons(Icon.VIRTUAL_SET_16);
        setTestingText("The Sith Will Rule The Galaxy");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, SwccgGame game, final PhysicalCard self) {
        final PlayInterruptAction action = new PlayInterruptAction(game, self);
        action.setText("Take a location, weapon, and two Effects into hand from Reserve Deck");
        // Allow response(s)
        action.allowResponses(
                new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        // Perform result(s)
                        Filter locationFilter = Filters.or(Filters.title(Title.Dathomir_Mauls_Chambers), Filters.title(Title.Exegol_Sith_Throne), Filters.title(Title.Throne_Room), Filters.title(Title.Vaders_Castle));
                        action.appendEffect(
                                new TakeCardIntoHandFromReserveDeckEffect(action, playerId, locationFilter, false));
                        action.appendEffect(
                                new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Filters.unique, Filters.weapon), false));
                        action.appendEffect(
                                new TakeCardsIntoHandFromReserveDeckEffect(action, playerId, 2, 2, Filters.and(Filters.Effect, Filters.immune_to_Alter), false));
                        action.appendEffect(
                                new ModifyNumCardsDrawnInStartingHandEffect(action, playerId, 6));
                        action.appendEffect(
                                new PutCardFromVoidInLostPileEffect(action, playerId, self));
                    }
                }
        );
        return action;
    }
}