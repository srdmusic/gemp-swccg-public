package com.gempukku.swccgo.cards.set501.light;

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
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameActionProxyEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.LoseCardsFromTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.JediTestSuspendedInsteadOfLostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.modifiers.PlaceJediTestOnTableWhenCompletedModifier;
import com.gempukku.swccgo.logic.modifiers.SpecialFlagModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: Mind What You Have Learned / Save You It Can (V)
 */
public class Card501_178 extends AbstractObjective {
    public Card501_178() {
        super(Side.LIGHT, 0, Title.Mind_What_You_Have_Learned, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy Beldon's Gallery, Yoda's Hut, [Cloud City] No Disintegrations!, and Patience! For remainder of game, your non-[Dagobah] characters of ability > 4 (except Ahsoka) are lost. Place completed Jedi Tests on table; they are suspended (not lost) while Luke not on table. [Dagobah] Yoda deploys -4 to Dagobah. While this side up, may [download] Wise Advice or Yoda's Hope. Once per turn, may [download] a Dagobah site. When drawing training destiny, draw two and choose one. May flip this card if Luke on Dagobah during your turn.");
        addIcons(Icon.SPECIAL_EDITION, Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Mind What You Have Learned (V)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Beldons_Gallery, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Beldon's Gallery to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Yodas_Hut, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Yoda's Hut to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.No_Disintegrations, Filters.any), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose No Disintegrations! to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Patience, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Patience! to deploy";
                    }
                });
        return action;
    }

    @Override
    protected RequiredGameTextTriggerAction getGameTextAfterDeploymentCompletedAction(String playerId, SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        final int permCardId = self.getPermanentCardId();

        RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);

        action.appendEffect(
                new AddUntilEndOfGameActionProxyEffect(action,
                        new AbstractActionProxy() {
                            @Override
                            public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                List<TriggerAction> actions = new LinkedList<TriggerAction>();
                                PhysicalCard self = game.findCardByPermanentId(permCardId);

                                // Check condition(s)
                                if (TriggerConditions.isTableChanged(game, effectResult)) {
                                    Collection<PhysicalCard> disallowedCharacters = Filters.filterActive(game, self, Filters.and(Filters.your(playerId), Filters.not(Icon.DAGOBAH), Filters.abilityMoreThan(4), Filters.not(Filters.Ahsoka)));
                                    if (!disallowedCharacters.isEmpty()) {

                                        final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                                        action.setSingletonTrigger(true);
                                        action.setText("Make character lost");
                                        action.setActionMsg("Make " + GameUtils.getAppendedNames(disallowedCharacters) + " lost");
                                        // Perform result(s)
                                        action.appendEffect(
                                                new LoseCardsFromTableEffect(action, disallowedCharacters));
                                        actions.add(action);
                                    }
                                }
                                return actions;
                            }
                        }
                ));
        return action;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();
        
        GameTextActionId gameTextActionId = GameTextActionId.MIND_WHAT_YOUR_HAVE_LEARNED_V__DOWNLOAD_EFFECT;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Arrays.asList(Title.Wise_Advice, Title.Yodas_Hope))) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Wise Advice or Yoda's Hope from Reserve Deck");
            action.setActionMsg("Deploy Wise Advice or Yoda's Hope from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Wise_Advice, Filters.Yodas_Hope), true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.MIND_WHAT_YOUR_HAVE_LEARNED_V__DOWNLOAD_SITE;
        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Dagobah site from Reserve Deck");
            action.setActionMsg("Deploy Dagobah site from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Wise_Advice, Filters.Yodas_Hope), true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        // Check condition(s)
        if (GameConditions.canBeFlipped(game, self)
                && GameConditions.canTarget(game, self, Filters.and(Filters.Luke, Filters.On_Dagobah))
                && GameConditions.isDuringYourTurn(game, playerId)){

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Flip");
            action.setActionMsg("Flip " + GameUtils.getCardLink(self));
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PlaceJediTestOnTableWhenCompletedModifier(self, Filters.any, new TrueCondition()));
        modifiers.add(new JediTestSuspendedInsteadOfLostModifier(self, Filters.any, new TrueCondition()));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Icon.DAGOBAH, Filters.Yoda), -4, Filters.Dagobah_location));

        modifiers.add(new SpecialFlagModifier(self, ModifierFlag.DRAW_TWO_AND_CHOOSE_ONE_FOR_TRAINING_DESTINY, playerId));
        return modifiers;
    }

}