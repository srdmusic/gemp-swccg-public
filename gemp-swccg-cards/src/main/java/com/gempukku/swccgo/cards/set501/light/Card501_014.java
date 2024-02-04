package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 23
 * Type: Objective
 * Title: I Can Bring You In Warm / ...Or I Can Bring You In Cold
 */
public class Card501_014 extends AbstractObjective {
    public Card501_014() {
        super(Side.LIGHT, 0, Title.I_Can_Bring_You_In_Warm, ExpansionSet.SET_23, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Mandalorian Covert, Bounty Hunter's Guild (with Bounty Puck there) and Bounty Hunting Is A Dangerous Profession. " +
                "For remainder of game you may not deploy Jedi (except Ahsoka and Luke), or [EPI] or [EPVII] characters. Once per turn, may [download] a [Mudhorn] location from Reserve Deck. " +
                "While this side up, during your move phase, may return your Din (and cards on him) to owner's hand. " +
                "Flip this card if Din or \"The Asset\" in battle.");
        addIcons(Icon.MUDHORN);
        addIcons(Icon.VIRTUAL_SET_23);
        setTestingText("I Can Bring You In Warm");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Mandalorian_Covert, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Mandalorian Covert to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Bounty_Hunters_Guild, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Bounty Hunter's Guild to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardToTargetFromReserveDeckEffect(action, Filters.Bounty_Puck, Filters.Bounty_Hunters_Guild, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Bounty Puck to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Bounty_Hunting_Is_A_Dangerous_Profession, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Bounty Hunting Is A Dangerous Profession to deploy";
                    }
                });

        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        Filter jediExeptLukeAhsoka = Filters.and(Filters.your(self), Filters.Jedi, Filters.except(Filters.or(Filters.Luke, Filters.Ahsoka)));

        modifiers.add(new MayNotDeployModifier(self, Filters.or(jediExeptLukeAhsoka, Filters.and(Filters.or(Icon.EPISODE_I, Icon.EPISODE_VII), Filters.character)), self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.I_CAN_BRING_YOU_IN_WARM__DOWNLOAD_MUDHORN_LOCATION;

        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy [Mudhorn] location from Reserve Deck");
            action.setActionMsg("Deploy a [Mudhorn] location from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Mudhorn_Location, true));
            return Collections.singletonList(action);
        }

        Filter din = Filters.and(Filters.your(playerId), Filters.Din);

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, Phase.MOVE)
                && GameConditions.canTarget(game, self, din)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Take Din into hand");
            // Perform result(s)
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Din to take into hand", din) {
                        @Override
                        protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Take " + GameUtils.getCardLink(targetedCard) + " into hand",
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ReturnCardToHandFromTableEffect(action, targetedCard, Zone.HAND));
                                        }
                                    }
                            );
                        }
                    }
            );
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();

        if (/*TriggerConditions.battleInitiated(game, effectResult)
                && */GameConditions.canBeFlipped(game, self)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.or(Filters.Din, Filters.The_Asset))) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }

        return null;
    }
}