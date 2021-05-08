package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.StackedOnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AttritionModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 15
 * Type: Character
 * Subtype: Jedi Master
 * Title: Master Yoda
 */
public class Card501_041 extends AbstractJediMaster {
    public Card501_041() {
        super(Side.LIGHT, 1, 5, 2, 7, 9, "Master Yoda", Uniqueness.UNIQUE);
        setLore("");
        setGameText("While 'communing': During your control phase, if you control more battlegrounds than opponent, retrieve 1 Force. Once per turn, may deploy a battleground with two [Dark Side] from Reserve Deck; reshuffle. Attrition against you is -2. You may not deploy [Permanent Weapon] cards.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_15);
        addPersona(Persona.YODA);
        setTestingText("Master Yoda");
    }

    public List<Modifier> getWhileStackedModifiers(SwccgGame game, PhysicalCard self) {
        Condition communing = new StackedOnCondition(self, Filters.Communing);
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Icon.PERMANENT_WEAPON, communing, self.getOwner()));
        modifiers.add(new AttritionModifier(self, -2, self.getOwner()));
        return modifiers;
    }

    public List<TopLevelGameTextAction> getGameTextTopLevelWhileStackedActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MASTER_YODA__DEPLOY_BATTLEGROUND;
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)){
            if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                    && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);

                action.setText("Deploy battleground from Reserve Deck");
                action.setActionMsg("Deploy a battleground with two [Dark Side] from Reserve Deck ");
                action.appendUsage(new OncePerTurnEffect(action));
                action.appendEffect(new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.battleground, Filters.iconCount(Icon.DARK_FORCE, 2)), true));

                actions.add(action);
            }
        }

        GameTextActionId gameTextActionId2 = GameTextActionId.OTHER_CARD_ACTION_1;

        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId2, Phase.CONTROL)
                && GameConditions.hasLostPile(game, playerId)
                && Filters.countAllOnTable(game, Filters.and(Filters.battleground, Filters.controls(playerId)))
                > Filters.countAllOnTable(game, Filters.and(Filters.battleground, Filters.controls(game.getOpponent(playerId))))
        ) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId2);

            action.setText("Retrieve 1 Force");
            action.appendUsage(new OncePerPhaseEffect(action));
            action.appendEffect(new RetrieveForceEffect(action, playerId, 1));

            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggersWhileStacked(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        String playerId = self.getOwner();
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        // Check if reached end of each control phase and action was not performed yet.
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && Filters.countAllOnTable(game, Filters.and(Filters.battleground, Filters.controls(playerId)))
                > Filters.countAllOnTable(game, Filters.and(Filters.battleground, Filters.controls(game.getOpponent(playerId))))
        ) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerId);

            action.setText("Retrieve 1 Force");
            action.setActionMsg("Retrieve 1 Force");
            action.appendUsage(new OncePerPhaseEffect(action));
            action.appendEffect(new RetrieveForceEffect(action, playerId, 1));

            actions.add(action);
        }

        return actions;
    }
}