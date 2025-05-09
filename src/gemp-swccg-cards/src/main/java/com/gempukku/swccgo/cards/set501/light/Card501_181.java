package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
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
import com.gempukku.swccgo.logic.modifiers.EachTrainingDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Yoda's Hope (V)
 */
public class Card501_181 extends AbstractNormalEffect {
    public Card501_181() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, Title.Yodas_Hope, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'You must feel the Force around you. Here, between you, me, the tree, the rock, everywhere! Yes, even between the land and the ship.'");
        setGameText("If your [Dagobah] objective on table, deploy on Yoda. Your training destiny draws are +1. At start of opponent's control phase, if Luke is present with his uncompleted Jedi Test, he may attempt it.");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Yoda's Hope (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(self), Icon.DAGOBAH, Filters.Objective));
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Yoda;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new EachTrainingDestinyModifier(self, Filters.any, 1));
        modifiers.add(new ModifyGameTextModifier(self, Filters.Jedi_Test, ModifyGameTextType.JEDI_TESTS__LUKE_ATTEMPTS_DURING_OPPONENTS_CONTROL_PHASE));
        return modifiers;
    }
}