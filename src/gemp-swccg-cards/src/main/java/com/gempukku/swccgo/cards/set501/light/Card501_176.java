package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalBattleDestinyEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.MovedResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost Interrupt
 * Title: Insertion Planning (V)
 */
public class Card501_176 extends AbstractUsedInterrupt {
    public Card501_176() {
        super(Side.LIGHT, 6, "Insertion Planning", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("The Rebellion employees every advantage it has over Imperial machines. A corps of well-trained scouts can elude detection in proper terrain.");
        setGameText("USED: Suspend Ominous Rumors or They Will Never Stop Hunting You for remainder of turn. LOST: If your scout is in battle at an opponent's site subtract 3 from opponent's total battle destiny. OR [UPLOAD] [Endor] Chewie, Lak Sivrak, or Orrimaarko.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Insertion Planning (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.your(self), Filters.scout))
                && GameConditions.isDuringBattleAt(game, Filters.exterior_planet_site)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Subtract 3 from opponent's total battle destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyTotalBattleDestinyEffect(action, opponent, -3));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.movedToLocation(game, effectResult, Filters.open_vehicle, Filters.sameSiteAs(self, Filters.and(Filters.your(self), Filters.scout)))) {
            MovedResult movedResult = (MovedResult) effectResult;
            Filter characterFilter = Filters.none;
            Collection<PhysicalCard> movedCards = Filters.filter(movedResult.getMovedCards(), game, Filters.open_vehicle);
            for (PhysicalCard movedCard : movedCards) {
                characterFilter = Filters.or(characterFilter, Filters.aboardExceptRelatedSites(movedCard));
            }
            characterFilter = Filters.and(Filters.opponents(self), Filters.character, characterFilter);

            if (GameConditions.canTarget(game, self, characterFilter)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Target character aboard open vehicle");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose character", characterFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard targetedCard) {
                                action.addAnimationGroup(targetedCard);
                                // Allow response(s)
                                action.allowResponses("Place " + GameUtils.getCardLink(targetedCard) + " in Used Pile",
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the targeted card(s) from the action using the targetGroupId.
                                                // This needs to be done in case the target(s) were changed during the responses.
                                                final PhysicalCard finalTarget = targetingAction.getPrimaryTargetCard(targetGroupId);

                                                // Perform result(s)
                                                action.appendEffect(
                                                        new DrawDestinyEffect(action, playerId) {
                                                            @Override
                                                            protected Collection<PhysicalCard> getGameTextAbilityManeuverOrDefenseValueTargeted() {
                                                                return Collections.singletonList(finalTarget);
                                                            }
                                                            @Override
                                                            protected void destinyDraws(SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, Float totalDestiny) {
                                                                GameState gameState = game.getGameState();

                                                                float ability = game.getModifiersQuerying().getAbility(game.getGameState(), finalTarget);
                                                                gameState.sendMessage("Destiny: " + (totalDestiny != null ? GuiUtils.formatAsString(totalDestiny) : "Failed destiny draw"));
                                                                gameState.sendMessage("Ability: " + GuiUtils.formatAsString(ability));

                                                                if (((totalDestiny != null ? totalDestiny : 0) + 2) > ability) {
                                                                    gameState.sendMessage("Result: Succeeded");
                                                                    action.appendEffect(
                                                                            new PlaceCardInUsedPileFromTableEffect(action, finalTarget));
                                                                } else {
                                                                    gameState.sendMessage("Result: Failed");
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}