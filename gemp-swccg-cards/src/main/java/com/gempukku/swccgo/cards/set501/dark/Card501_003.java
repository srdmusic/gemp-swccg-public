package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ExchangeCardInHandWithTopCardOfReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.DefinedByGameTextDefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.MayBeTargetedByWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Device
 * Title: Observation Holocam (V)
 */
public class Card501_003 extends AbstractDevice {
    public Card501_003() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "Observation Holocam");
        setVirtualSuffix(true);
        setLore("Remote surveillance viewers with droid controllers supplement security. Can activate alarms and automated weapons when needed, bringing help to endangered locations.");
        setGameText("Deploy on your interior site. In a battle opponent initiates here, may place this card in Lost Pile to exchange a card in your hand with the top card of your Reserve Deck. May be targeted by weapons like a character (defense value = 2). If 'hit', device lost.");
        addIcons(Icon.VIRTUAL_SET_17);
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        setTestingText("Observation Holocam (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Icon.INTERIOR_SITE, Filters.site);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayBeTargetedByWeaponsModifier(self, Filters.weapon));
        modifiers.add(new DefinedByGameTextDefenseValueModifier(self, 2));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.justHit(game, effectResult, self)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make " + GameUtils.getFullName(self) + " lost");
            action.appendEffect(
                    new LoseCardFromTableEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (GameConditions.isDuringBattleInitiatedBy(game, game.getOpponent(playerId))
                && GameConditions.isDuringBattleAt(game, Filters.here(self))
                && GameConditions.hasHand(game, playerId)
                && GameConditions.hasReserveDeck(game, playerId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Lose device to exchange cards");
            action.setActionMsg("Lose device to exchange a card from hand with top card of Reserve Deck");

            action.appendCost(
                    new LoseCardFromTableEffect(action, self));
            action.appendEffect(new ExchangeCardInHandWithTopCardOfReserveDeckEffect(action, playerId));

            return Collections.singletonList(action);
        }

        return null;
    }
}