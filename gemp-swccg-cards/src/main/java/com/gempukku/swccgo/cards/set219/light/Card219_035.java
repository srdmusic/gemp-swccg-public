package com.gempukku.swccgo.cards.set219.light;

import com.gempukku.swccgo.cards.AbstractAlienRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.InBattleWithCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnActionProxyEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
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
public class Card219_035 extends AbstractAlienRebel {
    public Card219_035() {
        super(Side.LIGHT, 2, 4, 4, 3, 5, "Fenn Rau", Uniqueness.UNIQUE);
        setArmor(5);
        setLore("Mandalorian scout.");
        setGameText("[Pilot] 3. During battle, if another Mandalorian here, opponent’s total power is -3. At the end of a battle here, " +
                "may return Fenn Rau to hand to activate 2 Force (if Fenn Rau won a battle this turn, may also retrieve 1 Force).");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.SCOUT);
        setSpecies(Species.MANDALORIAN);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new TotalPowerModifier(self, Filters.here(self), new InBattleWithCondition(self, Filters.Mandalorian), -3, game.getOpponent(self.getOwner())));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.battleEndingAt(game, effectResult, Filters.here(self))
                && GameConditions.canActivateForce(game, playerId)) {
            final PhysicalCard battleLocation = game.getGameState().getBattleLocation();
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Activate 2 Force");
            action.appendCost(
                    new ReturnCardToHandFromTableEffect(action, self));
            action.appendEffect(
                    new ActivateForceEffect(action, playerId, 2));
            action.appendEffect(
                    new AddUntilEndOfTurnActionProxyEffect(action, new AbstractActionProxy() {
                        @Override
                        public List<TriggerAction> getOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult) {
                            List<TriggerAction> actions = new LinkedList<>();
                            // Check condition(s)

                            if (GameConditions.isDuringBattleWonBy(game, playerId)
                                && TriggerConditions.battleEndingAt(game, effectResult, battleLocation)) {
                                final OptionalGameTextTriggerAction action2 = new OptionalGameTextTriggerAction(self, self.getCardId());
                                action2.appendEffect(
                                        new RetrieveForceEffect(action, playerId, 1));
                                actions.add(action2);
                            }
                            return actions;
                        }
                    })
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}