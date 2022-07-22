package com.gempukku.swccgo.cards.set501.dark;

//•Daultay Dofine (V) 2
//[Coruscant - R]
//Lore: Neimoidian Trade Federation captain who gained his current position through political backstabbing and family connections. Not favored by Darth Sidious.
//DARK- CHARACTER - REPUBLIC
//POWER 3 ABILITY 3 FORCE-ATTUNED
//Text: Adds 2 to power of anything he pilots. Your total battle destiny at sites is +1 for each of your participating [Presence] droids that did not fire its [Permanent Weapon] weapon in that battle. Weapon destinies at same system are -1.
//DEPLOY 3 FORFEIT 6
//[Coruscant] [Episode I] [Pilot] [Set 19]

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.evaluators.InBattleEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Character
 * Subtype: Republic
 * Title: Daultay Dofine (V)
 */
public class Card501_120 extends AbstractRepublic {
    public Card501_120() {
        super(Side.DARK, 2, 2, 2, 3, 4, "Daultay Dofine", Uniqueness.UNIQUE);
        setLore("Neimoidian Trade Federation captain who gained his current position through political backstabbing and family connections. Not favored by Darth Sidious.");
        setGameText("Adds 2 to power of anything he pilots. Your total battle destiny at sites is +1 for each of your " +
                "participating [Presence] droids that did not fire its [Permanent Weapon] weapon in that battle. " +
                "Weapon destinies at same system are -1.");
        addPersona(Persona.DOFINE);
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.PILOT, Icon.VIRTUAL_SET_19);
        setSpecies(Species.NEIMOIDIAN);
        setTestingText("Daultay Dofine (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new TotalBattleDestinyModifier(self, Filters.site, new InBattleEvaluator(self,
                Filters.and(Icon.PRESENCE, Filters.droid, Filters.didNotFireAPermanentWeaponThisBattle())), self.getOwner(), true));
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.at(Filters.sameSystem(self)), -1));
        return modifiers;
    }
}
