package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractVehicleWeapon;
import com.gempukku.swccgo.cards.effects.PreventEffectOnCardEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Statistic;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.FireWeaponAction;
import com.gempukku.swccgo.logic.actions.FireWeaponActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.MayTargetAdjacentSiteModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Weapon
 * Subtype: Vehicle
 * Title: AT-AT Cannon (V)
 */
public class Card501_011 extends AbstractVehicleWeapon {
    public Card501_011() {
        super(Side.DARK, 3, Title.AT_AT_Cannon, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Laser cannons mounted on the head of an Imperial walker provide devastating coordinated firepower. Effective against a wide variety of targets.");
        setGameText("Deploy on your non-[Maintenance] AT-AT. May target a character or vehicle at same or adjacent site for 2 Force. Draw destiny. Target hit if destiny +2 > defense value. If this card about to leave table, may instead place in Used Pile. Immune to Sabotage.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_22);
        addKeywords(Keyword.AT_AT_CANNON);
        setTestingText("AT-AT Cannon (V)");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToTitleModifier(self, Title.Sabotage));
        return modifiers;
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.not(Icon.MAINTENANCE), Filters.AT_AT);
    }

    @Override
    protected Filter getGameTextValidToUseWeaponFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.and(Filters.not(Icon.MAINTENANCE), Filters.AT_AT);
    }

    @Override
    protected List<FireWeaponAction> getGameTextFireWeaponActions(String playerId, final SwccgGame game, final PhysicalCard self, boolean forFree, int extraForceRequired, PhysicalCard sourceCard, boolean repeatedFiring, Filter targetedAsCharacter, Float defenseValueAsCharacter, Filter fireAtTargetFilter, boolean ignorePerAttackOrBattleLimit) {
        FireWeaponActionBuilder actionBuilder = FireWeaponActionBuilder.startBuildPrep(playerId, game, sourceCard, self, forFree, extraForceRequired, repeatedFiring, targetedAsCharacter, defenseValueAsCharacter, fireAtTargetFilter, ignorePerAttackOrBattleLimit)
                .targetAtSameOrAdjacentSiteUsingForce(Filters.or(Filters.character, targetedAsCharacter, Filters.vehicle), 2, TargetingReason.TO_BE_HIT).finishBuildPrep();
        if (actionBuilder != null) {

            // Build action using common utility
            FireWeaponAction action = actionBuilder.buildFireWeaponWithHitAction(1, 2, Statistic.DEFENSE_VALUE);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayTargetAdjacentSiteModifier(self));
        return modifiers;
    }


    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isAboutToLeaveTableExceptFromSourceCard(game, effectResult, self, self)
                || TriggerConditions.isAboutToBeLostIncludingAllCardsSituation(game, effectResult, self)
                || TriggerConditions.isAboutToBeForfeitedToLostPile(game, effectResult, self)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Place on Used Pile");
            action.setActionMsg("Place " + GameUtils.getCardLink(self) + " on Used Pile");

            // Perform result(s)
            action.appendEffect(
                    new PreventEffectOnCardEffect(action, ((AboutToLeaveTableResult) effectResult).getPreventableCardEffect(), self, null));
            action.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}