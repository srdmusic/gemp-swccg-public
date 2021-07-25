package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.StackedOnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.StackedEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.DrawOneCardFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 16
 * Type: Character
 * Subtype: Jedi Master
 * Title: Master Qui-Gon Jinn, An Old Friend
 */
public class Card501_039 extends AbstractJediMaster {
    public Card501_039() {
        super(Side.LIGHT, 1, 7, 6, 7, 8, "Master Qui-Gon Jinn, An Old Friend", Uniqueness.UNIQUE);
        setLore("");
        setGameText("While 'communing': Your total power in battles is +1 for each Jedi 'communing.' Anakin and Obi-Wan may deploy -2 as a 'react.' Once per turn, may place a card from hand on Used Pile to draw top card of Force Pile. You may not deploy Rebels.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_16, Icon.EPISODE_I);
        addPersona(Persona.QUIGON);
        setTestingText("Master Qui-Gon Jinn, An Old Friend");
    }

    public List<Modifier> getWhileStackedModifiers(SwccgGame game, PhysicalCard self) {
        Condition communing = new StackedOnCondition(self, Filters.Communing);
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.Rebel, communing, self.getOwner()));
        modifiers.add(new TotalPowerModifier(self, Filters.battleLocation, new StackedEvaluator(self, Filters.Communing), self.getOwner()));
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy Anakin or Obi-Wan as a react", communing, self.getOwner(), Filters.or(Filters.Anakin, Filters.ObiWan), Filters.any, -2));
        return modifiers;
    }

    public List<TopLevelGameTextAction> getGameTextTopLevelWhileStackedActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)){
            if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.numCardsInForcePile(game, playerId) > 0) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);

                action.setText("Place card from hand on Used Pile");
                action.setActionMsg("Place card from hand on Used Pile to draw card from Force Pile");
                action.appendUsage(new OncePerTurnEffect(action));
                action.appendCost(new PutCardFromHandOnUsedPileEffect(action, playerId));
                action.appendEffect(new DrawOneCardFromForcePileEffect(action, playerId));

                return Collections.singletonList(action);
            }
        }
        return null;
    }
}