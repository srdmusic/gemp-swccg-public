package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
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
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.DestinyDrawnResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Effect
 * Title: This Is Our Rebellion
 */
public class Card501_104 extends AbstractNormalEffect {
    public Card501_104() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "This Is Our Rebellion", Uniqueness.UNIQUE);
        setGameText("If Lothal on table, deploy on table. During battle, if you just drew a Rebel for destiny, opponent’s immunity to attrition is canceled (if a Phoenix squadron member, may also take it into hand). Whenever a Rebel wins a battle, opponent loses 1 Force. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_19);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("This Is Our Rebellion");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Lothal_system);
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && TriggerConditions.isDestinyJustDrawn(game, effectResult)) {
            DestinyDrawnResult destinyDrawnResult = (DestinyDrawnResult) effectResult;
            final PhysicalCard cardDrawn = destinyDrawnResult.getCard();
            if (cardDrawn != null
                    && Filters.Rebel.accepts(game, cardDrawn)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setPerformingPlayer(playerId);
                action.setText("Cancel opponent's immunity to attrition");
                // Perform result(s)

                action.appendEffect(
                        new CancelImmunityToAttritionUntilEndOfBattleEffect(action, Filters.opponents(self), "Cancels opponent's immunity to attrition until end of battle"));
                if (Filters.and(Keyword.PHOENIX_SQUADRON).accepts(game, cardDrawn)
                        && GameConditions.canTakeDestinyCardIntoHand(game, playerId)) {
                    action.appendEffect(new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Take " + GameUtils.getCardLink(cardDrawn) + " into hand?") {
                        @Override
                        protected void yes() {
                            action.appendEffect(
                                    new TakeDestinyCardIntoHandEffect(action));
                            action.appendEffect(new SendMessageEffect(action, "okay"));
                        }

                        @Override
                        protected void no() {
                            action.appendEffect(
                                    new SendMessageEffect(action, playerId + " chooses not to take just drawn destiny into hand"));
                        }
                    }));
                }
                actions.add(action);
            }

        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        if (TriggerConditions.wonBattle(game, effectResult, Filters.Rebel)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Opponent loses 1 Force");

            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        return actions;
    }
}