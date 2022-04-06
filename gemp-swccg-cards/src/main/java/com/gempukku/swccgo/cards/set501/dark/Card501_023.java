package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventPlayable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.UseWeaponEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.TargetTheMainGeneratorState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayEpicEventAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.PassthruEffect;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 18
 * Type: Epic Event
 * Title: Target The Main Generator (v)
 */
public class Card501_023 extends AbstractEpicEventPlayable {
    public Card501_023() {
        super(Side.DARK, Title.Target_The_Main_Generator);
        setVirtualSuffix(true);
        setGameText("At the end of your control phase, your AT-AT Cannon below 4th marker may fire (if in range) at 1st Marker. Prepare to Target the Main Generator: Draw destiny. Maximum Firepower: If (destiny + X + Y) > 8, 1st Marker is 'blown away' and this card is lost. Otherwise, this card is used. X = number of Imperials on Hoth (may not exceed 3). Y = number of marker sites controlled by AT-ATs");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Target The Main Generator (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayEpicEventAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
                && GameConditions.canDrawDestiny(game, playerId)) {
            final PhysicalCard mainPowerGenerators = Filters.findFirstFromTopLocationsOnTable(game, Filters.Main_Power_Generators);
            if (mainPowerGenerators != null) {
                Filter weaponToFireFilter = Filters.and(Filters.your(self), Filters.AT_AT_Cannon, Filters.at(Filters.or(Filters.First_Marker, Filters.Second_Marker, Filters.Third_Marker)), Filters.attachedTo(Filters.and(Filters.AT_AT, Filters.piloted)), Filters.canBeFiredAtLocationInRange(mainPowerGenerators));
                if (GameConditions.canSpot(game, self, weaponToFireFilter)) {
                    final TargetTheMainGeneratorState epicEventState = new TargetTheMainGeneratorState(self);

                    final PlayEpicEventAction action = new PlayEpicEventAction(self);
                    action.setText("Attempt to 'blow away' Main Power Generators");
                    action.setEpicEventState(epicEventState);
                    // Choose target(s)
                    action.appendTargeting(
                            new ChooseCardOnTableEffect(action, playerId, "Choose AT-AT Cannon", weaponToFireFilter) {
                                @Override
                                protected void cardSelected(final PhysicalCard weapon) {
                                    final PhysicalCard atat = weapon.getAttachedTo();

                                    action.appendUsage(
                                            new UseWeaponEffect(action, atat, weapon));
                                    action.addAnimationGroup(weapon);
                                    action.addAnimationGroup(mainPowerGenerators);
                                    // Update Epic Event State
                                    epicEventState.setAtat(atat);
                                    epicEventState.setAtatCannon(weapon);
                                    String actionText = "Fire " + GameUtils.getCardLink(weapon) + " at " + GameUtils.getCardLink(mainPowerGenerators);
                                    // Allow response(s)
                                    action.allowResponses(actionText,
                                            new RespondablePlayCardEffect(action) {
                                                @Override
                                                protected void performActionResults(Action targetingAction) {
                                                    final GameState gameState = game.getGameState();
                                                    final ModifiersQuerying modifiersQuerying = game.getModifiersQuerying();
                                                    // Begin weapon firing
                                                    action.appendEffect(
                                                            new PassthruEffect(action) {
                                                                @Override
                                                                protected void doPlayEffect(SwccgGame game) {
                                                                    gameState.sendMessage(playerId + " fires " + GameUtils.getCardLink(weapon) + " at " + GameUtils.getCardLink(mainPowerGenerators));
                                                                    gameState.activatedCard(playerId, weapon);
                                                                    gameState.cardAffectsCard(playerId, weapon, mainPowerGenerators);
                                                                    gameState.beginWeaponFiring(weapon, null);
                                                                    gameState.getWeaponFiringState().setCardFiringWeapon(atat);
                                                                    gameState.getWeaponFiringState().setTarget(mainPowerGenerators);
                                                                    // Finish weapon firing
                                                                    action.appendAfterEffect(
                                                                            new PassthruEffect(action) {
                                                                                @Override
                                                                                protected void doPlayEffect(SwccgGame game) {
                                                                                    gameState.finishWeaponFiring();
                                                                                }
                                                                            }
                                                                    );
                                                                }
                                                            }
                                                    );
                                                    // 1) Prepare To Target The Main Generator
                                                    action.appendEffect(
                                                            new DrawDestinyEffect(action, playerId, 1, DestinyType.EPIC_EVENT_AND_WEAPON_DESTINY) {
                                                                @Override
                                                                protected void destinyDraws(SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, Float totalDestiny) {
                                                                    // 2) Maximum Firepower!
                                                                    gameState.sendMessage("Destiny: " + (totalDestiny != null ? GuiUtils.formatAsString(totalDestiny) : "Failed destiny draw"));

                                                                    float valueForX = modifiersQuerying.getVariableValue(gameState, self, Variable.X, Math.min(3, Filters.countActive(game, self, Filters.and(Filters.character, Filters.Imperial, Filters.on(Title.Hoth)))));

                                                                    float valueForY = modifiersQuerying.getVariableValue(gameState, self, Variable.Y, Filters.countTopLocationsOnTable(game,
                                                                            Filters.and(Filters.marker_site, Filters.notIgnoredDuringEpicEventCalculation, Filters.controlsWith(playerId, self, Filters.and(Filters.piloted, Filters.AT_AT)))));

                                                                    gameState.sendMessage("X: " + GuiUtils.formatAsString(valueForX));
                                                                    gameState.sendMessage("Y: " + GuiUtils.formatAsString(valueForY));

                                                                    float total = modifiersQuerying.getEpicEventCalculationTotal(gameState, self, (totalDestiny != null ? totalDestiny : 0) + valueForX + valueForY);
                                                                    gameState.sendMessage("Epic Event Total: " + GuiUtils.formatAsString(total));

                                                                    if (total > 8) {
                                                                        gameState.sendMessage("Result: Succeeded");
                                                                        action.appendEffect(
                                                                                new BlowAwayEffect(action, mainPowerGenerators));
                                                                        action.appendEffect(
                                                                                new PutCardFromVoidInLostPileEffect(action, playerId, self));
                                                                    }
                                                                    else {
                                                                        gameState.sendMessage("Result: Failed");
                                                                        action.appendEffect(
                                                                                new PutCardFromVoidInUsedPileEffect(action, playerId, self));
                                                                    }
                                                                }
                                                            }
                                                    );
                                                }
                                            }
                                    );
                                }


                            }
                    );
                    return Collections.singletonList(action);
                }
            }
        }
        return null;
    }
}