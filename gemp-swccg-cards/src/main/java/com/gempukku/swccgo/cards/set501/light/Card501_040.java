package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 0
 * Type: Effect
 * Title: For Luck (V)
 */
public class Card501_040 extends AbstractNormalEffect {
    public Card501_040() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "For Luck", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Before swinging across the treacherous abyss, Leia gave Luke a kiss for luck. Despite an incessant storm of laserblasts, they made it.");
        setGameText("Deploy on table. Imperial Barrier is a Lost Interrupt. Stunning Leader is canceled. Once during your control phase, if opponent occupies a non-battleground (or Jabba's Prize on table), may peek at top two cards of Reserve Deck; take one into hand. [Immune to Alter.]");
        addIcons(Icon.PREMIUM, Icon.VIRTUAL_SET_0);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("For Luck (V) (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new LostInterruptModifier(self, Filters.Imperial_Barrier));
        return modifiers;
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Stunning_Leader))
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

        if (GameConditions.canTargetToCancel(game, self, Filters.Stunning_Leader)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Stunning_Leader, Title.Stunning_Leader);
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, Phase.CONTROL)
                && GameConditions.hasReserveDeck(game, playerId)
                && (GameConditions.canSpotLocation(game, Filters.and(Filters.non_battleground_location, Filters.occupies(opponent)))
                        || GameConditions.canSpot(game, self, SpotOverride.INCLUDE_CAPTIVE, Filters.Jabbas_Prize))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Peek at top two cards of Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, 2, 1, 1));
            return Collections.singletonList(action);
        }
        return null;
    }
}