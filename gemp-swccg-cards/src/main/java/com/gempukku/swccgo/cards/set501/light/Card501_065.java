package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToSystemFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 20
 * Type: Objective
 * Title: Hunt For The Droid General / Grievous Will Run And Hide
 */
public class Card501_065 extends AbstractObjective {
    public Card501_065() {
        super(Side.LIGHT, 0, "Hunt For The Droid General");
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy a [Clone Army] battleground and ♢Clone Command Center (to same system). Deploy He Is A Coward. \n" +
                "For remainder of game, you may not deploy non-[Episode I] Jedi. Your non-[Episode I] cards with ability are deploy +2. Jedi gain [Pilot] skill. Your [Episode I] sites are immune to No Escape. May lose 1 Force to cancel You Are Beaten. At end of opponent's turn, if you occupy more battlegrounds than opponent, opponent loses 1 Force. \n" +
                "Flip this card if He Is A Coward here (unless Grievous present at a battleground).");
        addIcons(Icon.CLONE_ARMY, Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        setTestingText("Hunt For The Droid General");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final Filter locationFilter = Filters.and(Icon.CLONE_ARMY, Filters.battleground, Filters.location);
        final ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, locationFilter, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Chose a [Clone Army] battleground to deploy";
                    }
                    @Override
                    protected void cardDeployed(PhysicalCard card) {
                        String systemName = card.getBlueprint().getSystemName();

                        action.appendRequiredEffect(
                                new DeployCardToSystemFromReserveDeckEffect(action, Filters.titleContains("Clone Command Center"), systemName, true, false) {
                                    @Override
                                    public String getChoiceText() {
                                        return "Choose Clone Command Center to deploy";
                                    }
                                });
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.title("He Is A Coward"), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Deploy He Is A Coward";
                    }

                });
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.not(Icon.EPISODE_I), Filters.Jedi), self.getOwner()));
        modifiers.add(new DeployCostModifier(self, Filters.and(Filters.your(self), Filters.not(Icon.EPISODE_I), Filters.hasAbilityOrHasPermanentPilotWithAbility), 2));
        modifiers.add(new IconModifier(self, Filters.and(Filters.your(self), Filters.Jedi), Icon.PILOT));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.your(self), Icon.EPISODE_I, Filters.site), Title.No_Escape));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.You_Are_Beaten)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.You_Are_Beaten, Title.You_Are_Beaten);
            action.appendCost(new LoseForceEffect(action, playerId, 1, true));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.You_Are_Beaten)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.appendCost(new LoseForceEffect(action, playerId, 1, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if (TriggerConditions.isEndOfOpponentsTurn(game, effectResult, playerId)) {
            int battlegroundsYouOccupy = Filters.filterTopLocationsOnTable(game, Filters.occupies(playerId)).size();
            int battlegroundsOpponentOccupies = Filters.filterTopLocationsOnTable(game, Filters.occupies(opponent)).size();

            if (battlegroundsYouOccupy > battlegroundsOpponentOccupies) {
                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText(opponent + " loses 1 Force");
                action.appendEffect(
                        new LoseForceEffect(action, opponent, 1));
                actions.add(action);
            }
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.hasAttached(game, self, Filters.title("He Is A Coward"))
                && !GameConditions.canSpot(game, self, Filters.and(Filters.Grievous, Filters.presentAt(Filters.battleground)))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }
        return actions;
    }
}