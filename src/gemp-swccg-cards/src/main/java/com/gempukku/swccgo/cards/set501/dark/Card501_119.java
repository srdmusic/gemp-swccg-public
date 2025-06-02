package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.CancelGameTextUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.CancelImmunityToAttritionUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalBattleDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost
 * Title: Orbital Bombardment
 */
public class Card501_119 extends AbstractLostInterrupt {
    public Card501_119() {
        super(Side.DARK, 4, "Orbital Bombardment", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("During battle at a site, if your [FO] capital ship controls the related system draw destiny (2 if on Fulminatrix): (0-2) add that to your total battle destiny; (3-5) opponent's total power is cumulatively-2; (6+) opponent's character is 'hit' (your choice).");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_25);
        setTestingText("Orbital Bombardment");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        if (GameConditions.isDuringBattleAt(game, Filters.site)
                && GameConditions.controlsWith(game, self, playerId, Filters.relatedSystem(self), Filters.and(Filters.icon(Icon.FIRST_ORDER), Filters.capital_starship))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.allowResponses("Draw Destiny",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(new DrawDestinyEffect(action, playerId) {
                                @Override
                                protected void destinyDraws(final SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, final Float totalDestiny) {
                                    if (totalDestiny >= 0 && totalDestiny <= 2) {
                                        //(1-2) subtract destiny draw from opponent's total battle destiny
                                        action.appendEffect(
                                                new ModifyTotalBattleDestinyEffect(action, game.getOpponent(playerId), 1));
                                    } else if (totalDestiny >= 3 && totalDestiny <= 5) {
                                        //(3-4) cancel a character's game text
                                        action.appendEffect(
                                                new ChooseCardOnTableEffect(action, playerId, "Choose character", Filters.and(Filters.character, Filters.participatingInBattle)) {
                                                    @Override
                                                    protected void cardSelected(final PhysicalCard selectedCard) {
                                                        action.addAnimationGroup(selectedCard);
                                                        // Perform result(s)
                                                        action.appendEffect(
                                                                new CancelGameTextUntilEndOfBattleEffect(action, selectedCard));
                                                    }
                                                });
                                    } else if (totalDestiny >= 6) {
                                        //(5+) cancel all opponent's immunity to attrition this battle
                                        action.appendEffect(
                                                new CancelImmunityToAttritionUntilEndOfBattleEffect(action, Filters.opponents(playerId), "cancels immunity to attrition")
                                        );
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