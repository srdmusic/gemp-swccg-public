package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HasPilotingCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.CancelImmunityToAttritionUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.ModifyPowerUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostForSimultaneouslyDeployingPilotModifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostToTargetModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Starfighter
 * Title: Kylo Ren's TIE Silencer
 */
public class Card501_073 extends AbstractStarfighter {
    public Card501_073() {
        super(Side.DARK, 2, 2, 3, null, 4, 3, 4, "Kylo Ren's TIE Silencer", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("May add 1 pilot. Kylo deploys -2 aboard. May reveal from hand to /\\ Kylo and deploy both simultaneously. Kylo and Silencer are immune to It Can Wait, Rebel Barrier and attrition < 5.");
        addIcons(Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_10, Icon.EPISODE_VII, Icon.FIRST_ORDER);
        addModelType(ModelType.TIE_VN);
        setPilotCapacity(1);
        setMatchingPilotFilter(Filters.Kylo);
        setTestingText("Kylo Ren's TIE Silencer (ERRATA)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostForSimultaneouslyDeployingPilotModifier(self, Filters.Kylo, -3));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiersEvenIfUnpiloted(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToTargetModifier(self, Filters.Kylo, -3, self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new HasPilotingCondition(self, Filters.Kylo), 5));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {

        // While Kylo piloting, immune to attrition < 5 and, unless Leia here,
        // once per battle may lose immunity to attrition to 'spin': add maneuver to power.

        Filter leiaHere = Filters.and(Persona.LEIA, Filters.atSameLocation(self));
        Filter selfWithImmunity = Filters.and(self, Filters.hasAnyImmunityToAttrition);

        // Check condition(s)
        if (GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId)
                && GameConditions.isInBattle(game, self)
                && GameConditions.hasPiloting(game, self, Filters.Kylo)
                && GameConditions.canSpot(game, self, selfWithImmunity)
                && !GameConditions.canSpot(game, self, leiaHere)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Cancel immunity and 'spin' to add power.");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));

            // Apply costs
            action.appendCost(
                    new CancelImmunityToAttritionUntilEndOfTurnEffect(action, self,
                            "Cancels " + GameUtils.getCardLink(self) + "'s immunity to attrition"));

            // Apply Effects
            float manueverOfShip = game.getModifiersQuerying().getManeuver(game.getGameState(), self);
            action.appendEffect(
                    new ModifyPowerUntilEndOfBattleEffect(action, self, manueverOfShip)
            );

            return Collections.singletonList(action);
        }
        return null;
    }
}
