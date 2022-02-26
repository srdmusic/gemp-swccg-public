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
import com.gempukku.swccgo.logic.effects.*;
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
 * Title: Understand Art, Understand A Species
 */
public class Card501_040 extends AbstractUsedOrLostInterrupt {
    public Card501_040() {
        super(Side.DARK, 4, "Understand Art, Understand A Species", Uniqueness.UNIQUE);
        setGameText("USED: Target a character in battle. Target and characters with a shared characteristic as target are power and forfeit -1. LOST: Once per game, during battle, If you just revealed a character as 'artwork' add one battle destiny (two if its species is participating in battle).");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("Understand Art, Understand A Species");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.Thrawns_Art_Collection, Filters.hasStacked(Filters.any)))
                && GameConditions.isDuringBattle(game)) {

            final int num = Filters.countStacked(game, Filters.stackedOn(self, Filters.Thrawns_Art_Collection));
            if (num>0) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Add "+num+" to total battle destiny");
                // Choose target(s)
                action.allowResponses(new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        action.appendEffect(new ModifyTotalBattleDestinyEffect(action, playerId, num));
                    }
                });
                actions.add(action);
            }
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