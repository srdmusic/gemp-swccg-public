package com.gempukku.swccgo.cards.set5.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardOfForcePileAndReserveDeckAndUsedPileAndReturnOneCardToEachEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.InitiateBattleAction;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.PassthruEffect;

import java.util.*;


/**
 * Set: Cloud City
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Captive Fury
 */
public class Card5_036 extends AbstractUsedOrLostInterrupt {
    public Card5_036() {
        super(Side.LIGHT, 4, Title.Captive_Fury, Uniqueness.UNIQUE, ExpansionSet.CLOUD_CITY, Rarity.U);
        setLore("Chewie's life debt to Han forced him to act, retaliating unexpectedly against his captors.");
        setGameText("USED: Cancel Force drain bonus from IT-O this turn.  LOST: During your battle phase, any of your escorted captives at same site may initiate and participate in one battle (they may not use weapons or devices and you may not voluntarily forfeit or relocate them).");
        addIcons(Icon.CLOUD_CITY);
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(String playerId, SwccgGame game, final PhysicalCard self) {
        // Check condition(s)

        if (GameConditions.isDuringYourPhase(game, self, Phase.BATTLE)) {
            final var yourEscortedCaptive = Filters.and(Filters.escortedCaptive, Filters.your(playerId));
            var validCaptives = getBattleEligibleEscortedCaptives(game, self, yourEscortedCaptive);
            if(validCaptives == null || validCaptives.isEmpty())
                return null;


            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Initiate battle with escorted captives");
            action.setActionMsg("Initiate battle with escorted captives");

            var selectedCaptives = new ArrayList<PhysicalCard>();

            var chooseCaptives = chooseCaptives(game, playerId, self, action, validCaptives, null, selectedCaptives);
            action.appendTargeting(chooseCaptives);

            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Update usage limit(s)
                            action.addAnimationGroup(selectedCaptives);
                            var firstCaptive = selectedCaptives.getFirst();
                            var location = firstCaptive.getAttachedTo().getAtLocation();
                            action.setActionMsg("Have " + GameUtils.getCardLink(firstCaptive) + " initiate a battle");
                            action.appendEffect(
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            for(var captive : selectedCaptives) {
                                                game.getGameState().moveCardToLocation(captive, location);
                                            }
                                        }
                                    }
                            );
                            initiateBattleWithCaptives(game, playerId, self, action, location, selectedCaptives);
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    private List<PhysicalCard> getBattleEligibleEscortedCaptives(SwccgGame game, PhysicalCard self, Filter filter) {
        var validCaptives = new ArrayList<PhysicalCard>();

        var captives = Filters.filterActive(game, self, SpotOverride.INCLUDE_CAPTIVE, filter);
        return captives.stream().filter(card -> !GameConditions.hasParticipatedInBattleThisTurn(game, card)).toList();
    }

    /**
     * We want to have the player choose 1 of the available captives, and then after that choose 0 or more other
     * escorted captives at the same site.
     */
    private static ChooseCardOnTableEffect chooseCaptives(final SwccgGame game, final String playerId, final PhysicalCard self,
            final PlayInterruptAction action,  final List<PhysicalCard> initialTargets, PhysicalCard location, final List<PhysicalCard> selectedCaptives) {

        var unselectedSameLocationCaptives = Filters.and(Filters.in(initialTargets),
                Filters.not(Filters.in(selectedCaptives)));
        int minChoices = 0; // optional
        if(selectedCaptives.isEmpty()) { //first time through
            minChoices = 1;
        }
        else {
            // We only permit captives at the same site as our initial choice which have not already been selected.
            unselectedSameLocationCaptives = Filters.and(Filters.in(initialTargets),
                    Filters.attachedTo(Filters.atSameLocation(location)),
                    Filters.not(Filters.in(selectedCaptives)));
            var otherCaptives = Filters.filterActive(game, self, SpotOverride.INCLUDE_CAPTIVE, unselectedSameLocationCaptives);
            if (otherCaptives.isEmpty())
                return null;
        }

        var chooseInitialCaptive = new ChooseCardOnTableEffect(action, playerId, "Choose escorted captives to participate in battle",
                SpotOverride.INCLUDE_CAPTIVE, unselectedSameLocationCaptives, minChoices) {
            @Override
            protected void cardSelected(PhysicalCard captive) {
                selectedCaptives.add(captive);
                var captiveLocation = captive.getCardAttachedToAtLocation();

                action.appendEffect(
                        new PassthruEffect(action) {
                            @Override
                            protected void doPlayEffect(SwccgGame game) {
                                var selectedCaptives = Collections.singletonList(captive);

                                // Optionally, add on another "choose card" instance
                                var bonusEffect = chooseCaptives(game, playerId, self, action, initialTargets, captiveLocation, selectedCaptives);
                                if (bonusEffect != null) {
                                    action.appendEffect(bonusEffect);
                                }
                            }
                        }
                );
            }
        };
        return chooseInitialCaptive;
    }


    private static void initiateBattleWithCaptives(final SwccgGame game, final String playerId, final PhysicalCard self,
            final PlayInterruptAction action, final PhysicalCard location, final List<PhysicalCard> chosenCaptives) {

        var battleAction = new InitiateBattleAction(playerId, location, false) {
            @Override
            public List<Modifier> getAddedModifiers() {
                List<Modifier> modifiers = new LinkedList<>();

                modifiers.add(new MayNotUseWeaponsModifier(self, Filters.in(chosenCaptives)));
                modifiers.add(new MayNotUseDevicesModifier(self, Filters.in(chosenCaptives)));
                modifiers.add(new MayNotBeForfeitedInBattleModifier(self, Filters.and(Filters.in(chosenCaptives),Filters.not(Filters.hit))));
                modifiers.add(new MayNotMoveAwayFromLocationModifier(self, Filters.in(chosenCaptives), location));

                return modifiers;
            }
        };

        action.appendEffect(new StackActionEffect(action, battleAction));
    }
}