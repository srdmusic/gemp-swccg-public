package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.DoubledCondition;
import com.gempukku.swccgo.cards.effects.usage.NumTimesPerGameEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.cards.evaluators.DivideEvaluator;
import com.gempukku.swccgo.cards.evaluators.HereEvaluator;
import com.gempukku.swccgo.cards.evaluators.MultiplyEvaluator;
import com.gempukku.swccgo.cards.evaluators.NegativeEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Character
 * Subtype: Alien
 * Title: Tonnika Sisters (V)
 */
public class Card501_133 extends AbstractAlien {
    public Card501_133() {
        super(Side.DARK, 2, 2, 2, 2, 2, Title.Tonnika_Sisters, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setComboCard(true);
        setLore("Twins. Thieves. Con artists. Spies. Swindlers. Double agents. Brea and Senni use their natural charm to sway the unwary on the fringe of society.");
        setGameText("Assassins. While at Cantina, opponent’s Force generation here is -1 for each of your sets of two aliens here. Once per game, may choose: add 2 to the destiny of an alien just drawn for destiny or cause each player to activate up to 2 Force.");
        addIcon(Icon.WARRIOR, 2);
        addKeywords(Keyword.SPY, Keyword.THIEF, Keyword.FEMALE, Keyword.ASSASSIN);
        addIcons(Icon.VIRTUAL_SET_22);
        setTestingText("Tonnika Sisters (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());
        List<Modifier> modifiers = new LinkedList<>();
        Evaluator alienEvaluator = new HereEvaluator(self, Filters.and(Filters.your(self), Filters.alien));
        modifiers.add(new ForceGenerationModifier(self, Filters.here(self), new AtCondition(self, Filters.Cantina),
                new NegativeEvaluator(new ConditionEvaluator(new DivideEvaluator(alienEvaluator, 2, false),
                        new MultiplyEvaluator(2, new DivideEvaluator(alienEvaluator, 4, false)),
                        new DoubledCondition(self))),  opponent));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawn(game, effectResult)
                && GameConditions.isDestinyCardMatchTo(game, Filters.alien)) {

            boolean doubled = game.getModifiersQuerying().isDoubled(game.getGameState(), self);
            GameTextActionId gameTextActionId = GameTextActionId.TONNIKA_SISTERS_V__CHOICE;

            if ((doubled && GameConditions.isTwicePerGame(game, self, gameTextActionId))
                    || (!doubled && GameConditions.isOncePerGame(game, self, gameTextActionId))) {
                final int printedOne = doubled ? 2 : 1;
                final int printedTwo = doubled ? 4 : 2;

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Add " + printedTwo + " to destiny");
                // Update usage limit(s)
                action.appendUsage(
                        new NumTimesPerGameEffect(action, printedOne));
                // Perform result(s)
                action.appendEffect(
                        new ModifyDestinyEffect(action, printedTwo));
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        boolean doubled = game.getModifiersQuerying().isDoubled(game.getGameState(), self);
        final String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.TONNIKA_SISTERS_V__CHOICE;

        if ((doubled && GameConditions.isTwicePerGame(game, self, gameTextActionId))
                || (!doubled && GameConditions.isOncePerGame(game, self, gameTextActionId))
                && (GameConditions.canActivateForce(game, playerId) || GameConditions.canActivateForce(game, opponent))) {

            final int printedOne = doubled ? 2 : 1;
            final int printedTwo = doubled ? 4 : 2;

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Each player activates up to " + printedTwo + " Force");
            // Update usage limit(s)
            action.appendUsage(
                    new NumTimesPerGameEffect(action, printedOne));


            final int maxOpponentForce = Math.min(printedTwo, game.getGameState().getReserveDeckSize(opponent));
            final int maxPlayerForce = Math.min(printedTwo, game.getGameState().getReserveDeckSize(playerId));
            // Perform result(s)
            if (maxOpponentForce>0) {
                action.appendEffect(new PlayoutDecisionEffect(action, opponent,
                        new IntegerAwaitingDecision("Choose amount of Force to activate", 1, maxOpponentForce, maxOpponentForce) {
                            @Override
                            public void decisionMade(int result) throws DecisionResultInvalidException {
                                action.appendEffect(
                                        new ActivateForceEffect(action, opponent, result));
                                if (maxPlayerForce>0) {
                                    action.appendEffect(new PlayoutDecisionEffect(action, playerId,
                                            new IntegerAwaitingDecision("Choose amount of Force to activate", 1, maxPlayerForce, maxPlayerForce) {
                                                @Override
                                                public void decisionMade(int result) throws DecisionResultInvalidException {
                                                    action.appendEffect(
                                                            new ActivateForceEffect(action, playerId, result));
                                                }
                                            }));
                                }
                            }
                        }));
            } else {
                action.appendEffect(new PlayoutDecisionEffect(action, playerId,
                        new IntegerAwaitingDecision("Choose amount of Force to activate", 1, maxPlayerForce, maxPlayerForce) {
                            @Override
                            public void decisionMade(int result) throws DecisionResultInvalidException {
                                action.appendEffect(
                                        new ActivateForceEffect(action, playerId, result));
                            }
                        }));
            }

            return Collections.singletonList(action);
        }

        return null;
    }
}
