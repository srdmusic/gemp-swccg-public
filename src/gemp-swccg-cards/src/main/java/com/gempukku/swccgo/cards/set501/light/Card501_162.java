package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.MayNotReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Corporal Beezer (V)
 */
public class Card501_162 extends AbstractRebel {
    public Card501_162() {
        super(Side.LIGHT, 3, 2, 2, 2, 4, "Corporal Beezer", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Alderaanian slicer and technician. Trained by Brooks Carlson to serve as a Scout for Alliance commandos. She gets nervous when General Solo tries to hotwire something.");
        setGameText("Prevents opponent's from 'reacting' to same site. Once per turn, if [V] Strike Planning on table, may place a card from hand on Used Pile; the next scout you deploy this turn is deploy -1. Your scouts here are immune to Trample.");
        addIcons(Icon.ENDOR, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.SCOUT, Keyword.FEMALE);
        setSpecies(Species.ALDERAANIAN);
        setVirtualSuffix(true);
        setTestingText("Corporal Beezer (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotReactToLocationModifier(self, Filters.sameSite(self), game.getOpponent(self.getOwner())));
        return modifiers;
    }
}
