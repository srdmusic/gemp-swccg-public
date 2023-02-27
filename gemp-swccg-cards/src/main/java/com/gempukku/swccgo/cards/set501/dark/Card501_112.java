package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Starship
 * Subtype: Capital
 * Title: Fulminatrix
 */
public class Card501_112 extends AbstractCapitalStarship {
    public Card501_112() {
        super(Side.DARK, 3, 9, 10, 8, null, 3, 9, Title.Fulminatrix, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 5 pilots, 4 passengers, 3 TIEs, and 2 vehicles. Permanent pilot provides ability of 2. Deploy -3 to same system as Tracked Fleet. Opponent must lose 1 force to move a starship from here.");
        addIcons(Icon.NAV_COMPUTER, Icon.EPISODE_VII, Icon.SCOMP_LINK, Icon.FIRST_ORDER, Icon.VIRTUAL_SET_22);
        addIcon(Icon.PILOT, 1);
        addModelType(ModelType.MANDATOR_IV_CLASS_DREADNAUGHT);
        setPilotCapacity(5);
        setPassengerCapacity(4);
        setStarfighterCapacity(3, Filters.First_Order_TIE);
        setVehicleCapacity(2);
        setTestingText(Title.Fulminatrix);
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(2) {});
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostModifier(self, new OnTableCondition(self, Filters.Tracked_Fleet), -3));
        return modifiers;
    }

    // ADD LOGIC FOR "Lose 1 Force to move"
}