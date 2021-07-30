package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.conditions.PresentAtCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Alien
 * Title: Grogu
 */
public class Card501_012 extends AbstractAlien {
    public Card501_012() {
        super(Side.LIGHT, 2, 4, 2, 4, 6, "Grogu", Uniqueness.UNIQUE);
        setLore("");
        setGameText("During battle here, Interrupts may not be played unless they are [Immune to Sense]. Opponent may not target him with weapons unless each of your Mandalorians and non-[Episode I] Jedi present are 'hit.' Immune to attrition < 3.");
        addIcons(Icon.VIRTUAL_SET_16);
        setTestingText("Grogu");
//        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotPlayUnlessImmuneToSpecificTitleModifier(self, Filters.Interrupt, new InBattleCondition(self), Title.Sense));
        modifiers.add(new MayNotBeTargetedByWeaponsModifier(self, new PresentAtCondition(Filters.and(Filters.your(self), Filters.or(Filters.Mandalorian, Filters.and(Filters.not(Icon.EPISODE_I), Filters.Jedi))), Filters.here(self))));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }
}
