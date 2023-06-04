package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.CancelCardBeingPlayedEffect;
import com.gempukku.swccgo.logic.effects.ModifyPowerUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayingCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardBeingPlayedForCancelingEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Interrupt
 * Subtype: Lost
 * Title: Attack Pattern Delta (V)
 */
public class Card501_014 extends AbstractLostInterrupt {
    public Card501_014() {
        super(Side.LIGHT, 3, "Attack Pattern Delta", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Snowspeeder attack plan devised by Commander Skywalker and Rebel tactician Beryl Chifonage. Single-file formation protects the squadron as the leader draws fire.");
        setGameText("If you have two T-47s in a battle together, add one battle destiny. OR Make opponent's combat vehicle power -3 until end of turn. OR If you occupy three Hoth battleground sites with T-47s, cancel a non-[immune to Sense] Interrupt just played.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_22);
        setTestingText("Attack Pattern Delta (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.canAddBattleDestinyDraws(game, self)
                && GameConditions.canTarget(game, self, 2, Filters.and(Filters.participatingInBattle, Filters.your(self), Filters.T_47, Filters.piloted))) {

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
                        }
                    }
            );
            actions.add(action);
        }


        Filter opponentsCombatVehicle = Filters.and(Filters.opponents(self), Filters.combat_vehicle);

        if (GameConditions.canTarget(game, self, opponentsCombatVehicle)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Make a combat vehicle power -3");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose opponent's combat vehicle", opponentsCombatVehicle) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Reduce power of " + GameUtils.getCardLink(targetedCard) + " by 3",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the final targeted card(s)
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ModifyPowerUntilEndOfTurnEffect(action, finalTarget, -3));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, Effect effect, final PhysicalCard self) {
        Filter interruptFilter = Filters.and(Filters.Interrupt, Filters.not(Filters.dejarikHologramAtHolosite), Filters.not(Filters.immune_to_Sense));

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, interruptFilter)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)
                && GameConditions.occupiesWith(game, self, playerId, 3, Filters.and(Filters.Hoth_site, Filters.battleground), Filters.and(Filters.T_47, Filters.canBeTargetedBy(self)))) {

            final RespondablePlayingCardEffect respondableEffect = (RespondablePlayingCardEffect) effect;

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Cancel " + GameUtils.getFullName(respondableEffect.getCard()));
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardBeingPlayedForCancelingEffect(action, respondableEffect) {
                        @Override
                        protected void cardTargetedToBeCanceled(final PhysicalCard targetedEffect) {
                            action.allowResponses("Cancel " + GameUtils.getCardLink(targetedEffect),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            action.appendEffect(
                                                    new CancelCardBeingPlayedEffect(action, respondableEffect));
                                        }
                                    }
                            );
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}