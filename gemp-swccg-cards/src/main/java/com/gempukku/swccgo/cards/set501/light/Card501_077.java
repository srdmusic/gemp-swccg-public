package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.conditions.HasPilotingCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 20
 * Type: Starship
 * Subtype: Capital
 * Title: Resolute
 */
public class Card501_077 extends AbstractCapitalStarship {
    public Card501_077() {
        super(Side.LIGHT, 2, 6, 7, 6, null, 4, 7, "Resolute", Uniqueness.UNIQUE);
        setGameText("May add 5 pilots, 5 passengers, 5 vehicles, and 5 starfighters. Permanent pilot provides ability of 2. While Anakin or Yularen piloting, immune to attrition < 5 and your [Clone Army] cards at related sites are power +1.");
        addModelType(ModelType.VENATOR_CLASS_ATTACK_CRUISER);
        addIcons(Icon.EPISODE_I, Icon.REPUBLIC, Icon.PILOT, Icon.NAV_COMPUTER, Icon.CLONE_ARMY, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_20);
        setPilotCapacity(5);
        setPassengerCapacity(5);
        setVehicleCapacity(5);
        setStarfighterCapacity(5);
        setMatchingPilotFilter(Filters.or(Filters.Anakin, Filters.title("Admiral Yularen")));
        setTestingText("Resolute");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new HasPilotingCondition(self, Filters.or(Filters.Anakin, Filters.title("Admiral Yularen"))), 5));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(self), Icon.CLONE_ARMY, Filters.at(Filters.relatedSite(self))), new HasPilotingCondition(self, Filters.or(Filters.Anakin, Filters.title("Admiral Yularen"))), 1));
        return modifiers;
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }
}