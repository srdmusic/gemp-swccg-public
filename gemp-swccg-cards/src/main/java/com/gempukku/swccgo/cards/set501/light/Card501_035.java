package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DrawCardsIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.IgnoresDeploymentRestrictionsFromCardModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Kind, But Sad
 */
public class Card501_035 extends AbstractNormalEffect {
    public Card501_035() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Kind, But Sad", Uniqueness.UNIQUE);
        setGameText("Deploy on table. At the start of your turn, if Prophecy Of The Force on table, may place two cards from hand on Reserve Deck, reshuffle, and draw two cards from Reserve Deck. Amidala ignores [Set 8] objective deployment restrictions. [Immune to Alter]");
        addIcons(Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Kind, But Sad");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, Filters.Amidala, null, self.getOwner(), Filters.and(Filters.your(self), Icon.VIRTUAL_SET_8, Filters.Objective)));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if(TriggerConditions.isStartOfYourTurn(game, effectResult, playerId)
            && GameConditions.numCardsInHand(game, playerId)>=2
            && GameConditions.canSpot(game, self, Filters.Prophecy_Of_The_Force)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Draw cards from Reserve Deck");

            action.appendCost(
                    new PutCardsFromHandOnReserveDeckEffect(action, playerId, 2, 2));
            action.appendEffect(
                    new ShuffleReserveDeckEffect(action, playerId));
            action.appendEffect(
                    new DrawCardsIntoHandFromReserveDeckEffect(action, playerId, 2));

            return Collections.singletonList(action);
        }

        return null;
    }
}