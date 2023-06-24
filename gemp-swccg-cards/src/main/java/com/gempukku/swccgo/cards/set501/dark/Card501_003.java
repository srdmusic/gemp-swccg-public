package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: The Shield Will Be Down In Moments / Imperial Troops Have Entered The Base!
 */
public class Card501_003 extends AbstractObjective {
    public Card501_003() {
        super(Side.DARK, 0, Title.The_Shield_Will_Be_Down_In_Moments, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Hoth system, [Set 17] 4th Marker, 1st Marker, and [Set 21] You May Start Your Landing. " +
                "For remainder of game, you may not play Sunsdown. Trample is a Lost Interrupt. X on Target The Main Generator is -2 (unless firing at or below the 3rd Marker) and maximum X = 3. " +
                "While this side up, once per turn, may deploy an exterior Hoth site from Reserve Deck; reshuffle. " +
                "Flip this card if Main Power Generators has been 'blown away' and you occupy Hoth system.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_22);
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
            new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_21, Filters.You_May_Start_Your_Landing), true, false) {
                @Override
                public String getChoiceText() {
                    return "Choose [Set 21] You May Start Your Landing to deploy";
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

        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a Hoth site from Reserve Deck");
            action.setActionMsg("Deploy an exterior Hoth site from Reserve Deck");

            action.appendUsage(
                    new OncePerTurnEffect(action));
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.Hoth_site, Filters.exterior_site), true));

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