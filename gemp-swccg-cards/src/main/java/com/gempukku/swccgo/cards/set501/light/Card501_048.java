package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.conditions.GameTextModificationCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.PlayCardAction;
import com.gempukku.swccgo.logic.conditions.*;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.MayNotDrawMoreThanBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
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
 * Title: Another Pathetic Lifeform (V)
 */
public class Card501_048 extends AbstractDefensiveShield {
    public Card501_048() {
        super(Side.LIGHT, "Another Pathetic Lifeform");
        setVirtualSuffix(true);
        setLore("Young Obi-Wan has much to learn about the living Force. Patience with others is also high on that list.");
        setGameText("Plays on table. While opponent has a non-unique alien or non-unique starfighter in battle, opponent may not draw more than two battle destiny. If your Jedi about to be placed out of play by Sidious's game text, may lose 2 Force to make that character lost instead.");
        addIcons(Icon.REFLECTIONS_III, Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("Another Pathetic Lifeform (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());

        Condition modified = new GameTextModificationCondition(self, ModifyGameTextType.LEGACY__REF_III_ANOTHER_PATHETIC_LIFEFORM__IGNORES_YOUR_NONUNIQUE_ALIENS);

        Condition condition = new OrCondition(new AndCondition(new NotCondition(modified),
                new InBattleCondition(self, Filters.and(Filters.opponents(self), Filters.non_unique, Filters.or(Filters.alien, Filters.starfighter)))),
                new InBattleCondition(self, Filters.and(Filters.opponents(self), Filters.non_unique, Filters.starfighter)));
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotDrawMoreThanBattleDestinyModifier(self, condition, 2, opponent));
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
                action.appendCost(new LoseForceEffect(action, playerId, 2));

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
                action.appendCost(new LoseForceEffect(action, playerId, 2));

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