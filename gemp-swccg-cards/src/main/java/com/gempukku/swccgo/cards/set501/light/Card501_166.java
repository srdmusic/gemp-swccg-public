package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.TwicePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 9
 * Type: Effect
 * Title: A Brave Resistance
 */
public class Card501_166 extends AbstractNormalEffect {
    protected Card501_166() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "A Brave Resistance", Uniqueness.UNIQUE, ExpansionSet.SET_9, Rarity.V);
        setLore("");
        setGameText("If Jakku on table, deploy on table. " +
                "Unless Strike Planning on table, twice per game may take a Resistance leader from Reserve Deck; reshuffle. " +
                "Resistance characters of printed forfeit < 6 are forfeit +1. " +
                "Where you have a Resistance Agent your total power is +3. [Immune to Alter].");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_9);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("A Brave Resistance (ERRATA)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Jakku_system);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.Resistance_character, Filters.printedForfeitValueLessThan(6)), 1));
        modifiers.add(new TotalPowerModifier(self, Filters.sameLocationAs(self, Filters.Resistance_Agent), 3, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.A_BRAVE_RESISTANCE__UPLOAD_RESISTANCE_CHARACTER;

        // Check condition(s)
        if (GameConditions.isTwicePerGame(game, self, gameTextActionId)
                && !GameConditions.canSpot(game, self, Filters.title(Title.Strike_Planning))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take card into hand from Reserve Deck");
            action.setActionMsg("Take a Resistance leader into hand from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new TwicePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.Resistance_leader, true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
