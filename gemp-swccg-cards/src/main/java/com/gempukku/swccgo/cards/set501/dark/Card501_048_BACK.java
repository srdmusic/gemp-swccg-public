package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Objective 
 * Title: On The Verge Of Greatness / Deploy The Garrison!
 */
public class Card501_048_BACK extends AbstractObjective {
    public Card501_048_BACK() {
        super(Side.DARK, 7, Title.Deploy_The_Garrison);
        setGameText("While this side up, Vader's game text may not be canceled and he is power +2. Your Force generation is +1 for each Scarif site 'blown away.' Tarkin Doctrine is [Immune to Alter.] and if it just caused Force loss, may take any one card into hand from Force Pile. Once per turn, may place opponent's character just lost from your location out of play unless opponent loses 1 Force.\n" +
                "Flip this card if you have no leaders on Scarif.\n" +
                "Place out of play if Shield Gate or Death Star 'blown away.'");
        addIcons(Icon.VIRTUAL_SET_15);
        setTestingText("Deploy The Garrison!");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, Filters.Vader, 2));
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self, Filters.Vader));
        modifiers.add(new ForceGenerationModifier(self, new OnTableEvaluator(self, Filters.and(Filters.partOfSystem(Title.Scarif), Filters.blown_away)), playerId));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.Tarkin_Doctrine, Title.Alter));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(playerId);

        // if [Tarkin Doctrine] just caused Force loss, may take any one card into hand from Force Pile.
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (TriggerConditions.justLostForceFromCard(game, effectResult, opponent, Filters.Tarkin_Doctrine)
                && GameConditions.hasForcePile(game, playerId)) {
            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Take a card into hand from Force Pile");
            action.setActionMsg("Take any one card into hand from Force Pile");

            action.appendEffect(new TakeCardIntoHandFromForcePileEffect(action, playerId, true));

            actions.add(action);
        }

        // Once per turn, may place opponent's character just lost from your location out of play unless opponent loses 1 Force.
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        // Check condition(s)
        if (TriggerConditions.justLostFromLocation(game, effectResult, Filters.and(Filters.opponents(self), Filters.character), Filters.your(self))
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            final GameState gameState = game.getGameState();
            final PhysicalCard lostCard = ((LostFromTableResult) effectResult).getCard();

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Place " + GameUtils.getFullName(lostCard) + " out of play");
            action.setActionMsg("Place " + GameUtils.getCardLink(lostCard) + " out of play");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new PlayoutDecisionEffect(action, opponent,
                            new YesNoDecision("Do you want to lose 1 Force instead of having " + GameUtils.getCardLink(lostCard) + " placed out of play?") {
                                @Override
                                protected void yes() {
                                    gameState.sendMessage(opponent + " chooses to lose 1 Force instead of having " + GameUtils.getCardLink(lostCard) + " placed out of play");
                                    action.appendEffect(
                                            new LoseForceEffect(action, opponent, 1, true));
                                }
                                protected void no() {
                                    gameState.sendMessage(opponent + " chooses to not lose 1 Force instead of having " + GameUtils.getCardLink(lostCard) + " placed out of play");
                                    action.appendEffect(
                                            new PlaceCardOutOfPlayFromOffTableEffect(action, lostCard));
                                }
                            }
                    ));
            actions.add(action);
        }


        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();

        // Check condition(s)
        if (TriggerConditions.isBlownAwayLastStep(game, effectResult, Filters.and(CardSubtype.SYSTEM, Filters.title(Title.Death_Star, true)))
            || (TriggerConditions.isTableChanged(game, effectResult)
                && (game.getModifiersQuerying().isShieldGateBlownAway(game.getGameState())
                        || GameConditions.canSpot(game, self, Filters.and(Filters.blown_away, Filters.or(Filters.and(Filters.system, Filters.title(Title.Death_Star, true)), Filters.Shield_Gate)))))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place out of play");
            action.setActionMsg("Place " + GameUtils.getCardLink(self) + " out of play");
            // Perform result(s)
            action.appendEffect(
                    new PlaceCardOutOfPlayFromTableEffect(action, self));
            return Collections.singletonList(action);
        }

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && !GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.and(Filters.your(self), Filters.leader, Filters.on(Title.Scarif)))) {

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
}
