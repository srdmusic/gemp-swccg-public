package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUniqueStarshipSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToDeployCardToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Location
 * Subtype: Site
 * Title: Razor Crest: Cold Storage
 */
public class Card501_007 extends AbstractUniqueStarshipSite {
    public Card501_007() {
        super(Side.LIGHT, Title.Cold_Storage, Persona.RAZOR_CREST, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("Once per game, may retrieve Din into hand.");
        setLocationDarkSideGameText("You must first use 3 Force to deploy a character here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.MUDHORN, Icon.INTERIOR_SITE, Icon.SCOMP_LINK, Icon.MOBILE, Icon.STARSHIP_SITE);
        setTestingText("Razor Crest: Cold Storage");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.COLD_STORAGE__RETRIEVE_DIN;

        if (GameConditions.canSearchLostPile(game, self.getOwner(), self, gameTextActionId)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve Din into hand");
            action.setActionMsg("Retrieve Din into hand");
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveCardIntoHandEffect(action, self.getOwner(), Filters.Din));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ExtraForceCostToDeployCardToLocationModifier(self,
                Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character), 3,
                Filters.here(self)));
        return modifiers;
    }
}
