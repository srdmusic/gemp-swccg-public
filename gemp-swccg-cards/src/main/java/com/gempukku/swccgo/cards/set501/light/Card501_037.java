package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.StackedEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PutStackedCardInUsedPileEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardsInUsedPileEffect;
import com.gempukku.swccgo.logic.effects.RefreshPrintedDestinyValuesEffect;
import com.gempukku.swccgo.logic.effects.UseCombatCardForSubstituteDestinyEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.effects.choose.StackCombatCardFromHandEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.AboutToDrawDestinyCardResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 15
 * Type: Epic Event
 * Title: Communing
 */
public class Card501_037 extends AbstractEpicEventDeployable {
    public Card501_037() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Communing, Uniqueness.UNIQUE);
        setGameText("Deploy on table.\n" +
                "One With The Force: If a Jedi was just lost (or placed out of play) from table, may stack that card here.\n" +
                "The Living Force: Jedi stacked here are 'communing' and are considered out of play. Your total Force generation is +1 for each Jedi stacked here.\n" +
                "The Cosmic Force: Once per turn, if a Jedi is 'communing,' may use 1 Force to peek at the top card of Reserve Deck, Force Pile, and/or Used Pile; return one card to each deck or pile.");
        addIcons(Icon.VIRTUAL_SET_15, Icon.EPISODE_I);
        setTestingText("Communing");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalForceGenerationModifier(self, new StackedEvaluator(self, self), self.getOwner()));
        modifiers.add(new CommuningModifier(self, Filters.and(Filters.stackedOn(self), Filters.Jedi)));
        modifiers.add(new ConsideredOutOfPlayModifier(self, Filters.stackedOn(self)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringEitherPlayersPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.hasHand(game, playerId)) {
            Filter jediFilter = Filters.and(Filters.your(self), Filters.Jedi, Filters.not(Filters.hasStacked(2, Filters.combatCard)));
            if (GameConditions.canSpot(game, self, jediFilter)) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Place combat card under your Jedi");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Choose target(s)
                action.appendTargeting(
                        new ChooseCardOnTableEffect(action, playerId, "Choose Jedi", jediFilter) {
                            @Override
                            protected void cardSelected(final PhysicalCard jedi) {
                                action.setActionMsg("Place a combat card under " + GameUtils.getCardLink(jedi));
                                action.appendTargeting(
                                        new ChooseCardFromHandEffect(action, playerId) {
                                            @Override
                                            protected void cardSelected(SwccgGame game, PhysicalCard selectedCard) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new StackCombatCardFromHandEffect(action, playerId, jedi, selectedCard));
                                            }
                                        }
                                );
                            }
                        });
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();

        // Check condition(s)
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.and(Filters.your(self), Filters.hasStacked(Filters.combatCard)))) {
            PhysicalCard cardAboutToLeaveTable = ((AboutToLeaveTableResult) effectResult).getCardAboutToLeaveTable();
            Collection<PhysicalCard> combatCards = Filters.filterStacked(game, Filters.and(Filters.combatCard, Filters.stackedOn(cardAboutToLeaveTable)));
            if (!combatCards.isEmpty()) {

                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Place combat cards in Used Pile");
                action.setActionMsg("Place " + GameUtils.getCardLink(cardAboutToLeaveTable) + "'s combat cards in Used Pile");
                // Perform result(s)
                action.appendEffect(
                        new PutStackedCardsInUsedPileEffect(action, playerId, combatCards, true));
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if ((TriggerConditions.isAboutToDrawDuelDestiny(game, effectResult, playerId) || TriggerConditions.isAboutToDrawLightsaberCombatDestiny(game, effectResult, playerId))
                && GameConditions.canUseCombatCard(game, playerId)) {
            final AboutToDrawDestinyCardResult aboutToDrawDestinyCardResult = (AboutToDrawDestinyCardResult) effectResult;
            PhysicalCard yourJedi = Filters.findFirstActive(game, self, Filters.and(Filters.your(self),
                    Filters.Jedi, Filters.or(Filters.participatingInDuel, Filters.participatingInLightsaberCombat)));
            if (yourJedi != null) {
                Collection<PhysicalCard> combatCards = Filters.filterStacked(game, Filters.combatCardUsableBy(yourJedi));
                if (!combatCards.isEmpty()) {

                    final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                    action.setText("Use combat card");
                    // Choose target(s)
                    action.appendTargeting(
                            new ChooseStackedCardEffect(action, playerId, Filters.any, Filters.in(combatCards)) {
                                @Override
                                protected void cardSelected(final PhysicalCard combatCard) {
                                    // Pay cost(s)
                                    action.appendCost(
                                            new RefreshPrintedDestinyValuesEffect(action, Collections.singletonList(combatCard)) {
                                                @Override
                                                protected void refreshedPrintedDestinyValues() {
                                                    final float destinyValue = game.getModifiersQuerying().getDestiny(game.getGameState(), combatCard);
                                                    action.setActionMsg("Substitute combat card " + GameUtils.getCardLink(combatCard) + "'s destiny value of " + GuiUtils.formatAsString(destinyValue) + " for " + aboutToDrawDestinyCardResult.getDestinyType().getHumanReadable());
                                                    // Perform result(s)
                                                    action.appendEffect(
                                                            new PutStackedCardInUsedPileEffect(action, playerId, combatCard, true));
                                                    action.appendEffect(
                                                            new UseCombatCardForSubstituteDestinyEffect(action, combatCard, destinyValue));
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