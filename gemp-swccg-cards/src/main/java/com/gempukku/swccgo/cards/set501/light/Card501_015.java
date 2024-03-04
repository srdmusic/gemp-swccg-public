package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.InPlayDataSetCondition;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.choose.PlaceCardOutOfPlayFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.CancelCardOnTableResult;
import com.gempukku.swccgo.logic.timing.results.ForfeitedCardToUsedPileFromTableResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;
import com.gempukku.swccgo.logic.timing.results.PlacedCardOutOfPlayFromTableResult;
import com.gempukku.swccgo.logic.timing.results.PlayCardResult;
import com.gempukku.swccgo.logic.timing.results.PutCardInForcePileFromTableResult;
import com.gempukku.swccgo.logic.timing.results.PutCardInReserveDeckFromTableResult;
import com.gempukku.swccgo.logic.timing.results.PutCardInUsedPileFromTableResult;
import com.gempukku.swccgo.logic.timing.results.ReturnedCardToHandFromTableResult;
import com.gempukku.swccgo.logic.timing.results.StackedFromTableResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Device
 * Title: Holopuck
 */
public class Card501_015 extends AbstractDevice {
    public Card501_015() {
        super(Side.LIGHT, 0, PlayCardZoneOption.ATTACHED, Title.Holopuck, Uniqueness.UNIQUE, ExpansionSet.SET_23, Rarity.V);
        setGameText("Deploy on Bounty Hunter's Guild. " +
                "Unless 'The Asset' on table, whenever an opponent deploys a character of ability > 2 (except a Dark Jedi) to a site," +
                "that character is 'The Asset' for as long as that character remains on table.");
        addIcons(Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        setTestingText("Holopuck");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Bounty_Hunters_Guild;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new KeywordModifier(self, Filters.isInCardInPlayData(self), new InPlayDataSetCondition(self), Keyword.THE_ASSET));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(self.getOwner());
        final Filter filter = Filters.and(Filters.opponents(self), Filters.character, Filters.abilityMoreThan(2), Filters.non_Dark_Jedi_character);

        if (!GameConditions.canSpot(game, self, Filters.The_Asset)
                && TriggerConditions.justDeployed(game, effectResult, opponent, filter)) {
            PhysicalCard deployedCard = ((PlayCardResult) effectResult).getPlayedCard();
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make " + GameUtils.getCardLink(deployedCard) + " 'The Asset'");
            action.setActionMsg("Make " + GameUtils.getCardLink(deployedCard) + " 'The Asset'");
            action.appendEffect(
                    new ShowCardOnScreenEffect(action, deployedCard)
            );
            action.appendEffect(
                    new SetWhileInPlayDataEffect(action, self, new WhileInPlayData(deployedCard)));
            actions.add(action);
        }

        if (TriggerConditions.leavesTable(game, effectResult, Filters.The_Asset)) {

            PhysicalCard leftTable = null;
            boolean placeOutPlay = false;

            if (effectResult.getType() == EffectResult.Type.LOST_FROM_TABLE || effectResult.getType() == EffectResult.Type.FORFEITED_TO_LOST_PILE_FROM_TABLE)
                leftTable = ((LostFromTableResult) effectResult).getCard();
            if (GameConditions.hasGameTextModification(game, self, ModifyGameTextType.HOLOPUCK__PLACES_OUT_OF_PLAY))
                placeOutPlay = true;
            else if (effectResult.getType() == EffectResult.Type.FORFEITED_TO_USED_PILE_FROM_TABLE)
                leftTable = ((ForfeitedCardToUsedPileFromTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.CANCELED_ON_TABLE)
                leftTable = ((CancelCardOnTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.STACKED_FROM_TABLE)
                leftTable = ((StackedFromTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.RETURNED_TO_HAND_FROM_TABLE)
                leftTable = ((ReturnedCardToHandFromTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.PUT_IN_RESERVE_DECK_FROM_TABLE)
                leftTable = ((PutCardInReserveDeckFromTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.PUT_IN_FORCE_PILE_FROM_TABLE)
                leftTable = ((PutCardInForcePileFromTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.PUT_IN_USED_PILE_FROM_TABLE)
                leftTable = ((PutCardInUsedPileFromTableResult) effectResult).getCard();
            else if (effectResult.getType() == EffectResult.Type.PLACED_OUT_OF_PLAY_FROM_TABLE)
                leftTable = ((PlacedCardOutOfPlayFromTableResult) effectResult).getCard();


            if (leftTable != null) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText(GameUtils.getCardLink(leftTable) + " is no longer the 'The Asset'");
                action.setActionMsg(GameUtils.getCardLink(leftTable) + " is no longer 'The Asset'");
                if (placeOutPlay) {
                    action.appendEffect(
                            new PlaceCardOutOfPlayFromLostPileEffect(action, self.getOwner(), game.getOpponent(self.getOwner()), leftTable, false)
                    );
                }
                action.appendEffect(
                        new SetWhileInPlayDataEffect(action, self, null));
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (self.getWhileInPlayData() != null
                && self.getWhileInPlayData().getPhysicalCard() != null) {
            return GameUtils.getCardLink(self.getWhileInPlayData().getPhysicalCard()) + " is 'The Asset'";
        }
        return null;
    }
}
