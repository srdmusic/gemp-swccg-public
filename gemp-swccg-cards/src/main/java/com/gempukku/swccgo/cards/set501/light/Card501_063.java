package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardOfForcePileEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
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
        super(Side.LIGHT, 4, "A Jedi's Fury", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("It had been decades since Vader had felt the sting of an enemy's blade.");
        setGameText("USED: If a character of ability > 4 was just hit during battle, opponent loses 1 Force. OR Peek at top card of Force Pile. LOST: If Luke alone during a battle or duel add or subtract 1 to a just drawn weapon, battle, or duel destiny for each 'conflict' card (limit 3).");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        setTestingText("A Jedi's Fury");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        if (GameConditions.isDuringBattle(game)
                && TriggerConditions.justHit(game, effectResult, Filters.and(Filters.character, Filters.abilityMoreThan(4)))) {

            final boolean lukeHit = TriggerConditions.justHit(game, effectResult, Filters.Luke);

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


        Filter lukeAlone = Filters.and(Filters.Luke, Filters.alone);

        if (GameConditions.canSpot(game, self, Filters.I_Feel_The_Conflict)
                && (GameConditions.isDuringBattleWithParticipant(game, lukeAlone)
                || GameConditions.isDuringDuelWithParticipant(game, lukeAlone))
                && (TriggerConditions.isBattleDestinyJustDrawn(game, effectResult)
                || TriggerConditions.isDuelDestinyJustDrawn(game, effectResult)
                || TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult))) {

            final int numCardsStacked =
                    Math.min(3, Filters.filterStacked(game, Filters.stackedOn(self, Filters.I_Feel_The_Conflict)).size());


            final PlayInterruptAction addAction = new PlayInterruptAction(game, self, CardSubtype.LOST);

            addAction.setText("Add " + numCardsStacked + " to destiny");
            addAction.allowResponses(
                    new RespondablePlayCardEffect(addAction) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            addAction.appendEffect(
                                    new ModifyDestinyEffect(addAction, numCardsStacked)
                            );
                        }
                    }
            );
            actions.add(addAction);

            final PlayInterruptAction subtractAction = new PlayInterruptAction(game, self, CardSubtype.LOST);

            subtractAction.setText("Subtract " + numCardsStacked + " from destiny");
            subtractAction.allowResponses(
                    new RespondablePlayCardEffect(subtractAction) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            subtractAction.appendEffect(
                                    new ModifyDestinyEffect(subtractAction, -numCardsStacked)
                            );
                        }
                    }
            );
            actions.add(subtractAction);
        }

        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.hasForcePile(game, playerId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Peek at top card of Force Pile");
            // Allow response(s)
            action.allowResponses("Peek at top card of Force Pile",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new PeekAtTopCardOfForcePileEffect(action, playerId, playerId));
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }
}
