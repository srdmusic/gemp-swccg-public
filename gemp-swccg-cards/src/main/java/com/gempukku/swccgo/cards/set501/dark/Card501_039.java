package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ExchangeCardFromLostPileWithStackedCardEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Do You Stand By Your Work?
 */
public class Card501_039 extends AbstractUsedOrLostInterrupt {
    public Card501_039() {
        super(Side.DARK, 4, "Do You Stand By Your Work?", Uniqueness.UNIQUE);
        setGameText("USED: If opponent is about to cancel and redraw a destiny, it is canceled instead. LOST: Once per game, if your objective just canceled a battle, none of the opponent's cards participating in that battle may move for remainder of turn.");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("Do You Stand By Your Work?");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.THRAWN_PINCER__RELOCATE_STAR_DESTROYER;

        if (effectResult.getType() == EffectResult.Type.ARTWORK_CARD_REVEALED
                && GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.isDuringBattleAt(game, Filters.system)
                && GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Filters.Star_Destroyer, Filters.canBeTargetedBy(self), Filters.canBeRelocatedToLocation(Filters.battleLocation, true, 0)))) {

            PhysicalCard artwork = ((ArtworkCardRevealedResult) effectResult).getCard();

            if (artwork != null
                    && Filters.starship.accepts(game, artwork)) {

                final PhysicalCard battleLocation = Filters.findFirstFromTopLocationsOnTable(game, Filters.battleLocation);
                if (battleLocation != null) {
                    final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
                    action.setText("Relocate Star Destroyer");

                    action.appendUsage(
                            new OncePerGameEffect(action));
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target a Star Destroyer to relocate to " + GameUtils.getCardLink(battleLocation), Filters.and(Filters.your(self), Filters.Star_Destroyer, Filters.canBeRelocatedToLocation(battleLocation, true, 0))) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.allowResponses(new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard starDestroyer = action.getPrimaryTargetCard(targetGroupId);
                                    action.appendEffect(new RelocateBetweenLocationsEffect(action, starDestroyer, battleLocation));
                                }
                            });
                        }
                    });

                    return Collections.singletonList(action);
                }
            }
        }

        return null;
    }
}