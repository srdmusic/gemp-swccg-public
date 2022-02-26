package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ExcludeFromBattleEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Character
 * Subtype: Imperial
 * Title: Colonel Jendon (V)
 */
public class Card501_015 extends AbstractImperial {
    public Card501_015() {
        super(Side.DARK, 1, 2, 2, 2, 4, "Colonel Jendon", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Senior test pilot ordered to shake down first TIE defenders assigned to fleet operations. Occasionally given honor duty of flying Vader's shuttle.");
        setGameText("Adds 3 to power and 1 to maneuver of anything he pilots. While piloting Vader's Personal Shuttle, it may not be targeted by weapons and, if it is alone and opponent just initiated battle here with two or more starships, may exclude one.");
        addPersona(Persona.JENDON);
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.VIRTUAL_SET_18);
        setMatchingStarshipFilter(Filters.title("Vader's Personal Shuttle"));
        setTestingText("Colonel Jendon (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new ManeuverModifier(self, Filters.hasPiloting(self), 1));
        modifiers.add(new MayNotBeTargetedByWeaponsModifier(self, Filters.and(Filters.title("Vader's Personal Shuttle"), Filters.hasPiloting(self))));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);

        // While piloting Vader's Personal Shuttle, if it is alone and opponent just initiated battle here with two or more starships, may exclude one.

        Filter opponentStarshipFilter = Filters.and(Filters.opponents(self), Filters.starship, Filters.participatingInBattle);
        if (GameConditions.isPiloting(game, self, Filters.and(Filters.title("Vader's Personal Shuttle"), Filters.alone))
                && TriggerConditions.battleInitiatedAt(game, effectResult, opponent, Filters.here(self))
                && GameConditions.canSpot(game, self, 2, opponentStarshipFilter)
                && GameConditions.canTarget(game, self, TargetingReason.TO_BE_EXCLUDED_FROM_BATTLE, opponentStarshipFilter)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Exclude opponent's starship");
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target a starship to exclude", TargetingReason.TO_BE_EXCLUDED_FROM_BATTLE, opponentStarshipFilter) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard starship = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(
                                    new ExcludeFromBattleEffect(action, starship));
                        }
                    });
                }
            });
            return Collections.singletonList(action);
        }
        return null;
    }
}
