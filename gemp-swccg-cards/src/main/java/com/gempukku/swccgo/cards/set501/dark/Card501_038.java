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
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Thrawn Pincer
 */
public class Card501_038 extends AbstractUsedOrLostInterrupt {
    public Card501_038() {
        super(Side.DARK, 4, "Thrawn Pincer", Uniqueness.UNIQUE);
        setGameText("USED: Exchange a card stacked on Thrawn's Art Collection with a card in opponent’s Lost Pile. LOST: Once per game, during battle at a system, if you just revealed a starship as ‘artwork’, relocate a Star Destroyer on table to that system.");
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("Thrawn Pincer");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.THRAWN_PINCER__EXCHANGE_CARD;

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.Thrawns_Art_Collection, Filters.hasStacked(Filters.any)))
                && GameConditions.hasLostPile(game, opponent)
                && GameConditions.canSearchOpponentsLostPile(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Exchange card");
            // Choose target(s)
            action.allowResponses(new RespondablePlayCardEffect(action) {
                @Override
                protected void performActionResults(Action targetingAction) {
                    action.appendEffect(
                            new ExchangeCardFromLostPileWithStackedCardEffect(action, opponent, Filters.any, Filters.Thrawns_Art_Collection, Filters.any, true));
                }
            });
            actions.add(action);

        }

        return actions;
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
                            action.allowResponses("Relocate "+GameUtils.getCardLink(targetedCard)+" to "+GameUtils.getCardLink(battleLocation), new RespondablePlayCardEffect(action) {
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