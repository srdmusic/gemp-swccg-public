package com.gempukku.swccgo.cards.set501.dark;

import java.util.ArrayList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractDarkJediMaster;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Dark Jedi Master
 * Title: Master Sidious
 */
public class Card501_050 extends AbstractDarkJediMaster {
    public Card501_050() {
        super(Side.DARK, 1, 6, 5, 7, 8, "Master Sidious", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader. Trade Federation.");
        setGameText("While alone on Coruscant, your apprentice is immune to attrition. Disarmed is canceled here. During your move phase, if at a site you control, may use 1 Force to relocate between a Coruscant site and your apprentice's site. Immune to attrition.");
        addIcons(Icon.EPISODE_I, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.LEADER);
        addPersona(Persona.SIDIOUS);
        setTestingText("Master Sidious");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        Condition aloneOnCoruscantCondition = new AndCondition(new AloneCondition(self), new OnCondition(self, Title.Coruscant));

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ImmuneToAttritionModifier(self, Filters.Sith_Apprentice, aloneOnCoruscantCondition));
        modifiers.add(new ImmuneToAttritionModifier(self));
        return modifiers;
    }
}
