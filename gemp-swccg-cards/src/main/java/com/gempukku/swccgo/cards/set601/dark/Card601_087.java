package com.gempukku.swccgo.cards.set601.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.evaluators.MultiplyEvaluator;
import com.gempukku.swccgo.cards.evaluators.PresentWhereAffectedCardIsAtEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Block 4
 * Type: Objective
 * Title: Hunt Down And Destroy The Jedi / Their Fire Has Gone Out Of The Universe (V)
 */
public class Card601_087 extends AbstractObjective {
    public Card601_087() {
        super(Side.DARK, 0, Title.Hunt_Down_And_Destroy_The_Jedi);
        setFrontOfDoubleSidedCard(true);
        setVirtualSuffix(true);
        setGameText("Deploy Coruscant system, Imperial City, and A Sith's Plans.  May deploy If The Trace Was Correct.\n" +
                "For remainder of game, you may not deploy [Episode I] Dark Jedi.  Whenever a character hit by Galen's Lightsaber or Vader's Lightsaber leaves table, opponent loses 2 Force.\n" +
                "While this side up, may take Rogue Shadow into hand from Reserve Deck; reshuffle.  Galen's immunity to attrition is +2 for each Jedi present.\n" +
                "Flip this card if Galen or Vader at a battleground site and opponent does not have a unique (*) character of ability > 3 present at a battleground site.");
        addIcons(Icon.SPECIAL_EDITION, Icon.LEGACY_BLOCK_4);
        setAsLegacy(true);
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Coruscant_system, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Coruscant system to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Imperial_City, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose Imperial City to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.title(Title.A_Siths_Plans), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose A Sith's Plans to deploy";
                    }
                });
        action.appendOptionalEffect(
                new DeployCardsFromReserveDeckEffect(action, Filters.If_The_Trace_Was_Correct, 0, 1, true, false) {
                    public String getChoiceText(int numCardsToChoose) {
                        return "Choose If The Trace Was Correct to deploy";
                    }
                });
        return action;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.LEGACY__HUNT_DOWN_V__UPLOAD_ROGUES_SHADOW;

        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Take Rogue's Shadow into hand from Reserve Deck");
            action.setActionMsg("Take Rogue's Shadow into hand from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.title("Rogue's Shadow"), true));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotPlayModifier(self, Filters.and(Icon.EPISODE_I, Filters.Dark_Jedi), self.getOwner()));
        modifiers.add(new ImmunityToAttritionChangeModifier(self, Filters.Galen, new TrueCondition(), new MultiplyEvaluator(2, new PresentWhereAffectedCardIsAtEvaluator(self, Filters.Jedi))));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.and(Filters.or(Filters.Galen, Filters.Vader), Filters.at(Filters.battleground_site)))
                && !GameConditions.canSpot(game, self, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.and(Filters.opponents(self), Filters.unique, Filters.character, Filters.abilityMoreThan(3), Filters.at(Filters.battleground_site)))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }

        //TODO Whenever a character hit by Galen's Lightsaber or Vader's Lightsaber leaves table, opponent loses 2 Force.
        return actions;
    }
}
