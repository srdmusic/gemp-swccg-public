package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Alien
 * Title: Chewbacca, Defender Of Kashyyyk
 */
public class Card501_081 extends AbstractAlien {
    public Card501_081() {
        super(Side.LIGHT, 1, 4, 6, 2, 7, "Chewbacca, Defender Of Kashyyyk", Uniqueness.UNIQUE);
        setLore("Wookiee scout. Volunteered for Han's Endor strike team. Keeps his distance, but doesn't look like he's keeping his distance. Always thinks with his stomach.");
        setGameText("Adds 2 to power of anything he pilots. Your total power here is +1 for each opponent's character present. While your total power here is greater than opponent's total power, adds one destiny to total power.");
        addPersona(Persona.CHEWIE);
        setSpecies(Species.WOOKIEE);
        addKeywords(Keyword.SCOUT);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.ENDOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setTestingText("Chewbacca, Defender Of Kashyyyk");
        hideFromDeckBuilder();
    }

    @Override
    public final boolean hasSpecialDefenseValueAttribute() {
        return true;
    }

    @Override
    public final float getSpecialDefenseValue() {
        return 4;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        return modifiers;
    }
}
