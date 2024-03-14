package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Location
 * Subtype: Site
 * Title: Mandalorian Covert
 */
public class Card501_008 extends AbstractSite{
    public Card501_008(){
        super(Side.LIGHT, Title.Mandalorian_Covert, Uniqueness.DIAMOND_1, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("You may not deploy characters here (except Mandalorians). May not be deployed to Dagobah, Endor, Jakku, or Tatooine.");
        setLocationDarkSideGameText("You may not deploy characters here (except Jango, Boba, and Jodo Kast). Immune to No Escape.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.INTERIOR_SITE, Icon.UNDERGROUND, Icon.PLANET);
        addImmuneToCardTitle(Title.No_Escape);
        setTestingText("Mandalorian Covert");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.not(Filters.Mandalorian)), Filters.sameSite(self)));
        return modifiers;
    }
        
    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character, Filters.not(Filters.or(Filters.Jango_Fett, Filters.Boba_Fett, Filters.Jodo))), Filters.sameSite(self)));
        return modifiers;
    }
}
