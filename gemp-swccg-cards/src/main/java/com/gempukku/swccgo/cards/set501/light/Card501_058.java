package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.MaxLimitEvaluator;
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
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.DuelDirections;
import com.gempukku.swccgo.logic.effects.DuelEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.ConflictCardModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

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
        super(Side.LIGHT, 6, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "His Destiny", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If He Is The Chosen One on table, deploy on table. Cards stacked on I Feel The Conflict are conflict cards. " +
                "Once per game, may deploy a Death Star II site from Reserve Deck; reshuffle. " +
                "While Luke is alone, your total power at other locations is +1 (limit +3) for each ‘conflict’ card.  " +
                "During your move phase, if you played a [Death Star II] Interrupt, may relocate Luke to a battleground site. " +
                "During your draw phase, if Luke present with a Dark Jedi, may initiate a duel between them. " +
                "Each player draws two destiny. Highest total wins. If Luke loses, lose 1 Force (if Luke wins by more than 5, Dark Jedi lost).");
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
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.HIS_DESTINY__DEPLOY_A_DEATH_STAR_II_SITE;

        if(GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)){

            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a Death Star II site");
            action.setActionMsg("Deply a Death Star II site from Reserve Deck");

            action.appendUsage(
                    new OncePerGameEffect(action)
            );
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Death_Star_II_site, true)
            );
            actions.add(action);
        }


        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)) {
            TargetingReason targetingReason = TargetingReason.TO_BE_DUELED;
            final PhysicalCard luke = Filters.findFirstActive(game, self, Filters.and(Filters.Luke, Filters.canBeTargetedBy(self, targetingReason)));
            if (luke != null) {
                Filter characterToDuel = Filters.and(Filters.Dark_Jedi, Filters.presentWith(self, Filters.sameCardId(luke)));
                if (GameConditions.canTarget(game, self, targetingReason, characterToDuel)) {
                    final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                    action.setText("Initiate a duel");
                    action.setActionMsg("Initiate a duel");
                    action.appendTargeting(
                            new TargetCardOnTableEffect(action, playerId, "Choose a Dark jedi", targetingReason, characterToDuel) {
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
                                                                    return new ConstantEvaluator(0);
                                                                }

                                                                @Override
                                                                public int getBaseNumDuelDestinyDraws(String playerId, DuelState duelState) {
                                                                    return 2;
                                                                }

                                                                @Override
                                                                public void performDuelDirections(final Action duelAction, SwccgGame game, final DuelState duelState) {
                                                                    duelAction.appendEffect(
                                                                            new DrawDestinyEffect(duelAction, game.getLightPlayer(), game.getModifiersQuerying().getNumDuelDestinyDraws(game.getGameState(), game.getLightPlayer()), DestinyType.DUEL_DESTINY) {
                                                                                @Override
                                                                                protected void destinyDraws(SwccgGame game, final List<PhysicalCard> lightDestinyCardDraws, List<Float> lightDestinyDrawValues, final Float lightTotalDestiny) {
                                                                                    if (lightTotalDestiny != null) {
                                                                                        duelState.increaseTotalDuelDestinyFromDraws(game.getLightPlayer(), lightTotalDestiny, lightDestinyCardDraws.size());
                                                                                    }
                                                                                    duelAction.appendEffect(
                                                                                            new DrawDestinyEffect(duelAction, game.getDarkPlayer(), game.getModifiersQuerying().getNumDuelDestinyDraws(game.getGameState(), game.getDarkPlayer()), DestinyType.DUEL_DESTINY) {
                                                                                                @Override
                                                                                                protected void destinyDraws(SwccgGame game, final List<PhysicalCard> darkDestinyCardDraws, List<Float> darkDestinyDrawValues, final Float darkTotalDestiny) {
                                                                                                    if (darkTotalDestiny != null) {
                                                                                                        duelState.increaseTotalDuelDestinyFromDraws(game.getDarkPlayer(), darkTotalDestiny, darkDestinyCardDraws.size());
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

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isPlayingCard(game, effect, playerId, Filters.and(Icon.DEATH_STAR_II, Filters.Interrupt))
                && GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && GameConditions.canTarget(game, self, Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(Filters.battleground_site, 0)))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Relocate Luke");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Luke", Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(Filters.battleground_site, 0))) {
                @Override
                protected void cardTargeted(final int targetGroupId1, PhysicalCard targetedCard) {
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose location", Filters.and(Filters.battleground_site, Filters.locationCanBeRelocatedTo(targetedCard, 0))) {
                        @Override
                        protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedCard) {

                            action.allowResponses(new RespondableEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard finalLuke = action.getPrimaryTargetCard(targetGroupId1);
                                    PhysicalCard finalLocation = action.getPrimaryTargetCard(targetGroupId2);

                                    action.appendEffect(
                                            new RelocateBetweenLocationsEffect(action, finalLuke, finalLocation));
                                }
                            });
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }

        return null;
    }
}