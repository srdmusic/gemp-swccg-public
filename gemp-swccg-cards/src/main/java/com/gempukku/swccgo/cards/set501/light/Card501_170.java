package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
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
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToDeployCardToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Greef Karga, Disgraced Magistrate
 */
public class Card501_170 extends AbstractAlien {
    public Card501_170() {
        super(Side.LIGHT, 3, 3, 3, 3, 5, "Greef Karga, Disgraced Magistrate", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader. Information broker.");
        setGameText("Opponent must first use 1 Force to deploy a character to same location as Holopuck or 'The Asset'. " +
                "Draws one battle destiny if unable to otherwise. " +
                "Immune to attrition < 3.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.LEADER, Keyword.INFORMATION_BROKER);
        addPersona(Persona.GREEF);
        setTestingText("Greef Karga, Disgraced Magistrate");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ExtraForceCostToDeployCardToLocationModifier(self,
                Filters.and(Filters.opponents(self), Filters.character),
                new ConstantEvaluator(1),
                Filters.sameLocationAs(self, Filters.or(Filters.Holopuck, Filters.The_Asset))));
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, 1));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }
}
