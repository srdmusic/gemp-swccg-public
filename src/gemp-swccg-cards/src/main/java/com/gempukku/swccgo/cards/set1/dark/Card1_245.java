package com.gempukku.swccgo.cards.set1.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.RelocateFromLocationToStarshipOrVehicle;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardsOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Premiere
 * Type: Interrupt
 * Subtype: Used
 * Title: Evacuate?
 */
public class Card1_245 extends AbstractUsedInterrupt {
    public Card1_245() {
        super(Side.DARK, 6, "Evacuate?", Uniqueness.UNRESTRICTED, ExpansionSet.PREMIERE, Rarity.U2);
        setLore("Escape pods are on many starships allowing those in peril to flee, an act considered cowardly by Imperial officers. 'We've analyzed their attack, sir, and there is a danger.'");
        setGameText("If your capital starship is about to be lost, unless Tarkin aboard, relocate your characters aboard to any one planet site or to one of your capital starships.");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        final Filter ship = Filters.and(Filters.your(self), Filters.capital_starship, Filters.not(Filters.hasAboard(self,Filters.Tarkin)));

        // Check condition(s)
        if (TriggerConditions.isAboutToBeLost(game, effectResult, ship)
                || TriggerConditions.isAboutToBeForfeitedToLostPile(game, effectResult, ship)) {

            final AboutToLeaveTableResult aboutToLeaveTableResult = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard cardToBeLost = aboutToLeaveTableResult.getCardAboutToLeaveTable();

            final Filter destinationSiteFilter = Filters.planet_site;
            final Filter destinationShipFilter = Filters.and(Filters.your(self), Filters.capital_starship, Filters.not(cardToBeLost)); //change to only allow eligible
            final Filter destinationFilter = Filters.or(destinationSiteFilter, destinationShipFilter);

            if(GameConditions.canSpot(game, self, destinationFilter)) {
                final Filter aboardFilter = Filters.and(Filters.your(self), Filters.character, Filters.aboard(cardToBeLost), Filters.canBeTargetedBy(self));
                final int charactersAboardCount = Filters.countActive(game, self, aboardFilter);
                final int astromechsAboardCount = Filters.countActive(game, self, Filters.and(aboardFilter, Filters.astromech_droid));

                Collection<PhysicalCard> possibleSites = new HashSet<>();
                for (PhysicalCard card : Filters.filterActive(game, self, aboardFilter)) {
                    possibleSites.addAll(Filters.filterTopLocationsOnTable(game, Filters.and(destinationSiteFilter, Filters.locationCanBeRelocatedTo(card, 0))));
                }

                //potential ships must be able to hold everyone from the aboardFilter
                Collection<PhysicalCard> possibleShips = new HashSet<>();
                for (PhysicalCard potentialShip : Filters.filterActive(game, self, destinationShipFilter)) {
                    /// consider reworking with a separate method that accepts: Collection<PhysicalCard> cardsAboard and PhysicalCard targetShip

                    //a few key assumptions are made:
                    // - astromechs cannot be pilots
                    // - pilot limitations (ex: must be alien) apply to all pilot capacity
                    // - no passenger limitations (ex: must be alien)

                    //record capacity on the potential ship
                    int availablePilotCapacity = game.getGameState().getAvailablePilotCapacity(game.getModifiersQuerying(), potentialShip, self);
                    int availablePassengerCapacity = game.getGameState().getAvailablePassengerCapacity(game.getModifiersQuerying(), potentialShip, self);
                    int availableAstromechCapacity = game.getGameState().getAvailablePassengerCapacityForAstromech(game.getModifiersQuerying(), potentialShip, self);

                    int eligiblePilotsAboard = Filters.countActive(game, self, Filters.and(aboardFilter, potentialShip.getBlueprint().getValidPilotFilter(playerId, game, potentialShip, false)));

                    //with optimal usage of pilot and astromech capacity...
                    int maxPilotCapacityToFill = Math.min(availablePilotCapacity,eligiblePilotsAboard);
                    int maxAstromechCapacityToFill = Math.min(availableAstromechCapacity,astromechsAboardCount);
                    //...check passenger capacity needed to hold the rest
                    int mandatoryPassengersAboard = charactersAboardCount - maxPilotCapacityToFill - maxAstromechCapacityToFill;
                    if(availablePassengerCapacity >= mandatoryPassengersAboard) {
                        possibleShips.add(potentialShip);
                    }
                    /// may need to somehow use Filters.canBeRelocated (maybe a new/modified version?)
                    /// may need to somehow use getValidPassengerFilter ?
                }

                Collection<PhysicalCard> possibleSitesAndShips = new HashSet<>();
                possibleSitesAndShips.addAll(possibleSites);
                possibleSitesAndShips.addAll(possibleShips);

                if (!possibleSitesAndShips.isEmpty()) {

                    final PlayInterruptAction action = new PlayInterruptAction(game, self);
                    action.setText("Relocate characters to a planet site or capital ship");

                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose planet site or ship to relocate characters", Filters.in(possibleSitesAndShips)) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            // Allow response(s)
                            action.allowResponses(
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            PhysicalCard destination = action.getPrimaryTargetCard(targetGroupId);
                                            if (possibleSites.contains(destination)) {
                                                Collection<PhysicalCard> toRelocate = Filters.filterActive(game, self, Filters.and(aboardFilter, Filters.canBeRelocatedToLocation(destination, 0)));
                                                action.addAnimationGroup(toRelocate);
                                                action.appendEffect(new RelocateBetweenLocationsEffect(action, toRelocate, destination));
                                            }
                                            else if(possibleShips.contains(destination)) {
                                                /// collect capacity stats again, here
                                                final int availablePilotCapacity = game.getGameState().getAvailablePilotCapacity(game.getModifiersQuerying(), destination, self);
                                                final int availablePassengerCapacity = game.getGameState().getAvailablePassengerCapacity(game.getModifiersQuerying(), destination, self);
                                                final int availableAstromechCapacity = game.getGameState().getAvailablePassengerCapacityForAstromech(game.getModifiersQuerying(), destination, self);

                                                //final int eligiblePilotsAboard = Filters.countActive(game, self, Filters.and(aboardFilter, destination.getBlueprint().getValidPilotFilter(playerId, game, destination, false)));

                                                //with optimal usage of pilot and astromech capacity, check passenger capacity needed to hold the rest
                                                //final int maxPilotCapacityToFill = Math.min(availablePilotCapacity,eligiblePilotsAboard);
                                                final int maxAstromechCapacityToFill = Math.min(availableAstromechCapacity,astromechsAboardCount);
                                                //final int mandatoryPassengersAboard = charactersAboardCount - maxPilotCapacityToFill - maxAstromechCapacityToFill;

                                                int minAboardThatMustBePilots = charactersAboardCount - availablePassengerCapacity - maxAstromechCapacityToFill;
                                                if(minAboardThatMustBePilots < 0) minAboardThatMustBePilots = 0;

                                                /// Figure out which characters can be relocated to ship as pilots
                                                Collection<PhysicalCard> validCharactersToRelocateAsPilots = new LinkedList<PhysicalCard>();
                                                Collection<PhysicalCard> validCharactersToRelocateAsPassengers = new LinkedList<PhysicalCard>();
                                                for (PhysicalCard characterToRelocate : Filters.filterActive(game, self, aboardFilter)) {
                                                    if (Filters.and(aboardFilter, destination.getBlueprint().getValidPilotFilter(playerId, game, destination, false)).accepts(game, characterToRelocate))
                                                        validCharactersToRelocateAsPilots.add(characterToRelocate);
                                                    if (Filters.and(aboardFilter, destination.getBlueprint().getValidPassengerFilter(playerId, game, destination, false)).accepts(game, characterToRelocate))
                                                        validCharactersToRelocateAsPassengers.add(characterToRelocate);
                                                }

                                                if((availablePilotCapacity > 0) && !validCharactersToRelocateAsPilots.isEmpty()) {
                                                    String choiceText = "Choose characters to relocate as pilots";
                                                    if(minAboardThatMustBePilots > 0) choiceText = choiceText + " (must choose at least " + minAboardThatMustBePilots + ")";
                                                    /// could add text about maximum... rework as (min = x, max = y)?
                                                    action.appendTargeting(
                                                            new TargetCardsOnTableEffect(action, playerId, choiceText, minAboardThatMustBePilots, availablePilotCapacity, Filters.in(validCharactersToRelocateAsPilots)) {
                                                                @Override
                                                                protected void cardsTargeted(final int targetGroupId, final Collection<PhysicalCard> cardsToRelocate) {
                                                                    final GameState gameState = game.getGameState();
                                                                    action.addAnimationGroup(cardsToRelocate);
                                                                    action.addAnimationGroup(destination);
                                                                    for (PhysicalCard cardToRelocate : cardsToRelocate) {
                                                                        /// relocate each pilot selected pilot, one at a time
                                                                        action.appendEffect(
                                                                                new RelocateFromLocationToStarshipOrVehicle(action, cardToRelocate, destination, true, self));
                                                                    }
                                                                    validCharactersToRelocateAsPassengers.removeAll(cardsToRelocate);

                                                                    if(!validCharactersToRelocateAsPassengers.isEmpty()) {
                                                                        action.addAnimationGroup(validCharactersToRelocateAsPassengers);
                                                                        for(PhysicalCard cardToRelocate : validCharactersToRelocateAsPassengers) {
                                                                            /// relocate all other characters as passengers, one at a time
                                                                            action.appendEffect(
                                                                                    new RelocateFromLocationToStarshipOrVehicle(action, cardToRelocate, destination, false, self));
                                                                        }
                                                                    }

                                                                }
                                                            }
                                                    );
                                                }
                                                else if(!validCharactersToRelocateAsPassengers.isEmpty()) {
                                                    action.addAnimationGroup(validCharactersToRelocateAsPassengers);
                                                    for(PhysicalCard cardToRelocate : validCharactersToRelocateAsPassengers) {
                                                        action.appendEffect(
                                                                new RelocateFromLocationToStarshipOrVehicle(action, cardToRelocate, destination, false, self));
                                                    }
                                                }

                                            /// other things to investigate: paying relocation costs
                                            /// recording regular move like in RelocateBetweenLocationsEffect?
                                            }
                                            else {
                                                //crash message? should be impossible
                                            }
                                        }
                                    }
                            );
                        }
                    });
                    return Collections.singletonList(action);
                }
            }
        }
        return null;
    }
}