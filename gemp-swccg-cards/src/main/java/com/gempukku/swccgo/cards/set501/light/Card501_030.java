package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.CancelDestinyAndCauseRedrawEffect;
import com.gempukku.swccgo.logic.effects.ModifyPowerUntilEndOfPlayersNextTurnEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetDeployCostModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Effect
 * Title: Your Thoughts Dwell On Your Mother
 */
public class Card501_030 extends AbstractNormalEffect {
    public Card501_030() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Your_Thoughts_Dwell_On_Your_Mother, Uniqueness.UNIQUE);
        setLore("");
        setGameText("If Slave Quarters on table, deploy on table. Anakin is deploy = 6 and, if drawn for destiny, may take him into hand to cancel and cause a redraw. Game text on Jedi Council Chamber is canceled. If Shmi is lost, Anakin is power +5 until end of your next turn. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.CORUSCANT, Icon.VIRTUAL_SET_16);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Your Thoughts Dwell On Your Mother");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.canSpot(game, self, Filters.Slave_Quarters);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ResetDeployCostModifier(self, Filters.Anakin, 6));
        modifiers.add(new CancelsGameTextModifier(self, Filters.Jedi_Council_Chamber));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalDrawnAsDestinyTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ANY_CARD__CANCEL_AND_REDRAW_A_DESTINY;

        // Check condition(s)
        if (GameConditions.isDestinyCardMatchTo(game, Filters.Anakin)
                && GameConditions.canTakeDestinyCardIntoHand(game, playerId)
                && GameConditions.canCancelDestinyAndCauseRedraw(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take into hand and cause re-draw");
            action.setActionMsg("Cancel destiny and cause re-draw");
            // Pay cost(s)
            action.appendEffect(
                    new TakeDestinyCardIntoHandEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new CancelDestinyAndCauseRedrawEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLeavesTableRequiredTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.justLost(game, effectResult, Filters.Shmi)
                && GameConditions.isOpponentsTurn(game, self)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make Anakin power +5");
            // Perform result(s)
            action.appendEffect(
                    new ModifyPowerUntilEndOfPlayersNextTurnEffect(action, self.getOwner(), Filters.Anakin, 5, "Makes Anakin's power +5"));
            return Collections.singletonList(action);
        }
        return null;
    }
}
