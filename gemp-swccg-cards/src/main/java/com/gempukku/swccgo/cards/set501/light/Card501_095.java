package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.LoseCardsFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Lars' Homestead
 */
public class Card501_095 extends AbstractSite {
    public Card501_095() {
        super(Side.LIGHT, Title.Lars_Homestead, Title.Tatooine);
        setLocationDarkSideGameText("Sandwhirl and Tusken Raiders are lost here.");
        setLocationLightSideGameText("May deploy Anakin's Lightsaber from Reserve Deck; reshuffle (or once per game, deploy it from Lost Pile).");
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.DEATH_STAR_II, Icon.EPISODE_VII, Icon.VIRTUAL_SET_16);
        setTestingText("[Set 17] Tatooine: Lars' Homestead");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextDarkSideRequiredAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.isHere(game, self, Filters.or(Filters.Sandwhirl, Filters.Tusken_Raider))) {

            Collection<PhysicalCard> toBeLost = Filters.filterActive(game, self, Filters.and(Filters.here(self), Filters.or(Filters.Sandwhirl, Filters.Tusken_Raider)));

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Make cards lost");
            // Perform result(s)
            action.appendEffect(
                    new LoseCardsFromTableEffect(action, toBeLost));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId)
    {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        // Using the same GameTextActionId for both actions since they are mutually exclusive per turn.
        GameTextActionId gameTextActionId = GameTextActionId.TATOOINE_LARS_HOMESTEAD__DEPLOY_ANAKINS_LIGHTSABER;

        // May deploy Anakin's Lightsaber from Reserve Deck; reshuffle (or once per game, deploy it from Lost Pile).

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId, Persona.ANAKINS_LIGHTSABER)) {

            // May deploy Anakin's Lightsaber from Reserve Deck; reshuffle

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Anakin's Lightsaber from Reserve Deck");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));

            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Anakins_Lightsaber), true));

            actions.add(action);
        }


        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.hasLostPile(game, playerOnLightSideOfLocation)
                && GameConditions.canDeployCardFromLostPile(game, playerOnLightSideOfLocation, self, gameTextActionId, Persona.ANAKINS_LIGHTSABER)) {

            // or once per game, deploy it from Lost Pile

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Anakin's Lightsaber from Lost Pile");

            // Update usage limit(s)
            // Note:  This case is a little unique because this action counts
            // towards Once-per-game AND Once-per-turn limits
            action.appendUsage(
                    new OncePerGameEffect(action));
            action.appendUsage(
                    new OncePerTurnEffect(action));

            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromLostPileEffect(action, Filters.title(Title.Anakins_Lightsaber), false));
            actions.add(action);
        }

        return actions;
    }
}