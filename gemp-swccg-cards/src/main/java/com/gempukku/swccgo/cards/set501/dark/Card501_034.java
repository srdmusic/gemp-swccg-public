package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Subtype: Effect
 * Title: Send A Detachment Down (V)
 */
public class Card501_034 extends AbstractNormalEffect {
    public Card501_034() {
        super(Side.DARK, 4, PlayCardZoneOption.ATTACHED, "Send A Detachment Down", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Vader sent Imperial stormtroopers to the surface of Tatooine in search of the stolen Death Star plans. 'There'll be no one to stop us this time.'");
        setGameText("Deploy on Tatooine system. Tatooine Occupation is canceled. While you occupy, at related sites where you have two troopers (or a sandtrooper) your Force drains are +1. [Immune to Alter.]");
        addImmuneToCardTitle(Title.Alter);
        addIcons(Icon.VIRTUAL_SET_20);
        setTestingText("Send A Detachment Down (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Tatooine_system;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();

        Filter siteFilter = Filters.and(Filters.relatedSite(self), Filters.sameSiteAs(self, Filters.or(Filters.and(Filters.your(self), Filters.sandtrooper),
                        Filters.and(Filters.your(self), Filters.trooper, Filters.with(self, Filters.and(Filters.your(self), Filters.trooper))))));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, siteFilter, new OccupiesCondition(playerId, Filters.hasAttached(self)), 1, playerId));
        return modifiers;
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Tatooine_Occupation)
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
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canTargetToCancel(game, self, Filters.Tatooine_Occupation)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Tatooine_Occupation, Title.Tatooine_Occupation);
            actions.add(action);
        }
        return actions;
    }
}