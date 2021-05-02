package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 15
 * Type: Epic Event
 * Title: Emperor's Orders
 */
public class Card501_029 extends AbstractEpicEventDeployable {
    public Card501_029() {
        super(Side.DARK, PlayCardZoneOption.ATTACHED, Title.Emperors_Orders);
        setGameText("The Alliance Will Die...: Deploy on Executor if you have no objective. Flagship Operations may deploy regardless of deployment restrictions. Your cards may not add more than 2 to the power, destiny and forfeit of a squadron." +
                "...As Will Your Friends: At battleground systems where you have a Star Destroyer and a piloted TIE, your force drains = 3. Your TIE assault squadrons may deploy for 3 force (without replacement). If Executor lost, this card lost and you lose 3 force." +
                "'I'm Hit!:' During battle, opponent may place their A-wing with Executor in Lost Pile to cancel Executor’s immunity to attrition");
        addIcons(Icon.VIRTUAL_SET_15);
        setTestingText("Emperor's Orders");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Executor;
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return !GameConditions.canSpot(game, self, Filters.and(Filters.your(playerId), Filters.Objective));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Flagship_Operations, ModifyGameTextType.FLAGSHIP_OPERATIONS__MAY_IGNORE_DEPLOYMENT_RESTRICTIONS));
        modifiers.add(new UseCalculationForDeployCostModifier(self, Filters.TIE_Assault_Squadron, new ConstantEvaluator(3)));
        modifiers.add(new MayDeployToTargetModifier(self, Filters.TIE_Assault_Squadron, Filters.any));
        modifiers.add(new ResetForceDrainModifier(self, Filters.and(Filters.battleground_system, Filters.sameLocationAs(self, Filters.and(Filters.your(self.getOwner()), Filters.and(Filters.piloted, Filters.TIE), Filters.with(self, Filters.Star_Destroyer)))), 3));
        modifiers.add(new PowerIncreaseLimitModifier(self, Filters.and(Filters.your(self.getOwner()), Filters.squadron), 2));
        modifiers.add(new DestinyIncreaseLimitModifier(self, Filters.and(Filters.your(self.getOwner()), Filters.squadron), 2));
        modifiers.add(new ForfeitIncreaseLimitModifier(self, Filters.and(Filters.your(self.getOwner()), Filters.squadron), 2));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLeavesTableRequiredTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.justLost(game, effectResult, self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Lose 3 Force");
            action.setActionMsg("Lose 3 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, self.getOwner(), 3)
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getOpponentsCardGameTextTopLevelActions(String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        Filter aWingWithExecutor = Filters.and(Filters.your(playerId), Filters.A_wing, Filters.with(self, Filters.Executor));

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(aWingWithExecutor))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Place A-Wing in Lost Pile");
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose A-Wing", aWingWithExecutor) {
                        @Override
                        protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            action.addAnimationGroup(Filters.findFirstActive(game, self, Filters.Executor));
                            action.allowResponses("Cancel Executor's Immunity to attrition",
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new PlaceCardInLostPileFromTableEffect(action, targetedCard)
                                            );
                                            action.appendEffect(
                                                    new CancelImmunityToAttritionUntilEndOfBattleEffect(action, Filters.Executor, "Cancel immunity to attrition")
                                            );
                                        }
                                    }
                            );

                        }
                    }
            );

            return Collections.singletonList(action);
        }
        return null;
    }
}
