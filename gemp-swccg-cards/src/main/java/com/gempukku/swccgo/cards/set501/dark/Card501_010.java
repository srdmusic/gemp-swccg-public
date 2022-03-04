package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.*;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveForfeitValueIncreasedModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Objective
 * Title: The Shield Will Be Down In Moments / Imperial Troops Have Entered The Base!
 */
public class Card501_010 extends AbstractObjective {
    public Card501_010() {
        super(Side.DARK, 0, Title.The_Shield_Will_Be_Down_In_Moments);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Hoth, [Set 17] 4th marker, 1st Marker and [Set 18] You May Start Your Landing. " +
                "For remainder of game, you may not play Sunsdown. " +
                "While this side up, Force loss from You May Start Your Landing is limited to 1. Once per turn, may deploy a snowtrooper or non-unique AT-AT to Hoth from Reserve Deck; reshuffle. When drawing for Target the Main Generator, X = 6 - the Marker Number from where you’re firing. Flip this card if Main Power Generators 'blown away.'");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
        setTestingText("The Shield Will Be Down in Moments");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
            new DeployCardFromReserveDeckEffect(action, Filters.Hoth_system, true, false) {
                @Override
                public String getChoiceText() {
                    return "Choose Hoth system to deploy";
                }
            });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_17, Filters.Fourth_Marker), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose [Set 17] 4th marker to deploy";
                    }
                });
        
        action.appendRequiredEffect(
            new DeployCardFromReserveDeckEffect(action, Filters.First_Marker, true, false) {
                @Override
                public String getChoiceText() {
                    return "Choose 1st marker to deploy";
                }
            });

        action.appendRequiredEffect(
            new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_18, Filters.You_May_Start_Your_Landing), true, false) {
                @Override
                public String getChoiceText() {
                    return "Choose [Set 18] You May Start Your Landing to deploy";
                }
            });
        return action;
    }

    @Override
    protected RequiredGameTextTriggerAction getGameTextAfterDeploymentCompletedAction(String playerId, SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
        action.appendEffect(
                new AddUntilEndOfGameModifierEffect(action,
                        new MayNotDeployModifier(self, Filters.Sunsdown, self.getOwner()), null));
        
        return action;
    }


    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Target_The_Main_Generator, ModifyGameTextType.TARGET_THE_MAIN_GENERATOR__MODIFY_X));
        modifiers.add(new LimitForceLossFromCardModifier(self, Filters.You_May_Start_Your_Landing, 1, opponent));
        modifiers.add(new LimitForceLossFromCardModifier(self, Filters.You_May_Start_Your_Landing, 1, playerId));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.THE_SHIELD_WILL_BE_DOWN__DOWNLOAD_ATAT_OR_TROOPER;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a snowtrooper or non-unique AT-AT from Reserve Deck");
            action.setActionMsg("Deploy a snowtrooper or non-unique AT-AT from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.snowtrooper, Filters.and(Filters.AT_AT, Filters.non_unique)), true));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.canBeFlipped(game, self)
                && TriggerConditions.isBlownAwayLastStep(game, effectResult, Filters.title(Title.Main_Power_Generators, true))) {

            RequiredGameTextTriggerAction action2 = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action2.setText("Flip");
            action2.setActionMsg(null);
            // Perform result(s)
            action2.appendEffect(
                    new FlipCardEffect(action2, self));
            actions.add(action2);
        }

        return actions;
    }
}