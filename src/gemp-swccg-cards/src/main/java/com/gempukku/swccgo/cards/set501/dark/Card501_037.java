package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collections;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractImmediateEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayCardAction;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.PlayCardResult;

/**
 * Set: Playtesting
 * Type: Effect
 * Subtype: Immediate
 * Title: The Client's Bounty
 */
public class Card501_037 extends AbstractImmediateEffect {
    public Card501_037() {
        super(Side.DARK, 4, PlayCardZoneOption.ATTACHED, "The Client's Bounty", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("One of the most profitable occupations in the galaxy is hunting down and capturing wanted beings. The more notable the quarry, the more profitable the venture.");
        setGameText("Deploy on opponent's just deployed character. Once per turn, if a bounty hunter here, may reveal the top card of each player's Reserve Deck. If this character captured and seized, retrieve 2 Force (3 if The Client on table) and return this card to your hand. [Immune to Control.]");
        addIcons(Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Control);
        addKeywords(Keyword.BOUNTY);
        setTestingText("The Client's Bounty");
    }

    @Override
    protected List<PlayCardAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, Filters.and(Filters.opponents(self), Filters.character))) {
            PhysicalCard deployedCard = ((PlayCardResult) effectResult).getPlayedCard();
            PlayCardAction action = getPlayCardAction(playerId, game, self, self, false, 0, null, null, null, null, null, false, 0, Filters.sameCardId(deployedCard), null);
            if (action != null) {
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
