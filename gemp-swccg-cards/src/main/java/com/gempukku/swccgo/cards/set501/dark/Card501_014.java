package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: The Shield Will Be Down In Moments
 */
public class Card501_014 extends AbstractNormalEffect {
    public Card501_014() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "The Shield Will Be Down In Moments", Uniqueness.UNIQUE);
        setLore("Death Squadron.");
        setGameText("Deploy on table. If you just deployed an AT-AT, may peek at top two cards of reserve deck and take one into hand (if 1st marker 'blown away,' may retrieve a non-vehicle, non-droid card without ability into hand instead). [Immune to Alter.]");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("The Shield Will Be Down In Moments");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        if(TriggerConditions.justDeployed(game, effectResult, playerId, Filters.AT_AT)) {
            final OptionalGameTextTriggerAction action1 = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action1.setText("Peek at top two cards of reserve deck and take one into hand");
            action1.appendEffect(
                new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action1, playerId, 2, 1, 1));

            actions.add(action1);
            
            if (GameConditions.isBlownAway(game, Filters.title(Title.Main_Power_Generators, true)) && GameConditions.hasLostPile(game, playerId)) {
                final OptionalGameTextTriggerAction action2 = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action2.setText("Retrieve any non-vehicle, non-droid card without ability into hand");
                action2.appendEffect(
                        new RetrieveCardIntoHandEffect(action2, playerId, Filters.and(Filters.not(Filters.vehicle), Filters.not(Filters.droid), Filters.not(Filters.hasAbilityOrHasPermanentPilotWithAbility))));
                actions.add(action2);
            }

            return actions;
        }

        return null;
    }
}