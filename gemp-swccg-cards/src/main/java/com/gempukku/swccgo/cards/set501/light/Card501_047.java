package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.MoveUsingLocationTextAction;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotForceDrainAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Location
 * Subtype: Site
 * Title: Coruscant: Jedi Temple Entrance
 */
public class Card501_047 extends AbstractSite {
    public Card501_047() {
        super(Side.LIGHT, "Coruscant: Jedi Temple Entrance", Title.Coruscant);
        setLocationDarkSideGameText("");
        setLocationLightSideGameText("During your move phase, Jedi Council members may move between here and any battleground (or Coruscant) site. While a Jedi here, opponent's characters deploy +2 here and opponent may not Force drain at Jedi Council Chamber.");
        addIcon(Icon.DARK_FORCE, 0);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EPISODE_I, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Coruscant: Jedi Temple Entrance");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.character), new HereCondition(self, Filters.Jedi), 2, self));
        modifiers.add(new MayNotForceDrainAtLocationModifier(self, Filters.Jedi_Council_Chamber, new HereCondition(self, Filters.Jedi), game.getOpponent(playerOnLightSideOfLocation)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();
        Filter otherBattlegroundSites = Filters.and(Filters.other(self), Filters.or(Filters.battleground_site, Filters.Coruscant_site));


        // Move a Jedi Council member
        // Check condition(s)
        if (GameConditions.isDuringYourPhase(game, playerOnLightSideOfLocation, Phase.MOVE)
                && GameConditions.canSpotLocation(game, otherBattlegroundSites)) {
            if (GameConditions.canPerformMovementUsingLocationText(playerOnLightSideOfLocation, game, Filters.Jedi_Council_member, self, otherBattlegroundSites, false)) {
                MoveUsingLocationTextAction action = new MoveUsingLocationTextAction(playerOnLightSideOfLocation, game, self, gameTextSourceCardId, Filters.Jedi_Council_member, self, otherBattlegroundSites, false);
                action.setText("Move from here to other site");
                action.setActionMsg("Move a Jedi Council member from " + GameUtils.getCardLink(self) + " to other battleground (or Coruscant) site");
                actions.add(action);
            }
            if (GameConditions.canPerformMovementUsingLocationText(playerOnLightSideOfLocation, game, Filters.Jedi_Council_member, otherBattlegroundSites, self, false)) {
                MoveUsingLocationTextAction action = new MoveUsingLocationTextAction(playerOnLightSideOfLocation, game, self, gameTextSourceCardId, Filters.Jedi_Council_member, otherBattlegroundSites, self, false);
                action.setText("Move from other site to here");
                action.setActionMsg("Move a Jedi Council member from other battleground (or Coruscant) site to " + GameUtils.getCardLink(self));
                actions.add(action);
            }
        }

        return actions;
    }
}
