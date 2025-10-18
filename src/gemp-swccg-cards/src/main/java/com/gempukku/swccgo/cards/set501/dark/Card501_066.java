package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeConvertedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: This Deal Is Getting Worse All The Time / Pray I Don't Alter It Any Further (V)
 */

public class Card501_066 extends AbstractObjective {
    public Card501_066() {
        super(Side.DARK, 0, Title.This_Deal_Is_Getting_Worse_All_The_Time, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy one Cloud City battleground site and I'm Sorry. For remainder of game, Surreptitious Glance may not cancel Dark Deal and your Bespin system may not be converted. While this side up, once per turn, may [upload] Cloud City Occupation, Dark Deal, or [Special Edition] Bespin system. Flip this card if [Set 23] Dark Deal on table, you occupy Bespin System, and opponent controls less than 3 Cloud City battlegrounds.");
        addIcons(Icon.CLOUD_CITY, Icon.PREMIUM, Icon.VIRTUAL_SET_26);
        setVirtualSuffix(true);
        setTestingText("This Deal Is Getting Worse All The Time (V)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Cloud_City_battleground_site, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Cloud City battleground site to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Im_Sorry, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose I'm Sorry to deploy";
                    }
                });
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        //For remainder of game
        modifiers.add(new ImmuneToTitleModifier(self, Filters.Dark_Deal, Title.Surreptitious_Glance));
        modifiers.add(new MayNotBeConvertedModifier(self, Filters.and(Filters.your(self), Filters.Bespin_system)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.THIS_DEAL_IS_GETTING_WORSE_ALL_THE_TIME_V__UPLOAD_CARD;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Take card into hand from Reserve Deck");
            action.setActionMsg("Take Cloud City Occupation, Dark Deal, or [Special Edition] Bespin system into hand from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Cloud_City_Occupation, Filters.Dark_Deal, Filters.and(Icon.SPECIAL_EDITION, Filters.Bespin_system)), true));
            actions.add(action);
        }
        return actions;
    }
}
