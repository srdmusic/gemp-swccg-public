package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.AboutToRetrieveForceResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: At Last We Are Getting Results & Begin Landing Your Troops
 */
public class Card501_051 extends AbstractNormalEffect {
    public Card501_051() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "At Last We Are Getting Results & Begin Landing Your Troops", Uniqueness.UNIQUE);
        addComboCardTitles("At Last We Are Getting Results", "Begin Landing Your Troops");
        setGameText("Deploy on table. Your unique (•) Republic characters of ability < 4 and your droid starfighters are forfeit +2. Nabrun Leids and Elis Helrot are limited to owner's move phase and exterior sites only. Once per turn, may deploy an [Episode I] (or Coruscant) docking bay from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("At Last We Are Getting Results & Begin Landing Your Troops");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.your(self), Filters.or(Filters.and(Filters.unique, Filters.Republic_character, Filters.abilityLessThan(4)), Filters.droid_starfighter)), 2));
        modifiers.add(new ModifyGameTextModifier(self, Filters.or(Filters.Nabrun_Leids, Filters.Elis_Helrot), ModifyGameTextType.NABRUN_LEIDS_ELIS_HELROT__LIMIT_USAGE));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.AT_LAST_WE_ARE_GETTING_RESULTS_BEGIN_LANDING_YOUR_TROOPS__DOWNLOAD_DOCKING_BAY;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy docking bay from Reserve Deck");
            action.setActionMsg("Deploy an [Episode I] (or Coruscant) docking bay from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.or(Icon.EPISODE_I, Filters.Coruscant_location), Filters.docking_bay), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}