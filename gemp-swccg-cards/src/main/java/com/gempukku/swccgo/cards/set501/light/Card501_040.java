package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.StackedOnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 16
 * Type: Character
 * Subtype: Rebel
 * Title: Master Kenobi
 */
public class Card501_040 extends AbstractRebel {
    public Card501_040() {
        super(Side.LIGHT, 1, 5, 5, 6, 9, "Master Kenobi", Uniqueness.UNIQUE);
        setLore("");
        setGameText("While 'communing': Once per turn, may deploy a battleground from Reserve Deck that is related to a location on table; reshuffle. If you just initiated battle, opponent loses 1 Force (2 if non-[Permanent Weapon] Luke in battle). You may not deploy Jedi (except Yoda).");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_16);
        addPersona(Persona.OBIWAN);
        setTestingText("Master Kenobi");
    }

    public List<Modifier> getWhileStackedModifiers(SwccgGame game, PhysicalCard self) {
        Condition communing = new StackedOnCondition(self, Filters.Communing);
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.Jedi, Filters.except(Filters.Yoda)), communing, self.getOwner()));
        return modifiers;
    }

    public List<TopLevelGameTextAction> getGameTextTopLevelWhileStackedActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MASTER_KENOBI__DEPLOY_BATTLEGROUND;

        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)){
            if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);

                action.setText("Deploy battleground from Reserve Deck");
                action.setActionMsg("Deploy a battleground from Reserve Deck that is related to a location on table");
                action.appendUsage(new OncePerTurnEffect(action));

                Collection<PhysicalCard> locationsOnTable = Filters.filterTopLocationsOnTable(game, Filters.any);
                Collection<PhysicalCard> reserveDeck = game.getGameState().getReserveDeck(playerId);
                Collection<PhysicalCard> locationsToDeployFromReserveDeck = new LinkedList<>();
                for(PhysicalCard c:locationsOnTable) {
                    locationsToDeployFromReserveDeck.addAll(Filters.filter(reserveDeck, game, Filters.relatedLocationEvenWhenNotInPlay(c)));
                }

                action.appendEffect(new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.battleground, Filters.in(locationsToDeployFromReserveDeck)), true));

                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggersWhileStacked(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)
            && TriggerConditions.battleInitiated(game, effectResult, self.getOwner())) {
            String opponent = game.getOpponent(self.getOwner());

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            int forceLoss = 1;
            if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.Luke, Filters.not(Filters.weapon_or_character_with_permanent_weapon))))
                forceLoss = 2;

            action.setText("Opponent loses "+forceLoss+" Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, forceLoss));
            return Collections.singletonList(action);
        }
        return null;
    }
}