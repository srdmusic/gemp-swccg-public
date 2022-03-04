package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ChoiceMadeResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Slave Quarters (V)
 */
public class Card501_086 extends AbstractSite {
    public Card501_086() {
        super(Side.LIGHT, Title.Slave_Quarters, Title.Tatooine);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("");
        setLocationLightSideGameText("Deploys only at start of game. Anakin may initiate battle and Force drain regardless of restrictions from Their Fire Has Gone Out Of The Universe.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.SKYWALKER, Icon.TATOOINE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("Tatooine: Slave Quarters (V) (ERRATA)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        // Deploys only at start of game
        return GameConditions.isDuringStartOfGame(game);
    }


    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        Condition condition = new AndCondition(new OnTableCondition(self, Filters.and(Icon.SKYWALKER, Filters.Objective)),new OnTableCondition(self, Filters.Their_Fire_Has_Gone_Out_Of_The_Universe));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new IgnoresObjectiveRestrictionsWhenForceDrainingAtLocationModifier(self, Filters.sameLocationAs(self, Filters.Anakin), condition, playerId));
        modifiers.add(new IgnoresObjectiveRestrictionsWhenInitiatingBattleAtLocationModifier(self, Filters.sameLocationAs(self, Filters.Anakin), condition, playerId));
        return modifiers;
    }
}