package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
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
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.InitiateBattlesForFreeModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDrawMoreThanBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Republic
 * Title: Poggle The Lesser
 */
public class Card501_022 extends AbstractRepublic {
    public Card501_022() {
        super(Side.DARK, 2, 3, 4, 2, 5, "Poggle The Lesser", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Geonosian leader.");
        setGameText("While with a battle droid, opponent may not draw more than one battle destiny here. Once per game, may deploy Dooku here from Reserve Deck; reshuffle. While on Geonosis and The Galaxy Torn Apart on table, you initiate battles for free.");
        addKeywords(Keyword.LEADER);
        setSpecies(Species.GEONOSIAN);
        addIcons(Icon.EPISODE_I, Icon.SEPARATIST, Icon.PILOT, Icon.VIRTUAL_SET_24);
        setTestingText("Poggle The Lesser");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDrawMoreThanBattleDestinyModifier(self, Filters.here(self), new WithCondition(self, Filters.battle_droid), 1, game.getOpponent(self.getOwner())));
        modifiers.add(new InitiateBattlesForFreeModifier(self, Filters.any, new AndCondition(new OnCondition(self, Title.Geonosis), new OnTableCondition(self, Filters.title("The Galaxy Torn Apart"))), self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.POGGLE_THE_LESSER__DEPLOY_DOOKU_FROM_RESERVE_DECK;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Dooku from Reserve Deck");
            action.setActionMsg("Deploy Dooku here from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.Dooku, Filters.here(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
