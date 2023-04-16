package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.MaxLimitEvaluator;
import com.gempukku.swccgo.cards.evaluators.PowerEvaluator;
import com.gempukku.swccgo.cards.evaluators.StackedEvaluator;
import com.gempukku.swccgo.common.DestinyType;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.DuelState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.DuelDirections;
import com.gempukku.swccgo.logic.effects.DuelEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.LoseForceAndStackFaceDownEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.ConflictCardModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 21
 * Type: Epic Event
 * Title: His Destiny
 */
public class Card501_058 extends AbstractEpicEventDeployable {
    public Card501_058() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "His Destiny", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If He Is The Chosen One on table, deploy on table. Cards stacked on I Feel The Conflict are conflict cards. " +
                "While Luke is alone, your total power at other locations is +1 (limit +3) for each ‘conflict’ card.  " +
                "During opponent's draw phase, if you occupy two battlegrounds and no battles occurred this turn, opponent loses 1 Force (and stacks it on I Feel The Conflict). " +
                "During your draw phase, if Luke present with a Dark Jedi, may initiate a duel between them. " +
                "Each player draws two destiny. Add power. Highest total wins. If Luke loses, lose 1 Force (if Luke wins by more than 5, Dark Jedi lost).");
        addIcons(Icon.VIRTUAL_SET_21);
        setTestingText("His Destiny");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.He_Is_The_Chosen_One);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        Filter lukeAlone = Filters.and(Filters.Luke, Filters.alone);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new TotalPowerModifier(self, Filters.not(Filters.sameLocationAs(self, lukeAlone)), new OnTableCondition(self, lukeAlone),
                new MaxLimitEvaluator(new StackedEvaluator(self, Filters.any, Filters.conflictCard), 3), playerId));
        modifiers.add(new ConflictCardModifier(self, Filters.stackedOn(self, Filters.I_Feel_The_Conflict)));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isEndOfOpponentsPhase(game, effectResult, Phase.DRAW, playerId)
               && GameConditions.isOnceDuringOpponentsPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)
                && GameConditions.occupies(game, playerId, 2, Filters.battleground)
                && !GameConditions.hasInitiatedBattleThisTurn(game, playerId)
                && !GameConditions.hasInitiatedBattleThisTurn(game, game.getOpponent(playerId))) {

            final PhysicalCard iFeelTheConflict = Filters.findFirstActive(game, self, Filters.I_Feel_The_Conflict);
            if (iFeelTheConflict != null) {
                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose 1 Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new LoseForceAndStackFaceDownEffect(action, opponent, 1, iFeelTheConflict) {
                            @Override
                            public boolean isShownIfLostFromHand() {
                                return true;
                            }
                        });
                return Collections.singletonList(action);
            }
        }
        return null;
    }
    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        //"During opponent's draw phase, if He Will Bring Balance on table, you occupy two battlegrounds, and no battles occurred this turn, opponent loses 1 Force. " +

        // Check condition(s)
        if (GameConditions.isOnceDuringOpponentsPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)
                && GameConditions.occupies(game, playerId, 2, Filters.battleground)
                && !GameConditions.hasInitiatedBattleThisTurn(game, playerId)
                && !GameConditions.hasInitiatedBattleThisTurn(game, game.getOpponent(playerId))) {

            final PhysicalCard iFeelTheConflict = Filters.findFirstActive(game, self, Filters.I_Feel_The_Conflict);
            if (iFeelTheConflict != null) {

                TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose 1 Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new LoseForceAndStackFaceDownEffect(action, game.getOpponent(playerId), 1, iFeelTheConflict) {
                            @Override
                            public boolean isShownIfLostFromHand() {
                                return true;
                            }
                        });

                actions.add(action);
            }
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)) {
            final PhysicalCard luke = Filters.findFirstActive(game, self, Filters.Luke);
            if (luke != null) {
                Filter characterToDuel = Filters.and(Filters.Dark_Jedi, Filters.presentWith(self, Filters.sameCardId(luke)));
                TargetingReason targetingReason = TargetingReason.TO_BE_DUELED;
                if (GameConditions.canTarget(game, self, targetingReason, characterToDuel)) {
                    final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                    action.setText("Initiate a duel");
                    action.setActionMsg("Initiate a duel");
                    action.appendTargeting(
                            new TargetCardOnTableEffect(action, playerId, "Choose a Dark jedi", characterToDuel) {
                                @Override
                                protected void cardTargeted(final int targetGroupId, final PhysicalCard targetedCard) {
                                    action.addAnimationGroup(luke, targetedCard);

                                    action.appendUsage(
                                            new OncePerPhaseEffect(action));
                                    // Perform result(s)
                                    action.allowResponses("Initiate duel between " + GameUtils.getCardLink(luke) + " and " + GameUtils.getCardLink(targetedCard),
                                            new RespondableEffect(action) {
                                                @Override
                                                protected void performActionResults(Action action) {
                                                    action.appendEffect(
                                                            new DuelEffect(action, targetedCard, luke, new DuelDirections() {
                                                                @Override
                                                                public boolean isEpicDuel() {
                                                                    return false;
                                                                }

                                                                @Override
                                                                public boolean isCrossOverToDarkSideAttempt() {
                                                                    return false;
                                                                }

                                                                @Override
                                                                public Evaluator getBaseDuelTotal(final String playerId, final DuelState duelState) {
                                                                    return new PowerEvaluator(duelState.getCharacter(playerId));
                                                                }

                                                                @Override
                                                                public int getBaseNumDuelDestinyDraws(String playerId, DuelState duelState) {
                                                                    return 2;
                                                                }

                                                                @Override
                                                                public void performDuelDirections(final Action duelAction, SwccgGame game, final DuelState duelState) {
                                                                    duelAction.appendEffect(
                                                                            new DrawDestinyEffect(duelAction, game.getDarkPlayer(), game.getModifiersQuerying().getNumDuelDestinyDraws(game.getGameState(), game.getDarkPlayer()), DestinyType.DUEL_DESTINY) {
                                                                                @Override
                                                                                protected void destinyDraws(SwccgGame game, final List<PhysicalCard> darkDestinyCardDraws, List<Float> darkDestinyDrawValues, final Float darkTotalDestiny) {
                                                                                    if (darkTotalDestiny != null) {
                                                                                        duelState.increaseTotalDuelDestinyFromDraws(game.getDarkPlayer(), darkTotalDestiny, darkDestinyCardDraws.size());
                                                                                    }
                                                                                    duelAction.appendEffect(
                                                                                            new DrawDestinyEffect(duelAction, game.getLightPlayer(), game.getModifiersQuerying().getNumDuelDestinyDraws(game.getGameState(), game.getLightPlayer()), DestinyType.DUEL_DESTINY) {
                                                                                                @Override
                                                                                                protected void destinyDraws(SwccgGame game, List<PhysicalCard> lightDestinyCardDraws, List<Float> lightDestinyDrawValues, Float lightTotalDestiny) {
                                                                                                    if (lightTotalDestiny != null) {
                                                                                                        duelState.increaseTotalDuelDestinyFromDraws(game.getLightPlayer(), lightTotalDestiny, lightDestinyCardDraws.size());
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                    );
                                                                                }
                                                                            }
                                                                    );
                                                                }

                                                                @Override
                                                                public void performDuelResults(final Action action, SwccgGame game, DuelState duelState) {
                                                                    // If no loser, then nothing to do
                                                                    final String winner = duelState.getWinner();
                                                                    if (winner == null)
                                                                        return;
                                                                    if (!winner.equals(self.getOwner())) {
                                                                        action.appendEffect(
                                                                                new LoseForceEffect(action, self.getOwner(), 1));
                                                                    } else if (duelState.getFinalDuelTotal(self.getOwner()) - duelState.getFinalDuelTotal(game.getOpponent(self.getOwner())) > 5) {
                                                                        // if Luke wins by more than 5, Dark Jedi lost
                                                                        PhysicalCard losingCharacter = duelState.getLosingCharacter();
                                                                        if (losingCharacter != null) {
                                                                            action.appendEffect(
                                                                                    new LoseCardFromTableEffect(action, losingCharacter));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            ));
                                                }
                                            });
                                }
                            });
                    actions.add(action);
                }
            }

        }
        return actions;
    }
}