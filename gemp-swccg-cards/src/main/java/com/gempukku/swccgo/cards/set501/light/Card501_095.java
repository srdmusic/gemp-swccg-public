package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Endor: Anakin's Funeral Pyre
 */
public class Card501_095 extends AbstractSite {
    public Card501_095() {
        super(Side.LIGHT, Title.Anakins_Funeral_Pyre, Title.Endor);
        setLocationDarkSideGameText("You must lose 1 Force to initiate a Force drain here.");
        setLocationLightSideGameText("Once during any draw phase, if Prophecy Of The Force on table, may activate 1 Force.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        setTestingText("Endor: Anakin's Funeral Pyre");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        //TODO modifiers.add(new LoseForceToInitiateForceDrainModifier(self, playerOnDarkSideOfLocation, 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Once during any draw phase, if Prophecy Of The Force on table, may activate 1 Force.

        // Check condition(s)
        if (GameConditions.isOnceDuringEitherPlayersPhase(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId, Phase.DRAW)
                && GameConditions.canActivateForce(game, playerOnLightSideOfLocation)
                && GameConditions.canSpot(game, self, Filters.Prophecy_Of_The_Force)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Activate 1 Force");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));

            // Perform result(s)
            action.appendEffect(
                    new ActivateForceEffect(action, playerOnLightSideOfLocation, 1));

            actions.add(action);
        }

        return actions;
    }
}