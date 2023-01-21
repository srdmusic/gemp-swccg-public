package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.evaluators.AddEvaluator;
import com.gempukku.swccgo.cards.evaluators.DivideEvaluator;
import com.gempukku.swccgo.cards.evaluators.HereEvaluator;
import com.gempukku.swccgo.cards.evaluators.NegativeEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Cantina (V)
 */
public class Card501_010 extends AbstractSite {
    public Card501_010() {
        super(Side.DARK, Title.Cantina, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PREMIERE, Rarity.R2);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Your Force generation here is -1 for each pair of aliens (or smugglers) opponent has here.");
        setLocationLightSideGameText("Your Force generation here is -1 for each pair of aliens (or sandtroopers) opponent has here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_21);
        setTestingText("Tatooine: Cantina (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        Evaluator pairEvaluator = new AddEvaluator(new DivideEvaluator(new HereEvaluator(self, Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.alien)), 2, false),
                new DivideEvaluator(new HereEvaluator(self, Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.smuggler)), 2, false));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new NegativeEvaluator(pairEvaluator), playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        Evaluator pairEvaluator = new AddEvaluator(new DivideEvaluator(new HereEvaluator(self, Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.alien)), 2, false),
                new DivideEvaluator(new HereEvaluator(self, Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.sandtrooper)), 2, false));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new NegativeEvaluator(pairEvaluator), playerOnLightSideOfLocation));
        return modifiers;
    }
}