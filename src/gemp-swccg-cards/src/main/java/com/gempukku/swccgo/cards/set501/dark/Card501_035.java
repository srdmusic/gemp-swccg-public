package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.evaluators.MaxLimitEvaluator;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInForcePileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.ShufflePileEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyWhenDrawnForBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.DestinyWhenDrawnForWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Imperial Assault
 */
public class Card501_035 extends AbstractUsedInterrupt {
    public Card501_035() {
        super(Side.DARK, 3, "Imperial Assault", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Destiny +1 for each of your battlegrounds on table when drawn for weapon or battle destiny (limit +4). If you have deployed four battlegrounds this game: place this card in your force pile and shuffle. OR During battle, your total power is +2.");
        addIcons(Icon.VIRTUAL_SET_23);
        setTestingText("Imperial Assault");
    }    
    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();;
        String playerId = self.getOwner();

        modifiers.add(new DestinyWhenDrawnForWeaponDestinyModifier(self, self, new MinLimitEvaluator(new OnTableEvaluator(self, Filters.and(Filters.your(playerId), Filters.battleground)), 4)));
        modifiers.add(new DestinyWhenDrawnForBattleDestinyModifier(self, self, new MaxLimitEvaluator(new OnTableEvaluator(self, Filters.and(Filters.your(playerId), Filters.battleground)), 4)));

        return modifiers;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
       int yourBattlegroundCount = Filters.countTopLocationsOnTable(game, Filters.and(Filters.your(self), Filters.battleground));

       List<PlayInterruptAction> actions = new LinkedList<>();

       // Check condition(s)
        if (yourBattlegroundCount >= 4) 
        {

            final PlayInterruptAction forcePileAction = new PlayInterruptAction(game, self);
            forcePileAction.setText("If you have deployed four battlegrounds this game: place this card in your force pile and shuffle.");
            // Allow response(s)
            forcePileAction.allowResponses(
                    new RespondablePlayCardEffect(forcePileAction) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            forcePileAction.appendEffect(
                                    new PutCardFromVoidInForcePileEffect(forcePileAction, playerId, self) {
                                        @Override
                                        protected final void afterCardPutInCardPile() {
                                            forcePileAction.appendEffect(
                                                new ShufflePileEffect(forcePileAction, playerId, Zone.FORCE_PILE));

                                        }
                                    }
                            );


                        }
                    }
            );
            actions.add(forcePileAction);

            final PlayInterruptAction powerAction = new PlayInterruptAction(game, self);
            powerAction.setText("Add 2 to total power");

            powerAction.allowResponses("Add 2 to total power during battles for remainder of turn",
                    new RespondablePlayCardEffect(powerAction) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            powerAction.appendEffect(new AddUntilEndOfTurnModifierEffect(powerAction,
                                    new TotalPowerModifier(self, Filters.battleLocation, 2, playerId),
                                    "Adds 2 to total power during battles"));
                        }
                    }
            );
            actions.add(powerAction);


        }
        return actions;
    }

}
