package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

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
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Demotion (V)
 */
public class Card501_172 extends AbstractNormalEffect {
    public Card501_172() {
        super(Side.LIGHT, 3, PlayCardZoneOption.ATTACHED, Title.Demotion, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Repercussions for failure are severe in the Imperial military. Many officers prefer demotion to 'alternative' punishment from Darth Vader.");
        setGameText("Deploy on an alien leader, Imperial, or senator (except Bib, Jabba, Sidious, Thrawn, Vader, or Xizor). Character's game text is canceled. Opponent's total battle destiny is -1 here (-2 if Kallus or Vader here).");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcon(Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Demotion (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        Filter baseRequirements = Filters.or(Filters.and(Filters.alien, Filters.leader), Filters.Imperial, Filters.senator);
        Filter notExceptions = Filters.not(Filters.or(Filters.Bib, Filters.Jabba, Filters.Sidious, Filters.Thrawn, Filters.Vader, Filters.Xizor));
        
        return Filters.and(baseRequirements, notExceptions);
    }

    @Override
    protected Filter getGameTextValidTargetFilterToRemainAttachedToAfterCrossingOver(final SwccgGame game, final PhysicalCard self, PlayCardOptionId playCardOptionId) {
        Filter baseRequirements = Filters.or(Filters.and(Filters.alien, Filters.leader), Filters.Imperial, Filters.senator);
        
        return baseRequirements;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter attachedTo = Filters.hasAttached(self);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new CancelsGameTextModifier(self, attachedTo));
        return modifiers;
    }
}