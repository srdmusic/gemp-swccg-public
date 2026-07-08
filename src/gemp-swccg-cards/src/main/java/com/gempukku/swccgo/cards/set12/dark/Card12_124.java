package com.gempukku.swccgo.cards.set12.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeOneCardIntoHandFromOffTableEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.RaceDestinyStackedResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Coruscant
 * Type: Character
 * Subtype: Alien
 * Title: Tusken Raider
 */
public class Card12_124 extends AbstractAlien {
    public Card12_124() {
        super(Side.DARK, 3, 2, 2, 1, 2, "Tusken Raider", Uniqueness.UNRESTRICTED, ExpansionSet.CORUSCANT, Rarity.C);
        setLore("Tusken Raiders are constantly taking bets to see who can hit the most Podracers on the Boonta Eve racetrack.");
        setGameText("While at Podrace Arena, if opponent just placed race destiny on their Podracer, once per turn may use 3 Force to 'shoot' (no other Tusken Raiders may 'shoot' this turn.) Draw destiny. If destiny > 3, place race destiny in opponent's hand.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I);
        setSpecies(Species.TUSKEN_RAIDER);
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.justStackedRaceDestinyOn(game, effectResult, Filters.any, Filters.and(Filters.opponents(self), Filters.Podracer))
                && GameConditions.isAtLocation(game, self, Filters.Podrace_Arena)
                && GameConditions.canUseForce(game, playerId, 3)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId)
                && (!GameConditions.isShootUsedByTuskenRaiderThisTurn(game)
                    || !self.getBlueprint().getSpecies().equals(Species.TUSKEN_RAIDER) ) //jic, for mindscan
                && GameConditions.canDrawDestiny(game, playerId)) {

            final RaceDestinyStackedResult raceDestinyStackedResult = (RaceDestinyStackedResult) effectResult;
            final PhysicalCard justStackedRaceDestiny = raceDestinyStackedResult.getCard();

            if(raceDestinyStackedResult.getPerformingPlayerId().equals(opponent) && justStackedRaceDestiny != null) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("'Shoot' " + GameUtils.getFullName(justStackedRaceDestiny));
                action.setActionMsg("'Shoot' " + GameUtils.getCardLink(justStackedRaceDestiny));
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                action.appendUsage(
                        new PassthruEffect(action) {
                            @Override
                            protected void doPlayEffect(SwccgGame game) {
                                game.getModifiersQuerying().shootUsedByTuskenRaider();
                            }
                        }
                );
                // Pay cost(s)
                action.appendCost(
                        new UseForceEffect(action, playerId, 3));
                // Perform result(s)
                action.appendEffect(
                        new DrawDestinyEffect(action, playerId) {
                            @Override
                            protected void destinyDraws(SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, Float totalDestiny) {
                                GameState gameState = game.getGameState();
                                if (totalDestiny == null) {
                                    gameState.sendMessage("Result: Failed due to failed destiny draw");
                                    return;
                                }
                                if (totalDestiny > 3) {
                                    gameState.sendMessage("Result: Succeeded");
                                    action.appendEffect(
                                            new TakeOneCardIntoHandFromOffTableEffect(action, opponent, justStackedRaceDestiny, "Take " + GameUtils.getCardLink(justStackedRaceDestiny) + " into hand") {
                                                @Override
                                                protected void afterCardTakenIntoHand() {
                                                }
                                            }
                                    );
                                } else {
                                    gameState.sendMessage("Result: Failed");
                                }
                            }
                        });
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
