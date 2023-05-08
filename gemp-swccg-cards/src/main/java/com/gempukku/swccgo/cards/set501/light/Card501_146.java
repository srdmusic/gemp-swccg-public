package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.AddDuelDestinyEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.ModifyDuelTotalEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotModifyTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotResetTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost
 * Title: Courage Of A Skywalker (V)
 */
public class Card501_146 extends AbstractLostInterrupt {
    public Card501_146() {
        super(Side.LIGHT, 2, Title.Courage_Of_A_Skywalker, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Despite being alone, trapped and desperately outmatched, Luke continued his battle with the Dark Lord of the Sith.");
        setGameText("Once per game, if on top of Lost Pile, may retrieve into hand. Add one battle destiny or duel destiny (if a Skywalker in that battle or duel, your total power or duel total is +2); for remainder of turn, opponent may not modify or reset either player’s total battle destiny.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_21);
        setTestingText("Courage Of A Skywalker (V)");
    }

    @Override
    public List<Action> getCardPilePhaseActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<Action> actions = new LinkedList<>();
        actions.addAll(super.getCardPilePhaseActions(playerId, game, self));


        GameTextActionId gameTextActionId = GameTextActionId.COURAGE_OF_A_SKYWALKER_V__RETRIEVE;

        if (self.getZone()== Zone.TOP_OF_LOST_PILE
                && !game.getModifiersQuerying().hasFlagActive(game.getGameState(), ModifierFlag.LOST_PILE_FACE_DOWN, playerId)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            TopLevelGameTextAction action = new TopLevelGameTextAction(self, self.getCardId(), gameTextActionId);
            action.setText("Retrieve into hand");

            action.appendUsage(
                    new OncePerGameEffect(action));

            action.appendEffect(
                    new RetrieveCardIntoHandEffect(action, playerId, self));

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.canAddBattleDestinyDraws(game, self)) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add one battle destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddBattleDestinyEffect(action, 1));
                            if (GameConditions.isDuringBattleWithParticipant(game, Filters.Skywalker)) {
                                action.appendEffect(new AddUntilEndOfBattleModifierEffect(action,
                                        new TotalPowerModifier(self, Filters.battleLocation, 2, playerId), "Adds 2 to your total power"));
                            }

                            //modify total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotModifyTotalBattleDestinyModifier(self, null, opponent),"Prevents "+opponent+" from modifying either player's total battle destiny"));
                            //reset total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotResetTotalBattleDestinyModifier(self, null, opponent),"Prevents "+opponent+" from resetting either player's total battle destiny"));
                        }
                    }
            );
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isDuelAddOrModifyDuelDestiniesStep(game, effectResult)) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add one duel destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddDuelDestinyEffect(action, 1));

                            if (GameConditions.isDuringDuelWithParticipant(game, Filters.Skywalker)) {
                                action.appendEffect(
                                        new ModifyDuelTotalEffect(action, 2, playerId, "Adds 2 to duel total"));
                            }

                            //modify total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotModifyTotalBattleDestinyModifier(self, null, opponent),"Prevents "+opponent+" from modifying either player's total battle destiny"));
                            //reset total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotResetTotalBattleDestinyModifier(self, null, opponent),"Prevents "+opponent+" from resetting either player's total battle destiny"));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}