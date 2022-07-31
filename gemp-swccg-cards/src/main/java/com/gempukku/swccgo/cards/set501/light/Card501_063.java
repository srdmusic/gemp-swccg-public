package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.ImprisonedOnlyCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.GenerateNoForceModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
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
        addIcons(Icon.PREMIUM, Icon.WARRIOR, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.SENATOR, Keyword.FEMALE);
        setSpecies(Species.ALDERAANIAN);
        setTestingText("~Prisoner 2187 (V)");
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
