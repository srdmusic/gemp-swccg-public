package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 12
 * Type: Interrupt
 * Subtype: Starting
 * Title: Slip Sliding Away (V)
 */
public class Card501_122 extends AbstractUsedOrStartingInterrupt {
    public Card501_122() {
        super(Side.DARK, 3, Title.Slip_Sliding_Away, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Luke got the shaft.");
        setGameText("USED: Place this card on top of Reserve Deck. STARTING: If you deployed a site with exactly two [Dark Side] (and no other locations), deploy a mobile battleground site and up to three Effects that deploy for free and are always immune to Alter. Place Interrupt in Lost Pile.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_12);
        setTestingText("Slip Sliding Away (V) (ERRATA)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
        action.setText("Place card on Reserve Deck");

        // Allow response(s)
        action.allowResponses(
                new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        // Perform result(s)
                        if (!Filters.stacked.accepts(game, self)
                                && !action.isToBePlacedOutOfPlay()) {
                            action.appendEffect(
                                    new PlaceCardsInReserveDeckFromOffTableEffect(action, Collections.singletonList(self)));
                        } else {
                            action.appendEffect(new SendMessageEffect(action, GameUtils.getCardLink(self) + " not placed on Reserve Deck"));
                        }
                    }
                }
        );

        return Collections.singletonList(action);
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PhysicalCard> cardsPlayedThisGame = game.getModifiersQuerying().getCardsPlayedThisGame(playerId);

        final List<PhysicalCard> startingLocations = new ArrayList<>();
        for(PhysicalCard card: cardsPlayedThisGame){
            if (Filters.location.accepts(game, card)){
                startingLocations.add(card);
            }
        }

        final Filter validStartingLocationFilter = Filters.and(Filters.owner(playerId),
                Filters.and(Filters.iconCount(Icon.DARK_FORCE, 2), Filters.site));

        if (startingLocations.size() == 1 && validStartingLocationFilter.accepts(game, startingLocations.get(0))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy a mobile battleground site and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.mobile_site, Filters.battleground_site), true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.deploysForFree, Filters.always_immune_to_Alter), 1, 3, true, false));
                            action.appendEffect(
                                    new PutCardFromVoidInLostPileEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }
        return null;
    }
}