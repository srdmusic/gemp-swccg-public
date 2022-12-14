package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.modifiers.EachBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayMoveOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Battle Of Geonosis
 */
public class Card501_072 extends AbstractNormalEffect {
    public Card501_072() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Battle Of Geonosis", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If a Geonosis location on table, deploy on table. Your clones may deploy or move as a 'react' to same location as He Is A Coward or your Jedi. While your Jedi/clone pair in battle, opponent's battle destiny draws are -1 (-2 if Yoda in battle). [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Battle Of Geonosis");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Geonosis_location);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new EachBattleDestinyModifier(self,
                new InBattleCondition(self, Filters.and(Filters.your(self), Filters.Jedi, Filters.with(self, Filters.and(Filters.your(self), Filters.clone)))),
                new ConditionEvaluator(-1, -2, new InBattleCondition(self, Filters.Yoda)), opponent));
        modifiers.add(new MayMoveOtherCardsAsReactToLocationModifier(self, "Move a clone as a react", playerId, Filters.and(Filters.your(self), Filters.clone), Filters.sameLocationAs(self, Filters.or(Filters.title("He Is A Coward"), Filters.and(Filters.your(self), Filters.Jedi)))));
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy a clone as a react", playerId, Filters.and(Filters.your(self), Filters.clone), Filters.sameLocationAs(self, Filters.or(Filters.title("He Is A Coward"), Filters.and(Filters.your(self), Filters.Jedi)))));
        return modifiers;
    }
}