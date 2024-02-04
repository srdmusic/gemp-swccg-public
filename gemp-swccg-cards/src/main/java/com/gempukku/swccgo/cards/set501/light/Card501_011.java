package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelBattleEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.DestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 23
 * Type: Epic Event
 * Title: Bounty Hunting Is A Dangerous Profession
 */
public class Card501_011 extends AbstractEpicEventDeployable {
    public Card501_011() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Bounty_Hunting_Is_A_Dangerous_Profession, Uniqueness.UNIQUE, ExpansionSet.SET_23, Rarity.V);
        setGameText("Deploy on table if your [Mudhorn] objective on table. " +
                "Din and Grogu are destiny +1. Boba, Bo-Katan, Cara Dune, and Grogu are deploy -1. " +
                "While you occupy a battleground: At the beginning of each control phase, opponent loses 1 Force unless \"The Asset\" present at a battleground. " +
                "If a battle just initiated against \"The Asset\", opponent may place \"The Asset\" in Used Pile to cancel the battle.");
        addIcon(Icon.MUDHORN);
        addIcon(Icon.VIRTUAL_SET_23);
        setTestingText("Bounty Hunting Is A Dangerous Profession");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        Filter dinGrogu = Filters.and(Filters.your(self), Filters.or(Filters.Din, Filters.Grogu));
        Filter groguBobaBoCara = Filters.and(Filters.your(self), Filters.or(Filters.Grogu, Filters.Bo_Katan, Filters.Boba_Fett, Filters.Cara_Dune));

        modifiers.add(new DestinyModifier(self, dinGrogu, 1));
        modifiers.add(new DeployCostModifier(self, groguBobaBoCara, -1));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (TriggerConditions.isStartOfEachPhase(game, effectResult, Phase.CONTROL)
                && GameConditions.isOnceDuringEitherPlayersPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && !GameConditions.occupiesWith(game, self, opponent, Filters.battleground, Filters.The_Asset)
                && GameConditions.canSpot(game, self, Filters.The_Asset)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make " + opponent + " lose 1 Force");
            action.setActionMsg("Make " + opponent + " lose 1 force for \"The Asset\" not being present at a battleground");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            return Collections.singletonList(action);
        }

        return null;
    }
    @Override
    protected List<OptionalGameTextTriggerAction> getOpponentsCardGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        PhysicalCard theAsset = Filters.findFirstActive(game, self, Filters.The_Asset);
        if (TriggerConditions.battleInitiated(game, effectResult, game.getLightPlayer())
                && GameConditions.isDuringBattleWithParticipant(game, Filters.The_Asset)) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place \"The Asset\" in Used Pile");
            action.setActionMsg("Place \"The Asset\" in Used Pile to cancel battle just initiated");
            action.appendEffect(
                    new CancelBattleEffect(action));
            action.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, theAsset));
            return Collections.singletonList(action);
        }

        return null;
    }
}
