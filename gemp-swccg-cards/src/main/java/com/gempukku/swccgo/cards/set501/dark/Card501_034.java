package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImmediateEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.InactiveReason;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayCardAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.CaptureCharacterResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Set: Playtesting
 * Type: Effect
 * Subtype: Immediate
 * Title: The Client's Bounty
 */
public class Card501_034 extends AbstractImmediateEffect {
    public Card501_034() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, "The Client's Bounty", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Unless a [Death Star II] objective on table, deploy on a just captured character. During your control phase, if with your leader (or The Client) at a site you control, may take this card into hand; captive is lost and you retrieve 3 Force. (Immune to Control while Greef or The Client on table.)");
        addIcons(Icon.VIRTUAL_SET_24);
        addKeywords(Keyword.BOUNTY);
        setTestingText("The Client's Bounty");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToTitleModifier(self, new OnTableCondition(self, Filters.or(Filters.persona(Persona.GREEF), Filters.title("The Client"))), Title.Control));
        return modifiers;
    }

    @Override
    public Map<InactiveReason, Boolean> getDeployTargetSpotOverride(PlayCardOptionId playCardOptionId) {
        return SpotOverride.INCLUDE_CAPTIVE;
    }

    @Override
    protected List<PlayCardAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (!GameConditions.canTarget(game, self, Filters.and(Icon.DEATH_STAR_II, Filters.Objective))
                && TriggerConditions.captured(game, effectResult, Filters.character)) {
            PhysicalCard capturedCard = ((CaptureCharacterResult) effectResult).getCapturedCard();
            PlayCardAction action = getPlayCardAction(playerId, game, self, self, false, 0, null, null, null, null, null, false, 0, Filters.sameCardId(capturedCard), null);
            if (action != null) {
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActionsWhenInactiveInPlay(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {

        if (GameConditions.isDuringYourPhase(game, playerId, Phase.CONTROL)
                && GameConditions.controls(game, playerId, Filters.here(self))
                && GameConditions.isAtLocation(game, self, Filters.sameLocationAs(self, Filters.or(Filters.and(Filters.your(self), Filters.leader), Filters.title("The Client"))))
                && GameConditions.canTarget(game, self, SpotOverride.INCLUDE_CAPTIVE, TargetingReason.TO_BE_LOST, Filters.and(Filters.captive, Filters.hasAttached(self)))) {

            PhysicalCard captive = Filters.findFirstActive(game, self, SpotOverride.INCLUDE_CAPTIVE, Filters.and(Filters.captive, Filters.hasAttached(self)));
            if (captive != null) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
                action.setText("Take into hand");
                action.setActionMsg("Make captive lost and retrieve 3 Force");

                action.appendCost(
                        new ReturnCardToHandFromTableEffect(action, self));

                action.appendEffect(
                        new LoseCardFromTableEffect(action, captive));
                action.appendEffect(
                        new RetrieveForceEffect(action, playerId, 3));

                return Collections.singletonList(action);
            }
        }

        return null;
    }
}