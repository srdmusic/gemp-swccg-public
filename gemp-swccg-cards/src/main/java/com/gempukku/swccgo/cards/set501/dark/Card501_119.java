package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.CancelGameTextUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Power Of The Hutt (V)
 */
public class Card501_119 extends AbstractNormalEffect {
    public Card501_119() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Power Of The Hutt", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Jabba runs his organization out of a palace built around a B'omarr monastery. His fortress near the border of the western Dune Sea is safe from enemies in Mos Eisley.");
        setGameText("Deploy on table. Once per turn, may deploy Jabba's Sail Barge, Hutt Influence, or Bib from Reserve Deck; reshuffle. If Jabba or Bib at Audience Chamber, once per turn may cancel [Reflections III] Leia's game text until end of turn (or may activate 1 Force if opponent's turn). [Immune to Alter.]");
        addIcons(Icon.PREMIUM, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Power Of The Hutt (V)");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.POWER_OF_THE_HUTT_V__DOWNLOAD_CARD;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Sets.newHashSet(Persona.JABBAS_SAIL_BARGE, Persona.BIB),
                Arrays.asList(Title.Hutt_Influence))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy Jabba's Sail Barge, Hutt Influence, or Bib from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Bib, Filters.Jabbas_Sail_Barge, Filters.Hutt_Influence), true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTarget(game, self, Filters.and(Filters.or(Filters.Jabba, Filters.Bib), Filters.at(Filters.Audience_Chamber)))) {

            // may cancel [Reflections III] Leia's game text
            if (GameConditions.canTarget(game, self, Filters.and(Icon.REFLECTIONS_III, Filters.Leia))) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Cancel Leia's game text");
                action.setActionMsg("Cancel [Reflections III] Leia's game text until end of turn");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Perform result(s)
                action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Leia", Filters.and(Icon.REFLECTIONS_III, Filters.Leia)) {
                    @Override
                    protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                        action.allowResponses(new RespondableEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                PhysicalCard finalLeia = action.getPrimaryTargetCard(targetGroupId);
                                action.appendEffect(
                                        new CancelGameTextUntilEndOfTurnEffect(action, finalLeia));
                            }
                        });
                    }
                });
                actions.add(action);
            }

            // may activate 1 Force if opponent's turn
            if (GameConditions.isOpponentsTurn(game, playerId)
                    && GameConditions.canActivateForce(game, playerId)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Activate 1 Force");

                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new ActivateForceEffect(action, playerId, 1));
                actions.add(action);
            }
        }
        return actions;
    }
}