package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.TriggerConditions;


import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

/**
 * Set: Hoth
 * Type: Effect
 * Title: Hoth Blockade
 */
public class Card501_022 extends AbstractNormalEffect {
    public Card501_022() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "Hoth Blockade", Uniqueness.UNIQUE);
        setLore("Death Squadron.");
        setGameText("Deploy on Hoth system. While a battleground marker site on table, adds one [Dark Side] icon and one [Light Side] icon here. Your Death Squadron cards deploy -1 here (-5 if Executor) and are immune to attrition < 3 here. Place in Used Pile if opponent Force drains here. [Immune to Alter.]");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Hoth Blockade");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Hoth_system;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter here = Filters.here(self);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new IconModifier(self, Filters.hasAttached(self), new OnTableCondition(self, Filters.and(Filters.battleground, Filters.marker_site)), Icon.DARK_FORCE, 1));
        modifiers.add(new IconModifier(self, Filters.hasAttached(self), new OnTableCondition(self, Filters.and(Filters.battleground, Filters.marker_site)), Icon.LIGHT_FORCE, 1));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(self), Filters.Death_Squadron_card), new CardMatchesEvaluator(-1, -5, Filters.Executor), here));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, Filters.and(Filters.your(self), Filters.Death_Squadron_card, here), 3));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(self.getOwner());
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if (TriggerConditions.forceDrainCompleted(game, effectResult, opponent, Filters.Hoth_system)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Place Hoth Blockade in Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}