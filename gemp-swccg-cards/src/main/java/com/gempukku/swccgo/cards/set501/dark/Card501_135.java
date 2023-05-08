package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
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
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.AttritionModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveTotalAbilityReducedModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotModifyTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotResetTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost
 * Title: Wounded Wookiee (V)
 */
public class Card501_135 extends AbstractLostInterrupt {
    public Card501_135() {
        super(Side.DARK, 2, Title.Wounded_Wookiee, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("When blaster fire from the barge's gun hit Chewie's skiff, his leg was injured by shrapnel. This setback distracted the Rebels, causing them to momentarily lose their advantage.");
        setGameText("Once per game, if on top of your Lost Pile, may retrieve into hand. Add one battle destiny (if Chewie or a guard in that battle, attrition against you is -2); for remainder of turn, opponent may not modify or reset either player's total battle destiny (or reduce your total ability).");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_21);
        setTestingText("Wounded Wookiee (V)");
    }

    @Override
    public List<Action> getCardPilePhaseActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<Action> actions = new LinkedList<>();
        actions.addAll(super.getCardPilePhaseActions(playerId, game, self));


        GameTextActionId gameTextActionId = GameTextActionId.WOUNDED_WOOKIEE_V__RETRIEVE;

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
                            if (GameConditions.isDuringBattleWithParticipant(game, Filters.or(Filters.Chewie, Filters.guard))) {
                                action.appendEffect(new AddUntilEndOfBattleModifierEffect(action,
                                        new AttritionModifier(self, -2, playerId), "Subtracts 2 from attrition against you"));
                            }

                            //modify total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotModifyTotalBattleDestinyModifier(self, null, opponent),"Prevents "+opponent+" from modifying either player's total battle destiny"));
                            //reset total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotResetTotalBattleDestinyModifier(self, null, opponent),"Prevents "+opponent+" from resetting either player's total battle destiny"));
                            //reduce total ability
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotHaveTotalAbilityReducedModifier(self, Filters.any, playerId),"Prevents your total ability from being reduced"));
                        }
                    }
            );
            return Collections.singletonList(action);
        }

        return null;
    }
}