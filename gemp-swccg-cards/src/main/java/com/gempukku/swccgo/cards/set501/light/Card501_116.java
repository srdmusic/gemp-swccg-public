package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HasAboardCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
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
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardAboardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PassengerAppliesAbilityForBattleDestinyModifier;

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
        super(Side.LIGHT, 3, 4, 5, 5, null, 5, 6, "Razor Crest", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 1 pilot and 2 passengers. Once per game, may [download] an alien of ability = 4 aboard. " +
                "Characters aboard apply their ability towards drawing battle destiny. " +
                "Immune to attrition < 5 (<6 if Din aboard.)");
        addPersona(Persona.RAZOR_CREST);
        addIcons(Icon.MUDHORN, Icon.NAV_COMPUTER, Icon.INDEPENDENT, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_23);
        addModelType(ModelType.ST_70_CLASS_RAZOR_CREST_M_111_ASSAULT_SHIP);
        setPilotCapacity(1);
        setPassengerCapacity(2);
        setMatchingPilotFilter(Filters.Din);
        setTestingText("Razor Crest");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActionsEvenIfUnpiloted(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.RAZOR_CREST__DOWNLOAD_ALIEN;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Alien aboard");
            action.setActionMsg("Deploy Alien of ability = 4 aboard from Reserve Deck");
            action.appendUsage(
                    new OncePerGameEffect(action));
            action.appendEffect(
                    new DeployCardAboardFromReserveDeckEffect(action, Filters.and(Filters.alien, Filters.abilityEqualTo(4)), Filters.sameCardId(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PassengerAppliesAbilityForBattleDestinyModifier(self, Filters.aboardAsPassenger(self)));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new ConditionEvaluator(5, 6,
                new HasAboardCondition(self, Filters.Din))));
        return modifiers;
    }
}