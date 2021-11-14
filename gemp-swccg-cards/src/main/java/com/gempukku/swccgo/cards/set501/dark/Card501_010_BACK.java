package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Objective
 * Title: Imperial Occupation (V) / Imperial Control (V)
 */
public class Card501_010_BACK extends AbstractObjective {
    public Card501_010_BACK() {
        super(Side.DARK, 7, Title.Imperial_Control);
        setVirtualSuffix(true);
        setGameText("While this side up, Rebel Leadership and We're Doomed are lost interrupts. Attrition against opponent is +1 for each Imperial leader in battle. Your Force drains are +1 at opponent's sites (or Echo sites) where your snowtrooper or non-unique AT-AT is present. Adds one [DS] icon and one [LS] icon at 'blown away' and 'collapsed' Hoth sites.  \n" +
                "Flip this card (unless 1st marker 'blown away') if you do not occupy Hoth system and two Hoth sites.");
        addIcons(Icon.SPECIAL_EDITION, Icon.HOTH, Icon.VIRTUAL_SET_17);
        setTestingText("Imperial Control (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        
        modifiers.add(new ImmuneToTitleModifier(self, Filters.title(Title.AT_AT_Cannon), Title.Sabotage));
        modifiers.add(new DestinyModifier(self, Filters.and(Filters.your(self), Filters.or(Filters.and(Filters.not(Icon.MAINTENANCE), Filters.AT_AT), Filters.snowtrooper, Filters.Star_Destroyer)), 1));
        modifiers.add(new MayNotHaveForfeitValueIncreasedModifier(self, Filters.and(Filters.character, Filters.Imperial, Filters.except(Filters.snowtrooper))));
        modifiers.add(new LostInterruptModifier(self, Filters.or(Filters.title("Rebel Leadership"), Filters.Were_Doomed)));
        modifiers.add(new AttritionModifier(self, new InBattleCondition(self, Filters.Imperial_leader), new OnTableEvaluator(self, Filters.and(Filters.participatingInBattle, Filters.Imperial_leader)), opponent));
        modifiers.add(new ForceDrainModifier(self, Filters.and(Filters.or(Filters.and(Filters.opponents(self), Filters.site), Filters.Echo_site), Filters.wherePresent(self, Filters.and(Filters.your(self), Filters.or(Filters.snowtrooper, Filters.and(Filters.non_unique, Filters.AT_AT))))), 1, playerId));
        modifiers.add(new IconModifier(self, Filters.and(Filters.site, Filters.or(Filters.blown_away, Filters.collapsed), Filters.Hoth_site), Icon.DARK_FORCE));
        modifiers.add(new IconModifier(self, Filters.and(Filters.site, Filters.or(Filters.blown_away, Filters.collapsed), Filters.Hoth_site), Icon.LIGHT_FORCE));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Sunsdown, Filters.Rebel_Base_Occupation))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && !GameConditions.canSpot(game, self, Filters.and(Filters.blown_away, Filters.title(Title.Main_Power_Generators, true)))
                && !(GameConditions.occupies(game, playerId, Filters.Hoth_system) && GameConditions.occupies(game, playerId, 2, Filters.Hoth_site))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }

        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, Filters.or(Filters.Sunsdown, Filters.Rebel_Base_Occupation))) {
            if (GameConditions.canTargetToCancel(game, self, Filters.Sunsdown)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                // Build action using common utility
                CancelCardActionBuilder.buildCancelCardAction(action, Filters.Sunsdown, Title.Sunsdown);
                actions.add(action);
            }
            if (GameConditions.canTargetToCancel(game, self, Filters.Rebel_Base_Occupation)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                // Build action using common utility
                CancelCardActionBuilder.buildCancelCardAction(action, Filters.Rebel_Base_Occupation, Title.Rebel_Base_Occupation);
                actions.add(action);
            }

        }

        return actions;
    }
}