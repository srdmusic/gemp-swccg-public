package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.AddDuelDestinyEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotModifyTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
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
        setGameText("During battle, add one battle destiny. For remainder of turn, opponent may not modify their total battle destiny. (If a Skywalker in battle, this is a Used Interrupt.) OR During a duel, add one destiny to your total.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_21);
        setTestingText("Courage Of A Skywalker (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.canAddBattleDestinyDraws(game, self)) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add one battle destiny");

            if (GameConditions.isDuringBattleWithParticipant(game, Filters.Skywalker))
                action.setPlayedAsSubtype(CardSubtype.USED);

            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddBattleDestinyEffect(action, 1));
                            //may not modify total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotModifyTotalBattleDestinyModifier(self, opponent, opponent),"Prevents "+opponent+" from modifying their total battle destiny"));
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
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}