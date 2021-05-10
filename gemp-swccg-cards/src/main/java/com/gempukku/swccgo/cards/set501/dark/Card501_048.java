package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.conditions.GameTextModificationCondition;
import com.gempukku.swccgo.cards.effects.CancelForceDrainEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameActionProxyEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToSystemFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 15
 * Type: Objective
 * Title: On The Verge Of Greatness / Deploy The Garrison!
 */
public class Card501_048 extends AbstractObjective {
    public Card501_048() {
        super(Side.DARK, 0, Title.On_The_Verge_Of_Greatness);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy [Set 15] Death Star and Scarif systems, Citadel Tower, and Shield Gate. May deploy Commence Primary Ignition.\n" +
                "For remainder of game, you may not deploy characters of ability > 4 (except Vader). Star Destroyers deploy -1 to Scarif. Superlaser ignores deployment restrictions.\n" +
                "While this side up, once per turn, may deploy a site or Imperial trooper to Scarif from Reserve Deck; reshuffle.\n" +
                "Flip this card if Tarkin or Krennic on Scarif and Death Star orbiting Scarif.");
        addIcons(Icon.VIRTUAL_SET_15);
        setTestingText("On The Verge Of Greatness");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_15, Filters.Death_Star_system), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Death Star system to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Scarif_system, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Scarif system to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.title("Scarif: Citadel Tower"), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Citadel Tower to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Shield_Gate, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Shield Gate to deploy";
                    }
                });
        action.appendOptionalEffect(
                new DeployCardsFromReserveDeckEffect(action, Filters.Commence_Primary_Ignition, 0, 1, true, false) {
                    @Override
                    public String getChoiceText(int numCardsToChoose) {
                        return "Choose Commence Primary Ignition to deploy";
                    }
                });
        return action;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ON_THE_VERGE_OF_GREATNESS__DEPLOY_SITE_OR_TROOPER_TO_SCARIF;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a site or Imperial trooper to Scarif");
            action.setActionMsg("Deploy a site or Imperial trooper to Scarif");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToSystemFromReserveDeckEffect(action, Filters.or(Filters.site, Filters.and(Filters.trooper, Filters.Imperial)), Title.Scarif, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.except(Filters.Vader), Filters.character, Filters.abilityMoreThan(4)), self.getOwner()));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.Star_Destroyer, -1, Filters.Scarif_system));
        modifiers.add(new ModifyGameTextModifier(self, Filters.Superlaser, ModifyGameTextType.SUPERLASER_IGNORES_DEPLOYMENT_RESTRICTIONS));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.and(Filters.on(Title.Scarif), Filters.or(Filters.Tarkin, Filters.Krennic)))
                && GameConditions.canSpot(game, self, Filters.and(Filters.Death_Star_system, Filters.isOrbiting(Title.Scarif)))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}