package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HasAttachedCondition;
import com.gempukku.swccgo.cards.evaluators.HereEvaluator;
import com.gempukku.swccgo.cards.evaluators.MultiplyEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.ForRemainderOfGameData;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.AttemptToBlowAwayShieldGateTotalModifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Scarif
 */
public class Card501_040 extends AbstractSystem {
    public Card501_040() {
        super(Side.DARK, Title.Scarif, 7, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("For remainder of game, while Shield Gate here, opponent's non-spy characters are deploy +1 to Scarif sites.");
        setLocationLightSideGameText("For remainder of game, add 2 to attempts to 'blow away' Shield Gate for each Profundity or Lightmaker here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.VIRTUAL_SET_16);
        setTestingText("Scarif (DS) (ERRATA)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextDarkSideRequiredAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        Condition shieldGateHere = new HasAttachedCondition(self, Filters.Shield_Gate);
        // Check condition(s)
        if (!GameConditions.cardHasAnyForRemainderOfGameDataSet(self)) {
            self.setForRemainderOfGameData(self.getCardId(), new ForRemainderOfGameData());
            // Add modifier here without creating an action
            game.getModifiersEnvironment().addUntilEndOfGameModifier(
                    new DeployCostToLocationModifier(self, Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.not(Filters.spy), Filters.character),
                    shieldGateHere, 1, Filters.Scarif_site));
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (!GameConditions.cardHasAnyForRemainderOfGameDataSet(self)) {
            self.setForRemainderOfGameData(self.getCardId(), new ForRemainderOfGameData());
            // Add modifier here without creating an action
            game.getModifiersEnvironment().addUntilEndOfGameModifier(
                    new AttemptToBlowAwayShieldGateTotalModifier(self, new MultiplyEvaluator(2, new HereEvaluator(self, Filters.or(Filters.Profundity, Filters.Lightmaker))))
            );
        }
        return null;
    }
}
