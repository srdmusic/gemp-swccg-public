package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.CommuningCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Obi-Wan's Hut (V)
 */
public class Card501_103 extends AbstractSite {
    public Card501_103() {
        super(Side.LIGHT, Title.ObiWans_Hut, Title.Tatooine);
        setLocationDarkSideGameText("While [Set 16] Obi-Wan 'communing,' no Force drains here.");
        setLocationLightSideGameText("While Obi-Wan here or [Set 16] Obi-Wan 'communing,' your total Force generation is +1.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_16);
        setVirtualSuffix(true);
        setTestingText("Tatooine: Obi-Wan's Hut (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotForceDrainAtLocationModifier(self, new CommuningCondition(Filters.and(Icon.VIRTUAL_SET_16, Filters.ObiWan))));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalForceGenerationModifier(self, new OrCondition(new HereCondition(self, Filters.ObiWan), new CommuningCondition(Filters.and(Icon.VIRTUAL_SET_16, Filters.ObiWan))), 1, playerOnLightSideOfLocation));
        return modifiers;
    }
}