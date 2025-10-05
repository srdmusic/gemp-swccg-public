package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.conditions.GameTextModificationCondition;
import com.gempukku.swccgo.cards.conditions.HitCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.conditions.PresentWithCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: Guri (V)
 */
public class Card501_060 extends AbstractDroid {
    public Card501_060() {
        super(Side.DARK, 2, 6, 6, 6, Title.Guri, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setArmor(5);
        setLore("Human-replica droid. Programmed to function as Xizor's personal bodyguard and assassin. Black Sun agent. Cost 9 million credits. Worth every decicred.");
        setGameText("[Pilot] 2. Draws one battle destiny if unable to otherwise. While present at a site with Xizor, Force drain +1 here and he may not be targeted by weapons (unless Guri 'hit'). If your [Reflections II] objective on table, immune to attrition < 5.");
        addIcons(Icon.REFLECTIONS_II, Icon.PILOT, Icon.WARRIOR, Icon.PRESENCE, Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.FEMALE, Keyword.BLACK_SUN_AGENT, Keyword.BODYGUARD, Keyword.ASSASSIN);
        addModelTypes(ModelType.ASSASSIN);
        setVirtualSuffix(true);
        setTestingText("Guri (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, 1));
        Condition notHit = new NotCondition(new HitCondition(self));
        Condition modifierXizorCondition = new AndCondition(new PresentWithCondition(self, Filters.Xizor), 
            new NotCondition(new GameTextModificationCondition(self, ModifyGameTextType.LEGACY__TREAT_XIZOR_AS_SHADA)), notHit);
        modifiers.add(new MayNotBeTargetedByWeaponsModifier(self, Filters.Xizor, modifierXizorCondition));
        Condition modifierShadaCondition = new AndCondition(new PresentWithCondition(self, Filters.title("Shada")), 
            new GameTextModificationCondition(self, ModifyGameTextType.LEGACY__TREAT_XIZOR_AS_SHADA), notHit);
        modifiers.add(new MayNotBeTargetedByWeaponsModifier(self, Filters.title("Shada"), modifierShadaCondition));
        Condition yourRef2ObjectiveOnTable = new OnTableCondition(self, Filters.and(Filters.your(self), Icon.REFLECTIONS_II, Filters.Objective));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, yourRef2ObjectiveOnTable, 5));
        return modifiers;
    }
}
