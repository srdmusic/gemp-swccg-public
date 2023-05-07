package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveGameTextCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Vader's Cape (V)
 */
public class Card501_129 extends AbstractNormalEffect {
    public Card501_129() {
        super(Side.DARK, 3, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Vader's Cape", Uniqueness.UNIQUE, ExpansionSet.SET_21, Rarity.V);
        setVirtualSuffix(true);
        setLore("A symbol of the Dark Lord of the Sith, and of the seductive power of the dark side.");
        setGameText("If Revenge Of The Sith or Visage Of The Emperor on table, deploy on table. Vader's game text may not be canceled. While Vader is alone, armed with a [Death Star II] lightsaber, and present at a site, your Force drains there are +1. [Immune to Alter.]");
        addIcons(Icon.CLOUD_CITY, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Vader's Cape (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.or(Filters.Revenge_Of_The_Sith, Filters.Visage_Of_The_Emperor));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self, Filters.Vader));
        Filter dsIILightsaber = Filters.and(Filters.icon(Icon.DEATH_STAR_II), Filters.lightsaber);
        Filter aloneAndPresentAtASite = Filters.and(Filters.alone, Filters.presentAt(Filters.site));
        Filter vaderArmedWithDsIILightsaberAloneAndPresentAtASite = Filters.and(Filters.Vader, Filters.armedWith(dsIILightsaber), aloneAndPresentAtASite);
        modifiers.add(new ForceDrainModifier(self, Filters.sameSiteAs(self, vaderArmedWithDsIILightsaberAloneAndPresentAtASite), 1, self.getOwner()));
        return modifiers;
    }
}