package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
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
        setGameText("Deploy on table. Force loss from Main Power Generators may not be reduced. If Main Power Generators 'blown away,' may retrieve any non-vehicle card without ability into hand whenever you deploy an AT-AT. [Immune to Alter.]");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("The Shield Will Be Down In Moments");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        //TODO force loss from MPG may not be reduced
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if(GameConditions.isBlownAway(game, Filters.title(Title.Main_Power_Generators, true))
            && TriggerConditions.justDeployed(game, effectResult, playerId, Filters.AT_AT)
            && GameConditions.hasLostPile(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve card into hand");
            action.setText("Retrieve any non-vehicle card without ability into hand");
            action.appendEffect(
                    new RetrieveCardIntoHandEffect(action, playerId, Filters.and(Filters.not(Filters.vehicle), Filters.not(Filters.hasAbilityOrHasPermanentPilotWithAbility))));

            return Collections.singletonList(action);
        }

        return null;
    }
}