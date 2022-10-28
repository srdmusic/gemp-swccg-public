package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HasPilotingCondition;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 20
 * Type: Starship
 * Subtype: Starfighter
 * Title: Millennium Falcon
 */
public class Card501_094 extends AbstractStarfighter {
    public Card501_094() {
        super(Side.LIGHT, 2, 3, 3, null, 4, 6, 7, "Millennium Falcon", Uniqueness.UNIQUE);
        setLore("Modified YT-1300 freighter. Owned by Lando Calrissian until won by Han in a sabacc game. 26.7 meters long. 'She may not look like much, but she's got it where it counts.'");
        setGameText("May add 2 pilots and 2 passengers. If The Force is Strong in my Family on table, may deploy [0]Chewie or Han aboard from Reserve Deck; reshuffle. Immune to attrition < 5 if Han or Chewie piloting. (< 6 if both).");
        addPersona(Persona.FALCON);
        addIcons(Icon.NAV_COMPUTER, Icon.SCOMP_LINK);
        addModelType(ModelType.MODIFIED_LIGHT_FREIGHTER);
        setPilotCapacity(2);
        setPassengerCapacity(2);
        setMatchingPilotFilter(Filters.or(Filters.Han, Filters.Chewie));
        setTestingText("Millennium Falcon (V)");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MILLENNIUM_FALCON__DOWNLOAD_HAN_OR_CHEWIE;

        if (GameConditions.canSpot(game, self, Filters.The_Force_Is_Strong_In_My_Family)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
            
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Download Han or Chewie to Millennium Falcon");
            action.appendEffect(new DeployCardToTargetFromReserveDeckEffect(action, Filters.or(Filters.Han, Filters.Chewie), Filters.sameCardId(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition hanPiloting = new HasPilotingCondition(self, Filters.Han);
        Condition chewiePiloting = new HasPilotingCondition(self, Filters.Chewie);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new OrCondition(hanPiloting, chewiePiloting), new ConditionEvaluator(5, 6, new AndCondition(hanPiloting, chewiePiloting))));
        return modifiers;
    }
}
