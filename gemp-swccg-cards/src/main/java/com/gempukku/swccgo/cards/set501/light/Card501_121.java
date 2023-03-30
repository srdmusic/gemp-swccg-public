package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.conditions.AttachedCondition;
import com.gempukku.swccgo.cards.conditions.PlayersTurnCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.CancelForceDrainBonusesFromCardModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeReducedModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveForfeitValueIncreasedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetForfeitModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Defensive Shield
 * Title: Let's Keep A Little Optimism Here (V)
 */
public class Card501_121 extends AbstractDefensiveShield {
    public Card501_121() {
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, Title.Lets_Keep_A_Little_Optimism_Here, ExpansionSet.REFLECTIONS_III, Rarity.PM);
        setVirtualSuffix(true);
        setLore("The heroes of the Rebellion know that where there is life, there is hope.");
        setGameText("Plays on opponent's location. During opponent's turn, forfeit values may not be increased here. While on a Renegade planet location, operatives are forfeit = 0, operatives do not add to Force drains, and your Force drains may not be reduced.");
        addIcons(Icon.REFLECTIONS_III, Icon.VIRTUAL_SET_21);
        setTestingText("Let's Keep A Little Optimism Here (V) (Shield)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.opponents(self), Filters.location);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Condition attachedToRenegadePlanetLocation = new AttachedCondition(self, Filters.Renegade_planet_location);
        Filter operatives = Filters.and(Filters.operative, Filters.canBeTargetedBy(self));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotHaveForfeitValueIncreasedModifier(self, Filters.here(self), new PlayersTurnCondition(opponent)));
        modifiers.add(new ResetForfeitModifier(self, operatives, attachedToRenegadePlanetLocation, 0));
        modifiers.add(new CancelForceDrainBonusesFromCardModifier(self, operatives, attachedToRenegadePlanetLocation));
        modifiers.add(new ForceDrainsMayNotBeReducedModifier(self, attachedToRenegadePlanetLocation, playerId));
        return modifiers;
    }
}