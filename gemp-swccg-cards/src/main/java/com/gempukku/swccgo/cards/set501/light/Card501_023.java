package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterDevice;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.EachTrainingDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Device
 * Title: Jedi Holocron
 */
public class Card501_023 extends AbstractCharacterDevice {
    public Card501_023() {
        super(Side.LIGHT, 2, "Jedi Holocron", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on your character of ability > 4. Adds 1 to training destiny draws here. Adds 1 to Force drain where present, and the first Force lost is stacked here face down. Opponent’s ability required to draw battle destiny here is +1 for each card stacked.");
        addIcons(Icon.VIRTUAL_SET_15);
        setTestingText("Jedi Holocron");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.abilityMoreThan(4));
    }

    @Override
    protected Filter getGameTextValidToUseDeviceFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.abilityMoreThan(4);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter hasAttached = Filters.hasAttached(self);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new EachTrainingDestinyModifier(self, hasAttached, 1));
        modifiers.add(new ForceDrainModifier(self, Filters.wherePresent(self, hasAttached), 1, self.getOwner()));
        return modifiers;
    }
}
