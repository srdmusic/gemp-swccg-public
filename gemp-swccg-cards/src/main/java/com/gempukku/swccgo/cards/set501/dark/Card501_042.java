package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ExchangeCardFromLostPileWithStackedCardEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Lost
 * Title: Not Within Sight Or Reach
 */
public class Card501_042 extends AbstractLostInterrupt {
    public Card501_042() {
        super(Side.DARK, 3, "Not Within Sight Or Reach", Uniqueness.UNIQUE);
        setGameText("If Thrawn and Vanto are participating in a battle, place Vanto in Used Pile to cancel all battle damage and attrition against you. OR If Vanto just lost, take Pellaeon into hand from Reserve Deck, reshuffle. OR Take your [Grabber] card on table into hand, place all cards stacked upon it in owner’s Used Pile.");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("Not Within Sight Or Reach");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        // If Thrawn and Vanto are participating in a battle, place Vanto in Used Pile to cancel all battle damage and attrition against you.

        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Take your [Grabber] card on table into hand, place all cards stacked upon it in owner’s Used Pile.

        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // If Vanto just lost, take Pellaeon into hand from Reserve Deck, reshuffle.

        return null;
    }
}