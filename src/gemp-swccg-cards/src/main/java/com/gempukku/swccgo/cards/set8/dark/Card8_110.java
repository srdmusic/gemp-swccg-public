package com.gempukku.swccgo.cards.set8.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;

import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ShieldOneCardAtEachLocationEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Set: Endor
 * Type: Character
 * Subtype: Imperial
 * Title: Navy Trooper Vesden
 */
public class Card8_110 extends AbstractImperial {
    public Card8_110() {
        super(Side.DARK, 3, 2, 2, 1, 3, "Navy Trooper Vesden", Uniqueness.UNIQUE, ExpansionSet.ENDOR, Rarity.U);
        setLore("Counterintelligence agent assigned by ISB. Operates sensors designed to protect the control bunker from infiltration.");
        setGameText("If present with a Scomp link when Never Tell Me The Odds just reached the top of your Reserve Deck, may 'shield' (add 3 to destiny number of) one of your characters at each location for remainder of turn.");
        addIcons(Icon.ENDOR, Icon.WARRIOR);
        addKeywords(Keyword.TROOPER);
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.justRevealedInsertCard(game, effectResult, Filters.Never_Tell_Me_The_Odds)
                && GameConditions.isAtScompLink(game, self)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("'Shield' characters");
            // Perform result(s)
            Collection<PhysicalCard> toShield = Filters.filterActive(game, self, Filters.and(Filters.your(self), Filters.character, Filters.at(Filters.location), Filters.canBeTargetedBy(self)));
            action.appendEffect(
                    new ShieldOneCardAtEachLocationEffect(action, toShield));
            return Collections.singletonList(action);

        }
        return null;
    }
}
