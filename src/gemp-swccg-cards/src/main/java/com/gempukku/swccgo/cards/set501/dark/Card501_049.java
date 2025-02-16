package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Republic
 * Title: Tey How (V)
 */
public class Card501_049 extends AbstractRepublic {
    public Card501_049() {
        super(Side.DARK, 2, 2, 2, 2, 4, "Tey How", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Neimoidian Trade Federation communications officer to Nute Gunray. Had audio and visual mechanics surgically implanted to assist her in shipboard operations.");
        setGameText("Adds 2 to the power of anything she pilots. Once per game may deploy a device on a capital starship she is piloting. While piloting a [Trade Federation] capital starship, opponent may not cancel your battle destiny draws where you have a character with \"Trade Federation\" in lore.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.PILOT, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.FEMALE);
        setSpecies(Species.NEIMOIDIAN);
        setVirtualSuffix(true);
        setTestingText("Tey How (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        return modifiers;
    }
}
