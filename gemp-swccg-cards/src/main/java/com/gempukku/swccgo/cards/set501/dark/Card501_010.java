package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.evaluators.NegativeEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;

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
        super(Side.DARK, Title.Cantina, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Your Force generation here is -1 for each pair of aliens (or smugglers) opponent has here.");
        setLocationLightSideGameText("Your Force generation here is -1 for each pair of aliens (or sandtroopers) opponent has here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_21);
        setTestingText("Tatooine: Cantina (DARK) (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self) {
        final Filter alienFilter = Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.alien, Filters.here(self));
        final Filter alternateFilter = Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.smuggler, Filters.here(self));

        Evaluator pairEvaluator = new Evaluator() {
            @Override
            public float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard cardAffected) {
                int opponentNonAlienAlternate = Filters.countActive(gameState.getGame(), self, Filters.and(Filters.not(alienFilter), alternateFilter));
                int opponentNonAlternateAlien = Filters.countActive(gameState.getGame(), self, Filters.and(Filters.not(alternateFilter), alienFilter));
                int opponentAlienAndAlternate = Filters.countActive(gameState.getGame(), self, Filters.and(alienFilter, alternateFilter));;

                if (opponentNonAlienAlternate%2==1 && opponentAlienAndAlternate>0) {
                    opponentNonAlienAlternate++;
                    opponentAlienAndAlternate--;
                }
                if (opponentNonAlternateAlien%2==1 && opponentAlienAndAlternate>0) {
                    opponentNonAlternateAlien++;
                    opponentAlienAndAlternate--;
                }

                return opponentNonAlienAlternate/2 + opponentNonAlternateAlien/2 + opponentAlienAndAlternate/2;
            }

            @Override
            public float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard cardAffected, PhysicalCard otherCard) {
                return 0;
            }
        };


        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new NegativeEvaluator(pairEvaluator), playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, final PhysicalCard self) {
        final Filter alienFilter = Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.alien, Filters.here(self));
        final Filter alternateFilter = Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.sandtrooper, Filters.here(self));

        Evaluator pairEvaluator = new Evaluator() {
            @Override
            public float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard cardAffected) {
                int opponentNonAlienAlternate = Filters.countActive(gameState.getGame(), self, Filters.and(Filters.not(alienFilter), alternateFilter));
                int opponentNonAlternateAlien = Filters.countActive(gameState.getGame(), self, Filters.and(Filters.not(alternateFilter), alienFilter));
                int opponentAlienAndAlternate = Filters.countActive(gameState.getGame(), self, Filters.and(alienFilter, alternateFilter));;

                if (opponentNonAlienAlternate%2==1 && opponentAlienAndAlternate>0) {
                    opponentNonAlienAlternate++;
                    opponentAlienAndAlternate--;
                }
                if (opponentNonAlternateAlien%2==1 && opponentAlienAndAlternate>0) {
                    opponentNonAlternateAlien++;
                    opponentAlienAndAlternate--;
                }

                return opponentNonAlienAlternate/2 + opponentNonAlternateAlien/2 + opponentAlienAndAlternate/2;
            }

            @Override
            public float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard cardAffected, PhysicalCard otherCard) {
                return 0;
            }
        };
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new NegativeEvaluator(pairEvaluator), playerOnLightSideOfLocation));
        return modifiers;
    }
}