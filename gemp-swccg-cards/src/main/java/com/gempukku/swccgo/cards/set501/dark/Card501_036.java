package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.ImmunityToAttritionLimitedToModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Dominator (V)
 */
public class Card501_036 extends AbstractCapitalStarship {
    public Card501_036() {
        super(Side.DARK, 1, 6, 6, 5, null, 4, 7, Title.Dominator, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Victory-class hull overhauled with powerful thrusters and latest hyperdrive technology. Engineered to support task forces combating Rebel starfighters.");
        setGameText("May add 4 pilots and 6 passengers. " +
                "Permanent pilots provide ability of 4. " +
                "Haven is suspended (and attrition immunity of starfighters is limited to < 5) here. " +
                "Immune to attrition < 4 (< 6 if with opponent's starfighter).");
        addIcon(Icon.PILOT, 2);
        addIcons(Icon.DEATH_STAR_II, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_23);
        addModelType(ModelType.VICTORY_CLASS_STAR_DESTROYER);
        setPilotCapacity(4);
        setPassengerCapacity(6);
        setTestingText("Dominator (V)");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(2) {
        });
        permanentsAboard.add(new AbstractPermanentPilot(2) {
        });
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Filter starfightersHereWithMoreThan5ITA = Filters.and(
                Filters.opponents(self.getOwner()),
                Filters.starfighter,
                Filters.immunityToAttritionMoreThan(5),
                Filters.here(self));
        modifiers.add(new SuspendsCardModifier(self, Filters.and(Filters.Haven, Filters.here(self))));
        modifiers.add(new ImmunityToAttritionLimitedToModifier(self, starfightersHereWithMoreThan5ITA, 5));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new ConditionEvaluator(4, 6,
                new WithCondition(self, Filters.and(Filters.opponents(self.getOwner()), Filters.starfighter)))));
        return modifiers;
    }
}
