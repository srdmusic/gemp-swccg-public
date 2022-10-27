package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.OccupiesWithCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Imperial Strip Mines
 */
public class Card501_103 extends AbstractSite {
    public Card501_103() {
        super(Side.DARK, "Lothal: Imperial Strip Mines", Title.Lothal);
        setLocationDarkSideGameText("If you occupy with an Imperial leader, Force generation +1 here (+2 if a miner).");
        setLocationLightSideGameText("Force drain -1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.EXTERIOR_SITE, Icon.UNDERGROUND, Icon.PLANET, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Imperial Strip Mines (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new OccupiesWithCondition(playerOnDarkSideOfLocation, self, Filters.Imperial_leader),
                new ConditionEvaluator(1, 2, new OccupiesWithCondition(playerOnDarkSideOfLocation, self, Filters.and(Filters.Imperial_leader, Filters.miner))),
                playerOnDarkSideOfLocation));
        return modifiers;
    }


    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, -1, playerOnLightSideOfLocation));
        return modifiers;
    }
}
