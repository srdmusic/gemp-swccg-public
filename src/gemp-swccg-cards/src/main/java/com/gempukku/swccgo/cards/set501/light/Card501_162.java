package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnActionProxyEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Corporal Beezer (V)
 */
public class Card501_162 extends AbstractRebel {
    public Card501_162() {
        super(Side.LIGHT, 3, 2, 2, 2, 4, "Corporal Beezer", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Alderaanian slicer and technician. Trained by Brooks Carlson to serve as a Scout for Alliance commandos. She gets nervous when General Solo tries to hotwire something.");
        setGameText("Prevents opponent's from 'reacting' to same site. Once per turn, if [V] Strike Planning on table, may place a card from hand on Used Pile; the next scout you deploy this turn is deploy -1. Your scouts here are immune to Trample.");
        addIcons(Icon.ENDOR, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.SCOUT, Keyword.FEMALE);
        setSpecies(Species.ALDERAANIAN);
        setVirtualSuffix(true);
        setTestingText("Corporal Beezer (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotReactToLocationModifier(self, Filters.sameSite(self), game.getOpponent(self.getOwner())));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.your(self), Filters.scout, Filters.here(self)), Title.Trample));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        final List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        Filter vStrikePlanning = Filters.and(Filters.icon(Icon.VIRTUAL_SET_21), Filters.Strike_Planning);

        // Check condition(s)
        if (GameConditions.canSpot(game, self, vStrikePlanning)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.hasHand(game, playerId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Place card from hand on Used Pile");
            action.setActionMsg("Make the next scout you deploy this turn deploy -1");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Pay cost(s)
            action.appendCost(
                    new PutCardFromHandOnUsedPileEffect(action, playerId));

            final int permanentCardId = self.getPermanentCardId();
            // Perform result(s)
            action.appendEffect(
                    new AddUntilEndOfTurnActionProxyEffect(action,
                            new AbstractActionProxy() {
                                @Override
                                public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                    List<TriggerAction> actions = new LinkedList<>();

                                    // don't actually need to return an action
                                    // only need to track that an scout was deployed to increment the limit counter so the modifier is turned off
                                    if (TriggerConditions.justDeployed(game, effectResult, playerId, Filters.and(Filters.your(playerId), Filters.scout))) {
                                        PhysicalCard card = game.findCardByPermanentId(permanentCardId);
                                        for (String title : card.getTitles()) {
                                            game.getModifiersQuerying().getUntilEndOfTurnForCardTitleLimitCounter(title, GameTextActionId.OTHER_CARD_ACTION_DEFAULT).incrementToLimit(1, 1);
                                        }
                                    }
                                    return actions;
                                }
                            }
                    ));

            // this can't use EndOfTurnLimitCounterNotReachedCondition just in case she leaves the table because that resets the cardId that it uses to find the limit
            Condition turnLimitCounterNotReachedCondition = new Condition() {
                @Override
                public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                    PhysicalCard card = gameState.findCardByPermanentId(permanentCardId);
                    for (String title : card.getTitles()) {
                        if (modifiersQuerying.getUntilEndOfTurnForCardTitleLimitCounter(title, GameTextActionId.OTHER_CARD_ACTION_DEFAULT).getUsedLimit() >= 1)
                            return false;
                    }
                    return true;
                }
            };
            action.appendEffect(
                    new AddUntilEndOfTurnModifierEffect(action, new DeployCostModifier(self, Filters.and(Filters.your(self), Filters.scout, Filters.not(Filters.onTable)),
                            turnLimitCounterNotReachedCondition,-1), "Reduces the cost of your next scout you deploy this turn by 1"));
            actions.add(action);
        }

        return actions;
    }
}
