package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ResetPowerUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Eventually You'll Lose (V)
 */
public class Card501_206 extends AbstractUsedOrLostInterrupt {
    public Card501_206() {
        super(Side.LIGHT, 4, "Eventually You'll Lose", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("In the end, Watto finally came to understand the agony of defeat.");
        //setGameText("USED: If The Hyperdrive Generator's Gone on table, [upload] Skywalker Hut or Jar Jar. LOST: If opponent's card was just stacked on Credits Will Do Fine, for remainder of turn, your Force drains may not be reduced.");
        setGameText("USED: If The Hyperdrive Generator's Gone or We'll Need A New One on table, [upload] Jar Jar or Skywalker Hut. LOST: If you have won a Podrace, target a character (except Vader) in battle with [Tatooine] Anakin. Target is power = 0.");
        addIcons(Icon.TATOOINE, Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        setTestingText("Eventually You'll Lose");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.EVENTUALLY_YOULL_LOSE_V__UPLOAD_CARD;

        if (GameConditions.canSpot(game, self, Filters.or(Filters.The_Hyperdrive_Generators_Gone, Filters.Well_Need_A_New_One))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take card into hand from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Take Jar Jar or Skywalker Hut into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Jar_Jar, Filters.Skywalker_Hut), true));
                        }
                    }
            );
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        Filter tatooineAnakin = Filters.and(Icon.TATOOINE, Filters.Anakin, Filters.participatingInBattle);
        Filter targetFilter = Filters.and(Filters.character, Filters.not(Filters.Vader), Filters.participatingInBattle, Filters.with(self, tatooineAnakin));

        if (GameConditions.hasWonPodrace(game, playerId)
                && GameConditions.canTarget(game, self, targetFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
            action.setText("Make a character power = 0");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose character", targetFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Reset " + GameUtils.getCardLink(targetedCard) + "'s power to 0",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(final Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ResetPowerUntilEndOfTurnEffect(action, finalTarget, 0));
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