package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDarkJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.conditions.PresentAtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Character
 * Subtype: Imperial
 * Title: Morgan Elsbeth
 */
public class Card501_045 extends AbstractDarkJediMaster {
    public Card501_045() {
        super(Side.DARK, 2, 4, 4, 4, 6, "Morgan Elsbeth", Uniqueness.UNIQUE);
        setLore("Female leader.");
        setGameText("While present at a battleground site and you control more systems than opponent, Force drain +1 here. Power and defense value +2 while Thrawn at a system with parsec > 5. Immune to attrition < 3.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.FEMALE, Keyword.LEADER);
        setTestingText("Morgan Elsbeth");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);
        Condition controlMoreSystemsThanOpponentCondition = new Condition() {
            @Override
            public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                return Filters.countTopLocationsOnTable(gameState.getGame(), Filters.and(Filters.system, Filters.controls(playerId)))
                        > Filters.countTopLocationsOnTable(gameState.getGame(), Filters.and(Filters.system, Filters.controls(opponent)));
            }
        };

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, Filters.here(self), new AndCondition(new PresentAtCondition(self, Filters.battleground_site), controlMoreSystemsThanOpponentCondition), 1, playerId));
        modifiers.add(new PowerModifier(self, new OnTableCondition(self, Filters.and(Filters.Thrawn, Filters.at(Filters.systemAboveParsec(5)))), 2));
        modifiers.add(new DefenseValueModifier(self, new OnTableCondition(self, Filters.and(Filters.Thrawn, Filters.at(Filters.systemAboveParsec(5)))), 2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }
}
