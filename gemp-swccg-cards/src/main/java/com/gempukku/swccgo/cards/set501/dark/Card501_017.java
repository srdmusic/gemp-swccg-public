package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.CancelGameTextUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Rolling, Rolling, Rolling & There They Are!
 */
public class Card501_017 extends AbstractUsedOrLostInterrupt {
    public Card501_017() {
        super(Side.DARK, 5, "Rolling, Rolling, Rolling & There They Are!", Uniqueness.UNIQUE);
        addComboCardTitles(Title.There_They_Are, Title.Rolling_Rolling_Rolling);
        setLore("");
        setGameText("USED: Take a non-unique battle droid or non-unique destroyer droid into hand from Reserve Deck; reshuffle. " +
                "OR During a battle you initiated, your non-unique battle droids and non-unique destroyer droids present are each power +1." +
                "LOST: Target a Jedi at a site where you have two battle droids and/or destroyer droids. Target's game text is canceled for remainder of turn.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("[Set 18] Rolling, Rolling, Rolling & There They Are!");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        final Filter filter = Filters.and(Filters.your(self), Filters.non_unique, Filters.or(Filters.battle_droid, Filters.destroyer_droid), Filters.participatingInBattle);

        // Check condition(s)
        if (TriggerConditions.battleInitiated(game, effectResult)
                && GameConditions.isDuringBattleWithParticipant(game, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Add power to battle droids and destroyer droids");
            // Allow response(s)
            action.allowResponses("Make non-unique battle droids and destroyer droids power +1",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            final Collection<PhysicalCard> battleDroidsAndDestroyerDroids = Filters.filterActive(game, self, filter);
                            if (!battleDroidsAndDestroyerDroids.isEmpty()) {

                                // Perform result(s)
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action,
                                                new PowerModifier(self, Filters.in(battleDroidsAndDestroyerDroids), 1),
                                                "Makes " + GameUtils.getAppendedNames(battleDroidsAndDestroyerDroids) + " power +1"));
                            }
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        final Filter nonUniqueBattleDroidAndNonUniqueDestroyerDroidFilter = Filters.and(Filters.non_unique, Filters.or(Filters.battle_droid, Filters.destroyer_droid));

        GameTextActionId gameTextActionId = GameTextActionId.THERE_THEY_ARE__UPLOAD_NON_UNIQUE_BATTLE_DROID_OR_DESTROYER_DROID;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take card into hand from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Take a non-unique battle droid or non-unique destroyer droid into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, nonUniqueBattleDroidAndNonUniqueDestroyerDroidFilter, true));
                        }
                    }
            );
            actions.add(action);
        }

        Filter jediFilter = Filters.and(Filters.Jedi, Filters.at(Filters.site), Filters.presentWith(self, 2, nonUniqueBattleDroidAndNonUniqueDestroyerDroidFilter));

        // Check condition(s)
        if (GameConditions.canTarget(game, self, jediFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Cancel Jedi's game text");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Jedi", jediFilter) {
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
                                                    new CancelGameTextUntilEndOfTurnEffect(action, finalTarget)
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