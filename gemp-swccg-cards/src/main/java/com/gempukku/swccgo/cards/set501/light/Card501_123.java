package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PlayCardOption;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PutStackedCardsInLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.StandardEffect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Character
 * Subtype: Alien
 * Title: Anakin Skywalker, Junkyard Slave
 */
public class Card501_123 extends AbstractAlien {
    public Card501_123() {
        super(Side.LIGHT, 6, 0, 2, 4, 6, "Anakin Skywalker, Junkyard Slave", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Mechanic.");
        setGameText("Adds 2 to power of anything he pilots. To deploy, place a ‘credit’ card in owner's Lost Pile. Deploys only to Watto’s Junkyard. May place a ‘credit’ card in owner's Lost pile to add one battle destiny at same [Episode I] Tatooine site (except Watto’s Junkyard).");
        addPersona(Persona.ANAKIN);
        addIcons(Icon.EPISODE_I, Icon.PILOT, Icon.TATOOINE, Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.SLAVE);
        setMatchingStarshipFilter(Filters.Azure_Angel);
        setTestingText("Anakin Skywalker, Junkyard Slave");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.hasStacked(Filters.creditCard));
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Wattos_Junkyard;
    }

    @Override
    protected StandardEffect getGameTextSpecialDeployCostEffect(Action action, String playerId, SwccgGame game, PhysicalCard self, PhysicalCard target, PlayCardOption playCardOption) {
        PhysicalCard credits = Filters.findFirstActive(game, self, Filters.hasStacked(Filters.creditCard));
        if (credits == null)
            return null; // this shouldn't happen
        StandardEffect effect = new PutStackedCardsInLostPileEffect(action, playerId, 1,1, credits);
        return effect;
    }


    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canAddBattleDestinyDraws(game, self)
                && GameConditions.isDuringBattleWithParticipant(game, self)
                && GameConditions.isDuringBattleAt(game, Filters.and(Icon.EPISODE_I, Filters.Tatooine_site, Filters.not(Filters.Wattos_Junkyard)))
                && GameConditions.canSpot(game, self, Filters.hasStacked(Filters.creditCard))) {

            PhysicalCard credits = Filters.findFirstActive(game, self, Filters.hasStacked(Filters.creditCard));
            if (credits != null) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Add one battle destiny");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerBattleEffect(action));
                // Pay Costs
                action.appendCost(
                        new PutStackedCardsInLostPileEffect(action, playerId, 1, 1, credits));

                action.appendEffect(
                        new AddBattleDestinyEffect(action, 1));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
