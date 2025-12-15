package com.gempukku.swccgo.cards.set7.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.conditions.PresentWithCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.PutStackedCardsInUsedPileEffect;
import com.gempukku.swccgo.logic.effects.choose.StackDestinyCardEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Special Edition
 * Type: Character
 * Subtype: Alien
 * Title: Joh Yowza
 */
public class Card7_023 extends AbstractAlien {
    public Card7_023() {
        super(Side.LIGHT, 3, 2, 2, 1, 3, Title.Joh_Yowza, Uniqueness.UNIQUE, ExpansionSet.SPECIAL_EDITION, Rarity.R);
        setLore("Yuzzum musician and thief. Singer for The Max Rebo Band. Stage name given to him by Sy Snootles. Jabba likes his performance, even though the Hutt despises Yuzzum.");
        setGameText("Power +2 on Endor or when present with your musician. When opponent draws destiny, Joh may 'jam' (place that card face down under Joh). Holds one card at a time. If Joh leaves table, place card under Joh in opponent's Used Pile.");
        addIcons(Icon.SPECIAL_EDITION);
        addKeywords(Keyword.MUSICIAN,Keyword.THIEF);
        setSpecies(Species.YUZZUM);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        Condition onEndor = new OnCondition(self, Title.Endor);
        Condition presentWithYourMusician = new PresentWithCondition(self, Filters.and(Filters.your(self), Filters.musician));
        modifiers.add(new PowerModifier(self, new OrCondition(onEndor, presentWithYourMusician), 2));
//        modifiers.add(new PowerModifier(self, new OrCondition(new OnCondition(self, Title.Endor),new PresentWithCondition(self, Filters.and(Filters.your(self), Filters.musician))), 2));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawnBy(game, effectResult, opponent)
                && !GameConditions.hasStackedCards(game, self, Filters.jamCard) //'jam' capacity is limited to 1
                && GameConditions.canStackDestinyCard(game)) {
            final PhysicalCard destinyToJam = game.getGameState().getTopOfUnresolvedDestinyDraws(opponent);

            ///may need to change some of these parameters for Bane Malar copying (use Filters.Joh_Yowza to get card instead of 'self'?)

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("'Jam' (stack) destiny");
            action.setActionMsg("'Jam' (stack) just-drawn destiny on " + GameUtils.getCardLink(self));
            // Perform result(s)
            action.appendEffect(
                    new StackDestinyCardEffect(action, self, true));
            action.appendEffect(
                    new PassthruEffect(action) {
                        @Override
                        protected void doPlayEffect(SwccgGame game) {
                            destinyToJam.setJamCard(true);
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    /// normally, use getGameTextRequiredAfterTriggers but this only works when active

    /// could try using getGameTextLeavesTableRequiredTriggers instead - probably too late and need to use isAboutToLeaveTable instead?

    /// handles if inactive or game text canceled when going off table (handle 'jam' stacked card)
    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggersAlwaysWhenInPlay(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(self.getOwner());
        //Collection<PhysicalCard> jamCard = Filters.filterStacked(game,Filters.jamCard);
        Collection<PhysicalCard> jamCard = Filters.filterStacked(game,Filters.and(Filters.stackedOn(self),Filters.jamCard));

        /// add handling incase stacked card is owned by self! (due to using with Bane Malar)

        // Check condition(s)
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, self)
                && GameConditions.hasStackedCards(game, self, Filters.jamCard)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Return stacked 'jam' card to Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PutStackedCardsInUsedPileEffect(action, opponent, jamCard, true));
            return Collections.singletonList(action);
        }
        return null;
    }

}
