package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.MoveCardAsRegularMoveEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromUsedPileEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Character
 * Subtype: Imperial
 * Title: Deputy Director Harus Ison
 */
public class Card501_001 extends AbstractImperial {
    public Card501_001() {
        super(Side.DARK, 2, 3, 3, 3, 4, "Deputy Director Harus Ison", Uniqueness.UNIQUE);
        setLore("ISB leader.");
        setGameText("When deployed, your unique (•) Imperial of lesser ability here may make a regular move. During your deploy phase, may place your unique (•) ISB agent here on Used Pile; search that pile for an ISB agent (except Tarkin) with a different title and deploy it here for free; reshuffle.");
        addKeywords(Keyword.LEADER);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_17);
        setTestingText("Deputy Director Harus Ison");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        Filter filter = Filters.and(Filters.your(self), Filters.unique, Filters.Imperial, Filters.abilityLessThan(game.getModifiersQuerying().getAbility(game.getGameState(), self)), Filters.here(self), Filters.movableAsRegularMove(playerId, false, 0, false, Filters.any));

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
            && GameConditions.canSpot(game, self, filter)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make a regular move");
            action.setActionMsg("Make a regular move with your unique Imperial of lesser ability here");
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose a unique Imperial of lesser ability here to move as a regular move", filter) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard toMove = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(new MoveCardAsRegularMoveEffect(action, playerId, toMove, false, false, Filters.any));
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        // During your deploy phase, may place your unique (•) ISB agent here on Used Pile;
        // search that pile for a non-leader ISB agent with a different title
        // and deploy it here for free; reshuffle.

        GameTextActionId gameTextActionId = GameTextActionId.DEPUTY_DIRECTOR_HARUS_ISON__DEPLOY_ISB_AGENT_FROM_USED_PILE;

        Filter isbAgentToPlaceInUsedPile = Filters.and(Filters.your(self), Filters.unique, Filters.ISB_agent, Filters.here(self));
        if (GameConditions.canSpot(game, self, isbAgentToPlaceInUsedPile)
            && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
            && GameConditions.canSearchUsedPile(game, playerId, self, gameTextActionId)) {

            final PhysicalCard here = Filters.findFirstFromTopLocationsOnTable(game, Filters.here(self));

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Place ISB agent on Used Pile");
            action.setActionMsg("Place your unique ISB agent here on Used Pile to search Used Pile for an ISB agent (except Tarkin) with a different title and deploy it here for free");

            action.appendUsage(
                    new OncePerPhaseEffect(action));

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose a unique ISB agent to place on Used Pile", isbAgentToPlaceInUsedPile) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard placeOnUsed = action.getPrimaryTargetCard(targetGroupId);
                            action.appendCost(
                                    new PlaceCardInUsedPileFromTableEffect(action, placeOnUsed));
                            action.appendEffect(
                                    new DeployCardToTargetFromUsedPileEffect(action, Filters.and(Filters.not(Filters.Tarkin), Filters.ISB_agent, Filters.not(Filters.sameTitle(placeOnUsed))), Filters.here(here), true, true));
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }

        return null;
    }
}