package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
import com.gempukku.swccgo.cards.effects.SatisfyAllBattleDamageEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.ForfeitCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.AboutToRetrieveForceResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Character
 * SubType: Alien
 * Title: Qi'ra, Top Lieutenant
 */
public class Card501_043 extends AbstractAlien {
    public Card501_043() {
        super(Side.DARK, 2, 3, 3, 4, 4, "Qi'ra, Top Lieutenant", Uniqueness.UNIQUE);
        setLore("Female Crimson Dawn leader. Corellian gangster.");
        setGameText("When forfeited at same location as Han or Vos, may satisfy all remaining battle damage against you. Unless opponent occupies a battleground site, cancels It Could Be Worse and when opponent retrieves X cards, opponent must first use X Force or that retrieval is canceled.");
        addPersona(Persona.QIRA);
        setSpecies(Species.CORELLIAN);
        addKeywords(Keyword.FEMALE, Keyword.LEADER, Keyword.GANGSTER);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_17);
        setTestingText("Qi'ra, Top Lieutenant");
    }

    @Override
    public final boolean hasSpecialDefenseValueAttribute() {
        return true;
    }

    @Override
    public final float getSpecialDefenseValue() {
        return 5;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (!GameConditions.occupies(game, game.getOpponent(self.getOwner()), Filters.battleground_site)
                && TriggerConditions.isPlayingCard(game, effect, Filters.It_Could_Be_Worse)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)
                && GameConditions.isDuringBattleAt(game, Filters.here(self))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        final String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (!GameConditions.occupies(game, opponent, Filters.battleground_site)
                && TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canTargetToCancel(game, self, Filters.It_Could_Be_Worse)
                && GameConditions.isDuringBattleAt(game, Filters.here(self))) {


            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.It_Could_Be_Worse, Title.It_Could_Be_Worse);
            actions.add(action);
        }

        // Check condition(s)
        if (TriggerConditions.isAboutToRetrieveForce(game, effectResult, opponent)) {
            AboutToRetrieveForceResult result = (AboutToRetrieveForceResult) effectResult;

            final float amountOfForce = result.getAmountOfForceToRetrieve();

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Use Force or cancel retrieval");
            action.setActionMsg("Make " + opponent + " use " + GuiUtils.formatAsString(amountOfForce) + " Force or Force retrieval is cancel");
            if (GameConditions.canUseForce(game, opponent, amountOfForce)) {
                // Ask player to Use Force or retrieval is canceled
                action.appendEffect(
                        new PlayoutDecisionEffect(action, opponent,
                                new YesNoDecision("Do you want to use " + GuiUtils.formatAsString(amountOfForce) + " Force to proceed with Force retrieval?") {
                                    @Override
                                    protected void yes() {
                                        action.appendEffect(
                                                new UseForceEffect(action, opponent, amountOfForce));
                                    }

                                    @Override
                                    protected void no() {
                                        action.appendEffect(
                                                new CancelForceRetrievalEffect(action));
                                    }
                                }
                        )
                );
            } else {
                action.appendEffect(
                        new CancelForceRetrievalEffect(action));
            }
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, final EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isResolvingBattleDamageAndAttrition(game, effectResult, playerId)
                && GameConditions.canForfeitToSatisfyBattleDamage(game, playerId, self)
                && GameConditions.isInBattleWith(game, self, Filters.or(Filters.Han, Filters.Vos))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Forfeit to satisfy all battle damage");
            // Pay cost(s)
            action.appendCost(
                    new ForfeitCardFromTableEffect(action, self));
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new SatisfyAllBattleDamageEffect(action, playerId));
            return Collections.singletonList(action);
        }
        return null;
    }
}
