package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.WeaponBeingFiredByCondition;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.modifiers.SpecialFlagModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Weapon
 * Subtype: Vehicle
 * Title: Electro-Rangefinder (V)
 */
public class Card501_134 extends AbstractDevice {
    public Card501_134() {
        super(Side.DARK, 6, PlayCardZoneOption.ATTACHED, "Electro-Rangefinder", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Long-range stereoscopic sighting device connected to the cannons of an Imperial walker. Calibrated to allow the AT-AT commander to accurately fire at distant targets.");
        setGameText("Deploy on a non-[M] AT-AT. " +
                "Once per turn, if this AT-AT just fired a weapon, may take that destiny draw into hand. " +
                "When this AT-AT firing an AT-AT Cannon with your [H] Epic Event, may draw two destinies and choose one.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_23);
        setTestingText("Electro-Rangefinder (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.not(Icon.MAINTENANCE), Filters.AT_AT);
    }

    @Override
    protected Filter getGameTextValidToUseDeviceFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.and(Filters.not(Icon.MAINTENANCE), Filters.AT_AT);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        Filter hasAttached = Filters.hasAttached(self);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new SpecialFlagModifier(self, new WeaponBeingFiredByCondition(Filters.AT_AT_Cannon, hasAttached),
                ModifierFlag.DRAW_TWO_AND_CHOOSE_ONE_FOR_EPIC_EVENT_AND_WEAPON_DESTINY, playerId));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isWeaponDestinyJustDrawnBy(game, effectResult, playerId, Filters.hasAttached(self))
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTakeDestinyCardIntoHand(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take destiny card into hand");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Pay cost(s)
            action.appendEffect(
                    new TakeDestinyCardIntoHandEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }
}
