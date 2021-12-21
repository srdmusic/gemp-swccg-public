package com.gempukku.swccgo.cards.set217.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.evaluators.OutOfPlayEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Epic Event
 * Title: Be With Me
 */
public class Card217_029 extends AbstractEpicEventDeployable {
    public Card217_029() {
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, "Be With Me", Uniqueness.UNIQUE);
        setGameText("Deploy on an Ahch-To location. Opponent generates no Force here. \n" +
                "A thousand generations live in you now: Rey is power and forfeit +1 for each Jedi out of play \n" +
                "Bring back the balance, Rey, as I did: [Set 14] Rey's battle and weapon destiny draws are +1. \n" +
                "Feel the Force Flowing Through You: At same location as [Set 14] Rey, characters may not add battle destinies. \n" +
                "Rey, the Force will be with you. Always: [Set 14] Rey ignores your Objective deployment restrictions.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.AhchTo_location;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        Filter set14Rey = Filters.and(Icon.VIRTUAL_SET_14, Filters.Rey);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new GenerateNoForceModifier(self, Filters.hasAttached(self), game.getOpponent(self.getOwner())));
        modifiers.add(new PowerModifier(self, Filters.Rey, new OutOfPlayEvaluator(self, Filters.Jedi)));
        modifiers.add(new ForfeitModifier(self, Filters.Rey, new OutOfPlayEvaluator(self, Filters.Jedi)));
        modifiers.add(new EachBattleDestinyModifier(self, Filters.sameLocationAs(self, set14Rey), 1, self.getOwner()));
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.any, set14Rey, 1));
        modifiers.add(new MayNotAddBattleDestinyDrawsModifier(self, Filters.and(Filters.character, Filters.at(Filters.sameLocationAs(self, set14Rey)))));
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, set14Rey, null, self.getOwner(), Filters.and(Filters.your(self), Icon.EPISODE_VII, Filters.Objective)));
        return modifiers;
    }
}