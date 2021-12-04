package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.MayNotFireWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.HitResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Glancing Blow (V)
 */
public class Card501_019 extends AbstractUsedOrLostInterrupt {
    public Card501_019() {
        super(Side.LIGHT, 3, "Glancing Blow", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("It had been decades since Vader had felt the sting of an enemy's blade.");
        setGameText("USED: If a lightsaber swung by your non-[Permanent Weapon] character just hit an opponent's character of equal or greater ability, opponent's character may not fire weapons this turn (if 'hit' by Luke, character is also power -3). LOST: Cancel the game text of a 'hit' character.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_17);
        setTestingText("[Set 18] Glancing Blow (V)");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        final int gameTextSourceCardId = self.getCardId();
        final String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (TriggerConditions.justHitBy(game, effectResult, Filters.and(Filters.opponents(self), Filters.character), Filters.lightsaber, Filters.and(Filters.your(self), Filters.not(Icon.PERMANENT_WEAPON), Filters.character))) {
            final PhysicalCard hitCharacter = ((HitResult) effectResult).getCardHit();
            final PhysicalCard firingWeapon = ((HitResult) effectResult).getCardFiringWeapon();

            if (game.getModifiersQuerying().getAbility(game.getGameState(), hitCharacter) >= game.getModifiersQuerying().getAbility(game.getGameState(), firingWeapon)
                && GameConditions.canTarget(game, self, hitCharacter)
                && GameConditions.canTarget(game, self, firingWeapon)) {

                final boolean hitByLuke = Filters.Luke.accepts(game, firingWeapon);

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Target a just hit character");
                action.setActionMsg("Prevent a just hit character from firing weapons" + (hitByLuke?" and make it power -3":""));

                action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target a just hit character", hitCharacter) {
                    @Override
                    protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                        action.allowResponses(new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action, new MayNotFireWeaponsModifier(self, finalTarget), "Prevents " + GameUtils.getCardLink(finalTarget) + " from firing weapons"));
                                if (hitByLuke) {
                                    action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action, new PowerModifier(self, finalTarget, -3), "Makes " + GameUtils.getCardLink(finalTarget) + " power -3"));
                                }
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
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        if (GameConditions.canTarget(game, self, Filters.hit_character)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Cancel game text of a hit character");
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose a hit character", Filters.hit_character) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard targetedCard) {
                            action.allowResponses(new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                    action.appendEffect(
                                            new CancelGameTextUntilEndOfBattleEffect(action, finalTarget)
                                    );
                                }
                            });
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }
}