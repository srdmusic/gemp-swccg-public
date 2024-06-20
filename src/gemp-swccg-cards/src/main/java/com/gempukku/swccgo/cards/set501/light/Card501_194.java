package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.IgnoresObjectiveRestrictionsWhenForceDrainingAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeExcludedFromBattle;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveGameTextCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetAbilityModifier;

import java.util.LinkedList;
import java.util.List;

/*
 * Set: Set 23
 * Type: Character
 * Subtype: Rebel
 * Title: Boushh (V)
 */
public class Card501_194 extends AbstractRebel {
    public Card501_194() {
        super(Side.LIGHT, 1, 4, 4, 4, 6, Title.Boushh, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leia obtained the armor of a notorious mercenary to sneak onto Coruscant. She later assumed the same role to spy on Jabba. Fearless and inventive. Jabba's kind of scum.");
        setGameText("At Jabba's Palace sites where you have no Jedi, " +
                "you may Force drain regardless of your [Premium] objective restrictions. " +
                "Characters may not be excluded from battle here. " +
                "While alone with frozen Han, Leia's ability = 0 and her game text may not be canceled.");
        setArmor(5);
        addIcons(Icon.PREMIUM, Icon.JABBAS_PALACE, Icon.PILOT, Icon.WARRIOR);
        addKeywords(Keyword.SPY, Keyword.FEMALE);
        addPersona(Persona.LEIA);
        setSpecies(Species.ALDERAANIAN);
        setVirtualSuffix(true);
        setTestingText("Boushh (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        modifiers.add(new MayNotBeExcludedFromBattle(self, Filters.and(Filters.here(self), Filters.character)));
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self,
                Filters.and(Filters.Leia, Filters.alone,
                        Filters.with(self, SpotOverride.INCLUDE_CAPTIVE, Filters.and(Filters.frozenCaptive, Filters.Han)))));
        modifiers.add(new ResetAbilityModifier(self,
                Filters.and(Filters.Leia, Filters.alone,
                        Filters.with(self, SpotOverride.INCLUDE_CAPTIVE, Filters.and(Filters.frozenCaptive, Filters.Han))), 0));
        modifiers.add(new IgnoresObjectiveRestrictionsWhenForceDrainingAtLocationModifier(self,
                Filters.and(Filters.Jabbas_Palace_site, Filters.not(Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.Jedi)))), self.getOwner()));

        return modifiers;
    }
}
