package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Resistance
 * Title: Paige Tico
 */
public class Card501_034 extends AbstractResistance {
    public Card501_034() {
        super(Side.LIGHT, 3, 2, 2, 2, 5, Title.Paige, Uniqueness.UNIQUE);
        setLore("Female Gunner.");
        setGameText("While out of play, adds 1 to your total power where you have a resistance character of ability = 2. Adds 1 to weapon destiny and defense value of anything she is aboard as a passenger. When lost may place of out play.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_15);
        addKeywords(Keyword.FEMALE, Keyword.GUNNER);
        setTestingText("Paige Tico");
    }

    @Override
    public List<Modifier> getWhileOutOfPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new TotalPowerModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.your(self.getOwner()), Filters.Resistance_character, Filters.abilityEqualTo(2))), 1, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.hasPassenger(self), 1));
        modifiers.add(new DefenseValueModifier(self, Filters.hasPassenger(self), 1));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isAboutToBeLost(game, effectResult, self)
                || TriggerConditions.isAboutToBeForfeitedToLostPile(game, effectResult, self)) {
            final AboutToLeaveTableResult result = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard cardToBeLost = result.getCardAboutToLeaveTable();
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place " + GameUtils.getFullName(cardToBeLost) + " out of play");
            action.setActionMsg("Place " + GameUtils.getCardLink(cardToBeLost) + " out of play");
            // Perform result(s)
            action.appendEffect(
                    new PassthruEffect(action) {
                        @Override
                        protected void doPlayEffect(SwccgGame game) {
                            result.getPreventableCardEffect().preventEffectOnCard(cardToBeLost);
                            action.appendEffect(
                                    new PlaceCardOutOfPlayFromTableEffect(action, result.getCardAboutToLeaveTable()));
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }
}