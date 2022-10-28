package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.conditions.OutOfPlayCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 9
 * Type: Character
 * Subtype: Resistance
 * Title: Rose Tico
 */
public class Card501_097 extends AbstractResistance {
    public Card501_097() {
        super(Side.LIGHT, 5, 1, 1, 2, 2, "Rose Tico", Uniqueness.UNIQUE);
        setLore("Female.");
        setGameText("Resistance characters here are immune to Dr. Evazan. If Finn is about to be lost from same site, may place him in your Used Pile instead. If with a Resistance character at opponent's site (or Paige out of play and Rose not on Jakku) Force drain +1 here.");
        addPersona(Persona.ROSE);
        addIcons(Icon.EPISODE_VII, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_9);
        addKeywords(Keyword.FEMALE);
        setTestingText("Rose Tico (ERRATA)");
    }
    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.Resistance_character, Filters.here(self)), Title.Dr_Evazan));
        modifiers.add(new ForceDrainModifier(self, Filters.here(self),
                new OrCondition(
                        new AndCondition(new WithCondition(self, Filters.Resistance_character), new AtCondition(self, Filters.and(Filters.opponents(self), Filters.site))),
                        new AndCondition(new OutOfPlayCondition(self, Filters.Paige), new NotCondition(new OnCondition(self, Title.Jakku)))),
                1, self.getOwner()));
        return modifiers;
    }
    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        Filter finnAtSameSite = Filters.and(Filters.Finn, Filters.atSameSite(self));
        if (TriggerConditions.isAboutToBeLost(game, effectResult, finnAtSameSite)
                || TriggerConditions.isAboutToBeForfeitedToLostPile(game, effectResult, finnAtSameSite)) {
            final AboutToLeaveTableResult result = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard cardToBeLost = result.getCardAboutToLeaveTable();
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place " + GameUtils.getFullName(cardToBeLost) + " in Used Pile");
            action.setActionMsg("Place " + GameUtils.getCardLink(cardToBeLost) + " in Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PassthruEffect(action) {
                        @Override
                        protected void doPlayEffect(SwccgGame game) {
                            result.getPreventableCardEffect().preventEffectOnCard(cardToBeLost);
                            action.appendEffect(
                                    new PlaceCardInUsedPileFromTableEffect(action, result.getCardAboutToLeaveTable()));
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }
}
