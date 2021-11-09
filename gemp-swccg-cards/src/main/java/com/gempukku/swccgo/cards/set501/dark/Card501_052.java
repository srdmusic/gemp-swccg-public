package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
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
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DrawCardsIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardsIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Unlimited Power!
 */
public class Card501_052 extends AbstractNormalEffect {
    public Card501_052() {
        super(Side.DARK, 1, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Unlimited Power!", Uniqueness.UNIQUE);
        setLore("Eliciting fear from the opponent gives the dark side a powerful advantage.");
        setGameText("Deploy on table.  Emperor, Maul, and your Dathomirians are lost. At the start of your turn, if Sidious on Coruscant (or Insidious Prisoner on table), may place two cards from hand on Reserve Deck, reshuffle, and draw two cards from Reserve Deck. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.SIDIOUS, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Unlimited Power!");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if(TriggerConditions.isStartOfYourTurn(game, effectResult, playerId)
                && GameConditions.numCardsInHand(game, playerId)>=2
                && GameConditions.hasReserveDeck(game, playerId)
                && (GameConditions.canSpot(game, self, Filters.and(Filters.Sidious, Filters.on(Title.Coruscant)))
                || GameConditions.canSpot(game, self, Filters.Insidious_Prisoner))) {

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

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {

        // Emperor, Maul, and your Dathomirians are lost.
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)) {
            Collection<PhysicalCard> toBeLost =
                    Filters.filterActive(game, self, Filters.or(Filters.Emperor, Filters.Maul, Filters.and(Filters.your(self), Filters.species(Species.DATHOMIRIAN))));
            if (!toBeLost.isEmpty()) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Emperor, Maul, and your Dathomirians lost");
                action.setActionMsg("Make " + GameUtils.getAppendedNames(toBeLost) + " lost");

                // Perform result(s)
                action.appendEffect(
                        new LoseCardsFromTableEffect(action, toBeLost));
                actions.add(action);
            }
        }
        return actions;
    }
}
