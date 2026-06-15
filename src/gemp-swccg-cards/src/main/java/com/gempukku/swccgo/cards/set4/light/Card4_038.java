package com.gempukku.swccgo.cards.set4.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.conditions.TargetedByUtinniEffectCondition;
import com.gempukku.swccgo.cards.conditions.TrainedByCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Dagobah
 * Type: Effect
 * Title: Smuggler's Blues
 */
public class Card4_038 extends AbstractNormalEffect {
    public Card4_038() {
        super(Side.LIGHT, 3, PlayCardZoneOption.ATTACHED, "Smuggler's Blues", Uniqueness.UNIQUE, ExpansionSet.DAGOBAH, Rarity.R);
        setLore("It's the lure of easy credits. It's got a very strong appeal. Perhaps you'd understand better wearing my flight suit. It's the ultimate special modification, it's the smuggler's blues.");
        setGameText("Deploy on a smuggler. May use 2 Force to cancel Limited Resources. Also, if 'trained' by Rycar Ryjerd and piloting a starship when that starship completes Kessel Run, Rycar's Run or The First Transport Is Away, any retrieved Force may be taken into hand.");
        addIcons(Icon.DAGOBAH);
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.smuggler;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (GameConditions.canUseForce(game, playerId, 2)
                && GameConditions.canTargetToCancel(game, self, Filters.Limited_Resources)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Limited_Resources, Title.Limited_Resources, 2);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Limited_Resources)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)
                && GameConditions.canUseForce(game, playerId, 2)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect, 2);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        PhysicalCard attachedTo = self.getAttachedTo();
        Condition trainedByRycar = new TrainedByCondition(attachedTo, Filters.Rycar_Ryjerd);

        Condition qualifyingKesselRunCondition = new AndCondition(
                trainedByRycar,
                new PilotingCondition(attachedTo),
                new TargetedByUtinniEffectCondition(attachedTo,Filters.Kessel_Run));

        Condition qualifyingRycarsRunCondition = new AndCondition(
                trainedByRycar,
                new PilotingCondition(attachedTo,Filters.targetedByCardOnTable(Filters.Rycars_Run)));
        ////////cannot use Filters.targetedByCardOnTable inside condition/modifier due to loop / stack overflow issues

        Condition qualifyingFirstTransportCondition = new AndCondition(
                trainedByRycar,
                new PilotingCondition(attachedTo,Filters.targetedByCardOnTable(Filters.The_First_Transport_Is_Away)));
        ////////cannot use Filters.targetedByCardOnTable inside condition/modifier due to loop / stack overflow issues

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Kessel_Run, qualifyingKesselRunCondition, ModifyGameTextType.KESSEL_RUN__RETRIEVE_FORCE_INTO_HAND));
        modifiers.add(new ModifyGameTextModifier(self, Filters.Rycars_Run, qualifyingRycarsRunCondition, ModifyGameTextType.RYCARS_RUN__RETRIEVE_FORCE_INTO_HAND));
        modifiers.add(new ModifyGameTextModifier(self, Filters.The_First_Transport_Is_Away, qualifyingFirstTransportCondition, ModifyGameTextType.THE_FIRST_TRANSPORT_IS_AWAY__RETRIEVE_FORCE_INTO_HAND));

        return modifiers;
    }
}
