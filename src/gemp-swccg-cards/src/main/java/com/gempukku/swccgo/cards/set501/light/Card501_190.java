package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
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
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.FlipSingleSidedStackedCard;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.Action;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost
 * Title: Honoring What They Fight For
 */
public class Card501_190 extends AbstractLostInterrupt {
    public Card501_190() {
        super(Side.LIGHT, 5, "Honoring What They Fight For", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Luke's experience on Dagobah gave him great skill in using the Force. Vader had to keep his focus on Luke at all times, or face the consequences.");
        setGameText("At the start of your turn, if a [Cloud City] Rebel controls a battleground, turn a card stacked on Patience! face up. OR If a [Cloud City] Rebel in battle, add one battle destiny. OR Place a [Cloud City] Rebel (except Luke) out of play from hand to cancel all battle damage against you.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_25);
        setTestingText("Honoring What They Fight For");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        final Filter jediTestFaceDown = Filters.and(Filters.Jedi_Test, Filters.face_down);
        final Filter patienceWithJediTestStackedFaceDown = Filters.and(Filters.Patience, Filters.hasStacked(jediTestFaceDown));

        // Check condition(s)
        if (TriggerConditions.isStartOfYourTurn(game, effectResult, playerId)
                && GameConditions.controlsWith(game, self, playerId, Filters.battleground, Filters.and(Icon.CLOUD_CITY, Filters.Rebel))
                && GameConditions.canSpot(game, self, patienceWithJediTestStackedFaceDown)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setPerformingPlayer(playerId);
            action.setText("Turn Jedi Test face up");
            action.setActionMsg("Turn a Jedi Test on Patience! face up");
            action.appendTargeting(
                    new ChooseStackedCardEffect(action, playerId, patienceWithJediTestStackedFaceDown, jediTestFaceDown, false) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            // Perform result(s)
                            action.appendEffect(
                                new FlipSingleSidedStackedCard(action, selectedCard));
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Icon.CLOUD_CITY, Filters.Rebel))
                && GameConditions.canAddBattleDestinyDraws(game, self)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Add one battle destiny");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new AddBattleDestinyEffect(action, 1));
                            }
                        }
                );
                actions.add(action);
        }
        return actions;
    }

}
