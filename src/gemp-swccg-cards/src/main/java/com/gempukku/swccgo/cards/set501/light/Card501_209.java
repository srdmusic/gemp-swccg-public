package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.DuringBattleWithParticipantCondition;
import com.gempukku.swccgo.cards.evaluators.InBattleEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
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
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.modifiers.MayFireOneWeaponTwicePerBattleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetPersonalForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: The Greatest Of All The Jedi
 */
public class Card501_209 extends AbstractNormalEffect {
    public Card501_209() {
        super(Side.LIGHT, 0, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.The_Greatest_Of_All_The_Jedi, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If your [Skywalker] Epic Event on table, deploy on table. Your personal Force generation = 2. While Jedi Anakin in battle alone, your total battle destiny is +1 for each character there and, if opponent has four characters (or three aliens) in battle, Anakin may fire a weapon twice. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.SKYWALKER, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("The Greatest Of All The Jedi");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Epic_Event));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        List<Modifier> modifiers = new LinkedList<>();

        // Your personal Force generation = 2.
        modifiers.add(new ResetPersonalForceGenerationModifier(self, 2, playerId));

        // While your Jedi Anakin is in battle alone.
        Condition jediAnakinInBattleAlone = new DuringBattleWithParticipantCondition(
                Filters.and(Filters.your(self), Filters.Anakin, Filters.Jedi, Filters.alone));

        // Your total battle destiny is +1 for each character in the battle.
        modifiers.add(new TotalBattleDestinyModifier(self, jediAnakinInBattleAlone,
                new InBattleEvaluator(self, Filters.character), playerId));

        // If opponent has four or more characters (or three or more aliens) in the battle, Anakin may fire a weapon twice.
        Filter opponentsCharacter = Filters.and(Filters.opponents(self), Filters.character);
        Filter opponentsAlien = Filters.and(Filters.opponents(self), Filters.alien);
        Condition mayFireWeaponTwice = new AndCondition(jediAnakinInBattleAlone,
                new OrCondition(
                        new DuringBattleWithParticipantCondition(4, opponentsCharacter),
                        new DuringBattleWithParticipantCondition(3, opponentsAlien)));
        modifiers.add(new MayFireOneWeaponTwicePerBattleModifier(self,
                Filters.and(Filters.your(self), Filters.Anakin), mayFireWeaponTwice, Filters.weapon));

        return modifiers;
    }
}
