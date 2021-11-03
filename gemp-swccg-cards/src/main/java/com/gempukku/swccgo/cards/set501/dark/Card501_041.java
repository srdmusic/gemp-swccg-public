package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Blast Door Controls (V)
 */
public class Card501_041 extends AbstractNormalEffect {
    public Card501_041() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Blast Door Controls", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Panels control blast doors and key security lock-downs during alerts. Luke destroyed one, locking Imperial forces out of Hangar Bay 327.");
        setGameText("Deploy on table. Cancels Blast The Door, Kid!. Rebel Barrier is a Lost Interrupt. If opponent just moved a character, starship, or vehicle away from (or just canceled) a battle, opponent must lose 1 Force.");
        addIcons(Icon.VIRTUAL_SET_17);
        setTestingText("Blast Door Controls (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new LostInterruptModifier(self, Filters.Rebel_Barrier));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Blast_The_Door_Kid))
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

        if (GameConditions.canTargetToCancel(game, self, Filters.Blast_The_Door_Kid)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Blast_The_Door_Kid, Title.Blast_The_Door_Kid);
            actions.add(action);
        }

        if (TriggerConditions.battleCanceledAt(game, effectResult, game.getOpponent(self.getOwner()), Filters.any)
             || (GameConditions.isDuringBattle(game)
                && TriggerConditions.moved(game, effectResult, game.getOpponent(self.getOwner()), Filters.or(Filters.character, Filters.starship, Filters.vehicle))
                && TriggerConditions.movedFromLocation(game, effectResult, Filters.or(Filters.character, Filters.starship, Filters.vehicle), game.getGameState().getBattleLocation()))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Opponent loses 1 Force");
            action.appendEffect(new LoseForceEffect(action, game.getOpponent(self.getOwner()), 1));

            actions.add(action);
        }

        return actions;
    }
}