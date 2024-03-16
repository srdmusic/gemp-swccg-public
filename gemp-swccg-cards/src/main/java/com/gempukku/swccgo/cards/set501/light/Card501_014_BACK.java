package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.RecirculateEffect;
import com.gempukku.swccgo.logic.effects.ShuffleReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.PlaceCardOutOfPlayFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 23
 * Type: Objective
 * Title: I Can Bring You In Warm / ...Or I Can Bring You In Cold
 */
public class Card501_014_BACK extends AbstractObjective {
    public Card501_014_BACK() {
        super(Side.LIGHT, 7, Title.Or_I_Can_Bring_You_In_Cold, ExpansionSet.SET_23, Rarity.V);
        setGameText("May immediately re-circulate and shuffle your Reserve Deck. " +
                "While this side up, during battles involving 'The Asset', " +
                "opponent may not cancel your non-lightsaber weapon destiny draws and if 'The Asset' just lost, " +
                "place that character out of play. The gametext of the 'The Asset' is cancelled." +
                "Flip this card at the start of any move phase.");
        addIcons(Icon.MUDHORN, Icon.VIRTUAL_SET_23);
        setTestingText("...Or I Can Bring You In Cold");
    }


    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.cardFlipped(game, effectResult, self)) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerId);
            action.setText("Re-circulate and reshuffle.");
            action.setActionMsg("Re-circulate and reshuffle.");

            action.appendEffect(
                    new RecirculateEffect(action, playerId)
            );
            action.appendEffect(
                    new ShuffleReserveDeckEffect(action, playerId)
            );

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        Filter jediExceptLukeAndAhsoka = Filters.and(Filters.your(self), Filters.Jedi, Filters.except(Filters.or(Filters.Luke, Filters.Ahsoka)));
        Filter nonLightsaberWeapon = Filters.and(Filters.your(self), Filters.weapon, Filters.not(Filters.lightsaber));

        modifiers.add(new MayNotDeployModifier(self, Filters.or(jediExceptLukeAndAhsoka, Filters.and(Filters.or(Icon.EPISODE_I, Icon.EPISODE_VII), Filters.character)), self.getOwner()));
        modifiers.add(new MayNotCancelWeaponDestinyModifier(self, new InBattleCondition(Filters.findFirstActive(game, self, Filters.The_Asset)), game.getDarkPlayer(), nonLightsaberWeapon));
        modifiers.add(new CancelsGameTextModifier(self, Filters.The_Asset));
        return modifiers;
    }

    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new ArrayList<>();

        PhysicalCard holoPuck = Filters.findFirstActive(game, self, Filters.Holopuck);

        if (holoPuck != null
                && TriggerConditions.leavesTable(game, effectResult, Filters.The_Asset)) {

            if (effectResult.getType() == EffectResult.Type.LOST_FROM_TABLE || effectResult.getType() == EffectResult.Type.FORFEITED_TO_LOST_PILE_FROM_TABLE) {
                PhysicalCard leftTable = ((LostFromTableResult) effectResult).getCard();
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Place " + GameUtils.getCardLink(leftTable) + " out of play");
                action.setActionMsg("Place " + GameUtils.getCardLink(leftTable) + " out of play");
                action.appendEffect(
                        new PlaceCardOutOfPlayFromLostPileEffect(action, self.getOwner(), game.getOpponent(self.getOwner()), leftTable, false)
                );
                action.appendEffect(
                        new SetWhileInPlayDataEffect(action, holoPuck, null));
                actions.add(action);
            } else {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.appendEffect(
                        new SetWhileInPlayDataEffect(action, holoPuck, null));
                actions.add(action);
            }
        }


        if (TriggerConditions.isStartOfEachPhase(game, effectResult, Phase.MOVE)
                && GameConditions.canBeFlipped(game, self)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }

        return actions;
    }
}