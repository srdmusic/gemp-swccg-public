package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.DestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.MovesForFreeModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Melas (V)
 */
public class Card501_215 extends AbstractAlien {
    public Card501_215() {
        super(Side.LIGHT, 2, 3, 2, 4, 4, "Melas", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Sarkan smuggler. Smokes an Essoomian gruu pipe to heighten awareness. Exiled from his home planet of Sarka for displaying curiosity in other aliens. Misses his homeworld.");
        setGameText("[Pilot] 2. Your StarSpeeders are destiny +2 and move for free. Once per turn, may place a card from hand on Used Pile to 'smoke pipe' (for remainder of turn, Melas is power and defense value +2 and may not move).");
        addIcons(Icon.SPECIAL_EDITION, Icon.PILOT, Icon.VIRTUAL_SET_1);
        setSpecies(Species.SARKAN);
        addKeywords(Keyword.SMUGGLER);
        setTestingText("Melas (V) (ERRATA)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));

        Filter yourStarSpeederFilter = Filters.and(Filters.your(self), Filters.titleContains("StarSpeeder"));
        modifiers.add(new DestinyModifier(self, yourStarSpeederFilter, 2));
        modifiers.add(new MovesForFreeModifier(self, yourStarSpeederFilter));
        return modifiers;
    }
}
