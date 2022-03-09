package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Make Ready To Land Our Troops
 */
public class Card501_014 extends AbstractNormalEffect {
    public Card501_014() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Make_Ready_To_Land_Our_Troops, Uniqueness.UNIQUE);
        setGameText("Deploy on table. Your AT-ATs and snowtroopers are destiny +1. Once per turn, may lose 2 Force to add 1 to your just drawn AT-AT Cannon weapon destiny. If you just deployed an AT-AT, may peek at top two cards of Reserve deck and take one into hand. [Immune to Alter.]");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Make Ready To Land Our Troops");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DestinyModifier(self, Filters.and(Filters.your(self), Filters.or(Filters.AT_AT, Filters.snowtrooper)), 1));
        return modifiers;
    }
    
    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // AT-AT Cannon weapon destiny draw trigger
        if (TriggerConditions.isWeaponDestinyJustDrawnBy(game, effectResult, playerId, Filters.AT_AT_Cannon)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Add 1 to weapon destiny");
            action.setActionMsg("Lose 2 Force to add 1 to your just drawn AT-AT Cannon weapon destiny");
            
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));

            // Pay Costs
            action.appendCost(
                    new LoseForceEffect(action, playerId, 2)
            );
            
            action.appendEffect(
                new ModifyDestinyEffect(action, 1));

            actions.add(action);
        }

        // Just deployed AT-AT trigger

        if(TriggerConditions.justDeployed(game, effectResult, playerId, Filters.AT_AT)
                && GameConditions.hasReserveDeck(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Peek at top two cards of Reserve Deck and take one into hand");
            action.appendEffect(
                    new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, 2, 1, 1));
            
            actions.add(action);
        }

        return actions;
    }
}