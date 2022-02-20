package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.conditions.DuringBattleAtCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceAndStackFaceDownEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: TIE Defender Project
 */
public class Card501_032 extends AbstractNormalEffect {
    public Card501_032() {
        super(Side.DARK, 2, PlayCardZoneOption.ATTACHED, "TIE Defender Project", Uniqueness.UNIQUE);
        setGameText("Deploy on Lothal system. TIEs may deploy and land at related sites. TIE Defenders deploy -1 and may move as a 'react.' During battles at sites, your total battle destiny is +1 for each of your piloted TIE Defenders at locations related to that battle. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("TIE Defender Project");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Lothal_system;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayDeployAsLandedToLocationModifier(self, Filters.TIE, Filters.relatedSite(self)));
        modifiers.add(new TIEsMayLandAtExteriorSiteModifier(self, Filters.TIE, Filters.relatedSite(self)));
        modifiers.add(new DeployCostModifier(self, Filters.TIE_Defender, -1));
        modifiers.add(new MayMoveOtherCardsAsReactToLocationModifier(self, "Move TIE Defender as a 'react'", self.getOwner(), Filters.TIE_Defender, Filters.battleLocation));
        modifiers.add(new TotalBattleDestinyModifier(self, new DuringBattleAtCondition(Filters.site), new OnTableEvaluator(self, Filters.and(Filters.your(self), Filters.TIE_Defender, Filters.piloted, Filters.at(Filters.relatedLocationTo(self, Filters.battleLocation)))), self.getOwner()));
        return modifiers;
    }
}