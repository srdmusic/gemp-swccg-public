package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Character
 * Subtype: Imperial
 * Title: Ensign Eli Vanto
 */
public class Card501_029 extends AbstractImperial {
    public Card501_029() {
        super(Side.DARK, 2, 3, 2, 3, 6, "Ensign Eli Vanto", Uniqueness.UNIQUE);
        setPolitics(1);
        setGameText("Adds 2 to power of any capital starship he pilots (3 if beyond parsec 5). If with Thrawn, your starships here are power and hyperspeed +1. During your control phase, may use 1 Force to take a card with 'artwork' in game text into hand from Reserve Deck; reshuffle.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_18);
        setTestingText("Ensign Eli Vanto");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, new ConditionEvaluator(2, 3, new AtCondition(self, Filters.systemAtOrAboveParsec(6))), Filters.capital_starship));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(self), Filters.starship, Filters.here(self)), new WithCondition(self, Filters.Thrawn), 1));
        modifiers.add(new HyperspeedModifier(self, Filters.and(Filters.your(self), Filters.starship, Filters.here(self)), new WithCondition(self, Filters.Thrawn), 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ENSIGN_ELI_VANTO__UPLOAD_CARD;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canUseForce(game, playerId, 1)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take card into hand from Reserve Deck");
            action.setActionMsg("Take card with 'artwork' in game text into hand from Reserve Deck");

            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Pay cost(s)
            action.appendCost(
                    new UseForceEffect(action, playerId, 1));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.gameTextContains("artwork"), Filters.gameTextContains("artworks")), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
