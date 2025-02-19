package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotCancelBattleDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Republic
 * Title: Tey How (V)
 */
public class Card501_049 extends AbstractRepublic {
    public Card501_049() {
        super(Side.DARK, 2, 2, 2, 2, 4, "Tey How", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Neimoidian Trade Federation communications officer to Nute Gunray. Had audio and visual mechanics surgically implanted to assist her in shipboard operations.");
        setGameText("[Pilot] 2. Once per game, may [download] a device on a capital starship she is piloting. While piloting a [Trade Federation] capital starship, opponent may not cancel your battle destiny draws where you have a character with 'Trade Federation' in lore.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.PILOT, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.FEMALE);
        setSpecies(Species.NEIMOIDIAN);
        setVirtualSuffix(true);
        setTestingText("Tey How (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Filter TradeCharFilter = Filters.and(Filters.your(self), Filters.character, Filters.loreContains("Trade Federation"));
        Filter TradeCapFilter = Filters.and(Icon.TRADE_FEDERATION, Filters.capital_starship);
        Condition PilotingTradeCapCondition = new PilotingCondition(self, TradeCapFilter);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new MayNotCancelBattleDestinyModifier(self, Filters.sameLocationAs(self, TradeCharFilter), playerId, PilotingTradeCapCondition, opponent));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.TEY_HOW__DOWNLOAD_DEVICE;
        Filter CapitalStarshipTeyHowIsPiloting = Filters.and(Filters.capital_starship, Filters.hasPiloting(self));

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.isDuringYourPhase(game, self, Phase.DEPLOY)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canTarget(game, self, CapitalStarshipTeyHowIsPiloting)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy device from Reserve Deck");
            action.setActionMsg("Deploy a device on " + GameUtils.getCardLink(self) + "'s starship from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.device, CapitalStarshipTeyHowIsPiloting, true));
            actions.add(action);
        }
        return actions;
    }
}
