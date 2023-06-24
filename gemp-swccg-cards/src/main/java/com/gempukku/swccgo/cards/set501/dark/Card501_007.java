package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Hoth Blockade
 */
public class Card501_007 extends AbstractNormalEffect {
    public Card501_007() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "Hoth Blockade", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Death Squadron.");
        setGameText("Deploy on Hoth system. Death Squadron starships deploy -1 here (-5 if Executor). While your Star Destroyer here, your AT-ATs deploy -1 to Hoth sites and Rebel starships deploy +1 here. While you control two Hoth sites, Haven is suspended here. [Immune to Alter.]");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_22);
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
        modifiers.add(new DeployCostToLocationModifier(self, Filters.Death_Squadron_starship, new CardMatchesEvaluator(-1, -5, Filters.Executor), here));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(self), Filters.AT_AT), new HereCondition(self, Filters.and(Filters.your(playerId), Filters.Star_Destroyer)), -1, Filters.relatedLocation(self)));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.Rebel_starship, new HereCondition(self, Filters.and(Filters.your(playerId), Filters.Star_Destroyer)), 1, here));
        modifiers.add(new SuspendsCardModifier(self, Filters.and(Filters.Haven, here), new ControlsCondition(playerId, 2, Filters.Hoth_site)));
        return modifiers;
    }
}