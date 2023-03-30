package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PlayersTurnCondition;
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
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.DrawOneCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeModifiedModifier;
import com.gempukku.swccgo.logic.modifiers.InitiateBattlesForFreeModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveForfeitValueIncreasedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Leave Them To Me (V)
 */
public class Card501_115 extends AbstractNormalEffect {
    public Card501_115() {
        super(Side.DARK, 5, PlayCardZoneOption.ATTACHED, Title.Leave_Them_To_Me, Uniqueness.UNIQUE, ExpansionSet.DEATH_STAR_II, Rarity.C);
        setVirtualSuffix(true);
        setLore("'I will deal with them myself.'");
        setGameText("Deploy on a battleground. During opponent's turn, forfeit values may not be increased here. You initiate battles here for free. If you just initiated battle here, may draw top card of Reserve Deck. Opponent may not cancel or modify Force drains here.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.DEPLOYS_ON_LOCATION);
        setTestingText("Leave Them To Me (V) (Effect)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.battleground;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotHaveForfeitValueIncreasedModifier(self, Filters.here(self), new PlayersTurnCondition(opponent)));
        modifiers.add(new InitiateBattlesForFreeModifier(self, Filters.here(self), playerId));
        modifiers.add(new ForceDrainsMayNotBeModifiedModifier(self, Filters.here(self), opponent, null));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.here(self), opponent, null));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if(TriggerConditions.battleInitiatedAt(game, effectResult, playerId, Filters.here(self))
                && GameConditions.hasReserveDeck(game, playerId))   {

            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Draw top card of Reserve Deck");

            action.appendEffect(
                    new DrawOneCardFromReserveDeckEffect(action, playerId));
            return Collections.singletonList(action);
        }
        return null;
    }
}