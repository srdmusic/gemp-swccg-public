package com.gempukku.swccgo.cards.set217.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseCardsFromOffTableSimultaneouslyEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInLostPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToPlaceCardOutOfPlayFromOffTableResult;
import com.gempukku.swccgo.logic.timing.results.AboutToPlaceCardOutOfPlayFromTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Defensive Shield
 * Title: Ounee Ta (V)
 */
public class Card217_043 extends AbstractDefensiveShield {
    public Card217_043() {
        super(Side.LIGHT, Title.Ounee_Ta);
        setVirtualSuffix(true);
        setLore("Jabba's decadent behavior makes him susceptible to deception. Leia and Lando exploited this weakness, posing as Jabba's kind of scum.");
        setGameText("Plays on table. At each opponent's <> site, your Rebels are each deploy -2 and your Force generation is +1. If [Theed Palace] Sidious is about to place your Jedi out of play, may lose 3 Force to place that character in your Lost Pile instead.");
        addIcons(Icon.REFLECTIONS_III, Icon.VIRTUAL_DEFENSIVE_SHIELD);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        Filter opponentsGenericSite = Filters.and(Filters.opponents(self), Filters.generic_site, Filters.canBeTargetedBy(self));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(self), Filters.Rebel), -2, opponentsGenericSite));
        modifiers.add(new ForceGenerationModifier(self, opponentsGenericSite, 1, playerId));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        Filter yourJedi = Filters.and(Filters.your(self), Filters.Jedi);
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.isAboutToBePlacedOutOfPlayFromTable(game, effectResult, opponent, yourJedi)) {
            final AboutToPlaceCardOutOfPlayFromTableResult result = (AboutToPlaceCardOutOfPlayFromTableResult) effectResult;
            final PhysicalCard card = result.getCardToBePlacedOutOfPlay();
            final PhysicalCard source = result.getSourceCard();

            if (source != null
                    && Filters.Sidious.accepts(game, source)) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Place Jedi in Lost Pile");
                action.setActionMsg("Place " + GameUtils.getCardLink(card) + " in Lost Pile instead of being placed out of play");
                action.appendCost(new LoseForceEffect(action, playerId, 3));

                action.allowResponses("Place " + GameUtils.getCardLink(card) + " in Lost Pile",
                        new RespondableEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                result.getPreventableCardEffect().preventEffectOnCard(card);
                                action.appendEffect(
                                        new PlaceCardInLostPileFromTableEffect(action, card));
                            }
                        }
                );
                actions.add(action);
            }
        }

        // Check condition(s)
        if (TriggerConditions.isAboutToBePlacedOutOfPlayFromOffTable(game, effectResult, opponent, yourJedi)) {
            final AboutToPlaceCardOutOfPlayFromOffTableResult result = (AboutToPlaceCardOutOfPlayFromOffTableResult) effectResult;
            final PhysicalCard card = result.getCardToBePlacedOutOfPlay();
            final PhysicalCard source = result.getSourceCard();

            if (source != null
                    && Filters.Sidious.accepts(game, source)) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Place Jedi in Lost Pile");
                action.setActionMsg("Place " + GameUtils.getCardLink(card) + " in Lost Pile instead of being placed out of play");
                action.appendCost(new LoseForceEffect(action, playerId, 3));

                action.allowResponses("Place " + GameUtils.getCardLink(card) + " in Lost Pile",
                        new RespondableEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                result.getPreventableCardEffect().preventEffectOnCard(card);
                                action.appendEffect(
                                        new LoseCardsFromOffTableSimultaneouslyEffect(action, Collections.singleton(card), false));
                            }
                        }
                );
                actions.add(action);
            }
        }

        return actions;
    }
}