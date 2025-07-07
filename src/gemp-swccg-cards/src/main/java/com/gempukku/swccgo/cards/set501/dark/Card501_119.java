package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.HitCardEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalBattleDestinyEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalPowerUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Orbital Bombardment
 */
public class Card501_119 extends AbstractUsedInterrupt {
    public Card501_119() {
        super(Side.DARK, 5, "Orbital Bombardment", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("During battle at a site, if your Dreadnaught or Star Destroyer occupies the related system, your total battle destiny is +1. If you control the system or Fulminatrix there, +2 instead and the number of battle destiny draws may not be limited.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_25);
        setTestingText("Orbital Bombardment");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        TargetingReason targetingReason = TargetingReason.TO_BE_HIT;
        Filter opponentsCharacterInBattleFilter = Filters.and(Filters.opponents(self), Filters.character, Filters.participatingInBattle);

        if (GameConditions.isDuringBattleAt(game, Filters.site)
                && GameConditions.controlsWith(game, self, playerId, Filters.relatedSystemTo(self, Filters.battleLocation), Filters.and(Icon.FIRST_ORDER, Filters.capital_starship))
                && GameConditions.canTarget(game, self, targetingReason, opponentsCharacterInBattleFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);

            String opponent = game.getOpponent(playerId);
            String actionMessage;

            final int drawX; //number of destiny to draw (as in "draw X choose Y")
            if (GameConditions.controlsWith(game, self, playerId, Filters.relatedSystemTo(self, Filters.battleLocation), Filters.Fulminatrix)) {
                drawX = 2;
                actionMessage = "Draw two destiny and choose one";
            } else {
                drawX = 1;
                actionMessage = "Draw destiny";
            }

            action.setText(actionMessage);
            action.setActionMsg(actionMessage);
            action.allowResponses(actionMessage,
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(new DrawDestinyEffect(action, playerId, drawX, 1) {
                                @Override
                                protected void destinyDraws(final SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, final Float totalDestiny) {
                                    if (totalDestiny >= 0 && totalDestiny <= 2) {
                                        // (0-2) your total battle destiny is +1
                                        action.appendEffect(
                                                new ModifyTotalBattleDestinyEffect(action, playerId, 1));
                                    } else if (totalDestiny >= 3 && totalDestiny <= 5) {
                                        // (3-5) opponent's total power is -2;
                                        action.appendEffect(
                                                new ModifyTotalPowerUntilEndOfBattleEffect(action, -2, opponent, "Subtracts 2 from total power"));
                                    } else if (totalDestiny >= 5) {
                                        // (6+) opponent's character is 'hit' (your choice)
                                        final List<PhysicalCard> cardsToHitOptions = new LinkedList<PhysicalCard>();
                                        cardsToHitOptions.addAll(Filters.filterActive(game, self, opponentsCharacterInBattleFilter));
                                        if (!cardsToHitOptions.isEmpty()) {
                                            action.appendEffect(
                                                new ChooseCardEffect(action, playerId, "Choose character to hit", cardsToHitOptions) {
                                                    @Override
                                                    protected void cardSelected(PhysicalCard selectedCard) {
                                                        action.appendEffect(new HitCardEffect(action, selectedCard, self));
                                                    }
                                                }
                                            );
                                        }
                                    } else {
                                        game.getGameState().sendMessage("Result: No effect");
                                    }
                                }
                            });
                        }
                    });
            return Collections.singletonList(action);
        }

        return null;
    }
}