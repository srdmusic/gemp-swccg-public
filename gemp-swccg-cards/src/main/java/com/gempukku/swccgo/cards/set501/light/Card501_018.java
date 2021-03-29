package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.DeployAsCaptiveOption;
import com.gempukku.swccgo.game.DeploymentRestrictionsOption;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 15
 * Type: Objective
 * Title: Rescue The Princess / Sometimes I Amaze Even Myself (V)
 */
public class Card501_018 extends AbstractObjective {
    public Card501_018() {
        super(Side.LIGHT, 0, Title.Rescue_The_Princess);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Central Core, Trash Compactor, and Detention Block Corridor (with Prisoner 2187 imprisoned there)." +
                "For remainder of game, your Death Star sites generate +1 Force for you and ignore Set Your Course For Alderaan. You may not deploy Jedi (except Obi-Wan)." +
                "While this side up, once per turn you may deploy a Death Star site or A Power Loss from Reserve Deck; reshuffle." +
                "Flip this card if Leia is present at a Death Star site and A Power Loss is 'shut down.'");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        setTestingText("Rescue The Princess / Sometimes I Amaze Even Myself (V)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameState gameState = game.getGameState();

        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Death_Star_Central_Core, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Central Core to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Trash_Compactor, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Trash Compactor to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Detention_Block_Corridor, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Detention Block to deploy";
                    }
                });

        if (Filters.canSpot(gameState.getReserveDeck(playerId), game, Filters.Detention_Block_Corridor)) {
            action.appendRequiredEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.Prisoner_2187, Filters.Detention_Block_Corridor, true, DeploymentRestrictionsOption.ignoreLocationDeploymentRestrictions(), DeployAsCaptiveOption.deployAsImprisonedCaptive(), false) {
                        @Override
                        public String getChoiceText() {
                            return "Choose Prisoner 2187 to deploy";
                        }
                    });
        }

        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        Filter yourDeathStarSites = Filters.and(Filters.your(playerId), Filters.Death_Star_site);
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ForceGenerationModifier(self, yourDeathStarSites, 1, playerId));
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.Jedi, Filters.not(Filters.ObiWan)), playerId));
        modifiers.add(new ModifyGameTextModifier(self, Filters.Set_Your_Course_For_Alderaan, ModifyGameTextType.SET_YOUR_COURSE_FOR_ALDERAAN__ONLY_AFFECTS_DARK_SIDE_DEATH_STAR_SITES));
        modifiers.add(new ModifyGameTextModifier(self, Filters.I_Cant_Believe_Hes_Gone, ModifyGameTextType.I_CANT_BELIEVE_HES_GONE__ONLY_EFFECTS_BATTLES_WITH_LUKE_OR_LEIA));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.RESCUE_THE_PRINCESS__DOWNLOAD_SITE_OR_A_POWER_LOSS;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a card from Reserve Deck");
            action.appendUsage(
                    new OncePerTurnEffect(action)
            );
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Death_Star_site, Filters.A_Power_Loss), true)
            );

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        if (GameConditions.canBeFlipped(game, self)
                && GameConditions.canSpot(game, self, Filters.and(Filters.Leia, Filters.presentAt(Filters.Death_Star_site)))
                && GameConditions.canSpot(game, self, Filters.and(Filters.A_Power_Loss))) {
            PhysicalCard aPowerLoss = Filters.findFirstActive(game, self, Filters.A_Power_Loss);
            if (aPowerLoss != null && !GameConditions.cardHasWhileInPlayDataSet(aPowerLoss)) {
                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Flip");
                action.appendEffect(
                        new FlipCardEffect(action, self)
                );
                actions.add(action);
            }
        }

        return actions;
    }
}
