package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
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
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayMoveOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.Effect;

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
        setGameText("If a Geonosis location on table, deploy on table. Once per battle, may deploy or move a clone as a 'react' to same location as your Jedi or Padawan. While your clone in battle with a Jedi or Padawan, opponent's total battle destiny is -1 (-2 if Yoda in battle). [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.CLONE_ARMY, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Battle Of Geonosis");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Geonosis_location);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(final SwccgGame game, final PhysicalCard self) {
        final String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        Condition oncePerBattleCondition = new Condition() {
            @Override
            public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                return game.getModifiersQuerying().getUntilEndOfBattleLimitCounter(self, playerId, self.getCardId(), GameTextActionId.OTHER_CARD_ACTION_1).getUsedLimit()<1;
            }
        };

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new TotalBattleDestinyModifier(self,
                new InBattleCondition(self, Filters.and(Filters.your(self), Filters.or(Filters.Jedi, Filters.padawan), Filters.with(self, Filters.and(Filters.your(self), Filters.clone)))),
                new ConditionEvaluator(-1, -2, new InBattleCondition(self, Filters.Yoda)), opponent));
        modifiers.add(new MayMoveOtherCardsAsReactToLocationModifier(self, "Move a clone as a react", oncePerBattleCondition, playerId, Filters.and(Filters.your(self), Filters.clone), Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.or(Filters.Jedi, Filters.padawan)))));
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy a clone as a react", oncePerBattleCondition, playerId, Filters.and(Filters.your(self), Filters.clone), Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.or(Filters.Jedi, Filters.padawan)))));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(String playerId, SwccgGame game, Effect effect, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isReact(game, effect)
                && effect.getAction()!=null) {
            PhysicalCard source = effect.getAction().getActionSource();

            // if this card is the source of the react then increment the per battle limit so the condition above can check for it
            if (source != null
                    && Filters.sameCardId(self).accepts(game, source)) {
                game.getModifiersQuerying().getUntilEndOfBattleLimitCounter(self, playerId, self.getCardId(), GameTextActionId.OTHER_CARD_ACTION_1).incrementToLimit(1,1);
            }
        }

        return null;
    }
}