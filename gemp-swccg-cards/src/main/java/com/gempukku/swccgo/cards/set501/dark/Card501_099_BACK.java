package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.MoveAsReactEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Objective
 * Title: Imperial Occupation (V) / Imperial Control (V)
 */
public class Card501_099_BACK extends AbstractObjective {
    public Card501_099_BACK() {
        super(Side.DARK, 7, Title.Imperial_Control);
        setVirtualSuffix(true);
        setGameText("While this side up, once per game, may deploy any Effect (or starship, at deploy -4) from your Reserve Deck; reshuffle. Once during your control phase, may place a card from hand on top of your Reserve Deck to take one Interrupt into hand from Reserve Deck; reshuffle. Once per turn, may move your character as a 'react' to a battle or Force drain at a site related to the renegade planet. " +
                "Flip this card if opponent controls more sites on the renegade planet than you.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_19);
        setTestingText("Imperial Control (V)");
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        String planet = game.getGameState().getRenegadePlanet();
        if (planet != null) {
            return "Renegade planet is " + planet;
        }
        return null;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.IMPERIAL_CONTROL_V__DOWNLOAD_EFFECT_OR_STARSHIP;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy an Effect or starship");
            action.setActionMsg("Deploy an Effect (or a starship for -4 Force) from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Effect, Filters.starship), -4, Filters.starship,true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.IMPERIAL_CONTROL_V__UPLOAD_INTERRUPT;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.hasHand(game, playerId)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take an Interrupt into hand from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Pay cost(s)
            action.appendCost(
                    new PutCardFromHandOnReserveDeckEffect(action, playerId));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.Interrupt, true));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if ((TriggerConditions.battleInitiatedAt(game, effectResult, opponent, Filters.and(Filters.Renegade_planet_location, Filters.site))
                || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent, Filters.and(Filters.Renegade_planet_location, Filters.site)))
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            Filter characterFilter = Filters.and(Filters.your(self), Filters.character, Filters.canMoveAsReactAsActionFromOtherCard(self, false, 0, false));
            if (GameConditions.canTarget(game, self, characterFilter)) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Move character as 'react'");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose character", characterFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard character) {
                                action.addAnimationGroup(character);
                                // Allow response(s)
                                action.allowResponses("Move " + GameUtils.getCardLink(character) + " as a 'react'",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new MoveAsReactEffect(action, character, false));
                                            }
                                        }
                                );
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)) {

            String opponent = game.getOpponent(playerId);
            int playerSitesControlled = Filters.countTopLocationsOnTable(game, Filters.and(Filters.site, Filters.Renegade_planet_location, Filters.controls(playerId, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE)));
            int opponentSitesControlled = Filters.countTopLocationsOnTable(game, Filters.and(Filters.site, Filters.Renegade_planet_location, Filters.controls(opponent, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE)));

            if (playerSitesControlled < opponentSitesControlled) {

                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Flip");
                action.setActionMsg(null);
                // Perform result(s)
                action.appendEffect(
                        new FlipCardEffect(action, self));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}