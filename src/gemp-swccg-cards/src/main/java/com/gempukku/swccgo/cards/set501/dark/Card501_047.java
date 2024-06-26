package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.BattleDamageLimitModifier;
import com.gempukku.swccgo.logic.modifiers.CancelOpponentsForceDrainBonusesModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: Captain Sarkli (V)
 */
public class Card501_047 extends AbstractImperial {
    public Card501_047() {
        super(Side.DARK, 2, 2, 2, 2, 4, Title.Captain_Sarkli, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.R);
        setVirtualSuffix(true);
        setLore("Piett's nephew. Once granted audience with Emperor. On fast-track to promotion. Absolutely fearless spy.");
        setGameText("Text: Adds 2 to power of anything he pilots. " +
                "While at opponent’s war room, opponent’s Force drain bonuses are canceled and their battle damage here is limited to 2. " +
                "While at a 'probed' or 'liberated' system, Force drains at battlegrounds may not be canceled. " +
                "Immune to Dodonna and You Are Beaten.");
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.SPY, Keyword.CAPTAIN);
        addImmuneToCardTitle(Title.General_Dodonna);
        addImmuneToCardTitle(Title.You_Are_Beaten);
        setTestingText("Captain Sarkli (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));

        AtCondition atOpponentsWarRoomCondition = new AtCondition(self, Filters.and(Filters.opponents(playerId), Filters.war_room));
        modifiers.add(new CancelOpponentsForceDrainBonusesModifier(self, atOpponentsWarRoomCondition));
        modifiers.add(new BattleDamageLimitModifier(self, Filters.here(self), atOpponentsWarRoomCondition, 2, opponent));

        AtCondition atProbedOrLiberatedSystemCondition = new AtCondition(self, Filters.and(Filters.system,
                Filters.or(Filters.hasStacked(Filters.probeCard), Filters.liberated_system)));

        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.battleground, atProbedOrLiberatedSystemCondition, playerId));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.battleground, atProbedOrLiberatedSystemCondition, opponent));
        return modifiers;
    }
}
