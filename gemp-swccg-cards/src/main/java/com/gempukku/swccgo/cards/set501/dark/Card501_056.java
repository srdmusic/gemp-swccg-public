package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.PlaceCardOutOfPlayFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.AttritionModifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Imperial
 * Title: Officer Valin Hess
 */
public class Card501_056 extends AbstractImperial {
    public Card501_056() {
        super(Side.DARK, 2, 3, 3, 3, 5, "Officer Valin Hess", Uniqueness.UNIQUE);
        setLore("Leader.");
        setGameText("During battle, may place an Imperial of ability < 4 out play from your lost pile to add their ability to your total attrition. Except during battle, may lose Valin Hess to place an opponent’s undercover spy here in lost pile.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_16);
        addKeywords(Keyword.LEADER);
        setTestingText("[Set 17] Officer Valin Hess");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OFFICER_VALIN_HESS__PLACE_IMPERIAL_OUT_OF_PLAY;

        if (GameConditions.isInBattle(game, self)
                && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Place an Imperial of ability < 4 out play");
            action.appendUsage(
                    new OncePerBattleEffect(action)
            );
            action.appendCost(
                    new PlaceCardOutOfPlayFromLostPileEffect(action, playerId, playerId, Filters.and(Filters.Imperial, Filters.abilityLessThan(4)), false) {
                        @Override
                        protected void cardPlacedOutOfPlay(PhysicalCard character) {
                            float ability = game.getModifiersQuerying().getAbility(game.getGameState(), character);
                            action.appendEffect(
                                    new AddUntilEndOfBattleModifierEffect(action,
                                            new AttritionModifier(self, ability, game.getOpponent(playerId)), "Add " + ability + " to your total attrition"
                                    )
                            );
                        }
                    });
            actions.add(action);
        }

        Filter targetFilter = Filters.and(Filters.opponents(self), Filters.undercover_spy, Filters.here(self));

        if (!GameConditions.isInBattle(game, self)
                && GameConditions.canTarget(game, self, SpotOverride.INCLUDE_UNDERCOVER, targetFilter)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Place opponent's undercover spy in lost pile");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target undercover spy", SpotOverride.INCLUDE_UNDERCOVER, targetFilter) {
                        @Override
                        protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(self);
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Place " + GameUtils.getCardLink(targetedCard) + " in lost pile",
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new LoseCardFromTableEffect(action, self));
                                            action.appendEffect(
                                                    new PlaceCardInLostPileFromTableEffect(action, targetedCard));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }
}

