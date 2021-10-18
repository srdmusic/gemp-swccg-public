package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceDrainEffect;
import com.gempukku.swccgo.cards.effects.InsteadOfFiringWeaponEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.*;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Walker Barrage (V)
 */
public class Card501_023 extends AbstractUsedOrLostInterrupt {
    public Card501_023() {
        super(Side.DARK, 5, Title.Walker_Barrage);
        setVirtualSuffix(true);
        setLore("Before an AT-AT's troops can disembark to engage the enemy, the walker must first destroy the Rebel traitors' defensive emplacements.");
        setGameText("USED: During a battle at a site, instead of firing one of your vehicle weapons, cause one opponent's character present to be power -4 until end of turn. \n" +
                "LOST: If you occupy a site with an AT-AT, cancel a Force drain at a related site.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        setTestingText("Walker Barrage (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.isDuringBattleAt(game, Filters.site)
            && GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.opponents(playerId), Filters.character, Filters.presentInBattle))) {
            List<PhysicalCard> validVehicleWeapons = new LinkedList<>();
            final Collection<PhysicalCard> cardsInBattle = Filters.filterActive(game, self, Filters.participatingInBattle);
            for (PhysicalCard cardInBattle : cardsInBattle) {
                if (Filters.and(Filters.your(self), Filters.vehicle_weapon, Filters.canBeFiredForFreeAt(self, 0, Filters.in(cardsInBattle))).accepts(game, cardInBattle)) {
                    validVehicleWeapons.add(cardInBattle);
                }
            }
            if (!validVehicleWeapons.isEmpty()) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Make a character power -4");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose vehicle weapon", Filters.in(validVehicleWeapons)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId1, final PhysicalCard weapon) {
                                action.appendTargeting(
                                        new TargetCardOnTableEffect(action, playerId, "Choose target", Filters.and(Filters.opponents(playerId), Filters.character, Filters.presentInBattle)) {
                                            @Override
                                            protected void cardTargeted(final int targetGroupId2, PhysicalCard target) {
                                                action.addAnimationGroup(weapon);
                                                action.addAnimationGroup(target);
                                                // Allow response(s)
                                                action.allowResponses("Reduce " + GameUtils.getCardLink(target) + "'s power by 4 instead of firing " + GameUtils.getCardLink(weapon),
                                                        new RespondablePlayCardEffect(action) {
                                                            @Override
                                                            protected void performActionResults(Action targetingAction) {
                                                                // Get the targeted card(s) from the action using the targetGroupId.
                                                                // This needs to be done in case the target(s) were changed during the responses.
                                                                PhysicalCard finalWeapon = action.getPrimaryTargetCard(targetGroupId1);
                                                                PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId2);

                                                                // Perform result(s)
                                                                action.appendEffect(
                                                                        new InsteadOfFiringWeaponEffect(action, finalWeapon,
                                                                                new ModifyPowerUntilEndOfTurnEffect(action, finalTarget, -4)));
                                                            }
                                                        }
                                                );
                                            }
                                        });
                            }
                        });
                return Collections.singletonList(action);
            }
        }

        return null;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.forceDrainInitiatedAt(game, effectResult, Filters.relatedSiteTo(self, Filters.and(Filters.site, Filters.occupiesWith(playerId, self, Filters.AT_AT))))
                && GameConditions.canCancelForceDrain(game, self)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Cancel Force drain");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new CancelForceDrainEffect(action));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}