package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Effect
 * Title: Now It Calls To You
 */
public class Card501_042 extends AbstractNormalEffect {
    public Card501_042() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Now_It_Calls_To_You, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on table. May deploy Anakin’s Lightsaber from Reserve Deck; reshuffle (or place this Effect out of play to deploy it from Lost Pile). If Anakin's Lightsaber present during battle or Force drain, may retrieve 1 Force. [Immune to Alter].");
        addIcons(Icon.EPISODE_I, Icon.EPISODE_VII, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_16);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Now It Calls To You");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.forceDrainInitiatedBy(game, effectResult, playerId, Filters.wherePresent(self, Filters.persona(Persona.ANAKINS_LIGHTSABER)))
                && GameConditions.hasLostPile(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Retrieve 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, 1));
            return Collections.singletonList(action);
        }
        return null;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();
        GameTextActionId gameTextActionId = GameTextActionId.NOW_IT_CALLS_TO_YOU__DOWNLOAD_ANAKINS_SABER;
        Filter anakinsLightsaberFilter = Filters.title(Title.Anakins_Lightsaber);

        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, false, Persona.ANAKINS_LIGHTSABER)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Anakin’s Lightsaber from Reserve Deck");
            action.setActionMsg("Deploy Anakin’s Lightsaber from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, anakinsLightsaberFilter, true));
            actions.add(action);
        }

        if (GameConditions.canDeployCardFromLostPile(game, playerId, self, gameTextActionId, false, Persona.ANAKINS_LIGHTSABER)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Anakin’s Lightsaber from Lost Pile");
            action.setActionMsg("Deploy Anakin’s Lightsaber from Lost Pile");
            // Pay Costs
            action.appendCost(
                    new PlaceCardOutOfPlayFromTableEffect(action, self)
            );
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromLostPileEffect(action, anakinsLightsaberFilter, false));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (GameConditions.isDuringBattleWithParticipant(game, Filters.persona(Persona.ANAKINS_LIGHTSABER))
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.hasLostPile(game, playerId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve 1 Force");
            // Add Usages
            action.appendUsage(
                    new OncePerBattleEffect(action)
            );
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(self, action, playerId, 1));
            actions.add(action);
        }

        return actions;
    }
}