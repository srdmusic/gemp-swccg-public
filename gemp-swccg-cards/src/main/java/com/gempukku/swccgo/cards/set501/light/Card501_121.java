package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotCloakModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotFireWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotForceDrainAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Transmission Terminated (V)
 */
public class Card501_121 extends AbstractUsedOrLostInterrupt {
    public Card501_121() {
        super(Side.LIGHT, 5, Title.Transmission_Terminated, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("After the mission, the Death Squadron HoloNet communications system reported fifteen system errors: ten computer malfunctions, four power failures and one asteroid.");
        setGameText("USED: Target a starfighter. Target may not ‘cloak,’ fire a weapon, or initiate a Force drain alone for remainder of turn. " +
                "LOST: Cancel a hologram. OR Suspend the game text of Emperor's Power or an Admiral’s Orders until start of your turn.");
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_23);
        setTestingText("Transmission Terminated (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        if (GameConditions.canTarget(game, self, Filters.starship)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Affect starship");

            // Allow response(s)
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target a starship", Filters.starship) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(null,
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    // Perform result(s)
                                    PhysicalCard finalCard = action.getPrimaryTargetCard(targetGroupId);
                                    action.appendEffect(
                                            new AddUntilEndOfTurnModifierEffect(action,
                                                    new MayNotFireWeaponsModifier(self, finalCard),
                                                    "Prevents " + GameUtils.getCardLink(finalCard) + " from firing weapons"));
                                    action.appendEffect(
                                            new AddUntilEndOfTurnModifierEffect(action,
                                                    new MayNotCloakModifier(self, finalCard),
                                                    "Prevents " + GameUtils.getCardLink(finalCard) + " from 'cloaking'"));
                                    action.appendEffect(
                                            new AddUntilEndOfTurnModifierEffect(action,
                                                    new MayNotForceDrainAtLocationModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.alone, finalCard)), finalCard.getOwner()),
                                                    "Prevents " + GameUtils.getCardLink(finalCard) + " from force draining alone"));
                                }
                            }
                    );
                }
            });
            actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.hologram)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.hologram, "hologram");
            actions.add(action);
        }

        if (GameConditions.canSpot(game, self, Filters.Admirals_Order) ||
                GameConditions.canSpot(game, self, Filters.Emperors_Power)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Suspend card for remainder of turn");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target card to suspend", Filters.or(Filters.Admirals_Order, Filters.Emperors_Power)) {
                @Override
                protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                    // Allow response(s)
                    action.allowResponses("Suspend " + GameUtils.getCardLink(targetedCard) + " for remainder of turn",
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    action.appendEffect(
                                            new AddUntilEndOfTurnModifierEffect(action,
                                                    new SuspendsCardModifier(self, targetedCard),
                                                    "Suspends " + GameUtils.getCardLink(targetedCard))
                                    );
                                }
                            }
                    );
                }
            });
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.hologram)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }
}
