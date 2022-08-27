package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckSimultaneouslyWithCardEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Title: Galactic Republic Navy
 */
public class Card501_076 extends AbstractNormalEffect {
    public Card501_076() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Galactic Republic Navy", Uniqueness.UNIQUE);
        setGameText("Deploy on table. Your [Clone Army] starships with a Jedi or clone aboard are power +1 (+2 if both). Once per turn, may reveal a [Clone Army] starship from hand to take a [Clone Army] pilot character from Reserve Deck (or vice versa) and deploy both simultaneously; reshuffle. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Galactic Republic Navy");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(self), Icon.CLONE_ARMY, Filters.starship, Filters.hasAboard(self, Filters.or(Filters.clone, Filters.Jedi))), new CardMatchesEvaluator(1, 2, Filters.and(Filters.hasAboard(self, Filters.clone), Filters.hasAboard(self, Filters.Jedi)))));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        final Filter starship = Filters.and(Icon.CLONE_ARMY, Filters.starship);
        final Filter pilot = Filters.and(Icon.CLONE_ARMY, Filters.pilot);
        Filter filter = Filters.and(Filters.or(pilot, starship), Filters.isUniquenessOnTableNotReached);

        GameTextActionId gameTextActionId = GameTextActionId.GALACTIC_REPUBLIC_NAVY__DEPLOY_CLONE_ARMY_STARSHIP_AND_PILOT;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.hasInHand(game, playerId, filter)
                && GameConditions.canSearchReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Reveal pilot or starship from hand");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Choose target(s)
            action.appendTargeting(
                    new ChooseCardFromHandEffect(action, playerId, filter) {
                        @Override
                        protected void cardSelected(SwccgGame game, final PhysicalCard selectedCard) {
                            final Filter searchFilter;
                            if (pilot.accepts(game, selectedCard)) {
                                action.setActionMsg("Take a [Clone Army] starship into hand from Reserve Deck and deploy both simultaneously");
                                searchFilter = starship;
                            }
                            else {
                                action.setActionMsg("Take a [Clone Army] pilot into hand from Reserve Deck and deploy both simultaneously");
                                searchFilter = pilot;
                            }
                            // Perform result(s)
                            action.appendEffect(
                                    new ShowCardOnScreenEffect(action, selectedCard));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckSimultaneouslyWithCardEffect(action, selectedCard, searchFilter, true));
                        }
                    });
            actions.add(action);
        }
        return actions;
    }
}