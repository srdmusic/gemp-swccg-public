package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayBeFiredTwicePerBattleModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.FiredWeaponResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: More! More!
 */

public class Card501_130 extends AbstractUsedInterrupt {
    public Card501_130() {
        super(Side.DARK, 5, "More! More!", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("If Kylo in battle, add 1 to a just drawn destiny. OR Once per game, if you just fired an [E7] weapon (except a lightsaber) in battle, it may fire again this battle. OR fire an [E7] blaster or cannon into a battle at an adjacent site.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_23);
        setTestingText("More! More!");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isBattleDestinyJustDrawn(game, effectResult)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Kylo)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add 1 to destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyDestinyEffect(action, 1));
                        }
                    }
            );
            actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && TriggerConditions.weaponJustFired(game, effectResult, 
                    Filters.and(Filters.weapon, Filters.icon(Icon.VIRTUAL_SET_17), Filters.not(Filters.lightsaber)))) {

            GameTextActionId gameTextActionId = GameTextActionId.MORE_MORE__FIRE_WEAPON_AGAIN;
            FiredWeaponResult weaponFiredResult = (FiredWeaponResult) effectResult;
            final PhysicalCard weaponCard = (weaponFiredResult.getPermanentWeaponFired() != null) ? weaponFiredResult.getPermanentWeaponFired().getPhysicalCard(game) : weaponFiredResult.getWeaponCardFired();

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Fire Weapon Again");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddUntilEndOfBattleModifierEffect(action, new MayBeFiredTwicePerBattleModifier(self, weaponCard), null));
                        }
                    }
            );
            actions.add(action);
        }
        return null;
    }

}
