package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.MoveUsingLocationTextAction;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Endor: Chief Chirpa's Hut (V)
 */
public class Card501_216 extends AbstractSite {
    public Card501_216() {
        super(Side.LIGHT, Title.Chief_Chirpas_Hut, Title.Endor, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("");
        setLocationLightSideGameText("Deploys only if a [Death Star II] objective on table. If an Imperial at Landing Platform during opponent's turn, may relocate Luke from here to there.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.ENDOR, Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_14);
        setVirtualSuffix(true);
        setTestingText("Endor: Chief Chirpa's Hut (V) (ERRATA)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        return GameConditions.canSpot(game, self, Filters.and(Icon.DEATH_STAR_II, Filters.Objective));
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        Filter character = Filters.Luke;
        Filter destination = Filters.Landing_Platform;
        
        if (GameConditions.canSpot(game, self, Filters.and(Filters.Imperial, Filters.atLocation(Filters.Landing_Platform)))
                && GameConditions.isOpponentsTurn(game, self)
                && GameConditions.canPerformMovementUsingLocationText(playerOnLightSideOfLocation, game, character, self, destination, true)) {
            
            MoveUsingLocationTextAction action = new MoveUsingLocationTextAction(playerOnLightSideOfLocation, game, self, gameTextSourceCardId, character, self, destination, true);
            action.setText("Move Luke to Landing Platform");
            actions.add(action);
            return actions;
        }
        return null;
    }

}