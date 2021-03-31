package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.ResetForceDrainModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Epic Event
 * Title: Emperor's Orders
 */
public class Card501_029 extends AbstractEpicEventDeployable {
    public Card501_029() {
        super(Side.DARK, PlayCardZoneOption.ATTACHED, "Emperor's Orders");
        setGameText("The Alliance Will Die...: Deploy on Executor if you have no objective. Flagship Operations may deploy regardless of deployment restrictions. You cards may not add more than 2 to the power, destiny and forfeit of a squadron." +
                "...As Will Your Friends: Where you have a TIE with a capital ship, your force drains = 3. Your TIE assault squadrons may deploy for 3 force (without replacement). If Executor lost, this card lost and you lose 3 force." +
                "'I'm Hit!:' During battle, opponent may place their A-wing with Executor in Lost Pile to cancel Executor’s immunity to attrition");
        addIcons(Icon.VIRTUAL_SET_15);
        setTestingText("Emperor's Orders");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Executor;
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return !GameConditions.canSpot(game, self, Filters.and(Filters.your(playerId), Filters.Objective));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        //You cards may not add more than 2 to the power, destiny and forfeit of a squadron.
        // Your TIE assault squadrons may deploy for 3 force (without replacement).

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Flagship_Operations, ModifyGameTextType.FLAGSHIP_OPERATIONS__MAY_IGNORE_DEPLOYMENT_RESTRICTIONS));
        modifiers.add(new ResetForceDrainModifier(self, Filters.and(Filters.your(self.getOwner()), Filters.sameLocationAs(self, Filters.TIE), Filters.sameLocationAs(self, Filters.capital_starship)), 3));
        return modifiers;
    }

    //If Executor lost, this card lost and you lose 3 force
    //During battle, opponent may place their A-wing with Executor in Lost Pile to cancel Executor’s immunity to attrition
}
