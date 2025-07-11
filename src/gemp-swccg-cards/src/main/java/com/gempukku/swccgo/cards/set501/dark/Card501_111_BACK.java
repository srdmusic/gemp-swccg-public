package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotForceDrainAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: The First Order Reigns / The Resistance Is Doomed
 */
public class Card501_111_BACK extends AbstractObjective {
    public Card501_111_BACK() {
        super(Side.DARK, 7, Title.The_Resistance_Is_Doomed, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May immediately retrieve up to 3 Force. While this side up, your Force drains at battlegrounds where you have two First Order characters are +1. Once per turn, may deploy a [First Order] vehicle (or trooper) from Lost Pile. While you control Salt Plateau (or opponent's site) with Kylo, opponent's Force retrieval is canceled and opponent may not Force drain where their character or permanent pilot is alone. Place out of play if Kylo just forfeited from a battle you lost at Salt Plateau where Han, Leia, or Luke present.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_25);
        setTestingText(Title.The_Resistance_Is_Doomed);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.THE_FIRST_ORDER_REIGNS__DOWNLOAD_EPISODE_7_BATTLEGROUND;
        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy location from Reserve Deck");
            action.setActionMsg("Deploy an [Episode VII] battleground from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.EPISODE_VII, Filters.battleground), true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.THE_RESISTANCE_IS_DOOMED__DOWNLOAD_FIRST_ORDER_VEHICLE_OR_TROOPER;
        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
            && GameConditions.canDeployCardFromLostPile(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Lost Pile");
            action.setActionMsg("Deploy [First Order] vehicle or Trooper from Lost Pile");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromLostPileEffect(action, Filters.and(Icon.FIRST_ORDER, Filters.or(Filters.vehicle, Filters.trooper)), true));
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        // Check condition(s)
        if (TriggerConditions.cardFlipped(game, effectResult, self)) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);

            action.setText("Retrieve Force");
            action.setActionMsg("Have " + playerId + " retrieve up to 3 Force");
            // Perform result(s)
            action.appendEffect(
                    new PlayoutDecisionEffect(action, playerId,
                            new IntegerAwaitingDecision("Choose amount of Force to retrieve", 0, 3, 3) {
                                @Override
                                public void decisionMade(final int amountToRetrieve) throws DecisionResultInvalidException {
                                    GameState gameState = game.getGameState();
                                    if (amountToRetrieve == 0) {
                                        gameState.sendMessage(playerId + " chooses to not retrieve any Force");
                                        return;
                                    }
                                    gameState.sendMessage(playerId + " chooses to retrieve " + amountToRetrieve + " Force");
                                    action.appendEffect(
                                            new RetrieveForceEffect(action, playerId, amountToRetrieve));
                                }
                            }
                    )
            );
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        Filter hanLeiaLuke = Filters.or(Filters.Han, Filters.Leia, Filters.Luke);
        Filter locationFilter = Filters.and(Filters.Crait_Salt_Plateau, Filters.wherePresent(self, hanLeiaLuke));
        // Check condition(s)
        if (TriggerConditions.justForfeitedToLostPileFromLocation(game, effectResult, Filters.Kylo, locationFilter)
                && GameConditions.isDuringBattleLostBy(game, playerId)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);

            action.setText("Place objective out of play.");
            action.setActionMsg("Place The First Order Reigns / The Resistance Is Doomed out of play.");

            action.appendEffect(new PlaceCardOutOfPlayFromTableEffect(action, self));
            actions.add(action);
        }

        Filter opponentsSite = Filters.and(Filters.your(opponent), Filters.site);
        // Check condition(s)
        if (TriggerConditions.isAboutToRetrieveForce(game, effectResult, game.getOpponent(self.getOwner()))
                && GameConditions.controlsWith(game, self, playerId, Filters.or(Filters.Crait_Salt_Plateau, opponentsSite), Filters.Kylo)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Cancel retrieval");
            action.setActionMsg("Force retrieval is canceled");
            action.appendEffect(
                    new CancelForceRetrievalEffect(action)
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        
        Filter opponentsSite = Filters.and(Filters.your(opponent), Filters.site);
        Condition kyloControlsCondition = new ControlsWithCondition(self, playerId, Filters.or(Filters.Crait_Salt_Plateau, opponentsSite), Filters.Kylo);
        Filter locationHasOneCardsWithAbility = Filters.and(Filters.sameLocationAs(self, Filters.and(Filters.opponents(self), Filters.characterOrPermanentPilotAlone)));
        Filter battlegroundsWithTwoFirstOrderCharacters = Filters.and(Filters.battleground, Filters.occupiesWith(playerId, self, Filters.and(Filters.First_Order_character, Filters.with(self, Filters.First_Order_character))));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, battlegroundsWithTwoFirstOrderCharacters, 1, playerId));
        modifiers.add(new MayNotForceDrainAtLocationModifier(self, Filters.sameLocationAs(self, locationHasOneCardsWithAbility), kyloControlsCondition, opponent));
        return modifiers;
    }

}