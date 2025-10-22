package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Rystall (V)
 */
public class Card501_062 extends AbstractAlien {
    public Card501_062() {
        super(Side.DARK, 3, 2, 1, 2, 3, "Rystall", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Musician. Raised by Ortolans. Grew up on the streets of Coruscant. Rescued from the Black Sun crime cartel by Lando Calrissian.");
        setGameText("Power and forfeit +2 at a Coruscant site. Once per turn, if you just deployed a Black Sun agent to same site, may retrieve 1 Force. If opponent's [Maintenance] card just deployed here, it may not battle for remainder of turn.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.MUSICIAN, Keyword.FEMALE);
        setVirtualSuffix(true);
        setTestingText("Rystall (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition atCoruscantSite = new AtCondition(self, Filters.Coruscant_site);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, atCoruscantSite, 2));
        modifiers.add(new ForfeitModifier(self, atCoruscantSite, 2));
        return modifiers;
    }
}
