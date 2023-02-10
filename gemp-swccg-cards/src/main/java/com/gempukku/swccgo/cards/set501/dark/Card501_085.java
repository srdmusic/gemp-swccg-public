package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.LimitForceLossFromForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.LimitForceLossFromInsertCardModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Defensive Shield
 * Title: Resistance (V)
 */
public class Card501_085 extends AbstractDefensiveShield {
    public Card501_085() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Resistance, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Oola had to choose between giving in to Jabba's constant advances or resisting him and inciting his wrath.");
        setGameText("Plays on table. May lose 2 Force to place an Undercover droid of play; opponent may take a card into hand from Lost Pile. While you occupy 3 battlegrounds (or opponent occupies none) you lose no more than 2 Force from each Force drain or 'insert' card.");
        addIcons(Icon.REFLECTIONS_III, Icon.VIRTUAL_SET_21);
        setTestingText("Resistance (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Condition condition = new OrCondition(new OccupiesCondition(playerId, 3, Filters.battleground),
                new NotCondition(new OccupiesCondition(opponent, Filters.battleground)));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new LimitForceLossFromForceDrainModifier(self, condition, 2, playerId));
        modifiers.add(new LimitForceLossFromInsertCardModifier(self, condition, 2, playerId));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(playerId);

        if (GameConditions.canTarget(game, self, SpotOverride.INCLUDE_UNDERCOVER, TargetingReason.TO_BE_PLACED_OUT_OF_PLAY, Filters.and(Filters.undercover_spy, Filters.droid))
                && GameConditions.hasLostPile(game, opponent)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Place an Undercover droid out of play");
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target an Undercover droid to place it out of play", SpotOverride.INCLUDE_UNDERCOVER, TargetingReason.TO_BE_PLACED_OUT_OF_PLAY, Filters.and(Filters.undercover_spy, Filters.droid)) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.appendCost(
                            new LoseForceEffect(action, playerId, 2));
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard undercoverDroid = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(
                                    new PlaceCardOutOfPlayFromTableEffect(action, undercoverDroid));
                            action.appendEffect(
                                    new PlayoutDecisionEffect(action, opponent, new YesNoDecision("Take a card into hand from Lost Pile?") {
                                @Override
                                protected void yes() {
                                    action.appendEffect(
                                            new TakeCardIntoHandFromLostPileEffect(action, opponent, Filters.any, false));
                                }
                            }));
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }

        return null;
    }
}