package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractFirstOrder;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PresentAtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
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
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.AddsBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToPowerModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.BattleResultDeterminedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 22
 * Type: Character
 * Subtype: First Order
 * Title: Supreme Leader Kylo Ren
 */
public class Card501_104 extends AbstractFirstOrder {
    public Card501_104() {
        super(Side.DARK, 1, 6, 6, 5, 8, "Supreme Leader Kylo Ren", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader.");
        setGameText("Adds 2 to power of anything he pilots. Snoke is forfeit -6. Once per turn, if an [E7] Objective on table, may take an [E7] character or starship drawn for destiny into hand. While with two First Order characters, adds one battle destiny. Immune to attrition < 5.");
        addPersona(Persona.KYLO);
        addIcons(Icon.EPISODE_VII, Icon.WARRIOR, Icon.VIRTUAL_SET_4);
        addKeywords(Keyword.LEADER);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new ForfeitModifier(self, Filters.Snoke, -6));
        modifiers.add(new AddsBattleDestinyModifier(self, new WithCondition(self, 2, Filters.and(Filters.your(self), Filters.first_order_character)), 1));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }

    //TODO Once Per Turn Logic

}
