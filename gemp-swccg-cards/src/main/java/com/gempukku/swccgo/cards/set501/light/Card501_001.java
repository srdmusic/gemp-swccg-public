package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ModifyNumCardsDrawnInStartingHandEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardCombinationIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Starting
 * Title: This Is The Way
 */
public class Card501_001 extends AbstractUsedOrStartingInterrupt {
    public Card501_001() {
        super(Side.LIGHT, 4, "This Is The Way", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Used: Take a unique Mandalorian into hand from Reserve Deck, reshuffle." +
                "Starting: Take Din, Weapons Are Part Of My Religion, The Razor Crest, and Mandalorian Forge into hand. " +
                "Draw 6 cards instead of 8. Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_23);
        setTestingText("This Is The Way");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.THIS_IS_THE_WAY__UPLOAD_MANDALORIAN;
        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Upload a Mandalorian");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Filters.unique, Filters.Mandalorian), true));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, SwccgGame game, final PhysicalCard self) {

        final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
        action.setText("Take cards into hand from Reserve Deck");
        // Allow response(s)
        action.allowResponses("Take Din, Weapons Are Part Of My Religion, The Razor Crest, and Mandalorian Forge into hand from Reserve Deck",
                new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        // Perform result(s)
                        action.appendEffect(
                                new TakeCardCombinationIntoHandFromReserveDeckEffect(action, playerId, false) {
                                    @Override
                                    public String getChoiceText(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                                        return "Choose Din, Weapons Are Part Of My Religion, The Razor Crest, and Mandalorian Forge";
                                    }

                                    @Override
                                    public Filter getValidToSelectFilter(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                                        return Filters.or(Filters.Din, Filters.title(Title.Weapons_Are_Part_Of_My_Religion), Filters.Razor_Crest, Filters.title(Title.Mandalorian_Forge));
                                    }

                                    @Override
                                    public boolean isSelectionValid(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                                        if (Filters.filter(cardsSelected, game, Filters.Din).size() != 1
                                                || Filters.filter(cardsSelected, game, Filters.title(Title.Weapons_Are_Part_Of_My_Religion)).size() != 1
                                                || Filters.filter(cardsSelected, game, Filters.Razor_Crest).size() != 1
                                                || Filters.filter(cardsSelected, game, Filters.title(Title.Mandalorian_Forge)).size() != 1) {
                                            return false;
                                        }
                                        return true;
                                    }
                                });
                        action.appendEffect(
                                new ModifyNumCardsDrawnInStartingHandEffect(action, playerId, 6));
                        action.appendEffect(
                                new PutCardFromVoidInReserveDeckEffect(action, playerId, self));
                    }
                }
        );
        return action;
    }
}