package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.cards.conditions.AloneAtCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.OutOfPlayCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToPowerModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeModifiedModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployToTargetModifier;
import com.gempukku.swccgo.logic.modifiers.MayUseWeaponModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Resistance
 * Title: Finn, Resistance Leader
 */
public class Card501_174 extends AbstractResistance {
    public Card501_174() {
        super(Side.LIGHT, 1, 4, 4, 4, 6, "Finn, Resistance Hero", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader.");
        setGameText("During battle, if Luke out of play (or Rose or Jannah here), adds one destiny to total power. While alone, opponent may not cancel or reduce Force drains at same [E7] battleground. Jedi Lightsaber may deploy on Finn. Immune to attrition < 4.");
        addPersona(Persona.FINN);
        addIcons(Icon.EPISODE_VII, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.LEADER);
        setTestingText("Finn, Resistance Hero");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition lukeOutOfPlay = new OutOfPlayCondition(self, Filters.Luke);
        Condition roseOrJannahHere = new HereCondition(self, Filters.or(Filters.Rose, Filters.Jannah));
        Condition aloneAtEVIIBG = new AloneAtCondition(self, Filters.and(Filters.icon(Icon.EPISODE_VII), Filters.battleground));
        String opponent = game.getOpponent(self.getOwner());

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsDestinyToPowerModifier(self, new OrCondition(lukeOutOfPlay, roseOrJannahHere), 1));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.here(self), aloneAtEVIIBG, opponent, self.getOwner()));
        modifiers.add(new ForceDrainsMayNotBeModifiedModifier(self, Filters.here(self), aloneAtEVIIBG, opponent, self.getOwner()));
        modifiers.add(new MayDeployToTargetModifier(self, Filters.Jedi_Lightsaber, self));
        modifiers.add(new MayUseWeaponModifier(self, Filters.Jedi_Lightsaber));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 4));
        return modifiers;
    }
}
