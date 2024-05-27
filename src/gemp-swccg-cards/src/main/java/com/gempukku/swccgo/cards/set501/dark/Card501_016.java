package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromOutsideTheGameEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Interrupt
 * Subtype: Used
 * Title: Put All Sections On Alert (V)
 */

public class Card501_016 extends AbstractUsedInterrupt {
    public Card501_016() {
        super(Side.DARK, 6, Title.Put_All_Sections_On_Alert, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("We have an emergency alert in detention block AA-twenty-three");
        setGameText("Prevent a character from moving (except during owner's move phase) until end of turn. OR Take [Set 0] Imperial Decree into hand from Reserve Deck; reshuffle. OR Cancel Jedi Presence or Rebel Ambush. OR Once per game, deploy a Lift Tube from outside your deck.");
        setVirtualSuffix(true);
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_23);
        setTestingText("Put All Sections On Alert (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        GameTextActionId gameTextActionId = GameTextActionId.PUT_ALL_SECTIONS_ON_ALERT__DOWNLOAD_LIFT_TUBE;
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.isDuringYourPhase(game, playerId, Phase.DEPLOY)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Deploy Lift Tube from outside your deck.");
            action.appendUsage(
                    new OncePerGameEffect(action));
            action.allowResponses("deploy a Lift Tube from outside your deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                new DeployCardFromOutsideTheGameEffect(action, Filters.Lift_Tube, 0));
                        }
                    }
            );
            actions.add(action);
        }

        GameTextActionId gameTextActionId1 = GameTextActionId.PUT_ALL_SECTIONS_ON_ALERT__DOWNLOAD_IMPERIAL_DECREE;

        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId1)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId1);
            action.allowResponses("Take [Set 0] Imperial Decree into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Filters.icon(Icon.VIRTUAL_SET_0), Filters.Imperial_Decree), true));
                        }
                    }
            );
            actions.add(action);
        }

        if (GameConditions.canTargetToCancel(game, self, Filters.Jedi_Presence)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Jedi_Presence, Title.Jedi_Presence);
            actions.add(action);
        }
        
        if (GameConditions.canTargetToCancel(game, self, Filters.Rebel_Ambush)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Rebel_Ambush, Title.Rebel_Ambush);
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Jedi_Presence, Filters.Rebel_Ambush))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }
        return actions;
    }
}
