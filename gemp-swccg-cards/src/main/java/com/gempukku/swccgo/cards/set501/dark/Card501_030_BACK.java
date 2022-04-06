package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelBattleEffect;
import com.gempukku.swccgo.cards.evaluators.OccupiesWithEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Objective
 * Title: A Great Tactician Creates Plans / The Result Is Often Resentment
 */
public class Card501_030_BACK extends AbstractObjective {
    public Card501_030_BACK() {
        super(Side.DARK, 7, Title.The_Result_Is_Often_Resentment);
        setGameText("While this side up, Rebels are destiny -1. If a battle was just initiated involving an Imperial leader or occupied TIE defender, may peek at cards stacked on Thrawn's Art Collection and reveal one 'artwork' card. If a weapon, cancel battle. Otherwise, if artwork's printed destiny number is: (0-2), opponent's immunity to attrition (if any) is canceled during battle (3-4): Exclude one character (if any) from battle (5+): Add destiny value to your total power. Place 'artwork' in owner's Lost Pile. " +
                "Flip this card (except during battle) if no 'artwork' stacked on Thrawn's Art Collection.");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] The Result Is Often Resentment");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostModifier(self, Filters.or(Filters.and(Filters.your(self), Filters.admiral, Filters.except(Filters.Thrawn)),
                Filters.and(Filters.your(self), Filters.or(Icon.EPISODE_I, Icon.EPISODE_VII), Filters.or(Filters.hasAbilityOrHasPermanentPilotWithAbility, Icon.PRESENCE))), 3));

        modifiers.add(new DestinyModifier(self, Filters.Rebel, -1));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.battleInitiated(game, effectResult)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.or(Filters.Imperial_leader, Filters.and(Filters.TIE_Defender, Filters.or(Filters.piloted, Filters.hasPermanentPilot, Filters.hasAboard(self, Filters.character)))))
                && GameConditions.canSpot(game, self, Filters.and(Filters.Thrawns_Art_Collection, Filters.hasStacked(Filters.any)))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Study artwork");
            action.setActionMsg("Peek at cards stacked on Thrawn's Art Collection and reveal one 'artwork' card");

            action.appendEffect(new ChooseStackedCardEffect(action, playerId, Filters.Thrawns_Art_Collection) {
                @Override
                protected void cardSelected(final PhysicalCard selectedCard) {
                    game.getGameState().sendMessage("Revealed "+ GameUtils.getCardLink(selectedCard));
                    game.getGameState().showCardOnScreen(selectedCard);
                    game.getActionsEnvironment().emitEffectResult(
                            new ArtworkCardRevealedResult(selectedCard));

                    if (Filters.weapon.accepts(game, selectedCard)) {
                        action.appendEffect(
                                new CancelBattleEffect(action));
                        action.appendEffect(
                                new PutStackedCardInLostPileEffect(action, playerId, selectedCard, false));
                    } else {
                        action.appendEffect(new RefreshPrintedDestinyValuesEffect(action, selectedCard) {
                            @Override
                            protected void refreshedPrintedDestinyValues() {
                                float printedDestinyValue = selectedCard.getDestinyValueToUse();

                                action.appendEffect(new SendMessageEffect(action, "Printed destiny value: "+ printedDestinyValue));

                                if (printedDestinyValue >= 0 && printedDestinyValue <= 2) {
                                    action.appendEffect(
                                            new CancelImmunityToAttritionUntilEndOfBattleEffect(action, Filters.and(Filters.participatingInBattle, Filters.opponents(playerId)), "Cancel "+ game.getOpponent(playerId) + "'s immunity to attrition"));
                                } else if (printedDestinyValue >= 3 && printedDestinyValue <= 4
                                        && GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.character, Filters.canBeTargetedBy(self, TargetingReason.TO_BE_EXCLUDED_FROM_BATTLE)))) {
                                    action.appendEffect(new ChooseCardOnTableEffect(action, playerId, "Target a character to exclude from battle", Filters.and(Filters.participatingInBattle, Filters.character, Filters.canBeTargetedBy(self, TargetingReason.TO_BE_EXCLUDED_FROM_BATTLE))) {
                                        @Override
                                        protected void cardSelected(PhysicalCard selectedCard) {
                                            action.appendEffect(
                                                    new ExcludeFromBattleEffect(action, selectedCard));
                                        }
                                    });
                                } else if (printedDestinyValue >= 5) {
                                    action.appendEffect(
                                            new ModifyTotalPowerUntilEndOfBattleEffect(action, printedDestinyValue, playerId, "Adds "+ printedDestinyValue + " to total power"));
                                } else {
                                    game.getGameState().sendMessage("Result: No effect");
                                }
                                action.appendEffect(
                                        new PutStackedCardInLostPileEffect(action, playerId, selectedCard, false));

                            }
                        });
                    }
                }
            });
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && !GameConditions.isDuringBattle(game)) {
            PhysicalCard thrawnsArtCollection = Filters.findFirstActive(game, self, Filters.Thrawns_Art_Collection);
            if (thrawnsArtCollection != null && !GameConditions.hasStackedCards(game, thrawnsArtCollection)) {

                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Flip");
                action.setActionMsg(null);
                // Perform result(s)
                action.appendEffect(
                        new FlipCardEffect(action, self));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
