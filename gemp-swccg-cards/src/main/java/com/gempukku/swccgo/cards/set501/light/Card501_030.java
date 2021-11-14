package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.ModifyPowerUntilEndOfPlayersNextTurnEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetDeployCostModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Your Thoughts Dwell On Your Mother
 */
public class Card501_030 extends AbstractNormalEffect {
    public Card501_030() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, Title.Your_Thoughts_Dwell_On_Your_Mother, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on Slave Quarters. Anakin is deploy = 6. Once per turn, may deploy Anakin's Lightsaber from Reserve Deck; reshuffle (or lose 1 Force to deploy from Lost Pile). If Shmi just lost, Anakin is power +5 until end of your next turn. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.CORUSCANT, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Your Thoughts Dwell On Your Mother");
    }

    @Override
    protected Filter getValidDeployTargetFilter(String playerId, SwccgGame game, PhysicalCard self, PhysicalCard sourceCard, PlayCardOption playCardOption, boolean forFree, float changeInCost, DeploymentRestrictionsOption deploymentRestrictionsOption, DeployAsCaptiveOption deployAsCaptiveOption, ReactActionOption reactActionOption, boolean isSimDeployAttached, boolean ignorePresenceOrForceIcons) {
        return Filters.Slave_Quarters;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ResetDeployCostModifier(self, Filters.Anakin, 6));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLeavesTableRequiredTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.justLost(game, effectResult, Filters.Shmi)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make Anakin power +5");
            // Perform result(s)
            action.appendEffect(
                    new ModifyPowerUntilEndOfPlayersNextTurnEffect(action, self.getOwner(), Filters.Anakin, 5, "Makes Anakin's power +5"));
            return Collections.singletonList(action);
        }
        return null;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.YOUR_THOUGHTS_DWELL_ON_YOUR_MOTHER__DEPLOY_LIGHTSABER;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Persona.ANAKINS_LIGHTSABER)) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Deploy Anakin's Lightsaber from Reserve Deck");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new DeployCardFromReserveDeckEffect(action, Filters.persona(Persona.ANAKINS_LIGHTSABER), true));
                actions.add(action);
            }
            if (GameConditions.canDeployCardFromLostPile(game, playerId, self, gameTextActionId, Persona.ANAKINS_LIGHTSABER)) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Deploy Anakin's Lightsaber from Lost Pile");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Pay cost(s)
                action.appendCost(
                        new LoseForceEffect(action, playerId, 1, true));
                // Perform result(s)
                action.appendEffect(
                        new DeployCardFromLostPileEffect(action, Filters.persona(Persona.ANAKINS_LIGHTSABER), false));
                actions.add(action);
            }
        }
        return actions;
    }
}
