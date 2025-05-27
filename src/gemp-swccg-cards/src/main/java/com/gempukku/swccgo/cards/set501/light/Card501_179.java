package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
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
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipSingleSidedStackedCard;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.StackCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayDeployAsIfFromHandModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: Patience!
 */
public class Card501_179 extends AbstractEpicEventDeployable {
    public Card501_179() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Patience, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If your [Dagobah] objective on table, deploy on table and stack Jedi Tests 1 - 5 from your Reserve Deck face up here. I Won't Fail You: You may deploy face up Jedi Tests from here as if from hand. I Saw A City In The Clouds: Once per turn, may [download] Bespin system or a Cloud City site. I've Got To Go To Them: Once per turn, if you just lost Force from a Force drain and you do not occupy a battleground, turn a Jedi Test here face down.");
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setTestingText("Patience!");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(playerId), Icon.DAGOBAH, Filters.Objective));
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();
        
        String playerId = self.getOwner();
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerId);
            // Perform result(s)
            action.appendEffect(
                new StackCardFromReserveDeckEffect(action, playerId, self, Filters.Jedi_Test_1, false, false));
            action.appendEffect(
                new StackCardFromReserveDeckEffect(action, playerId, self, Filters.Jedi_Test_2, false, false));
            action.appendEffect(
                new StackCardFromReserveDeckEffect(action, playerId, self, Filters.Jedi_Test_3, false, false));
            action.appendEffect(
                new StackCardFromReserveDeckEffect(action, playerId, self, Filters.Jedi_Test_4, false, false));
            action.appendEffect(
                new StackCardFromReserveDeckEffect(action, playerId, self, Filters.Jedi_Test_5, false, false));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        final Filter jediTestFaceUp = Filters.and(Filters.Jedi_Test, Filters.not(Filters.face_down));
        final Filter patienceWithJediTestStackedFaceUp = Filters.and(Filters.Patience, Filters.hasStacked(jediTestFaceUp));
        // Check condition(s)
        if (GameConditions.isOnceDuringOpponentsPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && TriggerConditions.justLostForce(game, effectResult, playerId)
                && !GameConditions.occupiesWith(game, self, playerId, Filters.battleground, Filters.and(Icon.CLOUD_CITY, Filters.Rebel))
                && GameConditions.canSpot(game, self, patienceWithJediTestStackedFaceUp)) {
        
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);

            action.setPerformingPlayer(playerId);
            action.setText("Turn Jedi Test face down");
            action.setActionMsg("Turn a Jedi Test on Patience! face down");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            action.appendTargeting(
                    new ChooseStackedCardEffect(action, playerId, patienceWithJediTestStackedFaceUp, jediTestFaceUp, false) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            // Perform result(s)
                            action.appendEffect(
                                new FlipSingleSidedStackedCard(action, selectedCard));
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayDeployAsIfFromHandModifier(self, Filters.and(Filters.not(Filters.face_down), Filters.Jedi_Test, Filters.stackedOn(self))));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();
        
        GameTextActionId gameTextActionId = GameTextActionId.PATIENCE__DOWNLOAD_LOCATION;

        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy location from Reserve Deck");
            action.setActionMsg("Deploy Bespin system or a Cloud City site from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Bespin_system, Filters.Cloud_City_site), true));
            actions.add(action);
        }
        return actions;
    }
}
