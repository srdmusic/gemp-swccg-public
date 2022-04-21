package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.InPlayDataEqualsCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Objective
 * Title: Zero Hour / Liberation of Lothal
 */
public class Card501_105_BACK extends AbstractObjective {
    public Card501_105_BACK() {
        super(Side.LIGHT, 7, "Liberation of Lothal");
        setGameText("While this side up, if you have Force drained this turn, your other Force drains are +1. Once per turn, may add or subtract X from a just drawn battle destiny (or opponent's weapon destiny), where X = number of battlegrounds occupied by Phoenix Squadron characters. If you just won a battle, may retrieve a Phoenix Squadron character. During battle at Lothal system, opponent may not limit the number of battle destinies you may draw. " +
                "Flip this card if opponent controls two Lothal locations.");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Liberation of Lothal");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, Filters.location, new InPlayDataEqualsCondition(self, true), 1, self.getOwner()));
        modifiers.add(new NumberOfBattleDestinyDrawsMayNotBeLimitedByOpponentModifier(self, Filters.Lothal_system, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        String opponent = game.getOpponent(playerId);
        //during battle add or subtract from a just drawn battle destiny (or opponent's weapon destiny)

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;
        if (GameConditions.isDuringBattle(game)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && (TriggerConditions.isBattleDestinyJustDrawn(game, effectResult)
                || TriggerConditions.isWeaponDestinyJustDrawnBy(game, effectResult, opponent))) {
            //X = the number of battlegrounds occupied by Chopper, Ezra, Hera, Kanan, Sabine, or Zeb
            int amount = Filters.countTopLocationsOnTable(game,
                    Filters.and(Filters.battleground,
                            Filters.or(Filters.occupiesWith(playerId, self, Filters.or(Filters.Chopper, Filters.Ezra, Filters.Hera, Filters.Kanan, Filters.Sabine, Filters.Zeb)),
                                    Filters.occupiesWith(opponent, self, Filters.or(Filters.Chopper, Filters.Ezra, Filters.Hera, Filters.Kanan, Filters.Sabine, Filters.Zeb)))));


            //add
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Add " + amount + " to destiny");
            action.appendUsage(new OncePerTurnEffect(action));
            action.appendEffect(new ModifyDestinyEffect(action, amount));
            actions.add(action);


            //subtract
            final OptionalGameTextTriggerAction action2 = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action2.setText("Subtract " + amount + " from destiny");
            action2.appendUsage(new OncePerTurnEffect(action2));
            action2.appendEffect(new ModifyDestinyEffect(action2, -amount));
            actions.add(action2);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        if (TriggerConditions.wonBattle(game, effectResult, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve a Phoenix Squadron character");
            action.appendEffect(
                    new RetrieveCardEffect(action, playerId, Filters.and(Keyword.PHOENIX_SQUADRON, Filters.character)));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        // Track if you have force drained this turn
        if (TriggerConditions.forceDrainCompleted(game, effectResult, playerId)) {
            self.setWhileInPlayData(new WhileInPlayData(true));
        }

        // Reset at the end of each turn
        if (TriggerConditions.isEndOfEachTurn(game, effectResult)) {
            self.setWhileInPlayData(null);
        }

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.controls(game, opponent, 2, Filters.Lothal_location)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);

        }
        return null;
    }
}