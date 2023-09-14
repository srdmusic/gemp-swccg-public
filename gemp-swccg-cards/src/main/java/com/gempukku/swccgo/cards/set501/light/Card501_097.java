package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmunityToAttritionChangeModifier;
import com.gempukku.swccgo.logic.modifiers.ManeuverModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: Lone Rogue (V)
 */
public class Card501_097 extends AbstractLostOrStartingInterrupt {
    public Card501_097() {
        super(Side.LIGHT, 4, Title.Lone_Rogue, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("The pilots at the Rebel Base on Hoth are trained to respond quickly to the Empire's forces. Many Rebels feel that they could take on the whole Empire themselves.");
        setGameText("LOST: If opponent just initiated a battle, target a T-47. T-47 is either maneuver or immunity to attrition +2 (your choice). " +
                "STARTING: If a [Hoth] objective on table, deploy Echo Base Garrison and 2 Effects that deploy for free and are always [Immune to Alter]. Place Interrupt in Lost Pile.");
        addIcons(Icon.PREMIUM, Icon.VIRTUAL_SET_22);
        setTestingText("Lone Rogue (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();
        final String opponent = game.getOpponent(playerId);
        Filter t47defending = Filters.and(Filters.T_47, Filters.piloted, Filters.defendingBattle);
        // Check condition(s)
        if (TriggerConditions.battleInitiated(game, effectResult, opponent)
                && GameConditions.canTarget(game, self, t47defending)) {


            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Add 2 to maneuver");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose T-47", t47defending) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Add 2 to maneuver of " + GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the final targeted card(s)
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddUntilEndOfBattleModifierEffect(action, new ManeuverModifier(self, finalTarget, 2), "Adds 2 to maneuver of " + GameUtils.getCardLink(finalTarget) + " until end of battle"));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);


            // needs to have immunity to attrition to add 2 to it
            Filter withImmunityFilter = Filters.and(t47defending, Filters.hasAnyImmunityToAttrition);

            if (GameConditions.canTarget(game, self, withImmunityFilter)) {
                final PlayInterruptAction action2 = new PlayInterruptAction(game, self, CardSubtype.LOST);
                action2.setText("Add 2 to immunity to attrition");
                // Choose target(s)
                action2.appendTargeting(
                        new TargetCardOnTableEffect(action2, playerId, "Choose T-47", withImmunityFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                                action2.addAnimationGroup(targetedCard);
                                // Allow response(s)
                                action2.allowResponses("Add 2 to immunity to attrition of " + GameUtils.getCardLink(targetedCard),
                                        new RespondablePlayCardEffect(action2) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the final targeted card(s)
                                                final PhysicalCard finalTarget = action2.getPrimaryTargetCard(targetGroupId);
                                                // Perform result(s)
                                                action2.appendEffect(
                                                        new AddUntilEndOfBattleModifierEffect(action2, new ImmunityToAttritionChangeModifier(self, finalTarget, 2), "Adds 2 to immunity to attrition of " + GameUtils.getCardLink(finalTarget) + " until end of battle"));
                                            }
                                        }
                                );
                            }
                        }
                );
                actions.add(action2);
            }
        }
        return actions;
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, SwccgGame game, final PhysicalCard self) {
        final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
        action.setText("Deploy Echo Base Garrison and up to 2 Effects that deploy for free and are always immune to Alter.");
        // Allow response(s)

        if (GameConditions.canSpot(game, self, Filters.and(Filters.icon(Icon.HOTH), Filters.Objective))) {
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title("Echo Base Garrison"), true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.immune_to_Alter,
                                            Filters.deploysForFree), 1, 2, true, false));
                            action.appendEffect(
                                    new PutCardFromVoidInLostPileEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }
        return null;
    }
}