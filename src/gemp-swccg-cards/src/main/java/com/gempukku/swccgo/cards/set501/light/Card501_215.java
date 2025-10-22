package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnBottomOfForcePileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master
 * Title: Kelnacca
 */

public class Card501_215 extends AbstractJediMaster {
    public Card501_215() {
        super(Side.LIGHT, 2, 5, 5, 7, 7, "Kelnacca", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Wookiee.");
        setGameText("At the start of each turn, if Wookiee Homestead on table, may place a card from hand on bottom of Force Pile to draw top card from Force Pile. Once per game, may [download] a unique (•) Forest. Immune to Wookiee Strangle and You Are Beaten.");
        addIcons(Icon.EPISODE_I, Icon.WARRIOR, Icon.VIRTUAL_SET_26);
        setSpecies(Species.WOOKIEE);
        addImmuneToCardTitle(Title.Wookiee_Strangle);
        addImmuneToCardTitle(Title.You_Are_Beaten);
        setTestingText("Kelnacca");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Card action 1
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isStartOfEachTurn(game, effectResult)
                && GameConditions.canSpot(game, self, Filters.Wookiee_Homestead)
                && GameConditions.hasHand(game, playerId)
                && GameConditions.hasForcePile(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Place card from hand on bottom of Force Pile");
            action.setActionMsg("Draw top card of Force Pile.");
            // Update usage limit(s)
            action.appendUsage(
                new OncePerTurnEffect(action));
            // Pay cost(s)
            action.appendCost(
                new PutCardFromHandOnBottomOfForcePileEffect(action, playerId));
            // Perform result(s)
            action.appendEffect(
                new DrawCardIntoHandFromForcePileEffect(action, playerId));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.KELNACCA__DOWNLOAD_FOREST;
        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.hasReserveDeck(game, playerId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy a unique Forest from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.unique, Filters.forest), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
