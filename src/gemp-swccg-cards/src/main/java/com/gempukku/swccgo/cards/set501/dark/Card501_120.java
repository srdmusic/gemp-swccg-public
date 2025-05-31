package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.CancelCardsOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: With Thunderous Applause
 */
public class Card501_120 extends AbstractEpicEventDeployable {
    public Card501_120() {
        super(Side.DARK, PlayCardZoneOption.ATTACHED, "With Thunderous Applause", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on Galactic Senate. [V17] Passel Argente's game text and your Political Effects are canceled. Twice per turn, may target your agenda here:\n" +
                    "Blockade: cancel a 'react';\n" +
                    "Taxation: place an [Episode I] character from hand on Used Pile; the next [Episode I] character you deploy this turn is deploy -1;\n" +
                    "Trade: during your turn, place a card from hand in Lost Pile, shuffle that pile, and take top card into hand;\n" +
                    "Wealth: subtract 1 from attrition against you.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.VIRTUAL_SET_25);
        setTestingText("With Thunderous Applause");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Galactic_Senate;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new CancelsGameTextModifier(self, Filters.and(Icon.VIRTUAL_SET_17, Filters.Passel_Argente)));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(SwccgGame game, Effect effect, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.and(Filters.your(self), Filters.Political_Effect))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        Filter yourPoliticalEffectsFilter = Filters.and(Filters.your(self), Filters.Political_Effect);

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canTargetToCancel(game, self, yourPoliticalEffectsFilter)) {

            Collection<PhysicalCard> cardsToCancel = Filters.filterActive(game, self, TargetingReason.TO_BE_CANCELED, yourPoliticalEffectsFilter);

            if (!cardsToCancel.isEmpty()) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Cancel your Political Effects");

                action.appendEffect(
                        new CancelCardsOnTableEffect(action, cardsToCancel));
                actions.add(action);
            }
        }
        return actions;
    }

}
