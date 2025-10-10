package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetDeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.ResetPowerModifier;


/**
 * Set: Playtesting
 * Type: Objective
 * Title: The Hidden Path / Gather Allies And Train
 */

public class Card501_201 extends AbstractObjective {
    public Card501_201() {
        super(Side.LIGHT, 0, Title.The_Hidden_Path, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Mining Village, Safehouse, Underground Corridor, and Fallen Order. For remainder of game, you may not deploy <> locations or Jedi (except Jedi Survivors). Once per turn, may [download] a holocron, Jabiim location, or non-[Reflections III] battleground (except Kamino system). While this side up, Jedi Survivors are deploy = 2, power = 3, and deploy only to Mining Village. Nabrun Leids and Odin Nesloor may not 'transport' Jedi. Your Force drains at Mapuzo sites are -1. Flip this card if Jedi occupy two non-Mapuzo locations.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("The Hidden Path");
    }

@Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Mining_Village, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Mining Village to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Safehouse, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Safehouse to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Underground_Corridor, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Underground Corridor to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Fallen_Order, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Fallen Order to deploy";
                    }
                });
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        Filter genericLocations = Filters.and(Filters.generic, Filters.location);
        Filter jediExceptJediSurvivors = Filters.and(Filters.Jedi, Filters.not(Filters.Jedi_Survivor));

        List<Modifier> modifiers = new LinkedList<Modifier>();
        // For remainder of game
        modifiers.add(new MayNotDeployModifier(self, Filters.or(genericLocations, jediExceptJediSurvivors), playerId));

        // While this side up
        modifiers.add(new ResetDeployCostModifier(self, Filters.Jedi_Survivor, 2));
        modifiers.add(new ResetPowerModifier(self, Filters.Jedi_Survivor, 3));
        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.Jedi_Survivor, Filters.not(Filters.Mining_Village)));
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.Jedi, Filters.or(Filters.Nabrun_Leids, Filters.Odin_Nesloor)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.THE_HIDDEN_PATH__DOWNLOAD_CARD;

        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);

            Filter nonRef3BattlegroundExceptKaminoSystem = Filters.and(Filters.not(Icon.REFLECTIONS_III), Filters.battleground, Filters.not(Filters.Kamino_system));

            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy a holocron, Jabiim location, or non-[Reflections III] battleground (except Kamino system) from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.holocron, Filters.Jabiim_location, nonRef3BattlegroundExceptKaminoSystem), true));
            actions.add(action);
        }

        return actions;
    }

}
