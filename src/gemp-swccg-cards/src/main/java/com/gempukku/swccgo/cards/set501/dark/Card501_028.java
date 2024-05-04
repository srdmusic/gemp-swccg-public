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
import com.gempukku.swccgo.logic.modifiers.AddsBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifiersMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeReducedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Republic
 * Title: General Whorm Loathsom
 */
public class Card501_028 extends AbstractRepublic {
    public Card501_028() {
        super(Side.DARK, 2, 3, 3, 3, 5, "General Whorm Loathsom", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Kerkoiden leader.");
        setGameText("While with a battle droid, adds one battle destiny. Once per game, may deploy Ventress here from Reserve Deck; reshuffle. While on Christophsis and The Galaxy Torn Apart on table, your Force drains (and Force drain bonuses) may not be canceled or reduced.");
        addKeywords(Keyword.LEADER, Keyword.GENERAL);
        setSpecies(Species.KERKOIDEN);
        addIcons(Icon.EPISODE_I, Icon.WARRIOR, Icon.PILOT, Icon.SEPARATIST, Icon.VIRTUAL_SET_24);
        setTestingText("General Whorm Loathsom");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsBattleDestinyModifier(self, new WithCondition(self, Filters.battle_droid), 1, self.getOwner()));
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, Filters.location, new AndCondition(new OnCondition(self, Title.Christophsis), new OnTableCondition(self, Filters.title("The Galaxy Torn Apart"))), null, self.getOwner()));
        modifiers.add(new ForceDrainsMayNotBeReducedModifier(self, Filters.location, new AndCondition(new OnCondition(self, Title.Christophsis), new OnTableCondition(self, Filters.title("The Galaxy Torn Apart"))), null, self.getOwner()));
        modifiers.add(new ForceDrainModifiersMayNotBeCanceledModifier(self, new AndCondition(new OnCondition(self, Title.Christophsis), new OnTableCondition(self, Filters.title("The Galaxy Torn Apart"))), Filters.your(self)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.GENERAL_WHORM_LOATHSOM__DEPLOY_VENTRESS_FROM_RESERVE_DECK;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Ventress from Reserve Deck");
            action.setActionMsg("Deploy Ventress here from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.Ventress, Filters.here(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
