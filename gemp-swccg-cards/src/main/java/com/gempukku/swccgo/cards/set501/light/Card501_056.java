package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.MoveAsReactEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromHandEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.MovedResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used
 * Title: Insertion Planning (V)
 */
public class Card501_056 extends AbstractUsedInterrupt {
    public Card501_056() {
        super(Side.LIGHT, 6, "Insertion Planning", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("The Rebellion employees every advantage it has over Imperial machines. A corps of well-trained scouts can elude detection in proper terrain.");
        setGameText("During opponent's deploy phase, deploy a spy from hand to a location you occupy. OR Move your spy (or [Endor] scout) of ability < 3 as a react. OR If your spy or scout is in battle, opponent's total battle destiny is -2.");
        addIcons(Icon.DEATH_STAR_II);
        setTestingText("[Set 19] Insertion Planning (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        final String opponent = game.getOpponent(playerId);

        if (GameConditions.isDuringOpponentsPhase(game, playerId, Phase.DEPLOY)
            && GameConditions.hasInHand(game, playerId, Filters.spy)
            && GameConditions.canSpotLocation(game, Filters.and(Filters.occupies(playerId), Filters.canBeTargetedBy(self)))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Deploy spy from hand");
            action.appendTargeting(new ChooseCardFromHandEffect(action, playerId, Filters.spy) {
                @Override
                protected void cardSelected(SwccgGame game, final PhysicalCard selectedCard) {
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose location where spy will be deployed", Filters.occupies(playerId)) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.allowResponses("Deploy "+GameUtils.getCardLink(selectedCard)+" from hand to "+GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            PhysicalCard location = action.getPrimaryTargetCard(targetGroupId);
                                            action.appendEffect(
                                                    new DeployCardToLocationFromHandEffect(action, selectedCard, location, false, false));
                                        }
                                    });
                        }
                    });
                }
            });
            actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.your(self), Filters.or(Filters.spy, Filters.scout)))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Subtract 2 from opponent's total battle destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyTotalBattleDestinyEffect(action, opponent, -2));
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.battleInitiated(game, effectResult, opponent)
            || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent)) {

            Filter characterFilter = Filters.and(Filters.your(self), Filters.or(Filters.spy, Filters.scout), Filters.abilityLessThan(3),
                    Filters.canMoveAsReactAsActionFromOtherCard(self, false, GameConditions.additionalForceUseRequiredToPlayInterrupt(game, playerId, self), false));
            if (GameConditions.canTarget(game, self, characterFilter)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Move spy or scout as 'react'");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose spy (or scout) of ability < 3", characterFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId1, final PhysicalCard targetedCharacter) {
                                action.addAnimationGroup(targetedCharacter);
                                // Set secondary target filter(s)
                                action.addSecondaryTargetFilter(Filters.battleLocation);
                                // Allow response(s)
                                action.allowResponses("Move " + GameUtils.getCardLink(targetedCharacter) + " as a 'react'",
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the final targeted card(s)
                                                PhysicalCard finalCharacter = action.getPrimaryTargetCard(targetGroupId1);
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new MoveAsReactEffect(action, finalCharacter, false));
                                            }
                                        }
                                );
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}