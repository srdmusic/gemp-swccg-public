package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: You May Start Your Landing (V)
 */
public class Card501_005 extends AbstractNormalEffect {
    public Card501_005() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.You_May_Start_Your_Landing, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Echo Base was no match for the Imperial war machine.");
        setGameText("Deploy on table. If you just deployed an AT-AT, may peek at top two cards of Reserve Deck and take one into hand. During your control phase, opponent loses 1 Force for each marker site your AT-AT controls (limit 1 unless you occupy Hoth system). [Immune to Alter.]");
        addIcons(Icon.TATOOINE, Icon.HOTH, Icon.VIRTUAL_SET_22);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("You May Start Your Landing (V)");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.controlsWith(game, self, playerId, Filters.marker_site, Filters.and(Filters.piloted, Filters.AT_AT))) {

            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.marker_site, Filters.controlsWith(playerId, self, Filters.and(Filters.piloted, Filters.AT_AT))));
            if (numForce > 0) {
                final boolean occupiesHoth = GameConditions.occupies(game, playerId, Filters.Hoth_system);
                if (!occupiesHoth) {
                    numForce = 1;
                }
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        // Check if reached end of each control phase and action was not performed yet.
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.controlsWith(game, self, playerId, Filters.marker_site, Filters.and(Filters.piloted, Filters.AT_AT))) {

            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.marker_site, Filters.controlsWith(playerId, self, Filters.and(Filters.piloted, Filters.AT_AT))));

            if (numForce > 0) {
                final boolean occupiesHoth = GameConditions.occupies(game, playerId, Filters.Hoth_system);
                if (!occupiesHoth) {
                    numForce = 1;
                }
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Just deployed AT-AT trigger
        if(TriggerConditions.justDeployed(game, effectResult, playerId, Filters.AT_AT)
                && GameConditions.hasReserveDeck(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Peek at top two cards of Reserve Deck and take one into hand");
            action.appendEffect(
                    new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, 2, 1, 1));

            actions.add(action);
        }

        return actions;
    }
}