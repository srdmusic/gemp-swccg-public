package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.MoveCardAsRegularMoveEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PassengerAppliesAbilityForBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Starfighter
 * Title: Razor Crest
 */
public class Card501_116 extends AbstractStarfighter {
    public Card501_116() {
        super(Side.LIGHT, 3, 4, 5, 5, null, 5, 7, "Razor Crest", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 1 pilot and 2 passengers. Once per turn, may make an additional move when landing or taking off. " +
                "Characters aboard apply their ability towards drawing battle destiny. " +
                "Immune to attrition < 5 (< 6 if Din piloting or at a related location).");
        addPersona(Persona.RAZOR_CREST);
        addIcons(Icon.MUDHORN, Icon.NAV_COMPUTER, Icon.INDEPENDENT, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_23);
        addModelType(ModelType.ST_70_CLASS_RAZOR_CREST_M_111_ASSAULT_SHIP);
        setPilotCapacity(1);
        setPassengerCapacity(2);
        setMatchingPilotFilter(Filters.Din);
        setTestingText("Razor Crest");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, gameTextSourceCardId)
                && Filters.movableAsAdditionalMove(playerId).accepts(game, self)
                && (TriggerConditions.justLandedAt(game, effectResult, self, Filters.any)
                || TriggerConditions.justTookOff(game, effectResult, self))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make an additional move");
            action.setActionMsg("Have " + GameUtils.getCardLink(self) + " make an additional move");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new MoveCardAsRegularMoveEffect(action, playerId, self, false, true, Filters.any));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PassengerAppliesAbilityForBattleDestinyModifier(self, Filters.aboardAsPassenger(self)));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new ConditionEvaluator(5, 6,
                new OnTableCondition(self, Filters.and(Filters.Din, Filters.at(Filters.relatedLocation(self)))))));
        return modifiers;
    }
}