package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCreature;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Creature
 * Title: Porg
 */
public class Card501_083 extends AbstractCreature {
    public Card501_083() {
        super(Side.LIGHT, 5, 2, 1, 3, 0, "Porg", Uniqueness.RESTRICTED_3);
        setGameText("Habitat: Ahch-To sites, either player’s starship (uses no capacity). Does not attack. Characters present (or starship aboard) are cumulatively power and defense value -1 (+1 if yours). Adds one [Light Side] icon here.");
        addModelType(ModelType.SEADWELLING);
        addIcons(Icon.SELECTIVE_CREATURE, Icon.VIRTUAL_SET_19);
        setTestingText("Porg");
    }

    @Override
    protected Filter getGameTextHabitatFilter(String playerId, final SwccgGame game, final PhysicalCard self) {
        return Filters.or(Filters.AhchTo_site, Filters.starship);
    }

    public boolean habitatIncludesAboardStarship() {
        return true;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotAttackModifier(self, self));
        modifiers.add(new IconModifier(self, Filters.sameLocation(self), Icon.LIGHT_FORCE));
        modifiers.add(new PowerModifier(self, Filters.or(Filters.and(Filters.character, Filters.present(self)),  Filters.and(Filters.starship, Filters.or(Filters.hasAttached(self), Filters.hasAboard(self)))), new TrueCondition(), new CardMatchesEvaluator(-1, 1, Filters.your(self)), true));
        modifiers.add(new DefenseValueModifier(self, Filters.or(Filters.and(Filters.character, Filters.present(self)),  Filters.and(Filters.starship, Filters.or(Filters.hasAttached(self), Filters.hasAboard(self)))), new TrueCondition(), new CardMatchesEvaluator(-1, 1, Filters.your(self)), true));
        return modifiers;
    }
}
