package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Scarif: Command Center
 */
public class Card501_050 extends AbstractSite {
    public Card501_050() {
        super(Side.DARK, "Scarif: Command Center", Title.Scarif);
        setLocationDarkSideGameText("If your Imperial leader here, may add one destiny to your Commence Primary Ignition total while targeting a Scarif site.");
        setLocationLightSideGameText("If your Rebel spy here, once per game may place a trooper out of play from opponent's Lost Pile.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.VIRTUAL_SET_16, Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK);
        setTestingText("Scarif: Command Center");
        hideFromDeckBuilder();
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextDarkSideOptionalAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        return null;
    }
}