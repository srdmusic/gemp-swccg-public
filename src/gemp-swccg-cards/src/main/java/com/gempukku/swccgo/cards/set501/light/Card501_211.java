package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.CommuningCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Anakin Skywalker, Redeemed
 */
public class Card501_211 extends AbstractRebel {
    public Card501_211() {
        super(Side.LIGHT, 0, 6, 3, 6, 8, "Anakin Skywalker, Redeemed", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'You were right about me. Tell your sister ... you were right.'");
        setGameText("While 'communing': You may not deploy Jedi Masters or characters of ability < 4 (except droids); your Force drains are -1; once per turn, if Return Of The Jedi on table, may [download] a Death Star II site.");
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_27);
        addPersona(Persona.ANAKIN);
        setTestingText("Anakin Skywalker, Redeemed");
    }

    public List<Modifier> getWhileStackedModifiers(SwccgGame game, PhysicalCard self) {
        Condition whileAnakinCommuning = new CommuningCondition(self);
        String playerId = self.getOwner();
        
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.or(Filters.Jedi_Master, Filters.and(Filters.character, Filters.abilityLessThan(4), Filters.not(Filters.droid))), whileAnakinCommuning, playerId));
        modifiers.add(new ForceDrainModifier(self, whileAnakinCommuning, -1, playerId));
        return modifiers;
    }

    public List<TopLevelGameTextAction> getGameTextTopLevelWhileStackedActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ANAKIN_SKYWALKER_REDEEMED__DOWNLOAD_DS2_SITE;
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        if (game.getModifiersQuerying().isCommuning(game.getGameState(), self)) {
            if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                    && GameConditions.canSpot(game, self, Filters.Return_Of_The_Jedi)
                    && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);

                action.setText("Deploy Death Star II site");
                action.setActionMsg("Deploy a Death Star II site from Reserve Deck");
                action.appendUsage(new OncePerTurnEffect(action));
                action.appendEffect(new DeployCardFromReserveDeckEffect(action, Filters.Death_Star_II_site, true));

                actions.add(action);
            }
        }
        return actions;
    }
}
