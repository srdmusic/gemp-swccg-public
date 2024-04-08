package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.evaluators.StackedEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardsToHandFromTableSimultaneouslyEffect;
import com.gempukku.swccgo.logic.effects.TargetCardsOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.TotalForceGenerationModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 23
 * Type: Epic Event
 *  Title: Bounty Hunting Is A Dangerous Profession
 */
public class Card501_011 extends AbstractEpicEventDeployable {
    public Card501_011() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Bounty_Hunting_Is_A_Dangerous_Profession, Uniqueness.UNIQUE, ExpansionSet.SET_23, Rarity.V);
        setGameText("Deploy on table if your [Mudhorn] objective on table. " +
                "Your Boba, Cara Dune, and Grogu are deploy -1. " +
                "At the end of opponent's deploy phase, opponent loses 1 Force unless 'The Asset' present at a battleground site." +
                "Your total force generation and battle destiny is +1 for each card stacked here. " +
                "During your move phase, may take Din (and Grogu if with Din) into hand from a location.");
        addIcons(Icon.MUDHORN, Icon.VIRTUAL_SET_24);
        setTestingText("Bounty Hunting Is A Dangerous Profession");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        Filter groguBobaBoCara = Filters.and(Filters.your(self), Filters.or(Filters.Grogu, Filters.Boba_Fett, Filters.Cara_Dune));

        modifiers.add(new DeployCostModifier(self, groguBobaBoCara, -1));
        modifiers.add(new TotalBattleDestinyModifier(self, new StackedEvaluator(self), self.getOwner()));
        modifiers.add(new TotalForceGenerationModifier(self, new StackedEvaluator(self), self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        PhysicalCard theAsset = Filters.findFirstActive(game, self, Filters.The_Asset);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (TriggerConditions.isEndOfOpponentsPhase(game, effectResult, Phase.DEPLOY, playerId)
                && !GameConditions.canSpot(game, self, Filters.and(Filters.The_Asset, Filters.presentAt(Filters.battleground_site)))) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make " + opponent + " lose 1 Force");
            action.setActionMsg("Make " + opponent + " lose 1 force for 'The Asset' not being present at a battleground");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        Filter din = Filters.and(Filters.your(playerId), Filters.Din, Filters.at(Filters.location));
        Filter groguWithDin = Filters.and(Filters.Grogu, Filters.with(self, Filters.Din));

        if (GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && GameConditions.canTarget(game, self, din)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);

            int numCards = GameConditions.canSpot(game, self, groguWithDin) ? 2 : 1;

            action.setText("Take card(s) into hand");

            // Perform result(s)
            action.appendTargeting(
                    new TargetCardsOnTableEffect(action, playerId, "Choose Din to take into hand", numCards, numCards, Filters.or(din, groguWithDin)) {
                        @Override
                        protected void cardsTargeted(int targetGroupId, final Collection<PhysicalCard> targetedCards) {
                            action.addAnimationGroup(targetedCards);
                            // Allow response(s)
                            action.allowResponses("Take " + GameUtils.getAppendedTextNames(targetedCards) + " into hand",
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ReturnCardsToHandFromTableSimultaneouslyEffect(action, targetedCards, true));
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
}
