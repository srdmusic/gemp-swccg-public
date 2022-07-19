package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Advanced Projects Laboratory
 */
public class Card501_027 extends AbstractSite {
    public Card501_027() {
        super(Side.DARK, Title.Lothal_Advanced_Projects_Laboratory, Title.Lothal);
        setLocationDarkSideGameText("Once per game, you may deploy Thrawn or TIE Defender Project here from reserve deck, reshuffle.");
        setLocationLightSideGameText("If you control with a Rebel, Force drain +1 here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Advanced Projects Laboratory");
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextDarkSideTopLevelActions(String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.LOTHAL_ADVANCED_PROJECTS_LABORATORY__DOWNLOAD_THRAWN_OR_TIE_DEFENDER_PROJECT;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnDarkSideOfLocation, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Thrawn or TIE Defender Project");
            action.setActionMsg("Deploy Thrawn or TIE Defender Project from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Thrawn, Filters.title("TIE Defender Project")), true));
            return Collections.singletonList(action);
        }
        return null;
    }


    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, new ControlsWithCondition(playerOnLightSideOfLocation, self, Filters.Rebel),1, playerOnLightSideOfLocation));
        return modifiers;
    }
}
