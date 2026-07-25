package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
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
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Return Of The Jedi
 */
public class Card501_212 extends AbstractNormalEffect {
    public Card501_212() {
        super(Side.LIGHT, 6, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Return Of The Jedi", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'I know there is good in you. The Emperor hasn't driven it from you fully.'");
        setGameText("If [Death Star II] Anakin 'communing,' deploy on table. Opponent loses 1 Force at the end of every turn (if you occupy more battleground sites than them), whenever they lose a character with ability in a battle you initiated (limit once per turn), and whenever they lose a battle. [Immune to Alter.]");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Return Of The Jedi");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return game.getModifiersQuerying().isCommuning(game.getGameState(), Filters.and(Icon.DEATH_STAR_II, Filters.Anakin));
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        //OTHER_CARD_ACTION_1: Opponent loses 1 Force at the end of every turn (if you occupy more battlegrounds than them)
        //OTHER_CARD_ACTION_2: Opponent loses 1 Force whenever they lose a character with ability during battle (limit once per battle)
        //OTHER_CARD_ACTION_3: Opponent loses 1 Force whenever they lose a battle

        //Opponent loses 1 Force at the end of every turn (if you occupy more battlegrounds than them)
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isEndOfEachTurn(game, effectResult)) {
            int battlegroundSitesYouOccupy = Filters.filterTopLocationsOnTable(game, Filters.and(Filters.battleground_site, Filters.occupies(playerId))).size();
            int battlegroundSitesOpponentOccupies = Filters.filterTopLocationsOnTable(game, Filters.and(Filters.battleground_site, Filters.occupies(opponent))).size();

            if (battlegroundSitesYouOccupy > battlegroundSitesOpponentOccupies) {
                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText(opponent + " loses 1 Force");
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, 1));
                actions.add(action);
            }
        }

        //Opponent loses 1 Force whenever they lose a character with ability during battle (limit once per battle)
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if (TriggerConditions.justLost(game, effectResult, Filters.and(Filters.opponents(playerId), Filters.character, Filters.hasAbility))
                && GameConditions.isDuringBattle(game)
                && GameConditions.isOncePerBattle(game, self, gameTextSourceCardId, gameTextActionId)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText(opponent + " loses 1 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        //Opponent loses 1 Force whenever they lose a battle
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;

        // Check condition(s)
        if (TriggerConditions.lostBattle(game, effectResult, opponent)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText(opponent + " loses 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        return actions;
    }
}
