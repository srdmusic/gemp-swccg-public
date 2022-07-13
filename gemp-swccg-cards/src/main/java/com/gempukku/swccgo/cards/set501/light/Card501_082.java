package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.MoveAsReactEffect;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UseMagneticSuctionTubeEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used
 * Title: Utinni! (V)
 */
public class Card501_082 extends AbstractUsedInterrupt {
    public Card501_082() {
        super(Side.LIGHT, 4, Title.Utinni);
        setLore("Jawa trade language word for 'Come here!' Jawas work communally and scavenge for equipment.");
        setGameText("Deploy or move a Jawa as a 'react' (for -1 Force). OR If a battle was just initiated where you have " +
                "three Jawas, choose: Use a magnetic suction tube. OR Each of your Jawas and Sandcrawlers there are " +
                "power and forfeit +1 and immune to attrition for remainder of turn");
        setTestingText("Utinni! (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.UTINNI__DOWNLOAD_OR_MOVE_JAWA;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Deploy a Jawa from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy a Jawa from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.species(Species.JAWA), -1, false, true));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.UTINNI__DOWNLOAD_OR_MOVE_JAWA;

        final Filter yourJawa = Filters.and(Filters.your(playerId), Filters.Jawa);

        // Check condition(s)
        if ((TriggerConditions.battleInitiated(game, effectResult, opponent)
                || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent))
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, true)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Deploy Jawa as 'react' from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy Jawa as a 'react' from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, yourJawa, -1, true, true));
                        }
                    }
            );
            actions.add(action);
        }

        if ((TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent)
                || TriggerConditions.battleInitiated(game, effectResult, opponent))
                && GameConditions.canTarget(game, self, yourJawa)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Move Jawa as a 'react'");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Jawa", yourJawa) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Move " + GameUtils.getCardLink(targetedCard) + " as a 'react'",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the final targeted card(s)
                                            final PhysicalCard finalJawa = targetingAction.getPrimaryTargetCard(targetGroupId);
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new MoveAsReactEffect(action, finalJawa, -1));
                                        }
                                    });
                        }
                    });
            actions.add(action);
        }

        Filters.canSpot(game, self, 3, Filters.and(yourJawa, Filters.at(Filters.battleLocation)));

        if (TriggerConditions.battleInitiated(game, effectResult)
            && GameConditions.canSpot(game, self, 3, Filters.and(yourJawa, Filters.at(Filters.battleLocation)))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add power, forfeit and immunity to your Jawas and Sandcrawlers");
            // Allow response(s)
            action.allowResponses("Make your Jawas and Sandcrawlers power +1, forfeit +1 and immune to attrition",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            Filter yourJawasAndSandcrwalersInBattle = Filters.and(Filters.your(playerId), Filters.at(Filters.battleLocation), Filters.or(yourJawa, Filters.sandcrawler));

                            final Collection<PhysicalCard> jawasAndSandcrawlers = Filters.filterActive(game, self, yourJawasAndSandcrwalersInBattle);
                            if (!jawasAndSandcrawlers.isEmpty()) {

                                // Perform result(s)
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action,
                                                new PowerModifier(self, Filters.in(jawasAndSandcrawlers), 1),
                                                "Makes " + GameUtils.getAppendedNames(jawasAndSandcrawlers) + " power +1"));
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action,
                                                new ForfeitModifier(self, Filters.in(jawasAndSandcrawlers), 1),
                                                "Makes " + GameUtils.getAppendedNames(jawasAndSandcrawlers) + " forfeit +1"));
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action,
                                                new ImmuneToAttritionModifier(self, Filters.in(jawasAndSandcrawlers)),
                                                "Makes " + GameUtils.getAppendedNames(jawasAndSandcrawlers) + " immune to attrition"));
                            }
                        }
                    }
            );
            actions.add(action);

            if(GameConditions.isDuringBattleWithParticipant(game, Filters.Magnetic_Suction_Tube)){
                final PlayInterruptAction action2 = new PlayInterruptAction(game, self);
                action2.setText("Use a Magnetic Suction Tube");
                // Choose target(s)
                action2.appendTargeting(
                        new TargetCardOnTableEffect(action2, playerId, "Choose a Magnetic Suction Tube", Filters.and(Filters.participatingInBattle, Filters.Magnetic_Suction_Tube)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                                action2.addAnimationGroup(targetedCard);
                                // Allow response(s)
                                action2.allowResponses("Use " + GameUtils.getCardLink(targetedCard),
                                        new RespondablePlayCardEffect(action2) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the final targeted card(s)
                                                final PhysicalCard finalTube = targetingAction.getPrimaryTargetCard(targetGroupId);
                                                // Perform result(s)
                                                action2.appendEffect(
                                                        new UseMagneticSuctionTubeEffect(action2, finalTube));
                                            }
                                        });
                            }
                        });
                actions.add(action2);
            }
        }

        return actions;
    }
}