package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.TakeCardFromVoidIntoHandEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;


/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: You Will Go To The Dagobah System (V)
 */
public class Card501_180 extends AbstractLostOrStartingInterrupt {
    public Card501_180() {
        super(Side.LIGHT, 4, "You Will Go To The Dagobah System", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'There you will learn from Yoda, the Jedi Master who instructed me.'");
        setGameText("LOST: [Upload] [Dagobah] Luke or retrieve Anakin's Lightsaber into hand. [Immune to Sense.] STARTING: If your [Dagobah] objective on table, deploy two Effects (except Wokling) that deploy for free and are always immune to Alter. Place Interrupt in hand.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("You Will Go To The Dagobah System (V)");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, SwccgGame game, final PhysicalCard self) {

        // Allow response(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Filters.icon(Icon.DAGOBAH), Filters.Objective))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            final Filter dagobahLuke = Filters.and(Filters.icon(Icon.DAGOBAH), Filters.Luke);
            final Filter effectFilter = Filters.and(Filters.Effect, Filters.not(Filters.Wokling), Filters.deploysForFree, Filters.always_immune_to_Alter);
            action.setText("Deploy two Effects that deploy for free and are always immune to Alter");
    
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, effectFilter, 2, 2, true, false));
                            action.appendEffect(
                                    new TakeCardFromVoidIntoHandEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }
        return null;
    }
}