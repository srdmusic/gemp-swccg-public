package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Starship
 * Subtype: Starfighter
 * Title: Jango Fett & Boba Fett In Slave I
 */
public class Card501_052 extends AbstractStarfighter {
    public Card501_052() {
        super(Side.DARK, 2, 7, 8, null, 6, 5, 8, "Jango Fett & Boba Fett In Slave I", Uniqueness.UNIQUE);
        setComboCard(true);
        setGameText("Permanent pilots are •Jango and •Boba, who provide ability of 6. May add 2 passengers. Power +2 at Nal Hutta or with Falcon. Immune to Eject, Eject! and attrition < 5.");
        addPersonas(Persona.SLAVE_I);
        setPassengerCapacity(2);
        addIcons(Icon.TRADE_FEDERATION, Icon.EPISODE_I, Icon.INDEPENDENT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_20);
        addIcon(Icon.PILOT, 2);
        addModelType(ModelType.FIRESPRAY_CLASS_ATTACK_SHIP);
        setTestingText("~Jango Fett & Boba Fett In Slave I");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<AbstractPermanentAboard>();
        permanentsAboard.add(new AbstractPermanentPilot(Persona.JANGO_FETT, 3) {
        });
        permanentsAboard.add(new AbstractPermanentPilot(Persona.BOBA_FETT, 3) {
        });
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self,new OrCondition(new AtCondition(self, Filters.Nal_Hutta_system), new WithCondition(self, Filters.Falcon)),2));
        modifiers.add(new ImmuneToTitleModifier(self, Title.Eject_Eject));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }
}