package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.StackCardsFromOutsideDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployAsIfFromHandModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Title: Battle Droid Reinforcements
 */
public class Card501_031 extends AbstractNormalEffect {
    public Card501_031() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Battle Droid Reinforcements", Uniqueness.UNIQUE);
        setGameText("If Geonosis on table, deploy on table. When deployed, stack two B-1 Battle Droids face up here from outside your deck. B-1 Battle Droids may deploy from here as if from hand (for -1 Force). [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Battle Droid Reinforcements");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Geonosis_system);
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {

        if (TriggerConditions.justDeployed(game, effectResult, self)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Stack two B-1 Battle Droids here");
            action.setActionMsg("Stack two B-1 Battle Droids here from outside your deck");
            action.appendEffect(
                    new StackCardsFromOutsideDeckEffect(action, self.getOwner(), 2, 2, self, false, Filters.title("B-1 Battle Droid")));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayDeployAsIfFromHandModifier(self, Filters.and(Filters.stackedOn(self), Filters.title("B-1 Battle Droid"))));
        modifiers.add(new DeployCostModifier(self, Filters.and(Filters.stackedOn(self), Filters.title("B-1 Battle Droid")), -1));
        return modifiers;
    }
}
