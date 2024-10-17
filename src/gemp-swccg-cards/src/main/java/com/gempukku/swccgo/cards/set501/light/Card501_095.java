package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.MultiplyEvaluator;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.DestinyType;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.ActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BlowAwayState;
import com.gempukku.swccgo.game.state.DeactivateTheShieldGeneratorState;
import com.gempukku.swccgo.game.state.DrawDestinyState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelEpicEventGameTextAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.AddToBlownAwayForceLossEffect;
import com.gempukku.swccgo.logic.effects.BlowAwayEffect;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.TriggeringResultEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.modifiers.TotalDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.StandardEffect;
import com.gempukku.swccgo.logic.timing.results.CalculatingEpicEventTotalResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: Deactivate The Shield Generator (V)
 */
public class Card501_095 extends AbstractEpicEventDeployable {
    public Card501_095() {
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, Title.Deactivate_The_Shield_Generator, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setGameText("Deploy on Bunker if your [V25] objective on table. " +
                "Once during your control phase, if you control Bunker (or have at least 5 more power than opponent there), may attempt to 'blow away' Bunker:" +
                "Charges! Come On, Come On!: Draw destiny. Add 3 to total for each Explosive Charge on Bunker." +
                "Move! Move! Move!: If total destiny > 8, your characters here may relocate to Back Door for free. " +
                "Bunker is 'blown away,' opponent loses 4 Force (may not be reduced).");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_25);
        setTestingText("Deactivate The Shield Generator (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Bunker;
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(playerId), Filters.icon(Icon.VIRTUAL_SET_25), Filters.Objective));
    }

    @Override
    protected List<TopLevelEpicEventGameTextAction> getEpicEventGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        PhysicalCard bunker = Filters.findFirstActive(game, self, Filters.Bunker);
        float yourPower = game.getModifiersQuerying().getTotalPowerAtLocation(game.getGameState(), bunker, playerId, false, false);
        float opponentPower = game.getModifiersQuerying().getTotalPowerAtLocation(game.getGameState(), bunker, game.getOpponent(playerId), false, false);

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, Phase.CONTROL)
                && (GameConditions.controls(game, playerId, Filters.Bunker) || yourPower > opponentPower)
                && GameConditions.canDrawDestiny(game, playerId)) {
            final DeactivateTheShieldGeneratorState epicEventState = new DeactivateTheShieldGeneratorState(self);

            final TopLevelEpicEventGameTextAction action = new TopLevelEpicEventGameTextAction(self, gameTextSourceCardId);
            action.setText("Attempt to 'blow away' Bunker");
            action.setEpicEventState(epicEventState);
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            // 1) Charges! Come On, Come On!
            action.appendEffect(
                    new DrawDestinyEffect(action, playerId, 1, DestinyType.EPIC_EVENT_DESTINY) {
                        @Override
                        protected List<Modifier> getDrawDestinyModifiers(SwccgGame game, DrawDestinyState drawDestinyState) {
                            Modifier modifier = new TotalDestinyModifier(self, drawDestinyState.getId(), new MultiplyEvaluator(3, new OnTableEvaluator(self, Filters.and(Filters.Explosive_Charge, Filters.attachedTo(Filters.Bunker)))));
                            return Collections.singletonList(modifier);
                        }

                        @Override
                        protected void destinyDraws(SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, final Float totalDestiny) {
                            final GameState gameState = game.getGameState();
                            final ModifiersQuerying modifiersQuerying = game.getModifiersQuerying();
                            // 2) Move! Move! Move!
                            if (totalDestiny == null) {
                                gameState.sendMessage("Result: Failed due to failed destiny draw");
                                return;
                            }

                            final float initialEpicEventTotal = modifiersQuerying.getEpicEventCalculationTotal(gameState, self, totalDestiny);
                            gameState.sendMessage("Epic Event Total: " + GuiUtils.formatAsString(initialEpicEventTotal));
                            // Emit effect result that Deactivate The Shield Generator total is being calculated
                            action.appendEffect(
                                    new TriggeringResultEffect(action, new CalculatingEpicEventTotalResult(playerId, self)));
                            action.appendEffect(
                                    new PassthruEffect(action) {
                                        @Override
                                        protected void doPlayEffect(SwccgGame game) {
                                            float finalEpicEventTotal = modifiersQuerying.getEpicEventCalculationTotal(gameState, self, totalDestiny);
                                            if (initialEpicEventTotal != finalEpicEventTotal) {
                                                gameState.sendMessage("Epic Event Total: " + GuiUtils.formatAsString(finalEpicEventTotal));
                                            }

                                            if (finalEpicEventTotal > 8) {
                                                gameState.sendMessage("Result: Succeeded");
                                                final PhysicalCard bunker = Filters.findFirstFromTopLocationsOnTable(game, Filters.Bunker);
                                                final PhysicalCard backDoor = Filters.findFirstFromTopLocationsOnTable(game, Filters.Back_Door);
                                                if (backDoor != null) {
                                                    final Collection<PhysicalCard> charactersToRelocate = Filters.filterActive(game, self,
                                                            Filters.and(Filters.your(self), Filters.character, Filters.at(bunker), Filters.canBeRelocatedToLocation(backDoor, true, 0)));
                                                    if (!charactersToRelocate.isEmpty()) {
                                                        action.appendEffect(
                                                                new PlayoutDecisionEffect(action, playerId,
                                                                        new YesNoDecision("Do you want to relocate your characters from " + GameUtils.getCardLink(bunker) + " to " + GameUtils.getCardLink(backDoor) + "?") {
                                                                            @Override
                                                                            protected void yes() {
                                                                                gameState.sendMessage(playerId + " chooses to relocate characters from " + GameUtils.getCardLink(bunker) + " to " + GameUtils.getCardLink(backDoor));
                                                                                action.insertEffect(
                                                                                        new RelocateBetweenLocationsEffect(action, charactersToRelocate, backDoor));
                                                                            }

                                                                            @Override
                                                                            protected void no() {
                                                                                gameState.sendMessage(playerId + " chooses to not relocate characters from " + GameUtils.getCardLink(bunker) + " to " + GameUtils.getCardLink(backDoor));
                                                                            }
                                                                        }
                                                                )
                                                        );
                                                    }
                                                }

                                                action.appendEffect(
                                                        new BlowAwayEffect(action, bunker) {
                                                            @Override
                                                            protected List<ActionProxy> getBlowAwayActionProxies(SwccgGame game, BlowAwayState blowAwayState) {
                                                                ActionProxy actionProxy = new AbstractActionProxy() {
                                                                    @Override
                                                                    public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                                                        List<TriggerAction> actions = new LinkedList<TriggerAction>();
                                                                        String opponent = game.getOpponent(playerId);

                                                                        // Check condition(s)
                                                                        if (TriggerConditions.isBlownAwayCalculateForceLossStep(game, effectResult, bunker)) {

                                                                            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                                                                            action.skipInitialMessageAndAnimation();
                                                                            // Perform result(s)
                                                                            action.appendEffect(
                                                                                    new AddToBlownAwayForceLossEffect(action, opponent, 4, true));
                                                                            actions.add(action);
                                                                        }
                                                                        return actions;
                                                                    }
                                                                };
                                                                return Collections.singletonList(actionProxy);
                                                            }

                                                            @Override
                                                            protected StandardEffect getAdditionalGameTextEffect(SwccgGame game, Action blowAwaySubAction) {
                                                                return new LoseCardFromTableEffect(blowAwaySubAction, self);
                                                            }
                                                        }
                                                );
                                            } else {
                                                gameState.sendMessage("Result: Failed");
                                            }
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