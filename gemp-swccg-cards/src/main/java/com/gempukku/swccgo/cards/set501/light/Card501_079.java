package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.conditions.PilotingAtCondition;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.*;


/**
 * Set: Set 20
 * Type: Character
 * Subtype: Republic
 * Title: Admiral Yularen
 */
public class Card501_079 extends AbstractRepublic {
    public Card501_079() {
        super(Side.LIGHT, 1, 3, 3, 3, 5, "Admiral Yularen", Uniqueness.UNIQUE);
        setLore("Leader.");
        setGameText("Adds 2 to power of anything he pilots (3 if a capital starship). While piloting at a battleground system, your Force drains here may not be reduced or canceled. While piloting Resolute, attrition against opponent is +1 here (+2 if with a [Separatist] or [Trade Federation] starship).");
        addIcons(Icon.EPISODE_I, Icon.PILOT, Icon.WARRIOR, Icon.CLONE_ARMY, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.LEADER, Keyword.ADMIRAL);
        setTestingText("Admiral Yularen");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, new CardMatchesEvaluator(2, 3, Filters.capital_starship)));
        modifiers.add(new ForceDrainsMayNotBeReducedModifier(self, Filters.here(self), new PilotingAtCondition(self, Filters.battleground_system), null, self.getOwner()));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.here(self), new PilotingAtCondition(self, Filters.battleground_system), null, self.getOwner()));
        modifiers.add(new AttritionModifier(self, new AndCondition(new PilotingCondition(self, Filters.title("Resolute")), new InBattleCondition(self)), new ConditionEvaluator(1, 2, new WithCondition(self, Filters.and(Filters.or(Icon.SEPARATIST, Icon.TRADE_FEDERATION), Filters.starship))), game.getOpponent(self.getOwner())));
        return modifiers;
    }
}