package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Icon;
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
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.PlayCardResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used
 * Title: I'd Just As Soon Kiss A Wookiee (V)
 */
public class Card501_005 extends AbstractUsedInterrupt {
    public Card501_005() {
        super(Side.DARK, 2, Title.Id_Just_As_Soon_Kiss_A_Wookiee, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'I can arrange that. You could USE a good kiss!'");
        setGameText("If opponent just deployed a character (except as a driver, pilot, or passenger), use 1 Force (free if a Wookiee on table and target is Leia). Opponent chooses: move target away to an adjacent site as a regular move for free, or return target to hand.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] I'd Just As Soon Kiss A Wookiee (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);

        final Filter targetFilter = Filters.and(Filters.character, Filters.not(Filters.or(Filters.piloting(Filters.any), Filters.driving(Filters.any), Filters.aboardAsPassenger(Filters.any))));
        final Filter targetForFreeIfWookieeOnTableFilter = Filters.and(targetFilter, Filters.Leia);

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, opponent, targetFilter)
                && (GameConditions.canUseForceToPlayInterrupt(game, playerId, self, 1)
                || (GameConditions.canSpot(game, self, Filters.Wookiee)
                    && TriggerConditions.justDeployed(game, effectResult, opponent, targetForFreeIfWookieeOnTableFilter)))) {

            final PlayCardResult playCardResult = (PlayCardResult) effectResult;
            final PhysicalCard location = playCardResult.getAtLocation();
            final PhysicalCard playedCard = playCardResult.getPlayedCard();
            final Filter possibleFilter = GameConditions.canUseForceToPlayInterrupt(game, playerId, self, 1) ? targetFilter : targetForFreeIfWookieeOnTableFilter;

            if (location != null
                    && GameConditions.canSpot(game, self, Filters.adjacentSite(location))
                    && possibleFilter.accepts(game, playedCard)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Opponent chooses to move " + GameUtils.getFullName(playedCard) + " or return it to hand");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose character",  Filters.and(playedCard, possibleFilter)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                                boolean targetForFree = GameConditions.canSpot(game, self, Filters.Wookiee) && targetForFreeIfWookieeOnTableFilter.accepts(game, targetedCard);

                                if (!targetForFree) {
                                    // Pay cost(s)
                                    action.appendCost(
                                            new UseForceEffect(action, playerId, 1));
                                }
                                // Allow response(s)
                                action.allowResponses("Opponent chooses to move " + GameUtils.getCardLink(targetedCard) + " or return it to hand",
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                PhysicalCard finalCard = action.getPrimaryTargetCard(targetGroupId);
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new ChooseToMoveAwayOrReturnToHandEffect(action, opponent, finalCard, true, Filters.adjacentSite(location)));
                                            }
                                        }
                                );
                            }

                            @Override
                            protected boolean getUseShortcut() {
                                return true;
                            }
                        }
                );
                return Collections.singletonList(action);

            }
        }
        return null;
    }
}