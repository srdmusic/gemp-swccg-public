package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromOutsideOfGameSimultaneouslyWithCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToSystemFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToPowerModifier;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.DeploysFreeAboardModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Character
 * Subtype: Imperial
 * Title: TD-4445
 */
public class Card501_038 extends AbstractImperial {
    public Card501_038() {
        super(Side.DARK, 3, 2, 2, 2, 4, "TD-4445", Uniqueness.UNIQUE);
        setArmor(4);
        setLore("Sandtrooper.");
        setGameText("Once per game, may reveal from hand to take a Dewback into hand from outside the game and deploy both simultaneously.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.SANDTROOPER);
        setTestingText("TD-4445");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelInHandActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.TK_4445__DEPLOY_WITH_DEWBACK;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
            && GameConditions.isDuringYourPhase(game, playerId, Phase.DEPLOY)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Reveal to deploy a Dewback");
            action.setActionMsg("Reveal to deploy simultaneously with a Dewback from outside the game");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new ShowCardOnScreenEffect(action, self));
            action.appendEffect(
                    new DeployCardFromOutsideOfGameSimultaneouslyWithCardEffect(action, self, playerId, Filters.Dewback));
            return Collections.singletonList(action);
        }
        return null;
    }
}