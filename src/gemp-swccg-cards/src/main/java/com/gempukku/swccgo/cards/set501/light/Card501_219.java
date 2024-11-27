package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCombatVehicle;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.DeploysFreeAboardModifier;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.MayMoveAsReactModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Vehicle
 * Subtype: Combat
 * Title: Zev In Rogue 2
 */
public class Card501_219 extends AbstractCombatVehicle {
    public Card501_219(){
        super(Side.LIGHT, 2, 6, 6, null, 6, 4, 6, "Zev In Rogue 2", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Enclosed. First snowspeeder to be successfully adapted to Hoth's environment. Piloted by Zev Senesca. Led team in search of Captain Solo and Commander Skywalker.");
        setGameText("Deploys -2 to Hoth. May add 1 pilot. Permanent pilot is •Zev, who provides ability of 2. Draws one battle destiny if unable to otherwise. May move as a 'react.' Vehicle weapons deploy free aboard.");
        addModelType(ModelType.T_47);
        addPersona(Persona.ROGUE2);
        addIcons(Icon.PILOT, Icon.HOTH, Icon.VIRTUAL_SET_24);
        addKeywords(Keyword.ENCLOSED, Keyword.SNOWSPEEDER, Keyword.ROGUE_SQUADRON);
        setPilotCapacity(1);
        setTestingText("Zev In Rogue 2");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToLocationModifier(self, -2, Filters.Deploys_at_Hoth));
        return modifiers;
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(Persona.ZEV, 2) {});
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, 1));
        modifiers.add(new MayMoveAsReactModifier(self));
        modifiers.add(new DeploysFreeAboardModifier(self, Filters.and(Filters.your(self), Filters.vehicle_weapon), self));
        return modifiers;
    }
}
