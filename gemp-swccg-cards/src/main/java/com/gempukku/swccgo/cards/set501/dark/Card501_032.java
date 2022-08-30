package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 20
 * Type: Character
 * Subtype: Droid
 * Title: B-1 Battle Droid
 */
public class Card501_032 extends AbstractDroid {
    public Card501_032() {
        super(Side.DARK, 2, 2, 1, 2, "B-1 Battle Droid");
        setArmor(3);
        setLore("Infantry battle droid.");
        setGameText("While with a Republic leader (or another battle droid), draws one battle destiny if unable to otherwise.");
        addIcons(Icon.SEPARATIST, Icon.EPISODE_I, Icon.PRESENCE, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.INFANTRY_BATTLE_DROID);
        addModelType(ModelType.BATTLE);
        setTestingText("B-1 Battle Droid");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, new WithCondition(self, Filters.or(Filters.and(Filters.Republic_character, Filters.leader), Filters.battle_droid)), 1));
        return modifiers;
    }
}
