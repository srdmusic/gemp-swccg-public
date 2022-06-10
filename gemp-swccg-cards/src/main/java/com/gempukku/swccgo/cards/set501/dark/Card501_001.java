package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Character
 * Subtype: Imperial
 * Title: Darth Vader, Dark Lord Of The Sith (V)
 */
public class Card501_001 extends AbstractImperial {
    public Card501_001() {
        super(Side.DARK, 1, 6, 6, 6, 8, Title.Darth_Vader_Dark_Lord_of_the_Sith, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Formerly Anakin Skywalker, Jedi Knight. Became Darth Vader. Ordered by Emperor Palpatine to deal with Luke Skywalker, but bargained for his son's life instead.");
        setGameText("Adds 3 to power of anything he pilots. If a battle just initiated here, may take Lightsaber Parry or Vader's Anger into hand from Reserve Deck; reshuffle. Opponent's non-Jedi characters here are power and forfeit -1. Immune to attrition < 5 (< 6 if alone, < 7 if with a Jedi Master).");
        addPersona(Persona.VADER);
        addIcons(Icon.SPECIAL_EDITION, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_19);
        setTestingText("Darth Vader, Dark Lord Of The Sith (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.opponents(self), Filters.non_Jedi_character, Filters.here(self)), -1));
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.opponents(self), Filters.non_Jedi_character, Filters.here(self)), -1));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new NotCondition(new WithCondition(self, Filters.Jedi_Master)), new ConditionEvaluator(5, 6, new AloneCondition(self))));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new WithCondition(self, Filters.Jedi_Master), 7));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.DARTH_VADER_DARK_LORD_OF_THE_SITH__UPLOAD_CARD;

        // Check condition(s)
        if (TriggerConditions.battleInitiatedAt(game, effectResult, opponent, Filters.here(self))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take card into hand from Reserve Deck");
            action.setActionMsg("Take Lightsaber Parry or Vader's Anger into hand from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.title("Lightsaber Parry"), Filters.title("Vader's Anger")), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
