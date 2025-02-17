package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractFirstOrder;
import com.gempukku.swccgo.cards.evaluators.OutOfPlayEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: First Order
 * Title: Vicrul
 */
public class Card501_016 extends AbstractFirstOrder {
    public Card501_016() {
        super(Side.DARK, 2, 4, 4, 4, 7, "Vicrul", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Knight of Ren.");
        setGameText("Power +1 for each opponent's character out of play. Opponent may not cancel your battle destiny draws where you have Kylo or a Knight of Ren. If you just initiated a Force drain (or won a battle) here, may place bottom card of opponent's Lost Pile out of play.");
        addIcons(Icon.EPISODE_VII, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.KNIGHT_OF_REN);
        setTestingText("Vicrul");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, new OutOfPlayEvaluator(self, Filters.and(Filters.opponents(self), Filters.character)) ));
        return modifiers;
    }
}
