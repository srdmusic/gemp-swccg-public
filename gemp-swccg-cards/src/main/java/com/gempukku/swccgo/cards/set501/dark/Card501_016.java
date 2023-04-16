package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Great Pit Of Carkoon (V)
 */
public class Card501_016 extends AbstractSite {
    public Card501_016() {
        super(Side.DARK, Title.Great_Pit_Of_Carkoon, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Jabba's Sail Barge is deploy -1 (-2 if Jabba on table) here.");
        setLocationLightSideGameText("Your characters of ability < 5 may not move from here if a Sarlacc or Barge here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.JABBAS_PALACE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.PIT);
        setTestingText("Tatooine: Great Pit Of Carkoon (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.Jabbas_Sail_Barge, new ConditionEvaluator(-1, -2, new OnTableCondition(self, Filters.Jabba)), Filters.here(self)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotMoveFromLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.abilityLessThan(5)), new HereCondition(self, Filters.or(Filters.Sarlacc, Filters.Jabbas_Sail_Barge)), self));
        return modifiers;
    }
}