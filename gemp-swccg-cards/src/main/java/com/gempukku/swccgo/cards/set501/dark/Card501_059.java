package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.conditions.ArmedWithCondition;
import com.gempukku.swccgo.cards.effects.PreventEffectOnCardEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ModifyForfeitEffect;
import com.gempukku.swccgo.logic.effects.ModifyPowerEffect;
import com.gempukku.swccgo.logic.modifiers.AddsBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToRemoveJustLostCardFromLostPileResult;
import com.gempukku.swccgo.logic.timing.results.HitResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Alien
 * Title: Dannik Jerriko (V)
 */
public class Card501_059 extends AbstractAlien {
    public Card501_059() {
        super(Side.DARK, 2, 3, 3, 3, 5, "Dannik Jerriko", Uniqueness.UNIQUE);
        setLore("Anzati assassin. Cheek-folds hide proboscises which allow him to 'eat the soup' (consume the life Force) of his victims. Smokes t'bac. Currently working for Jabba.");
        setGameText("Characters lost from here, may not be removed from lost pile. (Except to be placed out of play.) While armed with a blaster, adds one battle destiny. Anything he hits is power and forfeit -2.");
        addIcons(Icon.A_NEW_HOPE, Icon.WARRIOR);
        setSpecies(Species.ANZATI);
        addKeyword(Keyword.ASSASSIN);
        setTestingText("Dannik Jerriko (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsBattleDestinyModifier(self, new ArmedWithCondition(self, Filters.blaster), 1));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Prevent character removal from Lost Pile
        Filter characterFilter = Filters.and(Filters.character, Filters.here(self));
        if (TriggerConditions.isAboutToRemoveJustLostCardFromLostPile(game, effectResult, characterFilter) &&
                (!TriggerConditions.isAboutToBePlacedOutOfPlayFromTable(game, effectResult, characterFilter))) {
            final AboutToRemoveJustLostCardFromLostPileResult result = (AboutToRemoveJustLostCardFromLostPileResult) effectResult;
            final PhysicalCard cardToRemoveFromLostPile = result.getCardToRemoveFromLostPile();

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText(GameUtils.getFullName(cardToRemoveFromLostPile) + " may not be removed from Lost Pile.");
            action.appendEffect( new PreventEffectOnCardEffect(action, result.getPreventableCardEffect(), cardToRemoveFromLostPile, GameUtils.getCardLink(cardToRemoveFromLostPile) + " prevented from being removed from Lost Pile."));
            actions.add(action);
        }

        // Cards 'hit' by Dannik are power/forfeit -2
        if (TriggerConditions.justHitBy(game, effectResult, Filters.any, self)) {
            PhysicalCard cardHit = ((HitResult) effectResult).getCardHit();

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make " + GameUtils.getFullName(cardHit) + " power and forfeit - 2.");
            action.setActionMsg("Make " + GameUtils.getCardLink(cardHit) + " power and forfeit - 2.");
            // Perform result(s)
            action.appendEffect(
                    new ModifyPowerEffect(action, cardHit, -2));
            action.appendEffect(
                    new ModifyForfeitEffect(action, cardHit, -2));
            actions.add(action);
        }
        return actions;
    }
}
