package com.gempukku.swccgo.cards.set501.dark;

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
 * Title: Leave Them To Me (V)
 */
public class Card501_116 extends AbstractDefensiveShield {
    public Card501_116() {
        super(Side.DARK, PlayCardZoneOption.ATTACHED, Title.Leave_Them_To_Me, ExpansionSet.REFLECTIONS_III, Rarity.PM);
        setVirtualSuffix(true);
        setLore("'I will deal with them myself.'");
        setGameText("Plays on opponent's location. During opponent's turn, forfeit values may not be increased here. While on a Subjugated planet location, operatives are forfeit = 0, operatives do not add to Force drains, and your Force drains may not be reduced.");
        addIcons(Icon.REFLECTIONS_III, Icon.VIRTUAL_SET_21);
        setTestingText("Leave Them To Me (V) (Shield)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.opponents(self), Filters.location);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Condition attachedToSubjugatedPlanetLocation = new AttachedCondition(self, Filters.Subjugated_planet_location);
        Filter operatives = Filters.and(Filters.operative, Filters.canBeTargetedBy(self));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotHaveForfeitValueIncreasedModifier(self, Filters.here(self), new PlayersTurnCondition(opponent)));
        modifiers.add(new ResetForfeitModifier(self, operatives, attachedToSubjugatedPlanetLocation, 0));
        modifiers.add(new CancelForceDrainBonusesFromCardModifier(self, operatives, attachedToSubjugatedPlanetLocation));
        modifiers.add(new ForceDrainsMayNotBeReducedModifier(self, attachedToSubjugatedPlanetLocation, playerId));
        return modifiers;
    }
}