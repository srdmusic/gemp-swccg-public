package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.HasAboardCondition;
import com.gempukku.swccgo.cards.conditions.HasPilotingCondition;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.effects.RevealTopCardOfReserveDeckEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.ChooseEffectOrderEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.StandardEffect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Starship
 * Subtype: Starfighter
 * Title: Tydirium (V)
 */
public class Card501_092 extends AbstractStarfighter {
    public Card501_092() {
        super(Side.LIGHT, 3, 3, 2, null, 5, 3, 4, Title.Tydirium, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Stolen Imperial Lambda-class shuttle. Supposedly carried parts and technical crew. Delivered General Solo's crack team of Rebel scouts to the forest moon of Endor.");
        setGameText("May add 1 pilot and 6 passengers. Han deploys aboard for free. Permanent pilot provides ability of 2. Immune to attrition < 6 if a scout piloting (< 8 if Han). Once per game, may retrieve Fly Casual.");
        addIcons(Icon.ENDOR, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_18);
        addModelType(ModelType.LAMBDA_CLASS_SHUTTLE);
        setPilotCapacity(1);
        setPassengerCapacity(6);
        setAlwaysStolen(true);
        setMatchingPilotFilter(Filters.Han);
        setTestingText("Tydirium (V)");
    }


    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployForFreeForSimultaneouslyDeployingPilotModifier(self, Filters.Han));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiersEvenIfUnpiloted(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeploysFreeAboardModifier(self, Filters.Han, self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition scoutPiloting = new HasPilotingCondition(self, Filters.scout);
        Condition scoutHanPiloting = new HasPilotingCondition(self, Filters.and(Filters.Han, Filters.scout));

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, scoutPiloting, new ConditionEvaluator(6, 8, scoutHanPiloting)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.TYDIRIUM_V__RETRIEVE_FLY_CASUAL;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve Fly Casual");

            action.appendUsage(
                    new OncePerGameEffect(action));
            action.appendEffect(
                    new RetrieveCardEffect(action, playerId, Filters.title("Fly Casual")));

            return Collections.singletonList(action);
        }

        return null;
    }
}
