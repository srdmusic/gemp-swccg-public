package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: The Hidden Path / Gather Allies And Train
 */

public class Card501_201_BACK extends AbstractObjective {
        public Card501_201_BACK() {
        super(Side.LIGHT, 7, Title.Gather_Allies_And_Train, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("While this side up, at the end of your turn, opponent loses 1 Force. When you initiate battle with a Jedi, you may retrieve 1 Force. Opponent's total battle destiny where they have a character of ability > 4 is -1 (-2 if an Inquisitor there). Force drain bonuses from your lightsabers may not be canceled. If your holocron is about to leave table, place it in Used Pile. During your move phase, may relocate a Jedi between a Jabiim site and a battleground site as a regular move. Flip this card if Jedi do not occupy two locations.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Gather Allies And Train");
    }
}
