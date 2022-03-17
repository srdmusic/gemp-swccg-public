package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebelResistance;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.InBattleWithCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.AddDestinyToTotalPowerEffect;
import com.gempukku.swccgo.cards.effects.RevealTopCardsOfReserveDeckEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ChooseArbitraryCardsEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardsInUsedPileFromOffTableEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromReserveDeckOnTopOfCardPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Han Solo, Optimistic General
 */
public class Card501_088 extends AbstractRebelResistance {
    public Card501_088() {
        super(Side.LIGHT, 1, 4, 4, 3, 6, "Han Solo, Optimistic General", Uniqueness.UNIQUE);
        setLore("Leader. Scout.");
        setGameText("Adds 3 to power of anything he pilots. When deployed, may reveal top two cards of Reserve Deck; may take one in hand (place other(s) on Used Pile). Kylo's game text is canceled here. During battle with Chewie or Leia adds one battle destiny.");
        addPersona(Persona.HAN);
        addIcons(Icon.WARRIOR, Icon.PILOT, Icon.ENDOR, Icon.VIRTUAL_SET_15);
        addKeywords(Keyword.LEADER, Keyword.SCOUT, Keyword.GENERAL);
        setTestingText("Han Solo, Optimistic General (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new CancelsGameTextModifier(self, Filters.and(Filters.Kylo, Filters.here(self))));
        modifiers.add(new AddsBattleDestinyModifier(self, new InBattleWithCondition(self, Filters.or(Filters.Chewie, Filters.Leia)), 1));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && GameConditions.hasReserveDeck(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Reveal top two cards of Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new RevealTopCardsOfReserveDeckEffect(action, playerId, 2) {
                        @Override
                        protected void cardsRevealed(final List<PhysicalCard> cards) {
                            if (cards.size() == 2) {
                                action.appendEffect(
                                        new ChooseArbitraryCardsEffect(action, playerId, "Choose card to take into hand", cards, 0, 1) {
                                            @Override
                                            protected void cardsSelected(SwccgGame game, Collection<PhysicalCard> selectedCards) {
                                                if (selectedCards.size()==0) {
                                                    action.appendEffect(
                                                            new PlaceCardsInUsedPileFromOffTableEffect(action, cards));
                                                } else {
                                                    PhysicalCard cardToTakeIntoHand = selectedCards.iterator().next();
                                                    if (cardToTakeIntoHand != null) {
                                                        action.appendEffect(
                                                                new TakeCardIntoHandFromReserveDeckEffect(action, playerId, cardToTakeIntoHand, false));
                                                        Collection<PhysicalCard> nonSelectedCards = Filters.filter(cards, game, Filters.not(cardToTakeIntoHand));
                                                        PhysicalCard cardToPlaceInUsedPile = nonSelectedCards.iterator().next();
                                                        if (cardToPlaceInUsedPile != null) {
                                                            action.appendEffect(
                                                                    new PutCardFromReserveDeckOnTopOfCardPileEffect(action, cardToPlaceInUsedPile, Zone.USED_PILE, false));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                );
                            }
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}