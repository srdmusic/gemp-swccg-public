package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.GenerateNoForceModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotForceDrainAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Objective
 * Title: Rebel Strike Team (V) / Garrison Destroyed (V)
 */
public class Card501_094 extends AbstractObjective {
    public Card501_094() {
        super(Side.LIGHT, 0, Title.Rebel_Strike_Team, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Endor system and Rebel Landing Site." +
                "For remainder of game, opponent activates no Force at your Endor system (unless their [En] objective on table)." +
                "You may not deploy cards with ability except for Rebels, Ewoks, and Rebel starships." +
                "While this side up, once per turn may deploy one Explosive Charge or [Endor] Epic Event to Bunker; reshuffle. " +
                "You may not Force drain on Endor unless your Ewok or [En] Han present." +
                "Flip this card if bunker 'blown away.'");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_25);
        setTestingText("Rebel Strike Team (V)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Endor_system, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Endor system to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Rebel_Landing_Site, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Rebel Landing Site to deploy";
                    }
                });
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new GenerateNoForceModifier(self, Filters.Endor_system,
                new NotCondition(new OnTableCondition(self, Filters.and(Filters.opponents(self), Icon.ENDOR, Filters.Objective))), opponent));
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.hasAbilityOrHasPermanentPilotWithAbility,
                Filters.not(Filters.or(Filters.Rebel, Filters.Ewok, Filters.and(Icon.REBEL, Filters.starship)))), playerId));
        modifiers.add(new MayNotForceDrainAtLocationModifier(self,
                Filters.and(Filters.on(Title.Endor), Filters.not(Filters.wherePresent(self, Filters.or(Filters.and(Filters.icon(Icon.ENDOR), Filters.Han), Filters.Ewok)))), playerId));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.REBEL_STRIKE_TEAM__DOWNLOAD_EXPLOSIVE_CHARGE_OR_ENDOR_EPIC_EVENT;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy card to Bunker");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action,
                            Filters.or(Filters.Explosive_Charge, Filters.and(Filters.icon(Icon.ENDOR), Filters.Epic_Event)),
                            Filters.Bunker, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {

        // Check condition(s)
        if (TriggerConditions.isBlownAwayLastStep(game, effectResult, Filters.title(Title.Bunker, true))
                && GameConditions.canBeFlipped(game, self)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
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