package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.MovesFreeFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MovesFreeToLocationModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Location
 * Subtype: System
 * Title: Cyrkon
 */
public class Card501_123 extends AbstractSystem {
    public Card501_123() {
        super(Side.LIGHT, Title.Cyrkon, 8);
        setLocationDarkSideGameText("Your starships piloted by bounty hunters move to and from here for free.");
        setLocationLightSideGameText("During your control phase, if you control with two smugglers, may retrieve 1 Force.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.PLANET, Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_20);
        setTestingText("~Cyrkon");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MovesFreeToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.starship, Filters.hasPiloting(self, Filters.bounty_hunter)), self));
        modifiers.add(new MovesFreeFromLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.starship, Filters.hasPiloting(self, Filters.bounty_hunter)), self));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, Phase.CONTROL)
                && GameConditions.controlsWith(game, self, playerOnLightSideOfLocation, Filters.here(self), Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.smuggler, Filters.with(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.smuggler))))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId);
            action.setText("Retrieve 1 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerOnLightSideOfLocation, 1));
            return Collections.singletonList(action);
        }
        return null;
    }
}