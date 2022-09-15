package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardOfCardPileEffect;
import com.gempukku.swccgo.cards.effects.complete.ChooseExistingCardPileEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardsOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeOneCardIntoHandFromOffTableEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 20
 * Type: Objective
 * Title: Hunt For The Droid General / Grievous Will Run And Hide
 */
public class Card501_065_BACK extends AbstractObjective {
    public Card501_065_BACK() {
        super(Side.LIGHT, 7, "Grievous Will Run And Hide");
        setGameText("While this side up, your Force drains are +1 where you have a clone/Jedi pair. X = number of battlegrounds your [Clone Army] cards occupy. If you just initiated a battle: peek at the top card of your Reserve Deck or Used Pile (may take it into hand or place it on bottom of Reserve Deck), then if X > 1, retrieve a [Clone Army] card (or Cloning Cylinders) into hand, then if X > 2, your clone may make a regular move (for free) to the battle location. \n" +
                "Flip this card if He Is A Coward at a battleground or Grievous alone at a battleground.");
        addIcons(Icon.CLONE_ARMY, Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        setTestingText("Grievous Will Run And Hide");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.not(Icon.EPISODE_I), Filters.Jedi), self.getOwner()));
        modifiers.add(new DeployCostModifier(self, Filters.and(Filters.your(self), Filters.not(Icon.EPISODE_I), Filters.hasAbilityOrHasPermanentPilotWithAbility), 2));
        modifiers.add(new IconModifier(self, Filters.and(Filters.your(self), Filters.Jedi), Icon.PILOT));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.your(self), Icon.EPISODE_I, Filters.site), Title.No_Escape));
        modifiers.add(new ForceDrainModifier(self, Filters.and(Filters.occupiesWith(self.getOwner(), self, Filters.Jedi), Filters.occupiesWith(self.getOwner(), self, Filters.clone)), 1, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.You_Are_Beaten)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.You_Are_Beaten, Title.You_Are_Beaten);
            action.appendCost(new LoseForceEffect(action, playerId, 1, true));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.You_Are_Beaten)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.appendCost(new LoseForceEffect(action, playerId, 1, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (TriggerConditions.isEndOfOpponentsTurn(game, effectResult, playerId)) {
            int battlegroundsYouOccupy = Filters.filterTopLocationsOnTable(game, Filters.occupies(playerId)).size();
            int battlegroundsOpponentOccupies = Filters.filterTopLocationsOnTable(game, Filters.occupies(opponent)).size();

            if (battlegroundsYouOccupy > battlegroundsOpponentOccupies) {
                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText(opponent + " loses 1 Force");
                action.appendEffect(
                        new LoseForceEffect(action, opponent, 1));
                actions.add(action);
            }
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        if (TriggerConditions.battleInitiated(game, effectResult, playerId)) {
            int battlegroundCount = Filters.filterTopLocationsOnTable(game, Filters.and(Filters.battleground, Filters.occupiesWith(playerId, self, Filters.icon(Icon.CLONE_ARMY)))).size();
            float x = game.getModifiersQuerying().getVariableValue(game.getGameState(), self, Variable.X, battlegroundCount);

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);

            action.appendEffect(new ChooseExistingCardPileEffect(action, playerId, playerId, Filters.or(Zone.RESERVE_DECK, Zone.USED_PILE)) {
                @Override
                protected void pileChosen(SwccgGame game, String cardPileOwner, Zone cardPile) {
                    action.insertEffect(new PeekAtTopCardOfCardPileEffect(action, playerId, playerId, cardPile) {
                        @Override
                        protected void cardsPeekedAt(List<PhysicalCard> peekedAtCards) {
                            final PhysicalCard card = peekedAtCards.iterator().next();

                            final String intoHand = "Take card into hand";
                            final String bottomOfReserve = "Place card on bottom of Reserve Deck";
                            final String nothing = "Do not move the card";
                            action.insertEffect(new PlayoutDecisionEffect(action, playerId, new MultipleChoiceAwaitingDecision("Choose what to do with the card", new String[]{intoHand, bottomOfReserve, nothing}) {
                                @Override
                                protected void validDecisionMade(int index, String result) {
                                    if (intoHand.equals(result)) {
                                        action.insertEffect(new TakeOneCardIntoHandFromOffTableEffect(action, playerId, card, "Takes card into hand") {
                                            @Override
                                            protected void afterCardTakenIntoHand() {

                                            }
                                        });
                                    } else if (bottomOfReserve.equals(result)) {
                                        action.insertEffect(
                                                new PutCardFromCardPileOnBottomOfCardPileEffect(action, playerId, card, Zone.RESERVE_DECK, true));
                                    } else {
                                        action.insertEffect(new SendMessageEffect(action, playerId + " chooses not to move the card"));
                                    }
                                }
                            }));
                        }
                    });
                }
            });


            if (x > 1) {
                action.appendEffect(new RetrieveCardIntoHandEffect(action, playerId, Filters.or(Icon.CLONE_ARMY, Filters.title(Title.Cloning_Cylinders))));

                final PhysicalCard location = Filters.findFirstFromTopLocationsOnTable(game, Filters.battleLocation);
                final Filter cloneToMove = Filters.and(Filters.your(self), Filters.clone, Filters.movableAsRegularMove(playerId, true, 0, false, Filters.locationAndCardsAtLocation(Filters.battleLocation)));
                if (x > 2 && location != null
                        && GameConditions.canTarget(game, self, cloneToMove)) {

                    action.appendEffect(new ChooseCardsOnTableEffect(action, playerId, "Choose a clone to move as a regular move (for free) to "+GameUtils.getCardLink(location),0, 1, cloneToMove) {
                        @Override
                        protected void cardsSelected(Collection<PhysicalCard> selectedCards) {
                            if (selectedCards.size() == 1) {
                                PhysicalCard clone = selectedCards.iterator().next();
                                if (clone != null) {
                                    action.appendEffect(new MoveCardAsRegularMoveEffect(action, playerId, clone, true, false, Filters.locationAndCardsAtLocation(Filters.battleLocation)));
                                }
                            }
                        }
                    });

                }
            }

            actions.add(action);
        }



        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && (GameConditions.canSpot(game, self, Filters.and(Filters.Grievous, Filters.alone, Filters.at(Filters.battleground)))
                || !GameConditions.hasAttached(game, self, Filters.title("He Is A Coward")))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }
        return actions;
    }
}