package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.choose.DrawCardsIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeReducedModifier;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.PutCardsFromHandOnForcePileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.SendMessageEffect;
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
        setGameText("If your stormtroopers control three battleground and/or Rebel Base locations, choose: Draw three cards from Force Pile, then place two cards from hand on Force Pile. OR Once per game, your Force drains at those locations may not be canceled or reduced this turn.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Endless Legions");
    }
    
    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.controlsWith(game, self, playerId, 3, Filters.battleground, Filters.stormtrooper)) {

            GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
            // Check more condition(s) for action 1
            if (GameConditions.hasForcePile(game, playerId)
                    && GameConditions.hasHand(game, playerId)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);

                action.setText("Draw three cards from Force Pile");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                action.appendEffect(new DrawCardsIntoHandFromForcePileEffect(action, playerId, 3) {
                                    @Override
                                    protected void cardsDrawnIntoHand(Collection<PhysicalCard> cards) {
                                        if (cards.size()>=3) {
                                            action.appendEffect(new PutCardsFromHandOnForcePileEffect(action, playerId, 2, 2));
                                        }
                                        else {
                                            action.appendEffect(new SendMessageEffect(action, playerId + " failed to draw three cards."));
                                        }
                                    }
                                });
                            }
                        }
                );
                actions.add(action);
            }

            gameTextActionId = GameTextActionId.ENDLESS_LEGIONS__PROTECT_FORCE_DRAINS;
            // Check more condition(s) for action 2
            if (GameConditions.isOncePerGame(game, self, gameTextActionId)) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);

                Filter locationsWithYourStormtrooper = Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.stormtrooper));

                action.setText("Protect Force drains");
                action.setActionMsg("Protect Force drains where they have a stormtrooper from being canceled or reduced this turn");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerGameEffect(action));
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action,
                                                new ForceDrainsMayNotBeCanceledModifier(self, locationsWithYourStormtrooper, playerId), "Force drains may not be canceled")
                                );
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action,
                                                new ForceDrainsMayNotBeReducedModifier(self, locationsWithYourStormtrooper, playerId), "Force drains may not be reduced")
                                );
                            }
                        }
                );
                actions.add(action);
            }
        }
        return actions;
    }
}
