package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PayRelocateBetweenLocationsCostEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.CancelGameTextUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost
 * Title: A Jedi's Fury
 */
public class Card501_063 extends AbstractUsedOrLostInterrupt {
    public Card501_063() {
        super(Side.LIGHT, 5, "A Jedi's Fury", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("It had been decades since Vader had felt the sting of an enemy's blade.");
        setGameText("USED: If a character of ability > 4 was just ‘hit’ during battle, opponent loses 1 Force. LOST: If His Destiny on table, choose: For remainder of turn, cancel the game text of a Dark Jedi with Luke. OR During your move phase, relocate Luke from a site to Turbolift Walkway.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        setTestingText("A Jedi's Fury");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        if (GameConditions.isDuringBattle(game)
                && TriggerConditions.justHit(game, effectResult, Filters.and(Filters.character, Filters.abilityMoreThan(4)))) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Opponent loses 1 Force");
            action.allowResponses(new RespondablePlayCardEffect(action) {
                @Override
                protected void performActionResults(Action targetingAction) {
                    action.appendEffect(
                            new LoseForceEffect(action, opponent, 1));
                }
            });

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();


        Filter filter = Filters.and(Filters.opponents(self), Filters.Dark_Jedi, Filters.with(self, Filters.Luke));

        if (GameConditions.canTarget(game, self, Filters.title("His Destiny"))
                && GameConditions.canTarget(game, self, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Target opponent's character");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Dark Jedi", filter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Cancel " + GameUtils.getCardLink(targetedCard) + "'s game text",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new CancelGameTextUntilEndOfTurnEffect(action, finalTarget));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }


        final Filter locationFilter = Filters.title("Death Star II: Turbolift Walkway");
        Filter lukeFilter = Filters.and(Filters.Luke, Filters.canBeRelocatedToLocation(locationFilter, 0));

        // Check condition(s)
        if (GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && GameConditions.canTarget(game, self, Filters.title("His Destiny"))
                && GameConditions.canTarget(game, self, locationFilter)
                && GameConditions.canTarget(game, self, lukeFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Relocate Luke");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose character", lukeFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId1, final PhysicalCard characterToRelocate) {
                            action.appendTargeting(
                                    new TargetCardOnTableEffect(action, playerId, "Choose site to relocate " + GameUtils.getCardLink(characterToRelocate) + " to", locationFilter) {
                                        @Override
                                        protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedSite) {
                                            action.addAnimationGroup(characterToRelocate);
                                            action.addAnimationGroup(targetedSite);
                                            // Pay cost(s)
                                            action.appendCost(
                                                    new PayRelocateBetweenLocationsCostEffect(action, playerId, characterToRelocate, targetedSite, 0));
                                            // Allow response(s)
                                            action.allowResponses("Relocate " + GameUtils.getCardLink(characterToRelocate) + " to " + GameUtils.getCardLink(targetedSite),
                                                    new RespondablePlayCardEffect(action) {
                                                        @Override
                                                        protected void performActionResults(Action targetingAction) {
                                                            // Get the targeted card(s) from the action using the targetGroupId.
                                                            // This needs to be done in case the target(s) were changed during the responses.
                                                            PhysicalCard finalCharacter = action.getPrimaryTargetCard(targetGroupId1);
                                                            PhysicalCard finalSite = action.getPrimaryTargetCard(targetGroupId2);

                                                            // Perform result(s)
                                                            action.appendEffect(
                                                                    new RelocateBetweenLocationsEffect(action, finalCharacter, finalSite));
                                                        }
                                                    }
                                            );
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }
}
