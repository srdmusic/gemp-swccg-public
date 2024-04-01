package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.conditions.HasAboardCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.HyperspeedModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Supremacy
 */
public class Card501_113 extends AbstractCapitalStarship {
    public Card501_113() {
        super(Side.DARK, 1, 16, 13, 9, null, 2, 16, Title.Supremacy, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setAsHorizontal(true);
        setGameText("May add unlimited pilots, passengers, [First Order] TIEs, shuttles, and vehicles. If Hux aboard, hyperspeed +2. Immune to attrition < 10 (< 5 if with a Resistance ship).");
        addPersona(Persona.SUPREMACY);
        addIcons(Icon.SCOMP_LINK, Icon.EPISODE_VII, Icon.FIRST_ORDER, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_24);
        addIcon(Icon.PILOT, 1);
        addModelType(ModelType.MEGA_CLASS_DREADNAUGHT);
        setPilotCapacity(Integer.MAX_VALUE);
        setPassengerCapacity(Integer.MAX_VALUE);
        setVehicleCapacity(Integer.MAX_VALUE);
        setStarfighterCapacity(Integer.MAX_VALUE, Filters.or(Filters.shuttle,Filters.and(Filters.First_Order_TIE)));
        setMatchingPilotFilter(Filters.Hux);
        setTestingText(Title.Supremacy);
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(3) {});
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Condition huxAboard = new HasAboardCondition(self, Filters.Hux);
        modifiers.add(new HyperspeedModifier(self, self, huxAboard, 2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, self, new ConditionEvaluator(10, 5, new WithCondition(self, Filters.Resistance_starship))));
        return modifiers;
    }
}
