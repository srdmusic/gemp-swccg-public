package com.gempukku.swccgo.cards.set501.dark;

import java.util.List;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost Or Starting
 * Title: No Civility, Only Politics (V)
 */
public class Card501_121 extends AbstractLostOrStartingInterrupt {
    public Card501_121(){
        super(Side.DARK, 4, "No Civility, Only Politics", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("LOST: [upload] a Coruscant Guard. STARTING: If My Lord, Is That Legal? on table, deploy With Thunderous Applause and two Effects that deploy on your side of the table, deploy for free, and are always immune to Alter. Place Interrupt in hand.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("No Civility, Only Politics (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        return null;
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        return null;
    }

}
