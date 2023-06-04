package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TakeCardFromVoidIntoHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: Gangster's Paradise
 */
public class Card501_117 extends AbstractLostOrStartingInterrupt {
    public Card501_117() {
        super(Side.DARK, 4, "Gangster's Paradise", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jabba's Palace is considered a safe haven to many on the run. It is also widely known to provide luxurious accommodations to its welcomed guests.");
        setGameText("LOST: Deploy an interior Jabba's Palace site from Reserve Deck; reshuffle. STARTING: If you have deployed a Jabba's Palace site, deploy Jabba's Haven and up to two Effects that deploy on table and are always immune to Alter. Place Interrupt in hand.");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_21);
        setTestingText("Gangster's Paradise");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.GANGSTERS_PARADISE__DEPLOY_SITE;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
            action.setText("Deploy a site");
            // Allow response(s)
            action.allowResponses("Deploy an interior Jabba's Palace site from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.interior_site, Filters.Jabbas_Palace_site), true));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return actions;
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, SwccgGame game, final PhysicalCard self) {
        final Filter yourSiteEvenIfConverted = Filters.and(Filters.Jabbas_Palace_site, Filters.or(Filters.your(self), Filters.convertedLocationOnTopOfLocation(Filters.your(self))));

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, yourSiteEvenIfConverted)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Deploy Jabba's Haven and up to 2 Effects that deploy on table and are always immune to Alter.");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title("Jabba's Haven"),  true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.immune_to_Alter,
                                            Filters.or(Filters.gameTextContains("deploy on table"), Filters.gameTextContains("deploy on your side of table"))), 1, 2, true, false));
                            action.appendEffect(
                                    new TakeCardFromVoidIntoHandEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }
        return null;
    }
}
