package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: He's No Good To Me Dead
 */
public class Card501_026 extends AbstractUsedOrLostInterrupt {
    public Card501_026() {
        super(Side.DARK, 4, "He's No Good To Me Dead", Uniqueness.UNIQUE);
        setGameText("For remainder of turn, Fett's weapon destinies are +1 and targets he 'hits' are forfeit = 0. OR Once per game, if a battle was just initiated where Fett is escorting Jabba's Prize, cancel that battle (opponent's characters there may move away for free). [Immune to Sense.]");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("He's No Good To Me Dead");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {

        return null;
    }
}