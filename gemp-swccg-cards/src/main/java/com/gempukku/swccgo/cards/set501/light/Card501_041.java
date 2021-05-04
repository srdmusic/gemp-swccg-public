package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.StackedOnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AttritionModifier;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;

import java.util.Collections;
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

        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)){
            if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                    && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);

                action.setText("Deploy battleground from Reserve Deck");
                action.setActionMsg("Deploy a battleground with two [Dark Side] from Reserve Deck ");
                action.appendUsage(new OncePerTurnEffect(action));
                action.appendEffect(new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.battleground, Filters.iconCount(Icon.DARK_FORCE, 2)), true));

                return Collections.singletonList(action);
            }
        }
        return null;
    }
}