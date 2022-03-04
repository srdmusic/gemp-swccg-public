package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;


import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Hoth Blockade
 */
public class Card501_022 extends AbstractNormalEffect {
    public Card501_022() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "Hoth Blockade", Uniqueness.UNIQUE);
        setLore("Death Squadron.");
        setGameText("Deploy on Hoth system. Death Squadron Star Destroyers deploy -1 here (-5 if Executor). If your Star Destroyer here, your AT-ATs deploy -1 to related locations. While you control two Hoth sites, Haven suspended here. [Immune to Alter.]");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
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
        String playerId = self.getOwner();
        
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(self), Filters.Death_Squadron_card, Filters.Star_Destroyer), new CardMatchesEvaluator(-1, -5, Filters.Executor), here));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(self), Filters.AT_AT), new AtCondition(self, Filters.and(Filters.your(playerId), Filters.Star_Destroyer), here), -1, Filters.relatedLocation(self)));
        modifiers.add(new SuspendsCardModifier(self, Filters.and(Filters.Haven, here), new ControlsCondition(playerId, 2, Filters.Hoth_site)));
        return modifiers;
    }
}