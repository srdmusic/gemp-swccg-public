package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.conditions.HasPilotingCondition;
import com.gempukku.swccgo.cards.conditions.TotalAbilityPilotingMoreThanCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetTotalBattleDestinyModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Starfighter
 * Title: Grogu In Razor Crest
 */
public class Card501_116 extends AbstractStarfighter {
    public Card501_116() {
        super(Side.LIGHT, 1, 6, 5, null, 3, 4, 7, "Grogu In Razor Crest", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 1 pilot and 1 passenger. Permanent pilot is •Grogu, who provides ability of 4. " +
                "Unless opponent has total ability > 6 piloting here, reset opponent's total battle destiny here to 0. " +
                "Immune to attrition < 5 if Din piloting.");
        addPersonas(Persona.RAZOR_CREST);
        addIcons(Icon.SCOMP_LINK, Icon.INDEPENDENT, Icon.PILOT, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_27);
        addModelType(ModelType.ST_70_CLASS_RAZOR_CREST_M_111_ASSAULT_SHIP);
        setPilotCapacity(1);
        setPassengerCapacity(1);
        setMatchingPilotFilter(Filters.Din);
        setTestingText("Grogu In Razor Crest");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(
                new AbstractPermanentPilot(Persona.GROGU, 4) {
                });
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ResetTotalBattleDestinyModifier(self, Filters.here(self), new AndCondition(new InBattleCondition(self),
                new UnlessCondition(new TotalAbilityPilotingMoreThanCondition(opponent, 6, Filters.here(self)))), 0, opponent));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new HasPilotingCondition(self, Filters.Din), 5));
        return modifiers;
    }
}
