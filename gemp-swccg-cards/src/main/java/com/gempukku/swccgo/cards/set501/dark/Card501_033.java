package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.NumTimesPerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.StackActionEffect;
import com.gempukku.swccgo.logic.modifiers.MayDeployAsLandedToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Starship
 * Subtype: Starfighter
 * Title: CIS Transport Shuttle
 */
public class Card501_033 extends AbstractStarfighter {
    public Card501_033() {
        super(Side.DARK, 4, 2, 3, null, 5, 4, 4, "CIS Transport Shuttle", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 1 pilot and 2 passengers. Permanent pilot provides ability of 2. May deploy to exterior sites. Twice per turn,  may land or take off as an unlimited move for free.");
        addIcons(Icon.NAV_COMPUTER, Icon.EPISODE_I, Icon.SCOMP_LINK, Icon.SEPARATIST, Icon.VIRTUAL_SET_21);
        addIcon(Icon.PILOT, 1);
        addModelType(ModelType.SHEATHIPEDE_CLASS_SHUTTLE);
        setPilotCapacity(1);
        setPassengerCapacity(2);
        setTestingText("CIS Transport Shuttle");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(2) {});
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayDeployAsLandedToLocationModifier(self, Filters.exterior_site));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (GameConditions.isNumTimesPerTurn(game, self, playerId, 2, gameTextSourceCardId, gameTextActionId)) {

            Action landAction = getLandAction(playerId, game, self, true, false, false, false, true, Filters.any);
            if (landAction != null) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Land as an unlimited move");
                action.appendUsage(
                        new NumTimesPerTurnEffect(action, 2));
                action.appendEffect(
                        new StackActionEffect(action, landAction));

                actions.add(action);
            }
        }

        return actions;
    }
    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActionsEvenIfUnpiloted(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (GameConditions.isNumTimesPerTurn(game, self, playerId, 2, gameTextSourceCardId, gameTextActionId)
                && Filters.pilotedForTakeOff.accepts(game, self)) {

            Action takeOffAction = getTakeOffAction(playerId, game, self, true, false, false, false, true, Filters.any);
            if (takeOffAction != null) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Take off as an unlimited move");
                action.appendUsage(
                        new NumTimesPerTurnEffect(action, 2));
                action.appendEffect(
                        new StackActionEffect(action, takeOffAction));

                actions.add(action);
            }
        }
        return actions;
    }
}
