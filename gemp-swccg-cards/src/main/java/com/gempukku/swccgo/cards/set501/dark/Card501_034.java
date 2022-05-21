package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: System
 * Title: Lothal
 */
public class Card501_034 extends AbstractSystem {
    public Card501_034() {
        super(Side.DARK, Title.Lothal, 6);
        setLocationDarkSideGameText("While you control this system, your Force drains are +1 at related battlegrounds you control with an Imperial leader.");
        setLocationLightSideGameText("Unless two Rebels here, you may not draw more than one battle destiny here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal (DARK)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, Filters.and(Filters.relatedLocation(self), Filters.battleground, Filters.controlsWith(playerOnDarkSideOfLocation, self, Filters.Imperial_leader)), 1, playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDrawMoreThanBattleDestinyModifier(self, self, new UnlessCondition(new HereCondition(self, 2, Filters.Rebel)), 1, playerOnLightSideOfLocation));
        return modifiers;
    }
}