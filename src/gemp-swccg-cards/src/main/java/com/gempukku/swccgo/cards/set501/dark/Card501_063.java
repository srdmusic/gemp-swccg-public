package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.choose.DrawCardsIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.PutCardsFromHandOnForcePileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.timing.Action;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Endless Legions
 */
public class Card501_063 extends AbstractUsedInterrupt {
    public Card501_063() {
        super(Side.DARK, 3, "Endless Legions", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("If your stormtroopers control three battlegrounds and/or Rebel Base locations, choose: Draw 3 cards from Force Pile, then place 2 cards from hand on Force Pile. OR Once per game, your Force drains where you have a stormtrooper may not be canceled or reduced this turn.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Endless Legions");
    }
    
    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.hasForcePile(game, playerId)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);

            action.setText("Draw three cards from Force Pile");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            /*
                            action.appendEffect(
                                    new DrawCardsIntoHandFromForcePileEffect(action, playerId, 3));
                            action.appendEffect(
                                    new PutCardsFromHandOnForcePileEffect(action, playerId, 2, 2));
                            */
                            action.appendEffect(new DrawCardsIntoHandFromForcePileEffect(action, playerId, 3) {
                                @Override
                                protected void cardsDrawnIntoHand(Collection<PhysicalCard> cards) {
                                    if (cards.size()>=3) {
                                        action.appendEffect(new PutCardsFromHandOnForcePileEffect(action, playerId, 2, 2));
                                    }
                                }
                            });
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

}
