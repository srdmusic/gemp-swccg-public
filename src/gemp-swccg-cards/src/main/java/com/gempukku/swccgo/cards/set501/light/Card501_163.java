package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.AtLeastNumberOfAlienSpeciesOnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Ancient Watering Hole
 */
public class Card501_163 extends AbstractNormalEffect {
    public Card501_163() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Ancient Watering Hole", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Deploy on table. Your Rep is immune to attrition. While you have alien characters of five different species on table, at locations where you have an alien: Your total battle destiny is +1, your aliens are forfeit +1 and, if location is a battleground, your Force drains are +1. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_10, Icon.EPISODE_VII);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Ancient Watering Hole (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(final SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        List<Modifier> modifiers = new LinkedList<Modifier>();
        final PhysicalCard rep = game.getGameState().getRep(playerId);
        Filter repFilter = Filters.none;
        if (rep != null) {
            repFilter = Filters.sameTitle(rep);
        }
        Filter yourAlienFilter = Filters.and(Filters.your(playerId), Filters.alien);
        Filter battlegroundsWithYourAlien = Filters.and(Filters.battleground, Filters.sameSiteAs(self, yourAlienFilter));

        Condition fiveDifferentSpeciesCondition = new AtLeastNumberOfAlienSpeciesOnTableCondition(game, self, 5);
        Condition yourAlienInBattle = new InBattleCondition(self, yourAlienFilter);

        modifiers.add(new ImmuneToAttritionModifier(self, repFilter));
        modifiers.add(new TotalBattleDestinyModifier(self, new AndCondition(fiveDifferentSpeciesCondition, yourAlienInBattle), 1, playerId));
        modifiers.add(new ForfeitModifier(self, yourAlienFilter, fiveDifferentSpeciesCondition, 1));
        modifiers.add(new ForceDrainModifier(self, battlegroundsWithYourAlien, fiveDifferentSpeciesCondition, 1, playerId));

        return modifiers;
    }
}