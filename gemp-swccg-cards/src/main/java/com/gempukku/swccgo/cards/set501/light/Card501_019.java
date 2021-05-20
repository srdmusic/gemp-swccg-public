package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.takeandputcards.StackCardsFromHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardInLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.modifiers.MovesFreeFromLocationToLocationModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 15
 * Type: Effect
 * Title: A Power Loss
 */
public class Card501_019 extends AbstractEpicEventDeployable {
    public Card501_019() {
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, Title.A_Power_Loss, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on Central Core. Once per game, opponent may stack up to 4 cards from hand face-down here." +
                "Allow The Ship To Leave: If you just won a battle on Death Star (or occupy two Death Star battlegrounds at the start of your turn), place a card stacked here in owner’s Lost Pile." +
                "That Old Man Did It!: If you occupy this site and an additional Death Star site while no cards stacked here, power ‘shut down’ (place this card out of play; your movement this turn between Death Star locations is free).]");
        addIcons(Icon.VIRTUAL_SET_15);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("A Power Loss");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Death_Star_Central_Core;
    }

    @Override
    protected List<TopLevelGameTextAction> getOpponentsCardGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.A_POWER_LOSS__STACK_CARDS;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.hasHand(game, playerId)) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Stack cards from hand");
            action.setActionMsg("Stack cards from hand");
            action.appendUsage(
                    new OncePerGameEffect(action)
            );
            action.appendEffect(
                    new StackCardsFromHandEffect(action, playerId, 0, 4, self, true)
            );
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new ArrayList<>();

        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        if (GameConditions.hasStackedCards(game, self)
                && (TriggerConditions.wonBattleAt(game, effectResult, playerId, Filters.on(Title.Death_Star))
                || (TriggerConditions.isStartOfYourTurn(game, effectResult, self)
                && GameConditions.occupies(game, playerId, 2, Filters.and(Filters.battleground, Filters.Death_Star_location))))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Place a card stacked here in " + opponent + "'s Lost Pile");
            action.appendTargeting(
                    new ChooseStackedCardEffect(action, playerId, self) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            action.appendEffect(
                                    new PutStackedCardInLostPileEffect(action, playerId, selectedCard, false)
                            );

                        }
                    }
            );

            actions.add(action);
        }

        if (!GameConditions.hasStackedCards(game, self)
                && GameConditions.occupies(game, playerId, self.getAttachedTo())
                && GameConditions.occupies(game, playerId, Filters.and(Filters.Death_Star_site, Filters.other(self.getAttachedTo())))
                && !game.getModifiersQuerying().isDeathStarPowerShutDown()) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Death Star Power is 'shut down'");
            action.setSingletonTrigger(true);
            game.getModifiersQuerying().deathStarPowerIsShutDown();
            action.appendEffect(
                    new AddUntilEndOfTurnModifierEffect(
                            action, new MovesFreeFromLocationToLocationModifier(self, Filters.your(playerId), Filters.Death_Star_location, Filters.Death_Star_location), ""
                    )
            );
            action.appendEffect(
                    new PlaceCardOutOfPlayFromTableEffect(action, self)
            );

            actions.add(action);
        }

        return actions;
    }
}
