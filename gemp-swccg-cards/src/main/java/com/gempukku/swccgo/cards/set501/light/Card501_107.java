package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseCardsFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotReactFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Infiltration!
 */
public class Card501_107 extends AbstractNormalEffect {
    public Card501_107() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, "Infiltration!", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on a site. At same site, cancels Imperial Barrier and None Shall Pass. Players may not 'react' to or from here. Undercover spies here are lost. Non-[Maintenance] Lando is power +1 here. If Leia here, may place Effect in hand or Used Pile.");
        addIcons(Icon.VIRTUAL_SET_17);
        setTestingText("Infiltration!");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.site;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.atSameSite(self), Filters.not(Icon.MAINTENANCE), Filters.Lando), 1));
        modifiers.add(new MayNotReactFromLocationModifier(self, Filters.here(self), self.getOwner()));
        modifiers.add(new MayNotReactFromLocationModifier(self, Filters.here(self), game.getOpponent(self.getOwner())));
        modifiers.add(new MayNotReactToLocationModifier(self, Filters.here(self), self.getOwner()));
        modifiers.add(new MayNotReactToLocationModifier(self, Filters.here(self), game.getOpponent(self.getOwner())));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isHere(game, self, Filters.Leia)) {
            //If Leia here, may place Effect in hand ...
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Return Effect to hand");
            // Perform result(s)
            action.appendEffect(
                    new ReturnCardToHandFromTableEffect(action, self));
            actions.add(action);

            //If Leia here, may place Effect in ... Used Pile
            final TopLevelGameTextAction action2 = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action2.setText("Place Effect in Used Pile");
            // Perform result(s)
            action2.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            actions.add(action2);

        }

        return actions;
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.or(Filters.Imperial_Barrier, Filters.None_Shall_Pass), Filters.atSameSite(self))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, SpotOverride.INCLUDE_UNDERCOVER, Filters.and(Filters.undercover_spy, Filters.here(self)))) {

            Collection<PhysicalCard> toBeLost = Filters.filterAllOnTable(game, Filters.and(Filters.undercover_spy, Filters.here(self)));

            if (!toBeLost.isEmpty()) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Undercover spies here are lost");
                // Perform result(s)
                action.appendEffect(
                        new LoseCardsFromTableEffect(action, toBeLost));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}