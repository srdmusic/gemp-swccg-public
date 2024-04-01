package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
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
        setGameText("While this side up, once per turn, may deploy a [First Order] vehicle from Reserve Deck; reshuffle. " + 
                    "During your control phase, while you control Salt Plateau: opponent loses 1 Force at each battleground " +
                    "your First Order Leader controls with another First Order character and once per turn, may peek at the top X cards " +
                    "of your Reserve Deck and take one into hand, where X = number of locations both players occupy; shuffle Reserve Deck. Kylo is power +2 on Crait. " +
                    "Place out of play if Kylo lost a battle involving Luke.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_24);
        setTestingText(Title.The_Resistance_Is_Doomed);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        final String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.THE_RESISTANCE_IS_DOOMED__DOWNLOAD_FIRST_ORDER_VEHICLE;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
            && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy vehicle from Reserve Deck");
            action.setActionMsg("Deploy [First Order] vehicle from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.FIRST_ORDER, Filters.vehicle), true));
            actions.add(action);
        }

        GameTextActionId gameTextActionId1 = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId1, Phase.CONTROL)
            && GameConditions.controls(game, playerId, Filters.Crait_Salt_Plateau)) {
            
            int numForce = Filters.countTopLocationsOnTable(game, Filters.controlsWith(playerId, self, Filters.and(Icon.FIRST_ORDER, Filters.leader, Filters.with(self, Filters.First_Order_character))));
            if (numForce > 0) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId1);
                action.setText("Make opponent lose " + numForce + " Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }

        GameTextActionId gameTextActionId2 = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId2, Phase.CONTROL)
            && GameConditions.controls(game, playerId, Filters.Crait_Salt_Plateau)
            && GameConditions.hasReserveDeck(game, playerId)) {

            int numSitesOccupiedByBothPlayers = Filters.countTopLocationsOnTable(game, Filters.and(Filters.occupies(playerId), Filters.occupies(opponent)));
            if (numSitesOccupiedByBothPlayers > 0) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId2);
                action.setText("Peek at top " + numSitesOccupiedByBothPlayers + " card" + GameUtils.s(numSitesOccupiedByBothPlayers) + " of Reserve Deck");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                action.appendEffect(
                        new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, numSitesOccupiedByBothPlayers, 1, 1)
                );
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        String playerId = self.getOwner();
        GameTextActionId gameTextActionId1 = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
            && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId1, Phase.CONTROL)
            && GameConditions.controls(game, playerId, Filters.Crait_Salt_Plateau)) {
            
            String opponent = game.getOpponent(playerId);
            int numForce = Filters.countTopLocationsOnTable(game, Filters.controlsWith(playerId, self, Filters.and(Icon.FIRST_ORDER, Filters.leader, Filters.with(self, Filters.First_Order_character))));
            if (numForce > 0) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId1);
                action.setPerformingPlayer(playerId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }

        GameTextActionId gameTextActionId3 = GameTextActionId.OTHER_CARD_ACTION_3;

        // Check condition(s)
        if (TriggerConditions.wonBattleAgainst(game, effectResult, Filters.Luke, Filters.Kylo)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId3);

            action.setText("Place objective out of play.");
            action.setActionMsg("Place The First Order Reigns / The Resistance Is Doomed out of play.");

            action.appendEffect(new PlaceCardOutOfPlayFromTableEffect(action, self));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.Kylo, Filters.on(Title.Crait)), 2));
        return modifiers;
    }

}