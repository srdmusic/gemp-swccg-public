package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.evaluators.NegativeEvaluator;
import com.gempukku.swccgo.cards.evaluators.OccupiesEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.StealCardAndAttachFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Epic Event
 * Title: His Destiny
 */
public class Card501_058 extends AbstractEpicEventDeployable {
    public Card501_058() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "His Destiny", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If He Is The Chosen One on table, deploy on table. May lose 2 Force to cancel Force Lightning or relocate a stolen Luke's Lightsaber to Luke. " +
                "Once per game, may deploy a Death Star II site from Reserve Deck; reshuffle. " +
                "Opponent's total Force generation is -1 (and your total power is +1) for each Death Star II site you occupy.  " +
                "Once per game, during your move phase, may relocate Luke to same battleground site as a Dark Jedi. " +
                "If a card just stacked on I Feel The Conflict, may peek at top two cards of your Reserve Deck and take one into hand.");
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
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new TotalForceGenerationModifier(self, new NegativeEvaluator(new OccupiesEvaluator(playerId, Filters.Death_Star_II_site)), opponent));
        modifiers.add(new TotalPowerModifier(self, Filters.location, new OccupiesCondition(playerId, Filters.Death_Star_II_site), new OccupiesEvaluator(playerId, Filters.Death_Star_II_site), playerId));
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
            action.setActionMsg("Deploy a Death Star II site from Reserve Deck");

            action.appendUsage(
                    new OncePerGameEffect(action)
            );
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Death_Star_II_site, true)
            );
            actions.add(action);
        }

        if (GameConditions.canTargetToCancel(game, self, Filters.Force_Lightning)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Force_Lightning, Title.Force_Lightning);
            action.appendCost(new LoseForceEffect(action, playerId, 2, true));
            actions.add(action);
        }

        if (GameConditions.canTarget(game, self, TargetingReason.TO_BE_STOLEN, Filters.and(Filters.stolen, Filters.Lukes_Lightsaber))
                && GameConditions.canTarget(game, self, Filters.Luke)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId);
            action.setText("Steal Luke's Lightsaber");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target stolen Luke's Lightsaber", TargetingReason.TO_BE_STOLEN, Filters.and(Filters.stolen, Filters.Lukes_Lightsaber)) {
                @Override
                protected void cardTargeted(final int targetGroupId1, PhysicalCard targetedLightsaber) {
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Luke", Filters.Luke) {
                        @Override
                        protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedLuke) {
                            action.appendCost(
                                    new LoseForceEffect(action, playerId, 2, true));
                            action.allowResponses(new RespondableEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard finalLightsaber = action.getPrimaryTargetCard(targetGroupId1);
                                    PhysicalCard finalLuke = action.getPrimaryTargetCard(targetGroupId2);

                                    // Perform result(s)
                                    action.appendEffect(
                                            new StealCardAndAttachFromTableEffect(action, finalLightsaber, finalLuke));
                                }
                            });
                        }
                    });
                }
            });

            actions.add(action);
        }


        gameTextActionId = GameTextActionId.HIS_DESTINY__RELOCATE_LUKE;

        final Filter siteFilter = Filters.and(Filters.battleground_site, Filters.sameLocationAs(self, Filters.Dark_Jedi));

        if (GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canTarget(game, self, Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(siteFilter, 0)))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Relocate Luke");


            action.appendUsage(
                    new OncePerGameEffect(action));
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Luke", Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(siteFilter, 0))) {
                @Override
                protected void cardTargeted(final int targetGroupId1, PhysicalCard targetedLuke) {
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target location", Filters.and(siteFilter, Filters.locationCanBeRelocatedTo(targetedLuke, 0))) {
                        @Override
                        protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedLocation) {
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
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Force_Lightning)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.appendCost(new LoseForceEffect(action, playerId, 2, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if(TriggerConditions.justStackedCardOn(game, effectResult, Filters.any, Filters.I_Feel_The_Conflict)
                && GameConditions.hasReserveDeck(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Peek at top two cards of Reserve Deck");
            action.setActionMsg("Peek at top two cards of Reserve Deck and take one into hand");

            action.appendEffect(
                    new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, 2, 1, 1));

            actions.add(action);
        }

        return actions;
    }
}