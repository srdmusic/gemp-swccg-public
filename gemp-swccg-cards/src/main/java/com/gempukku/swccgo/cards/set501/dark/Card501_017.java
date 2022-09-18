package com.gempukku.swccgo.cards.set501.dark;


import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.ArmedWithCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Title: Trained In Your Jedi Arts
 */
public class Card501_017 extends AbstractNormalEffect {
    public Card501_017() {
        super(Side.DARK, 4, PlayCardZoneOption.ATTACHED, "Trained In Your Jedi Arts", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on your character. While you have two or fewer characters here, opponent may not cancel or reduce Force drains at same battleground. " +
                "While character armed with a lightsaber, weapon and battle destiny draws (and this character's game text) may not be canceled here.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        addKeyword(Keyword.DEPLOYS_ON_CHARACTERS);
        setTestingText("Trained In Your Jedi Arts");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self.getOwner()), Filters.character);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        String player = self.getOwner();
        String opponent = game.getOpponent(player);
        Filter sameBattleground = Filters.and(Filters.sameLocation(self), Filters.battleground);
        Condition twoOrFewerCharactersHereCondition = new NotCondition(new HereCondition(self, 3, false, Filters.and(Filters.your(player), Filters.character)));

        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, sameBattleground, twoOrFewerCharactersHereCondition, opponent, player));
        modifiers.add(new ForceDrainsMayNotBeReducedModifier(self, sameBattleground, twoOrFewerCharactersHereCondition, opponent, player));

        PhysicalCard attachedTo = self.getAttachedTo();
        Condition armedWithLightsaberCondition = new ArmedWithCondition(attachedTo, Filters.lightsaber);

        modifiers.add(new MayNotCancelWeaponDestinyModifier(self, armedWithLightsaberCondition, opponent, Filters.any, attachedTo));
        modifiers.add(new MayNotCancelBattleDestinyModifier(self, Filters.here(self), armedWithLightsaberCondition, opponent));
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self, attachedTo, armedWithLightsaberCondition));
        return modifiers;
    }
}
