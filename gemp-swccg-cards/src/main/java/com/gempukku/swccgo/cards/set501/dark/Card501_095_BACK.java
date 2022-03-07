package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PreventEffectOnCardEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 17
 * Type: Objective
 * Title: At Last The Jedi Are No More / Revenge Of The Sith
 */
public class Card501_095_BACK extends AbstractObjective {
    public Card501_095_BACK() {
        super(Side.DARK, 7, Title.Revenge_Of_The_Sith);
        setGameText("While this side up, A Sith Legend, Always Two There Are and Sith are destiny +2. If a Jedi was just lost from same location as your Dark Jedi, opponent loses 1 Force. Opponent may not cancel or reduce Force drains at their battlegrounds where you have a Dark Jedi. " +
                "Flip this card if no Dark Jedi are on table.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("Revenge Of The Sith (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayNotPlayModifier(self, Filters.and(Filters.Dark_Jedi, Filters.except(Filters.and(Icon.EPISODE_I, Filters.Sidious)), Filters.except(Filters.Sith_Apprentice)), self.getOwner()));
        modifiers.add(new AddCardTypeModifier(self, Filters.or(Filters.and(Filters.your(self), Icon.EPISODE_I, Filters.Sidious), Filters.Sith_Apprentice), CardType.SITH));
        modifiers.add(new DestinyModifier(self, Filters.or(Filters.A_Sith_Legend, Filters.Always_Two_There_Are, Filters.Sith), 2));
        modifiers.add(new ForceDrainsMayNotBeReducedModifier(self, Filters.and(Filters.opponents(self), Filters.battleground, Filters.occupiesWith(playerId, self, Filters.Dark_Jedi)), opponent, playerId));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.and(Filters.opponents(self), Filters.battleground, Filters.occupiesWith(playerId, self, Filters.Dark_Jedi)), opponent, playerId));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && !GameConditions.canSpot(game, self, Filters.Dark_Jedi)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);

        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (TriggerConditions.justLostFromLocation(game, effectResult, Filters.Jedi, Filters.sameSiteAs(self, Filters.Dark_Jedi))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        return null;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (self.getWhileInPlayData() == null)
            return null;

        return "Chosen Apprentice is " + self.getWhileInPlayData().getTextValue();
    }
}