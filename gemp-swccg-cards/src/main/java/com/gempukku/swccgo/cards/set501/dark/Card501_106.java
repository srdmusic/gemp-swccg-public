package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesMoreThanOpponentCondition;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalPowerUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelWeaponDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used
 * Title: A Dark Time For The Rebellion & Tarkin's Orders
 */
public class Card501_106 extends AbstractUsedInterrupt {
    public Card501_106() {
        super(Side.DARK, 5, "A Dark Time For The Rebellion & Tarkin's Orders", Uniqueness.UNIQUE);
        addComboCardTitles("A Dark Time For The Rebellion", "Tarkin's Orders");
        setGameText("For remainder of turn, opponent may not cancel your battle destiny draws (or character weapon destiny draws if opponent’s character is out of play). OR During battle, if you have more battlegrounds on table than opponent, add X to your total power (where X = number of opponent's non-battleground locations on table, if opponent has no battleground locations also add one battle destiny). OR Cancel It Could Be Worse or Projection Of A Skywalker.");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("A Dark Time For The Rebellion & Tarkin's Orders (ERRATA)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);

        List<PlayInterruptAction> actions = new LinkedList<>();

        if (GameConditions.isOutOfPlay(game, Filters.and(Filters.opponents(self), Filters.character))) {
            final PlayInterruptAction protectBattleDestinyDrawsAction = new PlayInterruptAction(game, self);
            protectBattleDestinyDrawsAction.setText("Affect battle destiny draws");
            protectBattleDestinyDrawsAction.setActionMsg("For remainder of turn, prevent opponent from canceling your battle destiny draws");

            // Allow response(s)
            protectBattleDestinyDrawsAction.allowResponses(
                    new RespondablePlayCardEffect(protectBattleDestinyDrawsAction) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            protectBattleDestinyDrawsAction.appendEffect(
                                    new AddUntilEndOfTurnModifierEffect(protectBattleDestinyDrawsAction,
                                            new MayNotCancelBattleDestinyModifier(self, playerId, opponent),
                                            "Prevent "+opponent+" from canceling "+playerId+"'s battle destiny draws")
                            );
                        }
                    }
            );
            actions.add(protectBattleDestinyDrawsAction);
        } else {
            final PlayInterruptAction protectBattleOrWeaponDestinyDrawsAction = new PlayInterruptAction(game, self);
            protectBattleOrWeaponDestinyDrawsAction.setText("Affect battle or weapon destiny draws");
            protectBattleOrWeaponDestinyDrawsAction.setActionMsg("For remainder of turn, prevent opponent from canceling your battle or weapon destiny draws.");

            // Allow response(s)
            protectBattleOrWeaponDestinyDrawsAction.allowResponses(
                    new RespondablePlayCardEffect(protectBattleOrWeaponDestinyDrawsAction) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            protectBattleOrWeaponDestinyDrawsAction.appendEffect(
                                    new AddUntilEndOfTurnModifierEffect(protectBattleOrWeaponDestinyDrawsAction,
                                            new MayNotCancelBattleDestinyModifier(self, playerId, opponent),
                                            "Prevent "+opponent+" from canceling "+playerId+"'s battle destiny draws")
                            );
                            protectBattleOrWeaponDestinyDrawsAction.appendEffect(
                                    new AddUntilEndOfTurnModifierEffect(protectBattleOrWeaponDestinyDrawsAction,
                                            new MayNotCancelWeaponDestinyModifier(self, opponent, Filters.character_weapon),
                                            "Prevent "+opponent+" from canceling "+playerId+"'s weapon destiny draws")
                            );
                        }
                    }
            );
            actions.add(protectBattleOrWeaponDestinyDrawsAction);
        } 

        int yourBattlegroundCount = Filters.countTopLocationsOnTable(game, Filters.and(Filters.your(self), Filters.battleground));
        final int opponentBattlegroundCount = Filters.countTopLocationsOnTable(game, Filters.and(Filters.opponents(self), Filters.battleground));

        if (GameConditions.isDuringBattle(game) && (yourBattlegroundCount > opponentBattlegroundCount)) {
            final int opponentNonBattlegroundCount = Filters.countTopLocationsOnTable(game, Filters.and(Filters.opponents(self), Filters.non_battleground_location));

            final PlayInterruptAction action = new PlayInterruptAction(game, self);

            action.allowResponses(
                new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        action.appendEffect(new ModifyTotalPowerUntilEndOfBattleEffect(action, opponentNonBattlegroundCount, playerId,
                            "Adds " + opponentNonBattlegroundCount + " to total power"));
                        
                        if (opponentBattlegroundCount == 0) {
                            action.appendEffect(new AddBattleDestinyEffect(action, 1));
                        }
                    }
                }
            );
            actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.It_Could_Be_Worse)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.It_Could_Be_Worse, Title.It_Could_Be_Worse);
            actions.add(action);
        }


        if (GameConditions.canTarget(game, self, TargetingReason.TO_BE_CANCELED, Filters.title(Title.Projection_Of_A_Skywalker))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.title(Title.Projection_Of_A_Skywalker), Title.Projection_Of_A_Skywalker);
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.It_Could_Be_Worse)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }

        return actions;
    }
}