package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.LoseForceFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeChokedModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelBattleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 21
 * Type: Objective
 * Title: You Can Either Profit By This... / Or Be Destroyed (V)
 */
public class Card501_088_BACK extends AbstractObjective {
    public Card501_088_BACK() {
        super(Side.LIGHT, 7, Title.Or_Be_Destroyed, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setGameText("Immediately retrieve 3 Force (cannot be canceled) once per game. " +
                "While this side up, Han's weapon destiny draws are +1. While Han with your non-'hit' Chewie, Lando, or Luke, Han may not be 'choked' or targeted by Force Lightning, Set For Stun, or weapons. During your control phase, opponent loses 1 Force for each battleground location occupied by Han, Luke, Leia, Chewie, or your Lando (limit 3 and cannot be reduced). " +
                "Flip this card if Han is captured or not on table.");
        addIcons(Icon.PREMIUM, Icon.VIRTUAL_SET_21);
        setTestingText("Or Be Destroyed (V)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OR_BE_DESTROYED__RETRIEVE_FORCE;

        // Check condition(s)
        if (TriggerConditions.cardFlipped(game, effectResult, self)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            int amountToRetrieve = 3;

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve Force");
            action.setActionMsg("Have " + playerId + " retrieve " + amountToRetrieve + " Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, amountToRetrieve) {
                        @Override
                        public boolean mayNotBeCanceled() {
                            return true;
                        }
                    });
            return Collections.singletonList(action);
        }

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && !GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.and(Filters.Han, Filters.not(Filters.captive)))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isEndOfYourPhase(game, self, effectResult, Phase.CONTROL)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)) {
            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.battleground,
                    Filters.occupiesWith(playerId, self, Filters.or(Filters.Han, Filters.Luke, Filters.Leia, Filters.Chewie, Filters.Lando))));
            if (numForce > 0) {
                numForce = Math.min(3, numForce);


                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setPerformingPlayer(playerId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));

                // Perform result(s)
                if (game.getModifiersQuerying().hasGameTextModification(game.getGameState(), self, ModifyGameTextType.LEGACY__OR_BE_DESTROYED__FORCE_LOSS)) {
                    // Force loss from ...Or Be Destroyed must come from Reserve Deck (if possible) and may not be reduced below 2
                    action.appendEffect(new LoseForceFromReserveDeckEffect(action, opponent, numForce, 2));
                } else {
                    action.appendEffect(
                            new LoseForceEffect(action, opponent, numForce, true));
                }
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        Filter hanFilter = Filters.and(Filters.Han, Filters.with(self, Filters.and(Filters.your(self), Filters.not(Filters.hit), Filters.or(Filters.Chewie, Filters.Lando, Filters.Luke))));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotCancelBattleModifier(self, null, self.getOwner()));
        modifiers.add(new MayNotBeChokedModifier(self, hanFilter));
        modifiers.add(new MayNotBeTargetedByModifier(self, hanFilter, Filters.or(Filters.Force_Lightning, Filters.Set_For_Stun)));
        modifiers.add(new MayNotBeTargetedByWeaponsModifier(self, hanFilter));
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.any, Filters.Han, 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)) {
            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.battleground,
                    Filters.occupiesWith(playerId, self, Filters.or(Filters.Han, Filters.Luke, Filters.Leia, Filters.Chewie, Filters.Lando))));
            if (numForce > 0) {
                numForce = Math.min(3, numForce);


                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                if (game.getModifiersQuerying().hasGameTextModification(game.getGameState(), self, ModifyGameTextType.LEGACY__OR_BE_DESTROYED__FORCE_LOSS)) {
                    // Force loss from ...Or Be Destroyed must come from Reserve Deck (if possible) and may not be reduced below 2
                    action.appendEffect(new LoseForceFromReserveDeckEffect(action, opponent, numForce, 2));
                } else {
                    action.appendEffect(
                            new LoseForceEffect(action, opponent, numForce, true));
                }
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}