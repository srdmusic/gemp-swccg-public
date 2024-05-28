package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Device
 * Title: Electro-Rangefinder (V)
 */
public class Card501_134 extends AbstractDevice {
    public Card501_134() {
        super(Side.DARK, 6, PlayCardZoneOption.ATTACHED, "Electro-Rangefinder", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Long-range stereoscopic sighting device connected to the cannons of an Imperial walker. Calibrated to allow the AT-AT commander to accurately fire at distant targets.");
        setGameText("Deploy on a [Hoth] or [Premium] AT-AT on Hoth. " +
                "Once per game, when deployed, may take any card from Force Pile into hand; reshuffle. " +
                "When this AT-AT fires an AT-AT Cannon with your [Hoth] Epic Event from a site you control, " +
                "may add one destiny to your total.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_23);
        setTestingText("Electro-Rangefinder (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.or(Icon.HOTH, Icon.PREMIUM), Filters.AT_AT, Filters.on(Title.Hoth));
    }

    @Override
    protected Filter getGameTextValidToUseDeviceFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.and(Filters.or(Icon.HOTH, Icon.PREMIUM), Filters.AT_AT, Filters.on(Title.Hoth));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Target_The_Main_Generator,
                new ControlsCondition(self.getOwner(), Filters.here(self)), ModifyGameTextType.TARGET_THE_MAIN_GENERATOR__ADDS_ONE_DESTINY));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ELECTRO_RANGEFINDER__UPLOAD_CARD_FROM_PILE;
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            
            if (GameConditions.canTakeCardsIntoHandFromForcePile(game, playerId, self, gameTextActionId)) {
                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Take card into hand from Force Pile");
                action.setActionMsg("Take a card into hand from Force Pile");
                action.appendUsage(
                    new OncePerGameEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new TakeCardIntoHandFromForcePileEffect(action, playerId, true));
                actions.add(action);
            }

            if (GameConditions.canTakeCardsIntoHandFromUsedPile(game, playerId, self, gameTextActionId)) {
                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Take card into hand from Force Pile");
                action.setActionMsg("Take a card into hand from Force Pile");
                action.appendUsage(
                    new OncePerGameEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new TakeCardIntoHandFromUsedPileEffect(action, playerId, true));
                actions.add(action);
            }
        }
        return actions;
    }
}
