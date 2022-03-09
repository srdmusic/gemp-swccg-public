package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgBuiltInCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardCombinationFromCardPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.FailCostEffect;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.ChoiceMadeResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Objective
 * Title: The Force Is Strong In My Family
 */
public class Card501_075 extends AbstractObjective {
    public Card501_075() {
        super(Side.LIGHT, 0, Title.The_Force_Is_Strong_In_My_Family);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy a [Skywalker] site and choose (reveal corresponding characters from Reserve Deck): " +
                "My Father Has It: Anakin (and [Episode I] Obi-Wan) " +
                "I Have It: Luke (and [Set 1] Obi-Wan) " +
                "You Have That Power, Too: Rey (and [Episode VII] Luke) " +
                "For remainder of game, your total Force generation is +1. You may not deploy Boss Nass’ Chambers or Jedi (except Yoda and the chosen characters). " +
                "Flip this card if a Skywalker or Jedi on table.");
        addIcons(Icon.SKYWALKER, Icon.EPISODE_I, Icon.EPISODE_VII, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_17);
        setTestingText("The Force Is Strong In My Family (ERRATA)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.SKYWALKER, Filters.site), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose [Skywalker] site to deploy";
                    }
                });

        final String MY_FATHER_HAS_IT = "My Father Has It";
        final String I_HAVE_IT = "I Have It";
        final String YOU_HAVE_THAT_POWER_TOO = "You Have That Power, Too";
        final String NO_VALID_CHOICE = "No valid choice";

        final Filter myFatherHasIt_Anakin = Filters.and(Filters.character, Filters.Anakin);
        final Filter myFatherHasIt_ObiWan = Filters.and(Filters.character, Icon.EPISODE_I, Filters.ObiWan);
        final Filter iHaveIt_Luke = Filters.and(Filters.character, Filters.Luke);
        final Filter iHaveIt_ObiWan = Filters.and(Filters.character, Icon.VIRTUAL_SET_1, Filters.ObiWan);
        final Filter youHaveThatPowerToo_Rey = Filters.and(Filters.character, Filters.Rey);
        final Filter youHaveThatPowerToo_Luke = Filters.and(Filters.character, Icon.EPISODE_VII, Filters.Luke);

        List<PhysicalCard> reserveDeck = game.getGameState().getReserveDeck(self.getOwner());
        List<String> possible = new LinkedList<>();
        if (!Filters.filter(reserveDeck, game, myFatherHasIt_Anakin).isEmpty()
                && !Filters.filter(reserveDeck, game, myFatherHasIt_ObiWan).isEmpty()) {
            possible.add(MY_FATHER_HAS_IT);
        }
        if (!Filters.filter(reserveDeck, game, iHaveIt_Luke).isEmpty()
                && !Filters.filter(reserveDeck, game, iHaveIt_ObiWan).isEmpty()) {
            possible.add(I_HAVE_IT);
        }
        if (!Filters.filter(reserveDeck, game, youHaveThatPowerToo_Rey).isEmpty()
                && !Filters.filter(reserveDeck, game, youHaveThatPowerToo_Luke).isEmpty()) {
            possible.add(YOU_HAVE_THAT_POWER_TOO);
        }
        if (possible.size() == 0)
            possible.add(NO_VALID_CHOICE);


        String[] possibleResults = possible.toArray(new String[0]);

        action.appendRequiredEffect(
                new PlayoutDecisionEffect(action, playerId, new MultipleChoiceAwaitingDecision("Choose an option", possibleResults) {
                    @Override
                    protected void validDecisionMade(int index, final String result) {
                        Filter skywalkerFilter = null;
                        Filter alternateFilter = null;

                        switch (result) {
                            case MY_FATHER_HAS_IT:
                                // Anakin and [Episode I] Obi-Wan.
                                skywalkerFilter = myFatherHasIt_Anakin;
                                alternateFilter = myFatherHasIt_ObiWan;
                                break;
                            case I_HAVE_IT:
                                // [Reflections II] Luke and [Set 1] Obi-Wan.
                                skywalkerFilter = iHaveIt_Luke;
                                alternateFilter = iHaveIt_ObiWan;
                                break;
                            case YOU_HAVE_THAT_POWER_TOO:
                                // Rey and [Episode VII] Luke.
                                skywalkerFilter = youHaveThatPowerToo_Rey;
                                alternateFilter = youHaveThatPowerToo_Luke;
                                break;
                            case NO_VALID_CHOICE:
                                action.appendRequiredEffect(new FailCostEffect(action));
                                return;
                        }

                        final Filter skywalker = Filters.and(skywalkerFilter);
                        final Filter friend = Filters.and(alternateFilter);

                        action.appendRequiredEffect(new ChooseCardCombinationFromCardPileEffect(action, playerId, Zone.RESERVE_DECK) {
                            @Override
                            public String getChoiceText(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                                return "Choose characters to reveal";
                            }

                            @Override
                            public Filter getValidToSelectFilter(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                                Filter filter = Filters.none;
                                if (cardsSelected.isEmpty()) {
                                    filter = Filters.or(skywalker, friend);
                                } else if (cardsSelected.size() == 1) {
                                    if (!Filters.filterCount(cardsSelected, game, 1, skywalker).isEmpty()) {
                                        filter = Filters.or(friend, filter);
                                    } else if (!Filters.filterCount(cardsSelected, game, 1, friend).isEmpty()) {
                                        filter = Filters.or(skywalker, filter);
                                    }
                                }
                                return filter;
                            }

                            @Override
                            public boolean isSelectionValid(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                                if (cardsSelected.size() == 2
                                        && Filters.filter(cardsSelected, game, skywalker).size() == 1
                                        && Filters.filter(cardsSelected, game, friend).size() == 1) {
                                    return true;
                                }
                                return false;
                            }

                            @Override
                            protected void cardsChosen(List<PhysicalCard> cardsChosen) {

                                Collection<PhysicalCard> theSkywalker = Filters.filter(cardsChosen, game, skywalker);
                                Collection<PhysicalCard> theFriend = Filters.filter(cardsChosen, game, friend);

                                for (PhysicalCard card : theSkywalker) {
                                    action.appendEffect(new ShowCardOnScreenEffect(action, card));
                                    action.appendEffect(new SendMessageEffect(action, playerId + " revealed " + GameUtils.getCardLink(card)
                                            + " with " + GameUtils.getCardLink(self)));
                                }
                                for (PhysicalCard card : theFriend) {
                                    action.appendEffect(new ShowCardOnScreenEffect(action, card));
                                    action.appendEffect(new SendMessageEffect(action, playerId + " revealed " + GameUtils.getCardLink(card)
                                            + " with " + GameUtils.getCardLink(self)));
                                }

                                self.setWhileInPlayData(new WhileInPlayData(result, cardsChosen));

                                action.appendEffect(new PassthruEffect(action) {
                                    @Override
                                    protected void doPlayEffect(SwccgGame game) {
                                        game.getActionsEnvironment().emitEffectResult(new ChoiceMadeResult(playerId, self, result));
                                    }
                                });
                            }
                        });
                    }
                })
        );

        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter revealedCardsFilter = new Filter() {
            @Override
            public boolean accepts(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard physicalCard) {
                List<PhysicalCard> cardsInData = self.getWhileInPlayData() != null ? self.getWhileInPlayData().getPhysicalCards() : null;
                if (cardsInData != null) {
                    for (PhysicalCard c : cardsInData) {
                        if (Filters.sameTitle(physicalCard).accepts(gameState, modifiersQuerying, c))
                            return true;
                    }
                }
                return false;
            }
        };

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new TotalForceGenerationModifier(self, 1, self.getOwner()));
        modifiers.add(new MayNotDeployModifier(self, Filters.Boss_Nass_Chambers, self.getOwner()));
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.Jedi, Filters.except(Filters.or(Filters.Yoda, revealedCardsFilter))), self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.canSpot(game, self, Filters.or(Filters.Skywalker, Filters.Jedi))) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);

        }
        return null;
    }


    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (self.getWhileInPlayData() == null)
            return null;

        String text = "Chosen option: " + self.getWhileInPlayData().getTextValue();

        if (self.getWhileInPlayData().getPhysicalCards() != null
                && !self.getWhileInPlayData().getPhysicalCards().isEmpty()) {
            text += "; Revealed card" + GameUtils.s(self.getWhileInPlayData().getPhysicalCards().size())
                    + ": " + GameUtils.getAppendedNames(self.getWhileInPlayData().getPhysicalCards());
        }

        return text;
    }
}