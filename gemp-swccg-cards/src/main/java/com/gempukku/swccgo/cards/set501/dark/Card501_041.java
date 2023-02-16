package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.evaluators.InBattleEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: I'm Sending My Apprentice
 */
public class Card501_041 extends AbstractNormalEffect {
    public Card501_041() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "I'm Sending My Apprentice", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Maul channeled his rage within himself, thus drawing on the dark side of the Force in his confrontation with the Jedi.");
        setGameText("If [Theed Palace] Throne Room on table, deploy on table. Maul deploys -2 to [Theed Palace] Throne Room. If Maul in battle alone, your total battle destiny is +1 for each character in battle. May deploy Sidious' Lightsaber or Theed Palace Generator from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("I'm Sending My Apprentice");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Theed_Palace_Throne_Room);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.Maul, -2, Filters.Theed_Palace_Throne_Room));
        modifiers.add(new TotalBattleDestinyModifier(self, new InBattleCondition(self, Filters.and(Filters.Maul, Filters.alone)), new InBattleEvaluator(self, Filters.character), self.getOwner(), true));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.IM_SENDING_MY_APPRENTICE__DOWNLOAD_CARD;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Collections.singleton(Persona.SIDIOUS_LIGHTSABER), Title.Theed_Palace_Generator)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy Sidious' Lightsaber or Theed Palace Generator from Reserve Deck");

            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.persona(Persona.SIDIOUS_LIGHTSABER), Filters.Theed_Palace_Generator), true));
            actions.add(action);
        }

        return actions;
    }
}