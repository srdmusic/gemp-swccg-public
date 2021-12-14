package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PresentWithCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 17
 * Type: Character
 * Subtype: Droid
 * Title: BB-9E
 */
public class Card501_105 extends AbstractDroid {
    public Card501_105() {
        super(Side.DARK, 0.5, 2, 2, 4, "BB-9E", Uniqueness.UNIQUE);
        setAlternateDestiny(5.5);
        setGameText("While present with your leader or First Order character, cancels BB-8's and/or Rose's game text at same and related sites. During your move phase, may place in Used Pile; 'break cover' of all Undercover spies here (if any).");
        addIcons(Icon.EPISODE_VII, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_17);
        addModelType(ModelType.ASTROMECH);
        setTestingText("BB-9E");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new CancelsGameTextModifier(self, Filters.and(Filters.or(Filters.BB8, Filters.Rose), Filters.at(Filters.sameOrRelatedSite(self))), new PresentWithCondition(self, Filters.and(Filters.your(self), Filters.or(Filters.leader, Filters.First_Order_character)))));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        Filter targetFilter = Filters.and(Filters.undercover_spy, Filters.atSameSite(self));

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.MOVE)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Place in Used Pile");
            action.setActionMsg("'Break cover' of all undercover spies here (if any)");

            //TODO this should really let you choose the order
            Collection<PhysicalCard> undercoverSpies = Filters.filterAllOnTable(game, Filters.and(targetFilter, Filters.canBeTargetedBy(self)));

            action.appendCost(
                    new PlaceCardInUsedPileFromTableEffect(action, self));

            for(PhysicalCard spy:undercoverSpies) {
                action.appendEffect(
                        new BreakCoverEffect(action, spy));
            }
            actions.add(action);
        }

        return actions;
    }
}
