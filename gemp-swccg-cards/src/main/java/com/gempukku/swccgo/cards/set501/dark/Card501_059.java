package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.conditions.ArmedWithCondition;
import com.gempukku.swccgo.common.*;
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
import com.gempukku.swccgo.logic.timing.results.HitResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Alien
 * Title: Dannik Jerriko (V)
 */
public class Card501_045 extends AbstractAlien {
    public Card501_045() {
        super(Side.DARK, 2, 3, 3, 3, 5, "Dannik Jerriko", Uniqueness.UNIQUE);
        setLore("Anzati assassin. Cheek-folds hide proboscises which allow him to 'eat the soup' (consume the life Force) of his victims. Smokes t'bac. Currently working for Jabba.");
        setGameText("Characters lost from here, may not be removed from lost pile. (Except to be placed out of play.) While armed with a blaster, adds one battle destiny. Anything he hits is power and forfeit -2.");
        addIcons(Icon.A_NEW_HOPE);
        setSpecies(Species.ANZATI);
        addKeyword(Keyword.ASSASSIN);
        setTestingText("Dannik Jerriko (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsBattleDestinyModifier(self, new ArmedWithCondition(self, Filters.blaster), 1));

        // Characters lost from here, may not be removed from lost pile. (Except to be placed out of play.)
        modifiers.add();

        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
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
            return Collections.singletonList(action);
        }
        return null;
    }
}
