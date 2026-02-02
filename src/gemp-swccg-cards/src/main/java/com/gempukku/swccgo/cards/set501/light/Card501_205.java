package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.DrawsNoMoreThanBattleDestinyEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
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
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.CancelGameTextUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotFireWeaponsModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Used
 * Title: Either Way, You Win (V)
 */
public class Card501_205 extends AbstractUsedInterrupt {
    public Card501_205() {
        super(Side.LIGHT, 4, "Either Way, You Win", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("'Deal!'");
        //setGameText("If [Tatooine] or [Coruscant] Qui-Gon in battle, he is power +1 for each 'credit.' OR Once per game, if a battle just initiated at Watto's Junkyard involving Qui-Gon, target a character. Lightsabers may not be fired this battle. Unless target is Watto or a Dark Jedi, cancel target's game text.");
        setGameText("If [Tatooine] Anakin in battle, opponent may not draw more than one battle destiny. OR If a battle just initiated at a site, target an opponent's character with your [Tatooine] or [Coruscant] Qui-Gon. Target's game text canceled and neither character may fire weapons.");
        addIcons(Icon.TATOOINE, Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        setTestingText("Either Way, You Win (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Icon.TATOOINE, Filters.Anakin))) {

            GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
            final String opponent = game.getOpponent(playerId);
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);

            action.setText("Limit opponent to one battle destiny");
            // Allow response(s)
            action.allowResponses("Prevent opponent from drawing more than one battle destiny",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DrawsNoMoreThanBattleDestinyEffect(action, opponent, 1));
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.EITHER_WAY_YOU_WIN_V__TARGET_CHARACTER;

        Filter quigonFilter = Filters.and(Filters.or(Icon.TATOOINE, Icon.CORUSCANT), Filters.QuiGon, Filters.participatingInBattle);
        Filter targetFilter = Filters.and(Filters.opponents(playerId), Filters.character, Filters.with(self, quigonFilter), Filters.participatingInBattle);
        // Check condition(s)
        if (TriggerConditions.battleInitiatedAt(game, effectResult, Filters.site)
                && GameConditions.canTarget(game, self, targetFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Target character with Qui-Gon");

            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target character with Qui-Gon", targetFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard character) {
                            action.addAnimationGroup(character);
                            // Allow response(s)

                            action.allowResponses(
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new CancelGameTextUntilEndOfBattleEffect(action, finalTarget));
                                            action.appendEffect(
                                                    new AddUntilEndOfBattleModifierEffect(action, new MayNotFireWeaponsModifier(self, Filters.or(quigonFilter, finalTarget)), "Prevents Qui-Gon and " + GameUtils.getCardLink(finalTarget) + " from firing weapons"));
                                        }
                                    }
                            );
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }
}