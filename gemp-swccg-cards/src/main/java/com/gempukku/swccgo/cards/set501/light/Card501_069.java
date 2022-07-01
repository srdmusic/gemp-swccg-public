package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.AbilityEvaluator;
import com.gempukku.swccgo.cards.evaluators.StackedEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.DuelState;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Epic Event
 * Title: His Destiny
 */
public class Card501_069 extends AbstractEpicEventDeployable {
    public Card501_069() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "His Destiny", Uniqueness.UNIQUE);
        setGameText("Deploy on table if your V8 objective on table. Padme ignores objective deployment restrictions. X = number of cards stacked on I Feel The Conflict. If Luke is alone: he is immunity to attrition +X, your total battle destiny at other locations is +X, and if you just took a card into hand with He Will Bring Balance, opponent stacks a card from hand (if possible) on I Feel The Conflict. During your move phase, if Luke present with opponent’s character of ability > 4, may initiate a duel. Each player draws destiny. Highest total wins. Winner retrieves 1 Force (cannot be canceled).");
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("His Destiny");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        Filter set8Objective = Filters.and(Icon.VIRTUAL_SET_8, Filters.Objective);
        return Filters.canSpot(game, self, set8Objective);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        Filter set3Padme = Filters.and(Icon.VIRTUAL_SET_3, Filters.Padme);
        Filter lukeAlone = Filters.and(Filters.Luke, Filters.alone);
        Filter lukesLocation = Filters.and(Filters.sameLocationAs(self, Filters.Luke));

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, set3Padme, null, self.getOwner(), Filters.and(Filters.your(self), Filters.Objective)));
        modifiers.add(new ImmunityToAttritionChangeModifier(self,lukeAlone,new StackedEvaluator(self, Filters.I_Feel_The_Conflict)));
        modifiers.add (new TotalBattleDestinyModifier(self,
                Filters.not(lukesLocation),
                new OnTableCondition(self, lukeAlone),
                new StackedEvaluator(self, Filters.I_Feel_The_Conflict),
                playerId));
        modifiers.add(new ModifyGameTextModifier(self, Filters.title(Title.He_Will_Bring_Balance), ModifyGameTextType.HE_WILL_BRING_BALANCE__STACK_IF_CARD_TAKEN_INTO_HAND));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.MOVE)) {
            final PhysicalCard luke = Filters.findFirstActive(game, self, Filters.Luke);
            if (luke != null) {
                Filter characterToDuel = Filters.and(Filters.opponents(self), Filters.character, Filters.abilityMoreThan(4), Filters.presentWith(self, Filters.sameCardId(luke)));
                TargetingReason targetingReason = TargetingReason.TO_BE_DUELED;
                if (GameConditions.canTarget(game, self, targetingReason, characterToDuel)) {
                    final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                    action.appendTargeting(
                            new TargetCardOnTableEffect(action, playerId, "Choose character of ability > 4", characterToDuel) {
                                @Override
                                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                                    action.setText("Initiate a duel");
                                    action.addAnimationGroup(luke, targetedCard);

                                    action.appendUsage(
                                            new OncePerTurnEffect(action));
                                    // Perform result(s)
                                    action.appendEffect(
                                            new DuelEffect(action, luke, targetedCard, new DuelDirections() {
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
                                                    return new AbilityEvaluator(duelState.getCharacter(playerId));
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
                                                    action.appendEffect(
                                                            new RetrieveForceEffect(action, winner, 1));
                                                }
                                            }
                                            ));

                                }
                            });
                }
            }

        }
        return null;
    }
}