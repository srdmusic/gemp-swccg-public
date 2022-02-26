package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.PlayCardResult;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Character
 * Subtype: Resistance
 * Title: Fleet Admiral Gial Ackbar
 */
public class Card501_058 extends AbstractResistance {
    public Card501_058() {
        super(Side.LIGHT, 1, 2, 2, 3, 6, "Fleet Admiral Gial Ackbar", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Your starships are power +1 here. While at a system, may place Ackbar out of play to either: subtract 4 from a just-drawn weapon destiny here OR to prevent a [First Order] capital starship just deployed here from battling for remainder of turn.");
        addIcons(Icon.PILOT, Icon.EPISODE_VII, Icon.VIRTUAL_SET_18);
        addKeywords(Keyword.ADMIRAL);
        addPersona(Persona.ACKBAR);
        setTestingText("Fleet Admiral Gial Ackbar");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(self), Filters.starship, Filters.here(self)), 1));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<OptionalGameTextTriggerAction>();

        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if ((TriggerConditions.isWeaponDestinyJustDrawnBy(game, effectResult, playerId, Filters.here(self))
                || TriggerConditions.isWeaponDestinyJustDrawnBy(game, effectResult, opponent, Filters.here(self)))
            && GameConditions.canTarget(game, self, TargetingReason.TO_BE_PLACED_OUT_OF_PLAY, Filters.Ackbar)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId);
            action.setText("Subtract 4 from destiny");
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose Ackbar to place out of play", Filters.Ackbar) {
                                  @Override
                                  protected void cardTargeted(int targetGroupId, PhysicalCard targetedCard) {
                                      action.appendCost(
                                              new PlaceCardOutOfPlayFromTableEffect(action, targetedCard));
                                      action.allowResponses(new RespondableEffect(action) {
                                          @Override
                                          protected void performActionResults(Action targetingAction) {
                                              action.appendEffect(
                                                      new ModifyDestinyEffect(action, -4));
                                          }
                                      });
                                  }

                                  protected boolean getUseShortcut() {
                                      return true;
                                  }
                              }
                    );
            actions.add(action);
        }


        // Check condition(s)
        if (TriggerConditions.justDeployedToLocation(game, effectResult, Filters.and(Icon.FIRST_ORDER, Filters.capital_starship), Filters.here(self))
                && GameConditions.canTarget(game, self, TargetingReason.TO_BE_PLACED_OUT_OF_PLAY, Filters.Ackbar)) {

            final PlayCardResult playCardResult = (PlayCardResult) effectResult;
            final PhysicalCard playedCard = playCardResult.getPlayedCard();

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId);
            action.setText("Prevent starship from battling");
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose Ackbar to place out of play", Filters.Ackbar) {
                                       @Override
                                       protected void cardTargeted(int targetGroupId, PhysicalCard targetedCard) {
                                           action.appendCost(
                                                   new PlaceCardOutOfPlayFromTableEffect(action, targetedCard));
                                           action.allowResponses(new RespondableEffect(action) {
                                               @Override
                                               protected void performActionResults(Action targetingAction) {
                                                   action.appendEffect(
                                                           new MayNotBattleUntilEndOfTurnEffect(action, playedCard));
                                               }
                                           });
                                       }

                                       protected boolean getUseShortcut() {
                                           return true;
                                       }
                                   }
            );
            actions.add(action);
        }
        return actions;
    }
}
