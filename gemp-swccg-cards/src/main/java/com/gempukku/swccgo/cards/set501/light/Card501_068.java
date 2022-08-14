package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PayRelocateBetweenLocationsCostEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Lost Or Starting
 * Title: You Cannot Escape Your Destiny
 */
public class Card501_068 extends AbstractLostOrStartingInterrupt {
    public Card501_068() {
        super(Side.LIGHT, 4, "You Cannot Escape Your Destiny", Uniqueness.UNIQUE);
        setGameText("LOST: During your move phase, relocate Luke from a site to a battleground site. STARTING: If He Is The Chosen One on table, deploy His Destiny and two Effects that deploy for free and are always immune to Alter. Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_20);
        setTestingText("You Cannot Escape Your Destiny");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)

        if (GameConditions.canSpot(game, self, Filters.title(Title.He_Is_The_Chosen_One))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy His Destiny and up to 3 Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy His Destiny and up to three Effects that deploy on table, deploy for free, and are always immune to Alter from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.His_Destiny), true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.always_immune_to_Alter), 2, 2, true, false));
                            action.appendEffect(
                                    new PutCardFromVoidInReserveDeckEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }

        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        Filter lukeAtSiteWhoCanBeRelocated =  Filters.and(Filters.Luke, Filters.at(Filters.site), Filters.canBeRelocatedToLocation(Filters.battleground_site, true, 0));

        if (GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && GameConditions.canTarget(game, self, Filters.and(Filters.Luke, lukeAtSiteWhoCanBeRelocated)))
               {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Relocate Luke to another site");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Luke", lukeAtSiteWhoCanBeRelocated) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard characterToRelocate) {
                            action.appendTargeting(
                                    new TargetCardOnTableEffect(action, playerId, "Choose battleground site to relocate " + GameUtils.getCardLink(characterToRelocate) + " to", Filters.locationCanBeRelocatedTo(characterToRelocate, true, 0)) {
                                        @Override
                                        protected void cardTargeted(final int targetGroupId2, final PhysicalCard siteSelected) {
                                            action.addAnimationGroup(characterToRelocate);
                                            action.addAnimationGroup(siteSelected);
                                            // Pay cost(s)
                                            action.appendCost(
                                                    new PayRelocateBetweenLocationsCostEffect(action, playerId, characterToRelocate, siteSelected, 0));
                                            // Allow response(s)
                                            action.allowResponses("Relocate " + GameUtils.getCardLink(characterToRelocate) + " to " + GameUtils.getCardLink(siteSelected),
                                                    new RespondablePlayCardEffect(action) {
                                                        @Override
                                                        protected void performActionResults(Action targetingAction) {
                                                            // Get the targeted card(s) from the action using the targetGroupId.
                                                            // This needs to be done in case the target(s) were changed during the responses.
                                                            PhysicalCard finalCharacter = action.getPrimaryTargetCard(targetGroupId);
                                                            PhysicalCard finalSite = action.getPrimaryTargetCard(targetGroupId2);

                                                            // Perform result(s)
                                                            action.appendEffect(
                                                                    new RelocateBetweenLocationsEffect(action, finalCharacter, finalSite));
                                                        }
                                                    }
                                            );
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


}