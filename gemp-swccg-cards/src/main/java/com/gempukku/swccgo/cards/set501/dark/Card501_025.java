package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
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
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Character
 * Subtype: Republic
 * Title: San Hill
 */
public class Card501_025 extends AbstractRepublic {
    public Card501_025() {
        super(Side.DARK, 2, 3, 3, 3, 5, "San Hill", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Muun leader. Banking Clan.");
        setGameText("At same site, your battle droids deploy -1 and are forfeit +1. Once per game, may deploy Grievous here from Reserve Deck; reshuffle. While on Utapau and The Galaxy Torn Apart on table, opponent's Force retrieval is canceled.");
        addKeywords(Keyword.LEADER);
        setSpecies(Species.MUUN);
        addIcons(Icon.EPISODE_I, Icon.SEPARATIST, Icon.PILOT, Icon.VIRTUAL_SET_21);
        setTestingText("San Hill");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self,  Filters.battle_droid, -1, Filters.here(self)));
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.your(self), Filters.battle_droid, Filters.here(self)), 1));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.SAN_HILL__DEPLOY_GRIEVOUS_FROM_RESERVE_DECK;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Grievous from Reserve Deck");
            action.setActionMsg("Deploy Grievous here from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.Grievous, Filters.here(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isAboutToRetrieveForce(game, effectResult, game.getOpponent(self.getOwner()))
                && GameConditions.isOnSystem(game, self, Title.Utapau)
                && GameConditions.canSpot(game, self, Filters.title("The Galaxy Torn Apart"))) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Cancel retrieval");
            action.setActionMsg("Force retrieval is canceled");
            action.appendEffect(
                    new CancelForceRetrievalEffect(action)
            );
            return Collections.singletonList(action);
        }

        return null;
    }
}
