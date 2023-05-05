package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.StackedEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.choose.StackOneCardFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeFiredModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Refuge On The Outskirts
 */
public class Card501_125 extends AbstractNormalEffect {
    public Card501_125() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Refuge On The Outskirts", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setGameText("If Credits Will Do Fine on table, deploy on table. While no 'credits' stacked, lightsabers may not fire at Watto's Junkyard. [Tatooine] or [Coruscant] Qui-Gon is power +1 for each 'credit.' [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Refuge On The Outskirts");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Icon.VIRTUAL_SET_21, Filters.Credits_Will_Do_Fine));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotBeFiredModifier(self, Filters.and(Filters.lightsaber, Filters.at(Filters.Wattos_Junkyard)), new NotCondition(new OnTableCondition(self, Filters.or(Filters.creditCard, Filters.hasStacked(Filters.creditCard))))));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.or(Icon.TATOOINE, Icon.CORUSCANT), Filters.QuiGon), new StackedEvaluator(self, Filters.any, Filters.creditCard)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, Phase.DRAW)
                && GameConditions.canTarget(game, self, Filters.and(Filters.ObiWan, Filters.at(Filters.City_Outskirts)))) {

            PhysicalCard credits = Filters.findFirstActive(game, self, Filters.Credits_Will_Do_Fine);
            if (credits != null) {

                PhysicalCard topOfLostPile = game.getGameState().getTopOfLostPile(playerId);
                if (topOfLostPile != null) {

                    final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
                    action.setText("Stack top card from Lost Pile");
                    action.setActionMsg("Stack top card of Lost Pile on " + GameUtils.getCardLink(credits));
                    // Update usage limit(s)
                    action.appendUsage(
                            new OncePerPhaseEffect(action));
                    // Perform result(s)
                    action.appendEffect(
                            new StackOneCardFromLostPileEffect(action, topOfLostPile, credits, true, false, false));
                    actions.add(action);
                }
            }

        }
        return actions;
    }
}