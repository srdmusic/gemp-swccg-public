package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Munificent-Class Star Frigate
 */
public class Card501_030 extends AbstractCapitalStarship {
    public Card501_030() {
        super(Side.DARK, 4, 4, 6, 5, null, 4, 6, "Munificent-Class Star Frigate", Uniqueness.RESTRICTED_2, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Banking Clan.");
        setGameText("May add 2 pilots, 5 passengers, and 1 shuttle. Permanent pilot provides ability of 2. While at a [Clone Army] or [Separatist] system, immune to attrition < 6 and, if you control, opponent's Force drains at related sites are -1.");
        addIcons(Icon.NAV_COMPUTER, Icon.EPISODE_I, Icon.SCOMP_LINK, Icon.SEPARATIST, Icon.VIRTUAL_SET_25);
        addIcon(Icon.PILOT, 1);
        addModelType(ModelType.MUNIFICENT_CLASS_STAR_FRIGATE);
        setPilotCapacity(2);
        setPassengerCapacity(5);
        setStarfighterCapacity(1, Filters.shuttle);
        setTestingText("Munificent-Class Star Frigate");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(2) {});
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new AtCondition(self, Filters.and(Filters.system, Filters.or(Icon.CLONE_ARMY, Icon.SEPARATIST))), 6));
        modifiers.add(new ForceDrainModifier(self, Filters.relatedSite(self), new AtCondition(self, Filters.and(Filters.system, Filters.controls(playerId), Filters.or(Icon.CLONE_ARMY, Icon.SEPARATIST))),-1, opponent));
        return modifiers;
    }
}