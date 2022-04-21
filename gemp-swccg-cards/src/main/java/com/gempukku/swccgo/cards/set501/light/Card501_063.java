package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.ImprisonedOnlyCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameActionProxyEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Character
 * Subtype: Rebel
 * Title: Prisoner 2187 (V)
 */
public class Card501_063 extends AbstractRebel {
    public Card501_063() {
        super(Side.LIGHT, 1, 0, 4, 3, 6, Title.Prisoner_2187, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Princess Leia Organa. Alderaanian senator. Targeted by Vader for capture and interrogation. The Dark Lord of the Sith wanted her alive.");
        setGameText("Deploys only if A Power Loss on table. Opponent generates no Force from same Death Star site (even if imprisoned) unless an Imperial here. Your Force drains are +1 where you have a stormtrooper. Immune to attrition < 4.");
        addPersona(Persona.LEIA);
        addIcons(Icon.PREMIUM, Icon.WARRIOR, Icon.VIRTUAL_SET_18);
        addKeywords(Keyword.SENATOR, Keyword.FEMALE);
        setSpecies(Species.ALDERAANIAN);
        setTestingText("[Set 19] Prisoner 2187 (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.A_Power_Loss);
    }

    @Override
    protected List<Modifier> getGameTextWhileInactiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new GenerateNoForceModifier(self, Filters.and(Filters.sameSite(self), Filters.Death_Star_site),
                new AndCondition(new ImprisonedOnlyCondition(self), new UnlessCondition(new HereCondition(self, Filters.Imperial))), opponent));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new GenerateNoForceModifier(self, Filters.and(Filters.sameSite(self), Filters.Death_Star_site), new UnlessCondition(new HereCondition(self, Filters.Imperial)), opponent));
        modifiers.add(new ForceDrainModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.stormtrooper)), 1, playerId));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 4));
        return modifiers;
    }
}
