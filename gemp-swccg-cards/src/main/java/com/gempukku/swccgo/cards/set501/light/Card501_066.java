package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Lost
 * Title: Full Throttle (V)
 */
public class Card501_066 extends AbstractLostInterrupt {
    public Card501_066() {
        super(Side.LIGHT, 4, "Full Throttle", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Rebel pilots use visual scanning to supplement sensors for an edge against Imperial fighter pilots. Natural instincts allow lone Rebels to overcome superior numbers.");
        setGameText("If your Skywalker piloting a starfighter alone in battle, that starfighter is immune to attrition. If opponent has two or more starships there (or your [Skywalker] Epic Event on table), add that starfighter's maneuver to your total power.");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_18);
        setTestingText("Full Throttle (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        Filter filter = Filters.and(Filters.starfighter, Filters.alone, Filters.hasPiloting(self, Filters.and(Filters.your(self), Filters.Skywalker)), Filters.participatingInBattle, Filters.canBeTargetedBy(self));

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Target a starfighter");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose starfighter", filter) {
                        @Override
                        protected boolean getUseShortcut() {
                            return true;
                        }

                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Target " + GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddUntilEndOfTurnModifierEffect(action,
                                                            new ImmuneToAttritionModifier(self, finalTarget),
                                                            "Makes " + GameUtils.getCardLink(finalTarget) + " immune to attrition"));
                                            
                                            if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Epic_Event))
                                                    || GameConditions.canSpot(game, self, 2, Filters.and(Filters.opponents(self), Filters.starship, Filters.participatingInBattle))) {
                                                float maneuver = game.getModifiersQuerying().getManeuver(game.getGameState(), finalTarget);
                                                action.appendEffect(
                                                        new ModifyTotalPowerUntilEndOfBattleEffect(action, maneuver, playerId, "Adds "+maneuver+" to total power"));
                                            }
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