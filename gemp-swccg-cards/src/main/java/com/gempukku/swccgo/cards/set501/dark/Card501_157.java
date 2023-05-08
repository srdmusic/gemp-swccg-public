package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.conditions.PhaseCondition;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Stranded
 */
public class Card501_157 extends AbstractNormalEffect {
    public Card501_157() {
        super(Side.DARK, 7, PlayCardZoneOption.ATTACHED, "Stranded", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Imperial troopers use tactics to strand and cut off fugitives. Only daring and unpredictable actions gave Luke and Leia a chance to escape.");
        setGameText("Deploy on a battleground site. While you occupy (except during a move phase): characters may not move from here and, if opponent occupies 2 systems (and no battleground sites) may place Effect in Used Pile to cause opponent to lose 3 Force.");
        addIcons(Icon.VIRTUAL_SET_21);
        setTestingText("Stranded");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.battleground_site;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotMoveFromLocationModifier(self, Filters.character, new AndCondition(new OccupiesCondition(self.getOwner(), Filters.here(self)), new NotCondition(new PhaseCondition(Phase.MOVE))), Filters.here(self)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(playerId);

        if (GameConditions.occupies(game, playerId, Filters.here(self))
                && !GameConditions.isDuringEitherPlayersPhase(game, Phase.MOVE)
                && !GameConditions.occupies(game, opponent, Filters.battleground_site)
                && GameConditions.occupies(game, opponent, 2, Filters.system)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Place in Used Pile");
            action.setActionMsg("Cause opponent to lose 3 Force");

            action.appendCost(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 3));

            return Collections.singletonList(action);
        }
        return null;
    }
}