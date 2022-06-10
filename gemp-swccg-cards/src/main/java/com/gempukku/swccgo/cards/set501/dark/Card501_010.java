package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Objective
 * Title: The Shield Will Be Down In Moments / Imperial Troops Have Entered The Base!
 */
public class Card501_010 extends AbstractObjective {
    public Card501_010() {
        super(Side.DARK, 0, Title.The_Shield_Will_Be_Down_In_Moments);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Hoth system, [Set 17] 4th marker, 1st Marker, and [Set 18] You May Start Your Landing. " +
                "For remainder of game, you may not play Sunsdown. Trample is a lost interrupt. When playing Target The Main Generator, X is limited to 3 (and is -2 unless firing at or below the 3rd Marker). " +
                "While this side up, once per turn, may deploy an exterior Hoth site from Reserve Deck; reshuffle." +
                "Flip this card if Main Power Generators has been 'blown away' and you occupy Hoth system.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_19);
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
            new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_19, Filters.You_May_Start_Your_Landing), true, false) {
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
        action.appendEffect(
                new AddUntilEndOfGameModifierEffect(action,
                        new LostInterruptModifier(self, Filters.Trample), null));
        action.appendEffect(
                new AddUntilEndOfGameModifierEffect(action,
                    new ModifyGameTextModifier(self, Filters.Target_The_Main_Generator, ModifyGameTextType.TARGET_THE_MAIN_GENERATOR__MODIFY_X), null));
        return action;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.THE_SHIELD_WILL_BE_DOWN__DOWNLOAD_HOTH_SITE;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a Hoth site from Reserve Deck");
            action.setActionMsg("Deploy a Hoth site from Reserve Deck");

            action.appendUsage(
                    new OncePerPhaseEffect(action));
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Hoth_site, Filters.exterior_site, true));

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
                && (
                    TriggerConditions.isBlownAwayLastStep(game, effectResult, Filters.title(Title.Main_Power_Generators, true)) ||
                    GameConditions.isBlownAway(game, Filters.title(Title.Main_Power_Generators, true))
                )
                && GameConditions.occupies(game, playerId, Filters.Hoth_system)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
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