package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Hoth: North Ridge (4th Marker) (V)
 */
public class Card501_021 extends AbstractSite {
    public Card501_021() {
        super(Side.DARK, Title.North_Ridge, Title.Hoth);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Your AT-ATs move to or from here for free. If you control with two AT-ATs, Force drain +1 here.");
        setLocationLightSideGameText("Your T-47s are power +1 here. Once per game, may deploy a combat vehicle here from Reserve Deck; reshuffle.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.HOTH, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        addKeywords(Keyword.MARKER_4);
        setTestingText("Hoth: North Ridge (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MovesFreeToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.AT_AT), self));
        modifiers.add(new MovesFreeFromLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.AT_AT), self));
        modifiers.add(new ForceDrainModifier(self, new ControlsWithCondition(playerId, self, 2, Filters.AT_AT), 1, playerId));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.T_47, Filters.here(self)), 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.HOTH_NORTH_RIDGE_V__DEPLOY_COMBAT_VEHICLE_FROM_RESERVE_DECK;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId)) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a combat vehicle from Reserve Deck");
            action.setActionMsg("Deploy a combat vehicle here from Reserve Deck");
            action.appendUsage(new OncePerGameEffect(action));
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.combat_vehicle, Filters.here(self), true));
            return Collections.singletonList(action);
        }

        return null;
    }
}