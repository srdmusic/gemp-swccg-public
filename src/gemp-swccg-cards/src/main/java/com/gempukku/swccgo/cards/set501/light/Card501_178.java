package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.PlaceJediTestOnTableWhenCompletedModifier;
import com.gempukku.swccgo.logic.modifiers.SpecialFlagModifier;

import java.util.Collections;
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
                new DeployCardFromReserveDeckEffect(action, Filters.No_Disintegrations, true, false) {
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
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MIND_WHAT_YOUR_HAVE_LEARNED_V__DOWNLOAD_EFFECT;

        // Check condition(s)
        if (GameConditions.isDuringYourPhase(game, self, Phase.DEPLOY)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Wise Advice or Yoda's Hope from Reserve Deck");
            action.setActionMsg("Deploy Wise Advice or Yoda's Hope from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Wise_Advice, Filters.Yodas_Hope), true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new SpecialFlagModifier(self, ModifierFlag.DRAW_TWO_AND_CHOOSE_ONE_FOR_TRAINING_DESTINY, playerId));
        modifiers.add(new ModifyGameTextModifier(self, Filters.and(Filters.Jedi_Test_5, Filters.completed_Jedi_Test), new TrueCondition(), ModifyGameTextType.IT_IS_THE_FUTURE_YOU_SEE__STACK_DESTINY_CARD_ON_JEDI_TEST_5));
        modifiers.add(new PlaceJediTestOnTableWhenCompletedModifier(self, Filters.any, new TrueCondition()));
        return modifiers;
    }

}