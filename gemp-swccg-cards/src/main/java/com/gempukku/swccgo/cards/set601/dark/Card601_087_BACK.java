package com.gempukku.swccgo.cards.set601.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.DuringBattleAtCondition;
import com.gempukku.swccgo.cards.conditions.DuringBattleWithParticipantCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Block 4
 * Type: Objective
 * Title: Hunt Down And Destroy The Jedi / Their Fire Has Gone Out Of The Universe (V)
 */
public class Card601_087_BACK extends AbstractObjective {
    public Card601_087_BACK() {
        super(Side.DARK, 7, Title.Their_Fire_Has_Gone_Out_Of_The_Universe);
        setVirtualSuffix(true);
        setGameText("While this side up, opponent's Force drain bonuses are canceled and your Force drains at battlegrounds may not be canceled.  During your control phase, may retrieve 1 Force.  During your control phase, may use 2 Force to take any one card without ability into hand from Reserve Deck; reshuffle.  If targeting a vehicle or starship with Lightsaber Parry, Tarkin's Orders, or There Is No Conflict, that destiny draw is +2.\n" +
                "Flip this card if opponent has a unique (*) character of ability > 3 present at a battleground site.");
        addIcons(Icon.SPECIAL_EDITION, Icon.LEGACY_BLOCK_4);
        setAsLegacy(true);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotPlayModifier(self, Filters.and(Icon.EPISODE_I, Filters.Dark_Jedi), self.getOwner()));

        modifiers.add(new CancelOpponentsForceDrainBonusesModifier(self, new TrueCondition()));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.battleground, playerId));
        //TODO If targeting a vehicle or starship with Lightsaber Parry, Tarkin's Orders, or There Is No Conflict, that destiny draw is +2.
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.hasLostPile(game, playerId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve 1 Force");
            action.setActionMsg("Retrieve 1 Force");
            action.appendUsage(new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, 1));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.LEGACY__THEIR_FIRE_HAS_GONE_OUT_OF_THE_UNIVERSE_V__UPLOAD_CARD;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canUseForce(game, playerId, 2)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Take a card into hand from Reserve Deck");
            action.setActionMsg("Take a card without ability into hand from Reserve Deck");
            action.appendUsage(new OncePerPhaseEffect(action));
            action.appendCost(new UseForceEffect(action, playerId, 2));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.not(Filters.hasAbilityOrHasPermanentPilotWithAbility), true, true));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && (GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.and(Filters.opponents(self), Filters.unique, Filters.character, Filters.abilityMoreThan(3), Filters.at(Filters.battleground_site))))) {

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
