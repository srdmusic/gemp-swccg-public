package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.evaluators.OutOfPlayEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.TakeFirstBattleWeaponsSegmentActionEffect;
import com.gempukku.swccgo.logic.modifiers.IgnoresDeploymentRestrictionsFromCardModifier;
import com.gempukku.swccgo.logic.modifiers.ImmunityToAttritionChangeModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 15
 * Type: Effect
 * Title: Be With Me
 */
public class Card501_009 extends AbstractNormalEffect {
    public Card501_009() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Be With Me", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on table. Rey is power and immunity to attrition +1 for each Jedi out of play and, while Luke on table, ignores deployment restrictions on your [Set 11] objective. If Rey in battle with Kylo or a Dark Jedi Master, you may take the first weapons phase action. [Immune to Alter.]");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_15);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Be With Me");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new PowerModifier(self, Filters.Rey, new OutOfPlayEvaluator(self, Filters.Jedi)));
        modifiers.add(new ImmunityToAttritionChangeModifier(self, Filters.Rey, new OutOfPlayEvaluator(self, Filters.Jedi)));
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, Filters.Rey, new OnTableCondition(self, Filters.Luke), self.getOwner(), Filters.and(Filters.icon(Icon.VIRTUAL_SET_11), Filters.Objective)));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.battleInitiatedAt(game, effectResult, Filters.sameLocationAs(self, Filters.and(Filters.Rey, Filters.with(self, Filters.or(Filters.Kylo, Filters.Dark_Jedi_Master)))))) {
            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Take first weapons segment action");
            action.appendEffect(
                    new TakeFirstBattleWeaponsSegmentActionEffect(action, playerId));
            return Collections.singletonList(action);
        }
        return null;
    }
}