package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DefinedByGameTextPowerModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotForceDrainAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: Swarming Vulture Droid
 */
public class Card501_004 extends AbstractStarfighter {
    public Card501_004() {
        super(Side.DARK, 0, 1, null, null, 3, null, 3, "Swarming Vulture Droid", Uniqueness.RESTRICTED_6, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("* Power = number of [Separatist] systems on table. While alone, you may not Force drain here. If just lost, may stack on Droid Racks.");
        addIcons(Icon.EPISODE_I, Icon.SEPARATIST, Icon.TRADE_FEDERATION, Icon.PILOT, Icon.PRESENCE, Icon.VIRTUAL_SET_27);
        setTestingText("Swarming Vulture Droid");
        addModelType(ModelType.DROID_STARFIGHTER);
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DefinedByGameTextPowerModifier(self, new OnTableEvaluator(self, Filters.and(Icon.SEPARATIST, Filters.system))));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotForceDrainAtLocationModifier(self, Filters.sameLocation(self), new AloneCondition(self), playerId));
        return modifiers;
    }
}
