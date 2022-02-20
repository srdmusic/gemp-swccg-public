package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtRandomCardInOpponentsHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.CancelDestinyAndCauseRedrawEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Shadows Of The Empire & Information Exchange
 */
public class Card501_003 extends AbstractNormalEffect {
    public Card501_003() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Shadows Of The Empire & Information Exchange", Uniqueness.UNIQUE);
        addComboCardTitles("Shadows Of The Empire", Title.Information_Exchange);
        setGameText("Deploy on table. If [Reflections II] Emperor drawn for destiny, may take him into hand to cancel that destiny and cause a re-draw. If [Reflections II] Emperor on Coruscant, and you just deployed an information broker, may peek at one card (random selection) from opponent's hand. If your [Reflections II] objective on table: Once during your control phase, may use 1 Force to take Imperial Square or [Reflections II] Emperor into hand from Reserve Deck; reshuffle; and once per turn, if [Reflections II] Emperor on Coruscant, may draw top card of Force Pile (if during your turn and you occupy three battlegrounds, opponent also loses 1 Force). [Immune to Alter.]");
        addIcons(Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Shadows Of The Empire & Information Exchange");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.SHADOWS_OF_THE_EMPIRE_INFORMATION_EXCHANGE__UPLOAD_IMPERIAL_SQUARE_OR_EMPEROR;
        if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Icon.REFLECTIONS_II, Filters.Objective))
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.canUseForce(game, playerId, 1)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take Imperial Square or Emperor into hand from Reserve Deck");
            action.setActionMsg("Take Imperial Square or [Reflections II] Emperor into hand from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Pay cost(s)
            action.appendCost(
                    new UseForceEffect(action, playerId, 1));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Coruscant_Imperial_Square, Filters.and(Icon.REFLECTIONS_II, Filters.Emperor)), true));
            actions.add(action);
        }


        // Check conditions for drawing top card of force pile
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Icon.REFLECTIONS_II, Filters.Objective))
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.hasForcePile(game, playerId)
                && GameConditions.canSpot(game, self, Filters.and(Icon.REFLECTIONS_II, Filters.Emperor, Filters.on(Title.Coruscant)))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Draw top card of Force Pile");
            action.setActionMsg("Draw top card of Force Pile");

            // Update usage limits
            action.appendUsage(new OncePerTurnEffect(action));

            // Perform result(s)
            action.appendEffect(new DrawCardIntoHandFromForcePileEffect(action, playerId));

            if (GameConditions.occupies(game, playerId, 3, Filters.battleground) && GameConditions.isDuringYourTurn(game, self)) {
                String opponent = game.getOpponent(playerId);
                action.appendEffect(new LoseForceEffect(action, opponent, 1));
            }

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.ANY_CARD__CANCEL_AND_REDRAW_A_DESTINY;

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawn(game, effectResult)
                && GameConditions.isDestinyCardMatchTo(game, Filters.and(Icon.REFLECTIONS_II, Filters.Emperor))
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
            actions.add(action);
        }


        String opponent = game.getOpponent(playerId);

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;

        if (GameConditions.canSpot(game, self, Filters.and(Icon.REFLECTIONS_II, Filters.Emperor, Filters.on(Title.Coruscant)))
                && TriggerConditions.justDeployed(game, effectResult, playerId, Filters.information_broker)
                && GameConditions.hasHand(game, opponent)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Peek at random card");
            action.setActionMsg("Peek at one card (random selection) from "+opponent+"'s hand");

            action.appendEffect(
                    new PeekAtRandomCardInOpponentsHandEffect(action, playerId));

            actions.add(action);
        }
        return actions;
    }
}