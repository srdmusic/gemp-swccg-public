package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: The Hidden Path / Gather Allies And Train
 */

public class Card501_201 extends AbstractObjective {
    public Card501_201() {
        super(Side.LIGHT, 0, Title.The_Hidden_Path, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Mining Village, Safehouse, Underground Corridor, and Fallen Order. For remainder of game, you may not deploy <> locations or Jedi (except Jedi Survivors). Once per turn, may [download] a holocron, Jabiim location, or non-[Reflections III] battleground (except Kamino). While this side up, Jedi Survivors are deploy = 2, power = 3, and deploy only to Mining Village. Nabrun Leids and Odin Nesloor may not 'transport' Jedi. Your Force drains at Mapuzo sites are -1. Flip this card if Jedi occupy two non-Mapuzo locations.");
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
        return modifiers;
    }

}
