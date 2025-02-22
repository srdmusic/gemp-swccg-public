package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.SendMessageEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Salacious Crumb (V)
 */
public class Card501_017 extends AbstractAlien {
    public Card501_017() {
        super(Side.DARK, 3, 2, 1, 1, 3, Title.Salacious_Crumb, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Male Kowakian. Prankster. Humiliates others for Jabba's amusement. His life depends on making Jabba laugh at least once per day.");
        setGameText("When deployed, may draw top card of Reserve Deck or place a droid present in owner's Used Pile ('AH-hahahaha!'). Game text of your alien leaders here may not be canceled. If at a converted Jabba's Palace site, may raise yours to the top.");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setSpecies(Species.KOWAKIAN);
        setVirtualSuffix(true);
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        //Both possible responses share a gameTextActionId so that the player can only perform one of them
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        Filter droidPresent = Filters.and(Filters.droid, Filters.present(self));

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && GameConditions.hasReserveDeck(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            
            action.setText("Draw top card of Reserve Deck");
            action.setActionMsg("Draw top card of Reserve Deck into hand");
            // Perform result(s)
            action.appendEffect(
                    new DrawCardIntoHandFromReserveDeckEffect(action, playerId));
            actions.add(action);
        }

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && GameConditions.canTarget(game, self, droidPresent)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            
            action.setText("Place a droid in owner's Used Pile");
            action.setActionMsg("Place a droid present in owner's Used Pile");
            // Choose target(s)
            action.appendTargeting(
              new ChooseCardOnTableEffect(action, playerId, "Choose a droid present", Filters.and(Filters.droid, Filters.present(self))) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            // Send Easter Egg Message
                            action.appendCost(
                                new SendMessageEffect(action, "Salacious Crumb: AH-hahahaha!"));
                            // Perform result(s)
                            action.appendEffect(
                                    new PlaceCardInUsedPileFromTableEffect(action, selectedCard));
                        }
              }
            );
            actions.add(action);
        }

        return actions;
    }
}
