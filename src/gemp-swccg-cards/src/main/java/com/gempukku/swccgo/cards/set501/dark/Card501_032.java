package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
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

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: B1 Clanker
 */
public class Card501_032 extends AbstractDroid {
    public Card501_032() {
        super(Side.DARK, 0, 1, null, 3, "B1 Clanker", Uniqueness.RESTRICTED_6, ExpansionSet.PLAYTESTING, Rarity.V);
        setArmor(3);
        setLore("Infantry battle droid.");
        setGameText("* Power = number of [Separatist] systems on table. While alone, you may not Force drain here.");
        addIcons(Icon.SEPARATIST, Icon.EPISODE_I, Icon.PRESENCE, Icon.VIRTUAL_SET_27);
        addKeywords(Keyword.INFANTRY_BATTLE_DROID);
        addModelType(ModelType.BATTLE);
        setTestingText("B1 Clanker");
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
