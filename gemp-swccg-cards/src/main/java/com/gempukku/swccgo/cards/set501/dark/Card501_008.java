package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Variable;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Juri Juice (V)
 */
public class Card501_008 extends AbstractNormalEffect {
    public Card501_008() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Juri_Juice, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Popular beverage served in many cantinas and tapcafes. Has intoxicating effect on many species. Favorite drink of Kabe, Chadra-Fan thief of Mos Eisley.");
        setGameText("Deploy on table. Once per turn, may deploy Baniss or Cantina from Reserve Deck; reshuffle. Once during your draw phase, may peek at up to X cards from the top of your Reserve Deck, where X = number of your aliens at Cantina; take one into hand. Immune to Blue Milk. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        addImmuneToCardTitle(Title.Blue_Milk);
        setTestingText("Juri Juice (V)");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        GameTextActionId gameTextActionId = GameTextActionId.JURI_JUICE_V__DEPLOY_CARD;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, "Baniss Keeg, Pilot Instructor")
                || GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Cantina))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Baniss or Cantina from Reserve Deck");
            action.setActionMsg("Deploy Baniss or Cantina from Reserve Deck");

            Filter filter = Filters.or(Filters.title("Baniss Keeg, Pilot Instructor"), Filters.Cantina);
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, filter, true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.JURI_JUICE_V__TAKE_CARD_INTO_HAND_FROM_USED_PILE;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DRAW)
                && GameConditions.hasReserveDeck(game, playerId)
                && GameConditions.canTarget(game, self, Filters.Cantina)) {

            int alienCount = Filters.countActive(game, self, Filters.and(Filters.your(self), Filters.alien, Filters.at(Filters.Cantina)));

            final int maxValueOfX = (int) Math.min(game.getModifiersQuerying().getVariableValue(game.getGameState(), self, Variable.X,
                            alienCount), game.getGameState().getReserveDeckSize(playerId));

            if (maxValueOfX > 0) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Peek at up to "+alienCount+" card"+ GameUtils.s(alienCount)+" from Reserve Deck");
                action.setActionMsg("Peek at up to "+alienCount+" card"+ GameUtils.s(alienCount)+" from the top of Reserve Deck and take one into hand");

                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new PlayoutDecisionEffect(action, playerId,
                                new IntegerAwaitingDecision("Choose number of cards to peek at", 1, maxValueOfX, maxValueOfX) {
                                    @Override
                                    public void decisionMade(final int num) {
                                        action.appendEffect(
                                                new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, num, 1, 1));
                                    }
                                }
                        ));

                // Perform result(s)
                actions.add(action);
            }
        }

        return actions;
    }
}