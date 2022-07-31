package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlienRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.DuringBattleAtCondition;
import com.gempukku.swccgo.cards.conditions.InBattleWithCondition;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 13
 * Type: Character
 * Subtype: Alien/Rebel
 * Title: Fenn Rau
 */
public class Card501_066 extends AbstractAlienRebel {
    public Card501_066() {
        super(Side.LIGHT, 2, 4, 4, 3, 5, "Fenn Rau", Uniqueness.UNIQUE);
        setArmor(5);
        setLore("Mandalorian scout.");
        setGameText("Adds 3 to power of anything he pilots. While with another Mandalorian in battle, opponent's total power is -3 here. During any draw phase, may take Fenn Rau into hand to activate 2 Force; may also retrieve 1 Force if Fenn Rau won a battle this turn.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.SCOUT);
        setSpecies(Species.MANDALORIAN);
        setTestingText("Fenn Rau");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new TotalPowerModifier(self, Filters.here(self), new InBattleWithCondition(self, Filters.Mandalorian), -3, game.getOpponent(self.getOwner())));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (GameConditions.isDuringEitherPlayersPhase(game, Phase.DRAW)
                && GameConditions.canActivateForce(game, playerId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Activate 2 Force");
            // Choose target(s)`1`
            action.appendCost(
                    new ReturnCardToHandFromTableEffect(action, self));
            action.appendEffect(
                    new ActivateForceEffect(action, playerId, 2));
            if (GameConditions.cardHasWhileInPlayDataEquals(self, true)) {
                action.appendEffect(new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Retrieve 1 Force?") {
                    @Override
                    protected void yes() {
                        action.appendEffect(
                                new RetrieveForceEffect(action, playerId, 1));
                    }

                    @Override
                    protected void no() {
                        action.appendEffect(
                                new SendMessageEffect(action, playerId+" chooses not to retrieve 1 Force"));
                    }
                }));
            }
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Track if he won a battle this turn
        if (TriggerConditions.wonBattle(game, effectResult, self)) {
            self.setWhileInPlayData(new WhileInPlayData(true));
        }

        // Reset at the end of each turn
        if (TriggerConditions.isEndOfEachTurn(game, effectResult)) {
            self.setWhileInPlayData(null);
        }
        return actions;
    }
}