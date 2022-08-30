package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.LandsForFreeModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TakesOffForFreeModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 20
 * Type: Starship
 * Subtype: Starfighter
 * Title: CIS Transport Shuttle
 */
public class Card501_033 extends AbstractStarfighter {
    public Card501_033() {
        super(Side.DARK, 4, 2, 3, null, 5, 4, 4, "CIS Transport Shuttle", Uniqueness.UNIQUE);
        setGameText("May add 1 pilot and 2 passengers. Permanent pilot provides ability of 2. When deployed, may deploy a battle droid aboard from Reserve Deck; reshuffle. Takes off and lands as an unlimited move (for free).");
        addIcons(Icon.NAV_COMPUTER, Icon.EPISODE_I, Icon.SCOMP_LINK, Icon.SEPARATIST, Icon.VIRTUAL_SET_20);
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
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggersEvenIfUnpiloted(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.CIS_TRANSPORT_SHUTTLE__DEPLOY_BATTLE_DROID_ABOARD;

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Persona.KYLO)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a battle droid from Reserve Deck");
            action.setActionMsg("Deploy a battle droid aboard " + GameUtils.getCardLink(self) + " from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.battle_droid, Filters.sameCardId(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new LandsForFreeModifier(self));
        return modifiers;
    }
//TODO "as an unlimited move"
    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiersEvenIfUnpiloted(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new TakesOffForFreeModifier(self));
        return modifiers;
    }
}
