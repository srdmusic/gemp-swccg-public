package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.conditions.ArmedWithCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
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
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostToTargetModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Character
 * Subtype: Rebel
 * Title: Young Skywalker
 */
public class Card501_087 extends AbstractRebel {
    public Card501_087() {
        super(Side.LIGHT, 6, 8, 6, 6, 9, "Young Skywalker", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Scout.");
        setGameText("[Pilot] 2. Deploys -3 to Endor. " +
                "A Jedi’s Fury does not require His Destiny on table and may be played if a battle just initiated. " +
                "Power +2 if armed with Luke's Lightsaber. " +
                "Immune to attrition < 5 (< 6 while armed with Luke's Lightsaber).");
        addPersona(Persona.LUKE);
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_22);
        addKeywords(Keyword.SCOUT);
        setTestingText("Young Skywalker");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToTargetModifier(self, -3, Filters.and(Filters.Endor_location, Filters.battleground)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition armedWithLukesLightsaber = new ArmedWithCondition(self, Filters.Lukes_Lightsaber);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new ModifyGameTextModifier(self, Filters.title("A Jedi's Fury"), ModifyGameTextType.A_JEDIS_FURY__HAS_NO_REQUIREMENT_AND_PLAYS_IN_BATTLE_JUST_INITIATED));
        modifiers.add(new PowerModifier(self, armedWithLukesLightsaber, 2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new ConditionEvaluator(5, 6, armedWithLukesLightsaber)));
        return modifiers;
    }
}
