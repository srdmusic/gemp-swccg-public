package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToDeployCardToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Greef Karga, Disgraced Magistrate
 */
public class Card501_170 extends AbstractAlien {
    public Card501_170() {
        super(Side.LIGHT, 3, 3, 3, 3, 5, "Greef Karga, Disgraced Magistrate", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader. Information broker.");
        setGameText("If with Bounty Puck, opponent must first use 1 Force to deploy a character here and once during your turn, may retrieve 1 force. " +
                "Draws one battle destiny if unable to otherwise. " +
                "Immune to attrition < 3.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.LEADER, Keyword.INFORMATION_BROKER);
        setTestingText("Greef Karga, Disgraced Magistrate");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ExtraForceCostToDeployCardToLocationModifier(self, Filters.and(Filters.opponents(self), Filters.character),
                new WithCondition(self, Filters.Bounty_Puck), new ConstantEvaluator(1), Filters.location));
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, 1));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId retrievedForce = GameTextActionId.GREEF_KARGA__RETRIEVE_FORCE;

        // Check Condition(s)
        if (GameConditions.isOnceDuringYourTurn(game, self, playerId, gameTextSourceCardId, retrievedForce)
                && GameConditions.isWith(game, self, Filters.Bounty_Puck)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, retrievedForce);
            action.setText("Retrieve 1 Force");
            action.setActionMsg("Retrieve 1 Force");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, 1));
            return Collections.singletonList(action);
        }

        return null;
    }
}
