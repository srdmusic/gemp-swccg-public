package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PreventEffectOnCardEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 17
 * Type: Objective
 * Title: The Force Is Strong In My Family / Rise Of Skywalker
 */
public class Card501_075_BACK extends AbstractObjective {
    public Card501_075_BACK() {
        super(Side.LIGHT, 7, Title.Rise_Of_Skywalker);
        setGameText("While this side up, if you just initiated battle involving a Skywalker (or if opponent's Sidious just lost from table), may retrieve 1 Force. During your deploy phase, you may deploy Lars’ Moisture Farm or Polis Masa from Reserve Deck; reshuffle. If Reflection about to leave table, may place it in Used Pile instead. Courage Of A Skywalker and Higher Ground are destiny +2. " +
                "Flip this card if no Skywalkers or Jedi are on table.");
        addIcons(Icon.SKYWALKER, Icon.EPISODE_I, Icon.EPISODE_VII, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_17);
        setTestingText("Rise Of Skywalker");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.RISE_OF_SKYWALKER__DEPLOY_LOCATION;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Lars_Moisture_Farm)
                || GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Polis_Massa) )) {


            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Lars' Moisture Farm or Polis Massa");
            action.setActionMsg("Deploy Lars' Moisture Farm or Polis Massa from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Choose target(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Lars_Moisture_Farm, Filters.Polis_Massa_system), true));
            actions.add(action);
        }

        return actions;
    }


    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter revealedCardsFilter = new Filter() {
            @Override
            public boolean accepts(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard physicalCard) {
                List<PhysicalCard> cardsInData = self.getWhileInPlayData() != null ? self.getWhileInPlayData().getPhysicalCards() : null;
                if (cardsInData != null) {
                    for (PhysicalCard c : cardsInData) {
                        if (Filters.sameTitle(physicalCard).accepts(gameState, modifiersQuerying, c))
                            return true;
                    }
                }
                return false;
            }
        };

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new TotalForceGenerationModifier(self, 1, self.getOwner()));
        modifiers.add(new MayNotDeployModifier(self, Filters.Boss_Nass_Chambers, self.getOwner()));
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.Jedi, Filters.except(Filters.or(Filters.Yoda, revealedCardsFilter))), self.getOwner()));
        modifiers.add(new DestinyModifier(self, Filters.or(Filters.title("Courage Of A Skywalker"), Filters.title("Higher Ground")), 2));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        if (GameConditions.hasLostPile(game, playerId)
                && (TriggerConditions.justLost(game, effectResult, Filters.and(Filters.opponents(self), Filters.Sidious))
                || (TriggerConditions.battleInitiated(game, effectResult, playerId)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Skywalker)))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Retrieve 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, 1));
            actions.add(action);
        }

        if (TriggerConditions.isAboutToBeLostIncludingAllCardsSituation(game, effectResult, Filters.Reflection)) {
            PhysicalCard reflection = ((AboutToLeaveTableResult)effectResult).getCardAboutToLeaveTable();
            if (reflection != null) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Put Reflection in Used Pile");

                action.appendEffect(
                        new PreventEffectOnCardEffect(action, ((AboutToLeaveTableResult) effectResult).getPreventableCardEffect(), reflection, null));
                action.appendEffect(
                        new PlaceCardInUsedPileFromTableEffect(action, reflection));
                actions.add(action);
            }
        }

        return actions;
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && !GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.or(Filters.Skywalker, Filters.Jedi))) {
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

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (self.getWhileInPlayData() == null)
            return null;

        String text = "Chosen option: " + self.getWhileInPlayData().getTextValue();

        if (self.getWhileInPlayData().getPhysicalCards() != null
                && !self.getWhileInPlayData().getPhysicalCards().isEmpty()) {
            text += "; Revealed card" + GameUtils.s(self.getWhileInPlayData().getPhysicalCards().size())
                    + ": " + GameUtils.getAppendedNames(self.getWhileInPlayData().getPhysicalCards());
        }

        return text;
    }
}