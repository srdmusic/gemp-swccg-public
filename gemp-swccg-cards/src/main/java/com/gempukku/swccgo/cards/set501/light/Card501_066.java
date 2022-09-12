package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Title: He Is A Coward
 */
public class Card501_066 extends AbstractNormalEffect {
    public Card501_066() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, "He Is A Coward", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on a battleground. [Clone Army] Jedi deploy -1 here. If you just won a battle (or just Force drained here), relocate this card to your [Clone Army] objective. If opponent just won a battle, opponent may relocate this Effect to an [Episode I] battleground. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_20, Icon.EPISODE_I);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("He Is A Coward");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.battleground;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Icon.CLONE_ARMY, Filters.Jedi), -1, Filters.hasAttached(self)));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        final String playerId = self.getOwner();
        Filter filter = Filters.and(Filters.your(self), Icon.CLONE_ARMY, Filters.Objective, Filters.not(Filters.hasAttached(self)));
        if (Filters.canSpot(game, self, filter)
                && (TriggerConditions.wonBattle(game, effectResult, playerId)
                || TriggerConditions.forceDrainCompleted(game, effectResult, playerId, Filters.hasAttached(self)))) {

            PhysicalCard objective = Filters.findFirstActive(game, self, filter);
            if (objective != null) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Relocate to "+GameUtils.getFullName(objective));
                action.setActionMsg("Relocate to "+GameUtils.getCardLink(objective));

                action.appendEffect(
                        new AttachCardFromTableEffect(action, self, objective));

                return Collections.singletonList(action);

            }
        }

        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getOpponentsCardGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (TriggerConditions.wonBattle(game, effectResult, playerId)
                && GameConditions.canSpotLocation(game, Filters.and(Icon.EPISODE_I, Filters.battleground))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Relocate to a battleground");
            action.setActionMsg("Relocate " + GameUtils.getCardLink(self) + " to an [Episode I] battleground");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose an [Episode I] battleground", Filters.and(Icon.EPISODE_I, Filters.battleground)) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(
                                    new AttachCardFromTableEffect(action, self, finalTarget));
                        }
                    });
                }
            });
            return Collections.singletonList(action);
        }

        return null;
    }
}