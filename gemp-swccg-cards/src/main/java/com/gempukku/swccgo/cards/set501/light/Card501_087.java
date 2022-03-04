package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.CancelDestinyAndCauseRedrawEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Your Thoughts Dwell On Your Mother
 */
public class Card501_087 extends AbstractNormalEffect {
    public Card501_087() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Your_Thoughts_Dwell_On_Your_Mother, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on table if Slave Quarters on table. Once per turn, may [download] Anakin's Lightsaber (or lose 1 Force to deploy it from Lost Pile). If Anakin just drawn for destiny, may take him into hand to cancel and cause a re-draw. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Your Thoughts Dwell On Your Mother (ERRATA)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Slave_Quarters);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

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

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ANY_CARD__CANCEL_AND_REDRAW_A_DESTINY;

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawn(game, effectResult)
                && GameConditions.isDestinyCardMatchTo(game, Filters.Anakin)
                && GameConditions.canTakeDestinyCardIntoHand(game, playerId)
                && GameConditions.canCancelDestinyAndCauseRedraw(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take into hand and cause re-draw");
            action.setActionMsg("Cancel destiny and cause re-draw");
            // Pay cost(s)
            action.appendEffect(
                    new TakeDestinyCardIntoHandEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new CancelDestinyAndCauseRedrawEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }
}
