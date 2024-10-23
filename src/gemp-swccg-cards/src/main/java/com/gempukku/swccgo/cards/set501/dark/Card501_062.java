package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.PutStackedCardInLostPileEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: Second Sister
 */
public class Card501_062 extends AbstractImperial {
    public Card501_062() {
        super(Side.DARK, 2, 5, 4, 5, 7, "Second Sister", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Female Inquisitor.");
        setGameText("Padawans are power -2 here. " +
                "Once per game, may place a 'Hatred' card here in owner’s Lost Pile to choose: " +
                "Take any card into hand from Used Pile; reshuffle. " +
                "OR Deploy a lightsaber on this character from Lost Pile. " +
                "Immune to attrition < 4 while Vader not here.");
        addKeywords(Keyword.INQUISITOR, Keyword.FEMALE);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_24);
        setTestingText("Second Sister");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.padawan, Filters.here(self)), -2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, new NotCondition(new HereCondition(self, Filters.Vader)), 4));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        final List<TopLevelGameTextAction> actions = new ArrayList<>();

        final GameTextActionId gameTextActionId = GameTextActionId.SECOND_SISTER__USE_HATRED_CARD;
        final Filter hatredCardStackedHere = Filters.and(Filters.hatredCard, Filters.stacked, Filters.here(self));


        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canSpot(game, self, hatredCardStackedHere)) {

            if (GameConditions.hasUsedPile(game, playerId)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Take any card into hand from Used Pile");
                action.appendUsage(
                        new OncePerGameEffect(action)
                );
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Target 'Hatred' card", hatredCardStackedHere) {
                            @Override
                            protected void cardTargeted(int targetGroupId, PhysicalCard targetedCard) {
                                action.appendEffect(
                                        new PutStackedCardInLostPileEffect(action, playerId, targetedCard, false)
                                );
                                // Update usage limit(s)
                                action.appendEffect(
                                        new TakeCardIntoHandFromUsedPileEffect(action, playerId, true));
                                actions.add(action);
                            }
                        }
                );
            }

            if (GameConditions.hasLostPile(game, playerId)
                    && GameConditions.isDuringYourPhase(game, playerId, Phase.DEPLOY)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Deploy a lightsaber on this character from Lost Pile");
                action.appendUsage(
                        new OncePerGameEffect(action)
                );
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Target 'Hatred' card", hatredCardStackedHere) {
                            @Override
                            protected void cardTargeted(int targetGroupId, PhysicalCard targetedCard) {
                                action.appendEffect(
                                        new PutStackedCardInLostPileEffect(action, playerId, targetedCard, false)
                                );
                                // Update usage limit(s)
                                action.appendEffect(
                                        new DeployCardToTargetFromLostPileEffect(action, Filters.lightsaber, Filters.sameCardId(self), true));
                                actions.add(action);
                            }
                        }
                );
            }
        }

        return actions;
    }
}
