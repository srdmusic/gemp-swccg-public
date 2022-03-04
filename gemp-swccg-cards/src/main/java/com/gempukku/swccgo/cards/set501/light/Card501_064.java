package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckSimultaneouslyWithCardEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Best Starpilot In The Galaxy
 */
public class Card501_064 extends AbstractNormalEffect {
    public Card501_064() {
        super(Side.LIGHT, 5, PlayCardZoneOption.ATTACHED, "Best Starpilot In The Galaxy", Uniqueness.UNIQUE);
        setGameText("Deploy on your [Skywalker] Epic Event; may deploy Polis Massa. During your deploy phase, may reveal Azure Angel, Falcon, or Red 5 from hand; take its matching pilot into hand (or vice versa) from Reserve Deck and deploy both simultaneously; reshuffle. [Immune to Alter.]");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Best Starpilot In The Galaxy");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Epic_Event);
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.BEST_STARPILOT_IN_THE_GALAXY__DEPLOY_POLIS_MASSA;
        if (TriggerConditions.justDeployed(game, effectResult, self)
            && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Polis_Massa, true)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Polis Massa from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Polis_Massa_system, true));
            return Collections.singletonList(action);

        }

        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.BEST_STARPILOT_IN_THE_GALAXY__REVEAL_SHIP_OR_PILOT;

        final Filter starshipFilter = Filters.or(
                Filters.Azure_Angel,
                Filters.Falcon,
                Filters.Red_5);

        Filter filter = Filters.and(Filters.or(Filters.pilot, starshipFilter), Filters.isUniquenessOnTableNotReached);


        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.hasInHand(game, playerId, filter)
                && GameConditions.canSearchReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Reveal pilot or starship from hand");
            action.setActionMsg("Reveal a pilot or a starship from hand");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));

            // Choose target(s)
            action.appendTargeting(
                    new ChooseCardFromHandEffect(action, playerId, filter) {
                        @Override
                        protected void cardSelected(SwccgGame game, final PhysicalCard selectedCard) {
                            final Filter searchFilter;
                            if (Filters.character.accepts(game, selectedCard)) {
                                action.setActionMsg("Take " + GameUtils.getCardLink(selectedCard) + "'s matching Azure Angel, Falcon, or Red 5 from Reserve Deck and deploy both simultaneously");

                                Filter matchingStarship = Filters.and(starshipFilter, Filters.matchingStarship(selectedCard));
                                searchFilter = Filters.and(matchingStarship);
                            }
                            else {
                                action.setActionMsg("Take " + GameUtils.getCardLink(selectedCard) + "'s matching pilot from Reserve Deck and deploy both simultaneously");
                                searchFilter = Filters.matchingPilot(selectedCard);
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