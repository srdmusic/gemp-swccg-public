package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeConvertedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: This Deal Is Getting Worse All The Time / Pray I Don't Alter It Any Further (V)
 */

public class Card501_066_BACK extends AbstractObjective {
    public Card501_066_BACK() {
        super(Side.DARK, 7, Title.Pray_I_Dont_Alter_It_Any_Further, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("While this side up, It's a Trap! is canceled. If you have an alien/Imperial pair in battle, your total battle destiny is +2 (or +4 if alien is an Ugnaught). Your Force drain bonuses at same site as your Lando or your Lobot may not be canceled. While Vader present at a Cloud City site, Sense and Alter are canceled. While Executor at Bespin, Cloud City Occupation may not be canceled. Flip this card if opponent controls Bespin system (or three Cloud City battlegrounds).");
        addIcons(Icon.CLOUD_CITY, Icon.PREMIUM, Icon.VIRTUAL_SET_26);
        setVirtualSuffix(true);
        setTestingText("Pray I Don't Alter It Any Further (V)");
    }
    
    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        //For remainder of game
        modifiers.add(new ImmuneToTitleModifier(self, Filters.Dark_Deal, Title.Surreptitious_Glance));
        modifiers.add(new MayNotBeConvertedModifier(self, Filters.and(Filters.your(self), Filters.Bespin_system)));
        return modifiers;
    }
}
