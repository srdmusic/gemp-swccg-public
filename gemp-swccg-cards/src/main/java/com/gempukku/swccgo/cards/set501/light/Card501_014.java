package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.ExcludeFromBattleEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveAwayFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
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
        setGameText("Deploy Mandalorian Covert, Bounty Hunter's Guild (with Holopuck there) and Bounty Hunting Is A Dangerous Profession. " +
                "For remainder of game you may not deploy Jedi (except Ahsoka and Luke), or [EPI] or [EPVII] characters. " +
                "Once per turn, may [download] a [Mudhorn] location from Reserve Deck; reshuffle. " +
                "'The Asset' may not move away from same location as Din." +
                "If a battle just initiated involving Din and 'The Asset', opponent chooses:" +
                "Lose the 'The Asset' to exclude Din from battle (you may [upload] any card; reshuffle)" +
                "OR Flip this card.");
        addIcons(Icon.MUDHORN, Icon.VIRTUAL_SET_23);
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
                new DeployCardToTargetFromReserveDeckEffect(action, Filters.Holopuck, Filters.Bounty_Hunters_Guild, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Holopuck to deploy";
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
        modifiers.add(new MayNotMoveAwayFromLocationModifier(self, Filters.The_Asset, Filters.sameLocationAs(self, Filters.Din)));
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

        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new ArrayList<>();

        if (TriggerConditions.battleInitiated(game, effectResult)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Din)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.The_Asset)
                && GameConditions.canBeFlipped(game, self)) {
            final PhysicalCard din = Filters.findFirstActive(game, self, Filters.Din);
            final PhysicalCard theAsset = Filters.findFirstActive(game, self, Filters.The_Asset);
            final GameState gameState = game.getGameState();
            final String opponent = game.getOpponent(self.getOwner());
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.appendEffect(
                    new PlayoutDecisionEffect(action, opponent,
                            new MultipleChoiceAwaitingDecision("Choose result", new String[]{"Lose " + GameUtils.getCardLink(theAsset) + " to exclude " + GameUtils.getCardLink(din) + " from battle",
                                    "Flip " + GameUtils.getCardLink(self)}) {
                                @Override
                                protected void validDecisionMade(int index, String result) {
                                    if (index == 0) {
                                        gameState.sendMessage(opponent + " chooses to lose " + GameUtils.getCardLink(theAsset) + " to exclude " + GameUtils.getCardLink(din) + " from battle");
                                        action.appendEffect(
                                                new LoseCardFromTableEffect(action, theAsset)
                                        );
                                        action.appendEffect(
                                                new ExcludeFromBattleEffect(action, din));
                                        action.appendEffect(
                                                new TakeCardIntoHandFromReserveDeckEffect(action, self.getOwner(), true)
                                        );
                                    } else {
                                        gameState.sendMessage(opponent + " chooses to Flip " + GameUtils.getCardLink(self));
                                        action.appendEffect(
                                                new FlipCardEffect(action, self));
                                    }
                                }
                            }
                    )
            );
            actions.add(action);
        }

        PhysicalCard holoPuck = Filters.findFirstActive(game, self, Filters.Holopuck);

        if (holoPuck != null
                && TriggerConditions.leavesTable(game, effectResult, Filters.The_Asset)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.appendEffect(
                    new SetWhileInPlayDataEffect(action, holoPuck, null));
            actions.add(action);
        }

        return actions;
    }
}