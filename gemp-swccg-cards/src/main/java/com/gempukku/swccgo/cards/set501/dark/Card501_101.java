package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.MoveCardUsingLandspeedEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Set: Set 22
 * Type: Interrupt
 * Subtype: Lost
 * Title: Vader's Obsession (V)
 */
public class Card501_101 extends AbstractLostInterrupt {
    public Card501_101() {
        super(Side.DARK, 6, Title.Vaders_Obsession, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Vader sought to hunt down and destroy all Jedi. After completing the circle with Obi-Wan, he turned his attention to the young Skywalker.");
        setGameText("If Revenge Of The Sith and I Am Your Father on table: Take The Works into hand from Reserve Deck; reshuffle. OR Cancel Uncontrollable Fury. (Immune to Run Luke, Run!) OR During your battle phase move Vader (using landspeed) to a site you do not occupy.");
        setTestingText("Vader's Obsession (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        GameTextActionId gameTextActionId = GameTextActionId.VADERS_OBSESSION__UPLOAD_THE_WORKS;

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.Revenge_Of_The_Sith)
            && GameConditions.canSpot(game, self, Filters.I_Am_Your_Father)) {
            if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
                action.setText("Take The Works into hand from Reserve Deck");
                action.setActionMsg("Take Coruscant: The Works into hand from Reserve Deck.");
                // Allow response(s)
                action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.Coruscant_The_Works, true));
                        }
                    }
                );
                actions.add(action);
            }

            if (GameConditions.canTargetToCancel(game, self, Filters.Uncontrollable_Fury)) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                // Build action using common utility
                action.isImmuneTo(Title.Run_Luke_Run);
                CancelCardActionBuilder.buildCancelCardAction(action, Filters.Uncontrollable_Fury, Title.Uncontrollable_Fury);
                actions.add(action);
            }

            final Filter vaderFilter = Filters.or(Filters.Vader, Filters.grantedMayBeTargetedBy(self));

            // Check condition(s)
            if (GameConditions.isDuringYourPhase(game, self, Phase.BATTLE)
                && GameConditions.canTarget(game, self, vaderFilter)) {

                Set<PhysicalCard> charactersThatCanMove = new HashSet<>();
                for(PhysicalCard card: Filters.filterActive(game, self, vaderFilter)){
                    if (Filters.canSpotFromTopLocationsOnTable(game, Filters.and(Filters.canMoveToUsingLandspeed(playerId, card, false, false, false, 0, 0), Filters.not(Filters.occupies(playerId))))) {
                        charactersThatCanMove.add(card);
                    }
                }

                if (!charactersThatCanMove.isEmpty()){
                    final PlayInterruptAction action = new PlayInterruptAction(game, self);
                    action.setText("Move card using landspeed");
                    // Choose target(s)
                    action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose card to move", Filters.in(charactersThatCanMove)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard vader) {
                                action.addAnimationGroup(vader);
                                Collection<PhysicalCard> unoccupiedSites = Filters.filterTopLocationsOnTable(game, Filters.and(Filters.canMoveToUsingLandspeed(playerId, vader, false, false, false, 0, 0), Filters.not(Filters.occupies(playerId))));
                                action.appendTargeting(
                                    new TargetCardOnTableEffect(action, playerId, "Choose site to move to", Filters.in(unoccupiedSites)) {
                                        @Override
                                        protected void cardTargeted(final int targetGroupId2, PhysicalCard location) {
                                            action.addAnimationGroup(location);
                                            action.allowResponses("Move " + GameUtils.getCardLink(vader) + " to " + GameUtils.getCardLink(location),
                                                new RespondablePlayCardEffect(action) {
                                                    @Override
                                                    protected void performActionResults(Action targetAction) {
                                                        PhysicalCard finalVader = action.getPrimaryTargetCard(targetGroupId);
                                                        PhysicalCard finalLocation = action.getPrimaryTargetCard(targetGroupId2);
                                                        action.appendEffect(new MoveCardUsingLandspeedEffect(action, playerId, finalVader, false, finalLocation));
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
            }
        }
        return actions;
    }
}