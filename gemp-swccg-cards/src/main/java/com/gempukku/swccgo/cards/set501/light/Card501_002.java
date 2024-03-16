package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Statistic;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.FireWeaponAction;
import com.gempukku.swccgo.logic.actions.FireWeaponActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.WeaponBeingFiredByCondition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.modifiers.TotalWeaponDestinyModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Weapon
 * Subtype: Character
 * Title: Din's IB-94 Blaster Pistol
 */
public class Card501_002 extends AbstractCharacterWeapon {
    public Card501_002() {
        super(Side.LIGHT, 2, "Din's IB-94 Blaster Pistol", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Deploy on Din. May target a character or creature for free. Draw destiny. Add 1 if targeting 'The Asset' " +
                "If destiny +1 > defense value, target hit and forfeit = 0. " +
                "If fired by Din in battle, he is power +1 for remainder of battle.");
        addKeywords(Keyword.BLASTER);
        setMatchingCharacterFilter(Filters.Din);
        setTestingText("Din's IB-94 Blaster Pistol");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Din;
    }

    @Override
    protected Filter getGameTextValidToUseWeaponFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.Din;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalWeaponDestinyModifier(self, 1, Filters.The_Asset));
        return modifiers;
    }


    @Override
    protected List<FireWeaponAction> getGameTextFireWeaponActions(String playerId, SwccgGame game, PhysicalCard self, boolean forFree, int extraForceRequired, PhysicalCard sourceCard, boolean repeatedFiring, Filter targetedAsCharacter, Float defenseValueAsCharacter, Filter fireAtTargetFilter, boolean ignorePerAttackOrBattleLimit) {
        FireWeaponActionBuilder actionBuilder = FireWeaponActionBuilder.startBuildPrep(playerId, game, sourceCard, self, forFree, extraForceRequired, repeatedFiring, targetedAsCharacter, defenseValueAsCharacter, fireAtTargetFilter, ignorePerAttackOrBattleLimit)
                .targetUsingForce(Filters.or(Filters.character, targetedAsCharacter, Filters.creature), 0, TargetingReason.TO_BE_HIT).finishBuildPrep();
        if (actionBuilder != null) {

            // Build action using common utility
            FireWeaponAction action = actionBuilder.buildFireWeaponWithHitAction(1, 1, Statistic.DEFENSE_VALUE,
                    new WeaponBeingFiredByCondition(self, Filters.Din), true, 0);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.weaponJustFiredBy(game, effectResult, self, Filters.Din)
                && GameConditions.isDuringBattle(game)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make Din power +1");
            // Perform result(s)
            action.appendEffect(
                    new AddUntilEndOfBattleModifierEffect(action,
                            new PowerModifier(self, Filters.Din, 1),
                            "Makes " + GameUtils.getCardLink(Filters.findFirstActive(game, self, Filters.Din)) + " power +1"));
            return Collections.singletonList(action);
        }
        return null;
    }
}
