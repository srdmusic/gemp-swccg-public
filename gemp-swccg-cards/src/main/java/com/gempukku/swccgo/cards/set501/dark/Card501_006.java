package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DockingBayTransitFromCostModifier;
import com.gempukku.swccgo.logic.modifiers.DockingBayTransitToCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Mustafar: Private Platform (Docking Bay)
 */
public class Card501_006 extends AbstractSite {
    public Card501_006() {
        super(Side.DARK, "Mustafar: Private Platform (Docking Bay)", Title.Mustafar);
        setLocationDarkSideGameText("May deploy a starfighter with “Vader” in title here from Reserve Deck; reshuffle. Vanee is power +2 here.");
        setLocationLightSideGameText("If Vader or Vanee on table, your docking bay transit to or from here requires +3 Force (+5 if both).");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 0);
        addIcons(Icon.EXTERIOR_SITE, Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_16);
        addKeyword(Keyword.DOCKING_BAY);
        setTestingText("Mustafar: Private Platform (Docking Bay)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.persona(Persona.VANEE), Filters.here(self)), 2));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Condition vaderOrVaneeOnTable = new OrCondition(new OnTableCondition(self, Filters.Vader), new OnTableCondition(self, Filters.persona(Persona.VANEE)));
        Condition vaderAndVaneeOnTable = new AndCondition(new OnTableCondition(self, Filters.Vader), new OnTableCondition(self, Filters.persona(Persona.VANEE)));
        modifiers.add(new DockingBayTransitFromCostModifier(self, vaderOrVaneeOnTable, new ConditionEvaluator(3, 5, vaderAndVaneeOnTable), playerOnLightSideOfLocation));
        modifiers.add(new DockingBayTransitToCostModifier(self, vaderOrVaneeOnTable, new ConditionEvaluator(3, 5, vaderAndVaneeOnTable), playerOnLightSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextDarkSideTopLevelActions(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MUSTAFAR_PRIVATE_PLATFORM__DOWNLOAD_STARSHIP;
        if (GameConditions.canDeployCardFromReserveDeck(game, playerOnDarkSideOfLocation, self, gameTextActionId)) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy starfighter with 'Vader' in title here");
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.and(Filters.starfighter, Filters.titleContains("Vader")), Filters.here(self), true)
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}
