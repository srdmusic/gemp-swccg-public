package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddToAttritionEffect;
import com.gempukku.swccgo.logic.effects.choose.PlaceCardOutOfPlayFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyWhenDrawnForDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: TD-110
 */

public class Card501_140 extends AbstractImperial {
    public Card501_140() {
        super(Side.DARK, 3, 2, 2, 2, 3, "TD-110", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setArmor(4);
        setLore("Sandtrooper. Leader.");
        setGameText("Destiny +2 if drawn for destiny if Send a Detachment Down on table.  While in battle at a Tatooine site, may place Tactical Support out of play from lost pule to add 2 to your total attrition.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.SANDTROOPER, Keyword.LEADER);
        setTestingText("TD-110");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DestinyWhenDrawnForDestinyModifier(self, new OnTableCondition(self, Filters.title(Title.Send_A_Detachment_Down)), 2));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.TD_110__MODIFY_ATTRITION;
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId)
            && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
            && GameConditions.isInBattleAt(game, self, Filters.Tatooine_site)
            && GameConditions.canModifyAttritionAgainst(game, opponent)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Place Tactial Support in Lost Pile out of play");
            action.setActionMsg("Add 2 to your total attrition");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));
            // Pay cost(s)
            action.appendCost(
                    new PlaceCardOutOfPlayFromLostPileEffect(action, playerId, playerId, Filters.Tactical_Support, false));
            // Perform result(s)
            action.appendEffect(
                    new AddToAttritionEffect(action, opponent, 2));
            return Collections.singletonList(action);
        }
        return null;
    }

}
