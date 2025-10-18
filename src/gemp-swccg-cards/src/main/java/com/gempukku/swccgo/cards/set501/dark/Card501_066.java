package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;

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
}
