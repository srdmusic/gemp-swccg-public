package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardCombinationFromCardPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.FailCostEffect;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.ChoiceMadeResult;

import java.util.*;

/**
 * Set: Set 17
 * Type: Objective
 * Title: At Last The Jedi Are No More / Revenge Of The Sith
 */
public class Card501_095 extends AbstractObjective {
    public Card501_095() {
        super(Side.DARK, 0, Title.At_Last_The_Jedi_Are_No_More);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy a battleground location and choose an apprentice: " +
                "Maul: Deploy Desert Landing Site. " +
                "Dooku: Deploy Invisble Hand: Bridge. " +
                "Vader: Deploy Vader's Castle. " +
                "For remainder of game, you may not deploy Dark Jedi except [Episode I] Sidious and the chosen apprentice. Your [Episode I] Sidious and the chosen apprentice gain [Sith]. " +
                "Flip this card if a Dark Jedi on table.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("At Last The Jedi Are No More (ERRATA)");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.battleground, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Choose a battleground to deploy";
                    }
                });

        final String MAUL = "Maul";
        final String DOOKU = "Dooku";
        final String VADER = "Vader";
        final String NO_VALID_CHOICE = "No valid choice";

        List<PhysicalCard> reserveDeck = game.getGameState().getReserveDeck(self.getOwner());
        List<String> possible = new LinkedList<>();
        if (!Filters.filter(reserveDeck, game, Filters.Desert_Landing_Site).isEmpty()) {
            possible.add(MAUL);
        }
        if (!Filters.filter(reserveDeck, game, Filters.Invisible_Hand_Bridge).isEmpty()) {
            possible.add(DOOKU);
        }
        if (!Filters.filter(reserveDeck, game, Filters.Vaders_Castle).isEmpty()) {
            possible.add(VADER);
        }
        if (possible.size() == 0)
            possible.add(NO_VALID_CHOICE);


        String[] possibleResults = possible.toArray(new String[0]);

        action.appendRequiredEffect(
                new PlayoutDecisionEffect(action, playerId, new MultipleChoiceAwaitingDecision("Choose an option", possibleResults) {
                    @Override
                    protected void validDecisionMade(int index, final String result) {
                        Filter siteFilter = null;
                        Filter apprenticeFilter = null;

                        switch (result) {
                            case MAUL:
                                siteFilter = Filters.Desert_Landing_Site;
                                apprenticeFilter = Filters.Maul;
                                break;
                            case DOOKU:
                                siteFilter = Filters.Invisible_Hand_Bridge;
                                apprenticeFilter = Filters.Dooku;
                                break;
                            case VADER:
                                siteFilter = Filters.Vaders_Castle;
                                apprenticeFilter = Filters.Vader;
                                break;
                            case NO_VALID_CHOICE:
                                action.appendRequiredEffect(new FailCostEffect(action));
                                return;
                        }
                        action.appendRequiredEffect(
                                new DeployCardFromReserveDeckEffect(action, siteFilter, false)
                        );
                        action.appendEffect(
                                new AddUntilEndOfGameModifierEffect(action,
                                        new KeywordModifier(self, apprenticeFilter, Keyword.SITH_APPRENTICE), " chooses " + result + " as the apprentice")
                        );
                        action.appendEffect(
                                new SetWhileInPlayDataEffect(action, self, new WhileInPlayData(result))
                        );
                    }
                })
        );

        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayNotPlayModifier(self, Filters.and(Filters.Dark_Jedi, Filters.except(Filters.and(Icon.EPISODE_I, Filters.Sidious)), Filters.except(Filters.Sith_Apprentice)), self.getOwner()));
        modifiers.add(new AddCardTypeModifier(self, Filters.or(Filters.and(Filters.your(self), Icon.EPISODE_I, Filters.Sidious), Filters.Sith_Apprentice), CardType.SITH));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.canSpot(game, self, Filters.Dark_Jedi)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);

        }
        return null;
    }


    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (self.getWhileInPlayData() == null)
            return null;

        return "Chosen Apprentice is " + self.getWhileInPlayData().getTextValue();
    }
}