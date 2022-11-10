package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HasStackedCondition;
import com.gempukku.swccgo.cards.effects.takeandputcards.StackCardsFromHandEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PutRandomCardsFromHandOnUsedPileEffect;
import com.gempukku.swccgo.logic.effects.ShuffleUsedPileEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeStackedCardsIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Defensive Shield
 * Title: Drop! (V)
 */
public class Card501_040 extends AbstractDefensiveShield {
    public Card501_040() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Drop, ExpansionSet.SET_20, Rarity.V);
        setVirtualSuffix(true);
        setGameText("Plays on table. May use 2 Force to target opponent's hand of > 12 cards; shuffle all but 9 random cards into opponent's Used Pile. If Grimtaash or Thrown Back just finished placing cards in your Used Pile, may exchange 3 cards in hand with 2 cards in Used Pile.");
        addIcons(Icon.VIRTUAL_DEFENSIVE_SHIELD);
        setTestingText("Drop! (V)");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.numCardsInHand(game, opponent) > 12
                && GameConditions.canUseForce(game, playerId, 2)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Target opponent's hand");
            action.setActionMsg("Shuffle all but 9 random cards from opponent's hand into opponent's Used Pile");

            action.appendCost(
                    new UseForceEffect(action, playerId, 2));

            // Perform result(s)
            action.appendEffect(
                    new PutRandomCardsFromHandOnUsedPileEffect(action, playerId, opponent, 9));
            action.appendEffect(
                    new ShuffleUsedPileEffect(action, self, opponent));

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.or(Filters.Grimtaash, Filters.title(Title.Thrown_Back)), new HasStackedCondition(self, Filters.any), ModifyGameTextType.REMOVE_TWO_MORE_CARDS));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(String playerId, SwccgGame game, Effect effect, PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(playerId);

        // protect 2 cards
        if (GameConditions.numCardsInHand(game, playerId) >= 2
                && (TriggerConditions.isAboutToPlaceRandomCardsFromOpponentsHandOnUsedPile(game, effect, opponent, Filters.or(Filters.Grimtaash, Filters.title(Title.Thrown_Back)))
                || TriggerConditions.isAboutToLookAtOpponentsHand(game, effect, opponent, Filters.or(Filters.Grimtaash, Filters.title(Title.Thrown_Back))))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Protect two cards in hand");

            action.appendEffect(
                    new StackCardsFromHandEffect(action, playerId, 2, 2, self, false));

            // add an effect to the existing action to return the cards to hand
            effect.getAction().appendAfterEffect(new TakeStackedCardsIntoHandEffect(action, self.getOwner(), 2, 2, self, Filters.any));

            actions.add(action);
        }

        return actions;
    }
}
