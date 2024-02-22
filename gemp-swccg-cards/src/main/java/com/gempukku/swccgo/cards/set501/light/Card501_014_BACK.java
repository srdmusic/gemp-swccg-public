package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
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
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromOffTableEffect;
import com.gempukku.swccgo.logic.effects.RecirculateEffect;
import com.gempukku.swccgo.logic.effects.ShuffleReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Set: Set 23
 * Type: Objective
 * Title: I Can Bring You In Warm / ...Or I Can Bring You In Cold
 */
public class Card501_014_BACK extends AbstractObjective {
    public Card501_014_BACK() {
        super(Side.LIGHT, 7, Title.Or_I_Can_Bring_You_In_Cold, ExpansionSet.SET_23, Rarity.V);
        setGameText("May immediately re-circulate and shuffle your Reserve Deck. " +
                "While this side up, during battles involving \"The Asset\", opponent may not cancel your non-lightsaber weapon destiny draws and if \"The Asset\" just lost, place that character out of play. " +
                "Flip this card during any move phase.");
        addIcons(Icon.MUDHORN);
        addIcons(Icon.VIRTUAL_SET_23);
        setTestingText("...Or I Can Bring You In Cold");
    }
// FIXME: Asset doesnt get placed out of play (puck relocated before asset goes lost probably what is causing it).
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
        Filter jediExeptLukeAhsoka = Filters.and(Filters.your(self), Filters.Jedi, Filters.except(Filters.or(Filters.Luke, Filters.Ahsoka)));
        Filter nonLightsaberWeapon = Filters.and(Filters.your(self), Filters.weapon, Filters.not(Filters.lightsaber));

        modifiers.add(new MayNotDeployModifier(self, Filters.or(jediExeptLukeAhsoka, Filters.and(Filters.or(Icon.EPISODE_I, Icon.EPISODE_VII), Filters.character)), self.getOwner()));
        modifiers.add(new MayNotCancelWeaponDestinyModifier(self, new InBattleCondition(Filters.findFirstActive(game, self, Filters.The_Asset)), game.getDarkPlayer(), nonLightsaberWeapon));
        return modifiers;
    }

    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Set asset data for placing OOP
        if (GameConditions.canSpot(game, self, Filters.The_Asset) && self.getWhileInPlayData() == null) {
            PhysicalCard card = Filters.findFirstActive(game, self, Filters.The_Asset);
            self.setWhileInPlayData(new WhileInPlayData(card.getTitle()));
        }

        // Check condition(s)
        if (TriggerConditions.justLost(game, effectResult, Filters.and(Filters.opponents(self.getOwner()), Filters.character))) {
            LostFromTableResult lostFromTable = (LostFromTableResult) effectResult;
            final PhysicalCard cardToBeLost = lostFromTable.getCard();
            final WhileInPlayData theAssetData = self.getWhileInPlayData();

            if (theAssetData != null) {
                if (theAssetData.getTextValue() != null && Objects.equals(theAssetData.getTextValue(), cardToBeLost.getTitle())) {
                    RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                    action.setActionMsg("Place \"The Asset\" out of play");
                    // Perform result(s)
                    action.appendEffect(
                            new PlaceCardOutOfPlayFromOffTableEffect(action, cardToBeLost));
                    // reset asset data
                    self.setWhileInPlayData(null);
                    return Collections.singletonList(action);
                }
            }
        }

        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.isDuringEitherPlayersPhase(game, Phase.MOVE)
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